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

package org.cablelabs.impl.manager.cdl;

import org.ocap.system.event.SystemEventManager;
import org.ocap.system.event.DeferredDownloadEvent;
import org.cablelabs.impl.util.ExtendedSystemEventManager;
import org.cablelabs.impl.manager.DownloadManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.apache.log4j.Logger;

public class DownloadManagerImpl implements DownloadManager, EDListener
{
    /**
     * Create the DownloadManagerImpl and read the current boot state from the
     * boot state file
     */
    private DownloadManagerImpl()
    {
        if (log.isDebugEnabled())
        {
            log.debug("DownloadManager being instantiated");
        }

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        // register CommonDownload async event listener with native-land
        edListenerHandle = nRegisterAsync(this);
        if (edListenerHandle == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Couldn't register a listener for native async CommonDownload events");
            }
        }

        return;
    }

    /**
     * Returns an instance of DownloadManagerImpl. Will be called only once for
     * each Manager class type.
     */
    public static Manager getInstance()
    {
        // return an instance of this class
        return new DownloadManagerImpl();
    }

    /**
     * Destroy this manager
     */
    public void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug("DownloadManager being destroyed");
        }

        // unregister CommonDownload async event listener from native-land
        if (edListenerHandle != 0)
        {
            nUnregisterAsync(edListenerHandle);
            edListenerHandle = 0;
        }

        return;
    }

    /**
     * This callback function is called from MPE to indicate an asynchronous
     * CommonDownload event.
     * 
     * @param eventID
     *            DeferredDownloadEvent typeCode
     * @param data1
     *            Unused
     * @param data2
     *            Unused
     */
    public void asyncEvent(int typeCode, int data1, int data2)
    {

        if (log.isDebugEnabled())
        {
            log.debug("DeferredDownload Event received w/ typeCode[" + typeCode + "]");
        }

        // ignore the 'queue shutdown' event that terminates a queue
        if (typeCode == DNLD_EVENT_SHUTDOWN_QUEUE)
        {
            // just ignore this event type
            return;
        }

        // Send a DeferredDownloadEvent System Event
        ExtendedSystemEventManager sem = (ExtendedSystemEventManager) SystemEventManager.getInstance();
        if (sem.log(new DeferredDownloadEvent(typeCode), 0) == ExtendedSystemEventManager.NOLISTENER)
        {
            // If no one is listening, then immediately start download
            if (log.isWarnEnabled())
            {
                log.warn("No Listener for DeferredDownloadEvent - starting download immediately");
            }
            startDownload();
        }

        return;
    }

    /**
     * Begins the code file download
     */
    public void startDownload()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Starting the Common Download");
        }

        // call into native-land to instigate the download
        nStartDownload();

        return;
    }

    // native methods
    private native int nRegisterAsync(Object EdListenerObject);

    private native void nUnregisterAsync(int EdListenerHandle);

    private native void nStartDownload();

    // private data

    /**
     * ED handle for the registered async CommonDownload listener
     */
    private int edListenerHandle = 0;

    /**
     * Constant that indicates an async event queue shutdown
     */
    private static final int DNLD_EVENT_SHUTDOWN_QUEUE = -1;

    /**
     * Private logger.
     */
    private static final Logger log = Logger.getLogger(DownloadManagerImpl.class);

}
