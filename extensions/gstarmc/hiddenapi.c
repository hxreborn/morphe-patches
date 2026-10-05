/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
#include <jni.h>
#include <stdbool.h>

static const char *const EXEMPT_ALL_HIDDEN_APIS = "L";

static bool clearException(JNIEnv *env) {
    if (!(*env)->ExceptionCheck(env)) {
        return false;
    }
    (*env)->ExceptionClear(env);
    return true;
}

static bool disableHiddenApiRestrictions(JNIEnv *env) {
    jclass runtimeClass = (*env)->FindClass(env, "dalvik/system/VMRuntime");
    if (runtimeClass == NULL) {
        clearException(env);
        return false;
    }

    jmethodID getRuntime = (*env)->GetStaticMethodID(
            env, runtimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
    jmethodID setHiddenApiExemptions = (*env)->GetMethodID(
            env, runtimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
    if (getRuntime == NULL || setHiddenApiExemptions == NULL) {
        clearException(env);
        (*env)->DeleteLocalRef(env, runtimeClass);
        return false;
    }

    jobject runtime = (*env)->CallStaticObjectMethod(env, runtimeClass, getRuntime);
    if (runtime == NULL || clearException(env)) {
        (*env)->DeleteLocalRef(env, runtimeClass);
        return false;
    }

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL) {
        clearException(env);
        (*env)->DeleteLocalRef(env, runtime);
        (*env)->DeleteLocalRef(env, runtimeClass);
        return false;
    }

    jstring exemption = (*env)->NewStringUTF(env, EXEMPT_ALL_HIDDEN_APIS);
    if (exemption == NULL) {
        clearException(env);
        (*env)->DeleteLocalRef(env, stringClass);
        (*env)->DeleteLocalRef(env, runtime);
        (*env)->DeleteLocalRef(env, runtimeClass);
        return false;
    }

    jobjectArray exemptions = (*env)->NewObjectArray(env, 1, stringClass, exemption);
    if (exemptions == NULL) {
        clearException(env);
        (*env)->DeleteLocalRef(env, exemption);
        (*env)->DeleteLocalRef(env, stringClass);
        (*env)->DeleteLocalRef(env, runtime);
        (*env)->DeleteLocalRef(env, runtimeClass);
        return false;
    }

    (*env)->CallVoidMethod(env, runtime, setHiddenApiExemptions, exemptions);
    bool exempted = !clearException(env);

    (*env)->DeleteLocalRef(env, exemptions);
    (*env)->DeleteLocalRef(env, exemption);
    (*env)->DeleteLocalRef(env, stringClass);
    (*env)->DeleteLocalRef(env, runtime);
    (*env)->DeleteLocalRef(env, runtimeClass);
    return exempted;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    disableHiddenApiRestrictions(env);
    return JNI_VERSION_1_6;
}
