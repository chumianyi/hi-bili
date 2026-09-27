package com.hi.bili;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;

import com.hi.bili.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends Activity {

    private ListView listView;
    private List commentList = new ArrayList();
    private CommentAdapter adapter;
    private Handler handler = new Handler();
    private String aid;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        aid = getIntent().getStringExtra("aid");
        listView = (ListView) findViewById(R.id.list_comments);
        adapter = new CommentAdapter(this, commentList);
        listView.setAdapter(adapter);

        loadComments();
    }

    private void loadComments() {
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.getComments(aid, 1);
                handler.post(new Runnable() {
                    public void run() {
                        commentList.clear();
                        commentList.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }
}
