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

package org.cablelabs.impl.media.streaming.session.data;


import java.util.StringTokenizer;

import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.util.MPEEnv;
import org.apache.log4j.Logger;

/**
  * Purpose: This class represents the protocol info associated with a content item in the CDS
  * and also returned in support of the UPnP Connection Management Service for sink and source.
  * 
  * Protocol Info consists of four fields:
  * 
  * 1. Transport Protocol - always "http-get"
  * 
  * 2. Network - always wildcard "*"
  * 
  * 3. Mime Type - may consist just of format or may also contain DTCP info such as:
  *             "video/vnd.dlna.mpeg-tts"
  *             "application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT=video/vnd.dlna.mpeg-tts
  *
  * 4. 4th_field = pn-param [op-param] [ps-param] [ci-param] [flags-param] [ *(other-param)]
  *  
 **/
public class HNStreamProtocolInfo
{
    private static final Logger log = Logger.getLogger(HNStreamProtocolInfo.class);

    // Supported protocol info types
    public static final int PROTOCOL_TYPE_UNDEFINED = 0;
    public static final int PROTOCOL_TYPE_RECORDING = 1;
    public static final int PROTOCOL_TYPE_RECORDING_INPROGRESS = 2;
    public static final int PROTOCOL_TYPE_LIVE_STREAMING_TSB = 3;
    public static final int PROTOCOL_TYPE_LIVE_STREAMING_TUNER = 4;
    public static final int PROTOCOL_TYPE_VIDEO_DEVICE = 5;
    public static final int PROTOCOL_TYPE_DTCP_RECORDING = 6;
    public static final int PROTOCOL_TYPE_DTCP_RECORDING_INPROGRESS = 7;

    // Type of protocol info
    private int m_protocolType;
    
    // Flag indicating if content is link protected
    private boolean m_isLinkProtected;
    
    // Transport field in protocol info
    private String m_transport;
    
    // Network field in protocol info
    private String m_network;
    
    // Complete Mime type field in protocol info, may include additional subfields such as
    // "CONTENTFORMAT" for DTCP encrypted content
    //TODO: should all instances of HNStreamProtocolInfo have the __host__ placeholder replaced with the actual host
    private String m_mimeType;
    
    // Represents non-encrypted mime type, will be same as mime type for non-encrypted content 
    private String m_contentFormat;
    
    // Subfields which make up the fourth field of protocol info
    //
    // DLNA Profile ID Parameter does not include DLNA prefix string
    // It does not include the DTCP_ prefix either
    private String m_profileId;
    
    // Operations Parameter does not include DLNA prefix string
    private String m_opParam;
    
    // Playspeed Parameter does not include DLNA prefix string
    private float[] m_playspeeds;
    
    // Flags Parameters does not include DLNA prefix string
    private String m_flagsParam;

    // Indicates whether the profile is a transformed profile
    private boolean m_transformed;
    
    // Name of the MPE variable that carries the MPEOS value for DTCP/IP AKE TCP port
    private static final String MPE_HN_SERVER_DTCPIP_AKE_PORT = "MPE.HN.SERVER.DTCPIP.AKE.PORT";

    // Value of the DTCP/IP AKE TCP port.
    private static int s_dtcpPort = 0;
    
    /**
     * Constants used to form protocol info
     */
    public static final String HTTP_TRANSPORT = "http-get";
    public static final String NETWORK_WILDCARD = "*";

    // Use this string when adding items with unknown protocol info
    public static final String UNKNOWN_PROTOCOL_INFO = "http-get:*:*:*";

    // Appended to every supported profileId when m_isLinkProtected
    private static final String PROFILE_ID_DTCP_PREFIX = "DTCP_";

    /**
     * Strings used to create mime type for DTCP link protected content such as:
     * 
     * "application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT=video/vnd.dlna.mpeg-tts
     */
    public static final String MIMETYPE_DTCP_APP = "application/x-dtcp1";
    public static final String MIMETYPE_DTCP_HOST = "DTCP1HOST=";
    public static final String MIMETYPE_DTCP_PORT = "DTCP1PORT=";
    public static final String MIMETYPE_DTCP_CONTENT_FORMAT = "CONTENTFORMAT=";

    /**
     * Constants used when creating protocol info string.
     * When using these parameters in protocol info, include in the order
     * of this list (this order is required and defined in DLNA spec) 
     */
    private static final String PROTOCOL_DLNA_ORG_PN = "DLNA.ORG_PN=";       // DLNA media format profile ID
    private static final String PROTOCOL_DLNA_ORG_OP = "DLNA.ORG_OP=";       // Operation Parameters for HTTP
    private static final String PROTOCOL_DLNA_ORG_PS = "DLNA.ORG_PS=";       // Server Side Play speeds
    private static final String PROTOCOL_DLNA_ORG_FLAGS = "DLNA.ORG_FLAGS="; // Flags parameter
    private static final String PROTOCOL_DLNA_ORG_CI = "DLNA.ORG_CI=";       // Conversion Indicator flag, 
    //private static final String PROTOCOL_DLNA_ORG_MAXSP = "DLNA.ORG_MAXSP=";// Maximum RTSP Speed  
                                                                             // Not Applicable - RI only supports HTTP, not RTP
    /**
     * DLNA Flag parameters defined in DLNA spec
     * primary flags - 8 hexadecimal digits representing 32 binary flags
     * protocol info dlna org flags represented by primary flags followed by reserved data of 24 hexadecimal digits (zeros)
     */
    protected static final int SP_FLAG = 1 << 31; //(Sender Paced Flag), content src is clock
    protected static final int LOP_NPT = 1 << 30; //(Limited Operations Flags: Time-Based Seek)
    protected static final int LOP_BYTES = 1 << 29; //(Limited Operations Flags: Byte-Based Seek)
    protected static final int PLAYCONTAINER_PARAM = 1 << 28; //(DLNA PlayContainer Flag)
    protected static final int S0_INCREASING = 1 << 27; //(UCDAM s0 Increasing Flag) (content has no fixed beginning)
    protected static final int SN_INCREASING = 1 << 26; //(UCDAM sN Increasing Flag) (content has no fixed ending)
    protected static final int RTSP_PAUSE = 1 << 25; //(Pause media operation support for RTP Serving Endpoints)
    protected static final int TM_S = 1 << 24; //(Streaming Mode Flag) - av content must have this set
    protected static final int TM_I = 1 << 23; //(Interactive Mode Flag)
    protected static final int TM_B = 1 << 22; //(Background Mode Flag)
    protected static final int HTTP_STALLING = 1 << 21; //(HTTP Connection Stalling Flag)
    protected static final int DLNA_V15_FLAG = 1 << 20; //(DLNA v1.5 versioning flag)
    protected static final int LP_FLAG = 1 << 16; //(Link Protected Content Flag)
    protected static final int CLEARTEXTBYTESEEK_FULL_FLAG = 1 << 15;  // Support for Full RADA ClearTextByteSeek hdr
    protected static final int LOP_CLEARTEXTBYTES = 1 << 14; // Support for Limited RADA ClearTextByteSeek hdr

    // Array containing int representation of all possible flags
    public static final int DLNA_FLAGS[] = new int[]{
        SP_FLAG,
        LOP_NPT,
        LOP_BYTES,
        PLAYCONTAINER_PARAM,
        S0_INCREASING,
        SN_INCREASING,
        RTSP_PAUSE,
        TM_S,
        TM_I,
        TM_B,
        HTTP_STALLING,
        DLNA_V15_FLAG,
        LP_FLAG,
        CLEARTEXTBYTESEEK_FULL_FLAG,
        LOP_CLEARTEXTBYTES
    };
    
    //flag format-related length constants
    private static final int PRIMARY_FLAGS_REQUIRED_LENGTH = 8;
    private static final int RESERVED_FLAGS_LENGTH = 24;

    //all other bits in primary-flags are reserved for future use and must be false
     

