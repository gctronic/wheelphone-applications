#include "attractor.hpp"

ColorTrackerAttractor::ColorTrackerAttractor(){
    mIsFollowingTarget = false;
// 
    //Set the required colors
    green = cv::Scalar(0,255,0);

    //Hue: In degrees
    //Saturation: In percentage
    //Value: In percentage

    //Green (H: 100-360, S: 50-100%, V: 50-100%)
    attractorRanges[0].push_back(cv::Scalar( 70, 125, 125)); //Min
    attractorRanges[1].push_back(cv::Scalar(106, 255, 255)); //Max

}

float ColorTrackerAttractor::process(cv::Mat &input, cv::Mat &output){
    mTmpMat = input.clone();

    cv::cvtColor(mTmpMat, mTmpMat, CV_BGR2HSV);

    cv::inRange(mTmpMat, attractorRanges[0].at(mSelectedTarget), attractorRanges[1].at(mSelectedTarget), mTmpMat);

    //Circle:
    mMoments = cv::moments(mTmpMat, true);

    // LOGD("advance");
    // cv::add(input, mTmpMat, output);
    cv::cvtColor(mTmpMat, mTmpMat, CV_GRAY2BGR);

    // std::cout << "typeTMP: " << mTmpMat.type() << ". TypeOut: " << output.type() << std::endl;
    // LOGD("typeTMP: %i TypeOut: %i", mTmpMat.type(), output.type());
    // LOGD("sizeTMP: %ix%i SizeOut: %ix%i", mTmpMat.size().width, mTmpMat.size().height, output.size().width, output.size().height );

    // std::cout << "sizeTMP: " << mTmpMat.width() << "x" << mTmpMat.height() << "sizeOut: " << output.width() << "x" << output.height() << std::endl;
    
    // output = mTmpMat.clone();
    cv::max(input, mTmpMat, output);

    // output = input.clone();
    // cv::merge(mTmpMat, output);
    // output.add(mTmpMat);

    //If we don't know the center already, set it:
    if (mOutputCenter.x == 0){
        mOutputCenter.x = output.cols / 2;
        mMomentsCenter.x = mOutputCenter.x;
    }

    // only consider if the mass of the object is big enough, proportional to the total area of the output (5%):
    /* (output.cols * output.rows) * 0.05 == (output.cols * output.rows) / 20 */
    if (mMoments.m00 > (output.cols * output.rows) / 20){
        mIsFollowingTarget = true;
        // Get center:
        mMomentsCenter.x = mMoments.m10/mMoments.m00;
        // mMomentsCenter.y = mMoments.m01/mMoments.m00;
        mMomentsCenter.y = 0; //don't want to draw the direction line to the center of the object but at the top of the screen
    }
    //If the area of the object is really big (50% of output area), we've reached it, so turn around:
    if (mMoments.m00 > (output.cols * output.rows) / 2){
        mIsTargetReached = true;
        // nextTarget();
    }

    cv::line(output,//output Mat
             mOutputCenter,// top, center of image
             mMomentsCenter,// new position
             green,//color
             output.rows / 100);//thikness (1% of the height)


    //Set the desired rotation: a float between -1000 and 1000 that points in the direction we want the robot to go:
    // std::cout << "rotation: " << (2000*(mMomentsCenter.x - mOutputCenter.x)/output.cols) << std::endl;
    mDesiredRotation = (2000*(mMomentsCenter.x - mOutputCenter.x)/output.cols);
}

void ColorTrackerAttractor::nextTarget(){
    mSelectedTarget = (mSelectedTarget + 1) % attractorRanges[0].size();
    mMomentsCenter.x = 0;
    mIsFollowingTarget = false;
}

float ColorTrackerAttractor::getDesiredRotation(){
    return mDesiredRotation;
}

bool ColorTrackerAttractor::isTargetReached(){
    if (mIsTargetReached){
        mIsTargetReached = false;
        return true;
    }
    return false;
}

bool ColorTrackerAttractor::isFollowingTarget(){
    return mIsFollowingTarget;
}

void ColorTrackerAttractor::updateCurrentTargetColor(cv::Scalar newSample){
    int i;
    for (i=0; i<3 ; i++){
        //New low_range/high_range from the point hovered. Replace current value if the hovered point would have not been recognized
        attractorRanges[0].at(mSelectedTarget)[i] = std::max(0, std::min((int)attractorRanges[0].at(mSelectedTarget)[i], (int)newSample[i]-5));
        attractorRanges[1].at(mSelectedTarget)[i] = std::min(255, std::max((int)attractorRanges[1].at(mSelectedTarget)[i], (int)newSample[i]+5));
    }

    // LOGD("attractorRangesLow[%i]: %f, %f, %f, %f", mSelectedTarget, attractorRanges[0].at(mSelectedTarget)[0], attractorRanges[0].at(mSelectedTarget)[1], attractorRanges[0].at(mSelectedTarget)[2], attractorRanges[1].at(mSelectedTarget)[3]);
    // LOGD("attractorRangesHigh[%i]: %f, %f, %f, %f", mSelectedTarget, attractorRanges[1].at(mSelectedTarget)[0], attractorRanges[1].at(mSelectedTarget)[1], attractorRanges[1].at(mSelectedTarget)[2], attractorRanges[1].at(mSelectedTarget)[3]);
    // std::cout << "lower: " << attractorRanges[0].at(mSelectedTarget) << std::endl;
    // std::cout << "upper: " << attractorRanges[1].at(mSelectedTarget) << std::endl;
}

void ColorTrackerAttractor::setTarget(int idx){
    if (idx >=0 && idx < 2){
        mSelectedTarget = idx;
    }
}

void ColorTrackerAttractor::release(){
    // attractorRangesLow.release();
    // attractorRangesHigh.release();

    // mMoments.release();
    // mMomentsCenter.release();
}