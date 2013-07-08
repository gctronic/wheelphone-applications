package com.wheelphone.follow;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class WheelphoneActivity extends Activity {
	
	// Various
	private String TAG = "Wheelphone";
	Timer timer = new Timer();
	private SoundPool soundPool = new SoundPool(4,AudioManager.STREAM_MUSIC,0);
	private Field[] raws = R.raw.class.getFields();
	private Context context;
	boolean getFirmwareFlag = true;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private static final int MIN_SPEED = -300;	// 300 mm/s
	private static final int MAX_SPEED = 300;
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
	private TextView batteryState, batteryValue, outputText;
	private TextView leftSpeed, rightSpeed, leftEncoder, rightEncoder;
	private TextView txtConnected;		
		
	private Button btnStayOnTable;
	
	//Follow
	private int desiredDist = 50;
	private Integer[] tmpFrontProxArray = new Integer[4];
	
	//tells activity to run on ui thread    
	class uiUpdateTask extends TimerTask {          
		@Override        
		public void run() {             
			WheelphoneActivity.this.runOnUiThread(new Runnable() {                  
				//@Override                 
				public void run() {

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
					
					leftEncoder.setText(String.valueOf(wheelphone.getLeftEncoder()));
					rightEncoder.setText(String.valueOf(wheelphone.getRightEncoder()));				
					if(wheelphone.isUSBConnected()) {
				    	txtConnected.setText("Connected");
				    	txtConnected.setTextColor(getResources().getColor(R.color.green));
					} else {
						txtConnected.setText("Disconnected");
						txtConnected.setTextColor(getResources().getColor(R.color.red));
					}
					
					
					//Spring damping system
					
					int [] frontProxArray = wheelphone.getFrontProxs();
					
					int maxIdx = 0;
					int max = frontProxArray[0];
					
					for (int i = 0; i < frontProxArray.length; i++) {
					    if (frontProxArray[i] > max) {
					        max = frontProxArray[i];
					        maxIdx = i;
					    }
					}
					//Check that the max is above 0 (there is actually an object in front that should be followed 
					int followSwitch = Math.max(0, Math.min(1, max));

					int linearAcc = (desiredDist - Math.max(frontProxArray[1], frontProxArray[2]));
					int linearSpringConst = 1;
					int angularSpringConst = 2;
					
					int angularAcc = 0;
					
//					switch (maxIdx){
//						case 0:
//							angularAcc = -frontProxArray[0];
//						break;
//						case 3: 
//							angularAcc =  frontProxArray[3];
//						break;
//						default:
//							angularAcc = (-frontProxArray[1] + frontProxArray[2])/2; //less weight for the center sensors							
//						break;
//					}
//					angularAcc = angularAcc / 2;
					angularAcc = - 2 * frontProxArray[0] - frontProxArray[1] 
							     + 2 * frontProxArray[3] + frontProxArray[2];

					int dampingNumerator = 1;
					int dampingDenominator = 1;

					lSpeed = lSpeed 
							+   linearAcc / linearSpringConst
							+	angularAcc / angularSpringConst
							-	dampingNumerator * lSpeed
							/ dampingDenominator;

					rSpeed = rSpeed 
							+   linearAcc / linearSpringConst
							-	angularAcc / angularSpringConst
							-	dampingNumerator * rSpeed
							/ dampingDenominator;

					outputText.setText("Left: " + lSpeed + "\n" +
							"Right: " + rSpeed + "\n" +
							"linearAcc: " + linearAcc + "\n" +
							"angularAcc: " + angularAcc + "\n" +
							"followSwitch:" + followSwitch
							);
			    	wheelphone.setSpeed(followSwitch * lSpeed, followSwitch * rSpeed);
				}             
			});         
		}    
	}; 	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//Prevent from sleeping:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        leftEncoder = (TextView)findViewById(R.id.leftEncTxt);
        rightEncoder = (TextView)findViewById(R.id.rightEncTxt);
    	txtConnected = (TextView)findViewById(R.id.txtConnection);
    	outputText = (TextView)findViewById(R.id.outputText);
    	
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				soundPool.play(sampleId, 0.99f, 0.99f, 1, 0, 1);
			}
		});    	
    	
		context = this.getApplicationContext();
		
        wheelphone = new WheelphoneRobot(this, this);
        wheelphone.startUSBCommunication();
//        wheelphone.enableSpeedControl();
//        wheelphone.enableSoftAcceleration();
        
        timer = new Timer();                                         
        timer.schedule(new uiUpdateTask(), 0, 50); 
		
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
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
    @Override
    public void onResume() {
    	super.onResume();
    	wheelphone.resumeUSBCommunication();
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
    
}
