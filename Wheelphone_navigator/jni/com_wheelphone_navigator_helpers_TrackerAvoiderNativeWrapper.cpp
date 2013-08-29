#include <com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper.h>
#include "opticalFlow_LK.hpp"
#include "opticalFlow_FB.hpp"
#include "attractor.hpp"

#include <android/log.h>

#define LOG_TAG "com.wheelphone.navigator.helpers.NativeMotionTrackerWrapper_JNI"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

MotionTrackerLK motionTracker;
// MotionTrackerFB motionTracker;
// ColorTrackerAttractor colorTrackerAttractor;

/*
 * Class:     com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper
 * Method:    nativeProcess
 * Signature: (JJJD)V
 */
JNIEXPORT void JNICALL Java_com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper_nativeProcess
(JNIEnv * jenv, jclass, jlong input, jlong inputGray, jlong output, jdouble minDisplace) {


    cv::Mat& matInput  = *(cv::Mat*)input;
    cv::Mat& matInputGray  = *(cv::Mat*)inputGray;
    cv::Mat& matOutput  = *(cv::Mat*)output;
    
    motionTracker.process(matInputGray, matOutput, minDisplace);
    // colorTrackerAttractor.process(matInput, matOutput);

}

/*
 * Class:     com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper
 * Method:    nativeGetDesiredRotation
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper_nativeGetDesiredRotation
(JNIEnv *, jclass){
    return (jfloat)motionTracker.getRotation();
}

/*
 * Class:     com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper
 * Method:    nativeRelease
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper_nativeRelease
(JNIEnv *, jclass) {
    LOGD("Java_com_wheelphone_navigator_helpers_NativeMotionTrackerWrapper_nativeRelease enter");
    motionTracker.release();
    LOGD("Java_com_wheelphone_navigator_helpers_NativeMotionTrackerWrapper_nativeRelease exit");
}