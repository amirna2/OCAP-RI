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

package org.cablelabs.xlet.AbstractSvcMgmtTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.CRC32;

import javax.tv.service.SIManager;

import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.DVBJProxy;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.system.MonitorAppPermission;
import org.ocap.application.OcapAppAttributes;

/**
 * A basic AppInfo skeleton.
 */
public class TestApp implements XAITGenerator.AppInfo
{
    private AppID id;

    private int svcId;

    public TestApp(AppID id, int svcId)
    {
        this.id = id;
        this.svcId = svcId;
    }

    public AppID getAppID()
    {
        return id;
    }

    public String getName()
    {
        return toString();
    }

    public int getControlCode()
    {
        return OcapAppAttributes.PRESENT;
    }

    public int getVisibility()
    {
        return VIS_FULL;
    }

    public int getPriority()
    {
        return 255;
    }

    public int getLaunchOrder()
    {
        return 1;
    }

    public int[] getPlatformVersion()
    {
        return new int[] { 1, 0, 0 };
    }

    public int getVersion()
    {
        return 1;
    }

    public int getServiceID()
    {
        return svcId;
    }

    public String getBaseDir()
    {
        return "/project/RI_Stack/java/xlet";
    }

    public String getClasspath()
    {
        return "";
    }

    public String getClassName()
    {
        return "org.cablelabs.xlet.Test";
    }

    public String[] getParameters()
    {
        String[] parms = new String[id.getAID() % 4];
        for (int i = 0; i < parms.length; ++i)
            parms[i] = "" + i;
        return parms;
    }

    public AppIcon getAppIcon()
    {
        return new AppIcon()
        {
            public org.davic.net.Locator getLocator()
            {
                try
                {
                    return new org.ocap.net.OcapLocator("ocap:/app-icons/" + id);
                }
                catch (Exception e)
                {
                    return null;
                }
            }

            public BitSet getIconFlags()
            {
                BitSet flags = new BitSet();
                final int len = id.getAID() % 5;
                for (int i = 0; i < len; ++i)
                    flags.set(i);
                return flags;
            }
        };
    }

    public int getStoragePriority()
    {
        return 1;
    }

    public TPInfo[] getTransportProtocols()
    {
        return new TPInfo[] { new OCInfo()
        {
            public int getId()
            {
                return 0x0001;
            }

            public int getLabel()
            {
                return 10;
            }

            public boolean isRemote()
            {
                return false;
            }

            public int getNetId()
            {
                return 0;
            }

            public int getTsId()
            {
                return 0;
            }

            public int getSId()
            {
                return 1;
            }

            public int getComponent()
            {
                return 0;
            }
        }, new IPInfo()
        {
            public int getId()
            {
                return 0x0002;
            }

            public int getLabel()
            {
                return 11;
            }

            public boolean isRemote()
            {
                return false;
            }

            public int getNetId()
            {
                return 0;
            }

            public int getTsId()
            {
                return 0;
            }

            public int getSId()
            {
                return 2;
            }

            public boolean isAligned()
            {
                return false;
            }

            public String[] getUrls()
            {
                return new String[] { "file:/" + getBaseDir() };
            }
        }, };
    }
}
