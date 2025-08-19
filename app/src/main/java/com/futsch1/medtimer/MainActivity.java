package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static androidx.navigation.ActivityKt.findNavController;
import static com.futsch1.medtimer.preferences.PreferencesNames.SECURE_WINDOW;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import kotlin.Unit;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Select theme
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String theme = sharedPref.getString("theme", "0");
        if (theme.equals("1")) {
            setTheme(R.style.Theme_MedTimer2);
        }

        // Screen capture
        if (sharedPref.getBoolean(SECURE_WINDOW, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        showIntro(sharedPref);

        EdgeToEdge.enable(this);

        ReminderNotificationChannelManager.Companion.initialize(this);

        authenticate(sharedPref);
    }

    private void showIntro(SharedPreferences sharedPref) {
        boolean introShown = sharedPref.getBoolean("intro_shown", false);
        if (!introShown && !BuildConfig.DEBUG) {
            startActivity(new Intent(getApplicationContext(), MedTimerAppIntro.class));
            sharedPref.edit().putBoolean("intro_shown", true).apply();
        } else {
            checkPermissions();
        }
    }

    private void authenticate(SharedPreferences sharedPref) {
        Biometrics biometrics = new Biometrics(this,
                () -> {
                    start();
                    return Unit.INSTANCE;
                }, () -> {
            this.finish();
            return Unit.INSTANCE;
        });
        if (sharedPref.getBoolean("app_authentication", false) && biometrics.hasBiometrics()) {
            biometrics.authenticate();
        } else {
            start();
        }

        handleBackPressed();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            new RequestPostNotificationPermission(this).requestPermission();
        }
    }

    private void start() {
        setContentView(R.layout.activity_main);
        setupNavigation();

        ActivityIntentKt.dispatch(this, this.getIntent());
        this.setIntent(new Intent());
    }

    private void handleBackPressed() {
        // Post the back event to the main loop to make sure all pending events are handled before
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Post the back event to the main loop to make sure all pending events are handled before
                new Handler(getMainLooper()).postDelayed(() -> {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }, 20);
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        setSupportActionBar(findViewById(R.id.toolbar));
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        bottomNavigationView.setOnItemReselectedListener(item -> {
            navController.popBackStack(item.getItemId(), false);
            List<Fragment> currentFragments = navHostFragment.getChildFragmentManager().getFragments();

            if (!currentFragments.isEmpty() && currentFragments.get(0) instanceof OnFragmentReselectedListener) {
                // Forward the reselection event to the current fragment
                ((OnFragmentReselectedListener) currentFragments.get(0)).onFragmentReselected();
            }
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            NavigationUI.onNavDestinationSelected(item, navController);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        // hack for https://issuetracker.google.com/issues/113122354
        // taken from https://stackoverflow.com/questions/52013545/android-9-0-not-allowed-to-start-service-app-is-in-background-after-onresume
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses != null) {
            int importance = runningAppProcesses.get(0).importance;
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                startService(new Intent(getApplicationContext(), ReminderSchedulerService.class));
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ActivityIntentKt.dispatch(this, intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            NavController navController = findNavController(this, R.id.navHost);
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}