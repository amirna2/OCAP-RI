/*
 * @(#)AgentXTestSetPDU.java									v1.0	2000/03/01
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
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransaction;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * The <code>AgentXTestSetPDU</code> class implements an AgentX TestSet PDU.
 * 
 * The structure of a <code>AgentXTestSetPDU</code> object is as follows: <br>
 * <blockquote>
 *      <li><code>Header</code>: AgentX Pdu Header.</li>
 *      <li><code>Context</code>: An optional non-default context.</li>
 *      <li><code>VarBindList</code>: A VarBindList containing the requested VarBinds for this subagent. </li>
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXTestSetPDU extends ProtocolDataUnit
{
    private AgentXOctetString myContext;
    private AgentXVarbindList myVarbindList;

    private final ProtocolDataUnitHandler testSetRequestHandler = new ProtocolDataUnitHandler()
    {
        /*
         * Behavior defined in RFC2741 section 7.2.4.1. Subagent Processing of the
         * Agentx-TestSet-PDU
         * 
         * @see org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnit#handlePDURequest
         *      (org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
         *       org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
         *       org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager)
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
                for (short i = 0; i < myVarbindList.size(); i++)
                {
                    /*
                     * Upon the subagent's receipt of an agentx-TestSet-PDU,
                     * each VarBind in the PDU is validated until they are all
                     * successful, or until one fails
                     * 
                     * @see RFC2741 section 7.2.4.1. Subagent Processing of the
                     * Agentx-TestSet-PDU
                     */
                    final AgentXVarbind varbind = myVarbindList.getValueAt(i);

                    /*
                     * As a result of this validation step, an
                     * agentx-Response-PDU is sent in reply whose res.error
                     * field is set to one of the following SNMPv2 PDU
                     * error-status values.
                     * 
                     * noAccess (6), If this value is not `noError', the
                     * res.index field must be set to the index of the VarBind
                     * for which validation failed.
                     * 
                     * wrongType (7), If this value is not `noError', the
                     * res.index field must be set to the index of the VarBind
                     * for which validation failed.
                     * 
                     * @see RFC2741 section 7.2.4.1. Subagent Processing of the
                     * Agentx-TestSet-PDU
                     */

                    final int testErrorCode = mib.testSet(varbind.getName(), varbind.getValue());
                    if (testErrorCode != AgentXResponseError.NOAGENTXERROR)
                    {
                        errorOccurred = (short) testErrorCode;
                        errorIndex = (short) (i + 1);
                        break;
                    }
                }

            }
            catch (SNMPBadValueException e)
            {
                e.printStackTrace();
                errorOccurred = AgentXResponseError.PARSEERROR;
            }
            
            final AgentXTransaction trans = transactionMgr.startTransaction(myHeader.getTransactionId());
            trans.setCommitData(myVarbindList);
            
            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };

    /**
     * Decodes a byte array containing the <code>AgentXTestSetPDU</code> object.
     * 
     * @param stream the data to be parsed.
     * 
     * @return a properly constructed instance of an AgentXTestSetPDU
     * 
     * @throws AgentXProtocolException if an error occurred during parsing.
     */
    public static AgentXTestSetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXTestSetPDU(stream);
    }

    /**
     * Constructs a <code>AgentXTestSetPDU</code> object from the parameters specified.
     * 
     * @param data raw AgentX Test Set PDU data stream to be parsed.
     * 
     * @throws AgentXProtocolException if the stream is invalid in any way.
     */
    private AgentXTestSetPDU(byte[] data) throws AgentXProtocolException
    {
        int index = 0;
        int payloadLength = 0;

        try
        {
            myHandler = noHeaderHandler;
            myHeader = AgentXHeader.decode(data);
            myHandler = parseErrorHandler;
            index += AgentXHeader.HEADER_LENGTH;

            if (!myHeader.getIsDefaultContext())
            {
                myContext = AgentXOctetString.decode(data, index);
                index += myContext.getLength();
                payloadLength = myContext.getLength();
            }

            myVarbindList = AgentXVarbindList.decode(data, index, myHeader.getPayloadLength() - payloadLength);
            myHandler = testSetRequestHandler;
        }
        catch (AgentXParseErrorException e)
        {
            /*
             * Nothing needs to be done here other than eating the exception.  The noHeaderHandler will
             * ensure that the PDU is handled correctly.
             */
        }
    }

}
