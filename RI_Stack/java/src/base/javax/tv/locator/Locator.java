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

package javax.tv.locator;

/**
 * The <code>Locator</code> interface provides an opaque reference to the
 * location information of objects which are addressable within the Java TV API.
 * A given locator may represent a transport independent object and have
 * multiple mappings to transport dependent locators. Methods are provided for
 * discovery of such circumstances and for transformation to transport dependent
 * locators.
 * 
 * @see javax.tv.locator.LocatorFactory
 * @see javax.tv.locator.LocatorFactory#transformLocator
 */
public interface Locator
{

    /**
     * Generates a canonical, string-based representation of this
     * <code>Locator</code>. The string returned may be entirely
     * platform-dependent. If two locators have identical external forms, they
     * refer to the same resource. However, two locators that refer to the same
     * resource may have different external forms.
     * <p>
     * 
     * This method returns the canonical form of the string that was used to
     * create the Locator (via <code>LocatorFactory.createLocator()</code>). In
     * generating canonical external forms, the implementation will make its
     * best effort at resolving locators to one-to-one relationships with the
     * resources that they reference.
     * <p>
     * 
     * The result of this method can be used to create new <code>Locator</code>
     * instances as well as other types of locators, such as JMF
     * <code>MediaLocator</code>s and <code>URL</code>s.
     * 
     * @return A string-based representation of this Locator.
     * 
     * @see LocatorFactory#createLocator
     * @see javax.media.MediaLocator javax.media.MediaLocator
     * @see java.net.URL
     */
    public String toExternalForm();

    /**
     * Indicates whether this <code>Locator</code> has a mapping to multiple
     * transports.
     * 
     * @return <code>true</code> if multiple transformations exist for this
     *         <code>Locator</code>, false otherwise.
     */
    public boolean hasMultipleTransformations();

    /**
     * Compares this <code>Locator</code> with the specified object for
     * equality. The result is <code>true</code> if and only if the specified
     * object is also a <code>Locator</code> and has an external form identical
     * to the external form of this <code>Locator</code>.
     * 
     * @param o
     *            The object against which to compare this <code>Locator</code>.
     * 
     * @return <code>true</code> if the specified object is equal to this
     *         <code>Locator</code>.
     * 
     * @see java.lang.String#equals(Object)
     */
    public boolean equals(Object o);

    /**
     * Generates a hash code value for this <code>Locator</code>. Two
     * <code>Locator</code> instances for which <code>Locator.equals()</code> is
     * <code>true</code> will have identical hash code values.
     * 
     * @return The hash code value for this <code>Locator</code>.
     * 
     * @see #equals(Object)
     */
    public int hashCode();

    /**
     * Returns the string used to create this locator.
     * 
     * @return The string used to create this locator.
     * 
     * @see LocatorFactory#createLocator
     */
    public String toString();

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
