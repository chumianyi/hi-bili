package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
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
    private ImageView ivUserAvatar;
    private TextView tvUserName;
    private Button btnLogin;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserManager.load(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A2E);

        // 顶部栏
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF0F3460);
        topBar.setPadding(16, 12, 16, 12);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Hi！bili");
        tvTitle.setTextColor(0xFFE94560);
        tvTitle.setTextSize(20);
        tvTitle.getPaint().setFakeBoldText(true);
        topBar.addView(tvTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // 用户区域
        LinearLayout userArea = new LinearLayout(this);
        userArea.setOrientation(LinearLayout.HORIZONTAL);
        userArea.setGravity(Gravity.CENTER_VERTICAL);

        ivUserAvatar = new ImageView(this);
        ivUserAvatar.setLayoutParams(new LinearLayout.LayoutParams(36, 36));
        ivUserAvatar.setBackgroundColor(0xFF333333);
        userArea.addView(ivUserAvatar);

        tvUserName = new TextView(this);
        tvUserName.setTextColor(0xFFFFFFFF);
        tvUserName.setTextSize(12);
        tvUserName.setPadding(8, 0, 8, 0);
        userArea.addView(tvUserName);

        btnLogin = new Button(this);
        btnLogin.setText("登录");
        btnLogin.setTextColor(0xFFFFFFFF);
        btnLogin.setTextSize(11);
        btnLogin.setBackgroundColor(0xFFE94560);
        btnLogin.setPadding(12, 4, 12, 4);
        userArea.addView(btnLogin, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        topBar.addView(userArea);
        root.addView(topBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        updateUserUI();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (UserManager.isLogin()) {
                    UserManager.logout(MainActivity.this);
                    updateUserUI();
                    Toast.makeText(MainActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
                } else {
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), 100);
                }
            }
        });

        // 标签栏
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(0xFF16213E);

        final Button tabHot = makeTab("热门", true);
        final Button tabSearch = makeTab("搜索", false);
        final Button tabAbout = makeTab("关于", false);

        tabBar.addView(tabHot, new LinearLayout.LayoutParams(0, 70, 1));
        tabBar.addView(tabSearch, new LinearLayout.LayoutParams(0, 70, 1));
        tabBar.addView(tabAbout, new LinearLayout.LayoutParams(0, 70, 1));
        root.addView(tabBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ViewFlipper
        flipper = new ViewFlipper(this);

        // 热门页
        LinearLayout hotPageLayout = new LinearLayout(this);
        hotPageLayout.setOrientation(LinearLayout.VERTICAL);
        hotProgress = new ProgressBar(this);
        hotPageLayout.addView(hotProgress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));
        hotList = new ListView(this);
        hotList.setCacheColorHint(0);
        hotList.setDividerHeight(1);
        hotList.setDivider(new android.graphics.drawable.ColorDrawable(0xFF2A2A4E));
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
        etSearch.setHint("搜索视频...");
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setHintTextColor(0xFF666666);
        etSearch.setBackgroundColor(0xFF16213E);
        etSearch.setPadding(12, 10, 12, 10);
        searchRow.addView(etSearch, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button btnSearch = new Button(this);
        btnSearch.setText("搜索");
        btnSearch.setTextColor(0xFFFFFFFF);
        btnSearch.setBackgroundColor(0xFFE94560);
        searchRow.addView(btnSearch);
        searchPageLayout.addView(searchRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        searchProgress = new ProgressBar(this);
        searchPageLayout.addView(searchProgress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 60));
        searchList = new ListView(this);
        searchList.setCacheColorHint(0);
        searchList.setDividerHeight(1);
        searchList.setDivider(new android.graphics.drawable.ColorDrawable(0xFF2A2A4E));
        searchPageLayout.addView(searchList, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));
        flipper.addView(searchPageLayout);

        // 关于页
        LinearLayout aboutPage = new LinearLayout(this);
        aboutPage.setOrientation(LinearLayout.VERTICAL);
        aboutPage.setPadding(32, 40, 32, 32);
        aboutPage.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView aboutTitle = new TextView(this);
        aboutTitle.setText("Hi！bili");
        aboutTitle.setTextColor(0xFFE94560);
        aboutTitle.setTextSize(28);
        aboutTitle.getPaint().setFakeBoldText(true);
        aboutTitle.setGravity(Gravity.CENTER);
        aboutPage.addView(aboutTitle, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView aboutVer = new TextView(this);
        aboutVer.setText("版本 1.1.0");
        aboutVer.setTextColor(0xFF8892B0);
        aboutVer.setTextSize(14);
        aboutVer.setGravity(Gravity.CENTER);
        aboutVer.setPadding(0, 8, 0, 30);
        aboutPage.addView(aboutVer, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView aboutDesc = new TextView(this);
        aboutDesc.setText("一个简洁的 B 站第三方客户端\n调用 B 站官方 API，无自有后端\n纯 Java 开发，兼容 Android 1.0+");
        aboutDesc.setTextColor(0xFFCCCCCC);
        aboutDesc.setTextSize(13);
        aboutDesc.setGravity(Gravity.CENTER);
        aboutDesc.setLineSpacing(6, 1);
        aboutPage.addView(aboutDesc, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btnCheckUpdate = new Button(this);
        btnCheckUpdate.setText("检查更新");
        btnCheckUpdate.setTextColor(0xFFFFFFFF);
        btnCheckUpdate.setBackgroundColor(0xFF0F3460);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(300, 80);
        btnLp.topMargin = 40;
        aboutPage.addView(btnCheckUpdate, btnLp);

        btnCheckUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UpdateActivity.class));
            }
        });

        flipper.addView(aboutPage);
        root.addView(flipper, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 0, 1));

        setContentView(root);

        // 适配器
        hotAdapter = new VideoAdapter(this, hotVideos);
        hotList.setAdapter(hotAdapter);
        searchAdapter = new VideoAdapter(this, searchVideos);
        searchList.setAdapter(searchAdapter);

        // 标签切换
        tabHot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flipper.setDisplayedChild(0);
                tabHot.setBackgroundColor(0xFFE94560);
                tabSearch.setBackgroundColor(0xFF16213E);
                tabAbout.setBackgroundColor(0xFF16213E);
            }
        });
        tabSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flipper.setDisplayedChild(1);
                tabHot.setBackgroundColor(0xFF16213E);
                tabSearch.setBackgroundColor(0xFFE94560);
                tabAbout.setBackgroundColor(0xFF16213E);
            }
        });
        tabAbout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flipper.setDisplayedChild(2);
                tabHot.setBackgroundColor(0xFF16213E);
                tabSearch.setBackgroundColor(0xFF16213E);
                tabAbout.setBackgroundColor(0xFFE94560);
            }
        });

        // 热门点击
        hotList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= hotVideos.size()) return;
                Video v = (Video) hotVideos.get(pos);
                Intent it = new Intent(MainActivity.this, VideoDetailActivity.class);
                it.putExtra("bvid", v.bvid);
                startActivity(it);
            }
        });

        // 搜索点击
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                if (pos >= searchVideos.size()) return;
                Video v = (Video) searchVideos.get(pos);
                Intent it = new Intent(MainActivity.this, VideoDetailActivity.class);
                it.putExtra("bvid", v.bvid);
                startActivity(it);
            }
        });

        // 搜索按钮
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String kw = etSearch.getText().toString().trim();
                if (kw.length() > 0) doSearch(kw);
            }
        });

        // 无限热门列表：滚动到底部自动回顶刷新
        hotList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
            public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
                if (hotRefreshing || hotLoading) return;
                if (totalCount > 0 && firstVisible + visibleCount >= totalCount) {
                    hotRefreshing = true;
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            hotList.smoothScrollToPosition(0);
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    hotPage = 1;
                                    hotVideos.clear();
                                    loadHot();
                                    hotRefreshing = false;
                                }
                            }, 500);
                        }
                    }, 300);
                }
            }
        });

        // 启动公告
        AnnouncementHelper.show(this);

        // 加载热门
        loadHot();
    }

    private void updateUserUI() {
        if (UserManager.isLogin()) {
            tvUserName.setText(UserManager.getUname());
            btnLogin.setText("退出");
            if (UserManager.getAvatar() != null && UserManager.getAvatar().length() > 0) {
                ImageLoader loader = new ImageLoader(this);
                loader.display(UserManager.getAvatar(), ivUserAvatar, 36, 36);
            }
        } else {
            tvUserName.setText("");
            btnLogin.setText("登录");
            ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            updateUserUI();
        }
    }

    private Button makeTab(String text, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(14);
        b.setBackgroundColor(active ? 0xFFE94560 : 0xFF16213E);
        return b;
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
                        if (result != null) {
                            hotVideos.addAll(result);
                            hotAdapter.notifyDataSetChanged();
                            hotPage++;
                        }
                    }
                });
            }
        }).start();
    }

    private void doSearch(final String keyword) {
        searchProgress.setVisibility(View.VISIBLE);
        searchVideos.clear();
        searchAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.search(keyword, 1);
                handler.post(new Runnable() {
                    public void run() {
                        searchProgress.setVisibility(View.GONE);
                        if (result != null) {
                            searchVideos.addAll(result);
                            searchAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }).start();
    }
}
