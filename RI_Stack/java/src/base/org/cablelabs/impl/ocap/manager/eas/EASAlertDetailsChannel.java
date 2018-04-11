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

package org.cablelabs.impl.ocap.manager.eas;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.ocap.system.EASEvent;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A concrete instance of {@link EASAlert} that represents the details channel
 * alert strategy. This strategy tunes to an in-band (IB) or out-of-band (OOB)
 * details channel to present the alert.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASAlertDetailsChannel extends EASAlert
{
    // Class Fields

    // Class Fields

    // Class Fields

    private static final Logger log = Logger.getLogger(EASAlertDetailsChannel.class);

    // Class Methods

    /**
     * Create a new instance of the {@link EASAlertDetailsChannel} strategy for
     * presenting EAS content from a details channel.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return an instance of {@link EASAlertDetailsChannel}
     */
    public static EASAlertDetailsChannel create(final EASState state, final EASMessage message)
    {
        return new EASAlertDetailsChannel(state, message);
    }

    // Instance Fields

    private Service m_detailsService;

    private EASTuner m_tuner;

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    EASAlertDetailsChannel(final EASState state, final EASMessage message)
    {
        super(state, message);
        this.m_tuner = new EASTuner(this);
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#getReason()
     */
    public int getReason()
    {
        return EASEvent.EAS_DETAILS_CHANNEL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#isForceTune()
     */
    public boolean isForceTune()
    {
        return this.m_tuner.isForceTune();
    }

    /**
     * Call back method from {@link EASTuner} indicating that the presentation
     * failed.
     */
    public void presentationFailed()
    {
        EASState.s_easManagerContext.getCurrentState().retryAlert();
    }

    /**
     * Call back method indicating that the presentation has terminated.
     * 
     * @see EASTuner#receiveServiceContextEvent(javax.tv.service.selection.ServiceContextEvent)
     * @see #stopPresentation(boolean)
     */
    public void presentationTerminated()
    {
        // Notify native that we are no longer displaying the details channel.
        displayAlert(false);

        // Restore graphics plane for normal presentation.
        this.m_graphicsManager.setVisible(true);

        // Complete alert processing.
        super.presentationTerminated();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#processAlert()
     */
    public void processAlert()
    {
        if (log.isInfoEnabled())
        {
            log.info("Processing details channel alert...");
        }
        Locator locator = this.m_easMessage.getDetailsChannelLocator();
        try
        {
            this.m_detailsService = this.m_siManager.getService(locator);

            // Warn registered listeners that EAS is about to acquire resources.
            super.m_easState.warnEASListeners(getReason());

            // Select and present the service (potential asynchronous call).
            if (this.m_tuner.select(this.m_detailsService))
            {
                // Details channel already tuned and presenting.
                presentationStarted();
            }
        }
        catch (InvalidLocatorException e)
        {
            SystemEventUtil.logRecoverableError("Failed to lookup service for locator:<" + locator + ">", e);
            presentationFailed();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#startPresentation()
     */
    public void startPresentation()
    {
        // Hide graphics plane for EAS details channel presentation --
        // presentation already started by tuning to it.
        if (log.isInfoEnabled()) 
        {
            log.info("startPresentation - setting graphics to not visible");
        }
        super.m_graphicsManager.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASAlert#stopPresentation(boolean)
     */
    public void stopPresentation(boolean force)
    {
        if (log.isInfoEnabled()) 
        {
            log.info("stopPresentation - force: " + force);
        }

        // Stop service presentation (asynchronous call).
        this.m_tuner.stop();
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        return "EASAlertDetailsChannel";
    }
}
