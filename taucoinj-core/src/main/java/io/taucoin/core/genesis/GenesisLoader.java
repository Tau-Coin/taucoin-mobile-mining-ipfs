package io.taucoin.core.genesis;

import com.google.common.io.ByteStreams;
import io.taucoin.core.Transaction;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Genesis;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.jsontestsuite.Utils;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

import static java.math.BigInteger.ZERO;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.ByteUtil.wrap;

public class GenesisLoader {

    public static Block loadGenesis() {
        return loadGenesis(CONFIG);
    }

    public static Genesis loadGenesis(SystemProperties config)  {
        String genesisFile = config.genesisInfo();

        InputStream is = GenesisLoader.class.getResourceAsStream("/genesis/" + genesisFile);
        return loadGenesis(is);
    }

    public static Genesis loadGenesis(InputStream genesisJsonIS)  {
        try {

            String json = new String(ByteStreams.toByteArray(genesisJsonIS));

            ObjectMapper mapper = new ObjectMapper();
            JavaType type = mapper.getTypeFactory().constructType(GenesisJson.class);
            long timebegin = System.currentTimeMillis();
            System.out.println("start time is:"+System.currentTimeMillis());
            GenesisJson genesisJson  = new ObjectMapper().readValue(json, type);
            System.out.println("consumption time is:"+(System.currentTimeMillis() - timebegin));
            Genesis genesis = createBlockForJson(genesisJson);
            genesis.setNumber(0);
            //set block signature
            genesis.setGenerationSignature(Hex.decode("442c29a4d18f192164006030640fb54c8b9ffd4f5750d2f6dca192dc653c52ad"));
            // Set genesis base target value and cumulative difficulty
            genesis.setBaseTarget(new BigInteger(
                    ByteUtil.removeHexPrefix(genesisJson.getGeneBasetarget()), 16));
            genesis.setCumulativeDifficulty(BigInteger.ZERO);

            Map<ByteArrayWrapper, AccountState> premine = generatePreMine(genesisJson.getAlloc());
            genesis.setPremine(premine);

            return genesis;
        } catch (Throwable e) {
            System.err.println("Genesis block configuration is corrupted or not found ./resources/genesis/...");
            e.printStackTrace();
            System.exit(-1);
        }

        System.err.println("Genesis block configuration is corrupted or not found ./resources/genesis/...");
        System.exit(-1);
        return null;
    }


    private static Genesis createBlockForJson(GenesisJson genesisJson){

        byte version       = Utils.parseByte(genesisJson.getVersion());
        byte[] baseTarget  = Utils.parseData(genesisJson.getGeneBasetarget());
        byte[] preheaderHash     = Utils.parseData(genesisJson.getPreviousHeaderHash());
        byte[][] coinbase    = Utils.parseHexArrayData(genesisJson.getCoinbase());

        byte[] timestampBytes = Utils.parseData(genesisJson.getTimestamp());
        long   timestamp         = ByteUtil.byteArrayToLong(timestampBytes);

        byte[] genePubkey  = Utils.parseData(genesisJson.getGeneratorPublicKey());
        byte[] geneSig   = Utils.parseData(genesisJson.getBlockSignature());

        byte option    = Utils.parseByte(genesisJson.getOption());
        //here is temporary method...
        List<Transaction> tr = new ArrayList<Transaction>();
        for(byte[] tcoinbase:coinbase) {
            tr.add(new Transaction(tcoinbase));
        }
        byte v = (byte) 27;
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(geneSig, 0, r, 0, 32);
        System.arraycopy(geneSig, 32, s, 0, 32);
        return new Genesis(version, timestampBytes, preheaderHash, v,
                            r,s, option, tr);
    }


    private static Map<ByteArrayWrapper, AccountState> generatePreMine(Map<String, AllocatedAccount> alloc){

        Map<ByteArrayWrapper, AccountState> premine = new HashMap<>();
        for (String key : alloc.keySet()){

            BigInteger balance = new BigInteger(alloc.get(key).getBalance());
            AccountState acctState = new AccountState(ZERO, balance);

            premine.put(wrap(Hex.decode(key)), acctState);
        }

        return premine;
    }

}
