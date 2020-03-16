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

import com.github.naturs.logger.Logger;
import com.google.gson.Gson;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.db.util.ForumTopicDaoUtils;
import io.taucoin.android.wallet.db.util.SpammerDaoUtils;
import io.taucoin.android.wallet.module.bean.AudioBean;
import io.taucoin.android.wallet.module.bean.MediaBaseBean;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.android.wallet.module.bean.NameAndType;
import io.taucoin.android.wallet.module.bean.PicBean;
import io.taucoin.android.wallet.module.bean.VideoBean;
import io.taucoin.android.wallet.net.service.IpfsRPCManager;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MultimediaUtil;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.util.StringUtil;

import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_FAIL;
import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_SUCCESS;

public class ForumModel implements IForumModel{

    @Override
    public void postMedia(ForumTopic forumTopic, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            List<ForumTopic> list = ForumTopicDaoUtils.getInstance().queryAll();
            forumTopic.setTxId(String.valueOf(list.size() + 1));
            forumTopic.setTimeStamp(DateUtil.getTime());
            forumTopic.setTSender(MyApplication.getKeyValue().getAddress());
            forumTopic.setIsender("ipfsadress222222222222222");
            long result = ForumTopicDaoUtils.getInstance().insert(forumTopic);
            emitter.onNext(result > 0);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void updateBookmark(String txId, int bookmark, LogicObserver<Boolean> observer){
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            ForumTopic bean = ForumTopicDaoUtils.getInstance().queryByTxId(txId);
            if(bean != null){
                bean.setBookmark(bookmark);
                emitter.onNext(true);
                long result = ForumTopicDaoUtils.getInstance().insert(bean);
                emitter.onNext(result > 0);
            }else{
                emitter.onNext(false);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getForumTopicList(int pageNo, String time, int bookmark, LogicObserver<List<ForumTopic>> observer) {
        Observable.create((ObservableOnSubscribe<List<ForumTopic>>) emitter -> {
            List<ForumTopic> list = ForumTopicDaoUtils.getInstance().query(pageNo, time, bookmark, 0);
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getCommentList(int pageNo, String time, String replayId, LogicObserver<List<ForumTopic>> observer) {
        Observable.create((ObservableOnSubscribe<List<ForumTopic>>) emitter -> {
            List<ForumTopic> list = ForumTopicDaoUtils.getInstance().queryComment(pageNo, time, replayId, 1);
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }


    @Override
    public void getMessageQue(int pageNo, String time, LogicObserver<List<ForumTopic>> observer) {
        Observable.create((ObservableOnSubscribe<List<ForumTopic>>) emitter -> {
            List<ForumTopic> list = ForumTopicDaoUtils.getInstance().getMessageQue(pageNo, time);
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void deleteMessage(long id, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            ForumTopicDaoUtils.getInstance().deleteMessage(id);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getSearchTopicList(int pageNo, String time, String searchKey, LogicObserver<List<ForumTopic>> observer){
        Observable.create((ObservableOnSubscribe<List<ForumTopic>>) emitter -> {
            List<ForumTopic> list = ForumTopicDaoUtils.getInstance().query(pageNo, time, searchKey);
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void picCompression(LocalMedia localMedia) {
        String mediaPath = localMedia.getPath();
        if (StringUtil.isNotEmpty(localMedia.getAndroidQToPath())) {
            mediaPath = localMedia.getAndroidQToPath();
        }
        String path = mediaPath;
        NameAndType split = MultimediaUtil.splitNameAndType(path);
        String compressPath = MultimediaUtil.PATH_PIC + split.getName() + MultimediaUtil.NAME_SUFFIX + split.getType();
        File originalFile = new File(path);
        long originalSize = originalFile.length();

        Logger.i("Original pic url=%s, exists=%s, size=%s", path, originalFile.exists(), MultimediaUtil.convertSize(originalFile));
        Logger.i("Compressed pic url=%s", compressPath);

        MediaBean mediaBean = new MediaBean();
        PicBean picOrigin = new PicBean();
        picOrigin.setWidth(localMedia.getWidth());
        picOrigin.setHeight(localMedia.getHeight());
        picOrigin.setSize(originalSize);
        picOrigin.setType(split.getType());
        picOrigin.setPath(path);
        mediaBean.setOriginMedia(picOrigin);

        if(originalSize <= MultimediaUtil.MAX_PIC_SIZE){
            EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
        }else{
            Observable.create((ObservableOnSubscribe<MediaBean>) emitter -> {
                try{
                    PicBean picBatch = new PicBean();
                    picBatch.setType(split.getType());
                    picBatch.setPath(compressPath);
                    MultimediaUtil.compressImage(path, picBatch);
                    mediaBean.setBatchMedia(picBatch);
                    EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
                }catch (Exception e){
                    EventBusUtil.post(COMPRESSION_FAIL, e.getCause().getMessage());
                    Logger.e(e, "picCompression error");
                }
            }).observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    @Override
    public void uploadMediaFile(MediaBean mediaBean, LogicObserver<String> observer) {
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            try{
                Object origin = mediaBean.getOriginMedia();
                Object batch = mediaBean.getBatchMedia();
                if(origin != null){
                    if(origin instanceof MediaBaseBean){
                        uploadMediaFile(origin);
                    }
                }
                if(batch != null){
                    if(batch instanceof PicBean){
                        uploadMediaFile(batch);
                    }else if(batch instanceof AudioBean){
                        uploadMediaFile(batch);
                    }else if(batch instanceof VideoBean){
                        VideoBean videoBean = (VideoBean) batch;
                        List<PicBean> pics = videoBean.getPics();
                        AudioBean audio= videoBean.getAudio();
                        if(pics != null && pics.size() > 0){
                            for(PicBean bean : pics){
                                uploadMediaFile(bean);
                            }
                        }
                        uploadMediaFile(audio);
                    }
                }
                String resultHash = putObjectData(mediaBean);
                Logger.i("uploadMediaFile json resultHash=%s", resultHash);
                emitter.onNext(resultHash);
            }catch (Exception e){
                emitter.onError(CodeException.getError());
                Logger.e(e, "uploadMediaFile error");
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private String putObjectData(Object object) {
        String data = new Gson().toJson(object);
        Logger.i("putObjectData json data=%s", data);
        return IpfsRPCManager.getInstance().putObjectData(data);
    }

    private void uploadMediaFile(Object object) {
        if(object instanceof MediaBaseBean){
            MediaBaseBean baseBean = (MediaBaseBean) object;
            String path = baseBean.getPath();
            String hash = IpfsRPCManager.getInstance().uploadFile(path);
            baseBean.setHash(hash);
            Logger.i("uploadMediaFile path=%s, hash=%s", path, hash);
        }
    }

    @Override
    public void getMediaData(String hash, LogicObserver<String> observer) {
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            try{
                byte[] dataBytes = IpfsRPCManager.getInstance().getObjectData(hash);
                String jsonData = null;
                if(dataBytes != null){
                    String data = new String(dataBytes);
                    MediaBean media = new Gson().fromJson(data, MediaBean.class);

                    if(media != null){
                        Object batch = media.getBatchMedia();
                        if(batch == null) {
                            batch = media.getOriginMedia();
                        }
                        jsonData = new Gson().toJson(batch);
                        Logger.d("getMediaData jsonData=%s", jsonData);
                    }
                }
                emitter.onNext(jsonData);
            }catch (Exception e){
                emitter.onError(CodeException.getError());
                Logger.e(e, "getMediaData error");
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void audioCompression(LocalMedia localMedia) {
        String mediaPath = localMedia.getPath();
        if (StringUtil.isNotEmpty(localMedia.getAndroidQToPath())) {
            mediaPath = localMedia.getAndroidQToPath();
        }
        String path = mediaPath;
        NameAndType split = MultimediaUtil.splitNameAndType(path);
        String compressPath = MultimediaUtil.PATH_AUDIO + split.getName() + MultimediaUtil.NAME_SUFFIX + split.getType();
        File originalFile = new File(path);
        long originalSize = originalFile.length();

        Logger.i("Original audio url=%s, exists=%s, size=%s", path, originalFile.exists(), MultimediaUtil.convertSize(originalFile));
        Logger.i("Compressed audio url=%s", compressPath);

        MediaBean mediaBean = new MediaBean();
        AudioBean audioOrigin = new AudioBean();
        audioOrigin.setDuration(localMedia.getDuration());
        audioOrigin.setSize(originalSize);
        audioOrigin.setType(split.getType());
        audioOrigin.setPath(path);
        mediaBean.setOriginMedia(audioOrigin);

        if(originalSize <= MultimediaUtil.MAX_AUDIO_SIZE){
            EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
        }else{
            AudioBean audioBatch = new AudioBean();
            audioBatch.setType(split.getType());
            audioBatch.setPath(compressPath);
            mediaBean.setBatchMedia(audioBatch);
            MultimediaUtil.audioCompression(mediaBean);
        }
    }

    @Override
    public void videoCompression(LocalMedia localMedia) {
        String mediaPath = localMedia.getPath();
        if (StringUtil.isNotEmpty(localMedia.getAndroidQToPath())) {
            mediaPath = localMedia.getAndroidQToPath();
        }
        String path = mediaPath;
        NameAndType split = MultimediaUtil.splitNameAndType(path);
        String compressPicPath = MultimediaUtil.PATH_VIDEO + split.getName() + MultimediaUtil.NAME_SUFFIX +
                MultimediaUtil.VIDEO_PIC_NO + MultimediaUtil.VIDEO_PIC_TYPE;
        String compressAudioPath = MultimediaUtil.PATH_VIDEO + split.getName() + MultimediaUtil.NAME_SUFFIX +
                MultimediaUtil.VIDEO_AUDIO_TYPE;
        File originalFile = new File(path);
        long originalSize = originalFile.length();

        Logger.i("Original video url=%s, exists=%s, size=%s", path, originalFile.exists(), MultimediaUtil.convertSize(originalFile));
        Logger.i("Compressed video pic url=%s, audio url=%s", compressPicPath, compressAudioPath);

        MediaBean mediaBean = new MediaBean();
        AudioBean videoOrigin = new AudioBean();
        videoOrigin.setDuration(localMedia.getDuration());
        videoOrigin.setSize(originalSize);
        videoOrigin.setType(split.getType());
        videoOrigin.setPath(path);
        mediaBean.setOriginMedia(videoOrigin);

        if(originalSize <= MultimediaUtil.MAX_AUDIO_SIZE){
            EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
        }else{
            VideoBean videoBatch = new VideoBean();
            mediaBean.setBatchMedia(videoBatch);
            MultimediaUtil.videoExtract9Pic(mediaBean, compressPicPath, compressAudioPath);
        }
    }

    @Override
    public void spamAddress(String tSender) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            Spammer spammer = SpammerDaoUtils.getInstance().queryByAddress(tSender);
            if(spammer == null){
                long result = SpammerDaoUtils.getInstance().insert(tSender);
                emitter.onNext(result > 0);
            }else {
                emitter.onNext(true);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe();
    }

    @Override
    public void getSpamList(int pageNo, String time, LogicObserver<List<Spammer>> observer) {
        Observable.create((ObservableOnSubscribe<List<Spammer>>) emitter -> {
            List<Spammer> list = SpammerDaoUtils.getInstance().getSpamList(pageNo, time);
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void unSpamList(List<String> list, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            SpammerDaoUtils.getInstance().unSpamList(list);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}