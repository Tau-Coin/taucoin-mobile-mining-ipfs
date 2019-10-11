package io.taucoin.net.tau.handler;

import io.netty.channel.ChannelHandlerContext;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockWrapper;
import io.taucoin.net.tau.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import static java.lang.Math.*;
import static java.util.Collections.reverse;
import static io.taucoin.net.tau.TauVersion.*;
import static io.taucoin.net.tau.message.TauMessageCodes.GET_BLOCK_HASHES_BY_NUMBER;
import static io.taucoin.sync.SyncStateName.DONE_HASH_RETRIEVING;
import static io.taucoin.sync.SyncStateName.HASH_RETRIEVING;

/**
 * Tau V61
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class Tau61 extends TauLegacy {

    private static final Logger logger = LoggerFactory.getLogger("sync");

    private static final int FORK_COVER_BATCH_SIZE = 512;

    /**
     * Last blockNumber value sent within GET_BLOCK_HASHES_BY_NUMBER msg
     */
    private long lastAskedNumber = 0;

    /**
     * In Tau 61 we have an ability to check if we're on the fork
     * before starting hash sync.
     *
     * To do this we just download hashes of already known blocks
     * from remote peer with best chain and comparing those hashes against ours.
     *
     * If best peer's hashes differ from ours then we're on the fork
     * and trying to jump back to canonical chain
     */
    // TODO: we need to handle bad peers somehow, cause it may revert us to the very beginning
    private boolean commonAncestorFound = false;

    public Tau61() {
        super(V61);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TauMessage msg) throws InterruptedException {

        super.channelRead0(ctx, msg);

        if (msg.getCommand() == GET_BLOCK_HASHES_BY_NUMBER) {
            processGetBlockHashesByNumber((GetBlockHashesByNumberMessage) msg);
        }
    }

    @Override
    protected void processBlockHashes(List<byte[]> received) {

        // todo check if remote peer responds with same hashes on different GET_BLOCK_HASHES

        if (received.isEmpty()) {
            return;
        }

        if (!commonAncestorFound) {
            maintainForkCoverage(received);
            return;
        }

        List<byte[]> adding = new ArrayList<>(received.size());
        for(byte[] hash : received) {

            adding.add(hash);

            if (Arrays.equals(hash, lastHashToAsk)) {
                changeState(DONE_HASH_RETRIEVING);
                logger.trace("Peer {}: got terminal hash [{}]", channel.getPeerIdShort(), Hex.toHexString(lastHashToAsk));
            }
        }

        queue.addHashesLast(adding);

        if (syncState == DONE_HASH_RETRIEVING) {
            return;
        }

        long blockNumber = lastAskedNumber + received.size();
        sendGetBlockHashesByNumber(blockNumber, maxHashesAsk);
    }

    private void sendGetBlockHashesByNumber(long blockNumber, int maxHashesAsk) {
        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send get block hashes by number, blockNumber [{}], maxHashesAsk [{}]",
                channel.getPeerIdShort(),
                blockNumber,
                maxHashesAsk
        );

        GetBlockHashesByNumberMessage msg = new GetBlockHashesByNumberMessage(blockNumber, maxHashesAsk);
        sendMessage(msg);

        lastAskedNumber = blockNumber;
    }

    protected void processGetBlockHashesByNumber(GetBlockHashesByNumberMessage msg) {
        List<byte[]> hashes = blockchain.getListOfHashesStartFromBlock(
                msg.getBlockNumber(),
                min(msg.getMaxBlocks(), config.maxHashesAsk())
        );

        BlockHashesMessage msgHashes = new BlockHashesMessage(hashes);
        sendMessage(msgHashes);
    }

    @Override
    protected void startHashRetrieving() {

        commonAncestorFound = true;
        long bestNumber = blockchain.getBestBlock().getNumber();

        if (bestNumber > 0) {

            // always assume we're on the fork if best block is not a Genesis one
            startForkCoverage();

        } else {

            // if we're at the beginning there can't be any fork
            sendGetBlockHashesByNumber(bestNumber + 1, maxHashesAsk);

        }

    }

    /************************
    *     Fork Coverage     *
    *************************/


    private void startForkCoverage() {

        commonAncestorFound = false;

        if (isNegativeGap()) {

            logger.trace("Peer {}: start fetching remote fork", channel.getPeerIdShort());
            BlockWrapper gap = syncManager.getGapBlock();
            sendGetBlockHashes(gap.getHash(), FORK_COVER_BATCH_SIZE);
            return;
        }

        logger.trace("Peer {}: start looking for common ancestor", channel.getPeerIdShort());

        long bestNumber = blockchain.getBestBlock().getNumber();
        long blockNumber = max(0, bestNumber - FORK_COVER_BATCH_SIZE + 1);
        sendGetBlockHashesByNumber(blockNumber, min(FORK_COVER_BATCH_SIZE, (int) (bestNumber - blockNumber + 1)));
    }

    private void maintainForkCoverage(List<byte[]> received) {

        if (!isNegativeGap()) reverse(received);

        ListIterator<byte[]> it = received.listIterator();

        if (isNegativeGap()) {

            BlockWrapper gap = syncManager.getGapBlock();

            // gap block didn't come, drop remote peer
            if (!Arrays.equals(it.next(), gap.getHash())) {

                logger.trace("Peer {}: gap block is missed in response, drop", channel.getPeerIdShort());
                syncManager.reportBadAction(channel.getNodeId());
                return;
            }
        }

        // start downloading hashes from blockNumber of the block with known hash
        List<byte[]> hashes = new ArrayList<>();
        while (it.hasNext()) {
            byte[] hash = it.next();
            if (blockchain.isBlockExist(hash)) {
                commonAncestorFound = true;
                if (logger.isTraceEnabled()) logger.trace(
                            "Peer {}: common ancestor found: block.number {}, block.hash {}",
                            channel.getPeerIdShort(),
                            blockchain.getBlockByHash(hash).getNumber(),
                            Hex.toHexString(hash)
                    );

                break;
            }
            hashes.add(hash);
        }

        if (!commonAncestorFound) {

            logger.trace("Peer {}: common ancestor is not found, drop", channel.getPeerIdShort());
            syncManager.reportBadAction(channel.getNodeId());
            return;
        }

        // add missed headers
        queue.addHashes(hashes);

        if (isNegativeGap()) {

            // fork headers should already be fetched here
            logger.trace("Peer {}: remote fork is fetched", channel.getPeerIdShort());
            changeState(DONE_HASH_RETRIEVING);
            return;
        }

        // start header sync
        sendGetBlockHashesByNumber(blockchain.getBestBlock().getNumber() + 1, maxHashesAsk);
    }

    private boolean isNegativeGap() {

        if (syncManager.getGapBlock() == null) return false;

        return syncManager.getGapBlock().getNumber() <= blockchain.getBestBlock().getNumber();
    }
}
