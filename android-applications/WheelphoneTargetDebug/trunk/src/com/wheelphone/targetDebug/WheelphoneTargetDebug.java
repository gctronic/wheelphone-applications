
package com.wheelphone.targetDebug;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import com.qualcomm.QCAR.QCAR;
import com.wheelphone.targetDebug.WheelphoneTargetDebug;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView.OnEditorActionListener;

public class WheelphoneTargetDebug extends Activity implements OnSharedPreferenceChangeListener, WheelPhoneRobotListener {
    
	// Various
	private static String TAG = WheelphoneTargetDebug.class.getName();
	boolean getFirmwareFlag = true;
	private PowerManager.WakeLock wl;
	private int lSpeedTemp=0, rSpeedTemp=0;
	public long startTime=0, endTime=0;
	public int procTime=0, minProcTime=1000, maxProcTime=0, procTimeResetCounter=0;
	public boolean startFlag=true;
	public int camOffset = 0;
	public boolean takeScreenshot=false;
	Context context;
	
	// Robot state
	WheelphoneRobot wheelphone;
	private int lSpeed=0, rSpeed=0;
	private int firmwareVersion=0;
	
	// target detection
	public static final int NUM_TARGETS = 4;	
	public int[] targetX = new int[NUM_TARGETS];		// x coordinate on the screen of the target
	public int[] lastTargetX = new int[NUM_TARGETS];
	public int[] targetY = new int[NUM_TARGETS];
	public float[] targetOrientation = new float[NUM_TARGETS];	// orientation of the robot/camera
	public float[] targetDist = new float[NUM_TARGETS];
	public float[] targetPoseX = new float[NUM_TARGETS];	// x coordinate (position) of the target with respect to the camera frame
	public float[] targetPoseZ = new float[NUM_TARGETS];
	public float[] angleTargetRobot = new float[NUM_TARGETS];	// angle between the robot and the target, independent from robot orientation
	public static final int NO_INFO = 0;    
	public static final int NO_TARGET_FOUND = 1;
	public static final int TARGET_FOUND = 2;	
	public int[] targetDetectedInfo = new int[NUM_TARGETS];
	public static final int ROBOT_NOT_CHARGING = 0;
	public static final int ROBOT_IN_CHARGE = 1;
	public static final int ROBOT_CHARGED = 2;
	public int chargeStatus = ROBOT_NOT_CHARGING;
	public int chargeCounter = 0;
	private int mCurrentTargetId=2;

