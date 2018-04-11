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
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.navigation.ContentList;

public class SearchContentItemsTest extends Test
{
    /** DOCUMENT ME! */
    private TestNetActionHandler netActionHandler = null;

    /** DOCUMENT ME! */
    private String failReason = "";

    /** DOCUMENT ME! */
    private ContentServerNetModule contentServerNetModule = null;

    /** DOCUMENT ME! */
    private NetActionRequest netActionRequest = null;

    /** DOCUMENT ME! */
    private String className = "SearchContentItemsTest";

    /** DOCUMENT ME! */
    private String description = "Searches the CDS for ContentContainers and ContentItems";

    /** DOCUMENT ME! */
    private String pass = "PASS";

    /** DOCUMENT ME! */
    private String fail = "FAIL";

    /** DOCUMENT ME! */
    private boolean PASS = true;

    /** DOCUMENT ME! */
    private boolean FAIL = !PASS;

    /** DOCUMENT ME! */
    private int startingIndex = 0;

    /** DOCUMENT ME! */
    private int requestedCount = 5;

    /** DOCUMENT ME! */
    private String parentId = "0";

    /** DOCUMENT ME! */
    private String propertyFilter = "";

    /** DOCUMENT ME! */
    private String searchCriteria = "";

    /** DOCUMENT ME! */
    private String sortCriteria = "";

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
        // TODO Auto-generated method stub
        return description;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getName()
    {
        // TODO Auto-generated method stub
        return className;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int prepare()
    {
        // TODO Auto-generated method stub
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

    public void messageOut(String msg)
    {
        testLogger.log(" " + msg);
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

        netActionRequest = contentServerNetModule.requestSearchEntries(parentId, propertyFilter, startingIndex,
                requestedCount, searchCriteria, sortCriteria, netActionHandler);

        if (!netActionHandler.waitRequestResponse())
        {
            return false;
        }

        ContentList contentList = getContentListFromEvent(netActionHandler.getNetActionEvent());

        if (contentList == null)
        {
            messageOut("********* " + getName() + " Failed to Get ContentList. ***********", FAIL);

            return false;
        }
        else
        {
            messageOut("********* " + getName() + " Received ContentList. ***********", PASS);
            messageOut("********* " + getName() + " ContentList Size: " + contentList.size() + " ***********",
                    (contentList.size() > 0));

            while (contentList.hasMoreElements())
            {
                ContentEntry contentEntry = (ContentEntry) contentList.nextElement();

                if (contentEntry instanceof ContentContainer)
                {
                    messageOut("********* FOUND CONTAINER ************");
                }
                else if (contentEntry instanceof ContentItem)
                {
                    messageOut("********* FOUND ITEM ************");
                }
                else
                {
                    messageOut("********* ERROR - NO ITEM OR CONTAINER FOUND ************");
                    return false;
                }
            }

            ContentEntry entry = contentList.find("dc:title", "root");

            if (entry != null)
            {
                messageOut("*********  FIND ENTRY BY KEY ***********");
                messageOut("ENTRY ID: " + entry.getID());
                messageOut("ENTRY PARENT ID: " + entry.getParentID());
            }

        }

        return true;
    }

    private boolean getServices()
    {
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);

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

            return false;
        }

        Enumeration serviceEnum = serviceList.getElements();

        while (serviceEnum.hasMoreElements())
        {
            Object obj = serviceEnum.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                contentServerNetModule = (ContentServerNetModule) obj;

                if (contentServerNetModule.isLocal() == false)
                {
                    contentServerNetModule = null;
                }

                System.out.println("found a ContentServerNetModule, isLocal = "
                        + String.valueOf(((ContentServerNetModule) obj).isLocal()));
            }
        }

        if (contentServerNetModule == null)
        {
            this.failReason = "could not find ContentServerNetModule";

            return false;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param event
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private ContentList getContentListFromEvent(NetActionEvent event)
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
            failReason = "Recieved NetActionRequest is not the same as the request return value.";

            return null;
        }

        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            failReason = "The NetActionRequest returned ActionStatus = ";

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                failReason += "ACTION_CANCELED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                failReason += "ACTION_FAILED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                failReason += "ACTION_IN_PROGRESS ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                failReason += "ACTION_STATUS_NOT_AVAILABLE ";
            }
            else
            {
                failReason += "UNKONWN ACTION STATUS ";
            }

            failReason += (" with Error value " + String.valueOf(receivedError));

            return null;
        }

        if (!(receivedResponse instanceof ContentList))
        {
            failReason = "Did not get the ContentList.";

            return null;
        }

        return (ContentList) receivedResponse;
    }
}
