
package com.wheelphone.alarm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

public class ActivityMain extends Activity implements SensorEventListener, WheelPhoneRobotListener {

	private Sensor mProximity;
	private SensorManager mSensorManager;
	private static Boolean isRinging = false;
	public static int runningSpeed = 0;
	private SharedPreferences sharedPrefs;
	private Ringtone mRingtone;
	//Vibrator mVibrator;

	// Robot state
	WheelphoneRobot wheelphone;
	private int firmwareVersion=0;
	private static int currSpeed=0;
	
	// Various
	private static final String TAG = ActivityMain.class.getName();
	private String logString;
	private boolean debug = false;
	boolean getFirmwareFlag = true;
	private boolean mStartAlarm = false;
	private boolean commStarted = false;
	public static int mEscapeTimeoutSec = 1;
	Random mRndColor = new Random();
	
	// Behavior
	private static final int GO_OUT_FROM_DOCKING = 0;
	private static final int ROTATE_180_DEGREES = 1;
	private static final int WAIT_TIMEOUT = 2;
	private static final int MOVING_AROUND = 3;
	private static final int RANDOM_TURN = 4;
	private int mState = GO_OUT_FROM_DOCKING;
	private long mTimeMillis = 0;
	private boolean mRestartState = true;
	private Random mRandom;
	private int mRndInt;
	
	// UI
	private TextView txtConnected, batteryState;	
	private Button btnCalibrate;
	private FragmentAlarm fragmentAlarm;
	private FragmentSettings fragmentSettings;
	private FragmentAbout fragmentAbout;

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		float eventValue = event.values[0];
		switch (event.sensor.getType()){
		case Sensor.TYPE_PROXIMITY:
			if (isRinging && eventValue == 0){//Hand approaching:
				Log.d(TAG, "Chassing");
				currSpeed = runningSpeed*2;
				wheelphone.setSpeed(currSpeed, currSpeed);
			} else if(isRinging) { //Nobody chasing
				//wheelphone.setSpeed(0, 0);
				Log.d(TAG, "Not chassing");
			}
			break;
		}
	}

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(debug) {
			logString = TAG + ": onCreate";
			Log.d(TAG, logString);
			appendLog("debug.txt", logString, false);
		} else {
			
		}
		
		// Init bot:
		wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
		wheelphone.enableSpeedControl();
		wheelphone.enableSoftAcceleration();
		wheelphone.enableObstacleAvoidance();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		//Call once the default settings setter:
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		//Read settings
		readSettings();

		setContentView(R.layout.activity_main);

		visualizeSettings();

		// Get an instance of the sensor service, and use that to get an instance of
		// a particular sensor.
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		
		txtConnected = (TextView)findViewById(R.id.txtConnection);
		batteryState = (TextView)findViewById(R.id.txtRobotInCharge);
		btnCalibrate = (Button)findViewById(R.id.btnCalibrate);
		
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
		if(debug) {
			Log.d("YourActivity", "onNewIntent is called!");
		}

		if (intent.hasExtra("start_alarm")) {
			mStartAlarm = true;
		}

		super.onNewIntent(intent);
	} 

	@Override
	public void onResume() {
		if(debug) {
			logString = TAG + ": onRsume()";
			Log.d(TAG, logString);
			appendLog("debug.txt", logString, false);
		}
		
		super.onResume();
		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
	
		if (mStartAlarm) {
			
			mStartAlarm = false;
			
			if(debug) {
				logString = TAG + ": ring alarm intent";
				Log.d(TAG, logString);
				appendLog("debug.txt", logString, false);
			}
			
			//Toast.makeText(this, "RING RING !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
			Log.d(TAG, "RING RING!!!!");

			final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
			audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE); 
			audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);		

			Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			mRingtone = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
			mRingtone.setStreamType(RingtoneManager.TYPE_ALARM);
			mRingtone.play();
			
