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

#ifndef JNI_UTIL_HN_H
#define JNI_UTIL_HN_H

#include <jni.h>
#include <mpeos_hn.h>



/*****************************************************************************/
/***                                                                       ***/
/***                         Shared player/server                          ***/
/***                                                                       ***/
/*****************************************************************************/

static const int NON_SPECIFIED_INT = 0xFFFF;
static const long NON_SPECIFIED_LONG = 0xFFFF;
static const float NON_SPECIFIED_FLOAT = 999.0;

void throwMPEMediaError(JNIEnv *env, int nativeErrCode, const char *nativeErrMsg);

mpe_Error buildStreamParamsFromObject(JNIEnv *env,
    jobject jStreamParams, mpe_HnStreamParams** streamParams);

void deallocateStreamParams(mpe_HnStreamParams* params);

mpe_Error buildPlaybackParamsFromObject(JNIEnv *env,
    jobject jPlaybackParams, mpe_HnPlaybackParams** playbackParams);

void deallocatePlaybackParams(mpe_HnPlaybackParams* params);



/*****************************************************************************/
/***                                                                       ***/
/***                             Server only                               ***/
/***                                                                       ***/
/*****************************************************************************/

mpe_Error buildContentDescriptionFromObject(JNIEnv *env,
        jint jContentLocationType, jobject jContentDescription,
        void** contentDescription);

void deallocateContentDescription(mpe_HnStreamContentLocation contentLocation,
        void *contentDescription);

mpe_Error buildContentTransformationFromObject(JNIEnv *env, jobject jTransformation,
                                                      mpe_hnContentTransformation** transformationPtr);

void deallocateContentTransformation(mpe_hnContentTransformation *contentTransformation);

/*****************************************************************************/
/***                                                                       ***/
/***                             Player only                               ***/
/***                                                                       ***/
/*****************************************************************************/

void populateAVStreamParametersFromObject(JNIEnv *env,
        jobject jAVStreamParams, mpe_HnHttpHeaderAVStreamParameters *avsParams);

mpe_Error populatePlayspeedsFromObject(JNIEnv *env, jobject jStreamParams,
            mpe_HnStreamParamsMediaPlayerHttp* playerStreamParams);

#endif /* #ifndef JNI_UTIL_HN_H */
