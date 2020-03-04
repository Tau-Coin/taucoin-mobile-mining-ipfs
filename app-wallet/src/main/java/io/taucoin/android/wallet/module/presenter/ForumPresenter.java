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

import com.github.naturs.logger.Logger;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.FragmentActivity;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.android.wallet.module.model.ForumModel;
import io.taucoin.android.wallet.module.model.IForumModel;
import io.taucoin.foundation.net.callback.LogicObserver;

public class ForumPresenter {
    private IForumModel mForumModel;
//    private LifecycleProvider<ActivityEvent> provider;

    public ForumPresenter(FragmentActivity activity) {
        mForumModel = new ForumModel();
//        provider = activity;
    }

    public ForumPresenter() {
        mForumModel = new ForumModel();
//        provider = activity;
    }

    public void postMedia(ForumTopic forumTopic) {
        mForumModel.postMedia(forumTopic);
    }

    public void getForumTopicList(LogicObserver<List<ForumTopic>> observer) {
        mForumModel.getForumTopicList(observer);
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

    public void uploadFileToLocalIPFS(MediaBean mediaBean, ForumTopic forumTopic) {
        mForumModel.uploadMediaFile(mediaBean, new LogicObserver<String>() {
            @Override
            public void handleData(String hash) {
                Logger.d("uploadFileToLocalIPFS result hash=%s", hash);
                forumTopic.setHash(hash);
                postMedia(forumTopic);
            }

            @Override
            public void handleError(int code, String msg) {
                super.handleError(code, msg);
            }
        });
    }

    public void getMediaData(String hash, LogicObserver<String> observer) {
        mForumModel.getMediaData(hash, observer);
    }
}