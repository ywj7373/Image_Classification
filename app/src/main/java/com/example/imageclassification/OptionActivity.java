package com.example.imageclassification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OptionActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener{
    private TextView textViewFrame;
    private TextView textViewDevice;
    private TextView textViewThreads;

    private Spinner spinnerThreads;
    private Spinner spinnerDevice;
    private Spinner spinnerFrame;

    private Button buttonStart;

    private String device = "CPU";
    private int threads = 4;
    private String frame = "360p";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        spinnerDevice = findViewById(R.id.spinner_device);
        spinnerThreads = findViewById(R.id.spinner_threads);
        spinnerFrame = findViewById(R.id.spinner_frame);
        textViewDevice = findViewById(R.id.textView_device);
        textViewFrame = findViewById(R.id.textView_frame);
        textViewThreads = findViewById(R.id.textView_threads);
        buttonStart = findViewById(R.id.button_start);

        spinnerDevice.setOnItemSelectedListener(this);
        spinnerFrame.setOnItemSelectedListener(this);
        spinnerThreads.setOnItemSelectedListener(this);
        buttonStart.setOnClickListener(this);

        device = spinnerDevice.getSelectedItem().toString();
        threads = Integer.parseInt(spinnerThreads.getSelectedItem().toString());
        frame = spinnerFrame.getSelectedItem().toString();
        textViewDevice.setText(device);
        textViewThreads.setText(Integer.toString(threads));
        textViewFrame.setText(frame);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start: {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_FRAME, frame);
                intent.putExtra(MainActivity.EXTRA_PROCESSOR, device);
                intent.putExtra(MainActivity.EXTRA_THREADS, threads);
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
}

