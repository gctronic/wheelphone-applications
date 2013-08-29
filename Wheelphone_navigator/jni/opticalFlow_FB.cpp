#include "opticalFlow_FB.hpp"
#include <vector>

void MotionTrackerFB::drawOptFlowMap(cv::Mat& cflowmap, int step, double, const cv::Scalar& color) {
    for(int y = step/2; y < cflowmap.rows; y += step)
        for(int x = step/2; x < cflowmap.cols; x += step) {
            const cv::Point2f& fxy = flow.at<cv::Point2f>(y + step/2, x + step/2);
            line(cflowmap, cv::Point(x,y), cv::Point(cvRound(x+fxy.x), cvRound(y+fxy.y)),
                 color);
            circle(cflowmap, cv::Point(x,y), 2, color, -1);
        }
}

bool MotionTrackerFB::isFlowCorrect(cv::Point2f u)
{
    return !cvIsNaN(u.x) && !cvIsNaN(u.y) && fabs(u.x) < 1e9 && fabs(u.y) < 1e9;
}

cv::Vec3b MotionTrackerFB::computeColor(float fx, float fy)
{
    const float rad = sqrt(fx * fx + fy * fy);
    const float a = atan2(-fy, -fx) / (float)CV_PI;

    const float fk = (a + 1.0f) / 2.0f * (NCOLS - 1);
    const int k0 = static_cast<int>(fk);
    const int k1 = (k0 + 1) % NCOLS;
    const float f = fk - k0;


    for (int b = 0; b < 3; b++) {
        const float col0 = colorWheel[k0][b] / 255.f;
        const float col1 = colorWheel[k1][b] / 255.f;

        float col = (1 - f) * col0 + f * col1;

        if (rad <= 1)
            col = 1 - rad * (1 - col); // increase saturation with radius
        else
            col *= .75; // out of range

        pix[2 - b] = static_cast<uchar>(255.f * col);
    }

    return pix;
}

void MotionTrackerFB::drawOpticalFlow(cv::Mat& output, float maxmotion)
{
    // determine motion range:
    maxrad = maxmotion;

    if (maxmotion <= 0) {
        maxrad = 1;
        for (int y = 0; y < flow.rows; ++y)
        {
            for (int x = 0; x < flow.cols; ++x)
            {
                cv::Point2f u = flow(y, x);

                if (!isFlowCorrect(u))
                    continue;

                maxrad = std::max(maxrad, (float)sqrt(u.x * u.x + u.y * u.y));
            }
        }
    }

    for (int y = 0; y < flow.rows; ++y) {
        for (int x = 0; x < flow.cols; ++x) {
            cv::Point2f u = flow(y, x);

            if (isFlowCorrect(u))
                output.at<cv::Vec3b>(y, x) = computeColor(u.x / maxrad, u.y / maxrad);
        }
    }
}
    
void MotionTrackerFB::process(const cv::Mat& input, cv::Mat& output){
    
    //Shift images to be processed
    if ( current_frame.data ){ //have only one frame, need another frame to calculate the motion:
        prev_frame.release();
        prev_frame = current_frame;
    }


    current_frame = input.clone();

    if ( prev_frame.data ){ //already have enough frames to calculate the motion:

    //  calcOpticalFlowFarneback(prev_frame, current_frame, flow, 0.5, 3, 15, 3, 5, 1.2, 0);
        cv::calcOpticalFlowFarneback(prev_frame,//prevImg
                                     current_frame,//nextImg
                                     flow,//flow
                                     0.5,//pyramid scale. 0.5
                                     3,//pyramid levels. 3
                                     10,//winsize. 15
                                     3,//iterations. 3
                                     5,//polyN. 5
                                     1.1,//polySigma. 1.1
                                     0);//flags. 0

        drawOpticalFlow(output, -1);

        // cvtColor(output, output, CV_GRAY2RGB);
        // drawOptFlowMap(output, 16, 1.5, CV_RGB(0, 255, 0));
    }
}

void MotionTrackerFB::release(){
    flow.release();
    cflow.release();
    current_frame.release();
    prev_frame.release();
}
