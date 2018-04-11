package javax.crypto;

// ----------------------------------------------------------------------------
// $Id: ExemptionMechanismSpi.java,v 1.1 2003/02/23 05:29:35 raif Exp $
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

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>) for the
 * {@link ExemptionMechanism} class. All the abstract methods in this class must
 * be implemented by each cryptographic service provider who wishes to supply
 * the implementation of a particular exemption mechanism.
 *
 * @since 1.4
 * @version $Revision: 33112 $
 */
public abstract class ExemptionMechanismSpi {

   // Constants and variables
   // -------------------------------------------------------------------------

   // Constructor(s)
   // -------------------------------------------------------------------------

   // default ctor

   // Class methods
   // -------------------------------------------------------------------------

   // Instance methods
   // -------------------------------------------------------------------------

   /**
    * <p>Returns the length in bytes that an output buffer would need to be in
    * order to hold the result of the next <code>engineGenExemptionBlob()</code>
    * operation, given the input length inputLen (in bytes).</p>
    *
    * <p>The actual output length of the next <code>engineGenExemptionBlob()</code>
    * call may be smaller than the length returned by this method.</p>
    *
    * @param inputLen the input length (in bytes)
    * @return the required output buffer size (in bytes)
    */
   protected abstract int engineGetOutputSize(int inputLen);

   /**
    * <p>Initializes this exemption mechanism with a key.</p>
    *
    * <p>If this exemption mechanism requires any algorithm parameters that
    * cannot be derived from the given key, the underlying exemption mechanism
    * implementation is supposed to generate the required parameters itself
    * (using provider-specific default values); in the case that algorithm
    * parameters must be specified by the caller, an {@link InvalidKeyException}
    * is raised.</p>
    *
    * @param key the key for this exemption mechanism
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   protected abstract void engineInit(Key key)
   throws InvalidKeyException, ExemptionMechanismException;

   /**
    * <p>Initializes this exemption mechanism with a key and a set of algorithm
    * parameters.</p>
    *
    * <p>If this exemption mechanism requires any algorithm parameters and
    * params is null, the underlying exemption mechanism implementation is
    * supposed to generate the required parameters itself (using provider-
    * specific default values); in the case that algorithm parameters must be
    * specified by the caller, an {@link InvalidAlgorithmParameterException} is
    * raised.</p>

    * @param key the key for this exemption mechanism.
    * @param params the algorithm parameters.
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws InvalidAlgorithmParameterException if the given algorithm
    * parameters are inappropriate for this exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
   throws InvalidKeyException, InvalidAlgorithmParameterException,
         ExemptionMechanismException;

   /**
    * <p>Initializes this exemption mechanism with a key and a set of algorithm
    * parameters.</p>
    *
    * <p>If this exemption mechanism requires any algorithm parameters and
    * params is null, the underlying exemption mechanism implementation is
    * supposed to generate the required parameters itself (using provider-
    * specific default values); in the case that algorithm parameters must be
    * specified by the caller, an {@link InvalidAlgorithmParameterException} is
    * raised.</p>
    *
    * @param key the key for this exemption mechanism.
    * @param params the algorithm parameters
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws InvalidAlgorithmParameterException if the given algorithm
    * parameters are inappropriate for this exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   protected abstract void engineInit(Key key, AlgorithmParameters params)
   throws InvalidKeyException, InvalidAlgorithmParameterException,
         ExemptionMechanismException;

   /**
    * Generates the exemption mechanism key blob.
    *
    * @return the new buffer with the result key blob.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of generating.
    */
   protected abstract byte[] engineGenExemptionBlob()
   throws ExemptionMechanismException;

   /**
    * <p>Generates the exemption mechanism key blob, and stores the result in
    * the output buffer, starting at outputOffset inclusive.</p>
    *
    * <p>If the output buffer is too small to hold the result, a
    * {@link ShortBufferException} is thrown. In this case, repeat this call
    * with a larger output buffer. Use <code>engineGetOutputSize()</code> to
    * determine how big the output buffer should be.</p>

    * @param output the buffer for the result.
    * @param outputOffset the offset in output where the result is stored.
    * @return the number of bytes stored in output
    * @throws ShortBufferException if the given output buffer is too small to
    * hold the result.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of generating.
    */
   protected abstract int engineGenExemptionBlob(byte[] output, int outputOffset)
   throws ShortBufferException, ExemptionMechanismException;
}
