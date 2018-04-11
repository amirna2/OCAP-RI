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

package org.ocap.media;


/**
 * This interface represents possible reasons that lead to alternative media
 * presentation. Registered {@link MediaAccessHandler} can define its own
 * reason and pass them to the OCAP implementation through the
 * MediaAccessHandler.checkMediaAccessAuthorization() method.
 */
public interface AlternativeMediaPresentationReason
{

    /**
     * Marks the first bit for the range of alternative media presentation
     * reasons.
     */
    public static final int REASON_FIRST = 0x01;

    /**
     * Bit indicating that service components are ciphered and the user has no
     * entitlement to view all or part of them.
     */
    public final static int NO_ENTITLEMENT = REASON_FIRST;

    /**
     * Reason indicating that media presentation is not authorized regarding to
     * the program rating.
     */
    public final static int RATING_PROBLEM = 0x04;

    /**
     * Bit indicating that media are ciphered and the CA does not correspond to
     * ciphering.
     */
    public final static int CA_UNKNOWN = 0x08;

    /**
     * Bit indicating that broadcast information is inconsistent : for example
     * PMT is missing.
     */
    public final static int BROADCAST_INCONSISTENCY = 0x10;

    /**
     * Bit indicating that hardware resource necessary for presenting service
     * components is not available.
     */
    public final static int HARDWARE_RESOURCE_NOT_AVAILABLE = 0x20;

    /**
     * Marks the last bit for the range of alternative media presentation
     * reasons.
     */
    public static final int REASON_LAST = HARDWARE_RESOURCE_NOT_AVAILABLE;

    /**
     * Bit indicating that a user dialog for payment is necessary before media
     * presentation.
     * */

    public final static int COMMERCIAL_DIALOG = 0x02;

}
