package org.cablelabs.impl.snmp.agentx.protocol;

import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager;

/**
 * This base class is contains the common base functionality for the various request PDUs that
 * implement the ProtocolDataUnitHandler interface in order to do work on the MIB at the 
 * request of the master agent.
 * 
 * @author Kevin Hendry, Mark Orchard.
 */
public abstract class ProtocolDataUnit implements ProtocolDataUnitHandler
{
    protected AgentXHeader myHeader;
    protected ProtocolDataUnitHandler myHandler = null;
    
    /**
     * The header has not yet been parsed for the PDU, if we are asked to generate a AgentXResponsePDU then we'll
     * have to return null.
     */
    final protected ProtocolDataUnitHandler noHeaderHandler = new ProtocolDataUnitHandler()
    {
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                                  MIBDelegate mib,
                                                  AgentXTransactionManager transactionMgr)
        {
            return null;
        }
    };
    
    /**
     * The header has been parsed for the PDU, but the other state variables have not been read, if we are asked to 
     * generate a AgentXResponsePDU then we'll have to return a response with a parse error
     */
    final protected ProtocolDataUnitHandler parseErrorHandler = new ProtocolDataUnitHandler()
    {
        public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                                  MIBDelegate mib,
                                                  AgentXTransactionManager transactionMgr)
        {
            return pduFactory.createResponsePDU(myHeader, AgentXResponseError.PARSEERROR, (short) 0, null);
        }
    };
    

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitHandler#handlePDURequest(
     *       org.cablelabs.impl.snmp.agentx.protocol.ProtocolDataUnitFactory,
     *       org.cablelabs.impl.snmp.agentx.AgentXMIBDelegate,
     *       org.cablelabs.impl.snmp.agentx.util.AgentXTransactionManager)
     */
    public AgentXResponsePDU handlePDURequest(ProtocolDataUnitFactory pduFactory, 
                                              MIBDelegate mib, 
                                              AgentXTransactionManager transactionMgr)
    {
        return myHandler.handlePDURequest(pduFactory, mib, transactionMgr);
    }
}
