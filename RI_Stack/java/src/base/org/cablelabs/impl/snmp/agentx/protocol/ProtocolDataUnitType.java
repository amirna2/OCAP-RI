package org.cablelabs.impl.snmp.agentx.protocol;

/**
 * The type used in constructing the header for AgentX PDUs.
 *  
 * @author Kevin Hendry
 */
public final class ProtocolDataUnitType
{
    private byte myType;
    private String myDescription;
       
    private ProtocolDataUnitType(byte type, String desc)
    {
        myType = type;
        myDescription = desc;
    }
        
    /**
     * Retrieve the type value.
     * 
     * @return the value of the type as defined by AgentX.
     */
    public final byte getTypeValue()
    {
        return myType;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return myDescription;
    }

    public static final byte OPEN_PDU = 1;
    public static final byte CLOSE_PDU = 2;
    public static final byte REGISTER_PDU = 3;
    public static final byte UNREGISTER_PDU = 4;
    public static final byte GET_PDU = 5;
    public static final byte GETNEXT_PDU = 6;
    public static final byte GETBULK_PDU = 7;
    public static final byte TESTSET_PDU = 8;
    public static final byte COMMITSET_PDU = 9;
    public static final byte UNDOSET_PDU = 10;                    
    public static final byte CLEANUPSET_PDU = 11;
    public static final byte NOTIFY_PDU = 12;
    public static final byte PING_PDU = 13;
    public static final byte INDEXALLOCATE_PDU = 14;
    public static final byte INDEXDEALLOCATE_PDU = 15;
    public static final byte ADDAGENTCAPS_PDU = 16;
    public static final byte REMOVEAGENTCAPS_PDU = 17;
    public static final byte RESPONSE_PDU = 18;

    /** AgentX Pdu type: Open Pdu. */
    public final static ProtocolDataUnitType AGENTX_OPEN_PDU = new ProtocolDataUnitType(OPEN_PDU, "Open-PDU");
    
    /** AgentX Pdu type: Close Pdu. */
    public final static ProtocolDataUnitType AGENTX_CLOSE_PDU = new ProtocolDataUnitType(CLOSE_PDU, "Close-PDU");
    
    /** AgentX Pdu type: Register Pdu. */
    public final static ProtocolDataUnitType AGENTX_REGISTER_PDU = new ProtocolDataUnitType(REGISTER_PDU, "Register-PDU");
    
    /** AgentX Pdu type: Unregister Pdu. */
    public final static ProtocolDataUnitType AGENTX_UNREGISTER_PDU = new ProtocolDataUnitType(UNREGISTER_PDU, "Unregister-PDU");
    
    /** AgentX Pdu type: Get Pdu. */
    public final static ProtocolDataUnitType AGENTX_GET_PDU = new ProtocolDataUnitType(GET_PDU, "Get-PDU");
    
    /** AgentX Pdu type: GetNext Pdu. */
    public final static ProtocolDataUnitType AGENTX_GETNEXT_PDU = new ProtocolDataUnitType(GETNEXT_PDU, "GetNext-PDU");
    
    /** AgentX Pdu type: GetBulk Pdu. */
    public final static ProtocolDataUnitType AGENTX_GETBULK_PDU = new ProtocolDataUnitType(GETBULK_PDU, "GetBulk-PDU");
    
    /** AgentX Pdu type: TestSet Pdu. */
    public final static ProtocolDataUnitType AGENTX_TESTSET_PDU = new ProtocolDataUnitType(TESTSET_PDU, "TestSet-PDU");
    
    /** AgentX Pdu type: CommitSet Pdu. */
    public final static ProtocolDataUnitType AGENTX_COMMITSET_PDU = new ProtocolDataUnitType(COMMITSET_PDU, "CommitSet-PDU");
    
    /** AgentX Pdu type: UndoSet Pdu. */
    public final static ProtocolDataUnitType AGENTX_UNDOSET_PDU = new ProtocolDataUnitType(UNDOSET_PDU, "UndoSet-PDU");
    
    /** AgentX Pdu type: CleanupSet Pdu. */
    public final static ProtocolDataUnitType AGENTX_CLEANUPSET_PDU = new ProtocolDataUnitType(CLEANUPSET_PDU, "CleanUpSet-PDU");
    
    /** AgentX Pdu type: Notify Pdu. */
    public final static ProtocolDataUnitType AGENTX_NOTIFY_PDU = new ProtocolDataUnitType(NOTIFY_PDU, "Notify-PDU");
    
    /** AgentX Pdu type: Ping Pdu. */
    public final static ProtocolDataUnitType AGENTX_PING_PDU = new ProtocolDataUnitType(PING_PDU, "Ping-PDU");
    
    /** AgentX Pdu type: IndexAllocate Pdu. */
    public final static ProtocolDataUnitType AGENTX_INDEXALLOCATE_PDU = new ProtocolDataUnitType(INDEXALLOCATE_PDU, "IndexAllocate-PDU");
    
    /** AgentX Pdu type: IndexDeallocate Pdu. */
    public final static ProtocolDataUnitType AGENTX_INDEXDEALLOCATE_PDU = new ProtocolDataUnitType(INDEXDEALLOCATE_PDU, "IndexDeallocate-PDU");
    
    /** AgentX Pdu type: AddAgentCaps Pdu. */
    public final static ProtocolDataUnitType AGENTX_ADDAGENTCAPS_PDU = new ProtocolDataUnitType(ADDAGENTCAPS_PDU, "AddAgentCaps-PDU");
    
    /** AgentX Pdu type: RemoveAgentCaps Pdu. */
    public final static ProtocolDataUnitType AGENTX_REMOVEAGENTCAPS_PDU = new ProtocolDataUnitType(REMOVEAGENTCAPS_PDU, "RemoveAgentCaps-PDU");
    
    /** AgentX Pdu type: Response Pdu. */
    public final static ProtocolDataUnitType AGENTX_RESPONSE_PDU = new ProtocolDataUnitType(RESPONSE_PDU, "Reponse-PDU");        
}
