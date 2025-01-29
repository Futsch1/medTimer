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

        OptionsMenu optionsMenu = new OptionsMenu(this.requireContext(),
                new ViewModelProvider(this).get(MedicineViewModel.class),
                this,
                statisticsView);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return statisticsView;
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
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();

        activeStatisticsFragment.setActiveFragment(fragmentType);
        checkTimeSpinnerVisibility();
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