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

package com.wheelphone.lineFollowing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

public class WheelphoneLineFollowing extends Activity implements WheelPhoneRobotListener {

	// various
	private String TAG = WheelphoneLineFollowing.class.getName();
	boolean getFirmwareFlag = true;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private int firmwareVersion=0;
	private static final int MAX_SPEED = 350;
	
	// line following
	public static final int LINE_SEARCH = 0;
	public static final int LINE_FOLLOW = 1;
	public static final int AVOID_OBSTACLE = 2;
	public static final int INIT_GROUND_THR = 125;
	public static final int MAX_DIFF_OUT = 20;
	public short globalState=LINE_SEARCH;
	public int groundThreshold = INIT_GROUND_THR;
	public int[] groundValues = {1023,1023,1023,1023};	// max adc value (super white surface)
	public int proxThreshold = 50;
	public int[] proxValues = {0, 0, 0, 0};
	public int lineFound = 0;
	public int outOfLine = 0;
	public short directionChanged = 0;
	public int lineFollowSpeed=0;
	public static final int FOLLOW_BLACK = 0;
	public static final int FOLLOW_WHITE = 1;
	public int followLogic = FOLLOW_BLACK;
	public int minSpeedLineFollow = 10;
	public int avoidObstacleCounter = 0;
	public int tempSpeed = 0;
	private int desiredSpeed = 20;
	
	// UI
	TextProgressBar barGround0, barGround1, barGround2, barGround3;   
		
