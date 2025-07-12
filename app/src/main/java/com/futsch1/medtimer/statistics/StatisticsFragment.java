package com.futsch1.medtimer.statistics;

import static com.futsch1.medtimer.statistics.ActiveStatisticsFragment.StatisticFragmentType;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.remindertable.ReminderTableFragment;
import com.google.android.material.chip.ChipGroup;

public class StatisticsFragment extends Fragment {
    private Spinner timeSpinner;

    private ChartsFragment chartsFragment;
    private AnalysisDays analysisDays;
    private ActiveStatisticsFragment activeStatisticsFragment;
    private OptionsMenu optionsMenu = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View statisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);

        analysisDays = new AnalysisDays(requireContext());
        activeStatisticsFragment = new ActiveStatisticsFragment(requireContext());
        timeSpinner = statisticsView.findViewById(R.id.timeSpinner);
        chartsFragment = new ChartsFragment();
        chartsFragment.setDays(analysisDays.getDays());

        setupTimeSpinner();

        setupFragmentButtons(statisticsView);

        loadActiveFragment(activeStatisticsFragment.getActiveFragment());

        optionsMenu = new OptionsMenu(this,
                new ViewModelProvider(this).get(MedicineViewModel.class),
                statisticsView, true);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return statisticsView;
    }

    @Override
    public void onPause() {
        try {
            requireActivity().getSupportFragmentManager().executePendingTransactions();
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Intentionally empty
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (optionsMenu != null) {
            optionsMenu.onDestroy();
        }
        try {
            requireActivity().getSupportFragmentManager().executePendingTransactions();
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Intentionally empty
        }
    }

    private void setupFragmentButtons(View statisticsView) {
        ChipGroup chipGroup = statisticsView.findViewById(R.id.analysisView);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (R.id.chartChip == checkedId) {
                    loadActiveFragment(StatisticFragmentType.CHARTS);
                } else if (R.id.tableChip == checkedId) {
                    loadActiveFragment(StatisticFragmentType.TABLE);
                } else {
                    loadActiveFragment(StatisticFragmentType.CALENDAR);
                }
            }
        });
        chipGroup.check(switch (activeStatisticsFragment.getActiveFragment()) {
            case TABLE -> R.id.tableChip;
            case CALENDAR -> R.id.calendarChip;
            default -> R.id.chartChip;
        });
    }

    private void loadActiveFragment(StatisticFragmentType fragmentType) {
        Fragment fragment = switch (fragmentType) {
            case TABLE -> new ReminderTableFragment();
            case CALENDAR -> new CalendarFragment();
            default -> chartsFragment;
        };
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        try {
            transaction.commit();
            activeStatisticsFragment.setActiveFragment(fragmentType);
            checkTimeSpinnerVisibility();
        } catch (IllegalStateException e) {
            // Intentionally empty
        }
    }

    private void checkTimeSpinnerVisibility() {
        if (activeStatisticsFragment.getActiveFragment() == ActiveStatisticsFragment.StatisticFragmentType.CHARTS) {
            timeSpinner.setVisibility(View.VISIBLE);
        } else {
            timeSpinner.setVisibility(View.INVISIBLE);
        }
    }

    private void setupTimeSpinner() {
        timeSpinner.setSelection(analysisDays.getPosition());
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != analysisDays.getPosition()) {
                    analysisDays.setPosition(position);

                    chartsFragment.setDays(analysisDays.getDays());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Intentionally empty
            }
        });
    }
}