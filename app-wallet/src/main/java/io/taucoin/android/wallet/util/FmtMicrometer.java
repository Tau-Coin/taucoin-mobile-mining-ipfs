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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import io.taucoin.android.wallet.core.Constants;

public class FmtMicrometer {
    
    private static String mDecimal = "100";
    private static String mDecimal8 = "100000000";
    private static String mDecimal8Pattern = "###,##0.00######";
    public static String mDecimal1Pattern = "###,##0.#";
    private static int mScale = 2;
    private static int mScale8 = 8;

    public static String fmtBalance(Long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.00");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
        return df.format(bigDecimal);
    }

    static String fmtMiningIncome(Long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.00");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
        return df.format(bigDecimal);
    }

    public static String fmtPower(Long power) {
        return fmtPower(String.valueOf(power));
    }

    public static String fmtPower(String power) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(power);
            return df.format(bigDecimal);
        }catch (Exception ignore) {

        }
        return new BigInteger("0").toString();
    }

    static String fmtValue(double value) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.##");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(value);
        return df.format(bigDecimal);
    }

    public static String fmtDecimal(String value) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0.00");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(value);
            return df.format(bigDecimal);
        } catch (Exception ignore) {
        }
        return new BigInteger("0").toString();
    }

    public static String fmtDecimal(double value) {
        return fmtDecimal(String.valueOf(value));
    }

    private static DecimalFormat getDecimalFormatInstance() {
        DecimalFormat df;
        try{
            df = (DecimalFormat)NumberFormat.getInstance(Locale.CHINA);
        }catch (Exception e){
            df = new DecimalFormat();
        }
        return df;
    }

    public static String fmtAmount(String amount) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
            return bigDecimal.toString();
        } catch (Exception e) {
            return amount;
        }
    }

    public static String fmtFormat(String num) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.00");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFeeValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static String fmtTxValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            bigDecimal = bigDecimal.multiply(new BigDecimal(mDecimal));

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static String fmtFormatFee(String num, String multiply) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.multiply(new BigDecimal(multiply));

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.00");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFormatRangeFee(String num) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.multiply(new BigDecimal(mDecimal));
            if(number.toBigInteger().compareTo(Constants.MIN_FEE) < 0){
                number = new BigDecimal(Constants.MIN_FEE);
            }
            number = number.divide(new BigDecimal(mDecimal), 2, RoundingMode.HALF_UP);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.00");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFormatAdd(String amount, String fee) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.add(new BigDecimal(fee));
            return bigDecimal.toString();
        } catch (Exception e) {
            return amount;
        }
    }

    public static String fmtMoney(long value) {
        try {
            value = fmtDecimal8(value);
            return fmtDecimal(value, mDecimal8Pattern);
        } catch (Exception ignore) {
        }
        return new BigInteger("0").toString();
    }

    public static String fmtDecimal(double value, String pattern) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern(pattern);
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(value);
            return df.format(bigDecimal);
        } catch (Exception ignore) {
        }
        return new BigInteger("0").toString();
    }

    private static long fmtDecimal8(long money) {
        BigDecimal bigDecimal = new BigDecimal(money);
        bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal8), mScale8, RoundingMode.HALF_UP);
        return bigDecimal.longValue();
    }
}
