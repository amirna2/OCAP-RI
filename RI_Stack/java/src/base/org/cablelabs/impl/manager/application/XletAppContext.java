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

import java.awt.Container;
import java.util.Hashtable;

import javax.microedition.xlet.UnavailableContainerException;

import org.dvb.application.AppProxy;

/**
 * Implementation of <code>XletContext</code> provided to an <code>Xlet</code>
 * upon calling its {@link javax.tv.xlet.Xlet#initXlet} method.
 * <p>
 * 
 * @author Aaron Kamienski
 */
public class XletAppContext implements javax.tv.xlet.XletContext, javax.microedition.xlet.XletContext
{
    /**
     * Basic constructor that takes the calling XletAppState and its associated
     * AppParameters object.
     * 
     * @param app
     *            the XletApp creating this XletContext Allows the application
     *            access to application properties.
     */
    public XletAppContext(XletApp app)
    {
        this.app = app;
    }

    /**
     * Notify the rest of the system that this application considers itself in
     * the destroyed state.
     */
    public void notifyDestroyed()
    {
        // Specify new state transition function
        app.state.setState(AppProxy.DESTROYED, true);
    }

    /**
     * Notify the rest of the system that this application considers itself in
     * the paused state.
     * 
     * @spec MHP 9.2.4.2 - only valid in ACTIVE state.
     */
    public void notifyPaused()
    {
        // Only valid in STARTED/RESUMED state
        // Silently ignored otherwise
        synchronized (app.state)
        {
            if (app.getState() == AppProxy.STARTED) app.state.setState(AppProxy.PAUSED, true);
        }
    }

    protected static final String APPID = "dvb.app.id";

    protected static final String ORGID = "dvb.org.id";

    protected static final String PARAMS = "dvb.caller.parameters";

    protected static final String CONTAINER = "javax.tv.xlet.container";

    protected static final String SERVICE_CONTEXT = "javax.tv.xlet.service_context";

    /**
     * Request information about the Xlet.
     * <p>
     * Should provide access to <code>AppInfo</code> properties as well as any
     * others that we deem necessary. Including:
     * <ul>
     * <li> <code>ARGS</code> = signalling or XAIT args
     * <li> <code>"dvb.app.id"</code> = <code>AppID.getAID()</code>
     * <li> <code>"dvb.org.id"</code> = <code>AppID.getOID()</code>
     * <li> <code>"dvb.caller.parameters"</code> = <code>start()</code> args
     * <li> <code>"javax.tv.xlet.container"</code> = <i>root container</i>
     * <li> <code>"javax.tv.xlet.service_context"</code> =
     * <code>ServiceContext</code>
     * </ul>
     * 
     * @param key
     *            The name of the property to retrieve
     * @return <code>Object</code> representing the requested property or
     *         <code>null</code> if not found. The application has access to all
     *         properties exposed by the <code>AppParameters</code> object that
     *         was created by the <code>AppsDatabaseMgr</code> object that
     *         listed the application.
     * 
     * @note Currently these data are stored in a <code>Hashtable</code> that is
     *       private to the <code>XletAppContext</code>; we <i>could</i> store
     *       the data as part of the application's <code>CallerContext</code>
     *       making the same data accessible in that manner.
     * 
     * @todo Implement ServiceContext access.
     * @todo Only make some items available when the context is alive? Can that
     *       be handled via CallerContext.checkAlive()?
     */
    public Object getXletProperty(java.lang.String key)
    {
        synchronized (properties)
        {
            Object value = properties.get(key);

            if (value != null) return value;

            if (APPID.equals(key))
                value = Integer.toHexString(app.entry.id.getAID());
            else if (ORGID.equals(key))
                value = Integer.toHexString(app.entry.id.getOID());
            else if ((app.getXlet() instanceof JavaTVXlet && javax.tv.xlet.XletContext.ARGS.equals(key))
                    || (app.getXlet() instanceof JavaMEXlet && javax.microedition.xlet.XletContext.ARGS.equals(key)))
            {
                String[] orig = app.entry.parameters;
                String[] copy = new String[orig.length];
                System.arraycopy(orig, 0, copy, 0, copy.length);
                value = copy;
            }
            else if (CONTAINER.equals(key))
                value = org.havi.ui.HSceneFactory.getInstance().getDefaultHScene();
            else if (SERVICE_CONTEXT.equals(key))
                value = app.getAppDomain().getServiceContext();
            else if (PARAMS.equals(key))
                value = new String[0]; // spec requires empty string array --
                                       // not null -- if PARAMS not set in the
                                       // app launching api (XletApp.doStart())
            else
                return null;

            // Save for later "quick lookup"
            properties.put(key, value);

            return value;
        }
    }

    /**
     * Request that the application be resumed. The application is not resumed
     * until it's startXlet() method is subsequently called. If it is not called
     * then the request could not be honored.
     * 
     * @spec MHP 9.2.4.2 - only valid in PAUSED state.
     * @note Currently only allowed in PAUSED-after-ACTIVE state...
     */
    public void resumeRequest()
    {
        // Only valid in PAUSED state
        // Silently ignored otherwise
        if (app.state.getState() == AppProxy.PAUSED) app.resumeRequest();
    }

    public ClassLoader getClassLoader()
    {
        return app.getAppClassLoader();
    }

    public Container getContainer() throws UnavailableContainerException
    {
        return org.havi.ui.HSceneFactory.getInstance().getDefaultHScene();
    }

    /**
     * Sets the given property to the given value.
     * 
     * @param property
     *            the xlet property to assign
     * @param value
     *            the new value
     */
    void setXletProperty(String property, Object value)
    {
        properties.put(property, value);
    }

    /**
     * Reference back to the owning application.
     */
    private XletApp app;

    /**
     * The set of properties.
     */
    private Hashtable properties = new Hashtable();
}
