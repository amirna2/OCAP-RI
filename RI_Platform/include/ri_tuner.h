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

#ifndef _RI_TUNER_H_
#define _RI_TUNER_H_

#include <ri_types.h>

#define MAX_PID          0x1FFF         // highest PID number (NULL PID)
#define MAX_PIDS         32             // the most PIDs likely in a PAT or PMT
#define MAX_PID_STORAGE  (MAX_PID+1)    // the storage required for all PIDs

/**
 * Supported modulation modes
 */
typedef enum ri_tuner_modulation_mode_enum
{
    RI_MODULATION_UNKNOWN = 0,
    RI_MODULATION_QPSK,
    RI_MODULATION_BPSK,
    RI_MODULATION_OQPSK,
    RI_MODULATION_VSB8,
    RI_MODULATION_VSB16,
    RI_MODULATION_QAM16,
    RI_MODULATION_QAM32,
    RI_MODULATION_QAM64,
    RI_MODULATION_QAM80,
    RI_MODULATION_QAM96,
    RI_MODULATION_QAM112,
    RI_MODULATION_QAM128,
    RI_MODULATION_QAM160,
    RI_MODULATION_QAM192,
    RI_MODULATION_QAM224,
    RI_MODULATION_QAM256,
    RI_MODULATION_QAM320,
    RI_MODULATION_QAM384,
    RI_MODULATION_QAM448,
    RI_MODULATION_QAM512,
    RI_MODULATION_QAM640,
    RI_MODULATION_QAM768,
    RI_MODULATION_QAM896,
    RI_MODULATION_QAM1024,
    RI_MODULATION_QAM_NTSC = 255
// for analog mode
} ri_tuner_modulation_mode;

/**
 * Supported tuner events
 */
typedef enum ri_tuner_event_enum
{
    RI_TUNER_EVENT_FAIL, // Tuner failed to set demod parameters
    RI_TUNER_EVENT_NOSYNC, // Tuner is tuned but no signal lock
    RI_TUNER_EVENT_SYNC
// Tuner is tuned and signal lock
} ri_tuner_event;

/**
 * Data structure representing the tuning parameters associated with a
 * particular tune request.
 *
 * frequency:  Carrier frequency in Hz.  A value of zero requests a
 *         'detune', which will drop SignalLock and forward no stream data
 * mode:  Modulation mode from RI_MODULATION_xxx
 * program_num: Program number, or -1 when the entire transport stream
 *              is or is to be tuned
 */
typedef struct ri_tune_params_s
{
    uint32_t frequency;
    ri_tuner_modulation_mode mode;
    int32_t program_num;
} ri_tune_params_t;

/**
 * Data structure representing the tuning status of a particular tuner
 * No other data is valid unless ri_error indicates success, mode and
 * program_num are only valid if frequency is non-zero, and no
 * other signal_xxx data is valid unless signal_lock is true.
 *
 * ri_error: the result status of the request.
 * frequency: last frequency requested, 0 if never tuned or detuned
 * mode:  Modulation mode from RI_MODULATION_xxx
 * program_num: Program number, or -1 when the entire transport stream
 *              is or is to be tuned
 * signal_lock: true if tuner has PLL sync
 * signal_present: true if tuner has acquired signal
 * signal_level: RF level in dBMv
 * signal_level_quality: quality indicator, 0 - 100
 * signal_noise_ratio: S/N in dB
 */
typedef struct ri_tuner_status_s
{
    ri_error status;
    uint32_t frequency;
    ri_tuner_modulation_mode mode;
    int32_t program_num;
    ri_bool signal_lock;
    ri_bool signal_present;
    float signal_level;
    int signal_level_quality;
    float signal_noise_ratio;
} ri_tuner_status_t;

typedef struct ri_tuner_s ri_tuner_t;
typedef struct ri_tuner_data_s ri_tuner_data_t;

typedef void (*ri_tuner_event_cb_f)(ri_tuner_event state, void* cb_data);

/**
 * This structure represents a tuner on the host platform capable of tuning
 * to a live carrier frequency that is presenting an MPEG-2 transport stream
 */
struct ri_tuner_s
{
    /**
     * Request that this tuner attempt to tune using the given tuning params.
     * A token is returned that uniquely identifies this request.
     * This is a syncronous call for initiating the tune. If the initiation
     * succeeds (return value = RI_ERROR_NONE), an asynchronous event will be
     * delivered to registered event callbacks indicate the final result.
     * Only one tune request is allowed at a time.  Any tune request made
     * while a tune is in progress will return RI_ERROR_TUNE_IN_PROGRESS and
     * will be ignored.
     *
     * @param object The "this" pointer
     * @param tune_param The tuning parameters that detail this tune request
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*request_tune)(ri_tuner_t* object, ri_tune_params_t tune_param);

    /**
     * Request the current state of the tuner.
     *
     * @param object The "this" pointer
     * @param state_cb A pointer to an allocated ri_tune_state_t structure
     *        that the implementation will populate with tune state information
     * @return An error code detailing the success or failure of initiating
     * the request.
     */
    void (*request_status)(ri_tuner_t* object, ri_tuner_status_t* tuner_status);

    /**
     * Registers a callback function that will be notified of tuner events
     * Only one callback function can be registered to a tuner at a time.
     *
     * @param object The "this" pointer
     * @param event_cb The callback function that will ne notified of
     *        tune-related events
     * @return An error code detailing the success or failure of the operation
     */
    void (*register_tuner_event_cb)(ri_tuner_t* object,
            ri_tuner_event_cb_f event_cb, void* cb_data);

    /**
     * Unregisters a callback function that has been registered for tuner
     * events on this tuner.  If the given callback function pointer
     * is not the currently registered callback, this operation is ignored.
     *
     * @param object The "this" pointer
     * @param event_cb A callback function that's registered for this tuner
     * @return An error code detailing the success or failure of the operation
     */
    ri_error (*unregister_tuner_event_cb)(ri_tuner_t* object,
            ri_tuner_event_cb_f state_cb);

    /**
     * Add transport stream PID
     *
     * @param object The "this" pointer
     * @param pid the PID to add to the currently tuned transport stream
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*add_TS_pid)(ri_tuner_t* object, uint16_t pid);

    /**
     * Remove transport stream PID
     *
     * @param object The "this" pointer
     * @param pid the PID to remove from the currently tuned transport stream
     * @return An error code detailing the success or failure of the request.
     */
    ri_error (*remove_TS_pid)(ri_tuner_t* object, uint16_t pid);

    // Private tuner data
    ri_tuner_data_t* data;
};

#endif /* _RI_TUNER_H_ */
