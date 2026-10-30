package com.ff.menu;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

public class EspView extends View {

    private static final int ENEMY_BYTES  =128;
    private static final int ITEM_BYTES   =32;
    private static final int VEH_BYTES    =20;
    private static final int TEAM_BYTES   =28;

    // ── Paints ──
    private final Paint pBoxStroke =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBoxFill   =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSkel      =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBar       =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBarBg     =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText      =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSnap      =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLocal     =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCross     =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRadar     =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pItem      =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pVeh       =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pOverlay   =new Paint();
    private final Paint pTeam      =new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSZ        =new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Data ──
    private EspData[]  enemies =new EspData[0];
    private ItemData[] items   =new ItemData[0];
    private VehData[]  vehicles=new VehData[0];
    private TeamData[] teams   =new TeamData[0];
    private float localHp=0f,localArmor=0f;
    private SZData szData=null;
    private boolean[] feat=new boolean[37];

    static class EspData{
        float hx,hy,brx,bry,hipx,hipy;
        float lshx,lshy,rshx,rshy;
        float lhax,lhay,rhax,rhay;
        float lax,lay,rax,ray;
        float boxTop,boxBot,boxCx,boxW;
        float hp,armor,dist; String name; boolean isBot;
    }
    static class ItemData{ float sx,sy,dist; int type; }
    static class VehData { float sx,sy,dist,hp; }
    static class TeamData{ float hp,armor; String name; }
    static class SZData  { float cx,cz,radius,timer; }

    public EspView(Context ctx){
        super(ctx);
        setLayerType(LAYER_TYPE_HARDWARE,null);
        initPaints();
    }

    private void initPaints(){
        pBoxStroke.setStyle(Paint.Style.STROKE);
        pBoxStroke.setStrokeWidth(2f);

        pBoxFill.setStyle(Paint.Style.FILL);

        pSkel.setStyle(Paint.Style.STROKE);
        pSkel.setStrokeWidth(1.6f);
        pSkel.setStrokeCap(Paint.Cap.ROUND);
        pSkel.setStrokeJoin(Paint.Join.ROUND);

        pBar.setStyle(Paint.Style.FILL);
        pBarBg.setStyle(Paint.Style.FILL);
        pBarBg.setColor(Color.argb(120,0,0,0));

        pText.setAntiAlias(true);
        pText.setTextAlign(Paint.Align.CENTER);

        pSnap.setStyle(Paint.Style.STROKE);
        pSnap.setStrokeWidth(1f);
        pSnap.setPathEffect(new DashPathEffect(new float[]{8f,6f},0));

        pLocal.setStyle(Paint.Style.FILL);

        pCross.setStyle(Paint.Style.STROKE);
        pCross.setStrokeCap(Paint.Cap.ROUND);

        pRadar.setAntiAlias(true);
        pItem.setAntiAlias(true);
        pItem.setTextAlign(Paint.Align.CENTER);
        pVeh.setAntiAlias(true);
        pVeh.setTextAlign(Paint.Align.CENTER);

        pTeam.setAntiAlias(true);
        pSZ.setAntiAlias(true);
        pOverlay.setStyle(Paint.Style.FILL);
    }

    public void setFeatures(boolean[] f){ this.feat=f; }

    // HP oranına göre renk (kırmızı → sarı → yeşil)
    private int hpColor(float hp,float maxHp){
        float r=Math.max(0f,Math.min(1f,hp/maxHp));
        if(r>0.6f) return Color.argb(230,30+Math.round((1-r)*180),200,30);
        if(r>0.3f) return Color.argb(230,220,Math.round(r*400),20);
        return Color.argb(230,220,40,30);
    }

