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

#ifndef _SIMGR_H_
#define _SIMGR_H_

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpeos_si.h"
#include <../include/si_util.h>
#include <mpe_os.h>
#include <mpe_ed.h>
#include "mpeos_hn.h"

#ifdef __cplusplus
extern "C"
{
#endif

/* SI return error codes */
#define MPE_SI_ERROR_BASE               (0x00004000)
#define MPE_SI_SUCCESS                  (MPE_SUCCESS)

/*  Ex: when a handle requested (say using freq, prog_num) is not found
 MPE_SI_ERROR_NOT_FOUND is returned
 */
#define MPE_SI_NOT_FOUND                (MPE_SI_ERROR_BASE + 1)
#define MPE_SI_OUT_OF_MEM               (MPE_SI_ERROR_BASE + 2)
#define MPE_SI_INVALID_PARAMETER        (MPE_SI_ERROR_BASE + 3)
#define MPE_SI_LOCKING_ERROR            (MPE_SI_ERROR_BASE + 4)

/*  Ex: when PSI info is requested but not yet acquired
 MPE_SI_ERROR_NOT_AVAILABLE is returned
 */
#define MPE_SI_NOT_AVAILABLE            (MPE_SI_ERROR_BASE + 5)
#define MPE_SI_NOT_AVAILABLE_YET        (MPE_SI_ERROR_BASE + 6)
/* Use this error case when PSI for a service entry is being updated */
#define MPE_SI_PSI_INVALID              (MPE_SI_ERROR_BASE + 7)
#define MPE_SI_ERROR_ACQUIRING          (MPE_SI_ERROR_BASE + 8)
#define MPE_SI__IN_USE                  (MPE_SI_ERROR_BASE + 9)
#define MPE_SI_DSG_UNAVAILABLE          (MPE_SI_ERROR_BASE + 10)

/*  SI DB Events */
/*  For all SI DB events, optional data 1 field contains the 'si handle' where
 appropriate and the optional data 2 field contains the 'ACT' value
 passed in at registration time. ('ACT' is the 'ed' handle)
 */
#define MPE_SI_EVENT_UNKNOWN                        (0x00003000)
#define MPE_SI_EVENT_OOB_VCT_ACQUIRED               (MPE_SI_EVENT_UNKNOWN + 1)
#define MPE_SI_EVENT_OOB_NIT_ACQUIRED               (MPE_SI_EVENT_UNKNOWN + 2)
#define MPE_SI_EVENT_OOB_PAT_ACQUIRED               (MPE_SI_EVENT_UNKNOWN + 3)
#define MPE_SI_EVENT_OOB_PMT_ACQUIRED               (MPE_SI_EVENT_UNKNOWN + 4)
#define MPE_SI_EVENT_IB_PAT_ACQUIRED                (MPE_SI_EVENT_UNKNOWN + 5)
#define MPE_SI_EVENT_IB_PMT_ACQUIRED                (MPE_SI_EVENT_UNKNOWN + 6)

#define MPE_SI_EVENT_TRANSPORT_STREAM_UPDATE        (MPE_SI_EVENT_UNKNOWN + 7)
#define MPE_SI_EVENT_NETWORK_UPDATE                 (MPE_SI_EVENT_UNKNOWN + 8)
#define MPE_SI_EVENT_SERVICE_DETAILS_UPDATE         (MPE_SI_EVENT_UNKNOWN + 9)
/*
 For service component update event, the 'event_flag' field indicates the
 type of change (ADD/REMOVE/MODIFY)
 */
#define MPE_SI_EVENT_SERVICE_COMPONENT_UPDATE           (MPE_SI_EVENT_UNKNOWN + 10)
#define MPE_SI_EVENT_IB_PAT_UPDATE                      (MPE_SI_EVENT_UNKNOWN + 11)
#define MPE_SI_EVENT_IB_PMT_UPDATE                      (MPE_SI_EVENT_UNKNOWN + 12)
#define MPE_SI_EVENT_OOB_PAT_UPDATE                     (MPE_SI_EVENT_UNKNOWN + 13)
#define MPE_SI_EVENT_OOB_PMT_UPDATE                     (MPE_SI_EVENT_UNKNOWN + 14)

/* This event is used to terminate the ED session and delete the associatd ED handle */
#define MPE_SI_EVENT_SI_ACQUIRING                   (MPE_SI_EVENT_UNKNOWN + 15)
#define MPE_SI_EVENT_SI_NOT_AVAILABLE_YET           (MPE_SI_EVENT_UNKNOWN + 16)
#define MPE_SI_EVENT_SI_FULLY_ACQUIRED              (MPE_SI_EVENT_UNKNOWN + 17)
#define MPE_SI_EVENT_SI_DISABLED                    (MPE_SI_EVENT_UNKNOWN + 18)
#define MPE_SI_EVENT_TUNED_AWAY                     (MPE_SI_EVENT_UNKNOWN + 19)
#define MPE_SI_EVENT_TERMINATE_SESSION              (MPE_SI_EVENT_UNKNOWN + 20)
#define MPE_SI_EVENT_IB_CVCT_ACQUIRED               (MPE_SI_EVENT_UNKNOWN + 21)
#define MPE_SI_EVENT_IB_CVCT_UPDATE                 (MPE_SI_EVENT_UNKNOWN + 22)
#define MPE_SI_EVENT_NIT_SVCT_ACQUIRED              (MPE_SI_EVENT_UNKNOWN + 23)

#define MPE_SI_CHANGE_TYPE_ADD                              0x01
#define MPE_SI_CHANGE_TYPE_REMOVE                           0x02
#define MPE_SI_CHANGE_TYPE_MODIFY                           0x03

/* SI Handles, default values */
#define MPE_SI_NUM_TRANSPORTS                   0x1
#define MPE_SI_DEFAULT_TRANSPORT_ID             0x1
#define MPE_SI_DEFAULT_TRANSPORT_HANDLE         0x1000

#define MPE_SI_NUM_NETWORKS                     0x1
#define MPE_SI_DEFAULT_NETWORK_ID               0x1
#define MPE_SI_DEFAULT_NETWORK_HANDLE           0x2000
#define MPE_SI_DEFAULT_CHANNEL_NUMBER           0xFFFFFFFF

#define MPE_SI_INVALID_HANDLE                   0

/* Default values for SI fields (OCAP specified) */
/* sourceId and tsId are set by default to -1 */
#define SOURCEID_UNKNOWN                (0xFFFFFFFF)
#define TSID_UNKNOWN                    (0xFFFFFFFF) // ECR 1072
#define MPE_SI_DEFAULT_PROGRAM_NUMBER           (0xFFFFFFFF)
#define PROGRAM_NUMBER_UNKNOWN                  (0xFFFF)
#define VIRTUAL_CHANNEL_UNKNOWN                 (0xFFFF)

#define NUMBER_NON_UNIQUE_SOURCEIDS    10

/* SI DB Supported handle types */
typedef uint32_t mpe_SiTransportHandle;
typedef uint32_t mpe_SiNetworkHandle;
typedef uint32_t mpe_SiTransportStreamHandle;
typedef uint32_t mpe_SiServiceHandle;
typedef uint32_t mpe_SiProgramHandle;
typedef uint32_t mpe_SiServiceDetailsHandle;
typedef uint32_t mpe_SiServiceComponentHandle;
typedef uint32_t mpe_SiRatingDimensionHandle;
typedef uint32_t mpe_SiGenericHandle; // Cast before use...

/* OOB frequency -1 */
#define MPE_SI_OOB_FREQUENCY                0xFFFFFFFF
/* HN frequency -2 */
#define MPE_SI_HN_FREQUENCY                 0xFFFFFFFE
/* Separate OOB and DSG such that they have different
 * frequencies. We may need to support legacy OOB as well
 * as DSG at which point it will become necessary that
 * they have different internal frequency values
 * DSG frequency -3
 */
#define MPE_SI_DSG_FREQUENCY                 0xFFFFFFFD

#define MAX_FREQUENCIES 255

/*  Data structures and Methods defined by SI DB */

typedef enum mpe_SiGlobalState
{
    SI_ACQUIRING = 0x00,
    SI_NOT_AVAILABLE_YET = 0x01,
    SI_NIT_SVCT_ACQUIRED = 0x02,
    SI_FULLY_ACQUIRED = 0x03,
    SI_DISABLED = 0x04
// when OOB SI is disabled
} mpe_SiGlobalState;

typedef enum mpe_SiDeliverySystemType
{
    MPE_SI_DELIVERY_SYSTEM_TYPE_CABLE = 0x00,
    MPE_SI_DELIVERY_SYSTEM_TYPE_SATELLITE = 0x01,
    MPE_SI_DELIVERY_SYSTEM_TYPE_TERRESTRIAL = 0x02,
    MPE_SI_DELIVERY_SYSTEM_TYPE_UNKNOWN = 0x03
} mpe_SiDeliverySystemType;

typedef enum mpe_SiServiceInformationType
{
    MPE_SI_SERVICE_INFORMATION_TYPE_ATSC_PSIP = 0x00,
    MPE_SI_SERVICE_INFORMATION_TYPE_DVB_SI = 0x01,
    MPE_SI_SERVICE_INFORMATION_TYPE_SCTE_SI = 0x02,
    MPE_SI_SERVICE_INFORMATION_TYPE_UNKNOWN = 0x03
} mpe_SiServiceInformationType;

/**
 * The DescriptorTag contains constant values to be read from the
 * descriptor_tag field in a Descriptor.  The constant values will
 * correspond to those specified in OCAP SI.
 */
typedef enum mpe_SiDescriptorTag
{
    MPE_SI_DESC_VIDEO_STREAM = 0x02,
    MPE_SI_DESC_AUDIO_STREAM = 0x03,
    MPE_SI_DESC_HIERARCHY = 0x04,
    MPE_SI_DESC_REGISTRATION = 0x05,
    MPE_SI_DESC_DATA_STREAM_ALIGNMENT = 0x06,
    MPE_SI_DESC_TARGET_BACKGROUND_GRID = 0x07,
    MPE_SI_DESC_VIDEO_WINDOW = 0x08,
    MPE_SI_DESC_CA_DESCRIPTOR = 0x09,
    MPE_SI_DESC_ISO_639_LANGUAGE = 0x0A,
    MPE_SI_DESC_SYSTEM_CLOCK = 0x0B,
    MPE_SI_DESC_MULTIPLEX_UTILIZATION_BUFFER = 0x0C,
    MPE_SI_DESC_COPYRIGHT = 0x0D,
    MPE_SI_DESC_MAXIMUM_BITRATE = 0x0E,
    MPE_SI_DESC_PRIVATE_DATA_INDICATOR = 0x0F,
    MPE_SI_DESC_SMOOTHING_BUFFER = 0x10,
    MPE_SI_DESC_STD = 0x11,
    MPE_SI_DESC_IBP = 0x12,
    /* Current spec doesn't define MPE_SI_DESC_CAROUSEL_ID descriptor tag */
    /* Needs to be added to DescriptorTagImpl */
    MPE_SI_DESC_CAROUSEL_ID = 0x13,
    MPE_SI_DESC_ASSOCIATION_TAG = 0x14,
    MPE_SI_DESC_DEFERRED_ASSOCIATION_TAG = 0x15,
    MPE_SI_DESC_APPLICATION = 0x00,
    MPE_SI_DESC_APPLICATION_NAME = 0x01,
    MPE_SI_DESC_TRANSPORT_PROTOCOL = 0x02,
    MPE_SI_DESC_DVB_J_APPLICATION = 0x03,
    MPE_SI_DESC_DVB_J_APPLICATION_LOCATION = 0x04,
    MPE_SI_DESC_EXTERNAL_APPLICATION_AUTHORISATION = 0x05,
    MPE_SI_DESC_IPV4_ROUTING = 0x06,
    MPE_SI_DESC_IPV6_ROUTING = 0x07,
    MPE_SI_DESC_APPLICATION_ICONS = 0x0B,
    MPE_SI_DESC_PRE_FETCH = 0x0C,
    MPE_SI_DESC_DII_LOCATION = 0x0D,
    MPE_SI_DESC_COMPONENT_DESCRIPTOR = 0x50,
    MPE_SI_DESC_STREAM_IDENTIFIER_DESCRIPTOR = 0x52,
    MPE_SI_DESC_PRIVATE_DATA_SPECIFIER = 0x5F,
    MPE_SI_DESC_DATA_BROADCAST_ID = 0x66,
    MPE_SI_DESC_APPLICATION_SIGNALING = 0x6F,
    MPE_SI_DESC_SERVICE_IDENTIFIER = 0x0D,
    MPE_SI_DESC_LABEL = 0x70,
    MPE_SI_DESC_CACHING_PRIORITY = 0x71,
    MPE_SI_DESC_CONTENT_TYPE = 0x72,
    MPE_SI_DESC_STUFFING = 0x80,
    MPE_SI_DESC_AC3_AUDIO = 0x81,
    MPE_SI_DESC_CAPTION_SERVICE = 0x86,
    MPE_SI_DESC_CONTENT_ADVISORY = 0x87,
    MPE_SI_DESC_REVISION_DETECTION = 0x93,
    MPE_SI_DESC_TWO_PART_CHANNEL_NUMBER = 0x94,
    MPE_SI_DESC_CHANNEL_PROPERTIES = 0x95,
    MPE_SI_DESC_DAYLIGHT_SAVINGS_TIME = 0x96,
    MPE_SI_DESC_EXTENDED_CHANNEL_NAME_DESCRIPTION = 0xA0,
    MPE_SI_DESC_SERVICE_LOCATION = 0xA1,
    MPE_SI_DESC_TIME_SHIFTED_SERVICE = 0xA2,
    MPE_SI_DESC_COMPONENT_NAME = 0xA3,
    MPE_SI_DESC_MAC_ADDRESS_LIST = 0xA4,
    MPE_SI_DESC_ATSC_PRIVATE_INFORMATION_DESCRIPTOR = 0xAD
} mpe_SiDescriptorTag;

/**
 * Service type values represents "digital television",
 * "digital radio", "NVOD reference service", "NVOD time-shifted service",
 * "analog television", "analog radio", "data broadcast" and "application".
 *
 * (These values are mappable to the ATSC service type in the VCT table and
 * the DVB service type in the Service Descriptor.)
 */
typedef enum mpe_SiServiceType
{
    MPE_SI_SERVICE_TYPE_UNKNOWN = 0x00,
    MPE_SI_SERVICE_ANALOG_TV = 0x01,
    MPE_SI_SERVICE_DIGITAL_TV = 0x02,
    MPE_SI_SERVICE_DIGITAL_RADIO = 0x03,
    MPE_SI_SERVICE_DATA_BROADCAST = 0x04,
    MPE_SI_SERVICE_NVOD_REFERENCE = 0x05,
    MPE_SI_SERVICE_NVOD_TIME_SHIFTED = 0x06,
    MPE_SI_SERVICE_ANALOG_RADIO = 0x07,
    MPE_SI_SERVICE_DATA_APPLICATION = 0x08
} mpe_SiServiceType;

/**
 * Service component represents an abstraction of an elementary
 * stream. It provides information about individual components of a
 * service.  Generally speaking, a service component carries content
 * such as media (e.g. as audio or video) or data.
 * ServiceComponent stream types (e.g., "video", "audio", "subtitles", "data",
 * "sections", etc.).
 */
typedef enum mpe_SiServiceComponentType
{
    MPE_SI_COMP_TYPE_UNKNOWN = 0x00,
    MPE_SI_COMP_TYPE_VIDEO = 0x01,
    MPE_SI_COMP_TYPE_AUDIO = 0x02,
    MPE_SI_COMP_TYPE_SUBTITLES = 0x03,
    MPE_SI_COMP_TYPE_DATA = 0x04,
    MPE_SI_COMP_TYPE_SECTIONS = 0x05
} mpe_SiServiceComponentType;

// Definition moved to mpeos_si.h
/**
 * This represents valid values for the elementarty stream_type field
 * in the PMT.
 */
/* Definition moved to mpeos_si.h
 typedef enum mpe_SiElemStreamType
 {
 MPE_SI_ELEM_MPEG_1_VIDEO = 0x01,
 MPE_SI_ELEM_MPEG_2_VIDEO = 0x02,
 MPE_SI_ELEM_MPEG_1_AUDIO = 0x03,
 MPE_SI_ELEM_MPEG_2_AUDIO = 0x04,
 MPE_SI_ELEM_MPEG_PRIVATE_SECTION = 0x05,
 MPE_SI_ELEM_MPEG_PRIVATE_DATA = 0x06,
 MPE_SI_ELEM_MHEG = 0x07,
 MPE_SI_ELEM_DSM_CC = 0x08,
 MPE_SI_ELEM_H_222 = 0x09,
 MPE_SI_ELEM_DSM_CC_MPE = 0x0A,
 MPE_SI_ELEM_DSM_CC_UN = 0x0B,
 MPE_SI_ELEM_DSM_CC_STREAM_DESCRIPTORS = 0x0C,
 MPE_SI_ELEM_DSM_CC_SECTIONS = 0x0D,
 MPE_SI_ELEM_AUXILIARY = 0x0E,
 MPE_SI_ELEM_VIDEO_DCII = 0x80,
 MPE_SI_ELEM_ATSC_AUDIO = 0x81,
 MPE_SI_ELEM_STD_SUBTITLE = 0x82,
 MPE_SI_ELEM_ISOCHRONOUS_DATA = 0x83,
 MPE_SI_ELEM_ASYNCHRONOUS_DATA = 0x84
 } mpe_SiElemStreamType;
 */

// Definition moved to mpeos_si.h
/**
 *  Modulation modes
 */
/*
 typedef enum mpe_SiModulationMode
 {
 MPE_SI_MODULATION_UNKNOWN=0,
 MPE_SI_MODULATION_QPSK,
 MPE_SI_MODULATION_BPSK,
 MPE_SI_MODULATION_OQPSK,
 MPE_SI_MODULATION_VSB8,
 MPE_SI_MODULATION_VSB16,
 MPE_SI_MODULATION_QAM16,
 MPE_SI_MODULATION_QAM32,
 MPE_SI_MODULATION_QAM64,
 MPE_SI_MODULATION_QAM80,
 MPE_SI_MODULATION_QAM96,
 MPE_SI_MODULATION_QAM112,
 MPE_SI_MODULATION_QAM128,
 MPE_SI_MODULATION_QAM160,
 MPE_SI_MODULATION_QAM192,
 MPE_SI_MODULATION_QAM224,
 MPE_SI_MODULATION_QAM256,
 MPE_SI_MODULATION_QAM320,
 MPE_SI_MODULATION_QAM384,
 MPE_SI_MODULATION_QAM448,
 MPE_SI_MODULATION_QAM512,
 MPE_SI_MODULATION_QAM640,
 MPE_SI_MODULATION_QAM768,
 MPE_SI_MODULATION_QAM896,
 MPE_SI_MODULATION_QAM1024,
 MPE_SI_MODULATION_QAM_NTSC // for analog mode
 } mpe_SiModulationMode;
 */
typedef enum mpe_SiStatus
{
    SI_NOT_AVAILABLE = 0x00, SI_AVAILABLE_SHORTLY = 0x01, SI_AVAILABLE = 0x02
} mpe_SiStatus;

// mpe_SiEntryState represents state of a service entry based on of SVCT-DCM, SVCT-VCM signaling
typedef enum mpe_SiEntryState
{
    SIENTRY_UNSPECIFIED = 0, SIENTRY_PRESENT_IN_DCM = 0x01, // Present in DCM
    SIENTRY_DEFINED = 0x02, // Defined in DCM
    SIENTRY_MAPPED = 0x04, // Mapped in VCM
    SIENTRY_DEFINED_MAPPED = SIENTRY_PRESENT_IN_DCM | SIENTRY_DEFINED
            | SIENTRY_MAPPED, // Marked defined in SVCT-DCM, mapped in SVCT-VCM
    SIENTRY_DEFINED_UNMAPPED = SIENTRY_PRESENT_IN_DCM | SIENTRY_DEFINED, // Marked defined in SVCT-DCM, not mapped in SVCT-VCM
    SIENTRY_UNDEFINED_MAPPED = SIENTRY_PRESENT_IN_DCM | SIENTRY_MAPPED, // Marked undefined in SVCT-DCM, mapped in SVCT-VCM
    SIENTRY_UNDEFINED_UNMAPPED = SIENTRY_PRESENT_IN_DCM
// Marked undefined SVCT-DCM, not mapped in SVCT-VCM
} mpe_SiEntryState;

typedef enum mpe_TsEntryState
{
    TSENTRY_NEW, TSENTRY_OLD, TSENTRY_UPDATED
} mpe_TsEntryState;

typedef enum mpe_SiSourceType
{
    MPE_SOURCE_TYPE_UNKNOWN = 0x00, 
    MPE_SOURCE_TYPE_OOB = 0x01, 
    MPE_SOURCE_TYPE_IB = 0x02, 
    MPE_SOURCE_TYPE_PVR_FILE = 0x03, 
    MPE_SOURCE_TYPE_HN_STREAM = 0x04, 
    MPE_SOURCE_TYPE_DSG = 0x05
} mpe_SiSourceType;

typedef struct mpe_SiPatProgramList
{
    uint32_t programNumber;
    uint32_t pmt_pid;
    mpe_Bool matched;
    struct mpe_SiPatProgramList* next;
} mpe_SiPatProgramList;

typedef struct mpe_SiTransportStreamEntry
{
    mpe_TsEntryState state;
    uint32_t frequency;
    mpe_SiModulationMode modulation_mode;
    uint32_t ts_id;
    char *ts_name;
    char *description;
    mpe_TimeMillis ptime_transport_stream;
    uint16_t service_count;
    uint16_t program_count;
    uint16_t visible_service_count;
    mpe_SiStatus siStatus;
    uint32_t pat_version; // PAT
    uint32_t pat_crc; // PAT
    uint32_t cvct_version; // CVCT
    uint32_t cvct_crc; // CVCT
    mpe_Bool check_crc;
    uint32_t pat_reference;
    mpe_SiPatProgramList* pat_program_list; // PAT
    struct mpe_SiTransportStreamEntry* next;

    // moved from mpe_SiTableEntry

    mpe_SiSourceType source_type; // IB/OOB etc.
    uint8_t* pat_byte_array;
    ListSI services;
    ListSI programs;
    mpe_Mutex list_lock;
} mpe_SiTransportStreamEntry;

typedef struct mpe_SiRegistrationListEntry
{
    mpe_EdEventInfo *edHandle;
    uint32_t terminationEvent;
    uint32_t frequency;
    uint32_t programNumber;
    struct mpe_SiRegistrationListEntry* next;
} mpe_SiRegistrationListEntry;

typedef struct mpe_SIQueueListEntry
{
    mpe_EventQueue queueId;
    struct mpe_SIQueueListEntry* next;
} mpe_SIQueueListEntry;

typedef enum mpe_SiTableType
{
    TABLE_UNKNOWN = 0,
    OOB_SVCT_VCM = 1,
    OOB_NTT_SNS = 2,
    OOB_NIT_CDS = 3,
    OOB_NIT_MMS = 4,
    OOB_PAT = 5,
    OOB_PMT = 6,
    IB_PAT = 7,
    IB_PMT = 8,
    IB_CVCT = 9,
    IB_EIT = 10,
    OOB_SVCT_DCM = 11
} mpe_SiTableType;

/**
 MPEG2 Descriptor List
 Represents a linked list of MPEG-2 descriptors.
 Tag is the eight bit field that identifies each descriptor.
 descriptor_length is the eight bit field specifying the number of bytes of the descriptor immediately following the descriptor_length field.
 descriptor_content is the data contained within this descriptor.
 Next points to the next descriptor in the list
 */
typedef struct mpe_SiMpeg2DescriptorList
{
    mpe_SiDescriptorTag tag;
    uint8_t descriptor_length;
    void* descriptor_content;
    struct mpe_SiMpeg2DescriptorList* next;
} mpe_SiMpeg2DescriptorList;

typedef struct mpe_SiLangSpecificStringList
{
    char language[4]; /* 3-character ISO 639 language code + null terminator */
    char *string;
    struct mpe_SiLangSpecificStringList *next;
} mpe_SiLangSpecificStringList;

/**
 Elementary Stream List
 Elementary stream list is a linked list of elementary streams with in a given service.
 It also contains service component specific information such as the associated
 language, etc.
 */
typedef struct mpe_SiElementaryStreamList
{
    mpe_SiElemStreamType elementary_stream_type; // PMT
    /* If the PMT contains a component name descriptor then this
     should be the string extracted from the component_name_string () MSS
     string in this descriptor. Otherwise, 'service_comp_type' is the
     generic name associated with this stream type */
    mpe_SiServiceComponentType service_comp_type; //
    mpe_SiLangSpecificStringList* service_component_names; // PMT (desc)
    uint32_t elementary_PID; // PMT
    char associated_language[4]; // PMT (desc)
    uint32_t es_info_length; // PMT
    uint32_t number_desc; // PMT
    uint32_t ref_count;
    mpe_Bool valid;
    // refers to the service this component belongs to
    uint32_t service_reference;
    mpe_SiMpeg2DescriptorList* descriptors; // PMT
    mpe_TimeMillis ptime_service_component;
    struct mpe_SiElementaryStreamList* next;
} mpe_SiElementaryStreamList;

typedef enum mpe_SiPMTStatus
{
    PMT_NOT_AVAILABLE = 0x00,
    PMT_AVAILABLE_SHORTLY = 0x01,
    PMT_AVAILABLE = 0x02
} mpe_SiPMTStatus;

typedef enum mpe_SiVideoStandard
{
    VIDEO_STANDARD_NTSC = 0x00,
    VIDEO_STANDARD_PAL625 = 0x01,
    VIDEO_STANDARD_PAL525 = 0x02,
    VIDEO_STANDARD_SECAM = 0x03,
    VIDEO_STANDARD_MAC = 0x04,
    VIDEO_STANDARD_UNKNOWN = 0x05
} mpe_SiVideoStandard;

typedef enum mpe_SiChannelType
{
    CHANNEL_TYPE_NORMAL = 0x00,
    CHANNEL_TYPE_HIDDEN = 0x01,
    CHANNEL_TYPE_RESERVED_2 = 0x02,
    CHANNEL_TYPE_RESERVED_3 = 0x03,
    CHANNEL_TYPE_RESERVED_4 = 0x04,
    CHANNEL_TYPE_RESERVED_5 = 0x05,
    CHANNEL_TYPE_RESERVED_6 = 0x06,
    CHANNEL_TYPE_RESERVED_7 = 0x07,
    CHANNEL_TYPE_RESERVED_8 = 0x08,
    CHANNEL_TYPE_RESERVED_9 = 0x09,
    CHANNEL_TYPE_RESERVED_10 = 0x0A,
    CHANNEL_TYPE_RESERVED_11 = 0x0B,
    CHANNEL_TYPE_RESERVED_12 = 0x0C,
    CHANNEL_TYPE_RESERVED_13 = 0x0D,
    CHANNEL_TYPE_RESERVED_14 = 0x0E,
    CHANNEL_TYPE_RESERVED_15 = 0x0F,
} mpe_SiChannelType;

typedef struct mpe_SiPMTReference
{
    uint32_t pcr_pid;
    uint32_t number_outer_desc;
    uint32_t outer_descriptors_ref;
    uint32_t number_elem_streams;
    uint32_t elementary_streams_ref;
} mpe_SiPMTReference;

typedef struct mpe_SiSourceIdList
{
    uint32_t source_id; // SVCT
    struct mpe_SiSourceIdList* next;
} mpe_SiSourceIdList;

typedef struct mpe_SiProgramInfo
{
    // These will only be valid for digital services.
    mpe_SiPMTStatus pmt_status; // move to program_info structure.
    uint32_t program_number; // PAT, PMT, SVCT
    uint32_t pmt_pid; // PAT
    uint32_t pmt_version; // PMT
    mpe_Bool crc_check;
    uint32_t pmt_crc; // PMT
    mpe_SiPMTReference* saved_pmt_ref;
    uint8_t* pmt_byte_array; // PMT
    // Duplicated in saved_pmt_ref, why 2 copies?
    uint32_t number_outer_desc; // PMT
    mpe_SiMpeg2DescriptorList* outer_descriptors; // PMT
    uint32_t number_elem_streams;
    mpe_SiElementaryStreamList* elementary_streams; // PMT
    uint32_t pcr_pid; // PMT
    ListSI services; // Add. List of pointers to mpe_SiTableEntry structures.
    // Can be Empty.
    uint32_t service_count;
    mpe_SiTransportStreamEntry *stream; // Transport stream this program is part of.
    // Should never be NULL.
} mpe_SiProgramInfo;

/**
 Source name table, by sourceID/AppID
 */
typedef struct mpe_siSourceNameEntry
{
  mpe_Bool appType;
  uint32_t id;              // if appType=FALSE, sourceID otherwise appID
  mpe_Bool mapped;          // true if mapped by a virtual channel false for DSG service name
  mpe_SiLangSpecificStringList *source_names;      // NTT_SNS, CVCT
  mpe_SiLangSpecificStringList *source_long_names; // NTT_SNS
  struct mpe_siSourceNameEntry* next;              // next source name entry
} mpe_siSourceNameEntry;

/**
 Service represents an abstract view of what is generally referred to as
 a television "service" or "channel".
 The following data structure encapsulates all the information for a
 service in one location.
 */

typedef struct mpe_SiTableEntry
{
    uint32_t ref_count;
    uint32_t activation_time; // SVCT
    mpe_TimeMillis ptime_service;
    mpe_SiTransportStreamHandle ts_handle;
    mpe_SiProgramInfo *program; // Program info for this service. Can be shared with other mpe_SiTableEntry elements,
    // and can be null (analog channels)
    uint32_t tuner_id; // '0' for OOB, start from '1' for IB
    mpe_Bool valid;
    uint16_t virtual_channel_number;
    mpe_Bool isAppType; // for DSG app
    uint32_t source_id; // SVCT
    uint32_t app_id; //
    mpe_Bool dsgAttached;
    uint32_t dsg_attach_count; // DSG attach count
    mpe_SiEntryState state;
    mpe_SiChannelType channel_type; // SVCT
    mpe_SiVideoStandard video_standard; // SVCT (NTSC, PAL etc.)
    mpe_SiServiceType service_type; // SVCT (desc)

    mpe_siSourceNameEntry *source_name_entry;      // Reference to the corresponding NTT_SNS entry

    mpe_SiLangSpecificStringList *descriptions; // LVCT/CVCT (desc)
    uint8_t freq_index; // SVCT
    uint8_t mode_index; // SVCT
    uint32_t major_channel_number;
    uint32_t minor_channel_number; // SVCT (desc)

    uint16_t program_number;       // SVCT
    uint8_t  transport_type;       // SVCT (0==MPEG2, 1==non-MPEG2)
    mpe_Bool scrambled;            // SVCT

    mpe_HnStreamSession hn_stream_session;  // HN stream session handle
    uint32_t hn_attach_count; // HN stream session PSI attach / registration count

    struct mpe_SiTableEntry* next; // next service entry
} mpe_SiTableEntry;

/*
 * *****************************************************************************
 *               Rating Region structures
 * *****************************************************************************
 */

typedef struct mpe_SiRatingValuesDefined
{
    mpe_SiLangSpecificStringList *abbre_rating_value_text;
    mpe_SiLangSpecificStringList *rating_value_text;
} mpe_SiRatingValuesDefined;

typedef struct mpe_SiRatingDimension
{
    mpe_SiLangSpecificStringList *dimension_names;
    uint8_t graduated_scale;
    uint8_t values_defined;
    mpe_SiRatingValuesDefined *rating_value;

} mpe_SiRatingDimension;

typedef struct mpe_SiRatingRegion
{
    mpe_SiLangSpecificStringList *rating_region_name_text;
    uint8_t dimensions_defined;
    mpe_SiRatingDimension *dimensions;
} mpe_SiRatingRegion;

void mpe_siSetup(void);
void mpe_siInit(void);
void mpe_siShutdown(void);

/******************************************
 * SI DB function prototypes
 *****************************************/
typedef struct _mpe_si_ftable_t
{
    void (*mpe_si_init_ptr)(void);
    void (*mpe_si_shutdown_ptr)(void);

    // OC specific
    mpe_Error (*mpe_si_getPidByAssociationTag_ptr)(
            mpe_SiServiceHandle service_handle, uint16_t tag,
            uint32_t *target_pid);
    mpe_Error (*mpe_si_getPidByCarouselID_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t carousel_id,
            uint32_t *target_pid);
    mpe_Error (*mpe_si_getPidByComponentTag_ptr)(
            mpe_SiServiceHandle service_handle, int component_tag,
            uint32_t *component_pid);
    mpe_Error (*mpe_si_getProgramNumberByDeferredAssociationTag_ptr)(
            mpe_SiServiceHandle service_handle, uint16_t tag,
            uint32_t *program_number);

    // lock/unlock
    mpe_Error (*mpe_si_lockForRead_ptr)(void);
    mpe_Error (*mpe_si_unlock_ptr)(void);

    mpe_Error
            (*mpe_si_getTotalNumberOfTransports_ptr)(uint32_t *num_transports);
    mpe_Error (*mpe_si_getAllTransports_ptr)(
            mpe_SiTransportHandle array_transports[], uint32_t* num_transports);

    // transport handle specific
    mpe_Error (*mpe_si_getTransportDeliverySystemType_ptr)(
            mpe_SiTransportHandle transport_handle,
            mpe_SiDeliverySystemType *delivery_type);
    mpe_Error (*mpe_si_getNumberOfNetworksForTransportHandle_ptr)(
            mpe_SiTransportHandle transport_handle, uint32_t *num_networks);
    mpe_Error (*mpe_si_getAllNetworksForTransportHandle_ptr)(
            mpe_SiTransportHandle transport_handle,
            mpe_SiNetworkHandle array_network_handle[], uint32_t* num_networks);
    mpe_Error (*mpe_si_getNumberOfTransportStreamsForTransportHandle_ptr)(
            mpe_SiTransportHandle transport_handle,
            uint32_t *num_transport_streams);
    mpe_Error (*mpe_si_getAllTransportStreamsForTransportHandle_ptr)(
            mpe_SiTransportHandle transport_handle,
            mpe_SiTransportStreamHandle ts_handle[],
            uint32_t* num_tansport_streams);
    mpe_Error (*mpe_si_getNetworkHandleByTransportHandleAndNetworkId_ptr)(
            mpe_SiTransportHandle transport_handle, uint32_t network_id,
            mpe_SiNetworkHandle* network_handle);
    mpe_Error (*mpe_si_getTransportIdForTransportHandle_ptr)(
            mpe_SiTransportHandle transport_handle, uint32_t* transport_id);
    mpe_Error (*mpe_si_getTransportHandleByTransportId_ptr)(
            uint32_t transportId, mpe_SiTransportHandle* outHandle);

    // network handle specific
    mpe_Error (*mpe_si_getNetworkServiceInformationType_ptr)(
            mpe_SiNetworkHandle network_handle,
            mpe_SiServiceInformationType *service_info_type);
    mpe_Error (*mpe_si_getNetworkIdForNetworkHandle_ptr)(
            mpe_SiNetworkHandle network_handle, uint32_t *network_id);
    mpe_Error (*mpe_si_getNetworkNameForNetworkHandle_ptr)(
            mpe_SiNetworkHandle network_handle, char **network_name);
    mpe_Error (*mpe_si_getNetworkLastUpdateTimeForNetworkHandle_ptr)(
            mpe_SiNetworkHandle network_handle, mpe_TimeMillis *time);
    mpe_Error
            (*mpe_si_getNumberOfTransportStreamsForNetworkHandle_ptr)(
                    mpe_SiNetworkHandle network_handle,
                    uint32_t *num_transport_streams);
    mpe_Error (*mpe_si_getAllTransportStreamsForNetworkHandle_ptr)(
            mpe_SiNetworkHandle network_handle,
            mpe_SiTransportStreamHandle transport_stream_handle[],
            uint32_t* num_transport_streams);
    mpe_Error (*mpe_si_getTransportHandleForNetworkHandle_ptr)(
            mpe_SiNetworkHandle network_handle,
            mpe_SiTransportHandle* transport_handle);
    mpe_Error
            (*mpe_si_getTransportStreamHandleByTransportFrequencyModulationAndTSID_ptr)(
                    mpe_SiTransportHandle transport_handle, uint32_t frequency,
                    uint32_t mode, uint32_t ts_id,
                    mpe_SiTransportStreamHandle* ts_handle);

    // transport stream handle specific
    mpe_Error (*mpe_si_getTransportStreamIdForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, uint32_t *ts_id);
    mpe_Error (*mpe_si_getDescriptionForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, char **description);
    mpe_Error (*mpe_si_getTransportStreamNameForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, char **ts_name);
    mpe_Error (*mpe_si_getFrequencyForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, uint32_t* freq);
    mpe_Error (*mpe_si_getModulationFormatForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, mpe_SiModulationMode *mode);
    mpe_Error (*mpe_si_getTransportStreamServiceInformationType_ptr)(
            mpe_SiTransportStreamHandle ts_handle,
            mpe_SiServiceInformationType* serviceInformationType);
    mpe_Error
            (*mpe_si_getTransportStreamLastUpdateTimeForTransportStreamHandle_ptr)(
                    mpe_SiTransportStreamHandle ts_handle, mpe_TimeMillis *time);
    mpe_Error (*mpe_si_getNumberOfServicesForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle, uint32_t *num_services);
    mpe_Error (*mpe_si_getAllServicesForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle,
            mpe_SiServiceHandle serviceHandle[], uint32_t* num_services);
    mpe_Error (*mpe_si_getNetworkHandleForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle,
            mpe_SiNetworkHandle* network_handle);
    mpe_Error (*mpe_si_getTransportHandleForTransportStreamHandle_ptr)(
            mpe_SiTransportStreamHandle ts_handle,
            mpe_SiTransportHandle* transport_handle);

    // service handle specific
    mpe_Error (*mpe_si_getServiceHandleByServiceNumber_ptr)(
            uint32_t majorNumber, uint32_t minorNumber,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_getServiceHandleBySourceId_ptr)(uint32_t sourceId,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_getServiceHandleByAppId_ptr)(uint32_t appId,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_getServiceHandleByFrequencyModulationProgramNumber_ptr)(
            uint32_t freq, uint32_t mode, uint32_t prog_num,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_getServiceHandleByServiceName_ptr)(char* service_name,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_createDynamicServiceHandle_ptr)(uint32_t freq,
            uint32_t prog_num, mpe_SiModulationMode modFormat,
            mpe_SiServiceHandle *service_handle);
    mpe_Error (*mpe_si_createDSGServiceHandle_ptr)(uint32_t appId,
            uint32_t prog_num, char* service_name, char* language,
            mpe_SiServiceHandle *service_handle);

    // service handle based queries
    mpe_Error (*mpe_si_getTransportStreamHandleForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle,
            mpe_SiTransportStreamHandle *ts_handle);
    mpe_Error (*mpe_si_getPMTPidForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pmt_pid);
    mpe_Error
            (*mpe_si_getServiceTypeForServiceHandle_ptr)(
                    mpe_SiServiceHandle service_handle,
                    mpe_SiServiceType *service_type);
    mpe_Error (*mpe_si_getNumberOfLongNamesForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *num_long_names);
    mpe_Error (*mpe_si_getLongNamesForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, char* languages[],
            char* service_long_names[]);
    mpe_Error (*mpe_si_getNumberOfNamesForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *num_names);
    mpe_Error (*mpe_si_getNamesForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, char* languages[],
            char* service_names[]);
    mpe_Error (*mpe_si_getServiceNumberForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* service_number,
            uint32_t *minor_number);
    mpe_Error (*mpe_si_getSourceIdForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* sourceId);
    mpe_Error (*mpe_si_getFrequencyForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* frequency);
    mpe_Error (*mpe_si_getProgramNumberForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *prog_num);
    mpe_Error
            (*mpe_si_getModulationFormatForServiceHandle_ptr)(
                    mpe_SiServiceHandle service_handle,
                    mpe_SiModulationMode* modFormat);
    mpe_Error (*mpe_si_getServiceDetailsLastUpdateTimeForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, mpe_TimeMillis *time);
    mpe_Error (*mpe_si_getIsFreeFlagForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, mpe_Bool *is_free);
    mpe_Error (*mpe_si_getNumberOfServiceDescriptionsForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *num_descriptions);
    mpe_Error (*mpe_si_getServiceDescriptionsForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, char* languages[],
            char *descriptions[]);
    mpe_Error
            (*mpe_si_getServiceDescriptionLastUpdateTimeForServiceHandle_ptr)(
                    mpe_SiServiceHandle service_handle, mpe_TimeMillis *time);
    mpe_Error (*mpe_si_getNumberOfComponentsForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *num_components);
    mpe_Error (*mpe_si_getAllComponentsForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle,
            mpe_SiServiceComponentHandle comp_handle[],
            uint32_t* num_components);
    mpe_Error (*mpe_si_getPcrPidForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pcr_pid);
    mpe_Error (*mpe_si_getPATVersionForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pat_version);
    mpe_Error (*mpe_si_getPMTVersionForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pmt_version);
    mpe_Error (*mpe_si_getPATCRCForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pat_crc);
    mpe_Error (*mpe_si_getPMTCRCForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* pmt_crc);
    mpe_Error (*mpe_si_getDeliverySystemTypeForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, mpe_SiDeliverySystemType* type);
    mpe_Error (*mpe_si_getCASystemIdArrayLengthForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t *length);
    mpe_Error (*mpe_si_getCASystemIdArrayForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* ca_array,
            uint32_t *length);
    mpe_Error (*mpe_si_getMultipleInstancesFlagForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, mpe_Bool* hasMultipleInstances);
    mpe_Error (*mpe_si_getOuterDescriptorsForServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t* numDescriptors,
            mpe_SiMpeg2DescriptorList** descriptors);
    mpe_Error (*mpe_si_getNumberOfServiceDetailsForServiceHandle_ptr)(
            mpe_SiServiceHandle serviceHandle, uint32_t* length);
    mpe_Error (*mpe_si_getServiceDetailsForServiceHandle_ptr)(
            mpe_SiServiceHandle serviceHandle,
            mpe_SiServiceDetailsHandle arr[], uint32_t length);
    mpe_Error (*mpe_si_getAppIdForServiceHandle_ptr)(
            mpe_SiServiceHandle serviceHandle, uint32_t *appId);
    mpe_Error (*mpe_si_ReleaseServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle);
    mpe_Error (*mpe_si_SetSourceId_ptr)(mpe_SiServiceHandle service_handle,
            uint32_t sourceId);
    mpe_Error (*mpe_si_UpdateFreqProgNumByServiceHandle_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t freq,
            uint32_t prog_num, uint32_t mod);

    // service details handle specific
    mpe_Error (*mpe_si_getIsFreeFlagForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, mpe_Bool* is_free);
    mpe_Error (*mpe_si_getSourceIdForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t* sourceId);
    mpe_Error (*mpe_si_getFrequencyForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t* frequency);
    mpe_Error (*mpe_si_getProgramNumberForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t* progNum);
    mpe_Error (*mpe_si_getModulationFormatForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle,
            mpe_SiModulationMode* modFormat);
    mpe_Error (*mpe_si_getServiceTypeForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, mpe_SiServiceType* type);
    mpe_Error (*mpe_si_getNumberOfLongNamesForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t *num_long_names);
    mpe_Error (*mpe_si_getLongNamesForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, char* languages[],
            char* longNames[]);
    mpe_Error (*mpe_si_getDeliverySystemTypeForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle,
            mpe_SiDeliverySystemType* type);
    mpe_Error (*mpe_si_getServiceInformationTypeForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle,
            mpe_SiServiceInformationType* type);
    mpe_Error
            (*mpe_si_getServiceDetailsLastUpdateTimeForServiceDetailsHandle_ptr)(
                    mpe_SiServiceDetailsHandle comp_handle,
                    mpe_TimeMillis* lastUpdate);
    mpe_Error (*mpe_si_getCASystemIdArrayLengthForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t* length);
    mpe_Error (*mpe_si_getCASystemIdArrayForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle, uint32_t* ca_array,
            uint32_t length);
    mpe_Error (*mpe_si_getTransportStreamHandleForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle,
            mpe_SiTransportStreamHandle *ts_handle);
    mpe_Error (*mpe_si_getServiceHandleForServiceDetailsHandle_ptr)(
            mpe_SiServiceDetailsHandle comp_handle,
            mpe_SiServiceHandle* serviceHandle);

    // service component handle specific
    mpe_Error (*mpe_si_getServiceComponentHandleByPid_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t component_Pid,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_getServiceComponentHandleByName_ptr)(
            mpe_SiServiceHandle service_handle, char *component_name,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_getServiceComponentHandleByTag_ptr)(
            mpe_SiServiceHandle service_handle, int component_tag,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_getServiceComponentHandleByAssociationTag_ptr)(
            mpe_SiServiceHandle service_handle, uint16_t association_tag,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_getServiceComponentHandleByCarouselId_ptr)(
            mpe_SiServiceHandle service_handle, uint32_t carousel_id,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_getServiceComponentHandleForDefaultCarousel_ptr)(
            mpe_SiServiceHandle service_handle,
            mpe_SiServiceComponentHandle *comp_handle);
    mpe_Error (*mpe_si_releaseServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle);

    mpe_Error (*mpe_si_getPidForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, uint32_t *comp_pid);
    mpe_Error (*mpe_si_getNumberOfNamesForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, uint32_t *numberOfNames);
    mpe_Error (*mpe_si_getNamesForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, char* languages[],
            char* comp_names[]);
    mpe_Error (*mpe_si_getComponentTagForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, int *comp_tag);
    mpe_Error (*mpe_si_getAssociationTagForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, uint16_t *assoc_tag);
    mpe_Error (*mpe_si_getCarouselIdForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, uint32_t *carousel_id);
    mpe_Error (*mpe_si_getLanguageForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, char **comp_lang);
    mpe_Error (*mpe_si_getStreamTypeForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle,
            mpe_SiElemStreamType *stream_type);
    mpe_Error
            (*mpe_si_getComponentLastUpdateTimeForServiceComponentHandle_ptr)(
                    mpe_SiServiceComponentHandle comp_handle,
                    mpe_TimeMillis *time);
    mpe_Error (*mpe_si_getServiceInformationTypeForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle,
            mpe_SiServiceInformationType* serviceInformationType);
    mpe_Error (*mpe_si_getDescriptorsForServiceComponentHandle_ptr)(
            mpe_SiServiceComponentHandle comp_handle, uint32_t* numDescriptors,
            mpe_SiMpeg2DescriptorList** descriptors);
    mpe_Error (*mpe_si_getServiceDetailsHandleForServiceComponentHandle)(
            mpe_SiServiceComponentHandle handle,
            mpe_SiServiceDetailsHandle* serviceDetailsHandle);
    mpe_Error (*mpe_si_getServiceHandleForServiceComponentHandle)(
            mpe_SiServiceComponentHandle handle,
            mpe_SiServiceHandle* serviceHandle);

    mpe_Error (*mpe_si_getTotalNumberOfServices_ptr)(uint32_t *num_services);
    mpe_Error (*mpe_si_getAllServices_ptr)(
            mpe_SiServiceHandle array_services[], uint32_t* num_services);

    mpe_Error (*mpe_si_registerForSIEvents_ptr)(mpe_EdEventInfo* edHandle,
            uint32_t frequency, uint32_t programNumber);
    mpe_Error (*mpe_si_unRegisterForSIEvents_ptr)(mpe_EdEventInfo* edHandle);

    mpe_Error (*mpe_si_getNumberOfSupportedRatingDimensions_ptr)(uint32_t *num);
    mpe_Error (*mpe_si_getSupportedRatingDimensions_ptr)(
            mpe_SiRatingDimensionHandle arr[]);
    mpe_Error (*mpe_si_getNumberOfLevelsForRatingDimensionHandle_ptr)(
            mpe_SiRatingDimensionHandle handle, uint32_t* length);
    mpe_Error (*mpe_si_getNumberOfNamesForRatingDimensionHandle_ptr)(
            mpe_SiRatingDimensionHandle handle, uint32_t *num_names);
    mpe_Error (*mpe_si_getNamesForRatingDimensionHandle_ptr)(
            mpe_SiRatingDimensionHandle handle, char* languages[],
            char* dimensionNames[]);
    mpe_Error (*mpe_si_getNumberOfNamesForRatingDimensionHandleAndLevel_ptr)(
            mpe_SiRatingDimensionHandle handle, uint32_t *num_names,
            uint32_t *num_descriptions, int levelNumber);
    mpe_Error
            (*mpe_si_getDimensionInformationForRatingDimensionHandleAndLevel_ptr)(
                    mpe_SiRatingDimensionHandle handle, char* nameLanguages[],
                    char* names[], char* descriptionLanguages[],
                    char* descriptions[], int levelNumber);
    mpe_Error (*mpe_si_getRatingDimensionHandleByName_ptr)(char* dimensionName,
            mpe_SiRatingDimensionHandle* outHandle);

    mpe_Error (*mpe_si_registerForPSIAcquisition_ptr)(
            mpe_SiServiceHandle service_handle);
    mpe_Error (*mpe_si_unRegisterForPSIAcquisition_ptr)(
            mpe_SiServiceHandle service_handle);

    mpe_Error (*mpe_si_registerForHNPSIAcquisition_ptr)(
            mpe_HnStreamSession session, mpe_SiServiceHandle *service_handle,
            mpe_SiTransportStreamHandle *ts_handle);
    mpe_Error (*mpe_si_unRegisterForHNPSIAcquisition_ptr)(
            mpe_HnStreamSession session);

    mpe_Error (*mpe_si_getNumberOfServiceEntriesForSourceId_ptr)(uint32_t sourceId,
            uint32_t *num_entries);
    mpe_Error (*mpe_si_getAllServiceHandlesForSourceId_ptr)(uint32_t sourceId,
    		mpe_SiServiceHandle service_handle[],
    		uint32_t* num_entries);

} mpe_si_ftable_t;

// OC specific
mpe_Error mpe_siGetPidByAssociationTag(mpe_SiServiceHandle service_handle,
        uint16_t tag, uint32_t *target_pid);
mpe_Error mpe_siGetPidByCarouselID(mpe_SiServiceHandle service_handle,
        uint32_t carousel_id, uint32_t *target_pid);
mpe_Error mpe_siGetPidByComponentTag(mpe_SiServiceHandle service_handle,
        int component_tag, uint32_t *component_pid);
mpe_Error mpe_siGetProgramNumberByDeferredAssociationTag(
        mpe_SiServiceHandle service_handle, uint16_t tag,
        uint32_t *program_number);

/* Used by OC only!!*/
mpe_Error mpe_siRegisterQueueForSIEvents(mpe_EventQueue queueId);
mpe_Error mpe_siUnRegisterQueue(mpe_EventQueue queueId);

/* DSG specific */
mpe_Error mpe_siRegisterForPSIAcquisition(mpe_SiServiceHandle service_handle);
mpe_Error mpe_siUnRegisterForPSIAcquisition(mpe_SiServiceHandle service_handle);
mpe_Error mpe_siIsServiceHandleAppType(mpe_SiServiceHandle service_handle,
        mpe_Bool *isAppType);

/* HN specific */
mpe_Error mpe_siRegisterForHNPSIAcquisition(mpe_HnStreamSession session,
                                            mpe_SiServiceHandle *service_handle,
                                            mpe_SiTransportStreamHandle *ts_handle);
mpe_Error mpe_siUnRegisterForHNPSIAcquisition(mpe_HnStreamSession session);

// lock/unlock
mpe_Error mpe_siLockForRead(void);
mpe_Error mpe_siUnLock(void);

mpe_Error mpe_siGetTotalNumberOfTransports(uint32_t *num_transports);
mpe_Error mpe_siGetAllTransports(mpe_SiTransportHandle array_transports[],
        uint32_t* num_transports);

// transport handle specific
mpe_Error mpe_siGetTransportDeliverySystemType(
        mpe_SiTransportHandle transport_handle,
        mpe_SiDeliverySystemType *delivery_type);
mpe_Error mpe_siGetNumberOfNetworksForTransportHandle(
        mpe_SiTransportHandle transport_handle, uint32_t *num_networks);
mpe_Error mpe_siGetAllNetworksForTransportHandle(
        mpe_SiTransportHandle transport_handle,
        mpe_SiNetworkHandle array_network_handle[], uint32_t* num_networks);
mpe_Error
        mpe_siGetNumberOfTransportStreamsForTransportHandle(
                mpe_SiTransportHandle transport_handle,
                uint32_t *num_transport_streams);
mpe_Error mpe_siGetAllTransportStreamsForTransportHandle(
        mpe_SiTransportHandle transport_handle,
        mpe_SiTransportStreamHandle ts_handle[],
        uint32_t* num_transport_streams);
mpe_Error mpe_siGetNetworkHandleByTransportHandleAndNetworkId(
        mpe_SiTransportHandle transport_handle, uint32_t network_id,
        mpe_SiNetworkHandle* network_handle);
mpe_Error mpe_siGetTransportIdForTransportHandle(
        mpe_SiTransportHandle transport_handle, uint32_t* transport_id);
mpe_Error mpe_siGetTransportHandleByTransportId(uint32_t transportId,
        mpe_SiTransportHandle* outHandle);

// network handle specific
mpe_Error mpe_siGetNetworkServiceInformationType(
        mpe_SiNetworkHandle network_handle,
        mpe_SiServiceInformationType *service_info_type);
mpe_Error mpe_siGetNetworkIdForNetworkHandle(
        mpe_SiNetworkHandle network_handle, uint32_t *network_id);
mpe_Error mpe_siGetNetworkNameForNetworkHandle(
        mpe_SiNetworkHandle network_handle, char **network_name);
mpe_Error mpe_siGetNetworkLastUpdateTimeForNetworkHandle(
        mpe_SiNetworkHandle network_handle, mpe_TimeMillis *time);
mpe_Error mpe_siGetNumberOfTransportStreamsForNetworkHandle(
        mpe_SiNetworkHandle network_handle, uint32_t *num_transport_streams);
mpe_Error mpe_siGetAllTransportStreamsForNetworkHandle(
        mpe_SiNetworkHandle network_handle,
        mpe_SiTransportStreamHandle transport_stream_handle[],
        uint32_t* num_transport_streams);
mpe_Error mpe_siGetTransportHandleForNetworkHandle(
        mpe_SiNetworkHandle network_handle,
        mpe_SiTransportHandle* transport_handle);

// program info handle specific
mpe_Error mpe_siGetPMTVersionForProgramHandle(
        mpe_SiProgramHandle program_handle, uint32_t* pmt_version);
mpe_Error mpe_siGetPMTCRCForProgramHandle(mpe_SiServiceHandle program_handle,
        uint32_t* pmt_crc);

// transport stream handle specific
mpe_Error mpe_siGetTransportStreamIdForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t *ts_id);
mpe_Error mpe_siGetDescriptionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, char **description);
mpe_Error mpe_siGetTransportStreamNameForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, char **ts_name);
mpe_Error mpe_siGetFrequencyForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* freq);
mpe_Error mpe_siGetModulationFormatForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, mpe_SiModulationMode *mode);
mpe_Error mpe_siGetTransportStreamServiceInformationType(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiServiceInformationType* serviceInformationType);
mpe_Error mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, mpe_TimeMillis *time);
mpe_Error mpe_siGetNumberOfServicesForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t *num_services);
mpe_Error mpe_siGetAllServicesForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiServiceHandle service_handle[], uint32_t* num_services);
mpe_Error mpe_siGetNetworkHandleForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiNetworkHandle* network_handle);
mpe_Error mpe_siGetTransportHandleForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiTransportHandle* transport_handle);
mpe_Error mpe_siGetTransportStreamHandleByTransportFrequencyModulationAndTSID(
        mpe_SiTransportHandle transport_handle, uint32_t frequency,
        uint32_t mode, uint32_t ts_id, mpe_SiTransportStreamHandle* ts_handle);
