package com.wheelphone.pet.helpers;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.wheelphone.pet.R;

//TODO: Migrate to libgdx to support animations: https://code.google.com/p/libgdx/wiki/SpriteAnimation
public class FaceExpressions extends View {
	private static final String TAG = FaceExpressions.class.getName();

	private static final Map<Integer, String> EXPRESSIONS_RESOURCES;
	static
	{
		EXPRESSIONS_RESOURCES = new HashMap<Integer, String>();
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_ANGRY, "expression_angry_001");
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_ANNOYED, "expression_annoy_001");
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_CURIOUS, "expression_curious_001");
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_NORMAL, "expression_idle_normal"); 
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_HAPPY, "expression_laugh_001");
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_ROB, "expression_rob_002");// ??????
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_SCARED, "expression_scared_001");
		EXPRESSIONS_RESOURCES.put(Behaviour.STATE_SURPRISE, "expression_surprise_001");
	}


	private Drawable [] mFaces;
	private Drawable mPupils, mFace;
	private Rect mPupilsCenterBounds;
	private Point mScreenCenter;
	private Context mContext;
	private int mDx, mDy;
	private double mPupilMovementRange = 50;
	private volatile boolean mShowPupilsMoving;

	public FaceExpressions(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	FaceExpressions(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init(context);
	}

	public FaceExpressions(Context context) { 
		super(context);
		init(context);
	}

	private void init(Context context){
		mContext = context;

		findScreenCenter();

		//Load pupils
		mPupils = mContext.getResources().getDrawable(R.drawable.expression_laugh_001_pupils);
		//Load faces:
		mFaces = new Drawable[EXPRESSIONS_RESOURCES.size()];

		for (Integer expressionResource : EXPRESSIONS_RESOURCES.keySet()){
			int resId = mContext.getResources().getIdentifier(EXPRESSIONS_RESOURCES.get(expressionResource), "drawable", mContext.getPackageName());
			mFaces[expressionResource] = mContext.getResources().getDrawable(resId);
			centerDrawable(mFaces[expressionResource]);
		}
		//Put the face and pupils where they should be:
		centerDrawable(mPupils);
		//Keep track of the eyes center position:
		mPupilsCenterBounds = new Rect(mPupils.getBounds());
	}

	private void findScreenCenter() {
		Point screenSize = new Point();
		((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
		mScreenCenter = new Point(screenSize.x/2, screenSize.y/2);
	}

	private void centerDrawable(Drawable d) {
		int x, y;
		x = mScreenCenter.x - d.getIntrinsicWidth()/2;
		y = mScreenCenter.y - d.getIntrinsicHeight()/2;

		Point faceLocation = new Point(x, y);
		d.setBounds(faceLocation.x, faceLocation.y, faceLocation.x + d.getIntrinsicWidth(), faceLocation.y + d.getIntrinsicHeight());
	}

	public void setExpression(int expressionId){
		Log.d(TAG, "setExpression: " + Behaviour.EXPRESSIONS_NAMES.get(expressionId));
		mFace = mFaces[expressionId];
		centerDrawable(mFace);
		mShowPupilsMoving = false;
		//TODO: invalidate only the face area
		postInvalidate();
	}

	/*
	 * Receives the position within a range of -1000 to 1000, movement range is 
	 */
	public void setPupilsPosition(float x, float y){
		//Normalize input, expect a range of input of -1000 to 1000, then move according to a given range (eye size estimation)
		mDx = (int)(-(mPupilMovementRange) * (x/1000.0));
		mDy = (int)(-(mPupilMovementRange) * (y/1000.0));

		Rect newPos = new Rect(mPupilsCenterBounds);
		newPos.offset(mDx, mDy);

		mPupils.setBounds(newPos);

		//If we set the pupils movement...show them:
		mShowPupilsMoving = true;

		//TODO: Invalidate the smallest area possible instead of the whole view
		postInvalidate();
	}

	@Override 
	public void onDraw(Canvas canvas) {
		if (mFace != null)
			mFace.draw(canvas);
		if (mShowPupilsMoving)
			mPupils.draw(canvas);
	} 
}
