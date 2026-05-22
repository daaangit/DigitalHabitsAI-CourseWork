package com.example.digitalhabitsai;

import java.util.ArrayList;
import java.util.List;

public class UsageSnapshot {
    public final double socialMins;
    public final double gameMins;
    public final double workMins;
    public final double nightMins;
    public final double unlockCount;;
    public final List<AppUsageEntry> apps;

    public UsageSnapshot(double socialMins,
                         double gameMins,
                         double workMins,
                         double nightMins,
                         double unlockCount) {
        this(socialMins, gameMins, workMins, nightMins, unlockCount, new ArrayList<AppUsageEntry>());
    }

    public UsageSnapshot(double socialMins,
                         double gameMins,
                         double workMins,
                         double nightMins,
                         double unlockCount,
                         List<AppUsageEntry> apps) {
        this.socialMins = socialMins;
        this.gameMins = gameMins;
        this.workMins = workMins;
        this.nightMins = nightMins;
        this.unlockCount = unlockCount;
        this.apps = apps == null ? new ArrayList<AppUsageEntry>() : apps;
    }
}
