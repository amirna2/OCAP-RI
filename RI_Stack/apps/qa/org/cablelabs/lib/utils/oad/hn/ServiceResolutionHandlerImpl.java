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

package org.cablelabs.lib.utils.oad.hn;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.davic.net.InvalidLocatorException;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.service.ServiceResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Purpose: This class contains methods related to support service resolution
 * HN functionality. 
*/
public class ServiceResolutionHandlerImpl implements ServiceResolutionHandler
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(ServiceResolutionHandlerImpl.class);
    
    // Variables used for ServiceResolutionHandler testing
    private OcapLocator m_loc1 = null;
    private OcapLocator m_loc2 = null;
    private OcapLocator m_loc3 = null;
    
    private OcapLocator m_mapLoc1 = null;
    private OcapLocator m_mapLoc2 = null;
    private OcapLocator m_mapLoc3 = null;

    private OcapLocator m_mapCurrentLoc = null;
    private ChannelContentItem m_channel = null;
    
    private OcapAppDriverHN m_oadHN;
    private OcapAppDriverCore m_oadCore;
    private ChannelContent m_channelContent;
    
    ServiceResolutionHandlerImpl(OcapAppDriverHN oadHN, OcapAppDriverCore oadCore, 
                                 ChannelContent channelContent)
    {
        m_oadHN = oadHN;
        m_oadCore = oadCore;
        m_channelContent = channelContent;
        
        // Initialize variables for ServiceResolutionHandling
        try 
        {
            m_loc1 = new OcapLocator(0x9990); //sourceId 0x9990 
            m_loc2 = new OcapLocator(0x9991); //sourceId 0x9991 
            m_loc3 = new OcapLocator(0x9992); //sourceId 0x9992 
        } 
        catch (InvalidLocatorException e) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Unable to init locators..");  
            }
        } 
        
        // Following locators are used to map SPI services when ServiceResHandler
        // is invoked with channelContentItems with unknown locators
        try 
        {
            //447 qam 64, prog 1 is baby, 489 prog 2, qam256 is golf
            m_mapLoc1 = new OcapLocator(0x1AA4ADC0, 0x1, 0x08); // (baby video) qam 64
            m_mapLoc2 = new OcapLocator(0x1D258C40, 0x2, 0x10); // (golf video) qam 256
            // Note: Table tennis video does not work for live streaming for some reason
            // Do not use it!!
            m_mapLoc3 = new OcapLocator(0x26CD78C0, 0x1, 0x10); // Clouds video qam 256
        } 
        catch (InvalidLocatorException e) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Unable to init mapping locators.."); 
            }
        }         
    }
    
    /**
     * Required to implement ServiceResolutionHandler
     */
    public boolean resolveChannelItem(ChannelContentItem channel)
    {
        if (log.isInfoEnabled())
        {
            log.info("resolveChannelItem called with " + channel);
        }

        OcapLocator tuningLocator = null;
        m_channel = channel;
        boolean handled = false;
        if (channel.getChannelLocator().equals(m_loc1))
        {
            tuningLocator = m_mapLoc1;
            m_mapCurrentLoc = m_mapLoc1;
            handled = true;
        }
        else if (channel.getChannelLocator().equals(m_loc2))
        {
            tuningLocator = m_mapLoc2;
            m_mapCurrentLoc = m_mapLoc2;
            handled = true;
        }
        else if (channel.getChannelLocator().equals(m_loc3))
        {
            tuningLocator = m_mapLoc3;
            m_mapCurrentLoc = m_mapLoc3;
            handled = true;
        }
        if (!handled)
        {
            return false;
        }
        
        // Specific to ServiceResolutionHandler
        if (log.isInfoEnabled())
        {
            log.info("Calling ChannelContentItem setTuningLocator = " + 
                    tuningLocator);
        }
        try 
        {
            boolean result = channel.setTuningLocator(tuningLocator);
            if (log.isInfoEnabled())
            {
                log.info("ChannelContentItem setTuningLocator returning: " + result);
            }
            return result;
        } 
        catch (javax.tv.locator.InvalidLocatorException ex1) 
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception in resolveChannelItem()", ex1);
            }
            return false;
        }
    }

    /**
     * Update locator1 to a bogus program number (supporting testing of notifyUntuned), 
     * update locator 2 to remap from clouds to golf (supporting testing of notifyRetunePending/notifyRetuneComplete)
     * 
     */
    protected boolean updateTuningLocatorWithSRH()
    {
        // Specific to ServiceResolutionHandler
        OcapLocator loc = null;
        int freq = 0;
        int mode = 0;
        int pn = 0;
        //447 qam 64, prog 1 is baby, 489 prog 2, qam256 is golf

        //map 3 to baby
        if(m_mapCurrentLoc.equals(m_mapLoc3))
        {
            freq = 0x1AA4ADC0;
            mode = 0x08;
            pn = 1;
            if (log.isInfoEnabled()) 
            {
                log.info("remapping 3 to 1");
            }
        }
        else if(m_mapCurrentLoc.equals(m_mapLoc1))
        {
            //map 1 to bogus
            freq = 0x1D258C40; // 489 MHz
            mode = 0x10; // QAM 256
            pn = 99; //bogus
            if (log.isInfoEnabled())
            {
                log.info("remapping 1 to bogus");
            }
        }
        else
        {
            if (log.isInfoEnabled()) 
            {
                log.info("not remapping current locator: " + m_mapCurrentLoc);
            }
            return false;
        }
           
        try 
        {
            loc = new OcapLocator(freq, pn, mode); 
        } 
        catch (InvalidLocatorException ex) 
        {
            //no-op
        } 
        
        try 
        {
            boolean result = m_channel.setTuningLocator(loc);
            if (log.isInfoEnabled())
            {
                log.info("ChannelContentItem setTuningLocator returning: " + result);
            }
            return result;
        }
        catch (javax.tv.locator.InvalidLocatorException ex1) 
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception in updateTuningLocatorWithSRH()", ex1);
            }
            return false;
        }
    }

    protected boolean publishAllServicesWithSRH(long timeoutMS)
    {
        m_oadHN.clearPublishedContent();
        boolean retVal = false;
        Vector services = m_oadCore.getServicesList();

        for (int i = 0; i < services.size(); i++)
        {
            if (false == (retVal = m_channelContent.publishService(i, timeoutMS)))
            {
                break;
            }
        }
        
        // If m_localContentServer hasn't been initialized yet, initialize it
        m_oadHN.findLocalMediaServer();
        
        // Set this as the ServiceResolutionHandler
        m_oadHN.setServiceResolutionHandler((ServiceResolutionHandler)this);
        
        if (!publishSRHChannelToCDS(m_loc1, "SRH Service 1", timeoutMS)) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Error publishing channel");
            }
            return false; 
        }
        if (!publishSRHChannelToCDS(m_loc2, "SRH Service 2", timeoutMS)) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Error publishing channel");
            }
            return false; 
        }
        if (!publishSRHChannelToCDS(m_loc3, "SRH Service 3", timeoutMS)) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Error publishing channel");
            }
            return false; 
        }
        return retVal;
    }
    
    private boolean publishSRHChannelToCDS(OcapLocator loc, String name, long timeoutMS)
    {
        ExtendedFileAccessPermissions perms = new ExtendedFileAccessPermissions
        (true, true, false, false, false, true, null, null);

        ContentContainer rootContainer = m_oadHN.getRootContainer(timeoutMS);
        if (rootContainer == null) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Failed to get root content container");
            }
            return false;
        }    

        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try 
        {
            cgContainer = rootContainer.createChannelGroupContainer
            (name,perms);
        }
        catch (Exception e) 
        {
            if (log.isErrorEnabled())
            {
                log.error("createChannelGroupContainer () threw an exception" + 
                        e.toString());
            }
            return false;
        }  
        if (cgContainer == null) 
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer(...) returned null.");
            }
            return false;
        }

        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        try 
        {
            cci = ContentEntryFactory.getInstance().createChannelContentItem
            (ContentItem.VIDEO_ITEM_BROADCAST, "Channel: " + name, name, "Digital,15,2", 
                    loc, perms);
            MetadataNode md = cci.getRootMetadataNode();
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            if (log.isInfoEnabled())
            {
                log.info("Publishing primary URI: " + primaryURI);
            }
        }
        catch (Exception e) 
        {
            if (log.isErrorEnabled())
            {
                log.error("createChannelContentItem(...) threw an exception." 
                        + e.toString());
            }
            return false;
        }
        if (cci == null) 
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelContentItem(...) returned null.");
            }
            return false;
        }
        try 
        {
            if (!cgContainer.addContentEntry(cci))
            {
                if (log.isInfoEnabled())
                {
                    log.info("addContentEntry(...) returned false.");
                }
                return false; 
            }
        }
        catch (Exception e) 
        {
            if (log.isErrorEnabled())
            {
                log.error("addContentEntry(...) threw an exception." + 
                        e.toString());
            }
            return false;
        }
        String id = cci.getID();
        String channelName = m_oadCore.getInformativeChannelName(cci.getItemService());
        String title = cci.getChannelTitle();
        m_oadHN.addPublishedContent("Channel: " + id + ", " + channelName + " "  + title); 
        return true;
    }
    

    /**
     * Required to implement ServiceResolutionHandler
     */
    public boolean notifyTuneFailed(ChannelContentItem channel)
    {
        // Re-try tune, return true
        return true;
    }
}

