package org.cablelabs.impl.snmp.agentx.protocol;

public final class AgentXResponseError
{
    /*
     * Ensure that no one can construct an instance of this class. 
     */
    private AgentXResponseError() {}

    /*
     * Defined SNMPv2 (in RFC 1905)
     */

    /** The value for the error field : No AgentX error. */
    public final static short NOAGENTXERROR = 0;
    
    /** The value for the error field : Too Big. */
    public final static short TOOBIG = 1;
    
    /** The value for the error field : No such name. */
    public final static short NOSUCHNAME = 2;
    
    /** The value for the error field : Bad value. */
    public final static short BADVALUE = 3;
    
    /** The value for the error field : Read only */
    public final static short READONLY = 4;
    
    /** The value for the error field : Generation error. */
    public final static short GENERR = 5;
    
    /** The value for the error field : No access. */
    public final static short NOACCESS = 6;
    
    /** The value for the error field : Wrong type. */
    public final static short WRONGTYPE = 7;
    
    /** The value for the error field : Wrong length. */
    public final static short WRONGLENGTH = 8;
    
    /** The value for the error field : Wrong encoding. */
    public final static short WRONGENCODING = 9;
    
    /** The value for the error field : Wrong value. */
    public final static short WRONGVALUE = 10;
    
    /** The value for the error field : No creation. */
    public final static short NOCREATION = 11;
    
    /** The value for the error field : Inconsistent value. */
    public final static short INCONSISTENTVALUE = 12;
    
    /** The value for the error field : Resource unavailable. */
    public final static short RESOURCEUNAVAILABLE = 13;
    
    /** The value for the error field : Commit failed. */
    public final static short COMMITFAILED = 14;
    
    /** The value for the error field : Undo failed. */
    public final static short UNDOFAILED = 15;
    
    /** The value for the error field : Authorization error. */
    public final static short AUTHORIZATIONERROR = 16;
    
    /** The value for the error field : Not writable. */
    public final static short NOTWRITABLE = 17;
    
    /** The value for the error field : Inconsistent name. */
    public final static short INCONSISTENTNAME = 18;
    
    // *********** Defined for AgentX ************

    /** The value for the error field : Open failed. */
    public final static short OPENFAILED = 256;
    
    /** The value for the error field : Not open. */
    public final static short NOTOPEN = 257;
    
    /** The value for the error field : Wrong type index. */
    public final static short INDEXWRONGTYPE = 258;
    
    /** The value for the error field : Already allocated index. */
    public final static short INDEXALREADYALLOCATED = 259;
    
    /** The value for the error field : No index available. */
    public final static short INDEXNONEAVAILABLE = 260;
    
    /** The value for the error field : Index not allocated. */
    public final static short INDEXNOTALLOCATED = 261;
    
    /** The value for the error field : Unsupported context. */
    public final static short UNSUPPORTEDCONTEXT = 262;
    
    /** The value for the error field : Duplicate registration. */
    public final static short DUPLICATEREGISTRATION = 263;
    
    /** The value for the error field : Unknown registration. */
    public final static short UNKNOWNREGISTRATION = 264;
    
    /** The value for the error field : Unknown Agent Capabilities. */
    public final static short UNKNOWNAGENTCAPS = 265;
    
    /** The value for the error field : Parse Error */
    public final static short PARSEERROR = 266;
    
    /** The value for the error field : Request Denied. */
    public final static short REQUESTDENIED = 267;
    
    /** The value for the error field : Processing Error. */
    public final static short PROCESSINGERROR = 268;
}
