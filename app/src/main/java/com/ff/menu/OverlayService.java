package com.ff.menu;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

public class OverlayService extends Service {

    private WindowManager wm;
    private View          fab;
    private View          menuRoot;
    private LinearLayout  contentArea;
    private EspView       espView;
    private boolean       menuOpen=false;
    private int           activeTab=0;

    // 37 feature
    private final boolean[] feat=new boolean[37];
    private static final String CFG="/data/local/tmp/.hcfg";
    private static final String ESP="/data/local/tmp/.hesp";

    // Tab tanımları
    private static final String[] TAB_NAMES ={"⚔","🛡","🏃","👁","🖥"};
    private static final String[] TAB_LABELS={"SAVAŞ","SAVUNMA","HAREKET","GÖRÜŞ","EKRAN"};
    private static final int[][] TAB_FEATS  ={
        {0,1,2,3,4,5,6,7,8},           // SAVAŞ
        {9,10,11,12,13,14},             // SAVUNMA
        {15,16,17,18,19,20},            // HAREKET
        {21,22,23,24,25,26,27,28,29,30,31}, // GÖRÜŞ
        {32,33,34,35,36}                // EKRAN
    };
    private static final String[] FEAT_NAMES={
        "No Recoil","Silent Aim","Aimbot","Rapid Fire","No Sway",
        "Bullet Speed","No Spread","Infinite Ammo","No Zoom Penalty",
        "Fast Reload","Max HP","Max Armor","Auto Heal","No Knockback","No Fall Dmg",
        "Speed x1.5","Speed x2","Speed x3","Fly Hack","Para Speed","Anti AIM",
        "Vehicle Speed","Vehicle ESP",
        "Wallhack","Enemy ESP","Item ESP","Loot Filter","Cam Lock","Wide FOV","Team HP","Drop Zone","Safe Zone Timer",
        "Crosshair","Radar","Night Mode","No Grass","Bright Mode"
    };
    private static final int[] TAB_COLORS={
        0xFFE53935, // Kırmızı - Savaş
        0xFF43A047, // Yeşil - Savunma
        0xFFFF8F00, // Turuncu - Hareket
        0xFF1E88E5, // Mavi - Görüş
        0xFF8E24AA  // Mor - Ekran
    };

