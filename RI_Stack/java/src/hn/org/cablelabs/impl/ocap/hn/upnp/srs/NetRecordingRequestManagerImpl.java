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

package org.cablelabs.impl.ocap.hn.upnp.srs;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingNetModuleImpl;
import org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingRequestHandler;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.common.UPnPActionResponse;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * NetRecordingRequestManagerImpl - implementation class for
 * <code>NetRecordingRequestManager</code>.
 *
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * @author Dan Woodard (Flashlight Engineering and Consulting)
 *
 * @version $Revision$
 *
 * @see {@link NetRecordingRequestManager}
 */
public class NetRecordingRequestManagerImpl extends RecordingNetModuleImpl implements NetRecordingRequestManager
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(NetRecordingRequestManagerImpl.class);

    /**
     * permissions
     */
    private static final HomeNetPermission PERMISSION = new HomeNetPermission("recordinghandler");

    /**
     * Construct a <code>NetRecordingRequestManagerImpl</code>.
     */
    public NetRecordingRequestManagerImpl(UPnPClientService service)
    {
        super(service);
    }

    /**
     * Implements org.ocap.hn.recording.NetRecordingRequestManager.
     * createNetRecordingEntry()
     */
    public NetRecordingEntry createNetRecordingEntry() throws IOException
    {
        SecurityUtil.checkPermission(PERMISSION);

        // This should be impossible; isLocal() should always be true here.
        if (! isLocal())
        {
            throw new IOException("isLocal() returned false.");
        }

        return new NetRecordingEntryLocal(new MetadataNodeImpl());
    }

    /**
     * Implements org.ocap.hn.recording.NetRecordingRequestManager.
     * setNetRecordingRequestHandler()
     */
    public void setNetRecordingRequestHandler(NetRecordingRequestHandler handler)
    {
        SecurityUtil.checkPermission(PERMISSION);

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        this.netRecordingRequestHandler = handler;

        this.netRecordingRequestHandlerContext = ccm.getCurrentContext();
    }

    /**
     * Returns true if a handler has been registered, false if not.
     */
    public boolean isHandlerSet()
    {
        return this.netRecordingRequestHandler != null && this.netRecordingRequestHandlerContext != null;
    }

    public boolean notifyDelete(InetAddress address, ContentEntry recording)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyDelete() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final ContentEntry finalEntry = recording;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyDelete(finalAddress, finalEntry);
            }
        }); // block until complete

        return result[0];
    }

    /**
     * Finds a NetRecordingEntry based on the response from a
     * CreateRecordSchedule action.
     *
     * @param response The response.
     *
     * @return A reference to the NetRecordingEntry if the operation is successful; else null.
     */
    public NetRecordingEntry getNetRecordingEntry(UPnPActionResponse response)
    {
        String recordScheduleID = response.getArgumentValue(ScheduledRecordingService.RECORD_SCHEDULE_ID_ARG_NAME);

        if (recordScheduleID == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("A_ARG_TYPE_RecordSchedule recordScheduleID is null");
            }

            return null;
        }

        NetRecordingEntry netRecordingEntry = ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).getNetRecordingEntry(recordScheduleID);

        if (netRecordingEntry == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("received a local action response "
                + "but could not find the local instance of the NetRecordingEntryLocal "
                + "(recordScheduleID = '" + recordScheduleID + "')");
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("received a local action response "
                + "and found the local instance of the NetRecordingEntryLocal");
            }
        }

        return netRecordingEntry;
    }

    // //////////////////////////////////////////////////////////////////
    //
    // Package Private
    //
    // //////////////////////////////////////////////////////////////////

    boolean notifyDeleteService(InetAddress address, ContentEntry recording)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyDeleteService() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final ContentEntry finalEntry = recording;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyDeleteService(finalAddress, finalEntry);
            }
        }); // block until complete

        return result[0];
    }

    boolean notifyDisable(InetAddress address, ContentEntry recording)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyDisable() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final ContentEntry finalEntry = recording;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyDisable(finalAddress, finalEntry);
            }
        }); // block until complete

        return result[0];
    }

    boolean notifyPrioritization(InetAddress address, NetRecordingEntry[] recordings)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyPrioritization() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final NetRecordingEntry[] finalEntry = recordings;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyPrioritization(finalAddress, finalEntry);
            }
        }); // block until complete

        return result[0];
    }

    boolean notifyPrioritization(InetAddress address, RecordingContentItem[] recordings)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyPrioritization() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final RecordingContentItem[] finalEntry = recordings;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyPrioritization(finalAddress, finalEntry);
            }
        }); // block until complete

        return result[0];
    }

    boolean notifyReschedule(InetAddress address, ContentEntry recording, NetRecordingEntry spec)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyReschedule() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final ContentEntry finalEntry = recording;

        final NetRecordingEntry finalSpec = spec;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifyReschedule(finalAddress, finalEntry, finalSpec);
            }
        }); // block until complete

        return result[0];
    }

    /**
     * Calls NetRecordingRequestHandler.notifySchedule() if the handler has been
     * set to this NetRecordingRequestManager.
     *
     * @param address
     *            address of requesting control point
     * @param spec
     *            the NetRecordingEntry created by the SRS
     * @return true if success, false if not.
     */
    boolean notifySchedule(InetAddress address, NetRecordingEntry spec)
    {
        if (!isHandlerSet())
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifySchedule() called without a handler set ");
            }
            return false;
        }

        final boolean result[] = { false };

        final InetAddress finalAddress = address;

        final NetRecordingEntry finalSpec = spec;

        CallerContext.Util.doRunInContextSync(netRecordingRequestHandlerContext, new Runnable()
        {
            public void run()
            {
                result[0] = netRecordingRequestHandler.notifySchedule(finalAddress, finalSpec);
            }
        }); // block until complete

        return result[0];
    }

    // //////////////////////////////////////////////////////////////////
    //
    // Private
    //
    // //////////////////////////////////////////////////////////////////

    private NetRecordingRequestHandler netRecordingRequestHandler;

    private CallerContext netRecordingRequestHandlerContext;
}
