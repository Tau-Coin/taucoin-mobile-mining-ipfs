package io.taucoin.ipfs.config;

public class Topic {

    public static final String TRANSACTION_SUFFIX = "t";
    public static final String BLOCK_SUFFIX = "b";

    public static String getTransactionId(String id) {
        return id + TRANSACTION_SUFFIX;
    }

    public static String getBlockId(String id) {
        return id + BLOCK_SUFFIX;
    }
}
