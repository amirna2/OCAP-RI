package org.cablelabs.impl.util;

/**************************************************************************
    Copyright 2005, Lexonics, Inc.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the
       distribution.
    3. The names of the copyright holders may not be used to endorse or
       promote products derived from this software without specific
       prior written permission.

    THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED
    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
    INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
    STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
    IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
 ***************************************************************************/

/**
    A class containing static methods for base64 encoding and decoding.
    <p>
    It provides two variants of encoding and decoding methods.
    One variant is compatible with the undocumented methods
    <code>sun.misc.BASE64Encoder.encodeBuffer</code>
    and <code>sun.misc.BASE64Decoder.decodeBuffer</code>,
    in that a CR/LF pair follows every 76 characters of the encoded string.
    The other variant, suitable in some applications,
    omits the CR/LF pairs.
    <p>
    Some timing results indicate that the Sun-compatible <code>encodeBuffer</code> method
    in this class is about 6 times faster than the Sun <code>encodeBuffer</code> method
    (though its memory requirements are higher).
    They also indicate that the Sun-compatible <code>decodeBuffer</code> method
    in this class is about 2.5 times faster than the Sun <code>decodeBuffer</code> method
    (and its memory requirements are lower).
 */

public final class Base64Codec
{
    private static final String  emptyString    = "";
    private static final byte [] emptyByteArray = {};

    private static final char [] encode =
    {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'
    };

    // assert encode.length = 64

    private static final byte [] decode =
    {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
        -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };

    // assert decode.length = 123

    private Base64Codec ()
    {
    }

    private static byte decode (char c)
    {
        byte result = c < 123 ? decode [c] : -1;

        if (result < 0)
        {
            throw new IllegalArgumentException ();
        }

        return result;
    }

    /**
        Base64 encode a byte array, omitting CR/LF pairs.
        @param ba The byte array;
        must not be null.
        @return The base64 encoding of the byte array,
        expressed as a String with CR/LF pairs omitted.
     */

    public static String encode (byte [] ba)
    {
        return encode (ba, false);
    }

    /**
        Base64 encode a byte array, including CR/LF pairs.
        @param ba The byte array;
        must not be null.
        @return The base64 encoding of the byte array,
        expressed as a String with CR/LF pairs included.
     */

    public static String encodeBuffer (byte [] ba)
    {
        return encode (ba, true);
    }

    /**
        Base64 encode a byte array, either including or omitting CR/LF pairs.
        @param ba The byte array;
        must not be null.
        @param lineTerm True if CR/LF pairs are to be included, false if they are to be omitted.
        @return The base64 encoding of the byte array,
        expressed as a String with CR/LF pairs included or omitted as specified.
     */

    private static String encode (byte [] ba, boolean lineTerm)
    {
        int m = ba.length;

        if (m == 0)
        {
            return emptyString;
        }

        int n = ((m / 3) << 2) +
                (
                        (m % 3) == 0    ?    0
                    :                        4
                );

        if (lineTerm)
        {
            n += ((n + 75) / 76) << 1;
        }

        char [] ca = new char [n];

        int j = 0, i = 0;

        if (lineTerm)
        {
            while (j < m - 56)
            {
                for (int t = 0; t < 19; ++ t)
                {
                    byte b1 = ba [j];
                    byte b2 = ba [j + 1];
                    byte b3 = ba [j + 2];

                    ca [i]     = encode [(b1 >> 2) & 63];
                    ca [i + 1] = encode [(((b1 &  3)    << 4)) + (((b2 >> 4)) & 15)];
                    ca [i + 2] = encode [(((b2 & 15)    << 2)) + (((b3 >> 6)) &  3)];
                    ca [i + 3] = encode [b3 & 63];

                    j += 3;
                    i += 4;
                }

                ca [i]     = '\r';
                ca [i + 1] = '\n';

                i += 2;
            }

            lineTerm = j < m;
        }

        while (j < m - 2)
        {
            byte b1 = ba [j];
            byte b2 = ba [j + 1];
            byte b3 = ba [j + 2];

            ca [i]     = encode [(b1 >> 2) & 63];
            ca [i + 1] = encode [(((b1 &  3)    << 4)) + (((b2 >> 4)) & 15)];
            ca [i + 2] = encode [(((b2 & 15)    << 2)) + (((b3 >> 6)) &  3)];
            ca [i + 3] = encode [b3 & 63];

            j += 3;
            i += 4;
        }

        if (j < m)
        {
            byte b1 = ba [j];

            ca [i]     = encode [(b1 >> 2) & 63];
            ca [i + 3] = '=';

            if (j < m - 1)
            {
                byte b2 = ba [j + 1];

                ca [i + 1] = encode [(((b1 & 3)    << 4)) + (((b2 >> 4)) & 15)];
                ca [i + 2] = encode [(b2 & 15)     << 2];
            }
            else
            {
                ca [i + 1] = encode [(b1 &  3)     << 4];
                ca [i + 2] = '=';
            }

            i += 4;
        }

        if (lineTerm)
        {
            ca [i]     = '\r';
            ca [i + 1] = '\n';
        }

        return new String (ca);
    }

