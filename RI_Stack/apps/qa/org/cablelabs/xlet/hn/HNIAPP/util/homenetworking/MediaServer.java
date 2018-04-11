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
package org.cablelabs.xlet.hn.HNIAPP.util.homenetworking;

import java.util.Enumeration;

import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.navigation.ContentList;
/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */

public class MediaServer implements NetActionHandler
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    /**
     * Device object corresponding to media server
     */
    private Device m_device;

    /**
     * New content server net module
     */
    private ContentServerNetModule m_contentServerNetModule;

    /**
     * Flag set to indicate a media server has been removed from network
     */
    public boolean m_removedFromNetwork;

    /**
     * Flag indicating that this media server is currently active
     */
    private boolean m_isActive;

    /**
     * Name of the Media Server
     */
    private String m_name;

    /**
     * Net Module ID of the device which hosts this UPnP Media Server
     */
    private String m_netModuleID;

    /**
     * Content List from the server.
     */
    private ContentList contentList = null;

    /**
     * Construct a MediaServer from the Device object.
     * 
     * @param device
     *            - Device object
     * @param netModuleID
     *            - NetModuleID of the device
     */
    protected MediaServer(Device device, String netModuleID) throws IllegalArgumentException
    {
        m_device = device;
        m_netModuleID = netModuleID;
        m_name = m_device.getProperty(Device.PROP_FRIENDLY_NAME);
    }

    /*
     * Getters and Setters - start
     */
    public boolean isActive()
    {
        return m_isActive;
    }

    public String getServerName()
    {
        return m_name;
    }

    public String getNetModuleID()
    {
        return m_netModuleID;
    }

    public ContentList getContentList()
    {
        return contentList;
    }

    /*
     * Getters and Setters - end
     */

    /**
     * Sets flag to indicate that the media server has been removed from the
     * network.
     */
    protected void removedFromNetwork()
    {
        m_removedFromNetwork = true;
    }

    /**
     * This method sets the ContentServerNetModule for the Media Server.
     * Iterates through the net module list and retrieves the
     * ContentServerNetModule.
     */
    public void activate()
    {
        // Set flag to indicate this media server is active
        m_isActive = true;
        if (m_contentServerNetModule == null)
        {
            if (Device.TYPE_MEDIA_SERVER.equals(m_device.getType()))
            {
                // Get the NetModules associated with the device
                NetList moduleList = m_device.getNetModuleList();
                Enumeration enm = moduleList.getElements();
                while (enm.hasMoreElements())
                {
                    NetModule module = (NetModule) enm.nextElement();
                    if (module instanceof ContentServerNetModule)
                    {
                        m_contentServerNetModule = (ContentServerNetModule) module;
                        hnLogger.homeNetLogger("ContentServerNetModule:" + m_contentServerNetModule);
                    }
                }
            }
        }
    }

    /**
     * This method retrieves the data present in the Media server by using the
     * requestBrowseEntries on ContentServerNetModule.
     * 
     * @param handler
     *            NetActionHandler
     * @param startingEntryID
     *            Start entry id
     * @param propertyFilter
     *            Filter to be applied
     * @param doBrowseChildren
     *            Indicates if the children should be browsed
     * @param startingIdx
     *            start index
     * @param requestedCnt
     *            total count
     * @param sortCriteria
     *            Sorting criteria
     * @return NetActionRequest.
     */
    public NetActionRequest browseData(String startingEntryID, String propertyFilter, boolean doBrowseChildren,
            int startingIdx, int requestedCnt, String sortCriteria)
    {
        NetActionRequest netActionRequest = null;
        if ((m_isActive) && (m_contentServerNetModule != null))
        {
            netActionRequest = m_contentServerNetModule.requestBrowseEntries(startingEntryID, propertyFilter,
                    doBrowseChildren, startingIdx, requestedCnt, sortCriteria, this);
        }
        return netActionRequest;
    }

    /**
     * Retrieves the ContentList from the event response.
     */
    public void notify(NetActionEvent event)
    {
        if (event.getActionStatus() == NetActionEvent.ACTION_COMPLETED)
        {
            contentList = (ContentList) event.getResponse();
            hnLogger.homeNetLogger("content list size:" + contentList.size());
        }
    }
}
