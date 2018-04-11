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
package org.cablelabs.impl.manager.application;

import java.util.HashMap;

import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.OcapAppAttributes;
import org.ocap.system.RegisteredApiManager;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;

public class CannedApplicationManager implements ApplicationManager
{
    private static CannedApplicationManager instance = new CannedApplicationManager();

    public static Manager getInstance()
    {
        return instance;
    }

    protected CannedApplicationManager()
    {

    }

    public OcapAppAttributes createAppAttributes(AppEntry entry, Service service)
    {
        return null;
    }

    public AppDomain createAppDomain(ServiceContext svcCtx)
    {
        // If real application support is enabled then create a real appDomain
        // instance. Otherwise, create a canned appDomain instance.
        if (MPEEnv.getEnv("OCAP.cannedSIDB.realApps") != null)
        {
            ApplicationManager appManager = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
            return appManager.createAppDomain(svcCtx);
        }
        else
            return new CannedAppDomain();
    }

    public ClassLoader getAppClassLoader(CallerContext ctx)
    {
        return null;
    }

    public AppManagerProxy getAppManagerProxy()
    {
        return null;
    }

    public AppsDatabase getAppsDatabase()
    {
        return null;
    }

    public RegisteredApiManager getRegisteredApiManager()
    {
        return null;
    }

    public int getRuntimePriority(AppID id)
    {
        CallerContext cc = (CallerContext) idCCMap.get(id);
        if (cc != null)
            return ((Integer) cc.get(CallerContext.APP_PRIORITY)).intValue();
        else
            return -1;
    }

    public boolean purgeLowestPriorityApp(long contextId, long timeout, boolean urgent)
    {
        return false;
    }

    public void destroy()
    {
        idCCMap.clear();
    }

    public void cannedMapAppIDAndCC(AppID id, CallerContext cc)
    {
        idCCMap.put(id, cc);
    }

    private HashMap idCCMap = new HashMap();

    /**
     * 
     * @author Joshua Keplinger
     * 
     */
    public static class CannedAppDomain implements AppDomain
    {
        public static final int STOPPED = 0x101;

        public static final int SELECTED = 0x102;

        public static final int DESTROYED = 0x103;

        private int state;

        private int stoppedCounter;

        private int selectedCounter;

        private int destroyedCounter;

        private int appsStoppedCounter;

        private CannedAppDomain()
        {
            state = STOPPED;
            stoppedCounter = 0;
            selectedCounter = 0;
            destroyedCounter = 0;
            appsStoppedCounter = 0;
        }

        public void stop()
        {
            if (state != STOPPED)
            {
                state = STOPPED;
            }
            stoppedCounter++;
        }

        public void destroy()
        {
            if (state != DESTROYED)
            {
                if (state != STOPPED) stop();
                state = DESTROYED;
            }
            destroyedCounter++;
        }

        public void select(ServiceDetails service, InitialAutostartAppsStartedListener InitialAutostartAppsStartedListener)
        {
            state = SELECTED;
            selectedCounter++;
        }

        public void preSelect(ServiceDetails service)
        {
            throw new UnsupportedOperationException("preSelect should not be invoked");
        }

        public void stopBoundApps()
        {
            appsStoppedCounter++;
        }

        public int cannedGetState()
        {
            return state;
        }

        public int cannedGetStopCount()
        {
            return stoppedCounter;
        }

        public int cannedGetDestroyCount()
        {
            return destroyedCounter;
        }

        public int cannedGetSelectCount()
        {
            return selectedCounter;
        }

        public int cannedGetAppStopCount()
        {
            return appsStoppedCounter;
        }

        public ServiceContentHandler[] getServiceContentHandlers()
        {
            return new ServiceContentHandler[0];
        }

        public AppProxy getAppProxy(AppID id)
        {
            return null;
        }
    }

    public AppEntry getRunningVersion(AppID id)
    {
        return null;
    }

}
