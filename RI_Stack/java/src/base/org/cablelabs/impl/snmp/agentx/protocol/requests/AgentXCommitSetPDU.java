/*
 * @(#)AgentXCommitSetPDU.java									v1.0	2000/03/01
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
import org.ocap.diagnostics.MIBObject;

/**
 * The <code>AgentXCommitSetPDU</code> class implements an AgentX CommitSet PDU.
 * 
 * The structure of a <code>AgentXCommitSetPDU</code> object is as follows: <br>
 * <blockquote> 
 *      <code><li>Header</code>: AgentX Pdu Header.</li> 
 * </blockquote>
 * 
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXCommitSetPDU extends ProtocolDataUnit
{
    private final ProtocolDataUnitHandler normalCommitSetHandler = new ProtocolDataUnitHandler()
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
            final AgentXTransaction trans = transactionMgr.getTransaction(myHeader.getTransactionId());
            final AgentXVarbindList commitList = trans.getCommitData();
            final AgentXVarbindList undoList = new AgentXVarbindList();
            
            try
            { 
                for (short i = 0; i < commitList.size(); i++)
                {
                    AgentXVarbind varbind = commitList.getValueAt(i);

                    /*
                     * We need to store the current value before we commit a new one
                     * In case we need to undo later. Do it here, rather than in the
                     * TestSet so we don't waste resources creating an Undo list we
                     * Don't end up needing
                     * 
                     * @see RFC2741 section 7.2.4.2. Subagent Processing of the
                     * agentx-CommitSet-PDU
                     */
                    final String oid = varbind.getName();
                    final MIBObject undoMap = mib.get(oid);
                    
                    if (mib.set(oid, varbind.getValue()) != true)
                    {
                        /*
                         * The subagent sends in reply an agentx-Response-PDU whose
                         * res.error field is set to one of the following SNMPv2 PDU
                         * error-status values (see section 3, "Definitions", in RFC
                         * 1905 [13]): noError (0), commitFailed (14) If this value
                         * is `commitFailed', the res.index field must be set to the
                         * index of the VarBind (as it occurred in the
                         * agentx-TestSet-PDU) for which the operation failed.
                         * Otherwise res.index is set to 0.
                         * 
                         * @see RFC2741 section 7.2.4.2. Subagent Processing of the
                         * agentx-CommitSet-PDU
                         */
                        errorOccurred = AgentXResponseError.COMMITFAILED;
                        errorIndex = (short) (i + 1);
                        break;
                    }
                    undoList.add(new AgentXVarbind(oid, undoMap.getData()));
                }
            }
            catch (SNMPBadValueException e)
            {
                errorOccurred = AgentXResponseError.PARSEERROR;
            }
            
            trans.setUndoData(undoList);
            
            return pduFactory.createResponsePDU(myHeader, errorOccurred, errorIndex, varbindList);
        }
    };

    /**
     * Decodes a byte array containing the <code>AgentXCommitSetPDU</code> object.
     * 
     * @param stream the encoded <code>AgentXCommitSetPDU</code> object.
     * @return a properly constructed instance of the <code>AgentXCommitSetPDU</code>
     * 
     * @throws AgentXProtocolException if the <code>byte</code> array is not a valid encoding.
     */
    public static AgentXCommitSetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXCommitSetPDU(stream);
    }

    /**
     * Constructs a <code>AgentXCommitSetPDU</code> object from the parameters specified.
     * 
     * @param data the encoded <code>AgentXCommitSetPDU</code> object.
     * @throws AgentXProtocolException if the <code>byte</code> array is not a valid encoding.
     */
    private AgentXCommitSetPDU(byte[] data) throws AgentXProtocolException
    {
        try
        {
            myHandler = noHeaderHandler;
            myHeader = AgentXHeader.decode(data);
            myHandler = normalCommitSetHandler;
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
