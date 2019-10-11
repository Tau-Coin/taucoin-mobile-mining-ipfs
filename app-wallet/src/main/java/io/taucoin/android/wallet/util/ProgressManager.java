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
package io.taucoin.android.wallet.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.github.naturs.logger.Logger;
import io.taucoin.android.wallet.R;

import java.lang.ref.WeakReference;

import io.taucoin.android.wallet.base.BaseActivity;

/**
 * Description: Progress Manager
 */
public class ProgressManager {

    private static volatile Dialog mProgress;

    private static WeakReference<BaseActivity> mWeakReference;

    private static synchronized void showProgressDialog(BaseActivity activity){
        activity.mDialog = showProgressDialog(activity, true);
    }

    public static synchronized void showProgressDialog(FragmentActivity activity){
        BaseActivity baseActivity = (BaseActivity) activity;
        showProgressDialog(baseActivity);
    }

    public static synchronized void showProgressDialog(FragmentActivity activity, boolean isCanCancel){
        BaseActivity baseActivity = (BaseActivity) activity;
        baseActivity.mDialog = showProgressDialog(baseActivity, isCanCancel);
    }

    private static synchronized Dialog showProgressDialog(BaseActivity activity, boolean isCanCancel){
        try {
            closeProgressDialog();
            Logger.d("showProgressDialog");
            mWeakReference = new WeakReference<>(activity);
            if(mWeakReference.get() != null && mWeakReference.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE) != null){

                Dialog progress = new Dialog(mWeakReference.get(), R.style.dialog_translucent);
                progress.requestWindowFeature(Window.FEATURE_NO_TITLE);
                progress.setContentView(R.layout.dialog_waiting);
                progress.setCanceledOnTouchOutside(isCanCancel);
                progress.setCancelable(isCanCancel);
                mProgress = progress;
                if(!mWeakReference.get().isFinishing()){
                    progress.show();
                }else{
                    closeProgressDialog();
                }
                mProgress.setOnCancelListener(ProgressManager::closeProgressDialog);
            }
        }catch (Exception ex){
            Logger.e(ex, "showProgressDialog is error");
        }
        return mProgress;
    }

    public static void closeProgressDialog(){
        if(isShowing()){
            mProgress.dismiss();
            if(mWeakReference != null){
                mWeakReference.clear();
                mWeakReference = null;
            }
            mProgress = null;
        }
        mWeakReference = null;
        mProgress = null;
    }

    public static synchronized void closeProgressDialog(DialogInterface dialog){
        if(dialog != null){
            dialog.dismiss();
        }
    }

    public static synchronized void closeProgressDialog(BaseActivity activity){
        try {
            if(activity != null && mProgress != null && mWeakReference != null){
                FragmentActivity activityReference = mWeakReference.get();
                if(activityReference != null &&
                        activity.getClass().equals(activityReference.getClass())){
                    Logger.d("closeProgressDialog(FragmentActivity activity)");
                    closeProgressDialog();
                }
            }
        }catch (Exception ignore){}
    }

    public static synchronized boolean isShowing(){
        return mProgress != null && mProgress.isShowing();
    }
}