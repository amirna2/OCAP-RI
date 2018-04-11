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
 *	DvrTest.java modified to support DvrTestMonAppXlet.java
 */
package org.cablelabs.xlet.DvrTest;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.EventObject;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.RateChangeEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import org.apache.log4j.*;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceReleasedEvent;
import org.davic.net.tuning.NetworkInterfaceReservedEvent;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppID;
import org.dvb.media.VideoTransformation;
import org.havi.ui.HScene;
import org.havi.ui.HVideoComponent;
import org.ocap.OcapSystem;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.dvr.BufferingRequest;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.resource.ResourceUsage;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListComparator;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;
import org.ocap.shared.media.LeavingLiveModeEvent;
import org.ocap.shared.media.TimeShiftControl;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.dvr.SharedResourceUsage;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageOption;

/**
 * DvrMonAppTest - This is very similar to the DvrTest class; it has been
 * renamed and slightly modified. It is called on by DvrTestMonAppXlet class and
 * provides the same functionality as DvrTest. This was done for WS22b ECR856
 * test cases.
 * 
 * This serves as a DVR schedule-based testing base class. It provides some
 * basic testing functionality, including test status tracking and lookup by
 * name utils
 * 
 * @author fsmith
 * 
 */
public class DvrMonAppTest
{
    public static final int TEST_PASSED = 0;

    public static final int TEST_FAILED = 1;

    public static final int TEST_INTERNAL_ERROR = 2;

    public static final long DISK_MIN = 616038400;

    DvrMonAppTest(Vector locators)
    {
        m_objects = new Vector();
        m_locators = locators;
        m_cond = new SimpleCondition(false);
        m_cond.unSet();
        testingSI = false;
    }

    DvrMonAppTest(Vector v, boolean testingNewSI)
    {
        this(v);
        testingSI = testingNewSI;
    }

    public class TestCase implements Runnable
    {
        public int getResult()
        {
            return m_failed;
        }

        public String getName()
        {
            return null;
        }

        public void waitForCompletion()
        {
            m_cond.get();
            m_cond.unSet();
        }

        public void run()
        {
            clearObjects();
            runTest();
            m_cond.set();
            DvrTestMonAppXlet.log("Test Run complete from Test Case class; result of " + getName() + " is "
                    + getResult());
            DvrTestMonAppXlet.notifyTestComplete(getName(), getResult(), m_failedReason);
        }

        public void runTest()
        {
        };

        public void clearObjects()
        {
            synchronized (m_objects)
            {
                DvrTestMonAppXlet.log("Removing objects from Vector object ");
                m_objects.removeAllElements();
            }
        }

        SimpleCondition m_cond = new SimpleCondition(false);

    }

    /**
     * 
     * ------------------- General Global Classes -------------------------
     * 
     * Methods used by the test cases
     * 
     * -------------------------------------------------------------------
     * 
     */

    /**
     * Eliminates all recordings in the test list and resets all recording
     * events
     * 
     */
    void reset()
    {
        System.out.println("Event Scheduler-reset called \n");
        m_eventScheduler.reset();
        System.out.println("Event Scheduler-reset finished \n");
    }

    /**
     * return the list of tests this class implements
     * 
     * @return a vector of strings naming the test
     */
    public Vector getTests()
    {
        return new Vector();
    }

    /**
     * Simple Map replacement functionality - findObject by key
     * 
     * @param key
     * @return
     */
    Object findObject(String key)
    {
        synchronized (m_objects)
        {
            for (int x = 0; x < m_objects.size(); x++)
            {
                mapEntry me = (mapEntry) m_objects.elementAt(x);
                if (me.key.equals(key))
                {
                    return me.entry;
                }
            }
        }
        return null;
    }

    /**
     * Simple Map replacement functionality - find string by object
     * 
     * @param object
     * @return
     */

    String findKey(Object object)
    {
        synchronized (m_objects)
        {
            for (int x = 0; x < m_objects.size(); x++)
            {
                mapEntry me = (mapEntry) m_objects.elementAt(x);
                // System.out.println("DvrTest::findKey() m_object.elementAt("+x+") - me.entry = "
                // + (me.entry!=null?me.entry:null) +".\n");
                if (me.entry.equals(object))
                {
                    // System.out.println("DvrTest::findKey() found key: " +
                    // me.key + ".\n");
                    return me.key;
                }
            }
        }
        return null;
    }

    /**
     * Simple Map replacement functionality - insert and object by key
     * 
     * @param item
     * @param key
     */
    void insertObject(Object item, String key)
    {
        removeObject(findObject(key));
        synchronized (m_objects)
        {
            mapEntry me = new mapEntry();
            me.entry = item;
            me.key = key;
            m_objects.addElement(me);
        }
    }

    /**
     * Simple Map replacement functionality - remove item from the map
     * 
     * @param item
     */
    void removeObject(Object item)
    {
        synchronized (m_objects)
        {
            for (int x = 0; x < m_objects.size(); x++)
            {
                mapEntry me = (mapEntry) m_objects.elementAt(x);
                if (me.entry == item)
                {
                    m_objects.removeElement(me);
                    return;
                }
            }
        }
    }

    /**
     * Query the RecordingManager for the list of all known recordings and
     * present them to System.out
     * 
     * @returns number of recordings in the master recording list
     */
    public int dumpDvrRecordings()
    {
        try
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList rl = rm.getEntries();

            if (rl == null)
            {
                System.out.println("<<dumpDvrRecordings>> Recording list null!");
                return 0;
            }

            System.out.println("<<<dumpDvrRecordings  Start:+ " + rl.size() + " entries >>>");
            for (int x = 0; x < rl.size(); x++)
            {
                printRecording((RecordingRequest) rl.getRecordingRequest(x));
            }
            System.out.println("<<<dumpDvrRecordings  End: + " + rl.size() + " entries >>>");
            return rl.size();
        }
        catch (Exception e)
        {
            System.out.println("Exception thrown in dumpDvrRecordings");
            m_failed = TEST_FAILED;
            m_failedReason = "Exception thrown in dumpDvrRecordings: " + e.getMessage();
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Clean out the RecordingManager (and the hard drive) by deleting all known
     * recordings.
     */
    public void deleteAllRecordings()
    {
        System.out.println("<<deleteAllRecordings: Start>>");
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

        RecordingList rl = rm.getEntries();

        if (rl == null)
        {
            System.out.println("<deleteAllRecordings: RecordingList = null!");
            return;
        }

        for (int x = 0; x < rl.size(); x++)
        {
            try
            {
                rl.getRecordingRequest(x).delete();
            }
            catch (Exception e)
            {
                System.out.println("<<Exception deleting recording request" + rl.getRecordingRequest(x));
                m_failed = TEST_FAILED;
                m_failedReason = "Exception deleting recording request " + rl.getRecordingRequest(x) + ": "
                        + e.getMessage();
                e.printStackTrace();
            }
        }
        System.out.println("<<deleteAllRecordings: " + rl.size() + " recordings. End>>");
    }

    /**
     * Dump the contents of a given RecordingListEntry
     * 
     * @param rle
     */
    void printRecording(RecordingRequest rr)
    {
        System.out.println("\n\n<<<<<<<<<<<<Recording List Entry <" + rr + "> Start>>>>>>>>>>>>");

        OcapRecordedService service = null;
        RecordingSpec rs = rr.getRecordingSpec();
        OcapRecordingProperties orp = (OcapRecordingProperties) rs.getProperties();
        System.out.println("rr: " + rr);

        if (rs instanceof LocatorRecordingSpec)
        {
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) rs;
            System.out.println("IsRoot    :  " + rr.isRoot());
            System.out.println("HasParent :  " + rr.getParent());
            System.out.println(("Root     :" + rr.getRoot()));
            System.out.println("Start Time:  " + lrs.getStartTime());
            System.out.println("Duration  :  " + lrs.getDuration());
            System.out.println("Expiration:  " + lrs.getProperties().getExpirationPeriod());
        }
        if (rs instanceof ServiceContextRecordingSpec)
        {
            ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec) rs;
            System.out.println("Start Time:" + scrs.getStartTime());
            System.out.println("Duration:" + scrs.getDuration());
            System.out.println("Expiration:" + scrs.getProperties().getExpirationPeriod());
        }
        System.out.println("Priority:" + orp.getPriorityFlag());
        System.out.println("State:" + rr.getState());
        System.out.println("Requested BitRate:" + orp.getBitRate());

