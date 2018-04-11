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

package org.ocap.diagnostics;

/**
 * This interface represents a response to an implementation request to an
 * application that has registered control over a specific MIB.
 */
public class SNMPResponse
{
    private int m_status;

    private MIBObject m_mibObject;

    /**
     * The request completed successfully and the MIBObject contains valid
     * contents.
     */
    public final static int SNMP_REQUEST_SUCCESS = 0;

    /**
     * The size of the Response-PDU would be too large to transport.
     */
    public final static int SNMP_REQUEST_TOO_BIG = 1;

    /**
     * The OID could not be found or there is no OID to respond to in a get next
     * request.
     */
    public final static int SNMP_REQUEST_NO_SUCH_NAME = 2;

    /**
     * A Check/Set value (or syntax) error occurred.
     */
    public final static int SNMP_REQUEST_BAD_VALUE = 3;

    /**
     * An attempt was made to set a variable that has an access value of
     * Read-Only
     */
    public final static int SNMP_REQUEST_READ_ONLY = 4;

    /**
     * Any error not covered by the other error types.
     */
    public final static int SNMP_REQUEST_GENERIC_ERROR = 5;

    /**
     * Access was denied to the object for security reasons.
     */
    public final static int SNMP_REQUEST_NO_ACCESS = 6;

    /**
     * The object type in the request is incorrect for the object.
     */
    public final static int SNMP_REQUEST_WRONG_TYPE = 7;

    /**
     * A variable binding specifies a length incorrect for the object.
     */
    public final static int SNMP_REQUEST_WRONG_LENGTH = 8;

    /**
     * A variable binding specifies an encoding incorrect for the object.
     */
    public final static int SNMP_REQUEST_WRONG_ENCODING = 9;

    /**
     * The value given in a variable binding is not possible for the object
     */
    public final static int SNMP_REQUEST_WRONG_VALUE = 10;

    /**
     * A specified variable does not exist and cannot be created.
     */
    public final static int SNMP_REQUEST_NO_CREATION = 11;

    /**
     * A variable binding specifies a value that could be held by the variable
     * but cannot be assigned to it at this time. (For example, is not CURRENTLY
     * valid to set because of the value of another MIB object, e.g. one MIB
     * value indicates if a clock display is 12 or 24 hours, and is set to 12,
     * but then someone tries to set the time to 13:00)
     */
    public final static int SNMP_REQUEST_INCONSISTENT_VALUE = 12;

    /**
     * An attempt to set a variable required a resource that is not available.
     */
    public final static int SNMP_REQUEST_RESOURCE_UNAVAILABLE = 13;

    /**
     * An attempt to set a particular variable failed.
     */
    public final static int SNMP_REQUEST_COMMIT_FAILED = 14;

    /**
     * An attempt to set a particular variable as part of a group of variables
     * failed, and the attempt to then undo the setting of other variables was
     * not successful.
     */
    public final static int SNMP_REQUEST_UNDO_FAILED = 15;

    /**
     * A problem occurred in authorization.
     */
    public final static int SNMP_REQUEST_AUTHORIZATION_ERROR = 16;

    /**
     * The variable does not exist; the agent cannot create it because the named
     * object instance is inconsistent with the values of other managed objects.
     */
    public final static int SNMP_REQUEST_NOT_WRITABLE = 17;

    /**
     * The variable does not exist; the agent cannot create it because the named
     * object instance is inconsistent with the values of other managed objects.
     */
    public final static int SNMP_REQUEST_INCONSISTENT_NAME = 18;

    /**
     * Constructs an SNMPResponse.
     * 
     * @param status
     *            Status of the corresponding request. Possible values include
     *            any of the SNMP_REQUEST_* constants in this class.
     * @param object
     *            MIBObject resulting from the corresponding request.
     */
    public SNMPResponse(int status, MIBObject object)
    {
        m_status = status;

        m_mibObject = object;
    }

    /**
     * Get the status of the response.
     * 
     * @return One of the request constants defined in this interface.
     */
    public int getStatus()
    {
        return m_status;
    }

    /**
     * Gets the encoding of the MIB object associated with the OID in the
     * request that caused this response.
     * 
     * @return If the getStatus method returns SNMP_REQUEST_SUCCESS this method
     *         SHALL return a populated MIB Object, otherwise the MIB object
     *         returned SHALL contain an empty data array with length 0.
     */
    public MIBObject getMIBObject()
    {
        if (m_status == SNMP_REQUEST_SUCCESS)
            return m_mibObject;
        else
        {
            // create 0 length array
            byte[] ba = new byte[0];
            // create MIB object with OID and 0 array
            MIBObject mo = new MIBObject(m_mibObject.getOID(), ba);
            return mo;
        }
    }
}
