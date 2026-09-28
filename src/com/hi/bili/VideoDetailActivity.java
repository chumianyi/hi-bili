package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hi.bili.model.Video;

public class VideoDetailActivity extends Activity {

    private ImageView ivCover;
    private TextView tvTitle, tvOwner, tvStats, tvDesc;
    private Button btnPlay, btnComments, btnCache, btnCoin;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private Video video;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF5F9FF);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        ivCover = new ImageView(this);
        ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivCover.setBackgroundColor(0xFFE3F2FD);
        root.addView(ivCover, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 200));

        tvTitle = new TextView(this);
        tvTitle.setTextColor(0xFF212121);
        tvTitle.setTextSize(18);
        tvTitle.getPaint().setFakeBoldText(true);
        tvTitle.setPadding(0, 12, 0, 6);
        root.addView(tvTitle);

        tvOwner = new TextView(this);
        tvOwner.setTextColor(0xFF2196F3);
        tvOwner.setTextSize(13);
        root.addView(tvOwner);

        tvStats = new TextView(this);
        tvStats.setTextColor(0xFF757575);
        tvStats.setTextSize(12);
        tvStats.setPadding(0, 4, 0, 8);
        root.addView(tvStats);

        tvDesc = new TextView(this);
        tvDesc.setTextColor(0xFF424242);
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(4, 1);
        root.addView(tvDesc);

        // 按钮行
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 16, 0, 0);

        btnPlay = makeBtn("▶ 播放", 0xFF2196F3);
        btnComments = makeBtn("评论", 0xFF03A9F4);
        btnRow.addView(btnPlay, new LinearLayout.LayoutParams(0, 70, 1));
        btnRow.addView(btnComments, new LinearLayout.LayoutParams(0, 70, 1));
        root.addView(btnRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 高级功能按钮（非精简模式）
        if (Build.VERSION.SDK_INT >= 3) {
            LinearLayout btnRow2 = new LinearLayout(this);
            btnRow2.setOrientation(LinearLayout.HORIZONTAL);
            btnRow2.setPadding(0, 8, 0, 0);
            btnCache = makeBtn("缓存", 0xFF43A047);
            btnCoin = makeBtn("投币", 0xFFFF9800);
            btnRow2.addView(btnCache, new LinearLayout.LayoutParams(0, 64, 1));
            btnRow2.addView(btnCoin, new LinearLayout.LayoutParams(0, 64, 1));
            root.addView(btnRow2, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));

        scroll.addView(root);
        setContentView(scroll);

        final String bvid = getIntent().getStringExtra("bvid");

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (video == null) return;
                try {
                    String mode = PrefsManager.getPlaybackMode();
                    if ("browser".equals(mode)) {
                        // 浏览器播放：打开 B 站视频页面
                        String bvid = video.bvid != null ? video.bvid : "";
                        if (bvid.length() > 0) {
                            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.bilibili.com/video/" + bvid));
                            startActivity(webIntent);
                        } else {
                            Toast.makeText(VideoDetailActivity.this, "无法获取视频地址", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // 内置播放器
                        Intent it = new Intent(VideoDetailActivity.this, PlayerActivity.class);
                        it.putExtra("avid", video.aid);
                        it.putExtra("cid", video.cid);
                        it.putExtra("title", video.title);
                        startActivity(it);
                    }
                } catch (Exception e) {
                    Toast.makeText(VideoDetailActivity.this, "播放启动失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnComments.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (video != null) {
                    Intent it = new Intent(VideoDetailActivity.this, CommentsActivity.class);
                    it.putExtra("aid", video.aid);
                    startActivity(it);
                }
            }
        });
        if (btnCache != null) {
            btnCache.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { cacheVideo(); }
            });
        }
        if (btnCoin != null) {
            btnCoin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showCoinDialog(); }
            });
        }

        loadDetail(bvid);
    }

    private Button makeBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundColor(color);
        b.setTextSize(14);
        return b;
    }

    private void cacheVideo() {
        if (video == null) return;
        if (Build.VERSION.SDK_INT >= 9) {
            CacheManager.cache(this, video);
            Toast.makeText(this, "已加入下载队列", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "当前系统版本不支持缓存", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCoinDialog() {
        if (!UserManager.isLogin()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        final String[] options = {"投 1 个硬币", "投 2 个硬币"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("投币支持 UP 主");
        builder.setItems(options, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                final int num = which + 1;
                new Thread(new Runnable() {
                    public void run() {
                        final boolean ok = BiliApi.addCoin(video.aid, num);
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(VideoDetailActivity.this, ok ? "投币成功！" : "投币失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
        });
        builder.show();
    }

    private void loadDetail(final String bvid) {
        progress.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            public void run() {
                video = BiliApi.getVideoDetail(bvid);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        if (video != null) {
                            tvTitle.setText(video.title);
                            tvOwner.setText("UP: " + video.ownerName);
                            tvStats.setText("播放 " + video.play + "  |  弹幕 " + video.danmaku + "  |  时长 " + formatDuration(video.duration));
                            tvDesc.setText(video.desc);
                            if (video.pic != null && video.pic.length() > 0) {
                                new ImageLoader(VideoDetailActivity.this).display(video.pic, ivCover, 400, 200);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private String formatDuration(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
