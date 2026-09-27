package com.hi.bili;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class PlayerActivity extends Activity {

    private VideoView videoView;
    private ProgressBar progress;
    private Handler handler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoView = (VideoView) findViewById(R.id.video_view);
        progress = (ProgressBar) findViewById(R.id.progress);

        final String avid = getIntent().getStringExtra("avid");
        final String cid = getIntent().getStringExtra("cid");
        final String title = getIntent().getStringExtra("title");

        if (title != null) {
            setTitle(title);
        }

        progress.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            public void run() {
                final String playUrl = BiliApi.getPlayUrl(avid, cid);
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        if (playUrl != null && playUrl.length() > 0) {
                            MediaController mc = new MediaController(PlayerActivity.this);
                            videoView.setMediaController(mc);
                            videoView.setVideoURI(Uri.parse(playUrl));
                            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer mp) {
                                    videoView.start();
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

    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
