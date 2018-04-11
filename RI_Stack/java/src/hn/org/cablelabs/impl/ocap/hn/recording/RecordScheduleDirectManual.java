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

package org.cablelabs.impl.ocap.hn.recording;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NamedNodeMap;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NodeList;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.hn.content.MetadataNode;

/**
 * This class represents the OBJECT.RECORDSCHEDULE.DIRECT.MANAUAL UPnP class. In
 * UPnP terminology it is used to create recordSchedule instances for manual
 * schedule of recordings uniquely identified by the scheduledChannelID,
 * scheduledStartDateTime, and scheduledDuration as defined by UPnP SRS
 * specification. This class supports property usage for recordScheduleParts,
 * the CreateRecordSchedule action input parameter, or recordSchedule, the reply
 * parameter for various SRS actions. See Table C-7 in the UPnP SRS
 * specification.
 *
 * @author Dan Woodard
 *
 */
public class RecordScheduleDirectManual
{
    public static final String CLASS_PROPERTY = "OBJECT.RECORDSCHEDULE.DIRECT.MANUAL";

    // This property helps in deciding whether to populate the default class
    // property when a create Recording Schedule request is received.
    public static final String DEFAULT_CLASS_PROPERTY = "OCAP.hn.server.schedule.default.classproperty";

    /*
     * Defines the usage level of this object.recordSchedule.direct.manual
     * instance The usage determines which properties are allowed.
     */
    public static final int RECORD_SCHEDULE_PARTS_USAGE = 1; // request: client -> server
    public static final int RECORD_SCHEDULE_USAGE       = 2; // response: server -> client

    private static final String AVDT_FIELD_BEG_XML =
        "<field>\n" +
        "<name>";
    private static final String AVDT_FIELD_STD_END_XML =
        "</name>\n" +
        "<dataType>xsd:string</dataType>\n" +
        "<minCountTotal>1</minCountTotal>\n" +
        "<allowedValueDescriptor>\n" +
        "<allowAny></allowAny>\n" +
        "</allowedValueDescriptor>\n" +
        "</field>\n";

