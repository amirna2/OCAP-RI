/*
 * @(#)AgentXPDUHeader.java									v1.0	2000/03/01
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
package org.cablelabs.impl.snmp.agentx.protocol;

import java.io.ByteArrayOutputStream;

import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.types.AgentXUInt32;

/**
 * The <code>AgentXPDUheader</code> class implements an generic AgentX PDU Header used in all the AgentX PDU's. 
 * This is a fixed-format, 20-octet structure.
 * <p>
 * The structure of a <code>AgentXPDUHeader</code> object is as follows: <br>
 * <p>
 * <blockquote>
 *      <li><code>Version</code>: The version of the AgentX protocol (1 for this implementation). <br> </li> 
 *      <li><code>Type</code>: The PDU type; one of the following values:
 *      <blockquote>
 *              agentx-Open-PDU (1) <br>
 *              agentx-Close-PDU (2) <br>
 *              agentx-Register-PDU (3) <br>
 *              agentx-Unregister-PDU (4) <br>
 *              agentx-Get-PDU (5) <br>
 *              agentx-GetNext-PDU (6) <br>
 *              agentx-GetBulk-PDU (7) <br>
 *              agentx-TestSet-PDU (8) <br>
 *              agentx-CommitSet-PDU (9) <br>
 *              agentx-UndoSet-PDU (10) <br>
 *              agentx-CleanupSet-PDU (11) <br>
 *              agentx-Notify-PDU (12) <br>
 *              agentx-Ping-PDU (13) <br>
 *              agentx-IndexAllocate-PDU (14) <br>
 *              agentx-IndexDeallocate-PDU (15) <br>
 *              agentx-AddAgentCaps-PDU (16) <br>
 *              agentx-RemoveAgentCaps-PDU (17) <br>
 *              agentx-Response-PDU (18) <br>
 *      </blockquote>
 *      </li>
 *       
 *      The set of PDU types for "administrative processing" are 1-4 and 12-17. The set of PDU types for 
 *      "SNMP request processing" are 5-11. <br>
 *      
 *      <li><code>Flags</code>: A bitmask, with bit 0 the least significant bit. The bit definitions are as follows:<br>
 *      <blockquote> 
 *              0 - INSTANCE_REGISTRATION <br>
 *              1 - NEW_INDEX <br>
 *              2 - ANY_INDEX <br>
 *              3 - NON_DEFAULT_CONTEXT <br>
 *              4 - NETWORK_BYTE_ORDER <br>
 *              5-7 - (reserved) <br>
 *      </blockquote>
 *      </li>
 *       
 *      <li><code>SessionID</code>: The session ID uniquely identifies a session over which AgentX PDUs are exchanged 
 *      between a subagent and the master agent. <br>
 *      </li>
 *       
 *      <li><code>TransactionID</code>: The transaction ID uniquely identifies, for a given session, the single SNMP 
 *      management request (and single SNMP PDU) with which an AgentX PDU is associated. <br>
 *      </li>
 *       
 *      <li><code>SessionID</code>: A packet ID generated by the sender for all AgentX PDUs except the 
 *      agentx-Response-PDU. In an agentx-Response-PDU, the packet ID must be the same as that in the received 
 *      AgentX PDU to which it is a response. <br>
 *      </li>
 *       
 *      <li><code>PayLoad_Length</code>: The size in octets of the PDU contents, excluding the 20-byte header. 
 *      As a result of the encoding schemes and PDU layouts, this value will always be either 0, or a multiple of 4.<br>
 *      </li> 
 *  </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXHeader
{
    public static final int HEADER_LENGTH = 20;

    private byte myVersion;
    private byte myPduType;
    private byte myFlags;
    private byte reserved = 0; // <reserved>

    private AgentXUInt32 mySessionId;
    private AgentXUInt32 myTransactionId;
    private AgentXUInt32 myPacketId;
    private AgentXUInt32 myPayloadLength;

    private boolean isBigEndian;
    private boolean isDefaultContext;
    
    /** 
     * AgentX supported version: 1.0 
     */
    private final static byte VERSION_1 = (byte) 1; 

    /** 
     * AgentX PDU Header Flags: Non Default Context (Bit 3). 
     */
    private final static byte AGENTX_NON_DEFAULT_CONTEXT = (byte) 8; 
    
    /** 
     * AgentX PDU Header Flags: Network Byte Order (Bit 4). 
     */
    private final static byte AGENTX_NETWORK_BYTE_ORDER = (byte) 16; 
    
    /**
     * Constructs a copy of the <code>AgentXPDUHeader</code> object changing the type field.
     * 
     * @param data the AgentX encoded stream of data representing the header for an AgentX-PDU
     * @throws AgentXProtocolException if an error occurred while decoding the data.
     * @throws AgentXParseErrorException 
     */
    private AgentXHeader(byte[] data) throws AgentXProtocolException, AgentXParseErrorException
    {
        if(data == null || data.length < HEADER_LENGTH)
        {
            throw new AgentXParseErrorException("Unable to parse PDU header, byte stream contained incomplete data");
        }
        
        int index = 0;
        myVersion = data[index];
        
        if (myVersion != VERSION_1)
        {
            throw new AgentXProtocolException(AgentXProtocolExceptionReason.UNSUPPORTED_AGENTX_VERSION);
        }
        
        myPduType = data[++index];
        
        if (myPduType < ProtocolDataUnitType.AGENTX_OPEN_PDU.getTypeValue() 
                || myPduType > ProtocolDataUnitType.AGENTX_RESPONSE_PDU.getTypeValue())
        {
            throw new AgentXProtocolException(AgentXProtocolExceptionReason.UNKNOWN_PDU_TYPE);
        }
        
        myFlags = data[++index];        
        
        isBigEndian = (myFlags & AGENTX_NETWORK_BYTE_ORDER) != 0;
        isDefaultContext = (myFlags & AGENTX_NON_DEFAULT_CONTEXT) == 0;
        
        reserved = data[++index];
        
        mySessionId = AgentXUInt32.decode(data, ++index);        
        index += mySessionId.getLength();
        
        myTransactionId = AgentXUInt32.decode(data, index);
        index += myTransactionId.getLength();
        
        myPacketId = AgentXUInt32.decode(data, index);
        index += myPacketId.getLength();
        
        myPayloadLength = AgentXUInt32.decode(data, index);
    }
       
    /**
     * Constructs a <code>AgentXPDUHeader</code> object from the parameters specified.
     * 
     * @param type the type of PDU this header represents.
     * @param sessionId the session associated with this PDU.
     * @param transactionId the transaction ID used to indicate a unique request transaction underway.
     * @param packetId the packet ID used to pair responses with requests.
     * @param bigEndian indicates if network byte ordering is used to encoded multi-byte values or not.
     * @param defaultContext indicates if the default context should be used or not in the payload.
     */
    public AgentXHeader(ProtocolDataUnitType type, 
                        long sessionId, 
                        long transactionId, 
                        long packetId, 
                        boolean bigEndian, 
                        boolean defaultContext)
    {
        myVersion = VERSION_1; // Only Version 1 is supported        
        myPduType = type.getTypeValue();
        
        mySessionId = new AgentXUInt32(sessionId);
        myTransactionId = new AgentXUInt32(transactionId);
        myPacketId = new AgentXUInt32(packetId);
        
        isBigEndian = bigEndian;
        isDefaultContext = defaultContext;
        setFlags();
        
        myPayloadLength = new AgentXUInt32(0);
    }
    
    /**
     * A copy-ish constructor primarily used to create the header for an AgentX-Response-PDU.  
     * All the data from the provided header will be used in the new one with the exception of
     * the type.
     * 
     * @param type the type of PDU the copied header will represent.  
     * @param header the old header being used to populate the majority of the fields in the new header.
     */
    public AgentXHeader(ProtocolDataUnitType type, AgentXHeader header)
    {
        myVersion = VERSION_1; // Only Version 1 is supported         
        myPduType = type.getTypeValue();
        
        mySessionId = header.mySessionId;
        myTransactionId = header.myTransactionId;
        myPacketId = header.myPacketId;
        
        isBigEndian = header.isBigEndian;
        isDefaultContext = header.isDefaultContext;
        setFlags();
        
        myPayloadLength = header.myPayloadLength;        
    }
    
    /**
     * Decodes a byte array containing the <code>AgentXHeader</code> object.
     * 
     * @param stream raw AgentX header data to be parsed.
     * @return a valid AgentXHeader instance based on the decoded stream.
     * 
     * @throws AgentXProtocolException if the parsing fails for any reason.
     * @throws AgentXParseErrorException 
     */
    public static AgentXHeader decode(byte[] data) throws AgentXProtocolException, AgentXParseErrorException
    {
        return new AgentXHeader(data);        
    }

    /**
     * Encodes an <code>AgentXPDUHeader</code> object into Agent X protocol data. 
     * 
     * @param payloadLength the payload length calculated by the caller, typically a PDU instance.
     * @return an <code>AgentXByteStream</code> with the encoded <code>AgentXPDUHeader</code> object.
     */
    public byte[] encode(int payloadLength)
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(HEADER_LENGTH);

        data.write(myVersion);
        data.write(myPduType);
        data.write(myFlags);
        data.write(reserved);
        
        data.write(mySessionId.encode(), 0, mySessionId.getLength());
        data.write(myTransactionId.encode(), 0, myTransactionId.getLength());
        data.write(myPacketId.encode(),  0, myPacketId.getLength());

        myPayloadLength = new AgentXUInt32(payloadLength);
        data.write(myPayloadLength.encode(),  0, myPayloadLength.getLength());
        
        return data.toByteArray();
    }
    
    /**
     * Retrieve the endianess to be used for encoding.
     * 
     * @return true if big endian or network byte order is to be use, false otherwise.
     */
    public boolean getIsBigEndian()
    {
        return isBigEndian;
    }
    
    /**
     * Retrieve whether or not the default context will be included in the payload. If the default
     * context is being used, then there is no context to be encoded or decoded in the AgentX PDUs.
     *  
     * @return true if the default context will be included, false otherwise.
     */
    public boolean getIsDefaultContext()
    {
        return isDefaultContext;
    }
    
    /**
     * Retrieve the stored payload length.  This method will not calculate the payload length as 
     * it is the responsibility of the PDU implementation to do so.  This method only allows for 
     * the retrieval of the stored value if the header was constructed from a byte stream.
     * 
     * @return the number of bytes that comprises the payload for this PDU.
     */
    public int getPayloadLength()
    {
        return (int)myPayloadLength.getValue();
    }
    
    /**
     * Retrieves the stored session ID value.
     * 
     * @return the session ID as stored in the header.
     */
    public long getSessionId()
    {
        return mySessionId.getValue();
    }
    
    /**
     * Retrieves the stored packet ID value.
     * 
     * @return the packet ID as stored in the header.
     */
    public long getPacketId() 
    {
        return myPacketId.getValue();
    }
        
    /**
     * Retrieves the transaction ID value.
     * 
     * @return the stored transaction ID value.
     */
    public long getTransactionId() 
    {
        return myTransactionId.getValue();
    }

    /*
     * This method is called only by the constructor to initialize the bit values in the flags byte.
     * It is assumed that myFlags has not previously been modified. 
     */
    private void setFlags()
    {
        /*
         * Set the network byte order bit if the byte ording is big endian, otherwise ensure the bit is cleard.
         */
        myFlags |= (isBigEndian) ? AGENTX_NETWORK_BYTE_ORDER : 0x00;
        
        /*
         * Clear the bit if the payload is using a default context.  Otherise the bit is set.
         */
        myFlags |= (isDefaultContext) ? 0x00 : AGENTX_NON_DEFAULT_CONTEXT;
    }

}