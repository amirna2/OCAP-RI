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

#ifndef _RI_CABLECARD_H_
#define _RI_CABLECARD_H_

#include <ri_types.h>

/**
 * CableCARD Generic Feature IDs
 * OC-SP-CCIF2.0 Table 9.15-2
 */
typedef enum ri_cablecard_generic_feature_enum
{
    RI_CCARD_GF_RF_OUTPUT_CHANNEL = 0x01,
    RI_CCARD_GF_PC_PIN = 0x02,
    RI_CCARD_GF_PC_SETTINGS = 0x03,
    RI_CCARD_GF_IPPV_PIN = 0x04,
    RI_CCARD_GF_TIME_ZONE = 0x05,
    RI_CCARD_GF_DAYLIGHT_SAVINGS = 0x06,
    RI_CCARD_GF_AC_OUTLET = 0x07,
    RI_CCARD_GF_LANGUAGE = 0x08,
    RI_CCARD_GF_RATING_REGION = 0x09,
    RI_CCARD_GF_RESET_PIN = 0x0A,
    RI_CCARD_GF_CABLE_URLS = 0x0B,
    RI_CCARD_GF_EA_LOCATION_CODE = 0x0C,
    RI_CCARD_GF_VCT_ID = 0x0D,
    RI_CCARD_GF_TURN_ON_CHANNEL = 0x0E,
    RI_CCARD_GF_TERM_ASSOC = 0x0F,
    RI_CCARD_GF_DOWNLOAD_GROUP_ID = 0x10,
    RI_CCARD_GF_ZIP_CODE = 0x11
} ri_cablecard_generic_feature;

/**
 * General CableCARD Events:
 *
 *   RI_CCARD_EVENT_CARD_INSERTED -- event_data = unused
 *       This event it sent when the CableCARD has been inserted.  Note:
 *       the card may not yet be ready to handle interactions until
 *       RI_CCARD_EVENT_CARD_READY is sent.
 *
 *   RI_CCARD_EVENT_CARD_REMOVED -- event_data = unused
 *       This event is sent when the CableCARD has been removed
 *
 *   RI_CCARD_EVENT_CARD_READY -- event_data = unused
 *       This event is sent when the CableCARD becomes ready
 *
 *   RI_CCARD_EVENT_GF_CHANGED -- event_data = ri_cablecard_generic_feature*
 *       This event is sent when a particular Generic Feature has been
 *       modified.  The event data contains the Generic Feature ID that
 *       changed.
 *
 *   RI_CCARD_EVENT_SAS_CONNECTION_AVAIL -- event_data = unused 
 *       This event is sent whenever a session is closed on the SAS resource.
 *
 */
typedef enum ri_cablecard_event_enum
{
    // Events sent to the main cablecard event handler function registered
    // with ri_cablecard_register_for_events()
    RI_CCARD_EVENT_CARD_INSERTED = 1,
    RI_CCARD_EVENT_CARD_REMOVED,
    RI_CCARD_EVENT_CARD_READY,
    RI_CCARD_EVENT_GF_CHANGED,
    RI_CCARD_EVENT_SAS_CONNECTION_AVAIL,

} ri_cablecard_event;

/**
 * Session-related CableCARD Events:
 *
 *   RI_CCARD_EVENT_APDU_RECV -- event_data = uint8_t*
 *       This event is sent when an APDU is received from the CableCARD.
 *       The event data points to an APDU as described in OC-SP-CCIF2.0 Section 9.3.
 *       After the caller has finished with the APDU data, it must be released 
 *       by calling ri_cablecard_release_data().
 *
 *   RI_CCARD_EVENT_SESSION_CLOSED -- event_data = unused
 *       This event is sent when an open session has been closed by the platform or
 *       CableCARD.  Expect no more APDU events for this session ID
 */
typedef enum ri_cablecard_session_event_enum
{
    // Events sent to the SAS and CAS session listeners
    RI_CCARD_EVENT_APDU_RECV = 1,
    RI_CCARD_EVENT_SESSION_CLOSED
} ri_cablecard_session_event;

