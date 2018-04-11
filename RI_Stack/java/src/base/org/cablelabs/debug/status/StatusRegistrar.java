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
package org.cablelabs.debug.status;

import java.util.Hashtable;
import java.util.Enumeration;


/**
 * The <code>StatusRegistrar</code> is the centralized component for access to
 * the various status information generators within the stack. It provides
 * access to both java and native status information producers. Status producers
 * register with the <code>StatusRegistrar</code> and consumers of status
 * inforamtion lookup producers using a name string as the key. Once an instance
 * to a status producer is acquired consumers can call the producers directly to
 * acquire status information. The <code>StatusRegistrar</code> provides a
 * default internal implementation for the MPE status producers.
 * 
 */
public class StatusRegistrar
{

    protected StatusRegistrar()
    {
        System.out.println("StatusRegistrar constructor executing");
    }

    static public StatusRegistrar getInstance()
    {
        return singleton;
    }

    /**
     * Register a <code>StatusProducer</code> associated with the specified
     * status producer component identifier and standardized lookup name. If the
     * <code>StatusProducer</code> is null then the associated producer is
     * unregistered.
     * 
     * @param sp
     *            the <code>StatusProducer</code> or null.
     * @param producerId
     *            is the associated producer component identifier.
     */
    public void registerStatusProducer(StatusProducer sp, String name)
    {
        System.out.println("producers = " + producers);

        // Check for removal from registry.
        if (sp == null)
            producers.remove(name); // Remove the producer.
        else
            producers.put(name, sp); // Register the producer.
    }

    /**
     * Acquire all registered producers.
     * 
     * return Enumeration of all of the registered producers.
     */
    public Enumeration getProducers()
    {
        return producers.elements();
    }

    /**
     * Given the logical name of a registered status producer return the
     * associated producer identifier.
     * 
     * @param name
     *            standardized name of the target status producer
     * 
     * @return StatusProducer associated with the specified name or null if one
     *         is not currently registered.
     */
    public StatusProducer getProducer(String name)
    {
        return (StatusProducer) producers.get(name);
    }

    private static Hashtable producers = new Hashtable(1);

    private static StatusRegistrar singleton = new StatusRegistrar();
}
