package io.taucoin.core;

import java.math.BigInteger;
import java.util.List;

public interface Blockchain {

    static final byte[] GENESIS_HASH = Genesis.getInstance().getHash();

    long getSize();

    boolean addBlock(Block block);

    ImportResult tryToConnect(Block block);

    void storeBlock(Block block, boolean isMainChain);

    Block getBlockByNumber(long blockNr);

    long getBlockTimeByNumber(long blockNumber);

    void setBestBlock(Block block);

    Block getBestBlock();

    boolean hasParentOnTheChain(Block block);

    void close();

    void updateTotalDifficulty(Block block);

    BigInteger getTotalDifficulty();

    void setTotalDifficulty(BigInteger totalDifficulty);

    byte[] getBestBlockHash();

    List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty);

    List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty);

    Block getBlockByHash(byte[] hash);

    List<Chain> getAltChains();

    List<Block> getGarbage();

    void setExitOn(long exitOn);

    boolean isBlockExist(byte[] hash);

    List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes);

    Transaction getTransactionByHash(byte[] hash);

    Block createNewBlock(Block parent, BigInteger baseTarget, byte[] generationSignature,
                         BigInteger cumulativeDifficulty, List<Transaction> transactions);

    /**
     * Get object to wait
     * @return
     */
    Object getLockObject();

    /**
     * Check database sanity.
     * Return false if there is nothing to do;
     * Return true else;
     */
    boolean checkSanity();
}
