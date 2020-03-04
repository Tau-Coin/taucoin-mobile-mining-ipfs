package io.taucoin.android.wallet.module.view.forum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.naturs.logger.Logger;
import com.google.gson.Gson;
import com.luck.picture.lib.PictureContextWrapper;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.dialog.PhotoItemSelectedDialog;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.tools.DateUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.ForumBaseActivity;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.GlideEngine;
import io.taucoin.android.wallet.util.MediaUtil;
import io.taucoin.android.wallet.util.NotchUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.FitStateUI;
import io.taucoin.foundation.util.StringUtil;

import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_FAIL;
import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_SUCCESS;

public class TopicAddActivity extends ForumBaseActivity implements PhotoItemSelectedDialog.OnItemClickListener {

    @BindView(R.id.ll_toolbar)
    RelativeLayout llToolbar;
    @BindView(R.id.media_view)
    View mediaView;
    @BindView(R.id.et_title)
    EditText etTitle;
    @BindView(R.id.et_text)
    EditText etText;

    private ViewHolder viewHolder;
    private MediaBean mediaBean;
    private int type = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_add);
        mPresenter = new ForumPresenter(this);
        ButterKnife.bind(this);
        initView();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PictureContextWrapper.wrap(newBase, LanguageConfig.ENGLISH));
    }

    // Initialize page view components
    private void initView() {
        NotchUtil.resetStatusBarOrNotchHeight(llToolbar);
        FitStateUI.setStatusBarDarkIcon(this, true);

        mediaView.setVisibility(View.GONE);
        viewHolder = new ViewHolder(mediaView);
    }

    @OnClick({R.id.iv_cancel, R.id.tv_post, R.id.iv_camera, R.id.iv_library, R.id.iv_voice})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_cancel:
                ToolbarView.handleLeftBack(view);
                break;
            case R.id.tv_post:
                postTopic();
                break;
            case R.id.iv_camera:
                cameraClick();
                break;
            case R.id.iv_library:
                MediaUtil.libraryClick(this);
                break;
            case R.id.iv_voice:
                MediaUtil.voiceClick(this);
                break;
            default:
                break;
        }
    }

    private void postTopic() {
        ForumTopic forumTopic = new ForumTopic();
        String title = etTitle.getText().toString().trim();
        String text = etText.getText().toString().trim();
        if(type == -1 && StringUtil.isNotEmpty(text)){
            type = 0;
        }
        forumTopic.setTitle(title);
        forumTopic.setType(type);
        forumTopic.setFee(0);
        forumTopic.setTimeStamp(DateUtil.getTime());
        forumTopic.setSender(MyApplication.getKeyValue().getAddress());
        switch (type){
            case 0:
                forumTopic.setText(text);
                mPresenter.postMedia(forumTopic);
                break;
            case 1:
            case 2:
            case 3:
                mPresenter.uploadFileToLocalIPFS(mediaBean, forumTopic);
                break;
            default:
                break;
        }
        ToastUtils.showShortToast(R.string.forum_post_successfully);
    }

    @Override
    public void onItemClick(int position) {
        switch (position) {
            case PhotoItemSelectedDialog.IMAGE_CAMERA:
                // Photograph
                MediaUtil.startOpenCamera(this);
                break;
            case PhotoItemSelectedDialog.VIDEO_CAMERA:
                // Video recording
                MediaUtil.startOpenCameraVideo(this);
                break;
            default:
                break;
        }
    }

    private void cameraClick() {
        PhotoItemSelectedDialog selectedDialog = PhotoItemSelectedDialog.newInstance();
        selectedDialog.setOnItemClickListener(this);
        selectedDialog.show(getSupportFragmentManager(), "PhotoItemSelectedDialog");
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if(object.getCode() == COMPRESSION_SUCCESS){
            if(object.getData() != null){
                mediaBean = (MediaBean) object.getData();
                Logger.d("compression success=" + new Gson().toJson(mediaBean));
            }
        }else if(object.getCode() == COMPRESSION_FAIL){
            if(object.getData() != null){
                ToastUtils.showShortToast(object.getData().toString());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case PictureConfig.CHOOSE_REQUEST:
                case PictureConfig.REQUEST_CAMERA:
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    if(selectList != null && selectList.size() == 1){
                        LocalMedia localMedia = selectList.get(0);
                        if(PictureMimeType.eqImage(localMedia.getMimeType())){
                            type = 1;
                            mPresenter.picCompression(localMedia);
                        }if(PictureMimeType.eqAudio(localMedia.getMimeType())){
                            type = 2;
                            mPresenter.audioCompression(localMedia);
                        }else if(PictureMimeType.eqVideo(localMedia.getMimeType())){
                            type = 3;
                            mPresenter.videoCompression(localMedia);
                        }
                        showMediaView(localMedia);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void showMediaView(LocalMedia media) {
        mediaView.setVisibility(View.VISIBLE);
        etText.setVisibility(View.GONE);
        viewHolder.mIvDel.setVisibility(View.VISIBLE);
        viewHolder.mIvDel.setOnClickListener(v -> {
            etText.setVisibility(View.VISIBLE);
            mediaView.setVisibility(View.GONE);
            viewHolder.mImg.setImageResource(R.mipmap.ic_launcher);

            String text = StringUtil.getText(etText);
            if(StringUtil.isEmpty(text)){
                type = -1;
            }else{
                type = 0;
            }
        });
        viewHolder.itemView.setOnClickListener(v -> {
            onPreview(media);
        });

        if (media == null || TextUtils.isEmpty(media.getPath())) {
            return;
        }
        int chooseModel = media.getChooseModel();
        String path;
        if (media.isCut() && !media.isCompressed()) {
            // Cut out
            path = media.getCutPath();
        } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
            // Compressed, or cropped at the same time, subject to the final compressed image
            path = media.getCompressPath();
        } else {
            // Original image
            path = media.getPath();
        }

        Logger.i("Original image url::" + media.getPath());

        if (media.isCut()) {
            Logger.i("Cut image url::" + media.getCutPath());
        }
        if (media.isCompressed()) {
            Logger.i("Compressed image url::" + media.getCompressPath());
            Logger.i("File size:: width=%s, height=%s", media.getWidth(), media.getHeight());
            Logger.i("Compressed file size::" + new File(media.getCompressPath()).length() / 1024 + "kb");
            Logger.i("Original file size::" + new File(media.getPath()).length() / 1024 + "kb");
        }
        if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
            Logger.i("Android Q specific url::" + media.getAndroidQToPath());
        }
        if (media.isOriginal()) {
            Logger.i("Whether to turn on the original drawing function::" + true);
            Logger.i("Url after opening the original image function::" + media.getAndroidQToPath());
        }

        long duration = media.getDuration();
        viewHolder.tvDuration.setVisibility(PictureMimeType.eqVideo(media.getMimeType())
                ? View.VISIBLE : View.GONE);
        if (chooseModel == PictureMimeType.ofAudio()) {
            viewHolder.tvDuration.setVisibility(View.VISIBLE);
            viewHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                    (R.drawable.picture_icon_audio, 0, 0, 0);

        } else {
            viewHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds
                    (R.drawable.picture_icon_video, 0, 0, 0);
        }
        viewHolder.tvDuration.setText(DateUtils.formatDurationTime(duration));
        if (chooseModel == PictureMimeType.ofAudio()) {
            viewHolder.mImg.setImageResource(R.drawable.picture_audio_placeholder);
        } else {
            Glide.with(viewHolder.itemView.getContext())
                    .load(path.startsWith("content://") && !media.isCut() && !media.isCompressed() ? Uri.parse(path)
                            : path)
                    .centerCrop()
                    .placeholder(R.color.app_color_f6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.mImg);
        }
//        String originMimeType = PictureMimeType.getMimeType(new File(media.getPath()));
//        Logger.i("originMimeType::" + originMimeType);
//        if(PictureMimeType.eqVideo(originMimeType)){
////            VideoCapture videoCapture = new VideoCaptureConfig.Builder()
////                    .set
////                    .build();
//        }
    }

    class ViewHolder{
        @BindView(R.id.fiv)
        ImageView mImg;
        @BindView(R.id.iv_del)
        ImageView mIvDel;
        @BindView(R.id.tv_duration)
        TextView tvDuration;
        View itemView;

        public ViewHolder(View view) {
            itemView = view;
            ButterKnife.bind(this, view);
        }
    }

    private void onPreview(LocalMedia media){
        if (null != media) {
            String mimeType = media.getMimeType();
            int mediaType = PictureMimeType.getMimeType(mimeType);
            if(mediaType == PictureConfig.TYPE_IMAGE){
                List<LocalMedia> selectList = new ArrayList<>();
                selectList.add(media);
                PictureSelector.create(TopicAddActivity.this)
                        .themeStyle(R.style.picture_default_style)
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .isNotPreviewDownload(true)
                        .loadImageEngine(GlideEngine.createGlideEngine())
                        .openExternalPreview(0, selectList);
            }
        }
    }
}