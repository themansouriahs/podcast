LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := sonic2
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	src\main\jni\Android.mk \
	src\main\jni\Application.mk \
	src\main\jni\hello-jni.c \
	src\main\jni\simpleTest.cpp \
	src\main\jni\sonic.c \
	src\main\jni\sonicjni.c \
	src\main\jni\SoundWavesUtils.cpp \

LOCAL_C_INCLUDES += src\main\jni
LOCAL_C_INCLUDES += src\google\jni
LOCAL_C_INCLUDES += src\debug\jni
LOCAL_C_INCLUDES += src\googleDebug\jni

include $(BUILD_SHARED_LIBRARY)
