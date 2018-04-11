/*
 * @(#)AgentX_Varbind.java									1.0	2000/03/01
 *
 * ------------------------------------------------------------------------
 *        Copyright (c) 2000 University of Coimbra, Portugal
 *
 *                     All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the name of the University of Coimbra
 * not be used in advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 *
 * University of Coimbra distributes this software in the hope that it will
 * be useful but DISCLAIMS ALL WARRANTIES WITH REGARD TO IT, including all
 * implied warranties of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. In no event shall University of Coimbra be liable for any
 * special, indirect or consequential damages (or any damages whatsoever)
 * resulting from loss of use, data or profits, whether in an action of
 * contract, negligence or other tortious action, arising out of or in
 * connection with the use or performance of this software.
 * ------------------------------------------------------------------------
 */

package org.cablelabs.impl.snmp.agentx.types;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPClient;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueCounter32;
import org.cablelabs.impl.snmp.SNMPValueCounter64;
import org.cablelabs.impl.snmp.SNMPValueGauge32;
import org.cablelabs.impl.snmp.SNMPValueIPAddress;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.cablelabs.impl.snmp.SNMPValueObjectId;
import org.cablelabs.impl.snmp.SNMPValueOctetString;
import org.cablelabs.impl.snmp.SNMPValueTimeTicks;
import org.cablelabs.impl.snmp.agentx.AgentXParseErrorException;

/**
 * This class implements AgentX Varbind as described in RFC 2257. The
 * representation of a variable binding (termed a VarBind) consists of a 2-byte
 * type field, a name (Object Identifier), and the actual value data. <br>
 * The structure of the <code>AgentX_OctetString</code> is as follows: <br>
 * <p>
 * 
 * <blockquote>
 * <li><code>Type</code>: Indicates the variable binding's syntax.<br>
 * </li>
 * <li><code>Name</code>: The Object Identifier which names the variable.<br>
 * </li>
 * <li><code>Data</code>: The actual value.<br>
 * </li> </blockquote>
 * 
 * @author Eduardo Lourenço
 * @version 1.0, 2000/03/01
 */
public class AgentXVarbind implements AgentXEncodableType
{
    public final static int INTEGER = 2;
    public final static int OCTET_STRING = 4;
    public final static int NULL = 5;
    public final static int OBJECT_IDENTIFIER = 6;
    public final static int IPADDRESS = 64;
    public final static int COUNTER32 = 65;
    public final static int GAUGE32 = 66;
    public final static int TIMETICKS = 67;
    public final static int OPAQUE = 68;
    public final static int COUNTER64 = 70;
    public final static int NO_SUCH_OBJECT = 128;
    public final static int NO_SUCH_INSTANCE = 129;
    public final static int END_OF_MIB_VIEW = 130;

    private final static byte reservedBytes[] = { 0, 0 };

    private AgentXUInt16 myType = new AgentXUInt16(0);
    private AgentXOid myName = null;
    private AgentXEncodableType myEncodeableData = null;

    /**
     * Decode the bytes from the network into an AgentXVarbind according to
     * RFC2741 Section 5.4. Value Representation
     * 
     * @param obj
     *            the sequence of bytes to decode
     * @param offset
     *            the offset to start decoding the byte array
     * @return A new AgentXVarbind containing the data from the specified byte
     *         array.
     * @throws AgentXParseErrorException
     */
    public static AgentXVarbind decode(byte[] obj, int offset) throws AgentXParseErrorException
    {
        return new AgentXVarbind(obj, offset);
    }

