/*
 * @(#)AgentXCleanupSetPDU.java									v1.0	2000/03/01
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
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXHeader;
import org.cablelabs.impl.snmp.agentx.protocol.AgentXProtocolException;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnit;
import org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * The <code>AgentXCleanupSetPDU</code> class implements an AgentX CleanupSet PDU.
 * The structure of a <code>AgentXCleanupSetPDU</code> object is as follows:
 * <blockquote>
 *      <li><code>Header</code>: AgentX PDU Header.</li> 
 * </blockquote> 
 *   
 * @author Pedro Pereira
 * @version 1.0, 2000/03/01
 */
public class AgentXCleanupSetPDU extends ProtocolDataUnit
{
    private final ProtocolDataUnitHandler normalCleanupSetHandler = new ProtocolDataUnitHandler()
    {
        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler#handlePDURequest
         *          (org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
         *           org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
         *           org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager)
         */
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, MIBDelegate mib,
                AgentXTransactionManager transactionMgr)
        {
            transactionMgr.endTransaction(myHeader.getTransactionId());
            return null;
        }
    };
    
    /**
     * Decodes a byte array containing the <code>AgentXCleanupSetPDU</code> object.
     * 
     * @param stream the encoded <code>AgentXCleanupSetPDU</code> object.
     * @return a properly constructed instance of the <code>AgentXCleanupSetPDU</code>
     * @throws AgentXProtocolException if the <code>byte</code> array is not a valid encoding.
     */
    public static AgentXCleanupSetPDU decode(byte[] stream) throws AgentXProtocolException
    {
        return new AgentXCleanupSetPDU(stream);
    }

    /**
     * Constructs a <code>AgentXCleanupSetPDU</code> raw AgentX encoded array.
     * 
     * @param data the AgentX encoded <code>AgentXCleanupSetPDU</code> object.
     * @throws AgentXProtocolException if the data is not a valid encoding of an AgentX CleanUp PDU.
     */
    private AgentXCleanupSetPDU(byte[] data) throws AgentXProtocolException
    {
        try
        {
            myHandler = noHeaderHandler;
            myHeader = AgentXHeader.decode(data);
            myHandler = normalCleanupSetHandler;
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
