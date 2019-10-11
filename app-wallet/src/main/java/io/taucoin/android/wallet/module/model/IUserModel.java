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
package io.taucoin.android.wallet.module.model;

import java.util.List;

import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface IUserModel {

    /** Save user key and address */
    void saveKeyAndAddress(KeyValue keyValue, LogicObserver<KeyValue> observer);

    /** Save user nickname */
    void saveName(String pubKey, String name, LogicObserver<KeyValue> observer);

    /** Get user key and address */
    void getKeyAndAddress(String publicKey, LogicObserver<KeyValue> observer);

    /** Save Transaction Expiry in blocks */
    void saveTransExpiry(long transExpiry, LogicObserver<KeyValue> observer);

    void getAddressList(String key, LogicObserver<List<KeyValue>> logicObserver);

    void deleteAddress(String pubKey, LogicObserver<Boolean> observer);
}