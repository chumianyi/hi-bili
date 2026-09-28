package com.hi.bili;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hi.bili.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends Activity {

    private ListView listView;
    private ProgressBar progress;
    private Button btnLoadMore;
    private CommentAdapter adapter;
    private List comments = new ArrayList();
    private Handler handler = new Handler();
    private String aid;
    private int currentPage = 1;
    private boolean loading = false;
    private boolean hasMore = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);
        aid = getIntent().getStringExtra("aid");
        setTitle("评论");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);

        progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));

        listView = new ListView(this);
        listView.setCacheColorHint(0);
        listView.setDividerHeight(1);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE0E0E0));
        root.addView(listView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));

        btnLoadMore = new Button(this);
        btnLoadMore.setText("加载更多");
        btnLoadMore.setTextColor(0xFFFFFFFF);
        btnLoadMore.setBackgroundColor(0xFF2196F3);
        btnLoadMore.setVisibility(View.GONE);
        root.addView(btnLoadMore, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvEmpty = new TextView(this);
        tvEmpty.setText("暂无评论");
        tvEmpty.setTextColor(0xFF9E9E9E);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        tvEmpty.setPadding(0, 40, 0, 40);
        listView.setEmptyView(tvEmpty);
        root.addView(tvEmpty);

        setContentView(root);

        adapter = new CommentAdapter(this, comments);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= comments.size()) return;
                final Comment c = (Comment) comments.get(pos);
                if (!UserManager.isLogin()) {
                    Toast.makeText(CommentsActivity.this, "请先登录后再点赞", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        final int action = (c.action == 1) ? 0 : 1;
                        final boolean ok = BiliApi.likeComment(c.rpid, aid, action);
                        handler.post(new Runnable() {
                            public void run() {
                                if (ok) {
                                    c.action = action;
                                    if (action == 1) c.like++;
                                    else c.like = Math.max(0, c.like - 1);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(CommentsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        btnLoadMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadComments(); }
        });

        loadComments();
    }

    private void loadComments() {
        if (loading || !hasMore) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        btnLoadMore.setVisibility(View.GONE);
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.getComments(aid, currentPage);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        loading = false;
                        if (result != null && result.size() > 0) {
                            comments.addAll(result);
                            adapter.notifyDataSetChanged();
                            currentPage++;
                            if (result.size() >= 20) {
                                hasMore = true;
                                btnLoadMore.setVisibility(View.VISIBLE);
                            } else {
                                hasMore = false;
                            }
                        } else {
                            hasMore = false;
                        }
                    }
                });
            }
        }).start();
    }
}
