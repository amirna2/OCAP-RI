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

/**
 * The <code>AppsDatabaseEvent</code> class indicates either an entry in the
 * application database has changed, or so many changes have occurred. that the
 * database should be considered totally new. An event with event_id
 * NEW_DATABASE shall always be sent after switching to a new service. After
 * such an event, the contents of the database (both the set of applications and
 * their attributes) shall reflect the new database contents. All former
 * contents of the database shall be discarded except for running externally
 * authorised applications. It is platform dependant if and when a new database
 * event is thrown while tuned to the same service except that a NEW_DATABASE
 * event shall not be sent when only one application has changed within a
 * service.
 * <p>
 * The APP_ADDED, APP_CHANGED and APP_DELETED events shall not be generated in
 * response to the same database change as caused a NEW_DATABASE event to be
 * generated.
 * <p>
 *
 * @since MHP1.0
 */
public class AppsDatabaseEvent extends java.util.EventObject
{
    /**
     * The new database event id.
     */
    static public final int NEW_DATABASE = 0;

    /**
     * The changed event id. The APP_CHANGED event is generated whenever any of
     * the information about an application changes. It is NOT generated when
     * the entry is added to or removed from the AppsDatabase. In such cases,
     * the APP_ADDED or APP_DELETED events will be generated instead.
     */
    static public final int APP_CHANGED = 1;

    /**
     * The addition event id. The APP_ADDED event is generated whenever an entry
     * is added to the AppsDatabase. It is NOT generated when the entry already
     * in the AppsDatabase changes.
     */
    static public final int APP_ADDED = 2;

    /**
     * The deletion event id. The APP_DELETED event is generated whenever an
     * entry is removed from the AppsDatabase.
     */
    static public final int APP_DELETED = 3;

    /**
     * Create a new AppsDatabaseEvent object for the entry in the database that
     * changed, or for a new database.
     * <p>
     *
     * @param id
     *            the cause of the event
     * @param appid
     *            the AppId of the entry that changed
     * @param source
     *            the AppaDatabase object.
     * @since MHP1.0
     */

    public AppsDatabaseEvent(int id, AppID appid, Object source)
    {
        super(source);
        this.eventid = id;
        this.appid = appid;
    }

    /**
     * gets the application ID object for the entry in the database that
     * changed.
     * <p>
     * When the event type is NEW_DATABASE, AppID will be null.
     *
     * @return application ID representing the application
     * @since MHP1.0
     */
    public AppID getAppID()
    {
        return appid;
    }

    /**
     * gets the type of the event.
     * <p>
     *
     * @return an integer that matches one of the static fields describing
     *         events.
     * @since MHP1.0
     */
    public int getEventId()
    {
        return eventid;
    }

    private final AppID appid;

    private final int eventid;
}
