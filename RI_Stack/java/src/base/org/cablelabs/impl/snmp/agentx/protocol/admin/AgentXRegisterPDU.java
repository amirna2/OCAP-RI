/*
 * @(#)AgentXRegisterPDU.java									v1.0	2000/03/01
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
import org.cablelabs.impl.snmp.agentx.types.AgentXUInt32;

/**
 * The <code>AgentXRegisterPDU</code> class implements an AgentX Register PDU.
 * <p>
 * The structure of a <code>AgentXRegisterPDU</code> object is as follows: <br>
 * <p>
 * <blockquote>
 *      <li><code>Header</code>: AgentX Pdu Header. <br> </li> 
 *      <li><code>Context</code>: An optional non-default context. <br> </li> 
 *      <li><code>Timeout</code>: The length of time, in seconds, that a master agent should allow to elapse after 
                            dispatching a message on a session before it regards the subagent as not 
 *                          responding. <br> </li> 
 *      <li><code>Priority</code>: A value between 0 and 255, used to achieve a desired configuration when different 
 *                          sessions register identical or overlapping regions. Subagents with no particular 
 *                          knowledge of priority should register with the default value of 127. <br> </li> 
 *      <li><code>SubTree</code>: An Object Identifier that names the basic subtree of a MIB region for which a 
 *                          subagent indicates its support. The term "subtree" is used generically here, it may 
 *                          represent a fully-qualified instance name, a partial instance name, a MIB table, an 
 *                          entire MIB, etc. <br> </li> 
 *      <li><code>RangeSubId</code>: Permits specifying a range in place of one of Subtree's sub-identifiers. If this 
 *                          value is 0, no range is being specified and there is no UpperBound field present 
 *                          in the PDU. In this case the MIB region being registered is the single subtree 
 *                          named by SubTree. <br> </li> 
 *      <li><code>UpperBound</code>: The upper bound of a sub-identifier's range. This field is present only if 
 *                          RangeSubId is not 0. <br> </li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXRegisterPDU
{
    private AgentXHeader myHeader;
    
    private byte myTimeout;
    private byte myPriority;
    private byte myRangeSubId;
    
    private static final byte RESERVED = 0;
    
    private AgentXOctetString myContext;
    private AgentXOid mySubtree;
    private AgentXUInt32 myUpperBound;

    /**
     * Constructs a <code>AgentXRegisterPDU</code> object set to the default contents.
     * 
     * @param header a header constructed to represent this PDU.
     * @param subTreeOid the OID used to define a basic sub tree of a MIB to be registered.
     * @param context the optional non-default context.  Typically null for this implementation.
     * @param timeout the length of time in seconds that a master should allow to elapse after dispatching a message
     *                on a session before it regards the subagent as not responding. A value of 0 indicates that there
     *                is no session wide default timeout value.
     * @param priority a value ranged between 1 and 255 to ensure desired configuration when different sessions 
     *                 register with identical or overlapping regions.  Subagents with no particular knowledge of 
     *                 priority should register with a value of 127. This is the default for this implementation.
     * @param rangeSubId specifies a range in place of one of the subTreeOid sub-identifiers.  If this value is 0, 
     *              no range is being specified & there is no upperBound field present in the PDU. In this case 
     *              the MIB region being registered is the single subtree named by r.subtree.
     * @param upperBound the upper bound of a sub ID's range. Required if the range is non-zero.  Otherwise, the value 
     *                   should be set to 0.
     */
    public AgentXRegisterPDU(AgentXHeader header, 
                             String subTreeOid,
                             String context, 
                             byte timeout, 
                             byte priority, 
                             byte rangeSubId,
                             int upperBound)
    {
        myHeader = header;
        
        if (context != null)
        {
            myContext = new AgentXOctetString(context);
        }
        
        myTimeout = timeout;
        myPriority = priority;

        mySubtree = new AgentXOid(subTreeOid);        

        myRangeSubId = rangeSubId;
        if (rangeSubId == 0)
        {
            myUpperBound = new AgentXUInt32(0);
        }
        else
        {
            myUpperBound = new AgentXUInt32(upperBound);
        }
    }

    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + getPayloadLength());
        
        data.write(myHeader.encode(getPayloadLength()), 0, AgentXHeader.HEADER_LENGTH);        
        
        if (myContext != null)
        {
            data.write(myContext.encode(), 0, myContext.getLength());
        }       

        data.write(myTimeout);
        data.write(myPriority);
        data.write(myRangeSubId);
        data.write(RESERVED);
        
        data.write(mySubtree.encode(), 0, mySubtree.getLength());        
        
        if (myRangeSubId != 0)
        {
            data.write(myUpperBound.encode(), 0, myUpperBound.getLength());
        }
        
        return data.toByteArray();
    }

    /*
     * Calculates the payload length of the <code>AgentXRegisterPDU</code> excluding the header in bytes.
     * @return the number of bytes in the AgentX encoded payload only. (i.e. it does not include the header)
     */
    private int getPayloadLength()
    {
        final int TIMEOUT_LENGHT = 1;
        final int PRIORITY_LENGHT = 1;
        final int RANGE_SUB_ID_LENGTH = 1;
        final int RESERVED_LENGTH = 1;
        
        int sum = 0;
        
        if (myContext != null)
        {
            sum += myContext.getLength();
        }
        
        sum += TIMEOUT_LENGHT + PRIORITY_LENGHT + RANGE_SUB_ID_LENGTH + RESERVED_LENGTH;         
        sum += mySubtree.getLength(); 
        
        if (myRangeSubId != 0)
        {
            sum += myUpperBound.getLength(); 
        }
        
        return sum;
    }
}
