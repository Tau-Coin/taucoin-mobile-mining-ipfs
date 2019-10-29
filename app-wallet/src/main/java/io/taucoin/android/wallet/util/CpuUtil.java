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

import com.jaredrummler.android.processes.models.Stat;

import java.util.Date;
/**
 * Description: cpu util
 * Author:yang
 * Date: 2019/10/29
 */
class CpuUtil {
    private Long lastMainCpuTime;
    private Long lastMiningCpuTime;
    private Long lastIpfsCpuTime;
    private Long lastMainAppCpuTime;
    private Long lastMiningAppCpuTime;
    private Long lastIpfsAppCpuTime;

    double sampleCPU(Stat appStat, int processType) {
        long cpuTime;
        long appTime;
        double sampleValue = 0.0D;
        try {
            if (appStat == null) {
                return sampleValue;
            }
            Date date = new Date();
            cpuTime = date.getTime();
            appTime = appStat.stime() + appStat.utime();
            long lastAppCpuTime = 0;
            long lastCpuTime = 0;
            if(processType == 0){
                if(lastMainCpuTime == null || lastMainAppCpuTime== null){
                    lastMainAppCpuTime = appTime;
                    lastMainCpuTime = cpuTime;
                    return sampleValue;
                }
                lastAppCpuTime = lastMainAppCpuTime;
                lastCpuTime = lastMainCpuTime;
            }else if(processType == 1){
                if(lastMiningCpuTime == null || lastMiningAppCpuTime== null){
                    lastMiningAppCpuTime = appTime;
                    lastMiningCpuTime = cpuTime;
                    return sampleValue;
                }
                lastAppCpuTime = lastMiningAppCpuTime;
                lastCpuTime = lastMiningCpuTime;
            }else{
                if(lastIpfsCpuTime == null || lastIpfsAppCpuTime== null){
                    lastIpfsAppCpuTime = appTime;
                    lastIpfsCpuTime = cpuTime;
                    return sampleValue;
                }
            }
            long appCpuTimeDiff = appTime - lastAppCpuTime;
            long cpuTimeDiff = cpuTime - lastCpuTime;
            sampleValue = ((double) appCpuTimeDiff / (double) cpuTimeDiff) * 100D;
//            Logger.i("sampleCPU=processType:" + processType + "\tappCpuTimeDiff:" + appCpuTimeDiff +
//                    "\tcpuTimeDiff:" + cpuTimeDiff + "\tsampleValue:" + sampleValue);
            if(processType == 0){
                lastMainAppCpuTime = appTime;
                lastMainCpuTime = cpuTime;
            }else if(processType == 1){
                lastMiningAppCpuTime = appTime;
                lastMiningCpuTime = cpuTime;
            }else{
                lastIpfsAppCpuTime = appTime;
                lastIpfsCpuTime = cpuTime;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sampleValue;
    }
}