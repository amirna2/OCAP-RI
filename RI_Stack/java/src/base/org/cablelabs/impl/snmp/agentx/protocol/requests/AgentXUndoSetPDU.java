/*
 * @(#)AgentXUndoSetPDU.java									v1.0	2000/03/01
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
import org.cablelabs.impl.snmp.agentx.types.AgentXVarbind;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransaction;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * The <code>AgentXUndoSetPDU</code> class implements an AgentX UndoSet PDU. The structure of 
 * a <code>AgentXUndoSetPDU</code> object is as follows: <br>
 * <blockquote> 
 *      <li><code>Header</code>: AgentX PDU Header.</li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXUndoSetPDU extends ProtocolDataUnit
{
    private final ProtocolDataUnitHandler undoSetRequestHandler = new ProtocolDataUnitHandler()
    {
        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler#handlePDURequest
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
                /*
                 * The agentx-UndoSet-PDU indicates that the subagent should
                 * undo the management operation requested in a preceding
                 * CommitSet-PDU.
                 * 
                 * @see RFC2741 section 7.2.4.3. Subagent Processing of the agentx-UndoSet-PDU
                 */
                final AgentXTransaction trans = transactionMgr.getTransaction(myHeader.getTransactionId());
                final AgentXVarbindList undoList = trans.getUndoData();

                for (short i = 0; i < undoList.size(); i++)
                {
                    final AgentXVarbind varbind = undoList.getValueAt(i);
                    if (mib.set(varbind.getName(), varbind.getValue()) != true)
                    {
                        errorOccurred = AgentXResponseError.UNDOFAILED;
                        errorIndex = (short) (i + 1);
                        break;
                    }
                }

                /*
                 * This PDU also signals the end of processing of the management
                 * operation initiated by the previous TestSet-PDU. The subagent
                 * should release resources
                 * 
                 * @see RFC2741 section 7.2.4.3. Subagent Processing of the agentx-UndoSet-PDU
                 */                
            }
            catch (SNMPBadValueException e)
            {
                errorOccurred = AgentXResponseError.PARSEERROR;
            }
            
            transactionMgr.endTransaction(myHeader.getTransactionId());
            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };

    /**
     * Decodes a byte array containing the <code>AgentXUndoSetPDU</code> object.
     * 
     * @param stream raw AgentX Undo SET PDU data to be parsed.
     * 
     * @return a valid AgentXUndoSetPDU instance based on the decoded stream.
     * @throws AgentXProtocolException if the parsing fails for any reason.
     */
    public static AgentXUndoSetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXUndoSetPDU(stream);
    }

    /**
     * Constructs a <code>AgentXUndoSetPDU</code> object from data on the wire.
     * 
     * @param data raw AgentX Undo SET PDU stream to be parsed.
     * @throws AgentXProtocolException if the parsing fails for any reason.
     */
    private AgentXUndoSetPDU(byte[] data) throws AgentXProtocolException
    {
        try
        {
            myHandler = noHeaderHandler;
            myHeader = AgentXHeader.decode(data);
            myHandler = undoSetRequestHandler;
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
