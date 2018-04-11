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

//TODO: figure out how use these classes instead of internal sun classes.
//import com.sun.net.ssl.internal.ssl.DerInputStream;
//import com.sun.net.ssl.internal.ssl.DerValue;

import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralName;
import sun.security.x509.SerialNumber;
import sun.security.util.DerValue;
import sun.security.util.DerInputStream;
import sun.security.util.ObjectIdentifier;

import java.io.IOException;
import java.util.Iterator;

import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.Arrays;

/**
 * This class us used to parse and provide the contents of a signature file. The
 * SignatureFile is a File containing one digital signature. It contains the
 * following ASN.1 DER structure:
 * 
 * Signature ::= SEQUENCE { certificateIdentifier AuthorityKeyIdentifier,
 * hashSignatureAlgorithm OBJECT IDENTIFIER, signatureValue BIT STRING }
 * certificateIdentifier : As defined in the ITU-T X.509 [54] extension for the
 * AuthorityKeyIdentifier field. It identifies the certificate that carries the
 * certified public key that is used to check the signature.
 * 
 * AuthorityKeyIdentifier ::= SEQUENCE { keyIdentifier [0] KeyIdentifier - NOT
 * OPTIONAL in OCAP, authorityCertIssuer [1] GeneralNames OPTIONAL in OCAP,
 * authorityCertSerialNumber [2] CertificateSerialNumber OPTIONAL in OCAP }
 * hashSignatureAlgorithm: this field identifies the hash algorithm that is
 * used. Note that the encryption algorithm used to compute the signature is
 * already described in the SubjectKeyInfo field of the certificate that
 * certifies this key, and thus only the identification of the hash algorithm is
 * needed. The supported algorithms are MD5 and SHA-1. 158 ETSI TS 101 812
 * V1.3.1 (2003-06) ETSI
 * 
 * md5 OBJECT IDENTIFIER ::= { iso(1) member-body(2) US(840) rsadsi(113549)
 * digestAlgorithm(2) 5 } sha-1 OBJECT IDENTIFIER ::= { iso(1)
 * identified-organization(3) oiw(14) secsig(3) algorithm(2) 26 }
 * 
 * Note: the DER parsing and utility classes from sun.security.util and
 * sun.security.x509 are used to extract and manage the contents of the
 * signature file.
 */
