package com.example.imageclassification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OptionActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private TextView textViewFrame;
    private TextView textViewDevice;
    private TextView textViewThreads;
    private TextView textViewFps;

    private Spinner spinnerThreads;
    private Spinner spinnerDevice;
    private Spinner spinnerFrame;
    private Spinner spinnerFps;

    private Switch switchAutoFocus;
    private Switch switchAutoExposure;
    private Switch switchWhiteBalance;
    private Switch switchOnlyCamera;

    private Button buttonStart;

    private String device = "CPU";
    private int threads = 4;
    private String frame = "1920 x 1080";
    private String fps = "low";
    private Boolean autoFocus = true;
    private Boolean autoExposure = true;
    private Boolean autoWhiteBalance = true;
    private Boolean onlyCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        spinnerDevice = findViewById(R.id.spinner_device);
        spinnerThreads = findViewById(R.id.spinner_threads);
        spinnerFrame = findViewById(R.id.spinner_frame);
        spinnerFps = findViewById(R.id.spinner_fps);
        textViewDevice = findViewById(R.id.textView_device);
        textViewFrame = findViewById(R.id.textView_frame);
        textViewThreads = findViewById(R.id.textView_threads);
        textViewFps = findViewById(R.id.textView_fps);
        buttonStart = findViewById(R.id.button_start);
        switchAutoExposure = findViewById(R.id.switch_autoExposure);
        switchAutoFocus = findViewById(R.id.switch_autoFocus);
        switchWhiteBalance = findViewById(R.id.switch_whiteBalance);
        switchOnlyCamera = findViewById(R.id.switch_onlyCamera);

        spinnerDevice.setOnItemSelectedListener(this);
        spinnerFrame.setOnItemSelectedListener(this);
        spinnerThreads.setOnItemSelectedListener(this);
        spinnerFps.setOnItemSelectedListener(this);

        buttonStart.setOnClickListener(this);

        switchWhiteBalance.setChecked(true);
        switchAutoFocus.setChecked(true);
        switchAutoExposure.setChecked(true);
        switchOnlyCamera.setChecked(false);

        switchWhiteBalance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) autoWhiteBalance = true;
                else autoWhiteBalance = false;
            }
        });
        switchAutoExposure.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) autoExposure = true;
                else autoExposure = false;
            }
        });
        switchAutoFocus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) autoFocus = true;
                else autoFocus = false;
            }
        });
        switchOnlyCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) onlyCamera = true;
                else onlyCamera = false;
            }
        });

        device = spinnerDevice.getSelectedItem().toString();
        threads = Integer.parseInt(spinnerThreads.getSelectedItem().toString());
        frame = spinnerFrame.getSelectedItem().toString();
        fps = spinnerFps.getSelectedItem().toString();

        textViewDevice.setText(device);
        textViewThreads.setText(Integer.toString(threads));
        textViewFrame.setText(frame);
        textViewFps.setText(fps);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start: {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_FRAME, frame);
                intent.putExtra(MainActivity.EXTRA_PROCESSOR, device);
                intent.putExtra(MainActivity.EXTRA_THREADS, threads);
                intent.putExtra(MainActivity.EXTRA_FPS, fps);
                intent.putExtra(MainActivity.EXTRA_AUTOFOCUS, autoFocus);
                intent.putExtra(MainActivity.EXTRA_AUTOEXPOSURE, autoExposure);
                intent.putExtra(MainActivity.EXTRA_WHITEBALANCE, autoWhiteBalance);
                intent.putExtra(MainActivity.EXTRA_ONLYCAMERA, onlyCamera);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == spinnerDevice) {
            device = parent.getItemAtPosition(position).toString();
            textViewDevice.setText(device);
        }
        else if (parent == spinnerThreads) {
            threads = Integer.parseInt(parent.getItemAtPosition(position).toString());
            textViewThreads.setText(Integer.toString(threads));
        }
        else if (parent == spinnerFrame) {
            frame = parent.getItemAtPosition(position).toString();
            textViewFrame.setText(frame);
        }
        else if (parent == spinnerFps) {
            fps = parent.getItemAtPosition(position).toString();
            textViewFps.setText(fps);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
}

