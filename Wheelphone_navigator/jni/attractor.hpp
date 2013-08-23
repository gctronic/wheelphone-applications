#include <opencv2/opencv.hpp>

// #include <android/log.h>
// #define LOG_TAG "com.wheelphone.navigator.helpers.NativeMotionTrackerWrapper_JNI"
// #define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
// #define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))


class ColorTrackerAttractor {
private:
    cv::Mat mTmpMat;

    std::vector<cv::Scalar> attractorRanges[2]; //0=low bound, 1=high bound

    int mSelectedTarget;

    cv::Moments mMoments;
    cv::Point2f mMomentsCenter;
    cv::Point2f mOutputCenter;

    cv::Scalar green;

    float mDesiredRotation;
    bool mIsFollowingTarget;
    bool mIsTargetReached;
    void nextTarget();

public:
    ColorTrackerAttractor();
    float process(cv::Mat &input, cv::Mat &output);
    void updateCurrentTargetColor(cv::Scalar newSample);
    void setTarget(int idx);
    float getDesiredRotation();
    bool isTargetReached();
    bool isFollowingTarget();
    void release();
};