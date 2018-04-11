package org.cablelabs.impl.snmp.agentx;

/**
 * A typesafe enumeration of reasons for closing a session with the master.
 * 
 * @author Kevin Hendry
 */
public final class AgentXCloseReason
{
    private byte reasonCode;
    private AgentXCloseReason(byte reason)
    {
        reasonCode = reason;
    }
    
    /**
     * Retrieve the raw value for this reason.
     *  
     * @return the raw value of the reason as defined by AgentX.
     */
    public byte getValue()
    {
        return reasonCode;
    }
    
    /** 
     * An undefined reason.
     */
    public final static AgentXCloseReason AGENTX_REASON_OTHER = new AgentXCloseReason((byte) 1);
    
    /** 
     * There was a parsing error while communicating with the master. 
     */
    public final static AgentXCloseReason AGENTX_REASON_PARSEERROR = new AgentXCloseReason((byte) 2);
    
    /** 
     * There was a protocol error while communicating with the master. 
     */
    public final static AgentXCloseReason AGENTX_REASON_PROTOCOLERROR = new AgentXCloseReason((byte) 3);
    
    /** 
     * Timeout occurred while communicating with the master. 
     */
    public final static AgentXCloseReason AGENTX_REASON_TIMEOUTS = new AgentXCloseReason((byte) 4);
    
    /** 
     * The client is shutting down.
     */
    public final static AgentXCloseReason AGENTX_REASON_SHUTDOWN = new AgentXCloseReason((byte) 5);
    
    /** 
     * The manager is closing the session. 
     */
    public final static AgentXCloseReason AGENTX_REASON_BYMANAGER = new AgentXCloseReason((byte) 6);
}
