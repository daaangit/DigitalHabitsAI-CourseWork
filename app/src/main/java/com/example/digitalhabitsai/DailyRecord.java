package com.example.digitalhabitsai;

import org.json.JSONException;
import org.json.JSONObject;

public class DailyRecord {
    public long timestamp;
    public double socialMins;
    public double gameMins;
    public double workMins;
    public double nightMins;
    public double unlockCount;
    public double predictedProductivity;
    public double actualProductivity;

    public DailyRecord(long timestamp,
                       double socialMins,
                       double gameMins,
                       double workMins,
                       double nightMins,
                       double unlockCount,
                       double predictedProductivity,
                       double actualProductivity) {
        this.timestamp = timestamp;
        this.socialMins = socialMins;
        this.gameMins = gameMins;
        this.workMins = workMins;
        this.nightMins = nightMins;
        this.unlockCount = unlockCount;
        this.predictedProductivity = predictedProductivity;
        this.actualProductivity = actualProductivity;
    }

    public double[] normalizedFeatures() {
        return new double[] {
                socialMins / 300.0,
                gameMins / 240.0,
                workMins / 360.0,
                nightMins / 180.0,
                unlockCount / 120.0
        };
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("timestamp", timestamp);
        object.put("socialMins", socialMins);
        object.put("gameMins", gameMins);
        object.put("workMins", workMins);
        object.put("nightMins", nightMins);
        object.put("unlockCount", unlockCount);
        object.put("predictedProductivity", predictedProductivity);
        object.put("actualProductivity", actualProductivity);
        return object;
    }

    public static DailyRecord fromJson(JSONObject object) throws JSONException {
        return new DailyRecord(
                object.getLong("timestamp"),
                object.getDouble("socialMins"),
                object.getDouble("gameMins"),
                object.getDouble("workMins"),
                object.getDouble("nightMins"),
                object.getDouble("unlockCount"),
                object.getDouble("predictedProductivity"),
                object.getDouble("actualProductivity")
        );
    }
}
