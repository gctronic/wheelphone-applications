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

package com.wheelphone.blobDocking;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import com.gctronic.android.blobDocking.R;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;

public class WheelphoneDocking extends Activity  implements TextToSpeech.OnInitListener, WheelPhoneRobotListener {

	// Various
	private String TAG = WheelphoneDocking.class.getName();
	boolean getFirmwareFlag = true;
	private TextToSpeech tts;
	private int instableChargeCounter=0;
	
	// HTTP server
    private CustomHttpServer httpServer = null;	
    private Context context;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private int firmwareVersion=0;
	
	// Blob detection and following
	private ColorBlobDetectionView mView;
	Point blobCenter = new Point();
    float blobRadius = 0;
	public static final int NO_INFO = 0;    
	public static final int NO_BLOB_FOUND = 1;
	public static final int BLOB_FOUND = 2;	
    public short newBlobInfo = NO_INFO;
	public int currentImgHeight=0, currentImgWidth=0;
	private int baseSpeed = 5;
	private int speedFactor = 0;
	private int rotationFactor = 0;
	private int speedLimit = 20;
	public static final int STATE_CHOOSE_BLOB = 0;
	public static final int STATE_WAITING_START = 1;
	public static final int STATE_DOCKING_SEARCH = 2;
	public static final int STATE_DOCKING_REACH = 3;	
	public static final int STATE_LINE_FOLLOW = 4;
	public static final int STATE_ROBOT_CHARGING = 5;
	public static final int STATE_ROBOT_GO_BACK = 6;
	public short globalState=STATE_DOCKING_SEARCH;
	public short prevGlobalState=STATE_DOCKING_SEARCH;
	public static final int ROBOT_NOT_CHARGING = 0;
	public static final int ROBOT_IN_CHARGE = 1;
	public static final int ROBOT_CHARGED = 2;
	public int chargeStatus = ROBOT_NOT_CHARGING;
	public int chargeCounter = 0;
	 	
	// line following
	public static final int OUT_OF_LINE_THR = 50;
	public static final int INIT_GROUND_THR = 130;
	public int groundThreshold = INIT_GROUND_THR;
	public int lineFound = 0;
	public int outOfLine = 0;	
	public int minSpeedLineFollow = 7;
	public int lineFollowTimeout = 0;
	
	// UI
	TabSpec spec1, spec2;
	TextView txtProx0, txtProx1, txtProx2, txtProx3, txtGround0, txtGround1, txtGround2, txtGround3;   
	private TextView batteryValue;
	private TextView leftSpeed, rightSpeed;
	
    public void onInit(int status) {
 
        if (status == TextToSpeech.SUCCESS) {
 
            int result = tts.setLanguage(Locale.US);
 
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
 
        } else {
        	tts.setSpeechRate((float)0.7);
            Log.e("TTS", "Initilization Failed!");
        }
 
    }	
	
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
				
