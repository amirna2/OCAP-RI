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

import javax.media.GainControl;
import javax.media.IncompatibleSourceException;
import javax.media.protocol.DataSource;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.presentation.BroadcastServicePresentation;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * This is a service player that plays a service only from live broadcast.
 * 
 * @author schoonma
 */
public abstract class BroadcastServicePlayer extends AbstractServicePlayer
{
    private static final Logger log = Logger.getLogger(BroadcastServicePlayer.class);
    private GainControlImpl gainControl;

    /**
     * Constructor called by {@link javax.tv.service.selection.ServiceContext}.
     * 
     * @param cc
     *            callerContext
     */
    protected BroadcastServicePlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        gainControl = new GainControlImpl();
        addControls(new ControlBase[] { gainControl });

        if (log.isDebugEnabled())
        {
            log.debug(getId() + "constructing BroadcastPlayer");
        }
    }

    public BroadcastAuthorization getBroadcastAuthorization()
    {
        boolean isEAS;
        synchronized(getLock())
        {
            isEAS = !isClosed() && getResourceUsage().isResourceUsageEAS();
        }
        
        return new BroadcastAuthorization(this, isEAS);
    }

    protected Object doAcquireRealizeResources()
    {
        // If this player was created by an application, assign the Service from
        // the DataSource.
        // Also, queue up the selection request.
        if (!isServiceContextPlayer())
        {
            // Get the Service from the DataSource.
            Service svc = ((ServiceDataSource) getSource()).getService();
            if (svc == null)
            {
                return "could not get Service from ServiceDataSource";
            }

            setNetworkInterface(Util.findNI(getOwnerCallerContext(), Util.getServiceDetails(svc).getLocator()));

            // Select the Service.
            try
            {
                //null locators
                setInitialSelection(Util.getServiceDetails(svc), null);
            }
            catch (Exception x)
            {
                return "error in selectService: " + x;
            }
        }

        return super.doAcquireRealizeResources();
    }

    public float getGain()
    {
        return gainControl.getDB();
    }

    public boolean getMute()
    {
        return gainControl.getMute();
    }

    public GainControl getGainControl()
    {
        return gainControl;
    }

    public void setSource(DataSource source) throws IOException, IncompatibleSourceException
    {
        if (!(source instanceof org.cablelabs.impl.media.protocol.ocap.DataSource))
            throw new IncompatibleSourceException("BroadcastPlayer assigned non-broadcast DataSource");

        super.setSource(source);
    }

    protected Presentation createPresentation()
    {
        return new BroadcastServicePresentation(this, showVideo(), initialSelection, getScalingBounds(), getClock().getMediaTime(), getClock().getRate());
    }
}
