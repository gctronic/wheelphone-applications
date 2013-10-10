#include "surfTracker.hpp"
#include <vector>

SurfTracker::SurfTracker(){
    targetImage = cv::imread( "../captures/target2.png" );
    // cv::cvtColor(targetImage, targetImage, CV_BGR2GRAY);
    int minHessian = 400;

    obj_corners = std::vector<cv::Point2f>(4);
    scene_corners = std::vector<cv::Point2f>(4);

    detector = cv::SurfFeatureDetector( minHessian );
    detector.detect( targetImage, keypoints_object );
    extractor.compute( targetImage, keypoints_object, descriptors_object );
}

void SurfTracker::process(const cv::Mat& input, cv::Mat& output){

    if( !targetImage.data || !input.data )
    { return; }

    //-- Step 1: Detect the keypoints using SURF Detector

    detector.detect( input, keypoints_scene );

    //-- Step 2: Calculate descriptors (feature vectors)

    extractor.compute( input, keypoints_scene, descriptors_scene );

    //-- Step 3: Matching descriptor vectors using FLANN matcher
    matcher.match( descriptors_object, descriptors_scene, matches );

    double max_dist = 0; double min_dist = 100;

    //-- Quick calculation of max and min distances between keypoints
    for( int i = 0; i < descriptors_object.rows; i++ ) {
        double dist = matches[i].distance;
        if( dist < min_dist ) min_dist = dist;
        if( dist > max_dist ) max_dist = dist;
    }

    // printf("-- Max dist : %f \n", max_dist );
    // printf("-- Min dist : %f \n", min_dist );

    //-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )

    for( int i = 0; i < descriptors_object.rows; i++ ){ 
        if( matches[i].distance < 3*min_dist ) { 
            good_matches.push_back( matches[i]); }
             }

    //-- Localize the object from img_1 in img_2

    obj.clear();
    scene.clear();
    for( size_t i = 0; i < good_matches.size(); i++ ){
        //-- Get the keypoints from the good matches
        obj.push_back( keypoints_object[ good_matches[i].queryIdx ].pt );
        scene.push_back( keypoints_scene[ good_matches[i].trainIdx ].pt );
    }

    H = findHomography( obj, scene, CV_RANSAC, 0, statusHomography );

    //-- Get the corners from the image_1 ( the object to be "detected" )
    obj_corners[0] = cv::Point(0,0); obj_corners[1] = cv::Point( targetImage.cols, 0 );
    obj_corners[2] = cv::Point( targetImage.cols, targetImage.rows ); obj_corners[3] = cv::Point( 0, targetImage.rows );

    cv::perspectiveTransform( obj_corners, scene_corners, H );

    //-- Draw lines between the corners (the mapped object in the scene - image_2 )
    cv::line( output, scene_corners[0], scene_corners[1], cv::Scalar( 0, 255, 0), 4 );
    cv::line( output, scene_corners[1], scene_corners[2], cv::Scalar( 0, 255, 0), 4 );
    cv::line( output, scene_corners[2], scene_corners[3], cv::Scalar( 0, 255, 0), 4 );
    cv::line( output, scene_corners[3], scene_corners[0], cv::Scalar( 0, 255, 0), 4 );

    
    for( size_t i = 0; i < scene.size(); i++ ){
        if (statusHomography[i]){
            color = cv::Scalar(0, 255, 0, 255);//green
        } else {
            color = cv::Scalar(0, 0, 255, 255);//red!
        }
        cv::circle(output, scene.at(i), 2, color, 1 /*thickness, so that it is filled*/);
    }
}

void SurfTracker::release(){
    // flow.release();
    // cflow.release();
    // current_frame.release();
    // prev_frame.release();
}