mpe_Error mpe_siGetPATCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* pat_crc);
mpe_Error mpe_siGetPATVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* pat_version);

// service handle specific
mpe_Error mpe_siGetServiceHandleByServiceNumber(uint32_t majorNumber,
        uint32_t minorNumber, mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetServiceHandleBySourceId(uint32_t sourceId,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetServiceHandleByAppId(uint32_t appId,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetServiceHandleByFrequencyModulationProgramNumber(
        uint32_t freq, uint32_t mode, uint32_t prog_num,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetServiceHandleByServiceName(char* service_name,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siCreateDynamicServiceHandle(uint32_t freq, uint32_t prog_num,
        mpe_SiModulationMode modFormat, mpe_SiServiceHandle *service_handle);
mpe_Error
        mpe_siCreateDSGServiceHandle(uint32_t appId, uint32_t prog_num,
                char* service_name, char* language,
                mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siCreateHNstreamServiceHandle(mpe_HnStreamSession session, uint32_t prog_num,
                char* service_name, char* language,
                mpe_SiServiceHandle *service_handle);

// service handle based queries
mpe_Error mpe_siGetTransportStreamHandleForServiceHandle(
        mpe_SiServiceHandle service_handle,
        mpe_SiTransportStreamHandle *ts_handle);
mpe_Error mpe_siGetPMTPidForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pmt_pid);
mpe_Error mpe_siGetServiceTypeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiServiceType *service_type);
mpe_Error mpe_siGetNumberOfLongNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_long_names);
mpe_Error mpe_siGetLongNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, char* languages[],
        char* service_long_names[]);
mpe_Error mpe_siGetNumberOfNamesForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_names);
mpe_Error mpe_siGetNamesForServiceHandle(mpe_SiServiceHandle service_handle,
        char* languages[], char* service_names[]);
mpe_Error mpe_siGetServiceNumberForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* service_number,
        uint32_t *minor_number);
