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

import java.io.File;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.navigation.ContentList;

/**
 * Purpose: This class contains methods related to support hidden content
 * item HN functionality. 
*/
public class HiddenContent
{
    private static final Logger log = Logger.getLogger(HiddenContent.class);

    private OcapAppDriverHN m_oadHN = null;
    private Vector m_hiddenContainers = null;

    HiddenContent(OcapAppDriverHN oadHN)
    {
        m_hiddenContainers = new Vector();
        m_oadHN = oadHN;
    }
    
    protected boolean createContentContainer(boolean readWorld, boolean writeWorld,
            boolean readOrg, boolean writeOrg, boolean readApp, boolean writeApp,
            int[] otherOrgRead, int[] otherOrgWrite, String containerName, long timeoutMS)
    {
        try
        {
            ContentContainer rootContainer = m_oadHN.getRootContainer(timeoutMS);
            if (rootContainer != null)
            {
                if (rootContainer.createContentContainer(containerName, 
                        m_oadHN.createEFAB(readWorld,writeWorld,readOrg,writeOrg,readApp,
                                writeApp,otherOrgRead,otherOrgWrite)))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Success creating the container :"+containerName);
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Failure creating the container :"+containerName);
                    }
                }
                addContentContainer(containerName, timeoutMS);
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createHiddenContentContainer threw exception", e);
            }

            return false;
        }

        return true;

    }

    /**
     * This method will add the created container to an existing list.
     */
    private void addContentContainer(String containerName, long timeoutMS)
    {
        ContentServerNetModule mediaServer = m_oadHN.getLocalContentServerNetModule();
        if (mediaServer != null)
        {
            // Get content list using search with no search criteria
            ContentList contentList = m_oadHN.searchContainer(mediaServer, RiExerciserConstants.ROOT_CONTAINER_ID, timeoutMS);

            if (contentList != null)
            {
                while (contentList.hasMoreElements())
                {
                    ContentEntry contentEntry =
                                (ContentEntry)contentList.nextElement();
                    if (contentEntry instanceof ContentContainer)
                    {
                       String l_name = ((ContentContainer) contentEntry).getName();
                       if(l_name.equalsIgnoreCase(containerName))
                       {
                           m_hiddenContainers.add((ContentContainer) contentEntry);
                       }
                    }
                }
            }
        }
    }
    
    protected int getHiddenContainerIndex(String containerName)
    {
        if (m_hiddenContainers != null && m_hiddenContainers.size()!= 0)
        {
            for(int i = 0; i < m_hiddenContainers.size();i++)
            {
                 ContentContainer l_cc =(ContentContainer)m_hiddenContainers.get(i);
                 if(l_cc.getName().equals(containerName))
                 {
                     return i;
                 }
            }
        }
        else
        {
             return -1;
        }
        return -1;
    }
    
    protected int getNumHiddenContainer()
    {
        if (m_hiddenContainers != null)
        {
            return m_hiddenContainers.size();
        }
        return -1;
    }
    
    protected boolean createItemsForContainer(int noOfItemstoCreate,int containerIndex,
            String contentItemName, boolean readWorld, boolean writeWorld, 
            boolean readOrg, boolean writeOrg, boolean readApp, boolean writeApp,
            int[] otherOrgRead, int[] otherOrgWrite, long timeoutMS)
    {
        try
        {
            if (m_hiddenContainers != null && m_hiddenContainers.size() > 0)
            {
                ContentContainer l_hiddenContainer = 
                    (ContentContainer)m_hiddenContainers.get(containerIndex);
                for (int i = 0; i < noOfItemstoCreate; i++)
                {
                    File cFile = new File(contentItemName+i);

                    l_hiddenContainer.createContentItem(cFile, contentItemName + i,
                            m_oadHN.createEFAB(readWorld,writeWorld,readOrg,writeOrg,readApp,
                                    writeApp,otherOrgRead,otherOrgWrite));
                }
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("createItemsForHiddenContainer threw exception", e);
            }

            return false;
        }        
        return true;
    }
}

