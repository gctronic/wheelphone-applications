package com.wheelphone.blobDocking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ColorBlobDetector
{
	public void setColorRadius(Scalar radius)
	{
		mColorRadius = radius;
	}
	
	public void setHsvColor(Scalar hsvColor)
	{
	    double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0; 
    	    double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

  		mLowerBound.val[0] = minH;
   		mUpperBound.val[0] = maxH;

  		mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
   		mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

  		mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
   		mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

   		mLowerBound.val[3] = 0;
   		mUpperBound.val[3] = 255;

   		Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

 		for (int j = 0; j < maxH-minH; j++)
   		{
   			byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
   			spectrumHsv.put(0, j, tmp);
   		}

   		Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);

	}
	
	public Mat getSpectrum()
	{
		return mSpectrum;
	}
	
	public void setMinContourArea(double area)
	{
		mMinContourArea = area;
	}
	
	public void process(Mat rgbaImage)
	{
    	Mat pyrDownMat = new Mat();

    	Imgproc.pyrDown(rgbaImage, pyrDownMat);
    	Imgproc.pyrDown(pyrDownMat, pyrDownMat);

      	Mat hsvMat = new Mat();
    	Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);

    	Mat Mask = new Mat();
    	Core.inRange(hsvMat, mLowerBound, mUpperBound, Mask);
    	Mat dilatedMask = new Mat();
    	Imgproc.dilate(Mask, dilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        blobCenters[0] = new Point();
        blobCenters[1] = new Point();        
        
        if(contours.size() > 0) {
        
	        // Find max contour area of the two biggest
	        double[] maxArea = new double[2];
	        maxArea[0] = 0;	// biggest
	        maxArea[1] = 0;	// 2nd biggest
	        Iterator<MatOfPoint> each = contours.iterator();
	        int[] contourIndex= new int[2];
	        contourIndex[0] = 0;
	        contourIndex[1] = 0;
	        int index=0;
	        while (each.hasNext())
	        {
	        	MatOfPoint wrapper = each.next();
	        	double area = Imgproc.contourArea(wrapper);
	        	if (area > maxArea[0]) {	// area bigger than the maximum found
	        		maxArea[1] = maxArea[0];
	        		contourIndex[1] = contourIndex[0];	        		
	        		maxArea[0] = area;
	        		contourIndex[0] = index;
	        	} else if(area > maxArea[1]) {	// area bigger than the second maximum found
	        		maxArea[1] = area;
	        		contourIndex[1] = index;
	        	}
	        	index++;
	        }
	                
	//        // another possibility to iterate through contours
	//        for (index = 0; index < contours.size(); index++) {  
	//            d = Imgproc.contourArea (contours.get(index));         
	
	        Log.d("index0", Integer.toString(contourIndex[0]));
	        Log.d("index1", Integer.toString(contourIndex[1]));
	        
	        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
	        float [] radius = new float[contours.size()];
	        
			// contours is a List<MatOfPoint>  
	        // so contours.get(x) is a single MatOfPoint  
	        // but to use approxPolyDP we need to pass a MatOfPoint2f  
	        // so we need to do a conversion   
	        
	        contours.get(contourIndex[0]).convertTo(mMOP2f1, CvType.CV_32FC2);          	       
	        // get the center of the bigger contour found
	        Point tempCenter = new Point();
	        Imgproc.minEnclosingCircle(mMOP2f1, tempCenter, radius);
	        blobCenters[0].x = tempCenter.x;
	        blobCenters[0].y = tempCenter.y;
	        blobRadius[0] = radius[0];	   
	        
	        Log.d("center0.x", Double.toString(blobCenters[0].x));
	        Log.d("center0.y", Double.toString(blobCenters[0].y));
	        Log.d("radius0", Double.toString(blobRadius[0]));
	       
	        contours.get(contourIndex[1]).convertTo(mMOP2f1, CvType.CV_32FC2);          
	        Imgproc.minEnclosingCircle(mMOP2f1, tempCenter, radius);
	        blobCenters[1].x = tempCenter.x;
	        blobCenters[1].y = tempCenter.y;
	        blobRadius[1] = radius[0];
	        
	        Log.d("center1.x", Double.toString(blobCenters[1].x));
	        Log.d("center1.y", Double.toString(blobCenters[1].y));
	        Log.d("radius1", Double.toString(blobRadius[1]));
	        
	        // Filter contours by area and resize to fit the original image size
	        mContours.clear();
	        each = contours.iterator();
	        while (each.hasNext())
	        {
	        	MatOfPoint contour = each.next();
	        	if (Imgproc.contourArea(contour) > mMinContourArea*maxArea[1])
	        	{
	        		Core.multiply(contour, new Scalar(4,4), contour);
	        		mContours.add(contour);
	        	}
	        }
	        
        } else {
        
        	blobCenters[0].x = 0;
        	blobCenters[0].y = 0;
        	blobCenters[1].x = 0;
        	blobCenters[1].y = 0;
        	
        }
        
        
	}

	public List<MatOfPoint> getContours()
	{
		return mContours;
	}
	
	public Point[] getCenter() {
		return blobCenters;
	}
	public float[] getRadius() {
		return blobRadius;
	}

	// Lower and Upper bounds for range checking in HSV color space
	private Scalar mLowerBound = new Scalar(0);
	private Scalar mUpperBound = new Scalar(0);
	// Minimum contour area in percent for contours filtering
	private static double mMinContourArea = 0.1;
	// Color radius for range checking in HSV color space
	private Scalar mColorRadius = new Scalar(25,50,50,0);
	private Mat mSpectrum = new Mat();
	private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    //List<Point> blobCenters = new ArrayList<Point>();
	Point[] blobCenters = new Point[2];
    float[] blobRadius = new float[2];
}
