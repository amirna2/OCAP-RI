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

import java.io.File;
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
import org.ocap.storage.ExtendedFileAccessPermissions;

public class ContentListTest extends Test
{
    /** DOCUMENT ME! */
    private TestNetActionHandler netActionHandler = null;

    /** DOCUMENT ME! */
    private ContentServerNetModule contentServerNetModule = null;

    /** DOCUMENT ME! */
    private NetActionRequest netActionRequest = null;

    /** DOCUMENT ME! */
    private String className = "ContentListTest";

    /** DOCUMENT ME! */
    private String description = "Test ContentList Functionality";

    /** DOCUMENT ME! */
    private String pass = "PASS";

    /** DOCUMENT ME! */
    private String fail = "FAIL";

    /** DOCUMENT ME! */
    private boolean PASS = true;

    /** DOCUMENT ME! */
    private boolean FAIL = !PASS;

    /** DOCUMENT ME! */
    private final int ITEM_COUNT = 5;

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
            messageOut("Failed to Get RootContainer.", FAIL);

        }
        else
        {
            messageOut("Received RootContainer.", PASS);

            ContentContainer container = null;
            ContentItem item = null;
            ContentList list = null;

            if (!createContainer(rootContentContainer, "TEST CONTAINER"))
            {
                messageOut("CREATING CONTAINER " + "\"" + "TEST CONTAINER" + "\"", FAIL);
                return state;
            }

            if ((container = browseContainer(rootContentContainer, "TEST CONTAINER")) == null)
            {
                messageOut("BROWSING CONTAINER " + "\"" + "TEST CONTAINER" + "\"", FAIL);
                return state;
            }
            else
            {
                for (int i = 0; i < ITEM_COUNT; ++i)
                {
                    if (!createItem(container, ("TEST ITEM:-" + i)))
                    {
                        messageOut("CREATING ITEM " + "\"" + "TEST ITEM:-" + i + "\"", FAIL);
                        return state;
                    }
                }
            }

            if (!createItem(container, "TEST ITEM"))
            {
                messageOut("CREATING ITEM " + "\"" + "TEST ITEM" + "\"", FAIL);
                return state;
            }

            if ((item = browseItem(container, "TEST ITEM")) == null)
            {
                messageOut("BROWSING ITEM " + "\"" + "TEST ITEM" + "\"", FAIL);
                return state;
            }

            state = PASS;
        }

        return state;
    }

    private boolean createContainer(ContentContainer container, String containerName)
    {
        messageOut("CREATINING CONTAINER " + "\"" + containerName + "\"", PASS);
        boolean state = false;

        if (container.createContentContainer(containerName, null))
        {
            messageOut("CONTAINER " + "\"" + containerName + "\"" + " CREATED.", PASS);
            state = true;
        }

        return state;
    }

    private ContentContainer browseContainer(ContentContainer inContainer, String containerName)
    {
        messageOut("BROWSING FOR CONTAINER " + "\"" + containerName + "\"", PASS);

        ContentEntry contentEntry = null;
        ContentContainer container = null;

        Enumeration enumContainer = inContainer.getEntries();

        while ((enumContainer != null) && enumContainer.hasMoreElements())
        {
            contentEntry = (ContentEntry) enumContainer.nextElement();

            if (contentEntry instanceof ContentContainer)
            {
                String name = ((ContentContainer) contentEntry).getName();
                if (name.equalsIgnoreCase(containerName))
                {
                    messageOut("CONTAINER " + "\"" + name + "\"" + " BROWSED FROM CONTAINER " + "\""
                            + inContainer.getName() + "\"", PASS);
                    container = ((ContentContainer) contentEntry);
                }
            }
        }

        return container;
    }

    private boolean createItem(ContentContainer container, String itemName)
    {
        messageOut("CREATING CONTENT ITEM " + "\"" + itemName + "\"", PASS);

        boolean state = false;
        File file = new File("c:\test.png");
        int[] readOrg = { 1 };
        int[] writeOrg = { 1 };

        ExtendedFileAccessPermissions eFap = new ExtendedFileAccessPermissions(state, state, state, state, state,
                state, readOrg, writeOrg);

        if (container.createContentItem(file, itemName, null)) ;
        {
            messageOut("CONTENT ITEM " + "\"" + itemName + "\"" + " CREATED.", PASS);
            state = true;
        }

        return state;
    }

    private ContentItem browseItem(ContentContainer container, String itemName)
    {
        ContentEntry contentEntry = null;
        ContentItem item = null;

        Enumeration enumContainer = container.getEntries();

        while ((enumContainer != null) && enumContainer.hasMoreElements())
        {
            contentEntry = (ContentEntry) enumContainer.nextElement();

            if (contentEntry instanceof ContentItem)
            {
                String name = ((ContentItem) contentEntry).getTitle();
                if (name.equalsIgnoreCase(itemName))
                {
                    messageOut("CONTENT ITEM " + "\"" + name + "\"" + " BROWSED FROM CONTAINER " + "\""
                            + container.getID() + "\"", PASS);
                    printContentItem(item);
                    item = (ContentItem) contentEntry;
                }
            }
        }

        return item;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param container
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private boolean printContentContainer(ContentContainer container)
    {
        boolean state = false;

        if (container != null)
        {
            messageOut("CONTAINER ID         :  " + container.getID());
            messageOut("CONTAINER NAME       :  " + container.getName());
            messageOut("CONTAINER CLASS      :  " + container.getContainerClass());
            messageOut("CONTAINER SIZE       :  " + container.getContentSize());
            messageOut("CONTAINER PARENT ID  :  " + container.getParentID());
            messageOut("CONTAINER DATE       :  " + container.getCreationDate());
            messageOut("CONTAINER COMPONENT# :  " + container.getComponentCount());

            state = true;
        }

        return state;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param container
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private boolean printContentItem(ContentItem item)
    {
        boolean state = false;

        if (item != null)
        {
            messageOut("ITEM ID              :  " + item.getID());
            messageOut("ITEM TITLE           :  " + item.getTitle());
            messageOut("ITEM CLASS           :  " + item.getContentClass());
            messageOut("ITEM SIZE            :  " + item.getContentSize());
            messageOut("ITEM PARENT ID       :  " + item.getParentID());
            messageOut("ITEM DATE            :  " + item.getCreationDate());
            messageOut("ITEM RESOURCE COUNT  :  " + item.getResourceCount());

            state = true;
        }

        return state;
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
