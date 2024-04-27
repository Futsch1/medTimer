package com.futsch1.medtimer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.futsch1.medtimer.medicine.MedicinesFragment;
import com.futsch1.medtimer.overview.OverviewFragment;
import com.futsch1.medtimer.statistics.StatisticsFragment;

public class ViewPagerAdapter
        extends FragmentStateAdapter {

    public ViewPagerAdapter(FragmentManager fm, Lifecycle lc) {
        super(fm, lc);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        if (position == 0)
            fragment = new OverviewFragment();
        else if (position == 1)
            fragment = new MedicinesFragment();
        else
            fragment = new StatisticsFragment();

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
