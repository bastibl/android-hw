LOCAL_PATH := $(call my-dir)

include GrAndroid.mk

include $(CLEAR_VARS)
NDK_TOOLCHAIN_VERSION := 4.8
LOCAL_MODULE      := uhdlib
LOCAL_CPP_FEATURES += rtti
LOCAL_CPP_FEATURES += exceptions
LOCAL_C_INCLUDES  := /opt/ndk/sources/gnu-libstdc++/system/include
LOCAL_C_INCLUDES  += /opt/ndk/sources/gnu-libstdc++/stlport/stlport
LOCAL_C_INCLUDES  += /opt/grandroid/include
LOCAL_LDLIBS      := -L/opt/ndk/platforms/android-21/arch-arm/usr/lib
LOCAL_LDLIBS      += -llog
LOCAL_LDLIBS      += -landroid
LOCAL_SRC_FILES   := uhdlib.cpp
LOCAL_C_INCLUDES  += $(LIBUSB_ROOT_ABS)
LOCAL_WHOLE_STATIC_LIBRARIES += $(GR_WHOLE_STATIC_LIBRARIES)
LOCAL_STATIC_LIBRARIES += $(GR_STATIC_LIBRARIES)
LOCAL_SHARED_LIBRARIES += $(GR_SHARED_LIBRARIES)

include $(BUILD_SHARED_LIBRARY)
