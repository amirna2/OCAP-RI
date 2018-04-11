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
#if !defined(_MPEOS_VBI_H)
#define _MPEOS_VBI_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>      /* mpe_Error */
#include <mpeos_event.h>    /* mpe_EventQueue */
#include <mpeos_filter.h>   /* mpe_FilterSpec */
#include <mpeos_media.h>    /* mpe_MediaDecodeSession */
#include <mpeos_dvr.h>      /* mpe_DvrPlayback */

/**
 * @file mpeos_vbi.h
 *
 * Defines all symbols and functions for the MPEOS VBI Filtering API.
 *
 */

/*
 * Events generated from VBI filter component.
 */
#define MPE_VBI_EVENT_UNKNOWN                        (0x00001200)
#define MPE_VBI_EVENT_FIRST_DATAUNIT                 (MPE_VBI_EVENT_UNKNOWN + 1)
/**< MPE_VBI_EVENT_FIRST_DATAUNIT is sent by an active VBI filter
 * to the associated filter event queue when a data unit is written to an
 * VBI session's empty buffer. optionalEventData1
 * will contain the originating mpe_VBIFilterSession and optionalEventData2
 * will contain the corresponding <i>act</i>. */
#define MPE_VBI_EVENT_DATAUNITS_RECEIVED             (MPE_VBI_EVENT_UNKNOWN + 2)
/**< MPE_VBI_EVENT_DATAUNITS_RECEIVED is sent by an active VBI filter
 * to the associated filter event queue when at least
 * dataUnitNotificationThreshold (MPE_VBI_PARAM_DATA_UNIT_THRESHOLD)
 * bytes are contained in the VBI session's buffer. optionalEventData1
 * will contain the originating mpe_VBIFilterSession and optionalEventData2
 * will contain the corresponding <i>act</i>. */
#define MPE_VBI_EVENT_BUFFER_FULL                    (MPE_VBI_EVENT_UNKNOWN + 3)
/**< MPE_VBI_EVENT_BUFFER_FULL is sent by an active VBI filter to the
 * associated filter event queue when at least bufferSize
 * (MPE_VBI_PARAM_DATABUFFER_SIZE) bytes are contained in the VBI session's
 * buffer. The filtering session is stopped. optionalEventData1
 * will contain the originating mpe_VBIFilterSession and optionalEventData2
 * will contain the corresponding <i>act</i>. */
#define MPE_VBI_EVENT_FILTER_STOPPED                 (MPE_VBI_EVENT_UNKNOWN + 4)
/**< MPE_VBI_EVENT_FILTER_STOPPED is sent by an active VBI filter to the
 * associated filter event queue when the filtering session has not already
 * been put into the stopped state and mpeos_vbiFilterStop()
 * has been called. optionalEventData1 will contain the originating
 * mpe_VBIFilterSession and optionalEventData2 will contain the
 * corresponding <i>act</i>. */
#define MPE_VBI_EVENT_SOURCE_CLOSED                  (MPE_VBI_EVENT_UNKNOWN + 5)
/**< MPE_VBI_EVENT_SOURCE_CLOSED is sent by an active VBI filter to the
 * associated filter event queue when the filtering session is stopped due
 * to termination of the associated decode session. optionalEventData1
 * will contain the originating mpe_VBIFilterSession and optionalEventData2
 * will contain the corresponding <i>act</i>. */
#define MPE_VBI_EVENT_SOURCE_SCRAMBLED               (MPE_VBI_EVENT_UNKNOWN + 6)
/**< MPE_VBI_EVENT_SOURCE_SCRAMBLED is sent by an active VBI filter to the
 * associated filter event queue when the VBISource becomes scrambled -
 * preventing extraction of VBI data. optionalEventData1 will contain the
 * originating mpe_VBIFilterSession and optionalEventData2 will contain the
 * corresponding <i>act</i>. This event does not stop the filtering
 * session. */
#define MPE_VBI_EVENT_OUT_OF_MEMORY                  (MPE_VBI_EVENT_UNKNOWN + 7)
/**< MPE_VBI_EVENT_OUT_OF_MEMORY is sent by an active VBI filter to the
 * associated filter event queue when memory or other resources are depleted.
 * The filtering session is stopped. optionalEventData1
 * will contain the originating mpe_VBIFilterSession and optionalEventData2.
 * will contain the corresponding <i>act</i>. */
