// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

#include "org_cablelabs_impl_manager_service_SIDatabaseImpl.h"
#include "jni_util.h"
#include <mpe_si.h>
#include <mpe_os.h>
#include <mpe_ed.h>

#define SI_REQUEST_INVALID_EXCEPTION "org/cablelabs/impl/service/SIRequestInvalidException"
#define SI_NOT_AVAILABLE_EXCEPTION "org/cablelabs/impl/service/SINotAvailableException"
#define SI_NOT_AVAILABLE_YET_EXCEPTION "org/cablelabs/impl/service/SINotAvailableYetException"
#define SI_LOOKUP_FAILED_EXCEPTION  "org/cablelabs/impl/service/SILookupFailedException"

/**
 * Prototypes for the helper functions (used to remove the warnings)
 */
static mpe_Error checkReturnCode(JNIEnv* env, mpe_Error returnCode,
        int invalid, int notAvail, int notYet);
static jfieldID resolveDeliverySystemType(mpe_SiDeliverySystemType type);
static jfieldID
        resolveServiceInformationType(mpe_SiServiceInformationType type);
static jfieldID resolveServiceType(mpe_SiServiceType type);

/**
 * Helper function used to build the PAT/PMT byte arrays
 */
static jbyteArray createPMTByteArray(JNIEnv *env,
        mpe_SiServiceHandle serviceHandle);
static jbyteArray createPATByteArray(JNIEnv *env,
        mpe_SiTransportStreamHandle tsHandle);

/* Native ED callback */
static void siEventCallback(JNIEnv *env, void* listenerObject,
        mpe_EdEventInfo *edHandle, uint32_t* eventCode, void **data1,
        void** data2, uint32_t* data3);
static int pidalgorithm(mpe_SiServiceHandle siHandle, uint16_t assocTag);

/**
 * Perform any necessary JNI initialization
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeInit
(JNIEnv *env, jclass cls)
{
    /* patpmtData update method */
    GET_METHOD_ID(SIDatabaseImpl_updatePatPmtData, "updatePatPmtData", "([B)V");

    /* ServiceType */
    GET_CLASS(ServiceType, "javax/tv/service/ServiceType");
    GET_STATIC_FIELD_ID(ServiceType_DIGITAL_TV, "DIGITAL_TV", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_DIGITAL_RADIO, "DIGITAL_RADIO", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_NVOD_REFERENCE, "NVOD_REFERENCE", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_NVOD_TIME_SHIFTED, "NVOD_TIME_SHIFTED", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_ANALOG_TV, "ANALOG_TV", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_ANALOG_RADIO, "ANALOG_RADIO", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_DATA_BROADCAST, "DATA_BROADCAST", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_DATA_APPLICATION, "DATA_APPLICATION", "Ljavax/tv/service/ServiceType;");
    GET_STATIC_FIELD_ID(ServiceType_UNKNOWN, "UNKNOWN", "Ljavax/tv/service/ServiceType;");

    /* DeliverySystemType */
    GET_CLASS(DeliverySystemType,"javax/tv/service/navigation/DeliverySystemType");
    GET_STATIC_FIELD_ID(DeliverySystemType_CABLE,"CABLE","Ljavax/tv/service/navigation/DeliverySystemType;");
    GET_STATIC_FIELD_ID(DeliverySystemType_SATELLITE,"SATELLITE","Ljavax/tv/service/navigation/DeliverySystemType;");
    GET_STATIC_FIELD_ID(DeliverySystemType_TERRESTRIAL,"TERRESTRIAL","Ljavax/tv/service/navigation/DeliverySystemType;");
    GET_STATIC_FIELD_ID(DeliverySystemType_UNKNOWN,"UNKNOWN","Ljavax/tv/service/navigation/DeliverySystemType;");

    /* ServiceInformationType */
    GET_CLASS(ServiceInformationType, "javax/tv/service/ServiceInformationType");
    GET_STATIC_FIELD_ID(ServiceInformationType_ATSC_PSIP, "ATSC_PSIP", "Ljavax/tv/service/ServiceInformationType;");
    GET_STATIC_FIELD_ID(ServiceInformationType_DVB_SI, "DVB_SI", "Ljavax/tv/service/ServiceInformationType;");
    GET_STATIC_FIELD_ID(ServiceInformationType_SCTE_SI, "SCTE_SI", "Ljavax/tv/service/ServiceInformationType;");
    GET_STATIC_FIELD_ID(ServiceInformationType_UNKNOWN, "UNKNOWN", "Ljavax/tv/service/ServiceInformationType;");

    //DTO information

    /* ServiceData */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$ServiceData");
    GET_FIELD_ID(ServiceData_serviceLanguages, "serviceLanguages", "[Ljava/lang/String;");
    GET_FIELD_ID(ServiceData_serviceNames, "serviceNames", "[Ljava/lang/String;");
    GET_FIELD_ID(ServiceData_hasMultipleInstances, "hasMultipleInstances", "Z");
    GET_FIELD_ID(ServiceData_serviceType, "serviceType", "Ljavax/tv/service/ServiceType;");
    GET_FIELD_ID(ServiceData_serviceNumber, "serviceNumber", "I");
    GET_FIELD_ID(ServiceData_minorNumber, "minorNumber", "I");
    GET_FIELD_ID(ServiceData_sourceID, "sourceID", "I");
    GET_FIELD_ID(ServiceData_appID, "appID", "I");
    GET_FIELD_ID(ServiceData_frequency, "frequency", "I");
    GET_FIELD_ID(ServiceData_programNumber, "programNumber", "I");
    GET_FIELD_ID(ServiceData_modulationFormat, "modulationFormat", "I");

    /* TransportData */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$TransportData");
    GET_FIELD_ID(TransportData_deliverySystemType,"deliverySystemType","Ljavax/tv/service/navigation/DeliverySystemType;");
    GET_FIELD_ID(TransportData_transportId,"transportId","I");

    /*RatingInformationData*/
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$RatingDimensionData");
    GET_FIELD_ID(RatingDimensionData_levelDescriptionLanguages,"levelDescriptionLanguages","[[Ljava/lang/String;");
    GET_FIELD_ID(RatingDimensionData_levelDescriptions,"levelDescriptions","[[Ljava/lang/String;");
    GET_FIELD_ID(RatingDimensionData_levelNameLanguages,"levelNameLanguages","[[Ljava/lang/String;");
    GET_FIELD_ID(RatingDimensionData_levelNames,"levelNames","[[Ljava/lang/String;");
    GET_FIELD_ID(RatingDimensionData_dimensionNames,"dimensionNames","[Ljava/lang/String;");
    GET_FIELD_ID(RatingDimensionData_dimensionLanguages,"dimensionLanguages","[Ljava/lang/String;");

    /*TransportStreamData*/
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$TransportStreamData");
    GET_FIELD_ID(TransportStreamData_transportStreamId,"transportStreamId","I");
    GET_FIELD_ID(TransportStreamData_serviceInformationType,"serviceInformationType","Ljavax/tv/service/ServiceInformationType;");
    GET_FIELD_ID(TransportStreamData_lastUpdate,"lastUpdate","J");
    GET_FIELD_ID(TransportStreamData_description,"description","Ljava/lang/String;");
    GET_FIELD_ID(TransportStreamData_frequency,"frequency","I");
    GET_FIELD_ID(TransportStreamData_modulationFormat,"modulationFormat","I");
    GET_FIELD_ID(TransportStreamData_transportHandle,"transportHandle","I");
    GET_FIELD_ID(TransportStreamData_networkHandle,"networkHandle","I");

    /* ServiceComponentData */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$ServiceComponentData");
    GET_FIELD_ID(ServiceComponentData_componentPID, "componentPID", "I");
    GET_FIELD_ID(ServiceComponentData_componentTag, "componentTag", "J");
    GET_FIELD_ID(ServiceComponentData_associationTag, "associationTag", "J");
    GET_FIELD_ID(ServiceComponentData_carouselID, "carouselID", "J");
    GET_FIELD_ID(ServiceComponentData_componentNames, "componentNames", "[Ljava/lang/String;");
    GET_FIELD_ID(ServiceComponentData_componentLangs, "componentLangs", "[Ljava/lang/String;");
    GET_FIELD_ID(ServiceComponentData_associatedLanguage, "associatedLanguage", "Ljava/lang/String;");
    GET_FIELD_ID(ServiceComponentData_streamType, "streamType", "S");
    GET_FIELD_ID(ServiceComponentData_serviceInformationType,"serviceInformationType","Ljavax/tv/service/ServiceInformationType;");
    GET_FIELD_ID(ServiceComponentData_updateTime, "updateTime", "J");
    GET_FIELD_ID(ServiceComponentData_serviceDetailsHandle,"serviceDetailsHandle","I");

    /* ServiceDetailsData  */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$ServiceDetailsData");
    GET_FIELD_ID(ServiceDetailsData_sourceId,"sourceId","I");
    GET_FIELD_ID(ServiceDetailsData_appId,"appId","I");
    GET_FIELD_ID(ServiceDetailsData_programNumber,"programNumber","I");
    GET_FIELD_ID(ServiceDetailsData_longNames,"longNames","[Ljava/lang/String;");
    GET_FIELD_ID(ServiceDetailsData_languages,"languages","[Ljava/lang/String;");
    GET_FIELD_ID(ServiceDetailsData_deliverySystemType,"deliverySystemType","Ljavax/tv/service/navigation/DeliverySystemType;");
    GET_FIELD_ID(ServiceDetailsData_serviceInformationType,"serviceInformationType","Ljavax/tv/service/ServiceInformationType;");
    GET_FIELD_ID(ServiceDetailsData_updateTime,"updateTime","J");
    GET_FIELD_ID(ServiceDetailsData_caSystemIds,"caSystemIds","[I");
    GET_FIELD_ID(ServiceDetailsData_isFree,"isFree","I");
    GET_FIELD_ID(ServiceDetailsData_pcrPID,"pcrPID","I");
    GET_FIELD_ID(ServiceDetailsData_transportStreamHandle,"transportStreamHandle","I");
    GET_FIELD_ID(ServiceDetailsData_serviceHandle,"serviceHandle","I");

    /* ServiceDescriptionData */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$ServiceDescriptionData");
    GET_FIELD_ID(ServiceDescriptionData_descriptions,"descriptions","[Ljava/lang/String;");
    GET_FIELD_ID(ServiceDescriptionData_languages,"languages","[Ljava/lang/String;");
    GET_FIELD_ID(ServiceDescriptionData_updateTime,"updateTime","J");

    /* NetworkData */
    FIND_CLASS("org/cablelabs/impl/manager/service/SIDatabaseImpl$NetworkData");
    GET_FIELD_ID(NetworkData_networkId,"networkId","I");
    GET_FIELD_ID(NetworkData_name,"name","Ljava/lang/String;");
    GET_FIELD_ID(NetworkData_serviceInformationType,"serviceInformationType","Ljavax/tv/service/ServiceInformationType;");
    GET_FIELD_ID(NetworkData_updateTime,"updateTime","J");
    GET_FIELD_ID(NetworkData_transportHandle,"transportHandle","I");
}

//a helper function to resolve a delivery system type to a jfieldID
jfieldID resolveDeliverySystemType(mpe_SiDeliverySystemType type)
{
    switch (type)
    {
    case MPE_SI_DELIVERY_SYSTEM_TYPE_CABLE:
        return jniutil_CachedIds.DeliverySystemType_CABLE;
    case MPE_SI_DELIVERY_SYSTEM_TYPE_SATELLITE:
        return jniutil_CachedIds.DeliverySystemType_SATELLITE;
    case MPE_SI_DELIVERY_SYSTEM_TYPE_TERRESTRIAL:
        return jniutil_CachedIds.DeliverySystemType_TERRESTRIAL;
    case MPE_SI_DELIVERY_SYSTEM_TYPE_UNKNOWN:
        return jniutil_CachedIds.DeliverySystemType_UNKNOWN;
    default:
        return jniutil_CachedIds.DeliverySystemType_CABLE;
    }
}

//helper to resolve a service information type to a jfieldID
jfieldID resolveServiceInformationType(mpe_SiServiceInformationType type)
{
    switch (type)
    {
    case MPE_SI_SERVICE_INFORMATION_TYPE_ATSC_PSIP:
        return jniutil_CachedIds.ServiceInformationType_ATSC_PSIP;
    case MPE_SI_SERVICE_INFORMATION_TYPE_DVB_SI:
        return jniutil_CachedIds.ServiceInformationType_DVB_SI;
    case MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI:
        return jniutil_CachedIds.ServiceInformationType_SCTE_SI;
    default:
        return jniutil_CachedIds.ServiceInformationType_UNKNOWN;
    }
}

//helper to resolve a service type to a jfieldID
jfieldID resolveServiceType(mpe_SiServiceType type)
{
    switch (type)
    {
    case MPE_SI_SERVICE_DIGITAL_TV:
        return jniutil_CachedIds.ServiceType_DIGITAL_TV;
    case MPE_SI_SERVICE_DIGITAL_RADIO:
        return jniutil_CachedIds.ServiceType_DIGITAL_RADIO;
    case MPE_SI_SERVICE_NVOD_REFERENCE:
        return jniutil_CachedIds.ServiceType_NVOD_REFERENCE;
    case MPE_SI_SERVICE_NVOD_TIME_SHIFTED:
        return jniutil_CachedIds.ServiceType_NVOD_TIME_SHIFTED;
    case MPE_SI_SERVICE_ANALOG_TV:
        return jniutil_CachedIds.ServiceType_ANALOG_TV;
    case MPE_SI_SERVICE_ANALOG_RADIO:
        return jniutil_CachedIds.ServiceType_ANALOG_RADIO;
    case MPE_SI_SERVICE_DATA_BROADCAST:
        return jniutil_CachedIds.ServiceType_DATA_BROADCAST;
    case MPE_SI_SERVICE_DATA_APPLICATION:
        return jniutil_CachedIds.ServiceType_DATA_APPLICATION;
    case MPE_SI_SERVICE_TYPE_UNKNOWN:
    default:
        return jniutil_CachedIds.ServiceType_UNKNOWN;
    }
}

/**
 * Used to check return codes from the native sidb.
 * This function will place the proper responses into the JNI environment
 * and return MPE_SUCCESS or MPE_ERROR. To indicate if the return code passed in
 * was an error or not.
 * Note: you should only check against the return value MPE_SUCCESS - if the
 * function does not return that you cannot reliably derive anything save that
 * an error was thrown from the return code.
 *
 * @param env
 *  the JNI environment to place and required errors into
 * @param returnCode
 *  the return code to check against known error codes from the native SIDB
 * @param invalid
 *  set to 1 if the calling method may throw SIRequestInvalidException
 * @param notAvail
 *  set to 1 if the calling method may throw SINotAvailableException
 * @param notYet
 *  set to 1 if the calling method may throw SINotAvailableYetException
 */
