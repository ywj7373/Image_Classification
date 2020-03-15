package com.example.imageclassification;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class ModifiedCameraView extends JavaCameraView {
    private static final String TAG = "ModifiedJavaCameraView";

    public ModifiedCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreviewFPS(double  min, double max){
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFpsRange((int)(min*1000), (int)(max*1000));
        //params.setPreviewFrameRate(min);
        mCamera.setParameters(params);
        // mCamera.getSupportedPreviewFpsRange();
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(int width, int height) {
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewSize(mFrameWidth, mFrameHeight);
        mCamera.setParameters(params);
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

}
