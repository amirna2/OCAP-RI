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

package org.cablelabs.test.cds;

import java.util.Enumeration;

import org.cablelabs.test.Test;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.DatabaseException;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.content.navigation.DatabaseQuery;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class DatabaseQueryTest extends Test
{

    /** DOCUMENT ME! */
    private TestNetActionHandler netActionHandler = null;

    /** DOCUMENT ME! */
    private ContentServerNetModule contentServerNetModule = null;

    /** DOCUMENT ME! */
    private NetActionRequest netActionRequest = null;

    /** DOCUMENT ME! */
    private String className = "DatabaseQueryTest";

    /** DOCUMENT ME! */
    private String description = "Build Queries for filtering ContentLists.";

    /** DOCUMENT ME! */
    private String pass = "PASS";

    /** DOCUMENT ME! */
    private String fail = "FAIL";

    /** DOCUMENT ME! */
    private boolean PASS = true;

    /** DOCUMENT ME! */
    private boolean FAIL = !PASS;

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int clean()
    {
        netActionHandler = null;
        contentServerNetModule = null;
        netActionRequest = null;

        return Test.TEST_STATUS_PASS;
    }

    /**
     * Main entry point to start the test...
     * 
     * @return An integer value to the TestRunner - representing Pass/Fail
     */
    public int execute()
    {
        testLogger.setPrefix(className);
        messageOut("started", PASS);

        if (runTest())
        {
            messageOut("Completed.", PASS);
            return Test.TEST_STATUS_PASS;
        }
        else
        {
            messageOut("Completed.,", FAIL);
            return Test.TEST_STATUS_FAIL;
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getName()
    {
        return className;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int prepare()
    {
        netActionHandler = new TestNetActionHandler();
        return Test.TEST_STATUS_PASS;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public void messageOut(String msg, boolean passed)
    {
        testLogger.log(" " + msg + "  " + (passed ? pass : fail));
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private boolean runTest()
    {
        if (!getServices())
        {
            messageOut("ContentServerNetModule Not Found.", FAIL);

            return FAIL;
        }
        else
        {
            messageOut("ContentServerNetModule Found.", PASS);
        }

        netActionRequest = contentServerNetModule.requestRootContainer(netActionHandler);

        if (!netActionHandler.waitRequestResponse())
        {
            return false;
        }

        ContentContainer rootContentContainer = getRootContainerFromEvent(netActionHandler.getNetActionEvent());

        if (rootContentContainer == null)
        {
            messageOut("********* " + getName() + " Failed to Get RootContainer. ***********", FAIL);

            return false;
        }
        else
        {
            boolean state = true;
            String field = "dc:title";
            String field2 = "id";

            String value1 = "A";
            String value2 = "B";
            String value3 = "C";

            createItem(rootContentContainer, value1);
            createItem(rootContentContainer, value2);
            createItem(rootContentContainer, value3);

            if (testQuery(rootContentContainer, field, DatabaseQuery.EQUALS, value2))
            {
                messageOut("********* " + getName() + " EQUALS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " EQUALS QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.GREATER_THAN, value2))
            {
                messageOut("********* " + getName() + " GREATER THAN QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " GREATER THAN QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.EXISTS, value2))
            {
                messageOut("********* " + getName() + " EXISTS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " EXISTS THAN QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.GREATER_THAN_OR_EQUALS, value1))
            {
                messageOut("********* " + getName() + " GREATER THAN OR EQUALS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " GREATER THAN OR EQUALS QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.LESS_THAN, value3))
            {
                messageOut("********* " + getName() + " LESS THAN QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " LESS THAN QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.LESS_THAN_OR_EQUALS, value3))
            {
                messageOut("********* " + getName() + " LESS THAN OR EQUALS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " LESS THAN OR EQUALS QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQuery(rootContentContainer, field, DatabaseQuery.NOT_EQUALS, value2))
            {
                messageOut("********* " + getName() + " NOT EQUALS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " NOT EQUALS QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (createQuery(field, -1, value2) == null)
            {
                messageOut("********* " + getName() + " QUERY EXCEPTION PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " QUERY EXCEPTION FAILED. ***********", FAIL);
                state = false;
            }

            String value4 = "D";
            String value5 = "E";
            String value6 = "F";
            String value7 = "This a test of contains";

            createItem(rootContentContainer, value4);
            createItem(rootContentContainer, value5);
            createItem(rootContentContainer, value6);

            if (testQueryOrQuery(rootContentContainer, field, DatabaseQuery.NOT_EQUALS, value2, field,
                    DatabaseQuery.EQUALS, value5))
            {
                messageOut("********* " + getName() + " QUERY OR QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " QUERY OR QUERY FAILED. ***********", FAIL);
                state = false;
            }

            if (testQueryAndQuery(rootContentContainer, field, DatabaseQuery.GREATER_THAN, value2, field2,
                    DatabaseQuery.NOT_EQUALS, "6"))
            {
                messageOut("********* " + getName() + " QUERY AND QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " QUERY AND QUERY FAILED. ***********", FAIL);
                state = false;
            }

            createItem(rootContentContainer, value7);

            if (testQuery(rootContentContainer, field, DatabaseQuery.CONTAINS, "test"))
            {
                messageOut("********* " + getName() + " CONTAINS QUERY PASSED. ***********", PASS);
            }
            else
            {
                messageOut("********* " + getName() + " CONTAINS QUERY FAILED. ***********", FAIL);
                state = false;
            }

            return state;
        }
    }

    public boolean testQuery(ContentContainer container, String field, int query, String value)
    {

        DatabaseQuery dbQuery = createQuery(field, query, value);

        if (dbQuery == null)
        {
            messageOut("********* " + getName() + "DATABASEQUERY IS NULL", FAIL);
            return false;
        }

        ContentList cList = container.getEntries(dbQuery, false);

        if (!cList.hasMoreElements())
        {

            messageOut("********* " + getName() + "CONTENTLIST IS EMPTY", FAIL);
            return false;
        }

        while (cList.hasMoreElements())
        {
            ContentEntry cEntry = (ContentEntry) cList.nextElement();

            if (cEntry != null)
            {
                System.out.println("CONTENT-ENTRY ID: " + cEntry.getID());
            }
            else
            {
                System.err.println("error: incorrect filtered content");
                return false;
            }
        }

        return true;
    }

    public boolean testQueryAndQuery(ContentContainer container, String pfield, int pquery, String pvalue,
            String sfield, int squery, String svalue)
    {

        DatabaseQuery dbQuery_pri = createQuery(pfield, pquery, pvalue);
        DatabaseQuery dbQuery_sec = createQuery(sfield, squery, svalue);

        if ((dbQuery_pri == null) || (dbQuery_sec == null))
        {
            messageOut("********* " + getName() + "DATABASEQUERIES IS NULL", FAIL);
            return false;
        }

        try
        {
            dbQuery_pri = dbQuery_pri.and(dbQuery_sec);
        }
        catch (DatabaseException e)
        {
            messageOut("********* " + getName() + " DATABASEQUERY EXCEPTION - MESSAGE: " + e.getMessage()
                    + "EXCEPTION CODE: " + e.getExceptionNumber() + " ***********", FAIL);
            return false;
        }

        ContentList cList = container.getEntries(dbQuery_pri, false);

        if (!cList.hasMoreElements())
        {

            messageOut("********* " + getName() + "CONTENTLIST IS EMPTY", FAIL);
            return false;
        }

        while (cList.hasMoreElements())
        {
            ContentEntry cEntry = (ContentEntry) cList.nextElement();

            if (cEntry != null)
            {
                System.out.println("CONTENT-ENTRY ID: " + cEntry.getID());
            }
            else
            {
                System.err.println("error: incorrect filtered content");
                return false;
            }
        }

        return true;
    }

    public boolean testQueryOrQuery(ContentContainer container, String pfield, int pquery, String pvalue,
            String sfield, int squery, String svalue)
    {

        DatabaseQuery dbQuery_pri = createQuery(pfield, pquery, pvalue);
        DatabaseQuery dbQuery_sec = createQuery(sfield, squery, svalue);

        if ((dbQuery_pri == null) || (dbQuery_sec == null))
        {
            messageOut("********* " + getName() + "DATABASEQUERIES IS NULL", FAIL);
            return false;
        }

        try
        {
            dbQuery_pri = dbQuery_pri.or(dbQuery_sec);
        }
        catch (DatabaseException e)
        {
            messageOut("********* " + getName() + " DATABASEQUERY EXCEPTION - MESSAGE: " + e.getMessage()
                    + "EXCEPTION CODE: " + e.getExceptionNumber() + " ***********", FAIL);
            return false;
        }

        ContentList cList = container.getEntries(dbQuery_pri, false);

        if (!cList.hasMoreElements())
        {

            messageOut("********* " + getName() + "CONTENTLIST IS EMPTY", FAIL);
            return false;
        }

        while (cList.hasMoreElements())
        {
            ContentEntry cEntry = (ContentEntry) cList.nextElement();

            if (cEntry != null)
            {
                System.out.println("CONTENT-ENTRY ID: " + cEntry.getID());
            }
            else
            {
                System.err.println("error: incorrect filtered content");
                return false;
            }
        }

        return true;
    }

    private void createItem(ContentContainer container, String name)
    {
        int[] readOrg = { 1 };
        int[] writeOrg = { 1 };

        ExtendedFileAccessPermissions eFap = new ExtendedFileAccessPermissions(true, true, true, true, true, true,
                readOrg, writeOrg);

        container.createContentItem(null, name, null);
    }

    private void createContainer(ContentContainer container, String name)
    {
        container.createContentContainer(name, null);
    }

    private DatabaseQuery createQuery(String field, int operator, String value)
    {
        DatabaseQuery dbQuery = null;

        try
        {
            messageOut("********* " + getName() + " DATABASEQUERY CREATION " + " ***********", PASS);
            dbQuery = DatabaseQuery.newInstance(field, operator, value);
        }
        catch (DatabaseException e)
        {
            messageOut("********* " + getName() + " DATABASEQUERY EXCEPTION - MESSAGE: " + e.getMessage()
                    + "EXCEPTION CODE: " + e.getExceptionNumber() + " ***********", FAIL);
        }

        return dbQuery;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private boolean getServices()
    {
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);

        boolean status = FAIL;

        if (serviceList.size() <= 0)
        {
            try
            {
                Thread.sleep(30000);
            }
            catch (InterruptedException e)
            {

            }

            serviceList = NetManager.getInstance().getNetModuleList(null);
        }

        if (serviceList.size() == 0)
        {
            messageOut("NetManager returned 0 size NetModuleList", FAIL);
            return status;
        }

        Enumeration serviceEnum = serviceList.getElements();

        while (serviceEnum.hasMoreElements())
        {
            Object obj = serviceEnum.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                contentServerNetModule = (ContentServerNetModule) obj;

                if (contentServerNetModule.isLocal())
                {
                    messageOut("Local ContentServerNetModule Found.", PASS);
                    status = PASS;
                    break;
                }
                else
                {
                    messageOut("Non-Local ContentServerNetModule Found.", PASS);
                }
            }
        }

        return status;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param event
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private ContentContainer getRootContainerFromEvent(NetActionEvent event)
    {
        if (event == null)
        {
            return null;
        }

        NetActionRequest receivedNetActionRequest = event.getActionRequest();
        int receivedActionStatus = event.getActionStatus();
        int receivedError = event.getError();
        Object receivedResponse = event.getResponse();

        if (receivedNetActionRequest != this.netActionRequest)
        {
            messageOut(getName() + " Recieved NetActionRequest is not the same as the request return value. error: "
                    + receivedError, FAIL);

            return null;
        }

        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            messageOut("The NetActionRequest returned ActionStatus = COMPLETED", PASS);

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                messageOut("ACTION_CANCELED ", PASS);
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                messageOut("ACTION_FAILED ", FAIL);
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                messageOut("ACTION_IN_PROGRESS ", PASS);
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                messageOut("ACTION_STATUS_NOT_AVAILABLE.", PASS);
            }
            else
            {
                messageOut("UNKONWN ACTION STATUS.", PASS);
            }

            return null;
        }

        if (!(receivedResponse instanceof ContentContainer))
        {
            messageOut("Did not get the root container of ContentContainer type.", FAIL);

            return null;
        }

        return (ContentContainer) receivedResponse;
    }
}
