package com.example.digitalhabitsai;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import java.util.Calendar;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends Activity {
    private static final String REMINDER_CHANNEL_ID = "save_day_reminders";
    private static final int BG = Color.rgb(245, 248, 255);
    private static final int PRIMARY = Color.rgb(36, 116, 240);
    private static final int PRIMARY_DARK = Color.rgb(27, 85, 184);
    private static final int TEXT = Color.rgb(26, 33, 40);
    private static final int MUTED = Color.rgb(94, 108, 122);
    private static final int CARD = Color.WHITE;
    private static final int GREEN = Color.GREEN;
    private static final int ORANGE = Color.rgb(255, 165, 0);
    private static final int RED = Color.RED;
    private LinearLayout contentRoot;
    private LinearLayout dashboardScreen;
    private LinearLayout dayScreen;
    private LinearLayout categoriesScreen;
    private LinearLayout modelScreen;
    private LinearLayout historyScreen;
    private LinearLayout settingsScreen;
    private LinearLayout debugScreen;

    private EditText socialInput;
    private EditText gamesInput;
    private EditText workInput;
    private EditText nightInput;
    private EditText unlockInput;
    private EditText appSearchInput;
    private SeekBar productivitySeekBar;
    private TextView productivityLabel;
    private TextView scoreBadge;
    private TextView recommendationText;
    private TextView mainFactorText;
    private TextView factorsText;
    private TextView endOfDayForecastText;
    private TextView modelText;
    private LinearLayout historyListContainer;
    private TextView trainingDaysText;
    private TextView usageStatusText;
    private TextView usageDetailsText;
    private TextView dashboardUsageText;
    private TextView categoryRulesText;
    private TextView calibrationHintText;
    private LinearLayout appsListContainer;
    private RadioGroup historyLimitGroup;
    private String selectedPackageFromPicker;
    private CheckBox weekendFlagCheckbox;
    private int historyLimit = 10;

    private Storage storage;
    private PersonalLinearModel model;
    private RecommendationEngine recommendationEngine;
    private UsageStatsHelper usageStatsHelper;
    private AppCategoryManager categoryManager;
    private UsageSnapshot lastSnapshot;
    private int repeatedSaveAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(PRIMARY_DARK);

        storage = new Storage(this);
        model = new PersonalLinearModel();
        model.load(storage.prefs());
        recommendationEngine = new RecommendationEngine();
        categoryManager = new AppCategoryManager(this, storage.prefs());
        usageStatsHelper = new UsageStatsHelper(this, categoryManager);
        historyLimit = storage.prefs().getInt("history_limit", 10);

        setContentView(buildUi());
        fillDefaultInputs();
        bindAutoPrediction();
        showScreen(dashboardScreen);
        showPrediction();
        refreshAllTexts();
        autoLoadUsageIfPermitted();
        promptSaveYesterdayIfMissing();
        ensureReminderChannel();
        requestNotificationPermissionIfNeeded();
        scheduleSaveDayReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUsagePermissionStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления выдано", Toast.LENGTH_SHORT).show();
                scheduleSaveDayReminders();
            } else {
                Toast.makeText(this, "Без разрешения уведомления могут не приходить", Toast.LENGTH_LONG).show();
            }
        }
    }

    private View buildUi() {
        LinearLayout appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(BG);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        scrollView.setLayoutParams(scrollLp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        scrollView.addView(root);

        root.addView(spacer(10));

        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentRoot);

        dashboardScreen = new LinearLayout(this);
        dashboardScreen.setOrientation(LinearLayout.VERTICAL);
        dashboardScreen.addView(resultCard());
        dashboardScreen.addView(spacer(14));

        dashboardScreen.addView(todayUsageCard());
        dashboardScreen.addView(spacer(14));

        dashboardScreen.addView(statusCard());
        dashboardScreen.addView(spacer(14));

        dashboardScreen.addView(quickActionsCard());

        dayScreen = new LinearLayout(this);
        dayScreen.setOrientation(LinearLayout.VERTICAL);
        dayScreen.addView(inputCard());

        categoriesScreen = new LinearLayout(this);
        categoriesScreen.setOrientation(LinearLayout.VERTICAL);
        categoriesScreen.addView(categoriesCard());

        historyScreen = new LinearLayout(this);
        historyScreen.setOrientation(LinearLayout.VERTICAL);
        historyScreen.addView(historyCard());

        settingsScreen = new LinearLayout(this);
        settingsScreen.setOrientation(LinearLayout.VERTICAL);
        settingsScreen.addView(settingsCard());

        settingsScreen.addView(spacer(14));
        settingsScreen.addView(modelCard());
        settingsScreen.addView(spacer(14));
        settingsScreen.addView(debugCard());


        contentRoot.addView(dashboardScreen);
        contentRoot.addView(dayScreen);
        contentRoot.addView(categoriesScreen);
        contentRoot.addView(historyScreen);
        contentRoot.addView(settingsScreen);

        View bottomNav = navBar();
        applyBottomInsets(bottomNav);
        appRoot.addView(scrollView);
        appRoot.addView(bottomNav);

        return appRoot;
    }
    private void applyBottomInsets(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int navBottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else if (Build.VERSION.SDK_INT >= 20) {
                navBottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(baseLeft, baseTop, baseRight, baseBottom + navBottom);
            return insets;
        });
        view.requestApplyInsets();
    }
    private View todayUsageCard() {
        LinearLayout usageCard = card(16, CARD);
        usageCard.addView(sectionTitle("Использование сегодня", ""));
        dashboardUsageText = text("Загрузка…", 14, false, TEXT);
        dashboardUsageText.setLineSpacing(dp(2), 1.0f);
        dashboardUsageText.setPadding(dp(10), dp(10), dp(10), dp(10));
        dashboardUsageText.setBackground(roundDrawable(Color.rgb(232, 238, 255), 14));
        usageCard.addView(dashboardUsageText);
        return usageCard;
    }

    private View navBar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);

        nav.addView(tabButton("Главная", dashboardScreen));
        nav.addView(tabButton("День", dayScreen));
        nav.addView(tabButton("Категории", categoriesScreen));
        nav.addView(tabButton("История", historyScreen));
        nav.addView(tabButton("Настройки", settingsScreen));
        return nav;
    }

    private Button tabButton(String title, LinearLayout screen) {
        Button b = button(title, Color.rgb(230, 235, 255), PRIMARY_DARK);
        b.setTextSize(13);
        b.setAllCaps(false);

        b.setPadding(dp(4), dp(20), dp(4), dp(20));

        b.setOnClickListener(v -> showScreen(screen));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void showScreen(LinearLayout screen) {
        dashboardScreen.setVisibility(screen == dashboardScreen ? View.VISIBLE : View.GONE);
        dayScreen.setVisibility(screen == dayScreen ? View.VISIBLE : View.GONE);
        categoriesScreen.setVisibility(screen == categoriesScreen ? View.VISIBLE : View.GONE);
        historyScreen.setVisibility(screen == historyScreen ? View.VISIBLE : View.GONE);
        settingsScreen.setVisibility(screen == settingsScreen ? View.VISIBLE : View.GONE);

        if (screen == historyScreen) {
            renderHistoryCards();
        }
    }

    private View statusCard() {
        LinearLayout card = card(16, CARD);
        trainingDaysText = text("Сохранено дней: 0", 16, true, PRIMARY_DARK);
        trainingDaysText.setPadding(0, dp(6), 0, 0);
        card.addView(trainingDaysText);
        calibrationHintText = text("Калибровка модели: идёт адаптация под пользователя.", 13, false, MUTED);
        calibrationHintText.setPadding(0, dp(6), 0, 0);
        card.addView(calibrationHintText);
        return card;
    }

    private View quickActionsCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Действия", ""));
        Button load = button("Обновить минуты с устройства", Color.rgb(220, 240, 235), PRIMARY_DARK);
        load.setOnClickListener(v -> loadTodayUsageFromPhone());
        card.addView(load);

        Button train = button("Перейти к вводу данных за день", PRIMARY, Color.WHITE);
        train.setOnClickListener(v -> showScreen(dayScreen));
        card.addView(train);
        return card;
    }

    private View inputCard() {
        LinearLayout card = card(16, CARD);
        card.addView(dayCardHeader());

        usageStatusText = text("Источник данных: ручной ввод. Для автосбора выдайте Usage Access.", 14, false, MUTED);
        usageStatusText.setLineSpacing(dp(2), 1.0f);
        usageStatusText.setPadding(0, 0, 0, dp(8));
        card.addView(usageStatusText);

        Button loadUsageButton = button("Получить данные с телефона за сегодня", PRIMARY, Color.WHITE);
        loadUsageButton.setOnClickListener(v -> loadTodayUsageFromPhone());
        card.addView(loadUsageButton);

        socialInput = numberInput("Соцсети", "минут за день", "160");
        gamesInput = numberInput("Игры", "минут за день", "45");
        workInput = numberInput("Работа / учёба", "минут за день", "110");
        nightInput = numberInput("Ночное использование", "минут с 00:00 до 05:00", "50");


        unlockInput = numberInput("Разблокировки", "количество за день", "70");

        attachMaxValueGuard(socialInput);
        attachMaxValueGuard(gamesInput);
        attachMaxValueGuard(workInput);
        attachMaxValueGuard(nightInput);
        attachMaxValueGuard(unlockInput);

        card.addView(twoColumnRow(labeledInput("Соцсети (мин/день)", socialInput), labeledInput("Игры (мин/день)", gamesInput)));
        card.addView(twoColumnRow(labeledInput("Работа / учёба (мин/день)", workInput), labeledInput("Ночное использование (00:00–05:00)", nightInput)));
        card.addView(labeledInput("Разблокировки (раз/день)", unlockInput));
        weekendFlagCheckbox = new CheckBox(this);
        weekendFlagCheckbox.setText("Выходной день (не учитывать в обучении модели)");
        weekendFlagCheckbox.setTextColor(MUTED);
        weekendFlagCheckbox.setPadding(0, dp(4), 0, dp(8));
        card.addView(weekendFlagCheckbox);

        TextView productivityTitle = text("Оценка продуктивности пользователем", 15, true, TEXT);
        productivityTitle.setPadding(0, dp(14), 0, 0);
        card.addView(productivityTitle);

        productivityLabel = text("Продуктивность дня: 5 / 10", 18, true, PRIMARY_DARK);
        productivityLabel.setGravity(Gravity.CENTER);
        productivityLabel.setPadding(0, dp(6), 0, dp(4));
        card.addView(productivityLabel);

        productivitySeekBar = new SeekBar(this);
        productivitySeekBar.setMax(10);
        productivitySeekBar.setProgress(5);
        productivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { productivityLabel.setText("Продуктивность дня: " + progress + " / 10"); showPrediction(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        card.addView(productivitySeekBar);

        Button predictButton = button("Обновить прогноз", PRIMARY, Color.WHITE);
        predictButton.setOnClickListener(v -> showPrediction());
        card.addView(predictButton);

        Button trainButton = button("Сохранить день и дообучить модель", PRIMARY, Color.WHITE);
        trainButton.setOnClickListener(v -> saveAndTrain());
        card.addView(trainButton);

        Button undoLastDayButton = button("Отменить последний сохранённый день", Color.rgb(255, 241, 232), RED);
        undoLastDayButton.setOnClickListener(v -> undoLastSavedDay());
        card.addView(undoLastDayButton);
        return card;
    }



    private View resultCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Ваш персональный прогноз", " "));
        scoreBadge = text("—", 26, true, Color.WHITE);
        scoreBadge.setGravity(Gravity.CENTER);
        scoreBadge.setPadding(dp(14), dp(12), dp(14), dp(12));
        scoreBadge.setBackground(roundDrawable(PRIMARY, 18));
        card.addView(scoreBadge);
        mainFactorText = text("Главный фактор: —", 16, true, ORANGE);
        mainFactorText.setPadding(0, dp(12), 0, dp(6));
        card.addView(mainFactorText);
        recommendationText = text("Рекомендация появится после расчёта.", 16, true, TEXT);
        recommendationText.setLineSpacing(dp(2), 1.0f);
        recommendationText.setPadding(dp(10), dp(10), dp(10), dp(10));
        recommendationText.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));
        card.addView(recommendationText);
        factorsText = text("Подробности прогноза доступны во вкладке «Модель».", 13, false, MUTED);
        factorsText.setPadding(0, dp(10), 0, 0);
        card.addView(factorsText);
        endOfDayForecastText = text("Прогноз к концу дня: —", 13, false, MUTED);
        endOfDayForecastText.setPadding(0, dp(6), 0, 0);
        card.addView(endOfDayForecastText);
        return card;
    }

    private View categoriesCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Категории приложений", ""));

        Button loadUsageButton = button("Обновить список приложений за сегодня", PRIMARY, Color.WHITE);
        loadUsageButton.setOnClickListener(v -> loadTodayUsageFromPhone());
        card.addView(loadUsageButton);

        TextView appsTitle = text("Приложения из сегодняшней статистики", 15, true, TEXT);
        appsTitle.setPadding(0, dp(12), 0, dp(6));
        card.addView(appsTitle);
        appsListContainer = new LinearLayout(this);
        appsListContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(appsListContainer);
        return card;
    }

    private View modelCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Модель", "веса и технические детали"));
        modelText = text("", 14, false, TEXT);
        modelText.setTypeface(Typeface.MONOSPACE);
        modelText.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));
        modelText.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(modelText);
        return card;
    }

    private View historyCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("История последних дней", ""));
        TextView limitTitle = text("Выберите количество дней:", 13, true, MUTED);
        limitTitle.setPadding(dp(4), 0, dp(4), dp(4));
        card.addView(limitTitle);
        historyLimitGroup = new RadioGroup(this);
        historyLimitGroup.setOrientation(LinearLayout.HORIZONTAL);
        historyLimitGroup.addView(historyLimitButton("10", 10));
        historyLimitGroup.addView(historyLimitButton("20", 20));
        historyLimitGroup.addView(historyLimitButton("50", 50));
        historyLimitGroup.addView(historyLimitButton("Все", -1));
        card.addView(historyLimitGroup);
        historyListContainer = new LinearLayout(this);
        historyListContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(historyListContainer);
        return card;
    }

    private CheckBox historyLimitButton(String text, int limit) {
        CheckBox b = new CheckBox(this);
        b.setText(text);
        b.setTextColor(PRIMARY_DARK);
        b.setChecked(historyLimit == limit);
        b.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            historyLimit = limit;
            storage.prefs().edit().putInt("history_limit", historyLimit).apply();
            if (historyLimitGroup != null) {
                for (int i = 0; i < historyLimitGroup.getChildCount(); i++) {
                    View v = historyLimitGroup.getChildAt(i);
                    if (v instanceof CheckBox && v != b) ((CheckBox) v).setChecked(false);
                }
            }
            renderHistoryCards();
        });
        return b;
    }

    private View demoCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Демо и сброс", "для быстрой демонстрации на защите"));
        Button demoButton = button("Демо-история: соцсети + ночь вредят", Color.rgb(230, 235, 255), PRIMARY_DARK);
        demoButton.setOnClickListener(v -> generateDemoHistory());
        card.addView(demoButton);
        Button resetButton = button("Сбросить историю и модель", Color.rgb(255, 236, 236), RED);
        resetButton.setOnClickListener(v -> resetAll());
        card.addView(resetButton);
        return card;
    }

    private View settingsCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Настройки", ""));
        Button permissionButton = button("Выдать доступ к статистике", Color.rgb(232, 238, 255), PRIMARY_DARK);
        permissionButton.setOnClickListener(v -> openUsageAccessSettings());
        card.addView(permissionButton);
        Button notificationPermissionButton = button("Выдать доступ к уведомлениям", Color.rgb(232, 238, 255), PRIMARY_DARK);
        notificationPermissionButton.setOnClickListener(v -> openNotificationAccessSettings());
        card.addView(notificationPermissionButton);
        return card;
    }

    private View debugCard() {
        LinearLayout card = card(16, CARD);
        card.addView(sectionTitle("Отладка", "демо-данные и сброс состояния"));

        TextView note = text(
                "Технические сведения модели и история вынесены в отдельные вкладки, " +
                        "чтобы текстовые поля не перезаписывались при обновлении интерфейса.",
                13,
                false,
                MUTED
        );
        note.setLineSpacing(dp(2), 1.0f);
        note.setPadding(0, 0, 0, dp(8));
        card.addView(note);

        card.addView(demoCard());
        return card;
    }

    private void updateUsagePermissionStatus() {
        if (usageStatusText == null) return;
        if (hasUsageStatsPermission()) {
            usageStatusText.setText("Есть доступ к статистике. Можно получить данные с телефона.");
        } else {
            usageStatusText.setText("Источник данных: ручной ввод. Для автосбора выдайте Usage Access.");
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return;

        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
    }

    private void openNotificationAccessSettings() {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 26) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void openUsageAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Найдите Digital Habits AI и включите доступ", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            } else {
                mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadTodayUsageFromPhone() {
        if (!hasUsageStatsPermission()) {
            if (usageStatusText != null) usageStatusText.setText("Нет доступа к UsageStats. Включите доступ для приложения.");
            Toast.makeText(this, "Сначала включите Usage Access", Toast.LENGTH_LONG).show();
            openUsageAccessSettings();
            return;
        }
        lastSnapshot = usageStatsHelper.collectToday();
        if (lastSnapshot == null) {
            if (usageStatusText != null) usageStatusText.setText("Не удалось получить UsageStats. Попробуйте позже.");
            Toast.makeText(this, "UsageStats временно недоступен", Toast.LENGTH_SHORT).show();
            return;
        }
        if (socialInput != null) socialInput.setText(String.format(Locale.US, "%.0f", lastSnapshot.socialMins));
        if (gamesInput != null) gamesInput.setText(String.format(Locale.US, "%.0f", lastSnapshot.gameMins));
        if (workInput != null) workInput.setText(String.format(Locale.US, "%.0f", lastSnapshot.workMins));
        if (nightInput != null) nightInput.setText(String.format(Locale.US, "%.0f", lastSnapshot.nightMins));
        if (unlockInput != null) unlockInput.setText(String.format(Locale.US, "%.0f", lastSnapshot.unlockCount));
        rebuildAppsList(lastSnapshot.apps);
        Toast.makeText(this, "Данные за сегодня загружены", Toast.LENGTH_SHORT).show();
        showPrediction();
        refreshAllTexts();
    }

    private void rebuildAppsList(List<AppUsageEntry> apps) {
        if (appsListContainer == null) return;
        appsListContainer.removeAllViews();
        if (apps == null || apps.isEmpty()) {
            appsListContainer.addView(text("Пока список пуст. Нажмите “Обновить список приложений за сегодня”.", 13, false, MUTED));
            return;
        }
        int limit = Math.min(12, apps.size());
        for (int i = 0; i < limit; i++) {
            AppUsageEntry app = apps.get(i);
            appsListContainer.addView(appCategoryRow(app));
        }
    }

    private View appCategoryRow(AppUsageEntry app) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxLp.setMargins(0, dp(6), 0, dp(6));
        box.setLayoutParams(boxLp);
        box.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));

        String displayName = categoryManager.getAppName(app.packageName);
        TextView title = text(displayName + " • " + String.format(Locale.US, "%.1f мин", app.minutes), 14, true, TEXT);

        box.addView(title);
        TextView sub = text("Категория: " + AppCategoryManager.categoryRu(app.category), 12, false, MUTED);
        sub.setPadding(0, dp(2), 0, dp(6));
        box.addView(sub);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.addView(categoryButton("Соцсети", AppCategoryManager.SOCIAL, app.packageName));
        buttons.addView(categoryButton("Игры", AppCategoryManager.GAME, app.packageName));
        buttons.addView(categoryButton("Работа", AppCategoryManager.WORK, app.packageName));
        buttons.addView(categoryButton("Другое", AppCategoryManager.OTHER, app.packageName));
        box.addView(buttons);
        return box;
    }

    private Button categoryButton(String title, String category, String packageNameOrNull) {
        Button b = button(title, Color.rgb(230, 235, 255), PRIMARY_DARK);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setMinHeight(dp(52));
        b.setOnClickListener(v -> {
            String pkg = packageNameOrNull;
            if (pkg == null) {
                if (selectedPackageFromPicker != null) pkg = selectedPackageFromPicker;
                else pkg = categoryManager.findPackageByQuery(appSearchInput.getText().toString());
                if (pkg == null) {
                    Toast.makeText(this, "Приложение не найдено. Введите точнее название или package.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            categoryManager.setManualCategory(pkg, category);
            Toast.makeText(this, categoryManager.getAppName(pkg) + " → " + AppCategoryManager.categoryRu(category), Toast.LENGTH_SHORT).show();
            refreshAllTexts();
            if (lastSnapshot != null) loadTodayUsageFromPhone();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        return b;
    }



    private void bindAutoPrediction() {
        TextWatcher watcher = simpleWatcher(this::showPrediction);
        socialInput.addTextChangedListener(watcher);
        gamesInput.addTextChangedListener(watcher);
        workInput.addTextChangedListener(watcher);
        nightInput.addTextChangedListener(watcher);
        unlockInput.addTextChangedListener(watcher);
    }

    private void fillDefaultInputs() {
        socialInput.setText("160");
        gamesInput.setText("45");
        workInput.setText("110");
        nightInput.setText("50");
        unlockInput.setText("70");
    }

    private void showPrediction() {
        DailyRecord today = readRecordFromInputs(productivitySeekBar == null ? 5 : productivitySeekBar.getProgress());
        updatePredictionUi(today, -999);
        refreshAllTexts();
    }

    private void promptSaveYesterdayIfMissing() {
    }

    private void markRecordExcludedFromTraining(long timestamp) {
        TrainingExclusionHelper.markExcluded(storage.prefs(), timestamp);
    }

    private List<DailyRecord> filterTrainingRecords(List<DailyRecord> records) {
        return TrainingExclusionHelper.filterTrainingRecords(storage.prefs(), records);
    }

    private void saveAndTrain() {
        if (hasValuesAboveMax()) {
            Toast.makeText(this, "Введены некорректные значения", Toast.LENGTH_LONG).show();
            return;
        }
        DailyRecord record = readRecordFromInputs(productivitySeekBar.getProgress());

        if (storage.hasRecordForDay(record.timestamp)) {
            repeatedSaveAttempts++;
            if (repeatedSaveAttempts >= 15) {
                Toast.makeText(this, "В глаз себе тыкни.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "День уже был сохранён", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        repeatedSaveAttempts = 0;
        double oldPrediction = model.predict(record.socialMins, record.gameMins, record.workMins, record.nightMins, record.unlockCount);
        record.predictedProductivity = oldPrediction;
        storage.addOrUpdateRecordForDay(record);
    }

    private boolean hasValuesAboveMax() {
        return isAboveMax(socialInput)
                || isAboveMax(gamesInput)
                || isAboveMax(workInput)
                || isAboveMax(nightInput)
                || isAboveMax(unlockInput);
    }

    private boolean isAboveMax(EditText input) {
        if (input == null) return false;
        String raw = input.getText().toString().trim().replace(',', '.');
        if (raw.isEmpty()) return false;
        try {
            return Double.parseDouble(raw) > 1000.0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void undoLastSavedDay() {
        DailyRecord removed = storage.removeLastRecord();
        if (removed == null) {
            Toast.makeText(this, "История пуста — отменять нечего", Toast.LENGTH_SHORT).show();
            return;
        }
        List<DailyRecord> records = storage.loadRecords();
        List<DailyRecord> trainRecords = TrainingExclusionHelper.filterTrainingRecords(storage.prefs(), records);
        if (trainRecords.isEmpty()) {
            model.reset();
        } else {
            DailyRecord latest = trainRecords.get(trainRecords.size() - 1);
            double oldPrediction = model.predict(latest.socialMins, latest.gameMins, latest.workMins, latest.nightMins, latest.unlockCount);
            model.retrainFromHistory(trainRecords, latest, oldPrediction);
        }
        model.save(storage.prefs());
        Toast.makeText(this, "Последний сохранённый день удалён", Toast.LENGTH_SHORT).show();
        refreshAllTexts();
        showPrediction();
    }

    private void generateDemoHistory() {
        Random random = new Random(42);
        for(int j =0; j<25; j++) {
            for (int i = 0; i < 18; i++) {
                double social = 60 + i * 10 + random.nextInt(40);
                double games = 20 + random.nextInt(45);
                double work = 180 - i * 4 + random.nextInt(35);
                double night = 20 + i * 5 + random.nextInt(35);
                double unlocks = 40 + random.nextInt(70);
                double actual = 8.3 - social / 90.0 - night / 85.0 + work / 320.0 - unlocks / 300.0 + random.nextGaussian() * 0.45;
                actual = Math.max(0, Math.min(10, actual));
                DailyRecord record = new DailyRecord(System.currentTimeMillis() - (long) (18 - i) * 24L * 60L * 60L * 1000L, social, games, work, night, unlocks, model.predict(social, games, work, night, unlocks), actual);
                storage.addRecord(record);
            }
        }
        List<DailyRecord> records = storage.loadRecords();
        if (!records.isEmpty()) {
            DailyRecord latest = records.get(records.size() - 1);
            model.retrainFromHistory(TrainingExclusionHelper.filterTrainingRecords(storage.prefs(), records), latest, latest.predictedProductivity);
        }
        model.save(storage.prefs());
        Toast.makeText(this, "Демо-история добавлена", Toast.LENGTH_SHORT).show();
        showPrediction();
        refreshAllTexts();
        showScreen(dashboardScreen);
    }



    private void resetAll() {
        if (endOfDayForecastText != null) endOfDayForecastText.setText("Прогноз к концу дня: —");
        String savedCategoryOverrides = storage.prefs().getString("category_overrides_json", "{}");
        storage.clearAll();
        storage.prefs().edit().putString("category_overrides_json", savedCategoryOverrides).apply();
        model.reset();
        model.save(storage.prefs());
        scoreBadge.setText("—");
        recommendationText.setText("История и модель сброшены. Получите UsageStats или запустите демо-историю.");
        factorsText.setText("Подробности прогноза доступны во вкладке «Модель».");
        lastSnapshot = null;
        if (appsListContainer != null) appsListContainer.removeAllViews();
        refreshAllTexts();
    }

    private void updatePredictionUi(DailyRecord today, double error) {
        if (endOfDayForecastText != null) {
            endOfDayForecastText.setText(ForecastHelper.buildEndOfDayForecast(today, model));
        }
        double prediction = model.predict(today.socialMins, today.gameMins, today.workMins, today.nightMins, today.unlockCount);
        int badgeColor;
        String status;
        if (prediction >= 7.0) { badgeColor = GREEN; status = "низкий риск непродуктивного дня"; }
        else if (prediction >= 4.5) { badgeColor = ORANGE; status = "средний риск, есть отвлекающие факторы"; }
        else { badgeColor = RED; status = "высокий риск непродуктивного дня"; }
        scoreBadge.setBackground(roundDrawable(badgeColor, 18));
        scoreBadge.setText(String.format(Locale.US, "Оценка модели: %.1f / 10", prediction));

        String mainFactor = model.mainProblemFactor(today);
        if (mainFactorText != null) mainFactorText.setText("Главный фактор: " + mainFactor);
        recommendationText.setText(recommendationEngine.adviceForFactor(mainFactor));
        String errorPart = error == -999 ? "" : String.format(Locale.US, " • ошибка %.2f", error);
        factorsText.setText(status + errorPart);
    }

    private DailyRecord readRecordFromInputs(double actualProductivity) {
        double social = readDouble(socialInput);
        double games = readDouble(gamesInput);
        double work = readDouble(workInput);
        double night = readDouble(nightInput);
        double unlocks = readDouble(unlockInput);
        double predicted = model.predict(social, games, work, night, unlocks);
        return new DailyRecord(System.currentTimeMillis(), social, games, work, night, unlocks, predicted, actualProductivity);
    }

    private void autoLoadUsageIfPermitted() {
        if (hasUsageStatsPermission()) {
            loadTodayUsageFromPhone();
        }
    }

    private String buildDashboardUsageText() {
        if (socialInput == null || gamesInput == null || workInput == null || nightInput == null || unlockInput == null) {
            return "Подготовка данных использования…";
        }
        DailyRecord current = readRecordFromInputs(productivitySeekBar == null ? 5 : productivitySeekBar.getProgress());
        return UsageSummaryFormatter.dashboardUsageText(current, lastSnapshot != null);
    }

    private void refreshAllTexts() { refreshAllTextsWithError(Double.NaN); }

    private void refreshAllTextsWithError(double lastError) {
        if (endOfDayForecastText != null) {
            DailyRecord current = readRecordFromInputs(productivitySeekBar == null ? 5 : productivitySeekBar.getProgress());
            endOfDayForecastText.setText(ForecastHelper.buildEndOfDayForecast(current, model));
        }
        if (trainingDaysText != null) trainingDaysText.setText("Сохранено дней: " + model.trainingDays);
        if (calibrationHintText != null) {
            calibrationHintText.setText(model.trainingDays < 7
                    ? "Калибровка модели: идёт адаптация под пользователя."
                    : "Калибровка модели: базовая адаптация завершена.");
        }
        if (modelText != null) {
            String errorText = Double.isNaN(lastError)
                    ? ""
                    : "\n\nПоследняя ошибка обучения: " + String.format(Locale.US, "%.2f", lastError);
            modelText.setText(
                    model.weightsText() +
                            errorText +
                            "\n\nПояснение: отрицательные веса показывают факторы, связанные со снижением пользовательской оценки. " +
                            "Работа/учёба имеет положительный вес, если связана с более продуктивными днями."
            );
        }
        renderHistoryCards();
        if (categoryRulesText != null) categoryRulesText.setText("Ручные правила:\n" + categoryManager.overridesText());
        if (dashboardUsageText != null) dashboardUsageText.setText(buildDashboardUsageText());
    }

    private void renderHistoryCards() {
        if (historyListContainer == null) return;
        historyListContainer.removeAllViews();

        List<DailyRecord> records = storage.loadRecords();
        if (records.isEmpty()) {
            TextView empty = text("Пока нет сохранённых дней. Получите данные с телефона, поставьте оценку продуктивности и сохраните день.", 14, false, MUTED);
            empty.setLineSpacing(dp(2), 1.0f);
            empty.setPadding(dp(12), dp(12), dp(12), dp(12));
            empty.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));
            historyListContainer.addView(empty);
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd.MM", Locale.US);
        java.util.Set<String> excluded = storage.prefs().getStringSet("excluded_training_timestamps", new java.util.HashSet<>());

        int shown = historyLimit < 0 ? records.size() : Math.min(historyLimit, records.size());
        TextView header = text("Последние " + shown + " из " + records.size() + " дней", 13, true, MUTED);
        header.setPadding(dp(6), dp(4), dp(6), dp(8));
        historyListContainer.addView(header);

        int start = Math.max(0, records.size() - shown);
        for (int i = records.size() - 1; i >= start; i--) {
            DailyRecord r = records.get(i);
            boolean isWeekendExcluded = excluded.contains(String.valueOf(r.timestamp));
            historyListContainer.addView(historyDayCard(r, format, isWeekendExcluded));
        }
    }

    private View historyDayCard(DailyRecord r, SimpleDateFormat format, boolean excludedFromTraining) {
        double delta = r.actualProductivity - r.predictedProductivity;
        int accent;
        if (delta >= 0.0) accent = Color.rgb(220, 245, 225);
        else if (delta > -1.0) accent = Color.rgb(232, 238, 255);
        else accent = Color.rgb(255, 236, 236);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        TextView date = text(format.format(new Date(r.timestamp)), 14, true, TEXT);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        row1.addView(date, dateLp);
        TextView badge = text(excludedFromTraining ? "Выходной" : "Будни", 12, true, excludedFromTraining ? ORANGE : PRIMARY_DARK);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(roundDrawable(excludedFromTraining ? Color.rgb(255, 244, 230) : Color.rgb(232, 238, 255), 12));
        row1.addView(badge);
        box.addView(row1);

        TextView row2 = text(
                String.format(Locale.US, "Модель %.1f • Оценка %.1f", r.predictedProductivity, r.actualProductivity),
                13, true, TEXT
        );
        row2.setPadding(0, dp(6), 0, dp(8));
        row2.setBackground(roundDrawable(accent, 10));
        row2.setPadding(dp(10), dp(6), dp(10), dp(6));
        box.addView(row2);

        LinearLayout row3a = new LinearLayout(this);
        row3a.setOrientation(LinearLayout.HORIZONTAL);
        row3a.addView(miniStat(String.valueOf((int) r.socialMins), "Соцсети", MUTED, 1));
        row3a.addView(miniStat(String.valueOf((int) r.gameMins), "Игры", MUTED, 1));
        row3a.addView(miniStat(String.valueOf((int) r.workMins), "Работа", MUTED, 1));
        box.addView(row3a);

        LinearLayout row3b = new LinearLayout(this);
        row3b.setOrientation(LinearLayout.HORIZONTAL);
        row3b.addView(miniStat(String.valueOf((int) r.nightMins), "Ночь", MUTED, 1));
        row3b.addView(miniStat(String.valueOf((int) r.unlockCount), "Кол-во разблокировок", MUTED, 1));
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        row3b.addView(spacer);
        box.addView(row3b);
        return box;
    }

    private double readDouble(EditText editText) {
        try {
            double value = Double.parseDouble(editText.getText().toString().trim().replace(',', '.'));
            return Math.min(value, 1000.0);
        } catch (Exception e) {
            return 0.0;
        }
    }



    private View dayCardHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);

        TextView title = text("Данные за день", 16, true, TEXT);
        title.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.setMargins(0, 0, dp(8), dp(10));
        header.addView(title, titleLp);

        Button helpButton = button("?", Color.rgb(238, 242, 252), PRIMARY_DARK);
        helpButton.setAllCaps(false);
        helpButton.setTextSize(12);
        helpButton.setPadding(0, 0, 0, 0);
        helpButton.setMinWidth(0);
        helpButton.setMinHeight(0);
        helpButton.setMinimumWidth(0);
        helpButton.setMinimumHeight(0);
        int helpSize = dp(28);
        helpButton.setBackground(roundDrawable(Color.rgb(238, 242, 252), helpSize / 2));


        helpButton.setOnClickListener(v -> showDayHelpDialog());
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(helpSize, helpSize);
        helpLp.topMargin = dp(2);

        header.addView(helpButton, helpLp);
        return header;
    }

    private void showDayHelpDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(6), dp(2), dp(6), 0);

        content.addView(helpStep("1", "Получите данные", "Нажмите «Получить данные с телефона за сегодня»."));
        content.addView(helpStep("2", "Проверьте минуты", "При необходимости поправьте значения вручную."));
        content.addView(helpStep("3", "Оцените день", "Поставьте оценку продуктивности."));
        content.addView(helpStep("4", "Сохраните", "Нажмите «Сохранить день и дообучить модель»."));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Как заполнять вкладку «День»")
                .setView(content)
                .setPositiveButton("Отлично", (d, which) -> d.dismiss())
                .create();
        dialog.show();
    }

    private View helpStep(String number, String title, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView badge = text(number, 12, true, Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        int badgeSize = dp(22);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
        badgeLp.setMargins(0, dp(1), dp(8), 0);
        badge.setBackground(roundDrawable(PRIMARY, badgeSize / 2));
        row.addView(badge, badgeLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 14, true, TEXT));
        TextView desc = text(description, 13, false, MUTED);
        desc.setLineSpacing(dp(1), 1.0f);
        texts.addView(desc);
        row.addView(texts);

        return row;
    }


    private void ensureReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(REMINDER_CHANNEL_ID, "Напоминания о сохранении дня", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Напоминания в 22:00, 23:00 и 23:30");
        nm.createNotificationChannel(channel);
    }

    private void scheduleSaveDayReminders() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;
        scheduleReminder(alarmManager, 22, 0, 2200);
        scheduleReminder(alarmManager, 23, 0, 2300);
        scheduleReminder(alarmManager, 23, 30, 2330);
    }

    private Intent buildReminderIntent(int hour, int minute) {
        Intent intent = new Intent(this, SaveDayReminderReceiver.class);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);
        return intent;
    }

    private Calendar nextTrigger(int hour, int minute) {
        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        if (trigger.getTimeInMillis() <= System.currentTimeMillis()) {
            trigger.add(Calendar.DAY_OF_YEAR, 1);
        }
        return trigger;
    }

    private void scheduleReminder(AlarmManager alarmManager, int hour, int minute, int requestCode) {
        Intent intent = buildReminderIntent(hour, minute);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);

        Calendar trigger = nextTrigger(hour, minute);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            trigger.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            trigger.getTimeInMillis(),
                            pendingIntent
                    );
                }
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        trigger.getTimeInMillis(),
                        pendingIntent
                );
            }
        } catch (SecurityException se) {
            Log.w("MainActivity", "Exact alarm denied, fallback to inexact", se);
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    trigger.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to schedule reminder", t);
        }
    }

    private TextWatcher simpleWatcher(final Runnable onChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (onChanged != null) onChanged.run();
            }
        };
    }

    private LinearLayout card(int radius, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundDrawable(color, radius));
        card.setElevation(dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView sectionTitle(String title, String subtitle) {
        String full = (subtitle == null || subtitle.trim().isEmpty()) ? title : (title + "\n" + subtitle);
        TextView v = text(full, 16, true, TEXT);
        v.setLineSpacing(dp(2), 1.0f);
        v.setPadding(0, 0, 0, dp(10));
        return v;
    }


    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView miniStat(String value, String label, int color, int weight) {
        TextView t = text(value + "\n" + label, 13, true, color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(6), dp(8), dp(6), dp(8));
        t.setBackground(roundDrawable(Color.rgb(248, 249, 253), 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(3), 0, dp(3), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private View labeledInput(String labelText, EditText input) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(labelText, 12, true, MUTED);
        label.setPadding(0, dp(4), 0, 0);
        wrap.addView(label);
        wrap.addView(input);
        return wrap;
    }

    private EditText numberInput(String title, String hint, String defaultValue) {
        EditText input = new EditText(this);
        input.setHint(title + ": " + hint);
        input.setText(defaultValue);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(roundDrawable(Color.rgb(248, 249, 253), 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        input.setLayoutParams(lp);
        return input;
    }

    private void attachMaxValueGuard(EditText input) {
        input.addTextChangedListener(new TextWatcher() {
            private boolean updating;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updating) return;
                String raw = s == null ? "" : s.toString().trim().replace(',', '.');
                if (raw.isEmpty()) return;
                try {
                    double value = Double.parseDouble(raw);
                    if (value <= 1000.0) return;
                    updating = true;
                    input.setSelection(input.getText().length());
                } catch (Exception ignored) {
                } finally {
                    updating = false;
                }
            }

            @Override public void afterTextChanged(Editable s) { }
        });
    }
    private LinearLayout twoColumnRow(View a, View b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp1.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp2.setMargins(dp(4), 0, 0, 0);
        row.addView(a, lp1);
        row.addView(b, lp2);
        return row;
    }

    private Button button(String title, int bgColor, int textColor) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(textColor);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundDrawable(bgColor, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private GradientDrawable roundDrawable(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

}
