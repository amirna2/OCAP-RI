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

package org.cablelabs.impl.ocap;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.manager.pod.PodImpl;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.ocap.hardware.pod.POD;
import org.cablelabs.impl.util.SimpleCondition;

public class TimeZoneInitializer implements PODListener
{
    private int m_timeZoneOffsetMinutes = -1;

    private int m_dstFlag = -1;

    // Added for findbugs issues fix - start
    private final SimpleCondition m_timeCondition = new SimpleCondition(false);
    // Added for findbugs issues fix - end

    // Added for findbugs issues fix - end

    // Added for findbugs issues fix - end

    private static final Logger log = Logger.getLogger(TimeZoneInitializer.class.getName());

    PODManager m_podman;

    public void initTimeZone()
    {
        if (log.isDebugEnabled())
        {
            log.debug("initTimeZone");
        }

            // register to receive POD events
            m_podman = (PODManager) ManagerManager.getInstance(PODManager.class);
            m_podman.addPODListener(this);

            boolean podReady = m_podman.isPODReady();
        if (log.isDebugEnabled())
        {
            log.debug("podReady = " + podReady);
        }

            // wait for POD to be in ready state (check explicitly -- if not yet
            // ready, wait for ready event)
            if (podReady)
            {
                updateTimeZoneAndDST();
            }
            else
            {
            // wait for pod ready
            if (log.isInfoEnabled())
            {
                log.info("initTimeZone - waiting for POD ready state");
            }
            try
            {
                // Added for findbugs issues fix - start
                m_timeCondition.waitUntilTrue();
                // Added for findbugs issues fix - end
                if (log.isInfoEnabled())
                {
                    log.info("initTimeZone - POD ready");
                }
            }
            catch (InterruptedException ex)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("InterruptedException received waiting for POD ready");
                }
            }

