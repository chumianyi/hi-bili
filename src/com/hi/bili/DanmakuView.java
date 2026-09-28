package com.hi.bili;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DanmakuView extends View {

    private List danmakus = new ArrayList();
    private List active = new ArrayList();
    private Paint paint;
    private boolean showDanmaku = true;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private float textSize = 18;
    private long lastTime = 0;
    private float speed = 120; // pixels per second

    class DanmakuItem {
        String text;
        float x;
        float y;
        int color;
        float size;
        float width;
    }

    public DanmakuView(Context context) {
        super(context);
        init();
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(2, 1, 1, Color.BLACK);
    }

    public void setDanmakus(List list) {
        danmakus.clear();
        danmakus.addAll(list);
        active.clear();
        invalidate();
    }

    public void setShow(boolean show) {
        showDanmaku = show;
        if (!show) active.clear();
        invalidate();
    }

    public void setTextSize(float size) {
        textSize = size;
        invalidate();
    }

    public boolean isShow() { return showDanmaku; }

    public void updateTime(float currentTime) {
        if (!showDanmaku) return;
        // Add danmakus that should appear at this time
        for (int i = 0; i < danmakus.size(); i++) {
            BiliApi.Danmaku d = (BiliApi.Danmaku) danmakus.get(i);
            if (d.time <= currentTime && d.time > currentTime - 0.5) {
                addDanmaku(d);
            }
        }
        invalidate();
    }

    private void addDanmaku(BiliApi.Danmaku d) {
        if (viewWidth == 0) return;
        DanmakuItem item = new DanmakuItem();
        item.text = d.text;
        item.x = viewWidth;
        item.color = d.color != 0 ? d.color : Color.WHITE;
        item.size = d.size > 0 ? d.size : textSize;
        paint.setTextSize(item.size);
        item.width = paint.measureText(d.text);
        // Random lane
        int lanes = Math.max(1, (int)(viewHeight / (item.size + 8)));
        int lane = (int)(Math.random() * lanes);
        item.y = (lane + 1) * (item.size + 8);
        if (item.y > viewHeight - item.size) item.y = viewHeight - item.size;
        active.add(item);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showDanmaku) return;

        long now = System.currentTimeMillis();
        float dt = lastTime > 0 ? (now - lastTime) / 1000f : 0.016f;
        lastTime = now;

        Iterator it = active.iterator();
        while (it.hasNext()) {
            DanmakuItem item = (DanmakuItem) it.next();
            item.x -= speed * dt;
            if (item.x + item.width < 0) {
                it.remove();
                continue;
            }
            paint.setTextSize(item.size);
            paint.setColor(item.color);
            canvas.drawText(item.text, item.x, item.y, paint);
        }

        if (active.size() > 0) {
            invalidate();
        }
    }
}