#define MPE_VBI_EVENT_FILTER_AVAILABLE               (MPE_VBI_EVENT_UNKNOWN + 8)
/**< MPE_VBI_EVENT_FILTER_AVAILABLE is sent by an active VBI filter to the
 * associated availability queue when memory or other resources are depleted.
 */

/*
 * Return values from VBI filter APIs. Each is defined in the appropriate
 * context.
 */
#define MPE_VBI_ERROR                                    (0x00002200)
#define MPE_VBI_ERROR_FILTER_NOT_AVAILABLE               (MPE_VBI_ERROR + 1)
#define MPE_VBI_ERROR_INVALID_FILTER_SESSION             (MPE_VBI_ERROR + 2)
#define MPE_VBI_ERROR_SOURCE_CLOSED                      (MPE_VBI_ERROR + 3)
#define MPE_VBI_ERROR_SOURCE_SCRAMBLED                   (MPE_VBI_ERROR + 4)
#define MPE_VBI_ERROR_UNSUPPORTED                        (MPE_VBI_ERROR + 5)
#define MPE_VBI_ERROR_SESSION_NOT_AVAILABLE              (MPE_VBI_ERROR + 6)

/**
 * VBI Session type
 */
typedef struct _mpe_VBIFilterSession
{
    int unused1;
}*mpe_VBIFilterSession;

/**
 * VBI Filtering source types
 */
typedef enum mpe_VBISourceType
{
    MPE_VBI_SOURCE_DECODE_SESSION = 1, /**< Source is live decode session */
    MPE_VBI_SOURCE_PLAYBACK_SESSION
/**< Source is a DVR playback session */
} mpe_VBISourceType;

/**
 * Container for all source-type-specific parameters
 */
typedef union mpe_VBISourceParams
{
    mpe_MediaDecodeSession mediaSessionId; /**< Decode session */
    mpe_DvrPlayback playbackSessionId; /**< DVR playback session */
} mpe_VBISourceParams;

/**
 * Target fields for VBI data acquisition
 */
typedef enum mpe_VBIFields
{
    MPE_VBI_FIELD_ODD = 1, /**< Designates the odd field/field 1 of a VBI line */
    MPE_VBI_FIELD_EVEN = 2, /**< Designates the even field/field 2 of a VBI line */
    MPE_VBI_FIELD_MIXED = 3
/**< Designates both the even and odd fields (field 1 and 2) of a VBI line */
} mpe_VBIFields;

/**
 * VBI source parameters
 */
typedef struct mpe_VBISource
{
    uint32_t * vbiLines; /**< Array of VBI line(s) to acquire */
    uint32_t vbiLineCount; /**< Number of elements in vbiLines */
    mpe_VBIFields vbiFields; /**< Field(s) from given line(s) to acquire  */
    mpe_VBISourceType sourceType; /**< Type of the VBI buffer source */
    mpe_VBISourceParams parm; /**< Type-specific parameters */
} mpe_VBISource;

/**
 * Supported VBI data formats
 */
