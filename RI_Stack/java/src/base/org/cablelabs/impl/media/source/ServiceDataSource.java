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

/**
 * 
 */
package org.cablelabs.impl.media.source;

import java.io.IOException;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.DataSource;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;


/**
 * This is an abstract base class {@link DataSource}
 * implementations that deliver content from a {@link Service}.
 * 
 * @author schoonma
 */
public abstract class ServiceDataSource extends DataSource
{
    private static final Logger log = Logger.getLogger(ServiceDataSource.class);

    /** Subclasses must define this appropriately. */
    abstract public Time getDuration();

    /**
     * Subclasses must define this method to return the appopriate content type
     * for creating the associated Player.
     */
    public abstract String getContentType();

    /**
     * This is the {@link Service} represented by this {@link ServiceDataSource}
     * . It is initialized by the {@link #connect()} method.
     */
    private Service service;

    /**
     * @return Returns the {@link Service} represented by this
     *         {@link ServiceDataSource}. If the {@link DataSource} has not yet
     *         been connected (via {@link #connect()}), then this returns
     *         <code>null</code>.
     */
    public Service getService()
    {
        return service;
    }

    /**
     * Explicitly assign the {@link Service}.
     * 
     * @param svc
     *            - the {@link Service} to assign
     */
    public void setService(Service svc)
    {
        this.service = svc;
    }

    /**
     * Associate the {@link Service} that is represented by this
     * {@link ServiceDataSource}.
     */
    public void connect() throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("connecting ServiceDataSource");
        }

        // If a Service is already assigned, there is no need to look it up.
        if (service != null)
            return;
        // Otherwise, look it up.
        service = doGetService();

    }

    /**
     * Return the {@link Service} identified by the {@link MediaLocator}
     * assigned to this {@link ServiceDataSource}.
     * 
     * @return Returns the {@link Service}.
     * @throws IOException
     *             If the {@link Service} can't be found.
     */
    protected Service doGetService() throws IOException
    {
        Service svc;

        MediaLocator ml = getLocator();
        if (ml == null) throw new NullPointerException("null MediaLocator");

        String url = ml.toExternalForm();
        try
        {
            OcapLocator ol = new OcapLocator(url);
            svc = SIManager.createInstance().getService(ol);
        }
        catch (Exception x)
        {
            String msg = "could not lookup Service for " + url;
            if (log.isDebugEnabled())
            {
                log.debug(msg, x);
            }
            throw new IOException(msg);
        }
        return svc;
    }

    public void disconnect()
    {
        service = null;
    }

    public void start() throws IOException
    { /* no-op */
    }

    public void stop() throws IOException
    { /* no-op */
    }

    public Object[] getControls()
    {
        return new Object[0];
    }

    public Object getControl(String controlType)
    {
        return null;
    }
}
