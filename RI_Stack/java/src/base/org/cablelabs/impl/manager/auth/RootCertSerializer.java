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

import java.security.cert.*;
import org.cablelabs.impl.persistent.*;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;
import java.util.HashSet;
import java.util.Vector;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import org.apache.log4j.Logger;

/**
 * This class is used by the <code>AuthManager</code> to acquire and save the
 * set of root certificates known to the system. Normally, the set of root
 * certificates will be installed on the platform prior to distribution. Updates
 * to the set of root certificates may occur via Common Download, used to add
 * new certificates, and CRL files to remove root certificates.
 * 
 * @author afhoffman
 */
class RootCertSerializer extends PersistentDataSerializer
{
    RootCertSerializer()
    {
        // Call PersistentDataSerializer constructor.
        super(getRootCertDir(), "ocap.roots.");

        // Read system's CRL cache.
        acquireSystemRoots();
    }

    /**
     * Get a reference to the system's set of root certificates. This method is
     * used by the <code>AuthManager</code> to get a reference to the root
     * certificates of the platform.
     * 
     * @return <code>HashSet</code> containing the root certificates.
     */
    HashSet getRootCerts()
    {
        return rootCerts;
    }

    /**
     * Save the new set of root certificates to persistent storage.
     * 
     * @param newRoots
     *            is the new set of root certificates.
     */
    void saveRootCerts(HashSet newRoots)
    {
        try
        {
            super.save(new RootCertStorage(newRoots, uniqueId));
        }
        catch (IOException ioe)
        {
            SystemEventUtil.logRecoverableError(ioe);
        }
    }

    /**
     * Retrieve the system's current set of revoked certificates from persistent
     * storage.
     */
    private void acquireSystemRoots()
    {
        Vector roots = super.load();

        // Attempt to load the serialized roots in persistent storage.
        if ((roots == null) || roots.isEmpty())
        {
            // Get them from native, and store them in persistent storage.
            if ((rootCerts = loadRootCerts()) != null) saveRootCerts(rootCerts);
        }

        // Load the <code>HashSet</code> of root certificates.
        Vector v = super.load();
        if (v.isEmpty() == false) rootCerts = ((RootCertStorage) v.firstElement()).getRoots();
    }

    /**
     * Acquire the root certificates from the native platform and generate.
     * 
     * @return
     */
    private HashSet loadRootCerts()
    {
        CertificateFactory cf;
        HashSet rootCerts = null;
        HashSet certs = new HashSet(1);
        byte[] rawCerts;

        // Attempt to load raw byte stream containing root certificates.
        if ((rawCerts = nativeLoadRootCerts()) == null) return null;

        ByteArrayInputStream bais = new ByteArrayInputStream(rawCerts);
        try
        {
            cf = CertificateFactory.getInstance("X.509");
        }
        catch (CertificateException e)
        {
            return null;
        }

        // Acquired raw byte array containing root certs, now generate
        // certificates.
        while (bais.available() > 0)
        {
            try
            {
                certs.add(cf.generateCertificate(bais));
            }
            catch (CertificateException ce)
            {
                break;
            }
            rootCerts = certs;
        }
        return rootCerts; // Return what was found so far.
    }

