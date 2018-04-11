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

#include <org_cablelabs_impl_manager_ed_EventDispatchImpl.h>
#include "jni_util.h"
#include <mpe_ed.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>
#include <mpe_dbg.h>

/*
 * Class:     org_cablelabs_impl_manager_ed_EventDispatchImpl
 * Method:    processNextEvent
 * Signature: (I[Ljava/lang/Object;[I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_manager_ed_EventDispatchImpl_processNextEvent(
        JNIEnv *jEnv, jclass jClass, jint jQType, jintArray paramArr)
{
    jobject edListener;
    uint32_t eventCode, eventData1, eventData2;
    jint* cArray;

    // Retrieve event data from MPE event queue
    edListener = mpe_edProcessNextEvent(jEnv, jQType, &eventCode, &eventData1,
            &eventData2);

    // Populate the Java array with the EDListener callback parameters
    cArray = (*jEnv)->GetIntArrayElements(jEnv, paramArr, NULL);
    cArray[0] = (jint) eventCode;
    cArray[1] = (jint) eventData1;
    cArray[2] = (jint) eventData2;
    (*jEnv)->ReleaseIntArrayElements(jEnv, paramArr, cArray, 0);

    // Finally, return the EDListener object
    return edListener;
}

/*
 * Class:     org_cablelabs_impl_manager_ed_EventDispatchImpl
 * Method:    createNativeEventQueue
 * Signature: (I)V
 */

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_ed_EventDispatchImpl_createNativeEventQueue
(JNIEnv *jEnv, jclass jClass, jint jQType)
{
    mpe_edCreateEventQueue((int)jQType);
}

/*
 * Class:     org_cablelabs_impl_manager_ed_EventDispatchImpl
 * Method:    deleteNativeEventQueue
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_ed_EventDispatchImpl_deleteNativeEventQueue
(JNIEnv *jEnv, jclass jClass, jint jQType)
{
    mpe_edDeleteEventQueue((int)jQType);
}

