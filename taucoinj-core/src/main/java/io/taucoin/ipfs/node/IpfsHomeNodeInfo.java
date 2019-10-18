package io.taucoin.ipfs.node;

/**
 * This class models the local ipfs node.
 */
public class IpfsHomeNodeInfo {

    protected String id;
    protected String version;

    public IpfsHomeNodeInfo(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("IpfsHomeNodeInfo: [ id=").append(getId())
                .append(" version=").append(getVersion()).append("] \n");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        IpfsHomeNodeInfo peerData = (IpfsHomeNodeInfo) obj;
        return peerData.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return id.hashCode() + version.hashCode();
    }
}
