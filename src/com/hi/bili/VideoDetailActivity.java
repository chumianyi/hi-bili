package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hi.bili.model.Video;

public class VideoDetailActivity extends Activity {

    private ImageView ivCover;
    private TextView tvTitle, tvUp, tvStats, tvDesc;
    private Button btnPlay, btnComments;
    private Handler handler = new Handler();
    private String bvid;
    private Video video;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        bvid = getIntent().getStringExtra("bvid");

        ivCover = (ImageView) findViewById(R.id.iv_cover);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvUp = (TextView) findViewById(R.id.tv_up);
        tvStats = (TextView) findViewById(R.id.tv_stats);
        tvDesc = (TextView) findViewById(R.id.tv_desc);
        btnPlay = (Button) findViewById(R.id.btn_play);
        btnComments = (Button) findViewById(R.id.btn_comments);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (video != null) {
                    Intent intent = new Intent(VideoDetailActivity.this, PlayerActivity.class);
                    intent.putExtra("avid", video.aid);
                    intent.putExtra("cid", video.cid);
                    intent.putExtra("title", video.title);
                    startActivity(intent);
                }
            }
        });

        btnComments.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (video != null) {
                    Intent intent = new Intent(VideoDetailActivity.this, CommentsActivity.class);
                    intent.putExtra("aid", video.aid);
                    startActivity(intent);
                }
            }
        });

        loadDetail();
    }

    private void loadDetail() {
        new Thread(new Runnable() {
            public void run() {
                final Video v = BiliApi.getVideoDetail(bvid);
                handler.post(new Runnable() {
                    public void run() {
                        if (v != null) {
                            video = v;
                            tvTitle.setText(v.title);
                            tvUp.setText("UP主: " + v.ownerName);
                            tvStats.setText("播放 " + v.play + "  弹幕 " + v.danmaku + "  时长 " + formatDuration(v.duration));
                            tvDesc.setText(v.desc);
                            ImageLoader.load(v.pic, ivCover);
                        } else {
                            tvTitle.setText("加载失败");
                        }
                    }
                });
            }
        }).start();
    }

    private String formatDuration(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
