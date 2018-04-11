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

import javax.tv.service.*;

/**
 * <code>ServiceTypeFilter</code> represents a <code>ServiceFilter</code> based
 * on a particular <code>ServiceType</code>. A <code>ServiceList</code>
 * resulting from this filter will include only <code>Service</code> objects of
 * the specified service type.
 * 
 * @see ServiceType
 * @see ServiceList
 */
public final class ServiceTypeFilter extends ServiceFilter
{

    private ServiceType type;

    /**
     * Constructs the filter based on a particular <code>ServiceType</code>.
     * 
     * @param type
     *            A <code>ServiceType</code> object indicating the type of
     *            services to be included in a resulting service list.
     */
    public ServiceTypeFilter(ServiceType type)
    {

        // Check for Null parameters
        if (type == null) throw new NullPointerException("null parameters not allowed");

        this.type = type;
    }

    /**
     * Reports the <code>ServiceType</code> used to create this filter.
     * 
     * @return The <code>ServiceType</code> used to create this filter.
     */
    public ServiceType getFilterValue()
    {
        return this.type;
    }

    /**
     * Tests if the given service passes the filter.
     * 
     * @param service
     *            An individual <code>Service</code> to be evaluated against the
     *            filtering algorithm.
     * 
     * @return <code>true</code> if <code>service</code> is of the type
     *         indicated by the filter value; <code>false</code> otherwise.
     */
    public boolean accept(Service service)
    {
        // use of == ok as service types are from a static enumeration.
        return this.type == service.getServiceType();
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