    /**
     * Returns protocol info for the supplied content item 
     * 
     * @param protocolType          indicates type of content - completed recording, inprogress recording, tsb, etc.
     * @param contentLocationType   indicates underlying platform location type for content 
     * @param contentDescription    specific content description based on content location type
     * @param isLinkProtected       if true, indicates content is link protected, false if not protected
     * @return  array of possible protocol info applicable to this content item
     */
    public static HNStreamProtocolInfo[] getProtocolInfoStrs(int protocolType,
                                                             int contentLocationType, 
                                                             HNStreamContentDescription contentDescription,
                                                             boolean isLinkProtected)
    {
        // Query platform for profileIds and mimeTypes supported for this content item
        final String profileIds[] = HNAPIImpl.nativeServerGetDLNAProfileIds(contentLocationType, contentDescription);
        return getProtocolInfoStrsForProfiles( protocolType, 
                                               contentLocationType, 
                                               contentDescription, 
                                               isLinkProtected, 
                                               profileIds );
    }

    /**
     * Returns protocol info for the supplied content item for the designated profiles
     * 
     * @param protocolType          indicates type of content - completed recording, inprogress recording, tsb, etc.
     * @param contentLocationType   indicates underlying platform location type for content 
     * @param contentDescription    specific content description based on content location type
     * @param isLinkProtected       if true, indicates content is link protected, false if not protected
     * @param profileIds            profiles to form Protocol Infos for
     * 
     * @return  array of possible protocol info applicable to this content item
     */
    public static HNStreamProtocolInfo[] getProtocolInfoStrsForProfiles( 
            final int protocolType,
            final int contentLocationType, 
            final HNStreamContentDescription contentDescription,
            final boolean isLinkProtected, 
            final String profileIds[] )
    {
        String contentFormats[][] = new String[profileIds.length][];
        int totalProtocolInfos = 0;
        int aVal; // indicates support for time seek  
        int bVal; // indicates support for byte seek 
        for (int i = 0; i < profileIds.length; i++)
        {
            contentFormats[i] = HNAPIImpl.nativeServerGetMimeTypes(contentLocationType, contentDescription, profileIds[i]);
            totalProtocolInfos += contentFormats[i].length;
            if (log.isDebugEnabled())
            {
                log.debug("getProtocolInfoStrs() - profile ID \"" + profileIds[i] + "\":");
                for (int j = 0; j < contentFormats[i].length; j++)
                {
                    log.debug("[" + j + "]: mime type \"" + contentFormats[i][j] + "\"");
                }
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("getProtocolInfoStrs() - totalProtocolInfos: " + totalProtocolInfos);
        }

        HNStreamProtocolInfo protocolInfos[] = new HNStreamProtocolInfo[totalProtocolInfos];

        int currentProfileIndex = 0;
        for (int i = 0; i < profileIds.length; i++)
        {
            for (int j = 0; j < contentFormats[i].length; j++)
            {
                // Get the flags for this content item
                boolean isConnectionStallingSupported = HNAPIImpl.nativeServerGetConnectionStallingFlag(
                                                                    contentLocationType, contentDescription,
                                                                    profileIds[i], contentFormats[i][j], null);
                // Determine if stack has disabled connection stalling by setting timeout to -l
                if ((isConnectionStallingSupported) && 
                    (HNServerSessionManager.getInstance().getConnectionStallingTimeoutMS() == -1))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getProtocolInfoStrs() - platform supports connection stalling, " +
                                "but has been disabled in hn.properties via OCAP.hn.server.connectionStallingTimeoutMS == -1");
                    }
                    isConnectionStallingSupported = false;
                }
                
                // The a-val/b-val of opParam should be set based on whether platform supports
                // time seek and byte range access for a given content item
                
                // Time seek support is not supported for following content types
                if (protocolType == PROTOCOL_TYPE_VIDEO_DEVICE // VPOP
                        || protocolType == PROTOCOL_TYPE_LIVE_STREAMING_TUNER) // non-DVR live streaming
                {
                    aVal = 0;
                }
                else
                {
                    aVal = 1;
                }

                long contentSize = -1;
                if (contentDescription != null)
                {
                    contentSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, contentDescription,
                            profileIds[i], contentFormats[i][j], null);
                }
                // Byte seek is only supported if content size is valid
                if(contentSize > 0)
                {
                    bVal = 1;
                }
                else //if(contentSize == -1)
                {
                    bVal = 0; 
                    if (log.isInfoEnabled())
                    {
                        log.info("getProtocolInfoStrs - contentSize cannot be determined. Setting b-val to 0...");
                    }
                }
                String opParam = Integer.toString(aVal) + Integer.toString(bVal);
                boolean isByteseekSupported = true;
                if(bVal == 0)
                {
                    isByteseekSupported = false;
                }
                String flagsParam = formatFlags( protocolType, isLinkProtected, 
                                                 isConnectionStallingSupported,
                                                 isByteseekSupported, false);          

                String mimeType = formatMimeTypeStr(contentFormats[i][j], isLinkProtected);
                String playspeedStrs[] = HNAPIImpl.nativeServerGetPlayspeeds(contentLocationType, contentDescription,
                        profileIds[i], contentFormats[i][j], null);
                
                float playspeeds[] = null;
                if (playspeedStrs != null)
                {
                    playspeeds = formatSupportedRates(playspeedStrs);
                }
                
                protocolInfos[currentProfileIndex] = new HNStreamProtocolInfo(protocolType, isLinkProtected, profileIds[i],
                        contentFormats[i][j], mimeType, opParam, playspeeds, flagsParam, false);

                if (log.isDebugEnabled())
                {
                    log.debug("getProtocolInfoStrs() - generated protocolInfos[" + currentProfileIndex + "]: " +
                            protocolInfos[currentProfileIndex]);
                }

