package io.taucoin.manager;

import io.taucoin.core.Transaction;
import io.taucoin.http.tau.message.NewTxMessage;
import io.taucoin.ipfs.config.Topic;

import io.ipfs.api.IPFS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IpfsService {

    private static final Logger logger = LoggerFactory.getLogger("IpfsService");

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    // temp home node id, just for test.
    private static final String HOME_NODE_ID = "id";

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
}
