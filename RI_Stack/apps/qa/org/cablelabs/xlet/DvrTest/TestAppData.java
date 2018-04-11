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
 * Created on Jul 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.*;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.*;

import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

class PersonInfo implements Serializable
{
    public int m_height;

    public int m_weight;

    public String m_name;

    PersonInfo()
    {
        System.out.println("PersonInfo CTOR NO Parms");

    }

    PersonInfo(int height, int weight, String name)
    {
        m_height = height;
        m_weight = weight;
        m_name = name;
    }

    public int getHeight()
    {
        return m_height;
    }

    public int getWeight()
    {
        return m_weight;
    }

    public String getName()
    {
        return m_name;
    }

    public boolean equals()
    {
        return true;

    }
}


/**
 * 
 * 
 * 
 * 
 * 
 * @author jspruiel
 * 
 * 
 * 
 */
public class TestAppData extends DvrTest
{

    /**
     * 
     * @param locators
     */
    TestAppData(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    /**
     * 
     * @return
     */
    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestAddAppData((OcapLocator) m_locators.elementAt(0), false, false));
        tests.addElement(new TestAddAppData((OcapLocator) m_locators.elementAt(0), true, false));
        tests.addElement(new TestAddAppData((OcapLocator) m_locators.elementAt(0), false, true));
        tests.addElement(new TestAddAppData((OcapLocator) m_locators.elementAt(0), true, true));
        return tests;
    }

    /**
     * 
     * @author jspruiel
     * 
     * Simple Test for: addAppData(),getKeys(),getAppData(),removeAppData()
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestAddAppData extends TestCase implements RequestResolutionHandler
    {
        /**
         * 
         * @param locator
         */
        TestAddAppData(OcapLocator locator, boolean parentRec, boolean addAppDataAtScheduling)
        {
            m_locator = locator;
            m_parentRec = parentRec;
            m_addAppDataAtScheduling = addAppDataAtScheduling;
        }

        /**
         * 
         * @return
         */
        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestAddAppData" +(m_addAppDataAtScheduling? "_AtScheduling" : "") +(m_parentRec? "(ParentRecordingRequest)" : "");
        }

        /**
         * 
         *
         */
        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            reset();

            if (m_parentRec)
            {
                // Install the request resolution handler.
                OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                rm.setRequestResolutionHandler(this);
            }

            Serializable appData[] = new Serializable[1];
            appData[0] = (Serializable) new PersonInfo(74, 202, "Jeff");

            String keys[] = new String[1];
            keys[0] = "jas";

            int keyCapacity = 64;

            if (m_addAppDataAtScheduling)
            {
                if (m_parentRec)
                    m_eventScheduler.scheduleCommand(new CreateParentRoot("Rec", keys, appData, 5000));

                else 
                    m_eventScheduler.scheduleCommand(new RecordAddData("Rec", m_locator, now + 90000, 30000, keys, appData, 5000));

                keyCapacity = 63;
            }
            else
            {
                if (m_parentRec)
                    m_eventScheduler.scheduleCommand(new CreateParentRoot("Rec", 5000));
                else
                    m_eventScheduler.scheduleCommand(new Record("Rec", m_locator, now + 90000, 30000, 5000));
            }

            //check IllegalArgumentException is thrown as expected
            appData[0] = new long[256 * 65];
            m_eventScheduler.scheduleCommand(new AddAppData("Rec", 6000, "testKey", appData[0], "java.lang.IllegalArgumentException", 1));

            // first check of AppData, the only AppData that should have been
            // added would have been the one added while rr was created 
            m_eventScheduler.scheduleCommand(new CheckAppData("Rec", -1, false, m_addAppDataAtScheduling, 8000));


            appData[0] = new PersonInfo(74, 202, "Jeff");
            m_eventScheduler.scheduleCommand(new AddAppData("Rec", 10000, "testKey", appData[0], null, keyCapacity));


            // addAppData again and verify NoMoreDataEntriesException 
            m_eventScheduler.scheduleCommand(new AddAppData("Rec", 12000, "failKey", appData, "org.ocap.shared.dvr.NoMoreDataEntriesException", 1));

            // check and remove AppData
            m_eventScheduler.scheduleCommand(new CheckAppData("Rec", 64, true, m_addAppDataAtScheduling, 14000));


            // Clean up
            m_eventScheduler.scheduleCommand(new DeleteRecording("Rec", 16000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }


        public void requestResolution(RecordingRequest request)
        {
            System.out.println(getName() + ": requestResolution ENTERED");
            RecordingSpec rspec = request.getRecordingSpec();
            if (rspec instanceof PrivateRecordingSpec)
            {
                System.out.println(getName() + ": successfully retrieved PrivateRecordingSpec");
                //OCORI_2356: data returned here is null 
                Serializable data = ((PrivateRecordingSpec) rspec).getPrivateData();
                if (data != null)
                {
                    System.out.println(getName() + ": successfully retrieve PrivateData from PrivateRecordingSpec");

                    String checkData = (String) data;
                    if (checkData.equals("Response for Root"))
                    {
                        System.out.println(getName() + ": PrivateRecordingSpec.getPrivateData() returned expected value");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = getName() + ":  failure, PrivateRecordingSpec.getPrivateData() returned incorrectly";
                        System.out.println(m_failedReason);
                    }
                }
                else 
                {
                    m_failedReason = getName() + ":  failure, RecordingSpec.getPrivateData() returned NULL";
                    System.out.println(m_failedReason);
                    m_failed = TEST_FAILED;
                }
            }
            else
            {
                m_failedReason = getName() + ": error RecordingSpec is wrong subclass";
                System.out.println(m_failedReason);
                m_failed = TEST_FAILED;
            }
        }

        private OcapLocator m_locator;
        private boolean m_parentRec;
        private boolean m_addAppDataAtScheduling;
    }


    public class RecordAddData extends NotifyShell
    {
        RecordAddData(String recordingName, OcapLocator source, long startTime, long duration, String[] keys, Serializable[] appData, long taskTriggerTime)
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
            m_keys = keys;
            m_appData = appData;
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
                    m_log.debug("DVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime + " Duration:" + m_duration + " retentionPriority = " + m_retentionPriority + " resourcePriority: " + m_resourcePriority);
                }
                else
                {
                    System.out.println("<<<<<<Record ProcessCommand>>>>>");
                    System.out.println("\nDVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime + " Duration:" + m_duration + " retentionPriority = " + m_retentionPriority + " resourcePriority: " + m_resourcePriority);

                }
                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration, m_retentionPriority, m_recordingPriority, null, null, null, m_resourcePriority);
                lrs = new LocatorRecordingSpec(m_source, new Date(m_startTime), m_duration, orp);

                m_defaultRecordingName = m_recordingName;

                rr = (OcapRecordingRequest) rm.record(lrs, m_keys, m_appData);

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
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Record due to rm.record() xception: " + e.toString());
                m_failedReason = "DvrTest: Flagged FAILURE in Record due to rm.record() xception: " + e.toString();
            }
        }

        private OcapLocator m_source[];

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;

        private int m_retentionPriority;

        private byte m_recordingPriority;

        private Logger m_log;

        private int m_resourcePriority = 0;

        private String[] m_keys;

        private Serializable[] m_appData;
    }


    /**
     * 
     * @author jspruiel
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    class CheckAppData extends EventScheduler.NotifyShell
    {
        CheckAppData(String rec, String key, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_expectedKeys.add(key);
            m_keyCt = 1; 
        }

        CheckAppData(String rec, int ct, boolean remove, boolean addAtSchedule, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
            m_remove = remove;

            m_keyCt = ct; 
            if (addAtSchedule) 
            {
                m_keyCt = 1;
                m_expectedKeys.add("jas");
            } 
            if (ct > 0)  
            {
                m_keyCt = ct;

                for (int i = 0; i < ct; i++) 
                    m_expectedKeys.add("testKey_"+i);

                if (addAtSchedule) 
                    m_expectedKeys.remove(m_expectedKeys.lastElement());
            }
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<CheckAppData::ProcessCommand>>>>");

            RecordingRequest rr = (RecordingRequest) findObject(m_rName);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils: CheckAppData failure - recording not found! " + m_rName;
                System.out.println(m_failedReason);
                return;
            }

            try
            {
                String[] keys = rr.getKeys(); 

                if (m_keyCt == -1)
                {
                    if (keys != null)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "DVRUtils: CheckAppData FAILED - getKeys() should have returned null";
                        System.out.println(m_failedReason);
                    }
                    return; 
                }

                if (keys.length != m_keyCt)
                {
                    m_failed = TEST_FAILED;

                    m_failedReason = "DVRUtils: CheckAppData FAILED - number of keys should be " +m_keyCt +", but got " +keys.length;
                    System.out.println(m_failedReason);
                    return;
                }
                
                for (int i = 0; i < m_keyCt; i++)
                {
                    String k = (String) keys[i];
                    System.out.println("DVRUtils: CheckAppData got key " +k);


                    if (!m_expectedKeys.remove(k))
                    {
                        m_failedReason = "DVRUtils: CheckAppData failed - found unexpected key " +k;
                        m_failed = TEST_FAILED;
                        System.out.println(m_failedReason);
                        return;
                    }

                    PersonInfo data = (PersonInfo) rr.getAppData(k);
                    int height = data.getHeight();
                    int weight = data.getWeight();
                    String name = data.getName();

                    if (height != 74 || weight != 202 || !name.equals("Jeff"))
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "DvrTest: ChecekAppData failure - Serializable data " +i +" not equal!";
                        System.out.println(m_failedReason);
                        return;
                    }

                    if (m_remove) rr.removeAppData(k);
                }
                if (m_expectedKeys.size() != 0)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DvrTest: CheckAppData failure - did not find the following expected keys: ";
                    for (int j = 0; j < m_expectedKeys.size(); j++)
                    {
                        m_failedReason = m_failedReason + (String)m_expectedKeys.get(j) +", ";
                    }
                }

                if (m_remove && rr.getKeys()!= null)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DvrTest: CheckAppData failure - getKeys() did not return null after removeAppData()";
                    System.out.println(m_failedReason);
                    return;
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DvrTest: CheckAppData failure - caught exception "+e;
                System.out.println(m_failedReason);
                e.printStackTrace();
            }
        }

        private String m_rName;
        private int m_keyCt;
        private boolean m_remove = true;
        private boolean m_addAtSchedule = false;
        private Vector m_expectedKeys = new Vector(); 
    }


    /**
     * 
     * @author jspruiel
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    class CreateParentRoot extends EventScheduler.NotifyShell
    {
        CreateParentRoot(String recordingName, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingName = recordingName;
        }

        CreateParentRoot(String recordingName, String[] keys, Serializable[] appData, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recordingName = recordingName;
            m_keys = keys;
            m_appData = appData;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<CreateParentRoot::ProcessCommand>>>>");

            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                System.out.println("DVRUtils: issueing root parent recording:");

                //OCORI_2355: ClassCastException when calling record(...);
                Props prop = new Props(1000L * 60L * 60L * 24L * 28L);
                //OcapRecordingProperties prop = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, (long) 1000L*60L*60L*24L*28L, 127, (byte) OcapRecordingProperties.RECORD_WITH_CONFLICTS , null, null, null);

                PrivateRecordingSpec prs = new PrivateRecordingSpec("Response for Root", prop);

                RecordingRequest rr;
                if (m_keys != null || m_appData != null)
                {
                    rr = (RecordingRequest) rm.record(prs, m_keys, m_appData);
                }
                else
                {
                    rr = (RecordingRequest) rm.record(prs);
                }

                if (rr != null)
                {
                    if (!(rr instanceof ParentRecordingRequest))
                    {
                        m_failedReason = "DVRUtils: RecordingRequest not parent";
                        System.out.println(m_failedReason);
                        m_failed = TEST_FAILED;
                    }
                    insertObject(rr, m_recordingName);
                }
            }
            catch (Exception e)
            {
                m_failedReason = "DVRUtils: CreateParentRoot : FAILED";
                System.out.println(m_failedReason);
                e.printStackTrace();
                
                m_failed = TEST_FAILED;
            }
        }

        class Props extends RecordingProperties
        {
            public Props(long expirationPeriod)
            {
                super(expirationPeriod);
            }
        }

        private String m_recordingName;

        private String[] m_keys = null;
        private Serializable[] m_appData = null;
    }
}
