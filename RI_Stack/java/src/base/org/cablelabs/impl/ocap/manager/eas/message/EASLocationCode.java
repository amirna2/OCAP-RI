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

package org.cablelabs.impl.ocap.manager.eas.message;

/**
 * An immutable instance of this class represents a regional definition (aka:
 * location code) for which an Emergency Alert event shall apply. An instance
 * can also represent the geographical location of the receiving device.
 * <p>
 * No accessors are provided for the individual fields as no requirement has
 * presented itself. Comparisons between instances are correctly handled by the
 * {@link #equals(EASLocationCode)} method, including the various levels of
 * granularity (e.g. nation-wide, state-wide, county-wide).
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class EASLocationCode
{
    // Following constant defines an unspecified location that matches all other
    // locations.
    public static final EASLocationCode UNSPECIFIED = new EASLocationCode();

    // Following constants defined per SCTE 18 2007 5 and 47 C.F.R.
    // 11.31(c)-(f).
    public static final int ALL_COUNTIES = 0;

    public static final int ALL_STATES = 0;

    public static final int ALL_SUBDIVISIONS = 0;

    public static final int MAX_COUNTIES = 999;

    public static final int MAX_STATES = 99;

    public static final int MAX_SUBDIVISIONS = 9;

    /**
     * An unsigned 8-bit number in the range 0 to 99 that represents the State,
     * Territory or Offshore (Marine Area) affected by the emergency alert. This
     * field shall be coded according to State and Territory FIPS number codes
     * per 47 C.F.R. 11.31(f). The value of 0 shall indicate all states, or a
     * national level alert.
     */
    private final int state_code;

    /**
     * An unsigned 4-bit number in the range 0 to 9 that defines county
     * subdivisions affected by the emergency alert. This field shall be coded
     * according to the county subdivision number codes per 47 C.F.R. 11.31(c).
     * A value of 0 shall indicate the entire county.
     */
    private final int county_subdivision;

    /**
     * An unsigned 10-bit number in the range 0 to 999 that identifies a county
     * within a state, and shall be the numeric representation of the CCC
     * field in the EAS Protocol coded as defined in 47 C.F.R. 11.31(c), which
     * states that county codes use the State and Territory Federal Information
     * Processing Standard (FIPS) numbers as described by the U.S. Department of
     * Commerce in the National Institute of Standards and Technology
     * publication FIPS PUB 6-4. The value of 0 shall indicate the entire state
     * or territory.
     */
    private final int county_code;

    /**
     * Constructs an instance of the receiver using the given 3-byte location
     * code which is encoded per SCTE 18 2007 5 and is summarized as follows:
     * <table>
     * <tr valign=bottom>
     * <th>Syntax</th>
     * <th>No. of Bits</th>
     * <th>Mnemonic</th>
     * </tr>
     * <tr valign=top>
     * <td><code>EA_location_code() {</code></td>
     * <td align=center>&nbsp;</code>
     * <td align=center>&nbsp;</code>
     * </tr>
     * <tr valign=top>
     * <td><code>&nbsp;&nbsp;state_code</code></td>
     * <td align=center>8</code>
     * <td align=center>uimsbf</code>
     * </tr>
     * <tr valign=top>
     * <td><code>&nbsp;&nbsp;county_subdivision</code></td>
     * <td align=center>4</code>
     * <td align=center>uimsbf</code>
     * </tr>
     * <tr valign=top>
     * <td><code>&nbsp;&nbsp;reserved</code></td>
     * <td align=center>2</code>
     * <td align=center>'11'</code>
     * </tr>
     * <tr valign=top>
     * <td><code>&nbsp;&nbsp;county_code</code></td>
     * <td align=center>10</code>
     * <td align=center>uimsbf</code>
     * </tr>
     * <tr valign=top>
     * <td><code>}</code></td>
     * <td align=center>&nbsp;</code>
     * <td align=center>&nbsp;</code>
     * </tr>
     * </table>
     * Field value validation is done separately to support the concept of
     * "strict" versus "lenient" parsing of EAS messages.
     * 
     * @param locationCode
     *            the 3-byte encoded EAS location code
     * @throws IllegalArgumentException
     *             if <code>locationCode</code> is null or the referenced array
     *             is not three bytes in length
     * @see #validate()
     */
    public EASLocationCode(final byte[] locationCode)
    {
        if (null == locationCode || 3 != locationCode.length)
        {
            throw new IllegalArgumentException("EAS location code must encoded as a non-null, 3-byte array");
        }

        this.state_code = locationCode[0] & 0xFF;
        this.county_code = ((locationCode[1] & 0x3) << 8) | (locationCode[2] & 0xFF);
        this.county_subdivision = (locationCode[1] >> 4) & 0x0F;
    }

    /**
     * Constructs a new instance of the receiver with the given state, county
     * and subdivision codes. This constructor is used for unit testing only.
     * 
     * @param stateCode
     *            an unsigned 8-bit number in the range 0 to 99 that represents
     *            the State, Territory or Offshore (Marine Area)
     * @param countyCode
     *            an unsigned 10-bit number in the range 0 to 999 that
     *            identifies a county within a state
     * @param countySubdivision
     *            an unsigned 4-bit number in the range 0 to 9 that defines a
     *            subdivision within a county
     */
    public EASLocationCode(final int stateCode, final int countyCode, final int countySubdivision)
    {
        this.state_code = stateCode & 0xFF;
        this.county_code = countyCode & 0x3FF;
        this.county_subdivision = countySubdivision & 0xF;
    }

    /**
     * Constructs an instance of the receiver with an unspecified location.
     */
    private EASLocationCode()
    {
        this.state_code = EASLocationCode.ALL_STATES;
        this.county_code = EASLocationCode.ALL_COUNTIES;
        this.county_subdivision = EASLocationCode.ALL_SUBDIVISIONS;
    }

    /**
     * Indicates whether the given object is equal to the receiver.
     * 
     * @param obj
     *            the <code>EASLocationCode</code> object to test for equality
     * @return <code>true</code> if the two objects are equal; otherwise
     *         <code>false</code>
     */
    public boolean equals(EASLocationCode obj)
    {
        if (obj == this)
        {
            return true;
        }
        else if (obj == null)
        {
            return false;
        }
        else if (this.state_code == EASLocationCode.ALL_STATES || obj.state_code == EASLocationCode.ALL_STATES)
        {
            return true;
        }
        else if (this.state_code != obj.state_code)
        {
            return false;
        }
        else if (this.county_code == EASLocationCode.ALL_COUNTIES || obj.county_code == EASLocationCode.ALL_COUNTIES)
        {
            return true;
        }
        else if (this.county_code != obj.county_code)
        {
            return false;
        }
        else if (this.county_subdivision == EASLocationCode.ALL_SUBDIVISIONS
                || obj.county_subdivision == EASLocationCode.ALL_SUBDIVISIONS)
        {
            return true;
        }
        else if (this.county_subdivision != obj.county_subdivision)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Indicates whether the given object is equal to the receiver.
     * 
     * @param obj
     *            the object to test for equality
     * @return <code>true</code> if the two objects are equal; otherwise
     *         <code>false</code>
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof EASLocationCode) ? equals((EASLocationCode) obj) : false;
    }

    /**
     * Returns a hash code for the receiver. Algorithm from <cite>Effective
     * Java&#153; Programming Language Guide</cite>, Joshua Block,
     * Addison-Wesley, 2001 (ISBN 0-201-31005-8).
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + this.state_code;
        result = 37 * result + this.county_code;
        result = 37 * result + this.county_subdivision;
        return result;
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("EASLocationCode");
        buf.append(": state=").append(this.state_code);
        buf.append("; county=").append(this.county_code);
        buf.append("; subdivision=").append(this.county_subdivision);
        return buf.toString();
    }

    /**
     * Ensures the state, county, and subdivision codes are valid.
     * 
     * @throws IllegalArgumentException
     *             if <code>locationCode</code> contains an invalid state,
     *             subdivision, or county code
     */
    public void validate()
    {
        if (this.state_code > EASLocationCode.MAX_STATES)
        {
            StringBuffer buf = new StringBuffer("State code must be within the range of ");
            buf.append(EASLocationCode.ALL_STATES).append("..");
            buf.append(EASLocationCode.MAX_STATES).append(":<");
            buf.append(this.state_code).append(">");
            throw new IllegalArgumentException(buf.toString());
        }
        else if (this.county_code > EASLocationCode.MAX_COUNTIES)
        {
            StringBuffer buf = new StringBuffer("County code must be within the range of ");
            buf.append(EASLocationCode.ALL_COUNTIES).append("..");
            buf.append(EASLocationCode.MAX_COUNTIES).append(":<");
            buf.append(this.county_code).append(">");
            throw new IllegalArgumentException(buf.toString());
        }
        else if (this.county_subdivision > EASLocationCode.MAX_SUBDIVISIONS)
        {
            StringBuffer buf = new StringBuffer("County subdivision code must be within the range of ");
            buf.append(EASLocationCode.ALL_SUBDIVISIONS).append("..");
            buf.append(EASLocationCode.MAX_SUBDIVISIONS).append(":<");
            buf.append(this.county_subdivision).append(">");
            throw new IllegalArgumentException(buf.toString());
        }
    }
}