mpe_Error checkReturnCode(JNIEnv* env, mpe_Error returnCode, int invalid,
        int notAvail, int notYet)
{
    // TODO(Todd): Map severe errors (e.g. out of memory) to unchecked exceptions

    if (returnCode == MPE_SUCCESS)
    {
        return returnCode;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "<<JNI-SIDB>> checkReturnCode() called with returnCode=%d\n",
                returnCode);
    }

    if (returnCode == MPE_SI_NOT_AVAILABLE_YET)
    {
        if (notYet)//can we throw not yet?
        {
            jniutil_throwByName(env, SI_NOT_AVAILABLE_YET_EXCEPTION, "");
            return returnCode;
        }
        goto error;
    }
    else if (returnCode == MPE_SI_NOT_AVAILABLE)
    {
        if (notAvail)//can we throw not avaialable?
        {
            jniutil_throwByName(env, SI_NOT_AVAILABLE_EXCEPTION, "");
            return returnCode;
        }
        goto error;
    }
    else if ((returnCode == MPE_SI_INVALID_HANDLE) || (returnCode
            == MPE_SI_NOT_FOUND))
    {
        if (invalid)//can we throw invalid?
        {
            jniutil_throwByName(env, SI_REQUEST_INVALID_EXCEPTION, "");
            return returnCode;
        }
        goto error;
    }

    error: jniutil_throwByName(env, SI_LOOKUP_FAILED_EXCEPTION, "");
    return returnCode;
}

