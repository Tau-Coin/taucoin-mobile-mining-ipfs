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

import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.greendao.KeyValueDao;
import io.taucoin.foundation.util.StringUtil;

/**
 * @version 1.0
 * Created by yang
 * KeyValue
 */
public class KeyValueDaoUtils {

    private final GreenDaoManager daoManager;
    private static KeyValueDaoUtils mUserDaoUtils;

    private KeyValueDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static KeyValueDaoUtils getInstance() {
        if (mUserDaoUtils == null) {
            mUserDaoUtils = new KeyValueDaoUtils();
        }
        return mUserDaoUtils;
    }

    private KeyValueDao getKeyValueDao() {
        return daoManager.getDaoSession().getKeyValueDao();
    }


    public KeyValue queryByPubicKey(String pubicKey) {
        List<KeyValue> list = getKeyValueDao().queryBuilder()
                .where(KeyValueDao.Properties.PubKey.eq(pubicKey))
                .orderDesc(KeyValueDao.Properties.Id)
                .list();
        if(list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public KeyValue queryByRawAddress(String rawAddress) {
        List<KeyValue> list = getKeyValueDao().queryBuilder()
                .where(KeyValueDao.Properties.RawAddress.eq(rawAddress))
                .orderDesc(KeyValueDao.Properties.Id)
                .list();
        if(list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public String querySignatureKey(String signatureKey) {
        String pubicKey = "";
        List<KeyValue> list = getKeyValueDao().queryBuilder().list();
        if(list.size() > 0){
            for (KeyValue keyValue : list) {
                if(StringUtil.isSame(keyValue.getPubKey().toLowerCase(), signatureKey.toLowerCase())){
                    pubicKey = keyValue.getPubKey();
                    break;
                }
            }
        }
        return pubicKey;
    }

    public KeyValue insertOrReplace(KeyValue keyValue) {
        KeyValue result = KeyValueDaoUtils.getInstance().queryByPubicKey(keyValue.getPubKey());
        if(result == null){
            result = keyValue;
        }else{
            result.setAddress(keyValue.getAddress());
            result.setPubKey(keyValue.getPubKey());
            result.setPriKey(keyValue.getPriKey());
        }
        getKeyValueDao().insertOrReplace(result);
        return result;
    }

    public void update(KeyValue keyValue) {
        getKeyValueDao().insertOrReplace(keyValue);
    }

    public boolean updateMiningState(KeyValue keyValue) {
        long result = getKeyValueDao().insertOrReplace(keyValue);
        return result > -1;
    }

    public List<KeyValue> querySearch(String key) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%");
        stringBuilder.append(key);
        stringBuilder.append("%");
        QueryBuilder<KeyValue> queryBuilder = getKeyValueDao().queryBuilder();
        if(StringUtil.isNotEmpty(key)){
            queryBuilder.where(queryBuilder.or(KeyValueDao.Properties.NickName.like(stringBuilder.toString()),
                    KeyValueDao.Properties.Address.like(stringBuilder.toString())));
        }
        queryBuilder.orderDesc(KeyValueDao.Properties.LastUseTime);
        return queryBuilder.list();
    }

    public void deleteByPubKey(String pubKey) {
        getKeyValueDao().queryBuilder()
            .where(KeyValueDao.Properties.PubKey.eq(pubKey))
            .orderDesc(KeyValueDao.Properties.Id)
            .buildDelete()
            .executeDeleteWithoutDetachingEntities();
    }
}
