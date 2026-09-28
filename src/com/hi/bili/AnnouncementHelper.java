package com.hi.bili;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class AnnouncementHelper {

    public static void show(final Activity activity) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("温馨提示");
            TextView msg = new TextView(activity);
            msg.setText("本项目是免费的，如果你是买的，你就被骗了，请退款。");
            msg.setTextColor(0xFF424242);
            msg.setTextSize(15);
            msg.setPadding(24, 20, 24, 10);
            msg.setLineSpacing(4, 1);
            builder.setView(msg);
            builder.setCancelable(false);
            builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
