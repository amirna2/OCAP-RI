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

/*
 * HSceneManager.java
 *
 * Created on November 23, 2004, 10:55 AM
 * per ECO OCAP1.0-O-04.0694-2
 * jdb
 */
package org.ocap.ui;

import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.ocap.application.OcapAppAttributes;

/**
 * This class represents a manager that lets an application register a handler
 * to requested HScene changes within a logical HScreen composited with all
 * HScenes. In addition, HScene z-ordering can be queried using this manager.
 */
public abstract class HSceneManager
{
    /**
     * Protected default constructor.
     **/
    protected HSceneManager()
    {
    }

    /**
     * Gets the singleton instance of the HScene manager. The singleton MAY be
     * implemented using application or implementation scope.
     * 
     * @return The HScene manager.
     **/
    public static HSceneManager getInstance()
    {
        GraphicsManager gm = (GraphicsManager) ManagerManager.getInstance(GraphicsManager.class);

        return gm.getHSceneManager();
    }

    /**
     * Lets an application add itself as the HScene change request handler. If a
     * handler is already registered when this method is called, it is replaced
     * with the parameter handler.
     * 
     * @param handler
     *            HSceneChangeRequestHandler for requests to HScene z-ordering
     *            changes. If this parameter is null the current handler is
     *            removed.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    public void setHSceneChangeRequestHandler(HSceneChangeRequestHandler handler)
    {
        // this method should not be directly called - a subclassed version
        // should be called instead
        return;
    }

    /**
     * Gets the current HScene z-ordering. The array of attributes returned is
     * ordered increasing in z-order where the first entry (0) corresponds to an
     * HScene on top and the last entry is on bottom.
     * 
     * @return Array of application attributes corresponding to HScene instances
     *         in z-order.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    public static OcapAppAttributes[] getHSceneOrder()
    {
        return getInstance().getHSceneOrderImpl();
    }

    /**
     * Gets the current HScene z-order location for a specific HScene.
     * Applications can call this to determine where their HScene is located.
     * 
     * @return HScene z-order location for the calling application. The value is
     *         ordered increasing in z-order where 0 is on top and all other
     *         values are in increasing order below the top. A value of -1
     *         indicates the HScene has not been ordered.
     */
    public int getAppHSceneLocation()
    {
        // this method should not be directly called - a subclassed version
        // should be called instead
        return -1;
    }

    /**
     * This method is not a part of the public API.
     * 
     * Gets the current HScene z-ordering. The array of attributes returned is
     * ordered increasing in z-order where the first entry (0) corresponds to an
     * HScene on top and the last entry is on bottom. This method is here for
     * now due to the fact that getHSceneOrder() is static. This may be an error
     * in the specification.
     * 
     * @return Array of application attributes corresponding to HScene instances
     *         in z-order.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    abstract protected OcapAppAttributes[] getHSceneOrderImpl();
}
