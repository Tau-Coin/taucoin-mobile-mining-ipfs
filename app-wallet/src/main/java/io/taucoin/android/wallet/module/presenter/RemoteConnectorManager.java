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

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import java.util.Date;

import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.service.events.BlockForgeExceptionStopEvent;
import io.taucoin.android.service.events.BlockForgedInternalEventData;
import io.taucoin.android.service.events.BlocksDownloadedData;
import io.taucoin.android.service.events.ChainInfoChangedData;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.MessageEventData;
import io.taucoin.android.service.events.NetworkTrafficData;
import io.taucoin.android.service.events.NextBlockForgedPOTDetail;
import io.taucoin.android.service.events.PeerDisconnectEventData;
import io.taucoin.android.service.events.PendingTransactionsEventData;
import io.taucoin.android.service.events.StatesLoadedData;
import io.taucoin.android.service.events.StatesLoadedResult;
import io.taucoin.android.service.events.TraceEventData;
import io.taucoin.android.service.events.VMTraceCreatedEventData;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.IMiningModel;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.module.service.StateTagManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.foundation.util.TrafficUtil;
import io.taucoin.net.p2p.HelloMessage;

public class RemoteConnectorManager extends ConnectorManager implements ConnectorHandler {

    private IMiningModel mMiningModel;

    private synchronized IMiningModel getMiningModel(){
        if(mMiningModel == null){
            synchronized (MiningModel.class){
                if(mMiningModel == null){
                    mMiningModel = new MiningModel();
                }
            }
        }
        return mMiningModel;
    }

