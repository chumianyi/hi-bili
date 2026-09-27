package com.hi.bili;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {

    private static Map cache = new HashMap();
    private static Handler handler = new Handler();

    public static void load(final String url, final ImageView imageView) {
        if (url == null || url.length() == 0) {
            imageView.setImageBitmap(null);
            return;
        }
        if (cache.containsKey(url)) {
            imageView.setImageBitmap((Bitmap) cache.get(url));
            return;
        }
        final WeakReference ref = new WeakReference(imageView);
        new Thread(new Runnable() {
            public void run() {
                try {
                    URL u = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("Referer", "https://www.bilibili.com");
                    InputStream is = conn.getInputStream();
                    final Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    conn.disconnect();
                    if (bmp != null) {
                        cache.put(url, bmp);
                        handler.post(new Runnable() {
                            public void run() {
                                ImageView iv = (ImageView) ref.get();
                                if (iv != null) {
                                    iv.setImageBitmap(bmp);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }
}
