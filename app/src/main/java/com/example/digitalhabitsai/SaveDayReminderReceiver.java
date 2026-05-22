package com.example.digitalhabitsai;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class SaveDayReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Не забудьте сохранить день")
                .setContentText("Откройте вкладку «День» и сохраните данные за сегодня.")
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("save_day_reminders");
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }

        scheduleNextReminder(context, intent);
    }

    private void scheduleNextReminder(Context context, Intent receivedIntent) {
        int hour = receivedIntent != null ? receivedIntent.getIntExtra("hour", -1) : -1;
        int minute = receivedIntent != null ? receivedIntent.getIntExtra("minute", -1) : -1;
        if (hour < 0 || minute < 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent nextIntent = new Intent(context, SaveDayReminderReceiver.class);
        nextIntent.putExtra("hour", hour);
        nextIntent.putExtra("minute", minute);

        int requestCode = hour * 100 + minute;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar trigger = Calendar.getInstance();
        trigger.add(Calendar.DAY_OF_YEAR, 1);
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            trigger.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            trigger.getTimeInMillis(),
                            pendingIntent
                    );
                }
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            }
        } catch (SecurityException se) {
            Log.w("SaveDayReminder", "Exact alarm denied, fallback to inexact", se);
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trigger.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Throwable t) {
            Log.e("SaveDayReminder", "Failed to schedule next reminder", t);
        }
    }
}