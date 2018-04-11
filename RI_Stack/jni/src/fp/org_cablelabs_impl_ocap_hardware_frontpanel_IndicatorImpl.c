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

#include "org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl.h"
#include "jni_util.h"

#include <mpe_frontpanel.h>
#include <mpe_dbg.h>
#include <mpe_error.h>

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl
 * Method:    nInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl_nInit
(JNIEnv* env, jclass cls)
{
    GET_FIELD_ID(IndicatorImpl_iterations,"m_blinkIterations","I");
    GET_FIELD_ID(IndicatorImpl_maxCycleRate,"m_blinkMaxCycleRate","I");
    GET_FIELD_ID(IndicatorImpl_onDuration,"m_blinkOnDuration","I");
    GET_FIELD_ID(IndicatorImpl_brightness,"m_brightness","I");
    GET_FIELD_ID(IndicatorImpl_brightnessLevels,"m_brightnessLevels","I");
    GET_FIELD_ID(IndicatorImpl_color,"m_color","B");
    GET_FIELD_ID(IndicatorImpl_supportedColors,"m_supportedColors","B");
}

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl
 * Method:    nSetIndicator
 * Signature: (IIIIB)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl_nSetIndicator
(JNIEnv* env, jobject obj,
        jint indicator, jint brightness, jint iterations, jint onDuration, jbyte color)
{
    mpe_Error err;
    mpe_FpBlinkSpec blinkSpec;
    blinkSpec.iterations = (uint16_t)iterations;
    blinkSpec.onDuration = (uint16_t)onDuration;

    if ((err = mpe_fpSetIndicator(indicator,brightness,color,blinkSpec)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "nGetIndicatorData() -- Error setting indicator data! returned: 0x%x\n", err);
    }
}

/*
 * Class:     org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl
 * Method:    nGetIndicatorData
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl_nGetIndicatorData
(JNIEnv* env, jobject obj, jint indicator)
{
    mpe_Error err;
    mpe_FpCapabilities* capabilities;
    uint32_t brightness, color;
    mpe_FpBlinkSpec blinkSpec;

    // Get our text capabilities from MPE
    if ((err = mpe_fpGetCapabilities(&capabilities)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "nGetIndicatorData() -- Error getting capabilities! returned: 0x%x\n", err);
        return;
    }

    // Get indicator settings
    if ((err = mpe_fpGetIndicator(indicator, &brightness, &color, &blinkSpec)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "nGetIndicatorData() -- Error getting indicator data! returned: 0x%x\n", err);
        return;
    }

    //
    // Set the data fields of our IndicatorImpl object
    //

    // Blink settings
    (*env)->SetIntField(env,obj,jniutil_CachedIds.IndicatorImpl_iterations,
            blinkSpec.iterations);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.IndicatorImpl_onDuration,
            blinkSpec.onDuration);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.IndicatorImpl_maxCycleRate,
            capabilities->maxCycleRate[indicator]);

    // Brightness settings
    (*env)->SetIntField(env,obj,jniutil_CachedIds.IndicatorImpl_brightness,
            brightness);
    (*env)->SetIntField(env,obj,jniutil_CachedIds.IndicatorImpl_brightnessLevels,
            capabilities->brightness[indicator]);

    // Color settings
    (*env)->SetByteField(env,obj,jniutil_CachedIds.IndicatorImpl_color,
            (char)color);
    (*env)->SetByteField(env,obj,jniutil_CachedIds.IndicatorImpl_supportedColors,
            (char)capabilities->colors[indicator]);
}
