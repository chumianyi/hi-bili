package com.hi.bili;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 内部 WebView 播放器 - 用 WebView 加载视频直链，在应用内播放
 */
public class WebViewPlayerActivity extends Activity {

    private WebView webView;
    private ProgressBar progress;
    private TextView tvError;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PrefsManager.init(this);

            FrameLayout root = new FrameLayout(this);
            root.setBackgroundColor(0xFF000000);

            webView = new WebView(this);
            root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

            progress = new ProgressBar(this);
            FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            plp.gravity = Gravity.CENTER;
            root.addView(progress, plp);

            tvError = new TextView(this);
            tvError.setTextColor(0xFFFF5252);
            tvError.setTextSize(14);
            tvError.setGravity(Gravity.CENTER);
            tvError.setPadding(20, 20, 20, 20);
            tvError.setVisibility(View.GONE);
            root.addView(tvError, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

            setContentView(root);

            final String title = getIntent().getStringExtra("title");
            if (title != null) setTitle(title);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setBuiltInZoomControls(true);

            webView.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int newProgress) {
                    try {
                        if (newProgress >= 100) {
                            progress.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {}
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    try {
                        progress.setVisibility(View.GONE);
                        tvError.setText("WebView 播放失败\n可在设置中切换为其他播放方式");
                        tvError.setVisibility(View.VISIBLE);
                    } catch (Exception e) {}
                }
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    try { view.loadUrl(url); } catch (Exception e) {}
                    return true;
                }
            });

            // 获取视频直链并加载
            final String avid = getIntent().getStringExtra("avid");
            final String cid = getIntent().getStringExtra("cid");

            new Thread(new Runnable() {
                public void run() {
                    String playUrl = null;
                    try { playUrl = BiliApi.getPlayUrl(avid, cid); } catch (Exception e) {}
                    final String finalUrl = playUrl;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                if (finalUrl == null || finalUrl.length() == 0) {
                                    progress.setVisibility(View.GONE);
                                    tvError.setText("视频地址获取失败\n可在设置中切换为其他播放方式");
                                    tvError.setVisibility(View.VISIBLE);
                                    return;
                                }
                                // 用 HTML5 video 标签播放直链
                                String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body{margin:0;background:#000;}video{width:100%;height:100%;object-fit:contain;}</style></head><body><video src=\"" + finalUrl + "\" controls autoplay></video></body></html>";
                                webView.loadDataWithBaseURL("https://www.bilibili.com", html, "text/html", "UTF-8", null);
                            } catch (Exception e) {
                                tvError.setText("WebView 初始化失败");
                                tvError.setVisibility(View.VISIBLE);
                                progress.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            try {
                TextView t = new TextView(this);
                t.setText("WebView 播放器初始化失败: " + e.getMessage());
                t.setTextColor(0xFFFFFFFF);
                setContentView(t);
            } catch (Exception ex) {}
        }
    }

    protected void onPause() {
        super.onPause();
        try { if (webView != null) webView.onPause(); } catch (Exception e) {}
    }

    protected void onResume() {
        super.onResume();
        try { if (webView != null) webView.onResume(); } catch (Exception e) {}
    }

    protected void onDestroy() {
        try {
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
                webView = null;
            }
        } catch (Exception e) {}
        super.onDestroy();
    }
}
