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

#ifndef _GLOBAL_TEST_CONFIG_
#define _GLOBAL_TEST_CONFIG_ 1

// Timeout for arbitrary operations
#define TEST_DEFAULT_TIMEOUT 10000	// 10 seconds
#define TEST_SETUP_ERROR (mpe_Error)0xCAFEBABE
#define TEST_TEARDOWN_ERROR (mpe_Error)0xCAFED00D

/*
 * Global tuning values
 */

// Frequencies, program numbers, and QAMs for testing as well
// These values correspond to the DVD analog channel
#define TEST_FREQUENCY_ANALOG 63000000
#define TEST_PROGRAM_NUMBER_ANALOG 0
#define TEST_QAM_MODE_ANALOG MPE_SI_MODULATION_QAM_NTSC

// These values correspond to a digital channel
#define TEST_FREQUENCY_DIGITAL_1 609000000
#define TEST_PROGRAM_NUMBER_DIGITAL_1 1
#define TEST_QAM_MODE_DIGITAL_1 MPE_SI_MODULATION_QAM256

// These values correspond to a digital channel
#define TEST_FREQUENCY_DIGITAL_2 609000000
#define TEST_PROGRAM_NUMBER_DIGITAL_2 2
#define TEST_QAM_MODE_DIGITAL_2 MPE_SI_MODULATION_QAM256

// Default tuner
// NOTE: This is a temporary value until bug 3293 is resolved
#define TEST_DEFAULT_TUNER 1

/*
 * Global decoding values
 */

// These PIDs should match the actual PIDs broadcast on the services
//   in the test digital freq/qam/mode values above. This will allow the test
//   to remain coherent by using similarly named constants.
#define TEST_PID_VIDEO_DIGITAL_1 0x45
#define TEST_PID_VIDEO_TYPE_DIGITAL_1 MPE_SI_ELEM_VIDEO_DCII
#define TEST_PID_AUDIO_DIGITAL_1 0x48
#define TEST_PID_AUDIO_TYPE_DIGITAL_1 MPE_SI_ELEM_ATSC_AUDIO

#define TEST_PID_VIDEO_DIGITAL_2 0x59
#define TEST_PID_VIDEO_TYPE_DIGITAL_2 MPE_SI_ELEM_VIDEO_DCII
#define TEST_PID_AUDIO_DIGITAL_2 0x5a
#define TEST_PID_AUDIO_TYPE_DIGITAL_2 MPE_SI_ELEM_ATSC_AUDIO

// This sets the default device type to 'video'
#define TEST_DEFAULT_DEVICE_TYPE MPE_DISPLAY_VIDEO_DEVICE

// This is used as an index into an array of screens
#define TEST_DEFAULT_SCREEN 0

/****************************************************************************
 *
 *  Thread test defines
 */

/* The following is never a valid value for a variable of type "mpe_ThreadId"  */

#define TEST_THREAD_INVALID_THREADID (0)

/*  test thread status values  */

#define TEST_THREAD_STAT_DEFAULT     (0)
#define TEST_THREAD_STAT_INVALID     (109)
#define TEST_THREAD_STAT_TEST_1      (207)
#define TEST_THREAD_STAT_TEST_2      (305)
#define TEST_THREAD_STAT_TEST_3      (403)

/****************************************************************************
 *
 *  Network test defines. Some of these will need to be changed for testing
 *  at different locations.
 *
 */

#define TEST_NET_NAMESERVER_IP      "192.168.158.5"   /*  IP address of DNS server  */

/*
 *  The following two #defines are a name and matching IP address used for lookup tests.
 *  They should be resolvable using the nameserver specified above.
 */

#define TEST_NET_HOSTIP             "192.160.73.62"    /*  IP address of "cablelabs.org"  */
#define TEST_NET_HOSTNAME           "cablelabs.org"       /*  name of "cablelabs.org"  */

#endif /*_GLOBAL_TEST_CONFIG_*/
