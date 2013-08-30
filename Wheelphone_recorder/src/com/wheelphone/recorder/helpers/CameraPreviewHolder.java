package com.wheelphone.recorder.helpers;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.wheelphone.recorder.FragmentRecorder;
import com.wheelphone.recorder.R;

public class CameraPreviewHolder extends SurfaceView implements SurfaceHolder.Callback{
	//TODO: Pick dynamically the best preview size.
	//TODO: Set autofocus for preview
	//TODO: give more weight to linear spring, as the image is not squared, but more taller (Eg: 480x640)

	private static final String TAG = CameraPreviewHolder.class.getName();

	private Context mContext;

	private Camera mCamera;
	MediaRecorder mMediaRecorder;
	
	private SurfaceHolder mCaptureSurfaceHolder;
	private int mLeftSpeed = 0;
	private int mRightSpeed = 0;

	private FragmentRecorder mController;

	private volatile boolean mIsRecording;

	public CameraPreviewHolder(Context context) { 
		super(context);
		init(context);
	}

	public CameraPreviewHolder(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CameraPreviewHolder(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		mCaptureSurfaceHolder = getHolder();
		mCaptureSurfaceHolder.addCallback(this);
	}

	public void setController(FragmentRecorder controller){
		mController = controller;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		try {
			setCamera();
		} catch (Exception e) {
			mController.showText(mContext.getString(R.string.error_no_camera));
			mCaptureSurfaceHolder.removeCallback(this);
			mCamera = null;
			return;
		}

		Parameters cameraParameters = mCamera.getParameters();
		//		List <Size> sizes = cameraParameters.getSupportedVideoSizes();
		//		Log.d(TAG, "supported sizes: ");
		//		for(Size size : sizes){
		//			Log.d(TAG, size.width + "x" + size.height);
		//		}

		cameraParameters.setPreviewSize(352, 288);
		mCamera.setParameters(cameraParameters);

		setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {

		            if (mIsRecording) {
		            	mController.setSpeed(0, 0);
		                // stop recording and release camera
		                mMediaRecorder.stop();  // stop the recording
		                releaseMediaRecorder(); // release the MediaRecorder object
		                mCamera.lock();         // take camera access back from MediaRecorder

		                // inform the user that recording has stopped
		                Log.d(TAG, "stopped");
		                mIsRecording = false;
		            } else {
		                // initialize video camera
		                if (prepareVideoRecorder()) {
		                	mController.setSpeed(20, 20);
		                    // Camera is available and unlocked, MediaRecorder is prepared,
		                    // now you can start recording
		                    mMediaRecorder.start();

		                    // inform the user that recording has started
		                    Log.d(TAG, "started");
		                    mIsRecording = true;
		                } else {
		                    // prepare didn't work, release the camera
		                    releaseMediaRecorder();
		                    // inform user
		                }
		            }
				}
				return true;
			}
		});
	}
	
	private boolean prepareVideoRecorder(){
	    mMediaRecorder = new MediaRecorder();

	    // Step 1: Unlock and set camera to MediaRecorder
	    mCamera.unlock();
	    mMediaRecorder.setCamera(mCamera);

	    // Step 2: Set sources
	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    // Step 2.5: Set resolution and framerate
//	    mMediaRecorder.setVideoSize(640, 480);
	    
	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
	    mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
	    mMediaRecorder.setVideoFrameRate(15);
	    mMediaRecorder.setOrientationHint(90);

	    // Step 4: Set output file
	    mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/DCIM/wheelphone_capture.mp4");

	    // Step 5: Set the preview output
	    mMediaRecorder.setPreviewDisplay(mCaptureSurfaceHolder.getSurface());

	    // Step 6: Prepare configured MediaRecorder
	    try {
	        mMediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    } catch (IOException e) {
	        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    }
	    return true;
	}

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
        	mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    
	// If preview changes or rotate, take care of those events here.
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");

		if (mCaptureSurfaceHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){ /*ignore: tried to stop a non-existent preview*/ }

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
			mCamera.startPreview();
			mCamera.setDisplayOrientation(90);
		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
        releaseMediaRecorder();
        releaseCamera();
	}

	//Try to set the camera
	private void setCamera() throws IOException, RuntimeException{
		PackageManager pm = mContext.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		}
		mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
	}

	public void stopTracking() {
		mLeftSpeed = 0;
		mRightSpeed = 0;
	}
}