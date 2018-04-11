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
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.ocap.system.EASEvent;
import org.ocap.system.EASHandler;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;


/**
 * A concrete implementation of {@link EASState} that provides the message
 * processing actions for an EAS message in the "not-in-progress" state.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASStateNotInProgress extends EASState
{
    // Class Constants

    static final EASState INSTANCE = new EASStateNotInProgress();

    private static final Logger log = Logger.getLogger(EASStateNotInProgress.class);

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    private EASStateNotInProgress()
    {
        // intentionally left empty
    }

    // Instance Methods

    /**
     * Completes the current alert by releasing any resources used by alert
     * message processing and notifying all registered {@link EASListener}
     * implementations that EAS processing is complete. The notifications occur
     * whether the alert was successfully presented or not.
     * 
     * @see EASStateNotInProgress#completeAlert()
     */
    public void completeAlert()
    {
        synchronized (EASState.s_strategyMutexLock)
        {
            if (log.isInfoEnabled())
            {
                log.info(EASState.getAlertStrategy().getMessage().formatLogMessage("Completing message..."));
            }
            notifyEASListeners(EASEvent.EAS_COMPLETE);
            EASState.purgeAlertStrategies();
        }

        updateEASHandler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getState()
     */
    public int getState()
    {
        return EASManager.EAS_NOT_IN_PROGRESS_STATE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getStateString()
     */
    public String getStateString()
    {
        return "EASStateNotInProgress";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#isAlertInProgress()
     */
    public boolean isAlertInProgress()
    {
        return false;
    }

    /**
     * Receives a new emergency alert message into the current state, checks for
     * a still "active" alert, and transitions to the {@link EASStateReceived}
     * state if no matching alert is found.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @see EASStateReceived#receiveAlert(EASMessage)
     */
    public void receiveAlert(final EASMessage message)
    {
        if (log.isInfoEnabled())
        {
            log.info(message.formatLogMessage("Receiving message..."));
        }

        if (super.matchesActiveAlert(message))
        {
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(message, "Previously processed alert still active - ignoring"));
            }
        }
        else
        {
            addActiveAlert(message);
            EASState.s_easManagerContext.changeState(EASStateReceived.INSTANCE);
            EASState.s_easManagerContext.getCurrentState().receiveAlert(message);
        }
    }

    /**
     * Registers an {@link EASHandler} instance. At most, only one instance can
     * be registered. Multiple calls of this method replace the previous
     * instance by a new one. By default, no instance is registered.
     * <p>
     * Since there's no alert in progress, it's safe to unconditionally replace
     * the handler and set the pending handler to the same value.
     * 
     * @param handler
     *            the handler to register
     * @throws IllegalArgumentException
     *             if null is specified
     */
    public void registerEASHandler(final EASHandler handler)
    {
        if (null == handler)
        {
            throw new IllegalArgumentException("EASHander reference must not be null");
        }

        synchronized (EASState.s_handlerMutexLock)
        {
            unregisterEASHandler();
            EASState.s_currentHandler = new EASHandlerContext(handler, super.m_callerContextManager.getCurrentContext());
            EASState.s_pendingHandler = EASState.s_currentHandler;
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
        StringBuffer buf = new StringBuffer("EASStateNotInProgress: message=[");
        buf.append((null != strategy) ? strategy.getMessage().toString() : "none");
        buf.append(']');
        return buf.toString();
    }

    /**
     * Unregisters the current registered {@link EASHandler} instance. If no
     * EASHandler instance has registered, do nothing.
     * <p>
     * Since there's no alert in progress, it's safe to unconditionally remove
     * the handler and set the pending handler to the same value.
     */
    public void unregisterEASHandler()
    {
        synchronized (EASState.s_handlerMutexLock)
        {
            if (null != EASState.s_currentHandler)
            {
                EASState.s_currentHandler.dispose();
            }

            EASState.s_currentHandler = null;
            EASState.s_pendingHandler = null;
        }
    }

    /**
     * Replaces the currently {@link EASHandler} with the pending handler if the
     * handlers are different. The current and pending handlers are different if
     * a host application registers as a handler while an alert is in-progress.
     * Registration of the new handler is delayed until after
     * {@link EASHandler#stopAudio()} can be invoked on the current handler at
     * the end of the alert.
     */
    private void updateEASHandler()
    {
        synchronized (EASState.s_handlerMutexLock)
        {
            if (EASState.s_currentHandler != EASState.s_pendingHandler)
            {
                if (null != EASState.s_currentHandler)
                {
                    EASState.s_currentHandler.dispose();
                }

                EASState.s_currentHandler = EASState.s_pendingHandler;
            }
        }
    }
}