/**
 *  Used to look up a network handle based on a transport handle and a network id.
 * @param env
 *  the JNI environment
 * @param jsidb
 *  the SIDatabaseImpl object making the JNI call
 * @param transportHandle
 *  the transportHandle to use for the lookup
 * @param networkId
 *  the networkId to use for the lookup.
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetNetworkHandleByNetworkId(
        JNIEnv *env, jobject jsidb, jint transportHandle, jint networkId)
{
    mpe_SiNetworkHandle networkHandle;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);
    MPE_UNUSED_PARAM(env);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetNetworkHandleByTransportHandleAndNetworkId(
            transportHandle, networkId, &networkHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return networkHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to look up a TransportStream handle based on transport stream id, frequency, and transport.
 * @param env
 *  The JNI environment
 * @param jsidb
 *  The SIDatabaseImpl object making the JNI call
 * @param transportHandle
 *  The transport handle of the transport to look within for the sought stream
 * @param frequency
 *  The frequency to look on for the transport stream
 * @param transportStreamId
 *  The id of the transportStream sought.
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 *
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportStreamHandleByTransportFreqAndTSID(
        JNIEnv *env, jobject jsidb, jint transportHandle, jint frequency,
        jint mode, jint transportStreamId)
{
    mpe_SiTransportStreamHandle tsHandle;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result
            = mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID(
                    transportHandle, frequency, mode, transportStreamId,
                    &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return tsHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Retrieve the transport stream handle associated with the service specified by
 * the given SourceID
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param sourceID
 *      The SourceID of the service in question
 * @return
 *      The transport stream handle
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportStreamHandleBySourceID(
        JNIEnv *env, jobject jsidb, jint sourceID)
{
    mpe_SiServiceHandle serviceHandle;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle assoicated with this sourceID
    result = mpe_siGetServiceHandleBySourceId(sourceID, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    // Get the transport stream handle assoicated with the service handle
    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return tsHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Retrieve the transport stream handle associated with the service specified by
 * the given frequency and program number
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param frequency
 *      The frequency of the service in question
 * @param programNumber
 *      The program number of the service in question
 * @return
 *      The transport stream handle
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportStreamHandleByProgramNumber(
        JNIEnv *env, jobject jsidb, jint frequency, jint mode,
        jint programNumber)
{
    mpe_SiServiceHandle serviceHandle;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle assoicated with this frequency and program number
    result = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
            frequency, mode, programNumber, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    // Get the transport stream handle assoicated with the service handle
    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return tsHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Initialize a RatingDimension based on information from the native SIDB.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param handle
 *      the handle of the rating dimension
 * @param ratingDimentionData
 *      a DTO for the Rating Dimension information. * @return
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateRatingDimension
(JNIEnv *env, jobject siDatabaseImpl, jint handle, jobject rdd)
{
    uint32_t numLevels;
    jobjectArray javaLevelDescriptions;
    jobjectArray javaLevelDescriptionLanguages;
    jobjectArray javaLevelNames;
    jobjectArray javaLevelNameLanguages;
    jclass stringArrayClass = (*env)->FindClass(env,"[Ljava/lang/String;");
    jclass stringClass = (*env)->FindClass(env,"java/lang/String");
    uint32_t numberOfNames;
    uint32_t numberOfDescriptions;
    char **dimensionNames = NULL;
    char **dimensionLanguages = NULL;
    char **levelNameLanguages = NULL;
    char **levelNames = NULL;
    char **levelDescriptionLanguages = NULL;
    char **levelDescriptions = NULL;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    // Rating Dimension Name
    if (checkReturnCode(env, mpe_siGetNumberOfNamesForRatingDimensionHandle(handle,&numberOfNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (numberOfNames > 0)
    {
        // Allocate space for our names and languages
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&dimensionNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&dimensionLanguages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

        // Get the name/language arrays for this rating dimension and create the associated
        // Java string arrays in our RatingDimensionData object
        result = mpe_siGetNamesForRatingDimensionHandle(handle, dimensionLanguages, dimensionNames);
        if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
        {
            uint32_t i;
            jobjectArray javaLangs = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);
            jobjectArray javaNames = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);

            // Create our java strings and insert them into the arrays
            for (i = 0; i < numberOfNames; ++i)
            {
                jstring lang = (*env)->NewStringUTF(env,dimensionLanguages[i]);
                jstring name = (*env)->NewStringUTF(env,dimensionNames[i]);
                (*env)->SetObjectArrayElement(env,javaLangs,i,lang);
                (*env)->SetObjectArrayElement(env,javaNames,i,name);
            }

            // Set the lang/name arrays into the ServiceData object
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_dimensionLanguages,
                    javaLangs);
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_dimensionNames,
                    javaNames);
        }
        else
        goto error;
    }

    //get the number of levels for this dimension
    if (checkReturnCode(env,mpe_siGetNumberOfLevelsForRatingDimensionHandle(handle, &numLevels),FALSE,FALSE,FALSE) != MPE_SUCCESS)
    goto error;

    // Create the outer Java arrays
    javaLevelDescriptions = (*env)->NewObjectArray(env,numLevels,stringArrayClass,NULL);
    javaLevelDescriptionLanguages = (*env)->NewObjectArray(env,numLevels,stringArrayClass,NULL);
    javaLevelNames = (*env)->NewObjectArray(env,numLevels,stringArrayClass,NULL);
    javaLevelNameLanguages = (*env)->NewObjectArray(env,numLevels,stringArrayClass,NULL);

    if (javaLevelDescriptions != NULL && javaLevelDescriptionLanguages != NULL &&
            javaLevelNames != NULL && javaLevelNameLanguages != NULL)
    {
        uint32_t level;

        for (level = 0; level < numLevels; level++)
        {
            //get the strings for level names and descriptions.
            if (checkReturnCode(env, mpe_siGetNumberOfNamesForRatingDimensionHandleAndLevel(handle,&numberOfNames,&numberOfDescriptions,(int)level), FALSE, FALSE, FALSE) != MPE_SUCCESS)
            goto error;

            // Allocate space for our names and languages
            if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&levelNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
            goto error;
            if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&levelNameLanguages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
            goto error;
            if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfDescriptions,(void**)&levelDescriptions), FALSE, FALSE, FALSE) != MPE_SUCCESS)
            goto error;
            if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfDescriptions,(void**)&levelDescriptionLanguages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
            goto error;

            // Get the name/language arrays for this rating dimension and create the associated
            // Java string arrays in our RatingDimensionData object
            result = mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel(handle, levelNameLanguages, levelNames, levelDescriptionLanguages, levelDescriptions, level);
            if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
            {
                uint32_t i;
                jobjectArray javaDescriptionLangs = (*env)->NewObjectArray(env,numberOfDescriptions,stringClass,NULL);
                jobjectArray javaDescriptions = (*env)->NewObjectArray(env,numberOfDescriptions,stringClass,NULL);
                jobjectArray javaNameLangs = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);
                jobjectArray javaNames = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);

                // Create our java strings and insert them into the arrays
                for (i = 0; i < numberOfNames; ++i)
                {
                    jstring lang = (*env)->NewStringUTF(env,levelNameLanguages[i]);
                    jstring name = (*env)->NewStringUTF(env,levelNames[i]);
                    (*env)->SetObjectArrayElement(env,javaNameLangs,i,lang);
                    (*env)->SetObjectArrayElement(env,javaNames,i,name);
                }
                for (i = 0; i < numberOfDescriptions; ++i)
                {
                    jstring lang = (*env)->NewStringUTF(env,levelDescriptionLanguages[i]);
                    jstring name = (*env)->NewStringUTF(env,levelDescriptions[i]);
                    (*env)->SetObjectArrayElement(env,javaDescriptionLangs,i,lang);
                    (*env)->SetObjectArrayElement(env,javaDescriptions,i,name);
                }

                (*env)->SetObjectArrayElement(env,javaLevelDescriptionLanguages,level,javaDescriptionLangs);
                (*env)->SetObjectArrayElement(env,javaLevelDescriptions,level,javaDescriptions);
                (*env)->SetObjectArrayElement(env,javaLevelNameLanguages,level,javaNameLangs);
                (*env)->SetObjectArrayElement(env,javaLevelNames,level,javaNames);
            }
            else
            goto error;

            // Set the lang/name arrays into the RatingDimensionData object
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_levelDescriptions,
                    javaLevelDescriptions);
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_levelDescriptionLanguages,
                    javaLevelDescriptionLanguages);
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_levelNames,
                    javaLevelNames);
            (*env)->SetObjectField(env, rdd,
                    jniutil_CachedIds.RatingDimensionData_levelNameLanguages,
                    javaLevelNameLanguages);

            // Free allocated memory
            mpe_memFreeP(MPE_MEM_SI,levelNameLanguages); levelNameLanguages = NULL;
            mpe_memFreeP(MPE_MEM_SI,levelNames); levelNames = NULL;
            mpe_memFreeP(MPE_MEM_SI,levelDescriptionLanguages); levelDescriptionLanguages = NULL;
            mpe_memFreeP(MPE_MEM_SI,levelDescriptions); levelDescriptions = NULL;
        }
    }
    else
    goto error;

    if (dimensionNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,dimensionNames);
    if (dimensionLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,dimensionLanguages);
    if (levelNameLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelNameLanguages);
    if (levelNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelNames);
    if (levelDescriptionLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelDescriptionLanguages);
    if (levelDescriptions != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelDescriptions);
    mpe_siUnLock();
    return;

    error:
    if (dimensionNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,dimensionNames);
    if (dimensionLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,dimensionLanguages);
    if (levelNameLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelNameLanguages);
    if (levelNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelNames);
    if (levelDescriptionLanguages != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelDescriptionLanguages);
    if (levelDescriptions != NULL)
    mpe_memFreeP(MPE_MEM_SI,levelDescriptions);
    mpe_siUnLock();
}

/**
 * Returns an array of handles to the rating dimensions that are available.
 * @param env
 *      The JNI Environment
 * @param jsidb
 *      The SIDatabaseImpl object making the jni call.
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetSupportedDimensions(
        JNIEnv *env, jobject jsidb)
{
    //TODO: test returned array length for failure condition.
    uint32_t length;
    jintArray ret;
    jint* cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    if (checkReturnCode(env,
            mpe_siGetNumberOfSupportedRatingDimensions(&length), FALSE, TRUE,
            TRUE) != MPE_SUCCESS)
        goto error;

    ret = (*env)->NewIntArray(env, length);
    if (ret == NULL)
    {
        goto error;
        //error allocating the java int array
    }
    else
    {
        if (length > 0)
        {
            //get access to the array
            cArr = (*env)->GetIntArrayElements(env, ret, NULL);
            if (cArr != NULL)
            {
                //and now the meat of the issue, pass the reference to mpe
                result = mpe_siGetSupportedRatingDimensions((uint32_t*) cArr);
                (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                if (checkReturnCode(env, result, FALSE, TRUE, TRUE)
                        != MPE_SUCCESS)
                    goto error;
            }
            else
                goto error;
        }
    }

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 * Returns the int handle for the rating dimension of the specified name
 * or -1 if there was no rating dimension found by the specified name.
 * @param env
 *      The JNI Environment
 * @param jsidb
 *      The SIDatabaseImpl object making the jni call.
 * @param name
 *      The name of the rating dimension we're looking for.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetRatingDimensionHandleByName(
        JNIEnv *env, jobject siDatabaseImpl, jstring name)
{
    mpe_SiRatingDimensionHandle dimHandle;
    const char *cRDName;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    if ((cRDName = (*env)->GetStringUTFChars(env, name, NULL)) == NULL)
        goto error;

    result = mpe_siGetRatingDimensionHandleByName((char *) cRDName, &dimHandle);
    (*env)->ReleaseStringUTFChars(env, name, cRDName);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return dimHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 *  Gets all transport streams for a particular network handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 *@throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportStreamsByNetwork(
        JNIEnv *env, jobject siDatabaseImpl, jint networkHandle)
{
    uint32_t length;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetNumberOfTransportStreamsForNetworkHandle(networkHandle,
            &length);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) == MPE_SUCCESS)
    {
        ret = (*env)->NewIntArray(env, length);
        if (ret == NULL)
        {
            goto error;
        }
        else
        {
            if (length > 0)
            {
                //get access to the array
                cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                if (cArr != NULL)
                {
                    //and now the meat of the issue, pass the reference to mpe
                    result = mpe_siGetAllTransportStreamsForNetworkHandle(
                            networkHandle, (uint32_t*) cArr, &length);
                    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                    if (checkReturnCode(env, result, TRUE, TRUE, TRUE)
                            != MPE_SUCCESS)
                        goto error;
                }
                else
                    goto error;
            }
        }
    }
    else
        goto error;

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 *  Gets all service details for a particular service handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceDetailsByService(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceHandle)
{
    uint32_t length;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetNumberOfServiceDetailsForServiceHandle(serviceHandle,
            &length);
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeGetServiceDetailsByService: mpe_siGetNumberOfServiceDetailsForServiceHandle()...returned %d\n",
            result);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) == MPE_SUCCESS)
    {
        ret = (*env)->NewIntArray(env, length);
        if (ret == NULL)
        {
            goto error;
        }
        else
        {
            if (length > 0)
            {
                //get access to the array
                cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                if (cArr != NULL)
                {
                    //and now the meat of the issue, pass the reference to mpe
                    result = mpe_siGetServiceDetailsForServiceHandle(
                            serviceHandle, (uint32_t*) cArr, length);
                    MPE_LOG(
                            MPE_LOG_DEBUG,
                            MPE_MOD_JNI,
                            "<<JNI-SIDB>> nativeGetServiceDetailsByService: mpe_siGetServiceDetailsForServiceHandle()...returned %d\n",
                            result);
                    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                    if (checkReturnCode(env, result, TRUE, TRUE, TRUE)
                            != MPE_SUCCESS)
                        goto error;
                }
                else
                    goto error;
            }
        }
    }
    else
        goto error;

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 *  Gets all service components for a particular service details handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceComponentsByServiceDetails(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceHandle)
{
    uint32_t length = 0;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> Lock for read success.  \n");
    result
            = mpe_siGetNumberOfComponentsForServiceHandle(serviceHandle,
                    &length);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> number of components: %x.  \n", length);

    if (result == MPE_SI_NOT_FOUND ||
        checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
    {
        goto error;
    }

    ret = (*env)->NewIntArray(env, length);
    if (ret == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "<<JNI-SIDB>> Error allocating int array.  \n");
        goto error;
    }
    else
    {
        if (length > 0)
        {
            //get access to the array
            cArr = (*env)->GetIntArrayElements(env, ret, NULL);
            if (cArr != NULL)
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_JNI,
                        "<<JNI-SIDB>> asking for the components for serviceDetails handle: %x \n",
                        (int)serviceHandle);
                //and now the meat of the issue, pass the reference to mpe
                result = mpe_siGetAllComponentsForServiceHandle(
                               serviceHandle, (uint32_t*) cArr, &length);
                (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                if (checkReturnCode(env, result, TRUE, TRUE, TRUE)
                    != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                            "<<JNI-SIDB>> Error fetching components.  \n");
                    goto error;
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                        "<<JNI-SIDB>> error getting access to the array.  \n");
                goto error;
            }
        }
    }

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 *  Gets all networks for a particular transport handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetNetworksByTransport(
        JNIEnv *env, jobject siDatabaseImpl, jint transportHandle)
{
    uint32_t length;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetNumberOfNetworksForTransportHandle(transportHandle,
            &length);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) == MPE_SUCCESS)
    {
        ret = (*env)->NewIntArray(env, length);
        if (ret == NULL)
            goto error;
        else
        {
            if (length > 0)
            {
                //get access to the array
                cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                if (cArr != NULL)
                {
                    //and now the meat of the issue, pass the reference to mpe
                    result = mpe_siGetAllNetworksForTransportHandle(
                            transportHandle, (uint32_t*) cArr, &length);
                    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                    if (checkReturnCode(env, result, TRUE, TRUE, TRUE)
                            != MPE_SUCCESS)
                        goto error;
                }
                else
                    goto error;
            }
        }
    }
    else
        goto error;

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 *  "create" a service description based on native SI by service handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceDescriptionData
 *      The ServiceDescriptionData object to populate.
 * @throws SIRequestInvalidException and SINotAvailableException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateServiceDescription
(JNIEnv *env, jobject siDatabaseImpl, jint serviceHandle, jobject serviceDescriptionData)
{
    //local variables
    mpe_TimeMillis updateTime;
    uint32_t numberOfDescriptions;
    char **descriptions = NULL;
    char **languages = NULL;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (checkReturnCode(env, mpe_siGetNumberOfServiceDescriptionsForServiceHandle(serviceHandle,&numberOfDescriptions), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (numberOfDescriptions > 0)
    {
        // Allocate space for our descriptions and languages
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfDescriptions,(void**)&descriptions), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfDescriptions,(void**)&languages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

        // Get the name/language arrays for this service and create the associated Java
        // string arrays in our ServiceData object
        result = mpe_siGetServiceDescriptionsForServiceHandle(serviceHandle, languages, descriptions);
        if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
        {
            uint32_t i;
            jclass stringClass = (*env)->FindClass(env,"java/lang/String");
            jobjectArray javaLangs = (*env)->NewObjectArray(env,numberOfDescriptions,stringClass,NULL);
            jobjectArray javaDescriptions = (*env)->NewObjectArray(env,numberOfDescriptions,stringClass,NULL);

            // Create our java strings and insert them into the arrays
            for (i = 0; i < numberOfDescriptions; ++i)
            {
                jstring lang = (*env)->NewStringUTF(env,languages[i]);
                jstring desc = (*env)->NewStringUTF(env,descriptions[i]);
                (*env)->SetObjectArrayElement(env,javaLangs,i,lang);
                (*env)->SetObjectArrayElement(env,javaDescriptions,i,desc);
            }

            // Set the lang/name arrays into the ServiceData object
            (*env)->SetObjectField(env, serviceDescriptionData,
                    jniutil_CachedIds.ServiceDescriptionData_languages,
                    javaLangs);
            (*env)->SetObjectField(env, serviceDescriptionData,
                    jniutil_CachedIds.ServiceDescriptionData_descriptions,
                    javaDescriptions);
        }
        else
        goto error;
    }

    //update time
    result = mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle(serviceHandle, &updateTime);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetLongField(env,serviceDescriptionData,
                jniutil_CachedIds.ServiceDescriptionData_updateTime,updateTime);
    }

    if (descriptions != NULL)
    mpe_memFreeP(MPE_MEM_SI,descriptions);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
    return;

    error:
    if (descriptions != NULL)
    mpe_memFreeP(MPE_MEM_SI,descriptions);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
}

/**
 * Used to look up a PAT handle based on transport stream id and frequency.
 * For now, transport stream handles are used as PAT handles
 * @param env
 *  The JNI environment
 * @param jsidb
 *  The SIDatabaseImpl object making the JNI call
 * @param frequency
 *  The frequency to look on for the transport stream
 * @param transportStreamId
 *  The id of the transportStream sought.
 * @return the PAT handle
 * @throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException, and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPATByTransportFreqAndTSID(
        JNIEnv *env, jobject jsidb, jint frequency, jint modulation, jint tsid)
{
    mpe_SiTransportStreamHandle tsHandle;
    mpe_SiTransportStreamEntry* tsEntry;
    mpe_SiModulationMode mode;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result
            = mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID(
                    MPE_SI_DEFAULT_TRANSPORT_HANDLE, frequency, modulation,
                    tsid, &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetModulationFormatForTransportStreamHandle(tsHandle, &mode);
    if ((result == MPE_SI_SUCCESS) && (mode == MPE_SI_MODULATION_QAM_NTSC))
    {
        // Analog service, no PAT can be expected.
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, FALSE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPATByTransportFreqAndTSID()... Analog service, no PAT...\n");
        goto error;
    }

    tsEntry = (mpe_SiTransportStreamEntry*) tsHandle;
    // Make sure the PAT is available
    if (tsEntry->siStatus == SI_NOT_AVAILABLE)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, TRUE);
        goto error;
    }
    if (tsEntry->siStatus == (mpe_SiStatus) SI_NOT_AVAILABLE_YET)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE_YET, TRUE, TRUE, TRUE);
        goto error;
    }
    else if (tsEntry->pat_version == 32)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, TRUE);
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "nativeCreateProgramAssociationTable()... PAT version = 0x%x -- Returned invalid value\n",
                tsEntry->pat_version);
        goto error;
    }

    mpe_siUnLock();
    return tsHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to look up a PAT handle based on service source ID
 * For now, transport stream handles are used as PAT handles
 *
 * @param env
 *      The JNI environment
 * @param sidb
 *      The SIDatabaseImpl object making the JNI call
 * @param sourceID
 *      The SourceID of the service in question
 * @return
 *      The PAT handle
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPATBySourceID(
        JNIEnv *env, jobject jsidb, jint sourceID)
{
    mpe_SiServiceHandle serviceHandle;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_SiModulationMode mode;
    mpe_SiTransportStreamEntry* tsEntry;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetServiceHandleBySourceId(sourceID, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetModulationFormatForTransportStreamHandle(tsHandle, &mode);
    if (result == MPE_SI_SUCCESS && mode == MPE_SI_MODULATION_QAM_NTSC)
    {
        // Analog service, no PAT can be expected.
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, FALSE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPATBySourceID()... Analog service, no PAT...\n");
        goto error;
    }

    tsEntry = (mpe_SiTransportStreamEntry*) tsHandle;
    // Make sure the PAT is available
    if (tsEntry->siStatus == SI_NOT_AVAILABLE)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, TRUE);
        goto error;
    }
    if (tsEntry->siStatus == (mpe_SiStatus) SI_NOT_AVAILABLE_YET)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE_YET, TRUE, TRUE, TRUE);
        goto error;
    }
    else if (tsEntry->pat_version == 32)
    {
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, TRUE);
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "nativeCreateProgramAssociationTable()... PAT version = 0x%x -- Returned invalid value\n",
                tsEntry->pat_version);
        goto error;
    }

    mpe_siUnLock();
    return tsHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to look up a PMT handle based on transport stream frequency and program number
 * For now, service handles are used as PMT handles
 * @param env
 *  The JNI environment
 * @param jsidb
 *  The SIDatabaseImpl object making the JNI call
 * @param frequency
 *  The service frequency
 * @param programNumber
 *  The service program number
 * @return the PMT handle
 * @throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException, and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPMTByProgramNumber(
        JNIEnv *env, jobject jsidb, jint frequency, jint modulation,
        jint programNumber)
{
    uint32_t pmtVersion;
    mpe_SiServiceHandle serviceHandle;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_SiModulationMode mode;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
            frequency, modulation, programNumber, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetModulationFormatForTransportStreamHandle(tsHandle, &mode);
    if (result == MPE_SI_SUCCESS && mode == MPE_SI_MODULATION_QAM_NTSC)
    {
        // Analog service, no PAT can be expected.
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, FALSE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTByFrequencyProgramNumber()... Analog service, no PMT...\n");
        goto error;
    }

    // Call this function to ensure that the PMT is actually available
    result = mpe_siGetPMTVersionForServiceHandle(serviceHandle, &pmtVersion);
    if (result != MPE_SI_SUCCESS)
    {
        (void) checkReturnCode(env, result, TRUE, TRUE, TRUE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTByFrequencyProgramNumber()... PMT may not be available yet!\n");
        goto error;
    }

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to look up a PMT handle based on source ID
 * For now, service handles are used as PMT handles
 * @param env
 *  The JNI environment
 * @param jsidb
 *  The SIDatabaseImpl object making the JNI call
 * @param sourceID
 *  The service sourceID
 * @return the PMT handle
 * @throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException, and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPMTBySourceID(
        JNIEnv *env, jobject jsidb, jint sourceID)
{
    uint32_t pmtVersion;
    mpe_SiServiceHandle serviceHandle;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_SiModulationMode mode;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetServiceHandleBySourceId(sourceID, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetModulationFormatForTransportStreamHandle(tsHandle, &mode);
    if (result == MPE_SI_SUCCESS && mode == MPE_SI_MODULATION_QAM_NTSC)
    {
        // Analog service, no PAT can be expected.
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, FALSE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTBySourceID()... Analog service, no PMT...\n");
        goto error;
    }

    // Call this function to ensure that the PMT is actually available
    result = mpe_siGetPMTVersionForServiceHandle(serviceHandle, &pmtVersion);
    if (result != MPE_SI_SUCCESS)
    {
        (void) checkReturnCode(env, result, TRUE, TRUE, TRUE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTBySourceID()... PMT may not be available yet!\n");
        goto error;
    }

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to look up a PMT handle based on source ID
 * For now, service handles are used as PMT handles
 * @param env
 *  The JNI environment
 * @param jsidb
 *  The SIDatabaseImpl object making the JNI call
 * @param serviceHandle
 *  The native service handle
 * @return the PMT handle
 * @throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException, and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPMTByService(
        JNIEnv *env, jobject jsidb, jint serviceHandle)
{
    uint32_t pmtVersion;
    mpe_SiTransportStreamHandle tsHandle;
    mpe_SiModulationMode mode;
    mpe_Error result;

    MPE_UNUSED_PARAM(jsidb);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
            &tsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    result = mpe_siGetModulationFormatForTransportStreamHandle(tsHandle, &mode);
    if (result == MPE_SI_SUCCESS && mode == MPE_SI_MODULATION_QAM_NTSC)
    {
        // Analog service, no PAT can be expected.
        (void) checkReturnCode(env, MPE_SI_NOT_AVAILABLE, TRUE, TRUE, FALSE);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTByService()... Analog service, no PMT...\n");
        goto error;
    }

    // Call this function to ensure that the PMT is actually available
    result = mpe_siGetPMTVersionForServiceHandle(serviceHandle, &pmtVersion);
    if (result != MPE_SI_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "nativeGetPMTByService()... PMT may not be available yet!\n");
        (void) checkReturnCode(env, result, TRUE, TRUE, TRUE);
        goto error;
    }

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Used to retrieve the data necessary to create a ProgramAssociationTable, based on
 * a transport stream handle
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param patHandle
 *      The handle associated with the PAT
 * @return a Java byte array containing the data needed to build a ProgramAssociationTable
 * @throws SIRequestInvalidException and SINotAvailableException
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateProgramAssociationTable(
        JNIEnv *env, jobject jsidb, jint patHandle)
{
    MPE_UNUSED_PARAM(jsidb);
    return createPATByteArray(env, patHandle);
}

/**
 * Used to retrieve the data necessary to create a ProgramMapTable, based on a
 * service handle
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param pmtHandle
 *      The handle associated with the PMT
 * @return a Java byte array containing the data needed to build a ProgramMapTable
 * @throws SIRequestInvalidException and SINotAvailableException
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateProgramMapTable(
        JNIEnv *env, jobject jsidb, jint pmtHandle)
{
    MPE_UNUSED_PARAM(jsidb);
    return createPMTByteArray(env, pmtHandle);
}

/**
 * Used to "create" a Network object from the native sidb, based on network handle.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param networkData
 *      The NetworkData object to populate.
 *
 * @throws SIRequestInvalidException and SINotAvailableException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateNetwork
(JNIEnv *env, jobject siDatabaseImpl, jint networkHandle, jobject networkData)
{
    uint32_t networkId;
    mpe_SiServiceInformationType serviceInformationType;
    jobject serviceInformationTypeObject;
    jfieldID serviceInformationTypeField;
    char * name = NULL;
    jobject nameObject;
    mpe_TimeMillis updateTime;
    mpe_Error result;
    mpe_SiTransportHandle transportHandle;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    //network name
    result = mpe_siGetNetworkNameForNetworkHandle(networkHandle, &name);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        if (name != NULL)
        {
            nameObject = (*env)->NewStringUTF(env, name);
            (*env)->SetObjectField(env, networkData,
                    jniutil_CachedIds.NetworkData_name, nameObject);
        }
    }
    else
    goto error;

    //networkid
    result = mpe_siGetNetworkIdForNetworkHandle(networkHandle,&networkId);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,networkData,
                jniutil_CachedIds.NetworkData_networkId,networkId);
    }
    else
    goto error;

    //serviceInformationType
    result = mpe_siGetNetworkServiceInformationType(networkHandle,&serviceInformationType);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        serviceInformationTypeField = resolveServiceInformationType(serviceInformationType);
        serviceInformationTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.ServiceInformationType,serviceInformationTypeField);
        (*env)->SetObjectField(env,networkData,
                jniutil_CachedIds.NetworkData_serviceInformationType,serviceInformationTypeObject);
    }
    else
    goto error;

    //last update time
    result = mpe_siGetNetworkLastUpdateTimeForNetworkHandle(networkHandle,&updateTime);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetLongField(env,networkData,
                jniutil_CachedIds.NetworkData_updateTime,updateTime);
    }
    else
    goto error;

    //transport handle
    result = mpe_siGetTransportHandleForNetworkHandle(networkHandle,&transportHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,networkData,
                jniutil_CachedIds.NetworkData_transportHandle,transportHandle);
    }
    else
    goto error;

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
}

/**
 * "create" a service details based on native SI by a service Handle
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceDetailsData
 *      The java object to populate for use in the Java SIDatabaseImpl
 *
 * @throws SIRequestInvalidException and SINotAvailableException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateServiceDetails
(JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle, jobject serviceDetailsData)
{
    //local variables
    uint32_t sourceId, appId, programNumber;
    mpe_SiDeliverySystemType deliverySystemType;
    mpe_SiServiceInformationType serviceInformationType;
    jobject deliverySystemTypeObject, serviceInformationTypeObject;
    uint32_t numberOfNames = 0;
    char **longNames = NULL;
    char **languages = NULL;
    jfieldID deliverySystemTypeField, serviceInformationTypeField;
    mpe_TimeMillis updateTime;
    int caSystemArrayLength = 0;
    jintArray caSystemIntArray;
    jint *caIds;
    mpe_Bool isFree;
    uint32_t pcrPID;
    mpe_Error result;
    mpe_SiTransportStreamHandle transportStreamHandle;
    mpe_SiServiceHandle serviceHandle;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    //lock the db
    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)!= MPE_SUCCESS)
    goto error;

    //sourceId
    result = mpe_siGetSourceIdForServiceDetailsHandle(serviceDetailsHandle,&sourceId);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> sourceID: %x\n",sourceId);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_sourceId, sourceId);
    }
    else
    goto error;

    //appId (Fix: Change the call below to take 'ServiceDetailsHandle'
    result = mpe_siGetAppIdForServiceHandle(serviceDetailsHandle,&appId);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> appID: %x\n",appId);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_appId, appId);
    }
    else
    goto error;

    //programnumber
    result = mpe_siGetProgramNumberForServiceDetailsHandle(serviceDetailsHandle,&programNumber);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> programNumber: %x\n",programNumber);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_programNumber,programNumber);
    }
    else
    goto error;

    //long names
    if (checkReturnCode(env, mpe_siGetNumberOfLongNamesForServiceDetailsHandle(serviceDetailsHandle,&numberOfNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (numberOfNames > 0)
    {
        // Allocate space for our names and languages
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&longNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&languages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

        // Get the name/language arrays for this service details and create the associated Java
        // string arrays in our ServiceData object
        result = mpe_siGetLongNamesForServiceDetailsHandle(serviceDetailsHandle, languages, longNames);
        if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
        {
            uint32_t i;
            jclass stringClass = (*env)->FindClass(env,"java/lang/String");
            jobjectArray javaLangs = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);
            jobjectArray javaNames = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);

            // Create our java strings and insert them into the arrays
            for (i = 0; i < numberOfNames; ++i)
            {
                jstring lang = (*env)->NewStringUTF(env,languages[i]);
                jstring name = (*env)->NewStringUTF(env,longNames[i]);
                (*env)->SetObjectArrayElement(env,javaLangs,i,lang);
                (*env)->SetObjectArrayElement(env,javaNames,i,name);
            }

            // Set the lang/name arrays into the ServiceData object
            (*env)->SetObjectField(env, serviceDetailsData,
                    jniutil_CachedIds.ServiceDetailsData_languages,
                    javaLangs);
            (*env)->SetObjectField(env, serviceDetailsData,
                    jniutil_CachedIds.ServiceDetailsData_longNames,
                    javaNames);
        }
        else
        goto error;
    }

    //deliverySystemType
    result = mpe_siGetDeliverySystemTypeForServiceDetailsHandle(serviceDetailsHandle,&deliverySystemType);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> deliverySystemType: %x\n", deliverySystemType);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        deliverySystemTypeField = resolveDeliverySystemType(deliverySystemType);

        deliverySystemTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.DeliverySystemType,deliverySystemTypeField);

        (*env)->SetObjectField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_deliverySystemType,deliverySystemTypeObject);
    }
    else
    goto error;

    //serviceInformationType
    result = mpe_siGetServiceInformationTypeForServiceDetailsHandle(serviceDetailsHandle,&serviceInformationType);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> service information type: %x\n",serviceInformationType);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        serviceInformationTypeField = resolveServiceInformationType(serviceInformationType);
        serviceInformationTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.ServiceInformationType,serviceInformationTypeField);

        (*env)->SetObjectField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_serviceInformationType,serviceInformationTypeObject);
    }
    else
    goto error;

    //last update time
    result = mpe_siGetServiceDetailsLastUpdateTimeForServiceDetailsHandle(serviceDetailsHandle,&updateTime);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetLongField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_updateTime,updateTime);
    }
    else
    goto error;

    //caSystemIds
    result = mpe_siGetCASystemIdArrayLengthForServiceDetailsHandle(serviceDetailsHandle, (uint32_t*)&caSystemArrayLength);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> ca array length: %x\n",caSystemArrayLength);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        if (caSystemArrayLength != 0)
        {
            //allocate the ca system id array on the java heap.
            caSystemIntArray = (*env)->NewIntArray(env,caSystemArrayLength);
            caIds = (*env)->GetIntArrayElements(env,caSystemIntArray,NULL);
            result = mpe_siGetCASystemIdArrayForServiceDetailsHandle(serviceDetailsHandle,(uint32_t*)caIds,caSystemArrayLength);
            if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
            {
                //set the caSystemIds
                (*env)->SetObjectField(env,serviceDetailsData,
                        jniutil_CachedIds.ServiceDetailsData_caSystemIds,caSystemIntArray);
                (*env)->ReleaseIntArrayElements(env,caSystemIntArray,caIds,0);
            }
            else
            {
                (*env)->ReleaseIntArrayElements(env,caSystemIntArray,caIds,0);
                goto error;
            }
        }
    }
    else
    goto error;

    result = mpe_siGetIsFreeFlagForServiceDetailsHandle(serviceDetailsHandle,&isFree);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> isFree flag: %x\n",isFree);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_isFree,isFree);
    }
    else
    goto error;

    //PCR PID
    result = mpe_siGetPcrPidForServiceHandle(serviceDetailsHandle,&pcrPID);
    if (result != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> PCR PID not available (returning -1)");
        pcrPID = (uint32_t)-1;
    }
    (*env)->SetIntField(env,serviceDetailsData,
            jniutil_CachedIds.ServiceDetailsData_pcrPID, pcrPID);

    //transportStreamHandle
    result = mpe_siGetTransportStreamHandleForServiceDetailsHandle(serviceDetailsHandle, &transportStreamHandle);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> transportStreamHandle: %x\n",transportStreamHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_transportStreamHandle, transportStreamHandle);
    }
    else
    goto error;

    //serviceHandle
    result = mpe_siGetServiceHandleForServiceDetailsHandle(serviceDetailsHandle, &serviceHandle);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> service handle: %x\n",serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceDetailsData,
                jniutil_CachedIds.ServiceDetailsData_serviceHandle, serviceHandle);
    }
    else
    goto error;

    if (longNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,longNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
    return;

    error:
    if (longNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,longNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
}

/**
 * @param env
 *       The JNI environment
 * @param siDatabaseImpl
 *       The SIDatabaseImpl object making the JNI call
 * @return
 *       An int[] for all transports in the system.
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetAllTransports(
        JNIEnv *env, jobject siDatabaseImpl)
{
    uint32_t length;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTotalNumberOfTransports(&length);
    if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
    {
        ret = (*env)->NewIntArray(env, length);
        if (ret == NULL)
            goto error;
        else
        {
            if (length > 0)
            {
                //get access to the array
                cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                if (cArr != NULL)
                {
                    //and now the meat of the issue, pass the reference to mpe
                    result = mpe_siGetAllTransports((uint32_t*) cArr, &length);
                    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                    if (checkReturnCode(env, result, FALSE, FALSE, FALSE)
                            != MPE_SUCCESS)
                        goto error;
                }
                else
                    goto error;
            }
        }
    }
    else
        goto error;

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 *  Gets an array of all service handles available.
 *  @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @return
 *      An int[] for all service handles in the system.
 *
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetAllServices(
        JNIEnv *env, jobject siDatabaseImpl)
{
    uint32_t length = 0;
    jintArray ret = NULL;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            == MPE_SUCCESS)
    {
        if (checkReturnCode(env, (result = mpe_siGetTotalNumberOfServices(
                &length)), FALSE, TRUE, TRUE) == MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "<<JNI-SIDB>> Number Of Services: 0x%x\n", length);
            if ((ret = (*env)->NewIntArray(env, length)) != NULL)
            {
                if (length > 0)
                {
                    //get access to the array
                    cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                    if (cArr != NULL)
                    {
                        //and now the meat of the issue, pass the reference to mpe
                        result
                                = mpe_siGetAllServices((uint32_t*) cArr,
                                        &length);
                        (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                        if (checkReturnCode(env, result, FALSE, TRUE, TRUE)
                                != MPE_SUCCESS)
                        {
                            MPE_LOG(
                                    MPE_LOG_WARN,
                                    MPE_MOD_JNI,
                                    "<<JNI-SIDB>> nativeGetAllServices - mpe_siGetAllServices() not successful = 0x%x\n",
                                    result);
                        }
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                                "<<JNI-SIDB>> nativeGetAllServices - couldn't GetIntArrayElements\n");
                    }
                }
                else
                {
                    MPE_LOG(
                            MPE_LOG_ERROR,
                            MPE_MOD_JNI,
                            "<<JNI-SIDB>> nativeGetAllServices - length = 0\n");
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "<<JNI-SIDB>> nativeGetAllServices - couldn't NewIntArray\n");
            }
        }
        else
        {
            MPE_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_JNI,
                    "<<JNI-SIDB>> nativeGetAllServices - mpe_siGetTotalNumberOfServices() not successful = 0x%x\n",
                    result);
        }

        mpe_siUnLock();
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "<<JNI-SIDB>> nativeGetAllServices - error geting siLockForRead \n");
    }

    return ret;
}

/**
 * Get an array of transport stream handles for a given transport.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param transportId
 *      The transport ID of the transport to query
 * @return
 *      An array of transport handles for the supplied tranpsort.
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportStreamsByTransport(
        JNIEnv *env, jobject siDatabaseImpl, jint transportHandle)
{
    uint32_t length;
    jintArray ret;
    jint *cArr;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetNumberOfTransportStreamsForTransportHandle(
            transportHandle, &length);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) == MPE_SUCCESS)
    {
        ret = (*env)->NewIntArray(env, length);
        if (ret == NULL)
            goto error;
        else
        {
            if (length > 0)
            {
                //get access to the array
                cArr = (*env)->GetIntArrayElements(env, ret, NULL);
                if (cArr != NULL)
                {
                    //and now the meat of the issue, pass the reference to mpe
                    result = mpe_siGetAllTransportStreamsForTransportHandle(
                            transportHandle, (uint32_t*) cArr, &length);
                    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
                    if (checkReturnCode(env, result, TRUE, TRUE, TRUE)
                            != MPE_SUCCESS)
                        goto error;
                }
                else
                    goto error;
            }
        }
    }
    else
        goto error;

    mpe_siUnLock();
    return ret;

    error: mpe_siUnLock();
    return NULL;
}

/**
 * Get a service handle based on a service major/minor number.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param majorNumber
 *      The major number of the service
 * @param minorNumber
 *      The minor number of the service.  If -1, it will be disregarded.
 * @return
 *      The service handle of the service with the specified source ID
 * @throws SIRequestInvalidException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceByServiceNumber(
        JNIEnv *env, jobject siDatabaseImpl, jint majorNumber, jint minorNumber)
{
    mpe_SiServiceHandle serviceHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle from the native SI database
    result = mpe_siGetServiceHandleByServiceNumber(majorNumber, minorNumber,
            &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get a service handle based on a service major/minor number.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param appId
 *      The app Id of the service (DSG only)
 * @return
 *      The service handle of the service with the specified app ID
 * @throws SIRequestInvalidException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceByAppId(
        JNIEnv *env, jobject siDatabaseImpl, jint appId)
{
    mpe_SiServiceHandle serviceHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle from the native SI database
    result = mpe_siGetServiceHandleByAppId(appId, &serviceHandle);

    // may be a dynamic service
    if (result == MPE_SI_NOT_FOUND || result == MPE_SI_NOT_AVAILABLE || result
            == MPE_SI_NOT_AVAILABLE_YET)
    {
        result = mpe_siCreateDSGServiceHandle(appId, PROGRAM_NUMBER_UNKNOWN,
                NULL, NULL, &serviceHandle);
    }

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeRegisterForHNPSIAcquisition
(JNIEnv *env, jobject siDatabaseImpl, jint hnStreamSession)
{
    mpe_Error result;
    mpe_SiTransportStreamHandle tsHandle = 0;
    mpe_SiServiceHandle         serviceHandle = 0;
    jbyteArray                  array = NULL;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeRegisterForHNPSIAcquisition enter\n");

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
       return NULL;

    // Get the service handle from the native SI database
    result = mpe_siRegisterForHNPSIAcquisition((mpe_HnStreamSession) hnStreamSession, &serviceHandle
                                             ,&tsHandle);

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
    goto error;

    // Create an int array of handles
    array = (*env)->NewIntArray(env, 2);

    // Populate Service Handle
    (*env)->SetIntArrayRegion(env, array, 0, 1, (jint*)&serviceHandle);

    // Populate Transport Handle
    (*env)->SetIntArrayRegion(env, array, 1, 1, (jint*)&tsHandle);

    mpe_siUnLock();
    return array;

    error:
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeRegisterForHNPSIAcquisition done\n");
    mpe_siUnLock();
    return NULL;
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeRegisterForPSIAcquisition
(JNIEnv *env, jobject siDatabaseImpl, jint sihandle)
{
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    // Get the service handle from the native SI database
    result = mpe_siRegisterForPSIAcquisition(sihandle);

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
    goto error;

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
    return;
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeUnregisterForPSIAcquisition
(JNIEnv *env, jobject siDatabaseImpl, jint sihandle)
{
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    // Get the service handle from the native SI database
    result = mpe_siUnRegisterForPSIAcquisition(sihandle);

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
    goto error;

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
    return;
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeUnregisterForHNPSIAcquisition
(JNIEnv *env, jobject siDatabaseImpl, jint hnStreamSession)
{
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeUnregisterForHNPSIAcquisition enter\n");

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        return;

    // use the HN session handle to unregister
    result = mpe_siUnRegisterForHNPSIAcquisition((mpe_HnStreamSession) hnStreamSession);

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
    goto error;

    mpe_siUnLock();
    return;

    error:
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeUnregisterForHNPSIAcquisition done\n");
    mpe_siUnLock();
    return;
}

/**
 * Get a service handle based on a source ID.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param sourceID
 *      The source ID of the service
 * @return
 *      The service handle of the service with the specified source ID
 * @throws SIRequestInvalidException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceBySourceID(
        JNIEnv *env, jobject siDatabaseImpl, jint sourceID)
{
    mpe_SiServiceHandle serviceHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle from the native SI database
    result = mpe_siGetServiceHandleBySourceId(sourceID, &serviceHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get all service details handle(s) based on a source ID.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param sourceID
 *      The source ID of the service
 * @return
 *      The service details handle of the service with the specified source ID
 * @throws SIRequestInvalidException
 */