    private HashSet loadDefaultRootCerts()
    {

        HashSet certs = new HashSet();

        String[] rootCertArr = {
                "-----BEGIN CERTIFICATE-----\n"
                        + // CableLabs production root cert
                        "MIIDTjCCAjagAwIBAgIQKa7zLoyi+URuXAmfQkHQojANBgkqhkiG9w0BAQUFADBBMQswCQYDVQQG\n"
                        + "EwJVUzESMBAGA1UEChMJQ2FibGVMYWJzMR4wHAYDVQQDExVDYWJsZUxhYnMgQ1ZDIFJvb3QgQ0Ew\n"
                        + "HhcNMDIwNDI5MDAwMDAwWhcNMzIwNDI4MjM1OTU5WjBBMQswCQYDVQQGEwJVUzESMBAGA1UEChMJ\n"
                        + "Q2FibGVMYWJzMR4wHAYDVQQDExVDYWJsZUxhYnMgQ1ZDIFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEB\n"
                        + "AQUAA4IBDwAwggEKAoIBAQC6vRAD5cBUk6T+an331jVLNTUbpqpeV0hzk8+yDgFeDGSCiSG3csAj\n"
                        + "GxhiGdNe/zq63kEFDfxlEu/GW8te5gfDCRYdaeMdV/XlLxHQfeHX+6PBtIYBJot62J0YWtah5odH\n"
                        + "p1ZsI2eeRimQIJt4U5LtYR7+ks+fcIh2Gnx0DvhcbMMprNwG648r6jzV2tD5ZNVCJ2x8+lrflu1g\n"
                        + "AIlifmg2KYwyS9NBeb5sInkLklcPIiz89ysGx9kGRpCGNQpfG8ymExY+GQwMa1NvGc85eu7YPrJM\n"
                        + "83JY3+MXzx6zQY7Buq77tcG/0c5KjtB89Imeu6I183EhpPxtijMdifbgFFCfAgMBAAGjQjBAMA8G\n"
                        + "A1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBS9WfcIxb/ae21HLEEs1aTm\n"
                        + "elWdCTANBgkqhkiG9w0BAQUFAAOCAQEAV2SyaOfCiTsLxYZh7iIM8pJUeetSllCAMXsGAYGjgbGC\n"
                        + "F4Gcvq7eP3vrmhPHLnVmM6hDLQUO0q19zfzMtda7dHZrGL00lbvWDfSgnNTYlDQj5XSJs7YieQnJ\n"
                        + "8Yq1aRmYzoIG6aRXOahVpYnOlb6CcP9mIxiNuge3qCtHj+o4ajGiNusi8jEcdzi4imWj3Yh30030\n"
                        + "QVmdWZFqIJPTy60bj3OzV8uByBjnm2hKUDkMQ/CzTaOGzltXD/hbYfunalZ+ePMLju+XkC/9YJx8\n"
                        + "Ca+XDAnpYL9mAazTT0tMP7lSIaWJa6mcIr8DSbB74IaDJV+B9lgbkf/s1cSg/VSqGnngpg==\n"
                        + "-----END CERTIFICATE-----\n",
                "-----BEGIN CERTIFICATE-----\n"
                        + // CableLabs test root cert
                        "MIIDSTCCAjGgAwIBAgIBADANBgkqhkiG9w0BAQUFADBGMQswCQYDVQQGEwJVUzES\n"
                        + "MBAGA1UEChMJQ2FibGVMYWJzMSMwIQYDVQQDExpURVNUIENhYmxlTGFicyBDVkMg\n"
                        + "Um9vdCBDQTAeFw0wNDA4MTMxNzA4MjVaFw0zNDA4MTMxNzA4MjVaMEYxCzAJBgNV\n"
                        + "BAYTAlVTMRIwEAYDVQQKEwlDYWJsZUxhYnMxIzAhBgNVBAMTGlRFU1QgQ2FibGVM\n"
                        + "YWJzIENWQyBSb290IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n"
                        + "xra38lnGERXvYYgBA5Cx7PwsKOv7a13mC88D/OKL6noxJJZn/umBPsv9Zmc4e0nB\n"
                        + "J8/GwAnmvPYTQ7ap9W9BeQ3AXZ+VmsMuHKQ0DbUBF/M3YwBI6YepbnwsIoa/b3WJ\n"
                        + "/U2yyc7egVWoWqz6cc/+uq/j1QH+qht32nVp9fT8XM3bC51UIkiklMClhnOLiFaq\n"
                        + "Z6A/UdA8XLuqdGilzxfMT/F0DpAr871SZSKCrtROqzKC9W01zsgcCFJnyJ1Ne9jv\n"
                        + "TeMYa3hckc5eR7GIJW/kxC5TZ5LQEsLeZzHusrb7+zV890gmDOZnyqnl24OL2fHW\n"
                        + "1eav0OFksgWO2de8n+hmGwIDAQABo0IwQDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0T\n"
                        + "AQH/BAUwAwEB/zAdBgNVHQ4EFgQUdjD6dfT0ujNSby7/ln8CyL2nB3gwDQYJKoZI\n"
                        + "hvcNAQEFBQADggEBAKyM294U/bBx1UNFZLJcoC4d+31vsXwOlDBYUEGQferYIr3I\n"
                        + "LP6ursqjvAT9k8ZkJeD76Rm+lf+dycMfT04g1xmFBHk+GZTZjF+4me+BaZPRDusT\n"
                        + "pPqylqolmJohwVIkECOxJrcV5sB2vEfs03+HaVeBHvRHcuofGY5dpsw/OtywfDmk\n"
                        + "LwqlfCQNComR+iY0kzG8CtsufzJla/o0H9MDieO/U/u3Cwe1MhLRKyIJEbzwZRFg\n"
                        + "szB1wWZXCFtUBrrS6Iv1ZhOFOy5xUjYd/fthnvJ62PTy270z27H1SI5Z7eJ3b7GZ\n"
                        + "lv7sbPN5Oey5ocCsZOT3CuNonXkX6cJBc7ehY9E=\n" + "-----END CERTIFICATE-----\n" };

        CertificateFactory cf = null;
        try
        {
            cf = CertificateFactory.getInstance("X.509");
        }
        catch (CertificateException e)
        {
            if (log.isErrorEnabled())
            {
                log.error(e.getMessage(), e);
            }
            return null;
        }

        for (int i = 0; i < rootCertArr.length; i++)
        {

            byte[] rawCert = rootCertArr[i].getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(rawCert);
            try
            {
                Certificate cert = cf.generateCertificate(bais);
                if (log.isDebugEnabled())
                {
                    log.debug("Root cert [" + i + "]\n" + cert.toString());
                }
                certs.add(cert); // cf.generateCertificate(bais) );
            }
            catch (CertificateException ce)
            {
                if (log.isErrorEnabled())
                {
                    log.error(ce.getMessage(), ce);
                }
            }
        }
        if (certs.size() < 1)
        {
            return null;
        }
        rootCerts = certs;
        return rootCerts;
    }

    /**
     * Get the location of the persistent storage for root certificates
     * 
     * @return <code>File</code> instance specifying the persistent storage
     *         location.
     */
    private static File getRootCertDir()
    {
        return new File(MPEEnv.getEnv("OCAP.persistent.certs"));
    }

    static final long uniqueId = 0L;

    native private byte[] nativeLoadRootCerts();

    private HashSet rootCerts = null;

    private static final Logger log = Logger.getLogger(RootCertSerializer.class.getName());
}
