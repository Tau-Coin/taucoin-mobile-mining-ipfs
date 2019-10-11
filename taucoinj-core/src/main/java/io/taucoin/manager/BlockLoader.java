package io.taucoin.manager;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.Blockchain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class BlockLoader {

    private static final Logger logger = LoggerFactory.getLogger("blockqueue");

    SystemProperties config = SystemProperties.CONFIG;

    protected Blockchain blockchain;

    Scanner scanner = null;

    @Inject
    public BlockLoader(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void loadBlocks(){

        String fileSrc = config.blocksLoader();
        try {
            FileInputStream inputStream = null;
            inputStream = new FileInputStream(fileSrc);
            scanner = new Scanner(inputStream, "UTF-8");

            System.out.println("Loading blocks: " + fileSrc);

            while (scanner.hasNextLine()) {

                byte[] blockRLPBytes = Hex.decode( scanner.nextLine());
                Block block = new Block(blockRLPBytes);

                long t1 = System.nanoTime();
                if (block.getNumber() >= blockchain.getBestBlock().getNumber()){

                    //if (block.getNumber() > 0 && !isValid(block.getHeader())) {
                    if (block.getNumber() > 0) {
                        break;
                    };

                    blockchain.tryToConnect(block);
                    long t1_ = System.nanoTime();

                    float elapsed = ((float)(t1_ - t1) / 1_000_000);

                    if (block.getNumber() % 1000 == 0 || elapsed > 10_000) {
                        String result = String.format("Imported block #%d took: [%02.2f msec]",
                                block.getNumber(), elapsed);

                        System.out.println(result);
                    }
                } else{

                    if (block.getNumber() % 10000 == 0)
                        System.out.println("Skipping block #" + block.getNumber());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(" * Done * ");
    }
}
