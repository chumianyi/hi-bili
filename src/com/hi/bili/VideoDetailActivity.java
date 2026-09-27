package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hi.bili.model.Video;

public class VideoDetailActivity extends Activity {

    private ImageView ivCover;
    private TextView tvTitle, tvOwner, tvStats, tvDesc;
    private Button btnPlay, btnComments;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private Video video;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF1A1A2E);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        ivCover = new ImageView(this);
        ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivCover.setBackgroundColor(0xFF333333);
        root.addView(ivCover, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 200));

        tvTitle = new TextView(this);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(18);
        tvTitle.getPaint().setFakeBoldText(true);
        tvTitle.setPadding(0, 12, 0, 6);
        root.addView(tvTitle);

        tvOwner = new TextView(this);
        tvOwner.setTextColor(0xFFE94560);
        tvOwner.setTextSize(13);
        root.addView(tvOwner);

        tvStats = new TextView(this);
        tvStats.setTextColor(0xFF8892B0);
        tvStats.setTextSize(12);
        tvStats.setPadding(0, 4, 0, 8);
        root.addView(tvStats);

        tvDesc = new TextView(this);
        tvDesc.setTextColor(0xFFCCCCCC);
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(4, 1);
        root.addView(tvDesc);

        btnPlay = new Button(this);
        btnPlay.setText("▶ 播放视频");
        btnPlay.setTextColor(0xFFFFFFFF);
        btnPlay.setBackgroundColor(0xFFE94560);
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 80);
        playLp.topMargin = 20;
        root.addView(btnPlay, playLp);

        btnComments = new Button(this);
        btnComments.setText("查看评论");
        btnComments.setTextColor(0xFFFFFFFF);
        btnComments.setBackgroundColor(0xFF0F3460);
        LinearLayout.LayoutParams cmtLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 70);
        cmtLp.topMargin = 12;
        root.addView(btnComments, cmtLp);

        progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));

        scroll.addView(root);
        setContentView(scroll);

        final String bvid = getIntent().getStringExtra("bvid");

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (video != null) {
                    Intent it = new Intent(VideoDetailActivity.this, PlayerActivity.class);
                    it.putExtra("avid", video.aid);
                    it.putExtra("cid", video.cid);
                    it.putExtra("title", video.title);
                    startActivity(it);
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

        loadDetail(bvid);
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
