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

package org.cablelabs.impl.signalling;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * An <code>XAppEntry</code> contains all of the information signalled in the AIT
 * about an application.
 * <p>
 * Instances of <code>XAppEntry</code> are created as the result of parsing
 * application signalling contained in either out-of-band XAIT.
 * <p>
 * While all attributes are publicly accessible and non-<code>final</code>, they
 * should be considered <code>final</code> and not modified.
 */
public class XAppEntry extends AppEntry
{
    /**
     * For <i>unbound</i> applications signalled in an XAIT, this corresponds to
     * the <i>service_id</i> field of the
     * <i>unbound_application_descriptor()</i>. For <i>bound</i> applications
     * signalled in an AIT, this corresponds to the service on which the AIT is
     * received. This field is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getServiceLocator}.
     * 
     * @see "OCAP-1.0: 11.2.2.3.15 Unbound Application Descriptor"
     */
    public int serviceId;

    /**
     * For <i>unbound</i> applications signalled in an XAIT, this corresponds to
     * the <i>storage_priority</i> field of the
     * <i>application_storage_descriptor()</i>. For <i>bound</i> applications
     * signalled in an AIT or if no such descriptor is specified, this always is
     * zero. This is the basis for determining whether the application should be
     * stored as well as for
     * {@link org.ocap.application.OcapAppAttributes#getStoragePriority}.
     * 
     * @see "OCAP-1.0: 11.2.2.3.16 Appliation Storage Descriptor"
     */
    public int storagePriority;

    /**
     * For <i>unbound</i> applications signalled in an XAIT, this corresponds to
     * the <i>launch_order</i> field of the
     * <i>application_storage_descriptor()</i>. For <i>bound</i> applications
     * signalled in an AIT or if no such descriptor is specified, this always is
     * zero. Given multiple instances of the same application, only the one with
     * the highest <code>launchOrder</code> shall be entered into the
     * applications database.
     * 
     * @see "OCAP-1.0: 11.2.2.3.16 Appliation Storage Descriptor"
     */
    public int launchOrder;
    
    /**
     * True if this application is the monitor application. Otherwise, false.
     * <p>
     * This is not set by signalling, but is instead used elsewhere within in
     * the implementation to indicate that a given app is <i>the</i> monitor
     * app.
     */
    public boolean isMonitorApp = false;

    /**
     * Corresponds to the <code>AppID</code> for the application that originally
     * registered this application via
     * {@link org.ocap.application.RegisteredApiManager#registerUnboundApp}.
     */
    public AppID owner;
    
    /**
     * Identifies how this unbound application was introduced into the system.
     * Can be one of:
     * <ol>
     * <li>{@link Xait#NETWORK_SIGNALLING}</li>
     * <li>{@link Xait#REGISTER_UNBOUND_APP}</li>
     * <li>{@link Xait#HOST_DEVICE}</li>
     * </ol>
     */
    public int source;
    
    /**
     * Newer version of this application currently signaled.  This doesn't always
     * represent an app with a higher version, it could be an app with a higher launch
     * order
     */
    public XAppEntry newVersion = null;
    
    /**
     * Returns whether this app is unsigned (0), signed (1), or dual-signed (2)
     * based on the signaling information.  Only dual-signed, unbound apps can
     * request monapp permissions
     * 
     * OCAP Table 11-5 Describes the AppID ranges that delineate unsigned/signed/dual-signed
     * OCAP Table 11-7 Describes the ServiceID ranges for unbound apps
     */
    public int getNumSigners()
    {
        int aid = id.getAID();
        if (aid < 0x4000)
            return 0;
        else if (aid < 0x6000 || serviceId <= 0xFFFF)
            return 1;
        else
            return 2;
    }
    
    public String toString()
    {
        StringBuffer str = new StringBuffer("[");
        
        str.append("service=0x" + Integer.toHexString(serviceId) + ",");
        str.append("version=" + version + ",");
        str.append("storagePriority=" + storagePriority + ",");
        str.append("launchOrder=" + launchOrder + ",");
        if (isMonitorApp)
        {
            str.append("MonApp");
        }
        str.append("]");
        
        return super.toString() + str;
    }
    
    /**
     * Standard comparator used to sort applications by AppID,
     * then by version (ascending), then by priority (ascending)
     */
    public static class AppIDVersPrioCompare extends AppIDCompare
    {
        public int compare(Object o1, Object o2)
        {
            XAppEntry app1 = (XAppEntry) o1;
            XAppEntry app2 = (XAppEntry) o2;
            
            int cmp = super.compare(o1, o2);
            if (cmp == 0)
            {
                if (app1.version - app2.version < 0)
                {
                    return -1;
                }
                else if (app1.version - app2.version > 0)
                {
                    return 1;
                }
                else
                {
                    cmp = app1.priority - app2.priority;
                }
            }
            return cmp;
        }
    }
    
    /**
     * Standard comparator used to sort applications by AppID,
     * then by launch order (ascending)
     */
    public static class AppIDLaunchCompare extends AppIDCompare
    {
        public int compare(Object o1, Object o2)
        {
            XAppEntry app1 = (XAppEntry) o1;
            XAppEntry app2 = (XAppEntry) o2;
            
            int cmp = super.compare(o1, o2);
            if (cmp == 0)
            {
                cmp = app1.launchOrder - app2.launchOrder;
            }
            return cmp;
        }
    }
    
    /**
     * Standard comparator used to sort applications by AppID,
     * then by launch order (ascending), then by version (ascending)
     */
    public static class AppIDLaunchVersCompare extends AppIDLaunchCompare
    {
        public int compare(Object o1, Object o2)
        {
            XAppEntry app1 = (XAppEntry) o1;
            XAppEntry app2 = (XAppEntry) o2;
            
            int cmp = super.compare(o1, o2);
            if (cmp == 0)
            {
                if (app1.version - app2.version < 0)
                {
                    return -1;
                }
                else if (app1.version - app2.version > 0)
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
            return cmp;
        }
    }
}