JNIEXPORT jintArray JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceDetailsBySourceID(
        JNIEnv *env, jobject siDatabaseImpl, jint sourceID)
{
    uint32_t length=0;
    jintArray ret;
    jint *cArr;
    mpe_Error result;
    uint32_t *array = NULL;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Use a fixed size array for non-unique sourceId service handles
    length = NUMBER_NON_UNIQUE_SOURCEIDS;

	{
		// Allocate the array to hold service details handles
		mpe_memAllocP(MPE_MEM_SI, sizeof(uint32_t)*length, (void**)&array);

		result = mpe_siGetAllServiceHandlesForSourceId(sourceID, array, &length);

		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
				"<<JNI-SIDB>> nativeGetServiceDetailsBySourceID - returned result:%d length:%d\n", result, length);
		if(result == MPE_SUCCESS && length > NUMBER_NON_UNIQUE_SOURCEIDS)
		{
			// There are more service details for this sourceId than 10
			// Free the allocated array
			mpe_memFreeP(MPE_MEM_SI, array);

			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
					"<<JNI-SIDB>> nativeGetServiceDetailsBySourceID - re-acquiring details..\n");

			// Re-allocate the array to hold service details handles
			mpe_memAllocP(MPE_MEM_SI, sizeof(uint32_t)*length, (void**)&array);

			result = mpe_siGetAllServiceHandlesForSourceId(sourceID, array, &length);
		}
		if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
			goto error;
	}

    mpe_siUnLock();
    {
    	int i=0;
		//get access to the array
    	ret = (*env)->NewIntArray(env, length);
    	if(ret != NULL)
    	{
    		cArr = (*env)->GetIntArrayElements(env, ret, NULL);
    		if (cArr != NULL)
    		{
    			for(i=0;i<length;i++)
    			{
    				cArr[i] = array[i];
    			}
    		    (*env)->ReleaseIntArrayElements(env, ret, cArr, 0);
    		    mpe_memFreeP(MPE_MEM_SI, array);
    	    }
    	}
    }
    return ret;

    error:
    mpe_siUnLock();
    return NULL;
}

