/*
 * @(#)AgentXResponsePDU.java									v1.0	2000/03/01
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
package org.cablelabs.impl.snmp.agentx.protocol.notifications;

import java.io.ByteArrayOutputStream;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXProtocolException;

import org.cablelabs.impl.snmp.agentx.types.AgentXUInt16;
import org.cablelabs.impl.snmp.agentx.types.AgentXUInt32;

import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;

/**
 * The <code>AgentXResponsePDU</code> class implements an AgentX Response PDU.
 * <p>
 * The structure of a <code>AgentXResponsePDU</code> object is as follows: <br>
 * <p>
 * <blockquote>
 *      <li><code>Header</code>: AgentX Pdu Header. <br> </li>
 *      <li><code>Error</code>: Indicates error status. Within responses to the set
 *                              of "administrative" PDU types, "AgentX PDU Header", values are limited to the
 *                              following: <br> </li>
 *          <blockquote> noAgentXError (0) <br>
 *                       openFailed (256) <br>
 *                       notOpen (257) <br>
 *                       indexWrongType (258) <br>
 *                       indexAlreadyAllocated (259) <br>
 *                       indexNoneAvailable (260) <br>
 *                       indexNotAllocated (261) <br>
 *                       unsupportedContext (262) <br>
 *                       duplicateRegistration (263) <br>
 *                       unknownRegistration (264) <br>
 *                       unknownAgentCaps (265) <br>
 *                       parseError (266) <br>
 *                       requestDenied (267) <br>
 *                       processingError (268) <br>
 *          </blockquote>
 *      <li><code>Index</code>: In error cases, this is the index of the failed variable binding within a received 
 *                              request PDU. <br> </li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXResponsePDU
{  
    private AgentXHeader myHeader;
    private AgentXUInt32 mySystemUpTime;
    private AgentXUInt16 myError;
    private AgentXUInt16 myIndex;
    private AgentXVarbindList myVarbindList;

    /**
     * Constructs a <code>AgentXResponsePDU</code> object from the parameters
     * specified.
     */
    public AgentXResponsePDU(AgentXHeader header, long sysUpTime, int error, int index,
            AgentXVarbindList varbindList)
    {
        myHeader = header;

        myError = new AgentXUInt16(error);
        mySystemUpTime = new AgentXUInt32(sysUpTime);
        myIndex = new AgentXUInt16(index);
        myVarbindList = new AgentXVarbindList(varbindList);
    }

    /**
     * Constructs a <code>AgentXResponsePDU</code> object from the parameters
     * specified.
     * @throws AgentXParseErrorException 
     */
    private AgentXResponsePDU(byte[] data) throws AgentXProtocolException, AgentXParseErrorException
    {
        myHeader = AgentXHeader.decode(data);

        int offset = AgentXHeader.HEADER_LENGTH;

        mySystemUpTime = AgentXUInt32.decode(data, offset);
        offset += mySystemUpTime.getLength();

        myError = AgentXUInt16.decode(data, offset);
        offset += myError.getLength();

        myIndex = AgentXUInt16.decode(data, offset);
        offset += myIndex.getLength();
        
        myVarbindList = AgentXVarbindList.decode(data, 
                                                 offset, 
                                                 myHeader.getPayloadLength() - this.getPayloadLength());
    }

    /**
     * Decodes an AgentX ByteStream containing the <code>AgentXResponsePDU</code> object.
     * 
     * @param stream the data to be decoded.
     * 
     * @return an AgentXResponsePDU instance which represents the data read off the wire.
     * 
     * @throws AgentXProtocolException if the protocol is not adhered to by the data.
     * @throws AgentXParseErrorException if there is an error detected while decoding the data. 
     */
    public static AgentXResponsePDU decode(byte[] stream) throws AgentXProtocolException, AgentXParseErrorException
    {
        return new AgentXResponsePDU(stream);
    }

    /**
     * Encodes an <code>AgentXResponsePDU</code> object into an AgentX byte stream. 
     * 
     * @return an <code>AgentXByteStream</code> with the encoded <code>AgentXResponsePDU</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + getPayloadLength());

        data.write(myHeader.encode(getPayloadLength()), 0, AgentXHeader.HEADER_LENGTH);
        data.write(mySystemUpTime.encode(), 0, mySystemUpTime.getLength());
        data.write(myError.encode(), 0, myError.getLength());
        data.write(myIndex.encode(), 0, myIndex.getLength());
        
        if (myVarbindList != null)
        {
            data.write(myVarbindList.encode(), 0, myVarbindList.getLength());
        }
        return data.toByteArray();
    }
    
    /**
     * Retrieve the list of varbinds returned in the response PDU.
     * 
     * @return a list of <code>AgentXVarind</code>'s
     */
    public AgentXVarbindList getVarbindList()
    {
        return myVarbindList;
    }

    /*
     * Returns the Payload Length of the <code>AgentXResponsePDU</code>.
     * 
     * @return an <code>AgentXUInt32</code> with the Payload Length of this
     * <code>AgentXResponsePDU</code> object.
     */
    private int getPayloadLength()
    {
        int sum = mySystemUpTime.getLength() + myError.getLength() + myIndex.getLength();

        if (myVarbindList != null && myVarbindList.size() > 0)
        {
            sum += myVarbindList.getLength();
        }

        return sum;
    }

    /**
     * Retrieve the session ID in the response PDU.
     * 
     * @return the session ID value.
     */
    public long getSessionId()
    {
        return myHeader.getSessionId();
    }
    
    /**
     * Retrieve the packet ID in the response PDU.
     * 
     * @return the packet ID value.
     */
    public long getPacketId()
    {
        return myHeader.getPacketId();
    }
    
    /**
     * Retrieve the error code in the response PDU.
     * 
     * @return the error code value.
     */
    public int getErrorCode()
    {
        return myError.getValue();
    }
    
    /**
     * Retrieve the index from the response PDU.
     * 
     * @return the index value.
     */
    public int getIndex()
    {
        return myIndex.getValue();
    }

}
