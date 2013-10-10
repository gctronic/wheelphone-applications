package com.wheelphone.pet;

import android.os.Bundle;
import android.preference.PreferenceFragment;


public class FragmentPreferences extends PreferenceFragment {

	private static final String TAG = FragmentPreferences.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings);
	}
}