typedef enum mpe_VBIDataFormat
{
    MPE_VBI_FORMAT_XDS = 1, /**< Designates the XDS data format of line 21 field 2 defined by EIA-608-B */
    MPE_VBI_FORMAT_T1 = 2, /**< Designates the Text service T1 format of VBI line 21 field 1 defined by EIA-608-B */
    MPE_VBI_FORMAT_T2 = 3, /**< Designates the Text service T2 format of VBI line 21 field 1 defined by EIA-608-B */
    MPE_VBI_FORMAT_T3 = 4, /**< Designates the Text service T3 format of VBI line 21 field 2 defined by EIA-608-B */
    MPE_VBI_FORMAT_T4 = 5, /**< Designates the Text service T4 format of VBI line 21 field 2 defined by EIA-608-B */
    MPE_VBI_FORMAT_EIA608B_GENERIC = 6, /**< Designates the EIA-608-B generic Character One and Character Two format of line 21 field 1 or 2. */
    MPE_VBI_FORMAT_NABTS = 7, /**< Designates the NABTS data format defined by EIA-516. */
    MPE_VBI_FORMAT_AMOL_I = 8, /**< Designates the AMOL I data format defined by ACN 403-1218-024. */
    MPE_VBI_FORMAT_AMOL_II = 9, /**< Designates the AMOL II data format defined by ACN 403-1218-024 */
    MPE_VBI_FORMAT_UNKNOWN = 10, /**< Designates a non-standard VBI waveform  */
    MPE_VBI_FORMAT_CC1 = 91, /**< Designates the CC1 data of VBI line 21 field 1 defined by EIA-608-B */
    MPE_VBI_FORMAT_CC2 = 92, /**< Designates the CC2 data of VBI line 21 field 1 defined by EIA-608-B */
    MPE_VBI_FORMAT_CC3 = 93, /**< Designates the CC3 data of VBI line 21 field 2 defined by EIA-608-B */
    MPE_VBI_FORMAT_CC4 = 94
/**< Designates the CC4 data of VBI line 21 field 2 defined by EIA-608-B */
} mpe_VBIDataFormat;

/**
 * VBI subsystem parameter identifiers
 */
typedef enum mpe_VBIParameter
{
    MPE_VBI_PARAM_SCTE20LINE21_CAPABILITY = 1,
    MPE_VBI_PARAM_SCTE21LINE21_CAPABILITY,
    MPE_VBI_SEPARATED_FILTERING_CAPABILITY,
    MPE_VBI_MIXED_FILTERING_CAPABILITY
} mpe_VBIParameter;

/**
 * VBI session parameter identifiers
 */
typedef enum mpe_VBISessionParameter
{
    MPE_VBI_PARAM_DATA_UNIT_THRESHOLD,
    MPE_VBI_PARAM_BUFFER_SIZE,
    MPE_VBI_PARAM_BUFFERED_DATA_SIZE
} mpe_VBISessionParameter;

/**
 * VBI Filtering option flags
 */

/** Indicates that the associated operation will only be carried out
 *   if the filter session is not in the Stopped state. */
#define MPE_VBI_OPTION_IF_NOT_STOPPED               (0x00000001)

/** Indicates that the associated operation will clear the associated
 *  data buffer of all contents upon successful completion.  */
#define MPE_VBI_OPTION_CLEAR_BUFFER                 (0x00000002)

/** Indicates that the associated operation will clear only the
 *  read data from the data buffer upon successful completion.  */
#define MPE_VBI_OPTION_CLEAR_READ_DATA              (0x00000004)

/**
 * <i>mpeos_vbiInit()</i> will initialize the VBI filtering system
 *  to prepare it for use. This function will only be called once and no
 *  other methods on this API will be called until this method returns with an
 *  MPE_SUCCESS return value.
 *
 * @return If initialization is successful, <i>mpeos_vbiInit()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return the
 *         following error code:
 *
 *         <i>MPE_VBI_ERROR</i>
 *            indicates that initialization failed.
 */
mpe_Error mpeos_vbiInit(void);

/**
 * <i>mpeos_vbiShutdown()</i> will shutdown the VBI filtering system.
 * This function will only be called once. No other methods on this API will
 * be called after this method returns - regardless of the return code.
 * The VBI buffer filtering subsystem will release as many resources as
 * possible. But outstanding filters do not need to be issued
 * <i>MPE_VBI_EVENT_SOURCE_CLOSED</i> or any other events on shutdown.
 *
 * @return If shutdown is successful, <i>mpeos_vbiShutdown()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return the
 *         following error code:
 *
 *         <i>MPE_VBI_ERROR</i>
 *            indicates that shutdown failed.
 */
mpe_Error mpeos_vbiShutdown(void);

