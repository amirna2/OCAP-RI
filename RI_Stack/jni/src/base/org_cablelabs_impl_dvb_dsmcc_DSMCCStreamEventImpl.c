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

#include <org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl.h>
#include <org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl.h>

#include <mgrdef.h>
#include <mpe_types.h>
#include <mpe_file.h>
#include <mpe_ed.h>

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl
 * Method:    nativeGetNumEvents
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_nativeGetNumEvents(
        JNIEnv *env, jclass clazz, jint streamHandle)
{
    mpe_Error retCode;
    mpe_FileInfo info;

    MPE_UNUSED_PARAM(clazz);

    // First, we check to make sure that we're a DSMCCStreamEvent object
    // Done here because the base constructor is common between the two, but DSMCCStreamEvent
    // will call this immediately afterwards.
    retCode = mpe_fileGetFStat((mpe_File *) streamHandle, MPE_FS_STAT_TYPE,
            &info);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot get size");
        return 0;
    }
    if (info.type != MPE_FS_TYPE_STREAMEVENT)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                "org/dvb/dsmcc/IllegalObjectTypeException"), "Cannot get size");
        return 0;
    }
    retCode = mpe_fileGetFStat((mpe_File *) streamHandle, MPE_FS_STAT_SIZE,
            &info);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot get size");
        return 0;
    }
    return info.size;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl
 * Method:    nativeGetEventName
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_nativeGetEventName(
        JNIEnv *env, jclass clazz, jint streamHandle, jlong eventNumber)
{
    mpe_Error retCode;
    mpe_StreamEventInfo event;
    int64_t seekPos = (int64_t) eventNumber;
    jobject eventName;

    MPE_UNUSED_PARAM(clazz);

    // Seek to the position and read the data out.
    retCode
            = mpe_fileSeek((mpe_File *) streamHandle, MPE_FS_SEEK_SET, &seekPos);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot seek to find event");
        return NULL;
    }
    retCode = mpe_streamReadEvent((mpe_File *) streamHandle, &event);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot read event");
        return NULL;
    }
    // Cons up a String object based on the event name field
    eventName = (*env)->NewStringUTF(env, event.name);
    return eventName;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl
 * Method:    nativeGetEventID
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_nativeGetEventID(
        JNIEnv *env, jclass clazz, jint streamHandle, jlong eventNumber)
{
    mpe_Error retCode;
    mpe_StreamEventInfo event;
    int64_t seekPos = (int64_t) eventNumber;

    MPE_UNUSED_PARAM(clazz);

    // Seek to the position and read the data out.
    retCode
            = mpe_fileSeek((mpe_File *) streamHandle, MPE_FS_SEEK_SET, &seekPos);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot seek to find event");
        return 0;
    }
    retCode = mpe_streamReadEvent((mpe_File *) streamHandle, &event);
    if (retCode != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot read event");
        return 0;
    }
    return event.eventId;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl
 * Method:    nativeGetEventTag
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_nativeGetEventTag(
        JNIEnv *env, jclass clazz, jint streamHandle)
{
    mpe_Error retCode;
    uint16_t tag; // Associated Tag
    uint16_t tapid; // tap ID

    MPE_UNUSED_PARAM(clazz);

    // OK, this algorithm is a little different now. The tag type field is normally set to MPE_OC_ALL_TAPS.
    // We are only interested in taps of type STR_EVENT_USE and STR_STATUS_AND_EVENT_USE.

    retCode = mpe_streamReadTap((mpe_File *) streamHandle,
            org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_STR_EVENT_USE, 0,
            &tag, &tapid);
    if (retCode != MPE_SUCCESS)
    {
        retCode
                = mpe_streamReadTap(
                        (mpe_File *) streamHandle,
                        org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_STR_STATUS_AND_EVENT_USE,
                        0, &tag, &tapid);
        if (retCode != MPE_SUCCESS)
        {
            // (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), "Cannot get tag");
            return org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl_INVALID_TAG;
        } // if (retCode....
    } // if (retCode....
    return (jint) tag;
}