		        EditText editText = (EditText) findViewById(R.id.txtGroundThr);
		        editText.setOnEditorActionListener(new OnEditorActionListener() {
		        	
		            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		                boolean handled = false;
		                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
		                    groundThreshold = Integer.parseInt(v.getText().toString());
		                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
		                    handled = true;
		                }
		                return handled;
		            }          
		            
		        });  		        
		        
		        displayIpAddress();
		        
				Log.i(TAG, "OpenCV loaded successfully");
				// Create and set View
				mView = (ColorBlobDetectionView)findViewById(R.id.blobdetect);

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
				
		        txtProx0 = (TextView)findViewById(R.id.prox0);
		        txtProx1 = (TextView)findViewById(R.id.prox1);
		        txtProx2 = (TextView)findViewById(R.id.prox2);
		        txtProx3 = (TextView)findViewById(R.id.prox3);
		        txtGround0 = (TextView)findViewById(R.id.ground0);
		        txtGround1 = (TextView)findViewById(R.id.ground1);
		        txtGround2 = (TextView)findViewById(R.id.ground2);
		        txtGround3 = (TextView)findViewById(R.id.ground3);
		        leftSpeed = (TextView)findViewById(R.id.leftSpeedTxt);
		        rightSpeed = (TextView)findViewById(R.id.rightSpeedTxt);
		        batteryValue = (TextView)findViewById(R.id.batteryLevel);
 
				
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
		
        context = this.getApplicationContext();		
        httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);       
		
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
        wheelphone.enableSoftAcceleration();
        
       	tts = new TextToSpeech(this, this);
       	
    }
	
	@Override
	public void onStart() {
		super.onStart();
		this.setTitle("Wheelphone blob docking");
		wheelphone.startUSBCommunication();
	}
	
    private void startServers() {
    	if (httpServer != null) {
    		CustomHttpServer.setScreenState(true);
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			//log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }	
	
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	TextView line1 = (TextView)findViewById(R.id.txtHttpStatus);
		if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	String ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	line1.setText("HTTP://");
	    	line1.append(ip);
	    	line1.append(":8080");
    	} else {
    		line1.setText("HTTP://xxx.xxx.xxx.xxx:8080");
    	}
    }    
    
    @Override
    public void onResume() {
    	super.onResume();	

    	startServers();

    	wheelphone.resumeUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(this);
    	
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
        
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	if (httpServer != null) httpServer.stop();
    	android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	CustomHttpServer.setScreenState(false);    	
    	wheelphone.pauseUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(null);
		if (null != mView) {
			mView.releaseCamera();
		}
    }

    /** 
     * Handler for receiving messages from the USB Manager thread or
     *   the LED control modules
     */
    private Handler handler = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		
			switch(msg.what)
			{	
    			case CustomHttpServer.MOVE_FORWARD:   			
    			case CustomHttpServer.MOVE_BACKWARD:  			    			
    			case CustomHttpServer.MOVE_LEFT:
    			case CustomHttpServer.MOVE_RIGHT:
    			case CustomHttpServer.ENABLE_OBSTACLE_AVOIDANCE:
    			case CustomHttpServer.DISABLE_OBSTACLE_AVOIDANCE:
    			case CustomHttpServer.ENABLE_CLIFF_AVOIDANCE:
    			case CustomHttpServer.DISABLE_CLIFF_AVOIDANCE:
    				globalState = STATE_DOCKING_SEARCH;
    				break;  			
    			case CustomHttpServer.STOP:
    				globalState = STATE_WAITING_START;
    				break;

			}	//switch msg.what
    	} //handleMessage
    }; //handler


    public void followBlob(Point[] centers, float[] radius, int imgWidth, int imgHeight) {
    	
    	if(centers[0].x==0 && centers[0].y==0) {	
    		newBlobInfo = NO_BLOB_FOUND;
    	} else { 
	    	blobCenter = new Point();
	    	blobCenter = centers[0];
	    	blobRadius = radius[0];
	    	currentImgHeight = imgHeight;
	    	currentImgWidth = imgWidth;
	    	newBlobInfo = BLOB_FOUND;
    	}
    
    }

    public void calibrateSensors(View view) {
    	wheelphone.calibrateSensors();
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
						Toast.makeText(WheelphoneDocking.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		txtProx0.setText(String.valueOf(wheelphone.getFrontProx(0)));
		txtProx1.setText(String.valueOf(wheelphone.getFrontProx(1)));
		txtProx2.setText(String.valueOf(wheelphone.getFrontProx(2)));
		txtProx3.setText(String.valueOf(wheelphone.getFrontProx(3)));
		txtGround0.setText(String.valueOf(wheelphone.getGroundProx(0)));
		txtGround1.setText(String.valueOf(wheelphone.getGroundProx(1)));
		txtGround2.setText(String.valueOf(wheelphone.getGroundProx(2)));
		txtGround3.setText(String.valueOf(wheelphone.getGroundProx(3)));					
		batteryValue.setText(String.valueOf(wheelphone.getBatteryRaw()));
		if(wheelphone.getBatteryRaw() <= 30) {
			batteryValue.setTextColor(getResources().getColor(R.color.red));
		} else {
			batteryValue.setTextColor(getResources().getColor(R.color.green));
		}					
		if(wheelphone.isCharging()) {
			if(wheelphone.isCharged()) {
				chargeStatus = ROBOT_CHARGED;
			} else {
				chargeStatus = ROBOT_IN_CHARGE;
			}
		} else {
			chargeStatus = ROBOT_NOT_CHARGING;
		}
		
		if(wheelphone.isCalibrating()) {
			return;
		}
		
		if(globalState == STATE_CHOOSE_BLOB || globalState == STATE_WAITING_START) {
			
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Waiting start...");
		    txtConnected.setTextColor(getResources().getColor(R.color.white));
			
		    rSpeed = 0;
			lSpeed = 0;
			wheelphone.setRawLeftSpeed(lSpeed);
			wheelphone.setRawRightSpeed(rSpeed);
		
		} else if(globalState == STATE_DOCKING_SEARCH) {
		    
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is looking for docking station");
		    txtConnected.setTextColor(getResources().getColor(R.color.red));
		    
		    wheelphone.enableObstacleAvoidance();
			lSpeed = 20;			// move around
			rSpeed = 20;

			if(newBlobInfo == BLOB_FOUND) {
				globalState = STATE_DOCKING_REACH;
				prevGlobalState = STATE_DOCKING_SEARCH;
			}				

		} else if(globalState == STATE_DOCKING_REACH) {
				
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot found docking station");
		    txtConnected.setTextColor(getResources().getColor(R.color.green));
											
			if(newBlobInfo == NO_BLOB_FOUND) {
				globalState = STATE_DOCKING_SEARCH;
				prevGlobalState = STATE_DOCKING_REACH;
			} else if(newBlobInfo == BLOB_FOUND) {
				// check if a black line is detected
				if(wheelphone.getGroundProx(0)<groundThreshold || wheelphone.getGroundProx(1)<groundThreshold || wheelphone.getGroundProx(2)<groundThreshold || wheelphone.getGroundProx(3)<groundThreshold) {
					lineFound++;
					if(lineFound > 3) {	// be sure to find a line to follow (avoid noise)
						globalState = STATE_LINE_FOLLOW;
						prevGlobalState = STATE_DOCKING_REACH;
						lineFollowTimeout = 0;
					}
				} else {
					lineFound = 0;
				}								
					
		        speedFactor = (int)blobRadius + baseSpeed;
		        if(speedFactor > speedLimit) {
		        	speedFactor = speedLimit;
		        } else if(speedFactor < -speedLimit) {
		        	speedFactor = -speedLimit;
		        }
		        		 
		        rotationFactor = ((currentImgWidth/8)-(int)blobCenter.x)/4; 
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
				
		} else if(globalState == STATE_LINE_FOLLOW) {
				
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is following the line");
		    txtConnected.setTextColor(getResources().getColor(R.color.blue));
		    
		    wheelphone.disableObstacleAvoidance();

		    if(wheelphone.getGroundProx(1)>(groundThreshold+OUT_OF_LINE_THR) && wheelphone.getGroundProx(2)>(groundThreshold+OUT_OF_LINE_THR)) {	// i'm going to go out of the line
				outOfLine++;
				if(outOfLine > 10) {	// about 3*50=150 ms
					globalState = STATE_DOCKING_SEARCH;
					prevGlobalState = STATE_LINE_FOLLOW;
				}
			} else {
				outOfLine = 0;
			}
		    
			if(wheelphone.getGroundProx(0) < groundThreshold && wheelphone.getGroundProx(1)>groundThreshold && wheelphone.getGroundProx(2)>groundThreshold && wheelphone.getGroundProx(3)>groundThreshold) { // left ground inside line, turn left
				lSpeed = -20;
				rSpeed = 20;
			} else if(wheelphone.getGroundProx(3)<groundThreshold && wheelphone.getGroundProx(0)>groundThreshold && wheelphone.getGroundProx(1)>groundThreshold && wheelphone.getGroundProx(2)>groundThreshold) {	// right ground inside line, turn right
				lSpeed = 20;
				rSpeed = -20;
			} else if(wheelphone.getGroundProx(1)>groundThreshold && wheelphone.getGroundProx(2)<groundThreshold) {	// left leaving the line => turn right
				lSpeed = (wheelphone.getGroundProx(1)-groundThreshold)/10;	// /20
				if(lSpeed < minSpeedLineFollow) {
					lSpeed = minSpeedLineFollow;
				}
				rSpeed = -((wheelphone.getGroundProx(1)-groundThreshold)/10);	
			} else if(wheelphone.getGroundProx(2)>groundThreshold && wheelphone.getGroundProx(1)<groundThreshold) {	// right leaving the line => turn left
				lSpeed = -((wheelphone.getGroundProx(2)-groundThreshold)/10);
				rSpeed = (wheelphone.getGroundProx(2)-groundThreshold)/10;	
				if(rSpeed < minSpeedLineFollow) {
					rSpeed = minSpeedLineFollow;
				}
			} else {	// within the line
				lSpeed = 20;
				rSpeed = 20;
				
				if(globalState==STATE_LINE_FOLLOW && prevGlobalState==STATE_ROBOT_CHARGING) {
					instableChargeCounter++;
					if(instableChargeCounter >= 40) {
						lSpeed = 80;
						rSpeed = 80;
					}
					if(instableChargeCounter >= 60) {
						lSpeed = 0;
						rSpeed = 0;
					}
					if(instableChargeCounter >= 80) {
						lSpeed = -10;
						rSpeed = 40;
					} 
					if(instableChargeCounter >= 90) {
						lSpeed = 0;
						rSpeed = 0;
					}
					if(instableChargeCounter >= 110) {
						lSpeed = 40;
						rSpeed = -10;
					}
					if(instableChargeCounter >= 120) {
						lSpeed = 0;
						rSpeed = 0;
					}
					if(instableChargeCounter >= 140) {
						instableChargeCounter = 40;
					}								
				}
			}			
				
			if(chargeStatus == ROBOT_IN_CHARGE) {
				chargeCounter = 0;
				globalState = STATE_ROBOT_CHARGING;
				prevGlobalState = STATE_LINE_FOLLOW;
				outOfLine = 0;
			}
			
			lineFollowTimeout++;
			if(lineFollowTimeout >= 400) {	// about 20 seconds
				lSpeed = -40;
				rSpeed = -40;
				lineFollowTimeout = 0;
				globalState = STATE_ROBOT_GO_BACK;
				prevGlobalState = STATE_LINE_FOLLOW;
			}
				
		} else if(globalState == STATE_ROBOT_GO_BACK) { 
			lineFollowTimeout++;
			if(lineFollowTimeout >= 40) {	// about 2 second
				globalState = STATE_DOCKING_SEARCH;
				prevGlobalState = STATE_ROBOT_GO_BACK;
			}
			
		} else if(globalState == STATE_ROBOT_CHARGING) {
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is charging");
		    txtConnected.setTextColor(getResources().getColor(R.color.yellow));
		    	
		    if(chargeStatus == ROBOT_NOT_CHARGING) {
		    	globalState = STATE_LINE_FOLLOW;
		    	prevGlobalState = STATE_ROBOT_CHARGING;
		    	lineFollowTimeout = 0;
		    } else {
		    	chargeCounter++;
		    	if(chargeCounter == 20) {		
		    		instableChargeCounter = 0;
		    		tts.speak("charging", TextToSpeech.QUEUE_FLUSH, null);
		    	} else if(chargeCounter > 20) {
		    		chargeCounter = 21;
		    	}
		    }
		    	
			rSpeed = 0;
			lSpeed = 0;	
			
		}
		
		wheelphone.setRawLeftSpeed(lSpeed);
		wheelphone.setRawRightSpeed(rSpeed);
		leftSpeed.setText(String.valueOf(lSpeed));
		rightSpeed.setText(String.valueOf(rSpeed));
		
	}
    
    
}