    /**
     * Constructs a newly allocated <code>AgentXVarbind</code> using the byte
     * array given as a parameter.
     * <p>
     * 
     * @param obj
     *            the <code>byte</code> array to be converted.
     * @param offset
     *            the offset into the array to start reading.
     * @throws AgentXParseErrorException
     */
    private AgentXVarbind(byte[] obj, int offset) throws AgentXParseErrorException
    {
        if (obj == null
                || obj.length < offset + AgentXUInt16.NUM_BYTES_IN_UINT16 + +reservedBytes.length
                        + AgentXOid.OID_HEADER_SIZE)
        {
            throw new AgentXParseErrorException("Unable to parse AgentXVarbind, the byte array is incomplete");
        }

        myType = AgentXUInt16.decode(obj, offset);
        offset += myType.getLength() + reservedBytes.length;

        myName = AgentXOid.decode(obj, offset);
        offset += myName.getLength();

        switch (myType.getValue())
        {
           case INTEGER:
               myEncodeableData = AgentXInt32.decode(obj, offset);
               break;
           case COUNTER32:
           case GAUGE32:
           case TIMETICKS:
               myEncodeableData = AgentXUInt32.decode(obj, offset);
               break;
           case COUNTER64:
               myEncodeableData = AgentXUInt64.decode(obj, offset);
               break;
           case OBJECT_IDENTIFIER:
               myEncodeableData = AgentXOid.decode(obj, offset);
               break;
           case IPADDRESS:
           case OPAQUE:
           case OCTET_STRING:
               myEncodeableData = AgentXOctetString.decode(obj, offset);
               break;
           case NULL:
           case NO_SUCH_INSTANCE:
           case NO_SUCH_OBJECT:
           case END_OF_MIB_VIEW:
           default:
               myEncodeableData = null;
        }
    }

    /**
     * Create a new varbind with the specified name, type, value.
     * 
     * @param name the name of the varbind.  Usually an OID.
     * @param type the type of variable bound to this OID name.
     * @param variable the data bound to the OID name.
     */
    public AgentXVarbind(String name, int type, AgentXEncodableType variable)
    {
        myName = new AgentXOid(name);
        myType = new AgentXUInt16(type & 0xFF);
        myEncodeableData = variable;        
    }
    
    /**
     * Create a new varbind with the specified name and decoded ASN.1 data
     * 
     * @param name The name of the Oid this Value Representation maps
     * @param enc an ASN.1-BER encoded byte array
     * 
     * @throws SNMPBadValueException
     * @throws AgentXParseErrorException
     */
    public AgentXVarbind(String name, byte[] enc) throws SNMPBadValueException
    {
        myName = new AgentXOid(name);
        if (enc != null)
        {
            myType = new AgentXUInt16(enc[0] & 0xFF);

            SNMPValue snmpValue = SNMPClient.getSNMPValueFromBER(enc);
            switch (myType.getValue())
            {
               case INTEGER:
                   BigInteger sigValue32 = ((SNMPValueInteger) snmpValue).getValue();
                   myEncodeableData = new AgentXInt32(sigValue32.longValue());
                   break;
               case COUNTER32:
               case GAUGE32:
               case TIMETICKS:
                   BigInteger value32 = ((SNMPValueInteger) snmpValue).getValue();
                   myEncodeableData = new AgentXUInt32(value32.longValue());
                   break;
               case COUNTER64:
                   BigInteger value64 = ((SNMPValueCounter64) snmpValue).getValue();
                   myEncodeableData = new AgentXUInt64(value64);
                   break;
               case OBJECT_IDENTIFIER:
                   myEncodeableData = new AgentXOid(snmpValue.toString());
                   break;
               case IPADDRESS:
               case OPAQUE:
               case OCTET_STRING:
                   myEncodeableData = new AgentXOctetString(snmpValue.toString());
                   break;
               case NULL:
               case NO_SUCH_INSTANCE:
               case NO_SUCH_OBJECT:
               case END_OF_MIB_VIEW:
               default:
                   myEncodeableData = null;
            }
        }
        else
        {
            myType = new AgentXUInt16(NULL);
            myEncodeableData = null;
        }
    }

