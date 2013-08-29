package com.wheelphone.alarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TimePicker;

public class AlarmPicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
	private static final String TAG = AlarmPicker.class.getName();
	
	protected static SimpleDateFormat alarmTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current time as the default values for the alarm picker
		final Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);

		// Create a new instance of TimePickerDialog and return it
		return new TimePickerDialog(getActivity(), this, hour, minute, true);
	}

	@Override
	public void onTimeSet(TimePicker view, int h, int m) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		Long oldAlarm = sharedPrefs.getLong("prefAlarmTime", 0);
		
		Calendar newAlarm = Calendar.getInstance();
		newAlarm.set(Calendar.MILLISECOND, 0);
		newAlarm.set(Calendar.SECOND, 0);
		newAlarm.set(Calendar.MINUTE, m);
		newAlarm.set(Calendar.HOUR_OF_DAY, h);

		Log.d(TAG, "old: " + oldAlarm);
		Log.d(TAG, "new: " + newAlarm.getTimeInMillis());

		//No old alarm or old alarm different from new alarm, then update the alarm
		if (oldAlarm == 0 || oldAlarm != newAlarm.getTimeInMillis()){

			//If alarm time is before current time, add a day
			if (newAlarm.before(Calendar.getInstance()))
				newAlarm.add(Calendar.DATE, 1);

			//Modify the preferences
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putLong("prefAlarmTime", newAlarm.getTimeInMillis());
			editor.commit();
			
			AlarmReceiver.SetAlarm(getActivity(), newAlarm);

			Log.d(TAG, "Time dialog changed. Changed prefAlarmTime to: " + alarmTimeFormat.format(newAlarm.getTime()));
		}
	}
	

}