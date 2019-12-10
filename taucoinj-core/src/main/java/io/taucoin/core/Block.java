package io.taucoin.core;

import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.ECKey.ECDSASignature;
import io.taucoin.crypto.HashUtil;
import io.taucoin.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The block in taucoin is the collection of relevant pieces of information
 * (known as the blockheader), H, together with information corresponding to
 * the comprised transactions, T.
 */
public class Block {

    private static final Logger logger = LoggerFactory.getLogger("block");

    //8 bits, keeping version is for upgrade to define the transition grace peroid
    private byte version;
    //32 bits, unix time stamp related to block
    private byte[] timeStamp;
    /**
     *ensure the integrity of the block 520 bits
     *the elliptic curve signature
     **/
    private ECDSASignature blockSignature;

    //160 bits, The SHA256 previous block header and RIPEMD 160 second 160 bits
    private byte[] previousHeaderHash;
    private byte[] blockhash = null;

    private Cid cid = null;

    /* Transactions */
    private List<Transaction> transactionsList = new CopyOnWriteArrayList<>();

    /*this is left for future use 8 bits*/
    private byte option;
    /* A scalar value equal to the number of ancestor blocks.
     * The genesis block has a number of zero */
    private long number;
    private BigInteger baseTarget; //this is uint64 type so here we should use compact type
    private byte[] generationSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO; //this is total chain difficulty
    private BigInteger cumulativeFee = BigInteger.ZERO;

    private byte[] stateRoot;

    private byte[] forgerPubkey = null;
    private byte[] forgerAddress = null;

    protected byte[] rlpEncoded;
    private byte[] rlpEncodedMsg;
    private byte[] rlpEncodedCache = null;
    private byte[] rlpRaw;
    private boolean isMsg = false;
    private boolean parsed = false;

    /* Constructors */

    private Block() {
    }

    public Block(byte[] rawData) {
        logger.debug("new from [" + Hex.toHexString(rawData) + "]");
        this.rlpEncoded = rawData;
        this.rlpEncodedMsg = null;
        this.rlpRaw = null;
        this.parsed = false;
    }

    public Block(byte[] rawData, boolean isMsg) {
        logger.debug("new from net [" + Hex.toHexString(rawData) + "]");
        if (isMsg) {
            this.rlpEncoded = null;
            this.rlpEncodedMsg = rawData;
            this.rlpRaw = null;
        } else {
            this.rlpEncoded = rawData;
            this.rlpEncodedMsg = null;
            this.rlpRaw = null;
        }
        this.parsed = false;
        this.isMsg = isMsg;
    }

    public Block(byte version, byte[] timestamp, byte[] previousHeaderHash,
                 byte option, List<Transaction> transactionsList) {
        this.version = version;
        this.timeStamp = timestamp;
        this.previousHeaderHash = previousHeaderHash;
        this.option = option;

        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }

    public Block(byte version, byte[] timestamp, byte[] previousHeaderHash, byte v,
                 byte[] r, byte[] s, byte option,
                 List<Transaction> transactionsList) {
        this.version = version;
        this.timeStamp = timestamp;
        this.previousHeaderHash = previousHeaderHash;
        this.blockSignature = ECDSASignature.fromComponents(r,s,v);

        this.option = option;
        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }


    private void parseRLP() {

        if (!isMsg) {
            RLPList params = RLP.decode2(rlpEncoded);
            RLPList block = (RLPList) params.get(0);

            // Parse block
            this.version = block.get(0).getRLPData() == null ? (byte) 0 : block.get(0).getRLPData()[0];
            this.timeStamp = block.get(1).getRLPData();
            RLPList signature = (RLPList) RLP.decode2(block.get(2).getRLPData()).get(0);
            // Parse blockSignature
            byte[] r = signature.get(0).getRLPData();
            byte[] s = signature.get(1).getRLPData();
            byte v = signature.get(2).getRLPData()[0];
            this.blockSignature = ECDSASignature.fromComponents(r, s,v);

            this.previousHeaderHash = block.get(3).getRLPData();
            byte[] nrBytes = block.get(4).getRLPData();
            this.number = nrBytes == null ? 0 : (new BigInteger(1, nrBytes)).longValue();

            byte[] btBytes = block.get(5).getRLPData();
            this.baseTarget = new BigInteger(1, btBytes);

            this.generationSignature = block.get(6).getRLPData();

            byte[] cyBytes = block.get(7).getRLPData();
            this.cumulativeDifficulty = cyBytes == null ? BigInteger.ZERO
                    : new BigInteger(1, cyBytes);

            byte[] culFee  = block.get(8).getRLPData();
            this.cumulativeFee = culFee == null ? BigInteger.ZERO
                    : new BigInteger(1,culFee);

            this.forgerPubkey = block.get(9).getRLPData();

            // Parse option
            this.option = block.get(10).getRLPData() == null ? (byte) 0 : block.get(10).getRLPData()[0];

            this.stateRoot = block.get(11).getRLPData();

            if(block.size() > 12) {
                // Parse Transactions
                RLPList txTransactions = (RLPList) block.get(12);
                this.parseTxs(txTransactions, true);
            }
        } else {
            RLPList params = RLP.decode2(rlpEncodedMsg);
            RLPList block = (RLPList) params.get(0);
            // Parse block
            this.version = block.get(0).getRLPData() == null ? (byte) 0 : block.get(0).getRLPData()[0];
            this.timeStamp = block.get(1).getRLPData();
            RLPList signature = (RLPList) RLP.decode2(block.get(2).getRLPData()).get(0);

            // Parse blockSignature
            byte[] r = signature.get(0).getRLPData();
            byte[] s = signature.get(1).getRLPData();
            byte v = signature.get(2).getRLPData()[0];
            this.blockSignature = ECDSASignature.fromComponents(r, s,v);

            this.previousHeaderHash = block.get(3).getRLPData();
            // Parse option
            this.option = block.get(4).getRLPData() == null ? (byte) 0 : block.get(4).getRLPData()[0];
            // Parse Transactions
            if(block.size() > 5){
                RLPList txTransactions = (RLPList) block.get(5);
                this.parseTxs(txTransactions, false);
            }
        }

        this.parsed = true;
    }

