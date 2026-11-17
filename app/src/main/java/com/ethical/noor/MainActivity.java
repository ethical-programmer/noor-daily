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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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

    static final String[] ICONS = {"🏠", "💧", "✅", "📖", "📿", "📊"};
    static final String[] NAMES = {"Today", "Water", "Tasks", "Quran", "Tasbeeh", "Report"};
    static final int[][] TAB_G = {Ui.G_SCORE, Ui.G_WATER, Ui.G_TASK, Ui.G_QURAN, Ui.G_ZIKR, Ui.G_REPORT};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT); w.setNavigationBarColor(Ui.BG);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        FrameLayout root = new FrameLayout(this);
        root.addView(new Charts.Aurora(this), new FrameLayout.LayoutParams(-1, -1));
        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        nav = Ui.row(this);
        nav.setBackground(Ui.glass(this, 30));
        nav.setElevation(Ui.dp(this, 12));
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        nl.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 14));
        root.addView(nav, nl);
        setContentView(root);
        tab = getIntent().getIntExtra("tab", 0);
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); tab = mapTab(i.getIntExtra("tab", tab)); counterId = null; }
    static int mapTab(int t) { return t == 4 ? 5 : t == 3 ? 4 : t; } // notification ids from older layout

    @Override protected void onResume() {
        super.onResume();
        if (d.hasPin() && !App.unlocked) { startActivity(new Intent(this, LockActivity.class)); overridePendingTransition(0, 0); }
        Alarms.ensure(this);
        show(false);
        h.postDelayed(new Runnable() { public void run() { updateFocus(); h.postDelayed(this, 1000); } }, 1000);
    }
    @Override protected void onPause() { super.onPause(); h.removeCallbacksAndMessages(null); }

    @Override public void onBackPressed() {
        if (counterId != null) { counterId = null; show(true); return; }
        if (tab != 0) { tab = 0; show(true); return; }
        super.onBackPressed();
    }

    // ================= shell =================
    void buildNav() {
        nav.removeAllViews();
        for (int i = 0; i < ICONS.length; i++) {
            final int k = i;
            LinearLayout it = Ui.col(this); it.setGravity(Gravity.CENTER);
            TextView ic = Ui.text(this, ICONS[i], 19, Color.WHITE, false); ic.setGravity(Gravity.CENTER);
            int s = Ui.dp(this, 40);
            if (i == tab) { ic.setBackground(Ui.grad(TAB_G[i], s / 2f)); ic.setElevation(Ui.dp(this, 6)); }
            else ic.setAlpha(0.55f);
            it.addView(ic, new LinearLayout.LayoutParams(s, s));
            TextView nm = Ui.text(this, NAMES[i], 10.5f, i == tab ? Ui.TEXT : Ui.MUTED, i == tab); nm.setGravity(Gravity.CENTER);
            nm.setPadding(0, Ui.dp(this, 3), 0, 0);
            it.addView(nm);
            it.setOnClickListener(v -> { if (tab != k || counterId != null) { tab = k; counterId = null; buzz(10); show(true); } });
            Ui.press(it);
            nav.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
            if (i == tab) Ui.pop(ic);
        }
    }

    void show(boolean animate) {
        buildNav();
        content.removeAllViews();
        nav.setVisibility(counterId != null ? View.GONE : View.VISIBLE);
        if (counterId != null) { View cv = counter(); content.addView(cv); Ui.enter(cv, 0); return; }
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = Ui.col(this);
        int p = Ui.dp(this, 18); col.setPadding(p, Ui.dp(this, 46), p, Ui.dp(this, 120));
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
        if (animate || true) for (int i = 0; i < col.getChildCount(); i++) Ui.enter(col.getChildAt(i), i);
    }

    void header(LinearLayout col, String title, String sub) {
        LinearLayout r = Ui.row(this);
        LinearLayout t = Ui.col(this);
        t.addView(Ui.title(this, title, 30));
        if (sub != null) { TextView s = Ui.text(this, sub, 14, Ui.MUTED, false); s.setPadding(0, Ui.dp(this, 2), 0, 0); t.addView(s); }
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView gear = Ui.text(this, "⚙️", 20, Ui.TEXT, false); gear.setGravity(Gravity.CENTER);
        gear.setBackground(Ui.glass(this, 16));
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        Ui.press(gear);
        r.addView(gear, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));
        col.addView(r);
    }

    // ================= TODAY =================
    void today(LinearLayout col) {
        String day = Data.today();
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hr < 12 ? "Good morning ☀️" : hr < 17 ? "Good afternoon 🌤️" : "Good evening 🌙";
        header(col, "Assalamu Alaikum", greet + "  ·  " + new SimpleDateFormat("EEE, d MMM", Locale.US).format(new Date()));

        LinearLayout hero = Ui.card(this); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring big = new Charts.Ring(this); big.stroke = 0.075f;
        int sc = d.score(day);
        big.set(sc / 100f, sc + "%", "today's score", Ui.G_SCORE);
        hero.addView(big, new LinearLayout.LayoutParams(Ui.dp(this, 210), Ui.dp(this, 210)));
        int st = d.streak();
        TextView streak = Ui.text(this, st > 0 ? "🔥  " + st + " day streak" : "Reach 70% today to start a streak", 14, st > 0 ? Ui.GOLD : Ui.MUTED, true);
        streak.setGravity(Gravity.CENTER);
        streak.setBackground(Ui.glass(this, 20)); streak.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2); sl.topMargin = Ui.dp(this, 8); sl.bottomMargin = Ui.dp(this, 14);
        hero.addView(streak, sl);
        LinearLayout minis = Ui.row(this);
        minis.addView(mini(d.waterPct(day), "💧", "Water", Ui.G_WATER, 1), Ui.weight(this, 0, 0));
        minis.addView(mini(d.taskPct(day), "✅", "Tasks", Ui.G_TASK, 2), Ui.weight(this, 0, 6));
        minis.addView(mini(d.quranPct(day), "📖", "Quran", Ui.G_QURAN, 3), Ui.weight(this, 0, 6));
        minis.addView(mini(d.zikrPct(day), "📿", "Tasbeeh", Ui.G_ZIKR, 4), Ui.weight(this, 0, 6));
        hero.addView(minis, new LinearLayout.LayoutParams(-1, -2));
        col.addView(hero);

        // water quick
        long nxt = d.p.getLong("waterNextAt", 0);
        String wsub = d.water(day) >= d.waterGoal() ? "Goal reached, MashaAllah!" : Data.fmt(d.water(day)) + " / " + Data.fmt(d.waterGoal()) + " ml" + (nxt > System.currentTimeMillis() && d.getBool("waterOn", true) ? "  ·  next " + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(nxt)) : "");
        TextView add = Ui.button(this, "+ " + d.glass(), Ui.G_WATER);
        add.setOnClickListener(v -> drink(d.glass()));
        col.addView(quick("💧", "Water", wsub, Ui.G_WATER, add, v -> { tab = 1; show(true); }));

        // quran quick
        int pos = d.hifzPos(); int para = Math.min(30, pos / 8 + 1);
        TextView qb = Ui.button(this, d.quranDone(day) ? "✓" : "Open", Ui.G_QURAN);
        qb.setOnClickListener(v -> { tab = 3; show(true); });
        col.addView(quick("📖", "Quran hifz", d.quranDone(day) ? "Today's portion done ✓ · Para " + para : "Para " + para + "  ·  " + d.quranToday(day) + "/" + d.hifzDaily() + " quarters today", Ui.G_QURAN, qb, v -> { tab = 3; show(true); }));

        // focus
        focusBtn = Ui.button(this, "Start", Ui.G_FOCUS);
        focusBtn.setOnClickListener(v -> { if (d.p.getLong("focusEnd", 0) > System.currentTimeMillis()) Alarms.stopFocus(this); else pickFocus(); updateFocus(); });
        LinearLayout fq = quick("🎯", "Focus session", "", Ui.G_FOCUS, focusBtn, null);
        focusText = (TextView) ((LinearLayout) ((LinearLayout) fq.getChildAt(0)).getChildAt(1)).getChildAt(1);
        col.addView(fq);
        updateFocus();

        // up next
        LinearLayout nx = Ui.card(this);
        nx.addView(Ui.text(this, "Up next", 18, Ui.TEXT, true));
        int shown = 0;
        for (JSONObject t : d.tasks()) { if (d.taskDone(day, t) || shown >= 3) continue; shown++; nx.addView(taskRow(t, day)); }
        if (shown == 0) { TextView done = Ui.text(this, "All tasks done today 🎉", 15, Ui.GREEN, true); done.setPadding(0, Ui.dp(this, 10), 0, 0); nx.addView(done); }
        TextView all = Ui.text(this, "See all tasks  →", 14, Ui.GREEN, true); all.setPadding(0, Ui.dp(this, 14), 0, 0);
        all.setOnClickListener(v -> { tab = 2; show(true); });
        nx.addView(all);
        col.addView(nx);
    }

    LinearLayout quick(String emoji, String title, String sub, int[] g, View right, View.OnClickListener tap) {
        LinearLayout c = Ui.card(this);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.tile(this, emoji, g, 48));
        LinearLayout tx = Ui.col(this); tx.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        tx.addView(Ui.text(this, title, 16.5f, Ui.TEXT, true));
        tx.addView(Ui.text(this, sub, 13, Ui.MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(right);
        c.addView(r);
        if (tap != null) { c.setOnClickListener(tap); Ui.press(c); }
        return c;
    }

    LinearLayout mini(float f, String emoji, String label, int[] g, int goTab) {
        LinearLayout b = Ui.col(this); b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setBackground(Ui.glass(this, 20)); b.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.11f;
        r.set(f, Math.round(f * 100) + "%", "", g);
        b.addView(r, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62)));
        TextView l = Ui.text(this, emoji + " " + label, 11.5f, Ui.TEXT, true); l.setGravity(Gravity.CENTER); l.setPadding(0, Ui.dp(this, 4), 0, 0);
        b.addView(l);
        b.setOnClickListener(x -> { tab = goTab; show(true); });
        Ui.press(b);
        return b;
    }

    void pickFocus() {
        String[] o = {"15 min", "25 min (Pomodoro)", "45 min", "60 min"}; int[] m = {15, 25, 45, 60};
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Focus for how long?").setItems(o, (x, i) -> { Alarms.startFocus(this, m[i]); Toast.makeText(this, "Focus started. Put your phone away 📵", Toast.LENGTH_SHORT).show(); updateFocus(); }).show();
    }
    void updateFocus() {
        if (focusText == null || focusBtn == null || tab != 0 || counterId != null) return;
        long left = d.p.getLong("focusEnd", 0) - System.currentTimeMillis();
        int done = d.getInt("focusDone_" + Data.today(), 0);
        if (left > 0) { focusText.setText(String.format(Locale.US, "%02d:%02d left · stay off your phone", left / 60000, (left / 1000) % 60)); focusBtn.setText("Stop"); }
        else { focusText.setText(done > 0 ? done + " session" + (done > 1 ? "s" : "") + " done today" : "Deep work, no distractions"); focusBtn.setText("Start"); }
    }

    void drink(int ml) {
        boolean before = d.water(Data.today()) >= d.waterGoal();
        d.addWater(ml); buzz(20);
        Notifs.cancel(this, Notifs.N_WATER);
        Alarms.scheduleWater(this);
        if (!before && d.water(Data.today()) >= d.waterGoal()) Toast.makeText(this, "Water goal reached, MashaAllah! 💧", Toast.LENGTH_LONG).show();
        show(false);
    }

    // ================= WATER =================
    void water(LinearLayout col) {
        String day = Data.today();
        header(col, "Water", "Goal " + Data.fmt(d.waterGoal()) + " ml · from your age, height & weight");
        LinearLayout c = Ui.card(this); c.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.08f;
        r.set(d.waterPct(day), Data.fmt(d.water(day)), "of " + Data.fmt(d.waterGoal()) + " ml", Ui.G_WATER);
        c.addView(r, new LinearLayout.LayoutParams(Ui.dp(this, 220), Ui.dp(this, 220)));
        int left = Math.max(0, d.waterGoal() - d.water(day));
        TextView lt = Ui.text(this, left == 0 ? "Goal reached 🎉" : Data.fmt(left) + " ml left · about " + (int) Math.ceil(left / (double) d.glass()) + " glasses", 14.5f, Ui.MUTED, false);
        lt.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 14));
        c.addView(lt);
        LinearLayout btns = Ui.row(this);
        int[] am = {150, 250, 500};
        for (int i = 0; i < am.length; i++) {
            final int ml = am[i];
            TextView b = Ui.button(this, "+" + ml + " ml", ml == d.glass() ? Ui.G_WATER : null);
            b.setOnClickListener(v -> drink(ml));
            btns.addView(b, Ui.weight(this, 52, i == 0 ? 0 : 8));
        }
        c.addView(btns, new LinearLayout.LayoutParams(-1, -2));
        TextView undo = Ui.text(this, "↶  Undo last", 14, Ui.BLUE, true); undo.setPadding(0, Ui.dp(this, 14), 0, 0);
        undo.setOnClickListener(v -> { d.undoWater(); show(false); });
        c.addView(undo);
        col.addView(c);

        LinearLayout hc = Ui.card(this);
        hc.addView(Ui.text(this, "Today, hour by hour", 16.5f, Ui.TEXT, true));
        float[] all = d.waterByHour(day);
        int from = d.wake() / 60, to = Math.max(from + 6, Math.min(23, d.sleep() / 60));
        int n = to - from + 1; float[] v = new float[n]; String[] lab = new String[n];
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        for (int i = 0; i < n; i++) { int hh = from + i; v[i] = all[hh]; lab[i] = (i % 3 == 0) ? (hh % 12 == 0 ? 12 : hh % 12) + (hh < 12 ? "a" : "p") : ""; }
        hc.addView(new Charts.Bars(this).set(v, lab, 0, Ui.G_WATER, now - from), Ui.top(this, 10));
        ((LinearLayout.LayoutParams) hc.getChildAt(1).getLayoutParams()).height = Ui.dp(this, 150);
        col.addView(hc);

        LinearLayout wc = Ui.card(this);
        wc.addView(Ui.text(this, "Last 7 days", 16.5f, Ui.TEXT, true));
        wc.addView(Ui.text(this, "Dashed line = your daily goal", 12.5f, Ui.MUTED, false));
        float[] w = new float[7]; String[] wl = new String[7];
        for (int i = 0; i < 7; i++) { w[i] = d.water(Data.daysAgo(6 - i)); wl[i] = dayName(6 - i); }
        LinearLayout.LayoutParams bl = Ui.top(this, 10); bl.height = Ui.dp(this, 160);
        wc.addView(new Charts.Bars(this).set(w, wl, d.waterGoal(), Ui.G_WATER, 6), bl);
        col.addView(wc);

        LinearLayout lg = Ui.card(this);
        lg.addView(Ui.text(this, "Log", 16.5f, Ui.TEXT, true));
        List<int[]> log = d.waterLog(day);
        if (log.isEmpty()) { TextView e = Ui.text(this, "Nothing logged yet today.", 14, Ui.MUTED, false); e.setPadding(0, Ui.dp(this, 8), 0, 0); lg.addView(e); }
        for (int i = log.size() - 1; i >= 0; i--) {
            LinearLayout rr = Ui.row(this); rr.setPadding(0, Ui.dp(this, 10), 0, 0);
            rr.addView(Ui.text(this, "💧  " + log.get(i)[1] + " ml", 15, Ui.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
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
        header(col, "Daily tasks", d.tasksDone(day) + " of " + d.tasks().size() + " done today");
        LinearLayout pc = Ui.card(this);
        LinearLayout pr = Ui.row(this);
        pr.addView(Ui.text(this, "Progress", 15, Ui.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        pr.addView(Ui.text(this, Math.round(d.taskPct(day) * 100) + "%", 15, Ui.GREEN, true));
        pc.addView(pr);
        View bar = Ui.bar(this, d.tasksDone(day) / (float) Math.max(1, d.tasks().size()), Ui.G_TASK);
        ((LinearLayout.LayoutParams) bar.getLayoutParams()).topMargin = Ui.dp(this, 10);
        pc.addView(bar);
        col.addView(pc);
        for (JSONObject t : d.tasks()) col.addView(taskCard(t, day));
    }

    /** big tappable task card: one tap = done */
    View taskCard(JSONObject t, String day) {
        String id = t.optString("id");
        boolean done = d.taskDone(day, t);
        LinearLayout c = Ui.card(this);
        if (done) c.setBackground(Ui.tint(this, Ui.G_TASK, 26, 0x40));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.tile(this, t.optString("emoji", "⭐"), done ? Ui.G_TASK : new int[]{0x33FFFFFF, 0x14FFFFFF}, 50));
        TextView nm = Ui.text(this, t.optString("name"), 16, Ui.TEXT, true);
        nm.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        if (done) nm.setAlpha(0.85f);
        r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chk = Ui.text(this, done ? "✓" : "", 22, Color.parseColor("#062014"), true);
        chk.setGravity(Gravity.CENTER);
        chk.setBackground(done ? Ui.grad(Ui.G_TASK, Ui.dp(this, 18)) : Ui.glass(this, 18));
        r.addView(chk, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        c.addView(r);
        c.setOnClickListener(v -> {
            d.setTaskVal(id, done ? 0 : 1); buzz(done ? 10 : 35);
            chk.setText(done ? "" : "✓"); chk.setBackground(done ? Ui.glass(this, 18) : Ui.grad(Ui.G_TASK, Ui.dp(this, 18)));
            Ui.pop(chk);
            h.postDelayed(() -> show(false), 380);
        });
        Ui.press(c);
        return c;
    }
    View taskRow(JSONObject t, String day) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 12), 0, 0);
        r.addView(Ui.tile(this, t.optString("emoji", "⭐"), new int[]{0x33FFFFFF, 0x14FFFFFF}, 38));
        TextView nm = Ui.text(this, t.optString("name"), 15, Ui.TEXT, false); nm.setPadding(Ui.dp(this, 12), 0, 0, 0);
        r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chk = Ui.text(this, "", 18, Color.parseColor("#062014"), true); chk.setGravity(Gravity.CENTER);
        chk.setBackground(Ui.glass(this, 16));
        r.addView(chk, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
        r.setOnClickListener(v -> { d.setTaskVal(t.optString("id"), 1); buzz(35); chk.setText("✓"); chk.setBackground(Ui.grad(Ui.G_TASK, Ui.dp(this, 16))); Ui.pop(chk); h.postDelayed(() -> show(false), 380); });
        return r;
    }

    // ================= QURAN =================
    void quran(LinearLayout col) {
        String day = Data.today();
        int pos = d.hifzPos(), today = d.quranToday(day), daily = d.hifzDaily();
        int para = Math.min(30, pos / 8 + 1);
        header(col, "Quran hifz", daily == 4 ? "Half para a day" : daily == 8 ? "One para a day" : "Quarter para a day");

        LinearLayout hero = Ui.card(this); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setBackground(Ui.tint(this, Ui.G_QURAN, 26, 0x2A));
        TextView ar = Ui.text(this, "حِفْظُ ٱلْقُرْآنِ", 26, Ui.GOLD, true); ar.setGravity(Gravity.CENTER);
        hero.addView(ar);
        TextView pn = Ui.title(this, pos >= 240 ? "Hafiz, MashaAllah! 🎉" : "Para " + para, 28); pn.setGravity(Gravity.CENTER); pn.setPadding(0, Ui.dp(this, 4), 0, 0);
        hero.addView(pn);
        if (pos < 240) { TextView nm = Ui.text(this, Data.PARA[para - 1] + "  ·  " + ((pos % 8) < 4 ? "1st half" : "2nd half"), 14.5f, Ui.MUTED, false); nm.setGravity(Gravity.CENTER); hero.addView(nm); }
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.08f;
        r.set(Math.min(1f, today / (float) daily), today + "/" + daily, "quarters today", Ui.G_QURAN);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(Ui.dp(this, 180), Ui.dp(this, 180)); rl.topMargin = Ui.dp(this, 12);
        hero.addView(r, rl);
        col.addView(hero);

        // today's quarters
        LinearLayout tq = Ui.card(this);
        tq.addView(Ui.text(this, "Today's portion", 17, Ui.TEXT, true));
        tq.addView(Ui.text(this, "Tick each quarter after you've memorized it", 13, Ui.MUTED, false));
        int startPos = pos - today;
        for (int i = 0; i < daily; i++) {
            int q = startPos + i; if (q >= 240) break;
            final int k = i;
            boolean done = i < today;
            LinearLayout row = Ui.row(this);
            row.setBackground(done ? Ui.tint(this, Ui.G_QURAN, 18, 0x38) : Ui.glass(this, 18));
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            TextView num = Ui.text(this, String.valueOf(i + 1), 15, Color.parseColor("#1A1300"), true); num.setGravity(Gravity.CENTER);
            num.setBackground(done ? Ui.grad(Ui.G_QURAN, Ui.dp(this, 14)) : Ui.glass(this, 14));
            if (!done) num.setTextColor(Ui.TEXT);
            row.addView(num, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));
            LinearLayout tx = Ui.col(this); tx.setPadding(Ui.dp(this, 12), 0, 0, 0);
            tx.addView(Ui.text(this, "Para " + (q / 8 + 1) + "  ·  Quarter " + (q % 8 + 1) + " of 8", 15, Ui.TEXT, true));
            tx.addView(Ui.text(this, done ? "Memorized ✓" : (i == today ? "Up next" : "Later today"), 12.5f, done ? Ui.GOLD : Ui.MUTED, false));
            row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            TextView chk = Ui.text(this, done ? "✓" : "", 18, Color.parseColor("#1A1300"), true); chk.setGravity(Gravity.CENTER);
            chk.setBackground(done ? Ui.grad(Ui.G_QURAN, Ui.dp(this, 16)) : Ui.glass(this, 16));
            row.addView(chk, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
            row.setOnClickListener(v -> {
                int t2 = d.quranToday(Data.today());
                if (k == t2) { d.hifzStep(1); buzz(40); Ui.pop(chk); }
                else if (k == t2 - 1) { d.hifzStep(-1); buzz(10); }
                else { Toast.makeText(this, "Tick the quarters in order", Toast.LENGTH_SHORT).show(); return; }
                if (d.quranDone(Data.today()) && k == daily - 1) Toast.makeText(this, "Today's portion complete, MashaAllah! 📖", Toast.LENGTH_LONG).show();
                h.postDelayed(() -> show(false), 300);
            });
            Ui.press(row);
            tq.addView(row, Ui.top(this, 10));
        }
        if (today < daily && pos < 240) {
            TextView all = Ui.button(this, "✓  Mark today's portion done", Ui.G_QURAN);
            all.setOnClickListener(v -> { int need = d.hifzDaily() - d.quranToday(Data.today()); for (int i = 0; i < need; i++) d.hifzStep(1); buzz(60); Toast.makeText(this, "Today's portion complete, MashaAllah! 📖", Toast.LENGTH_LONG).show(); show(false); });
            tq.addView(all, Ui.top(this, 14));
        }
        col.addView(tq);

        // overall
        LinearLayout ov = Ui.card(this);
        LinearLayout orow = Ui.row(this);
        Charts.Ring tr = new Charts.Ring(this); tr.stroke = 0.1f;
        tr.set(pos / 240f, Math.round(pos / 2.4f) + "%", "", Ui.G_QURAN);
        orow.addView(tr, new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 92)));
        LinearLayout otx = Ui.col(this); otx.setPadding(Ui.dp(this, 16), 0, 0, 0);
        otx.addView(Ui.text(this, "Whole Quran", 17, Ui.TEXT, true));
        otx.addView(Ui.text(this, (pos / 8) + " paras + " + (pos % 8) + " quarters memorized", 13.5f, Ui.MUTED, false));
        int daysLeft = (int) Math.ceil((240 - pos) / (double) daily);
        Calendar fin = Calendar.getInstance(); fin.add(Calendar.DAY_OF_YEAR, daysLeft);
        otx.addView(Ui.text(this, pos >= 240 ? "Completed 🎉" : "At this pace: " + daysLeft + " days · " + new SimpleDateFormat("MMM yyyy", Locale.US).format(fin.getTime()), 13.5f, Ui.GOLD, true));
        orow.addView(otx, new LinearLayout.LayoutParams(0, -2, 1));
        ov.addView(orow);

        GridLayout g = new GridLayout(this); g.setColumnCount(6);
        int cell = (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 36 + 36 + 6 * 8)) / 6;
        for (int i = 0; i < 30; i++) {
            int got = Math.max(0, Math.min(8, pos - i * 8));
            TextView t = Ui.text(this, String.valueOf(i + 1), 14, got == 8 ? Color.parseColor("#1A1300") : Ui.TEXT, true); t.setGravity(Gravity.CENTER);
            if (got == 8) t.setBackground(Ui.grad(Ui.G_QURAN, Ui.dp(this, 12)));
            else if (got > 0) t.setBackground(Ui.tint(this, Ui.G_QURAN, 12, 0x20 + got * 0x10));
            else t.setBackground(Ui.glass(this, 12));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(); lp.width = cell; lp.height = cell; lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
            g.addView(t, lp);
            t.setAlpha(0f); t.animate().alpha(1f).setStartDelay(200 + i * 18L).setDuration(300).start();
        }
        ov.addView(g, Ui.top(this, 14));
        col.addView(ov);

        LinearLayout wk = Ui.card(this);
        wk.addView(Ui.text(this, "Last 7 days", 16.5f, Ui.TEXT, true));
        float[] v = new float[7]; String[] l = new String[7];
        for (int i = 0; i < 7; i++) { v[i] = d.quranToday(Data.daysAgo(6 - i)); l[i] = dayName(6 - i); }
        LinearLayout.LayoutParams bl = Ui.top(this, 10); bl.height = Ui.dp(this, 140);
        wk.addView(new Charts.Bars(this).set(v, l, daily, Ui.G_QURAN, 6), bl);
        col.addView(wk);
    }

    // ================= TASBEEH =================
    void tasbeeh(LinearLayout col) {
        String day = Data.today();
        header(col, "Tasbeeh", d.zikrsDone(day) + " of " + d.zikrs().size() + " completed today");
        for (JSONObject z : d.zikrs()) {
            String id = z.optString("id");
            int cnt = d.zikrCount(day, id), tg = Math.max(1, z.optInt("target", 33));
            boolean done = cnt >= tg;
            LinearLayout c = Ui.card(this);
            if (done) c.setBackground(Ui.tint(this, Ui.G_ZIKR, 26, 0x38));
            LinearLayout r = Ui.row(this);
            LinearLayout tx = Ui.col(this);
            tx.addView(Ui.text(this, z.optString("ar"), 23, Color.parseColor("#F5D0FE"), true));
            tx.addView(Ui.text(this, z.optString("name"), 15.5f, Ui.TEXT, true));
            tx.addView(Ui.text(this, cnt + " / " + tg + (done ? "  ·  done ✓" : ""), 13, done ? Ui.GREEN : Ui.MUTED, done));
            r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.11f;
            ring.set(cnt / (float) tg, done ? "✓" : String.valueOf(cnt), "", Ui.G_ZIKR);
            r.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 70), Ui.dp(this, 70)));
            c.addView(r);
            c.setOnClickListener(v -> { counterId = id; show(true); });
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
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, 46), Ui.dp(this, 20), Ui.dp(this, 30));
        LinearLayout top = Ui.row(this);
        TextView back = Ui.button(this, "←  Back", null); back.setOnClickListener(v -> { counterId = null; show(true); });
        top.addView(back);
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        TextView reset = Ui.button(this, "Reset", null); reset.setTextColor(Ui.RED);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Reset today's count?").setPositiveButton("Reset", (x, y) -> { d.setZikr(id, 0); show(false); }).setNegativeButton("Cancel", null).show());
        top.addView(reset);
        col.addView(top, new LinearLayout.LayoutParams(-1, -2));
        TextView ar = Ui.text(this, z.optString("ar"), 36, Color.parseColor("#F5D0FE"), true); ar.setGravity(Gravity.CENTER); ar.setPadding(0, Ui.dp(this, 30), 0, Ui.dp(this, 4));
        col.addView(ar, new LinearLayout.LayoutParams(-1, -2));
        TextView nm = Ui.text(this, z.optString("name"), 17, Ui.MUTED, false); nm.setGravity(Gravity.CENTER);
        col.addView(nm, new LinearLayout.LayoutParams(-1, -2));
        Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.055f;
        int cnt = d.zikrCount(Data.today(), id);
        ring.set(cnt / (float) tg, String.valueOf(cnt), "of " + tg, Ui.G_ZIKR);
        FrameLayout tap = new FrameLayout(this);
        tap.setBackground(Ui.glass(this, 200));
        int sz = Ui.dp(this, 300);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(sz, sz); tl.topMargin = Ui.dp(this, 34);
        tap.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        col.addView(tap, tl);
        TextView hint = Ui.text(this, cnt >= tg ? "MashaAllah, target complete ✓" : "Tap anywhere on the circle", 14.5f, cnt >= tg ? Ui.GREEN : Ui.MUTED, false);
        hint.setGravity(Gravity.CENTER); hint.setPadding(0, Ui.dp(this, 22), 0, 0);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        tap.setOnClickListener(v -> {
            int c = d.zikrCount(Data.today(), id) + 1;
            d.setZikr(id, c);
            ring.set(c / (float) tg, String.valueOf(c), "of " + tg, Ui.G_ZIKR);
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()).start();
            if (c == tg) { buzz(450); hint.setText("MashaAllah, target complete ✓"); hint.setTextColor(Ui.GREEN); Ui.pop(hint); }
            else buzz(c % 33 == 0 ? 120 : 18);
        });
        return col;
    }

    // ================= REPORT =================
    void report(LinearLayout col) {
        String day = Data.today();
        header(col, "Report", "Your progress at a glance");
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "Last 7 days score", 16.5f, Ui.TEXT, true));
        float[] v = new float[7]; String[] l = new String[7];
        for (int i = 0; i < 7; i++) { v[i] = d.score(Data.daysAgo(6 - i)); l[i] = dayName(6 - i); }
        LinearLayout.LayoutParams bl = Ui.top(this, 10); bl.height = Ui.dp(this, 170);
        c.addView(new Charts.Bars(this).set(v, l, 70, Ui.G_TASK, 6), bl);
        TextView s = Ui.text(this, "🔥 Streak: " + d.streak() + " days  ·  dashed line = 70% (a good day)", 13, Ui.MUTED, false); s.setPadding(0, Ui.dp(this, 8), 0, 0);
        c.addView(s);
        col.addView(c);

        LinearLayout t = Ui.card(this);
        t.addView(Ui.text(this, "Today so far  ·  " + d.score(day) + "%", 16.5f, Ui.TEXT, true));
        t.addView(line("💧 Water", Data.fmt(d.water(day)) + " / " + Data.fmt(d.waterGoal()) + " ml", d.water(day) >= d.waterGoal()));
        t.addView(line("📖 Quran hifz", d.quranToday(day) + "/" + d.hifzDaily() + " quarters", d.quranDone(day)));
        for (JSONObject k : d.tasks()) t.addView(line(k.optString("emoji") + " " + k.optString("name"), d.taskDone(day, k) ? "done" : "not yet", d.taskDone(day, k)));
        for (JSONObject z : d.zikrs()) t.addView(line("📿 " + z.optString("name"), d.zikrCount(day, z.optString("id")) + "/" + z.optInt("target"), d.zikrDone(day, z)));
        col.addView(t);

        LinearLayout hc = Ui.card(this);
        hc.addView(Ui.text(this, "History", 16.5f, Ui.TEXT, true));
        for (int i = 1; i <= 14; i++) {
            String k = Data.daysAgo(i); int sc = d.score(k);
            LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 11), 0, 0);
            Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i);
            r.addView(Ui.text(this, new SimpleDateFormat("EEE, d MMM", Locale.US).format(cal.getTime()), 14.5f, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(Ui.text(this, "💧" + Math.round(d.waterPct(k) * 100) + "%  📖" + d.quranToday(k) + "  ✅" + d.tasksDone(k) + "  📿" + d.zikrsDone(k) + "   ", 12, Ui.MUTED, false));
            TextView sct = Ui.text(this, sc + "%", 15, sc >= 70 ? Ui.GREEN : sc >= 40 ? Ui.GOLD : Ui.RED, true);
            r.addView(sct);
            hc.addView(r);
        }
        col.addView(hc);
    }

    LinearLayout line(String name, String val, boolean ok) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 11), 0, 0);
        r.addView(Ui.text(this, (ok ? "✅  " : "◻️  ") + name, 14.5f, ok ? Ui.TEXT : Ui.MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.text(this, val, 13.5f, ok ? Ui.GREEN : Ui.MUTED, true));
        return r;
    }

    void buzz(int ms) {
        try { Vibrator v = getSystemService(Vibrator.class); if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)); } catch (Exception ignored) {}
    }
}
