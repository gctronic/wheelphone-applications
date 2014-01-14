
package com.wheelphone.odomMotion;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class WheelphoneOdomMotion extends Activity implements WheelPhoneRobotListener {

	// various
	private String TAG = WheelphoneOdomMotion.class.getName();
	boolean getFirmwareFlag = true;
	private int mCurrentRotation = 0;		// target angle in degrees
	private int mCurrentMovement = 0;		// target position in cm (forward)
	private boolean mStartRotation = false;
	private boolean mStartMovement = false;
	private int mTempRotation = 0;
	private int mTempMovement = 0;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int mDesSpeed=0;
	private int lSpeed=0, rSpeed=0;
	private int firmwareVersion=0;
	private static final int MAX_SPEED = 350;
	private double mOdomX=0, mOdomY=0, mOdomTheta=0;

	// UI
	private CheckBox chkSpeedControl, chkSoftAcc;
	EditText desSpeed, desRotation, desMovement; 
	private TextView leftSpeed, rightSpeed, txtStatus, txtConnected, txtOdomX, txtOdomY, txtOdomTheta, batteryValue;
	
	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            
        setContentView(R.layout.main);
       	       
        chkSpeedControl = (CheckBox)findViewById(R.id.chkSpeedControl);
    	chkSoftAcc = (CheckBox)findViewById(R.id.chkSoftAcc);
    	leftSpeed = (TextView)findViewById(R.id.leftSpeedTxt);
        rightSpeed = (TextView)findViewById(R.id.rightSpeedTxt);
        txtStatus = (TextView)findViewById(R.id.txtGeneralStatus);
        txtOdomX = (TextView)findViewById(R.id.odomXTxt);
        txtOdomY = (TextView)findViewById(R.id.odomYTxt);
        txtOdomTheta = (TextView)findViewById(R.id.odomThetaTxt);
        txtConnected = (TextView)findViewById(R.id.txtConnection);
        batteryValue = (TextView)findViewById(R.id.batteryTxt);
        
    	desSpeed = (EditText) findViewById(R.id.txtDesSpeed);
    	desSpeed.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mDesSpeed = Integer.parseInt(v.getText().toString());
                    if(mDesSpeed < -MAX_SPEED) {
                    	mDesSpeed = -MAX_SPEED;
                    	v.setText(Integer.toString(-MAX_SPEED));
                    } else if(mDesSpeed > MAX_SPEED) {
                    	mDesSpeed = MAX_SPEED;
                    	v.setText(Integer.toString(MAX_SPEED));
                    }                    
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });       
        
    	desRotation = (EditText) findViewById(R.id.txtDesRot);
    	desRotation.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mCurrentRotation = Integer.parseInt(v.getText().toString());                   
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });
    	
    	desMovement = (EditText) findViewById(R.id.txtDesMov);
    	desMovement.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mCurrentMovement = Integer.parseInt(v.getText().toString());                   
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });    	
        
		//Make sure that the app stays on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);	
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        chkSpeedControl.setChecked(true);
     
        wheelphone.resetOdometry();
        
        this.setTitle("WP Odometry Motion");
    }
	
	@Override
	public void onStart() {
		super.onStart();   		
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	wheelphone.startUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(this);      		
    }  
    
    @Override
    public void onStop() {
    	Log.d("stop", "stop");    	
    	super.onStop();
    	//android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    @Override
    public void onPause() {
    	Log.d("pause", "pause"); 
    	super.onPause();
    	//wheelphone.pauseUSBCommunication();
    	//wheelphone.setWheelPhoneRobotListener(null);
    }   
    
    public void startRotation(View view) {
    	mStartRotation = true;
    	mTempRotation = 0;
    	wheelphone.resetOdometry();
    }
    
    public void startMovement(View view) {
    	mStartMovement = true;
    	mTempMovement = 0;
    	wheelphone.resetOdometry();
    }
    
    public void stopRobot(View view) {
    	mStartRotation = false;
    	mStartMovement = false;
    	lSpeed = 0;
    	rSpeed = 0;
    }
    
    public void controllerOnOff(View view) {
    	if(chkSpeedControl.isChecked()) {
    		wheelphone.enableSpeedControl();
    	} else {
    		wheelphone.disableSpeedControl();
    	}
    }    
    
    public void softAccOnOff(View view) {
    	if(chkSoftAcc.isChecked()) {
    		wheelphone.enableSoftAcceleration();
    	} else {
    		wheelphone.disableSoftAcceleration();
    	}
    } 
    
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneOdomMotion.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		// here you can insert behavior processed about every 50 ms (20 Hz)
		
		if(wheelphone.isRobotConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));

	    	batteryValue.setText(String.valueOf(wheelphone.getBatteryCharge())+"%");
	    	
			mOdomX = wheelphone.getOdometryX();
			mOdomY = wheelphone.getOdometryY();
			mOdomTheta = wheelphone.getOdometryTheta();
		
			txtOdomX.setText(String.format("%.2f", mOdomX));
			txtOdomY.setText(String.format("%.2f",mOdomY));
			txtOdomTheta.setText(String.format("%.4f",mOdomTheta*180/Math.PI));


	    	if(mStartRotation) {
	    		txtStatus.setText("Rotating");
	    		if(mDesSpeed > 0) {
	    			lSpeed = 0;
	    			rSpeed = mDesSpeed;
	    		} else {
	    			lSpeed = mDesSpeed;
	    			rSpeed = 0;
	    		}
		    	if((mOdomTheta*180.0/Math.PI) >= mCurrentRotation) {
		    		lSpeed = 0;
		    		rSpeed = 0;
		    		mStartRotation = false;
		    		txtStatus.setText("Waiting");
		    	}
	    	}
	    	
	    	if(mStartMovement) {
	    		txtStatus.setText("Moving");
	    		lSpeed = mDesSpeed;
	    		rSpeed = mDesSpeed;
		    	if((mOdomX/10) >= mCurrentMovement) {
		    		lSpeed = 0;
		    		rSpeed = 0;
		    		mStartMovement = false;
		    		txtStatus.setText("Waiting");
		    	}	    		
	    	}
		    
			leftSpeed.setText(String.valueOf(lSpeed));
			rightSpeed.setText(String.valueOf(rSpeed));
			
			wheelphone.setLeftSpeed(lSpeed);
			wheelphone.setRightSpeed(rSpeed);
			
		
		} else {			
	    	txtConnected.setText("Disconnected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.red));
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
