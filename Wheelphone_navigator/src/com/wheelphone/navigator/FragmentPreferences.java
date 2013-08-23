package com.wheelphone.navigator;

import java.util.ArrayList;
import java.util.List;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.wheelphone.R;


public class FragmentPreferences extends PreferenceFragment {

	private static final String TAG = FragmentPreferences.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings);
		populateCameras();
//		populateCameraResolutions();
	}

	private void populateCameras(){
		PackageManager pm = getActivity().getPackageManager();
		List <CharSequence>cameras = new ArrayList<CharSequence>(); 
		List <CharSequence>camerasIds = new ArrayList<CharSequence>(); 
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			cameras.add("Front camera");
			camerasIds.add(Integer.toString(Camera.CameraInfo.CAMERA_FACING_FRONT));
		}
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			cameras.add("Back camera");
			camerasIds.add(Integer.toString(Camera.CameraInfo.CAMERA_FACING_BACK));
		}

		CharSequence[] entries = cameras.toArray(new CharSequence[cameras.size()]);
		CharSequence[] entriesValues = camerasIds.toArray(new CharSequence[camerasIds.size()]);

		ListPreference lp = (ListPreference)findPreference("pref_camera");
		lp.setEntries(entries);
		lp.setEntryValues(entriesValues);
	}
	
	private void populateCameraResolutions(){
		Camera camera = Camera.open();
		Camera.Parameters params = camera.getParameters();
		 
		// Check what resolutions are supported by your camera
		List<Size> sizes = params.getSupportedPreviewSizes();
		 
		// Iterate through all available resolutions and choose one.
		// The chosen resolution will be stored in mSize.
//		Size mSize = null;
		for (Size size : sizes) {
		    Log.i(TAG, "Available resolution: "+size.width+"x"+size.height);
		}
//		 
//		Log.i(TAG, "Chosen resolution: "+mSize.width+" "+mSize.height);
//		params.setPictureSize(mSize.width, mSize.height);
//		camera.setParameters(params);
		camera.stopPreview();
		camera.release();
	}
}
