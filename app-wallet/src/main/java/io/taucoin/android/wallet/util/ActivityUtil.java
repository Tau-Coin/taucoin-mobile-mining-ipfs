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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.naturs.logger.Logger;

import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.module.view.SplashActivity;

/**
 * Description: Activity tools
 * Author:yang
 * Date: 2019/01/02
 */
public class ActivityUtil {

    public static void startActivity(FragmentActivity context, Class<?> zClass){
        Intent intent = new Intent(context, zClass);
        context.startActivity(intent);
    }

    public static void startActivity(Fragment fragment, Class<?> zClass){
        Intent intent = new Intent(fragment.getActivity(), zClass);
        fragment.startActivity(intent);
    }

    public static void startActivity(Intent intent, FragmentActivity context, Class<?> zClass){
        intent.setClass(context, zClass);
        context.startActivity(intent);
    }

    public static void startActivity(Intent intent, Fragment fragment, Class<?> zClass){
        intent.setClass(fragment.getActivity(), zClass);
        fragment.startActivity(intent);
    }

    public static boolean moveTaskToFront(){
        boolean isSuccess = false;
        try {
            Context context = MyApplication.getInstance();
            android.app.ActivityManager mAm = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
            for (android.app.ActivityManager.RunningTaskInfo rti : taskList) {
                if (rti.topActivity.getPackageName().equals(context.getPackageName())) {
                    mAm.moveTaskToFront(rti.id, 0);
                    isSuccess = true;
                    break;
                }
            }
        }catch (Exception e){
            Logger.e("task switch err!", e);
        }
        return isSuccess;
    }

    public static void restartAppTask(){
        Context context = MyApplication.getInstance();
        Intent intentSplash = new Intent(context, SplashActivity.class);
        intentSplash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intentSplash);
    }

    public static void openUri(Context context, String uriStr) {
        boolean isExistBrowser = true;
        try{
            Uri uri = Uri.parse(uriStr);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            if(list != null && list.size() > 0){
                context.startActivity(intent);
            }else{
                isExistBrowser = false;
            }
        }catch (Exception e){
            Logger.e(e, "No browser installed");
            isExistBrowser = false;
        }
        if(!isExistBrowser){
            ToastUtils.showShortToast(R.string.common_install_browser);
        }
    }
}