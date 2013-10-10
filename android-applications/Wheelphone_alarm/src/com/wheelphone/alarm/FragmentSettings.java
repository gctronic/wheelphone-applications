package com.wheelphone.alarm;

import java.util.Calendar;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FragmentSettings extends Fragment {
	private static final String TAG = FragmentSettings.class.getName();
	private static SharedPreferences sharedPrefs;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_settings, container, false);

		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

		WakeUpConfiguration fragment = new WakeUpConfiguration();
		fragmentTransaction.add(R.id.fragment_preferences, fragment);
		fragmentTransaction.commit();

		return v;
	}

	public void setQuickAlarm(View view) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.SECOND, 5);

		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putLong("prefAlarmTime", c.getTimeInMillis());
		editor.commit();
		AlarmReceiver.SetAlarm(getActivity(), c);
	}

	public void cancelAlarm(View view) {
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putLong("prefAlarmTime", 0);
		editor.commit();
		AlarmReceiver.CancelAlarm(getActivity());
	}

	public static class WakeUpConfiguration extends PreferenceFragment implements OnSharedPreferenceChangeListener{

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);

			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			//Set current setting values to the presented interface
			updatePreferenceSummary(sharedPrefs, "prefAlarmTime");
			updatePreferenceSummary(sharedPrefs, "prefRunningSpeed");	
		}

		protected void updateAlarmSummary(){
			updatePreferenceSummary(sharedPrefs, "prefAlarmTime");
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference p) {
			//Show TimePickerDialog if the prefAlarmTime was clicked 
			if (p.getKey().equals("prefAlarmTime")){
				DialogFragment newFragment = new AlarmPicker();
				newFragment.show(this.getFragmentManager(), "timePicker");
			}
			return super.onPreferenceTreeClick(preferenceScreen, p);
		}

		/*
		 * onSharedPreferenceChanged
		 * Handle when a preference has changed 
		 */
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
			updatePreferenceSummary(sharedPrefs, key);
		}

		/*
		 * Update the summary of a setting to reflect its current value.
		 */
		private void updatePreferenceSummary(SharedPreferences sharedPrefs,
				String key) {
			Log.d(TAG, key + " changed. Updating summary");

			Preference p = findPreference(key);
			if (p instanceof EditTextPreference) {
				p.setSummary(sharedPrefs.getString(key, ""));
			}
			if (key.equals("prefAlarmTime")){
				Long alarm = sharedPrefs.getLong("prefAlarmTime", 0);

				String alarmString;
				if (alarm == 0){
					alarmString = "not set";
				} else {
					Calendar newAlarm = Calendar.getInstance();
					newAlarm.setTimeInMillis(alarm);
					alarmString = AlarmPicker.alarmTimeFormat.format(newAlarm.getTime());
				}
				p.setSummary(alarmString); 
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}

	}

}
