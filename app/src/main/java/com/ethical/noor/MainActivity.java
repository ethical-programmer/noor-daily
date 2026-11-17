package com.ethical.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public class MainActivity extends Activity {
    Data d;
    int tab = 0;
    FrameLayout content;
    LinearLayout nav;
    String counterId = null;
    final Handler h = new Handler(Looper.getMainLooper());
    TextView focusText, focusBtn;
    static final int DLG = android.R.style.Theme_Material_Dialog_Alert;

    static final String[] ICONS = {"home", "water", "tasks", "quran", "tasbeeh", "report"};
    static final String[] NAMES = {"Today", "Water", "Tasks", "Quran", "Tasbeeh", "Report"};
    static final int[] TAB_C = {Ui.ACCENT, Ui.BLUE, Ui.ACCENT, Ui.GOLD, Ui.VIOLET, Ui.TEXT};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT); w.setNavigationBarColor(Ui.BG);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.addView(new Charts.Aurora(this), new FrameLayout.LayoutParams(-1, -1));
        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        nav = Ui.row(this);
        android.graphics.drawable.GradientDrawable nb = Ui.solid(0xF20D1017, Ui.dp(this, 26));
        nb.setStroke(Math.max(1, Ui.dp(this, 0.8f)), 0x1FFFFFFF);
        nav.setBackground(nb);
        nav.setElevation(Ui.dp(this, 16));
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        nl.setMargins(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 14));
        root.addView(nav, nl);
        setContentView(root);
        tab = getIntent().getIntExtra("tab", 0);
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); tab = mapTab(i.getIntExtra("tab", tab)); counterId = null; }
    static int mapTab(int t) { return t == 4 ? 5 : t == 3 ? 4 : t; }

    @Override protected void onResume() {
        super.onResume();
        if (d.hasPin() && !App.unlocked) { startActivity(new Intent(this, LockActivity.class)); overridePendingTransition(0, 0); }
        Alarms.ensure(this);
        show();
        h.postDelayed(new Runnable() { public void run() { updateFocus(); h.postDelayed(this, 1000); } }, 1000);
    }
    @Override protected void onPause() { super.onPause(); h.removeCallbacksAndMessages(null); }

    @Override public void onBackPressed() {
        if (counterId != null) { counterId = null; show(); return; }
        if (tab != 0) { tab = 0; show(); return; }
        super.onBackPressed();
    }

    // ================= shell =================
    void buildNav() {
        nav.removeAllViews();
        for (int i = 0; i < ICONS.length; i++) {
            final int k = i;
            boolean on = i == tab;
            LinearLayout it = Ui.col(this); it.setGravity(Gravity.CENTER);
            FrameLayout pill = new FrameLayout(this);
            if (on) pill.setBackground(Ui.solid((TAB_C[i] & 0x00FFFFFF) | 0x24000000, Ui.dp(this, 14)));
            ImageView ic = new ImageView(this); ic.setImageResource(Ui.icon(this, ICONS[i]));
            ic.setImageTintList(android.content.res.ColorStateList.valueOf(on ? TAB_C[i] : Ui.FAINT));
            pill.addView(ic, new FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
            it.addView(pill, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 32)));
            TextView nm = Ui.text(this, NAMES[i], 10.5f, on ? Ui.TEXT : Ui.FAINT, on); nm.setGravity(Gravity.CENTER);
            nm.setPadding(0, Ui.dp(this, 3), 0, 0);
            it.addView(nm);
            it.setOnClickListener(v -> { if (tab != k || counterId != null) { tab = k; counterId = null; buzz(8); show(); } });
            nav.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
            if (on) Ui.pop(pill);
        }
    }

    void show() {
        buildNav();
        content.removeAllViews();
        nav.setVisibility(counterId != null ? View.GONE : View.VISIBLE);
        if (counterId != null) { View cv = counter(); content.addView(cv); Ui.enter(cv, 0); return; }
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = Ui.col(this);
        int p = Ui.dp(this, 18); col.setPadding(p, Ui.dp(this, 48), p, Ui.dp(this, 124));
        sv.addView(col);
        switch (tab) {
            case 0: today(col); break;
            case 1: water(col); break;
            case 2: tasks(col); break;
            case 3: quran(col); break;
            case 4: tasbeeh(col); break;
            default: report(col);
        }
        content.addView(sv);
        for (int i = 0; i < col.getChildCount(); i++) Ui.enter(col.getChildAt(i), i);
    }

    void header(LinearLayout col, String title, String sub) {
        LinearLayout r = Ui.row(this);
        LinearLayout t = Ui.col(this);
        if (sub != null) { TextView s = Ui.label(this, sub); t.addView(s); }
        TextView tt = Ui.title(this, title, 30); tt.setPadding(0, Ui.dp(this, 2), 0, 0);
        t.addView(tt);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout gear = Ui.tile(this, R.drawable.ic_settings, Ui.TEXT, 44, false);
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        Ui.press(gear);
        r.addView(gear);
        col.addView(r);
    }

    TextView sectionTitle(String s) { TextView t = Ui.text(this, s, 16.5f, Ui.TEXT, true); return t; }

    // ================= TODAY =================
    void today(LinearLayout col) {
        String day = Data.today();
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hr < 12 ? "Good morning" : hr < 17 ? "Good afternoon" : "Good evening";
        header(col, "Assalamu Alaikum", greet + "  ·  " + new SimpleDateFormat("EEE d MMM", Locale.US).format(new Date()));

        LinearLayout hero = Ui.card(this); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring big = new Charts.Ring(this); big.stroke = 0.06f;
        int sc = d.score(day);
        big.set(sc / 100f, sc + "%", "today's score", Ui.G_SCORE);
        hero.addView(big, new LinearLayout.LayoutParams(Ui.dp(this, 200), Ui.dp(this, 200)));
        int st = d.streak();
        LinearLayout streak = Ui.row(this); streak.setBackground(Ui.glass(this, 18));
        streak.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 16), Ui.dp(this, 7));
        streak.addView(Ui.img(this, R.drawable.ic_flame, st > 0 ? Ui.GOLD : Ui.FAINT, 16));
        TextView stt = Ui.text(this, st > 0 ? "  " + st + " day streak" : "  Reach 70% to start a streak", 13.5f, st > 0 ? Ui.TEXT : Ui.MUTED, true);
        streak.addView(stt);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2); sl.topMargin = Ui.dp(this, 10); sl.bottomMargin = Ui.dp(this, 16);
        hero.addView(streak, sl);
        LinearLayout minis = Ui.row(this);
        minis.addView(mini(d.waterPct(day), "water", "Water", Ui.G_WATER, Ui.BLUE, 1), Ui.weight(this, 0, 0));
        minis.addView(mini(d.taskPct(day), "tasks", "Tasks", Ui.G_TASK, Ui.ACCENT, 2), Ui.weight(this, 0, 8));
        minis.addView(mini(d.quranPct(day), "quran", "Quran", Ui.G_QURAN, Ui.GOLD, 3), Ui.weight(this, 0, 8));
        minis.addView(mini(d.zikrPct(day), "tasbeeh", "Tasbeeh", Ui.G_ZIKR, Ui.VIOLET, 4), Ui.weight(this, 0, 8));
        hero.addView(minis, new LinearLayout.LayoutParams(-1, -2));
        col.addView(hero);

        long nxt = d.p.getLong("waterNextAt", 0);
        String wsub = d.water(day) >= d.waterGoal() ? "Goal reached, MashaAllah" : Data.fmt(d.water(day)) + " / " + Data.fmt(d.waterGoal()) + " ml" + (nxt > System.currentTimeMillis() && d.getBool("waterOn", true) ? "  ·  next " + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(nxt)) : "");
        TextView add = Ui.button(this, "+ " + d.glass() + " ml", true);
        add.setOnClickListener(v -> drink(d.glass()));
        col.addView(quick(R.drawable.ic_water, Ui.BLUE, "Water", wsub, add, v -> { tab = 1; show(); }));

        int pos = d.hifzPos(); int para = Math.min(30, pos / 8 + 1);
        TextView qb = Ui.button(this, d.quranDone(day) ? "Done" : "Open", !d.quranDone(day));
        qb.setOnClickListener(v -> { tab = 3; show(); });
        col.addView(quick(R.drawable.ic_quran, Ui.GOLD, "Quran hifz", d.quranDone(day) ? "Today's portion done  ·  Para " + para : "Para " + para + "  ·  " + d.quranToday(day) + "/" + d.hifzDaily() + " quarters today", qb, v -> { tab = 3; show(); }));

        focusBtn = Ui.button(this, "Start", false);
        focusBtn.setOnClickListener(v -> { if (d.p.getLong("focusEnd", 0) > System.currentTimeMillis()) Alarms.stopFocus(this); else pickFocus(); updateFocus(); });
        focusText = Ui.text(this, "", 13, Ui.MUTED, false);
        col.addView(quickWith(R.drawable.ic_focus, Ui.VIOLET, "Focus session", focusText, focusBtn, null));
        updateFocus();

        LinearLayout nx = Ui.card(this);
        nx.addView(sectionTitle("Up next"));
        int shown = 0;
        for (JSONObject t : d.tasks()) { if (d.taskDone(day, t) || shown >= 3) continue; shown++; nx.addView(taskRow(t, day, true)); }
        if (shown == 0) { TextView done = Ui.text(this, "All tasks done today", 15, Ui.ACCENT, true); done.setPadding(0, Ui.dp(this, 10), 0, 0); nx.addView(done); }
        LinearLayout all = Ui.row(this); all.setPadding(0, Ui.dp(this, 14), 0, 0);
        all.addView(Ui.text(this, "See all tasks", 14, Ui.MUTED, true));
        all.addView(Ui.img(this, R.drawable.ic_chevron, Ui.MUTED, 16));
        all.setOnClickListener(v -> { tab = 2; show(); });
        nx.addView(all);
        col.addView(nx);
    }

    LinearLayout quick(int icon, int color, String title, String sub, View right, View.OnClickListener tap) {
        return quickWith(icon, color, title, Ui.text(this, sub, 13, Ui.MUTED, false), right, tap);
    }
    LinearLayout quickWith(int icon, int color, String title, TextView sub, View right, View.OnClickListener tap) {
        LinearLayout c = Ui.card(this);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.tile(this, icon, color, 46, true));
        LinearLayout tx = Ui.col(this); tx.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        tx.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        tx.addView(sub);
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(right);
        c.addView(r);
        if (tap != null) { c.setOnClickListener(tap); Ui.press(c); }
        return c;
    }

    LinearLayout mini(float f, String icon, String label, int[] g, int color, int goTab) {
        LinearLayout b = Ui.col(this); b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setBackground(Ui.glass(this, 18)); b.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 10));
        FrameLayout fr = new FrameLayout(this);
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.1f;
        r.set(f, "", "", g);
        fr.addView(r, new FrameLayout.LayoutParams(-1, -1));
        ImageView ic = new ImageView(this); ic.setImageResource(Ui.icon(this, icon)); ic.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        fr.addView(ic, new FrameLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20), Gravity.CENTER));
        b.addView(fr, new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 58)));
        TextView l = Ui.text(this, Math.round(f * 100) + "%", 14, Ui.TEXT, true); l.setGravity(Gravity.CENTER); l.setPadding(0, Ui.dp(this, 6), 0, 0);
        b.addView(l);
        TextView n = Ui.text(this, label, 11, Ui.FAINT, false); n.setGravity(Gravity.CENTER); b.addView(n);
        b.setOnClickListener(x -> { tab = goTab; show(); });
        Ui.press(b);
        return b;
    }

    void pickFocus() {
        String[] o = {"15 minutes", "25 minutes (Pomodoro)", "45 minutes", "60 minutes"}; int[] m = {15, 25, 45, 60};
        new AlertDialog.Builder(this, DLG).setTitle("Focus for how long?").setItems(o, (x, i) -> { Alarms.startFocus(this, m[i]); Toast.makeText(this, "Focus started. Put your phone away.", Toast.LENGTH_SHORT).show(); updateFocus(); }).show();
    }
    void updateFocus() {
        if (focusText == null || focusBtn == null || tab != 0 || counterId != null) return;
        long left = d.p.getLong("focusEnd", 0) - System.currentTimeMillis();
        int done = d.getInt("focusDone_" + Data.today(), 0);
        if (left > 0) { focusText.setText(String.format(Locale.US, "%02d:%02d left  ·  stay off your phone", left / 60000, (left / 1000) % 60)); focusBtn.setText("Stop"); }
        else { focusText.setText(done > 0 ? done + " session" + (done > 1 ? "s" : "") + " done today" : "Deep work, no distractions"); focusBtn.setText("Start"); }
    }

    void drink(int ml) {
        boolean before = d.water(Data.today()) >= d.waterGoal();
        d.addWater(ml); buzz(18);
        Notifs.cancel(this, Notifs.N_WATER);
        Alarms.scheduleWater(this);
        if (!before && d.water(Data.today()) >= d.waterGoal()) Toast.makeText(this, "Water goal reached, MashaAllah", Toast.LENGTH_LONG).show();
        show();
    }

    // ================= WATER =================
    void water(LinearLayout col) {
        String day = Data.today();
        header(col, "Water", "Goal " + Data.fmt(d.waterGoal()) + " ml");
        LinearLayout c = Ui.card(this); c.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.065f;
        r.set(d.waterPct(day), Data.fmt(d.water(day)), "of " + Data.fmt(d.waterGoal()) + " ml", Ui.G_WATER);
        c.addView(r, new LinearLayout.LayoutParams(Ui.dp(this, 210), Ui.dp(this, 210)));
        int left = Math.max(0, d.waterGoal() - d.water(day));
        TextView lt = Ui.text(this, left == 0 ? "Goal reached" : Data.fmt(left) + " ml left  ·  about " + (int) Math.ceil(left / (double) d.glass()) + " glasses", 14, Ui.MUTED, false);
        lt.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
        c.addView(lt);
        LinearLayout btns = Ui.row(this);
        int[] am = {150, 250, 500};
        for (int i = 0; i < am.length; i++) {
            final int ml = am[i];
            TextView b = Ui.button(this, "+" + ml, ml == d.glass());
            b.setOnClickListener(v -> drink(ml));
            btns.addView(b, Ui.weight(this, 50, i == 0 ? 0 : 8));
        }
        c.addView(btns, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout undo = Ui.row(this); undo.setPadding(0, Ui.dp(this, 16), 0, 0);
        undo.addView(Ui.img(this, R.drawable.ic_undo, Ui.MUTED, 16));
        undo.addView(Ui.text(this, "  Undo last", 14, Ui.MUTED, true));
        undo.setOnClickListener(v -> { d.undoWater(); show(); });
        c.addView(undo);
        col.addView(c);

        LinearLayout hc = Ui.card(this);
        hc.addView(sectionTitle("Today, hour by hour"));
        float[] all = d.waterByHour(day);
        int from = d.wake() / 60, to = Math.max(from + 6, Math.min(23, d.sleep() / 60));
        int n = to - from + 1; float[] vals = new float[n]; String[] lab = new String[n];
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        for (int i = 0; i < n; i++) { int hh = from + i; vals[i] = all[hh]; lab[i] = (i % 3 == 0) ? (hh % 12 == 0 ? 12 : hh % 12) + (hh < 12 ? "a" : "p") : ""; }
        LinearLayout.LayoutParams b1 = Ui.top(this, 12); b1.height = Ui.dp(this, 140);
        hc.addView(new Charts.Bars(this).set(vals, lab, 0, Ui.G_WATER, now - from), b1);
        col.addView(hc);

        LinearLayout wc = Ui.card(this);
        wc.addView(sectionTitle("Last 7 days"));
        TextView gl = Ui.text(this, "Dashed line is your daily goal", 12.5f, Ui.FAINT, false); wc.addView(gl);
        float[] w = new float[7]; String[] wl = new String[7];
        for (int i = 0; i < 7; i++) { w[i] = d.water(Data.daysAgo(6 - i)); wl[i] = dayName(6 - i); }
        LinearLayout.LayoutParams b2 = Ui.top(this, 12); b2.height = Ui.dp(this, 150);
        wc.addView(new Charts.Bars(this).set(w, wl, d.waterGoal(), Ui.G_WATER, 6), b2);
        col.addView(wc);

        LinearLayout lg = Ui.card(this);
        lg.addView(sectionTitle("Log"));
        List<int[]> log = d.waterLog(day);
        if (log.isEmpty()) { TextView e = Ui.text(this, "Nothing logged yet today.", 14, Ui.MUTED, false); e.setPadding(0, Ui.dp(this, 8), 0, 0); lg.addView(e); }
        for (int i = log.size() - 1; i >= 0; i--) {
            LinearLayout rr = Ui.row(this); rr.setPadding(0, Ui.dp(this, 12), 0, 0);
            rr.addView(Ui.img(this, R.drawable.ic_water, Ui.BLUE, 16));
            rr.addView(Ui.text(this, "   " + log.get(i)[1] + " ml", 15, Ui.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            rr.addView(Ui.text(this, Data.hm(log.get(i)[0]), 13.5f, Ui.MUTED, false));
            lg.addView(rr);
        }
        col.addView(lg);
    }

    static String dayName(int ago) {
        if (ago == 0) return "Today";
        Calendar k = Calendar.getInstance(); k.add(Calendar.DAY_OF_YEAR, -ago);
        return new SimpleDateFormat("EEE", Locale.US).format(k.getTime());
    }

    // ================= TASKS =================
    void tasks(LinearLayout col) {
        String day = Data.today();
        header(col, "Daily tasks", d.tasksDone(day) + " of " + d.tasks().size() + " done");
        LinearLayout pc = Ui.card(this);
        LinearLayout pr = Ui.row(this);
        pr.addView(Ui.text(this, "Progress", 15, Ui.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        pr.addView(Ui.text(this, Math.round(d.tasksDone(day) * 100f / Math.max(1, d.tasks().size())) + "%", 15, Ui.ACCENT, true));
        pc.addView(pr);
        View bar = Ui.bar(this, d.tasksDone(day) / (float) Math.max(1, d.tasks().size()), Ui.G_TASK);
        ((LinearLayout.LayoutParams) bar.getLayoutParams()).topMargin = Ui.dp(this, 12);
        pc.addView(bar);
        col.addView(pc);
        for (JSONObject t : d.tasks()) col.addView(taskCard(t, day));
    }

    View taskCard(JSONObject t, String day) {
        LinearLayout c = Ui.card(this);
        c.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        c.addView(taskRow(t, day, false));
        if (d.taskDone(day, t)) c.setBackground(Ui.glassAccent(this, Ui.ACCENT, 24));
        c.setOnClickListener(v -> toggle(t, c));
        Ui.press(c);
        return c;
    }
    View taskRow(JSONObject t, String day, boolean small) {
        boolean done = d.taskDone(day, t);
        LinearLayout r = Ui.row(this); if (small) r.setPadding(0, Ui.dp(this, 12), 0, 0);
        r.addView(Ui.tile(this, Ui.icon(this, t.optString("emoji")), Ui.ACCENT, small ? 38 : 46, done));
        TextView nm = Ui.text(this, t.optString("name"), small ? 15 : 15.5f, done ? Ui.MUTED : Ui.TEXT, true);
        nm.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.check(this, done, Ui.ACCENT, small ? 30 : 34));
        if (small) r.setOnClickListener(v -> toggle(t, r));
        return r;
    }
    void toggle(JSONObject t, View v) {
        boolean done = d.taskDone(Data.today(), t);
        d.setTaskVal(t.optString("id"), done ? 0 : 1);
        buzz(done ? 8 : 30); Ui.pop(v);
        h.postDelayed(this::show, 260);
    }

    // ================= QURAN =================
    void quran(LinearLayout col) {
        String day = Data.today();
        int pos = d.hifzPos(), today = d.quranToday(day), daily = d.hifzDaily();
        int para = Math.min(30, pos / 8 + 1);
        header(col, "Quran hifz", daily == 4 ? "Half para a day" : daily == 8 ? "One para a day" : "Quarter para a day");

        LinearLayout hero = Ui.card(this); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView ar = Ui.text(this, "حِفْظُ ٱلْقُرْآنِ", 24, Ui.GOLD, true); ar.setGravity(Gravity.CENTER);
        hero.addView(ar);
        TextView pn = Ui.title(this, pos >= 240 ? "Hafiz, MashaAllah" : "Para " + para, 28); pn.setGravity(Gravity.CENTER); pn.setPadding(0, Ui.dp(this, 6), 0, 0);
        hero.addView(pn);
        if (pos < 240) { TextView nm = Ui.text(this, Data.PARA[para - 1] + "  ·  " + ((pos % 8) < 4 ? "first half" : "second half"), 14, Ui.MUTED, false); nm.setGravity(Gravity.CENTER); hero.addView(nm); }
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.07f;
        r.set(Math.min(1f, today / (float) daily), today + "/" + daily, "quarters today", Ui.G_QURAN);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(Ui.dp(this, 170), Ui.dp(this, 170)); rl.topMargin = Ui.dp(this, 14);
        hero.addView(r, rl);
        col.addView(hero);

        LinearLayout tq = Ui.card(this);
        tq.addView(sectionTitle("Today's portion"));
        tq.addView(Ui.text(this, "Tick each quarter after you've memorized it", 13, Ui.FAINT, false));
        int startPos = pos - today;
        for (int i = 0; i < daily; i++) {
            int q = startPos + i; if (q >= 240) break;
            final int k = i;
            boolean done = i < today;
            LinearLayout row = Ui.row(this);
            row.setBackground(done ? Ui.glassAccent(this, Ui.GOLD, 16) : Ui.glass(this, 16));
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            TextView num = Ui.text(this, String.valueOf(i + 1), 14, done ? Ui.GOLD : Ui.MUTED, true); num.setGravity(Gravity.CENTER);
            num.setBackground(Ui.glass(this, 12));
            row.addView(num, new LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30)));
            LinearLayout tx = Ui.col(this); tx.setPadding(Ui.dp(this, 12), 0, 0, 0);
            tx.addView(Ui.text(this, "Para " + (q / 8 + 1) + "  ·  Quarter " + (q % 8 + 1) + " of 8", 15, Ui.TEXT, true));
            tx.addView(Ui.text(this, done ? "Memorized" : (i == today ? "Up next" : "Later today"), 12.5f, done ? Ui.GOLD : Ui.FAINT, false));
            row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(Ui.check(this, done, Ui.GOLD, 30));
            row.setOnClickListener(v -> {
                int t2 = d.quranToday(Data.today());
                if (k == t2) { d.hifzStep(1); buzz(35); }
                else if (k == t2 - 1) { d.hifzStep(-1); buzz(8); }
                else { Toast.makeText(this, "Tick the quarters in order", Toast.LENGTH_SHORT).show(); return; }
                Ui.pop(v);
                if (d.quranDone(Data.today()) && k == daily - 1) Toast.makeText(this, "Today's portion complete, MashaAllah", Toast.LENGTH_LONG).show();
                h.postDelayed(this::show, 260);
            });
            Ui.press(row);
            tq.addView(row, Ui.top(this, 10));
        }
        if (today < daily && pos < 240) {
            TextView allBtn = Ui.button(this, "Mark today's portion done", true);
            allBtn.setOnClickListener(v -> { int need = d.hifzDaily() - d.quranToday(Data.today()); for (int j = 0; j < need; j++) d.hifzStep(1); buzz(50); Toast.makeText(this, "Today's portion complete, MashaAllah", Toast.LENGTH_LONG).show(); show(); });
            tq.addView(allBtn, Ui.top(this, 14));
        }
        col.addView(tq);

        LinearLayout ov = Ui.card(this);
        LinearLayout orow = Ui.row(this);
        Charts.Ring tr = new Charts.Ring(this); tr.stroke = 0.09f;
        tr.set(pos / 240f, Math.round(pos / 2.4f) + "%", "", Ui.G_QURAN);
        orow.addView(tr, new LinearLayout.LayoutParams(Ui.dp(this, 88), Ui.dp(this, 88)));
        LinearLayout otx = Ui.col(this); otx.setPadding(Ui.dp(this, 16), 0, 0, 0);
        otx.addView(Ui.text(this, "Whole Quran", 16.5f, Ui.TEXT, true));
        otx.addView(Ui.text(this, (pos / 8) + " paras and " + (pos % 8) + " quarters memorized", 13.5f, Ui.MUTED, false));
        int daysLeft = (int) Math.ceil((240 - pos) / (double) daily);
        Calendar fin = Calendar.getInstance(); fin.add(Calendar.DAY_OF_YEAR, daysLeft);
        otx.addView(Ui.text(this, pos >= 240 ? "Completed" : "At this pace: " + daysLeft + " days  ·  " + new SimpleDateFormat("MMM yyyy", Locale.US).format(fin.getTime()), 13.5f, Ui.GOLD, true));
        orow.addView(otx, new LinearLayout.LayoutParams(0, -2, 1));
        ov.addView(orow);

        GridLayout g = new GridLayout(this); g.setColumnCount(6);
        int cell = (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 36 + 36 + 6 * 8)) / 6;
        for (int i = 0; i < 30; i++) {
            int got = Math.max(0, Math.min(8, pos - i * 8));
            TextView t = Ui.text(this, String.valueOf(i + 1), 13.5f, got == 8 ? Ui.INK : got > 0 ? Ui.GOLD : Ui.MUTED, true); t.setGravity(Gravity.CENTER);
            if (got == 8) t.setBackground(Ui.grad(Ui.G_QURAN, Ui.dp(this, 12)));
            else if (got > 0) t.setBackground(Ui.glassAccent(this, Ui.GOLD, 12));
            else t.setBackground(Ui.glass(this, 12));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(); lp.width = cell; lp.height = cell; lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
            g.addView(t, lp);
            t.setAlpha(0f); t.animate().alpha(1f).setStartDelay(200 + i * 15L).setDuration(260).start();
        }
        ov.addView(g, Ui.top(this, 16));
        col.addView(ov);

        LinearLayout wk = Ui.card(this);
        wk.addView(sectionTitle("Last 7 days"));
        float[] vals = new float[7]; String[] l = new String[7];
        for (int i = 0; i < 7; i++) { vals[i] = d.quranToday(Data.daysAgo(6 - i)); l[i] = dayName(6 - i); }
        LinearLayout.LayoutParams bl = Ui.top(this, 12); bl.height = Ui.dp(this, 130);
        wk.addView(new Charts.Bars(this).set(vals, l, daily, Ui.G_QURAN, 6), bl);
        col.addView(wk);
    }

    // ================= TASBEEH =================
    void tasbeeh(LinearLayout col) {
        String day = Data.today();
        header(col, "Tasbeeh", d.zikrsDone(day) + " of " + d.zikrs().size() + " completed");
        for (JSONObject z : d.zikrs()) {
            String id = z.optString("id");
            int cnt = d.zikrCount(day, id), tg = Math.max(1, z.optInt("target", 33));
            boolean done = cnt >= tg;
            LinearLayout c = Ui.card(this);
            if (done) c.setBackground(Ui.glassAccent(this, Ui.VIOLET, 24));
            LinearLayout r = Ui.row(this);
            LinearLayout tx = Ui.col(this);
            tx.addView(Ui.text(this, z.optString("ar"), 22, Color.parseColor("#E4DBFF"), true));
            tx.addView(Ui.text(this, z.optString("name"), 15, Ui.TEXT, true));
            tx.addView(Ui.text(this, cnt + " / " + tg + (done ? "  ·  completed" : ""), 13, done ? Ui.VIOLET : Ui.FAINT, false));
            r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.09f;
            ring.set(cnt / (float) tg, String.valueOf(cnt), "", Ui.G_ZIKR);
            r.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 66), Ui.dp(this, 66)));
            c.addView(r);
            c.setOnClickListener(v -> { counterId = id; show(); });
            Ui.press(c);
            col.addView(c);
        }
    }

    View counter() {
        JSONObject found = null;
        for (JSONObject o : d.zikrs()) if (o.optString("id").equals(counterId)) found = o;
        if (found == null) { counterId = null; return new View(this); }
        final JSONObject z = found; final String id = counterId;
        final int tg = Math.max(1, z.optInt("target", 33));
        LinearLayout col = Ui.col(this); col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, 48), Ui.dp(this, 20), Ui.dp(this, 30));
        LinearLayout top = Ui.row(this);
        FrameLayout back = Ui.tile(this, R.drawable.ic_back, Ui.TEXT, 44, false); back.setOnClickListener(v -> { counterId = null; show(); }); Ui.press(back);
        top.addView(back);
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        FrameLayout reset = Ui.tile(this, R.drawable.ic_reset, Ui.TEXT, 44, false); Ui.press(reset);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this, DLG).setTitle("Reset today's count?").setPositiveButton("Reset", (x, y) -> { d.setZikr(id, 0); show(); }).setNegativeButton("Cancel", null).show());
        top.addView(reset);
        col.addView(top, new LinearLayout.LayoutParams(-1, -2));
        TextView ar = Ui.text(this, z.optString("ar"), 34, Color.parseColor("#E4DBFF"), true); ar.setGravity(Gravity.CENTER); ar.setPadding(0, Ui.dp(this, 34), 0, Ui.dp(this, 4));
        col.addView(ar, new LinearLayout.LayoutParams(-1, -2));
        TextView nm = Ui.text(this, z.optString("name"), 16, Ui.MUTED, false); nm.setGravity(Gravity.CENTER);
        col.addView(nm, new LinearLayout.LayoutParams(-1, -2));
        Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.045f;
        int cnt = d.zikrCount(Data.today(), id);
        ring.set(cnt / (float) tg, String.valueOf(cnt), "of " + tg, Ui.G_ZIKR);
        FrameLayout tap = new FrameLayout(this);
        tap.setBackground(Ui.glass(this, 200));
        int sz = Ui.dp(this, 290);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(sz, sz); tl.topMargin = Ui.dp(this, 36);
        tap.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        col.addView(tap, tl);
        TextView hint = Ui.text(this, cnt >= tg ? "MashaAllah, target complete" : "Tap the circle to count", 14, cnt >= tg ? Ui.VIOLET : Ui.FAINT, false);
        hint.setGravity(Gravity.CENTER); hint.setPadding(0, Ui.dp(this, 24), 0, 0);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        tap.setOnClickListener(v -> {
            int c = d.zikrCount(Data.today(), id) + 1;
            d.setZikr(id, c);
            ring.set(c / (float) tg, String.valueOf(c), "of " + tg, Ui.G_ZIKR);
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();
            if (c == tg) { buzz(420); hint.setText("MashaAllah, target complete"); hint.setTextColor(Ui.VIOLET); Ui.pop(hint); }
            else buzz(c % 33 == 0 ? 110 : 16);
        });
        return col;
    }

    // ================= REPORT =================
    void report(LinearLayout col) {
        String day = Data.today();
        header(col, "Report", "Your progress");
        LinearLayout c = Ui.card(this);
        c.addView(sectionTitle("Last 7 days"));
        float[] vals = new float[7]; String[] l = new String[7];
        for (int i = 0; i < 7; i++) { vals[i] = d.score(Data.daysAgo(6 - i)); l[i] = dayName(6 - i); }
        LinearLayout.LayoutParams bl = Ui.top(this, 12); bl.height = Ui.dp(this, 160);
        c.addView(new Charts.Bars(this).set(vals, l, 70, Ui.G_TASK, 6), bl);
        LinearLayout s = Ui.row(this); s.setPadding(0, Ui.dp(this, 10), 0, 0);
        s.addView(Ui.img(this, R.drawable.ic_flame, Ui.GOLD, 15));
        s.addView(Ui.text(this, "  " + d.streak() + " day streak  ·  dashed line = 70%", 13, Ui.MUTED, false));
        c.addView(s);
        col.addView(c);

        LinearLayout t = Ui.card(this);
        t.addView(sectionTitle("Today  ·  " + d.score(day) + "%"));
        t.addView(line("water", Ui.BLUE, "Water", Data.fmt(d.water(day)) + " / " + Data.fmt(d.waterGoal()) + " ml", d.water(day) >= d.waterGoal()));
        t.addView(line("quran", Ui.GOLD, "Quran hifz", d.quranToday(day) + "/" + d.hifzDaily() + " quarters", d.quranDone(day)));
        for (JSONObject k : d.tasks()) t.addView(line(k.optString("emoji"), Ui.ACCENT, k.optString("name"), d.taskDone(day, k) ? "done" : "not yet", d.taskDone(day, k)));
        for (JSONObject z : d.zikrs()) t.addView(line("tasbeeh", Ui.VIOLET, z.optString("name"), d.zikrCount(day, z.optString("id")) + "/" + z.optInt("target"), d.zikrDone(day, z)));
        col.addView(t);

        LinearLayout hc = Ui.card(this);
        hc.addView(sectionTitle("History"));
        for (int i = 1; i <= 14; i++) {
            String k = Data.daysAgo(i); int sc = d.score(k);
            LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 12), 0, 0);
            Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i);
            r.addView(Ui.text(this, new SimpleDateFormat("EEE, d MMM", Locale.US).format(cal.getTime()), 14.5f, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
            View b = Ui.bar(this, sc / 100f, Ui.G_TASK);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(this, 90), Ui.dp(this, 6)); bp.rightMargin = Ui.dp(this, 12);
            r.addView(b, bp);
            TextView sct = Ui.text(this, sc + "%", 14, sc >= 70 ? Ui.ACCENT : sc >= 40 ? Ui.GOLD : Ui.RED, true);
            sct.setMinWidth(Ui.dp(this, 40)); sct.setGravity(Gravity.END);
            r.addView(sct);
            hc.addView(r);
        }
        col.addView(hc);
    }

    LinearLayout line(String icon, int color, String name, String val, boolean ok) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 12), 0, 0);
        r.addView(Ui.img(this, Ui.icon(this, icon), ok ? color : Ui.FAINT, 17));
        r.addView(Ui.text(this, "   " + name, 14.5f, ok ? Ui.TEXT : Ui.MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.text(this, val, 13.5f, ok ? color : Ui.FAINT, true));
        return r;
    }

    void buzz(int ms) {
        try { Vibrator v = getSystemService(Vibrator.class); if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)); } catch (Exception ignored) {}
    }
}
