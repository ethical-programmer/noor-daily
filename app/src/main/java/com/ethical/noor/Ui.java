package com.ethical.noor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Ui {
    public static final int BG = Color.parseColor("#0A0F14");
    public static final int NAV = Color.parseColor("#0E151C");
    public static final int CARD = Color.parseColor("#121B24");
    public static final int LINE = Color.parseColor("#1F2C38");
    public static final int GREEN = Color.parseColor("#10B981");
    public static final int BLUE = Color.parseColor("#38BDF8");
    public static final int GOLD = Color.parseColor("#F5B840");
    public static final int RED = Color.parseColor("#F87171");
    public static final int PURPLE = Color.parseColor("#A78BFA");
    public static final int TEXT = Color.parseColor("#E9F2EE");
    public static final int MUTED = Color.parseColor("#8A9AA8");

    public static int dp(Context c, float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()); }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }
    public static GradientDrawable round(int color, float r, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(r);
        if (stroke != 0) g.setStroke(2, stroke);
        return g;
    }
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setBackground(round(CARD, dp(c, 20), LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }
    public static TextView button(Context c, String s, int color, boolean filled) {
        TextView b = text(c, s, 15, filled ? Color.parseColor("#06140F") : color, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(filled ? color : CARD, dp(c, 14), filled ? 0 : LINE));
        b.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        b.setClickable(true);
        b.setForeground(c.getDrawable(android.R.drawable.list_selector_background));
        return b;
    }
    public static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    public static LinearLayout.LayoutParams weight(Context c, int hDp, int leftDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, hDp <= 0 ? -2 : dp(c, hDp), 1);
        l.leftMargin = dp(c, leftDp);
        return l;
    }
    public static View space(Context c, int dpH) { View v = new View(c); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpH))); return v; }
    public static View bar(Context c, float frac, int color) {
        LinearLayout track = new LinearLayout(c);
        track.setBackground(round(LINE, dp(c, 6), 0));
        View fill = new View(c);
        fill.setBackground(round(color, dp(c, 6), 0));
        track.setWeightSum(1f);
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, Math.max(0.001f, Math.min(1f, frac))));
        track.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 10)));
        return track;
    }
}
