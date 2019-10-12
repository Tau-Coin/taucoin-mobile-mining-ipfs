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

public class TransactionCidList {
    private static final Logger logger = LoggerFactory.getLogger("txCidlist");

    private Cid cid = null;
    private List<byte[]> txCidList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded;
    private boolean parsed = false;

    private TransactionCidList() {
    }

    public TransactionCidList(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.parsed = false;
    }

    public TransactionCidList(List<byte[]> txCidList) {
        this.txCidList = txCidList;
        if (this.txCidList == null) {
            this.txCidList = new CopyOnWriteArrayList<>();
        }
        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (null == rlpEncoded) {
            byte[][] elements = new byte[txCidList.size()][];
            int i = 0;
            for (byte[] bytes : txCidList) {
                logger.info("cid:{}", Cid.cast(bytes).toString());
                elements[i] = RLP.encodeElement(bytes);
                i++;
            }

            rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    private void parseRLP() {
        logger.info("rlp encode:{}", rlpEncoded);
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList cidList = (RLPList) params.get(0);
        int size = cidList.size();
        for (int i = 0; i < size; i++) {
            txCidList.add(cidList.get(i).getRLPData());
        }
        parsed = true;
    }


    public Cid getCid() {
        if (null == cid) {
            byte[] encoded = getEncoded();
            Multihash multihash = new Multihash(Multihash.Type.sha2_256, HashUtil.sha256(encoded));
            cid = Cid.buildCidV0(multihash);
        }
        return cid;
    }

    public List<byte[]> getTxCidList() {

        if (!parsed) {
            parseRLP();
        }
        return txCidList;
    }
}
