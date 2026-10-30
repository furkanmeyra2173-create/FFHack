package com.ff.menu;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

public class EspView extends View {

    private static final int ENEMY_BYTES   = 128;
    private static final int ITEM_BYTES    = 32;
    private static final int VEHICLE_BYTES = 20;

    // Paints
    private final Paint pBox   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSkel  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHp    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pAr    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBg    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pName  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDist  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSnap  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLocal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCross = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRadar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pItem  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pVeh   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pOverlay=new Paint();

    // Data
    private EspData[]  enemies  = new EspData[0];
    private ItemData[] items    = new ItemData[0];
    private VehData[]  vehicles = new VehData[0];
    private float localHp=0f, localArmor=0f;
    private boolean[] feat = new boolean[22];

    static class EspData{
        float hx,hy,brx,bry,hipx,hipy;
        float lshx,lshy,rshx,rshy;
        float lhax,lhay,rhax,rhay;
        float lax,lay,rax,ray;
        float boxTop,boxBot,boxCx,boxW;
        float hp,armor,dist; String name; boolean isBot;
    }
    static class ItemData {
        float sx,sy,dist; int type; String name;
    }
    static class VehData {
        float sx,sy,dist,hp;
    }

    public EspView(Context ctx){
        super(ctx);
        setLayerType(LAYER_TYPE_HARDWARE,null);

        pBox.setStyle(Paint.Style.STROKE); pBox.setStrokeWidth(1.8f);
        pFill.setStyle(Paint.Style.FILL);  pFill.setColor(Color.argb(20,255,255,255));
        pSkel.setStyle(Paint.Style.STROKE);pSkel.setStrokeWidth(1.4f);
        pSkel.setColor(Color.argb(200,255,255,255));
        pHp.setStyle(Paint.Style.FILL);
        pAr.setStyle(Paint.Style.FILL); pAr.setColor(Color.argb(200,80,160,255));
        pBg.setStyle(Paint.Style.FILL); pBg.setColor(Color.argb(160,0,0,0));
        pName.setColor(Color.WHITE); pName.setTextSize(24f);
        pName.setTextAlign(Paint.Align.CENTER);
        pName.setShadowLayer(4f,1f,1f,Color.BLACK);
        pDist.setTextSize(20f); pDist.setTextAlign(Paint.Align.CENTER);
        pDist.setShadowLayer(3f,1f,1f,Color.BLACK);
        pSnap.setStyle(Paint.Style.STROKE); pSnap.setStrokeWidth(1f);
        pLocal.setStyle(Paint.Style.FILL); pLocal.setAntiAlias(true);

        // Crosshair
        pCross.setColor(Color.argb(220,0,255,80));
        pCross.setStyle(Paint.Style.STROKE);
        pCross.setStrokeWidth(2f);
        pCross.setAntiAlias(true);

        // Radar
        pRadar.setAntiAlias(true);

        // Item
        pItem.setAntiAlias(true);
        pItem.setTextSize(22f);
        pItem.setTextAlign(Paint.Align.CENTER);
        pItem.setShadowLayer(3f,1f,1f,Color.BLACK);

        // Vehicle
        pVeh.setAntiAlias(true);
        pVeh.setTextSize(22f);
        pVeh.setTextAlign(Paint.Align.CENTER);
        pVeh.setColor(Color.argb(220,255,200,50));
        pVeh.setShadowLayer(3f,1f,1f,Color.BLACK);

        pOverlay.setStyle(Paint.Style.FILL);
    }

    public void setFeatFlags(boolean[] f){ this.feat=f; invalidate(); }

    private int hpColor(float hp){
        float r=hp/200f;
        if(r>0.66f)return Color.argb(220,40,210,40);
        if(r>0.33f)return Color.argb(220,210,190,30);
        return Color.argb(220,210,50,30);
    }

