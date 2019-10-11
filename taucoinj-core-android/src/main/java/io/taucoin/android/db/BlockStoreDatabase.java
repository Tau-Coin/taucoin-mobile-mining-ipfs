package io.taucoin.android.db;

import io.taucoin.core.Block;

import java.math.BigInteger;
import java.util.List;

public interface BlockStoreDatabase {

    public List<BlockVO> getByNumber(Long number);

    public List<BlockVO> getByHash(byte[] hash);

    public List<byte[]> getHashListByNumberLimit(Long from, Long to);

    public void deleteBlocksSince(long number);

    public void save(BlockVO block);

    public BigInteger getTotalDifficultySince(long number);

    public BigInteger getTotalDifficulty();

    public Block getBestBlock();

    public List<Block> getAllBlocks();

    public void reset();

    public boolean flush(List<Block> blocks);

    public void save(BlockTransactionVO blockTransactionVO);

    public BlockTransactionVO getTransactionLocation(byte[] transactionHash);

    public void setFullStorage(boolean storeAllBlocks);

    public long getMaxNumber();
}
