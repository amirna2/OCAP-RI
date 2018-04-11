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

package org.cablelabs.impl.ocap.hn.upnp.srs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * This is the container for all of the SRS created RecordSchedule instances.
 * 
 * @author Dan Woodard
 * 
 */
public class RecordSchedules
{
    private static final Logger log = Logger.getLogger(RecordSchedules.class);

    /**
     * package private use only constructor
     */
    RecordSchedules()
    {
    }

    /**
     * adds a RecordSchedule to the list
     * 
     * @param recordSchedule
     *            The RecordSchedule to add
     */
    synchronized void add(RecordSchedule recordSchedule)
    {
        if (list.contains(recordSchedule))
        {
            return; // only add once
        }

        list.add(recordSchedule);
    }

    /**
     * removes a RecordSchedule to the list
     * 
     * @param recordSchedule
     *            The RecordSchedule to remove
     */
    synchronized void remove(RecordSchedule recordSchedule)
    {
        list.remove(recordSchedule);
    }

    /**
     * Check for containment
     * 
     * @param recordSchedule
     *            The RecordSchedule to check for
     * @return true if in the list, false if not
     * 
     *         synchronized boolean contains( RecordSchedule recordSchedule ) {
     *         return list.contains(recordSchedule); }
     */

    synchronized RecordSchedule getRecordSchedule(String objectID)
    {
        Iterator iterator = list.iterator();

        while (iterator.hasNext())
        {
            RecordSchedule recordSchedule = (RecordSchedule) iterator.next();
            if (recordSchedule.getObjectID().equals(objectID))
            {
                return recordSchedule;
            }
        }

        return null;
    }

    /**
     * Returns a list of all records schedules
     * 
     * @return  list of all record schedules
     */
    synchronized ArrayList getRecordSchedules()
    {
        ArrayList rsList = new ArrayList();
        Iterator iterator = list.iterator();

        while (iterator.hasNext())
        {
            RecordSchedule recordSchedule = (RecordSchedule) iterator.next();
            rsList.add(recordSchedule);
        }

        if (log.isDebugEnabled())
        {
            log.debug("getRecordSchedules: returning list cnt: " + rsList.size());
        }
        return rsList;
    }

    synchronized RecordTask getRecordTask(String objectID)
    {
        if (objectID == null || objectID.equals(""))
        {
            return null;
        }

        RecordTask recordTask = null;

        Iterator iterator = list.iterator();

        while (iterator.hasNext())
        {
            RecordSchedule recordSchedule = (RecordSchedule) iterator.next();
            recordTask = recordSchedule.getRecordTask(objectID);
            if (recordTask != null)
            {
                break;
            }
        }

        return recordTask;
    }

    /**
     * Returns the RecordSchedule that has been stored in the CDS that has the
     * ObjectID of the input parameter.
     */
    synchronized RecordSchedule getRecordScheduleByCDSID(String objectID)
    {
        RecordSchedule recordSchedule = null;

        Iterator iterator = list.iterator();

        while (iterator.hasNext())
        {
            RecordSchedule rs = (RecordSchedule) iterator.next();

            NetRecordingEntryLocal netRecordingEntry = rs.getNetRecordingEntry();

            if (netRecordingEntry != null)
            {
                String id = netRecordingEntry.getScheduledCDSEntryID();

                if (id.equals(objectID))
                {
                    recordSchedule = rs;
                    break;
                }
            }
        }

        return recordSchedule;
    }

    /**
     * Returns a list of record tasks.  If a record schedule ID was supplied,
     * returns only the tasks associated with that record schedule.  Otherwise,
     * it returns all known record tasks.
     * 
     * @param rsID      id of specific record schedule, null if to return all record tasks
     * @param rtList    list of RecordTasks
     */
    synchronized void getRecordTasks(String rsID, ArrayList rtList)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getRecordTasks() - rsID: " + rsID + ", list cnt: " + rtList.size());
        }

        Iterator iterator = list.iterator();

        if (rsID.equals(""))
        {
            while (iterator.hasNext())
            {
                RecordSchedule recordSchedule = (RecordSchedule)iterator.next();
                recordSchedule.getRecordTasks(rtList);
            }            
        }
        // If a record schedule ID was supplied, just get the tasks associated with that schedule
        else if (rsID != null)
        {
            // Get the tasks associated with this record schedule
            RecordSchedule rs = getRecordSchedule(rsID);
            if (rs != null)
            {
                rs.getRecordTasks(rtList);
            }
        }
     }

    /**
     * Returns the RecordTask that has been stored in the CDS that has the
     * ObjectID of the input parameter.
     */
    synchronized RecordTask getRecordTaskByCDSID(String objectID)
    {
        RecordTask recordTask = null;

        Iterator iterator = list.iterator();

        while (iterator.hasNext())
        {
            RecordSchedule recordSchedule = (RecordSchedule) iterator.next();

            recordTask = recordSchedule.getRecordTaskByCDSID(objectID);

            if (recordTask != null)
            {
                break;
            }
        }

        return recordTask;
    }

    private LinkedList list = new LinkedList();

}
