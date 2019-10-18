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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.github.naturs.logger.Logger;

import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinConnector;
import io.taucoin.android.service.TaucoinServiceMessage;
import io.taucoin.android.service.events.BlockForgeExceptionStopEvent;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.StateTagManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.module.service.RemoteService;
import io.taucoin.android.wallet.util.WifiSettings;
import io.taucoin.core.Transaction;

public abstract class ConnectorManager implements ConnectorHandler {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger("ConnectorManager");
    private ScheduledExecutorService initializer = Executors.newSingleThreadScheduledExecutor();
    private TaucoinConnector mTaucoinConnector = null;
    private WifiSettings mWifiSettings = null;

    @SuppressLint("SimpleDateFormat")
    private DateFormat mDateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
    private String mHandlerIdentifier = UUID.randomUUID().toString();
    private String mConsoleLog = "";
    private boolean isTaucoinConnected = false;
    int isInit = -1;
    private int isSyncMe = -1;
    private volatile boolean miningSwitch = false;
    BlockForgeExceptionStopEvent mExceptionStop;

    private final static int CONSOLE_LENGTH = 10000;
    private static final int BOOT_UP_DELAY_INIT_SECONDS = 2;
    private static final int BOOT_UP_DELAY_FORGE_SECONDS = 20;

    private void createRemoteConnector(){
        if (mTaucoinConnector == null) {
            if(mWifiSettings != null){
                mWifiSettings.destroy();
                mWifiSettings = null;
            }
            mWifiSettings = new WifiSettings();
            mWifiSettings.init();
            addLogEntry("Create Remote Connector...");
            Context context = MyApplication.getInstance();
            Parcelable parcelable = NotifyManager.getInstance().getNotifyData();
            mTaucoinConnector = new TaucoinConnector(context, RemoteService.class, parcelable);
            mTaucoinConnector.registerHandler(this);
            mTaucoinConnector.bindService();
        }
    }

    public void cancelRemoteConnector(){
        if (mTaucoinConnector != null) {
            addLogEntry("Cancel Remote Connector...");
            stopSyncAll();
            stopBlockForging();
            closeTaucoin();
            cancelLocalConnector();
            mWifiSettings.destroy();
            mWifiSettings = null;
        }
    }

    public void cancelRemoteProgress(){
        Logger.d("cancelRemoteProgress");
        closeTaucoin();

        Context context = MyApplication.getInstance();
        Parcelable parcelable = NotifyManager.getInstance().getNotifyData();
        Intent intent = new Intent(context, RemoteService.class);
        intent.putExtra("bean", parcelable);
        intent.putExtra(TransmitKey.SERVICE_TYPE, TaucoinServiceMessage.MSG_CLOSE_MINING_PROGRESS);
        context.startService(intent);
    }

    void cancelLocalConnector(){
        if (mTaucoinConnector != null) {
            mTaucoinConnector.removeListener(mHandlerIdentifier);
            mTaucoinConnector.removeHandler(this);
            try {
                mTaucoinConnector.unbindService();
                mTaucoinConnector.stopHandlerThread();
            }catch (Exception e){
                Logger.e(e, "cancelLocalConnector.unbindService is error!");
            }
            mTaucoinConnector = null;
        }
        isInit = -1;
        isSyncMe = -1;
        isTaucoinConnected = false;
        mExceptionStop = null;
    }

    public boolean isInit() {
        return isInit == 1;
    }

    public boolean isError() {
        return mExceptionStop != null;
    }

    public int getErrorCode() {
        if(isError()){
            return mExceptionStop.getCode();
        }
        return -1;
    }

    public void clearErrorCode() {
        mExceptionStop = null;
    }

    public boolean isSyncMe() {
        return isSyncMe == 1;
    }

    public boolean isCalculatingMe() {
        return isSyncMe == 0;
    }

    @Override
    public void onConnectorConnected() {
        if (!isTaucoinConnected) {
            addLogEntry("Connector Connected");
            isTaucoinConnected = true;
            if(!isInit()){
                isInit = -1;
            }
        }
        if(isTaucoinConnected && mTaucoinConnector != null){
            init();
            mTaucoinConnector.addListener(mHandlerIdentifier, EnumSet.allOf(EventFlag.class));
        }
    }

