/*
 * @(#)AgentXUnregisterPDU.java									v1.0	2000/03/01
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
 * The <code>AgentXUnregisterPDU</code> class implements an AgentX Unregister PDU.
 * <p>
 * The structure of a <code>AgentXUnregisterPDU</code> object is as follows: <br>
 * <p>
 * <blockquote>
 *      <li><code>Header</code>: AgentX Pdu Header. <br></li> 
 *      <li><code>Context</code>: An optional non-default context. <br></li> 
 *      <li><code>Priority</code>: The priority at which this region was originally registered. <br></li>
 *      <li><code>SubTree</code>: Indicates a previously-registered region of the MIB that a subagent no 
 *                  longer wishes to support. <br></li> 
 *      <li><code>RangeSubId</code>: Indicates a sub-identifier in u.subtree is a range lower bound. <br></li> 
 *      <code><li>UpperBound</code>: The upper bound of the range sub-identifier. This field is present 
 *                  in the PDU only if u.rangesubid is not 0. <br> </li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXUnregisterPDU
{
    private static final byte RESERVED = 0;

    private byte myPriority;
    private byte myRangeSubId;
    
    private AgentXHeader myHeader;
    private AgentXOctetString myContext;
    private AgentXOid mySubTree;
    private AgentXUInt32 myUpperBound;

    /**
     * Constructs a <code>AgentXUnregisterPDU</code> object from the parameters specified.
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
    public AgentXUnregisterPDU(AgentXHeader header, 
                               String subTreeOid,
                               String context, 
                               byte priority, 
                               byte rangeSubId, 
                               int upperBound)    
    {
        myHeader = header;
        
        if (context != null)
        {
            myContext = new AgentXOctetString(context);
        }

        myPriority = priority;
        
        mySubTree = new AgentXOid(subTreeOid);
        
        myRangeSubId = rangeSubId;
        
        if (myRangeSubId == 0)
        {
            myUpperBound = null;
        }
        else
        {
            myUpperBound = new AgentXUInt32(upperBound);
        }
    }
    
    /**
     * Encodes an <code>AgentXUnregisterPDU</code> object into an AgentX data stream. 
     * 
     * @return an <code>AgentXByteStream</code> with the encoded <code>AgentXUnregisterPDU</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream(AgentXHeader.HEADER_LENGTH + getPayloadLength());
        
        data.write(myHeader.encode(getPayloadLength()), 0, AgentXHeader.HEADER_LENGTH);        
        
        data.write(RESERVED);
        data.write(myPriority);
        data.write(myRangeSubId);
        data.write(RESERVED);
        data.write(mySubTree.encode(), 0, mySubTree.getLength());
        
        if (myRangeSubId != 0)
        {
            data.write(myUpperBound.encode(), 0, myUpperBound.getLength());
        }
        
        return data.toByteArray();
    }        

    /*
     * Returns the Payload Length of the <code>AgentXUnregisterPDU</code>.
     */
    private int getPayloadLength()
    {
        final int RESERVED_LENGTH = 1;
        final int PRIORITY_LENGTH = 1;
        final int SUB_RANGE_ID_LENGTH = 1;
        
        int sum = RESERVED_LENGTH + PRIORITY_LENGTH + SUB_RANGE_ID_LENGTH + RESERVED_LENGTH;        

        if (myContext != null)
        {
            sum += myContext.getLength();
        }

        sum += mySubTree.getLength(); 
                
        if (myRangeSubId != 0)
        {
            sum += myUpperBound.getLength(); 
        }
        
        return sum;
    }
}
