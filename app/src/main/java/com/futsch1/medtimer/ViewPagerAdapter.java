package com.futsch1.medtimer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter
        extends FragmentStateAdapter {

    public ViewPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        if (position == 0)
            fragment = new StatusFragment();
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
