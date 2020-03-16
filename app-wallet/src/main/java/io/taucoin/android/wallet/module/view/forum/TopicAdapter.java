package io.taucoin.android.wallet.module.view.forum;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.naturs.logger.Logger;
import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.DateUtils;
import com.lwy.righttopmenu.MenuItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.ForumBaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.bean.AudioBean;
import io.taucoin.android.wallet.module.bean.PicBean;
import io.taucoin.android.wallet.module.bean.VideoBean;
import io.taucoin.android.wallet.net.service.IpfsRPCManager;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.GlideEngine;
import io.taucoin.android.wallet.util.MediaPlayerUtil;
import io.taucoin.android.wallet.util.PopupMenuUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.ForumComment;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class TopicAdapter extends BaseAdapter {

    private List<ForumTopic> list = new ArrayList<>();
    private ForumBaseActivity activity;
    private static boolean isNormalModel;
    private int pageType;

    public TopicAdapter(ForumBaseActivity activity, int pageType) {
        this.pageType = pageType;
        isNormalModel = ForumUtil.isNormalModel();
        this.activity = activity;
    }

    public void switchBrowseModel(){
        isNormalModel = ForumUtil.isNormalModel();
        notifyDataSetChanged();
    }

    public void setListData(List<ForumTopic> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ForumTopic bean = list.get(position);
        handleItemView(activity, viewHolder, bean, pageType);
        return convertView;
    }

    static void handleDetailView(ForumBaseActivity activity, ViewHolder viewHolder, ForumTopic bean){
        isNormalModel = ForumUtil.isNormalModel();
        handleItemView(activity, viewHolder, bean, 3);
    }

    /**
     * handle list, search and detailed data of topic
     * type: 1 : list
     *       2 : search
     *       3 : detailed
     * */
    private static void handleItemView(ForumBaseActivity activity, ViewHolder viewHolder, ForumTopic bean, int pageType){
        if(bean == null){
            return;
        }
        viewHolder.tvText.setVisibility(View.GONE);
        viewHolder.ivPic.setVisibility(View.GONE);
        viewHolder.rlAudio.setVisibility(View.GONE);

        viewHolder.pageType = pageType;

        if(bean.getType() == 1){
            handlePicView(activity, viewHolder, bean);
        }else if(bean.getType() == 2){
            handleAudioView(activity, viewHolder, bean);
        }else if(bean.getType() == 3){
            handleVideoView(activity, viewHolder, bean);
        }else{
            viewHolder.tvText.setVisibility(View.VISIBLE);
            viewHolder.ivPic.setVisibility(View.GONE);
            viewHolder.tvText.setText(bean.getText());
        }
        viewHolder.tvTitle.setText(bean.getTitle());

        String communityName = activity.getString(R.string.forum_community);
        String userName = activity.getString(R.string.forum_posted);
        String appName = activity.getString(R.string.app_name);
        communityName = String.format(communityName, appName);
        userName = String.format(userName, ForumUtil.getUserName(bean.getUserName(), bean.getTSender()));
        viewHolder.tvCommunityName.setText(communityName);
        viewHolder.tvUsername.setText(userName);
        viewHolder.forumComment.setData(bean.getCommentCount(), bean.getTauTotal());

        if(pageType != 3){
            viewHolder.rootView.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.putExtra(TransmitKey.DATA, new Gson().toJson(bean));
                ActivityUtil.startActivity(intent, activity, TopicDetailActivity.class);});
        }
