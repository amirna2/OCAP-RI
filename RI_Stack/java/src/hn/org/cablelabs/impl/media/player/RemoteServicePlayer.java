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

package org.cablelabs.impl.media.player;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.media.GainControl;
import javax.media.IncompatibleSourceException;
import javax.media.StopEvent;
import javax.media.protocol.DataSource;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.presentation.PlaybackPresentationContext;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.presentation.RemoteServicePresentation;
import org.cablelabs.impl.media.source.LocatorDataSource;
import org.cablelabs.impl.media.source.RemoteServiceDataSource;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.RemoteServiceImpl;
import org.cablelabs.impl.service.SIRequestException;
import org.ocap.media.S3DConfiguration;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;

public class RemoteServicePlayer extends AbstractServicePlayer implements PlaybackPresentationContext,
        ServiceMediaHandler
{
    private static final Logger log = Logger.getLogger(RemoteServicePlayer.class);

    private GainControlImpl gainControl;

    //available if contentFeatures.dlna.org header is present after the HEAD request
    private HNStreamProtocolInfo protocolInfo;

    public RemoteServicePlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        gainControl = new GainControlImpl();
        addControls(new ControlBase[] { gainControl });
    }

    public void setSource(DataSource ds) throws IncompatibleSourceException, IOException
    {
        RemoteServiceDataSource remoteServiceDataSource;
        if (ds instanceof RemoteServiceDataSource)
        {
            remoteServiceDataSource = (RemoteServiceDataSource)ds;
            super.setSource(remoteServiceDataSource);
            clockSetMediaTime(remoteServiceDataSource.getStartMediaTime(), false);
        }
        else
        {
            if (ds instanceof LocatorDataSource)
            {
                //string key, list value
                Map headers = ((LocatorDataSource)ds).getHeaders();
                List contentFeatures = (List)headers.get("contentFeatures.dlna.org");
                if (contentFeatures != null && contentFeatures.size() > 0)
                {
                    // Need to build string to create all four fields of protocol info
                    StringBuffer sb = new StringBuffer(64);
                    sb.append(HNStreamProtocolInfo.HTTP_TRANSPORT);
                    sb.append(":");
                    sb.append(HNStreamProtocolInfo.NETWORK_WILDCARD);
                    sb.append(":");
                    sb.append(((LocatorDataSource)ds).getFullContentType());
                    sb.append(":");
                    sb.append(contentFeatures.get(0).toString());
                    protocolInfo = new HNStreamProtocolInfo(sb.toString());
                }
                else
                {
                    // Non-DLNA server. Spoof the protocol info structure just
                    // so that we can pass the mimeType / contentFormat strings.
                    protocolInfo = new HNStreamProtocolInfo("*:*:" + ds.getContentType());
                }
            }
            //was not a remoteServiceDataSource - create one
            // create RemoteService and pass it into dataSource
            RemoteServiceImpl remoteService = new RemoteServiceImpl(ds.getLocator().toExternalForm(), new Object());
            remoteServiceDataSource = new RemoteServiceDataSource(remoteService, ds.getContentType());
            remoteServiceDataSource.connect();
            super.setSource(remoteServiceDataSource);
            clockSetMediaTime(remoteServiceDataSource.getStartMediaTime(), false);
        }
    }

    protected Presentation createPresentation()
    {
        return new RemoteServicePresentation(this, showVideo(), initialSelection, getScalingBounds(), getClock().getMediaTime(), getClock().getRate(), protocolInfo);
    }

    protected boolean isServiceBound()
    {
        return true;
    }

    protected Object doAcquireRealizeResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doAcquireRealizeResources");
        }

        RemoteServiceImpl service = (RemoteServiceImpl) ((ServiceDataSource) (getSource())).getService();
        try
        {
            if (!isServiceContextPlayer())
            {
                //default application player context set in super constructor is ok, just set initial selection
                //null locators
                setInitialSelection(service.getDetails(), null);
            }
            return super.doAcquireRealizeResources();
        }
        catch (SIRequestException e)
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "Unable to acquire realized resources", e);
            }
            return "Unable to retrieve SI - " + e.getMessage();
        }
        catch (InterruptedException e)
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "Unable to acquire realized resources", e);
            }
            return "Interrupted - " + e.getMessage();
        }
    }

    public GainControl getGainControl()
    {
        return gainControl;
    }

    protected int doGetInputVideoScanMode(int handle)
    {
        return HNAPIImpl.getInputVideoScanMode(handle);
    }

    protected S3DConfiguration doGetS3DConfiguration(int handle)
    {
        return HNAPIImpl.getS3DConfiguration(handle);
    }

    /**
     * Notify the context that the presentation has reached the end of the
     * content.
     * @param rate
     */
    public void notifyEndOfContent(float rate)
    {
        // pause
        postEvent(new EndOfContentEvent(this, rate));
    }

    /**
     * Notify the context that the presentation has reached the start of the
     * content.
     * @param rate
     */
    public void notifyBeginningOfContent(float rate)
    {
        // pause
        postEvent(new BeginningOfContentEvent(this, rate));
    }

    /**
     * Notify the context that the presentation's playback session has been
     * closed by the native layer.
     */
    public void notifySessionClosed()
    {
        // assuming we want 'resources removed' to be the reason for the
        // presentation terminated event
        stop(new StopEvent(this, Started, Started, Prefetched, getMediaTime()));
    }

    public boolean getMute()
    {
        return gainControl.getMute();
    }

    public float getGain()
    {
        return gainControl.getDB();
    }
}
