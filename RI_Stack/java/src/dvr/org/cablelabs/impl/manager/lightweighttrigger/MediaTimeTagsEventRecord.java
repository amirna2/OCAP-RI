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

import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.player.AlarmClock;
import org.cablelabs.impl.media.player.FixedAlarmSpec;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamEventInterface;
import org.cablelabs.impl.dvb.dsmcc.EventRecord;
import org.cablelabs.impl.media.player.AbstractPlayer;
import org.cablelabs.impl.media.player.AlarmClock.Alarm;
import org.cablelabs.impl.media.player.AlarmClock.AlarmException;
import org.cablelabs.impl.media.player.AlarmClock.Alarm.Callback;
import org.cablelabs.impl.util.SystemEventUtil;

public class MediaTimeTagsEventRecord extends EventRecord implements Callback
{
    private Vector m_players;

    private Hashtable m_playerAlarms = new Hashtable();

    private byte[] m_data;

    private AlarmClock.AlarmSpec m_forwardAlarmSpec;

    private AlarmClock.AlarmSpec m_reverseAlarmSpec;

    private boolean m_filtering = false;

    private static final Logger log = Logger.getLogger(MediaTimeTagsEventRecord.class.getName());

    MediaTimeTagsEventRecord(LightweightTriggerEvent ev, DSMCCStreamEventInterface parent, Vector players)
    {
        super(ev.id, ev.eventName, parent);

        m_scheduledTime = ev.getTimeNanos();
        m_data = ev.data;
        m_players = players;

        m_forwardAlarmSpec = new FixedAlarmSpec(m_name, m_scheduledTime, AlarmClock.AlarmSpec.Direction.FORWARD);
        m_reverseAlarmSpec = new FixedAlarmSpec(m_name, m_scheduledTime, AlarmClock.AlarmSpec.Direction.REVERSE);
    }

    public boolean isScheduled()
    {
        // MediaTimeTags event are always "DoItNow", at least as far as an NPT
        // is concerned.
        return false;
    }

    protected boolean isValid()
    {
        // MediaTimeTags events are always valid.
        return true;
    }

    protected void addFilter() throws InsufficientResourcesException, MPEGDeliveryException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Initializing alarms on event " + m_name + " num players: " + m_players.size());
        }
        if (!m_filtering)
        {
            m_filtering = true;
            int numPlayers = m_players.size();
            for (int i = 0; i < numPlayers; i++)
            {
                try
                {
                    addPlayer((AbstractPlayer) m_players.elementAt(i));
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError(e);
                    if (log.isErrorEnabled())
                    {
                        log.error("Initializing alarms had exception", e);
                    }
                }
            }
        }
    }

    protected void removeFilter()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Stopping alarms on event " + m_name);
        }
        m_filtering = false;
        int numPlayers = m_players.size();
        for (int i = 0; i < numPlayers; i++)
        {
            removePlayer((AbstractPlayer) m_players.elementAt(i));
        }
    }

    void addPlayer(AbstractPlayer player) throws AlarmException
    {
        if (m_filtering)
        {
            if (m_playerAlarms.get(player) == null)
            {
            // Added code for caching for findbugs issues fix
                long l_scheduledTime;
                synchronized(this)
                {
                    l_scheduledTime = m_scheduledTime;
                }
                if (log.isDebugEnabled())
                {
                    log.debug("Creating alarms for event " + m_name + " on player " + player + " at " + l_scheduledTime
                            + " (ns).  " + "Current player mediatime (ns) is " + player.getMediaNanoseconds()
                            + ". Delta is " + (l_scheduledTime - player.getMediaNanoseconds()));
                }
                Alarms a = new Alarms();
                a.forward = player.createAlarm(m_forwardAlarmSpec, this);
                a.reverse = player.createAlarm(m_reverseAlarmSpec, this);
                a.forward.activate();
                a.reverse.activate();
                m_playerAlarms.put(player, a);
            }
        }
    }

    void removePlayer(AbstractPlayer player)
    {
        Alarms a = (Alarms) m_playerAlarms.remove(player);
        if (a != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Removing alarms for event " + m_name + " on player " + player);
            }
            a.forward.deactivate();
            a.reverse.deactivate();
            player.destroyAlarm(a.forward);
            player.destroyAlarm(a.reverse);
        }
    }

    void setPlayers(Vector players)
    {
        boolean filteringAlready = m_filtering;
        removeFilter();
        m_players = players;
        if (filteringAlready)
        {
            try
            {
                addFilter();
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Caught exception resetting players in event " + m_name, e);
                }
                // Can't really be thrown, so ignore.
            }
        }
    }

    public void destroyed(Alarm alarm, AlarmException reason)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Alarm destroyed called for event " + m_name);
        }
    }

    public void fired(Alarm alarm)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Alarm fired for event " + m_name + ".  Signalling event");
        }
        signalEvent(-1, m_data);
    }

    private class Alarms
    {
        Alarm forward;

        Alarm reverse;
    }
}
