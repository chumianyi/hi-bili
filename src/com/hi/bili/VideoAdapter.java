package com.hi.bili;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hi.bili.model.Video;

import java.util.List;

public class VideoAdapter extends BaseAdapter {

    private Context context;
    private List list;
    private LayoutInflater inflater;

    public VideoAdapter(Context ctx, List data) {
        context = ctx;
        list = data;
        inflater = LayoutInflater.from(ctx);
    }

    public int getCount() {
        return list.size();
    }

    public Object getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_video, null);
            holder = new ViewHolder();
            holder.thumb = (ImageView) convertView.findViewById(R.id.iv_thumb);
            holder.title = (TextView) convertView.findViewById(R.id.tv_title);
            holder.up = (TextView) convertView.findViewById(R.id.tv_up);
            holder.play = (TextView) convertView.findViewById(R.id.tv_play);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Video v = (Video) list.get(position);
        holder.title.setText(v.title);
        holder.up.setText("UP: " + v.ownerName);
        holder.play.setText("播放 " + formatNum(v.play) + "  弹幕 " + formatNum(v.danmaku));
        ImageLoader.load(v.pic, holder.thumb);
        return convertView;
    }

    private String formatNum(int num) {
        if (num >= 10000) {
            return (num / 10000) + "." + ((num % 10000) / 1000) + "万";
        }
        return String.valueOf(num);
    }

    static class ViewHolder {
        ImageView thumb;
        TextView title;
        TextView up;
        TextView play;
    }
}
