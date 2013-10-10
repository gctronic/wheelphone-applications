
#include <opencv2/opencv.hpp>

class MotionTrackerFB {
private:
    cv::Mat_<cv::Point2f> flow;
    
    cv::Mat cflow;
    cv::Mat current_frame;
    cv::Mat prev_frame;

    // relative lengths of color transitions:
    // these are chosen based on perceptual similarity
    // (e.g. one can distinguish more shades between red and yellow
    //  than between yellow and green)
    const static int RY = 15;
    const static int YG = 6;
    const static int GC = 4;
    const static int CB = 11;
    const static int BM = 13;
    const static int MR = 6;
    const static int NCOLS = RY + YG + GC + CB + BM + MR;
    cv::Vec3i colorWheel[NCOLS];

    cv::Vec3b pix;
    float maxrad;



    void drawOptFlowMap(cv::Mat& cflowmap, int step, double, const cv::Scalar& color);

    bool isFlowCorrect(cv::Point2f u);
    cv::Vec3b computeColor(float fx, float fy);
    void drawOpticalFlow(cv::Mat& dst, float maxmotion);

    
public:
    MotionTrackerFB(){
        int k = 0;

        for (int i = 0; i < RY; ++i, ++k)
            colorWheel[k] = cv::Vec3i(255, 255 * i / RY, 0);

        for (int i = 0; i < YG; ++i, ++k)
            colorWheel[k] = cv::Vec3i(255 - 255 * i / YG, 255, 0);

        for (int i = 0; i < GC; ++i, ++k)
            colorWheel[k] = cv::Vec3i(0, 255, 255 * i / GC);

        for (int i = 0; i < CB; ++i, ++k)
            colorWheel[k] = cv::Vec3i(0, 255 - 255 * i / CB, 255);

        for (int i = 0; i < BM; ++i, ++k)
            colorWheel[k] = cv::Vec3i(255 * i / BM, 0, 255);

        for (int i = 0; i < MR; ++i, ++k)
            colorWheel[k] = cv::Vec3i(255, 0, 255 - 255 * i / MR);

    }
    void process(const cv::Mat& input, cv::Mat& output);

    void release();
};