/**
 * Get a service handle based on a service name
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceName
 *      The service name of the service
 * @return
 *      The service handle of the service with the specified service name
 * @throws SIRequestInvalidException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceByServiceName(
        JNIEnv *env, jobject siDatabaseImpl, jstring serviceName)
{
    mpe_SiServiceHandle serviceHandle;
    const char *cServiceName;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle from the native SI database
    if ((cServiceName = (*env)->GetStringUTFChars(env, serviceName, NULL))
            == NULL)
        goto error;

    result = mpe_siGetServiceHandleByServiceName((char *) cServiceName,
            &serviceHandle);
    (*env)->ReleaseStringUTFChars(env, serviceName, cServiceName);
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get a service handle based on a frequency, program number and modulation
 * format.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param frequency
 *      The frequency that carries the service
 * @param programNumber
 *      The program number of the service
 * @param modulationFormat
 *      The modulation format used to carry the service
 * @return
 *      The service handle of the service with the specified frequency, program
 *      number and modulation format.
 * @throws SIRequestInvalidException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceByProgramNumber(
        JNIEnv *env, jobject siDatabaseImpl, jint frequency,
        jint modulationFormat, jint programNumber)
{
    mpe_Error result;
    mpe_SiServiceHandle serviceHandle;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service handle from the native SI database
    result = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
            frequency, modulationFormat, programNumber, &serviceHandle);

    // may be a dynamic service
    if (result == MPE_SI_NOT_FOUND || result == MPE_SI_NOT_AVAILABLE || result
            == MPE_SI_NOT_AVAILABLE_YET)
    {
        if (modulationFormat == -1)
        {
            // Per OCAP Spec an unknown/unspecified modulation is returned as -1
            // But the MPE layer expects a '0' for unknown modulation per SCTE-65
            modulationFormat = MPE_SI_MODULATION_UNKNOWN;
        }
        result = mpe_siCreateDynamicServiceHandle(frequency, programNumber,
                modulationFormat, &serviceHandle);
    }

    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return serviceHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Fetch a transport handle by transportID.
 *
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTransportHandleByTransportId(
        JNIEnv *env, jobject siDatabaseImpl, jint transportId)
{
    mpe_Error result;
    mpe_SiTransportHandle outHandle;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    result = mpe_siGetTransportHandleByTransportId(transportId, &outHandle);
    if (checkReturnCode(env, result, TRUE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return outHandle;

    error: mpe_siUnLock();
    return -1;
}

/*
 * Populate a TransportData object from the native SI database
 * @param env - the JNI environment
 * @param siDatabaseImpl - the calling SIDatabase object
 * @param transportHandle - the handle of the transport to "create"
 * @param transportData - the object to populate with the Transport's information.
 *
 * @throws SIRequestInvalidException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateTransport
(JNIEnv *env,jobject siDatabaseImpl,jint transportHandle,jobject transportData)
{
    jfieldID field;
    jobject deliverySystemTypeObject;
    mpe_SiDeliverySystemType deliveryType;
    uint32_t transportId;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    result = mpe_siGetTransportDeliverySystemType(transportHandle,&deliveryType);
    if (checkReturnCode(env,result,TRUE,FALSE,FALSE) == MPE_SUCCESS)
    {
        field = resolveDeliverySystemType(deliveryType);
    }
    else
    goto error;

    deliverySystemTypeObject = (*env)->GetStaticObjectField(env,
            jniutil_CachedIds.DeliverySystemType,field);

    (*env)->SetObjectField(env, transportData,
            jniutil_CachedIds.TransportData_deliverySystemType, deliverySystemTypeObject);

    result = mpe_siGetTransportIdForTransportHandle(transportHandle, &transportId);
    if (checkReturnCode(env, result, TRUE, FALSE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, transportData,
                jniutil_CachedIds.TransportData_transportId, transportId);
    }
    else
    goto error;

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
}

/**
 * Populate a TransportStreamData object from the native SI database.
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param tranportStreamHandle
 *      The transportStreamHandle for the TransportStream we're looking for.
 * @param transportStreamData
 *      The TransportStreamData object to populate
 * @throws SIRequestInvalidException SINotAvailableException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateTransportStream
(JNIEnv *env, jobject siDatabaseImpl, jint transportStreamHandle, jobject transportStreamData)
{
    //stream description local vars
    char *streamDescription = NULL;
    jobject streamDescriptionObject;
    //tsid local var
    uint32_t frequency, tsId;
    // serviceinformationtype
    jfieldID siTypeField;
    jobject serviceInformationTypeObject;
    mpe_SiServiceInformationType serviceType;
    // update time var
    mpe_TimeMillis updateTime;
    mpe_Error result;
    mpe_SiTransportHandle transportHandle;
    mpe_SiNetworkHandle networkHandle;
    mpe_SiModulationMode modulationFormat;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    //lock the sidb.
    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    //stream description
    result = mpe_siGetDescriptionForTransportStreamHandle(transportStreamHandle, &streamDescription);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        if (streamDescription != NULL)
        {
            streamDescriptionObject = (*env)->NewStringUTF(env, streamDescription);
            (*env)->SetObjectField(env, transportStreamData,
                    jniutil_CachedIds.TransportStreamData_description, streamDescriptionObject);
        }
    }
    else
    {
        goto error;
    }
    //frequency
    result = mpe_siGetFrequencyForTransportStreamHandle(transportStreamHandle,&frequency);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> frequency: %x\n",frequency);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,transportStreamData,
                jniutil_CachedIds.TransportStreamData_frequency,frequency);
    }
    else
    goto error;
    //modulationFormat
    result = mpe_siGetModulationFormatForTransportStreamHandle(transportStreamHandle,&modulationFormat);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> modulation format: %x\n",modulationFormat);
    if (checkReturnCode(env,result,TRUE,TRUE,FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env,transportStreamData,
                jniutil_CachedIds.TransportStreamData_modulationFormat,modulationFormat);
    }
    else
    goto error;
    //tsid
    result = mpe_siGetTransportStreamIdForTransportStreamHandle(transportStreamHandle, &tsId);
    if (result != MPE_SUCCESS)
    {
        tsId = (uint32_t)-1;
    }
    (*env)->SetIntField(env, transportStreamData,
            jniutil_CachedIds.TransportStreamData_transportStreamId, tsId);

    //ServiceInformationType
    result = mpe_siGetTransportStreamServiceInformationType(transportStreamHandle,&serviceType);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        siTypeField = resolveServiceInformationType(serviceType);
        serviceInformationTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.ServiceInformationType,siTypeField);

        (*env)->SetObjectField(env, transportStreamData,
                jniutil_CachedIds.TransportStreamData_serviceInformationType, serviceInformationTypeObject);
    }
    else
    {
        goto error;
    }

    //update time
    result = mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle(transportStreamHandle,&updateTime);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetLongField(env, transportStreamData,
                jniutil_CachedIds.TransportStreamData_lastUpdate, updateTime);
    }
    else
    goto error;

    //network handle
    result = mpe_siGetNetworkHandleForTransportStreamHandle(transportStreamHandle, &networkHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, transportStreamData,
                jniutil_CachedIds.TransportStreamData_networkHandle, networkHandle);
    }
    else
    goto error;

    //transport handle
    result = mpe_siGetTransportHandleForTransportStreamHandle(transportStreamHandle, &transportHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, transportStreamData,
                jniutil_CachedIds.TransportStreamData_transportHandle, transportHandle);
    }
    else
    goto error;

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
}

/**
 * Populate a ServiceData object from the native SI database.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceHandle
 *      The handle to the service
 * @param serviceData
 *      The ServiceData object to populate
 * @throws SIRequestInvalidException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateService
(JNIEnv *env, jobject siDatabaseImpl, jint serviceHandle, jobject serviceData)
{
    jfieldID field;
    jobject serviceTypeObject;
    mpe_SiServiceType serviceType;
    uint32_t serviceNumber, minorNumber, sourceID, appId, frequency, programNumber, modulationFormat;
    uint32_t numberOfNames;
    char **serviceNames = NULL;
    char **languages = NULL;
    mpe_Bool hasMultipleInstances;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> SIDatabaseImpl_nativeCreateService... \n");

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    // fetch the service names/languages
    if (checkReturnCode(env, mpe_siGetNumberOfNamesForServiceHandle(serviceHandle,&numberOfNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (numberOfNames > 0)
    {
        // Allocate space for our names and languages
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&serviceNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&languages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

        // Get the name/language arrays for this service and create the associated Java
        // string arrays in our ServiceData object
        result = mpe_siGetNamesForServiceHandle(serviceHandle, languages, serviceNames);
        if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
        {
            uint32_t i;
            jclass stringClass = (*env)->FindClass(env,"java/lang/String");
            jobjectArray javaLangs = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);
            jobjectArray javaNames = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);

            // Create our java strings and insert them into the arrays
            for (i = 0; i < numberOfNames; ++i)
            {
                jstring lang = (*env)->NewStringUTF(env,languages[i]);
                jstring name = (*env)->NewStringUTF(env,serviceNames[i]);
                (*env)->SetObjectArrayElement(env,javaLangs,i,lang);
                (*env)->SetObjectArrayElement(env,javaNames,i,name);
            }

            // Set the lang/name arrays into the ServiceData object
            (*env)->SetObjectField(env, serviceData,
                    jniutil_CachedIds.ServiceData_serviceLanguages,
                    javaLangs);
            (*env)->SetObjectField(env, serviceData,
                    jniutil_CachedIds.ServiceData_serviceNames,
                    javaNames);
        }
        else
        goto error;
    }

    // Get hasMultipleInstances and store it in the data object
    result = mpe_siGetMultipleInstancesFlagForServiceHandle(serviceHandle,&hasMultipleInstances);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        if (hasMultipleInstances == 1)
        (*env)->SetBooleanField(env, serviceData,
                jniutil_CachedIds.ServiceData_hasMultipleInstances, JNI_TRUE);
        else
        (*env)->SetBooleanField(env, serviceData,
                jniutil_CachedIds.ServiceData_hasMultipleInstances, JNI_FALSE);
    }
    else
    goto error;

    // Get service type and store it in the data object
    result = mpe_siGetServiceTypeForServiceHandle(serviceHandle, &serviceType);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        //resolve the service type to a java field.
        field = resolveServiceType(serviceType);
        serviceTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.ServiceType, field);
        (*env)->SetObjectField(env, serviceData,
                jniutil_CachedIds.ServiceData_serviceType, serviceTypeObject);
    }
    else
    goto error;

    result = mpe_siGetServiceNumberForServiceHandle(serviceHandle, &serviceNumber, &minorNumber);
    // Get service number and minor number and store them in the data object
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_serviceNumber, serviceNumber);
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_minorNumber, minorNumber);
    }
    else
    goto error;

    // Get source ID and store it in the data object
    result = mpe_siGetSourceIdForServiceHandle(serviceHandle, &sourceID);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_sourceID, sourceID);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> sourceID: 0x%x\n", sourceID);
    }
    else
    goto error;

    // Get app ID and store it in the data object
    result = mpe_siGetAppIdForServiceHandle(serviceHandle, &appId);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_appID, appId);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> appId: 0x%x\n", appId);
    }
    else
    goto error;

    // Get frequency and store it in the data object
    result = mpe_siGetFrequencyForServiceHandle(serviceHandle, &frequency);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_frequency, frequency);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> frequency: 0x%x\n", frequency);
    }
    else
    goto error;

    // Get program number and store it in the data object
    result = mpe_siGetProgramNumberForServiceHandle(serviceHandle, &programNumber);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_programNumber, programNumber);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> program number: 0x%x\n", programNumber);
    }
    else
    goto error;

    // Get modulation format and store it in the data object
    result = mpe_siGetModulationFormatForServiceHandle(serviceHandle, (mpe_SiModulationMode*)&modulationFormat);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, serviceData,
                jniutil_CachedIds.ServiceData_modulationFormat, modulationFormat);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> modulation format: 0x%x\n", modulationFormat);
    }
    else
    goto error;

    if (serviceNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,serviceNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
    return;

    error:
    if (serviceNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,serviceNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
}

/**
 * Get a service component handle based on a component PID.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param pid
 *      The PID of the service component
 * @return
 *      The service component handle of the component with the specified PID
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceComponentByPID(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle,
        jint pid)
{
    mpe_SiServiceComponentHandle componentHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the component handle from the native SI database
    result = mpe_siGetServiceComponentHandleByPid(serviceDetailsHandle, pid,
            &componentHandle);

    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return componentHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get a service component handle based on a component tag.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param tag
 *      The component tag of the service component
 * @return
 *      The service component handle of the component with the specified component tag
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceComponentByTag(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle,
        jint tag)
{
    mpe_SiServiceComponentHandle componentHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the component handle from the native SI database
    result = mpe_siGetServiceComponentHandleByTag(serviceDetailsHandle, tag,
            &componentHandle);

    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return componentHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get a service component handle based on a component name.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param name
 *      The component name of the service component
 * @return
 *      The service component handle of the component with the specified component name
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetServiceComponentByName(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle,
        jstring componentName)
{
    mpe_SiServiceComponentHandle componentHandle;
    const char *cComponentName;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the service component handle from the native SI database
    if ((cComponentName = (*env)->GetStringUTFChars(env, componentName, NULL))
            == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return -1;
    }

    result = mpe_siGetServiceComponentHandleByName(serviceDetailsHandle,
            (char *) cComponentName, &componentHandle);
    (*env)->ReleaseStringUTFChars(env, componentName, cComponentName);

    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return componentHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get the default carousel for the specified service details
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceDetailsHandle
 *      The handle to the service details that carries the carousel
 * @return
 *      The service component handle of the component that carries the carousel
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetCarouselComponentByServiceDetails__I(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle)
{
    mpe_SiServiceComponentHandle componentHandle = 0;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the component handle from the native SI database
    result = mpe_siGetServiceComponentHandleForDefaultCarousel(
            serviceDetailsHandle, &componentHandle);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeGetCarouselComponentByServiceDetails__I.  \n");
    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return componentHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get the carousel with the specified carousel ID for the specified service details
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param carouselID
 *      The carousel ID
 * @param serviceDetailsHandle
 *      The handle to the service details that carries the carousel
 * @return
 *      The service component handle of the component that carries the carousel
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetCarouselComponentByServiceDetails__II(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle,
        jint carouselID)
{
    mpe_SiServiceComponentHandle componentHandle;
    mpe_Error result;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Get the component handle from the native SI database
    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeGetCarouselComponentByServiceDetails: serviceDetailsHandle:0x%x, carouselID:%d  \n",
            (int)serviceDetailsHandle, (int)carouselID);
    result = mpe_siGetServiceComponentHandleByCarouselId(serviceDetailsHandle,
            carouselID, &componentHandle);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<JNI-SIDB>> nativeGetCarouselComponentByServiceDetails__II.  \n");
    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return componentHandle;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get PCR Pid given a ServiceDetails handle
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceDetailsHandle
 *      The associated serviceDetailsHandle
 * @return
 *      The PCR Pid
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetPCRPidForServiceDetails(
        JNIEnv *env, jobject siDatabaseImpl, jint serviceDetailsHandle)
{
    mpe_Error result;
    uint32_t pcrPID;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // PCR PID
    result = mpe_siGetPcrPidForServiceHandle(serviceDetailsHandle, &pcrPID);

    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return pcrPID;

    error: mpe_siUnLock();
    return -1;
}

/**
 * Get ts id given a transport stream handle
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param serviceDetailsHandle
 *      The associated transport stream
 * @return
 *      The tsId
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetTsIDForTransportStream(
        JNIEnv *env, jobject siDatabaseImpl, jint tsHandle)
{
    mpe_Error result;
    uint32_t tsId;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // Ts Id
    result = mpe_siGetTransportStreamIdForTransportStreamHandle(tsHandle, &tsId);

    // Throw an exception on error
    if (checkReturnCode(env, result, TRUE, TRUE, TRUE) != MPE_SUCCESS)
        goto error;

    mpe_siUnLock();
    return tsId;

    error: mpe_siUnLock();
    return -1;
}

/**
 *  Implement the PID algorithm...cannot be done directly inside a JNI call, but can be called directly
 *  from a JNI call. Note that while this is called the PID algorithm, it is implemented in a way here
 *  that it is being used to search for the correct service component. It is a matter of finding the
 *  association tag, and if not found, calculating a new siHandle and using that to find the association
 *  tag, until eventually found with a matching service component, which then gets returned. A zero is
 *  returned if an error has occurred.
 *
 * @param assocTag
 *      The association tag
 * @param siHandle
 *      The handle to the service details where the search should be started
 * @return
 *      The service component handle of the component with the specified association tag
 */
