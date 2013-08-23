#include <com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper.h>
#include "opticalFlow_LK.hpp"
#include "opticalFlow_FB.hpp"
#include "attractor.hpp"

#include <android/log.h>

#define LOG_TAG "com.wheelphone.navigator.helpers.NativeMotionTrackerWrapper_JNI"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

// MotionTrackerLK motionTracker;
MotionTrackerFB motionTracker;
ColorTrackerAttractor colorTrackerAttractor;

/*
/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeProcess
 * Signature: (JJJ)F
 */
JNIEXPORT jfloat JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeProcess
(JNIEnv * jenv, jclass, jlong input, jlong inputGray, jlong output) {
    cv::Mat& matInput  = *(cv::Mat*)input;
    cv::Mat& matInputGray  = *(cv::Mat*)inputGray;
    cv::Mat& matOutput  = *(cv::Mat*)output;

    // motionTracker.process(matInputGray, matOutput);
    // colorTrackerAttractor.process(matInput, matOutput);

    return (jfloat)0;
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeGetDesiredRotation
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeGetDesiredRotation
(JNIEnv *, jclass){
    return (jfloat)colorTrackerAttractor.getDesiredRotation();
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeIsFollowingTarget
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeIsFollowingTarget
(JNIEnv *, jclass){
    return (jfloat)colorTrackerAttractor.isFollowingTarget();
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeTargetReached
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeTargetReached
(JNIEnv *, jclass){
    return colorTrackerAttractor.isTargetReached();
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeUpdateRangeBounds
 * Signature: ([D)Z
 */
JNIEXPORT jboolean JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeUpdateRangeBounds
(JNIEnv *env , jclass, jdoubleArray jarray){

    jboolean isCopy1;

    jdouble* srcArrayElems = env -> GetDoubleArrayElements(jarray, &isCopy1);

    cv::Scalar scalarSample = cv::Scalar((int)srcArrayElems[0], (int)srcArrayElems[1], (int)srcArrayElems[2]);

    // LOGD("srcArrayElems: %f, %f, %f", srcArrayElems[0], srcArrayElems[1], srcArrayElems[2]);
    // LOGD("scalarSample: %f, %f, %f", scalarSample[0], scalarSample[1], scalarSample[2]);
    // std::cout << "lower: " << attractorRanges[0].at(mSelectedTarget) << std::endl;
    colorTrackerAttractor.updateCurrentTargetColor(scalarSample);
    // std::cout << "upper: " << attractorRanges[1].at(mSelectedTarget) << std::endl;

    if (isCopy1 == JNI_TRUE) {
       env -> ReleaseDoubleArrayElements(jarray, srcArrayElems, JNI_ABORT);
    }
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeAddTarget
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeAddTarget
(JNIEnv *, jclass){
    LOGD("Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeAddTarget enter");
}

/*
 * Class:     com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper
 * Method:    nativeRelease
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper_nativeRelease
(JNIEnv *, jclass)
{
    LOGD("Java_com_wheelphone_navigator_helpers_NativeMotionTrackerWrapper_nativeRelease enter");
    motionTracker.release();
    colorTrackerAttractor.release();
    LOGD("Java_com_wheelphone_navigator_helpers_NativeMotionTrackerWrapper_nativeRelease exit");
}