#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <android/log.h>

#define LOG_TAG "MemoryHogNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tdcolvin_memoryhog_MainActivity_allocateNativeRam(JNIEnv* env, jobject thiz, jint size, jboolean consume) {
    void* chunk = malloc(size);
    if (chunk == nullptr) {
        LOGE("malloc failed to allocate 40MB native memory");
        return JNI_FALSE;
    }

    if (consume) {
        memset(chunk, 0x5A, size);
    }

    return JNI_TRUE;
}
