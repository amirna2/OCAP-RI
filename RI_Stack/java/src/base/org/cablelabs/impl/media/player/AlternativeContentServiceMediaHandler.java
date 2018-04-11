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

import javax.media.IncompatibleSourceException;
import javax.media.Time;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.presentation.AlternativeContentServicePresentation;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * This is a service player that renders alternativecontent only (can't be constructed via standalone JMF)
 */
public class AlternativeContentServiceMediaHandler extends AbstractServicePlayer implements ServiceMediaHandler
{
    private static final Logger log = Logger.getLogger(AlternativeContentServiceMediaHandler.class);

    private final Class alternativeContentClass;
    private final int alternativeContentReasonCode;

    /**
     * Constructor called by {@link ServiceContext}.
     *
     * @param alternativeContentClass AlternativeContentErrorEvent or subclass
     * @param alternativeContentReasonCode alternative content reason code
     */
    public AlternativeContentServiceMediaHandler(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage, Class alternativeContentClass, int alternativeContentReasonCode)
    {
        super(cc, lock, resourceUsage);
        this.alternativeContentClass = alternativeContentClass;
        this.alternativeContentReasonCode = alternativeContentReasonCode;
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "constructing AlternativeContentServiceMediaHandler");
        }
        try
        {
            setSource(new NoOpAlternativeContentDataSource());
        }
        catch (IOException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(getId() + "Exception setting NoOp alternativecontent datasource", e);
            }
        }
        catch (IncompatibleSourceException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(getId() + "Exception setting NoOp alternativecontent datasource", e);
            }
        }
    }

    protected boolean isServiceBound()
    {
        return true;
    }

    protected Presentation createPresentation()
    {
        return new AlternativeContentServicePresentation(this, showVideo(), initialSelection, getScalingBounds(),
                alternativeContentClass, alternativeContentReasonCode, getClock().getMediaTime(), getClock().getRate());
    }

    public boolean getMute()
    {
        return false;
    }

    public float getGain()
    {
        return 0.0F;
    }

    private class NoOpAlternativeContentDataSource extends ServiceDataSource
    {
        public String getContentType()
        {
            return null;
        }

        protected Service doGetService() throws IOException
        {
            return null;
        }

        public void connect() throws IOException
        {
            //no-op
        }

        public void disconnect()
        {
            //no-op
        }

        public void start() throws IOException
        {
            //no-op
        }

        public void stop() throws IOException
        {
            //no-op
        }

        public Object[] getControls()
        {
            return new Object[0];
        }

        public Object getControl(String controlType)
        {
            return null;
        }

        public Time getDuration()
        {
            return null;
        }
    }
}
