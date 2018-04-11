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

package org.dvb.application;

import java.util.*;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ApplicationManager;

/**
 * The <code>AppsDatabase</code> is an abstract view of the currently available
 * applications. The entries will be provided by the application manager, and
 * gleaned from the AIT signaling. When the service context in which an
 * application is running undergoes service selection, instances of
 * <code>AppsDatabase</code> used by that application shall be updated from the
 * new service before an <code>AppsDatabaseEvent</code> is sent to the
 * <code>newDatabase</code> method of any registered
 * <code>AppsDatabaseEventListeners</code>. For applications fully signalled in
 * the current service (i.e. excluding externally authorised ones), the
 * attributes entries shall be the ones from the signalling of the current
 * service even if the application was originally launched from another service
 * and then survived service selection. For running externally authorised
 * applications, the entries will be those from the last service in which they
 * ran fully signalled.
 * <p>
 * Externally authorized applications shall not appear unless an instance of
 * that application is actually running.
 * <p>
 * A generic launcher may be written which uses the database to display
 * information in AppAttributes and uses an AppProxy to launch it
 * <p>
 * Methods on classes in this package do not block, they return the information
 * the system currently has. Therefore applications should be aware that data
 * may be stale, to within one refresh period of the AIT.
 * <p>
 * e.g.:
 *
 * <pre>
 * <code>
 *   AppsDatabase theDatabase = AppsDatabase.getDatabase();
 *   if (theDatabase != null ) {
 *       Enumeration attributes = theDatabase.getAppAttributes();
 *       if(attributes != null) {
 *          while(attributes.hasMoreElements()) {
 *              AppAttributes info ;
 *              AppProxy proxy ;
 *
 *              info = (AppAttributes)attributes.nextElement();
 *              proxy = (AppProxy)theDatabase.getAppProxy(info.getIdentifier());
 *              URL icon = info.getIcon();
 *              // blah blah..
 *              // lets start it.
 *              proxy.start(false, null);
 *          }
 *       }
 *   }
 * </code>
 * </pre>
 *
 * Where methods on this class as specified as working on "available"
 * applications or "currently available" applications the following definition
 * shall apply. An application is "currently available" if and only if one of
 * the following applies in the service context within which the application
 * calling the method is executing and the visibility of the application is not '00'.
 * <ul>
 * <li>it is signalled as being present or autostart in the currently selected
 * service of that service context and could be started.
 * <li>it is currently running in that service context.
 * </ul>
 * In addition to the methods listed below, all calls made using an
 * AppsDatabaseFilter shall only use that filter to test "currently available"
 * applications as defined here.
 * <p>
 * Applications whose information (e.g. signaling) is invalid (e.g. one or more
 * mandatory descriptors are missing or incorrect) may not be listed in the
 * AppsDatabase. Where applications are signalled in a broadcast AIT and the MHP
 * terminal tunes away from the service on which the AIT is carried, but without
 * selecting a new service, the <code>AppsDatabase</code> shall retain the
 * entries as signalled in that AIT until a new service is selected.
 */

public class AppsDatabase
{

    /**
     * This constructor is provided for the use of implementations and
     * specifications which extend this specification. Applications shall not
     * define sub-classes of this class. Implementations are not required to
     * behave correctly if any such application defined sub-classes are used.
     */
    protected AppsDatabase()
    {
    }

    /**
     * Returns the singleton AppsDatabase object. The AppsDatabase is either a
     * singleton for each MHP application or a singleton for the MHP terminal.
     *
     * @return the singleton AppsDatabase object.
     * @since MHP1.0
     */
    static public AppsDatabase getAppsDatabase()
    {
        ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        return am.getAppsDatabase();
    }

