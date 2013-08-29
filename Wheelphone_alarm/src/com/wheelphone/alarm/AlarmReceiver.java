package com.wheelphone.alarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver 
{    
	private static final String TAG = AlarmReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) 
	{   
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
		Log.d(TAG, "aquiring lock");
		wl.acquire();

		Intent startIntent = new Intent(context, ActivityMain.class);
		startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		startIntent.putExtra("start_alarm", true);
		
		wl.release();

		context.startActivity(startIntent);

		Log.d(TAG, "lock released");
	}
	
	public static void SetAlarm(Context context, Calendar c)
	{
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

		Toast.makeText(context, "Setting alarm for:" + AlarmPicker.alarmTimeFormat.format(c.getTime()), Toast.LENGTH_SHORT).show();

		Log.d(TAG, "Setting alarm for: " + timeFormat.format(c.getTime()));

		am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
	}

	public static void CancelAlarm(Context context)
	{
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
		
		Toast.makeText(context, "Cancelling alarm", Toast.LENGTH_SHORT).show();

		Log.d(TAG, "Cancelling alarm");
	}


}