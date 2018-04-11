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
 * The MPE OS POD API. This API provides a consistent interface to POD functionality
 * regardless of the underlying operating system.
 *
 * @author Todd Earles - Vidiom Systems Corporation
 */

#ifndef _MPEOS_POD_H_
#define _MPEOS_POD_H_
#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_error.h>
#include <mpe_pod.h>
#include <mpeos_time.h>

#define MAX_UDP_DATAGRAM_SIZE (4 * 1024)

/****************************************************************************************
 *
 * TYPE AND STRUCTURE DEFINITIONS
 *
 ***************************************************************************************/

/****************************************************************************************
 *
 * POD FUNCTIONS
 *
 ***************************************************************************************/

/**
 * <i>mpeos_podInit()</i>
 *
 * Perform any target specific operations to enable interfacing with the POD
 * device via the target HOST-POD devices stack interface.  Depending on the platform
 * implementation this may include stack API call(s) to get the HOST-POD stack resources
 * initialized, or it may simply involve stack API calls(s) to access the data
 * already exchanged between the HOST and POD during the initial platform bootstrap
 * process.
 *
 * @param podDB is a pointer to the MPE layer platform-independent POD information
 *              database used to cache the POD-Host information.
 *
 * @return MPE_SUCCESS if the initialization process was successful.
 */
mpe_Error mpeos_podInit(mpe_PODDatabase *podDB);

/**
 * <i>mpeos_podRegister()</i>
 *
 * Handles registration for pod-specific asynchronous events. The platform-specific
 * layer is responsible for notifying the stack of asynchronous changes to the POD
 * generic features list or app info.  An asynchronous completion token and
 * queueID are provided to the native layer to facilitate this communication.
 *
 * For efficiency reasons there is only one queue between MPEOS and higher levels, so registering queue B when queue A is
 * still active is invalid and a MPE_EINVAL failure should be returned.
 *
 * These events are defined in mpe_PodEvent.  At this time the MPEOS is only required to send the following asynchronous events:
 *
 *            MPE_POD_EVENT_GF_UPDATE
 *            MPE_POD_EVENT_APPINFO_UPDATE
 *            MPE_POD_EVENT_INSERTED
 *            MPE_POD_EVENT_READY
 *            MPE_POD_EVENT_REMOVED
 *            MPE_POD_EVENT_RECV_APDU
 *
 * Other events may be defined in mpe_podEvent, but in the default MPE/MPEOS implementation those events are generated at the MPE layer.
 *
 * @param queueId is the event queue ID to which POD events should be sent
 * @param data is the EdHandle if needed (comm with JNI) or private queue data.
 *
 * @return MPE_SUCCESS if the registration process was successful.  Returns MPE_EINVAL if the previous queue has not been unregistered
 * or if one of the parameters is otherwise invalid.
 *
 */
mpe_Error mpeos_podRegister(mpe_EventQueue queueId, void* data);

/**
 * <i>mpeos_podUnregister()</i>
 *
 * Removes any previous event handler for pod-specific asynchronous events
 *
 * @return MPE_SUCCESS if the unregistration process was successful.
 */
mpe_Error mpeos_podUnregister(void);

/**
 * <i>mpeos_podGetFeatures</i>
 *
 * Get the POD's generic features list.
 *
 * @param podDB is a pointer to the MPE layer platform-independent POD information
 *              database used to cache the POD-Host information.
 *
 * @return MPE_SUCCESS if the features list was successfully acquired.
 */
mpe_Error mpeos_podGetFeatures(mpe_PODDatabase *podDB);

/**
 * <i>mpeos_podGetFeatureParam</i>
 *
 * Populate the internal POD database with the specified feature parameter value.
 *
 * @param featureId is the identifier of the feature parameter of interest.
 *
 * @return MPE_SUCCESS if the value of the feature was acquired successfully.
 */
mpe_Error mpeos_podGetFeatureParam(mpe_PODDatabase *podDBPtr,
        uint32_t featureId);

/**
 * <i>mpeos_podSetFeatureParam<i/>
 *
 * Perform actual Generic Feature parameter set operation to POD-HOST interface.
 * If the POD accepts the proposed change in value, return TRUE to call to indicate
 * successful set operation.
 *
 * @param featureId is the generic feature parameter to set
 * @param param is a pointer to the value of the generic feature
 * @param size is the size in bytes of the parameter value
 *
 * @return TRUE if the values was accepted by the POD.
 */