    private final Handler  handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            espView.loadEsp(ESP);
            espView.setFeatures(feat);
            updateFabBadge();
            handler.postDelayed(this,33);
        }
    };

    private TextView fabBadge;

    @Override
    public void onCreate(){
        super.onCreate();
        startForeground(1,buildNotif());
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        addEspView();
        buildMenu();
        addFab();
        handler.post(tick);
    }

    // ── FAB (animasyonlu, badge'li) ──
    private void addFab(){
        FrameLayout fabContainer=new FrameLayout(this);

        // Ana buton
        View btn=new View(this);
        GradientDrawable gd=new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(0xFFE53935);
        gd.setStroke(3,Color.WHITE);
        btn.setBackground(gd);
        FrameLayout.LayoutParams btnLp=new FrameLayout.LayoutParams(68,68);
        fabContainer.addView(btn,btnLp);

        // Aktif özellik sayısı badge
        fabBadge=new TextView(this);
        fabBadge.setTextColor(Color.WHITE);
        fabBadge.setTextSize(10f);
        fabBadge.setTypeface(Typeface.DEFAULT_BOLD);
        fabBadge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg=new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(0xFF222222);
        badgeBg.setStroke(2,Color.WHITE);
        fabBadge.setBackground(badgeBg);
        FrameLayout.LayoutParams badgeLp=new FrameLayout.LayoutParams(26,26);
        badgeLp.gravity=Gravity.TOP|Gravity.END;
        fabContainer.addView(fabBadge,badgeLp);

        fab=fabContainer;
        WindowManager.LayoutParams lp=mkParams(72,72);
        lp.gravity=Gravity.TOP|Gravity.START;
        lp.x=20;lp.y=350;
        fabContainer.setOnTouchListener(new DragTap(lp,fabContainer,wm,this::toggleMenu));
        wm.addView(fabContainer,lp);
    }

    private void updateFabBadge(){
        int count=0;
        for(boolean f:feat)if(f)count++;
        fabBadge.setText(String.valueOf(count));
        fabBadge.setVisibility(count>0?View.VISIBLE:View.GONE);
    }

    // ── MENÜ ──
    private void buildMenu(){
        // Dış container
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Arka plan
        GradientDrawable rootBg=new GradientDrawable();
        rootBg.setColor(0xF0111111);
        rootBg.setCornerRadius(20f);
        rootBg.setStroke(1,0x44FFFFFF);
        root.setBackground(rootBg);
        root.setClipToOutline(true);
        root.setOutlineProvider(new ViewOutlineProvider(){
            @Override public void getOutline(View v,android.graphics.Outline o){
                o.setRoundRect(0,0,v.getWidth(),v.getHeight(),20f);
            }
        });

        // Header
        root.addView(buildHeader());

        // Tab bar
        root.addView(buildTabBar());

        // Divider
        View div=new View(this);
        div.setBackgroundColor(0x22FFFFFF);
        root.addView(div,new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,1));

        // İçerik alanı
        contentArea=new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(0,4,0,12);

        ScrollView scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(contentArea);

        int maxH=(int)(getResources().getDisplayMetrics().heightPixels*0.55f);
        root.addView(scroll,new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,maxH));

        menuRoot=root;

        WindowManager.LayoutParams lp=mkParams(340,
            WindowManager.LayoutParams.WRAP_CONTENT);
        lp.gravity=Gravity.TOP|Gravity.START;
        lp.x=100;lp.y=330;
        menuRoot.setVisibility(View.GONE);
        wm.addView(menuRoot,lp);

        showTab(0);
    }

    private View buildHeader(){
        LinearLayout h=new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setPadding(16,14,16,10);
        h.setGravity(Gravity.CENTER_VERTICAL);

        // Kırmızı nokta
        View dot=new View(this);
        GradientDrawable dotBg=new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(0xFFE53935);
        dot.setBackground(dotBg);
        h.addView(dot,new LinearLayout.LayoutParams(10,10));

        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(8,0);
        h.addView(new View(this),gap);

        TextView title=new TextView(this);
        title.setText("FF HACK");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.15f);
        h.addView(title,new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT,1f));

        // Versiyon
        TextView ver=new TextView(this);
        ver.setText("v3.0");
        ver.setTextColor(0x66FFFFFF);
        ver.setTextSize(11f);
        h.addView(ver);

        return h;
    }

    private LinearLayout tabBar;
    private View[] tabViews;

    private View buildTabBar(){
        tabBar=new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(0xFF0A0A0A);
        tabViews=new View[TAB_NAMES.length];

        for(int i=0;i<TAB_NAMES.length;i++){
            final int idx=i;
            LinearLayout tab=new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0,10,0,8);

            TextView icon=new TextView(this);
            icon.setText(TAB_NAMES[i]);
            icon.setTextSize(16f);
            icon.setGravity(Gravity.CENTER);

            TextView label=new TextView(this);
            label.setText(TAB_LABELS[i]);
            label.setTextColor(0x88FFFFFF);
            label.setTextSize(9f);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(Typeface.DEFAULT_BOLD);

            tab.addView(icon);
            tab.addView(label);
            tab.setOnClickListener(v->showTab(idx));
            tabViews[i]=tab;

            tabBar.addView(tab,new LinearLayout.LayoutParams(
                0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        }
        return tabBar;
    }

    private void showTab(int idx){
        activeTab=idx;
        // Tab renklerini güncelle
        for(int i=0;i<tabViews.length;i++){
            LinearLayout tab=(LinearLayout)tabViews[i];
            if(i==idx){
                // Aktif tab alt çizgi + renkli arka plan
                GradientDrawable activeBg=new GradientDrawable();
                activeBg.setColor(0x22FFFFFF);
                tab.setBackground(activeBg);
                ((TextView)tab.getChildAt(0)).setTextColor(Color.WHITE);
                ((TextView)tab.getChildAt(1)).setTextColor(Color.argb(180,
                    Color.red(TAB_COLORS[i]),
                    Color.green(TAB_COLORS[i]),
                    Color.blue(TAB_COLORS[i])));
            } else {
                tab.setBackground(null);
                ((TextView)tab.getChildAt(0)).setTextColor(0x66FFFFFF);
                ((TextView)tab.getChildAt(1)).setTextColor(0x44FFFFFF);
            }
        }
        // İçeriği yenile
        contentArea.removeAllViews();
        int color=TAB_COLORS[idx];
        for(int fi:TAB_FEATS[idx]){
            contentArea.addView(buildRow(fi,color));
        }
    }

    private View buildRow(int idx,int accentColor){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16,0,16,0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(52);

        // Aktifse arka plan tonu
        if(feat[idx]){
            GradientDrawable rowBg=new GradientDrawable();
            int alpha=Color.argb(30,Color.red(accentColor),
                Color.green(accentColor),Color.blue(accentColor));
            rowBg.setColor(alpha);
            row.setBackground(rowBg);
        }

        // Sol aksent çizgi (aktifse)
        View accent=new View(this);
        accent.setBackgroundColor(feat[idx]?accentColor:0x00000000);
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(3,40);
        alp.rightMargin=12;
        row.addView(accent,alp);

        // Özellik adı
        TextView name=new TextView(this);
        name.setText(FEAT_NAMES[idx]);
        name.setTextColor(feat[idx]?Color.WHITE:0xAAFFFFFF);
        name.setTextSize(14f);
        if(feat[idx])name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT,1f));

        // Custom toggle
        ToggleButton toggle=new ToggleButton(this);
        toggle.setTextOn("");
        toggle.setTextOff("");
        toggle.setChecked(feat[idx]);

        // Toggle arka plan
        GradientDrawable tBg=new GradientDrawable();
        tBg.setShape(GradientDrawable.RECTANGLE);
        tBg.setCornerRadius(20f);
        tBg.setColor(feat[idx]?accentColor:0x44FFFFFF);
        toggle.setBackground(tBg);
        toggle.setWidth(50);
        toggle.setHeight(26);

        toggle.setOnCheckedChangeListener((v,checked)->{
            feat[idx]=checked;
            // Mutex: Speed hack sadece biri aktif
            if(checked&&(idx==15||idx==16||idx==17)){
                if(idx!=15)feat[15]=false;
                if(idx!=16)feat[16]=false;
                if(idx!=17)feat[17]=false;
                feat[idx]=true;
            }
            // Aimbot / CamLock mutex
            if(checked&&idx==2)feat[27]=false;
            if(checked&&idx==27)feat[2]=false;
            // Night / Bright mutex
            if(checked&&idx==34)feat[36]=false;
            if(checked&&idx==36)feat[34]=false;
            writeConfig();
            showTab(activeTab); // refresh
        });

        row.addView(name);
        row.addView(toggle);
        return row;
    }

    private void toggleMenu(){
        menuOpen=!menuOpen;
        if(menuOpen){
            menuRoot.setVisibility(View.VISIBLE);
            menuRoot.startAnimation(AnimationUtils.loadAnimation(this,android.R.anim.fade_in));
        } else {
            Animation out=AnimationUtils.loadAnimation(this,android.R.anim.fade_out);
            out.setAnimationListener(new Animation.AnimationListener(){
                public void onAnimationStart(Animation a){}
                public void onAnimationRepeat(Animation a){}
                public void onAnimationEnd(Animation a){menuRoot.setVisibility(View.GONE);}
            });
            menuRoot.startAnimation(out);
        }
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

    private void writeConfig(){
        try(java.io.FileOutputStream f=new java.io.FileOutputStream(CFG)){
            byte[] buf=new byte[feat.length];
            for(int i=0;i<feat.length;i++)buf[i]=feat[i]?(byte)1:0;
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
        if(fab!=null)wm.removeView(fab);
        if(menuRoot!=null)wm.removeView(menuRoot);
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