    private void addLogEntry(String message) {
        Date date = new Date();
        addLogEntry(date.getTime(), message);
    }

    @Override
    public void onConnectorDisconnected() {
        try {
            if (mTaucoinConnector != null) {
                addLogEntry("Connector Disconnected");
                mTaucoinConnector.removeListener(mHandlerIdentifier);
                mTaucoinConnector.removeHandler(this);
                isTaucoinConnected = false;
                mTaucoinConnector.stopHandlerThread();
                mTaucoinConnector = null;
                mWifiSettings.destroy();
                mWifiSettings = null;
                isInit = -1;
                isSyncMe = -1;
                init();
            }
        }catch (Exception ignore){}
    }

    @Override
    public String getID() {
        return mHandlerIdentifier;
    }

    void addLogEntry(long timestamp, String message) {
        Date date = new Date(timestamp);
        logger.info("consoleLog=" + message);
        mConsoleLog += mDateFormatter.format(date) + " -> " + (message.length() > 100 ? message.substring(0, 100) + "..." : message) + "\n";

        int length = mConsoleLog.length();
        if (length > CONSOLE_LENGTH) {
            mConsoleLog = mConsoleLog.substring(CONSOLE_LENGTH * ((length / CONSOLE_LENGTH) - 1) + length % CONSOLE_LENGTH);
        }
        EventBusUtil.post(MessageEvent.EventCode.CONSOLE_LOG);
    }

    private void importForgerPrivkey(String privateKey){
        Logger.d("importForgerPrivkey");
        if(mTaucoinConnector != null){
            mTaucoinConnector.importForgerPrivkey(privateKey);
        }
    }

