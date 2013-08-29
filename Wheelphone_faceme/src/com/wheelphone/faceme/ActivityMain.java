package com.wheelphone.faceme;

import android.app.Activity;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.wheelphone.util.Fullscreen;

public class ActivityMain extends Activity {

	private static final String TAG = ActivityMain.class.getName();

	private View mContentView;

	private Fullscreen mFullscreen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.activity_main);

		getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentFaceme()).commit();

		mContentView = findViewById(R.id.content_frame);
		
		mFullscreen = new Fullscreen(mContentView, getActionBar());

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mFullscreen.resume();
	}
	
	private  OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {    
        public void onBackStackChanged() {
        	Log.d(TAG, "getFragmentManager().getBackStackEntryCount(): " + getFragmentManager().getBackStackEntryCount());
            if (getFragmentManager().getBackStackEntryCount() == 0) {
            	Log.d(TAG, "back to petfragment");
            	mFullscreen.show();
            }
        }
    };
}
