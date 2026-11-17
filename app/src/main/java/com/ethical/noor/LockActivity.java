package com.ethical.noor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** PIN screen shown whenever the app is opened (if a PIN is set). */
public class LockActivity extends Activity {
    Data d; StringBuilder pin = new StringBuilder(); TextView dots, msg;
    final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = new Data(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 30), Ui.dp(this, 90), Ui.dp(this, 30), Ui.dp(this, 30));
        FrameLayout icon = Ui.tile(this, R.drawable.ic_lock, Ui.ACCENT, 72, true);
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(-2, -2); il.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(icon, il);
        TextView t = Ui.text(this, "Noor Daily is locked", 22, Ui.TEXT, true); t.setGravity(Gravity.CENTER); t.setPadding(0, Ui.dp(this, 12), 0, 0);
        root.addView(t);
        dots = Ui.text(this, "", 34, Ui.GREEN, true); dots.setGravity(Gravity.CENTER); dots.setPadding(0, Ui.dp(this, 24), 0, 0);
        root.addView(dots);
        msg = Ui.text(this, "Enter your PIN", 15, Ui.MUTED, false); msg.setGravity(Gravity.CENTER); msg.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 24));
        root.addView(msg);
        GridLayout g = new GridLayout(this); g.setColumnCount(3);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓"};
        for (String k : keys) {
            TextView b2 = Ui.button(this, k, k.equals("✓")); b2.setTextSize(24);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Ui.dp(this, 86); lp.height = Ui.dp(this, 72); lp.setMargins(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
            b2.setOnClickListener(v -> key(k));
            g.addView(b2, lp);
        }
        root.addView(g);
        setContentView(root);
        render();
    }

    void key(String k) {
        int wait = d.lockoutSeconds();
        if (wait > 0) { msg.setText("Too many tries. Wait " + wait + "s"); msg.setTextColor(Ui.RED); return; }
        if (k.equals("⌫")) { if (pin.length() > 0) pin.deleteCharAt(pin.length() - 1); }
        else if (k.equals("✓")) check();
        else if (pin.length() < 8) { pin.append(k); }
        render();
    }

    void check() {
        if (pin.length() < 4) return;
        String p = pin.toString(); pin.setLength(0);
        msg.setText("Checking..."); msg.setTextColor(Ui.MUTED);
        new Thread(() -> {
            boolean ok = d.checkPin(p);
            h.post(() -> {
                if (ok) { App.unlocked = true; finish(); overridePendingTransition(0, 0); }
                else { msg.setText("Wrong PIN"); msg.setTextColor(Ui.RED); render(); }
            });
        }).start();
    }

    void render() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < Math.max(4, pin.length()); i++) s.append(i < pin.length() ? "● " : "○ ");
        dots.setText(s.toString().trim());
    }

    @Override public void onBackPressed() { moveTaskToBack(true); }
}
