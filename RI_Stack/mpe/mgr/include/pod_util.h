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
 * mpe_podImpl.h
 *
 *  Created on: July 27, 2009
 *  Author: Alan Cossitt
 */

#ifndef MPE_PODIMPL_H_
#define MPE_PODIMPL_H_

#include <mpe_pod.h>

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * Tag definition from CCIF-2.0-I18
 */
#define APDU_TAG_SIZE               (3)         /* 3 BYTES */
#define CA_PMT_HEADER_LENGTH        (14)
#define CA_PMT_OK_DESCRAMBLE_CMD    (1)
#define CA_PMT_UNKNOWN_PROGRAM_NUM  (0)
#define CA_PMT_UNKNOWN_SOURCE_ID    (0)

/*CA PMT Command ID required to send pmt to the POD stack*/
typedef enum
{
	MPE_MPOD_CA_OK_DESCRAMBLE = 0x01,
	MPE_MPOD_CA_OK_MMI = 0x02,
	MPE_MPOD_CA_QUERY = 0x03,
	MPE_MPOD_CA_NOT_SELECTED = 0x04
} MPE_MPOD_CA_PMT_CMD_ID;

#define CA_INFO_INQUIRY_TAG         (0x9F8030)
#define CA_INFO_TAG                 (0x9F8031)
#define CA_PMT_TAG                  (0x9F8032)
#define CA_PMT_REPLY_TAG            (0x9F8033)
#define CA_PMT_UPDATE_TAG           (0x9F8034)

#define CA_ENABLE_NO_VALUE                      (0x0)
#define CA_ENABLE_SET                           (0x80)
#define CA_ENABLE_DESCRAMBLING_NO_CONDITIONS    (0x1)
#define CA_ENABLE_DESCRAMBLING_WITH_PAYMENT     (0x2)
#define CA_ENABLE_DESCRAMBLING_WITH_TECH        (0x3)
#define CA_ENABLE_DESCRAMBLING_PAYMENT_FAIL     (0x71)
#define CA_ENABLE_DESCRAMBLING_TECH_FAIL        (0x73)

/**
 * <i>mpe_podImplInit()</i>
 *
 * Designed to be called from the podmgr code.  Does any initialization that should be done before any
 * of the other methods are called.
 *
 * @return MPE_SUCCESS if the initialization process was successful.
 *
 */
mpe_Error mpe_podImplInit(void);

/**
 * <i>mpe_podImplRegister()</i>
 *
 * Handles registration for pod-specific asynchronous events from JNI down to the MPE layer.  This code relays events
 * received from MPEOS to the JNI layer.  The code also handles keeping track of the state of the POD based upon
 * the events sent.  There can only one event queue at a time
 *
 * The register will add a mpeos level event handler.
 *
 * Any of the events defined in mpe_PodEvent can be sent.
 *
 * @param jniEdHandle is the edHandle representing the queue to which POd events should be sent
 *
 * @return MPE_SUCCESS if the registration process was successful.
 */
mpe_Error mpe_podImplRegister(mpe_EdEventInfo *jniEdHandle);

/**
 * <i>mpeos_podImplUnregister()</i>
 *
 * Removes any previous JNI level event handler for pod-specific asynchronous events.  It will also remove the MPEOS
 * level event handler
 *
 * @return MPE_SUCCESS if the unregistration process was successful.
 */
mpe_Error mpe_podImplUnregister(void);

/**
 * The mpe_podImplReceiveAPDU() function retrieves the next available APDU from the POD.
 * This function also will receive "copy" APDU's that failed to be sent to the POD so that
 * the APDU can be returned to the original sender to be notified of the failure.
 *
 * @param sessionId is a pointer for returning the native session handle associated with
 *          the received APDU.
 * @param apdu the next available APDU from the POD
 * @param len the length of the APDU in bytes
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * <li>     MPE_ENODATA - Indicates that the APDU received is actually the last APDU that
 *                       failed to be sent.
 * </ul>
 */
mpe_Error mpe_podImplReceiveAPDU(uint32_t *sessionId, uint8_t **apdu,
        int32_t *len);

/**
 * <i>mpe_podImplStartDecrypt()</i>
 *
 * Tells the CableCARD to start decrypting the streams represented by decodeRequest
 *
 * @param decodeRequest   decode request params
 * @param queueId         the queue to which decrypt session events should be sent
 * @param act             usually the edHandle (if caller is at JNI level)
 * @param handle          a session handle used to represent the session
 *
 * @return MPE_SUCCESS, valid session handle      If a session is successfully created
 *         MPE_SUCCESS, NULL session handle       If analog or clear channel
 *
 */
mpe_Error mpe_podImplStartDecrypt(
        mpe_PodDecryptRequestParams* decryptRequestPtr,
        mpe_EventQueue queueId, void *act,
        mpe_PODDecryptSessionHandle* sessionHandlePtr);

/**
 * <i>mpe_podImplStopDecrypt()</i>
 *
 * Tells the POD to stop decrypting the stream represented by the sessionHandle.  When this method returns, the
 * session represented by the sessionHandle is no longer valid.  Since this initiated by caller, no session lost or
 * other events are sent.
 *
 * @param sessionHandle  a session handle used to represent the session
 *
 * @return MPE_SUCCESS  if the unregistration process was successful.
 *         MPE_EINVAL   if the handle does not represent an existing session or is NULL
 */
mpe_Error mpe_podImplStopDecrypt(mpe_PODDecryptSessionHandle sessionHandle);

/**
 * <i>mpe_podImplGetDecryptStreamStatus()</i>
 *
 * Gets the per PID stream authorization status for decrypt session represented by the
 * decrypt session handle (returned from mpe_podImplStartDecrypt).
 *
 * @param handle   session handle returned by mpe_podImplStartDecrypt
 * @param numStreams the number of streams the info is being requested for
 * @param streamInfo  array of streamInfo data structures, to be filled in by the PODMgr
 *
 * @return MPE_SUCCESS if the handle represents a session.
 */
mpe_Error mpe_podImplGetDecryptStreamStatus(mpe_PODDecryptSessionHandle handle, uint8_t numStreams, mpe_PODStreamDecryptInfo streamInfo[]);

/* TODO, TODO_POD:  method comments */
mpe_Error podImplRegisterMPELevelQueueForPodEvents(mpe_EventQueue queueId);
mpe_Error podImplUnregisterMPELevelQueueForPodEvents(mpe_EventQueue queueId);

#ifdef __cplusplus
}
;
#endif

#endif /* MPE_PODIMPL_H_ */
