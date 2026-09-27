package com.hi.bili;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends Activity {

    private ImageView ivQR;
    private TextView tvStatus;
    private Button btnCancel;
    private Handler handler = new Handler();
    private String qrcodeKey;
    private boolean polling = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A2E);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("扫码登录");
        title.setTextColor(0xFFE94560);
        title.setTextSize(22);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ivQR = new ImageView(this);
        ivQR.setBackgroundColor(0xFFFFFFFF);
        ivQR.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(480, 480);
        qrLp.topMargin = 24;
        qrLp.bottomMargin = 16;
        root.addView(ivQR, qrLp);

        tvStatus = new TextView(this);
        tvStatus.setText("正在生成二维码...");
        tvStatus.setTextColor(0xFF8892B0);
        tvStatus.setTextSize(14);
        tvStatus.setGravity(Gravity.CENTER);
        root.addView(tvStatus, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        btnCancel = new Button(this);
        btnCancel.setText("取消");
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setBackgroundColor(0xFF0F3460);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(200, 80);
        btnLp.topMargin = 24;
        root.addView(btnCancel, btnLp);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                polling = false;
                finish();
            }
        });

        setContentView(root);
        generateQR();
    }

    private void generateQR() {
        new Thread(new Runnable() {
            public void run() {
                final String[] result = BiliApi.generateQRCode();
                handler.post(new Runnable() {
                    public void run() {
                        if (result != null) {
                            qrcodeKey = result[1];
                            Bitmap bmp = createQRBitmap(result[0], 480);
                            if (bmp != null) {
                                ivQR.setImageBitmap(bmp);
                                tvStatus.setText("请使用哔哩哔哩APP扫码登录");
                                startPolling();
                            } else {
                                tvStatus.setText("二维码生成失败");
                            }
                        } else {
                            tvStatus.setText("获取二维码失败，请检查网络");
                        }
                    }
                });
            }
        }).start();
    }

    private Bitmap createQRBitmap(String content, int size) {
        try {
            Map hints = new HashMap();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.setPixels(pixels, 0, width, 0, 0, width, height);
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startPolling() {
        new Thread(new Runnable() {
            public void run() {
                int count = 0;
                while (polling && count < 120) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    count++;
                    final int code = BiliApi.pollQRCode(qrcodeKey);
                    handler.post(new Runnable() {
                        public void run() {
                            if (code == 0) {
                                polling = false;
                                tvStatus.setText("登录成功！");
                                fetchUserInfo();
                            } else if (code == 86038) {
                                tvStatus.setText("二维码已失效，请重新生成");
                                polling = false;
                            } else if (code == 86090) {
                                tvStatus.setText("已扫码，请在手机上确认");
                            } else if (code == 86101) {
                                tvStatus.setText("等待扫码...");
                            }
                        }
                    });
                    if (code == 0 || code == 86038) break;
                }
            }
        }).start();
    }

    private void fetchUserInfo() {
        new Thread(new Runnable() {
            public void run() {
                final String[] info = BiliApi.getUserInfo();
                handler.post(new Runnable() {
                    public void run() {
                        if (info != null) {
                            UserManager.save(LoginActivity.this,
                                UserManager.getCookie(), info[0], info[1], info[2]);
                            Toast.makeText(LoginActivity.this, "欢迎，" + info[0], Toast.LENGTH_SHORT).show();
                        }
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        }).start();
    }

    protected void onDestroy() {
        super.onDestroy();
        polling = false;
    }
}
