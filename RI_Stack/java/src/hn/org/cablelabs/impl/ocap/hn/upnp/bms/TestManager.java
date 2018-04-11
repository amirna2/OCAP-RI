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

package org.cablelabs.impl.ocap.hn.upnp.bms;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer; 
import org.cablelabs.impl.ocap.hn.upnp.bms.TestID; 

/**
 * This class represents a singleton BasicManagerService TestManager.
 * It maintains a list of TestIDs( active or saved) that the
 * BasicManagementService uses to track it's activites. 
 * 
 * It is only accessible by the org.cablelabs.impl.ocap.hn.upnp.bms package.
 * 
 * 
 */
class TestManager 
{

    private static TestManager s_instance = null;
    private Map m_testIDS = null; 
    private int m_nextAvailableID; 


    private TestManager()
    {
        m_testIDS = new HashMap();
        m_nextAvailableID = 1;
    }

    /**
     * Returns the singleton TestManager.
     *
     * @return Singleton instance of TestManager
     *
     */
    public synchronized static TestManager getInstance()
    {
        if (s_instance == null)
        {
            s_instance = new TestManager();
        }

        return s_instance;

    }

    private synchronized int getNextAvailableID()
    {
        return m_nextAvailableID++;
    }

    public synchronized int addTest(String testType)
    {
        Integer id = new Integer(getNextAvailableID());
        TestID newTest = new TestID(id.intValue(), testType, TestID.REQUESTED);
        m_testIDS.put(id, newTest);

        MediaServer.getInstance().getBMS().updateTestIDs(getTestIDsCSV());
        MediaServer.getInstance().getBMS().updateActiveTestIDs(getActiveTestIDsCSV());

        return id.intValue(); 
    }

    public synchronized TestID getTestID(int id)
    {
        Integer key = new Integer(id);
        return (TestID) m_testIDS.get(key);

    }

    public Map getTestIDs()
    {
        return m_testIDS;

    }

    public synchronized String getTestIDsCSV()
    {
        // Create testid csv string 
        Integer[] ids = (Integer[]) (m_testIDS.keySet().toArray( new Integer[m_testIDS.size()]));
        Arrays.sort(ids);
        StringBuffer testIDEvent = new StringBuffer();
        for (int i = 0; i < ids.length; i++)
        {
            testIDEvent.append(ids[i].toString());
            testIDEvent.append(",");
        }
        if (testIDEvent.length() > 0)
        {
            // remove last ,
            testIDEvent.deleteCharAt(testIDEvent.length()-1);
        }

        return testIDEvent.toString();
    }

    public synchronized String getActiveTestIDsCSV()
    {
        // Create testid csv string 
        Integer[] ids = (Integer[]) (m_testIDS.keySet().toArray( new Integer[m_testIDS.size()]));
        Arrays.sort(ids);
        StringBuffer activeTests= new StringBuffer();
        for (int i = 0; i < ids.length; i++)
        {
            TestID diagTest = (TestID) m_testIDS.get(ids[i]);
            String state = diagTest.getTestState();
            if (TestID.REQUESTED.equals(state)
                || TestID.IN_PROGRESS.equals(state))
            {
                activeTests.append(ids[i].toString());
                activeTests.append(",");
            }
        }
        if (activeTests.length() > 0)
        {
            // remove last ,
            activeTests.deleteCharAt(activeTests.length()-1);
        }

        return activeTests.toString();

    }

}

