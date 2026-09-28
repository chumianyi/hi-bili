package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UploadActivity extends Activity {

    private static final int REQUEST_PICK_VIDEO = 1001;
    private TextView tvFile;
    private EditText etTitle, etDesc;
    private Button btnPick, btnUpload;
    private ProgressBar progress;
    private String videoPath;
    private Handler handler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("发布视频");
        title.setTextColor(0xFF1565C0);
        title.setTextSize(20);
        title.getPaint().setFakeBoldText(true);
        root.addView(title);

        // 选择文件
        btnPick = new Button(this);
        btnPick.setText("选择视频文件");
        btnPick.setTextColor(0xFFFFFFFF);
        btnPick.setBackgroundColor(0xFF2196F3);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 70);
        plp.topMargin = 20;
        root.addView(btnPick, plp);

        tvFile = new TextView(this);
        tvFile.setText("未选择文件");
        tvFile.setTextColor(0xFF757575);
        tvFile.setTextSize(12);
        tvFile.setPadding(0, 8, 0, 8);
        root.addView(tvFile);

        // 标题
        TextView lblTitle = new TextView(this);
        lblTitle.setText("视频标题");
        lblTitle.setTextColor(0xFF212121);
        lblTitle.setTextSize(14);
        lblTitle.setPadding(0, 12, 0, 6);
        root.addView(lblTitle);

        etTitle = new EditText(this);
        etTitle.setHint("请输入标题");
        etTitle.setTextColor(0xFF212121);
        etTitle.setHintTextColor(0xFF9E9E9E);
        etTitle.setBackgroundColor(0xFFFFFFFF);
        etTitle.setPadding(14, 10, 14, 10);
        root.addView(etTitle, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 简介
        TextView lblDesc = new TextView(this);
        lblDesc.setText("视频简介");
        lblDesc.setTextColor(0xFF212121);
        lblDesc.setTextSize(14);
        lblDesc.setPadding(0, 12, 0, 6);
        root.addView(lblDesc);

        etDesc = new EditText(this);
        etDesc.setHint("请输入简介（可选）");
        etDesc.setTextColor(0xFF212121);
        etDesc.setHintTextColor(0xFF9E9E9E);
        etDesc.setBackgroundColor(0xFFFFFFFF);
        etDesc.setPadding(14, 10, 14, 10);
        etDesc.setMinLines(3);
        root.addView(etDesc, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 分区提示
        TextView lblTip = new TextView(this);
        lblTip.setText("\n分区: 生活 / 日常\n（投稿接口需要完整的上传凭证和分片上传流程）");
        lblTip.setTextColor(0xFF9E9E9E);
        lblTip.setTextSize(11);
        root.addView(lblTip);

        // 上传按钮
        btnUpload = new Button(this);
        btnUpload.setText("发布视频");
        btnUpload.setTextColor(0xFFFFFFFF);
        btnUpload.setBackgroundColor(0xFF43A047);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 80);
        ulp.topMargin = 24;
        root.addView(btnUpload, ulp);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));

        setContentView(root);

        btnPick.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickVideo(); }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doUpload(); }
        });
    }

    private void pickVideo() {
        try {
            // 运行时权限
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{"android.permission.READ_MEDIA_VIDEO", "android.permission.READ_EXTERNAL_STORAGE"}, 200);
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_PICK_VIDEO);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                String[] proj = {MediaStore.Video.Media.DATA};
                Cursor cursor = managedQuery(uri, proj, null, null, null);
                if (cursor != null) {
                    int col = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    cursor.moveToFirst();
                    videoPath = cursor.getString(col);
                    tvFile.setText("已选择: " + videoPath);
                } else {
                    videoPath = uri.getPath();
                    tvFile.setText("已选择: " + videoPath);
                }
            } catch (Exception e) {
                Toast.makeText(this, "获取文件失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doUpload() {
        if (videoPath == null || videoPath.length() == 0) {
            Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        String t = etTitle.getText().toString().trim();
        if (t.length() == 0) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        new Thread(new Runnable() {
            public void run() {
                // B 站完整投稿流程：preupload -> 分片上传 -> submit
                // 这里获取上传凭证，实际完整上传需要多步交互
                final String[] cred = BiliApi.getUploadCredential();
                handler.post(new Runnable() {
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        if (cred != null) {
                            Toast.makeText(UploadActivity.this,
                                "已获取上传凭证，完整投稿需要分片上传流程\n文件: " + videoPath,
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(UploadActivity.this, "获取上传凭证失败，请确认已登录", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}
