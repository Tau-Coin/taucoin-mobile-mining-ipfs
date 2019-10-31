package io.taucoin.manager;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.db.state.StateLoader;
import io.taucoin.debug.RefWatcher;
import io.taucoin.facade.IpfsAPI;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.net.client.PeerClient;
import io.taucoin.sync2.SyncManager;
import io.taucoin.sync2.PoolSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * WorldManager is a singleton containing references to different parts of the system.
 *
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
@Singleton
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private TaucoinListener listener;

    private Blockchain blockchain;

    private Repository repository;

    private PeerClient activePeer;

    private BlockStore blockStore;

    private SyncManager syncManager;

    private PendingState pendingState;

    private PoolSynchronizer poolSynchronizer;

    private StateLoader stateLoader;

    private RefWatcher refWatcher;

    private IpfsAPI ipfsAPI;

    private volatile boolean isSyncRunning = false;

    private volatile boolean isSyncDownloading = false;

    SystemProperties config = SystemProperties.CONFIG;

    @Inject
    public WorldManager(TaucoinListener listener, Blockchain blockchain, Repository repository
                        , BlockStore blockStore, SyncManager syncManager
                        , PendingState pendingState
                        , PoolSynchronizer poolSynchronizer, StateLoader stateLoader
                        , IpfsAPI ipfsAPI, RefWatcher refWatcher) {
        logger.info("World manager instantiated");
        this.listener = listener;
        this.blockchain = blockchain;
        this.repository = repository;
        this.blockStore = blockStore;
        this.syncManager = syncManager;
        this.pendingState = pendingState;
        this.poolSynchronizer = poolSynchronizer;
        this.stateLoader = stateLoader;
        this.ipfsAPI = ipfsAPI;
        this.refWatcher = refWatcher;
    }

    public void init() {
        loadBlockchain();
        logger.info("chain size is {}",blockchain.getSize());
    }

    public void initSync() {

        // must be initialized after blockchain is loaded
        pendingState.init();
        syncManager.init();
    }

    public void addListener(TaucoinListener listener) {
        logger.info("Ethereum listener added");
        ((CompositeTaucoinListener) this.listener).addListener(listener);
    }

    public void startPeerDiscovery() {
        // TODO: start peer discovery
    }

    public void stopPeerDiscovery() {
        // TODO: stop peer discovery
    }

    public void startSync() {
        if (isSyncRunning) {
            return;
        }
        isSyncRunning = true;

        // start sync module
        startDownload();
        syncManager.start();
    }

    public void stopSync() {
        if (!isSyncRunning) {
            return;
        }
        isSyncRunning = false;

        stopDownload();
        syncManager.stop();
    }

    public boolean isSync() {
        return isSyncRunning;
    }

    public void startDownload() {
        if (isSyncDownloading) {
            return;
        }
        ipfsAPI.startDownload();
        isSyncDownloading = true;
    }

    public void stopDownload() {
        if (!isSyncDownloading) {
            return;
        }
        ipfsAPI.stopDownload();
        isSyncDownloading = false;
    }

    public TaucoinListener getListener() {
        return listener;
    }

    public io.taucoin.facade.Repository getRepository() {
        return (io.taucoin.facade.Repository)repository;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public void setActivePeer(PeerClient peer) {
        this.activePeer = peer;
    }

    public PeerClient getActivePeer() {
        return activePeer;
    }

    public BlockStore getBlockStore() {
        return blockStore;
    }

    public PendingState getPendingState() {
        return pendingState;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public void loadBlockchain() {

        if (!config.databaseReset())
            blockStore.load();

        Block bestBlock = blockStore.getBestBlock();
        if (bestBlock == null) {

            // Try loading states tag.
            // If states loaded successfully, set best block again.
            if (stateLoader.loadStatesTag()) {
                Block tagBestBlock = blockStore.getBestBlock();
                if (tagBestBlock != null) {
                    logger.info("Tag best block {} {}", tagBestBlock.getNumber(),
                            tagBestBlock.getShortHash());
                    blockchain.setBestBlock(tagBestBlock);
                    blockchain.setTotalDifficulty(tagBestBlock.getCumulativeDifficulty());
                }
            } else {
                logger.info("DB is empty - adding Genesis");

                Genesis genesis = (Genesis)Genesis.getInstance(config);
                long startTime0 = System.nanoTime();
                for (ByteArrayWrapper key : genesis.getPremine().keySet()) {
                    repository.createAccount(key.getData());
                    BigInteger power = repository.increaseforgePower(key.getData());
                    logger.info("address : {} forge power : {}",Hex.toHexString(key.getData()),power);
                    repository.addBalance(key.getData(), genesis.getPremine().get(key).getBalance());
                }
                long endTime0 = System.nanoTime();
                logger.info("Import accounts time: {}",((endTime0 - startTime0) / 1000000));
                logger.info("genesis block hash: {}",Hex.toHexString(Genesis.getInstance(config).getHash()));
                logger.info("genesis block cid: {}", Genesis.getInstance(config).getCid().toString());
                Object object= blockStore.getClass();
                logger.info("blockStore class : {}",((Class) object).getName());

                HashPair genesisHashPair = new HashPair(Genesis.getInstance(config).getNumber(),
                        Genesis.getInstance(config).getCid(), Genesis.getInstance(config).getCid());
                logger.info("genesis hash pair cid: {}", genesisHashPair.getCid().toString());

                blockStore.saveBlockHashPair(Genesis.getInstance(config), genesisHashPair, Genesis.getInstance(config).getCumulativeDifficulty(), true);
                blockStore.flush();
                blockchain.setBestBlock(Genesis.getInstance(config));
                blockchain.setTotalDifficulty(Genesis.getInstance(config).getCumulativeDifficulty());

                listener.onBlock(Genesis.getInstance(config));

                logger.info("Genesis block loaded");
            }
        } else {

            // Note: 'BlockchainImpl' maybe undo transactions which call blockchain.getSize().
            // So we have to set best block first of all.
            blockchain.setBestBlock(bestBlock);

            // First of all, check database sanity
            if (blockchain.checkSanity()) {
                // Here best block changed, so set it again.
                bestBlock = blockStore.getBestBlock();
                blockchain.setBestBlock(bestBlock);
            }

            BigInteger totalDifficulty = blockStore.getTotalDifficulty();
            blockchain.setTotalDifficulty(totalDifficulty);

            logger.info("*** Loaded up to block [{}] totalDifficulty [{}] with best block hash [{}]",
                    blockchain.getBestBlock().getNumber(),
                    blockchain.getTotalDifficulty().toString(),
                    Hex.toHexString(blockchain.getBestBlock().getHash()));

            final long bestNumber = bestBlock.getNumber();
            logger.info("Blockchain best number {}", bestNumber);
            if (bestNumber > config.blockStoreCapability()) {
                EventDispatchThread.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        blockStore.delChainBlocksWithNumberLessThan(
                                bestNumber - config.blockStoreCapability());
                    }
                });
            }
        }

/* todo: return it when there is no state conflicts on the chain
        boolean dbValid = this.repository.getWorldState().validate() || bestBlock.isGenesis();
        if (!dbValid){
            logger.error("The DB is not valid for that blockchain");
            System.exit(-1); //  todo: reset the repository and blockchain
        }
*/
    }

    public void close() {
        stopPeerDiscovery();
        stopSync();

        poolSynchronizer.close();
        syncManager.close();
        repository.close();
        blockchain.close();

        refWatcher.watch(listener);
        refWatcher.watch(blockchain);
        refWatcher.watch(repository);
        refWatcher.watch(activePeer);
        refWatcher.watch(blockStore);
        refWatcher.watch(syncManager);
        refWatcher.watch(pendingState);
        refWatcher.watch(poolSynchronizer);
    }
}
