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
package org.cablelabs.impl.util;

import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.transformation.TransformationManager;


public class HNUtil
{
    private static final Logger log = Logger.getLogger(HNUtil.class);

    public ContentItem getContentItemFromID(String uuid, String contentItemID)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getContentItemFromId - uuid: " + uuid + ", contentItemID: " + contentItemID);
        }
        NetActionHandlerImpl netActionHandler = new NetActionHandlerImpl();
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);
        Enumeration serviceEnum = serviceList.getElements();
        ContentServerNetModule contentServerNetModule = null;
        while (serviceEnum.hasMoreElements())
        {
            Object obj = serviceEnum.nextElement();
            if (obj instanceof ContentServerNetModule)
            {
                String deviceId = ((ContentServerNetModule) obj).getDevice().getProperty(Device.PROP_UDN);
                // strip off uuid:
                int idx = deviceId.indexOf(":");
                deviceId = deviceId.substring(idx+1);
                if (deviceId.equals(uuid))
                {
                    contentServerNetModule = (ContentServerNetModule) obj;
                    break;
                }
            }
        }
        if (contentServerNetModule == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("no contentServerNetModule found");
            }
            return null;
        }
        // initiate request and wait for result
        contentServerNetModule.requestBrowseEntries(contentItemID, "*", false, 0, 1, "", netActionHandler);
        return netActionHandler.waitForContentItem();
    }
    
    /**
     * This will search for a "transform=<id>" query parameter, extract the numeric transformation 
     * id, query the TransformationManager for a NativeContentTransform corresponding to the ID,
     * and return it, if found. If the parameter cannot be found, the parameter is ill-formed,
     * or no transformation is associated with the ID, this method will return null.
     * 
     * @param query The <code>query</code> portion of the URI to be scanned.
     * 
     * @return The NativeContentTransformation associated with the ID or null of the query 
     *         doesn't contain a transformation ID that can be associated with the ID.
     */
    public static NativeContentTransformation getTransformationFromURIQuery(final String query)
    {
        NativeContentTransformation nativeTransform = null;
        // Look for transformation ID (optional param)
        final String transformParamPrefix = ContentDirectoryService.REQUEST_URI_TRANSFORMATION_PREFIX;
        final int paramIndex = query.indexOf(transformParamPrefix);
        if (paramIndex > 0)
        {
            // ID should be everything between "blah=" and the next '&' or end-of-string
            String transformationIDStr 
                       = query.substring(paramIndex + transformParamPrefix.length());
            final int endIndex = transformationIDStr.indexOf('&');
            if (endIndex > 0)
            {
                transformationIDStr = transformationIDStr.substring(0, endIndex);
            }
            try 
            {
                final int transformationID = Integer.parseInt(transformationIDStr);
                if (log.isInfoEnabled())
                {
                    log.info("getTransformationFromURIQuery: request is for transformation ID: " + transformationID);
                }
                TransformationManagerImpl tm = (TransformationManagerImpl)
                                               (TransformationManagerImpl.getInstanceRegardless());
                final OutputVideoContentFormatExt 
                          ovcfe = tm.getOutputContentFormatForID(transformationID);
                if (ovcfe != null)
                {
                    nativeTransform = ovcfe.getNativeTransformation();
                    if (log.isInfoEnabled())
                    {
                        log.info("getTransformationFromURIQuery: native transformation for request: " 
                                 + nativeTransform);
                    }
                }
            }
            catch (NumberFormatException nfe)
            { // This means that the transformation ID was not parseable, but it matched
              //  a res URI from the CDS (above). So this would mean we generated a URI 
              //  with a bad transformation parameter, which shouldn't happen
                if (log.isWarnEnabled())
                {
                    log.warn( "getTransformationFromURIQuery: Error parsing transformation ID " + transformationIDStr, nfe); 
                }
            }
        }
        
        return nativeTransform;
    } // END getTransformationFromURIQuery()
    
    /**
     * Return the path/query/fragment portion of the given URI (everything after "http://authority/")
     * or null if a null URI string is provided.
     * 
     * @param uri The URI to pull the path from
     * @return The path or null
     */
    public static String getPathFromHttpURI(final String uri)
    {
        // Return everything after (and including) the third slash
        // "http://" is (and always will be) 7 characters. And we only support http here
        return (uri == null) ? null : uri.substring(uri.indexOf('/', 7));
    }
    
    /**
     * Convert an <code>Object</code>, which must be a <code>String</code> or a
     * <code>Long</code>, to a <code>Long</code>.
     * <p>
     *
     * @param o
     *            The object.
     *            <p>
     * @return A reference to the <code>Long</code> if the <code>Object</code>
     *         is a <code>Long</code> or a <code>String</code> that can be
     *         converted to a <code>Long</code>; else null.
     */
    public static Long toLong(Object o)
    {
        if (o instanceof Long)
        {
            return (Long) o;
        }

        if (o instanceof String)
        {
            try
            {
                return new Long((String) o);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        return null;
    }

    protected static class NetActionHandlerImpl implements NetActionHandler
    {
        private static final long TIMEOUT = 10000;

        private static final long WAIT_MILLIS = 500;

        private final Object signal = new Object();

        private NetActionEvent netActionEvent;

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

        public ContentItem waitForContentItem()
        {
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
                            if (log.isDebugEnabled())
                            {
                                log.debug("timed out");
                            }
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
                    if (log.isDebugEnabled())
                    {
                        log.debug("received event: " + netActionEvent);
                    }
                    // we've received event...reset signaled flag
                    signaled = false;

                    // if we've received an in-progress event, wait for another
                    // event
                    if (netActionEvent.getActionStatus() == NetActionEvent.ACTION_IN_PROGRESS)
                    {
                        continue;
                    }
                    // if we've received a failed event, return null
                    if (netActionEvent.getActionStatus() == NetActionEvent.ACTION_FAILED)
                    {
                        return null;
                    }

                    // assuming we have a contentitem we can now return
                    Object response = netActionEvent.getResponse();
                    if (response == null)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Request response is null");
                        }
                        return null;
                    }
                    if (!(response instanceof ContentList))
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Request response is not a ContentList: " + response);
                        }
                        return null;
                    }
                    ContentEntry contentEntry = (ContentEntry) ((ContentList) response).nextElement();
                    if (!(contentEntry instanceof ContentItem))
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("contentList first element is not a contentItem: " + contentEntry);
                        }
                        return null;
                    }
                    return (ContentItem) contentEntry;
                }
            }
        }
    }
}
