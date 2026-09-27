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
    private Button btnDanmaku;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private boolean hasFocus = false;
    private String cid;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoView = (VideoView) findViewById(R.id.video_view);
        danmakuView = (DanmakuView) findViewById(R.id.danmaku_view);
        btnDanmaku = (Button) findViewById(R.id.btn_danmaku_toggle);
        progress = (ProgressBar) findViewById(R.id.progress);

        final String avid = getIntent().getStringExtra("avid");
        cid = getIntent().getStringExtra("cid");
        final String title = getIntent().getStringExtra("title");

        if (title != null) setTitle(title);

        // 音频焦点
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        focusListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    if (videoView.isPlaying()) videoView.pause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    if (!videoView.isPlaying()) videoView.start();
                }
            }
        };

        btnDanmaku.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean show = !danmakuView.isShow();
                danmakuView.setShow(show);
                btnDanmaku.setText(show ? "弹幕:开" : "弹幕:关");
            }
        });

        progress.setVisibility(View.VISIBLE);

        // 加载弹幕
        loadDanmaku();

        // 获取播放地址
        new Thread(new Runnable() {
            public void run() {
                final String playUrl = BiliApi.getPlayUrl(avid, cid);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        if (playUrl != null && playUrl.length() > 0) {
                            requestAudioFocus();
                            MediaController mc = new MediaController(PlayerActivity.this);
                            videoView.setMediaController(mc);
                            videoView.setVideoURI(Uri.parse(playUrl));
                            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer mp) {
                                    mp.setVolume(1.0f, 1.0f);
                                    videoView.start();
                                    startDanmakuSync();
                                }
                            });
                            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                public boolean onError(MediaPlayer mp, int what, int extra) {
                                    finish();
                                    return true;
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= 8) {
            int result = audioManager.requestAudioFocus(focusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
    }

    private void loadDanmaku() {
        new Thread(new Runnable() {
            public void run() {
                final List list = BiliApi.getDanmaku(cid);
                handler.post(new Runnable() {
                    public void run() {
                        danmakuView.setDanmakus(list);
                    }
                });
            }
        }).start();
    }

    private void startDanmakuSync() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    int pos = videoView.getCurrentPosition();
                    danmakuView.updateTime(pos / 1000f);
                }
                handler.postDelayed(this, 100);
            }
        }, 100);
    }

    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        if (hasFocus && Build.VERSION.SDK_INT >= 8) {
            audioManager.abandonAudioFocus(focusListener);
        }
    }
}
