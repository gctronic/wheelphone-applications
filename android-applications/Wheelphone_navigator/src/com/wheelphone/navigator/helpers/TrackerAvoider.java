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

public class TrackerAvoider implements CameraHandler.FrameProcessor {
	private static final String TAG = TrackerAvoider.class.getName();
	private Mat mYuv, mRgba, mGray, mOutput;

	private TrackerAvoiderNativeWrapper mNativeMotionTrackerWrapper;
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
	private Rect mTargetBoundingRect;

	private double mTargetYDisplacement;
	private double mTargetLastY;
	private boolean mUseOpticalFlow;


	public TrackerAvoider(CameraViewOverlay cameraViewOverlay, boolean useOpticalflow) {
		mCameraViewOverlay = cameraViewOverlay;
		mNativeMotionTrackerWrapper = new TrackerAvoiderNativeWrapper();
		mUseOpticalFlow = useOpticalflow;
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


		getTargetLocation();
		
		mOutput = mRgba.clone();
		
		//If not exploring
		if (!mIsExploring && mUseOpticalFlow && mTargetYDisplacement > 0)
			mNativeMotionTrackerWrapper.process(mRgba, mGray, mOutput, mTargetYDisplacement);
		
		if (mMotionTrackerListener != null)
			mMotionTrackerListener.onDesiredRotationChange();

		//        Log.d(TAG, "rotation: " + mNativeMotionTrackerWrapper.getDesiredRotation() + ". target visible: " + mNativeMotionTrackerWrapper.isFollowingTarget());

//		mCameraViewOverlay.setImage(mRgba, mContours);
		mCameraViewOverlay.setImage(mOutput, mContours);
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

		Mat hsvMat = new Mat();
		Imgproc.cvtColor(mRgba, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

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
		
		mTargetYDisplacement = 0;

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

			//scale up the retrieved contour:
//			Core.multiply(contours.get(contourIndex), new Scalar(4,4), contours.get(contourIndex));

			mTargetBoundingRect = Imgproc.boundingRect(contours.get(contourIndex));
			
			//Assuming that the screen is inclinated, the lower part of the target is the fastest moving part of the target, so use it as lower bound...anything moving slower than it is ignored (obstacles are closer than the target, thus they should displace faster than this point of reference)
			mTargetYDisplacement = mTargetBoundingRect.br().y - mTargetLastY;
			mTargetLastY = mTargetBoundingRect.br().y;
			
//			Log.d(TAG, "displacement Y: " + mTargetYDisplacement);
			mBlobCenter.x = mTargetBoundingRect.x + mTargetBoundingRect.width/2;
			mBlobCenter.y = mTargetBoundingRect.y + mTargetBoundingRect.height/2;

			int boundingRectPercentualWidth = 100 * mTargetBoundingRect.width / mRgba.width();
			int boundingRectPercentualHeight = 100 * mTargetBoundingRect.height / mRgba.height();
			
//			Log.d(TAG, "Width %: " + boundingRectPercentualWidth);
//			Log.d(TAG, "Height %: " + boundingRectPercentualHeight);
			
			//Only notify/draw if the tracked object is large enough (enclosing rectangle is at least 13% of the width).
			if (boundingRectPercentualWidth > 13){
				
				mIsTargetVisible = true;
				mTimestampLastSeen = System.currentTimeMillis();
				
				followTarget();
				
				// Make sure that we draw the largest contour on the screen:
				mContours.add(contours.get(contourIndex));

				//if enclosing rectangle's width is larger than 80% of the screen or height 60%. Notify listener that target has been reached (probably the listener would make this class iterate to the next target)
				if (boundingRectPercentualWidth > 80 || boundingRectPercentualHeight > 60){
					if (mMotionTrackerListener != null)
						mMotionTrackerListener.onTargedReached();
				}
			} 
			
		} else if (System.currentTimeMillis() - mTimestampLastSeen > 10000) {//If more than 10 seconds have happend since obstacle was last seen, explore
//			Log.d(TAG, "LOOOOOST!!! exploring");
			mIsExploring = true;
			//rotate in place to find the direction to go:
			mDesiredRotation = 1;
			mDesiredAcceleration = 0;
		}
	}

	public List<MatOfPoint> getContours() {
		return mContours;
	}

	private synchronized void followTarget() {
		mIsExploring = false;
		// Target is/was visible, advance while trying to reach it:
		//Set the rotation on a range between -1 and 1:
		mDesiredRotation = (2 * (mBlobCenter.x / mRgba.width())) - 1;
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
		if (mUseOpticalFlow && mNativeMotionTrackerWrapper.getDesiredRotation() != 0)//if we see an obstacle, try to avoid it:
			return 1.5*mNativeMotionTrackerWrapper.getDesiredRotation();
		else
			return mDesiredRotation;
	}

	public int getDesiredLinearAcc() {
		return mDesiredAcceleration;
	}

//	Returns the range of the currently selected color. It is an Scalar array of size 2, position 0 is for lower bound and position 1 is for upper bound
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
	
	public double getTargetYDisplacement(){
		return mTargetYDisplacement;
	}
}