package io.taucoin.net.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.Transaction;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.net.MessageQueue;
import io.taucoin.net.client.Capability;
import io.taucoin.net.tau.handler.Tau;
import io.taucoin.net.tau.handler.TauAdapter;
import io.taucoin.net.tau.handler.TauHandler;
import io.taucoin.net.tau.handler.TauHandlerFactory;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.tau.message.Tau60MessageFactory;
import io.taucoin.net.tau.message.Tau61MessageFactory;
import io.taucoin.net.tau.message.Tau62MessageFactory;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.rlpx.*;
import io.taucoin.sync.SyncStateName;
import io.taucoin.sync.SyncStatistics;
import io.taucoin.net.message.MessageFactory;
import io.taucoin.net.message.StaticMessages;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.p2p.P2pHandler;
import io.taucoin.net.p2p.P2pMessageFactory;
import io.taucoin.net.rlpx.discover.NodeManager;
import io.taucoin.net.rlpx.discover.NodeStatistics;
import io.taucoin.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
public class Channel {

    private final static Logger logger = LoggerFactory.getLogger("net");

    public static final int MAX_SAFE_TXS = 192;

    private MessageQueue msgQueue;

    private P2pHandler p2pHandler;

    private MessageCodec messageCodec;

    private HandshakeHandler handshakeHandler;

    private NodeManager nodeManager;

    private TauHandlerFactory ethHandlerFactory;

    private Tau tau = new TauAdapter();

    private InetSocketAddress inetSocketAddress;

    private Node node;
    private NodeStatistics nodeStatistics;

    private boolean discoveryMode;
    private boolean isActive;

    @Inject
    public Channel(MessageQueue msgQueue, P2pHandler p2pHandler
            , MessageCodec messageCodec
            , NodeManager nodeManager, TauHandlerFactory ethHandlerFactory
            , HandshakeHandler handshakeHandler) {
        this.msgQueue = msgQueue;
        this.p2pHandler = p2pHandler;
        this.messageCodec = messageCodec;
        this.nodeManager = nodeManager;
        this.ethHandlerFactory = ethHandlerFactory;
        this.handshakeHandler = handshakeHandler;
    }