/**
 * <i>mpeos_vbiFilterStart()</i> will initiate VBI data acquisition and filtering.
 *
 * A buffer of the designated buffer size will be allocated for the duration
 * of the VBI filtering and VBI data of the designated format which conforms
 * to the designated filter will be written into the buffer as it arrives.
 *
 * When the first data unit is placed in the VBI session's data buffer,
 * a MPE_VBI_EVENT_FIRST_DATAUNIT will be sent to the queue associated
 * with the filter session. MPE_VBI_EVENT_DATAUNITS_RECEIVED will be
 * sent each time dataUnitNotificationThreshold data units are received,
 * if non-zero.
 *
 * A variety of conditions may stop the filter session - including
 * mpeos_vbiFilterStop() (MPE_VBI_EVENT_FILTER_STOPPED), closing of the VBI
 * source (MPE_VBI_EVENT_SOURCE_CLOSED), or resource depletion
 * (MPE_VBI_EVENT_OUT_OF_MEMORY).
 *
 * If the buffer is filled, filtering will terminate and a
 * MPE_VBI_EVENT_BUFFER_FULL will be sent to the associated event queue.
 *
 * @param filterSource
 *           pointer to a description of the VBI data source. The
 *           filter will operate on VBI data from this source.
 * @param filterSpec
 *           pointer to the filter specification. This defines the
 *           criteria that the filter will evaluate data units against
 *           before placing them in the associated data buffer. If null,
 *           all data units will be acquired.
 * @param queueId
 *           designates the queue in which to deliver filter events.
 * @param act
 *           optional asynchronous completion token to be delivered as
 *           <i>optionalEventData2</i> part of associated events this would be
 *           the ED handle if ED is being used.
 * @param dataFormat
 *           The data format to be retrieved from the VBI data source
 * @param dataUnitSize
 *           The data unit size, in bits, to be used for
 *           dataFormat MPE_VBI_FORMAT_UNKNOWN. Ignored for all other
 *           MPE_VBI_FORMAT values.
 * @param bufferSize
 *           The size of the buffer to be used for storing retrieved VBI
 *           data, in bytes.
 * @param dataUnitNotificationThreshold
 *           The number of received data units that will trigger a
 *           MPE_VBI_EVENT_DATAUNITS_RECEIVED. If 0, this notification
 *           is disabled.
 * @param filterSession
 *           designates a location to store the filtering session handle
 *           used to identify the filter in event notifications and any
 *           subsequent operations.
 * @param flags
 *           Specifies extended options for setting the VBI filter.
 *           will be 0 currently.
 *
 * @return If the call is successful, <i>mpeos_vbiFilterStart()</i> will return
 *         <i>MPE_SUCCESS</i>. Otherwise, it will return one of the following
 *         error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates at least one input parameter has an invalid value.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 *         <i>MPE_VBI_ERROR_FILTER_NOT_AVAILABLE</i>
 *            indicates insufficient filtering resources for the requested
 *            filter specification.
 *         <i>MPE_VBI_ERROR_SOURCE_CLOSED</i>
 *            indicates the specified filter source is not/no longer active.
 *         <i>MPE_VBI_ERROR_SOURCE_SCRAMBLED</i>
 *            indicates the specified filter source cannot be descrabled.
 */
mpe_Error mpeos_vbiFilterStart(mpe_VBISource * filterSource,
        mpe_FilterSpec * filterSpec, mpe_EventQueue queueId, void * act,
        mpe_VBIDataFormat dataFormat, uint32_t dataUnitSize,
        uint32_t bufferSize, uint32_t dataUnitNotificationThreshold,
        uint32_t flags, mpe_VBIFilterSession * filterSession);

/**
 * <i>mpeos_vbiFilterStop</i> will stop the designated filter.
 *  If the filter has not already been put into the stopped state, a
 *  MPE_VBI_EVENT_FILTER_STOPPED event is put in the event queue
 *  associated with filterSession.
 *  Any matched data in the filter's data buffer will be retained
 *  until <i>mpeos_vbiFilterRelease()</i> is called.
 *
 * @param filterSession
 *           identifies the filter to be stopped.
 *
 * @return If the call is successful, <i>mpeos_vbiCancelFilter()</i>
 *         will return <i>MPE_SUCCESS</i>. Otherwise, it will return
 *         the following error code:
 *
 *         <i>MPE_VBI_ERROR_INVALID_FILTER_SESSION</i>
 *            indicates filterSession is invalid.
 */
mpe_Error mpeos_vbiFilterStop(mpe_VBIFilterSession filterSession);

