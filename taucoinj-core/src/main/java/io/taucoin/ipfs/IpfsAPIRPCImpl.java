package io.taucoin.ipfs;

import io.taucoin.core.Block;
import io.taucoin.core.Blockchain;
import io.taucoin.core.PendingState;
import io.taucoin.core.Transaction;
import io.taucoin.facade.IpfsAPI;
import io.taucoin.http.tau.message.NewBlockMessage;
import io.taucoin.http.tau.message.NewTxMessage;
import io.taucoin.ipfs.config.Topic;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;
import io.taucoin.listener.TaucoinListener;

import io.ipfs.api.IPFS;
import io.ipfs.api.Peer;

import io.taucoin.sync2.SyncQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IpfsAPIRPCImpl implements IpfsAPI {

    private static final Logger logger = LoggerFactory.getLogger("IpfsService");

    private static final long RECONNECT_IPFS_DAEMON_DURATION = 3000;

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    // temp home node id, just for test.
    private static final String HOME_NODE_ID = "id";

    private TaucoinListener tauListener;

    protected IpfsHomeNodeInfo ipfsHomeNodeInfo = null;

    private IPFS ipfs;

    private boolean initDone = false;

    // 'initLock' protects race for 'ipfs' & 'initDone'.
    private final ReentrantLock initLock = new ReentrantLock();
    private final Condition init = initLock.newCondition();

    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);

    private Runnable ipfsConnector = new Runnable() {
        @Override
        public void run() {
            connectToIpfs();
        }
    };

    private Thread connectWorker = new Thread(ipfsConnector);

    /**
     * these 2 thread used to publish transaction and block coming from client to reduce the blocking time of main thread.
     */
    private Thread txPubThread;
    private Thread blockPubThread;

    /**
     * Queue with new blocks forged.
     */
    private BlockingQueue<Block> newBlocks = new LinkedBlockingQueue<>();

    /**
     * Queue with new transactions.
     */
    private BlockingQueue<Transaction> newTransactions = new LinkedBlockingQueue<>();

    @Inject
    public IpfsAPIRPCImpl(TaucoinListener tauListener) {
        this.tauListener = tauListener;
        init();
    }

    public IpfsAPIRPCImpl() {
        init();
    }

    private void init() {
        tryToConnectToIpfsDaemon();
    }

    private void tryToConnectToIpfsDaemon() {
        if (isConnected.get() || isConnecting.get()) {
            logger.info("IPFS connection {} is still alive", ipfs);
            return;
        }

        isConnecting.set(true);

        connectWorker.start();

        /**
         * create above definete thread and start them to loop publish tx and block.
         */
        this.txPubThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newTxDistributeLoop();
            }
        },"newTxPublishThread");
        this.txPubThread.start();

        this.blockPubThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newBlockDistributeLoop();
            }
        },"newBlockPublishThread");
        this.blockPubThread.start();
    }

    private void connectToIpfs() {
        if (isConnected.get()) {
            logger.info("IPFS connection {} is still alive", ipfs);
            return;
        }

        initLock.lock();

        while(true) {
            // From the source: https://github.com/ipfs/java-ipfs-http-client/blob/master/src/main/java/io/ipfs/api/IPFS.java,
            // It can be found that IPFS constructor will try to connect to ipfs daemon.
            // And if ipfs daemon hasn't been launched, exception will be thrown.
            // Try to connect to ipfs daemon util connected.
            try {
                ipfs = new IPFS(LOCAL_IPFS);

                isConnecting.set(false);
                isConnected.set(true);
            } catch (Exception e) {
                logger.error("Connecting to ipfs daemon error: {}", e);

                try {
                    Thread.sleep(RECONNECT_IPFS_DAEMON_DURATION);
                } catch (InterruptedException ie) {
                    logger.error("Connecting to ipfs daemon error: {}", ie);
                }

                continue;
            }

            if (isConnected.get()) {
                logger.info("Connection to ipfs daemon is alive");
                initDone = true;
                init.signalAll();
                initLock.unlock();

                break;
            }
        }
    }

    protected void onIpfsDaemonDisconnected() {
        logger.warn("Ipfs daemon dead");

        initLock.lock();
        initDone = false;
        isConnecting.set(false);
        isConnected.set(false);
        initLock.unlock();

        if (tauListener != null) {
            tauListener.onIpfsDaemonDead();
        }

        tryToConnectToIpfsDaemon();
    }

    private boolean isDaemonDisconnected(Exception e) {
        // Hack code, very ugly.
        // From the source: https://github.com/ipfs/java-ipfs-http-client/blob/master/src/main/java/io/ipfs/api/IPFS.java,
        // if rpc connection disappears, RuntimeException will be thrown.
        // throw new RuntimeException("Couldn't connect to IPFS daemon at "+target+"\n Is IPFS running?");
        if (e != null && e instanceof RuntimeException) {
            String message = e.getMessage();
            if (message.contains("Couldn't connect to IPFS daemon")) {
                return true;
            }
        }

        return false;
    }

    public IPFS getLocalIpfs() {
        awaitInit();

        return ipfs;
    }

    public boolean sendTransaction(Transaction tx) {
        awaitInit();

        if (tx == null) {
            logger.warn("send null transaction");
            return false;
        }

        /**
         * here is ipfs publish strategry although there will be 0 peer to sub in worst situation
         * this queue doesn't become large and large.
         */
        return this.newTransactions.add(tx);
    }

    public boolean sendNewBlock(Block block){
        awaitInit();

        if (block == null) {
            logger.warn("send null block");
            return false;
        }

        return this.newBlocks.add(block);
    }

    public IpfsHomeNodeInfo getIpfsHomeNode() {
        awaitInit();

        if (ipfsHomeNodeInfo != null) {
            return ipfsHomeNodeInfo;
        }

        String id = "";
        String version = "";

        try {
            Map idMap = ipfs.id();
            logger.info("Ipfs home node id:{}", idMap);
            if (idMap != null) {
                id = (String)idMap.get("ID");
            }
        } catch (IOException ioe) {
            logger.error("getId ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("getId exception: {}", e);
        }

        try {
            version = ipfs.version();
        } catch (IOException ioe) {
            logger.error("get version ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("get version exception: {}", e);
            if (isDaemonDisconnected(e)) {
                onIpfsDaemonDisconnected();
                return getIpfsHomeNode();
            }
        }

        ipfsHomeNodeInfo = new IpfsHomeNodeInfo(id, version);
        logger.info("Ipfs home node: {}", ipfsHomeNodeInfo);

        return ipfsHomeNodeInfo;
    }

    public List<IpfsPeerInfo> getPeers() {
        awaitInit();

        List<IpfsPeerInfo> peers = new ArrayList<IpfsPeerInfo>();
        List<Peer> swarmPeers = null;

        try {
            swarmPeers = ipfs.swarm.peers();
        } catch (IOException ioe) {
            logger.error("getPeers ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("getPeers exception: {}", e);
            if (isDaemonDisconnected(e)) {
                onIpfsDaemonDisconnected();
                return getPeers();
            }
        }

        if (swarmPeers != null && swarmPeers.size() > 0) {
            for (Peer p : swarmPeers) {
                peers.add(new IpfsPeerInfo(p));
            }
        }

        return peers;
    }

    private void awaitInit() {
        initLock.lock();
        try {
            if(!initDone) {
                init.awaitUninterruptibly();
            }
        } finally {
            initLock.unlock();
        }
    }
    /**
     * Sends all pending txs from wallet to new active peers
     */
    private void newTxDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Transaction tx = null;
            try {
                tx = newTransactions.take();
                String txPayload = new NewTxMessage(tx).toJsonString();
                ipfs.pubsub.pub(Topic.getTransactionId(HOME_NODE_ID), txPayload);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (tx != null) {
                    logger.error("Error publishing transaction {}: ",  e);
                } else {
                    logger.error("null tx error when publishing transaction {}", e);
                }
                if (isDaemonDisconnected(e)) {
                    onIpfsDaemonDisconnected();
                    sendTransaction(tx);
                } else {

                }
            }
        }
    }

    /**
     * TODO: 10/24/19
     * Sends all new blocks forged to peer when IDB topic ready.
     */
    private void newBlockDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Block block = null;
            try {
                block = newBlocks.take();
                String blockPayload = new NewBlockMessage(block.getNumber(),block.getCumulativeDifficulty(),block).toJsonString();
                //ipfs.pubsub.pub(Topic.getBlockId(HOME_NODE_ID),blockPayload);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (block != null) {
                    logger.error("Error publishing block {}: ",  e);
                } else {
                    logger.error("null block error when publishing block {}", e);
                }
                if (isDaemonDisconnected(e)) {
                    onIpfsDaemonDisconnected();
                    sendNewBlock(block);
                } else {

                }
            }
        }
    }
}
