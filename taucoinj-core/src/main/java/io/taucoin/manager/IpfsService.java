package io.taucoin.manager;

import io.ipfs.api.IPFS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IpfsService {

    private static final Logger logger = LoggerFactory.getLogger("IpfsService");

    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    private IPFS ipfs;

    @Inject
    public IpfsService() {
        init();
    }

    private void init() {
        ipfs = new IPFS(LOCAL_IPFS);
    }
}
