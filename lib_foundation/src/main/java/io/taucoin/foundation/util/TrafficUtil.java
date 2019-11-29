package io.taucoin.foundation.util;

import com.github.naturs.logger.Logger;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

public class TrafficUtil {
    private static final String TRAFFIC_ALL_OLD = "trafficAllOld";
    private static final String TRAFFIC_ALL = "trafficAll";
    private static final String TRAFFIC_CLIENT = "trafficClient";
    private static final String TRAFFIC_TIME = "trafficTime";

    public static void saveTrafficAll(long byteSize){
        resetTrafficInfo();
        long oldTraffic = SharedPreferencesHelper.getInstance().getLong(TRAFFIC_ALL_OLD, 0);
        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_ALL_OLD, byteSize);
        if(oldTraffic > 0 && byteSize > oldTraffic){
            byteSize = byteSize - oldTraffic;
        }else {
            byteSize = 0;
        }
        byteSize += SharedPreferencesHelper.getInstance().getLong(TRAFFIC_ALL, 0);
        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_ALL, byteSize);
    }

    public static void saveTrafficWallet(long byteSize){
        resetTrafficInfo();
        byteSize += SharedPreferencesHelper.getInstance().getLong(TRAFFIC_CLIENT, 0);
        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_CLIENT, byteSize);
    }

//    public static void saveTrafficMining(long byteSize){
//        resetTrafficInfo();
//        byteSize += SharedPreferencesHelper.getInstance().getLong(TRAFFIC_MINING, 0);
//        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_MINING, byteSize);
//    }

    public static long getTrafficTotal() {
        long trafficClient = getTrafficClient();
        long trafficAll = getTrafficAll();
        if(trafficAll > trafficClient){
            trafficAll = trafficAll - trafficClient;
        }
        long totalTraffic = trafficClient + trafficAll / 3;
        Logger.d("trafficClient=%s(%s), trafficAll=%s(%s), totalTraffic=%s(%s)",
                formatFileSizeMb(trafficClient), trafficClient,
                formatFileSizeMb(trafficAll), trafficAll,
                formatFileSizeMb(totalTraffic), totalTraffic);
        // Exclude local traffic
        return totalTraffic;
    }

    private static long getTrafficAll() {
        resetTrafficInfo();
        return SharedPreferencesHelper.getInstance().getLong(TRAFFIC_ALL, 0);
    }

    private static long getTrafficClient() {
        resetTrafficInfo();
        return SharedPreferencesHelper.getInstance().getLong(TRAFFIC_CLIENT, 0);
    }

    private static void resetTrafficInfo() {
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = SharedPreferencesHelper.getInstance().getLong(TRAFFIC_TIME, 0);
        if(oldTrafficTime == 0 || compareDay(oldTrafficTime, currentTrafficTime) > 0){
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_TIME, currentTrafficTime);
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_ALL, 0);
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_ALL_OLD, 0);
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_CLIENT, 0);
        }
    }

    private static int compareDay(long formerTime, long latterTime) {
        int day = 0;
        if(latterTime > formerTime){
            try {
                Date date1 = new Date(formerTime);
                Date date2 = new Date(latterTime);
                day = differentDays(date1, date2);
            }catch (Exception ignore){
            }
        }
        return day;
    }

    private static int differentDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2) {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++) {
                if(i%4==0 && i%100!=0 || i%400==0){
                    timeDistance += 366;
                } else {
                    timeDistance += 365;
                }
            }
            return timeDistance + (day2 - day1) ;
        } else {
            return day2 - day1;
        }
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
}
