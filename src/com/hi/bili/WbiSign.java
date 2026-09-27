package com.hi.bili;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class WbiSign {

    private static final int[] MIXIN_KEY_ENC_TAB = new int[] {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    private static String wbiKey = null;
    private static long lastFetchTime = 0;

    public static synchronized String getWbiKey() {
        long now = System.currentTimeMillis();
        if (wbiKey != null && (now - lastFetchTime) < 3600000) {
            return wbiKey;
        }
        try {
            String navJson = HttpUtil.get("https://api.bilibili.com/x/web-interface/nav");
            if (navJson != null) {
                JSONObject obj = new JSONObject(navJson);
                if (obj.optInt("code") == 0) {
                    JSONObject data = obj.getJSONObject("data");
                    JSONObject wbiImg = data.optJSONObject("wbi_img");
                    if (wbiImg != null) {
                        String imgUrl = wbiImg.optString("img_url", "");
                        String subUrl = wbiImg.optString("sub_url", "");
                        String imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1, imgUrl.lastIndexOf('.'));
                        String subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1, subUrl.lastIndexOf('.'));
                        String raw = imgKey + subKey;
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < MIXIN_KEY_ENC_TAB.length; i++) {
                            int idx = MIXIN_KEY_ENC_TAB[i];
                            if (idx < raw.length()) {
                                sb.append(raw.charAt(idx));
                            }
                        }
                        wbiKey = sb.toString().substring(0, 32);
                        lastFetchTime = now;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wbiKey;
    }

    public static String sign(Map params) {
        String key = getWbiKey();
        if (key == null) {
            return buildQuery(params);
        }
        TreeMap sorted = new TreeMap();
        sorted.putAll(params);
        sorted.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
        StringBuffer query = new StringBuffer();
        Iterator it = sorted.entrySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            if (!first) query.append('&');
            try {
                query.append(URLEncoder.encode(String.valueOf(entry.getKey()), "UTF-8"));
                query.append('=');
                query.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            } catch (Exception e) {
            }
            first = false;
        }
        String wbiSign = md5(query.toString() + key);
        return query.toString() + "&w_rid=" + wbiSign;
    }

    private static String buildQuery(Map params) {
        StringBuffer query = new StringBuffer();
        Iterator it = params.entrySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            if (!first) query.append('&');
            try {
                query.append(URLEncoder.encode(String.valueOf(entry.getKey()), "UTF-8"));
                query.append('=');
                query.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            } catch (Exception e) {
            }
            first = false;
        }
        return query.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xff & digest[i]);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