    private void importPrivkeyAndInit(String privateKey){
        Logger.d("importPrivkeyAndInit");
        initializer.schedule(new InitTask(mTaucoinConnector, mHandlerIdentifier, privateKey),
                BOOT_UP_DELAY_INIT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * If the connection with the mining process is interrupted, restore the connection
     * */
    public void restoreConnection(){
        if(mTaucoinConnector == null || !isTaucoinConnected){
            init();
        }
    }

    private class InitTask implements Runnable {
        private TaucoinConnector taucoinConnector;
        private String handlerIdentifier;
        private List<String> privateKeys = new ArrayList<>();

        InitTask(TaucoinConnector taucoinConnector, String handlerIdentifier, String privateKey) {
            this.taucoinConnector = taucoinConnector;
            this.handlerIdentifier = handlerIdentifier;
            this.privateKeys.add(privateKey);
        }

        @Override
        public void run() {
            if(mTaucoinConnector != null){
                taucoinConnector.init(handlerIdentifier, privateKeys);
            }
        }
    }

    private class ForgingTask implements Runnable {
        private TaucoinConnector taucoinConnector;
        private int targetAmount;

        ForgingTask(TaucoinConnector taucoinConnector, int targetAmount) {
            this.taucoinConnector = taucoinConnector;
            this.targetAmount = targetAmount;
        }

        @Override
        public void run() {
            if(mTaucoinConnector != null && isInit() && miningSwitch){
                logger.info("startBlockForging=" + targetAmount);
                taucoinConnector.startBlockForging(targetAmount);
            }
        }
    }

    /**
     * start sync block
     * */
    public void startSync(){
        Logger.d("startSync");
        if(mTaucoinConnector != null){
            mTaucoinConnector.startSync();
        }
    }

    /**
     * 1、start sync block
     * 2、get block height
     * 3、get block list (update my mining block)
     * */
    void startSyncAll(){
        if(mTaucoinConnector != null && isInit()){
            logger.info("startSyncAll");
            mTaucoinConnector.startSync();
            getChainHeight();
        }
    }

    public void stopSyncAll(){
        if(mTaucoinConnector != null && isInit()){
            logger.info("stopSyncAll");
            mTaucoinConnector.stopSync();
        }
    }

    public boolean submitTransaction(Transaction transaction){
        Logger.d("submitTransaction");
        io.taucoin.android.interop.Transaction interT = new io.taucoin.android.interop.Transaction(transaction);
        if(mTaucoinConnector != null && isInit()){
            mTaucoinConnector.submitTransaction(mHandlerIdentifier, interT);
            return true;
        }
        return false;
    }

    public void startBlockForging(){
        startSyncAll();
        if(mWifiSettings != null){
            mWifiSettings.onForgingWifiOnlySettingChanged();
        }
        mExceptionStop = null;
        startBlockForging(-1);
    }

    private void startBlockForging(int targetAmount){
        miningSwitch = true;
        initializer.schedule(new ForgingTask(mTaucoinConnector, targetAmount),
                BOOT_UP_DELAY_FORGE_SECONDS, TimeUnit.SECONDS);
    }

    public void stopBlockForging(){
        stopDownload();
        mExceptionStop = null;
        stopBlockForging(-1);
    }

    private void stopBlockForging(int targetAmount){
        miningSwitch = false;
        if(mTaucoinConnector != null && isInit()){
            logger.info("stopBlockForging=" + targetAmount);
            mTaucoinConnector.stopBlockForging(targetAmount);
        }
    }

    public void getBlockHashList(long start, long limit){
        Logger.d("getBlockHashList");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockHashList(start, limit);
        }
    }

    public void getPendingTxs(){
        Logger.d("getPendingTxs");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getPendingTxs();
        }
    }
    /**
     * get block data by number
     * */
    public void getBlockByNumber(long number){
        Logger.d("getBlockByNumber");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockByNumber(mHandlerIdentifier, number);
        }
    }

    /**
     * get block data by number
     *
     * @param height
     * */

    public void getBlockList(long height){
        getBlockList(0, height);
    }

    private void getBlockList(int num, long height){
        Logger.d("getBlockList num=" + num + "\theight=" + height);
        isSyncMe = -1;
        int limit = (int) height - num + 1;
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockListByStartNumber(mHandlerIdentifier, num, limit);
        }
    }

    /**
     * get chain height (block sync)
     * */
    private void getChainHeight(){
        Logger.d("getChainHeight");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getChainHeight(mHandlerIdentifier);
        }
    }

    private void closeTaucoin(){
        Logger.d("closeTaucoin");
        if(mTaucoinConnector != null){
            mTaucoinConnector.closeEthereum();
        }
    }

    /**
     * init
     * 1、import key and init or only import key
     * 2、start sync block
     * 3、start block forging
     * */
    public void init(){
        Logger.d("init");
        if(UserUtil.isImportKey()){
            if(isTaucoinConnected){
               // statesTag function enter
//                if(!isInit() && !StateTagManager.isStateTagLoading()){
//                    TxService.startTxService(TransmitKey.ServiceType.DOWNLOAD_STATE_TAG);
//                }
                initBlockChain();
            }else{
                if(StateTagManager.isStateTagLoading()){
                    StateTagManager.reLoadStateTags();
                }
                createRemoteConnector();
            }
        }
    }

    public void initBlockChain(){
        if(UserUtil.isImportKey()){
            String privateKey = MyApplication.getKeyValue().getPriKey();
            // import privateKey and init
            if(isInit()){
                importForgerPrivkey(privateKey);
            }else{
                importPrivkeyAndInit(privateKey);
            }
        }
    }

    public void stopDownload(){
        if(mTaucoinConnector != null && isInit()){
            Logger.d("stopDownload");
            mTaucoinConnector.stopDownload();
        }
    }

    public void startDownload(){
        if(mTaucoinConnector != null && isInit()){
            Logger.d("startDownload");
            mTaucoinConnector.startDownload();
        }
    }

    public boolean getIpfsPeers(){
        if(mTaucoinConnector != null && isInit()){
            Logger.d("getIpfsPeers");
            mTaucoinConnector.getIPFSConnectedPeers(mHandlerIdentifier);
            return true;
        }
        return false;
    }

    public boolean getHomeNodeInfo(){
        if(mTaucoinConnector != null && isInit()){
            Logger.d("getHomeNodeInfo");
            mTaucoinConnector.getIPFSHomeNodeInfo(mHandlerIdentifier);
            return true;
        }
        return false;
    }
}