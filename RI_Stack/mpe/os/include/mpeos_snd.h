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

#ifndef _MPEOS_SND_H
#define _MPEOS_SND_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_types.h>
#include <mpe_ed.h>
#include <mpeos_event.h>

typedef enum
{
    MPE_SND_ERROR_NO_ERROR = MPE_SUCCESS,
    MPE_SND_ERROR_EMIME,
    MPE_SND_ERROR_ERES
} mpe_SndError;

/* Sound related events */
typedef enum
{
    MPE_SND_EVENT_COMPLETE = 0x2000, /* playback completed successfully */
    MPE_SND_EVENT_ERROR
/* error occurred during playback */
} mpe_SndEvent;

/**
 * Sound handle
 * Serves as an abstraction to a sound object that contains sound data and type
 * information.
 */
typedef struct
{
    int unused;
}*mpe_SndSound;

/**
 * Sound device handle
 * Serves as an abstraction to a device for initiating playback of a sound.  A
 * device may only support playback of a limited number of playbacks and sound
 * types.
 */
typedef struct
{
    int unused;
}*mpe_SndDevice;

/**
 * Sound playback handle
 * Serves as an abstraction for an ongoing playback on a mpe_SndDevice.
 */
typedef struct
{
    int unused;
}*mpe_SndPlayback;

/**
 * audio port handle
 * Serves as an abstraction for an audio output port info
 */
typedef struct
{
    int unused;
}*mpe_SndAudioPort;

/**
 * Structure that provides the information on a sound object.  Pointed to by a
 * mpe_SndSound handle.
 */
typedef struct
{
    uint8_t *data;
    uint32_t size;
    char *type;
} mpeos_SndSound;

/**
 * Structure which provides fields for describing an audio output port.
 *
 * @see #mpeos_sndGetAudioOutputPortInfo
 */
typedef enum
{
    AUDIO_PORT_COMPRESSION_VALUE_ID = 0,
    AUDIO_PORT_GAIN_VALUE_ID,
    AUDIO_PORT_ENCODING_VALUE_ID,
    AUDIO_PORT_LEVEL_VALUE_ID,
    AUDIO_PORT_OPTIMAL_LEVEL_VALUE_ID,
    AUDIO_PORT_MAX_DB_VALUE_ID,
    AUDIO_PORT_MIN_DB_VALUE_ID,
    AUDIO_PORT_STEREO_MODE_VALUE_ID,
    AUDIO_PORT_LOOP_THRU_VALUE_ID,
    AUDIO_PORT_MUTED_VALUE_ID,
} mpe_SndAudioValueId;

typedef struct
{
    /**
     * Unique identification for this port
     */
    const char* idString;

    /**
     * Level of compression.  Must be one of the following values:
     * AudioOutputPort -- COMPRESSION_NONE, COMPRESSION_LIGHT, COMPRESSION_MEDIUM, COMPRESSION_HEAVY
     */
    int32_t compression; /* valueId == 0 */
    /**
     * gain in DB
     */
    float gain; /* valueId == 1 */
    /**
     * Type of audio encoding.  Must be one of the following values:
     * AudioOutputPort -- ENCODING_NONE, ENCODING_DISPLAY, ENCODING_PCM, ENCODING_PCM
     */
    int32_t encoding; /* valueId == 2 */
    /**
     * The current gain set for this AudioOutputPort
     * Is a value between 0.0 and 1.0.
     */
    float level; /* valueId == 3 */
    /**
     * The gain level that is optimal for stereo playback.
     * Javadoc is not clear, it appears this is a value between 0.0 and 1.0.
     */
    float optimalLevel; /* valueId == 4 */
    /**
     * The maximum gain in decibels.  Values more than this will have no effect.
     */
    float maxDb; /* valueId == 5 */
    /**
     * The minimum gain in decibels.  Values less than this will have no effect.
     */
    float minDb; /* valueId == 6 */
    /**
     * stereo mode.  Must be one of the following values:
     * AudioOutputPort -- STEREO_MODE_MONO, STEREO_MODE_STEREO, STEREO_MODE_SURROUND
     */
    int32_t stereoMode; /* valueId == 7 */
    /**
     * All compressions supported.
     */
    int32_t supportedCompressionsSize;
    int32_t* supportedCompressions;

    /**
     * All encodings supported
     */
    int32_t supportedEncodingsSize;
    int32_t* supportedEncodings;
    /**
     * All stereo modes supported.
     */
    int32_t supportedStereoModesSize;
    int32_t* supportedStereoModes;
    /**
     * True if in loopThrough mode
     */
    mpe_Bool loopThru; /* valueId == 8 */
    /**
     * True if muted
     */
    mpe_Bool muted; /* valueId == 9 */

} mpe_SndAudioOutputPortInfo;

