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
import javax.tv.service.Service;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.protocol.ocap.DataSource;
import org.cablelabs.impl.service.ServiceContextExt;

/**
 * CannedOcapServiceDataSource
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedOcapServiceDataSource extends DataSource
{
    private ServiceContextExt serviceCtx;

    private Service service;

    private ExtendedNetworkInterface ni;

    private Time duration;

    /** 
     *  
     */
    public CannedOcapServiceDataSource()
    {
        super();
        duration = new Time(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.OcapServiceDataSource#reconnect(org.ocap
     * .net.OcapLocator)
     */
    public void reconnect(OcapLocator locator) throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.ServiceDataSource#setNI(org.cablelabs
     * .impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void setNI(ExtendedNetworkInterface ni)
    {
        this.ni = ni;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.source.ServiceDataSource#getNI()
     */
    public ExtendedNetworkInterface getNI()
    {
        return ni;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#connect()
     */
    public void connect() throws IOException
    {
        super.connect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#disconnect()
     */
    public void disconnect()
    {
        super.disconnect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#getContentType()
     */
    public String getContentType()
    {
        return super.getContentType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#start()
     */
    public void start() throws IOException
    {
        super.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#stop()
     */
    public void stop() throws IOException
    {
        super.stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType)
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.Controls#getControls()
     */
    public Object[] getControls()
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.Duration#getDuration()
     */
    public Time getDuration()
    {
        return duration;
    }

    public ServiceContextExt getServiceCtx()
    {
        // TODO Auto-generated method stub
        return serviceCtx;
    }

    public void setServiceCtx(ServiceContextExt sce)
    {
        serviceCtx = sce;
    }

    public Service getService()
    {
        // TODO Auto-generated method stub
        return service;
    }

    public void setService(Service s)
    {
        service = s;
    }

    public void cannedSetDuration(Time t)
    {
        duration = t;
    }
}