mpe_Error mpeos_podSetFeatureParam(uint32_t featureId, uint8_t *param,
        uint32_t size);

/**
 * The mpeos_podCASConnect() function shall establish a connection between a private
 * Host application and the POD Conditional Access Support (CAS) resource.  It is the MPEOS call's responsibility
 * determine the correct resource ID based upon whether the card is M or S mode.
 *
 * @param sessionId points to a location where the session ID can be returned. The session
 *          ID is implementation dependent and represents the CAS session to the POD application.
 * @param version a pointer to the location where the implementation will store the supported
 *                          version of the Conditional Access Support resource
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podCASConnect(uint32_t *sessionId, uint16_t *version);

/**
 * The mpeos_podCASClose() function provides an optional API for systems that may
 * require additional work to maintain session resources when an application unregisters
 * its handler from POD.  The implementation of this function may
 * need to: 1) update internal implementation resources, 2) make an OS API call to
 * allow the OS to update session related resources or 3) do nothing since it's
 * entirely possible that the sessions can be maintained as "connected" upon
 * deregistration and simply reused if the same host or another host application makes a
 * later request to connect to the CAS application.
 *
 * @param sessionId the session identifier of the target CAS session.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podCASClose(uint32_t sessionId);

/**
 * The mpeos_podSASConnect() function shall establish a connection between a private
 * Host application and the corresponding POD Module vendor-specific application.
 *
 * @param appId specifies a unique identifier of the private Host application.
 * @param sessionId points to a location where the session ID can be returned. The session
 *          ID is implementation dependent and represents the SAS session to the POD application.
 * @param version a pointer to the location where the implementation will store the supported
 *                          version of the Conditional Access Support resource
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podSASConnect(uint8_t *appId, uint32_t *sessionId, uint16_t *version);

/**
 * The mpeos_podSASClose() function provides an optional API for systems that may
 * require additional work to maintain session resources when an application unregisters
 * its handler from an SAS application.  The implementation of this function may
 * need to: 1) update internal implementation resouces, 2) make an OS API call to
 * allow the OS to update session related resources or 3) do nothing since it's
 * entirely possible that the sessions can be maintained as "connected" upon
 * deregistration and simply reused if the same host or another host application makes a
 * later request to connect to the SAS application.
 *
 * @param sessionId the session identifier of the target SAS session.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podSASClose(uint32_t sessionId);

/**
 * The mpeos_podMMIConnect() function shall establish a connection with the MMI
 * resource on the POD device.
 *
 * @param sessionId the session identifier of the target MMI session.
 * @param version a pointer to the location where the implementation will store the supported
 *                          version of the Man-Machine Interface resource
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podMMIConnect(uint32_t *sessionId, uint16_t *version);

/**
 * The mpeos_podMMIClose() function provides an optional API for systems that may
 * require additional work to maintain MMI resources when an application unregisters
 * its MMI handler from the MMI POD application.  The implementation of this function may
 * need to: 1) update internal implementation resouces, 2) make an OS API call to
 * allow the OS to update MMI session related resources or 3) do nothing since it's
 * entirely possible that the MMI sessions can be maintained as "connected" upon
 * deregistration.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podMMIClose(void);

/**
 * The mpeos_podAIConnect() function shall establish a connection with the
 * Application Information resource on the POD device.
 *
 * @param sessionId the session identifier of the target application information session.
 * @param version a pointer to the location where the implementation will store the supported
 *                          version of the Application Information resource
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podAIConnect(uint32_t *sessionId, uint16_t *version);

/**
 * The mpeos_podReceiveAPDU() function retrieves the next available APDU from the POD.
 * This function also will receive "copy" APDU's that failed to be sent to the POD so that
 * the APDU can be returned to the original sender to be notified of the failure.
 *
 * This API utilizes the definition of an APDU as defined by CCIF:
 *
 * APDU() {
 *   apdu_tag 24 uimsbf
 *   length_field()
 *   for (i=0; i<length_value; i++) {
 *     data_byte 8 uimsbf
 * }}
 *
 * mpeos_podReleaseAPDU() will release the APDU buffer returned by this function.
 *
 * @param sessionId location to store the native session handle associated with
 *                   the received APDU.
 * @param apdu location to store a pointer to the received APDU
 * @param len location to store the length of the APDU data, in bytes
 * @return Upon successful completion, this function shall return
 * <ul>
 * <li>     MPE_SUCCESS - The APDU was successfully retrieved, the associated session
 *                        ID was copied to *sessionId, a pointer to the buffer
 *                        was stored in *apdu, and the length of the buffer copied to *len
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * <li>     MPE_ENODATA - Indicates that the APDU received is actually the last APDU that
 *                       failed to be sent.
 * </ul>
 */
