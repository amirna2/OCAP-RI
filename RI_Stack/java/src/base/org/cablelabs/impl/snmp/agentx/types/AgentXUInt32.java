/*
 * @(#)AgentX_UInt32.java									1.0	2000/03/01
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
 * This class implements unsigned integer numbers with 32 bit length.
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXUInt32 implements AgentXEncodableType
{
    private long myValue = 0;

    public static final int NUM_BYTES_IN_UINT32 = 4;
    private static final long MAX_UINT32 = 0xFFFFFFFFL;

    /**
     * Decode the bytes from the network into an AgentXUInt32
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @return A new AgentXUInt32 containing the data from the specified byte array
     * 
     * @throws AgentXParseErrorException if an error is detected while parsing.
     */
    public static AgentXUInt32 decode(byte[] data, int offset) throws AgentXParseErrorException
    {
        return new AgentXUInt32(data, offset);
    }

    /**
     * Constructs a new allocated <code>AgentX_UInt32</code> with its component
     * set to the object given as parameter.
     * 
     * @param value the <code>long</code> value.
     */
    public AgentXUInt32(long value)
    {
        if (value < 0 || value > MAX_UINT32)
        {
            throw new IllegalArgumentException("Unable to parse UInt32, the supplied integer is outside of the UINT32 bounds");
        }
        myValue = value & 0xFFFFFFFF;
    }

    /**
     * Construct the AgentXUInt32 instance from the AgentX data stream. 
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * 
     * @throws AgentXParseErrorException if an error is detected while parsing.
     */
    private AgentXUInt32(byte value[], int offset) throws AgentXParseErrorException
    {
        if (value == null || value.length < offset + NUM_BYTES_IN_UINT32)
        {
            throw new AgentXParseErrorException("Unable to parse UInt32, byte stream did not contain enough bytes");
        }

        /*
         * Big Endian so the first byte is most significant
         */
        myValue = ((long) (((value[offset]     & 0xFF) << 24) 
                         + ((value[offset + 1] & 0xFF) << 16)
                         + ((value[offset + 2] & 0xFF) << 8) 
                         +  (value[offset + 3] & 0xFF))) 
                         & 0xFFFFFFFFFFFFL;
    }

    /**
     * Constructs and AgentX encoded array of data representing the AgentXUInt32 instance.
     * 
     * @return the AgentX encoded data.
     */
    public byte[] encode()
    {
        byte ret[] = new byte[4];

        // Big Endian so the first byte is most significant
        ret[0] = (byte) ((myValue >> 24) & 0x00FF);
        ret[1] = (byte) ((myValue >> 16) & 0x00FF);
        ret[2] = (byte) ((myValue >> 8) & 0x00FF);
        ret[3] = (byte) (myValue & 0x00FF);

        return ret;
    }

    /**
     * Returns the number of bytes for this <code>AgentXUInt32</code>.
     * 
     * @return the <code>int</code> value of the <code>AgentXUInt32</code> byte size.
     */
    public int getLength()
    {
        return NUM_BYTES_IN_UINT32;
    }

    /**
     * Returns the value of this <code>AgentX_UInt32</code>.
     * 
     * @return the <code>long</code> value of the <code>AgentX_UInt32</code> object.
     */
    public long getValue()
    {
        return myValue;
    }

}
