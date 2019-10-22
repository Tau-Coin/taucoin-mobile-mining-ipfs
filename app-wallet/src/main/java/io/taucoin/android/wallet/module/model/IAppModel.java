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

import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.List;

import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;
import io.taucoin.android.wallet.module.bean.HelpBean;
import io.taucoin.android.wallet.module.bean.StatesTagBean;
import io.taucoin.android.wallet.module.bean.VersionBean;
import io.taucoin.android.wallet.net.callback.TAUObserver;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.foundation.net.callback.DataResult;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface IAppModel {

    /** Get the user's IP for the background */
    void getInfo();

    /** Get help page data */
    void getHelpData(LifecycleProvider<ActivityEvent> provider, TAUObserver<DataResult<List<HelpBean>>> observer);

    /** Check app version info */
    void checkAppVersion(TAUObserver<DataResult<VersionBean>> observer);

    void checkStateTag(TxObserver<StatesTagBean> observer);

    void getPeersList(LogicObserver<List<IpfsPeerInfo>> listLogicObserver);

    void getIpfsNode(LogicObserver<IpfsHomeNodeInfo> ipfsHomeNodeInfoLogicObserver);
}