//        viewHolder.llPermalink.setVisibility(pageType == 2 ? View.GONE : View.VISIBLE);
//        viewHolder.llPermalink.setOnClickListener(v -> ActivityUtil.startActivity(activity, PermalinkActivity.class));
        viewHolder.llPermalink.setVisibility(View.GONE);
        viewHolder.tvMore.setOnClickListener(v -> showMenuItem(activity, v, bean));
        viewHolder.tvUsername.setOnClickListener(v -> showPersonalInfo(activity, bean));
    }

    private static void handlePicView(ForumBaseActivity activity, ViewHolder viewHolder, ForumTopic bean) {
        if(isNormalModel){
            viewHolder.ivPic.setVisibility(View.VISIBLE);
            viewHolder.ivPic.setTag("");
            viewHolder.ivPic.setOnClickListener(v -> {
                String url = StringUtil.getTag(v);
                if(StringUtil.isNotEmpty(url)){
                    onPreview(activity, url);
                }
            });
            if(activity.mPresenter != null){
                Logger.d("mediaData=%s", bean.getHash());
                activity.mPresenter.getMediaData(bean.getHash(), new LogicObserver<String>(){

                    @Override
                    public void handleData(String jsonData) {
                        String mediaUrl = "";
                        if(StringUtil.isNotEmpty(jsonData)){
                            PicBean pic = new Gson().fromJson(jsonData, PicBean.class);
                            mediaUrl = IpfsRPCManager.FILE_IPFS + pic.getHash();
                        }
                        viewHolder.ivPic.setTag(mediaUrl);
                        Logger.d("handleData mediaUrl=%s", mediaUrl);
                        Glide.with(activity)
                                .load(mediaUrl)
                                .placeholder(R.mipmap.ic_placeholder)
                                .error(R.mipmap.ic_error)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(viewHolder.ivPic);
                    }

                    @Override
                    public void handleError(int code, String msg) {
                        Logger.d("handleError=%s", msg);
                        viewHolder.ivPic.setImageResource(R.mipmap.ic_error);
                    }
                });
            }
        }
    }

    private static void handleAudioView(ForumBaseActivity activity, ViewHolder viewHolder, ForumTopic bean) {
        if(isNormalModel){
            viewHolder.rlAudio.setVisibility(View.VISIBLE);
            viewHolder.audioSeekBar.setProgress(0);
            if(activity.mPresenter != null){
                Logger.d("mediaData=%s", bean.getHash());
                activity.mPresenter.getMediaData(bean.getHash(), new LogicObserver<String>(){

                    @Override
                    public void handleData(String jsonData) {
                        String mediaUrl = "";
                        long duration = 0;
                        if(StringUtil.isNotEmpty(jsonData)){
                            AudioBean audio = new Gson().fromJson(jsonData, AudioBean.class);
                            mediaUrl = IpfsRPCManager.FILE_IPFS + audio.getHash();
                            duration = audio.getDuration();
                        }
                        viewHolder.tvPlayTime.setText(DateUtils.formatDurationTime(0));
                        viewHolder.tvTotalTime.setText(DateUtils.formatDurationTime(duration));
                        Logger.d("handleData mediaUrl=%s, duration=%s", mediaUrl, duration);
                        String audioUrl = mediaUrl;
                        viewHolder.ivPlayPause.setOnClickListener(v -> MediaPlayerUtil.getInstance().playOrPause(viewHolder, audioUrl));
                    }

                    @Override
                    public void handleError(int code, String msg) {
                        Logger.d("handleError=%s", msg);
                    }
                });
            }
        }
    }

    private static void handleVideoView(ForumBaseActivity activity, ViewHolder viewHolder, ForumTopic bean) {
        if(isNormalModel){
            viewHolder.rlAudio.setVisibility(View.VISIBLE);
            viewHolder.audioSeekBar.setProgress(0);
            if(activity.mPresenter != null){
                Logger.d("mediaData=%s", bean.getHash());
                activity.mPresenter.getMediaData(bean.getHash(), new LogicObserver<String>(){

                    @Override
                    public void handleData(String jsonData) {
                        String mediaUrl = "";
                        long duration = 0;
                        if(StringUtil.isNotEmpty(jsonData)){
                            VideoBean video = new Gson().fromJson(jsonData, VideoBean.class);
                            AudioBean audio = video.getAudio();
                            mediaUrl = IpfsRPCManager.FILE_IPFS + audio.getHash();
                            duration = audio.getDuration();
                        }
                        viewHolder.tvPlayTime.setText(DateUtils.formatDurationTime(0));
                        viewHolder.tvTotalTime.setText(DateUtils.formatDurationTime(duration));
                        Logger.d("handleData mediaUrl=%s, duration=%s", mediaUrl, duration);
                        String audioUrl = mediaUrl;
                        viewHolder.ivPlayPause.setOnClickListener(v -> MediaPlayerUtil.getInstance().playOrPause(viewHolder, audioUrl));
                    }

                    @Override
                    public void handleError(int code, String msg) {
                        Logger.d("handleError=%s", msg);
                    }
                });
            }
        }
    }

    public static class ViewHolder {
        @BindView(R.id.tv_community_name)
        TextView tvCommunityName;
        @BindView(R.id.tv_username)
        TextView tvUsername;
        @BindView(R.id.tv_permalink_coins)
        TextView tvPermalinkCoins;
        @BindView(R.id.ll_permalink)
        LinearLayout llPermalink;
        @BindView(R.id.tv_title)
        TextView tvTitle;
        @BindView(R.id.tv_text)
        TextView tvText;
        @BindView(R.id.iv_pic)
        ImageView ivPic;
        @BindView(R.id.tv_play_time)
        public TextView tvPlayTime;
        @BindView(R.id.tv_total_time)
        public TextView tvTotalTime;
        @BindView(R.id.audio_seek_bar)
        public SeekBar audioSeekBar;
        @BindView(R.id.rl_audio)
        View rlAudio;
        @BindView(R.id.iv_play_pause)
        public ImageView ivPlayPause;
        @BindView(R.id.tv_more)
        public TextView tvMore;
        @BindView(R.id.forum_comment)
        public ForumComment forumComment;
        View rootView;

        public int pageType;

        ViewHolder(View view) {
            rootView = view;
            ButterKnife.bind(this, view);
        }
    }

    private static void onPreview(FragmentActivity activity, String url){
        LocalMedia media = new LocalMedia();
        media.setPath(url);
        media.setCut(false);
        media.setCompressed(false);
        List<LocalMedia> selectList = new ArrayList<>();
        selectList.add(media);
        PictureSelector.create(activity)
                .themeStyle(R.style.picture_default_style)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .isNotPreviewDownload(true)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .openExternalPreview(0, selectList);
    }


    private static void showMenuItem(ForumBaseActivity activity, View view, ForumTopic bean){
        if(bean == null){
            return;
        }
        List<MenuItem> menuItems = new ArrayList<>();
        int bookmark = bean.getBookmark() == 0 ? R.mipmap.icon_bmark_no : R.mipmap.icon_bmark_yes;
        menuItems.add(new MenuItem(bookmark, "Bookmark"));
        menuItems.add(new MenuItem(R.mipmap.icon_ipfs, "IPFS Address"));
        PopupMenuUtil.showMenuItem(activity, view, menuItems, pos -> {
            ToastUtils.showShortToast(bean.getTSender() + pos);
            switch (pos){
                case 0:
                    if(activity != null && activity.mPresenter != null){
                        int newBookmark = bean.getBookmark() == 0 ? 1 : 0;
                        bean.setBookmark(newBookmark);
                        activity.mPresenter.updateBookmark(bean.getTxId(), newBookmark);
                    }
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        });
    }

    private static void showPersonalInfo(ForumBaseActivity activity, ForumTopic bean) {
        View view = LinearLayout.inflate(activity, R.layout.view_dialog_personal_info, null);
        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvTelegram = view.findViewById(R.id.tv_telegram);
        TextView tvInfo = view.findViewById(R.id.tv_info);
        tvName.setText(ForumUtil.getUserName(bean.getUserName(), bean.getTSender()));
        tvTelegram.setText(bean.getContactInfo());
        tvInfo.setText(bean.getProfile());
        new CommonDialog.Builder(activity)
                .setContentView(view)
                .setButtonWidth(240)
                .setPositiveButton(R.string.send_dialog_spammer, (dialog, which) -> {
                    dialog.cancel();
                    if(activity != null && activity.mPresenter != null){
                        activity.mPresenter.spamAddress(bean.getTSender());
                    }
                }).create().show();
    }
}