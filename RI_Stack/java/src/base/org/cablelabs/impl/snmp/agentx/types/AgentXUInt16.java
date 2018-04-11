/*
 * @(#)AgentX_UInt16.java									1.0	2000/03/01
 *
 * ------------------------------------------------------------------------
 *        Copyright (c) 2000 University of Coimbra, Portugal
 *
 *                     All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the name of the University of Coimbra
 * not be used in advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 *
 * University of Coimbra distributes this software in the hope that it will
 * be useful but DISCLAIMS ALL WARRANTIES WITH REGARD TO IT, including all
 * implied warranties of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. In no event shall University of Coimbra be liable for any
 * special, indirect or consequential damages (or any damages whatsoever)
 * resulting from loss of use, data or profits, whether in an action of
 * contract, negligence or other tortious action, arising out of or in
 * connection with the use or performance of this software.
 * ------------------------------------------------------------------------
 */

package org.cablelabs.impl.snmp.agentx.types;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;

/**
 * This class implements unsigned integer numbers with 16 bit length.
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXUInt16 implements AgentXEncodableType
{
    private int myValue = 0;

    public static final int NUM_BYTES_IN_UINT16 = 2;
    private static final int MAX_UINT16 = (int) 0xFFFF;

    /**
     * Decode the bytes from the network into an AgentXUInt16
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * 
     * @return a new AgentXUInt16 instance containing the data from the specified byte array.
     * @throws AgentXParseErrorException if an error is detected while parsing.
     */
    public static AgentXUInt16 decode(byte[] data, int offset) throws AgentXParseErrorException
    {
        return new AgentXUInt16(data, offset);
    }

    /**
     * Constructs a new allocated <code>AgentXUInt16</code>
     * 
     * @param value the data to be stored in this format.
     */
    public AgentXUInt16(int value)
    {
        if (value < 0 || value > MAX_UINT16)
        {
            throw new IllegalArgumentException("The supplied integer is outside of the UINT16 bounds");
        }
        
        myValue = (value & 0x0000FFFF);
    }

    /**
     * Constructs a <code>AgentXUInt16</code> instance based on AgentX encoded data. 
     * 
     * @param value the sequence of bytes to decode.
     * @param offset the offset to start decoding the byte array
     * @throws AgentXParseErrorException if an error is detected while parsing.
     */
    private AgentXUInt16(byte value[], int offset) throws AgentXParseErrorException
    {
        if (value == null || value.length < offset + NUM_BYTES_IN_UINT16)
        {
            throw new AgentXParseErrorException("Unable to parse UInt16, byte stream did not contain enough bytes");
        }
        
        myValue = ((value[offset] & 0xFF) << 8) + (value[offset + 1] & 0xFF);
    }

    /**
     * Retrieves the number of bytes for this <code>AgentXUInt16</code>.
     * 
     * @return the number of bytes used to represent an unsigned 16 bit integer.
     */
    public int getLength()
    {
        return NUM_BYTES_IN_UINT16;
    }

    /**
     * Retrieves the value of this <code>AgentXUInt16</code> instance.
     * @return the value stored in this <code>AgentXUInt16</code> instance.
     */
    public int getValue()
    {
        return myValue;
    }

    /**
     * Encodes the <code>AgentXUInt16</code> instance as an AgentX byte stream.
     * 
     * @return the AgentX encoded data stream representing this <code>AgentXUInt16</code> instance.
     */
    public byte[] encode()
    {
        final byte ret[] = new byte[2];
        
        /*
         * Big Endian so the first byte is most significant
         */
        ret[0] = (byte) ((myValue >> 8) & 0x00FF);
        ret[1] = (byte) (myValue & 0x00FF);

        return ret;
    }

}
