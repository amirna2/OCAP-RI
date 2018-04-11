/*
 * @(#)AgentXOpenPDU.java									v1.0	2000/03/01
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
package org.cablelabs.impl.snmp.agentx.protocol.admin;

import java.io.ByteArrayOutputStream;

import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.types.AgentXOctetString;
import org.cablelabs.impl.snmp.agentx.types.AgentXOid;

/**
 * The <code>AgentXOpenPDU</code> class implements an AgentX Open PDU.
 * <p>
 * The structure of a <code>AgentXOpenPDU</code> object is as follows: <br>
 * <p>
 * <blockquote> 
 *      <li><code>Header</code>: AgentX Pdu Header. <br> </li> 
 *      <li><code>Timeout</code>: The length of time, in seconds, that a master agent should allow to elapse 
 *      after dispatching a message on a session before it regards the subagent as not responding. This is the 
 *      default value for the session, and may be overridden by values associated with specific registered
 *      MIB regions. The default value of 0 indicates that there is no session-wide default value. <br></li> 
 *      <li><code>OID</code>: An Object Identifier that identifies the subagent. Subagents that do not support 
 *      such an notion may send a null Object Identifier. <br></li> 
 *      <li><code>Description</code>: An Octet String containing a DisplayString describing the subagent. <br></li>
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXOpenPDU
{
    private byte myTimeout;
    private final static byte[] RESERVED_BYTES = { 0, 0, 0 };
    private AgentXOid myObjectId;
    private AgentXOctetString myDescription;
    private AgentXHeader myHeader;

    /**
     * Constructs a <code>AgentXOpenPDU</code> object from the parameters specified.
     * 
     * @param header the <code>AgentXHeader</code> object configured for this PDU.
     * @param description a display string describing the subagent.
     * @param oid an Object ID for the subagent.
     * @param timeout the length of time in seconds that a master should allow to elapse after dispatching a message
     *                on a session before it regards the subagent as not responding. A value of 0 indicates that there
     *                is no session wide default timeout value.
     */
    public AgentXOpenPDU(AgentXHeader header, String description, String oid, byte timeout)
    {
        myHeader = header;        
        myTimeout = timeout;
        myDescription = new AgentXOctetString(description);
        myObjectId = new AgentXOid(oid);
    }
    
    /* (non-Javadoc)
     * @see org.cablelabs.impl.snmp.agentx.pdu.AgentXProtocolDataUnitEncoder#encode()
     */
    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + getPayloadLength()); 

        data.write(myHeader.encode(getPayloadLength()), 0, AgentXHeader.HEADER_LENGTH);                
        data.write(myTimeout);
        data.write(RESERVED_BYTES, 0, RESERVED_BYTES.length);
        data.write(myObjectId.encode(), 0, myObjectId.getLength());
        data.write(myDescription.encode(), 0, myDescription.getLength());
        
        return data.toByteArray();
    }

    /*
     * Calculates the payload length for this <code>AgentXOpenPDU</code> instance in bytes.
     * @return the number of bytes in the AgentX encoded payload only. (i.e. it does not include the header)
     */
    private int getPayloadLength()
    {
        final int TIMEOUT_LENGTH = 1;
        
        int sum = TIMEOUT_LENGTH + RESERVED_BYTES.length; 
        sum += myObjectId.getLength(); 
        sum += myDescription.getLength();
        return sum;
    }
}
