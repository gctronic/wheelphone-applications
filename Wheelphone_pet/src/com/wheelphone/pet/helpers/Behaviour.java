package com.wheelphone.pet.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wheelphone.facetrack.FaceDetector;
import com.wheelphone.pet.FragmentPet;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class Behaviour implements SensorEventListener, FaceTracking.FaceTrackingListener, WheelphoneRobot.WheelPhoneRobotListener{
	private static String TAG = Behaviour.class.getName();

	private static Random mRrandomGenerator = new Random();


	private static final int MSG_UPDATE_EXPRESSION = 0;
	private static final int MSG_UPDATE_TEXT = 1;
	private static final int MSG_UPDATE_PUPILS_POSITION = 2;
	
	private static final int SPEED_EXPLORE = 20;
	private static final int SPEED_ESCAPE = 50;

	
	public static final Map<Integer, String> EXPRESSIONS_NAMES;
	static
	{
		EXPRESSIONS_NAMES = new HashMap<Integer, String>();
		EXPRESSIONS_NAMES.put(Behaviour.STATE_ANGRY, "ANGRY");
		EXPRESSIONS_NAMES.put(Behaviour.STATE_ANNOYED, "ANNOYED");
		EXPRESSIONS_NAMES.put(Behaviour.STATE_CURIOUS, "CURIOUS");
		EXPRESSIONS_NAMES.put(Behaviour.STATE_NORMAL, "NORMAL"); 
		EXPRESSIONS_NAMES.put(Behaviour.STATE_HAPPY, "HAPPY");
		EXPRESSIONS_NAMES.put(Behaviour.STATE_ROB, "ROB");// ??????
		EXPRESSIONS_NAMES.put(Behaviour.STATE_SCARED, "SCARED");
		EXPRESSIONS_NAMES.put(Behaviour.STATE_SURPRISE, "SURPRISED");
	}

	
	public static final int STATE_NORMAL = 0; //1. Start face
	public static final int STATE_CURIOUS = 1; //2. No face for 5 seconds, do a spin
	public static final int STATE_ANNOYED = 2; //3. Curious for 5 seconds and no face.
	public static final int STATE_ANGRY = 3; //4. Annoyed for 5 seconds
	public static final int STATE_HAPPY = 4; //looking at a face
	public static final int STATE_ROB = 5; // ???
	public static final int STATE_SCARED = 6; //Raised from the table  
	public static final int STATE_SURPRISE = 7; //Something too close (proximity sensor)

	private static FaceExpressions mFaceExpression;
	private static WheelphoneRobot mWheelphone;
	private FaceTracking mFaceTracking;
	private static FragmentPet mFragmentPet;

	private static boolean mInBehaviourTransition = false;//TODO: Instead of having a flag for the behaviour transition, register and unregister eventlisteners for the different functions

	private static int mLeftSpeed = 0;
	private static int mRightSpeed = 0;
	private static float mOldPosX = 0;
	private static long mOldPosTime = 0;
	private static long mNewPosTime = 0;
	
	/* 
	 * Initial state is only one: Normal
	 * PI(normal) = 1
	 */
	private static int mCurrentState = STATE_NORMAL;

	private SensorManager mSensorManager;

	private Sensor mProximity;

	private Talker mTalker;

	public Behaviour(FaceExpressions faceExpression, WheelphoneRobot wheelphone, FaceTracking faceTracking, Talker talker, FragmentPet fragmentPet){
		mFaceExpression = faceExpression;
		mWheelphone = wheelphone;
		mFragmentPet = fragmentPet;
		mFaceTracking = faceTracking;
		mTalker = talker;
		// Get an instance of the sensor service, and use that to get an instance of
		// a particular sensor.
		mSensorManager = (SensorManager) mFragmentPet.getActivity().getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	}

	public void pause(){
		mSensorManager.unregisterListener(this, mProximity); //Proximity (surprise) sensor
		mFaceTracking.removeFaceTrackingListener(); //FaceTracking sensor
		mWheelphone.removeWheelPhoneRobotListener(); //Ground (scared) sensor
		stopBehaviourTransitions(); //basic state transitions
	}

	public void resume(){
		mCurrentState = STATE_NORMAL;
		mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);

		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
		mFaceTracking.setFaceTrackingListener(this);
		mWheelphone.setWheelPhoneRobotListener(this);
		mLeftSpeed = 0;
		mRightSpeed = 0;
		changeSpeed();
		startBehaviourTransitions();
	}
	
	private synchronized void stopBehaviourTransitions(){
		if (!mInBehaviourTransition)
			return;
		mInBehaviourTransition = false;
		mLeftSpeed = 0;
		mRightSpeed = 0;
		changeSpeed();
//		Log.d(TAG, "stopBehaviourTransitions");
		mTimer.cancel();
	}

	private synchronized void startBehaviourTransitions(){
		if (mInBehaviourTransition)
			return;
		mInBehaviourTransition = true;
//		Log.d(TAG, "startBehaviourTransitions");
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new StateTransitionTask(), 8000, 5000);
	}

	/* 
	 * TODO: State transitions should be also affected by the observations, Eg. if it rains it is more to be in a good mood, thus more explorative
	 * State transitions
	 *      N    C    A    M
	 * N |0.25|0.70|0.03|0.02|
	 * C |0.05|0.55|0.25|0.15|
	 * A |0.02|0.10|0.60|0.28|
	 * M |0.01|0.05|0.10|0.84|
	 * 
	 * P(normal|normal)  = 0.25
	 * P(curious|normal) = 0.70
	 * P(annoyed|normal) = 0.03
	 * P(mad|normal)     = 0.02
	 * 
	 * P(normal|curious)  = 0.05 
	 * P(curious|curious) = 0.55
	 * P(annoyed|curious) = 0.25
	 * P(mad|curious)     = 0.15
	 * 
	 * P(normal|mad)  = 0.01
	 * P(curious|mad) = 0.10
	 * P(annoyed|mad) = 0.60
	 * P(mad|mad)     = 0.29
	 * 
	 * P(normal|mad)  = 0.01
	 * P(curious|mad) = 0.05
	 * P(annoyed|mad) = 0.10
	 * P(mad|mad)     = 0.84
	 */

	private float mTransitionMatrix[][] = {{0.25f, 0.70f, 0.03f, 0.02f},
										   {0.05f, 0.55f, 0.25f, 0.15f},
										   {0.02f, 0.10f, 0.60f, 0.28f},
										   {0.01f, 0.05f, 0.10f, 0.84f}};

	private Timer mTimer;

	private static PointF mFaceDistanceToCenter;

	private class StateTransitionTask extends TimerTask {


		@Override        
		public void run() {
			//Roulette type selection
			float randomNumber = mRrandomGenerator.nextFloat();
//			Log.d(TAG, "Random: " + randomNumber);
			float sum = 0;
			int nextState;
			for (int i=0 ; i<mTransitionMatrix[mCurrentState].length ; i++){
				nextState = i;
				sum = sum + mTransitionMatrix[mCurrentState][i];
				if (randomNumber <= sum){//Found the lucky winner!
					mCurrentState = nextState;
//					Log.d(TAG, "NextState: " + nextState);
					mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);
					mLeftSpeed = 0;
					mRightSpeed = 0;

					if (mCurrentState == STATE_CURIOUS){
						mLeftSpeed = SPEED_EXPLORE;
						mRightSpeed = -SPEED_EXPLORE;
					}
//					mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_SPEED);
					changeSpeed();
					return;
				}
			}
		}    
	};

	/* TODO: Integrate both type of transitions into one holistic approach that considers the state transitions and the observations simultaneously
	 * Behaviours and states due to observations:
	 * P("follow face and happy"|Face observed) = 1
	 * P("nothing and scared"|Raised from surface) = 1
	 * P("escape and surprise"|Object too close observed) = 1
	 * P("do nothing and normal"|No observation) = 1
	 */

	/*
	 * Follow face and happy:
	 */

	//Face observed:
	@Override
	public void onFaceDetected(FaceDetector.Face face) {
//		Log.d(TAG, "Desired dist: " + face.getDesiredEyesDist());
//		Log.d(TAG, "Center: [" + face.getDistanceToCenter().x + ", " + face.getDistanceToCenter().y + "]");

		// Angular speed (Face coordinates in range from (-1000, 1000), scaled):
		float angularAcc = face.getDistanceToCenter().x / 1000;

		//Spring damping system (scaled)
		float linearAcc = (face.eyesDistance() - face.getDesiredEyesDist()) / face.getDesiredEyesDist();

		int linearSpringConst = 7;//5
		int angularSpringConst = 5;//5

		int dampingNumerator = 1;
		int dampingDenominator = 1;
		
		mLeftSpeed = 0;
		mRightSpeed = 0;

		float angularSpeed = 0;
		int angularSpeedConst = 2;
		mNewPosTime = System.currentTimeMillis();
		if (mOldPosTime != 0){
			//change of position over change in time
			angularSpeed =  (face.getDistanceToCenter().x - mOldPosX)/(mNewPosTime - mOldPosTime);
			Log.d(TAG, "Sidespeed: " + (angularSpeed * angularSpeedConst));
			Log.d(TAG, "angular: " + (angularAcc * angularSpringConst));
		}
		
		mOldPosTime = mNewPosTime;
		mOldPosX = face.getDistanceToCenter().x;
		
		// closer face = negative back/forward speed
		// left right = negative X-axis speed
		
//		mLeftSpeed = (int)(
////				+   linearAcc * linearSpringConst
//				+	(angularAcc * angularSpringConst) + (angularSpeed * angularSpeedConst)
//				);
//		mRightSpeed = (int)( 
////				+   (linearAcc * linearSpringConst)
//				-	(angularAcc * angularSpringConst) - (angularSpeed * angularSpeedConst)
//				);
//		Log.d(TAG, "L: " + mLeftSpeed + ", R:" + mRightSpeed);
		
		mLeftSpeed = (int)(mLeftSpeed
				+   linearAcc * linearSpringConst
				+	angularAcc * angularSpringConst
				-	dampingNumerator * mLeftSpeed
				/ dampingDenominator);
		
		mRightSpeed = (int)(mRightSpeed 
				+   linearAcc * linearSpringConst
				-	angularAcc * angularSpringConst
				-	dampingNumerator * mRightSpeed
				/ dampingDenominator);

		//Stop behaviour transitions
		stopBehaviourTransitions();

		//If I wasn't already happy, become happy! (Seeing a person makes me happy):
		if (mCurrentState != STATE_HAPPY){
			mTalker.say("hello!");
			mCurrentState = STATE_HAPPY;
			mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);
		}

		//change the position of the face eyes according to the face position
		mFaceDistanceToCenter = face.getDistanceToCenter();
		mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_PUPILS_POSITION);

		changeSpeed();
	}

	@Override
	public void onFaceNotDetected() {
		if (mInBehaviourTransition)//Do nothing if we are in basic behaviour mode
			return;
		mOldPosTime = 0;
		mOldPosX = 0;
		
		mTalker.say("bye!");
		mRightSpeed = 0;
		mLeftSpeed = 0;
		changeSpeed();
		mCurrentState = STATE_NORMAL;
		mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);
		startBehaviourTransitions();
	}

	/*
	 * Escape and surprised:
	 */
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		float eventValue = event.values[0];
		switch (event.sensor.getType()){
		case Sensor.TYPE_PROXIMITY:
			if (mCurrentState != STATE_SURPRISE & eventValue == 0){//if not surprised already and hand approaches:
				mFaceTracking.removeFaceTrackingListener(); //FaceTracking sensor
				stopBehaviourTransitions(); //basic state transitions
//				Log.d(TAG, "Scared: Trying to escape!!!");
				mCurrentState = STATE_SURPRISE;
				mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);
				mLeftSpeed = SPEED_ESCAPE;
				mRightSpeed = SPEED_ESCAPE;
				changeSpeed();
			} else if (mCurrentState == STATE_SURPRISE & eventValue > 0){ //Go back to normal behaviour after being surprised
				//Nobody chasing, start again normal behaviour
//				Log.d(TAG, "Not scared anymore. Stop");
				mLeftSpeed = 0;
				mRightSpeed = 0;
				changeSpeed();
				resume();
			}
			break;
		}
	}
	
	/*
	 * Nothing and scared:
	 */
	@Override
	public void onWheelphoneUpdate() {
		int [] groundProx = mWheelphone.getGroundProxs();
		int min = Integer.MAX_VALUE; 
		for (int i=0 ; i<groundProx.length ; i++)
			if (groundProx[i] < min)
				min = groundProx[i];
		
		if (mCurrentState != STATE_SCARED & min<50){ //Robot is not scared and it is lifted...thus it is scared!
			Log.d(TAG, "Min: " + min + ". So I'm scared!");
			mFaceTracking.removeFaceTrackingListener(); //FaceTracking sensor
			stopBehaviourTransitions(); //basic state transitions
			
			mCurrentState = STATE_SCARED;
			mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_EXPRESSION);
		} else if(min>=50 & mCurrentState == STATE_SCARED) { //Back in the ground...act normal
			Log.d(TAG, "Min: " + min + ". So not scared anymore. Stop");
			resume();
		}
	}

	/*
	 * Helper functions
	 */
	
	/*
	 * Communicate from background threads to the UI thread. (required to update the UI)
	 */
	private static Handler mUIThreadWorker = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
			case MSG_UPDATE_EXPRESSION:
//				Log.d(TAG, "MSG_UPDATE_EXPRESSION");
				mFragmentPet.showText(getStatus());
				mFaceExpression.setExpression(mCurrentState);
				break;
			case MSG_UPDATE_TEXT:
//				Log.d(TAG, "MSG_UPDATE_TEXT");
				mFragmentPet.showText(getStatus());
				break;
			case MSG_UPDATE_PUPILS_POSITION:
//				Log.d(TAG, "MSG_UPDATE_TEXT");
				mFaceExpression.setPupilsPosition(mFaceDistanceToCenter.x, mFaceDistanceToCenter.y);
				break;
			}
		}
	};

	private static String getStatus(){
		String status = mWheelphone.isUSBConnected() ? "Connected" : "Disconnected";
		return status + ". L: " + mLeftSpeed + ", R: " + mRightSpeed + ". " + EXPRESSIONS_NAMES.get(mCurrentState);
	}
	
	private static void changeSpeed() {
		mUIThreadWorker.sendEmptyMessage(MSG_UPDATE_TEXT);
//		mWheelphone.setSpeed(mLeftSpeed, mRightSpeed);
		mWheelphone.setRawSpeed(mLeftSpeed, mRightSpeed);
	}

}