/* TODO: Put a depth limit in to avoid infinite recursion in the case of broken streams.
 * TODO: Just generally don't like the structure.  Hate while(1) ...if (done) break
 * TODO: This is basically duplicated in ocuTranslateAssociationTag().  Nice if we could
 *       have it in only one place.
 */
static
int pidalgorithm(mpe_SiServiceHandle siHandle, uint16_t assocTag)
{
    mpe_Error retCode;
    mpe_SiServiceComponentHandle componentHandle = 0;
    mpe_SiServiceHandle newHandle;
    uint32_t prognum;
    uint32_t frequency;
    mpe_SiModulationMode mode;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "<<pidalgorithm>> siHandle:0x%x, assocTag:%d\n", siHandle, assocTag);

    retCode = mpe_siLockForRead(); // Lock the SI Database
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "<<pidalgorithm>> Couldn't lock SI DB, Handle = 0x%x\n",
                siHandle);
        return 0;
    }
    while (1)
    {
        retCode = mpe_siGetProgramNumberByDeferredAssociationTag(siHandle,
                assocTag, &prognum);
        if (retCode != MPE_SUCCESS)
        {
            break;
        }

        // Get Frequency for this entry
        retCode = mpe_siGetFrequencyForServiceHandle(siHandle, &frequency);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "<<pidalgorithm>> Unable to get frequency for siHandle 0x%x\n",
                    siHandle);
            goto CleanAssoc;
        }

        // Get modulation foramt for this entry
        retCode = mpe_siGetModulationFormatForServiceHandle(siHandle, &mode);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "<<pidalgorithm>> Unable to get mode for siHandle 0x%x\n",
                    siHandle);
            goto CleanAssoc;
        }

        // Get new siHandle
        retCode = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
                frequency, mode, prognum, &newHandle);

        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "<<pidalgorithm>> Could not get SI handle for deferred association target, f = %d, p = %d\n",
                    frequency, prognum);
            goto CleanAssoc;
        }
    }

    // break from: retCode = mpe_siGetProgramNumberByDeferredAssociationTag(siHandle, assocTag, &prognum);
    if (retCode == MPE_SI_NOT_FOUND) // If assoc tag not found
    {
        retCode = mpe_siGetServiceComponentHandleByAssociationTag(siHandle,
                (uint16_t) assocTag, &componentHandle);
        if (retCode == MPE_SI_NOT_FOUND) // Association tag not found
        {
            retCode = mpe_siGetServiceComponentHandleByTag(siHandle, (assocTag
                    & 0x00ff), &componentHandle);
        }
    }
    CleanAssoc: mpe_siUnLock();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "<<pidalgorithm>> Unable to get transport stream corresponding to Association tag %08x\n",
                assocTag);
        return 0;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "<<pidalgorithm>> Translated Assocation Tag %08x into service component\n",
            assocTag);
    return componentHandle;
} // pidalgorithm

/**
 * Get the component with the specified association tag for the specified service details.
 * Note that this function performs the PID retrieval algorithm. This algorithm really just
 * tries to get the service Component based on association tag, and if it cannot find the
 * tag, it gets the frequency for this siHandle, and gets a new siHandle, and then, calls this
 * function recursively to try again, until the association tag is found.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param assocTag
 *      The association tag
 * @param siHandle
 *      The handle to the service details where the search should be started
 * @return
 *      The service component handle of the component with the specified association tag
 *
 * @throws SIRequestInvalidException SINotAvailableException and SINotAvailableYetException
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeGetComponentByAssociationTag(
        JNIEnv *env, jobject siDatabaseImpl, jint siHandle, jint assocTag)
{
    //uint32_t pidval;
    MPE_UNUSED_PARAM(siDatabaseImpl);
    MPE_UNUSED_PARAM(env);

    // Call pid algorithm.....but return a service Component Handle.
    //(void)mpe_siGetPidByAssociationTag((mpe_SiServiceHandle)siHandle, (uint16_t)assocTag, &pidval);
    //return pidval;
    return pidalgorithm((mpe_SiServiceHandle) siHandle, (uint16_t) assocTag);
}

/**
 * Populate a ServiceComponentData object from the native SI database.
 *
 * @param env
 *      The JNI environment
 * @param siDatabaseImpl
 *      The SIDatabaseImpl object making the JNI call
 * @param componentHandle
 *      The handle to the service component
 * @param componentData
 *      The ServiceComponentData object to populate
 *
 * @throws SIRequestInvalidException SINotAvailableException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeCreateServiceComponent
(JNIEnv *env, jobject siDatabaseImpl, jint componentHandle, jobject componentData)
{
    mpe_Error result;
    //jfieldID field;
    int tag;
    uint16_t assoc_tag;
    uint32_t numberOfNames, pid, carouselID;
    char** componentNames = NULL;
    char** languages = NULL;
    char *associatedLanguage = NULL;
    jobject associatedLanguageObject;
    jobject serviceInformationTypeObject;
    mpe_SiElemStreamType streamType;
    mpe_SiServiceDetailsHandle serviceDetailsHandle;
    mpe_TimeMillis updateTime;
    // serviceinformationtype
    jfieldID siTypeField;

    mpe_SiServiceInformationType serviceType;

    MPE_UNUSED_PARAM(siDatabaseImpl);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;
    // Get PID and store it in the data object
    result = mpe_siGetPidForServiceComponentHandle(componentHandle, &pid);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, componentData,
                jniutil_CachedIds.ServiceComponentData_componentPID, pid);
    }
    else
    goto error;

    // Get component tag and store it in the data object. If there is no tag
    // for this component then leave the data field unassigned so it retains
    // its initial value.
    result = mpe_siGetComponentTagForServiceComponentHandle(componentHandle, &tag);

    if (result != MPE_SI_NOT_FOUND)
    {
        if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
        {
            (*env)->SetLongField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_componentTag, tag);
        }
        else
        goto error;
    }

    // Get association tag and store it in the data object. If there is no tag
    // for this component then leave the data field unassigned so it retains
    // its initial value.
    result = mpe_siGetAssociationTagForServiceComponentHandle(componentHandle, &assoc_tag);
    if (result != MPE_SI_NOT_FOUND)
    {
        if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
        {
            (*env)->SetLongField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_associationTag, assoc_tag);
        }
        else
        goto error;
    }

    // Get carousel ID and store it in the data object. If there is no carousel ID
    // for this component then leave the data field unassigned so it retains
    // its initial value.
    result = mpe_siGetCarouselIdForServiceComponentHandle(componentHandle, &carouselID);
    if (result != MPE_SI_NOT_FOUND)
    {
        if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
        {
            (*env)->SetLongField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_carouselID, carouselID);
        }
        else
        goto error;
    }

    // Get component names/languages and store it in the data object
    if (checkReturnCode(env, mpe_siGetNumberOfNamesForServiceComponentHandle(componentHandle,&numberOfNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    if (numberOfNames > 0)
    {
        // Allocate space for our names and languages
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&componentNames), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;
        if (checkReturnCode(env, mpe_memAllocP(MPE_MEM_SI, sizeof(char*)*numberOfNames,(void**)&languages), FALSE, FALSE, FALSE) != MPE_SUCCESS)
        goto error;

        // Get the name/language arrays for this service component and create the associated Java
        // string arrays in our ServiceComponentData object
        result = mpe_siGetNamesForServiceComponentHandle(componentHandle, languages, componentNames);
        if (checkReturnCode(env, result, FALSE, FALSE, FALSE) == MPE_SUCCESS)
        {
            uint32_t i;
            jclass stringClass = (*env)->FindClass(env,"java/lang/String");
            jobjectArray javaLangs = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);
            jobjectArray javaNames = (*env)->NewObjectArray(env,numberOfNames,stringClass,NULL);

            // Create our java strings and insert them into the arrays
            for (i = 0; i < numberOfNames; ++i)
            {
                jstring lang = (*env)->NewStringUTF(env,languages[i]);
                jstring name = (*env)->NewStringUTF(env,componentNames[i]);
                (*env)->SetObjectArrayElement(env,javaLangs,i,lang);
                (*env)->SetObjectArrayElement(env,javaNames,i,name);
            }

            // Set the lang/name arrays into the ServiceData object
            (*env)->SetObjectField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_componentLangs,
                    javaLangs);
            (*env)->SetObjectField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_componentNames,
                    javaNames);
        }
        else
        goto error;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<<JNI-SIDB>> Getting ServiceComponent associated language...\n");
    // Get associated language and store it in the data object
    result = mpe_siGetLanguageForServiceComponentHandle(componentHandle, &associatedLanguage);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        if (associatedLanguage != NULL)
        {
            associatedLanguageObject = (*env)->NewStringUTF(env, associatedLanguage);
            (*env)->SetObjectField(env, componentData,
                    jniutil_CachedIds.ServiceComponentData_associatedLanguage, associatedLanguageObject);
        }
    }
    else
    goto error;

    // Get stream type and store it in the data object
    result = mpe_siGetStreamTypeForServiceComponentHandle(componentHandle, &streamType);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetShortField(env, componentData,
                jniutil_CachedIds.ServiceComponentData_streamType, (short)streamType);
    }
    else
    goto error;

    result = mpe_siGetServiceInformationTypeForServiceComponentHandle(componentHandle,&serviceType);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        siTypeField = resolveServiceInformationType(serviceType);
        serviceInformationTypeObject = (*env)->GetStaticObjectField(env,
                jniutil_CachedIds.ServiceInformationType,siTypeField);
        (*env)->SetObjectField(env, componentData,
                jniutil_CachedIds.ServiceComponentData_serviceInformationType, serviceInformationTypeObject);
    }
    else
    goto error;

    //last update time
    result = mpe_siGetComponentLastUpdateTimeForServiceComponentHandle(componentHandle,&updateTime);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetLongField(env,componentData,jniutil_CachedIds.ServiceComponentData_updateTime,updateTime);
    }
    else
    goto error;

    //serviceDetailsHandle for the serviceComponentHandle
    result = mpe_siGetServiceDetailsHandleForServiceComponentHandle(componentHandle, &serviceDetailsHandle);
    if (checkReturnCode(env, result, TRUE, TRUE, FALSE) == MPE_SUCCESS)
    {
        (*env)->SetIntField(env, componentData,
                jniutil_CachedIds.ServiceComponentData_serviceDetailsHandle, serviceDetailsHandle);
    }
    else
    goto error;

    if (componentNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,componentNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
    return;

    error:
    if (componentNames != NULL)
    mpe_memFreeP(MPE_MEM_SI,componentNames);
    if (languages != NULL)
    mpe_memFreeP(MPE_MEM_SI,languages);
    mpe_siUnLock();
}

/*
 * Class:     org_cablelabs_impl_manager_service_SIDatabaseImpl
 * Method:    nativeEventRegistration
 * Signature: (Lorg/cablelabs/impl/manager/service/SIDatabaseImpl;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeEventRegistration
(JNIEnv *env , jobject caller)
{
    mpe_EdEventInfo *edHandle;

    MPE_UNUSED_PARAM(env);

    /* Create the ED handle and pass it to the native SI manager */
    (void)mpe_edCreateHandle(caller, MPE_ED_QUEUE_NORMAL, siEventCallback, MPE_ED_TERMINATION_OPEN, 0, &edHandle);
    (void)mpe_siRegisterForSIEvents(edHandle,0,0);
}

