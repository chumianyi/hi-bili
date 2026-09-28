package com.hi.bili;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import java.util.List;

public class PlayerActivity extends Activity {

    private VideoView videoView;
    private DanmakuView danmakuView;
    private Button btnDanmaku, btnSize;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private boolean hasFocus = false;
    private String cid;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);
        setContentView(R.layout.activity_player);

        videoView = (VideoView) findViewById(R.id.video_view);
        danmakuView = (DanmakuView) findViewById(R.id.danmaku_view);
        btnDanmaku = (Button) findViewById(R.id.btn_danmaku_toggle);
        progress = (ProgressBar) findViewById(R.id.progress);

        final String avid = getIntent().getStringExtra("avid");
        cid = getIntent().getStringExtra("cid");
        final String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        focusListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                try {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        if (videoView.isPlaying()) videoView.pause();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        if (!videoView.isPlaying()) videoView.start();
                    }
                } catch (Exception e) {}
            }
        };

        // 弹幕开关
        boolean dmEnabled = PrefsManager.getDanmakuEnabled();
        danmakuView.setShow(dmEnabled);
        btnDanmaku.setText(dmEnabled ? "弹幕:开" : "弹幕:关");
        btnDanmaku.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean show = !danmakuView.isShow();
                danmakuView.setShow(show);
                PrefsManager.setDanmakuEnabled(show);
                btnDanmaku.setText(show ? "弹幕:开" : "弹幕:关");
            }
        });

        // 弹幕大小按钮（动态添加）
        btnSize = new Button(this);
        btnSize.setText("字号");
        btnSize.setTextColor(0xFFFFFFFF);
        btnSize.setTextSize(11);
        btnSize.setBackgroundColor(0x882196F3);
        android.widget.FrameLayout.LayoutParams slp = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, 36);
        slp.topMargin = 8;
        slp.rightMargin = 8;
        slp.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
        ((android.widget.FrameLayout) btnDanmaku.getParent()).addView(btnSize, slp);
        btnSize.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] sizes = {"小", "中", "大"};
                final int[] vals = {14, 18, 24};
                android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(PlayerActivity.this);
                b.setTitle("弹幕大小");
                b.setItems(sizes, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        PrefsManager.setDanmakuSize(vals[w]);
                        danmakuView.setTextSize(vals[w]);
                    }
                });
                b.show();
            }
        });

        // 设置弹幕大小
        danmakuView.setTextSize(PrefsManager.getDanmakuSize());

        progress.setVisibility(View.VISIBLE);
        loadDanmaku();

        new Thread(new Runnable() {
            public void run() {
                final String playUrl = BiliApi.getPlayUrl(avid, cid);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        if (playUrl != null && playUrl.length() > 0) {
                            requestAudioFocus();
                            try {
                                MediaController mc = new MediaController(PlayerActivity.this);
                                videoView.setMediaController(mc);
                                videoView.setVideoURI(Uri.parse(playUrl));
                                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    public void onPrepared(MediaPlayer mp) {
                                        try { mp.setVolume(1.0f, 1.0f); } catch (Exception e) {}
                                        videoView.start();
                                        startDanmakuSync();
                                    }
                                });
                                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                    public boolean onError(MediaPlayer mp, int what, int extra) {
                                        finish(); return true;
                                    }
                                });
                            } catch (Exception e) {
                                finish();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                int result = audioManager.requestAudioFocus(focusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            } catch (Exception e) {}
        }
    }

    private void loadDanmaku() {
        new Thread(new Runnable() {
            public void run() {
                final List list = BiliApi.getDanmaku(cid);
                handler.post(new Runnable() {
                    public void run() {
                        try { danmakuView.setDanmakus(list); } catch (Exception e) {}
                    }
                });
            }
        }).start();
    }

    private void startDanmakuSync() {
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (videoView != null && videoView.isPlaying()) {
                        int pos = videoView.getCurrentPosition();
                        danmakuView.updateTime(pos / 1000f);
                    }
                } catch (Exception e) {}
                handler.postDelayed(this, 100);
            }
        }, 100);
    }

    protected void onPause() {
        super.onPause();
        try { if (videoView != null && videoView.isPlaying()) videoView.pause(); } catch (Exception e) {}
    }

    protected void onDestroy() {
        super.onDestroy();
        try { if (videoView != null) videoView.stopPlayback(); } catch (Exception e) {}
        if (hasFocus && Build.VERSION.SDK_INT >= 8) {
            try { audioManager.abandonAudioFocus(focusListener); } catch (Exception e) {}
        }
    }
}