    /**
     * Returns the number of applications currently available.
     *
     * @return the number of applications currently available.
     * @since MHP1.0
     */
    public int size()
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return -1;
    }

    /**
     * Returns an enumeration of the application IDs available. The Enumeration
     * will contain the set of AppID that match the filtering criteria. For
     * implementations conforming to this version of the specification, only
     * <code>CurrentServiceFilter</code> or
     * <code>RunningApplicationsFilter</code> filters may return a non empty
     * Enumeration. If the filter object is not an instance of
     * <code>CurrentServiceFilter</code> or
     * <code>RunningApplicationsFilter</code> or one of their subclasses then,
     * the method shall return an empty Enumeration.
     *
     * This method will return an empty Enumeration if there are no matching
     * applications.
     *
     * @param filter
     *            the filter to apply
     *
     * @return the applications available matching the filtering criteria
     * @since MHP1.0
     */

    public Enumeration getAppIDs(AppsDatabaseFilter filter)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return null;
    }

    /**
     * Returns an enumeration of AppAttributes of the applications available.
     * The Enumeration will contain the set of AppAttributes that satisfy the
     * filtering criteria. For implementations conforming to this version of the
     * specification, only <code>CurrentServiceFilter</code> or
     * <code>RunningApplicationsFilter</code> filters may return a non empty
     * Enumeration. If the filter object is not an instance of
     * <code>CurrentServiceFilter</code> or
     * <code>RunningApplicationsFilter</code> or a subclass of either then, the
     * method shall return an empty Enumeration.
     * <p>
     * This method shall return instances which reflect the contents of the
     * database at the time the method is called. After an AppsDatabaseEvent has
     * been generated, new instances may be returned. After a service selection
     * has taken place, applications which survived the service selection may
     * call this method in order to discover the attributes of the applications
     * signalled on the new service.
     * <p>
     * This method will return an empty Enumeration if there are no attributes.
     *
     * @param filter
     *            the filter to apply
     * @return an enumeration of the applications attributes.
     * @since MHP1.0
     */
    public Enumeration getAppAttributes(AppsDatabaseFilter filter)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return null;
    }

    /**
     * Returns the properties associated with the given ID. Returns null if no
     * such application is available.
     * <p>
     * Only one AppAttributes object shall be returned in the case where there
     * are several applications having the same (organisationId, applicationId)
     * pair. In such a case, the same algorithm as would be used to autostart
     * such applications shall be used to decide between the available choices
     * by the implementation.
     * <p>
     * This method shall return instances which reflect the contents of the
     * database at the time the method is called. After an AppsDatabaseEvent has
     * been generated, new instances may be returned. After a service selection
     * has taken place, applications which survived the service selection may
     * call this method in order to discover the attributes of the applications
     * signalled on the new service.
     *
     * @return the value to which the key is mapped in this dictionary or
     *         <code>null</code> if the key is not an application ID, or not
     *         mapped to any application currently available.
     * @param key
     *            an application ID.
     * @since MHP1.0
     */
    public AppAttributes getAppAttributes(AppID key)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return null;
    }

    /**
     * Returns the ApplicationProxy associated with the given ID. Returns null
     * if no such application available.
     * <p>
     * Only one AppProxy object shall be returned in the case where there are
     * several applications having the same (organisationId, applicationId)
     * pair. In such a case, the same algorithm as would be used to autostart
     * such applications shall be used to decide between the available choices
     * by the implementation.
     * <p>If an application has an application instance in the destroyed state then a
     * proxy for that appplication instance shall not be retrieved. Instead, what
     * shall be retrieved is a proxy for another application instance which shall
     * be in the not loaded state unless that application instance has already been started.
     *
     * @return the value to which the key is mapped in this dictionary;
     * @param key
     *            an application ID. <code>null</code> if the key is not an
     *            application ID, or not mapped to any application available.
     * @throws SecurityException
     *             shall not be thrown for AppIDs which are returned by
     *             getAppIDs(CurrentServiceFilter) or
     *             getAppIDs(RunningApplicationsFilter)
     * @since MHP1.0
     */
    public AppProxy getAppProxy(AppID key)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return null;
    }

    /**
     * Add a listener to the database so that an application can be informed if
     * the database changes.
     *
     * @param listener
     *            the listener to be added.
     * @since MHP1.0
     */
    public void addListener(AppsDatabaseEventListener listener)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * remove a listener on the database.
     *
     * @param listener
     *            the listener to be removed.
     * @since MHP1.0
     */
    public void removeListener(AppsDatabaseEventListener listener)
    {
        // this method should not be directly called - the AppDomainImpl
        // subclassed version should be called instead
        return;
    }

}
