/*
 * @(#)AgentXNotifyPDU.java									v1.0	2000/03/01
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

import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;

import org.cablelabs.impl.snmp.agentx.types.AgentXOctetString;
import org.cablelabs.impl.snmp.agentx.types.AgentXOid;
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;

/**
 * The <code>AgentXNotifyPDU</code> class implements an AgentX Notify PDU.
 * <p>
 * The structure of a <code>AgentXNotifyPDU</code> object is as follows: <br>
 * <p>
 * <blockquote> 
 *      <li><code>Header</code>: AgentX Pdu Header. <br></li> 
 *      <li><code>Context</code>: An optional non-default context. <br></li> 
 *      <li><code>VarBindList</code>: A VarBindList whose contents define the actual PDU to be sent.<br> 
 *              Its contents have the following restrictions:<br> </li>
 *                  - If the subagent supplies sysUpTime.0, it must be present as the first varbind.<br>
 *                  - snmpTrapOID.0 must be present, as the second varbind if sysUpTime.0 was supplied, as the first 
 *                    if it was not.<br>
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXNotifyPDU
{    
    /*
     * This is a well-known OID string value defined by SNMPv2 for notifications.
     */
    private static final String SNMP_TRAP_OID = "1.3.6.1.6.3.1.1.4.1.0";
    
    private AgentXHeader myHeader;
    private AgentXOctetString myContext;    
    private AgentXVarbindList myVarbindList;

    /**
     * Constructs a <code>AgentXNotifyPDU</code> object from the parameters specified.
     * 
     * @param header a header for <code>AgentXNotifyPDU</code>'s.
     * @param context the context for the session if it is not the default.
     * @param varbindList the list of OID,data pairs being sent with this notification.
     * @throws SNMPBadValueException if the trapOID cannot be encoded properly.
     */
    public AgentXNotifyPDU(AgentXHeader header, String context, String trapOID, AgentXVarbindList varbindList) 
        throws SNMPBadValueException
    {        
        myHeader = header;
        if (context != null)
        {
            myContext = new AgentXOctetString(context);
        }
        
        /*
         * Ensure that the snmpTrapOID.0 is the first value in the varbindlist as 
         * per RFC 2257 6.2.10  The agentx-Notify-PDU
         */
        AgentXVarbind snmpTrapOID = new AgentXVarbind(SNMP_TRAP_OID, 
                                                      AgentXVarbind.OBJECT_IDENTIFIER, 
                                                      new AgentXOid(trapOID));
        varbindList.add(0, snmpTrapOID);
        
        myVarbindList = varbindList;        
    }

    /**
     * Encode the PDU into the AgentX protocol format.
     * 
     * @return a raw byte array of the PDU formatted such that the master can interpret it.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + getPayloadLength());
        
        data.write(myHeader.encode(getPayloadLength()), 0, AgentXHeader.HEADER_LENGTH);        
        
        if (myContext != null)
        {
            data.write(myContext.encode(), 0, myContext.getLength());
        }
            
        data.write(myVarbindList.encode(), 0,  myVarbindList.getLength());
        return data.toByteArray();
    }

    /*
     * Returns the Payload Length of the <code>AgentXNotifyPDU</code>.
     */
    private int getPayloadLength()
    {
        int sum = 0;
        
        if (myContext != null)
        {
            sum += myContext.getLength();
        }
        
        sum += myVarbindList.getLength();        
        return sum;
    }
}
