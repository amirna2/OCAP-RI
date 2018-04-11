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

package org.davic.mpeg;

/**
 * NotAuthorizedInterface shall be implemented by classes which can report
 * failure to access broadcast content due to failure to descramble that
 * content. The interface provides an ability for an application to find out
 * some information about the reason for the failure.
 * 
 * @version created for DAVIC 1.3.1 from NotAuthorizedException
 * 
 */

public interface NotAuthorizedInterface
{
    /**
     * Major reason - access may be possible under certain conditions.
     */
    public final static int POSSIBLE_UNDER_CONDITIONS = 0;

    /**
     * Major reason - access not possible
     */
    public final static int NOT_POSSIBLE = 1;

    /**
     * Minor reason for POSSIBLE_UNDER_CONDITIONS - user dialog needed for
     * payment
     */
    public final static int COMMERCIAL_DIALOG = 1;

    /**
     * Minor reason for POSSIBLE_UNDER_CONDITIONS - user dialog needed for
     * maturity
     */
    public final static int MATURITY_RATING_DIALOG = 2;

    /**
     * Minor reason for POSSIBLE_UNDER_CONDITIONS - user dialog needed for
     * technical purposes.
     */
    public final static int TECHNICAL_DIALOG = 3;

    /**
     * Minor reason for POSSIBLE_UNDER_CONDITIONS - user dialog needed to
     * explain about free preview.
     */
    public final static int FREE_PREVIEW_DIALOG = 4;

    /**
     * Minor reason for NOT_POSSIBLE - user does not have an entitlement
     */
    public final static int NO_ENTITLEMENT = 1;

    /**
     * Minor reason for NOT_POSSIBLE - user does not have suitable maturity
     */
    public final static int MATURITY_RATING = 2;

    /**
     * Minor reason for NOT_POSSIBLE - a technical reason of some kind
     */
    public final static int TECHNICAL = 3;

    /**
     * Minor reason for NOT_POSSIBLE - not allowed for geographical reasons
     */
    public final static int GEOGRAPHICAL_BLACKOUT = 4;

    /**
     * Minor reason for both POSSIBLE_UNDER_CONDITIONS and NOT_POSSIBLE. Another
     * reason.
     */
    public final static int OTHER = 5;

    /**
     * The component to which access was refused was a MPEG Program/DVB Service
     * 
     * @see NotAuthorizedInterface#getType
     */
    public final static int SERVICE = 0;

    /**
     * The component to which access was refused was an MPEG elementary stream
     * 
     * @see NotAuthorizedInterface#getType
     */
    public final static int ELEMENTARY_STREAM = 1;

    /**
     * @return SERVICE or ELEMENTARY_STREAM to indicate that either a service
     *         (MPEG program) or one or more elementary streams could not be
     *         descrambled.
     */
    public int getType();

    /**
     * If getType() returns SERVICE, then this method returns the Service that
     * could not be descrambled. Otherwise it returns null.
     * 
     * @return either the Service that could not be descrambled or null
     */
    public Service getService();

    /**
     * If getType() returns ELEMENTARY_STREAM, then this method returns the set
     * of ElementaryStreams that could not be descrambled. Otherwise it returns
     * null.
     * 
     * @return either the set of ElementaryStreams that could not be descrambled
     *         or null
     */
    public ElementaryStream[] getElementaryStreams();

    /**
     * Returns the reason(s) why descrambling was not possible.
     * 
     * @param index
     *            If the component to which access failed is a Service, index
     *            shall be 0. Otherwise index shall refer to one stream in the
     *            set returnedby getElementaryStreams().
     * 
     * @return an array of length 2 where the first element of the array is the
     *         major reason and the second element of the array is the minor
     *         reason.
     * 
     * @exception IndexOutOfBoundsException
     *                If the component to which access failed is a Service, this
     *                exception will be thrown if index is non zero. If the
     *                component(s) to which access failed was a (set of)
     *                elementary streams then this exception will be thrown
     *                where index is beyond the size of the array returned by
     *                getElementaryStreams.
     * 
     * @see NotAuthorizedInterface#getElementaryStreams
     */
    public int[] getReason(int index) throws java.lang.IndexOutOfBoundsException;
}
