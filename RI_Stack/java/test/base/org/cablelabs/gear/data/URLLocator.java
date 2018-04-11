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

package org.cablelabs.gear.data;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * An implementation of {@link Locator} which is based upon a <code>URL</code>.
 * This is an immutable class with the <code>URL</code> value assignable only in
 * the constructor.
 * 
 * @author Jay Tracy
 * @version $Revision: 1.2 $, $Date: 2002/06/03 21:31:10 $
 */
public class URLLocator implements Locator
{
    /**
     * <code>URL</code> constructor. The given <code>URL</code> is saved and
     * will be returned by {@link #getLocation()}, specifying the location of
     * the resource.
     * 
     * @param url
     *            the location of the resource
     */
    public URLLocator(URL url)
    {
        this.url = url;
    }

    /**
     * <code>String</code> constructor. The given <code>String</code> is used to
     * construct an <code>URL</code> which is stored and used to specify the
     * location of the resource.
     * <p>
     * If a valid <code>URL</code> cannot be constructed for this resource, then
     * {@link #getLocation()} will return <code>null</code>.
     * 
     * @param location
     *            the location of the resource
     */
    public URLLocator(String location)
    {
        try
        {
            url = new URL(location);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    // Description copied from Locator interface
    public URL getLocation()
    {
        return url;
    }

    /**
     * The URL that locates the resource.
     */
    private URL url;
}