class SignatureFile
{
    /**
     * Constructor.
     * 
     * @param file
     *            is the byte array containing the file contents.
     * @param prefix
     *            is the string prefix of the file indicating whether it is an
     *            OCAP or DVB signature file.
     */
    SignatureFile(byte[] file)
    {
        try
        {
            // Create a DER input stream for parsing from file contents.
            DerInputStream di = new DerInputStream(file);
            DerValue[] vals = di.getSequence(3);

            // Make sure there is a sequence of three values.
            if (vals.length != 3) return; // Invalid signature file.

            // Extract the KeyIdentifier
            aki = new AuthorityKeyIdentifier(vals[0]);

            // Extract the hash signature algorithm.
            ObjectIdentifier oid = vals[1].getOID();
            if (oid != null)
            {
                String algo = oid.toString();
                if (algo.compareTo(md5) == 0)
                    sigHashAlgo = "MD5WITHRSA";
                else if (algo.compareTo(sha1) == 0) sigHashAlgo = "SHA1WITHRSA";
            }

            // Extract the hash value.
            signature = vals[2].getBitString();
        }
        catch (IOException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Get the issuer unique key identifier for this signature.
     * 
     * @return byte[] or null if the identifier could not be read from the file.
     */
    byte[] getKeyIdentifier()
    {
        // Extract and return just the key identifier.
        return (aki != null ? aki.getKeyId() : null);
    }

    String getSigAlgo()
    {
        return sigHashAlgo;
    }

    /**
     * Get the signature value for this signature.
     * 
     * @return byte[] or null if the signature could not be read from the file.
     */
    byte[] getSignature()
    {
        return signature;
    }

    /**
     * Get the <code>authorityCertIssuer</code> directory name used for validate
     * of GEM signature files.
     * 
     * @return String representing the directory name if present, otherwise
     *         null.
     */
    String getAuthorityCertIssuer()
    {
        return aki.getCertIssuerDirectoryName();
    }

    /**
     * This class represents the Authority Key Identifier portion of the
     * signature file.
     * 
     * <p>
     * The authority key identifier extension provides a means of identifying
     * the particular public key used to sign a certificate. This extension
     * would be used where an issuer has multiple signing keys (either due to
     * multiple concurrent key pairs or due to changeover).
     * <p>
     * The ASN.1 syntax for this is:
     * 
     * <pre>
     * AuthorityKeyIdentifier ::= SEQUENCE {
     *    keyIdentifier             [0] KeyIdentifier           OPTIONAL,
     *    authorityCertIssuer       [1] GeneralNames            OPTIONAL,
     *    authorityCertSerialNumber [2] CertificateSerialNumber OPTIONAL
     * }
     * KeyIdentifier ::= OCTET STRING
     */
    private class AuthorityKeyIdentifier
    {
        /**
         * Create an AuthorityKeyIdentifier from the byte array from the
         * signature file.
         * 
         * @param val
         *            DER value to parse.
         * 
         * @exception IOException
         *                on error.
         */
        AuthorityKeyIdentifier(DerValue val) throws IOException
        {
            if (val.tag != DerValue.tag_Sequence)
                throw new IOException("AuthorityKeyIdentifier is not a proper DER sequence.");

            // MHP & OCAP differ on requirements for what fields in
            // in the AuthorityKeyIdentifier must be present. MHP considers
            // the key identifier as optional and OCAP considers the cert issuer
            // and cert serial number as optional. The code below handles
            // these options appropriately and any fields not present will be
            // null.
            while ((val.data != null) && (val.data.available() != 0))
            {
                DerValue field = val.data.getDerValue();

                if (field.isContextSpecific((byte) 0) && !field.isConstructed())
                {
                    if (keyId != null) throw new IOException("More than one KeyIdentifier in AuthorityKeyIdentifier.");
                    field.resetTag(DerValue.tag_OctetString);
                    keyId = field.getOctetString();
                }
                else if (field.isContextSpecific((byte) 1) && field.isConstructed())
                {
                    if (certIssuer != null)
                        throw new IOException("More than one set of GeneralNames in AuthorityKeyIdentifier.");
                    field.resetTag(DerValue.tag_Sequence);
                    try
                    {
                        certIssuer = new GeneralNames(field);
                    }
                    catch (Exception e)
                    {
                        throw new IOException(e.toString());
                    }
                }
                else if (field.isContextSpecific((byte) 2) && !field.isConstructed())
                {
                    if (certSerial != null)
                        throw new IOException("More than one SerialNumber in AuthorityKeyIdentifier.");
                    field.resetTag(DerValue.tag_Integer);

                    certSerial = new SerialNumber(field);
                    break;
                }
                else
                    throw new IOException("Invalid AuthorityKeyIdentifierExtension found in signature file.");
            }
        }

        /**
         * Get the <code>keyIdentifier</code> used by OCAP.
         * 
         * @return byte[] containing the keyIdentifier.
         */
        byte[] getKeyId()
        {
            return keyId;
        }

        /**
         * Get the <code>authorityCertIssuer</code> directory name, which must
         * be present in GEM signed signature files.
         * 
         * @return String representing the directory name or null.
         */
        String getCertIssuerDirectoryName()
        {
            if (certIssuer == null) return null;

            for (Iterator i = certIssuer.iterator(); i.hasNext();)
            {
                GeneralName name = (GeneralName) i.next();
                if (name.getType() == GeneralNameInterface.NAME_DIRECTORY) return name.toString();
            }

            return null;
        }

        public String toString()
        {
            String s = super.toString() + "AuthorityKeyIdentifier: \n    ";
            /* if ( keyId != null) s += keyId.toString() + "\n"; */
            if (keyId != null) s += Arrays.toString(keyId) + "\n";
            if (certIssuer != null) s += certIssuer.toString() + "\n";
            if (certSerial != null) s += certSerial.toString() + "\n";
            return s;
        }

        // keyIdentifier.
        private byte[] keyId = null;

        // authorityCertIssuer
        private GeneralNames certIssuer = null;

        // authorityCertSerialNumber
        private SerialNumber certSerial = null;

    }

    private AuthorityKeyIdentifier aki = null;

    // The hash algorithm used.
    private String sigHashAlgo = null;

    private final String md5 = "1.2.840.113549.2.5";

    private final String sha1 = "1.3.14.3.2.26";

    // The signature value.
    private byte[] signature = null;

}
