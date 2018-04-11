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
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValueError;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;

/**
 * This class is the base class for all the table MIB classes.
 * 
 * @author karunakarm
 * 
 */
public abstract class TableMIBModuleHandler extends LeafMIBModuleHandler
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(TableMIBModuleHandler.class.getName());
    
    private String[] rootColOIDs = null;
    private String tableOID = null;
    private String[] supportedOIDs = null;
    
    /**
     * The constructor.
     *  @param tableRootOID - The table root OID
     * @param rootColOIDs - array of column root OIDs
     */
    public TableMIBModuleHandler(String tableRootOID, String[] rootColOIDs)
    {
        this.rootColOIDs = rootColOIDs;
        this.tableOID = tableRootOID;
        this.supportedOIDs = new String[] {tableRootOID };
    }

    /**
     * This is the call back method for the <code>MIBListener</code>. Handles
     * and process the SNMP requests.
     * 
     * @param request
     *            - The incoming SNMP request.
     * @return snmpResponse - The <code>SNMPResponse</code> for the request.
     */
    public SNMPResponse notifySNMPRequest(SNMPRequest request)
    {
        SNMPResponse snmpResponse = null;
        MIBObject reqMIBObj = request.getMIBObject();
        if (validateOID(reqMIBObj.getOID()))
        {
            if (request.getRequestType() == SNMPRequest.SNMP_GET_NEXT_REQUEST)
            {
                snmpResponse = processGetNextRequest(reqMIBObj);
            }
            else
            {
                String oID = reqMIBObj.getOID();
                if (isRoot(oID) || isRootCol(oID) || isRootEntry(oID))
                {
                    try
                    {
                        snmpResponse = new SNMPResponseExt(reqMIBObj.getOID(), SNMPValueError.NO_SUCH_OBJECT);
                    }
                    catch (SNMPBadValueException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("Error while constructing SNMPResponseExt obj", e);
                        }
                    }
                }
                else
                {
                    snmpResponse = super.processRequest(request);
                }
            }
        }
        else
        {
            snmpResponse = new SNMPResponse(SNMPResponse.SNMP_REQUEST_AUTHORIZATION_ERROR, reqMIBObj);
        }
        return snmpResponse;
    }

    /**
     * This method checks whether incoming OID belongs to this listener or not.
     * 
     * @param mibOID
     *            - The OId for which data is requested.
     * @return found - Returns true if the OID belongs to the listener.
     */
    private boolean validateOID(String mibOID)
    {
        return (tableOID != null && mibOID.startsWith(tableOID));
    }

    /**
     * This method returns the next OID details of the given OID.
     * 
     * @param oID
     *            - The OID to which next OID details to be returned.
     * @return details - The string array containing the next OID and its row
     *         and column.
     */
    protected RowColDetails getNextOIDDetails(String oID)
    {
        int noOfRows = getRows();
        if (noOfRows == -1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Returning null. Since no of rows is -1");
            }
            return null;
        }
        
        String formattedOID = formatOIDIfRequired(oID);
        boolean changed = formattedOID.equals(oID) ? false : true; 

        RowColDetails rowColDetails = parseRowCol(formattedOID);
        int tempRow = rowColDetails.row;
        int row = changed ? tempRow : tempRow + 1;
        int col = rowColDetails.col;

        if (!isRowValid(row))
        {
            col = col + 1;
            row = 1;
        }

        if (!isValidColumn(col))
        {
            cleanUp();
            return null;
        }

        rowColDetails.setOID(appendNewRow(formattedOID, row, col));
        rowColDetails.setRow(row);
        rowColDetails.setCol(col);
        return rowColDetails;
    }
    
    /**
     * This method formats the OID by adding entry or row or column if they are
     * missing
     * @param oID - The OID to be formatted.
     * @return true if it is formatted.
     */
    private String formatOIDIfRequired(String oID)
    {
        if (isRoot(oID))
        {
            // adding index, column and row.
            oID = oID + ".1.1.1";
        }
        else if (isRootEntry(oID))
        {
            // add column and row
            oID = oID + ".1.1";
        }
        else if (isRootCol(oID))
        {
            // adding row.
            oID = oID + ".1";
        }
        return oID;
    }

    /**
     * This method verifies whether the oID passed is table root entry OID or not.
     * @param oID - 
     *          The OID to be verifed.
     * @return true - if the OID is table root entry OID
     */
    private boolean isRootEntry(String oID)
    {
        boolean rootIndex = false;
        if (this.tableOID != null)
        {
            String tableEntryOID = this.tableOID + ".1";
            rootIndex = tableEntryOID.equals(oID);
        }
        return rootIndex;
    }

    /**
     * This method determines whether any given column is valid or not.
     * 
     * @param col
     *            - The column to be verified.
     * @return true if it is valid column.
     */
    private boolean isValidColumn(int col)
    {
        return col <= getColumns();
    }

    /**
     * This method determines whether end of row is valid or not.
     * 
     * @param col
     *            - The row to be verified.
     * @return true - if it is valid row.
     */
    private boolean isRowValid(int row)
    {
        return row <= getRows();
    }

    /**
     * This method parses the OID and returns the row and col.
     * 
     * @param oID
     *            - The OID to be parsed for row and column.
     * @return int[] - The array containing the row and column.
     */
    protected RowColDetails parseRowCol(String oID)
    {
        String row = oID.substring(oID.lastIndexOf(".") + 1);
        oID = oID.substring(0, oID.lastIndexOf("."));
        String col = oID.substring(oID.lastIndexOf(".") + 1);
        return new RowColDetails(Integer.parseInt(row), Integer.parseInt(col));
    }

    /**
     * This method registers the OIDs with the <code>MIBManager</code>
     * 
     * @param oID
     *            - The OID to be registered.
     * @param dataType
     *            - The data type of the OID.
     */
    protected void registerOID(String oID, int dataType)
    {
        MIBManager.getInstance().registerOID(oID, MIBManager.MIB_ACCESS_READONLY, false, dataType, this);
    }

    /**
     * This method appends new row and column for the OID.
     * 
     * @param oID
     *            - The OID to which row id is to be appended.
     * @param row
     *            - The row id of the table.
     * @param col
     *            - The column id of the table.
     * @return oID - The new OID.
     */
    protected String appendNewRow(String oID, int row, int col)
    {
        // remove old row
        oID = oID.substring(0, oID.lastIndexOf("."));
        // remove old col
        oID = oID.substring(0, oID.lastIndexOf("."));

        return oID + "." + col + "." + row;
    }

    static protected class RowColDetails
    {
        private String OID = null;
        private int row = -1;
        private int col = -1;

        public RowColDetails(int row, int col, String OID)
        {
            this.row = row;
            this.col = col;
            this.OID = OID;
        }

        public RowColDetails(int row, int col)
        {
            this.row = row;
            this.col = col;
        }

        /**
         * @param oID
         *            the oID to set
         */
        public void setOID(String oID)
        {
            OID = oID;
        }

        public void setRow(int row)
        {
            this.row = row;
        }

        public void setCol(int col)
        {
            this.col = col;
        }

        /**
         * @return the oID
         */
        public String getOID()
        {
            return OID;
        }

        /**
         * @return the row
         */
        public int getRow()
        {
            return row;
        }

        /**
         * @return the col
         */
        public int getCol()
        {
            return col;
        }

        /**
         * Overriden to String.
         * 
         * @return sb - The contents of the objects.
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("Row: " + row);
            sb.append("\nColumn: " + col);
            sb.append("\nOID: " + OID);
            return sb.toString();
        }
    }

    /**
     * The method verifies whether the OID is the base table OID or not.
     * 
     * @param oID
     *            - The incoming OID to the listener.
     * @return boolean - true, if it is base table OID.
     */
    private boolean isRoot(String toValidateOID)
    {
        return tableOID.equals(toValidateOID);
    }
    
    /**
     * This method verifies whether the OID passed is table root OID or not.
     * @param oID -
     *          The OID to be verified.
     * @return true - if it is table root oid.
     */
    private boolean isRootCol(String oID)
    {
        boolean contains = false;
        if (rootColOIDs != null)
        {
            for (int i = 0; i < rootColOIDs.length; i++)
            {
                if (oID.equals(rootColOIDs[i]))
                {
                    contains = true;
                    break;
                }
            }
        }
        return contains; 
    }

    /**
     * This method process the GET_NEXT_REQUEST SNMP requests
     * 
     * @param mibObject
     *            - The <code>MIBObject</code> of the SNMP request.
     * @return response - The <code>SNMPResponse</code> of the SNMP request.
     */
    protected abstract SNMPResponse processGetNextRequest(MIBObject mibObject);

    /**
     * This method cleans is called whenever SNMP GET_NEXT_REQUEST returns null
     * as its response.
     * 
     * @param row
     *            - The row id of the table.
     * @param col
     *            - The column id of the table.
     */
    protected abstract void cleanUp();

    /**
     * This method returns the no of rows the table supports.
     * 
     * @return rows - The no of rows in the table.
     */
    protected abstract int getRows();

    /**
     * This method returns the no of columns the table supports.
     * 
     * @return columns - The no of columns in the table.
     */
    private int getColumns()
    {
        return this.rootColOIDs.length;
    }
    

    /**
     * This method lists all the OID's hosted by this module.
     * 
     * @return String[] - List of OID's
     */
    public String[] getOIDs()
    {
        return supportedOIDs;
    }
}
