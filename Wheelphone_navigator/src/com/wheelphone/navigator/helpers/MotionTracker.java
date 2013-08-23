package com.wheelphone.navigator.helpers;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.hardware.Camera.Size;
import android.util.Log;

import com.wheelphone.helpers.CameraHandler;
import com.wheelphone.helpers.CameraViewOverlay;

public class MotionTracker implements CameraHandler.FrameProcessor {
	private static final String TAG = MotionTracker.class.getName();
	private Mat mYuv, mRgba, mGray, mOutput;

//	private MotionTrackerNativeWrapper mNativeMotionTrackerWrapper;
	private CameraViewOverlay mCameraViewOverlay;
	private Scalar mEmptyPixel = new Scalar(0);

	//Desired rotation should be a value between -1 and 1
	private double mDesiredRotation;

	private MotionTrackerListener mMotionTrackerListener;
	private MotionTrackerColorListener mMotionTrackerListenerColor;
	
	
	// Lower and Upper bounds for range checking in HSV color space
	private List<Scalar[]> mBounds = new ArrayList<Scalar[]>();

	// Color radius for range checking in HSV color space
	private Scalar mColorRadius = new Scalar(25,50,50,0); //new Scalar(25,50,50,0);
	private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
	private Point mBlobCenter;
	private int mTargetIdx;
	private int mDesiredAcceleration;
	private volatile boolean mIsExploring;
	private volatile boolean mIsTargetVisible;
	private long mTimestampLastSeen;


	public MotionTracker(CameraViewOverlay cameraViewOverlay) {
		mCameraViewOverlay = cameraViewOverlay;
//		mNativeMotionTrackerWrapper = new MotionTrackerNativeWrapper();
	}

	/*
	 * Interface that should be implemented by classes that would like to be notified by this class events. (Observer pattern)
	 */
	public interface MotionTrackerListener{
		public void onDesiredRotationChange();
		public void onTargedReached();
	}

	/*
	 * Observer pattern glue code:
	 */
	public void setMotionTrackerListener(MotionTrackerListener eventListener) {
		Log.d(TAG, "setMotionTrackerListener");
		//Start by exploring the area:
		mIsExploring = true;
		mDesiredAcceleration = 0;
		mDesiredRotation = 1;
		mMotionTrackerListener = eventListener;
		if (mMotionTrackerListener != null) 
			mMotionTrackerListener.onDesiredRotationChange();
	}

	/*
	 * Interface that should be implemented by classes that would like to be notified by this class events. (Observer pattern)
	 */
	public interface MotionTrackerColorListener{
		public void onTargetChange(int selectedColor);
	}

	/*
	 * Observer pattern glue code:
	 */
	public void setMotionTrackerColorListener(MotionTrackerColorListener eventListener) {
		Log.d(TAG, "setMotionTrackerColorListener");
		mMotionTrackerListenerColor = eventListener;
	}

	@Override
	public void process(byte[] frame, Size frameSize) {
		if (mYuv == null){
			mYuv = new Mat(frameSize.width + frameSize.width / 2, frameSize.height, CvType.CV_8UC1);
			Log.d(TAG, "Allocating mYuv");
		}
		if (mOutput == null){
			mOutput = new Mat(frameSize.width, frameSize.height, CvType.CV_8UC3);

			//		    mOutput = new Mat(frameSize.height, frameSize.width, CvType.CV_8UC3);
			Log.d(TAG, "Allocating mOutput");
		}
		mOutput.setTo(mEmptyPixel);

		setYuv(frame);
		getRgba();
		setGray(frameSize);


		//        Log.d(TAG, "before: " + mOutput.cols() + "x" + mOutput.rows());
		//        mNativeMotionTrackerWrapper.process(mRgba, mGray, mOutput);


		getTargetLocation();

		//        Log.d(TAG, "rotation: " + mNativeMotionTrackerWrapper.getDesiredRotation() + ". target visible: " + mNativeMotionTrackerWrapper.isFollowingTarget());

		//	    mCameraViewOverlay.setImage(mOutput);
		mCameraViewOverlay.setImage(mRgba, mContours);
		mCameraViewOverlay.postInvalidate();
	}

	private synchronized void setYuv(byte[] frame) {
		mYuv.put(0, 0, frame);
	}

	@Override
	public synchronized Mat getRgba(){
		if (mRgba == null){
			mRgba = new Mat();
			Log.d(TAG, "Allocating mRgba");
		}
		Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
		return mRgba; 
	}