    /**
        Decode a base64 encoding,
        expressed as a String with CR/LF pairs omitted.
        @param s The base64 encoding, with CR/LF pairs omitted;
        must not be null.
        @return The decoding of the base64 encoding.
        @throws IllegalArgumentException The String does not express
        a valid base64 encoding.
     */

    public static byte [] decode (String s)
    {
        return decode (s, false);
    }

    /**
        Decode a base64 encoding,
        expressed as a String with CR/LF pairs included.
        @param s The base64 encoding, with CR/LF pairs included;
        must not be null.
        @return The decoding of the base64 encoding.
        @throws IllegalArgumentException The String does not express
        a valid base64 encoding.
     */

    public static byte [] decodeBuffer (String s)
    {
        return decode (s, true);
    }

    /**
        Decode a base64 encoding,
        expressed as a String with CR/LF pairs included or omitted as specified.
        @param s The base64 encoding, with CR/LF pairs included or omitted as specified;
        must not be null.
        @param lineTerm True if CR/LF pairs are included, false if they are omitted.
        @return The decoding of the base64 encoding.
        @throws IllegalArgumentException The String does not express
        a valid base64 encoding.
     */

    private static byte [] decode (String s, boolean lineTerm)
    {
        char [] ca = s.toCharArray ();

        if (lineTerm)
        {
            int n = ca.length;

            int z = 0;

            for (int i = 0; i < n; ++ i)
            {
                char c = ca [i];

                if (c == '\r' || c == '\n')
                {
                    ++ z;
                }
            }

            char [] da = new char [n - z];

            for (int i = 0, j = 0; i < n; ++ i)
            {
                char c = ca [i];

                if (c != '\r' && c != '\n')
                {
                    da [j] = c;
                    ++ j;
                }
            }

            ca = da;
        }

        int n = ca.length;

        if (n == 0)
        {
            return emptyByteArray;
        }

        if ((n & 3) != 0)
        {
            throw new IllegalArgumentException ();
        }

        int m = 3 * (n >> 2) -
                (
                        ca [n - 1] != '='    ?    0
                    :   ca [n - 2] != '='    ?    1
                    :                             2
                );

        byte [] ba = new byte [m];

        int i = 0, j = 0;

        while (i < n - 4)
        {
            byte b1 = decode (ca [i]);
            byte b2 = decode (ca [i + 1]);
            byte b3 = decode (ca [i + 2]);
            byte b4 = decode (ca [i + 3]);

            ba [j]     = (byte) ((b1           << 2) + (b2 >> 4));
            ba [j + 1] = (byte) (((b2 & 15)    << 4) + (b3 >> 2));
            ba [j + 2] = (byte) (((b3 & 3)     << 6) + b4);

            i += 4;
            j += 3;
        }

        byte b1 = decode (ca [i]);
        byte b2 = decode (ca [i + 1]);

        ba [j] = (byte) ((b1 << 2) + (b2 >> 4));

        if (ca [n - 1] != '=')
        {
            byte b3 = decode (ca [i + 2]);
            byte b4 = decode (ca [i + 3]);

            ba [j + 1] = (byte) (((b2 & 15)    << 4) + (b3 >> 2));
            ba [j + 2] = (byte) (((b3 & 3)     << 6) + b4);
        }
        else if (ca [n - 2] != '=')
        {
            byte b3 = decode (ca [i + 2]);

            if ((b3 & 3) != 0)
            {
                throw new IllegalArgumentException ();
            }

            ba [j + 1] = (byte) (((b2 & 15)    << 4) + (b3 >> 2));
        }
        else
        {
            if ((b2 & 15) != 0)
            {
                throw new IllegalArgumentException ();
            }
        }

        return ba;
    }
}
