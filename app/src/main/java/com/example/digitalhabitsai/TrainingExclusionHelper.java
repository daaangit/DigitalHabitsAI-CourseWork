package com.example.digitalhabitsai;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TrainingExclusionHelper {
    private static final String KEY_EXCLUDED = "excluded_training_timestamps";

    private TrainingExclusionHelper() {
    }

    public static void markExcluded(SharedPreferences prefs, long timestamp) {
        Set<String> excluded = new HashSet<>(prefs.getStringSet(KEY_EXCLUDED, new HashSet<>()));
        excluded.add(String.valueOf(timestamp));
        prefs.edit().putStringSet(KEY_EXCLUDED, excluded).apply();
    }

    public static List<DailyRecord> filterTrainingRecords(SharedPreferences prefs, List<DailyRecord> records) {
        Set<String> excluded = prefs.getStringSet(KEY_EXCLUDED, new HashSet<>());
        List<DailyRecord> filtered = new ArrayList<>();
        for (DailyRecord record : records) {
            if (!excluded.contains(String.valueOf(record.timestamp))) {
                filtered.add(record);
            }
        }
        return filtered;
    }
}