//			long[] pattern = {0, 500, 500};
//			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//			mVibrator.vibrate(pattern, 1);			
			
			currSpeed = runningSpeed;
			if(debug) {
				logString = TAG + ": curr speed = " + currSpeed;
				Log.d(TAG, logString);
				appendLog("debug.txt", logString, false);
			}
			
			wheelphone.setSpeed(currSpeed, currSpeed);			
			
			hideRobotInfo();		
			visualizeAlarm();

			//Remove from the preferences the current alarm time
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putLong("prefAlarmTime", 0);
			editor.commit();	
			
			mState = GO_OUT_FROM_DOCKING;
			mRestartState = true;
			
			isRinging = true;
			
		} else {
			if(!commStarted) {
				if(debug) {
					logString = TAG + ": start communication with robot";
					Log.d(TAG, logString);
					appendLog("debug.txt", logString, false);
				}
				commStarted = true;
				wheelphone.startUSBCommunication();
			}
		}		
		
		wheelphone.setWheelPhoneRobotListener(this);
	}

	@Override
	public void onPause() {
		if(debug) {
			logString = TAG + ": onPause()";
			Log.d(TAG, logString);
			appendLog("debug.txt", logString, false);
		}
		
		super.onPause();
		mSensorManager.unregisterListener(this);
		//wheelphone.closeUSBCommunication();
		wheelphone.setWheelPhoneRobotListener(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Fragment fragment = null;
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_about:
	        	Log.d(TAG, "about");
	    		if (fragmentAbout == null)
	    			fragmentAbout = new FragmentAbout();
	    		fragment = fragmentAbout;
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
		if(fragment != null){
			hideRobotInfo();
			fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
			ft.addToBackStack(null).replace(R.id.container, fragment).commit();			
		}
		return true;
	}
	
	private  OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {
		@Override
        public void onBackStackChanged() {
        	Log.d(TAG, "getFragmentManager().getBackStackEntryCount(): " + getFragmentManager().getBackStackEntryCount());
            if (getFragmentManager().getBackStackEntryCount() == 0) {
            	Log.d(TAG, "back to setting fragment");            	
            	showRobotInfo();
            	FragmentManager manager = getFragmentManager();
            	FragmentSettings.WakeUpConfiguration fw = (FragmentSettings.WakeUpConfiguration)manager.findFragmentById(R.id.fragment_preferences);
            	fw.onResume();
            }
        }
    };
	
    private void hideRobotInfo() {
    	txtConnected.setVisibility(View.GONE);
		batteryState.setVisibility(View.GONE);
		btnCalibrate.setVisibility(View.GONE);
    }
    
    private void showRobotInfo() {
    	txtConnected.setVisibility(TextView.VISIBLE);
		batteryState.setVisibility(TextView.VISIBLE);
		btnCalibrate.setVisibility(Button.VISIBLE);
    }
    
	private void visualizeSettings() {
		Fragment fragment = null;
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		if (fragmentSettings == null)
			fragmentSettings = new FragmentSettings();
		fragment = fragmentSettings;
		if (fragment != null){
			ft.replace(R.id.container, fragment);
			ft.commit();
		}		
	}
	
	private void visualizeAlarm() {
		Fragment fragment = null;
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		if (fragmentAlarm == null)
			fragmentAlarm = new FragmentAlarm();
		fragment = fragmentAlarm;
		if (fragment != null){
			ft.replace(R.id.container, fragment);
			ft.commit();
		}		
	}
	
	/* 
	 * Read alarm and robot settings from Preferences
	 */
	private void readSettings() {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String tPref = sharedPrefs.getString("prefRunningSpeed", "250");
		runningSpeed = Integer.valueOf(tPref);
		
		mEscapeTimeoutSec = Integer.valueOf(sharedPrefs.getString("prefEscapeTimeout", "1"));
		
		Long alarm = sharedPrefs.getLong("prefAlarmTime", 0);

		if (alarm != 0) {
			Calendar newAlarm = Calendar.getInstance();
			newAlarm.setTimeInMillis(alarm);
		}
	}

	public void setQuickAlarm(View view){
		fragmentSettings.setQuickAlarm(view);
	}

	public void cancelAlarm(View view){
		fragmentSettings.cancelAlarm(view);
	}

	public void cancelRinging(View view){
		if (isRinging){
			Toast.makeText(this, "OK! I give up", Toast.LENGTH_SHORT).show();

			mRingtone.stop();
//			mVibrator.cancel();			
			
			if(debug) {
				logString = TAG + ": alarm canceled";
				Log.d(TAG, logString);
				appendLog("debug.txt", logString, false);
			}
			
			currSpeed = 0;
			isRinging = false;
			wheelphone.setSpeed(currSpeed, currSpeed);

			showRobotInfo();
			visualizeSettings();	
			
		}
	}
	
    public void calibrateSensors(View view) {
    	wheelphone.calibrateSensors();
    }  

	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(ActivityMain.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		if(wheelphone.isCharging()) {
			batteryState.setText("Charging");
			batteryState.setTextColor(getResources().getColor(R.color.green));
		} else if(wheelphone.isCharged()) {
			batteryState.setText("Charge complete");
			batteryState.setTextColor(getResources().getColor(R.color.green));
		} else {
			batteryState.setText("NOT charging");
			batteryState.setTextColor(getResources().getColor(R.color.red));
		}
		
		if(wheelphone.isRobotConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}
				
		if(isRinging) {
			
			FragmentAlarm.changeButtonColor(Color.argb(255, mRndColor.nextInt(255), mRndColor.nextInt(255), mRndColor.nextInt(255)));
			
			switch(mState) {
				case GO_OUT_FROM_DOCKING:					
					if(mRestartState) {						
						mRestartState = false;
						wheelphone.disableObstacleAvoidance();
						mTimeMillis = System.currentTimeMillis();
						wheelphone.setSpeed(-40, -40);
					}
					if((System.currentTimeMillis() - mTimeMillis) > 2000) {						
						mState = ROTATE_180_DEGREES;
						wheelphone.resetOdometry();
						wheelphone.setSpeed(0, 160);
					}
					break;
				
				case ROTATE_180_DEGREES:
				    if(wheelphone.getOdometryTheta() >= Math.PI) {
				    	mRestartState = true;
				    	mState = WAIT_TIMEOUT;
						wheelphone.setSpeed(0, 0);
				    }
					break;
					
				case WAIT_TIMEOUT:	// user can define a timeout before the robot start escaping
					if(mRestartState) {
						mRestartState = false;
						mTimeMillis = System.currentTimeMillis();
					}
					if((System.currentTimeMillis() - mTimeMillis) > (long)(mEscapeTimeoutSec*1000)) {						
						mRestartState = true;
						mState = MOVING_AROUND;
					}
					break;
					
				case MOVING_AROUND:
					if(mRestartState) {						
						mRestartState = false;
						wheelphone.enableObstacleAvoidance();
						mTimeMillis = System.currentTimeMillis();
						currSpeed = runningSpeed;
						wheelphone.setSpeed(currSpeed, currSpeed);	
						if(debug) {
							logString = TAG + ": MOVING_AROUND: curr speed = " + currSpeed;
							Log.d(TAG, logString);
							appendLog("debug.txt", logString, false);
						}
					} else {
						if((System.currentTimeMillis() - mTimeMillis) > 5000) {
							mState = RANDOM_TURN;
							mRestartState = true;
						}
					}
										
					break;									
			
				case RANDOM_TURN:
					if(mRestartState) {						
						mRestartState = false;
						wheelphone.enableObstacleAvoidance();
						mTimeMillis = System.currentTimeMillis();
						mRandom = new Random();
						mRndInt = mRandom.nextInt(3000) + 100;	// random from 100 to 3100 ms
						if(mRndInt > 1550) {
							wheelphone.setSpeed(-runningSpeed, runningSpeed);
							if(debug) {
								logString = TAG + ": RANDOM_TURN: curr speed = " + runningSpeed/2 + " (left), random = " + mRndInt;
								Log.d(TAG, logString);
								appendLog("debug.txt", logString, false);
							}
						} else {
							wheelphone.setSpeed(runningSpeed, -runningSpeed);
							if(debug) {
								logString = TAG + ": RANDOM_TURN: curr speed = " + runningSpeed/2 + " (right), random = " + mRndInt;
								Log.d(TAG, logString);
								appendLog("debug.txt", logString, false);
							}
						}
					} else {
						if(System.currentTimeMillis() > (mTimeMillis + mRndInt)) {
							mState = MOVING_AROUND;
							mRestartState = true;
						}
					}
					break;
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
	
	static void appendLog(String fileName, String text, boolean clearFile)
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
