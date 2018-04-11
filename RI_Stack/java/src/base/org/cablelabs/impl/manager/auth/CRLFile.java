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
package org.cablelabs.impl.manager.auth;

import org.cablelabs.impl.persistent.PersistentData;
import java.security.cert.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.math.BigInteger;
import java.util.Date;

/**
 * This class is used to maintain the contents of a single CRL file. It used to
 * help maintain the system's set of CRLs and associated revoked certificates.
 * 
 * @author afhoffman
 * 
 */
class CRLFile extends PersistentData
{

    CRLFile(X509CRL crl, long uniqueId)
    {
        // Call PersistentData constructor.
        super(uniqueId);

        // Get the issuer name.
        issuer = crl.getIssuerDN().getName();

        // Get the update date.
        thisUpdate = crl.getThisUpdate();

        // Get an iterator for the new CRL entries.
        Iterator iterator = crl.getRevokedCertificates().iterator();

        // Iterator through the CRL & adding non-duplicates to the system's
        // cache.
        while (iterator.hasNext())
        {
            X509CRLEntry next = (X509CRLEntry) iterator.next(); // Get next
                                                                // entry.
            BigInteger serial = next.getSerialNumber(); // Get serial number.
            serials.add(serial); // Add serial number.
        }
    }

    /**
     * Accessor method for the issuer of the CRL file.
     * 
     * @return String representation of the issuer.
     */
    String getIssuer()
    {
        return issuer;
    }

    /**
     * Accessor method for the "thisUpdate" date of the CRL file.
     * 
     * @return Date of the CRL file.
     */
    Date getThisUpdate()
    {
        return thisUpdate;
    }

    /**
     * Get the set of certificate serial number revoked by this CRL.
     * 
     * @return Vector of serial numbers.
     */
    Vector getSerialNumbers()
    {
        return serials;
    }

    /**
     * Determine if the specified serial number is contained within this CRL
     * file.
     * 
     * @param serial
     *            is the serial number in question.
     * 
     * @return boolean indicating whether this CRL file contains the specified
     *         serial number.
     */
    boolean contains(BigInteger serial)
    {
        // Get an enumeration of all of the serial numbers.
        Enumeration serials = this.serials.elements();

        // Compare the specified serial number again each serial number.
        while (serials.hasMoreElements())
        {
            if (serial.compareTo((BigInteger) serials.nextElement()) == 0) return true; // Found
                                                                                        // it.
        }
        return false; // Not found.
    }

    // CRL Issuer.
    String issuer;

    // CRL update date.
    Date thisUpdate;

    // CRL serial numbers.
    Vector serials = new Vector();
}
