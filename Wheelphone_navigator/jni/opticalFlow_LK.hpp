#include <opencv2/opencv.hpp>

class MotionTrackerLK {
private:
    cv::Mat frameCurrent;
    cv::Mat framePrev;

    // tracked features from 0->1
    std::vector<cv::Point2f> pointsPrev;
    std::vector<cv::Point2f> pointsPrevReverse;
    std::vector<cv::Point2f> pointsCurrent;


    std::vector<float> displacement;
    std::vector<float> magnitude;
    std::vector<float> angle;
    float displacementScaler;
    
    int maxFeatures;
    double qLevel;
    double minDist;
    std::vector<uchar> statusLk; // status of tracked features (Optical Flow)
    std::vector<uchar> statusLkReverse; // status of tracked features (Optical Flow second)
    std::vector<uchar> statusHomography; // status of tracked features (homography)
    std::vector<double> diffReverse;    // Difference of the original feature position and the estimated original position by the second OF pass
    std::vector<float> err;    // error in tracking

    cv::Scalar color;
    int colorIntensity;
    int pointSize;

    bool acceptTrackedPoint(int i);
    
public:
    MotionTrackerLK();

    void process(cv::Mat &input, cv::Mat &output);

    void release();
};