    public void loadEsp(String path){
        try(FileInputStream fis=new FileInputStream(path);
            FileChannel fc=fis.getChannel()){
            long size=fc.size();
            if(size<32){reset();return;}
            ByteBuffer bb=ByteBuffer.allocate((int)size);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            fc.read(bb); bb.flip();

            int en=bb.getInt(),in=bb.getInt(),vn=bb.getInt(),tn=bb.getInt();
            localHp   =bb.getFloat();
            localArmor=bb.getFloat();

            // Safe zone
            SZData sz=new SZData();
            sz.cx=bb.getFloat();sz.cz=bb.getFloat();
            sz.radius=bb.getFloat();sz.timer=bb.getFloat();
            szData=(sz.radius>0)?sz:null;

            // Enemies
            EspData[] ea=new EspData[Math.min(en,100)];
            for(int i=0;i<ea.length;i++){
                EspData e=new EspData();
                e.hx=bb.getFloat();e.hy=bb.getFloat();
                e.brx=bb.getFloat();e.bry=bb.getFloat();
                e.hipx=bb.getFloat();e.hipy=bb.getFloat();
                e.lshx=bb.getFloat();e.lshy=bb.getFloat();
                e.rshx=bb.getFloat();e.rshy=bb.getFloat();
                e.lhax=bb.getFloat();e.lhay=bb.getFloat();
                e.rhax=bb.getFloat();e.rhay=bb.getFloat();
                e.lax=bb.getFloat();e.lay=bb.getFloat();
                e.rax=bb.getFloat();e.ray=bb.getFloat();
                e.boxTop=bb.getFloat();e.boxBot=bb.getFloat();
                e.boxCx=bb.getFloat();e.boxW=bb.getFloat();
                e.hp=bb.getFloat();e.armor=bb.getFloat();e.dist=bb.getFloat();
                byte[] nb=new byte[24];bb.get(nb);
                e.name=new String(nb).replace("\0","").trim();
                e.isBot=bb.get()!=0;bb.get(new byte[3]);
                ea[i]=e;
            }
            enemies=ea;

            // Items
            ItemData[] ia=new ItemData[Math.min(in,200)];
            for(int i=0;i<ia.length;i++){
                ItemData it=new ItemData();
                it.sx=bb.getFloat();it.sy=bb.getFloat();
                it.dist=bb.getFloat();it.type=bb.get()&0xFF;
                bb.get(new byte[20]);bb.get(new byte[3]);ia[i]=it;
            }
            items=ia;

            // Vehicles
            VehData[] va=new VehData[Math.min(vn,20)];
            for(int i=0;i<va.length;i++){
                VehData v=new VehData();
                v.sx=bb.getFloat();v.sy=bb.getFloat();
                v.dist=bb.getFloat();v.hp=bb.getFloat();
                bb.get(new byte[4]);va[i]=v;
            }
            vehicles=va;

            // Teams
            TeamData[] ta=new TeamData[Math.min(tn,4)];
            for(int i=0;i<ta.length;i++){
                TeamData t=new TeamData();
                t.hp=bb.getFloat();t.armor=bb.getFloat();
                byte[] nb=new byte[20];bb.get(nb);
                t.name=new String(nb).replace("\0","").trim();
                ta[i]=t;
            }
            teams=ta;

        }catch(Exception ex){reset();}
        invalidate();
    }

    private void reset(){enemies=new EspData[0];items=new ItemData[0];vehicles=new VehData[0];teams=new TeamData[0];}

    @Override
    protected void onDraw(Canvas c){
        float sw=getWidth(),sh=getHeight();

        // Ortam overlay
        if(feat[34]){pOverlay.setColor(Color.argb(100,0,8,30));c.drawRect(0,0,sw,sh,pOverlay);}
        if(feat[36]){pOverlay.setColor(Color.argb(30,255,255,200));c.drawRect(0,0,sw,sh,pOverlay);}

        // ESP
        if(feat[24])for(EspData e:enemies)if(e!=null)drawEnemy(c,e,sw,sh);
        if(feat[25]||feat[26])for(ItemData it:items)if(it!=null)drawItem(c,it);
        if(feat[22])for(VehData v:vehicles)if(v!=null)drawVehicle(c,v);
        if(feat[29])drawTeamHP(c,sw,sh);
        if(feat[31]&&szData!=null)drawSafeZoneTimer(c,sw);
        if(feat[33])drawRadar(c,sw,sh);
        if(feat[32])drawCrosshair(c,sw,sh);
        drawLocalBar(c,sw,sh);
    }

