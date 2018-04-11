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

package org.cablelabs.impl.util;

import java.util.*;
import javax.tv.locator.*;

import org.ocap.net.*;

import org.cablelabs.impl.service.NetworkLocator;

/**
 * The <code>LocatorUtil</code> class defines utility methods to be used with
 * locators.
 * 
 * @author Todd Earles
 */
public class LocatorUtil
{
    /**
     * Determine if the specified locator refers to a broadcast service.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if the locator refers to a service, otherwise false.
     */
    public static boolean isService(Locator locator)
    {
        if ((locator != null) && (locator instanceof OcapLocator))
        {
            final OcapLocator ocapLocator = (OcapLocator)locator;
            final boolean isFPQLocator = (ocapLocator.getProgramNumber() != -1)
                                         || (ocapLocator.getModulationFormat() == 255);
            final boolean isSourceIDLocator = ocapLocator.getSourceID() != -1;
            final boolean isServiceNameLocator = ocapLocator.getServiceName() != null;
            return (isFPQLocator || isSourceIDLocator || isServiceNameLocator);
        }
        else
        {
            return false;
        }
    }

    /**
     * Determine if the specified locator refers to a broadcast service component.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if the locator refers to a service component, otherwise
     *         false.
     */
    public static boolean isServiceComponent(Locator locator)
    {
        return (isService(locator) && ((((OcapLocator) locator).getComponentNames().length > 0)
                || (((OcapLocator) locator).getComponentTags().length > 0)
                || (((OcapLocator) locator).getStreamTypes().length > 0) || (((OcapLocator) locator).getPIDs().length > 0)));
    }

    /**
     * Determine if the specified locator refers to a network.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if locator refers to a network, otherwise false.
     */
    public static boolean isNetwork(Locator locator)
    {
        return ((locator != null) && (locator instanceof NetworkLocator));
    }

    /**
     * Determine if the specified locator refers to a program event.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if locator refers to a program event, otherwise false.
     */
    public static boolean isProgramEvent(Locator locator)
    {
        return ((locator != null) && (locator instanceof OcapLocator) && (((OcapLocator) locator).getEventId() != -1));
    }

    /**
     * Determine if the specified locator refers to a transport independent
     * service.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if locator refers to transport independent service,
     *         otherwise false.
     */
    public static boolean isTIService(Locator locator)
    {
        return ((locator != null) && (locator instanceof OcapLocator) && (locator.hasMultipleTransformations()));
    }

    /**
     * Determine if the specified locator refers to a transport dependent
     * service.
     * 
     * @param locator
     *            the locator to be checked.
     * @return true if locator refers to transport dependent service, otherwise
     *         false.
     */
    public static boolean isTDService(Locator locator)
    {
        return ((locator != null) && (locator instanceof OcapLocator) && (!(locator.hasMultipleTransformations())));
    }

    /**
     * Perform a transform on this locator. If no transformations are indicated
     * then return the original locator.
     * 
     * @param source
     *            the locator to be transformed
     * @return a transformed array of locators.
     */
    public static Locator[] transformLocator(Locator source)
    {
        Locator locators[] = null;
        Vector list = (Vector) transforms.get(source.toExternalForm());
        if (list == null)
        {
            locators = new Locator[1];
            locators[0] = source;
        }
        else
        {
            locators = new Locator[list.size()];
            for (int i = 0; i < list.size(); i++)
            {
                locators[i] = (Locator) list.elementAt(i);
            }
        }

        return locators;
    }

    /**
     * Convert a JavaTV locator to an OCAP locator
     * 
     * @param locator
     *            the JavaTV locator to convert
     * @return the OCAP locator
     * @throws InvalidLocatorException
     *             if the given locator could not be converted to an OCAP
     *             locator
     */
    public static OcapLocator convertJavaTVLocatorToOcapLocator(Locator locator) throws InvalidLocatorException
    {
        OcapLocator ol;
        try
        {
            if (locator instanceof OcapLocator)
                ol = (OcapLocator) locator;
            else
                ol = new OcapLocator(locator.toExternalForm());
        }
        catch (Exception e)
        {
            throw new InvalidLocatorException(locator);
        }

        return ol;
    }

    // TODO(Todd): Add support for alternate content
    public static Locator transformToAlternate(Locator locator)
    {
        return null;
    }

    // 1102
    public static boolean isOOB(Locator locator)
    {
        return ((locator != null) && (locator instanceof OcapLocator) && (((OcapLocator) locator).getFrequency() == -1) && (((OcapLocator) locator).getProgramNumber() != -1));
    }

    public static boolean isAbstract(Locator locator)
    {
        return ((locator != null) && (locator instanceof OcapLocator)
                && (((OcapLocator) locator).getSourceID() > 0x10000) && (((OcapLocator) locator).getSourceID() <= 0xFFFFFF));
    }

    private static Hashtable transforms = new Hashtable();
}
