package io.taucoin.android.wallet.module.bean;

import java.util.List;

public class RawTxList {

    private int status;
    private String message;
    private List<RawTxBean> records;

    public List<RawTxBean> getRecords() {
        return records;
    }

    public void setRecords(List<RawTxBean> records) {
        this.records = records;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
