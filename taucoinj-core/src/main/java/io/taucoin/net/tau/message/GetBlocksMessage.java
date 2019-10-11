package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum GetBlocks message on the network
 *
 * @see TauMessageCodes#GET_BLOCKS
 */
public class GetBlocksMessage extends TauMessage {

    /**
     * List of block hashes for which to retrieve the blocks
     */
    private List<byte[]> blockHashes;

    public GetBlocksMessage(byte[] encoded) {
        super(encoded);
    }

    public GetBlocksMessage(List<byte[]> blockHashes) {
        this.blockHashes = blockHashes;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockHashes = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            blockHashes.add(paramsList.get(i).getRLPData());
        }
        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (byte[] hash : blockHashes)
            encodedElements.add(RLP.encodeElement(hash));
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }


    @Override
    public Class<BlocksMessage> getAnswerMessage() {
        return BlocksMessage.class;
    }

    public List<byte[]> getBlockHashes() {
        if (!parsed) parse();
        return blockHashes;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.GET_BLOCKS;
    }

    public String getDetailedString(){

        StringBuilder stringBuilder = new StringBuilder();

        for (byte[] hash : getBlockHashes()){
            stringBuilder.append(Hex.toHexString(hash)).append("\n");
        }

        return "[" + this.getCommand().name() + "\n" + stringBuilder.toString() + "]";
    }


    public String toString() {
        if (!parsed) parse();

        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blockHashes.size()).append(" ) ");

        if (logger.isDebugEnabled()) {
            for (byte[] hash : blockHashes) {
                payload.append(Hex.toHexString(hash)).append(" | ");
            }
            if (!blockHashes.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            payload.append(Utils.getHashListShort(blockHashes));
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
