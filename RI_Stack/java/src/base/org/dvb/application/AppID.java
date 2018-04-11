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

package org.dvb.application;

/**
 * The <code>AppID</code> is a representation of the unique identifier for
 * applications.
 * <p>
 * Its string form is the Hex representation of the 48 bit number.
 */
public class AppID
{
    /**
     * This method returns the integer value of the organization number supplied
     * in the constructor.
     * 
     * @return the integer value of the organization number supplied in the
     *         constructor.
     * @since MHP1.0
     */
    public int getOID()
    {
        return OID;
    }

    /**
     * This method returns the integer value of the application count supplied
     * in the constructor
     * 
     * @return the integer value of the application count supplied in the
     *         constructor
     * @since MHP1.0
     */
    public int getAID()
    {
        return AID;
    }

    /**
     * Create a new AppID based on the given integers. There is no range
     * checking on these numbers.
     * 
     * @param oid
     *            the globally unique organization number.
     * @param aid
     *            the unique count within the organization.
     * @since MHP1.0
     */
    public AppID(int oid, int aid)
    {
        this.OID = oid;
        this.AID = aid;
    }

    /**
     * This method returns a string containing the Hex representation of the 48
     * bit number. The string shall be formatted as specified in the section on
     * "Text encoding of application identifiers" in the System Integration
     * chapter of the MHP specification.
     * 
     * @return a string containing the Hex representation of the 48 bit number.
     * @since MHP1.0
     */
    public String toString()
    {
        return Long.toHexString(longID());
    }

    /**
     * Compares two AppIDs for equality.
     * 
     * @param obj
     *            the reference object with which to compare.
     * @return <code>true</code> if this obj is an AppID and its Organisation ID
     *         and its Application ID match the ID's for this AppID;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj)
    {
        return (obj != null) && (obj instanceof AppID) && ((AppID) obj).AID == AID && ((AppID) obj).OID == OID;
    }

    /**
     * Returns a hash code value for this AppID. The hashcode for two AppID's
     * with the same Organisation ID and Application ID are equal.
     * 
     * @return a hash code value for this AppID
     */
    public int hashCode()
    {
        return AID ^ (OID << 16);
    }

    private long longID()
    {
        long id;
        id = ((long) OID & 0xffffffff) << 16;
        id |= AID & 0xffff;

        return id & 0xffffffffffffL;
    }

    private int OID;

    private int AID;
}
