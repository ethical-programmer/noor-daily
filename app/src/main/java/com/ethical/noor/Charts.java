package com.ethical.noor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

public class Charts {
    /** Progress ring with big centered text. */
    public static class Ring extends View {
        float frac; String big = "", small = ""; int color = Ui.GREEN; float stroke = 0.09f;
        final Paint tr = new Paint(Paint.ANTI_ALIAS_FLAG), ar = new Paint(Paint.ANTI_ALIAS_FLAG), t1 = new Paint(Paint.ANTI_ALIAS_FLAG), t2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        public Ring(Context c) {
            super(c);
            tr.setStyle(Paint.Style.STROKE); tr.setColor(Ui.LINE);
            ar.setStyle(Paint.Style.STROKE); ar.setStrokeCap(Paint.Cap.ROUND);
            t1.setColor(Ui.TEXT); t1.setTextAlign(Paint.Align.CENTER); t1.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            t2.setColor(Ui.MUTED); t2.setTextAlign(Paint.Align.CENTER);
        }
        public Ring set(float f, String b, String s, int col) { frac = Math.max(0, Math.min(1, f)); big = b; small = s; color = col; invalidate(); return this; }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), sw = Math.min(w, h) * stroke, r = Math.min(w, h) / 2 - sw;
            tr.setStrokeWidth(sw); ar.setStrokeWidth(sw); ar.setColor(color);
            RectF o = new RectF(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r);
            c.drawArc(o, 0, 360, false, tr);
            if (frac > 0.002f) c.drawArc(o, -90, 360 * frac, false, ar);
            t1.setTextSize(r * (big.length() > 5 ? 0.36f : 0.46f)); t2.setTextSize(r * 0.2f);
            c.drawText(big, w / 2, h / 2 + r * (small.isEmpty() ? 0.16f : 0.08f), t1);
            if (!small.isEmpty()) c.drawText(small, w / 2, h / 2 + r * 0.38f, t2);
        }
    }

    /** Simple rounded bar chart with an optional goal line. */
    public static class Bars extends View {
        float[] v = new float[0]; String[] labels = new String[0]; float goal; int color = Ui.BLUE; int highlight = -1;
        final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG), gp = new Paint(Paint.ANTI_ALIAS_FLAG), lp = new Paint(Paint.ANTI_ALIAS_FLAG), vp = new Paint(Paint.ANTI_ALIAS_FLAG);
        public Bars(Context c) {
            super(c);
            gp.setStyle(Paint.Style.STROKE); gp.setColor(Ui.GOLD); gp.setStrokeWidth(3); gp.setPathEffect(new DashPathEffect(new float[]{12, 10}, 0));
            lp.setColor(Ui.MUTED); lp.setTextAlign(Paint.Align.CENTER);
            vp.setColor(Ui.TEXT); vp.setTextAlign(Paint.Align.CENTER); vp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        public Bars set(float[] values, String[] lab, float goalLine, int col, int hl) { v = values; labels = lab; goal = goalLine; color = col; highlight = hl; invalidate(); return this; }
        @Override protected void onDraw(Canvas c) {
            if (v.length == 0) return;
            float w = getWidth(), h = getHeight(), lh = h * 0.14f, top = h * 0.1f, bottom = h - lh;
            float max = goal; for (float x : v) max = Math.max(max, x); if (max <= 0) max = 1;
            float slot = w / v.length, bw = slot * 0.58f;
            lp.setTextSize(Math.min(lh * 0.7f, slot * 0.5f)); vp.setTextSize(lp.getTextSize() * 0.95f);
            for (int i = 0; i < v.length; i++) {
                float x = slot * i + (slot - bw) / 2, bh = (bottom - top) * (v[i] / max);
                bp.setColor(Ui.LINE);
                c.drawRoundRect(new RectF(x, top, x + bw, bottom), bw / 3, bw / 3, bp);
                if (v[i] > 0) {
                    bp.setColor(i == highlight ? Ui.GOLD : color);
                    c.drawRoundRect(new RectF(x, bottom - Math.max(bh, bw * 0.4f), x + bw, bottom), bw / 3, bw / 3, bp);
                }
                if (i < labels.length && labels[i] != null) c.drawText(labels[i], x + bw / 2, h - lh * 0.15f, lp);
            }
            if (goal > 0) { float gy = bottom - (bottom - top) * (goal / max); c.drawLine(0, gy, w, gy, gp); }
        }
    }
}
