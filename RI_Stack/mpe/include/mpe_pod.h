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

#ifndef _MPE_POD_H_
#define _MPE_POD_H_

#include <mpe_media.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_sys.h>
#include <mpeos_sync.h>
#include <mpeos_event.h>
#include <mpe_ed.h>
#include "../mgr/include/mgrdef.h"


/* POD Generic Feature Identifiers per CCIF Table 9.15-2 */
#define MPE_POD_GF_RF_OUTPUT_CHANNEL 0x1  // rf output channel
#define MPE_POD_GF_P_C_PIN           0x2  // parental control pin
#define MPE_POD_GF_P_C_SETTINGS      0x3  // parental control settings
#define MPE_POD_GF_IPPV_PIN          0x4  // ippv pin
#define MPE_POD_GF_TIME_ZONE         0x5  // time zone
#define MPE_POD_GF_DAYLIGHT_SAVINGS  0x6  // daylight savings control
#define MPE_POD_GF_AC_OUTLET         0x7  // ac outlet
#define MPE_POD_GF_LANGUAGE          0x8  // language
#define MPE_POD_GF_RATING_REGION     0x9  // rating region
#define MPE_POD_GF_RESET_P_C_PIN     0xa  // reset pin
#define MPE_POD_GF_CABLE_URLS        0xb  // cable urls
#define MPE_POD_GF_EA_LOCATION       0xc  // EAS location code
#define MPE_POD_GF_VCT_ID            0xd  // emergency alert location code
#define MPE_POD_GF_TURN_ON_CHANNEL   0xe  // turn-on channel
#define MPE_POD_GF_TERM_ASSOC        0xf  // terminal association
#define MPE_POD_GF_DOWNLOAD_GRP_ID   0x10 // download group ID
#define MPE_POD_GF_ZIP_CODE          0x11 // zip code

/* POD Events */
typedef enum
{
    MPE_POD_EVENT_NO_EVENT = 0x9000,
    MPE_POD_EVENT_GF_UPDATE, /* Generic Feature values updated */
    MPE_POD_EVENT_APPINFO_UPDATE, /* App Info updated */
    MPE_POD_EVENT_INSERTED, /* CableCard inserted into slot */
    MPE_POD_EVENT_READY, /* CableCard is ready for use */
    MPE_POD_EVENT_REMOVED, /* CableCard removed from slot */
    MPE_POD_EVENT_RECV_APDU, /* Message is available to a mpe_podReceiveAPDU call. optionalEventData1 == sessionID of APDU. */

    /*
     * Note that the mpe_podReceiveAPDU also receives APDU's that failed to be sent to the POD.  This is so that
     * the APDU can be returned to the original sender so they can be notified of the failure.
     *
     * MPEOS will send a APDU_FAILURE message when there is a failed SEND message available to the RECEIVE function.
     */
    MPE_POD_EVENT_SEND_APDU_FAILURE,
    MPE_POD_EVENT_RESOURCE_AVAILABLE, /* resource has been freed and is available to an interested party */
    MPE_POD_EVENT_SHUTDOWN, /* close down the queue */
    MPE_POD_EVENT_RESET_PENDING /* A CableCard reset will start shortly
                                   (5 seconds or more). POD resource use should
                                   be discontinued until MPE_POD_EVENT_READY
                                   is received. */
} mpe_PodEvent;

/**
 * POD Decrypt Session Events - Events sent by a decrypt session
 *
 * Note: In all cases, only one event is generated by ca_pmt_reply/update.
 * If the program/streams are signaled with different values of CA_enable,
 * the rules corresponding to the event are evaluated in the order listed
 * below (per OCAP 1.1.3 16.2.1.7 Figure 16–1).
 *
 * For all events:
 *   OptionalEventData1: Decrypt session handle (mpe_PODDecryptSessionHandle)
 *   OptionalEventData2: data/act value from mpe_podStartDecrypt (queue registration)
 *   OptionalEventData3: message-specific value
 */
