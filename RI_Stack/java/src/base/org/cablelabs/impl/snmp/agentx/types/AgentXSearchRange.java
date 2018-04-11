/*
 * @(#)AgentX_SearchRange.java									1.0	2000/03/01
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
 * This class implements AgentX Search Range as described in RFC 2257.
 * 
 * A SearchRange consists of two Object Identifiers. In its communication with a
 * subagent, the master agent uses a SearchRange to identify a requested
 * variable binding, and, in GetNext operations, to set an upper bound on the
 * names of managed object instances the subagent may send in reply. <br>
 * 
 * The structure of the <code>AgentX_OctetString</code> is as follows: <br>
 *  <blockquote> 
 *      <li><code>First Oid</code>: The first Object Identifier in a SearchRange indicates the beginning of the range.</li>
 *      <li><code>Second Oid</code>: The second object identifier indicates the non-inclusive end of the range, 
 *                                   and its "include" field is always 0.</li> 
 * </blockquote>
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXSearchRange  implements AgentXEncodableType
{
    AgentXOid myFirstOid = null;
    AgentXOid mySecondOid = null;
    
    /*
     * Decode the bytes from the network into an AgentXSearchRange according to RFC2741
     * Section 5.2. SearchRange
     * 
     * @param obj the sequence of bytes to decode
     * @param offset the offset to start decoding the byte array
     * 
     * @return A new AgentXSearchRange containing the data from the specified byte array
     * @throws AgentXParseErrorException Thrown when the byte array is too short to parse
     */
    public static AgentXSearchRange decode(byte[] obj, int offset) throws AgentXParseErrorException 
    {
        return new AgentXSearchRange(obj, offset);
    }

    /**
     * Constructs a newly allocated <code>AgentXSearchRange</code> with its
     * components set to the components given as parameters.
     * 
     * @param startingOid the initial <code>AgentXSearchRange</code> object component.
     * @param endOid the final <code>AgentXSearchRange</code> object component.
     */
    public AgentXSearchRange(AgentXOid startingOid, AgentXOid endOid)
    {
        myFirstOid = startingOid;
        mySecondOid = endOid;
    }
    
    /**
     * Constructs a new <code>AgentXSearchRange</code> based on the AgentX encoded data.
     * 
     * @param data the raw AgentX stream representing the search range.
     * @param offset the offset into the array to start reading.
     * @throws AgentXParseErrorException  it the byte array is too short to be parsed
     */
    private AgentXSearchRange(byte[] data, int offset) throws AgentXParseErrorException
    {
        myFirstOid = AgentXOid.decode(data, offset);
        mySecondOid = AgentXOid.decode(data, offset + myFirstOid.getLength());
    }

    /**
     * Returns the byte length of the <code>AgentXSearchRange</code> object.
     * 
     * @return an <code>int</code> with the byte length of the <code>AgentXSearchRange</code> object.
     */
    public int getLength()
    {
        return myFirstOid.getLength() + mySecondOid.getLength();
    }

    /**
     * Returns the array of the bytes that compose this <code>AgentXSearchRange</code>.
     * @return an AgentX encoded data stream representing the <code>AgentXSearchRange</code>.
     */
    public byte[] encode() {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        encodedData.write(myFirstOid.encode(), 0, myFirstOid.getLength());
        encodedData.write(mySecondOid.encode(), 0, mySecondOid.getLength());
        return encodedData.toByteArray();
    }
    
    /**
     * Retrieve the first OID from the SearchRange pair
     * @return the first OID from the SearchRange pair
     */
    public AgentXOid getFirstValue() 
    {
        return myFirstOid;
    }
    
    /**
     * Retrieve the second OID from the SearchRange pair
     * @return the second OID from the SearchRange pair
     */
    public AgentXOid getSecondValue()
    {
        return mySecondOid;
    }
}
