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
    private ScrollView    scroll;
    private LinearLayout  panel;
    private EspView       espView;
    private boolean       panelOpen = false;

    private final boolean[] feat = new boolean[22];
    private static final String CFG = "/data/local/tmp/.hcfg";
    private static final String ESP = "/data/local/tmp/.hesp";

    private final Handler  handler = new Handler(Looper.getMainLooper());
    private final Runnable tick    = new Runnable(){
        @Override public void run(){
            espView.loadEsp(ESP);
            espView.setFeatFlags(feat);
            handler.postDelayed(this,33);
        }
    };

    @Override
    public void onCreate(){
        super.onCreate();
        startForeground(1,buildNotif());
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        addEspView();
        addFab();
        addPanel();
        handler.post(tick);
    }

    private void addEspView(){
        espView=new EspView(this);
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        wm.addView(espView,lp);
    }

    private void addFab(){
        fab=new View(this);
        GradientDrawable gd=new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.argb(220,200,30,30));
        gd.setStroke(3,Color.WHITE);
        fab.setBackground(gd);
        WindowManager.LayoutParams lp=mkParams(64,64);
        lp.gravity=Gravity.TOP|Gravity.START;
        lp.x=20; lp.y=320;
        fab.setOnTouchListener(new DragTap(lp,fab,wm,this::togglePanel));
        wm.addView(fab,lp);
    }

    private void addPanel(){
        scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);

        panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24,24,24,24);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.argb(240,8,8,8));
        bg.setCornerRadius(18f);
        bg.setStroke(2,Color.argb(200,200,30,30));
        panel.setBackground(bg);

        // Başlık
        addTitle("◉ FF HACK");

        // Kategoriler
        addCategory("── SAVAŞ ──");
        addRow("No Recoil",    0);
        addRow("Silent Aim",   1);
        addRow("Aimbot",       2);
        addRow("Rapid Fire",   3);
        addRow("No Sway",      4);
        addRow("Bullet Speed", 5);

        addCategory("── HAREKET ──");
        addRow("Fast Reload",  6);
        addRow("Speed x1.5",   7);
        addRow("Fly Hack",     8);
        addRow("Anti AIM",     9);

        addCategory("── GÖRÜŞ ──");
        addRow("Wallhack",     10);
        addRow("ESP",          11);
        addRow("Item ESP",     12);
        addRow("Vehicle ESP",  13);
        addRow("Loot Filter",  14);
        addRow("Camera Lock",  15);
        addRow("Geniş FOV",    16);

        addCategory("── EKRAN ──");
        addRow("Crosshair",    17);
        addRow("Radar",        18);
        addRow("Night Mode",   19);
        addRow("No Grass",     20);
        addRow("Bright Mode",  21);

        scroll.addView(panel);

        WindowManager.LayoutParams lp=mkParams(340,
            WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity=Gravity.TOP|Gravity.START;
        lp.x=94; lp.y=300;
        // Ekranın %70'ini geçmesin
        int maxH=(int)(getResources().getDisplayMetrics().heightPixels*0.70f);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(340,maxH));

        scroll.setVisibility(View.GONE);
        wm.addView(scroll,lp);
    }

    private void addTitle(String text){
        TextView tv=new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.argb(255,220,40,40));
        tv.setTextSize(17f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0,0,0,16);
        panel.addView(tv);
        // ince çizgi
        View div=new View(this);
        div.setBackgroundColor(Color.argb(80,200,30,30));
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,1);
        dlp.bottomMargin=16;
        div.setLayoutParams(dlp);
        panel.addView(div);
    }

    private void addCategory(String text){
        TextView tv=new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.argb(180,255,180,50));
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin=12; lp.bottomMargin=4;
        tv.setLayoutParams(lp);
        panel.addView(tv);
    }

    private void addRow(String label,int idx){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8,8,8,8);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // aktif arka plan
        GradientDrawable rowBg=new GradientDrawable();
        rowBg.setColor(Color.argb(30,255,255,255));
        rowBg.setCornerRadius(8f);
        row.setBackground(rowBg);

        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin=4;
        row.setLayoutParams(rlp);

        TextView tv=new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));

        Switch sw=new Switch(this);
        sw.setChecked(feat[idx]);
        sw.setOnCheckedChangeListener((v,checked)->{
            feat[idx]=checked;
            // Karşılıklı toggle mantığı
            if(idx==2&&checked) feat[15]=false; // Aimbot açıksa CamLock kapat
            if(idx==15&&checked)feat[2]=false;  // CamLock açıksa Aimbot kapat
            if(idx==19&&checked)feat[21]=false; // NightMode açıksa BrightMode kapat
            if(idx==21&&checked)feat[19]=false;
            writeConfig();
        });

        row.addView(tv);
        row.addView(sw);
        panel.addView(row);
    }

    private void togglePanel(){
        panelOpen=!panelOpen;
        scroll.setVisibility(panelOpen?View.VISIBLE:View.GONE);
    }

    private void writeConfig(){
        try(java.io.FileOutputStream f=new java.io.FileOutputStream(CFG)){
            byte[] buf=new byte[feat.length];
            for(int i=0;i<feat.length;i++) buf[i]=feat[i]?(byte)1:0;
            f.write(buf);
        }catch(Exception ignored){}
    }

    private WindowManager.LayoutParams mkParams(int w,int h){
        return new WindowManager.LayoutParams(w,h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
    }

    private Notification buildNotif(){
        String ch="ff"; 
        NotificationChannel nc=new NotificationChannel(ch,"bg",
            NotificationManager.IMPORTANCE_MIN);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
            .createNotificationChannel(nc);
        return new Notification.Builder(this,ch)
            .setContentTitle("System Service")
            .setSmallIcon(android.R.drawable.ic_menu_manage).build();
    }

    @Override public IBinder onBind(Intent i){return null;}

    @Override public void onDestroy(){
        handler.removeCallbacks(tick);
        if(fab!=null)    wm.removeView(fab);
        if(scroll!=null) wm.removeView(scroll);
        if(espView!=null)wm.removeView(espView);
        super.onDestroy();
    }

    static class DragTap implements View.OnTouchListener{
        private final WindowManager.LayoutParams lp;
        private final View v; private final WindowManager wm;
        private final Runnable tap;
        private int sx,sy,ix,iy; private boolean drag;
        DragTap(WindowManager.LayoutParams lp,View v,WindowManager wm,Runnable tap){
            this.lp=lp;this.v=v;this.wm=wm;this.tap=tap;
        }
        @Override public boolean onTouch(View view,MotionEvent e){
            switch(e.getAction()){
                case MotionEvent.ACTION_DOWN:
                    sx=(int)e.getRawX();sy=(int)e.getRawY();
                    ix=lp.x;iy=lp.y;drag=false;break;
                case MotionEvent.ACTION_MOVE:
                    int dx=(int)e.getRawX()-sx,dy=(int)e.getRawY()-sy;
                    if(Math.abs(dx)>6||Math.abs(dy)>6)drag=true;
                    if(drag){lp.x=ix+dx;lp.y=iy+dy;wm.updateViewLayout(v,lp);}
                    break;
                case MotionEvent.ACTION_UP:
                    if(!drag)tap.run();break;
            }
            return true;
        }
    }
}
