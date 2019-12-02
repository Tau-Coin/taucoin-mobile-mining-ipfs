package io.taucoin.android.wallet.util;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;

import com.github.naturs.logger.Logger;

import java.util.Calendar;

import io.taucoin.android.wallet.MyApplication;

public class NetworkStatsUtil {

    private static NetworkStatsManager networkStatsManager = null;

    private static synchronized void init() {
        if (networkStatsManager == null) {
            synchronized (SharedPreferencesHelper.class) {
                if (networkStatsManager == null) {
                    Context context = MyApplication.getInstance();
                    networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                }
            }
        }
    }

    public static long getSummaryTotal(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return -1;
        }
        if (context == null) {
            return -1;
        }
        long summaryTotal = -1;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo == null) {
                return -1;
            }
            int uid = packageInfo.applicationInfo.uid;
            summaryTotal = getSummaryTotal(uid);
        } catch (Exception e) {
            Logger.e(e, "");
        }
//        Logger.d("summaryTotal=%s, Mb=%s", summaryTotal,  SysUtil.formatFileSizeMb(summaryTotal));
        return summaryTotal;
    }

    public boolean hasPermissionToReadNetworkStats(FragmentActivity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) activity.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), activity.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        requestReadNetworkStats(activity);
        return false;
    }

    // Open the apps with permission to view usage page
    private void requestReadNetworkStats(FragmentActivity activity) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        activity.startActivity(intent);
    }

    private static long getSummaryTotal(int uid) {
        init();
        long summaryMobile = -1;
        long summaryWifi = -1;
        try {
            NetworkStats summaryStats;
            NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
            long timesDayMorning =  getTimesDayMorning();
            long currentTimeMillis =  System.currentTimeMillis();
            summaryStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, null, timesDayMorning, currentTimeMillis);
            do {
                summaryStats.getNextBucket(summaryBucket);
                int summaryUid = summaryBucket.getUid();
                if (uid == summaryUid) {
                    summaryMobile += summaryBucket.getRxBytes();
                    summaryMobile += summaryBucket.getTxBytes();
                }
            } while (summaryStats.hasNextBucket());

            summaryStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI, null, timesDayMorning, currentTimeMillis);
            do {
                summaryStats.getNextBucket(summaryBucket);
                int summaryUid = summaryBucket.getUid();
                if (uid == summaryUid) {
                    summaryWifi += summaryBucket.getRxBytes();
                    summaryWifi += summaryBucket.getTxBytes();
                }
            } while (summaryStats.hasNextBucket());
        }catch (Exception e) {
            Logger.e(e, "networkStatsManager.querySummary is error");
        }
        if(summaryMobile == -1 && summaryWifi == -1){
            return -1;
        }else{
            if(summaryMobile == -1){
                summaryMobile = 0;
            }else if(summaryWifi == -1){
                summaryWifi = 0;
            }
        }
        long summaryTotal = summaryMobile + summaryWifi;
        Logger.i("uid:%s, mobile:%s(%s), wifi:%s(%s), total:%s(%s)", uid,
                SysUtil.formatFileSizeMb(summaryMobile), summaryMobile,
                SysUtil.formatFileSizeMb(summaryWifi), summaryWifi,
                SysUtil.formatFileSizeMb(summaryTotal), summaryTotal);
        return summaryTotal;
    }

    private static long getTimesDayMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
//        cal.set(Calendar.DAY_OF_MONTH, cal.getLeastMaximum(Calendar.DAY_OF_MONTH));
//        Logger.e("TimeInMillis: %s, %s", cal.getTimeInMillis(), DateUtil.format(cal.getTimeInMillis(), DateUtil.pattern6));
        return cal.getTimeInMillis();
    }
}