    /**
     * this situation blocks and tx are from another store
     * although isMsg is false,block is not verified by block chain
     * which is msg treated.
     */
    public void rlpSyncParseDisk(){

        //todo
    }

    public boolean isMsg() {
        return isMsg;
    }

    /**
     * Indicate this block is from network
     */
    public void setIsMsg(boolean isMsg) {
        this.isMsg = isMsg;
    }

    @Deprecated
    public BlockHeader getHeader() {
        if (!parsed) parseRLP();
        return null;
    }

    public byte[] getHash() {
        if (!parsed) parseRLP();
        if (this.blockhash == null) {
            this.blockhash = HashUtil.ripemd160(HashUtil.sha256(this.getHashEncoded()));
        }
        return this.blockhash;
    }

    /**
     * get block cid
     * cid version:0, cid codec:DagProtobuf
     * @return
     */
    public Cid getCid() {
        if (this.cid == null) {
            byte[] encoded = getEncodedMsg();
            Multihash multihash = new Multihash(Multihash.Type.sha2_256, HashUtil.sha256(encoded));
            cid = Cid.buildCidV0(multihash);
        }
        return cid;
    }

    public byte[] getPreviousHeaderHash() {
        if (!parsed) parseRLP();
        return this.previousHeaderHash;
    }

    public byte[] getTimestamp() {
        if (!parsed) parseRLP();
        return this.timeStamp;
    }

