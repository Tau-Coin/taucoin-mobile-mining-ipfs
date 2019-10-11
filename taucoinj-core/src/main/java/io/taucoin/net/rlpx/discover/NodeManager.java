package io.taucoin.net.rlpx.discover;

import io.taucoin.config.SystemProperties;
import io.taucoin.crypto.ECKey;
import io.taucoin.datasource.mapdb.MapDBFactory;
import io.taucoin.http.RequestManager;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.rlpx.*;
import io.taucoin.net.rlpx.discover.table.NodeTable;
import io.taucoin.util.CollectionUtils;
import io.taucoin.util.Functional;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Math.min;
import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * The central class for Peer Discovery machinery.
 *
 * The NodeManager manages info on all the Nodes discovered by the peer discovery
 * protocol, routes protocol messages to the corresponding NodeHandlers and
 * supplies the info about discovered Nodes and their usage statistics
 *
 * Created by Anton Nashatyrev on 16.07.2015.
 */
@Singleton
public class NodeManager implements Functional.Consumer<DiscoveryEvent>{
    static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    // to avoid checking for null
    private static NodeStatistics DUMMY_STAT = new NodeStatistics(new Node(new byte[0], "dummy.node", 0));
    private boolean PERSIST;

    private static final long LISTENER_REFRESH_RATE = 3000;
    private static final long DB_COMMIT_RATE = 1 * 60 * 1000;
    private static final int DB_MAX_LOAD_NODES = 100;
    static final int MAX_NODES = 2000;

    PeerConnectionTester peerConnectionManager;

    MapDBFactory mapDBFactory;

    TaucoinListener ethereumListener;

    WorldManager worldManager;

    SystemProperties config = SystemProperties.CONFIG;

    Functional.Consumer<DiscoveryEvent> messageSender;

    NodeTable table;
    private Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    ECKey key;
    Node homeNode;
    private List<Node> bootNodes;

    // option to handle inbounds only from known peers (i.e. which were discovered by ourselves)
    boolean inboundOnlyFromKnownNodes = false;

    private boolean discoveryEnabled;

    private Map<DiscoverListener, ListenerHandler> listeners = new IdentityHashMap<>();

    private DB db;
    private HTreeMap<Node, NodeStatistics.Persistent> nodeStatsDB;
    private boolean inited = false;

    private Timer statisTimer = null;
    private Timer taskTimer = null;

