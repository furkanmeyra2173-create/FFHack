package com.ff.menu;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

public class EspView extends View {

    private static final int ENTRY_BYTES = 128;

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

    private EspData[] entries = new EspData[0];
    private float localHp     = 0f;
    private float localArmor  = 0f;

    static class EspData {
        float hx, hy, brx, bry, hipx, hipy;
        float lshx, lshy, rshx, rshy;
        float lhax, lhay, rhax, rhay;
        float lax, lay, rax, ray;
        float boxTop, boxBot, boxCx, boxW;
        float hp, armor, dist;
        String name;
        boolean isBot;
    }

    public EspView(Context ctx) {
        super(ctx);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        pBox.setStyle(Paint.Style.STROKE);
        pBox.setStrokeWidth(1.8f);

        pFill.setStyle(Paint.Style.FILL);
        pFill.setColor(Color.argb(20, 255, 255, 255));

        pSkel.setStyle(Paint.Style.STROKE);
        pSkel.setStrokeWidth(1.4f);
        pSkel.setColor(Color.argb(200, 255, 255, 255));

        pHp.setStyle(Paint.Style.FILL);

        pAr.setStyle(Paint.Style.FILL);
        pAr.setColor(Color.argb(200, 80, 160, 255));

        pBg.setStyle(Paint.Style.FILL);
        pBg.setColor(Color.argb(160, 0, 0, 0));

        pName.setColor(Color.WHITE);
        pName.setTextSize(24f);
        pName.setTextAlign(Paint.Align.CENTER);
        pName.setShadowLayer(4f, 1f, 1f, Color.BLACK);

        pDist.setTextSize(20f);
        pDist.setTextAlign(Paint.Align.CENTER);
        pDist.setShadowLayer(3f, 1f, 1f, Color.BLACK);

        pSnap.setStyle(Paint.Style.STROKE);
        pSnap.setStrokeWidth(1f);

        pLocal.setStyle(Paint.Style.FILL);
        pLocal.setAntiAlias(true);
    }

    private int hpColor(float hp) {
        float r = hp / 200f;
        if (r > 0.66f) return Color.argb(220, 40, 210, 40);
        if (r > 0.33f) return Color.argb(220, 210, 190, 30);
        return Color.argb(220, 210, 50, 30);
    }

