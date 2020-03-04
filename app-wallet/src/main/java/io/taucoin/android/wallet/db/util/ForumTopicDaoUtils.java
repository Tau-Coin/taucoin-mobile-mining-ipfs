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

import java.util.List;

import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.greendao.BlockInfoDao;
import io.taucoin.android.wallet.db.greendao.ForumTopicDao;
import io.taucoin.android.wallet.db.greendao.KeyValueDao;

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

    public List<ForumTopic> query() {
        return getForumTopicDao().queryBuilder()
                .where(ForumTopicDao.Properties.ReferId.isNull())
                .orderDesc(ForumTopicDao.Properties.TimeStamp)
                .list();
    }
}