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


package org.cablelabs.impl.snmp;

import org.ocap.diagnostics.MIBObject;

/**
 * The interface that a MIB management implementation will adhere to in order to allow the 
 * AgentX library to interact with the MIB without any need to know the inner workings of 
 * the MIB implementation other than what is exposed here. 
 * 
 * @author Kevin Hendry, Mark Orchard
 */
public interface MIBDelegate 
{
    /**
     * Retrieves OID data from the MIB.
     *  
     * @param oid The value of the Object ID where the data can be found.
     * @return a single Varbind Object which can be found in the MIB at the requested OID.
     */
    MIBObject get(String oid);
    
    /**
     * Retrieves the data from the MIB for the next OID in the tree after the OID provided.
     * 
     * @param oid The value of the Object ID which is lexicographically previous to where the data can be found.
     * @return a single Varbind object which represents the OID and Data lexicographically subsequent to 
     *         the OID provided 
     */
    MIBObject getNext(String oid);
    
    /**
     * Determine if the requested OID can be set using the value provided.
     * 
     * @param oid The Object ID to be set.
     * @param data The raw data for the OID formatted in ASN.1.
     * 
     * @return a <code>integer</code> containing the error code for this test.
     */
    int testSet(String oid, byte[] data);
    
    /**
     * Set the OID in the MIB with the value of the provided variable binding.
     * 
     * @param oid The Object ID to be set.
     * @param data The raw data for the OID formatted in ASN.1.
     * 
     * @return true if the data was successfully set for the specified OID, false if any error occurred.
     * @see org.ocap.diagnostics.SNMPResponse
     */
    boolean set(String oid, byte[] data);
    
    /**
     * Retrieve the AgentXTransactionManager to be used to ensure that if multiple operations are done on the MIB
     * such operations will be atomic.
     * 
     * @return an object implementing the AgentXTransactionManager interface.
     */
    MIBTransactionManager getTransactionManager();
}
