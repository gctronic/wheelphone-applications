#include "opticalFlow_LK.hpp"

MotionTrackerLK::MotionTrackerLK() : maxFeatures(500),
                        qLevel(0.01),
                        minDist(10.) {}

void MotionTrackerLK::process(cv::Mat &input, cv::Mat &output){
    //Shift images to be processed
    if ( frameCurrent.data ){ //have only one frame, need another frame to calculate the motion:
        framePrev.release();
        framePrev = frameCurrent;
    } else {//init code:
      // pointSize = input.cols/16;
      pointSize = input.cols/32;
    }
    frameCurrent = input.clone();

    // output = input.clone();

    if (pointsPrev.size() > 0){
        // Find position of feature in new image
        cv::calcOpticalFlowPyrLK(
          framePrev, frameCurrent, // 2 consecutive images
          pointsPrev, // input: interesting features points
          pointsCurrent, // output: the respective positions (in second frame) of the input points
          statusLk,    // tracking success
          err      // tracking error
        );
        cv::calcOpticalFlowPyrLK(
          frameCurrent, framePrev, // 2 consecutive images (first the current then the previous, to verify that the points we got in the previous OF were correct)
          pointsCurrent, // input: interesting features points
          pointsPrevReverse, // output: the respective positions (in second frame) of the input points
          statusLkReverse,    // tracking success
          err      // tracking error
        );


        //resize the
        diffReverse.resize(pointsPrev.size());
        displacement.resize(pointsPrev.size());
        // magnitude.resize(pointsPrev.size());

        //Use findHomography to find the most probably correct displacements (ransac), we don't really care for the homography:
        cv::findHomography(pointsPrev, pointsCurrent, CV_RANSAC, 1, statusHomography);

        displacementScaler = 0;

        // calculate the displacement. (TODO: do this more efficiently! it is a matrix operation)
        for(int i= 0; i < pointsPrev.size(); i++ ) {
          //Calculate the displacement between the original feature position and the estimated origial feature position by the second OF pass:
          diffReverse[i] = cv::norm(pointsPrevReverse[i]-pointsPrev[i]);

          if (acceptTrackedPoint(i)){
            //Calculate the displacement of the feature between frames
            // displacement[i] = cv::norm(pointsCurrent[i]-pointsPrev[i]); // Total displacement
            displacement[i] = pointsCurrent[i].y - pointsPrev[i].y; // Y-axis displacement
            // std::cout << "norm: " << displacement[i] << ". x,y 2: " << pointsCurrent[i].x << ", " << pointsCurrent[i].y << ". x,y 1: " << pointsPrev[i].x << ", " << pointsPrev[i].y << std::endl;
            if (displacement[i] > displacementScaler)
              displacementScaler = displacement[i];
          }
        }

        // cv::cvtColor(output, output, CV_GRAY2RGB);
        for(int i= 0; i < pointsPrev.size(); i++ ) {
            if (acceptTrackedPoint(i)){
              colorIntensity = 255 * displacement[i] / displacementScaler;
              // std::cout << "colorIntensity: " << colorIntensity << std::endl;
              color = cv::Scalar(colorIntensity, colorIntensity, colorIntensity);
            } else {
              color = cv::Scalar(0, 0, 255);
            }
            cv::line(output,
            		pointsPrev[i],// initial position
            		pointsCurrent[i],// new position
                   color);
            cv::circle(output, pointsCurrent[i], pointSize, color, -1 /*thickness, so that it is filled*/);
        }
    }


    // Keep track of the good features in this frame (used as baseline in next iteration)
    cv::goodFeaturesToTrack(frameCurrent, // the image 
      pointsPrev,   // the output detected features
      maxFeatures,  // the maximum number of features 
      qLevel,     // quality level
      minDist     // min distance between two features
    );

}

void MotionTrackerLK::release(){
    std::cout << "MotionTrackerLK cleaning." << std::endl;
    frameCurrent.release();
    framePrev.release();
}

// determine which tracked point should be accepted
bool MotionTrackerLK::acceptTrackedPoint(int i) {
  return statusLk[i] && statusLkReverse[i] && statusHomography[i];// IGNORE NOT MOVING FEATURES OR FEATURES MOVING IN WEIRD DIRECTIONS
}
