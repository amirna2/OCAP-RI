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

package org.ocap.system;

import org.cablelabs.impl.manager.ManagerManager;

/**
 * This class represents a manager that allows applications to register
 * listeners for EAS events.
 */
public abstract class EASManager
{
    /**
     * Indicates an EAS message has been received and is about to be processed.
     * This state MAY be entered before an EASListener receives an EAS event.
     */
    public final static int EAS_MESSAGE_RECEIVED_STATE = 0;

    /**
     * Indicates the implementation is processing the EAS message and EAS
     * information is being presented. This state MAY coincide with resources
     * being taken away from applications.
     */
    public final static int EAS_MESSAGE_IN_PROGRESS_STATE = 1;

    /**
     * Indicates an EAS message is not being processed. This state MAY be
     * entered before an EASListener receives an EAS_COMPLETE_EVENT.
     */
    public final static int EAS_NOT_IN_PROGRESS_STATE = 2;

    /**
     * Gets the instance of the EAS Manager class that may be used by the
     * application to register an EASListener.
     * 
     * @return The EAS manager.
     */
    public static EASManager getInstance()
    {
        return ((org.cablelabs.impl.manager.EASManager) ManagerManager.getInstance(org.cablelabs.impl.manager.EASManager.class)).getEASManager();
    }

    /**
     * Adds a listener for EAS events.
     * 
     * @param listener
     *            The new EAS listener.
     */
    public abstract void addListener(EASListener listener);

    /**
     * Removes a listener from receiving EAS events. If the parameter listener
     * wasn't previously added with the <code>addListener</code> method, this
     * method does nothing.
     * 
     * @param listener
     *            The EAS listener to be removed.
     */
    public abstract void removeListener(EASListener listener);

    /**
     * Gets the EAS state. Possible return values are defined by state constants
     * in this class.
     * 
     * @return EAS state.
     */
    public int getState()
    {
        return EAS_NOT_IN_PROGRESS_STATE;
    }
}
