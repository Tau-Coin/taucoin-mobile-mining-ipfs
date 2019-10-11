package io.taucoin.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;

import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class OrmLiteBlockStoreDatabase extends OrmLiteSqliteOpenHelper implements BlockStoreDatabase {

    private static OrmLiteBlockStoreDatabase instance;

    private static final String DATABASE_NAME = "blockchain.db";
    private static final int DATABASE_VERSION = 1;

    private Dao<BlockVO, Integer> blockDao = null;
    private Dao<BlockTransactionVO, Integer> blockTransactionDao = null;

    protected boolean storeAllBLocks = false;

    public static synchronized OrmLiteBlockStoreDatabase getHelper(Context context)
    {
        if (instance == null)
            instance = new OrmLiteBlockStoreDatabase(context);

        return instance;
    }

    public OrmLiteBlockStoreDatabase(Context context) {
        super(context, SystemProperties.CONFIG.databaseDir()
                + File.separator + DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {

        try {
            Log.i(OrmLiteBlockStoreDatabase.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, BlockVO.class);
            TableUtils.createTable(connectionSource, BlockTransactionVO.class);
        } catch (SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {

        try {
            Log.i(OrmLiteBlockStoreDatabase.class.getName(), "onUpgrade");
            TableUtils.dropTable(connectionSource, BlockVO.class, true);
            TableUtils.dropTable(connectionSource, BlockTransactionVO.class, true);
            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<BlockVO, Integer> getBlockDao() throws SQLException {

        if (blockDao == null) {
            blockDao = getDao(BlockVO.class);
        }
        return blockDao;
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<BlockTransactionVO, Integer> getBlockTransactionDao() throws SQLException {
        if (blockTransactionDao == null) {
            blockTransactionDao = getDao(BlockTransactionVO.class);
        }
        return blockTransactionDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {

        super.close();
        blockDao = null;
    }

    public List<BlockVO> getByNumber(Long number) {

        List<BlockVO> list = new ArrayList<BlockVO>();
        try {
            list = getBlockDao().queryForEq("number", number);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error querying for number", e);
        }

        return list;
    }

    public List<BlockVO> getByHash(byte[] hash) {

        List<BlockVO> list = new ArrayList<BlockVO>();
        try {
            list = getBlockDao().queryForEq("hash", hash);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error querying for hash", e);
        }

        return list;
    }

    public List<byte[]> getHashListByNumberLimit(Long from, Long to) {

        List<byte[]> results = new ArrayList<byte[]>();
        try {
            List<BlockVO> list = new ArrayList<BlockVO>();
            list = getBlockDao().queryBuilder().orderBy("number", false).limit(to - from).where().between("number", from, to).query();
            for (BlockVO block : list) {
                results.add(block.hash);
            }
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error querying for hash list", e);
        }

        return results;
    }

    public void deleteBlocksSince(long number) {

        try {
            DeleteBuilder<BlockVO, Integer> deleteBuilder = getBlockDao().deleteBuilder();
            deleteBuilder.where().gt("number", number);
            deleteBuilder.delete();
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error deleting blocks since", e);
        }
    }

    public void save(BlockVO block) {

        try {
            getBlockDao().create(block);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error saving block", e);
        }
    }

    public BigInteger getTotalDifficultySince(long number) {

        try {
            GenericRawResults<String[]> rawResults = getBlockDao().queryRaw("select sum(cumulativedifficulty) from block where number > " + number);
            List<String[]> results = rawResults.getResults();
            return new BigInteger(results.get(0)[0]);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error getting total difficulty since", e);
        }
        return null;
    }

    public BigInteger getTotalDifficulty() {

        try {
            GenericRawResults<String[]> rawResults = getBlockDao().queryRaw("select sum(cumulativedifficulty) from block");
            List<String[]> results = rawResults.getResults();
            return new BigInteger(results.get(0)[0]);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error getting total difficulty", e);
        }
        return null;
    }

    public long getMaxNumber() {
        Long bestNumber = null;
        try {
            GenericRawResults<String[]> rawResults = getBlockDao().queryRaw("select max(number) from block");
            List<String[]> results = rawResults.getResults();
            if (results.get(0)[0] != null) {
                bestNumber = Long.valueOf(results.get(0)[0]);
            }
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Sql Error getting best block", e);
        } catch (Exception e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error getting best block", e);
        }

        return bestNumber != null ? bestNumber : 0;
    }

    public Block getBestBlock() {

        Long bestNumber = getMaxNumber();
        if (bestNumber == null) {
            return null;
        }
        List result = getByNumber(bestNumber);

        if (result.isEmpty()) return null;
        BlockVO vo = (BlockVO) result.get(0);

        return new Block(vo.rlp);
    }

    public List<Block> getAllBlocks() {

        ArrayList<Block> blocks = new ArrayList<>();
        try {
            for (BlockVO blockVO : getBlockDao()) {
                blocks.add(new Block(blockVO.getRlp()));
            }
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error getting all blocks", e);
        }

        return blocks;
    }

    @Override
    public void reset() {

        deleteBlocksSince(Long.valueOf(0));
    }


    public void save(BlockTransactionVO blockTransactionV0) {

        try {
            getBlockTransactionDao().create(blockTransactionV0);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error saving blockTransaction", e);
        }
    }


    public BlockTransactionVO getTransactionLocation(byte[] transactionHash) {

        List<BlockTransactionVO> list = new ArrayList<>();
        try {
            list = getBlockTransactionDao().queryForEq("transactionHash", transactionHash);
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error querying for transaction hash", e);
        }

        if (list.size() == 0) return null;
        BlockTransactionVO blockTransactionVO = list.get(0);

        return blockTransactionVO;
    }

    public boolean flush(final List<Block> blocks) {

        if (!storeAllBLocks) {
            reset();
        }
        try {
            TransactionManager.callInTransaction(getBlockDao().getConnectionSource(),
                    new Callable<Void>() {
                        public Void call() throws Exception {

                            if (storeAllBLocks) {
                                for (Block block: blocks) {
                                    byte[] blockHash = block.getHash();
                                    BlockVO blockVO = new BlockVO(block.getNumber(), blockHash, block.getEncoded(), block.getCumulativeDifficulty());
                                    save(blockVO);

                                    List<Transaction> transactions = block.getTransactionsList();

                                    int index = 0;
                                    for (Transaction transaction: transactions) {
                                        BlockTransactionVO transactionVO = new BlockTransactionVO(blockHash, transaction.getHash(), index);
                                        save(transactionVO);
                                        index++;
                                    }
                                }
                            } else {
                                int size = blocks.size();
                                int lastIndex = size - 1;
                                for (int i = 0;
                                     i < (size > 1000 ? 1000 : size);
                                     ++i){
                                    Block block = blocks.get(lastIndex - i);
                                    BlockVO blockVO = new BlockVO(block.getNumber(), block.getHash(), block.getEncoded(), block.getCumulativeDifficulty());
                                    save(blockVO);
                                }
                            }
                            // you could pass back an object here
                            return null;
                        }
                    });


            return true;
        } catch(java.sql.SQLException e) {
            Log.e(OrmLiteBlockStoreDatabase.class.getName(), "Error flushing blocks", e);
            return false;
        }
    }

    public void setFullStorage(boolean storeAllBlocks) {

        this.storeAllBLocks = storeAllBlocks;
    }
}