    // ── ENEMY ESP ──
    private void drawEnemy(Canvas c,EspData e,float sw,float sh){
        float bT=e.boxTop,bB=e.boxBot;
        float bL=e.boxCx-e.boxW*0.5f,bR=e.boxCx+e.boxW*0.5f;
        float bH=bB-bT;
        int col=hpColor(e.hp,200f);
        float hpR=Math.max(0f,Math.min(1f,e.hp/200f));
        float arR=Math.max(0f,Math.min(1f,e.armor/200f));

        // Snapline (dashed)
        pSnap.setColor(Color.argb(60,col>>16&0xFF,col>>8&0xFF,col&0xFF));
        c.drawLine(sw*0.5f,sh,e.boxCx,bB,pSnap);

        // Kutu dolgu (çok şeffaf)
        pBoxFill.setColor(Color.argb(15,col>>16&0xFF,col>>8&0xFF,col&0xFF));
        c.drawRoundRect(new RectF(bL,bT,bR,bB),4f,4f,pBoxFill);

        // Köşe bracket (L şekli)
        pBoxStroke.setColor(col);
        pBoxStroke.setStrokeWidth(2.2f);
        float cl=Math.min(bH*0.25f,20f);
        // Sol üst
        c.drawLine(bL,bT+cl,bL,bT,pBoxStroke);c.drawLine(bL,bT,bL+cl,bT,pBoxStroke);
        // Sağ üst
        c.drawLine(bR-cl,bT,bR,bT,pBoxStroke);c.drawLine(bR,bT,bR,bT+cl,pBoxStroke);
        // Sol alt
        c.drawLine(bL,bB-cl,bL,bB,pBoxStroke);c.drawLine(bL,bB,bL+cl,bB,pBoxStroke);
        // Sağ alt
        c.drawLine(bR-cl,bB,bR,bB,pBoxStroke);c.drawLine(bR,bB,bR,bB-cl,pBoxStroke);

        // HP bar (sol, yuvarlak köşe)
        float bx=bL-9f,bw=5f;
        c.drawRoundRect(new RectF(bx,bT,bx+bw,bB),2f,2f,pBarBg);
        pBar.setColor(col);
        c.drawRoundRect(new RectF(bx,bB-bH*hpR,bx+bw,bB),2f,2f,pBar);

        // Armor bar (daha solda, mavi)
        if(e.armor>0f){
            float ax=bx-7f;
            c.drawRoundRect(new RectF(ax,bT,ax+bw,bB),2f,2f,pBarBg);
            pBar.setColor(Color.argb(200,60,140,255));
            c.drawRoundRect(new RectF(ax,bB-bH*arR,ax+bw,bB),2f,2f,pBar);
        }

        // İskelet — renk koyulaştırılmış beyaz
        pSkel.setColor(Color.argb(160,220,220,220));
        pSkel.setStrokeWidth(1.5f);
        sk(c,e.hx,e.hy,e.brx,e.bry);
        sk(c,e.brx,e.bry,e.hipx,e.hipy);
        sk(c,e.hx,e.hy,e.lshx,e.lshy);
        sk(c,e.lshx,e.lshy,e.lhax,e.lhay);
        sk(c,e.hx,e.hy,e.rshx,e.rshy);
        sk(c,e.rshx,e.rshy,e.rhax,e.rhay);
        sk(c,e.hipx,e.hipy,e.lax,e.lay);
        sk(c,e.hipx,e.hipy,e.rax,e.ray);

        // Eklemler
        drawJoint(c,e.hx,e.hy,5f,col,true);      // baş - büyük + renkli
        drawJoint(c,e.brx,e.bry,3f,Color.argb(180,200,200,200),false);
        drawJoint(c,e.hipx,e.hipy,3f,Color.argb(180,200,200,200),false);
        drawJoint(c,e.lshx,e.lshy,2.5f,Color.argb(160,180,180,180),false);
        drawJoint(c,e.rshx,e.rshy,2.5f,Color.argb(160,180,180,180),false);
        drawJoint(c,e.lhax,e.lhay,2f,Color.argb(140,160,160,160),false);
        drawJoint(c,e.rhax,e.rhay,2f,Color.argb(140,160,160,160),false);
        drawJoint(c,e.lax,e.lay,2f,Color.argb(140,160,160,160),false);
        drawJoint(c,e.rax,e.ray,2f,Color.argb(140,160,160,160),false);

        // İsim (kutu üstünde, küçük dark bg)
        String label=e.isBot?"🤖 "+e.name:e.name;
        pText.setTextSize(22f);
        pText.setColor(Color.WHITE);
        pText.setShadowLayer(5f,0,1f,Color.BLACK);
        c.drawText(label,e.boxCx,bT-26f,pText);

        // Mesafe (daha küçük, sarımsı)
        pText.setTextSize(18f);
        pText.setColor(Color.argb(200,255,210,60));
        pText.setShadowLayer(4f,0,1f,Color.BLACK);
        c.drawText(String.format("%.0fm",e.dist),e.boxCx,bT-8f,pText);

        // HP sayısı (sol bar yanı)
        pText.setTextSize(17f);
        pText.setColor(col);
        pText.setTextAlign(Paint.Align.RIGHT);
        c.drawText((int)e.hp+"",bL-12f,bB+4f,pText);
        pText.setTextAlign(Paint.Align.CENTER);
    }

