package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.hi.bili.model.Video;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ViewFlipper flipper;
    private ListView hotList;
    private VideoAdapter hotAdapter;
    private List hotVideos = new ArrayList();
    private ListView searchList;
    private VideoAdapter searchAdapter;
    private List searchVideos = new ArrayList();
    private EditText etSearch;
    private ProgressBar hotProgress;
    private ProgressBar searchProgress;
    private Handler handler = new Handler();
    private int hotPage = 1;
    private boolean hotLoading = false;
    private boolean hotRefreshing = false;
    private boolean isLiteMode = false;
    private Button tabHot, tabSearch, tabPersonal;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager.init(this);
        UserManager.load(this);

        if (PrefsManager.isFirstLaunch()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        isLiteMode = Build.VERSION.SDK_INT < 3;
        buildUI();
        AnnouncementHelper.show(this);

        String defTab = PrefsManager.getDefaultTab();
        if ("search".equals(defTab)) {
            flipper.setDisplayedChild(1); updateTabs(1);
        } else if ("personal".equals(defTab) && !isLiteMode) {
            flipper.setDisplayedChild(2); updateTabs(2);
        } else {
            flipper.setDisplayedChild(0); updateTabs(0);
        }
        loadHot();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F9FF);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF2196F3);
        topBar.setPadding(20, 14, 20, 14);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Hi！bili");
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(20);
        tvTitle.getPaint().setFakeBoldText(true);
        topBar.addView(tvTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        if (!isLiteMode) {
            TextView tvUser = new TextView(this);
            tvUser.setText(UserManager.isLogin() ? UserManager.getUname() : "未登录");
            tvUser.setTextColor(0xFFFFFFFF);
            tvUser.setTextSize(12);
            tvUser.setPadding(0, 0, 12, 0);
            topBar.addView(tvUser);
        }
        root.addView(topBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(0xFFFFFFFF);
        tabHot = makeTab("热门", true);
        tabSearch = makeTab("搜索", false);
        tabBar.addView(tabHot, new LinearLayout.LayoutParams(0, 64, 1));
        tabBar.addView(tabSearch, new LinearLayout.LayoutParams(0, 64, 1));
        if (!isLiteMode) {
            tabPersonal = makeTab("我的", false);
            tabBar.addView(tabPersonal, new LinearLayout.LayoutParams(0, 64, 1));
        }
        root.addView(tabBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        flipper = new ViewFlipper(this);

        // 热门页
        LinearLayout hotPageLayout = new LinearLayout(this);
        hotPageLayout.setOrientation(LinearLayout.VERTICAL);
        hotProgress = new ProgressBar(this);
        hotPageLayout.addView(hotProgress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 50));
        hotList = new ListView(this);
        hotList.setCacheColorHint(0);
        hotList.setDividerHeight(1);
        hotList.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE0E0E0));
        hotPageLayout.addView(hotList, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));
        flipper.addView(hotPageLayout);

        // 搜索页
        LinearLayout searchPageLayout = new LinearLayout(this);
        searchPageLayout.setOrientation(LinearLayout.VERTICAL);
        searchPageLayout.setPadding(16, 16, 16, 16);
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        etSearch = new EditText(this);
        etSearch.setHint("搜索视频 / BV号 / AV号");
        etSearch.setTextColor(0xFF212121);
        etSearch.setHintTextColor(0xFF9E9E9E);
        etSearch.setBackgroundColor(0xFFE3F2FD);
        etSearch.setPadding(14, 10, 14, 10);
        etSearch.setSingleLine(true);
        searchRow.addView(etSearch, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button btnSearch = new Button(this);
        btnSearch.setText("搜索");
        btnSearch.setTextColor(0xFFFFFFFF);
        btnSearch.setBackgroundColor(0xFF2196F3);
        searchRow.addView(btnSearch);
        searchPageLayout.addView(searchRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        searchProgress = new ProgressBar(this);
        searchPageLayout.addView(searchProgress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 50));
        searchList = new ListView(this);
        searchList.setCacheColorHint(0);
        searchList.setDividerHeight(1);
        searchList.setDivider(new android.graphics.drawable.ColorDrawable(0xFFE0E0E0));
        searchPageLayout.addView(searchList, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));
        flipper.addView(searchPageLayout);

        if (!isLiteMode) {
            flipper.addView(buildPersonalPage());
        }

        root.addView(flipper, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));
        setContentView(root);

        hotAdapter = new VideoAdapter(this, hotVideos);
        hotList.setAdapter(hotAdapter);
        searchAdapter = new VideoAdapter(this, searchVideos);
        searchList.setAdapter(searchAdapter);

        tabHot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { flipper.setDisplayedChild(0); updateTabs(0); }
        });
        tabSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { flipper.setDisplayedChild(1); updateTabs(1); }
        });
        if (tabPersonal != null) {
            tabPersonal.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { flipper.setDisplayedChild(2); updateTabs(2); }
            });
        }

        hotList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= hotVideos.size()) return;
                Video v = (Video) hotVideos.get(pos);
                openVideo(v.bvid, v.title, v.pic);
            }
        });
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= searchVideos.size()) return;
                Video v = (Video) searchVideos.get(pos);
                openVideo(v.bvid, v.title, v.pic);
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSearch(); }
        });
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                doSearch(); return true;
            }
        });

        hotList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
            public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
                if (hotRefreshing || hotLoading) return;
                if (totalCount > 0 && firstVisible + visibleCount >= totalCount) {
                    hotRefreshing = true;
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            hotList.smoothScrollToPosition(0);
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    hotPage = 1; hotVideos.clear(); loadHot(); hotRefreshing = false;
                                }
                            }, 400);
                        }
                    }, 200);
                }
            }
        });
    }

    private LinearLayout buildPersonalPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);

        LinearLayout userCard = new LinearLayout(this);
        userCard.setOrientation(LinearLayout.HORIZONTAL);
        userCard.setBackgroundColor(0xFFFFFFFF);
        userCard.setPadding(20, 20, 20, 20);
        userCard.setGravity(Gravity.CENTER_VERTICAL);

        final ImageView ivAvatar = new ImageView(this);
        ivAvatar.setLayoutParams(new LinearLayout.LayoutParams(72, 72));
        ivAvatar.setBackgroundColor(0xFFE3F2FD);
        userCard.addView(ivAvatar);

        LinearLayout userInfo = new LinearLayout(this);
        userInfo.setOrientation(LinearLayout.VERTICAL);
        userInfo.setPadding(16, 0, 0, 0);
        TextView tvName = new TextView(this);
        tvName.setText(UserManager.isLogin() ? UserManager.getUname() : "未登录");
        tvName.setTextColor(0xFF212121);
        tvName.setTextSize(18);
        tvName.getPaint().setFakeBoldText(true);
        userInfo.addView(tvName);
        TextView tvTip = new TextView(this);
        tvTip.setText(UserManager.isLogin() ? "已登录 B 站账号" : "点击登录享受更多功能");
        tvTip.setTextColor(0xFF757575);
        tvTip.setTextSize(12);
        userInfo.addView(tvTip);
        userCard.addView(userInfo);
        page.addView(userCard, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (UserManager.isLogin() && UserManager.getAvatar().length() > 0) {
            new ImageLoader(this).display(UserManager.getAvatar(), ivAvatar, 72, 72);
        }

        String[][] items = {
            {"发布视频", "upload"}, {"我的视频", "myvideos"}, {"每日签到", "checkin"},
            {"缓存管理", "cache"}, {"弹幕设置", "danmaku"}, {"检查更新", "update"}, {"退出登录", "logout"}
        };
        for (int i = 0; i < items.length; i++) {
            final String tag = items[i][1];
            Button btn = new Button(this);
            btn.setText(items[i][0]);
            btn.setTextColor(0xFF212121);
            btn.setBackgroundColor(0xFFFFFFFF);
            btn.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            btn.setPadding(24, 0, 24, 0);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, 56);
            blp.topMargin = 2;
            page.addView(btn, blp);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { handlePersonalAction(tag); }
            });
        }
        return page;
    }

    private void handlePersonalAction(String tag) {
        try {
            if ("upload".equals(tag)) {
                if (!UserManager.isLogin()) { Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); return; }
                startActivity(new Intent(this, UploadActivity.class));
            } else if ("myvideos".equals(tag)) {
                if (!UserManager.isLogin()) { Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); return; }
                Intent it = new Intent(this, PersonalActivity.class); it.putExtra("mode", "myvideos"); startActivity(it);
            } else if ("checkin".equals(tag)) {
                if (!UserManager.isLogin()) { Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show(); startActivity(new Intent(this, LoginActivity.class)); return; }
                doCheckIn();
            } else if ("cache".equals(tag)) {
                Intent it = new Intent(this, PersonalActivity.class); it.putExtra("mode", "cache"); startActivity(it);
            } else if ("danmaku".equals(tag)) {
                showDanmakuSettings();
            } else if ("update".equals(tag)) {
                startActivity(new Intent(this, UpdateActivity.class));
            } else if ("logout".equals(tag)) {
                UserManager.logout(this); Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show(); recreate();
            }
        } catch (Exception e) {
            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void doCheckIn() {
        new Thread(new Runnable() {
            public void run() {
                final String[] result = BiliApi.dailyCheckIn();
                handler.post(new Runnable() {
                    public void run() { Toast.makeText(MainActivity.this, result[1], Toast.LENGTH_SHORT).show(); }
                });
            }
        }).start();
    }

    private void showDanmakuSettings() {
        final int current = PrefsManager.getDanmakuSize();
        final String[] sizes = {"小", "中", "大"};
        final int[] sizeVals = {14, 18, 24};
        int checked = 1;
        for (int i = 0; i < sizeVals.length; i++) { if (sizeVals[i] == current) checked = i; }
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("弹幕大小");
        final int fChecked = checked;
        builder.setSingleChoiceItems(sizes, fChecked, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                PrefsManager.setDanmakuSize(sizeVals[which]);
                Toast.makeText(MainActivity.this, "已设置为" + sizes[which], Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openVideo(String bvid, String title, String pic) {
        try {
            PrefsManager.addHistory(bvid, title, pic);
            Intent it = new Intent(this, VideoDetailActivity.class);
            it.putExtra("bvid", bvid);
            startActivity(it);
        } catch (Exception e) {
            Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    private Button makeTab(String text, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(active ? 0xFF2196F3 : 0xFF9E9E9E);
        b.setTextSize(14);
        b.setBackgroundColor(0xFFFFFFFF);
        return b;
    }

    private void updateTabs(int active) {
        tabHot.setTextColor(active == 0 ? 0xFF2196F3 : 0xFF9E9E9E);
        tabSearch.setTextColor(active == 1 ? 0xFF2196F3 : 0xFF9E9E9E);
        if (tabPersonal != null) tabPersonal.setTextColor(active == 2 ? 0xFF2196F3 : 0xFF9E9E9E);
    }

    private void doSearch() {
        String kw = etSearch.getText().toString().trim();
        if (kw.length() == 0) return;
        if (kw.matches("^BV[a-zA-Z0-9]{10}$")) { openVideo(kw, kw, ""); return; }
        if (kw.matches("^[aA][vV]\\d+$")) { searchByAid(kw.replaceAll("[aA][vV]", "")); return; }
        if (kw.matches("^\\d+$") && kw.length() > 4) { searchByAid(kw); return; }

        searchProgress.setVisibility(View.VISIBLE);
        searchVideos.clear();
        searchAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.search(etSearch.getText().toString().trim(), 1);
                handler.post(new Runnable() {
                    public void run() {
                        searchProgress.setVisibility(View.GONE);
                        if (result != null) { searchVideos.addAll(result); searchAdapter.notifyDataSetChanged(); }
                    }
                });
            }
        }).start();
    }

    private void searchByAid(final String aid) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String json = HttpUtil.get("https://api.bilibili.com/x/web-interface/view?aid=" + aid);
                    if (json != null) {
                        JSONObject obj = new JSONObject(json);
                        if (obj.optInt("code") == 0) {
                            final String bvid = obj.getJSONObject("data").optString("bvid", "");
                            final String title = obj.getJSONObject("data").optString("title", "");
                            handler.post(new Runnable() {
                                public void run() {
                                    if (bvid.length() > 0) openVideo(bvid, title, "");
                                    else Toast.makeText(MainActivity.this, "未找到视频", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }
                    }
                } catch (Exception e) {}
                handler.post(new Runnable() {
                    public void run() { Toast.makeText(MainActivity.this, "AV号查找失败", Toast.LENGTH_SHORT).show(); }
                });
            }
        }).start();
    }

    private void loadHot() {
        if (hotLoading) return;
        hotLoading = true;
        hotProgress.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.getPopular(hotPage);
                handler.post(new Runnable() {
                    public void run() {
                        hotProgress.setVisibility(View.GONE);
                        hotLoading = false;
                        if (result != null) { hotVideos.addAll(result); hotAdapter.notifyDataSetChanged(); hotPage++; }
                    }
                });
            }
        }).start();
    }
}
