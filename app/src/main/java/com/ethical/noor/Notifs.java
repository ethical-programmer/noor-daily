package com.ethical.noor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Quiet notifications: no ringtone, just one gentle buzz. */
public class Notifs {
    public static final String GENTLE = "gentle_v1", REPORT = "report_v1";
    public static final int N_WATER = 1, N_WARN = 2, N_REPORT = 3, N_FOCUS = 4;

    public static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel a = new NotificationChannel(GENTLE, "Reminders", NotificationManager.IMPORTANCE_DEFAULT);
        a.setDescription("Water, tasks and focus reminders (no ringtone)");
        a.setSound(null, null); a.enableVibration(true); a.setVibrationPattern(new long[]{0, 120});
        NotificationChannel b = new NotificationChannel(REPORT, "Daily report", NotificationManager.IMPORTANCE_DEFAULT);
        b.setDescription("Your end-of-day summary"); b.setSound(null, null); b.enableVibration(true); b.setVibrationPattern(new long[]{0, 120});
        nm.createNotificationChannel(a); nm.createNotificationChannel(b);
    }

    static PendingIntent open(Context c, int tab) {
        Intent i = new Intent(c, MainActivity.class).putExtra("tab", tab).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 200 + tab, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void show(Context c, int id, String ch, String title, String text, int tab, Notification.Action action) {
        Notification.Builder b = new Notification.Builder(c, ch)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(Ui.ACCENT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setContentIntent(open(c, tab));
        if (action != null) b.addAction(action);
        c.getSystemService(NotificationManager.class).notify(id, b.build());
    }
    public static void cancel(Context c, int id) { c.getSystemService(NotificationManager.class).cancel(id); }
}