                currentProfileIndex++;
            }
        }
        
        return protocolInfos;
    }
    
    /**
     * Returns protocol info for the supplied content item for the designated profiles
     * 
     * @param protocolType          indicates type of content - completed recording, inprogress recording, tsb, etc.
     * @param contentLocationType   indicates underlying platform location type for content 
     * @param contentDescription    specific content description based on content location type
     * @param isLinkProtected       if true, indicates content is link protected, false if not protected
     * @param transformedContentFormat            transformation to generate Protocol Infos for
     * @return  array of possible protocol info applicable to this content item
     */
    public static HNStreamProtocolInfo[] getProtocolInfoStrsForTransformedContent( 
            final int protocolType,
            final int contentLocationType, 
            final HNStreamContentDescription contentDescription,
            final boolean isLinkProtected, 
            final OutputVideoContentFormatExt transformedContentFormat )
    {
        int totalProtocolInfos = 0;
        int aVal; // indicates support for time seek  
        int bVal; // indicates support for byte seek 
        final NativeContentTransformation nativeTransformation = 
            transformedContentFormat.getNativeTransformation();

        String contentFormats[] = HNAPIImpl.nativeServerGetMimeTypes( 
                                         contentLocationType, 
                                         contentDescription, 
                                         nativeTransformation.transformedProfile );
        totalProtocolInfos += contentFormats.length;
        if (log.isDebugEnabled())
        {
            log.debug("getProtocolInfoStrsForTransformedContent(): " + transformedContentFormat + ":");
            for (int j = 0; j < contentFormats.length; j++)
            {
                log.debug("  [" + j + "]: mime type \"" + contentFormats[j] + "\"");
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("getProtocolInfoStrsForTransformedContent(): " + totalProtocolInfos);
        }

        HNStreamProtocolInfo protocolInfos[] = new HNStreamProtocolInfo[totalProtocolInfos];

        int currentProfileIndex = 0;
            
        for (int j = 0; j < contentFormats.length; j++)
        {
            // Get the flags for this content item
            boolean isConnectionStallingSupported = HNAPIImpl.nativeServerGetConnectionStallingFlag(
                                                                contentLocationType, contentDescription,
                                                                nativeTransformation.transformedProfile, 
                                                                contentFormats[j],
                                                                nativeTransformation );
            // Determine if stack has disabled connection stalling by setting timeout to -l
            if ((isConnectionStallingSupported) && 
                (HNServerSessionManager.getInstance().getConnectionStallingTimeoutMS() == -1))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getProtocolInfoStrsForTransformedContent() - platform supports connection stalling, " +
                            "but has been disabled in hn.properties via OCAP.hn.server.connectionStallingTimeoutMS == -1");
                }
                isConnectionStallingSupported = false;
            }
            
            // The a-val/b-val of opParam should be set based on whether platform supports
            // time seek and byte range access for a given content item
            
            // Time seek is not supported for the following content types
            if (protocolType == PROTOCOL_TYPE_VIDEO_DEVICE // VPOP
                    || protocolType == PROTOCOL_TYPE_LIVE_STREAMING_TUNER) // non-DVR live streaming
            {
                aVal = 0;
            }
            else
            {
                aVal = 1;
            }

            long contentSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, contentDescription,
                    nativeTransformation.transformedProfile, contentFormats[j], nativeTransformation);
            if (log.isInfoEnabled())
            {
                log.info("getProtocolInfoStrsForTransformedContent - contentItemSize: " + contentSize );
            }

            // Byte seek is only supported if content size is valid
            if(contentSize > 0)
            {
                bVal = 1;
            }
            else //if(contentSize == -1)
            {
                bVal = 0; 
                if (log.isInfoEnabled())
                {
                    log.info("getProtocolInfoStrsForTransformedContent - contentSize cannot be determined. Setting b-val to 0...");
                }
            }
            String opParam = Integer.toString(aVal) + Integer.toString(bVal); 
            boolean isByteseekSupported = true;
            if(bVal == 0)
            {
                isByteseekSupported = false;
            }
            String flagsParam = formatFlags( protocolType, isLinkProtected, 
                                             isConnectionStallingSupported,
                                             isByteseekSupported, true );          

            String mimeType = formatMimeTypeStr(contentFormats[j], isLinkProtected);
            String playspeedStrs[] = HNAPIImpl.nativeServerGetPlayspeeds(
                                                   contentLocationType, 
                                                   contentDescription,
                                                   nativeTransformation.transformedProfile, 
                                                   contentFormats[j],
                                                   nativeTransformation );
            float playspeeds[] = null;
            if (playspeedStrs != null)
            {
                playspeeds = formatSupportedRates(playspeedStrs);
            }
            
            protocolInfos[currentProfileIndex] = new HNStreamProtocolInfo(
                                                         protocolType, 
                                                         isLinkProtected, 
                                                         nativeTransformation.transformedProfile,
                                                         contentFormats[j], 
                                                         mimeType,
                                                         opParam, playspeeds, flagsParam, true );
            if (log.isDebugEnabled())
            {
                log.debug("getProtocolInfoStrsForTransformedContent() - generated protocolInfos[" + currentProfileIndex + "]: " +
                        protocolInfos[currentProfileIndex]);
            }

            currentProfileIndex++;
        } // END for (loop through mime types
        
        return protocolInfos;
    } // END getProtocolInfoStrsForTransformedContent()
    
    /**
     * Constructor used in response to getProtocolInfoStrs() which constructs protocol info objects
     * based on supplied protocol type and content description.  The following values are obtained
     * by using the profile info supplied by platform.
     * 
     * @param protocolType
     * @param isLinkProtected
     * @param profileId
     * @param contentFormat
     * @param mimeType
     * @param opParam
     * @param playspeeds
     * @param flagsParam
     * @param transformed
     */
    public HNStreamProtocolInfo( int protocolType, boolean isLinkProtected, String profileId, 
                                 String contentFormat, String mimeType, String opParam, 
                                 float[] playspeeds, String flagsParam, boolean transformed)
    {
        m_transport = HTTP_TRANSPORT;
        m_network = NETWORK_WILDCARD;
        m_protocolType = protocolType;
        m_isLinkProtected = isLinkProtected;
        m_profileId = profileId;
        m_contentFormat = contentFormat;
        m_mimeType = mimeType;
        m_opParam = opParam;
        m_playspeeds = null;
        if (playspeeds != null)
        {
            m_playspeeds = new float[playspeeds.length];
            System.arraycopy(playspeeds, 0, m_playspeeds, 0, playspeeds.length);
        }
        m_flagsParam = flagsParam;
        m_transformed = transformed;
    }
    
    /**
     * Constructs an instance of protocol info extracting values from supplied string.
     * 
     * @param s string to use to obtain values for protocol info fields.
     */
    public HNStreamProtocolInfo(String s)
    {
        if (log.isDebugEnabled())
        {
            log.debug("HNStreamProtocolInfo() - using string: " + s);
        }
        String[] info = null;
        if (s != null)
        {
            info = Utils.split(s, ":");
        }
        if (info != null)
        {
            if (info.length >= 1)
            {
                m_transport = info[0];
            }
            else
            {
                m_transport = HTTP_TRANSPORT;                
            }
            if (info.length >= 2)
            {
                m_network = info[1];
            }
            else
            {
                m_network = NETWORK_WILDCARD;                            
            }

            if (info.length >= 3)
            {
                m_mimeType = info[2];
                if (m_mimeType.indexOf(MIMETYPE_DTCP_CONTENT_FORMAT) != -1)
                {
                    m_contentFormat = parseContentFormat(m_mimeType);
                    m_isLinkProtected = true;
                }
                else
                {
                    m_contentFormat = m_mimeType;
                }
            }
            else
            {
                // No Mime type was supplied, set to wildcard
                m_mimeType = "*";
                m_contentFormat = m_mimeType;
            }

            if (info.length >= 4)
            {
                String[] fourthField = null;
                if (info[3] != null)
                {
                    fourthField = Utils.split(info[3], ";");

                    // Set param based on prefix in string
                    int idx = -1;
                    for (int i = 0; i < fourthField.length; i++)
                    {
                        String param = fourthField[i];
                        if (param.indexOf(PROTOCOL_DLNA_ORG_PN) != -1)
                        {
                            // Parse profile id 
                            idx = param.indexOf(PROTOCOL_DLNA_ORG_PN)+ PROTOCOL_DLNA_ORG_PN.length();
                            String profileId = param.substring(idx);
                            if (profileId.startsWith(PROFILE_ID_DTCP_PREFIX))
                            {
                                m_profileId = profileId.substring(PROFILE_ID_DTCP_PREFIX.length());
                                m_isLinkProtected = true;
                            }
                            else
                            {
                                m_profileId = profileId;
                            }
                        }
                        else if (param.indexOf(PROTOCOL_DLNA_ORG_OP) != -1)
                        {
                            // Parse op param
                            idx = param.indexOf(PROTOCOL_DLNA_ORG_OP)+ PROTOCOL_DLNA_ORG_OP.length();
                            m_opParam = param.substring(idx);
                        }
                        else if (param.indexOf(PROTOCOL_DLNA_ORG_PS) != -1)
                        {
                            // Parse playspeeds
                            idx = param.indexOf(PROTOCOL_DLNA_ORG_PS)+ PROTOCOL_DLNA_ORG_PS.length();
                            String psParam = param.substring(idx);  
                            m_playspeeds = parsePlayspeedsStr(psParam);                            
                        }
                        else if (param.indexOf(PROTOCOL_DLNA_ORG_FLAGS) != -1)
                        {
                            // Parse flags param
                            idx = param.indexOf(PROTOCOL_DLNA_ORG_FLAGS)+ PROTOCOL_DLNA_ORG_FLAGS.length();
                            m_flagsParam = param.substring(idx);
                        }
                        else if (param.indexOf(PROTOCOL_DLNA_ORG_CI) != -1)
                        {
                            // Parse conversion indicator param
                            idx = param.indexOf(PROTOCOL_DLNA_ORG_CI)+ PROTOCOL_DLNA_ORG_CI.length();
                            m_transformed = (param.charAt(idx) == '1');
                        }
                        else if (param.trim().equals("*"))
                        {
                            // Fourth field is wildcard, set profile id to *
                            m_profileId = param.trim();
                        }
                        else
                        {
                            // No recognized param prefix
                            if (log.isWarnEnabled())
                            {
                                log.warn("HNStreamProtocolInfo() - unrecognized param prefix: " + param);
                            }            
                        }                       
                    }
                }
            }   
        }
        else
        {
            m_transport = HTTP_TRANSPORT;
            m_network = NETWORK_WILDCARD;     
            m_mimeType = "*";
            m_contentFormat = m_mimeType;
        }

        // Determine what kind of protocol info this is - recording, inprogress rec or live streaming
        m_protocolType = PROTOCOL_TYPE_RECORDING;        
        if (m_flagsParam != null)
        {
            // If size S0 is increasing, this is live streaming or video device
            if (isFlagSet(m_flagsParam, S0_INCREASING))
            {
                // If trick mode play speeds are supported this is TSB
                if (m_playspeeds != null)
                {
                    m_protocolType = PROTOCOL_TYPE_LIVE_STREAMING_TSB;
                }
                else
                {
                    // Playspeeds are null, so this could be either a VIDEO_DEVICE or a TUNER
                    // If op-param was supplied it is VIDEO_DEVICE, if not TUNER
                    if (m_opParam != null)
                    {
                        m_protocolType = PROTOCOL_TYPE_VIDEO_DEVICE;
                    }
                    else
                    {
                        m_protocolType = PROTOCOL_TYPE_LIVE_STREAMING_TUNER;                        
                    }
                }
            }
            // If size SN is increasing, this is an in-progress recording
            else if (isFlagSet(m_flagsParam, SN_INCREASING))
            {
                if (MediaServer.getLinkProtectionFlag() == true)
                {
                    m_protocolType = PROTOCOL_TYPE_RECORDING_INPROGRESS;
                }
                else
                {
                    m_protocolType = PROTOCOL_TYPE_DTCP_RECORDING_INPROGRESS;
                }
            }
        }
    }
    
    /**
     * Return a formatted protocol info string without escaping embedded commas.
     * 
     * @return protocol info string formatted based on DLNA requirements.
     */
    public String getAsString()
    {
        return getAsString(false);
    }

    /**
     * Return a formatted protocol info string with the commas escaped, or not,
     * as indicated by the parameter.
     * 
     * @param escapeCommas
     *            equals true if embedded commas should be escaped with '\'.
     * @return protocol info string formatted based on DLNA requirements.
     */
    public String getAsString(final boolean escapeCommas)
    {
        StringBuffer sb = new StringBuffer(64);

        sb.append(m_transport);
        sb.append(":");

        sb.append(m_network);
        sb.append(":");

        sb.append(m_mimeType);
        sb.append(":");

        sb.append(getFourthField(escapeCommas));

        return sb.toString();
    }
    
    /**
     * Returns string representing fourth field information for this protocol
     * info
     * 
     * @param escapeCommas
     *            equals true if embedded commas should be escaped with '\'.
     * @return fourth field of protocol info string formatted based on DLNA
     *         requirements.
     */
    public String getFourthField(final boolean escapeCommas)
    {
        StringBuffer sb = new StringBuffer(64);
        
        if ((m_profileId != null) && (!m_profileId.equals("*")))
        {
            sb.append(PROTOCOL_DLNA_ORG_PN);
            sb.append(getProfileId());
            
            // Don't include op param for live streaming based on DLNA 7.3.42.1, 7.3.46.2
            if (m_protocolType != PROTOCOL_TYPE_LIVE_STREAMING_TSB && m_protocolType != PROTOCOL_TYPE_LIVE_STREAMING_TUNER && m_opParam != null)
            {
                sb.append(";");
                sb.append(PROTOCOL_DLNA_ORG_OP);
                sb.append(m_opParam);
            }
            
            if (m_playspeeds != null)
            {
                sb.append(";");
                sb.append(PROTOCOL_DLNA_ORG_PS);
                sb.append(formatPlayspeedsStr(m_playspeeds, escapeCommas));
            }
            
            if (m_flagsParam != null)
            {
                sb.append(";");
                sb.append(PROTOCOL_DLNA_ORG_FLAGS);
                sb.append(m_flagsParam);
            }
            
            if (m_transformed)
            {
                sb.append(";");
                sb.append(PROTOCOL_DLNA_ORG_CI);
                sb.append(m_transformed ? '1' : '0');
            }
        }
        else if ((m_profileId != null) && m_profileId.equals("*"))
        {
            sb.append(m_profileId);
        }
        return sb.toString();
    }
    
    public String getMimeType()
    {
        return m_mimeType;
    }
    
    /**
     * Get the DLNA profile value as it should appear after DLNA.ORG_PN=, incorporating the 
     * link-protected prefix (e.g. "DLNA_")
     */
    public String getProfileId()
    {
        // This new check has to be added to avoid duplicate DTCP_ prefix
        // getting appended when there are more than one DTCP contents.
        if (m_isLinkProtected && !m_profileId.startsWith(PROFILE_ID_DTCP_PREFIX))
        {
            return PROFILE_ID_DTCP_PREFIX + m_profileId;
        }
        return m_profileId;
    }
    
    /**
     * Get the DLNA profile value without the link-protected prefix (e.g. without "DLNA_")
     */
    public String getBaseProfileId()
    {
        return m_profileId;
    }
    
    public boolean isSizeIncreasing()
    {
        boolean isSizeIncreasing = true;
        if (m_protocolType == PROTOCOL_TYPE_RECORDING)
        {
            isSizeIncreasing = false;
        }
        return isSizeIncreasing;
    }
    
    public boolean isValidPlaySpeed(float rate)
    {
        boolean isValidSpeed = false;
        
        // Rate of 1x is always valid
        if (rate == 1.0f)
        {
            isValidSpeed = true;
        }
        else if (m_playspeeds != null)
        {
            for (int i = 0; i < m_playspeeds.length; i++)
            {
                if (rate == m_playspeeds[i])
                {
                    isValidSpeed = true;
                    break;
                }
            }            
        }
        
        return isValidSpeed;
    }
    
    public boolean useChunkEncoding()
    {
        boolean useChunkEncoding = false;
        if (isSizeIncreasing())
        {
            useChunkEncoding = true;
        }
        return useChunkEncoding;
    }
    
    /**
     * Determines if time seek range request is supported for
     * the content item.
     * 
     * @return  true if time seek range is supported, false otherwise
     */
    public boolean isTimeSeekSupported()
    {
        boolean isTimeSeekSupported = false;
        if ((isLOPNPT()) || ((m_opParam != null) && (m_opParam.charAt(0) == '1')))
        {
            isTimeSeekSupported = true;
        }
        return isTimeSeekSupported;
    }
    
    /**
     * Determines if network byte range request is supported for
     * the content item.  Network range requests are denoted by
     * the HTTP request header value of "Range: bytes=x-y" and
     * must be applied to all bytes being returned even in the
     * case of protected content which has additional bytes added
     * for encryption purposes.
     *
     * @return  true if network byte range is supported, false otherwise
     */
    public boolean isNetworkRangeSupported()
    {
        boolean isSupported = false;
        if (isLOPBytes() || (m_opParam != null && m_opParam.charAt(1) == '1'))
        {
            isSupported = true;
        }
        return isSupported;
    }

    /**
     * Determines if cleartext (DTCP) byte range request is supported for
     * the content item.  Cleartext range requests are denoted by the HTTP
     * request header value of "Range.dtcp.com: bytes=x-y" and will apply
     * only to the content bytes being returned, not any of the bytes being
     * added to the streamed response containing the PCP data required by
     * the client for decryption.
     *
     * @return  true if cleartext byte range is supported, false otherwise
     */
    public boolean isCleartextRangeSupported()
    {
        return (m_flagsParam != null
                && (isFlagSet(m_flagsParam, CLEARTEXTBYTESEEK_FULL_FLAG)
                    || isFlagSet(m_flagsParam, LOP_CLEARTEXTBYTES)));
    }

    /**
     * Determines if available seek request is supported for
     * the content item.
     * 
     * @return  true if available seek is supported, false otherwise
     */
    public boolean isAvailableSeekSupported()
    {
        boolean isAvailableSeekSupported = false;
        if (isLOPBytes() || isLOPNPT())
        {
            isAvailableSeekSupported = true;
        }
        return isAvailableSeekSupported;
    }
    
    public String getContentFormat()
    {
        return m_contentFormat;
    }

    public String getNetwork()
    {
        return m_network;
    }

    public String getProtocol()
    {
        return m_transport;
    }

    public String getOpParam()
    {
        return m_opParam;
    }
    
    public float[] getPlayspeeds()
    {
        return m_playspeeds;
    }

    public String getFlagsParam()
    {
        return m_flagsParam;
    }
    
    private String getFlagsParamDescription()
    {
        StringBuffer sb = new StringBuffer(32);
        if (m_flagsParam != null)
        {
            sb.append("\nFollowing flags are set: \n");
            if (isFlagSet(m_flagsParam, SP_FLAG))
            {
                sb.append("SP_FLAG - Sender Paced Flag\n");
            }
            if (isFlagSet(m_flagsParam, LOP_NPT))
            {
                sb.append("LOP_NPT - Limited Operations Flags: Time-Based Seek\n");
            }
            if (isFlagSet(m_flagsParam, LOP_BYTES))
            {
                sb.append("LOP_BYTES - Limited Operations Flags: Byte-Based Seek\n");
            }
            if (isFlagSet(m_flagsParam, PLAYCONTAINER_PARAM))
            {
                sb.append("PLAYCONTAINER_PARAM - DLNA PlayContainer\n");
            }
            if (isFlagSet(m_flagsParam, S0_INCREASING))
            {
                sb.append("S0_INCREASING - UCDAM s0 Increasing, content has no fixed beginning\n");
            }
            if (isFlagSet(m_flagsParam, SN_INCREASING))
            {
                sb.append("SN_INCREASING - UCDAM sN Increasing, content has no fixed ending\n");
            }
            if (isFlagSet(m_flagsParam, RTSP_PAUSE))
            {
                sb.append("RTSP_PAUSE - Pause media operation support for RTP Serving Endpoints\n");
            }
            if (isFlagSet(m_flagsParam, TM_S))
            {
                sb.append("TM_S - Streaming Transfer Mode\n");
            }
            if (isFlagSet(m_flagsParam, TM_I))
            {
                sb.append("TM_I - Interactive Transfer Mode\n");
            }
            if (isFlagSet(m_flagsParam, TM_B))
            {
                sb.append("TM_B - Background Transfer Mode\n");
            }
            if (isFlagSet(m_flagsParam, HTTP_STALLING))
            {
                sb.append("HTTP_STALLING - HTTP Connection Stalling\n");
            }
            if (isFlagSet(m_flagsParam, DLNA_V15_FLAG))
            {
                sb.append("DLNA_V15_FLAG - DLNA v1.5 version\n");
            }
            if (isFlagSet(m_flagsParam, LP_FLAG))
            {
                sb.append("LP_FLAG - Link Protected Content\n");
            }
            if (isFlagSet(m_flagsParam, CLEARTEXTBYTESEEK_FULL_FLAG))
            {
                sb.append("CLEARTEXTBYTESEEK_FULL_FLAG - Support for Full RADA ClearTextByteSeek hdr\n");
            }
            if (isFlagSet(m_flagsParam, LOP_CLEARTEXTBYTES))
            {
                sb.append("LOP_CLEARTEXTBYTES - Support for Limited RADA ClearTextByteSeek hdr\n");
            }
        }
        else
        {
            sb.append("Flags param is null\n");
        }
        return sb.toString();
    }

    public boolean isLinkProtected()
    {
        return m_isLinkProtected;
    }
    
    public int getProtocolType()
    {
        return m_protocolType;
    }
    
    public static String parseContentFormat(String mimeType)
    {
        String contentType; //content type content format without dtcp info

        //We want the actual underlying media mime type, the <contentformat> field
        //can other information such as content_protection_mime_type, media_type_mime
        //but all we want is really the media type specification after the CONTENTFORMAT 

        //sample URIs are as follows
        // video/vnd.dlna.mpeg-tts
        // application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT=video/vnd.dlna.mpeg-tts
        // application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT="video/mpeg"
        // application/x-dtcp1;CONTENTFORMAT=video/MP2T
        // application/vnd.oma.drm.dcf;CONTENTFORMAT=video/MP2P
        // etc

        //if possible, split contentformat into substring 
        String[] info =  Utils.split(mimeType,";"); 

        //initialize content format to the first element
        contentType = info[0];

        //check and parse if the <contentformat> field contains other mimetypes
        //aside from the media type,
        for(int i=0; i < info.length; i++)
        {
            if(info[i].startsWith("CONTENTFORMAT="))
            {
                //split the string and return what's after the =
                String [] splitedinfo = Utils.split(info[i],"=");
                contentType = splitedinfo[1];
                if (contentType.startsWith("\"") && contentType.endsWith("\""))
                {
                    contentType = contentType.substring(1, contentType.length() - 1);
                }
            }
        }
        
        return contentType;
    }
    
    private static synchronized int getDtcpPort()
    {
        if (s_dtcpPort == 0)
        {
            String dtcpPortStr = MPEEnv.getEnv(MPE_HN_SERVER_DTCPIP_AKE_PORT);
            if (dtcpPortStr != null)
            {
                try
                {
                    int dtcpPort = Integer.parseInt(dtcpPortStr);
                    if (dtcpPort <= 0 || dtcpPort > 65535)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("getDtcpPort() - invalid port " + dtcpPort);
                        }
                    }
                    else
                    {
                        s_dtcpPort = dtcpPort;
                    }
                }
                catch (NumberFormatException nfe)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("getDtcpPort() - unable to parse \"" + dtcpPortStr + "\"");
                    }
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("getDtcpPort() - " + MPE_HN_SERVER_DTCPIP_AKE_PORT + " not defined");
                }
            }
        }

        return s_dtcpPort;
    }

    /**
     * Formats the mime type based on link protected content or not.
     * 
     * @param contentFormat     non-encrypted mime type
     * @param isLinkProtected   true if content is link protected, false otherwise
     * 
     * @return  string to use as mime type in protocol info
     */
    private static String formatMimeTypeStr(String contentFormat, boolean isLinkProtected)
    {
        String mimeType = contentFormat;
        if (isLinkProtected)
        {
            // Mime type string needs additional field such as:
            // "application/x-dtcp1;DTCP1HOST=xxx.xxx.xxx.xxx;DTCP1PORT=xxxxx;CONTENTFORMAT=video/vnd.dlna.mpeg-tts
            
            StringBuffer sb = new StringBuffer(128);

            sb.append(MIMETYPE_DTCP_APP);
            sb.append(";");

            sb.append(MIMETYPE_DTCP_HOST);
            sb.append(MediaServer.HOST_PLACEHOLDER);
            sb.append(";");

            int dtcpPort = getDtcpPort();
            if (dtcpPort == 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("formatMimeTypeStr() - not adding " + MIMETYPE_DTCP_PORT + " to mime type");
                }
            }
            else
            {
                sb.append(MIMETYPE_DTCP_PORT);
                sb.append(dtcpPort);
                sb.append(";");
            }

            sb.append(MIMETYPE_DTCP_CONTENT_FORMAT);
            sb.append('"');
            sb.append(contentFormat);
            sb.append('"');
            
            mimeType = sb.toString();
        }
        return mimeType;
    }
        
    /**
     * Convert the supplied strings into floats
     * 
     * @param playspeedStrs list of strings to convert into floats
     * 
     * @return  array of floats which represent supplied strings
     */
    private static float[] formatSupportedRates(String playspeedStrs[])
    {
        float playspeeds[] = null;
        if (playspeedStrs != null)
        {
            playspeeds = new float[playspeedStrs.length];

            for (int i = 0; i < playspeedStrs.length; i++)
            {
                try
                {
                    playspeeds[i] = fractionToFloat(playspeedStrs[i]).floatValue();
                }
                catch (HNStreamingException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("formatSupportedRates() - invalid playspeed str: " + playspeedStrs[i]);
                    }                
                }
            }
        }
        return playspeeds;
    }

    /**
     * Creates a comma-separated list of supported integer play speed values for
     * the ps-param value in a protocol info string per DLNA Requirement
     * [7.3.35.1] & UPnP AVTransport:1 Service Template Version 1.01 - 2.2.8.
     * 
     * However, the commas need to be escaped with a '\' if the ps-param value
     * is to be returned in a CMS:GetProtocolInfo response per DLNA Requirement
     * [7.3.35.1] & [7.3.28.5].
     * 
     * @param playspeeds
     *            supported play speeds to include in the CSV list.
     * @param escapeCommas
     *            equals true if embedded commas should be escaped with '\'.
     * 
     * @return string representing the ps-param value (e.g. "-4,-2,-1,2,4")
     */
    private static String formatPlayspeedsStr(float playspeeds[], final boolean escapeCommas)
    {
        String playspeedsStr = null;
        StringBuffer sb = new StringBuffer(32);
        if (playspeeds != null)
        {
            for (int i = 0; i < playspeeds.length; i++)
            {
                if (i > 0)
                {
                    sb.append(escapeCommas ? "\\," : ",");
                }
                sb.append(floatToFraction(playspeeds[i], 2));
            }
            playspeedsStr = sb.toString();
        }
        if (log.isTraceEnabled())
        {
            log.trace("formatPlayspeedsStr() - returning playspeed str: " + playspeedsStr);
        }                
        return playspeedsStr;
    }

    /**
     * Tokenize supplied string of comma-separated values into array of floats.
     * 
     * @param playspeedsStr
     *            string of float values
     * 
     * @return array of floats which represent supported playspeeds
     */
    private static float[] parsePlayspeedsStr(String playspeedsStr)
    {
        float playspeeds[] = null;
        if (playspeedsStr != null)
        {
            StringTokenizer st = new StringTokenizer(playspeedsStr, ",");
            int cnt = st.countTokens();
            playspeeds = new float[cnt];
            int idx = 0;
            String curStr = null;
            while (st.hasMoreTokens())
            {
                curStr = st.nextToken();
                try
                {
                    playspeeds[idx] = fractionToFloat(curStr).floatValue();
                }
                catch (HNStreamingException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("parsePlayspeedStr() - invalid playspeed str: " + curStr);
                    }
                }
                idx++;
            }
        }
        return playspeeds;
    }
    
    /**
     * Create flags param value to be used in protocol info.
     * 
     * @param protocolType                      type of content for this protocol
     * @param isLinkProtected                   true indicates protected, false otherwise
     * @param isConnectionStallingSupported     true indicates supported, false otherwise
     * @param isByteseekSupported               true indicates if byte seek is supported, false otherwise
     * @param isTransformed                     true indicates transformed, false otherwise
     * @return  string to be used as flagsParam in protocol info
     */
    private static String formatFlags(int protocolType, boolean isLinkProtected,
                                      boolean isConnectionStallingSupported, 
                                      boolean isByteseekSupported, boolean isTransformed)
    {
        StringBuffer sb = new StringBuffer(32);
        
        // Initialize the flag value to include all the flags which are common
        // to all types of content
        long flags = DLNA_V15_FLAG |    // DLNA V1.5 flags
                     TM_S;              // streaming mode transfer
        
        // If connection stalling is supported for this content, include stalling flag
        // along with the background transfer flag as requirement in DLNA 7.3.50.1:
        //      "If the http-stalling flag is true, then tm-b flag must be set to true."
        if (isConnectionStallingSupported)
        {
            flags = flags | HTTP_STALLING;  // HTTP Connection Stalling Flag
            flags = flags | TM_B;           // Background transfer support
        }
        
        switch (protocolType)
        {
            case PROTOCOL_TYPE_RECORDING:
                // For completed recordings:
                // * Supports Full Random Access Data Availability Model
                // * Fixed S0, Fixed SN model
                // * link protection impact on protocol info - LP flag
                // * Cleartext Byte Full Data Seek supported

                if (isLinkProtected)
                {
                    flags = flags | LP_FLAG |                       // link protected content flag
                                    CLEARTEXTBYTESEEK_FULL_FLAG;    // full RADA cleartext seeking
                }
                break;

            case PROTOCOL_TYPE_RECORDING_INPROGRESS:
                // For in-progress recordings:
                // * Supports Full Random Access Data Availability Model
                // * Fixed SO, Increasing SN model
                // * link protection impact on protocol info - LP flag
                // * Cleartext Byte Full Data Seek supported

                flags = flags | SN_INCREASING;   // UCDAM sN increasing
                break;

            case PROTOCOL_TYPE_DTCP_RECORDING_INPROGRESS:
                // For in-progress recordings:
                // * Supports Full Random Access Data Availability Model
                // * Fixed SO, Increasing SN model
                // * link protection impact on protocol info - LP flag
                // * Limited Operation Cleartext Byte Data Seek supported

                flags = flags | SN_INCREASING
                              | LP_FLAG
                              | LOP_CLEARTEXTBYTES; // UCDAM sN increasing
                break;

            case PROTOCOL_TYPE_DTCP_RECORDING:
                // For completed recordings:
                // * Supports Full Random Access Data Availability Model
                // * Fixed S0, Fixed SN model
                // * link protection impact on protocol info - LP flag
                // * Cleartext Byte Full Data Seek supported

                flags = flags | LP_FLAG                         // link protected content flag
                              | CLEARTEXTBYTESEEK_FULL_FLAG;    // full RADA cleartext seeking
                break;

            case PROTOCOL_TYPE_LIVE_STREAMING_TSB:
            // For Live Streaming:
            // * Supports Limited Random Access Data Availability Model 
            // * Increasing S0 model
            // * Increasing SN model for time shifted content, Mode=0 
            // * lop-bytes, lop-npt & lop-cleartextbytes - Limited RADA - DLNA Reqmt. 7.3.37.2, 7.3.42 
            // * link protection impact on protocol info - LP flag 

            flags = flags | LOP_NPT           // limited RADA - TimeSeekRange hdr supported
                          | S0_INCREASING     // UCDAM s0 increasing
                          | SN_INCREASING;    // UCDAM sN increasing

            if (isLinkProtected)
            {
                flags = flags | LP_FLAG |             // link protected content flag
                                LOP_CLEARTEXTBYTES;   // limited RADA cleartext seeking
            }
            else
            {
                // If byte seek operation is supported then set the LOP_BYTES
                // Since we cannot determine content size for live streaming 
                // (dvr and non-dvr) LOP_BYTES cannot be set.
                if(isByteseekSupported)
                {
                    flags = flags | LOP_BYTES; // limited RADA - Range hdr supported
                }
            }
            break;
        
        case PROTOCOL_TYPE_LIVE_STREAMING_TUNER:
        case PROTOCOL_TYPE_VIDEO_DEVICE:
            // For tuner or video device (neither are random access):
            // * Do not support Limited Random Access Data Availability Model 
            // * link protection impact on protocol info - LP flag 
            // * neither full nor limited cleartext seeking is supported

            flags = flags | S0_INCREASING |   // UCDAM s0 increasing
                            SN_INCREASING;    // UCDAM sN increasing

            if (isLinkProtected)
            {
                flags = flags | LP_FLAG;    // link protected content flag
            }
            break;
        
        default:
            if (log.isErrorEnabled())
            {
                log.error("formatFlags() - unsupported protocol type: " + protocolType);
                return null;
            }                
        }

        sb.append(generateFlags(flags)); 
        
        return sb.toString();
    }

    /**
     * Utility method to generate value of protocol info flags parameter
     * 
     * @param input flags to include
     * 
     * @return  flag param value
     */
    public static String generateFlags(long input)
    {
        StringBuffer stringBuffer = new StringBuffer();

        //cast to int to use only 32 bits of input
        //use of Integer.toHexString ok here because value is never negative
        String unpadded = Integer.toHexString((int)(input)).toUpperCase();

        //prepend leading zeros if needed
        for (int i = PRIMARY_FLAGS_REQUIRED_LENGTH - unpadded.length(); i > 0; i--) {
            stringBuffer.append('0');
        }
        stringBuffer.append(unpadded);
        //append reserved hexdigits
        for (int i = 0; i < RESERVED_FLAGS_LENGTH; i++) {
            stringBuffer.append('0');
        }
        return stringBuffer.toString();
    }
    
    /**
     * Utility method which converts an array of strings into array of floats
     * which represent playspeeds.
     * 
     * @param playspeedStrs array of strings to convert to floats
     * 
     * @return  arrray of floats which represent supplied strings,
     *          if string is encountered that can not be converted to float,
     *          a value of 0 will be assigned
     */
    public static float[] generatePlayspeeds(String playspeedStrs[])
    {
        float playspeeds[] = null;
        if (playspeedStrs != null)
        {
            playspeeds = new float[playspeedStrs.length];
            for (int i = 0; i < playspeeds.length; i++)
            {
                try
                {
                    playspeeds[i] = Float.parseFloat(playspeedStrs[i]);
                }
                catch (NumberFormatException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("generatePlayspeeds() - invalid playspeed str: " + playspeedStrs[i]);
                    }
                }                
            }
        }
        return playspeeds;
    }
    
    /**
     * Utility method which determines if a given flag is set in the flags string.
     * 
     * @param flagsStr the fourth field of a protocolInfo string
     * @return
     */
    public static boolean isFlagSet(String flagsStr, int flag)
    {
        if (flagsStr == null || flagsStr.length() <= RESERVED_FLAGS_LENGTH)
        {
            return false;
        }
        //drop reserved flags off of value (prepended zeros will be ignored)
        flagsStr = flagsStr.substring(0, flagsStr.length() - RESERVED_FLAGS_LENGTH);
        //use of Long.parseLong ok here because value is never negative
        long value = Long.parseLong(flagsStr, 16);
        return (value & flag) == flag;
    }
    
    public boolean isS0Increasing()
    {
        return (m_flagsParam != null && isFlagSet(m_flagsParam, S0_INCREASING));
    }
    
    public boolean isLOPNPT()
    {
        return (m_flagsParam != null && isFlagSet(m_flagsParam, LOP_NPT));
    }
    
    /**
     * Determines if Limited Operations Flags: Byte-Based Seek is supported by
     * looking for flag set in protocol flags param field.
     * 
     * @return  true if flag is set, false otherwise
     */
    public boolean isLOPBytes()
    {
        return (m_flagsParam != null && isFlagSet(m_flagsParam, LOP_BYTES));
    }
    
    /**
     * Returns indication if the content associated with this request is limited RADA.
     * DLNA Requirement [7.3.32.2]: If the op-param is present and if either a-val or 
     * b-val is "1", then the "Full Random Access Data Availability" model must be the 
     * data access model that applies in the context of the protocolInfo value.
     * 
     * @return true if operating under limited RADA, false otherwise (operating under full RADA).
     */
    public boolean isLimitedRADA()
    {
        boolean isLimitedRADA = true;
        
        // If op-param is not null and if either a or b is 1
        if ((m_opParam != null) && (m_opParam.indexOf("1") != -1))
        {
            isLimitedRADA = false;
        }

        return isLimitedRADA;
    }
    
    /**
     * Returns indication if connection stalling/pause is supported for this content.
     * 
     * @return  true if connection stalling/pause is supported, false otherwise 
     */
    public boolean isConnectionStallingSupported()
    {
        return (m_flagsParam != null && isFlagSet(m_flagsParam, HTTP_STALLING));
    }
    
    /**
     * Returns indication if background transfer is supported for this content.
     *  
     * @return true if background transfers are supported, false otherwise
     */
    public boolean isBackgroundTransferSupported()
    {
        return (m_flagsParam != null && isFlagSet(m_flagsParam, TM_B));
    }
    
    /**
     * Returns whether these two ProtocolInfo are profile-compatible. Currently this
     *  compares for equality of the Profile and ContentFormat fields
     *  
     * @param otherProtocolInfo The PI to compare with
     * @return true if otherProtocolInfo is considered "compatible" with this ProtocolInfo
     */
    public boolean isProfileCompatibleWith(final HNStreamProtocolInfo otherProtocolInfo)
    {
        if (otherProtocolInfo == null)
        {
            return false;
        }

        final String myProfileId = getProfileId();
        final String myContentFormat = getContentFormat();
        final String theirProfile = otherProtocolInfo.getProfileId();
        final String theirContentFormat = otherProtocolInfo.getContentFormat();
        
        return ( ( "*".equals(theirProfile) 
                   || (myProfileId != null && myProfileId.equals(theirProfile))
                 && ( "*".equals(theirContentFormat)
                      || ( myContentFormat != null) 
                           && myContentFormat.equals(theirContentFormat) ) ) );
    }
    
    /**
     * List of mime types to be supported
     * 
     * Included for reference only, don't delete
     */
    /*
    private static final String MIME_TYPE_VIDEO_MPEG = "video/mpeg";
    private static final String MIME_TYPE_VIDEO_VND_DLNA_MPEG_TTS = "video/vnd.dlna.mpeg-tts";
    private static final String MIME_TYPE_AUDIO_L16 = "audio/L16";
    private static final String MIME_TYPE_AUDIO_VND_DOLBY_DD_RAW = "audio/vnd.dolby.dd-raw";
    private static final String MIME_TYPE_AUDIO_VND_DLNA_ADTS = "audio/vnd.dlna.adts";
    private static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    private static final String MIME_TYPE_AUDIO_3GPP = "audio/3gpp";
    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    private static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    private static final String MIME_TYPE_IMAGE_PNG = "image/png";
    private static final String MIME_TYPE_IMAGE_GIF = "image/gif";
    */
    
    /**
     * List of DLNA Media Format Profile IDs. 
     * This list is based on the DLNA Profiles required in 
     * OCAP HNP specification Table 5-2
     * 
     * Included for reference when looking for actual profile ID string to use, don't delete
     */
    /*
    private static final String DLNA_PROFILE_JPEG_SM = "JPEG_SM";
    private static final String DLNA_PROFILE_JPEG_MED = "JPEG_MED";
    private static final String DLNA_PROFILE_JPEG_LRG = "JPEG_LRG";
    private static final String DLNA_PROFILE_JPEG_TN = "JPEG_TN";
    private static final String DLNA_PROFILE_JPEG_SM_ICO = "JPEG_SM_ICO";
    private static final String DLNA_PROFILE_JPEG_LRG_ICO = "JPEG_LRG_ICO";
    private static final String DLNA_PROFILE_PNG_TN = "PNG_TN";
    private static final String DLNA_PROFILE_PNG_SM_ICO = "PNG_SM_ICO";
    private static final String DLNA_PROFILE_PNG_LRG_ICO = "PNG_LRG_ICO";
    private static final String DLNA_PROFILE_PNG_LRG = "PNG_LRG";
    private static final String DLNA_PROFILE_GIF_LRG = "GIF_LRG";                      // image/gif
    private static final String DLNA_PROFILE_MPEG_PS_NTSC = "MPEG_PS_NTSC";                 // video/mpeg
    private static final String DLNA_PROFILE_MPEG_PS_NTSC_XAC3 = "MPEG_PS_NTSC_XAC3";            // video/mpeg
    private static final String DLNA_PROFILE_MPEG_TS_SD_NA_ISO = "MPEG_TS_SD_NA_ISO";            // video/mpeg
    private static final String DLNA_PROFILE_MPEG_TS_SD_NA_XAC3_ISO = "MPEG_TS_SD_NA_XAC3_ISO";       // video/mpeg
    private static final String DLNA_PROFILE_MPEG_TS_HD_NA_ISO = "MPEG_TS_HD_NA_ISO";            // video/mpeg
    private static final String DLNA_PROFILE_MPEG_TS_SD_NA_T = "MPEG_TS_SD_NA_T";              // video/vnd.dlna.mpeg-tts
    private static final String DLNA_PROFILE_MPEG_TS_HD_NA_T = "MPEG_TS_HD_NA_T";              // video/vnd.dlna.mpeg-tts
    private static final String DLNA_PROFILE_MPEG_TS_HD_NA_XAC3_ISO = "MPEG_TS_HD_NA_XAC3_ISO";       // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_SD_MPEG1_L3_ISO = "AVC_TS_MP_SD_MPEG1_L3_ISO";    // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_SD_AAC_MULT5_ISO = "AVC_TS_MP_SD_AAC_MULT5_ISO";   // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_SD_AC3_ISO = "AVC_TS_MP_SD_AC3_ISO";         // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_HD_MPEG1_L3_ISO = "AVC_TS_MP_HD_MPEG1_L3_ISO";    // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_HD_AAC_MULT5_ISO = "AVC_TS_MP_HD_AAC_MULT5_ISO";   // video/mpeg
    private static final String DLNA_PROFILE_AVC_TS_MP_HD_AC3_ISO = "AVC_TS_MP_HD_AC3_ISO";         // video/mpeg
    private static final String DLNA_PROFILE_LPCM = "LPCM";             // audio/L16
    private static final String DLNA_PROFILE_AC3 = "AC3";              // audio/vnd.dolby.dd-raw
    private static final String DLNA_PROFILE_MP3 = "MP3";              // audio/mpeg
    private static final String DLNA_PROFILE_MP3X = "MP3X";             // audio/mpeg
    private static final String DLNA_PROFILE_AAC_ADTS = "AAC_ADTS";         // audio/mp4
    private static final String DLNA_PROFILE_AAC_ADTS_320 = "AAC_ADTS_320";     // audio/vnd.dlna.adts
    private static final String DLNA_PROFILE_AAC_ISO = "AAC_ISO";          // audio/mp4, audio/3gpp
    private static final String DLNA_PROFILE_AAC_ISO_320 = "AAC_ISO_320";      // audio/mp4, audio/3gpp
    private static final String DLNA_PROFILE_AAC_LTP_ISO = "AAC_LTP_ISO";      // audio/mp4, audio/3gpp, audio/vnd.dlna.adts
    private static final String DLNA_PROFILE_AAC_LTP_MULT5_ISO = "AAC_LTP_MULT5_ISO";// audio/mp4, audio/3gpp, audio/vnd.dlna.adts
    private static final String DLNA_PROFILE_AAC_LTP_MULT7_ISO = "AAC_LTP_MULT7_ISO";// audio/mp4, audio/3gpp, audio/vnd.dlna.adts
    private static final String DLNA_PROFILE_AAC_MULT5_ADTS = "AAC_MULT5_ADTS";   // audio/vnd.dlna.adts
    private static final String DLNA_PROFILE_AAC_MULT5_ISO = "AAC_MULT5_ISO";     // audio/mp4, audio/3gpp
    */
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer(128);
        sb.append(getAsString());
        sb.append(getFlagsParamDescription());
        return sb.toString();
    }
    
    /**
     * Utility method to convert a possible fractional string to a float
     * 
     * @param fractionStr   string representing a fraction to convert to a float
     * 
     * @return  float representing numeric value of supplied string
     * 
     * @throws HNStreamingException
     */
    public static Float fractionToFloat(String fractionStr) throws HNStreamingException
    {
        float startRate = 0;
        
        // contains a space if it contains a mixed fraction
        // (separates fraction from whole)
        // may not have a fractional part
        // may not be a mixed fraction
        if (fractionStr.indexOf("/") == 0) // starts with the
                                              // fraction symbol -
                                              // invalid
        {
            String errMsg = "Invalid header format for playspeeddlnaorg: " + fractionStr;
            if (log.isWarnEnabled())
            {
                log.warn("fractionToFloat() - " + errMsg);
            }
            throw new HNStreamingException(errMsg);
        }

        if (fractionStr.indexOf("/") == -1)
        {
            // whole number
            startRate = Float.parseFloat(fractionStr);
        }
        else
        {
            // we have a fraction
            if (fractionStr.indexOf(" ") > -1)
            {
                // mixed fraction, grab the whole number part and
                // then divide the fraction and add
                StringTokenizer mixedNumberTokenizer = new StringTokenizer(fractionStr);
                int wholeNumber = Integer.parseInt(mixedNumberTokenizer.nextToken());

                StringTokenizer fractionTokenizer = new StringTokenizer(mixedNumberTokenizer.nextToken(),
                        "/");
                int numerator = Integer.parseInt(fractionTokenizer.nextToken());
                int denominator = Integer.parseInt(fractionTokenizer.nextToken());
                startRate = (float)wholeNumber + ((float)numerator / (float)denominator);
            }
            else
            {
                // just a fraction, divide the fraction
                StringTokenizer fractionTokenizer = new StringTokenizer(fractionStr, "/");
                int numerator = Integer.parseInt(fractionTokenizer.nextToken());
                int denominator = Integer.parseInt(fractionTokenizer.nextToken());
                // divide by zero invalid
                if (denominator == 0)
                {
                    String errMsg = "Invalid header format for playspeeddlnaorg: " + fractionStr +
                                    ", denominator is 0";
                    if (log.isWarnEnabled())
                    {
                        log.warn("fractionToFloat() - " + errMsg);
                    }
                    throw new HNStreamingException(errMsg);
                }
                startRate = (float)((float)numerator / (float)denominator);
            }
        }
        if (log.isTraceEnabled())
        {
            log.trace("fractionToFloat() - fraction str = " + fractionStr + ", returning value: " + startRate);
        }
        return new Float(startRate);
    }
    
    /**
     * Utility method to convert number into a fractional string
     * 
     * @param d         number to convert to fractional string
     * @param factor    precision of conversion (1/3 vs 10/33 etc.)
     * @return  fraction string to the precision requested of the supplied number
     */
    private static String floatToFraction(double d, int factor) 
    {
        StringBuffer sb = new StringBuffer();
        if (d < 0) 
        {
            sb.append('-');
            d = -d;
        }
        
        long l = (long)d;
        if (l != 0)
        {
            sb.append(l);
        }
        
        d -= l;
        double error = Math.abs(d);
        int bestDenominator = 1;
        for (int i = 2; i <= factor; i++) 
        {
            double error2 = Math.abs(d - (float)Math.round(d * i) / i);
            if (error2 < error) 
            {
                error = error2;
                bestDenominator = i;
            }
        }
        if (bestDenominator > 1)
        {
            sb.append(Math.round(d * bestDenominator)).append('/').append(bestDenominator);
        }
        
        if (log.isTraceEnabled())
        {
            log.trace("floatToFraction() - float = " + d + ", returning str: " + sb.toString());
        }
        return sb.toString();
    }
}
