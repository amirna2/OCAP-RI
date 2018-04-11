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
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

/**
 * The <code>HVersion</code> interface defines some versioning constants that
 * are accessible by using the java.lang.System method getProperty, with the
 * appropriate property name.
 * <p>
 * Note that it is a valid implementation to return empty strings for
 * HAVI_IMPLEMENTATION_NAME, HAVI_IMPLEMENTATION_VENDOR and
 * HAVI_IMPLEMENTATION_VERSION strings.
 * <p>
 * In MHP, a call to <code>getProperty()</code> when referencing the constants
 * listed in column <i>Constant</i> in the table below shall return a string as
 * listed in column <i>Value</i>.
 * <table border>
 * <tr>
 * <th>Constant</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>HAVI_SPECIFICATION_VENDOR</td>
 * <td>&quot;DVB&quot;</td>
 * </tr>
 * <tr>
 * <td>HAVI_SPECIFICATION_NAME</td>
 * <td>&quot;MHP&quot;</td>
 * </tr>
 * <tr>
 * <td>HAVI_SPECIFICATION_VERSION</td>
 * <td>&quot;&lt;version&gt;&quot;</td>
 * </tr>
 * </table>
 * &quot;&lt;version&gt;&quot; shall be the MHP version to which this HAVi
 * implementation is conformant, e.g.: &quot;1.0&quot; or &quot;1.0.1&quot; or
 * &quot;1.0.2&quot; or &quot;1.0.3&quot;.
 */

public interface HVersion
{
    /**
     * A string constant describing the HAVi specification vendor, as returned
     * via java.lang.System.getProperty(HVersion.HAVI_SPECIFICATION_VENDOR).
     */
    public static final String HAVI_SPECIFICATION_VENDOR = "havi.specification.vendor";

    /**
     * A string constant describing the HAVi specification name, as returned via
     * java.lang.System.getProperty(HVersion.HAVI_SPECIFICATION_NAME).
     */
    public static final String HAVI_SPECIFICATION_NAME = "havi.specification.name";

    /**
     * A string constant describing the HAVi specification version, as returned
     * via java.lang.System.getProperty(HVersion.HAVI_SPECIFICATION_VERSION).
     */
    public static final String HAVI_SPECIFICATION_VERSION = "havi.specification.version";

    /**
     * A string constant describing the HAVi implementation vendor, as returned
     * via java.lang.System.getProperty(HVersion.HAVI_IMPLEMENTATION_VENDOR).
     */
    public static final String HAVI_IMPLEMENTATION_VENDOR = "havi.implementation.vendor";

    /**
     * A string constant describing the HAVi implementation version, as returned
     * via java.lang.System.getProperty(HVersion.HAVI_IMPLEMENTATION_VERSION).
     */
    public static final String HAVI_IMPLEMENTATION_VERSION = "havi.implementation.version";

    /**
     * A string constant describing the HAVi implementation name, as returned
     * via java.lang.System.getProperty(HVersion.HAVI_IMPLEMENTATION_NAME).
     */
    public static final String HAVI_IMPLEMENTATION_NAME = "havi.implementation.name";

}
