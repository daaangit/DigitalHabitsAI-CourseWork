package com.example.digitalhabitsai;

import java.util.Locale;

public final class UsageSummaryFormatter {
    private UsageSummaryFormatter() {
    }

    public static String dashboardUsageText(DailyRecord current, boolean hasSnapshot) {
        return String.format(
                Locale.US,
                        " Соцсети: %.0f мин\n" +
                        " Игры: %.0f мин\n" +
                        " Работа/учёба: %.0f мин\n" +
                        " Ночное использование: %.0f мин\n" +
                        " Разблокировки: %.0f",
                current.socialMins,
                current.gameMins,
                current.workMins,
                current.nightMins,
                current.unlockCount
        );
    }
}