/**
 * <i>mpeos_vbiFilterRelease()</i> will release the filter and any associated
 * resources - including the associated VBI buffer and its contents.
 * If the filter has not already been put into the cancalled state, an
 * <i>MPE_VBI_EVENT_FILTER_STOPPED</i> event is put in the event queue
 * of the filtering request identified by filterSession.
 *
 * @param filterSession
 *           identifies the filter session. filterSession must have been
 *           returned by a previous call to <i>mpeos_vbiFilterStart()</i>.
 *
 * @return If the call is successful, <i>mpeos_vbiFilterRelease()</i>
 *         will return MPE_SUCCESS. Otherwise, it will return the
 *         following error code:
 *
 *         <i>MPE_VBI_ERROR_INVALID_FILTER_SESSION</i>
 *            indicates filterSession is invalid.
 */
mpe_Error mpeos_vbiFilterRelease(mpe_VBIFilterSession filterSession);

/**
 * <i>mpeos_vbiFilterReadData()</i> will copy byteCount,
 *  or (<i>vbi buffer size - offset</i>) if smaller than <i>byteCount</i>,
 *  starting at <i>offset</i> bytes within the VBI buffer. The total number of
 *  bytes read will be stored in <i>*bytesRead</i>, if <i>bytesRead</i>
 *  is non-null.
 *
 * Note that the filtering session's VBI buffer can be cleared by calling
 * mpeos_vbiFilterReadData(session,0,0,MPE_VBI_OPTION_CLEAR_BUFFER,NULL,NULL);
 *
 * @param filterSession
 *           identifies the filtering session. filterSession must have been
 *           returned by a previous call to <i>mpeos_vbiFilterStart()</i>.
 * @param offset
 *           designates the offset to read from within the VBI buffer.
 * @param byteCount
 *           designates the number of bytes to read from the VBI buffer. This can
 *           be larger than the size of the VBI buffer (for example, the target
 *           buffer size).
 * @param flags
 *           Specifies extended options for reading the VBI buffer.
 *
 *           The flag MPE_VBI_OPTION_CLEAR_BUFFER indicates that the entire
 *           VBI buffer associated with the VBI filter session will be cleared
 *           upon a successful read.
 *
 *           The flag MPE_VBI_OPTION_CLEAR_READ_DATA indicates that the
 *           data read from the VBI buffer associated with the VBI filter
 *           session will be cleared upon a successful read. Any data
 *           bytes remaining in the buffer beyond the read data will be
 *           packed.
 * @param buffer
 *           is a pointer to the buffer where the VBI buffer data is copied.
 * @param bytesRead
 *           is a pointer to the destination for the number of bytes actually
 *           copied.
 *
 * @return If the call is successful, <i>mpeos_vbiBufferRead()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            returned if <i>buffer</i> is null, <i>byteCount</i> is 0, or
 *            <i>offset</i> is equal or greater than the VBI size.
 *         <i>MPE_VBI_ERROR_INVALID_FILTER_SESSION</i>
 *            indicates an invalid filterSession handle was specified.
 */
mpe_Error mpeos_vbiFilterReadData(mpe_VBIFilterSession filterSession,
        uint32_t offset, uint32_t byteCount, uint32_t flags, uint8_t * buffer,
        uint32_t * bytesRead);

/**
 * <i>mpeos_vbiFilterSetParam()</i> will set the designated parameter
 * on the VBI filtering session.
 *
 * The following parameters are supported:
 *
 * MPE_VBI_PARAM_DATA_UNIT_THRESHOLD with a value parameter designating
 * the data unit threshold for triggering the MPE_VBI_EVENT_DATAUNITS_RECEIVED
 * notification. A value of 0 disables this notification.
 *
 * @return If the call is successful, <i>mpeos_vbiFilterSetParam()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            returned if <i>parameter</i> is unsupported for this
 *            operation.
 */
mpe_Error mpeos_vbiFilterSetParam(mpe_VBIFilterSession session,
        mpe_VBIParameter parameter, uint32_t value);

