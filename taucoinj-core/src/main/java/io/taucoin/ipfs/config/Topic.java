package io.taucoin.ipfs.config;

public class Topic {

    public static final String TRANSACTION_SUFFIX = "t";

    public static String getTransactionId(String id) {
        return id + TRANSACTION_SUFFIX;
    }
}
