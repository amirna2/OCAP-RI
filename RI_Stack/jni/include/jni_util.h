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

#ifndef JNI_UTIL_H
#define JNI_UTIL_H

#include <jni.h>
#include <mpe_gfx.h>
#include <mpe_media.h>
#include <mpe_dvr.h>
#include <mpe_hn.h>
/**
 * Structure used to maintain a cache of JNI class, field, and method
 * identifiers.
 */
typedef struct
{
    /**
     * Ids for org.cablelabs.impl.manager.timeshift.TimeShiftBufferImpl
     */
    jfieldID TimeShiftBufferImpl_tsbHandle;
    jfieldID TimeShiftBufferImpl_bufferingHandle;
    jfieldID TimeShiftBufferImpl_effectiveMediaTime;

    /**
     * Ids for java.awt.Dimension.
     */
    jclass Dimension;
    jfieldID Dimension_width;
    jfieldID Dimension_height;

    /**
     * Ids for java.awt.Rectangle.
     */
    jclass Rectangle;
    jfieldID Rectangle_x;
    jfieldID Rectangle_y;
    jfieldID Rectangle_width;
    jfieldID Rectangle_height;

    /**
     * Ids for org.havi.ui.HScreenRectangle.
     */
    jclass HScreenRectangle;
    jfieldID HScreenRectangle_x;
    jfieldID HScreenRectangle_y;
    jfieldID HScreenRectangle_width;
    jfieldID HScreenRectangle_height;

    /**
     * Ids for org.cablelabs.impl.media.mpe.MediaDecodeParams
     */
    jclass MediaDecodeParams;
    jfieldID MediaDecodeParams_listener;
    jfieldID MediaDecodeParams_videoHandle;
    jfieldID MediaDecodeParams_tunerHandle;
    jfieldID MediaDecodeParams_ltsid;
    jfieldID MediaDecodeParams_pcrPid;
    jfieldID MediaDecodeParams_streamPids;
    jfieldID MediaDecodeParams_streamTypes;
    jfieldID MediaDecodeParams_blocked;
    jfieldID MediaDecodeParams_muted;
    jfieldID MediaDecodeParams_gain;
    jfieldID MediaDecodeParams_cci;

    /**
     * Ids for org.cablelabs.impl.media.mpe.MediaDripFeedParams
     */
    jclass MediaDripFeedParams;
    jfieldID MediaDripFeedParams_listener;
    jfieldID MediaDripFeedParams_videoHandle;

    /**
     * Ids for org.cablelabs.impl.havi.port.mpe.HDConfigInfo.
     */
    jclass HDConfigInfo;
    jfieldID HDConfigInfo_interlacedDisplay;
    jfieldID HDConfigInfo_flickerFilter;
    jfieldID HDConfigInfo_stillImage;
    jfieldID HDConfigInfo_changeableSingleColor;
    jfieldID HDConfigInfo_pixelResolution;
    jfieldID HDConfigInfo_pixelAspectRatio;
    jfieldID HDConfigInfo_screenArea;
    jfieldID HDConfigInfo_screenAspectRatio;

    /**
     * Ids for org.cablelabs.impl.ocap.hardware.VideoOutputPortImpl.
     */
    jclass VideoOutputPortImpl;
    jfieldID VideoOutputPortImpl_type;
    jfieldID VideoOutputPortImpl_hdcp;
    jfieldID VideoOutputPortImpl_dtcp;
    jfieldID VideoOutputPortImpl_restrictedResolution;
    jfieldID VideoOutputPortImpl_pixelResolution;

    /**
     * Ids for org.cablelabs.impl.ocap.hardware.device.AudioOutputPortImpl.
     */
    jclass AudioOutputPortImpl;
    jfieldID AudioOutputPortImpl_uniqueId;
    jfieldID AudioOutputPortImpl_compression;
    jfieldID AudioOutputPortImpl_gain;
    jfieldID AudioOutputPortImpl_encoding;
    jfieldID AudioOutputPortImpl_level;
    jfieldID AudioOutputPortImpl_optimalLevel;
    jfieldID AudioOutputPortImpl_maxDb;
    jfieldID AudioOutputPortImpl_minDb;
    jfieldID AudioOutputPortImpl_stereoMode;
    jfieldID AudioOutputPortImpl_loopThru;
    jfieldID AudioOutputPortImpl_muted;
    jfieldID AudioOutputPortImpl_audioPortHandle;
    // integer arrays
    jfieldID AudioOutputPortImpl_supportedCompressions;
    jfieldID AudioOutputPortImpl_supportedEncodings;
    jfieldID AudioOutputPortImpl_supportedStereoModes;

    /**
     * Ids for org.cablelabs.impl.ocap.hardware.device.VideoOutputSettingsProxy.
     */
    jclass VideoOutputSettingsProxy;
    jfieldID VideoOutputSettingsProxy_manufacturerName;
    jfieldID VideoOutputSettingsProxy_productCode;
    jfieldID VideoOutputSettingsProxy_serialNumber;
    jfieldID VideoOutputSettingsProxy_manufactureYear;
    jfieldID VideoOutputSettingsProxy_manufactureWeek;
    jfieldID VideoOutputSettingsProxy_aspectRatio;
    jfieldID VideoOutputSettingsProxy_currentConfig;
    jfieldID VideoOutputSettingsProxy_isDisplayConnected;
    jfieldID VideoOutputSettingsProxy_isContentProtected;
    jfieldID VideoOutputSettingsProxy_portUniqueId;
    jfieldID VideoOutputSettingsProxy_isDynamicConfigSupported;
    jmethodID VideoOutputSettingsProxy_setCurrOutputConfigUsingHandle;
    jmethodID VideoOutputSettingsProxy_addFixedConfig;
    jmethodID VideoOutputSettingsProxy_updateFixedConfig;
    jmethodID VideoOutputSettingsProxy_startDynamicConfig;
    jmethodID VideoOutputSettingsProxy_addOutputResolution;
    jmethodID VideoOutputSettingsProxy_endDynamicConfig;

    /**
     * Ids for javax.tv.service.ServiceType
     */
    jclass ServiceType;
    jfieldID ServiceType_DIGITAL_TV;
    jfieldID ServiceType_DIGITAL_RADIO;
    jfieldID ServiceType_NVOD_REFERENCE;
    jfieldID ServiceType_NVOD_TIME_SHIFTED;
    jfieldID ServiceType_ANALOG_TV;
    jfieldID ServiceType_ANALOG_RADIO;
    jfieldID ServiceType_DATA_BROADCAST;
    jfieldID ServiceType_DATA_APPLICATION;
    jfieldID ServiceType_UNKNOWN;

    /**
     * Ids for javax.tv.service.navigation.DeliverySystemType
     */
    jclass DeliverySystemType;
    jfieldID DeliverySystemType_CABLE;
    jfieldID DeliverySystemType_SATELLITE;
    jfieldID DeliverySystemType_TERRESTRIAL;
    jfieldID DeliverySystemType_UNKNOWN;

    /**
     * Ids for javax.tv.service.StreamType
     */
    jclass StreamType;
    jfieldID StreamType_VIDEO;
    jfieldID StreamType_AUDIO;
    jfieldID StreamType_SUBTITLES;
    jfieldID StreamType_DATA;
    jfieldID StreamType_SECTIONS;
    jfieldID StreamType_UNKNOWN;

    /**
     * Ids for java.lang.Thread
     */
    jclass Thread;
    jmethodID Thread_interrupted;

    /**
     * Ids for DVR org.cablelabs.impl.media.decoder.DVRPlayback
     */
    jclass DVRPlayback;
    jfieldID DVRPlayback_handle;

    /**
     * Ids for DVR org.cablelabs.impl.util.TimeTable
     */
    jclass TimeTable;
    jfieldID TimeTable_m_elements;
    jfieldID TimeTable_m_size;

    /**
     * Ids for DVR org.cablelabs.impl.util.TimeAssociatedElement
     */
    jclass TimeAssociatedElement;
    jfieldID TimeElement_time;

    /**
     * Ids for DVR org.cablelabs.impl.util.GenericTimeAssociatedElement
     */
    jclass GenericTimeAssociatedElement;
    jfieldID TimeElement_value;

    /**
     * Ids for DVR org.cablelabs.impl.util.PidMapTable
     */
    jclass PidMapTable;
    jfieldID PidMapTable_pidTableSize;
    jfieldID PidMapTable_pidMapEntryArray;

    /**
     * Ids for DVR org.cablelabs.impl.util.PidMapEntry
     */
    jclass PidMapEntry;
    jfieldID PidMapEntry_streamType;
    jfieldID PidMapEntry_srcElementaryStreamType;
    jfieldID PidMapEntry_recElementaryStreamType;
    jfieldID PidMapEntry_srcPID;
    jfieldID PidMapEntry_recPID;

    /**
     * Ids for org.cablelabs.impl.manager.pod.CAElementaryStreamAuthorization
     */
    jclass CAElementaryStreamAuthorization;
    jfieldID CAElementaryStreamAuthorization_pid;
    jfieldID CAElementaryStreamAuthorization_reason;

    /**
     * Ids for SectionFilterManagerImpl.
     */
    jclass SFEvent;
    jfieldID SFEvent_dispatchTarget;
    jfieldID SFEvent_eventId;
    jfieldID SFEvent_data;

    /**
     * Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl
     */
    jclass SIDatabaseImpl;
    jfieldID SIDatabaseImpl_EMPTY_STRING;
    jmethodID SIDatabaseImpl_updatePatPmtData;

    //$ServiceData
    jfieldID ServiceData_serviceLanguages;
    jfieldID ServiceData_serviceNames;
    jfieldID ServiceData_hasMultipleInstances;
    jfieldID ServiceData_serviceType;
    jfieldID ServiceData_serviceNumber;
    jfieldID ServiceData_minorNumber;
    jfieldID ServiceData_sourceID;
    jfieldID ServiceData_appID;
    jfieldID ServiceData_frequency;
    jfieldID ServiceData_programNumber;
    jfieldID ServiceData_modulationFormat;

    //$ServiceComponentData
    jfieldID ServiceComponentData_componentPID;
    jfieldID ServiceComponentData_componentTag;
    jfieldID ServiceComponentData_associationTag;
    jfieldID ServiceComponentData_carouselID;
    jfieldID ServiceComponentData_componentNames;
    jfieldID ServiceComponentData_componentLangs;
    jfieldID ServiceComponentData_associatedLanguage;
    jfieldID ServiceComponentData_streamType;
    jfieldID ServiceComponentData_serviceInformationType;
    jfieldID ServiceComponentData_updateTime;
    jfieldID ServiceComponentData_serviceHandle;
    jfieldID ServiceComponentData_serviceDetailsHandle;

    //$TransportData
    jfieldID TransportData_deliverySystemType;
    jfieldID TransportData_transportId;

    /** Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl$RatingDimensionData */
    jfieldID RatingDimensionData_dimensionNames;
    jfieldID RatingDimensionData_dimensionLanguages;
    jfieldID RatingDimensionData_levelDescriptions;
    jfieldID RatingDimensionData_levelDescriptionLanguages;
    jfieldID RatingDimensionData_levelNameLanguages;
    jfieldID RatingDimensionData_levelNames;

    /** Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl$TransportStreamData */
    jfieldID TransportStreamData_transportStreamId;
    jfieldID TransportStreamData_serviceInformationType;
    jfieldID TransportStreamData_lastUpdate;
    jfieldID TransportStreamData_description;
    jfieldID TransportStreamData_frequency;
    jfieldID TransportStreamData_modulationFormat;
    jfieldID TransportStreamData_transportHandle;
    jfieldID TransportStreamData_networkHandle;

    /**
     * Ids for javax.tv.service.ServiceInformationType
     */
    jclass ServiceInformationType;
    jfieldID ServiceInformationType_ATSC_PSIP;
    jfieldID ServiceInformationType_DVB_SI;
    jfieldID ServiceInformationType_SCTE_SI;
    jfieldID ServiceInformationType_UNKNOWN;

    /**
     * Ids for org.davic.mpeg.sections.
     */
    jclass SectionFilter;

    /**
     * Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl$ServiceDetailsData
     */
    jfieldID ServiceDetailsData_sourceId;
    jfieldID ServiceDetailsData_appId;
    jfieldID ServiceDetailsData_programNumber;
    jfieldID ServiceDetailsData_serviceType;
    jfieldID ServiceDetailsData_longNames;
    jfieldID ServiceDetailsData_languages;
    jfieldID ServiceDetailsData_deliverySystemType;
    jfieldID ServiceDetailsData_serviceInformationType;
    jfieldID ServiceDetailsData_updateTime;
    jfieldID ServiceDetailsData_caSystemIds;
    jfieldID ServiceDetailsData_isFree;
    jfieldID ServiceDetailsData_pcrPID;
    jfieldID ServiceDetailsData_transportStreamHandle;
    jfieldID ServiceDetailsData_serviceHandle;

    /**
     * Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl$ServiceDescription
     */
    jfieldID ServiceDescriptionData_descriptions;
    jfieldID ServiceDescriptionData_languages;
    jfieldID ServiceDescriptionData_updateTime;

    /**
     * Ids for org.cablelabs.impl.manager.service.SIDatabaseImpl$NetworkData
     */
    jfieldID NetworkData_networkId;
    jfieldID NetworkData_name;
    jfieldID NetworkData_serviceInformationType;
    jfieldID NetworkData_updateTime;
    jfieldID NetworkData_transportHandle;

    /**
     * Ids for org.cablelabs.impl.ocap.si.SIRequestImpl
     */
    jfieldID SIRequestImpl_asyncRequestHandle;
    jfieldID SIRequestImpl_asyncData;

    /**
     * IDs for org.cablelabs.impl.ocap.hardware.frontpanel.TextDisplayImpl
     */
    jclass TextDisplayImpl;
    jfieldID TextDisplayImpl_characterSet;
    jfieldID TextDisplayImpl_numColumns;
    jfieldID TextDisplayImpl_numRows;
    jfieldID TextDisplayImpl_mode;
    jfieldID TextDisplayImpl_iterations;
    jfieldID TextDisplayImpl_maxCycleRate;
    jfieldID TextDisplayImpl_onDuration;
    jfieldID TextDisplayImpl_brightness;
    jfieldID TextDisplayImpl_brightnessLevels;
    jfieldID TextDisplayImpl_color;
    jfieldID TextDisplayImpl_supportedColors;
    jfieldID TextDisplayImpl_maxHorizontalIterations;
    jfieldID TextDisplayImpl_maxVerticalIterations;
    jfieldID TextDisplayImpl_horizontalIterations;
    jfieldID TextDisplayImpl_verticalIterations;
    jfieldID TextDisplayImpl_holdDuration;

    /**
     * IDs for org.cablelabs.impl.ocap.hardware.frontpanel.IndicatorImpl
     */
    jclass IndicatorImpl;
    jfieldID IndicatorImpl_iterations;
    jfieldID IndicatorImpl_maxCycleRate;
    jfieldID IndicatorImpl_onDuration;
    jfieldID IndicatorImpl_brightness;
    jfieldID IndicatorImpl_brightnessLevels;
    jfieldID IndicatorImpl_color;
    jfieldID IndicatorImpl_supportedColors;

    /**
     * IDs for org.cablelabs.impl.manager.lightweighttrigger.NISessionManager
     */
    jfieldID NISessionManager_EST_pids;
    jfieldID NISessionManager_EST_types;

    /**
     * Method IDs for org.cablelabs.impl.manager.reclaim.RRMgrImpl
     */
    jmethodID RRMgrImpl_forceGC;
    jmethodID RRMgrImpl_notifyMonApp;
    jmethodID RRMgrImpl_destroyApp;
    jclass Callback;
    jmethodID Callback_releaseResources;

    /**
     * Method IDs for org.cablelabs.impl.davic.mpeg.NotAuthorizedException
     */
    jclass NotAuthorizedException;
    jmethodID NotAuthorizedException_init;

    /**
     * IDs for org.ocap.media.ClosedCaptioningAttributes
     */
    jclass ClosedCaptioningMpeColor;
    jclass ClosedCaptioningInteger;
    jmethodID ClosedCaptioningMpeColor_init;
    jmethodID ClosedCaptioningInteger_init;

    jclass PodAPDU;
    jmethodID PodAPDU_init;

    /**
     * Home Networking specific IDs follow here.
     * Refer to org.cablelabs.impl.media.mpe.HNAPIImpl
     */
    jclass String;

    jclass Socket;

    jclass Time;
    jmethodID Time_init;

    jclass MPEMediaError;
    jmethodID MPEMediaError_init;

    jclass HNAPI_Playback;
    jmethodID HNAPI_Playback_init;

    jclass HNAPI_LPEWakeUp;
    jmethodID HNAPI_LPEWakeUp_init;
    
    jclass HNStreamParams;
    jmethodID HNStreamParams_getStreamType;

    jclass HNStreamParamsMediaServerHttp;
    jfieldID HNStreamParamsMediaServerHttp_connectionId;                                        
    jfieldID HNStreamParamsMediaServerHttp_dlnaProfileId;
    jfieldID HNStreamParamsMediaServerHttp_mimeType;
    jfieldID HNStreamParamsMediaServerHttp_mpeSocket;
    jfieldID HNStreamParamsMediaServerHttp_chunkedEncodingMode;
    jfieldID HNStreamParamsMediaServerHttp_maxTrickModeBandwidth;
    jfieldID HNStreamParamsMediaServerHttp_currentDecodePTS;
    jfieldID HNStreamParamsMediaServerHttp_maxGOPsPerChunk;
    jfieldID HNStreamParamsMediaServerHttp_maxFramesPerGOP;
    jfieldID HNStreamParamsMediaServerHttp_useServerSidePacing;
    jfieldID HNStreamParamsMediaServerHttp_frameTypesInTrickModes;
    jfieldID HNStreamParamsMediaServerHttp_connectionStallingTimeoutMS;

    jclass HNStreamParamsMediaPlayerHttp;
    jfieldID HNStreamParamsMediaPlayerHttp_connectionId;
    jfieldID HNStreamParamsMediaPlayerHttp_uri;
    jfieldID HNStreamParamsMediaPlayerHttp_dlnaProfileId;
    jfieldID HNStreamParamsMediaPlayerHttp_mimeType;
    jfieldID HNStreamParamsMediaPlayerHttp_host;
    jfieldID HNStreamParamsMediaPlayerHttp_port;
    jfieldID HNStreamParamsMediaPlayerHttp_dtcpHost;
    jfieldID HNStreamParamsMediaPlayerHttp_dtcpPort;
    jfieldID HNStreamParamsMediaPlayerHttp_isTimeSeekSupported;
    jfieldID HNStreamParamsMediaPlayerHttp_isRangeSupported;
    jfieldID HNStreamParamsMediaPlayerHttp_isSenderPaced;
    jfieldID HNStreamParamsMediaPlayerHttp_isLimitedTimeSeekSupported;
    jfieldID HNStreamParamsMediaPlayerHttp_isLimitedByteSeekSupported;
    jfieldID HNStreamParamsMediaPlayerHttp_isPlayContainer;
    jfieldID HNStreamParamsMediaPlayerHttp_isS0Increasing;
    jfieldID HNStreamParamsMediaPlayerHttp_isSnIncreasing;
    jfieldID HNStreamParamsMediaPlayerHttp_isStreamingMode;
    jfieldID HNStreamParamsMediaPlayerHttp_isInteractiveMode;
    jfieldID HNStreamParamsMediaPlayerHttp_isBackgroundMode;
    jfieldID HNStreamParamsMediaPlayerHttp_isHTTPStallingSupported;
    jfieldID HNStreamParamsMediaPlayerHttp_isDLNAV15;
    jfieldID HNStreamParamsMediaPlayerHttp_isLinkProtected;
    jfieldID HNStreamParamsMediaPlayerHttp_isFullClearByteSeek;
    jfieldID HNStreamParamsMediaPlayerHttp_isLimitedClearByteSeek;
    jfieldID HNStreamParamsMediaPlayerHttp_playspeedsCnt;
    jfieldID HNStreamParamsMediaPlayerHttp_playspeeds;

    jclass HNPlaybackParams;
    jmethodID HNPlaybackParams_getPlaybackType;

    jclass HNPlaybackParamsMediaServerHttp;
    jfieldID HNPlaybackParamsMediaServerHttp_contentLocation;
    jfieldID HNPlaybackParamsMediaServerHttp_contentDescription;
    jfieldID HNPlaybackParamsMediaServerHttp_playspeedRate;
    jfieldID HNPlaybackParamsMediaServerHttp_useTimeOffset;
    jfieldID HNPlaybackParamsMediaServerHttp_startBytePosition;
    jfieldID HNPlaybackParamsMediaServerHttp_endBytePosition;
    jfieldID HNPlaybackParamsMediaServerHttp_startTimePosition;
    jfieldID HNPlaybackParamsMediaServerHttp_endTimePosition;
    jfieldID HNPlaybackParamsMediaServerHttp_cciDescriptors;
    jfieldID HNPlaybackParamsMediaServerHttp_transformation;

    jclass HNPlaybackParamsMediaPlayerHttp;
    jfieldID HNPlaybackParamsMediaPlayerHttp_avStreamParameters;
    jfieldID HNPlaybackParamsMediaPlayerHttp_videoDevice;
    jfieldID HNPlaybackParamsMediaPlayerHttp_initialBlockingState;
    jfieldID HNPlaybackParamsMediaPlayerHttp_muted;
    jfieldID HNPlaybackParamsMediaPlayerHttp_requestedGain;
    jfieldID HNPlaybackParamsMediaPlayerHttp_requestedRate;
    jfieldID HNPlaybackParamsMediaPlayerHttp_initialMediaTimeNS;
    jfieldID HNPlaybackParamsMediaPlayerHttp_cciDescriptors;

    jclass HNPlaybackCopyControlInfo;
    jfieldID HNPlaybackCopyControlInfo_pid;
    jfieldID HNPlaybackCopyControlInfo_isProgram;
    jfieldID HNPlaybackCopyControlInfo_isAudio;
    jfieldID HNPlaybackCopyControlInfo_cci;

    jclass HNStreamContentDescription;
    
    jclass HNStreamContentDescriptionLocalSV;
    jfieldID HNStreamContentDescriptionLocalSV_contentName;
    jfieldID HNStreamContentDescriptionLocalSV_volumeHandle;

    jclass HNStreamContentDescriptionApp;
    jfieldID HNStreamContentDescriptionApp_contentName;
    jfieldID HNStreamContentDescriptionApp_contentPath;

    jclass HNStreamContentDescriptionTSB;
    jfieldID HNStreamContentDescriptionTSB_nativeTSBHandle;

    jclass HNStreamContentDescriptionVideoDevice;
    jfieldID HNStreamContentDescriptionVideoDevice_nativeVideoDeviceHandle;

    jclass HNStreamContentDescriptionTuner;
    jfieldID HNStreamContentDescriptionTuner_tunerId;
    jfieldID HNStreamContentDescriptionTuner_frequency;
    jfieldID HNStreamContentDescriptionTuner_ltsid;
    jfieldID HNStreamContentDescriptionTuner_pids;
    jfieldID HNStreamContentDescriptionTuner_elemStreamTypes;
    jfieldID HNStreamContentDescriptionTuner_mediaStreamTypes;

    jclass HNHttpHeaderAVStreamParameters;
    jfieldID HNHttpHeaderAVStreamParameters_videoType;
    jfieldID HNHttpHeaderAVStreamParameters_videoPID;
    jfieldID HNHttpHeaderAVStreamParameters_audioType;
    jfieldID HNHttpHeaderAVStreamParameters_audioPID;

    jclass NativeContentTransformation;
    jmethodID NativeContentTransformation_init;
    jfieldID NativeContentTransformation_id;
    jfieldID NativeContentTransformation_sourceProfile;
    jfieldID NativeContentTransformation_transformedProfile;
    jfieldID NativeContentTransformation_bitrate;
    jfieldID NativeContentTransformation_width;
    jfieldID NativeContentTransformation_height;
    jfieldID NativeContentTransformation_progressive;
} JNICachedIds_t;