    public void loadEsp(String path){
        try(FileInputStream fis=new FileInputStream(path);
            FileChannel fc=fis.getChannel()){
            long size=fc.size();
            if(size<20){enemies=new EspData[0];invalidate();return;}
            ByteBuffer bb=ByteBuffer.allocate((int)size);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            fc.read(bb); bb.flip();

            int en=bb.getInt(), in=bb.getInt(), vn=bb.getInt();
            localHp   =bb.getFloat();
            localArmor=bb.getFloat();

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
                e.isBot=bb.get()!=0; bb.get(new byte[3]);
                ea[i]=e;
            }
            enemies=ea;

            // Items
            ItemData[] ia=new ItemData[Math.min(in,200)];
            for(int i=0;i<ia.length;i++){
                ItemData it=new ItemData();
                it.sx=bb.getFloat();it.sy=bb.getFloat();
                it.dist=bb.getFloat();it.type=bb.get()&0xFF;
                byte[] nb=new byte[20];bb.get(nb);
                it.name=new String(nb).replace("\0","").trim();
                bb.get(new byte[3]); ia[i]=it;
            }
            items=ia;

            // Vehicles
            VehData[] va=new VehData[Math.min(vn,20)];
            for(int i=0;i<va.length;i++){
                VehData v=new VehData();
                v.sx=bb.getFloat();v.sy=bb.getFloat();
                v.dist=bb.getFloat();v.hp=bb.getFloat();
                bb.get(new byte[4]); va[i]=v;
            }
            vehicles=va;

        }catch(Exception ex){enemies=new EspData[0];}
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c){
        float sw=getWidth(), sh=getHeight();

        // ── Night Mode overlay
        if(feat[19]){
            pOverlay.setColor(Color.argb(120,0,10,40));
            c.drawRect(0,0,sw,sh,pOverlay);
        }
        // ── Bright Mode overlay
        if(feat[21]){
            pOverlay.setColor(Color.argb(40,255,255,220));
            c.drawRect(0,0,sw,sh,pOverlay);
        }

        // ── Düşman ESP
        if(feat[11]){
            for(EspData e:enemies){
                if(e==null)continue;
                drawEnemy(c,e,sw,sh);
            }
        }

        // ── Item ESP
        if(feat[12]||feat[14]){
            for(ItemData it:items){
                if(it==null)continue;
                drawItem(c,it);
            }
        }

        // ── Vehicle ESP
        if(feat[13]){
            for(VehData v:vehicles){
                if(v==null)continue;
                drawVehicle(c,v);
            }
        }

        // ── Radar
        if(feat[18]) drawRadar(c,sw,sh);

        // ── Crosshair
        if(feat[17]) drawCrosshair(c,sw,sh);

        // ── Local HP bar (daima)
        drawLocalBar(c,sw,sh);
    }

    private void drawEnemy(Canvas c,EspData e,float sw,float sh){
        float bT=e.boxTop,bB=e.boxBot;
        float bL=e.boxCx-e.boxW*0.5f,bR=e.boxCx+e.boxW*0.5f;
        float bH=bB-bT;
        int col=hpColor(e.hp);
        float hpR=Math.max(0f,Math.min(1f,e.hp/200f));
        float arR=Math.max(0f,Math.min(1f,e.armor/200f));

        // Snapline
        pSnap.setColor(Color.argb(90,255,60,60));
        c.drawLine(sw*0.5f,sh,e.boxCx,bB,pSnap);

        // Kutu
        c.drawRect(bL,bT,bR,bB,pFill);
        pBox.setColor(col);
        float cl=bH*0.22f;
        c.drawLine(bL,bT,bL+cl,bT,pBox);c.drawLine(bL,bT,bL,bT+cl,pBox);
        c.drawLine(bR,bT,bR-cl,bT,pBox);c.drawLine(bR,bT,bR,bT+cl,pBox);
        c.drawLine(bL,bB,bL+cl,bB,pBox);c.drawLine(bL,bB,bL,bB-cl,pBox);
        c.drawLine(bR,bB,bR-cl,bB,pBox);c.drawLine(bR,bB,bR,bB-cl,pBox);

        // HP bar
        float bx=bL-7f,bw=4f;
        c.drawRect(bx,bT,bx+bw,bB,pBg);
        pHp.setColor(col);
        c.drawRect(bx,bB-bH*hpR,bx+bw,bB,pHp);
        if(e.armor>0f){
            float ax=bx-6f;
            c.drawRect(ax,bT,ax+bw,bB,pBg);
            c.drawRect(ax,bB-bH*arR,ax+bw,bB,pAr);
        }

        // İskelet
        pSkel.setAlpha(180);
        skel(c,e.hx,e.hy,e.brx,e.bry);
        skel(c,e.brx,e.bry,e.hipx,e.hipy);
        skel(c,e.hx,e.hy,e.lshx,e.lshy);
        skel(c,e.lshx,e.lshy,e.lhax,e.lhay);
        skel(c,e.hx,e.hy,e.rshx,e.rshy);
        skel(c,e.rshx,e.rshy,e.rhax,e.rhay);
        skel(c,e.hipx,e.hipy,e.lax,e.lay);
        skel(c,e.hipx,e.hipy,e.rax,e.ray);
        pSkel.setStyle(Paint.Style.FILL);
        joint(c,e.hx,e.hy,4f,col);
        joint(c,e.brx,e.bry,3f,Color.WHITE);
        joint(c,e.hipx,e.hipy,3f,Color.WHITE);
        joint(c,e.lshx,e.lshy,2.5f,Color.WHITE);
        joint(c,e.rshx,e.rshy,2.5f,Color.WHITE);
        joint(c,e.lhax,e.lhay,2f,Color.WHITE);
        joint(c,e.rhax,e.rhay,2f,Color.WHITE);
        joint(c,e.lax,e.lay,2f,Color.WHITE);
        joint(c,e.rax,e.ray,2f,Color.WHITE);
        pSkel.setStyle(Paint.Style.STROKE);

        // İsim + mesafe
        c.drawText(e.isBot?"[BOT] "+e.name:e.name,e.boxCx,bT-28f,pName);
        pDist.setColor(Color.argb(220,255,220,80));
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
