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


package org.cablelabs.impl.ocap.hn.upnp;

import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;

/**
 * This interface extends constants defined in the HN specification and API that
 * are specific to UPnP and used in conjunction with the
 * <code>org.ocap.hn.content.MetadataNode</code> interface.
 */
public interface UPnPConstants extends org.ocap.hn.profiles.upnp.UPnPConstants
{
    /* Namespace names / prefixes standardized from Table 1-3 [UPNP CDS] */

    public static final String NSN_AV            = "urn:schemas-upnp-org:av:av";
    public static final String NSN_AVS           = "urn:schemas-upnp-org:av:avs";
    public static final String NSN_AVDT          = "urn:schemas-upnp-org:av:avdt";
    public static final String NSN_AVT_EVENT     = "urn:schemas-upnp-org:metadata-1-0/AVT/";
    public static final String NSN_CDS_EVENT     = "urn:schemas-upnp-org:av:cds-event";
    public static final String NSN_DC            = "http://purl.org/dc/elements/1.1/";
    public static final String NSN_DIDL_LITE     = "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/";
    public static final String NSN_OCAP_DEVICE   = "urn:schemas-cablelabs-com:device-1-0";
    public static final String NSN_OCAP_METADATA = "urn:schemas-cablelabs-org:metadata-1-0/";
    public static final String NSN_OCAPAPP       = "urn:schemas-opencable-com:ocap-application";
    public static final String NSN_RCS_EVENT     = "urn:schemas-upnp-org:metadata-1-0/RCS/";
    public static final String NSN_SRS           = "urn:schemas-upnp-org:av:srs";
    public static final String NSN_SRS_EVENT     = "urn:schemas-upnp-org:av:srs-event";
    public static final String NSN_UPNP          = "urn:schemas-upnp-org:metadata-1-0/upnp/";
    public static final String NSN_DLNA          = "urn:schemas-dlna-org:metadata-1-0/";
    public static final String NSN_XSD           = "http://www.w3.org/2001/XMLSchema";
    public static final String NSN_XSI           = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String NSN_XML           = "http://www.w3.org/XML/1998/namespace";

    /* Namespace prefix */
    public static final String NSN_AV_PREFIX        = "av";
    public static final String NSN_AVS_PREFIX       = "avs";
    public static final String NSN_AVDT_PREFIX      = "avdt";
    public static final String NSN_AVT_EVENT_PREFIX = "avt-event";
    public static final String NSN_CDS_EVENT_PREFIX = "cds-event";
    public static final String NSN_DC_PREFIX        = "dc";
    public static final String NSN_DIDL_LITE_PREFIX = "didl-lite";
    public static final String NSN_OCAP_PREFIX      = "ocap";
    public static final String NSN_OCAPAPP_PREFIX   = "ocapApp";
    public static final String NSN_RCS_EVENT_PREFIX = "rcs-event";
    public static final String NSN_SRS_PREFIX       = "srs";
    public static final String NSN_SRS_EVENT_PREFIX = "srs-event";
    public static final String NSN_UPNP_PREFIX      = "upnp";
    public static final String NSN_XSD_PREFIX       = "xsd";
    public static final String NSN_XSI_PREFIX       = "xsi";
    public static final String NSN_XML_PREFIX       = "xml";
    public static final String NSN_DLNA_PREFIX      = "dlna";

    /* Qualified names */
    public static final QualifiedName QN_DC_CONTRIBUTOR                         = new QualifiedName(NSN_DC, "contributor");
    public static final QualifiedName QN_DC_DATE                                = new QualifiedName(NSN_DC, "date");
    public static final QualifiedName QN_DC_LANGUAGE                            = new QualifiedName(NSN_DC, "language");
    public static final QualifiedName QN_DC_PUBLISHER                           = new QualifiedName(NSN_DC, "publisher");
    public static final QualifiedName QN_DC_RELATION                            = new QualifiedName(NSN_DC, "relation");
    public static final QualifiedName QN_DC_RIGHTS                              = new QualifiedName(NSN_DC, "rights");
    public static final QualifiedName QN_DC_TITLE                               = new QualifiedName(NSN_DC, "title");

