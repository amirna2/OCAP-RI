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
package org.cablelabs.impl.media.presentation;

import javax.media.Time;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.ServiceSession;
import org.ocap.media.MediaPresentationEvaluationTrigger;

/**
 * CannedAbstractServicePresentation
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedAbstractServicePresentation extends AbstractServicePresentation
{

    /**
     * @param pc
     * @param isShown
     * @param initialSelection
     * @param bounds
     */
    public CannedAbstractServicePresentation(ServicePresentationContext pc, boolean isShown,
            Selection initialSelection, ScalingBounds bounds, Time startMediaTime, float startRate)
    {
        super(pc, isShown, initialSelection, bounds, startMediaTime, startRate);
        // TODO Auto-generated constructor stub
    }

    ExtendedNetworkInterface getNetworkInterface()
    {
        return null;
    }

    protected CreateSessionResult doCreateSession(Selection selection, Time mediaTime, float rate) throws NoSourceException, MPEException
    {
        return null;
    }

    protected void doStartSession(ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException, NoSourceException
    {
        //no-op
    }


    protected void doUpdateSession(Selection selection) throws NoSourceException, MPEException
    {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.AbstractPresentation#doSetMediaTime
     * (javax.media.Time)
     */
    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        context.getClock().setMediaTime(mt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.AbstractPresentation#doGetMediaTime
     * ()
     */
    protected Time doGetMediaTime()
    {
        return context.getClock().getMediaTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.AbstractPresentation#doSetRate(
     * float)
     */
    protected float doSetRate(float rate)
    {
        return context.getClock().setRate(rate);
    }
}