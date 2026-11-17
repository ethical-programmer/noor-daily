package com.ethical.noor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class Alarms {
    public static final String WATER = "noor.WATER", WARN1 = "noor.WARN1", WARN2 = "noor.WARN2", REPORT = "noor.REPORT", FOCUS = "noor.FOCUS", ADD_WATER = "noor.ADD_WATER";

    static PendingIntent pi(Context c, String action) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, action.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void at(Context c, String action, long t) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(c, action));
    }
    static void cancel(Context c, String action) { c.getSystemService(AlarmManager.class).cancel(pi(c, action)); }

    /** next time the clock shows minuteOfDay (today or tomorrow) */
    static long next(int minuteOfDay) {
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60); k.set(Calendar.MINUTE, minuteOfDay % 60);
        k.set(Calendar.SECOND, 0); k.set(Calendar.MILLISECOND, 0);
        if (k.getTimeInMillis() <= System.currentTimeMillis() + 5000) k.add(Calendar.DAY_OF_YEAR, 1);
        return k.getTimeInMillis();
    }

    /** safe to call often: only re-arms the water timer if it is missing */
    public static void ensure(Context c) {
        Data d = new Data(c);
        if (d.p.getLong("waterNextAt", 0) < System.currentTimeMillis() - 60_000) scheduleWater(c);
        scheduleDaily(c);
    }

    public static void scheduleAll(Context c) {
        scheduleWater(c);
        scheduleDaily(c);
    }

    static void scheduleDaily(Context c) {
        Data d = new Data(c);
        if (d.getBool("warnOn", true)) { at(c, WARN1, next(d.getInt("warn1", 20 * 60))); at(c, WARN2, next(d.getInt("warn2", 22 * 60 + 30))); }
        else { cancel(c, WARN1); cancel(c, WARN2); }
        if (d.getBool("reportOn", true)) at(c, REPORT, next(23 * 60 + 59)); else cancel(c, REPORT);
    }

    public static void scheduleWater(Context c) {
        Data d = new Data(c);
        if (!d.getBool("waterOn", true)) { cancel(c, WATER); d.setInt("waterNext", 0); return; }
        long t = System.currentTimeMillis() + d.waterInterval() * 60_000L;
        Calendar k = Calendar.getInstance(); k.setTimeInMillis(t);
        int m = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE);
        if (!d.awake(m) || d.water(Data.today()) >= d.waterGoal()) t = next((d.wake() + 20) % 1440);
        at(c, WATER, t);
        d.p.edit().putLong("waterNextAt", t).apply();
    }

    public static void startFocus(Context c, int minutes) {
        long end = System.currentTimeMillis() + minutes * 60_000L;
        new Data(c).p.edit().putLong("focusEnd", end).putInt("focusMin", minutes).apply();
        at(c, FOCUS, end);
    }
    public static void stopFocus(Context c) { new Data(c).p.edit().putLong("focusEnd", 0).apply(); cancel(c, FOCUS); }
}
