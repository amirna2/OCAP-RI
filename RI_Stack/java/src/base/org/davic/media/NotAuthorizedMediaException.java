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

package org.davic.media;

/**
 * This exception indicates that the source can not be accessed in order to
 * reference the new content, the source has not been accepted.
 */
public class NotAuthorizedMediaException extends NotAuthorizedException implements
        org.davic.mpeg.NotAuthorizedInterface
{
    /**
     * Constructor for exception due to failure accessing an MPEG service
     * 
     * @param s
     *            the service which could not be accessed
     * @param reason
     *            the reason why the service could not be accessed
     */
    public NotAuthorizedMediaException(org.davic.mpeg.Service s, int reason)
    {
    }

    /**
     * Constructor for exception due to failure accessing one or more MPEG
     * elementary streams The caller of this constructor is responsible for
     * ensuring the two arrays provided as parameters are the same size. The
     * implementation is not expected to check this.
     * <p>
     * Use of the constructor NotAuthorizedMediaException(ElementaryStream[] e,
     * int[] reason) will result in the major reason for each elementary stream
     * being the one specified in the reason parameter to the method and the
     * minor reason being OTHER as defined in NotAuthorizedInterface.
     * 
     * @param e
     *            the elementary streams which could not be accessed
     * @param reason
     *            the reason why the exception was thrown for each elementary
     *            stream
     */
    public NotAuthorizedMediaException(org.davic.mpeg.ElementaryStream[] e, int reason[])
    {
    }

    /**
     * Constructor for exception due to failure accessing one or more MPEG
     * elementary streams The caller of this constructor is responsible for
     * ensuring the three arrays provided as parameters are the same size. The
     * implementation is not expected to check this.
     * 
     * @param e
     *            the elementary streams which could not be accessed
     * @param major_reason
     *            the major reason why the exception was thrown for each
     *            elementary stream
     * @param minor_reason
     *            the minor reason why the exception was thrown for each
     *            elementary stream
     */
    public NotAuthorizedMediaException(org.davic.mpeg.ElementaryStream[] e, int major_reason[], int minor_reason[])
    {
    }

    /**
     * Constructor for exception due to failure accessing an MPEG service
     * 
     * @param s
     *            the service which could not be accessed
     * @param major_reason
     *            the major reason why the service could not be accessed
     * @param minor_reason
     *            the minor reason why the service could not be accessed
     * @since MHP 1.0.2
     */
    public NotAuthorizedMediaException(org.davic.mpeg.Service s, int major_reason, int minor_reason)
    {
    }

    /**
     * @return SERVICE or ELEMENTARY_STREAM to indicate that either a service
     *         (MPEG program) or one or more elementary streams could not be
     *         descrambled. Implements method from
     *         org.davic.mpeg.NotAuthorizedInterface.
     */
    public int getType()
    {
        return 0;
    }

    /**
     * If getType() returns SERVICE, then this method returns the Service that
     * could not be descrambled. Otherwise it returns null. Implements method
     * from org.davic.mpeg.NotAuthorizedInterface.
     * 
     * @return either the Service that could not be descrambled or null
     */
    public org.davic.mpeg.Service getService()
    {
        return null;
    }

    /**
     * If getType() returns ELEMENTARY_STREAM, then this method returns the set
     * of ElementaryStreams that could not be descrambled. Otherwise it returns
     * null. Implements method from org.davic.mpeg.NotAuthorizedInterface.
     * 
     * @return either the set of ElementaryStreams that could not be descrambled
     *         or null
     */
    public org.davic.mpeg.ElementaryStream[] getElementaryStreams()
    {
        return null;
    }

    /**
     * Returns the reason(s) why descrambling was not possible. Implements
     * method from org.davic.mpeg.NotAuthorizedInterface.
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
     * @see org.davic.mpeg.NotAuthorizedInterface#getElementaryStreams
     */
    public int[] getReason(int index) throws java.lang.IndexOutOfBoundsException
    {
        return null;
    }
}
