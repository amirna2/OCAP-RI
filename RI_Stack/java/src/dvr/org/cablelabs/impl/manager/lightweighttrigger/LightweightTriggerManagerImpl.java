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
package org.cablelabs.impl.manager.lightweighttrigger;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowListener;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindowMonitorListener;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.ocap.dvr.event.LightweightTriggerHandler;
import org.ocap.dvr.event.LightweightTriggerManager;
import org.ocap.dvr.event.LightweightTriggerSession;
import org.ocap.net.OcapLocator;

public class LightweightTriggerManagerImpl extends LightweightTriggerManager implements TimeShiftWindowListener
{
    private static LightweightTriggerManager m_instance = null;

    private StreamTypeHandlerList m_handlers = null;

    private Vector m_monitors = new Vector();

    private static final short STREAM_RANGE_MINIMUM = 0;

    private static final short STREAM_RANGE_MAXIMUM = 0xff;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(LightweightTriggerManagerImpl.class.getName());

    private LightweightTriggerManagerImpl()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating new LightweightTriggerManagerImpl");
        }

        m_handlers = StreamTypeHandlerList.getInstance();
        // Note: monitors are not registered at this point
    }

    /**
     * Get the singleton LightweightTriggerManager. Create it if it doesn't
     * exist.
     * 
     * @return The singleton.
     */
    public static synchronized LightweightTriggerManager getInstance()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getInstance");
        }

        if (m_instance == null)
        {
            LightweightTriggerManagerImpl lwtmi = new LightweightTriggerManagerImpl();
            m_instance = lwtmi;
            lwtmi.registerTimeShiftWindowMonitors();
        }
        return m_instance;
    }

    protected void registerTimeShiftWindowMonitors()
    {
        synchronized (m_monitors)
        {
            TimeShiftManager tswm = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);
            tswm.setNewTimeShiftWindowListener(this);

            Vector windows = tswm.getAllTSWs();

            for (int i = 0; i < windows.size(); i++)
            {
                TimeShiftWindow tsw = null;
                try
                {
                    tsw = (TimeShiftWindow) windows.elementAt(i);
                    if (tsw.getState() != TimeShiftManager.TSWSTATE_IDLE)
                    {
                        ProgramMonitor pm = new ProgramMonitor(tsw);
                        tsw.setMonitor(pm);
                        m_monitors.add(pm);
                    }
                }
                catch (Exception e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Not able to create/add ProgramMonitor for " + tsw, e);
                    }
                }
            }
        }
    }

    /**
     * Register a new handler. If this is the first handler registered, fire up
     * the entire LightweightTrigger mechanism.
     */
    public void registerHandler(LightweightTriggerHandler handler, short streamType)
    {
        if (log.isDebugEnabled())
        {
            log.debug("registerHandler: " + streamType);
        }

        if (handler == null)
        {
            throw new NullPointerException("Handler is Null");
        }
        // Check arguments and security.
        if (streamType < STREAM_RANGE_MINIMUM || streamType > STREAM_RANGE_MAXIMUM)
        {
            throw new IllegalArgumentException("Invalid Stream Type: " + streamType);
        }
        if (!SecurityUtil.isSignedApp())
        {
            throw new SecurityException("Must be signed to register LightweightTriggerHandler.");
        }

        // Create the
        LightweightTriggerCallback cb = null;
        synchronized (m_handlers)
        {
            cb = m_handlers.registerHandler(handler, streamType);
            if (cb == null)
            {
                SystemEventUtil.logRecoverableError("registerHandler failed to register handler",
                        new IllegalStateException("unable to register handler"));
                if (log.isErrorEnabled())
                {
                    log.error("registerHandler failed to register handler");
                }
            }
        }
        // Notify all the monitors that a new handler has been created.
        synchronized (m_monitors)
        {
            Enumeration x = m_monitors.elements();
            while (x.hasMoreElements())
            {
                ProgramMonitor monitor = (ProgramMonitor) x.nextElement();
                monitor.addCallback(cb, streamType);
            }
        }
    }

    /**
     * Remove a handler. If this is the last handler registered, shutdown the
     * mechanism.
     */
    public void unregisterHandler(LightweightTriggerHandler handler) throws IllegalArgumentException
    {
        if (log.isDebugEnabled())
        {
            log.debug("unregisterHandler");
        }
        if (handler == null)
        {
            throw new IllegalArgumentException("Null handler.  Not registered");
        }
        synchronized (m_handlers)
        {
            m_handlers.unregisterHandler(handler);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftWindowListener#timeShiftWindowCreated
     * (org.cablelabs.impl.manager.timeshift.TimeShiftWindow)
     */
    public TimeShiftWindowMonitorListener timeShiftWindowCreated(TimeShiftWindow tsw)
    {
        if (log.isDebugEnabled())
        {
            log.debug("TimeShiftWindowCreated: " + tsw);
        }
        ProgramMonitor pm = null;

        synchronized (m_monitors)
        {
            try
            {
                pm = new ProgramMonitor(tsw);
                m_monitors.add(pm);
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Not able to create/add ProgramMonitor for " + tsw, e);
                }
            }
        }

        return pm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftWindowListener#timeShiftWindowDestroyed
     * (org.cablelabs.impl.manager.timeshift.TimeShiftWindowMonitorListener)
     */
    public void timeShiftWindowDestroyed(TimeShiftWindowMonitorListener pm)
    {
        if (log.isDebugEnabled())
        {
            log.debug("TimeShiftWindowDestroyed: " + pm);
        }
    }

    /**
     * Locate a LightweightTriggerSession which corresponds to the given
     * locator.
     * 
     * @param loc
     *            The locator.
     * @return The session which corresponds to this locator, or null.
     */
    public LightweightTriggerSession getSessionByLocator(OcapLocator loc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("searching for " + loc.toExternalForm());
        }
        Enumeration x = m_monitors.elements();
        while (x.hasMoreElements())
        {
            ProgramMonitor pm = (ProgramMonitor) x.nextElement();
            LightweightTriggerSession session = pm.getSessionByLocator(loc);
            if (session != null)
            {
                return session;
            }
        }
        return null;
    }

    public void removePM(ProgramMonitor pm)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removePM: " + pm);
        }
        synchronized (m_monitors)
        {
            m_monitors.remove(pm);
        }
    }
}
