package com.wheelphone.pet.util;

import android.app.ActionBar;
import android.os.Handler;
import android.view.View;

public class Fullscreen {
	private static final String TAG = Fullscreen.class.getName();

	private View mContentView;
	private static final int HIDE_DELAY = 3000;
	private boolean mIsHidden = false;

	private ActionBar mActionBar;

	public Fullscreen(View contentView, ActionBar actionBar) {
		mActionBar = actionBar;
		mContentView = contentView;
		mContentView.setOnSystemUiVisibilityChangeListener(mSystemUiVisibilityChangeListener);

		// Set up the user interaction to manually show or hide the system UI.
		mContentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mIsHidden){
					show();
				}
			}
		});
	}
	public void resume() {
		delayedHide(HIDE_DELAY);
	}

	private synchronized void hide() {
		if (mIsHidden)
			return;
		mActionBar.hide();
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		mIsHidden = true;
	}

	public synchronized void show() {
		if (!mIsHidden)
			return;
		mActionBar.show();
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		mIsHidden = false;
		delayedHide(HIDE_DELAY);
	}

	/*
	 * After hiding the navigation bar, tapping in the app brings up the navigation bar again (partial show), so show the rest of the components
	 */
	private View.OnSystemUiVisibilityChangeListener mSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
		@Override
		public void onSystemUiVisibilityChange(int vis) {
//			Log.d(TAG, "onSystemUiVisibilityChange: " + ((vis & (View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)) == 0));
//			Log.d(TAG, "onSystemUiVisibilityChange2: " + ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0));
//			Log.d(TAG, "vis: " + vis);

			if (((vis & (View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)) == 0) || (vis == View.SYSTEM_UI_FLAG_LOW_PROFILE)) {
				show();
			}
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hide();
		}
	};

	public synchronized void hideActionBar() {
		mIsHidden = true;
		mHideHandler.removeCallbacks(mHideRunnable);
		mActionBar.hide();
	}
}
