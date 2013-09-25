package com.wheelphone.calibration;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.os.Bundle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WheelphoneCalibration extends Activity implements WheelPhoneRobotListener {
	
	// Various
	private String TAG = WheelphoneCalibration.class.getName();
	boolean getFirmwareFlag = true;
	private int calibState = 0;
	private boolean calibStarted = false;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int firmwareVersion=0;
	
	// UI
	private TextView txtConnected;
	private Button btnStartCalib;
	private ProgressBar mProgress;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().getDecorView().setBackgroundColor(Color.BLACK);

		txtConnected = (TextView)findViewById(R.id.txtConnection);
		btnStartCalib = (Button)findViewById(R.id.btnCalibration);
		btnStartCalib.setText("Calibrate left wheel");
		mProgress = (ProgressBar) findViewById(R.id.progressBar1);
		mProgress.setIndeterminate(true);
		mProgress.setVisibility(ProgressBar.INVISIBLE);
		
		//Make sure that the app stays open:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);		
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.setUSBCommunicationTimeout(5000);
                
        msgbox("calibration", "Place the robot with the right wheel next to the black line and press the calibrate button. Wait until the process is terminated.");
		
	}

	@Override
	public void onStart() {
		super.onStart();
		wheelphone.startUSBCommunication();
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	wheelphone.resumeUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(this);
    }	
    
    @Override
    public void onStop() {
    	super.onStop();
    	android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	wheelphone.pauseUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(null);
    }  
    
    public void startCalibration(View view) {
    	wheelphone.calibrateOdometry();
    	btnStartCalib.setEnabled(false);
    	calibStarted = true;	
    }
    
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneCalibration.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
				
		if(wheelphone.isUSBConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}
		
		if(calibStarted && wheelphone.odometryCalibrationTerminated()) {			
			mProgress.setVisibility(ProgressBar.INVISIBLE);
			calibStarted = false;
			if(calibState == 0) {
				calibState = 1;
				btnStartCalib.setEnabled(true);
				btnStartCalib.setText("Calibrate right wheel");
				msgbox("calibration", "Left wheel calibrated, now place the robot with the left wheel next to the black line and press the calibrate button. Wait until the process is terminated.");
			} else if(calibState == 1) {
				btnStartCalib.setEnabled(true);
				btnStartCalib.setText("Calibrate left wheel");
				msgbox("calibratoin", "Calibration terminated!");
				calibState = 0;
			}
		} else if(calibStarted && !wheelphone.odometryCalibrationTerminated()) {			
			mProgress.setVisibility(ProgressBar.VISIBLE);
		}
		
	}
    
    public void msgbox(String title,String msg) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);                      
        dlgAlert.setTitle(title); 
        dlgAlert.setMessage(msg); 
        dlgAlert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                 //finish(); 
            }
       });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }
    
}
