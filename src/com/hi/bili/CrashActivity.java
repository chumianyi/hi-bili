package com.hi.bili;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 全局崩溃日志页面 - 防止闪退，显示错误信息
 */
public class CrashActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String errorLog = getIntent().getStringExtra("error_log");
        if (errorLog == null) errorLog = "未知错误";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);
        root.setPadding(20, 30, 20, 20);

        TextView title = new TextView(this);
        title.setText("应用可能崩溃");
        title.setTextColor(0xFFE53935);
        title.setTextSize(20);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("\n以下是错误详情，可截图反馈：");
        subtitle.setTextColor(0xFF757575);
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1);
        scrollLp.topMargin = 16;
        scrollLp.bottomMargin = 16;

        TextView log = new TextView(this);
        log.setText(errorLog);
        log.setTextColor(0xFF424242);
        log.setTextSize(11);
        log.setBackgroundColor(0xFFFFFFFF);
        log.setPadding(14, 14, 14, 14);
        scroll.addView(log);
        root.addView(scroll, scrollLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnRestart = new Button(this);
        btnRestart.setText("重新启动");
        btnRestart.setTextColor(0xFFFFFFFF);
        btnRestart.setBackgroundColor(0xFF2196F3);
        btnRestart.setTextSize(14);
        btnRow.addView(btnRestart, new LinearLayout.LayoutParams(0, 70, 1));

        Button btnExit = new Button(this);
        btnExit.setText("退出应用");
        btnExit.setTextColor(0xFFFFFFFF);
        btnExit.setBackgroundColor(0xFF9E9E9E);
        btnExit.setTextSize(14);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(0, 70, 1);
        exitLp.leftMargin = 12;
        btnRow.addView(btnExit, exitLp);

        root.addView(btnRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        btnRestart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pi = PendingIntent.getActivity(CrashActivity.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        am.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pi);
                    }
                } catch (Exception e) {}
                System.exit(0);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.exit(0);
            }
        });
    }
}
