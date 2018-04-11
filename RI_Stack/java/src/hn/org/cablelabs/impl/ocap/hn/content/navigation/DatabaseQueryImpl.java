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

package org.cablelabs.impl.ocap.hn.content.navigation;

import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.DatabaseExceptionImpl;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.DatabaseException;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.DatabaseQuery;

/**
 * DatabaseQueryImpl subclasses DatabaseQuery, and is used to implement
 * ContentDatabaseFilter.
 * 
 * @author Michael A. Jastad
 * @version $Revision$
 * 
 * @see
 */
public class DatabaseQueryImpl extends DatabaseQuery
{
    /** Property field definition */
    private String m_primaryFieldName = "";

    /** Property Value to compare with */
    private String m_primaryValue = "";

    /** Local compare operator reference */
    private int m_primaryOperator = -1;

    /** Local compare operator reference */
    private DatabaseQueryImpl m_remoteQuery = null;

    /** Local AND compare operation */
    private static final int m_AND = 0;

    /** Local OR compare operation */
    private static final int m_OR = 1;

    /** Local NOT compare operation */
    private static final int m_NOT = 2;

    /** Local NOT compare operation */
    private int m_secondaryOperator = -1;

    /**
     * Creates a new DatabaseQueryImpl object.
     * 
     * @param fieldName
     *            The name of the property
     * @param compare
     *            The operator used to compare the value(s)
     * @param value
     *            The value to be filtered on.
     */
    public DatabaseQueryImpl(String fieldName, int compare, String value)
    {
        m_primaryFieldName = fieldName;
        m_primaryValue = value;
        m_primaryOperator = compare;
    }

    /**
     * Overrides the OCAP HN DatabaseQuery class.
     * 
     * @param fieldName
     *            The name of the property field.
     * @param comparison
     *            The operator used to compare fieldName and value
     * @param value
     *            The value of the property field to test for when filtering
     * 
     * @return DatabaseQuery the Query to be used by a consumer
     * 
     * @throws DatabaseException
     *             Thrown if the arguments do meet requirements
     */
    public static DatabaseQuery newInstance(String fieldName, int comparison, String value) throws DatabaseException
    {
        DatabaseQueryImpl databaseQuery = null;

        if (isNull(fieldName) || isEmpty(fieldName) || isNull(value) || isEmpty(value))
        {
            throw new DatabaseExceptionImpl("", DatabaseException.FIELD_IS_EMPTY);
        }
        else if (!isValid(comparison))
        {
            throw new DatabaseExceptionImpl("", DatabaseException.INVALID_PARAMETER_SPECIFIED);
        }
        else if (!isValid(value) || !isValid(fieldName))
        {
            throw new DatabaseExceptionImpl("", DatabaseException.FIELD_IS_WRONG_FORMAT);
        }
        else
        {
            databaseQuery = new DatabaseQueryImpl(fieldName, comparison, value);
        }

        return databaseQuery;
    }

    /**
     * Returns a DatabaseQuery representing a logical 'AND' of this
     * DatabaseQuery, and a Specified DatabaseQuery.
     * 
     * @param query
     *            DataBaseQuery to be ANDed.
     * 
     * @return DatabaseQuery the result of the logical AND between this
     *         DatabaseQuery and the specified DatabaseQuery.
     * 
     * @throws DatabaseException
     *             Thrown if the DatabaseQuery is null or the contentNode is
     *             null or an empty string.
     * 
     */
    public DatabaseQuery and(DatabaseQuery query) throws DatabaseException
    {
        DatabaseQueryImpl databaseQuery = null;

        if (isValid((DatabaseQueryImpl) query))
        {
            m_secondaryOperator = m_AND;
            m_remoteQuery = (DatabaseQueryImpl) query;

            databaseQuery = this;
        }
        else
        {
            throw new DatabaseExceptionImpl("Invalid Query", DatabaseException.REMOTE_QUERY_IS_INVALID);
        }

        return databaseQuery;
    }

