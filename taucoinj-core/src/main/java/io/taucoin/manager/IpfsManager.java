package io.taucoin.manager;


import io.ipfs.api.IPFS;

public class IpfsManager {
    private static final String LOCAL_IPFS = "/ip4/127.0.0.1/tcp/5001";

    private IPFS ipfs;
    private IpfsManager() {
    }

    private static class IPFSHolder {
        private static final IpfsManager INSTANCE = new IpfsManager();
    }

    public static IpfsManager getInstance() {
        return IpfsManager.IPFSHolder.INSTANCE;
    }

    public void init() {
        ipfs = new IPFS(LOCAL_IPFS);
    }

    public IPFS getLocalIpfs() {
        if (null == ipfs) {
            init();
        }
        return ipfs;
    }
}