    @Override
    public void onConnectorConnected() {
        super.onConnectorConnected();
        mMiningModel = null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean handleMessage(Message message) {
        boolean isClaimed = true;
        long time = new Date().getTime();
        switch (message.what) {
            case TaucoinClientMessage.MSG_EVENT:
                Bundle data = message.getData();
                data.setClassLoader(EventFlag.class.getClassLoader());
                EventFlag event = (EventFlag) data.getSerializable("event");
                if (event == null)
                    return false;
                EventData eventData;
                MessageEventData messageEventData;
                String logMessage;
                switch (event) {
                    case EVENT_BLOCK:
                        BlockEventData blockEventData = data.getParcelable("data");
                        logMessage = "Added block with " + /*blockEventData.receipts.size() +*/ " transaction receipts.";
                        time = blockEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_HANDSHAKE_PEER:
                        messageEventData = data.getParcelable("data");
                        logMessage = "Peer " + new HelloMessage(messageEventData.message).getPeerId() + " said hello";
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_NO_CONNECTIONS:
                        eventData = data.getParcelable("data");
                        logMessage = "No connections";
                        time = eventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_PEER_DISCONNECT:
                        PeerDisconnectEventData peerDisconnectEventData = data.getParcelable("data");
                        logMessage = "Peer " + peerDisconnectEventData.host + ":" + peerDisconnectEventData.port + " disconnected.";
                        time = peerDisconnectEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_PENDING_TRANSACTIONS_RECEIVED:
                        PendingTransactionsEventData pendingTransactionsEventData = data.getParcelable("data");
                        logMessage = "Received " + pendingTransactionsEventData.transactions.size() + " pending transactions";
                        time = pendingTransactionsEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_RECEIVE_MESSAGE:
                        messageEventData = data.getParcelable("data");
                        String className = messageEventData.messageClass.getSimpleName();
                        logMessage = "Received message: " + className;
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);

//                        if ("DisconnectMessage".equals(className)) {
//                            DisconnectMessage disconnectMessage = new DisconnectMessage(messageEventData.message);
//                            if (disconnectMessage.getReason() == ReasonCode.DUPLICATE_PEER) {
//                                // TODO Multiple private key question
////                                ToastUtils.showShortToast("DisconnectMessage");
//                            }
//                        }
                        break;
                    case EVENT_SEND_MESSAGE:
                        messageEventData = data.getParcelable("data");
                        logMessage = "Sent message: " + messageEventData.messageClass.getName();
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_HAS_SYNC_DONE:
                    case EVENT_SYNC_DONE:
//                        startBlockForging();
                        eventData = data.getParcelable("data");
                        logMessage = "Sync done";
                        time = eventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_VM_TRACE_CREATED:
                        VMTraceCreatedEventData vmTraceCreatedEventData = data.getParcelable("data");
                        logMessage = "VM trace created: " + vmTraceCreatedEventData.transactionHash;// + " - " + vmTraceCreatedEventData.trace);
                        time = vmTraceCreatedEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_TRACE:
                        TraceEventData traceEventData = data.getParcelable("data");
                        //System.out.println("We got a trace message: " + traceEventData.message);
                        logMessage = traceEventData.message;
                        time = traceEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    // import key and init return
                    case EVENT_TAUCOIN_CREATED:
                    case EVENT_TAUCOIN_EXIST:
                        isInit = 1;
                        startSyncAndMining();
                        break;
                    case EVENT_TAUCOIN_DB_CORRUPTION:
                        MiningUtil.clearAndReloadBlocks();
                        break;
                    case EVENT_BLOCK_DISCONNECT:
                        blockEventData = data.getParcelable("data");
                        handleSynchronizedBlock(blockEventData, false);
                        logMessage = "Disconnect block with " + /*blockEventData.receipts.size() +*/ " transaction receipts.";
                        time = blockEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_BLOCK_CONNECT:
                        blockEventData = data.getParcelable("data");
                        handleSynchronizedBlock(blockEventData, true);
                        logMessage = "Connect block with " + /*blockEventData.receipts.size() +*/ " transaction receipts.";
                        time = blockEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;

                    case EVENT_BLOCK_FORGED_TIME_INTERNAL:
                        BlockForgedInternalEventData blockForgedInternal = data.getParcelable("data");
                        logMessage = "Block forged internal " + blockForgedInternal.blockForgedInternal;
                        addLogEntry(time, logMessage);
                        MessageEvent messageEvent = new MessageEvent();
                        messageEvent.setData(blockForgedInternal.blockForgedInternal);
                        messageEvent.setCode(MessageEvent.EventCode.FORGED_TIME);
                        EventBusUtil.post(messageEvent);
                        break;
                    case EVENT_BLOCK_FORGE_STOP:
                        BlockForgeExceptionStopEvent exceptionStop = data.getParcelable("data");
                        logMessage = "Block forged stop: " + exceptionStop.getMsg();
                        addLogEntry(time, logMessage);
                        if(exceptionStop.getCode() == 3){
                            mExceptionStop = exceptionStop;
                            messageEvent = new MessageEvent();
                            messageEvent.setCode(MessageEvent.EventCode.MINING_STATE);
                            EventBusUtil.post(messageEvent);
                        }
                        break;
                    case EVENT_TRANSACTION_EXECUATED:
//                        TransactionExecuatedEvent txExe = data.getParcelable("data");
//                        if(txExe != null && txExe.outcome != null){
//                            String txId = Hex.toHexString(txExe.outcome.getTxid());
//                            logMessage = "transaction executed id=" + txId;
//                            addLogEntry(time, logMessage);
//                            saveMiningReward(txExe.outcome);
//                        }
                        break;
                    case EVENT_NETWORK_TRAFFIC:
                        NetworkTrafficData networkTrafficData = data.getParcelable("data");
                        int trafficSize = networkTrafficData.trafficSize;
                        TrafficUtil.saveTrafficMining(trafficSize);
                        break;
                    case EVENT_CHAININFO_CHANGED:
                        ChainInfoChangedData chainInfoData = data.getParcelable("data");
                        long height = chainInfoData.height;
                        updateBlockHeight(height);
                        break;
                    case EVENT_BLOCKS_DOWNLOADED:
                        BlocksDownloadedData blocksDownloadedData = data.getParcelable("data");
                        updateBlocksDownloaded(blocksDownloadedData);
                        break;
                    case EVENT_BLOCK_FORGED_POT_DETAIL:
                        NextBlockForgedPOTDetail nextBlockForgedPOTDetail = data.getParcelable("data");
                        messageEvent = new MessageEvent();
                        messageEvent.setData(nextBlockForgedPOTDetail);
                        messageEvent.setCode(MessageEvent.EventCode.FORGED_POT_DETAIL);
                        EventBusUtil.post(messageEvent);
                        break;
                    case EVENT_STATES_LOADED:
                        StatesLoadedData statesLoadedData = data.getParcelable("data");
                        if(statesLoadedData != null){
                            logMessage = "event states hasLoaded= " + statesLoadedData.hasLoaded +
                                    " total=" + statesLoadedData.total;
                            time = statesLoadedData.registeredTime;
                            addLogEntry(time, logMessage);
                        }
                        break;
                    case EVENT_STATES_LOADED_COMPLETED:
                    case EVENT_STATES_LOADED_FAILED:
                        StatesLoadedResult statesLoadedResult = data.getParcelable("data");
                        if(statesLoadedResult != null){
                            logMessage = "event states result= " + statesLoadedResult.success +
                                    " tagHeight=" + statesLoadedResult.tagHeight;
                            time = statesLoadedResult.registeredTime;
                            addLogEntry(time, logMessage);
                            StateTagManager.setStateTagLoaded(statesLoadedResult.tagHeight, statesLoadedResult.success ? 1 : 2);
                            if(statesLoadedResult.success){
                                // delete file
                                MiningUtil.deleteStatesTagFileDir();
                            }
                        }
                        break;
                }
                break;
            case TaucoinClientMessage.MSG_POOL_TXS:
                Intent intent = new Intent();
                intent.putExtras(message.getData());
                break;
            // sync block return
            case TaucoinClientMessage.MSG_START_SYNC_RESULT:
                Bundle replyData = message.getData();
                String result =  replyData.getString(TransmitKey.RESULT);
                logMessage = "start sync result: " + result;
                addLogEntry(time, logMessage);
                break;
            // get block return
            case TaucoinClientMessage.MSG_BLOCK:
                MessageEvent msgEvent = new MessageEvent();
                msgEvent.setCode(MessageEvent.EventCode.GET_BLOCK);
                msgEvent.setData(message.getData());
                EventBusUtil.post(msgEvent);
                break;
            // get block list return
            case TaucoinClientMessage.MSG_BLOCKS:
//                replyData = message.getData();
//                replyData.setClassLoader(BlockEventData.class.getClassLoader());
//                BlockEventData blocks = replyData.getParcelable(TransmitKey.RemoteResult.BLOCK);
//                boolean isFinish = replyData.getBoolean(TransmitKey.RemoteResult.IS_FINISH, false);
//                updateMyMiningBlock(blocks, isFinish);
                break;
            // get block chain height return
            case TaucoinClientMessage.MSG_CHAIN_HEIGHT:
                replyData = message.getData();
                long height =  replyData.getLong(TransmitKey.RemoteResult.HEIGHT);
                updateSynchronizedBlockNum(height);
//                getBlockList(height);
                break;
            case TaucoinClientMessage.MSG_SUBMIT_TRANSACTION_RESULT:
//                replyData = message.getData();
//                replyData.setClassLoader(Transaction.class.getClassLoader());
//                Transaction transaction = replyData.getParcelable(TransmitKey.RemoteResult.TRANSACTION);
//                submitTransactionResult(transaction);
                break;
            case TaucoinClientMessage.MSG_CLOSE_DONE:
                cancelLocalConnector();
                EventBusUtil.post(MessageEvent.EventCode.MINING_STATE);
                break;
            default:
                isClaimed = false;
        }
        return isClaimed;
    }

