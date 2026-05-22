package com.example.digitalhabitsai;

import android.content.SharedPreferences;

import java.util.List;
import java.util.Locale;

public class PersonalLinearModel {
    public static final int FEATURE_COUNT = 5;

    public double bias = 5.0;
    public double wSocial = 0.0;
    public double wGames = 0.0;
    public double wWork = 0.0;
    public double wNight = 0.0;
    public double wUnlocks = 0.0;
    public int trainingDays = 0;

    private static final String KEY_BIAS = "model_bias";
    private static final String KEY_W_SOCIAL = "model_w_social";
    private static final String KEY_W_GAMES = "model_w_games";
    private static final String KEY_W_WORK = "model_w_work";
    private static final String KEY_W_NIGHT = "model_w_night";
    private static final String KEY_W_UNLOCKS = "model_w_unlocks";
    private static final String KEY_TRAINING_DAYS = "model_training_days";

    private static final double DEFAULT_BIAS = 5.0;
    private static final double WEIGHT_LIMIT = 4.0;

    public double predict(double socialMins, double gameMins, double workMins, double nightMins, double unlockCount) {
        double[] x = normalize(socialMins, gameMins, workMins, nightMins, unlockCount);
        return predictNormalized(x);
    }

    public double predictNormalized(double[] x) {
        double y = bias
                + wSocial * x[0]
                + wGames * x[1]
                + wWork * x[2]
                + wNight * x[3]
                + wUnlocks * x[4];
        return clip(y, 0.0, 10.0);
    }

    public UpdateResult update(DailyRecord record, double ignoredLearningRate) {
        double oldPrediction = predict(
                record.socialMins,
                record.gameMins,
                record.workMins,
                record.nightMins,
                record.unlockCount
        );
        double error = record.actualProductivity - oldPrediction;

        double[] x = record.normalizedFeatures();
        double lr = 0.015;
        bias += lr * error;
        wSocial += lr * error * x[0];
        wGames += lr * error * x[1];
        wWork += lr * error * x[2];
        wNight += lr * error * x[3];
        wUnlocks += lr * error * x[4];
        applyConstraints();
        trainingDays++;

        double newPrediction = predictNormalized(x);
        return new UpdateResult(oldPrediction, newPrediction, error);
    }

    public UpdateResult retrainFromHistory(List<DailyRecord> records, DailyRecord latestRecord, double oldPrediction) {
        if (records == null || records.isEmpty()) {
            return new UpdateResult(oldPrediction, oldPrediction, 0.0);
        }

        double error = latestRecord.actualProductivity - oldPrediction;
        fitBatch(records);
        double newPrediction = predict(
                latestRecord.socialMins,
                latestRecord.gameMins,
                latestRecord.workMins,
                latestRecord.nightMins,
                latestRecord.unlockCount
        );
        return new UpdateResult(oldPrediction, newPrediction, error);
    }

