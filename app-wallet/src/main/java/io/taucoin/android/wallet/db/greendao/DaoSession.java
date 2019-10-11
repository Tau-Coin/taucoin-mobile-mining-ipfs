package io.taucoin.android.wallet.db.greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.IncreasePower;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;

import io.taucoin.android.wallet.db.greendao.BlockInfoDao;
import io.taucoin.android.wallet.db.greendao.IncreasePowerDao;
import io.taucoin.android.wallet.db.greendao.KeyValueDao;
import io.taucoin.android.wallet.db.greendao.TransactionHistoryDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig blockInfoDaoConfig;
    private final DaoConfig increasePowerDaoConfig;
    private final DaoConfig keyValueDaoConfig;
    private final DaoConfig transactionHistoryDaoConfig;

    private final BlockInfoDao blockInfoDao;
    private final IncreasePowerDao increasePowerDao;
    private final KeyValueDao keyValueDao;
    private final TransactionHistoryDao transactionHistoryDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        blockInfoDaoConfig = daoConfigMap.get(BlockInfoDao.class).clone();
        blockInfoDaoConfig.initIdentityScope(type);

        increasePowerDaoConfig = daoConfigMap.get(IncreasePowerDao.class).clone();
        increasePowerDaoConfig.initIdentityScope(type);

        keyValueDaoConfig = daoConfigMap.get(KeyValueDao.class).clone();
        keyValueDaoConfig.initIdentityScope(type);

        transactionHistoryDaoConfig = daoConfigMap.get(TransactionHistoryDao.class).clone();
        transactionHistoryDaoConfig.initIdentityScope(type);

        blockInfoDao = new BlockInfoDao(blockInfoDaoConfig, this);
        increasePowerDao = new IncreasePowerDao(increasePowerDaoConfig, this);
        keyValueDao = new KeyValueDao(keyValueDaoConfig, this);
        transactionHistoryDao = new TransactionHistoryDao(transactionHistoryDaoConfig, this);

        registerDao(BlockInfo.class, blockInfoDao);
        registerDao(IncreasePower.class, increasePowerDao);
        registerDao(KeyValue.class, keyValueDao);
        registerDao(TransactionHistory.class, transactionHistoryDao);
    }
    
    public void clear() {
        blockInfoDaoConfig.clearIdentityScope();
        increasePowerDaoConfig.clearIdentityScope();
        keyValueDaoConfig.clearIdentityScope();
        transactionHistoryDaoConfig.clearIdentityScope();
    }

    public BlockInfoDao getBlockInfoDao() {
        return blockInfoDao;
    }

    public IncreasePowerDao getIncreasePowerDao() {
        return increasePowerDao;
    }

    public KeyValueDao getKeyValueDao() {
        return keyValueDao;
    }

    public TransactionHistoryDao getTransactionHistoryDao() {
        return transactionHistoryDao;
    }

}