    public static final QualifiedName QN_DIDL_LITE_CONTAINER                    = new QualifiedName(NSN_DIDL_LITE, "container");
    public static final QualifiedName QN_DIDL_LITE_DESC                         = new QualifiedName(NSN_DIDL_LITE, "desc");
    public static final QualifiedName QN_DIDL_LITE_ID_ATTR                      = new QualifiedName(NSN_DIDL_LITE, "@id");
    public static final QualifiedName QN_DIDL_LITE_ITEM                         = new QualifiedName(NSN_DIDL_LITE, "item");
    public static final QualifiedName QN_DIDL_LITE_NAMESPACE                    = new QualifiedName(NSN_DIDL_LITE, "nameSpace");
    public static final QualifiedName QN_DIDL_LITE_PARENT_ID_ATTR               = new QualifiedName(NSN_DIDL_LITE, "@parentID");
    public static final QualifiedName QN_DIDL_LITE_RESTRICTED_ATTR              = new QualifiedName(NSN_DIDL_LITE, "@restricted");
    public static final QualifiedName QN_DIDL_LITE_SEARCHABLE_ATTR              = new QualifiedName(NSN_DIDL_LITE, "@searchable");
    public static final QualifiedName QN_DIDL_LITE_CHILD_COUNT_ATTR             = new QualifiedName(NSN_DIDL_LITE, "@childCount");
    public static final QualifiedName QN_DIDL_LITE_RES                          = new QualifiedName(NSN_DIDL_LITE, "res");
    public static final QualifiedName QN_DIDL_LITE_RES_BIT_RATE                 = new QualifiedName(NSN_DIDL_LITE, "res@bitRate");
    public static final QualifiedName QN_DIDL_LITE_RES_BITS_PER_SAMPLE          = new QualifiedName(NSN_DIDL_LITE, "res@bitsPerSample");
    public static final QualifiedName QN_DIDL_LITE_RES_COLOR_DEPTH              = new QualifiedName(NSN_DIDL_LITE, "res@colorDepth");
    public static final QualifiedName QN_DIDL_LITE_RES_CLEARTEXT_SIZE           = new QualifiedName(NSN_DIDL_LITE, "res@dlna:cleartextSize");
    public static final QualifiedName QN_DIDL_LITE_RES_DURATION                 = new QualifiedName(NSN_DIDL_LITE, "res@duration");
    public static final QualifiedName QN_DIDL_LITE_RES_NR_AUDIO_CHANNELS        = new QualifiedName(NSN_DIDL_LITE, "res@nrAudioChannels");
    public static final QualifiedName QN_DIDL_LITE_RES_PROTOCOL_INFO            = new QualifiedName(NSN_DIDL_LITE, "res@protocolInfo");
    public static final QualifiedName QN_DIDL_LITE_RES_RESOLUTION               = new QualifiedName(NSN_DIDL_LITE, "res@resolution");
    public static final QualifiedName QN_DIDL_LITE_RES_SAMPLE_FREQUENCY         = new QualifiedName(NSN_DIDL_LITE, "res@sampleFrequency");
    public static final QualifiedName QN_DIDL_LITE_RES_SIZE                     = new QualifiedName(NSN_DIDL_LITE, "res@size");
    public static final QualifiedName QN_DIDL_LITE_RES_UPDATE_COUNT             = new QualifiedName(NSN_DIDL_LITE, "res@updateCount");

