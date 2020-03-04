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
package io.taucoin.android.wallet.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.naturs.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.module.bean.AudioBean;
import io.taucoin.android.wallet.module.bean.MediaBean;
import io.taucoin.android.wallet.module.bean.NameAndType;
import io.taucoin.android.wallet.module.bean.PicBean;
import io.taucoin.android.wallet.module.bean.VideoBean;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_FAIL;
import static io.taucoin.android.wallet.module.bean.MessageEvent.EventCode.COMPRESSION_SUCCESS;

/**
 *
 * Multimedia processing related logic processing:
 * compression, clipping, image extraction, audio extraction
 *
 * */
public class MultimediaUtil {

    private static final String PATH_FILE = "/storage/emulated/0/Android/data/io.taucoin.android.wallet/files/";
    public static final String PATH_PIC = PATH_FILE + "Pictures" + File.separator;
    public static final String PATH_AUDIO = PATH_FILE + "Music" + File.separator;
    public static final String PATH_VIDEO = PATH_FILE + "Movies" + File.separator;
    public static final String NAME_SUFFIX = "_COMPRESS";
    public static final String VIDEO_PIC_NO = "%03d";
    public static final String VIDEO_PIC_TYPE = ".jpeg";
    public static final String VIDEO_AUDIO_TYPE = ".aac";

    public static final int MAX_PIC_SIZE = 100 * 1024;          // byte
    private static final int MAX_PIC_WIDTH = 1200;              // px
    private static final int MAX_PIC_HEIGHT = 1200;             // px

    public static final int MAX_AUDIO_SIZE = 2 * 1024 * 1024;  // byte
    private static final int MAX_AUDIO_DURATION = 15 * 60 * 1000;      // ms

    public static String convertSize(File file){
        double size = 0;
        if(file != null){
            size = file.length() / 1024;
        }
        return size + "kb";
    }

    public static NameAndType splitNameAndType(String path){
        String name = "";
        String type = "";
        if(StringUtil.isNotEmpty(path)){

            int splitIndex = path.lastIndexOf("/");
            String fileName = "";
            if(splitIndex > 0 && splitIndex < path.length()){
               fileName = path.substring(splitIndex + 1);
            }
            splitIndex = fileName.lastIndexOf(".");
            if(StringUtil.isNotEmpty(fileName) &&
                    splitIndex > 0 && splitIndex < fileName.length()){
                name = fileName.substring(0, splitIndex);
                type = fileName.substring(splitIndex);
            }
        }
        return new NameAndType(name, type);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > MAX_PIC_HEIGHT || width > MAX_PIC_WIDTH) {
            final int heightRatio = Math.round((float) height/ (float) MAX_PIC_HEIGHT);
            final int widthRatio = Math.round((float) width / (float) MAX_PIC_WIDTH);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static Bitmap imageScale(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (h > MAX_PIC_HEIGHT || w > MAX_PIC_WIDTH) {
            float scale;
            if(w >= h){
                scale = (float) MAX_PIC_WIDTH / w;
            }else{
                scale = (float) MAX_PIC_HEIGHT / h;
            }
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix,
                    true);
        }
        return bitmap;
    }

    public static void compressImage(String originPath, PicBean picBatch) throws IOException {
        Bitmap tagBitmap = getSmallBitmap(originPath);
        tagBitmap = imageScale(tagBitmap);
        picBatch.setWidth(tagBitmap.getWidth());
        picBatch.setHeight(tagBitmap.getHeight());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        tagBitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        tagBitmap.recycle();

        File compressImage = new File(picBatch.getPath());
        FileOutputStream fos = new FileOutputStream(compressImage);
        fos.write(stream.toByteArray());
        fos.flush();
        fos.close();
        stream.close();

        picBatch.setSize(compressImage.length());
    }

