package org.cablelabs.impl.snmp.agentx;

/**
 * This exception is thrown to indicate a request has been denied as described in
 * RFC 2741 section 7.1.4:
 * 
 *      if the master agent does not wish to permit this registration for 
 *      implementation-specific reasons, the request fails and an agentx-Response-PDU 
 *      is returned with res.error set to`requestDenied'.
 *  
 * @author Kevin Hendry
 */
public class AgentXRequestDeniedException extends Exception
{
    /**
     * Auto Generated UID Value.
     */
    private static final long serialVersionUID = -745767409063393734L;
}
