package com.hi.bili;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hi.bili.model.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends BaseAdapter {

    private Context context;
    private List comments;
    private ImageLoader imageLoader;

    public CommentAdapter(Context ctx, List list) {
        context = ctx;
        comments = list;
        imageLoader = new ImageLoader(ctx);
    }

    public int getCount() { return comments.size(); }
    public Object getItem(int pos) { return comments.get(pos); }
    public long getItemId(int pos) { return pos; }

    public View getView(int pos, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_comment, null);
            holder = new ViewHolder();
            holder.ivAvatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
            holder.tvUname = (TextView) convertView.findViewById(R.id.tv_uname);
            holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
            holder.tvLike = (TextView) convertView.findViewById(R.id.tv_like);
            holder.tvTime = (TextView) convertView.findViewById(R.id.tv_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Comment c = (Comment) comments.get(pos);
        holder.tvUname.setText(c.uname);
        holder.tvContent.setText(c.content);
        holder.tvLike.setText("♥ " + c.like);
        if (c.action == 1) {
            holder.tvLike.setTextColor(0xFFE94560);
        } else {
            holder.tvLike.setTextColor(0xFF8892B0);
        }
        if (c.ctime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(c.ctime * 1000)));
        } else {
            holder.tvTime.setText("");
        }

        if (c.avatar != null && c.avatar.length() > 0) {
            imageLoader.display(c.avatar, holder.ivAvatar, 64, 64);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView ivAvatar;
        TextView tvUname;
        TextView tvContent;
        TextView tvLike;
        TextView tvTime;
    }
}
