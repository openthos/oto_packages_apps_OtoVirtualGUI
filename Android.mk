LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    $(call all-proto-files-under, protos)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/protos/
LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_SDK_VERSION := current
LOCAL_PACKAGE_NAME := OtoVirtualGUI
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
