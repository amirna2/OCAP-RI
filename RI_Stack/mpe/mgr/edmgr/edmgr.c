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

#include <osmgr.h>
#include <sysmgr.h>
#include <edmgr.h>
#include <jvmmgr.h>
#include <mpe_sys.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_event.h>
#include <mpe_dbg.h>

#if MPE_FEATURE_DEBUG
/* ED handle active sync code. */
#define MPE_ED_SYNCCODE 0xFA57F00D
#endif /* MPE_FEATURE_DEBUG */

/* polling period for java thread interruptions */
#define ED_POLL_PERIOD_MS   (1000 * 10)

/* Cast for SendEvent function */
#define SEND_EVENT_FUNCPTR void (*)(mpe_EdHandle,int)

/* array of event dispatch queues */
/* TODO: How do we communicated queue size from Java layer (or should we) */
static mpe_EventQueue eventQs[MPE_ED_QUEUE_MAX];
static mpe_Bool eventQsInUse[MPE_ED_QUEUE_MAX];

/* Function prototypes. */
void mpe_edmgrInit(void);
mpe_Error mpe_edmgrCreateHandle(void *listenerObj, int eventQId,
        mpe_EdNativeCallback nativeCallback, uint32_t terminationType,
        uint32_t terminationCode, mpe_EdEventInfo **edHandle);
jobject mpe_edmgrProcessNextEvent(JNIEnv *jEnv, jint jQType,
        uint32_t* eventCode, uint32_t* eventData1, uint32_t* eventData2);
void mpe_edmgrCreateEventQueue(int jQType);
void mpe_edmgrDeleteEventQueue(int jQType);
mpe_Error mpe_edmgrDeleteHandle(mpe_EdEventInfo *handle);

/* Event Dispatch manager function table. */
mpe_ed_ftable_t ed_ftable =
{ mpe_edmgrInit, mpe_edmgrCreateHandle, mpe_edmgrDeleteHandle,
        mpe_edmgrProcessNextEvent, mpe_edmgrCreateEventQueue,
        mpe_edmgrDeleteEventQueue };

/**
 * ED manager setup function called during MPE initialization.
 */
void mpe_edSetup(void)
{
    /* Install the function table. */
    mpe_sys_install_ftable(&ed_ftable, MPE_MGR_TYPE_ED);
}

/**
 * ED manager initialization function called during MPE intialization.
 */
void mpe_edmgrInit(void)
{
    static mpe_Bool inited = false;

    if (!inited)
    {
        int i;

        for (i = 0; i < MPE_ED_QUEUE_MAX; i++)
            eventQsInUse[i] = FALSE;

        inited = true;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_ED,
                "<mpe_edInit> ED Initialization Complete\n");
    }
}

/**
 * mpe_Error mpe_edCreateHandle(void *listenerObj, int eventQId,
 *                              mpe_EdNativeCallback nativeCallback, uint32_t terminationType,
 *                              uint32_t terminationCode, mpe_EdEventInfo **edHandle)
 *
 * Allocated and initializes an ED information structure for use with aync
 * callbacks to Java. This will internally cause the creation of a global
 * reference to the passed in listenerObj.
 *
 * @param listenerObj  the corresponding Java listener object
 *                     This object should implement the EdListener interface
 *
 * @param eventQId   Specifies the ED Q thread to be utilized for async notification
 *                  (set to zero for now)
 *
 * @param nativeCallback A native method callback which is called by the event dispatch
 *                thread immediately prior to issueing the Java callback
 *                This may be NULL (optional)
 *
 * @param terminationType Indicates the release policy for this ed handle
 *                MPE_ED_TERMINATION_ONESHOT - release after first notification
 *                MPE_ED_TERMINATION_EVCODE  - release after an event matching terminationCode
 *                                             is detected
 *                MPE_ED_TERMINATION_OPEN    - EdMgr will not release this handle
 *
 * @param terminationCode For termination type EVCODE, this code will be compared to the eventID field
 *                of each event
 *
 * @param edHandle       Returned ed info structure (should be passed into ACT field of async API)
 *
 * @return MPE_SUCCESS if the handle was successfully created.
 */
