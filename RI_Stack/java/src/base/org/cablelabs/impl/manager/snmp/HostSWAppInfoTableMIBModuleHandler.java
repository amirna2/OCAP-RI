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

import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.application.AppAttributesExt;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueError;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.cablelabs.impl.snmp.SNMPValueOctetString;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.RunningApplicationsFilter;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;


/**
 * This class registers all the columns of the table with the MIBManager and
 * also listener for all of the its columns.
 * 
 * @author karunakarm
 * @author krushnar
 */
public class HostSWAppInfoTableMIBModuleHandler extends TableMIBModuleHandler implements MIBModuleHandler
{

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HostSWAppInfoTableMIBModuleHandler.class
            .getName());

    private static final String HOST_SWAPP_TABLE_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1";
    private static final String HOST_SWAPP_NAME_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.1";
    private static final String HOST_SWAPP_VERSION_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.2";
    private static final String HOST_SW_STATUS_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.3";
    private static final String HOST_SWAPP_INFO_INDEX_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.4";
    private static final String HOST_SW_ORG_ID_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.5";
    private static final String HOST_SW_APP_ID_COL_OID = "1.3.6.1.4.1.4491.2.3.1.1.3.3.1.1.6";
    private static final String[] SUPPORTED_COLS = new String[] { HOST_SWAPP_NAME_COL_OID, HOST_SWAPP_VERSION_COL_OID,
            HOST_SW_STATUS_COL_OID, HOST_SWAPP_INFO_INDEX_COL_OID, HOST_SW_ORG_ID_COL_OID, HOST_SW_APP_ID_COL_OID, };

    private AppsDatabase appDB = null;
    private int noOfApps = -1;
    
    /**
     * The constructor.
     */
    public HostSWAppInfoTableMIBModuleHandler()
    {
        super(HOST_SWAPP_TABLE_OID, SUPPORTED_COLS);
    }

    /**
     * This method reqisters all the MIBs associated with this module with the
     * <code>MIBManager</code> class.
     */
    public void registerMIBObjects()
    {
        registerOID(HOST_SWAPP_TABLE_OID, MIBDefinition.SNMP_TYPE_OCTETSTRING);
    }

    /**
     * This method process the GET_NEXT_REQUEST SNMP requests
     * 
     * @param mibObject
     *            - The <code>MIBObject</code> of the SNMP request.
     * @return response - The <code>SNMPResponse</code> of the SNMP request.
     */
    public SNMPResponse processGetNextRequest(MIBObject mibObject)
    {
        SNMPResponse response = null;

        getAppsInfo();
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
     * This method handles all the GET requests of the incoming SNMP request.
     * 
     * @param request
     *            - <code>SNMPRequest</code> object.
     * @return snmpResponse - The <code>SNMPResponse</code> object for the GET
     *         request.
     */
    public SNMPResponse processGetRequest(SNMPRequest request)
    {
        getAppsInfo();
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
        appDB = null;
        return snmpResponse;
    }

    /**
     * This method cleans is called whenever SNMP GET_NEXT_REQUEST returns null
     * as its response.
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
        appDB = null;
        noOfApps = -1;
    }

    /**
     * This method queries for the apps DB.
     */
    private void getAppsInfo()
    {
        if (appDB == null)
        {
            appDB = AppsDatabase.getAppsDatabase();
            if (appDB == null)
            {
                return;
            }
            noOfApps = appDB.size();
        }
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
        AppAttributes rowObj = getRow(row);
        if (log.isDebugEnabled())
        {
            log.debug("Querying for row: " + row + ", and the column is " + column);
        }
        return getColumnValue(rowObj, column, row);
    }

    /**
     * This method returns the column value for the given row and column.
     * 
     * @param appObj
     *            - The <code>AppAttributes</code> from which data has to be
     *            retrieved.
     * @param column
     *            - The column Id of the table.
     * @param row
     *            - The row Id of the table.
     * @return res - The result of the column in the byte array.
     */
    private SNMPValue getColumnValue(AppAttributes appObj, int column, int row)
    {

        SNMPValue snmpValue = null;
        switch (column)
        {
        case 1:
            String appName = appObj.getName();
                if (log.isDebugEnabled())
                {
                    log.debug("getValue() :: appName  " + appName);
                }
            // ocStbHostSoftwareAppNameString
            snmpValue = new SNMPValueOctetString(appName);
            break;

        case 2:
            // get the application version number.
            String version = ((AppAttributesExt) appObj).getAppVersion();
                if (log.isDebugEnabled())
                {
                    log.debug("getValue) ::appVersion " + version);
                }
            snmpValue = new SNMPValueOctetString(version);
            break;

        case 3:
            // get the software status.
            AppProxy proxy = appDB.getAppProxy(appObj.getIdentifier());
            int state = proxy.getState();
                if (log.isDebugEnabled())
                {
                    log.debug("getValue() :: the app State " + state);
                }
            snmpValue = new SNMPValueInteger(state);
            break;

        case 4:
            // returning the index
            snmpValue = new SNMPValueInteger(row);
            break;

        case 5:
            // get the ORG ID.
            String orgID = String.valueOf(appObj.getIdentifier().getOID());
                if (log.isDebugEnabled())
                {
                    log.debug("getValue():: the orgId " + orgID);
                }
            snmpValue = new SNMPValueOctetString(orgID);
            break;

        case 6:
            // get the APPID.
            String appID = String.valueOf(appObj.getIdentifier().getAID());
                if (log.isDebugEnabled())
                {
                    log.debug("getValue() :: the app ID is " + appID);
                }
            snmpValue = new SNMPValueOctetString(appID);
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
     * This method searches and find the row object in the collection
     * corresponding to the row id passed.
     * 
     * @param row
     *            - The row id of the table.
     * @return appAttributes - The
     *         <code>org.dvb.application.AppAttributes</code> object.
     */
    private AppAttributes getRow(int row)
    {
        int counter = 1;
        AppAttributes appAttributes = null;
        Enumeration attributesCollection = appDB.getAppAttributes(new RunningApplicationsFilter());
        while (attributesCollection.hasMoreElements())
        {
            appAttributes = (AppAttributes) attributesCollection.nextElement();
            if (counter == row)
            {
                break;
            }
            counter++;
        }
        return appAttributes;
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
