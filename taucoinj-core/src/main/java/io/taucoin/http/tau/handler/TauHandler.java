package io.taucoin.http.tau.handler;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import io.taucoin.http.client.HttpClient;
import io.taucoin.http.message.Message;
import io.taucoin.http.RequestQueue;
import io.taucoin.http.tau.message.*;
import io.taucoin.sync2.SyncStateEnum;
import io.taucoin.sync2.SyncManager;
import io.taucoin.sync2.SyncQueue;
import io.taucoin.util.BIUtil;
import io.taucoin.util.ByteUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.sync2.SyncStateEnum.*;
import static io.taucoin.util.BIUtil.isLessThan;

/**
 * Process the messages between peerson the network.
 */
public class TauHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger = LoggerFactory.getLogger("http");

    protected SystemProperties config = SystemProperties.CONFIG;

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected CompositeTaucoinListener tauListener;

    protected PendingState pendingState;

    protected HttpClient httpClient;

    private RequestQueue requestQueue;

    @Inject
    public TauHandler(Blockchain blockchain, BlockStore blockstore, SyncManager syncManager,
            SyncQueue queue, PendingState pendingState, TaucoinListener tauListener) {
        this.blockchain = blockchain;
        this.blockstore = blockstore;
        this.syncManager = syncManager;
        this.queue = queue;
        this.pendingState = pendingState;
        this.tauListener = (CompositeTaucoinListener)tauListener;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        httpClient.activate(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        httpClient.deactivate(ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Message msg) throws InterruptedException {

        tauListener.trace(String.format("TauHandler invoke: [%s]", msg.getClass()));

        requestQueue.receivedMessage(msg);

        if (msg instanceof PoolTxsMessage) {
            processPoolTxsMessage((PoolTxsMessage)msg);
        } else {
            //requestManager.processMessage(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Tau handling failed ", cause);
        httpClient.deactivate(ctx);
        ctx.close();
    }

    private void processPoolTxsMessage(PoolTxsMessage msg) {
        final List<Transaction> txList = msg.getTransactions();
        Set<Transaction>  txSet = new HashSet<Transaction>();
        for (Transaction tx : txList) {
            txSet.add(tx);
        }

        for (Transaction tx : txSet) {
            logger.debug("Get pool tx {}", Hex.toHexString(tx.getHash()));
        }

        List<Transaction> txListAdded = pendingState.addWireTransactions(txSet);
        logger.debug("Recv txs [{}], added [{}], dropped [{}]", txSet.size(),
                txListAdded.size(), txSet.size() - txListAdded.size());

        EventDispatchThread.invokeLater(new Runnable() {
            @Override
            public void run() {
                tauListener.onPendingTransactionsReceived(txList);
            }
        });
    }
}
