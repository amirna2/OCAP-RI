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

import java.io.IOException;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.apache.log4j.Logger;

import java.security.cert.X509Certificate;
import java.security.Signature;
import java.security.PublicKey;

import java.util.Vector;

/**
 * This class is used to validate and maintain the verification state of a
 * particular signed sub-tree within a file system. It essentially contains the
 * logic for verifying a signature file against a certificate file, including
 * verification of the root certificate against the known certificates to the
 * platform along with check against any CRLs. Once a particular subtree has
 * been verified the authentication subtree object can be subsequently checked
 * for fast determination of the entire authentication chain.
 * 
 * Support for authentication of class loader and java.io accessed files is
 * provided, but through different internal private methods since the
 * authentication requirements are slightly different.
 */
class AuthSubTree
{
    /**
     * Constructor.
     * 
     * @param treePath
     *            is the sub-path the location of the authentication subtree
     *            (i.e. the location of the signature and certificate files.
     * @param prefix
     *            is the security message file prefix (i.e. ocap or dvb).
     * @param xletSigners
     *            is a vector of the signers that signed the initial xlet class
     *            file. It's used to verify that other files loaded through the
     *            class loader are signed by one of the same signers.
     */
    AuthSubTree(String treePath, FileSys fs, String prefix, Vector xletSigners)
    {
        this.fs = fs; // Save the file system to use.
        this.path = treePath; // Save path to sub-tree.
        this.msgPrefix = prefix; // Save the security message prefix (ocap or
                                 // dvb).
        this.xletSigners = xletSigners; // Save the signers of the initial xlet
                                        // file.

        this.authMgr = (AuthManagerImpl) ManagerManager.getInstance(AuthManager.class);
    }

    /**
     * Determine the type of authentication required (class loader or java.io)
     * and dispatch to the appropriate method.
     * 
     * @param hf
     *            is the hash file to check against the signature file.
     * @param numSigners
     *            is the number of signers that must be present. The possible
     *            values are 1, 2 and (-1). (-1) indicates a "get signers"
     *            operation is under way and will cause all signers to be
     *            collected.
     * @param rtnSigners
     *            is a vector used to record the signers or null if the caller
     *            doesn't care.
     * @param isClassLoader
     *            is a flag that indicates if the verification process should
     *            include the semantic checks required for accessing files
     *            through the class loader (i.e. privileged certificates and
     *            files signed by at least the same certificate as the initial
     *            Xlet class).
     * @param checkRoot
     *            is a flag indicating whether or not to verify that the root
     *            certificate of each chain is valid (i.e. known to the box).
     * @param e
     *            is a single element array for returning any exceptions that
     *            occur.
     * 
     * @return boolean indicating whether the file is correctly signed by the
     *         specified number of signers.
     */
    boolean isSigned(HashFile hf, int numSigners, Vector rtnSigners, boolean isClassLoader, boolean checkRoot, int orgId)
            throws FileSysCommunicationException
    {
        // Call method based on action type.
        return ((isClassLoader) ? isSigned(hf, numSigners, rtnSigners, orgId) // Class
                                                                              // loader
                                                                              // check.
                : isSigned(hf, rtnSigners, checkRoot, orgId)); // java.io check.
    }

