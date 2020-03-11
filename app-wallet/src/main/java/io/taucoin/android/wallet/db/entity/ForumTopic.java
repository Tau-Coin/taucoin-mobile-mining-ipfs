package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

/**
 * @version 1.0
 * Forum topic
 */
@Entity
public class ForumTopic {
    @Id
    private Long id;
    private String chainid;
    private String txId;
    private String blockHash;
    private long nounce;
    private long timeStamp;
    private String tSender;
    private String isender;
    private String relayMa;
    private long fee;

    private String title;
    private String text;
    private int type;
    private String hash;

    // personal info
    private String contactInfo;
    private String userName;
    private String profile;

    // message/comment
    private String referId;
    private String intro;
    private String content;

    @Transient
    private int commentCount;
    @Transient
    private long tauTotal;
    @Generated(hash = 1104854910)
    public ForumTopic(Long id, String chainid, String txId, String blockHash,
            long nounce, long timeStamp, String tSender, String isender,
            String relayMa, long fee, String title, String text, int type,
            String hash, String contactInfo, String userName, String profile,
            String referId, String intro, String content) {
        this.id = id;
        this.chainid = chainid;
        this.txId = txId;
        this.blockHash = blockHash;
        this.nounce = nounce;
        this.timeStamp = timeStamp;
        this.tSender = tSender;
        this.isender = isender;
        this.relayMa = relayMa;
        this.fee = fee;
        this.title = title;
        this.text = text;
        this.type = type;
        this.hash = hash;
        this.contactInfo = contactInfo;
        this.userName = userName;
        this.profile = profile;
        this.referId = referId;
        this.intro = intro;
        this.content = content;
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
    public String getChainid() {
        return this.chainid;
    }
    public void setChainid(String chainid) {
        this.chainid = chainid;
    }
    public String getTxId() {
        return this.txId;
    }
    public void setTxId(String txId) {
        this.txId = txId;
    }
    public String getBlockHash() {
        return this.blockHash;
    }
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    public long getNounce() {
        return this.nounce;
    }
    public void setNounce(long nounce) {
        this.nounce = nounce;
    }
    public long getTimeStamp() {
        return this.timeStamp;
    }
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getTSender() {
        return this.tSender;
    }
    public void setTSender(String tSender) {
        this.tSender = tSender;
    }
    public String getIsender() {
        return this.isender;
    }
    public void setIsender(String isender) {
        this.isender = isender;
    }
    public String getRelayMa() {
        return this.relayMa;
    }
    public void setRelayMa(String relayMa) {
        this.relayMa = relayMa;
    }
    public long getFee() {
        return this.fee;
    }
    public void setFee(long fee) {
        this.fee = fee;
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
    public String getContactInfo() {
        return this.contactInfo;
    }
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    public String getUserName() {
        return this.userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getProfile() {
        return this.profile;
    }
    public void setProfile(String profile) {
        this.profile = profile;
    }
    public String getReferId() {
        return this.referId;
    }
    public void setReferId(String referId) {
        this.referId = referId;
    }
    public String getIntro() {
        return this.intro;
    }
    public void setIntro(String intro) {
        this.intro = intro;
    }
    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public long getTauTotal() {
        return tauTotal;
    }

    public void setTauTotal(long tauTotal) {
        this.tauTotal = tauTotal;
    }
}