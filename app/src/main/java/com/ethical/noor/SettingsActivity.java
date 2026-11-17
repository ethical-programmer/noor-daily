package com.ethical.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    Data d; LinearLayout root; TextView goalInfo;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (d.hasPin() && !App.unlocked) startActivity(new Intent(this, LockActivity.class));
    }

    void build() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Ui.BG);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 18); root.setPadding(p, Ui.dp(this, 24), p, Ui.dp(this, 40));
        sv.addView(root);

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "←", 26, Ui.TEXT, true); back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back); top.addView(Ui.text(this, "Settings", 26, Ui.TEXT, true));
        root.addView(top);

        // ---- profile ----
        LinearLayout pr = section("👤  Your profile");
        pr.addView(numRow("Age (years)", "age", d.age(), 10, 100));
        pr.addView(numRow("Height (cm)", "height", d.height(), 100, 230));
        pr.addView(numRow("Weight (kg)", "weight", d.weight(), 25, 250));
        pr.addView(choice("Sex", new String[]{"Male", "Female"}, d.female() ? 1 : 0, i -> { d.setBool("female", i == 1); after(); }));
        pr.addView(choice("Activity", new String[]{"Low", "Moderate", "High"}, d.activity(), i -> { d.setInt("activity", i); after(); }));
        goalInfo = Ui.text(this, "", 14, Ui.BLUE, true); goalInfo.setPadding(0, Ui.dp(this, 12), 0, 0);
        pr.addView(goalInfo);

        // ---- water & day ----
        LinearLayout w = section("💧  Water & daily schedule");
        w.addView(choice("Glass size", new String[]{"150 ml", "250 ml", "350 ml", "500 ml"}, idx(d.glass(), new int[]{150, 250, 350, 500}), i -> { d.setInt("glass", new int[]{150, 250, 350, 500}[i]); after(); }));
        w.addView(timeRow("Wake up", "wake", d.wake()));
        w.addView(timeRow("Sleep", "sleep", d.sleep()));
        w.addView(sw("Water reminders (one quiet notification)", "waterOn", true));

        // ---- warnings ----
        LinearLayout wr = section("⏰  Warnings & report");
        wr.addView(sw("Warn me about unfinished tasks", "warnOn", true));
        wr.addView(timeRow("First warning", "warn1", d.getInt("warn1", 20 * 60)));
        wr.addView(timeRow("Last warning", "warn2", d.getInt("warn2", 22 * 60 + 30)));
        wr.addView(sw("Daily report at 11:59 PM", "reportOn", true));

        // ---- security ----
        LinearLayout se = section("🔒  Privacy & security");
        se.addView(note("Noor Daily has no internet permission. Your data never leaves this phone and is excluded from cloud backups."));
        TextView pinBtn = Ui.button(this, d.hasPin() ? "Change PIN" : "Set a PIN lock", Ui.GREEN, true);
        pinBtn.setOnClickListener(v -> askPin());
        se.addView(pinBtn, lp(10));
        if (d.hasPin()) {
            TextView rm = Ui.button(this, "Remove PIN", Ui.RED, false);
            rm.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Remove PIN?").setPositiveButton("Remove", (x, y) -> { d.clearPin(); recreate(); }).setNegativeButton("Cancel", null).show());
            se.addView(rm, lp(8));
        }
        se.addView(sw("Block screenshots & hide in recent apps", "secure", true));

        // ---- permissions ----
        LinearLayout pm = section("⚙️  Make reminders reliable");
        TextView n = Ui.button(this, "Allow notifications", Ui.TEXT, false);
        n.setOnClickListener(v -> { if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1); else Toast.makeText(this, "Already allowed ✓", Toast.LENGTH_SHORT).show(); });
        pm.addView(n, lp(6));
        TextView bat = Ui.button(this, "Allow running in background", Ui.TEXT, false);
        bat.setOnClickListener(v -> {
            PowerManager pw = getSystemService(PowerManager.class);
            if (pw.isIgnoringBatteryOptimizations(getPackageName())) { Toast.makeText(this, "Already allowed ✓", Toast.LENGTH_SHORT).show(); return; }
            try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { Toast.makeText(this, "Open Settings > Apps > Noor Daily > Battery", Toast.LENGTH_LONG).show(); }
        });
        pm.addView(bat, lp(8));

        setContentView(sv);
        after();
    }

    void after() {
        goalInfo.setText("Daily water goal: " + Data.fmt(d.waterGoal()) + " ml   ·   BMI " + String.format(java.util.Locale.US, "%.1f", d.bmi()) + " (" + Data.bmiLabel(d.bmi()) + ")");
        Alarms.scheduleAll(this);
    }

    LinearLayout section(String t) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, t, 17, Ui.TEXT, true));
        root.addView(c);
        return c;
    }
    TextView note(String s) { TextView t = Ui.text(this, s, 13.5f, Ui.MUTED, false); t.setPadding(0, Ui.dp(this, 8), 0, 0); return t; }
    LinearLayout.LayoutParams lp(int top) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2); l.topMargin = Ui.dp(this, top); return l; }
    static int idx(int v, int[] a) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return 1; }

    LinearLayout labelRow(String label, View right) {
        LinearLayout r = Ui.row(this); r.setPadding(0, Ui.dp(this, 12), 0, 0);
        r.addView(Ui.text(this, label, 15.5f, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(right);
        return r;
    }
    LinearLayout numRow(String label, String key, int val, int min, int max) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER); e.setText(String.valueOf(val));
        e.setTextColor(Ui.TEXT); e.setGravity(Gravity.CENTER); e.setMinWidth(Ui.dp(this, 90));
        e.setBackground(Ui.round(Ui.BG, Ui.dp(this, 12), Ui.LINE));
        e.setOnFocusChangeListener((v, f) -> { if (!f) save(e, key, min, max); });
        e.setOnEditorActionListener((v, a, ev) -> { save(e, key, min, max); return false; });
        return labelRow(label, e);
    }
    void save(EditText e, String key, int min, int max) {
        try { int v = Integer.parseInt(e.getText().toString().trim()); if (v >= min && v <= max) { d.setInt(key, v); after(); } else Toast.makeText(this, "Enter " + min + " to " + max, Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
    }
    interface Pick { void on(int i); }
    LinearLayout choice(String label, String[] opts, int sel, Pick pick) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        TextView l = Ui.text(this, label, 15.5f, Ui.TEXT, false); l.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        box.addView(l);
        LinearLayout r = Ui.row(this);
        for (int i = 0; i < opts.length; i++) {
            final int k = i;
            TextView b = Ui.button(this, opts[i], i == sel ? Ui.GREEN : Ui.TEXT, i == sel);
            b.setTextSize(13.5f);
            b.setOnClickListener(v -> { pick.on(k); recreate(); });
            r.addView(b, Ui.weight(this, 44, i == 0 ? 0 : 8));
        }
        box.addView(r);
        return box;
    }
    LinearLayout timeRow(String label, String key, int min) {
        TextView t = Ui.button(this, Data.hm(min), Ui.TEXT, false);
        t.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> { d.setInt(key, h * 60 + m); t.setText(Data.hm(h * 60 + m)); after(); }, min / 60, min % 60, false).show());
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
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(a); box.addView(c);
        new AlertDialog.Builder(this).setTitle("Set PIN").setView(box).setPositiveButton("Save", (x, y) -> {
            String p1 = a.getText().toString(), p2 = c.getText().toString();
            if (p1.length() < 4 || p1.length() > 8) { Toast.makeText(this, "PIN must be 4-8 digits", Toast.LENGTH_LONG).show(); return; }
            if (!p1.equals(p2)) { Toast.makeText(this, "PINs don't match", Toast.LENGTH_LONG).show(); return; }
            d.setPin(p1); App.unlocked = true;
            Toast.makeText(this, "PIN set ✓", Toast.LENGTH_SHORT).show(); recreate();
        }).setNegativeButton("Cancel", null).show();
    }
}
