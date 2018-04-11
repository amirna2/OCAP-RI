// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

package org.cablelabs.impl.snmp;

import java.net.SocketException;
import java.net.UnknownHostException;
import org.cablelabs.impl.snmp.drexel.SNMPv1CommunicationInterface;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;
import java.io.IOException;
import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;
import org.cablelabs.impl.snmp.drexel.SNMPVarBindList;
import org.cablelabs.impl.snmp.drexel.SNMPSequence;
import org.cablelabs.impl.snmp.drexel.SNMPObjectIdentifier;
import org.cablelabs.impl.snmp.drexel.SNMPObject;
import org.cablelabs.impl.snmp.drexel.SNMPGetException;
import org.cablelabs.impl.snmp.drexel.SNMPSetException;

import org.cablelabs.impl.snmp.drexel.SNMPBERCodec;

import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionImpl;
import org.cablelabs.impl.ocap.diagnostics.MIBManagerExt;
import org.ocap.diagnostics.MIBObject;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPBadValueException;

/**
 * The SNMPClient class provides methods to to make SNMP Get and Set Requests
 * over UDP on the standard SNMP port 161 to a master SNMP Agent. This class
 * uses SNMP protocol version 2 of SNMP (RFC 1157).
 */

public class SNMPClient
{
    // member variables
    private SNMPv1CommunicationInterface comInterface = null;
    private static HashMap INSTANCES_MAP = new HashMap();

    // SNMP Protocl version as defined in RFC 1157
    private static final int SNMP_VERSION_V2 = 1;
    private static final String DEFAULT_ECM_IP_ADDRESS = "192.168.1.2";
    private static final String LOOP_BACK_ADDRESS = "127.0.0.1";

    private static final Integer ESTB_SUBDEVICE_INT_OBJ = new Integer(MIBManagerExt.ESTB_SUBDEVICE);
    private static final Integer ECM_SUBDEVICE_INT_OBJ = new Integer(MIBManagerExt.ECM_SUBDEVICE);
    private static final Logger log = Logger.getLogger(SNMPClient.class);