    /**
     * Construct an instance using the metadata from a MetadataNode.
     * <p>
     * To validate this instance, call isValid() after construction completes.
     * <p>
     * Only the first level metadata Strings from the input MetadataNode will be
     * used. If the MetadataNode contains other MetadataNodes or data type other
     * than String, they will be ignored. Only metadata appropriate for the
     * input usage will be added.
     *
     * @param metadataNode
     *            The MetadataNode from which to take properties and values. For
     *            requests, the client invokes this constructor, and the
     *            MetadataNode is from the NetRecordingSpec; for responses, the
     *            server invokes this constructor, and the MetadataNode is from
     *            the NetRecordingEntry. See steps 6 and 14 of Appendix I-1 of
     *            OC-SP-HNP2.0-I02-091217, respectively.
     *
     * @param usage
     *            Determines which properties are allowed for a usage of
     *            RECORD_SCHEDULE_PARTS_USAGE or RECORD_SCHEDULE_USAGE.
     */
    public RecordScheduleDirectManual(MetadataNodeImpl metadataNode, int usage)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Constructing a RecordScheduleDirectManual from a MetadataNodeImpl, usage " + prettyUsage(usage));
        }

        // can only create an instance if it is one of the allowed usages
        if (usage != RECORD_SCHEDULE_PARTS_USAGE && usage != RECORD_SCHEDULE_USAGE)
        {
            throw new IllegalArgumentException("unexpected usage: " + usage);
        }

        isRequest = usage == RECORD_SCHEDULE_PARTS_USAGE;

        initInstance();

            if (isRequest)
            {
                thisParticipant = "Client HNIMP";
                dataOrigin = "NetRecordingSpec MetadataNode";
            }
            else
            {
                thisParticipant = "Server HNIMP";
                dataOrigin = "NetRecordingEntry MetadataNode";
            }

        if (log.isDebugEnabled())
        {
            log.debug("Build RecordSchedule with " + dataOrigin + " keys");
        }

        recursiveBuild(metadataNode); // build from the entire node tree

        setDefaults(); // set the default property values
    }

    /**
     * Construct an instance using the metadata from a DOM Node.
     * <p>
     * To validate this instance, call isValid() after construction completes.
     *
     * @param node
     *            The DOM Node from which to take properties and values. For
     *            requests, the server invokes this constructor, and the DOM
     *            Node is from the NetRecordingSpec; for responses, the client
     *            invokes this constructor, and the DOM Node is from the
     *            NetRecordingEntry. See steps 7 and 15 of Appendix I-1 of
     *            OC-SP-HNP2.0-I02-091217, respectively.
     *
     * @param usage
     *            Determines which properties are allowed for a usage of
     *            RECORD_SCHEDULE_PARTS_USAGE or RECORD_SCHEDULE_USAGE.
     */
    public RecordScheduleDirectManual(Node node, int usage)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Constructing a RecordScheduleDirectManual from a DOM Node, usage " + prettyUsage(usage));
        }

        // can only create an instance if it is one of the allowed usages
        if (usage != RECORD_SCHEDULE_PARTS_USAGE && usage != RECORD_SCHEDULE_USAGE)
        {
            throw new IllegalArgumentException("unexpected usage: " + usage);
        }

        isRequest = usage == RECORD_SCHEDULE_PARTS_USAGE;

        initInstance();

            if (isRequest)
            {
                thisParticipant = "Server HNIMP";
                dataOrigin = "NetRecordingSpec DOM Node";
            }
            else
            {
                thisParticipant = "Client HNIMP";
                dataOrigin = "NetRecordingEntry DOM Node";
            }

        if (log.isDebugEnabled())
        {
            log.debug("Build RecordSchedule with " + dataOrigin + " keys");
        }

        recursiveBuild(node); // build from the entire node tree

        setDefaults(); // set the default property values
    }

    /**
     * Sets the value for the input property.
     *
     * @param property
     *            The input property is restricted to the statics defined in
     *            this class. If the property is not defined here, the value
     *            will not be stored.
     * @param value
     *            The value to store for the property. Any previous value is
     *            replaced.
     * @return true if the value was successfully stored, false if not.
     */
    public boolean setValue(QualifiedName property, String value)
    {
        if (property != null && value != null && isValidPropertyName(property))
        {
            synchronized (properties)
            {
                properties.put(property, value);

                return true;
            }
        }

        return false;
    }

    /**
     * Creates a copy of the properties associated with this record schedule so
     * it can be used with associated record tasks.
     *
     * @return  HashMap containing the QualifiedNames and values associated with
     * this record schedule.
     */
    public Map getPropertiesCopy()
    {
        Map props = new HashMap();
        synchronized (properties)
        {
            for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iter.next();
                QualifiedName propName = (QualifiedName) entry.getKey();
                String propValue = (String) entry.getValue();
                props.put(propName, propValue);
            }
        }
        return props;
    }

    /**
     * Returns the value for the ocap:cdsReference property.
     *
     * @return The value for the ocap:cdsReference property or null if the
     *         value does not exist.
     */
    public String getCdsReference()
    {
        synchronized (properties)
        {
            return (String) properties.get(UPnPConstants.QN_OCAP_CDS_REFERENCE);
        }
    }

    /**
     * Returns the value for the srs:@id property.
     *
     * @return The value for the srs:@id property or null if the
     *         value does not exist.
     */
    public String getID()
    {
        synchronized (properties)
        {
            return (String) properties.get(UPnPConstants.QN_SRS_ID_ATTR);
        }
    }

    /**
     * Factory for NetRecordingEntryLocal MetadataNodeImpl instances based on
     * property/value pairs in this instance.
     *
     * @return MetadataNodeImpl that contains all the property/value pairs from
     *         this instance that are suitable for a NetRecordingEntryLocal.
     */
    public MetadataNodeImpl getNetRecordingEntryMetadataNode()
    {
        // root node has null key
        MetadataNodeImpl rootNode = (MetadataNodeImpl) MetadataNode.createMetadataNode();

        for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iter.next();
            QualifiedName propName = (QualifiedName) entry.getKey();
            String propValue = (String) entry.getValue();

            assert isRequest;

            propName = srsToOcap(propName);

            // Ignore attribute names. Attributes are handled for each element
            // below.
            if (ATTRIBUTENAMES.contains(propName))
            {
                continue;
            }

            if (UPnPConstants.NSN_SRS.equals(propName.namespaceName()))
            {
                continue;
            }

            // get value for property name
            if(propValue != null && propValue.length() > 0)
            {
                rootNode.addMetadata(propName, propValue);
            }

            //
            // See if there is an attribute for this property name element.
            //
            Object obj = PROPERTYATTRIBUTEMAP.get(propName);

            if (obj != null)
            {
                if (obj instanceof QualifiedName)
                {
                    QualifiedName attributeName = (QualifiedName) obj;
                    String attributeValue = (String) properties.get(attributeName);

                    rootNode.addMetadata(attributeName, attributeValue);
                }
                else if (obj instanceof Vector)
                {
                    // multiple attributes for this element
                    Enumeration enumeration = ((Vector) obj).elements();

                    while (enumeration.hasMoreElements())
                    {
                        QualifiedName attributeName = (QualifiedName) enumeration.nextElement();
                        String attributeValue = (String) properties.get(attributeName);

                        rootNode.addMetadata(attributeName, attributeValue);
                    }
                }
                // else, can't happen unless a mistake is made in
                // initInstance();
            }
        }

        return rootNode;
    }

    /**
     * Validates if only the allowed values are present in the srs property
     */
    public boolean isAllowedPropertyValue()
    {
        String srsClassValue = (String) properties.get(UPnPConstants.QN_SRS_CLASS);
        if (!srsClassValue.equalsIgnoreCase(CLASS_PROPERTY))
        {
            if (log.isDebugEnabled())
            {
                log.debug("isAllowedPropertyValue() srs:class contains a value that is not allowed." + srsClassValue);
            }
            return false;
        }
        return true;
    }
    
    /**
     * Tests that all of the fields are valid. See Table C-7 in UPnP
     * ScheduledRecordingService specification
     */
    public boolean isValid()
    {
        // first check all common usage properties
        if (properties.get(UPnPConstants.QN_SRS_TITLE).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:title");
            }

            return false;
        }

        if (properties.get(UPnPConstants.QN_SRS_CLASS).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:class");
            }

            return false;
        }

        if (properties.get(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:scheduledChannelID");
            }

            return false;
        }

        if (properties.get(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:scheduledChannelID@type");
            }

            return false;
        }

        if (properties.get(UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:scheduledStartDateTime");
            }

            return false;
        }

        if (properties.get(UPnPConstants.QN_SRS_SCHEDULED_DURATION).equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("isValid() missing srs:scheduledDuration");
            }

            return false;
        }

        // next check properties unique to usage
        if (!isRequest)
        {
            if (properties.get(UPnPConstants.QN_SRS_ID_ATTR).equals(""))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isValid() missing srs:@id");
                }

                return false;
            }

            if (properties.get(UPnPConstants.QN_SRS_SCHEDULE_STATE).equals(""))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isValid() missing srs:scheduleState");
                }

                return false;
            }

            // From Section B.9.1.2 scheduleState@currentErrors SHALL be empty
            // if scheduleState is OPERATIONAL
            if (properties.get(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR) == null
                    || (!properties.get(UPnPConstants.QN_SRS_SCHEDULE_STATE).equals("OPERATIONAL")
                        && properties.get(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR).equals("")))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isValid() missing srs:scheduleState@currentErrors");
                }

                return false;
            }

            if (properties.get(UPnPConstants.QN_SRS_ABNORMAL_TASKS_EXIST).equals(""))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isValid() missing srs:abnormalTasksExist");
                }

                return false;
            }

            if (properties.get(UPnPConstants.QN_SRS_CURRENT_RECORD_TASK_COUNT).equals(""))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isValid() missing srs:currentRecordTaskCount");
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Creates an xml document for all the property values contained in this
     * instance.
     *
     * @return The xml string for the item values.
     */
    public String toXMLString(String[] filteredProperties)
    {
        return toXMLString(filteredProperties, properties);
    }

    /**
     * Creates an xml document for all the property values contained in this
     * instance.
     *
     * @return The xml string for the item values.
     */
    public static String toXMLString(String[] filteredProperties, Map props)
    {
        StringBuffer xml = new StringBuffer();

        // wrap the properties with <item id=""> property values </item>
        xml.append("<item id=\"" + props.get(UPnPConstants.QN_SRS_ID_ATTR) + "\">");

        // title and class must be first
        appendXML(xml, filteredProperties, props, UPnPConstants.QN_SRS_TITLE);
        appendXML(xml, filteredProperties, props, UPnPConstants.QN_SRS_CLASS);

        for (Iterator iter = props.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();

            QualifiedName propName = (QualifiedName) entry.getKey();
            String propValue = (String) entry.getValue();

            if (UPnPConstants.QN_SRS_TITLE.equals(propName) || UPnPConstants.QN_SRS_CLASS.equals(propName))
            {
                continue;
            }

            appendXML(xml, filteredProperties, props, propName, propValue);
        }

        xml.append("</item>");

        return xml.toString();
    }

    public static String getPropertyStr(QualifiedName propName, boolean appendSRS)
    {
        // TODO: replace this hack by namespace communication between toXMLString
        //       and its callers, as there is between MetadataNodeImpl.toDIDLLite
        //       and its callers.
        String propKey;

        if (UPnPConstants.NSN_SRS.equals(propName.namespaceName()))
        {
            if (appendSRS)
            {
                propKey = "srs:" + propName.localPart();
            }
            else
            {
                propKey = propName.localPart();
            }
        }
        else if (UPnPConstants.NSN_OCAP_METADATA.equals(propName.namespaceName()))
        {
            propKey = UPnPConstants.NSN_OCAP_PREFIX + ":" + propName.localPart();
        }
        else if (UPnPConstants.NSN_DC.equals(propName.namespaceName()))
        {
            propKey = UPnPConstants.NSN_DC_PREFIX + ":" + propName.localPart();
        }
        else if (UPnPConstants.NSN_UPNP.equals(propName.namespaceName()))
        {
            propKey = UPnPConstants.NSN_UPNP_PREFIX + ":" + propName.localPart();
        }
        // OC-SP-HNP2.0-I07-120224 : 6.8.3.6 notifySchedule method :
        // The HNIMP SHALL contain all the "ocap" and "ocapApp" name space
        // properties received with the action to the response.
        else if (UPnPConstants.NSN_OCAPAPP.equals(propName.namespaceName()))
        {
            propKey = UPnPConstants.NSN_OCAPAPP_PREFIX + ":" + propName.localPart();
        }
        else
        {
            if (log.isInfoEnabled()) 
            {
                log.info("Unmatched namespace: " + propName.namespaceName());
            }
            propKey = propName.localPart();
        }

        return propKey;
    }

    /**
     * Generate a comma separated list of properties which may appear
     * in UPnP actions related to RecordSchedule.
     *
     * @return  string containing comma separated values which are
     * property names which may be used in UPnP actions
     */
    public static String getRecordSchedulePropertyCSVStr()
    {
        return buildPropertyCSVStr(ALLOWEDPROPERTYKEYSFORRESPONSE.iterator());
    }

    public static String[] getRecordScheduleProperties()
    {
        return buildPropertiesArray(ALLOWEDPROPERTYKEYSFORRESPONSE.iterator());
    }

    public static String[] getRecordSchedulePropertiesRequired()
    {
        return buildPropertiesArray(REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.iterator());
    }

    public static String getRecordScheduleAllowedFieldsXMLStr(String properties[])
    {
        return buildAllowedValuesXMLStr(properties);
    }

    /**
     * Generate a comma separated list of properties which may appear
     * in UPnP actions related to RecordScheduleParts.
     *
     * @return  string containing comma separated values which are
     * property names which may be used in UPnP actions
     */
    public static String getRecordSchedulePartsPropertyCSVStr()
    {
        return buildPropertyCSVStr(ALLOWEDPROPERTYKEYSFORREQUEST.iterator());
    }

    public static String[] getRecordSchedulePartsProperties()
    {
        return buildPropertiesArray(ALLOWEDPROPERTYKEYSFORREQUEST.iterator());
    }

    public static String getRecordSchedulePartsAllowedFieldsXMLStr(String properties[])
    {
        return buildAllowedValuesXMLStr(properties);
    }

    // //////////////////////////////////////////////////////////////////
    //
    // Private
    //
    // //////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////

    // Log4J logger.
    private static final Logger log = Logger.getLogger(RecordScheduleDirectManual.class);

    private Map properties = new HashMap(); // used to hold property values with
                                            // property static as key

    // used to hold the Set of allowed property keys for
    // RECORD_SCHEDULE_PARTS_USAGE
    private static final Set ALLOWEDPROPERTYKEYSFORREQUEST = new HashSet();

    // used to hold the Set of allowed property keys for RECORD_SCHEDULE_USAGE
    private static final Set ALLOWEDPROPERTYKEYSFORRESPONSE = new HashSet();

    // used to hold the Set of required property keys for RECORD_SCHEDULE_USAGE
    private static final Set REQUIREDPROPERTYKEYSFORRECORDSCHEDULE = new HashSet();

    // used to store the attribute keys associated with the element that they
    // appear in
    // map property key to attribute key, property keys with multiple attributes
    // are stored as
    // a Vector of attribute keys
    private static final Map PROPERTYATTRIBUTEMAP = new HashMap();

    // used to store all of the attribute names
    private static final Set ATTRIBUTENAMES = new HashSet();

    // defines the usage of this as request (true) or response (false)
    private final boolean isRequest;

    /**
     * Append the XML representation of a property to a StringBuffer
     * instance, given its name.
     *
     * @param xml                The StringBuffer.
     * @param filteredProperties The property filter array.
     * @param props              The map of property name/value pairs.
     * @param propName           The QualifiedName of the property.
     */
    private static void appendXML(StringBuffer xml, String[] filteredProperties, Map props, QualifiedName propName)
    {
        appendXML(xml, filteredProperties, props, propName, (String) props.get(propName));
    }

    /**
     * Append the XML representation of a property to a StringBuffer
     * instance, given its name and value.
     *
     * @param xml                The StringBuffer.
     * @param filteredProperties The property filter array.
     * @param props              The map of property name/value pairs.
     * @param propName           The QualifiedName of the property.
     * @param propValue          The String value of the property.
     */
    private static void appendXML(StringBuffer xml, String[] filteredProperties, Map props, QualifiedName propName, String propValue)
    {
        String propStr = getPropertyStr(propName, true);

        // If a filter is applied, see if this property is included
        if (filteredProperties != null)
        {
            // If this property isn't in the supplied list of filtered properties, don't include it
            boolean found = false;

            for (int i = 0; i < filteredProperties.length; i++)
            {
                if (filteredProperties[i].trim().equals(propStr.trim()))
                {
                    found = true;
                    break; // jump out since found this name in list
                }
            }
            if (!found)
            {
                return; // skip this property since it isn't included in filtered list
            }
        }

        // Ignore attribute names. Attributes are handled for each element
        // below.
        if (ATTRIBUTENAMES.contains(propName))
        {
            return;
        }

        String propKey = getPropertyStr(propName, false);

        // fill in the property name element
        xml.append("<" + propKey);

        //
        // See if there is an attribute for this property name element.
        //
        Object obj = PROPERTYATTRIBUTEMAP.get(propName);

        if (obj != null)
        {
            if (obj instanceof QualifiedName)
            {
                QualifiedName attributeName = (QualifiedName) obj;

                xml.append(" " + buildAttributeXML(attributeName, props));
            }
            else if (obj instanceof Vector)
            {
                // multiple attributes for this element
                Enumeration enumeration = ((Vector) obj).elements();

                while (enumeration.hasMoreElements())
                {
                    QualifiedName attributeName = (QualifiedName) enumeration.nextElement();

                    xml.append(" " + buildAttributeXML(attributeName, props));
                }
            }
            // else, can't happen unless a mistake is made in
            // initInstance();
        }

        xml.append(">");

        // get value for property name

        xml.append(Utils.toXMLEscaped(propValue));

        xml.append("</" + propKey + ">");
    }

    // TODO: Document that this is also called in step 14; see JIRA issue
    // OCSPEC-234.
    /**
     * Adjust the properties of the NetRecordingSpec passed to the
     * RecordingNetModule's requestSchedule method to be suitable for the
     * CreateRecordSchedule action, as required in OC-SP-HNP2.0-I02-091217.pdf,
     * section 6.8.4.7:
     *
     * "... the HNIMP SHALL support OCAP properties for purposes of remote
     * recording scheduling in a NetRecordingSpec as specified the left column
     * in the table below. If the ocap:scheduledStartDateTime is included in the
     * NetRecordingSpec parameter, the implementation SHALL copy its value to
     * srs:scheduledStartDateTime property in the CreateRecordSchedule action
     * and SHALL NOT add the ocap:scheduledStartDateTime property. The same rule
     * SHALL be applied to ocap:scheduledChannelID and srs:scheduledChannelID
     * pair, ocap:scheduledChannelID@type and ocap:scheduledChannelIDType pair,
     * and ocap:scheduledDuration and srs:scheduledDuration pair."
     *
     * Table 6-2, Recording Properties, from that reference ("the table below")
     * is this:
     *
     * OCAP Property Use for SRS property if specified
     *
     * ocap:scheduledStartDateTime srs:scheduledStartDateTime
     * ocap:scheduledChannelID srs:scheduledChannelID
     * ocap:scheduledChannelIDType srs:scheduledChannelID@type
     * ocap:scheduledDuration srs:scheduledDuration ocap:priorityFlag
     * ocap:retentionPriority ocap:accessPermissions ocap:organization
     * ocap:appID ocap:msoContentIndicator ocap:expirationPeriod
     */
    private static QualifiedName ocapToSrs(QualifiedName property)
    {
        if (UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME.equals(property))
        {
            return UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME;
        }

        if (UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID.equals(property))
        {
            return UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID;
        }

        if (UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID_TYPE.equals(property))
        {
            return UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR;
        }

        if (UPnPConstants.QN_OCAP_SCHEDULED_DURATION.equals(property))
        {
            return UPnPConstants.QN_SRS_SCHEDULED_DURATION;
        }

        if (UPnPConstants.QN_DC_TITLE.equals(property))
        {
            return UPnPConstants.QN_SRS_TITLE;
        }

        return property;
    }

    /**
     * Adjust the CreateRecordSchedule action properties to be suitable for the
     * NetRecordingEntry passed to the NetRecordingRequestHandler's
     * notifySchedule method, as required in OC-SP-HNP2.0-I02-091217.pdf,
     * section 6.8.3.6:
     *
     * "The NetRecordingEntry parameter SHALL contain the property and value
     * pairs received with the action and defined in Table 6-2. If the action
     * does not contain ocap:scheduledStartDateTime, ocap:scheduledChannelID,
     * ocap:scheduledChannelIDType, and ocap:scheduledDuration, the HNIMP SHALL
     * create the missing properties among them by using SRS properties and
     * their values in the action, and SHALL add the created properties to the
     * NetRecordingEntry. The relationship between these OCAP properties and SRS
     * properties is as represented in Table 6-2. The HNIMP SHALL convert
     * srs:title in the action to dc:title in the NetRecordingEntry parameter.
     * In addition, the HNIMP SHALL add all the property and value pairs
     * received with the action, whose name space is not SRS namespace, to the
     * NetRecordingEntry parameter. Note that the HNIMP SHALL NOT add SRS
     * properties to the NetRecordingEntry parameter."
     *
     * Table 6-2, Recording Properties, from that reference is this:
     *
     * OCAP Property Use for SRS property if specified
     *
     * ocap:scheduledStartDateTime srs:scheduledStartDateTime
     * ocap:scheduledChannelID srs:scheduledChannelID
     * ocap:scheduledChannelIDType srs:scheduledChannelID@type
     * ocap:scheduledDuration srs:scheduledDuration ocap:priorityFlag
     * ocap:retentionPriority ocap:accessPermissions ocap:organization
     * ocap:appID ocap:msoContentIndicator ocap:expirationPeriod
     */
    private static QualifiedName srsToOcap(QualifiedName property)
    {
        if (UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME.equals(property))
        {
            return UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME;
        }

        if (UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID.equals(property))
        {
            return UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID;
        }

        if (UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR.equals(property))
        {
            return UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID_TYPE;
        }

        if (UPnPConstants.QN_SRS_SCHEDULED_DURATION.equals(property))
        {
            return UPnPConstants.QN_OCAP_SCHEDULED_DURATION;
        }

        if (UPnPConstants.QN_SRS_TITLE.equals(property))
        {
            return UPnPConstants.QN_DC_TITLE;
        }

        return property;
    }

    /**
     * Recursively build this instance from an input MetadataNode tree. Called
     * by constructor.
     *
     * NOTE: This method is not really recursive. It relies on there being no nested
     *       MetadataNodes in this MetadataNode.
     *
     * NOTE: This method also relies on there being no multivalued metadata in this
     *       MetadataNode.
     *
     * @param metadataNode
     *            The MetadataNode to build this instance with.
     */
    private void recursiveBuild(MetadataNodeImpl metadataNode)
    {
        // for all names in current MetadataNode
        QualifiedName[] names = metadataNode.getNames();

        for (int i = 0, n = names.length; i < n; ++ i)
        {
            QualifiedName name = names[i];

            // get value assigned to name
            String str = metadataNode.getMetadataAsString(name);

            name = ocapToSrs(name);

            if (isValidPropertyName(name))
            {
                // str pulled out of MetadataNode as escaped XML, un-escape when placing into another MetadataNode
                properties.put(name, Utils.fromXMLEscaped(str));
            }
            else
            {
                // ignore
                if (log.isDebugEnabled())
                {
                    log.debug(thisParticipant + " is not adding <" + name + ", " + str + "> from " + dataOrigin
                    + " to RecordSchedule");
                }
            }
        }
    }

    /**
     * Recursively build this instance from an input DOM Node tree. Called by
     * constructor
     *
     * @param node
     *            The DOM Node to build this instance from.
     */
    private void recursiveBuild(Node node)
    {
        QualifiedName nodeName = node.getName();

        // build up attributes
        if (node.hasAttributes() && ! nodeName.equals(UPnPConstants.QN_SRS_SRS))
        {
            NamedNodeMap attributes = node.getAttributes();

            for (int i = 0, n = attributes.getLength(); i < n; ++ i)
            {
                Node attribute = attributes.item(i);

                if (attribute.getType() != Node.ATTRIBUTE_NODE)
                {
                    throw new IllegalArgumentException("input Node is not of type Node.ATTRIBUTE_NODE");
                }

                // keys for attributes are in the form <node key>'@'<attribute name>
                // except for "item" attribute which is "@id"

                QualifiedName attrName = attribute.getName();

                String propKey;

                if (nodeName.equals(UPnPConstants.QN_SRS_ITEM))
                {
                    propKey = "@" + attrName.localPart();
                }
                else
                {
                    propKey = nodeName.localPart() + "@" + attrName.localPart();
                }

                QualifiedName attributeName = new QualifiedName(nodeName.namespaceName(), propKey);

                Object attributeValue = attribute.getValue();

                // save attribute name and value
                if (isValidPropertyName(attributeName))
                {
                    properties.put(attributeName, attributeValue);
                }
                else
                // ignore
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(thisParticipant + " is not adding <" + attributeName + ", " + attributeValue
                        + "> from " + dataOrigin + " to RecordSchedule");
                    }
                }
            }
        }

        // recurse add the child node tree
        if (node.hasChildNodes())
        {
            NodeList childNodes = node.getChildNodes();

            for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
            {
                Node childNode = childNodes.item(i);

                int nodeType = childNode.getType();

                if (nodeType == Node.TEXT_NODE)
                {
                    // The child key is used to store the text node instead of the
                    // childNode key which would be "#text"

                    Object value = childNode.getValue();

                    // save name and value
                    if (isValidPropertyName(nodeName))
                    {
                        // TODO fix: If there are multiple child nodes, this
                        // will be a problem
                        // since the value at key will be overwritten by the
                        // last child value.
                        // This will not be a problem for a typical
                        // A_ARG_TYPE_RecordScheduleParts xml
                        // except for multi-value properties which will need to
                        // be handled. For
                        // now, keep it simple and flat.

                        properties.put(nodeName, value);
                    }
                    else
                    // ignore
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(thisParticipant + " is not adding <" + nodeName + ", " + value + "> from "
                            + dataOrigin + " to RecordSchedule");
                        }
                    }
                }
                else if (nodeType != Node.ELEMENT_NODE)
                {
                    // attribute nodes should not appear as child nodes in DOM
                    throw new IllegalArgumentException("expected a Node of type Node.ELEMENT_NODE");
                }
                else
                // node type is a Node.ELEMENT_NODE
                {
                    // recurse create new child MetadataNodes and put them in
                    // the map
                    recursiveBuild(childNode);
                }
            }
        }
    }

    /**
     * Initialize this instance. Called from constructors.
     */
    private void initInstance()
    {
        // initialize the static structures for this class, if needed
        /*initStaticStructures();*/

        //
        // Set default property values for this instance
        //
        Iterator iter = (isRequest ? ALLOWEDPROPERTYKEYSFORREQUEST : ALLOWEDPROPERTYKEYSFORRESPONSE).iterator();

        while (iter.hasNext())
        {
            // for now all defaults are empty string
            properties.put(iter.next(), "");
        }
    }

    /**
     * Set the default property values. This will override any values that must
     * be set by the SRS
     */
    private void setDefaults()
    {
        // For UPnP test tool to pass SRS_CLASS must not be populated default
        // OBJECT.RECORDSCHEDULE.DIRECT.MANUAL because if there is a schedule
        // request that contains bad srs class name it will be overwritten by
        // the default and hence RI does not throw a proper error code as per
        // UPnP-av-ScheduledRecording-v2-Service section 2.9.3.1.1.
        String defaultClassProp = MPEEnv.getEnv(DEFAULT_CLASS_PROPERTY);
        if(defaultClassProp!=null && defaultClassProp.trim().equalsIgnoreCase("true"))
        {
            properties.put(UPnPConstants.QN_SRS_CLASS, CLASS_PROPERTY);
        }
        properties.put(UPnPConstants.QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR, "HDD");
        properties.put(UPnPConstants.QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR, "1");
    }

    /**
     * Initializes the static structures for this class
     */
    private static void initStaticStructures()
    {
        //
        // Initialize static allowed properties Sets once for all instances
        //

        // TODO: need to create a set of multi-value properties to be used to
        // build a Vector of values

        // This Set contains all of the allowed property names that can be used
        // to store property values
        // for the RecordScheduleParts usage
        //if (allowedPropertyKeysForRequest == null)
        //{
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_ID_ATTR);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_TITLE);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_CLASS);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_SCHEDULED_DURATION);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_APP_ID);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_EXPIRATION_PERIOD);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_ORGANIZATION);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_PRIORITY_FLAG);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_OCAP_RETENTION_PRIORITY);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_UPNP_CLASS);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_UPNP_SRS_RECORD_SCHEDULE_ID);
        ALLOWEDPROPERTYKEYSFORREQUEST.add(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID);
        // This Set contains all of the allowed property names that can be used
        // to store property values
        // for the RecordSchedule usage
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_ID_ATTR);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_TITLE);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_CLASS);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_PRIORITY);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULED_DURATION);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULE_STATE);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_ABNORMAL_TASKS_EXIST);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_CURRENT_RECORD_TASK_COUNT);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_OCAP_CDS_REFERENCE);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_OCAP_SCHEDULED_CDS_ENTRY_ID);
        ALLOWEDPROPERTYKEYSFORRESPONSE.add(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID);

        // This Set contains all of the required property names that are used
        // to store property values
        // for the RecordSchedule usage
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_ID_ATTR);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_TITLE);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_CLASS);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_PRIORITY);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULED_DURATION);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULE_STATE);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_ABNORMAL_TASKS_EXIST);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_CURRENT_RECORD_TASK_COUNT);
        REQUIREDPROPERTYKEYSFORRECORDSCHEDULE.add(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID);

        // Init the map of all elements that have attributes.
        // Maps the element name to the attribute name or names.
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_ITEM, UPnPConstants.QN_SRS_ID_ATTR);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID, UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_TASK_CHANNEL_ID, UPnPConstants.QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_RECORD_QUALITY, UPnPConstants.QN_SRS_RECORD_QUALITY_TYPE_ATTR);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_SCHEDULE_STATE, UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR);
            // element names with multiple attributes are saved in Vectors.
            Vector victor = new Vector(2);
            victor.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR);
            victor.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_RECORD_DESTINATION, victor);
        // element names with multiple attributes are saved in Vectors.
        victor = new Vector(9);
        victor.add(UPnPConstants.QN_SRS_FATAL_ERROR);
        victor.add(UPnPConstants.QN_SRS_BITS_RECORDED);
        victor.add(UPnPConstants.QN_SRS_BITS_MISSING);
        victor.add(UPnPConstants.QN_SRS_INFO_LIST);
        victor.add(UPnPConstants.QN_SRS_CURRENT_ERRORS);
        victor.add(UPnPConstants.QN_SRS_PENDING_ERRORS);
        victor.add(UPnPConstants.QN_SRS_RECORDING);
        victor.add(UPnPConstants.QN_SRS_PHASE);
        victor.add(UPnPConstants.QN_SRS_ERROR_HISTORY);
        PROPERTYATTRIBUTEMAP.put(UPnPConstants.QN_SRS_TASK_STATE, victor);

        // Init the set of attribute names
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_ID_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_RECORD_QUALITY_TYPE_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_MEDIATYPE_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_RECORD_DESTINATION_PREFERENCE_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_FATAL_ERROR);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_BITS_RECORDED);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_BITS_MISSING);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_INFO_LIST);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_CURRENT_ERRORS);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_PENDING_ERRORS);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_RECORDING);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_PHASE);
        ATTRIBUTENAMES.add(UPnPConstants.QN_SRS_ERROR_HISTORY);
    }

    /**
     * Checks if input property name is in the Set of allowed property name
     * strings
     *
     * @param property
     *            The property name to check for
     * @return true if this property name can be used to get and set values
     *         with, false if not
     */
    private boolean isValidPropertyName(QualifiedName propertyName)
    {
        // check for allowed name space
        if (propertyName.namespaceName().equals(UPnPConstants.NSN_OCAPAPP))
        {
            return true;
        }

        return (isRequest ? ALLOWEDPROPERTYKEYSFORREQUEST : ALLOWEDPROPERTYKEYSFORRESPONSE).contains(propertyName);
    }

    // utility to build an xml string for an attribute name and value
    private static String buildAttributeXML(QualifiedName attributeName, Map props)
    {
        String lp = attributeName.localPart();
        String attrName = lp.substring(lp.indexOf('@') + 1);
        String attrValue = (String) props.get(attributeName);

        return attrName + "=\"" + attrValue + "\"";
    }

    private static String prettyUsage(int usage)
    {
        switch (usage)
        {
            case RECORD_SCHEDULE_PARTS_USAGE:
                return "RECORD_SCHEDULE_PARTS_USAGE";
            case RECORD_SCHEDULE_USAGE:
                return "RECORD_SCHEDULE_USAGE";
            default:
                throw new IllegalArgumentException("unexpected usage: " + usage);
        }
    }

    /**
     * Generate a comma separated list of properties which may appear
     * in UPnP actionw
     *
     * @param itr   supplied iterator of QualifiedName values
     *
     * @return  string containing comma separated values which are
     * property names which may be used in UPnP actions
     */
    public static String buildPropertyCSVStr(Iterator itr)
    {
        StringBuffer sb = new StringBuffer();

        int cnt = 0;
        while (itr.hasNext())
        {
            QualifiedName qn = (QualifiedName)itr.next();
            if (cnt > 0)
            {
                sb.append(",");
            }
            sb.append(getPropertyStr(qn, true));
            cnt++;
        }

        return sb.toString();
    }

    public static String[] buildPropertiesArray(Iterator itr)
    {
        ArrayList list = new ArrayList();
        while (itr.hasNext())
        {
            QualifiedName qn = (QualifiedName)itr.next();
            list.add(getPropertyStr(qn, true));
        }
        String properties[] = new String[list.size()];
        for (int i = 0; i <list.size(); i++)
        {
            properties[i] = (String)list.get(i);
        }

        return properties;
    }

    public static String buildAllowedValuesXMLStr(String propNames[])
    {
        StringBuffer sb = new StringBuffer();

        if (propNames != null)
        {
            for (int i = 0; i < propNames.length; i++)
            {
                sb.append(AVDT_FIELD_BEG_XML);
                sb.append(propNames[i]);

                // *TODO* - currently just using a "standard" allowed value template
                // Need to specify allowed values which are not "standard"
                sb.append(AVDT_FIELD_STD_END_XML);
            }
        }
        return sb.toString();
    }

    private String thisParticipant;

    private String dataOrigin;
 // Changes done for synchronization issue - Start
    /*
     * This block initializes the static final properties during the class
     * loading itself
     */
    static
    {
        initStaticStructures();
    }
    // Changes done for synchronization issue - End
}
