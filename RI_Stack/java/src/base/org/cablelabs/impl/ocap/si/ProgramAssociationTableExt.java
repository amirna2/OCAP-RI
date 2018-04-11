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

package org.cablelabs.impl.ocap.si;

import org.ocap.si.ProgramAssociationTable;

import javax.tv.service.transport.TransportStream;
import javax.tv.locator.Locator;

import org.cablelabs.impl.service.ProgramAssociationTableHandle;

/**
 * This extension to the ProgramAssociationTable interface provides methods for
 * accessing the SourceIDs and/or Frequency of the services associated with this
 * PAT. This information allows the implementation to match a PAT with a
 * OcapLocator used to request table change notification.
 * 
 * @author Greg Rutz
 */
public interface ProgramAssociationTableExt extends ProgramAssociationTable
{
    /**
     * Returns all of the SourceIDs of the services associated with this PAT.
     * Any services associated with this PAT that are not signaled in the SVCT
     * (such as a VOD service) will not be represented in this array. For
     * out-of-band PAT, this method will return null.
     * 
     * @return an array of all SVCT-signaled services associated with this PAT.
     */
    int[] getSourceIDs();

    /**
     * Returns the frequency of the transport stream associated with this PAT.
     * For out-of-band PAT, this method will return -1
     * 
     * @return the frequency of the transport stream associated with this PAT.
     */
    int getFrequency();

    /**
     * Returns the handle associated with this PAT. The handle is used by the
     * SICache to uniquely identify this PAT.
     * 
     * @return the handle associated with this PAT
     */
    ProgramAssociationTableHandle getPATHandle();

    /**
     * Returns the transport stream associated with this PAT
     * 
     * @return the transport stream associated with this PAT
     */
    public TransportStream getTransportStream();

    /**
     * Returns the transport stream Id associated with this PAT
     * 
     * @return the transport stream Id associated with this PAT
     */
    public int getTransportStreamId();
    
    /**
     * Used to set the locator that was used to request this PAT
     * 
     * @param locator
     *            the locator associated this PAT request
     */
    public void setLocator(Locator locator);
}
