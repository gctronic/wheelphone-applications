#include "opticalFlow_LK.hpp"

MotionTrackerLK::MotionTrackerLK() : maxFeatures(100),
                        qLevel(0.01),
                        minDist(10.) {}

void MotionTrackerLK::process(cv::Mat &input, cv::Mat &output, double minDisplace){
    //Shift images to be processed
    if ( frameCurrent.data ){ //have only one frame, need another frame to calculate the motion:
        framePrev.release();
        framePrev = frameCurrent;
    } else {//init code:
      // pointSize = input.cols/128;
      pointSize = 3;

      
      repulsiveRange = 3 * input.cols / 4;
      leftRepulsivePoint.x = 0;
      rightRepulsivePoint.x = input.cols;

      leftRepulsivePoint.y = input.rows;
      rightRepulsivePoint.y = input.rows;

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
        cv::findHomography(pointsPrev, pointsCurrent, CV_RANSAC, 2, statusHomography);

        displacementScaler = 0;
        
        hulls.clear();
        hull.clear();

        // calculate the displacement. (TODO: do this more efficiently! it is a matrix operation)
        for(int i= 0; i < pointsPrev.size(); i++ ) {
          //Calculate the displacement between the original feature position and the estimated origial feature position by the second OF pass:
          diffReverse.at(i) = cv::norm(pointsPrevReverse.at(i)-pointsPrev.at(i));

          if (acceptTrackedPoint(i)){
            //Calculate the displacement of the feature between frames
            // displacement[i] = cv::norm(pointsCurrent[i]-pointsPrev[i]); // Total displacement
            displacement[i] = pointsCurrent.at(i).y - pointsPrev.at(i).y; // Y-axis displacement
            // std::cout << "norm: " << displacement[i] << ". x,y 2: " << pointsCurrent[i].x << ", " << pointsCurrent[i].y << ". x,y 1: " << pointsPrev[i].x << ", " << pointsPrev[i].y << std::endl;

            if (displacement[i] < minDisplace)
              displacement[i] = -1;

            // if (displacement[i] > displacementScaler)
            //   displacementScaler = displacement[i];
          }
        }

        cv::Point2f center(0.0f, 0.0f); // Implicitly the position of the obstacle center would stay to be 0, 0 if there are no fast moving features
        objectAvgIntensity = 0; //
        double avgDisplacement = 0;//if no obstacle present, will remain being zero.

        for(int i= 0; i < pointsPrev.size(); i++ ) {
            if (acceptTrackedPoint(i)){
              if (displacement.at(i) > (1.3 * minDisplace)){
                //Everything going three times as fast as the target is moving really fast:
                colorIntensity = 255 * displacement.at(i) / (4 * minDisplace);

                // if (colorIntensity>120){
                  hull.push_back(pointsCurrent.at(i));

                  objectAvgIntensity = objectAvgIntensity + ((colorIntensity - objectAvgIntensity)/hull.size());

                  center.x = center.x + ((pointsCurrent.at(i).x - center.x)/hull.size());
                  center.y = center.y + ((pointsCurrent.at(i).y - center.y)/hull.size());
                  avgDisplacement = avgDisplacement + (displacement.at(i) - avgDisplacement)/hull.size();
                // }

                std::cout << "colorIntensity: " << colorIntensity << std::endl;
                color = cv::Scalar(colorIntensity, colorIntensity, colorIntensity, 255);
                // if (colorIntensity > 255)
                  // color = cv::Scalar(0, 255, 0, 255);


              } else {
                color = cv::Scalar(0, 255, 255, 255);
                // continue;//Don't draw features moving slower than 3 * minimum displacement
              }
            } else {
              // continue;//Don't draw invalid features
              color = cv::Scalar(0, 0, 255, 255);
            }
            cv::line(output,
                pointsPrev.at(i),// initial position
                pointsCurrent.at(i),// new position
                   color);
            cv::circle(output, pointsCurrent.at(i), pointSize, color, -1 /*thickness, so that it is filled*/);
        }

        //Make sure that the intensity (how fast the object moves), is consistent:
        objectAvgIntensity = std::min(objectAvgIntensity, 255.);
        double effectProximity = objectAvgIntensity / 255.;//Avg color is proportional to the speed of the object
        double direction = ((center.x < input.cols / 2) ? 1 : -1);//set direction (if obstacle to the left: rotate to the right)
        double effectXPosition = direction * sin(CV_PI * center.x / input.cols);//set magnitude. Range: 0->1

        //set the desired rotation depending on the visible obstacles:
        if (hull.size() > 2 && avgDisplacement > 1.3 * minDisplace){
          std::cout << "effectProximity: " << effectProximity << std::endl;
          std::cout << "effectXPosition: " << effectXPosition << std::endl;

          hulls.push_back(hull);
          convexHull(cv::Mat(hulls[0]), hulls[0]);

          rotation = effectProximity * effectXPosition;

          std::cout << "rotation: " << rotation << std::endl;

          cv::drawContours(output, hulls, 0, cv::Scalar(objectAvgIntensity, objectAvgIntensity, objectAvgIntensity, 255), 1);
          
          //draw center of convex hull
          cv::circle(output, center, 2*pointSize, cv::Scalar(255, 0, 0, 255), -1 /*thickness, so that it is filled*/);
      
          mTimestampObstacleLastSeen = (double)cv::getTickCount();
        } else {
          double timeLapse = (double)cv::getTickCount() - mTimestampObstacleLastSeen;
          if (timeLapse/((double)cvGetTickFrequency()*1000.) > 1000){//if an obstacle has not been seen in the last 1000ms
            rotation = 0;
          } //else (an obstacle was seen at least 1000ms ago) keep last rotation value
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

float MotionTrackerLK::getRotation(){
  return rotation;
}

void MotionTrackerLK::release(){
    std::cout << "MotionTrackerLK cleaning." << std::endl;
    frameCurrent.release();
    framePrev.release();
}

// determine which tracked point should be accepted. Rejected by: reverse OF match, OF status (OF and reverseOF) or RANSAC)
bool MotionTrackerLK::acceptTrackedPoint(int i) {
  // return diffReverse[i] < 0.5;
  // return statusLk[i];
  // return statusHomography[i];
  return diffReverse[i] < 1 && statusLk[i] && statusLkReverse[i] && statusHomography[i];// IGNORE NOT MOVING FEATURES OR FEATURES MOVING IN WEIRD DIRECTIONS
}