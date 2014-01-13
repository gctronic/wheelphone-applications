package com.wheelphone.wheelphone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

public class WheelphoneActivity extends Activity implements WheelPhoneRobotListener {
	
	// Various
	private String TAG = WheelphoneActivity.class.getName();
	private SoundPool soundPool = new SoundPool(4,AudioManager.STREAM_MUSIC,0);
	private Field[] raws = R.raw.class.getFields();
	private Context context;
	boolean getFirmwareFlag = true;
	private String logString;
	private boolean debugUsbComm = false;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private static final int MIN_SPEED = -350;	// 350 mm/s
	private static final int MAX_SPEED = 350;
	private static final int SPEED_STEP_MM_S = 20;
	private int firmwareVersion=0;
	
	// stay on table behavior
	private boolean stayOnTableFlag = false;
	private static final int MOVE_AROUND = 0;
	private static final int COME_BACK = 1;
	private static final int ROTATE = 2;
	private short globalState = MOVE_AROUND;
	private static final int GROUND_LEFT = 0;
	private static final int GROUND_CENTER_LEFT = 1;
	private static final int GROUND_CENTER_RIGHT = 2;
	private static final int GROUND_RIGHT = 3;
	private int minSensorValue = 0;
	private int minSensor = GROUND_LEFT;
	private int moveBackCounter = 0;
	private int rotateCounter = 0;
	private int rSpeedDes=20, lSpeedDes=20;
	private int robotStoppedCounter=0;
	private int initCounter=0;
	private static final int HORN_THR = 20;
	
