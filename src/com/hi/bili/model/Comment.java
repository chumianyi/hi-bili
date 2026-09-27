package com.hi.bili.model;

public class Comment {
    public String rpid;
    public String uname;
    public String mid;
    public String avatar;
    public String content;
    public int like;
    public int action; // 0=none, 1=liked
    public long ctime;

    public Comment() {
    }
}