typedef enum
{
    /**
     * One or more streams in the decrypt request cannot be descrambled due to
     * inability to purchase (ca_pmt_reply/update with CA_enable of 0x71).
     *
     * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
     */
    MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_ENTITLEMENT = 0x9271,

    /**
     * One or more streams in the decrypt request cannot be descrambled due to
     * lack of resources (ca_pmt_reply/update with CA_enable of 0x73).
     *
     * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
     */
    MPE_POD_DECRYPT_EVENT_CANNOT_DESCRAMBLE_RESOURCES = 0x9273,

    /**
     * One or more streams in the decrypt request required MMI interaction
     * for a purchase dialog (ca_pmt_reply/update CA_enable of 0x02) and user
     * interaction is now in progress.
     *
     * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
     */
    MPE_POD_DECRYPT_EVENT_MMI_PURCHASE_DIALOG = 0x9202,

    /**
     * One or more streams in the decrypt request required MMI interaction
     * for a technical dialog (ca_pmt_reply/update CA_enable of 0x03) and user
     * interaction is now in progress.
     *
     * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
     */
    MPE_POD_DECRYPT_EVENT_MMI_TECHNICAL_DIALOG = 0x9203,

    /**
     * All streams in the decrypt request can be descrambled (ca_pmt_reply/update
     *  with CA_enable of 0x01).
     * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
     */
    MPE_POD_DECRYPT_EVENT_FULLY_AUTHORIZED = 0x9201,

   /**
     * The session is terminated. All resources have been released.
     * No more events will be received on the registered queue/ed handler.
     * Only induced as a response to mpe_podStopDecrypt().
     */
    MPE_POD_DECRYPT_EVENT_SESSION_SHUTDOWN = 0x9300,

    /**
     * CableCard removed. The session is terminated and will not recover.
     */
    MPE_POD_DECRYPT_EVENT_POD_REMOVED = 0x9301,

    /**
     * The CableCard resources for this session have been lost to a higher priority session.
     * The session is still active and is awaiting resources. An appropriate
     * mpe_PodDecryptSessionEvent is signaled when resources become available (see OCAP 16.2.1.7).
     */
    MPE_POD_DECRYPT_EVENT_RESOURCE_LOST = 0x9302
} mpe_PodDecryptSessionEvent;

typedef enum
{
    //MPE_POD_DECRYPT_STATE_IDLE = 0,
	MPE_POD_DECRYPT_STATE_NOT_SELECTED = 0,
    MPE_POD_DECRYPT_STATE_ISSUED_QUERY,
    MPE_POD_DECRYPT_STATE_ISSUED_MMI,
    MPE_POD_DECRYPT_STATE_DESCRAMBLING,
    MPE_POD_DECRYPT_STATE_FAILED_DESCRAMBLING
} mpe_PODDecryptState;

typedef enum
{
	MPE_POD_DECRYPT_REQUEST_STATE_UNKNOWN = 0,
    MPE_POD_DECRYPT_REQUEST_STATE_ACTIVE,
    MPE_POD_DECRYPT_REQUEST_STATE_WAITING_FOR_RESOURCES
} mpe_PODDecryptRequestState;

/* POD Params (not generic features) */
typedef enum
{
    MPE_POD_PARAM_ID_MAX_NUM_ELEMENTARY_STREAM = 1,
    MPE_POD_PARAM_ID_MAX_NUM_PROGRAMS,
    MPE_POD_PARAM_ID_MAX_NUM_TRANSPORT_STREAMS
} mpe_podParamId;

typedef enum
{
	MPE_POD_UNKNOWN_HANDLE = 0,
    MPE_POD_SERVICE_DETAILS_HANDLE = 1,
    MPE_POD_TRANSPORT_STREAM_HANDLE
} mpe_podHandleType;

/**
 * Structure representing data from a feature_list() response message.
 * See SCTE28 Table 8.12-E.
 */
typedef struct
{
    uint8_t number; /* Number of generic features identifiers. */
    uint8_t* featureIds; /* Array of feature identifiers of supported features. */
} mpe_PODFeatures;

