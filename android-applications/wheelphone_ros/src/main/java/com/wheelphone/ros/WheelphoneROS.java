/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.wheelphone.ros;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class WheelphoneROS extends RosActivity implements WheelPhoneRobotListener {

	// Various
	private static String TAG = WheelphoneROS.class.getName();;
	boolean getFirmwareFlag = true;
	private PowerManager.WakeLock wl;
	private boolean usbStarted=false;
	
	// ROS
	private RosCameraPreviewView rosCameraPreviewView;
	private int cameraId;
	private Camera backCam;
	private GroundPublisher groundPub=null;
	private ProximityPublisher proximityPub=null;
	private BatteryPublisher batteryPub=null;
	private PhoneListeners phoneLis=null;
	private OdometryPublisher odometryPub=null;
	private TransformBroadcaster odom_broadcaster=null;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int firmwareVersion=0;
	private int lSpeed=0, rSpeed=0;
	private byte groundValues[] = new byte[4];
	private byte proxValues[] = new byte[4];
	private byte batteryValue;
	private double xPos = 0.0;	// mm
	private double yPos = 0.0;	// mm
	private double theta = 0.0;	// radians
	
	// UI
	TextView txtConnected;
  
	public WheelphoneROS() {
		// The RosActivity constructor configures the notification title and ticker
		// messages.
		super("Wheelphone ROS", "Wheelphone ROS");
	}
  
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);   

		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);		
		
		rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
		//rosCameraPreviewView = new RosCameraPreviewView(this); //(RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
		//rosCameraPreviewView.setMinimumHeight(320);
		//rosCameraPreviewView.setMinimumWidth(480);

		// avoid screen suspending
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakelock");	
   	
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();     
        wheelphone.setCommunicationTimeout(5000);

    	txtConnected = (TextView)findViewById(R.id.txtConnection);
    	
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

	    //NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
	    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress().toString());
	    // At this point, the user has already been prompted to either enter the URI
	    // of a master to use or to start a master locally.
	    nodeConfiguration.setMasterUri(getMasterUri());
	    //nodeConfiguration.setMasterUri(URI.create("http://192.168.2.9:11311"));
    
	    groundPub = new GroundPublisher();
	    nodeMainExecutor.execute(groundPub, nodeConfiguration);
	    
	    proximityPub = new ProximityPublisher();
	    nodeMainExecutor.execute(proximityPub, nodeConfiguration);     
	   
	    batteryPub = new BatteryPublisher();
	    nodeMainExecutor.execute(batteryPub, nodeConfiguration);      
	    
	    phoneLis = new PhoneListeners();
	    phoneLis.setMainActivity(this);
	    nodeMainExecutor.execute(phoneLis, nodeConfiguration);
	   
	    cameraId=0;
	    backCam = Camera.open(cameraId);
	    backCam.setDisplayOrientation(90);
	    
	    /*
	    Camera.Parameters parameters = backCam.getParameters();
	    List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
	    for (Size size : supportedPreviewSizes) {
	    	Log.d("width=", Integer.toString(size.width));
	    	Log.d("height=", Integer.toString(size.height));
	 
	    }
	    List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
	    for (int[] range : supportedFpsRanges) {
	    	Log.d("min=", Integer.toString(range[0]));
	    	Log.d("max=", Integer.toString(range[1]));
	 
	    }
	    parameters.setJpegQuality(50);
	    parameters.setPreviewFpsRange(1000, 7000);
	    backCam.setParameters(parameters);
	    */
	    rosCameraPreviewView.setCamera(backCam);
	    nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);    
	   
	    odometryPub = new OdometryPublisher();
	    nodeMainExecutor.execute(odometryPub, nodeConfiguration);
	    
	    odom_broadcaster = new TransformBroadcaster();
	    odometryPub.setTransformBroadcaster(odom_broadcaster);
	    nodeMainExecutor.execute(odom_broadcaster, nodeConfiguration);
    
	}


	@Override
	public void onStart() {
		super.onStart();
		
    	// Lock screen
    	wl.acquire();

		this.setTitle("Wheelphone ROS");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(!usbStarted) {
			usbStarted = true;
			wheelphone.startUSBCommunication();
		}
		wheelphone.setWheelPhoneRobotListener(this);
	}
  
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
 		//Call the super function that we are over writing now that we have saved our data.
		super.onSaveInstanceState(savedInstanceState);
	}
   
	@Override
	public void onStop() {
		super.onStop();
		//android.os.Process.killProcess(android.os.Process.myPid());
		wl.release();
	}
  
	@Override
	public void onPause() {
		wheelphone.setWheelPhoneRobotListener(null);
		super.onPause();
	}
  
	public void updateFlagStatus(byte f) {
		if((f&0x01)==0x01) {
			wheelphone.enableSpeedControl();
		} else {
			wheelphone.disableSpeedControl();
		}
		if((f&0x02)==0x02) {
			wheelphone.enableSoftAcceleration();
		} else {
			wheelphone.disableSoftAcceleration();
		}
		if((f&0x04)==0x04) {
			wheelphone.enableObstacleAvoidance();
		} else {
			wheelphone.disableObstacleAvoidance();
		}
		if((f&0x08)==0x08) {
			wheelphone.enableCliffAvoidance();
		} else {
			wheelphone.disableCliffAvoidance();
		}
		if((f&0x10)==0x10) {	
			odometryPub.resetValues();
			wheelphone.calibrateSensors();
		}
	}

	public void updateMotorSpeed(short[] vel) {
		lSpeed = vel[0];
		rSpeed = vel[1];
		wheelphone.setLeftSpeed(lSpeed);
		wheelphone.setRightSpeed(rSpeed);
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

	public void onWheelphoneUpdate() {
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneROS.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		groundValues[0] = (byte)wheelphone.getGroundProx(0);
		groundValues[1] = (byte)wheelphone.getGroundProx(1);
		groundValues[2] = (byte)wheelphone.getGroundProx(2);
		groundValues[3] = (byte)wheelphone.getGroundProx(3);
		if(groundPub != null) {
			groundPub.updateData(groundValues);
		}
		
		proxValues[0] = (byte)wheelphone.getFrontProx(0);
		proxValues[1] = (byte)wheelphone.getFrontProx(1);
		proxValues[2] = (byte)wheelphone.getFrontProx(2);
		proxValues[3] = (byte)wheelphone.getFrontProx(3);
		if(proximityPub != null) {
			proximityPub.updateData(proxValues);
		}
									
		if(odometryPub != null) {
			xPos = wheelphone.getOdometryX();
			yPos = wheelphone.getOdometryY();
			theta = wheelphone.getOdometryTheta();
			odometryPub.updateData(xPos, yPos, theta);
		}
		
		batteryValue = (byte)wheelphone.getBatteryCharge();
		if(batteryPub != null) {
			batteryPub.updateData(batteryValue);
		}

		if(!wheelphone.isRobotConnected()) {
	    	txtConnected.setText("Robot disconnected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.red));
		} else {
	    	txtConnected.setText("Robot connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		}			
	}    
    
}
