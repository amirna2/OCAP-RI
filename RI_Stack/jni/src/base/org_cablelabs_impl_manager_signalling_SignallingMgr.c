#include <org_cablelabs_impl_manager_signalling_SignallingMgr.h>

/*
 * Class:     org_cablelabs_impl_manager_signalling_SignallingMgr
 * Method:    getPODHostAddressableProperty
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_org_cablelabs_impl_manager_signalling_SignallingMgr_getPODHostAddressableProperty
(JNIEnv *env, jobject thisObj, jstring propertyName)
{
    /* TODO:  IMPLEMENT ME! */
    jstring retVal = (*env)->NewStringUTF(env,"SecurityPropsNotImplemented");
    return retVal;
}