    public boolean extractForgerPublicKey() {
        ECKey key;
        try{
            key = ECKey.signatureToKey(this.getRawHash(), blockSignature.toBase64());
            if(key != null){
                forgerPubkey = key.getCompressedPubKey();
            } else {
                return false;
            }
        }catch (SignatureException e){
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public byte[] getForgerPublicKey() {
        if (forgerPubkey == null) {
            if (!parsed) parseRLP();
            extractForgerPublicKey();
        }
        return forgerPubkey;
    }

    public byte[] getForgerAddress() {
        if (forgerAddress == null) {
            ECKey key = ECKey.fromPublicOnly(getForgerPublicKey());
            forgerAddress = key.getAddress();
        }
        return forgerAddress;
    }

    public byte getVersion() {
        if (!parsed) parseRLP();
        return this.version;
    }

    public ECDSASignature getblockSignature(){
        if (!parsed) parseRLP();
        return this.blockSignature;
    }

    public byte getOption() {
        if (!parsed) parseRLP();
        return this.option;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public long getNumber() {
        if (!parsed) parseRLP();
        return this.number;
    }

    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = baseTarget;
    }

    public BigInteger getBaseTarget() {
        if (!parsed) parseRLP();
        return this.baseTarget;
    }

    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = generationSignature;
    }

    public byte[] getGenerationSignature() {
        if (!parsed) parseRLP();
        return this.generationSignature;
    }

    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public BigInteger getCumulativeDifficulty() {
        if (!parsed) parseRLP();
        return this.cumulativeDifficulty;
    }
    public BigInteger getCumulativeFee() {
        if (!parsed) parseRLP();
        return cumulativeFee;
    }

    public void setCumulativeFee(BigInteger cumulativeFee) {
        this.cumulativeFee = cumulativeFee;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public byte[] getStateRoot() {
        if (!parsed) parseRLP();
        return this.stateRoot;
    }

    public List<Transaction> getTransactionsList() {
        if (!parsed) parseRLP();
        return transactionsList;
    }


    private StringBuffer toStringBuff = new StringBuffer();
    // [parent_hash, uncles_hash, coinbase, state_root, tx_trie_root,
    // difficulty, number, minGasPrice, gasLimit, gasUsed, timestamp,
    // extradata, nonce]

    @Override
    public String toString() {

        if (!parsed) parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append(Hex.toHexString(this.getEncodedMsg())).append("\n");
        toStringBuff.append("BlockData [ ");
        toStringBuff.append("hash=" + ByteUtil.toHexString(this.getHash())).append("\n");
//        toStringBuff.append(header.toString());
//        toStringBuff.append("blocksig=" + ByteUtil.toHexString(this.blockSignature)).append("\n");
//        toStringBuff.append("option=" + ByteUtil.toHexString(this.option)).append("\n");
        toStringBuff.append("\nTransactions [\n");
        for (Transaction tx : getTransactionsList()) {
            toStringBuff.append("\n");
            toStringBuff.append(tx.toString());
        }
        toStringBuff.append("]");
        toStringBuff.append("\n]");

        return toStringBuff.toString();
    }

    public String toFlatString() {
        if (!parsed) parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append("BlockData [");
        toStringBuff.append("hash=").append(ByteUtil.toHexString(this.getHash()));

        for (Transaction tx : getTransactionsList()) {
            toStringBuff.append("\n");
            toStringBuff.append(tx.toString());
        }

        toStringBuff.append("]");
        return toStringBuff.toString();
    }

    private void parseTxs(RLPList txTransactions, boolean isComposite) {

        for (int i = 0; i < txTransactions.size(); i++) {
            RLPElement transactionRaw = txTransactions.get(i);
            this.transactionsList.add(new Transaction(transactionRaw.getRLPData(), isComposite));
        }
    }

    /**
     * check if param block is son of this block
     *
     * @param block - possible a son of this
     * @return - true if this block is parent of param block
     */
    public boolean isParentOf(Block block) {
        return Arrays.areEqual(this.getHash(), block.getPreviousHeaderHash());
    }

    public boolean isGenesis() {
        return false;
    }

    public boolean isEqual(Block block) {
        return Arrays.areEqual(this.getHash(), block.getHash());
    }

    private byte[] getSignatureEncoded() {
        if (!parsed) parseRLP();
        byte[] r, s,v;
        r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(blockSignature.r));
        s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(blockSignature.s));
        v = RLP.encodeByte(blockSignature.v);
        return RLP.encodeList(r, s,v);
    }

    private byte[] getOptionEncoded() {
        byte[] option = RLP.encodeByte(this.option);
        return option;
    }

    private byte[] getFullTransactionsEncoded() {

        byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncodedComposite();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }

    private byte[] getTransactionsEncoded() {

        byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncoded();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }

    private byte[] getTransactionEncodedForCache() {
        byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncodedForCache();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }

