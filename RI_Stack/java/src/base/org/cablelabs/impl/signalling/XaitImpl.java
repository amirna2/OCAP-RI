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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.dvb.application.AppID;

public class XaitImpl extends AitImpl implements Xait
{
    public void initialize(int version, Vector externalAuth, Vector allApplications,
                           Hashtable attributeMap, Hashtable addrGroups,
                           int source, byte[] privilegedCerts, Hashtable abstractServices)
    {
        super.initialize(version,externalAuth,allApplications,attributeMap,addrGroups);
        this.source = source;
        this.privilegedCertificates = privilegedCerts;
        this.abstractServices = abstractServices;
    }
    
    /**
     * Implements Ait.getApps(). Returns the list of apps for which there are no
     * abstract service descriptors.  Only REGISTER_UNBOUND_APP is allowed to
     * have applications signaled without an abstract service descriptor
     * 
     * @return the list of applications that have no abstract service descriptor.
     *         Returns an empty list if no such apps exists
     */
    public synchronized AppEntry[] getApps()
    {
        XAppEntry[] apps = new XAppEntry[noServiceApps.size()];
        noServiceApps.copyInto(apps);
        return apps;
    }

    // Description copied from Xait
    public AbstractServiceEntry[] getServices()
    {
        AbstractServiceEntry[] array = new AbstractServiceEntry[abstractServices.size()];
        int i = 0;
        for (Enumeration e = abstractServices.elements(); e.hasMoreElements();)
        {
            array[i++] = (AbstractServiceEntry) e.nextElement();
        }
        return array;
    }

    // Description copied from Xait
    public int getSource()
    {
        return source;
    }

    // Description copied from Xait
    public byte[] getPrivilegedCertificateBytes()
    {
        return privilegedCertificates;
    }

    // Description copied from Ait
    public ExternalAuthorization[] getExternalAuthorization()
    {
        // Not valid for XAIT
        return new ExternalAuthorization[0];
    }

    // Description copied from Ait
    public synchronized boolean filterApps(Properties securityProps, Properties registeredProps)
    {
        boolean result = super.filterApps(securityProps, registeredProps);

        // Now that filtering is complete, rebuild the application lists associated
        // with our abstract services
        
        // First clear out all the old applications from our abstract services
        for (Enumeration e = abstractServices.elements(); e.hasMoreElements();)
        {
            AbstractServiceEntry ase = (AbstractServiceEntry) e.nextElement();
            ase.apps.clear();
        }
        
        Vector newValidApps = new Vector();
        noServiceApps = new Vector();
            
        // Go through the valid apps list and add each one to its appropriate 
        // AbstractService.  If this is REGISTER_UNBOUND_APP signaling, an app
        // can be signaled without an abstract service descriptor
        for (Iterator i = validApps.iterator(); i.hasNext();)
        {
            XAppEntry xae = (XAppEntry)i.next();
            AbstractServiceEntry ase =
                (AbstractServiceEntry)abstractServices.get(new Integer(xae.serviceId));
            if (ase != null)
            {
                ase.apps.add(xae);
                newValidApps.add(xae);
            }
            else if (source == Xait.REGISTER_UNBOUND_APP)
            {
                noServiceApps.add(xae);
            }
            
            // If this app has new versions, make sure the abstract services
            // associated with those new versions are marked so that they don't
            // get ignored
            while (xae.newVersion != null)
            {
                ase = (AbstractServiceEntry)abstractServices.get(new Integer(xae.newVersion.serviceId));
                if (ase != null)
                {
                    ase.hasNewAppVersions = true;
                }
                xae = xae.newVersion;
            }
        }
        
        // Finally, make sure that any of our abstract services that contain
        // new application versions are duly marked
        for (Iterator i = servicesWithNewVersions.iterator(); i.hasNext();)
        {
            AbstractServiceEntry ase = (AbstractServiceEntry) abstractServices.get(i.next());
            if (ase != null)
            {
                ase.hasNewAppVersions = true;
            }
        }
        
        validApps = newValidApps;
        
        return result;
    }
    
