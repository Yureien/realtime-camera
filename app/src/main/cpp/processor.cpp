#include "processor.h"

#include <string>
#include <cmath>
#include <vector>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

bool throwJavaException(JNIEnv *env, std::string method_name, std::string exception_msg,
                        int errorCode = 0) {
    char buf[8];
    sprintf(buf, "%d", errorCode);
    std::string code(buf);

    std::string msg = "@" + method_name + ": " + exception_msg + " ";
    if (errorCode != 0) msg += code;

    jclass generalExp = env->FindClass("java/lang/Exception");
    if (generalExp != 0) {
        env->ThrowNew(generalExp, msg.c_str());
        return true;
    }
    return false;
}

JNIEXPORT jbyteArray JNICALL Java_com_sohamsendev_realtime_1camera_Media_CameraPreview_NV21toRGBA(
        JNIEnv *env,
        jclass clazz,
        jbyteArray inData,
        jint width,
        jint height) {
    jbyte *inPtr = (jbyte *) env->GetPrimitiveArrayCritical(inData, 0);
    if (inPtr == NULL) {
        throwJavaException(env, "gaussianBlur", "NV21 byte stream getPointer returned NULL");
        return inData;
    }
    // Process image
    cv::UMat img = Mat(height + height / 2, width, CV_8UC1, (uchar *) inPtr).getUMat(ACCESS_FAST);
    cv::cvtColor(img, img, CV_YUV2BGR_NV21);
    std::vector<uchar> buf;
    std::vector<int> compression_params;
    compression_params.push_back(CV_IMWRITE_JPEG_QUALITY);
    compression_params.push_back(65);
    cv::imencode(".jpeg", img, buf, compression_params);
    char *data = reinterpret_cast<char *>(buf.data());

    // This is absolutely necessary before calling any other JNI function
    env->ReleasePrimitiveArrayCritical(inData, inPtr, 0);

    jbyteArray jImg = env->NewByteArray((jsize) buf.size());
    env->SetByteArrayRegion(jImg, 0, (jsize) buf.size(), (jbyte *) data);
    return jImg;
}