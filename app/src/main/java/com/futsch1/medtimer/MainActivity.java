package com.futsch1.medtimer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.viewpager2.widget.ViewPager2;

import com.futsch1.medtimer.logic.Medicines;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tabs);

        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText("OBJECT " + (position + 1))).attach();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "medTimer").build();
        Medicines medicines = new Medicines(db.medicineDao());
    }
}