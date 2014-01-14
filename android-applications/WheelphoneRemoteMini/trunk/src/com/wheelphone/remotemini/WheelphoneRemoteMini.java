/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.wheelphone.remotemini;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpStatus;

import com.qualcomm.QCAR.QCAR;
import com.wheelphone.remotemini.WheelphoneRemoteMini;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import net.majorkernelpanic.http.CustomHttpServer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class WheelphoneRemoteMini extends Activity implements OnSharedPreferenceChangeListener, WheelPhoneRobotListener {
    
	// Various
	private static String TAG = WheelphoneRemoteMini.class.getName();
	Timer timerImg = new Timer();
	boolean getFirmwareFlag = true;
	private int firmwareVersion=0;
	private SoundPool soundPool = new SoundPool(4,AudioManager.STREAM_MUSIC,0);
	private Field[] raws = R.raw.class.getFields();
	public static final int IMG_NORMAL = 0;	
	public static final int IMG_DRIVE = 1;
	public static final int IMG_BORED = 2;
	public static final int IMG_BORED_1 = 3;	
	public static final int IMG_BORED_2 = 4;
	public static final int IMG_BORED_3 = 5;
	public static final int IMG_SLEEP = 6;
	public static final int IMG_ANGRY = 7;	
	public int imgState = IMG_NORMAL; 
    public Intent intent;
    boolean frontImageActivityStarted = false;
	private String logString;
	private boolean debugUsbComm = false;
	
    // UI
	Button btnStart;		
    
	// Robot state
	WheelphoneRobot wheelphone;
	
	// target detection
    public int targetX=0, targetY=0;
    public int targetTimeout=0;
    public int isFollowingTarget = 0;
    
    //motion
	private int lSpeed=0, rSpeed=0;
	public static final int MIN_SPEED = -350;
	public static final int MAX_SPEED = 350;
	public static final int SPEED_STEP = 20;	//mm/s
	public static final int ROTATION_STEP = 10;
	public static final int MAX_SPEED_INDEX = MAX_SPEED/SPEED_STEP;
	public int currentSpeed=0;
	public int currentSpeedIndex=0;
	public int currentRotation=0;
	public int currentRotationIndex=0;
	public static final int NO_ROTATION = 0;
	public static final int ROTATE_LEFT = 1;
	public static final int ROTATE_RIGHT = 2;
	public int pivotType = NO_ROTATION;
	public int pivotCounter = 0;	
   
    // http server
    private CustomHttpServer httpServer = null;
    private PowerManager.WakeLock wl;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView line1, line2, version, signWifi, signStreaming;
    private LinearLayout signInformation;
    private Context context;
    private Animation pulseAnimation;

    // AR
    // Menu item string constants:
    private static final String MENU_ITEM_ACTIVATE_CONT_AUTO_FOCUS =
        "Activate Cont. Auto focus";
    private static final String MENU_ITEM_DEACTIVATE_CONT_AUTO_FOCUS =
        "Deactivate Cont. Auto focus";
    // Focus mode constants:
    private static final int FOCUS_MODE_NORMAL = 0;
    private static final int FOCUS_MODE_CONTINUOUS_AUTO = 1;
    // Application status constants:
    private static final int APPSTATUS_UNINITED         = -1;
    private static final int APPSTATUS_INIT_APP         = 0;
    private static final int APPSTATUS_INIT_QCAR        = 1;
    private static final int APPSTATUS_INIT_APP_AR      = 2;
    private static final int APPSTATUS_INIT_TRACKER     = 3;
    private static final int APPSTATUS_INITED           = 4;
    private static final int APPSTATUS_CAMERA_STOPPED   = 5;
    private static final int APPSTATUS_CAMERA_RUNNING   = 6;
    // Name of the native dynamic libraries to load:
    private static final String NATIVE_LIB_SAMPLE = "WheelphoneRemoteMini";
    private static final String NATIVE_LIB_QCAR = "QCAR";    
    // Display size of the device:
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    // The current application status:
    private int mAppStatus = APPSTATUS_UNINITED;
    // The async task to initialize the QCAR SDK:
    private InitQCARTask mInitQCARTask;
    // An object used for synchronizing QCAR initialization, dataset loading and
    // the Android onDestroy() life cycle event. If the application is destroyed
    // while a data set is still being loaded, then we wait for the loading:
    // operation to finish before shutting down QCAR.
    private Object mShutdownLock = new Object();
    // QCAR initialization flags:
    private int mQCARFlags = 0;    
    // The current focus mode selected:
    private int mFocusMode;
    /** Static initializer block to load native libraries on start-up. */
    static
    {
        loadLibrary(NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_SAMPLE);
    }
        
    /** An async task to initialize QCAR asynchronously. */
    private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value
        private int mProgressValue = -1;

        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mShutdownLock)
            {
                QCAR.setInitParameters(WheelphoneRemoteMini.this, mQCARFlags);

                do
                {
                    // QCAR.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%)
                    // If QCAR.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = QCAR.init();

                    // Publish the progress value:
                    publishProgress(mProgressValue);

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true))
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0
                         && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }


        protected void onPostExecute(Boolean result)
        {
            // Done initializing QCAR, proceed to next application
            // initialization status:
            if (result)
            {
                Log.d(TAG, "InitQCARTask::onPostExecute: QCAR " +
                              "initialization successful");

                updateApplicationStatus(APPSTATUS_INIT_TRACKER);
            }
            else
            {
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder(
                    WheelphoneRemoteMini.this
                ).create();

                dialogError.setButton
                (
                    DialogInterface.BUTTON_POSITIVE,
                    "Close",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Exiting application:
                            System.exit(1);
                        }
                    }
                );

                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED)
                {
                    logMessage = "Failed to initialize QCAR because this " +
                        "device is not supported.";
                }
                else
                {
                    logMessage = "Failed to initialize QCAR.";
                }

                // Log error:
                Log.e(TAG, "InitQCARTask::onPostExecute: " + logMessage +
                                " Exiting.");

                // Show dialog box with error message:
                dialogError.setMessage(logMessage);
                dialogError.show();
            }
        }
    }    
    
	//tells activity to run on ui thread    
	class frontImageStateTask extends TimerTask {          
		@Override        
		public void run() {             
			WheelphoneRemoteMini.this.runOnUiThread(new Runnable() {                  
				//@Override                 
				public void run() {
					switch(imgState) {
						case IMG_DRIVE:
							FrontImageActivity.setFrontImage("smiley_bored2");
							imgState = IMG_BORED;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds
							break;
							
						case IMG_BORED:
							FrontImageActivity.setFrontImage("smiley_annoyed");
							imgState = IMG_BORED_1;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds							
							break;
							
						case IMG_BORED_1:
							FrontImageActivity.setFrontImage("smiley_annoyed3");
							imgState = IMG_BORED_2;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds							
							break;
							
						case IMG_BORED_2:
							FrontImageActivity.setFrontImage("smiley_bored");
							imgState = IMG_BORED_3;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds							
							break;
							
						case IMG_BORED_3:
							FrontImageActivity.setFrontImage("smiley_yawn1");
							imgState = IMG_SLEEP;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds							
							break;
							
						case IMG_SLEEP:
							FrontImageActivity.setFrontImage("smiley_asleep");
							break;
							
						case IMG_ANGRY:
							FrontImageActivity.setFrontImage("smiley01_drive_car");
							imgState = IMG_DRIVE;
							timerImg.schedule(new frontImageStateTask(), 15000);	// after 15 seconds														
							break;
						
					}
				}             
			});         
		}    
	}; 	
	
	/** A helper for loading native libraries stored in "libs/armeabi*". */
    public static boolean loadLibrary(String nLibName)
    {
        try
        {
            System.loadLibrary(nLibName);
            Log.i(TAG, "Native library lib" + nLibName + ".so loaded");
            return true;
        }
        catch (UnsatisfiedLinkError ulee)
        {
        	Log.e(TAG, "The library lib" + nLibName +
                            ".so could not be loaded");
        }
        catch (SecurityException se)
        {
        	Log.e(TAG, "The library lib" + nLibName +
                            ".so was not allowed to be loaded");
        }

        return false;
    }	
	
    private void storeScreenDimensions() {
        // Query display dimensions:
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }    
    
    /** Configure QCAR with the desired version of OpenGL ES. */
    private int getInitializationFlags() {
        return QCAR.GL_20;
    }    
    
    /** Native tracker initialization and deinitialization. */
    public native int initTracker();
    public native void deinitTracker();
    
    /** Native methods for starting and stopping the camera. */
    private native void startCamera();
    private native void stopCamera();
    
    /** Native method for setting / updating the projection matrix for
     * AR content rendering */
    private native void setProjectionMatrix();    
    
    public void onConfigurationChanged(Configuration config) {
        Log.d(TAG, "FrameMarkers::onConfigurationChanged");
        super.onConfigurationChanged(config);

        storeScreenDimensions();

        // Set projection matrix:
        if (QCAR.isInitialized() && (mAppStatus == APPSTATUS_CAMERA_RUNNING))
            setProjectionMatrix();
    }    
    
    /** Native function to deinitialize the application.*/
    private native void deinitApplicationNative();    
    
    /** Native function to update the renderer. */
    public native void updateRendering(int width, int height);
    
    /** NOTE: this method is synchronized because of a potential concurrent
     * access by FrameMarkers::onResume() and InitQCARTask::onPostExecute(). */
    private synchronized void updateApplicationStatus(int appStatus)
    {
        // Exit if there is no change in status:
        if (mAppStatus == appStatus)
            return;

        // Store new status value
        mAppStatus = appStatus;

        // Execute application state-specific actions:
        switch (mAppStatus)
        {
            case APPSTATUS_INIT_APP:
                // Initialize application elements that do not rely on QCAR
                // initialization:
                initApplication();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_INIT_QCAR);
                break;

            case APPSTATUS_INIT_QCAR:
                // Initialize QCAR SDK asynchronously to avoid blocking the
                // main (UI) thread.
                // This task instance must be created and invoked on the UI
                // thread and it can be executed only once!
                try
                {
                    mInitQCARTask = new InitQCARTask();
                    mInitQCARTask.execute();
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Initializing QCAR SDK failed");
                }
                break;

            case APPSTATUS_INIT_TRACKER:

                // Initialize the marker tracker and create markers:
                if (initTracker() >= 0)
                {
                	Log.d(TAG, "Tracker initialized!\n");
                    // Proceed to next application initialization status:
                    updateApplicationStatus(APPSTATUS_INIT_APP_AR);
                }
                break;

            case APPSTATUS_INIT_APP_AR:
                // Initialize Augmented Reality-specific application elements
                // that may rely on the fact that the QCAR SDK has been
                // already initialized:
                initApplicationAR();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_INITED);
                break;

            case APPSTATUS_INITED:
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

