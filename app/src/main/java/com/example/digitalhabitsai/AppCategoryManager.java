package com.example.digitalhabitsai;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AppCategoryManager {
    public static final String SOCIAL = "social";
    public static final String GAME = "game";
    public static final String WORK = "work";
    public static final String OTHER = "other";

    private static final String KEY_OVERRIDES_JSON = "category_overrides_json";

    private final Context context;
    private final PackageManager packageManager;
    private final SharedPreferences preferences;

    public AppCategoryManager(Context context, SharedPreferences preferences) {
        this.context = context.getApplicationContext();
        this.packageManager = this.context.getPackageManager();
        this.preferences = preferences;
    }

    public String categoryFor(String packageName, String appName) {
        String manual = getManualCategory(packageName);
        if (manual != null) return manual;
        return detectCategory(packageName, appName);
    }

    public void setManualCategory(String packageName, String category) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        if (!isValidCategory(category)) category = OTHER;
        JSONObject object = loadOverridesObject();
        try {
            object.put(packageName, category);
            preferences.edit().putString(KEY_OVERRIDES_JSON, object.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public void removeManualCategory(String packageName) {
        JSONObject object = loadOverridesObject();
        object.remove(packageName);
        preferences.edit().putString(KEY_OVERRIDES_JSON, object.toString()).apply();
    }

    public String getManualCategory(String packageName) {
        JSONObject object = loadOverridesObject();
        if (object.has(packageName)) {
            return object.optString(packageName, OTHER);
        }
        return null;
    }

    public String findPackageByQuery(String query) {
        if (query == null) return null;
        String q = query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) return null;

        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : apps) {
            String pkg = info.packageName == null ? "" : info.packageName.toLowerCase(Locale.US);
            String label = getAppName(info.packageName).toLowerCase(Locale.US);
            if (pkg.equals(q) || pkg.contains(q) || label.contains(q)) {
                return info.packageName;
            }
        }
        return null;
    }

    public String getAppName(String packageName) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(info);
            return label == null ? packageName : label.toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    public String overridesText() {
        JSONObject object = loadOverridesObject();
        if (object.length() == 0) {
            return "Пока нет ручных правил. Нажмите категорию рядом с приложением или добавьте приложение через поиск.";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String pkg = keys.next();
            String category = object.optString(pkg, OTHER);
            builder.append("• ")
                    .append(getAppName(pkg))
                    .append(" → ")
                    .append(categoryRu(category))
                    .append("\n  ")
                    .append(pkg)
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private JSONObject loadOverridesObject() {
        String json = preferences.getString(KEY_OVERRIDES_JSON, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public String detectCategory(String packageName, String appName) {
        String p = packageName == null ? "" : packageName.toLowerCase(Locale.US);
        String n = appName == null ? "" : appName.toLowerCase(Locale.US);

        if (containsAny(p, "instagram", "tiktok", "facebook", "twitter", "snapchat", "vkontakte", "telegram", "whatsapp", "discord", "reddit", "pinterest", "threads", "messenger", "youtube", "rutube")
                || containsAny(n, "instagram", "tiktok", "facebook", "twitter", "snapchat", "telegram", "whatsapp", "discord", "reddit", "pinterest", "threads", "youtube")) {
            return SOCIAL;
        }

        if (isSystemGame(packageName)
                || containsAny(p, "game", "games", "supercell", "clash", "roblox", "minecraft", "pubg", "brawl", "candycrush", "fortnite", "mihoyo", "hoyoverse", "steam", "riotgames", "ea.gp", "king")) {
            return GAME;
        }

        if (containsAny(p, "gmail", "mail", "calendar", "docs", "sheets", "slides", "drive", "notion", "todo", "trello", "slack", "teams", "zoom", "office", "word", "excel", "powerpoint", "onenote", "dropbox", "obsidian", "github", "gitlab", "androidstudio", "coursera", "classroom", "moodle", "canvas")
                || containsAny(n, "gmail", "mail", "calendar", "docs", "sheets", "slides", "drive", "notion", "todo", "trello", "slack", "teams", "zoom", "office", "word", "excel", "powerpoint", "onenote", "obsidian", "github", "classroom", "moodle")) {
            return WORK;
        }

        return OTHER;
    }

    private boolean isSystemGame(String packageName) {
        if (Build.VERSION.SDK_INT < 26 || packageName == null) return false;
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return info.category == ApplicationInfo.CATEGORY_GAME;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean containsAny(String text, String... parts) {
        for (String part : parts) {
            if (text.contains(part)) return true;
        }
        return false;
    }

    private boolean isValidCategory(String category) {
        return SOCIAL.equals(category) || GAME.equals(category) || WORK.equals(category) || OTHER.equals(category);
    }

    public static String categoryRu(String category) {
        if (SOCIAL.equals(category)) return "соцсети";
        if (GAME.equals(category)) return "игры";
        if (WORK.equals(category)) return "работа/учёба";
        return "другое";
    }
}