    public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode) {

        isActive = remoteId != null && !remoteId.isEmpty();

        pipeline.addLast("readTimeoutHandler",
                new ReadTimeoutHandler(CONFIG.peerChannelReadTimeout(), TimeUnit.SECONDS));
        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.discoveryMode = discoveryMode;

        if (discoveryMode) {
            // temporary key/nodeId to not accidentally smear our reputation with
            // unexpected disconnect
            handshakeHandler.generateTempKey();
        }

        handshakeHandler.setRemoteId(remoteId, this);

        messageCodec.setChannel(this);

        msgQueue.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);
        messageCodec.setP2pMessageFactory(new P2pMessageFactory());
    }

    public void publicRLPxHandshakeFinished(ChannelHandlerContext ctx, FrameCodec frameCodec,
                                            HelloMessage helloRemote) throws IOException, InterruptedException {

        logger.debug("publicRLPxHandshakeFinished with " + ctx.channel().remoteAddress());
        if (P2pHandler.isProtocolVersionSupported(helloRemote.getP2PVersion())) {

            if (helloRemote.getP2PVersion() < 5) {
                messageCodec.setSupportChunkedFrames(false);
            }

            FrameCodecHandler frameCodecHandler = new FrameCodecHandler(frameCodec, this);
            ctx.pipeline().addLast("medianFrameCodec", frameCodecHandler);
            ctx.pipeline().addLast("messageCodec", messageCodec);
            ctx.pipeline().addLast(Capability.P2P, p2pHandler);

            p2pHandler.setChannel(this);
            p2pHandler.setHandshake(helloRemote, ctx);

            getNodeStatistics().rlpxHandshake.add();
        }
    }

    public void sendHelloMessage(ChannelHandlerContext ctx, FrameCodec frameCodec, String nodeId,
                                 HelloMessage inboundHelloMessage) throws IOException, InterruptedException {

        // in discovery mode we are supplying fake port along with fake nodeID to not receive
        // incoming connections with fake public key
        HelloMessage helloMessage = discoveryMode ? StaticMessages.createHelloMessage(nodeId, 9) :
                StaticMessages.createHelloMessage(nodeId);

        if (inboundHelloMessage != null && P2pHandler.isProtocolVersionSupported(inboundHelloMessage.getP2PVersion())) {
            // the p2p version can be downgraded if requested by peer and supported by us
            helloMessage.setP2pVersion(inboundHelloMessage.getP2PVersion());
        }

        byte[] payload = helloMessage.getEncoded();

        ByteBuf byteBufMsg = ctx.alloc().buffer();
        frameCodec.writeFrame(new FrameCodec.Frame(helloMessage.getCode(), payload), byteBufMsg);
        ctx.writeAndFlush(byteBufMsg).sync();

        if (logger.isInfoEnabled())
            logger.info("To: \t{} \tSend: \t{}", ctx.channel().remoteAddress(), helloMessage);
        getNodeStatistics().rlpxOutHello.add();
    }

    public void activateEth(ChannelHandlerContext ctx, TauVersion version) {
        TauHandler handler = ethHandlerFactory.create(version);
        MessageFactory messageFactory = createTauMessageFactory(version);
        messageCodec.setTauVersion(version);
        messageCodec.setTauMessageFactory(messageFactory);

        logger.info("Eth{} [ address = {} | id = {} ]", handler.getVersion(), inetSocketAddress, getPeerIdShort());

        ctx.pipeline().addLast(Capability.TAU, handler);

        handler.setMsgQueue(msgQueue);
        handler.setChannel(this);
        handler.setPeerDiscoveryMode(discoveryMode);

        handler.activate();

        tau = handler;
    }

    private MessageFactory createTauMessageFactory(TauVersion version) {
        switch (version) {
            case V60:   return new Tau60MessageFactory();
            case V61:   return new Tau61MessageFactory();
            case V62:   return new Tau62MessageFactory();
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    public NodeStatistics getNodeStatistics() {
        return nodeStatistics;
    }

    public void setNode(byte[] nodeId) {
        node = new Node(nodeId, inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        nodeStatistics = nodeManager.getNodeStatistics(node);
    }

    public Node getNode() {
        return node;
    }

    public void initMessageCodes(List<Capability> caps) {
        messageCodec.initMessageCodes(caps);
    }

    public boolean isProtocolsInitialized() {
        return tau.hasStatusPassed();
    }

    public void onDisconnect() {
    }

    public void onSyncDone() {
        tau.enableTransactions();
        tau.onSyncDone();
    }

    public boolean isDiscoveryMode() {
        return discoveryMode;
    }

    public String getPeerId() {
        return node == null ? "<null>" : node.getHexId();
    }

    public String getPeerIdShort() {
        return node == null ? "<null>" : node.getHexIdShort();
    }

    public byte[] getNodeId() {
        return node == null ? null : node.getId();
    }

    /**
     * Indicates whether this connection was initiated by our peer
     */
    public boolean isActive() {
        return isActive;
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return node == null ? null : new ByteArrayWrapper(node.getId());
    }

    public void disconnect(ReasonCode reason) {
        msgQueue.disconnect(reason);
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    // ETH sub protocol

    public boolean isEthCompatible(Channel peer) {
        return peer != null && peer.getTauVersion().isCompatible(getTauVersion());
    }

    public Tau getTauHandler() {
        return tau;
    }

    public boolean hasEthStatusSucceeded() {
        return tau.hasStatusSucceeded();
    }

    public void logSyncStats() {
        tau.logSyncStats();
    }

    public BigInteger getTotalDifficulty() {
        return nodeStatistics.getEthTotalDifficulty();
    }

    public void changeSyncState(SyncStateName newState) {
        tau.changeState(newState);
    }

    public boolean hasBlocksLack() {
        return tau.hasBlocksLack();
    }

    public void setMaxHashesAsk(int maxHashesAsk) {
        tau.setMaxHashesAsk(maxHashesAsk);
    }

    public int getMaxHashesAsk() {
        return tau.getMaxHashesAsk();
    }

    public void setLastHashToAsk(byte[] lastHashToAsk) {
        tau.setLastHashToAsk(lastHashToAsk);
    }

    public byte[] getLastHashToAsk() {
        return tau.getLastHashToAsk();
    }

    public byte[] getBestKnownHash() {
        return tau.getBestKnownHash();
    }

    public SyncStatistics getSyncStats() {
        return tau.getStats();
    }

    public boolean isHashRetrievingDone() {
        return tau.isHashRetrievingDone();
    }

    public boolean isHashRetrieving() {
        return tau.isHashRetrieving();
    }

    public boolean isIdle() {
        return tau.isIdle();
    }

    public void prohibitTransactionProcessing() {
        tau.disableTransactions();
    }

    /**
     * Sames as {@link #sendTransactionsCapped(List)}} but input list is randomly sliced to
     * contain not more than {@link #MAX_SAFE_TXS} if needed
     * @param txs   List of txs to send
     */
    public void sendTransactionsCapped(List<Transaction> txs) {
        List<Transaction> slicedTxs;
        if (txs.size() <= MAX_SAFE_TXS) {
            slicedTxs = txs;
        } else {
            slicedTxs = CollectionUtils.truncateRand(txs, MAX_SAFE_TXS);
        }
        tau.sendTransaction(slicedTxs);
    }

    public void sendTransaction(List<Transaction> tx) {
        tau.sendTransaction(tx);
    }

    public void sendNewBlock(Block block) {
        tau.sendNewBlock(block);
    }

    public void sendNewBlockHeader(BlockHeader header) {
        tau.sendNewBlockHeader(header);
    }

    public TauVersion getTauVersion() {
        return tau.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress) : channel.inetSocketAddress != null) return false;
        return !(node != null ? !node.equals(channel.node) : channel.node != null);

    }

    @Override
    public int hashCode() {
        int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", getPeerIdShort(), inetSocketAddress);
    }
}
