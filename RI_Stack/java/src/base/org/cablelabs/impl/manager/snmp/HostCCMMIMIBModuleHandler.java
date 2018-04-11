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
package org.cablelabs.impl.manager.snmp;

import org.apache.log4j.Logger;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueError;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.cablelabs.impl.snmp.SNMPValueOctetString;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.PODApplication;

/**
 * This class registers all the columns of the CCI MIB table with the MIBManager
 * and also listener for all of the its columns.
 * 
 * @author karunakarm
 */
public class HostCCMMIMIBModuleHandler extends TableMIBModuleHandler implements MIBModuleHandler
{

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HostCCMMIMIBModuleHandler.class
            .getName());

    private static final String HOST_CC_MMI_MIB_TABLE = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1";
    private static final String HOST_CC_APP_INDEX_COL_ID = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1.1.1";
    private static final String HOST_CC_APP_TYPE_COL_ID = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1.1.2";
    private static final String HOST_CC_APP_NAME_COL_ID = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1.1.3";
    private static final String HOST_CC_APP_VERSION_COL_ID = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1.1.4";
    private static final String HOST_CC_INFO_PAGE_COL_ID = "1.3.6.1.4.1.4491.2.3.1.1.4.4.5.1.1.1.5";
    private static final String[] SUPPORTED_COLS = new String[] { HOST_CC_APP_INDEX_COL_ID, HOST_CC_APP_TYPE_COL_ID,
            HOST_CC_APP_NAME_COL_ID, HOST_CC_APP_VERSION_COL_ID, HOST_CC_INFO_PAGE_COL_ID };

    private PODApplication[] applications = null;
    private int noOfApps = -1;
    
    /**
     * The constructor.
     */
    public HostCCMMIMIBModuleHandler()
    {
        super(HOST_CC_MMI_MIB_TABLE, SUPPORTED_COLS);
    }

    /**
     * This method process the GET_NEXT_REQUEST SNMP requests
     * 
     * @param mibObject
     *            - The <code>MIBObject</code> of the SNMP request.
     * @return response - The <code>SNMPResponse</code> of the SNMP request.
     */
    protected SNMPResponse processGetNextRequest(MIBObject mibObject)
    {
        SNMPResponse response = null;
        fetchData();
        String oID = mibObject.getOID();
        RowColDetails rowColDetails = getNextOIDDetails(oID);
        if (rowColDetails == null)
        {
            try
            {
                response = new SNMPResponseExt(oID, SNMPValueError.END_OF_MIB_VIEW);
            }
            catch (SNMPBadValueException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error while parsing the value");
                }
            }
        }
        else
        {
            SNMPValue snmpValue = getValue(rowColDetails.getRow(), rowColDetails.getCol());
            try
            {
                response = new SNMPResponseExt(rowColDetails.getOID(), snmpValue);
            }
            catch (SNMPBadValueException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error while parsing the value");
                }
            }
        }
        return response;
    }

    /**
     * This method returns the value for the row and column.
     * 
     * @param row
     *            - The row id of the table.
     * @param column
     *            - The column id of the table.
     * @return - The value of the table cell in the byte array
     */
    public SNMPValue getValue(int row, int column)
    {
        PODApplication rowObj = getRow(row);
        if (log.isDebugEnabled())
        {
            log.debug("Querying for row: " + row + ", and the column is " + column);
        }
        SNMPValue snmpValue = getColumnValue(rowObj, column, row);
        return snmpValue;
    }

    /**
     * This method searches and find the row object in the collection
     * corresponding to the row id passed.
     * 
     * @param row
     *            - The row id of the table.
     * @return appAttributes - The <code>PODApplication</code> object.
     */
    private PODApplication getRow(int row)
    {
        PODApplication application = null;
        if ((row <= noOfApps) && (row > 0))
        {
            application = applications[row - 1];
        }
        return application;
    }

    /**
     * This method returns the column value for the given row and column.
     * 
     * @param appObj
     *            - The <code>PODApplication</code> from which data has to be
     *            retrieved.
     * @param column
     *            - The column Id of the table.
     * @param row
     *            - The row Id of the table.
     * @return res - The result of the column in the byte array.
     */
    private SNMPValue getColumnValue(PODApplication appObj, int column, int row)
    {
        SNMPValue snmpValue = null;

        switch (column)
        {
        case 1:
            // index column id.
            snmpValue = new SNMPValueInteger(row);
            break;
        case 2:
            // ocStbHostCCApplicationType
            snmpValue = new SNMPValueInteger(appObj.getType());
            break;

        case 3:
            // ocStbHostCCApplicationName
            snmpValue = new SNMPValueOctetString(appObj.getName());
            break;

        case 4:
            // ocStbHostCCApplicationVersion
            snmpValue = new SNMPValueInteger(appObj.getVersionNumber());
            break;

        case 5:
            // ocStbHostCCAppInfoPage
            snmpValue = new SNMPValueOctetString(appObj.getURL());
            break;

        default:
                if (log.isWarnEnabled())
                {
                    log.warn("Un expected column requested. Col Requested: " + column);
                }
            break;
        }
        return snmpValue;
    }

    /**
     * This method cleans all the data sources after final column is returned.
     * 
     * @param row
     *            - The row id of the table.
     * @param col
     *            - The column id of the table.
     */
    protected void cleanUp()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Cleaned up the data sources for the table.");
        }
        applications = null;
        noOfApps = -1;
    }

    /**
     * Fetches the data required for this table.
     */
    private void fetchData()
    {
        if (applications == null)
        {
            applications = POD.getInstance().getApplications();
            noOfApps = applications.length;
        }
    }


    /**
     * This method handles all the GET requests of the incoming SNMP request.
     * 
     * @param request
     *            - <code>SNMPRequest</code> object.
     * @return snmpResponse - The <code>SNMPResponse</code> object for the GET
     *         request.
     */
    public SNMPResponse processGetRequest(SNMPRequest request)
    {
        fetchData();
        MIBObject mibObject = request.getMIBObject();
        String oID = mibObject.getOID();
        RowColDetails rowColDetails = parseRowCol(oID);
        SNMPValue snmpValue = getValue(rowColDetails.getRow(), rowColDetails.getCol());
        SNMPResponse snmpResponse = null;
        try
        {
            snmpResponse = new SNMPResponseExt(oID, snmpValue);
        }
        catch (SNMPBadValueException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error while parsing the value");
            }

        }
        applications = null;
        return snmpResponse;
    }

    /**
     * This method reqisters all the MIBs associated with this module with the
     * <code>MIBManager</code> class.
     */
    public void registerMIBObjects()
    {
        registerOID(HOST_CC_MMI_MIB_TABLE, MIBDefinition.SNMP_TYPE_OCTETSTRING);
    }

    /**
     * This method returns the no of rows the table supports.
     * 
     * @return rows - The no of rows in the table.
     */
    protected int getRows()
    {
        return noOfApps;
    }
}
