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
package io.taucoin.android.wallet.module.model;

import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface IForumModel {

    void postMedia(ForumTopic forumTopic, LogicObserver<Boolean> observer);

    void updateBookmark(String txId, int bookmark, LogicObserver<Boolean> observer);

    void getForumTopicList(int pageNo, String time, int bookmark, LogicObserver<List<ForumTopic>> observer);

    void getCommentList(int pageNo, String time, String replayId, LogicObserver<List<ForumTopic>> observer);

    void getSearchTopicList(int pageNo, String time, String searchKey, LogicObserver<List<ForumTopic>> observer);

    void picCompression(LocalMedia localMedia);

    void uploadMediaFile(MediaBean mediaBean, LogicObserver<String> observer);

    void getMediaData(String hash, LogicObserver<String> observer);

    void audioCompression(LocalMedia localMedia);

    void videoCompression(LocalMedia localMedia);

    void spamAddress(String tSender);

    void getSpamList(int pageNo, String time, LogicObserver<List<Spammer>> observer);

    void unSpamList(List<String> list, LogicObserver<Boolean> observer);

    void getMessageQue(int pageNo, String time, LogicObserver<List<ForumTopic>> observer);

    void deleteMessage(long id, LogicObserver<Boolean> observer);
}
