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

import android.database.Cursor;

import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.List;

import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.greendao.BlockInfoDao;
import io.taucoin.android.wallet.db.greendao.ForumTopicDao;
import io.taucoin.android.wallet.db.greendao.KeyValueDao;
import io.taucoin.android.wallet.db.greendao.TransactionHistoryDao;

/**
 * @version 1.0
 * Edited by yang
 * @description: mining info
 */
public class ForumTopicDaoUtils {

    private final GreenDaoManager daoManager;
    private static ForumTopicDaoUtils mForumTopicDaoUtils;

    private ForumTopicDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static ForumTopicDaoUtils getInstance() {
        if (mForumTopicDaoUtils == null) {
            mForumTopicDaoUtils = new ForumTopicDaoUtils();
        }
        return mForumTopicDaoUtils;
    }

    private ForumTopicDao getForumTopicDao() {
        return daoManager.getDaoSession().getForumTopicDao();
    }

    public long insert(ForumTopic bean) {
        if(bean != null){
            return getForumTopicDao().insertOrReplace(bean);
        }
        return -1;
    }

    public List<ForumTopic> query(int pageNo, String time, int type ) {
//        WhereCondition.StringCondition groupCondition = new WhereCondition.StringCondition(
//                TransactionHistoryDao.Properties.TxId.columnName + " IS NOT NULL GROUP BY " + TransactionHistoryDao.Properties.TxId.columnName);
        QueryBuilder<ForumTopic> db = getForumTopicDao().queryBuilder();
        db.where(ForumTopicDao.Properties.TimeStamp.lt(time),
                ForumTopicDao.Properties.Type.eq(type))
                .orderDesc(ForumTopicDao.Properties.TimeStamp)
                .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);

        List<ForumTopic> list = db.list();
        if(list != null && list.size() > 0){
            for (ForumTopic bean: list) {
                statisticsCountAndTau(bean);
            }
        }
        return list;
    }

    private void statisticsCountAndTau(ForumTopic bean) {
        String sql = "select count(*), sum(" +
                ForumTopicDao.Properties.Fee.columnName +
                ") from " +
                ForumTopicDao.TABLENAME +
                " where type=1 and " +
                ForumTopicDao.Properties.ReferId.columnName +
                "='" +
                bean.getTxId() +
                "'";
        Cursor cursor = daoManager.getDaoSession().getDatabase().rawQuery(sql, null);
        if(cursor != null && cursor.getColumnCount() == 2){
            if(cursor.moveToFirst()){
                bean.setCommentCount(cursor.getInt(0));
                bean.setTauTotal(cursor.getLong(1));
                cursor.close();
            }
        }
    }

    public List<ForumTopic> queryComment(int pageNo, String time, String replyId, int type ) {
        QueryBuilder<ForumTopic> db = getForumTopicDao().queryBuilder();
        db.where(ForumTopicDao.Properties.TimeStamp.lt(time),
                ForumTopicDao.Properties.ReferId.eq(replyId),
                ForumTopicDao.Properties.Type.eq(type))
                .orderDesc(ForumTopicDao.Properties.TimeStamp)
                .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);
        return db.list();
    }

    public List<ForumTopic> query(int pageNo, String time, String searchKey) {
        searchKey = "%" + searchKey + "%";
        QueryBuilder<ForumTopic> db = getForumTopicDao().queryBuilder();
        db.where(ForumTopicDao.Properties.TimeStamp.lt(time),
                ForumTopicDao.Properties.Type.eq(0),
                db.or(ForumTopicDao.Properties.Title.like(searchKey), ForumTopicDao.Properties.Text.like(searchKey)))
                .orderDesc(ForumTopicDao.Properties.TimeStamp)
                .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);
        return db.list();
    }
}