	private synchronized void setGray(Size frameSize){
		mGray = mYuv.submat(0, frameSize.width, 0, frameSize.height);
	}

	private Scalar[] getBoundsArray(double lowH, double highH, double lowS, double highS, double lowV, double highV) {
		Scalar bounds[] = new Scalar[2];
		bounds[0] = new Scalar(0);
		bounds[1] = new Scalar(0);

		//H
		bounds[0].val[0] = (lowH < 0 || lowH > 255) ? 0 : lowH;
		bounds[1].val[0] = (highH < 0 || highH > 255) ? 255 : highH;

		//S
		bounds[0].val[1] = (lowS < 0 || lowS > 255) ? 0 : lowS;
		bounds[1].val[1] = (highS < 0 || highS > 255) ? 255 : highS;

		//V
		bounds[0].val[2] = (lowV < 0 || lowV > 255) ? 0 : lowV;
		bounds[1].val[2] = (highV < 0 || highV > 255) ? 255 : highV;

		bounds[0].val[3] = 0;
		bounds[1].val[3] = 255;

		return bounds;
		
	}

	private synchronized void getTargetLocation(){
		if (mBounds.size() < 1)
			return;
		Mat pyrDownMat = new Mat();

		Imgproc.pyrDown(mRgba, pyrDownMat);
		Imgproc.pyrDown(pyrDownMat, pyrDownMat);

		Mat hsvMat = new Mat();
		Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

		Mat Mask = new Mat();
		Core.inRange(hsvMat, mBounds.get(mTargetIdx)[0], mBounds.get(mTargetIdx)[1], Mask);
		Mat dilatedMask = new Mat();
		Imgproc.dilate(Mask, dilatedMask, new Mat());

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();

		Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		mBlobCenter = new Point();

		mContours.clear();
		
		mIsTargetVisible = false;

		if(contours.size() > 0) {

			// Find max contour area of the two biggest
			double maxArea = 0;
			int contourIndex = 0;
			int index=0;
			double area = 0;
			for (MatOfPoint wrapper : contours){
				area = Imgproc.contourArea(wrapper);
				if (area > maxArea) {
					maxArea = area;
					contourIndex = index;
				}
				index++;
			}

			Rect boundingRect = Imgproc.boundingRect(contours.get(contourIndex));
			
//			Log.d(TAG, "X: " + boundingRect.x + ". TL: " + boundingRect.tl());
			mBlobCenter.x = boundingRect.x + boundingRect.width/2;
			mBlobCenter.y = boundingRect.y + boundingRect.height/2;

			int boundingRectPercentualWidth = 100 * boundingRect.width / pyrDownMat.width();
			int boundingRectPercentualHeight = 100 * boundingRect.height / pyrDownMat.height();
			
//			Log.d(TAG, "Width %: " + 100 * boundingRect.width / pyrDownMat.width());
//			Log.d(TAG, "diff: " + Math.abs(boundingRect.width - boundingRect.height));
//			Log.d(TAG, "%: " + boundingRect.width/20);

			
			//Only notify/draw if the tracked object is large enough (enclosing rectangle is at least 13% of the width).
			if (boundingRectPercentualWidth > 13){
				
				mIsTargetVisible = true;
				mTimestampLastSeen = System.currentTimeMillis();
				
				followTarget(pyrDownMat);
				
				// Make sure that we draw the largest contour on the screen:
				Core.multiply(contours.get(contourIndex), new Scalar(4,4), contours.get(contourIndex));
				mContours.add(contours.get(contourIndex));

				//if enclosing rectangle's width is larger than 80% of the screen or height 60%. Notify listener that target has been reached (probably the listener would make this class iterate to the next target)
				if (boundingRectPercentualWidth > 80 || boundingRectPercentualHeight > 60){
					if (mMotionTrackerListener != null)
						mMotionTrackerListener.onTargedReached();
				}
				if (mMotionTrackerListener != null)
					mMotionTrackerListener.onDesiredRotationChange();
			} 
			
		} else if (System.currentTimeMillis() - mTimestampLastSeen > 10000) {//If more than 10 seconds have happend since obstacle was last seen, explore
			//rotate in place to find the direction to go:
			mDesiredRotation = 1;
			mDesiredAcceleration = 0;
			
			if (mMotionTrackerListener != null)
				mMotionTrackerListener.onDesiredRotationChange();
		}
	}

	public List<MatOfPoint> getContours() {
		return mContours;
	}

