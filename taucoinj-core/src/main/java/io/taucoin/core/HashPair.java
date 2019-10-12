package io.taucoin.core;

import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.taucoin.crypto.HashUtil;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class HashPair {
    private static final Logger logger = LoggerFactory.getLogger("hashpair");

    private long number;
    private Cid blockCid;
    private Cid previousHashPairCid;

    private byte[] hash = null;
    private Cid cid = null;

    private byte[] rlpEncoded;
    private boolean parsed = false;

    public HashPair(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.parsed = false;
    }

    public HashPair(long number, Cid blockCid, Cid previousHashPairCid) {
        this.number = number;
        this.blockCid = blockCid;
        this.previousHashPairCid = previousHashPairCid;
        this.parsed = true;
    }

    public void parseRLP() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList hashPair = (RLPList) params.get(0);

        byte[] nrBytes = hashPair.get(0).getRLPData();
        this.number = ByteUtil.byteArrayToLong(nrBytes);

        byte[] blockMultihashBytes = hashPair.get(1).getRLPData();
        Multihash blockMultihash = new Multihash(blockMultihashBytes);
        blockCid = Cid.buildCidV0(blockMultihash);

        byte[] previousHashPairMultihashBytes = hashPair.get(2).getRLPData();
        Multihash previousHashPairMultihash = new Multihash(previousHashPairMultihashBytes);
        previousHashPairCid = Cid.buildCidV0(previousHashPairMultihash);

        this.parsed = true;
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
            byte[] blockCid = RLP.encodeElement(this.blockCid.toBytes());
            byte[] previousHashPairCid = RLP.encodeElement(this.previousHashPairCid.toBytes());

            this.rlpEncoded = RLP.encodeList(number, blockCid, previousHashPairCid);
        }
        return rlpEncoded;
    }

    /**
     * get corresponding block number
     * @return
     */
    public long getNumber() {
        if (!parsed) {
            parseRLP();
        }
        return number;
    }

    /**
     * get block cid of corresponding height
     * @return
     */
    public Cid getBlockCid() {
        if (!parsed) {
            parseRLP();
        }
        return blockCid;
    }

    /**
     * get cid of previous hash pair
     * @return
     */
    public Cid getPreviousHashPairCid() {
        if (!parsed) {
            parseRLP();
        }
        return previousHashPairCid;
    }

    /**
     * sha2-256 hash
     * @return
     */
    public byte[] getHash() {
        if (!parsed) {
            parseRLP();
        }
        if (this.hash == null) {
            this.hash = HashUtil.sha256(this.getEncoded());
        }
        return this.hash;
    }

    /**
     * get cid
     * cid version:0, cid codec:DagProtobuf
     * @return
     */
    public Cid getCid() {
        if (this.cid == null) {
            Multihash multihash = new Multihash(Multihash.Type.sha2_256, getHash());
            cid = Cid.buildCidV0(multihash);
        }
        return cid;
    }
}