mpe_Error mpe_siGetSourceIdForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* sourceId);
mpe_Error mpe_siGetFrequencyForServiceHandle(mpe_SiServiceHandle handle,
        uint32_t* frequency);
mpe_Error mpe_siGetProgramNumberForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *prog_num);
mpe_Error mpe_siGetModulationFormatForServiceHandle(mpe_SiServiceHandle handle,
        mpe_SiModulationMode* modFormat);
mpe_Error mpe_siGetServiceDetailsLastUpdateTimeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_TimeMillis *time);
mpe_Error mpe_siGetIsFreeFlagForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_Bool *is_free);
mpe_Error mpe_siGetNumberOfServiceDescriptionsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_descriptions);
mpe_Error mpe_siGetServiceDescriptionsForServiceHandle(
        mpe_SiServiceHandle service_handle, char* languages[],
        char *descriptions[]);
mpe_Error mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_TimeMillis *time);
mpe_Error mpe_siGetNumberOfComponentsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *num_components);
mpe_Error mpe_siGetAllComponentsForServiceHandle(
        mpe_SiServiceHandle service_handle,
        mpe_SiServiceComponentHandle comp_handle[], uint32_t* num_components);
mpe_Error mpe_siGetPcrPidForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pcr_pid);
mpe_Error mpe_siGetPATVersionForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* pat_version);
mpe_Error mpe_siGetPMTVersionForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* pmt_version);
mpe_Error mpe_siGetPATCRCForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pat_crc);
mpe_Error mpe_siGetPMTCRCForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* pmt_crc);
mpe_Error mpe_siGetDeliverySystemTypeForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_SiDeliverySystemType* type);
mpe_Error mpe_siGetCASystemIdArrayLengthForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t *length);
mpe_Error mpe_siGetCASystemIdArrayForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* ca_array,
        uint32_t *length);
