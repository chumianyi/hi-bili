package com.hi.bili;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hi.bili.model.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends BaseAdapter {

    private Context context;
    private List list;
    private LayoutInflater inflater;

    public CommentAdapter(Context ctx, List data) {
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
            convertView = inflater.inflate(R.layout.item_comment, null);
            holder = new ViewHolder();
            holder.uname = (TextView) convertView.findViewById(R.id.tv_uname);
            holder.like = (TextView) convertView.findViewById(R.id.tv_like);
            holder.content = (TextView) convertView.findViewById(R.id.tv_content);
            holder.time = (TextView) convertView.findViewById(R.id.tv_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Comment c = (Comment) list.get(position);
        holder.uname.setText(c.uname);
        holder.like.setText("赞 " + c.like);
        holder.content.setText(c.content);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.time.setText(sdf.format(new Date(c.ctime * 1000)));
        return convertView;
    }

    static class ViewHolder {
        TextView uname;
        TextView like;
        TextView content;
        TextView time;
    }
}