    /**
     * Returns a DatabaseQuery representing a logical 'AND' of this
     * DatabaseQuery, and a Specified DatabaseQuery.
     * 
     * @param query
     *            DataBaseQuery to be ANDed
     * @param contextNode
     * 
     * @return DatabaseQuery the result of the logical AND between this
     *         DatabaseQuery and the specified DatabaseQuery.
     * 
     * @throws DatabaseException
     *             Thrown if the DatabaseQuery is null or the contentNode is
     *             null or an empty string.
     * 
     */
    public DatabaseQuery and(DatabaseQuery query, String contextNode) throws DatabaseException
    {
        DatabaseQueryImpl databaseQuery = null;

        if (isValid((DatabaseQueryImpl) query))
        {
            m_secondaryOperator = m_AND;
            m_remoteQuery = (DatabaseQueryImpl) query;

            databaseQuery = this;
        }
        else
        {
            throw new DatabaseExceptionImpl("Invalid Query", DatabaseException.REMOTE_QUERY_IS_INVALID);
        }

        return databaseQuery;
    }

    /**
     * Returns the reciprical of this DatabaseQuery
     * 
     * @return DatabaseQuery
     */
    public DatabaseQuery negate()
    {
        m_secondaryOperator = m_NOT;
        return this;
    }

    /**
     * Performs a Logical OR using this DatabaseQuery, and the Query passed in
     * as an argument, and generates the Logical equivalence in the form of a
     * DatabaseQuery
     * 
     * @param query
     *            The specified DatabaseQuery to be ORed with this DatabaseQuery
     * 
     * @return DatabaseQuery the result of ORing this DatabaseQuery and the
     *         specified query.
     * 
     * @throws DatabaseException
     *             Thrown if the argument is invalid or null.
     */
    public DatabaseQuery or(DatabaseQuery query) throws DatabaseException
    {
        DatabaseQueryImpl databaseQuery = null;

        if (isValid((DatabaseQueryImpl) query))
        {
            m_secondaryOperator = m_OR;
            m_remoteQuery = (DatabaseQueryImpl) query;

            databaseQuery = this;
        }
        else
        {
            throw new DatabaseExceptionImpl("Invalid Query", DatabaseException.REMOTE_QUERY_IS_INVALID);
        }

        return databaseQuery;
    }

    /**
     * Returns the primary field of this DatabaseQuery
     * 
     * @return String Field
     */
    public String getField()
    {
        return m_primaryFieldName;
    }

    /**
     * Returns the primary Operator of this DatabaseQuery
     * 
     * @return int operator
     */
    public int getOperator()
    {
        return m_primaryOperator;
    }

    /**
     * Returns the primary Value of this DatabaseQuery
     * 
     * @return String value
     */
    public String getValue()
    {
        return m_primaryValue;
    }

    /**
     * Returns the secondary Operator of this DatabaseQuery
     * 
     * @return int secondary operator
     */
    public int getSecondaryOperator()
    {
        return m_secondaryOperator;
    }

    /**
     * Returns the remote query of this DatabaseQuery
     * 
     * @return DatabaseQuery remote query
     */
    public DatabaseQueryImpl getRemoteQuery()
    {
        return m_remoteQuery;
    }

    /**
     * Checks to see if a String item is null
     * 
     * @param item
     *            The String item to be checked
     * 
     * @return boolean True if the String item is null
     */
    private static boolean isNull(String item)
    {
        return (item == null);
    }

    /**
     * Checks if a specified field is an empty string
     * 
     * @param field
     *            The specified field to be checked
     * 
     * @return boolean True if the specified field is empty
     */
    private static boolean isEmpty(String field)
    {
        boolean status = true;

        if (!isNull(field))
        {
            status = (field.equalsIgnoreCase("NULL") || field.equalsIgnoreCase(""));
        }

        return status;
    }

    /**
     * Checks if the specified field is valid. Validation is performed by
     * checking the field for unsupported characters
     * 
     * @param field
     *            the specified field to be validated
     * 
     * @return boolean true if the field is valid.
     */
    private static boolean isValid(String field)
    {
        boolean status = false;

        if (!isNull(field))
        {
            status = (field.indexOf(">") == -1) && (field.indexOf("<") == -1) && (field.indexOf("!") == -1)
                    && (field.indexOf(".") == -1);
        }

        return status;
    }

