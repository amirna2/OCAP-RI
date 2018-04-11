package org.cablelabs.impl.snmp.agentx.protocol;

import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.agentx.AgentXCloseReason;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXClosePDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXOpenPDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXRegisterPDU;
import org.cablelabs.impl.snmp.agentx.protocol.admin.AgentXUnregisterPDU;

import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXNotifyPDU;
import org.cablelabs.impl.snmp.agentx.protocol.notifications.AgentXResponsePDU;
import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;

/**
 * It is the responsibility of this class to properly construct the various admin and notification PDUs
 * to be delivered to the master agent.
 * 
 * @author Kevin Hendry
 */
public class ProtocolDataUnitFactory
{               
    public static final byte DEFAULT_TIMEOUT = (byte) 0;
    public static final byte DEFAULT_PRIORITY = (byte) 127;
    public static final String DEFULAT_CONTEXT = null;
    public static final boolean DEFAULT_USE_NETWORK_BYTE_ORDER = true;
    public static final boolean DEFAULT_USE_DEFAULT_CONTEXT = true;
    
    byte myTimeout;
    byte myPriority;
    String myContext;
    boolean isNetworkByteOrder;
    boolean isDefaultContext;
    
    /**
     * The default construct with default configuration.
     */
    public ProtocolDataUnitFactory()
    {
        this(DEFAULT_TIMEOUT, 
             DEFAULT_PRIORITY, 
             DEFULAT_CONTEXT, 
             DEFAULT_USE_NETWORK_BYTE_ORDER, 
             DEFAULT_USE_DEFAULT_CONTEXT);
    }
    
    /*
     * Used to construct the class in a more general purpose manner should the defaults no longer
     * be appropriate in the future. 
     * 
     * @param defaultTimeout an appropriate timeout value.
     * @param defaultPriority an appropriate priority value.
     * @param defaultContext a representation of the context to use.
     * @param useNetworkByteOrder should the PDUs be constructed using network byte ordering or not.
     * @param useDefaultContext should the default context be assumed or not.
     */
    private ProtocolDataUnitFactory(byte defaultTimeout, 
                                    byte defaultPriority, 
                                    String defaultContext, 
                                    boolean useNetworkByteOrder,
                                    boolean useDefaultContext)
    {
        myTimeout = defaultTimeout;
        myPriority = defaultPriority;
        myContext = defaultContext;        
        isNetworkByteOrder = useNetworkByteOrder;
        isDefaultContext = useDefaultContext;
    }
    
    /**
     * Construct an AgentX-OpenPDU instance to be used to start a session with the master agent.
     * 
     * @param packetId an ID used to associate the response from the master with this request.
     * @param desc a description of the sub agent establishing the session.
     * 
     * @return an AgentXOpenPDU instance.
     */
    public AgentXOpenPDU createOpenPDU(long packetId, String desc)
    {
        AgentXHeader header = new AgentXHeader(ProtocolDataUnitType.AGENTX_OPEN_PDU, 
                                               0, /* No session ID yet. We get that from the Master */
                                               0, /* Transaction ID  not required */ 
                                               packetId,  
                                               isNetworkByteOrder, 
                                               isDefaultContext);
        
        return new AgentXOpenPDU(header, desc, null, myTimeout);
    }
    
    /**
     * Construct an AgentX-ClosePDU to shut down a session with the master agent.
     * 
     * @param sessionId the ID of the session being closed.
     * @param packetId an ID used to associate the response from the master with this request.
     * @param reason a code to indicate why the session is being closed.
     * 
     * @return an instance of AgentXClosePDU.
     */
    public AgentXClosePDU createClosePDU(long sessionId, long packetId, AgentXCloseReason reason)
    {
        AgentXHeader header = new AgentXHeader(ProtocolDataUnitType.AGENTX_CLOSE_PDU, 
                                               sessionId, 
                                               0, /* Transaction ID */ 
                                               packetId,  
                                               isNetworkByteOrder, 
                                               isDefaultContext);
        
        return new AgentXClosePDU(header, reason);
    }

