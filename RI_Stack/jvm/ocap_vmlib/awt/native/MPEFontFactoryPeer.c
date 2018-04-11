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

/**
 * Allow native layer to initialize and cache ids.
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_ui_MPEFontFactoryPeer_initJNI
(JNIEnv *env, jclass cls)
{
    UNUSED(env);

    MPE_LOG(MPE_LOG_DEBUG, MPEAWT_LOG_MOD, "MPEFontFactoryPeer_initJNI!\n");

    GET_CLASS(MPEFontMetrics, "java/awt/MPEFontMetrics");
    GET_STATIC_METHOD_ID(MPEFontMetrics_getFontMetricsMID, "getFontMetrics", "(Ljava/awt/Font;I)Ljava/awt/MPEFontMetrics;");
}

/**
 * Creates the native peer for this <code>FontFactory</code>.
 * Will throw any appropriate runtime exceptions or errors if necessary.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_ui_MPEFontFactoryPeer_nCreate
(JNIEnv *env, jobject this)
{
    mpe_GfxFontFactory ff = (mpe_GfxFontFactory)0;
    UNUSED(env);
    UNUSED(this);

    if (mpe_gfxFontFactoryNew(&ff) != MPE_SUCCESS)
    {
        MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nCreate - could not create FontFactory\n" );

        (*env)->ThrowNew(env, MPECachedIDs.OutOfMemoryError, "Could not allocate FontFactory");
    }
    MPE_LOG( MPEAWT_LOGFONT, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nCreate() -> %08x\n", ff );

    return (jint)ff;
}

/**
 * Adds the given font description to the native peer font factory.
 *
 * @param ff native font factory
 * @param data array of PFR font data
 * @param name name of font or <code>null</code> if it should be assumed from
 * PFR font data; if <code>null</code> then <code>style</code>, <code>minSize</code>,
 * and <code>maxSize</code> will be ignored
 * @param style style for which this font is to be used
 * @param minSize minimum size for which this font is to be used; use 0 for all sizes
 * @param maxSize maximum size for which this font is to be used; use 65535 for all sizes
 *
 * @throws FontFormatException  if the file at that URL is not a valid
 *         font file as specified in the main body of this specification
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_ui_MPEFontFactoryPeer_nAddFont
(JNIEnv *env, jobject this, jint _ff, jbyteArray byteArray, jstring jname,
        jint style, jint minSize, jint maxSize)
{
    mpe_GfxFontFactory ff = (mpe_GfxFontFactory)_ff;
    mpe_Error err = (mpe_Error)-1;
    mpe_GfxFontDesc font;
    int noName;
    jbyte* bytes;
    jint bytesLength;
    UNUSED(this);

    bytesLength = (*env)->GetArrayLength(env, byteArray);
    if (bytesLength == 0)
    {
        MPE_LOG( MPEAWT_LOGFAILUSER, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nAddFont - byte[].length == 0\n" );
        return -1;
    }

    font.fnt_format = GFX_FONT_PFR;
    if (jname == NULL)
    {
        noName = 1;
        font.name = NULL;
        font.style = 0; /* !!! should we have style here? */
        font.minsize = 0; /* !!!! */
        font.maxsize = 65535; /* !!!! */
    }
    else
    {
        noName = 0;
        font.style = style;
        font.minsize = minSize;
        font.maxsize = maxSize;
        font.name = (mpe_GfxWchar*)(*env)->GetStringChars(env, jname, NULL);
        font.namelength = (*env)->GetStringLength(env, jname);
        if (font.name == NULL)
        {
            MPE_LOG( MPEAWT_LOGFAILUSER, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nAddFont - could not get name\n" );
            return -1;
        }
    }

    bytes = (*env)->GetByteArrayElements(env, byteArray, NULL);
    if (bytes == NULL)
    {
        err = (mpe_Error)-1;
        goto fail;
    }
    font.data = (uint8_t*)bytes;
    font.datasize = bytesLength;

    /* Add font to factory */
    err = mpe_gfxFontFactoryAdd(ff, &font);

    /* Release String and byte[] */
    fail:
    if (bytes != NULL)
    (*env)->ReleaseByteArrayElements(env, byteArray, bytes, JNI_ABORT);
    if (!noName && font.name != NULL)
    (*env)->ReleaseStringChars(env, jname, (jchar*)font.name);

#if 0
    switch(err)
    {
        case MPE_SUCCESS:
        break;
        case GFX_ERR_FONTFORMAT:
        case GFX_ERR_NOFONT:
        (*env)->ThrowNew(env, MPECachedIDs.FontFormatException, "Invalid font format");
        break;
        case GFX_ERR_FF_DEL_PENDING:
        default:
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "MPEFontFactoryPeer invalid");
        break;
    }
#endif

    /* non-zero means an error! */
    return err;
}

/**
 * Disposes the given FontFactory.
 *
 * @param  ff native font factory
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_ui_MPEFontFactoryPeer_nDispose
(JNIEnv *env, jobject this, int _ff)
{
    mpe_GfxFontFactory ff = (mpe_GfxFontFactory)_ff;
    mpe_Error err;
    UNUSED(this);
    UNUSED(env);

    MPE_LOG( MPEAWT_LOGFONT, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nDispose(%08x)\n", ff );

    if ((err = mpe_gfxFontFactoryDelete(ff)) != MPE_SUCCESS)
    {
        MPE_LOG( MPEAWT_LOGFAIL, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nDispose - could not delete FontFactory (%d)!\n", err );
    }
}

/**
 * Using the native peer for this <code>FontFactory</code>, create a new font.
 * Instantiates a new font using the native peer font factory and
 * performs any necessary tasks to provide the "glue" into the java.awt.Toolkit
 * implementation.
 *
 * @return a new instance of a font or null if the font is not available
 */
JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_dvb_ui_MPEFontFactoryPeer_nNewFont
(JNIEnv *env, jobject this, jint ff, jobject fontObj)
{
    jobject fontMetricsObj;
    UNUSED(this);

    MPE_LOG( MPEAWT_LOGFONT, MPEAWT_LOG_MOD, "MPEFontFactoryPeer::nNewFont(ff=%08x)\n", ff );

    /*
     * Call MPEFontMetrics.getFontMetrics to get around package-private nature of call.
     * We don't really want the fontmetrics object at this time.
     * This will create the fontmetrics as well as the native font peer.
     * It will store the fontmetrics in the font object; 
     * the fontmetrics and native font peer will be stored in the fontmetrics object.
     *     fontMetricsObj = MPEFontMetrics.getFontMetrics(font, ff);
     */
    fontMetricsObj = (*env)->CallStaticObjectMethod(env, MPECachedIDs.MPEFontMetrics,
            MPECachedIDs.MPEFontMetrics_getFontMetricsMID,
            fontObj, ff);

    return (fontMetricsObj == NULL) ? NULL : fontObj;
}
