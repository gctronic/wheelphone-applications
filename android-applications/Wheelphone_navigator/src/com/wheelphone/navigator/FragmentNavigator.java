package com.wheelphone.navigator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Scalar;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelphone.R;
import com.wheelphone.helpers.CameraHandler;
import com.wheelphone.helpers.CameraViewOverlay;
import com.wheelphone.navigator.helpers.TrackerAvoider;
import com.wheelphone.navigator.helpers.TrackerAvoider.MotionTrackerColorListener;
import com.wheelphone.navigator.helpers.TrackerAvoider.MotionTrackerListener;
import com.wheelphone.util.RangeSeekBar;
import com.wheelphone.util.RangeSeekBar.OnRangeSeekBarChangeListener;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;
//import com.wheelphone.util.LogcatStreamer;

public class FragmentNavigator extends Fragment implements MotionTrackerListener, WheelPhoneRobotListener, MotionTrackerColorListener {

	private static final String TAG = FragmentNavigator.class.getName();

	private static TextView mOutput;

	private static WheelphoneRobot mWheelphone;

	private static int mLeftSpeed;
	private static int mRightSpeed;

	private static boolean mIsMoving = false;

	private SurfaceView mCameraSurfaceView;
	private CameraHandler mCameraHandler;
	private CameraViewOverlay mCameraViewOverlay;

	private TrackerAvoider mFrameProcessor;

	private boolean mIsUpdatingTarget;

	private boolean mIsGoing = false;
	
	private double mObstacleDesiredRotation;
	private double mObstacleDesiredAcceleration;

	private long mObstacleLastSeenTimestamp;

	// Scaling factor for each spring. Since the spring values come between -1 and 1, this value defines the maximum contribution by each spring
	private static final int LINEAR_SPRING_CONST = 50;//100
	private static final int ANGULAR_SPRING_CONST = 30;//30
	private static final double DUMPING_FACTOR = 1;

//	private LogcatStreamer mLogcatStreamer;

	private Button mButtonNext, mButtonStart, mButtonDel, mButtonAdd, mButtonStop;
	private RangeSeekBar<Double> mRangeSeekBarH,  mRangeSeekBarS, mRangeSeekBarV;

	private boolean mUseProximity;

	private boolean mUseOpticalflow;

