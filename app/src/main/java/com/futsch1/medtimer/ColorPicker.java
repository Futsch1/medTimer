package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_COLOR;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.skydoves.colorpickerview.ColorPickerView;

public class ColorPicker extends AppCompatActivity {

    private ColorPickerView colorPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);

        colorPicker = findViewById(R.id.colorPicker);
        colorPicker.setInitialColor(getIntent().getIntExtra(EXTRA_COLOR, Color.DKGRAY));
        Button ok = findViewById(R.id.colorOk);
        ok.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_COLOR, colorPicker.getColor());
            setResult(RESULT_OK, data);
            finish();
        });
    }
}