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

//import org.cablelabs.impl.io.RawInputStream;
import java.util.Vector;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import java.util.Arrays;

import java.security.cert.CertificateFactory;
import sun.security.util.DerValue;
import sun.security.x509.*;
import javax.security.auth.x500.X500Principal;

import org.cablelabs.impl.util.SystemEventUtil;
import org.apache.log4j.Logger;

import sun.security.x509.PKIXExtensions;

/**
 * This class is used to parse and provide the contents of a certificate file.
 */
class CertificateFile
{
    /**
     * Constructor. Upon construction the certificate file that the raw input
     * stream points to is parsed. If any errors occur during the parsing
     * operation the accessor method will return null values.
     * 
     * The format of the certificate file is defined by MHP as follows:
     * 
     * CertificateFile() { certificate_count 16-bits uimsbf for ( i=0; i <
     * certificate_count; i++ ) { certificate_length 24-bits uimsbf
     * certificate() } }
     * 
     * @param RawInputStream
     *            to the certificate file.
     */
    CertificateFile(byte[] file, AuthManagerImpl authMgr)
    {
        this.authMgr = authMgr;

        // Check for empty certificate file.
        if (file.length == 0) return;

        try
        {
            // Get an X509 certificate factory for generation of X509
            // certificates.
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            int certCnt = ((0x0FF00 & (file[0] << 8)) | (0x0FF & file[1]));
            if (log.isDebugEnabled())
            {
                log.debug("Number of certs: " + certCnt);
            }
            // Make sure there are a useful number of certificates.
            if (certCnt > 1)
            {
                // Get an array of the proper size.
                certs = new X509Certificate[certCnt];

                int offset = 2; // Adjust offset past count.

                // Read & generate the certificates from the file.
                for (int i = 0; i < certCnt; ++i)
                {
                    // Extract the size of the next certificate.
                    int size = ((0x0FF0000 & (file[offset] << 16)) | (0x0FF00 & (file[offset + 1] << 8)) | (0x0FF & file[offset + 2]));

                    offset += 3; // Move past certificate size.

                    // Convert the contents to a byte array input stream for
                    // generation of the cert.
                    ByteArrayInputStream bais = new ByteArrayInputStream(file, offset, size);
                    certs[i] = (X509Certificate) cf.generateCertificate(bais); // Gen
                                                                               // cert.
                    offset += size; // Move past certificate.
                }
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            certs = null;
        }
    }

    /**
     * Verify the signature file's authoriy key identifer matches the subject
     * key identifier of the leaf certificate of this certificate file. The
     * equality determination is made based on the distinct rules between OCAP &
     * GEM.
     * 
     * @param sf
     *            is the signature file associated with the certificate file.
     * @param prefix
     *            is the prefix of the files (i.e. ocap/dvb)
     * 
     * @return true if the
     */
    boolean verifyKeyIdentifiers(SignatureFile sf, String prefix)
    {
        X509Certificate cert = getLeafCertificate();

        if (cert == null) return false;

        // For OCAP signed make sure the leaf certificate SubjectKeyIdentifier
        // matches the signature's AuthorityKeyIdentifier (OCAP 14.2.1.13-14).
        if (prefix.compareTo("ocap") == 0)
        {
            // Get SubjectKeyIdentifier from extension in certificate.
            byte[] sId = getKeyId(cert.getExtensionValue(SUBJ_KEY_ID), SUBJ_KEY_ID);
            if (sId == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Leaf cert SubjectKeyIdentifier is null");
                }
                return false;
            }
            // Get AuthorityKeyIdentifier from extension in signature file.
            byte[] aId = sf.getKeyIdentifier();
            if (aId == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Signature's AuthorityKeyIdentifier is null");
                }
                return false;
            }
            // Check the signature file keyIdentifier field
            // against the cert subject keyIdentifier field.
            if (!Arrays.equals(sId, aId))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Leaf cert KeyIdentifier not matching signature's AuthorityKeyIdentifier");
                }
                return false;
            }
        }
        else
        {
            String dn = sf.getAuthorityCertIssuer();
            if (dn == null || cert.getIssuerDN().toString().compareTo(dn) != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Certificate KeyIdentifier check failed");
                }
                return false;
            }
        }
        return true; // Subject & authority key identifiers match.
    }

    /**
     * Verify the certificate chain is valid, which requires the following: 1.
     * Each certificate must be valid. 2. Each certificate issuer ID must match
     * the subject ID of the next higher certificat in the chain. 3. Each
     * certificate must be verified to be signed using the public key of the
     * next higher certificate in the chain. 4. The Root certificate must be
     * "known", which is actually checked early on during the signature
     * file-leaf certificate validation process.
     * 
     * @param signers
     *            is a vector to used to record the certificate chain or null if
     *            not interested in recording the signers.
     * @param prefix
     *            is the string prefix identifying the type of certificate file
     *            (i.e. ocap or dvb).
     * 
     * @return boolean indicating whether the certificate chain is valid.
     */
    boolean verifyCertificates(Vector signers, String prefix)
    {
        X509Certificate[] certChain = getCertificates();

            for (int i = 0; i < certChain.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("---------------------- cert " + i + " -------------------");
            }
            if (log.isDebugEnabled())
            {
                log.debug(certChain[i].toString());
            }
            if (log.isDebugEnabled())
            {
                log.debug("---------------------- end cert -------------------");
            }
        }

        int i;

        // Sanity check (must be at least two certificates in chain).
        if (certChain.length < 2)
        {
            if (log.isErrorEnabled())
            {
                log.error("Certificate chain is less than two");
            }
            return false;
        }

        // Validate each certificate
        for (i = 0; i < certChain.length; i++)
        {
            boolean isDeviceCert = (i == 0) && (certChain.length > 2);
            boolean isLeaf = (i == 0);
            if (!verifyCertificatePerOCAP((X509CertImpl) certChain[i], isLeaf, certChain.length, i, isDeviceCert))
            {
                if (log.isErrorEnabled())
                {
                    log.error("Certificate #" + i + " failed OCAP verification");
                }

                return false; // Invalid cert
            }

            // check name constraints
            NameConstraintsExtension nameConstraintsExt = ((X509CertImpl) certChain[i]).getNameConstraintsExtension();
            if (nameConstraintsExt != null)
            {
                for (int j = 0; j < i; j++)
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Validating certificate #" + j + " according to NameConstraint");
                        }
                        if (!nameConstraintsExt.verify(certChain[j]))
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("Certificate #" + j + " failed NameConstraint verification");
                            }

                            return false; // Invalid name
                        }
                    }
                    catch (IOException ex)
                    {

                        if (log.isErrorEnabled())
                        {
                            log.error("Certificate #" + i + ": caucght exception checking NameConstraint: "
                                    + ex.getMessage());
                        }

                        return false; // Invalid nameconstraint
                    }
                }
            }

        }

        // Validate each certificate against the next higher certificate.
        for (i = 0; i < certChain.length - 1; ++i)
        {
            // Get SubjectKeyIdentifier from next higher cert &
            // AuthorityKeyIdentifier from current.
            byte[] sId = getKeyId(certChain[i + 1].getExtensionValue(SUBJ_KEY_ID), SUBJ_KEY_ID);
            byte[] aId = getKeyId(certChain[i].getExtensionValue(AUTH_KEY_ID), AUTH_KEY_ID);

            // First, make sure the fields are present.
            // TODO: remove OCAP check?
            if ((prefix.compareToIgnoreCase("ocap") == 0) && (sId == null || aId == null))
            {
                if (log.isErrorEnabled())
                {
                    log.error("Certificates missing authority or subject key identifiers");
                }
                return false; // Invalid OCAP chain.
            }
            // Check the validity of the current certificate and make sure it's
            // authority identifier matches the subject identifier of the next
            // one.
            if (!checkValidity(certChain[i]) || !Arrays.equals(sId, aId))
            {
                if (log.isErrorEnabled())
                {
                    log.error("Certificates authority-subject key identifiers mismatch");
                }
                return false; // Invalid chain.
            }
            else
            {
                // Verify the current certificate is signed by the next.
                try
                {
                    certChain[i].verify(certChain[i + 1].getPublicKey());
                }
                catch (Exception e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Certificates chain does not verify");
                    }
                    return false; // Invalid certificate.
                }
            }
        }
        // Now make sure the final certificate in the chain is valid.
        if (!checkValidity(certChain[i]))
        {
            if (log.isErrorEnabled())
            {
                log.error("Certificates chain does not verify");
            }
            return false;
        }
        // If requested, record the certificates.
        if (signers != null) signers.add(certChain);

        return true; // Valid chain.
    }

    private String getDistinguishedNameField(String fieldName, String distinguishedName)
    {
        // distiguished name e.g.
        // "CN=Duke, OU=JavaSoft, O=Sun Microsystems, C=US"

        int startIndex = 0;
        int endIndex = 0;
        while (true)
        {
            String field = "";
            endIndex = distinguishedName.indexOf(",", startIndex);
            if (endIndex >= 0)
            {
                field = distinguishedName.substring(startIndex, endIndex);
                field = field.trim();
                startIndex = endIndex + 1; // size of ", "
            }
            else
            {
                field = distinguishedName.substring(startIndex);
                field = field.trim();
                startIndex += field.length();
            }

            int index = field.indexOf("=");
            if (index >= 0)
            {
                String fieldNameTemp = field.substring(0, index);
                if (fieldNameTemp.equals(fieldName))
                {
                    return field.substring(index + 1);
                }
            }

            if (startIndex >= distinguishedName.length())
            {
                break;
            }
        }

        return null;
    }

    public int getLeafOrgID()
    {
        if ((certs == null) || certs.length <= 2)
        {
            return 0;
        }

        X509CertImpl leafCert = (X509CertImpl) certs[0];
        try
        {
            return getOrgID(leafCert);
        }
        catch (Exception ex)
        {
            return 0;
        }
    }

    private int getOrgID(X509CertImpl cert) throws Exception
    {
        X500Principal subjectPrincipal = cert.getSubjectX500Principal();

        String subjectOrgName = getDistinguishedNameField("O", subjectPrincipal.getName());
        if (subjectOrgName == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error getting OrgID from cert: NULL subjectOrgName");
            }
            throw new Exception("Error getting OrgID from cert: NULL subjectOrgName");
        }
        else
        {
            subjectOrgName = subjectOrgName.trim();
            if (subjectOrgName.length() == 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error getting OrgID from cert: Empty subjectOrgName");
                }
                throw new Exception("Error getting OrgID from cert: Empty subjectOrgName");
            }
        }

        // parse subjectOrgName to get orgID
        int index = subjectOrgName.indexOf(".", 0);
        if (index >= 0)
        {
            String orgIDString = subjectOrgName.substring(index + 1);
            int orgID = (int)(Long.parseLong(orgIDString, 16) & 0xFFFFFFFF);
            if (log.isDebugEnabled())
            {
                log.debug("orgID = " + orgID);
            }
            return orgID;
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("Error getting OrgID from cert: Failed to parse subjectOrgName");
            }
            throw new Exception("Error getting OrgID from cert: Failed to parse subjectOrgName");
        }
    }

    private boolean verifyCertificatePerOCAP(X509CertImpl cert, boolean leaf, int certChainLength,
            int certChainPosition, boolean isDeviceCert)
    {
        boolean pass = true;

        if (log.isDebugEnabled())
        {
            log.debug("Starting certificate verification...");
        }

        // all certificates must have non-empty Issuer Common Name
        X500Principal issuerPrincipal = cert.getIssuerX500Principal();

        String issuerCommonName = getDistinguishedNameField("CN", issuerPrincipal.toString());
        if (log.isDebugEnabled())
        {
            log.debug("Verifying that issuerCommonName is not null or empty...");
        }
        if (issuerCommonName == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("NULL issuerCommonName: cert verification failed");
            }
            pass = false;
        }
        else
        {
            issuerCommonName = issuerCommonName.trim();
            if (issuerCommonName.length() == 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Empty issuerCommonName: cert verification failed");
                }
                pass = false;
            }
        }

        // all certificates must have non-empty commonName subject attribute
        // all certificates must have non-empty countryName subject attribute
        // leaf certificate must have non-empty organizationName subject
        // attribute
        X500Principal subjectPrincipal = cert.getSubjectX500Principal();

        String subjectCommonName = getDistinguishedNameField("CN", subjectPrincipal.toString());
        if (log.isDebugEnabled())
        {
            log.debug("Verifying that subjectCommonName is not null or empty...");
        }
        if (subjectCommonName == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("NULL subjectCommonName: cert verification failed");
            }
            pass = false;
        }
        else
        {
            subjectCommonName = subjectCommonName.trim();
            if (subjectCommonName.length() == 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Empty subjectCommonName: cert verification failed");
                }
                pass = false;
            }
        }

        String subjectCountryName = getDistinguishedNameField("C", subjectPrincipal.getName());
        if (log.isDebugEnabled())
        {
            log.debug("Verifying that subjectCountryName is not null or empty...");
        }
        if (subjectCountryName == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("NULL subjectCountryName: cert verification failed");
            }
            pass = false;
        }
        else
        {
            subjectCountryName = subjectCountryName.trim();
            if (subjectCountryName.length() == 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Empty subjectCountryName: cert verification failed");
                }
                pass = false;
            }
        }

        String subjectOrgName = getDistinguishedNameField("O", subjectPrincipal.getName());
        if (log.isDebugEnabled())
        {
            log.debug("Verifying that subjectOrgName is not null or empty...");
        }
        if (subjectOrgName == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("NULL subjectOrgName: cert verification failed");
            }
            pass = false;
        }
        else
        {
            subjectOrgName = subjectOrgName.trim();
            if (subjectOrgName.length() == 0)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Empty subjectOrgName: cert verification failed");
                }
                pass = false;
            }
        }

        // if keyUsage extension is present, check value
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that KeyUsageExtension is present...");
            }
            String extAlias = OIDMap.getName(PKIXExtensions.KeyUsage_Id);
            KeyUsageExtension keyUsageExt = (KeyUsageExtension) cert.get(extAlias);
            if (keyUsageExt != null)
            {
                // if keyUsage extension in a leaf certificate is marked as
                // critical then digitalSignature bit must be set
                // if keyUsage extension in a non-leaf certificate is marked as
                // critical then the keyCertSign bit must be set
                if (keyUsageExt.isCritical())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("KeyUsageExtension is critical -- Verifying that required bits are set...");
                    }
                    if (isDeviceCert)
                    {
                        if (!((Boolean) keyUsageExt.get(KeyUsageExtension.DIGITAL_SIGNATURE)).booleanValue())
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("DIGITAL_SIGNATURE bit not set: cert verification failed");
                            }
                            pass = false;
                        }
                    }
                    else
                    {
                        if (!((Boolean) keyUsageExt.get(KeyUsageExtension.KEY_CERTSIGN)).booleanValue())
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("KEY_CERTSIGN bit not set: cert verification failed");
                            }
                            pass = false;
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            // key usage not present -- discard exception
        }

        // the number of certificates in the chain below a non-leaf certificate
        // must not exceed that specified in the Basic Constraints
        // extension if the Basicconstraints cert has CA=true
        if (log.isDebugEnabled())
        {
            log.debug("Verifying that BasicConstraintsExtension is followed...");
        }
        BasicConstraintsExtension basicConstraintsExt = cert.getBasicConstraintsExtension();
        if (basicConstraintsExt != null)
        {
            try
            {
                if (((Boolean) basicConstraintsExt.get(BasicConstraintsExtension.IS_CA)).booleanValue())
                {
                    Integer pathLengthInteger = (Integer) basicConstraintsExt.get(BasicConstraintsExtension.PATH_LEN);
                    if (pathLengthInteger != null) // null indicate no
                                                   // pathlength restriction
                    {
                        int pathLength = pathLengthInteger.intValue();
                        if (pathLength == 0)
                        {
                            pathLength = 1; // x509 says if pathlength = 0, then
                                            // can have one leaf cert
                        }

                        if (certChainPosition > pathLength)
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("BasicConstraintsExtension pathLength (" + pathLength + ") exceeded ("
                                        + certChainPosition + "): cert verification failed");
                            }
                            pass = false;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                if (log.isErrorEnabled())
                {
                    log.error("BasicConstraintsExtension decode failed: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Authority Key Identifier" certificate extension must not be
        // marked critical
        AuthorityKeyIdentifierExtension authorityKeyIdentifierExt = cert.getAuthorityKeyIdentifierExtension();
        if (authorityKeyIdentifierExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that AuthorityKeyIdentifierExtension is not critical...");
            }
            if (authorityKeyIdentifierExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("AuthorityKeyIdentifierExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Subject Key Identifier" certificate extension must not be marked
        // critical
        SubjectKeyIdentifierExtension subjectKeyIdentifierExt = cert.getSubjectKeyIdentifierExtension();
        if (subjectKeyIdentifierExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that SubjectKeyIdentifierExtension is not critical...");
            }
            if (subjectKeyIdentifierExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("SubjectKeyIdentifierExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Private Key Usage" certificate extension must not be marked
        // critical
        PrivateKeyUsageExtension privateKeyUsageExt = cert.getPrivateKeyUsageExtension();
        if (privateKeyUsageExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that PrivateKeyUsageExtension is not critical...");
            }
            if (privateKeyUsageExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("PrivateKeyUsageExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Certificate policies" certificate extension must not be marked
        // critical
        // SA: JVM doesn't support CertificatePoliciesExtension, so use base
        // class Extension
        Extension certPoliciesExt = cert.getExtension(PKIXExtensions.CertificatePolicies_Id);
        if (certPoliciesExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that CertificatePoliciesExtension is not critical...");
            }
            if (certPoliciesExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("CertificatePoliciesExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Policy Mappings" certificate extension must not be marked
        // critical
        PolicyMappingsExtension policyMappingsExt = cert.getPolicyMappingsExtension();
        if (policyMappingsExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that PolicyMappingsExtension is not critical...");
            }
            if (policyMappingsExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("PolicyMappingsExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Subject Alternative Name" certificate extension must not be
        // marked critical
        SubjectAlternativeNameExtension subjectAlternativeNameExt = cert.getSubjectAlternativeNameExtension();
        if (subjectAlternativeNameExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that SubjectAlternativeNameExtension is not critical...");
            }
            if (subjectAlternativeNameExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("SubjectAlternativeNameExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Issuer Alternative Name" certificate extension must not be
        // marked critical
        IssuerAlternativeNameExtension issuerAlternativeNameExt = cert.getIssuerAlternativeNameExtension();
        if (issuerAlternativeNameExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that IssuerAlternativeNameExtension is not critical...");
            }
            if (issuerAlternativeNameExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("IssuerAlternativeNameExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "Policy Constraints" certificate extension must not be marked
        // critical
        PolicyConstraintsExtension policyConstraintsExt = cert.getPolicyConstraintsExtension();
        if (policyConstraintsExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that PolicyConstraintsExtension is not critical...");
            }
            if (policyConstraintsExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("PolicyConstraintsExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        // the "CRL Distribution points" certificate extension must not be
        // marked critical
        // SA: JVM doesn't support CRLDistributionPointsExtension, so use base
        // class Extension
        Extension crlDistPointsExt = cert.getExtension(PKIXExtensions.CRLDistributionPoints_Id);
        if (crlDistPointsExt != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Verifying that CRLDistributionPointsExtension is not critical...");
            }
            if (crlDistPointsExt.isCritical())
            {
                if (log.isErrorEnabled())
                {
                    log.error("CRLDistributionPointsExtension marked critical: cert verification failed");
                }
                pass = false;
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("Certificate verification complete: " + (pass ? "SUCCESS" : "FAILURE"));
        }

        return pass;
    }

    /**
     * Verify that the specified CRL file is correctly signed by the certificate
     * chain in this certificate file. The verification is made by first
     * locating the certificate in the chain that is identified as the signing
     * certificate and then verifying the certificate chain from that point on
     * up.
     * 
     * @param crl
     *            is the X509CRL to verify.
     * 
     * @return true if the CRL file is correctly signed.
     */
    boolean verifyCRLSignature(X509CRL crl)
    {
        X509Certificate cert = null;
        byte[] auth;
        int i;

        // Get the authority key identifier from the CRL extension field.
        if ((auth = crl.getExtensionValue(AUTH_KEY_ID)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CRL file's signer is missing the authority key identifier");
            }
            return false; // Can't get authority key identifier.
        }

        // Locate the certificate with a matching AuthorityKeyIdentifier.
        X509Certificate[] certChain = getCertificates();
        for (i = 0; i < certChain.length; ++i, cert = null)
        {
            if (java.util.Arrays.equals(certChain[i].getExtensionValue(AUTH_KEY_ID), auth))
            {
                cert = certChain[i];
                break; // Potential signing certificate found!
            }
        }

        if (cert == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No matching certificate found for CRL file");
            }
            return false; // No signing certificate found.
        }

        // Now verify the certificate chain from the potential signing cert on
        // up.
        // Validate each certificate against the next higher certificate.
        for (; i < certChain.length - 1; ++i)
        {
            // Check the validity of the current certificate and make sure it's
            // authority identifier matches the subject identifier of the next
            // one.
            if (!checkValidity(certChain[i])
                    || !Arrays.equals((certChain[i + 1]).getExtensionValue(SUBJ_KEY_ID),
                            (certChain[i]).getExtensionValue(AUTH_KEY_ID)))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CRL file signed by invalid certificate chain");
                }
                return false; // Invalid chain.
            }
            else
            {
                // Verify the current certificate is signed by the next.
                try
                {
                    certChain[i].verify(certChain[i + 1].getPublicKey());
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CRL file signed by invalid certificate chain");
                    }
                    return false; // Invalid certificate.
                }
            }
        }
        // Now make sure the final certificate in the chain is valid.
        return checkValidity(certChain[i]);
    }

    /**
     * Determine the validity of a specific certificate. Validity
     * 
     * @param hf
     *            is the target hashfile associated with the certificate chain.
     * @param cert
     *            is the certificate to check.
     * 
     * @return boolean indicating whether it is valid or not (true == valid).
     */
    private boolean checkValidity(X509Certificate cert)
    {
        // First check the validity of the certificate data.
        try
        {
            cert.checkValidity();
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Invalid certificate error");
            }
            return false; // Not valid.
        }

        // Now verify it hasn't been revoked.
        return (authMgr.isRevoked(cert) == false);
    }

    /**
     * Get the <code>KeyIdentifier</code> value of the specified key type, which
     * is either a <code>SubjectKeyIdentifier</code> or
     * <code>AuthorityKeyIdentier</code>.
     * 
     * @param val
     *            is the byte array holding the DER value.
     * @param keyType
     *            is the key type to decode.
     * 
     * @return byte array containing the key value.
     */
    private byte[] getKeyId(byte[] val, String keyType)
    {
        byte[] keyId;

        try
        {
            // Get DEV value representation.
            DerValue der = new DerValue(val);
            der = der.data.getDerValue();

            // Check for subject key.
            if (keyType.compareTo(SUBJ_KEY_ID) == 0)
            {
                // Get octet representation (i.e. the value).
                der.resetTag(DerValue.tag_OctetString);
                keyId = der.getOctetString();
            }
            else
            {
                // For authority, pull out secondary DER value first.
                der = der.data.getDerValue();

                // Get octet representation (i.e. the value).
                der.resetTag(DerValue.tag_OctetString);
                keyId = der.getOctetString();
            }
            return keyId;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get the root certificate.
     * 
     * @return Certificate entry from file that is the root (last).
     */
    X509Certificate getRootCert()
    {
        // Root certificate is the last one in the file.
        return ((certs != null) ? certs[certs.length - 1] : null);
    }

    /**
     * Get the leaf Certificate.
     * 
     * @return Certificate entry from the file that is the leaf (first).
     */
    X509Certificate getLeafCertificate()
    {
        // Leaf certificate is the first one in the file.
        return ((certs != null) ? certs[0] : null);
    }

    /**
     * Get the entire certificate chain.
     * 
     * @return Certificate[] return the array of certificates.
     */
    X509Certificate[] getCertificates()
    {
        return certs;
    }

    // Array of certificates from file (in order).
    private X509Certificate[] certs = null;

    private AuthManagerImpl authMgr;

    private final String AUTH_KEY_ID = "2.5.29.35";

    private final String SUBJ_KEY_ID = "2.5.29.14";

    private final String KEY_USAGE_ID = "2.5.29.15";

    private static final Logger log = Logger.getLogger(CertificateFile.class.getName());
}
