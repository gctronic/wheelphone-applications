package com.wheelphone.faceme;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.wheelphone.util.Fullscreen;

public class ActivityMain extends Activity {

	private static final String TAG = ActivityMain.class.getName();

	private View mContentView;
	private View mControlsView;

	private Fullscreen mFullscreen;

	private FragmentFaceme mFragmentNavigator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Make sure that the app stays open:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.activity_main);

		mFragmentNavigator = new FragmentFaceme();
//		fragment.setRetainInstance(true);
		getFragmentManager().beginTransaction().replace(R.id.frame_preview, mFragmentNavigator).commit();

		mContentView = findViewById(R.id.frame_preview);
		mControlsView = findViewById(R.id.frame_controls);
		
		mFullscreen = new Fullscreen(mContentView, mControlsView, getActionBar());

	}    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Fragment fragment = null;
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_about:
	        	Log.d(TAG, "about");
	        	fragment = new FragmentAbout();
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
		if(fragment != null){
			mFullscreen.hideActionBar();
			mControlsView.setVisibility(View.GONE);
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.addToBackStack(null).replace(R.id.frame_preview, fragment).commit();
		}
		return true;
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
            	Log.d(TAG, "back to initial fragment");
            	mFullscreen.show();
            }
        }
    };
}
