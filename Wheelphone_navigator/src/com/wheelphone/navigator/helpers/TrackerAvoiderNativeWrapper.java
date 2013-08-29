package com.wheelphone.navigator.helpers;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import android.util.Log;

public class TrackerAvoiderNativeWrapper{
	private static final String TAG = TrackerAvoiderNativeWrapper.class.getName();

    public void process(Mat input, Mat inputGray, Mat output, double minDisplacement) {
    	nativeProcess(input.getNativeObjAddr(), inputGray.getNativeObjAddr(), output.getNativeObjAddr(), minDisplacement);
    }

    public float getDesiredRotation(){
    	return nativeGetDesiredRotation();
    }

    public void release() {
        nativeRelease();
    }
    
    private static native void nativeProcess(long inputImage, long inputGrayImage, long outputImage, double minDisplacement);
    private static native float nativeGetDesiredRotation();
    private static native void nativeRelease();

}