    /**
     * Constructs an AgentX-RegisterPDU to register an OID subtree with the master.
     * 
     * NOTE from RFC 2741:
     *      The upper bound of a sub-identifier's range.  This field is
     *      present only if r.range_subid is not 0.
     *      
     *      The use of r.range_subid and r.upper_bound provide a general
     *      shorthand mechanism for specifying a MIB region. For
     *      example, if r.subtree is the OID 1.3.6.1.2.1.2.2.1.1.7,
     *      r.range_subid is 10, and r.upper_bound is 22, the specified
     *      MIB region can be denoted 1.3.6.1.2.1.2.2.1.[1-22].7.
     *      Registering this region is equivalent to registering the
     *      union of subtrees
     *      
     *       1.3.6.1.2.1.2.2.1.1.7
     *       1.3.6.1.2.1.2.2.1.2.7
     *       1.3.6.1.2.1.2.2.1.3.7
     *       ...
     *       1.3.6.1.2.1.2.2.1.22.7
     *       
     *      One expected use of this mechanism is registering a
     *      conceptual row with a single PDU. 
     * 
     * @param sessionId the ID of the session associated with the register request.
     * @param packetId an ID used to associate the response from the master with this request.
     * @param subTree the subtree being registered.
     * @param rangeSubId either 0, or a value indicating the rangeSubId-th value of the OID as the lower bound.
     * @param upperBound either 0, or the OID value at the rangeSubId-th position of the OID.
     * 
     * @return an AgentXRegisterPDU instance.
     */
    public AgentXRegisterPDU createRegisterPDU(long sessionId, long packetId, String subTree, byte rangeSubId, int upperBound)
    {
        AgentXHeader header = new AgentXHeader(ProtocolDataUnitType.AGENTX_REGISTER_PDU, 
                                               sessionId, 
                                               0, /* Transaction ID */ 
                                               packetId,  
                                               isNetworkByteOrder, 
                                               isDefaultContext);
        
        return new AgentXRegisterPDU(header, subTree, myContext, myTimeout, myPriority, rangeSubId, upperBound);          
    }    
    
    /**
     * Constructs and AgentX-UnregisterPDU used to unregister an subtree with the master agent.
     * 
     * @param sessionId the session ID associated with the unregister request.
     * @param packetId an ID used to associate the response from the master with this request.
     * @param subTree the subtree being unregistered.
     * @param rangeSubId either 0, or a value indicating the rangeSubId-th value of the OID as the lower bound.
     * @param upperBound either 0, or the OID value at the rangeSubId-th position of the OID.
     * 
     * @return an instance of AgentXUnregisterPDU.
     * @see ProtocolDataUnitFactory.createRegisterPDU()
     */
    public AgentXUnregisterPDU createUnregisterPDU(long sessionId, 
                                                   long packetId, 
                                                   String subTree, 
                                                   byte rangeSubId, 
                                                   int upperBound)
    {
        AgentXHeader header = new AgentXHeader(ProtocolDataUnitType.AGENTX_UNREGISTER_PDU, 
                                               sessionId, 
                                               0, /* Transaction ID */ 
                                               packetId,  
                                               isNetworkByteOrder, 
                                               isDefaultContext);
        
        return new AgentXUnregisterPDU(header, subTree, myContext, myPriority, rangeSubId, upperBound);
    }
    
    /**
     * Constructs an AgentX-ResponsePDU in reply to a request from the master agent.
     * 
     * @param oldHeader the header from the originating request is used to construct the response.
     * @param error an error code based on how the request was processed.
     * @param index an index indicated the location of the probematic value if there was one.
     * @param list a list of variable bindings to be returned to the master.
     * 
     * @return an instance of AgentXResponsePDU.
     */
    public AgentXResponsePDU createResponsePDU(AgentXHeader oldHeader, 
                                               int error, 
                                               int index, 
                                               AgentXVarbindList list)
    {
        AgentXHeader newHeader = new AgentXHeader(ProtocolDataUnitType.AGENTX_RESPONSE_PDU, oldHeader); 
        
        /*
         * In an agentx response PDU from the subagent to the master agent, the value of res.sysUpTime 
         * has no significance and is ignored by the master agent. So set it to zero
        */
        int systemUpTime = 0;
        return new AgentXResponsePDU(newHeader, systemUpTime, error, index, list);
    }
    
    /**
     * Constructs an AgentX-NofityPDU to be delivered to the master agent when a trap/notification is triggered.
     * 
     * @param sessionId the session associated with this notifications.
     * @param packetId an ID used to associate the response from the master with this request.
     * @param list a list of variable bindings being delivered along with the notification.
     * 
     * @return an instance of AgentXNotifyPDU.
     * 
     * @throws SNMPBadValueException if the PDU could not be created due to a bad trapOID value.
     */
    public AgentXNotifyPDU createNotifyPDU(long sessionId, long packetId, String trapOID, AgentXVarbindList list) 
        throws SNMPBadValueException
    {
        AgentXHeader header = new AgentXHeader(ProtocolDataUnitType.AGENTX_NOTIFY_PDU, 
                                               sessionId, 
                                               0, /* Transaction ID */ 
                                               packetId,  
                                               isNetworkByteOrder, 
                                               isDefaultContext);
        
        return new AgentXNotifyPDU(header, myContext, trapOID, list);
    }
}
