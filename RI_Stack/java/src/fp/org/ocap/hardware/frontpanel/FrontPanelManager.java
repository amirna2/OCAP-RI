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

package org.ocap.hardware.frontpanel;

import org.davic.resources.ResourceClient;
import org.ocap.system.MonitorAppPermission;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.ocap.hardware.frontpanel.FrontPanelManagerImpl;

/**
 * This class represents an optional front panel display and SHOULD not be
 * present in any device that does not support one. A front panel may include a
 * text based display with one or more rows of characters. This API is agnostic
 * as to the type of hardware used in the display (e.g. segmented LED, LCD). The
 * display may also contain individual indicators for status indication such as
 * power.
 */
public class FrontPanelManager
{

    /**
     * Protected constructor. Cannot be used by an application.
     */
    protected FrontPanelManager()
    {
    }

    /**
     * Gets the singleton instance of the front panel manager. The singleton MAY
     * be implemented using application or implementation scope.
     * 
     * @return The front panel manager.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("frontpanel").
     */
    public static FrontPanelManager getInstance()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("frontpanel"));

        synchronized (lock)
        {
            if (instance == null) instance = new FrontPanelManagerImpl();
        }
        return instance;
    }

    /**
     * Reserves the front panel text display for exclusive use by an
     * application. The ResourceProxy of the TextDisplay SHALL be used for
     * resource contention.
     * 
     * @param resourceClient
     *            A DAVIC resource client for resource control.
     * 
     * @return True if the implementation accepted the reservation request,
     *         otherwise returns false.
     */
    public boolean reserveTextDisplay(ResourceClient resourceClient)
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Reserves one of the indicators for exclusive use by an application. The
     * ResourceProxy of the Indicator SHALL be used for resource contention.
     * 
     * @param resourceClient
     *            A DAVIC resource client for resource control.
     * @param indicator
     *            One of the indicator String names found in the table returned
     *            by the {@link FrontPanelManager#getSupportedIndicators}
     *            method.
     * 
     * @return True if the implementation accepted the reservation request,
     *         otherwise returns false.
     * 
     * @throws IllegalArgumentException
     *             if the indicator argument is not contained in the table
     *             returned by the
     *             {@link FrontPanelManager#getSupportedIndicators} method.
     */
    public boolean reserveIndicator(ResourceClient resourceClient, String indicator)
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Releases the front panel text display from a previous reservation. If the
     * calling application is not the application that reserved the front panel,
     * or if the front panel is not reserved when this method is called, this
     * method does nothing.
     */
    public void releaseTextDisplay()
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Releases a front panel indicator from a previous reservation. If the
     * calling application is not the application that reserved the indicator,
     * or if the indicator is not reserved when this method is called, this
     * method does nothing.
     * 
     * @param indicator
     *            One of the indicator String names found in the table returned
     *            by the {@link FrontPanelManager#getSupportedIndicators}
     *            method.
     * 
     * @throws IllegalArgumentException
     *             if the indicator argument is not contained in the table
     *             returned by the
     *             {@link FrontPanelManager#getSupportedIndicators} method.
     */
    public void releaseIndicator(String indicator)
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Gets the front panel text display. A front panel must be reserved before
     * an application can get it with this method.
     * 
     * @return Front panel text display, or null if the application has not
     *         reserved it.
     */
    public TextDisplay getTextDisplay()
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Gets the set of available indicators. The array returned SHALL contain
     * the name of all the indicators supported with
     * {@link #getIndicatorDisplay(String[])}. The set of standardized
     * indicators includes "power", "rfbypass", "message", and "record" and
     * these MAY be returned.
     * 
     * @return The set of supported indicators. MAY return indicators not
     *         included in the standardized set.
     */
    public String[] getSupportedIndicators()
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Gets the individual indicators display. Indicators must be reserved
     * before an application can get them with this method.
     * 
     * @param indicators
     *            Set of indicator names.
     * 
     * @return Set of individual indicators, or null if the application has not
     *         reserved one or more of the parameter indicators.
     * 
     * @throws IllegalArgumentException
     *             if any of the indicator arguments are not contained in the
     *             table returned by the {@link IndicatorDisplay#getIndicators}
     *             method.
     */
    public IndicatorDisplay getIndicatorDisplay(String[] indicators)
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    /**
     * Provides an indication of whether or not text display is supported. For
     * backwards compatibility, the implementation must allow an application to
     * reserve, get, and write to the text display even if the device does not
     * contain a text display. If an application attempts to write to a
     * nonexistent text display, the implementation shall not return an error.
     * 
     * @return True if text display is supported, false if text display is not
     *         supported.
     */
    public boolean isTextDisplaySupported()
    {
        // Expected to be overridden by implementation class
        throw new UnsupportedOperationException("Should not create an instance directly");
    }

    private static Object lock = new Object();

    private static FrontPanelManager instance = null;
}