    @Inject
    public NodeManager(PeerConnectionTester peerConnectionManager, MapDBFactory mapDBFactory, TaucoinListener ethereumListener) {

        this.peerConnectionManager = peerConnectionManager;
        this.mapDBFactory = mapDBFactory;
        this.ethereumListener = ethereumListener;

        PERSIST = config.peerDiscoveryPersist();
        discoveryEnabled = config.peerDiscovery();

        key = CONFIG.getMyKey();
        homeNode = new Node(CONFIG.nodeId(), CONFIG.externalIp(), CONFIG.listenPort());
        homeNode.setType(config.getHomeNodeType());
        table = new NodeTable(homeNode, CONFIG.isPublicHomeNode());

        this.statisTimer = new Timer();
        statisTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                logger.trace("Statistics:\n {}", dumpAllStatistics());
            }
        }, 1 * 1000, 10 * 1000);
    }

    public void setWorldManager(WorldManager worldManager) {
        this.worldManager = worldManager;
        this.peerConnectionManager.setWorldManager(worldManager);
    }

    void setBootNodes(List<Node> bootNodes) {
        this.bootNodes = bootNodes;
    }

    void channelActivated() {
        // channel activated now can send messages
        if (!inited) {
            // no another init on a new channel activation
            inited = true;
            logger.info("channel activated");
            peerConnectionManager.init();

            this.taskTimer = new Timer("NodeManagerTasks");

            // this task is done asynchronously with some fixed rate
            // to avoid any overhead in the NodeStatistics classes keeping them lightweight
            // (which might be critical since they might be invoked from time critical sections)
            taskTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    processListeners();
                }
            }, LISTENER_REFRESH_RATE + 10000, LISTENER_REFRESH_RATE);

            if (PERSIST) {
                dbRead();
                taskTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        dbWrite();
                    }
                }, DB_COMMIT_RATE, DB_COMMIT_RATE);
            }

            for (Node node : config.peerActive()) {
                getNodeHandler(node).getNodeStatistics().setPredefined(true);
            }

            for (Node node : bootNodes) {
                getNodeHandler(node);
                // Hack code just for debugging. It should be removed ASAP.
                // getNodeHandler(node).getNodeStatistics().setPredefined(true);
            }
        }
    }

    public void shutdown() {
        if (!inited) return;

        peerConnectionManager.shutdown();
        statisTimer.cancel();
        taskTimer.cancel();
        dbWrite();
        dbClose();
        inited = false;
    }

    private void dbRead() {
        try {
            db = mapDBFactory.createTransactionalDB("network/discovery");
            if (SystemProperties.CONFIG.databaseReset()) {
                logger.info("Resetting DB Node statistics...");
                db.delete("nodeStats");
            }
            nodeStatsDB = db.hashMapCreate("nodeStats")
                    .keySerializer(Node.MapDBSerializer)
                    .valueSerializer(NodeStatistics.Persistent.MapDBSerializer)
                    .makeOrGet();

            List<Map.Entry<Node, NodeStatistics.Persistent>> sorted = new ArrayList<>(nodeStatsDB.entrySet());
            Collections.sort(sorted, new Comparator<Map.Entry<Node, NodeStatistics.Persistent>>() {
                public int compare(Map.Entry<Node, NodeStatistics.Persistent> o1, Map.Entry<Node, NodeStatistics.Persistent> o2) {
                    return o2.getValue().reputation - o1.getValue().reputation;
                }
            });

            logger.info("Reading Node statistics from DB: " + min(DB_MAX_LOAD_NODES, nodeStatsDB.size())  + " of " + nodeStatsDB.size() + " nodes.");

            int cnt = DB_MAX_LOAD_NODES;
            for (Map.Entry<Node, NodeStatistics.Persistent> entry : sorted) {
                getNodeHandler(entry.getKey()).getNodeStatistics().setPersistedData(entry.getValue());
                if (--cnt == 0) break;
            }
        } catch (Exception e) {
            try {
                logger.error("Error reading db. Recreating from scratch:", e);
                if (db != null) {
                    db.delete("nodeStats");
                    nodeStatsDB = db.hashMap("nodeStats");
                }
            } catch (Exception e1) {
                logger.error("DB recreation has been failed. Node statistics persistence disabled. The problem needs to be fixed manually.", e1);
            }
        }
    }

    private void dbWrite() {
        synchronized (this) {
            for (NodeHandler handler : nodeHandlerMap.values()) {
                if(nodeStatsDB != null){
                    nodeStatsDB.put(handler.getNode(), handler.getNodeStatistics().getPersistent());
                }
            }
        }
        if (db != null) {
            db.commit();
        }
        logger.info("Write Node statistics to DB: " + (nodeStatsDB != null ? nodeStatsDB.size() : 0) + " nodes.");
    }

    private void dbClose() {
        if (db != null) {
            db.close();
            db = null;
        }

        nodeStatsDB = null;
    }

    public void setMessageSender(Functional.Consumer<DiscoveryEvent> messageSender) {
        this.messageSender = messageSender;
    }

    private String getKey(Node n) {
        return getKey(new InetSocketAddress(n.getHost(), n.getPort()));
    }

    private String getKey(InetSocketAddress address) {
        InetAddress addr = address.getAddress();
        // addr == null if the hostname can't be resolved
        return (addr == null ? address.getHostString() : addr.getHostAddress()) + ":" + address.getPort();
    }

    public synchronized NodeHandler getNodeHandler(Node n) {
        String key = getKey(n);
        NodeHandler ret = nodeHandlerMap.get(key);
        if (ret == null) {
            ret = new NodeHandler(n ,this);
            nodeHandlerMap.put(key, ret);
            logger.info(" +++ New node: " + ret + " " + n);
            if (!n.isDiscoveryNode() && !n.getHexId().equals(homeNode.getHexId())) {
                ethereumListener.onNodeDiscovered(ret.getNode());
            }
        } else if (ret.getNode().isDiscoveryNode() && !n.isDiscoveryNode()) {
            // we found discovery node with same host:port,
            // replace node with correct nodeId
            n.setType(ret.node.getType());
            ret.node = n;
            if (!n.getHexId().equals(homeNode.getHexId())) {
                ethereumListener.onNodeDiscovered(ret.getNode());
            }
            logger.info(" +++ Found real nodeId for discovery endpoint {}", n);
        } else if (!ret.getNode().getHexId().equals(n.getHexId())) {
            // Peer nodeId changed
            logger.info("NodeId changed old {} new {}", ret, n);
            ret.node = n;
        }

        return ret;
    }


    boolean hasNodeHandler(Node n) {
        return nodeHandlerMap.containsKey(getKey(n));
    }

    public NodeTable getTable() {
        return table;
    }

    public NodeStatistics getNodeStatistics(Node n) {
        return discoveryEnabled ? getNodeHandler(n).getNodeStatistics() : DUMMY_STAT;
    }

    @Override
    public void accept(DiscoveryEvent discoveryEvent) {
        handleInbound(discoveryEvent);
    }

    public void handleInbound(DiscoveryEvent discoveryEvent) {
        Message m = discoveryEvent.getMessage();
        InetSocketAddress sender = discoveryEvent.getAddress();

        Node n = new Node(m.getNodeId(), sender.getHostName(), sender.getPort());

        if (inboundOnlyFromKnownNodes && !hasNodeHandler(n)) {
            logger.debug("=/=> (" + sender + "): inbound packet from unknown peer rejected due to config option.");
            return;
        }
        NodeHandler nodeHandler = getNodeHandler(n);

        logger.trace("===> ({}) {} [{}] {}", sender, m.getClass().getSimpleName(), nodeHandler, m);

        byte type = m.getType()[0];
        switch (type) {
            case 1:
                nodeHandler.handlePing((PingMessage) m);
                break;
            case 2:
                nodeHandler.handlePong((PongMessage) m);
                break;
            case 3:
                nodeHandler.handleFindNode((FindNodeMessage) m);
                break;
            case 4:
                nodeHandler.handleNeighbours((NeighborsMessage) m);
                break;
        }
    }

    public void sendOutbound(DiscoveryEvent discoveryEvent) {
        if (discoveryEnabled && messageSender != null) {
            logger.trace(" <===({}) {} [{}] {}", discoveryEvent.getAddress(),
                    discoveryEvent.getMessage().getClass().getSimpleName(), this, discoveryEvent.getMessage());
            messageSender.accept(discoveryEvent);
        }
    }

    public void stateChanged(NodeHandler nodeHandler, NodeHandler.State oldState, NodeHandler.State newState) {
        if (discoveryEnabled) {
            peerConnectionManager.nodeStatusChanged(nodeHandler);
        }
    }

    public synchronized List<NodeHandler> getNodes(int minReputation) {
        List<NodeHandler> ret = new ArrayList<>();
        for (NodeHandler nodeHandler : nodeHandlerMap.values()) {
            if (nodeHandler.getNodeStatistics().getReputation() >= minReputation) {
                ret.add(nodeHandler);
            }
        }
        return ret;
    }

    /**
     * Returns list of unused Tau nodes with highest total difficulty<br>
     *     Search criteria:
     *     <ul>
     *         <li>not presented in {@code usedIds} collection</li>
     *         <li>eth status processing succeeded</li>
     *         <li>difficulty is higher than {@code lowerDifficulty}</li>
     *     </ul>
     *
     *
     * @param usedIds collections of ids which are excluded from results
     * @param lowerDifficulty nodes having TD lower than this value are sorted out
     * @param limit max size of returning list
     *
     * @return list of nodes with highest difficulty, ordered by TD in desc order
     */
    public List<NodeHandler> getBestEthNodes(
            final Set<String> usedIds,
            final BigInteger lowerDifficulty,
            int limit
    ) {
        return getNodes(new Functional.Predicate<NodeHandler>() {
            @Override
            public boolean test(NodeHandler handler) {
                if (usedIds.contains(handler.getNode().getHexId())) {
                    return false;
                }

                if (handler.getNodeStatistics().isPredefined()) return false;

                if (handler.getNodeStatistics().getEthTotalDifficulty() == null) {
                    return false;
                }
                // discard total difficulty
                //return handler.getNodeStatistics().getEthTotalDifficulty().compareTo(lowerDifficulty) >= 0;
                return true;
            }
        }, BEST_DIFFICULTY_COMPARATOR, limit);
    }

    public List<NodeHandler> getBestEthNodes(
            final List<RequestManager> usedIds,
            final BigInteger lowerDifficulty,
            int limit
    ){
       return null;
    }

    /**
     * Returns limited list of nodes matching {@code predicate} criteria<br>
     * Sorting is performed before result truncation,
     * therefore result list contains best nodes according to provided {@code comparator}
     *
     * @param predicate only those nodes which are satisfied to its condition are included in results
     * @param comparator used to sort nodes before truncation
     * @param limit max size of returning list
     *
     * @return list of nodes matching criteria
     */
    private List<NodeHandler> getNodes(
            Functional.Predicate<NodeHandler> predicate,
            Comparator<NodeHandler> comparator,
            int limit
    ) {
        Map<String, NodeHandler> handlersMap = new HashMap<String, NodeHandler>();
        List<NodeHandler> filtered = new ArrayList<>();
        synchronized (this) {
            for (NodeHandler handler : nodeHandlerMap.values()) {
                if ((handler.getState() == NodeHandler.State.Active)
                        && !handlersMap.containsKey(handler.getNode().getHexId())
                        && predicate.test(handler)) {
                    filtered.add(handler);
                    handlersMap.put(handler.getNode().getHexId(), handler);
                }
            }
        }
        Collections.sort(filtered, comparator);
        return CollectionUtils.truncate(filtered, limit);
    }

    private synchronized void processListeners() {
        for (ListenerHandler handler : listeners.values()) {
            try {
                handler.checkAll();
            } catch (Exception e) {
                logger.error("Exception processing listener: " + handler, e);
            }
        }
    }

    /**
     * Add a listener which is notified when the node statistics starts or stops meeting
     * the criteria specified by [filter] param.
     */
    public synchronized void addDiscoverListener(DiscoverListener listener, Functional.Predicate<NodeStatistics> filter) {
        listeners.put(listener, new ListenerHandler(listener, filter));
    }

    public synchronized void removeDiscoverListener(DiscoverListener listener) {
        listeners.remove(listener);
    }

    public synchronized String dumpAllStatistics() {
        List<NodeHandler> l = new ArrayList<>(nodeHandlerMap.values());
        Collections.sort(l, new Comparator<NodeHandler>() {
            public int compare(NodeHandler o1, NodeHandler o2) {
                return -(o1.getNodeStatistics().getReputation() - o2.getNodeStatistics().getReputation());
            }
        });

        StringBuilder sb = new StringBuilder();
        int zeroReputCount = 0;
        for (NodeHandler nodeHandler : l) {
            if (nodeHandler.getNodeStatistics().getReputation() > 0) {
                sb.append(nodeHandler).append("\t").append(nodeHandler.getNodeStatistics()).append("\n");
            } else {
                zeroReputCount++;
            }
        }
        sb.append("0 reputation: " + zeroReputCount + " nodes.\n");
        return sb.toString();
    }

    public boolean isNeedMoreSyncPeers() {
        //return this.worldManager.getSyncManager().isNeedMorePeers();
        return false;
    }

    private class ListenerHandler {
        Map<NodeHandler, Object> discoveredNodes = new IdentityHashMap<>();
        DiscoverListener listener;
        Functional.Predicate<NodeStatistics> filter;

        ListenerHandler(DiscoverListener listener, Functional.Predicate<NodeStatistics> filter) {
            this.listener = listener;
            this.filter = filter;
        }

        void checkAll() {
            for (NodeHandler handler : nodeHandlerMap.values()) {
                boolean has = discoveredNodes.containsKey(handler);
                boolean test = filter.test(handler.getNodeStatistics());
                if (!has && test) {
                    listener.nodeAppeared(handler);
                    discoveredNodes.put(handler, null);
                } else if (has && !test) {
                    listener.nodeDisappeared(handler);
                    discoveredNodes.remove(handler);
                }
            }
        }
    }

    private static final Comparator<NodeHandler> BEST_DIFFICULTY_COMPARATOR = new Comparator<NodeHandler>() {
        @Override
        public int compare(NodeHandler n1, NodeHandler n2) {
            BigInteger td1 = null;
            BigInteger td2 = null;
            if(n1.getNodeStatistics().getEthTotalDifficulty() != null) {
                td1 = n1.getNodeStatistics().getEthTotalDifficulty();
            }
            if(n2.getNodeStatistics().getEthTotalDifficulty() != null) {
                td2 = n2.getNodeStatistics().getEthTotalDifficulty();
            }
            if (td1 != null && td2 != null) {
                return td2.compareTo(td1);
            } else if (td1 == null && td2 == null) {
                return 0;
            } else if (td1 != null) {
                return -1;
            } else {
                return 1;
            }
        }
    };
}
