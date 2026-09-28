package com.hi.bili;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.hi.bili.model.Video;

import java.io.File;

public class CacheManager {

    public static void cache(final Context ctx, final Video video) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String playUrl = BiliApi.getPlayUrl(video.aid, video.cid);
                    if (playUrl == null || playUrl.length() == 0) return;

                    File dir = new File(Environment.getExternalStorageDirectory(), "HiBili/Cache");
                    if (!dir.exists()) dir.mkdirs();
                    File outFile = new File(dir, video.bvid + ".mp4");

                    DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(playUrl));
                    req.setTitle("缓存: " + video.title);
                    req.setDescription("Hi！bili 视频缓存");
                    req.setDestinationUri(Uri.fromFile(outFile));
                    req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.addRequestHeader("Referer", "https://www.bilibili.com");
                    req.addRequestHeader("User-Agent", "Mozilla/5.0");
                    dm.enqueue(req);

                    PrefsManager.addCache(video.bvid, video.title, outFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
