package com.hi.bili;

import android.app.Application;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 全局 Application - 设置未捕获异常处理器，防止闪退
 */
public class HibiApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    pw.flush();
                    String errorLog = sw.toString();

                    Intent intent = new Intent(HibiApplication.this, CrashActivity.class);
                    intent.putExtra("error_log", errorLog);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {}
                System.exit(0);
            }
        });
    }
}
