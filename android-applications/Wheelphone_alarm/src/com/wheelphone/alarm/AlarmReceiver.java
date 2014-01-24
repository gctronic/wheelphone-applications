package com.wheelphone.alarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver 
{    
	private static final String TAG = AlarmReceiver.class.getName();
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
		
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		//PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ""); // only turn on CPU
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
		
		Log.d(TAG, "aquiring lock");
		wl.acquire();

		Intent startIntent = new Intent(context, ActivityMain.class);
		//startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
		startIntent.putExtra("start_alarm", true);
		
		wl.release();

		
//		final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
//		audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE); 
//		audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);		
//
//		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)  
//		        .setSmallIcon(R.drawable.wheelphone_icon)  
//		        .setContentTitle("Notifications Example")  
//		        .setContentText("This is a test notification");  
//
//		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, startIntent,   
//		        PendingIntent.FLAG_UPDATE_CURRENT);  
//
//		//builder.setContentIntent(contentIntent);
//		builder.setFullScreenIntent(contentIntent, true);
//		builder.setAutoCancel(true);
//		builder.setLights(Color.BLUE, 500, 500);
//		long[] pattern = {0, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500};
//		builder.setVibrate(pattern);
//		builder.setStyle(new NotificationCompat.InboxStyle());
//		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//	
//		builder.setSound(alarmSound); //, RingtoneManager.TYPE_ALARM);
//		// Add as notification  
//		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);  
//		manager.notify(1, builder.build());  		
					
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