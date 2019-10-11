package io.taucoin.foundation.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.github.naturs.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class AppUtil {

    public static boolean isNotMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        String processName = null;
        android.app.ActivityManager mActivityManager = (android.app.ActivityManager)context
                .getSystemService(Context.ACTIVITY_SERVICE);
        if (mActivityManager != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess :
                    mActivityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }
        }
        String mainProcessName = context.getPackageName();
        return StringUtil.isNotSame(processName, mainProcessName);
    }


    public static String getSysVersion(){
        String sysVersion = android.os.Build.VERSION.RELEASE;
        return sysVersion;
    }

    // Version name
    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }
    // Version code
    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * Does the application run in the foreground
     */
    public static boolean isOnForeground(Context context) {
        android.app.ActivityManager activityManager = (android.app.ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (android.app.ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                return appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }

    public static long getLastUpdateTime(Context context) {
        try {
            PackageManager packageManager = context.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            // app install time
            long firstInstallTime = packageInfo.firstInstallTime;
            // app last update time
            long lastUpdateTime = packageInfo.lastUpdateTime;

            return firstInstallTime > lastUpdateTime?firstInstallTime:lastUpdateTime;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean getUnInstallApkInfo(Context context, String filePath) {
        boolean result = false;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                result = true;
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    public static void killProcess(Context context, boolean isKillMainProcess) {
        int myPid = android.os.Process.myPid();
        String packageName = context.getPackageName();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager
                .getRunningAppProcesses()) {
            String processName = appProcess.processName;
            if (StringUtil.isNotEmpty(processName) &&
                    processName.contains(packageName) && myPid != appProcess.pid) {
                Logger.d("killProcess.RemoteService=" + appProcess.pid);

                if(isKillMainProcess || processName.contains("taucoin_service")){
                    android.os.Process.killProcess(appProcess.pid);
                }
            }
        }

        if(isKillMainProcess){
            android.os.Process.killProcess(myPid);
            Logger.d("killProcess=" + myPid);
            System.exit(0);
        }
    }

    public static boolean isServiceRunning(Context context, String serviceName) {
        if (StringUtil.isEmpty(serviceName)){
            return false;
        }
        try{
            ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                    .getRunningServices(200);
            for (int i = 0; i < runningService.size(); i++) {
                String className = runningService.get(i).service.getClassName();
                if (StringUtil.isSame(className, serviceName)) {
                    return true;
                }
            }
        }catch (Exception ignore){

        }
        return false;
    }
}