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

package javax.tv.service.navigation;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.cablelabs.impl.util.LocatorUtil;

import java.util.Vector;

/**
 * <code>LocatorFilter</code> represents a <code>ServiceFilter</code> based on a
 * set of locators. A <code>ServiceList</code> resulting from this filter will
 * include only services matching the specified locators.
 * 
 * @see Locator
 * @see ServiceList
 */
public final class LocatorFilter extends ServiceFilter
{
    private Locator[] locators;

    private Vector serviceLocators = new Vector();

    /**
     * Constructs the filter based on a set of locators.
     * 
     * @param locators
     *            An array of locators representing services to be included in a
     *            resulting <code>ServiceList</code>.
     * 
     * @throws InvalidLocatorException
     *             If one of the given <code>locators</code> does not reference
     *             a valid <code>Service</code>.
     */
    public LocatorFilter(Locator[] locators) throws InvalidLocatorException
    {
        // Check for Null parameters
        if (locators == null) throw new NullPointerException("null parameter not allowed");

        SIManager simgr = SIManager.createInstance();

        // Check all locators
        for (int i = 0; i < locators.length; i++)
        {
            // Check for Null parameters
            if (locators[i] == null) throw new InvalidLocatorException(locators[i], "null locator not allowed");

            // Must be a proper Service or Network locator
            if (!(LocatorUtil.isService(locators[i]) || LocatorUtil.isNetwork(locators[i])))
                throw new InvalidLocatorException(locators[i], "Locator was not an OCAP service or network locator");

            // insure that this locator points to a valid service
            try
            {
                // attempt to retrieve the service that corresponds to this
                // locator
                Service sv = simgr.getService(locators[i]);

                // save off this service for future use
                // keyed by the canonical form of the locator (from the service)
                serviceLocators.addElement(sv.getLocator());
            }
            catch (SecurityException e)
            {
                throw new InvalidLocatorException(locators[i], "locator does not refer to a valid service");
            }
            // allow the original (checked) InvalidLocatorException to pass up
            // unimpeded
            // allow all other (unchcecked) exceptions to pass thru unimpeded
        }

        // Save original list of locators
        this.locators = locators;
    }

    /**
     * Reports the locators used to create this filter.
     * 
     * @return The array of locators used to create this filter.
     */
    public Locator[] getFilterValue()
    {
        return this.locators;
    }

    /**
     * Tests if the given service passes the filter. The service passes the
     * filter if, for one or more Locators specified in the filter constructor,
     * <code>locator.equals(Service.getLocator())</code> returns
     * <code>true</code>.
     * 
     * @param service
     *            An individual <code>Service</code> to be evaluated against the
     *            filtering algorithm.
     * 
     * @return <code>true</code> if <code>service</code> passes the filter;
     *         <code>false</code> otherwise.
     * 
     * @see javax.tv.service.Service#getLocator
     * @see javax.tv.locator.Locator#equals
     * @see javax.tv.locator.Locator#toExternalForm
     */
    public boolean accept(Service service)
    {
        // Check the saved Service object locators to see if we can find a
        // canonical
        // service locator that matches the canonical locator for this requested
        // service
        // - return 'true' if we find a match or 'false' if we don't
        return (serviceLocators.contains(service.getLocator()));
    }
}
