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

package org.cablelabs.xlet.LoggingTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.ocap.diagnostics.*;

public class LoggingTest implements Xlet
{
    MIBManager mibm = null;
    XletContext m_ctx = null;
    private static final Logger m_log = Logger.getLogger(LoggingTest.class);

    // @Override
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_log.info(".initXlet()");
        m_ctx = ctx;

        if (null == (mibm = MIBManager.getInstance()))
        {
            throw new XletStateChangeException("No MIBManager Found");
        }
    }

    // @Override
    public void startXlet() throws XletStateChangeException
    {
        m_log.info(".startXlet()");

        String ocStbHostSystemLoggingControlReset_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.1";
        String ocStbHostSystemLoggingSize_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.2";
        String ocStbHostSystemLoggingLevelControl_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.3";
        String ocStbHostSystemLoggingGroupControl_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.4";
        String ocStbHostSystemLoggingEventTable_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5";
        String ocStbHostSystemLoggingEventIndex_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5.1.1";
        String ocStbHostSystemLoggingEventTimeStamp_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5.1.2";
        String ocStbHostSystemLoggingEventMessage_oid =
            "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5.1.3";

        int control = getMIBIntValue(ocStbHostSystemLoggingControlReset_oid);
        m_log.info("control: " + control);

        int loggingSize = getMIBIntValue(ocStbHostSystemLoggingSize_oid);
        m_log.info("loggingSize: " + loggingSize);

        int levelCtrl = getMIBIntValue(ocStbHostSystemLoggingLevelControl_oid);
        m_log.info("levelCtrl: " + levelCtrl);

        int groupCtrl = getMIBBitsValue(ocStbHostSystemLoggingGroupControl_oid);
        m_log.info("groupCtrl: " + groupCtrl);

        setMIBIntValue(ocStbHostSystemLoggingControlReset_oid + ".0", 1);
        setMIBIntValue(ocStbHostSystemLoggingSize_oid + ".0", 600);
        setMIBIntValue(ocStbHostSystemLoggingLevelControl_oid + ".0", 5);
        setMIBBitsValue(ocStbHostSystemLoggingGroupControl_oid + ".0", 1);
        loggingSize = getMIBIntValue(ocStbHostSystemLoggingSize_oid);
        m_log.info("loggingSize: " + loggingSize);
        levelCtrl = getMIBIntValue(ocStbHostSystemLoggingLevelControl_oid);
        m_log.info("levelCtrl: " + levelCtrl);
        groupCtrl = getMIBBitsValue(ocStbHostSystemLoggingGroupControl_oid);
        m_log.info("groupCtrl: " + groupCtrl);

        m_log.info("vars complete - test table...");

        int eventIndex = getMIBIntValue(ocStbHostSystemLoggingEventIndex_oid);
        m_log.info("eventIndex: " + eventIndex);

        String eventTS = getMIBStringValue(
                                      ocStbHostSystemLoggingEventTimeStamp_oid);
        m_log.info("eventTS: " + eventTS);

        String eventStr = getMIBStringValue(
                                      ocStbHostSystemLoggingEventMessage_oid);
        m_log.info("eventStr: " + eventStr);

        String tableStr = getMIBValue(ocStbHostSystemLoggingEventTable_oid);
        m_log.info("tableStr: " + tableStr);

        setMIBStringValue(ocStbHostSystemLoggingEventTimeStamp_oid + ".0",
                          "20000", MIBDefinition.SNMP_TYPE_OCTETSTRING);
        setMIBStringValue(ocStbHostSystemLoggingEventMessage_oid + ".0",
                          "test", MIBDefinition.SNMP_TYPE_OCTETSTRING);
 
        eventTS = getMIBStringValue(ocStbHostSystemLoggingEventTimeStamp_oid);
        m_log.info("eventTS: " + eventTS);
        eventStr = getMIBStringValue(ocStbHostSystemLoggingEventMessage_oid);
        m_log.info("eventStr: " + eventStr);

        m_log.info("done.");
    }

    // @Override
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO Auto-generated method stub
    }

    // @Override
    public void pauseXlet()
    {
        // TODO Auto-generated method stub
    }

    private String getMIBValue(String oid)
    {
        String retVal = "";
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBValue null returned from queryMibs");
        }

        for (int i = 0; i < mibd.length; i++)
        {
            MIBObject mibo = mibd[i].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBValue getMIBObject returned null");
            }
            else if (mibd[i].getDataType() == MIBDefinition.SNMP_TYPE_INTEGER)
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                int val = 0;

                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }

                retVal += new Integer(val).toString() + ", ";
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                String tlv = "t: " + tag + ", l: " + len + ", v: " +
                             new String(mibObjBytes, 2, len);
                m_log.info("getMIBValue " + tlv.trim());
                retVal += tlv + ", ";
            }
        }

        return retVal;
    }

    private int getMIBIntValue(String oid)
    {
        int retVal = 0;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("getMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            m_log.info("getMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBIntValue getMIBObject returned null");
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                int val = 0;

                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }

                retVal = val;
            }
        }

        return retVal;
    }

    private int getMIBBitsValue(String oid)
    {
        int retVal = 0;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBBitsValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("getMIBBitsValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_BITS)
        {
            m_log.info("getMIBBitsValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBBitsValue getMIBObject returned null");
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                int val = 0;

                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }

                retVal = val;
            }
        }

        return retVal;
    }

    private String getMIBStringValue(String oid)
    {
        String retVal = "Error - not found";
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBStringValue null returned from queryMibs");
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_OCTETSTRING)
        {
            m_log.info("getMIBStringValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBStringValue getMIBObject returned null");
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                retVal = new String(mibObjBytes, 2, len);
            }
        }

        return retVal.trim();
    }

    private boolean setMIBIntValue(String oid, int val)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("setMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("setMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            m_log.info("setMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            // BER encode integer result
            byte[] mibObjBytes = new byte[6];
            byte x = 0;
            int offset = 24;

            for(x = 0; x < 4; x++)
            {
                mibObjBytes[2+x] = (byte)((val >>> offset) & 0xFF);
                offset -= 8;
            }

            mibObjBytes[0] = MIBDefinition.SNMP_TYPE_INTEGER;
            mibObjBytes[1] = x;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            mibm.setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

    private boolean setMIBBitsValue(String oid, int val)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("setMIBBitsValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("setMIBBitsValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_BITS)
        {
            m_log.info("setMIBBitsValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            // BER encode integer result
            byte[] mibObjBytes = new byte[6];
            byte x = 0;
            int offset = 24;

            for(x = 0; x < 4; x++)
            {
                mibObjBytes[2+x] = (byte)((val >>> offset) & 0xFF);
                offset -= 8;
            }

            mibObjBytes[0] = MIBDefinition.SNMP_TYPE_BITS;
            mibObjBytes[1] = x;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            mibm.setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

    private boolean setMIBStringValue(String oid, String val, int strType)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("setMIBStringValue null returned from queryMibs");
        }
        else
        {
            byte[] valBytes = val.getBytes();
            byte[] mibObjBytes = new byte[2+valBytes.length];

            for(int x = 0; x < valBytes.length; x++)
            {
                mibObjBytes[2+x] = valBytes[x];
            }

            mibObjBytes[0] = (byte) strType;
            mibObjBytes[1] = (byte) valBytes.length;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            mibm.setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

}
