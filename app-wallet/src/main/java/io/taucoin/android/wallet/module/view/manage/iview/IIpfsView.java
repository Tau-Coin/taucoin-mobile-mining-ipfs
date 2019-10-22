package io.taucoin.android.wallet.module.view.manage.iview;

import java.util.List;

import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

public interface IIpfsView {
    void loadPeerData(List<IpfsPeerInfo> peers);
    void loadHomeNodeData(IpfsHomeNodeInfo homeNode);
}
