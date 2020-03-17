package com.example.imageclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Main Activity";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    public static final String EXTRA_PROCESSOR = "com.example.imageclassification.EXTRA_PROCESSOR";
    public static final String EXTRA_FRAME = "com.example.imageclassification.EXTRA_FRAME";
    public static final String EXTRA_THREADS = "com.example.imageclassfication.EXTRA_THREADS";
    public static final String EXTRA_FPS = "com.example.imageclassfication.EXTRA_FPS";
    public static final String EXTRA_AUTOFOCUS = "com.example.imageclassfication.EXTRA_AUTOFOCUS";
    public static final String EXTRA_AUTOEXPOSURE = "com.example.imageclassfication.EXTRA_AUTOEXPOSURE";
    public static final String EXTRA_WHITEBALANCE = "com.example.imageclassfication.EXTRA_WHITEBALANCE";

    private ModifiedCameraView cameraBridgeViewBase;
    private TextView textViewObject;
    private TextView textViewFPS;
    private BatteryManager bm;

    private String frame = "1920 x 1080";
    private String processor = "CPU";
    private int numThreads = 4;
    private String fps = "low";
    private Boolean autoFocus = true;
    private Boolean autoExposure =true;
    private Boolean autoWhiteBalance = true;

    private int counter = 0;
    private int maxFrameCount = 30;
    static int FPSCounter = 0;
    private long fpsCaptureTime = 0;

    private Mat mRGBA;
    private Runnable postInferenceCallbackDNN;
    public boolean isInferenceOngoing = false;

    //Thread that runs local DNN execution
    private HandlerThread handlerThread_inference;
    private Handler handler_inference;

    //Thread that renders frame on screen
    private HandlerThread handlerThread_rendering;
    private Handler handler_rendering;

    public Classifier classifier;

    // Load OpenCV library
    static {
        try {
            System.loadLibrary("opencv_java3");
            Log.d(TAG, "Successfully loaded OpenCV");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "OpenCV not found");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen turn on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textViewObject = findViewById(R.id.textView_object);
        textViewFPS = findViewById(R.id.textView_fps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //check for permissions
            if (!checkCameraPermissions()) {
                requestCameraPermissions();
            } else {
                // start camera
                openCamera();
            }
        }
        else {
            openCamera();
        }

        /** Get processor, frame, numThreads information */
        frame = getIntent().getStringExtra(EXTRA_FRAME);
        processor = getIntent().getStringExtra(EXTRA_PROCESSOR);
        numThreads = getIntent().getIntExtra(EXTRA_THREADS, 4);
        fps = getIntent().getStringExtra(EXTRA_FPS);
        autoExposure = getIntent().getBooleanExtra(EXTRA_AUTOEXPOSURE, true);
        autoFocus = getIntent().getBooleanExtra(EXTRA_AUTOFOCUS, true);
        autoWhiteBalance = getIntent().getBooleanExtra(EXTRA_WHITEBALANCE, true);

        Log.d(TAG, "AutoFocus: " + autoFocus);

        switch (fps) {
            case "low": {
                maxFrameCount = 6;
                break;
            }
            case "mid": {
                maxFrameCount = 3;
                break;
            }
            case "high": {
                maxFrameCount = 0;
                break;
            }
            default: {
                maxFrameCount = 6;
            }
        }

        //callback
        postInferenceCallbackDNN =
                new Runnable() {
                    @Override
                    public void run() {
                        isInferenceOngoing = false;
                    }
                };
        /** Battery related code */
        BroadcastReceiver battery = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    onBatteryChanged(intent);
                }
            }
        };

        bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(battery, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());
            appendLog(percentage + "% at " + currentDateandTime);
        }
    }

    //camera related functions
    public void openCamera(){
        cameraBridgeViewBase = (ModifiedCameraView) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.enableView();
        cameraBridgeViewBase.enableFpsMeter();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        if (!autoFocus) cameraBridgeViewBase.disableAutoFocus();
        if (!autoExposure) cameraBridgeViewBase.disableAutoExposure();
        if (!autoWhiteBalance) cameraBridgeViewBase.disableWhiteBalance();

        switch (frame) {
            case "1920 x 1080": {
                cameraBridgeViewBase.setResolution(1920, 1080);
                break;
            }
            case "1440 x 1080": {
                cameraBridgeViewBase.setResolution(1440, 1080);
                break;
            }
            case "1280 x 720": {
                cameraBridgeViewBase.setResolution(1280, 720);
                break;
            }
            case "960 x 540": {
                cameraBridgeViewBase.setResolution(960, 540);
                break;
            }
            case "320 x 240": {
                cameraBridgeViewBase.setResolution(320, 240);
                break;
            }
            default: cameraBridgeViewBase.setResolution(1920, 1080);
        }

        classifier = new Classifier(this);
        isInferenceOngoing = true;

        inferenceInBackground(new Runnable() {
            @Override
            public void run() {
                classifier.initialize(processor, numThreads);
                readyForNextInference();
            }
        });
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Control FPS
        counter++;
        if (counter < maxFrameCount)
            return null;
        counter = 0;

        // Set FPS Text
        long currentTime = SystemClock.uptimeMillis();
        if ((currentTime - fpsCaptureTime) > 1000) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,  "fps: " + FPSCounter);
                    textViewFPS.setText(Integer.toString(FPSCounter));
                    FPSCounter = 0;
                }
            });
            fpsCaptureTime = currentTime;
        }
        else {
            FPSCounter++;
        }

        mRGBA = inputFrame.rgba();

        // If inference is not on going, start new inference
        if (!isInferenceOngoing) {
            isInferenceOngoing = true;
            inferenceInBackground(new Runnable() {
                @Override
                public void run() {
                    long startTime = SystemClock.uptimeMillis();

                    Bitmap bitmap = toBitmap(mRGBA.clone());
                    final String result = classifier.classify(bitmap);

                    long endTime = SystemClock.uptimeMillis();
                    final long totalTime = endTime - startTime;
                    Log.d("Time", "Total Time: " + totalTime);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewObject.setText(result +" " +totalTime);
                        }
                    });
                    readyForNextInference();
                }
            });
        }

        return mRGBA;
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        Log.d(TAG, "Camera stopped!");
    }

    private Bitmap toBitmap(Mat mat) {
        final Bitmap bitmap =
                Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap, true);
        return bitmap;
    }

    protected void readyForNextInference() {
        if (postInferenceCallbackDNN != null) {
            Log.d(TAG,"run() at readyForNextInference");
            postInferenceCallbackDNN.run(); //this takes 0 ms
        }
    }

    protected synchronized void renderInBackground(final Runnable r) {
        if (handler_rendering != null) {
            Log.d(TAG,"posting a new runnable on rendering thread");
            handler_rendering.post(r);
        }
        else{
            Log.d(TAG,"null compression handler");
        }
    }

    protected synchronized void inferenceInBackground(final Runnable r) {
        if (handler_inference != null) {
            Log.d(TAG,"posting a new runnable on inference thread");
            handler_inference.post(r);
        }
        else{
            Log.d(TAG,"null inference handler");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        openCamera();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        stopBackgroundThread();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    private void startBackgroundThread() {
        Log.d(TAG,"opening a new inference thread");
        handlerThread_inference = new HandlerThread("inference");
        handlerThread_inference.start();
        handler_inference = new Handler(handlerThread_inference.getLooper());

        Log.d(TAG,"opening a new rendering thread");
        handlerThread_rendering = new HandlerThread("rendering");
        handlerThread_rendering.start();
        handler_rendering = new Handler(handlerThread_rendering.getLooper());
    }

    private void stopBackgroundThread() {
        handlerThread_inference.quitSafely();
        try {
            handlerThread_inference.join();
            handlerThread_inference = null;
            handler_inference= null;
        } catch (final InterruptedException e) {
            Log.d(TAG,"exception closing the local DNN inference thread in onPause()");
        }

        Log.d(TAG,"closing the rendering thread");
        handlerThread_rendering.quit();
        try {
            handlerThread_rendering.join();
            handlerThread_rendering = null;
            handler_rendering = null;
        } catch (final InterruptedException e) {
            Log.d(TAG,"exception closing the rendering thread in onPause()");
        }
    }

    /**
     * Log battery information when battery level changes
     * @param intent
     */
    public void onBatteryChanged(Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());
            appendLog(percentage + "% at " + currentDateandTime);
        }
    }

    /**
     * append text to the log file
     * @param text
     */
    public void appendLog(String text) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/batteryLog.txt";
        File log = new File(path);
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
        } else {
            try {
                BufferedWriter bfw = new BufferedWriter(new FileWriter(path, true));
                bfw.write(text);
                bfw.write("\n");
                bfw.flush();
                bfw.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    // Permission related functions
    private boolean checkCameraPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        // Check if the Camera permission is already available.
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }


    private void requestCameraPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // TODO: Permission Rationale
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        openCamera();
    }
}