    // ── ITEM ESP ──
    private static final int[] ITEM_CLR={
        Color.argb(220,255,210,30),  // silah - altın
        Color.argb(180,180,180,180), // mermi - gri
        Color.argb(220,40,200,80),   // heal - yeşil
        Color.argb(220,60,140,255),  // armor - mavi
        Color.argb(160,200,200,200)  // diğer
    };
    private static final String[] ITEM_LBL={"GUN","AMO","MED","ARM","BOX"};

    private void drawItem(Canvas c,ItemData it){
        int t=Math.min(it.type,4);
        int col=ITEM_CLR[t];

        // Küçük renkli badge
        float r=14f;
        pItem.setStyle(Paint.Style.FILL);
        pItem.setColor(Color.argb(160,0,0,0));
        c.drawRoundRect(new RectF(it.sx-r,it.sy-10f,it.sx+r,it.sy+10f),6f,6f,pItem);
        pItem.setColor(col);
        pItem.setStyle(Paint.Style.STROKE);
        pItem.setStrokeWidth(1.5f);
        c.drawRoundRect(new RectF(it.sx-r,it.sy-10f,it.sx+r,it.sy+10f),6f,6f,pItem);

        pItem.setStyle(Paint.Style.FILL);
        pItem.setTextSize(16f);
        c.drawText(ITEM_LBL[t],it.sx,it.sy+5f,pItem);

        pItem.setColor(Color.argb(160,255,255,255));
        pItem.setTextSize(14f);
        c.drawText(String.format("%.0f",it.dist)+"m",it.sx,it.sy+22f,pItem);
    }

    // ── VEHICLE ESP ──
    private void drawVehicle(Canvas c,VehData v){
        int col=Color.argb(220,255,180,30);
        pVeh.setStyle(Paint.Style.FILL);
        pVeh.setColor(Color.argb(150,0,0,0));
        c.drawRoundRect(new RectF(v.sx-32f,v.sy-12f,v.sx+32f,v.sy+12f),8f,8f,pVeh);
        pVeh.setColor(col);
        pVeh.setStyle(Paint.Style.STROKE);
        pVeh.setStrokeWidth(1.5f);
        c.drawRoundRect(new RectF(v.sx-32f,v.sy-12f,v.sx+32f,v.sy+12f),8f,8f,pVeh);
        pVeh.setStyle(Paint.Style.FILL);
        pVeh.setTextSize(18f);
        c.drawText("🚗 "+String.format("%.0fm",v.dist),v.sx,v.sy+6f,pVeh);

        // HP bar
        float bW=64f,bH=4f,bX=v.sx-32f,bY=v.sy+16f;
        float hR=Math.max(0f,Math.min(1f,v.hp/1000f));
        pBarBg.setColor(Color.argb(100,0,0,0));
        c.drawRoundRect(new RectF(bX,bY,bX+bW,bY+bH),2f,2f,pBarBg);
        pBar.setColor(col);
        c.drawRoundRect(new RectF(bX,bY,bX+bW*hR,bY+bH),2f,2f,pBar);
    }

