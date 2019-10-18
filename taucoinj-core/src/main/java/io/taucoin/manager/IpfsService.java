package io.taucoin.manager;

import io.taucoin.core.Transaction;
import io.taucoin.http.tau.message.NewTxMessage;
import io.taucoin.ipfs.config.Topic;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

import io.ipfs.api.IPFS;
import io.ipfs.api.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IpfsService {

    private static final Logger logger = LoggerFactory.getLogger("IpfsService");

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    // temp home node id, just for test.
    private static final String HOME_NODE_ID = "id";

    private IpfsHomeNodeInfo ipfsHomeNodeInfo = null;

    private IPFS ipfs;

    @Inject
    public IpfsService() {
        init();
    }

    private void init() {
        ipfs = new IPFS(LOCAL_IPFS);
    }

    public boolean sendTransaction(Transaction tx) {
        if (tx == null) {
            logger.warn("send null transaction");
            return false;
        }

        String txPayload = new NewTxMessage(tx).toJsonString();
        try {
            ipfs.pubsub.pub(Topic.getTransactionId(HOME_NODE_ID), txPayload);
        } catch (Exception e) {
            logger.error("pub tx exception: {}", e);
            return false;
        }

        return true;
    }

    public IpfsHomeNodeInfo getIpfsHomeNode() {
        if (ipfsHomeNodeInfo != null) {
            return ipfsHomeNodeInfo;
        }

        String id = "";
        String version = "";

        try {
            Map idMap = ipfs.id();
            logger.info("Ipfs home node id:{}", idMap);
            if (idMap != null) {
                id = (String)idMap.get("ID");
            }
        } catch (IOException ioe) {
            logger.error("getId ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("getId exception: {}", e);
        }

        try {
            version = ipfs.version();
        } catch (IOException ioe) {
            logger.error("get version ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("get version exception: {}", e);
        }

        ipfsHomeNodeInfo = new IpfsHomeNodeInfo(id, version);
        logger.info("Ipfs home node: {}", ipfsHomeNodeInfo);

        return ipfsHomeNodeInfo;
    }

    public List<IpfsPeerInfo> getPeers() {
        List<IpfsPeerInfo> peers = new ArrayList<IpfsPeerInfo>();
        List<Peer> swarmPeers = null;

        try {
            swarmPeers = ipfs.swarm.peers();
        } catch (IOException ioe) {
            logger.error("getPeers ioexception: {}", ioe);
        } catch (Exception e) {
            logger.error("getPeers exception: {}", e);
        }

        if (swarmPeers != null && swarmPeers.size() > 0) {
            for (Peer p : swarmPeers) {
                peers.add(new IpfsPeerInfo(p));
            }
        }

        return peers;
    }
}