    /**
     * This method is used to check the "signed" status of files that are access
     * via java.io (e.g. java.io.File, DSMCCObject). If "root" validity is
     * indicated, check that the certificate chain is signed by a root
     * certificate. And, return all of the valid root or non-root signers that
     * are found.
     * 
     * @param hf
     *            is the hash file to check against the signature file.
     * @param rtrnSigners
     *            is a vector used to record the signers or null if the caller
     *            doesn't care.
     * @param checkRoot
     *            is a flag indicating whether or not to verify that the root
     *            certificate of each chain is valid (i.e. known to the box).
     * 
     * @return boolean indicating whether the file is correctly signed by the
     *         specified number of signers.
     */
    private boolean isSigned(HashFile hf, Vector rtnSigners, boolean checkRoot, int orgId)
    {
        Vector signers = new Vector(1);
        int validSignerCnt = 0;
        SignatureFile sf;
        CertificateFile cf;

        // value of the organisation_id contained within the organisationName of
        // the Subject of any of the leaf certificates
        // used to sign an application must match the organisation_id in the
        // application's application identifier
        boolean leafOrgIdVerified = (orgId == 0); // don't verify orgID if ==
                                                  // zero
        if (log.isDebugEnabled())
        {
            log.debug("OrgId = " + orgId);
        }

        // If subtree has already been checked, just return the validity
        // state and possibly associated signers (root or non-root).
        if (checkRoot)
        {
            if (rootAuthState == true)
            {
                // Return signers if requested.
                if (rtnSigners != null) rtnSigners.addAll(0, rootSigners);
                return rootValidity;
            }
        }
        else
        {
            if (nonRootAuthState == true)
            {
                // Return signers if requested.
                if (rtnSigners != null) rtnSigners.addAll(0, nonRootSigners);
                return nonRootValidity;
            }
        }

        // Scan for signature file/certificate file pairs verifying as we go.
        for (int i = 1; true; ++i)
        {
            byte[] file = null;
            String cfName = path + "/" + msgPrefix + crtFile + i;

            // First, try to get the certificate file.
            try
            {
                file = fs.getFileData(cfName).getByteData();

                // Instantiate a certificate file (i.e. parse it).
                cf = new CertificateFile(file, authMgr);

                // Check for a valid root certificate.
                if (checkRoot)
                {
                    if (isValidRoot(cf.getRootCert()) == false) continue; // Not
                                                                          // signed
                                                                          // by
                                                                          // root,
                                                                          // skip
                                                                          // it...
                }

                // First, try to get the signature file.
                file = fs.getFileData(path + "/" + msgPrefix + sigFile + i).getByteData();
            }
            catch (Exception e)
            {
                break;
            }

            // Instantiate a signature file (i.e. parse it).
            sf = new SignatureFile(file);

            // Verify the signature file correctly signs the hash file.
            if (!verifySignature(hf, sf, cf)) continue; // Not valid, try next
                                                        // pair.

            // Found a certificate file that correctly signs a signature file,
            // now follow the certificate chain up to validate
            if (cf.verifyCertificates(signers, msgPrefix))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("The cert chain is validated");
                }

                ++validSignerCnt; // Increment signer count

                if (log.isDebugEnabled())
                {
                    log.debug("Verifying leaf org id");
                }
                if (cf.getLeafOrgID() == orgId)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("leafOrgIdVerified");
                    }
                    leafOrgIdVerified = true;
                }
            }

        }

        if (!leafOrgIdVerified)
        {
            return false;
        }

        // Cache validity results based on search type.
        if (checkRoot)
        {
            // Set state of authentication for root signers.
            rootAuthState = true;

            // Set validity of root signers.
            if (validSignerCnt == 0)
                rootValidity = false; // No valid root signers.
            else
            {
                rootValidity = true; // Valid root signers.
                rootSigners = (Vector) signers.clone(); // Cache the root
                                                        // signers.
                if (rtnSigners != null) rtnSigners.addAll(0, signers); // Return
                                                                       // the
                                                                       // root
                                                                       // signers.
            }
            return rootValidity;
        }

        // Set state of authentication for root signers.
        nonRootAuthState = true;

        // Set validity of root signers.
        if (validSignerCnt == 0)
            nonRootValidity = false; // No valid non-root signers.
        else
        {
            nonRootValidity = true; // Valid non-root signers.
            nonRootSigners = (Vector) signers.clone();// Cache the non-root
                                                      // signers.
            if (rtnSigners != null) rtnSigners.addAll(0, signers); // Return the
                                                                   // non-root
                                                                   // signers.
        }
        return nonRootValidity;
    }

    /**
     * This method is used to check the "signed" status of files that are being
     * accessed via a class loader for applications that are either signed or
     * dually signed. Determine if any signature files are present at the level
     * of the specified hash file and if so, verify that the hash file is
     * correctly signed by the number of signature files and certificate chains
     * specified.
     * 
     * @param hf
     *            is the hash file to check against the signature file.
     * @param numSigners
     *            is the number of signers that must be present (i.e. 1 or 2).
     * @param rtrnSigners
     *            is a vector used to record the signers or null if the caller
     *            doesn't care.
     * 
     * @return boolean indicating whether the file is correctly signed by the
     *         specified number of signers.
     */
    private boolean isSigned(HashFile hf, int numSigners, Vector rtnSigners, int orgId)
            throws FileSysCommunicationException
    {
        boolean isDuallySigned = (numSigners == 2);
        boolean isPrivileged = false;

        // value of the organisation_id contained within the organisationName of
        // the Subject of any of the leaf certificates
        // used to sign an application must match the organisation_id in the
        // application's application identifier
        boolean leafOrgIdVerified = (orgId == 0); // don't verify orgID if ==
                                                  // zero
        if (log.isDebugEnabled())
        {
            log.debug("OrgId = " + orgId);
        }

        Vector signers = new Vector(1);
        SignatureFile sf;
        CertificateFile cf;
        int signerCnt = 0;

        if (log.isDebugEnabled())
        {
            log.debug("beginning validation of signature file and certificate chain");
        }
        if (log.isDebugEnabled())
        {
            log.debug("Hash file path is " + hf.getPath());
        }
        if (log.isDebugEnabled())
        {
            log.debug("Number of signers is " + numSigners);
        }
        // If subtree has already been checked and we aren't gathering
        // signers, just return the validity state.
        if (authState == true && rtnSigners == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("This subtree has already been checked and we are not gathering signers.");
            }
            return validity;
        }
        // Set flag indicating the tree has gone through class loader
        // authentication process.
        authState = true;

        // Scan for signature file/certificate file pairs verifying as we go.
        for (int i = 1; signerCnt != numSigners; ++i)
        {
            String cfName = path + "/" + msgPrefix + crtFile + i;
            if (log.isInfoEnabled())
            {
                log.info("Certificate file name is " + cfName);
            }

            // First, try to get the certificate file.
            byte[] file = null;
            try
            {
                file = fs.getFileData(cfName).getByteData();
            }
            catch (IOException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unable to locate certificate file -- " + cfName);
                }
                // Can't locate certificate file, invalid chain.
                break;
            }

            // Validate that the certificate file is listed in the hashfile.
            if (hf.getEntry(cfName) == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("The certificate file is not listed in the hash file");
                }
                continue; // Not listed in hashfile, ignore it.
            }
            // Instantiate a certificate file (i.e. parse it).
            cf = new CertificateFile(file, authMgr);

            X509Certificate rootCert = cf.getRootCert();
                if (rootCert != null)
                {
                if (log.isDebugEnabled())
                {
                    log.debug("The root cert in the cert file is as follows:\n" + rootCert.toString());
                }
            }
                else
                {
                if (log.isDebugEnabled())
                {
                    log.debug("The root Cert in the cert file is null");
                }
            }
            // Check for a valid root certificate.
            if (isValidRoot(rootCert) == false)
            {
                if (log.isErrorEnabled())
                {
                    log.error("This root cert is not known to the middleware");
                }
                continue; // Try next set of certificate/signature pairs.
            }
            // First, try to get the signature file.
            String sfName = path + "/" + msgPrefix + sigFile + i;
            if (log.isInfoEnabled())
            {
                log.info("Signature file is " + sfName);
            }

            try
            {
                file = fs.getFileData(sfName).getByteData();
            }
            catch (IOException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unable to locate signature file -- " + sfName);
                }
                // Can't locate certificate file, invalid chain.
                break;
            }

            // Instantiate a signature file (i.e. parse it).
            sf = new SignatureFile(file);

            // Verify the signature file correctly signs the hash file.
            if (verifySignature(hf, sf, cf) == false)
            {
                if (log.isErrorEnabled())
                {
                    log.error("The signature file does not correctly sign the hash file");
                }
                continue; // Not valid, try next pair.
            }
            // Found a certificate file that correctly signs a signature file,
            // now follow the certificate chain up to further validate.
            if (cf.verifyCertificates(signers, msgPrefix))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("The cert chain is validated");
                }
                ++signerCnt; // Increment signer count;

                if (log.isDebugEnabled())
                {
                    log.debug("Verifying leafOrgId");
                }
                if (cf.getLeafOrgID() == orgId)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("leafOrgId Verified");
                    }
                    leafOrgIdVerified = true;
                }

                // If it's a class loader verification and the app is dually
                // signed,
                // check for privileged leaf certificate (at least one of the
                // leaf
                // certificates needs to be privileged).
                if (isDuallySigned) isPrivileged = (isPrivileged | isLeafPrivileged(cf));
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("The cert chain failed validation");
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("The end of processing signature number " + i + ".");
            }
        }
        // If collecting signers, return the signers.
        if (rtnSigners != null && signerCnt != 0)
        {
            if (rootSigners == null) rootSigners = (Vector) signers.clone(); // Cache
                                                                             // root
                                                                             // signers.
            rtnSigners.addAll(0, signers);
        }

        // Specified number of signers were found, now if it's dually signed
        // make sure at least
        // one of the certificates was privileged & there's aleast 2 unique
        // signer chains.
        // Also make sure the target is signed by one of the initial xlet
        // signers.
        validity = ((isDuallySigned ? (isPrivileged && areUnique(signers)) : true) && isXletSigner(signers)
                && (signerCnt == numSigners) && leafOrgIdVerified);
        if (log.isErrorEnabled())
        {
            if (validity == false)
        {
                if (signerCnt != numSigners)
        {
                    if (log.isErrorEnabled())
        {
                    log.error("Expected " + numSigners + " and found " + signerCnt + " signers");
        }
                }
                else if (isXletSigner(signers) == false)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Target file is not signed by the same certificates as initial xlet");
                    }
                }
                else if (isDuallySigned)
                {
                    if (isPrivileged == false)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Dual signed application is not signed by privileged certificate");
                        }
                    }
                    else if (areUnique(signers) == false)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Dual signed application does not have unique signers");
                        }
                    }
                }
                else if (!leafOrgIdVerified)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("OrgId does not match orgId in any leaf certificate");
                    }
            }
        }
        }

        // Return validity state.
        return validity;
    }

    /**
     * Check that the specified hash file is signed by the signature file and
     * leaf certificate (from certificate file) pair.
     * 
     * @param hf
     *            is the hash file to verify against the signature/certificate
     *            file pair.
     * @param sf
     *            is the associated signature file to check.
     * @param cf
     *            is the associated certificate file containing the leaf
     *            certificate to check.
     * 
     * @return boolean indicating whether the
     */
    private boolean verifySignature(HashFile hf, SignatureFile sf, CertificateFile cf)
    {
        // Get the leaf certificate.
        X509Certificate leafCert = cf.getLeafCertificate();
        Signature sig;

        if (leafCert == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Leaf cert is null.");
            }
            return false;
        }

        // First check AuthorityKeyIdentifier of the signature file
        // against the SubjectKeyIdentifier of the leaf certificate.
        if (cf.verifyKeyIdentifiers(sf, msgPrefix) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("The check of signature's AuthorityKeyIdentifier against leaf cert's SubjectKeyIdentifier failed");
            }
            return false;
        }

        // Initialize the signature with the public key from the certificate.
        try
        {
            // Get signature algorithm and signature from signature file.
            String algo = sf.getSigAlgo();
            byte[] sfsig = sf.getSignature();

            // Make sure they were found in signature file.
            if (algo == null || sfsig == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Hash file not signed, signature algorithm or signature is null");
                }
                return false;
            }
            if (log.isDebugEnabled())
            {
                log.debug("Got algorithm  for signature: " + algo + " ;sig bytes:" + sfsig.length);
            }

            // Get an instance of a signature implementing the correct
            // algorithm.
            sig = Signature.getInstance(algo);

            PublicKey pk = leafCert.getPublicKey();
            sig.initVerify(pk);

            // Read the contents of the hashfile.
            byte[] hashfile;
            if ((hashfile = hf.getBytes()) == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Hash file not signed, hash file bytes are unavailable");
                }
                return false; // Oops, can't read hash file, fail.
            }
            // Update the signature with the contents of the hash file.
            sig.update(hashfile);
            if (!sig.verify(sfsig))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Hash file signature is invalid");
                }
                return false; // Signature doesn't authenticate file.
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            return false;
        }
        // Signature file does sign hash file.
        return true;
    }

    /**
     * Determine if the specified root certificate is known to the platform.
     * 
     * @param Certificate
     *            is the root certificate reference.
     * 
     * @return boolean true value if the root certificate is valid.
     */
    private boolean isValidRoot(X509Certificate root)
    {
        return authMgr.isValidRoot(root);
    }

    /**
     * Determine if the leaf certificate is a member of the set of privileged
     * certificates signaled in the privileged_certificates_descriptor in the
     * XAIT.
     * 
     * @param cf
     *            is the certificate file containing the target leaf
     *            certificate.
     * 
     * @return boolean indicating whether the leaf certificate is privileged.
     */
    private boolean isLeafPrivileged(CertificateFile cf)
    {
        return authMgr.isPrivileged(cf.getLeafCertificate());
    }

    /**
     * Determine if any of the specified signers is included in the set of
     * initial xlet class file signers. This check is required for all accesses
     * to files via the class loader. Since each of the certificate chains in
     * both the specified subtree and the initial xlet have been verified up to
     * this point, all that needs to be done is make sure one of the leaf
     * certificates (signers) of the subtree is the same as one of the leaf
     * certificates (signers) of the initial xlet.
     * 
     * @param signers
     *            is a vector of signer chains for a subtree.
     * 
     * @return boolean indicating
     */
    private boolean isXletSigner(Vector signers)
    {
        X509Certificate[] xletCerts; // Initial xlet certificat chain.
        X509Certificate[] certs; // Subtree certificate chain.

        // Iterate through the subtree signers.
        for (int i = 0; i < signers.size(); ++i)
        {
            certs = (X509Certificate[]) signers.get(i);

            // Iterate through the initial xlet signers.
            for (int j = 0; j < xletSigners.size(); ++j)
            {
                xletCerts = (X509Certificate[]) xletSigners.get(j);

                // If the leaf certificates match, we're ok.
                if (certs[0].equals(xletCerts[0])) return true;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("No common signers found between target and initial xlet");
        }
        return false; // No common signers between subtree & initial xlet.
    }

    /**
     * This method enforces the certificate requirements associated with dually
     * signed applications. The two requirements being: 1) that at least 2 of
     * the leaf certificates found are unique and 2) that these certificate
     * chains have a common root certificate. These rules come from OCAP
     * 14.2.1.15 item 9.
     * 
     * @param signers
     *            this set of signer chains found.
     * 
     * @return true if there are aleast 2 unique signing chains.
     */
    private boolean areUnique(Vector signers)
    {
        if (signers.size() == 1) return true;

        X509Certificate[] certs; // Subtree certificate chain.

        // Iterate through the subtree signers.
        for (int i = 0; i < signers.size(); ++i)
        {
            // Get the next chain.
            certs = (X509Certificate[]) signers.get(i);

            // Iterate through the remaining chains looking for a unique leaf.
            for (int j = i + 1; j < signers.size(); ++j)
            {
                X509Certificate[] nextSet = (X509Certificate[]) signers.get(j);

                // If the leaf certificates don't match & have common roots,
                // we're ok.
                if ((certs[0].equals(nextSet[0]) == false) && haveCommonRoots(certs, nextSet)) return true;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("No unique leaf signers or no common root found for dual signed target");
        }
        return false; // No unique leaf signers found.
    }

    /**
     * Verify the two certificate chains have a common root certificate (i.e.
     * the last certificates in each array are the same).
     * 
     * @param chain1
     *            is the first certificate chain.
     * @param chain2
     *            is the second certificate chain.
     * 
     * @return true if the chains have a common root certificate.
     */
    private boolean haveCommonRoots(X509Certificate[] chain1, X509Certificate[] chain2)
    {
        return (chain1[chain1.length - 1].equals(chain2[chain2.length - 1]));
    }

    // File system to use for file access.
    private FileSys fs;

    // Subpath to the authentication subtree.
    private String path;

    // Security message prefix (i.e. ocap or dvb).
    private String msgPrefix;

    // Flags indicating java.io related validity check has been made on
    // this sub-tree and the result of that check. The root vs. non-root
    // certificate distinction is in support of
    // DSMCCObject.getSigners(knownRoot);
    private boolean rootAuthState = false; // Root cert based auth.

    private boolean rootValidity = false;

    private boolean nonRootAuthState = false; // Non-root cert based auth.

    private boolean nonRootValidity = false;

    // Flags indicating class loader related validity check has been made
    // on this sub-tree and the result of that check.
    private boolean authState = false;

    private boolean validity = false;

    // Vector containing the signers of the initial Xlet class file.
    private Vector xletSigners = null;

    // Vectors (i.e. cache) containing root and non-root signers.
    private Vector rootSigners = null;

    private Vector nonRootSigners = null;

    private final String sigFile = ".signaturefile.";

    private final String crtFile = ".certificates.";

    private AuthManagerImpl authMgr = null;

    private static final Logger log = Logger.getLogger(AuthSubTree.class.getName());
}
