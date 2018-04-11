package org.cablelabs.impl.snmp.agentx;

/**
 * As per RFC 2741 section 7.1.5:
 * If u.subtree, u.priority, u.range_subid (and if u.range_subid is not 0, 
 * u.upper_bound), and the indicated context do not match an existing registration 
 * made during this session, the agentx-Response-PDU is returned with res.error set to 
 * `unknownRegistration'.
 * 
 * This exception represents an instance of the error described above from RFC 2741.
 * 
 */
public class AgentXUnknownRegistrationException extends Exception
{
    /**
     * Auto Generated UID Value.
     */
    private static final long serialVersionUID = -2773368513207760960L;
}
