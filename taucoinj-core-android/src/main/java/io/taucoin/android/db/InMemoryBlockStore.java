package io.taucoin.android.db;


import io.taucoin.core.Block;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigInteger.ZERO;
import static io.taucoin.util.ByteUtil.wrap;


public class InMemoryBlockStore implements BlockStore {

    private static final Logger logger = LoggerFactory.getLogger("general");

    Map<ByteArrayWrapper, Block> hashIndex = new HashMap<>();
    Map<Long, Block> numberIndex = new HashMap<>();
    List<Block> blocks = new ArrayList<>();

    private BlockStoreDatabase database;
    protected boolean storeAllBlocks = false;

    BigInteger totalDifficulty = ZERO;

    public InMemoryBlockStore(BlockStoreDatabase database) {

        this.database = database;
    }

    public InMemoryBlockStore(BlockStoreDatabase database, boolean storeAllBlocks) {

        this.database = database;
        this.database.setFullStorage(storeAllBlocks);
        this.storeAllBlocks = storeAllBlocks;
    }

    @Override
    public byte[] getBlockHashByNumber(long blockNumber) {

        Block block = numberIndex.get(blockNumber);

        if (block == null)
            return dbGetBlockHashByNumber(blockNumber);
        else
            return block.getHash();
    }

    @Override
    public Block getChainBlockByNumber(long blockNumber) {

        Block block = numberIndex.get(blockNumber);

        if (block == null)
            return dbGetBlockByNumber(blockNumber);
        else
            return block;
    }

    @Override
    public Block getBlockByHash(byte[] hash) {

        Block block = hashIndex.get(wrap(hash));

        if (block == null)
            return dbGetBlockByHash(hash);
        else
            return block;
    }

    @Override
    public boolean isBlockExist(byte[] hash) {
        Block block = hashIndex.get(wrap(hash));
        return block != null || dbGetBlockByHash(hash) != null;
    }

    @Override
    public List<byte[]> getListHashesEndWith(byte[] hash, long qty) {

        List<Block> blocks = getListBlocksEndWith(hash, qty);
        List<byte[]> hashes = new ArrayList<>(blocks.size());

        for (Block b : blocks) {
            hashes.add(b.getHash());
        }

        return hashes;
    }

    @Override
    public List<Block> getListBlocksEndWith(byte[] hash, long qty) {
        Block startBlock = hashIndex.get(wrap(hash));

        long endIndex = startBlock.getNumber() + qty;
        endIndex = getBestBlock().getNumber() < endIndex ? getBestBlock().getNumber() : endIndex;

        List<Block> blocks = new ArrayList<>();

        for (long i = startBlock.getNumber();  i <= endIndex; ++i){
            Block block = getChainBlockByNumber(i);
            blocks.add(block);
        }

        return blocks;
    }

    @Override
    public void saveBlock(Block block, BigInteger cummDifficulty, boolean mainChain) {
        ByteArrayWrapper wHash = wrap(block.getHash());
        blocks.add(block);
        hashIndex.put(wHash, block);
        numberIndex.put(block.getNumber(), block);
        totalDifficulty = totalDifficulty.add(block.getCumulativeDifficulty());
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    @Override
    public Block getBestBlock() {
        if (blocks.size() == 0) return null;
        return blocks.get(blocks.size() - 1);
    }

    // FIXME: wrap from here in to db class

    public byte[] dbGetBlockHashByNumber(long blockNumber) {

        Block block = getChainBlockByNumber(blockNumber);
        if (block != null) return block.getHash();
        return null;
    }

    public Block dbGetBlockByNumber(long blockNumber) {

        List result = database.getByNumber(blockNumber);
        if (result.size() == 0) return null;
        BlockVO vo = (BlockVO) result.get(0);

        return new Block(vo.rlp);
    }

    public Block dbGetBlockByHash(byte[] hash) {

        List result = database.getByHash(hash);

        if (result.size() == 0) return null;
        BlockVO vo = (BlockVO) result.get(0);

        return new Block(vo.rlp);
    }

    @Override
    public void flush(){

        long t_ = System.nanoTime();

        database.flush(blocks);
        Block block = getBestBlock();

        blocks.clear();
        hashIndex.clear();
        numberIndex.clear();

        saveBlock(block, BigInteger.ZERO, true);

        long t__ = System.nanoTime();
        logger.info("Flush block store in: {} ms", ((float) (t__ - t_) / 1_000_000));

        totalDifficulty = (BigInteger) database.getTotalDifficulty();
    }

    public void load(){

        logger.info("loading db");

        long t = System.nanoTime();

        Block bestBlock = database.getBestBlock();
        if (bestBlock == null) return;
        saveBlock(bestBlock, BigInteger.ZERO, true);

        totalDifficulty =  database.getTotalDifficulty();

        long t_ = System.nanoTime();

        logger.info("Loaded db in: {} ms", ((float)(t_ - t) / 1_000_000));
    }

    @Override
    public long getMaxNumber() {
        Long bestNumber = database.getMaxNumber();

        return bestNumber == null ? 0 : bestNumber;
    }

    @Override
    public void setSessionFactory(SessionFactory sessionFactory) {

    }

    @Override
    public void delNonChainBlock(byte[] hash) {

    }

    @Override
    public void delNonChainBlocksEndWith(byte[] hash) {

    }

    @Override
    public void delNonChainBlocksByNumber(long number) {

    }

    @Override
    public void delChainBlocksWithNumberLessThan(long number) {
    }

    @Override
    public void delChainBlockByNumber(long number) {
    }

    @Override
    public boolean getForkBlocksInfo(Block forkBlock, List<Block> undoBlocks, List<Block> newBlocks) {
        return true;
    }

    @Override
    public void reBranchBlocks(List<Block> undoBlocks, List<Block> newBlocks) {

    }

    @Override
    public BigInteger getTotalDifficultyForHash(byte[] hash) {
        return null;
    }

    @Override
    public long getBlockTimeByNumber(long blockNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(){

    }

    @Override
    public void reset(){
    }
}
