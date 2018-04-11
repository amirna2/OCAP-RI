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

package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * This class specifies an <i>initialization vector</i> (IV). IVs are used
 * by ciphers in feedback mode, e.g., DES in CBC mode.
 */
public class IvParameterSpec
    implements AlgorithmParameterSpec
{
    private byte[]  iv;

    /**
     * Uses the bytes in <code>iv</code> as the IV.
     *
     * @param iv the buffer with the IV
     */
    public IvParameterSpec(
        byte[]  iv)
    {
        if (iv == null)
        {
            throw new IllegalArgumentException("null iv passed");
        }

        this.iv = new byte[iv.length];

        System.arraycopy(iv, 0, this.iv, 0, iv.length);
    }

    /**
     * Uses the first <code>len</code> bytes in <code>iv</code>,
     * beginning at <code>offset</code> inclusive, as the IV.
     * <p>
     * The bytes that constitute the IV are those between
     * <code>iv[offset]</code> and <code>iv[offset+len-1]</code> inclusive.
     *
     * @param iv the buffer with the IV
     * @param offset the offset in <code>iv</code> where the IV starts
     * @param len the number of IV bytes
     */
    public IvParameterSpec(
        byte[]  iv,
        int     offset,
        int     len)
    {
        if (iv == null)
        {
            throw new IllegalArgumentException("Null iv passed");
        }

        if (offset < 0 || len < 0 || (iv.length - offset) < len)
        {
            throw new IllegalArgumentException("Bad offset/len");
        }

        this.iv = new byte[len];

        System.arraycopy(iv, offset, this.iv, 0, len);
    }

    /**
     * Returns the initialization vector (IV).
     *
     * @return the initialization vector (IV)
     */
    public byte[] getIV()
    {
        byte[]  tmp = new byte[iv.length];

        System.arraycopy(iv, 0, tmp, 0, iv.length);
        return tmp;
    }
}
