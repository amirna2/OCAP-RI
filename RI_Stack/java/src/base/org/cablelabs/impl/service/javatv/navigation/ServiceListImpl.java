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

package org.cablelabs.impl.service.javatv.navigation;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.locator.LocatorFactory;
import javax.tv.locator.MalformedLocatorException;
import javax.tv.service.Service;
import javax.tv.service.ServiceNumber;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.SortNotAvailableException;

import org.ocap.net.OcapLocator;

/**
 * The <code>ServiceList</code> implementation.
 * 
 * @author Brian Greene
 * @author Todd Earles
 */
public class ServiceListImpl implements ServiceList
{
    private static Comparator nameComparator = new ServiceNameComparator();

    private static Comparator numberComparator = new ServiceNumberComparator();

    private Vector serviceVector;

    private boolean wasSortByName;

    private boolean wasSortByNumber;

    /**
     * Constructs a ServiceListImpl with the specified services
     * 
     * @param services
     *            The vector of services to construct this service list with
     * @param filter
     *            The service filter to use for filtering the list of services
     */
    public ServiceListImpl(Vector services, ServiceFilter filter)
    {
        serviceVector = new Vector();
        Enumeration e = services.elements();
        while (e.hasMoreElements())
        {
            Service service = (Service) e.nextElement();
            if (filter == null || filter.accept(service)) serviceVector.add(service);
        }
    }

    private ServiceListImpl(Vector v)
    {
        if (v == null)
            serviceVector = new Vector();
        else
            serviceVector = v;
    }

    // Description copied from ServiceList
    public ServiceList sortByName()
    {
        Vector returner = (Vector) serviceVector.clone();
        Collections.sort(returner, nameComparator);
        ServiceListImpl list = new ServiceListImpl(returner);
        list.wasSortByName = true;
        list.wasSortByNumber = false;
        return list;
    }

    // Description copied from ServiceList
    public ServiceList sortByNumber() throws SortNotAvailableException
    {
        Vector returner = (Vector) serviceVector.clone();

        // insure that all of these services implement ServiceNumber
        // (so we can properly sort them by number!)
        for (int i = 0; i < returner.size(); i++)
        {
            if (!(returner.elementAt(i) instanceof ServiceNumber))
            {
                throw new SortNotAvailableException("the service '" + returner.elementAt(i).toString()
                        + "' does not implement ServiceNumber");
            }
        }

        Collections.sort(returner, numberComparator);
        ServiceListImpl list = new ServiceListImpl(returner);
        list.wasSortByName = false;
        list.wasSortByNumber = true;
        return list;
    }

    // Description copied from ServiceList
    public Service findService(Locator locator) throws InvalidLocatorException
    {
        // Convert locator to an OcapLocator
        OcapLocator oLoc;
        try
        {
            oLoc = (OcapLocator) LocatorFactory.getInstance().createLocator(locator.toExternalForm());
        }
        catch (MalformedLocatorException x)
        {
            throw new InvalidLocatorException(locator, "Malformed locator");
        }
        catch (ClassCastException x)
        {
            throw new InvalidLocatorException(locator, "Not an OCAP locator");
        }

        Iterator iter = serviceVector.iterator();
        Service ser = null;
        while (iter.hasNext())
        {
            ser = (Service) iter.next();

            // TODO(Todd): This will only match if locator specifies a source
            // ID. It
            // should also check for a match based on a service name. Keep in
            // mind
            // that a source ID of -1 should never match. A service name of null
            // or
            // "" should never match either.

            if (((OcapLocator) ser.getLocator()).getSourceID() == oLoc.getSourceID()) return ser;
        }
        return null;
    }

    // Description copied from ServiceList
    public ServiceList filterServices(ServiceFilter filter)
    {
        if (filter == null)
        {
            Vector returner = (Vector) serviceVector.clone();
            return new ServiceListImpl(returner);
        }
        Vector newVector = new Vector();
        Iterator iter = serviceVector.iterator();
        Service s = null;
        while (iter.hasNext())
        {
            s = (Service) iter.next();
            if (filter.accept(s)) newVector.add(s);
        }
        return new ServiceListImpl(newVector);
    }

    // Description copied from ServiceList
    public ServiceIterator createServiceIterator()
    {
        return new ServiceIteratorImpl(serviceVector);
    }

    // Description copied from ServiceList
    public boolean contains(Service service)
    {
        // Check for Null parameters
        if (service == null) throw new NullPointerException("null parameters not allowed");

        return serviceVector.contains(service);
    }

    // Description copied from ServiceList
    public int indexOf(Service service)
    {
        return serviceVector.indexOf(service);
    }

    // Description copied from ServiceList
    public int size()
    {
        return serviceVector.size();
    }

    // Description copied from ServiceList
    public Service getService(int index)
    {
        if (index < 0 || index >= serviceVector.size())
            throw new IndexOutOfBoundsException("The specified index was outside the bounds: 0 - "
                    + serviceVector.size());
        return (Service) serviceVector.get(index);
    }

    // Description copied from ServiceList
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        ServiceIterator siter = ((ServiceList) obj).createServiceIterator();
        Iterator thisIter = serviceVector.iterator();
        // check the order of the argument's services
        while (siter.hasNext())
        {
            if (thisIter.hasNext() == false) return false;
            if (!siter.nextService().equals(thisIter.next())) return false;
        }
        // check to see that the internal vector was exhausted.
        if (thisIter.hasNext()) return false;
        return true;
    }

    // Description copied from ServiceList
    public int hashCode()
    {
        int returner;
        if (wasSortByName)
            returner = 31;
        else if (wasSortByNumber)
            returner = 51;
        else
            returner = 73;
        Iterator iter = serviceVector.iterator();
        while (iter.hasNext())
            returner = returner ^ ((Service) iter.next()).hashCode();
        return returner;
    }

    // compares 2 Services based on name.
    private static class ServiceNameComparator implements Comparator
    {
        public int compare(Object lhs, Object rhs)
        {
            if (lhs == null && rhs == null) return 0;
            if (lhs == null && rhs != null) return 1;
            if (rhs == null && lhs != null) return -1;
            if (!(lhs instanceof Service)) return -1;
            if (!(rhs instanceof Service)) return 1;
            return ((Service) lhs).getName().compareTo(((Service) rhs).getName());
        }
    }

    // compares 2 Services base on service number.
    private static class ServiceNumberComparator implements Comparator
    {
        public int compare(Object lhs, Object rhs)
        {
            if (lhs == null && rhs == null) return 0;
            if (lhs == null && rhs != null) return 1;
            if (rhs == null && lhs != null) return -1;
            if (!(lhs instanceof ServiceNumber)) return -1;
            if (!(rhs instanceof ServiceNumber)) return 1;
            return ((ServiceNumber) lhs).getServiceNumber() - ((ServiceNumber) rhs).getServiceNumber();
        }
    }
}
