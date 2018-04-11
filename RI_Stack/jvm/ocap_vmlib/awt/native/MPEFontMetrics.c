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

#include "common.h"
#include "awt.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_dbg.h>
#include <mpe_os.h>

#include <jni.h>
#include <java_awt_MPEFontMetrics.h>

JNIEXPORT void JNICALL Java_java_awt_MPEFontMetrics_initIDs
(JNIEnv * env, jclass cls)
{
    GET_FIELD_ID(MPEFontMetrics_ascentFID, "ascent", "I");
    GET_FIELD_ID(MPEFontMetrics_descentFID, "descent", "I");
    GET_FIELD_ID(MPEFontMetrics_heightFID, "height", "I");
    GET_FIELD_ID(MPEFontMetrics_leadingFID, "leading", "I");
    GET_FIELD_ID(MPEFontMetrics_maxAdvanceFID, "maxAdvance", "I");
    GET_FIELD_ID(MPEFontMetrics_maxAscentFID, "maxAscent", "I");
    GET_FIELD_ID(MPEFontMetrics_maxDescentFID, "maxDescent", "I");
    GET_FIELD_ID(MPEFontMetrics_maxHeightFID, "maxHeight", "I");
    GET_FIELD_ID(MPEFontMetrics_widthsFID, "widths", "[I");
    GET_CLASS(java_awt_Font, "java/awt/Font");
    GET_FIELD_ID(java_awt_Font_metricsFID, "metrics", "Ljava/awt/FontMetrics;");
}

JNIEXPORT jobject JNICALL Java_java_awt_MPEFontMetrics_pGetFontMetrics(
        JNIEnv *env, jclass cls, jobject font)
{
    /* Access the font.metrics element */
    jobject fm = (*env)->GetObjectField(env, font,
            MPECachedIDs.java_awt_Font_metricsFID);
    /* Return the fontMetrics */
    return fm;
}

JNIEXPORT void JNICALL Java_java_awt_MPEFontMetrics_pSetFontMetrics
(JNIEnv *env, jclass cls, jobject font, jobject fontMetrics)
{
    /* Set the font.metrics element */
    (*env)->SetObjectField (env, font,
            MPECachedIDs.java_awt_Font_metricsFID,
            (jobject)fontMetrics);
}

JNIEXPORT jint JNICALL Java_java_awt_MPEFontMetrics_pLoadFont
(JNIEnv * env, jobject this, jint _ff, jstring fontName, jint style, jint height)
{
    mpe_GfxFontFactory ff = (mpe_GfxFontFactory)_ff;
    const jchar *fname;
    mpe_GfxFont font = (mpe_GfxFont)NULL;
    mpe_GfxFontMetrics fontinfo;
    mpe_Error err;
    jboolean isCopy;
    jint nameLen;
    UNUSED(style);

    fname = (*env)->GetStringChars (env, fontName, &isCopy);
    nameLen = (*env)->GetStringLength(env, fontName);
    err = mpe_gfxFontNew(ff, (mpe_GfxWchar*)fname, nameLen, style, height, &font);
    (*env)->ReleaseStringChars (env, fontName, fname);

    if (err != MPE_SUCCESS || (mpe_GfxFont)font == NULL)
    {
        /* Should NOT happen! */
        (*env)->ThrowNew (env, MPECachedIDs.AWTError, "Could not load font");
        return 0;
    }

    mpe_gfxGetFontMetrics(font, &fontinfo);

    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_ascentFID, (jint) fontinfo.ascent);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_descentFID, (jint) fontinfo.descent);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_leadingFID, (jint) fontinfo.leading);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_heightFID, (jint) fontinfo.height);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_maxAdvanceFID, (jint) fontinfo.maxadvance);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_maxAscentFID, (jint) fontinfo.ascent);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_maxDescentFID, (jint) fontinfo.descent);
    (*env)->SetIntField (env, this, MPECachedIDs.MPEFontMetrics_maxHeightFID, (jint) fontinfo.height);

    /* Cache the first widths of the first 256 characters. */
    {
        jintArray intArray = (*env)->GetObjectField(env, this, MPECachedIDs.MPEFontMetrics_widthsFID);
        jint length;
        if (intArray != NULL
                && 0 < (length = (*env)->GetArrayLength(env, intArray)))
        {
            jint *widths = (*env)->GetIntArrayElements(env, intArray, NULL);
            if ( widths == NULL ) // Exception has been thrown

            {
                mpe_gfxFontDelete(font);
                return 0;
            }
            for(; length-- > 0;)
            mpe_gfxGetCharWidth(font, (mpe_GfxWchar)length, &widths[length]);
            (*env)->ReleaseIntArrayElements(env, intArray, widths, 0);
        }
    }

    MPE_LOG( MPEAWT_LOGFONT, MPEAWT_LOG_MOD, "MPEFontMetrics::pLoadFont(ff=%08x) -> %08x\n", ff, font );

    return (jint) font;
}

