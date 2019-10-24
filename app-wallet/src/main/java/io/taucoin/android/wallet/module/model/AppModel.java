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

import android.util.ArrayMap;

import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.ipfs.node.IpfsPeerInfo;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.HelpBean;
import io.taucoin.android.wallet.module.bean.StatesTagBean;
import io.taucoin.android.wallet.module.bean.VersionBean;
import io.taucoin.android.wallet.net.callback.TAUObserver;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.net.service.AppService;
import io.taucoin.android.wallet.net.service.IpfsRPCManager;
import io.taucoin.android.wallet.net.service.TransactionService;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.foundation.net.NetWorkManager;
import io.taucoin.foundation.net.callback.DataResult;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.AppUtil;

public class AppModel implements IAppModel{
    @Override
    public void getInfo() {
        String publicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        Map<String,String> map = new ArrayMap<>();
        map.put("pubkey",  publicKey);
        NetWorkManager.createApiService(AppService.class)
                .getIp(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TAUObserver<Object>() {
                    @Override
                    public void handleError(String msg, int msgCode) {
                    }
                });
    }

    @Override
    public void getHelpData(LifecycleProvider<ActivityEvent> provider, TAUObserver<DataResult<List<HelpBean>>> observer) {
        NetWorkManager.createApiService(AppService.class)
                .getHelpData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(provider.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(observer);
    }

    @Override
    public void checkAppVersion(TAUObserver<DataResult<VersionBean>> observer) {
        int version = AppUtil.getVersionCode(MyApplication.getInstance());
        Map<String, Object> map = new ArrayMap<>();
        map.put("version", version);
        NetWorkManager.createApiService(AppService.class)
                .checkAppVersion(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    @Override
    public void checkStateTag(TxObserver<StatesTagBean> observer) {
        NetWorkManager.createMainApiService(TransactionService.class)
                .getStatesTag()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getPeersList(LogicObserver<List<IpfsPeerInfo>> observer) {
        Observable.create((ObservableOnSubscribe<List<IpfsPeerInfo>>) emitter -> {
            IpfsRPCManager service = IpfsRPCManager.getInstance();
            List<IpfsPeerInfo> data = service.getPeers();
            emitter.onNext(data);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getIpfsNode(LogicObserver<IpfsHomeNodeInfo> observer) {
        Observable.create((ObservableOnSubscribe<IpfsHomeNodeInfo>) emitter -> {
            IpfsRPCManager service = IpfsRPCManager.getInstance();
            IpfsHomeNodeInfo node = service.getIpfsHomeNode();
            emitter.onNext(node);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}