        try
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) rr;
            try
            {
                service = (OcapRecordedService) orr.getService();
            }
            catch (Exception e)
            {
                System.out.println("Exception retrieving recorded service" + e);
                // e.printStackTrace();
            }

            if (service != null)
            {
                System.out.println("Recorded Service Locator: " + service.getLocator());
                System.out.println("Service Service Name: " + service.getName());
                System.out.println("Recorded Size: " + service.getRecordedSize());
                System.out.println("Recorded BitRate:" + service.getRecordedBitRate());
                System.out.println("Recorded Duration:" + service.getRecordedDuration());
                System.out.println("Recorded Media Locator:" + service.getMediaLocator().toString());
            }
        }
        catch (Exception e)
        {
            System.out.println("No info - parent recording");
        }

        System.out.println("<<<<<<<<<<<Recording List Entry <" + rr + "> End>>>>>>>>>>>>>\n\n");
    }

    public long diskFreeCheck()
    {
        long size = 0;
        StorageProxy[] sproxy = StorageManager.getInstance().getStorageProxies();

        for (int i = 0; i < sproxy.length; i++)
        {
            LogicalStorageVolume[] lsv = sproxy[i].getVolumes();

            for (int j = 0; j < lsv.length; j++)
            {
                if (lsv[j] instanceof MediaStorageVolume)
                {
                    size += ((MediaStorageVolume) (lsv[j])).getFreeSpace();
                }
            }
        }
        System.out.println("Disk space left is " + size + " bytes.");
        if (size < DISK_MIN)
        {
            m_failed = TEST_INTERNAL_ERROR;
            DvrTestMonAppXlet.log("DvrMonAppTest: Flagged INTERNAL ERROR in diskFreeCheck: Required " + DISK_MIN
                    + " bytes of space. Currently only " + size + " bytes left.");
            m_failedReason = "Flagged INTERNAL ERROR in diskFreeCheck: Required " + DISK_MIN
                    + " bytes of space. Currently only " + size + " bytes left.";
        }
        return size;
    }

    String getNameForResourceUsage(final ResourceUsage ru)
    {
        if (ru instanceof ApplicationResourceUsage)
        {
            System.out.println("<<<<<<<<<Found a Application resource usage>>>>>>>>>>>" + ru);
            String names[] = ((ApplicationResourceUsage) ru).getResourceNames();
            if (names == null)
            {
                System.out.println("ARU getResourcesNames == null");
            }
            else
            {
                System.out.println("ARU getResourcesNames cnt == " + names.length);
            }
            for (int i = 0; i < names.length; i++)
            {
                System.out.println("ARU name[" + i + "] = " + names[i]);
            }

            ResourceProxy rp = ((ApplicationResourceUsage) ru).getResource("org.davic.net.tuning.NetworkInterfaceController");
            if (rp == null)
            {
                System.out.println("ARU returning null proxy");
                Thread.dumpStack();
                return null;
            }
            else
                System.out.println("ARU proxy = " + rp);

            return findKey(rp);
        }

        if (ru instanceof RecordingResourceUsage)
        {
            System.out.println("<<<<<<<<<Found a Recording resource usage>>>>>>>>>>>");
            RecordingRequest rr = ((RecordingResourceUsage) ru).getRecordingRequest();

            if (findKey(rr) == null)
            { // Hack to deal with the fact that a recording won't be added to
              // the
                // DVRTest recording list until record() has returned - which
                // will
                // be after the RCH has been called and returned
                if (m_defaultRecordingName != null)
                {
                    insertObject(rr, m_defaultRecordingName);
                    System.out.print("DvrMonAppTest.getNameForResourceUsage():findKey(rr) == null");
                }
                else
                {
                    System.out.print("DvrMonAppTest.getNameForResourceUsage():defaultRecordingName == null : findKey(rr) == null");
                }
            }
            return findKey(rr);
        }

        if (ru instanceof ServiceContextResourceUsage)
        {
            System.out.println("<<<<<<<<<Found a Service Context resource usage>>>>>>>>>>>");
            ServiceContext sc = ((ServiceContextResourceUsage) ru).getServiceContext();
            return findKey(sc);
        }

        if (ru instanceof TimeShiftBufferResourceUsage)
        {
            System.out.println("<<<<<<<<<Found Time Shift resource usage>>>>>>>");
            BufferingRequest br = null;
            // Get Recording Manager
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            // Get a list of buffer requests
            BufferingRequest[] brs = rm.getBufferingRequests();
            // Go through the Buffering Request list and find the comparable
            // service
            // object to time shift buffer resource usage
            // Get the service object of the time shift buffer resource usage
            for (int i = 0; i < brs.length; ++i)
            {
                Service svc = ((TimeShiftBufferResourceUsage) ru).getService();
                if (svc.equals(brs[i].getService()))
                {
                    br = brs[i];
                    break;
                }
            }
            if (br == null)
            {
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in instance of TimeShiftResourceUsage: unable to find BufferingRequest");
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in instance of TimeShiftResourceUsage: unable to find BufferingRequest";
            }
            // return matched object
            return findKey(br);
        }

        if (ru instanceof SharedResourceUsage)
        {
            System.out.println("<<<<<<<<<Found a Shared resource usage>>>>>>>>>>>");
            String item = null;
            String keyitem = null;
            ResourceUsage[] rus = ((SharedResourceUsage) ru).getResourceUsages();
            for (int i = 0; i < rus.length; ++i)
            {
                System.out.println("SharedResourceUsage - usage " + i + ": " + rus[i] + ".\n");
                if (rus[i] instanceof RecordingResourceUsage)
                {
                    RecordingRequest rr = ((RecordingResourceUsage) rus[i]).getRecordingRequest();
                    if (findKey(rr) == null)
                    { // Hack to deal with the fact that a recording won't be
                      // added to the
                        // DVRTest recording list until record() has returned -
                        // which will
                        // be after the RCH has been called and returned
                        insertObject(rr, m_defaultRecordingName);
                        System.out.print("DvrMonAppTest.getNameForResourceUsage():findKey(rr) == null");
                    }
                    keyitem = findKey(rr);
                    System.out.println("<<<<<<<<<Found Recording in Shared Resource Usage:" + keyitem + ">>>>>>>");
                }
                if (rus[i] instanceof ServiceContextResourceUsage)
                {
                    ServiceContext sc = ((ServiceContextResourceUsage) rus[i]).getServiceContext();
                    keyitem = findKey(sc);
                    System.out.println("<<<<<<<<<Found Service Context in Shared Resource Usage:" + keyitem + ">>>>>>>");
                }
                if (rus[i] instanceof TimeShiftBufferResourceUsage)
                {
                    System.out.println("<<<<<<<<<Found Time Shift in Shared Resource Usage>>>>>>>");
                }
                if (item != null)
                {
                    if (item.compareTo(keyitem) <= 0)
                    {
                        item = keyitem;
                        System.out.println("<<<<<<<<<Passing back :" + keyitem + ">>>>>>>");
                    }
                }
                else
                {
                    item = keyitem;
                    System.out.println("<<<<<<<<<Passing back :" + keyitem + ">>>>>>>");
                }
            }
            return item;
        }

        // Otherwise assume that the ResouceClient is the registered object
        String[] resourceNames = ru.getResourceNames();

        System.out.println("DvrMonAppTest.getNameForResourceUsage(): Resource names for ResourceUsage " + ru + ':');
        for (int i = 0; i < resourceNames.length; i++)
        {
            System.out.println("DvrMonAppTest.getNameForResourceUsage(): ResourceUsage name: " + resourceNames[i]);
        }

        System.out.println("DvrMonAppTest.getNameForResourceUsage(): Getting client for resource " + resourceNames[0]);

        ResourceClient rc = ru.getResource(resourceNames[0]).getClient();

        return findKey(rc);
    } // END getNameForResourceUsage

    void initSC()
    {
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            m_serviceContext = scf.createServiceContext();
            if (m_serviceContext instanceof TimeShiftProperties)
            {
                TimeShiftProperties tsp = (TimeShiftProperties) m_serviceContext;
                // Duration is in seconds
                tsp.setMinimumDuration(30);
            }
        }
        catch (Exception e)
        {
            System.out.println("SelectService - createServiceContext failed!");
            m_failed = TEST_FAILED;
            DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in initSC() due to scf.createServiceContext() exception: "
                    + e.toString());
            m_failedReason = "Flagged FAILURE in initSC() due to scf.createServiceContext() exception: " + e.toString();
            e.printStackTrace();
            return;
        }

    }

    void cleanSC()
    {
        System.out.println("\ncleanSC - Cleaning up ServiceContext and TS.");
        DvrTestMonAppXlet.log("\ncleanSC - Cleaning up ServiceContext and TS.");

        // remove the TimeShift from the SC
        if (m_serviceContext instanceof TimeShiftProperties)
        {
            TimeShiftProperties tsp = (TimeShiftProperties) m_serviceContext;
            tsp.setMinimumDuration(0);
        }

        if (null != m_serviceContext)
        {
            try
            {
                m_serviceContext.stop();
            }
            catch (Exception e)
            {
                System.out.println("cleanSC - problem stopping SC: " + e.toString());
                DvrTestMonAppXlet.log("cleanSC - problem stopping SC: " + e.toString());
                e.printStackTrace();
            }
            try
            {
                Thread.sleep(15000);
            }
            catch (Exception e)
            {
                System.out.println("Thread does not sleep");
            }

            try
            {
                m_serviceContext.destroy();
            }
            catch (Exception e)
            {
                System.out.println("cleanSC - problem stopping TSB: " + e.toString());
                DvrTestMonAppXlet.log("cleanSC - problem stopping TSB: " + e.toString());
                e.printStackTrace();
            }
            try
            {
                Thread.sleep(15000);
            }
            catch (Exception e)
            {
                System.out.println("Thread does not sleep");
            }

            DvrTestMonAppXlet.log("SC and TSB Cleanup complete");
        }
        else
        {
            System.out.println("cleanSC - SC = null - nothing to clean");
            DvrTestMonAppXlet.log("cleanSC - SC = null - nothing to clean");
        }
    }

    Player getServicePlayer(ServiceContext sc)
    {
        System.out.println("getServicePlayer");
        ServiceContentHandler[] handlers = sc.getServiceContentHandlers();
        for (int i = 0; i < handlers.length; ++i)
        {
            ServiceContentHandler handler = handlers[i];
            System.out.println("check handler " + handler);
            if (handler instanceof Player)
            {
                System.out.println("found player " + handler + " for context " + sc);
                return (Player) handler;
            }
        }
        System.out.println("getServicePlayer, no player found for context " + sc);
        m_failedReason = "getServicePlayer, no player found for context " + sc;
        m_failed = TEST_FAILED;
        return null;

    }

    void postResults(String testName, boolean checkSpace)
    {
        if (m_failed == TEST_FAILED)
        {
            if (checkSpace)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DvrTestMonAppXlet.log(testName + "TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DvrTestMonAppXlet.log(testName + DvrEventPrinter.Fail);
                }
            }
            else
            {
                DvrTestMonAppXlet.log(testName + DvrEventPrinter.Fail);
            }
        }
        else
        {
            DvrTestMonAppXlet.log(testName + DvrEventPrinter.Pass);
        }
    }

    /**
     * @author Ryan
     * 
     *         Method that finds the specific recording request based on the key
     *         and stored entry in AppData. Places entry into the vector list of
     *         cached objects.
     * 
     */
    public void getRecReq(String recName, String key, String data)
    {
        // TODO Auto-generated constructor stub

        System.out.println("<<<<<<<<< getRecReq CALLED >>>>>>>>>>>>>>>>");

        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        RecordingList rl = rm.getEntries();

        if (rl == null)
        {
            System.out.println("<<<<getRecReq: RecordingList = null!");
            m_failed = TEST_FAILED;
            return;
        }

        System.out.println("<<<<<<<<< getRecReq : " + rl.size() + " entries >>>>>>>>>>>>>>>>");

        for (int x = 0; x < rl.size(); x++)
        {
            try
            {
                RecordingRequest rr = rl.getRecordingRequest(x);
                String info = (String) rr.getAppData(key);
                System.out.println("<<<<<<<<<ENTRY DATA :" + info + " >>>>>>>>>>>");
                if ((info != null) && (info.equals(data)))
                {
                    DvrTestMonAppXlet.log("<<<< REQUEST FOUND! giving request the name : " + recName + " >>>>");
                    insertObject(rr, recName);
                    return;
                }
                else
                {
                    System.out.println("<<<<< ENTRY " + x + " not it!>>>>>");
                }
            }
            catch (Exception e)
            {
                System.out.println("<<<<getRecReq: Exception finding recording request" + rl.getRecordingRequest(x));
                m_failed = TEST_FAILED;
                m_failedReason = "getRecReq: Exception finding recording request" + rl.getRecordingRequest(x) + ": "
                        + e.getMessage();
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * -----------------------------DVR Utils ----------------------------
     * 
     * Event classes used to schedule actions in the test cases
     * 
     * -------------------------------------------------------------------
     */

    /**
     * Utility tasks - schedule a printout of all the recordings in the
     * recording manager
     */
    class PrintRecordings extends EventScheduler.NotifyShell
    {
        PrintRecordings(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            dumpDvrRecordings();
        }
    }

    /**
     * Schedule a printout statement to the logging console and display
     */
    class PrintOut extends EventScheduler.NotifyShell
    {
        PrintOut(String output, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_output = output;
        }

        public void ProcessCommand()
        {
            DvrTestMonAppXlet.log(m_output);
        }

        private String m_output;
    }

    /**
     * Counts the number of recordings present in the recording manager DB If
     * the count does not equal the number specified in the constructor, the
     * test status is set to FAILED.
     */
    class CountRecordings extends EventScheduler.NotifyShell
    {
        CountRecordings(int expectedCount, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_expectedCount = expectedCount;
        }

        public void ProcessCommand()
        {
            try
            {
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                RecordingList rl = rm.getEntries();

                if (rl.size() != m_expectedCount)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CountRecordings: Expected "
                            + m_expectedCount + " recordings, found " + rl.size() + "!");
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in CountRecordings: Expected " + m_expectedCount
                            + " recordings, found " + rl.size() + "!";
                    dumpDvrRecordings();
                }
            }
            catch (Exception e)
            {
                System.out.println("Exception thrown in CountRecrodings method");
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in CountRecrodings method";
                e.printStackTrace();
            }
        }

        private int m_expectedCount = 0;
    }

    /**
     * Schedules a delete of all recordings in the recording DB
     */
    class DeleteRecordings extends EventScheduler.NotifyShell
    {
        DeleteRecordings(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            deleteAllRecordings();
            // flatten recording list
            m_objects = new Vector();
        }
    }

    /**
     * Schedules a future call to schedule a recording by locator At the task's
     * trigger time, a call to RecordingManager.record will be made with the
     * parameters specified. The resulting recording will placed in the test's
     * recording map.
     */
    class Record extends EventScheduler.NotifyShell
    {
        Record(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        Record(String recordingName, OcapLocator source, long startTime, long duration, MediaStorageVolume destination,
                long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
            m_msv = destination;
        }

        Record(Logger log, String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
            m_log = log;
        }

        Record(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime,
                long expiration)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = expiration;
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        Record(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime,
                long expiration, int retentionPriority)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = expiration;
            m_recordingName = recordingName;
            m_retentionPriority = retentionPriority;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        Record(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime,
                long expiration, int retentionPriority, byte recordingPriority)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = expiration;
            m_recordingName = recordingName;
            m_retentionPriority = retentionPriority;
            m_recordingPriority = recordingPriority;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                OcapRecordingRequest rr;
                LocatorRecordingSpec lrs;
                OcapRecordingProperties orp;

                if (m_log != null)
                {
                    m_log.debug("<<<<<<Record ProcessCommand>>>>>");
                    m_log.debug("DVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime
                            + " Duration:" + m_duration + " retentionPriority = " + m_retentionPriority);
                }
                else
                {
                    System.out.println("<<<<<<Record ProcessCommand>>>>>");
                    System.out.println("\nDVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime
                            + " Duration:" + m_duration + " retentionPriority = " + m_retentionPriority);
                }
                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration,
                        m_retentionPriority, m_recordingPriority, null, null, m_msv);
                lrs = new LocatorRecordingSpec(m_source, new Date(m_startTime), m_duration, orp);

                m_defaultRecordingName = m_recordingName;

                rr = (OcapRecordingRequest) rm.record(lrs);

                m_defaultRecordingName = null;

                if (rr != null)
                {
                    insertObject(rr, m_recordingName);

                    if (m_log != null)
                    {
                        m_log.debug("*****************************************************************");
                        m_log.debug("****" + m_recordingName + " scheduled as " + rr.toString() + "*****");
                        m_log.debug("*****************************************************************");
                    }
                    else
                    {
                        System.out.println("*****************************************************************");
                        System.out.println("****" + m_recordingName + " scheduled as " + rr.toString() + "*****");
                        System.out.println("*****************************************************************");
                    }

                }
            }
            catch (Exception e)
            {
                if (m_log != null)
                {
                    m_log.debug("DVRUtils: Record: FAILED");
                }
                else
                {
                    System.out.println("DVRUtils: Record: FAILED");
                }
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in Record due to rm.record() xception: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in Record due to rm.record() xception: "
                        + e.toString();
            }
        }

        private OcapLocator m_source[];

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;

        private int m_retentionPriority;

        private byte m_recordingPriority;

        private MediaStorageVolume m_msv = null;

        private Logger m_log;
    }

    /**
     * Schedules a future call to schedule a recording by service At the task's
     * trigger time, a call to RecordingManager.record will be made with the
     * parameters specified. The resulting recording will placed in the test's
     * recording map.
     */
    class RecordByService extends EventScheduler.NotifyShell
    {
        /**
         * @param time
         */
        RecordByService(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                OcapRecordingRequest rr;
                ServiceRecordingSpec srs;
                OcapRecordingProperties orp;
                System.out.println("\nDVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime
                        + " Duration:" + m_duration + " retentionPriority = " + m_retentionPriority);

                System.out.println("Retrieving service for" + m_source[0]);
                SIManager siManager = SIManager.createInstance();
                Service service = siManager.getService(m_source[0]);

                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration,
                        m_retentionPriority, m_recordingPriority, null, null, null);

                srs = new ServiceRecordingSpec(service, new Date(m_startTime), m_duration, orp);

                m_defaultRecordingName = m_recordingName;

                rr = (OcapRecordingRequest) rm.record(srs);

                m_defaultRecordingName = null;

                if (rr != null)
                {
                    insertObject(rr, m_recordingName);

                    System.out.println("*****************************************************************");
                    System.out.println("****" + m_recordingName + " scheduled as " + rr.toString() + "*****");
                    System.out.println("*****************************************************************");

                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Record: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in Record due to rm.record() xception: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in Record due to rm.record() xception: "
                        + e.toString();
            }
        }

        private OcapLocator[] m_source;

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;

        private int m_retentionPriority;

        private byte m_recordingPriority;
    }

    /**
     * Schedules a future call to reschedule a recording At the task's trigger
     * time, a call to RecordingRequest.reschedule will be made with the
     * parameters specified.
     */
    class Reschedule extends EventScheduler.NotifyShell
    {

        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newStartTime
         *            New start time. If 0, use the pre-existing start time.
         * @param newDuration
         *            New duration. If 0, use the pre-existing duration.
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        Reschedule(String recordingName, long newStartTime, long newDuration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_startTime = newStartTime;
            m_duration = newDuration;
            m_recordingName = recordingName;
            m_source = null;
            m_prop = null;
        }

        Reschedule(String recordingName, long newStartTime, long newDuration, long taskTriggerTime, OcapLocator source)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_startTime = newStartTime;
            m_duration = newDuration;
            m_recordingName = recordingName;
            m_source[0] = source;
            m_prop = null;
        }

        Reschedule(String recordingName, long newStartTime, long newDuration, long taskTriggerTime, OcapLocator source,
                OcapRecordingProperties prop)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_startTime = newStartTime;
            m_duration = newDuration;
            m_recordingName = recordingName;
            m_source[0] = source;
            m_prop = prop;
        }

        public void ProcessCommand()
        {
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

                if (rr == null)
                {
                    System.out.println("DVRUtils: Reschedule recording - recording not found: " + m_recordingName);
                    return;
                }

                LocatorRecordingSpec oldLrs, newLrs = null;
                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());
                Date newStartTime = (m_startTime == 0) ? oldLrs.getStartTime() : new Date(m_startTime);
                long newDuration = (m_duration == 0) ? oldLrs.getDuration() : m_duration;

                System.out.println("DVRUtils: issuing reschedule for " + m_recordingName + ": " + oldLrs.getSource()
                        + " StartTime:" + newStartTime.getTime() + " Duration:" + m_duration);

                if (m_prop == null)
                {
                    if (m_source == null)
                    {
                        newLrs = new LocatorRecordingSpec(oldLrs.getSource(), newStartTime, newDuration,
                                oldLrs.getProperties());
                    }
                    else
                    {
                        newLrs = new LocatorRecordingSpec(m_source, newStartTime, newDuration, oldLrs.getProperties());
                    }
                }
                else
                {
                    if (m_source == null)
                    {
                        newLrs = new LocatorRecordingSpec(oldLrs.getSource(), newStartTime, newDuration, m_prop);
                    }
                    else
                    {
                        newLrs = new LocatorRecordingSpec(m_source, newStartTime, newDuration, m_prop);
                    }
                }
                rr.reschedule(newLrs);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in Reschedule due to exception: " + e.toString();
            }
        }

        private long m_startTime;

        private long m_duration;

        private OcapLocator m_source[];

        private String m_recordingName;

        private OcapRecordingProperties m_prop;
    } // END class Reschedule

    /**
     * Intializes the ServiceContext in the DVRTestFramework Default settings
     * used.
     */
    class initServiceContext extends EventScheduler.NotifyShell
    {
        private String m_name;

        /**
         * string
         * 
         * @param time
         */
        initServiceContext(long time)
        {
            super(time);
            m_name = "DefaultDvrTestServiceContextName";
            // TODO Auto-generated constructor stub
        }

        initServiceContext(String name, long time)
        {
            super(time);
            m_name = name;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<initServiceContext::ProcessCommand>>>>");

            ServiceContextFactory scf = ServiceContextFactory.getInstance();
            try
            {
                m_serviceContext = scf.createServiceContext();
                insertObject(m_serviceContext, m_name);
            }
            catch (Exception e)
            {
                System.out.println("SelectService - createServiceContext failed!");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in initSC() due to scf.createServiceContext() exception: "
                        + e.toString());
                m_failedReason = "Flagged FAILURE in initSC() due to scf.createServiceContext() exception: "
                        + e.toString();
                e.printStackTrace();
                return;
            }
        }
    }


    class setSCBuffering extends EventScheduler.NotifyShell
    {

        /**
         * @param time
         * @param minDuration
         * @param maxDuration
         */
        setSCBuffering(long minDuration, long maxDuration, long time)
        {
            super(time);
            m_minDur = minDuration;
            m_maxDur = maxDuration;
            m_lastServ = false;
            m_pref = false;
            m_SCname = "";
        }

        setSCBuffering(long minDuration, long maxDuration, boolean lastService, boolean preference, long time)
        {
            super(time);
            m_minDur = minDuration;
            m_maxDur = maxDuration;
            m_lastServ = lastService;
            m_pref = preference;
            m_SCname = "";
        }

        setSCBuffering(String SCname, long minDuration, long maxDuration, boolean lastService, boolean preference,
                long time)
        {
            super(time);
            m_minDur = minDuration;
            m_maxDur = maxDuration;
            m_lastServ = lastService;
            m_pref = preference;
            m_SCname = SCname;
        }

        public setSCBuffering(Logger log, String SCname, long minDuration, long maxDuration, boolean lastService,
                boolean preference, long time)
        {
            super(time);
            m_minDur = minDuration;
            m_maxDur = maxDuration;
            m_lastServ = lastService;
            m_pref = preference;
            m_SCname = SCname;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<setSCBuffering::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<setSCBuffering::ProcessCommand>>>>");
            }

            try
            {
                if (m_SCname != "")
                {
                    // Find the object in the hashtable if specific service
                    // conext is specified
                    m_serviceContext = (ServiceContext) findObject(m_SCname);
                }
                TimeShiftProperties tsp = null;

                if (m_serviceContext instanceof TimeShiftProperties)
                {
                    tsp = (TimeShiftProperties) m_serviceContext;
                }
                // set the properties
                tsp.setMaximumDuration(m_maxDur);
                tsp.setMinimumDuration(m_minDur);
                tsp.setLastServiceBufferedPreference(m_lastServ);
                tsp.setSavePreference(m_pref);

                // Verify the properties just set
                long min = tsp.getMinimumDuration();
                long max = tsp.getMaximumDuration();
                boolean lsbp = tsp.getLastServiceBufferedPreference();
                boolean sp = tsp.getSavePreference();

                if (min != m_minDur)
                {
                    DvrTestMonAppXlet.log("Mimimum Duration mismatch: set value = " + m_minDur + " | get value = "
                            + min);
                    m_failedReason = "Mimimum Duration mismatch: set value = " + m_minDur + " | get value = " + min;
                    m_failed = TEST_FAILED;
                }
                if ((max != m_maxDur) && ((max == 0) && (m_minDur != 0)))
                {
                    DvrTestMonAppXlet.log("Maximum Duration mismatch: set value = " + m_maxDur + " | get value = "
                            + max);
                    m_failedReason = "Maximum Duration mismatch: set value = " + m_maxDur + " | get value = " + max;
                    m_failed = TEST_FAILED;
                }
                if (lsbp != m_lastServ)
                {
                    DvrTestMonAppXlet.log("Last Buffered Preference mismatch: set value = "
                            + (m_lastServ ? "true" : "false") + " | get value = " + (lsbp ? "true" : "false"));
                    m_failedReason = "Last Buffered Preference mismatch: set value = "
                            + (m_lastServ ? "true" : "false") + " | get value = " + (lsbp ? "true" : "false");
                    m_failed = TEST_FAILED;
                }
                if (sp != m_pref)
                {
                    DvrTestMonAppXlet.log("Save Preference mismatch: set value = " + (m_pref ? "true" : "false")
                            + " | get value = " + (sp ? "true" : "false"));
                    m_failedReason = "Save Preference mismatch: set value = " + (m_pref ? "true" : "false")
                            + " | get value = " + (sp ? "true" : "false");
                    m_failed = TEST_FAILED;
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
            }
            if (m_log != null)
            {
                m_log.debug("<<<<setSCBuffering::ProcessCommand  Done>>>>");
            }
            else
            {
                System.out.println("<<<<setSCBuffering::ProcessCommand  Done>>>>");
            }
        }

        private long m_minDur;

        private long m_maxDur;

        private boolean m_lastServ;

        private boolean m_pref;

        private String m_SCname;

        private Logger m_log;
    }

    /**
     * Schedules a buffering request in the DVRTestFramework
     * 
     */
    class ScheduleBufferingRequest extends EventScheduler.NotifyShell
    {
        private OcapLocator m_source;

        private long m_minDur;

        private long m_maxDur;

        private String m_name;

        /**
         * @param time
         */
        ScheduleBufferingRequest(String bufferName, OcapLocator source, long minimumDuration, long maximumDuration,
                long time)
        {
            super(time);
            m_source = source;
            m_name = bufferName;
            m_minDur = minimumDuration;
            m_maxDur = maximumDuration;
        }

        public void ProcessCommand()
        {
            // TODO Auto-generated constructor stub
            System.out.println("<<<<ScheduleBufferingRequest::ProcessCommand>>>>");
            System.out.println("Retrieving service for" + m_source);
            SIManager siManager = SIManager.createInstance();
            Service service = null;
            try
            {
                service = siManager.getService(m_source);
            }
            catch (InvalidLocatorException e)
            {
                m_failed = TEST_FAILED;
                System.out.println("No service");
            }
            try
            {
                StorageProxy[] sproxy = StorageManager.getInstance().getStorageProxies();
                LogicalStorageVolume[] lsv = null;

                for (int i = 0; i < sproxy.length; i++)
                {
                    lsv = sproxy[i].getVolumes();
                    if (lsv != null)
                    {
                        // System.out.println("---- Logical Volume found ----");
                        break;
                    }
                }
                ExtendedFileAccessPermissions efap = lsv[0].getFileAccessPermissions();
                BufferingRequest bufferReq = BufferingRequest.createInstance(service, m_minDur, m_maxDur, efap);
                insertObject(bufferReq, m_name);
                DvrTestMonAppXlet.log("<<<<<<<< Buffering Request " + m_name + " created>>>>>>>");

                // Verify the properties just set
                long min = bufferReq.getMinimumDuration();
                long max = bufferReq.getMaxDuration();

                if (min != m_minDur)
                {
                    DvrTestMonAppXlet.log("Mimimum Duration mismatch: set value = " + m_minDur + " | get value = "
                            + min);
                    m_failedReason = "Mimimum Duration mismatch: set value = " + m_minDur + " | get value = " + min;
                    m_failed = TEST_FAILED;
                }
                if (max != m_maxDur)
                {
                    DvrTestMonAppXlet.log("Maximum Duration mismatch: set value = " + m_maxDur + " | get value = "
                            + max);
                    m_failedReason = "Maximum Duration mismatch: set value = " + m_maxDur + " | get value = " + max;
                    m_failed = TEST_FAILED;
                }
            }
            catch (Exception e)
            {
                DvrTestMonAppXlet.log("!!!!!Exception thrown in Schedule Buffering Request!!!!!!");
                m_failed = TEST_PASSED;
                e.printStackTrace();
            }
        }
    }

    class SetServiceOnBufferingRequest extends EventScheduler.NotifyShell
    {

        SetServiceOnBufferingRequest(Logger log, String name, OcapLocator loc, long triggerTime)
        {
            super(triggerTime);
            m_log = log;
            m_name = name;
            m_locator = loc;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("\n");
                m_log.debug("<<<<SetServiceOnBufferingRequest::ProcessCommand>>>>");

            }
            else
            {
                System.out.println();
                System.out.println("<<<<SetServiceOnBufferingRequest::ProcessCommand>>>>");
            }

            BufferingRequest br = (BufferingRequest) findObject(m_name);
            if (br == null)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:SetServiceOnBufferingRequest findObject failed: " + m_name);
                m_failedReason = "DvrMonAppTest:SetServiceOnBufferingRequest findObject failed: " + m_name;
                return;
            }

            Service service;
            try
            {
                SIManager siManager = SIManager.createInstance();
                service = siManager.getService(m_locator);
            }
            catch (Exception e)
            {
                if (m_log != null)
                {
                    m_log.debug("SetServiceOnBufferingRequest - getService failed!" + m_locator);
                }
                else
                {
                    System.out.println("SetServiceOnBufferingRequest - siManager.getService failed!" + m_locator);
                }
                DvrTestMonAppXlet.log("DvrMonAppTest:SetServiceOnBufferingRequest Flagged FAILURE in siManager.getService due to sim.getService() exception: "
                        + e.toString());
                m_failed = TEST_FAILED;
                m_failedReason = "DvrMonAppTest:SetServiceOnBufferingRequest Flagged FAILURE in siManager.getService due to sim.getService() exception: "
                        + e.toString();
                e.printStackTrace();
                return;
            }

            try
            {
                br.setService(service);
            }
            catch (IllegalArgumentException iae)
            {
                if (m_log != null)
                {
                    m_log.debug("SetServiceOnBufferingRequest:br.setService IllegalArguementException");

                }
                else
                {
                    System.out.println("SetServiceOnBufferingRequest:br.setService IllegalArguementException");
                }
                iae.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SetServiceOnBufferingRequest:setService : " + iae.toString();
                return;
            }
            catch (SecurityException se)
            {
                if (m_log != null)
                {
                    m_log.debug("SetServiceOnBufferingRequest:br.setService SecurityException");
                }
                else
                {
                    System.out.println("SetServiceOnBufferingRequest:br.setService SecurityException");
                }
                m_failed = TEST_FAILED;
                m_failedReason = "SetServiceOnBufferingRequest:br.setService SecurityException" + se.toString();
                return;
            }
        }

        private Logger m_log;

        private String m_name;

        private OcapLocator m_locator;
    }

    /*
     * 
     * @author Ryan
     * 
     * Verifies if the active buffer inherited by a buffering Service Context
     * contains at least the time passed in through the contructor (minTime)
     */
    class checkSCBufferDuration extends EventScheduler.NotifyShell
    {

        private long m_minTime;

        private String m_scName;

        /**
         * @param time
         */
        checkSCBufferDuration(long minTime, long time)
        {
            super(time);
            m_minTime = minTime;
            m_scName = "";
        }

        checkSCBufferDuration(String scName, long minTime, long time)
        {
            super(time);
            m_minTime = minTime;
            m_scName = scName;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<checkSCBufferDuration::ProcessCommand>>>>");
            if (m_scName != "")
            {
                // Find the object in the hashtable if specific service conext
                // is specified
                m_serviceContext = (ServiceContext) findObject(m_scName);
            }
            try
            {
                Player player = getServicePlayer(m_serviceContext);
                TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                long bufferSize = (long) (tsc.getDuration().getSeconds() * 1000);
                if (bufferSize < (m_minTime / 1000))
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("Duration of buffer too small - should be at least: " + m_minTime + " was :"
                            + bufferSize);
                    m_failedReason = "Duration of buffer too small - should be at least: " + m_minTime + " was :"
                            + bufferSize;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("Exception thrown in checkSCBufferDuration");
                m_failedReason = "Exception thrown in checkSCBufferDuration" + e.toString();
            }
        }
    }

    /**
     * Verifies if a buffering request has the specified App ID ir is null
     * 
     */
    class VerifyBufferingRequest_AppID extends EventScheduler.NotifyShell
    {
        /**
         * @param name
         * @param time
         */
        VerifyBufferingRequest_AppID(Logger log, String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_isCalledFromApp = true;
            m_log = log;
        }

        VerifyBufferingRequest_AppID(Logger log, String name, boolean isCalledFromApp, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_isCalledFromApp = isCalledFromApp;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("\n");
                m_log.debug("<<<<VerifyBufferingRequest_AppID::ProcessCommand>>>>");

            }
            else
            {
                System.out.println();
                System.out.println("<<<<VerifyBufferingRequest_AppID::ProcessCommand>>>>");
            }

            BufferingRequest br = (BufferingRequest) findObject(m_name);
            if (br == null)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:VerifyBufferingRequest_AppID findObject failed: " + m_name);
                m_failedReason = "DvrMonAppTest:VerifyBufferingRequest_AppID findObject failed: " + m_name;
                return;
            }
            System.out.println("Buffering Request " + br.toString());
            AppID aid = br.getAppID();
            if ((m_isCalledFromApp && aid == null) || (!m_isCalledFromApp && aid != null))
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:VerifyBufferingRequest_AppID:FAILURE IsCalledFromApp: "
                        + m_isCalledFromApp + " AppID: " + aid);
                m_failedReason = "DvrMonAppTest:VerifyBufferingRequest_AppID:FAILURE: IsCalledFromApp: "
                        + m_isCalledFromApp + " AppID: " + aid;
            }

            if (m_log != null)
            {
                m_log.debug("<<<<VerifyBufferingRequest_AppID::ProcessCommand done>>>>");

            }
            else
            {
                System.out.println("<<<<VerifyBufferingRequest_AppID::ProcessCommand done>>>>");
            }
        }

        String m_name;

        boolean m_isCalledFromApp;

        private Logger m_log;
    }

    /**
     * Schedules a call to stop the recording specified. When triggered, this
     * task will lookup the recording in this tests internal recording map and
     * issue OcapRecordingRequest.stop() if found.
     */
    class StopRecording extends EventScheduler.NotifyShell
    {
        StopRecording(String recording, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_ignoreFailure = false;
        }

        StopRecording(Logger log, String recording, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_ignoreFailure = false;
            m_log = log;
        }

        StopRecording(String recording, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_ignoreFailure = ignoreFailure;
        }

        StopRecording(Logger log, String recording, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<StopRecording::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<StopRecording::ProcessCommand>>>>");
            }
            try
            {

                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

                if (rr == null)
                {
                    System.out.println("DVRUtils: Stopping recording - recording not found: " + m_recording);
                    return;
                }
                // removeObject(rr);
                rr.stop();
            }
            catch (Exception e)
            {
                if (m_log != null)
                {
                    m_log.debug("DvrMonAppTest: Exception on Stop!");
                }
                else
                {
                    System.out.println("DvrMonAppTest: Exception on Stop!");
                }
                e.printStackTrace();
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in StopRecording - exception: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in StopRecording - exception: " + e.toString();
                }
            }
        }

        private String m_recording;

        private boolean m_ignoreFailure;

        private Logger m_log;
    }

    class SelectService extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        SelectService(OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_sl = this;
            m_locator = serviceLocator;
            m_serviceContextName = "DefaultDvrTestServiceContextName";
            m_ignoreFailure = false;
            m_globalSC = true;
        }

        SelectService(String serviceContextName, OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_sl = this;
            m_locator = serviceLocator;
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = false;
            m_globalSC = true;
        }

        SelectService(Logger log, String serviceContextName, OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_sl = this;
            m_locator = serviceLocator;
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = false;
            m_globalSC = true;
            m_log = log;
        }

        SelectService(String serviceContextName, OcapLocator serviceLocator, ServiceContextListener sl, long triggerTime)
        {
            super(triggerTime);
            m_sl = sl;
            m_locator = serviceLocator;
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = false;
            m_globalSC = true;
        }

        SelectService(String serviceContextName, OcapLocator serviceLocator, boolean ignoreFailure, boolean globalSC,
                long triggerTime)
        {
            super(triggerTime);
            m_sl = this;
            m_locator = serviceLocator;
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = ignoreFailure;
            m_globalSC = globalSC;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<SelectService::ProcessCommand>>>>\nSCname = " + m_serviceContextName + "\n locator = "
                        + m_locator);
            }
            else
            {
                System.out.println("<<<<SelectService::ProcessCommand>>>>\nSCname = " + m_serviceContextName
                        + "\n locator = " + m_locator);
            }

            Service service;

            try
            {
                m_serviceContext.addListener(this);
                SIManager siManager = SIManager.createInstance();
                service = siManager.getService(m_locator);
            }
            catch (Exception e)
            {
                if (m_log != null)
                {
                    m_log.debug("SelectService - getService failed!" + m_locator);
                }
                else
                {
                    System.out.println("SelectService - getService failed!" + m_locator);
                }
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SelectService due to sim.getService() exception: "
                        + e.toString());
                m_failed = TEST_FAILED;
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in SelectService due to sim.getService() exception: "
                        + e.toString();
                e.printStackTrace();
                return;
            }
            if (m_globalSC)
            {
                insertObject(m_serviceContext, m_serviceContextName);
            }
            else
            {
                // Or find the object in the hashtable
                m_serviceContext = (ServiceContext) findObject(m_serviceContextName);
            }

            try
            {
                m_serviceContext.select(service);
            }
            catch (Exception e)
            {
                System.out.println("SelectService - Service selection failed on " + m_locator);
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "SelectService - Service selection failed on " + m_locator + ". Exception: "
                            + e.getMessage();
                }
                e.printStackTrace();
            }
            if (m_log != null)
            {
                m_log.debug("<<<<SelectService::ProcessCommand>>>>  Done.");
            }
            else
            {
                System.out.println("<<<<SelectService::ProcessCommand>>>>  Done.");
            }
        }

        public ServiceContext getServiceContext()
        {
            return m_serviceContext;
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (m_log != null)
            {
                m_log.debug("DvrMonAppTest:SelectService: receiveServiceContextEvent" + ev + "SCname = "
                        + m_serviceContextName + "locator = " + m_locator);
            }
            else
            {
                System.out.println("DvrMonAppTest:SelectService: receiveServiceContextEvent" + ev + "SCname = "
                        + m_serviceContextName + "locator = " + m_locator);
            }
            m_serviceContext.removeListener(m_sl);
        }

        private OcapLocator m_locator;

        private String m_serviceContextName;

        private boolean m_ignoreFailure;

        private boolean m_globalSC;

        private ServiceContextListener m_sl;

        private Logger m_log;

    } // END class SelectService

    class StopService extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        StopService(long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = "DefaultDvrTestServiceContextName";
            m_ignoreFailure = false;
        }

        StopService(String serviceContextName, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = false;
        }

        StopService(String serviceContextName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = ignoreFailure;
        }

        StopService(Logger log, String serviceContextName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public void ProcessCommand()
        {
            try
            {
                if (m_log != null)
                    m_log.debug("<<<<StopService::ProcessCommand>>>> SCname = " + m_serviceContextName);
                else
                    System.out.println("<<<<StopService::ProcessCommand>>>> SCname = " + m_serviceContextName);

                Object o = findObject(m_serviceContextName);

                if (o == null)
                {
                    System.out.println("DVRUtils: StopService - service not found: " + m_serviceContextName);
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in StopService due to unfound service context name: "
                            + m_serviceContextName);
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in StopService due to unfound service context name: "
                            + m_serviceContextName;
                    return;
                }
                ServiceContext sc = (ServiceContext) o;

                sc.stop();
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: StopService - ServiceContext.stop() failed: " + m_serviceContextName);
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in StopService sc.stop() due to sc.stop() exception: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in StopService sc.stop() due to sc.stop() exception: "
                            + e.toString();
                }
                e.printStackTrace();
                return;
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (m_log != null)
                m_log.debug("DVRUtils: StopService: SCname = " + m_serviceContextName + " receiveServiceContextEvent"
                        + ev);
            else
                System.out.println("DVRUtils: StopService: SCname = " + m_serviceContextName
                        + " receiveServiceContextEvent" + ev);
        }

        private String m_serviceContextName;

        private boolean m_ignoreFailure;

        private Logger m_log;
    } // END class StopService

    class DestroyService extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        DestroyService(long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = "DefaultDvrTestServiceContextName";
            m_ignoreFailure = false;
        }

        DestroyService(String serviceContextName, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = false;
        }

        DestroyService(String serviceContextName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = ignoreFailure;
        }

        DestroyService(Logger log, String serviceContextName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_serviceContextName = serviceContextName;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public void ProcessCommand()
        {
            try
            {
                if (m_log != null)
                    m_log.debug("<<<<DestroyService::ProcessCommand>>>>");
                else
                    System.out.println("<<<<DestroyService::ProcessCommand>>>>");

                Object o = findObject(m_serviceContextName);

                if (o == null)
                {
                    System.out.println("DVRUtils: DestroyService - service not found: " + m_serviceContextName);
                    return;
                }
                ServiceContext sc = (ServiceContext) o;

                sc.destroy();
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: DestroyService - ServiceContext.destroy() failed: "
                        + m_serviceContextName);
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in DestroyService due to sc.destroy() exception: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in DestroyService due to sc.destroy() exception: "
                            + e.toString();
                }
                e.printStackTrace();
                return;
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (m_log != null)
                m_log.debug("DVRUtils: DestroyService: receiveServiceContextEvent" + ev);
            else
                System.out.println("DVRUtils: DestroyService: receiveServiceContextEvent" + ev);
        }

        private String m_serviceContextName;

        private boolean m_ignoreFailure;

        private Logger m_log;
    } // END class DestroyService

    class TuneNetworkWrapper extends NetworkInterfaceController
    {
        TuneNetworkInterface getTNI()
        {
            return m_tni;
        }

        NetworkInterfaceController getNetworkInterfaceController()
        {
            return this;
        }

        // Overriden methods delegates to NetworkInterfaceController
        public TuneNetworkWrapper(ResourceClient rc)
        {
            super(rc);
            m_tni = (TuneNetworkInterface) rc;
        }

        public ResourceClient getClient()
        {
            return super.getClient();
        }

        public NetworkInterface getNetworkInterface()
        {
            return super.getNetworkInterface();
        }

        public void release() throws NetworkInterfaceException
        {
            super.release();
        }

        public void reserve(NetworkInterface nwif, Object requestData) throws NetworkInterfaceException
        {
            super.reserve(nwif, requestData);
        }

        public void reserveFor(Locator locator, Object requestData) throws NetworkInterfaceException
        {
            super.reserveFor((org.davic.net.Locator) locator, requestData);
        }

        public void tune(org.davic.net.Locator locator) throws NetworkInterfaceException
        {
            super.tune(locator);
        }

        public void tune(org.davic.mpeg.TransportStream ts) throws NetworkInterfaceException
        {
            super.tune(ts);
        }

        private NetworkInterfaceController m_nic;

        private NetworkInterface m_ni;

        private TuneNetworkInterface m_tni;
    }

    class TuneNetworkInterface extends EventScheduler.NotifyShell implements NetworkInterfaceListener, ResourceClient,
            ResourceStatusListener
    {
        TuneNetworkInterface(OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = "DefaultDvrTestNetworkInterfaceName";
        }

        TuneNetworkInterface(Logger log, OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = "DefaultDvrTestNetworkInterfaceName";
            m_log = log;
        }

        TuneNetworkInterface(String niName, OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = niName;
        }

        TuneNetworkInterface(Logger log, String niName, OcapLocator serviceLocator, long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = niName;
            m_log = log;
        }

        TuneNetworkInterface(String niName, OcapLocator serviceLocator, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = niName;
            m_ignoreFailure = ignoreFailure;
        }

        TuneNetworkInterface(Logger log, String niName, OcapLocator serviceLocator, boolean ignoreFailure,
                long triggerTime)
        {
            super(triggerTime);
            m_locator = serviceLocator;
            m_niName = niName;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public void ProcessCommand()
        {

            NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
            nim.addResourceStatusEventListener(this);
            try
            {
                if (m_log != null)
                {
                    m_log.debug("<<<<TuneNetworkInterface::ProcessCommand>>>> - niName = " + m_niName + " loc = "
                            + m_locator);
                }
                else
                {
                    System.out.println("<<<<TuneNetworkInterface::ProcessCommand>>>> - niName = " + m_niName
                            + " loc = " + m_locator);
                }
                // m_nic = new NetworkInterfaceController(this);

                // TuneNetworkWrapper is a NetworkInterfaceController.
                m_nic = new TuneNetworkWrapper(this);

                // Put the name of 'this' into object/name lookup table
                insertObject(m_nic, m_niName);

                // This is added because the ReleaseNetworkInterface command
                // expects a
                // TuneNetworkInterface object so we add it to the map. In
                // ReleaseNetworkInterface
                //
                // insertObject(this, m_niName + "_Obj");
                System.out.println("TuneNetworkInterface inserted with KEY = " + m_niName);
                if (findKey(m_nic) == null)
                {
                    if (m_log != null)
                        m_log.debug("TuneNetworkInterface1: findKey returned null!!!");
                    else
                        System.out.println("TuneNetworkInterface1: findKey returned null!!!");
                }
                else
                {
                    if (m_log != null)
                        m_log.debug("TuneNetworkInterface1: findKey = " + findKey(m_nic));
                    else
                        System.out.println("TuneNetworkInterface1: findKey = " + findKey(m_nic));
                }
                /*
                 * if(findKey(m_niName + "_Obj") == null) { if (m_log != null)
                 * m_log.debug(
                 * "TuneNetworkInterface3: findKey (m_niName + _Obj) returned null!!!"
                 * ); elseSystem.out.println(
                 * "TuneNetworkInterface3: findKey (m_niName + _Obj)returned null!!!"
                 * ); }else { if (m_log != null)
                 * m_log.debug("TuneNetworkInterface3: findKey(m_niName + Obj) = "
                 * + findKey(m_niName + "_Obj")); elseSystem.out.println(
                 * "TuneNetworkInterface3: findKey(m_niName + Obj) = " +
                 * findKey(m_niName + "_Obj")); }
                 */
                m_nic.reserveFor(m_locator, null);
            }
            catch (Exception e)
            {
                // Could not reserve tuner...
                nim.removeResourceStatusEventListener(this);
                m_nic = null;
                if (m_log != null)
                {
                    m_log.debug("TuneNetworkInterface - reserveFor() failed for <niName, loc> = <" + m_niName + ", "
                            + m_locator + ">");
                }
                else
                {
                    System.out.println("TuneNetworkInterface - reserveFor() failed for <niName, loc> = <" + m_niName
                            + ", " + m_locator + ">");
                }
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in TuneNetworkInterface ");
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in TuneNetworkInterface ";
                e.printStackTrace();
                return;
            }

            m_niResourceHeld = true;

            // Add listener to watch for tune complete
            m_ni = m_nic.getNetworkInterface();
            m_ni.addNetworkInterfaceListener(this);

            try
            {
                // Tune
                if (m_log != null)
                {
                    m_log.debug("TuneNetworkInterface:" + m_niName + "tuning to " + m_locator);
                }
                else
                {
                    System.out.println("TuneNetworkInterface:" + m_niName + "tuning to " + m_locator);
                }

                m_nic.tune(m_locator);
            }
            catch (Exception e)
            {
                e.printStackTrace();

                try
                {
                    m_nic.release();
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();

                }
                m_ni.removeNetworkInterfaceListener(this);

                // Could not tune...
                // Should just star the app!
                m_nic = null;
                m_ni = null;
                return;
            }

        }

        public void statusChanged(ResourceStatusEvent event)
        {
            if (event instanceof NetworkInterfaceReleasedEvent)
            {
                if (m_log != null)
                {
                    m_log.debug("TuneNetworkInterface:" + m_niName + " recvd NetworkInterfaceReleasedEvent");
                }
                else
                {
                    System.out.println("TuneNetworkInterface:" + m_niName + " recvd NetworkInterfaceReleasedEvent");
                }
                return;
            }

            if (event instanceof NetworkInterfaceReservedEvent)
            {
                if (m_log != null)
                {
                    m_log.debug("TuneNetworkInterface:" + m_niName + " recvd NetworkInterfaceReservedEvent");
                }
                else
                {
                    System.out.println("TuneNetworkInterface:" + m_niName + " recvd NetworkInterfaceReservedEvent");
                }
                return;
            }

            if (m_log != null)
            {
                m_log.debug("TuneNetworkInterface:" + m_niName + " recvd ResourceStateEvent = " + event);
            }
            else
            {
                System.out.println("TuneNetworkInterface:" + m_niName + " recvd ResourceStateEvent = " + event);
            }
        }

        public void receiveNIEvent(org.davic.net.tuning.NetworkInterfaceEvent ev)
        {
            if (m_log != null)
            {
                m_log.debug("TuneNetworkInterface:" + m_niName + " receiveNIEvent = " + ev);
            }
            else
            {
                System.out.println("TuneNetworkInterface: " + m_niName + " receiveNIEvent" + ev);
            }

            if (ev instanceof NetworkInterfaceTuningOverEvent)
            {
                NetworkInterfaceTuningOverEvent status = (NetworkInterfaceTuningOverEvent) ev;
                if (status.getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
                {
                    if (!m_ignoreFailure)
                    {
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log(m_failedReason = "TuneNetworkInterface " + m_niName
                                + " NetworkInterfaceTuningOverEvent.FAILED but ignoring failure");
                    }
                    else
                    {

                        DvrTestMonAppXlet.log(m_failedReason = "TuneNetworkInterface " + m_niName
                                + " NetworkInterfaceTuningOverEvent.FAILED");
                    }
                }
                else
                {
                    DvrTestMonAppXlet.log("TuneNetworkInterface: NetworkInterfaceTuningOverEvent.SUCCESS <name, ni> = "
                            + m_niName + ", " + ev.getSource());
                }
            }
        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            if (m_log != null)
            {
                m_log.debug("TuneNetworkInterface:" + m_niName + "  requestRelease for " + proxy);
            }
            else
                System.out.println("TuneNetworkInterface:" + m_niName + "  requestRelease for " + proxy);
            return false;
        }

        public void release(ResourceProxy proxy)
        {
            if (m_log != null)
            {
                m_log.debug("TuneNetworkInterface:" + m_niName + "  resource release for " + proxy);
            }
            else
                System.out.println("TuneNetworkInterface:" + m_niName + "  resource release for " + proxy);

            m_niResourceHeld = false;
        }

        public void notifyRelease(ResourceProxy proxy)
        {
            if (m_log != null)
            {
                m_log.debug("TuneNetworkInterface:" + m_niName + "  notifyRelease for " + proxy);
            }
            else
                System.out.println("TuneNetworkInterface:" + m_niName + "  notifyRelease for " + proxy);
        }

        public boolean isResourceHeld()
        {
            return m_niResourceHeld;
        }

        public NetworkInterface getNetworkInterface()
        {
            return m_ni;
        }

        public NetworkInterfaceController getNetworkInterfaceController()
        {
            return m_nic;
        }

        private OcapLocator m_locator;

        private String m_niName;

        private NetworkInterfaceController m_nic;

        private NetworkInterface m_ni;

        private boolean m_niResourceHeld = false;

        private boolean m_ignoreFailure = false;

        private Logger m_log;

    } // END class TuneNetworkInterface

    class ConfirmNetworkInterfaceReserved extends EventScheduler.NotifyShell
    {
        ConfirmNetworkInterfaceReserved(long triggerTime, boolean shouldBeReserved)
        {
            super(triggerTime);
            m_niName = "DefaultDvrTestNetworkInterfaceName";
            m_shouldBeReserved = shouldBeReserved;
        }

        ConfirmNetworkInterfaceReserved(String niName, boolean shouldBeReserved, long triggerTime)
        {
            super(triggerTime);
            m_niName = niName;
            m_shouldBeReserved = shouldBeReserved;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<ConfirmNetworkInterfaceReserved::ProcessCommand>>>>");

            Object o = findObject(m_niName);

            if ((o == null) || !(o instanceof TuneNetworkWrapper))
            {
                System.out.println("DVRUtils: ConfirmNetworkInterfaceReserved - TuneNetworkInterface not found for "
                        + m_niName);
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmNetworkInterfaceReserved due to unfound NI for: "
                        + m_niName);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmNetworkInterfaceReserved due to unfound NI for: "
                        + m_niName;
                return;
            }

            try
            {

                TuneNetworkInterface tni = ((TuneNetworkWrapper) o).getTNI(); // (TuneNetworkInterface)o;
                if (tni.isResourceHeld() != m_shouldBeReserved)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmNetworkInterfaceReserved due to reserved state: expected "
                            + m_shouldBeReserved + ", found " + tni.isResourceHeld());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmNetworkInterfaceReserved due to reserved state: expected "
                            + m_shouldBeReserved + ", found " + tni.isResourceHeld();
                }
            }
            catch (Exception e)
            {
                System.out.println("Confirm NetworkInterfaceReserved - NI check failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "Confirm NetworkInterfaceReserved - NI check failed, Exception: " + e.getMessage();
            }
        }

        private String m_niName;

        boolean m_shouldBeReserved;
    } // END class ConfirmNetworkInterfaceReserved

    class ReleaseNetworkInterface extends EventScheduler.NotifyShell
    {
        ReleaseNetworkInterface(long triggerTime, boolean ignoreFailure)
        {
            super(triggerTime);
            m_niName = "DefaultDvrTestNetworkInterfaceName";
            m_ignoreFailure = ignoreFailure;
        }

        ReleaseNetworkInterface(String niName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_niName = niName;
            m_ignoreFailure = ignoreFailure;
        }

        ReleaseNetworkInterface(Logger log, String niName, boolean ignoreFailure, long triggerTime)
        {
            super(triggerTime);
            m_niName = niName;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<ReleaseNetworkInterface::ProcessCommand>>>>");
            }
            else
                System.out.println("<<<<ReleaseNetworkInterface::ProcessCommand>>>>");

            // The TuneNetworkInterface is referenced via a munged key.
            Object o = findObject(m_niName);
            if ((o == null) || !(o instanceof TuneNetworkWrapper))
            {
                System.out.println("DVRUtils: ReleaseNetworkInterface - TuneNetworkInterface not found for " + m_niName);
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ReleaseNetworkInterface due to unfound NI for: "
                        + m_niName);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ReleaseNetworkInterface due to unfound NI for: "
                        + m_niName;
                return;
            }

            try
            {
                TuneNetworkInterface tni = ((TuneNetworkWrapper) o).getTNI();

                tni.getNetworkInterface().removeNetworkInterfaceListener(tni);
                tni.getNetworkInterfaceController().release();

                NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
                nim.removeResourceStatusEventListener(tni);
            }
            catch (Exception e)
            {
                System.out.println("ReleaseNetworkInterface - release() failed for " + m_niName);
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ReleaseNetworkInterface due to nic.release() exception: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in ReleaseNetworkInterface due to nic.release() exception: "
                            + e.toString();
                }
                NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
                TuneNetworkInterface tni = (TuneNetworkInterface) o;
                nim.removeResourceStatusEventListener(tni);
                e.printStackTrace();
                return;
            }
        }

        private String m_niName;

        boolean m_ignoreFailure;

        private Logger m_log;
    } // END class ReleaseNetworkInterface

    // Used in recording tests to verify that the SI that was persisted matches
    // what was gathered from the service that was recorded.
    class SIChecker extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        OcapLocator locator;

        Service service;

        ServiceDetails details;

        ServiceComponent[] components;

        boolean fetchFailed = false;

        SIChecker(OcapLocator loc, long triggerTime)
        {
            super(triggerTime);
            locator = loc;
        }

        public void ProcessCommand()
        {
            SIManager siManager = SIManager.createInstance();
            Service service = null;
            try
            {
                service = siManager.getService(locator);
            }
            catch (Exception e)
            {
                System.out.println("SimpleSIChecker could not find Service based on locator: " + locator);
                fetchFailed = true;
            }
            if (service != null)
            {
                System.out.println("* * * * * * * * * * * * * * * * * * * *");
                System.out.println("*  calling retrieve details  *");
                System.out.println("* * * * * * * * * * * * * * * * * * * *");
                service.retrieveDetails(new SimpleSIrequestor());
            }
            else
            {
                System.out.println("SiChecker could not proceed without a valid service.");
                fetchFailed = true;
            }
        }

        // a little helper for getting the SI that is fetched.
        class SimpleSIrequestor implements SIRequestor
        {

            public void notifySuccess(SIRetrievable[] result)
            {
                // if we have a result
                if (result != null && result.length != 0)
                {
                    if (result[0] instanceof ServiceDetails)
                    {
                        System.out.println("* * * * * * * * * * * * * * * * * * * *");
                        System.out.println("*  got the details  *");
                        System.out.println("* * * * * * * * * * * * * * * * * * * *");
                        details = (ServiceDetails) result[0];
                        details.retrieveComponents(this);
                    }
                    else if (result[0] instanceof ServiceComponent)
                    {
                        System.out.println("* * * * * * * * * * * * * * * * * * * *");
                        System.out.println("*  Got the components  *");
                        System.out.println("* * * * * * * * * * * * * * * * * * * *");
                        components = new ServiceComponent[result.length];
                        for (int i = 0; i < result.length; i++)
                        {
                            components[i] = (ServiceComponent) result[i];
                        }
                    }
                    else
                    {
                        fetchFailed = true;
                        System.out.println(" BAD TYPE IN RESULTING SI");
                    }
                }
                else
                {
                    fetchFailed = true;
                    System.out.println(" NO SI IN RESULTING ARRAY");
                }
            }

            public void notifyFailure(SIRequestFailureType reason)
            {
                System.out.println(" - - - - SIFAILURE NOTIFICATION - - - - -" + reason);
            }

        }

        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            if (e instanceof NormalContentEvent)
            {
                System.out.println("SIChecker getting the SI from the presenting service.");
                System.out.println("do more later...");
            }
            System.out.println("SIChecker received ServiceContextEvent: " + e);
        }
    }

    // Used in recording tests to fetch the recorded SI
    class RecordedSIChecker extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        SIChecker checker;

        Service service;

        ServiceDetails details;

        ServiceComponent[] components;

        boolean fetchFailed = false;

        String recName;

        boolean siMatched = false;

        RecordedSIChecker(SIChecker siChecker, String recordingName, long triggerTime)
        {
            super(triggerTime);
            this.recName = recordingName;
            this.checker = siChecker;
        }

        public void ProcessCommand()
        {
            System.out.println("* * * * * * * * * * * * * * * * * * * *");
            System.out.println("*  Processing command in RecordedSIChecker *");
            System.out.println("* * * * * * * * * * * * * * * * * * * *");
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(recName);
                service = rr.getService();
                if (service != null)
                {
                    System.out.println("*  calling retrieve details for recording *");
                    service.retrieveDetails(new SimpleSIrequestor());
                }
                else
                {
                    System.out.println("RecordedSiChecker could not proceed without a valid service.");
                    fetchFailed = true;
                }
            }
            catch (Exception e)
            {
                System.out.println("*  Error getting recorded service from request.  *");
                m_failed = TEST_FAILED;
                m_failedReason = "*  Error getting recorded service from request.  *, Exception: " + e.getMessage();
                e.printStackTrace();
            }
        }

        // a little helper for getting the SI that is fetched.
        class SimpleSIrequestor implements SIRequestor
        {

            public void notifySuccess(SIRetrievable[] result)
            {
                // if we have a result
                if (result != null && result.length != 0)
                {
                    if (result[0] instanceof ServiceDetails)
                    {
                        System.out.println("*      got the recorded details       *");
                        details = (ServiceDetails) result[0];
                        details.retrieveComponents(this);
                    }
                    else if (result[0] instanceof ServiceComponent)
                    {
                        System.out.println("*      Got the recorded components    *");
                        components = new ServiceComponent[result.length];
                        for (int i = 0; i < result.length; i++)
                        {
                            components[i] = (ServiceComponent) result[i];
                        }
                        compareResults();
                    }
                    else
                    {
                        fetchFailed = true;
                        System.out.println(" BAD TYPE IN RESULTING SI");
                    }
                }
                else
                {
                    fetchFailed = true;
                    System.out.println(" NO SI IN RESULTING ARRAY");
                }
            }

            public void compareResults()
            {
                boolean matched = true;
                for (int i = 0; i < components.length; i++)
                {
                    if (!equalObjects(components[i].getAssociatedLanguage(),
                            checker.components[i].getAssociatedLanguage()))
                    {
                        System.out.println("Associated languages not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getLocator(), checker.components[i].getLocator()))
                    {
                        System.out.println("Locators not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getName(), checker.components[i].getName()))
                    {
                        System.out.println("Names not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getServiceInformationType(),
                            checker.components[i].getServiceInformationType()))
                    {
                        System.out.println("ServiceInfomationType not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getStreamType(), checker.components[i].getStreamType()))
                    {
                        System.out.println("StreamType not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getStreamType(), checker.components[i].getStreamType()))
                    {
                        System.out.println("StreamType not equal");
                        matched = false;
                    }
                    if (!equalObjects(components[i].getUpdateTime(), checker.components[i].getUpdateTime()))
                    {
                        System.out.println("UpdateTime not equal");
                        matched = false;
                    }

                }
                siMatched = matched;
                if (siMatched)
                    System.out.println("The SI for the live service was the same as the recorded service");
                else
                    System.out.println("The SI for the live service was NOT the same as the recorded service");
            }

            private boolean equalObjects(Object a, Object b)
            {
                if (a != null)
                    System.out.print("Comparing: [" + a.toString() + "]");
                else
                    System.out.print("First arg null ");
                System.out.print(" to: ");
                if (b != null)
                    System.out.println(" [" + b.toString() + "]");
                else
                    System.out.println(" Second arg null ");
                if (((a == null) && (b != null)) || ((a != null && (b == null)))) return false;
                if (a != null && a.equals(b)) return true;
                return false;
            }

            public boolean siMatched()
            {
                return siMatched;
            }

            public void notifyFailure(SIRequestFailureType reason)
            {
                System.out.println(" - - - - SIFAILURE NOTIFICATION - - - - -" + reason);
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            if (e instanceof NormalContentEvent)
            {
                System.out.println("RecordedSIChecker getting the SI from the presenting service.");
            }
            System.out.println("RecordedSIChecker received ServiceContextEvent: " + e);
        }
    }

    /**
     * Selects a service context to stop presenting When triggered, this task
     * will alert the active Service context to go to the not presenting state.
     */
    class StopBroadcastService extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        StopBroadcastService(long triggerTime)
        {
            super(triggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                System.out.println("<<<<StopBroadcastService::ProcessCommand>>>>");
                m_serviceContext.addListener(this);
                m_serviceContext.stop();
            }
            catch (Exception e)
            {
                System.out.println("StopBroadcastService - Service setup failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "StopBroadcastService - Service setup failed, Exception: " + e.getMessage();
            }

        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("StopBroadcastService: ServiceContextEvent" + ev);
            m_serviceContext.removeListener(this);
        }

        private OcapLocator m_locator;
    }

    /**
     * Selects a service context to be detstoyed and resources to be deallocated
     * When triggered, this task will alert the active Service context to go to
     * the not presenting state and bedestroyed. Resources used should be
     * disposed and cleaned up
     */
    class DestroyBroadcastService extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        DestroyBroadcastService(long triggerTime)
        {
            super(triggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                System.out.println("<<<<DestroyBroadcastService::ProcessCommand>>>>");
                m_serviceContext.addListener(this);
                m_serviceContext.destroy();
            }
            catch (Exception e)
            {
                System.out.println("DestroyBroadcastService - Service setup failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "DestroyBroadcastService - Service setup failed, Exception: " + e.getMessage();
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("DestroyService: receiveServiceContextEvent" + ev);
        }

        private OcapLocator m_locator;
    }

    class SelectRecordedService extends EventScheduler.NotifyShell implements ServiceContextListener,
            ControllerListener
    {
        private long m_timeToDie;

        SelectRecordedService(String recording, long triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_timeToDie = 30000;
            m_ignoreEvent = true;
        }

        SelectRecordedService(OcapRecordingRequest rr, long triggerTime)
        {
            super(triggerTime);
            m_recording = null;
            m_rr = rr;
            m_timeToDie = 30000;
            m_ignoreEvent = true;
        }

        SelectRecordedService(String recording, long timeToDie, long triggerTime, boolean ignoreEvent)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_timeToDie = timeToDie;
            m_ignoreEvent = ignoreEvent;
        }

        public void ProcessCommand()
        {
            startPresenting();
            waitToDie();
            cleanServiceContext();
        }

        protected void startPresenting()
        {
            System.out.println("<<<<SelectRecordedService::ProcessCommand>>>>");
            OcapRecordingRequest rr = null;
            OcapRecordedService rsvc = null;

            try
            {
                ServiceContextFactory scf = ServiceContextFactory.getInstance();
                serviceContext = scf.createServiceContext();
                serviceContext.addListener(this);

                // were we given a recording list entry, or do we look it up?
                if (m_rr == null)
                    rr = (OcapRecordingRequest) findObject(m_recording);
                else
                    rr = m_rr;

                if (rr == null)
                {
                    System.out.println("SelectRecordedService - entry not found!" + m_recording);
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SelectRecordedService due to unfound recording: "
                            + m_recording);
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in SelectRecordedService due to unfound recording: "
                            + m_recording;
                    return;
                }

                rsvc = (OcapRecordedService) rr.getService();

                if (rsvc == null)
                {
                    System.out.println("SelectRecordedService - Service not found!" + m_recording);
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SelectRecordingService due to unfound service for: "
                            + m_recording);
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in SelectRecordingService due to unfound service for: "
                            + m_recording;
                    return;
                }
                printRecording(rr);
                System.out.println("Selecting Recorded Service\n");
                playStart = System.currentTimeMillis();
                serviceContext.select(rsvc);
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedService - Service selection failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedService - Service selection failed. Exception: " + e.getMessage();
            }
        }

        protected void waitToDie()
        {
            try
            {
                Thread.sleep(m_timeToDie);
            }
            catch (Exception e)
            {
                System.out.println("Thread does not sleep");
            }
        }

        protected void cleanServiceContext()
        {
            try
            {
                serviceContext.removeListener(this);
                serviceContext.destroy();
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedService - Unable to destroy service after 30 seconds");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedService - Unable to destroy service after 30 seconds. Exception: "
                        + e.getMessage();
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("SelectRecordedService: receiveServiceContextEvent" + ev);
            System.out.println(DvrEventPrinter.xletSCE(ev));
            if (ev instanceof PresentationTerminatedEvent)
            {
                if (!m_ignoreEvent) validAction(ev);

            }
            if (ev instanceof NormalContentEvent)
            {
                Player player = getServicePlayer(serviceContext);
                if (player == null)
                {
                    System.out.println("could not get Player for currently presenting Service");
                    return;
                }
                player.addControllerListener(this);
            }
        }

        public void controllerUpdate(ControllerEvent ev)
        {
            // TODO Auto-generated method stub
            if (ev instanceof EndOfContentEvent)
            {
                if (!m_ignoreEvent) validAction(ev);
            }
        }

        /**
         * @param ev
         */
        protected void validAction(EventObject ev)
        {
            // TODO Auto-generated method stub
            long duration = System.currentTimeMillis() - playStart;
            long minDuration = m_timeToDie - fudgeFactor;
            if (duration < minDuration)
            {
                System.out.println("SelectRecordedService - FAILED " + ev.toString());
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedService - FAILED" + ev.toString();
            }
        }

        protected long playStart = 0;

        protected boolean m_ignoreEvent;

        protected String m_recording;

        protected OcapRecordingRequest m_rr;

        protected ServiceContext serviceContext;
        /*
         * (non-Javadoc)
         * 
         * @seejavax.media.ControllerListener#controllerUpdate(javax.media.
         * ControllerEvent)
         */

    }

    class SelectRecordedServiceAsync extends SelectRecordedService
    {
        private final Vector listeners = new Vector();

        SelectRecordedServiceAsync(String recording, long triggerTime)
        {
            super(recording, triggerTime);
        }

        SelectRecordedServiceAsync(OcapRecordingRequest rr, long triggerTime)
        {
            super(rr, triggerTime);
        }

        SelectRecordedServiceAsync(String recording, long timeToDie, long triggerTime, boolean ignoreEvent)
        {
            super(recording, timeToDie, triggerTime, ignoreEvent);
        }

        // same as parent, but without the Thread.sleep() in it.
        // Instead of sleeping until the presentation is finished and then
        // cleaning up the service context, we just start playing and return.
        // When the presentation ends for any reason, we should get a
        // PresentationTerminatedEvent which we can handle and use to clean
        // up the service context.
        public void ProcessCommand()
        {
            startPresenting();

            // if we have any listeners queued up, add them to the service
            // context now that it exists.

            if (serviceContext != null)
            { // don't NPE if something went wrong
                for (int i = 0; i < listeners.size(); i++)
                {
                    ServiceContextListener scl = (ServiceContextListener) listeners.elementAt(i);

                    serviceContext.addListener(scl);
                }

                listeners.removeAllElements();
            }
        }

        // same as parent, except this catches PresentationTerminatedEvent
        // and takes the opportunity to clean up the service context.
        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("SelectRecordedService: receiveServiceContextEvent" + ev);
            System.out.println(DvrEventPrinter.xletSCE(ev));

            if (ev instanceof PresentationTerminatedEvent)
            {
                if (!m_ignoreEvent) validAction(ev);

            }

            if (ev instanceof NormalContentEvent)
            {
                Player player = getServicePlayer(serviceContext);
                if (player == null)
                {
                    System.out.println("could not get Player for currently presenting Service");
                    return;
                }
                player.addControllerListener(this);
            }

            if (ev instanceof PresentationTerminatedEvent)
            {
                cleanServiceContext();
            }
        }

        /**
         * Adds a ServiceContextListener to this task's ServiceContext. If the
         * ServiceContext has not yet been created, the listener will be stored
         * until the ServiceContext is created.
         * 
         * @param scl
         *            The listener to add.
         */
        public void addListener(ServiceContextListener scl)
        {
            if (serviceContext == null)
            {
                listeners.insertElementAt(scl, 0);
            }
            else
            {
                serviceContext.addListener(scl);
            }
        }

        /**
         * @return The service context we created and will manage until we have
         *         finished presenting.
         */
        public ServiceContext getServiceContext()
        {
            return serviceContext;
        }
    }

    class SelectRecordedServiceUsingJMFPlayer extends EventScheduler.NotifyShell implements ControllerListener,
            ComponentListener
    {
        SelectRecordedServiceUsingJMFPlayer(String recording, long triggerTime, HScene sc)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            scene = sc;
        }

        SelectRecordedServiceUsingJMFPlayer(OcapRecordingRequest rr, long triggerTime, HScene sc)
        {
            super(triggerTime);
            m_recording = null;
            m_rr = rr;
            scene = sc;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SelectRecordedServiceUsingJMFPlayer::ProcessCommand>>>>");

            OcapRecordingRequest rr;

            // were we given a recording list entry, or do we look it up?
            if (m_rr == null)
                rr = (OcapRecordingRequest) findObject(m_recording);
            else
                rr = m_rr;

            if (rr == null)
            {
                System.out.println("SelectRecordedServiceUsingJMFPlayer - entry not found!" + m_recording);
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer due to unfound recording: "
                        + m_recording);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer due to unfound recording: "
                        + m_recording;
                return;
            }

            OcapRecordedService rsvc = null;
            try
            {
                rsvc = (OcapRecordedService) rr.getService();
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedServiceUsingJMFPlayer - Exception obtaining service." + m_recording);
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedServiceUsingJMFPlayer - Exception obtaining service." + m_recording;
                e.printStackTrace();
            }
            if (rsvc == null)
            {
                System.out.println("SelectRecordedServiceUsingJMFPlayer - Service not found!" + m_recording);
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer due to failed getService() for "
                        + m_recording);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer due to failed getService() for "
                        + m_recording;
                return;
            }

            printRecording(rr);
            System.out.println("Selecting Recorded Service using JMF Player\n");

            if (state == UNREALIZED)
            {
                state = CREATING;
                try
                {
                    // Create the Player and wait for it to be realized.
                    System.out.println("\nMediaLocator: " + rsvc.getMediaLocator() + "\n");
                    player = Manager.createPlayer(rsvc.getMediaLocator());
                    System.out.println("\nplayer: " + player + "\n");
                    player.addControllerListener(this);
                    state = REALIZING;
                    player.realize();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in creating JMF player: " + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in creating JMF player: " + e.toString();

                }
            }
            /*
             * try{ Thread.sleep(40000); } catch(Exception e){
             * System.out.println("Thread not sleeping!"); } try{
             * System.out.println
             * ("SelectRecordedServiceUsingJMFPlayer - Player cleanup in progress"
             * ); player.stop(); } catch(Exception e){System.out.println(
             * "SelectRecordedServiceUsingJMFPlayer - Unable to destroy JMF player after 30 seconds"
             * ); e.printStackTrace(); m_failed = TEST_FAILED; m_failedReason =
             * "SelectRecordedServiceUsingJMFPlayer - Unable to destroy JMF player after 30 seconds. Exception: "
             * +e.getMessage(); }
             */

        }

        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("SelectRecordedServiceUsingJMFPlayer - controllerUpdate ( " + event + ")\n");

            synchronized (this)
            {
                if (event instanceof RealizeCompleteEvent)
                {
                    System.out.println("SelectRecordedServiceUsingJMFPlayer controllerUpdate - RealizeCompleteEvent\n");
/* TODO: VideoComponentControl is not a spec compliant class, remove its use
                    VideoComponentControl vidCtrl = (VideoComponentControl) player.getControl("org.ocap.media.VideoComponentControl");
                    video = (HVideoComponent) vidCtrl.getVisualComponent();
                    video.setVisible(false);
                    video.addComponentListener(this);
                    video.setBounds(getBounds());
                    System.out.println("add video: " + video);
                    scene.add(video);
                    video.setVisible(true);
*/
                    player.start();
                }
                else if (event instanceof StartEvent)
                {
                    System.out.println("SelectRecordedServiceUsingJMFPlayer controllerUpdate - StartEvent\n");
                    System.out.println("pop to front");
                    scene.popToFront(video);
                    System.out.println("setVisible\n");
                    video.setVisible(true);
                    System.out.println("<<<<<<<<<<Redrawing of the screen>>>>>>>>>>");
                    Rectangle bounds = video.getBounds();
                    scene.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
                }
                else if (event instanceof EndOfContentEvent)
                {
                    System.out.println("SelectRecordedServiceUsingJMFPlayer controllerUpdate -  StopEvent or End of Content event\n");
                    scene.remove(video);
                    System.out.println("<<<<<<<<<<Redrawing of the screen>>>>>>>>>>");
                    Rectangle bounds = video.getBounds();
                    scene.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
                    video = null;
                    player.stop();
                }
                else if (event instanceof StopEvent)
                {
                    player.close();
                    player.removeControllerListener(this);
                }
            }
        }

        public void componentShown(ComponentEvent e)
        {
            System.out.println("componentShown: " + e + "\n");

            synchronized (this)
            {
                // After receiving the PaintEvent, start the decoder and repaint
                // to change from opaque to transparent to PIP video underneath.
                if (e.getID() == ComponentEvent.COMPONENT_SHOWN)
                {
                    player.start();
                }
            }
        }

        public void componentResized(ComponentEvent e)
        {
            System.out.println("SelectRecordedServiceUsingJMFPlayer componentResized...\n");
            System.out.println("componentResized: " + e + "\n");
        }

        public void componentMoved(ComponentEvent e)
        {
            System.out.println("componentMoved: " + e + "\n");
        }

        public void componentHidden(ComponentEvent e)
        {
            System.out.println("SelectRecordedServiceUsingJMFPlayer componentHidden...\n");
            System.out.println("componentHidden: " + e + "\n");
        }

        private Rectangle getBounds()
        {
            return BOUNDS[0];
        }

        public class State
        {
            String name;

            State(String name)
            {
                this.name = name;
            }

            public String toString()
            {
                return name;
            }
        };

        private String m_recording;

        private OcapRecordingRequest m_rr;

        // the Player that will present the video
        private Player player;

        // component player is added to this scene
        private HScene scene;

        private boolean visible;

        // the AWT component that contains the player
        private HVideoComponent video;

        private Rectangle BOUNDS[] = { new Rectangle(340, 260, 240, 180), };

        public final State UNREALIZED = new State("UNREALIZED");

        public final State CREATING = new State("CREATING");

        public final State REALIZING = new State("REALIZING");

        // current state -- start at UNREALIZED
        private State state = UNREALIZED;
    }

    class SetMediaTimeBack extends EventScheduler.NotifyShell implements ControllerListener
    {

        private double m_jog;

        private String m_scName;

        private boolean m_ignoreEvent;

        private boolean m_jogBack;

        private boolean m_paused;

        /**
         * @param time
         *            in milisecodns
         * @param jogBack
         *            in seconds contructor in Time object in miliseconds
         */
        SetMediaTimeBack(double jog, long time)
        {
            super(time);
            m_jog = jog;
            m_ignoreEvent = false;
            m_scName = "";
            m_jogBack = true;
            m_paused = false;
        }

        SetMediaTimeBack(double jog, boolean ignoreEvent, long time)
        {
            super(time);
            m_jog = jog;
            m_ignoreEvent = ignoreEvent;
            m_scName = "";
            m_jogBack = true;
            m_paused = false;
        }

        SetMediaTimeBack(String scName, double jog, boolean ignoreEvent, boolean jogBack, boolean paused, long time)
        {
            super(time);
            m_jog = jog;
            m_ignoreEvent = ignoreEvent;
            m_scName = scName;
            m_jogBack = jogBack;
            m_paused = paused;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SetMediaTimeBack::ProcessCommand>>>>");
            if (!m_ignoreEvent)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("<<<<SetMediaTimeBack::Setting to FAILED unless MediaTimeSetEvent occurs");
                m_failedReason = "<<<<SetMediaTimeBack::Setting to FAILED unless MediaTimeSetEvent occurs";
            }
            try
            {
                // Use the default Service Context
                if (m_scName != "")
                {
                    // Or find the object in the hashtable
                    m_serviceContext = (ServiceContext) findObject(m_scName);
                }
                // Get the Player
                Player player = getServicePlayer(m_serviceContext);
                player.addControllerListener(this);
                if (player == null)
                {
                    System.out.println("Can't get Player");
                    m_failedReason = "Can't get Player";
                    m_failed = TEST_FAILED;
                    return;
                }
                // Get the current media time from the SC
                double secs = player.getMediaTime().getSeconds();
                double newsecs = 0;
                // Reset the current playback to the given time
                System.out.println("jog step size in secs " + m_jog);
                if (m_jogBack)
                {
                    newsecs = secs - m_jog;
                }
                else
                {
                    newsecs = secs + m_jog;
                }
                System.out.println("jog back secs " + secs + " newsecs " + newsecs);
                player.setMediaTime(new Time(newsecs));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                DvrTestMonAppXlet.log("SetMediaTimeBack - Exception thrown in processing command");
                m_failedReason = "SetMediaTimeBack - Exception thrown in processing command" + e;
                m_failed = TEST_FAILED;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.media.ControllerListener#controllerUpdate(javax.media.
         * ControllerEvent)
         */
        public void controllerUpdate(ControllerEvent event)
        {
            if (event instanceof MediaTimeSetEvent)
            {
                DvrTestMonAppXlet.log("Received MediaTimeSetEventEvent");
                Player player = getServicePlayer(m_serviceContext);
                float rate = player.getRate();
                if (rate != 1)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("Received MediaTimeSetEventEvent Rate not 1.0");
                    m_failedReason = "<<<<Received MediaTimeSetEventEvent Rate not 1.0";
                }
                if (!m_ignoreEvent)
                {
                    m_failed = TEST_PASSED;
                    DvrTestMonAppXlet.log("Received MediaTimeSetEventEvent PASSED");
                }
            }
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * Sets the media time on the recorded service and plays back the recorded
     * service
     * 
     * @params String recording: String name of the referenced recording
     * request. The referrence is stored away in a vector paired list
     * 
     * @params String SCName:String name for the
     * 
     * @params double seconds:
     * 
     * @params long timeToDie:
     * 
     * @params long triggerTime:
     * 
     * @params boolean ignoreEvent:
     */
    class SetMediaTimeTest extends EventScheduler.NotifyShell implements ServiceContextListener, ControllerListener
    {

        SetMediaTimeTest(String recording, long triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_seconds = 10;
            m_ignoreEvent = true;
            m_timeToDie = 30000;
            SvcCtx = null;
        }

        SetMediaTimeTest(OcapRecordingRequest rr, long triggerTime)
        {
            super(triggerTime);
            m_recording = null;
            m_rr = rr;
            m_seconds = 10;
            m_ignoreEvent = true;
            m_timeToDie = 30000;
            SvcCtx = null;
        }

        SetMediaTimeTest(String recording, double seconds, long triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_seconds = seconds;
            m_ignoreEvent = true;
            m_timeToDie = 30000;
            SvcCtx = null;
        }

        SetMediaTimeTest(String recording, String SCName, double seconds, long timeToDie, long triggerTime,
                boolean ignoreEvent)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            m_seconds = seconds;
            m_ignoreEvent = ignoreEvent;
            m_timeToDie = timeToDie;
            SvcCtx = SCName;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SetMediaTimeTest::ProcessCommand>>>>");
            m_checkTime = false; // set the falg for checking the Media Time
            OcapRecordingRequest rr = null;
            OcapRecordedService rsvc = null;
            ServiceContext serviceContext;
            if (SvcCtx != null)
            {
                serviceContext = (ServiceContext) findObject(SvcCtx);
            }
            else
            {
                try
                {
                    ServiceContextFactory scf = ServiceContextFactory.getInstance();
                    serviceContext = scf.createServiceContext();
                    insertObject(serviceContext, "RecSvc");
                }
                catch (Exception e)
                {
                    System.out.println("SetMediaTimeTest - createServiceContext failed!");
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to exception in Service Context creation: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to exception in Service Context creation: "
                            + e.toString();
                    e.printStackTrace();
                    return;
                }
            }
            try
            {
                serviceContext.addListener(this);

                // were we given a recording list entry, or do we look it up?
                if (m_rr == null)
                    rr = (OcapRecordingRequest) findObject(m_recording);
                else
                    rr = m_rr;

                if (rr == null)
                {
                    System.out.println("SetMediaTimeTest - entry not found!" + m_recording);
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to unfound recording "
                            + m_recording);
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to unfound recording "
                            + m_recording;
                    return;
                }
                rsvc = (OcapRecordedService) rr.getService();
                if (rsvc == null)
                {
                    System.out.println("SetMediaTimeTest - Service not found!" + m_recording);
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to rr.getService() failure");
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to rr.getService() failure";
                    return;
                }
            }
            catch (Exception e)
            {
                System.out.println("SetMediaTimeTest - Exception obtaining service." + m_recording);
                e.printStackTrace();
            }

            Time mediaTime = new Time(m_seconds);
            printRecording(rr);
            System.out.println("SetMediaTimeTest mediaTime: " + m_seconds + "\n");
            try
            {
                rsvc.setMediaTime(mediaTime);
            }
            catch (Exception e)
            {
                System.out.println("SetMediaTimeTest - createServiceContext failed!");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in SetMediaTimeTest due to rsvc.setMediaTime() exception: "
                        + e.toString());
                m_failedReason = "Flagged FAILURE in SetMediaTimeTest due to rsvc.setMediaTime() exception: "
                        + e.toString();
                e.printStackTrace();
                return;
            }
            try
            {
                serviceContext.addListener(this);
                serviceContext.select(rsvc);
            }
            catch (Exception e)
            {
                System.out.println("SetMediaTime - Service selection failed");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "SetMediaTime - Service selection failed. Exception: " + e.getMessage();
            }
            if (SvcCtx == null)
            {
                try
                {
                    Thread.sleep(m_timeToDie);
                }
                catch (Exception e)
                {
                    System.out.println("Thread does not sleep");
                }
                try
                {
                    removeObject("RecSvc");
                    serviceContext.removeListener(this);
                    serviceContext.destroy();
                }
                catch (Exception e)
                {
                    System.out.println("SetMediaTimeTest - Unable to destroy service after 30 seconds");
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    m_failedReason = "SetMediaTimeTest - Unable to destroy service after 30 seconds. Exception: "
                            + e.getMessage();
                }
            }
        }

        public void receiveServiceContextEvent(ServiceContextEvent event)
        {
            System.out.println("SetMediaTimeTest: receiveServiceContextEvent" + event);

            synchronized (this)
            {
                if (event instanceof NormalContentEvent)
                {
                    System.out.println("receiveServiceContextEvent - NormalContentEvent\n");

                    ServiceContentHandler[] schArray;
                    ServiceContext sc = event.getServiceContext();

                    if (sc == null) return;

                    schArray = sc.getServiceContentHandlers();
                    System.out.println("ServiceContext returned " + schArray.length + " handlers\n");

                    if (schArray.length == 0) return;

                    if (schArray[0] != null && schArray[0] instanceof Player)
                    {
                        Player player = (Player) schArray[0];
                        Time mtime = player.getMediaTime();
                        double sec1 = mtime.getSeconds();
                        System.out.println("\nget Player media time: " + sec1 + "\n");
                        if (!m_checkTime)
                        {
                            player.addControllerListener(this);
                            if ((sec1 < m_seconds) || (sec1 > (m_seconds + 10.0)))
                            {
                                DvrTestMonAppXlet.log("SetMediaTimeTest - SetMediaTime out of bounds");
                                m_failed = TEST_FAILED;
                                m_failedReason = "SetMediaTimeTest - SetMediaTime out of bounds";
                            }
                            m_checkTime = true;
                        }
                    }
                }
                if (event instanceof SelectionFailedEvent)
                {
                    System.out.println("receiveServiceContextEvent - SelectionFailedEvent)\n");
                    DvrTestMonAppXlet.log("SetMediaTimeTest - SelectionFailedEvent received");
                    m_failed = TEST_FAILED;
                    m_failedReason = "SetMediaTimeTest - SelectionFailedEvent received";
                }
                if (event instanceof PresentationTerminatedEvent)
                {
                    if (!m_ignoreEvent) validAction(event);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.media.ControllerListener#controllerUpdate(javax.media.
         * ControllerEvent)
         */
        public void controllerUpdate(ControllerEvent event)
        {
            // TODO Auto-generated method stub
            if (event instanceof EndOfContentEvent)
            {
                if (!m_ignoreEvent) validAction(event);
            }
        }

        private void validAction(EventObject ev)
        {
            // TODO Auto-generated method stub
            long duration = System.currentTimeMillis() - playStart;
            long minDuration = m_timeToDie - fudgeFactor;
            if (duration < minDuration)
            {
                System.out.println("SelectRecordedService - FAILED " + ev.toString());
                m_failed = TEST_FAILED;
                m_failedReason = "SelectRecordedService - FAILED" + ev.toString();
            }
        }

        private String SvcCtx;

        private long playStart = 0;

        private String m_recording;

        private OcapRecordingRequest m_rr;

        private double m_seconds;

        private boolean m_ignoreEvent;

        private boolean m_checkTime;

        private long m_timeToDie;
    }

    class SetRate extends EventScheduler.NotifyShell implements ControllerListener
    {
        /**
         * @param time
         */

        SetRate(String scName, double rate, boolean rateWorked, long time)
        {
            super(time);
            m_rate = rate;
            m_scName = scName;
            m_rateWorked = rateWorked;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            if (m_rateWorked)
            {
                m_failed = TEST_FAILED;
            }
            Player player = null;
            ServiceContext sc = null;
            try
            {
                if (m_scName != "")
                    sc = (ServiceContext) findObject(m_scName);
                else
                {
                    sc = m_serviceContext;
                }
                ServiceContentHandler[] handlers = sc.getServiceContentHandlers();

                for (int i = 0; i < handlers.length; ++i)
                {
                    ServiceContentHandler handler = handlers[i];
                    System.out.println("check handler " + handler);
                    if (handler instanceof Player)
                    {
                        System.out.println("found player " + handler + " for context " + sc);
                        player = (Player) handler;
                    }
                }
                if (player == null)
                {
                    System.out.println("could not get Player for currently presenting Service");
                    return;
                }

                player.removeControllerListener(this);
                player.addControllerListener(this);
                player.setRate((float) m_rate);
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<SetRate: Exception thrown on retrieveing player>>>");
                m_failedReason = "SetRate: Exception thrown on retrieveing player " + e.toString();
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.media.ControllerListener#controllerUpdate(javax.media.
         * ControllerEvent)
         */
        public void controllerUpdate(ControllerEvent event)
        {
            // TODO Auto-generated method stub
            if (event instanceof RateChangeEvent)
            {
                RateChangeEvent rateChange = (RateChangeEvent) event;
                float newRate = (float) (Math.floor((rateChange.getRate()) * 1000.0 + 0.5) / 1000.0);
                float setRate = (float) m_rate;
                System.out.println("Received RateChangeEvent to " + newRate);
                if (newRate == setRate)
                {
                    m_failed = TEST_PASSED;
                    System.out.println("Rate properly set in the stack");
                }
                else
                {
                    DvrTestMonAppXlet.log("SetRate : Rate was not properly set in the stack : got=" + newRate
                            + " sent=" + setRate);
                }
            }
            if (event instanceof LeavingLiveModeEvent)
            {
                if (m_rateWorked)
                {
                    System.out.println("PASSED Received LeavingLiveModeEvent: OK");
                    m_failed = TEST_PASSED;
                }
                else
                {
                    System.out.println("<<<FAILED Received LeavingLiveModeEvent: TimeSiftBuffering present>>>");
                    m_failedReason = "FAILED Received LeavingLiveModeEvent: TimeSiftBuffering present ";
                    m_failed = TEST_FAILED;
                }
            }
        }

        private boolean m_rateWorked;

        private String m_scName;

        private double m_rate;
    }

    /**
     * 
     * @author jspruiel
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    class AddAppData extends EventScheduler.NotifyShell
    {
        AddAppData(String rec, long taskTriggerTime, Serializable appData)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_appData = appData;
            m_key = "jas";
        }

        AddAppData(String rec, String key, String data, int triggerTime)
        {
            super(triggerTime);
            m_rName = rec;
            m_key = key;
            m_appData = data;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<AddAppData::ProcessCommand>>>>");
            RecordingRequest rr = (RecordingRequest) findObject(m_rName);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                System.out.println("DVRUtils: AddAppData recording - recording not found! " + m_rName);
                return;
            }

            try
            {

                System.out.println("<<<<AddAppData::ProcessCommand>>>> create PersonInfo");
                rr.addAppData(m_key, (Serializable) m_appData);
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("DvrMonAppTest: Exception on AddAppData" + e);
                e.printStackTrace();
            }
        }

        private String m_key;

        protected String m_rName;

        protected Serializable m_appData;
    }

    /**
     * Walk the recording list, and confirm that there are the expected number
     * of recordings in the IN_PROGRESS state
     */
    class ConfirmRecordingStateCount extends EventScheduler.NotifyShell
    {
        ConfirmRecordingStateCount(int count, int state, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingCount = count;
            m_state = state;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<ConfirmRecordingStateCount::ProcessCommand>>>>");

            int count = countRecordingsByState(m_state);

            if (count != m_recordingCount)
            {
                System.out.println("<<ConfirmRecordingStateCount>> FAILED: Expected " + m_recordingCount + " found "
                        + count);
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmRecordingStateCount due to incorrect count: Expected "
                        + m_recordingCount + " found " + count);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmRecordingStateCount due to incorrect count: Expected "
                        + m_recordingCount + " found " + count;
            }

        }

        private int m_recordingCount;

        private int m_state;
    }

    /**
     * Walk the recording list, and confirm that the specific recording is in
     * the proper state passed to it
     */
    class ConfirmRecordingReq_CheckState extends EventScheduler.NotifyShell
    {
        ConfirmRecordingReq_CheckState(String rec, int state, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = rec;
            m_state = state;
        }

        ConfirmRecordingReq_CheckState(Logger log, String rec, int state, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = rec;
            m_state = state;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<ConfirmRecordingReq_CheckState::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<ConfirmRecordingReq_CheckState::ProcessCommand>>>>");
            }

            try
            {
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);
                m_current_state = rr.getState();
                if (m_current_state == m_state)
                {
                    if (m_log != null)
                    {
                        m_log.debug("<<<<ConfirmRecordingReq_CheckState::PASSED " + m_recording + ">>>>");
                    }
                    else
                    {
                        System.out.println("<<<<ConfirmRecordingReq_CheckState::PASSED " + m_recording + ">>>>");
                        // Added by bforan for debug purposes
                        System.out.println("<<<<" + m_recording + " is in "
                                + DvrEventPrinter.xletState(m_current_state, rr) + ">>>>");
                    }
                }
                else
                {
                    m_failed = TEST_FAILED;
                    if (m_log != null)
                    {
                        m_log.debug("<<<<ConfirmRecordingReq_CheckState::FAILED " + m_recording + ">>>>");
                        m_log.debug("<<<<" + m_recording + " is in " + DvrEventPrinter.xletState(m_current_state, rr)
                                + ">>>>");
                        m_log.debug("<<<<" + m_recording + " should be in " + DvrEventPrinter.xletState(m_state, rr)
                                + ">>>>");

                    }
                    else
                    {
                        System.out.println("<<<<ConfirmRecordingReq_CheckState::FAILED " + m_recording + ">>>>");
                        System.out.println("<<<<" + m_recording + " is in "
                                + DvrEventPrinter.xletState(m_current_state, rr) + ">>>>");
                        System.out.println("<<<<" + m_recording + " should be in "
                                + DvrEventPrinter.xletState(m_state, rr) + ">>>>");

                    }
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckState due to state mismatch for "
                            + m_recording
                            + ": expected "
                            + DvrEventPrinter.xletState(m_state, rr)
                            + ", found "
                            + DvrEventPrinter.xletState(m_current_state, rr));
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckState due to state mismatch for "
                            + m_recording
                            + ": expected "
                            + DvrEventPrinter.xletState(m_state, rr)
                            + ", found "
                            + DvrEventPrinter.xletState(m_current_state, rr);
                }
            }
            catch (Exception e)
            {
                System.out.println("Parent or corrupt Recording Request");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckState due to exception while accessing recording "
                        + m_recording + ": " + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckState due to exception while accessing recording "
                        + m_recording + ": " + e.toString();

                e.printStackTrace();
            }
        }

        private int m_state;

        private int m_current_state;

        private String m_recording;

        private Logger m_log;

    }

    /**
     * Walk the recording list, and confirm that the specific recording is in
     * the proper state passed to it
     */
    class ConfirmRecordingReq_CheckStateOneOf extends EventScheduler.NotifyShell
    {
        ConfirmRecordingReq_CheckStateOneOf(String rec, int[] state, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = rec;
            m_state = state;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<ConfirmRecordingReq_CheckStateOneOf::ProcessCommand>>>>");

            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);
                m_current_state = rr.getState();
                System.out.println("<<<<ConfirmRecordingReq_CheckStateOneOf: curr state = "
                        + DvrEventPrinter.xletState(m_current_state, rr));

                int i = 0;
                for (i = 0; i < m_state.length; i++)
                {
                    if (m_current_state == m_state[i])
                    {
                        System.out.println("<<<<ConfirmRecordingReq_CheckStateOneOf::PASSED " + m_recording + ">>>>");
                        return;
                    }
                }

                m_failed = TEST_FAILED;
                System.out.println("<<<<ConfirmRecordingReq_CheckStateOneOf::FAILED " + m_recording + ">>>>");
                System.out.println("<<<<" + m_recording + " is in " + DvrEventPrinter.xletState(m_current_state, rr)
                        + ">>>>");
                System.out.println("<<<<" + m_recording + " should be in " + DvrEventPrinter.xletState(m_state[i], rr)
                        + ">>>>");
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckStateOneOf due to state mismatch for "
                        + m_recording
                        + ": expected "
                        + DvrEventPrinter.xletState(m_state[i], rr)
                        + ", found "
                        + DvrEventPrinter.xletState(m_current_state, rr));
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckStateOneOf due to state mismatch for "
                        + m_recording
                        + ": expected "
                        + DvrEventPrinter.xletState(m_state[i], rr)
                        + ", found "
                        + DvrEventPrinter.xletState(m_current_state, rr);
            }
            catch (Exception e)
            {
                System.out.println("Parent or corrupt Recording Request");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckStateOneOf due to exception while accessing recording "
                        + m_recording + ": " + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in ConfirmRecordingReq_CheckStateOneOf due to exception while accessing recording "
                        + m_recording + ": " + e.toString();

                e.printStackTrace();
            }
        }

        private int[] m_state;

        private int m_current_state;

        private String m_recording;
    }

    /*
     * 
     * @author Ryan
     * 
     * Checks the Recording Failed Exception for the correct value posted passed
     * into the constructor.
     */
    class CheckFailedException extends EventScheduler.NotifyShell
    {
        private int m_reason;

        private String m_recordingName;

        CheckFailedException(String recordingName, int reason, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_reason = reason;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<CheckFailedException::ProcessCommand>>>>");
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);
                RecordingFailedException exception = (RecordingFailedException) rr.getFailedException();
                int reasonCode = exception.getReason();
                if (reasonCode != m_reason)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckFailedException due to state mismatch");
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckFailedException due to state mismatch ";
                    DvrTestMonAppXlet.log("Reason code is " + DvrEventPrinter.xletRecFailedRsn(reasonCode)
                            + "should be " + DvrEventPrinter.xletRecFailedRsn(m_reason));
                    m_failedReason = "Reason code is " + DvrEventPrinter.xletRecFailedRsn(reasonCode) + "should be "
                            + DvrEventPrinter.xletRecFailedRsn(m_reason);
                }
                else
                {
                    DvrTestMonAppXlet.log("Reason code is " + DvrEventPrinter.xletRecFailedRsn(reasonCode));
                    DvrTestMonAppXlet.log("!!!!Reason code is correct!!!");
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: CheckFailedException: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckFailedException due to exception while setting up recording: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckFailedException due to exception while setting up recording: "
                        + e.toString();
            }
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Checks the associated recorded services with the given recording
     * requests. This object can check whether or not the recording is segmented
     * and if it contatins the specified amout of segmentes passsed into the
     * constructor.
     */
    class CheckRecordedServices extends EventScheduler.NotifyShell
    {
        private String m_recordingName;

        private boolean m_segmented;

        private int m_segments;

        CheckRecordedServices(String recordingName, boolean segmented, int segments, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingName = recordingName;
            m_segmented = segmented;
            m_segments = segments;
        }

        public void ProcessCommand()
        {
            try
            {
                System.out.println("<<<<CheckRecordedServices::ProcessCommand>>>>");
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);
                RecordedService rs = (OcapRecordedService) rr.getService();
                if (rs instanceof SegmentedRecordedService)
                {
                    if (m_segmented)
                    {
                        int segs = ((SegmentedRecordedService) rs).getSegments().length;
                        if (m_segments != segs)
                        {
                            System.out.println("DVRUtils: CheckRecordedServices: FAILED");
                            m_failed = TEST_FAILED;
                            DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckRecordedServices; mismatch - should be "
                                    + m_segments + " returned " + segs);
                            m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckRecordedServices; mismatch - should be "
                                    + m_segments + " returned " + segs;
                        }
                        else
                        {
                            DvrTestMonAppXlet.log("DvrMonAppTest: CheckRecordedServices: PASSED - number of segments match");
                        }
                    }
                    else
                    {
                        System.out.println("DVRUtils: CheckRecordedServices: FAILED");
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to segmneted recording ");
                        m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to segmneted recording ";
                    }
                }
                else
                {
                    if (m_segmented)
                    {
                        System.out.println("DVRUtils: CheckRecordedServices: FAILED");
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to NON segmneted recording ");
                        m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to NON segmneted recording ";
                    }
                    else
                    {
                        System.out.println("DVRUtils: CheckRecordedServices: PASSED - recording not a segment");
                    }
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: CheckRecordedServices: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to exception while setting up recording: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckRecordedServices due to exception while setting up recording: "
                        + e.toString();
            }
        }
    }

    /**
     * Schedules a future call to schedule a recording At the task's trigger
     * time, a call to RecordingManager.record will be made with the parameters
     * specified. The resulting recording will placed in the test's recording
     * map.
     */
    class RecordBySC extends EventScheduler.NotifyShell
    {
        private byte m_recordingPriority;

        private int m_retentionPriority;

        RecordBySC(String recordingName, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24;
            m_recordingName = recordingName;
            m_scName = "";
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        RecordBySC(String recordingName, String scName, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24;
            m_recordingName = recordingName;
            m_scName = scName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
            m_recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        }

        RecordBySC(String recordingName, long startTime, long duration, long taskTriggerTime, int retentionPriority,
                byte recordingPriority)
        {
            super(taskTriggerTime);
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24;
            m_recordingName = recordingName;
            m_scName = "";
            m_recordingPriority = recordingPriority;
            m_retentionPriority = retentionPriority;
        }

        // TODO Auto-generated constructor stub
        public void ProcessCommand()
        {
            System.out.println("<<<<RecordBySC::ProcessCommand>>>>");
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                OcapRecordingRequest rr = null;
                if (m_startTime == 0) m_startTime = System.currentTimeMillis();

                System.out.println("DVRUtils: issueing recording:" + m_serviceContext + " StarTime:" + m_startTime
                        + " Duration:" + m_duration);

                OcapRecordingProperties orp;
                ServiceContextRecordingSpec scrs;

                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration,
                        m_retentionPriority, m_recordingPriority, null, null, null);
                if (m_scName != "")
                {
                    // Or find the object in the hashtable
                    m_serviceContext = (ServiceContext) findObject(m_scName);
                }
                scrs = new ServiceContextRecordingSpec(m_serviceContext, new Date(m_startTime), m_duration, orp);
                try
                {
                    rr = (OcapRecordingRequest) rm.record(scrs);
                }
                catch (Exception e)
                {
                    System.out.println("RecordBySC - Exception on record()");
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in RecordBySC due to rm.record() exception: "
                            + e.toString());
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in RecordBySC due to rm.record() exception: "
                            + e.toString();
                }
                if (rr != null)
                {
                    insertObject(rr, m_recordingName);
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: RecordBySC: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in RecordBySC due to exception while setting up recording: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in RecordBySC due to exception while setting up recording: "
                        + e.toString();
            }

        }

        private String m_scName;

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;
    }

    /**
     * Counts all recordings in the nav db whose state matches those passed in.
     * 
     * @param state
     *            state to check against
     * 
     * @return number of recordings found
     */
    int countRecordingsByState(int state)
    {
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        RecordingList rl = rm.getEntries();

        int count = 0;

        if (rl == null)
        {
            System.out.println("<<countCompletedRecordings>> Recording list null. returning 0.");
            m_failed = TEST_FAILED;
            DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in countCompletedRecordings due to null return from rm.getEntries()");
            m_failedReason = "Flagged FAILURE in countCompletedRecordings due to null return from rm.getEntries()";
            return 0;
        }

        for (int x = 0; x < rl.size(); x++)
        {
            RecordingRequest rr = (RecordingRequest) rl.getRecordingRequest(x);
            if (rr.getState() == state)
            {
                count++;
            }
        }
        System.out.println("<<countRecordingsByState>>" + state + " : returning " + count + ".");
        return count;
    }

    class FilterCompleted extends EventScheduler.NotifyShell
    {
        FilterCompleted(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<FilterCompleted::ProcessCommand>>>>");
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList rlist = rm.getEntries(new RecordingStateFilter(LeafRecordingRequest.COMPLETED_STATE));
            if (rlist == null)
            {
                System.out.println("DVRUtils: FilterCompleted recordings - returned null! ");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in FilterCompleted due to null return from rm.getEntries()");
                m_failedReason = "Flagged FAILURE in FilterCompleted due to null return from rm.getEntries()";
                return;
            }

            if (rlist.size() == 0)
            {
                System.out.println("DVRUtils: FilterCompleted recordings - returned zero recordings! ");
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in FilterCompleted due to zero-sized list from rm.getEntries()");
                m_failedReason = "Flagged FAILURE in FilterCompleted due to zero-sized list from rm.getEntries()";
                return;
            }

            // ensure each recording returned is in the expected state.
            for (int i = 0; i < rlist.size(); i++)
            {
                RecordingRequest rr = rlist.getRecordingRequest(i);
                if (rr.getState() != LeafRecordingRequest.COMPLETED_STATE)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils: FilterCompleted recordings - one of the returned recordings have incorrected state! ");
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in FilterCompleted due to recording in non-completed state: "
                            + DvrEventPrinter.xletState(rr.getState(), rr));
                    m_failedReason = "DvrMonAppTest: Flagged FAILURE in FilterCompleted due to recording in non-completed state: "
                            + DvrEventPrinter.xletState(rr.getState(), rr);
                    return;
                }
            }
        }
    }

    class FilterPendingNoConflict extends EventScheduler.NotifyShell
    {
        FilterPendingNoConflict(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<FilterPendingNoConflict::ProcessCommand>>>>");
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList rlist = rm.getEntries(new RecordingStateFilter(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE));
            if (rlist == null)
            {
                System.out.println("DVRUtils: FilterPendingNoConflict recordings - returned null! ");
                m_failed = TEST_FAILED;
                m_failedReason = "FilterPendingNoConflict recordings - returned null!";
                return;
            }

            if (rlist.size() == 0)
            {
                System.out.println("DVRUtils: FilterPendingNoConflict recordings - returned zero recordings! ");
                m_failed = TEST_FAILED;
                m_failedReason = "FilterPendingNoConflict recordings - returned zero recordings!";
                return;
            }

            // ensure each recording returned is in the expected state.
            for (int i = 0; i < rlist.size(); i++)
            {
                RecordingRequest rr = rlist.getRecordingRequest(i);
                if (rr.getState() != LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils: FilterPendingNoConflict recordings - one of the returned recordings have incorrected state! ");
                    m_failedReason = "FilterPendingNoConflict recordings - one of the returned recordings have incorrected state!";
                    return;
                }
            }
        }
    }

    class SortStartTime extends EventScheduler.NotifyShell
    {
        class Comparator implements RecordingListComparator
        {
            // sort by start time
            public int compare(RecordingRequest first, RecordingRequest second)
            {
                long f = 0, s = 0;
                if ((first.getRecordingSpec() instanceof LocatorRecordingSpec)
                        && (second.getRecordingSpec() instanceof LocatorRecordingSpec))
                {
                    f = ((LocatorRecordingSpec) first.getRecordingSpec()).getStartTime().getTime();
                    s = ((LocatorRecordingSpec) second.getRecordingSpec()).getStartTime().getTime();
                }

                if (f > s)
                {
                    // put second arg after first
                    return 1;
                }
                else if (f < s)
                {
                    // put first arg after second
                    return -1;
                }
                // retain the ordering.
                return 0;
            }
        }

        SortStartTime(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                System.out.println("<<<<SortStartTime::ProcessCommand>>>>");
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                RecordingList rlist = rm.getEntries();
                if (rlist == null)
                {
                    System.out.println("DVRUtils: SortStartTime recordings - returned null! ");
                    m_failed = TEST_FAILED;
                    m_failedReason = "SortStartTime recordings - returned null!";
                    return;
                }

                if (rlist.size() == 0)
                {
                    System.out.println("DVRUtils: SortStartTime recordings - returned zero recordings! ");
                    m_failed = TEST_FAILED;
                    m_failedReason = "SortStartTime recordings - returned zero recordings!";
                    return;
                }

                RecordingList newList = rlist.sortRecordingList(new Comparator());

                long prev = 0;
                long curr;
                // validate results visually
                for (int i = 0; i < rlist.size(); i++)
                {
                    curr = ((LocatorRecordingSpec) newList.getRecordingRequest(i).getRecordingSpec()).getStartTime()
                            .getTime();
                    System.out.println("DVRUtils: start time for record[" + i + "]  = " + curr);
                }

                // validate results programmatically
                for (int i = 0; i < rlist.size(); i++)
                {
                    curr = ((LocatorRecordingSpec) newList.getRecordingRequest(i).getRecordingSpec()).getStartTime()
                            .getTime();
                    if (curr >= prev)
                    {
                        prev = curr;
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        System.out.println("DVRUtils: SortStartTime recordings - one of the returned recordings have incorrected state! ");
                        m_failedReason = "SortStartTime recordings - one of the returned recordings have incorrected state!";
                        return;
                    }
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Exception thrown in SortStartTime ");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "Exception thrown in SortStartTime. Exception: " + e.getMessage();
                return;
            }
        }
    }

    /**
     * The default resource contention handler will prioritize the resource
     * usages lexicographically based upon the name associated DvrTest
     * Recording/ServiceContext/ResourceUsage.
     * 
     * @author craigp
     * 
     */
    protected class DefaultResourceContentionHandler implements org.ocap.resource.ResourceContentionHandler
    {
        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            DvrTestMonAppXlet.log("! Setting test to PASSED state - RCH is envoked !");
            // m_failed = TEST_PASSED;

            int i, j;

            DvrTestMonAppXlet.log("*** Default DvrMonAppTest contention handler called with:");

            DvrTestMonAppXlet.log("***  newRequest: " + getNameForResourceUsage(newRequest) + " (" + newRequest + ")");

            for (i = 0; i < currentReservations.length; i++)
            {
                DvrTestMonAppXlet.log("***  curRes[" + i + "]: " + getNameForResourceUsage(currentReservations[i])
                        + " (" + currentReservations[i] + ")");
            }

            ResourceUsage[] neworder = new ResourceUsage[currentReservations.length + 1];

            //
            // Perform insertion sort of each element
            //
            neworder[0] = newRequest;

            for (i = 0; i < currentReservations.length; i++)
            {
                String resKeyToInsert = getNameForResourceUsage(currentReservations[i]);

                for (j = i + 1; j > 0; j--)
                {
                    String sortResKey = getNameForResourceUsage(neworder[j - 1]);

                    if (resKeyToInsert != null && sortResKey != null)
                    {
                        if ((resKeyToInsert.compareTo(sortResKey) >= 0))
                        { // Stop - we hit the top or the entry below us is >=
                          // resToInsert
                            break;
                        }
                    }

                    // Assert: key for neworder[j] <= key for
                    // currentReservations[i]
                    // Move up the current entry - making a hole for the next
                    // interation
                    neworder[j] = neworder[j - 1];
                } // END for (j)

                // j will have stopped at the right place or hit the top
                // Either way, put it where it needs to go
                neworder[j] = currentReservations[i];
            } // END for (i)

            // Assert: neworder is sorted by key value (string name)

            DvrTestMonAppXlet.log("*** Default DvrMonAppTest contention handler returning prioritized list:");
            for (i = 0; i < neworder.length; i++)
            {
                DvrTestMonAppXlet.log("***  neworder[" + i + "]: " + getNameForResourceUsage(neworder[i]) + " ("
                        + neworder[i] + ")");
            }

            return neworder;
        } // END resolveResourceContention()

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            m_counter++;
            DvrTestMonAppXlet.log("!!!!!!!! Warning called on " + getNameForResourceUsage(newRequest) + " !!!!!!!!!");
        }
    }

    protected class RegisterResourceContentionHandler extends EventScheduler.NotifyShell
    {
        ResourceContentionHandler m_rch;

        private int m_warning;

        Logger m_log;

        RegisterResourceContentionHandler(long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rch = new DefaultResourceContentionHandler();
            m_counter = 0;
            m_warning = 0;
        }

        RegisterResourceContentionHandler(long taskTriggerTime, int warning)
        {
            super(taskTriggerTime);
            m_rch = new DefaultResourceContentionHandler();
            m_counter = 0;
            m_warning = warning;
        }

        RegisterResourceContentionHandler(org.ocap.resource.ResourceContentionHandler rch, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rch = rch;
        }

        RegisterResourceContentionHandler(Logger log, org.ocap.resource.ResourceContentionHandler rch,
                long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rch = rch;
            m_log = log;
        }

        RegisterResourceContentionHandler(org.ocap.resource.ResourceContentionHandler rch, long taskTriggerTime,
                int warning)
        {
            super(taskTriggerTime);
            m_rch = rch;
            m_counter = 0;
            m_warning = warning;
        }

        RegisterResourceContentionHandler(Logger log, org.ocap.resource.ResourceContentionHandler rch,
                long taskTriggerTime, int warning)
        {
            super(taskTriggerTime);
            m_rch = rch;
            m_counter = 0;
        }

        public void ProcessCommand()
        {

            if (m_log != null)
            {
                m_log.debug("<<<<RegisterResourceContentionHandler::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<RegisterResourceContentionHandler::ProcessCommand>>>>");
            }

            try
            {
                ResourceContentionManager rcm = org.ocap.resource.ResourceContentionManager.getInstance();
                rcm.setResourceContentionHandler(m_rch);
                rcm.setWarningPeriod(m_warning);
                if (m_log != null)
                {
                    m_log.debug("<<<Warning period set to " + rcm.getWarningPeriod() + ">>>");
                }
                else
                {
                    System.out.println("<<<Warning period set to " + rcm.getWarningPeriod() + ">>>");
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Exception registering contention handler");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in RegisterResourceContentionHandler due to rcm.setResourceContentionHandler() exception: "
                        + e.toString());
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in RegisterResourceContentionHandler due to rcm.setResourceContentionHandler() exception: "
                        + e.toString();
                return;
            }
        } // END ProcessCommand()
    } // END class RegisterResourceContentionHandler

    class CheckResourceContentionWarningCount extends EventScheduler.NotifyShell
    {

        /**
         * @param time
         */
        CheckResourceContentionWarningCount(int count, long time)
        {
            super(time);
            m_count = count;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<CheckResourceContentionWarningCount::ProcessCommand>>>>");
            if (m_count != m_counter)
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<<<CheckResourceContentionWarningCount FAILED>>>>>>");
                System.out.println("<<<<<<Count is " + m_counter + " :: Supposed to be " + m_count + ">>>>>>");
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in CheckResourceContentionWarningCount due to mismatch count: counted "
                        + m_counter + " not " + m_count);
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in CheckResourceContentionWarningCount due to mismatch count ";
            }
            else
            {
                System.out.println("<<<<<<CheckResourceContentionWarningCount PASSED>>>>>>");
            }
        }

        private int m_count;
    }

    class GetPrioritizedResourceUsages extends EventScheduler.NotifyShell
    {
        GetPrioritizedResourceUsages(String recordingName, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<GetPrioritizedResourceUsages::ProcessCommand>>>>");

            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

                if (rr == null)
                {
                    System.out.println("DVRUtils: GetPrioritizedResourceUsages - recording not found: "
                            + m_recordingName);
                    return;
                }
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

                ResourceUsage[] prioritizedUsages;

                prioritizedUsages = rm.getPrioritizedResourceUsages(rr);

                System.out.println("GetPrioritizedResourceUsages:: Prioritized resource usages for " + m_recordingName
                        + ":");
                for (int i = 0; i < prioritizedUsages.length; i++)
                {
                    RecordingResourceUsage rru_i = (RecordingResourceUsage) prioritizedUsages[i];
                    OcapRecordingRequest rr_i = (OcapRecordingRequest) rru_i.getRecordingRequest();

                    System.out.println("GetPrioritizedResourceUsages:: prioritizedUsages[" + i + "]: " + findKey(rr_i)
                            + "(state " + rr_i.getState() + ")");
                    System.out.println("GetPrioritizedResourceUsages::   rec req: " + rr_i);
                }

            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Exception getting prioritized resource usages for DvrMonAppTest recording "
                        + m_recordingName + "!");

                e.printStackTrace();
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in GetPrioritizedResourceUsages due to rm.getPrioritizedResourceUsages() exception: "
                        + e.toString());
                m_failedReason = "Flagged FAILURE in GetPrioritizedResourceUsages due to rm.getPrioritizedResourceUsages() exception: "
                        + e.toString();

                return;
            }
        } // END ProcessCommand()

        private String m_recordingName;
    } // END class GetPrioritizedResourceUsages

    /**
     * Schedules a future call to schedule a recording At the task's trigger
     * time, a call to RecordingManager.record will be made with the parameters
     * specified. The resulting recording will placed in the test's recording
     * map.
     * 
     * @author jspruiel
     */
    class AddEpisode extends EventScheduler.NotifyShell
    {
        AddEpisode(String recordingName, String parentName, OcapLocator source, long millisecStartTime,
                long millsecDuration, int rezState, long taskTriggerTime, long secExpiration)
        {
            super(taskTriggerTime);
            m_parentName = parentName;
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_millisecStartTime = millisecStartTime;
            m_millsecDuration = millsecDuration;
            m_secExpiration = secExpiration;
            m_recordingName = recordingName;
            m_rezState = rezState;
            m_recPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
            m_retPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        }

        AddEpisode(String recordingName, String parentName, OcapLocator source, long millisecStartTime,
                long millsecDuration, int rezState, long taskTriggerTime, long secExpiration, byte recPriority,
                int retPriority)
        {
            super(taskTriggerTime);
            m_parentName = parentName;
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_millisecStartTime = millisecStartTime;
            m_millsecDuration = millsecDuration;
            m_secExpiration = secExpiration;
            m_recordingName = recordingName;
            m_rezState = rezState;
            m_retPriority = retPriority;
            m_recPriority = recPriority;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<AddEpisode::ProcessCommand>>>>");

            try
            {
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                OcapRecordingRequest rr;
                LocatorRecordingSpec lrs;
                OcapRecordingProperties orp;

                // get parent rr
                ParentRecordingRequest parent = (ParentRecordingRequest) findObject(m_parentName);
                if (parent == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddEpisode:" + DvrEventPrinter.FindObjFailed + m_parentName);
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in AddEpisode due to unfound parent recording: "
                            + m_parentName);
                    m_failedReason = "Flagged FAILURE in AddEpisode due to unfound parent recording: " + m_parentName;
                    return;
                }

                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_secExpiration,
                        m_retPriority, m_recPriority, null, null, null);

                lrs = new LocatorRecordingSpec(m_source, new Date(m_millisecStartTime), m_millsecDuration, orp);

                m_defaultRecordingName = m_recordingName;

                System.out.println("DVRUtils:AddEpisode issueing rm.resolve()");
                RecordingRequest child = (RecordingRequest) rm.resolve(parent, //
                        lrs, // private spec and props
                        m_rezState); // resolution state

                m_defaultRecordingName = null;

                System.out.println("*****************************************************************");
                System.out.println("****" + m_recordingName + " scheduled as " + child.toString() + "*****");
                System.out.println("*****************************************************************");

                if (child == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddEpisode:FAILED rm.resolve()");
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in AddEpisode due to unfound child for parent recording: "
                            + m_parentName);
                    m_failedReason = "Flagged FAILURE in AddEpisode due to unfound child for parent recording: "
                            + m_parentName;
                    return;
                }

                if (child instanceof ParentRecordingRequest)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddEpisode:FAILED rm.resolve() LeafRecordingRequest expected");
                    DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in AddEpisode due to parent recording class mismatch: "
                            + m_parentName + " not a ParentRecordingRequest");
                    m_failedReason = "Flagged FAILURE in AddEpisode due to parent recording class mismatch: "
                            + m_parentName + " not a ParentRecordingRequest";
                    return;
                }
                insertObject(child, m_recordingName);
            }

            catch (Exception e)
            {
                System.out.println("DVRUtils:AddEpisode:FAILED caught exception");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "AddEpisode:FAILED caught exception. Exception: " + e.getMessage();
            }
        }

        private int m_rezState;

        private String m_recordingName;

        private String m_parentName;

        private OcapLocator m_source[];

        private long m_millisecStartTime;

        private long m_millsecDuration;

        private long m_secExpiration;

        private int m_retPriority;

        private byte m_recPriority;
    }

    /**
     * There must be a 1.1.8 map utility class that should be used here...
     */
    class mapEntry
    {
        String key;

        Object entry;
    }

    /**
     * Schedules a future call to schedule a recording At the task's trigger
     * time, a call to RecordingManager.record will be made with the parameters
     * specified. The resulting recording will placed in the test's recording
     * map.
     */
    class AddSeason extends EventScheduler.NotifyShell
    {
        AddSeason(String requestName, String parentName, PrivateRecordingSpec privSpec, int rezState,
                long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_parentName = parentName;
            m_requestName = requestName;
            m_privSpec = privSpec;
            m_rezState = rezState;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<AddSeason::ProcessCommand>>>>");

            try
            {
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

                // get actual rr
                ParentRecordingRequest parent = (ParentRecordingRequest) findObject(m_parentName);
                if (parent == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddSeason:" + DvrEventPrinter.FindObjFailed + m_parentName);
                    m_failedReason = "AddSeason:" + DvrEventPrinter.FindObjFailed + m_parentName;
                    return;
                }

                System.out.println("DVRUtils:AddSeason:issueing rm.resolve()");
                RecordingRequest child = (RecordingRequest) rm.resolve(parent, //
                        m_privSpec, // private spec and props
                        m_rezState); // resolution state

                if (child == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddSeason:FAILED child resolve returned null");
                    m_failedReason = "AddSeason:FAILED child resolve returned null";
                    return;
                }

                if (!(child instanceof ParentRecordingRequest))
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:AddSeason:FAILED RecordingRequest not parent");
                    m_failedReason = "AddSeason:FAILED RecordingRequest not parent";
                    return;
                }

                insertObject(child, m_requestName);
                findObject(m_requestName);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils:AddSeason:FAILED caught exception");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "AddSeason:FAILED caught exception: " + e.getMessage();
            }
        }

        private int m_rezState;

        private String m_requestName;

        private String m_parentName;

        private PrivateRecordingSpec m_privSpec;
    }

    /**
     * Schedules a future call to schedule a recording At the task's trigger
     * time, a call to RecordingManager.record will be made with the parameters
     * specified. The resulting recording will placed in the test's recording
     * map.
     * 
     * @author Jeff Spruiel
     */
    class NewSeriesRoot extends EventScheduler.NotifyShell
    {
        NewSeriesRoot(String recordingName, PrivateRecordingSpec privSpec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingName = recordingName;
            m_privSpec = privSpec;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<NewSeriesRoot::ProcessCommand>>>>");

            try
            {
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                System.out.println("DVRUtils:NewSeriesRoot issueing rm.record()");
                RecordingRequest rr = (RecordingRequest) rm.record(m_privSpec);

                if (rr != null)
                {
                    if (!(rr instanceof ParentRecordingRequest))
                    {
                        System.out.println("DVRUtils:NewSeriesRoot FAILED RecordingRequest not_parent.");
                        m_failed = TEST_FAILED;
                        m_failedReason = "NewSeriesRoot FAILED RecordingRequest not_parent.";
                        return;
                    }
                    insertObject(rr, m_recordingName);
                    findObject(m_recordingName);
                }
                else
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:NewSeriesRoot FAILED recording(RecordingRequest rr)");
                    m_failedReason = "NewSeriesRoot FAILED recording(RecordingRequest rr)";
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils:NewSeriesRoot FAILED caught exception");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_failedReason = "NewSeriesRoot FAILED caught exception: " + e.getMessage();
            }
        }

        private String m_recordingName;

        public PrivateRecordingSpec m_privSpec;
    }

    /**
     * Deletes the RecordedService associated with the recording.
     * 
     * @author jspruiel
     * 
     */
    class DeleteRecordedService extends EventScheduler.NotifyShell
    {
        /**
         * @param rec
         *            the recording
         * @param taskTriggerTime
         *            when to run
         */
        DeleteRecordedService(String rec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<DeleteRecordedService::ProcessCommand>>>>");
            try
            {
                RecordingRequest rr = (RecordingRequest) findObject(m_rName);

                if (rr == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:DeleteRecordedService:" + DvrEventPrinter.FindObjFailed + m_rName);
                    m_failedReason = "DVRUtils:DeleteRecordedService:" + DvrEventPrinter.FindObjFailed + m_rName;
                    return;
                }
                System.out.println("DVRUtils:DeleteRecordedService:issueing rs.delete()");
                RecordedService rs = ((LeafRecordingRequest) rr).getService();
                rs.delete();
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("DVRUtils:DeleteRecordedService:failed exception on rr.delete()");
                m_failedReason = "DVRUtils:DeleteRecordedService:failed exception on rr.delete(). Exception: "
                        + e.getMessage();
                e.printStackTrace();
            }
        }

        private String m_rName;

        private long m_refZero;
    }

    /**
     * Deletes a RecordingRequest object. Does not remove the object from the
     * test framework.
     * 
     * @author jspruiel
     * 
     */
    class DeleteRecordingRequest extends EventScheduler.NotifyShell
    {

        DeleteRecordingRequest(String rec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_ignoreFailure = false;
        }

        DeleteRecordingRequest(Logger log, String rec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_ignoreFailure = false;
            m_log = log;
        }

        DeleteRecordingRequest(String rec, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_ignoreFailure = false;
        }

        DeleteRecordingRequest(Logger log, String rec, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_ignoreFailure = false;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<DeleteRecordingRequest::ProcessCommand>>>>");
            }
            else
            {
                System.out.println();
                System.out.println("<<<<DeleteRecordingRequest::ProcessCommand>>>>");
            }

            try
            {
                RecordingRequest rr = (RecordingRequest) findObject(m_rName);

                if (rr == null)
                {
                    if (!m_ignoreFailure)
                    {
                        m_failed = TEST_FAILED;
                        if (m_log != null)
                        {
                            m_log.debug("DVRUtils:DeleteRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName);
                        }
                        else
                        {
                            System.out.println("DVRUtils:DeleteRecordingRequest:" + DvrEventPrinter.FindObjFailed
                                    + m_rName);
                        }
                        m_failedReason = "DeleteRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName;
                    }
                    return;
                }

                if (m_log != null)
                {
                    m_log.debug("DVRUtils:DeleteRecordingRequest:issueing rr.delete() on: " + m_rName + " : " + rr);
                }
                else
                {
                    System.out.println("DVRUtils:DeleteRecordingRequest:issueing rr.delete() on: " + m_rName + " : "
                            + rr);
                }
                rr.delete();
            }
            catch (Exception e)
            {
                if (!m_ignoreFailure)
                {
                    m_failed = TEST_FAILED;
                    if (m_log != null)
                    {
                        m_log.debug("DVRUtils:DeleteRecordingRequest:failed exception on rr.delete()");
                    }
                    else
                    {
                        System.out.println("DVRUtils:DeleteRecordingRequest:failed exception on rr.delete()");
                    }
                    m_failedReason = "DeleteRecordingRequest:failed exception on rr.delete(). Exception: "
                            + e.getMessage();
                }
                e.printStackTrace();
            }
        }

        private String m_rName;

        private boolean m_ignoreFailure;

        private Logger m_log;
    }

    /**
     * Utility tasks - schedule a printout of all the recordings in the
     * recording manager
     */
    class PrintEventListElems extends EventScheduler.NotifyShell
    {
        PrintEventListElems(Vector list, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_eventList = list;
        }

        public void ProcessCommand()
        {
            DvrEventPrinter.printEventQueue(m_eventList);
        }

        private Vector m_eventList;
    }

    class ValidateRequest extends EventScheduler.NotifyShell
    {
        ValidateRequest(String recReq, boolean exist, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_nameReq = recReq;
            m_exist = exist;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList rlist = orm.getEntries();

            RecordingRequest rr = (RecordingRequest) findObject(m_nameReq);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<ValidateRequest>> findObject for leaf failed. ");
                return;
            }
            else
            {
                if (m_exist)
                {
                    if (!rlist.contains(rr))
                    {
                        m_failed = TEST_FAILED;
                        System.out.println("<<<<ValidateRequest>> FAILED " + m_nameReq + " missing.");
                        return;
                    }
                }
                else
                {
                    if (rlist.contains(rr))
                    {
                        m_failed = TEST_FAILED;
                        System.out.println("<<<<ValidateRequest>> FAILED " + m_nameReq + " should not be present.");
                        return;
                    }
                }
            }
        }

        static final boolean IN_LIST = true;

        static final boolean NOT_IN_LIST = false;

        private boolean m_exist;

        private String m_nameReq;
    }

    class CancelRecordingRequest extends EventScheduler.NotifyShell
    {
        CancelRecordingRequest(String rec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
        }

        public void ProcessCommand()
        {
            RecordingRequest rr = null;

            try
            {
                System.out.println();
                System.out.println("<<<<CancelRecordingRequest::ProcessCommand>>>>");
                rr = (RecordingRequest) findObject(m_rName);

                if (rr == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("CancelRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName);
                    m_failedReason = "CancelRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName;
                    return;
                }

                System.out.println("CancelRecordingRequest:_issueing_rr.cancel()");

                if (rr instanceof ParentRecordingRequest)
                {
                    ((ParentRecordingRequest) rr).cancel();
                }
                else
                {
                    ((LeafRecordingRequest) rr).cancel();
                }
                return;
            }
            catch (IllegalStateException e)
            {
                m_failed = TEST_FAILED;
                System.out.println("CancelRecordingRequest:_IllegalStateException_from_" + rr);
                m_failedReason = "CancelRecordingRequest:_IllegalStateException_from_" + rr;
                e.printStackTrace();
            }
            catch (AccessDeniedException e)
            {
                m_failed = TEST_FAILED;
                System.out.println("CancelRecordingRequest:_AccessDeniedException_from_" + rr);
                m_failedReason = "CancelRecordingRequest:_AccessDeniedException_from_" + rr;
                e.printStackTrace();
            }
        }

        private String m_rName;
    }

    /**
     * @author jspruiel Creates a BufferingRequest
     */
    class CreateBufferingRequest extends EventScheduler.NotifyShell
    {

        /**
         * Creates a BufferingRequest
         * 
         * @param name
         *            - A name by which the framework tracks the
         *            BufferingRequest.
         * @param service
         *            - The service to buffer.
         * @param minDuration
         *            - Minimum duration in seconds to buffer.
         * @param maxDuration
         *            - Maximum duration in seconds to buffer.
         * @param efap
         *            - Extended file access permissions for this request. If
         *            this parameter is null, no write permissions are given to
         *            this request. Read permissions for BufferingRequest
         *            instances are always world regardless of read permissions
         *            set by this parameter.
         * @param taskTriggerTime
         *            - Time DvrTest issues the command.
         */
        CreateBufferingRequest(String name, Service service, long minDuration, long maxDuration,
                ExtendedFileAccessPermissions efap, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_service = service;
            m_minDuration = minDuration;
            m_maxDuration = maxDuration;
            m_efap = efap;
        }

        CreateBufferingRequest(Logger log, String name, OcapLocator source, long minDuration, long maxDuration,
                ExtendedFileAccessPermissions efap, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_source = source;
            m_minDuration = minDuration;
            m_maxDuration = maxDuration;
            m_efap = efap;
            m_log = log;

            SIManager siManager = SIManager.createInstance();
            try
            {
                m_service = siManager.getService(m_source);
            }
            catch (InvalidLocatorException e)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:CreateBufferingRequest: Flagged FAILURE: No Service:" + m_source);
                m_failedReason = "DvrMonAppTest:CreateBufferingRequest: Flagged FAILURE: No Service.";
            }
        }

        public void ProcessCommand()
        {
            BufferingRequest br = null;

            if (m_service != null)
            {
                try
                {
                    if (m_log != null)
                    {
                        m_log.debug("\n");
                        m_log.debug("<<<<CreateBufferingRequest::ProcessCommand>>>>");
                    }
                    else
                    {

                        System.out.println();
                        System.out.println("<<<<CreateBufferingRequest::ProcessCommand>>>>");
                    }
                    br = BufferingRequest.createInstance(m_service, m_minDuration, m_maxDuration, m_efap);
                    if (br == null)
                    {
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log("DvrMonAppTest:CreateBufferingRequest: Flagged FAILURE IllegalArgumentException");
                        m_failedReason = "DvrMonAppTest:CreateBufferingRequest:" + m_name;
                        return;
                    }
                    insertObject(br, m_name);
                    DvrTestMonAppXlet.log("<<<<DvrMonAppTest:CreateBufferingRequest... done>>>>");
                }
                catch (IllegalArgumentException iae)
                {
                    m_failed = TEST_FAILED;
                    DvrTestMonAppXlet.log("DvrMonAppTest:CreateBufferingRequest: Flagged FAILURE IllegalArgumentException");
                    iae.printStackTrace();
                    m_failedReason = "DvrMonAppTest:CreateBufferingRequest:" + m_name;
                    return;
                }
            }
        }

        private OcapLocator m_source;

        private Service m_service;

        private long m_minDuration;

        private long m_maxDuration;

        private ExtendedFileAccessPermissions m_efap;

        private String m_name;

        private Logger m_log;
    }// end class

    /**
     * @author jspruiel Command to start a BufferingRequest
     */
    class StartBufferingRequest extends EventScheduler.NotifyShell
    {
        /**
         * Ctor
         * 
         * @param name
         *            - Framework command identifier.
         * @param taskTriggerTime
         *            - Time to execute command.
         */
        StartBufferingRequest(String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
        }

        StartBufferingRequest(Logger log, String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("\n");
                m_log.debug("<<<<StartBufferingRequest::ProcessCommand>>>>");

            }
            else
            {
                System.out.println();
                System.out.println("<<<<StartBufferingRequest::ProcessCommand>>>>");
            }
            BufferingRequest br = null;
            br = (BufferingRequest) findObject(m_name);
            if (br == null)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:StartBufferingRequest: Flagged FAILURE: "
                        + DvrEventPrinter.FindObjFailed + m_name);
                if (m_log != null)
                {
                    m_log.debug("DvrMonAppTest:StartBufferingRequest: Flagged FAILURE: "
                            + DvrEventPrinter.FindObjFailed + m_name);
                }

                m_failedReason = "DvrMonAppTest:StartBufferingRequest: Flagged FAILURE: "
                        + DvrEventPrinter.FindObjFailed + m_name;
                return;
            }

            OcapRecordingManager ocm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            try
            {
                ocm.requestBuffering(br);
                if (m_log != null)
                {
                    m_log.debug("<<<<DvrMonAppTest:StartBufferingRequest...done>>>>");
                }
            }
            catch (SecurityException se)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:StartBufferingRequest Flagged FAILURE SecurityException");
                se.printStackTrace();
                m_failedReason = "DvrMonAppTest:StartBufferingRequest: SecurityException:" + m_name;
            }
        }

        private String m_name;

        Logger m_log;
    }// end class

    /**
     * VerifyBufferingRequest: Confirms the specified BufferingRequest</code> is
     * active.
     * 
     * How: TBD
     * 
     * @author jspruiel
     * 
     */
    class VerifyBufferingRequest extends EventScheduler.NotifyShell
    {
        VerifyBufferingRequest(String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
        }

        public VerifyBufferingRequest(Logger log, String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_log = log;
            m_name = name;
        }

        public void ProcessCommand()
        {
            System.out.println();
            if (m_log != null)
            {
                m_log.debug("<<<<VerifyBufferingRequest::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<VerifyBufferingRequest::ProcessCommand>>>>");
            }

            BufferingRequest br = (BufferingRequest) findObject(m_name);
            if (br == null)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE in VerifyBufferingRequest:"
                        + DvrEventPrinter.FindObjFailed + m_name);
                if (m_log != null)
                {
                    m_log.debug("DvrMonAppTest: Flagged FAILURE in VerifyBufferingRequest:"
                            + DvrEventPrinter.FindObjFailed + m_name);
                }
                m_failedReason = "DvrMonAppTest: Flagged FAILURE in VerifyBufferingRequest:"
                        + DvrEventPrinter.FindObjFailed + m_name;
                return;
            }

            OcapRecordingManager ocm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            BufferingRequest[] bra = ocm.getBufferingRequests();

            BufferingRequest brCurrent;
            int length = bra.length;
            boolean found = false;
            if (length == 0)
            {
                DvrTestMonAppXlet.log("DvrMonAppTest: Flagged FAILURE:VerifyBufferingRequest: BR[].lenth == 0");
                if (m_log != null)
                {
                    m_log.debug("DvrMonAppTest: Flagged FAILURE:VerifyBufferingRequest: BR[].lenth == 0");
                }
            }

            // If br is present, then it is active
            for (int i = 0; i < length; i++)
            {
                brCurrent = bra[i];
                if (br.equals(brCurrent))
                {
                    found = true;
                    break;
                }
            }// end for

            if (!found)
            {
                m_failed = TEST_FAILED;
                DvrTestMonAppXlet.log("DvrMonAppTest:VerifyBufferingRequest: Flagged FAILURE BR Not found in list BR[]");
                if (m_log != null)
                {
                    m_log.debug("DvrMonAppTest:VerifyBufferingRequest: Flagged FAILURE BR Not found in list BR[]");
                }
                m_failedReason = "DvrMonAppTest:VerifyBufferingRequest: Not Active" + m_name;
                return;
            }
            if (m_log != null)
            {
                m_log.debug("DvrMonAppTest:VerifyBufferingRequest...ok");
            }
            DvrTestMonAppXlet.log("DvrMonAppTest:VerifyBufferingRequest...ok");
        }

        private String m_name;

        private Logger m_log;
    }

    /**
     * CancelBufferingRequest: Cancels all or the specified
     * BufferingRequest</code>.
     * 
     * How: TBD
     * 
     * @author jspruiel
     * 
     */
    class CancelBufferingRequest extends EventScheduler.NotifyShell
    {
        CancelBufferingRequest(long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_cancelAll = true;
        }

        CancelBufferingRequest(String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
        }

        CancelBufferingRequest(String name, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_ignoreFailure = ignoreFailure;
        }

        CancelBufferingRequest(Logger log, String name, boolean ignoreFailure, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_name = name;
            m_ignoreFailure = ignoreFailure;
            m_log = log;
        }

        public CancelBufferingRequest(Logger log, String name, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_log = log;
            m_name = name;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("\n");
                m_log.debug("<<<<CancelBufferingRequest::ProcessCommand>>>>");
            }
            else
            {
                System.out.println();
                System.out.println("<<<<CancelBufferingRequest::ProcessCommand>>>>");
            }

            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            BufferingRequest[] bra = orm.getBufferingRequests();

            if (m_cancelAll)
            {
                for (int i = 0; i < bra.length; i++)
                {
                    orm.cancelBufferingRequest(bra[i]);
                }
            }
            else
            {
                BufferingRequest br = (BufferingRequest) findObject(m_name);
                if (br == null)
                {
                    if (m_ignoreFailure)
                    {
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log("DvrMonAppTest:CancelBufferingRequest Flagged FAILURE:"
                                + DvrEventPrinter.FindObjFailed + m_name);
                        if (m_log != null)
                        {
                            m_log.debug("DvrMonAppTest:CancelBufferingRequest Flagged FAILURE:"
                                    + DvrEventPrinter.FindObjFailed + m_name);
                        }
                        m_failedReason = "DvrMonAppTest:CancelBufferingRequest Flagged FAILURE:"
                                + DvrEventPrinter.FindObjFailed + m_name;
                    }
                    return;
                }

                BufferingRequest brCurrent;
                int length = bra.length;
                boolean found = false;

                // If br is present, then it is active
                for (int i = 0; i < length; i++)
                {
                    brCurrent = bra[i];
                    if (br.equals(brCurrent))
                    {
                        found = true;
                        orm.cancelBufferingRequest(brCurrent);
                        break;
                    }
                }// end for

                if (!found)
                {
                    if (m_ignoreFailure)
                    {
                        DvrTestMonAppXlet.log("DvrMonAppTest:CancelBufferingRequest: Flagged FAILURE Not Active:IgnoreFailure is true");
                        if (m_log != null)
                        {
                            m_log.debug("DvrMonAppTest:CancelBufferingRequest: Flagged FAILURE Not Active:IgnoreFailure is true");
                        }
                        m_failedReason = "DvrMonAppTest:CancelBufferingRequest: Not Active:IgnoreFailer is true"
                                + m_name;
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DvrTestMonAppXlet.log("DvrMonAppTest:CancelBufferingRequest: Flagged FAILURE Not Active");
                        if (m_log != null)
                        {
                            m_log.debug("DvrMonAppTest:CancelBufferingRequest: Flagged FAILURE Not Active");
                        }
                        m_failedReason = "DvrMonAppTest:CancelBufferingRequest: Not Active" + m_name;
                        return;
                    }
                }
            }
            if (m_log != null)
            {
                m_log.debug("DvrMonAppTest:CancelBufferingRequest...done: ignoreFailure = " + m_ignoreFailure);
            }
            DvrTestMonAppXlet.log("DvrMonAppTest:CancelBufferingRequest...done: ignoreFailure = " + m_ignoreFailure);
        }

        private String m_name;

        private boolean m_ignoreFailure = false;

        private boolean m_cancelAll = false;

        private Logger m_log;
    }

    /**
     * 
     * @author bforan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    /*
     * class CallMonitorConfiguredSignal extends EventScheduler.NotifyShell{
     * private Logger m_log;
     * 
     * CallMonitorConfiguredSignal(long taskTriggerTime) {
     * super(taskTriggerTime); }
     * 
     * public void ProcessCommand(){ if(m_log != null) { m_log.debug("\n");
     * m_log.debug("<<<<CallMonitorConfiguredSignal::ProcessCommand>>>>");
     * }else{ System.out.println();
     * System.out.println("<<<<CallMonitorConfiguredSignal::ProcessCommand>>>>"
     * ); }
     * 
     * try { OcapSystem.monitorConfiguredSignal(); } catch (SecurityException e)
     * { m_failed = TEST_FAILED; DvrTestMonAppXlet.log(
     * "CallMonitorConfiguredSignal:: Flagged FAILURE"); if(m_log != null) {
     * m_log.debug( "CallMonitorConfiguredSignal:: Flagged FAILURE"); }
     * m_failedReason = "CallMonitorConfiguredSignal:: " + e.getClass(); return;
     * }
     * 
     * if(m_log != null) {
     * m_log.debug("<<<<CallMonitorConfiguredSignal::PASSED>>>>"); } else{
     * System.out.println("<<<<ConfirmRecordingReq_CheckState::PASSED>>>>"); } }
     * }
     * 
     * class SetRecordingDelay extends EventScheduler.NotifyShell{ private
     * Logger m_log; private long m_delayTime;
     * 
     * SetRecordingDelay(long delayTime, long taskTriggerTime) {
     * super(taskTriggerTime); m_delayTime = delayTime; }
     * 
     * public void ProcessCommand(){ if(m_log != null) { m_log.debug("\n");
     * m_log.debug("<<<<SetRecordingDelay::ProcessCommand>>>>"); }else{
     * System.out.println();
     * System.out.println("<<<<SetRecordingDelay::ProcessCommand>>>>"); }
     * 
     * OcapRecordingManager recordingManager =
     * (OcapRecordingManager)OcapRecordingManager.getInstance();
     * recordingManager.setRecordingDelay(m_delayTime); }
     * 
     * }
     */

    /**
     * Returns the corresponding service or null if failed.
     */
    Service locatorToService(Logger log, OcapLocator locator)
    {
        SIManager siManager = SIManager.createInstance();
        Service service = null;
        try
        {
            service = siManager.getService(locator);
            log.debug("locatorToService returning " + service);
            return service;
        }
        catch (SecurityException e)
        {
            log.debug("locatorToService SecurityException");
            e.printStackTrace();
        }
        catch (InvalidLocatorException e)
        {
            log.debug("locatorToService InvalidLocatorException");
            e.printStackTrace();
        }

        log.debug("locatorToService returnin null");
        return null;
    }

    // FIELDS

    static Vector m_objects = new Vector();

    Vector m_locators = null;

    EventScheduler m_eventScheduler = null;

    static final long m_oneSecond = 1000;

    static final long m_oneMinute = m_oneSecond * 60;

    static final long m_oneHour = m_oneMinute * 60;

    static final long m_oneDay = m_oneHour * 24;

    static final long fudgeFactor = 5000;

    int m_counter = 0;

    int m_failed = 0;

    String m_failedReason = "";

    SimpleCondition m_cond = null;

    protected static ServiceContext m_serviceContext;

    protected boolean testingSI = false;

    String m_defaultRecordingName; // Used to discover a new recording's name
                                   // while in process of being constructed
}