mpe_Error mpe_siGetMultipleInstancesFlagForServiceHandle(
        mpe_SiServiceHandle service_handle, mpe_Bool* hasMultipleInstances);
mpe_Error mpe_siGetOuterDescriptorsForServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t* numDescriptors,
        mpe_SiMpeg2DescriptorList** descriptors);
mpe_Error mpe_siGetNumberOfServiceDetailsForServiceHandle(
        mpe_SiServiceHandle serviceHandle, uint32_t* length);
mpe_Error mpe_siGetServiceDetailsForServiceHandle(
        mpe_SiServiceHandle serviceHandle, mpe_SiServiceDetailsHandle arr[],
        uint32_t length);
mpe_Error mpe_siGetAppIdForServiceHandle(mpe_SiServiceHandle service_handle,
        uint32_t* appId);
mpe_Error mpe_siReleaseServiceHandle(mpe_SiServiceHandle service_handle);
mpe_Error mpe_siSetSourceId(mpe_SiServiceHandle service_handle,
        uint32_t sourceId);
void mpe_siSetAppId(mpe_SiServiceHandle service_handle, uint32_t appId);
mpe_Error mpe_siUpdateFreqProgNumByServiceHandle(
        mpe_SiServiceHandle service_handle, uint32_t freq, uint32_t prog_num,
        uint32_t mod);

