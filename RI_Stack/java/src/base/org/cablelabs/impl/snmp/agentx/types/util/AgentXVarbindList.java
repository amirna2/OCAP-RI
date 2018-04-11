/*
 * @(#)AgentXVarbindList.java									1.0	2000/03/01
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

import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.types.AgentXEncodableType;
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.ocap.diagnostics.MIBObject;

/**
 * This class implements AgentX Search Range List as described in RFC 2257. A
 * VarbindList is a contiguous list of Varbinds.
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXVarbindList implements AgentXEncodableType
{
    ArrayList myList;

    /**
     * A copy constructor.
     * 
     * @param list the list.
     */
    public AgentXVarbindList(AgentXVarbindList list)
    {
        myList = (list == null ? new ArrayList(): new ArrayList(list.myList));
    }
   
    /**
     * Constructs a newly allocated <code>AgentXVarbindList</code> by decoding
     * the MIBObect array into AgentXVarbinds.
     * @param list A list of MIBObects to be converted into Varbinds
     * @throws SNMPBadValueException Indicates byte array in value field is uninterprettable for
    *     specified SNMP object type.
     * @throws AgentXParseErrorException Thrown when the byte array is too short to parse
     */
    public AgentXVarbindList(MIBObject[] list) throws SNMPBadValueException, AgentXParseErrorException
    {
        if(list == null)
        {
            throw new IllegalArgumentException("MIBObject list cannot be null!");
        }
        myList = new ArrayList();
        for (int i = 0; i < list.length; i++)
        {
            myList.add(new AgentXVarbind(list[i].getOID(), list[i].getData()));
        }
    }
    
    /**
     * Constructs a newly allocated <code>AgentXVarbindList</code> using the
     * byte array given as a parameter.
     * <p>
     * 
     * @param obj the <code>byte</code> array to be converted.
     * @param offset the offset into the array to start reading.
     * @param maxLength the maximum number of bytes to read before stopping
     * @throws AgentXParseErrorException Thrown if byte array is too short to read
     */
    private AgentXVarbindList(byte[] data, 
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
            AgentXVarbind vb = AgentXVarbind.decode(data, i);
            this.add(vb);
            i += vb.getLength();
        }
    }
    
    /**
     * Decode the bytes from the network into an AgentXVarbindList according to RFC2741
     * Section 5.4. Value Representation
     * 
     * @param obj the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * @param maxLength the maximum number of bytes to read before stopping
     * @return A new AgentXVarbind containing the data from the specified byte array.
     * @throws AgentXParseErrorException Thrown if byte array is too short to read
     */
    public static AgentXVarbindList decode(byte[] data, 
                                            int offset, 
                                            int maxLength) throws AgentXParseErrorException
    {
        return new AgentXVarbindList(data, offset, maxLength);
    }
    
    /**
     * Constructs an empty <code>AgentXVarbindList<code> object.
     */
    public AgentXVarbindList()
    {
        myList = new ArrayList();
    }

    /**
     * Adds an <code>AgentXVarbind<code> object to the <code>AgentXVarbindList<code> object.
     * 
     * @param ob the <code>AgentXVarbind<code> object to be added to the <code>AgentXVarbindList<code> object.
     */
    public void add(AgentXVarbind ob)
    {
        myList.add(ob);
    }
    
    /**
     * Adds an AgentXVarbind object to the list at the specified location.
     * 
     * @param index where to insert the varbind object.
     * @param ob the object to be added to the list.
     */
    public void add(int index, AgentXVarbind ob) 
    {
        myList.add(index, ob);
    }

    /**
     * Returns the number of elements there are in the
     * <code>AgentXVarbindList<code> object.
     * <p>
     * 
     * @param index
     *            <code>int<code> value for the index of the desired
     *            <code>AgentXVarbind<code> object.
     * @return the desired <code>AgentXVarbind<code> object.
     */
    public AgentXVarbind getValueAt(int index)
    {
        AgentXVarbind ret = (AgentXVarbind) myList.get(index);
        return ret;
    }

    /**
     * Returns the byte length of the <code>AgentXVarbindList<code> object.
     * <p>
     * 
     * @return a <code>long</code> value for the byte length of the
     *         <code>AgentXVarbindList<code> object.
     */
    public int getLength()
    {
        int ret = 0;
        for (int i = 0; i < myList.size(); i++)
            ret += getValueAt(i).getLength();
        return ret;
    }

    /**
     * Returns the array of the bytes that compose this
     * <code>AgentXVarbindList</code>.
     * <p>
     * @return a <code>btye</code> array corresponding the
     *         <code>AgentXVarbindList</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        for (int i = 0; i < myList.size(); i++)
        {
            AgentXVarbind vb = getValueAt(i);
            encodedData.write(vb.encode(), 0, vb.getLength());
        }
        return encodedData.toByteArray();
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
        return myList.size();
    }
}
