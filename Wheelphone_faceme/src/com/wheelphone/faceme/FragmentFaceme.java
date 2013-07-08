package com.wheelphone.faceme;

import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentFaceme extends Fragment implements SurfaceHolder.Callback {

	private static String TAG = FragmentFaceme.class.getName();

	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mCaptureSurfaceHolder;
	private boolean previewing = false;
	private int mLeftSpeed = 0;
	private int mRightSpeed = 0;
	private int currDesiredHeight = 0;
	private int mDirection = 1;

	private TextView mOutput;

	private WheelphoneRobot mWheelphone;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_faceme,
				container, false);
		mOutput = (TextView)rootView.findViewById(R.id.output);

		mSurfaceView = (SurfaceView)rootView.findViewById(R.id.camerapreview);
		mCaptureSurfaceHolder = mSurfaceView.getHolder();
		mCaptureSurfaceHolder.addCallback(this);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity(), getActivity());
		mWheelphone.startUSBCommunication();
		mWheelphone.enableSoftAcceleration();
		mWheelphone.enableSpeedControl();
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		mWheelphone.resumeUSBCommunication();
	}

	@Override
	public void onPause() {		
		//Stop robot before disconnecting:
		mLeftSpeed = 0;
		mRightSpeed = 0;
		currDesiredHeight = 0;
		mWheelphone.setSpeed(mLeftSpeed, mRightSpeed);

		mWheelphone.pauseUSBCommunication();
		super.onPause();
	}

	public void updateOutput() {
		if (mOutput != null){
			String status = mWheelphone.isUSBConnected() ? "Connected" : "Disconnected";
			mOutput.setText(status + ". L: " + mLeftSpeed + ", R: " + mRightSpeed);
		}
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(previewing){
			mCamera.stopFaceDetection();
			mCamera.stopPreview();
			previewing = false;
		}

		if (mCamera != null){
			try {			    
				mCamera.setPreviewDisplay(mCaptureSurfaceHolder);
				mCamera.startPreview();

				mCamera.setDisplayOrientation(90);

				//TODO: Pick dynamically the best preview size.

				mCamera.startFaceDetection();
				previewing = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	    
		if (setCamera()){
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

	
	private boolean setCamera() {
		CameraInfo ci = new CameraInfo();
	    for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
	        Camera.getCameraInfo(i, ci);
	        if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) {
	        	mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
				break;
	        } 
	    }
	    if (mCamera == null){
	    	for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
	    		Camera.getCameraInfo(i, ci);
	    		if (ci.facing == CameraInfo.CAMERA_FACING_BACK) {
	    			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
	    			//device is inverted, so invert the spinning direction of the wheels:
	    			mDirection = -1;
	    			break;
	    		}
	    	}
	    }
	    //If couldn't find a camera show error
	    if (mCamera == null){
	    	mOutput.setText(getString(R.string.error_no_camera));
	    	Log.e(TAG, "No camera available!");
	    	return false;
	    }
		//Camera does not support face detection, show error
		if (mCamera.getParameters().getMaxNumDetectedFaces() < 1){
			Log.e(TAG, "No face detection available!");
			mOutput.setText(getString(R.string.error_no_facedetection));
			mCamera.release();
			mCamera = null;
			return false;
		}
	    return true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera.stopFaceDetection();
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
		previewing = false;
	}

	FaceDetectionListener faceDetectionListener = new FaceDetectionListener(){
		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {
			Log.d(TAG, "Faces detected: " + faces.length);
			if (faces.length > 0){
				if (currDesiredHeight == 0){
					currDesiredHeight = faces[0].rect.height();
				}
				
				// Face coordinates range from (-1000, 1000). Scale it to -64 to 64 (robot supports -127 to 127, but that is too fast and can easily overshoot):
				int faceYPos = 32 * faces[0].rect.centerY() / 1000;

				//Spring damping system
				int linearAcc = 64 * (faces[0].rect.height() - currDesiredHeight) / currDesiredHeight;
				int linearSpringConst = 1;
				int angularSpringConst = 1 * mDirection;
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


				//direct speed
				//				int change = 64 * (faces[0].rect.height() - currDesiredHeight)/currDesiredHeight;
				//
				//				if (change < 0) //strange hack! reverse speed reflect in a slower real speed (compared to forward)
				//					change = change*4;
				//
				//				//Calculate face rectangle area:
				//				Log.d(TAG, "change:" + change);

				// Speed ranges from -127 to 127, but only use speed ranges from -64 to 64:
				//				int l = faceYPos;
				//				int r = -faceYPos; 
				//
				//				leftSpeed = l + change;
				//				rightSpeed = r + change;

				//					Log.d(TAG, "Left=: " + faceYPos * 127 / 1000); 
				//					Log.d(TAG, "Right=: " + (-faceYPos * 127 / 1000));
			} else {
				mLeftSpeed = 0;
				mRightSpeed = 0;
				currDesiredHeight = 0;
			}

			mWheelphone.setSpeed(mLeftSpeed, mRightSpeed);
			updateOutput();
		}
	};
}