/**
 * Basic CableCARD information, including applications located on the card.
 *
 * Most of this data comes from the application_info_cnf() APDU defined in
 * OC-SP-CCIF2.0 Table 9.5-4.
 */
typedef struct ri_cablecard_info_s
{
    uint16_t manuf_id;
    uint16_t version_number;

    uint8_t mac_address[6];

    uint8_t serialnum_length;
    uint8_t* card_serialnum;

    uint16_t app_data_length; // The length (in bytes) of the application_data byte array
    uint8_t* application_data; // The exact byte contents of the app loop from Table 9.5-4
    // (including the number_of_applications field)
    uint8_t max_programs;
    uint8_t max_elementary_streams;
    uint8_t max_transport_streams;

} ri_cablecard_info_t;

/**
 * Represents an open connection to a CableCARD resource
 */
typedef void* ri_session_id;

/**
 * CableCARD event callback function
 */
typedef void (*ri_cablecard_callback_f)(ri_cablecard_event event,
        void* event_data, void* cb_data);

/**
 * Callback associated with open card resource sessions.  Used to receive APDUs
 */
typedef void (*ri_cablecard_session_callback_f)(
        ri_cablecard_session_event event, ri_session_id sess, void* event_data,
        uint32_t data_length, void* cb_data);

/**
 * Registers the given callback function to receive events related to CableCARD
 * status and the Generic Features resource
 *
 * @param event_func the function that will receive CableCARD events
 * @param data callback data to be passed on every invocation of event_func
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT ri_error ri_cablecard_register_for_events(
        ri_cablecard_callback_f event_func, void* data);

/**
 * Returns whether or not the card is inserted
 *
 * @return TRUE if the card is inserted, FALSE otherwise
 */
RI_MODULE_EXPORT ri_bool ri_cablecard_is_inserted();

/**
 * Returns whether or not the card is inserted and ready
 *
 * @return TRUE if the card is inserted and ready, FALSE otherwise
 */
RI_MODULE_EXPORT ri_bool ri_cablecard_is_ready();

/**
 * Lookup a host by name
 *
 * @param name for lookup
 *
 * @return pointer to structure containing host entry or NULL on ERROR
 */
RI_MODULE_EXPORT struct hostent *ri_cablecard_gethostbyname(const char *name);

/**
 * Retrieve basic CableCARD information.  The returned pointer must be subsequently
 * passed to ri_cablecard_release_data() so that the platform may release allocated
 * resources associated with the info structure
 *
 * @param info the address of a pointer to a ri_cablecard_info_t structure that
 *        will hold the platform-allocated CableCARD information upon successful
 *        return
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT ri_error ri_cablecard_get_info(ri_cablecard_info_t** info);

/**
 * Returns the list of Generic Features supported by this CableCARD.  The returned
 * feature list pointer must be subsequently passed to ri_cablecard_release_data()
 * so that the platform may release allocated resources (feature_list only)
 *
 * @param feature_list the address of an array of generic features IDs that
 *        that will hold the platform-allocated list of generic features
 *        supported by this card upon successful return
 * @param num_features the address where the platform will return the length of
 *        the returned generic feature array      
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_get_supported_features(
        ri_cablecard_generic_feature** feature_list, uint8_t* num_features);

/**
 * Returns the generic feature associated with the given feature ID.  The returned
 * feature data poitner must be subsequently passed to ri_cablecard_release_data()
 * so that the platform may release allocated resources (feature_data only)
 *
 * @param feature_id the requested generic feature ID
 * @param feature_data the address of a byte buffer that will hold the generic
 *        feature data upon successful return
 * @param data_length the address where the platform will return the length of
 *        the returned generic feature data bufer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_GF_NOT_SUPPORTED: The given generic feature ID is not supported
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_get_generic_feature(
        ri_cablecard_generic_feature feature_id, uint8_t** feature_data,
        uint8_t* data_length);

/**
 * Sets the generic feature associated with the given feature ID.
 *
 * @param feature_id the desired generic feature ID
 * @param feature_data the generic feature data
 * @param data_length the length of the generic feature data buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_GF_NOT_SUPPORTED: The given generic feature ID is not supported
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_set_generic_feature(
        ri_cablecard_generic_feature feature_id, uint8_t* feature_data,
        uint8_t data_length);

/**
 * Opens a connection to the given method/dialog on the MMI resource.  The
 *  given callback function will be notified of asynchronous MMI events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_MMI_dialog(ri_session_id* session,
         int dialog, char *method,
         ri_cablecard_session_callback_f callback, void* cb_data);

/**
 * Close the given MMI dialog
 *
 * @param the session to be closed
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Invalid session ID
 */