    // ── TEAM HP ──
    private void drawTeamHP(Canvas c,float sw,float sh){
        if(teams==null||teams.length==0)return;
        float startY=sh*0.3f;
        float x=sw-14f;
        for(int i=0;i<teams.length;i++){
            TeamData t=teams[i];
            if(t==null)continue;
            float y=startY+i*70f;
            float hR=Math.max(0f,Math.min(1f,t.hp/200f));
            float aR=Math.max(0f,Math.min(1f,t.armor/200f));
            int col=hpColor(t.hp,200f);

            // İsim
            pText.setTextSize(18f);
            pText.setColor(Color.argb(200,255,255,255));
            pText.setShadowLayer(3f,0,1f,Color.BLACK);
            pText.setTextAlign(Paint.Align.RIGHT);
            c.drawText(t.name.isEmpty()?"Takım "+(i+1):t.name,x,y,pText);

            // HP bar
            float bH2=6f,bW=80f,bX=x-bW;
            pBarBg.setColor(Color.argb(100,0,0,0));
            c.drawRoundRect(new RectF(bX,y+4f,bX+bW,y+4f+bH2),2f,2f,pBarBg);
            pBar.setColor(col);
            c.drawRoundRect(new RectF(bX,y+4f,bX+bW*hR,y+4f+bH2),2f,2f,pBar);

            // Armor bar
            if(t.armor>0f){
                pBarBg.setColor(Color.argb(80,0,0,0));
                c.drawRoundRect(new RectF(bX,y+13f,bX+bW,y+13f+4f),2f,2f,pBarBg);
                pBar.setColor(Color.argb(200,60,140,255));
                c.drawRoundRect(new RectF(bX,y+13f,bX+bW*aR,y+13f+4f),2f,2f,pBar);
            }

            // HP sayısı
            pText.setTextSize(16f);
            pText.setColor(col);
            c.drawText((int)t.hp+"HP",x,y+26f,pText);
            pText.setTextAlign(Paint.Align.CENTER);
        }
    }

    // ── SAFE ZONE TIMER ──
    private void drawSafeZoneTimer(Canvas c,float sw){
        if(szData==null)return;
        float timer=szData.timer;
        String timerStr=String.format("%d:%02d",(int)timer/60,(int)timer%60);

        // Üst orta
        float cx=sw*0.5f, cy=80f;

        pSZ.setStyle(Paint.Style.FILL);
        pSZ.setColor(Color.argb(160,0,0,0));
        c.drawRoundRect(new RectF(cx-70f,cy-22f,cx+70f,cy+10f),8f,8f,pSZ);

        pSZ.setStyle(Paint.Style.STROKE);
        pSZ.setStrokeWidth(1.5f);
        pSZ.setColor(Color.argb(200,40,160,255));
        c.drawRoundRect(new RectF(cx-70f,cy-22f,cx+70f,cy+10f),8f,8f,pSZ);

        pText.setTextSize(13f);
        pText.setColor(Color.argb(180,180,220,255));
        pText.setShadowLayer(0f,0f,0f,0);
        c.drawText("GÜVENLI BÖLGE",cx,cy-9f,pText);

        pText.setTextSize(20f);
        pText.setColor(timer<30?Color.argb(255,255,80,50):Color.WHITE);
        pText.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(timerStr,cx,cy+7f,pText);
        pText.setTypeface(null);
    }