// service details handle based queries
mpe_Error mpe_siGetIsFreeFlagForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_Bool* is_free);
mpe_Error mpe_siGetSourceIdForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* sourceId);
mpe_Error mpe_siGetFrequencyForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* frequency);
mpe_Error mpe_siGetProgramNumberForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* progNum);
mpe_Error mpe_siGetModulationFormatForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiModulationMode* modFormat);
mpe_Error mpe_siGetServiceTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceType* type);
mpe_Error mpe_siGetNumberOfLongNamesForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle comp_handle, uint32_t *num_long_names);
mpe_Error
        mpe_siGetLongNamesForServiceDetailsHandle(
                mpe_SiServiceDetailsHandle handle, char* languages[],
                char* longNames[]);
mpe_Error mpe_siGetDeliverySystemTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiDeliverySystemType* type);
mpe_Error mpe_siGetServiceInformationTypeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceInformationType* type);
mpe_Error mpe_siGetServiceDetailsLastUpdateTimeForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_TimeMillis* lastUpdate);
mpe_Error mpe_siGetCASystemIdArrayLengthForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* length);
mpe_Error mpe_siGetCASystemIdArrayForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, uint32_t* ca_array, uint32_t length);
mpe_Error mpe_siGetTransportStreamHandleForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle,
        mpe_SiTransportStreamHandle *ts_handle);
