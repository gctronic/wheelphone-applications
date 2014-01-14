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

package com.wheelphone.remote;

import java.io.IOException;
import java.util.Timer;
import com.wheelphone.remote.WheelphoneRemote;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot.WheelPhoneRobotListener;

import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class WheelphoneRemote extends Activity implements OnSharedPreferenceChangeListener, WheelPhoneRobotListener {
    
	// Various
	private static String TAG = WheelphoneRemote.class.getName();
	Timer timer = new Timer(), timerImg = new Timer();
	boolean getFirmwareFlag = true;
	boolean optionsStarted = false;
	
	// motion
	public static final int MIN_SPEED = -350;
	public static final int MAX_SPEED = 350;
	public static final int SPEED_STEP = 20;	//mm/s
	public static final int ROTATION_STEP = 10;
	public static final int MAX_SPEED_INDEX = MAX_SPEED/SPEED_STEP;
	public int currentSpeed=0;
	public int currentSpeedIndex=0;
	public int currentRotationIndex=0;
	public static final int NO_ROTATION = 0;
	public static final int ROTATE_LEFT = 1;
	public static final int ROTATE_RIGHT = 2;
	public int pivotType = NO_ROTATION;
	public int pivotCounter = 0;

	// http server
    private CustomHttpServer httpServer = null;
    private PowerManager.WakeLock wl;
    private RtspServer rtspServer = null;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView line1, line2, version, signWifi, signStreaming;
    private ImageView buttonSettings, buttonClient;
    private LinearLayout signInformation;
    private Context context;
    private Animation pulseAnimation;

	// Robot state
	WheelphoneRobot wheelphone;
	private int firmwareVersion=0;
	private int lSpeed=0, rSpeed=0;
	
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        context = this.getApplicationContext();
        line1 = (TextView)findViewById(R.id.line1);
        line2 = (TextView)findViewById(R.id.line2);
        version = (TextView)findViewById(R.id.version);
        buttonSettings = (ImageView)findViewById(R.id.button_settings);
        buttonClient = (ImageView)findViewById(R.id.button_client);
        signWifi = (TextView)findViewById(R.id.advice);
        signStreaming = (TextView)findViewById(R.id.streaming);
        signInformation = (LinearLayout)findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        H264Stream.setPreferences(settings);
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "com.wheelphone.remote");
    
        
        Session.setSurfaceHolder(holder);
        Session.setHandler(handler);
        Session.setDefaultAudioEncoder(settings.getBoolean("stream_audio", false)?Integer.parseInt(settings.getString("audio_encoder", "3")):0);
        Session.setDefaultVideoEncoder(settings.getBoolean("stream_video", true)?Integer.parseInt(settings.getString("video_encoder", "2")):0);
        Session.setDefaultVideoQuality(new VideoQuality(settings.getInt("video_resX", 0), 
        		settings.getInt("video_resY", 0), 
        		Integer.parseInt(settings.getString("video_framerate", "0")), 
        		Integer.parseInt(settings.getString("video_bitrate", "0"))*1000));
        
        rtspServer = new RtspServer(8086, handler);
        httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);

        buttonSettings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
	            // Starts QualityListActivity where user can change the quality of the stream
				Intent intent = new Intent(context,OptionsActivity.class);
	            startActivityForResult(intent, 0);
			}
		});        
        buttonClient.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Starts ClientActivity, the user can then capture the stream from another phone running Spydroid
	            Intent intent = new Intent(context,ClientActivity.class);
	            startActivityForResult(intent, 0);
			}
		});      

		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);		
		
        wheelphone = new WheelphoneRobot(getApplicationContext(), getIntent());
        wheelphone.enableSpeedControl();  
        wheelphone.setWheelPhoneRobotListener(this);
		
    }
    
    // Save preferences when modified in the OptionsActivity
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals("video_resX")) {
    		Session.defaultVideoQuality.resX = sharedPreferences.getInt("video_resX", 0);
    	}
    	else if (key.equals("video_resY"))  {
    		Session.defaultVideoQuality.resY = sharedPreferences.getInt("video_resY", 0);
    	}
    	else if (key.equals("video_framerate")) {
    		Session.defaultVideoQuality.frameRate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
    	}
    	else if (key.equals("video_bitrate")) {
    		Session.defaultVideoQuality.bitRate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
    	}
    	else if (key.equals("stream_audio") || key.equals("audio_encoder")) { 
    		Session.setDefaultAudioEncoder(sharedPreferences.getBoolean("stream_audio", true)?Integer.parseInt(sharedPreferences.getString("audio_encoder", "3")):0);
    	}
    	else if (key.equals("stream_video") || key.equals("video_encoder")) {
    		Session.setDefaultVideoEncoder(sharedPreferences.getBoolean("stream_video", true)?Integer.parseInt(sharedPreferences.getString("video_encoder", "2")):0);
    	}
    	else if (key.equals("enable_http")) {
    		if (sharedPreferences.getBoolean("enable_http", true)) {
    			if (httpServer == null) httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);
    		} else {
    			if (httpServer != null) httpServer = null;
    		}
    	}
    	else if (key.equals("enable_rtsp")) {
    		if (sharedPreferences.getBoolean("enable_rtsp", true)) {
    			if (rtspServer == null) rtspServer = new RtspServer(8086, handler);
    		} else {
    			if (rtspServer != null) rtspServer = null;
    		}
    	}	
    }
    
    public void onStart() {
    	super.onStart();  	

    	// Lock screen
    	wl.acquire();
    	
    	Intent notificationIntent = new Intent(this, WheelphoneRemote.class);
    	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setContentIntent(pendingIntent)
    	        .setWhen(System.currentTimeMillis())
    	        .setTicker(getText(R.string.notification_title))
    	        .setSmallIcon(R.drawable.wheelphone_logo_remote_small)
    	        .setContentTitle(getText(R.string.notification_title))
    	        .setContentText(getText(R.string.notification_content));
    	Notification notification = builder.getNotification();
    	notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
    	
    }
    	
    public void onStop() {
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
    	super.onResume();
    	
    	if(!optionsStarted) {
    		wheelphone.startUSBCommunication();
    	}
    	
    	// Determines if user is connected to a wireless network & displays ip 
    	if (!streaming) displayIpAddress();
    	startServers();
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
 
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {	    
		//Call the super function that we are over writing now that we have saved our data.
		super.onSaveInstanceState(savedInstanceState);
    }
    
    public void onPause() {
    	super.onPause();
    	if (rtspServer != null) rtspServer.stop();
    	CustomHttpServer.setScreenState(false);
    	unregisterReceiver(wifiStateReceiver);
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	// Remove notification
    	((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    	if (httpServer != null) httpServer.stop();
    	if (rtspServer != null) rtspServer.stop();
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
    	Intent intent;
    	
        switch (item.getItemId()) {
        /*case R.id.client:
            // Starts ClientActivity where user can view stream from another phone
            intent = new Intent(this.getBaseContext(),ClientActivity.class);
            startActivityForResult(intent, 0);
            return true;*/
        case R.id.options:
        	optionsStarted = true;
            // Starts QualityListActivity where user can change the streaming quality
            intent = new Intent(this.getBaseContext(),OptionsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        case R.id.quit:
        	// Quits Spydroid i.e. stops the HTTP server
        	wheelphone.closeUSBCommunication();
        	if (httpServer != null) httpServer.stop();
        	finish();	
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void startServers() {
    	if (rtspServer != null) {
    		try {
    			rtspServer.start();
    		} catch (IOException e) {
    			log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
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
    			currentSpeedIndex = 0;
    			currentRotationIndex = 0;
    			pivotCounter = 0;
    			pivotType = NO_ROTATION;
    			break;
    		case CustomHttpServer.INC_SPEED:
    	    	if(currentSpeed < (MAX_SPEED-10)) {
    	    		currentSpeed+=10;
    	    	} else {
    	    		currentSpeed = MAX_SPEED;
    	    	}
    			break;
    		case CustomHttpServer.DEC_SPEED:
    	    	if(currentSpeed > (MIN_SPEED+10)) {
    	    		currentSpeed-=10;
    	    	} else {
    	    		currentSpeed = MIN_SPEED;
    	    	}
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
    		case CustomHttpServer.ROTATE_LEFT:
    			pivotType = ROTATE_LEFT;
    			pivotCounter += 9;	// (should be 7 theoretically) 45 degrees CCW with speed l=-50, r=15  			
    			break;    
    		case CustomHttpServer.ROTATE_RIGHT:
    			pivotType = ROTATE_RIGHT;
    			pivotCounter -= 9;	// 45 degrees CW with speed l=15, r=-50     			
    			break;  
    			
    		case RtspServer.MESSAGE_LOG:
    			log((String)msg.obj);
    			break;
    		case RtspServer.MESSAGE_ERROR:
    			log((String)msg.obj);
    			break;
    		case Session.MESSAGE_START:
    			streaming = true;
    			streamingState(1);
    			break;
    		case Session.MESSAGE_STOP:
    			streaming = false;
    			displayIpAddress();
    			break;
    		case Session.MESSAGE_ERROR:
    			log((String)msg.obj);
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
    		
			rSpeed *= SPEED_STEP;
			lSpeed *= SPEED_STEP;
			
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
			
			wheelphone.setRawLeftSpeed(lSpeed);
			wheelphone.setRawRightSpeed(rSpeed);
			
    	} //handle_message
    	
    };
       
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
    
	public void onWheelphoneUpdate() {
		if(getFirmwareFlag) {
			firmwareVersion=wheelphone.getFirmwareVersion();
			if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
					getFirmwareFlag = false;
					if(firmwareVersion >= 3) {
						Toast.makeText(WheelphoneRemote.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
						//msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
					} else {
						//Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
						msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
					}
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

	}
	
    
}