    // ── RADAR ──
    private void drawRadar(Canvas c,float sw,float sh){
        float cx=140f,cy=sh-140f,r=110f;

        // Arka plan
        pRadar.setStyle(Paint.Style.FILL);
        pRadar.setColor(Color.argb(170,5,5,15));
        c.drawCircle(cx,cy,r,pRadar);

        // Çerçeve
        pRadar.setStyle(Paint.Style.STROKE);
        pRadar.setStrokeWidth(1.5f);
        pRadar.setColor(Color.argb(150,40,160,255));
        c.drawCircle(cx,cy,r,pRadar);

        // İç halkalar
        pRadar.setColor(Color.argb(30,255,255,255));
        pRadar.setStrokeWidth(0.8f);
        c.drawCircle(cx,cy,r*0.5f,pRadar);
        c.drawCircle(cx,cy,r*0.25f,pRadar);

        // Çapraz çizgiler
        c.drawLine(cx-r,cy,cx+r,cy,pRadar);
        c.drawLine(cx,cy-r,cx,cy+r,pRadar);

        // Local nokta
        pRadar.setStyle(Paint.Style.FILL);
        pRadar.setColor(Color.argb(255,40,220,80));
        c.drawCircle(cx,cy,5f,pRadar);
        // Ok (yön)
        pRadar.setColor(Color.argb(200,40,220,80));
        c.drawLine(cx,cy,cx,cy-10f,pRadar);

        // Düşman noktaları
        for(EspData e:enemies){
            if(e==null)continue;
            float rx=((e.hx/getWidth())-0.5f)*2f*r*0.9f;
            float ry=((e.hy/getHeight())-0.5f)*2f*r*0.9f;
            float ex=cx+rx,ey=cy+ry;
            float dd=(float)Math.sqrt((ex-cx)*(ex-cx)+(ey-cy)*(ey-cy));
            if(dd>r-5f){ex=cx+(ex-cx)/dd*(r-5f);ey=cy+(ey-cy)/dd*(r-5f);}
            int col=hpColor(e.hp,200f);
            pRadar.setColor(col);
            c.drawCircle(ex,ey,5f,pRadar);
            // HP mini bar
            pRadar.setColor(Color.argb(100,0,0,0));
            c.drawRect(ex-5f,ey+7f,ex+5f,ey+9f,pRadar);
            pRadar.setColor(col);
            float hR=Math.max(0f,Math.min(1f,e.hp/200f));
            c.drawRect(ex-5f,ey+7f,ex-5f+10f*hR,ey+9f,pRadar);
        }

        // Item noktaları
        for(ItemData it:items){
            if(it==null)continue;
            float rx=((it.sx/getWidth())-0.5f)*2f*r*0.9f;
            float ry=((it.sy/getHeight())-0.5f)*2f*r*0.9f;
            float ex=cx+rx,ey=cy+ry;
            float dd=(float)Math.sqrt((ex-cx)*(ex-cx)+(ey-cy)*(ey-cy));
            if(dd>r-5f){ex=cx+(ex-cx)/dd*(r-5f);ey=cy+(ey-cy)/dd*(r-5f);}
            pRadar.setColor(Color.argb(180,255,200,30));
            c.drawRect(ex-2f,ey-2f,ex+2f,ey+2f,pRadar);
        }

        // Radar etiketi
        Paint lp=new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setColor(Color.argb(120,180,220,255));
        lp.setTextSize(14f);
        lp.setTextAlign(Paint.Align.CENTER);
        c.drawText("RADAR",cx,cy-r-6f,lp);
        lp.setTextSize(12f);
        lp.setColor(Color.argb(80,255,255,255));
        c.drawText(enemies.length+" düşman",cx,cy+r+14f,lp);
    }

