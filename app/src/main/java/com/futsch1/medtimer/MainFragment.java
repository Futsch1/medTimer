package com.futsch1.medtimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainFragment extends Fragment {

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup view pager
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tabs);
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            int[] tabNames = new int[]{R.string.tab_overview, R.string.tab_medicine, R.string.statistics};
            tab.setText(tabNames[position]);
        }).attach();

        optionsMenu = new OptionsMenu(requireContext(),
                new MedicineViewModel(requireActivity().getApplication()),
                requestFileLauncher,
                Navigation.findNavController(requireActivity(), R.id.navHost));

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        optionsMenu.onDestroy();
    }
}