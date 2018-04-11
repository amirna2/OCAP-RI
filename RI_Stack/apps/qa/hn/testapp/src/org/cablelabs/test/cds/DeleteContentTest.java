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

import java.io.IOException;
import java.util.Enumeration;

import org.cablelabs.test.Test;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;

public class DeleteContentTest extends Test
{

    /** DOCUMENT ME! */
    private TestNetActionHandler netActionHandler = null;

    /** DOCUMENT ME! */
    private ContentServerNetModule contentServerNetModule = null;

    /** DOCUMENT ME! */
    private NetActionRequest netActionRequest = null;

    /** DOCUMENT ME! */
    private String className = "DeleteContainerTest";

    /** DOCUMENT ME! */
    private String description = "Deletes a Local ContentContainer's ContentItems";

    /** DOCUMENT ME! */
    private String pass = "PASS";

    /** DOCUMENT ME! */
    private String fail = "FAIL";

    /** DOCUMENT ME! */
    private boolean PASS = true;

    /** DOCUMENT ME! */
    private boolean FAIL = !PASS;

    public int clean()
    {

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

    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return className;
    }

    public int prepare()
    {
        netActionHandler = new TestNetActionHandler();
        return Test.TEST_STATUS_PASS;
    }

    public void messageOut(String msg, boolean passed)
    {
        testLogger.log(" " + msg + "  " + (passed ? pass : fail));
    }

    public void messageOut(String msg)
    {
        testLogger.log(" " + msg);
    }

    private boolean runTest()
    {
        boolean state = FAIL;

        if (!getServices())
        {
            messageOut("ContentServerNetModule Not Found.", FAIL);

            return state;
        }
        else
        {
            messageOut("ContentServerNetModule Found.", PASS);
        }

        netActionRequest = contentServerNetModule.requestRootContainer(netActionHandler);

        if (!netActionHandler.waitRequestResponse())
        {
            return state;
        }

        ContentContainer rootContentContainer = getRootContainerFromEvent(netActionHandler.getNetActionEvent());

        if (rootContentContainer == null)
        {
            messageOut("********* " + getName() + " Failed to Get RootContainer. ***********", FAIL);

        }
        else
        {
            messageOut("********* " + getName() + " Received RootContainer. ***********", PASS);

            if (rootContentContainer.createContentContainer("TEST_DELETE_CONTAINER", null))
            {
                messageOut("********* " + getName() + " CONTAINER " + "\"" + "TEST_DELETE_CONTAINER" + "\""
                        + " SUCCESSFULLY CREATED. ***********", PASS);
            }

            Enumeration enumContainer = rootContentContainer.getEntries();
            ContentEntry contentEntry = null;

            while ((enumContainer != null) && enumContainer.hasMoreElements())
            {
                contentEntry = (ContentEntry) enumContainer.nextElement();

                if (contentEntry instanceof ContentContainer)
                {
                    String name = ((ContentContainer) contentEntry).getName();
                    if (name.equalsIgnoreCase("TEST_DELETE_CONTAINER"))
                    {
                        messageOut("CONTAINER NAME: " + ((ContentContainer) contentEntry).getName());
                        messageOut("********* " + getName() + " CONTAINER " + "\"" + name + "\""
                                + " SUCCESSFULLY BROWSED FROM ROOT CONTAINER. ***********", PASS);

                        state = PASS;
                    }
                }
            }

            if (state)
            {
                if (((ContentContainer) contentEntry).createContentItem(null, "TEST_CONTENT_ITEM_1", null)
                        && ((ContentContainer) contentEntry).createContentItem(null, "TEST_CONTENT_ITEM_2", null))
                {

                    if (!(((ContentContainer) contentEntry).getComponentCount() == 2))
                    {
                        messageOut("********* ERROR: TEST ITEMS NOT CREATED ***********", FAIL);
                        return FAIL;
                    }
                    try
                    {

                        ((ContentContainer) contentEntry).deleteContents();

                    }
                    catch (IOException e)
                    {
                        messageOut("********* ERROR: IOEXCEPTION ***********", FAIL);
                        state = FAIL;
                    }

                    if (((ContentContainer) contentEntry).getComponentCount() == 0)
                    {
                        messageOut("********* SUCCESS: TEST ITEMS WERE DELETED ***********", PASS);
                        state = PASS;

                    }
                    else
                    {

                        messageOut("********* ERROR: TEST ITEMS NOT DELETED ***********", FAIL);
                        state = FAIL;

                    }
                }
            }

            if (state)
            {
                enumContainer = rootContentContainer.getEntries();
                contentEntry = null;

                state = FAIL;

                while ((enumContainer != null) && enumContainer.hasMoreElements())
                {
                    contentEntry = (ContentEntry) enumContainer.nextElement();

                    if (contentEntry instanceof ContentContainer)
                    {
                        String name = ((ContentContainer) contentEntry).getName();
                        if (name.equalsIgnoreCase("TEST_DELETE_CONTAINER"))
                        {
                            messageOut("CONTAINER NAME: " + ((ContentContainer) contentEntry).getName());
                            messageOut("********* " + getName() + " CONTAINER " + "\"" + name + "\""
                                    + " SUCCESSFULLY BROWSED FROM ROOT CONTAINER. ***********", PASS);

                            state = PASS;
                        }
                    }
                }

                if (!state)
                {

                    messageOut("********* ERROR: CONTAINER HAS BEEN REMOVED ***********", FAIL);
                }
            }

            if (state)
            {
                if (((ContentContainer) contentEntry).createContentItem(null, "TEST_CONTENT_ITEM_1", null)
                        && ((ContentContainer) contentEntry).createContentItem(null, "TEST_CONTENT_ITEM_2", null))
                {

                    if (!(((ContentContainer) contentEntry).getComponentCount() == 2))
                    {
                        messageOut("********* ERROR: TEST ITEMS NOT CREATED ***********", FAIL);
                        return FAIL;
                    }
                    try
                    {

                        state = ((ContentContainer) contentEntry).delete();

                    }
                    catch (IOException e)
                    {
                        messageOut("********* SUCCESS: IOEXCEPTION OCCURED AS EXPECTED ***********", PASS);
                        state = PASS;
                    }

                    if (state)
                    {
                        messageOut("********* SUCCESS: TEST ITEMS AND CONTAINER WERE NOT DELETED ***********", PASS);
                        state = PASS;

                    }
                    else
                    {

                        messageOut("********* ERROR: TEST ITEMS AND CONTAINER WERE DELETED ***********", FAIL);
                        state = FAIL;

                    }
                }

            }

        }

        return state;
    }

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
