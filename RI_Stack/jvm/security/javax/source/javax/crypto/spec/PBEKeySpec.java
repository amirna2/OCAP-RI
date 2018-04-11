package javax.crypto.spec;

// ----------------------------------------------------------------------------
// $Id: PBEKeySpec.java,v 1.2 2003/02/23 05:41:11 raif Exp $
//
// Copyright (C) 2003, Free Software Foundation, Inc.
//
// This file is part of GNU Crypto.
//
// GNU Crypto is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2, or (at your option)
// any later version.
//
// GNU Crypto is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    59 Temple Place - Suite 330,
//    Boston, MA 02111-1307
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
// ----------------------------------------------------------------------------

import java.security.spec.KeySpec;

/**
 * <p>A user-chosen password that can be used with password-based encryption
 * (<i>PBE</i>).</p>
 *
 * <p>The password can be viewed as some kind of raw key material, from which
 * the encryption mechanism that uses it derives a cryptographic key.</p>
 *
 * <p>Different PBE mechanisms may consume different bits of each password
 * character. For example, the PBE mechansim defined in <a
 * href="http://www.rsa.com/rsalabs/pubs/PKCS/html/pkcs-5.html">PKCS #5</a>
 * looks at only the low order 8 bits of each character, whereas <a
 * href="http://www.rsa.com/rsalabs/pubs/PKCS/html/pkcs-12.html">PKCS #12</a>
 * looks at all 16 bits of each character.</p>
 *
 * <p>You convert the password characters to a PBE key by creating an instance
 * of the appropriate secret-key factory. For example, a secret-key factory for
 * PKCS #5 will construct a PBE key from only the low order 8 bits of each
 * password character, whereas a secret-key factory for PKCS #12 will take all
 * 16 bits of each character.</p>
 *
 * <p>Also note that this class stores passwords as char arrays instead of
 * {@link String} objects (which would seem more logical), because the String
 * class is immutable and there is no way to overwrite its internal value when
 * the password stored in it is no longer needed. Hence, this class requests
 * the password as a char array, so it can be overwritten when done.</p>
 *
 * @see javax.crypto.SecretKeyFactory
 * @see PBEParameterSpec
 * @since 1.4
 * @version $Revision: 33112 $
 */
public class PBEKeySpec implements KeySpec {

   // Constants and variables
   // -------------------------------------------------------------------------

   private char[] password;
   private byte[] salt;
   private int iterationCount;
   private int keyLength;

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * <p>Constructor that takes a password.</p>
    *
    * <p> Note: <code>password</code> is cloned before it is stored in the new
    * <code>PBEKeySpec</code> object.</p>
    *
    * @param password the password.
    */
   public PBEKeySpec(char[] password) {
      super();

      this.password = (char[]) password.clone();
   }

   /**
    * <p>Constructor that takes a password, salt, iteration count, and
    * to-be-derived key length for generating PBEKey of variable-key-size PBE
    * ciphers. An empty char[] is used if null is specified for password.</p>
    *
    * <p>Note: the <code>password</code> and <code>salt</code> are cloned before
    * they are stored in the new <code>PBEKeySpec</code> object.</p>
    *
    * @param password the password.
    * @param salt the salt.
    * @param iterationCount the iteration count.
    * @param keyLength the to-be-derived key length.
    * @exception NullPointerException if salt is null.
    * @exception IllegalArgumentException if salt is empty, i.e. 0-length,
    * iterationCount or keyLength is not positive.
    */
   public
   PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) {
      this(password == null ? new char[0] : password);

      if (salt.length == 0 || iterationCount < 0 || keyLength < 0) {
         throw new IllegalArgumentException();
      }
      this.salt = (byte[]) salt.clone();
   }

   /**
    * <p>Constructor that takes a password, salt, iteration count for
    * generating PBEKey of fixed-key-size PBE ciphers. An empty char[] is used
    * if null is specified for password.</p>
    *
    * <p>Note: the <code>password</code> and <code>salt</code> are cloned before
    * they are stored in the new <code>PBEKeySpec</code> object.</p>
    *
    * @param password the password.
    * @param salt the salt.
    * @param iterationCount the iteration count.
    * @exception NullPointerException if salt is null.
    * @exception IllegalArgumentException if salt is empty, i.e. 0-length, or
    * iterationCount is not positive.
    */
   public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
      this(password == null ? new char[0] : password);

      if (salt.length == 0 || iterationCount < 0) {
         throw new IllegalArgumentException();
      }
      this.salt = (byte[]) salt.clone();
   }

   // Class methods
   // -------------------------------------------------------------------------

   // Instance methods
   // -------------------------------------------------------------------------

   /** Clears the internal copy of the password. */
   public final void clearPassword() {
      password = null;
   }

   /**
    * <p>Returns a copy of the password.</p>
    *
    * <p>Note: this method returns a copy of the password. It is the caller's
    * responsibility to zero out the password information after it is no longer
    * needed.</p>
    *
    * @return the password.
    * @exception IllegalStateException if password has been cleared by calling
    * <code>clearPassword()</code> method.
    */
   public final char[] getPassword() {
      if (password == null) {
         throw new IllegalStateException();
      }
      return (char[]) password.clone();
   }

   /**
    * <p>Returns a copy of the salt or null if not specified.</p>
    *
    * <p>Note: this method returns a copy of the salt. It is the caller's
    * responsibility to zero out the salt information after it is no longer
    * needed.</p>

    * @return the salt.
    */
   public final byte[] getSalt() {
      return salt == null ? null : (byte[]) salt.clone();
   }

   /**
    * Returns the iteration count or 0 if not specified.
    *
    * @return the iteration count.
    */
   public final int getIterationCount() {
      return iterationCount;
   }

   /**
    * <p>Returns the to-be-derived key length or 0 if not specified.</p>
    *
    * <p>Note: this is used to indicate the preference on key length for
    * variable-key-size ciphers. The actual key size depends on each provider's
    * implementation.</p>
    *
    * @return the to-be-derived key length.
    */
   public final int getKeyLength() {
      return keyLength;
   }
}
