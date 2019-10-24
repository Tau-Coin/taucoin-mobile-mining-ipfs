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

import com.github.naturs.logger.Logger;

import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.ipfs.IpfsAPIRPCImpl;

/**
 * Local Ipfs api rpc manager
 * */
public class IpfsRPCManager extends IpfsAPIRPCImpl {
    private static IpfsRPCManager mInstance;

    public static IpfsRPCManager getInstance() {
        if (mInstance == null) {
            synchronized (IpfsRPCManager.class) {
                if (mInstance == null) {
                    mInstance = new IpfsRPCManager();
                }
            }
        }
        return mInstance;
    }

    @Override
    protected void onIpfsDaemonDisconnected() {
        super.onIpfsDaemonDisconnected();
        restartIpfsProgress();
    }

    public void restartIpfsProgress() {
        TxService.startTxService(TransmitKey.ServiceType.RESTART_IPFS_PROCESS);
    }
}