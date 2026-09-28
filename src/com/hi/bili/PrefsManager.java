package com.hi.bili;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PrefsManager {

    private static final String PREF = "hibili_prefs";
    private static SharedPreferences sp;

    public static void init(Context ctx) {
        if (sp == null) {
            sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }
    }

    // === 引导 ===
    public static boolean isFirstLaunch() {
        return sp.getBoolean("first_launch", true);
    }
    public static void setFirstLaunchDone() {
        sp.edit().putBoolean("first_launch", false).commit();
    }

    // === 默认主页 ===
    public static String getDefaultTab() {
        return sp.getString("default_tab", "hot");
    }
    public static void setDefaultTab(String tab) {
        sp.edit().putString("default_tab", tab).commit();
    }

    // === 弹幕大小 ===
    public static int getDanmakuSize() {
        return sp.getInt("danmaku_size", 18);
    }
    public static void setDanmakuSize(int size) {
        sp.edit().putInt("danmaku_size", size).commit();
    }

    // === 弹幕开关 ===
    public static boolean getDanmakuEnabled() {
        return sp.getBoolean("danmaku_enabled", true);
    }
    public static void setDanmakuEnabled(boolean e) {
        sp.edit().putBoolean("danmaku_enabled", e).commit();
    }

    // === 观看历史 ===
    public static void addHistory(String bvid, String title, String pic) {
        try {
            String hist = sp.getString("history", "[]");
            JSONArray arr = new JSONArray(hist);
            // 去重
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!o.optString("bvid","").equals(bvid)) {
                    newArr.put(o);
                }
            }
            JSONObject item = new JSONObject();
            item.put("bvid", bvid);
            item.put("title", title);
            item.put("pic", pic);
            item.put("time", System.currentTimeMillis());
            newArr.put(item);
            // 最多50条
            if (newArr.length() > 50) {
                JSONArray trimmed = new JSONArray();
                for (int i = newArr.length() - 50; i < newArr.length(); i++) {
                    trimmed.put(newArr.get(i));
                }
                newArr = trimmed;
            }
            sp.edit().putString("history", newArr.toString()).commit();
        } catch (Exception e) {
        }
    }

    public static JSONArray getHistory() {
        try {
            return new JSONArray(sp.getString("history", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    // === 缓存列表 ===
    public static void addCache(String bvid, String title, String path) {
        try {
            String c = sp.getString("caches", "[]");
            JSONArray arr = new JSONArray(c);
            JSONObject item = new JSONObject();
            item.put("bvid", bvid);
            item.put("title", title);
            item.put("path", path);
            item.put("time", System.currentTimeMillis());
            arr.put(item);
            sp.edit().putString("caches", arr.toString()).commit();
        } catch (Exception e) {
        }
    }

    public static JSONArray getCaches() {
        try {
            return new JSONArray(sp.getString("caches", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    // === 通用 ===
    public static String getString(String key, String def) {
        return sp.getString(key, def);
    }
    public static void putString(String key, String val) {
        sp.edit().putString(key, val).commit();
    }
    public static int getInt(String key, int def) {
        return sp.getInt(key, def);
    }
    public static void putInt(String key, int val) {
        sp.edit().putInt(key, val).commit();
    }
    public static boolean getBoolean(String key, boolean def) {
        return sp.getBoolean(key, def);
    }
    public static void putBoolean(String key, boolean val) {
        sp.edit().putBoolean(key, val).commit();
    }
}
