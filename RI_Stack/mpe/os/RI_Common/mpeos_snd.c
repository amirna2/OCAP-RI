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

/* Header Files */
#include <mpe_error.h>
#include <mpeos_snd.h>
#include <mpe_dbg.h>
#include <ri_ui_manager.h>

/**
 * <i>mpeos_sndInit()</i>
 *
 * Initializes platform specific sound support.
 *
 */
void mpeos_sndInit(void)
{
}

/**
 * <i>mpeos_sndGetDeviceCount()</i>
 *
 * Get the number of sound devices
 */
mpe_Error mpeos_sndGetDeviceCount(uint32_t *count)
{
    MPE_UNUSED_PARAM(count);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndGetSoundDevices()</i>
 *
 * Get the sound device handles
 */
mpe_Error mpeos_sndGetDevices(mpe_SndDevice *devs, uint32_t *count)
{
    MPE_UNUSED_PARAM(devs);
    MPE_UNUSED_PARAM(count);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndGetMaxPlaybacks()</i>
 *
 * Get the maximum number of playbacks allowed for dev.
 */
mpe_Error mpeos_sndGetMaxPlaybacks(mpe_SndDevice dev, int32_t *maxPlaybacks)
{
    MPE_UNUSED_PARAM(dev);
    MPE_UNUSED_PARAM(maxPlaybacks);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndGetDevicesForSound()</i>
 *
 * Get the devices that can play the specified sound.
 */
mpe_Error mpeos_sndGetDevicesForSound(mpe_SndSound sound,
        mpe_SndDevice *devices, uint32_t *count)
{
    MPE_UNUSED_PARAM(sound);
    MPE_UNUSED_PARAM(devices);
    MPE_UNUSED_PARAM(count);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndCreateSound()</i>
 *
 * Create a sound object.
 */
mpe_Error mpeos_sndCreateSound(const char *type, const char *data,
        uint32_t offset, uint32_t size, mpe_SndSound *sound)
{
    MPE_UNUSED_PARAM(type);
    MPE_UNUSED_PARAM(data);
    MPE_UNUSED_PARAM(offset);
    MPE_UNUSED_PARAM(size);
    MPE_UNUSED_PARAM(sound);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndDeleteSound()</i>
 *
 * Release all resources allocated in CreateSound()
 */
mpe_Error mpeos_sndDeleteSound(mpe_SndSound sound)
{
    MPE_UNUSED_PARAM(sound);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndPlay()</i>
 *
 * Play the specified sound.
 */
mpe_Error mpeos_sndPlay(mpe_SndDevice device, mpe_SndSound sound,
        mpe_EdHandle handle, int64_t start, mpe_Bool loop, mpe_Bool muted, float requestedGain, float *actualGain,
        mpe_SndPlayback *playback)
{
    MPE_UNUSED_PARAM(device);
    MPE_UNUSED_PARAM(sound);
    MPE_UNUSED_PARAM(handle);
    MPE_UNUSED_PARAM(start);
    MPE_UNUSED_PARAM(loop);
    MPE_UNUSED_PARAM(muted);
    *actualGain = requestedGain;
    MPE_UNUSED_PARAM(playback);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndStop()</i>
 *
 * Stop the ongoing playback
 */
mpe_Error mpeos_sndStop(mpe_SndPlayback playback, int64_t *stopTime)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(stopTime);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndSetTime()</i>
 *
 * Set the media time.
 */
mpe_Error mpeos_sndSetTime(mpe_SndPlayback playback, int64_t *setTime)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(setTime);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndGetTime()</i>
 *
 * Get the media time
 */
mpe_Error mpeos_sndGetTime(mpe_SndPlayback playback, int64_t *getTime)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(getTime);
    return MPE_EINVAL;
}

/**
 * <i>mpeos_sndSetMute()</i>
 *
 * Set the mute
 */
mpe_Error mpeos_sndSetMute(mpe_SndPlayback playback, mpe_Bool mute)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(mute);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_sndSetGain()</i>
 *
 * Set the gain
 */
mpe_Error mpeos_sndSetGain(mpe_SndPlayback playback, float gain, float *actualGain)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(gain);
    *actualGain = gain;
    return MPE_SUCCESS;
}

/* DSExt */
/* See API doc (mpeos_snd.h) */
mpe_Error mpeos_sndAddAudioOutputPort(mpe_SndDevice device,
        mpe_SndAudioPort port)
{
    MPE_UNUSED_PARAM(device);
    MPE_UNUSED_PARAM(port);
    return MPE_SUCCESS;

}

/* DSExt */
/* See API doc (mpeos_snd.h) */
mpe_Error mpeos_sndRemoveAudioOutputPort(mpe_SndDevice device,
        mpe_SndAudioPort port)
{
    MPE_UNUSED_PARAM(device);
    MPE_UNUSED_PARAM(port);
    return MPE_SUCCESS;

}

/* DSExt */
/* See API doc (mpeos_snd.h) */
mpe_Error mpeos_sndGetAudioOutputPortInfo(mpe_SndAudioPort handle,
        mpe_SndAudioOutputPortInfo* info)
{
    mpe_Error returnThis = MPE_EINVAL;
    if (handle != NULL && info != NULL)
    {
        ri_backpanel_t* bp = ri_get_backpanel();
        bp->getAudioOutputPortValue(handle, AUDIO_OUTPUT_PORT_ID,
                &(info->idString));
        bp->getAudioOutputPortValue(handle, COMPRESSION, &(info->compression));
        bp->getAudioOutputPortValue(handle, GAIN, &(info->gain));
        bp->getAudioOutputPortValue(handle, ENCODING, &(info->encoding));
        bp->getAudioOutputPortValue(handle, LEVEL, &(info->level));
        bp->getAudioOutputPortValue(handle, STEREO_MODE, &(info->stereoMode));
        bp->getAudioOutputPortValue(handle, LOOP_THRU, &(info->loopThru));
        bp->getAudioOutputPortValue(handle, MUTED, &(info->muted));
        bp->getAudioOutputPortValue(handle, OPTIMAL_LEVEL,
                &(info->optimalLevel));
        bp->getAudioOutputPortValue(handle, MAX_DB, &(info->maxDb));
        bp->getAudioOutputPortValue(handle, MIN_DB, &(info->minDb));

        info->supportedCompressionsSize
                = bp->getAudioOutputPortSupportedCompressions(handle,
                        &(info->supportedCompressions));
        info->supportedEncodingsSize
                = bp->getAudioOutputPortSupportedEncodings(handle,
                        &(info->supportedEncodings));
        info->supportedStereoModesSize
                = bp->getAudioOutputPortSupportedStereoModes(handle,
                        &(info->supportedStereoModes));

        returnThis = MPE_SUCCESS;
    }
    return returnThis;
}

/* DSExt */
/* See API doc (mpeos_snd.h) */
mpe_Error mpeos_sndSetAudioOutputPortValue(mpe_SndAudioPort handle,
        int32_t valueId, void* valuePtr, void* actualValuePtr)
{
    mpe_SndAudioOutputPortInfo audioInfo;
    if (handle == NULL || valuePtr == NULL || mpeos_sndGetAudioOutputPortInfo(
            handle, &audioInfo) != MPE_SUCCESS)
    {
        return MPE_EINVAL;
    }

    ri_backpanel_t* bp = ri_get_backpanel();
    mpe_Error returnValue = MPE_EINVAL;
    int supportedValuesSize = 0;
    int* supportedValues = NULL;
    ri_audio_output_port_cap whichDiscreetValue = -1;
    ri_bool loopThruSupported;

    switch (valueId)
    {
    //boolean
    case AUDIO_PORT_LOOP_THRU_VALUE_ID:
        bp->getAudioOutputPortValue(handle, LOOP_THRU_SUPPORTED,
                &loopThruSupported);
        if (loopThruSupported == true)
        {
            bp->setAudioOutputPortValue(handle, LOOP_THRU, valuePtr);
            returnValue = MPE_SUCCESS;
        }
        break;

    case AUDIO_PORT_MUTED_VALUE_ID:
        bp->setAudioOutputPortValue(handle, MUTED, valuePtr);
        returnValue = MPE_SUCCESS;
        break;

        //floats
    case AUDIO_PORT_GAIN_VALUE_ID:
        //do nothing if invalid value...
        if (*((float*) valuePtr) > audioInfo.maxDb || *((float*) valuePtr)
                < audioInfo.minDb)
        {
            returnValue = MPE_EINVAL;
        }
        else if (actualValuePtr != NULL)
        {
            bp->setAudioOutputPortValue(handle, GAIN, valuePtr);
            *((float*) actualValuePtr) = *((float*) valuePtr);
            returnValue = MPE_SUCCESS;
        }
        else
        {
            returnValue = MPE_SUCCESS;
        }
        break;

    case AUDIO_PORT_LEVEL_VALUE_ID:
        //do nothing if invalid value...
        if (*((float*) valuePtr) > 1.0 || *((float*) valuePtr) < 0.0)
        {
            returnValue = MPE_EINVAL;
        }
        else if (actualValuePtr != NULL)
        {
            bp->setAudioOutputPortValue(handle, LEVEL, valuePtr);
            *((float*) actualValuePtr) = *((float*) valuePtr);
            returnValue = MPE_SUCCESS;
        }
        else
        {
            returnValue = MPE_SUCCESS;
        }
        break;

        //int - set of values
    case AUDIO_PORT_COMPRESSION_VALUE_ID:
        supportedValuesSize = audioInfo.supportedCompressionsSize;
        supportedValues = audioInfo.supportedCompressions;
        whichDiscreetValue = COMPRESSION;
        break;

    case AUDIO_PORT_ENCODING_VALUE_ID:
        supportedValuesSize = audioInfo.supportedEncodingsSize;
        supportedValues = audioInfo.supportedEncodings;
        whichDiscreetValue = ENCODING;
        break;

    case AUDIO_PORT_STEREO_MODE_VALUE_ID:
        supportedValuesSize = audioInfo.supportedStereoModesSize;
        supportedValues = audioInfo.supportedStereoModes;
        whichDiscreetValue = STEREO_MODE;
        break;

    default:
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_SOUND,
                "mpeos_sndSetAudioOutputPortValue:  unknown audio mode value! = %d \n",
                valueId);
        return MPE_EINVAL;
    }

    if (supportedValues != NULL)
    {
        int myValue = *((int32_t*) valuePtr);

        int valueIsValid = -1; // -1 == false
        int i = 0;
        for (i = 0; i < supportedValuesSize; i++)
        {
            if (myValue == supportedValues[i])
            {
                if (actualValuePtr != NULL)
                {
                    bp->setAudioOutputPortValue(handle, whichDiscreetValue,
                            valuePtr);
                    *((int32_t*) actualValuePtr) = myValue;
                }
                returnValue = MPE_SUCCESS;
                valueIsValid = 0; // 0 == true
                break;
            }
        }
        if (valueIsValid == -1)
        {
            returnValue = MPE_EINVAL; //value is not supported
        }
    }

    return returnValue;
}
