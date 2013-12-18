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

package com.wheelphone.targetDocking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import com.qualcomm.QCAR.QCAR;
import com.wheelphone.targetDocking.WheelphoneTargetDocking;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView.OnEditorActionListener;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class WheelphoneTargetDocking extends Activity implements OnSharedPreferenceChangeListener,  TextToSpeech.OnInitListener, WheelPhoneRobotListener {
    
	// Various
	private static String TAG = "Wheelphone";
	boolean getFirmwareFlag = true;
	private TextToSpeech tts;
	private PowerManager.WakeLock wl;
	private int instableChargeCounter=0;
	private int lSpeedTemp=0, rSpeedTemp=0;
	public long startTime=0, endTime=0;
	public int camOffset = 0;
	public boolean takeScreenshot=false;
	private int mCurrentTab = 0;
	private boolean mContinuousMode = true;
	String logString;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private int firmwareVersion=0;
	private double mOdomX=0, mOdomY=0, mOdomTheta=0;
	
	// target detection
	public static final int NUM_TARGETS = 4;	// 2 for docking station + 2 target points
	private int mCurrentScreenshot = 2;
	public static final int markerSize = 50;
	public static final int MIN_SPEED = -350;
	public static final int MAX_SPEED = 350;
	public int[] targetX = new int[NUM_TARGETS];		// x coordinate on the screen of the target
	public int[] lastTargetX = new int[NUM_TARGETS];
	public int[] targetY = new int[NUM_TARGETS];
	public float[] targetOrientation = new float[NUM_TARGETS];	// orientation of the robot/camera
	public float[] targetDist = new float[NUM_TARGETS];
	public float[] targetDistPrev = new float[NUM_TARGETS];
	public boolean[] switchToLittleTag = new boolean[NUM_TARGETS];	// all minus docking tags
	public static final int DIST_DIFF_THR = 200;
	public float[] targetPoseX = new float[NUM_TARGETS];	// x coordinate (position) of the target with respect to the camera frame
	public float[] targetPoseZ = new float[NUM_TARGETS];
	public float[] angleTargetRobot = new float[NUM_TARGETS];	// angle between the robot and the target, independent from robot orientation
	public static final int NO_INFO = 0;    
	public static final int NO_TARGET_FOUND = 1;
	public static final int TARGET_FOUND = 2;	
	public int[] targetDetectedInfo = new int[NUM_TARGETS];
	private double mCoordCoeff = 0.07;
	private double mOrientCoeff = 0.36;
	public static final int BASE_SPEED = 80; //20;
	private int baseSpeed = BASE_SPEED;
	private int speedFactor = 0;
	private int rotationFactor = 0;
	private int speedLimit = 20;
	private static final int ROT_SPEED = 160;
	public static final int STATE_WAITING_START = 1;
	public static final int STATE_DOCKING_SEARCH = 2;
	public static final int STATE_DOCKING_APPROACH_BIG = 3;
	public static final int STATE_DOCKING_APPROACH_SMALL = 4;
	public static final int STATE_DOCKING_REACHED = 5;
	public static final int STATE_ROBOT_CHARGING = 6;
	public static final int STATE_ROBOT_GO_BACK_DOCKING = 7;
	public static final int STATE_ROBOT_GO_FORWARD = 8;
	public static final int STATE_TARGET_SEARCH_AND_APPROACH = 9;
	public static final int STATE_ROBOT_GO_BACK_TARGET = 10;
	public static final int STATE_ROTATE_FOR_SCREENSHOT = 11;
	public static final int STATE_WAIT_STOPPED_FOR_SCREENSHOT = 12;
	public static final int STATE_TAKE_SCREENSHOT = 13;
	public short globalState=STATE_TARGET_SEARCH_AND_APPROACH;
	public short prevGlobalState=STATE_TARGET_SEARCH_AND_APPROACH;
	public static final int ROBOT_NOT_CHARGING = 0;
	public static final int ROBOT_IN_CHARGE = 1;
	public static final int ROBOT_CHARGED = 2;
	public int chargeStatus = ROBOT_NOT_CHARGING;
	public int chargeCounter = 0;
	public boolean invertRotation= true;
	public int rotCounter = 0;	
	public int targetTimesInScreen = 1;
	public static final int ROT_LEFT = 0;
	public static final int ROT_RIGHT = 1;
	public int lastRotation = ROT_LEFT;
	public int dockReachedTimeout = 0;	
	private double mDesiredRotation;			// Desired rotation should be a value between -1 and 1
	private int mDesiredAcceleration;			// between -1 and 1
	private volatile boolean mIsExploring;		// true when no targets detected
	private boolean mIsAvoiding;
	private double mObstacleDesiredRotation;	// rotation and acceleration resulting from the proximity sensors (obstacle avoidance)
	private double mObstacleDesiredAcceleration;
	private long mObstacleLastSeenTimestamp;
	private long mTimestampLastSeen;
	private long mTimestampDocking;
	private static final long DOCK_TIMEOUT = 40000;	// 30 seconds  
	private static final long TARGET_TIMEOUT = 30000;	// 10 seconds
	// Scaling factor for each spring. Since the spring values come between -1 and 1, this value defines the maximum contribution by each spring
	private static final int LINEAR_SPRING_CONST = 80; //12;
	private static final int ANGULAR_SPRING_CONST = 16; //8;
	private static final double DUMPING_FACTOR = 1;
	private int mLeftSpeed;
	private int mRightSpeed;
	private int mCurrentTargetId=2;
	private int mStopDistance=250;	// distance (in scene units) used to know when we are near a target point and we want to pass to the next
	private int mStopDistanceTemp=250;
	private int mChargeTime=20;		// given in seconds (base time is usb communication of 50 ms)
	private int mLastDetectedMarkerId=0;
	private static final long GLOBAL_TIMEOUT = 120000;	// 2 minutes
	private long mGlobalTimestamp=0;
	private boolean mSeenOnce = false;
	
    // http server
    private CustomHttpServer httpServer = null;
    private Context context;

	// UI
	TabSpec spec1, spec2, spec3;
	TabHost tabHost;
	TextView txtX0, txtY0, txtd0, txtOrient0;   
	private TextView batteryValue;
	private TextView leftSpeed, rightSpeed;
	private TextView txtConnected;
	FloorPlanView fpv;
	private EditText txtRoomWidth, txtRoomLength;
	private EditText txtChargeTime, txtStopDistance;
	private TextView robotX, robotY, robotTheta;
	private EditText txtK1, txtK2;
    
	// map
	int roomWidth, roomLength;
	
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
    private static final String NATIVE_LIB_SAMPLE = "WheelphoneTargetDocking";
    private static final String NATIVE_LIB_QCAR = "QCAR";   
    // Our OpenGL view:
    private QCARSampleGLView mGlView;
    // Our renderer:
    private FrameMarkersRenderer mRenderer;
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
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    // The current focus mode selected:
    private int mFocusMode;
    /** Static initializer block to load native libraries on start-up. */
    static
    {
        loadLibrary(NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_SAMPLE);
    }

    // testing
	//tells activity to run on ui thread    
    Timer timer = new Timer();
	class trackerTask extends TimerTask {          
		@Override        
		public void run() {             
			WheelphoneTargetDocking.this.runOnUiThread(new Runnable() {                  
				//@Override                 
				public void run() {
					getTrackInfo();
					txtd0.setText(String.valueOf(String.format("%.4f", targetOrientation[0]))); //targetDist[0])));
					txtX0.setText(String.valueOf(targetPoseX[0])); //targetX[0]));
					txtY0.setText(String.valueOf(targetPoseZ[0])); //targetY[0]));
					txtOrient0.setText(String.valueOf(String.format("%.4f", angleTargetRobot[0]))); //targetOrientation[0])));
					float currentAbsoluteOrientation = (0 + (int)targetOrientation[1] + 180)%360; // ID + orientation + 180
					robotX.setText(String.valueOf(0+(int)(Math.cos(targetOrientation[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0)));
					robotY.setText(String.valueOf(100+(int)(Math.sin(targetOrientation[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0)));
					robotTheta.setText(String.valueOf((int)currentAbsoluteOrientation));
					fpv.updateRobotInfo(0+(int)(Math.cos(angleTargetRobot[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0), 100-(int)(Math.sin(angleTargetRobot[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0), (int)currentAbsoluteOrientation);
				}
			});         
		}    
	}; 	
    
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
                QCAR.setInitParameters(WheelphoneTargetDocking.this, mQCARFlags);

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
                    WheelphoneTargetDocking.this
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
    
//    /** Native function to update the renderer. */
//    public native void updateRendering(int width, int height);
    
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
                            // Activate the renderer:
                            mRenderer.mIsActive = true;

                            // Now add the GL surface view. It is important
                            // that the OpenGL ES surface view gets added
                            // BEFORE the camera is started and video
                            // background is configured.

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
        
        
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = QCAR.requiresAlpha();

        //mGlView = new QCARSampleGLView(this);
        mGlView = (QCARSampleGLView) findViewById(R.id.glView);

        mGlView.init(mQCARFlags, translucent, depthSize, stencilSize);

        mRenderer = new FrameMarkersRenderer();
        mRenderer.setReference(this);
        mGlView.setRenderer(mRenderer);

        mGlView.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                autofocus();
                
                // Autofocus action resets focus mode
                mFocusMode = FOCUS_MODE_NORMAL;
            }});
        
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
   
   /** Returns the number of registered textures. */
   public int getTextureCount()
   {
       return mTextures.size();
   }


   /** Returns the texture object at the specified index. */
   public Texture getTexture(int i)
   {
       return mTextures.elementAt(i);
   }
   
   /** The native render function. */
   public native void getTrackInfo();    
   
   /** We want to load specific textures from the APK, which we will later
   use for rendering. */
   private void loadTextures()
   {
       mTextures.add(Texture.loadTextureFromApk("letter_Q.png", getAssets()));
       mTextures.add(Texture.loadTextureFromApk("letter_C.png", getAssets()));
       mTextures.add(Texture.loadTextureFromApk("letter_A.png", getAssets()));
       mTextures.add(Texture.loadTextureFromApk("letter_R.png", getAssets()));
   }
   
   public void updateMarkersInfo(int markerId, boolean detected, int i1, int i2, float dist, float z, float tpx, float tpz) {
       // We use a handler because this thread cannot change the UI
       Message message = new Message();
       message.obj = String.valueOf(i1) + ", " + String.valueOf(i2);
       
       targetX[markerId] = i1;
       targetY[markerId] = i2;
       targetOrientation[markerId] = (float) ((Math.asin(z)/Math.PI)*180.0);
       targetDist[markerId] = dist;
       targetPoseX[markerId] = tpx;
       targetPoseZ[markerId] = tpz;
       angleTargetRobot[markerId] = (float) ((Math.atan2(tpx, tpz)/Math.PI)*180.0) + targetOrientation[markerId];       
       
       Log.d(TAG, "detected=" + detected + "(" + markerId + ")" + ", x=" + i1 + ", y=" + i2 + ", dist=" + dist + "angle=" + ((Math.asin(z)/Math.PI)*180.0) + " (z=" + z + "), h=" + mScreenHeight + ", w=" + mScreenWidth + "\n");
       Log.d(TAG, "target x=" + tpx + ", target z=" + tpz + ", angle target robot=" + angleTargetRobot[markerId]);
       //Log.d(TAG, "x=" + x + ", y=" + y + ", z=" + z + "\n");
       //Log.d(TAG, "sin(x)=" + Math.sin(x)/Math.PI*180.0 + ", cos(x)=" + Math.cos(x)/Math.PI*180.0 + "\n");
       //Log.d(TAG, "sin(y)=" + Math.sin(y)/Math.PI*180.0 + ", cos(y)=" + Math.cos(y)/Math.PI*180.0 + "\n");
       //Log.d(TAG, "sin(z)=" + Math.sin(z)/Math.PI*180.0 + ", cos(z)=" + Math.cos(z)/Math.PI*180.0 + "\n");         
    	
       logString = "UM,";
       logString += "detected=" + detected + "(" + markerId + ")" + ",Xscreen="+targetX[markerId] + ",Yscreen="+targetY[markerId] + ",orient="+targetOrientation[markerId] + ",dist="+targetDist[markerId] + ",Xpose="+targetPoseX[markerId] + ",Zpose="+targetPoseZ[markerId] + ",angle="+angleTargetRobot[markerId];
       
	   	if(!detected) {	
	   		targetDetectedInfo[markerId] = NO_TARGET_FOUND;
	   		//switchToLittleTag[markerId] = false;	// if we lose the big target and some time is needed to detect the small target then we don't stop 
	   												// if we set this flag to false!
		} else { 
			targetDetectedInfo[markerId] = TARGET_FOUND;
			mLastDetectedMarkerId = markerId;
		       
			logString += "\n" + "targetDistPrev="+targetDistPrev[markerId] + ",targetDist=" +  targetDist[markerId];
			
			if(targetDistPrev[markerId]==0) {	// at the beginning we don't have a previous distance value, so initializes it with the current distance
		       targetDistPrev[markerId] = targetDist[markerId];
		       logString+="\n targetDistPrev reset! "  + "targetDistPrev="+targetDistPrev[markerId] + ",targetDist=" +  targetDist[markerId];;
		    } 
		    if((targetDist[markerId] - targetDistPrev[markerId]) > DIST_DIFF_THR) {	// from big to small
		       switchToLittleTag[markerId] = true;
		    } else if((targetDist[markerId] - targetDistPrev[markerId]) < -DIST_DIFF_THR) {	// from small to big
		    	switchToLittleTag[markerId] = false;
		    }
		    targetDistPrev[markerId] = targetDist[markerId];
		    Log.d(TAG, "switchToLittleTag = " + switchToLittleTag[markerId]);
		    logString += "\n" + "switchToLittleTag = " + switchToLittleTag[markerId];
 		}
       
	   	if(markerId >= 2) {	// only for targets that aren't on docking station
		   	if(markerId==mCurrentTargetId) {
		   		if(targetDetectedInfo[markerId]==NO_TARGET_FOUND) {
		   			if((System.currentTimeMillis() - mTimestampLastSeen > 500) && mSeenOnce==true) {
		   				mIsExploring = true;			
		   			    if(lastRotation == ROT_LEFT) {
		   			    	mDesiredRotation = -4;
		   			    } else {
		   			    	mDesiredRotation = 4;
		   			    }
		   				mDesiredAcceleration = 0;
		   			}
		   			if (System.currentTimeMillis() - mTimestampLastSeen > TARGET_TIMEOUT && globalState!=STATE_WAITING_START) {
		   				startExploring();
		   				goToNextTarget();		   				
		   			}
		   		} else {
		   			mSeenOnce = true;
		   			mIsExploring = false;
					// Target is/was visible, advance while trying to reach it:
					// Set the rotation on a range between -1 and 1:
					mDesiredRotation = (2 * ((double)targetX[markerId] / (double)mScreenWidth)) - 1;
					mDesiredAcceleration = 1;
					mTimestampLastSeen = System.currentTimeMillis();
		   		}
		   	}
	   	}	   	
	   	
	   	appendLog(logString);

   }   
   
   public void onInit(int status) {
	   
       if (status == TextToSpeech.SUCCESS) {

           int result = tts.setLanguage(Locale.US);

           if (result == TextToSpeech.LANG_MISSING_DATA
                   || result == TextToSpeech.LANG_NOT_SUPPORTED) {
               Log.e("TTS", "This Language is not supported");
           }

       } else {
       	tts.setSpeechRate((float)0.7);
           Log.e("TTS", "Initilization Failed!");
       }

   }
   
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        context = this.getApplicationContext();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        settings.registerOnSharedPreferenceChangeListener(this);
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "com.wheelphone.targetdocking.wakelock");
           
        httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);       

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();
        
        // Query the QCAR initialization flags
        mQCARFlags = getInitializationFlags();

        // Update the application status to start initializing application
        updateApplicationStatus(APPSTATUS_INIT_APP);        
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        //updateRendering(size.x, size.y);
               
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();   
        //wheelphone.disableSpeedControl();   
        wheelphone.enableSoftAcceleration();
        wheelphone.setUSBCommunicationTimeout(10000);     
        
		tts = new TextToSpeech(this, this);
		
        tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        spec1=tabHost.newTabSpec("Robot");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Robot");
        spec2=tabHost.newTabSpec("Target");
        spec2.setIndicator("Target");
        spec2.setContent(R.id.tab2);
        spec3=tabHost.newTabSpec("Map");
        spec3.setIndicator("Map");
        spec3.setContent(R.id.tab3);        
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);  	
        tabHost.addTab(spec3);
        for (int i = 0; i < tabHost.getTabWidget().getTabCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = getWindowManager().getDefaultDisplay().getHeight()*7/100; // 7% of total screen height
        }  
        
        SeekBar seekbar = (SeekBar) findViewById(R.id.camOffset);
        seekbar.setMax(mScreenWidth);
        seekbar.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        		// TODO Auto-generated method stub
                camOffset = progress - mScreenWidth/2;
        	}
        	public void onStartTrackingTouch(SeekBar seekBar) {
        		// TODO Auto-generated method stub
        	}
        	public void onStopTrackingTouch(SeekBar seekBar) {
        		// TODO Auto-generated method stub
        	}
        });
        seekbar.setProgress(mScreenWidth/2);
        
        displayIpAddress();
        	
        txtX0 = (TextView)findViewById(R.id.X0);
        txtY0 = (TextView)findViewById(R.id.Y0);
        txtd0 = (TextView)findViewById(R.id.d0);
        txtOrient0 = (TextView)findViewById(R.id.orient0);
        leftSpeed = (TextView)findViewById(R.id.leftSpeedTxt);
        rightSpeed = (TextView)findViewById(R.id.rightSpeedTxt);
        batteryValue = (TextView)findViewById(R.id.batteryLevel);
        txtConnected = (TextView)findViewById(R.id.txtConnection);

        for(int i=0; i<NUM_TARGETS; i++) {
        	targetDetectedInfo[i] = NO_INFO;
        }
        
        fpv = (FloorPlanView)findViewById(R.id.floorPlanView);
        roomWidth = 200;
        roomLength = 200;
        fpv.updateMapSize(roomWidth, roomLength);
        txtRoomWidth = (EditText) findViewById(R.id.txtRoomWidth);
        txtRoomWidth.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    roomWidth = Integer.parseInt(v.getText().toString());
                    fpv.updateMapSize(roomWidth, roomLength);
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        }); 
        txtRoomLength = (EditText) findViewById(R.id.txtRoomLength);
        txtRoomLength.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    roomLength = Integer.parseInt(v.getText().toString());
                    fpv.updateMapSize(roomWidth, roomLength);
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });
        
        txtChargeTime = (EditText) findViewById(R.id.chargeTime);
        txtChargeTime.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mChargeTime = Integer.parseInt(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });        
        txtStopDistance = (EditText) findViewById(R.id.stopDistance);
        txtStopDistance.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mStopDistance = Integer.parseInt(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });       
        
        txtK1 = (EditText) findViewById(R.id.k1);
        txtK1.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mCoordCoeff = Double.parseDouble(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        }); 
        
        txtK2 = (EditText) findViewById(R.id.k2);
        txtK2.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    mOrientCoeff = Double.parseDouble(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });
        
        robotX = (TextView)findViewById(R.id.robotX);
        robotY = (TextView)findViewById(R.id.robotY);
        robotTheta = (TextView)findViewById(R.id.robotTheta);
        
        startExploring();
		
		mTimestampLastSeen = System.currentTimeMillis();
		mGlobalTimestamp = System.currentTimeMillis();
		
        // for testing target detection without robot
        //timer = new Timer();                               
        //timer.schedule(new trackerTask(), 0, 500); 
		
		logString = "\n\n\nSTART NEW LOG\n";
		appendLog(logString);
    }
    
    public void onStart() {
    	super.onStart();

    	wheelphone.startUSBCommunication();
    	    	
    	// Lock screen
    	wl.acquire();
    	
    }
    	
    public void onStop() {
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
    	super.onResume();

   		wheelphone.resumeUSBCommunication();    
   		wheelphone.setWheelPhoneRobotListener(this);

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
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
    }
    
    public void onPause() {
    	super.onPause();

    	wheelphone.pauseUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(null);
    	
    	CustomHttpServer.setScreenState(false);
    	unregisterReceiver(wifiStateReceiver);
    	    	
//        if (mGlView != null)
//        {
//            mGlView.setVisibility(View.INVISIBLE);
//            mGlView.onPause();
//        }
//    	
//        if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
//        {
//            updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
//        }

        // QCAR-specific pause operation:
        QCAR.onPause();
        
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

            // Unload texture:
            mTextures.clear();
            mTextures = null;
            
            // Deinit the tracker:
            deinitTracker();

            // Deinitialize QCAR SDK:
            QCAR.deinit();
        }
        
        timer.cancel();
        
        System.gc();
        
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
				case CustomHttpServer.MOVE_BACKWARD:  			    			
				case CustomHttpServer.MOVE_LEFT:
				case CustomHttpServer.MOVE_RIGHT:
				case CustomHttpServer.ENABLE_OBSTACLE_AVOIDANCE:
				case CustomHttpServer.DISABLE_OBSTACLE_AVOIDANCE:
				case CustomHttpServer.ENABLE_CLIFF_AVOIDANCE:
				case CustomHttpServer.DISABLE_CLIFF_AVOIDANCE:
					mContinuousMode = false;
					break;  			
				case CustomHttpServer.STOP:
					mContinuousMode = true;				
					break;
    		}			
			prevGlobalState = globalState;
			if(wheelphone.isCharging()) {	// go out of docking station
				dockReachedTimeout = 0;
				lSpeed = -40;
				rSpeed = -40;	
				mCurrentScreenshot = 1;
				mCurrentTargetId = 1;
	    		globalState = STATE_ROBOT_GO_BACK_TARGET;
			} else {	// restart from beginning otherwise
				globalState = STATE_TARGET_SEARCH_AND_APPROACH;						
				mCurrentScreenshot = 1;
				mCurrentTargetId = 1;
				goToNextTarget();
			}
    		
    	} //handle_message
    	
    };

    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	TextView line1 = (TextView)findViewById(R.id.txtHttpStatus);
		if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	String ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	line1.setText("HTTP://");
	    	line1.append(ip);
	    	line1.append(":8080");
    	} else {
    		line1.setText("HTTP://xxx.xxx.xxx.xxx:8080");
    	}
    }
    
    public void log(String s) {
    	Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		// TODO Auto-generated method stub
		
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
    
    public void calibrateSensors(View view) {
    	wheelphone.calibrateSensors();
    }
    
    void startExploring() {
		mIsExploring = true;			
		//rotate in place to find the direction to go:
		mDesiredRotation = -5;
		mDesiredAcceleration = 0;    	
    }
    
    void goToNextTarget() {
    	mGlobalTimestamp = System.currentTimeMillis();
    	prevGlobalState = globalState;
		if(mCurrentTargetId == (NUM_TARGETS-1)) {	// all target points reached, go to docking station
			mCurrentScreenshot = 1;
			mCurrentTargetId = 1;	// set it to "first target id - 1" because we take a screenshot also after being docked
									// thus the mCurrentTargetId is incremented by 1
			globalState = STATE_DOCKING_SEARCH;
			lastRotation = ROT_LEFT;
			invertRotation = true;
			rotCounter = 0;
			mTimestampDocking = System.currentTimeMillis();
		} else {
			mCurrentScreenshot++;
			mCurrentTargetId = mCurrentTargetId + 1;
			rotCounter = 0;
			globalState = STATE_TARGET_SEARCH_AND_APPROACH;		
			startExploring();
			switchToLittleTag[mCurrentTargetId] = false;
			mTimestampLastSeen = System.currentTimeMillis();
			mSeenOnce = false;
		}	
		
		targetDistPrev[mCurrentTargetId] = 0;
		
		if(mCurrentScreenshot == 1) {
			mRenderer.rotataImages();
		}
    }

	@Override
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneTargetDocking.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
		
		mOdomX = wheelphone.getOdometryX();
		mOdomY = wheelphone.getOdometryY();
		mOdomTheta = wheelphone.getOdometryTheta();
		
		txtd0.setText(String.valueOf(String.format("%.2f",targetDist[mLastDetectedMarkerId]))); // mOdomTheta*180/Math.PI))); //targetDist[mCurrentTargetId])));
		txtX0.setText(String.valueOf(targetX[mLastDetectedMarkerId]));
		txtY0.setText(String.valueOf(targetY[mLastDetectedMarkerId]));
		txtOrient0.setText(String.valueOf(String.format("%.2f", angleTargetRobot[mLastDetectedMarkerId])));
		//batteryValue.setText(String.valueOf(String.format("%.2f", wheelphone.getBatteryCharge())));
		batteryValue.setText(String.valueOf(wheelphone.getBatteryCharge())+"%");
		
		if(wheelphone.isUSBConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}

		if(wheelphone.getBatteryRaw() <= 30) {
			batteryValue.setTextColor(getResources().getColor(R.color.red));
		} else {
			batteryValue.setTextColor(getResources().getColor(R.color.green));
		}					
		if(wheelphone.isCharging()) {
			if(wheelphone.isCharged()) {
				chargeStatus = ROBOT_CHARGED;
			} else {
				chargeStatus = ROBOT_IN_CHARGE;
			}
		} else {
			chargeStatus = ROBOT_NOT_CHARGING;
		}
		
		if(wheelphone.isCalibrating()) {
			return;
		}
		
		if(mAppStatus == APPSTATUS_CAMERA_RUNNING) {
			getTrackInfo();
		}
		
		fpv.updateRobotInfo(100, 100, (0 + (int)targetOrientation[0] + 180)%360);
		
		if(mIsExploring) {
			mObstacleDesiredRotation = 0;
			mObstacleDesiredAcceleration = 0;
		} else {
			int[] frontProx = wheelphone.getFrontProxs();
			int maxIdx = 0;
			double max = 0;

			//find closest object:
			for (int i=0 ; i<frontProx.length ; i++){
				if (frontProx[i]>max){
					maxIdx = i;
					max = frontProx[i];
				}
			}

			//Rotate away from the closest sensed object (scaling: uses sensor idx to scale)
			if (max > 40){
				mObstacleLastSeenTimestamp = System.currentTimeMillis();

				//obstacle is present, rotation points away from the sensor that feels the obstacle the closest. Range [-2, 2]. (turn faster than the target seeker) 
				mObstacleDesiredRotation = - (double)2 / (double)((maxIdx <= 1) ? maxIdx-2 : maxIdx-1);
				//the higher the value of the sensor, the closest the obstacle. So the closer the slower we should go. Range [0, 1]
				mObstacleDesiredAcceleration = Math.abs(frontProx[maxIdx] / 255. - 1);
				
				Log.d(TAG, "seen: [" + mObstacleDesiredRotation + ", " + mObstacleDesiredAcceleration + "]");
			} else {
				//Obstacle is on one side of the robot, so go parallel to it:
				mObstacleDesiredRotation = 0;
				mObstacleDesiredAcceleration = 1;
			}
		}
		
		calculateSpeeds();
		
		logString = "WU: globalState="+globalState;
		logString += "\nLooking for target " + mCurrentTargetId + "(" + (mIsExploring?1:0) + ", " + (mIsAvoiding?1:0) + ", " + (switchToLittleTag[mCurrentTargetId]?1:0) + ")" + "/" + (System.currentTimeMillis()-mGlobalTimestamp)/1000;
		
		if(globalState == STATE_WAITING_START) {
			
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Waiting start..."+ mCurrentTargetId + "(" + (mIsExploring?1:0) + ", " + (mIsAvoiding?1:0) + ", " + (switchToLittleTag[mCurrentTargetId]?1:0) + ")" + "/" + (System.currentTimeMillis()-mGlobalTimestamp)/1000);
		    txtConnected.setTextColor(getResources().getColor(R.color.white));
			
		    rSpeed = 0;
			lSpeed = 0;
		
		} else if(globalState==STATE_TARGET_SEARCH_AND_APPROACH) {
			
			if((System.currentTimeMillis()-mGlobalTimestamp) > GLOBAL_TIMEOUT) {
				goToNextTarget();
			}
			
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Looking for target " + mCurrentTargetId + "(" + (mIsExploring?1:0) + ", " + (mIsAvoiding?1:0) + ", " + (switchToLittleTag[mCurrentTargetId]?1:0) + ")" + "/" + (System.currentTimeMillis()-mGlobalTimestamp)/1000);
		    txtConnected.setTextColor(getResources().getColor(R.color.red));					    
		    
		    if(mIsExploring) {
			    rotCounter++;
			    if(rotCounter == 7) {	//10
			    	lSpeedTemp = mLeftSpeed;
			    	rSpeedTemp = mRightSpeed;
			    	lSpeed = 0;
			    	rSpeed = 0;	
			    } else if(rotCounter >= 35) {	//38
		    		rotCounter = 0;
		    		lSpeed = lSpeedTemp;
		    		rSpeed = rSpeedTemp;
		    	}		    	
		    } else {
				lSpeed = mLeftSpeed;
				rSpeed = mRightSpeed;		    	
		    }
			
		    if(switchToLittleTag[mCurrentTargetId]) {
		    	mStopDistanceTemp = mStopDistance*4;
		    } else {
		    	mStopDistanceTemp = mStopDistance;
		    }
		    
		    if(targetDetectedInfo[mCurrentTargetId]==TARGET_FOUND) {
		    	if(targetX[mCurrentTargetId] < mScreenWidth/2) {
		    		lastRotation = ROT_LEFT;
		    	} else {
		    		lastRotation = ROT_RIGHT;
		    	}
		    }
		    
			if(targetDist[mCurrentTargetId] <= mStopDistanceTemp && targetDetectedInfo[mCurrentTargetId]==TARGET_FOUND) {
				if(mCurrentTargetId == 2) {
					tts.speak("living room", TextToSpeech.QUEUE_FLUSH, null);
				} else if(mCurrentTargetId == 3) {
					tts.speak("kitchen", TextToSpeech.QUEUE_FLUSH, null);
				}
				startExploring();			
				lSpeed = 0;
				rSpeed = ROT_SPEED;		
				rotCounter = 0;
				wheelphone.resetOdometry();
				globalState = STATE_ROTATE_FOR_SCREENSHOT;
				prevGlobalState = STATE_TARGET_SEARCH_AND_APPROACH;

			}
			
			//globalState = STATE_DOCKING_SEARCH;
			
		} else if(globalState == STATE_DOCKING_SEARCH) {
			
			if(System.currentTimeMillis() - mTimestampDocking > DOCK_TIMEOUT) {
				goToNextTarget();
			}
			wheelphone.disableSoftAcceleration();
			baseSpeed = 35; //BASE_SPEED;
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is looking for docking station");
		    txtConnected.setTextColor(getResources().getColor(R.color.red));					    
		    
		    wheelphone.disableObstacleAvoidance();
		    
		    if(invertRotation) {
		    	invertRotation = false;
		    	if(lastRotation == ROT_RIGHT) {
		    		lSpeed = baseSpeed;		// pivot rotating to find the docking station
		    		rSpeed = -baseSpeed;		// rotate at low speed to have time to detect the target
		    	} else {
		    		lSpeed = -baseSpeed;
		    		rSpeed = baseSpeed;
		    	}
		    }
		    
		    rotCounter++;
		    if(rotCounter == 10) {
		    	lSpeedTemp = lSpeed;
		    	rSpeedTemp = rSpeed;
		    	lSpeed = 0;
		    	rSpeed = 0;	
		    } else if(rotCounter >= 38) {
	    		rotCounter = 0;
	    		lSpeed = lSpeedTemp;
	    		rSpeed = rSpeedTemp;
	    	}
		    
			if(targetDetectedInfo[0]==TARGET_FOUND) {
				globalState = STATE_DOCKING_APPROACH_SMALL;
				prevGlobalState = STATE_DOCKING_SEARCH;
			} else if(targetDetectedInfo[1]==TARGET_FOUND) {
				globalState = STATE_DOCKING_APPROACH_BIG;
				prevGlobalState = STATE_DOCKING_SEARCH;
				wheelphone.enableObstacleAvoidance();
			}
			

		} else if(globalState == STATE_DOCKING_APPROACH_BIG) {
			
			if((System.currentTimeMillis()-mGlobalTimestamp) > GLOBAL_TIMEOUT) {
				goToNextTarget();
			}
			
			mTimestampDocking = System.currentTimeMillis();			
			
			wheelphone.enableSoftAcceleration();
			baseSpeed = BASE_SPEED;
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot found docking station (big)");
		    txtConnected.setTextColor(getResources().getColor(R.color.green));
					
		    if(targetDetectedInfo[1] == TARGET_FOUND) {						
				targetTimesInScreen = (int)(targetDist[1]/markerSize);	// if the ratio is 1 (target dist = marker size) it means that the target fill the image completely		
				if(targetTimesInScreen < 1) {	// shouldn't never be lower than 1, but check it for safety
					targetTimesInScreen = 1;
				}
				//rotationFactor = (int)(COORDINATE_COEFF*(mScreenWidth/2-targetX[1]-(int)(camOffset*50.0/targetDist[1]))) + (int)(ORIENTATION_COEFF*targetOrientation[1]);
				rotationFactor = (int)(mCoordCoeff*(mScreenWidth/2-targetX[1]) + mOrientCoeff*angleTargetRobot[1]);
				//if(rotationFactor >= 0) {
				if(targetX[1] < mScreenWidth/2) {
					lastRotation = ROT_LEFT;
				} else {
					lastRotation = ROT_RIGHT;
				}
//				// avoid to lose the target; check the x coordinate and when it is near the border stop rotating
//				// otherwise the rotation will lead to lose the target, instead goes forward
//				if(targetX[1] < (mScreenWidth*1/targetTimesInScreen)) {
//					rotationFactor = 0;
//				}
//				if(targetX[1] > (mScreenWidth*(targetTimesInScreen-1)/targetTimesInScreen)) {
//					rotationFactor = 0;
//				}
				lSpeed = baseSpeed - rotationFactor;
				rSpeed = baseSpeed + rotationFactor; 														
			} else if(targetDetectedInfo[0] == TARGET_FOUND) {
				globalState = STATE_DOCKING_APPROACH_SMALL;
				prevGlobalState = STATE_DOCKING_APPROACH_BIG;
		    } else if(targetDetectedInfo[1] == NO_TARGET_FOUND) {							
				//globalState = STATE_DOCKING_SEARCH;
		    	dockReachedTimeout = 0;
				globalState = STATE_ROBOT_GO_FORWARD;
				prevGlobalState = STATE_DOCKING_APPROACH_BIG;
				invertRotation = true;
				rotCounter = 0;							
			}
				
		} else if(globalState == STATE_DOCKING_APPROACH_SMALL) {
			
			if((System.currentTimeMillis()-mGlobalTimestamp) > GLOBAL_TIMEOUT) {
				goToNextTarget();
			}
			
			mTimestampDocking = System.currentTimeMillis();				
			
			wheelphone.enableSoftAcceleration();
			baseSpeed = BASE_SPEED;
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot found docking station (small)");
		    txtConnected.setTextColor(getResources().getColor(R.color.green));
													
			if(targetDist[0] <= 380) {	// disable obstacle avoidance when approaching the docking station
				wheelphone.disableObstacleAvoidance();
				baseSpeed = BASE_SPEED*2/3;
			} else {
				wheelphone.enableObstacleAvoidance();
				baseSpeed = BASE_SPEED;
			}
					
			if(targetDist[0] <= 200) {
				globalState = STATE_DOCKING_REACHED;
				prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
				dockReachedTimeout = 0;
			}
			
			if(targetDetectedInfo[1] == TARGET_FOUND) {
				globalState = STATE_DOCKING_APPROACH_BIG;
				prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
				wheelphone.enableObstacleAvoidance();
			} else if(targetDetectedInfo[0] == NO_TARGET_FOUND) {							
				//globalState = STATE_DOCKING_SEARCH;
				dockReachedTimeout = 0;
				globalState = STATE_ROBOT_GO_FORWARD;
				prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
				invertRotation = true;
				rotCounter = 0;							
			} else if(targetDetectedInfo[0] == TARGET_FOUND) {	
				lastTargetX[0] = targetX[0];
				targetTimesInScreen = (int)(targetDist[0]/markerSize);	// if the ratio is 1 (target dist = marker size) it means that the target fill the image completely
				if(targetTimesInScreen < 1) {	// shouldn't never be lower than 1, but check it for safety
					targetTimesInScreen = 1;
				}				
				// camOffset influences the rotationFactor linearly with the inverse of the distance, the near the target the more influence is actuated   
				//rotationFactor = (int)(COORDINATE_COEFF*(mScreenWidth/2-targetX[0]+(int)(camOffset*50.0/targetDist[0])) + ORIENTATION_COEFF*angleTargetRobot[0]);
				rotationFactor = (int)(mCoordCoeff*(mScreenWidth/2-targetX[0]) + mOrientCoeff*angleTargetRobot[0]);
				//if(rotationFactor >= 0) {
				if(targetX[0] < mScreenWidth/2) {
					lastRotation = ROT_LEFT;
				} else {
					lastRotation = ROT_RIGHT;
				}
//				// avoid to lose the target; check the x coordinate and when it is near the border stop rotating
//				// otherwise the rotation will lead to lose the target, instead goes forward				
//				if(targetX[0] < (mScreenWidth*1/targetTimesInScreen)) {
//					rotationFactor = 0;
//				}
//				if(targetX[0] > (mScreenWidth*(targetTimesInScreen-1)/targetTimesInScreen)) {
//					rotationFactor = 0;
//				}
				lSpeed = baseSpeed - rotationFactor;
				rSpeed = baseSpeed + rotationFactor; 														
			}
				
		} else if(globalState == STATE_DOCKING_REACHED) {
			
			if(System.currentTimeMillis() - mTimestampDocking > DOCK_TIMEOUT) {
				goToNextTarget();
			}
			
			wheelphone.enableSoftAcceleration();
			baseSpeed = BASE_SPEED;
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Docking reached");
		    txtConnected.setTextColor(getResources().getColor(R.color.green));							
			
			lSpeed = baseSpeed; //*2;
			rSpeed = baseSpeed; //*2;
			
			if(chargeStatus == ROBOT_IN_CHARGE) {
				chargeCounter = 0;
				globalState = STATE_ROBOT_CHARGING;
				prevGlobalState = STATE_DOCKING_REACHED;
			}
			
			if(prevGlobalState==STATE_ROBOT_CHARGING) {
				instableChargeCounter++;
				if(instableChargeCounter >= 40) {
					lSpeed = 40;
					rSpeed = 40;
				}
				if(instableChargeCounter >= 55) {
					lSpeed = 0;
					rSpeed = 0;
				}
				if(instableChargeCounter >= 70) {
					lSpeed = -20;
					rSpeed = 20;
				} 
				if(instableChargeCounter >= 85) {
					lSpeed = 0;
					rSpeed = 0;
				}
				if(instableChargeCounter >= 100) {
					lSpeed = 40;
					rSpeed = 40;
				}
				if(instableChargeCounter >= 115) {
					lSpeed = 0;
					rSpeed = 0;
				}
				if(instableChargeCounter >= 130) {
					lSpeed = 20;
					rSpeed = -20;
				}	
				if(instableChargeCounter >= 145) {
					lSpeed = 0;
					rSpeed = 0;
				}	
				if(instableChargeCounter >= 160) {
					lSpeed = 40;
					rSpeed = 40;
				}	
				if(instableChargeCounter >= 175) {
					lSpeed = 0;
					rSpeed = 0;
				}	
				if(instableChargeCounter >= 190) {
					lSpeed = 20;
					rSpeed = -20;
				}		
				if(instableChargeCounter >= 205) {
					lSpeed = 0;
					rSpeed = 0;
				}	
				if(instableChargeCounter >= 220) {
					lSpeed = 40;
					rSpeed = 40;
				}	
				if(instableChargeCounter >= 235) {
					lSpeed = 0;
					rSpeed = 0;
				}	
				if(instableChargeCounter >= 250) {
					lSpeed = -20;
					rSpeed = 20;
				}	
				if(instableChargeCounter >= 265) {
					lSpeed = 0;
					rSpeed = 0;
				}				
				if(instableChargeCounter >= 280) {
					instableChargeCounter = 40;
				}								
			}	
			
			dockReachedTimeout++;	// if the robot is near the charger but do not get ever the contact with the charger station
									// or cannot get well docked then go back and retry
			
			if(dockReachedTimeout>=50 && prevGlobalState==STATE_DOCKING_APPROACH_SMALL) {	// the first time we try to dock maybe we are a little skew,
				if(lastTargetX[0] < mScreenWidth/2) {										// so try to straighten
					lSpeed = -20;
					rSpeed = 20;
				} else {
					lSpeed = 20;
					rSpeed = -20;
				}
			}
			if(dockReachedTimeout>=58 && prevGlobalState==STATE_DOCKING_APPROACH_SMALL) {
				lSpeed = baseSpeed*2;
				rSpeed = baseSpeed*2;
			}
			
			if(dockReachedTimeout >= 300) {	// about 15 seconds
				if(lastTargetX[0] < mScreenWidth/2) {										// so try to straighten
					lSpeed = -70;
					rSpeed = -50;
				} else {
					lSpeed = -50;
					rSpeed = -70;
				}
				dockReachedTimeout = 0;
				globalState = STATE_ROBOT_GO_BACK_DOCKING;
				prevGlobalState = STATE_DOCKING_REACHED;
			}
			
			if(targetDist[0]>380 || targetDist[1]>380) { // || (targetDist[0]==0 && targetDist[1]==0)) {
				globalState = STATE_DOCKING_SEARCH;
				prevGlobalState = STATE_DOCKING_REACHED;
				invertRotation = true;
				rotCounter = 0;	
			}
			
			
		} else if(globalState == STATE_ROBOT_GO_BACK_DOCKING) { 
			
			if(System.currentTimeMillis() - mTimestampDocking > DOCK_TIMEOUT) {
				goToNextTarget();
			}
			
			dockReachedTimeout++;
			if(dockReachedTimeout >= 40) {	// go back for about 0.5 seconds
				globalState = STATE_DOCKING_SEARCH;
				prevGlobalState = STATE_ROBOT_GO_BACK_DOCKING;
				invertRotation = true;
				rotCounter = 0;
			}
			
		} else if(globalState == STATE_ROBOT_GO_FORWARD) { 
			baseSpeed = BASE_SPEED*2/3;
			lSpeed = baseSpeed;
			rSpeed = baseSpeed;
			//dockReachedTimeout++;
			//if(dockReachedTimeout >= 25) {	// go forward for about 1.25 seconds
				globalState = STATE_DOCKING_SEARCH;
				prevGlobalState = STATE_ROBOT_GO_FORWARD;
				invertRotation = true;
				rotCounter = 0;
			//}
			
		} else if(globalState == STATE_ROBOT_CHARGING) {
			
			TextView txtConnected;	
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is charging");
		    txtConnected.setTextColor(getResources().getColor(R.color.yellow));
		    	
			rSpeed = 0;
			lSpeed = 0;	
		    
		    if(chargeStatus == ROBOT_NOT_CHARGING) {
		    	globalState = STATE_DOCKING_REACHED;
		    	prevGlobalState = STATE_ROBOT_CHARGING;
		    	dockReachedTimeout = 0;
		    } else {
		    	chargeCounter++;
		    	if(chargeCounter == 20) {	
		    		instableChargeCounter = 0;
		    		tts.speak("charging", TextToSpeech.QUEUE_FLUSH, null);
		    	} else if(chargeCounter >= (mChargeTime*20)) {	// *1000/50
		    		mGlobalTimestamp = System.currentTimeMillis();
		    		mTimestampDocking = System.currentTimeMillis();
		    		chargeCounter = mChargeTime*20;
		    		if(mContinuousMode) {
			    		dockReachedTimeout = 0;
						lSpeed = -40;
						rSpeed = -40;
			    		globalState = STATE_ROBOT_GO_BACK_TARGET;
			    		prevGlobalState = STATE_ROBOT_CHARGING;
		    		}
		    	}
		    }					    	
		    
		} else if(globalState == STATE_ROBOT_GO_BACK_TARGET) { 			
			dockReachedTimeout++;
			if(dockReachedTimeout >= 40) {	// go back for about 0.5 seconds
				lSpeed = 0;
				rSpeed = ROT_SPEED;
				globalState = STATE_ROTATE_FOR_SCREENSHOT;
				prevGlobalState = STATE_ROBOT_GO_BACK_TARGET;
				invertRotation = true;
				rotCounter = 0;
				wheelphone.resetOdometry();
			}			
		} else if(globalState == STATE_ROTATE_FOR_SCREENSHOT) {
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is rotating for screenshot");
		    txtConnected.setTextColor(getResources().getColor(R.color.red));	
			//rotCounter++;
			//if(rotCounter >= 55) {	
		    if(mOdomTheta >= Math.PI) {
				lSpeed = 0;
				rSpeed = 0;
				rotCounter=0;
				globalState = STATE_WAIT_STOPPED_FOR_SCREENSHOT;
				prevGlobalState = STATE_ROTATE_FOR_SCREENSHOT;	
				mCurrentTab = tabHost.getCurrentTab();
				tabHost.setCurrentTab(1);	// select the camera tab to take the screenshot
		    }
		} else if(globalState == STATE_WAIT_STOPPED_FOR_SCREENSHOT) {
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is waiting to be stopped");
		    txtConnected.setTextColor(getResources().getColor(R.color.red));	
		    rotCounter++;
			if(rotCounter>=20) {
				tts.speak("taking picture", TextToSpeech.QUEUE_FLUSH, null);
				takeScreenshot = true;
				//mRenderer.saveScreenShot();
				prevGlobalState = STATE_WAIT_STOPPED_FOR_SCREENSHOT;
				globalState = STATE_TAKE_SCREENSHOT;
			}
		} else if(globalState == STATE_TAKE_SCREENSHOT) {			
			TextView txtConnected;
		    txtConnected = (TextView)findViewById(R.id.txtGeneralStatus);
		    txtConnected.setText("Robot is taking screenshot");
		    txtConnected.setTextColor(getResources().getColor(R.color.red));				
			if(takeScreenshot == false) {
				tabHost.setCurrentTab(mCurrentTab);				
				goToNextTarget();
			}
		}
		
		logString += "\nl="+lSpeed+",r="+rSpeed;
		appendLog(logString);
		
		wheelphone.setLeftSpeed(lSpeed);
		wheelphone.setRightSpeed(rSpeed);
		leftSpeed.setText(String.valueOf(lSpeed));
		rightSpeed.setText(String.valueOf(rSpeed));		
		
	}
    
	private synchronized void calculateSpeeds(){
		//angularAcc should come in a range from -1 to +1
		double angularAcc = mDesiredRotation;

		//Spring damping system (should always be between -1 and 1)
		double linearAcc = mDesiredAcceleration;
		
		mIsAvoiding = false;
		
		if (!mIsExploring){
			if (System.currentTimeMillis() - mObstacleLastSeenTimestamp < 500) { //300) {
				//seeing or recently saw an obstacle: use obstacle avoidance rotation and acceleration values 
				angularAcc = mObstacleDesiredRotation;
				linearAcc = mObstacleDesiredAcceleration;
				mIsAvoiding = true;
			}
		} 

		mLeftSpeed = (int)(mLeftSpeed
				+   linearAcc * LINEAR_SPRING_CONST
				+	angularAcc * ANGULAR_SPRING_CONST
				-	DUMPING_FACTOR * mLeftSpeed);

		mRightSpeed = (int)(mRightSpeed 
				+   linearAcc * LINEAR_SPRING_CONST
				-	angularAcc * ANGULAR_SPRING_CONST
				-	DUMPING_FACTOR * mRightSpeed);
		
	}
	
	boolean takeScreenshot() {
		return takeScreenshot;
	}
	
	void takeScreenshot(boolean b) {
		takeScreenshot = b;
	}
	
	int getCurrentScreenshot() {
		return mCurrentScreenshot;
	}
	
	void setCurrentScreenshot(int s) {
		mCurrentScreenshot = s;
	}
	
	void appendLog(String text)
	{       
	   File logFile = new File("sdcard/logFile.csv");
	   if (!logFile.exists())
	   {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
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
	
    public void resetBehavior(View view) {
    	mContinuousMode = false;
    	lSpeed=0;
    	rSpeed=0;
    	prevGlobalState=globalState;
    	globalState=STATE_WAITING_START;
    }
	
}