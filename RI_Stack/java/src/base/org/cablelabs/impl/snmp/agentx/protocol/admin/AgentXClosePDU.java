/*
 * @(#)AgentXClosePDU.java									v1.0	2000/03/01
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

import org.cablelabs.impl.snmp.agentx.AgentXCloseReason;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;

/**
 * The <code>AgentXClosePDU</code> class implements an AgentX Close PDU.
 * <p>
 * The structure of a <code>AgentXClosePDU</code> object is as follows: <br>
 * <p>
 * <blockquote> 
 *      <li><code>Header</code>: AgentX PDU Header. <br> </li> 
 *      <li><code>Reason</code>: An enumerated value that gives the reason that the master agent or subagent closed 
 *                               the AgentX session. This field may take one of the following values: <br> 
 *      </li>
 *      <blockquote> 
 *          <code>reasonOther(1)</code> - None of the following reasons <br>
 *          <code>reasonParseError(2)</code> - Too many AgentX parse errors from peer <br>
 *          <code>reasonProtocolError(3)</code> - Too many AgentX protocol errors from peer <br>
 *          <code>reasonTimeouts(4)</code> - Too many timeouts waiting for peer <br>
 *          <code>reasonShutdown(5)</code> - Sending entity is shutting down <br>
 *          <code>reasonByManager(6)</code> - Due to Set operation; this reason code can be used only by the 
 *                                            master agent, in response to an SNMP management request. <br>
 *      </blockquote> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXClosePDU
{
    private AgentXHeader myHeader;
    private byte myReason;

    private final static byte[] RESERVED_BYTES = { 0, 0, 0 };
    private final static int REASON_PLUS_RESERVED_LENGTH = 4;

    /**
     * Constructs a <code>AgentXClosePDU</code> object from the parameters specified.
     * 
     * @param header the AgentX PDU header for this <code>AgentXClosePDU</code> object.
     * @param reason the AgentXCloseReason defined reason for closing the session.
     */
    public AgentXClosePDU(AgentXHeader header, AgentXCloseReason reason)
    {
        myHeader = header;
        myReason = reason.getValue();
    }

    /**
     * Encodes this <code>AgentXClosePDU</code> object into an AgentX encoded byte array. 
     * 
     * @return an <code>AgentXByteStream</code> with the encoded <code>AgentXClosePDU</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + REASON_PLUS_RESERVED_LENGTH);
        
        data.write(myHeader.encode(REASON_PLUS_RESERVED_LENGTH), 0, AgentXHeader.HEADER_LENGTH);                
        data.write(myReason);        
        data.write(RESERVED_BYTES, 0, RESERVED_BYTES.length);
        
        return data.toByteArray();
    }
}
