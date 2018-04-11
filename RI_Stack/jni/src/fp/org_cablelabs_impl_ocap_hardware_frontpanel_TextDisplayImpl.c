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

#include "org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl.h"

#include <mpe_frontpanel.h>
#include <mpe_dbg.h>
#include <mpe_os.h>
#include <mpe_error.h>

#include "jni_util.h"
#include <jni.h>

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl
 * Method:    nInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl_nInit
(JNIEnv* env, jclass cls)
{
    GET_FIELD_ID(TextDisplayImpl_characterSet,"m_characterSet","Ljava/lang/String;");
    GET_FIELD_ID(TextDisplayImpl_numColumns,"m_numColumns","I");
    GET_FIELD_ID(TextDisplayImpl_numRows,"m_numRows","I");
    GET_FIELD_ID(TextDisplayImpl_mode,"m_mode","I");
    GET_FIELD_ID(TextDisplayImpl_iterations,"m_blinkIterations","I");
    GET_FIELD_ID(TextDisplayImpl_maxCycleRate,"m_blinkMaxCycleRate","I");
    GET_FIELD_ID(TextDisplayImpl_onDuration,"m_blinkOnDuration","I");
    GET_FIELD_ID(TextDisplayImpl_brightness,"m_brightness","I");
    GET_FIELD_ID(TextDisplayImpl_brightnessLevels,"m_brightnessLevels","I");
    GET_FIELD_ID(TextDisplayImpl_color,"m_color","B");
    GET_FIELD_ID(TextDisplayImpl_supportedColors,"m_supportedColors","B");
    GET_FIELD_ID(TextDisplayImpl_maxHorizontalIterations,"m_scrollMaxHorizontalIterations","I");
    GET_FIELD_ID(TextDisplayImpl_maxVerticalIterations,"m_scrollMaxVerticalIterations","I");
    GET_FIELD_ID(TextDisplayImpl_horizontalIterations,"m_scrollHorizontalIterations","I");
    GET_FIELD_ID(TextDisplayImpl_verticalIterations,"m_scrollVerticalIterations","I");
    GET_FIELD_ID(TextDisplayImpl_holdDuration,"m_scrollHoldDuration","I");
}

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl
 * Method:    nGetTextData
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl_nGetTextData
(JNIEnv* env, jobject obj, jint indicator)
{
    mpe_Error err;
    mpe_FpCapabilities* capabilities;
    mpe_FpTextPanelMode mode;
    jstring charset;
    uint32_t brightness, color;
    mpe_FpBlinkSpec blinkSpec;
    mpe_FpScrollSpec scrollSpec;

    // Get our text capabilities from MPE
    if ((err = mpe_fpGetCapabilities(&capabilities)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nGetTextData() -- Error getting capabilities! returned: 0x%x\n", err);
        return;
    }

    // Get indicator settings
    if ((err = mpe_fpGetText(&mode, &color, &brightness, &blinkSpec, &scrollSpec)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nGetTextData() -- Error getting text data! returned: 0x%x\n", err);
        return;
    }

    // Character Set
    charset = (*env)->NewStringUTF(env,(const char*)(capabilities->supportedChars));
    (*env)->SetObjectField(env,obj,jniutil_CachedIds.TextDisplayImpl_characterSet,
            charset);

    // Columns, Rows, Mode
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_numColumns,
            capabilities->columns);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_numRows,
            capabilities->rows);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_mode,
            mode);

    // Blink settings
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_iterations,
            blinkSpec.iterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_maxCycleRate,
            capabilities->maxCycleRate[indicator]);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_onDuration,
            blinkSpec.onDuration);

    // Brightness settings
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_brightness,
            brightness);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_brightnessLevels,
            capabilities->brightness[indicator]);

    // Color settings
    (*env)->SetByteField(env,obj,jniutil_CachedIds.TextDisplayImpl_color,
            (char)color);
    (*env)->SetByteField(env,obj,jniutil_CachedIds.TextDisplayImpl_supportedColors,
            (char)capabilities->colors[indicator]);

    // Scroll settings
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_maxHorizontalIterations,
            capabilities->maxHorizontalIterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_maxVerticalIterations,
            capabilities->maxVerticalIterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_horizontalIterations,
            scrollSpec.horizontalIterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_verticalIterations,
            scrollSpec.verticalIterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.TextDisplayImpl_holdDuration,
            scrollSpec.holdDuration);
}

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl
 * Method:    nSetTextDisplay
 * Signature: (I[Ljava/lang/String;IIIIIII)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl_nSetTextDisplay
(JNIEnv* env, jobject obj,
        jint mode, jobjectArray text,
        jint blinkIterations, jint blinkOnDuration,
        jint brightness, jbyte color,
        jint horizontalIterations, jint verticalIterations,
        jint holdDuration)
{
    mpe_Error err = MPE_EINVAL;
    mpe_FpBlinkSpec blinkSpec =
    {   0, 0};
    mpe_FpScrollSpec scrollSpec =
    {   0, 0, 0};

    scrollSpec.horizontalIterations = (uint16_t)horizontalIterations;
    scrollSpec.verticalIterations = (uint16_t)verticalIterations;

    blinkSpec.iterations = (uint16_t)blinkIterations;
    blinkSpec.onDuration = (uint16_t)blinkOnDuration;

    if (mode == MPE_FP_TEXT_MODE_CLOCK_12HOUR || mode == MPE_FP_TEXT_MODE_CLOCK_24HOUR)
    {
        if ((err = mpe_fpSetText(mode,0,NULL,color,brightness,blinkSpec,scrollSpec)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nSetTextDisplay() -- Error setting indicator data! returned: 0x%x\n", err);
        }
    }
    else
    {
        int i;
        const char** textLines;
        jint numLines = (*env)->GetArrayLength(env,text);

        // Allocate space for our text lines
        if (mpe_memAllocP(MPE_MEM_FP, sizeof(char*)*numLines,(void**)&textLines) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nSetTextDisplay() -- Error allocating memory for text lines\n");
            return;
        }

        // Allocate C-strings for each line in the java string array
        for (i = 0; i < numLines; ++i)
        {
            if ((textLines[i] =
                            (*env)->GetStringUTFChars(env,(*env)->GetObjectArrayElement(env,text,i),NULL)) == NULL)
            {
                // we got an error (w/ an OutOfMemoryException) in GetStringUTFChars
                // clean up strings & exit
                while (--i >= 0)
                {
                    (*env)->ReleaseStringUTFChars(env,(*env)->GetObjectArrayElement(env,text,i),textLines[i]);
                }
                (void)mpe_memFreeP(MPE_MEM_FP, (void*)textLines);
                return;
            }
        }

        // Set the text display
        if ((err = mpe_fpSetText(mode,numLines,textLines,color,brightness,blinkSpec,scrollSpec)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR,MPE_MOD_JNI,"nSetTextDisplay() -- Error setting indicator data! returned: 0x%x\n", err);
        }

        // Release the C-strings & text line array
        for (i = 0; i < numLines; ++i)
        {
            (*env)->ReleaseStringUTFChars(env,(*env)->GetObjectArrayElement(env,text,i),textLines[i]);
        }
        (void)mpe_memFreeP(MPE_MEM_FP, (void*)textLines);
    }
}