mpe_Error mpe_edmgrCreateHandle(void *listenerObj, int eventQId,
        mpe_EdNativeCallback nativeCallback, uint32_t terminationType,
        uint32_t terminationCode, mpe_EdEventInfo **edHandle)
{
    int detach = FALSE;
    JNIEnv *env = NULL;
    JavaVM *jvm = mpe_jvmGetJVM();
    mpe_EdEventInfo *edInfo = NULL;

    /* TODO: change to extensible array w/ mutexes instead of malloc/free of info struct */

    /* parameter checking */
    if ((eventQId < 0) || (eventQId >= MPE_ED_QUEUE_MAX))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED,
                "<edmgrCreateHandle> invalid eventQId '%d'\n", eventQId);
        return MPE_EINVAL;
    }
    if (!eventQsInUse[eventQId])
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_ED,
                "<edmgrCreateHandle> invalid eventQId '%d', queue not initialized!\n",
                eventQId);
        return MPE_EINVAL;
    }

    /* Check if we are attached (but only if the user intended to register a java callback */
    if (listenerObj != NULL)
    {
        /* Get a JNI environment parameter for this thread. */
        if (((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2) != JNI_OK)
                || (env == NULL))
        {
            /* Attach the current thread so that the global object references can be made. */
            if (((*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL)
                    != JNI_OK) || (env == NULL))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED,
                        "<edmgrCreateHandle> Could not attach to JVM\n");
                return MPE_EINVAL;
            }
            detach = TRUE;
        }
    }

    /* allocate some memory for this handle */
    if (mpeos_memAllocP(MPE_MEM_EVENT, sizeof(mpe_EdEventInfo),
            (void**) &edInfo) == MPE_SUCCESS)
    {
        /* initialize this handle & create a new Java reference to it *
         * (so that it doesn't go away from underneath us!)           */
        edInfo->jListenerObject = NULL;
        if ((listenerObj != NULL) && (env != NULL) && (*env != NULL))
        {
            edInfo->jListenerObject = (void*) (*env)->NewGlobalRef(env,
                    listenerObj);
        }
        edInfo->terminationCode = terminationCode;
        edInfo->terminationType = terminationType;
        edInfo->nativeCallback = nativeCallback;
        edInfo->eventQ = eventQs[eventQId];
#if MPE_FEATURE_DEBUG
        edInfo->activeSync = MPE_ED_SYNCCODE; /* Mark handle as active and valid. */
#endif /* MPE_FEATURE_DEBUG */
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_ED,
                "<edmgrCreateHandle> 0x%p, terminationType: %x, original listenerObj: 0x%p, global ref listenerObj[0x%p]\n",
                edInfo, edInfo->terminationType, listenerObj,
                edInfo->jListenerObject);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED,
                "<edmgrCreateHandle> Could not allocate memory for ED handle\n");
        return MPE_ENOMEM;
    }

    /* detach from vm if necessary */
    if (detach == TRUE)
    {
        (void) (*jvm)->DetachCurrentThread(jvm);
    }

    /* Return the ED handle. */
    *edHandle = edInfo;
    return MPE_SUCCESS;
}

/**
 * Create a new ED event queue for processing ED events.
 *
 * @param qType is the queue specifier of the queue to create (0 to N).
 */
void mpe_edmgrCreateEventQueue(int qType)
{
    mpe_Error err;
    char qName[16];

    /* Validate queue identifier. */
    if ((qType < 0) || (qType >= MPE_ED_QUEUE_MAX) || eventQsInUse[qType])
        return;

    /* Create unique queue name */
    snprintf(qName, 16, "MpeEdmgr-%d", qType);

    /* create the native dispatch event queue */
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_ED,
            "<edmgrCreateEventQueue> allocating a new queue #%d with name %s\n", qType, qName);

    if ((err = mpe_eventQueueNew(&eventQs[qType], qName)) != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_ED,
                "<edmgrCreateEventQueue> could not allocate a new queue #%d, error = %d\n",
                qType, err);
    }
    eventQsInUse[qType] = TRUE;
}

/**
 * Delete the specified ED event queue.
 *
 * @param qType is the queue identifier of the queue to delete.
 */
void mpe_edmgrDeleteEventQueue(int qType)
{
    /* Validate queue identifier. */
    if ((qType < 0) || (qType >= MPE_ED_QUEUE_MAX))
        return;
    if (!eventQsInUse[qType])
        return;

    /* cleanup: remove queue */
    mpe_eventQueueDelete(eventQs[qType]);
    eventQsInUse[qType] = FALSE;
}

/**
 * Process the next ED event for the specified ED event queue.
 *
 * @param jEnv is the ED java thread's JNI environment
 * @param jQType is the ED queue identifier
 * @param eventCode is a pointer for returning the event code
 * @param eventData1 is a pointer for returning the first optional event data value
 * @param eventData2 is a pointer for returning the second optional event data value
 *
 * @return the new local reference for the target EDListener object.
 */
jobject mpe_edmgrProcessNextEvent(JNIEnv *jEnv, jint jQType,
        uint32_t* eventCode, uint32_t* eventData1, uint32_t* eventData2)
{
    mpe_Error err;
    mpe_Event edEventId;
    void *edEventData1, *edEventData2;
    uint32_t edEventData3;
    jobject object = NULL;
    uint32_t terminationType;
    uint32_t terminationCode;

    /* Validate queue identifier. */
    if ((jQType < 0) || (jQType >= MPE_ED_QUEUE_MAX) || !eventQsInUse[jQType])
        return NULL;

    /* process any async event responses */
    err = mpeos_eventQueueWaitNext(eventQs[(int) jQType], &edEventId,
            &edEventData1, &edEventData2, &edEventData3, ED_POLL_PERIOD_MS);

    if (err == MPE_SUCCESS)
    {
        mpe_EdEventInfo* edHandle = (mpe_EdEventInfo*) edEventData2;

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_ED,
                "<edmgrProcessNextEvent> Got event %d %p\n", edEventId,
                edHandle);

        if (edHandle != NULL)
        {

#if MPE_FEATURE_DEBUG
            /* Check for invalid ED handle case. */
            if (edHandle->activeSync != MPE_ED_SYNCCODE)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED, "<edmgrProcessNextEvent> error - inactive handle in use %p\n", edHandle);
                return NULL;
            }
