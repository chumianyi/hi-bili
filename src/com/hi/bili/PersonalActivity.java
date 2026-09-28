package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hi.bili.model.Video;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PersonalActivity extends Activity {

    private ListView listView;
    private ProgressBar progress;
    private TextView tvHeader;
    private VideoAdapter adapter;
    private List videos = new ArrayList();
    private Handler handler = new Handler();
    private String mode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "myvideos";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);

        tvHeader = new TextView(this);
        tvHeader.setText("myvideos".equals(mode) ? "我的视频" : "缓存管理");
        tvHeader.setTextColor(0xFFFFFFFF);
        tvHeader.setBackgroundColor(0xFF2196F3);
        tvHeader.setTextSize(18);
        tvHeader.getPaint().setFakeBoldText(true);
        tvHeader.setPadding(20, 16, 20, 16);
        root.addView(tvHeader, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 硬币信息（仅我的视频模式）
        if ("myvideos".equals(mode) && UserManager.isLogin()) {
            final TextView tvCoin = new TextView(this);
            tvCoin.setText("硬币: 加载中...");
            tvCoin.setTextColor(0xFFFF9800);
            tvCoin.setTextSize(14);
            tvCoin.setPadding(20, 12, 20, 12);
            tvCoin.setBackgroundColor(0xFFFFFFFF);
            root.addView(tvCoin, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            new Thread(new Runnable() {
                public void run() {
                    final int coins = BiliApi.getCoinCount();
                    handler.post(new Runnable() {
                        public void run() { tvCoin.setText("硬币: " + coins); }
                    });
                }
            }).start();
        }

        progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 50));

        listView = new ListView(this);
        listView.setCacheColorHint(0);
        listView.setDividerHeight(1);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE0E0E0));
        root.addView(listView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));

        setContentView(root);

        adapter = new VideoAdapter(this, videos);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= videos.size()) return;
                Video v = (Video) videos.get(pos);
                if ("cache".equals(mode)) {
                    // 播放本地缓存
                    File f = new File(v.pic); // pic 字段存的是本地路径
                    if (f.exists()) {
                        Intent it = new Intent(Intent.ACTION_VIEW);
                        it.setDataAndType(android.net.Uri.fromFile(f), "video/*");
                        startActivity(it);
                    } else {
                        Toast.makeText(PersonalActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Intent it = new Intent(PersonalActivity.this, VideoDetailActivity.class);
                    it.putExtra("bvid", v.bvid);
                    startActivity(it);
                }
            }
        });

        loadData();
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        if ("cache".equals(mode)) {
            loadCaches();
        } else {
            loadMyVideos();
        }
    }

    private void loadMyVideos() {
        if (!UserManager.isLogin()) {
            progress.setVisibility(View.GONE);
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        final String mid = UserManager.getMid();
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.getMyVideos(mid, 1);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        if (result != null) {
                            videos.addAll(result);
                            adapter.notifyDataSetChanged();
                        }
                        if (videos.size() == 0) {
                            Toast.makeText(PersonalActivity.this, "暂无投稿视频", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void loadCaches() {
        try {
            JSONArray arr = PrefsManager.getCaches();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Video v = new Video();
                v.bvid = o.optString("bvid", "");
                v.title = o.optString("title", "");
                v.pic = o.optString("path", ""); // 存本地路径
                v.ownerName = "已缓存";
                videos.add(v);
            }
        } catch (Exception e) {}
        progress.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        if (videos.size() == 0) {
            Toast.makeText(this, "暂无缓存视频", Toast.LENGTH_SHORT).show();
        }
    }
}
