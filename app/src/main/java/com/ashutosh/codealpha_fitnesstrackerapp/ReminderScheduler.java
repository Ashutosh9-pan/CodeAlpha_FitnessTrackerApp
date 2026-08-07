package com.ashutosh.codealpha_fitnesstrackerapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

final class ReminderScheduler {

    static final String PREFERENCES_NAME = "ReminderSettings";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_HOUR = "hour";
    static final String KEY_MINUTE = "minute";

    private static final int REMINDER_REQUEST_CODE = 1001;

    private ReminderScheduler() {
    }

    static void scheduleNext(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );

        if (!preferences.getBoolean(KEY_ENABLED, false)) {
            cancel(context);
            return;
        }

        int hour = preferences.getInt(KEY_HOUR, 6);
        int minute = preferences.getInt(KEY_MINUTE, 0);

        Calendar triggerCalendar = Calendar.getInstance();
        triggerCalendar.set(Calendar.HOUR_OF_DAY, hour);
        triggerCalendar.set(Calendar.MINUTE, minute);
        triggerCalendar.set(Calendar.SECOND, 0);
        triggerCalendar.set(Calendar.MILLISECOND, 0);

        if (triggerCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            triggerCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE
        );

        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context);
        long triggerAtMillis = triggerCalendar.getTimeInMillis();

        alarmManager.cancel(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE
        );

        PendingIntent pendingIntent = createPendingIntent(context);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);

        return PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}