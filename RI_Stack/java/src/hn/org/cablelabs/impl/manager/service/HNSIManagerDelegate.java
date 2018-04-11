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

package org.cablelabs.impl.manager.service;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.service.RemoteServiceLocator;
import org.cablelabs.impl.util.HNUtil;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.service.RemoteService;

public class HNSIManagerDelegate implements SIManagerDelegate
{
    private static final Logger log = Logger.getLogger(HNSIManagerDelegate.class.getName());

    public Service getService(Locator locator) throws InvalidLocatorException, SecurityException
    {
        if (locator instanceof RemoteServiceLocator)
        {
            RemoteService service = ((RemoteServiceLocator)locator).getService();
            if(service == null)
            {
                // This should be rare!
                // We are going to extract the 'uuid' and 'content_id' and lookup
                // remote service
                service = getRemoteService((RemoteServiceLocator)locator);
                
                if (service == null)
                {
                    throw new InvalidLocatorException(locator, "Remote service not found");
                }
                ((RemoteServiceLocator)locator).setService(service);
            }
            return service;
        }
        return null;
    }

    /**
     * Get the RemoteService which has a locator with the specified external
     * form.
     * 
     * @param locator
     *            The remote service url
     * @return The remote service or null if not found
     */
    private RemoteService getRemoteService(final RemoteServiceLocator locator)
    {
        HNUtil util = new HNUtil();
        if (log.isDebugEnabled())
        {
            log.debug("getRemoteService for " + locator);
        }

        RemoteServiceLocator rsl = (RemoteServiceLocator)locator;
        String uuid = rsl.getDeviceId();
        
        if(uuid == null)
        {
            // Must be the other form of locator with 'object_id'
            // Will not have uuid or content_id
            if (log.isDebugEnabled())
            {
                log.debug("getRemoteService cannot find uuid for " + locator);
            }
            return null;
        }
        
        String content_id = rsl.getContentId();

        ContentItem contentItem = util.getContentItemFromID(uuid, content_id);
        if (contentItem == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to find content item with id: " + content_id);
            }            
        }
        else
        {
            if (contentItem.getItemService() == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Content id: " + content_id + " had no associated item service");
                }                            
            }
            else
            {
                RemoteService rs = (RemoteService) contentItem.getItemService();
                if (log.isInfoEnabled())
                {
                    log.info("getRemoteService remoteService: " + rs);
                }
                return rs;
            }
        }
        
        return null;
    }
}
