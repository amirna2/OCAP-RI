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

#ifndef _MPE_ED_H_
#define _MPE_ED_H_

#include <mpe_types.h>
#include <mpe_os.h>
#include <mpeos_event.h>
#include "jni.h"

/* ED native callback function definition */
typedef struct _mpe_EdEventInfo mpe_EdEventInfo;

/*
 * The native event dispatch callback function is called prior to the return of the event
 * to the java event dispatch system.  The callback function receives the event parameters
 * as pointers giving it the option of modifying the values prior to the return to java.
 */
typedef void(*mpe_EdNativeCallback)(JNIEnv *, void* jListenerObject,
        mpe_EdEventInfo *, uint32_t *, void **, void **, uint32_t*);

#define MPE_ED_TERMINATION_ONESHOT   1        /* EDMgr will delete handle after first event */
#define MPE_ED_TERMINATION_EVCODE    2        /* EDMgr will delete handle after receiving
                                                 event code matching terminationCode */
#define MPE_ED_TERMINATION_OPEN      3        /* EDMgr will not delete the handle */

/*
 * ED event queue identifier used to specify which event queue and thread to utilize
 * for delivery of ED events. Specified when an ED handle is created.  These values
 * are coordinated with the java ED manager do not modify.
 */
#define MPE_ED_QUEUE_NORMAL   0
#define MPE_ED_QUEUE_SPECIAL1 1
#define MPE_ED_QUEUE_TUNE_EVENTS 2
#define MPE_ED_QUEUE_MAX	  3

struct _mpe_EdEventInfo
{
    mpe_EventQueue eventQ; /* queue to place upgoing (to Java) events */
    void *jListenerObject; /* Source Java listener object */
    uint32_t terminationType; /* Specifies how this asynchronous registration 
     will be ended */
    uint32_t terminationCode; /* Event code to indicate that the asynchronous
     session is closed */
    mpe_EdNativeCallback nativeCallback; /* native method to be called when asynchronous 
     notification is received */
    uint32_t activeSync; /* Sync-code indicates active handle. */
};

/* event dispatch handle */
typedef struct _mpe_EdEventInfo *mpe_EdHandle;

#ifdef __cplusplus
extern "C"
{
#endif

#include "../mgr/include/mgrdef.h"
#include "../mgr/include/edmgr.h"

#define mpe_ed_mgr_ftable ((mpe_ed_ftable_t*)(FTABLE[MPE_MGR_TYPE_ED]))

#define mpe_edInit (mpe_ed_mgr_ftable->mpe_ed_init_ptr)

/**
 * mpe_Error mpe_edCreateHandle(void *listenerObj, int eventQId, 
 *                              mpe_EdNativeCallback nativeCallback, uint32_t terminationType, 
 *                              uint32_t terminationCode, mpe_EdEventInfo **edHandle)
 *
 * Allocated and initializes an ED information structure for use with aync
 * callbacks to Java. This will internally cause the creation of a global
 * reference to the passed in listenerObj.
 *
 * listenerObj    the corresponding Java listener object
 *                This object should implement the EdListener interface
 *
 * eventQId       Specifies the ED Q thread to be utilized for async notification
 *                (set to zero for now)
 *
 * nativeCallback A native method callback which is called by the event dispatch
 *                thread immediately prior to issueing the Java callback
 *                This may be NULL (optional)
 *
 * terminationType Indicates the release policy for this ed handle
 *	              MPE_ED_TERMINATION_ONESHOT - release after first notification
 *                MPE_ED_TERMINATION_EVCODE  - release after an event matching terminationCode
 *                                             is detected
 *                MPE_ED_TERMINATION_OPEN    - EdMgr will not release this handle
 *
 * terminationCode For termination type EVCODE, this code will be compared to the eventID field
 *                of each event
 *
 * edHandle       Returned ed info structure (should be passed into ACT field of async API)
 */
#define mpe_edCreateHandle  (mpe_ed_mgr_ftable->mpe_edCreateHandle_ptr)

/**
 *   mpe_Error mpe_edDeleteHandle(mpe_EdEventInfo *edHandle)
 *
 *   Deletes (deallocates) this ed info structure. This will cause the
 *   global reference to the listener object associated with the ED 
 *   structure to be deleted.
 *
 *  edHandle Ed structure to be deleted
 */
#define mpe_edDeleteHandle  (mpe_ed_mgr_ftable->mpe_edDeleteHandle_ptr)

/**
 * Internal ED function.
 */
#define mpe_edProcessNextEvent (mpe_ed_mgr_ftable->mpe_edProcessNextEvent_ptr)
#define mpe_edCreateEventQueue (mpe_ed_mgr_ftable->mpe_edCreateEventQueue_ptr)
#define mpe_edDeleteEventQueue (mpe_ed_mgr_ftable->mpe_edDeleteEventQueue_ptr)

#ifdef __cplusplus
}
;
#endif

#endif /* _MPE_ED_H_ */
