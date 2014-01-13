package com.wheelphone.navigator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.wheelphone.R;


public class FragmentPreferences extends PreferenceFragment {

	private static final String TAG = FragmentPreferences.class.getName();

	private String logString;
	private boolean debugUsbComm = true;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(debugUsbComm) {
			logString = TAG + ": onCreate";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings);
		
	}
	
	@Override
	public void onStart() {
		if(debugUsbComm) {
			logString = TAG + ": onStart";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
		super.onStart();
	}
	
	@Override
	public void onStop() {
		if(debugUsbComm) {
			logString = TAG + ": onStop";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
		super.onStop();
	}

	@Override
	public void onResume() {
		if(debugUsbComm) {
			logString = TAG + ": onResume";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}	
		super.onResume();
	}
	
	@Override
	public void onPause() {		
		if(debugUsbComm) {
			logString = TAG + ": onPause";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		} 		
		super.onPause();
	}
	
	void appendLog(String fileName, String text, boolean clearFile)
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
