package com.wheelphone.remotemini;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class FrontImageActivity extends Activity {

	public static ImageView img=null, logo=null;
	private static Field[] drawable = R.drawable.class.getFields();
	public static TextView txtTarget;
	public static String sTarget = "";
	public Timer timer;
	private String logString;
	private boolean debugUsbComm = false;
	private static String TAG = FrontImageActivity.class.getName();
	
	//tells activity to run on ui thread    
	class textTask extends TimerTask {          
		@Override        
		public void run() {             
			FrontImageActivity.this.runOnUiThread(new Runnable() {                  
				//@Override                 
				public void run() {
					txtTarget.setText(sTarget);
					txtTarget.setGravity(Gravity.CENTER);
				}             
			});         
		}    
	}; 		
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.front_image); 
        img = (ImageView) findViewById(R.id.imageView1);
        img.setBackgroundColor(Color.argb(255,255,255,255));
        logo = (ImageView) findViewById(R.id.imageViewLogo);
        logo.setBackgroundColor(Color.argb(255,255,255,255)); 
        txtTarget = (TextView)findViewById(R.id.text_target);
        txtTarget.setTextColor(Color.RED); 
        txtTarget.setText(sTarget);
        timer = new Timer();                                         
        timer.schedule(new textTask(), 0, 150);
        
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);        
    }
    
    public static void setFrontImage(int id) {
    	if(img==null) {
    		return;
    	}
    	try {
			img.setImageResource(drawable[id].getInt(null));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void setFrontImage(String name) {
    	if(img==null) {
    		return;
    	}
    	try {
			try {
				img.setImageResource(R.drawable.class.getField(name).getInt(null));
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }    
    
    public static void setTextTarget(String s) {
    	sTarget = s;
    }
    
    public void onStart() {
		if(debugUsbComm) {
			logString = TAG + ": onStart";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
    	super.onStart();
    }
    
    public void onStop() {
		if(debugUsbComm) {
			logString = TAG + ": onStop";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
    	super.onStop();
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