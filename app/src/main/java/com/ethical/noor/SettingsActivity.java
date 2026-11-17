package com.ethical.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class SettingsActivity extends Activity {
    Data d; LinearLayout root; TextView goalInfo;
    static final int THEME = android.R.style.Theme_Material_Dialog_Alert;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (d.hasPin() && !App.unlocked) startActivity(new Intent(this, LockActivity.class));
    }

    void build() {
        FrameLayout frame = new FrameLayout(this);
        frame.addView(new Charts.Aurora(this), new FrameLayout.LayoutParams(-1, -1));
        ScrollView sv = new ScrollView(this);
        root = Ui.col(this);
        int p = Ui.dp(this, 18); root.setPadding(p, Ui.dp(this, 46), p, Ui.dp(this, 50));
        sv.addView(root);
        frame.addView(sv);

        LinearLayout top = Ui.row(this);
        TextView back = Ui.button(this, "←", null); back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 48)));
        TextView t = Ui.title(this, "Settings", 28); t.setPadding(Ui.dp(this, 14), 0, 0, 0);
        top.addView(t);
        root.addView(top);

        // profile
        LinearLayout pr = section("👤", "Your profile", Ui.G_WATER);
        pr.addView(numRow("Age (years)", "age", d.age(), 10, 100));
        EditText ft = num(d.feet()), in = num(d.inches());
        LinearLayout hr = Ui.row(this);
        hr.addView(ft, new LinearLayout.LayoutParams(Ui.dp(this, 64), -2));
        hr.addView(Ui.text(this, " ft  ", 15, Ui.MUTED, false));
        hr.addView(in, new LinearLayout.LayoutParams(Ui.dp(this, 64), -2));
        hr.addView(Ui.text(this, " in", 15, Ui.MUTED, false));
        Runnable saveH = () -> { try { int f = Integer.parseInt(ft.getText().toString().trim()), i = Integer.parseInt(in.getText().toString().trim()); if (f >= 3 && f <= 7 && i >= 0 && i <= 11) { d.setFeetInches(f, i); after(); } } catch (Exception ignored) {} };
        ft.setOnFocusChangeListener((v, f) -> { if (!f) saveH.run(); });
        in.setOnFocusChangeListener((v, f) -> { if (!f) saveH.run(); });
        ft.setOnEditorActionListener((v, a, e) -> { saveH.run(); return false; });
        in.setOnEditorActionListener((v, a, e) -> { saveH.run(); return false; });
        pr.addView(labelRow("Height", hr));
        pr.addView(numRow("Weight (kg)", "weight", d.weight(), 25, 250));
        pr.addView(choice("Sex", new String[]{"Male", "Female"}, d.female() ? 1 : 0, Ui.G_WATER, i -> d.setBool("female", i == 1)));
        pr.addView(choice("Activity", new String[]{"Low", "Moderate", "High"}, d.activity(), Ui.G_WATER, i -> d.setInt("activity", i)));
        goalInfo = Ui.text(this, "", 14, Ui.BLUE, true); goalInfo.setPadding(0, Ui.dp(this, 14), 0, 0);
        pr.addView(goalInfo);

        // quran
        LinearLayout qu = section("📖", "Quran hifz", Ui.G_QURAN);
        qu.addView(choice("Daily portion", new String[]{"¼ para", "½ para", "1 para"}, d.hifzDaily() == 2 ? 0 : d.hifzDaily() == 8 ? 2 : 1, Ui.G_QURAN, i -> d.setInt("hifzDaily", new int[]{2, 4, 8}[i])));
        TextView sp = Ui.button(this, "Start from Para " + (d.hifzPos() / 8 + 1), null);
        sp.setOnClickListener(v -> {
            String[] items = new String[30]; for (int i = 0; i < 30; i++) items[i] = "Para " + (i + 1) + "  ·  " + Data.PARA[i];
            new AlertDialog.Builder(this, THEME).setTitle("Where are you starting?").setItems(items, (x, i) -> {
                new AlertDialog.Builder(this, THEME).setTitle("Start from Para " + (i + 1) + "?").setMessage("Paras before it will be marked as memorized.")
                        .setPositiveButton("Yes", (a, b2) -> { d.setHifzStartPara(i + 1); recreate(); }).setNegativeButton("Cancel", null).show();
            }).show();
        });
        qu.addView(sp, Ui.top(this, 12));

        // water & schedule
        LinearLayout w = section("💧", "Water & daily schedule", Ui.G_WATER);
        w.addView(choice("Glass size", new String[]{"150", "250", "350", "500 ml"}, idx(d.glass(), new int[]{150, 250, 350, 500}), Ui.G_WATER, i -> d.setInt("glass", new int[]{150, 250, 350, 500}[i])));
        w.addView(timeRow("Wake up", "wake", d.wake()));
        w.addView(timeRow("Sleep", "sleep", d.sleep()));
        w.addView(sw("Water reminders (one quiet notification)", "waterOn", true));

        // warnings
        LinearLayout wr = section("⏰", "Warnings & report", Ui.G_REPORT);
        wr.addView(sw("Warn me about unfinished tasks", "warnOn", true));
        wr.addView(timeRow("First warning", "warn1", d.getInt("warn1", 20 * 60)));
        wr.addView(timeRow("Last warning", "warn2", d.getInt("warn2", 22 * 60 + 30)));
        wr.addView(sw("Daily report at 11:59 PM", "reportOn", true));

        // security
        LinearLayout se = section("🔒", "Privacy & security", Ui.G_ZIKR);
        se.addView(note("Noor Daily has no internet permission. Your data never leaves this phone and is excluded from cloud backups."));
        TextView pinBtn = Ui.button(this, d.hasPin() ? "Change PIN" : "Set a PIN lock", Ui.G_ZIKR);
        pinBtn.setOnClickListener(v -> askPin());
        se.addView(pinBtn, Ui.top(this, 12));
        if (d.hasPin()) {
            TextView rm = Ui.button(this, "Remove PIN", null); rm.setTextColor(Ui.RED);
            rm.setOnClickListener(v -> new AlertDialog.Builder(this, THEME).setTitle("Remove PIN?").setPositiveButton("Remove", (x, y) -> { d.clearPin(); recreate(); }).setNegativeButton("Cancel", null).show());
            se.addView(rm, Ui.top(this, 8));
        }
        se.addView(sw("Block screenshots & hide in recent apps", "secure", true));

        // permissions
        LinearLayout pm = section("⚙️", "Make reminders reliable", Ui.G_FOCUS);
        TextView n = Ui.button(this, "Allow notifications", null);
        n.setOnClickListener(v -> { if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1); else Toast.makeText(this, "Already allowed ✓", Toast.LENGTH_SHORT).show(); });
        pm.addView(n, Ui.top(this, 12));
        TextView bat = Ui.button(this, "Allow running in background", null);
        bat.setOnClickListener(v -> {
            PowerManager pw = getSystemService(PowerManager.class);
            if (pw.isIgnoringBatteryOptimizations(getPackageName())) { Toast.makeText(this, "Already allowed ✓", Toast.LENGTH_SHORT).show(); return; }
            try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { Toast.makeText(this, "Open Settings > Apps > Noor Daily > Battery", Toast.LENGTH_LONG).show(); }
        });
        pm.addView(bat, Ui.top(this, 8));

        setContentView(frame);
        for (int i = 0; i < root.getChildCount(); i++) Ui.enter(root.getChildAt(i), i);
        after();
    }

    void after() {
        goalInfo.setText("💧 Water goal " + Data.fmt(d.waterGoal()) + " ml   ·   BMI " + String.format(Locale.US, "%.1f", d.bmi()) + " (" + Data.bmiLabel(d.bmi()) + ")");
        Alarms.scheduleAll(this);
    }

    LinearLayout section(String emoji, String t, int[] g) {
        LinearLayout c = Ui.card(this);
        LinearLayout h = Ui.row(this);
        h.addView(Ui.tile(this, emoji, g, 36));
        TextView tt = Ui.text(this, t, 17, Ui.TEXT, true); tt.setPadding(Ui.dp(this, 12), 0, 0, 0);
        h.addView(tt);
        c.addView(h);
        root.addView(c);
        return c;
    }
    TextView note(String s) { TextView t = Ui.text(this, s, 13.5f, Ui.MUTED, false); t.setPadding(0, Ui.dp(this, 10), 0, 0); return t; }
    static int idx(int v, int[] a) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return 1; }

    EditText num(int v) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER); e.setText(String.valueOf(v));
        e.setTextColor(Ui.TEXT); e.setGravity(Gravity.CENTER); e.setTextSize(16);
        e.setBackground(Ui.glass(this, 14)); e.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10));
        return e;
    }
    LinearLayout labelRow(String label, View right) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 14), 0, 0);
        r.addView(Ui.text(this, label, 15.5f, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(right);
        return r;
    }
    LinearLayout numRow(String label, String key, int val, int min, int max) {
        EditText e = num(val); e.setMinWidth(Ui.dp(this, 90));
        e.setOnFocusChangeListener((v, f) -> { if (!f) save(e, key, min, max); });
        e.setOnEditorActionListener((v, a, ev) -> { save(e, key, min, max); return false; });
        return labelRow(label, e);
    }
    void save(EditText e, String key, int min, int max) {
        try { int v = Integer.parseInt(e.getText().toString().trim()); if (v >= min && v <= max) { d.setInt(key, v); after(); } else Toast.makeText(this, "Enter " + min + " to " + max, Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
    }
    interface Pick { void on(int i); }
    LinearLayout choice(String label, String[] opts, int sel, int[] g, Pick pick) {
        LinearLayout box = Ui.col(this);
        TextView l = Ui.text(this, label, 15.5f, Ui.TEXT, false); l.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 8));
        box.addView(l);
        LinearLayout r = Ui.row(this);
        TextView[] bs = new TextView[opts.length];
        for (int i = 0; i < opts.length; i++) {
            final int k = i;
            TextView b = Ui.button(this, opts[i], i == sel ? g : null);
            b.setTextSize(13.5f);
            bs[i] = b;
            b.setOnClickListener(v -> {
                pick.on(k); after();
                for (int j = 0; j < bs.length; j++) { bs[j].setBackground(j == k ? Ui.grad(g, Ui.dp(this, 18)) : Ui.glass(this, 18)); bs[j].setTextColor(j == k ? Color.parseColor("#0B1020") : Ui.TEXT); }
                Ui.pop(b);
            });
            r.addView(b, Ui.weight(this, 46, i == 0 ? 0 : 8));
        }
        box.addView(r);
        return box;
    }
    LinearLayout timeRow(String label, String key, int min) {
        TextView t = Ui.button(this, Data.hm(min), null);
        t.setOnClickListener(v -> new TimePickerDialog(this, THEME, (tp, h, m) -> { d.setInt(key, h * 60 + m); t.setText(Data.hm(h * 60 + m)); after(); }, min / 60, min % 60, false).show());
        return labelRow(label, t);
    }
    LinearLayout sw(String label, String key, boolean def) {
        Switch s = new Switch(this); s.setChecked(d.getBool(key, def));
        s.setOnCheckedChangeListener((v, on) -> { d.setBool(key, on); after(); if (key.equals("secure")) Toast.makeText(this, "Applies next time you open the app", Toast.LENGTH_SHORT).show(); });
        LinearLayout r = labelRow(label, s);
        ((TextView) r.getChildAt(0)).setTextSize(14.5f);
        return r;
    }

    void askPin() {
        EditText a = new EditText(this); a.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); a.setHint("New PIN (4-8 digits)");
        EditText c = new EditText(this); c.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); c.setHint("Repeat PIN");
        LinearLayout box = Ui.col(this); box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(a); box.addView(c);
        new AlertDialog.Builder(this, THEME).setTitle("Set PIN").setView(box).setPositiveButton("Save", (x, y) -> {
            String p1 = a.getText().toString(), p2 = c.getText().toString();
            if (p1.length() < 4 || p1.length() > 8) { Toast.makeText(this, "PIN must be 4-8 digits", Toast.LENGTH_LONG).show(); return; }
            if (!p1.equals(p2)) { Toast.makeText(this, "PINs don't match", Toast.LENGTH_LONG).show(); return; }
            d.setPin(p1); App.unlocked = true;
            Toast.makeText(this, "PIN set ✓", Toast.LENGTH_SHORT).show(); recreate();
        }).setNegativeButton("Cancel", null).show();
    }
}