	public void appendLog(String text)
	{       
	   File logFile = new File("sdcard/log.file");
	   if (!logFile.exists())
	   {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   }
	   try
	   {
	      //BufferedWriter for performance, true to set append to file flag
	      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	      buf.append(text);
	      buf.newLine(); 
	      buf.close();
	   }
	   catch (IOException e)
	   {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	   }
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
            
        setContentView(R.layout.main);
       	       
        barGround0 = (TextProgressBar) findViewById(R.id.barGround0);
        barGround1 = (TextProgressBar) findViewById(R.id.barGround1);
        barGround2 = (TextProgressBar) findViewById(R.id.barGround2);
        barGround3 = (TextProgressBar) findViewById(R.id.barGround3);
        
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
        
        EditText editText2 = (EditText) findViewById(R.id.txtSpeed);
        editText2.setOnEditorActionListener(new OnEditorActionListener() {
        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    desiredSpeed = Integer.parseInt(v.getText().toString());
                    if(desiredSpeed < 20) {
                    	desiredSpeed = 20;
                    	v.setText(Integer.toString(20));
                    } else if(desiredSpeed > MAX_SPEED) {
                    	desiredSpeed = MAX_SPEED;
                    	v.setText(Integer.toString(MAX_SPEED));
                    }
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }          
            
        }); 
        
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);	
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();
        wheelphone.enableObstacleAvoidance();
     
    }
	
	@Override
	public void onStart() {
		super.onStart();
		wheelphone.startUSBCommunication();	    
		this.setTitle("Wheelphone Line Following");
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	wheelphone.resumeUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(this);      		
    }
    
    public void onBackPressed() {
    	Log.d("back pressed", "back pressed");
    	Intent setIntent = new Intent(Intent.ACTION_MAIN);
    	setIntent.addCategory(Intent.CATEGORY_HOME);
    	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(setIntent);
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
    
    private void updateSensor(int id, int value) {
		
		switch(id) {
			case R.id.barGround0:
				barGround0.setProgress(value);
				barGround0.setText(String.valueOf(value));
				break;
				
			case R.id.barGround1:
				barGround1.setProgress(value);
				barGround1.setText(String.valueOf(value));
				break;
				
			case R.id.barGround2:
				barGround2.setProgress(value);
				barGround2.setText(String.valueOf(value));
				break;
				
			case R.id.barGround3:
				barGround3.setProgress(value);
				barGround3.setText(String.valueOf(value));
				break;
				
			default:
				TextView textviewToUpdate;	
				textviewToUpdate = (TextView)findViewById(id);
				textviewToUpdate.setText(String.valueOf(value));			
				break;
		
		}
		
    }    
    
    public void calibrateSensors(View view) {
    	wheelphone.calibrateSensors();
    }
    
    public void invertLogic(View view) {
    	CheckBox chkToUpdate;	
    	chkToUpdate = (CheckBox)findViewById(R.id.chkInvert);

    	if(chkToUpdate.isChecked()) {
    		followLogic = FOLLOW_WHITE;
    	} else {
    		followLogic = FOLLOW_BLACK;
    	}    	
    }
    
    
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneLineFollowing.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		// here you can insert behavior processed about every 50 ms (20 Hz)
		
		// line following						
		// ground sensors position:
		//		G1		G3
		// G0				G2
		
		if(wheelphone.isUSBConnected()) {
		
	    	TextView txtConnected;	
	    	txtConnected = (TextView)findViewById(R.id.txtConnection);
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
			
			if(globalState == LINE_SEARCH) {
		    	
				TextView txtStatus;	
				txtStatus = (TextView)findViewById(R.id.txtGeneralStatus);
				txtStatus.setText("Robot is searching a line");
		    	txtStatus.setTextColor(getResources().getColor(R.color.red));
		    	
				// wheelphone.enableObstacleAvoidance();	// enable obstacle avoidance
				lSpeed = desiredSpeed;			// move around
				rSpeed = desiredSpeed;
				
				if(followLogic == FOLLOW_BLACK) {
					if(groundValues[0]<groundThreshold || groundValues[1]<groundThreshold || groundValues[2]<groundThreshold || groundValues[3]<groundThreshold) {
						lineFound++;
						if(lineFound > 3) {	// be sure to find a line to follow (avoid noise)
							globalState = LINE_FOLLOW;
						}
					} else {
						lineFound = 0;
					}
				} else {
					if(groundValues[0]>groundThreshold || groundValues[1]>groundThreshold || groundValues[2]>groundThreshold || groundValues[3]>groundThreshold) {
						lineFound++;
						if(lineFound > 3) {	// be sure to find a line to follow (avoid noise)
							globalState = LINE_FOLLOW;
						}
					} else {
						lineFound = 0;
					}								
				}
			} else if(globalState == LINE_FOLLOW) {
				TextView txtStatus;	
				txtStatus = (TextView)findViewById(R.id.txtGeneralStatus);
				txtStatus.setText("Robot is following the line");
				txtStatus.setTextColor(getResources().getColor(R.color.green));
		    	
				// wheelphone.disableObstacleAvoidance();
				
		    	if(proxValues[0]>proxThreshold || proxValues[1]>proxThreshold || proxValues[2]>proxThreshold || proxValues[3]>proxThreshold) {
		    		globalState = AVOID_OBSTACLE;
		    		avoidObstacleCounter = 0;
		    	}
		    	
				if(followLogic == FOLLOW_BLACK) {							
				
					if(groundValues[1]>(groundThreshold+MAX_DIFF_OUT) && groundValues[3]>(groundThreshold+MAX_DIFF_OUT) && groundValues[0]>(groundThreshold+MAX_DIFF_OUT) && groundValues[2]>(groundThreshold+MAX_DIFF_OUT)) {	// i'm going to go out of the line
						outOfLine++;
						if(outOfLine > 3) {
							globalState = LINE_SEARCH;
						}
					} else {
						outOfLine = 0;
					}
					
					if(groundValues[0] < groundThreshold && groundValues[1]>groundThreshold && groundValues[2]>groundThreshold && groundValues[3]>groundThreshold) { // left ground inside line, turn left
						lSpeed = 1; //-desiredSpeed; //-20*3;
						rSpeed = desiredSpeed*4; //20;
						if(rSpeed > MAX_SPEED) {
							rSpeed = MAX_SPEED;
						}
						//directionChanged=1;
					} else if(groundValues[3]<groundThreshold && groundValues[0]>groundThreshold && groundValues[1]>groundThreshold && groundValues[2]>groundThreshold) {	// right ground inside line, turn right
						lSpeed = desiredSpeed*4; //20;
						rSpeed = 1;//-desiredSpeed; //-20*3;
						if(lSpeed > MAX_SPEED) {
							lSpeed = MAX_SPEED;
						}									
						//directionChanged=1;
					} else if(groundValues[1]>groundThreshold) { // || groundValues[2]>groundThreshold) {
						tempSpeed = (groundValues[1]-groundThreshold)/((MAX_SPEED-desiredSpeed)/10+1)*4;
						if(tempSpeed > MAX_SPEED) {
							tempSpeed = MAX_SPEED;
						}										
						if(groundValues[2] <= groundValues[1]) {	// leaving line to the left => need to turn right
							//lSpeed = 20+((groundValues[1]-groundThreshold)/20)-((groundValues[3]-groundThreshold)/20);
							//rSpeed = 20-((groundValues[1]-groundThreshold)/20)+((groundValues[3]-groundThreshold)/20);
							lSpeed = tempSpeed; //(groundValues[1]-groundThreshold)/20;
							if(lSpeed < minSpeedLineFollow) {
								lSpeed = minSpeedLineFollow;
							}
							rSpeed = 1; //-tempSpeed; //-((groundValues[1]-groundThreshold)/20)*3;										
						} else {	// leaving line to the right => need to turn left
							//lSpeed = 20-((groundValues[3]-groundThreshold)/20)+((groundValues[1]-groundThreshold)/20);
							//rSpeed = 20+((groundValues[3]-groundThreshold)/20)-((groundValues[1]-groundThreshold)/20);
							lSpeed = 1; //-tempSpeed; //-((groundValues[3]-groundThreshold)/20)*3;
							rSpeed = tempSpeed; //(groundValues[3]-groundThreshold)/20;	
							if(rSpeed < minSpeedLineFollow) {
								rSpeed = minSpeedLineFollow;
							}											
						}
					} else {	// within the line
						lSpeed = desiredSpeed;
						rSpeed = desiredSpeed;									
						directionChanged=0;
					}			
					
				} else {
					
					if(groundValues[0]<(groundThreshold-MAX_DIFF_OUT) && groundValues[1]<(groundThreshold-MAX_DIFF_OUT) && groundValues[2]<(groundThreshold-MAX_DIFF_OUT) && groundValues[3]<(groundThreshold-MAX_DIFF_OUT)) {	// i'm going to go out of the line
						outOfLine++;
						if(outOfLine > 3) {
							globalState = LINE_SEARCH;
						}
					} else {
						outOfLine = 0;
					}
					
					if(groundValues[0] > groundThreshold && groundValues[1]<groundThreshold && groundValues[2]<groundThreshold && groundValues[3]<groundThreshold) { // left ground inside line, turn left
						lSpeed = 1;
						rSpeed = desiredSpeed*4;
						if(rSpeed > MAX_SPEED) {
							rSpeed = MAX_SPEED;
						}
					} else if(groundValues[3]>groundThreshold && groundValues[0]<groundThreshold && groundValues[1]<groundThreshold && groundValues[2]<groundThreshold) {	// right ground inside line, turn right
						lSpeed = desiredSpeed*4;
						rSpeed = 1;
						if(lSpeed > MAX_SPEED) {
							lSpeed = MAX_SPEED;
						}
					} else if(groundValues[1]<groundThreshold) {
						tempSpeed = (groundThreshold-groundValues[1])/((MAX_SPEED-desiredSpeed)/10+1)*4;
						if(tempSpeed > MAX_SPEED) {
							tempSpeed = MAX_SPEED;
						}										
						if(groundValues[2] >= groundValues[1]) {	// leaving line to the left => need to turn right
							lSpeed = tempSpeed;
							if(lSpeed < minSpeedLineFollow) {
								lSpeed = minSpeedLineFollow;
							}
							rSpeed = 1;									
						} else {	// leaving line to the right => need to turn left
							lSpeed = 1;
							rSpeed = tempSpeed;	
							if(rSpeed < minSpeedLineFollow) {
								rSpeed = minSpeedLineFollow;
							}											
						}
					} else {	// within the line
						lSpeed = desiredSpeed;
						rSpeed = desiredSpeed;									
						directionChanged=0;
					}									
					
				}
				
			} else if(globalState == AVOID_OBSTACLE) {
				avoidObstacleCounter++;
				if(avoidObstacleCounter >= 20) {
					globalState = LINE_SEARCH;
				}
				rSpeed = 20;
				lSpeed = 20;
			}
		
		} else {			
	    	TextView txtConnected;	
	    	txtConnected = (TextView)findViewById(R.id.txtConnection);
	    	txtConnected.setText("Disconnected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.red));
		}
			
		//appendLog("l=" + lSpeed + ", r=" + rSpeed + ", grounds:" + groundValues[0] + "," + groundValues[1] + "," + groundValues[3] + "," + groundValues[2] + "\n");

		proxValues[0] = wheelphone.getFrontProx(0);
		proxValues[1] = wheelphone.getFrontProx(1);
		proxValues[2] = wheelphone.getFrontProx(2);
		proxValues[3] = wheelphone.getFrontProx(3);
		groundValues[0] = wheelphone.getGroundProx(0);
		groundValues[1] = wheelphone.getGroundProx(1);
		groundValues[2] = wheelphone.getGroundProx(2);
		groundValues[3] = wheelphone.getGroundProx(3);
		updateSensor(R.id.barGround0, groundValues[0]);
		updateSensor(R.id.barGround1, groundValues[1]);
		updateSensor(R.id.barGround2, groundValues[2]);
		updateSensor(R.id.barGround3, groundValues[3]);
		updateSensor(R.id.rightSpeedTxt, rSpeed);
		updateSensor(R.id.leftSpeedTxt, lSpeed);
		
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
	    
    
}
