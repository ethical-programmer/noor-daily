package com.ethical.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    String counterId = null;       // open tasbeeh counter
    final Handler h = new Handler(Looper.getMainLooper());
    Runnable ticker;

    static final String[] TABS = {"🏠", "💧", "✅", "📿", "📊"};
    static final String[] TAB_NAMES = {"Today", "Water", "Tasks", "Tasbeeh", "Report"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = Ui.row(this); nav.setBackgroundColor(Ui.NAV);
        nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        tab = getIntent().getIntExtra("tab", 0);
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); tab = i.getIntExtra("tab", tab); counterId = null; }

    @Override protected void onResume() {
        super.onResume();
        if (d.hasPin() && !App.unlocked) { startActivity(new Intent(this, LockActivity.class)); overridePendingTransition(0, 0); }
        Alarms.ensure(this);
        show();
        ticker = new Runnable() { public void run() { if (tab == 0) updateFocus(); h.postDelayed(this, 1000); } };
        h.postDelayed(ticker, 1000);
    }
    @Override protected void onPause() { super.onPause(); h.removeCallbacksAndMessages(null); }

    @Override public void onBackPressed() {
        if (counterId != null) { counterId = null; show(); return; }
        if (tab != 0) { tab = 0; show(); return; }
        super.onBackPressed();
    }

    // ================= navigation =================
    void buildNav() {
        nav.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            LinearLayout it = new LinearLayout(this); it.setOrientation(LinearLayout.VERTICAL); it.setGravity(Gravity.CENTER);
            TextView ic = Ui.text(this, TABS[i], 20, Ui.TEXT, false); ic.setGravity(Gravity.CENTER);
            ic.setAlpha(i == tab ? 1f : 0.5f);
            TextView nm = Ui.text(this, TAB_NAMES[i], 11.5f, i == tab ? Ui.GREEN : Ui.MUTED, i == tab); nm.setGravity(Gravity.CENTER);
            it.addView(ic); it.addView(nm);
            it.setOnClickListener(v -> { tab = k; counterId = null; show(); });
            nav.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
        }
    }

    void show() {
        buildNav();
        content.removeAllViews();
        if (counterId != null) { content.addView(counter()); return; }
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 18); col.setPadding(p, Ui.dp(this, 22), p, Ui.dp(this, 28));
        sv.addView(col);
        switch (tab) {
            case 0: today(col); break;
            case 1: water(col); break;
            case 2: tasks(col); break;
            case 3: tasbeeh(col); break;
            default: report(col);
        }
        content.addView(sv);
    }

    LinearLayout header(LinearLayout col, String title, String sub) {
        LinearLayout r = Ui.row(this);
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(this, title, 26, Ui.TEXT, true));
        if (sub != null) t.addView(Ui.text(this, sub, 14, Ui.MUTED, false));
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView gear = Ui.text(this, "⚙️", 24, Ui.TEXT, false);
        gear.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), 0, Ui.dp(this, 6));
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        r.addView(gear);
        col.addView(r);
        return r;
    }

    // ================= TODAY =================
    TextView focusText, focusBtn;
    void today(LinearLayout col) {
        String day = Data.today();
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hr < 12 ? "Good morning" : hr < 17 ? "Good afternoon" : "Good evening";
        header(col, "Assalamu Alaikum", greet + " · " + new SimpleDateFormat("EEEE, d MMMM", Locale.US).format(new Date()));

        LinearLayout hero = Ui.card(this); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring big = new Charts.Ring(this);
        int sc = d.score(day);
        big.set(sc / 100f, sc + "%", "today's score", sc >= 70 ? Ui.GREEN : sc >= 40 ? Ui.GOLD : Ui.RED);
        hero.addView(big, new LinearLayout.LayoutParams(Ui.dp(this, 190), Ui.dp(this, 190)));
        int st = d.streak();
        TextView streak = Ui.text(this, st > 0 ? "🔥 " + st + " day streak" : "Score 70%+ today to start a streak", 14.5f, st > 0 ? Ui.GOLD : Ui.MUTED, true);
        streak.setGravity(Gravity.CENTER); streak.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        hero.addView(streak, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout minis = Ui.row(this);
        minis.addView(mini(d.waterPct(day), "Water", Data.fmt(d.water(day)) + " ml", Ui.BLUE, 1), Ui.weight(this, 0, 0));
        minis.addView(mini(d.taskPct(day), "Tasks", d.tasksDone(day) + "/" + d.tasks().size(), Ui.GREEN, 2), Ui.weight(this, 0, 6));
        minis.addView(mini(d.zikrPct(day), "Tasbeeh", d.zikrsDone(day) + "/" + d.zikrs().size(), Ui.GOLD, 3), Ui.weight(this, 0, 6));
        hero.addView(minis, new LinearLayout.LayoutParams(-1, -2));
        col.addView(hero);

        // quick water
        LinearLayout qw = Ui.card(this);
        LinearLayout r = Ui.row(this);
        LinearLayout tx = new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(Ui.text(this, "💧 Water", 16, Ui.TEXT, true));
        long nxt = d.p.getLong("waterNextAt", 0);
        tx.addView(Ui.text(this, d.water(day) >= d.waterGoal() ? "Goal reached, MashaAllah!" : (nxt > System.currentTimeMillis() && d.getBool("waterOn", true) ? "Next reminder " + new SimpleDateFormat("h:mm a", Locale.US).format(new Date(nxt)) : "Stay hydrated"), 13, Ui.MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = Ui.button(this, "+ " + d.glass() + " ml", Ui.BLUE, true);
        add.setOnClickListener(v -> { drink(d.glass()); });
        r.addView(add);
        qw.addView(r);
        col.addView(qw);

        // focus timer
        LinearLayout fc = Ui.card(this);
        LinearLayout fr = Ui.row(this);
        LinearLayout ft = new LinearLayout(this); ft.setOrientation(LinearLayout.VERTICAL);
        ft.addView(Ui.text(this, "🎯 Focus session", 16, Ui.TEXT, true));
        focusText = Ui.text(this, "", 13, Ui.MUTED, false);
        ft.addView(focusText);
        fr.addView(ft, new LinearLayout.LayoutParams(0, -2, 1));
        focusBtn = Ui.button(this, "", Ui.PURPLE, true);
        focusBtn.setOnClickListener(v -> {
            if (d.p.getLong("focusEnd", 0) > System.currentTimeMillis()) Alarms.stopFocus(this);
            else pickFocus();
            updateFocus();
        });
        fr.addView(focusBtn);
        fc.addView(fr);
        col.addView(fc);
        updateFocus();

        // next up
        LinearLayout nx = Ui.card(this);
        nx.addView(Ui.text(this, "Up next", 17, Ui.TEXT, true));
        int shown = 0;
        for (JSONObject t : d.tasks()) {
            if (d.taskDone(day, t) || shown >= 4) continue;
            shown++;
            nx.addView(taskRow(t, day, true));
        }
        if (shown == 0) { TextView done = Ui.text(this, "All tasks done today 🎉", 14.5f, Ui.GREEN, true); done.setPadding(0, Ui.dp(this, 10), 0, 0); nx.addView(done); }
        TextView all = Ui.text(this, "See all tasks →", 14, Ui.GREEN, true); all.setPadding(0, Ui.dp(this, 12), 0, 0);
        all.setOnClickListener(v -> { tab = 2; show(); });
        nx.addView(all);
        col.addView(nx);
    }

    LinearLayout mini(float f, String label, String val, int color, int goTab) {
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setBackground(Ui.round(Ui.BG, Ui.dp(this, 16), Ui.LINE)); b.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        Charts.Ring r = new Charts.Ring(this); r.stroke = 0.12f;
        r.set(f, Math.round(f * 100) + "%", "", color);
        b.addView(r, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)));
        TextView l = Ui.text(this, label, 13, Ui.TEXT, true); l.setGravity(Gravity.CENTER); b.addView(l);
        TextView v = Ui.text(this, val, 11.5f, Ui.MUTED, false); v.setGravity(Gravity.CENTER); b.addView(v);
        b.setOnClickListener(x -> { tab = goTab; show(); });
        return b;
    }

    void pickFocus() {
        String[] o = {"15 min", "25 min (Pomodoro)", "45 min", "60 min"}; int[] m = {15, 25, 45, 60};
        new AlertDialog.Builder(this).setTitle("Focus for how long?").setItems(o, (x, i) -> { Alarms.startFocus(this, m[i]); Toast.makeText(this, "Focus started. Put your phone away 📵", Toast.LENGTH_SHORT).show(); updateFocus(); }).show();
    }
    void updateFocus() {
        if (focusText == null || tab != 0 || counterId != null) return;
        long end = d.p.getLong("focusEnd", 0), left = end - System.currentTimeMillis();
        int done = d.getInt("focusDone_" + Data.today(), 0);
        if (left > 0) {
            focusText.setText(String.format(Locale.US, "%02d:%02d left · stay off your phone", left / 60000, (left / 1000) % 60));
            focusBtn.setText("Stop");
        } else {
            focusText.setText(done > 0 ? done + " session" + (done > 1 ? "s" : "") + " done today" : "Deep work without distractions");
            focusBtn.setText("Start");
        }
    }

    void drink(int ml) {
        boolean before = d.water(Data.today()) >= d.waterGoal();
        d.addWater(ml);
        Notifs.cancel(this, Notifs.N_WATER);
        Alarms.scheduleWater(this);
        if (!before && d.water(Data.today()) >= d.waterGoal()) Toast.makeText(this, "Water goal reached, MashaAllah! 💧", Toast.LENGTH_LONG).show();
        show();
    }

    // ================= WATER =================
    void water(LinearLayout col) {
        String day = Data.today();
        header(col, "Water", "Goal " + Data.fmt(d.waterGoal()) + " ml · based on your age, weight & activity");
        LinearLayout c = Ui.card(this); c.setGravity(Gravity.CENTER_HORIZONTAL);
        Charts.Ring r = new Charts.Ring(this);
        r.set(d.waterPct(day), Data.fmt(d.water(day)), "of " + Data.fmt(d.waterGoal()) + " ml", Ui.BLUE);
        c.addView(r, new LinearLayout.LayoutParams(Ui.dp(this, 210), Ui.dp(this, 210)));
        int left = Math.max(0, d.waterGoal() - d.water(day));
        TextView lt = Ui.text(this, left == 0 ? "Goal reached 🎉" : Data.fmt(left) + " ml left · about " + (int) Math.ceil(left / (double) d.glass()) + " glasses", 14.5f, Ui.MUTED, false);
        lt.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        c.addView(lt);
        LinearLayout btns = Ui.row(this);
        int[] am = {150, 250, 500};
        for (int i = 0; i < am.length; i++) {
            final int ml = am[i];
            TextView b = Ui.button(this, "+" + ml, Ui.BLUE, ml == d.glass());
            b.setOnClickListener(v -> drink(ml));
            btns.addView(b, Ui.weight(this, 50, i == 0 ? 0 : 8));
        }
        c.addView(btns, new LinearLayout.LayoutParams(-1, -2));
        TextView undo = Ui.text(this, "Undo last", 14, Ui.BLUE, true); undo.setPadding(0, Ui.dp(this, 12), 0, 0);
        undo.setOnClickListener(v -> { d.undoWater(); show(); });
        c.addView(undo);
        col.addView(c);

        LinearLayout hc = Ui.card(this);
        hc.addView(Ui.text(this, "Today, hour by hour", 16, Ui.TEXT, true));
        float[] all = d.waterByHour(day);
        int from = d.wake() / 60, to = Math.max(from + 6, Math.min(23, d.sleep() / 60));
        int n = to - from + 1; float[] v = new float[n]; String[] lab = new String[n];
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        for (int i = 0; i < n; i++) { v[i] = all[from + i]; int hh = from + i; lab[i] = (i % 3 == 0) ? (hh % 12 == 0 ? 12 : hh % 12) + (hh < 12 ? "a" : "p") : ""; }
        Charts.Bars hb = new Charts.Bars(this).set(v, lab, 0, Ui.BLUE, now - from);
        hc.addView(hb, new LinearLayout.LayoutParams(-1, Ui.dp(this, 150)));
        col.addView(hc);

        LinearLayout wc = Ui.card(this);
        wc.addView(Ui.text(this, "Last 7 days", 16, Ui.TEXT, true));
        wc.addView(Ui.text(this, "Dashed line = your daily goal", 12.5f, Ui.MUTED, false));
        float[] w = new float[7]; String[] wl = new String[7];
        for (int i = 0; i < 7; i++) { String k = Data.daysAgo(6 - i); w[i] = d.water(k); wl[i] = dayName(6 - i); }
        wc.addView(new Charts.Bars(this).set(w, wl, d.waterGoal(), Ui.BLUE, 6), new LinearLayout.LayoutParams(-1, Ui.dp(this, 160)));
        col.addView(wc);

        LinearLayout lg = Ui.card(this);
        lg.addView(Ui.text(this, "Log", 16, Ui.TEXT, true));
        List<int[]> log = d.waterLog(day);
        if (log.isEmpty()) { TextView e = Ui.text(this, "Nothing logged yet today.", 14, Ui.MUTED, false); e.setPadding(0, Ui.dp(this, 8), 0, 0); lg.addView(e); }
        for (int i = log.size() - 1; i >= 0; i--) {
            LinearLayout rr = Ui.row(this); rr.setPadding(0, Ui.dp(this, 9), 0, 0);
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
        header(col, "Daily tasks", d.tasksDone(day) + " of " + d.tasks().size() + " done · long-press a task to delete");
        LinearLayout c = Ui.card(this);
        c.addView(Ui.bar(this, d.taskPct(day), Ui.GREEN));
        for (JSONObject t : d.tasks()) c.addView(taskRow(t, day, false));
        col.addView(c);
        TextView add = Ui.button(this, "+ Add your own task", Ui.GREEN, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = Ui.dp(this, 12);
        add.setOnClickListener(v -> addTask());
        col.addView(add, lp);
    }

    View taskRow(JSONObject t, String day, boolean compact) {
        String id = t.optString("id");
        int val = d.taskVal(day, id), target = Math.max(1, t.optInt("target", 1));
        boolean tick = "tick".equals(t.optString("type")), done = val >= target;
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, Ui.dp(this, 14), 0, compact ? 0 : Ui.dp(this, 4));
        LinearLayout r = Ui.row(this);
        TextView em = Ui.text(this, t.optString("emoji", "•"), 22, Ui.TEXT, false); em.setPadding(0, 0, Ui.dp(this, 12), 0);
        r.addView(em);
        LinearLayout tx = new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL);
        TextView nm = Ui.text(this, t.optString("name"), 15.5f, done ? Ui.GREEN : Ui.TEXT, true);
        tx.addView(nm);
        if (!tick) tx.addView(Ui.text(this, val + " / " + target + " " + t.optString("unit"), 13, Ui.MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        if (tick) {
            TextView cb = Ui.button(this, done ? "✓" : "", Ui.GREEN, done);
            cb.setOnClickListener(v -> { d.setTaskVal(id, done ? 0 : 1); buzz(25); show(); });
            r.addView(cb, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        } else {
            int step = Math.max(1, t.optInt("step", 1));
            TextView minus = Ui.button(this, "−", Ui.TEXT, false);
            minus.setOnClickListener(v -> { d.setTaskVal(id, val - step); show(); });
            TextView plus = Ui.button(this, done ? "✓" : "+" + (step > 1 ? step : ""), Ui.GREEN, true);
            plus.setOnClickListener(v -> { d.setTaskVal(id, val + step); buzz(25); if (val + step >= target && val < target) Toast.makeText(this, t.optString("name") + " done ✓", Toast.LENGTH_SHORT).show(); show(); });
            r.addView(minus, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 48)); pl.leftMargin = Ui.dp(this, 8);
            r.addView(plus, pl);
        }
        box.addView(r);
        if (!tick && !compact) { View b = Ui.bar(this, val / (float) target, done ? Ui.GREEN : Ui.BLUE); ((LinearLayout.LayoutParams) b.getLayoutParams()).topMargin = Ui.dp(this, 10); box.addView(b); }
        if (!compact) box.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setTitle("Delete \"" + t.optString("name") + "\"?").setPositiveButton("Delete", (x, y) -> { d.removeTask(id); show(); }).setNegativeButton("Cancel", null).show(); return true; });
        return box;
    }

    void addTask() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        EditText name = new EditText(this); name.setHint("Task name (e.g. Push-ups)"); box.addView(name);
        EditText target = new EditText(this); target.setHint("Daily target number (leave empty = just tick)"); target.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(target);
        EditText unit = new EditText(this); unit.setHint("Unit (e.g. times, min, pages)"); box.addView(unit);
        new AlertDialog.Builder(this).setTitle("New daily task").setView(box).setPositiveButton("Add", (x, y) -> {
            String n = name.getText().toString().trim(); if (n.isEmpty()) return;
            int tg = 1; try { tg = Integer.parseInt(target.getText().toString().trim()); } catch (Exception ignored) {}
            boolean tick = target.getText().toString().trim().isEmpty() || tg <= 1;
            d.addTask(Data.task("c" + System.currentTimeMillis(), "⭐", n, tick ? "tick" : "count", tick ? 1 : tg, unit.getText().toString().trim(), tg >= 20 ? 5 : 1));
            show();
        }).setNegativeButton("Cancel", null).show();
    }

    // ================= TASBEEH =================
    void tasbeeh(LinearLayout col) {
        String day = Data.today();
        header(col, "Tasbeeh", d.zikrsDone(day) + " of " + d.zikrs().size() + " completed today · tap to count");
        for (JSONObject z : d.zikrs()) {
            String id = z.optString("id");
            int cnt = d.zikrCount(day, id), tg = Math.max(1, z.optInt("target", 33));
            LinearLayout c = Ui.card(this);
            LinearLayout r = Ui.row(this);
            LinearLayout tx = new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL);
            TextView ar = Ui.text(this, z.optString("ar"), 22, Ui.GOLD, true);
            tx.addView(ar);
            tx.addView(Ui.text(this, z.optString("name"), 15, Ui.TEXT, true));
            tx.addView(Ui.text(this, cnt + " / " + tg + (cnt >= tg ? "  ✓ done" : ""), 13, cnt >= tg ? Ui.GREEN : Ui.MUTED, cnt >= tg));
            r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.12f;
            ring.set(cnt / (float) tg, cnt >= tg ? "✓" : String.valueOf(cnt), "", cnt >= tg ? Ui.GREEN : Ui.GOLD);
            r.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)));
            c.addView(r);
            c.setOnClickListener(v -> { counterId = id; show(); });
            c.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setTitle("Delete " + z.optString("name") + "?").setPositiveButton("Delete", (x, y) -> { d.removeZikr(id); show(); }).setNegativeButton("Cancel", null).show(); return true; });
            col.addView(c);
        }
        TextView add = Ui.button(this, "+ Add a zikr", Ui.GOLD, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = Ui.dp(this, 12);
        add.setOnClickListener(v -> addZikr());
        col.addView(add, lp);
    }

    View counter() {
        JSONObject z = null;
        for (JSONObject o : d.zikrs()) if (o.optString("id").equals(counterId)) z = o;
        if (z == null) { counterId = null; LinearLayout e = new LinearLayout(this); return e; }
        final JSONObject zz = z;
        final String id = counterId;
        int tg = Math.max(1, z.optInt("target", 33));
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 20));
        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "←  Back", 16, Ui.TEXT, true); back.setOnClickListener(v -> { counterId = null; show(); });
        top.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        TextView reset = Ui.text(this, "Reset", 15, Ui.RED, true);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Reset today's count?").setPositiveButton("Reset", (x, y) -> { d.setZikr(id, 0); show(); }).setNegativeButton("Cancel", null).show());
        top.addView(reset);
        col.addView(top, new LinearLayout.LayoutParams(-1, -2));
        TextView ar = Ui.text(this, z.optString("ar"), 34, Ui.GOLD, true); ar.setGravity(Gravity.CENTER); ar.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 4));
        col.addView(ar, new LinearLayout.LayoutParams(-1, -2));
        TextView nm = Ui.text(this, z.optString("name"), 17, Ui.TEXT, false); nm.setGravity(Gravity.CENTER);
        col.addView(nm, new LinearLayout.LayoutParams(-1, -2));
        Charts.Ring ring = new Charts.Ring(this); ring.stroke = 0.06f;
        int cnt = d.zikrCount(Data.today(), id);
        ring.set(cnt / (float) tg, String.valueOf(cnt), "of " + tg, cnt >= tg ? Ui.GREEN : Ui.GOLD);
        FrameLayout tapArea = new FrameLayout(this);
        tapArea.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 200), Ui.LINE));
        int sz = Ui.dp(this, 290);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(sz, sz); tl.topMargin = Ui.dp(this, 26);
        tapArea.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        col.addView(tapArea, tl);
        TextView hint = Ui.text(this, "Tap the circle to count", 14, Ui.MUTED, false); hint.setGravity(Gravity.CENTER); hint.setPadding(0, Ui.dp(this, 18), 0, 0);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        tapArea.setOnClickListener(v -> {
            int c = d.zikrCount(Data.today(), id) + 1;
            d.setZikr(id, c);
            ring.set(c / (float) tg, String.valueOf(c), "of " + tg, c >= tg ? Ui.GREEN : Ui.GOLD);
            if (c == tg) { buzz(400); hint.setText("MashaAllah, target complete ✓"); hint.setTextColor(Ui.GREEN); }
            else buzz(c % 33 == 0 ? 120 : 18);
        });
        return col;
    }

    void addZikr() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        EditText name = new EditText(this); name.setHint("Name (e.g. SubhanAllahi wa bihamdihi)"); box.addView(name);
        EditText ar = new EditText(this); ar.setHint("Arabic text (optional)"); box.addView(ar);
        EditText target = new EditText(this); target.setHint("Daily count (e.g. 100)"); target.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(target);
        new AlertDialog.Builder(this).setTitle("New zikr").setView(box).setPositiveButton("Add", (x, y) -> {
            String n = name.getText().toString().trim(); if (n.isEmpty()) return;
            int tg = 33; try { tg = Integer.parseInt(target.getText().toString().trim()); } catch (Exception ignored) {}
            d.addZikr(Data.zikr("z" + System.currentTimeMillis(), n, ar.getText().toString().trim(), Math.max(1, tg)));
            show();
        }).setNegativeButton("Cancel", null).show();
    }

    // ================= REPORT =================
    void report(LinearLayout col) {
        String day = Data.today();
        header(col, "Report", "Your progress at a glance");
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "Last 7 days score", 16, Ui.TEXT, true));
        float[] v = new float[7]; String[] l = new String[7];
        for (int i = 0; i < 7; i++) { v[i] = d.score(Data.daysAgo(6 - i)); l[i] = dayName(6 - i); }
        c.addView(new Charts.Bars(this).set(v, l, 70, Ui.GREEN, 6), new LinearLayout.LayoutParams(-1, Ui.dp(this, 170)));
        TextView s = Ui.text(this, "🔥 Streak: " + d.streak() + " days  ·  Dashed line = 70% (a good day)", 13, Ui.MUTED, false); s.setPadding(0, Ui.dp(this, 6), 0, 0);
        c.addView(s);
        col.addView(c);

        LinearLayout t = Ui.card(this);
        t.addView(Ui.text(this, "Today so far · " + d.score(day) + "%", 16, Ui.TEXT, true));
        t.addView(line("💧 Water", Data.fmt(d.water(day)) + " / " + Data.fmt(d.waterGoal()) + " ml", d.water(day) >= d.waterGoal()));
        for (JSONObject k : d.tasks()) {
            int val = d.taskVal(day, k.optString("id"));
            t.addView(line(k.optString("emoji") + " " + k.optString("name"), "tick".equals(k.optString("type")) ? (val > 0 ? "done" : "not done") : val + "/" + k.optInt("target") + " " + k.optString("unit"), d.taskDone(day, k)));
        }
        for (JSONObject z : d.zikrs())
            t.addView(line("📿 " + z.optString("name"), d.zikrCount(day, z.optString("id")) + "/" + z.optInt("target"), d.zikrDone(day, z)));
        col.addView(t);

        LinearLayout hcard = Ui.card(this);
        hcard.addView(Ui.text(this, "History", 16, Ui.TEXT, true));
        for (int i = 1; i <= 14; i++) {
            String k = Data.daysAgo(i);
            int sc = d.score(k);
            LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 10), 0, 0);
            Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -i);
            r.addView(Ui.text(this, new SimpleDateFormat("EEE, d MMM", Locale.US).format(cal.getTime()), 14.5f, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(Ui.text(this, "💧" + Math.round(d.waterPct(k) * 100) + "%  ✅" + d.tasksDone(k) + "  📿" + d.zikrsDone(k) + "   ", 12.5f, Ui.MUTED, false));
            TextView sct = Ui.text(this, sc + "%", 15, sc >= 70 ? Ui.GREEN : sc >= 40 ? Ui.GOLD : Ui.RED, true);
            r.addView(sct);
            hcard.addView(r);
        }
        col.addView(hcard);
    }

    LinearLayout line(String name, String val, boolean ok) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 10), 0, 0);
        r.addView(Ui.text(this, (ok ? "✅ " : "⬜ ") + name, 14.5f, ok ? Ui.TEXT : Ui.MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.text(this, val, 13.5f, ok ? Ui.GREEN : Ui.MUTED, true));
        return r;
    }

    void buzz(int ms) {
        try { Vibrator v = getSystemService(Vibrator.class); if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)); } catch (Exception ignored) {}
    }
}
