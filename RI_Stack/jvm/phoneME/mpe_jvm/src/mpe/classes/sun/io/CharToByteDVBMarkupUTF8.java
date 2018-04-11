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

/*
 * The UTF8 implementation in this file carries the following copyright notice:
 * @(#)CharToByteUTF8.java	1.19 06/10/10
 *
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER  
 *   
 * This program is free software; you can redistribute it and/or  
 * modify it under the terms of the GNU General Public License version  
 * 2 only, as published by the Free Software Foundation.   
 *   
 * This program is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * General Public License version 2 for more details (a copy is  
 * included at /legal/license.txt).   
 *   
 * You should have received a copy of the GNU General Public License  
 * version 2 along with this work; if not, write to the Free Software  
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA   
 *   
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa  
 * Clara, CA 95054 or visit www.sun.com if you need additional  
 * information or have any questions. 
 *
 */

package sun.io;

/**
 * UCS2 (UTF16) -> UCS Transformation Format 8 (UTF-8) converter It's
 * represented like below.
 * 
 * # Bits Bit pattern 1 7 0xxxxxxx 2 11 110xxxxx 10xxxxxx 3 16 1110xxxx 10xxxxxx
 * 10xxxxxx 4 21 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx 5 26 111110xx 10xxxxxx
 * 10xxxxxx 10xxxxxx 10xxxxxx 6 31 1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
 * 10xxxxxx
 * 
 * UCS2 uses 1-3 / UTF16 uses 1-4 / UCS4 uses 1-6
 */

public class CharToByteDVBMarkupUTF8 extends CharToByteConverter
{

    private char highHalfZoneCode;

    private int savedSize;

    private char[] savedChars;

    public int flush(byte[] output, int outStart, int outEnd) throws MalformedInputException
    {
        if (highHalfZoneCode != 0)
        {
            highHalfZoneCode = 0;
            badInputLength = 0;
            throw new MalformedInputException();
        }
        byteOff = charOff = 0;
        return 0;
    }

