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

package org.cablelabs.impl.manager.pod;

import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.util.NativeHandle;

/**
 * This class is used to control and monitor a Conditional Access decryption
 * activity. A valid CASession is a precondition to certain activities that 
 * require data from the cable network. e.g. broadcast service decode, 
 * time-shift buffering, and section acquisition.
 * 
 * @author Alan Cossitt - enableTV
 * @author Craig Pratt - enableTV
 */

public interface CASession extends NativeHandle
{
    /**
     * ca_enable codes from CCIF 9.7
     */
    interface CAStatus
    {
        int UNDEFINED = 0x0;
        int DESCRAMBLING_POSSIBLE_NO_CONDITIONS = 0x01;
        int DESCRAMBLING_POSSIBLE_WITH_CONDITIONS_PURCHASE = 0x02;
        int DESCRAMBLING_POSSIBLE_WITH_CONDITIONS_TECHNICAL = 0x03;
        int DESCRAMBLING_NOT_POSSIBLE_NO_ENTITLEMENT = 0x71;
        int DESCRAMBLING_NOT_POSSIBLE_TECHNICAL = 0x73;
    }
    /**
     * Stop any decryption activity represented by this session. If CableCard resources are
     * backing this session, a ca_pmt_cmd_id of 0x04 (not selected) will be issued for the 
     * program/streams managed in this session.
     * 
     * @throws IllegalStateException if the session has already been closed
     */
    public void stop()
        throws IllegalStateException;

    /**
     * Get the last event returned on this session.
     * 
     * @return the EventID
     */
    public CASessionEvent getLastEvent();

    /**
     * Get the Logical Transport Stream ID associated with this session.
     * 
     * @return the LTSID
     */
    public short getLTSID();
    
    /**
     * The value given when no LTSID is defined (The MPE CA manager never uses 0 as an LTSID). 
     */
    public static final short LTSID_UNDEFINED = 0;

    /**
     * Update the passed-in array of CAElementaryStreamAuthorizations with appropriate CA authorization reasons
     *
     * @param authorizations array of CAElementaryStreamAuthorizations which will have the authorization reason updated
     *
     * @throws MPEException if stream authorizations could not be updated with authorization reasons
     */
    void getStreamAuthorizations(CAElementaryStreamAuthorization[] authorizations) throws MPEException;    
}