    public static final QualifiedName QN_OCAP_ACCESS_PERMISSIONS                = new QualifiedName(NSN_OCAP_METADATA, "accessPermissions");
    public static final QualifiedName QN_OCAP_APP_ID                            = new QualifiedName(NSN_OCAP_METADATA, "appID");
    public static final QualifiedName QN_OCAP_CDS_REFERENCE                     = new QualifiedName(NSN_OCAP_METADATA, "cdsReference");
    public static final QualifiedName QN_OCAP_CONTENT_URI                       = new QualifiedName(NSN_OCAP_METADATA, "contentURI");
    public static final QualifiedName QN_OCAP_DESTINATION                       = new QualifiedName(NSN_OCAP_METADATA, "destination");
    public static final QualifiedName QN_OCAP_EXPIRATION_PERIOD                 = new QualifiedName(NSN_OCAP_METADATA, "expirationPeriod");
    public static final QualifiedName QN_OCAP_MEDIA_FIRST_TIME                  = new QualifiedName(NSN_OCAP_METADATA, "mediaFirstTime");
    public static final QualifiedName QN_OCAP_MEDIA_PRESENTATION_POINT          = new QualifiedName(NSN_OCAP_METADATA, "mediaPresentationPoint");
    public static final QualifiedName QN_OCAP_MSO_CONTENT_INDICATOR             = new QualifiedName(NSN_OCAP_METADATA, "msoContentIndicator");
    public static final QualifiedName QN_OCAP_NET_RECORDING_ENTRY               = new QualifiedName(NSN_OCAP_METADATA, "netRecordingEntry");
    public static final QualifiedName QN_OCAP_ORGANIZATION                      = new QualifiedName(NSN_OCAP_METADATA, "organization");
    public static final QualifiedName QN_OCAP_PRIORITY_FLAG                     = new QualifiedName(NSN_OCAP_METADATA, "priorityFlag");
    public static final QualifiedName QN_OCAP_RETENTION_PRIORITY                = new QualifiedName(NSN_OCAP_METADATA, "retentionPriority");
    public static final QualifiedName QN_OCAP_SCHEDULE_START_DATE_TIME          = new QualifiedName(NSN_OCAP_METADATA, "scheduleStartDateTime");
    public static final QualifiedName QN_OCAP_SCHEDULED_CDS_ENTRY_ID            = new QualifiedName(NSN_OCAP_METADATA, "scheduledCDSEntryID");
    public static final QualifiedName QN_OCAP_SCHEDULED_CHANNEL_ID              = new QualifiedName(NSN_OCAP_METADATA, "scheduledChannelID");
    public static final QualifiedName QN_OCAP_SCHEDULED_CHANNEL_ID_TYPE         = new QualifiedName(NSN_OCAP_METADATA, "scheduledChannelIDType");
    public static final QualifiedName QN_OCAP_SCHEDULED_DURATION                = new QualifiedName(NSN_OCAP_METADATA, "scheduledDuration");
    public static final QualifiedName QN_OCAP_SCHEDULED_START_DATE_TIME         = new QualifiedName(NSN_OCAP_METADATA, "scheduledStartDateTime");
    public static final QualifiedName QN_OCAP_SPACE_REQUIRED                    = new QualifiedName(NSN_OCAP_METADATA, "spaceRequired");
    public static final QualifiedName QN_OCAP_TASK_STATE                        = new QualifiedName(NSN_OCAP_METADATA, "taskState");
    public static final QualifiedName QN_OCAP_RES_ALT_URI                       = new QualifiedName(NSN_OCAP_METADATA, "res@ocap:alternateURI");

