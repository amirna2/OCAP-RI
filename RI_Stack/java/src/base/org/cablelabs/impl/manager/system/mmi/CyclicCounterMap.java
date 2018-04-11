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

package org.cablelabs.impl.manager.system.mmi;

import java.util.HashMap;

/**
 * Class defining a map whose keys are issued from an 8 bit cyclic
 * counter. This class is used for server_query/server_reply transaction
 * numbers and for MMI dialog numbers. The values in the map are the dialogs
 * associated with the pending transactions or dialog numbers.
 */
class CyclicCounterMap
{
    private int nextNumber = 0;

    private HashMap map = new HashMap();

    /**
     * <p>
     * Get the next available number. Starts at 0 and increments by 1,
     * wrapping around after value 255. If the value is in use, the next
     * unused value is returned instead.
     * </p>
     * 
     * <p>
     * Upon return, the number will be placed in the map with a null value.
     * Use {@link put()} to insert the actual value for this number, or
     * {@link remove()} to release the number without using it.
     * </p>
     * 
     * @return next number, or -1 if all 256 numbers are in use.
     */
    public synchronized int getNumber()
    {
        int number;
        int firstAttempt = nextNumber;

        while (true)
        {
            // Get next number from 8 bit cyclic counter
            number = nextNumber++;
            if (256 == nextNumber)
            {
                nextNumber = 0;
            }

            // Is the number in use?
            Integer key = new Integer(number);
            if (!map.containsKey(key))
            {
                // Add number to map with temporary placeholder value
                map.put(key, null);
                return number;
            }

            // Have all possibilities been tried?
            if (firstAttempt == nextNumber)
            {
                return -1;
            }
        }
    }

    /**
     * Associate a value with a number returned from {@link getNumber()}.
     * 
     * @param number
     *            number reserved through call to {@link getNumber()}.
     * 
     * @param value
     *            value to place in map.
     * 
     */
    public synchronized void put(int number, Object value)
    {
        Integer key = new Integer(number);
        map.put(key, value);
    }

    /**
     * Remove a number and its associated value from the map.
     * 
     * @param number
     *            number to remove
     * 
     * @return value previously stored under the key, or null
     * 
     */
    public synchronized Object remove(int number)
    {
        Integer key = new Integer(number);
        return map.remove(key);
    }
    
    public int getSize()
    {
        return map.size();
    }
}
