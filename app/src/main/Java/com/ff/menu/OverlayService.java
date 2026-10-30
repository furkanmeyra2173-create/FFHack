package com.ff.menu;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class OverlayService extends Service {

    private WindowManager wm;
    private View          fab;
    private LinearLayout  panel;
    private EspView       espView;
    private boolean       panelOpen = false;

    private final boolean[] feat = new boolean[6];
    private static final String CFG = "/data/local/tmp/.hcfg";
    private static final String ESP = "/data/local/tmp/.hesp";

    private final Handler  handler = new Handler(Looper.getMainLooper());
    private final Runnable espTick = new Runnable() {
        @Override
        public void run() {
            if (feat[4]) {
                espView.setVisibility(View.VISIBLE);
                espView.loadEsp(ESP);
            } else {
                espView.setVisibility(View.GONE);
            }
            handler.postDelayed(this, 33);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        addEspView();
        addFab();
        addPanel();
        handler.post(espTick);
    }

    private void addEspView() {
        espView = new EspView(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        espView.setVisibility(View.GONE);
        wm.addView(espView, lp);
    }

    private void addFab() {
        fab = new View(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.argb(220, 200, 30, 30));
        gd.setStroke(3, Color.WHITE);
        fab.setBackground(gd);

        WindowManager.LayoutParams lp = makeParams(64, 64);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 20;
        lp.y = 320;

        fab.setOnTouchListener(new DragTap(lp, fab, wm, this::togglePanel));
        wm.addView(fab, lp);
    }

    private void addPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(28, 28, 28, 28);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235, 8, 8, 8));
        bg.setCornerRadius(18f);
        bg.setStroke(2, Color.argb(200, 200, 30, 30));
        panel.setBackground(bg);

        // Başlık
        TextView title = new TextView(this);
        title.setText("◉ FF HACK");
        title.setTextColor(Color.argb(255, 220, 40, 40));
        title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        panel.addView(title);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(Color.argb(100, 200, 30, 30));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dlp.bottomMargin = 16;
        div.setLayoutParams(dlp);
        panel.addView(div);

        String[] labels = {
            "No Recoil",
            "Silent Aim",
            "Wallhack",
            "Fast Reload",
            "ESP",
            "Camera Lock"
        };

        for (int i = 0; i < labels.length; i++) {
            panel.addView(makeRow(labels[i], i));
        }

        WindowManager.LayoutParams lp = makeParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 94;
        lp.y = 300;
        panel.setVisibility(View.GONE);
        wm.addView(panel, lp);
    }

    private View makeRow(String label, int idx) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(feat[idx]);
        sw.setOnCheckedChangeListener((v, checked) -> {
            feat[idx] = checked;
            writeConfig();
        });

        row.addView(tv);
        row.addView(sw);
        return row;
    }

    private void togglePanel() {
        panelOpen = !panelOpen;
        panel.setVisibility(panelOpen ? View.VISIBLE : View.GONE);
    }

    private void writeConfig() {
        try (java.io.FileOutputStream f = new java.io.FileOutputStream(CFG)) {
            byte[] buf = new byte[feat.length];
            for (int i = 0; i < feat.length; i++)
                buf[i] = feat[i] ? (byte) 1 : 0;
            f.write(buf);
        } catch (Exception ignored) {}
    }

    private WindowManager.LayoutParams makeParams(int w, int h) {
        return new WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
    }

    private Notification buildNotification() {
        String channelId = "ff_hack";
        NotificationChannel ch = new NotificationChannel(
            channelId, "background", NotificationManager.IMPORTANCE_MIN);
        NotificationManager nm =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(ch);

        return new Notification.Builder(this, channelId)
            .setContentTitle("System Service")
            .setContentText("Running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(espTick);
        if (fab     != null) wm.removeView(fab);
        if (panel   != null) wm.removeView(panel);
        if (espView != null) wm.removeView(espView);
        super.onDestroy();
    }

    // ── Sürükleme + tap ──
    static class DragTap implements View.OnTouchListener {
        private final WindowManager.LayoutParams lp;
        private final View            view;
        private final WindowManager   wm;
        private final Runnable        onTap;
        private int sx, sy, ix, iy;
        private boolean dragging;

        DragTap(WindowManager.LayoutParams lp, View v,
                WindowManager wm, Runnable onTap) {
            this.lp = lp; this.view = v;
            this.wm = wm; this.onTap = onTap;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx = (int) e.getRawX(); sy = (int) e.getRawY();
                    ix = lp.x;             iy = lp.y;
                    dragging = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) e.getRawX() - sx;
                    int dy = (int) e.getRawY() - sy;
                    if (Math.abs(dx) > 6 || Math.abs(dy) > 6) dragging = true;
                    if (dragging) {
                        lp.x = ix + dx;
                        lp.y = iy + dy;
                        wm.updateViewLayout(view, lp);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!dragging) onTap.run();
                    break;
            }
            return true;
        }
    }
}
