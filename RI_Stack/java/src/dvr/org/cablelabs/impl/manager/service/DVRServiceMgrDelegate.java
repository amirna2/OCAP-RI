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

package org.cablelabs.impl.manager.service;

import javax.media.Time;
import javax.media.protocol.DataSource;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.media.player.RecordedServiceMediaHandler;
import org.cablelabs.impl.media.player.SegmentedRecordedServiceMediaHandler;
import org.cablelabs.impl.media.player.TSBServiceMediaHandler;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.media.source.TSBDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.javatv.selection.DVRPresentation;
import org.cablelabs.impl.service.javatv.selection.DVRServiceContextExt;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.SegmentedRecordedService;

/**
 * A <code>ServiceManagerDelegate</code> implementation that supports DVR.
 * 
 * @author Todd Earles
 */
public class DVRServiceMgrDelegate implements ServiceMgrDelegate
{
    private static final Logger log = Logger.getLogger(DVRServiceMgrDelegate.class);

    /**
     * Contstruct this object.
     */
    public DVRServiceMgrDelegate()
    {
        // Do not perform any initialization here that indirectly relies on
        // the storage manager because we have not finished constructing
        // the storage manager yet.
    }

    // Description copied from ServiceManager
    public ServiceDataSource createServiceDataSource(ServiceContextExt sc, Service service)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createServiceDataSource: " + sc + ", service: " + service);
        }
        // must be a TSBDataSource
        if (sc instanceof DVRServiceContextExt)
        {
            DVRServiceContextExt dvrCtx = (DVRServiceContextExt) sc;
            // If DVRPresentation is assigned for this service,
            // set DataSource's initial media time and rate from the
            // DVRPresentation.
            DVRPresentation p = dvrCtx.getDVRPresentation(service);
            if (p != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("using DVRPresentation: " + p);
                }
            }
            if (service instanceof SegmentedRecordedService)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("creating segmented recording service datasource");
                }
                if (p != null)
                {
                    return new org.cablelabs.impl.media.protocol.segrecsvc.DataSource(p.getMediaTime(), p.getRate());
                }
                else
                {
                    SegmentedRecordedService segRecSvc = (SegmentedRecordedService) service;
                    // the recorded service provides start mediatime
                    return new org.cablelabs.impl.media.protocol.segrecsvc.DataSource(segRecSvc.getMediaTime(), 1.0F);
                }
            }
            else if (service instanceof RecordedService)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("creating recording service datasource");
                }
                if (p != null)
                {
                    return new org.cablelabs.impl.media.protocol.recording.DataSource(p.getMediaTime(), p.getRate());
                }
                else
                {
                    RecordedService recSvc = (RecordedService) service;
                    // the recorded service provides start mediatime
                    return new org.cablelabs.impl.media.protocol.recording.DataSource(recSvc.getMediaTime(), 1.0F);
                }
            }
            TimeShiftWindowClient tsb = ((DVRServiceContextExt) sc).getTimeShiftWindowClient();
            if (tsb != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("creating tsb datasource");
                }

                if (p != null)
                {
                    return new TSBDataSource(dvrCtx, p.getMediaTime(), p.getRate());
                }
                // No presentation settings, so just set default startup
                // values--i.e.,
                // start at the live point and rate of 1.
                return new TSBDataSource(dvrCtx, new Time(Double.POSITIVE_INFINITY), 1);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("tsb was null - unable to create datasource");
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("createServiceDataSource called, but not a DVRServiceContext - unable to create datasource");
            }
        }
        // not supported by this delegate, return null
        return null;
    }

    // Description copied from ServiceManager
    public ServiceMediaHandler createServiceMediaHandler(DataSource ds, ServiceContextExt sc, Object lock, ResourceUsageImpl resourceUsage)
    {
        if (ds == null || sc == null) throw new IllegalArgumentException("null argument");

        CallerContext cc = sc.getCallerContext();
        if (ds instanceof org.cablelabs.impl.media.protocol.segrecsvc.DataSource)
        {
            return new SegmentedRecordedServiceMediaHandler(cc, lock, resourceUsage);
        }
        else if (ds instanceof org.cablelabs.impl.media.protocol.recording.DataSource)
        {
            return new RecordedServiceMediaHandler(cc, lock, resourceUsage);
        }
        else if (ds instanceof TSBDataSource)
        {
            return new TSBServiceMediaHandler(cc, lock, resourceUsage);
        }
        // not supported by this delegate, return null
        return null;
    }
}