mpe_Error mpe_siGetServiceHandleForServiceDetailsHandle(
        mpe_SiServiceDetailsHandle handle, mpe_SiServiceHandle* serviceHandle);

// service component handle specific
mpe_Error mpe_siGetServiceComponentHandleByPid(
        mpe_SiServiceDetailsHandle service_handle, uint32_t component_Pid,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siGetServiceComponentHandleByName(
        mpe_SiServiceDetailsHandle service_handle, char *component_name,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siGetServiceComponentHandleByTag(
        mpe_SiServiceDetailsHandle service_handle, int component_tag,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siGetServiceComponentHandleByAssociationTag(
        mpe_SiServiceHandle service_handle, uint16_t association_tag,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siGetServiceComponentHandleByCarouselId(
        mpe_SiServiceDetailsHandle service_handle, uint32_t carousel_id,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siGetServiceComponentHandleForDefaultCarousel(
        mpe_SiServiceDetailsHandle service_handle,
        mpe_SiServiceComponentHandle *comp_handle);
mpe_Error mpe_siReleaseServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle);

// service component handle based queries
mpe_Error mpe_siGetPidForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *comp_pid);
mpe_Error mpe_siGetNumberOfNamesForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *numberOfNames);
mpe_Error mpe_siGetNamesForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, char* languages[],
        char* comp_names[]);
mpe_Error mpe_siGetComponentTagForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, int *comp_tag);
mpe_Error mpe_siGetAssociationTagForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint16_t *assoc_tag);
mpe_Error mpe_siGetCarouselIdForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t *carousel_id);
mpe_Error mpe_siGetLanguageForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, char **comp_lang);
mpe_Error mpe_siGetStreamTypeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiElemStreamType *stream_type);
mpe_Error mpe_siGetComponentLastUpdateTimeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, mpe_TimeMillis *time);
mpe_Error mpe_siGetServiceInformationTypeForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceInformationType* serviceInformationType);
mpe_Error mpe_siGetDescriptorsForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle, uint32_t* num_descriptors,
        mpe_SiMpeg2DescriptorList** descriptors);
