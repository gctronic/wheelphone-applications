package com.wheelphone.navigator.helpers;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import android.util.Log;

public class MotionTrackerNativeWrapper{
	private static final String TAG = MotionTrackerNativeWrapper.class.getName();

    public float process(Mat input, Mat inputGray, Mat output) {
    	return nativeProcess(input.getNativeObjAddr(), inputGray.getNativeObjAddr(), output.getNativeObjAddr());
    }

    public float getDesiredRotation(){
    	return nativeGetDesiredRotation();
    }
    public boolean isFollowingTarget(){
    	return nativeIsFollowingTarget();
    }
    
	public void addTarget() {
		nativeAddTarget();
	}

	public boolean targetReached() {
		return nativeTargetReached();
	}
	
	public void updateRangeBounds(double[] newSample) {
		Log.d(TAG, "newSample: " + newSample[0] + ", " + newSample[1] + ", " + newSample[2]);
		nativeUpdateRangeBounds(newSample);
	}


    public void release() {
        nativeRelease();
    }
    
    private static native float nativeProcess(long inputImage, long inputGrayImage, long outputImage);
    private static native float nativeGetDesiredRotation();
    private static native boolean nativeIsFollowingTarget();
    private static native boolean nativeTargetReached();
    private static native boolean nativeUpdateRangeBounds(double[] newSample);
    private static native boolean nativeAddTarget();
    private static native void nativeRelease();


    //    public void setMovementSensibility(int movementSensibility){
//    	nativeSetMovementSensibility(movementSensibility);
//    }
//    private static native void nativeSetMovementSensibility(int movementSensibility);
//    private static native void nativeInit();
}