	// UI
	private TextProgressBar barProx0, barProx1, barProx2, barProx3, barGround0, barGround1, barGround2, barGround3;   
	private TextView batteryState, batteryValue;
	private TextView leftSpeed, rightSpeed, leftMeasSpeed, rightMeasSpeed;
	private TextView txtConnected;		
	private TabSpec tabSensors, tabBehaviors;	
	private Button btnStayOnTable;
	private CheckBox chkSpeedControl, chkSoftAcc, chkObstacleAvoid, chkCliffAvoid;		
	private ImageView img;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		if(debugUsbComm) {
			logString = TAG + ": onCreate";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
      		        
        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        tabSensors=tabHost.newTabSpec("Sensors");
        tabSensors.setContent(R.id.tab_sensors);
        tabSensors.setIndicator("Sensors");
        tabBehaviors=tabHost.newTabSpec("Behaviors");        
        tabBehaviors.setContent(R.id.tab_behaviors);
        tabBehaviors.setIndicator("Behaviors");
        tabHost.addTab(tabSensors);
        tabHost.addTab(tabBehaviors);        
        for (int i = 0; i < tabHost.getTabWidget().getTabCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = getWindowManager().getDefaultDisplay().getHeight()*7/100; // 7% of total screen height
        }  
        
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
        leftSpeed = (TextView)findViewById(R.id.leftSpeedTxt);
        rightSpeed = (TextView)findViewById(R.id.rightSpeedTxt);
        leftMeasSpeed = (TextView)findViewById(R.id.leftMeasSpeedTxt);
        rightMeasSpeed = (TextView)findViewById(R.id.rightMeasSpeedTxt);
    	txtConnected = (TextView)findViewById(R.id.txtConnection);
    	btnStayOnTable = (Button)findViewById(R.id.btnStayOnTable);
    	chkSpeedControl = (CheckBox)findViewById(R.id.chkSpeedControl);
    	chkSoftAcc = (CheckBox)findViewById(R.id.chkSoftAcc);
    	chkObstacleAvoid = (CheckBox)findViewById(R.id.chkObstacleAvoid);
    	chkCliffAvoid = (CheckBox)findViewById(R.id.chkCliffAvoid);
    	img = (ImageView) findViewById(R.id.imageView1);
    	
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				soundPool.play(sampleId, 0.99f, 0.99f, 1, 0, 1);
			}
		});    	
    	
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		context = this.getApplicationContext();
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();
        chkSpeedControl.setChecked(true);
        wheelphone.enableSoftAcceleration();
        chkSoftAcc.setChecked(true);

		
	}

	public void updateImage(String name) {
		try {
			img.setImageResource(R.drawable.class.getField(name).getInt(null));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void playSound(int id) {
		try {
			soundPool.load(context, raws[id].getInt(null), 0);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void playSound(String name) {
		try {
			soundPool.load(context, R.raw.class.getField(name).getInt(null), 0);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public void onStart() {
		if(debugUsbComm) {
			logString = TAG + ": onStart";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}		
		super.onStart();	
	}
	
    @Override
    public void onResume() {
		if(debugUsbComm) {
			logString = TAG + ": onResume";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}    	
    	super.onResume();
    	wheelphone.startUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(this);
    }	
    
    @Override
    public void onStop() {
		if(debugUsbComm) {
			logString = TAG + ": onStop";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}    	
    	super.onStop();    	
    	android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    @Override
    public void onPause() {
		if(debugUsbComm) {
			logString = TAG + ": onPause";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}    	
    	super.onPause();
    	wheelphone.closeUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(null);
    }  
    
    public void leftIncrement(View view) { 
    	if(lSpeed < (MAX_SPEED-SPEED_STEP_MM_S)) {
    		lSpeed+=SPEED_STEP_MM_S;
    	} else {
    		lSpeed = MAX_SPEED;
    	}
    	wheelphone.setLeftSpeed(lSpeed);
    	//wheelphone.setRawLeftSpeed(lSpeed);
    	leftSpeed.setText(String.valueOf(lSpeed));
    }
    	
    public void leftDecrement(View view) {     	
    	if(lSpeed > (MIN_SPEED+SPEED_STEP_MM_S)) {
    		lSpeed-=SPEED_STEP_MM_S;
    	} else {
    		lSpeed = MIN_SPEED;
    	}
    	wheelphone.setLeftSpeed(lSpeed);
    	//wheelphone.setRawLeftSpeed(lSpeed);
    	leftSpeed.setText(String.valueOf(lSpeed));
    }
    
    public void rightIncrement(View view) { 

    	if(rSpeed < (MAX_SPEED-SPEED_STEP_MM_S)) {
    		rSpeed+=SPEED_STEP_MM_S;
    	} else {
    		rSpeed = MAX_SPEED;
    	}
    	wheelphone.setRightSpeed(rSpeed); 
    	//wheelphone.setRawRightSpeed(rSpeed);
    	rightSpeed.setText(String.valueOf(rSpeed));
    }
    	
    public void rightDecrement(View view) { 

    	if(rSpeed > (MIN_SPEED+SPEED_STEP_MM_S)) {
    		rSpeed-=SPEED_STEP_MM_S;
    	} else {
    		rSpeed = MIN_SPEED;
    	}
    	wheelphone.setRightSpeed(rSpeed); 
    	//wheelphone.setRawRightSpeed(rSpeed);
    	rightSpeed.setText(String.valueOf(rSpeed));
    }

    public void stopMotors(View view) { 
    	rSpeed=0;
    	lSpeed=0;
    	wheelphone.setSpeed(lSpeed, rSpeed);
    	leftSpeed.setText(String.valueOf(lSpeed));
    	rightSpeed.setText(String.valueOf(rSpeed));
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
    
    public void obstacleAvoidOnOff(View view) {
    	if(chkObstacleAvoid.isChecked()) {
    		wheelphone.enableObstacleAvoidance();
    	} else {
    		wheelphone.disableObstacleAvoidance();
    	}
    }  
    
    public void cliffAvoidOnOff(View view) {
    	if(chkCliffAvoid.isChecked()) {
    		wheelphone.enableCliffAvoidance();
    	} else {
    		wheelphone.disableCliffAvoidance();
    	}
    }     
    
    public void calibrateSensors(View view) {
    	wheelphone.calibrateSensors();
    }    
	
    public void stayOnTableState(View view) {
    	if(stayOnTableFlag) {
    		stayOnTableFlag = false;
    		btnStayOnTable.setText(getResources().getString(R.string.txtStartStayOnTable));
            wheelphone.disableObstacleAvoidance();
            chkObstacleAvoid.setChecked(false);
            wheelphone.disableCliffAvoidance();
            chkCliffAvoid.setChecked(false);   		
            wheelphone.setRawSpeed(0, 0);
    	} else {    		
    		wheelphone.calibrateSensors();
    		btnStayOnTable.setText(getResources().getString(R.string.txtStopStayOnTable));            
    		wheelphone.enableSpeedControl();
            chkSpeedControl.setChecked(true);
            wheelphone.enableSoftAcceleration();
            chkSoftAcc.setChecked(true);
            wheelphone.enableObstacleAvoidance();
            chkObstacleAvoid.setChecked(true);
            wheelphone.enableCliffAvoidance();
            chkCliffAvoid.setChecked(true);
    		initCounter = 20;            
            globalState = MOVE_AROUND;
    		stayOnTableFlag = true;
    		playSound("audio02_car_starting");
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
 
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		barProx0.setProgress((int)wheelphone.getFrontProx(0));
		barProx0.setText(String.valueOf(wheelphone.getFrontProx(0)));
		barProx1.setProgress(wheelphone.getFrontProx(1));
		barProx1.setText(String.valueOf(wheelphone.getFrontProx(1)));
		barProx2.setProgress(wheelphone.getFrontProx(2));
		barProx2.setText(String.valueOf(wheelphone.getFrontProx(2)));
		barProx3.setProgress(wheelphone.getFrontProx(3));
		barProx3.setText(String.valueOf(wheelphone.getFrontProx(3)));
		barGround0.setProgress(wheelphone.getGroundProx(0));
		barGround0.setText(String.valueOf(wheelphone.getGroundProx(0)));
		barGround1.setProgress(wheelphone.getGroundProx(1));
		barGround1.setText(String.valueOf(wheelphone.getGroundProx(1)));
		barGround2.setProgress(wheelphone.getGroundProx(2));
		barGround2.setText(String.valueOf(wheelphone.getGroundProx(2)));
		barGround3.setProgress(wheelphone.getGroundProx(3));
		barGround3.setText(String.valueOf(wheelphone.getGroundProx(3)));
		if(wheelphone.isCharging()) {
			batteryState.setText(getResources().getString(R.string.robotInCharge));
		} else if(wheelphone.isCharged()) {
			batteryState.setText("Robot is charged");
		} else {
			batteryState.setText("");
		}
		batteryValue.setText(String.valueOf(wheelphone.getBatteryRaw()));
		if(wheelphone.getBatteryRaw() <= 30) {
			batteryValue.setTextColor(getResources().getColor(R.color.red));
		} else {
			batteryValue.setTextColor(getResources().getColor(R.color.green));
		}	
		
		leftMeasSpeed.setText(String.valueOf(wheelphone.getLeftSpeed()));
		rightMeasSpeed.setText(String.valueOf(wheelphone.getRightSpeed()));				
		if(wheelphone.isRobotConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}

		if(stayOnTableFlag) {
			
			if(wheelphone.isCalibrating()) {
				return;
			}
			
			if(globalState == MOVE_AROUND) {
				
				wheelphone.enableCliffAvoidance();
				wheelphone.setRawLeftSpeed(lSpeedDes);
				wheelphone.setRawRightSpeed(rSpeedDes);
				
				if(wheelphone.getFrontProx(0) >= HORN_THR || wheelphone.getFrontProx(1) >= HORN_THR || wheelphone.getFrontProx(2) >= HORN_THR || wheelphone.getFrontProx(3) >= HORN_THR) {
					updateImage("turn_idle");
					playSound("audio00_car_horn1");				    					
				} 							
				
				if(initCounter > 0) { 	// let the robot start moving forward before beginning the behavior otherwise 
					initCounter--;		// a false cliff is detected (vel=0 for both motors)
					return;
				}
				
				if(wheelphone.getRightSpeed()==0 && wheelphone.getLeftSpeed()==0) { // && cliffFlag) {	// cliff detected
					robotStoppedCounter++;
//					if(robotStoppedCounter==1) {
//						updateImage("drive_01");
//						playSound("audio01_car_skid2");
//					}
				} else {
					robotStoppedCounter = 0;
				}
				
				if(robotStoppedCounter >= 5) {	//5*50  about 250 ms	
							
					updateImage("drive_01");
					playSound("audio01_car_skid2");
					
					robotStoppedCounter = 0;
					
					minSensorValue = wheelphone.getGroundProx(0);
					minSensor = GROUND_LEFT;
					if(wheelphone.getGroundProx(1) < minSensorValue) {
						minSensorValue = wheelphone.getGroundProx(1);
						minSensor = GROUND_CENTER_LEFT;
					}
					if(wheelphone.getGroundProx(2) < minSensorValue) {
						minSensorValue = wheelphone.getGroundProx(2);
						minSensor = GROUND_CENTER_RIGHT;
					}
					if(wheelphone.getGroundProx(3) < minSensorValue) {
						minSensorValue = wheelphone.getGroundProx(3);
						minSensor = GROUND_RIGHT;
					}	
					
					wheelphone.setRawLeftSpeed(-60);
					wheelphone.setRawRightSpeed(-60);								
					wheelphone.disableCliffAvoidance();	// disable cliff avoidance to let the robot move backward
					globalState = COME_BACK;								
					moveBackCounter = 0;	
					wheelphone.disableObstacleAvoidance();
				}

			} else if(globalState == COME_BACK) {

				if(moveBackCounter==0) {
					playSound("audio05_truck_back");
				}
				
				moveBackCounter++;
		    	if(moveBackCounter >= 15) {	// about 750 msec
		    		rotateCounter = 0;
					switch(minSensor) {
						case GROUND_LEFT:
						case GROUND_CENTER_LEFT:
							wheelphone.setRawLeftSpeed(20);
							wheelphone.setRawRightSpeed(-20);
							updateImage("drive_dx");
							break;
					
						case GROUND_RIGHT:
						case GROUND_CENTER_RIGHT:
							wheelphone.setRawLeftSpeed(-20);
							wheelphone.setRawRightSpeed(20);
							updateImage("drive_sx");
							break;
							
					}					    		
		    		globalState = ROTATE;
		    	}
				
			} else if(globalState == ROTATE) {
				
				rotateCounter++;
				if(rotateCounter >= 10) {
					globalState = MOVE_AROUND;
					updateImage("drive_00");
					playSound("audio02_car_starting");
					wheelphone.enableObstacleAvoidance();
				}
				
			}						
		} // stay on table
		
	}      
	
	void appendLog(String fileName, String text, boolean clearFile)
	{       
	   File logFile = new File("sdcard/" + fileName);
	   if (!logFile.exists()) {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   } else {
		   if(clearFile) {
			   logFile.delete();
			   try {
				   logFile.createNewFile();
			   } catch (IOException e) {
				   // TODO Auto-generated catch block
				   e.printStackTrace();
			   }
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
    
}
