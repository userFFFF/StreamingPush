#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include "rtmp_external.h"

extern "C" {

#define LOG_TAG    "rtmplive_native"

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define _JNI_VERSION JNI_VERSION_1_4
#define THREAD_NAME "lib_rtmplive"

static JavaVM *JavaVM_t;
static pthread_key_t jni_env_key;
static jobject mCallBack_Obj = NULL;

JNIEnv *RtmpLive_JNI_getEnv(const char *name);

static void RtmpLive_JNI_Detachthread(void *data) {
    JavaVM_t->DetachCurrentThread();
}

JNIEnv *RtmpLive_JNI_getEnv(const char *name) {
    JNIEnv *env = (JNIEnv *) pthread_getspecific(jni_env_key);
    if (env == NULL) {
        if (JavaVM_t->GetEnv((void **) &env, _JNI_VERSION) != JNI_OK) {
            /* attach the thread to the Java VM */
            JavaVMAttachArgs args;
            jint result;

            args.version = _JNI_VERSION;
            args.name = name;
            args.group = NULL;

            if (JavaVM_t->AttachCurrentThread(&env, &args) != JNI_OK)
                return NULL;

            /* Set the attached env to the thread-specific data area (TSD) */
            if (pthread_setspecific(jni_env_key, env) != 0) {
                JavaVM_t->DetachCurrentThread();
                return NULL;
            }
        }
    }

    return env;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    JavaVM_t = vm;

    if (vm->GetEnv((void **) &env, _JNI_VERSION) != JNI_OK)
        return -1;

    if (pthread_key_create(&jni_env_key, RtmpLive_JNI_Detachthread) != 0)
        return -1;

    return _JNI_VERSION;
}

void RtmpLive_Callback(jint value) {
    JNIEnv *env;
    if (!(env = RtmpLive_JNI_getEnv(THREAD_NAME))) {
        LOGE("JNI_GetEnv ERROR");
        return;
    }

    if (mCallBack_Obj == NULL) {
        LOGE("onCallBack NULL");
        return;
    }
    jclass objClass = env->GetObjectClass(mCallBack_Obj);
    jmethodID methodId = env->GetMethodID(objClass, "onCallbak", "(I)V");
    env->CallVoidMethod(mCallBack_Obj, methodId, value);
    env->DeleteLocalRef(objClass);
    //RtmpLive_JNI_Detachthread((void *)THREAD_NAME);
}

JNIEXPORT jstring JNICALL
Java_com_user_streamingpush_RtmpLive_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++++====";
    LOGD("StringFromJNI");
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jlong JNICALL
Java_com_user_streamingpush_RtmpLive_Init(
        JNIEnv *env, jobject /* this */, jstring url, jobject callback) {
    LOGD("RtmpLive_Init");
    if (mCallBack_Obj != NULL) {
        env->DeleteGlobalRef(mCallBack_Obj);
    }
    mCallBack_Obj = callback ? env->NewGlobalRef(callback) : NULL;
    const char *pUrl = env->GetStringUTFChars(url, false);
    LOGD("url = [%s]", pUrl);
    int ret = rtmp_success;
    rtmp_t rtmpHandle_t = rtmp_create(pUrl);
    if (rtmpHandle_t == NULL) {
        RtmpLive_Callback(rtmp_create_error);
        env->ReleaseStringUTFChars(url, pUrl);
        LOGE("Create RTMP Handle ERROR");
        return NULL;
    }

    ret = rtmp_connect((rtmp_t *) rtmpHandle_t);
    RtmpLive_Callback(ret);
    env->ReleaseStringUTFChars(url, pUrl);

    return (long) rtmpHandle_t;
}

JNIEXPORT jint JNICALL
Java_com_user_streamingpush_RtmpLive_PushStreaming(
        JNIEnv *env, jobject /* this */, jlong pushObj, jbyteArray dataArray, jint size, jlong timestamp, jint type) {
    //LOGI("RtmpLive_PushStreaming");
    if (!pushObj) {
        LOGE("RTMP Handle ERROR");
        return rtmp_handle_error;
    }

    if (size == 0) {
        LOGE("Stream size = 0");
        return rtmp_stream_size_error;
    }

    jbyte *pbuffer = (jbyte *) env->GetByteArrayElements(dataArray, 0);
    int ret;
    if (type == 0x00) {
        ret = rtmp_push_audio((rtmp_t *) pushObj, 10, 3, 1, 1, (char *) pbuffer, size, timestamp);
    } else if (type == 0x01) {
        ret = rtmp_push_video((rtmp_t *) pushObj, (char *) pbuffer, size, timestamp, timestamp);
    } else {
        RtmpLive_Callback(rtmp_stream_unsupport);
        LOGE("Stream UNSupport");
    }

    env->ReleaseByteArrayElements(dataArray, pbuffer, 0);
    return ret;
}

JNIEXPORT void JNICALL
Java_com_user_streamingpush_RtmpLive_Stop(
        JNIEnv *env, jobject /* this */, jlong pushObj) {
    LOGD("RtmpLive_Stop");
    if (!pushObj) {
        LOGE("RTMP Handle ERROR");
        return;
    }

    rtmp_stop((rtmp_t *) pushObj);
    RtmpLive_Callback(rtmp_stopped);
}
}