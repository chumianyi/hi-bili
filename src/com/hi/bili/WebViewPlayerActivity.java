package com.hi.bili;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 内部 WebView 播放器 - 加载 B 站官方嵌入播放器
 * 嵌入播放器页面自己处理 Referer 和视频流，不需要直链
 */
public class WebViewPlayerActivity extends Activity {

    private WebView webView;
    private ProgressBar progress;
    private TextView tvError;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenContainer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PrefsManager.init(this);

            FrameLayout root = new FrameLayout(this);
            root.setBackgroundColor(0xFF000000);

            webView = new WebView(this);
            root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));

            fullscreenContainer = new FrameLayout(this);
            fullscreenContainer.setVisibility(View.GONE);
            fullscreenContainer.setBackgroundColor(0xFF000000);
            root.addView(fullscreenContainer, new FrameLayout.LayoutParams(
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

            final String avid = getIntent().getStringExtra("avid");
            final String cid = getIntent().getStringExtra("cid");
            final String bvid = getIntent().getStringExtra("bvid");

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setBuiltInZoomControls(true);
            settings.setSupportZoom(true);

            webView.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int newProgress) {
                    try {
                        if (newProgress >= 100) progress.setVisibility(View.GONE);
                    } catch (Exception e) {}
                }
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    try {
                        customView = view;
                        customViewCallback = callback;
                        fullscreenContainer.addView(view);
                        fullscreenContainer.setVisibility(View.VISIBLE);
                        webView.setVisibility(View.GONE);
                    } catch (Exception e) {}
                }
                public void onHideCustomView() {
                    try {
                        if (customView != null) {
                            fullscreenContainer.removeView(customView);
                            customView = null;
                            fullscreenContainer.setVisibility(View.GONE);
                            webView.setVisibility(View.VISIBLE);
                            if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                        }
                    } catch (Exception e) {}
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    try {
                        progress.setVisibility(View.GONE);
                        tvError.setText("WebView 加载失败\n可在设置中切换为其他播放方式");
                        tvError.setVisibility(View.VISIBLE);
                    } catch (Exception e) {}
                }
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    try { view.loadUrl(url); } catch (Exception e) {}
                    return true;
                }
            });

            // 加载 B 站官方嵌入播放器
            String playerUrl;
            if (bvid != null && bvid.length() > 0) {
                playerUrl = "https://player.bilibili.com/player.html?bvid=" + bvid + "&cid=" + cid + "&page=1&high_quality=1&danmaku=0&autoplay=1";
            } else {
                playerUrl = "https://player.bilibili.com/player.html?aid=" + avid + "&cid=" + cid + "&page=1&high_quality=1&danmaku=0&autoplay=1";
            }

            try {
                webView.loadUrl(playerUrl);
            } catch (Exception e) {
                tvError.setText("WebView 加载失败: " + e.getMessage());
                tvError.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            try {
                TextView t = new TextView(this);
                t.setText("WebView 播放器初始化失败: " + e.getMessage());
                t.setTextColor(0xFFFFFFFF);
                setContentView(t);
            } catch (Exception ex) {}
        }
    }

    public void onBackPressed() {
        try {
            if (customView != null) {
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                fullscreenContainer.removeView(customView);
                customView = null;
                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                return;
            }
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return;
            }
        } catch (Exception e) {}
        super.onBackPressed();
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
