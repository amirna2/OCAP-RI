/*
 * @(#)AgentXSearchRangeList.java						 	1.0	2000/03/01
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

package org.cablelabs.impl.snmp.agentx.types.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.types.AgentXEncodableType;
import org.cablelabs.impl.snmp.agentx.types.AgentXSearchRange;

/**
 * This class implements AgentX Search Range List as described in RFC 2257. A
 * SearchRangeList is a contiguous list of SearchRanges.
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXSearchRangeList implements AgentXEncodableType
{

    ArrayList myList;

    /**
     * Constructs a newly allocated <code>AgentXSearchRangeList</code> with its
     * components set to default values.
     */
    public AgentXSearchRangeList()
    {
        myList = new ArrayList();
    }

    /*
     * Constructs a newly allocated <code>AgentXSearchRangeList</code> using the
     * byte array given as a parameter.
     * <p>
     * 
     * @param obj the <code>byte</code> array to be converted.
     * @param offset the offset into the array to start reading.
     * @param maxLength the maximum number of bytes to read before stopping
     * @throws AgentXParseErrorException Thrown if byte array is too short to read
     */
    private AgentXSearchRangeList(byte[] data, 
            int offset, 
            int maxLength) throws AgentXParseErrorException
    {
        if(data == null || data.length < offset + maxLength)
        {
            throw new AgentXParseErrorException("Unable to parse: The array was incomplete");
        }
        myList = new ArrayList();
        for (int i = offset; i < offset + maxLength;)
        {
            AgentXSearchRange sr = AgentXSearchRange.decode(data, i);
            this.add(sr);
            i += sr.getLength();
        }
    }

    /**
     * Decode the bytes from the network into an AgentXSearchRangeList according to RFC2741
     * Section 5.2. SearchRange
     * 
     * @param obj the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @param maxLength the maximum number of bytes to read before stopping
     * @return A new AgentXVarbind containing the data from the specified byte array.
     * @throws AgentXParseErrorException Thrown if byte array is too short to read
     */
    public static AgentXSearchRangeList decode(byte[] data, 
                                               int offset, 
                                               int maxLength) throws AgentXParseErrorException
    {
        return new AgentXSearchRangeList(data, offset, maxLength);
    }

    /**
     * Adds a new <code>AgentXSearchRange</code> given as parameter to the
     * <code>AgentXSearchRangeList</code>.
     * <p>
     * 
     * @param ob
     *            the adding <code>AgentXSearchRange</code> object.
     */
    public void add(AgentXSearchRange ob)
    {
        myList.add(ob);
    }

    /**
     * Returns the number of elements in the list.
     * <p>
     * 
     * @return an <code>int</code> with the value of the number of
     *         <code>AgentXSearchRange</code> objects in the
     *         <code>AgentXSearchRangeList</code> object.
     */
    public int size()
    {
        return myList == null ? 0 : myList.size();
    }

    /**
     * Returns the element in the index given as parameter.
     * <p>
     * 
     * @param index
     *            the index of the <code>AgentXSearchRangeList</code> object
     *            from which it will be taked the element.
     * @return the <code>AgentXSearchRange</code> in the index of the
     *         <code>AgentXSearchRangeList</code> object.
     */
    public AgentXSearchRange getValueAt(int index)
    {
        AgentXSearchRange ret = (AgentXSearchRange) myList.get(index);
        return ret;
    }

    /**
     * Returns the byte length.
     * <p>
     * 
     * @return a <code>long</code> containing the value of the byte length of
     *         the <code>AgentXSearchRangeList</code> object.
     */
    public int getLength()
    {
        int ret = 0;
        for (int i = 0; i < myList.size(); i++)
        {
            ret += getValueAt(i).getLength();
        }
        return ret;
    }
    
    /**
     * Returns the array of the bytes that compose this
     * <code>AgentXSearchRangeList</code>.
     * <p>
     * @return a <code>btye</code> array corresponding the
     *         <code>AgentXSearchRangeList</code> object.
     */
    public byte[] encode() {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        for (int i = 0; i < myList.size(); i++)
        {
            AgentXSearchRange sr = getValueAt(i);
            encodedData.write(sr.encode(), 0, sr.getLength());
        }
        return encodedData.toByteArray();
    }
}
