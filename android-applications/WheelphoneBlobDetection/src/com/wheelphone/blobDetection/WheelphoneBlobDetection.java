/********************************************************************
 Software License Agreement:

 The software supplied herewith by Microchip Technology Incorporated
 (the "Company") for its PIC(R) Microcontroller is intended and
 supplied to you, the Company�s customer, for use solely and
 exclusively on Microchip PIC Microcontroller products. The
 software is owned by the Company and/or its supplier, and is
 protected under applicable copyright laws. All rights are reserved.
 Any use in violation of the foregoing restrictions may subject the
 user to criminal sanctions under applicable laws, as well as to
 civil liability for the breach of the terms and conditions of this
 license.

 THIS SOFTWARE IS PROVIDED IN AN �AS IS� CONDITION. NO WARRANTIES,
 WHETHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED
 TO, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE APPLY TO THIS SOFTWARE. THE COMPANY SHALL NOT,
 IN ANY CIRCUMSTANCES, BE LIABLE FOR SPECIAL, INCIDENTAL OR
 CONSEQUENTIAL DAMAGES, FOR ANY REASON WHATSOEVER.
********************************************************************/

package com.wheelphone.blobDetection;

import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;
import com.gctronic.android.blobDetection.R;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class WheelphoneBlobDetection extends Activity implements WheelPhoneRobotListener {
	
	// Various
	private String TAG = WheelphoneBlobDetection.class.getName();
	boolean getFirmwareFlag = true;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private byte flagController=1;
	private int firmwareVersion=0;
	
	// Blob detection behavior
	public int blobExecStep = 0;	// based on 50 ms of communication timer	
	public int forwardCounter = 0;
	private int Pblob = 1;
	private int baseSpeed = 5;
	private int speedFactor = 0;
	private int rotationFactor = 0;
	private int speedLimit = 20;
	public static final int NO_ROTATION = 0;
	public static final int LEFT_ROTATION = 1;
	public static final int RIGHT_ROTATION = 2;
	private int lastRotation = NO_ROTATION;
	
	// UI
	private ColorBlobDetectionView mView;	
	TabSpec spec1, spec2;
	
	private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				
				// the ColorBlobDetectionView can be created only when opencv is 
				// loaded so move here the layout settings
		        setContentView(R.layout.main);        		        
		        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
		        tabHost.setup();
		        spec1=tabHost.newTabSpec("Robot");
		        spec1.setContent(R.id.tab1);
		        spec1.setIndicator("Robot");
		        spec2=tabHost.newTabSpec("Blob");
		        spec2.setIndicator("Blob");
		        spec2.setContent(R.id.tab2);
		        tabHost.addTab(spec1);
		        tabHost.addTab(spec2);  				
				
				Log.i(TAG, "OpenCV loaded successfully");
				// Create and set View
				mView = (ColorBlobDetectionView)findViewById(R.id.blobdetect);
				//mView = new ColorBlobDetectionView(mAppContext);
				
				//setContentView(mView);
				//spec2.setContent(mView);
				// Check native OpenCV camera
				if( !mView.openCamera() ) {
					AlertDialog ad = new AlertDialog.Builder(mAppContext).create();
					ad.setCancelable(false); // This blocks the 'BACK' button
					ad.setMessage("Fatal error: can't open camera!");
					ad.setButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					});
					ad.show();
				}
				
				setMainView();
				
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};	
	
	public void setMainView() {
		mView.setMainView(this);
	}
	
	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    
		try {
	        PackageManager manager = this.getPackageManager();
	        PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
	        Log.d(TAG, "Info:" + info.packageName + "\n" + info.versionCode + "\n" + info.versionName); 
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
            
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
        	Log.e(TAG, "Cannot connect to OpenCV Manager");
        }     		
		     
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);	
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();
       	       
    }
	
	@Override
	public void onStart() {
		super.onStart();
		
		wheelphone.startUSBCommunication();
	    
		this.setTitle("Wheelphone blob following");
		
	}
	
    @Override
    public void onResume() {
    	super.onResume();
        
		if( (null != mView) && !mView.openCamera() ) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // This blocks the 'BACK' button
			ad.setMessage("Fatal error: can't open camera!");
			ad.setButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			    }
			});
			ad.show();
		}		
        
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
    	
		if (null != mView)
			mView.releaseCamera();
    	
    }
    
    private void updateSensor(int id, int value) {
		TextView textviewToUpdate;	
		textviewToUpdate = (TextView)findViewById(id);
		textviewToUpdate.setText(String.valueOf(value));
    }    

    public void followBlob(Point[] centers, float[] radius, int imgWidth, int imgHeight) {
    	  	
    	if(centers[0].x==0 && centers[0].y==0) {	
    		
    		lSpeed = 0;
    		rSpeed = 0;
    		
    	} else {
    			
    		speedFactor = (int)radius[0] + baseSpeed;
    		if(speedFactor > speedLimit) {
    			speedFactor = speedLimit;
    		} else if(speedFactor < -speedLimit) {
    			speedFactor = -speedLimit;
    		}

    		rotationFactor = ((imgWidth/8)-(int)centers[0].x); 
    		if(rotationFactor > speedLimit) {
    			rotationFactor = speedLimit;
    		} else if(rotationFactor < -speedLimit) {
    			rotationFactor = -speedLimit;
    		}

    		int result = speedFactor - rotationFactor;
    		if(result > speedLimit) {
    			result = speedLimit;
    		} else if(result < -speedLimit) {
    			result = -speedLimit;
    		}
    		lSpeed = result;

    		result = speedFactor + rotationFactor;
    		if(result > speedLimit) {
    			result = speedLimit;
    		} else if(result < -speedLimit) {
    			result = -speedLimit;
    		}
    		rSpeed = result;

        		
    	}
    	
    
    }
    
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneBlobDetection.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		updateSensor(R.id.prox0, wheelphone.getFrontProx(0));
		updateSensor(R.id.prox1, wheelphone.getFrontProx(1));
		updateSensor(R.id.prox2, wheelphone.getFrontProx(2));
		updateSensor(R.id.prox3, wheelphone.getFrontProx(3));
		updateSensor(R.id.ground0, wheelphone.getGroundProx(0));
		updateSensor(R.id.ground1, wheelphone.getGroundProx(1));
		updateSensor(R.id.ground2, wheelphone.getGroundProx(2));
		updateSensor(R.id.ground3, wheelphone.getGroundProx(3));
		updateSensor(R.id.batteryLevel, wheelphone.getBatteryCharge());
		updateSensor(R.id.rightSpeedTxt, rSpeed);
		updateSensor(R.id.leftSpeedTxt, lSpeed);
		
		wheelphone.setRawLeftSpeed(lSpeed);
		wheelphone.setRawRightSpeed(rSpeed);
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
	
} //Class definition BasicAccessoryDemo
