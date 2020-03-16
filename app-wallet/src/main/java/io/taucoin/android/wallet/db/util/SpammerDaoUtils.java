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

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.db.greendao.SpammerDao;
import io.taucoin.android.wallet.util.DateUtil;

/**
 * @version 1.0
 * Edited by yang
 * @description: Spammer
 */
public class SpammerDaoUtils {

    private final GreenDaoManager daoManager;
    private static SpammerDaoUtils mSpammerDaoUtils;

    private SpammerDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static SpammerDaoUtils getInstance() {
        if (mSpammerDaoUtils == null) {
            mSpammerDaoUtils = new SpammerDaoUtils();
        }
        return mSpammerDaoUtils;
    }

    private SpammerDao getSpammerDao() {
        return daoManager.getDaoSession().getSpammerDao();
    }

    public long update(Spammer bean) {
        if(bean != null){
            return getSpammerDao().insertOrReplace(bean);
        }
        return -1;
    }

    public long insert(String address) {
        Spammer bean = new Spammer();
        bean.setTime(DateUtil.getTime());
        bean.setAddress(address);
        return getSpammerDao().insertOrReplace(bean);
    }

    public void unSpamList(List<String> list) {
        QueryBuilder<Spammer> db = getSpammerDao().queryBuilder();
        db.where(SpammerDao.Properties.Address.in(list));
        db.buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public List<Spammer> getSpamList(int pageNo, String time) {
        QueryBuilder<Spammer> db = getSpammerDao().queryBuilder()
            .where(SpammerDao.Properties.Time.lt(time))
            .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);
        List<Spammer> list = db.list();
        if(list.size() > 0){
            for (Spammer bean : list) {
                String latestName = ForumTopicDaoUtils.getInstance().getLatestName(bean.getAddress());
                bean.setName(latestName);
            }
        }
        return list;
    }

    public Spammer queryByAddress(String tSender) {
        QueryBuilder<Spammer> db = getSpammerDao().queryBuilder();
        db.where(SpammerDao.Properties.Address.eq(tSender));
        return db.build().unique();
    }
}