#define NULL_FONT ((mpe_GfxFont)0)

JNIEXPORT void JNICALL Java_java_awt_MPEFontMetrics_pDestroyFont
(JNIEnv *env, jclass cls, jint _font)
{
    mpe_GfxFont font = (mpe_GfxFont)_font;

    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGFONT, MPEAWT_LOG_MOD, "MPEFontMetrics::pDestroyFont(%08x)\n", font );
    if (font != NULL_FONT)
    mpe_gfxFontDelete(font);
}

JNIEXPORT jint JNICALL Java_java_awt_MPEFontMetrics_pCharWidth(JNIEnv * env,
        jclass cls, jint _font, jchar c)
{
    mpe_GfxFont font = (mpe_GfxFont) _font;
    int32_t width;

    UNUSED(cls);

    if (mpe_gfxGetCharWidth(font, (mpe_GfxWchar) c, &width) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not get string width");
        return 0;
    }
    return width;
}

JNIEXPORT jint JNICALL Java_java_awt_MPEFontMetrics_pCharsWidth(JNIEnv * env,
        jclass cls, jint _font, jcharArray charArray, jint offset, jint length)
{
    jchar *chars;
    jint width;
    mpe_GfxFont font = (mpe_GfxFont) _font;

    UNUSED(cls);

    if (mpe_memAllocP(MPE_MEM_TEMP, sizeof(jchar) * length, (void**) &chars)
            != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.OutOfMemoryError, NULL);
        return 0;
    }

    (*env)->GetCharArrayRegion(env, charArray, offset, length, chars);
    if ((*env)->ExceptionCheck(env))
    {
        mpe_memFreeP(MPE_MEM_TEMP, chars);
        return 0;
    }

    if (mpe_gfxGetString16Width(font, (mpe_GfxWchar*) chars, length, &width)
            != MPE_SUCCESS)
    {
        mpe_memFreeP(MPE_MEM_TEMP, chars);
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not get chars width");
        return 0;
    }
    mpe_memFreeP(MPE_MEM_TEMP, chars);

    return width;
}

JNIEXPORT jint JNICALL Java_java_awt_MPEFontMetrics_pStringWidth(JNIEnv * env,
        jclass cls, jint _font, jstring string)
{
    const jchar *chars;
    jboolean isCopy;
    jsize length;
    mpe_GfxFont font = (mpe_GfxFont) _font;
    mpe_Error err;
    jint width;

    UNUSED(cls);

    length = (*env)->GetStringLength(env, string);
    if (length == 0)
        return 0;

    chars = (*env)->GetStringChars(env, string, &isCopy);
    if (chars == NULL)
        return 0;

    err = mpe_gfxGetString16Width(font, (mpe_GfxWchar*) chars, length, &width);

    (*env)->ReleaseStringChars(env, string, chars);

    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not get string width");
        return 0;
    }
    return width;
}

