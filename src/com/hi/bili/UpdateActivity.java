package com.hi.bili;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;

public class UpdateActivity extends Activity {

    private static final String UPDATE_URL = "https://chumianyi.github.io/biliclassic-update/version.json";

    private TextView tvCurrent, tvLatest, tvStatus;
    private Button btnCheck, btnUpdate;
    private ProgressBar progress;
    private Handler handler = new Handler();
    private String latestVersionName = "";
    private int latestVersionCode = 0;
    private String downloadUrl = "";
    private long downloadId = -1;
    private DownloadReceiver receiver;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);
        root.setPadding(32, 40, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("检查更新");
        title.setTextColor(0xFF1565C0);
        title.setTextSize(22);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 卡片
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = 24;
        root.addView(card, cardLp);

        tvCurrent = makeInfoRow(card, "当前版本", getCurrentVersionName());
        tvLatest = makeInfoRow(card, "最新版本", "—");

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 50));

        tvStatus = new TextView(this);
        tvStatus.setText("点击下方按钮检查更新");
        tvStatus.setTextColor(0xFF757575);
        tvStatus.setTextSize(13);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 16, 0, 16);
        root.addView(tvStatus, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        btnCheck = new Button(this);
        btnCheck.setText("检查更新");
        btnCheck.setTextColor(0xFFFFFFFF);
        btnCheck.setBackgroundColor(0xFF2196F3);
        btnCheck.setTextSize(15);
        root.addView(btnCheck, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 80));

        btnUpdate = new Button(this);
        btnUpdate.setText("立即更新");
        btnUpdate.setTextColor(0xFFFFFFFF);
        btnUpdate.setBackgroundColor(0xFF43A047);
        btnUpdate.setTextSize(15);
        btnUpdate.setVisibility(View.GONE);
        LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 80);
        upLp.topMargin = 12;
        root.addView(btnUpdate, upLp);

        setContentView(root);

        btnCheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { checkUpdate(); }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startDownload(); }
        });

        receiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);
    }

    private TextView makeInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(0xFF757575);
        tvLabel.setTextSize(14);
        row.addView(tvLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(0xFF212121);
        tvVal.setTextSize(14);
        tvVal.getPaint().setFakeBoldText(true);
        row.addView(tvVal);
        parent.addView(row);
        return tvVal;
    }

    protected void onDestroy() {
        super.onDestroy();
        try { if (receiver != null) unregisterReceiver(receiver); } catch (Exception e) {}
    }

    private String getCurrentVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) { return "2.0.0"; }
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) { return 3; }
    }

    private void checkUpdate() {
        tvStatus.setText("正在检查更新...");
        progress.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.GONE);
        new Thread(new Runnable() {
            public void run() {
                String json = HttpUtil.get(UPDATE_URL);
                if (json != null) {
                    try {
                        JSONObject obj = new JSONObject(json);
                        latestVersionName = obj.optString("versionName", "");
                        latestVersionCode = obj.optInt("versionCode", 0);
                        downloadUrl = obj.optString("downloadUrl", "");
                        handler.post(new Runnable() {
                            public void run() {
                                progress.setVisibility(View.GONE);
                                tvLatest.setText(latestVersionName);
                                if (latestVersionCode > getCurrentVersionCode()) {
                                    tvStatus.setText("发现新版本，点击下方按钮更新");
                                    btnUpdate.setVisibility(View.VISIBLE);
                                } else {
                                    tvStatus.setText("已是最新版本");
                                }
                            }
                        });
                        return;
                    } catch (Exception e) {}
                }
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        tvStatus.setText("检查失败，请检查网络连接");
                    }
                });
            }
        }).start();
    }

    private void startDownload() {
        if (downloadUrl == null || downloadUrl.length() == 0) return;
        if (Build.VERSION.SDK_INT >= 9) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Hi！bili 更新");
            request.setDescription("正在下载更新...");
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setMimeType("application/vnd.android.package-archive");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "HiBili_update.apk");
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(request);
            tvStatus.setText("开始下载，请看通知栏");
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            startActivity(intent);
        }
    }

    private void installApk(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= 24) {
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HiBili_update.apk");
                Uri contentUri = HibiFileProvider.getUriForFile(this, f);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) { e.printStackTrace(); }
    }

    class DownloadReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId && Build.VERSION.SDK_INT >= 9) {
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    Uri uri = dm.getUriForDownloadedFile(downloadId);
                    if (uri != null) installApk(uri);
                }
            }
        }
    }
}
