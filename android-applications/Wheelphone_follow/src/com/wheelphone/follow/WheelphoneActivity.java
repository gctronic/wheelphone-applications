package com.wheelphone.follow;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

public class WheelphoneActivity extends Activity implements WheelPhoneRobotListener {
	
	// Various
	private static final String TAG = WheelphoneActivity.class.getName();
	boolean getFirmwareFlag = true;
	
	// Robot state
	private WheelphoneRobot mWheelphone;
	private double lSpeed=0, rSpeed=0;
	
	// UI
	private TextProgressBar barProx0, barProx1, barProx2, barProx3, barGround0, barGround1, barGround2, barGround3;   
	private TextView batteryState, batteryValue, outputText;
	private TextView mReadLeftSpeed, mReadRightSpeed;
	private TextView txtConnected;
		
	//Follow
	private final static double LINEAR_SPRING_CONST = 80;
	private final static double ANGULAR_SPRING_CONST = 30;
	private final static double DAMPLING_FACTOR = 0.9;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		setContentView(R.layout.main);

        barProx0 = (TextProgressBar) findViewById(R.id.barProx0);
        barProx1 = (TextProgressBar) findViewById(R.id.barProx1);
        barProx2 = (TextProgressBar) findViewById(R.id.barProx2);
        barProx3 = (TextProgressBar) findViewById(R.id.barProx3);
        barGround0 = (TextProgressBar) findViewById(R.id.barGround0);
        barGround1 = (TextProgressBar) findViewById(R.id.barGround1);
        barGround2 = (TextProgressBar) findViewById(R.id.barGround2);
        barGround3 = (TextProgressBar) findViewById(R.id.barGround3);
        batteryState = (TextView)findViewById(R.id.txtRobotInCharge);
        batteryValue = (TextView)findViewById(R.id.batteryLevel);
        mReadLeftSpeed = (TextView)findViewById(R.id.leftEncTxt);
        mReadRightSpeed = (TextView)findViewById(R.id.rightEncTxt);
    	txtConnected = (TextView)findViewById(R.id.txtConnection);
    	outputText = (TextView)findViewById(R.id.outputText);
    			
        mWheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        mWheelphone.startUSBCommunication();
        mWheelphone.enableSpeedControl();
        mWheelphone.disableSoftAcceleration();
        mWheelphone.setWheelPhoneRobotListener(this);
        
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	mWheelphone.resumeUSBCommunication();
    }	
    
    @Override
    public void onPause() {
    	super.onPause();
    	mWheelphone.pauseUSBCommunication();
    }  

    public void calibrateSensors(View view) {
    	mWheelphone.calibrateSensors();
    }
    
	@Override
	public void onWheelphoneUpdate() {
		
		//UI update:
		barProx0.setProgress((int)mWheelphone.getFrontProx(0));
		barProx0.setText(String.valueOf(mWheelphone.getFrontProx(0)));
		barProx1.setProgress(mWheelphone.getFrontProx(1));
		barProx1.setText(String.valueOf(mWheelphone.getFrontProx(1)));
		barProx2.setProgress(mWheelphone.getFrontProx(2));
		barProx2.setText(String.valueOf(mWheelphone.getFrontProx(2)));
		barProx3.setProgress(mWheelphone.getFrontProx(3));
		barProx3.setText(String.valueOf(mWheelphone.getFrontProx(3)));
		barGround0.setProgress(mWheelphone.getGroundProx(0));
		barGround0.setText(String.valueOf(mWheelphone.getGroundProx(0)));
		barGround1.setProgress(mWheelphone.getGroundProx(1));
		barGround1.setText(String.valueOf(mWheelphone.getGroundProx(1)));
		barGround2.setProgress(mWheelphone.getGroundProx(2));
		barGround2.setText(String.valueOf(mWheelphone.getGroundProx(2)));
		barGround3.setProgress(mWheelphone.getGroundProx(3));
		barGround3.setText(String.valueOf(mWheelphone.getGroundProx(3)));
		if(mWheelphone.isCharging()) {
			batteryState.setText(getResources().getString(R.string.robotInCharge));
		} else if(mWheelphone.isCharged()) {
			batteryState.setText("Robot is charged");
		} else {
			batteryState.setText("");
		}
		batteryValue.setText(String.valueOf(mWheelphone.getBatteryRaw()));
		if(mWheelphone.getBatteryRaw() <= 30) {
			batteryValue.setTextColor(getResources().getColor(R.color.red));
		} else {
			batteryValue.setTextColor(getResources().getColor(R.color.green));
		}	
		
		mReadLeftSpeed.setText(String.valueOf(mWheelphone.getLeftSpeed()));
		mReadRightSpeed.setText(String.valueOf(mWheelphone.getRightSpeed()));				
		if(mWheelphone.isUSBConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}
		
		//The actual follow close by object code:
		
		//Spring damping system
		int [] frontProxArray = mWheelphone.getFrontProxs();
		
		int max = frontProxArray[0];
		int maxIdx = 0;
		
		for (int i = 1; i < frontProxArray.length; i++) {
		    if (frontProxArray[i] > max) {
		        max = frontProxArray[i];
		        maxIdx = i;
		    }
		}
		//Check that the max is above 0 (there is actually an object in front that should be followed 
		int followSwitch = Math.max(0, Math.min(1, max));

		//Produce the linear desired acceleration. Range: [-1, 1]
		double linearAcc = (1 - 2*frontProxArray[maxIdx]/255.);

		//Produce the angular acceleration value. Range: [-1, 1]:
		double angularAcc = ((maxIdx <=1) ? maxIdx - 2 : maxIdx - 1 ) / 2.;
		
		lSpeed = followSwitch * (
				lSpeed 
				+   linearAcc * LINEAR_SPRING_CONST
				+	angularAcc * ANGULAR_SPRING_CONST
				-	DAMPLING_FACTOR * lSpeed);

		rSpeed = followSwitch * (
				rSpeed 
				+   linearAcc * LINEAR_SPRING_CONST
				-	angularAcc * ANGULAR_SPRING_CONST
				-	DAMPLING_FACTOR * rSpeed);
		
		outputText.setText("Left: " + lSpeed + "\n" +
				"Right: " + rSpeed + "\n" +
				"linearAcc: " + linearAcc + "\n" +
				"angularAcc: " + angularAcc + "\n" +
				"followSwitch:" + followSwitch
				);
    	mWheelphone.setSpeed((int)lSpeed, (int)rSpeed);
	}
    
}