	private synchronized void followTarget(Mat pyrDownMat) {
		mIsExploring = false;
		// Target is/was visible, advance while trying to reach it:
		mDesiredRotation = ((2 * mBlobCenter.x) / pyrDownMat.width()) - 1;
		mDesiredAcceleration = 1;

	}

	public synchronized boolean nextTarget() {
		if (mBounds.size() < 2)
			return false;
		
		mIsExploring = true;
		
		//rotate in place to find the direction to go:
		mDesiredRotation = 1;
		mDesiredAcceleration = 0;
		
		mTargetIdx = mTargetIdx + 1;
		sanitizeTargetIdx();
		if (mMotionTrackerListener != null)
			mMotionTrackerListener.onDesiredRotationChange();

		if (mMotionTrackerListenerColor != null)
			mMotionTrackerListenerColor.onTargetChange(mTargetIdx);
		return true;
	}

	private void sanitizeTargetIdx(){
		if (mBounds.size() == 0)
			mTargetIdx = 0;
		else
			mTargetIdx = (mTargetIdx % mBounds.size());
	}

	public synchronized void deleteCurrent() {
		mBounds.remove(mTargetIdx);
		mTargetIdx = Math.min(mTargetIdx, mBounds.size()-1); 
		mContours.clear();
		if (mMotionTrackerListenerColor != null)
			mMotionTrackerListenerColor.onTargetChange(mTargetIdx);
	}

	public int getTargetCount() {
		return mBounds.size();
	}

	public boolean isTargetVisible() {
		return mIsTargetVisible;
	}
	
	public boolean isExploring() {
		return mIsExploring;
	}

	public double getDesiredRotation() {
		return mDesiredRotation;
	}

	public int getDesiredLinearAcc() {
		return mDesiredAcceleration;
	}

	//Returns the range of the currently selected color. It is an Scalar array of size 2, position 0 is for lower bound and position 1 is for upper bound
	public Scalar[] getHsvRanges(int idx) {
		if (idx < mBounds.size() && idx >= 0){
			Log.i(TAG, "getHsvRanges. Returning HSV ranges for: " + idx + ", BoundsL: " + mBounds.get(idx)[0] + ". BoundsH: " + mBounds.get(idx)[1]);
			return mBounds.get(idx);
		} else { 
			return null;
		}
	}

	//returns the current color (takes min-max values and returns the value of the middle of the range):
	//Returned float array is scaled as follows: Hue [0...360) hsv[1] is Saturation [0...1] hsv[2] is Value [0...1]
	public float[] getHsvColor() {
		float [] color = new float[3];
		//H:
		color[0] = (float) (360 * ((mBounds.get(mTargetIdx)[0].val[0] + mBounds.get(mTargetIdx)[1].val[0]) / 2) / 255 );
		//S:
		color[1] = (float) ((mBounds.get(mTargetIdx)[0].val[1] + mBounds.get(mTargetIdx)[1].val[1]) / 2) / 255;
		//V:
		color[2] = (float) ((mBounds.get(mTargetIdx)[0].val[2] + mBounds.get(mTargetIdx)[1].val[2]) / 2) / 255;
		return color;
	}

	public void setTargetHSV(Double minH, Double maxH, Double minS, Double maxS, Double minV, Double maxV) {
		mBounds.set(mTargetIdx, getBoundsArray(minH, maxH,//H
							      			   minS, maxS,//S
					      					   minV, maxV //V
					      					    ));
	}

	public void addColor(Scalar hsvColor){		
		mBounds.add(getBoundsArray(hsvColor.val[0]-mColorRadius.val[0], hsvColor.val[0]+mColorRadius.val[0],  //H
								   hsvColor.val[1]-mColorRadius.val[1], hsvColor.val[1]+mColorRadius.val[1],  //S
								   hsvColor.val[2]-mColorRadius.val[2], hsvColor.val[2]+mColorRadius.val[2]));//V
		postColorAddition();
	}

	public void addColorWithRanges(double minH, double maxH, double minS, double maxS, double minV, double maxV) {
		mBounds.add(getBoundsArray(minH, maxH,  //H
   			   					   minS, maxS,  //S
   			   					   minV, maxV));//V
		postColorAddition();
	}
	
	private void postColorAddition(){
		mTargetIdx = mBounds.size() - 1;

		Log.i(TAG, "Added boundsL: " + mBounds.get(mTargetIdx)[0] + ". BoundsH: " + mBounds.get(mTargetIdx)[1]);

		if (mMotionTrackerListenerColor != null)
			mMotionTrackerListenerColor.onTargetChange(mTargetIdx);
	}
}
