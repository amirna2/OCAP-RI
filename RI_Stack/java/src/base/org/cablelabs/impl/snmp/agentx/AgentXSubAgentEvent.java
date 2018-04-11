package org.cablelabs.impl.snmp.agentx;

/**
 * A container for event information. A single event class which contains the specific event code 
 * information.
 * 
 * @author Kevin Hendry
 */
public class AgentXSubAgentEvent
{
    /**
     * This code indicates that there was a connection issue and as such a PDU may 
     * not have been properly delivered to the master agent.
     */
    public static final int AGENTX_EVENT_CONNECTION_ERROR_CODE = 1;
    
    /**
     * This code indicates that a the connection was closed.
     */
    public static final int AGENTX_EVENT_CONNECTION_CLOSED_CODE = 2;
    
    
    /**
     * The event instance for a connection closed event.
     */
    public static final AgentXSubAgentEvent CONNECTION_CLOSED_EVENT 
        = new AgentXSubAgentEvent(AGENTX_EVENT_CONNECTION_CLOSED_CODE);
    
    /**
     * The event instance for a connection error event. 
     */
    public static final AgentXSubAgentEvent CONNECTION_ERROR_EVENT 
        = new AgentXSubAgentEvent(AGENTX_EVENT_CONNECTION_ERROR_CODE);
    
    private int eventCode;
    
    
    
    /**
     * Only AgentX implementation classes will ever construct an instance of this class.
     * 
     * @param code the event code describing the event.
     */
    private AgentXSubAgentEvent(int code)
    {
        eventCode = code;
    }
    
    /**
     * Retrieve the event code for this event.
     * 
     * @return an value representing one of the specified event codes. 
     * @see AGENTX_EVENT_CONNECTION_ERROR, AGENTX_EVENT_CONNECTION_CLOSED
     */
    public int getCode()
    {
        return eventCode;
    }
}
