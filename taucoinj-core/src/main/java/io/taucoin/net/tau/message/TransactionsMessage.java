package io.taucoin.net.tau.message;

import io.taucoin.core.Transaction;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around an Ethereum Transactions message on the network
 *
 * @see TauMessageCodes#TRANSACTIONS
 */
public class TransactionsMessage extends TauMessage {

    private List<Transaction> transactions;

    public TransactionsMessage(byte[] encoded) {
        super(encoded);
    }

    public TransactionsMessage(Transaction transaction) {

        transactions = new ArrayList<>();
        transactions.add(transaction);
        parsed = true;
    }

    public TransactionsMessage(List<Transaction> transactionList) {
        this.transactions = transactionList;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        transactions = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpTxData = (RLPList) paramsList.get(i);
            Transaction tx = new Transaction(rlpTxData.getRLPData());
            transactions.add(tx);
        }
        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (Transaction tx : transactions)
            encodedElements.add(tx.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }


    public List<Transaction> getTransactions() {
        if (!parsed) parse();
        return transactions;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.TRANSACTIONS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) parse();
        final StringBuilder sb = new StringBuilder();
        if (transactions.size() < 8) {
            for (Transaction transaction : transactions)
                sb.append("\n   ").append(transaction);
        } else {
            for (int i = 0; i < 3; i++) {
                sb.append("\n   ").append(transactions.get(i));
            }
            sb.append("\n   ").append("[Skipped " + (transactions.size() - 6) + " transactions]");
            for (int i = transactions.size() - 3; i < transactions.size(); i++) {
                sb.append("\n   ").append(transactions.get(i));
            }
        }
        return "[" + getCommand().name() + " num:"
                + transactions.size() + " " + sb.toString() + "]";
    }
}