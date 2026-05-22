package com.example.digitalhabitsai;

public class AppUsageEntry {
    public final String packageName;
    public final String appName;
    public final String category;
    public final double minutes;

    public AppUsageEntry(String packageName, String appName, String category, double minutes) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
        this.minutes = minutes;
    }
}