mpe_Error mpeos_podReceiveAPDU(uint32_t *sessionId, uint8_t **apdu,
        int32_t *len);

/**
 * The mpeos_podReleaseAPDU() function
 *
 * This will release an APDU retrieved via mpeos_podReceiveAPDU()
 *
 * @param apdu the APDU pointer retrieved via mpeos_podReceiveAPDU()
 * @return Upon successful completion, this function shall return
 * <ul>
 * <li>     MPE_EINVAL - The apdu pointer was invalid.
 * <li>     MPE_SUCCESS - The apdu was successfully deallocated.
 * </ul>
 */
mpe_Error mpeos_podReleaseAPDU(uint8_t *apdu);

/**
 * The mpeos_podSendAPDU() function sends an APDU packet.
 *
 * @param sessionId is the native handle for the SAS session for the APDU.
 * @param apduTag APDU identifier
 * @param length is the length of the data buffer portion of the APDU
 * @param apdu is a pointer to the APDU data buffer
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podSendAPDU(uint32_t sessionId, uint32_t apduTag,
        uint32_t length, uint8_t *apdu);

/**
 * The mpeos_podGetParam() function gets a resource parameter from the POD.
 *
 * @param paramId       MPE defined identifier.
 * @param paramValue    pointer to the parameter value.
 *
 * @return Upon successful completion, this function shall return MPE_SUCCESS. Otherwise,
 *          one of the errors below is returned.
 * <ul>
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_EINVAL - One or more parameters were out of range or unusable.
 * </ul>
 */
mpe_Error mpeos_podGetParam(mpe_podParamId paramId, uint32_t* paramValue);

/**
  * <i>mpeos_podStartCPSession<i/>
  *
  * Start the CP (Copy Protection) session for the specified service.
  *
  * This method will be called after the initiation of CA (a ca_pmt
  * sent via the CAS session) and will precede initiation of any
  * MPEOS functions which operate on encrypted content. (e.g.
  * mpeos_mediaDecode(), mpeos_dvrTsbBufferingStart(),
  * and mpeos_filterSetFilter() for in-band sources)
  *
  * @param tunerId that's tuned to the transport stream on which
  * the desired service is carried.
  *
  * @param programNumber the program_number of the associated
  * service.
  *
  * @param ltsid The Logical Transport Stream ID associated with
  * the CA resource (setup via the ca_pmt).
  *
  * @param ecmPid the ECM PID associated with the CableCARD
  * program to monitor.
  *
  * @param programIndex the ca_pmt program index. The program
  * index is used to uniquely identify a service when multiple
  * programs are being decrypted for the same transport stream.
  *
  * @param numPids the number of PIDs in the pids array.
  *
  * @param pids array of PIDs as supplied in the CA_PMT APDU
  * used to initiate the CA session.
  *
  * @return MPE_SUCCESS if the Copy Protection session is
  * successfully started for the identified program.
  */
mpe_Error mpeos_podStartCPSession( uint32_t tunerId,
                                   uint16_t programNumber,
                                   uint32_t ltsid,
                                   uint16_t ecmPid,
                                   uint8_t programIndex,
                                   uint32_t numPids,
                                   uint32_t pids[],
                                   mpe_PODCPSession * session );


/**
  * <i>mpeos_podStopCPSession<i/>
  *
  * Stop the CP (Copy Protection) session for the specified service.
  *
  * This method will be called after the termination of any MPEOS
  * functions which operate on encrypted content and precede the
  * termination of the ca_pmt session.
  *
  * @return MPE_SUCCESS if the Copy Protection session is
  * successfully stopped.
  */
mpe_Error mpeos_podStopCPSession(mpe_PODCPSession session);

#ifdef __cplusplus
}
#endif
#endif