    private void fitBatch(List<DailyRecord> records) {
        resetWeightsOnly();
        trainingDays = records.size();

        double mean = 0.0;
        for (DailyRecord r : records) {
            mean += r.actualProductivity;
        }
        bias = clip(mean / records.size(), 0.0, 10.0);

        int epochs = records.size() < 8 ? 500 : 850;
        double lr = records.size() < 8 ? 0.020 : 0.014;
        double l2 = 0.018;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double gradBias = 0.0;
            double gradSocial = 0.0;
            double gradGames = 0.0;
            double gradWork = 0.0;
            double gradNight = 0.0;
            double gradUnlocks = 0.0;

            for (DailyRecord r : records) {
                double[] x = r.normalizedFeatures();
                double pred = rawPredictNormalized(x);
                double err = pred - r.actualProductivity;

                gradBias += err;
                gradSocial += err * x[0];
                gradGames += err * x[1];
                gradWork += err * x[2];
                gradNight += err * x[3];
                gradUnlocks += err * x[4];
            }

            double n = records.size();
            bias -= lr * (gradBias / n);
            wSocial -= lr * ((gradSocial / n) + l2 * wSocial);
            wGames -= lr * ((gradGames / n) + l2 * wGames);
            wWork -= lr * ((gradWork / n) + l2 * wWork);
            wNight -= lr * ((gradNight / n) + l2 * wNight);
            wUnlocks -= lr * ((gradUnlocks / n) + l2 * wUnlocks);

            applyConstraints();
        }
    }

    private double rawPredictNormalized(double[] x) {
        return bias
                + wSocial * x[0]
                + wGames * x[1]
                + wWork * x[2]
                + wNight * x[3]
                + wUnlocks * x[4];
    }

    private void resetWeightsOnly() {
        bias = DEFAULT_BIAS;
        wSocial = 0.0;
        wGames = 0.0;
        wWork = 0.0;
        wNight = 0.0;
        wUnlocks = 0.0;
    }

    private void applyConstraints() {
        bias = clip(bias, 0.0, 10.0);

        wSocial = clip(wSocial, -WEIGHT_LIMIT, 0.0);
        wGames = clip(wGames, -WEIGHT_LIMIT, 0.0);
        wNight = clip(wNight, -WEIGHT_LIMIT, 0.0);
        wUnlocks = clip(wUnlocks, -WEIGHT_LIMIT, 0.0);
        wWork = clip(wWork, 0.0, WEIGHT_LIMIT);
    }

    public FactorScore[] factorScores(DailyRecord today) {
        double[] x = today.normalizedFeatures();
        return new FactorScore[] {
                new FactorScore("соцсети", Math.max(0.0, -wSocial) * x[0], wSocial * x[0]),
                new FactorScore("игры", Math.max(0.0, -wGames) * x[1], wGames * x[1]),
                new FactorScore("недостаток работы/учёбы", Math.max(0.0, wWork) * Math.max(0.0, 1.0 - x[2]), wWork * x[2]),
                new FactorScore("ночное использование", Math.max(0.0, -wNight) * x[3], wNight * x[3]),
                new FactorScore("частые разблокировки", Math.max(0.0, -wUnlocks) * x[4], wUnlocks * x[4])
        };
    }

    public String mainProblemFactor(DailyRecord today) {
        FactorScore[] scores = factorScores(today);
        int best = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i].problemScore > scores[best].problemScore) {
                best = i;
            }
        }
        if (scores[best].problemScore < 0.05) {
            return "нет выраженного фактора";
        }
        return scores[best].name;
    }

    public String strongestNegativeFactor() {
        String factor = "соцсети";
        double value = Math.abs(wSocial);

        if (Math.abs(wGames) > value) {
            value = Math.abs(wGames);
            factor = "игры";
        }
        if (Math.abs(wNight) > value) {
            value = Math.abs(wNight);
            factor = "ночное использование";
        }
        if (Math.abs(wUnlocks) > value) {
            value = Math.abs(wUnlocks);
            factor = "частые разблокировки";
        }
        if (value < 0.05 && Math.abs(wWork) > value) {
            factor = "рабочие/учебные приложения";
        }
        if (value < 0.05) {
            return "калибровка";
        }
        return factor;
    }

    public String weightsText() {
        return String.format(Locale.US,
                "Дней обучения: %d\n" +
                        "bias: %.3f\n" +
                        "соцсети: %.3f\n" +
                        "игры: %.3f\n" +
                        "рабочие/учёба: %.3f\n" +
                        "ночное использование: %.3f\n" +
                        "разблокировки: %.3f",
                trainingDays, bias, wSocial, wGames, wWork, wNight, wUnlocks);
    }

    public void save(SharedPreferences preferences) {
        preferences.edit()
                .putFloat(KEY_BIAS, (float) bias)
                .putFloat(KEY_W_SOCIAL, (float) wSocial)
                .putFloat(KEY_W_GAMES, (float) wGames)
                .putFloat(KEY_W_WORK, (float) wWork)
                .putFloat(KEY_W_NIGHT, (float) wNight)
                .putFloat(KEY_W_UNLOCKS, (float) wUnlocks)
                .putInt(KEY_TRAINING_DAYS, trainingDays)
                .apply();
    }

    public void load(SharedPreferences preferences) {
        bias = preferences.getFloat(KEY_BIAS, (float) bias);
        wSocial = preferences.getFloat(KEY_W_SOCIAL, (float) wSocial);
        wGames = preferences.getFloat(KEY_W_GAMES, (float) wGames);
        wWork = preferences.getFloat(KEY_W_WORK, (float) wWork);
        wNight = preferences.getFloat(KEY_W_NIGHT, (float) wNight);
        wUnlocks = preferences.getFloat(KEY_W_UNLOCKS, (float) wUnlocks);
        trainingDays = preferences.getInt(KEY_TRAINING_DAYS, trainingDays);
        applyConstraints();
    }

    public void reset() {
        bias = DEFAULT_BIAS;
        wSocial = 0.0;
        wGames = 0.0;
        wWork = 0.0;
        wNight = 0.0;
        wUnlocks = 0.0;
        trainingDays = 0;
    }

    public static double[] normalize(double socialMins, double gameMins, double workMins, double nightMins, double unlockCount) {
        return new double[] {
                socialMins / 300.0,
                gameMins / 240.0,
                workMins / 360.0,
                nightMins / 180.0,
                unlockCount / 120.0
        };
    }

    private double clip(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class FactorScore {
        public final String name;
        public final double problemScore;
        public final double modelContribution;

        public FactorScore(String name, double problemScore, double modelContribution) {
            this.name = name;
            this.problemScore = problemScore;
            this.modelContribution = modelContribution;
        }
    }

    public static class UpdateResult {
        public final double oldPrediction;
        public final double newPrediction;
        public final double error;

        public UpdateResult(double oldPrediction, double newPrediction, double error) {
            this.oldPrediction = oldPrediction;
            this.newPrediction = newPrediction;
            this.error = error;
        }
    }
}
