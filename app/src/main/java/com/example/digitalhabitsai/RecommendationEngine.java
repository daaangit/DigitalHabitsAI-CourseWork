package com.example.digitalhabitsai;

import java.util.Locale;

public class RecommendationEngine {
    public String buildRecommendation(PersonalLinearModel model, DailyRecord today) {
        double modelScore = model.predict(
                today.socialMins,
                today.gameMins,
                today.workMins,
                today.nightMins,
                today.unlockCount
        );

        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Оценка продуктивности по модели: %.1f / 10\n\n", modelScore));

        if (model.trainingDays < 5) {
            builder.append("Модель ещё калибруется. Рекомендации предварительные: для устойчивой персонализации нужно хотя бы 5–7 оценённых дней.\n\n");
        }

        String mainFactor = model.mainProblemFactor(today);
        builder.append("Главный фактор текущего дня: ").append(mainFactor).append(".\n\n");
        builder.append(adviceForFactor(mainFactor));

        PersonalLinearModel.FactorScore[] scores = model.factorScores(today);
        builder.append("\n\nПроблемность факторов:\n");
        for (PersonalLinearModel.FactorScore score : scores) {
            builder.append(String.format(Locale.US, "%s: %.2f\n", score.name, score.problemScore));
        }

        builder.append("\nВклад в оценку модели:\n");
        for (PersonalLinearModel.FactorScore score : scores) {
            builder.append(String.format(Locale.US, "%s: %.2f\n", score.name, score.modelContribution));
        }

        return builder.toString().trim();
    }

    public String adviceForFactor(String factor) {
        switch (factor) {
            case "соцсети":
                return "Рекомендация: ограничить соцсети короткими сессиями по 10–15 минут и проверить, не проседает ли оценка дня при росте времени в соцсетях.";
            case "игры":
                return "Рекомендация: перенести игровые сессии на конец дня и ограничить длительность одной сессии.";
            case "ночное использование":
                return "Рекомендация: отключить уведомления вечером и убрать телефон за 30–60 минут до сна.";
            case "частые разблокировки":
                return "Рекомендация: отключить лишние уведомления и проверять телефон пакетно, а не каждые несколько минут.";
            case "недостаток работы/учёбы":
            case "рабочие/учебные приложения":
            case "работа/учёба":
                return "Рекомендация: в дни с низким временем в рабочих/учебных приложениях стоит заранее выделять отдельные фокус-блоки для важных задач.";
            case "нет выраженного фактора":
                return "Рекомендация: выраженного негативного фактора пока не видно. Продолжайте накапливать историю и оценивать дни.";
            default:
                return "Рекомендация: снизить наиболее отвлекающие цифровые привычки и повторно оценить день вечером.";
        }
    }
}
