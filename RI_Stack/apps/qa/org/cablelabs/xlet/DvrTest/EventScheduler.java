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

/*
 * Created on Jan 27, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.cablelabs.xlet.DvrTest;

import java.util.*;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffListener;
import javax.tv.util.TVTimerWentOffEvent;

/**
 * @author Fred Smith
 * 
 *         TVTimer based implementation of event scheduler
 */
public class EventScheduler
{

    public EventScheduler()
    {
        m_commandList = new Vector();
    }

    public void scheduleCommand(NotifyShell shell)
    {
        synchronized (m_commandList)
        {
            m_commandList.addElement((Object) shell);
        }

    }

    /**
     * Run the registered list of commands and terminate after list has
     * completed
     * 
     * @param terminateDelay
     *            length of time to wait after termination
     */
    public void run(long terminateDelay)
    {
        Object sync = new Object();
        long time = 0;
        synchronized (m_commandList)
        {
            for (int x = 0; x < m_commandList.size(); x++)
            {
                NotifyShell ns = (NotifyShell) m_commandList.elementAt(x);
                if (ns.getScheduledTime() > time) time = ns.getScheduledTime();
            }
        }

        System.out.println("EventScheduler: running with delay: expected time:" + (time + terminateDelay) / 1000 + " seconds.");
        this.scheduleCommand(new TaskExit(sync, time + terminateDelay));

        run();

        try
        {
            synchronized (sync)
            {
                sync.wait();
            }
        }
        catch (Exception e)
        {
            System.out.println("EventScheduler sync.wait exception");
            e.printStackTrace();
        }

    }

    public void cancel()
    {
        synchronized (m_commandList)
        {
            for (int x = 0; x < m_commandList.size(); x++)
            {
                NotifyShell ns = (NotifyShell) m_commandList.elementAt(x);
                try
                {
                    ns.deschedule();
                }
                catch (Exception e)
                {
                    // TODO: Handle schedule failed
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public void run()
    {
        long time = System.currentTimeMillis();
        synchronized (m_commandList)
        {
            for (int x = 0; x < m_commandList.size(); x++)
            {
                NotifyShell ns = (NotifyShell) m_commandList.elementAt(x);

                try
                {
                    ns.schedule(ns.getScheduledTime() + time);
                }
                catch (Exception e)
                {
                    // TODO: Handle schedule failed
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public void reset()
    {
        m_commandList = new Vector();
    }

    public static abstract class NotifyShell implements TVTimerWentOffListener
    {
        TVTimerSpec retSpec = null;

        public NotifyShell(long time)
        {
            retSpec = new TVTimerSpec();
            retSpec.addTVTimerWentOffListener(this);
            m_scheduledTime = time;
        }

        public void ProcessCommand()
        {
        }

        public void schedule(long time)
        {
            retSpec.setAbsoluteTime(time);
            try
            {
                retSpec = m_timer.scheduleTimerSpec(retSpec);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
        }

        public void deschedule()
        {
            m_timer.deschedule(retSpec);
        }

        public void timerWentOff(TVTimerWentOffEvent e)
        {
            ProcessCommand();
            retSpec.removeTVTimerWentOffListener(this);
        }

        public long getScheduledTime()
        {
            return m_scheduledTime;
        }

        private long m_scheduledTime;
    }

    public static class TaskExit extends EventScheduler.NotifyShell
    {
        public TaskExit(Object syncObject, long time)
        {
            super(time);
            m_syncObject = syncObject;
        }

        public void ProcessCommand()
        {
            // System.out.println(" TaskExit: calling m_object.notify()");
            synchronized (m_syncObject)
            {
                m_syncObject.notify();
            }

        }

        private Object m_syncObject;
    }

    private static final TVTimer m_timer = TVTimer.getTimer();

    private Vector m_commandList;

}
