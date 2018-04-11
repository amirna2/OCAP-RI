package javax.crypto;

// ----------------------------------------------------------------------------
// $Id: ExemptionMechanism.java,v 1.2 2003/02/23 05:31:34 raif Exp $
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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;

/**
 * <p>This class provides the functionality of an exemption mechanism, examples
 * of which are key recovery, key weakening, and key escrow.</p>
 *
 * <p>Applications or applets that use an exemption mechanism may be granted
 * stronger encryption capabilities than those which don't.</p>
 *
 * <p><b>IMPLEMENTATION NOTE</b>: This implementation does not support exemption
 * mechanisms. As a result, all invocations to the declared methods throw one of
 * exceptions declared in their signature.</p>
 *
 * @since 1.4
 * @version $Revision: 33112 $
 */
public class ExemptionMechanism {

   // Constants and variables
   // -------------------------------------------------------------------------

   private ExemptionMechanismSpi delegate;
   private Provider provider;
   private String mechanism;

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Creates a ExemptionMechanism object.
    *
    * @param exmechSpi the delegate.
    * @param provider the provider.
    * @param mechanism the exemption mechanism.
    */
   protected ExemptionMechanism(ExemptionMechanismSpi exmechSpi,
                                Provider provider, String mechanism) {
      super();

      this.delegate = exmechSpi;
      this.provider = provider;
      this.mechanism = mechanism;
   }

   // Class methods
   // -------------------------------------------------------------------------

   /**
    * <p>Generates a ExemptionMechanism object that implements the specified
    * exemption mechanism. If the default provider package provides an
    * implementation of the requested exemption mechanism, an instance of
    * ExemptionMechanism containing that implementation is returned. If the
    * exemption mechanism is not available in the default provider package,
    * other provider packages are searched.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws a <code>
    * NoSuchAlgorithmException</code>.</p>
    *
    * @param mechanism the standard name of the requested exemption mechanism.
    * See Appendix A in the Java Cryptography Extension Reference Guide for
    * information about standard exemption mechanism names.
    * @return the new ExemptionMechanism object
    * @throws NoSuchAlgorithmException if the specified exemption mechanism is
    * not available in the default provider package or any of the other provider
    * packages that were searched.
    */
   public static final ExemptionMechanism getInstance(String mechanism)
   throws NoSuchAlgorithmException {
      throw new NoSuchAlgorithmException();
   }

   /**
    * <p>Generates a ExemptionMechanism object for the specified exemption
    * mechanism from the specified provider.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws a <code>
    * NoSuchAlgorithmException</code>, except when the designated <code>provider
    * </code> is null, in which case it throws an <code>IllegalArgumentException
    * </code>.</p>
    *
    * @param mechanism the standard name of the requested exemption mechanism.
    * See Appendix A in the Java Cryptography Extension Reference Guide for
    * information about standard exemption mechanism names.
    * @param provider the name of the provider.
    * @return the new ExemptionMechanism object.
    * @throws NoSuchAlgorithmException if the specified exemption mechanism is
    * not available from the specified provider.
    * @throws NoSuchProviderException if the specified provider has not been
    * configured.
    * @throws IllegalArgumentException if the provider is null.
    */
   public static final ExemptionMechanism
   getInstance(String mechanism, String provider)
   throws NoSuchAlgorithmException, NoSuchProviderException {
      if (provider == null) {
         throw new IllegalArgumentException();
      }
      throw new NoSuchAlgorithmException();
   }

   /**
    * <p>Generates a ExemptionMechanism object for the specified exemption
    * mechanism from the specified provider. Note: the provider doesn't have to
    * be registered.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws a <code>
    * NoSuchAlgorithmException</code>, except when the designated <code>provider
    * </code> is null, in which case it throws an <code>IllegalArgumentException
    * </code>.</p>
    *
    * @param mechanism the standard name of the requested exemption mechanism.
    * See Appendix A in the Java Cryptography Extension Reference Guide for
    * information about standard exemption mechanism names.
    * @param provider the provider.
    * @return the new ExemptionMechanism object.
    * @throws NoSuchAlgorithmException if the specified exemption mechanism is
    * not available from the specified provider.
    * @throws IllegalArgumentException if the provider is null.
    */
   public static final ExemptionMechanism
   getInstance(String mechanism, Provider provider)
   throws NoSuchAlgorithmException {
      if (provider == null) {
         throw new IllegalArgumentException();
      }
      throw new NoSuchAlgorithmException();
   }

   // Instance methods
   // -------------------------------------------------------------------------

   /**
    * <p>Returns the exemption mechanism name of this ExemptionMechanism object.
    * </p>
    *
    * <p>This is the same name that was specified in one of the
    * <code>getInstance()</code> calls that created this ExemptionMechanism
    * object.
    *
    * @return the exemption mechanism name of this ExemptionMechanism object.
    */
   public final String getName() {
      return mechanism;
   }

   /**
    * Returns the provider of this ExemptionMechanism object.
    *
    * @return the provider of this ExemptionMechanism object.
    */
   public final Provider getProvider() {
      return provider;
   }

