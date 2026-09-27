package com.hi.bili;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class AnnouncementHelper {

    public static void show(final Activity activity) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("温馨提示");
            builder.setMessage("本项目是免费的，如果你是买的，你就被骗了，请退款。");
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
