LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := \
	com_wheelphone_navigator_helpers_TrackerAvoiderNativeWrapper.cpp \
	opticalFlow_LK.cpp
#	attractor.cpp \
#	opticalFlow_FB.cpp

	
LOCAL_LDLIBS     += -llog -ldl -lopencv_java

LOCAL_MODULE     := obstacles_tracker

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
	${NDKROOT}/platforms/android-14/arch-arm/usr/include/ \
	${OPENCVROOT}/sdk/native/jni/include/ \
	${NDKROOT}/sources/cxx-stl/stlport/stlport/

include $(BUILD_SHARED_LIBRARY)