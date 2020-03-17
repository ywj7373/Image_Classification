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


    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(int width, int height) {
        disconnectCamera();
        mMaxHeight = height;
        mMaxWidth = width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void disableAutoFocus() {
        mCamera.cancelAutoFocus();
    }

    public void disableAutoExposure() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(true);
            mCamera.setParameters(parameters);
        }
    }

    public void disableWhiteBalance() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setAutoWhiteBalanceLock(true);
        mCamera.setParameters(parameters);
    }

}
