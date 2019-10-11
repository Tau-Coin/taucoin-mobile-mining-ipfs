/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.db.util;

import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.greendao.TransactionHistoryDao;

import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.util.DateUtil;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

/**
 * @version 1.0
 * Created by ly on 18-10-31
 * @version 2.0
 * Edited by yang
 * TransactionHistory
 */
public class TransactionHistoryDaoUtils {

    private final GreenDaoManager daoManager;
    private static TransactionHistoryDaoUtils mTransactionHistoryDaoUtils;

    private TransactionHistoryDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static TransactionHistoryDaoUtils getInstance() {
        if (mTransactionHistoryDaoUtils == null) {
            mTransactionHistoryDaoUtils = new TransactionHistoryDaoUtils();
        }
        return mTransactionHistoryDaoUtils;
    }

    private TransactionHistoryDao getTransactionHistoryDao() {
        return daoManager.getDaoSession().getTransactionHistoryDao();
    }

    public List<TransactionHistory> getTxPendingListDelay(String formAddress) {
        long time = DateUtil.getTime() - 2 * 60; // delay 5min
        QueryBuilder<TransactionHistory> qb = getTransactionHistoryDao().queryBuilder();
        qb.where(TransactionHistoryDao.Properties.FromAddress.eq(formAddress),
                TransactionHistoryDao.Properties.CreateTime.lt(time),
                qb.or(TransactionHistoryDao.Properties.Result.eq(TransmitKey.TxResult.BROADCASTING),
                        TransactionHistoryDao.Properties.Result.eq(TransmitKey.TxResult.CONFIRMING)));
        qb.orderAsc(TransactionHistoryDao.Properties.CreateTime);
        return qb.list();
    }

    public List<TransactionHistory> getPendingAmountList(String formAddress) {
        QueryBuilder<TransactionHistory> qb = getTransactionHistoryDao().queryBuilder();
        qb.where(TransactionHistoryDao.Properties.FromAddress.eq(formAddress),
            qb.or(TransactionHistoryDao.Properties.Result.eq(TransmitKey.TxResult.BROADCASTING),
                TransactionHistoryDao.Properties.Result.eq(TransmitKey.TxResult.CONFIRMING)));
        qb.orderAsc(TransactionHistoryDao.Properties.CreateTime);
        return qb.list();
    }

    public TransactionHistory queryTransactionById(String txId) {
        List<TransactionHistory> list = getTransactionHistoryDao().queryBuilder()
                .where(TransactionHistoryDao.Properties.TxId.eq(txId))
                .orderDesc(TransactionHistoryDao.Properties.CreateTime)
                .list();
        if(list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public void insertOrReplace(TransactionHistory tx) {
        getTransactionHistoryDao().insertOrReplace(tx);
    }

    public List<TransactionHistory> queryData(int pageNo, String time, String address) {
         QueryBuilder<TransactionHistory> db = getTransactionHistoryDao().queryBuilder();
         db.where(TransactionHistoryDao.Properties.CreateTime.lt(time),
                db.or(TransactionHistoryDao.Properties.FromAddress.eq(address),
                    db.and(TransactionHistoryDao.Properties.ToAddress.eq(address),
                        TransactionHistoryDao.Properties.Result.eq(TransmitKey.TxResult.SUCCESSFUL)))
                ).orderDesc(TransactionHistoryDao.Properties.CreateTime)
                 .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);
        return db.list();
    }

    /** It's your latest block height to accept your address here.*/
    public long getNewestBlockHeight(String address) {
        long blockHeight = 0;
        QueryBuilder<TransactionHistory> db = getTransactionHistoryDao().queryBuilder();
        db.where(TransactionHistoryDao.Properties.TimeBasis.eq(1),
            db.or(TransactionHistoryDao.Properties.FromAddress.eq(address),
                TransactionHistoryDao.Properties.ToAddress.eq(address))
        )
        .orderDesc(TransactionHistoryDao.Properties.BlockHeight);
        List<TransactionHistory> list = db.list();
        if(list.size() > 0){
            blockHeight = list.get(0).getBlockHeight();
        }
        return blockHeight;
    }
}