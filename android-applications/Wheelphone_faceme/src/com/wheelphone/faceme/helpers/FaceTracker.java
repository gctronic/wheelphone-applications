package com.wheelphone.faceme.helpers;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wheelphone.faceme.FragmentFaceme;
import com.wheelphone.faceme.R;
import com.wheelphone.facetrack.FaceDetector;

public class FaceTracker extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	//TODO: Pick dynamically the best preview size.
	//TODO: Set autofocus for preview
	//TODO: give more weight to linear spring, as the image is not squared, but more taller (Eg: 480x640)

	private static final String TAG = FaceTracker.class.getName();

	private static final int CAMERA_FRONT_DIRECTION = 0;
	private static final int CAMERA_BACK_DIRECTION = 1;

	private Context mContext;

	private Camera mCamera;
	private SurfaceHolder mCaptureSurfaceHolder;
	private int mLeftSpeed = 0;
	private int mRightSpeed = 0;
	private float mDesiredEyesDist = 0;

	private static final double DAMPING_FACTOR = 1;
	private static final int LINEAR_SPRING_CONST = 60;//60
	private static final int ANGULAR_SPRING_CONST = 25;//25

	private android.hardware.Camera.Size mPreviewSize;

	private FragmentFaceme mController;

	private byte[] mPreviewBuffer;
	private byte[] mRotatedPreviewBuffer;

	private float mFaceDistToCenterX;
	private float mFaceDistToCenterY;


	private static final int NUM_FACES = 1; // max is 64

	private FaceDetector mFaceDetector;
	private FaceDetector.Face mFaces[];
	private FaceDetector.Face mFace = null;
	private PointF mEyesMP = new PointF();
	private float mEyesDistance;

	private int mCameraDirection = 0;

	public FaceTracker(Context context) { 
		super(context);
		init(context);
	}

	public FaceTracker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public FaceTracker(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		mCaptureSurfaceHolder = getHolder();
		mCaptureSurfaceHolder.addCallback(this);
	}

	public void setController(FragmentFaceme controller){
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

		mPreviewSize = cameraParameters.getPreviewSize();
		int bufferSize = mPreviewSize.width * mPreviewSize.height * 3;

		mPreviewBuffer = new byte[bufferSize + 4096];
		mRotatedPreviewBuffer = new byte[bufferSize + 4096];

		mFaceDetector = new FaceDetector(mPreviewSize.height, mPreviewSize.width, NUM_FACES );

		Log.d(TAG, "prev height: " + mPreviewSize.height); 
		Log.d(TAG, "prev width: " + mPreviewSize.width); 
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
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}

		//Start face detection 
		mCamera.setPreviewCallbackWithBuffer(this);
		mCamera.addCallbackBuffer(mPreviewBuffer);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		//Stop face detection
		mCamera.setPreviewCallbackWithBuffer(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	@Override
	public void onPreviewFrame(byte[] d, Camera camera) {
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
			mFace.getMidPoint(mEyesMP);
			mEyesDistance = mFace.eyesDistance();

			if (mDesiredEyesDist == 0){
				mDesiredEyesDist = mEyesDistance;
			}

			//calculate the face position (in range of -1000 to 1000) with respect to the center of the image, taking in consideration the preview resolution.
			//Since we rotated the image, we also rotate the width and height here:
			mFaceDistToCenterX = 2*(mEyesMP.x/mPreviewSize.height) - 1;
			mFaceDistToCenterY = -2*(mEyesMP.y/mPreviewSize.width) + 1;

			Log.d(TAG, "Center: [" + mFaceDistToCenterX + ", " + mFaceDistToCenterY + "]");
//			Log.d(TAG, "Eyes dist: [" + mEyesDistance + "]");

			// Face coordinates range: [-1, 1]:
			float angularAcc = mFaceDistToCenterX;

			//Spring damping system (scaled)
			float linearAcc = (mEyesDistance - mDesiredEyesDist) / mDesiredEyesDist;

			Log.d(TAG, "angularAcc: " + angularAcc + ". linearAcc: " + linearAcc);
			
			mLeftSpeed = (int)(mLeftSpeed
					+   linearAcc * LINEAR_SPRING_CONST
					+	angularAcc * ANGULAR_SPRING_CONST
					-	DAMPING_FACTOR * mLeftSpeed);
			
			mRightSpeed = (int)(mRightSpeed 
					+   linearAcc * LINEAR_SPRING_CONST
					-	angularAcc * ANGULAR_SPRING_CONST
					-	DAMPING_FACTOR * mRightSpeed);

		} else { //No face, so stop:
			stopTracking();
		}
		mController.setSpeed(mLeftSpeed, mRightSpeed);
		if (mCamera != null)
			mCamera.addCallbackBuffer(mPreviewBuffer);
		//		long end = System.currentTimeMillis();
		//		Log.d(TAG, "Time: " + (end - start));
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
	private void setCamera() throws IOException, RuntimeException{
		PackageManager pm = mContext.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
			mCameraDirection = CAMERA_FRONT_DIRECTION;
		} else if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			mCameraDirection = CAMERA_BACK_DIRECTION;
		}

		mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
	}

	public void stopTracking() {
		mDesiredEyesDist = 0;
		mLeftSpeed = 0;
		mRightSpeed = 0;
	}
}