    private static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);

    }

    // Using ffmpeg audio compression
    public static void audioCompression(MediaBean mediaBean) {
        AudioBean audioOriginal = (AudioBean) mediaBean.getOriginMedia();
        AudioBean audioBatch = (AudioBean) mediaBean.getBatchMedia();
        String compressPath = audioBatch.getPath();
        Logger.i("Compressed audio url::" + compressPath);
        File originalFile = new File(audioOriginal.getPath());
        long duration = audioOriginal.getDuration();
        if(duration > MAX_AUDIO_DURATION){
            duration = MAX_AUDIO_DURATION;
        }
        audioBatch.setDuration(duration);

        audioCompression(mediaBean, originalFile.getPath(), audioBatch);
    }

    private static void audioCompression(MediaBean mediaBean, String originalPath, AudioBean audioBatch){
        final String[] cmd = new String[]{"-y", "-i", originalPath,
                "-ss", "0",
                "-t", String.valueOf(audioBatch.getDuration() / 1000),
                "-ab", "16", // bitrate
                "-ar", "11025",  // freq
                "-ac", "1",   // channels
                audioBatch.getPath()};
        try {
            Context context = MyApplication.getInstance();
            FFmpegExecuteResponseHandler fFmpegExecuteResponseHandler = new FFmpegExecuteResponseHandler() {
                @Override
                public void onStart() {
                    Logger.i("FFmpeg onStart");
                }

                @Override
                public void onProgress(String message) {
                    Logger.i("FFmpeg onProgress::%s", message);
                }

                @Override
                public void onSuccess(String message) {
                    Logger.i("FFmpeg onSuccess::%s", message);
                }

                @Override
                public void onFailure(String message) {
                    EventBusUtil.post(COMPRESSION_FAIL, message);
                    Logger.i("FFmpeg onFailure::%s", message);
                }

                @Override
                public void onFinish() {
                    Logger.i("FFmpeg onFinish");
                    File file = new File(audioBatch.getPath());
                    if(file.exists()){
                        long size = file.length();
                        audioBatch.setSize(size);
                    }
                    EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
                    FFmpeg.getInstance(context).killRunningProcesses();
                }
            };
            FFmpeg.getInstance(context).execute(cmd, fFmpegExecuteResponseHandler);
        } catch (Exception e){
            EventBusUtil.post(COMPRESSION_FAIL, e.getCause().getMessage());
            Logger.e(e, "FFmpeg error");
        }
    }

    // Video extract 9 pictures
    public static void videoExtract9Pic(MediaBean mediaBean, String compressPicPath, String compressAudioPath) {
        AudioBean videoOriginal = (AudioBean) mediaBean.getOriginMedia();
        String path = videoOriginal.getPath();
        long duration = videoOriginal.getDuration();
        if(duration > MAX_AUDIO_DURATION){
            duration = MAX_AUDIO_DURATION;
        }
        float fps = ((float) 9) * 1000 / duration ;

        File originalFile = new File(path);
        final File convertedFile = new File(compressPicPath);
        final String[] cmd = new String[]{"-y", "-i", originalFile.getPath(),
                "-ss", "0",
                "-t", String.valueOf(duration / 1000),
                "-vf",
                "fps=" + fps,
                "-q:v", "1",
                convertedFile.getPath()};
        Logger.i("FFmpeg -t=%s, fps=%s", String.valueOf(duration / 1000), fps);
        try {
            Context context = MyApplication.getInstance();
            FFmpegExecuteResponseHandler fFmpegExecuteResponseHandler = new FFmpegExecuteResponseHandler() {
                @Override
                public void onStart() {
                    Logger.i("FFmpeg onStart");
                }

                @Override
                public void onProgress(String message) {
                    Logger.i("FFmpeg onProgress::" + message);
                }

                @Override
                public void onSuccess(String message) {
                    Logger.i("FFmpeg onSuccess::" + message);
                }

                @Override
                public void onFailure(String message) {
                    Logger.i("FFmpeg onFailure::" + message);
                    EventBusUtil.post(COMPRESSION_FAIL, message);
                    Logger.i("FFmpeg onFailure::%s", message);
                }

                @Override
                public void onFinish() {
                    Logger.i("FFmpeg Compressed onFinish::");
                    compressVideoPicAndAudio(mediaBean, compressPicPath, compressAudioPath);
                }
            };
            FFmpeg.getInstance(context).execute(cmd, fFmpegExecuteResponseHandler);
        } catch (Exception e){
            Logger.e(e, "FFmpeg Compressed error");
        }
    }

    private static void compressVideoPicAndAudio(MediaBean mediaBean, String compressPicPath, String compressAudioPath) {
        Observable.create((ObservableOnSubscribe<MediaBean>) emitter -> {
            try{
                VideoBean videoBatch = (VideoBean) mediaBean.getBatchMedia();
                List<PicBean> pics = new ArrayList<>();
                videoBatch.setPics(pics);
                for (int i = 1; i <= 9; i++) {
                    String path = String.format(compressPicPath, i);
                    File file = new File(path);
                    if(file.exists()){
                        long size = file.length();
                        PicBean pic = new PicBean();
                        pic.setPath(path);
                        pic.setSize(file.length());
                        pic.setType(VIDEO_PIC_TYPE);
                        if(size <= MultimediaUtil.MAX_PIC_SIZE){
                            parseDimension(pic);
                        }else{
                            MultimediaUtil.compressImage(path, pic);
                        }
                        pics.add(pic);
                    }else {
                        break;
                    }
                }
                AudioBean videoOriginal = (AudioBean) mediaBean.getOriginMedia();
                long duration = videoOriginal.getDuration();
                if(duration > MAX_AUDIO_DURATION){
                    duration = MAX_AUDIO_DURATION;
                }
                AudioBean audio = new AudioBean();
                audio.setPath(compressAudioPath);
                audio.setType(VIDEO_AUDIO_TYPE);
                audio.setDuration(duration);
                videoBatch.setAudio(audio);
                emitter.onNext(mediaBean);
            }catch (Exception e){
                EventBusUtil.post(COMPRESSION_FAIL, e.getCause().getMessage());
                Logger.e(e, "compressVideoPicAndAudio error");
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(new LogicObserver<MediaBean>() {
                    @Override
                    public void handleData(MediaBean mediaBean) {
                        videoExtractAudio(mediaBean);
                    }

                    @Override
                    public void handleError(int code, String msg) {
                    }
                });
    }

    private static void parseDimension(PicBean pic) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pic.getPath(), options);
        pic.setWidth(options.outWidth);
        pic.setHeight(options.outHeight);
    }

    // Video extract audio
    private static void videoExtractAudio(MediaBean mediaBean) {
        AudioBean videoOriginal = (AudioBean) mediaBean.getOriginMedia();
        String path = videoOriginal.getPath();
        VideoBean videoBean = (VideoBean) mediaBean.getBatchMedia();
        AudioBean audioBatch = videoBean.getAudio();
        String compressAudioPath = audioBatch.getPath();
        long duration = audioBatch.getDuration();
        audioBatch.setDuration(duration);

        File originalFile = new File(path);
        final File convertedFile = new File(compressAudioPath);

        final String[] cmd = new String[]{"-y", "-i", originalFile.getPath(),
                "-vn",
                "-ss", "0",
                "-t",  String.valueOf(duration / 1000),
                "-ab", "16", // bitrate
                "-ar", "11025",  // freq
                "-ac", "1",   // channels
                convertedFile.getPath()};
        try {
            Context context = MyApplication.getInstance();
            FFmpegExecuteResponseHandler fFmpegExecuteResponseHandler = new FFmpegExecuteResponseHandler() {
                @Override
                public void onStart() {
                    Logger.i("FFmpeg onStart");
                }

                @Override
                public void onProgress(String message) {
                    Logger.i("FFmpeg onProgress::" + message);
                }

                @Override
                public void onSuccess(String message) {
                    Logger.i("FFmpeg onSuccess::" + message);
                }

                @Override
                public void onFailure(String message) {
                    EventBusUtil.post(COMPRESSION_FAIL, message);
                    Logger.i("FFmpeg onFailure::" + message);
                }

                @Override
                public void onFinish() {
                    Logger.i("FFmpeg onFinish::");
                    File file = new File(compressAudioPath);
                    if(file.exists()){
                        long size = file.length();
                        audioBatch.setSize(size);
                    }
                    EventBusUtil.post(COMPRESSION_SUCCESS, mediaBean);
                }
            };
            FFmpeg.getInstance(context).execute(cmd, fFmpegExecuteResponseHandler);
        } catch (Exception e){
            EventBusUtil.post(COMPRESSION_FAIL, e.getCause().getMessage());
            Logger.e(e, "FFmpeg videoExtractAudio error");
        }
    }
}
