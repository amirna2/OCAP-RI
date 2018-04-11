/*
 * @(#)AgentX_UInt32.java                                   1.0 2000/03/01
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

import java.math.BigInteger;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;

/**
 * This class implements unsigned integer numbers with 64 bit length.
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXUInt64 implements AgentXEncodableType
{
    public static final int NUM_BYTES_IN_UINT64 = 8;
    private BigInteger myValue;
    private static final BigInteger MAX_UINT64 = new BigInteger("18446744073709551616"); // (2 ^ 64 - 1)

    /**
     * Decode the bytes from the network into an AgentXUInt64
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @return A new AgentXUInt64 containing the data from the specified byte array
     * @throws AgentXParseErrorException if any errors are detected while parsing the data.
     */
    public static AgentXUInt64 decode(byte[] data, int offset) throws AgentXParseErrorException
    {
        return new AgentXUInt64(data, offset);
    }

    /**
     * Constructs an instance of <code>AgentXUInt64</code> with the provided value.
     * 
     * @param value the data stored by this instance.
     */
    public AgentXUInt64(BigInteger value)
    {
        if (value.intValue() < 0 || value.compareTo(MAX_UINT64) > 0) 
        {
            throw new IllegalArgumentException("Unable to parse UInt64, the supplied integer is outside of the UINT64 bounds");
        }
        myValue = new BigInteger(value.toByteArray());
    }

    /**
     * Constructs an instance of <code>AgentXUInt64</code> based on the parsed AgentX data stream.
     * 
     * @param value the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @throws AgentXParseErrorException if any errors are detected while parsing the data.
     */
    private AgentXUInt64(byte[] value, int offset) throws AgentXParseErrorException
    {
        if(value == null || value.length < offset + NUM_BYTES_IN_UINT64)
        {
            throw new AgentXParseErrorException("Unable to parse UInt64, byte stream did not contain enough bytes");
        }
        
        final byte[] uint64 = new byte[NUM_BYTES_IN_UINT64];
        System.arraycopy(value, offset, uint64, 0, NUM_BYTES_IN_UINT64);
        myValue = new BigInteger(uint64);
    }

    /**
     * Encodes this instance into the appropriate AgentX encoded data stream.
     * 
     * @return an array of data representing his type encoded according to AgentX.
     */
    public byte[] encode()
    {
        final byte[] value = myValue.toByteArray();
        final byte[] uint64 = new byte[NUM_BYTES_IN_UINT64];
        System.arraycopy(value, 0, uint64, NUM_BYTES_IN_UINT64 - value.length, value.length);
        return uint64;
    }

    /**
     * Returns the number of bytes for the <code>AgentXUInt64</code>.
     * 
     * @return the length of the AentXUInt64 in bytes.
     */
    public int getLength()
    {
        return NUM_BYTES_IN_UINT64;
    }

    /**
     * Retrieves the value of stored in the <code>AgentXUInt64</code>.
     * 
     * @return the data stored in this <code>AgentXUInt64</code> instance.
     */
    public BigInteger getValue()
    {
        return myValue;
    }

}