    public static final QualifiedName QN_SRS_ABNORMAL_TASKS_EXIST               = new QualifiedName(NSN_SRS, "abnormalTasksExist");
    public static final QualifiedName QN_SRS_BITS_MISSING                       = new QualifiedName(NSN_SRS, "someBitsMissing");
    public static final QualifiedName QN_SRS_BITS_RECORDED                      = new QualifiedName(NSN_SRS, "someBitsRecorded");
    public static final QualifiedName QN_SRS_CDS_REFERENCE                      = new QualifiedName(NSN_SRS, "cdsReference");
    public static final QualifiedName QN_SRS_CLASS                              = new QualifiedName(NSN_SRS, "class");
    public static final QualifiedName QN_SRS_CURRENT_ERRORS                     = new QualifiedName(NSN_SRS, "currentErrors");
    public static final QualifiedName QN_SRS_CURRENT_RECORD_TASK_COUNT          = new QualifiedName(NSN_SRS, "currentRecordTaskCount");
    public static final QualifiedName QN_SRS_ERROR_HISTORY                      = new QualifiedName(NSN_SRS, "errorHistory");
    public static final QualifiedName QN_SRS_FATAL_ERROR                        = new QualifiedName(NSN_SRS, "fatalError");
    public static final QualifiedName QN_SRS_ID_ATTR                            = new QualifiedName(NSN_SRS, "@id");
    public static final QualifiedName QN_SRS_INFO_LIST                          = new QualifiedName(NSN_SRS, "infoList");
    public static final QualifiedName QN_SRS_ITEM                               = new QualifiedName(NSN_SRS, "item");
    public static final QualifiedName QN_SRS_MATCHED_RATING                     = new QualifiedName(NSN_SRS, "matchedRating");
    public static final QualifiedName QN_SRS_MATCHING_CHANNEL_ID                = new QualifiedName(NSN_SRS, "matchingChannelID");
    public static final QualifiedName QN_SRS_MATCHING_DURATION_RANGE            = new QualifiedName(NSN_SRS, "matchingDurationRange");
    public static final QualifiedName QN_SRS_MATCHING_RATING_LIMIT              = new QualifiedName(NSN_SRS, "matchingRatingLimit");
    public static final QualifiedName QN_SRS_MATCHING_START_DATE_TIME_RANGE     = new QualifiedName(NSN_SRS, "matchingStartDateTimeRange");
    public static final QualifiedName QN_SRS_PENDING_ERRORS                     = new QualifiedName(NSN_SRS, "pendingErrors");
    public static final QualifiedName QN_SRS_PHASE                              = new QualifiedName(NSN_SRS, "phase");
    public static final QualifiedName QN_SRS_PRIORITY                           = new QualifiedName(NSN_SRS, "priority");
    public static final QualifiedName QN_SRS_RECORD_DESTINATION                 = new QualifiedName(NSN_SRS, "recordDestination");
    public static final QualifiedName QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR  = new QualifiedName(NSN_SRS, "recordDestination@mediaType");
    public static final QualifiedName QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR = new QualifiedName(NSN_SRS, "recordDestination@preference");
    public static final QualifiedName QN_SRS_RECORD_QUALITY                     = new QualifiedName(NSN_SRS, "recordQuality");
    public static final QualifiedName QN_SRS_RECORD_QUALITY_TYPE_ATTR           = new QualifiedName(NSN_SRS, "recordQuality@type");
    public static final QualifiedName QN_SRS_RECORD_SCHEDULE_ID                 = new QualifiedName(NSN_SRS, "recordScheduleID");
    public static final QualifiedName QN_SRS_RECORD_TASK_ID                     = new QualifiedName(NSN_SRS, "recordTaskID");
    public static final QualifiedName QN_SRS_RECORDING                          = new QualifiedName(NSN_SRS, "recording");
    public static final QualifiedName QN_SRS_SCHEDULE_STATE                     = new QualifiedName(NSN_SRS, "scheduleState");
    public static final QualifiedName QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR = new QualifiedName(NSN_SRS, "scheduleState@currentErrors");
    public static final QualifiedName QN_SRS_SCHEDULED_CHANNEL_ID               = new QualifiedName(NSN_SRS, "scheduledChannelID");
    public static final QualifiedName QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR     = new QualifiedName(NSN_SRS, "scheduledChannelID@type");
    public static final QualifiedName QN_SRS_SCHEDULED_DURATION                 = new QualifiedName(NSN_SRS, "scheduledDuration");
    public static final QualifiedName QN_SRS_SCHEDULED_START_DATE_TIME          = new QualifiedName(NSN_SRS, "scheduledStartDateTime");
    public static final QualifiedName QN_SRS_SRS                                = new QualifiedName(NSN_SRS, "srs");
    public static final QualifiedName QN_SRS_TASK_CHANNEL_ID                    = new QualifiedName(NSN_SRS, "taskChannelID");
    public static final QualifiedName QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR          = new QualifiedName(NSN_SRS, "taskChannelID@type");
    public static final QualifiedName QN_SRS_TASK_START_DATE_TIME               = new QualifiedName(NSN_SRS, "taskStartDateTime");
    public static final QualifiedName QN_SRS_TASK_DURATION                      = new QualifiedName(NSN_SRS, "taskDuration");
    public static final QualifiedName QN_SRS_TASK_STATE                         = new QualifiedName(NSN_SRS, "taskState");
    public static final QualifiedName QN_SRS_TITLE                              = new QualifiedName(NSN_SRS, "title");

    public static final QualifiedName QN_UPNP_ALBUM_ART_URI                     = new QualifiedName(NSN_UPNP, "albumArtURI");
    public static final QualifiedName QN_UPNP_AUTHOR                            = new QualifiedName(NSN_UPNP, "author");
    public static final QualifiedName QN_UPNP_BOOKMARK_ID                       = new QualifiedName(NSN_UPNP, "bookmarkID");
    public static final QualifiedName QN_UPNP_CHANNEL_ID                        = new QualifiedName(NSN_UPNP, "channelID");
    public static final QualifiedName QN_UPNP_CHANNEL_ID_TYPE_ATTR              = new QualifiedName(NSN_UPNP, "channelID@type");
    public static final QualifiedName QN_UPNP_CHANNEL_NAME                      = new QualifiedName(NSN_UPNP, "channelName");   
    public static final QualifiedName QN_UPNP_CHANNEL_NR                        = new QualifiedName(NSN_UPNP, "channelNr");
    public static final QualifiedName QN_UPNP_CLASS                             = new QualifiedName(NSN_UPNP, "class");
    public static final QualifiedName QN_UPNP_CONTAINER_UPDATE_ID               = new QualifiedName(NSN_UPNP, "containerUpdateID");
    public static final QualifiedName QN_UPNP_CREATE_CLASS                      = new QualifiedName(NSN_UPNP, "createClass");
    public static final QualifiedName QN_UPNP_DIRECTOR                          = new QualifiedName(NSN_UPNP, "director");
    public static final QualifiedName QN_UPNP_DVD_REGION_CODE                   = new QualifiedName(NSN_UPNP, "DVDRegionCode");
    public static final QualifiedName QN_UPNP_FOREIGN_METADATA                  = new QualifiedName(NSN_UPNP, "foreignMetadata");
    