#endif /* MPE_FEATURE_DEBUG */

            /* Populate our return values.  We create a new LocalReference to the EDListener
             object.  If the edHandle is terminated below, the GlobalReference stored in the
             edHandle is deleted.  By creating a LocalReference, we ensure that the reference
             will survive until it is returned to the Java layer */
            if (edHandle->jListenerObject != NULL)
            {
                object = (*jEnv)->NewLocalRef(jEnv, (jobject)(
                        edHandle->jListenerObject));
            }

            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_ED,
                    "<edmgrProcessNextEvent> ed event fired for listenerObj[0x%p], eventId[0x%x]\n",
                    edHandle->jListenerObject, edEventId);

            /* Save important ED handle values prior to calling native callback. */
            terminationType = edHandle->terminationType;
            terminationCode = edHandle->terminationCode;

            /* If this ed handle has a registered native callback, call it now */
            if (edHandle->nativeCallback != NULL)
            {
                (*edHandle->nativeCallback)(jEnv, edHandle->jListenerObject,
                        edHandle, (uint32_t*) &edEventId, &edEventData1,
                        &edEventData2, &edEventData3);
            }

            /* Return values after native callback in case native callback modified values. */
            *eventCode = (uint32_t) edEventId;
            *eventData1 = (uint32_t) edEventData1;
            *eventData2 = (uint32_t) edEventData3; /* 3rd value returned because edEventData2 is normally the edhandle */

            /* free up reference to java object if this necessary                 *
             * (ie, if it's a one-shot event or the termination code is detected) */
            if ((terminationType == MPE_ED_TERMINATION_ONESHOT)
                    || ((terminationType == MPE_ED_TERMINATION_EVCODE)
                            && (terminationCode == edEventId)))
            {
                MPE_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_ED,
                        "<edmgrProcessNextEvent> Ed Handle termination set to true - handle=0x%p terminationType=0x%x\n",
                        edHandle, edHandle->terminationType);
                mpe_edDeleteHandle(edHandle);
            }
        }
    }

    return object;
}

/**
 * Delete the specified ED handle.
 *
 * @param edHandle is the ED handle to delete.
 *
 * @return MPE_SUCCESS if successfully deleted.
 */
mpe_Error mpe_edmgrDeleteHandle(mpe_EdEventInfo *edHandle)
{
    JavaVM *jvm = mpe_jvmGetJVM();
    JNIEnv *env = NULL;
    int detach = false;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_ED, "<edmgrDeleteHandle> enter %p\n",
            edHandle);

    /* insure valid parameters */
    if (edHandle != NULL)
    {
#if MPE_FEATURE_DEBUG
        if ( edHandle->activeSync != MPE_ED_SYNCCODE )
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED, "<edmgrDeleteHandle> error - deletetion of inactive handle %p\n", edHandle);
            return MPE_EINVAL;
        }
#endif /* MPE_FEATURE_DEBUG */

        /* remove the reference to the java source object, if necessary */
        if (edHandle->jListenerObject != NULL)
        {
            /* Check if we are attached */
            if (((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2) != JNI_OK)
                    || (env == NULL))
            {
                // Not attached, so attempt to attach
                if (((*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL)
                        != JNI_OK) || (env == NULL))
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_ED,
                            "<edmgrDeleteHandle> Could not attach to JVM\n");

                    // Still free the edHandle memory.  At least this way, we only leak the
                    // Java object and not both.
                    (void) mpeos_memFreeP(MPE_MEM_EVENT, edHandle);

                    return MPE_EINVAL;
                }
                detach = true;
            }
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_ED,
                    "<edmgrDeleteHandle> removing global reference to listenerObj[0x%p]\n",
                    edHandle->jListenerObject);

            (*env)->DeleteGlobalRef(env, edHandle->jListenerObject);
        }

        /* detach from vm if necessary */
        if (detach)
        {
            (void) (*jvm)->DetachCurrentThread(jvm);
        }

        /* free up the memory for this handle */
#if MPE_FEATURE_DEBUG
        edHandle->activeSync = 0;
#endif /* MPE_FEATURE_DEBUG */

        (void) mpeos_memFreeP(MPE_MEM_EVENT, edHandle);
    }

    return MPE_SUCCESS;
}
