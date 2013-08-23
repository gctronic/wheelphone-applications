LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include ~/Dropbox/wheelphone/sample_code/OpenCV-2.4.6-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := \
	com_wheelphone_navigator_helpers_MotionTrackerNativeWrapper.cpp \
	attractor.cpp \
	opticalFlow_FB.cpp
#	opticalFlow_LK.cpp \

	
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := obstacles_tracker

include $(BUILD_SHARED_LIBRARY)
