package com.ethical.noor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Checks this app's own GitHub repo for a newer release and installs it.
 * Only https://github.com/ethical-programmer/noor-daily is ever contacted, and Android
 * refuses any APK that isn't signed with the same key, so a fake update can't be installed.
 */
public class Updater {
    static final String REPO = "ethical-programmer/noor-daily";
    static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    static final Handler ui = new Handler(Looper.getMainLooper());

    public static long currentBuild(Context c) {
        try { PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0); return pi.getLongVersionCode(); }
        catch (Exception e) { return 0; }
    }

    public static void check(Activity a) {
        ProgressDialog pd = ProgressDialog.show(a, null, "Checking for updates...", true, false);
        new Thread(() -> {
            try {
                JSONObject rel = new JSONObject(new String(get(API, null), "UTF-8"));
                String tag = rel.optString("tag_name", "");          // e.g. build-12
                String digits = tag.replaceAll("\\D", "");
                long latest = digits.isEmpty() ? 0 : Long.parseLong(digits);
                String url = null;
                JSONArray assets = rel.optJSONArray("assets");
                if (assets != null) for (int i = 0; i < assets.length(); i++) {
                    String u = assets.optJSONObject(i).optString("browser_download_url");
                    if (u.endsWith(".apk") && u.startsWith("https://github.com/" + REPO + "/")) url = u;
                }
                final String dl = url;
                ui.post(() -> {
                    pd.dismiss();
                    if (dl == null || latest <= currentBuild(a)) { Toast.makeText(a, "You're on the latest version", Toast.LENGTH_LONG).show(); return; }
                    new AlertDialog.Builder(a, android.R.style.Theme_Material_Dialog_Alert)
                            .setTitle("Update available")
                            .setMessage("Build " + latest + " is ready (you have build " + currentBuild(a) + ").\n\n" + rel.optString("name", ""))
                            .setPositiveButton("Update", (x, y) -> download(a, dl))
                            .setNegativeButton("Later", null).show();
                });
            } catch (Exception e) {
                ui.post(() -> { pd.dismiss(); Toast.makeText(a, "Couldn't check for updates. Check your internet.", Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    static void download(Activity a, String url) {
        if (!a.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(a, "Allow Noor Daily to install updates, then tap Update again", Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
            return;
        }
        ProgressDialog pd = new ProgressDialog(a);
        pd.setMessage("Downloading update..."); pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); pd.setMax(100); pd.setCancelable(false); pd.show();
        new Thread(() -> {
            try {
                byte[] apk = get(url, p -> ui.post(() -> pd.setProgress(p)));
                PackageInstaller pi = a.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sp.setAppPackageName(a.getPackageName());
                int id = pi.createSession(sp);
                try (PackageInstaller.Session s = pi.openSession(id)) {
                    try (OutputStream out = s.openWrite("update.apk", 0, apk.length)) { out.write(apk); s.fsync(out); }
                    Intent cb = new Intent(a, InstallReceiver.class);
                    PendingIntent p = PendingIntent.getBroadcast(a, 9, cb, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    s.commit(p.getIntentSender());
                }
                ui.post(pd::dismiss);
            } catch (Exception e) {
                ui.post(() -> { pd.dismiss(); Toast.makeText(a, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    interface Progress { void on(int pct); }

    static byte[] get(String url, Progress prog) throws Exception {
        String u = url;
        for (int hop = 0; hop < 5; hop++) {
            if (!u.startsWith("https://")) throw new Exception("insecure link");
            HttpsURLConnection c = (HttpsURLConnection) new URL(u).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000); c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "NoorDaily-Updater");
            c.setRequestProperty("Accept", prog == null ? "application/vnd.github+json" : "application/octet-stream");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) { u = c.getHeaderField("Location"); c.disconnect(); continue; }
            if (code != 200) throw new Exception("HTTP " + code);
            long len = c.getContentLengthLong();
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[16384]; int n; long got = 0; int last = -1;
                while ((n = in.read(buf)) > 0) {
                    bo.write(buf, 0, n); got += n;
                    if (prog != null && len > 0) { int pc = (int) (got * 100 / len); if (pc != last) { last = pc; prog.on(pc); } }
                }
                return bo.toByteArray();
            }
        }
        throw new Exception("too many redirects");
    }
}