    // TODO: are these handled property?
    public static final QualifiedName QN_UPNP_FOREIGN_METADATA_FM_ID            = new QualifiedName(NSN_UPNP, "foreignMetadata::fmId");
    public static final QualifiedName QN_UPNP_FOREIGN_METADATA_FM_CLASS         = new QualifiedName(NSN_UPNP, "foreignMetadata::fmClass");
    public static final QualifiedName QN_UPNP_FOREIGN_METADATA_FM_BODY_FM_URI   = new QualifiedName(NSN_UPNP, "foreignMetadata::fmBody::fmURI");
    public static final QualifiedName QN_UPNP_GENRE                             = new QualifiedName(NSN_UPNP, "genre");
    public static final QualifiedName QN_UPNP_LYRICS_URI                        = new QualifiedName(NSN_UPNP, "lyricsURI");
    public static final QualifiedName QN_UPNP_OBJECT_UPDATE_ID                  = new QualifiedName(NSN_UPNP, "objectUpdateID");
    public static final QualifiedName QN_UPNP_ORIGINAL_TRACK_NUMBER             = new QualifiedName(NSN_UPNP, "originalTrackNumber");
    public static final QualifiedName QN_UPNP_PLAYLIST                          = new QualifiedName(NSN_UPNP, "playlist");
    public static final QualifiedName QN_UPNP_PRICE                             = new QualifiedName(NSN_UPNP, "price");
    public static final QualifiedName QN_UPNP_PRODUCER                          = new QualifiedName(NSN_UPNP, "producer");
    public static final QualifiedName QN_UPNP_SCHEDULED_END_TIME                = new QualifiedName(NSN_UPNP, "scheduledEndTime");
    public static final QualifiedName QN_UPNP_SCHEDULED_START_TIME              = new QualifiedName(NSN_UPNP, "scheduledStartTime");
    public static final QualifiedName QN_UPNP_SEARCH_CLASS                      = new QualifiedName(NSN_UPNP, "searchClass");
    public static final QualifiedName QN_UPNP_SEARCH_CLASS_DERIVED_ATTR         = new QualifiedName(NSN_UPNP, "searchClass@includeDerived");
    public static final QualifiedName QN_UPNP_SRS_RECORD_SCHEDULE_ID            = new QualifiedName(NSN_UPNP, "srsRecordScheduleID");
    public static final QualifiedName QN_UPNP_SRS_RECORD_TASK_ID                = new QualifiedName(NSN_UPNP, "srsRecordTaskID");
    public static final QualifiedName QN_UPNP_STATE_VARIABLE_COLLECTION         = new QualifiedName(NSN_UPNP, "stateVariableCollection");
    public static final QualifiedName QN_UPNP_STORAGE_FREE                      = new QualifiedName(NSN_UPNP, "storageFree");
    public static final QualifiedName QN_UPNP_STORAGE_TOTAL                     = new QualifiedName(NSN_UPNP, "storageTotal");
    public static final QualifiedName QN_UPNP_TOTAL_DELETED_CHILD_COUNT         = new QualifiedName(NSN_UPNP, "totalDeletedChildCount");
    public static final QualifiedName QN_UPNP_USER_ANNOTATION                   = new QualifiedName(NSN_UPNP, "userAnnotation");
    
    public static final QualifiedName QN_DLNA_CONTAINER_TYPE                    = new QualifiedName(NSN_DLNA, "containerType");

    // TODO: all of the following should now be able to be replaced by their QN equivalents

    /** ID attribute. */
    public static final String ID_ATTR = NSN_DIDL_LITE_PREFIX + ":@" + ID;

