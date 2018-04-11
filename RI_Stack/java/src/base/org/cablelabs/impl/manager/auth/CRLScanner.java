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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.util.Enumeration;
import java.util.Vector;
import java.util.HashSet;

import java.security.cert.X509CRL;
import java.security.cert.CertificateFactory;

import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimerManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This internal implementation class is responsible for periodically scanning
 * mounted file systems for CRL files. Each <code>ServiceDomain</code> that gets
 * attached will register their mount point with this class so that it can
 * periodically go search for CRL files. Any CRL files that are located are then
 * passed to the authentication manager for processing.
 * 
 * @author afhoffman
 * 
 */
class CRLScanner
{
    protected CRLScanner()
    {
        mounts = new Vector();
        sysCtx = ccm.getSystemContext(); // Get system's caller context.

        // Create a timer specification for periodic scanning for CRL files.
        scanner = new TVTimerSpec();
        scanner.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                doCRLScan(); // Scan for CRL files.
            }
        });
        // Set delay time for scanner (timer setup, but not started).
        scanner.setDelayTime(SCAN_DELAY);
    }

    /**
     * Returns the singleton instance of the <code>CRLScanner</code>.
     */
    static synchronized CRLScanner getInstance()
    {
        if (singleton == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating CRLScanner singleton");
            }
            singleton = new CRLScanner();
        }

        if (log.isDebugEnabled())
        {
            log.debug("Returning the CRLScanner instance");
        }

        return singleton;
    }

    /**
     * Register a <code>ServiceDomain</code> mount point for CRL file scanning.
     * 
     * @param mount
     *            is the mount point to register.
     */
    void registerMount(String mount, FileSys fs, CRLCache crlCache, RootCertSerializer rootSerializer, HashSet rootCerts)
    {
        if (log.isDebugEnabled())
        {
            log.debug("registering mount point " + mount + " for CRL file scanning.");
        }

        synchronized (mounts)
        {
            // Get the mount point.
            MountPoint mpt = getMountPoint(mount, fs);

            // Increment the reference count.
            mpt.increment();

            // Add the mount point to the set of mounts.
            mounts.add(mpt);

            // If scanner isn't active, start it up.
            if (scanning == false)
            {
                this.crlCache = crlCache; // Set the runtime CRL cache.
                this.rootSerializer = rootSerializer; // Set root cert
                                                      // serializer.
                this.rootCerts = rootCerts; // Set root certificates.
                startScanner(); // Begin scan (updates scanning flag).
            }
        }
    }

    /**
     * Unregister a <code>ServiceDomain</code> mount point for CRL file
     * scanning.
     * 
     * @param mount
     */
    void unregisterMount(String mount)
    {
        if (log.isDebugEnabled())
        {
            log.debug("unregistering mount point " + mount + " for CRL file scanning.");
        }

        synchronized (mounts)
        {
            Enumeration mounts = this.mounts.elements();
            MountPoint mpt;

            // Search mount points for target mount point.
            while (mounts.hasMoreElements())
            {
                mpt = (MountPoint) mounts.nextElement();
                if (mpt.isSame(mount))
                {
                    // If the reference count is zero, remove it.
                    if (mpt.decrement() == 0) this.mounts.remove(mpt); // Remove
                                                                       // it
                                                                       // from
                                                                       // scan
                                                                       // list.
                }
            }
            // If no mount points registered, stop the scanner.
            if (this.mounts.size() == 0) stopScanner(); // Stop scanning
                                                        // (updates scanning
                                                        // flag).
        }
    }

    /**
     * Get an existing mount point or instantiate a new one if the target mount
     * point does not exist in the CRL file scan list.
     * 
     * @param mount
     *            is the target mount point.
     * 
     * @return <code>MountPoint</code> for the existing a new mount point.
     */
    private MountPoint getMountPoint(String mount, FileSys fs)
    {
        Enumeration mounts = this.mounts.elements();
        MountPoint mpt;

        // Search through existing mount points.
        while (mounts.hasMoreElements())
        {
            mpt = (MountPoint) mounts.nextElement();
            if (mpt.isSame(mount)) return mpt; // Found it.
        }

        // Didn't find the mount point, instantiate a new one.
        return new MountPoint(mount, fs);
    }

    /**
     * Start the timer to run the CRL file scanner.
     */
    private void startScanner()
    {
        if (log.isDebugEnabled())
        {
            log.debug("starting CRL file scanner.");
        }

        // Install the timer.
        try
        {
            scanner = tm.getTimer(sysCtx).scheduleTimerSpec(scanner);
            scanning = true;
        }
        catch (TVTimerScheduleFailedException e)
        {
                SystemEventUtil.logRecoverableError(new Exception("Failed to set timer for CRL scan."));
            }
        }

    /**
     * Stop the timer to run the CRL file scanner.
     */
    void stopScanner()
    {
        if (log.isDebugEnabled())
        {
            log.debug("stopping CRL file scanner.");
        }

        // Remove the timer.
        tm.getTimer(sysCtx).deschedule(scanner);
        scanning = false;
    }

    /**
     * CRL file scanner method, which iterates through each mount point looking
     * for CRL files.
     * 
     * Note scanning for "dvb" signed files had been removed.
     */
    private void doCRLScan()
    {
        synchronized (mounts)
        {
            // Check for mounts to scan.
            if (mounts.size() > 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("about to scan all registered mount points...");
                }

                // Scan each registered mount.
                Enumeration mpts = mounts.elements();
                while (mpts.hasMoreElements())
                {
                    MountPoint mpt = (MountPoint) mpts.nextElement();

                    if (log.isDebugEnabled())
                    {
                        log.debug("about to scan mount point: " + mpt.mount);
                    }

                    mpt.scan("ocap"); // Scan for ocap CRL files.
                }
            }
        }
    }

    /**
     * Internal class used to keep track of individual mount points and provide
     * the actual CRL file scanning.
     * 
     * Note support for scanning for "dvb" signing has been removed.
     */
    private class MountPoint
    {
        MountPoint(String mount, FileSys fs)
        {
            if (log.isDebugEnabled())
            {
                log.debug("MountPoint installed for: " + mount);
            }

            this.mount = mount; // Set mount point.
            this.fs = fs; // Set file system.
            this.refCount = 0; // Initialize reference count.
            filter = new FilenameFilter()
            {
                public boolean accept(File dir, String f)
                {
                    try
                    {
                        if (f.startsWith("ocap.crl.") && Integer.parseInt(f.substring(f.lastIndexOf(".") + 1)) > 0)
                            return true;
                    }
                    catch (NumberFormatException e)
                    {
                        return false;
                    }
                    return false;
                }
            };
        }

        /**
         * Scan for the specified CRL file type (i.e. "ocap" or "dvb"). If a CRL
         * file is found it is passed to the <code>AuthManager</code> for
         * processing.
         * 
         * @param type
         *            is a string representing the file type to scan for.
         */
        void scan(String type)
        {
            String path = mount + "/" + type + ".crl";

            if (log.isDebugEnabled())
            {
                log.debug("attempting to scan for CRL files in directory: " + path);
            }

            // List the directory filtering for CRL files.
            String[] crls = (new File(path).list(filter));
            if (crls != null && crls.length > 0)
                for (int i = 0; i < crls.length; ++i)
                updateCRL(path + "/" + crls[i], fs); // Inform the
                                                     // authentication manager.
        }

        /**
         * Update the system's CRL cache with the contents of the specified CRL
         * file. The CRL files that must be supported include:
         * 
         * <ul>
         * <li>ocap.crl.x - for OCAP CRLs authenticated by broadcast certs.
         * <li>ocap.crl.root.x - for OCAP CRLs authentication by a root cer.
         * <li>dvb.crl.x - for DVB CRLs authenticated by broadcast certs.
         * <li>dvb.crl.root.x - for DVB CRLs authenticated by a root cert.
         * </ul>
         * 
         * The x portion of the file name is a number used as a discriminator to
         * ensure non-collision of CRL file names in the event that there is
         * more than one in a directory.
         * 
         * @param path
         *            is the path name to the target CRL file.
         */
        private synchronized void updateCRL(String path, FileSys fs)
        {
            CertificateFile cf;
            String crlName;
            byte[] file;

            if (crlCache == null || rootSerializer == null) return; // Do
                                                                    // nothing
                                                                    // if
                                                                    // resources
                                                                    // not
                                                                    // available.

            // First, read the entire contents of the CRL file.
            byte[] crlFile = null;
            try
            {
                crlFile = fs.getFileData(path).getByteData();
            }
            catch (Exception ioe)
            {
                return; // Do nothing if file inaccessible.
            }

            // Get a CertificateFactory and generate a CRL from the file
            // contents.
            X509CRL crl;
            try
            {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                crl = (X509CRL) factory.generateCRL(new ByteArrayInputStream(crlFile));
            }
            catch (Exception e)
            {
                return; // Can't construct X509CRL, ignore it.
            }

            // Now, check to see if it's a new or replacement CRL (MHP
            // 12.9.1.9).
            if (crlCache.isNewCRL(crl) == false) return; // Not a new one,
                                                         // ignore it.

            // It's a new CRL, now attempt to locate its associated certificate
            // file
            // and verify that the CRL is signed by a trusted certificate (MHP
            // 12.9.1.9).
            try
            {
                String certPath = path.substring(0, path.lastIndexOf("/")); // Get
                                                                            // path
                                                                            // to
                                                                            // file.
                crlName = path.substring(path.lastIndexOf("/")) + 1; // Get file
                                                                     // name.

                // Now formulate the certificate file name, e.g.
                // ocap.certificate.x
                String certName = crlName.substring(0, crlName.indexOf(".")) + ".certificates."
                        + path.substring(path.lastIndexOf(".") + 1); // Get
                                                                     // number,
                                                                     // "x"

                // First look for certificate file in the same directory.
                try
                {
                    file = fs.getFileData(certPath + "/" + certName).getByteData();
                }
                catch (Exception e)
                {
                    // Back path up one level and look for cert file.
                    certPath = certPath.substring(0, certPath.lastIndexOf("/"));
                    try
                    {
                        file = fs.getFileData(certPath + "/" + certName).getByteData();
                    }
                    catch (Exception e2)
                    {
                        return;
                    }
                }
                // Instantiate a certificate file (i.e. parse it).
                cf = new CertificateFile(file, am);
            }
            catch (Exception e)
            {
                return; // Couldn't process the file, ignore it.
            }

            // If it's a root clr file, check for a valid root certificate.
            if ((crlName.indexOf(".root.") != (-1)) && am.isValidRoot(cf.getRootCert()) == false) return; // ".root."
                                                                                                          // CRL
                                                                                                          // file,
                                                                                                          // but
                                                                                                          // not
                                                                                                          // signed
                                                                                                          // by
                                                                                                          // a
                                                                                                          // root.

            // Now, verify the CRL file is signed.
            if (cf.verifyCRLSignature(crl) == false) return; // Not properly
                                                             // signed, ignore
                                                             // it.

            // Update the CRL cache. If it updated root certificate status too,
            // then update the set of root certificates in persistent storage.
            if (crlCache.update(path, fs, crl, rootCerts)) rootSerializer.saveRootCerts(rootCerts);
        }

        /**
         * Increment the reference count for this mount point.
         */
        void increment()
        {
            ++refCount;
        }

        /**
         * Decrement the reference count for this mount point.
         * 
         * @return the new value of the reference count.
         */
        int decrement()
        {
            return --refCount;
        }

        /**
         * Determine if two mount points are the same.
         * 
         * @param mount
         *            is the targe mount point.
         * 
         * @return true if the mount points match.
         */
        boolean isSame(String mount)
        {
            return (mount.compareTo(this.mount) == 0);
        }

        String mount; // Mount point of service domain.

        int refCount; // Reference count for multiply mounted locations.

        FileSys fs; // File system used for access.
    }

    // Default scan every 5 minutes.
    static final long SCAN_DELAY = MPEEnv.getEnv("OCAP.auth.crlScanDelay", 1000 * 5L);

    private static CRLScanner singleton = null; // CRL scanner instance
                                                // singleton.

    private TVTimerSpec scanner; // Time used for periodic scanning of
                                 // registered mounts.

    private boolean scanning = false; // Indicates timer is set for scanning.

    private FilenameFilter filter; // Filter for CRL files.

    private Vector mounts; // Registered mounts.

    private CallerContext sysCtx; // Scanner run in system context.

    private CRLCache crlCache; // The current runtime CRL cache.

    // Root certificate serializer used to aquire the system's root certs.
    private RootCertSerializer rootSerializer;

    // Set of root certificates for the platform.
    private HashSet rootCerts;

    // Managers used by CRL scanner.
    // TODO: add when auth manager gets officially added to mgrmgr list.
    private AuthManagerImpl am = (AuthManagerImpl) AuthManagerImpl.getInstance();

    // private AuthManagerImpl am =
    // (AuthManagerImpl)ManagerManager.getInstance(AuthManager.class);
    private TimerManager tm = (TimerManager) ManagerManager.getInstance(TimerManager.class);

    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static final Logger log = Logger.getLogger(CRLScanner.class.getName());
}
