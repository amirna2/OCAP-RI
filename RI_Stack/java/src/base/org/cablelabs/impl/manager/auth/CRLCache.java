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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.persistent.PersistentDataSerializer;
import org.cablelabs.impl.util.MPEEnv;

/**
 * This class is used to manage the set of CRL files known to the host. Upon
 * instantiation by the <code>AuthManager</code> it will acquire the stored CRL
 * files, which will be stored as <code>PersistentData</code>. The contents of
 * these files will be cached in <code>CRLFile</code> instances so that they can
 * be maintained when updates occur. The serial numbers from each of the CRL
 * files will maintained within this class with a <code>Hashtable</code> so that
 * revocation checks can be optimized. Updates to the CRL cache will be made by
 * a background thread in the ??? as they appear on newly mounted file systems.
 */
class CRLCache extends PersistentDataSerializer
{
    /**
     * Creates an instance of <code>CRLCache</code>.
     */
    CRLCache()
    {
        // Call PersistentDataSerializer constructor.
        super(getCRLDir(), "ocap.crl.");

        // Read system's CRL cache.
        getSystemCRL();
    }

    /**
     * Update the system's CRL cache with the new certificates contained in the
     * specified CRL file.
     * 
     * @param CRLPath
     *            is the path to the CRL file containing the newly revoked
     *            certificates.
     * @param fs
     *            is the file system to use to retrieve the file.
     * @param roots
     *            is the current known set of root certificates.
     * 
     * @return true if the set of root certificates was affected by the CRL
     *         file.
     */
    boolean update(String CRLPath, FileSys fs, X509CRL crl, HashSet roots)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Adding CRL file '" + CRLPath + "' to CRL cache.");
        }

        // First remove the old CRLFile is this is a replacement.
        CRLFile old;
        if ((old = getCRLFile(crl)) != null)
            removeCRLFile(old); // Remove the
                                                                 // old CRLFile
                                                                 // from system.

        // Add the new CRL to the cached CRLs.
        CRLFile newcrlf = new CRLFile(crl, super.nextUniqueId());
        addCRLFile(newcrlf); // Add contents to cache.

        // Save it to persistent storage.
        try
        {
            super.save(newcrlf);
        }
        catch (IOException ioe)
        { /* Ignore errors... */
        }

        // Now determine if the new CRL file revokes any certificates of any CA
        // CA that may have previously issued a CRL in which case we have to
        // revoke
        // its certificate(s) and remove its CRL file (MHP 12.9.1.9).
        if (roots != null)
        {
            boolean rootsUpdated = false;
            Iterator rts = roots.iterator();

            while (rts.hasNext())
            {
                // See if root is in the list.
                X509Certificate rcert = (X509Certificate) rts.next();
                if (newcrlf.contains(rcert.getSerialNumber()))
                {
                    // Check all CRLs for any issued by revoked CA.
                    for (int i = 0; i < crls.size(); ++i)
                    {
                        CRLFile crlf = (CRLFile) crls.get(i);
                        if ((crlf.getIssuer().compareTo(rcert.getIssuerDN().getName()) == 0))
                        {
                            // Remove revoked certificate.
                            AuthManagerImpl mgr = (AuthManagerImpl) ManagerManager.getInstance(AuthManager.class);
                            mgr.revokeRoot(rcert);
                            removeCRLFile(crlf);// Removed the CRL file from
                                                // persistent storage.
                            rootsUpdated = true;
                        }
                    }
                }
            }
            return rootsUpdated; // Indicate if roots were updated.
        }
        return false; // No update of roots.
    }

    /**
     * Determine if the specified CRL is newer than a previously issued CRL.
     * 
     * @param crl
     *            is the new CRL.
     * 
     * @return true if is is newer than a previous one, false otherwise.
     */
    boolean isNewCRL(X509CRL crl)
    {
        CRLFile crlf = null;

        // Search the existing set of CRL files to see if this is
        // a newer version (MHP 12.9.1.9).
        if ((crlf = getCRLFile(crl)) == null) return true;

        // Compare issue dates.
        return (crl.getThisUpdate().after(crlf.getThisUpdate()));
    }

    /**
     * Locate an existing <code>CRLFile</code> that was issued by the same
     * authority. Note this is not the CRL file as delivered by an attached file
     * system, rather it's an internally cached form of a previously acquired
     * file.
     * 
     * @param crl
     *            the <code>X509CRL</code> of the target issuer.
     * 
     * @return <code>CRLFile</code> for the same issuer, null otherwise.
     */
    private CRLFile getCRLFile(X509CRL crl)
    {
        synchronized (crls)
        {
            // Iterate through all current CRLFIles.
            for (int i = 0; i < crls.size(); ++i)
            {
                // Check for matching issuers.
                CRLFile crlf = (CRLFile) crls.get(i);
                if ((crlf.getIssuer().compareTo(crl.getIssuerDN().getName()) == 0)) return crlf; // Found
                                                                                                 // it,
                                                                                                 // return
                                                                                                 // it.
            }
        }
        return null; // Not found!
    }

    /**
     * Get the location of the persistent storage for CRL files.
     * 
     * @return <code>File</code> instance specifying the persistent storage
     *         location.
     */
    private static File getCRLDir()
    {
        return (new File(MPEEnv.getEnv("OCAP.persistent.crlstor")));
    }

    /**
     * Retrieve the system's current set of revoked certificates from persistent
     * storage.
     */
    void getSystemCRL()
    {
        // Load CRL files from persistent storage.
        Enumeration crlFiles = super.load().elements();

        // Enumerate through entries adding them to the cache.
        while (crlFiles.hasMoreElements())
            addCRLFile((CRLFile) crlFiles.nextElement());
    }

    /**
     * Add the specified CRL file to persistent storage and add it's serial
     * number entries to the revoked certificate cached.
     * 
     */
    void addCRLFile(CRLFile crlf)
    {
        if (log.isDebugEnabled())
        {
            log.debug("adding CRL file from issuer: " + crlf.getIssuer());
        }

        synchronized (crls)
        {
            crls.add(crlf); // Add file to cached set.
            updateRevokedCache(crlf); // Add serial numbers to revoked cache.
        }
    }

    /**
     * Remove the specified CRL file from persistent storage.
     * 
     * @param clrf
     *            the CRLFIle class associated with the target file.
     */
    void removeCRLFile(CRLFile crlf)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removing CRL file from issuer: " + crlf.getIssuer());
        }

        synchronized (crls)
        {
            crls.remove(crlf); // Remove the CRLFile from cached set.
            super.delete(crlf); // Remove the CRLFile from persistent storage.
        }
    }

    /**
     * Update the intenal cache of revoked certificates.
     * 
     * @param crlf
     *            the CRLFile with revoked certs.
     * 
     */
    void updateRevokedCache(CRLFile crlf)
    {
        if (log.isDebugEnabled())
        {
            log.debug("updating set of revoke certificates from issuer: " + crlf.getIssuer());
        }

        // Get an enumeration of all of the CRL file's serial numbers.
        Enumeration serials = crlf.getSerialNumbers().elements();

        synchronized (revoked)
        {
            // Add all of the serial numbers to the revoked hashtable.
            while (serials.hasMoreElements())
            {
                BigInteger serial = (BigInteger) serials.nextElement();
                revoked.put(serial.toString(), serial);
            }
        }
    }

    /**
     * Determine if the specified certificate has been revoked.
     * 
     * @param cert
     *            the certificate in question.
     * 
     * @return boolean indicating whether the certificate has been revoked.
     */
    boolean isRevoked(X509Certificate cert)
    {
        synchronized (revoked)
        {
            return revoked.contains(cert.getSerialNumber().toString());
        }
    }

    // Vector of all of the CRLFile objects representing the know CRL files.
    private Vector crls = new Vector();

    // Hashtable of all serial numbers from CRL files.
    private Hashtable revoked = new Hashtable();

    private static final Logger log = Logger.getLogger(CRLCache.class.getName());
}
