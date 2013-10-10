#include <opencv2/opencv.hpp>


class MotionTrackerDense {
private:
    cv::Mat mFrameCurrent;
    cv::Mat mFramePrev;

    cv::Mat_<cv::Point2f> flow;
    cv::Ptr<cv::DenseOpticalFlow> tvl1;

    bool isFlowCorrect(cv::Point2f u);
    cv::Vec3b computeColor(float fx, float fy);
    void drawOpticalFlow(cv::Mat& dst, float maxmotion);
    
public:
    MotionTrackerDense();
    void process(const cv::Mat& input, cv::Mat& output);
    void release();
};