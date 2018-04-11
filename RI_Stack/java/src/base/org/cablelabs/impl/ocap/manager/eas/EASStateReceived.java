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

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.EASAlert.EASAlertEmpty;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.ocap.system.EASHandler;
import org.ocap.system.EASManager;


/**
 * A concrete implementation of {@link EASState} that provides the message
 * processing actions for an EAS message in the "received" state.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASStateReceived extends EASState
{
    // Class Constants

    static final EASState INSTANCE = new EASStateReceived();

    private static final Logger log = Logger.getLogger(EASStateReceived.class);

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    private EASStateReceived()
    {
        // Intentionally left empty
    }

    // Instance methods

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getState()
     */
    public int getState()
    {
        return EASManager.EAS_MESSAGE_RECEIVED_STATE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getStateString()
     */
    public String getStateString()
    {
        return "EASStateReceived";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#isAlertInProgress()
     */
    public boolean isAlertInProgress()
    {
        return true;
    }

    /**
     * Receives a new emergency alert message into the current state, determines
     * if it's a text-only, text+audio, or details channel alert, and then
     * initiates processing of that alert using the appropriate strategy.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    public void receiveAlert(final EASMessage message)
    {
        if (log.isInfoEnabled())
        {
            log.info(message.formatLogMessage("Receiving message..."));
        }

        // Release any unneeded resources when transitioning from
        // EASStateInProgress only.
        if (null != EASState.getAlertStrategy())
        {
            EASState.purgeAlertStrategies();
            // TODO: this works for text-only alerts but not necessarily the
            // other strategies. The current thinking is to process
            // the incoming message into a pending alert strategy local to this
            // method, then if EASState.s_currentAlertStrategy
            // is not null, call recycleResources(pendingStrategy) to release or
            // reuse the resources of the current strategy.
            // Then the pending strategy would be saved as the current strategy.
            // If resources are successfully recycled, then the
            // areResourcesRequired() must return false to avoid warning the
            // listeners and re-acquiring the resources yet again.

            // TODO: may also need to update EASHandler after stopping the old
            // alert presentation and __before__ determining the
            // strategy for the new message (might be able to do it
            // unconditionally on entry).
        }

        // Determine alert strategies for the incoming message. Last item pushed
        // has highest precedence of all strategies.
        EASState.pushAlertStrategy(new EASAlertEmpty(this, message));

        if (message.isAlertTextAvailable())
        {
            EASState.pushAlertStrategy(super.m_easAlertTextFactory.createTextOnly(this, message));
        }

        if (message.isDetailsChannelAvailable())
        {
            EASState.pushAlertStrategy(EASAlertDetailsChannel.create(this, message));
        }

        if (message.isAudioChannelAvailable())
        {
            EASState.pushAlertStrategy(super.m_easAlertTextFactory.createTextAudio(this, message));
        }

        // Use systemContext.runInContext(new Runnable() since some some
        // resources are acquired asynchronously.
        super.m_callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                EASState.getAlertStrategy().processAlert();
            }
        });
    }

    /**
     * Registers an {@link EASHandler} instance. At most, only one instance can
     * be registered. Multiple calls of this method replace the previous
     * instance by a new one. By default, no instance is registered.
     * <p>
     * We're just starting to process an alert so just set the pending handler
     * to allow the current handler to be notified to stop playing audio. The
     * issue is that notifying the {@link EASHandler} of a private descriptor
     * may cause it to start playing the audio track so we need to be able to
     * call {@link EASHandler#stopAudio()} on <em>that handler</em> to halt the
     * audio presentation.
     */
    public void registerEASHandler(final EASHandler handler)
    {
        if (null == handler)
        {
            throw new IllegalArgumentException("EASHander reference must not be null");
        }

        synchronized (EASState.s_handlerMutexLock)
        {
            EASState.s_pendingHandler = new EASHandlerContext(handler, super.m_callerContextManager.getCurrentContext());
        }
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        EASAlert strategy = EASState.getAlertStrategy();
        StringBuffer buf = new StringBuffer("EASStateReceived: message=[");
        buf.append((null != strategy) ? strategy.getMessage().toString() : "none");
        buf.append(']');
        return buf.toString();
    }

    /**
     * Unregisters the current registered {@link EASHandler} instance. If no
     * EASHandler instance has registered, do nothing.
     * <p>
     * We're just starting to process an alert so just clear the pending handler
     * to allow the current handler to be notified to stop playing audio. The
     * issue is that notifying the {@link EASHandler} of a private descriptor
     * may cause it to start playing the audio track so we need to be able to
     * call {@link EASHandler#stopAudio()} on <em>that handler</em> to halt the
     * audio presentation.
     */
    public void unregisterEASHandler()
    {
        synchronized (EASState.s_handlerMutexLock)
        {
            EASState.s_pendingHandler = null;
        }
    }
}
