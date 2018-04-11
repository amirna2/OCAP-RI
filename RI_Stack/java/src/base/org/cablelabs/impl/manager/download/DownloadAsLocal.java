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

/*
 * Created on Nov 30, 2006
 */
package org.cablelabs.impl.manager.download;

import org.cablelabs.impl.manager.AppDownloadManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.TaskQueue;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * This is essentially a <i>fake</i> implementation of an
 * <code>AppDownloadManager</code> to be used during testing when an <i>Object
 * Carousel</i> is not available. Whereas a <i>real</i> application download
 * manager will only download when necessary (i.e., when an app is delivered via
 * in-band Object Carousel), this implementation will <i>download</i>
 * applications loaded via the {@link LocalTransportProtocol}. Actually, no
 * <i>download</i> takes place, instead this implementation simply pretends like
 * one does.
 * 
 * @author Aaron Kamienski
 */
public class DownloadAsLocal implements AppDownloadManager
{
    /**
     * Implements <code>getInstance()</code> to satisfy the basic
     * {@link Manager} contract.
     * 
     * @returns a new instance of {@link DownloadToStorage}
     */
    public static Manager getInstance()
    {
        return new DownloadAsLocal();
    }

    private DownloadAsLocal()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        tq = ccm.getSystemContext().createTaskQueue();
    }

    public DownloadRequest download(XAppEntry entry, boolean authenticate, boolean stealTuner, Callback callback)
    {
        // Examine entry and determine if needs to be downloaded or not
        // Will only download if "transport protocol" is inband OC
        TransportProtocol[] tp = entry.transportProtocols;
        if (tp == null) return null;
        boolean local = false;
        for (int i = 0; i < tp.length; ++i)
        {
            if (tp[i] instanceof LocalTransportProtocol)
            {
                local = true;
                break;
            }
        }
        if (!local)
            return null;

        Download pending = new Download(entry, callback);

        if (log.isDebugEnabled())
        {
            log.debug("Scheduling download for " + entry.id);
        }

        tq.post(pending); // Schedule delivery (should we delay?)
        return pending;
    }

    public void destroy()
    {
        /* Does nothing */
    }

    private class Download implements DownloadRequest, DownloadedApp, Runnable
    {
        Download(XAppEntry app, Callback cb)
        {
            String dir = app.baseDirectory;

            if (!dir.startsWith(File.separator)) dir = CWD + dir;

            this.baseDir = new File(dir);
            this.cb = cb;
        }

        public void run()
        {
            // Go based upon when app files are available or not...
            if (baseDir.exists())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Signalling downloadSuccess for " + baseDir);
                }
                cb.downloadSuccess(this);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Signalling downloadFailure for " + baseDir);
                }
                cb.downloadFailure(Callback.DSMCC_FAILURE, "BaseDir doesn't exist");
            }
            cb = null;
        }

        public boolean cancel()
        {
            boolean cancelled = baseDir != null;
            if (cancelled)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Download cancelled for " + baseDir);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Download already cancelled for " + baseDir);
                }
            }
            baseDir = null;
            return cancelled;
        }

        public void dispose()
        {
            if (log.isDebugEnabled())
            {
                log.debug("DownloadedApp disposed for " + baseDir);
            }
            baseDir = null;
        }

        public File getBaseDirectory()
        {
            return baseDir;
        }

        private File baseDir;

        private Callback cb;
    }

    /**
     * TaskQueue used to make all download delivery synchronous, but otherwise
     * asynchronous compared to other operations.
     */
    private TaskQueue tq;

    private static final Logger log = Logger.getLogger(DownloadAsLocal.class);

    /**
     * The <i>system default directory</i>. Relative paths should be prepended
     * with this.
     */
    private static final String CWD = File.separatorChar + "syscwd" + File.separatorChar;
}