    /**
     * Protected constructor, no application access.
     */
    private SNMPClient(String host)
    {
        // Create a communications interface to a remote SNMP-capable device;
        // need to provide the remote host's InetAddress and the community
        // name for the device; in addition, need to supply the version number
        // for the SNMP messages to be sent (the value 0 corresponding to SNMP
        // version 1 and the value 1 corresponding to SNMP version 2)
        try
        {
            InetAddress hostAddress = InetAddress.getByName(host);
            //String community = "public"; this is the read-only community
            String community = "private";
            int version = SNMP_VERSION_V2;

            if (log.isInfoEnabled())
            {
                log.info("Creating SNMPClient comInterface (" + version +
                         ", " + hostAddress + ", " + community + ")");
            }

            comInterface = new SNMPv1CommunicationInterface(version, hostAddress, community);
        }
        catch (UnknownHostException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        catch (SocketException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Gets the SNMPClient for the default source.
     * 
     * @return The SNMPClient.
     */
    public static synchronized SNMPClient getInstance()
    {
        return getInstance(MIBManagerExt.ESTB_SUBDEVICE);
    }

    /**
     * Gets the SNMPClient for given source.
     * 
     * @param host
     *            - The host where the OID is hosted.
     * 
     * @return The SNMPClient.
     */
    public static synchronized SNMPClient getInstance(int source)
    {
        SNMPClient client = null;
        if (MIBManagerExt.ESTB_SUBDEVICE == source)
        {
            client = getSubDevicesInstance(source, LOOP_BACK_ADDRESS);
        }
        else if (MIBManagerExt.ECM_SUBDEVICE == source)
        {
            client = getSubDevicesInstance(source, MPEEnv
                    .getEnv("SNMP.ClientAddress." + source, DEFAULT_ECM_IP_ADDRESS));
        }
        return client;
    }

    /**
     * The method returns the <code>SNMPClient</code> instance for various aub
     * devices.
     * 
     * @param source
     *            - The source to which <code>SNMPClient</code> instance to
     *            create.
     * @param host
     *            - The host address of the source
     * @return
     */
    private static SNMPClient getSubDevicesInstance(int source, String host)
    {
        SNMPClient instance = null;
        Integer keyObj = (source == MIBManagerExt.ECM_SUBDEVICE ? ECM_SUBDEVICE_INT_OBJ : ESTB_SUBDEVICE_INT_OBJ);
        if (INSTANCES_MAP.containsKey(keyObj))
        {
            instance = (SNMPClient) INSTANCES_MAP.get(keyObj);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Creating SNMPClient(" + host + ")");
            }

            instance = new SNMPClient(host);
            INSTANCES_MAP.put(keyObj, instance);
        }
        return instance;
    }

    /**
     * Set a value in a MIB
     * 
     * @param oid
     *            OID in {@link String} form. Must be leaf or value in table
     * @param newValue
     *            the new value as a byte array TLV
     * 
     * @throws IllegalArgumentException
     *             if invalid OID or OID does not exist
     * @throws IOException
     *             if trying to write a read-only value
     */
    public void setMIBValue(String oid, byte[] newValue) throws IllegalArgumentException, IOException
    {
        SNMPObject snmpObject;

        if (!oid.endsWith(".0"))
        {
            throw new IllegalArgumentException("oid:" + oid + " is not a leaf (does not end in .0)");
        }

        // Create an object to set in SNMP
        try
        {
            snmpObject = getSNMPObjectFromBER(newValue);
        }
        catch (SNMPBadValueException e)
        {
            throw new IllegalArgumentException("Unable to getSNMPObjectFromBER ");
        }

        // Set the Object
        try
        {
            comInterface.setMIBEntry(oid, snmpObject);
        }
        catch (SNMPBadValueException e)
        {
            throw new IllegalArgumentException("setMIBEntry threw " + e);
        }
        catch (SNMPSetException e)
        {
            throw new IOException("setMIBEntry threw " + e);
        }
    }

    /**
     * @param oid
     *            OID in {@link String} form
     * 
     * @return an array of all values represented by OID as an array of
     *         MIBDefinitionImplExt objects. If the OID does not exist the
     *         method will return an empty array (zero length).
     * 
     * @throws IllegalArgumentException
     *             if invalid OID.
     * @throws IOException
     *             for some reason not able to access the MIB that contains the
     *             value
     */
    public MIBDefinitionExt[] getMIBDefinition(String oid) throws IllegalArgumentException, IOException

    {
        SNMPVarBindList snmpVarBindList;
        SNMPValue snmpValue;

        // Get the SNMP value(s) out and convert to MIBDefinitionExt format.
        if (oid.endsWith(".0"))
        {
            // Get this leaf's single value out
            try
            {
                snmpVarBindList = comInterface.getMIBEntry(oid);
            }
            catch (IOException e)
            {
                throw new IOException("getMIBEntry(" + oid + ") failed with " + e);
            }
            catch (SNMPBadValueException e)
            {
                throw new IllegalArgumentException("getMIBEntry(" + oid + ") failed");
            }
            catch (SNMPGetException e)
            {
                throw new IOException("getMIBEntry(" + oid + ") failed");
            }

        }
        else
        {
            // This gets all values out from the base oid starting with the NEXT
            // one
            try
            {
                snmpVarBindList = comInterface.retrieveMIBTable(oid);
            }
            catch (IOException e)
            {
                throw new IOException("retrieveMIBTable(" + oid + ") failed");
            }
            catch (SNMPGetException e)
            {
                throw new IOException("retrieveMIBTable(" + oid + ") failed");
            }
            catch (SNMPBadValueException e)
            {
                throw new IOException("retrieveMIBTable(" + oid + ") failed");
            }
        }

        int numVarBinds = snmpVarBindList.size();

        /*
         * Loop through (OID,Value) pairs making a new MIBDefinitionExt element
         * for each pair but don't allow NULL objects through.
         */
        List resultsList = new ArrayList();         

        for (int x = 0; x < numVarBinds; x++)
        {
            SNMPSequence snmpSequence = (SNMPSequence) (snmpVarBindList.getSNMPObjectAt(x));

            // Extract the object identifier from the pair; it's the first
            // element in the sequence
            SNMPObjectIdentifier snmpObjectId = (SNMPObjectIdentifier) snmpSequence.getSNMPObjectAt(0);

            // Extract the corresponding value from the pair; it's the second
            // element in the sequence
            SNMPObject snmpObject = snmpSequence.getSNMPObjectAt(1);

            // Only include objects with data
            if (snmpObject.getBEREncoding()[0] != SNMPBERCodec.SNMPNULL)
            {
                // Convert the Drexel obj into appropriate SNMPValue object
                try
                {
                    snmpValue = snmpObject.getSNMPValue();
                }
                catch (SNMPBadValueException e)
                {
                    // This happens if a non Value such as an SNMPSequence is
                    // queried, this can
                    // not happen in practice
                    break;
                }

                MIBObject mibObj = new MIBObject(snmpObjectId.toString(), snmpObject.getBEREncoding());

                // Add this MIBDefinition to the array list
                resultsList.add(new MIBDefinitionImpl(mibObj, snmpValue));
            }
        }

        return (MIBDefinitionExt[])(resultsList.toArray(new MIBDefinitionExt[resultsList.size()]));
      }

    /*
     * Get the appropriate SNMPValue<XXX> class from the BER ASN.1 encoded
     * bytes.
     * 
     * @param enc The ASN.1 encoded TLV bytes representing the value
     * 
     * @return the appropriate SNMPValue object
     */
    public static SNMPValue getSNMPValueFromBER(byte[] enc) throws SNMPBadValueException
    {
        return getSNMPObjectFromBER(enc).getSNMPValue();
    }

    /*
     * Get the appropriate SNMPObject<XXX> class from the BER ASN.1 encoded
     * bytes (i.e. in the form TLV)
     * 
     * @param enc The ASN.1 encoded bytes
     * 
     * @return the appropriate SNMPValue object
     */
    private static SNMPObject getSNMPObjectFromBER(byte[] enc) throws SNMPBadValueException
    {
        SNMPObject snmpObj = null;

        // Convert the BER into an SNMPObject this might throw
        // SNMPBadValueException
        snmpObj = SNMPBERCodec.extractEncoding(SNMPBERCodec.extractNextTLV(enc, 0));

        return snmpObj;
    }

}