/**
 * Global instance of structure used to maintain a cache of JNI identifiers.
 */
extern JNICachedIds_t jniutil_CachedIds;

/*
 * These macros are used to convert JNI jstring objects to a native char*.
 * Use like this:
 *
 *        WITH_NATIVE_STRING(env, javastring, nativestring)
 *        {
 *             .... do something with native string ....
 *        }
 *        END_NATIVE_STRING(env, nativestring);
 *
 * env is the JNIEnv*, javastring is the jstring variable, and nativestring
 * is the name of a const char* that will be created for you to hold the
 * native string.
 *
 * If javastring is NULL, NullPointerException is thrown.
 */
#define WITH_NATIVE_STRING(env, javastring, nativestring)               \
    do {                                                                \
        const char* nativestring;                                       \
        jstring _##nativestring##str = (javastring);                    \
        if (_##nativestring##str == NULL) {                             \
            jniutil_throwByName(env,"java/lang/NullPointerException","");   \
            goto _##nativestring##end;                                  \
        }                                                               \
        if (((nativestring) = (*env)->GetStringUTFChars((env), _##nativestring##str, NULL)) == NULL) \
            goto _##nativestring##end;

#define END_NATIVE_STRING(env, nativestring)                            \
        (*env)->ReleaseStringUTFChars((env), _##nativestring##str, (nativestring)); \
        _##nativestring##end: ;                                                 \
    } while (0);

/*
 * These macros can be used when initializing cached ids stored in the CachedIds
 * structure.
 * They should be called from static initializes of classes to get any IDs they use.
 *  <p>
 * In debug mode, they will check the ID exists and return if it doesn't.
 * In non-debug mode they are optimised for space and performance to assume
 * the ID exists. This is because they are probably romized with the classes
 * and once they work it is not necessary to check every time that they exist.
 *  <p>
 * Note that all of the following macros reference a <code>jclass</code> variable
 * called <i>cls</i>.  This should already be defined and may be overwritten.
 *
 * Types:
 *  Java Type       JNI Type    machine dependent/C/C++ typedef     Signature   Call...Method/Get...Field
 *  boolean         jboolean    unsigned char                       Z           Boolean
 *  byte            jbyte       signed char                         B           Byte
 *  char            jchar       unsigned short                      C           Char
 *  short           jshort      short                               S           Short
 *  int             jint        int                                 I           Int
 *  long            jlong       long                                J           Long
 *  float           jfloat      float                               F           Float
 *  double          jdouble     double                              D           Double
 *  void            void        void                                V           Void
 *  nonprimitive    jobject     *...                                L...;       Object
 *
 * Examples:
 *  Method definition               signature
 *  int m1 ()                       ()I
 *  double m2 (long l, char c)      (JC)D
 *  void m3 (String s, int[] a)     (Ljava/lang/String;[I)V         // from documentation:  "If the name begins with �[� (the array signature character), it returns an array"
 *  String m4 (boolean b)           (Z)Ljava/lang/String;
 *  Object m4 (BigDecimal b)        (Ljava/math/BigDecimal;)Ljava/lang/Object;
 */

#if 1
#include <mpe_dbg.h>

#define FIND_CLASS(name) do {                                                   \
    if ((cls = (*env)->FindClass(env, name)) == NULL)                           \
    {                                                                           \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't find class %s\n", name);  \
        return;                                                                 \
    }                                                                           \
} while(0)
#define GET_CLASS(id,name) do {                                                 \
    if (!jniutil_CachedIds.id                                                   \
        && (cls = jniutil_CachedIds.id                                          \
            = (*env)->NewGlobalRef(env, (*env)->FindClass(env, name))) == NULL) \
    {                                                                           \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't get class %s\n", name);   \
        return;                                                                 \
    }                                                                           \
} while(0)
#define GET_FIELD_ID(id,name,type) do {                                                 \
    if (!jniutil_CachedIds.id                                                           \
        && (jniutil_CachedIds.id = (*env)->GetFieldID(env, cls, name, type)) == NULL)   \
    {                                                                                   \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't find field %s %s\n", name, type); \
        return;                                                                         \
    }                                                                                   \
} while(0)
#define GET_STATIC_FIELD_ID(id,name,type) do {                                                  \
    if (!jniutil_CachedIds.id                                                                   \
        && (jniutil_CachedIds.id = (*env)->GetStaticFieldID(env, cls, name, type)) == NULL)     \
    {                                                                                           \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't find static field %s %s\n", name, type);  \
        return;                                                                                 \
    }                                                                                           \
} while(0)
#define GET_METHOD_ID(id,name,type) do {                                                        \
    if (!jniutil_CachedIds.id                                                                   \
        && (jniutil_CachedIds.id = (*env)->GetMethodID(env, cls, name, type)) == NULL)          \
    {                                                                                           \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't find method %s %s\n", name, type);        \
        return;                                                                                 \
    }                                                                                           \
} while(0)
#define GET_STATIC_METHOD_ID(id,name,type) do {                                                 \
    if (!jniutil_CachedIds.id                                                                   \
        && (jniutil_CachedIds.id = (*env)->GetStaticMethodID(env, cls, name, type)) == NULL)    \
    {                                                                                           \
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Couldn't find static method %s %s\n", name, type); \
        return;                                                                                 \
    }                                                                                           \
} while(0)

#else

#define FIND_CLASS(name) (cls = (*env)->FindClass(env, name))
#define GET_CLASS(id,name) (!jniutil_CachedIds.id && (cls = jniutil_CachedIds.id = (*env)->NewGlobalRef(env, (*env)->FindClass(env, name))))
#define GET_FIELD_ID(id,name,type) (!jniutil_CachedIds.id && (jniutil_CachedIds.id = (*env)->GetFieldID(env, cls, name, type)))
#define GET_STATIC_FIELD_ID(id,name,type) (!jniutil_CachedIds.id && (jniutil_CachedIds.id = (*env)->GetStaticFieldID(env, cls, name, type)))
#define GET_METHOD_ID(id,name,type) (!jniutil_CachedIds.id && (jniutil_CachedIds.id = (*env)->GetMethodID(env, cls, name, type)))
#define GET_STATIC_METHOD_ID(id,name,type) (!jniutil_CachedIds.id && (jniutil_CachedIds.id = (*env)->GetStaticMethodID(env, cls, name, type)))

#endif

/**
 * Used to evaluate for JNI_TRUE/JNI_FALSE.
 * This is necessary to avoid problems truncating jboolean (e.g., if
 * <code>x == 0x10000000</code> a cast to jboolean will lose the
 * <i>true</i> nature of the value).
 *
 * @param x a class integer-based C boolean
 * @return x evaluated to JNI_TRUE or JNI_FALSE
 */
#define JNI_ISTRUE(x) ((jboolean)((x) ? JNI_TRUE : JNI_FALSE))

/**
 * Used to eliminate warnings about unused variables.
 */
#define JNI_UNUSED(x) (void)x

#define JNI_DETACH_ENV(jvm, detach)  do { if (detach) { (void)(*jvm)->DetachCurrentThread(jvm); } } while(0)
#define JNI_GET_ENV(jvm, detachp)          jniutil_getJNIEnv( jvm, detachp )

/**
 * Sets the fields of the given <code>Dimension</code> object to
 * <i>w</i> and <i>h</i>.
 *
 * @param dimension the <code>Dimension</code> to modify
 * @param w the new width
 * @param h the new height
 */
void jniutil_setDimension(JNIEnv *env, jobject dimension, jint w, jint h);

/**
 * Sets the fields of the given <code>Rectangle</code> object to
 * <i>x</i>, <i>y</i>, <i>w</i>, and <i>h</i>.
 *
 * @param rectangle the <code>Rectangle</code> to modify
 * @param x the new x
 * @param y the new y
 * @param w the new width
 * @param h the new height
 */
void jniutil_setRectangle(JNIEnv *env, jobject rectangle, jint x, jint y,
        jint w, jint h);

/**
 * Gets the fields of the given <code>Rectangle</code>.
 *
 * @param rectangle the <code>Rectangle</code> to read from
 * @param x the address to write x
 * @param y the address to write y
 * @param w the address to write width
 * @param h the address to write height
 */
void jniutil_getRectangle(JNIEnv *env, jobject rectangle, jint *x, jint *y,
        jint *w, jint *h);

/**
 * Gets the fields of the given <code>Rectangle</code> into an mpe_MediaRectangle.
 *
 * @param rectangle the <code>Rectangle</code> to read from
 * @param grOut pointer to the mpe_MediaRectangle into which result is copied
 */
void jniutil_getRectangleMedia(JNIEnv *env, jobject rectangle,
        mpe_MediaRectangle *grOut);

/**
 * Sets the fields of the given <code>Rectangle</code> object from an mpe_MediaRectangle.
 *
 * @param rectangle the <code>Rectangle</code> to copy to
 * @param grOut the mpe_MediaRectangle from which values are copied
 */
void jniutil_setRectangleMedia(JNIEnv *env, jobject rectangle,
        mpe_MediaRectangle *grIn);

/**
 * Sets the fields of the given <code>HScreenRectangle</code> object to
 * <i>x</i>, <i>y</i>, <i>w</i>, and <i>h</i>.
 *
 * @param hrect the <code>HScreenRectangle</code> to modify
 * @param x the new x
 * @param y the new y
 * @param w the new width
 * @param h the new height
 */
void jniutil_setHScreenRectangle(JNIEnv *env, jobject hrect, jfloat x,
        jfloat y, jfloat w, jfloat h);

/**
 * Gets the fields of the given <code>HScreenRectangle</code>
 *
 * @param hrect the <code>HScreenRectangle</code> to read from
 * @param x the address to write x
 * @param y the address to write y
 * @param w the address to write width
 * @param h the address to write height
 */
void jniutil_getHScreenRectangle(JNIEnv *env, jobject hrect, jfloat *x,
        jfloat *y, jfloat *w, jfloat *h);

/**
 * Throws an exception, specifying the given message.
 *
 * @param name exception class name
 * @param msg message to pass to constructor
 */
void jniutil_throwByName(JNIEnv *env, const char *name, const char *msg);

/**
 * Copy PID values and type values into mpe_MediaPID array.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param typeArray the array of elementary stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_MediaPID array, which must be freed by caller
 */
void jniutil_createPidArray(JNIEnv *env, jintArray pidArray,
        jshortArray typeArray, int pidCount, mpe_MediaPID* pids);

/**
 * Copy DVR PID values and type values into mpe_DvrPidInfo array.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param typeArray the array of elementary stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_DvrPidInfo array, which must be freed by caller
 */
void jniutil_createDvrPidInfoArray(JNIEnv *env, jintArray pidArray,
        jshortArray typeArray, int pidCount, mpe_DvrPidInfo* pids);

/**
 * Update the Pid Map table with recorded pids from native dvr
 *
 * @param env the JNI environment handle
 * @param pidCount number of pids in the pidMapTable
 * @param dvrPidInfo native dvr pid information (contains the recorded pids)
 * @param pidMapEntryArray the pid array to update
 */
void jniutil_updatePidMapTable(JNIEnv *env, jint pidCount,
        mpe_DvrPidInfo dvrPidInfo[], jobjectArray pidMapEntryArray);

/**
 * Create a pid table from TimeTable object
 *
 * @param env the JNI environment handle
 * @param elementArray array  of TimeTable elements
 * @param pidTableCount number of elements in the array
 * @param pidTable the dvr pid tables to create
 */
void jniutil_createDvrPidTable(JNIEnv *env, jobjectArray elementArray,
        int pidTableCount, mpe_DvrPidTable *dvrPidTable);

/**
 * Convert a pidMapTable to DvrPidInfo array
 *
 */
void jniutil_convertToDvrPidInfoArray(JNIEnv *env, jobject pidMapTable,
        mpe_DvrPidInfo *dvrPidInfo);

/**
 * Acquire a JNIEnv* for the current  thread.
 *
 * @param jvm the current JavaVM*
 * @param detach pointer to jboolean where JNI_TRUE is stored if detach should be performed
 * @return JNIEnv* or NULL
 */
JNIEnv* jniutil_getJNIEnv(JavaVM *jvm, jboolean *detach);

/**
 * Copy PID values and type values into mpe_MediaPID array.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param typeArray the array of elementary stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_MediaPID array, which must be freed by caller
 */
void jniutil_createHnPidArray(JNIEnv *env, jintArray pidArray,
        jshortArray elemTypeArray, jshortArray mediaTypeArray, int pidCount, mpe_HnPidInfo* pids);

#endif /* #ifndef JNI_UTIL_H */
