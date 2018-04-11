/*
 * @(#)AgentXGetBulkPDU.java									v1.0	2000/03/01
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

package org.cablelabs.impl.snmp.agentx.protocol.requests;

import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXProtocolException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXResponseError;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.types.AgentXOctetString;
import org.cablelabs.impl.snmp.agentx.types.AgentXOid;
import org.cablelabs.impl.snmp.agentx.types.AgentXSearchRange;
import org.cablelabs.impl.snmp.agentx.types.AgentXUInt16;
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXSearchRangeList;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * The <code>AgentXGetBulkPDU</code> class implements an AgentX GetBulk PDU.
 * <p>
 * The structure of a <code>AgentXGetBulkPDU</code> object is as follows: <br>
 * <p>
 * <blockquote>
 *      <li><code>Header</code>: AgentX PDU Header. <br></li>
 *      <li><code>Context</code>: An optional non-default context. <br></li>
 *      <li><code>NonRepeaters</code>: The number of variables in the SearchRangeList that are not repeaters.</li>
 *      <li><code>MaxRepetitions</code>: The maximum number of repetitions requested for repeating variables.</li>
 *      <li><code>SearchRangeList</code>: A SearchRangeList containing the requested variables for this session.</li> 
 *</blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXGetBulkPDU extends AgentXGetNextPDU
{
    private AgentXUInt16 myNonRepeaters;
    private AgentXUInt16 myMaxRepetitions;

    private final ProtocolDataUnitHandler normalGetBulkHandler = new ProtocolDataUnitHandler()
    {
        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.snmp.agentx.protocol.requests.AgentXGetNextPDU#handlePDURequest
         *      (org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
         *       org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
         *       org.cablelabs.impl.snmp.agentx.util.AgentXTransaction)
         */
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                                  MIBDelegate mib, 
                                                  AgentXTransactionManager transactionMgr)
        {
            /*
             * A maximum of N + (M * R) VarBinds are returned, where N equals
             * g.non_repeaters, M equals g.max_repetitions, and R is (number of
             * SearchRanges in the GetBulk request) - N.
             * 
             * @see RFC2741 section 7.2.3.3. Subagent Processing of the
             * agentx-GetBulk-PDU
             */
            short errorOccurred = AgentXResponseError.NOAGENTXERROR;
            final short errorIndex = 0;
            AgentXVarbindList varbindList = null;
            
            try
            {
                varbindList = new AgentXVarbindList();
                final AgentXSearchRangeList searchRangeList = getSearchRangeList();
                final int nonRepeaters = myNonRepeaters.getValue();
                final int remainingSearchRanges = searchRangeList.size() - nonRepeaters;
                
                if(remainingSearchRanges < 0)
                {
                    throw new SNMPBadValueException();
                }
                
                /* 
                 * Do the GetNext operation for all the nonRepeaters, and if the Max repetitions is greater then 0 
                 * we'll do the first iteration over our "R" (remaining) Search Ranges as well
                 *  
                 * NOTE: We do this here, not in the next loop because the FIRST iteration over "R" uses the 
                 *       OIDs sent from the master.  
                 */
                final int maxIterations = nonRepeaters + (myMaxRepetitions.getValue() > 0 ? remainingSearchRanges : 0);

                for (int index = 0; index < maxIterations; index++)
                {
                    final AgentXSearchRange searchRange = searchRangeList.getValueAt(index);
                    varbindList.add(getNext(mib, searchRange));
                }

                if ((myMaxRepetitions.getValue() > 0) && (remainingSearchRanges > 0))
                {
                    /*
                     * Now if max repetitions is greater then one we'll perform the remaining iterations over "R"
                     */
                    for (int i = 1; i < myMaxRepetitions.getValue(); i++)
                    {
                        int endOfMibViewCounter = 0;
                        
                        for (int s = 0; s < remainingSearchRanges; s++)
                        {
                            /*
                             * We need to grab the End OID from the original search range
                             */
                            final AgentXSearchRange originalSearchRange = searchRangeList.getValueAt(nonRepeaters + s);
                            
                            /* 
                             * To find i-th lexicographical successor to an OID N, we can call getNext on the
                             * (i-1)th OID which we've already looked up and stored in the VarbindList
                             */
                            final AgentXVarbind previousVarbind 
                                = varbindList.getValueAt(nonRepeaters + s + ((i - 1) * remainingSearchRanges));
                            
                            final AgentXOid previousOid = new AgentXOid(previousVarbind.getName());
                            
                            /* 
                             * Create a new Search range from the OID we just looked up and the end OID
                             * from the original search range
                             */
                            final AgentXSearchRange searchRange 
                                = new AgentXSearchRange(previousOid, originalSearchRange.getSecondValue());
                            /*
                             * Now we can just call getNext like normal
                             */
                            final AgentXVarbind varbind = getNext(mib, searchRange);
                            varbindList.add(varbind);

                            /*
                             * And keep track whenever we get an END_OF_MIB_VIEW as
                             * this may result in terminating the loop early
                             */
                            if (varbind.getType() == AgentXVarbind.END_OF_MIB_VIEW)
                            {
                                endOfMibViewCounter++;
                            }
                        }
                        
                        /*
                         * Further iterative processing should stop if the value from every getNext call in an  
                         * iteration is 'endOfMibView'.
                         * 
                         * @see RFC2741 section 7.2.3.3. Subagent Processing of the
                         * agentx-GetBulk-PDU
                         */
                        if (endOfMibViewCounter == remainingSearchRanges)
                        {
                            break;
                        }
                    }
                }
            }
            catch (SNMPBadValueException e)
            {
                /*
                 * If processing should fail for any reason not described below,
                 * res.error is set to 'genErr',
                 * 
                 * @see RFC2741 section 7.2.3.3. Subagent Processing of the
                 * agentx-GetBulk-PDU
                 */
                errorOccurred = AgentXResponseError.GENERR;
            }
            
            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };
    
    /**
     * Constructs a <code>AgentXGetBulkPDU</code> object from the AgentX encoded data.
     * 
     * @param data this <code>byte</code> array represents an encoded <code>AgentXGetBulkPDU</code> object.
     * @throws AgentXProtocolException if the data isn't a valid encoding of <code>AgentXGetBulkPDU</code>.
     */
    private AgentXGetBulkPDU(byte[] data) throws AgentXProtocolException
    {
        int index = 0;

        try
        {
            myHandler = noHeaderHandler;
            index += decodeHeader(data);
            myHandler = parseErrorHandler;
            index += decodeContext(data, index);

            myNonRepeaters = AgentXUInt16.decode(data, index);
            index += AgentXUInt16.NUM_BYTES_IN_UINT16;

            myMaxRepetitions = AgentXUInt16.decode(data, index);
            index += AgentXUInt16.NUM_BYTES_IN_UINT16;

            decodeSearchRangeList(data, index);
            myHandler = normalGetBulkHandler;
        }
        catch (AgentXParseErrorException e)
        {
            /*
             * Nothing needs to be done here other than eating the exception.  The noHeaderHandler will
             * ensure that the PDU is handled correctly.
             */
        }
    }

    /**
     * Constructor for testing purposes only. DO NOT USE!
     * 
     * @param context - the context to use in a test
     * @param searchRangeList - the list of search ranges to use in a test 
     * @param nonRepeaters - test value.
     * @param maxrepetitions - test value.
     */
    protected AgentXGetBulkPDU(AgentXHeader header, 
                               AgentXOctetString context, 
                               AgentXSearchRangeList searchRangeList, 
                               int nonRepeaters, 
                               int maxrepetitions)
    {
        super(header, context, searchRangeList);
        
        myNonRepeaters = new AgentXUInt16(nonRepeaters);
        myMaxRepetitions = new AgentXUInt16(maxrepetitions);  
        myHandler = normalGetBulkHandler;
    }

    /**
     * Decodes a byte array containing the <code>AgentXGetBulkPDU</code> object.
     * 
     * @param stream the encoded <code>AgentXGetBulkPDU</code> object.
     * @returns a properly constructed instance of the <code>AgentXGetBuildPDU</code>
     * 
     * @throws AgentXProtocolException if the <code>stream</code> is not valid encoded <code>AgentXGetBulkPDU</code>.
     */
    public static AgentXGetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXGetBulkPDU(stream);
    }

    /**
     * Returns the Payload Length of the <code>AgentXGetBulkPDU</code>.
     * 
     * @return the length of this <code>AgentXGetBulkPDU</code> payload.
     */
    protected int getPayloadLength()
    {
        int sum = super.getPayloadLength();
        sum += myNonRepeaters.getLength();
        sum += myMaxRepetitions.getLength();
        return sum;
    }
}
