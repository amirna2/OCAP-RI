package org.cablelabs.impl.snmp.agentx.protocol;

import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * Any PDU that implements this interface is intended to do work on the MIB at the request of the the
 * master agent and return a response.
 * 
 * @author Kevin Hendry
 */
public interface ProtocolDataUnitHandler
{
    /**
     * Process the Protocol Data unit that this Object represents and generate a AgentXResponsePDU where appropriate
     * 
     * @param pduFactory The factory to use for creating new PDUs 
     * @param mib The MIB Delegate which bridges the interface between the AgentX code and the SNMP MIB impl
     * @param stateData An Object containing any state information that this PDU may need to set or process
     * 
     * @return A new AgentXResponsePDU object or null
     * 
     * @throws AgentXProtocolException An exception occurred that prevented the Response from being generated
     */
    AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                       MIBDelegate mib, 
                                       AgentXTransactionManager transactionMgr);
}