	private boolean mMenuState = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_navigator,
				container, false);
		//Read the stored preferences:
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mUseProximity = sharedPref.getBoolean("pref_proximity", false);
		mUseOpticalflow = sharedPref.getBoolean("pref_opticalflow", false);
		boolean useSpeedControl = sharedPref.getBoolean("pref_speedcontrol", false);
		
		mOutput = (TextView)rootView.findViewById(R.id.output);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity().getApplicationContext(), getActivity().getIntent());
		mWheelphone.startUSBCommunication();
		//		mWheelphone.enableSoftAcceleration();
		if (useSpeedControl) {
			mWheelphone.enableSpeedControl();
			Toast.makeText(getActivity(), "Speed control ENABLED", Toast.LENGTH_SHORT).show();
		} else {
			mWheelphone.disableSpeedControl();
			Toast.makeText(getActivity(), "Speed control DISABLED", Toast.LENGTH_SHORT).show();
		}
		
		mWheelphone.disableSoftAcceleration();
		//		mWheelphone.enableObstacleAvoidance();
		//		mWheelphone.disableSpeedControl();


		//Keep track of the overlay view, it has to be given to the processor so it can output into it
		mCameraViewOverlay = (CameraViewOverlay) rootView.findViewById(R.id.camera_preview_overlay);
		mCameraViewOverlay.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//				Log.d(TAG, "touched: " + event.getAction());

				if (mIsUpdatingTarget && event.getAction() == MotionEvent.ACTION_DOWN){
					Scalar HSVcolor = mCameraViewOverlay.getPixel(mFrameProcessor.getRgba(), (int)event.getX(), (int)event.getY());
					
					//Add picked color to the processor and the overlay
					mFrameProcessor.addColor(HSVcolor);
					//Get the currently selected color HSV medium values (expects the HSV values of the middle of the range we sent) and update the overlay:
					mCameraViewOverlay.addColor(mFrameProcessor.getHsvColor());


					Toast.makeText(getActivity(), "Target (color) added", Toast.LENGTH_SHORT).show();

					mIsUpdatingTarget = false;

					checkEnableStatus();
					
					return true;
				}

				return false;
			}

		});
		
		//		mOpenCvCameraView = (CameraBridgeViewBase) rootView.findViewById(R.id.camerapreview);
		mCameraSurfaceView = (SurfaceView) rootView.findViewById(R.id.camera_preview);
		mCameraHandler = new CameraHandler(getActivity(), mCameraSurfaceView.getHolder());
		mFrameProcessor = new TrackerAvoider(mCameraViewOverlay, mUseOpticalflow);

		mButtonAdd = (Button) getActivity().findViewById(R.id.button_add);
		mButtonAdd.setOnClickListener(mButtonsListener);

		mButtonDel = (Button) getActivity().findViewById(R.id.button_del);
		mButtonDel.setOnClickListener(mButtonsListener);

		mButtonStart = (Button) getActivity().findViewById(R.id.button_start);
		mButtonStart.setOnClickListener(mButtonsListener);

		mButtonStop = (Button) getActivity().findViewById(R.id.button_stop);
		mButtonStop.setOnClickListener(mButtonsListener);

		mButtonNext = (Button) getActivity().findViewById(R.id.button_next);
		mButtonNext.setOnClickListener(mButtonsListener);

		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.frame_edit);

		if (layout.getChildCount() == 0) {
			mRangeSeekBarH = new RangeSeekBar<Double>(0., 255., getActivity());
			mRangeSeekBarH.setOnRangeSeekBarChangeListener(mSeekBarListener);
			layout.addView(mRangeSeekBarH);
	
			mRangeSeekBarS = new RangeSeekBar<Double>(0., 255., getActivity());
			mRangeSeekBarS.setOnRangeSeekBarChangeListener(mSeekBarListener);
			layout.addView(mRangeSeekBarS);
	
			mRangeSeekBarV = new RangeSeekBar<Double>(0., 255., getActivity());
			mRangeSeekBarV.setOnRangeSeekBarChangeListener(mSeekBarListener);
			layout.addView(mRangeSeekBarV);
		}
		
		return rootView;
	}

	
	
	private OnRangeSeekBarChangeListener<Double> mSeekBarListener = new OnRangeSeekBarChangeListener<Double>() {
		@Override
		public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Double minValue, Double maxValue) {
			mFrameProcessor.setTargetHSV(mRangeSeekBarH.getSelectedMinValue(), mRangeSeekBarH.getSelectedMaxValue(), 
					mRangeSeekBarS.getSelectedMinValue(), mRangeSeekBarS.getSelectedMaxValue(),
					mRangeSeekBarV.getSelectedMinValue(), mRangeSeekBarV.getSelectedMaxValue());

			mCameraViewOverlay.setColor(mFrameProcessor.getHsvColor());
		}
	};

	private View.OnClickListener mButtonsListener = new View.OnClickListener() {
		@Override
		public void onClick(View button) {
			switch (button.getId()){
			case R.id.button_add:
				Log.d(TAG, "add");
				Toast.makeText(getActivity(), "Tap on the screen to add select the target color", Toast.LENGTH_SHORT).show();
				mIsUpdatingTarget = true;
				break;
			case R.id.button_del:
				Log.d(TAG, "del");
				mCameraViewOverlay.deleteCurrent();
				mFrameProcessor.deleteCurrent();
				break;
			case R.id.button_start:
				mIsGoing = true;
				mFrameProcessor.setMotionTrackerListener(FragmentNavigator.this);
				Log.d(TAG, "start");
				break;
			case R.id.button_stop:
				mIsGoing = false;
				setSpeed(0, 0);
				mFrameProcessor.setMotionTrackerListener(null);
				Log.d(TAG, "stop");
				break;
			case R.id.button_next:
				mFrameProcessor.nextTarget();
				Log.d(TAG, "next");
				break;
			}
			checkEnableStatus();
			
		}
	};
	
	public boolean getMenuState(){
		return mMenuState;
	}
	
	private void checkEnableStatus() {
		mButtonNext.setEnabled(false);
		mButtonStart.setEnabled(false);
		mButtonDel.setEnabled(false);
		mButtonAdd.setEnabled(false);
		mButtonStop.setEnabled(false);
		
		mRangeSeekBarH.setEnabled(false);
		mRangeSeekBarS.setEnabled(false);
		mRangeSeekBarV.setEnabled(false);
		
		mMenuState = false; //disabled
		
		if (mIsUpdatingTarget)
			return;

		if (mIsGoing){
			mButtonStop.setEnabled(true);
			mButtonNext.setEnabled(true);
			return;
		}

		mMenuState = true; //enabled
		
		mButtonAdd.setEnabled(true);
		
		if (mFrameProcessor.getTargetCount() > 0){
			mRangeSeekBarH.setEnabled(true);
			mRangeSeekBarS.setEnabled(true);
			mRangeSeekBarV.setEnabled(true);
			
			mButtonDel.setEnabled(true);

			if (mFrameProcessor.getTargetCount() > 1){
				mButtonNext.setEnabled(true);
				mButtonStart.setEnabled(true);
			}
		}
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native library after(!) OpenCV initialization
				System.loadLibrary("obstacles_tracker");

				mCameraHandler.setFrameProcessor(mFrameProcessor);
				mFrameProcessor.setMotionTrackerColorListener(FragmentNavigator.this);
				
				insertColors();

			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}

	};

	
	private void insertColors() {
		getActivity();
		// Restore preferences
		SharedPreferences settings = getActivity().getSharedPreferences(TAG, Activity.MODE_PRIVATE);
		
		int rangesCount = settings.getInt("rangesCount", 0);
		if (mFrameProcessor.getTargetCount() == 0){//if app has no colors already
			if (rangesCount > 0){//if have at least one color stored in the preferences				
				Log.i(TAG, "insert, count:" + mFrameProcessor.getTargetCount());
				int targetCounter, hsvCounter;
				for (targetCounter=0 ; targetCounter<rangesCount ; targetCounter++){
					//Both ranges (lower and upper) in same array
					double ranges[] = new double[6];
	
					//Lower
					for (hsvCounter=0 ; hsvCounter<3 ; hsvCounter++){
						//Can't put doubles...so use a long (same size, so no precision lost):
						ranges[hsvCounter] = Double.longBitsToDouble(settings.getLong("low_" + targetCounter + "_" + hsvCounter, 0));
					}
	
					//Upper
					for (hsvCounter=0 ; hsvCounter<3 ; hsvCounter++){
						//Can't put doubles...so use a long (same size, so no precision lost):
						ranges[hsvCounter+3] = Double.longBitsToDouble(settings.getLong("high_" + targetCounter + "_" + hsvCounter, 0));
					}
					
					Log.d(TAG, "ranges: " + ranges);
					mFrameProcessor.addColorWithRanges(ranges[0], ranges[3], //H
													   ranges[1], ranges[4], //S
													   ranges[2], ranges[5]);//V
					//Get the currently selected color HSV medium values (expects the HSV values of the middle of the range we sent) and update the overlay:
					mCameraViewOverlay.addColor(mFrameProcessor.getHsvColor());
	
					
				}
			} else {
				//Yellow (H: 60/360)
				mFrameProcessor.addColorWithRanges( 35,  56, //H
										   		   100, 255, //S
										   		   100, 255);//V
				//Get the currently selected color HSV medium values (expects the HSV values of the middle of the range we sent) and update the overlay:
				mCameraViewOverlay.addColor(mFrameProcessor.getHsvColor());
				
				//Green (H: 120/360)
				mFrameProcessor.addColorWithRanges( 67, 125, //H
								      			   125, 255, //S
						      					    40, 240);//V
				
				//Get the currently selected color HSV medium values (expects the HSV values of the middle of the range we sent) and update the overlay:
				mCameraViewOverlay.addColor(mFrameProcessor.getHsvColor());
				
				//Magenta (H: 300/360
				mFrameProcessor.addColorWithRanges(200, 255, //H
												   125, 255, //S
												    70, 255);//V
				//Get the currently selected color HSV medium values (expects the HSV values of the middle of the range we sent) and update the overlay:
				mCameraViewOverlay.addColor(mFrameProcessor.getHsvColor());
				Log.i(TAG, "insert: inserted hardcode!");
			}
		}
	}


	@Override
	public void onResume() {
	    Log.i(TAG, "Trying to load OpenCV library");
	    if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, getActivity(), mLoaderCallback)) {
	        Log.e(TAG, "Cannot connect to OpenCV Manager");
	    }
		
		mWheelphone.resumeUSBCommunication();
		if(mUseProximity)
			mWheelphone.setWheelPhoneRobotListener(this);
		
