package com.hi.bili;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.hi.bili.model.Video;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ViewFlipper viewFlipper;
    private TextView tabHot, tabSearch, tabAbout;
    private ListView listHot, listSearch;
    private EditText etSearch;
    private Button btnSearch, btnAboutUpdate;

    private List hotList = new ArrayList();
    private List searchList = new ArrayList();
    private VideoAdapter hotAdapter, searchAdapter;
    private Handler handler = new Handler();
    private int hotPage = 1;
    private int searchPage = 1;
    private String currentKeyword = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnnouncementHelper.show(this);

        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        tabHot = (TextView) findViewById(R.id.tab_hot);
        tabSearch = (TextView) findViewById(R.id.tab_search);
        tabAbout = (TextView) findViewById(R.id.tab_about);
        listHot = (ListView) findViewById(R.id.list_hot);
        listSearch = (ListView) findViewById(R.id.list_search);
        etSearch = (EditText) findViewById(R.id.et_search);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnAboutUpdate = (Button) findViewById(R.id.btn_about_update);

        hotAdapter = new VideoAdapter(this, hotList);
        searchAdapter = new VideoAdapter(this, searchList);
        listHot.setAdapter(hotAdapter);
        listSearch.setAdapter(searchAdapter);

        tabHot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(0); }
        });
        tabSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(1); }
        });
        tabAbout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(2); }
        });

        listHot.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Video v = (Video) hotList.get(position);
                openDetail(v.bvid);
            }
        });
        listSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Video v = (Video) searchList.get(position);
                openDetail(v.bvid);
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSearch(); }
        });
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

        btnAboutUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UpdateActivity.class));
            }
        });

        loadHot();
    }

    private void switchTab(int index) {
        viewFlipper.setDisplayedChild(index);
        tabHot.setTextColor(index == 0 ? 0xFFE94560 : 0xFF8892B0);
        tabSearch.setTextColor(index == 1 ? 0xFFE94560 : 0xFF8892B0);
        tabAbout.setTextColor(index == 2 ? 0xFFE94560 : 0xFF8892B0);
        if (index == 0) tabHot.getPaint().setFakeBoldText(true); else tabHot.getPaint().setFakeBoldText(false);
        if (index == 1) tabSearch.getPaint().setFakeBoldText(true); else tabSearch.getPaint().setFakeBoldText(false);
        if (index == 2) tabAbout.getPaint().setFakeBoldText(true); else tabAbout.getPaint().setFakeBoldText(false);
    }

    private void openDetail(String bvid) {
        Intent intent = new Intent(this, VideoDetailActivity.class);
        intent.putExtra("bvid", bvid);
        startActivity(intent);
    }

    private void loadHot() {
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.getPopular(hotPage);
                handler.post(new Runnable() {
                    public void run() {
                        hotList.clear();
                        hotList.addAll(result);
                        hotAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    private void doSearch() {
        final String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) return;
        currentKeyword = keyword;
        searchPage = 1;
        new Thread(new Runnable() {
            public void run() {
                final List result = BiliApi.search(keyword, searchPage);
                handler.post(new Runnable() {
                    public void run() {
                        searchList.clear();
                        searchList.addAll(result);
                        searchAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }
}
