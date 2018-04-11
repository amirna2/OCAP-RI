package org.cablelabs.impl.snmp.agentx;

/**
 * This exception will be thrown if the error is detected as per RFC 2741:
 * <blockquote>
 *      If this registration would result in duplicate subtrees registered with the 
 *      same value of r.priority, the request fails and an agentx-Response-PDU is 
 *      returned with res.error set to `duplicateRegistration'.
 * </blockquote>
 * 
 * @author Kevin Hendry
 */
public class AgentXDuplicateRegistrationException extends Exception
{
    /**
     * Auto Generated UID Value.
     */
    private static final long serialVersionUID = -7488524891832255107L;

    AgentXDuplicateRegistrationException()
    {
        super();
    }

    AgentXDuplicateRegistrationException(String message)
    {
        super(message);
    }
}