//                // The elapsed time since the splash screen was visible:
//                long splashScreenTime = System.currentTimeMillis() -
//                                            mSplashScreenStartTime;
//                long newSplashScreenTime = 0;
//                if (splashScreenTime < MIN_SPLASH_SCREEN_TIME)
//                {
//                    newSplashScreenTime = MIN_SPLASH_SCREEN_TIME -
//                                            splashScreenTime;
//                }
//
//                // Request a callback function after a given timeout to dismiss
//                // the splash screen:
//                mSplashScreenHandler = new Handler();
//                mSplashScreenRunnable =
//                    new Runnable() {
//                        public void run()
//                        {
//                            // Hide the splash screen:
//                            mSplashScreenView.setVisibility(View.INVISIBLE);
//
//                            // Activate the renderer:
//                            mRenderer.mIsActive = true;
//
//                            // Now add the GL surface view. It is important
//                            // that the OpenGL ES surface view gets added
//                            // BEFORE the camera is started and video
//                            // background is configured.
//                            addContentView(mGlView, new LayoutParams(
//                                            LayoutParams.MATCH_PARENT,
//                                            LayoutParams.MATCH_PARENT));
//
//                            // Start the camera:
                            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
//                        }
//                };
//
//                mSplashScreenHandler.postDelayed(mSplashScreenRunnable,
//                                                    newSplashScreenTime);
                break;

            case APPSTATUS_CAMERA_STOPPED:
                // Call the native function to stop the camera:
                stopCamera();
                break;

            case APPSTATUS_CAMERA_RUNNING:
                // Call the native function to start the camera:
                startCamera();
                setProjectionMatrix();

                // Set continuous auto-focus if supported by the device,
                // otherwise default back to regular auto-focus mode.
                // This will be activated by a tap to the screen in this
                // application.
                mFocusMode = FOCUS_MODE_CONTINUOUS_AUTO;
                if(!setFocusMode(mFocusMode))
                {
                    mFocusMode = FOCUS_MODE_NORMAL;
                    setFocusMode(mFocusMode);
                }

                break;

            default:
                throw new RuntimeException("Invalid application state");
        }
    }    
    
    /** Tells native code whether we are in portait or landscape mode */
    private native void setActivityPortraitMode(boolean isPortrait);


    /** Initialize application GUI elements that are not related to AR. */
    private void initApplication()
    {
        // Set the screen orientation:
        int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        // Apply screen orientation:
        setRequestedOrientation(screenOrientation);

        // Pass on screen orientation info to native code:
        setActivityPortraitMode(
            screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        storeScreenDimensions();

        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright.
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    /** Native function to initialize the application. */
    private native void initApplicationNative(int width, int height);


    /** Initializes AR application components. */
    private void initApplicationAR()
    {
        // Do application initialization in native code (e.g. registering
        // callbacks, etc.)
        initApplicationNative(mScreenWidth, mScreenHeight);

    }


    /** Invoked every time before the options menu gets displayed to give
     *  the Activity a chance to populate its Menu with menu items. */
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        super.onPrepareOptionsMenu(menu);
        
//        menu.clear();
//
//        if(mFocusMode == FOCUS_MODE_CONTINUOUS_AUTO)
//            menu.add(MENU_ITEM_DEACTIVATE_CONT_AUTO_FOCUS);
//        else
//            menu.add(MENU_ITEM_ACTIVATE_CONT_AUTO_FOCUS);

        return true;
    }

   private native boolean autofocus();
   private native boolean setFocusMode(int mode);    
   /** The native render function. */
   public native void getTrackInfo();    
    
   public void displayMessageInt2(int i1, int i2) {
       // We use a handler because this thread cannot change the UI
       Message message = new Message();
       message.obj = String.valueOf(i1) + ", " + String.valueOf(i2);
       Log.d(TAG, "x=" + i1 + ", y=" + i2 + "\n");
       targetX = i1;
       targetY = i2;
       FrontImageActivity.setTextTarget("Target detected at (" + String.valueOf(i1) + ", " + String.valueOf(i2) + ")");
       //mainActivityHandler.sendMessage(message);
       targetTimeout = 0;
       isFollowingTarget = 1;
   }    
   
    public void onCreate(Bundle savedInstanceState) {
		if(debugUsbComm) {
			logString = TAG + ": onCreate";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}
		
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        context = this.getApplicationContext();
        line1 = (TextView)findViewById(R.id.line1);
        line2 = (TextView)findViewById(R.id.line2);
        version = (TextView)findViewById(R.id.version);
        signWifi = (TextView)findViewById(R.id.advice);
        signStreaming = (TextView)findViewById(R.id.streaming);
        signInformation = (LinearLayout)findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "com.wheelphone.remotemini.wakelock");
           
        httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);       
         
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				soundPool.play(sampleId, 0.99f, 0.99f, 1, 0, 1);
			}
		});
		
        timerImg = new Timer();
        
        intent = new Intent(context, FrontImageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(intent);        
        
        // Query the QCAR initialization flags:
        mQCARFlags = getInitializationFlags();

        // Update the application status to start initializing application
        updateApplicationStatus(APPSTATUS_INIT_APP);        
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        updateRendering(size.x, size.y);
        
		btnStart = (Button)findViewById(R.id.btnStart);
        
		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);		
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();  
        wheelphone.setWheelPhoneRobotListener(this);
		
    }
    
    public void onStart() {
		if(debugUsbComm) {
			logString = TAG + ": onStart";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}	
    	super.onStart();
    	   	
    	// Lock screen
    	wl.acquire();
    	
    	Intent notificationIntent = new Intent(this, WheelphoneRemoteMini.class);
    	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setContentIntent(pendingIntent)
    	        .setWhen(System.currentTimeMillis())
    	        .setTicker(getText(R.string.notification_title))
    	        .setSmallIcon(R.drawable.wheelphone_logo_remote_mini_small)
    	        .setContentTitle(getText(R.string.notification_title))
    	        .setContentText(getText(R.string.notification_content));
    	Notification notification = builder.getNotification();
    	notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
    	
    }
    	
    public void onStop() {
		if(debugUsbComm) {
			logString = TAG + ": onStop";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}  
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
		if(debugUsbComm) {
			logString = TAG + ": onResume";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}   
    	super.onResume();
    	
    	// when FrontImageActivity is started and the back button is pressed then this "onResume"
    	// is called again and we don't want to restart twice the communication. Basically only at the 
    	// first call to "onStart" we start the USB communication.    	
    	if(!frontImageActivityStarted) { 
    		wheelphone.startUSBCommunication();        		
    	} 	    	
    	

    	// Determines if user is connected to a wireless network & displays ip 
    	if (!streaming) displayIpAddress();
    	startServers();
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        // QCAR-specific resume operation:
        QCAR.onResume();

        // We may start the camera only if the QCAR SDK has already been
        // initialized:
        if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
        }                
        
    }
    
    public void onPause() {
		if(debugUsbComm) {
			logString = TAG + ": onPause";
			Log.d(TAG, logString);
			appendLog("debugUsbComm.txt", logString, false);
		}     	
    	super.onPause();
    	
    	// do not close the communication here because otherwise when "FrontImageActivity" is 
    	// started then the communication doens't work anymore!
    	//wheelphone.closeUSBCommunication();  
    	
    	CustomHttpServer.setScreenState(false);
    	unregisterReceiver(wifiStateReceiver);
    	    	
//        if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
//        {
//            updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
//        }
//
//        // QCAR-specific pause operation:
//        QCAR.onPause();
        
    }
        
    public void onDestroy() {
    	super.onDestroy();
    	// Remove notification
    	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    	if (httpServer != null) httpServer.stop();
    	
        // Cancel potentially running tasks:
        if (mInitQCARTask != null &&
            mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            mInitQCARTask.cancel(true);
            mInitQCARTask = null;
        }

        // Ensure that the asynchronous operations to initialize QCAR does
        // not overlap:
        synchronized (mShutdownLock) {

            // Do application deinitialization in native code:
            deinitApplicationNative();

            // Deinit the tracker:
            deinitTracker();

            // Deinitialize QCAR SDK:
            QCAR.deinit();
        }
        
        System.gc();
        
    }
    
    public void onBackPressed() {
    	Intent setIntent = new Intent(Intent.ACTION_MAIN);
    	setIntent.addCategory(Intent.CATEGORY_HOME);
    	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(setIntent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.quit:
	        	// Quits Spydroid i.e. stops the HTTP server
	        	if (httpServer != null) httpServer.stop();
	            wheelphone.closeUSBCommunication();
	        	finish();	
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
    private void startServers() {
    	if (httpServer != null) {
    		CustomHttpServer.setScreenState(true);
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		if (!streaming) displayIpAddress();
        	}
        } 
    };
    
    private boolean streaming = false;
    
    // The Handler that gets information back from the RtspServer and Session
    private final Handler handler = new Handler() {

    	public void handleMessage(Message msg) { 
    		
    		switch (msg.what) {
    		case CustomHttpServer.MOVE_FORWARD:
    			if(frontImageActivityStarted) {
    				FrontImageActivity.setFrontImage("smiley01_drive_car");
    				imgState = IMG_DRIVE; 
    				timerImg.cancel();
    				timerImg = new Timer();
    				timerImg.schedule(new frontImageStateTask(), 15000);
    			}
    			try {    	
    				if(currentSpeedIndex == 0) {
    					soundPool.load(context, raws[2].getInt(null), 0);
    				}  		
    			} catch (Exception e) {
					Log.e(TAG,"Error !");
					e.printStackTrace();
				}    			
    			if(currentRotationIndex == 0) {
    				if(currentSpeedIndex < MAX_SPEED_INDEX) {
    					currentSpeedIndex++;
    				} else {
    					currentSpeedIndex = MAX_SPEED_INDEX;
    				}    				
    			} else {
    				currentRotationIndex = 0;
    			}
    			pivotCounter = 0;
    			pivotType = NO_ROTATION;    			
    			break;    			
    		case CustomHttpServer.MOVE_BACKWARD:
    			if(frontImageActivityStarted) {
    				FrontImageActivity.setFrontImage("smiley01_drive_car");
    				imgState = IMG_DRIVE; 
    				timerImg.cancel();
    				timerImg = new Timer();
    				timerImg.schedule(new frontImageStateTask(), 15000);
    			}
    			try {    	
    				if(currentSpeedIndex == 0) {
    					soundPool.load(context, raws[5].getInt(null), 0);
    				}  		
    			} catch (Exception e) {
					Log.e(TAG,"Error !");
					e.printStackTrace();
				}    			
    			if(currentRotationIndex == 0) {
    				if(currentSpeedIndex > -MAX_SPEED_INDEX) {
    					currentSpeedIndex--;
    				} else {
    					currentSpeedIndex = -MAX_SPEED_INDEX;
    				}    				
    			} else {
    				currentRotationIndex = 0;
    			}
    			pivotCounter = 0;
    			pivotType = NO_ROTATION;    			
    			break;    			    			
    		case CustomHttpServer.MOVE_LEFT:
    			if(frontImageActivityStarted) {
    				FrontImageActivity.setFrontImage("smiley01_drive_car");
    				imgState = IMG_DRIVE; 
    				timerImg.cancel();
    				timerImg = new Timer();
    				timerImg.schedule(new frontImageStateTask(), 15000);
    			}
    			try {    	
    				if(currentSpeedIndex >= 3 && Math.abs(currentRotationIndex)>=3) {
    					soundPool.load(context, raws[4].getInt(null), 0);
    				}  		
    			} catch (Exception e) {
					Log.e(TAG,"Error !");
					e.printStackTrace();
				}    			
    			if(currentSpeedIndex == 0) {    				
        			pivotType = ROTATE_LEFT;
        			pivotCounter += 9;	// (should be 7 theoretically) 45 degrees CCW with speed l=-50, r=15  			
    			} else {    			
	    			if(Math.abs(currentSpeedIndex) < 3) {
	    				if(currentRotationIndex < MAX_SPEED_INDEX) {
	    					currentRotationIndex++;
	    				} else {
	    					currentRotationIndex = MAX_SPEED_INDEX;
	    				}
	    			} else {
	    				if(currentSpeedIndex >= 0) {    				
				    		if(currentRotationIndex < (MAX_SPEED_INDEX-(currentSpeedIndex/3))) {
				    			currentRotationIndex += currentSpeedIndex/3;
				    		} else {
				    			currentRotationIndex = MAX_SPEED_INDEX;
				    		}		
	    				} else {
				    		if(currentRotationIndex < (MAX_SPEED_INDEX+(currentSpeedIndex/3))) {
				    			currentRotationIndex -= currentSpeedIndex/3;
				    		} else {
				    			currentRotationIndex = MAX_SPEED_INDEX;
				    		}    					
	    				}
	    			}
    			}
    			break;
    		case CustomHttpServer.MOVE_RIGHT:
    			if(frontImageActivityStarted) {
    				FrontImageActivity.setFrontImage("smiley01_drive_car");
    				imgState = IMG_DRIVE; 
    				timerImg.cancel();
    				timerImg = new Timer();
    				timerImg.schedule(new frontImageStateTask(), 15000);
    			}
    			try {    	
    				if(currentSpeedIndex >= 3 && Math.abs(currentRotationIndex)>=3) {
    					soundPool.load(context, raws[4].getInt(null), 0);
    				}  		
    			} catch (Exception e) {
					Log.e(TAG,"Error !");
					e.printStackTrace();
				}    	    			
    			if(currentSpeedIndex == 0) {    				
        			pivotType = ROTATE_RIGHT;
        			pivotCounter -= 9;	// 45 degrees CW with speed l=15, r=-50     			    				    				
    			} else {    			
	    			if(Math.abs(currentSpeedIndex) < 3) {
			    		if(currentRotationIndex > -MAX_SPEED_INDEX) {
			    			currentRotationIndex--;
			    		} else {
			    			currentRotationIndex = -MAX_SPEED_INDEX;
			    		}    				
	    			} else {  	
	    				if(currentSpeedIndex >= 0) {   
				    		if(currentRotationIndex > (-MAX_SPEED_INDEX+(currentSpeedIndex/3))) {
				    			currentRotationIndex -= currentSpeedIndex/3;
				    		} else {
				    			currentRotationIndex = -MAX_SPEED_INDEX;
				    		}  
	    				} else {
				    		if(currentRotationIndex > (-MAX_SPEED_INDEX-(currentSpeedIndex/3))) {
				    			currentRotationIndex += currentSpeedIndex/3;
				    		} else {
				    			currentRotationIndex = -MAX_SPEED_INDEX;
				    		}      					
	    				}
	    			}
    			}
    			break;
    		case CustomHttpServer.STOP:
    			if(frontImageActivityStarted) {
    				FrontImageActivity.setFrontImage("smiley01_drive_car");
    				imgState = IMG_DRIVE; 
    				timerImg.cancel();
    				timerImg = new Timer();
    				timerImg.schedule(new frontImageStateTask(), 15000);
    			}
    			try {    	
    				if(currentSpeedIndex >= 0) {
    					soundPool.load(context, raws[1].getInt(null), 0);
    				}  		
    			} catch (Exception e) {
					Log.e(TAG,"Error !");
					e.printStackTrace();
				}
    			currentSpeedIndex = 0;
    			currentRotationIndex = 0;	
    			pivotCounter = 0;
    			pivotType = NO_ROTATION;    			
    			break;
    		case CustomHttpServer.ENABLE_OBSTACLE_AVOIDANCE:
    			wheelphone.enableObstacleAvoidance();
    			break;
    		case CustomHttpServer.DISABLE_OBSTACLE_AVOIDANCE:
    			wheelphone.disableObstacleAvoidance();
    			break;
    		case CustomHttpServer.ENABLE_CLIFF_AVOIDANCE:
    			wheelphone.enableCliffAvoidance();
    			break;
    		case CustomHttpServer.DISABLE_CLIFF_AVOIDANCE:
    			wheelphone.disableCliffAvoidance();
    			break;    			
   				
			default:
				break;
				
    		}//switch
			
			if(currentSpeedIndex >= 0) {	// forward direction
	    		rSpeed = currentSpeedIndex + currentRotationIndex;
	    		lSpeed = currentSpeedIndex - currentRotationIndex;
			} else {	// backward direction
	    		rSpeed = currentSpeedIndex - currentRotationIndex;	
	    		lSpeed = currentSpeedIndex + currentRotationIndex;
			}
    		
			// handle speed measure in different ways based on positive velocity (speed controller) 
			// and negative velocity (no speed controller)
//			if(rSpeed >= 0) {
				rSpeed *= SPEED_STEP;
//			} else {				
//				if(rSpeed == -1) {
//					rSpeed = -30;
//				} else if(rSpeed == -2) {
//					rSpeed = -50;
//				} else if(rSpeed == -3) {
//					rSpeed = -65;
//				} else if(rSpeed == -4) {
//					rSpeed = -70;
//				} else if(rSpeed == -5) {
//					rSpeed = -80;
//				} else if(rSpeed == -6) {
//					rSpeed = -90;
//				} else if(rSpeed == -7) {
//					rSpeed = -100;
//				} else if(rSpeed == -8) {
//					rSpeed = -110;
//				} else if(rSpeed == -9) {
//					rSpeed = -120;
//				} else {
//					rSpeed = -MAX_SPEED;
//				}
//			}
//			if(lSpeed >= 0) {
				lSpeed *= SPEED_STEP;
//			} else {								
//				if(lSpeed == -1) {
//					lSpeed = -30;
//				} else if(lSpeed == -2) {
//					lSpeed = -50;
//				} else if(lSpeed == -3) {
//					lSpeed = -65;
//				} else if(lSpeed == -4) {
//					lSpeed = -70;
//				} else if(lSpeed == -5) {
//					lSpeed = -80;
//				} else if(lSpeed == -6) {
//					lSpeed = -90;
//				} else if(lSpeed == -7) {
//					lSpeed = -100;
//				} else if(lSpeed == -8) {
//					lSpeed = -110;
//				} else if(lSpeed == -9) {
//					lSpeed = -120;
//				} else {
//					lSpeed = -MAX_SPEED;
//				}
//			}
			
			if(rSpeed > MAX_SPEED) {
				rSpeed = MAX_SPEED;
			}
			if(rSpeed < -MAX_SPEED) {
				rSpeed = -MAX_SPEED;
			}
			if(lSpeed > MAX_SPEED) {
				lSpeed = MAX_SPEED;
			}
			if(lSpeed < -MAX_SPEED) {
				lSpeed = -MAX_SPEED;
			}
			
			wheelphone.setLeftSpeed(lSpeed);
			wheelphone.setRightSpeed(rSpeed);
			
    	} //handle_message
    	
    };
    
    public void startAnimation(View view) {
        startActivity(intent);             
        frontImageActivityStarted = true;
    }
    
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	String ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	line1.setText("HTTP://");
	    	line1.append(ip);
	    	line1.append(":8080");
	    	line2.setText("RTSP://");
	    	line2.append(ip);
	    	line2.append(":8086");
	    	streamingState(0);
    	} else {
    		line1.setText("HTTP://xxx.xxx.xxx.xxx:8080");
    		line2.setText("RTSP://xxx.xxx.xxx.xxx:8086");
    		streamingState(2);
    	}
    }
    
    public void log(String s) {
    	Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

	private void streamingState(int state) {
		// Not streaming
		if (state==0) {
			signStreaming.clearAnimation();
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.VISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.VISIBLE);
			signStreaming.startAnimation(pulseAnimation);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			signStreaming.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.VISIBLE);
			signWifi.startAnimation(pulseAnimation);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onWheelphoneUpdate() {
		
		//Toast.makeText(WheelphoneRemoteMini.this, "target Task is running", Toast.LENGTH_SHORT).show();

		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneRemoteMini.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}		
		
		getTrackInfo(); 
		targetTimeout++;
		if(targetTimeout >= 2) {
			if(frontImageActivityStarted) {
				FrontImageActivity.setTextTarget("");
			}
			isFollowingTarget = 0;
			if(targetTimeout==2) {	// stop rotating when target lost
				lSpeed = 0;
				rSpeed = 0;
				pivotType = NO_ROTATION;
			}
		}					

		if(isFollowingTarget==1) {
			if(frontImageActivityStarted) {
				FrontImageActivity.setFrontImage("smiley01_drive_car");
				imgState = IMG_DRIVE; 
				timerImg.cancel();
				timerImg = new Timer();
				timerImg.schedule(new frontImageStateTask(), 15000);
			}			
			if(targetX > (mScreenWidth/2)+20) {
				lSpeed = 10;
				rSpeed = -10;
			} else if(targetX < (mScreenWidth/2)-20) {
				lSpeed = -10;
				rSpeed = 10;
			} else {
				lSpeed = 0;
				rSpeed = 0;
			}
		}
			
		if(pivotType != NO_ROTATION) {
			
			if(pivotCounter == 0) {
				
				lSpeed = 0;
				rSpeed = 0;
				pivotType = NO_ROTATION;
					
			} else {
				
				if(pivotCounter > 0) {
					pivotCounter--;
					lSpeed = -25;
					rSpeed = 25;
				} else {
					pivotCounter++;
					lSpeed = 25;
					rSpeed = -25;
				}
				
			}
																		
		}
		
		wheelphone.setLeftSpeed(lSpeed);
		wheelphone.setRightSpeed(rSpeed);
		
		try {    	
			if(wheelphone.getFrontProx(0) >= 180 || wheelphone.getFrontProx(1) >= 180 || wheelphone.getFrontProx(2) >= 180 || wheelphone.getFrontProx(3) >= 180) {
				soundPool.load(context, raws[0].getInt(null), 0);
				if(frontImageActivityStarted) {
					FrontImageActivity.setFrontImage("smiley_angry3");
					imgState = IMG_ANGRY; 
					timerImg.cancel();
					timerImg = new Timer();
					timerImg.schedule(new frontImageStateTask(), 5000);
				}
			}  						    				
		} catch (Exception e) {
			Log.e(TAG,"Error !");
			e.printStackTrace();
		}
		if(wheelphone.getGroundProx(0)<50 && wheelphone.getGroundProx(1)<50 && wheelphone.getGroundProx(2)<50 && wheelphone.getGroundProx(3)<50) {
			if(frontImageActivityStarted) {
				FrontImageActivity.setFrontImage("smiley_afraid3");
			}
		} else {
//			switch(imgState) {
//				case IMG_NORMAL:
//					FrontImageActivity.setFrontImage("smiley00_normal");
//					break;
//				case IMG_DRIVE:
//					FrontImageActivity.setFrontImage("smiley01_drive_car");
//					break;
//				case IMG_BORED:
//					FrontImageActivity.setFrontImage("smiley_bored2");
//					break;
//				case IMG_BORED_1:
//					FrontImageActivity.setFrontImage("smiley_annoyed");
//					break;
//				case IMG_BORED_2:
//					FrontImageActivity.setFrontImage("smiley_annoyed3");
//					break;
//				case IMG_BORED_3:
//					FrontImageActivity.setFrontImage("smiley_bored");
//					break;
//				case IMG_SLEEP:
//					FrontImageActivity.setFrontImage("smiley_sleep");
//					break;
//				case IMG_ANGRY:
//					FrontImageActivity.setFrontImage("smiley_angry3");
//					break;										
//			}									
		}
		
	}
    
    public void msgbox(String title,String msg) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);                      
        dlgAlert.setTitle(title); 
        dlgAlert.setMessage(msg); 
        dlgAlert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                 //finish(); 
            }
       });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
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