    /** Parent ID attribute. */
    public static final String PARENT_ID_ATTR = NSN_DIDL_LITE_PREFIX + ":@" + PARENT_ID;

    /** Restricted attribute. */
    public static final String RESTRICTED_ATTR = NSN_DIDL_LITE_PREFIX + ":@restricted";

    /** Searchable attribute. */
    public static final String SEARCHABLE_ATTR = NSN_DIDL_LITE_PREFIX + ":@searchable";
    
    /** desc element for vendor specific metadata as per Section B.14.5 in UPnP-av-ContentDirectory-v3-Service */
    public static final String DESC_ELEMENT = UPnPConstants.NSN_DIDL_LITE_PREFIX + ":desc";

    /** Child count attribute. */
    public static final String CHILD_COUNT_ATTR = UPnPConstants.NSN_DIDL_LITE_PREFIX + ":@childCount";

    /** Resource element. */
    public static final String RESOURCE = NSN_DIDL_LITE_PREFIX + ":res";

    /** Resource size attribute. */
    public static final String RESOURCE_SIZE = RESOURCE + "@size";

    /** Resource duration attribute. */
    public static final String RESOURCE_DURATION = RESOURCE + "@duration";

    /** Resource protocol info attribute. */
    public static final String RESOURCE_PROTOCOL_INFO = RESOURCE + "@protocolInfo";

    /** Resource resolution attribute. */
    public static final String RESOURCE_RESOLUTION = RESOURCE + "@resolution";

    /** Resource number of audio channels attribute. */
    public static final String RESOURCE_NR_AUDIO_CHANNELS = RESOURCE + "@nrAudioChannels";

    /** Resource bits per sample attribute. */
    public static final String RESOURCE_BITS_PER_SAMPLE = RESOURCE + "@bitsPerSample";

    /** Resource bit rate attribute. */
    public static final String RESOURCE_BIT_RATE = RESOURCE + "@bitRate";

    /** Resource sample frequency attribute. */
    public static final String RESOURCE_SAMPLE_FREQUENCY = RESOURCE + "@sampleFrequency";

    /** Resource color depth attribute. */
    public static final String RESOURCE_COLOR_DEPTH = RESOURCE + "@colorDepth";

    // *TODO* - not sure if this the right place for this constant - but other OCAP vals are in here...
    /** Alternate URI for the resource */
    public static final String RESOURCE_ALT_URI = RESOURCE + "@ocap:alternateURI";

    /** UPnP Name space constants */
    public static final String UPNP_CLASS               = "upnp:class";
    public static final String UPNP_OBJECT_UPDATE_ID    = "upnp:objectUpdateID";
    public static final String UPNP_CONTAINER_UPDATE_ID = "upnp:containerUpdateID";
    public static final String UPNP_SRS_RECORD_TASK_ID  = "upnp:srsRecordTaskID";
    
    /** DLNA Name space constants */

    /** Defined list of sort capabilities */
    // dc:date,upnp:class,res,res@protocolInfo"; TODO : Come up with full list
    // from TCO DLNA requirements: "upnp:objectUpdateID", "upnp:containerUpdateID"
    public static final String SORT_CAPABILITIES =          "@" + ID
                                                    + "," + UPNP_OBJECT_UPDATE_ID
                                                    + "," + UPNP_CONTAINER_UPDATE_ID
                                                    + "," + TITLE;
    
    // *TODO* - not sure if this the right place for these constants
    // *TODO* - moved from CDevice
    public final static String MIDDLEWARE_PROFILE = "ocap:X_MiddlewareProfile";
    public final static String MIDDLEWARE_VERSION = "ocap:X_MiddlewareVersion";
    public final static String OCAP_HOME_NETWORK = "ocap:X_OCAPHN";
    
    // *TODO* - not sure if this the right place for these constants
    // *TODO* - moved from CService in order to support getType() for NetModuleImpl
    public static final String CONTENT_DIRECTORY_URN = "urn:schemas-upnp-org:service:ContentDirectory:";
    public static final String SCHEDULED_RECORDING_URN = "urn:schemas-upnp-org:service:ScheduledRecording:";
    public static final String CONNECTION_MANAGER_URN = "urn:schemas-upnp-org:service:ConnectionManager:";
    public static final String RENDERING_CONTROL_URN = "urn:schemas-upnp-org:service:RenderingControl:";
    public static final String AV_TRANSPORT_URN = "urn:schemas-upnp-org:service:AVTransport:";

    // *TODO* - end of constants to be moved  
}
