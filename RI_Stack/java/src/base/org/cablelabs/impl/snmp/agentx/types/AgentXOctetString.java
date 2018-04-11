/*
 * @(#)AgentX_OctetString.java								1.0	2000/03/01
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

import java.io.ByteArrayOutputStream;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;

/**
 * This class implements AgentX Octet String as described in RFC 2257. An octet string is 
 * represented by a contiguous series of bytes.
 * 
 * The structure of the <code>AgentX_OctetString</code> is as follows: <br>
 * <blockquote>
 *      <li><code>Octet Lenght</code>: The number of octets.</li>
 *      <li><code>Octet List</code>: The octet list. This list is inherited from the <code>OctetString</code> class.</li>
 *      <li><code>Padding</code>: Padding bytes are appended whenever the last octet does not end on a 
 *                                4-byte offset from the start of the Octet String.</li> 
 * </blockquote>
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXOctetString implements AgentXEncodableType
{
    private static final int PADDING_BIT = 0;
    private static final int OCTET_LENGTH_SIZE = AgentXUInt32.NUM_BYTES_IN_UINT32;
    private byte myData[] = null;
    private byte myPadding[] = null;

    private AgentXUInt32 myLength;

    /**
     * Decode the bytes from the network into an AgentXOctetString according to
     * RFC2741 Section 5.3. Octet String
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @return A new AgentXOctetString containing the data from the specified byte array
     * 
     * @throws AgentXParseErrorException if any error occurs while parsing the data.
     */
    public static AgentXOctetString decode(byte[] data, int offset) throws AgentXParseErrorException
    {
        return new AgentXOctetString(data, offset);
    }

    /**
     * Constructs a newly allocated <code>AgentX_OctetString</code> using a
     * string given as a parameter.
     * 
     * @param value the <code>String</code> to be converted.
     */
    public AgentXOctetString(String value)
    {
        myData = (value == null ? new byte[0] : value.getBytes());
        myPadding = addPadding(myData.length);
        myLength = new AgentXUInt32((long) myData.length);
    }

    /**
     * Constructs a newly allocated <code>AgentX_OctetString</code> using the AgentX encoded data.
     * 
     * @param data the <code>byte</code> array to be converted.
     * @param offset the offset into the array to start reading.
     * @throws AgentXParseErrorException if any error occurs while parsing the data.
     */
    private AgentXOctetString(byte[] data, int offset) throws AgentXParseErrorException
    {
        validateDataStream(data, offset);        
        myLength = AgentXUInt32.decode(data, offset);
        offset = offset + OCTET_LENGTH_SIZE;
        validateLength(data, offset);        
        myData = decodeString(data, (int) myLength.getValue(), offset);
        myPadding = addPadding(myData.length);
    }

    private void validateLength(byte[] data, int offset) throws AgentXParseErrorException
    {
        if (data.length < offset + myLength.getValue())
        {
            throw new AgentXParseErrorException("Can't parse OctetString, incorrect number of Octets");
        }
    }

    private void validateDataStream(byte[] data, int offset) throws AgentXParseErrorException
    {
        if (data == null || data.length < offset + OCTET_LENGTH_SIZE)
        {
            throw new AgentXParseErrorException("Unable to parse OctetString, byte stream is null");
        }
        else if ((data.length - offset) % OCTET_LENGTH_SIZE != 0)
        {
            int missing = (OCTET_LENGTH_SIZE - ((data.length - offset) % OCTET_LENGTH_SIZE));
            
            throw new AgentXParseErrorException("Octet String is badly formed. Must align to octet boundaries "
                    + "add " + missing + " zero filled bytes to fix");
        }
    }

    private byte[] addPadding(int stringLength)
    {
        byte paddedBits[] = null;
        if (stringLength % OCTET_LENGTH_SIZE != 0)
        {
            final int size = OCTET_LENGTH_SIZE - (stringLength % OCTET_LENGTH_SIZE);
            paddedBits = new byte[size];
            
            for (int i = 0; i < size; i++)
            {
                paddedBits[i] = PADDING_BIT;
            }
        }
        else
        {
            paddedBits = new byte[0];
        }
        return paddedBits;
    }

    private byte[] decodeString(byte[] obj, int length, int offset)
    {
        byte returnData[] = new byte[length];
        
        for (int i = 0; i < length; i++)
        {
            returnData[i] = obj[offset + i];
        }
        return returnData;
    }

    /**
     * Returns the byte length of this <code>AgentX_OctetString</code>.
     * 
     * @return a the byte length of the <code>AgentX_OctetString</code> object.
     */
    public int getLength()
    {
        int ret = AgentXUInt32.NUM_BYTES_IN_UINT32;
        ret += myLength.getValue();
        ret += myPadding.length;
        return ret;
    }

    /**
     * Retrieves the value of the octet string.
     * 
     * @return the value contained in this octet string instance.
     */
    public String getValue()
    {
        return new String(myData);
    }

    /**
     * Returns the array of the bytes that compose this <code>AgentXOctetString</code>.
     * 
     * @return an AgentX encoded <code>AgentXOctetString</code> instance.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        encodedData.write(myLength.encode(), 0, AgentXUInt32.NUM_BYTES_IN_UINT32);
        encodedData.write(myData, 0, myData.length);
        encodedData.write(myPadding, 0, myPadding.length);
        return encodedData.toByteArray();
    }
}
