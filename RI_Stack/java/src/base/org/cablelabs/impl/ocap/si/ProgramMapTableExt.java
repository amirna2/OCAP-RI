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

import org.ocap.si.ProgramMapTable;

import javax.tv.service.transport.TransportStream;
import javax.tv.locator.Locator;

import org.cablelabs.impl.service.ProgramMapTableHandle;

/**
 * This extension to the ProgramMapTable interface provides methods for
 * accessing the SourceID and/or Frequency of the service associated with this
 * PMT. Also provides a method which allows the caller to determine if the PMT
 * is associated with the out-of-band channel. This information allows the
 * implementation to match a PMT with a OcapLocator used to request table change
 * notification.
 * 
 * @author Greg Rutz
 */
public interface ProgramMapTableExt extends ProgramMapTable
{
    /**
     * Returns the SourceID of the service associated with this PMT. If the
     * service associated with this PMT is not signaled in the SVCT (such as a
     * VOD service) or is an out-of-band service, this method will return -1.
     * 
     * @return the SourceID of the service associated with this PMT.
     */
    public int getSourceID();

    /**
     * Returns the frequency of the transport stream associated with this PMT.
     * For out-of-band PMT, this method will return -1
     * 
     * @return the frequency of the transport stream associated with this PMT.
     */
    public int getFrequency();

    /**
     * Returns the handle associated with this PMT. The handle is used by the
     * SICache to uniquely identify the PMT
     * 
     * @return the handle associated with this PMT
     */
    public ProgramMapTableHandle getPMTHandle();

    /**
     * Returns the handle associated with this PMT. The handle is used by the
     * SICache to uniquely identify the PMT
     * 
     * @return the handle associated with this PMT
     */
    public int getServiceHandle();
    
    /**
     * Returns the transport stream associated with this PMT
     * 
     * @return the transport stream associated with this PMT
     */
    public TransportStream getTransportStream();

    /**
     * Used to set the locator that was used to request this PMT
     * 
     * @param locator
     *            the locator associated this PMT request
     */
    public void setLocator(Locator locator);
}
