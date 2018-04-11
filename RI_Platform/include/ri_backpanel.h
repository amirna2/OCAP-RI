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

/*
 * ri_backpanel.h
 *
 *  Created on: October 7, 2009
 *      Author: Chris Sweeney
 */

#ifndef _RI_BACKPANEL_H_
#define _RI_BACKPANEL_H_

// Include RI Platform header files.
#include <ri_types.h>

typedef enum
{
    AUDIO_OUTPUT_PORT_ID,
    COMPRESSION,
    GAIN,
    ENCODING,
    LEVEL,
    STEREO_MODE,
    LOOP_THRU,
    MUTED,
    OPTIMAL_LEVEL,
    MAX_DB,
    MIN_DB,
    LOOP_THRU_SUPPORTED
} ri_audio_output_port_cap;

typedef enum
{
    VIDEO_OUTPUT_PORT_ID,
    VIDEO_OUTPUT_PORT_TYPE,
    VIDEO_OUTPUT_ENABLED,
    VIDEO_OUTPUT_CONNECTED,
    VIDEO_OUTPUT_AUDIO_PORT,
    VIDEO_OUTPUT_DTCP_SUPPORTED,
    VIDEO_OUTPUT_HDCP_SUPPORTED,
    VIDEO_OUTPUT_RESOLUTION_RESTRICTION
} ri_video_output_port_cap;

/**
 * The RI Platform Back Panel object type.
 */
typedef struct ri_backpanel_s ri_backpanel_t;

/**
 * The RI Platform Back Panel data type.
 */
typedef struct ri_backpanel_data_s ri_backpanel_data_t;

/**
 * The RI Platform Back Panel Object definition.
 */
struct ri_backpanel_s
{
    // Back Panel APIs

    /**
     * This method is used to obtain the list of ports supported by the
     * Back Panel.
     *
     * @return  The number of output ports (audio and video) supported by the back panel.
     *          This is also the size of the returned array of names.
     *
     * @param   ids   The array of the names of the supported output ports
     *                  for the back panel.  This array is allocated and
     *                  controlled within the RI Platform, and is not to be modified
     *                  by the caller.  MUST NOT BE NULL.
     *
     * @remarks This routine is used to find the number and names of the
     *          audio output ports available in the back panel.
     *
     * @warning The returned array MUST be freed using freeAudioOutputPortList().
     */
    uint8_t (*getAudioOutputPortNameList)(char*** ids);

    /**
     * This method is used to free the memory allocated by
     * getOutputPortList().
     *
     * @param   ids   The array of char* passed out by getAudioOutputPortList().
     *
     * @warning This method MUST be called for each call to
     *          getAudioOutputPortList().
     */
    void (*freeAudioOutputPortNameList)(char** ids);

    /*
     * This method provides a handle for named audio output port.
     */

    void* (*getAudioOutputPortHandle)(char* id);

    /*
     * Methods to get/set audio output port values.
     *
     * @remarks value returned/passed in may be a pointer to an int or float, depending on cap.
     */
    void (*getAudioOutputPortValue)(void* handle, ri_audio_output_port_cap cap,
            void* value);
    void (*setAudioOutputPortValue)(void* handle, ri_audio_output_port_cap cap,
            void* value);

    /*
     * Methods to query for audio output port limits
     *
     * @remarks returns the number of elements in the array.
     */
    uint8_t (*getAudioOutputPortSupportedCompressions)(void* handle,
            int** array);
    uint8_t (*getAudioOutputPortSupportedEncodings)(void* handle, int** array);
    uint8_t
            (*getAudioOutputPortSupportedStereoModes)(void* handle, int** array);

    /**
     * This method is used to obtain the list of video ports supported by the
     * Back Panel.
     *
     * @return  The number of video output ports supported by the back panel.
     *          This is also the size of the returned array of names.
     *
     * @param   ids   The array of the names of the supported output ports
     *                  for the back panel.  This array is allocated and
     *                  controlled within the RI Platform, and is not to be modified
     *                  by the caller.  MUST NOT BE NULL.
     *
     * @remarks This routine is used to find the number and names of the
     *          video output ports available in the back panel.
     *
     * @warning The returned array MUST be freed using freeVideoOutputPortList().
     */
    uint8_t (*getVideoOutputPortNameList)(char*** ids);

    /**
     * This method is used to free the memory allocated by
     * getVideoOutputPortList().
     *
     * @param   ids   The array of char* passed out by getAudioOutputPortList().
     *
     * @warning This method MUST be called for each call to
     *          getVideoOutputPortList().
     */
    void (*freeVideoOutputPortNameList)(char** ids);

    /*
     * This method provides a handle for named audio output port.
     */

    void* (*getVideoOutputPortHandle)(char* id);

    /*
     * Methods to get/set video output port values.
     *
     * @remarks value returned/passed in may be a pointer to an int or float, depending on cap.
     */
    void (*getVideoOutputPortValue)(void* handle, ri_video_output_port_cap cap,
            void* value);
    void (*setVideoOutputPortValue)(void* handle, ri_video_output_port_cap cap,
            void* value);

    /*
     * Method to register video output port connect/disconnect callback.
     */
    void (*setVideoOutputPortConnectDisconnectCallback)(void (*cb)(ri_bool connected, void* videoOutputPortHandle));

    // Back Panel data.
    ri_backpanel_data_t *ri_backpanel_data;
};

#endif /* _RI_BACKPANEL_H_ */
