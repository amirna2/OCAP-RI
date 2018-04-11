package javax.crypto.interfaces;

// ----------------------------------------------------------------------------
// $Id: PBEKey.java,v 1.3 2003/02/23 06:50:00 raif Exp $
//
// Copyright (C) 2001, 2002, 2003, Free Software Foundation, Inc.
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

import javax.crypto.SecretKey;

/**
 * An interface to a key used in <i>password-based encryption</i> (<b>PBE</b>).
 * Typically, keys that are known to be derived from passwords can be cast to
 * instances of a <code>PBEKey</code>.
 *
 * @see SecretKey
 * @see javax.crypto.spec.PBEKeySpec
 * @since 1.4
 * @version $Revision: 33112 $
 */
public interface PBEKey extends SecretKey {

   // Constants
   // ------------------------------------------------------------------------

   // Methods
   // ------------------------------------------------------------------------

   /**
    * Returns the iteration count, or 0 if not specified.
    *
    * @return the iteration count.
    */
   int getIterationCount();

   /**
    * <p>Returns the password.</p>
    *
    * <p>Note: this method should return a copy of the password. It is the
    * caller's responsibility to zero out the password information after it is
    * no longer needed.</p>
    *
    * @return the password.
    */
   char[] getPassword();

   /**
    * <p>Returns the salt or null if not specified.</p>
    *
    * <p>Note: this method should return a copy of the salt. It is the caller's
    * responsibility to zero out the salt information after it is no longer
    * needed.</p>
    *
    * @return the salt.
    */
   byte[] getSalt();
}
