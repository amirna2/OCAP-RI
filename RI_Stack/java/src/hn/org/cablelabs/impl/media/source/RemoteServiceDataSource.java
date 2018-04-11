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

package org.cablelabs.impl.media.source;

import java.io.IOException;

import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.ContentResourceImpl;
import org.cablelabs.impl.service.RemoteServiceLocator;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.service.RemoteService;

/**
 * This is the abstract base class of all HN-related data sources. It defines
 * methods that are unique to HN data sources.
 *
 * @author kmastranunzio
 */
public class RemoteServiceDataSource extends ServiceDataSource
{
    private static final Logger log = Logger.getLogger(RemoteServiceDataSource.class);

    private RemoteServiceLocator m_remoteServiceLocator;
    // This resource should only be StreamableContentResource
    private ContentResource m_resource;
    private RemoteService m_remoteService;
    private String m_contentType;

    //form to use when a ContentItem is not available (presenting mpeg video from a non-DLNA-compliant http server)    
    //use when a RemoteService is available
    public RemoteServiceDataSource(RemoteService remoteService, String contentType)
    {
        // Called from RemoteServicePlayer
        m_remoteService = remoteService;
        m_contentType = contentType;
    }    
    
    //form to use when a ContentResource is available
    public RemoteServiceDataSource(RemoteServiceLocator remoteServiceLocator, ContentResource resource)
    {
        // Called from HNServiceMgrDelegate
        m_remoteServiceLocator = remoteServiceLocator;
        // This resource should only be StreamableContentResource
        // Do the checking where its created!!
        m_resource = resource;
        m_contentType = m_resource.getContentFormat();
    }
    
    public ContentResource getContentResource()
    {
        // This resource should only be StreamableContentResource
        return m_resource;
    }
    
    public Time getDuration()
    {
        //StreamableContentResource 
        if(m_resource != null)
        {
            // The getDuration() method is implemented in ContentResourceImpl
            // NOT in StreamableContentResource.
            if( ((ContentResourceImpl)m_resource).getDuration() != null)
            {
                return ((ContentResourceImpl)m_resource).getDuration();
            }
        }

        return DURATION_UNKNOWN;
    }

    public Time getStartMediaTime()
    {
        if(m_resource != null)
        {
            ContentItem contentItem = m_resource.getContentItem();
            if (contentItem != null)
            {
                Object result = contentItem.getRootMetadataNode().getMetadata(
                        RecordingContentItem.PROP_PRESENTATION_POINT);
                if (result instanceof Long)
                {
                    // is millis, time constructor wants nanos
                    return new Time(((Long)result).longValue() * 1000000);
                }
            }
        }
        // no presentation point property, use start time of zero
        return new Time(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.media.source.ServiceDataSource#setService(javax.tv
     * .service.Service) This method does nothing with the service that is being
     * set. Instead it should use the the cached ContentItem to do a proper
     * lookup.
     */
    public void setService(Service svc)
    {
        // no-op
    }

    protected Service doGetService() throws IOException
    {
        // If we have m_remoteService return it
        if(m_remoteService != null)
        {
            return m_remoteService;
        }
        
        // Otherwise lookup using m_remoteServiceLocator
        Service svc;

        try
        {
            svc = SIManager.createInstance().getService(m_remoteServiceLocator);
        }
        catch (Exception x)
        {
            String msg = "could not lookup Service for " + m_remoteServiceLocator.toExternalForm();
            if (log.isDebugEnabled())
            {
                log.debug(msg, x);
            }
            throw new IOException(msg);
        }
        return svc;
    }

    public String getContentType()
    {
        return m_contentType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.cablelabs.impl.media.source.ServiceDataSource#disconnect()
     */
    public void disconnect()
    {
        super.disconnect();
        m_resource = null;
    }
}
