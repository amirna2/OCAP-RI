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

package org.cablelabs.impl.manager.recording;

import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.dvb.application.AppID;
import org.ocap.dvr.BufferingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class BufferingRequestManager
{
    BufferingRequest[] getBufferingRequests()
    {
        BufferingRequest[] ba = new BufferingRequest[m_bufferingRequestList.size()];

        return (BufferingRequest[]) m_bufferingRequestList.toArray(ba);
    }

    /**
     * @param service
     * @param minDuration
     * @param maxDuration
     * @param efap
     * @return
     */
    public BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration, ExtendedFileAccessPermissions efap)
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cctx = ccm.getCurrentContext();

        return createBufferingRequest(service, minDuration, maxDuration, efap, cctx);
    }

    BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration, ExtendedFileAccessPermissions efap,
            CallerContext cctx)
    {
        validateService(service);

        RecordingManager rm = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        RecordingManagerImpl orm = (RecordingManagerImpl) rm.getRecordingManager();
        if ((minDuration < orm.getSmallestTimeShiftDuration()) || maxDuration < minDuration)
        {
            if (log.isDebugEnabled())
            {
                log.debug("w. minD = " + minDuration + " maxD = " + maxDuration + "getSmalleTSD = "
                        + orm.getSmallestTimeShiftDuration());
            }
            throw new IllegalArgumentException("wrong min or max duration");
        }

        AppID appID = (AppID) cctx.get(CallerContext.APP_ID);
        return new BufferingRequestImpl(this, this, cctx, appID, service, minDuration, maxDuration, efap);
    }

    public void addAppToActiveList()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext ctx = ccm.getCurrentContext();

        if (ctx != ccm.getSystemContext())
        {
            // I don't like the idea of holding a reference
            // to the actual caller context.
            if (!m_activeAppsList.contains(ctx.toString()))
            {
                m_activeAppsList.add(ctx.toString());
                ctx.addCallbackData(new AppTerminationNotification(), this);
            }
        }
    }

    void removeAppFromActiveList(CallerContext ctx)
    {
        if (!m_activeAppsList.contains(ctx.toString()))
        {
            m_activeAppsList.remove(ctx.toString());
        }
    }

    void addActiveBufferingRequest(BufferingRequest br)
    {
        m_bufferingRequestList.add(br);
    }

    /**
     * @param request
     */
    public void cancelBufferingRequest(BufferingRequestImpl request)
    {
        request.cancelBufferingRequest(m_bufferingRequestList);
    }

    /**
     * Validates the <code>Service</code> parameter.
     * 
     * @param service
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    void validateService(Service service) throws IllegalArgumentException, SecurityException
    {
        if (service == null)
        {
            throw new IllegalArgumentException("Service is null");
        }

        SIManager sim = SIManager.createInstance();
        Locator loc = service.getLocator();

        try
        {
            sim.getService(loc);
        }
        catch (InvalidLocatorException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("validateService failed");
            }
            e.printStackTrace();
            throw new IllegalArgumentException("InvalidService");
        }
    }

    /**
     * @param ctx
     */
    void releaseBufferingRequestsForApp(CallerContext ctx)
    {
        AppID appIDParam = (AppID) (ctx.get(CallerContext.APP_ID));

        int length = m_bufferingRequestList.size();
        for (int i = 0; i < length; i++)
        {
            BufferingRequestImpl br = (BufferingRequestImpl) m_bufferingRequestList.get(i);

            if (br.getAppID().equals(appIDParam))
            {
                br.cancelBufferingRequest(m_bufferingRequestList);
            }
        }
        m_activeAppsList.remove(appIDParam.toString());
        ctx.removeCallbackData(this);
    }

    /**
     * An object that is activated when an application terminates.
     * 
     * @author Jeff Spruiel
     */
    class AppTerminationNotification implements CallbackData
    {
        // Called when an application is terminated.
        public void destroy(CallerContext ctx)
        {
            RecordingManager rm = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
            RecordingManagerImpl rmi = (RecordingManagerImpl) rm.getRecordingManager();
            rmi.releaseBufferingRequestsForApp(ctx);
        }

        public void pause(CallerContext ctx)
        {
        }

        public void active(CallerContext callerContext)
        {
        }
    }

    /**
	 * 
	 */
    private Vector m_bufferingRequestList = new Vector();

    /**
	 * 
	 */
    private Vector m_activeAppsList = new Vector();

    private static final Logger log = Logger.getLogger(BufferingRequestManager.class.getName());

}
