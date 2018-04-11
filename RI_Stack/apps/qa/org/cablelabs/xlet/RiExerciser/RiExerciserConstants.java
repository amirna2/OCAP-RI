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

package org.cablelabs.xlet.RiExerciser;

import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * @author Nicolas Metts
 * This class provides constants for the RiExerciser Xlet
 *
 */
public class RiExerciserConstants
{
    // Adding a private no-arg constructor to prevent instantiation, since this is
    // a utility class
    
    private RiExerciserConstants()
    {
        // Do nothing
    }
    
    private static Dimension m_screenDimension;
    
    static
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        m_screenDimension = tk.getScreenSize(); 
    }
    /**
     * The screen width for QuarterPages
     */
    public static final int QUARTER_PAGE_WIDTH = m_screenDimension.width;
    
    /**
     * The screen height for QuarterPages
     */
    public static final int QUARTER_PAGE_HEIGHT = m_screenDimension.height;
    
    /**
     * The screen width for FullPages
     */
    public static final int FULL_PAGE_WIDTH = m_screenDimension.width;
    
    /**
     * The screen height for FullPages
     */
    public static final int FULL_PAGE_HEIGHT = m_screenDimension.height;
    
    /**
     * The Y screen location for displaying media time in full screen playback
     */
    public static final int FULL_PAGE_MEDIA_TIME_Y = 460;
    
    /**
     * The font size to be used for menu options
     */
    public static final int FONT_SIZE = 16;
    
    /**
     * The font size to be used for banner or page titles
     */
    public static final int FONT_SIZE_LARGE = 20;
    
    /**
     * The ID of the root Container
     */
    public static final String ROOT_CONTAINER_ID = "0";
    
    
    /**
     * The name for the root Container
     */
    public static final String ROOT_CONTAINER_NAME = "Root";
    
    /**
     * The name of the Ri Exerciser General Menu Page
     */
    public static final String GENERAL_MENU_PAGE = "GeneralMenuPage";
    
    /**
     * The name of the DVR Menu Page
     */
    public static final String DVR_MENU_PAGE = "DVRMenuPage";
    
    /**
     * The name of the Recording Menu Page
     */
    public static final String RECORDING_MENU_PAGE = "RecordingMenuPage";
    
    /**
     * The name of the HN General Menu Page
     */
    public static final String HN_GENERAL_MENU_PAGE = "HomeNetworkingMenuPage";
    
    /**
     * The name of the HN Server Options Page
     */
    public static final String HN_SERVER_OPTIONS_PAGE = "HNServerOptionsPage";
    
    /**
     * The name of the HN Player Options Page
     */
    public static final String HN_PLAYER_OPTIONS_PAGE = "HNPlayerOptionsPage";
    
    /**
     * The name of the HN Diagnostics Page
     */
    public static final String HN_DIAGNOSTICS_PAGE = "HNDiagnosticsPage";
    
    /**
     * The name of the DVR Playback Menu Page
     */
    public static final String DVR_PLAYBACK_PAGE = "DvrPlaybackPage";
    
    /**
     * The name of the Media Control Page
     */
    public static final String MEDIA_CONTROL_PAGE = "MediaControlPage";
    
    /**
     * The name of the HN Test Page
     */
    public static final String HN_TEST_PAGE = "HNTestPage";
    
    /**
     * The name of the HN Page displayed during playback of HN Content
     */
    public static final String HN_PLAYBACK_PAGE = "HNPlaybackPage";
    
    /**
     * The name of the HN DLNA CTT Test Page
     */
    public static final String HN_DLNA_CTT_TEST_PAGE = "HNDlnaCttTestPage";
    
    /**
     * The name of the Delete Menu Page
     */
    public static final String DVR_DELETE_MENU_PAGE = "DeleteMenuPage";
    
    /**
     * The name of the VPOP Client Menu Page
     */
    public static final String VPOP_CLIENT_MENU_PAGE = "VPOPClientMenuPage";
    
    /**
     * The name of the VPOP Server Menu Page
     */
    public static final String VPOP_SERVER_MENU_PAGE = "VPOPServerMenuPage";
    
    /**
     * The name of the VPOP Tuner Menu Page
     */
    public static final String VPOP_TUNER_MENU_PAGE = "VPOPTunerMenuPage";
    
    /**
     * The name of the DVR Non-Selected Channel Menu Page
     */
    public static final String DVR_NON_SELECTED_CHANNEL_MENU_PAGE = "DVRNonSelectedChannelMenuPage";
    
    /**
     * The name of the Remote UI Server Manager Page
     */
    public static final String REMOTE_UI_SERVER_MANAGER_PAGE = "RemoteUIServerManagerPage";
    
    /**
     * The name of the HN Publish Recording Menu Page
     */
    public static final String HN_PUBLISH_RECORDING_MENU_PAGE = "HNPublishRecordingMenuPage";
    
    /**
     * The name of the HN Publish Channel Menu Page
     */
    public static final String HN_PUBLISH_CHANNEL_MENU_PAGE = "HNPublishChannelMenuPage";
    
    /**
     * The name of the HN Net Authorization Handler Menu Page
     */
    public static final String HN_NET_AUTHORIZATION_HANDLER_MENU_PAGE = "HNNetAuthorizationHandlerMenuPage";
    
    /**
     * The name of the HN Encrypted Recording Menu Page
     */
    public static final String HN_ENCRYPTED_RECORDING_MENU_PAGE = "HNEncryptedRexordingMenuPage";
    
    /**
     * The name of the HN Content Transformation Menu Page
     */
    public static final String HN_CONTENT_TRANSFORMATION_MENU_PAGE = "HNContentTransformationMenuPage";

    /**
     * The name of the HN Content Transformation Recording Page
     */
    public static final String HN_CONTENT_TRANSFORMATION_RECORDING_PAGE = "HNContentTransformationRecordingPage";
    
    /**
     * The name of the HN Content Transformation CHannel Page
     */
    public static final String HN_CONTENT_TRANSFORMATION_CHANNEL_PAGE = "HNContentTransformationChannelPage";
    
    /**
     * Constants used on HN Playback page
     */
    public static final String PLAYBACK_REMOTE_JMF = "Remote JMF Playback";
    public static final String PLAYBACK_REMOTE_SERVICE = "Remote Service Playback";
    
    // Actions for SelectorList choices
    public static final int ADD_TRANSFORMATION = 0;
    public static final int REMOVE_TRANSFORMATION = 1;
    public static final int REMOVE_RESOURCE = 2;
    public static final int SELECT_TRANSFORM_TO_ADD = 3;
    public static final int SELECT_TRANSFORM_TO_REMOVE = 4;
    public static final int SELECT_RESOURCE_TO_REMOVE = 5;
}
