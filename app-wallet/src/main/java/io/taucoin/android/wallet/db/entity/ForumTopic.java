package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @version 1.0
 * Forum topic
 */
@Entity
public class ForumTopic {
    @Id
    private Long id;
    private String title;
    private String text;
    private long fee;
    private String sender;
    private long timeStamp;
    private String referId;
    private int type;
    private String hash;
    @Generated(hash = 1232029847)
    public ForumTopic(Long id, String title, String text, long fee, String sender,
            long timeStamp, String referId, int type, String hash) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.fee = fee;
        this.sender = sender;
        this.timeStamp = timeStamp;
        this.referId = referId;
        this.type = type;
        this.hash = hash;
    }
    @Generated(hash = 1734309132)
    public ForumTopic() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getText() {
        return this.text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public long getFee() {
        return this.fee;
    }
    public void setFee(long fee) {
        this.fee = fee;
    }
    public String getSender() {
        return this.sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    public long getTimeStamp() {
        return this.timeStamp;
    }
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getReferId() {
        return this.referId;
    }
    public void setReferId(String referId) {
        this.referId = referId;
    }
    public int getType() {
        return this.type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getHash() {
        return this.hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
}