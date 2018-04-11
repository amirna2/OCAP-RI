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

package org.cablelabs.ocap.util;

import org.dvb.application.AppID;

/**
 * The <code>ConversionUtil</code> class provides common conversion utility
 * functions (e.g. converting an RF channel identification number to a
 * frequency).
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class ConversionUtil
{
    // Class Constants

    /**
     * The map of CEA-542-B RF channel identification numbers to the
     * corresponding standard QAM frequency. The map is ordered by RF channel
     * number. Once the correct entry is found (i.e. iterate across first
     * dimension until <code>rfChannel <= cNum</code>), the frequency is
     * calculated using the formula:
     * 
     * <pre>
     * freq = (((rfChannel - cBase) * 6) + fBase) * 1000000;
     * </pre>
     */
    private static final int[][] CHANNEL_FREQUENCY_MAP = new int[][] { // cNum
                                                                       // cBase
                                                                       // fBase
                                                                       // rfChannel
                                                                       // ->
                                                                       // Frequency
                                                                       // (MHz)
    { 1, 0, 0 }, // 0 - 1 -> IllegalArgumentException
            { 4, 29, 219 }, // 2 - 4 -> 57 - 69
            { 6, 5, 79 }, // 5 - 6 -> 79 - 85
            { 13, 14, 219 }, // 7 - 13 -> 177 - 213
            { 22, 30, 219 }, // 14 - 22 -> 123 - 171
            { 94, 23, 219 }, // 23 - 94 -> 219 - 645
            { 99, 116, 219 }, // 95 - 99 -> 93 - 117
            { 158, 28, 219 }, // 100 - 158 -> 651 - 999
            { 255, 0, 0 }, // 159 - 255 -> IllegalArgumentException
    };

    // Class Methods

    /**
     * Parses the string argument as an unsigned 48-bit unique application
     * identifier. The string must begin with "0x". The remaining characters in
     * the string must be hexadecimal digits.
     * 
     * @param s
     *            a <code>String</code> containing the <code>AppID</code>
     *            representation to be parsed
     * @return the <code>AppID</code> instance representing the argument
     * @throws NumberFormatException
     *             if the string is null, or does not begin with "0x", or does
     *             not contain a parsable <code>long</code> value
     */
    public static AppID parseAppID(String s) throws NumberFormatException
    {
        if (s != null && s.startsWith("0x"))
        {
            long id = Long.parseLong(s.substring(2), 16);
            return new AppID((int) ((id >> 16) & 0xFFFFFFFF), (int) (id & 0xFFFF));
        }

        throw new NumberFormatException("String must be non-null and must start with \"0x\"");
    }

    /**
     * Converts a CEA-542-B RF channel identification number to its
     * corresponding standard QAM frequency.
     * 
     * @param rfChannel
     *            an unsigned 8-bit integer identifying the 6MHz RF frequency
     *            band using the RF channel identification number
     * @return the equivalent standard QAM frequency, in hertz
     * @throws IllegalArgumentException
     *             if the RF channel number is outside the usable range of
     *             2..158 for cable systems
     */
    public static int rfChannelToFrequency(final int rfChannel)
    {
        for (int i = 0; i < CHANNEL_FREQUENCY_MAP.length; ++i)
        {
            if (rfChannel <= CHANNEL_FREQUENCY_MAP[i][0] && 0 != CHANNEL_FREQUENCY_MAP[i][1])
            {
                return (((rfChannel - CHANNEL_FREQUENCY_MAP[i][1]) * 6) + CHANNEL_FREQUENCY_MAP[i][2]) * 1000000;
            }
        }

        throw new IllegalArgumentException("RF channel number not in range of 2..158:<" + rfChannel + ">");
    }
}
