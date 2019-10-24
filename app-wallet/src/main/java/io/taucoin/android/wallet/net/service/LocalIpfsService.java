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
package io.taucoin.android.wallet.net.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;
import io.taucoin.ipfs.IpfsAPIRPCImpl;

/**
 * Local Ipfs Service
 * */
public class LocalIpfsService extends IpfsAPIRPCImpl {
    private static LocalIpfsService mInstance;
    private final Object mIpfsLocked = new Object();
    private final Logger logger = LoggerFactory.getLogger("LocalIpfsService");

    public static LocalIpfsService getInstance() {
        if (mInstance == null) {
            synchronized (LocalIpfsService.class) {
                if (mInstance == null) {
                    mInstance = new LocalIpfsService();
                }
            }
        }
        return mInstance;
    }

    private void waitForRecovery() {
        synchronized(mIpfsLocked) {
            try {
                logger.info("waitForRecovery");
                checkedConnector();
                mIpfsLocked.wait();
            } catch (InterruptedException e) {
                logger.warn("Waiting for recovery is interrupted");
            }
        }
    }

    private void checkedConnector(){
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            synchronized(mIpfsLocked) {
                TxService.startTxService(TransmitKey.ServiceType.RESTART_IPFS_PROCESS);
                while (true){
                    try {
                        getLocalIpfs().version();
                        mIpfsLocked.notifyAll();
                        emitter.onNext(true);
                        break;
                    }catch (Throwable e){
                        logger.warn("Ipfs connector not available: {}", e.getMessage());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Ipfs connector sleep interrupted: {}", e.getMessage());
                    }
                }
            }
        }).observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean aBoolean) {

                }
            });
    }

    @Override
    public List<IpfsPeerInfo> getPeers() {
        synchronized(mIpfsLocked) {
            try {
                List<IpfsPeerInfo> peers = super.getPeers();
                if(peers != null){
                    logger.info("Peers count {}", peers.size());
                    return peers;
                }
            }catch (Throwable e){
                logger.error("getPeers is fail", e);
            }
            waitForRecovery();
            return getPeers();
        }
    }

    @Override
    public IpfsHomeNodeInfo getIpfsHomeNode() {
        synchronized(mIpfsLocked) {
            try {
                IpfsHomeNodeInfo info = super.getIpfsHomeNode();
                if(info != null && StringUtil.isNotEmpty(info.getId())){
                    logger.info("Node id {}", info.getId());
                    return info;
                }
            }catch (Throwable e){
                logger.error("getIpfsHomeNode is fail", e);
            }
            waitForRecovery();
            return getIpfsHomeNode();
        }
    }

    @Override
    public boolean sendTransaction(Transaction tx) {
        synchronized(mIpfsLocked) {
            try {
                return super.sendTransaction(tx);
            }catch (Throwable e){
                logger.error("getIpfsHomeNode is fail", e);
            }
            return false;
        }
    }
}