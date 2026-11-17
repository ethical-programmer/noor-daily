package com.ethical.noor;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Glassy, colorful UI kit. */
public class Ui {
    public static final int BG = Color.parseColor("#070A12");
    public static final int TEXT = Color.parseColor("#F4F7FB");
    public static final int MUTED = Color.parseColor("#A3AEC2");
    public static final int GREEN = Color.parseColor("#34D399");
    public static final int BLUE = Color.parseColor("#38BDF8");
    public static final int GOLD = Color.parseColor("#FBBF24");
    public static final int RED = Color.parseColor("#FB7185");
    public static final int PURPLE = Color.parseColor("#A78BFA");
    public static final int LINE = Color.parseColor("#2A3346");

    // gradient palettes
    public static final int[] G_WATER = {Color.parseColor("#22D3EE"), Color.parseColor("#3B82F6"), Color.parseColor("#6366F1")};
    public static final int[] G_TASK = {Color.parseColor("#34D399"), Color.parseColor("#10B981"), Color.parseColor("#06B6D4")};
    public static final int[] G_QURAN = {Color.parseColor("#FDE047"), Color.parseColor("#F59E0B"), Color.parseColor("#10B981")};
    public static final int[] G_ZIKR = {Color.parseColor("#C084FC"), Color.parseColor("#A855F7"), Color.parseColor("#EC4899")};
    public static final int[] G_REPORT = {Color.parseColor("#F472B6"), Color.parseColor("#FB923C"), Color.parseColor("#FBBF24")};
    public static final int[] G_FOCUS = {Color.parseColor("#818CF8"), Color.parseColor("#A78BFA"), Color.parseColor("#F0ABFC")};
    public static final int[] G_SCORE = {Color.parseColor("#22D3EE"), Color.parseColor("#34D399"), Color.parseColor("#FBBF24"), Color.parseColor("#F472B6"), Color.parseColor("#22D3EE")};

    public static final PathInterpolator EASE = new PathInterpolator(0.2f, 0.9f, 0.25f, 1f);

    public static int dp(Context c, float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()); }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }
    public static TextView title(Context c, String s, float sp) {
        TextView t = text(c, s, sp, TEXT, true);
        t.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        t.setLetterSpacing(-0.02f);
        return t;
    }
    public static GradientDrawable grad(int[] colors, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(r);
        return g;
    }
    public static GradientDrawable glass(Context c, float rDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x2EFFFFFF, 0x0FFFFFFF});
        g.setCornerRadius(dp(c, rDp));
        g.setStroke(dp(c, 1), 0x30FFFFFF);
        return g;
    }
    public static GradientDrawable tint(Context c, int[] colors, float rDp, int alpha) {
        int[] a = new int[colors.length];
        for (int i = 0; i < colors.length; i++) a[i] = (colors[i] & 0x00FFFFFF) | (alpha << 24);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, a);
        g.setCornerRadius(dp(c, rDp));
        g.setStroke(dp(c, 1), (colors[0] & 0x00FFFFFF) | 0x66000000);
        return g;
    }
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 18);
        l.setPadding(p, p, p, p);
        l.setBackground(glass(c, 26));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 14);
        l.setLayoutParams(lp);
        return l;
    }
    /** emoji inside a glowing gradient tile */
    public static TextView tile(Context c, String emoji, int[] colors, int sizeDp) {
        TextView t = text(c, emoji, sizeDp * 0.42f, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        t.setBackground(grad(colors, dp(c, sizeDp * 0.32f)));
        t.setElevation(dp(c, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        t.setLayoutParams(lp);
        return t;
    }
    public static TextView button(Context c, String s, int[] colors) {
        TextView b = text(c, s, 15, colors == null ? TEXT : Color.parseColor("#0B1020"), true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(colors == null ? glass(c, 18) : grad(colors, dp(c, 18)));
        b.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        if (colors != null) b.setElevation(dp(c, 4));
        press(b);
        return b;
    }
    public static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    public static LinearLayout col(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    public static LinearLayout.LayoutParams weight(Context c, int hDp, int leftDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, hDp <= 0 ? -2 : dp(c, hDp), 1);
        l.leftMargin = dp(c, leftDp);
        return l;
    }
    public static LinearLayout.LayoutParams top(Context c, int topDp) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2); l.topMargin = dp(c, topDp); return l; }

    /** gradient progress bar that animates to its value */
    public static View bar(Context c, float frac, int[] colors) {
        LinearLayout track = new LinearLayout(c);
        GradientDrawable tr = new GradientDrawable(); tr.setColor(0x22FFFFFF); tr.setCornerRadius(dp(c, 6));
        track.setBackground(tr);
        View fill = new View(c);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors); g.setCornerRadius(dp(c, 6));
        fill.setBackground(g);
        track.setWeightSum(1f);
        float f = Math.max(0.001f, Math.min(1f, frac));
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, f));
        fill.setPivotX(0); fill.setScaleX(0f);
        fill.animate().scaleX(1f).setDuration(900).setStartDelay(150).setInterpolator(EASE).start();
        track.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 10)));
        return track;
    }

    /** springy press feedback */
    @SuppressLint("ClickableViewAccessibility")
    public static void press(View v) {
        v.setOnTouchListener((x, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) x.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
            else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
                x.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(new OvershootInterpolator(3f)).start();
            return false;
        });
    }
    /** slide-up fade-in, staggered */
    public static void enter(View v, int i) {
        v.setAlpha(0f); v.setTranslationY(v.getResources().getDisplayMetrics().density * 26);
        v.animate().alpha(1f).translationY(0).setDuration(520).setStartDelay(40L * i).setInterpolator(EASE).start();
    }
    public static void pop(View v) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat("scaleX", 0.7f, 1.12f, 1f), PropertyValuesHolder.ofFloat("scaleY", 0.7f, 1.12f, 1f));
        a.setDuration(420); a.start();
    }
}
