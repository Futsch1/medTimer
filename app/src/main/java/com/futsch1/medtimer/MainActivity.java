package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (!result) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("show_notification", false).apply();
                }
            }
    );
    TabLayout tabLayout;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View pager
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

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ReminderSchedulerService.serviceRunning) {
            startService(new Intent(getApplicationContext(), ReminderSchedulerService.class));
        } else {
            ReminderProcessor.requestReschedule(this);
        }
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
    }
}