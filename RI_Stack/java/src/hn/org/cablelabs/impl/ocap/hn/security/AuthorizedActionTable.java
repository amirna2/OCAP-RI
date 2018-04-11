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

import java.net.InetAddress;

import java.util.Hashtable;

/**
 * Table containing Authorized Active UPnP Actions
 * 
 * @author Michael A. Jastad
 * @version 1.0
 * 
 * @see
 */
public class AuthorizedActionTable
{
    /**
     * Reference to the Table
     */
    private Hashtable a_table = null;

    /**
     * Creates a new ActionTable object.
     */
    public AuthorizedActionTable()
    {

    }

    /**
     * Adds an AuthorizedAction to the Table
     * 
     * @param aa
     *            a reference to the Action to be authorized.
     */
    public void addAuthorizedAction(AuthorizedAction aa)
    {
        getTable().put(getKey(aa), aa);
    }

    /**
     * Returns the Authorized Action
     * 
     * @param iAddress
     *            InetAddress of the action
     * @param actionName
     *            the actions name.
     * 
     * @return AuthorizedAction
     */
    public AuthorizedAction getAuthorizedAction(InetAddress iAddress, String actionName)
    {
        return (AuthorizedAction) getTable().get(getKey(iAddress, actionName));
    }

    /**
     * Removes an AuthorizedAction from the table
     * 
     * @param iAddress
     *            InetAddress
     * @param actionName
     *            The action name.
     */
    public void removeAuthorizedAction(InetAddress iAddress, String actionName)
    {
        getTable().remove(getKey(iAddress, actionName));
    }

    public boolean exists(InetAddress iAddress, String actionName)
    {
        return (getAuthorizedAction(iAddress, actionName) != null);
    }


    public void clear()
    {
        getTable().clear();
    }
    /**
     * Generates a key based on the actions InetAddress and action Name.
     * 
     * @param inetAddress
     *            InetAddress
     * @param name
     *            The action Name
     * 
     * @return String the generated key.
     */
    private String getKey(InetAddress inetAddress, String name)
    {
        return (inetAddress.getHostAddress() + name);
    }

    /**
     * Generates a key based on the AuthorizedAction
     * 
     * @param aa
     *            The AuthorizedAction
     * 
     * @return String the generated key
     */
    private String getKey(AuthorizedAction aa)
    {
        return (aa.getInetAddress().getHostAddress() + aa.getAction());
    }

    /**
     * Returns a handle to the table
     * 
     * @return Hashtable
     */
    private Hashtable getTable()
    {
        if (a_table == null)
        {
            a_table = new Hashtable();
        }

        return a_table;
    }
}
