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
 * @(#)ByteToCharUTF8.java	1.25 06/10/10
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
 * UCS Transformation Format 8 (UTF-8) -> UCS2 (UTF16) converter
 * 
 * see CharToByteUTF8.java about UTF-8 format
 */

public class ByteToCharDVBMarkupUTF8 extends ByteToCharConverter
{

    private int savedSize;

    private byte[] savedBytes;


    public ByteToCharDVBMarkupUTF8()
    {
        super();
        savedSize = 0;
        savedBytes = new byte[5];
    }

    public int flush(char[] output, int outStart, int outEnd) throws MalformedInputException
    {
        if (savedSize != 0)
        {
            savedSize = 0;
            badInputLength = 0;
            throw new MalformedInputException();
        }
        byteOff = charOff = 0;
        return 0;
    }

    /**
     * Character converson
     */
    public int convert(byte[] input, int inOff, int inEnd, char[] output, int outOff, int outEnd)
            throws MalformedInputException, ConversionBufferFullException
    {
        int byte1, byte2, byte3, byte4, byte5, byte6, byte7;
        char[] outputChar = new char[7];
        int outputSize;
        int byteOffAdjustment = 0;


        if (savedSize != 0)
        {
            byte[] newBuf;
            newBuf = new byte[inEnd - inOff + savedSize];
            for (int i = 0; i < savedSize; i++)
            {
                newBuf[i] = savedBytes[i];
            }
            System.arraycopy(input, inOff, newBuf, savedSize, inEnd - inOff);
            input = newBuf;
            inOff = 0;
            inEnd = newBuf.length;
            byteOffAdjustment = -savedSize;
            savedSize = 0;
        }

        charOff = outOff;
        byteOff = inOff;
        int startByteOff;

        boolean bSpecialDVBMarkupEncodingDetected = false;

        while (byteOff < inEnd)
        {

            startByteOff = byteOff;
            byte1 = input[byteOff++] & 0xff;

            outputSize = 0;

            bSpecialDVBMarkupEncodingDetected = false;

            if (byte1 == 0x001b)
            {
                System.out.println("\n### MATCH 0x1B \n");
                // look ahead to see if the next bytes match any of the
                // following
                //
                // start sequences
                // 0x1B 0x42 0x00
                // 0x1B 0x43 0x04 0xrr 0xgg 0xbb 0xtt
                //
                // end sequences:
                // 0x1B 0x62
                // 0x1B 0x63
                //
                // note that 0x1B 0x42 0x00, 0x1B 0x43 0x04, 0x1B 0x62, and 0x1B
                // 0x63 will all automatically
                // pass through the UTF8 encoding with their values preserved,
                // since that are all ASCII values
                // of less that 127. The only case that needs special treatment
                // is the 0xrr 0xgg 0xbb 0xtt
                // sequence. This treatment consists of formatting chars for
                // these bytes consisting of just the byte value
                // in the lower byte of the char and 0 in the upper byte of the
                // char
                //
                // NOTE: should other sequences following 0x1b be illegal and be
                // flagged as such? No, the spec says
                // that aside from the markup codes below, bytes are to be
                // encoded as UTF8, so ne need to detect
                // invalid markup codes.

                try
                {
                    byte2 = input[byteOff] & 0x00ff;
                    if (byte2 == 0x0043)
                    {
                        System.out.println("\n### MATCH 0x43 \n");
                        byte3 = input[byteOff + 1] & 0x00ff;
                        if (byte3 == 0x04)
                        {
                            System.out.println("\n### MATCH 0x04: FORMATTING BYTES \n");

                            byte4 = input[byteOff + 2] & 0x00ff;
                            byte5 = input[byteOff + 3] & 0x00ff;
                            byte6 = input[byteOff + 4] & 0x00ff;
                            byte7 = input[byteOff + 5] & 0x00ff;

                            // if we got this far, then we have enough bytes in
                            // the buffer, and we can format chars here

                            outputChar[0] = (char) byte1;
                            outputChar[1] = (char) byte2;
                            outputChar[2] = (char) byte3;
                            outputChar[3] = (char) byte4;
                            outputChar[4] = (char) byte5;
                            outputChar[5] = (char) byte6;
                            outputChar[6] = (char) byte7;
                            outputSize = 7;

                            byteOff += 6;
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
                    savedSize = inEnd - byteOff + 1;
                    savedBytes[0] = input[byteOff - 1];;
                    for (int i = 1; i < savedSize; i++)
                    {
                        savedBytes[i] = input[byteOff++];
                    }
                    break;
                }
            }

            if (bSpecialDVBMarkupEncodingDetected)
            {
            }
            else if ((byte1 & 0x80) == 0)
            {
                outputChar[0] = (char) byte1;
                outputSize = 1;
            }
            else if ((byte1 & 0xe0) == 0xc0)
            {
                if (byteOff >= inEnd)
                {
                    savedSize = 1;
                    savedBytes[0] = (byte) byte1;
                    break;
                }
                byte2 = input[byteOff++] & 0xff;
                if ((byte2 & 0xc0) != 0x80)
                {
                    badInputLength = 2;
                    byteOff += byteOffAdjustment;
                    throw new MalformedInputException();
                }
                outputChar[0] = (char) (((byte1 & 0x1f) << 6) | (byte2 & 0x3f));
                outputSize = 1;
            }
            else if ((byte1 & 0xf0) == 0xe0)
            {
                if (byteOff + 1 >= inEnd)
                {
                    savedBytes[0] = (byte) byte1;
                    if (byteOff >= inEnd)
                    {
                        savedSize = 1;
                    }
                    else
                    {
                        savedSize = 2;
                        savedBytes[1] = (byte) input[byteOff++];
                    }
                    break;
                }
                byte2 = input[byteOff++] & 0xff;
                byte3 = input[byteOff++] & 0xff;
                if ((byte2 & 0xc0) != 0x80 || (byte3 & 0xc0) != 0x80)
                {
                    badInputLength = 3;
                    byteOff += byteOffAdjustment;
                    throw new MalformedInputException();
                }
                outputChar[0] = (char) (((byte1 & 0x0f) << 12) | ((byte2 & 0x3f) << 6) | (byte3 & 0x3f));
                outputSize = 1;
            }
            else if ((byte1 & 0xf8) == 0xf0)
            {
                if (byteOff + 2 >= inEnd)
                {
                    savedBytes[0] = (byte) byte1;
                    if (byteOff >= inEnd)
                    {
                        savedSize = 1;
                    }
                    else if (byteOff + 1 >= inEnd)
                    {
                        savedSize = 2;
                        savedBytes[1] = input[byteOff++];
                    }
                    else
                    {
                        savedSize = 3;
                        savedBytes[1] = input[byteOff++];
                        savedBytes[2] = input[byteOff++];
                    }
                    break;
                }
                byte2 = input[byteOff++] & 0xff;
                byte3 = input[byteOff++] & 0xff;
                byte4 = input[byteOff++] & 0xff;
                if ((byte2 & 0xc0) != 0x80 || (byte3 & 0xc0) != 0x80 || (byte4 & 0xc0) != 0x80)
                {
                    badInputLength = 4;
                    byteOff += byteOffAdjustment;
                    throw new MalformedInputException();
                }
                // this byte sequence is UTF16 character
                int ucs4 = (0x07 & byte1) << 18 | (0x3f & byte2) << 12 | (0x3f & byte3) << 6 | (0x3f & byte4);
                outputChar[0] = (char) ((ucs4 - 0x10000) / 0x400 + 0xd800);
                outputChar[1] = (char) ((ucs4 - 0x10000) % 0x400 + 0xdc00);
                outputSize = 2;
            }
            else
            {
                badInputLength = 1;
                byteOff += byteOffAdjustment;
                throw new MalformedInputException();
            }

            if (charOff + outputSize > outEnd)
            {
                byteOff = startByteOff;
                byteOff += byteOffAdjustment;
                throw new ConversionBufferFullException();
            }

            for (int i = 0; i < outputSize; i++)
            {
                output[charOff + i] = outputChar[i];
            }
            charOff += outputSize;
        }

        byteOff += byteOffAdjustment;
        return charOff - outOff;
    }

    /*
     * Return the character set id
     */
    public String getCharacterEncoding()
    {
        return "DVBMarkupUTF8";
    }

    /*
     * Reset after finding bad input
     */
    public void reset()
    {
        byteOff = charOff = 0;
        savedSize = 0;
    }
}
