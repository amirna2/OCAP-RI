package org.cablelabs.impl.snmp.agentx.types;

/**
 * The interface implemented by any AgentX types that may be encoded into an AgentX byte stream.
 * 
 * @author Mark Orchard
 */
public interface AgentXEncodableType
{
    /**
     * Generate a byte array containing the data for this encodable type according to the RFC2741
     * Section 5. AgentX Encodings. All byte encodings will be big endian.
     * 
     * @return the AgentX encoded stream of data.
     */
    byte[] encode();
    
    /**
     * Retrieves the length of the type implementing this interface.
     * 
     * @return the length of the type in bytes.
     */
    int getLength();
}
