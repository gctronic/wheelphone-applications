package com.wheelphone.helpers;

import java.io.IOException;

import org.opencv.core.Mat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

import com.wheelphone.R;
import com.wheelphone.navigator.FragmentNavigator;

public class CameraHandler implements SurfaceHolder.Callback, Camera.PreviewCallback {
	//TODO: Pick dynamically the best preview size.
	//TODO: Set autofocus for preview
	//TODO: give more weight to linear spring, as the image is not squared, but more taller (Eg: 480x640)
	//TODO: Check if we should run the onPreviewFrame in another thread, if we have to...then migrate it to another thread. (THIS CLASS IS ALL ABOUT SPEED)
	//TODO: Allow setting back and front camera
	//TODO: Allow setting a specific resolution
	//TODO: Do the controller better...still have to show errors, but do it in a generic way, so that other apps can work

	private static final String TAG = CameraHandler.class.getName();

	private Context mContext;

	private Camera mCamera;
	private SurfaceHolder mCaptureSurfaceHolder;

	private Size mPreviewSize;

	private byte[] mPreviewBuffer;
	private byte[] mPreviewBufferRotated;

	private boolean mStopThread = false;
	private Thread mThread;

	private Object mLockObject = new Object();

	private FragmentNavigator mController;

	private FrameProcessor mFrameProcessor;
	//	private boolean mIsEnabled;

	public CameraHandler(Context context, SurfaceHolder holder) {
		mContext = context;
		mCaptureSurfaceHolder = holder;
		mCaptureSurfaceHolder.addCallback(this);
	}

	public interface FrameProcessor{
		public void process(byte[] frame, Size frameSize);
		public Mat getRgba();
	}

	public void setController(FragmentNavigator controller){
		mController = controller;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		try {
			setCamera();
		} catch (Exception e) { //catch any exception while trying to setup the camera, and show a friendly error message 
			mController.showText(mContext.getString(R.string.error_camera));
			mCaptureSurfaceHolder.removeCallback(this);
			mCamera = null;
			Log.e(TAG, "Error starting camera preview: " + e.getMessage());
			return;
		}

		Parameters cameraParameters = mCamera.getParameters();
		//				List <Size> sizes = cameraParameters.getSupportedVideoSizes();
		//				Log.d(TAG, "supported sizes: ");
		//				for(Size size : sizes){
		//					Log.d(TAG, size.width + "x" + size.height);
		//				}

//		cameraParameters.setPreviewSize(320, 240);
		cameraParameters.setPreviewSize(352, 288);
		//		cameraParameters.setPreviewSize(640, 480);
		mCamera.setParameters(cameraParameters); 

		mPreviewSize = cameraParameters.getPreviewSize();

		int bufferSize = mPreviewSize.width * mPreviewSize.height * 3;

		mPreviewBuffer = new byte[bufferSize + 4096];
		//Rotated buffer:
		mPreviewBufferRotated = new byte[mPreviewBuffer.length];

		Log.d(TAG, "Starting camera processing thread");
		mStopThread = false;
		mThread = new Thread(new CameraWorker());
		mThread.start();

		//		Log.d(TAG, "prev height: " + mPreviewSize.height); 
		//		Log.d(TAG, "prev width: " + mPreviewSize.width); 
	}

	// If preview changes or rotate, take care of those events here.
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");

		if (mCaptureSurfaceHolder.getSurface() == null || mCamera == null){
			// preview surface or camera does not exist
			return;
		}

		//Stop receiving frames
		mCamera.setPreviewCallbackWithBuffer(null);

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
			Log.e(TAG, "Error starting camera preview: " + e.getMessage());
		}

		//Start face detection 
		mCamera.setPreviewCallbackWithBuffer(this);
		mCamera.addCallbackBuffer(mPreviewBuffer);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//		Log.d(TAG, "surfaceDestroyed");
		//Stop face detection
		mCamera.setPreviewCallbackWithBuffer(null);
		/* 1. We need to stop thread which updating the frames
		 * 2. Stop camera and release it
		 */
		Log.d(TAG, "Disconnecting from camera");
		try {
			mStopThread = true;
			synchronized (mLockObject) {
				Log.d(TAG, "Notify camera thread to close...waiting");
				mLockObject.notify();
			}
			if (mThread != null)
				mThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mThread =  null;
		}
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	@Override
	public void onPreviewFrame(byte[] d, Camera camera) {
		synchronized (mLockObject) {
			mLockObject.notifyAll();
		}
	}

	private class CameraWorker implements Runnable {

		private long mTimestampStart;
		private long mTimestampEnd;

		@Override
		public void run() {
			do {
				rotate();

				mTimestampStart = System.currentTimeMillis();

				if (mFrameProcessor != null)
					mFrameProcessor.process(mPreviewBufferRotated, mPreviewSize);

				mTimestampEnd = System.currentTimeMillis();
//				Log.d(TAG, "Time: " + (mTimestampEnd - mTimestampStart));


				if (!mStopThread){
					synchronized (mLockObject) {
						if (mCamera != null)
							mCamera.addCallbackBuffer(mPreviewBuffer);
						try {
							mLockObject.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} while (!mStopThread);
			Log.d(TAG, "Finish camera thread");
		}
	}

	/*
	 * Rotate the image in the byte array that was filled with the latest frame
	 */
	private void rotate() {
		// Rotate the Y luma
		int i = 0;
		for(int x = 0;x < mPreviewSize.width ;x++) {
			for(int y = mPreviewSize.height-1;y >= 0;y--) {
				mPreviewBufferRotated[i] = mPreviewBuffer[y*mPreviewSize.width+x];
				i++;
			}
		}
		// Rotate the U and V color components 
		i = mPreviewSize.width*mPreviewSize.height*3/2-1;
		for(int x = mPreviewSize.width-1;x > 0;x=x-2) {
			for(int y = 0;y < mPreviewSize.height/2;y++) {
				mPreviewBufferRotated[i] = mPreviewBuffer[(mPreviewSize.width*mPreviewSize.height)+(y*mPreviewSize.width)+x];
				i--;
				mPreviewBufferRotated[i] = mPreviewBuffer[(mPreviewSize.width*mPreviewSize.height)+(y*mPreviewSize.width)+(x-1)];
				i--;
			}
		}
	}

	//Try to set the camera
	private void setCamera() throws IOException, RuntimeException {
		PackageManager pm = mContext.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		} else {
			mController.showText(mContext.getString(R.string.error_no_camera));
		}

		mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
	}

	public void setFrameProcessor(FrameProcessor processor) {
		mFrameProcessor = processor;
	}
}
