#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <vector>
using namespace std;
using namespace cv;

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_example_a2lpr_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
int m_angle = 20;
bool verifySizes(RotatedRect mr)
{
    float error=0.4;
    //Spain car plate size: 52x11 aspect 4,7272
    //Taiwan car plate size: 38x16 aspect 2.3750
    float aspect=2.254;
    //Set a min and max area. All other patchs are discarded
    int min= 15*aspect*15; // minimum area
    int max= 125*aspect*125; // maximum area
    //Get only patchs that match to a respect ratio.
    float rmin= aspect-aspect*error;
    float rmax= aspect+aspect*error;

    int area= mr.size.height * mr.size.width;
    float r= (float)mr.size.width / (float)mr.size.height;
    if(r<1)
        r= (float)mr.size.height / (float)mr.size.width;

    float dr = (float) mr.size.width / (float) mr.size.height;
    float roi_angle = mr.angle;
    if (dr < 1) {
        mr.angle = 90 + roi_angle;
        swap(mr.size.width, mr.size.height);
    }


    if(( area < min || area > max ) || ( r < rmin || r > rmax )){
        return false;
    }else{

            return true;
    }

}


