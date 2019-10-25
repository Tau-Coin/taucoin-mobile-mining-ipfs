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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Thread.sleep;

@Singleton
public class IpfsAPIRPCImpl implements IpfsAPI {

    private static final Logger logger = LoggerFactory.getLogger("ipfsapi");

    private Blockchain blockchain;

    private SyncQueue queue;

    private PendingState pendingState;

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

    private Thread blockChainSubThread;
    private Thread transactionListSubThread;
    private Thread blockChainProcessThread;
    private Thread transactionListProcessThread;
    ConcurrentLinkedQueue<Object> hashlQueue = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Object> hashcQueue = new ConcurrentLinkedQueue<>();

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

        try {
            sleep(2000);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

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

        this.blockChainProcessThread = new Thread(new Runnable() {
            @Override
            public void run() {
                blockChainProcess();
            }
        },"blockChainProcessThread");
        this.blockChainProcessThread.start();

        this.transactionListProcessThread = new Thread(new Runnable() {
            @Override
            public void run() {
                transactionListProcess();
            }
        },"transactionListProcessThread");
        this.transactionListProcessThread.start();

        blockChainSubThread = new Thread(() -> {
            try {
                ipfs.pubsub.sub("idc", hashcQueue::add, x -> logger.error(x.getMessage(), x));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        blockChainSubThread.start();

        transactionListSubThread = new Thread(() -> {
            try {
                ipfs.pubsub.sub("idl", hashlQueue::add, x -> logger.error(x.getMessage(), x));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        transactionListSubThread.start();
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

    private void transactionListProcess(){
        while (true) {
            if (hashlQueue.size() > 0) {
                Map msg = (Map) hashlQueue.poll();
                if (msg.size() > 0) {
//                    String from = Base58.encode(Base64.getDecoder().decode(msg.get("from").toString()));
//                    String topicId = msg.get("topicIDs").toString();
//                    String seqno = new BigInteger(Base64.getDecoder().decode(msg.get("seqno").toString())).toString();
                    String data = new String(Base64.getDecoder().decode(msg.get("data").toString()));
                    syncTransactions(data);
                }
                hashlQueue.clear();
            }

            //sleep 1s
            try {
                sleep(1000);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void syncTransactions(String txListCid) {
        try {
            Cid cid = Cid.decode(txListCid);
            logger.info("HASHL:{}", cid.toString());
            Multihash multihash = new Multihash(cid);
            byte[] rlpEncoded = ipfs.block.get(multihash);
            TransactionCidList transactionCidList = new TransactionCidList(rlpEncoded);
            Multihash txMultihash;
            Transaction tx;
            HashSet<Transaction> txs = new HashSet<Transaction>();
            for (byte[] txCid : transactionCidList.getTxCidList()) {
                txMultihash = new Multihash(txCid);
                byte[] txRlp = ipfs.block.get(txMultihash);
                tx = new Transaction(txRlp);
                txs.add(tx);
            }
            pendingState.addWireTransactions(txs);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void blockChainProcess() {
        while (true) {
            if (hashcQueue.size() > 0) {
                Map msg = (Map) hashcQueue.poll();
                if (msg.size() > 0) {
//                    String from = Base58.encode(Base64.getDecoder().decode(msg.get("from").toString()));
//                    String topicId = msg.get("topicIDs").toString();
//                    String seqno = new BigInteger(Base64.getDecoder().decode(msg.get("seqno").toString())).toString();
                    String data = new String(Base64.getDecoder().decode(msg.get("data").toString()));
                    syncBlockChain(data);
                }
                hashcQueue.clear();
            }

            //sleep 1s
            try {
                sleep(1000);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private synchronized void syncBlockChain(String hashChainCid) {
        try {
            Cid cid = Cid.decode(hashChainCid);
            logger.info("HASHC:{}", cid.toString());
            Multihash multihash = new Multihash(cid);
            byte[] rlpEncoded = ipfs.block.get(multihash);
            HashChain hashChain = new HashChain(rlpEncoded);
            List<byte[]> cidList = hashChain.getHashPairCidList();

            //simple mode
            Cid latestHashPairCid = Cid.cast(cidList.get(0));
            logger.info("first cid in cid list:{}", latestHashPairCid.toString());
            multihash = new Multihash(latestHashPairCid);
            byte[] latestHashPairRlpEncoded = ipfs.block.get(multihash);
            HashPair hashPair = new HashPair(latestHashPairRlpEncoded);
            Block bestBlock = blockchain.getBestBlock();
            long currentNumber = bestBlock.getNumber();
            logger.info("current block number:{}, hash:{}, cid:{}",
                    currentNumber, Hex.toHexString(bestBlock.getHash()), bestBlock.getCid().toString());
            List<HashPair> hashPairList = new ArrayList<>();
            byte[] hashPairRlp;
            byte[] rawBlock;
            while (hashPair.getNumber() > currentNumber) {
                //test block tic
//                Cid blockCid = hashPair.getBlockCid();
//                rawBlock = ipfs.block.get(blockCid);
//                Block block = new Block(rawBlock, true);
//                String blockCidFromHashPair = blockCid.toString();
//                String blockCidFromBlock = block.getCid().toString();
//                logger.info("blockCidFromHashPair:{}, blockCidFromBlock:{}",
//                        blockCidFromHashPair, blockCidFromBlock);
//                if (blockCidFromHashPair.compareTo(blockCidFromBlock) != 0) {
//                    logger.error("Block hash:{}", Hex.toHexString(block.getHash()));
//                    break;
//                }
                //sync hash pair list
                logger.info("hash pair number:{}, cid:{}, block cid:{}, previous hash pair cid:{}",
                        hashPair.getNumber(),
                        hashPair.getCid().toString(),
                        hashPair.getBlockCid().toString(),
                        hashPair.getPreviousHashPairCid().toString());
                hashPairList.add(hashPair);
                cid = hashPair.getPreviousHashPairCid();
                if (cid.toString().compareTo(Constants.GENESIS_HASHPAIR_CID) == 0) {
                    logger.info("...sync to genesis hash pair...");
                    break;
                }
                multihash = hashPair.getPreviousHashPairCid();
                hashPairRlp = ipfs.block.get(multihash);
                hashPair = new HashPair(hashPairRlp);
            }

            //sync block from hash pair list
            logger.info("sync start from number:{}", hashPair.getNumber());
            logger.info("Hash Pair number:{}, cid:{}, block cid:{}, previous hash pair cid:{}",
                    hashPair.getNumber(),
                    hashPair.getCid().toString(),
                    hashPair.getBlockCid().toString(),
                    hashPair.getPreviousHashPairCid().toString());

            byte[] blockNetBytes = ipfs.block.get(hashPair.getBlockCid());
            logger.info("block form net number:{}, string:{}",
                    hashPair.getNumber(), Hex.toHexString(blockNetBytes));
            logger.info("block form local number:{}, string:{}",
                    bestBlock.getNumber(), Hex.toHexString(bestBlock.getEncodedMsg()));

            logger.info("Best Block number:{}, hash:{}, block cid:{}",
                    bestBlock.getNumber(),
                    Hex.toHexString(bestBlock.getHash()),
                    bestBlock.getCid().toString());
            logger.info("In the same height, Cid from hash pair:{}, Cid from best block:{}",
                    hashPair.getBlockCid().toString(),
                    bestBlock.getCid().toString());
            if (hashPair.getPreviousHashPairCid().toString().
                    compareTo(Constants.GENESIS_HASHPAIR_CID) == 0 ||
                    hashPair.getBlockCid().toString().
                            compareTo(bestBlock.getCid().toString()) == 0) {
                int size = hashPairList.size();
                logger.info("There are {} blocks to sync", size);
                for (int i = size - 1; i >= 0; i--) {
                    hashPair = hashPairList.get(i);
                    logger.info("block cid:{}", hashPair.getBlockCid().toString());
                    multihash = hashPair.getBlockCid();
//                    while (!blockQueue.isEmpty()) {
//                        sleep(10);
//                    }
                    byte[] blockRlp = ipfs.block.get(multihash);
                    Block block = new Block(blockRlp, true);
                    logger.info("sync block hash:{}", Hex.toHexString(block.getHash()));
//                    BlockWrapper wrapper = new BlockWrapper(block, new byte[1], "127.0.0.1");
//                    blockQueue.add(wrapper);
                    List<Block> list = new ArrayList<>(1);
                    block.setNumber(hashPair.getNumber());
                    list.add(block);
                    queue.addList(list, new byte[0]);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
