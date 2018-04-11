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

import org.cablelabs.test.Test;

import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.ContentContainer;

import java.util.Enumeration;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision$
 * 
 * @see
 */
public class CreateItemTest extends Test
{
    /** DOCUMENT ME! */
    private MyNetActionHandler netActionHandler = new MyNetActionHandler();

    /** DOCUMENT ME! */
    private String failReason = "";

    /** DOCUMENT ME! */
    private ContentServerNetModule contentServerNetModule = null;

    /** DOCUMENT ME! */
    private NetActionRequest netActionRequest = null;

    /** DOCUMENT ME! */
    private String className = "CreateItemTest";

    /** DOCUMENT ME! */
    private String description = "Creates a CDS ContentItem from a ContentContainer";

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int clean()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public int execute()
    {
        testLogger.setPrefix(className);
        testLogger.log(" start");

        if (runTest())
        {
            testLogger.log(" pass");

            return Test.TEST_STATUS_PASS;
        }
        else
        {
            testLogger.log(" fail");

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
        return 0;
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
            exit("Could not find local SRS or CDS NetModules. Reason: " + this.failReason);

            return false;
        }

        this.netActionRequest = this.contentServerNetModule.requestRootContainer(this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            exit("Request response failed. Reason: " + this.netActionHandler.getFailReason());

            return false;
        }

        ContentContainer rootContentContainer = getRootContainerFromEvent(this.netActionHandler.getNetActionEvent());

        if (rootContentContainer == null)
        {
            exit("Failed to getRootContainer. Reason: " + this.failReason);

            return false;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
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
            this.failReason = "NetManager returned 0 size NetModuleList";

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

                testLogger.log(" found a ContentServerNetModule, isLocal = "
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
     * @param reason
     *            DOCUMENT ME!
     */
    private void exit(String reason)
    {
        testLogger.log(" ContentContainerAddTest: FAIL: " + reason);

        setFailedDescription(reason);
        netActionHandler = null;
        contentServerNetModule = null;
        netActionRequest = null;
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

        if (!(receivedResponse instanceof ContentContainer))
        {
            failReason = "Did not get the root container of ContentContainer type.";

            return null;
        }

        return (ContentContainer) receivedResponse;
    }

    private class MyNetActionHandler implements NetActionHandler
    {
        private static final long TIMEOUT = 5000;

        private NetActionEvent netActionEvent = null;

        private boolean receivedNetActionEvent = false;

        private Object signal = new Object();

        private String failReason = "";

        public void notify(NetActionEvent arg0)
        {
            testLogger.log(" got notify to MyNetActionHandler");

            this.netActionEvent = arg0;

            this.receivedNetActionEvent = true;

            this.signal.notifyAll();
        }

        public boolean waitRequestResponse()
        {
            testLogger.log(" wait for notify to MyNetActionHandler");

            if (!this.receivedNetActionEvent)
            {
                try
                {
                    synchronized (this.signal)
                    {
                        this.signal.wait(TIMEOUT);
                    }
                }
                catch (InterruptedException e)
                {
                }
            }

            if (!this.receivedNetActionEvent)
            {
                failReason = "Failed to get NetActionEvent within " + this.TIMEOUT + " milliseconds";

                return false;
            }

            testLogger.log(" got NetActionEvent to MyNetActionHandler");

            return true;
        }

        public NetActionEvent getNetActionEvent()
        {
            return this.netActionEvent;
        }

        public String getFailReason()
        {
            return this.failReason;
        }
    }
}
