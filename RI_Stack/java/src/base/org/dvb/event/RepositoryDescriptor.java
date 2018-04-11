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

package org.dvb.event;

/**
 * An instance of this class will be sent to clients of the DVB event API to
 * notify them (through the interface org.davic.resources.ResourceClient) when
 * they are about to lose, or have lost, access to an event source. This object
 * can be used by the application to get the name of the repository from which
 * it will no longer be able to receive events.
 * All instances of RepositoryDescriptor are also instances of UserEventRepository.
 * This class is preserved for backwards compatibility with existing applications.
 */
public class RepositoryDescriptor implements org.davic.resources.ResourceProxy
{

    /**
     * Package-private constructor for sub-classes.
     */
    RepositoryDescriptor()
    {
    }

    /**
     * Package-private constructor for sub-classes.
     */
    RepositoryDescriptor(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the repository to which the lost, or about to be
     * lost, user event belongs.
     *
     * @return String the name of the repository.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Return the object which asked to be notified about withdrawal of the event
     * source. This is the object passed as the <code>ResoourceClient</code> to
     * whichever of the various 'add' methods on EventManager was used by the
     * application to express interest in this repository.
     *
     * @return the object which asked to be notified about withdrawl of the
     *         event source. If the <code>UserEventRepository</code> has not yet
     *         been added to an <code>EventManager</code> then null shall be
     *         returned. Once the <code>UserEventRepository</code> has been
     *         added, the last used <code>ResourceClient</code> shall be
     *         returned even if the <code>UserEventRepository</code> has been
     *         since removed.
     */
    public org.davic.resources.ResourceClient getClient()
    {
        // Should be implemented by subclass
        return null;
    }

    /**
     * Repository name.
     */
    private String name;
}
