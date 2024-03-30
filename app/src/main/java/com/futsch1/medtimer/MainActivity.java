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
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (Boolean.FALSE.equals(result)) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("show_notification", false).apply();
                }
            }
    );
    TabLayout tabLayout;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;
    OptionsMenu optionsMenu;
    private final ActivityResultLauncher<Intent> requestFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            optionsMenu.fileSelected(result.getData().getData());
        }
    });


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

        // Setup view pager
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tabs);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            int[] tabNames = new int[]{R.string.tab_overview, R.string.tab_medicine, R.string.tab_settings};
            tab.setText(tabNames[position]);
        }).attach();

        checkPermissions();

        NotificationChannelManager.createNotificationChannel(getApplicationContext());

        // Make sure work manager runs in a single thread to avoid race conditions
        WorkManager.initialize(
                this,
                new Configuration.Builder()
                        .setExecutor(Executors.newFixedThreadPool(1))
                        .build());
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.setGroupDividerEnabled(true);
        optionsMenu = new OptionsMenu(this, menu, new MedicineViewModel(getApplication()), requestFileLauncher);
        return true;
    }
}