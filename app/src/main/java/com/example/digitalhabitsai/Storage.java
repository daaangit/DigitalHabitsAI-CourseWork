package com.example.digitalhabitsai;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Storage {
    private static final String PREFS_NAME = "digital_habits_storage";
    private static final String KEY_RECORDS = "records_json"; // legacy JSON storage key

    private static final String DB_NAME = "digital_habits.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_RECORDS = "daily_records";

    private final SharedPreferences preferences;
    private final DbHelper dbHelper;

    public void addOrUpdateRecordForDay(DailyRecord record) {
        List<DailyRecord> records = loadRecords();
        int index = findRecordIndexForDay(records, record.timestamp);
        if (index >= 0) {
            records.set(index, record);
        } else {
            records.add(record);
        }
        saveRecords(records);
    }

    public boolean hasRecordForDay(long timestamp) {
        List<DailyRecord> records = loadRecords();
        return findRecordIndexForDay(records, timestamp) >= 0;
    }

    private int findRecordIndexForDay(List<DailyRecord> records, long timestamp) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        for (int i = 0; i < records.size(); i++) {
            DailyRecord current = records.get(i);
            Calendar existing = Calendar.getInstance();
            existing.setTimeInMillis(current.timestamp);
            boolean sameDay = target.get(Calendar.YEAR) == existing.get(Calendar.YEAR)
                    && target.get(Calendar.DAY_OF_YEAR) == existing.get(Calendar.DAY_OF_YEAR);
            if (sameDay) return i;
        }
        return -1;
    }
    public Storage(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dbHelper = new DbHelper(context.getApplicationContext());
        migrateLegacyJsonIfNeeded();
    }

    public SharedPreferences prefs() {
        return preferences;
    }

    public List<DailyRecord> loadRecords() {
        List<DailyRecord> records = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_RECORDS,
                null,
                null,
                null,
                null,
                null,
                "timestamp ASC"
        );

        try {
            int iTimestamp = cursor.getColumnIndexOrThrow("timestamp");
            int iSocial = cursor.getColumnIndexOrThrow("social_mins");
            int iGames = cursor.getColumnIndexOrThrow("game_mins");
            int iWork = cursor.getColumnIndexOrThrow("work_mins");
            int iNight = cursor.getColumnIndexOrThrow("night_mins");
            int iUnlocks = cursor.getColumnIndexOrThrow("unlock_count");
            int iPred = cursor.getColumnIndexOrThrow("predicted_productivity");
            int iActual = cursor.getColumnIndexOrThrow("actual_productivity");

            while (cursor.moveToNext()) {
                records.add(new DailyRecord(
                        cursor.getLong(iTimestamp),
                        cursor.getDouble(iSocial),
                        cursor.getDouble(iGames),
                        cursor.getDouble(iWork),
                        cursor.getDouble(iNight),
                        cursor.getDouble(iUnlocks),
                        cursor.getDouble(iPred),
                        cursor.getDouble(iActual)
                ));
            }
        } finally {
            cursor.close();
        }

        return records;
    }

    public void saveRecords(List<DailyRecord> records) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_RECORDS, null, null);
            for (DailyRecord record : records) {
                insertRecord(db, record);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addRecord(DailyRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        insertRecord(db, record);
    }
    public DailyRecord removeLastRecord() {
        List<DailyRecord> records = loadRecords();
        if (records.isEmpty()) return null;
        DailyRecord removed = records.remove(records.size() - 1);
        saveRecords(records);
        return removed;
    }
    public void clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_RECORDS, null, null);
        preferences.edit().clear().apply();
    }

    private void insertRecord(SQLiteDatabase db, DailyRecord record) {
        ContentValues values = new ContentValues();
        values.put("timestamp", record.timestamp);
        values.put("social_mins", record.socialMins);
        values.put("game_mins", record.gameMins);
        values.put("work_mins", record.workMins);
        values.put("night_mins", record.nightMins);
        values.put("unlock_count", record.unlockCount);
        values.put("predicted_productivity", record.predictedProductivity);
        values.put("actual_productivity", record.actualProductivity);
        db.insert(TABLE_RECORDS, null, values);
    }

    private void migrateLegacyJsonIfNeeded() {
        boolean migrated = preferences.getBoolean("records_migrated_to_sqlite", false);
        if (migrated) return;

        String json = preferences.getString(KEY_RECORDS, "[]");
        List<DailyRecord> legacyRecords = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                legacyRecords.add(DailyRecord.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }

        if (!legacyRecords.isEmpty() && loadRecords().isEmpty()) {
            saveRecords(legacyRecords);
        }

        preferences.edit()
                .remove(KEY_RECORDS)
                .putBoolean("records_migrated_to_sqlite", true)
                .apply();
    }

    private static class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_RECORDS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "timestamp INTEGER NOT NULL, "
                    + "social_mins REAL NOT NULL, "
                    + "game_mins REAL NOT NULL, "
                    + "work_mins REAL NOT NULL, "
                    + "night_mins REAL NOT NULL, "
                    + "unlock_count REAL NOT NULL, "
                    + "predicted_productivity REAL NOT NULL, "
                    + "actual_productivity REAL NOT NULL"
                    + ")");
            db.execSQL("CREATE INDEX idx_daily_records_timestamp ON " + TABLE_RECORDS + "(timestamp)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // No-op for now, initial SQLite version.
        }
    }
}
