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

#include <org_cablelabs_debug_status_MpeStatusProducer.h>
#include <mpe_types.h>
#include <mpe_os.h>
#include <mpe_dbg.h>
#include <mpe_ed.h>

/* ED event handle. */
static mpe_EdHandle edHandle = NULL;

/**
 * Acquire the string representation of a specific SIDB SI entry.
 *
 * @param env is the JNI environment pointer.
 * @param index is the index within the SIDB si entries of the target entry.
 *
 * @return jstring reference to the generated string.
 */
static jstring getSiEntry(JNIEnv *env, jint index)
{
    char entry[512];
    uint32_t size = 512;

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "MpeStatusProducer::getSiEntry - acquiring SI entry for indexed entry %d\n",
            (int)index);

    /* Acquire the target entry, which is a null terminate "char *" value. */
    if (mpe_dbgStatusGet(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRY, &size, entry,
            (void*) &index) != MPE_SUCCESS)
        return NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "MpeStatusProducer::getSiEntry - size = %d, entry = %s\n", size,
            entry);

    /* Convert to a java string. */
    return (*env)->NewStringUTF(env, entry);
}

/**
 * Aquire the complete set of SIDB entries.
 *
 * @param env is the JNI environment pointer.
 * @param index is the index within the SIDB si entries of the target entry.
 *
 * @return jobjectArray of strings for each SI entry.
 */
static jobjectArray getSiEntries(JNIEnv *env)
{
    uint32_t i;
    uint32_t count = 0;
    uint32_t size = sizeof(uint32_t);
    jobjectArray siEntries;
    jclass stringArrayClass = (*env)->FindClass(env, "java/lang/String");

    if (stringArrayClass == NULL)
        return NULL;

    /* First get the number of table entries. */
    if (mpe_dbgStatusGet(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRY_COUNT, &size,
            &count, NULL) != MPE_SUCCESS)
        return NULL;

    /* Now allocate an object array for the table entries. */
    if ((siEntries = (*env)->NewObjectArray(env, count, stringArrayClass, NULL))
            == NULL)
        return NULL;

    for (i = 0; i < count; i++)
    {
        jstring js;

        if ((js = getSiEntry(env, i)) != NULL)
        {
            (*env)->SetObjectArrayElement(env, siEntries, i, js);
            (*env)->DeleteLocalRef(env, js);
        }
        else
        {
            (*env)->DeleteLocalRef(env, js);
            (*env)->DeleteLocalRef(env, siEntries);
            return NULL;
        }
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "MpeStatusProducer::sidbSiEntries - returning String[%d] containing SI entries.\n",
            count);

    return siEntries;
}

/**
 * This ED native callback function can be used to convert native status components into
 * java objects.  The event identifier is the status type identifier, which can be used
 * to decide what form of conversion needs to be performed if any.  This callback allows
 * for potentially complex operations to be done such as instantiating a new object and
 * populating that object with data values.
 *
 * @param env is the JNI environment pointer
 * @param jListenerObj is the EDListener object
 * @param ed is the EDHandle
 * @param evId is the event identifier (status type identifier)
 * @param evData1 is a pointer to a location containing the status value reference
 * @param evData2 is the EDHandle used as an ACT
 * @param evData3 is a pointer to the format of the status
 */