    public void loadEsp(String path) {
        try (FileInputStream fis = new FileInputStream(path);
             FileChannel fc = fis.getChannel()) {

            long size = fc.size();
            if (size < 12) {
                entries = new EspData[0];
                invalidate();
                return;
            }

            ByteBuffer bb = ByteBuffer.allocate((int) size);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            fc.read(bb);
            bb.flip();

            int n      = bb.getInt();
            localHp    = bb.getFloat();
            localArmor = bb.getFloat();

            if (n <= 0 || n > 100) {
                entries = new EspData[0];
                invalidate();
                return;
            }

            EspData[] arr = new EspData[n];
            for (int i = 0; i < n; i++) {
                EspData e = new EspData();
                e.hx   = bb.getFloat(); e.hy   = bb.getFloat();
                e.brx  = bb.getFloat(); e.bry  = bb.getFloat();
                e.hipx = bb.getFloat(); e.hipy = bb.getFloat();
                e.lshx = bb.getFloat(); e.lshy = bb.getFloat();
                e.rshx = bb.getFloat(); e.rshy = bb.getFloat();
                e.lhax = bb.getFloat(); e.lhay = bb.getFloat();
                e.rhax = bb.getFloat(); e.rhay = bb.getFloat();
                e.lax  = bb.getFloat(); e.lay  = bb.getFloat();
                e.rax  = bb.getFloat(); e.ray  = bb.getFloat();
                e.boxTop = bb.getFloat(); e.boxBot = bb.getFloat();
                e.boxCx  = bb.getFloat(); e.boxW   = bb.getFloat();
                e.hp     = bb.getFloat();
                e.armor  = bb.getFloat();
                e.dist   = bb.getFloat();
                byte[] nb = new byte[24];
                bb.get(nb);
                e.name  = new String(nb).replace("\0", "").trim();
                e.isBot = bb.get() != 0;
                bb.get(new byte[3]);
                arr[i]  = e;
            }
            entries = arr;

        } catch (Exception ex) {
            entries = new EspData[0];
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float sw = getWidth();
        float sh = getHeight();

        for (EspData e : entries) {
            if (e == null) continue;
            float bT = e.boxTop, bB = e.boxBot;
            float bL = e.boxCx - e.boxW * 0.5f;
            float bR = e.boxCx + e.boxW * 0.5f;
            float bH = bB - bT;
            int   col = hpColor(e.hp);
            float hpR = Math.max(0f, Math.min(1f, e.hp    / 200f));
            float arR = Math.max(0f, Math.min(1f, e.armor / 200f));

            // Snapline
            pSnap.setColor(Color.argb(90, 255, 60, 60));
            c.drawLine(sw * 0.5f, sh, e.boxCx, bB, pSnap);

            // Kutu dolgu
            c.drawRect(bL, bT, bR, bB, pFill);

            // Köşe çerçeve
            pBox.setColor(col);
            float cl = bH * 0.22f;
            c.drawLine(bL, bT, bL + cl, bT, pBox);
            c.drawLine(bL, bT, bL, bT + cl, pBox);
            c.drawLine(bR, bT, bR - cl, bT, pBox);
            c.drawLine(bR, bT, bR, bT + cl, pBox);
            c.drawLine(bL, bB, bL + cl, bB, pBox);
            c.drawLine(bL, bB, bL, bB - cl, pBox);
            c.drawLine(bR, bB, bR - cl, bB, pBox);
            c.drawLine(bR, bB, bR, bB - cl, pBox);

            // HP bar (sol)
            float bx = bL - 7f, bw = 4f;
            c.drawRect(bx, bT, bx + bw, bB, pBg);
            pHp.setColor(col);
            c.drawRect(bx, bB - bH * hpR, bx + bw, bB, pHp);

            // Zırh bar
            if (e.armor > 0f) {
                float ax = bx - 6f;
                c.drawRect(ax, bT, ax + bw, bB, pBg);
                c.drawRect(ax, bB - bH * arR, ax + bw, bB, pAr);
            }

            // İskelet
            pSkel.setAlpha(180);
            skel(c, e.hx,  e.hy,   e.brx,  e.bry);
            skel(c, e.brx, e.bry,  e.hipx, e.hipy);
            skel(c, e.hx,  e.hy,   e.lshx, e.lshy);
            skel(c, e.lshx,e.lshy, e.lhax, e.lhay);
            skel(c, e.hx,  e.hy,   e.rshx, e.rshy);
            skel(c, e.rshx,e.rshy, e.rhax, e.rhay);
            skel(c, e.hipx,e.hipy, e.lax,  e.lay);
            skel(c, e.hipx,e.hipy, e.rax,  e.ray);

            // Eklemler
            pSkel.setStyle(Paint.Style.FILL);
            joint(c, e.hx,   e.hy,   4f, col);
            joint(c, e.brx,  e.bry,  3f, Color.WHITE);
            joint(c, e.hipx, e.hipy, 3f, Color.WHITE);
            joint(c, e.lshx, e.lshy, 2.5f, Color.WHITE);
            joint(c, e.rshx, e.rshy, 2.5f, Color.WHITE);
            joint(c, e.lhax, e.lhay, 2f, Color.WHITE);
            joint(c, e.rhax, e.rhay, 2f, Color.WHITE);
            joint(c, e.lax,  e.lay,  2f, Color.WHITE);
            joint(c, e.rax,  e.ray,  2f, Color.WHITE);
            pSkel.setStyle(Paint.Style.STROKE);

            // İsim
            String label = e.isBot ? "[BOT] " + e.name : e.name;
            c.drawText(label, e.boxCx, bT - 28f, pName);

            // Mesafe
            pDist.setColor(Color.argb(220, 255, 220, 80));
            c.drawText(String.format("%.0fm", e.dist), e.boxCx, bT - 8f, pDist);

            // HP sayısı
            pDist.setColor(col);
            c.drawText((int) e.hp + "HP", bL - 14f, bB, pDist);
        }

        // Local HP bar — kalıcı yeşil
        drawLocalBar(c, sw, sh);
    }

    private void drawLocalBar(Canvas c, float sw, float sh) {
        float barW = sw * 0.40f;
        float barH = 16f;
        float barX = (sw - barW) * 0.5f;
        float barY = sh - 130f;
        float hpR  = Math.max(0f, Math.min(1f, localHp    / 200f));
        float arR  = Math.max(0f, Math.min(1f, localArmor / 200f));

        // Arka plan
        pBg.setAlpha(190);
        RectF bgRect = new RectF(barX - 3, barY - 3, barX + barW + 3, barY + barH + 3);
        c.drawRoundRect(bgRect, 8f, 8f, pBg);

        // Yeşil HP
        pLocal.setColor(Color.argb(240, 45, 200, 75));
        RectF hpRect = new RectF(barX, barY, barX + barW * hpR, barY + barH);
        c.drawRoundRect(hpRect, 6f, 6f, pLocal);

        // HP yazısı
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE);
        pt.setTextSize(22f);
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setShadowLayer(4f, 1f, 1f, Color.BLACK);
        pt.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText((int) localHp + " / 200", sw * 0.5f, barY - 7f, pt);

        // Mavi zırh bar
        if (localArmor > 0f) {
            float aY = barY + barH + 6f;
            float aH = 8f;
            RectF arBg = new RectF(barX - 3, aY - 2, barX + barW + 3, aY + aH + 2);
            pBg.setAlpha(160);
            c.drawRoundRect(arBg, 5f, 5f, pBg);
            pLocal.setColor(Color.argb(210, 70, 150, 255));
            RectF arFill = new RectF(barX, aY, barX + barW * arR, aY + aH);
            c.drawRoundRect(arFill, 4f, 4f, pLocal);
        }
    }

    private void skel(Canvas c, float x1, float y1, float x2, float y2) {
        if (x1 == 0 && y1 == 0) return;
        if (x2 == 0 && y2 == 0) return;
        c.drawLine(x1, y1, x2, y2, pSkel);
    }

    private void joint(Canvas c, float x, float y, float r, int color) {
        if (x == 0 && y == 0) return;
        pSkel.setColor(color);
        c.drawCircle(x, y, r, pSkel);
        pSkel.setColor(Color.argb(180, 255, 255, 255));
    }
}
