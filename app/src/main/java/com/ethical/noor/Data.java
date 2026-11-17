package com.ethical.noor;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** All app data, stored privately on the phone (never leaves the device). */
public class Data {
    final Context c;
    final SharedPreferences p;

    public Data(Context ctx) {
        c = ctx.getApplicationContext();
        p = c.getSharedPreferences("noor", Context.MODE_PRIVATE);
        if (!p.contains("tasks")) seedDefaults();
    }

    // ---------------- dates ----------------
    public static String key(long t) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t)); }
    public static String today() { return key(System.currentTimeMillis()); }
    public static String daysAgo(int n) { Calendar k = Calendar.getInstance(); k.add(Calendar.DAY_OF_YEAR, -n); return key(k.getTimeInMillis()); }
    public static int nowMin() { Calendar k = Calendar.getInstance(); return k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE); }
    public static String hm(int min) {
        int h = (min / 60) % 24, m = min % 60;
        return String.format(Locale.US, "%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM");
    }
    public static String fmt(int n) { return String.format(Locale.US, "%,d", n); }

    // ---------------- profile ----------------
    public int age() { return p.getInt("age", 25); }
    public int height() { return p.getInt("height", 170); }
    public int weight() { return p.getInt("weight", 70); }
    public boolean female() { return p.getBoolean("female", false); }
    public int activity() { return p.getInt("activity", 1); } // 0 low, 1 moderate, 2 high
    public void setInt(String k, int v) { p.edit().putInt(k, v).apply(); }
    public void setBool(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }
    public int getInt(String k, int def) { return p.getInt(k, def); }
    public boolean getBool(String k, boolean def) { return p.getBoolean(k, def); }

    /** Daily water target in ml from age, weight, sex and activity. */
    public int waterGoal() {
        double perKg = age() < 30 ? 40 : age() <= 55 ? 35 : 30;
        double g = weight() * perKg;
        if (female()) g *= 0.92;
        g += activity() * 350;
        int r = (int) (Math.round(g / 50.0) * 50);
        return Math.max(1500, Math.min(5000, r));
    }
    public double bmi() { double h = height() / 100.0; return h <= 0 ? 0 : weight() / (h * h); }
    public static String bmiLabel(double b) { return b < 18.5 ? "Underweight" : b < 25 ? "Healthy" : b < 30 ? "Overweight" : "Obese"; }

    public int glass() { return p.getInt("glass", 250); }
    public int wake() { return p.getInt("wake", 7 * 60); }
    public int sleep() { return p.getInt("sleep", 23 * 60); }
    public boolean awake(int m) { int w = wake(), z = sleep(); return w < z ? (m >= w && m < z) : (m >= w || m < z); }
    public int waterInterval() {
        int awake = sleep() - wake(); if (awake <= 0) awake += 1440;
        int glasses = (int) Math.ceil(waterGoal() / (double) glass());
        return Math.max(30, awake / Math.max(1, glasses));
    }

    // ---------------- per-day data ----------------
    JSONObject day(String d) {
        try { String s = p.getString("d_" + d, null); return s == null ? new JSONObject() : new JSONObject(s); }
        catch (Exception e) { return new JSONObject(); }
    }
    void save(String d, JSONObject o) {
        p.edit().putString("d_" + d, o.toString()).apply();
    }
    static void put(JSONObject o, String k, Object v) { try { o.put(k, v); } catch (Exception ignored) {} }

    public int water(String d) { return day(d).optInt("water", 0); }
    public void addWater(int ml) {
        String d = today(); JSONObject o = day(d);
        put(o, "water", Math.max(0, o.optInt("water", 0) + ml));
        JSONArray log = o.optJSONArray("wlog"); if (log == null) log = new JSONArray();
        log.put(nowMin() * 10000 + Math.max(0, ml));
        put(o, "wlog", log);
        save(d, o);
    }
    public void undoWater() {
        String d = today(); JSONObject o = day(d);
        JSONArray log = o.optJSONArray("wlog");
        if (log == null || log.length() == 0) return;
        int last = log.optInt(log.length() - 1); log.remove(log.length() - 1);
        put(o, "water", Math.max(0, o.optInt("water", 0) - last % 10000));
        put(o, "wlog", log); save(d, o);
    }
    /** list of {minuteOfDay, ml} */
    public List<int[]> waterLog(String d) {
        List<int[]> out = new ArrayList<>();
        JSONArray log = day(d).optJSONArray("wlog");
        if (log != null) for (int i = 0; i < log.length(); i++) { int v = log.optInt(i); out.add(new int[]{v / 10000, v % 10000}); }
        return out;
    }
    public float[] waterByHour(String d) {
        float[] h = new float[24];
        for (int[] e : waterLog(d)) h[Math.min(23, e[0] / 60)] += e[1];
        return h;
    }

    // ---------------- tasks ----------------
    void seedDefaults() {
        JSONArray t = new JSONArray();
        t.put(task("walk", "🚶", "Walk", "count", 30, "min", 5));
        t.put(task("stretch", "🧘", "Stretching", "count", 3, "times", 1));
        t.put(task("stairs", "🪜", "Stairs up & down", "count", 10, "floors", 1));
        t.put(task("eyes", "👀", "Eye rest (20-20-20)", "count", 6, "times", 1));
        t.put(task("breath", "🌬️", "Deep breathing", "count", 5, "min", 1));
        t.put(task("sun", "☀️", "Sunlight / fresh air", "tick", 1, "", 1));
        t.put(task("read", "📖", "Read or learn 15 min", "tick", 1, "", 1));
        t.put(task("junk", "🥗", "No junk food or soda", "tick", 1, "", 1));
        t.put(task("sleep", "😴", "Phone away 30 min before sleep", "tick", 1, "", 1));
        JSONArray z = new JSONArray();
        z.put(zikr("subhan", "SubhanAllah", "سُبْحَانَ ٱللَّٰهِ", 33));
        z.put(zikr("hamd", "Alhamdulillah", "ٱلْحَمْدُ لِلَّٰهِ", 33));
        z.put(zikr("akbar", "Allahu Akbar", "ٱللَّٰهُ أَكْبَرُ", 34));
        z.put(zikr("istighfar", "Astaghfirullah", "أَسْتَغْفِرُ ٱللَّٰهَ", 100));
        z.put(zikr("tahlil", "La ilaha illallah", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", 100));
        z.put(zikr("durood", "Durood Shareef", "ٱللَّٰهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", 100));
        p.edit().putString("tasks", t.toString()).putString("zikr", z.toString()).apply();
    }
    static JSONObject task(String id, String emoji, String name, String type, int target, String unit, int step) {
        JSONObject o = new JSONObject();
        put(o, "id", id); put(o, "emoji", emoji); put(o, "name", name); put(o, "type", type);
        put(o, "target", target); put(o, "unit", unit); put(o, "step", step);
        return o;
    }
    static JSONObject zikr(String id, String name, String ar, int target) {
        JSONObject o = new JSONObject();
        put(o, "id", id); put(o, "name", name); put(o, "ar", ar); put(o, "target", target);
        return o;
    }
    JSONArray arr(String k) { try { return new JSONArray(p.getString(k, "[]")); } catch (Exception e) { return new JSONArray(); } }
    public List<JSONObject> tasks() { List<JSONObject> l = new ArrayList<>(); JSONArray a = arr("tasks"); for (int i = 0; i < a.length(); i++) l.add(a.optJSONObject(i)); return l; }
    public List<JSONObject> zikrs() { List<JSONObject> l = new ArrayList<>(); JSONArray a = arr("zikr"); for (int i = 0; i < a.length(); i++) l.add(a.optJSONObject(i)); return l; }
    void saveList(String k, List<JSONObject> l) { JSONArray a = new JSONArray(); for (JSONObject o : l) a.put(o); p.edit().putString(k, a.toString()).apply(); }
    public void addTask(JSONObject t) { List<JSONObject> l = tasks(); l.add(t); saveList("tasks", l); }
    public void removeTask(String id) { List<JSONObject> l = tasks(); for (int i = l.size() - 1; i >= 0; i--) if (id.equals(l.get(i).optString("id"))) l.remove(i); saveList("tasks", l); }
    public void addZikr(JSONObject z) { List<JSONObject> l = zikrs(); l.add(z); saveList("zikr", l); }
    public void removeZikr(String id) { List<JSONObject> l = zikrs(); for (int i = l.size() - 1; i >= 0; i--) if (id.equals(l.get(i).optString("id"))) l.remove(i); saveList("zikr", l); }

    public int taskVal(String d, String id) { JSONObject t = day(d).optJSONObject("tasks"); return t == null ? 0 : t.optInt(id, 0); }
    public void setTaskVal(String id, int v) {
        String d = today(); JSONObject o = day(d);
        JSONObject t = o.optJSONObject("tasks"); if (t == null) t = new JSONObject();
        put(t, id, Math.max(0, v)); put(o, "tasks", t); save(d, o);
    }
    public boolean taskDone(String d, JSONObject t) { return taskVal(d, t.optString("id")) >= t.optInt("target", 1); }
    public int tasksDone(String d) { int n = 0; for (JSONObject t : tasks()) if (taskDone(d, t)) n++; return n; }

    public int zikrCount(String d, String id) { JSONObject z = day(d).optJSONObject("zikr"); return z == null ? 0 : z.optInt(id, 0); }
    public void setZikr(String id, int v) {
        String d = today(); JSONObject o = day(d);
        JSONObject z = o.optJSONObject("zikr"); if (z == null) z = new JSONObject();
        put(z, id, Math.max(0, v)); put(o, "zikr", z); save(d, o);
    }
    public boolean zikrDone(String d, JSONObject z) { return zikrCount(d, z.optString("id")) >= z.optInt("target", 33); }
    public int zikrsDone(String d) { int n = 0; for (JSONObject z : zikrs()) if (zikrDone(d, z)) n++; return n; }

    // ---------------- scores ----------------
    public float waterPct(String d) { return Math.min(1f, water(d) / (float) waterGoal()); }
    public float taskPct(String d) { int n = tasks().size(); if (n == 0) return 1f; float s = 0; for (JSONObject t : tasks()) s += Math.min(1f, taskVal(d, t.optString("id")) / (float) Math.max(1, t.optInt("target", 1))); return s / n; }
    public float zikrPct(String d) { int n = zikrs().size(); if (n == 0) return 1f; float s = 0; for (JSONObject z : zikrs()) s += Math.min(1f, zikrCount(d, z.optString("id")) / (float) Math.max(1, z.optInt("target", 33))); return s / n; }
    public int score(String d) { return Math.round((waterPct(d) + taskPct(d) + zikrPct(d)) / 3f * 100); }
    public int streak() {
        int s = 0;
        for (int i = (score(today()) >= 70 ? 0 : 1); i < 365; i++) { if (score(daysAgo(i)) >= 70) s++; else break; }
        return s;
    }

    // ---------------- security ----------------
    public boolean hasPin() { return p.contains("pin_hash"); }
    static String sha(String s) {
        try { MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] b = md.digest(s.getBytes("UTF-8")); StringBuilder sb = new StringBuilder(); for (byte x : b) sb.append(String.format("%02x", x)); return sb.toString(); }
        catch (Exception e) { return ""; }
    }
    public void setPin(String pin) {
        byte[] r = new byte[16]; new SecureRandom().nextBytes(r);
        StringBuilder salt = new StringBuilder(); for (byte x : r) salt.append(String.format("%02x", x));
        String h = pin;
        for (int i = 0; i < 20000; i++) h = sha(salt + h);
        p.edit().putString("pin_salt", salt.toString()).putString("pin_hash", h).putInt("pin_fail", 0).apply();
    }
    public boolean checkPin(String pin) {
        String salt = p.getString("pin_salt", ""), h = pin;
        for (int i = 0; i < 20000; i++) h = sha(salt + h);
        boolean ok = h.equals(p.getString("pin_hash", "-"));
        p.edit().putInt("pin_fail", ok ? 0 : p.getInt("pin_fail", 0) + 1).putLong("pin_last", System.currentTimeMillis()).apply();
        return ok;
    }
    public void clearPin() { p.edit().remove("pin_hash").remove("pin_salt").apply(); }
    /** seconds the user must wait after repeated wrong PINs */
    public int lockoutSeconds() {
        int f = p.getInt("pin_fail", 0); if (f < 5) return 0;
        long wait = 30L * (1L << Math.min(6, f - 5)) * 1000L;
        long left = p.getLong("pin_last", 0) + wait - System.currentTimeMillis();
        return (int) Math.max(0, left / 1000);
    }
}
