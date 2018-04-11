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

package org.cablelabs.impl.ocap.hn;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.util.TaskQueue;
import org.dvb.application.AppID;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.PropertyFilter;
import org.ocap.hn.content.ContentContainer;

public class TestUtils
{

    public static NetModule getOcapMediaServer()
    {
        MediaServer.getInstance().startMediaServer();
        
        final int WAIT_TIME = 3000;
        final int MAX_WAITS = 40;
        String name = "OCAP Media Server";
        PropertyFilter filter = new PropertyFilter(new Properties());
        filter.addProperty(Device.PROP_FRIENDLY_NAME, name);
        NetManager nm = NetManager.getInstance();
        NetModule module = null;
        if (nm != null)
        {
            NetList list = null;
            for (int x = 0; x < MAX_WAITS; x++)
            {
                list = nm.getDeviceList(filter);
                if (list.size() > 0)
                {
                    break;
                }

                try
                {
                    Thread.sleep(WAIT_TIME);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // NOTE: see OCSPEC-296.
            Device d = (Device) list.getElement(0);
            Object obj = null;
            list = d.getNetModuleList();
            for (int x = 0; x < list.size(); x++)
            {
                obj = list.getElement(x);
                if (((NetModule) obj).getNetModuleType().equals(NetModule.CONTENT_SERVER))
                {
                    module = (NetModule) obj;
                    break;
                }
            }
        }

        return module;
    }

    public static ContentContainer getRootContainer()
    {
        ContentServerNetModule module = (ContentServerNetModule) TestUtils.getOcapMediaServer();

        TestNetActionHandler nah = new TestNetActionHandler();
        module.requestRootContainer(nah);

        NetActionEvent event = nah.getLocalEvent();
        return (ContentContainer) event.getResponse();
    }

    public static CallerContextManager replaceCallerContext(AppID appId)
    {
        CallerContextManager save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save, appId));

        return save;
    }

    public static void restoreCallerContext(CallerContextManager save)
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    /**
     * Replacement CallerContextManager so we can affect the getCurrentContext()
     * method.
     */
    public static class CCMgr implements CallerContextManager
    {
        private DummyContext dummyContext;

        private CallerContextManager ccm;

        public CCMgr(CallerContextManager original, AppID appId)
        {
            ccm = original;
            dummyContext = new DummyContext(appId);
        }

        public CallerContext getCurrentContext()
        {
            return dummyContext;
        }

        public void runAsContext(CallerContext ct, Runnable run)
        {
        }

        public CallerContext getSystemContext()
        {
            return ccm.getSystemContext();
        }

        public static Manager getInstance()
        {
            throw new UnsupportedOperationException("Unexpected");
        }

        public void destroy()
        {
            throw new UnsupportedOperationException("Unexpected");
        }
    }

    /**
     * A CallerContext implementation that allows the test to control the thread
     * group used and record whether the runInContext method is called.
     */
    public static class DummyContext implements CallerContext
    {
        private AppID appId = null;

        public DummyContext(AppID appId)
        {
            this.appId = appId;
        }

        public void addCallbackData(CallbackData data, Object key)
        {
        }

        public void checkAlive() throws SecurityException
        {
        }

        public TaskQueue createTaskQueue()
        {
            return null;
        }

        public Object get(Object key)
        {
            if (CallerContext.APP_ID.equals(key))
            {
                return appId;
            }
            return null;
        }

        public CallbackData getCallbackData(Object key)
        {
            return null;
        }

        public boolean isActive()
        {
            return false;
        }

        public boolean isAlive()
        {
            return false;
        }

        public void removeCallbackData(Object key)
        {
        }

        public void runInContext(Runnable run) throws SecurityException, IllegalStateException
        {
        }

        public void runInContextAsync(Runnable run) throws SecurityException, IllegalStateException
        {
        }

        public void runInContextSync(Runnable run) throws SecurityException, IllegalStateException,
                InvocationTargetException
        {

        }

        public void runInContextAWT(Runnable run) throws SecurityException,
                IllegalStateException
        {
        }

    }

}
