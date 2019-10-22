package io.taucoin.ipfs.node;

import io.ipfs.api.Peer;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

/**
 * This class models a peer in the ipfs swarm network.
 */
public class IpfsPeerInfo {

    protected String host;
    protected int port;
    protected String peerId;

    public IpfsPeerInfo(String host, int port, String peerId) {
        this.host = host;
        this.port = port;
        this.peerId = peerId;
    }

    public IpfsPeerInfo(Peer peer) {
        init();
        parseMultiAddress(peer.address);
        parsePeerId(peer.id);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPeerId() {
        return peerId == null ? "" : peerId;
    }

    private void init() {
        this.host = "";
        this.port = -1;
        this.peerId = "";
    }

    private void parseMultiAddress(MultiAddress address) {
        this.host = address.getHost();
        if (address.isTCPIP()) {
            this.port = address.getTCPPort();
        }
    }

    private void parsePeerId(Multihash id) {
        this.peerId = id.toBase58();
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("ipfsPeerInfo: [ host=").append(getHost())
                .append(" port=").append(getPort())
                .append(" peerId=").append(getPeerId()).append("] \n");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        IpfsPeerInfo peerData = (IpfsPeerInfo) obj;
        return peerData.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port + peerId.hashCode();
        return result;
    }
}