    //encode key items of block to get hash finger print
    private byte[] getHashEncoded(){
        byte[] version = RLP.encodeByte(this.version);
        byte[] timestamp = RLP.encodeElement(this.timeStamp);
        byte[] signature = getSignatureEncoded();
        byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);
        return RLP.encodeList(version, timestamp, signature,previousHeaderHash);
    }

    //encode block on disk
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(this.timeStamp);
            byte[] signature = getSignatureEncoded();
            byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);

            List<byte[]> block = getFullBodyElements();
            block.add(0, version);
            block.add(1,timestamp);
            block.add(2,signature);
            block.add(3,previousHeaderHash);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    //encode block on net
    public byte[] getEncodedMsg() {
        if (rlpEncodedMsg == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(this.timeStamp);
            byte[] signature = getSignatureEncoded();
            byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);

            List<byte[]> block = getBodyElements();
            block.add(0, version);
            block.add(1,timestamp);
            block.add(2,signature);
            block.add(3,previousHeaderHash);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncodedMsg = RLP.encodeList(elements);
        }
        return rlpEncodedMsg;
    }

    //encode block from net into mapdb cache
    public byte[] getEncodedCacheData() {
        if (rlpEncodedCache == null) {
            if (!parsed) parseRLP();
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(this.timeStamp);
            byte[] signature = getSignatureEncoded();
            byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);

            List<byte[]> block = getBodyElementsCache();
            block.add(0, version);
            block.add(1,timestamp);
            block.add(2,signature);
            block.add(3,previousHeaderHash);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncodedCache = RLP.encodeList(elements);
        }
        return rlpEncodedCache;
    }

    //encode block for signature
    public byte[] getEncodedRaw() {
        if (rlpRaw == null) {
            byte[] version = RLP.encodeByte(this.version);
            byte[] timestamp = RLP.encodeElement(this.timeStamp);
            byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);

            List<byte[]> block = getBodyElementsWithoutBlockSignature();
            block.add(0, version);
            block.add(1,timestamp);
            block.add(2,previousHeaderHash);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpRaw = RLP.encodeList(elements);
        }
        return rlpRaw;
    }


    public byte[] getEncodedBody() {
        List<byte[]> body = getBodyElements();
        byte[][] elements = body.toArray(new byte[body.size()][]);
        return RLP.encodeList(elements);
    }

    private List<byte[]> getBodyElementsWithoutBlockSignature() {
        if (!parsed) parseRLP();

        byte[] option = RLP.encodeByte(this.option);
        byte[] transactions = getTransactionEncodeForBlockSig();

        List<byte[]> body = new ArrayList<>();
        body.add(option);
        body.add(transactions);

        return body;
    }

    private byte[] getTransactionEncodeForBlockSig() {
        byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncodeForSig();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }
    private List<byte[]> getBodyElements() {
        if (!parsed) parseRLP();

        byte[] option = getOptionEncoded();
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(option);
        body.add(transactions);

        return body;
    }

    private List<byte[]> getBodyElementsCache() {
        if (!parsed) parseRLP();

        byte[] option = getOptionEncoded();
        byte[] transactions = getTransactionEncodedForCache();

        List<byte[]> body = new ArrayList<>();
        body.add(option);
        body.add(transactions);

        return body;
    }
    private List<byte[]> getFullBodyElements() {
        if (!parsed) parseRLP();

        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        if (this.baseTarget == null) {
            throw new IllegalArgumentException("baseTarget is null when encode");
        }
        byte[] baseTarget = RLP.encodeBigInteger(this.baseTarget);
        byte[] generationSignature = RLP.encodeElement(this.generationSignature);
        byte[] cumulativeDifficulty = RLP.encodeBigInteger(this.cumulativeDifficulty == null ? BigInteger.valueOf(0xffffff):this.cumulativeDifficulty);
        byte[] cumulativeFee = RLP.encodeBigInteger(this.cumulativeFee == null ? BigInteger.ZERO: this.cumulativeFee);
        byte[] forgerpubkey = RLP.encodeElement(this.forgerPubkey);
        byte[] option = getOptionEncoded();
        byte[] stateRootEncoded = RLP.encodeElement(this.stateRoot);
        byte[] transactions = getFullTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(number);
        body.add(baseTarget);
        body.add(generationSignature);
        body.add(cumulativeDifficulty);
        body.add(cumulativeFee);
        body.add(forgerpubkey);
        body.add(option);
        body.add(stateRootEncoded);
        body.add(transactions);

        return body;
    }

    public String getShortHash() {
        if (!parsed) parseRLP();
        return Hex.toHexString(getHash()).substring(0, 6);
    }

    public byte[] getRawHash() {
        if (!parsed) parseRLP();
        byte[] plainMsg = this.getEncodedRaw();
        return HashUtil.sha3(plainMsg);
    }

    public void sign(byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        byte[] hash = this.getRawHash();
        ECKey key = ECKey.fromPrivate(privKeyBytes).decompress();
        this.blockSignature = key.sign(hash);
        this.rlpEncoded = null;
        this.rlpEncodedMsg = null;
    }

    public String getShortDescr() {
        return "#" + getNumber() + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + Hex.toHexString(getPreviousHeaderHash()).substring(0,6) + ") Txs:" + getTransactionsList().size();
    }

    public static class Builder {

        private BlockHeader header;
        private byte[] body;
        // Is from network or disk?
        private boolean isMsg = false;

        public Builder withHeader(BlockHeader header) {
            this.header = header;
            return this;
        }

        public Builder withBody(byte[] body ,boolean isMsg) {
            this.body = body;
            this.isMsg = isMsg;
            return this;
        }

        public Block create() {
            if (header == null || body == null) {
                return null;
            }
            //tempory support simplied pure block 
            if(this.isMsg){
                Block block = new Block();
                //block.header = header;
                block.setIsMsg(true);
                block.parsed = true;
                RLPList items = (RLPList) RLP.decode2(body).get(0);
                RLPList signature = (RLPList) items.get(0);
                byte[] r = signature.get(0).getRLPData();
                byte[] s = signature.get(1).getRLPData();
                block.blockSignature = ECDSASignature.fromComponents(r, s);
                block.option = items.get(1).getRLPData()[0];
                RLPList transactions = (RLPList) items.get(2);
                //RLPList transactions = (RLPList) items.get(0);
                if (transactions.size() == 0){

                } else{
                   block.parseTxs(transactions, false);
                }
               //delete txState may be stupid....
               //we avoid trie,because we think block header doesn't have large capacity
                return block;
            }else{
                return null;
            }
        }
    }
}

