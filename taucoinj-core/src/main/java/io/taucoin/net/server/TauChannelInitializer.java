package io.taucoin.net.server;

import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
public class TauChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    ChannelManager channelManager;

    Provider<Channel> channelProvider;

    private String remoteId;

    private boolean peerDiscoveryMode = false;

    @Inject
    public TauChannelInitializer(ChannelManager channelManager, Provider<Channel> channelProvider, String remoteId) {
        this.channelManager = channelManager;
        this.channelProvider = channelProvider;
        this.remoteId = remoteId;
    }

    @Override
    public void initChannel(NioSocketChannel ch) throws Exception {
        try {
            if (!peerDiscoveryMode) {
                logger.info("Open {} connection, channel: {}", isInbound() ? "inbound" : "outbound", ch.toString());
            }

            if (isInbound() && channelManager.isRecentlyDisconnected(ch.remoteAddress().getAddress())) {
                // avoid too frequent connection attempts
                logger.info("Drop connection - the same IP was disconnected recently, channel: {}", ch.toString());
                ch.disconnect();
                return;
            }

            final Channel channel = channelProvider.get();
            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode);

            if(!peerDiscoveryMode) {
                channelManager.add(channel);
            }

            // limit the size of receiving buffer to 1024
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(16_777_216));
            ch.config().setOption(ChannelOption.SO_RCVBUF, 16_777_216);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

            // be aware of channel closing
            ch.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!peerDiscoveryMode) {
                        channelManager.notifyDisconnect(channel);
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
        }
    }

    private boolean isInbound() {
        return remoteId == null || remoteId.isEmpty();
    }

    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }
}
