/*
 *         NOTICE OF SOFTWARE ACKNOWLEDGMENT AND REDISTRIBUTION 
 *
 * The software (named NDRI, for NIST/DASE API RI) provided herein is released by 
 * the National Institute of Standards and Technology (NIST), an agency of the 
 * U.S. Department of Commerce, Gaithersburg MD 20899, USA. The software presented
 * here is intended to be utilized for research purposes only and bear no warranty,
 * either express or implied. NIST does not assume legal liability nor 
 * responsibility for a USER's use of a NIST-derived software product or the 
 * results of such use. 
 *
 * Please note that within the United States, copyright protection, under Section 
 * 105 of the United States Code, Title 17, is not available for any work of the 
 * United States Government and/or for any works created by United States 
 * Government employees. USER acknowledges that this software contains work which 
 * was created by NIST employees and is therefore in the public domain and is not
 * subject to copyright. The USER may use, distribute, or incorporate this code or
 * any part of it provided the USER acknowledges this via an explicit 
 * acknowledgment of NIST-related contributions to the USER's work. USER also 
 * agrees to acknowledge, via an explicit acknowledgment, that modifications or 
 * alterations have been made to this software by USER before redistribution. 
 */

package org.cablelabs.ocap.util.string;

/**
 * 
 * This class implements encoding of Huffman-compressed Multiple String
 * Structures (cf. A/65 spec).
 * 
 * <p>
 * Revision information:<br>
 * $Revision: 1.3 $
 * 
 */

public class ATSCHuffmanEncoder
{

    /**
     * Creates an ATSCHuffmanEncoder that will encode <code>String</code>s
     * arrays into Huffman-compressed raw byte arrays.
     * 
     * @param s
     *            the <code>String</code> array to encode
     * @param het
     *            the reference encoding table
     * @return the coded form of <code>s</code>.
     */

    public byte[] encode(String[] s, ATSCHuffmanEncodeTable het)
    {

        int i, j, k;

        /* 1st : build a binary representation of the coded String array */

        StringBuffer bin = new StringBuffer(1000);

        byte[] string = null;
        String saux;
        byte prevSymbol, currentSymbol;
        prevSymbol = 0;

        for (i = 0; i < s.length; i++)
        {
            try
            {
                string = s[i].getBytes("8859_1");
            }
            catch (java.io.UnsupportedEncodingException e)
            {
                System.err.println(e);
                return null;
            }

            for (j = 0; j <= string.length; j++)
            {

                /* Add a null character at the end of each string */

                if (j < string.length)
                {
                    currentSymbol = string[j];
                }
                else
                {
                    currentSymbol = 0;
                }

                if ((prevSymbol < 0) || (prevSymbol == 27))
                {

                    /* A) previous symbol was a (128...255) character or 27 */
                    /* -> then put the uncompressed current symbol */

                    saux = Integer.toString((currentSymbol >= 0) ? currentSymbol : 256 + currentSymbol, 2);
                    for (k = 8; k > saux.length(); k--)
                    { // Leading zeros
                        bin.append("0");
                    }
                    bin.append(saux);

                }
                else
                {

                    /* B) previous symbol was a (0..127) character (not 27) */

                    saux = het.getCode(prevSymbol, currentSymbol);

                    if (saux == null)
                    { // 1st case : current char. is uncompressable

                        /* Put an additional compressed ESC character */

                        bin.append(het.getCode(prevSymbol, (byte) (27)));

                        /* Then put the uncompressed character */

                        saux = Integer.toString((currentSymbol >= 0) ? currentSymbol : 256 + currentSymbol, 2);
                        for (k = 8; k > saux.length(); k--)
                        { // Leading zeros
                            bin.append("0");
                        }
                        bin.append(saux);
                    }
                    else
                    { // 2nd case : current char. is compressable
                        bin.append(saux);
                    } // if (saux == null)

                } // if ((prevSymbol < 0) || (prevSymbol == 27))

                prevSymbol = currentSymbol;

            } // for (j=0; j < string.length; j++)

        } // for (i=0; i < s.length; i++)

        /*
         * 2nd : convert the binary representation into a byte array, adding
         * (binary) zeros if needed.
         */

        j = bin.length() % 8;
        if (j != 0)
        { // Add zeros
            for (i = 0; i < 8 - j; i++)
            {
                bin.append("0");
            }
        }

        byte[] result = new byte[bin.length() / 8];
        j = 0;
        for (i = 0; i < bin.length(); i += 8)
        { // Code byte per byte
            result[j++] = (Integer.valueOf(bin.substring(i, i + 8), 2)).byteValue();
        }

        return result;

    } // public byte[] encode(...)

} // public class ATSCHuffmanEncoder
