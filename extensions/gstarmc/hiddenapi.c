/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
#include <jni.h>

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_VERSION_1_6;

    jclass runtimeClass = (*env)->FindClass(env, "dalvik/system/VMRuntime");
    if (runtimeClass) {
        jmethodID getRuntime = (*env)->GetStaticMethodID(
                env, runtimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
        jmethodID setExemptions = (*env)->GetMethodID(
                env, runtimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
        if (getRuntime && setExemptions) {
            jobject runtime = (*env)->CallStaticObjectMethod(env, runtimeClass, getRuntime);
            jclass stringClass = (*env)->FindClass(env, "java/lang/String");
            jobjectArray exemptions = (*env)->NewObjectArray(
                    env, 1, stringClass, (*env)->NewStringUTF(env, "L"));
            (*env)->CallVoidMethod(env, runtime, setExemptions, exemptions);
        }
    }
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
    return JNI_VERSION_1_6;
}
