package io.taucoin.forge;

import io.taucoin.core.Block;

/**
 * Created by Anton Nashatyrev on 10.12.2015.
 */
public interface ForgerListener {

    void forgingStarted();

    void forgingStopped(ForgeStatus status);

    void blockForgingStarted(Block block);

    void nextBlockForgedInternal(long internal);

    void nextBlockForgedDetail(NextBlockForgedDetail detail);

    void blockForged(Block block);

    void blockForgingCanceled(Block block);
}
