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
package org.cablelabs.xlet.RemoteServiceSelection;

import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.navigation.ContentList;

import java.net.InetAddress;
import java.util.Enumeration;

public class HNUtil
{

    // TODO: use UUID to resolve contentservernetmodule - clarify format of uuid
    public ContentItem doGetContentItemFromID(String uuid, String contentItemID)
    {

        NetActionHandlerImpl netActionHandler = new NetActionHandlerImpl();
        // initiate request and wait for result
        getContentServerNetModuleByUuid(uuid).requestBrowseEntries(contentItemID, "*", false, 0, 1, "",
                netActionHandler);
        ContentList list = netActionHandler.waitForContentList();
        ContentEntry contentEntry = (ContentEntry) list.nextElement();
        if (!(contentEntry instanceof ContentItem))
        {
            System.out.println("contentList first element is not a contentItem: " + contentEntry);
            return null;
        }
        return (ContentItem) contentEntry;
    }

    public ContentList browseEntries(ContentServerNetModule contentServerNetModule, String startingContentId,
            String propertyFilter, boolean browseChildren, int startingIndex, int requestedCount, String sortCriteria)
    {
        NetActionHandlerImpl netActionHandler = new NetActionHandlerImpl();

        System.out.println("Issuing browse request to ContentServerNet Module uuid "
                + contentServerNetModule.getDevice().getProperty("UDN") + ", ip addr "
                + contentServerNetModule.getDevice().getInetAddress());

        contentServerNetModule.requestBrowseEntries(startingContentId, propertyFilter, browseChildren, startingIndex,
                requestedCount, sortCriteria, netActionHandler);
        return netActionHandler.waitForContentList();
    }

    public ContentList searchRootContainer(ContentServerNetModule contentServerNetModule)
    {
        NetActionHandlerImpl netActionHandler = new NetActionHandlerImpl();

        System.out.println("Issuing search request to ContentServerNet Module uuid "
                + contentServerNetModule.getDevice().getProperty("UDN") + ", ip addr "
                + contentServerNetModule.getDevice().getInetAddress());

        contentServerNetModule.requestSearchEntries("0", "*", 0, 0, null, "", netActionHandler);
        return netActionHandler.waitForContentList();
    }

    public ContentServerNetModule getContentServerNetModuleByUuid(String uuid)
    {
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);
        Enumeration serviceEnum = serviceList.getElements();
        while (serviceEnum.hasMoreElements())
        {
            Object obj = serviceEnum.nextElement();
            if (obj instanceof ContentServerNetModule)
            {
                ContentServerNetModule module = (ContentServerNetModule) obj;
                if (module.getProperty(NetModule.PROP_NETMODULE_ID).equals(uuid))
                {
                    return module;
                }
            }
        }
        System.out.println("no contentServerNetModule found");
        return null;
    }

    public ContentServerNetModule getContentServerNetModuleByNameAndAddress(String name, String inetAddressString)
    {
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);
        Enumeration serviceEnum = serviceList.getElements();
        while (serviceEnum.hasMoreElements())
        {
            Object obj = serviceEnum.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                ContentServerNetModule module = (ContentServerNetModule) obj;
                InetAddress inetAddress = module.getDevice().getInetAddress();
                if (module.getDevice().getName().equalsIgnoreCase(name) && !module.getDevice().isLocal()
                        && inetAddress!= null && inetAddress.getHostAddress().equals(inetAddressString))
                {
                    return module;
                }
            }
        }
        System.out.println("unable to find remote ContentServerNetModule with name: " + name + " and address: "
                + inetAddressString + " - examined services: " + serviceList);
        return null;
    }

    protected static class NetActionHandlerImpl implements NetActionHandler
    {
        private static final long TIMEOUT = 10000;

        private static final long WAIT_MILLIS = 500;

        private final Object signal = new Object();

        NetActionEvent netActionEvent;

        private boolean signaled;

        public void notify(NetActionEvent event)
        {
            synchronized (signal)
            {
                signaled = true;
                netActionEvent = event;
                signal.notifyAll();
            }
        }

        public ContentList waitForContentList()
        {
            System.out.println("Waiting for browse request response");

            // may have received a 'notify' call before we began to wait
            synchronized (signal)
            {
                while (true)
                {
                    long startTime = System.currentTimeMillis();
                    // we may receive multiple in-progress events...only return
                    // when we receive a completed event or time out

                    // loop on condition variable
                    while (!signaled)
                    {
                        // break out if past timeout
                        if (System.currentTimeMillis() - startTime > TIMEOUT)
                        {
                            // we timed out, return null
                            System.out.println("timed out");
                            return null;
                        }
                        try
                        {
                            signal.wait(WAIT_MILLIS);
                        }
                        catch (InterruptedException ie)
                        {
                            // no-op
                        }
                    }
                    // reset so we can call this method in a loop waiting for
                    // new events
                    System.out.println("received event: " + netActionEvent);
                    // we've received event...reset signaled flag
                    signaled = false;

                    // if we've received an in-progress event, wait for another
                    // event
                    if (netActionEvent.getActionStatus() == NetActionEvent.ACTION_IN_PROGRESS)
                    {
                        System.out.println("Request is in progress");
                        continue;
                    }
                    // if we've received a failed event, return null
                    if (netActionEvent.getActionStatus() == NetActionEvent.ACTION_FAILED)
                    {
                        System.out.println("Request failed");
                        return null;
                    }

                    // assuming we have a content item we can now return
                    Object response = netActionEvent.getResponse();
                    if (response == null)
                    {
                        System.out.println("Request response is null");
                        return null;
                    }
                    if (!(response instanceof ContentList))
                    {
                        System.out.println("Request response is not a ContentList: " + response);
                        return null;
                    }
                    System.out.println("Request response content list received");
                    return (ContentList) response;
                }
            }
        }
    }
}
