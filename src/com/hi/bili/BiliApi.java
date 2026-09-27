package com.hi.bili;

import com.hi.bili.model.Comment;
import com.hi.bili.model.Video;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiliApi {

    private static final String BASE = "https://api.bilibili.com";

    public static List getPopular(int page) {
        List list = new ArrayList();
        try {
            String url = BASE + "/x/web-interface/popular?pn=" + page + "&ps=20";
            String json = HttpUtil.get(url);
            if (json == null) return list;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return list;
            JSONObject data = obj.getJSONObject("data");
            JSONArray arr = data.optJSONArray("list");
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject v = arr.getJSONObject(i);
                Video video = new Video();
                video.bvid = v.optString("bvid", "");
                video.aid = String.valueOf(v.optLong("aid", 0));
                video.cid = String.valueOf(v.optLong("cid", 0));
                video.title = v.optString("title", "");
                video.pic = v.optString("pic", "");
                video.desc = v.optString("desc", "");
                JSONObject owner = v.optJSONObject("owner");
                if (owner != null) {
                    video.ownerName = owner.optString("name", "");
                    video.ownerMid = String.valueOf(owner.optLong("mid", 0));
                }
                JSONObject stat = v.optJSONObject("stat");
                if (stat != null) {
                    video.play = stat.optInt("view", 0);
                    video.danmaku = stat.optInt("danmaku", 0);
                }
                list.add(video);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Video getVideoDetail(String bvid) {
        try {
            String url = BASE + "/x/web-interface/view?bvid=" + bvid;
            String json = HttpUtil.get(url);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return null;
            JSONObject data = obj.getJSONObject("data");
            Video v = new Video();
            v.bvid = data.optString("bvid", "");
            v.aid = String.valueOf(data.optLong("aid", 0));
            v.cid = String.valueOf(data.optLong("cid", 0));
            v.title = data.optString("title", "");
            v.pic = data.optString("pic", "");
            v.desc = data.optString("desc", "");
            v.duration = data.optInt("duration", 0);
            JSONObject owner = data.optJSONObject("owner");
            if (owner != null) {
                v.ownerName = owner.optString("name", "");
                v.ownerMid = String.valueOf(owner.optLong("mid", 0));
            }
            JSONObject stat = data.optJSONObject("stat");
            if (stat != null) {
                v.play = stat.optInt("view", 0);
                v.danmaku = stat.optInt("danmaku", 0);
            }
            return v;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPlayUrl(String avid, String cid) {
        try {
            Map params = new HashMap();
            params.put("avid", avid);
            params.put("cid", cid);
            params.put("qn", "64");
            params.put("fnval", "16");
            params.put("fourk", "1");
            String query = WbiSign.sign(params);
            String url = BASE + "/x/player/wbi/playurl?" + query;
            String json = HttpUtil.get(url);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return null;
            JSONObject data = obj.getJSONObject("data");
            JSONObject dash = data.optJSONObject("dash");
            if (dash != null) {
                JSONArray videos = dash.optJSONArray("video");
                if (videos != null && videos.length() > 0) {
                    JSONObject v0 = videos.getJSONObject(0);
                    String baseUrl = v0.optString("baseUrl", "");
                    if (baseUrl.length() > 0) return baseUrl;
                    JSONArray backup = v0.optJSONArray("backupUrl");
                    if (backup != null && backup.length() > 0) {
                        return backup.optString(0, "");
                    }
                }
            }
            JSONArray durl = data.optJSONArray("durl");
            if (durl != null && durl.length() > 0) {
                JSONObject d0 = durl.getJSONObject(0);
                return d0.optString("url", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List search(String keyword, int page) {
        List list = new ArrayList();
        try {
            Map params = new HashMap();
            params.put("search_type", "video");
            params.put("keyword", keyword);
            params.put("page", String.valueOf(page));
            String query = WbiSign.sign(params);
            String url = BASE + "/x/web-interface/wbi/search/type?" + query;
            String json = HttpUtil.get(url);
            if (json == null) return list;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return list;
            JSONObject data = obj.getJSONObject("data");
            JSONArray arr = data.optJSONArray("result");
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject v = arr.getJSONObject(i);
                Video video = new Video();
                video.bvid = v.optString("bvid", "");
                video.aid = String.valueOf(v.optLong("aid", 0));
                video.title = v.optString("title", "").replaceAll("<em class=\"keyword\">", "").replaceAll("</em>", "");
                video.pic = "https:" + v.optString("pic", "");
                video.ownerName = v.optString("author", "");
                video.play = v.optInt("play", 0);
                video.danmaku = v.optInt("video_review", 0);
                video.desc = v.optString("description", "");
                list.add(video);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List getComments(String aid, int page) {
        List list = new ArrayList();
        try {
            String url = BASE + "/x/v2/reply/main?type=1&oid=" + aid + "&next=" + page + "&mode=3";
            String json = HttpUtil.get(url);
            if (json == null) return list;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return list;
            JSONObject data = obj.getJSONObject("data");
            JSONArray arr = data.optJSONArray("replies");
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.getJSONObject(i);
                Comment comment = new Comment();
                comment.rpid = String.valueOf(c.optLong("rpid", 0));
                comment.ctime = c.optLong("ctime", 0);
                comment.like = c.optInt("like", 0);
                JSONObject member = c.optJSONObject("member");
                if (member != null) {
                    comment.uname = member.optString("uname", "");
                    comment.mid = String.valueOf(member.optLong("mid", 0));
                }
                JSONObject content = c.optJSONObject("content");
                if (content != null) {
                    comment.content = content.optString("message", "");
                }
                list.add(comment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