    private void startSyncAndMining() {
        logger.info("startSyncAndMining");
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            startSyncAll();
            boolean isMiningStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
            if(isMiningStart){
                startBlockForging();
            }else{
                stopBlockForging();
            }
        }
    }

    private void handleSynchronizedBlock(BlockEventData block, boolean isConnect) {
        if(block != null){
            getMiningModel().handleSynchronizedBlock(block, isConnect, new LogicObserver<MessageEvent.EventCode>() {
                @Override
                public void handleData(MessageEvent.EventCode eventCode) {
                    if(eventCode != null){
                        if(eventCode == MessageEvent.EventCode.ALL){
                            EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                            EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
                        }else{
                            EventBusUtil.post(eventCode);
                        }
                    }
                }
            });
        }
    }

    private void updateSynchronizedBlockNum(long height){
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);

        int blockHeight = (int) height;
        getMiningModel().updateSynchronizedBlockNum(blockHeight, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
            }
        });
    }

    private void updateBlockHeight(long height){
        int blockHeight = (int) height;
        getMiningModel().updateBlockHeight(blockHeight, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean keyValue) {
                EventBusUtil.post(MessageEvent.EventCode.BLOCK_HEIGHT);
            }
        });
    }

    private void updateBlocksDownloaded(BlocksDownloadedData blocksDownloadedData) {
        if(blocksDownloadedData != null){
            getMiningModel().updateBlocksDownloaded(blocksDownloadedData.end, new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean aBoolean) {
                    if(aBoolean){
                        EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                    }
                }
            });
        }
    }
}
