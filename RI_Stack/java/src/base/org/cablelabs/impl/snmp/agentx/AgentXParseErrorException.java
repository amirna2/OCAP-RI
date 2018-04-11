package org.cablelabs.impl.snmp.agentx;


/**
 * This exception is thrown whenever there is an error parsing the AgentX data.
 * 
 * @author Kevin Hendry
 */
public class AgentXParseErrorException extends Exception
{
    /**
     * Generated value.
     */
    private static final long serialVersionUID = -193060153880478101L;

    public AgentXParseErrorException()
    {
        super();
    }
    
    public AgentXParseErrorException(String message)
    {
        super(message);
    }
}
