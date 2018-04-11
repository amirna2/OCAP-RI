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

#include <org_cablelabs_impl_debug_Debug.h>
#include <mpe_types.h>
#include <mpe_os.h>
#include <mpe_dbg.h>

/*
 * Class:     org_cablelabs_impl_debug_Debug
 * Method:    nMsg
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_debug_Debug_nMsg
(JNIEnv *env, jclass cls, jstring jMsg, jint jLevel)
{
    const char *msg;

    MPE_UNUSED_PARAM(cls);

    /* get local reference to debug string */
    if ((msg = (*env)->GetStringUTFChars(env,jMsg,NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    switch (jLevel)
    {
        case org_cablelabs_impl_debug_Debug_FATAL: MPE_LOG(MPE_LOG_FATAL, MPE_MOD_JAVA, "%s", msg); break;
        case org_cablelabs_impl_debug_Debug_ERROR: MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JAVA, "%s", msg); break;
        case org_cablelabs_impl_debug_Debug_WARN: MPE_LOG(MPE_LOG_WARN, MPE_MOD_JAVA, "%s", msg); break;
        case org_cablelabs_impl_debug_Debug_INFO: MPE_LOG(MPE_LOG_INFO, MPE_MOD_JAVA, "%s", msg); break;
        case org_cablelabs_impl_debug_Debug_DEBUG: MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JAVA, "%s", msg); break;
        case org_cablelabs_impl_debug_Debug_TRACE: MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_JAVA, "%s", msg); break;
        default:
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "<Debug_nMsg> unknown log level %d\n", (int)jLevel);
        /* leave level at default (DEBUG) level */
        break;
    }

    // Release string resource.
    (*env)->ReleaseStringUTFChars(env, jMsg, msg);
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_debug_Debug_nProdMsg
(JNIEnv *env, jclass cls, jstring string)
{
    // get local reference to debug string
    const char *msg = (*env)->GetStringUTFChars(env,string, 0);

    MPE_UNUSED_PARAM(cls);

    /* log this production level message */
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_PROD, "%s", msg);

    // Release string resource.
    (*env)->ReleaseStringUTFChars(env,string, msg);
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_debug_Debug_nFDRMsg
(JNIEnv *env, jclass cls, jstring string)
{
    // get local reference to debug string
    const char *msg = (*env)->GetStringUTFChars(env,string, 0);

    MPE_UNUSED_PARAM(cls);

    /* log this FDR level messages */
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FDR, "%s", msg);

    // Release string resource.
    (*env)->ReleaseStringUTFChars(env,string, msg);
}

JNIEXPORT void JNICALL Java_org_cablelabs_impl_debug_Debug_nAddLogEntry
  (JNIEnv *env, jclass cls, jstring oidIn, jstring timeStampIn, jstring messageIn)
{
    // get local reference to OID
    char *oid = (char*)((*env)->GetStringUTFChars(env, oidIn, 0));

    // get local reference to timeStamp
    char *timeStamp = (char*)((*env)->GetStringUTFChars(env, timeStampIn, 0));

    // get local reference to message
    char *message = (char*)((*env)->GetStringUTFChars(env, messageIn, 0));

    mpe_Error rc = mpe_dbgAddLogEntry(oid, timeStamp, message);

    if (rc != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "<Debug_nAddLogEntry> %d = mpe_dbgAddLogEntry();\n", rc);
    }
 
    // Release string resources.
    (*env)->ReleaseStringUTFChars(env, oidIn, oid);
    (*env)->ReleaseStringUTFChars(env, timeStampIn, timeStamp);
    (*env)->ReleaseStringUTFChars(env, messageIn, message);
}

