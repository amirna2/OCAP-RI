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

#include <org_cablelabs_impl_ocap_hn_NetManagerImpl.h>
#include "jni_util.h"
#include "jni_util_hn.h"
#include <jni.h>
#include <string.h>
#include <mpe_dbg.h>
#include <mpe_hn.h>
#include <mpe_ed.h>
#include <mpe_os.h>
#include <mpe_socket.h>
#include <mpeos_dll.h>

#include <inttypes.h> // for PRIx64

// Define the memory to be HN allocated category
#define MPE_MEM_DEFAULT MPE_MEM_HN

/*
 * Class:     org_cablelabs_impl_ocap_hn_NetManagerImpl
 * Method:    setInterfaceListener 
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hn_NetManagerImpl_setInterfaceListener
  (JNIEnv *env, jclass cls, jstring interface, jobject caller)
{

    mpe_Error err = MPE_SUCCESS;
    mpe_EdEventInfo *edHandle = NULL;

    MPE_UNUSED_PARAM(cls);
    MPE_UNUSED_PARAM(caller);

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,
        "HN NetManagerImpl.setInterfaceListener - Enter\n");

    const char *interfaceSz = NULL;

    /* Allocate interface string */
    if (interface != NULL &&
        (interfaceSz = (*env)->GetStringUTFChars(env, interface, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "%s() - GetStringUTFChars(interface) "
                "returned NULL\n", __FUNCTION__);
        return;
    }

    if (caller != NULL)
    {
        err = mpe_edCreateHandle(caller, MPE_ED_QUEUE_NORMAL, NULL,
                MPE_ED_TERMINATION_OPEN, 0, &edHandle);
        if (err != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                    "mpe_edCreateHandle() FAILED! -- returned: 0x%x\n", err);
        }
        else
        {
            err = mpe_socketRegisterForIPChanges((char *)interfaceSz, edHandle->eventQ,
               (void*) edHandle);
            if (err != MPE_SUCCESS)
            {
                mpe_edDeleteHandle(edHandle);
                MPE_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_JNI,
                        "mpe_hnRegisterForIPChanges() FAILED! -- returned: 0x%x\n",
                        err);
            }
        }

    }

}