/*
 * Class:     org_cablelabs_impl_manager_service_SIDatabaseImpl
 * Method:    nativeReleaseServiceComponentHandle
 * Signature: (I)I
 *
 * Release the ServiceComponent handle
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIDatabaseImpl_nativeReleaseServiceComponentHandle
(JNIEnv *env, jclass caller, jint serviceComponentHandle)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(caller);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE) != MPE_SUCCESS)
    goto error;

    (void)mpe_siReleaseServiceComponentHandle(serviceComponentHandle);

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
}

void siEventCallback(JNIEnv *env, void* listenerObject,
        mpe_EdEventInfo *edHandle, uint32_t *evCode, void** data1,
        void** data2, uint32_t* data3)
{
    mpe_SiTransportStreamHandle tsHandle;
    uint32_t eventCode = *evCode;
    uint32_t changeType = *data3;

    MPE_UNUSED_PARAM(edHandle);
    MPE_UNUSED_PARAM(data2);

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // In-Band Program Association Table
    if (eventCode == MPE_SI_EVENT_IB_PAT_ACQUIRED || eventCode
            == MPE_SI_EVENT_IB_PAT_UPDATE)
    {
        if (changeType != MPE_SI_CHANGE_TYPE_REMOVE)
        {
            // Find the transport stream handle associated with this service
            tsHandle = (mpe_SiTransportStreamHandle) * data1;
            (*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    createPATByteArray(env, tsHandle));
        }
        else
        { // case MPE_SI_CHANGE_TYPE_REMOVE
            uint32_t totalSize = 0;
            uint32_t currentPos = 0;
            jbyteArray array = NULL;
            uint32_t frequency = 0;
            int32_t tsId = -1;
            uint32_t numPrograms = 0;

            tsHandle = (mpe_SiTransportStreamHandle) * data1;

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "<<JNI-SIDB>> ChangeType is MPE_SI_CHANGE_TYPE_REMOVE, tsHandle: %d...\n",
                    tsHandle);

            totalSize += sizeof(uint32_t); // tsHandle

            // TSID
            totalSize += sizeof(uint32_t); // tsId

            // FREQUENCY
            if (mpe_siGetFrequencyForTransportStreamHandle(tsHandle, &frequency)
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "Error getting frequency for transport stream handle\n");
                goto error;
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "frequency = %d\n", frequency);
            totalSize += sizeof(uint32_t); // frequency

            // NUM PROGRAMS
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "numPrograms = %d\n",
                    numPrograms);
            totalSize += sizeof(uint32_t);

            // Create a byte array of size 'totalSize' and populate all the fields
            array = (*env)->NewByteArray(env, totalSize);

            // TRANSPORT STREAM HANDLE
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &tsHandle);
            currentPos += sizeof(uint32_t);

            // TSID
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(int32_t), (jbyte*) &tsId);
            currentPos += sizeof(int32_t);

            // FREQUENCY
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &frequency);
            currentPos += sizeof(uint32_t);

            // NUM PROGRAMS
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &numPrograms);
            (*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    array);
        }
    }
    // Out-of-Band Program Association Table
    else if (eventCode == MPE_SI_EVENT_OOB_PAT_ACQUIRED || eventCode
            == MPE_SI_EVENT_OOB_PAT_UPDATE)
    {
        if (changeType != MPE_SI_CHANGE_TYPE_REMOVE)
        {
            // Find the transport stream handle associated with the OOB frequency
            tsHandle = (mpe_SiTransportStreamHandle) * data1;
			(*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    createPATByteArray(env, tsHandle));
        }
        else
        { // case MPE_SI_CHANGE_TYPE_REMOVE
            uint32_t frequency = 0;
            uint32_t totalSize = 0;
            uint32_t currentPos = 0;
            jbyteArray array = NULL;
            uint32_t numPrograms = 0;
            int32_t tsId = -1;

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "<<JNI-SIDB>> ChangeType is MPE_SI_CHANGE_TYPE_REMOVE, setting tsHandle...\n");
            tsHandle = (mpe_SiTransportStreamHandle) * data1;

            // FREQUENCY
            if (mpe_siGetFrequencyForTransportStreamHandle(tsHandle, &frequency)
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "Error getting frequency for transport stream handle\n");
                goto error;
            }

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "<<JNI-SIDB>> ChangeType is MPE_SI_CHANGE_TYPE_REMOVE, tsHandle: %d...\n",
                    tsHandle);

            totalSize += sizeof(uint32_t); // tsHandle

            totalSize += sizeof(uint32_t); // tsId

            totalSize += sizeof(uint32_t); // frequency

            totalSize += sizeof(uint32_t); // NUM PROGRAMS

            // Create a byte array of size 'totalSize' and populate all the fields
            array = (*env)->NewByteArray(env, totalSize);

            // TRANSPORT STREAM HANDLE
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &tsHandle);
            currentPos += sizeof(uint32_t);

            // TSID
            (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                    (jbyte*) &tsId);
            currentPos += sizeof(int32_t);

            // FREQUENCY
            if (frequency == MPE_SI_OOB_FREQUENCY || frequency == MPE_SI_DSG_FREQUENCY)
            {
                int32_t oobFrequency = -1;
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(int32_t), (jbyte*) &oobFrequency);
                currentPos += sizeof(int32_t);
            }
            else if (frequency == MPE_SI_HN_FREQUENCY)
            {
                int32_t oobFrequency = -2;
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(int32_t), (jbyte*) &oobFrequency);
                currentPos += sizeof(int32_t);
            }

            // NUM PROGRAMS
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &numPrograms);
            (*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    array);
        }
    }
    // In-Band / Out-of-Band Program Map Table
    else if (eventCode == MPE_SI_EVENT_IB_PMT_ACQUIRED || eventCode
            == MPE_SI_EVENT_IB_PMT_UPDATE || eventCode
            == MPE_SI_EVENT_OOB_PMT_ACQUIRED || eventCode
            == MPE_SI_EVENT_OOB_PMT_UPDATE)
    {
        if (changeType != MPE_SI_CHANGE_TYPE_REMOVE)
        {

            // data1 is the service handle of the service that just had a PMT change
            (*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    createPMTByteArray(env, (mpe_SiServiceHandle) * data1));
        }
        else
        { // case MPE_SI_CHANGE_TYPE_REMOVE
            // build a PMT with no service components
            uint32_t totalSize = 0;
            uint32_t currentPos = 0;
            jbyteArray array = NULL;
            mpe_SiServiceHandle serviceHandle;
            uint32_t frequency = 0;
            uint32_t sourceID = 0;
            uint32_t programNumber = 0;
            int32_t undefinedSourceID = -1;
            uint32_t pcrPID = 0;
            uint32_t numOuterDescriptors = 0;
            uint32_t numServiceComponents = 0;

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "<<JNI-SIDB>> ChangeType is MPE_SI_CHANGE_TYPE_REMOVE, setting serviceHandle...\n");

            serviceHandle = (mpe_SiServiceHandle) * data1;

            // TRANSPORT STREAM HANDLE
            if (mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle,
                    &tsHandle) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "...Error getting transport stream handle for service handle\n");
                goto error;
            }

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "...transport stream handle = 0x%x\n", tsHandle);
            totalSize += sizeof(uint32_t); // tsHandle

            // SERVICE HANDLE
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "...serviceHandle: 0x%x\n",
                    serviceHandle);
            totalSize += sizeof(uint32_t); // serviceHandle

            // FREQUENCY
            if (mpe_siGetFrequencyForServiceHandle(serviceHandle, &frequency)
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "...Error getting frequency for service handle\n");
                goto error;
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "...frequency: 0x%x\n",
                    frequency);
            totalSize += sizeof(uint32_t); // frequency

            // SOURCE ID
            if (mpe_siGetSourceIdForServiceHandle(serviceHandle, &sourceID)
                    != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "...Error getting sourceID for service handle\n");
                goto error;
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "...sourceID: 0x%x\n", sourceID);
            totalSize += sizeof(uint32_t); // sourceID

            // PROGRAM NUMBER
            if (mpe_siGetProgramNumberForServiceHandle(serviceHandle,
                    &programNumber) != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                        "createPMTByteArray()...Error getting program number for service handle\n");
                goto error;
            }
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "createPMTByteArray()...programNumber: 0x%x\n",
                    programNumber);
            totalSize += sizeof(uint32_t); // programNumber

            totalSize += sizeof(uint32_t); // PCR_PID

            totalSize += sizeof(uint32_t); // NUM outer descriptors

            totalSize += sizeof(uint32_t); // NUM elem streams

            // Create a byte array of size 'totalSize' and populate all the fields
            array = (*env)->NewByteArray(env, totalSize);

            // TRANSPORT STREAM HANDLE
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &tsHandle);
            currentPos += sizeof(uint32_t);

            // SERVICE HANDLE
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &serviceHandle);
            currentPos += sizeof(uint32_t);

            // FREQUENCY
            if (frequency == MPE_SI_OOB_FREQUENCY || frequency == MPE_SI_DSG_FREQUENCY)
            {
                int32_t oobFrequency = -1;
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(int32_t), (jbyte*) &oobFrequency);
                currentPos += sizeof(int32_t);
            }
            else if (frequency == MPE_SI_HN_FREQUENCY)
            {
                int32_t oobFrequency = -2;
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(int32_t), (jbyte*) &oobFrequency);
                currentPos += sizeof(int32_t);
            }
            else
            {
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(uint32_t), (jbyte*) &frequency);
                currentPos += sizeof(uint32_t);
            }

            // SOURCE ID
            // Check that the sourceID is valid. Set to -1 if invalid.
            if (sourceID == SOURCEID_UNKNOWN)
            {
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(int32_t), (jbyte*) &undefinedSourceID);
                currentPos += sizeof(int32_t);
            }
            else
            {
                (*env)->SetByteArrayRegion(env, array, currentPos,
                        sizeof(uint32_t), (jbyte*) &sourceID);
                currentPos += sizeof(uint32_t);
            }

            // PROGRAM NUMBER
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &programNumber);
            currentPos += sizeof(uint32_t);

            // pcr PID
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &pcrPID);
            currentPos += sizeof(uint32_t);

            // NUM OUTER DESCRIPTORS
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &numOuterDescriptors);
            currentPos += sizeof(uint32_t);

            // NUM service components
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &numServiceComponents);
            (*env)->CallVoidMethod(env, listenerObject,
                    jniutil_CachedIds.SIDatabaseImpl_updatePatPmtData,
                    array);
        }
    }
    else
    {
        //MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "siEventCallback()... Setting patpmtData to NULL..\n");
    }

    mpe_siUnLock();
    return;

    error:
    mpe_siUnLock();
}

jbyteArray createPMTByteArray(JNIEnv *env, mpe_SiServiceHandle serviceHandle)
{
    uint32_t i;
    mpe_SiMpeg2DescriptorList *walker = NULL;

    uint32_t sourceID;
    uint32_t frequency;
    int32_t undefinedSourceID = -1;
    uint32_t programNumber;
    uint32_t pcrPID;
    mpe_SiTransportStreamHandle tsHandle;

    uint32_t numOuterDescriptors;
    mpe_SiMpeg2DescriptorList* outerDescList = NULL;

    uint32_t numServiceComponents;
    mpe_SiServiceComponentHandle* componentHandles = NULL;

    uint32_t* numESDescriptors = NULL;
    mpe_SiMpeg2DescriptorList** esDescriptorLists = NULL;

    uint32_t totalSize = 0;
    uint32_t currentPos = 0;

    jbyteArray array = NULL;

    if (serviceHandle == MPE_SI_INVALID_HANDLE)
        return NULL;

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // TRANSPORT STREAM HANDLE
    if (mpe_siGetTransportStreamHandleForServiceHandle(serviceHandle, &tsHandle)
            != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "createPMTByteArray()...Error getting transport stream handle for service handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...transport stream handle = 0x%x\n", tsHandle);
    totalSize += sizeof(uint32_t);

    // SERVICE HANDLE
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...serviceHandle: 0x%x\n", serviceHandle);
    totalSize += sizeof(uint32_t);

    // FREQUENCY
    if (mpe_siGetFrequencyForServiceHandle(serviceHandle, &frequency)
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "createPMTByteArray()...Error getting frequency for service handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...frequency: 0x%x\n", frequency);
    totalSize += sizeof(uint32_t);

    // SOURCE ID
    if (mpe_siGetSourceIdForServiceHandle(serviceHandle, &sourceID)
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "createPMTByteArray()...Error getting sourceID for service handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...sourceID: 0x%x\n", sourceID);
    totalSize += sizeof(uint32_t);

    // PROGRAM NUMBER
    if (mpe_siGetProgramNumberForServiceHandle(serviceHandle, &programNumber)
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "createPMTByteArray()...Error getting program number for service handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...programNumber: 0x%x\n", programNumber);
    totalSize += sizeof(uint32_t);

    // PCR PID
    if (mpe_siGetPcrPidForServiceHandle(serviceHandle, &pcrPID) != MPE_SUCCESS)
    {
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...pcrPID: 0x%x\n", pcrPID);
    totalSize += sizeof(uint32_t);

    // OUTER DESCRIPTORS
    if (mpe_siGetOuterDescriptorsForServiceHandle(serviceHandle,
            &numOuterDescriptors, &outerDescList) != MPE_SUCCESS)
    {
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...numOuterDescriptors: %d\n",
            numOuterDescriptors);
    totalSize += sizeof(uint32_t); // Num outer descriptors

    // Walk over outer descriptors to calculate data size
    walker = outerDescList;
    while (walker != NULL)
    {
        totalSize += 1; // Descriptor Tag
        totalSize += 1; // Descriptor Length
        totalSize += walker->descriptor_length; // Descriptor
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...outerdescriptor total length: %d\n",
                walker->descriptor_length + 2);
        walker = walker->next;
    }

    // Elementary Streams

    // NUMBER OF ELEMENTARY STREAMS
    if (mpe_siGetNumberOfComponentsForServiceHandle(serviceHandle,
            &numServiceComponents) != MPE_SUCCESS)
    {
        goto error;
    }
    if (numServiceComponents == 0)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_JNI,
                "createPMTByteArray()...no ServiceComponents...(May be PMT was never populated!!!)\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...numServiceComponents: %d\n",
            numServiceComponents);
    totalSize += sizeof(uint32_t); // Num service components

    // Allocate array for service component handles
    if (mpe_memAllocP(MPE_MEM_SI, sizeof(mpe_SiServiceComponentHandle)
            * numServiceComponents, (void**) &componentHandles) != MPE_SUCCESS)
    {
        goto error;
    }

    // Get service component handles
    if (mpe_siGetAllComponentsForServiceHandle(serviceHandle, componentHandles,
            &numServiceComponents) != MPE_SUCCESS)
    {
        goto error;
    }

    // Allocate array for service component descriptor lists
    if (mpe_memAllocP(MPE_MEM_SI, sizeof(mpe_SiMpeg2DescriptorList*)
            * numServiceComponents, (void**) &esDescriptorLists) != MPE_SUCCESS)
    {
        goto error;
    }

    // Allocate array of ES decriptor counts
    if (mpe_memAllocP(MPE_MEM_SI, sizeof(uint32_t) * numServiceComponents,
            (void**) &numESDescriptors) != MPE_SUCCESS)
    {
        goto error;
    }

    for (i = 0; i < numServiceComponents; ++i)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...serviceComponents[%d]: 0x%x\n", i,
                componentHandles[i]);

        totalSize += sizeof(uint32_t); // pid
        totalSize += 1; // stream_type
        totalSize += sizeof(uint32_t); // number of descriptors

        if (mpe_siGetDescriptorsForServiceComponentHandle(componentHandles[i],
                &numESDescriptors[i], &esDescriptorLists[i]) != MPE_SUCCESS)
        {
            goto error;
        }

        // Walk over elementary stream descriptors to calculate data size
        walker = esDescriptorLists[i];
        while (walker != NULL)
        {
            totalSize += 1; // Descriptor Tag
            totalSize += 1; // Descriptor Length
            totalSize += walker->descriptor_length; // Descriptor
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "createPMTByteArray()...componentDescriptor[%d] total descriptor length: %d\n",
                    i, walker->descriptor_length + 2);
            walker = walker->next;
        }
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...totalSize: %d\n", totalSize);

    // Create a byte array of size 'totalSize' and populate all the fields
    array = (*env)->NewByteArray(env, totalSize);

    /* Byte arrays are constructed in the following foramt
     *
     * PMT:
     *       uint32_t transportSreamHandle
     *       uint32_t serviceHandle;
     *       uint32_t frequency;
     *       uint32_t sourceId;
     *       uint32_t programNumber;
     *       uint32_t pcrPID;
     *       uint32_t numOuterDesc;
     *       for (int i=0; i<numOuterDesc; i++)
     *       {
     *           uint8_t tag
     *           uint8_t length
     *           uint8_t content[]
     *       }
     *       uint32_t numEStreams;
     *       for (int i=0; i<numEStreams; i++)
     *       {
     *           uint8_t streamType;
     *           uint32_t pid;
     *           uint32_t numDesc;
     *           for (int j=0; j<numDesc; j++)
     *           {
     *               uint8_t tag
     *               uint8_t length
     *               uint8_t content[]
     *           }
     *       }
     */

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...start populating pmt byte array...\n");

    // TRANSPORT STREAM HANDLE
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &tsHandle);
    currentPos += sizeof(uint32_t);

    // SERVICE HANDLE
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &serviceHandle);
    currentPos += sizeof(uint32_t);

    // FREQUENCY
    if (frequency == MPE_SI_OOB_FREQUENCY || frequency == MPE_SI_DSG_FREQUENCY)
    {
        int32_t oobFrequency = -1;
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                (jbyte*) &oobFrequency);
        currentPos += sizeof(int32_t);
    }
    else if (frequency == MPE_SI_HN_FREQUENCY)
    {
        int32_t oobFrequency = -2;
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                (jbyte*) &oobFrequency);
        currentPos += sizeof(int32_t);
    }
    else
    {
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &frequency);
        currentPos += sizeof(uint32_t);
    }

    // SOURCE ID
    // Check that the sourceID is valid. Set to -1 if invalid.
    if (sourceID == SOURCEID_UNKNOWN)
    {
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                (jbyte*) &undefinedSourceID);
        currentPos += sizeof(int32_t);
    }
    else
    {
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &sourceID);
        currentPos += sizeof(uint32_t);
    }

    // PROGRAM NUMBER
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &programNumber);
    currentPos += sizeof(uint32_t);

    // PCR PID
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &pcrPID);
    currentPos += sizeof(uint32_t);

    // NUM OUTER DESCRIPTORS
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &numOuterDescriptors);
    currentPos += sizeof(uint32_t);

    // OUTER DESCRIPTORS
    for (walker = outerDescList; walker != NULL; walker = walker->next)
    {
        uint8_t tag = (uint8_t) walker->tag;

        // TAG
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint8_t),
                (jbyte*) &tag);
        currentPos += sizeof(uint8_t);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...outerDescriptor tag = %d\n", tag);

        // CONTENT LENGTH
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint8_t),
                (jbyte*) &(walker->descriptor_length));
        currentPos += sizeof(uint8_t);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...outerDescriptor contentLength = %d\n",
                tag);

        // CONTENT
        (*env)->SetByteArrayRegion(env, array, currentPos,
                walker->descriptor_length,
                (jbyte*) (walker->descriptor_content));
        currentPos += walker->descriptor_length;
    }

    // NUMBER SERVICE COMPONENTS
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &numServiceComponents);
    currentPos += sizeof(uint32_t);

    // Set elementary stream DEBUG
    for (i = 0; i < numServiceComponents; ++i)
    {
        mpe_SiElemStreamType streamType;
        uint8_t streamType8;
        uint32_t pid;

        // STREAM TYPE
        if (mpe_siGetStreamTypeForServiceComponentHandle(componentHandles[i],
                &streamType) != MPE_SUCCESS)
        {
            goto error;
        }
        streamType8 = (uint8_t) streamType;
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint8_t),
                (jbyte*) &streamType8);
        currentPos += sizeof(uint8_t);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...streamType[%d]: 0x%x\n", i, streamType);

        // PID
        if (mpe_siGetPidForServiceComponentHandle(componentHandles[i], &pid)
                != MPE_SUCCESS)
        {
            goto error;
        }
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &pid);
        currentPos += sizeof(uint32_t);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...pid[%d]: 0x%x\n", i, pid);

        // NUM DESCRIPTORS
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &numESDescriptors[i]);
        currentPos += sizeof(uint32_t);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPMTByteArray()...numESDescriptors[%d]: 0x%x\n", i,
                numESDescriptors[i]);

        // DESCRIPTORS
        walker = esDescriptorLists[i];
        while (walker != NULL)
        {
            uint8_t tag = (uint8_t)(walker->tag);

            // TAG
            (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint8_t),
                    (jbyte*) &tag);
            currentPos += sizeof(uint8_t);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "createPMTByteArray()...esDescriptors[%d]: tag = %d\n", i,
                    tag);

            // CONTENT LENGTH
            (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint8_t),
                    (jbyte*) &(walker->descriptor_length));
            currentPos += sizeof(uint8_t);
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_JNI,
                    "createPMTByteArray()...esDescriptors[%d]: content length = %d\n",
                    i, walker->descriptor_length);

            // CONTENT
            (*env)->SetByteArrayRegion(env, array, currentPos,
                    walker->descriptor_length,
                    (jbyte*) (walker->descriptor_content));
            currentPos += walker->descriptor_length;

            walker = walker->next;
        }
        // release the service component handles
        (void) mpe_siReleaseServiceComponentHandle(componentHandles[i]);
    }

    if (numESDescriptors != NULL)
        mpe_memFreeP(MPE_MEM_SI, numESDescriptors);
    if (esDescriptorLists != NULL)
        mpe_memFreeP(MPE_MEM_SI, esDescriptorLists);
    if (componentHandles != NULL)
        mpe_memFreeP(MPE_MEM_SI, componentHandles);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPMTByteArray()...pmtArray created..\n");

    mpe_siUnLock();
    return array;

    error: if (numESDescriptors != NULL)
        mpe_memFreeP(MPE_MEM_SI, numESDescriptors);
    if (esDescriptorLists != NULL)
        mpe_memFreeP(MPE_MEM_SI, esDescriptorLists);
    if (componentHandles != NULL)
        mpe_memFreeP(MPE_MEM_SI, componentHandles);
    mpe_siUnLock();
    return NULL;
}

