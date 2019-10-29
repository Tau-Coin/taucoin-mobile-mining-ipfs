package io.taucoin.facade;

import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

import io.ipfs.api.IPFS;

import java.util.List;

public interface IpfsAPI {

    boolean sendTransaction(Transaction tx);

    boolean sendNewBlock(Block block);

    IpfsHomeNodeInfo getIpfsHomeNode();

    List<IpfsPeerInfo> getPeers();
}
