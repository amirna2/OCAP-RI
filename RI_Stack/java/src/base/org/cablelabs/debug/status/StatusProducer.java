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

/**
 * An interface for implementation by components of the stack that provide
 * operational status information.
 * 
 */
public interface StatusProducer
{

    /**
     * Method to acquire the specified operational status information.
     * 
     * @param type
     *            identifier for the requested status information.
     * @param format
     *            is the information format identifier (e.g. STRING or BEAN).
     * @param params
     *            is an object specific to the producer and consumer that can
     *            carry any additional information the producer may require to
     *            generate or deliver the information the caller is interested
     *            in.
     * 
     * @return Object containing the requested status information or null if the
     *         type is unknown or the information is not available.
     */
    public Object getStatus(String type, int format, Object params);

    /**
     * Acquire all of the status information types provided by the producer.
     * 
     * @return String array of hierarchical status types.
     */
    public String[] getStatusTypes();

    /**
     * Register a listener for the specified status type. Registered status
     * listeners will receive active status information in the same way that
     * status inforamtion would be delivered upon request using
     * <code>getStatus</code>. Only a single <code>StatusListener</code> may be
     * registered for a particular status type.
     * 
     * @param sl
     *            is the <code>StatusListener</code> to register.
     * @parma types is the information type to deliver.
     * @param formats
     *            is the format associated with each type.
     * @param param
     *            is an object containing any parameters specific to the
     *            associated inforamtion type (e.g. additional information
     *            indicating the nature of the status desired)
     * 
     * @return false if this producer does not support active delivery of status
     *         information to status listeners.
     */
    public boolean registerStatusListener(StatusListener sl, String type, int format, Object param)
            throws IllegalArgumentException;

    /**
     * Register a listener for the specified status types. Registered status
     * listeners will receive active status information in the same way that
     * status inforamtion would be delivered upon request using
     * <code>getStatus</code>. Only a single <code>StatusListener</code> may be
     * registered for a particular status type.
     * 
     * @param sl
     *            is the <code>StatusListener</code> to register.
     * @parma types is an array of the information types to deliver.
     * @param formats
     *            is an array of the formats associated with each type.
     * @param params
     *            is an array of objects containing any parameters specific to
     *            the associated inforamtion type (e.g. additional information
     *            indicating the nature of the status desired)
     * 
     * @return false if this producer does not support active delivery of status
     *         information to status listeners.
     */
    public boolean registerStatusListener(StatusListener sl, String[] types, int[] formats, Object[] params)
            throws IllegalArgumentException;

    /**
     * Unregister the specific <code>StatusListener</code> from receiving any
     * further active status information.
     * 
     * @param sl
     *            is the <code>StatusListener</code> to unregister.
     */
    public void unregisterStatusListener(StatusListener sl);

    /*
     * Valid "format" identifiers.
     */
    public static final int FORMAT_ANY = 0;

    public static final int FORMAT_INT = 1;

    public static final int FORMAT_STRING = 2;

    public static final int FORMAT_BEAN = 3;
}
