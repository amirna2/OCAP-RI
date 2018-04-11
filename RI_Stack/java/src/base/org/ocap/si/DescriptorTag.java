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
 * DescriptorTag.java
 */

package org.ocap.si;

/**
 * The DescriptorTag contains constant values to be read from the descriptor_tag
 * field in a Descriptor. The constant values will correspond to those specified
 * in OCAP SI.
 */
public interface DescriptorTag
{

    /**
     * MPEG-2 video_stream_descriptor.
     */
    public static final short VIDEO_STREAM = 0x02;

    /**
     * MPEG-2 audio_stream_descriptor.
     */
    public static final short AUDIO_STREAM = 0x03;

    /**
     * MPEG-2 hierarchy_descriptor.
     */
    public static final short HIERARCHY = 0x04;

    /**
     * MPEG-2 registration_descriptor.
     */
    public static final short REGISTRATION = 0x05;

    /**
     * MPEG-2 data_stream_alignment_descriptor.
     */
    public static final short DATA_STREAM_ALIGNMENT = 0x06;

    /**
     * MPEG-2 target_background_grid_descriptor.
     */
    public static final short TARGET_BACKGROUND_GRID = 0x07;

    /**
     * MPEG-2 video_window_descriptor.
     */
    public static final short VIDEO_WINDOW = 0x08;

    /**
     * MPEG-2 CA_descriptor.
     */
    public static final short CA = 0x09;

    /**
     * MPEG-2 ISO_639_language_descriptor.
     */
    public static final short ISO_639_LANGUAGE = 0x0A;

    /**
     * MPEG-2 system_clock_descriptor.
     */
    public static final short SYSTEM_CLOCK = 0x0B;

    /**
     * MPEG-2 multiplex_utilization_buffer_descriptor.
     */
    public static final short MULTIPLEX_UTILIZATION_BUFFER = 0x0C;

    /**
     * MPEG-2 copyright_descriptor.
     */
    public static final short COPYRIGHT = 0x0D;

    /**
     * MPEG-2 maximum_bitrate_descriptor.
     */
    public static final short MAXIMUM_BITRATE = 0x0E;

    /**
     * MPEG-2 private_data_indicator_descriptor.
     */
    public static final short PRIVATE_DATA_INDICATOR = 0x0F;

    /**
     * MPEG-2 smoothing_buffer_descriptor.
     */
    public static final short SMOOTHING_BUFFER = 0x10;

    /**
     * MPEG-2 STD_descriptor.
     */
    public static final short STD = 0x11;

    /**
     * MPEG-2 IBP_descriptor.
     */
    public static final short IBP = 0x12;

    /**
     * DSMCC carousel_identifier_descriptor
     */
    public static final short CAROUSEL_IDENTIFIER = 0x13;

    /**
     * DSMCC association_tag_descriptor
     */
    public static final short ASSOCIATION_TAG = 0x14;

    /**
     * MHP application_descriptor.
     */
    public static final short APPLICATION = 0x00;

    /**
     * MHP application_name_descriptor.
     */
    public static final short APPLICATION_NAME = 0x01;

    /**
     * MHP transport_protocol_descriptor.
     */
    public static final short TRANSPORT_PROTOCOL = 0x02;

    /**
     * MHP DVB-J_application_descriptor.
     */
    public static final short DVB_J_APPLICATION = 0x03;

    /**
     * MHP DVB-J_application_location_descriptor.
     */
    public static final short DVB_J_APPLICATION_LOCATION = 0x04;

    /**
     * MHP external_application_authorisation_descriptor.
     */
    public static final short EXTERNAL_APPLICATION_AUTHORISATION = 0x05;

    /**
     * MHP IPV4_routing_descriptor.
     */
    public static final short IPV4_ROUTING = 0x06;

    /**
     * MHP IPV6_routing_descriptor.
     */
    public static final short IPV6_ROUTING = 0x07;

    /**
     * MHP application_icons_descriptor.
     */
    public static final short APPLICATION_ICONS = 0x0B;

    /**
     * MHP pre-fetch_descriptor.
     */
    public static final short PRE_FETCH = 0x0C;

    /**
     * MHP DII_location_descriptor.
     */
    public static final short DII_LOCATION = 0x0D;

    /**
     * DVB stream_identifier_descriptor. Used when resolving component tags
     * (e.g. when mounting a carousel). Defined in ETSI EN 300 468.
     */
    public static final short STREAM_IDENTIFIER = 0x52;

    /**
     * MHP private_data_specifier_descriptor.
     */
    public static final short PRIVATE_DATA_SPECIFIER = 0x5F;

    /**
     * MHP data_broadcast_id_descriptor.
     */
    public static final short DATA_BROADCAST_ID = 0x66;

    /**
     * MHP application_signaling_descriptor.
     */
    public static final short APPLICATION_SIGNALING = 0x6F;

    /**
     * MHP service_identifier_descriptor.
     */
    public static final short SERVICE_IDENTIFIER = 0x0D;

    /**
     * MHP label_descriptor.
     */
    public static final short LABEL = 0x70;

    /**
     * MHP caching_priority_descriptor.
     */
    public static final short CACHING_PRIORITY = 0x71;

    /**
     * MHP content_type_descriptor.
     */
    public static final short CONTENT_TYPE = 0x72;

    /**
     * SCTE stuffing_descriptor.
     */
    public static final short STUFFING = 0x80;

    /**
     * SCTE AC-3_audio_descriptor.
     */
    public static final short AC3_AUDIO = 0x81;

    /**
     * SCTE caption_service_descriptor.
     */
    public static final short CAPTION_SERVICE = 0x86;

    /**
     * SCTE content_advisory_descriptor.
     */
    public static final short CONTENT_ADVISORY = 0x87;

    /**
     * SCTE revision_detection_descriptor.
     */
    public static final short REVISION_DETECTION = 0x93;

    /**
     * SCTE two_part_channel_number_descriptor.
     */
    public static final short TWO_PART_CHANNEL_NUMBER = 0x94;

    /**
     * SCTE channel_properties_descriptor
     */
    public static final short CHANNEL_PROPERTIES = 0x95;

    /**
     * SCTE daylight_savings_time_descriptor.
     */
    public static final short DAYLIGHT_SAVINGS_TIME = 0x96;

    /**
     * SCTE extended_channel_name_description_descriptor.
     */
    public static final short EXTENDED_CHANNEL_NAME_DESCRIPTION = 0xA0;

    /**
     * SCTE service_location_descriptor.
     */
    public static final short SERVICE_LOCATION = 0xA1;

    /**
     * SCTE time_shifted_service_descriptor.
     */
    public static final short TIME_SHIFTED_SERVICE = 0xA2;

    /**
     * SCTE component_name_descriptor.
     */
    public static final short COMPONENT_NAME = 0xA3;

    /**
     * MAC address list
     */
    public static final short MAC_ADDRESS_LIST = 0xA4;

    /**
     * ATSC private_information_descriptor
     */
    public static final short ATSC_PRIVATE_INFORMATION = 0xAD;

}
