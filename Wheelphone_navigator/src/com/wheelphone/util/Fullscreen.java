package com.wheelphone.util;

import android.app.ActionBar;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class Fullscreen {
	private static final String TAG = Fullscreen.class.getName();

	private View mContentView;
	private View mControlsView;
	private static final int HIDE_DELAY = 3000;
	private volatile boolean mIsHidden = false;

	private ActionBar mActionBar;

	public Fullscreen(View contentView, View controlsView, ActionBar actionBar) {
		mActionBar = actionBar;
		mContentView = contentView;
		mControlsView = controlsView;
		mContentView.setOnSystemUiVisibilityChangeListener(mSystemUiVisibilityChangeListener);

		// Set up the user interaction to manually show or hide the system UI.
		mContentView.setOnTouchListener(mTouchListener);
	}
	
	private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			if (event.getAction() != MotionEvent.ACTION_DOWN)
				return false;
			
			if (!mIsHidden)
				hide();
			else
				show();
			return false;
		}
	};

	public void resume() {
		delayedHide(HIDE_DELAY);
	}
	
	private synchronized void hide() {
		if (mIsHidden)
			return;
		mIsHidden = true;
		mHideHandler.removeCallbacks(mHideRunnable);
		mActionBar.hide();
		mControlsView.setVisibility(View.GONE);
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//		Log.d(TAG, "hide: " + mIsHidden);
	}

	public synchronized void show() {
		if (!mIsHidden)
			return;
		mIsHidden = false;
		mActionBar.show();
		mControlsView.setVisibility(View.VISIBLE);
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
//		Log.d(TAG, "show: " + mIsHidden);
	}

	/*
	 * After hiding the navigation bar, tapping in the app brings up the navigation bar again (partial show), so show the rest of the components
	 */
	private View.OnSystemUiVisibilityChangeListener mSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
		@Override
		public void onSystemUiVisibilityChange(int vis) {
//			Log.d(TAG, "View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION: " + (View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION));
//			Log.d(TAG, "View.SYSTEM_UI_FLAG_LOW_PROFILE: " + (View.SYSTEM_UI_FLAG_LOW_PROFILE));
//			Log.d(TAG, "View.SYSTEM_UI_FLAG_VISIBLE: " + (View.SYSTEM_UI_FLAG_VISIBLE));
//			Log.d(TAG, "vis: " + vis);

			//returned from having the navigation controls hidden, so show the rest of the UI:
			if ((vis == View.SYSTEM_UI_FLAG_LOW_PROFILE) || (vis == View.SYSTEM_UI_FLAG_VISIBLE)) {
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