/* handle to Decrypt session structure */
typedef struct _mpe_PODDecryptSessionH
{
    int unused1;
}*mpe_PODDecryptSessionHandle;

/* handle to Copy Protection session */
typedef struct
{
    int unused1;
}*mpe_PODCPSession;

/*
 * Generic feature parameter database structure.  This structure contains
 * the current values of the generic features that are configurable between
 * the CableCard device and the Host.  It does not represent all of the
 * feature parameter messages possible within SCTE28 Table 8.12-I, but does
 * include the parameters that are configurable by the CableCard and/or
 * and OCAP application.
 */
typedef struct
{
    /* RF Output Channel data. */
    uint8_t rf_output_channel[2]; /* RF output channel number & UI enable/disable flag. */

    /* Parental Control PIN. */
    uint8_t *pc_pin; /* Parental control PIN =
     * {
     *    pc_pin_length - 8 bits,
     *    for (i=0; i < pc_pin_length; ++i)
     *      pc_pin - 8 bits
     * }
     */
    /* Parental Control Settings. */
    uint8_t *pc_setting; /* Channel settings =
     * {
     *    p_c_factory_reset - 8 bits
     *    p_c_channel_count - 16 bits,
     *    for (i=0; i < p_c_channel_count; ++i) {
     *      reserved - 4 bits
     *      major_channel_number - 10 bits
     *      minor_channel_number - 10 bits
     * };
     */

    /* IPPV PIN */
    uint8_t *ippv_pin; /* IPPV PIN =
     * {
     *    ippv_pin_length - 8 bits
     *    for (i=0; i < ippv_pin_length; ++i)
     *      ippv_pin_chr - 8 bits
     * }
     */
    /* Time zone. */
    uint8_t time_zone_offset[2]; /* Time zone offset in minutes from GMT. */

    /* Daylight savings. */
    uint8_t daylight_savings[10]; /* Daylight savings data =
     * {
     *     daylight_savings_control  -- 8 bits
     *     if(daylight_savings_control == 0x02)
     *     {
     *         daylight_savings_delta_minutes  -- 8 bits
     *         daylight_savings_entry_time (GPS secs) -- 32 bits
     *         daylight_savings_exit_time (GPS secs) -- 32 bits
     *     }
     * }
     */

    /* AC outlet control. */
    uint8_t ac_outlet_ctrl;

    /* Language control. */
    uint8_t language_ctrl[3]; /* ISO 639 3-byte language code. */

    /* Ratings region code. */
    uint8_t ratings_region; /* Region identifier. */

    /* Reset PIN */
    uint8_t reset_pin_ctrl; /* Reset pin control indicator. */

    /* Cable URLs */
    uint8_t cable_urls_length; /* Total size of cable URL data. */
    uint8_t *cable_urls; /* Cable URLs =
     * {
     *     number_of_urls - 8 bits
     *     for (i=0; i < number_of_urls; ++i) {
     *       url_type - 8 bits
     *       url_length - 8 bits
     *       for (i=0; i < url_length; ++i)
     *         url_chr
     *     }
     * }
     */

    /* EA location code */
    uint8_t ea_location[3]; /* EA location code =
     * {
     *     state_code - 8 bits
     *     county_subdivision - 4 bits
     *     reserved - 2 bits
     *     county_code - 10 bits
     * }
     */

    /* VCT ID */
    uint8_t vct_id[2]; /* VCT ID */

    /* Turn-on Channel */
    uint8_t turn_on_channel[2]; /* Virtual channel to be tuned on power-up =
     * {
     *     reserved - 3 bits (set to zero)
     *     channel_defined - 1 bit
     *     if (channel_defined == 1) {
     *         turn_on_channel - 12 bits
     *     } else {
     *         reserved - 12 bits
     *     }
     * }
     */

    /* Terminal Association */
    uint8_t *term_assoc; /* Terminal association =
     * {
     *     identifier_length - 16 bits
     *     for (i=0; i < identifier_length; ++i) {
     *       term_assoc_identifier - 8 bits
     *     }
     * }
     */

    /* Common download group ID */
    uint8_t cdl_group_id[2]; /* Common download group ID assignment */

    /* Zip Code */
    uint8_t *zip_code; /* Zip Code =
     * {
     *     zip_code_length - 16 bits
     *     for (i=0; i < zip_code_length; ++i) {
     *       zip_code_byte - 8 bits
     *     }
     * }
     */

} mpe_PODFeatureParams;