    /**
     * Create a new AgentXVarbind for the specified OID with an empty data type
     * 
     * @param name OID that this Varbind maps
     * @param type The {empty} type of this OID
     * 
     * @throws AgentXParseErrorException
     * @throws SNMPBadValueException
     */
    public AgentXVarbind(String name, int type)
    {
        if (type != NULL && type != NO_SUCH_INSTANCE && type != NO_SUCH_OBJECT && type != END_OF_MIB_VIEW)
        {
            throw new IllegalArgumentException("Invalid type for empty data, type: " + type);
        }

        myType = new AgentXUInt16(type);
        myName = new AgentXOid(name);
        myEncodeableData = null;
    }

    /**
     * Returns the byte length of the <code>AgentX_Varbind</code> object.
     * <p>
     * 
     * @return a <code>long</code> value defining the byte length of the <code>AgentX_Varbind</code> object.
     */
    public int getLength()
    {
        int ret = myType.getLength() + reservedBytes.length;

        if (myName != null)
        {
            ret += myName.getLength();
        }

        if (myEncodeableData != null)
        {
            ret += myEncodeableData.getLength();
        }
        return ret;
    }

    /**
     * Get the Oid from this varbind
     * 
     * @return the Oid from this varbind
     */
    public String getName()
    {
        return myName.getValue();
    }

    /**
     * Get the type of data encoded in this varbind
     * 
     * @return the type of data encoded in this varbind
     */
    public int getType()
    {
        return myType.getValue();
    }

    /**
     * Get the value of the data stored in this vartype encoded as an ASN.1-BER
     * byte array
     * 
     * @return the value of the data stored in this vartype encoded as an
     *         ASN.1-BER byte array
     * @throws SNMPBadValueException
     */
    public byte[] getValue() throws SNMPBadValueException
    {
        SNMPValue snmpValue;
        switch (myType.getValue())
        {
        case INTEGER:
            snmpValue = new SNMPValueInteger(((AgentXInt32) myEncodeableData).getValue());
            break;
        case COUNTER32:
            snmpValue = new SNMPValueCounter32(((AgentXUInt32) myEncodeableData).getValue());
            break;
        case GAUGE32:
            snmpValue = new SNMPValueGauge32(((AgentXUInt32) myEncodeableData).getValue());
            break;
        case TIMETICKS:
            snmpValue = new SNMPValueTimeTicks(((AgentXUInt32) myEncodeableData).getValue());
            break;
        case COUNTER64:
            snmpValue = new SNMPValueCounter64(((AgentXUInt64) myEncodeableData).getValue());
            break;
        case OBJECT_IDENTIFIER:
            snmpValue = new SNMPValueObjectId(((AgentXOid) myEncodeableData).getValue());
            break;
        case IPADDRESS:
            snmpValue = new SNMPValueIPAddress(((AgentXOctetString) myEncodeableData).getValue());
            break;
        case OPAQUE:
            snmpValue = new SNMPValueOctetString(((AgentXOctetString) myEncodeableData).getValue());
            break;
        case OCTET_STRING:
            snmpValue = new SNMPValueOctetString(((AgentXOctetString) myEncodeableData).getValue());
            break;
        default:
            snmpValue = null;
        }

        return snmpValue == null ? null : snmpValue.getBEREncoding();
    }

    /**
     * Returns the array of the bytes that compose this
     * <code>AgentXVarbind</code>.
     * 
     * @return a <code>btye</code> array corresponding the
     *         <code>AgentXVarbind</code> object.
     */
    public byte[] encode()
    {
        ByteArrayOutputStream encodedData = new ByteArrayOutputStream(getLength());
        encodedData.write(myType.encode(), 0, myType.getLength());
        encodedData.write(reservedBytes, 0, reservedBytes.length);
        encodedData.write(myName.encode(), 0, myName.getLength());
        if (myEncodeableData != null)
        {
            encodedData.write(myEncodeableData.encode(), 0, myEncodeableData.getLength());
        }
        return encodedData.toByteArray();
    }
}
