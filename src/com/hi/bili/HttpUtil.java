package com.hi.bili;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpUtil {

    public static String get(String urlStr) {
        return get(urlStr, null);
    }

    public static String get(String urlStr, String cookie) {
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            if (cookie != null && cookie.length() > 0) {
                conn.setRequestProperty("Cookie", cookie);
            }
            int code = conn.getResponseCode();
            if (code == 200) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
            }
        }
    }

    public static String getWithCookie(String urlStr) {
        return get(urlStr, UserManager.getCookie());
    }

    public static String extractCookies(HttpURLConnection conn) {
        Map<String, List<String>> headerFields = conn.getHeaderFields();
        if (headerFields == null) return "";
        List cookies = headerFields.get("Set-Cookie");
        if (cookies == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cookies.size(); i++) {
            String c = (String) cookies.get(i);
            int idx = c.indexOf(';');
            if (idx > 0) c = c.substring(0, idx);
            if (sb.length() > 0) sb.append("; ");
            sb.append(c);
        }
        return sb.toString();
    }
}