/**
 * This structure is the main database container for all of the POD related information.
 */
typedef struct
{
    mpe_Mutex pod_mutex; /* Mutex for synchronous access */
    mpe_Bool pod_isPresent; /* POD present flag (possibly not yet initialized). */
    mpe_Bool pod_isReady; /* POD present & initialized flag. */
    mpe_PODFeatures *pod_features; /* POD generic feature list. */
    mpe_PODFeatureParams pod_featureParams; /* POD generic feature parameters. */
    uint32_t pod_maxElemStreams; /* how many Elementary streams the POD can decode at one time */
    uint32_t pod_maxPrograms; /* how many programs the POD can decode at one time */
    uint32_t pod_maxTransportStreams; /* how many transport streams the POD can decode at one time */
} mpe_PODDatabase;

typedef struct _mpe_PodDecryptRequestParams
{
	uint8_t handleType;
    uint32_t handle;
    uint32_t tunerId;
    uint32_t numPids;
    mpe_MediaPID *pids;
    uint32_t priority;
    mpe_Bool mmiEnable;
} mpe_PodDecryptRequestParams;

/*
typedef struct
{
    uint8_t programIdx; // session identifier.  The POD "program" resource used for the session
    uint8_t ltsid; // local transport stream ID.  At this time, this be convention is the Tuner ID
    uint8_t transactionId; // local POD Transaction ID.  Defined and maintained by MPE
    uint8_t ca_pmt_cmd_id; // command sent to POD
    mpe_Bool added; // was the session added (TRUE) or did it replace another session
    uint32_t priority; // priority of session
    uint16_t sourceId; // SI Source ID
    mpe_EventQueue eventQ; // queue to communicate session events to higher levels
    void* act; // edHandle or user data, always goes into optionaldata2 field of queue send
} mpe_PODDecryptSession;
*/

typedef struct _mpe_PODStreamDecryptInfo
{
    uint16_t pid;       // PID to query (filled in by the caller)
    uint16_t status;    // ca_enable value (0x1,0x2,0x3,0x71,0x73) or 0 if no status provided (filled in by MPE)
} mpe_PODStreamDecryptInfo;

/**
 * MPE API function mapping call table for POD support APIs:
 */
typedef struct
{
    void (*mpe_pod_init_ptr)(void);

    mpe_Error (*mpe_podIsReady_ptr)(void);
    mpe_Error (*mpe_podIsPresent_ptr)(void);
    mpe_Error (*mpe_podGetFeatures_ptr)(mpe_PODFeatures **features);
    mpe_Error (*mpe_podGetFeatureParam_ptr)(uint32_t featureId, void *parm,
            uint32_t *len);
    mpe_Error (*mpe_podSetFeatureParam_ptr)(uint32_t featureId, void *param,
            uint32_t len);
    mpe_Error (*mpe_podRegister_ptr)(mpe_EdEventInfo *edHandle);
    mpe_Error (*mpe_podUnregister_ptr)(void);
    mpe_Error (*mpe_podSASConnect_ptr)(uint8_t *appId, uint32_t *sessionId, uint16_t *version);
    mpe_Error (*mpe_podSASClose_ptr)(uint32_t sessionId);
    mpe_Error (*mpe_podMMIConnect_ptr)(uint32_t *sessionId, uint16_t *version);
    mpe_Error (*mpe_podMMIClose_ptr)(void);
    mpe_Error (*mpe_podAIConnect_ptr)(uint32_t *sessionId, uint16_t *version);
    mpe_Error (*mpe_podReceiveAPDU_ptr)(uint32_t *sessionId, uint8_t **apdu,
            int32_t *size);
    mpe_Error (*mpe_podSendAPDU_ptr)(uint32_t sessionId, uint32_t apduTag,
            uint32_t size, uint8_t *apdu);
    mpe_Error (*mpe_podGetParam_ptr)(mpe_podParamId paramId,
            uint32_t* paramValue);
    mpe_Error (*mpe_podStartDecrypt_ptr)(
    		mpe_PodDecryptRequestParams* decryptRequestPtr,
            mpe_EventQueue queueId, void* act,
            mpe_PODDecryptSessionHandle* sessionPtr); /* act needs to be edHandle */
    mpe_Error (*mpe_podStopDecrypt_ptr)(
            mpe_PODDecryptSessionHandle sessionHandle);
    mpe_Error (*mpe_podCASConnect_ptr)(uint32_t *sessionId, uint16_t *version);
    mpe_Error (*mpe_podCASClose_ptr)(uint32_t sessionId);
    mpe_Error (*mpe_podReleaseAPDU_ptr)(uint8_t* apdu);
    mpe_Error (*mpe_podGetDecryptStreamStatus_ptr)(
    		mpe_PODDecryptSessionHandle handle,
    		uint8_t numStreams,
    		mpe_PODStreamDecryptInfo streamInfo[]);
} mpe_pod_ftable_t;

