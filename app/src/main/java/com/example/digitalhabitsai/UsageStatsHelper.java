package com.example.digitalhabitsai;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageStatsHelper {
    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final AppCategoryManager categoryManager;

    public UsageStatsHelper(Context context, AppCategoryManager categoryManager) {
        this.context = context.getApplicationContext();
        this.usageStatsManager = (UsageStatsManager) this.context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.categoryManager = categoryManager;
    }

    public UsageSnapshot collectToday() {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        Map<String, Long> foregroundMillis = collectForegroundMillisByEvents(start, now);
        if (foregroundMillis.isEmpty()) {
            mergeWithUsageStatsFallback(foregroundMillis, start, now);
        }

        double night = collectNightUsageMinutes(start, now);
        double unlocks = collectUnlockCount(start, now);

        List<AppUsageEntry> apps = new ArrayList<>();
        double social = 0.0;
        double games = 0.0;
        double work = 0.0;

        for (String packageName : foregroundMillis.keySet()) {
            if (packageName == null) continue;
            if (packageName.equals(context.getPackageName())) continue;

            long millis = foregroundMillis.get(packageName);
            if (millis <= 0) continue;

            double minutes = millis / 60000.0;
            if (minutes < 0.15) continue;

            String appName = categoryManager.getAppName(packageName);
            String category = categoryManager.categoryFor(packageName, appName);

            if (AppCategoryManager.SOCIAL.equals(category)) social += minutes;
            else if (AppCategoryManager.GAME.equals(category)) games += minutes;
            else if (AppCategoryManager.WORK.equals(category)) work += minutes;

            apps.add(new AppUsageEntry(packageName, appName, category, minutes));
        }

        Collections.sort(apps, new Comparator<AppUsageEntry>() {
            @Override
            public int compare(AppUsageEntry a, AppUsageEntry b) {
                return Double.compare(b.minutes, a.minutes);
            }
        });


        return new UsageSnapshot(round(social), round(games), round(work), round(night), round(unlocks), apps);
    }

    private Map<String, Long> collectForegroundMillisByEvents(long start, long end) {
        Map<String, Long> totals = new HashMap<>();
        Map<String, Long> activeStarts = new HashMap<>();

        UsageEvents events = usageStatsManager.queryEvents(start, end);
        if (events == null) return totals;

        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            if (pkg == null) continue;

            int type = event.getEventType();
            if (isForegroundEvent(type)) {
                activeStarts.put(pkg, event.getTimeStamp());
            } else if (isBackgroundEvent(type)) {
                Long fgStart = activeStarts.remove(pkg);
                if (fgStart != null && event.getTimeStamp() > fgStart) {
                    addMillis(totals, pkg, event.getTimeStamp() - fgStart);
                }
            }
        }

        for (String pkg : activeStarts.keySet()) {
            Long fgStart = activeStarts.get(pkg);
            if (fgStart != null && end > fgStart) {
                addMillis(totals, pkg, end - fgStart);
            }
        }
        return totals;
    }

    private void mergeWithUsageStatsFallback(Map<String, Long> totals, long start, long end) {
        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) return;

        for (UsageStats usage : stats) {
            String pkg = usage.getPackageName();
            if (pkg == null) continue;

            long millis = usage.getTotalTimeInForeground();
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    millis = Math.max(millis, usage.getTotalTimeVisible());
                } catch (Exception ignored) {
                }
            }

            Long current = totals.get(pkg);
            if (current == null || millis > current) {
                totals.put(pkg, millis);
            }
        }
    }

    private void addMillis(Map<String, Long> map, String packageName, long millis) {
        Long old = map.get(packageName);
        map.put(packageName, (old == null ? 0L : old) + Math.max(0L, millis));
    }

    private double collectUnlockCount(long start, long end) {
        double count = 0;
        UsageEvents events = usageStatsManager.queryEvents(start, end);
        if (events == null) return count;

        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type == UsageEvents.Event.SCREEN_INTERACTIVE) {
                count++;
            }
        }
        return count;
    }

    private double collectNightUsageMinutes(long start, long end) {
        UsageEvents events = usageStatsManager.queryEvents(start, end);
        if (events == null) return 0.0;

        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Long> foregroundStart = new HashMap<>();
        double nightMillis = 0.0;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            if (pkg == null) continue;
            if (pkg.equals(context.getPackageName())) continue;

            int type = event.getEventType();
            if (isForegroundEvent(type)) {
                foregroundStart.put(pkg, event.getTimeStamp());
            } else if (isBackgroundEvent(type)) {
                Long fgStart = foregroundStart.remove(pkg);
                if (fgStart != null && event.getTimeStamp() > fgStart) {
                    nightMillis += overlapWithNightMillis(fgStart, event.getTimeStamp());
                }
            }
        }

        for (Long fgStart : foregroundStart.values()) {
            if (end > fgStart) {
                nightMillis += overlapWithNightMillis(fgStart, end);
            }
        }
        return nightMillis / 60000.0;
    }

    private boolean isForegroundEvent(int type) {
        if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) return true;
        return Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_RESUMED;
    }

    private boolean isBackgroundEvent(int type) {
        if (type == UsageEvents.Event.MOVE_TO_BACKGROUND) return true;
        return Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_PAUSED;
    }

    private long overlapWithNightMillis(long start, long end) {
        long total = 0;
        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(start);

        while (cursor.getTimeInMillis() < end) {
            Calendar next = (Calendar) cursor.clone();
            next.add(Calendar.MINUTE, 1);
            long segmentStart = cursor.getTimeInMillis();
            long segmentEnd = Math.min(next.getTimeInMillis(), end);
            int hour = cursor.get(Calendar.HOUR_OF_DAY);
            if (hour < 5) {
                total += Math.max(0, segmentEnd - segmentStart);
            }
            cursor = next;
        }
        return total;
    }


    private double round(double value) {
        return Math.round(value);
    }
}
