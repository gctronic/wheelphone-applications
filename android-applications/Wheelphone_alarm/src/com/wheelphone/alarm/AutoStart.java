package com.wheelphone.alarm;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoStart extends BroadcastReceiver
{   
	private static final String TAG = AutoStart.class.getName();
	private String logString;
	private boolean debug = true;
	
    @Override
    public void onReceive(Context context, Intent intent)
    {   
		if(debug) {
			logString = TAG + ": onReceive()";
			Log.d(TAG, logString);
			ActivityMain.appendLog("debug.txt", logString, false);
		}
		
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
        	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);;
    		Long alarm = sharedPrefs.getLong("prefAlarmTime", 0);
    		
    		//If there is an alarm, set it!
    		if (alarm != 0){
    			Calendar c = Calendar.getInstance();
    			c.setTimeInMillis(alarm);
    			AlarmReceiver.SetAlarm(context, c);
    		}
        }
    }
}