/**
 * <i>mpeos_vbiFilterGetParam()</i> will retrieve the designated parameter
 * on the VBI filtering session.
 *
 * The following parameters are supported:
 *
 * MPE_VBI_PARAM_DATA_UNIT_THRESHOLD will return the
 * the data unit threshold for triggering the MPE_VBI_EVENT_DATAUNITS_RECEIVED
 * notification into the *value parameter.
 *
 * MPE_VBI_PARAM_BUFFER_SIZE will return the size, in bytes, of the VBI session
 * data buffer into the *value parameter.
 *
 * MPE_VBI_PARAM_BUFFERED_DATA_SIZE will return the number of bytes currently
 * in the VBI session data buffer into the *value parameter.
 *
 * @return If the call is successful, <i>mpeos_vbiFilterGetParam()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            returned if <i>parameter</i> is unsupported for this
 *            operation.
 */
mpe_Error mpeos_vbiFilterGetParam(mpe_VBIFilterSession session,
        mpe_VBIParameter parameter, uint32_t * value);

/**
 * <i>mpeos_vbiGetParam</i> will retrieve the designated parameter from the
 * VBI filtering subsystem.
 *
 * The following parameters are supported:
 *
 *      MPE_VBI_PARAM_SCTE20LINE21_CAPABILITY (no option values) sets *outval
 *          to 1 if SCTE-20 Line 21 VBI data retrieval is supported and 0
 *          otherwise.
 *      MPE_VBI_PARAM_SCTE21LINE21_CAPABILITY (no option values) sets *outval
 *          to 1 if SCTE-21 Line 21 VBI data retrieval is supported and 0
 *          otherwise.
 *      MPE_VBI_SEPARATED_FILTERING_CAPABILITY (option 1: line,
 *          option 2: mpe_VBIDataFormat) sets *outval to 1 if filtering for the
 *          format is supported on either field 1 or field 2 on the given line
 *          and 0 otherwise.
 *      MPE_VBI_MIXED_FILTERING_CAPABILITY (option 1: line,
 *          option 2: mpe_VBIDataFormat) sets *outval to 1 if filtering for the
 *          format is supported on both field 1 or field 2 on the given line
 *          and 0 otherwise.
 *
 * @return If the call is successful, <i>mpeos_vbiGet()</i> will
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            returned if <i>parameter</i> is unsupported for this
 *            operation.
 */
mpe_Error mpeos_vbiGetParam(mpe_VBIParameter parameter, uint32_t option1,
        uint32_t option2, uint32_t * outval);

/**
 * <i>mpeos_vbiRegisterAvailability()</i> requests that the given event queue
 * be notified when additional VBI filters are made generally available.
 * Following successful registration, an event will be sent to the given event
 * queue signaling filter availability when applicable.
 * <p>
 * When an event is sent (and received) it will be composed of the following
 * information:
 * <ul>
 * <li> eventId = MPE_VBI_FILTERS_AVAILABLE
 * <li> optionalEventData1 = reserved/unused
 * <li> optionalEventData2 = ACT (as given upon invocation of this function)
 * <li> optionalEventData3 = reserved/unused
 * </ul>
 *
 * @param queueId the event queue to send availability events to
 * @param act asynchronous completion token
 * @return if the call is successful, then <i>MPE_SUCCESS</i> will be returned.
 *         Otherwise, it will return the following error code:
 *
 *         <i>MPE_VBI_EINVAL</i>
 *            indicates that invalid parameters were provided.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 */
mpe_Error mpeos_vbiRegisterAvailability(mpe_EventQueue queueId, void* act);

/**
 * Unregisters the given eventQueue/completion token pair that was previously
 * provided on a call to <i>mpeos_vbiRegisterAvailability()</i>.  After successful
 * completion of this call, the given queue will no longer receive notifications
 * of filter availability.
 *
 * @param queueId the event queue to send availability events to
 * @param act asynchronous completion token
 * @return if the call is successful, then <i>MPE_SUCCESS</i> will be returned.
 *         Otherwise, it will return the following error code:
 *
 *         <i>MPE_VBI_EINVAL</i>
 *            indicates that invalid parameters were provided.
 */
mpe_Error mpeos_vbiUnregisterAvailability(mpe_EventQueue queueId, void* act);

#ifdef __cplusplus
}
#endif

#endif  /* _MPEOS_VBI_H */