/**
 * <i>mpeos_sndInit()</i>
 *
 * Initializes platform specific sound support.
 *
 */
void mpeos_sndInit(void);

/**
 * <i>mpeos_sndGetDeviceCount()</i>
 *
 * Returns the number of sound devices supported by the port.
 *
 * @param count uint32_t pointer that will be assigned the number of devices
 *              supported by the port.
 *
 * @return MPE_EINVAL if the parameter is NULL, otherwise MPE_SUCCESS.
 */
mpe_Error mpeos_sndGetDeviceCount(uint32_t *count);

/**
 * <i>mpeos_sndGetDevices()</i>
 *
 * Returns an array of sound device handles supported by the port.
 *
 * @param dev   mpe_SndDevice array that will contain handles of the supported
 *              devices.
 * @param count pointer to a uint32_t that contains the number of elements in the
 *              dev array on input and is set to the number of devices assigned
 *              on return.
 *
 * @return MPE_EINVAL if a parameter is NULL, otherwise MPE_SUCCESS.
 */
mpe_Error mpeos_sndGetDevices(mpe_SndDevice *devs, uint32_t *count);

/**
 * <i>mpeos_sndGetMaxPlaybacks()</i>
 *
 * Returns the maximum number of simultaneous playbacks supported for a device.
 *
 * @param dev          sound device to get the playbacks for.
 * @param maxPlaybacks pointer to an int32_t that is assigned the number of
 *                     simultaneous playbacks.  If unlimited playbacks are
 *                     supported, -1 is assigned.
 *
 * @return MPE_EINVAL if a parameter is NULL, otherwise MPE_SUCCESS.
 */
mpe_Error mpeos_sndGetMaxPlaybacks(mpe_SndDevice dev, int32_t *maxPlaybacks);

/**
 * <i>mpeos_sndGetDevicesForSound()</i>
 *
 * Returns the sound devices that can play a sound, sorted in order of preference.
 *
 * @param sound      sound handle to request playbacks for.
 * @param devices    array pointer that is assigned the prioritized list of
 *                   sound device handles that can play the sound
 * @param count      pointer to an uint32_t that contains the number of elements
 *                   in the devices array on input and is set to the number of
 *                   assigned elements on return.
 *
 * @return MPE_EINVAL if a parameter is NULL, otherwise MPE_SUCCESS.
 */
mpe_Error mpeos_sndGetDevicesForSound(mpe_SndSound sound,
        mpe_SndDevice *devices, uint32_t *count);

/**
 * <i>mpeos_sndCreateSound()</i>
 *
 * Creates a sound object.
 *
 * mpeos_sndCreateSound is required to make a copy of 'data'.
 *
 *
 * @param   type    MIME type of the sound data
 * @param   data    sound data to be played
 * @param   offset  byte offset into the sound data where playback should start
 * @param   size    number of bytes of sound data to copy
 * @param   sound   pointer to a mpe_SndSound
 *
 * @return  MPE_SND_ERROR_EMIME if type is not supported.
 * @return  MPE_ENOMEM if memory cannot be allocated
 * @return  MPE_SUCCESS if the mpe_SndSound was created successfully.
 */
mpe_Error mpeos_sndCreateSound(const char *type, const char *data,
        uint32_t offset, uint32_t size, mpe_SndSound *sound);

/**
 * <i>mpeos_sndDelete()</i>
 *
 * Release any resources allocated with a call to mpeos_sndCreateSound().
 *
 * @param sound handle to release resources
 *
 * @return MPE_EINVAL if the sound handle is NULL, otherwise MPE_SUCCESS.
 */
mpe_Error mpeos_sndDeleteSound(mpe_SndSound sound);

