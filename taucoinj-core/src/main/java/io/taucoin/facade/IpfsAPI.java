package io.taucoin.facade;

import io.taucoin.core.Transaction;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

import io.ipfs.api.IPFS;

import java.util.List;

public interface IpfsAPI {

    IPFS getLocalIpfs();

    boolean sendTransaction(Transaction tx);

    IpfsHomeNodeInfo getIpfsHomeNode();

    List<IpfsPeerInfo> getPeers();
}
