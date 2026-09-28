package com.hi.bili;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    private DanmakuView danmakuView;
    private Button btnDanmaku, btnPlayPause;
    private SeekBar seekBar;
    private TextView tvTimeCurrent, tvTimeTotal, tvError, tvLoading;
    private LinearLayout loadingLayout, controlBar;
    private Handler handler = new Handler();
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private boolean hasFocus = false;
    private boolean destroyed = false;
    private boolean surfaceReady = false;
    private String pendingUrl = null;
    private String cid;
    private int duration = 0;
    private boolean danmakuSyncRunning = false;
    private boolean isSeeking = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PrefsManager.init(this);
            setContentView(R.layout.activity_player);

            surfaceView = (SurfaceView) findViewById(R.id.surface_view);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);

            danmakuView = (DanmakuView) findViewById(R.id.danmaku_view);
            btnDanmaku = (Button) findViewById(R.id.btn_danmaku_toggle);
            btnPlayPause = (Button) findViewById(R.id.btn_play_pause);
            seekBar = (SeekBar) findViewById(R.id.seek_bar);
            tvTimeCurrent = (TextView) findViewById(R.id.tv_time_current);
            tvTimeTotal = (TextView) findViewById(R.id.tv_time_total);
            tvError = (TextView) findViewById(R.id.tv_error);
            tvLoading = (TextView) findViewById(R.id.tv_loading);
            loadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
            controlBar = (LinearLayout) findViewById(R.id.control_bar);

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
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
                        }
                    } catch (Exception e) {}
                }
            };

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

            danmakuView.setTextSize(PrefsManager.getDanmakuSize());

            btnPlayPause.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.pause();
                                btnPlayPause.setText("▶");
                            } else {
                                mediaPlayer.start();
                                btnPlayPause.setText("||");
                            }
                        }
                    } catch (Exception e) {}
                }
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser) tvTimeCurrent.setText(formatTime(progress));
                }
                public void onStartTrackingTouch(SeekBar sb) { isSeeking = true; }
                public void onStopTrackingTouch(SeekBar sb) {
                    try {
                        if (mediaPlayer != null) mediaPlayer.seekTo(sb.getProgress());
                    } catch (Exception e) {}
                    isSeeking = false;
                }
            });

            surfaceView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        controlBar.setVisibility(controlBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    } catch (Exception e) {}
                }
            });

            loadDanmaku();
            fetchPlayUrl(avid, cid);

        } catch (Exception e) {
            showError("播放器初始化失败: " + e.getMessage());
        }
    }

    private void fetchPlayUrl(final String avid, final String cid) {
        tvLoading.setText("正在获取视频地址...");
        new Thread(new Runnable() {
            public void run() {
                String playUrl = null;
                try { playUrl = BiliApi.getPlayUrl(avid, cid); } catch (Exception e) {}
                final String finalUrl = playUrl;
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if (destroyed) return;
                            if (finalUrl == null || finalUrl.length() == 0) {
                                showError("视频地址获取失败\n可能是会员专享视频或需要登录");
                                return;
                            }
                            pendingUrl = finalUrl;
                            if (surfaceReady) {
                                startPlay(finalUrl);
                                pendingUrl = null;
                            } else {
                                tvLoading.setText("等待画面就绪...");
                            }
                        } catch (Exception e) {
                            showError("播放启动失败: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    private void startPlay(String url) {
        try {
            loadingLayout.setVisibility(View.VISIBLE);
            tvLoading.setText("缓冲中...");
            tvError.setVisibility(View.GONE);

            requestAudioFocus();

            if (mediaPlayer != null) {
                try { mediaPlayer.release(); } catch (Exception e) {}
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDisplay(surfaceHolder);

            Uri uri = Uri.parse(url);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Referer", "https://www.bilibili.com");
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");

            if (Build.VERSION.SDK_INT >= 14) {
                mediaPlayer.setDataSource(this, uri, headers);
            } else {
                mediaPlayer.setDataSource(url);
            }

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    try {
                        if (destroyed) return;
                        loadingLayout.setVisibility(View.GONE);
                        controlBar.setVisibility(View.VISIBLE);
                        duration = mp.getDuration();
                        seekBar.setMax(duration);
                        tvTimeTotal.setText(formatTime(duration));
                        mp.setVolume(1.0f, 1.0f);
                        mp.start();
                        btnPlayPause.setText("||");
                        startProgressSync();
                        startDanmakuSync();
                    } catch (Exception e) {}
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    try { showError("视频播放失败 (错误码: " + what + ")\n可能是视频源不可用或网络问题"); } catch (Exception e) {}
                    return true;
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    try { btnPlayPause.setText("▶"); danmakuSyncRunning = false; } catch (Exception e) {}
                }
            });

            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    try { if (duration > 0) seekBar.setSecondaryProgress(duration * percent / 100); } catch (Exception e) {}
                }
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            showError("播放初始化失败: " + e.getMessage());
        }
    }

    private void startProgressSync() {
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (destroyed) return;
                    if (mediaPlayer != null && !isSeeking) {
                        int pos = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(pos);
                        tvTimeCurrent.setText(formatTime(pos));
                    }
                } catch (Exception e) {}
                if (!destroyed) handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void startDanmakuSync() {
        if (danmakuSyncRunning) return;
        danmakuSyncRunning = true;
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (destroyed || !danmakuSyncRunning) return;
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int pos = mediaPlayer.getCurrentPosition();
                        danmakuView.updateTime(pos / 1000f);
                    }
                } catch (Exception e) {}
                if (!destroyed && danmakuSyncRunning) handler.postDelayed(this, 100);
            }
        }, 100);
    }

    private void showError(String msg) {
        try {
            if (destroyed) return;
            loadingLayout.setVisibility(View.GONE);
            controlBar.setVisibility(View.GONE);
            tvError.setText(msg);
            tvError.setVisibility(View.VISIBLE);
        } catch (Exception e) {}
    }

    private String formatTime(int ms) {
        try {
            int s = ms / 1000;
            int m = s / 60;
            s = s % 60;
            return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        } catch (Exception e) { return "00:00"; }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                int result = audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            } catch (Exception e) {}
        }
    }

    private void loadDanmaku() {
        new Thread(new Runnable() {
            public void run() {
                List list = null;
                try { list = BiliApi.getDanmaku(cid); } catch (Exception e) {}
                final List finalList = list;
                handler.post(new Runnable() {
                    public void run() {
                        try { if (destroyed) return; if (finalList != null) danmakuView.setDanmakus(finalList); } catch (Exception e) {}
                    }
                });
            }
        }).start();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            surfaceReady = true;
            if (pendingUrl != null) {
                startPlay(pendingUrl);
                pendingUrl = null;
            } else if (mediaPlayer != null) {
                mediaPlayer.setDisplay(holder);
            }
        } catch (Exception e) {}
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            surfaceReady = false;
            if (mediaPlayer != null) mediaPlayer.setDisplay(null);
        } catch (Exception e) {}
    }

    protected void onPause() {
        super.onPause();
        try { if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause(); } catch (Exception e) {}
    }

    protected void onDestroy() {
        destroyed = true;
        danmakuSyncRunning = false;
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {}
        if (hasFocus && Build.VERSION.SDK_INT >= 8) {
            try { audioManager.abandonAudioFocus(focusListener); } catch (Exception e) {}
        }
        super.onDestroy();
    }
}
