package org.cablelabs.impl.snmp.agentx.protocol;

import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXCleanupSetPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXCommitSetPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXGetBulkPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXGetNextPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXGetPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXTestSetPDU;
import org.cablelabs.impl.snmp.agentx.protocol.requests.AgentXUndoSetPDU;

/**
 * This factory is used to construct an instance of the proper handler for the PDU request retrieved
 * from the master agent.
 * 
 * @author Kevin Hendry
 */
public class ProtocolDataUnitHandlerFactory
{    
    /**
     * Retrieve the proper handler for the request PDU sent from the master agent.
     * 
     * @param type the PDU type value.
     * @param stream the stream of AgentX encoded data.
     * @return a handler that knows how to respond to the PDU request from the master.
     * 
     * @throws AgentXProtocolException if there was an error during the parsing of the stream.
     */
    public ProtocolDataUnitHandler getRequestPDUHandler(byte type, byte[] stream) throws AgentXProtocolException
    {
        ProtocolDataUnitHandler handler = null;
        
        switch (type)
        {
            case ProtocolDataUnitType.CLEANUPSET_PDU:
                handler = AgentXCleanupSetPDU.decode(stream);
                break;
            case ProtocolDataUnitType.COMMITSET_PDU:
                handler = AgentXCommitSetPDU.decode(stream);
                break;
            case ProtocolDataUnitType.GET_PDU:
                handler = AgentXGetPDU.decode(stream);
                break;
            case ProtocolDataUnitType.GETBULK_PDU:
                handler = AgentXGetBulkPDU.decode(stream);
                break;
            case ProtocolDataUnitType.GETNEXT_PDU:
                handler = AgentXGetNextPDU.decode(stream);
                break;
            case ProtocolDataUnitType.TESTSET_PDU:
                handler = AgentXTestSetPDU.decode(stream);
                break;
            case ProtocolDataUnitType.UNDOSET_PDU:        
                handler = AgentXUndoSetPDU.decode(stream);
                break;
                
            case ProtocolDataUnitType.RESPONSE_PDU:
            case ProtocolDataUnitType.ADDAGENTCAPS_PDU:
            case ProtocolDataUnitType.REMOVEAGENTCAPS_PDU:
            case ProtocolDataUnitType.INDEXALLOCATE_PDU:
            case ProtocolDataUnitType.INDEXDEALLOCATE_PDU:
            case ProtocolDataUnitType.CLOSE_PDU:
            case ProtocolDataUnitType.NOTIFY_PDU:
            case ProtocolDataUnitType.OPEN_PDU:
            case ProtocolDataUnitType.PING_PDU:
            case ProtocolDataUnitType.REGISTER_PDU:
            case ProtocolDataUnitType.UNREGISTER_PDU:
                throw new AgentXProtocolException(AgentXProtocolExceptionReason.UNHANDLED_PDU_TYPE);
                
            default:
                throw new AgentXProtocolException(AgentXProtocolExceptionReason.UNKNOWN_PDU_TYPE);
        }
        
        return handler;
    }
}
