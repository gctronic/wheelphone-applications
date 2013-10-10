package com.wheelphone.helpers;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CameraViewOverlay extends View{
	private static final String TAG = CameraViewOverlay.class.getName();
	private Bitmap mBitmap;
	private Paint mPaintFill;
	private Paint mPaintBorder;

	private android.graphics.Rect mPaintableArea;
	private float mScale;

	private Scalar mBlobColorHsv;
	private ArrayList<Integer> mTargetColors = new ArrayList<Integer>();
	private int mCurrentTargetIdx;
	
	private Rect mColorSquare = new Rect();


	private static final Scalar CONTOUR_COLOR = new Scalar(0,255,0,255);


	public CameraViewOverlay(Context context) {
		super(context);
		init();
	}

	public CameraViewOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CameraViewOverlay(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init();
	}

	private void init(){
		mBlobColorHsv = new Scalar(255);
	}

	public void setImage(Mat output, List<MatOfPoint> contour){
		if (mBitmap == null){
			Log.d(TAG, "Bitmap allocated");
			mBitmap = Bitmap.createBitmap(output.width(), output.height(), Config.ARGB_8888);

			mPaintFill = new Paint(Paint.FILTER_BITMAP_FLAG |
					Paint.DITHER_FLAG |
					Paint.ANTI_ALIAS_FLAG);
			
			mPaintBorder = new Paint(Paint.FILTER_BITMAP_FLAG |
					Paint.DITHER_FLAG |
					Paint.ANTI_ALIAS_FLAG);

			//Fill the drawn squares:
			mPaintFill.setStyle(Paint.Style.FILL);
			mPaintFill.setStrokeWidth(0);

			//Draw only the border
			mPaintBorder.setStrokeWidth(5);
			mPaintBorder.setStyle(Paint.Style.STROKE);
			mPaintBorder.setColor(Color.WHITE);


			mPaintableArea = new android.graphics.Rect(0, 0, getWidth(), getHeight());

			mScale = (float)mPaintableArea.width() / (float)mBitmap.getWidth();
		}

		//Do not resize if the navigation bar is on top the overlay view
		if (mPaintableArea.height() < getHeight()){
			mPaintableArea.set(0, 0, getWidth(), getHeight());
			mScale = (float)mPaintableArea.width() / (float) mBitmap.getWidth();
		}

        Core.polylines(output, contour, true, CONTOUR_COLOR);

        try{
        	Utils.matToBitmap(output, mBitmap);
        } catch (CvException e){Log.d(TAG, e.getMessage());}
	}

	public Scalar getPixel(Mat mat, int x, int y){
		//        Log.d(TAG, "x: " + x + ". y: " + y);

		org.opencv.core.Rect touchedRect = new org.opencv.core.Rect();

		//scale x and y coordinates:
		x = (int) (x/mScale);
		y = (int) (y/mScale);

		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

		if ((x < 0) || (y < 0) || (x > mPaintableArea.width()) || (y > mPaintableArea.height())){
			Log.d(TAG, "weird touch");
			return null;
		}


		touchedRect.x = (x>4) ? x-4 : 0;
		touchedRect.y = (y>4) ? y-4 : 0;

		touchedRect.width = (x+4 > mBitmap.getWidth()) ? mBitmap.getWidth() - x : 4;
		touchedRect.height = (y+4 > mBitmap.getHeight()) ? mBitmap.getHeight() - y : 4;

		Mat touchedRegionRgba = mat.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

		// Calculate average color of touched region
		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width*touchedRect.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++) {
			mBlobColorHsv.val[i] /= pointCount;
		}

		//        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgb.val[0] + ", " + mBlobColorRgb.val[1] + 
		//    			", " + mBlobColorRgb.val[2] + ", " + mBlobColorRgb.val[3] + ")");

		return mBlobColorHsv;
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mBitmap != null){
			canvas.drawBitmap(mBitmap, null, mPaintableArea, mPaintFill);
			if (mTargetColors.size() > 0){
				//A squared sample, relative to the width of the canvas:
				mColorSquare.set(10, 100, 10+mPaintableArea.width()/10, 100+mPaintableArea.width() / 10);
				for (int i=0 ; i<mTargetColors.size() ; i++){
					mPaintFill.setColor(mTargetColors.get(i));
					canvas.drawRect(mColorSquare, mPaintFill);
					if (i == mCurrentTargetIdx)
						canvas.drawRect(mColorSquare, mPaintBorder);
					mColorSquare.offset(0, (int)mPaintBorder.getStrokeWidth() + mPaintableArea.width() / 10);
				}
			}

		}
	}

	public void updateTargetIdx(int selectedColor) {
		Log.d(TAG, "updateTargetIdx: " + selectedColor);
		mCurrentTargetIdx = selectedColor;
	}

	public void deleteCurrent() {
		mTargetColors.remove(mCurrentTargetIdx);
	}
	
	public void addColor(float hsvColor[]){
		mTargetColors.add(Color.HSVToColor(hsvColor));
	}

	public void setColor(float hsvColor[]){
		mTargetColors.set(mCurrentTargetIdx, Color.HSVToColor(hsvColor));
	}
}
