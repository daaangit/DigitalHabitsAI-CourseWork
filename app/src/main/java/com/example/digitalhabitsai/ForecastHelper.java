package com.example.digitalhabitsai;

import java.util.Calendar;
import java.util.Locale;

public final class ForecastHelper {
    private ForecastHelper() {
    }

    public static String buildEndOfDayForecast(DailyRecord current, PersonalLinearModel model) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        double elapsedHours = Math.max(1.0, hour + minute / 60.0);
        double multiplier = Math.min(3.0, Math.max(1.0, 24.0 / elapsedHours));

        double eodPrediction = model.predict(
                current.socialMins * multiplier,
                current.gameMins * multiplier,
                current.workMins * multiplier,
                current.nightMins * multiplier,
                current.unlockCount * multiplier
        );

        return String.format(
                Locale.US,
                "Прогноз к концу дня: %.1f / 10 (если текущий темп сохранится)",
                eodPrediction
        );
    }
}
