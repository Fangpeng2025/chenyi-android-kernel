#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <android/bitmap.h>

#define LOG_TAG "RapidOCR"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// RapidOCR C++ API (需要链接 rapidocr 库)
// 这里是占位实现，实际需要集成 RapidOCR 的 C++ 库

extern "C" {

// 初始化 OCR
JNIEXPORT jboolean JNICALL
Java_com_chenyi_agent_OcrEngine_nativeInit(
    JNIEnv *env,
    jobject thiz,
    jstring modelPath
) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGD("初始化 OCR，模型路径: %s", path);
    
    // TODO: 实际初始化 RapidOCR
    // 这里需要加载 ONNX 模型文件
    
    env->ReleaseStringUTFChars(modelPath, path);
    return JNI_TRUE;
}

// OCR 识别（从 Bitmap）
JNIEXPORT jstring JNICALL
Java_com_chenyi_agent_OcrEngine_nativeOcr(
    JNIEnv *env,
    jobject thiz,
    jbyteArray imageData,
    jint width,
    jint height
) {
    LOGD("OCR 识别: %dx%d", width, height);
    
    // 获取图片数据
    jbyte *data = env->GetByteArrayElements(imageData, nullptr);
    jsize dataLen = env->GetArrayLength(imageData);
    
    // TODO: 实际调用 RapidOCR 进行识别
    // 这里返回占位符结果
    
    std::string json = R"({
        "success": true,
        "words": [
            {
                "text": "测试文本",
                "confidence": 0.95,
                "x": 100,
                "y": 200,
                "width": 80,
                "height": 30
            }
        ]
    })";
    
    env->ReleaseByteArrayElements(imageData, data, JNI_ABORT);
    return env->NewStringUTF(json.c_str());
}

// OCR 识别（从文件路径）
JNIEXPORT jstring JNICALL
Java_com_chenyi_agent_OcrEngine_nativeOcrFromPath(
    JNIEnv *env,
    jobject thiz,
    jstring imagePath
) {
    const char *path = env->GetStringUTFChars(imagePath, nullptr);
    LOGD("OCR 识别文件: %s", path);
    
    // TODO: 实际调用 RapidOCR 进行识别
    
    std::string json = R"({
        "success": true,
        "words": [
            {
                "text": "测试文本",
                "confidence": 0.95,
                "x": 100,
                "y": 200,
                "width": 80,
                "height": 30
            }
        ]
    })";
    
    env->ReleaseStringUTFChars(imagePath, path);
    return env->NewStringUTF(json.c_str());
}

} // extern "C"