//		mLogcatStreamer = new LogcatStreamer();
		super.onResume();
	}

	@Override
	public void onPause() {		
		super.onPause();
		
//		mLogcatStreamer.stop();

		mWheelphone.setWheelPhoneRobotListener(null);
		mCameraHandler.setFrameProcessor(null);
		//Stop robot before disconnecting:
		setSpeed(0, 0);
		mWheelphone.pauseUSBCommunication();

		// Store current colors:
		storeCurrentColors();
	}


	private void storeCurrentColors() {
		SharedPreferences settings = getActivity().getSharedPreferences(TAG, Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		
		Log.d(TAG, "onPause, saving " + mFrameProcessor.getTargetCount() + " colors");
		int targetCounter, hsvCounter;
		for (targetCounter=0 ; targetCounter<mFrameProcessor.getTargetCount() ; targetCounter++){
			Scalar[] ranges = mFrameProcessor.getHsvRanges(targetCounter);

			//Lower
			for (hsvCounter=0 ; hsvCounter<3 ; hsvCounter++){
				//Can't put doubles...so use a long (same size, so no precision lost):
				editor.putLong("low_" + targetCounter + "_" + hsvCounter, Double.doubleToRawLongBits(ranges[0].val[hsvCounter]));
			}

			//Upper
			for (hsvCounter=0 ; hsvCounter<3 ; hsvCounter++){
				//Can't put doubles...so use a long (same size, so no precision lost):
				editor.putLong("high_" + targetCounter + "_" + hsvCounter, Double.doubleToRawLongBits(ranges[1].val[hsvCounter]));
			}
			
		}
		editor.putInt("rangesCount", targetCounter);

		// Commit the edits!
		editor.commit();
	}

	public void showText(final String text) {
		if (mOutput != null){
			mOutput.post(new Runnable() {
				public void run() {
					mOutput.setText(getStatus() + ". " + text);
				} 
			});
		}
	}

	public void onDestroy() {
		super.onDestroy();
	}

	private String getStatus(){
		String status = mWheelphone.isUSBConnected() ? "Connected" : "Disconnected";
		return status + ". L: " + mLeftSpeed + ", R: " + mRightSpeed + ". " + mFrameProcessor.getTargetYDisplacement();
	}

	public static boolean isMoving(){
		return mIsMoving;
	}

	public void setSpeed(int l, int r) {
		mLeftSpeed = l;
		mRightSpeed = r;
		showText("");
		mWheelphone.setSpeed(mLeftSpeed, mRightSpeed);
	}

	@Override
	public void onDesiredRotationChange() {
		calculateSpeeds();
	}

	@Override
	public void onWheelphoneUpdate() {
		if (mFrameProcessor.isExploring() || !mIsGoing){
			mObstacleDesiredRotation = 0;
			mObstacleDesiredAcceleration = 0;
			return;
		}

		int[] frontProx = mWheelphone.getFrontProxs();
		int maxIdx = 0;
		double max = 0;

		//find closest object:
		for (int i=0 ; i<frontProx.length ; i++){
			if (frontProx[i]>max){
				maxIdx = i;
				max = frontProx[i];
			}
		}

		//Rotate away from the closest sensed object (scaling: uses sensor idx to :
		if (max > 40){
			mObstacleLastSeenTimestamp = System.currentTimeMillis();

			//obstacle is present, rotation points away from the sensor that feels the obstacle the closest. Range [-2, 2]. (turn faster than the target seeker) 
			mObstacleDesiredRotation = - (double)2 / (double)((maxIdx <= 1) ? maxIdx-2 : maxIdx-1);
			//the higher the value of the sensor, the closest the obstacle. So the closer the slower we should go. Range [0, 1]
			mObstacleDesiredAcceleration = Math.abs(frontProx[maxIdx] / 255. - 1);
			
			Log.d(TAG, "seen: [" + mObstacleDesiredRotation + ", " + mObstacleDesiredAcceleration + "]");
		} else {
			//Obstacle is on one side of the robot, so go parallel to it:
			mObstacleDesiredRotation = 0;
			mObstacleDesiredAcceleration = 1;
		}

		calculateSpeeds();			
	}

	private synchronized void calculateSpeeds(){
		//angularAcc should come in a range from -1 to +1
		double angularAcc = mFrameProcessor.getDesiredRotation();

		//Spring damping system (should always be between -1 and 1)
		double linearAcc = mFrameProcessor.getDesiredLinearAcc();

		if (!mFrameProcessor.isExploring()){
			if (System.currentTimeMillis() - mObstacleLastSeenTimestamp < 300) {
				//seeing or recently saw an obstacle: use obstacle avoidance rotation and acceleration values 
				angularAcc = mObstacleDesiredRotation;
				linearAcc = mObstacleDesiredAcceleration;
			}
		} 

		mLeftSpeed = (int)(mLeftSpeed
				+   linearAcc * LINEAR_SPRING_CONST
				+	angularAcc * ANGULAR_SPRING_CONST
				-	DUMPING_FACTOR * mLeftSpeed);

		mRightSpeed = (int)(mRightSpeed 
				+   linearAcc * LINEAR_SPRING_CONST
				-	angularAcc * ANGULAR_SPRING_CONST
				-	DUMPING_FACTOR * mRightSpeed);
		showText("");
		mWheelphone.setSpeed(mLeftSpeed, mRightSpeed);
	}

	@Override
	public void onTargedReached() {
		if (mIsGoing){
			mObstacleDesiredRotation = 0;
			mFrameProcessor.nextTarget();
		}
	}

	@Override
	public void onTargetChange(int selectedColor) {
		Log.d(TAG, "onTargetChange: " + selectedColor);
		Scalar []hsvBoundRange = mFrameProcessor.getHsvRanges(selectedColor);
		
		if (hsvBoundRange == null)
			return;

//		Log.d(TAG, "H: " + hsvBoundRange[0].val[0] + ", " + hsvBoundRange[1].val[0]);
//		Log.d(TAG, "S: " + hsvBoundRange[0].val[1] + ", " + hsvBoundRange[1].val[1]);
//		Log.d(TAG, "V: " + hsvBoundRange[0].val[2] + ", " + hsvBoundRange[1].val[2]);

		mRangeSeekBarH.setSelectedRange(hsvBoundRange[0].val[0], hsvBoundRange[1].val[0]);
		mRangeSeekBarS.setSelectedRange(hsvBoundRange[0].val[1], hsvBoundRange[1].val[1]);
		mRangeSeekBarV.setSelectedRange(hsvBoundRange[0].val[2], hsvBoundRange[1].val[2]);

//		Log.d(TAG, "H: " + mRangeSeekBarH.getSelectedMinValue() + ", " + mRangeSeekBarH.getSelectedMaxValue());
//		Log.d(TAG, "S: " + mRangeSeekBarS.getSelectedMinValue() + ", " + mRangeSeekBarS.getSelectedMaxValue());
//		Log.d(TAG, "V: " + mRangeSeekBarV.getSelectedMinValue() + ", " + mRangeSeekBarV.getSelectedMaxValue());

		mCameraViewOverlay.updateTargetIdx(selectedColor);
	}
}