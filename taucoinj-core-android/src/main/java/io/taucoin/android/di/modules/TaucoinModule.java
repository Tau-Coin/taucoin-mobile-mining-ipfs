package io.taucoin.android.di.modules;

import android.content.Context;

import io.taucoin.android.datasource.LevelDbDataSource;
import io.taucoin.android.datasource.mmkv.MmkvDataSource;
import io.taucoin.android.db.AccountStateDatabaseImpl;
import io.taucoin.android.db.MMKVIndexedBlockStore;
import io.taucoin.android.debug.TauMobileRefWatcher;
import io.taucoin.android.manager.BlockLoader;
import io.taucoin.android.settings.TaucoinSettings;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.Account;
import io.taucoin.core.Blockchain;
import io.taucoin.core.BlockchainImpl;
import io.taucoin.core.PendingState;
import io.taucoin.core.PendingStateImpl;
import io.taucoin.core.Repository;
import io.taucoin.datasource.HashMapDB;
import io.taucoin.datasource.KeyValueDataSource;
import io.taucoin.datasource.mapdb.MapDBFactory;
import io.taucoin.datasource.mapdb.MapDBFactoryImpl;
import io.taucoin.db.BlockStore;
import io.taucoin.db.IndexedBlockStore;
import io.taucoin.db.MemoryIndexedBlockStore;
import io.taucoin.db.RepositoryImpl;
import io.taucoin.db.state.StateLoader;
import io.taucoin.debug.RefWatcher;
import io.taucoin.facade.IpfsAPI;
import io.taucoin.facade.Taucoin;
import io.taucoin.forge.BlockForger;
import io.taucoin.http.client.ClientsPool;
import io.taucoin.http.client.HttpClient;
import io.taucoin.http.client.HttpClientInitializer;
import io.taucoin.http.ConnectionManager;
import io.taucoin.http.discovery.PeersManager;
import io.taucoin.http.RequestQueue;
import io.taucoin.http.tau.codec.TauMessageCodec;
import io.taucoin.http.tau.handler.TauHandler;
import io.taucoin.ipfs.IpfsAPIRPCImpl;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.manager.WorldManager;
import io.taucoin.sync2.SyncManager;
import io.taucoin.sync2.SyncQueue;
import io.taucoin.sync2.ChainInfoManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import org.mapdb.DB;
import org.mapdb.Serializer;

import static io.taucoin.db.IndexedBlockStore.BLOCK_INFO_SERIALIZER;
import static io.taucoin.config.SystemProperties.CONFIG;

@Module
public class TaucoinModule {

    private Context context;

    boolean storeAllBlocks;
    static WorldManager worldManager = null;
    static Taucoin taucoin = null;

    static BlockStore sBlockStore = null;
    static boolean isStarted = false;

    public TaucoinModule(Context context) {

        this.context = context;
        this.storeAllBlocks = false;
        isStarted = true;
    }

    public TaucoinModule(Context context,boolean storeAllBlocks) {

        this.context = context;
        this.storeAllBlocks = storeAllBlocks;
        isStarted = true;
    }

    @Provides
    @Singleton
    WorldManager provideWorldManager(TaucoinListener listener, Blockchain blockchain, Repository repository,
            BlockStore blockStore, SyncManager syncManager, PendingState pendingState,
            StateLoader stateLoader, IpfsAPI ipfsAPI,
            RefWatcher refWatcher) {

        return new WorldManager(listener, blockchain, repository, blockStore, syncManager,
                pendingState, stateLoader, ipfsAPI, refWatcher);
    }

    @Provides
    @Singleton
    Taucoin provideTaucoin(WorldManager worldManager,
                             io.taucoin.manager.BlockLoader blockLoader, PendingState pendingState,
                             BlockForger blockForger,
                             IpfsAPI ipfsAPI, RefWatcher refWatcher) {
        return new io.taucoin.android.Taucoin(worldManager, blockLoader, pendingState, blockForger,
                ipfsAPI, refWatcher);
    }

    @Provides
    @Singleton
    TaucoinSettings provideTaucoinSettings(WorldManager worldManager) {
        return new TaucoinSettings(this.context, worldManager);
    }

    @Provides
    @Singleton
    io.taucoin.core.Blockchain provideBlockchain(BlockStore blockStore, io.taucoin.core.Repository repository,
            PendingState pendingState, TaucoinListener listener, ChainInfoManager chainInfoManager,
            RefWatcher refWatcher) {
        return new BlockchainImpl(blockStore, repository, pendingState, listener,
                chainInfoManager, refWatcher);
    }

    @Provides
    @Singleton
    BlockStore provideBlockStore(MapDBFactory mapDBFactory) {
        //OrmLiteBlockStoreDatabase database = OrmLiteBlockStoreDatabase.getHelper(context);
        //return new InMemoryBlockStore(database, storeAllBlocks);
/*
        if (sBlockStore != null) {
            return sBlockStore;
        }

        String database = CONFIG.databaseDir();

        String blocksIndexFile = database + "/blocks/index";
        File dbFile = new File(blocksIndexFile);
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        DB indexDB = mapDBFactory.createDB("blocks/index");

        Map<Long, List<IndexedBlockStore.BlockInfo>> indexMap = indexDB.hashMapCreate("index")
                .keySerializer(Serializer.LONG)
                .valueSerializer(BLOCK_INFO_SERIALIZER)
                .counterEnable()
                .makeOrGet();

        KeyValueDataSource blocksDB = new io.taucoin.android.datasource.LevelDbDataSource("blocks");
        //KeyValueDataSource blocksDB = new io.taucoin.android.datasource.RocksDbDataSource("blocks");
        blocksDB.init();

        IndexedBlockStore cache = new IndexedBlockStore();
        cache.init(new HashMap<Long, List<IndexedBlockStore.BlockInfo>>(), new HashMapDB(), null, null);

        IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
        indexedBlockStore.init(indexMap, blocksDB, cache, indexDB);
        sBlockStore = indexedBlockStore;

        return sBlockStore;
*/
        //return new MemoryIndexedBlockStore();
        return new MMKVIndexedBlockStore();
    }