jbyteArray createPATByteArray(JNIEnv *env, mpe_SiTransportStreamHandle tsHandle)
{
    uint32_t frequency;
    uint32_t tsId;
    uint32_t numPrograms = 0;
    mpe_SiTransportStreamEntry* tsEntry =
            (mpe_SiTransportStreamEntry*) tsHandle;
    mpe_SiPatProgramList* walker = NULL;
    uint32_t totalSize = 0;
    uint32_t currentPos = 0;
    jbyteArray array = NULL;
	mpe_Error result = MPE_SUCCESS;

    if (checkReturnCode(env, mpe_siLockForRead(), FALSE, FALSE, FALSE)
            != MPE_SUCCESS)
        goto error;

    // TRANSPORT STREAM HANDLE
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPATByteArray()...transport stream handle = 0x%x\n", tsHandle);
    totalSize += sizeof(uint32_t);

    // TsID
    if (mpe_siGetTransportStreamIdForTransportStreamHandle(tsHandle, &tsId)
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "createPATByteArray()...Error getting tsId for transport stream handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPATByteArray()...tsId = %d\n", tsId);
    totalSize += sizeof(uint32_t);

    // FREQUENCY
    if (mpe_siGetFrequencyForTransportStreamHandle(tsHandle, &frequency)
            != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "createPATByteArray()...Error getting frequency for transport stream handle\n");
        goto error;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPATByteArray()...frequency = %d\n", frequency);
    totalSize += sizeof(uint32_t);

    // NUM PROGRAMS
    walker = tsEntry->pat_program_list;
    while (walker != NULL)
    {
        numPrograms++;
        walker = walker->next;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPATByteArray()...numPrograms = %d\n", numPrograms);
    totalSize += sizeof(uint32_t);

    // Calculate remaining size (all known values).  Each program is 4-byte PID and
    // 4-byte program number and 4-byte sourceID.
    totalSize += numPrograms * (4 + 4 + 4);

    // Create a byte array of size 'totalSize' and populate all the fields
    array = (*env)->NewByteArray(env, totalSize);

    /* Create a java byte array with pat programs, pmt pids...
     *
     * PAT:
     *       uint32_t transportStreamHandle;
     *       uint32_t transportStreamId;
     *       uint32_t frequency;
     *       uint32_t numPrograms;
     *       for(int i=0;i<numPrograms;i++)
     *       {
     *           uint32_t PMT_PID;
     *           uint32_t programNumber;
     *           int32_t  sourceID;
     *       }
     *
     */

    // TRANSPORT STREAM HANDLE
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &tsHandle);
    currentPos += sizeof(uint32_t);

    // TRANSPORT STREAM ID
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &tsId);
    currentPos += sizeof(uint32_t);

    // FREQUENCY
    if (frequency == MPE_SI_OOB_FREQUENCY || frequency == MPE_SI_DSG_FREQUENCY)
    {
        int32_t oobFrequency = -1;
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                (jbyte*) &oobFrequency);
        currentPos += sizeof(int32_t);
    }
    else if (frequency == MPE_SI_HN_FREQUENCY)
    {
        int32_t oobFrequency = -2;
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                (jbyte*) &oobFrequency);
        currentPos += sizeof(int32_t);
    }
    else
    {
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &frequency);
        currentPos += sizeof(uint32_t);
    }

    // NUM PROGRAMS
    (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
            (jbyte*) &numPrograms);
    currentPos += sizeof(uint32_t);

    walker = tsEntry->pat_program_list;
    while (walker != NULL)
    {
        uint32_t sourceID;
        int32_t undefinedSourceID = -1;
        mpe_SiServiceHandle serviceHandle;

        // PROGRAM NUMBER
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPATByteArray()...programNumber = 0x%x\n",
                walker->programNumber);
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &walker->programNumber);
        currentPos += sizeof(uint32_t);

        // PMT PID
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "createPATByteArray()...pmtPID = 0x%x\n", walker->pmt_pid);
        (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(uint32_t),
                (jbyte*) &walker->pmt_pid);
        currentPos += sizeof(uint32_t);

		result = mpe_siGetServiceHandleByFrequencyModulationProgramNumber(frequency,
                tsEntry->modulation_mode, walker->programNumber, &serviceHandle);
				
		if (result == MPE_SI_NOT_FOUND || result == MPE_SI_NOT_AVAILABLE || result
            == MPE_SI_NOT_AVAILABLE_YET)
        {
			result = mpe_siCreateDynamicServiceHandle(frequency, walker->programNumber,
					tsEntry->modulation_mode, &serviceHandle);
        }
	
		if (result != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_JNI,
                    "createPATByteArray()...Error getting serviceHandle for frequency/programNumber frequency: %d programNumber: %d\n",
                    frequency, walker->programNumber);
            goto error;
        }
		
		// SourceID
        if (mpe_siGetSourceIdForServiceHandle(serviceHandle, &sourceID)
                != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "createPATByteArray()...Error getting sourceID for service handle\n");
            goto error;
        }

        // Check that the sourceID is valid. Set to -1 if invalid.
        if (sourceID == SOURCEID_UNKNOWN)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "createPATByteArray()...sourceID = UnknownSourceID\n");

            (*env)->SetByteArrayRegion(env, array, currentPos, sizeof(int32_t),
                    (jbyte*) &undefinedSourceID);
            currentPos += sizeof(int32_t);
        }
        else
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "createPATByteArray()...sourceID = 0x%x\n", sourceID);

            (*env)->SetByteArrayRegion(env, array, currentPos,
                    sizeof(uint32_t), (jbyte*) &sourceID);
            currentPos += sizeof(uint32_t);
        }

        walker = walker->next;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "createPATByteArray()...PAT Array created\n");

    mpe_siUnLock();
    return array;

    error: mpe_siUnLock();
    return NULL;
}
