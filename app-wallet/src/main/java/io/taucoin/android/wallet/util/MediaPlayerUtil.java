package io.taucoin.android.wallet.util;

import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.SeekBar;

import com.github.naturs.logger.Logger;
import com.luck.picture.lib.tools.DateUtils;

import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.module.view.forum.TopicAdapter;
import io.taucoin.foundation.util.StringUtil;

public class MediaPlayerUtil implements SeekBar.OnSeekBarChangeListener {
    private static MediaPlayerUtil instance;
    private MediaPlayer mediaPlayer;
    private TopicAdapter.ViewHolder viewHolder;
    private boolean isPlayAudio = false;
    private boolean isInit = false;
    private boolean isNeedResume = false;
    private String pathTemp;
    private Class classZ;

    public static MediaPlayerUtil getInstance() {
        if (instance == null) {
            synchronized (MediaPlayerUtil.class) {
                if (instance == null) {
                    instance = new MediaPlayerUtil();
                }
            }
        }
        return instance;
    }

    private MediaPlayerUtil(){
        mediaPlayer = new MediaPlayer();
    }

    private void initPlayer(TopicAdapter.ViewHolder viewHolder, String path) {
        this.viewHolder = viewHolder;
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            playAudio();
        } catch (Exception e) {
            Logger.e(e, "initPlayer error");
            ToastUtils.showShortToast("initPlayer error");
        }
    }

    public void playOrPause(TopicAdapter.ViewHolder viewHolder, String path) {
        try {
            isNeedResume = false;
            if(isInit){
                if(StringUtil.isNotSame(pathTemp, path)){
                    pathTemp = path;
                    stop(path);
                    this.viewHolder.audioSeekBar.setProgress(0);
                    this.viewHolder.ivPlayPause.setImageResource(R.mipmap.icon_post);
                    this.viewHolder = viewHolder;
                    viewHolder.audioSeekBar.setOnSeekBarChangeListener(this);
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.icon_add);
                }
                playOrPause();
                return;
            }
            isInit = true;
            initPlayer(viewHolder, path);
        } catch (Exception e) {
            Logger.e(e, "initPlayer error");
            ToastUtils.showShortToast("initPlayer error");
        }
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mediaPlayer != null) {
                    if(viewHolder != null){
                        viewHolder.tvPlayTime.setText(DateUtils.formatDurationTime(mediaPlayer.getCurrentPosition()));
                        viewHolder.audioSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                        viewHolder.audioSeekBar.setMax(mediaPlayer.getDuration());
                        viewHolder.tvTotalTime.setText(DateUtils.formatDurationTime(mediaPlayer.getDuration()));
                    }
                    handler.postDelayed(runnable, 200);
                }
            } catch (Exception e) {
                Logger.e(e, "runnable error");
            }
        }
    };

    private void playAudio() {
        if (mediaPlayer != null) {
            viewHolder.audioSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            viewHolder.audioSeekBar.setMax(mediaPlayer.getDuration());
        }

        playOrPause();

        if (!isPlayAudio) {
            handler.post(runnable);
            isPlayAudio = true;
        }
    }

    public void stop(String path) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            } catch (Exception e) {
                Logger.e(e, "stop error");
            }
        }
    }

    private void playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.icon_post);
                } else {
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.icon_add);
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            Logger.e(e, "playOrPause error");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mediaPlayer.seekTo(progress);
        }
        viewHolder.audioSeekBar.setProgress(progress);
        if(progress == seekBar.getMax()){
            viewHolder.audioSeekBar.setProgress(0);
            viewHolder.ivPlayPause.setImageResource(R.mipmap.icon_post);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void pause(Class classZ) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    this.classZ = classZ;
                    isNeedResume = true;
                    playOrPause();
                }
            }
        } catch (Exception e) {
            Logger.e(e, "playOrPause error");
        }
    }

    public void resume(Class classZ) {
        if(isNeedResume && this.classZ != null && this.classZ.equals(classZ)){
            isNeedResume = false;
            if (!mediaPlayer.isPlaying()) {
                playOrPause();
            }
        }
    }

    public void destroyView(int pageType) {
        if(viewHolder != null && viewHolder.pageType == pageType){
            isNeedResume = false;
            viewHolder = null;
        }
    }

    public void destroy() {
        if (mediaPlayer != null && handler != null) {
            isInit = false;
            handler.removeCallbacks(runnable);
            mediaPlayer.release();
            mediaPlayer = null;
            viewHolder = null;
        }
    }
}