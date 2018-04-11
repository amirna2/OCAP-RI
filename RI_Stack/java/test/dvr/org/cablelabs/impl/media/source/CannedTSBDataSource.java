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

import java.awt.Container;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import org.davic.net.tuning.NetworkInterface;
import org.dvb.media.VideoTransformation;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.media.player.TSBPlayerTest;
import org.cablelabs.impl.service.javatv.selection.DVRPresentation;
import org.cablelabs.impl.service.javatv.selection.DVRServiceContextExt;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;

/**
 * CannedTSBDataSource
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedTSBDataSource extends TSBDataSource
{
    // private CannedTSBOExt tsbo;
    private Service service;

    private ExtendedNetworkInterface ni;

    public static class CannedDVRServiceContext implements DVRServiceContextExt
    {
        public void setDestroyWhenIdle(boolean destroyWhenIdle)
        {
        }

        public void stopAbstractService()
        {
        }; // adc

        public void setAvailableServiceContextDelegates(List serviceContextDelegates)
        {
            // TODO: implement
        }

        public void forceEASTune(Service service)
        {
            // TODO: implement
        }

        public void unforceEASTune()
        {
            
        }

        public TimeShiftWindowClient getTimeShiftWindowClient()
        {
            return new TSBPlayerTest.CannedTimeShiftWindowClient();
        }

        public void requestBuffering()
        {
            // no-op
        }

        public boolean isBuffering()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isPresenting()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isDestroyed()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public NetworkInterface getNetworkInterface()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void setDefaultVideoTransformation(VideoTransformation vt)
        {

        }

        public void setPersistentVideoMode(boolean enable)
        {
            // TODO Auto-generated method stub

        }

        public void setApplicationsEnabled(boolean appsEnabled)
        {
            // TODO Auto-generated method stub

        }

        public boolean isAppsEnabled()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isPersistentVideoMode()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public void swapSettings(ServiceContext sc, boolean audioUse, boolean swapAppSettings)
                throws IllegalArgumentException
        {
            // TODO Auto-generated method stub

        }

        public AppDomain getAppDomain()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void setInitialBackground(VideoTransformation trans)
        {
            // TODO Auto-generated method stub

        }

        public void setDefautVideoTransformation(org.dvb.media.VideoTransformation trans)
        {
            // TODO Auto-generated method stub

        }

        public void setInitialComponent(Container parent, Rectangle rect)
        {
            // TODO Auto-generated method stub

        }

        public CallerContext getCallerContext()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public CallerContext getCreatingContext()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void select(Service selection) throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public void select(Locator[] components) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
            // TODO Auto-generated method stub

        }

        public void stop() throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public void destroy() throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public ServiceContentHandler[] getServiceContentHandlers() throws SecurityException
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Service getService()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void addListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        public void removeListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        public DVRPresentation getDVRPresentation(Service service)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceMediaHandler addServiceContextCallback(ServiceContextCallback callback, int priority)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void removeServiceContextCallback(ServiceContextCallback callback)
        {
            // TODO Auto-generated method stub
        }
    }

    /**
     *
     */
    public CannedTSBDataSource(Service svc)
    {
        super(new CannedDVRServiceContext(), new Time(Double.POSITIVE_INFINITY), 1);
        // tsbo = new CannedTSBOExt();
        setService(svc);
    }

    /**
     * @param source
     */
    /*
     * public CannedTSBDataSource(MediaLocator source) { super(source); tsbo =
     * new CannedTSBOExt(); }
     */
    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#getContentType()
     */
    public String getContentType()
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#connect()
     */
    public void connect()
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#disconnect()
     */
    public void disconnect()
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#start()
     */
    public void start() throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#stop()
     */
    public void stop() throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.source.ServiceDataSource#getService()
     */
    public Service getService()
    {
        // TODO (Josh) Implement
        return service;
    }

    public void setService(Service s)
    {
        service = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.ServiceDataSource#setNI(org.cablelabs
     * .impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void setNI(ExtendedNetworkInterface eni)
    {
        // TODO (Josh) Implement
        this.ni = eni;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.source.ServiceDataSource#getNI()
     */
    public ExtendedNetworkInterface getNI()
    {
        // TODO (Josh) Implement
        return ni;
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
     * @see javax.media.Duration#getDuration()
     */
    public Time getDuration()
    {
        // TODO (Josh) Implement
        return null;
    }
}
