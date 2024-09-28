package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.futsch1.medtimer.helpers.MedicineIcons;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (Boolean.FALSE.equals(result)) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("show_notification", false).apply();
                }
            }
    );
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
        if (sharedPref.getBoolean("window_flag_secure", false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        showIntro(sharedPref);

        EdgeToEdge.enable(this);

        ReminderNotificationChannelManager.Companion.initialize(this);

        setContentView(R.layout.activity_main);

        setupNavigation();

        MedicineIcons.init(this);
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

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        bottomNavigationView.setOnItemReselectedListener(item -> navController.popBackStack(item.getItemId(), false));
        bottomNavigationView.setOnItemSelectedListener(item -> {
            NavigationUI.onNavDestinationSelected(item, navController);
            return true;
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
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
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.navHost);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}