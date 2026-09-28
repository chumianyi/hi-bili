package com.hi.bili;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerActivity extends Activity {

    private VideoView videoView;
    private DanmakuView danmakuView;
    private Button btnDanmaku, btnSize;
    private ProgressBar progress;
    private TextView tvError;
    private Handler handler = new Handler();
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private boolean hasFocus = false;
    private boolean destroyed = false;
    private String cid;
    private boolean danmakuSyncRunning = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PrefsManager.init(this);
            setContentView(R.layout.activity_player);

            videoView = (VideoView) findViewById(R.id.video_view);
            danmakuView = (DanmakuView) findViewById(R.id.danmaku_view);
            btnDanmaku = (Button) findViewById(R.id.btn_danmaku_toggle);
            progress = (ProgressBar) findViewById(R.id.progress);

            // 错误提示文本
            tvError = new TextView(this);
            tvError.setTextColor(0xFFFFFFFF);
            tvError.setTextSize(14);
            tvError.setGravity(Gravity.CENTER);
            tvError.setPadding(20, 20, 20, 20);
            tvError.setVisibility(View.GONE);
            FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            elp.gravity = Gravity.CENTER;
            ((FrameLayout) videoView.getParent()).addView(tvError, elp);

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
                            if (videoView != null && videoView.isPlaying()) videoView.pause();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            if (videoView != null && !videoView.isPlaying()) videoView.start();
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
                    try {
                        boolean show = !danmakuView.isShow();
                        danmakuView.setShow(show);
                        PrefsManager.setDanmakuEnabled(show);
                        btnDanmaku.setText(show ? "弹幕:开" : "弹幕:关");
                    } catch (Exception e) {}
                }
            });

            // 弹幕大小按钮
            try {
                btnSize = new Button(this);
                btnSize.setText("字号");
                btnSize.setTextColor(0xFFFFFFFF);
                btnSize.setTextSize(11);
                btnSize.setBackgroundColor(0x882196F3);
                FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, 36);
                slp.topMargin = 8;
                slp.rightMargin = 8;
                slp.gravity = Gravity.TOP | Gravity.RIGHT;
                ((FrameLayout) btnDanmaku.getParent().getParent()).addView(btnSize, slp);
                btnSize.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            final String[] sizes = {"小", "中", "大"};
                            final int[] vals = {14, 18, 24};
                            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(PlayerActivity.this);
                            b.setTitle("弹幕大小");
                            b.setItems(sizes, new android.content.DialogInterface.OnClickListener() {
                                public void onClick(android.content.DialogInterface d, int w) {
                                    try {
                                        PrefsManager.setDanmakuSize(vals[w]);
                                        danmakuView.setTextSize(vals[w]);
                                    } catch (Exception e) {}
                                }
                            });
                            b.show();
                        } catch (Exception e) {}
                    }
                });
            } catch (Exception e) {}

            danmakuView.setTextSize(PrefsManager.getDanmakuSize());

            progress.setVisibility(View.VISIBLE);
            loadDanmaku();
            loadAndPlay(avid, cid);

        } catch (Exception e) {
            showError("播放器初始化失败: " + e.getMessage());
        }
    }

    private void loadAndPlay(final String avid, final String cid) {
        new Thread(new Runnable() {
            public void run() {
                String playUrl = null;
                try {
                    playUrl = BiliApi.getPlayUrl(avid, cid);
                } catch (Exception e) {}
                final String finalUrl = playUrl;
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if (destroyed) return;
                            progress.setVisibility(View.GONE);
                            if (finalUrl == null || finalUrl.length() == 0) {
                                showError("视频地址获取失败\n可能是会员专享视频或需要登录");
                                return;
                            }
                            startPlay(finalUrl);
                        } catch (Exception e) {
                            showError("播放启动失败: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    private void startPlay(String playUrl) {
        try {
            requestAudioFocus();

            Uri uri = Uri.parse(playUrl);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Referer", "https://www.bilibili.com");
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");

            // API 21+ 支持带 headers 的 setVideoURI
            if (Build.VERSION.SDK_INT >= 21) {
                videoView.setVideoURI(uri, headers);
            } else {
                videoView.setVideoURI(uri);
            }

            MediaController mc = new MediaController(this);
            videoView.setMediaController(mc);

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    try {
                        mp.setVolume(1.0f, 1.0f);
                        if (!destroyed) {
                            videoView.start();
                            startDanmakuSync();
                        }
                    } catch (Exception e) {}
                }
            });

            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    try {
                        showError("视频播放失败 (错误码: " + what + ")\n可能是视频源不可用或网络问题");
                    } catch (Exception e) {}
                    return true;
                }
            });

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    danmakuSyncRunning = false;
                }
            });

        } catch (Exception e) {
            showError("播放初始化失败: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        try {
            if (destroyed) return;
            progress.setVisibility(View.GONE);
            if (tvError != null) {
                tvError.setText(msg);
                tvError.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {}
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
                List list = null;
                try {
                    list = BiliApi.getDanmaku(cid);
                } catch (Exception e) {}
                final List finalList = list;
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if (destroyed) return;
                            if (finalList != null) danmakuView.setDanmakus(finalList);
                        } catch (Exception e) {}
                    }
                });
            }
        }).start();
    }

    private void startDanmakuSync() {
        if (danmakuSyncRunning) return;
        danmakuSyncRunning = true;
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (destroyed || !danmakuSyncRunning) return;
                    if (videoView != null && videoView.isPlaying()) {
                        int pos = videoView.getCurrentPosition();
                        danmakuView.updateTime(pos / 1000f);
                    }
                } catch (Exception e) {}
                if (!destroyed && danmakuSyncRunning) {
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    protected void onPause() {
        super.onPause();
        try { if (videoView != null && videoView.isPlaying()) videoView.pause(); } catch (Exception e) {}
    }

    protected void onDestroy() {
        destroyed = true;
        danmakuSyncRunning = false;
        try { if (videoView != null) videoView.stopPlayback(); } catch (Exception e) {}
        if (hasFocus && Build.VERSION.SDK_INT >= 8) {
            try { audioManager.abandonAudioFocus(focusListener); } catch (Exception e) {}
        }
        super.onDestroy();
    }
}