/**
 * <i>mpeos_sndPlay()</i>
 *
 * Plays the sound data using the device specified by device.
 *
 * @param   device        handle of the sound device to start playback
 * @param   sound         handle of sound to play
 * @param   handle        event dispatch handle to use for delivering native
 *                          events back to Java.
 * @param   start         media start time in nanoseconds
 * @param   loop          set to TRUE if sound is to be played in loop mode
 * @param   muted         Initial mute state
 * @param   requestedGain initial gain
 * @param   actualGain    pointer to a float that will be set with the actual gain
 * @param   playback      pointer that will be assigned to a valid playback
 *                          handle if successful.
 *
 * @return MPE_EINVAL if any of the handles or pointers are NULL.
 * @return MPE_ENOMEM if memory cannot be allocated.
 * @return MPE_SND_ERROR_ERES if native resources cannot be obtained.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndPlay(mpe_SndDevice device, mpe_SndSound sound,
        mpe_EdHandle handle, int64_t start, mpe_Bool loop,
        mpe_Bool muted, float requestedGain, float *actualGain,
        mpe_SndPlayback *playback);

/**
 * <i>mpeos_sndStop()</i>
 *
 * Stop the playback of a sound and release all resources.
 *
 * @param playback handle to the in-progress playback.
 * @param time     pointer to an int64_t that is assigned the media time in
 *                 nanoseconds when the stop occurred.  If playback is already
 *                 stopped, -1 is returned.
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndStop(mpe_SndPlayback playback, int64_t *time);

/**
 * <i>mpeos_sndSetTime()</i>
 *
 * Set a new media time in nanoseconds.
 *
 * @param playback handle to the in-progress playback.
 * @param time     pointer to an int64_t containing the media time to set.  On
 *                 return, the variable is assigned the actual media time that
 *                 was set. (which may be different than what was requested).
 *                 If the playback has already terminated, then assigned -1.
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndSetTime(mpe_SndPlayback playback, int64_t *time);

/**
 * <i>mpeos_sndGetTime()</i>
 *
 * Get the current media time, in nanoseconds.
 *
 * @param playback the handle of the playback to get the current time.
 * @param time     pointer to an int64_t which will be assigned the current media
 *                 time
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndGetTime(mpe_SndPlayback playback, int64_t *time);

/**
 * <i>mpeos_sndSetMute()</i>
 *
 * Set the current mute state. Audio must be muted if mute is TRUE
 * regardless of content format and the gain value must be unaffected.
 * When FALSE, audio must be restored and gain must be set to the
 * pre-mute level.
 *
 * @param playback the handle of the playback to set the mute setting.
 * @param mute     an mpe_Bool representing the new mute setting
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndSetMute(mpe_SndPlayback playback, mpe_Bool mute);

/**
 * <i>mpeos_sndSetGain()</i>
 *
 * Set the current decibel gain for the supplied playback session.
 * Positive values amplify the audio signal and negative values
 * attenuate the signal.
 *
 * If the platform does not support the setting of the gain level for the
 * presenting content format, actualGain should be returned as 0.0.
 *
 * Subsequent calls to mpeos_sndGetGain() must return the same value
 * returned in *actualLevel.
 *
 * @param playback the handle of the playback to set the gain
 * @param gain     a float representing the new gain, in decibels
 * @param actualGain pointer to a float which will be assigned the new gain setting
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndSetGain(mpe_SndPlayback playback, float gain, float *actualGain);

/**
 * <i>mpeos_sndAddAudioOutputPort()</i>
 *
 * Get the information structure for an Audio Port.
 *
 * DSExt function
 *
 * NOT IMPLEMENTED
 *
 * @param device the handle of a sound device
 * @param info   the port to associate with the sound device
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndAddAudioOutputPort(mpe_SndDevice device,
        mpe_SndAudioPort port);

/**
 * <i>mpeos_sndRemoveAudioOutputPort()</i>
 *
 * Get the information structure for an Audio Port.
 *
 * DSExt function
 *
 * NOT IMPLEMENTED
 *
 * @param device the handle of a sound device
 * @param info   the port to disassociate with the sound device
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndRemoveAudioOutputPort(mpe_SndDevice device,
        mpe_SndAudioPort port);

/**
 * <i>mpeos_sndGetAudioOutputPortInfo()</i>
 *
 * Get the information structure for an Audio Port.
 *
 * DSExt function
 *
 * NOT IMPLEMENTED
 *
 * @param handle the handle of the audio port
 * @param info   pointer to the info structure
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndGetAudioOutputPortInfo(mpe_SndAudioPort handle,
        mpe_SndAudioOutputPortInfo* info);

/**
 * <i>mpeos_sndSetAudioOutputPortValue()</i>
 *
 * Get a value in an Audio Port.
 *
 * DSExt function
 *
 * NOT IMPLEMENTED
 *
 * @param handle            the handle of the audio port
 * @param valueId           a value such as AUDIO_PORT_COMPRESSION_VALUE_ID that represents a value
 * @param requestedValuePtr the requested value
 * @param actualValuePtr    contents of ptr is set to the value set in MPEOS.  MPEOS may not allow
 *                          the requested value.  If NULL, this is ignored.
 *
 * @return MPE_EINVAL if any parameter is NULL.
 * @return MPE_SUCCESS if successful.
 */
mpe_Error mpeos_sndSetAudioOutputPortValue(mpe_SndAudioPort handle,
        int32_t valueId, void* requestedValuePtr, void* actualValuePtr);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_SND_H */

