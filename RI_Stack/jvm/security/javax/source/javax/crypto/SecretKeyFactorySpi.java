/*
 * Copyright (c) 2000 The Legion Of The Bouncy Castle
 * (http://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */

package javax.crypto;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the <code>SecretKeyFactory</code> class.
 * All the abstract methods in this class must be implemented by each 
 * cryptographic service provider who wishes to supply the implementation
 * of a secret-key factory for a particular algorithm.
 * <p>
 * A provider should document all the key specifications supported by its
 * secret key factory.
 * For example, the DES secret-key factory supplied by the "SunJCE" provider
 * supports <code>DESKeySpec</code> as a transparent representation of DES
 * keys, and that provider's secret-key factory for Triple DES keys supports
 * <code>DESedeKeySpec</code> as a transparent representation of Triple DES
 * keys.
 *
 * @see SecretKey
 * @see javax.crypto.spec.DESKeySpec
 * @see javax.crypto.spec.DESedeKeySpec
 */
public abstract class SecretKeyFactorySpi
{
    public SecretKeyFactorySpi()
    {
    }

    /**
     * Generates a <code>SecretKey</code> object from the
     * provided key specification (key material).
     *
     * @param keySpec the specification (key material) of the secret key
     * @return the secret key
     * @exception InvalidKeySpecException if the given key specification
     * is inappropriate for this secret-key factory to produce a secret key.
     */
    protected abstract SecretKey engineGenerateSecret(
        KeySpec  keySpec)
    throws InvalidKeySpecException;

    /**
     * Returns a specification (key material) of the given key object in the requested format.
     *
     * @param key the key
     * @param keySpec the requested format in which the key material shall be returned
     * @return the underlying key specification (key material) in the requested format
     * @exception InvalidKeySpecException if the requested key specification is inappropriate for
     * the given key (e.g., the algorithms associated with <code>key</code> and <code>keySpec</code> do
     * not match, or <code>key</code> references a key on a cryptographic hardware device whereas
     * <code>keySpec</code> is the specification of a software-based key), or the given key cannot be
     * dealt with (e.g., the given key has an algorithm or format not supported by this secret-key factory).
     */
    protected abstract KeySpec engineGetKeySpec(
        SecretKey   key,
        Class       keySpec)
    throws InvalidKeySpecException;

    /**
     * Translates a key object, whose provider may be unknown or potentially untrusted, into a
     * corresponding key object of this secret-key factory.
     *
     * @param key the key whose provider is unknown or untrusted
     * @return InvalidKeyException if the given key cannot be processed by this secret-key factory.
     */
    protected abstract SecretKey engineTranslateKey(
        SecretKey   key)
    throws InvalidKeyException;
}
