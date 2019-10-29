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

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.storage.StorageManager;

import com.github.naturs.logger.Logger;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;
import com.jaredrummler.android.processes.models.Statm;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.foundation.util.StringUtil;

/**
 * Description: sys tools
 * Author:yang
 * Date: 2019/01/02
 */
public class SysUtil {
    private CpuUtil cpuUtil = new CpuUtil();

    public void getPkgInfo(String pkg, IPackageStatsObserver.Stub packageStatsStub) {
        Context context = MyApplication.getInstance();
        PackageManager pm = context.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
                StorageStats storageStats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, Process.myUserHandle());
                PackageStats info = new PackageStats(pkg);
                info.cacheSize = storageStats.getCacheBytes();
                info.dataSize = storageStats.getDataBytes();
                info.codeSize = storageStats.getAppBytes();
                packageStatsStub.onGetStatsCompleted(info, true);
            }else {
                Method getPackageSizeInfo = pm.getClass().getMethod(
                        "getPackageSizeInfo", String.class,
                        IPackageStatsObserver.class);
                getPackageSizeInfo.invoke(pm, pkg, packageStatsStub);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MemoryInfo loadAppProcess() {
        Context context = MyApplication.getInstance();
        MemoryInfo memoryInfo = new MemoryInfo();
        try {
            List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
            for (AndroidAppProcess process : processes) {
                // Get some information about the process
                String processName = process.name;
                String mainProcess = context.getPackageName();
                String miningProcess = mainProcess + ":taucoin_service";

                if(StringUtil.isNotSame(processName, mainProcess)
                        && StringUtil.isNotSame(processName, miningProcess) && !process.isIpfsProcess()){
                    continue;
                }
                Statm statm = process.statm();
                long totalSizeOfProcess = statm.getSize();
                long residentSetSize = statm.getResidentSetSize();
                memoryInfo.maxMemory += totalSizeOfProcess;
                memoryInfo.totalMemory += residentSetSize * 4;
//                Logger.d("pid=%s, MemorySize=%s, MemorySize*4=%s, name=%s", process.pid, formatFileSize(residentSetSize), formatFileSize(residentSetSize*4), process.name);

                Stat stat = process.stat();
                int processType = StringUtil.isSame(processName, context.getPackageName()) ? 0 : 2;
                processType = StringUtil.isSame(processName, miningProcess) ? 1 : processType;
                double cpu = cpuUtil.sampleCPU(stat, processType);
                memoryInfo.cpuUsageRate += cpu;
            }
            if(memoryInfo.cpuUsageRate < 0){
                memoryInfo.cpuUsageRate = 0;
            }else if(memoryInfo.cpuUsageRate > 100){
                memoryInfo.cpuUsageRate = 100;
            }
        } catch (Exception e) {
            Logger.e(e,"loadAppProcess.is error");
            e.printStackTrace();
        }
        return memoryInfo;
    }

    public static String formatFileSize(long length) {
        String result = null;
        int sub_string = 0;
        if (length >= 1073741824) {
            sub_string = String.valueOf((float) length / 1073741824).indexOf(
                    ".");
            result = ((float) length / 1073741824 + "000").substring(0,
                    sub_string + 3) + "G";
        } else if (length >= 1048576) {
            sub_string = String.valueOf((float) length / 1048576).indexOf(".");
            result = ((float) length / 1048576 + "000").substring(0,
                    sub_string + 3) + "M";
        } else if (length >= 1024) {
            sub_string = String.valueOf((float) length / 1024).indexOf(".");
            result = ((float) length / 1024 + "000").substring(0,
                    sub_string + 3) + "K";
        } else if (length >= 0) {
            result = Long.toString(length) + "B";
        }
        return result;
    }

    public static String formatFileSizeMb(long length) {
        float lengthM = (float) length / 1048576;
        BigDecimal bigDecimal = new BigDecimal(lengthM);
        String lengthStr = bigDecimal.toString();
        int sub_string = lengthStr.indexOf(".");
        String result = lengthStr + "000";
        result = result.substring(0, sub_string + 3);
        if(lengthM >= 0.01){
            result = result.substring(0, sub_string + 3) + "M";
        }else{
            result = "0M";
        }
        return result;
    }

    public class MemoryInfo implements Parcelable {
        public long maxMemory;
        public long totalMemory;
        public long freeMemory;
        public double cpuUsageRate;
        public long netDataSize;

        MemoryInfo() {

        }
        MemoryInfo(Parcel in) {
            maxMemory = in.readLong();
            totalMemory = in.readLong();
            freeMemory = in.readLong();
        }

        public final Creator<MemoryInfo> CREATOR = new Creator<MemoryInfo>() {
            @Override
            public MemoryInfo createFromParcel(Parcel in) {
                return new MemoryInfo(in);
            }

            @Override
            public MemoryInfo[] newArray(int size) {
                return new MemoryInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(maxMemory);
            dest.writeLong(totalMemory);
            dest.writeLong(freeMemory);
        }
    }

    static class PackageStats extends android.content.pm.PackageStats{

        public PackageStats(String pkgName) {
            super(pkgName);
        }
    }
}