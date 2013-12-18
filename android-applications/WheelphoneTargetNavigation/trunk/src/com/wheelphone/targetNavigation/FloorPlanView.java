package com.wheelphone.targetNavigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class FloorPlanView extends View {
   
    int width, length;
    int robotX, robotY, robotTheta;
    int targetX, targetY, targetTheta;	// will be transformed in array list
    Paint mPaint;
    int viewWidth, viewHeight;
    float sizeFactor;
    int TARGET_WIDTH = 12;	// cm
    int TARGET_LENGTH = 5;	// cm
    float ROBOT_RADIUS = 4.5f;	// cm
    
    public FloorPlanView(Context context, AttributeSet attrs) {
    	super(context, attrs);
           
        mPaint=new Paint(); 		// pennello    
        mPaint.setAntiAlias(true);
        
        targetX = 0;
        targetY = 0;
        targetTheta = 0;
        robotX = 0;
        robotY = 0;
        robotTheta = 0;
           
    }
      
    @Override
    protected void onDraw(Canvas canvas) {
    	// TODO Auto-generated method stub
    	super.onDraw(canvas);
    	// draw floor plan
        mPaint.setColor(Color.CYAN); 
    	canvas.drawLine(0, 0, width*sizeFactor, 0, mPaint);
    	canvas.drawLine(width*sizeFactor, 0, width*sizeFactor, length*sizeFactor, mPaint);
    	canvas.drawLine(width*sizeFactor, length*sizeFactor, 0, length*sizeFactor, mPaint);
    	canvas.drawLine(0, length*sizeFactor, 0, 0, mPaint);
    	Log.d(FloorPlanView.class.getName(), "width*sizeFactor="+width*sizeFactor+", length*sizeFactor="+length*sizeFactor);
    	
    	// draw targets
        mPaint.setColor(Color.GREEN);
        canvas.save();
        canvas.translate(targetX*sizeFactor, targetY*sizeFactor);
        canvas.rotate(-targetTheta);
        canvas.drawRect(0,(-(TARGET_WIDTH/2))*sizeFactor, TARGET_LENGTH*sizeFactor, (TARGET_WIDTH/2)*sizeFactor, mPaint);        
        canvas.drawLine(0, 0, TARGET_WIDTH*sizeFactor, 0, mPaint);        
        canvas.restore();
        
        // draw robot        
        mPaint.setColor(Color.RED); 
        canvas.save();
        canvas.translate(robotX*sizeFactor, robotY*sizeFactor);
        canvas.rotate(-robotTheta);
        canvas.drawCircle(0, 0, ROBOT_RADIUS*sizeFactor, mPaint);
        canvas.drawLine(0, 0, (ROBOT_RADIUS*2)*sizeFactor, 0, mPaint);
        canvas.restore();
    }
   
    public void updateMapSize(int w, int l) {
    	float f1=0, f2=0;
    	width = w;
    	length = l;
    	f1 = (float)viewWidth/(float)width;
    	f2 = (float)viewHeight/(float)length;
    	if(f1 < f2) {
    		sizeFactor = f1;
    	} else {
    		sizeFactor = f2;
    	}
    	Log.d(FloorPlanView.class.getName(), "width="+width+", length="+length+", factor="+sizeFactor);
    	
    	targetY = length/2;
    	
    	invalidate();
    }
    
    public void updateRobotInfo(int x, int y, int theta) {
    	robotX = x;
    	robotY = y;
    	robotTheta = theta;
    	invalidate();
    }
   
    public void updateTargetInfo(int x, int y, int theta) {
    	targetX = x;
    	targetY = y;
    	targetTheta = theta;
    	invalidate();    	
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	// save view size
        viewWidth=MeasureSpec.getSize(widthMeasureSpec);
        viewHeight=MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(viewWidth, viewHeight);
        Log.d(FloorPlanView.class.getName(), "viewWidth="+viewWidth+", viewHeight="+viewHeight);
    }
    
}
