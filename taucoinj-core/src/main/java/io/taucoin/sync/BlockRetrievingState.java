package io.taucoin.sync;

import static io.taucoin.net.tau.TauVersion.V61;
import static io.taucoin.net.tau.TauVersion.V62;
import static io.taucoin.sync.SyncStateName.*;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public class BlockRetrievingState extends AbstractSyncState {

    public BlockRetrievingState() {
        super(BLOCK_RETRIEVING);
    }

    @Override
    public void doOnTransition() {

        super.doOnTransition();

        syncManager.pool.changeState(BLOCK_RETRIEVING);
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        if (!syncManager.queue.isMoreBlocksNeeded()) {
            syncManager.changeState(IDLE);
            return;
        }
        
        if ((syncManager.queue.isHashesEmpty()  || !syncManager.pool.hasCompatible(V61)) &&
            (syncManager.queue.isHeadersEmpty() || !syncManager.pool.hasCompatible(V62))) {

            syncManager.changeState(IDLE);
            return;
        }

        syncManager.pool.changeStateForIdles(BLOCK_RETRIEVING);
    }
}
