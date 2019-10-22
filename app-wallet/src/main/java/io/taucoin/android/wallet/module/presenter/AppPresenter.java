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
package io.taucoin.android.wallet.module.presenter;

import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.List;

import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;
import io.taucoin.android.wallet.module.bean.HelpBean;
import io.taucoin.android.wallet.module.model.AppModel;
import io.taucoin.android.wallet.module.model.IAppModel;
import io.taucoin.android.wallet.module.view.manage.HelpActivity;
import io.taucoin.android.wallet.module.view.manage.IPFSInfoActivity;
import io.taucoin.android.wallet.module.view.manage.iview.IHelpView;
import io.taucoin.android.wallet.module.view.manage.iview.IIpfsView;
import io.taucoin.android.wallet.net.callback.TAUObserver;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.foundation.net.callback.DataResult;
import io.taucoin.foundation.net.callback.LogicObserver;

public class AppPresenter {
    private IHelpView mHelpView;
    private IIpfsView mIpfsView;
    private IAppModel mAppModel;
    private LifecycleProvider<ActivityEvent> provider;

    public AppPresenter(HelpActivity activity) {
        mAppModel = new AppModel();
        mHelpView = activity;
        provider = activity;
    }

    public AppPresenter(IPFSInfoActivity activity) {
        mAppModel = new AppModel();
        mIpfsView = activity;
        provider = activity;
    }

    public void getHelpData() {
        mAppModel.getHelpData(provider, new TAUObserver<DataResult<List<HelpBean>>>() {
            @Override
            public void handleError(String msg, int msgCode) {
                super.handleError(msg, msgCode);
                ProgressManager.closeProgressDialog();
            }

            @Override
            public void handleData(DataResult<List<HelpBean>> listDataResult) {
                super.handleData(listDataResult);
                ProgressManager.closeProgressDialog();
                if(listDataResult != null && listDataResult.getData() != null){
                    mHelpView.loadHelpData(listDataResult.getData());
                }
            }
        });
    }

    public void getPeersList() {
        mAppModel.getPeersList(new LogicObserver<List<IpfsPeerInfo>>(){

            @Override
            public void handleData(List<IpfsPeerInfo> peers) {
                if(mIpfsView != null){
                    mIpfsView.loadPeerData(peers);
                }
            }
        });
    }

    public void getIpfsNode() {
        mAppModel.getIpfsNode(new LogicObserver<IpfsHomeNodeInfo>(){

            @Override
            public void handleData(IpfsHomeNodeInfo node) {
                if(mIpfsView != null){
                    mIpfsView.loadHomeNodeData(node);
                }
            }
        });
    }
}