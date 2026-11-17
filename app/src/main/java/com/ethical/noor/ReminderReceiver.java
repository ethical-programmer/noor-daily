package com.ethical.noor;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent in) {
        String a = in.getAction(); if (a == null) return;
        Data d = new Data(c); String today = Data.today();
        switch (a) {
            case Alarms.WATER: {
                if (d.awake(Data.nowMin()) && d.water(today) < d.waterGoal()) {
                    int left = d.waterGoal() - d.water(today);
                    Intent add = new Intent(c, ReminderReceiver.class).setAction(Alarms.ADD_WATER);
                    PendingIntent pa = PendingIntent.getBroadcast(c, 77, add, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Notification.Action act = new Notification.Action.Builder(Icon.createWithResource(c, android.R.drawable.ic_input_add), "I drank " + d.glass() + " ml", pa).build();
                    Notifs.show(c, Notifs.N_WATER, Notifs.GENTLE, "💧 Time for some water", "Drink " + d.glass() + " ml. " + Data.fmt(left) + " ml left to reach today's goal.", 1, act);
                }
                Alarms.scheduleWater(c);
                break;
            }
            case Alarms.ADD_WATER: {
                d.addWater(d.glass());
                Notifs.cancel(c, Notifs.N_WATER);
                Alarms.scheduleWater(c);
                break;
            }
            case Alarms.WARN1:
            case Alarms.WARN2: {
                List<String> left = pending(d, today);
                if (!left.isEmpty()) {
                    boolean last = Alarms.WARN2.equals(a);
                    String title = last ? "⚠️ Day is almost over" : "⏳ " + left.size() + " things still left today";
                    String body = (last ? "You still haven't done: " : "Still to do: ") + String.join(", ", left.subList(0, Math.min(6, left.size()))) + (left.size() > 6 ? " and more." : ".");
                    Notifs.show(c, Notifs.N_WARN, Notifs.GENTLE, title, body, 2, null);
                }
                Alarms.at(c, a, Alarms.next(d.getInt(Alarms.WARN1.equals(a) ? "warn1" : "warn2", Alarms.WARN1.equals(a) ? 20 * 60 : 22 * 60 + 30)));
                break;
            }
            case Alarms.REPORT: {
                int score = d.score(today);
                String body = "Score " + score + "%  ·  Water " + Data.fmt(d.water(today)) + "/" + Data.fmt(d.waterGoal()) + " ml  ·  Tasks " + d.tasksDone(today) + "/" + d.tasks().size() + "  ·  Tasbeeh " + d.zikrsDone(today) + "/" + d.zikrs().size()
                        + (score >= 70 ? "\nGreat day, MashaAllah! Keep the streak going 🔥" : "\nTomorrow is a new chance. You've got this 💪");
                Notifs.show(c, Notifs.N_REPORT, Notifs.REPORT, "🌙 Your daily report", body, 4, null);
                Alarms.at(c, Alarms.REPORT, Alarms.next(23 * 60 + 59));
                break;
            }
            case Alarms.FOCUS: {
                d.p.edit().putLong("focusEnd", 0).putInt("focusDone_" + today, d.getInt("focusDone_" + today, 0) + 1).apply();
                Notifs.show(c, Notifs.N_FOCUS, Notifs.GENTLE, "🎯 Focus session complete", "Nice work! Stand up, stretch and drink some water for 5 minutes.", 0, null);
                break;
            }
        }
    }

    static List<String> pending(Data d, String day) {
        List<String> out = new ArrayList<>();
        if (d.water(day) < d.waterGoal()) out.add("water (" + Data.fmt(d.waterGoal() - d.water(day)) + " ml left)");
        for (JSONObject t : d.tasks()) if (!d.taskDone(day, t)) out.add(t.optString("name").toLowerCase());
        int z = d.zikrs().size() - d.zikrsDone(day);
        if (z > 0) out.add(z + " tasbeeh");
        return out;
    }
}
