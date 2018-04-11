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

package org.cablelabs.impl.signalling;

import java.util.Vector;

/**
 * Describes an <code>AbstractService</code> signalled in an XAIT. The source of
 * this information is the <i>abstract_service_descriptor()</i> of the XAIT.
 * 
 * @author Aaron Kamienski
 */
public class AbstractServiceEntry implements Cloneable
{
    /**
     * The service id for this service. The source of this value is the
     * <i>service_id</i> field of the <i>abstract_service_descriptor()</i>.
     * 
     * @see "OCAP-1.0 11.2.2.3.15"
     */
    public int id;

    /**
     * Whether this service should be auto-selected or not. The source of this
     * value is the <i>auto_select</i> field of the
     * <i>abstract_service_descriptor()</i>.
     * 
     * @see "OCAP-1.0 11.2.2.3.15"
     */
    public boolean autoSelect;

    /**
     * The name of this abstract service corresponds to the <i>service_name</i>
     * entry of the <i>abstract_service_descriptor()</i>.
     * 
     * @see "OCAP-1.0 11.2.2.3.15"
     */
    public String name;

    /**
     * A <code>Vector</code> of <code>XAppSignalling</code> entries for each of
     * the unbound applications signalled as part of this service. This is
     * constructed based upon the information found in the
     * <i>unbound_application_descriptor()</i> for each application.
     */
    public Vector apps;

    /**
     * The value of this field is not extracted from signalling. Instead it is a
     * convenience field for the implementation to use in order to mark services
     * that are scheduled to be removed from the services database.
     * 
     * @see "OCAP-1.0 10.2.2.2.2 Abstract Services"
     */
    public boolean markedForRemoval;

    /**
     * Normally, signalling listeners will only receive notifications when
     * signalling changes in some way. However, when an app fails to download
     * due to a transport protocol communication failure, the same signalling
     * must be processed again. This flag indicates whether or not this service
     * entry is a result of "resignalling".
     * 
     * @see "OCAP 10.2.2.3"
     */
    public boolean resignal = false;

    /**
     * The <i>update</i> time for this <code>AbstractServiceEntry</code>. This
     * should be the time at which the XAIT is parsed.
     */
    // TODO: don't assign current time at creation, but time at which XAIT was
    // received.
    public long time = System.currentTimeMillis();
    
    /**
     * We don't store new app versions with their associated service, we store
     * them with the service of the current version.  This field prevents us
     * from ignoring this abstract service because it looks like there are no
     * apps associated with it
     */
    public boolean hasNewAppVersions = false;

    /**
     * Clones this <code>AbstractServiceEntry</code>.
     * 
     * @return a shallow copy of this <code>AbstractServiceEntry</code>.
     */
    public AbstractServiceEntry copy()
    {
        try
        {
            return (AbstractServiceEntry) clone();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public String toString()
    {
        return "AbstractServiceEntry[" + Integer.toHexString(id) + "," + autoSelect + "," + name + "]";
    }
}