   /**
    * <p>Returns whether the result blob has been generated successfully by this
    * exemption mechanism.</p>
    *
    * <p>The method also makes sure that the key passed in is the same as the
    * one this exemption mechanism used in initializing and generating phases.
    * </p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws a <code>
    * ExemptionMechanismException</code></p>
    *
    * @param key the key the crypto is going to use.
    * @return whether the result blob of the same key has been generated
    * successfully by this exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered while
    * determining whether the result blob has been generated successfully by
    * this exemption mechanism object.
    */
   public final boolean isCryptoAllowed(Key key)
   throws ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

   /**
    * <p>Returns the length in bytes that an output buffer would need to be in
    * order to hold the result of the next genExemptionBlob operation, given the
    * input length inputLen (in bytes).</p>
    *
    * <p>The actual output length of the next <code>genExemptionBlob()</code>
    * call may be smaller than the length returned by this method.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * IllegalStateException</code></p>
    *
    * @param inputLen the input length (in bytes).
    * @return the required output buffer size (in bytes).
    * @throws IllegalStateException if this exemption mechanism is in a wrong
    * state (e.g., has not yet been initialized).

    */
   public final int getOutputSize(int inputLen) throws IllegalStateException {
      throw new IllegalStateException();
   }

   /**
    * <p>Initializes this exemption mechanism with a key.</p>
    *
    * <p>If this exemption mechanism requires any algorithm parameters that
    * cannot be derived from the given key, the underlying exemption mechanism
    * implementation is supposed to generate the required parameters itself
    * (using provider-specific default values); in the case that algorithm
    * parameters must be specified by the caller, an InvalidKeyException is
    * raised.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>
    *
    * @param key the key for this exemption mechanism.
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   public final void init(Key key)
   throws InvalidKeyException, ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

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
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>
    *
    * @param key the key for this exemption mechanism.
    * @param params the algorithm parameters.
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws InvalidAlgorithmParameterException if the given algorithm
    * parameters are inappropriate for this exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   public final void init(Key key, AlgorithmParameterSpec params)
   throws InvalidKeyException, InvalidAlgorithmParameterException,
         ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

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
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>

    * @param key the key for this exemption mechanism.
    * @param params the algorithm parameters.
    * @throws InvalidKeyException if the given key is inappropriate for this
    * exemption mechanism.
    * @throws InvalidAlgorithmParameterException if the given algorithm
    * parameters are inappropriate for this exemption mechanism.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of initializing.
    */
   public final void init(Key key, AlgorithmParameters params)
   throws InvalidKeyException, InvalidAlgorithmParameterException,
         ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

   /**
    * <p>Generates the exemption mechanism key blob.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>
    *
    * @return the new buffer with the result key blob.
    * @throws IllegalStateException if this exemption mechanism is in a wrong
    * state (e.g., has not been initialized).
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of generating.
    */
   public final byte[] genExemptionBlob()
   throws IllegalStateException, ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

   /**
    * <p>Generates the exemption mechanism key blob, and stores the result in
    * the output buffer.</p>
    *
    * <p>If the output buffer is too small to hold the result, a {@link
    * ShortBufferException} is thrown. In this case, repeat this call with a
    * larger output buffer. Use <code>getOutputSize()</code> to determine how
    * big the output buffer should be.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>
    *
    * @param output the buffer for the result.
    * @return the number of bytes stored in output.
    * @throws IllegalStateException if this exemption mechanism is in a wrong
    * state (e.g., has not been initialized).
    * @throws ShortBufferException if the given output buffer is too small to
    * hold the result.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of generating.
    */
   public final int genExemptionBlob(byte[] output)
   throws IllegalStateException, ShortBufferException,
         ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

   /**
    * <p>Generates the exemption mechanism key blob, and stores the result in
    * the <code>output</code> buffer, starting at <code>outputOffset</code>
    * inclusive.</p>
    *
    * <p>If the output buffer is too small to hold the result, a {@link
    * ShortBufferException} is thrown. In this case, repeat this call with a
    * larger output buffer. Use <code>getOutputSize()</code> to determine how
    * big the output buffer should be.</p>
    *
    * <p><b>IMPLEMENTATION NOTE</b>: This implementation always throws an <code>
    * ExemptionMechanismException</code></p>
    *
    * @param output the buffer for the result.
    * @param outputOffset the offset in output where the result is stored.
    * @return the number of bytes stored in output
    * @throws IllegalStateException if this exemption mechanism is in a wrong
    * state (e.g., has not been initialized).
    * @throws ShortBufferException if the given output buffer is too small to
    * hold the result.
    * @throws ExemptionMechanismException if problem(s) encountered in the
    * process of generating.
    */
   public final int genExemptionBlob(byte[] output, int outputOffset)
   throws IllegalStateException, ShortBufferException,
         ExemptionMechanismException {
      throw new ExemptionMechanismException();
   }

   // over-ridden Object method(s)
   // -------------------------------------------------------------------------

   /**
    * Ensures that the key stored away by this ExemptionMechanism object will be
    * wiped out when there are no more references to it.
    */
   protected void finalize() {
      try {
         super.finalize();
      } catch (Throwable x) {
         //TODO: RI - commented out
         //x.printStackTrace(System.err);
      }
   }
}
