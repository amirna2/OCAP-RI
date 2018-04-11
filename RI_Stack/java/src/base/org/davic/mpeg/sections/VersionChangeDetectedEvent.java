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

package org.davic.mpeg.sections;

/**
 * This class is used by TableSectionFilter to report that a section has been
 * encountered which has a different version_number from earlier sections. It is
 * generated only once per filtering action. The section with a different
 * version_number is ignored.
 * 
 * @version new for DAVIC 1.3.1
 */

public class VersionChangeDetectedEvent extends SectionFilterEvent
{
    private int newVersion = 0;

    private int originalVersion = 0;

    /**
     * This constructs a VersionChangeDetectedEvent event for the specified
     * SectionFilter object.
     * 
     * @param f
     *            the SectionFilter object which filtered the data.
     * @param appData
     *            application data that was passed to the startFiltering method
     */
    public VersionChangeDetectedEvent(SectionFilter f, Object appData)
    {
        super(f, appData);
    }

    /**
     * This constructs a VersionChangeDetectedEvent event for the specified
     * SectionFilter object.
     * 
     * @param f
     *            the SectionFilter object which filtered the data.
     * @param appData
     *            application data that was passed to the startFiltering method
     */
    VersionChangeDetectedEvent(TableSectionFilter f, Object appData, int originalVersion, int newVersion)
    {
        super(f, appData);
        this.newVersion = newVersion;
        this.originalVersion = originalVersion;
    }

    /**
     * This returns the SectionFilter object which filtered the data.
     */
    public Object getSource()
    {
        return super.getSource();
    }

    /**
     * This returns the original version number of the table.
     */
    public int getOriginalVersion()
    {
        return originalVersion;
    }

    /**
     * This returns the version number of the new table.
     */
    public int getNewVersion()
    {
        return newVersion;
    }
}
