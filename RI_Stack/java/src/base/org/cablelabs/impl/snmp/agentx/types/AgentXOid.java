/*
 * @(#)AgentX_Oid.java	    								1.0	2000/03/01
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
import java.util.StringTokenizer;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;

/**
 * This class implements AgentX Object Identifier as described in RFC 2257.
 * 
 * The structure of the <code>AgentXOID</code> is as follows: <br>
 * <blockquote>
 *      <li><code>N_SubId</code>: The number (0-128) of sub-identifiers in the object identifier.</li>
 *      <li><code>Prefix</code>: An unsigned value used to reduce the length of object identifier encodings. 
 *                               A non-zero value "x" is interpreted as the firstsub-identifier after 
 *                               "internet" (1.3.6.1).</li>
 *      <li><code>Include</code>: Used only when the Object Identifier is the start of a SearchRange.</li> 
 * </blockquote>
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXOid implements AgentXEncodableType
{
    /**
     * Minimum size of an SNMP AgentX OID (the number of bytes in the header block)
     */
    public static final int OID_HEADER_SIZE = 4;
    
    private static final int RESERVED = 0;
    private static final String OID_INTERNET_PREFIX_STRING = "1.3.6.1.";
    
    private static final AgentXUInt32 OID_INTERNET_PREFIX_VALUES[] = {
        new AgentXUInt32(1),
        new AgentXUInt32(3),
        new AgentXUInt32(6),
        new AgentXUInt32(1),
    };

    private byte myNumSubids = 0;
    private byte myPrefix = 0;
    private byte myInclude = 0;

    private AgentXUInt32 mySubids[] = null;

    /**
     * Decode the bytes from the network into an AgentXOid according to RFC2741
     * Section 5.1. Object Identifier
     * 
     * @param data the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @return A new AgentXOid containing the data from the specified byte array
     * @throws AgentXParseErrorException if any error occurs while parsing the data.
     */
    public static AgentXOid decode(byte[] data, int offset) throws AgentXParseErrorException
    {
        return new AgentXOid(data, offset);
    }

    /**
     * Constructs a newly allocated <code>AgentXOID</code> using the string given as parameter.
     * 
     * @param oid a string representation of the OID in dotted decimal notation. eg. "1.2.3.4"
     */
    public AgentXOid(String oid)
    {
        if (oid == null || oid.equals(""))
        {
            myNumSubids = 0;
            myPrefix = 0;
            myInclude = 0;
            mySubids = new AgentXUInt32[0];
        }
        else
        {
            tokenizeString(oid);
        }
    }
    
    /**
     * Constructs a newly allocated <code>AgentXOID</code> using the string
     * given as parameter.
     * 
     * @param obj a string representation of the OID in dotted decimal notation. eg. "1.2.3.4"
     * @param isIncluded used when this OID is the start of a search range.
     */
    public AgentXOid(String oid, boolean isIncluded)
    {
        if (oid == null || oid.equals(""))
        {
            myNumSubids = 0;
            myPrefix = 0;
            mySubids = new AgentXUInt32[0];
        }
        else
        {
            tokenizeString(oid);
        }
        myInclude = (byte) (isIncluded ? 1 : 0);
    }

    private AgentXOid(byte[] data, int offset) throws AgentXParseErrorException
    {
        validateDataStream(data, offset);        
        myNumSubids = data[offset];
        validateSubIdCount(data, offset);

        myPrefix = data[offset + 1];
        myInclude = data[offset + 2];

        mySubids = new AgentXUInt32[myNumSubids];
        offset = offset + OID_HEADER_SIZE;
        
        for (int i = 0; i < myNumSubids; i++)
        {
            mySubids[i] = AgentXUInt32.decode(data, offset);
            offset += AgentXUInt32.NUM_BYTES_IN_UINT32;
        }
    }

    private void validateSubIdCount(byte[] data, int offset) throws AgentXParseErrorException
    {
        if (data.length < OID_HEADER_SIZE + offset + (myNumSubids * AgentXUInt32.NUM_BYTES_IN_UINT32))
        {
            throw new AgentXParseErrorException("Unable to parse OID, byte stream contained incorrect number of subIds");
        }
    }

    private void validateDataStream(byte[] data, int offset) throws AgentXParseErrorException
    {
        if (data == null || data.length < OID_HEADER_SIZE + offset)
        {
            throw new AgentXParseErrorException("Unable to parse OID, byte stream contained incomplete header");
        }
    }

    private void tokenizeString(String oid)
    {
        if ((oid.charAt(0) == '.') || (oid.charAt(oid.length() - 1) == '.'))
        {
            throw new IllegalArgumentException("Unable to parse OID, String is badly formed");
        }

        if (oid.startsWith(OID_INTERNET_PREFIX_STRING))
        {
            myPrefix = (byte) Integer.parseInt(oid.substring(OID_INTERNET_PREFIX_STRING.length(),
                                                             OID_INTERNET_PREFIX_STRING.length() + 1));
            
            oid = oid.substring(OID_INTERNET_PREFIX_STRING.length() + 1);
        }

        StringTokenizer stringTokenizer;
        stringTokenizer = new StringTokenizer(oid, ".", false);
        
        myNumSubids = (byte) stringTokenizer.countTokens();
        mySubids = new AgentXUInt32[stringTokenizer.countTokens()];
        myInclude = 0;
        
        for (int i = 0; stringTokenizer.hasMoreElements(); i++)
        {
            mySubids[i] = new AgentXUInt32(Integer.parseInt(stringTokenizer.nextToken()));
        }
    }

    /**
     * Returns the byte length of this <code>AgentXOid</code>.
     * <p>
     * 
     * @return an <code>int</code> with the byte length of the
     *         <code>AgentXOid</code> object.
     */
    public int getLength()
    {
        int ret = OID_HEADER_SIZE;
        ret += mySubids.length * AgentXUInt32.NUM_BYTES_IN_UINT32;
        return ret;
    }

    /**
     * Returns a string corresponding the oid.
     * <p>
     * 
     * @return a <code>String</code> within the corresponding tree nodes of the
     *         <code>AgentXOid</code> object.
     */
    public String getValue()
    {
        StringBuffer str = new StringBuffer();
        int i;
        if (myPrefix > 0)
        {
            str.append(OID_INTERNET_PREFIX_STRING);
            str.append(myPrefix);
            if (mySubids.length > 0)
            {
                str.append('.');
            }
        }
        for (i = 0; i < mySubids.length; i++)
        {
            str.append(mySubids[i].getValue());
            if (i < mySubids.length - 1)
            {
                str.append('.');
            }
        }
        return str.toString();
    }

    /**
     * Get the array of sub ids for the comparison method, injects the prefix values for ease of comparison
     * 
     * @return the array of sub ids for the comparison method
     */
    protected AgentXUInt32[] getValueArray()
    {
        int isPrefixSet = Math.min(1, myPrefix);
        int length = mySubids.length + (isPrefixSet * OID_INTERNET_PREFIX_VALUES.length) + isPrefixSet;
        AgentXUInt32 completeSubids[];
        if(length == mySubids.length)
        {
            completeSubids = mySubids;
        }
        else
        {
            completeSubids = new AgentXUInt32[length];
            for(int i = 0; i < OID_INTERNET_PREFIX_VALUES.length; i++)
            {
                completeSubids[i] = OID_INTERNET_PREFIX_VALUES[i];
            }
            completeSubids[OID_INTERNET_PREFIX_VALUES.length] = new AgentXUInt32(myPrefix);
            for(int i = 0; i < mySubids.length; i++)
            {
                completeSubids[OID_INTERNET_PREFIX_VALUES.length + 1 + i] = mySubids[i];
            }
        }
        
        return completeSubids;
    }

    /**
     * Returns the array of the bytes that compose this <code>AgentXOid</code>.
     * <p>
     * @return a <code>btye</code> array corresponding the
     *         <code>AgentXOid</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        encodedData.write(myNumSubids);
        encodedData.write(myPrefix);
        encodedData.write(myInclude);
        encodedData.write(RESERVED);
        for (int i = 0; i < mySubids.length; i++)
        {
            encodedData.write(mySubids[i].encode(), 0, AgentXUInt32.NUM_BYTES_IN_UINT32);
        }
        return encodedData.toByteArray();
    }

    /**
     * Test if the isIncluded bit is set for this oid
     * 
     * @return true if the bit is set to 1, false if set to 0
     */
    public boolean isIncluded()
    {
        return myInclude == 1;
    }

    protected byte getPrefix()
    {
        return myPrefix;
    }

    public long compareTo(AgentXOid oid)
    {
        long result = 0;
        AgentXUInt32 thisSubids[] = getValueArray();
        AgentXUInt32 oidSubids[] = oid.getValueArray();
        
        int maxCount = Math.min(thisSubids.length, oidSubids.length);
        for(int i = 0; i < maxCount; i++) {
            result = thisSubids[i].getValue() - oidSubids[i].getValue();
            if(result != 0)
            {
                break;
            }

        }
        if(result == 0)
        {
            result = thisSubids.length - oidSubids.length;
        }
        
        return result;
    }
}
