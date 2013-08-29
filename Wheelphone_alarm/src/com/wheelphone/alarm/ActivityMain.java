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

package com.wheelphone.alarm;

import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class ActivityMain extends Activity implements SensorEventListener, ActionBar.OnNavigationListener {

	private static final String TAG = ActivityMain.class.getName();

	private Sensor mProximity;
	private SensorManager mSensorManager;
	private static Boolean isRinging = false;
	private int runningSpeed = 0;
	private SharedPreferences sharedPrefs;
	private ArrayAdapter<String> navigationAdapter;
	private FragmentAlarm fragmentAlarm;
	private FragmentSettings fragmentSettings;

	private static int currSpeed=0;

	private WheelphoneRobot wheelphone;


	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";


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
				currSpeed = runningSpeed;
				wheelphone.setSpeed(currSpeed, currSpeed);
			} else if(isRinging) { //Nobody chasing
				wheelphone.setSpeed(0, 0);
				Log.d(TAG, "Not chassing");
			}
			break;
		}
	}

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Init bot:
		wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());

//		wheelphone.enableSpeedControl();
		wheelphone.startUSBCommunication();
		wheelphone.enableSoftAcceleration();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		try {
			PackageManager manager = this.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
			Log.d(TAG, "Info:" + info.packageName + "\n" + info.versionCode + "\n" + info.versionName); 
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//Call once the default settings setter:
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		//Read settings
		readSettings();

		setContentView(R.layout.activity_main);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		navigationAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				android.R.id.text1, new String[] {
				getString(R.string.action_settings),
				getString(R.string.action_preview), });

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				navigationAdapter, this);


		Intent i = getIntent();
		if (i.hasExtra("start_alarm")){
			Toast.makeText(this, "RING RING !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
			Log.d(TAG, "RING RING!!!!");

			//Set Alarm fragment:
			actionBar.setSelectedNavigationItem(1);

			//Remove from the preferences the current alarm time
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putLong("prefAlarmTime", 0);
			editor.commit();

			isRinging = true;
		}

		// Get an instance of the sensor service, and use that to get an instance of
		// a particular sensor.
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
		wheelphone.resumeUSBCommunication();
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		wheelphone.pauseUSBCommunication();
	}

	/* 
	 * Read alarm and robot settings from Preferences
	 */
	private void readSettings() {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String tPref = sharedPrefs.getString("prefRunningSpeed", null);
		runningSpeed = Integer.valueOf(tPref);

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

			currSpeed = 0;
			isRinging = false;
			wheelphone.setSpeed(currSpeed, currSpeed);

			//Set Alarm configuration fragment:
			getActionBar().setSelectedNavigationItem(0);

		}
	}


	/*
	 * Bar navigation code:
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		Fragment fragment = null;

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		switch(position) {
		case 0:
			if (fragmentSettings == null)
				fragmentSettings = new FragmentSettings();
			fragment = fragmentSettings;
			break;
		case 1:
			if (fragmentAlarm == null)
				fragmentAlarm = new FragmentAlarm();
			fragment = fragmentAlarm;
			break;
		}
		if (fragment != null){
			ft.replace(R.id.container, fragment);
			ft.commit();
		}
		return true;
	}

}
