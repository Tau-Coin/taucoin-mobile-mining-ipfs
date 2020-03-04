package io.taucoin.android.wallet.module.bean;

import java.util.List;

public class VideoBean{
    private List<PicBean> pics;
    private AudioBean audio;

    public AudioBean getAudio() {
        return audio;
    }

    public void setAudio(AudioBean audio) {
        this.audio = audio;
    }

    public List<PicBean> getPics() {
        return pics;
    }

    public void setPics(List<PicBean> pics) {
        this.pics = pics;
    }
}
