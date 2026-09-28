package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class OnboardingActivity extends Activity {

    private ViewFlipper flipper;
    private String selectedTab = "hot";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        flipper = new ViewFlipper(this);

        // 第一屏：欢迎
        LinearLayout page1 = new LinearLayout(this);
        page1.setOrientation(LinearLayout.VERTICAL);
        page1.setGravity(Gravity.CENTER);

        TextView welcome = new TextView(this);
        welcome.setText("欢迎使用\nHi！bili");
        welcome.setTextColor(0xFF1565C0);
        welcome.setTextSize(32);
        welcome.getPaint().setFakeBoldText(true);
        welcome.setGravity(Gravity.CENTER);
        welcome.setLineSpacing(8, 1);
        page1.addView(welcome);

        TextView sub = new TextView(this);
        sub.setText("\n简洁好看的 B 站客户端\n纯 Java 开发，兼容 Android 1.0+");
        sub.setTextColor(0xFF757575);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setLineSpacing(4, 1);
        page1.addView(sub);

        Button next1 = new Button(this);
        next1.setText("下一步");
        next1.setTextColor(0xFFFFFFFF);
        next1.setBackgroundColor(0xFF2196F3);
        LinearLayout.LayoutParams b1lp = new LinearLayout.LayoutParams(300, 90);
        b1lp.topMargin = 60;
        page1.addView(next1, b1lp);
        next1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flipper.showNext();
            }
        });

        flipper.addView(page1);

        // 第二屏：选择默认主页
        LinearLayout page2 = new LinearLayout(this);
        page2.setOrientation(LinearLayout.VERTICAL);
        page2.setGravity(Gravity.CENTER);

        TextView chooseTitle = new TextView(this);
        chooseTitle.setText("选择默认主页");
        chooseTitle.setTextColor(0xFF1565C0);
        chooseTitle.setTextSize(22);
        chooseTitle.getPaint().setFakeBoldText(true);
        chooseTitle.setGravity(Gravity.CENTER);
        page2.addView(chooseTitle);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, 30, 0, 0);

        String[][] tabs = {
            {"hot", "热门推荐"},
            {"search", "搜索"},
            {"personal", "我的"}
        };
        for (int i = 0; i < tabs.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(tabs[i][1]);
            rb.setTextColor(0xFF212121);
            rb.setTextSize(16);
            rb.setTag(tabs[i][0]);
            rb.setPadding(20, 16, 20, 16);
            if (i == 0) rb.setChecked(true);
            group.addView(rb);
        }
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                if (rb != null) selectedTab = (String) rb.getTag();
            }
        });
        page2.addView(group);

        Button finish = new Button(this);
        finish.setText("开始使用");
        finish.setTextColor(0xFFFFFFFF);
        finish.setBackgroundColor(0xFF2196F3);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(300, 90);
        flp.topMargin = 50;
        page2.addView(finish, flp);
        finish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PrefsManager.setDefaultTab(selectedTab);
                PrefsManager.setFirstLaunchDone();
                startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
                finish();
            }
        });

        flipper.addView(page2);
        root.addView(flipper, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        setContentView(root);
    }
}