    @Provides
    @Singleton
    StateLoader proviStateLoader(BlockStore blockStore, Repository repository,
            TaucoinListener listener) {
        return new StateLoader(blockStore, repository, listener);
    }


    @Provides
    @Singleton
    Repository provideRepository() {
        //LevelDbDataSource stateDS = new LevelDbDataSource();
        MmkvDataSource stateDS = new MmkvDataSource();
        //RocksDbDataSource stateDS = new RocksDbDataSource();
        //AccountStateDatabaseImpl accountStateDb = new AccountStateDatabaseImpl(stateDS);
        return new RepositoryImpl(stateDS);
    }

    @Provides
    @Singleton
    SyncManager provideSyncManager(Blockchain blockchain, SyncQueue queue,
            TaucoinListener taucoinListener, ChainInfoManager chainInfoManager,
            ConnectionManager connectionManager) {
        return new SyncManager(blockchain, queue, taucoinListener,
                chainInfoManager, connectionManager);
    }

    @Provides
    @Singleton
    SyncQueue provideSyncQueue(Blockchain blockchain, MapDBFactory mapDBFactory) {
        return new SyncQueue(blockchain, mapDBFactory);
    }

    @Provides
    @Singleton
    TaucoinListener provideTaucoinListener() {
        return new CompositeTaucoinListener();
    }

    @Provides
    @Singleton
    MapDBFactory provideMapDBFactory() {
        return new MapDBFactoryImpl();
    }


    @Provides
    @Singleton
    BlockLoader provideBlockLoader(Blockchain blockchain) {
        return new BlockLoader(blockchain);
    }

    @Provides
    @Singleton
    PendingState providePendingState(TaucoinListener listener, Repository repository,BlockStore blockStore) {
        return new PendingStateImpl(listener, repository,blockStore);
    }

    @Provides
    Account provideAccount(Repository repository) {
        return new Account(repository);
    }

    @Provides
    String provideRemoteId() {
        return SystemProperties.CONFIG.peerActive().get(0).getHexId();
    }

    @Provides
    @Singleton
    Context provideContext() { return context; }

    @Provides
    @Singleton
    BlockForger provideBlockForger(ChainInfoManager chainInfoManager) {
        return new BlockForger(chainInfoManager);
    }

    @Provides
    RequestQueue provideRequestQueue(TaucoinListener listener) {
        return new RequestQueue(listener);
    }

    @Provides
    @Singleton
    ClientsPool provideClientsPool(Provider<HttpClient> clientProvider,
            Provider<RequestQueue> queueProvider) {
        return new ClientsPool(clientProvider, queueProvider);
    }

    @Provides
    HttpClient provideHttpClient(TaucoinListener listener,
            Provider<HttpClientInitializer> provider, PeersManager peersManager) {
        return new HttpClient(listener, provider, peersManager);
    }

    @Provides
    HttpClientInitializer provideHttpClientInitializer(
            Provider<TauMessageCodec> messageCodecProvider,
            Provider<TauHandler> handlerProvider) {
        return new HttpClientInitializer(messageCodecProvider,
                handlerProvider);
    }

    @Provides
    TauMessageCodec provideTauMessageCodec(TaucoinListener listener) {
        return new TauMessageCodec(listener);
    }

    @Provides
    TauHandler provideTauHandler(Blockchain blockchain, BlockStore blockstore,
            SyncManager syncManager, SyncQueue queue, PendingState pendingState,
            TaucoinListener tauListener) {
        return new TauHandler(blockchain, blockstore, syncManager, queue, pendingState,
                tauListener);
    }

    @Provides
    @Singleton
    PeersManager providePeersManager() {
        return new PeersManager();
    }

    @Provides
    @Singleton
    ChainInfoManager provideChainInfoManager() {
        return new ChainInfoManager();
    }

    @Provides
    @Singleton
    ConnectionManager provideConnectionManager() {
        return new ConnectionManager();
    }

    @Provides
    @Singleton
    IpfsAPI provideIpfsAPI(Blockchain blockchain, SyncQueue queue, PendingState pendingState,
                           BlockForger blockForger, TaucoinListener tauListener) {
        return new IpfsAPIRPCImpl(blockchain, queue, pendingState, blockForger, tauListener);
    }

    @Provides
    @Singleton
    RefWatcher provideRefWatcher() {
        return new TauMobileRefWatcher();
    }

    public static void close() {
        if (!isStarted) {
            return;
        }

        worldManager = null;
        taucoin = null;
        if (sBlockStore != null) {
            sBlockStore.close();
            sBlockStore = null;
        }
        isStarted = false;
    }
}
