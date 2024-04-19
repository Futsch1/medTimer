package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

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

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (Boolean.FALSE.equals(result)) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("show_notification", false).apply();
                }
            }
    );
    OptionsMenu optionsMenu;
    private final ActivityResultLauncher<Intent> requestFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            optionsMenu.fileSelected(result.getData().getData());
        }
    });
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

        setContentView(R.layout.activity_main);

        checkPermissions();

        NotificationChannelManager.createNotificationChannel(getApplicationContext());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(
                navController.getGraph())
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(getApplicationContext(), ReminderSchedulerService.class));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        optionsMenu.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.navHost);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.setGroupDividerEnabled(true);
        NavController navController = Navigation.findNavController(this, R.id.navHost);
        optionsMenu = new OptionsMenu(this, menu, new MedicineViewModel(getApplication()), requestFileLauncher, navController);
        return true;
    }
}