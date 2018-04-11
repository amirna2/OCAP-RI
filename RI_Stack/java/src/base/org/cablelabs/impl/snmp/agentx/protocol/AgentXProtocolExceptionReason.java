package org.cablelabs.impl.snmp.agentx.protocol;

/**
 * An enumeration of possible reasons for an AgentXProtocolException to be thrown. Most of these 
 * reasons aren't currently used, but did exist in the source AgentX implementation.  So they are
 * kept for here posterity. 
 * 
 * @author Kevin Hendry.
 */
public class AgentXProtocolExceptionReason
{
    private String myDescription;
    
    private AgentXProtocolExceptionReason(String description)
    {
        myDescription = description;
    }
    
    public String toString()
    {
        return myDescription;
    }
    
    public static final AgentXProtocolExceptionReason UNKNOWN_ERROR = 
        new AgentXProtocolExceptionReason("Unknown");
    
    public static final AgentXProtocolExceptionReason UNSUPPORTED_AGENTX_VERSION = 
        new AgentXProtocolExceptionReason("Unsupported AgentX version");
    
    public static final AgentXProtocolExceptionReason INVALID_PAYLOAD_LENGTH = 
        new AgentXProtocolExceptionReason("Invalid Payload Length (multiple of 4 required)");

    public static final AgentXProtocolExceptionReason HEADER_SMALLER_THAN_EXPECTED = 
        new AgentXProtocolExceptionReason("PDU Header smaller than expected (20 bytes required)");
    
    public static final AgentXProtocolExceptionReason PDU_LESS_THAN_PAYLOAD_LENGTH = 
        new AgentXProtocolExceptionReason("PDU smaller than Payload Length");
    
    public static final AgentXProtocolExceptionReason PDU_GREATER_THAN_PAYLOAD_LENGTH = 
        new AgentXProtocolExceptionReason("PDU bigger than Payload Length");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_INT16 = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing UInt16");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_INT32 = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing UInt32");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_OID = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing AgentXOid");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_OCTECTSTRING = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing AgentXOctetString");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_SEARCHRANGE = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing AgentXSearchRange");
    
    public static final AgentXProtocolExceptionReason UNEXPTECTED_END_OF_PARSING_VARBIND = 
        new AgentXProtocolExceptionReason("Unexpected end of PDU parsing AgentXVarbind");
    
    public static final AgentXProtocolExceptionReason NOT_VALID_OID = 
        new AgentXProtocolExceptionReason("Not Valid Oid");
    
    public static final AgentXProtocolExceptionReason UNKNOWN_VARBIND_TYPE = 
        new AgentXProtocolExceptionReason("Unknown Varbind Type");
    
    public static final AgentXProtocolExceptionReason UNKNOWN_REASON_IN_CLOSE = 
        new AgentXProtocolExceptionReason("Unknown Reason in Close PDU");
    
    public static final AgentXProtocolExceptionReason UNKNOWN_ERROR_IN_RESPONSE = 
        new AgentXProtocolExceptionReason("Unknown Error Type in Response PDU");
    
    public static final AgentXProtocolExceptionReason UNKNOWN_PDU_TYPE = 
        new AgentXProtocolExceptionReason("Unknown PDU Type");

    public static final AgentXProtocolExceptionReason UNHANDLED_PDU_TYPE = 
        new AgentXProtocolExceptionReason("PDU Type is NOT handled at this time.");
    
    public static final AgentXProtocolExceptionReason IOEXCEPTION_IN_SOCKET = 
        new AgentXProtocolExceptionReason("IOException in Socket");
}