ri_error ri_cablecard_close_MMI_dialog(int nextDialog, int dialog, int reason);

/**
 * Opens a connection to the only broadcast MMI resource.  The
 *  given callback function will be notified of asynchronous APDU events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_open_MMI_connection(ri_session_id* session,
         ri_cablecard_session_callback_f callback, void* cb_data);

/**
 * Close the only broadcast MMI connection
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Invalid session ID
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_close_MMI_connection(void);

/**
 * Opens a connection to the single Application Information resource.  The
 *  given callback function will be notified of asynchronous APDU events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_open_AI_connection(ri_session_id* session,
         ri_cablecard_session_callback_f callback, void* cb_data);

/**
 * Close the only Application Information resource connection
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Invalid session ID
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_close_AI_connection(void);

/**
 * Opens a connection to the given application ID on the SAS resource.  The given
 * callback function will be notified of asynchronous APDU events.
 *
 * @param session the address where the implementation will store the established
 *        session ID upon successful return
 * @param appID the 4-byte (big-endian) application ID
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_INVALID_SAS_APPID: Application ID not supported by card
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No more connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_open_SAS_connection(ri_session_id* session,
        uint8_t appID[8], ri_cablecard_session_callback_f callback,
        void* cb_data);

/**
 * Close the given SAS connection
 *
 * @param the session to be closed
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Invalid session ID
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_close_SAS_connection(ri_session_id session);

/**
 * Registers a callback to listen for APDU events on the Conditional Access Support
 * resource.  Only one callback may be registered at a time.  If a listener is
 * already registered, RI_CCARD_SESSION_CLOSED is sent to the that listener and
 * it is automatically replaced by the given listener.
 *
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_register_CAS_listener(
        ri_cablecard_session_callback_f callback, void* cb_data);

/**
 * Unregisters the given Conditional Access Support resource listener.  RI_CCARD_SESSION_CLOSED
 * event is not sent to the listener.
 *
 * @param callback the callback function to be unregistered
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_unregister_CAS_listener(
        ri_cablecard_session_callback_f callback);

/**
 * Opens a CFD socket flow based on the given application ID
 *
 * @param appID the 4-byte (big-endian) application ID
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_CFD(uint32_t appID);

/**
 * Closes a CFD socket flow based on the given application ID
 *
 * @param appID the 4-byte (big-endian) application ID
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_close_CFD(uint32_t appID);

/**
 * Retrieves a property value from the Host Addressable Properties resource.
 * The caller should pass the returned property value to ri_cablecard_release_data()
 * when it has finished copying/inspecting the data.
 *
 * @param name a null-terminated UTF-8 string describing the property name
 * @param value the memory location where the platform will return the property
 *        value upon successful return.  Value will be a null-terminated UTF-8
 *        string.  If the property name was not found, the returned value will
 *        be NULL and should not be passed to ri_cablecard_release_data()
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_get_addressable_property(const char* name, char** value);

/**
 * Sends an APDU to the CableCARD resource.  The APDU buffer is formatted
 * as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param session the APDU will be sent to this open session
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
RI_MODULE_EXPORT
ri_error ri_cablecard_send_APDU(ri_session_id session, uint8_t* apdu);

/**
 * Instructs the platform to release the resources allocated by any of the RI
 * Platform CableCARD module APIs defined in this file
 *
 * @param a pointer to the data allocated by the platform that should be released
 */
RI_MODULE_EXPORT void ri_cablecard_release_data(void* data);

#endif

