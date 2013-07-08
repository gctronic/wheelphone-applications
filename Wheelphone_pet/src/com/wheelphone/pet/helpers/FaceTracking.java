package com.wheelphone.pet.helpers;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wheelphone.pet.FragmentPet;
import com.wheelphone.pet.R;

public class FaceTracking extends SurfaceView implements SurfaceHolder.Callback {
	private static String TAG = FaceTracking.class.getName();

	private Camera mCamera;
	private SurfaceHolder mCaptureSurfaceHolder;
	private boolean mPreviewing = false;
	private int mLeftSpeed = 0;
	private int mRightSpeed = 0;
	private int mCurrDesiredHeight = 0;

	private FragmentPet mController;
	
	private Context mContext;

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
		this.mController = controller;
//		if (mError != null) {
//			mController.showError(mError);
//		} else {
//			mController.updateOutput(0,0);
//		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(mPreviewing){
			mCamera.stopFaceDetection();
			mCamera.stopPreview();
			mPreviewing = false;
		}

		if (mCamera != null){
			try {
				mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
				mCamera.startPreview();

				mCamera.setDisplayOrientation(90);

				//TODO: Pick dynamically the best preview size.

				Log.d(TAG, "This camera can detect up to " + mCamera.getParameters().getMaxNumDetectedFaces() + " faces");
				mCamera.startFaceDetection();
				mPreviewing = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		if (setCamera()){//If can set camera, furthermore set the face detection listener
			mCamera.setFaceDetectionListener(faceDetectionListener);
		} else {
			mCaptureSurfaceHolder.removeCallback(this);
		}
		
		//		Camera.Parameters param = camera.getParameters();

		//		Log.d(TAG, "preferred height: " + param.getPreferredPreviewSizeForVideo().height); 
		//		Log.d(TAG, "preferred width: " + param.getPreferredPreviewSizeForVideo().width); 
		//		Log.d(TAG, "surf width: " + holder.getSurfaceFrame().width()); 
		//		Log.d(TAG, "surf height: " + holder.getSurfaceFrame().height());
		//		Log.d(TAG, "prev height: " + param.getPreviewSize().height); 
		//		Log.d(TAG, "prev width: " + param.getPreviewSize().width); 

		//		param.setPreviewSize(param.getPreferredPreviewSizeForVideo().width, param.getPreferredPreviewSizeForVideo().height);

		//		Log.d(TAG, "prev height: " + param.getPreviewSize().height); 
		//		Log.d(TAG, "prev width: " + param.getPreviewSize().width); 

		//		List<int[]> fpslist = param.getSupportedPreviewFpsRange();
		//		Log.d(TAG, "size= " + fpslist.size());
		//		for (int i=0;i < fpslist.size();i++) {
		//			fpslist.get(i);
		//			Log.d(TAG, i + " fps= " + fpslist.get(i)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);
		//			Log.d(TAG, i + " fps= " + fpslist.get(i)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
		//		}

		//		camera.setParameters(param);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera.stopFaceDetection();
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
		mPreviewing = false;
	}

	FaceDetectionListener faceDetectionListener = new FaceDetectionListener(){
		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {
			if (faces.length > 0){
				if (mCurrDesiredHeight == 0){
					mCurrDesiredHeight = faces[0].rect.height();
				}

				// Face coordinates range from (-1000, 1000). Scale it to -64 to 64 (robot supports -127 to 127, but that is too fast and can easily overshoot):
				int faceYPos = 32 * faces[0].rect.centerY() / 1000;

				//Spring damping system
				int linearAcc = 64 * (faces[0].rect.height() - mCurrDesiredHeight) / mCurrDesiredHeight;
				int linearSpringConst = 1;
				int angularSpringConst = 1;
				int angularAcc = faceYPos;


				int dampingNumerator = 1;
				int dampingDenominator = 1;

				mLeftSpeed = mLeftSpeed 
						+   linearAcc * linearSpringConst
						+	angularAcc * angularSpringConst
						-	dampingNumerator * mLeftSpeed
						/ dampingDenominator;

				mRightSpeed = mRightSpeed 
						+   linearAcc * linearSpringConst
						-	angularAcc * angularSpringConst
						-	dampingNumerator * mRightSpeed
						/ dampingDenominator;

				//Seeing a person makes it happy:
				mController.changeExpression(FaceExpressionsView.FACE_HAPPY);

				//change the position of the face eyes according to the face position
				mController.setEyesPosition(faces[0].rect.centerY(), faces[0].rect.centerX()); //Y-axis of the camera SurfaceView is the X-axis of the face
			} else {
				//Nobody to play, normal face and stop
				mController.changeExpression(FaceExpressionsView.FACE_NORMAL);

				stopTracking();
			}

			mController.setSpeed(mLeftSpeed, mRightSpeed);
		}
	};

	private String mError;

	public void stopTracking() {
		mLeftSpeed = 0;
		mRightSpeed = 0;
		mCurrDesiredHeight = 0;
	}

	private boolean setCamera() {
		CameraInfo ci = new CameraInfo();
		for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo(i, ci);
			if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) {
				mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
			} 
		}
		//If couldn't find a camera show error
		if (mCamera == null){
			mError = mContext.getString(R.string.error_no_camera);
			mController.showError(mError);
			Log.e(TAG, "No camera available!");
			return false;
		}
		//Camera does not support face detection, show error
		if (mCamera.getParameters().getMaxNumDetectedFaces() < 1){
			Log.e(TAG, "No face detection!");
			mError = mContext.getString(R.string.error_no_facedetection);
			mController.showError(mError);
			mCamera.release();
			mCamera = null;
			return false;
		}
		
		return true;
	}

}