mpe_Error mpe_siGetServiceDetailsHandleForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceDetailsHandle* serviceDetailsHandle);
mpe_Error mpe_siGetServiceHandleForServiceComponentHandle(
        mpe_SiServiceComponentHandle comp_handle,
        mpe_SiServiceHandle* serviceHandle);

mpe_Error mpe_siGetTotalNumberOfServices(uint32_t *num_services);
mpe_Error mpe_siGetAllServices(mpe_SiServiceHandle array_services[],
        uint32_t* num_services);

mpe_Error mpe_siRegisterForSIEvents(mpe_EdEventInfo *edHandle,
        uint32_t frequency, uint32_t programNumber);
mpe_Error mpe_siUnRegisterForSIEvents(mpe_EdEventInfo *edHandle);

// Rating dimension specific
mpe_Error mpe_siGetNumberOfSupportedRatingDimensions(uint32_t *num);
mpe_Error mpe_siGetSupportedRatingDimensions(mpe_SiRatingDimensionHandle arr[]);
mpe_Error mpe_siGetNumberOfLevelsForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, uint32_t *length);
mpe_Error mpe_siGetNumberOfNamesForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, uint32_t *num_names);
mpe_Error mpe_siGetNamesForRatingDimensionHandle(
        mpe_SiRatingDimensionHandle handle, char* languages[],
        char* dimensionNames[]);
mpe_Error mpe_siGetNumberOfNamesForRatingDimensionHandleAndLevel(
        mpe_SiRatingDimensionHandle handle, uint32_t *num_names,
        uint32_t *num_descriptions, int levelNumber);
mpe_Error mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel(
        mpe_SiRatingDimensionHandle handle, char* nameLanguages[],
        char* names[], char* descriptionLanguages[], char* descriptions[],
        int levelNumber);
mpe_Error mpe_siGetRatingDimensionHandleByName(char* dimensionName,
        mpe_SiRatingDimensionHandle* outHandle);

/* These methods are called from Table Parser layer to populate SI data as
 various tables are acquired and parsed */
