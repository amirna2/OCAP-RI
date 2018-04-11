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

import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;

/**
 * <code>SIElementFilter</code> represents a <code>ServiceFilter</code> based on
 * a particular <code>SIElement</code> (such as a <code>TransportStream</code>
 * or <code>ProgramEvent</code>). A <code>ServiceList</code> resulting from this
 * filter will include only <code>Service</code> objects with one or more
 * corresponding <code>ServiceDetails</code>, <code>sd</code>, such that:
 * <ul>
 * <li> <code>sd</code> is contained by the specified <code>SIElement</code>, or
 * <li><code>sd</code> contains the specified <code>SIElement</code>
 * </ul>
 * -- according to the type of <code>SIElement</code> provided. Note that no
 * guarantee is made that every <code>SIElement</code> type is supported for
 * filtering.
 * 
 * @see SIElement
 * @see ServiceList
 */
public final class SIElementFilter extends ServiceFilter
{
    private SIElement element;

    /**
     * Constructs the filter based on a particular <code>SIElement</code>.
     * 
     * @param element
     *            An <code>SIElement</code> indicating the services to be
     *            included in a resulting service list.
     * 
     * @throws FilterNotSupportedException
     *             If <code>element</code> is not supported for filtering.
     */
    public SIElementFilter(SIElement element) throws FilterNotSupportedException
    {
        // Do the check for for null parameters first (before anything else)
        if (element == null)
        {
            throw new NullPointerException("null element parameter not supported");
        }

        // TODO(Todd): if we do support ProgramEvents we need to allow them in
        // this filter.
        // modify here to allow it and modify the accept() call to support it.
        if ((element instanceof ServiceDetailsExt) || (element instanceof ServiceComponentExt)
                || (element instanceof TransportStreamExt) || (element instanceof NetworkExt))
        {
            this.element = element;
        }
        else
        {
            throw new FilterNotSupportedException("SIElement not supported for the filtering");
        }
    }

    /**
     * Reports the <code>SIElement</code> used to create this filter.
     * 
     * @return The <code>SIElement</code> used to create this filter.
     */
    public SIElement getFilterValue()
    {
        return element;
    }

    /**
     * Tests if the given service passes the filter.
     * 
     * @param service
     *            An individual <code>Service</code> to be evaluated against the
     *            filtering algorithm.
     * 
     * @return <code>true</code> if <code>service</code> has a corresponding
     *         <code>ServiceDetails</code> which contains or is contained by the
     *         <code>SIElement</code> indicated by the filter value;
     *         <code>false</code> otherwise.
     */
    public boolean accept(Service service)
    {
        // Do the check for for null parameters first (before anything else)
        if (service == null) throw new NullPointerException("null element parameter not supported");

        // Fail if service passed to this method is not a ServiceExt
        if (!(service instanceof ServiceExt)) return false;

        // Get our results from the requestor
        ServiceDetails[] results = null;
        try
        {
            SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
            results = sim.getServiceDetails(service.getLocator());
        }
        catch (Exception ex)
        {
            return false;
        }

        // Fail if there are no results
        if (results == null) return false;

        // Go through the array and match the results against the element filter
        for (int i = 0; i < results.length; i++)
        {
            ServiceDetailsExt details = (ServiceDetailsExt) results[i];

            // TODO(Todd): Add support for element of type ProgramEvent.

            if (element instanceof NetworkExt)
            {
                if (details.getTransportStream() != null
                        && ((TransportStreamExt) details.getTransportStream()).getNetwork().equals(element))
                    return true;
            }
            else if (element instanceof TransportStreamExt)
            {
                if (details.getTransportStream() != null && details.getTransportStream().equals(element)) return true;
            }
            else if (element instanceof ServiceDetailsExt)
            {
                if (details.equals(element)) return true;
            }
            else if (element instanceof ServiceComponentExt)
            {
                if (details.equals(((ServiceComponentExt) element).getServiceDetails())) return true;
            }
        }
        return false;
    }
}
