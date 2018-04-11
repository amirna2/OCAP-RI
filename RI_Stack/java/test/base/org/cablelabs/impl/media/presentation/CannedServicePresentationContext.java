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

import javax.tv.locator.Locator;

import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.media.player.BroadcastAuthorization;
import org.dvb.spi.selection.SelectionSession;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;

/**
 * CannedServicePresentationContext
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedServicePresentationContext extends CannedVideoPresentationContext implements
        ServicePresentationContext
{
    /**
	 *
	 */
    public CannedServicePresentationContext()
    {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * getNetworkInterface(javax.tv.service.Service)
     */
    public ExtendedNetworkInterface getNetworkInterface()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public BroadcastAuthorization getBroadcastAuthorization()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.ServicePresentationContext#notifyCAStop
     * ()
     */
    public void notifyCAStop()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyMediaAuthorization(org.cablelabs.impl.media.player.Authorization)
     */
    public void notifyMediaAuthorization(ComponentAuthorization auth)
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyMediaSelectFailed(javax.tv.locator.Locator[])
     */
    public void notifyMediaSelectFailed(Locator[] locators)
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyMediaSelectSucceeded(javax.tv.locator.Locator[])
     */
    public void notifyMediaSelectSucceeded(Locator[] locators)
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyNoComponentSelected()
     */
    public void notifyNoComponentSelected()
    {
        // TODO Auto-generated method stub

    }

    public void notifyAlternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
    {
        // TODO: implement
    }

    public void notifyNormalContent()
    {
        // TODO: implement
    }

    public void notifyNoReasonAlternativeMediaPresentation(ElementaryStreamExt[] streams, OcapLocator locator, MediaPresentationEvaluationTrigger trigger, boolean digital)
    {
        // TODO: implement
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.ServicePresentationContext#notifyNoData
     * ()
     */
    public void notifyNoData()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyNoSource(java.lang.String)
     */
    public void notifyNoSource(String msg, Throwable throwable)
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.ServicePresentationContext#
     * notifyPresentationChanged(int, int)
     */
    public void notifyPresentationChanged(int reason)
    {
        // TODO Auto-generated method stub

    }

    public void notifySessionClosed(int sessionHandle)
    {
        // TODO Auto-generated method stub

    }

    public void notifySessionComplete(int sessionHandle, boolean succeeded)
    {
        // TODO Auto-generated method stub

    }

    public void notifyStartingSession(int sessionHandle)
    {
        // TODO Auto-generated method stub

    }

    public void notifyStoppingSession(int sessionHandle)
    {
        // TODO Auto-generated method stub

    }

    public SelectionSession getSelectionSession()
    {
        return null;
    }

}
