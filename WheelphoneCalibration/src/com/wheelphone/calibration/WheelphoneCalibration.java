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
	private boolean getCalibrationData = false;
	private int calibrationDataIndex = 0;
	private final int CALIBRATION_SAMPLES = 10;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int firmwareVersion=0;
	
	// UI
	private TextView txtConnected;
	private Button btnStartCalib;
	private ProgressBar mProgress;
	private TextView txtFwScL[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtFwScR[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtFwL[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtFwR[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtBwL[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtBwR[][] = new TextView[2][CALIBRATION_SAMPLES];
	private TextView txtCalibResult;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int i=0;
		String textID;
		int resID;
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().getDecorView().setBackgroundColor(Color.BLACK);

		txtConnected = (TextView)findViewById(R.id.txtConnection);
		btnStartCalib = (Button)findViewById(R.id.btnCalibration);
		btnStartCalib.setText("Calibrate left wheel");
		mProgress = (ProgressBar) findViewById(R.id.progressBar1);
		mProgress.setIndeterminate(true);
		mProgress.setVisibility(ProgressBar.INVISIBLE);
		txtCalibResult = (TextView)findViewById(R.id.txtCalibrationResult);
        for(i=0; i<CALIBRATION_SAMPLES; i++) {
        	
            textID = "txtFwScLAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwScL[0][i] = ((TextView) findViewById(resID));                
            textID = "txtFwScLSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwScL[1][i] = ((TextView) findViewById(resID));  
            
            textID = "txtFwScRAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwScR[0][i] = ((TextView) findViewById(resID));                
            textID = "txtFwScRSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwScR[1][i] = ((TextView) findViewById(resID));            
            
            textID = "txtFwLAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwL[0][i] = ((TextView) findViewById(resID));                
            textID = "txtFwLSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwL[1][i] = ((TextView) findViewById(resID)); 
            
            textID = "txtFwRAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwR[0][i] = ((TextView) findViewById(resID));                
            textID = "txtFwRSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtFwR[1][i] = ((TextView) findViewById(resID));   
            
            textID = "txtBwLAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtBwL[0][i] = ((TextView) findViewById(resID));                
            textID = "txtBwLSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtBwL[1][i] = ((TextView) findViewById(resID));          
            
            textID = "txtBwRAdc" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtBwR[0][i] = ((TextView) findViewById(resID));                
            textID = "txtBwRSpeed" + i;
            resID = getResources().getIdentifier(textID, "id", "com.wheelphone.calibration");
            txtBwR[1][i] = ((TextView) findViewById(resID));  
            
        }		
		
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
				getCalibrationData = true;		
				calibrationDataIndex = 0;
			}
		} else if(calibStarted && !wheelphone.odometryCalibrationTerminated()) {			
			mProgress.setVisibility(ProgressBar.VISIBLE);
		}
		
		if(getCalibrationData) {
			
			if(wheelphone.getBatteryRaw()==0) {
				txtCalibResult.setText("Write OK ("+wheelphone.getBatteryRaw()+")");
			} else {
				txtCalibResult.setText("Write ERROR ("+wheelphone.getBatteryRaw()+")");				
			}
			
			txtFwScL[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getFrontProx(0)&0xFF)+(wheelphone.getFrontProx(1)*256)));
			txtFwScL[1][calibrationDataIndex].setText(String.valueOf((wheelphone.getFrontProx(2)&0xFF)+(wheelphone.getFrontProx(3)*256)));
			
			txtFwScR[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getFrontAmbient(0)&0xFF)+(wheelphone.getFrontAmbient(1)*256)));
			txtFwScR[1][calibrationDataIndex].setText(String.valueOf((wheelphone.getFrontAmbient(2)&0xFF)+(wheelphone.getFrontAmbient(3)*256)));

			txtFwL[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundProx(0)&0xFF)+(wheelphone.getGroundProx(1)*256)));
			txtFwL[1][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundProx(2)&0xFF)+(wheelphone.getGroundProx(3)*256)));
			
			txtFwR[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundProx(0)&0xFF)+(wheelphone.getGroundProx(1)*256)));
			txtFwR[1][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundAmbient(0)&0xFF)+(wheelphone.getGroundAmbient(1)*256)));			
			
			txtBwL[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundAmbient(2)&0xFF)+(wheelphone.getGroundAmbient(3)*256)));
			txtBwL[1][calibrationDataIndex].setText(String.valueOf(wheelphone.getLeftSpeed()));
			
			txtBwR[0][calibrationDataIndex].setText(String.valueOf((wheelphone.getGroundAmbient(2)&0xFF)+(wheelphone.getGroundAmbient(3)*256)));
			txtBwR[1][calibrationDataIndex].setText(String.valueOf(wheelphone.getRightSpeed()));			

			calibrationDataIndex++;
			if(calibrationDataIndex >= CALIBRATION_SAMPLES) {
				getCalibrationData = false;
			}
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
