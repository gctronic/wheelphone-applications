package com.wheelphone.pet.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import com.wheelphone.pet.R;

//TODO: Migrate to libgdx to support animations: https://code.google.com/p/libgdx/wiki/SpriteAnimation
public class FaceExpressionsView extends View {
	public static final int FACE_NORMAL = 1;
	public static final int FACE_HAPPY = 2;

//	private static String TAG = FaceExpressionsView.class.getName();

	private Drawable mPupils, mFace;
	private Point mPupilsCenterLocation;
	private Point mScreenCenter;
	private Context mContext;
	private double mNewX, mNewY;


	// Expressions (cache):
	private Drawable mNormal, mHappy;

	public FaceExpressionsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	FaceExpressionsView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init(context);
	}

	public FaceExpressionsView(Context context) { 
		super(context);
		init(context);
	}

	private void init(Context context){
		this.mContext = context;

		findScreenCenter();

		//Load pupils
		mPupils = context.getResources().getDrawable(R.drawable.pupils);
		//Load faces:
		mNormal = context.getResources().getDrawable(R.drawable.normal1);
		mHappy = context.getResources().getDrawable(R.drawable.happy);
		//Initial face is normal:
		mFace = mNormal;

		findPupilsCenter();

		//Put the face and pupils where they should be:
		centerDrawable(mFace);
		setPupilsPosition(0, 0);
	}

	private void findScreenCenter() {
		Point screenSize = new Point();
		((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
		mScreenCenter = new Point(screenSize.x/2, screenSize.y/2);
	}

	private void findPupilsCenter() {
		//Find the pupils Center, for these faces it is in the X-axis center and just above the Y-axis center
		int x = mScreenCenter.x - mPupils.getIntrinsicWidth()/2;
		int y = mScreenCenter.y - mFace.getIntrinsicHeight()/8; //the eye pupils are roughly at the center of the face (and the face center is at the center of the screen)

		//initial location is at the center
		mPupilsCenterLocation = new Point(x, y);
	}

	private void centerDrawable(Drawable d) {
		int x, y;
		x = mScreenCenter.x - d.getIntrinsicWidth()/2;
		y = mScreenCenter.y - d.getIntrinsicHeight()/2;

		Point faceLocation = new Point(x, y);
		d.setBounds(faceLocation.x, faceLocation.y, faceLocation.x + d.getIntrinsicWidth(), faceLocation.y + d.getIntrinsicHeight());
	}

	public void changeExpression(int newExpressionType){
		switch(newExpressionType){
		case FACE_NORMAL:
			mFace = mNormal;
			break;
		case FACE_HAPPY:
			mFace = mHappy;
			break;
		default:
			throw new UnsupportedOperationException();
		}
		centerDrawable(mFace);
		//TODO: invalidate only the face area
		invalidate();
	}

	/*
	 * Receives the position within a range of -1000 to 1000, movement range is 
	 */
	public void setPupilsPosition(int x, int y){
		//eye (half) width is aprox 5/41 of the head width. So the range for the eye movement is: x * (((5/41)*faceWidth)/1000)
		mNewX = mPupilsCenterLocation.x - x * (((5.0/41.0)*mFace.getIntrinsicWidth())/1000.0);
		//eye (half) height is aprox 4/19 of the head height. So the range for the eye movement is: x * (((4/19)*faceWidth)/1000)
		mNewY = mPupilsCenterLocation.y - y * (((4.0/19.0)*mFace.getIntrinsicHeight())/1000.0);
		Point pupilsLocation = new Point((int)mNewX, (int)mNewY);
		mPupils.setBounds(pupilsLocation.x, pupilsLocation.y, pupilsLocation.x + mPupils.getIntrinsicWidth(), pupilsLocation.y + mPupils.getIntrinsicHeight());

		//TODO: Invalidate the smallest area possible instead of the whole view
		invalidate();
	}

	@Override 
	public void onDraw(Canvas canvas) {
		mFace.draw(canvas);
		mPupils.draw(canvas);
	} 
}
