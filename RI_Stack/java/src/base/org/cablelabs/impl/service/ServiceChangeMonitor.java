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

package org.cablelabs.impl.service;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.si.TableManagerExt;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.TableChangeListener;

/**
 * Registers for changes to PAT and PMT tables.
 * <p>
 * <p/>
 * Notifies the registered {@link ServiceChangeListener} of any updates.
 */
public class ServiceChangeMonitor
{
    private static final Logger log = Logger.getLogger(ServiceChangeMonitor.class);

    private ServiceChangeListener serviceChangeListener;

    private final TableChangeListener pmtTableChangeListener;

    private final Object lock;

    private ServiceDetailsExt serviceDetailsExt;

    private boolean initialized;

    private boolean pmtRemoved;

    /**
     * Constructor
     *
     * @param lock
     *            the shared lock
     * @param serviceChangeListener
     *            the listener that will receive notifications of service changes
     */
    public ServiceChangeMonitor(Object lock, ServiceChangeListener serviceChangeListener)
    {
        this.lock = lock;
        this.serviceChangeListener = serviceChangeListener;

        pmtTableChangeListener = new PMTTableChangeListenerImpl();
    }

    /**
     * Initialize the ServiceChangeMonitor
     *
     * @param serviceDetailsExt
     */
    public void initialize(ServiceDetailsExt serviceDetailsExt)
    {
        synchronized (lock)
        {
            this.serviceDetailsExt = serviceDetailsExt;
            pmtRemoved = false;

            // add these listeners once
            if (!initialized)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initialize - first initialization");
                }

                // PMT changes (removal of PAT will cause PMT removal as well)
                ((TableManagerExt) ProgramMapTableManager.getInstance()).addTableChangeListener(pmtTableChangeListener,
                        serviceDetailsExt.getService(), 10);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initialize - subsequent initialization - ignoring");
                }
            }
            initialized = true;
        }
    }

    /**
     * Release resources. Safe to be called multiple times.
     */
    public void cleanup()
    {
        synchronized (lock)
        {
            // don't clean up if not initialized
            if (!initialized)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("cleanup called when not initialized - ignoring");
                }
                return;
            }
            if (log.isDebugEnabled())
            {
                log.debug("cleanup: " + this);
            }
            ProgramMapTableManager.getInstance().removeInBandChangeListener(pmtTableChangeListener);

            serviceDetailsExt = null;

            pmtRemoved = false;
            initialized = false;
        }
    }

    public boolean isPMTRemoved()
    {
        synchronized (lock)
        {
            if (!initialized)
            {
                throw new IllegalStateException("isPMTRemoved called when not initialized!");
            }
            return pmtRemoved;
        }
    }

    public String toString()
    {
        return "Service change monitor - initialized: " + initialized
                + ", serviceDetails: " + serviceDetailsExt + " - " + super.toString();
    }

    class PMTTableChangeListenerImpl implements TableChangeListener
    {
        // Listens for PMT changes. Will get only events for this service since
        // it was registered with the locator.
        public void notifyChange(SIChangeEvent event)
        {
            boolean removed = false;
            boolean modified = false;
            ServiceChangeListener tempServiceChangeListener;

            synchronized (lock)
            {
                if (!initialized)
                {
                    return;
                }
                tempServiceChangeListener = serviceChangeListener;

                if (log.isInfoEnabled())
                {
                    log.info("received PMT change notification - type: " + event.getChangeType());
                }

                if (event.getChangeType() == SIChangeType.REMOVE)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("PMT removed");
                    }
                    pmtRemoved = true;
                    removed = true;
                }
                // change type is ADD or MODIFY; so treat as PIDCHANGE
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("PMT added or modified");
                    }
                    pmtRemoved = false;
                    modified = true;
                }
            }
            if (removed)
            {
                tempServiceChangeListener.serviceChangeEvent(ServiceChangeEvent.PMT_REMOVED);
            }
            if (modified)
            {
                tempServiceChangeListener.serviceChangeEvent(ServiceChangeEvent.PMT_CHANGED);
            }
        }
    }
}