	// UI
	TabSpec spec1, spec2;
	TabHost tabHost;
	TextView txtX0, txtY0, txtd0, txtOrient0; 
	TextView minTime, maxTime;
	TextView txtMarkerId;
	private TextView batteryValue;
	private EditText leftSpeed, rightSpeed;
	private TextView txtConnected;
	private CheckBox chkSpeedControl, chkSoftAcc;
	
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
    private static final String NATIVE_LIB_SAMPLE = "WheelphoneTargetDebug";
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
			WheelphoneTargetDebug.this.runOnUiThread(new Runnable() {                  
				//@Override                 
				public void run() {
					getTrackInfo();
					txtd0.setText(String.valueOf(String.format("%.4f", targetOrientation[0]))); //targetDist[0])));
					txtX0.setText(String.valueOf(targetPoseX[0])); //targetX[0]));
					txtY0.setText(String.valueOf(targetPoseZ[0])); //targetY[0]));
					txtOrient0.setText(String.valueOf(String.format("%.4f", angleTargetRobot[0]))); //targetOrientation[0])));
					float currentAbsoluteOrientation = (0 + (int)targetOrientation[1] + 180)%360; // ID + orientation + 180
					//robotX.setText(String.valueOf(0+(int)(Math.cos(targetOrientation[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0)));
					//robotY.setText(String.valueOf(100+(int)(Math.sin(targetOrientation[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0)));
					//robotTheta.setText(String.valueOf((int)currentAbsoluteOrientation));
					//fpv.updateRobotInfo(0+(int)(Math.cos(angleTargetRobot[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0), 100-(int)(Math.sin(angleTargetRobot[1]*Math.PI/180.0)*targetDist[1]*16.0/50.0), (int)currentAbsoluteOrientation);
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
                QCAR.setInitParameters(WheelphoneTargetDebug.this, mQCARFlags);

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
                    WheelphoneTargetDebug.this
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
    		   
	   	if(!detected) {	
	   		targetDetectedInfo[markerId] = NO_TARGET_FOUND;
		} else { 
			targetDetectedInfo[markerId] = TARGET_FOUND;	
			mCurrentTargetId = markerId;			
		}
	   	
	   	
	   	if(markerId==mCurrentTargetId) {
	   		if(targetDetectedInfo[mCurrentTargetId]==TARGET_FOUND) {
	   			txtMarkerId.setText(String.valueOf(mCurrentTargetId));
			   	if(startFlag) {
			   		startFlag = false;
			   		startTime = System.currentTimeMillis();	   				 
			   	} else {
			   		startFlag = true;
			   		endTime = System.currentTimeMillis();
			   		procTime = (int)(endTime - startTime);
			   		if(minProcTime > procTime) {
			   			minProcTime = procTime;
			   		}
			   		if(maxProcTime < procTime) {
			   			maxProcTime = procTime;
			   		}
			   	}
		   	} else {
		   		txtMarkerId.setText(String.valueOf(-1));
		   		minProcTime = 1000;
		   		maxProcTime = 0;
		   	}
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

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();
        
        // Query the QCAR initialization flags
        mQCARFlags = getInitializationFlags();

        // Update the application status to start initializing application
        updateApplicationStatus(APPSTATUS_INIT_APP);        
                      
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();   
        //wheelphone.disableSpeedControl();   
        wheelphone.enableSoftAcceleration();
        wheelphone.setCommunicationTimeout(10000);     
		
        tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        spec1=tabHost.newTabSpec("Robot");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Robot");
        spec2=tabHost.newTabSpec("Target");
        spec2.setIndicator("Target");
        spec2.setContent(R.id.tab2);      
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);  	
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
        	
        txtX0 = (TextView)findViewById(R.id.X0);
        txtY0 = (TextView)findViewById(R.id.Y0);
        txtd0 = (TextView)findViewById(R.id.d0);
        txtOrient0 = (TextView)findViewById(R.id.orient0);
        batteryValue = (TextView)findViewById(R.id.batteryLevel);
        txtConnected = (TextView)findViewById(R.id.txtConnection);
        
        for(int i=0; i<NUM_TARGETS; i++) {
        	targetDetectedInfo[i] = NO_INFO;
        }

        minTime = (TextView)findViewById(R.id.minTime);
        maxTime = (TextView)findViewById(R.id.maxTime);
        
        txtMarkerId = (TextView)findViewById(R.id.markerId);
        
        chkSpeedControl = (CheckBox)findViewById(R.id.chkSpeedControl);
    	chkSoftAcc = (CheckBox)findViewById(R.id.chkSoftAcc);
        
        leftSpeed = (EditText) findViewById(R.id.txtLeftSpeed);
        leftSpeed.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    lSpeed = Integer.parseInt(v.getText().toString());
                    wheelphone.setLeftSpeed(lSpeed);
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        }); 
        rightSpeed = (EditText) findViewById(R.id.txtRightSpeed);
        rightSpeed.setOnEditorActionListener(new OnEditorActionListener() {        	
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {               	
                    rSpeed = Integer.parseInt(v.getText().toString());
                    wheelphone.setRightSpeed(rSpeed);
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);                     
                    handled = true;
                }
                return handled;
            }                      
        });
		
        // for testing target detection without robot
        //timer = new Timer();                               
        //timer.schedule(new trackerTask(), 0, 500); 
    }
    
    public void onStart() {
    	super.onStart();
   	    	
    	// Lock screen
    	wl.acquire();
    	
    }
    	
    public void onStop() {
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
    	super.onResume();

   		wheelphone.startUSBCommunication();    
   		wheelphone.setWheelPhoneRobotListener(this);

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

    	wheelphone.closeUSBCommunication();
    	wheelphone.setWheelPhoneRobotListener(null);
    	    	
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

	@Override
	public void onWheelphoneUpdate() {
		
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneTargetDebug.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
			}
		}
				
		txtX0.setText(String.valueOf(targetX[mCurrentTargetId]));
		txtY0.setText(String.valueOf(targetY[mCurrentTargetId]));
		txtd0.setText(String.valueOf(String.format("%.2f", targetDist[mCurrentTargetId])));
		txtOrient0.setText(String.valueOf(String.format("%.2f", targetOrientation[mCurrentTargetId])));				
		batteryValue.setText(String.valueOf(wheelphone.getBatteryCharge())+"%");
		
		if(wheelphone.isRobotConnected()) {
	    	txtConnected.setText("Connected");
	    	txtConnected.setTextColor(getResources().getColor(R.color.green));
		} else {
			txtConnected.setText("Disconnected");
			txtConnected.setTextColor(getResources().getColor(R.color.red));
		}
		
		
		minTime.setText(String.valueOf(minProcTime));
		maxTime.setText(String.valueOf(maxProcTime));
		procTimeResetCounter++;
		if(procTimeResetCounter >= 200) {
			minProcTime = 1000;
			maxProcTime = 0;
			procTimeResetCounter = 0;
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
		
		//TextView txtStatus;
		//txtStatus = (TextView)findViewById(R.id.txtGeneralStatus);
		//txtStatus.setText("Robot found docking station (big)");
		//txtStatus.setTextColor(getResources().getColor(R.color.green));

		
	}
	
	boolean takeScreenshot() {
		return takeScreenshot;
	}
	
	void takeScreenshot(boolean b) {
		takeScreenshot = b;
	}
	
    public void controllerOnOff(View view) {
    	if(chkSpeedControl.isChecked()) {
    		wheelphone.enableSpeedControl();
    	} else {
    		wheelphone.disableSpeedControl();
    	}
    }    
    
    public void softAccOnOff(View view) {
    	if(chkSoftAcc.isChecked()) {
    		wheelphone.enableSoftAcceleration();
    	} else {
    		wheelphone.disableSoftAcceleration();
    	}
    } 
	
}