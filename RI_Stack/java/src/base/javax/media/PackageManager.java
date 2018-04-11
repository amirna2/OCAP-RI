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
package javax.media;

import java.util.Vector;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallbackData.SimpleData;

/**
 * A <CODE>PackageManager</CODE> maintains a persistent store of package-prefix
 * lists. A package prefix specifies the prefix for a complete class name. A
 * factory uses a package-prefix list to find a class that might belong to any
 * of the packages that are referenced in the prefix list.
 * <p>
 *
 * The <code>Manager</code> uses package-prefix lists to find protocol handlers
 * and content handlers for time-based media.
 * <p>
 *
 * The current version of a package-prefix list is obtained with the
 * <code>get&lt;package-prefix&gt;List</code> method. This method returns the
 * prefix list in use; any changes to the list take effect immediately.
 *
 * Unless it is made persistent with
 * <code>commit&lt;package-prefix&gt;List</code>, a package-prefix list is only
 * valid while the <code>Manager</code> is referenced.
 *
 * The <code>commit&lt;package-prefix&gt;List</code> method ensures that any
 * changes made to a package-prefix list are still visible the next time that
 * the <code>Manager</code> is referenced.
 *
 * @see Manager
 * @version 1.11, 97/08/23.
 */
public class PackageManager
{
    public PackageManager() { }

    /**
     * Both protocol and content prefix lists have the same value. Access it via
     * {@link #getCommonPrefixList()}.
     */
    static private Vector commonPrefixList = null;

    static private Vector getCommonPrefixList()
    {
        if (commonPrefixList == null)
        {
            commonPrefixList = new Vector();
            commonPrefixList.addElement("org.cablelabs.impl");
        }
        return commonPrefixList;
    }

    /**
     * The package prefix used when searching for protocol handlers.
     *
     * @see Manager
     */
    static Object protoPrefixListKey = new Object();

    /**
     * Get the current value of the protocol package-prefix list.
     * <p>
     *
     * @return The protocol package-prefix list.
     */
    static public Vector getProtocolPrefixList()
    {
        return getPrefixList(protoPrefixListKey);
    }

    /**
     * Set the protocol package-prefix list. This is required for changes to
     * take effect.
     *
     * @param list
     *            The new package-prefix list to use.
     */
    static public void setProtocolPrefixList(Vector list)
    {
        setPrefixList(protoPrefixListKey, list);
    }

    /**
     * Make changes to the protocol package-prefix list persistent.
     * <p>
     * This method throws a <code>SecurityException</code> if the calling thread
     * does not have access to system properties.
     */
    static public void commitProtocolPrefixList()
    {
        throw (new SecurityException("Commit not allowed"));
    }

    /**
     * The package prefix used when searching for content handlers.
     *
     * @see Manager
     */
    static Object contentPrefixListKey = new Object();

    /**
     * Get the current value of the content package-prefix list. Any changes
     * made to this list take effect immediately.
     * <p>
     *
     * @return The content package-prefix list.
     */
    static public Vector getContentPrefixList()
    {
        return getPrefixList(contentPrefixListKey);
    }

    /**
     * Set the current value of the content package-prefix list. This is
     * required for changes to take effect.
     *
     * @param list
     *            The content package-prefix list to set.
     */
    static public void setContentPrefixList(Vector list)
    {
        setPrefixList(contentPrefixListKey, list);
    }

    /**
     * Make changes to the content prefix-list persistent.
     * <p>
     * This method throws a <code>SecurityException</code> if the calling thread
     * does not have access to system properties.
     *
     */
    static public void commitContentPrefixList()
    {
        throw (new SecurityException("Commit not allowed"));
    }

    private static void setPrefixList(Object key, Vector list)
    {
        CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        SimpleData ccData = (SimpleData) ccMgr.getCurrentContext().getCallbackData(key);
        if (ccData == null)
        {
            ccData = new SimpleData(list.clone());
        }
        else
        {
            ccData.setData(list.clone());
        }
        ccMgr.getCurrentContext().addCallbackData(ccData, key);
    }

    private static Vector getPrefixList(Object key)
    {
        CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        SimpleData ccData = (SimpleData) ccMgr.getCurrentContext().getCallbackData(key);
        Vector prefixList;

        if (ccData == null)
        {
            prefixList = getCommonPrefixList();
        }
        else
        {
            prefixList = (Vector) ccData.getData();
        }

        return (Vector) prefixList.clone();
    }
}