    protected void checkAppIDDuplicates()
    {
        XAppEntry[] copy;
        
        // First, remove any duplicate AppIDs with the same application version
        // based on application priority
        Collections.sort(validApps, new XAppEntry.AppIDVersPrioCompare());
        copy = new XAppEntry[validApps.size()];
        validApps.copyInto(copy);
        for (int i = 0; i < (copy.length-1); i++)
        {
            XAppEntry test1 = copy[i];
            XAppEntry test2 = copy[i+1];
            if (test1.id.equals(test2.id) && test1.version == test2.version)
            {
                // Since we are sorted by priority (ascending), we can assume
                // that index i is lower or equal priority
                validApps.remove(test1);
                if (log.isWarnEnabled())
                {
                    log.warn("Removing duplicate AppID (same version, lower piority) -- " + test1);
                }
            }
        }
        
        // Next, remove duplicate AppIDs with the same launch order.  Higher application
        // version will be chosen
        Collections.sort(validApps, new XAppEntry.AppIDLaunchVersCompare());
        copy = new XAppEntry[validApps.size()];
        validApps.copyInto(copy);
        for (int i = 0; i < (copy.length-1); i++)
        {
            XAppEntry test1 = copy[i];
            XAppEntry test2 = copy[i+1];
            if (test1.id.equals(test2.id) && test1.launchOrder == test2.launchOrder)
            {
                // Since we are sorted by version (ascending), we can assume
                // that index i is lower version
                validApps.remove(test1);
                if (log.isWarnEnabled())
                {
                    log.warn("Removing duplicate AppID (same launchOrder, lower version) -- " + test1);
                }
            }
        }
        
        // Next, we search for duplicates again but this time looking for 
        // version and launch order attributes
        ApplicationManager am =
            (ApplicationManager)ManagerManager.getInstance(ApplicationManager.class);
        copy = new XAppEntry[validApps.size()];
        validApps.copyInto(copy);
        for (int i = 0; i < copy.length; i++)
        {
            AppEntry runningVersion;
            XAppEntry ae = copy[i];
            AppID id = ae.id;
            
            // Build duplicate appID list
            Vector sameAppIDs = new Vector();
            AppEntry dupe;
            while (i < copy.length && (dupe = copy[i++]).id.equals(id))
            {
                sameAppIDs.add(dupe);
            }
            if (sameAppIDs.size() <= 1) // No dupes
            {
                // Even though this is the only app with this AppID, this
                // could be a new version of an app that is no longer signaled
                // but is still running
                AppEntry app = (AppEntry)sameAppIDs.elementAt(0); 
                if ((runningVersion = am.getRunningVersion(app.id)) != null &&
                    app.version > runningVersion.version)
                {
                    XAppEntry xae = (XAppEntry)app;
                    ((XAppEntry)runningVersion).newVersion = xae;
                    validApps.remove(xae);
                    servicesWithNewVersions.add(new Integer(xae.serviceId));
                }
                break;
            }
            
            // First determine if one of these versions is currently running
            XAppEntry running = null;
            runningVersion = am.getRunningVersion(id);
            if (runningVersion != null)
            {
                for (Iterator iter = sameAppIDs.iterator(); iter.hasNext();)
                {
                    XAppEntry app = (XAppEntry)iter.next();
                    if (runningVersion.version == app.version)
                    {
                        running = app;
                        iter.remove();
                        break;
                    }
                }
            }
            
            // Now sort by launch order, these will become our new version list
            Collections.sort(sameAppIDs, new XAppEntry.AppIDLaunchCompare());
            
            // The primary version of this app is either the running version or
            // the one with the highest launch order.
            ListIterator reverseIter = sameAppIDs.listIterator(sameAppIDs.size());
            XAppEntry primaryApp =
                (running == null) ? (XAppEntry)reverseIter.previous() : running;
                
            // Now continue iterating through our list, chaining each subsequent new
            // version to the previous app
            while (reverseIter.hasPrevious())
            {
                XAppEntry app = (XAppEntry)reverseIter.previous();
                validApps.remove(app); // Only want the primary app to show up in the apps list
                
                // Don't allow any apps with a lower version than the currently
                // running app
                if (runningVersion == null || app.version > runningVersion.version)
                {
                    primaryApp.newVersion = app; 
                    primaryApp = app;
                }
            }
        }
    }

    /**
     * The source of the signalling.
     * 
     * @see Xait#getSource()
     * @see Xait#NETWORK_SIGNALLING
     * @see Xait#REGISTER_UNBOUND_APP
     */
    private int source;

    /**
     * The list of abstract services signaled in this XAIT (AbstractServiceEntry)
     * keyed by service ID (Integer)
     */
    private Hashtable abstractServices;
    
    /**
     * The list of XAppEntrys representing applications for which there was no
     * abstract service descriptor
     */
    private Vector noServiceApps;

    /**
     * This is a very special case list.  It is used when an abstract service
     * contains an application that is a new version of a currently running app
     * that is no longer signaled.  In this case, the old app is not terminated.
     * The new version will only launch when the old is terminated by the app
     * itself or by updated signalling.  However, we need to make sure that the
     * new version's abstract service is kept around even if it only has the one
     * app.  Confusing, I know -- but required.
     * 
     * List contains Integer objects that represent abstract service IDs that
     * we need to make sure to keep
     */
    private Vector servicesWithNewVersions = new Vector();
    /**
     * The set of <i>privileged_certificate_bytes</i> signalled in the XAIT.
     */
    private byte[] privilegedCertificates;

    private static final Logger log = Logger.getLogger(XaitImpl.class.getName());
}
