package com.hi.bili;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREF_NAME = "hibili_user";
    static String cookie = "";
    private static String uname = "";
    private static String avatar = "";
    private static String mid = "";
    private static boolean loaded = false;

    public static synchronized void load(Context ctx) {
        if (loaded) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        cookie = sp.getString("cookie", "");
        uname = sp.getString("uname", "");
        avatar = sp.getString("avatar", "");
        mid = sp.getString("mid", "");
        loaded = true;
    }

    public static synchronized void save(Context ctx, String c, String name, String av, String m) {
        cookie = c;
        uname = name;
        avatar = av;
        mid = m;
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("cookie", c);
        ed.putString("uname", name);
        ed.putString("avatar", av);
        ed.putString("mid", m);
        ed.commit();
    }

    public static synchronized void logout(Context ctx) {
        cookie = "";
        uname = "";
        avatar = "";
        mid = "";
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.clear();
        ed.commit();
    }

    public static String getCookie() { return cookie; }
    public static String getUname() { return uname; }
    public static String getAvatar() { return avatar; }
    public static String getMid() { return mid; }
    public static boolean isLogin() { return cookie != null && cookie.length() > 0 && cookie.indexOf("SESSDATA") >= 0; }
}