/**
 * MPE API definitions for POD support.
 *
 * These definitions used to bind the APIs references to the function table references.
 */
#define mpe_podmgr_ftable ((mpe_pod_ftable_t*)(FTABLE[MPE_MGR_TYPE_POD]))

#define mpe_podInit                 ((mpe_podmgr_ftable->mpe_pod_init_ptr))
#define mpe_podIsReady              ((mpe_podmgr_ftable->mpe_podIsReady_ptr))
#define mpe_podIsPresent            ((mpe_podmgr_ftable->mpe_podIsPresent_ptr))
#define mpe_podGetFeatures          ((mpe_podmgr_ftable->mpe_podGetFeatures_ptr))
#define mpe_podGetFeatureParam      ((mpe_podmgr_ftable->mpe_podGetFeatureParam_ptr))
#define mpe_podSetFeatureParam      ((mpe_podmgr_ftable->mpe_podSetFeatureParam_ptr))
#define mpe_podRegister             ((mpe_podmgr_ftable->mpe_podRegister_ptr))
#define mpe_podUnregister           ((mpe_podmgr_ftable->mpe_podUnregister_ptr))
#define mpe_podSASConnect           ((mpe_podmgr_ftable->mpe_podSASConnect_ptr))
#define mpe_podSASClose             ((mpe_podmgr_ftable->mpe_podSASClose_ptr))
#define mpe_podMMIConnect           ((mpe_podmgr_ftable->mpe_podMMIConnect_ptr))
#define mpe_podMMIClose             ((mpe_podmgr_ftable->mpe_podMMIClose_ptr))
#define mpe_podAIConnect            ((mpe_podmgr_ftable->mpe_podAIConnect_ptr))
#define mpe_podReceiveAPDU          ((mpe_podmgr_ftable->mpe_podReceiveAPDU_ptr))
#define mpe_podSendAPDU             ((mpe_podmgr_ftable->mpe_podSendAPDU_ptr))
#define mpe_podGetParam             ((mpe_podmgr_ftable->mpe_podGetParam_ptr))
#define mpe_podStartDecrypt         ((mpe_podmgr_ftable->mpe_podStartDecrypt_ptr))
#define mpe_podStopDecrypt          ((mpe_podmgr_ftable->mpe_podStopDecrypt_ptr))
#define mpe_podCASConnect           ((mpe_podmgr_ftable->mpe_podCASConnect_ptr))
#define mpe_podCASClose             ((mpe_podmgr_ftable->mpe_podCASClose_ptr))
#define mpe_podReleaseAPDU          ((mpe_podmgr_ftable->mpe_podReleaseAPDU_ptr))
#define mpe_podGetDecryptStreamStatus	((mpe_podmgr_ftable->mpe_podGetDecryptStreamStatus_ptr))

#endif /* _MPE_POD_H_ */
