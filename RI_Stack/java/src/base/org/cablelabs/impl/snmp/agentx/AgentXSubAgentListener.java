package org.cablelabs.impl.snmp.agentx;

/**
 * A client of the AgentX subagent can implement this interface in order to be notified 
 * of certain asynchronous events within the subagent.  In particular when there is a connection
 * loss detected.  However the set of events could be updated to make the entire AgentX Sub Agent API 
 * an asynchronous implementation rather than it's current synchronous approach.
 *  
 * @author Kevin Hendry.
 */
public interface AgentXSubAgentListener
{
    /**
     * Called by the subagent whenever a reportable event is detected.
     * 
     * @param event the event being delivered.
     */
    void notify(AgentXSubAgentEvent event);
}
