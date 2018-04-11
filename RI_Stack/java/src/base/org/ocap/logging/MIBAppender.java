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

package org.ocap.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import org.cablelabs.impl.manager.ProgressListener;
import org.cablelabs.impl.manager.progress.ProgressMgr;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBObject;

import org.cablelabs.impl.debug.Debug;

/**
 * The <i>MIBAppender</i> appends events to a circular queue which is made
 * available at a given OID.  The [MIB-HOST] specification defines a table
 * specifically for this purpose, for which an application can use the OID
 * of the ocStbHostSystemLoggingTable.
 *
 * set/getSize or get/setLevelMin may be called during initial configuration, which depends on MIBManager being already started.
 * 
 * As initial logging configuration happens prior to manager initialization, this is a cycle that has to be broken.
 * 
 * The implementation now caches level/size updates and applies them once the kStartBoot signal is received by the ProgressListener.
 * 
 * Also inlining MIBManager getInstance calls to avoid a static load-time dependency.
 *
 */
public class MIBAppender extends AppenderSkeleton
{
    private String ocStbHostSystemLoggingSize_oid =
                   "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.2";
    private String ocStbHostSystemLoggingLevelControl_oid =
                   "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.3";
    private String ocStbHostSystemLoggingEventTable_oid =
                   "1.3.6.1.4.1.4491.2.3.1.1.4.3.5.5";
    private String oid = ocStbHostSystemLoggingEventTable_oid;
    private int size;
    private Level levelMin = Level.TRACE;
    volatile boolean initialized = false;
    boolean initializeLevel = false;
    boolean initializeSize = false;

    private final ProgressListener progressListenerImpl = new ProgressListenerImpl();
    
    public void activateOptions()
    {
        ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();
        progressMgr.addListener(progressListenerImpl);
    }
    /**
     * Closes the MIB appender.
     */
    public void close()
    {
        LogLog.debug("close");
    }

    /**
     * Appends a logging event to the MIB Appender.  If the OID set for this
     * MIB Appender is not an OID to a known circular logging table this method
     * does nothing successfully. Since the event is in a table maintained by
     * the underlying handler, writing the timestamp/message pair increments
     * internal table row pointers. New events are always added at index 1.
     */
    public void append(LoggingEvent event)
    {
        if ((event.getLevel()).toInt() >= levelMin.toInt())
        {
            Debug.AddLogEntry(ocStbHostSystemLoggingEventTable_oid,
                              new Long(event.getTimeStamp()).toString(),
                              layout.format(event));
        }
    }

    /**
     * Sets the OID that this MIBAppender should be attached to. This is a
     * required value for the <i>MIBAppender</i> and there is no default.
     *
     * @param  oid The OID for the MIB node where the queue of logger messages
     *      will be accessible.
     *
     */
    public void setOid(String oid)
    {
        LogLog.debug("setoid: " + oid);
        this.oid = oid;
    }
    
    public String getOid()
    {
        return oid;
    }

    /**
     * Set the size of the circular queue in number of messages. (Default: 10)
     *
     * @param  size  the size to make the circular queue
     */
    public void setSize(int size)
    {
        this.size = size;
        if (initialized)
        {
            setMIBIntValue(ocStbHostSystemLoggingSize_oid + ".0", size);
            LogLog.debug("setSize: " + size);
        }
        else
        {
            initializeSize = true;
        }
    }
    
    public int getSize()
    {
        if (initialized)
        {
            this.size = getMIBIntValue(ocStbHostSystemLoggingSize_oid);
            LogLog.debug("getSize: " + this.size);
        }
        return this.size;
    }

    /**
     * Set the minimum priority level that this Appender will actually execute
     * for. Although a Logger may be configured to send a message based upon its
     * current level, this level will further restrict the verbosity of this
     * specific Appender. By default, this is set to {@link Level#ALL}.
     *
     * @param levelMin The minimum priority level that this appender will
     *      actually log.
     */
    public void setLevelMin(Level levelMin)
    {
        this.levelMin = levelMin;
        if (initialized)
        {
            setMIBIntValue(ocStbHostSystemLoggingLevelControl_oid + ".0",
                           levelMin.toInt());
            LogLog.debug("setLevelMin: " + levelMin);
        }
        else
        {
            initializeLevel = true;
        }
    }

    public Level getLevelMin()
    {
        if (initialized)
        {
            int level = getMIBIntValue(ocStbHostSystemLoggingLevelControl_oid);
            this.levelMin = Level.toLevel(level);
            LogLog.debug("getLevelMin: " + levelMin);
        }
        return this.levelMin;
    }

    /**
     * Accessor reporting if MIBAppender requires a layout
     * @return true
     */
    public boolean requiresLayout()
    {
        return true;
    }

    /**
     * Helper to get an int value from an SNMP MIB ObjectID
     * @param oid the Object ID to query
     * @return the int value obtained else 0;  TODO: needs error handling
     */
    private int getMIBIntValue(String oid)
    {
        int retVal = 0;
        MIBDefinition[] mibd = MIBManager.getInstance().queryMibs(oid);

        if (mibd == null)
        {
            LogLog.error("getMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            LogLog.error("getMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            LogLog.error("getMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                LogLog.error("getMIBIntValue getMIBObject returned null");
            }
            else
            {
                // BER decode the integer value
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

    /**
     * Helper to set an int value on an SNMP MIB ObjectID
     * @param oid the Object ID to modify
     * @param val the int value to write
     * @return boolean success/fail
     */
    private boolean setMIBIntValue(String oid, int val)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = MIBManager.getInstance().queryMibs(oid);

        if (mibd == null)
        {
            LogLog.error("setMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            LogLog.error("setMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            LogLog.error("setMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            // BER integer result
            byte[] mibObjBytes = new byte[6];
            byte x = 0;
            int offset = 24;

            for(x = 0; x < 4; x++)
            {
                mibObjBytes[2+x] = (byte)((val >>> offset) & 0xFF);
                offset -= 8;
            }

            mibObjBytes[0] = MIBDefinition.SNMP_TYPE_INTEGER;
            mibObjBytes[1] = 4;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            MIBManager.getInstance().setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

    /**
     * Helper to set a string value on an SNMP MIB ObjectID
     * @param oid the Object ID to modify
     * @param val the string value to write
     * @param strType the type of string value to write
     * @return boolean success/fail
     */
    private boolean setMIBStringValue(String oid, String val, int strType)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = MIBManager.getInstance().queryMibs(oid);

        if (mibd == null)
        {
            LogLog.error("setMIBStringValue null returned from queryMibs");
        }
        else
        {
            // BER string result
            byte[] valBytes = val.getBytes();
            byte[] mibObjBytes = new byte[2+valBytes.length];

            for(int x = 0; x < valBytes.length; x++)
            {
                mibObjBytes[2+x] = valBytes[x];
            }

            mibObjBytes[0] = (byte) strType;
            mibObjBytes[1] = (byte) valBytes.length;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            MIBManager.getInstance().setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

    private class ProgressListenerImpl implements ProgressListener 
    {
        public void progressSignalled(int milestone) 
        {
            if (ProgressMgr.kStartBoot == milestone)
            {
                LogLog.debug("received 'kStartBoot' signal");
                initialized = true;
                if (initializeLevel)
                {
                    LogLog.debug("updating level on the MIB to: " + levelMin);
                    setLevelMin(levelMin);
                }
                if (initializeSize)
                {
                    LogLog.debug("updating size on the MIB to: " + size);
                    setSize(size);
                }
                //ok to remove listener here
                ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();
                progressMgr.removeListener(progressListenerImpl);
            }
        }
    }
}