/* SVCT */
mpe_Error mpe_siGetServiceEntryFromSourceId(uint32_t sourceId,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetServiceEntryFromSourceIdAndChannel(uint32_t sourceId,
        uint32_t major_number, uint32_t minor_number,
        mpe_SiServiceHandle *service_handle, mpe_SiTransportHandle ts_handle,
        mpe_SiProgramHandle pi_handle);
void mpe_siGetFrequencyFromCDSRef(uint8_t cds_ref, uint32_t *frequency);
void mpe_siGetModulationFromMMSRef(uint8_t mms_ref,
        mpe_SiModulationMode *modulation);
/* PAT, PMT */

void mpe_siLockForWrite(void);
void mpe_siReleaseWriteLock(void);
mpe_Error mpe_siGetServiceEntryFromFrequencyProgramNumberModulationAppId(
        uint32_t freq, uint32_t program_number, mpe_SiModulationMode mode,
        uint32_t app_id, mpe_SiServiceHandle *si_entry_handle);
mpe_Error mpe_siGetTransportStreamEntryFromFrequencyModulation(uint32_t freq,
        mpe_SiModulationMode mode, mpe_SiTransportStreamHandle *ts_handle);
mpe_Error mpe_siGetServiceEntryFromFrequencyProgramNumberModulation(
        uint32_t frequency, uint32_t program_number, mpe_SiModulationMode mode,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetProgramEntryFromFrequencyProgramNumberModulation(
        uint32_t frequency, uint32_t program_number, mpe_SiModulationMode mode,
        mpe_SiProgramHandle *service_handle);
mpe_Error mpe_siGetProgramEntryFromTransportStreamEntry(
        mpe_SiTransportStreamHandle ts_handle, uint32_t program_number,
        mpe_SiProgramHandle *prog_handle);
mpe_Error mpe_siGetServiceEntryFromTransportStreamEntry(
        mpe_SiTransportStreamHandle ts_handle,
        mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetNumberOfServiceEntriesForSourceId(uint32_t sourceId,
        uint32_t *num_entries);
mpe_Error mpe_siGetAllServiceHandlesForSourceId(uint32_t sourceId,
		mpe_SiServiceHandle service_handle[],
		uint32_t* num_entries);
void mpe_siInsertServiceEntryForChannelNumber(uint16_t channel_number,
        mpe_SiServiceHandle *service_handle);
void mpe_siGetServiceEntryFromChannelNumber(uint16_t channel_number,
        mpe_SiServiceHandle *service_handle);
void mpe_siUpdateServiceEntry(mpe_SiServiceHandle service_handle,
        mpe_SiTransportHandle ts_handle, mpe_SiProgramHandle pi_handle);
mpe_Error mpe_siGetServiceEntryFromAppIdProgramNumber(uint32_t appId,
        uint32_t program_number, mpe_SiServiceHandle *service_handle);
mpe_Error mpe_siGetProgramEntryFromAppId(mpe_SiTransportStreamHandle ts_handle,
        uint32_t appId, mpe_SiProgramHandle *pi_handle);
mpe_Error mpe_siGetTransportStreamEntryFromAppId(uint32_t appId,
        mpe_SiTransportStreamHandle *ts_handle);
uint32_t mpe_siGetDSGTransportStreamHandle(void);
mpe_Error mpe_siReleaseServiceEntry(mpe_SiServiceHandle service_handle);
mpe_Error mpe_siGetProgramEntryFromTransportStreamAppIdProgramNumber(
        mpe_SiTransportStreamHandle ts_handle, uint32_t appId,
        uint32_t program_number, mpe_SiProgramHandle *pi_handle);

mpe_Error
        mpe_siGetProgramHandleFromServiceEntry(
                mpe_SiServiceHandle service_handle,
                mpe_SiProgramHandle *program_handle);

uint32_t mpe_siGetHNTransportStreamHandle(void);

mpe_Error mpe_siGetServiceEntryFromHNstreamProgramNumber(mpe_HnStreamSession session,
        uint32_t program_number, mpe_SiServiceHandle *service_handle);

mpe_Error mpe_siGetProgramEntryFromTransportStreamHNstreamProgramNumber(
        mpe_SiTransportStreamHandle ts_handle, mpe_HnStreamSession session,
        uint32_t program_number, mpe_SiProgramHandle *pi_handle);

/**
 Set Methods: To populate individual fields from various tables
 */

/* SVCT */
mpe_Error mpe_siSetProgramNumber(mpe_SiServiceHandle service_handle,
        uint32_t prog_num);
mpe_Error mpe_siSetSourceId(mpe_SiServiceHandle service_handle,
        uint32_t sourceId);
void mpe_siSetAppType(mpe_SiServiceHandle service_handle, mpe_Bool bIsApp);
void mpe_siGetServiceEntryState(mpe_SiServiceHandle service_handle,
        uint32_t *state);
void mpe_siSetServiceEntryState(mpe_SiServiceHandle service_handle,
        uint32_t state);
void mpe_siSetServiceEntryStateDefined(mpe_SiServiceHandle service_handle);
void mpe_siSetServiceEntryStateUnDefined(mpe_SiServiceHandle service_handle);
void mpe_siSetServiceEntryStateMapped(mpe_SiServiceHandle service_handle);
void mpe_siSetServiceEntriesStateDefined(uint16_t channel_number);
void mpe_siSetServiceEntriesStateUnDefined(uint16_t channel_number);
void mpe_siSetActivationTime(mpe_SiServiceHandle service_handle,
        uint32_t activation_time);
mpe_Error mpe_siSetChannelNumber(mpe_SiServiceHandle service_handle,
        uint32_t major_number, uint32_t minor_number);
void mpe_siSetCDSRef(mpe_SiServiceHandle service_handle, uint8_t cds_ref);
void mpe_siSetMMSRef(mpe_SiServiceHandle service_handle, uint8_t mms_ref);
void mpe_siSetChannelType(mpe_SiServiceHandle service_handle,
        uint8_t channel_type);
void mpe_siSetVideoStandard(mpe_SiServiceHandle service_handle,
        mpe_SiVideoStandard video_std);
void mpe_siSetServiceType(mpe_SiServiceHandle service_handle,
        mpe_SiServiceType service_type);
void mpe_siSetAnalogMode(mpe_SiServiceHandle service_handle,
        mpe_SiModulationMode mode);
mpe_Error mpe_siSetSVCTDescriptor(mpe_SiServiceHandle service_handle,
        mpe_SiDescriptorTag tag, uint32_t length, void *content);
mpe_Bool mpe_siIsVCMComplete(void);
mpe_Error mpe_siSetTransportType(mpe_SiServiceHandle service_handle, uint8_t transportType);
mpe_Error mpe_siSetScrambled(mpe_SiServiceHandle service_handle, mpe_Bool scrambled);

void mpe_siUpdateServiceEntries();

/* NTT_SNS / CVCT */
mpe_Error mpe_siSetSourceName(mpe_siSourceNameEntry *source_name_entry,
        char *sourceName, char *language, mpe_Bool override_existing);

/* NTT_SNS */
mpe_Error mpe_siSetSourceLongName(mpe_siSourceNameEntry *source_name_entry,
        char *sourceLongName, char *language);
//mpe_Error mpe_siSetSourceNameForSourceId(uint32_t source_id, char *sourceName,
//        char *language, mpe_Bool override_existing);
//mpe_Error mpe_siSetSourceLongNameForSourceId(uint32_t source_id,
//        char *sourceName, char *language);
mpe_Error mpe_siGetSourceNameEntry(uint32_t id, mpe_Bool isAppId,  mpe_siSourceNameEntry **source_name_entry, mpe_Bool create);
mpe_Bool mpe_siIsSourceNameLanguagePresent(mpe_siSourceNameEntry *source_name_entry, char *language);

/* PAT */
void mpe_siSetTSIdForTransportStream(mpe_SiTransportStreamHandle ts_handle,
        uint32_t tsId);
void mpe_siSetTSIdStatusNotAvailable(mpe_SiTransportStreamHandle ts_handle);
void mpe_siSetTSIdStatusAvailableShortly(mpe_SiTransportStreamHandle ts_handle);
void mpe_siRevertTSIdStatus(mpe_SiTransportHandle ts_handle);
void mpe_siSetPATVersionForTransportStream(
        mpe_SiTransportStreamHandle ts_handle, uint32_t pat_version);
void mpe_siSetPATCRCForTransportStream(mpe_SiTransportStreamHandle ts_handle,
        uint32_t crc);
mpe_Error mpe_siSetPATProgramForTransportStream(
        mpe_SiTransportStreamHandle ts_handle, uint32_t program_number,
        uint32_t pmt_pid);
mpe_Error mpe_siSetTSId(mpe_SiServiceHandle service_handle, uint32_t tsId);
void mpe_siSetPATVersion(mpe_SiTransportStreamHandle ts_handle,
        uint32_t pat_version);
void mpe_siSetPMTPid(mpe_SiProgramHandle program_handle, uint32_t pmt_pid);
//mpe_Error mpe_siSetPATPrograms (mpe_SiHandle si_handle, uint32_t pmt_pid);

mpe_Error mpe_siSetPAT(mpe_SiServiceHandle service_handle, uint32_t length,
        uint32_t* pat_programs_array);
void mpe_siSetPATStatusNotAvailable(
        mpe_SiTransportStreamHandle transport_handle);
void mpe_siSetPATStatusAvailable(mpe_SiTransportStreamHandle transport_handle);
void mpe_siResetPATProgramStatus(mpe_SiTransportHandle ts_handle);

/* CVCT */
mpe_Error mpe_siGetCVCTVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* cvct_version);
mpe_Error mpe_siGetCVCTCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t* cvct_crc);
void mpe_siSetCVCTVersionForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t cvct_version);
void mpe_siSetCVCTCRCForTransportStreamHandle(
        mpe_SiTransportStreamHandle ts_handle, uint32_t cvct_crc);

/* PMT */
void mpe_siSetPCRPid(mpe_SiProgramHandle program_handle, uint32_t pcr_pid);
mpe_Error mpe_siSetPMTVersion(mpe_SiProgramHandle program_handle,
        uint32_t pmt_version);
mpe_Error mpe_siSetOuterDescriptor(mpe_SiProgramHandle program_handle,
        mpe_SiDescriptorTag tag, uint32_t length, void *content);
mpe_Error mpe_siSetDescriptor(mpe_SiProgramHandle program_handle,
        uint32_t elem_pid, mpe_SiElemStreamType type, mpe_SiDescriptorTag tag,
        uint32_t length, void *content);
void mpe_siSetESInfoLength(mpe_SiProgramHandle program_handle,
        uint32_t elem_pid, mpe_SiElemStreamType type, uint32_t es_info_length);
mpe_Error mpe_siSetElementaryStream(mpe_SiProgramHandle program_handle,
        mpe_SiElemStreamType stream_type, uint32_t elem_pid);

mpe_Error mpe_siSetPMT(mpe_SiServiceHandle service_handle, uint32_t length,
        uint8_t* pmt_array);

void mpe_siSetPMTStatus(mpe_SiProgramHandle program_handle);
void mpe_siSetPMTStatusAvailableShortly(mpe_SiTransportStreamHandle ts_handle);
mpe_Error mpe_siSetPMTStatusNotAvailable(mpe_SiProgramHandle pi_handle);
mpe_Error mpe_siSetPMTProgramStatusNotAvailable(mpe_SiProgramHandle pi_handle);
mpe_Error mpe_siGetPMTProgramStatus(mpe_SiProgramHandle pi_handle, uint8_t *pmt_status);
mpe_Error mpe_siRevertPMTStatus(mpe_SiProgramHandle pi_handle);
void mpe_siSetPMTCRC(mpe_SiProgramHandle pi_handle, uint32_t new_crc);

/* NIT_CD */
/* Populate carrier frequencies from NIT_CD table */
/*  Not sure about the signature, Talk to danny about how
 frequencies might be populated */
mpe_Error mpe_siSetCarrierFrequencies(uint32_t frequency[], uint8_t offset,
        uint8_t length);

/* NIT_MM */
/* Populate modulation modes from NIT_MM table */
/*  Not sure about the signature, Talk to danny about how
 modes might be populated */
mpe_Error mpe_siSetModulationModes(mpe_SiModulationMode mode[], uint8_t offset,
        uint8_t length);

mpe_Error mpe_siNotifyTableDetected(mpe_SiTableType table_type,
        uint32_t optional_data);
mpe_Error mpe_siNotifyTableChanged(mpe_SiTableType table_type,
        uint32_t changeType, uint32_t optional_data);
mpe_Error mpe_siNotifyTunedAway(uint32_t frequency);

void mpe_siSetGlobalState(mpe_SiGlobalState state);
mpe_Error mpe_siUnsetGlobalState(void);
mpe_Error mpe_siChanMapReset(void);

/* @@@@ TODO @@@@ */
/*  Add methods for NTT_SNS (source name subtable)
 Service(source) name is obtained from this table */

//
// Use these to validate when a handle is held through a time when there were
// (potentially) no readers in the SIDB, so it might have been deleted!
//
// The case I stumbled on, where this occurs, is when an update occurs, and
// SIDatabaseImpl or dcCarousel code is notified through an event.
//
// This begs the question, 'what do we DO in that situation?'
// SIDatabaseImpl should cause the cache to be invalidated,
// OC code should unmount the affected carousel, or signal an
// error up somehow.
//
// These should be called after acquiring the reader/writer lock.
//
mpe_Error mpe_siValidateTransportHandle(mpe_SiTransportHandle transport_handle);
mpe_Error mpe_siValidateServiceHandle(mpe_SiServiceHandle service_handle);

/* iterator for services attached to a program */
//
// TransportStreams's list_lock is held from the return of 'FindFirst' until 'Release' is called.
//
typedef struct publicSIIterator
{
    uint32_t dummy1;
    uint32_t dummy2;
} publicSIIterator; //  Size/alignment must agree with private version, in simgr.c.

typedef struct SIIterator
{
    uint32_t lastReturnedIndex;
    mpe_SiProgramInfo *program;
} SIIterator;

mpe_SiServiceHandle mpe_siFindFirstServiceFromProgram(
        mpe_SiProgramHandle si_program_handle, SIIterator *iter);
mpe_SiServiceHandle mpe_siGetNextServiceFromProgram(SIIterator *iter);

void mpe_SiTakeFromProgram(mpe_SiProgramHandle program_handle,
        mpe_SiServiceHandle service_handle);
void mpe_SiGiveToProgram(mpe_SiProgramHandle program_handle,
        mpe_SiServiceHandle service_handle);

#ifdef __cplusplus
}
;
#endif

#endif /* _SIMGR_H_ */
