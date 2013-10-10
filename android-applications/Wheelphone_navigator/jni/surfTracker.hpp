#include <stdio.h>
#include <iostream>
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/calib3d/calib3d.hpp"
#include "opencv2/nonfree/features2d.hpp"
#include "opencv2/imgproc/imgproc.hpp"

class SurfTracker {
private:
    cv::Mat targetImage;
    cv::Mat H;
    cv::SurfFeatureDetector detector;
    std::vector<cv::KeyPoint> keypoints_object, keypoints_scene;
    cv::Mat descriptors_object, descriptors_scene;
    cv::SurfDescriptorExtractor extractor;
    std::vector<uchar> statusHomography; // status of tracked features (homography)
    
    std::vector<cv::Point2f> obj_corners;
    std::vector<cv::Point2f> scene_corners;

    std::vector<cv::Point2f> obj;
    std::vector<cv::Point2f> scene;

    cv::Scalar color;

    cv::FlannBasedMatcher matcher;
    std::vector< cv::DMatch > matches;
    std::vector< cv::DMatch > good_matches;


public:
    SurfTracker();

    void process(const cv::Mat& input, cv::Mat& output);

    void release();
};
