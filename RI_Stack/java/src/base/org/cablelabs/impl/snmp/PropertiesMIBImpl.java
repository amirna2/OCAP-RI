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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.snmp.MIB;
import org.cablelabs.impl.manager.snmp.MIBValueMap;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A MIB that wraps a properties file. Data is in the form
 * 1.2.3.4.15=2:true:\u0002\u0003\u0004\u000f. All the data is in leaf form
 * (tables must be spelled out in detail).
 * 
 * @author Alan Cossitt
 * 
 */
public class PropertiesMIBImpl implements PropertiesMIB
{
    private static final Logger log = Logger.getLogger(PropertiesMIBImpl.class);

    protected static final String DEFAULT_DIR = "/syscwd/snmp/";

    protected static final String BASEDIR_PROP = "OCAP.snmp.basedir"; // mpeenv.ini

    protected static final String DEFAULT_SUBDIR = "mibs";

    protected static final String FILE_NAME = "propertiesMib.props";

    private static PropertiesMIBImpl impl = null;

    private Properties props = null;

    private HashMap oidToData = new HashMap();

    private PropertiesMIBImpl()
    {
    }

    public static synchronized MIB getInstance()
    {
        if (impl == null)
        {
            impl = new PropertiesMIBImpl();
            resetToDefaultProperties();
        }
        return impl;
    }

    /**
     * For testing and getInstance()
     */
    static synchronized void resetToDefaultProperties()
    {
        try
        {
            impl.nullProperties();
            impl.loadProperties();
        }
        catch (IOException e)
        {
            SystemEventUtil.logCatastrophicError("Unable to load PropertiesMIBImpl initialization file", e);
        }
    }

    private void nullProperties()
    {
        props = null;
    }

    private void loadProperties() throws IOException
    {
        File parent = new File(MPEEnv.getEnv(BASEDIR_PROP, DEFAULT_DIR), DEFAULT_SUBDIR);

        File initFile = new File(parent, FILE_NAME);

        FileInputStream inStream = new FileInputStream(initFile);
        props = new Properties(); // important! don't remove this.
        props.load(inStream);

        parseProperties();
    }

    /**
     * Properties data is in the form "oid=type:writable:data":
     * "1.2.3.4.1=2:true:\u0001\u0002\u0003\u0004\u000f"
     * 
     */
    private void parseProperties()
    {
        Enumeration keyEnum = props.keys();

        while (keyEnum.hasMoreElements())
        {
            String oid = (String) keyEnum.nextElement();
            String encodedStr = props.getProperty(oid);

            MIBValueMap data = new MIBValueMap();

            data.oid = oid;

            int typeEndIdx = encodedStr.indexOf(":");
            String typeStr = encodedStr.substring(0, typeEndIdx);
            data.type = Integer.parseInt(typeStr);

            int writableEndIdx = encodedStr.indexOf(":", typeEndIdx + 1);

            String writable = encodedStr.substring(typeEndIdx + 1, writableEndIdx);

            data.writable = Boolean.valueOf(writable).booleanValue() ? MIBValueMap.VALUE_WRITABLE_TRUE
                    : MIBValueMap.VALUE_WRITABLE_TRUE;

            String valueStr = encodedStr.substring(writableEndIdx + 1);
            byte[] value = ASN1Helper.getDataFromString(valueStr);
            data.value = value;

            oidToData.put(oid, data);
        }
    }

    /**
     *@see base.org.cablelabs.impl.snmp.PropertiesMIB#getProperties()
     */
    public Properties getProperties()
    {
        return props;
    }

    /*
     * Written assuming that this is not called very often
     * 
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.snmp.MIB#getOIDs()
     */
    public String[] getOIDs()
    {
        synchronized (oidToData)
        {
            String[] strArray = new String[oidToData.size()];

            Iterator keyIt = oidToData.keySet().iterator();

            for (int i = 0; keyIt.hasNext(); i++)
            {
                String oid = (String) keyIt.next();
                strArray[i] = oid;
            }
            return strArray;
        }
    }

    public MIBValueMap[] getPropertiesMIBValues(String oid) throws IllegalArgumentException
    {
        // TODO, TODO_SNMP, validate oid
        synchronized (oidToData)
        {
            // for this implementation all oids are scalars or refer to a
            // specific array value

            MIBValueMap data = getData(oid);

            MIBValueMap[] values = null;

            if (data == null)
            {
                return new MIBValueMap[0];
            }

            values = new MIBValueMap[1];

            values[0] = data;

            return values;
        }
    }

    private void setPropertiesMIBValue(String oid, byte[] value) throws IllegalArgumentException, IOException
    {
        synchronized (oidToData)
        {
            MIBValueMap data = getData(oid);

            if (data == null)
            {
                throw new IllegalArgumentException("OID not found");
            }
            if (data.writable != MIBValueMap.VALUE_WRITABLE_TRUE)
            {
                throw new IOException("OID is not writable, OID=" + oid);
            }

            data.value = value;
        }
    }

    private MIBValueMap getData(String oid)
    {
        MIBValueMap data = (MIBValueMap) oidToData.get(oid);
        return data;
    }

    public SNMPResponseExt getMIBValue(String oid)
    {
        // generate an empty SNMPReponseExt to pass back if the access attempt fails
        SNMPResponseExt response = null;
        try
        {
            response = new SNMPResponseExt(oid, SNMPValueError.NULL);
        }
        catch (SNMPBadValueException e) {}

        MIBValueMap mvm = null;

        try
        {
            mvm = getPropertiesMIBValues(oid)[0];
        }
        catch (IllegalArgumentException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("IllegalArgumentException" + e);
            }
        }

        if (null != mvm)
        {
            try
            {
                response = new SNMPResponseExt(oid, SNMPClient.getSNMPValueFromBER(mvm.value));
            }
            catch (SNMPBadValueException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("SNMPBadValueException" + e);
                }
            }
        }

        return response;
    }

    public SNMPResponseExt setMIBValue(String oid, byte[] setData)
    {
        SNMPResponseExt response = null;

        try
        {
            setPropertiesMIBValue(oid, setData);
        }

        catch (IllegalArgumentException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("IllegalArgumentException" + e);
            }
            try
            {
                response = new SNMPResponseExt(oid, SNMPValueError.NULL);
            }
            catch (SNMPBadValueException e1)
            {
            }
            return response;
        }
        catch (IOException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("IOException" + e);
            }
            try
            {
                response = new SNMPResponseExt(oid, SNMPValueError.NULL);
            }
            catch (SNMPBadValueException e1) {}
            return response;
        }

        try
        {
            response = new SNMPResponseExt(oid, SNMPClient.getSNMPValueFromBER(setData));
        }
        catch (SNMPBadValueException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("SNMPBadValueException" + e);
            }
            try
            {
                response = new SNMPResponseExt(oid, SNMPValueError.NULL);
            }
            catch (SNMPBadValueException e1) {}
            return response;
        }

        return response;
    }

    public SNMPResponseExt getNextMIBValue(String oid)
    {
        // TODO operation not yet supported, pass back an empty SNMPReponseExt
        SNMPResponseExt response = null;
        try
        {
            response = new SNMPResponseExt(oid, SNMPValueError.NULL);
        }
        catch (SNMPBadValueException e) {}
        return response;
    }

    public SNMPResponseExt testSetMIBValue(String oid, byte[] setData)
    {
        // TODO operation not yet supported, pass back an empty SNMPReponseExt
        SNMPResponseExt response = null;
        try
        {
            response = new SNMPResponseExt(oid, SNMPValueError.NULL);
        }
        catch (SNMPBadValueException e) {}
        return response;
    }
}
