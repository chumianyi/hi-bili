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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

public class UpdateActivity extends Activity {

    private static final String UPDATE_URL = "https://chumianyi.github.io/biliclassic-update/version.json";

    private TextView tvCurrent, tvLatest, tvStatus;
    private Button btnCheck, btnUpdate;
    private Handler handler = new Handler();
    private String latestVersionName = "";
    private int latestVersionCode = 0;
    private String downloadUrl = "";
    private long downloadId = -1;
    private DownloadReceiver receiver;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        tvCurrent = (TextView) findViewById(R.id.tv_current_version);
        tvLatest = (TextView) findViewById(R.id.tv_latest_version);
        tvStatus = (TextView) findViewById(R.id.tv_update_status);
        btnCheck = (Button) findViewById(R.id.btn_check_update);
        btnUpdate = (Button) findViewById(R.id.btn_do_update);

        tvCurrent.setText(getCurrentVersionName());

        btnCheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkUpdate();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDownload();
            }
        });

        receiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);
    }

    protected void onDestroy() {
        super.onDestroy();
        try {
            if (receiver != null) unregisterReceiver(receiver);
        } catch (Exception e) {
        }
    }

    private String getCurrentVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void checkUpdate() {
        tvStatus.setText("正在检查更新...");
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                handler.post(new Runnable() {
                    public void run() {
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
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class DownloadReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId && Build.VERSION.SDK_INT >= 9) {
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    Uri uri = dm.getUriForDownloadedFile(downloadId);
                    if (uri != null) {
                        installApk(uri);
                    }
                }
            }
        }
    }
}
