//
//  ViewController.m
//  WPBlobTracking
//
//  Created by Stefano Morgani on 22.10.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import "ViewController.h"

@interface ViewController ()

@end

@implementation ViewController

@synthesize imageView;
@synthesize btnStart;
@synthesize videoCamera;
@synthesize pickedColor;

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    self.videoCamera = [[CvVideoCamera alloc] initWithParentView:imageView];
    self.videoCamera.defaultAVCaptureDevicePosition = AVCaptureDevicePositionBack;
    self.videoCamera.defaultAVCaptureSessionPreset = AVCaptureSessionPreset352x288;
    self.videoCamera.defaultAVCaptureVideoOrientation = AVCaptureVideoOrientationPortrait;
    self.videoCamera.defaultFPS = 30;
    self.videoCamera.grayscaleMode = NO;
    self.videoCamera.delegate = self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)actionStart:(UIButton *)sender {
    [self.videoCamera start];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    UITouch *touch = [[event allTouches] anyObject];
    CGPoint loc = [touch locationInView:imageView];
    pickedColor = [imageView colorOfPoint:loc];
}

#pragma mark - Protocol CvVideoCameraDelegate

#ifdef __cplusplus
- (void)processImage:(Mat&)image;
{
    // Do some OpenCV stuff with the image
    
    Mat pyrDownMat;
    
    cv::pyrDown(image, pyrDownMat);
    cv::pyrDown(pyrDownMat, pyrDownMat);
    
    Mat hsvMat;
    cvtColor(pyrDownMat, hsvMat, cv::COLOR_RGB2HSV_FULL);
    
    Mat Mask;
    cv::inRange(hsvMat, mLowerBound, mUpperBound, Mask);
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
#endif

@end
