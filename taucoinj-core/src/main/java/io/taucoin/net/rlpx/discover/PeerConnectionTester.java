package io.taucoin.net.rlpx.discover;

import org.spongycastle.util.encoders.Hex;
import io.taucoin.config.SystemProperties;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.rlpx.Node;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Makes test RLPx connection to the peers to acquire statistics
 *
 * Created by Anton Nashatyrev on 17.07.2015.
 */
@Singleton
public class PeerConnectionTester {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    private int ConnectThreads;
    private long ReconnectPeriod;
    private long ReconnectMaxPeers;

    private WorldManager worldManager;

    SystemProperties config = SystemProperties.CONFIG;

    // NodeHandler instance should be unique per Node instance
    private Map<NodeHandler, ?> connectedCandidates = new IdentityHashMap<>();

    // executor with Queue which picks up the Node with the best reputation
    private ExecutorService peerConnectionPool;

    private Timer reconnectTimer = new Timer("DiscoveryReconnectTimer");
    private int reconnectPeersCount = 0;


    private class ConnectTask implements Runnable {
        NodeHandler nodeHandler;

        public ConnectTask(NodeHandler nodeHandler) {
            this.nodeHandler = nodeHandler;
        }

        @Override
        public void run() {
            try {
                if (nodeHandler != null) {
                    nodeHandler.getNodeStatistics().rlpxConnectionAttempts.add();
                    logger.debug("Trying node connection: " + nodeHandler);
                    Node node = nodeHandler.getNode();
                    worldManager.getActivePeer().connect(node.getHost(), node.getPort(),
                            Hex.toHexString(node.getId()), true);
                    logger.debug("Terminated node connection: " + nodeHandler);
                    nodeHandler.getNodeStatistics().disconnected();
                    if (!nodeHandler.getNodeStatistics().getEthTotalDifficulty().equals(BigInteger.ZERO) &&
                            ReconnectPeriod > 0 && (reconnectPeersCount < ReconnectMaxPeers || ReconnectMaxPeers == -1)) {
                        // trying to keep good peers information up-to-date
                        reconnectPeersCount++;
                        reconnectTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                logger.debug("Trying the node again: " + nodeHandler);
                                peerConnectionPool.execute(new ConnectTask(nodeHandler));
                                reconnectPeersCount--;
                            }
                        }, ReconnectPeriod);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject
    public PeerConnectionTester() {
    }

    public void setWorldManager(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public void init() {
        ConnectThreads = config.peerDiscoveryWorkers();
        ReconnectPeriod = config.peerDiscoveryTouchPeriod() * 1000;
        ReconnectMaxPeers = config.peerDiscoveryTouchMaxNodes();
        peerConnectionPool = new ThreadPoolExecutor(ConnectThreads,
                ConnectThreads, 0L, TimeUnit.SECONDS,
                new MutablePriorityQueue<Runnable, ConnectTask>(new Comparator<ConnectTask>() {
                    @Override
                    public int compare(ConnectTask h1, ConnectTask h2) {
                        return h2.nodeHandler.getNodeStatistics().getReputation() -
                                h1.nodeHandler.getNodeStatistics().getReputation();
                    }
                }));
    }

    public void nodeStatusChanged(final NodeHandler nodeHandler) {
        if (!config.peerConnectionTestEnabled() || peerConnectionPool == null || peerConnectionPool.isShutdown()) return;
        if (connectedCandidates.size() < NodeManager.MAX_NODES
                && !connectedCandidates.containsKey(nodeHandler)
                && !nodeHandler.getNode().isDiscoveryNode()) {
            logger.debug("Submitting node for RLPx connection : " + nodeHandler);
            connectedCandidates.put(nodeHandler, null);
            peerConnectionPool.execute(new ConnectTask(nodeHandler));
        }
    }

    public void shutdown() {
       if (peerConnectionPool != null) {
           peerConnectionPool.shutdownNow();
           peerConnectionPool = null;
       }

       reconnectTimer.cancel();
       reconnectTimer = null;
    }

    /**
     * The same as PriorityBlockQueue but with assumption that elements are mutable
     * and priority changes after enqueueing, thus the list is sorted by priority
     * each time the head queue element is requested.
     * The class has poor synchronization since the prioritization might be approximate
     * though the implementation should be inheritedly thread-safe
     */
    public static class MutablePriorityQueue<T, C extends T> extends LinkedBlockingQueue<T> {
        Comparator<C> comparator;

        public MutablePriorityQueue(Comparator<C> comparator) {
            this.comparator = comparator;
        }

        @Override
        public T take() throws InterruptedException {
            if (isEmpty()) {
                return super.take();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public T poll(long timeout, TimeUnit unit) throws InterruptedException {
            if (isEmpty()) {
                return super.poll(timeout, unit);
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public T poll() {
            if (isEmpty()) {
                return super.poll();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public T peek() {
            if (isEmpty()) {
                return super.peek();
            } else {
                T ret = Collections.min(this, (Comparator<? super T>) comparator);
                return ret;
            }
        }
    }

}
