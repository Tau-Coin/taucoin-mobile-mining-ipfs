/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.module.presenter;

import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.naturs.logger.Logger;
import com.luck.picture.lib.entity.LocalMedia;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.ForumModel;
import io.taucoin.android.wallet.module.model.IForumModel;
import io.taucoin.android.wallet.module.view.forum.CommentAddActivity;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.MoneyValueFilter;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.EditInput;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class ForumPresenter {
    private IForumModel mForumModel;

    public ForumPresenter() {
        mForumModel = new ForumModel();
    }

    public void postMedia(ForumTopic forumTopic, LogicObserver<Boolean> observer) {
        mForumModel.postMedia(forumTopic, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                ToastUtils.showShortToast(R.string.forum_post_successfully);
                EventBusUtil.post(MessageEvent.EventCode.TOPIC_REFRESH);
                observer.onNext(true);
            }

            @Override
            public void handleError(int code, String msg) {
                ToastUtils.showShortToast(R.string.forum_post_failed);
                EventBusUtil.post(MessageEvent.EventCode.TOPIC_REFRESH);
                observer.onNext(false);
            }
        });
    }

    public void updateBookmark(String txId, int bookmark) {

        mForumModel.updateBookmark(txId, bookmark, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                EventBusUtil.post(MessageEvent.EventCode.BOOKMARK_REFRESH);
            }

            @Override
            public void handleError(int code, String msg) {
                EventBusUtil.post(MessageEvent.EventCode.BOOKMARK_REFRESH);
            }
        });
    }

    public void changeName() {
        ForumTopic forumTopic = new ForumTopic();
        forumTopic.setType(2);
        forumTopic.setFee(12);
        forumTopic.setUserName("yang");
        forumTopic.setContactInfo("T12345678");
        forumTopic.setProfile("News show");
        forumTopic.setTimeStamp(DateUtil.getTime());
        forumTopic.setTSender(MyApplication.getKeyValue().getAddress());
        mForumModel.postMedia(forumTopic, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                ToastUtils.showShortToast(R.string.forum_change_name_successfully);
                EventBusUtil.post(MessageEvent.EventCode.TOPIC_REFRESH);
            }

            @Override
            public void handleError(int code, String msg) {
                ToastUtils.showShortToast(R.string.forum_change_name_failed);
                EventBusUtil.post(MessageEvent.EventCode.TOPIC_REFRESH);
            }
        });
        ToastUtils.showShortToast(R.string.forum_change_name_successfully);
    }

    public void postComment(ForumTopic forumTopic, LogicObserver<Boolean> observer) {
        if(forumTopic == null){
            return;
        }
        if(StringUtil.isEmpty(forumTopic.getText())){
            ToastUtils.showShortToast(R.string.forum_comment_invalid);
            return;
        }
        forumTopic.setType(1);
        mForumModel.postMedia(forumTopic, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                observer.onNext(true);
                ToastUtils.showShortToast(R.string.forum_comment_successfully);
                EventBusUtil.post(MessageEvent.EventCode.COMMENT_REFRESH);
            }

            @Override
            public void handleError(int code, String msg) {
                observer.onNext(false);
                ToastUtils.showShortToast(R.string.forum_comment_failed);
                EventBusUtil.post(MessageEvent.EventCode.COMMENT_REFRESH);
            }
        });
    }

    public void getForumTopicList(int pageNo, String time, int bookmark, LogicObserver<List<ForumTopic>> observer) {
        mForumModel.getForumTopicList(pageNo, time, bookmark, observer);
    }

    public void getCommentList(int pageNo, String time, String replayId, LogicObserver<List<ForumTopic>> observer) {
        mForumModel.getCommentList(pageNo, time, replayId, observer);
    }

    public void getSearchTopicList(int pageNo, String time, String searchKey, LogicObserver<List<ForumTopic>> observer) {
        mForumModel.getSearchTopicList(pageNo, time, searchKey, observer);
    }

    public void picCompression(LocalMedia localMedia) {
        mForumModel.picCompression(localMedia);
    }

    public void audioCompression(LocalMedia localMedia) {
        mForumModel.audioCompression(localMedia);
    }

    public void videoCompression(LocalMedia localMedia) {
        mForumModel.videoCompression(localMedia);
    }

    public void uploadFileToLocalIPFS(MediaBean mediaBean, ForumTopic forumTopic, LogicObserver<Boolean> observer) {
        mForumModel.uploadMediaFile(mediaBean, new LogicObserver<String>() {
            @Override
            public void handleData(String hash) {
                Logger.d("uploadFileToLocalIPFS result hash=%s", hash);
                forumTopic.setHash(hash);
                postMedia(forumTopic, observer);
            }

            @Override
            public void handleError(int code, String msg) {
                ToastUtils.showShortToast(R.string.forum_post_failed);
                observer.onNext(false);
            }
        });
    }

    public void getMediaData(String hash, LogicObserver<String> observer) {
        mForumModel.getMediaData(hash, observer);
    }

    public void spamAddress(String tSender) {
        mForumModel.spamAddress(tSender);
    }

    public void getSpamList(int pageNo, String time, LogicObserver<List<Spammer>> observer) {
        mForumModel.getSpamList(pageNo, time, observer);
    }

    public void unSpamList(List<String> list, LogicObserver<Boolean> observer) {
        mForumModel.unSpamList(list, observer);
    }

    public void getMessageQue(int pageNo, String time, LogicObserver<List<ForumTopic>> observer) {
        mForumModel.getMessageQue(pageNo, time, observer);
    }

    public void deleteMessage(long id, LogicObserver<Boolean> observer) {
        mForumModel.deleteMessage(id, observer);
    }

    public void showEditFeeDialog(FragmentActivity activity, TextView tvFee, boolean isCanZero) {
        View view = LinearLayout.inflate(activity, R.layout.view_dialog_fee, null);
        EditText editText = view.findViewById(R.id.edit_text);
        TextView tvMedianFee = view.findViewById(R.id.tv_median_fee);
        editText.setTextAppearance(activity, R.style.style_normal_yellow);
        String medianFee = StringUtil.getTag(tvFee).trim();
        String oldFee = StringUtil.getText(tvFee).trim();
        editText.setText(oldFee);
        tvMedianFee.setText(medianFee);
        editText.setHint(R.string.forum_fee_enter_tip);
        editText.setFilters(new InputFilter[]{new MoneyValueFilter().setDigits(7).setEndSpace()});
        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setMaxLines(1);
        new CommonDialog.Builder(activity)
                .setContentView(view)
                .setButtonWidth(240)
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    String text = editText.getText().toString().trim();
                    if(StringUtil.isEmpty(text)){
                        ToastUtils.showShortToast(R.string.send_tx_invalid_fee);
                        return;
                    }
                    if(!isCanZero){
                        double fee = new BigDecimal(text).doubleValue();
                        if(fee <= 0){
                            ToastUtils.showShortToast(R.string.send_tx_invalid_fee);
                            return;
                        }
                    }
                    tvFee.setText(editText.getText().toString().trim());
                    dialog.cancel();

                }).create().show();
    }
}