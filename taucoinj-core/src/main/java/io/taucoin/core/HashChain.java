package io.taucoin.core;

import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.taucoin.crypto.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HashChain {
    private static final Logger logger = LoggerFactory.getLogger("hashchain");

    private Cid hashc = null;
    private List<byte[]> hashPairCidList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded;
    private boolean parsed = false;

    private HashChain() {
    }

    public HashChain(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.parsed = false;
    }

    public HashChain(List<byte[]> hashPairCidList) {
        this.hashPairCidList = hashPairCidList;
        if (this.hashPairCidList == null) {
            this.hashPairCidList = new CopyOnWriteArrayList<>();
        }
        this.parsed = true;
    }


    public byte[] getEncoded() {
        if (null == rlpEncoded) {
            byte[][] elements = new byte[hashPairCidList.size()][];
            int i = 0;
            for (byte[] bytes : hashPairCidList) {
                elements[i] = RLP.encodeElement(bytes);
                i++;
            }

            rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    private void parseRLP() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList cidList = (RLPList) params.get(0);
        int size = cidList.size();
        for (int i = 0; i < size; i++) {
            hashPairCidList.add(cidList.get(i).getRLPData());
        }
        parsed = true;
    }


    public Cid getHashC() {
        if (null == hashc) {
            byte[] encoded = getEncoded();
            Multihash multihash = new Multihash(Multihash.Type.sha2_256, HashUtil.sha256(encoded));
            hashc = Cid.buildCidV0(multihash);
        }
        return hashc;
    }

    public List<byte[]> getHashPairCidList() {

        if (!parsed) {
            parseRLP();
        }
        return hashPairCidList;
    }

}
