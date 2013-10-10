package com.wheelphone.pet.helpers;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wheelphone.facetrack.FaceDetector;
import com.wheelphone.pet.FragmentPet;
import com.wheelphone.pet.R;

public class FaceTracking extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	//TODO: Pick dynamically the best preview size.
	//TODO: Set autofocus for preview
	//TODO: give more weight to linear spring, as the image is not squared, but more taller (Eg: 480x640)
	//TODO: Check if we should run the onPreviewFrame in another thread, if we have to...then migrate it to another thread. (THIS CLASS IS ALL ABOUT SPEED)

	private static final String TAG = FaceTracking.class.getName();

	private Context mContext;

	private Camera mCamera;
	private SurfaceHolder mCaptureSurfaceHolder;
	private float mDesiredEyesDist = 0;

	private Size mPreviewSize;

	private FragmentPet mController;

	private byte[] mPreviewBuffer;
	private byte[] mRotatedPreviewBuffer;

	private static final int NUM_FACES = 1; // max is 64

	private FaceDetector mFaceDetector;
	private FaceDetector.Face mFaces[];
	private FaceDetector.Face mFace = null;
	private float mEyesDistance;

	private int mCameraDirection = 0;

	private FaceTrackingListener mListener;

	private long mTimestampLastDetected;
	private long mTimestampLastNotDetected;

	private boolean mStopThread = false;
	private Thread mThread;
	
	private Object mLockObject = new Object();


	public static final String INTENT_NOFACE = FaceTracking.class.getName() + ".FACE_NOT_DETECTED";
	public static final String INTENT_FACEMOVES = FaceTracking.class.getName() + ".FACE_MOVED";

	private static final int CAMERA_FRONT_DIRECTION = 0;
	private static final int CAMERA_BACK_DIRECTION = 1;

	public FaceTracking(Context context) { 
		super(context);
		init(context);
	}

	public FaceTracking(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public FaceTracking(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		mCaptureSurfaceHolder = getHolder();
		mCaptureSurfaceHolder.addCallback(this);
	}

	public void setController(FragmentPet controller){
		mController = controller;
	}

	/*
	 * Interface that should be implemented by classes that would like to be notified by face tracking events. (Observer pattern)
	 */
	public interface FaceTrackingListener{
		public void onFaceDetected(FaceDetector.Face face);
		public void onFaceNotDetected();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//		Log.d(TAG, "surfaceCreated");
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
		//		List <Size> sizes = cameraParameters.getSupportedVideoSizes();
		//		Log.d(TAG, "supported sizes: ");
		//		for(Size size : sizes){
		//			Log.d(TAG, size.width + "x" + size.height);
		//		}

		cameraParameters.setPreviewSize(352, 288);
		mCamera.setParameters(cameraParameters); 

		mPreviewSize = cameraParameters.getPreviewSize();
		
		int bufferSize = mPreviewSize.width * mPreviewSize.height * 3;

		mPreviewBuffer = new byte[bufferSize + 4096];
		//Rotated buffer is only one channel (grayscale)
		mRotatedPreviewBuffer = new byte[mPreviewSize.width * mPreviewSize.height];

		mFaceDetector = new FaceDetector(mPreviewSize.height, mPreviewSize.width, NUM_FACES);

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
		//		Log.d(TAG, "surfaceChanged");

		if (mCaptureSurfaceHolder.getSurface() == null || mCamera == null){
			// preview surface or camera does not exist
			return;
		}

		//Stop face detection
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
//			Log.d(TAG, "notify");
		}
	}

	private class CameraWorker implements Runnable {
		@Override
		public void run() {
			do {
				//		long start = System.currentTimeMillis();

				mFaces = new FaceDetector.Face[NUM_FACES];

				if (mCameraDirection == CAMERA_FRONT_DIRECTION) {
					rotateCounterClockwise();
				} else if (mCameraDirection == CAMERA_BACK_DIRECTION) {
					rotateClockwise();
				}

				mFaceDetector.findFaces(mRotatedPreviewBuffer, mFaces);

				mFace = mFaces[0];
				if (mFace != null){
					mTimestampLastDetected = System.currentTimeMillis();
					if (System.currentTimeMillis() - mTimestampLastNotDetected > 200){ //Only process the face if it has been seen for some time (200ms)
						
						mEyesDistance = mFace.eyesDistance();

						if (mDesiredEyesDist == 0){
							Log.d(TAG, "Tracking new face, with eye distance: " + mEyesDistance);

							mDesiredEyesDist = mEyesDistance;
						}

						//Update the desired distance of the face
						mFace.setDesiredEyesDist(mDesiredEyesDist);

						//Notify interested parts!
						if(mListener!=null) mListener.onFaceDetected(mFace);
					}
				} else {
					mTimestampLastNotDetected = System.currentTimeMillis();
					if (mDesiredEyesDist != 0 && System.currentTimeMillis() - mTimestampLastDetected > 500){ //Only forget the eyes distance if more than half a second has passed since we last saw the face
						mDesiredEyesDist = 0;
						//Notify interested parts!
						if(mListener!=null) mListener.onFaceNotDetected();
					}
				}

				//		long end = System.currentTimeMillis();
				//		Log.d(TAG, "Time: " + (end - start));

				if (!mStopThread){
					synchronized (mLockObject) {
						if (mCamera != null)
							mCamera.addCallbackBuffer(mPreviewBuffer);
						try {
//							Log.d(TAG, "waiting");
							mLockObject.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} while (!mStopThread);
			Log.d(TAG, "Finish camera thread");
		}
	}

	//Even though they are small, lets assign them outside.
	private int _i, _x, _y;

	//Rotation also extracts the Y from the YUV (NV21) frame. The first mPreviewSize.width*mPreviewSize.height bytes of the frame are the Y.
	private void rotateCounterClockwise() {
		//Rotate frame 90 degrees counter clockwise:
		_i = 0;
		for(_x=mPreviewSize.width-1 ; _x>=0 ;_x--){
			for(_y=0 ; _y<mPreviewSize.height ; _y++){
				mRotatedPreviewBuffer[_i] = mPreviewBuffer[_y*mPreviewSize.width+_x];
				_i++;
			}
		}
	}
	private void rotateClockwise() {
		//Rotate frame 90 degrees clockwise:
		_i = 0;
		for(_x=0 ; _x<mPreviewSize.width ;_x++){
			for(_y=mPreviewSize.height-1 ; _y>=0 ; _y--){
				mRotatedPreviewBuffer[_i] = mPreviewBuffer[_y*mPreviewSize.width+_x];
				_i++;
			}
		}
	}

	//Try to set the camera
	private void setCamera() throws IOException, RuntimeException {
		PackageManager pm = mContext.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
			mCameraDirection = CAMERA_FRONT_DIRECTION;
		} else if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			mCameraDirection = CAMERA_BACK_DIRECTION;
			mController.showText(mContext.getString(R.string.error_no_front_facing_camera));
		}

		mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
	}

	/*
	 * Observer pattern glue code:
	 */
	public void setFaceTrackingListener(FaceTrackingListener eventListener) {
		Log.d(TAG, "setFaceTrackingListener");
		mListener = eventListener;
	}

	public void removeFaceTrackingListener() {
		Log.d(TAG, "removeFaceTrackingListener");
		mDesiredEyesDist = 0;
		mListener = null;
	}
}
