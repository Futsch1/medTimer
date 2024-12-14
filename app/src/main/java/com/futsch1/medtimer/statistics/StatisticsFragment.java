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

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.remindertable.ReminderTableFragment;
import com.google.android.material.button.MaterialButton;

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
        setupReminderChartButton(statisticsView);
        setupReminderTableButton(statisticsView);
        setupReminderCalendarButton(statisticsView);

        loadActiveFragment(activeStatisticsFragment.getActiveFragment());

        OptionsMenu optionsMenu = new OptionsMenu(this.requireContext(),
                new MedicineViewModel(requireActivity().getApplication()),
                this,
                statisticsView);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return statisticsView;
    }

    private void loadActiveFragment(StatisticFragmentType fragmentType) {
        Fragment fragment = switch (fragmentType) {
            case TABLE -> new ReminderTableFragment();
            case CALENDAR -> new CalendarFragment();
            default -> chartsFragment;
        };
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
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

    private void setupReminderChartButton(View statisticsView) {
        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderChartsButton);
        reminderTableButton.setOnClickListener(view -> loadActiveFragment(StatisticFragmentType.CHARTS));
    }

    private void setupReminderTableButton(View statisticsView) {
        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderTableButton);
        reminderTableButton.setOnClickListener(view -> loadActiveFragment(StatisticFragmentType.TABLE));
    }

    private void setupReminderCalendarButton(View statisticsView) {
        MaterialButton reminderCalendarButton = statisticsView.findViewById(R.id.reminderCalendarButton);
        reminderCalendarButton.setOnClickListener(view -> loadActiveFragment(StatisticFragmentType.CALENDAR));
    }
}