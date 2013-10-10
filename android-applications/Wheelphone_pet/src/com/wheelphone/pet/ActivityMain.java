package com.wheelphone.pet;

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

import com.wheelphone.pet.util.Fullscreen;

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

		getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPet()).commit();

		mContentView = findViewById(R.id.content_frame);
		
		mFullscreen = new Fullscreen(mContentView, getActionBar());

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
	        case R.id.action_settings:
	        	Log.d(TAG, "settings");
	        	fragment = new FragmentPreferences();
	        	break;
	        case R.id.action_about:
	        	Log.d(TAG, "about");
	        	fragment = new FragmentAbout();
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
		if(fragment != null){
			mFullscreen.hideActionBar();
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.addToBackStack(null).replace(R.id.content_frame, fragment).commit();
		}
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mFullscreen.resume();
	}
	
	private  OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {
		@Override
        public void onBackStackChanged() {
        	Log.d(TAG, "getFragmentManager().getBackStackEntryCount(): " + getFragmentManager().getBackStackEntryCount());
            if (getFragmentManager().getBackStackEntryCount() == 0) {
            	Log.d(TAG, "back to petfragment");
            	mFullscreen.show();
            }
        }
    };
}
