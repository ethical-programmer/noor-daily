package com.ethical.noor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.view.View;

public class Charts {
    /** Animated gradient progress ring with a soft glow. */
    public static class Ring extends View {
        float frac, shown; String big = "", small = ""; int[] colors = Ui.G_TASK; public float stroke = 0.09f;
        final Paint tr = new Paint(Paint.ANTI_ALIAS_FLAG), ar = new Paint(Paint.ANTI_ALIAS_FLAG), glow = new Paint(Paint.ANTI_ALIAS_FLAG), t1 = new Paint(Paint.ANTI_ALIAS_FLAG), t2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        ValueAnimator anim;
        public Ring(Context c) {
            super(c);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            tr.setStyle(Paint.Style.STROKE); tr.setColor(0x22FFFFFF);
            ar.setStyle(Paint.Style.STROKE); ar.setStrokeCap(Paint.Cap.ROUND);
            glow.setStyle(Paint.Style.STROKE); glow.setStrokeCap(Paint.Cap.ROUND);
            t1.setColor(Ui.TEXT); t1.setTextAlign(Paint.Align.CENTER); t1.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            t2.setColor(Ui.MUTED); t2.setTextAlign(Paint.Align.CENTER);
        }
        public Ring set(float f, String b, String s, int[] col) {
            frac = Math.max(0, Math.min(1, f)); big = b; small = s; colors = col;
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(shown, frac); anim.setDuration(1000); anim.setInterpolator(Ui.EASE);
            anim.addUpdateListener(a -> { shown = (float) a.getAnimatedValue(); invalidate(); });
            anim.start();
            return this;
        }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), sw = Math.min(w, h) * stroke, r = Math.min(w, h) / 2 - sw * 1.4f;
            float cx = w / 2, cy = h / 2;
            tr.setStrokeWidth(sw); ar.setStrokeWidth(sw); glow.setStrokeWidth(sw * 1.6f);
            RectF o = new RectF(cx - r, cy - r, cx + r, cy + r);
            c.drawArc(o, 0, 360, false, tr);
            if (shown > 0.002f) {
                int[] cc = new int[colors.length + 1]; System.arraycopy(colors, 0, cc, 0, colors.length); cc[colors.length] = colors[0];
                SweepGradient sg = new SweepGradient(cx, cy, cc, null);
                Matrix m = new Matrix(); m.setRotate(-90, cx, cy); sg.setLocalMatrix(m);
                ar.setShader(sg); glow.setShader(sg);
                glow.setMaskFilter(new BlurMaskFilter(sw * 0.9f, BlurMaskFilter.Blur.NORMAL)); glow.setAlpha(150);
                c.drawArc(o, -90, 360 * shown, false, glow);
                c.drawArc(o, -90, 360 * shown, false, ar);
            }
            t1.setTextSize(r * (big.length() > 5 ? 0.36f : big.length() > 3 ? 0.44f : 0.52f)); t2.setTextSize(r * 0.19f);
            c.drawText(big, cx, cy + r * (small.isEmpty() ? 0.18f : 0.1f), t1);
            if (!small.isEmpty()) c.drawText(small, cx, cy + r * 0.42f, t2);
        }
    }

    /** Animated gradient bar chart with an optional dashed goal line. */
    public static class Bars extends View {
        float[] v = new float[0]; String[] labels = new String[0]; float goal; int[] colors = Ui.G_WATER; int highlight = -1; float grow = 0;
        final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG), gp = new Paint(Paint.ANTI_ALIAS_FLAG), lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        public Bars(Context c) {
            super(c);
            gp.setStyle(Paint.Style.STROKE); gp.setColor(Ui.GOLD); gp.setStrokeWidth(4); gp.setPathEffect(new DashPathEffect(new float[]{14, 12}, 0));
            lp.setColor(Ui.MUTED); lp.setTextAlign(Paint.Align.CENTER);
        }
        public Bars set(float[] values, String[] lab, float goalLine, int[] col, int hl) {
            v = values; labels = lab; goal = goalLine; colors = col; highlight = hl;
            ValueAnimator a = ValueAnimator.ofFloat(0, 1); a.setDuration(1100); a.setStartDelay(200); a.setInterpolator(Ui.EASE);
            a.addUpdateListener(x -> { grow = (float) x.getAnimatedValue(); invalidate(); }); a.start();
            return this;
        }
        @Override protected void onDraw(Canvas c) {
            if (v.length == 0) return;
            float w = getWidth(), h = getHeight(), lh = h * 0.15f, top = h * 0.08f, bottom = h - lh;
            float max = goal; for (float x : v) max = Math.max(max, x); if (max <= 0) max = 1;
            float slot = w / v.length, bw = slot * 0.56f;
            lp.setTextSize(Math.min(lh * 0.66f, slot * 0.46f));
            for (int i = 0; i < v.length; i++) {
                float x = slot * i + (slot - bw) / 2, bh = (bottom - top) * (v[i] / max) * grow;
                bp.setShader(null); bp.setColor(0x16FFFFFF);
                c.drawRoundRect(new RectF(x, top, x + bw, bottom), bw / 2.4f, bw / 2.4f, bp);
                if (v[i] > 0) {
                    float y0 = bottom - Math.max(bh, bw * 0.5f * grow);
                    int[] cc = i == highlight ? Ui.G_REPORT : colors;
                    bp.setShader(new LinearGradient(0, y0, 0, bottom, cc[0], cc[cc.length - 1], Shader.TileMode.CLAMP));
                    c.drawRoundRect(new RectF(x, y0, x + bw, bottom), bw / 2.4f, bw / 2.4f, bp);
                }
                if (i < labels.length && labels[i] != null) c.drawText(labels[i], x + bw / 2, h - lh * 0.12f, lp);
            }
            if (goal > 0) { float gy = bottom - (bottom - top) * (goal / max); c.drawLine(0, gy, w, gy, gp); }
        }
    }

    /** Slowly drifting colorful aurora behind everything. */
    public static class Aurora extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int[] cols = {Color.parseColor("#7C3AED"), Color.parseColor("#06B6D4"), Color.parseColor("#EC4899"), Color.parseColor("#10B981")};
        final float[][] seed = {{0.15f, 0.12f, 0.0f}, {0.9f, 0.3f, 1.7f}, {0.2f, 0.75f, 3.1f}, {0.85f, 0.9f, 4.4f}};
        long start = System.currentTimeMillis();
        public Aurora(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), t = (System.currentTimeMillis() - start) / 1000f;
            c.drawColor(Ui.BG);
            for (int i = 0; i < cols.length; i++) {
                float x = w * (seed[i][0] + 0.12f * (float) Math.sin(t * 0.13 + seed[i][2]));
                float y = h * (seed[i][1] + 0.08f * (float) Math.cos(t * 0.11 + seed[i][2]));
                float r = Math.max(w, h) * 0.55f;
                p.setShader(new RadialGradient(x, y, r, (cols[i] & 0x00FFFFFF) | 0x55000000, 0x00000000, Shader.TileMode.CLAMP));
                c.drawCircle(x, y, r, p);
            }
            postInvalidateDelayed(50);
        }
    }
}
