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

#ifndef _AWT_H_
#define _AWT_H_

#include <jni.h>

struct CachedIDs
{
    jclass OutOfMemoryError;
    jclass NullPointerException;
    jclass AWTError;
    jclass DirectColorModel;
    jclass IndexColorModel;

    jclass java_awt_image_BufferedImage;

    jclass MPEFontMetrics;

    jfieldID MPEFontMetrics_ascentFID;
    jfieldID MPEFontMetrics_descentFID;
    jfieldID MPEFontMetrics_heightFID;
    jfieldID MPEFontMetrics_leadingFID;
    jfieldID MPEFontMetrics_maxAdvanceFID;
    jfieldID MPEFontMetrics_maxAscentFID;
    jfieldID MPEFontMetrics_maxDescentFID;
    jfieldID MPEFontMetrics_maxHeightFID;
    jfieldID MPEFontMetrics_widthsFID;

    jfieldID MPEImage_widthFID;
    jfieldID MPEImage_heightFID;

    jfieldID java_awt_image_IndexColorModel_rgbFID;

    jfieldID java_awt_image_DirectColorModel_red_maskFID;
    jfieldID java_awt_image_DirectColorModel_red_offsetFID;
    jfieldID java_awt_image_DirectColorModel_red_scaleFID;
    jfieldID java_awt_image_DirectColorModel_green_maskFID;
    jfieldID java_awt_image_DirectColorModel_green_offsetFID;
    jfieldID java_awt_image_DirectColorModel_green_scaleFID;
    jfieldID java_awt_image_DirectColorModel_blue_maskFID;
    jfieldID java_awt_image_DirectColorModel_blue_offsetFID;
    jfieldID java_awt_image_DirectColorModel_blue_scaleFID;
    jfieldID java_awt_image_DirectColorModel_alpha_maskFID;
    jfieldID java_awt_image_DirectColorModel_alpha_offsetFID;
    jfieldID java_awt_image_DirectColorModel_alpha_scaleFID;

    jfieldID java_awt_image_BufferedImage_peerFID;

    jmethodID java_awt_image_BufferedImage_constructor;

    jclass Thread;
    jmethodID java_lang_Thread_interruptedMID;

    jmethodID DirectColorModel_constructor;
    jmethodID IndexColorModel_constructor;
    jmethodID java_awt_image_ColorModel_getRGBMID;

    jclass MPEToolkit;
    jmethodID MPEToolkit_postKeyEventMID;

    jmethodID MPEFontMetrics_getFontMetricsMID;

    jclass java_awt_Font;
    jfieldID java_awt_Font_metricsFID;

    jclass java_awt_Component;
    jmethodID java_awt_Component_setBoundsMID;
};

extern struct CachedIDs MPECachedIDs;

#define JNIVERSION JNI_VERSION_1_2

/* These macros can be used when initializing cached ids stored in the MPECachedIDs structure.
 They should be called from static initializes of classes to get any IDs they use.
 In debug mode, they will check the ID exists and return if not (causing an exception to be thrown).
 In non-debug mode they are optimised for space and performance to assume the ID exists. This
 is because they are probably romized with the classes and once they work it is not necessary
 to check every time that they exist. */

#if CVM_DEBUG

#define FIND_CLASS(name) if ((cls = (*env)->FindClass(env, name)) == NULL) return;
#define GET_CLASS(id,name) if ((cls = MPECachedIDs.id = (*env)->NewGlobalRef(env, (*env)->FindClass(env, name))) == NULL) return;
#define GET_FIELD_ID(id,name,type) if ((MPECachedIDs.id = (*env)->GetFieldID(env, cls, name, type)) == NULL) return;
#define GET_METHOD_ID(id,name,type) if ((MPECachedIDs.id = (*env)->GetMethodID(env, cls, name, type)) == NULL) return;
#define GET_STATIC_METHOD_ID(id,name,type) if ((MPECachedIDs.id = (*env)->GetStaticMethodID(env, cls, name, type)) == NULL) return;

#else

#define FIND_CLASS(name) cls = (*env)->FindClass(env, name);
#define GET_CLASS(id,name) cls = MPECachedIDs.id = (*env)->NewGlobalRef(env, (*env)->FindClass(env, name));
#define GET_FIELD_ID(id,name,type) MPECachedIDs.id = (*env)->GetFieldID(env, cls, name, type);
#define GET_METHOD_ID(id,name,type) MPECachedIDs.id = (*env)->GetMethodID(env, cls, name, type);
#define GET_STATIC_METHOD_ID(id,name,type) MPECachedIDs.id = (*env)->GetStaticMethodID(env, cls, name, type);

#endif

#define TRUE 1
#define FALSE 0

#ifndef min
# define min(x,y) ((x < y) ? x : y)
#endif
#ifndef max
# define max(x,y) ((x > y) ? x : y)
#endif

#endif

