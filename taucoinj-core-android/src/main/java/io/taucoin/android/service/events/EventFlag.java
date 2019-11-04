package io.taucoin.android.service.events;

public enum EventFlag {

    /**
     * Listen to all events
     */
    EVENT_ALL,
    
    /**
     * Trace messages event
     */
    EVENT_TRACE,

    /**
     * onBlock event
     */
    EVENT_BLOCK,

    /**
     * onBlockConnected event
     * The block with max diffculity is connected to main chain.
     */
    EVENT_BLOCK_CONNECT,

    /**
     * onBlockDisconnected event
     */
    EVENT_BLOCK_DISCONNECT,

    /**
     * Received message event
     */
    EVENT_RECEIVE_MESSAGE,

    /**
     * Send message event
     */
    EVENT_SEND_MESSAGE,

    /**
     * Peer disconnect event
     */
    EVENT_PEER_DISCONNECT,

    /**
     * Pending transactions received event
     */
    EVENT_PENDING_TRANSACTIONS_RECEIVED,

    /**
     * Sync done event
     */
    EVENT_SYNC_DONE,

    /**
     * No connections event
     */
    EVENT_NO_CONNECTIONS,

    /**
     * Peer handshake event
     */
    EVENT_HANDSHAKE_PEER,

    /**
     * VM trace created event
     */
    EVENT_VM_TRACE_CREATED,

    /**
     * Create taucoin blockchain successfully
     */
    EVENT_TAUCOIN_CREATED,

    /**
     * Taucoin blockchain has been created previously.
     */
    EVENT_TAUCOIN_EXIST,

    /**
     * Start sync blocks evnet.
     */
    EVENT_START_SYNC,

    /**
     * Start sync blocks but has sync done.
     */
    EVENT_HAS_SYNC_DONE,

    /**
     * Next block forged time internal event.
     */
    EVENT_BLOCK_FORGED_TIME_INTERNAL,
    /**
     * forge exit event
     */
    EVENT_BLOCK_FORGE_STOP,

    /**
     * new transaction on block have an effect on chain state
     */
    EVENT_TRANSACTION_EXECUATED,

    /**
     * Send or receive http payload event.
     */
    EVENT_NETWORK_TRAFFIC,

    /**
     * Chain information changed event.
     */
    EVENT_CHAININFO_CHANGED,

    /**
     * Stop sync blocks evnet.
     */
    EVENT_STOP_SYNC,

    /**
     * Creating taucoin blockchain failed due to db corruption.
     */
    EVENT_TAUCOIN_DB_CORRUPTION,

    /**
     * Blocks downloaded from peer.
     */
    EVENT_BLOCKS_DOWNLOADED,

    /**
     *  Next block forged POT detail event.
     */
    EVENT_BLOCK_FORGED_POT_DETAIL,

    /**
     * Start downloading blocks evnet.
     */
    EVENT_START_DOWNLOAD,

    /**
     * Stop downloading blocks evnet.
     */
    EVENT_STOP_DOWNLOAD,

    /**
     * States loading progress.
     */
    EVENT_STATES_LOADED,

    /**
     * States loaded successfully.
     */
    EVENT_STATES_LOADED_COMPLETED,

    /**
     * States loaded successfully.
     */
    EVENT_STATES_LOADED_FAILED,

    /**
     * Ipfs daemon dead.
     */
    EVENT_IPFS_DAEMON_DEAD,

    /**
     * Hashpair Synchronized.
     */
    EVENT_HASHPAIR_SYNCHRONIZED

}