    /**
     * Checks if the specified field operator is valid. The operator is valid if
     * it's value is between a specified range.
     * 
     * @param field
     *            The field operator to be validated
     * 
     * @return boolean True if the field operator is within range
     */
    private static boolean isValid(int field)
    {
        return ((field >= DatabaseQuery.EQUALS) && (field <= DatabaseQuery.EXISTS));
    }

    /**
     * Checks if the specified field operator is valid. The operator is valid if
     * it's value is between a specified range.
     * 
     * @param field
     *            The field operator to be validated
     * 
     * @return boolean True if the field operator is within range
     */
    private boolean isValid(DatabaseQueryImpl query)
    {
        return ((query != null) && (!query.equals(this)) && isValid(query.getField()) && isValid(query.getValue()) && isValid(query.getOperator()));

    }

    /**
     * Verifies that the ContentEntry arguments metadata matches the query
     * specified by this DatabaseQuery
     * 
     * @param entry
     *            the ContentCntry to be tested with the query.
     * 
     * @return boolean True if the Content metadata matches the specified query.
     */
    public boolean accept(ContentEntry entry)
    {
        return doAccept((ContentEntryImpl) entry, this);
    }

    /**
     * Verifies that the ContentEntry arguments metadata matches the query
     * specified by this DatabaseQuery
     * 
     * @param entry
     *            the ContentCntry to be tested with the query.
     * 
     * @return boolean True if the Content metadata matches the specified query.
     */
    public boolean doAccept(ContentEntryImpl entry, DatabaseQueryImpl dbQuery)
    {
        boolean status = false;
        boolean op1 = false;
        boolean op2 = false;

        if (isValid(dbQuery.getRemoteQuery()))
        {
            status = doAccept(entry, dbQuery.getRemoteQuery());
        }

        if (dbQuery.getSecondaryOperator() == m_AND)
        {
            op1 = filter(entry.getRootMetadataNode(), dbQuery.getOperator(), dbQuery.getValue(), dbQuery.getField());

            status = (status && op1);
        }
        else if (dbQuery.getSecondaryOperator() == m_OR)
        {
            op1 = filter(entry.getRootMetadataNode(), dbQuery.getOperator(), dbQuery.getValue(), dbQuery.getField());

            status = (status || op1);
        }
        else if (dbQuery.getSecondaryOperator() == m_NOT)
        {
            status = !filter(entry.getRootMetadataNode(), dbQuery.getOperator(), dbQuery.getValue(), dbQuery.getField());
        }
        else
        {
            status = filter(entry.getRootMetadataNode(), dbQuery.getOperator(), dbQuery.getValue(), dbQuery.getField());
        }

        return status;
    }

    /**
     * Filter operator performs the operation.
     * 
     * @param metadataNode
     *            The ContentEntry metadata
     * 
     * @return boolean True if the metadata matches the query operator.
     */
    private boolean filter(MetadataNode metadataNode, int operator, String value, String field)
    {
        boolean match = false;
        if (metadataNode == null)
        {
            return false;
        }

        Object object = metadataNode.getMetadata(field);

        if (object instanceof String)
        {
            String strObj = (String) object;

            switch (operator)
            {

                /* 1 */
                case DatabaseQuery.EQUALS:
                    match = strObj.equalsIgnoreCase(value);
                    break;

                /* 2 */
                case DatabaseQuery.GREATER_THAN:
                    match = strObj.compareTo(value) > 0;
                    break;

                /* 3 */
                case DatabaseQuery.LESS_THAN:
                    match = strObj.compareTo(value) < 0;
                    break;

                /* 4 */
                case DatabaseQuery.GREATER_THAN_OR_EQUALS:
                    match = strObj.compareTo(value) >= 0;
                    break;

                /* 5 */
                case DatabaseQuery.LESS_THAN_OR_EQUALS:
                    match = strObj.compareTo(value) <= 0;
                    break;

                /* 6 */
                case DatabaseQuery.CONTAINS:
                    match = strObj.indexOf(value) != -1;
                    break;

                /* 7 */
                case DatabaseQuery.NOT_EQUALS:
                    match = !(strObj.equalsIgnoreCase(value));
                    break;

                /* 8 */
                case DatabaseQuery.EXISTS:
                    match = true;
                    break;
            }
        }
        return match;
    }
}
