package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;
import com.google.android.material.color.MaterialColors;

@SuppressWarnings("java:S110")
public class MedTimerAppIntro extends AppIntro {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addSlide(AppIntroFragment.createInstance(getString(R.string.app_name),
                "This is a demo example in java of AppIntro library, with a custom background on each slide!",
                R.drawable.capsule
        ));

        addSlide(AppIntroFragment.createInstance(getString(R.string.custom),
                "This is a demo example in java of AppIntro library, with a custom background on each slide!",
                R.drawable.capsule
        ));

        askForPermissions(new String[]{POST_NOTIFICATIONS},
                1,
                true);
        this.setSystemBackButtonLocked(true);
        this.setButtonsEnabled(true);
        this.setImmersive(false);
        this.setColorDoneText(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.WHITE));
        this.setNextArrowColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.WHITE));
        this.setColorSkipButton(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.WHITE));
    }

    @Override
    protected void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }

    @Override
    protected void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }
}
