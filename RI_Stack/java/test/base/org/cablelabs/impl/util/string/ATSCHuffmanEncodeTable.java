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

package org.cablelabs.impl.util.string;

import java.io.*;
import java.util.Vector;

/**
 * 
 * The Huffman encoding tree. This class is only a data structure.<BR>
 * The constructor needs a file description of the tree. Here is the file format
 * : each line must be like this :
 * 
 * <pre>
 * Prior Symbol: SSS  Symbol: SSS   Code: 110110101
 * </pre>
 * 
 * where <code>SSS</code> is a decimal number (27) or a letter ('A').
 * 
 * The file has to describe a Standard Huffman Encode Table, as specified in
 * Annex C of ATSC A/65 doc.
 * 
 * <p>
 * Revision information:<br>
 * $Revision: 1.4 $
 * 
 */

public class ATSCHuffmanEncodeTable
{

    /** Data structure that associate a code to a symbol (= a character). */

    private static class ATSCHuffmanEncodeTranslation
    {

        /** The symbol to encode. */

        byte symbol;

        /**
         * The code of the combination of <code>symbol</code> and a specific
         * prior symbol. This is a <code>String</code> of "0" and "1".
         */

        String code;

    }

    /**
     * The encoding table : first index is the prior symbol (0 to 127), second
     * index corresponds to an array of <code>Translation</code>s.
     */

    private ATSCHuffmanEncodeTranslation[][] table;

    /**
     * Creates an <code>ATSCHuffmanEncodeTable</code> from a file. The file
     * shall contain the data for the decoding tree as they are in the A/65
     * spec. NB: this constructor is not verbose, nothing is written to
     * <code>System.out</code>.
     * 
     * @param fileName
     *            the name of the source file. If it doesn't starts with '/'
     *            (relative path), then the data is found using
     *            <code>Class.getResourceAsStream()</code>.
     */

    public ATSCHuffmanEncodeTable(String fileName) throws java.io.IOException
    {

        private_ATSCHuffmanEncodeTable(fileName, false);

    }

    /**
     * Creates an <code>ATSCHuffmanEncodeTable</code> from a file. The file
     * shall contain the data for the decoding tree as they are in the A/65
     * spec.
     * 
     * @param fileName
     *            the name of the source file. If it doesn't starts with '/'
     *            (relative path), then the data is found using
     *            <code>Class.getResourceAsStream()</code>.
     * @param verbose
     *            if true, the tree and many other informations are dumped on
     *            the standard output as the file is read.
     */

    public ATSCHuffmanEncodeTable(String fileName, boolean verbose) throws java.io.IOException
    {

        private_ATSCHuffmanEncodeTable(fileName, verbose);

    }

    /** This is the implementation of the constructor. */

    private void private_ATSCHuffmanEncodeTable(String fileName, boolean verbose) throws java.io.IOException
    {

        int i, j;
        String s;
        if (verbose)
        {
            System.out.println("Decoding ATSCHuffmanEncodeTable from file \"" + fileName + "\"");
        }

        InputStream is = getClass().getResourceAsStream(fileName);
        if (is == null)
        {
            is = getClass().getResourceAsStream("/" + fileName);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(is));

        Vector priorSymb = new Vector(1000, 1000); // Vector of Bytes
        Vector currentSymb = new Vector(1000, 1000); // Vector of Bytes
        Vector code = new Vector(1000, 1000); // Vector of Strings
        String priorSymbString, currentSymbString, codeString;

        /* First read the data */

        while ((s = in.readLine()) != null)
        {

            /* Pick up the information */

            priorSymbString = s.substring(14, 17);
            currentSymbString = s.substring(27, 30);
            codeString = s.substring(39);

            /* Decode it & put it into raw arrays */

            priorSymb.addElement(decodeSymbString(priorSymbString));
            currentSymb.addElement(decodeSymbString(currentSymbString));
            code.addElement(codeString);

            if (verbose)
            {
                System.out.println("[ATSCHuffmanEncodeTable] Line " + priorSymb.size() + " :  "
                        + priorSymb.lastElement() + " " + currentSymb.lastElement() + " " + code.lastElement());
            }
        }
        in.close();

        /* Then decode it */

        /* 1) Allocate space */

        int[] space = new int[128];
        for (i = 0; i < priorSymb.size(); i++)
        {
            space[((Byte) (priorSymb.elementAt(i))).byteValue()]++;
        }
        table = new ATSCHuffmanEncodeTranslation[128][];
        for (i = 0; i < 128; i++)
        {
            table[i] = new ATSCHuffmanEncodeTranslation[space[i]];
            if (verbose)
            {
                System.out.println("[ATSCHuffmanEncodeTable] space[" + i + "] == " + space[i] + "  table[" + i
                        + "].length == " + table[i].length);
            }
        }

        /* 2) Fill the space with decoded date */

        int[] index = new int[128];
        for (i = 0; i < priorSymb.size(); i++)
        {
            j = ((Byte) (priorSymb.elementAt(i))).byteValue();
            table[j][index[j]] = new ATSCHuffmanEncodeTranslation();
            table[j][index[j]].symbol = ((Byte) (currentSymb.elementAt(i))).byteValue();
            table[j][index[j]].code = (String) (code.elementAt(i));
            if (verbose)
            {
                System.out.println("[ATSCHuffmanEncodeTable] i=" + i + " index[" + j + "] = " + index[j] + " : "
                        + table[j][index[j]].symbol + " " + table[j][index[j]].code);
            }
            index[j]++;
        }

    } // private void private_ATSCHuffmanEncodeTable(...)

    /**
     * Decodes a symbol in a String. 2 possibilities : 'A' or 65.
     * 
     * @param s
     *            the symbol to be decoded.
     * @return the corresponding value of the symbol (from 0 to 127).
     */

    private static Byte decodeSymbString(String s)
    {

        s = s.trim();
        if (s.charAt(0) == '\'')
        { // 'A'
            return new Byte((byte) (s.charAt(1)));
        }
        else
        { // 65
            return Byte.valueOf(s);
        }

    } // private static Byte decodeSymbString(String s)

    /**
     * Access to the code that encodes a (previous symbol, current symbol)
     * couple.
     * 
     * @param prevSymbol
     *            the previous symbol that has been encoded (0 if starting).
     *            Range is 0-127.
     * @param currentSymbol
     *            the symbol to encode. Range is 0-127.
     * @return the code (a <code>String</code> of '0' and '1'),
     *         <code>null</code> if this couple doesn't appear in the table.
     */

    public String getCode(byte prevSymbol, byte currentSymbol)
    {

        if (prevSymbol < 0 || currentSymbol < 0)
        { // check the range (0-127)
            return null;
        }

        for (int i = 0; i < table[prevSymbol].length; i++)
        {
            if (table[prevSymbol][i].symbol == currentSymbol)
            {
                return table[prevSymbol][i].code;
            }
        }

        return null;
    } // public String getCode(...)

} // public class ATSCHuffmanEncodeTable
