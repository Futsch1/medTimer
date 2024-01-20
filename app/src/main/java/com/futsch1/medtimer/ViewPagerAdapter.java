package com.futsch1.medtimer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

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
            fragment = new MedicineFragment();
        else
            fragment = new SettingsFragment();

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