    /**
     * Character conversion
     */
    public int convert(char[] input, int inOff, int inEnd, byte[] output, int outOff, int outEnd)
            throws ConversionBufferFullException, MalformedInputException
    {
        char inputChar;
        byte[] outputByte = new byte[7];
        int inputSize = 0;
        int outputSize = 0;

        charOff = inOff;
        byteOff = outOff;
        int charOffAdjustment = 0;


        if (highHalfZoneCode != 0)
        {
            inputChar = highHalfZoneCode;
            highHalfZoneCode = 0;
            if (input[inOff] >= 0xdc00 && input[inOff] <= 0xdfff)
            {
                // This is legal UTF16 sequence.
                int ucs4 = (highHalfZoneCode - 0xd800) * 0x400 + (input[inOff] - 0xdc00) + 0x10000;
                output[0] = (byte) (0xf0 | ((ucs4 >> 18)) & 0x07);
                output[1] = (byte) (0x80 | ((ucs4 >> 12) & 0x3f));
                output[2] = (byte) (0x80 | ((ucs4 >> 6) & 0x3f));
                output[3] = (byte) (0x80 | (ucs4 & 0x3f));
                charOff++;
                highHalfZoneCode = 0;
            }
            else
            {
                // This is illegal UTF16 sequence.
                badInputLength = 0;
                throw new MalformedInputException();
            }
        }

        if (savedSize != 0)
        {
            char[] newBuf;
            newBuf = new char[inEnd - inOff + savedSize];
            for (int i = 0; i < savedSize; i++)
            {
                newBuf[i] = savedChars[i];
            }
            System.arraycopy(input, inOff, newBuf, savedSize, inEnd - inOff);
            input = newBuf;
            inOff = 0;
            inEnd = newBuf.length;
            charOffAdjustment = -savedSize;
            savedSize = 0;
        }

        boolean bSpecialDVBMarkupEncodingDetected = false;

        while (charOff < inEnd)
        {
            inputChar = input[charOff];
            bSpecialDVBMarkupEncodingDetected = false;

            if (inputChar == 0x001b)
            {
                char char1, char2, char3, char4, char5, char6, char7;
                char1 = inputChar;

                // look ahead to see if the next chars match any of the
                // following
                //
                // start sequences
                // 0x001B 0x0042 0x0000
                // 0x001B 0x0043 0x0004 0x00rr 0x00gg 0x00bb 0x00tt
                //
                // end sequences:
                // 0x001B 0x0062
                // 0x001B 0x0063
                //
                // note that 0x001B 0x0042 0x0000, 0x001B 0x0043 0x0004, 0x001B
                // 0x0062, and 0x001B 0x0063 will all automatically
                // pass through the UTF8 encoding with their values preserved,
                // since that are all ASCII values
                // of less that 127. The only case that needs special treatment
                // is the 0x00rr 0x00gg 0x00bb 0x00tt
                // sequence. This treatment consists of formatting bytes for
                // these chars consisting of just the byte value
                // of the lower byte of the char.
                //
                // NOTE: should other sequences following 0x1b be illegal and be
                // flagged as such? No, the spec says
                // that aside from the markup codes below, bytes are to be
                // encoded as UTF8, so ne need to detect
                // invalid markup codes.

                System.out.println("\n### MATCH 0x1B \n");

                try
                {
                    char2 = input[charOff + 1];
                    if (char2 == 0x0043)
                    {
                        System.out.println("\n### MATCH 0x43 \n");

                        char3 = input[charOff + 2];
                        if (char3 == 0x0004)
                        {
                            System.out.println("\n### MATCH 0x04: FORMATTING BYTES \n");

                            char4 = input[charOff + 3];
                            char5 = input[charOff + 4];
                            char6 = input[charOff + 5];
                            char7 = input[charOff + 6];

                            // if we got this far, then we have enough bytes in
                            // the buffer, and we can format chars here

                            outputByte[0] = (byte) ((short) char1 & 0x00FF);
                            outputByte[1] = (byte) ((short) char2 & 0x00FF);
                            outputByte[2] = (byte) ((short) char3 & 0x00FF);
                            outputByte[3] = (byte) ((short) char4 & 0x00FF);
                            outputByte[4] = (byte) ((short) char5 & 0x00FF);
                            outputByte[5] = (byte) ((short) char6 & 0x00FF);
                            outputByte[6] = (byte) ((short) char7 & 0x00FF);

                            outputSize = 7;
                            inputSize = 7;

                            bSpecialDVBMarkupEncodingDetected = true;
                        }
                        else
                        {
                            System.out.println("\n### NO MATCH 0x04 \n");
                        }
                    }
                    else
                    {
                        System.out.println("\n### NO MATCH 0x43 \n");
                    }
                }
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    System.out.println("\n### ArrayIndexOutOfBoundsException \n");

                    // not enough bytes -- save bytes and continue

                    // GORP: need to implement this -- be sure to put in
                    // analogue of byteOffAdjustment
                    savedSize = inEnd - charOff + 1;
                    for (int i = 0; i < savedSize; i++)
                    {
                        savedChars[i] = input[charOff + i];
                    }
                    break;
                }
            }

            if (bSpecialDVBMarkupEncodingDetected)
            {
            }
            else if (inputChar < 0x80)
            {
                outputByte[0] = (byte) inputChar;
                inputSize = 1;
                outputSize = 1;
            }
            else if (inputChar < 0x800)
            {
                outputByte[0] = (byte) (0xc0 | ((inputChar >> 6) & 0x1f));
                outputByte[1] = (byte) (0x80 | (inputChar & 0x3f));
                inputSize = 1;
                outputSize = 2;
            }
            else if (inputChar >= 0xd800 && inputChar <= 0xdbff)
            {
                // this is <high-half zone code> in UTF-16
                if (charOff + 1 >= inEnd)
                {
                    highHalfZoneCode = inputChar;
                    break;
                }
                // check next char is valid <low-half zone code>
                char lowChar = input[charOff + 1];
                if (lowChar < 0xdc00 || lowChar > 0xdfff)
                {
                    badInputLength = 1;
                    charOff -= charOffAdjustment;
                    throw new MalformedInputException();
                }
                int ucs4 = (inputChar - 0xd800) * 0x400 + (lowChar - 0xdc00) + 0x10000;
                outputByte[0] = (byte) (0xf0 | ((ucs4 >> 18)) & 0x07);
                outputByte[1] = (byte) (0x80 | ((ucs4 >> 12) & 0x3f));
                outputByte[2] = (byte) (0x80 | ((ucs4 >> 6) & 0x3f));
                outputByte[3] = (byte) (0x80 | (ucs4 & 0x3f));
                outputSize = 4;
                inputSize = 2;
            }
            else
            {
                outputByte[0] = (byte) (0xe0 | ((inputChar >> 12)) & 0x0f);
                outputByte[1] = (byte) (0x80 | ((inputChar >> 6) & 0x3f));
                outputByte[2] = (byte) (0x80 | (inputChar & 0x3f));
                inputSize = 1;
                outputSize = 3;
            }
            if (byteOff + outputSize > outEnd)
            {
                throw new ConversionBufferFullException();
            }
            for (int i = 0; i < outputSize; i++)
            {
                output[byteOff++] = outputByte[i];
            }
            charOff += inputSize;
        }
        return byteOff - outOff;
    }

    public boolean canConvert(char ch)
    {
        return true;
    }

    public int getMaxBytesPerChar()
    {
        return 3;
    }

    public void reset()
    {
        byteOff = charOff = 0;
        highHalfZoneCode = 0;
    }

    public String getCharacterEncoding()
    {
        return "DVBMarkupUTF8";
    }
}
