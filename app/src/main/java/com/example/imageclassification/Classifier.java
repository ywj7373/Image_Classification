package com.example.imageclassification;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class Classifier {
    private static final String TAG = "Classifier";
    private static final int FLOAT_TYPE_SIZE = 4;
    private static final int  CHANNEL_SIZE = 3;
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    private Activity activity;
    private Boolean isInitialized;
    private ArrayList<String> labels;

    private Interpreter interpreter;
    private Delegate gpuDelegate = null;
    private Delegate nnAPIDelegate = null;

    private int width = 0;
    private int height = 0;
    private int modelSize = 0;

    public Classifier(Activity activity) {
        this.activity = activity;
    }

    void initialize(String device, int numThreads) {
        try {
            AssetManager assetManager = activity.getAssets();
            MappedByteBuffer model = loadModelFile(assetManager, "mobilenet_v1_1.0_224.tflite");
            labels = loadLines(activity, "labels.txt");

            Interpreter.Options options = new Interpreter.Options();
            switch (device) {
                case "GPU": {
                    gpuDelegate = new GpuDelegate();
                    options.addDelegate(gpuDelegate);
                    break;
                }
                case "nnAPI": {
                    nnAPIDelegate = new NnApiDelegate();
                    options.addDelegate(nnAPIDelegate);
                    break;
                }
                default:
                    break;
            }
            options.setNumThreads(numThreads);
            interpreter = new Interpreter(model, options);

            int[] inputShape = interpreter.getInputTensor(0).shape();
            width = inputShape[1];
            height = inputShape[2];
            //Log.d(TAG, "width: " + width + " height: " + height);
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Log.d(TAG, "D" + outputShape[1]);
            modelSize = FLOAT_TYPE_SIZE * width * height * CHANNEL_SIZE;
            isInitialized = true;
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    String classify(Bitmap bitmap) {
        if (isInitialized) {
            // Convert to byteBuffer
            long startTimePreprocess = SystemClock.uptimeMillis();
            Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, width, height, true);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);
            long endTimePreprocess = SystemClock.uptimeMillis();

            // Log conversion time
            long preprocessTime = endTimePreprocess - startTimePreprocess;
            Log.d("Time", "Preprocess time: " + preprocessTime + "ms");

            float[][] output = new float[1][labels.size()];

            // Run inference model
            long startTime = SystemClock.uptimeMillis();
            interpreter.run(byteBuffer, output);
            long endTime = SystemClock.uptimeMillis();

            // Log inference time
            long inferenceTime = endTime - startTime;
            Log.d("Time", "Inference time: " + inferenceTime + "ms");

            int index = getMaxIndex(output[0]);
            return labels.get(index) + " " + inferenceTime + " " + preprocessTime;
        }
        return "Unknown";
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private ArrayList<String> loadLines(Context context, String filename) throws IOException {
        Scanner scanner = new Scanner(new InputStreamReader(context.getAssets().open(filename)));
        ArrayList<String> labels = new ArrayList<>();
        while(scanner.hasNextLine()) {
            labels.add(scanner.nextLine());
        }
        scanner.close();
        return labels;
    }

    private int getMaxIndex(float[] result) {
        float max = result[0];
        int index = 0;
        for(int i = 0; i < result.length; i++) {
            if (result[i] > max) {
                max = result[i];
                index = i;
            }
        }
        return index;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(modelSize);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[width * height];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                final int val = intValues[pixel++];
                    byteBuffer.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    byteBuffer.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        bitmap.recycle();
        return byteBuffer;
    }
}
