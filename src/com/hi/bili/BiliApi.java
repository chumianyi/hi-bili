package com.hi.bili;

import com.hi.bili.model.Comment;
import com.hi.bili.model.Video;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiliApi {

    private static final String BASE = "https://api.bilibili.com";
    private static final String PASSPORT = "https://passport.bilibili.com";

    // === 热门视频 ===
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
                Video video = parseVideo(v);
                if (video != null) list.add(video);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static Video parseVideo(JSONObject v) {
        try {
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
            return video;
        } catch (Exception e) {
            return null;
        }
    }

    // === 视频详情 ===
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

    // === 播放地址 ===
    public static String getPlayUrl(String avid, String cid) {
        try {
            Map params = new HashMap();
            params.put("avid", avid);
            params.put("cid", cid);
            params.put("qn", "64");
            params.put("fnval", "16");
            String query = WbiSign.sign(params);
            String url = BASE + "/x/player/wbi/playurl?" + query;
            String json = HttpUtil.getWithCookie(url);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return null;
            JSONObject data = obj.getJSONObject("data");
            JSONObject dash = data.optJSONObject("dash");
            if (dash != null) {
                JSONArray videos = dash.optJSONArray("video");
                JSONArray audios = dash.optJSONArray("audio");
                if (videos != null && videos.length() > 0) {
                    JSONObject v0 = videos.getJSONObject(0);
                    String baseUrl = v0.optString("baseUrl", "");
                    if (baseUrl.length() > 0) return baseUrl;
                    JSONArray backup = v0.optJSONArray("backupUrl");
                    if (backup != null && backup.length() > 0) return backup.optString(0, "");
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

    // === 搜索 ===
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

    // === 评论列表 ===
    public static List getComments(String aid, int page) {
        List list = new ArrayList();
        try {
            String url = BASE + "/x/v2/reply/main?type=1&oid=" + aid + "&next=" + page + "&mode=3";
            String json = HttpUtil.getWithCookie(url);
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
                comment.action = c.optInt("action", 0);
                JSONObject member = c.optJSONObject("member");
                if (member != null) {
                    comment.uname = member.optString("uname", "");
                    comment.mid = String.valueOf(member.optLong("mid", 0));
                    comment.avatar = member.optString("avatar", "");
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

    // === 评论点赞 ===
    public static boolean likeComment(String rpid, String oid, int action) {
        try {
            String cookie = UserManager.getCookie();
            if (cookie == null || cookie.length() == 0) return false;
            String url = BASE + "/x/v2/reply/action?type=1&oid=" + oid + "&rpid=" + rpid + "&action=" + action;
            String json = HttpUtil.getWithCookie(url);
            if (json == null) return false;
            JSONObject obj = new JSONObject(json);
            return obj.optInt("code") == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // === 二维码登录 ===
    public static String[] generateQRCode() {
        try {
            String url = PASSPORT + "/x/passport-login/web/qrcode/generate?source=main-fe-header";
            String json = HttpUtil.get(url);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return null;
            JSONObject data = obj.getJSONObject("data");
            String qrUrl = data.optString("url", "");
            String qrKey = data.optString("qrcode_key", "");
            return new String[]{qrUrl, qrKey};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int pollQRCode(String qrKey) {
        try {
            String url = PASSPORT + "/x/passport-login/web/qrcode/poll?qrcode_key=" + qrKey + "&source=main-fe-header";
            String json = HttpUtil.get(url);
            if (json == null) return -1;
            JSONObject obj = new JSONObject(json);
            JSONObject data = obj.optJSONObject("data");
            if (data == null) return -1;
            int code = data.optInt("code", -1);
            if (code == 0) {
                String cookie = extractLoginCookie(data);
                if (cookie != null && cookie.length() > 0) {
                    UserManager.cookie = cookie;
                }
            }
            return code;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String extractLoginCookie(JSONObject data) {
        try {
            String url = data.optString("url", "");
            if (url.length() > 0 && url.indexOf('?') > 0) {
                String query = url.substring(url.indexOf('?') + 1);
                String[] parts = query.split("&");
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < parts.length; i++) {
                    String[] kv = parts[i].split("=", 2);
                    if (kv.length == 2) {
                        if (sb.length() > 0) sb.append("; ");
                        sb.append(kv[0]).append("=").append(kv[1]);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
        }
        return "";
    }

    // === 获取用户信息 ===
    public static String[] getUserInfo() {
        try {
            String url = BASE + "/x/web-interface/nav";
            String json = HttpUtil.getWithCookie(url);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            if (obj.optInt("code") != 0) return null;
            JSONObject data = obj.getJSONObject("data");
            if (!data.optBoolean("isLogin", false)) return null;
            String uname = data.optString("uname", "");
            String avatar = data.optString("face", "");
            String mid = String.valueOf(data.optLong("mid", 0));
            return new String[]{uname, avatar, mid};
        } catch (Exception e) {
            return null;
        }
    }

    // === 弹幕 XML 解析 ===
    public static List getDanmaku(String cid) {
        List list = new ArrayList();
        try {
            String url = "https://api.bilibili.com/x/v1/dm/list.so?oid=" + cid;
            String xml = HttpUtil.get(url);
            if (xml == null) return list;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "d".equals(parser.getName())) {
                    String p = parser.getAttributeValue(null, "p");
                    String text = parser.nextText();
                    if (p != null && text != null) {
                        String[] attrs = p.split(",");
                        if (attrs.length >= 4) {
                            Danmaku d = new Danmaku();
                            d.time = Float.parseFloat(attrs[0]);
                            d.type = Integer.parseInt(attrs[1]);
                            d.size = Integer.parseInt(attrs[2]);
                            d.color = Integer.parseInt(attrs[3]);
                            d.text = text;
                            list.add(d);
                        }
                    }
                }
                event = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static class Danmaku {
        public float time;
        public int type;
        public int size;
        public int color;
        public String text;
    }
}
