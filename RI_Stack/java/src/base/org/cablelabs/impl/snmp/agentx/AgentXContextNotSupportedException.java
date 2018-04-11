package org.cablelabs.impl.snmp.agentx;

/**
 * This exception is thrown if an error is detected as per RFC 2741:
 * <blockquote>
 *       Otherwise, if the NON_DEFAULT_CONTEXT bit is set and the master
 *       agent does not support the indicated context, res.error is set 
 *       to `unsupportedContext'.  If the master agent does support the
 *       indicated context, the value of res.sysUpTime is set to the 
 *       value of sysUpTime.0 for that context.
 * </blockquote>
 * 
 * @author Kevin Hendry
 */
public class AgentXContextNotSupportedException extends Exception
{
    /**
     * Auto Generated UID Value.
     */
    private static final long serialVersionUID = 1685674709583879321L;
}
