package io.taucoin.ipfs;

import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.taucoin.config.Constants;
import io.taucoin.core.*;
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
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IpfsAPIRPCImpl implements IpfsAPI {

    private static final Logger logger = LoggerFactory.getLogger("ipfsapi");

    private Blockchain blockchain;

    private SyncQueue queue;

    private PendingState pendingState;

    private static final long RECONNECT_IPFS_DAEMON_DURATION = 3000;

    private static final int HASH_PAIR_CID_INTERVAL = (144 * 3);

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    // temp home node id, just for test.
    private static final String HOME_NODE_ID = "id";

    private TaucoinListener tauListener;

    protected IpfsHomeNodeInfo ipfsHomeNodeInfo = null;

    private IPFS ipfs;

    private boolean initDone = false;

    private boolean isSyncDone = false;

    // 'initLock' protects race for 'ipfs' & 'initDone'.
    private final ReentrantLock initLock = new ReentrantLock();
    private final Condition init = initLock.newCondition();

    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);

    //0:initial 1:synced
    private int counter = 0;

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

    private CircularFifoQueue<Object> hashcCircularFifoQueue = new CircularFifoQueue<>(1);
    private CircularFifoQueue<Object> hashlCircularFifoQueue = new CircularFifoQueue<>(1);

    private Runnable blockChainProcessSubscribe = new Runnable() {
        @Override
        public void run() {
            blockChainProcess();
        }
    };

    private Runnable transactionListProcessSubscribe = new Runnable() {
        @Override
        public void run() {
            transactionListProcess();
        }
    };

    private Runnable blockChainSubscribe = new Runnable() {
        @Override
        public void run() {
            try {
                logger.info("Start to sub from topic:[idc].");
                ipfs.pubsub.sub("idc", hashcCircularFifoQueue::add, x -> logger.error(x.getMessage(), x));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
//                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    };

    private Runnable transactionListSubscribe = new Runnable() {
        @Override
        public void run() {
//            while (!Thread.currentThread().isInterrupted()) {
//            }
            try {
                logger.info("Start to sub from topic:[idl].");
                ipfs.pubsub.sub("idl", hashlCircularFifoQueue::add, x -> logger.error(x.getMessage(), x));
            } catch (NullPointerException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (ConnectException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    };

    private Thread blockChainProcessThread = null;
    private Thread blockChainSubThread = null;
    private Thread transactionListProcessThread = null;
    private Thread transactionListSubThread = null;

    /**
     * Queue with new blocks forged.
     */
    private BlockingQueue<Block> newBlocks = new LinkedBlockingQueue<>();

    /**
     * Queue with new transactions.
     */
    private BlockingQueue<Transaction> newTransactions = new LinkedBlockingQueue<>();

    @Inject
    public IpfsAPIRPCImpl(Blockchain blockchain, SyncQueue queue, PendingState pendingState, TaucoinListener tauListener) {
        this.blockchain = blockchain;
        this.queue = queue;
        this.pendingState = pendingState;
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

    public void startDownload() {
//        try {
            if (null == blockChainProcessThread || !blockChainProcessThread.isAlive()) {
                blockChainProcessThread = new Thread(blockChainProcessSubscribe, "blockChainProcessThread");
                blockChainProcessThread.start();
            }
            if (null == blockChainSubThread || !blockChainSubThread.isAlive()) {
                blockChainSubThread = new Thread(blockChainSubscribe, "blockChainSubThread");
                blockChainSubThread.start();
            }
//        } catch (Exception e) {
//            if (isDaemonDisconnected(e)) {
//                onIpfsDaemonDisconnected();
//                try {
//                    Thread.sleep(5000);
//                } catch (Exception ex) {
//                    logger.error(ex.getMessage(), e);
//                }
//                startDownload();
//            }
//        }
    }

    public void stopDownload() {
        if (null != transactionListProcessThread) {
            transactionListProcessThread.interrupt();
        }
        if (null != transactionListSubThread) {
            transactionListSubThread.interrupt();
        }
        if (null != blockChainProcessThread) {
            blockChainProcessThread.interrupt();
        }
        if (null != blockChainSubThread) {
            blockChainSubThread.interrupt();
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
            if (message != null && message.contains("Couldn't connect to IPFS daemon")) {
                return true;
            }
        }

        return false;
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
                ipfs.pubsub.pub(Topic.getBlockId(HOME_NODE_ID),blockPayload);
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

    private void transactionListProcess(){
        while (!Thread.currentThread().isInterrupted()) {
            if (hashlCircularFifoQueue.size() > 0) {
                Map msg = (Map) hashlCircularFifoQueue.poll();
                if (msg.size() > 0) {
//                    String from = Base58.encode(Base64.getDecoder().decode(msg.get("from").toString()));
//                    String topicId = msg.get("topicIDs").toString();
//                    String seqno = new BigInteger(Base64.getDecoder().decode(msg.get("seqno").toString())).toString();
                    String data = new String(Base64.getDecoder().decode(msg.get("data").toString()));
                    syncTransactions(data);
                }
            }

            //sleep 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("-----interrupt-----");
                logger.info(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void syncTransactions(String txListCid) {
        try {
            Cid cid = Cid.decode(txListCid);
            logger.info("HASHL:{}", cid.toString());
            byte[] rlpEncoded = ipfs.block.get(cid);
            TransactionCidList transactionCidList = new TransactionCidList(rlpEncoded);
            Multihash txMultihash;
            Transaction tx;
            for (byte[] txCid : transactionCidList.getTxCidList()) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                txMultihash = new Multihash(txCid);
                byte[] txRlp = ipfs.block.get(txMultihash);
                HashSet<Transaction> txs = new HashSet<Transaction>();
                tx = new Transaction(txRlp);
                txs.add(tx);
                pendingState.addWireTransactions(txs);
            }
        } catch (ClosedByInterruptException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void blockChainProcess() {
        while (!Thread.currentThread().isInterrupted()) {
            if (hashcCircularFifoQueue.size() > 0) {
                Map msg = (Map) hashcCircularFifoQueue.poll();
                if (msg.size() > 0) {
//                    String from = Base58.encode(Base64.getDecoder().decode(msg.get("from").toString()));
//                    String topicId = msg.get("topicIDs").toString();
//                    String seqno = new BigInteger(Base64.getDecoder().decode(msg.get("seqno").toString())).toString();
                    String data = new String(Base64.getDecoder().decode(msg.get("data").toString()));
                    syncBlockChain(data);
                }
            }

            //sleep 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("-----interrupt-----");
                logger.info(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void syncBlockChain(String hashChainCid) {
        try {
            //get hash pair cid list
            Cid cid = Cid.decode(hashChainCid);
            logger.info("HASHC :{}", cid.toString());
            Multihash multihash = new Multihash(cid);
            byte[] rlpEncoded = ipfs.block.get(multihash);
            HashChain hashChain = new HashChain(rlpEncoded);
            List<byte[]> cidList = hashChain.getHashPairCidList();

            //simple mode
            //get remote chain info
            Cid latestHashPairCid = Cid.cast(cidList.get(cidList.size() - 1));
            logger.info("cid list size is {}, latest hash pair cid is :{}",
                    cidList.size(), latestHashPairCid.toString());
            multihash = new Multihash(latestHashPairCid);
            byte[] latestHashPairRlpEncoded = ipfs.block.get(multihash);
            HashPair latestHashPair = new HashPair(latestHashPairRlpEncoded);
            byte[] latestBlockRlp = ipfs.block.get(latestHashPair.getBlockCid());
            Block latestBlock = new Block(latestBlockRlp, true);
            //get best block info
            Block bestBlock = blockchain.getBestBlock();
            if (blockchain.isBlockExist(latestBlock.getHash()) &&
                    !Arrays.equals(bestBlock.getHash(), latestBlock.getHash())) {
                logger.info("Remote chain is not best chain. Best block from remote and local are not equal.");
                logger.info("Remote best block hash [{}], local best block hash [{}].",
                        Hex.toHexString(latestBlock.getHash()), Hex.toHexString(bestBlock.getHash()));
                return;
            }
            long currentNumber = bestBlock.getNumber();
            logger.info("current block number:{}, hash:{}, cid:{}",
                    currentNumber, Hex.toHexString(bestBlock.getHash()), bestBlock.getCid().toString());
            //compare local height with remote height
            while (latestHashPair.getNumber() > currentNumber) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                int index = (int) currentNumber / (HASH_PAIR_CID_INTERVAL);
                if (index > cidList.size() - 1) {
                    logger.info("index {} is out of cid list size {}", index, cidList.size());
                    break;
                }
                //syncing
                counter = 1;
                Cid syncHashPairCid = Cid.cast(cidList.get(index));
                logger.info("cid index[{}] in cid list to sync is :{}", index, syncHashPairCid.toString());
                multihash = new Multihash(syncHashPairCid);
                byte[] syncHashPairRlpEncoded = ipfs.block.get(multihash);
                HashPair hashPair = new HashPair(syncHashPairRlpEncoded);
                List<HashPair> hashPairList = new ArrayList<>();
                byte[] hashPairRlp;
                //compare best block number with hash pair number to decide if need to sync
                while (hashPair.getNumber() > currentNumber) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    //sync hash pair list
                    logger.info("hash pair number:{}, cid:{}, block cid:{}, previous hash pair cid:{}",
                            hashPair.getNumber(),
                            hashPair.getCid().toString(),
                            hashPair.getBlockCid().toString(),
                            hashPair.getPreviousHashPairCid().toString());
                    hashPairList.add(hashPair);
//                    cid = hashPair.getPreviousHashPairCid();
//                    if (cid.toString().compareTo(Constants.GENESIS_HASHPAIR_CID) == 0) {
//                        logger.info("---------- sync to genesis hash pair ----------");
//                        break;
//                    }
                    multihash = hashPair.getPreviousHashPairCid();
                    hashPairRlp = ipfs.block.get(multihash);
                    hashPair = new HashPair(hashPairRlp);
                }

                //make sure that block hash(cid in later) from different branch are equal, or hash pair makes one step forward
                if (hashPair.getNumber() > 0) {
                    byte[] blockRemoteBytes = ipfs.block.get(hashPair.getBlockCid());
                    Block blockRemote = new Block(blockRemoteBytes, true);
                    Block blockLocal = bestBlock;
                    //find common fork point
                    while (!Arrays.equals(blockRemote.getHash(), blockLocal.getHash())) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        logger.info("Number :[{}], remote block hash :[{}], local block hash :[{}]",
                                hashPair.getNumber(), Hex.toHexString(blockRemote.getHash()),
                                Hex.toHexString(blockLocal.getHash()));
                        hashPairList.add(hashPair);
                        multihash = hashPair.getPreviousHashPairCid();
                        hashPairRlp = ipfs.block.get(multihash);
                        hashPair = new HashPair(hashPairRlp);

                        blockRemoteBytes = ipfs.block.get(hashPair.getBlockCid());
                        blockRemote = new Block(blockRemoteBytes, true);

                        logger.info("Number :[{}], next remote block hash :[{}], next local block hash :[{}]",
                                hashPair.getNumber(), Hex.toHexString(blockRemote.getHash()),
                                Hex.toHexString(blockLocal.getPreviousHeaderHash()));
                        blockLocal = blockchain.getBlockByHash(blockLocal.getPreviousHeaderHash());
                        if (null == blockLocal) {
                            logger.error("Cannot find common fork point!!!");
                            return;
                        }
                    }
                }

                //sync block from hash pair list
                logger.info("Sync start from hash Pair number:{}, cid:{}, block cid:{}, previous hash pair cid:{}",
                        hashPair.getNumber(),
                        hashPair.getCid().toString(),
                        hashPair.getBlockCid().toString(),
                        hashPair.getPreviousHashPairCid().toString());

//            if (hashPair.getBlockCid().toString().
//                    compareTo(bestBlock.getCid().toString()) == 0 ||
//                    hashPair.getPreviousHashPairCid().toString().
//                    compareTo(Constants.GENESIS_HASHPAIR_CID) == 0) {
                //sync blocks
                int size = hashPairList.size();
                logger.info("There are {} blocks to sync", size);
                for (int i = size - 1; i >= 0; i--) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    hashPair = hashPairList.get(i);
                    if (hashPair.getNumber() <= queue.getBlockqueueMaxNumber()) {
                        logger.info("Block [{}] in queue has exited!", hashPair.getNumber());
                        continue;
                    }
                    logger.info("block cid:{}", hashPair.getBlockCid().toString());
                    multihash = hashPair.getBlockCid();
                    byte[] blockRlp = ipfs.block.get(multihash);
                    Block block = new Block(blockRlp, true);
                    logger.info("sync block hash:{}", Hex.toHexString(block.getHash()));
                    List<Block> list = new ArrayList<>(1);
                    block.setNumber(hashPair.getNumber());
                    list.add(block);
                    while (!queue.isMoreBlocksNeeded()) {
                        logger.info("Block queue is full. Sleep 60s.");
                        Thread.sleep(60000);
                    }
//                    while (queue.getBlockQueueSize() > 0) {
//                        Thread.sleep(10);
//                    }
                    queue.addList(list, new byte[0]);
                }
                //get best block info
                bestBlock = blockchain.getBestBlock();
                currentNumber = bestBlock.getNumber();
                logger.info("Now current block number:{}, hash:{}, cid:{}",
                        currentNumber, Hex.toHexString(bestBlock.getHash()), bestBlock.getCid().toString());
            }

            if (1 == counter) {
                logger.info("syncing...");
                counter = 0;
            } else {
                //sync done
                logger.info("Block chain download is complete.");
                if (queue.isBlocksEmpty()) {
                    logger.info("Block chain verification is complete.");
                    if (null == transactionListProcessThread || !transactionListProcessThread.isAlive()) {
                        transactionListProcessThread = new Thread(transactionListProcessSubscribe, "transactionListProcessThread");
                        logger.info("Start Transaction Verification thread.");
                        transactionListProcessThread.start();
                    }
                    if (null == transactionListSubThread || !transactionListSubThread.isAlive()) {
                        transactionListSubThread = new Thread(transactionListSubscribe, "transactionListSubThread");
                        transactionListSubThread.start();
                    }
                    if (!isSyncDone) {
                        logger.info("Send signal: sync done!!!");
                        isSyncDone = true;
                        tauListener.onSyncDone();
                    }
                }
            }

        } catch (InterruptedException e) {
            logger.info("-----interrupt-----");
            logger.info(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (ClosedByInterruptException e) {
            logger.info("-----interrupt-----");
            logger.info(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
