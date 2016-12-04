#include <jni.h>
#include <string>

extern "C"
jstring
Java_sensornetworks_com_sensornetwork_WiFiDirectActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
