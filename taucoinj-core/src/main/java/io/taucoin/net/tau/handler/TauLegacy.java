package io.taucoin.net.tau.handler;

import io.netty.channel.ChannelHandlerContext;
import io.taucoin.core.Block;
import io.taucoin.core.Genesis;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.tau.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.sync.SyncStateName.*;
import static io.taucoin.sync.SyncStateName.BLOCK_RETRIEVING;
import static io.taucoin.util.BIUtil.isMoreThan;
import static io.taucoin.util.ByteUtil.wrap;

/**
 * Holds commons for legacy versions of Tau protocol <br>
 * Covers PV 60 and PV 61
 *
 * @author Mikhail Kalinin
 * @since 05.09.2015
 */
public abstract class TauLegacy extends TauHandler {

    private final static Logger logger = LoggerFactory.getLogger("sync");

    /**
     * Hash list sent in GET_BLOCKS message,
     * useful if returned BLOCKS msg doesn't cover all sent hashes
     * or in case when peer is disconnected
     */
    private final List<ByteArrayWrapper> sentHashes = Collections.synchronizedList(new ArrayList<ByteArrayWrapper>());

    protected TauLegacy(TauVersion version) {
        super(version);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TauMessage msg) throws InterruptedException {

        super.channelRead0(ctx, msg);

        switch (msg.getCommand()) {
            case NEW_BLOCK_HASHES:
                processNewBlockHashes((NewBlockHashesMessage) msg);
                break;
            case GET_BLOCK_HASHES:
                processGetBlockHashes((GetBlockHashesMessage) msg);
                break;
            case BLOCK_HASHES:
                dispatchBlockHashes((BlockHashesMessage) msg);
                break;
            case GET_BLOCKS:
                processGetBlocks((GetBlocksMessage) msg);
                break;
            case BLOCKS:
                processBlocks((BlocksMessage) msg);
                break;
            default:
                break;
        }
    }

    @Override
    public void sendNewBlockHashes(Block block) {

        NewBlockHashesMessage msg = new NewBlockHashesMessage(singletonList(block.getHash()));
        sendMessage(msg);
    }

    @Override
    public void onShutdown() {
        super.onShutdown();
        returnHashes();
    }

    abstract protected void processBlockHashes(List<byte[]> received);

    @Override
    protected boolean startBlockRetrieving() {
        return sendGetBlocks();
    }

    private void processNewBlockHashes(NewBlockHashesMessage msg) {
        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing NEW block hashes, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockHashes().size()
        );

        List<byte[]> hashes = msg.getBlockHashes();
        if (hashes.isEmpty()) {
            return;
        }

        this.bestHash = hashes.get(hashes.size() - 1);

        queue.addNewBlockHashes(hashes);
    }

    protected void processGetBlockHashes(GetBlockHashesMessage msg) {
        List<byte[]> hashes = blockchain.getListOfHashesStartFrom(
                msg.getBestHash(),
                min(msg.getMaxBlocks(), MAX_HASHES_TO_SEND)
        );

        BlockHashesMessage msgHashes = new BlockHashesMessage(hashes);
        sendMessage(msgHashes);
    }

    protected void dispatchBlockHashes(BlockHashesMessage blockHashesMessage) {
        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing block hashes, size [{}]",
                channel.getPeerIdShort(),
                blockHashesMessage.getBlockHashes().size()
        );

        if (syncState != HASH_RETRIEVING) {
            return;
        }

        List<byte[]> receivedHashes = blockHashesMessage.getBlockHashes();

        // treat empty hashes response as end of hash sync
        if (receivedHashes.isEmpty()) {
            changeState(DONE_HASH_RETRIEVING);
        } else {
            syncStats.addHashes(receivedHashes.size());
            processBlockHashes(receivedHashes);
        }

        if (logger.isInfoEnabled()) {
            if (syncState == DONE_HASH_RETRIEVING) {
                logger.info(
                        "Peer {}: hashes sync completed, [{}] hashes in queue",
                        channel.getPeerIdShort(),
                        queue.hashStoreSize()
                );
            } else {
                queue.logHashesSize();
            }
        }
    }

    private void processGetBlocks(GetBlocksMessage msg) {

        List<byte[]> hashes = msg.getBlockHashes();

        List<Block> blocks = new ArrayList<>(hashes.size());
        for (byte[] hash : hashes) {
            Block block = blockchain.getBlockByHash(hash);
            if (block != null) {
                blocks.add(block);
            }
        }

        BlocksMessage bm = new BlocksMessage(blocks);
        sendMessage(bm);
    }

    // Parallel download blocks based on hashQueue
    private boolean sendGetBlocks() {
        // retrieve list of block hashes from queue
        // save them locally in case the remote peer
        // will return less blocks than requested.
        List<byte[]> hashes = queue.pollHashes();
        if (hashes.isEmpty()) {
            if(logger.isInfoEnabled()) logger.trace(
                    "Peer {}: no more hashes in queue, idle",
                    channel.getPeerIdShort()
            );
            changeState(IDLE);
            return false;
        }

        sentHashes.clear();
        for (byte[] hash : hashes)
            this.sentHashes.add(wrap(hash));

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send get blocks, hashes.count [{}]",
                channel.getPeerIdShort(),
                sentHashes.size()
        );

        Collections.shuffle(hashes);
        GetBlocksMessage msg = new GetBlocksMessage(hashes);

        sendMessage(msg);

        return true;
    }

    private void processBlocks(BlocksMessage blocksMessage) {
        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: process blocks, size [{}]",
                channel.getPeerIdShort(),
                blocksMessage.getBlocks().size()
        );

        List<Block> blockList = blocksMessage.getBlocks();

        syncStats.addBlocks(blockList.size());

        // check if you got less blocks than you asked,
        // and keep the missing to ask again
        sentHashes.remove(wrap(Genesis.getInstance().getHash()));
        for (Block block : blockList){
            ByteArrayWrapper hash = wrap(block.getHash());
            sentHashes.remove(hash);
        }
        returnHashes();

        if(!blockList.isEmpty()) {

            List<Block> regularBlocks = new ArrayList<>(blockList.size());

            for (Block block : blockList) {
                if (block.getNumber() < newBlockLowerNumber) {
                    regularBlocks.add(block);
                } else {
                    queue.addNew(block, channel.getNodeId());
                }
            }

            queue.addAndValidate(regularBlocks, channel.getNodeId());
            queue.logHashesSize();
        } else {
            changeState(BLOCKS_LACK);
        }

        if (syncState == BLOCK_RETRIEVING) {
            sendGetBlocks();
        }
    }

    protected void sendGetBlockHashes() {
        sendGetBlockHashes(lastHashToAsk, maxHashesAsk);
    }

    protected void sendGetBlockHashes(byte[] lastHashToAsk, int maxHashesAsk) {
        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send get block hashes, hash [{}], maxHashesAsk [{}]",
                channel.getPeerIdShort(),
                Hex.toHexString(lastHashToAsk),
                maxHashesAsk
        );
        GetBlockHashesMessage msg = new GetBlockHashesMessage(lastHashToAsk, maxHashesAsk);
        sendMessage(msg);
    }

    private void returnHashes() {
        if(logger.isDebugEnabled()) logger.debug(
                "Peer {}: return [{}] hashes back to store",
                channel.getPeerIdShort(),
                sentHashes.size()
        );

        synchronized (sentHashes) {
            queue.returnHashes(sentHashes);
        }

        sentHashes.clear();
    }
}
