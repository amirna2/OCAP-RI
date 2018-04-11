/*
 * @(#)AgentXGetPDU.java									v1.0	2000/03/01
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
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnit;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.types.AgentXOctetString;
import org.cablelabs.impl.snmp.agentx.types.AgentXSearchRange;
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXSearchRangeList;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;
import org.ocap.diagnostics.MIBObject;

/**
 * The <code>AgentXGetPDU</code> class implements an AgentX Get PDU.
 * 
 * The structure of a <code>AgentXGetPDU</code> object is as follows: <br>
 * <blockquote>
 *      <li><code>Header</code>: AgentX PDU Header.</li>
 *      <li><code>Context</code>: An optional non-default context.</li> 
 *      <li><code>SearchRangeList</code>: A SearchRangeList containing the requested variables for this session. 
 *                                        Within the agentx-Get-PDU, the Ending OIDs within SearchRanges are 
 *                                        null-valued Object Identifiers. <br>
 *      </li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXGetPDU extends ProtocolDataUnit
{
    private AgentXOctetString myContext;
    private AgentXSearchRangeList mySearchRangeList;

    protected final ProtocolDataUnitHandler getRequestHandler = new ProtocolDataUnitHandler()
    {
        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler#handlePDURequest
         *          (org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
         *           org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
         *           org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager)
         */
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                                  MIBDelegate mib,
                                                  AgentXTransactionManager transactionMgr)
        {
            short errorOccurred = AgentXResponseError.NOAGENTXERROR;
            short errorIndex = 0;
            
            AgentXVarbindList varbindList = null;
            try
            {
                varbindList = new AgentXVarbindList();
                for (int index = 0; index < mySearchRangeList.size(); index++)
                {
                    final AgentXSearchRange searchRange = mySearchRangeList.getValueAt(index);
                    final MIBObject value = mib.get(searchRange.getFirstValue().getValue());

                    /*
                     * If the starting OID exactly matches the name of a variable
                     * instantiated by this subagent within the indicated context
                     * and session, v.type and v.data are encoded to represent the
                     * variable's syntax and value, as described in section 5.4,
                     * "Value Representation".
                     * 
                     * @see RFC2741 section 7.2.3.1. Subagent Processing of the
                     * agentx-Get-PDU
                     */
                    varbindList.add(new AgentXVarbind(value.getOID(), value.getData()));
                }
            }
            catch (SNMPBadValueException e)
            {
                /*
                 * If processing should fail for any reason not described below,
                 * res.error is set to `genErr',
                 * 
                 * @see RFC2741 section 7.2.3.1. Subagent Processing of the
                 * agentx-Get-PDU
                 */
                errorOccurred = AgentXResponseError.GENERR;
            }

            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };

    /**
     * Constructs a <code>AgentXGetPDU</code> object from the parameters specified.
     * 
     * @param stream data representing an encoded <code>AgentXGetPDU</code> object.
     * @throws AgentXProtocolException if the <code>data</code> is not valid encoded <code>AgentXGetPDU</code>.
     */
    protected AgentXGetPDU(byte[] data) throws AgentXProtocolException
    {
        try
        {
            int index = 0;
            myHandler = noHeaderHandler;
            index += decodeHeader(data);
            myHandler = parseErrorHandler;
            index += decodeContext(data, index);
            decodeSearchRangeList(data, index);
            myHandler = getRequestHandler;
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
     * Default empty constructor, used for inheritance only.
     */
    protected AgentXGetPDU(){ }
    
    /**
     * Constructor for testing purposes only. DO NOT USE!
     * 
     * @param context - the context to use in a test
     * @param searchRangeList - the list of search ranges to use in a test 
     */
    protected AgentXGetPDU(AgentXHeader header, AgentXOctetString context, AgentXSearchRangeList searchRangeList)
    {
        myHeader = header;
        myContext = context;
        mySearchRangeList = searchRangeList;
        myHandler = getRequestHandler;
    }

    /**
     * Used to set this class's private AgentXContext object
     * 
     * @param data the AgentX encoded context info
     * @param offset the offset into the byte array for decoding the context
     * 
     * @return the length of the context section in bytes.
     * 
     * @throws AgentXParseErrorException if any error occurs while decoding the data.
     */
    protected int decodeContext(byte[] data, int offset) throws AgentXParseErrorException
    {
        int length = 0;
        
        /*
         * If we are using the default context, then there is no context encoding to be read.
         */
        if (!myHeader.getIsDefaultContext())
        {
            myContext = AgentXOctetString.decode(data, offset);
            length = myContext.getLength();
        }
        
        return length;
    }

    /**
     * Used to set this class's private AgentXHeader object
     * 
     * @param data encoded data containing the Header info
     * @param offset the offset into the byte array for decoding the Header
     * 
     * @return the length of the header in bytes.
     * 
     * @throws AgentXParseErrorException if there is a parsing error while attempting to decode the data.
     * @throws AgentXProtocolException if there is a protocol violation detected while decoding the data.
     */
    protected int decodeHeader(byte[] data) throws AgentXParseErrorException, AgentXProtocolException
    {
        myHeader = AgentXHeader.decode(data);
        return AgentXHeader.HEADER_LENGTH;
    }

    /**
     * Decodes the search range list from the provided stream of data.
     * 
     * @param data the byte array containing the SearchRangeList info.
     * @param offset the offset into the byte array for decoding the SearchRangeList
     * 
     * @throws AgentXParseErrorException if there is a parsing error while attempting to decode the data.
     */
    protected void decodeSearchRangeList(byte[] data, int offset) throws AgentXParseErrorException
    {
        final int numOfBytes = myHeader.getPayloadLength() - this.getPayloadLength();
        mySearchRangeList = AgentXSearchRangeList.decode(data, offset, numOfBytes);
    }

    /**
     * Decodes a byte array containing the <code>AgentXGetPDU</code> object.
     * 
     * @param stream the encoded <code>AgentXGetPDU</code> object.
     * @return an instance of AgentXGetPDU decoded from the stream.
     *  
     * @throws AgentXProtocolException if the stream is not a valid encoding of <code>AgentXGetPDU</code>.
     */
    public static AgentXGetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXGetPDU(stream);
    }

    /**
     * Returns the Payload Length of the <code>AgentXGetPDU</code>.
     * 
     * @return the size of the payload in bytes.
     */
    protected int getPayloadLength()
    {
        int sum = 0;
        if (myContext != null)
        {
            sum += myContext.getLength();
        }
        if (mySearchRangeList != null)
        {
            sum += mySearchRangeList.getLength();
        }
        return sum;
    }

    /**
     * Returns the search range list of the <code>AgentXGetPDU</code>.
     * 
     * @return the <code>AgentXSearchRangeList</code> included in the decoded PDU.
     */
    protected AgentXSearchRangeList getSearchRangeList()
    {
        return mySearchRangeList;
    }
}