            // no need to update time zone here -- that is done in POD event handler
            // after the POD_ready event is received
        }
    }

    public void notify(PODEvent event)
    {
        int eventId = event.getEvent();

        if (log.isDebugEnabled())
        {
            log.debug("PODEvent received: 0X" + Integer.toHexString(eventId));
        }

        if (eventId == PODEvent.EventID.POD_EVENT_POD_READY)
        {
            updateTimeZoneAndDST();

            // in case we are waiting during initialization for the POD to be
            // ready, signal that it is ready
            // Added for findbugs issues fix - start
            m_timeCondition.setTrue();
            // Added for findbugs issues fix - end
        }
        else if (eventId == PODEvent.EventID.POD_EVENT_APP_INFO_UPDATE)
        {
            // the event doesn't tell us which app info has been updated, so
            // just
            // redo default time zone and DST
            updateTimeZoneAndDST();
        }
    }

    private synchronized void updateTimeZoneAndDST()
    {
        SimpleTimeZone myTimeZone = null;

        POD pod = m_podman.getPOD();
        byte[] timeZoneOffsetTemp = pod.getHostParam(PodImpl.GF_TIME_ZONE);
        if (timeZoneOffsetTemp == null || timeZoneOffsetTemp.length != 2)
        {
            return;
        }

        m_timeZoneOffsetMinutes = 0;
        m_timeZoneOffsetMinutes = ((int) timeZoneOffsetTemp[0]) & 0x000000FF;
        m_timeZoneOffsetMinutes = m_timeZoneOffsetMinutes << 8;

        int temp = ((int) timeZoneOffsetTemp[1]) & 0x000000FF;

        m_timeZoneOffsetMinutes |= temp;

        // check if offset is negative
        if ((m_timeZoneOffsetMinutes & 0x00008000) != 0)
        {
            m_timeZoneOffsetMinutes |= 0xFFFF0000;
        }


        if (log.isDebugEnabled())
        {
            log.debug("m_timeZoneOffsetMinutes = " + m_timeZoneOffsetMinutes);
        }

        String timeZoneID = getTimeZoneID(m_timeZoneOffsetMinutes);
        if (log.isDebugEnabled())
        {
            log.debug("timeZoneID = " + timeZoneID);
        }

        byte[] dstTemp = pod.getHostParam(PodImpl.GF_DAYLIGHT_SAVINGS);
        if (dstTemp == null || dstTemp.length == 0)
        {
            // no dst, so just do time zone offset
            myTimeZone = new SimpleTimeZone((short) m_timeZoneOffsetMinutes, timeZoneID);
            TimeZone.setDefault(myTimeZone);

            return;
        }

        m_dstFlag = dstTemp[0];
        if (log.isDebugEnabled())
        {
            log.debug("m_dstFlag = " + m_dstFlag);
        }

        if (m_dstFlag == 2)
        {
            if (dstTemp.length == 10)
            {
                // M-card

                // daylight_savings_delta 8 uimsbf: Daylight savings delta time
                // in number of minutes
                // daylight_savings_entry_time 32 uimsbf: Daylight savings entry
                // time given as time lapsed since 12 AM Jan 6, 1980, in units
                // of GPS seconds
                // daylight_savings_exit_time 32 uimsbf: Daylight savings exit
                // time given as time lapsed since 12 AM Jan 6, 1980, in units
                // of GPS seconds.

                int dstDeltaMinutes = ((int) dstTemp[1]) & 0x000000FF;

                int dstEntryTime = 0;
                temp = ((int) dstTemp[2]) & 0x000000FF;
                dstEntryTime = (temp << 24);
                temp = ((int) dstTemp[3]) & 0x000000FF;
                dstEntryTime |= (temp << 16);
                temp = ((int) dstTemp[4]) & 0x000000FF;
                dstEntryTime |= (temp << 8);
                temp = ((int) dstTemp[5]) & 0x000000FF;
                dstEntryTime |= (temp);

                int dstExitTime = 0;
                temp = ((int) dstTemp[6]) & 0x000000FF;
                dstExitTime = (temp << 24);
                temp = ((int) dstTemp[7]) & 0x000000FF;
                dstExitTime |= (temp << 16);
                temp = ((int) dstTemp[8]) & 0x000000FF;
                dstExitTime |= (temp << 8);
                temp = ((int) dstTemp[9]) & 0x000000FF;
                dstExitTime |= (temp);

                if (log.isDebugEnabled())
                {
                    log.debug("M-card DST param detected ");
                }
                if (log.isDebugEnabled())
                {
                    log.debug("dstDeltaMinutes = " + dstDeltaMinutes);
                }
                if (log.isDebugEnabled())
                {
                    log.debug("dstEntryTime = " + dstEntryTime);
                }
                if (log.isDebugEnabled())
                {
                    log.debug("dstExitTime = " + dstExitTime);
                }

                GregorianCalendar gpsZeroCal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                gpsZeroCal.clear();
                gpsZeroCal.set(1980 /* year */, 0 /* month */, 6 /* date */, 0 /* hourOfDay */, 0 /* minute */, 0 /* second */);
                long gpsZeroMillis = gpsZeroCal.getTimeInMillis();

                long dstEntryTimeMillis = gpsZeroMillis + ((long) dstEntryTime) * 1000 - 15 * 1000 /*
                                                                                                    * subtract
                                                                                                    * current
                                                                                                    * leap
                                                                                                    * seconds
                                                                                                    */;
                long dstExitTimeMillis = gpsZeroMillis + ((long) dstExitTime) * 1000 - 15 * 1000 /*
                                                                                                  * subtract
                                                                                                  * current
                                                                                                  * leap
                                                                                                  * seconds
                                                                                                  */;

                GregorianCalendar dstEntryTimeCal = new GregorianCalendar();
                dstEntryTimeCal.setTimeInMillis(dstEntryTimeMillis);

                GregorianCalendar dstExitTimeCal = new GregorianCalendar();
                dstExitTimeCal.setTimeInMillis(dstExitTimeMillis);

                try
                {
                    myTimeZone = new SimpleTimeZone((short) m_timeZoneOffsetMinutes * 60000, timeZoneID,

                    dstEntryTimeCal.get(Calendar.MONTH) /* startMonth */,
                            dstEntryTimeCal.get(Calendar.DAY_OF_MONTH) /* startDay */, 0 /* startDayOfWeek */,
                            dstEntryTimeCal.get(Calendar.HOUR_OF_DAY) * 3600000 + dstEntryTimeCal.get(Calendar.MINUTE)
                                    * 60000 + dstEntryTimeCal.get(Calendar.SECOND) * 1000 /* startTime */,
                            SimpleTimeZone.UTC_TIME,

                            dstExitTimeCal.get(Calendar.MONTH) /* endMonth */,
                            dstExitTimeCal.get(Calendar.DAY_OF_MONTH) /* endDay */, 0 /* endDayOfWeek */,
                            dstExitTimeCal.get(Calendar.HOUR_OF_DAY) * 3600000 + dstExitTimeCal.get(Calendar.MINUTE)
                                    * 60000 + dstExitTimeCal.get(Calendar.SECOND) * 1000 /* endTime */,
                            SimpleTimeZone.UTC_TIME,
                            dstDeltaMinutes * 60 * 1000 /* dstSavings */
                            
                    );
          
                }
                catch (Exception ex)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Error creating time zone: " + ex.getMessage());
                    }
            }
            }
            else if (dstTemp.length == 1)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("S-card DST param detected ");
                }

                myTimeZone = new SimpleTimeZone((short) m_timeZoneOffsetMinutes * 60000, timeZoneID,
                        Calendar.MARCH /* startMonth */, 8 /* startDay */, -Calendar.SUNDAY /* startDayOfWeek */,
                        7200000 /* startTime = 2AM */, Calendar.NOVEMBER /* endMonth */, 1 /* endDay */,
                        -Calendar.SUNDAY /* endDayOfWeek */, 7200000 /*
                                                                      * endTime
                                                                      * = 2AM
                                                                      */, 3600000 /* dstSavings */);
            }
            else
            {
                // error here -- unexpected param length
                if (log.isErrorEnabled())
                {
                    log.error("Error creating time zone: unexpected param length (" + dstTemp.length + ")");
                }
        }
        }
        else
        {
            // no dst, so just do time zone offset
            myTimeZone = new SimpleTimeZone((short) m_timeZoneOffsetMinutes, timeZoneID);
        }

        if (myTimeZone != null)
        {
            TimeZone.setDefault(myTimeZone);
        }
    }

    private String getTimeZoneID(int timeZoneOffsetMinutes)
    {
        String timeZoneID;

        int gmtOffsetHours = m_timeZoneOffsetMinutes / 60;
        int gmtOffsetMins = m_timeZoneOffsetMinutes % 60;

        if (timeZoneOffsetMinutes >= 0)
        {
            timeZoneID = "GMT+";
        }
        else
        {
            timeZoneID = "GMT-";

            gmtOffsetHours = -gmtOffsetHours;
            gmtOffsetMins = -gmtOffsetMins;
        }
            
        String temp = Integer.toString(gmtOffsetHours);
        if (temp.length() == 1)
        {
            temp = "0" + temp;
        }
        timeZoneID += temp + ":";

        temp = Integer.toString(gmtOffsetMins);
        if (temp.length() == 1)
        {
            temp = "0" + temp;
        }
        timeZoneID += temp;

        return timeZoneID;
    }
}
