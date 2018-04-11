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

package org.cablelabs.impl.ocap.hn.security;

import org.ocap.hn.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;

/**
 * List containing NetPassword objects
 * 
 * @author Michael A. Jastad
 * @version $Revision$
 * 
 * @see
 */
public class NetPasswordList extends Vector
{
    /**
     * Creates a new NetPasswordList object.
     */
    public NetPasswordList()
    {
        super();
    }

    /**
     * Adds a NetPassword object to the list
     * 
     * @param npw
     *            The NetPassword Object to be added.
     */
    public void addNetPassword(NetPassword npw)
    {
        if (npw != null)
        {
            super.add(npw);
        }
    }

    /**
     * Return a NetPassword Object for a specified index
     * 
     * @param i
     *            an integer value ot index into the array
     * 
     * @return NetPassword from the list specified by the index. Null is
     *         returned if the index is out of range.
     */
    public NetPassword getNetPassword(int i)
    {
        NetPassword np = null;

        if ((i >= 0) && (i < super.size()))
        {
            np = (NetPassword) super.get(i);
        }

        return np;
    }

    /**
     * Returns a NetPassword object for a specified NetworkInterface
     * 
     * @param nwi
     *            The specified NetworkInterface
     * 
     * @return NetPassword. Null if there is no NetPassword Object relative to
     *         the specified NetworkInterface.
     */
    public NetPassword getNetPassword(NetworkInterface nwi)
    {
        NetPassword np = null;

        if (nwi != null)
        {
            for (int i = 0; i < super.size(); ++i)
            {
                np = getNetPassword(i);

                if ((np != null) && (np.getNetworkInterface().getDisplayName().equalsIgnoreCase(nwi.getDisplayName())))
                {
                    break;
                }
            }
        }

        return np;
    }

    /**
     * Returns a NetPassword object for a specified password
     * 
     * @param pw
     *            The specified password
     * 
     * @return NetPassword. Null if there is no NetPassword Object relative to
     *         the specified password.
     */
    public NetPassword getNetPassword(String pw)
    {
        NetPassword np = null;

        if (pw != null)
        {
            for (int i = 0; i < super.size(); ++i)
            {
                np = getNetPassword(i);

                if ((np != null) && (np.getPassword().equalsIgnoreCase(pw)))
                {
                    break;
                }
            }
        }

        return np;
    }

    /**
     * Checks to see if the list contains a NetPassword object relative to a
     * specified NetworkInterface, and Password
     * 
     * @param nwi
     *            The NetworkInterface reference
     * @param pw
     *            The password reference
     * 
     * @return boolean true if the list contains a NetPassword for the specified
     *         criteria. False otherwise.
     */
    public boolean contains(NetworkInterface nwi, String pw)
    {
        NetPassword np = null;
        boolean retVal = false;

        if ((nwi != null) && (pw != null))
        {
            for (int i = 0; i < super.size(); ++i)
            {
                np = getNetPassword(i);

                if ((np != null)
                        && (np.getPassword().equalsIgnoreCase(pw) && np.getNetworkInterface()
                                .getDisplayName()
                                .equalsIgnoreCase(nwi.getDisplayName())))
                {
                    retVal = true;

                    break;
                }
            }
        }

        return retVal;
    }
}