    // ── CROSSHAIR ──
    private void drawCrosshair(Canvas c,float sw,float sh){
        float cx=sw*0.5f,cy=sh*0.5f;
        float gap=14f,arm=22f,dot=3.5f,thick=2.5f;

        // Gölge
        pCross.setColor(Color.argb(100,0,0,0));
        pCross.setStrokeWidth(thick+2f);
        c.drawLine(cx-gap-arm,cy,cx-gap,cy,pCross);
        c.drawLine(cx+gap,cy,cx+gap+arm,cy,pCross);
        c.drawLine(cx,cy-gap-arm,cx,cy-gap,pCross);
        c.drawLine(cx,cy+gap,cx,cy+gap+arm,pCross);

        // Çizgiler
        pCross.setColor(Color.argb(230,0,230,80));
        pCross.setStrokeWidth(thick);
        c.drawLine(cx-gap-arm,cy,cx-gap,cy,pCross);
        c.drawLine(cx+gap,cy,cx+gap+a20,255,220,80));
        c.drawText(String.format("%.0fm",e.dist),e.boxCx,bT-8f,pDist);
        pDist.setColor(col);
        c.drawText((int)e.hp+"HP",bL-14f,bB,pDist);
    }

    // ── Item ESP çizimi
    private static final String[] ITEM_ICONS = {"🔫","🔫","💊","🛡️","📦"};
    private static final int[]    ITEM_COLORS= {
        Color.argb(220,255,220,50),   // gun - sarı
        Color.argb(200,200,200,200),  // ammo - gri
        Color.argb(220,50,255,80),    // heal - yeşil
        Color.argb(220,80,160,255),   // armor - mavi
        Color.argb(180,200,200,200)   // other - gri
    };

    private void drawItem(Canvas c,ItemData it){
        int t=Math.min(it.type,4);
        pItem.setColor(ITEM_COLORS[t]);
        // Küçük kare
        pBg.setAlpha(140);
        c.drawRoundRect(new RectF(it.sx-24,it.sy-12,it.sx+24,it.sy+12),6f,6f,pBg);
        c.drawText(ITEM_ICONS[t],it.sx,it.sy+8f,pItem);
        // Mesafe
        pItem.setTextSize(18f);
        pItem.setColor(Color.argb(180,255,255,255));
        c.drawText(String.format("%.0fm",it.dist),it.sx,it.sy+28f,pItem);
        pItem.setTextSize(22f);
    }

    // ── Vehicle ESP çizimi
    private void drawVehicle(Canvas c,VehData v){
        pBg.setAlpha(160);
        c.drawRoundRect(new RectF(v.sx-30,v.sy-14,v.sx+30,v.sy+14),8f,8f,pBg);
        pVeh.setColor(Color.argb(220,255,200,50));
        c.drawText("🚗 "+String.format("%.0fm",v.dist),v.sx,v.sy+8f,pVeh);
        // HP bar
        float bW=60f,bH=5f,bX=v.sx-30f,bY=v.sy+18f;
        float hpR=Math.max(0f,Math.min(1f,v.hp/1000f));
        pBg.setAlpha(120);
        c.drawRect(bX,bY,bX+bW,bY+bH,pBg);
        pHp.setColor(Color.argb(200,255,200,50));
        c.drawRect(bX,bY,bX+bW*hpR,bY+bH,pHp);
    }

    // ── Radar (sol alt köşe)
    private void drawRadar(Canvas c,float sw,float sh){
        float cx=160f, cy=sh-160f, r=120f;
        // Arka plan daire
        pRadar.setStyle(Paint.Style.FILL);
        pRadar.setColor(Color.argb(160,0,0,0));
        c.drawCircle(cx,cy,r,pRadar);
        // Çerçeve
        pRadar.setStyle(Paint.Style.STROKE);
        pRadar.setColor(Color.argb(180,200,30,30));
        pRadar.setStrokeWidth(2f);
        c.drawCircle(cx,cy,r,pRadar);
        // Çapraz çizgiler
        pRadar.setColor(Color.argb(60,255,255,255));
        pRadar.setStrokeWidth(1f);
        c.drawLine(cx-r,cy,cx+r,cy,pRadar);
        c.drawLine(cx,cy-r,cx,cy+r,pRadar);
        // Local player nokta (merkez, yeşil)
        pRadar.setStyle(Paint.Style.FILL);
        pRadar.setColor(Color.argb(255,50,255,80));
        c.drawCircle(cx,cy,5f,pRadar);
        // Düşman noktaları
        for(EspData e:enemies){
            if(e==null)continue;
            // Ekran koordinatlarını radar koordinatına çevir
            float rx=((e.hx/getWidth())-0.5f)*2f*r;
            float ry=((e.hy/getHeight())-0.5f)*2f*r;
            float ex=cx+rx, ey=cy+ry;
            // Radar sınırı içinde mi?
            float dd=(float)Math.sqrt((ex-cx)*(ex-cx)+(ey-cy)*(ey-cy));
            if(dd>r-4f){ex=cx+(ex-cx)/dd*(r-4f);ey=cy+(ey-cy)/dd*(r-4f);}
            pRadar.setColor(hpColor(e.hp));
            c.drawCircle(ex,ey,5f,pRadar);
        }
        // Item noktaları (sarı, küçük)
        pRadar.setColor(Color.argb(200,255,220,50));
        for(ItemData it:items){
            if(it==null)continue;
            float rx=((it.sx/getWidth())-0.5f)*2f*r;
            float ry=((it.sy/getHeight())-0.5f)*2f*r;
            float ex=cx+rx,ey=cy+ry;
            float dd=(float)Math.sqrt((ex-cx)*(ex-cx)+(ey-cy)*(ey-cy));
            if(dd>r-4f){ex=cx+(ex-cx)/dd*(r-4f);ey=cy+(ey-cy)/dd*(r-4f);}
            c.drawCircle(ex,ey,3f,pRadar);
        }
        // Mesafe halkası etiketi
        pRadar.setColor(Color.argb(100,255,255,255));
        Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.argb(120,255,255,255));
        tp.setTextSize(16f);
        tp.setTextAlign(Paint.Align.CENTER);
        c.drawText("RADAR",cx,cy-r-6f,tp);
    }

    // ── Crosshair
    private void drawCrosshair(Canvas c,float sw,float sh){
        float cx=sw/2f, cy=sh/2f;
        float gap=12f, len=24f, dot=3f;
        // Gölge
        pCross.setColor(Color.argb(120,0,0,0));
        pCross.setStrokeWidth(4f);
        c.drawLine(cx-gap-len,cy,cx-gap,cy,pCross);
        c.drawLine(cx+gap,cy,cx+gap+len,cy,pCross);
        c.drawLine(cx,cy-gap-len,cx,cy-gap,pCross);
        c.drawLine(cx,cy+gap,cx,cy+gap+len,pCross);
        // Ana çizgiler
        pCross.setColor(Color.argb(220,0,255,80));
        pCross.setStrokeWidth(2f);
        c.drawLine(cx-gap-len,cy,cx-gap,cy,pCross);
        c.drawLine(cx+gap,cy,cx+gap+len,cy,pCross);
        c.drawLine(cx,cy-gap-len,cx,cy-gap,pCross);
        c.drawLine(cx,cy+gap,cx,cy+gap+len,pCross);
        // Merkez nokta
        pCross.setStyle(Paint.Style.FILL);
        pCross.setColor(Color.argb(220,0,255,80));
        c.drawCircle(cx,cy,dot,pCross);
        pCross.setStyle(Paint.Style.STROKE);
    }

    // ── Local HP bar
    private void drawLocalBar(Canvas c,float sw,float sh){
        float barW=sw*0.40f,barH=16f;
        float barX=(sw-barW)*0.5f,barY=sh-130f;
        float hpR=Math.max(0f,Math.min(1f,localHp/200f));
        float arR=Math.max(0f,Math.min(1f,localArmor/200f));

        pBg.setAlpha(190);
        c.drawRoundRect(new RectF(barX-3,barY-3,barX+barW+3,barY+barH+3),8f,8f,pBg);
        pLocal.setColor(Color.argb(240,45,200,75));
        c.drawRoundRect(new RectF(barX,barY,barX+barW*hpR,barY+barH),6f,6f,pLocal);

        Paint pt=new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE); pt.setTextSize(22f);
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setShadowLayer(4f,1f,1f,Color.BLACK);
        pt.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText((int)localHp+" / 200",sw*0.5f,barY-7f,pt);

        if(localArmor>0f){
            float aY=barY+barH+6f,aH=8f;
            pBg.setAlpha(160);
            c.drawRoundRect(new RectF(barX-3,aY-2,barX+barW+3,aY+aH+2),5f,5f,pBg);
            pLocal.setColor(Color.argb(210,70,150,255));
            c.drawRoundRect(new RectF(barX,aY,barX+barW*arR,aY+aH),4f,4f,pLocal);
        }
    }

    private void skel(Canvas c,float x1,float y1,float x2,float y2){
        if(x1==0&&y1==0||x2==0&&y2==0)return;
        c.drawLine(x1,y1,x2,y2,pSkel);
    }
    private void joint(Canvas c,float x,float y,float r,int col){
        if(x==0&&y==0)return;
        pSkel.setColor(col); c.drawCircle(x,y,r,pSkel);
        pSkel.setColor(Color.argb(180,255,255,255));
    }
             } return;
        pSkel.setColor(color);
        c.drawCircle(x, y, r, pSkel);
        pSkel.setColor(Color.argb(180, 255, 255, 255));
    }
}
