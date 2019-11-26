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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.ipfs.api.IPFS;
import io.ipfs.api.Peer;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.util.ThreadPool;
import io.taucoin.http.tau.message.NewTxMessage;
import io.taucoin.ipfs.config.Topic;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

/**
 * Local Ipfs api rpc manager
 * */
public class IpfsRPCManager {
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

    private static final Logger logger = LoggerFactory.getLogger("IpfsRPCManager");

    private static final long RECONNECT_IPFS_DAEMON_DURATION = 3000;

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    // temp home node id, just for test.
    private static final String HOME_NODE_ID = "id";

    private IpfsHomeNodeInfo ipfsHomeNodeInfo = null;

    private IPFS ipfs;

    private boolean initDone = false;

    // 'initLock' protects race for 'ipfs' & 'initDone'.
    private final ReentrantLock initLock = new ReentrantLock();
    private final Condition init = initLock.newCondition();

    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);

    private Runnable ipfsConnector = this::connectToIpfs;

    private IpfsRPCManager() {
        init();
    }

    private void init() {
        tryToConnectToIpfsDaemon();
    }

    private void tryToConnectToIpfsDaemon() {
        if (isConnected.get() || isConnecting.get()) {
            logger.info("IPFS connection {} is still alive", ipfs);
            return;
        }
        isConnecting.set(true);
        ThreadPool.getThreadPool().execute(ipfsConnector);
    }

    private void connectToIpfs() {
        if (isConnected.get()) {
            logger.info("IPFS connection {} is still alive", ipfs);
            return;
        }
        initLock.lock();

        while(true) {
            // From the source: https://github.com/ipfs/java-ipfs-http-client/blob/master/src/main/java/io/ipfs/api/IPFS.java,
            // It can be found that IPFS constructor will try to connect to ipfs daemon.
            // And if ipfs daemon hasn't been launched, exception will be thrown.
            // Try to connect to ipfs daemon util connected.
            try {
                ipfs = new IPFS(LOCAL_IPFS);

                isConnecting.set(false);
                isConnected.set(true);
            } catch (Exception e) {
                logger.error("Connecting to ipfs daemon error: ", e);
                try {
                    Thread.sleep(RECONNECT_IPFS_DAEMON_DURATION);
                } catch (InterruptedException ie) {
                    logger.error("Connecting to ipfs daemon error: ", ie);
                }

                continue;
            }

            if (isConnected.get()) {
                logger.info("Connection to ipfs daemon is alive");
                initDone = true;
                init.signalAll();
                initLock.unlock();

                break;
            }
        }
    }

    private void onIpfsDaemonDisconnected() {
        initLock.lock();
        initDone = false;
        isConnecting.set(false);
        isConnected.set(false);
        initLock.unlock();
        logger.error("restartIpfsProgress ");
        restartIpfsProgress();
        tryToConnectToIpfsDaemon();
    }

    private boolean isDaemonDisconnected(Exception e) {
        // Hack code, very ugly.
        // From the source: https://github.com/ipfs/java-ipfs-http-client/blob/master/src/main/java/io/ipfs/api/IPFS.java,
        // if rpc connection disappears, RuntimeException will be thrown.
        // throw new RuntimeException("Couldn't connect to IPFS daemon at "+target+"\n Is IPFS running?");
        if (e instanceof RuntimeException) {
            String message = e.getMessage();
            return message != null && message.contains("Couldn't connect to IPFS daemon");
        }

        return false;
    }

    public boolean sendTransaction(Transaction tx) {
        awaitInit();

        if (tx == null) {
            logger.warn("send null transaction");
            return false;
        }

        String txPayload = new NewTxMessage(tx).toJsonString();
        logger.info("send tx {}, payload {}", tx.getTxid(), txPayload);
        try {
            ipfs.pubsub.pub(Topic.getTransactionId(HOME_NODE_ID), txPayload);
        } catch (Exception e) {
            logger.error("pub tx exception: ", e);

            if (isDaemonDisconnected(e)) {
                onIpfsDaemonDisconnected();
                return sendTransaction(tx);
            } else {
                return false;
            }
        }

        return true;
    }

    public IpfsHomeNodeInfo getIpfsHomeNode() {
        awaitInit();

        if (ipfsHomeNodeInfo != null) {
            return ipfsHomeNodeInfo;
        }

        String id = "";
        String version = "";

        try {
            Map idMap = ipfs.id();
            logger.info("Ipfs home node id:{}", idMap);
            if (idMap != null) {
                id = (String)idMap.get("ID");
            }
        } catch (IOException ioe) {
            logger.error("getId ioexception: ", ioe);
        } catch (Exception e) {
            logger.error("getId exception: ", e);
        }

        try {
            version = ipfs.version();
        } catch (IOException ioe) {
            logger.error("get version ioexception: ", ioe);
        } catch (Exception e) {
            logger.error("get version exception: ", e);
            if (isDaemonDisconnected(e)) {
                onIpfsDaemonDisconnected();
                return getIpfsHomeNode();
            }
        }

        ipfsHomeNodeInfo = new IpfsHomeNodeInfo(id, version);
        logger.info("Ipfs home node: {}", ipfsHomeNodeInfo);

        return ipfsHomeNodeInfo;
    }

    public List<IpfsPeerInfo> getPeers() {
        awaitInit();

        List<IpfsPeerInfo> peers = new ArrayList<>();
        List<Peer> swarmPeers = null;

        try {
            swarmPeers = ipfs.swarm.peers();
        } catch (IOException ioe) {
            logger.error("getPeers ioexception: ", ioe);
        } catch (Exception e) {
            logger.error("getPeers exception: ", e);
            if (isDaemonDisconnected(e)) {
                onIpfsDaemonDisconnected();
                return getPeers();
            }
        }

        if (swarmPeers != null && swarmPeers.size() > 0) {
            for (Peer p : swarmPeers) {
                peers.add(new IpfsPeerInfo(p));
            }
        }

        return peers;
    }

    private void awaitInit() {
        initLock.lock();
        try {
            if(!initDone) {
                init.awaitUninterruptibly();
            }
        } finally {
            initLock.unlock();
        }
    }

    public void restartIpfsProgress() {
        TxService.startTxService(TransmitKey.ServiceType.RESTART_IPFS_PROCESS);
    }
}