static void convertStatus(JNIEnv *env, void* jListenerObj, mpe_EdEventInfo *ed,
        uint32_t *evId, void **evData1, void **evData2, uint32_t *evData3)
{
    /* Event identifier is status type identifier. */
    switch (*evId & MPE_DBG_STATUS_TYPEID_MASK)
    {
    case MPE_DBG_STATUS_OC_MOUNT_EVENT:
    case MPE_DBG_STATUS_OC_UNMOUNT_EVENT:
        /*
         * Convert native string status to java String object and create a global reference.
         */
        *evData1 = (*env)->NewGlobalRef(env,
                (*env)->NewStringUTF(env, *evData1));
        break;
    default:
        break;
    }
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    getMPEStatus
 * Signature: (III)Ljava/lang/Object;
 *
 * All calls to StatusProducer.getStatus() whose "param" parameter is either "null" or an
 * instance of an Integer will channeled through this JNI function.
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_getMPEStatus__III(
        JNIEnv *env, jobject jobj, jint typeId, jint format, jint param)
{
    switch (typeId & MPE_DBG_STATUS_TYPEID_MASK)
    {
    case MPE_DBG_STATUS_SI_ENTRY_COUNT: /* Acquire the total number of SI entries. */
    {
        uint32_t size = sizeof(uint32_t);
        uint32_t count = 0;
        jclass intClass;
        jmethodID intMethod;

        /* Acquire the count. */
        if (mpe_dbgStatusGet(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRY_COUNT, &size,
                &count, NULL) != MPE_SUCCESS)
            return NULL;

        /* Get the Integer class for returning the count. */
        if ((intClass = (*env)->FindClass(env, "java/lang/Integer")) == NULL)
            return NULL;

        /* Get the method Id for the Integer constructor. */
        if ((intMethod = (*env)->GetMethodID(env, intClass, "<init>", "(I)V"))
                == NULL)
            return NULL;

        /* Instantiate an Integer with the count and return it. */
        return (*env)->NewObject(env, intClass, intMethod, (int) count);
    }
    case MPE_DBG_STATUS_SI_ENTRY:
        return getSiEntry(env, param); /* Return the specific entry. */
    case MPE_DBG_STATUS_SI_ENTRIES:
        return getSiEntries(env);
    case MPE_DBG_STATUS_OC_MOUNT_EVENT:
        return NULL;
        /*
         * Add new status calls here for calls that have either not 3rd parameter or an integer  3rd parameter.
         */
    default:
        return NULL;
    }
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    getMPEStatus
 * Signature: (IILjava/lang/Object;)Ljava/lang/Object;
 *
 * This JNI method differs from the one above in that it receives the optional "param" value as
 * an object reference.  This allows any status information APIs that require use of additional
 * input or output parameters to be supported.  The "param" object can provide additional input
 * parameters to the request and/or provide an additional object for return of output values.
 * All calls to StatusProducer.getStatus() whose "param" value is not "null" and not an Integer
 * will be channeled through this JNI function.
 *
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_getMPEStatus__IILjava_lang_Object_2(
        JNIEnv *env, jobject jobj, jint typeId, jint format, jobject param)
{
    switch (typeId & MPE_DBG_STATUS_TYPEID_MASK)
    {
    /*
     * Add new status calls here for calls that have either not 3rd parameter or an integer  3rd parameter.
     */
    default:
        return NULL;
    }
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    registerStatusEvent
 * Signature: (IILjava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_registerStatusEvent(
        JNIEnv *env, jobject jobj, jint typeId, jint format, jobject param)
{
    return mpe_dbgStatusRegisterInterest(typeId, format, (void*) param);
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    unregisterStatusEvent
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_unregisterStatusEvent(JNIEnv *env, jobject jobj, jint typeId)
{
    (void)mpe_dbgStatusUnregisterInterest(typeId);
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    getObject
 * Signature: (III)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
        Java_org_cablelabs_debug_status_MpeStatusProducer_getObject(JNIEnv *,
                jobject, jint, jint, jint);
/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    getObject
 * Signature: (II)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_getObject(
        JNIEnv *env, jobject jobj, jint typeId, jint format, jint iobj)
{
    jobject jiobj;

    switch (typeId & MPE_DBG_STATUS_TYPEID_MASK)
    {
    case MPE_DBG_STATUS_OC_MOUNT_EVENT:
    case MPE_DBG_STATUS_OC_UNMOUNT_EVENT:
        /*
         * Get a new local reference for the object.
         */
        jiobj = (*env)->NewLocalRef(env, (jobject) iobj);

        /*
         * Delete the global reference created in the ED callback function 'convertStatus'.
         */
        (*env)->DeleteGlobalRef(env, (jobject) iobj);

        break;
    default:
        jiobj = (jobject) iobj;
        break;
    }

    return jiobj;
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    initMPEStatusEvents
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_initMPEStatusEvents(JNIEnv *env, jobject jobj)
{
    mpe_Error ec;

    if ( edHandle == NULL )
    {
        /* Create an ED handle for returning native status events to the java listeners. */
        if ( (ec = mpe_edCreateHandle((void*)jobj, MPE_ED_QUEUE_NORMAL, convertStatus, MPE_ED_TERMINATION_OPEN, 0, &edHandle)) == MPE_SUCCESS)
        /* Register handler with native debug status interface. */
        (void)mpe_dbgStatusRegister(edHandle->eventQ, (void*)edHandle);
        else
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "registerStatusEvents - mpe_edCreateHandle() returned error = %d\n", ec);
    }
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    termMPEStatusEvents
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_termMPEStatusEvents(JNIEnv *env, jobject jobj)
{
    /* Delete ED handle. */
    mpe_edDeleteHandle(edHandle);
    edHandle = NULL;
}

/*
 * Class:     org_cablelabs_debug_status_MpeStatusProducer
 * Method:    getMPEStatusTypes
 * Signature: ()[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_debug_status_MpeStatusProducer_getMPEStatusTypes(
        JNIEnv *env, jobject jobj)
{
    mpe_DbgStatusType *mpetypes = NULL;
    int i, cnt;
    jobjectArray strings;
    jobjectArray types;
    jintArray ints;
    jstring js;
    jclass stringArrayClass = (*env)->FindClass(env, "java/lang/String");
    jclass objectArrayClass = (*env)->FindClass(env, "java/lang/Object");

    /* Make sure class finds worked. */
    if (stringArrayClass == NULL || objectArrayClass == NULL)
        return NULL;

    /* Get the native types supported. */
    if (mpe_dbgStatusGetTypes(&mpetypes) != MPE_SUCCESS)
        return NULL;

    /* Count the number of valid entries. */
    for (i = cnt = 0; i < ENUM_DBG_STATUS_TOTAL; ++i)
    {
        if (mpetypes[i].stringId != NULL)
            ++cnt;
    }

    /* Alllocate Object array for returning string and integer arrays. */
    if ((types = (*env)->NewObjectArray(env, 2, objectArrayClass, NULL))
            == NULL)
        return NULL;

    /* Allocate String and integer arrays for delivery of types. */
    if ((strings = (*env)->NewObjectArray(env, cnt, stringArrayClass, NULL))
            == NULL)
    {
        (*env)->DeleteLocalRef(env, types);
        return NULL;
    }

    /* Allocate the integer array for the type identifiers. */
    if ((ints = (*env)->NewIntArray(env, cnt)) == NULL)
    {
        (*env)->DeleteLocalRef(env, types);
        (*env)->DeleteLocalRef(env, strings);
        return NULL;
    }

    // Now populate the string and integer arrays.
    for (i = 0; i < ENUM_DBG_STATUS_TOTAL; ++i)
    {
        if (mpetypes[i].stringId != NULL)
        {
            jint typeId = mpetypes[i].typeId;

            (*env)->SetIntArrayRegion(env, ints, i, 1, &typeId);
            if ((js = (*env)->NewStringUTF(env, mpetypes[i].stringId)) != NULL)
            {
                (*env)->SetObjectArrayElement(env, strings, i, js);
                (*env)->DeleteLocalRef(env, js);
            }
            else
            {
                (*env)->DeleteLocalRef(env, js);
                (*env)->DeleteLocalRef(env, strings);
                (*env)->DeleteLocalRef(env, types);
                (*env)->DeleteLocalRef(env, ints);
                return NULL;
            }
        }
    }

    /* Now populate the object array with the string and integer arrays. */
    (*env)->SetObjectArrayElement(env, types, 0, strings);
    (*env)->SetObjectArrayElement(env, types, 1, ints);

    /* Delete local references to string and integer arrays. */
    (*env)->DeleteLocalRef(env, strings);
    (*env)->DeleteLocalRef(env, ints);

    /* Return object array containing string and integer arrays. */
    return types;
}
