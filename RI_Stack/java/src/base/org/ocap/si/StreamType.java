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
 * StreamType.java
 */

package org.ocap.si;

/**
 * This interface represents valid values for the stream_type field in the PMT,
 * and returned by the getStreamType method from an implemented object of the
 * ProgramMapTable interface.
 */
public interface StreamType
{
    /**
     * ISO/IEC 11172-2 Video.
     */
    public static final short MPEG_1_VIDEO = 0x01;

    /**
     * ITU-T Rec. H.262 ISO/IEC 13818-2 Video or ISO/IEC 11172-2 constrained
     * parameter video stream.
     */
    public static final short MPEG_2_VIDEO = 0x02;

    /**
     * ISO/IEC 11172-3 Audio.
     */
    public static final short MPEG_1_AUDIO = 0x03;

    /**
     * ISO/IEC 13818-3 Audio.
     */
    public static final short MPEG_2_AUDIO = 0x04;

    /**
     * ITU-T Rec. H.222.0 ISO/IEC 13818-1 private-sections.
     */
    public static final short MPEG_PRIVATE_SECTION = 0x05;

    /**
     * ITU-T Rec. H.222.0 ISO/IEC 13818-1 PES packets containing private data.
     */
    public static final short MPEG_PRIVATE_DATA = 0x06;

    /**
     * ISO/IEC 13522 MHEG.
     */
    public static final short MHEG = 0x07;

    /**
     * 13818-1 Annex A - DSM CC.
     */
    public static final short DSM_CC = 0x08;

    /**
     * ITU-T Rec. H.222.1.
     */
    public static final short H_222 = 0x09;

    /**
     * ISO/IEC 13818-6 type A (Multi-protocol Encapsulation).
     */
    public static final short DSM_CC_MPE = 0x0A;

    /**
     * ISO/IEC 13818-6 type B (DSM-CC U-N Messages).
     */
    public static final short DSM_CC_UN = 0x0B;

    /**
     * ISO/IEC 13818-6 type C (DSM-CC Stream Descriptors).
     */
    public static final short DSM_CC_STREAM_DESCRIPTORS = 0x0C;

    /**
     * ISO/IEC 13818-6 type D (DSM-CC Sections any type, including private
     * data).
     */
    public static final short DSM_CC_SECTIONS = 0x0D;

    /**
     * ISO/IEC 13818-1 auxiliary.
     */
    public static final short AUXILIARY = 0x0E;

    /**
     * ISO/IEC 13818-7 AAC Audio with ADTS transport syntax
     */
    public static final short AAC_ADTS_AUDIO = 0x0F;

    /**
     * ISO/IEC 14496-2 Visual
     */
    public static final short ISO_14496_VISUAL = 0x10;

    /**
     * ISO/IEC 14496-3 and ISO/IEC 13818-7 AAC Audio with the LATM transport
     * syntax as defined in ISO/IEC 14496-3/AMD-1
     */
    public static final short AAC_AUDIO_LATM = 0x11;

    /**
     * ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in PES
     * packets
     */
    public static final short FLEXMUX_PES = 0x12;

    /**
     * ISO/IEC 14496-1 SL-packetized stream or FlexMux stream carried in
     * ISO/IEC14496_sections
     */
    public static final short FLEXMUX_SECTIONS = 0x13;

    /**
     * ISO/IEC 13818-6 Synchronized Download Protocol
     */
    public static final short SYNCHRONIZED_DOWNLOAD = 0x14;

    /**
     * Metadata carried in PES packets
     */
    public static final short METADATA_PES = 0x15;

    /**
     * Metadata carried in metadata_sections
     */
    public static final short METADATA_SECTIONS = 0x16;

    /**
     * Metadata carried in ISO/IEC 13818-6 Data Carousel
     */
    public static final short METADATA_DATA_CAROUSEL = 0x17;

    /**
     * Metadata carried in ISO/IEC 13818-6 Object Carousel
     */
    public static final short METADATA_OBJECT_CAROUSEL = 0x18;

    /**
     * Metadata carried in ISO/IEC 13818-6 Synchronized Download Protocol
     */
    public static final short METADATA_SYNCH_DOWNLOAD = 0x19;

    /**
     * IPMP stream (defined in ISO/IEC 13818-11, MPEG-2 IPMP)
     */
    public static final short MPEG_2_IPMP = 0x1A;

    /**
     * AVC video stream as defined in ITU-T Rec. H.264 | ISO/IEC 14496-10
     */
    public static final short AVC_VIDEO = 0x1B;

    /**
     * DigiCipher II video.
     */
    public static final short VIDEO_DCII = 0x80;

    /**
     * ATSC A/53 audio (ATSC Standard A/53, 1995, ATSC Digital Television
     * Standard).
     */
    public static final short ATSC_AUDIO = 0x81;

    /**
     * Standard subtitle.
     */
    public static final short STD_SUBTITLE = 0x82;

    /**
     * Isochronous data (Methods for Isochronous Data Services Transport,
     * ANSI/SCTE 19 2006)
     */
    public static final short ISOCHRONOUS_DATA = 0x83;

    /**
     * Asynchronous data (Methods for Asynchronous Data Services Transport,
     * ANSI/SCTE 53 2008)
     */
    public static final short ASYNCHRONOUS_DATA = 0x84;

    /**
     * Enhanced ATSC A/53 audio (Enhanced AC-3 Audio, ATSC Standard A/53, 2007).
     */
    public static final short ENHANCED_ATSC_AUDIO = 0x87;

}
