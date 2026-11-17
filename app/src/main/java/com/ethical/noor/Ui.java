package com.ethical.noor;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Premium dark glassmorphism UI kit. */
public class Ui {
    public static final int BG = Color.parseColor("#06080C");
    public static final int TEXT = Color.parseColor("#EEF1F6");
    public static final int MUTED = Color.parseColor("#8C95A6");
    public static final int FAINT = Color.parseColor("#5B6475");
    public static final int ACCENT = Color.parseColor("#7CE0C3");   // mint
    public static final int GREEN = ACCENT;
    public static final int BLUE = Color.parseColor("#82B8F2");
    public static final int GOLD = Color.parseColor("#E5C07B");
    public static final int VIOLET = Color.parseColor("#B7A6F2");
    public static final int RED = Color.parseColor("#F0868E");
    public static final int PURPLE = VIOLET;
    public static final int INK = Color.parseColor("#0A0D12");

    // two-tone gradients used only for rings and bars
    public static final int[] G_WATER = {Color.parseColor("#9CC8F5"), Color.parseColor("#5B8DEF")};
    public static final int[] G_TASK = {Color.parseColor("#9AF0D6"), Color.parseColor("#3FBF9B")};
    public static final int[] G_QURAN = {Color.parseColor("#F1D59A"), Color.parseColor("#C99A4B")};
    public static final int[] G_ZIKR = {Color.parseColor("#CFC2F8"), Color.parseColor("#8E7AE6")};
    public static final int[] G_REPORT = {Color.parseColor("#F3F4F6"), Color.parseColor("#9CA3AF")};
    public static final int[] G_FOCUS = G_ZIKR;
    public static final int[] G_SCORE = {Color.parseColor("#9AF0D6"), Color.parseColor("#82B8F2"), Color.parseColor("#B7A6F2"), Color.parseColor("#9AF0D6")};

    public static final PathInterpolator EASE = new PathInterpolator(0.2f, 0.9f, 0.25f, 1f);

    public static int dp(Context c, float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()); }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }
    public static TextView title(Context c, String s, float sp) {
        TextView t = text(c, s, sp, TEXT, true);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLetterSpacing(-0.025f);
        return t;
    }
    public static TextView label(Context c, String s) {
        TextView t = text(c, s.toUpperCase(), 11.5f, FAINT, true);
        t.setLetterSpacing(0.12f);
        return t;
    }
    public static GradientDrawable grad(int[] colors, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(r);
        return g;
    }
    /** frosted glass: faint white fill + hairline border */
    public static GradientDrawable glass(Context c, float rDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x1AFFFFFF, 0x08FFFFFF});
        g.setCornerRadius(dp(c, rDp));
        g.setStroke(Math.max(1, dp(c, 0.8f)), 0x1FFFFFFF);
        return g;
    }
    /** glass with a soft accent edge (used for completed items) */
    public static GradientDrawable glassAccent(Context c, int color, float rDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{(color & 0x00FFFFFF) | 0x1C000000, 0x08FFFFFF});
        g.setCornerRadius(dp(c, rDp));
        g.setStroke(Math.max(1, dp(c, 1)), (color & 0x00FFFFFF) | 0x55000000);
        return g;
    }
    public static GradientDrawable solid(int color, float r) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(r); return g; }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 18);
        l.setPadding(p, p, p, p);
        l.setBackground(glass(c, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    public static int icon(Context c, String name) {
        int id = c.getResources().getIdentifier("ic_" + name, "drawable", c.getPackageName());
        return id != 0 ? id : R.drawable.ic_check;
    }
    public static ImageView img(Context c, int res, int color, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setImageTintList(ColorStateList.valueOf(color));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }
    /** icon inside a glass tile; filled softly with the colour when active */
    public static FrameLayout tile(Context c, int res, int color, int sizeDp, boolean active) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(active ? solid((color & 0x00FFFFFF) | 0x2E000000, dp(c, sizeDp * 0.32f)) : glass(c, sizeDp * 0.32f));
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setImageTintList(ColorStateList.valueOf(active ? color : TEXT));
        int s = dp(c, sizeDp * 0.48f);
        f.addView(i, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return f;
    }
    /** primary = soft white pill with dark text; otherwise glass */
    public static TextView button(Context c, String s, boolean primary) {
        TextView b = text(c, s, 15, primary ? INK : TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? solid(Color.parseColor("#EEF1F6"), dp(c, 16)) : glass(c, 16));
        b.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        press(b);
        return b;
    }
    public static void setButton(Context c, TextView b, boolean primary) {
        b.setBackground(primary ? solid(Color.parseColor("#EEF1F6"), dp(c, 16)) : glass(c, 16));
        b.setTextColor(primary ? INK : TEXT);
    }
    /** round check bubble */
    public static FrameLayout check(Context c, boolean done, int color, int sizeDp) {
        FrameLayout f = new FrameLayout(c);
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL);
        if (done) g.setColor(color); else { g.setColor(0x0AFFFFFF); g.setStroke(dp(c, 1.4f), 0x40FFFFFF); }
        f.setBackground(g);
        if (done) { ImageView i = new ImageView(c); i.setImageResource(R.drawable.ic_check); i.setImageTintList(ColorStateList.valueOf(INK)); int s = dp(c, sizeDp * 0.56f); f.addView(i, new FrameLayout.LayoutParams(s, s, Gravity.CENTER)); }
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return f;
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

    public static View bar(Context c, float frac, int[] colors) {
        LinearLayout track = new LinearLayout(c);
        track.setBackground(solid(0x14FFFFFF, dp(c, 4)));
        View fill = new View(c);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors); g.setCornerRadius(dp(c, 4));
        fill.setBackground(g);
        track.setWeightSum(1f);
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, Math.max(0.001f, Math.min(1f, frac))));
        fill.setPivotX(0); fill.setScaleX(0f);
        fill.animate().scaleX(1f).setDuration(900).setStartDelay(150).setInterpolator(EASE).start();
        track.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 6)));
        return track;
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void press(View v) {
        v.setOnTouchListener((x, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) x.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
                x.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(new OvershootInterpolator(2.2f)).start();
            return false;
        });
    }
    public static void enter(View v, int i) {
        v.setAlpha(0f); v.setTranslationY(v.getResources().getDisplayMetrics().density * 18);
        v.animate().alpha(1f).translationY(0).setDuration(480).setStartDelay(35L * i).setInterpolator(EASE).start();
    }
    public static void pop(View v) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.08f, 1f), PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.08f, 1f));
        a.setDuration(380); a.start();
    }
}
