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

package org.cablelabs.impl.ocap.hn.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParser;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NamedNodeMap;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NodeList;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.dvb.application.AppID;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.service.MediaServerManager;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 *
 * Implementation of MetadataNode.
 * <p>
 * Beware of confusing the "CDS default namespace" (standard prefix "didl-lite")
 * with the "OCAP application default name space [sic]" (standard prefix "ocapApp").
 *
 * @see org.ocap.hn.content.MetadataNode
 */
public final class MetadataNodeImpl extends MetadataNode
{
    public static final String SERIALIZABLE = "ocapSerializedObject";

    private static final String OCAP_APPLICATION_DEFAULT_NAMESPACE_PREFIX = "ocapApp";
    private static final String OCAP_APPLICATION_DEFAULT_NAMESPACE_NAME = UPnPConstants.NSN_OCAPAPP;

    private static final String[] STANDARD_NAMESPACES = { UPnPConstants.NSN_DC_PREFIX,
                                                          UPnPConstants.NSN_UPNP_PREFIX,
                                                          UPnPConstants.NSN_DLNA_PREFIX,
                                                          UPnPConstants.NSN_SRS_PREFIX,
                                                          UPnPConstants.NSN_OCAP_PREFIX,
                                                          UPnPConstants.NSN_DIDL_LITE_PREFIX };

    private static final Map registeredProperties = new HashMap();

    private static final Class[] OBJECT_PARAM       = { boolean.class, Object.class };
    private static final Class[] OBJECT_ARRAY_PARAM = { boolean.class, Object[].class };
    private static final Class[] STRING_PARAM       = { boolean.class, String.class };
    private static final Class[] STRING_ARRAY_PARAM = { boolean.class, String[].class };

    private static final int PROPERTY_ADDITION     = 1;
    private static final int PROPERTY_MODIFICATION = 2;
    private static final int PROPERTY_DELETION     = 3;

    /**
     * A Set<MetadataNodeAddMetadataListener> that contains the listeners to
     * notify after any metadata of any <code>MetadataNodeImpl</code> has been
     * added, removed, or modified.
     */
    private static final Set postAddListeners = new HashSet();

    private static final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static final OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);

    // Log4J logger.
    private static final Logger log = Logger.getLogger(MetadataNodeImpl.class);

    // INSTANCE VARIABLES

    private AppID            appID                   = null;
    private ContentEntry     containingContentEntry  = null;
    private Map              efaps                   = new HashMap();
    private String           key                     = null;
    private Map              nameSpaces              = new HashMap();
    private MetadataNodeImpl parent                  = null;
    private Map              properties              = new HashMap();

    /**
     *  Register converters from typed objects to strings and back for properties defined
     *  in HNEXT-I05
     */
    static
    {
        registerProperty(UPnPConstants.QN_DC_CONTRIBUTOR,                 true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_DC_DATE,                        false, DateWrapper.class);
        registerProperty(UPnPConstants.QN_DC_LANGUAGE,                    true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_DC_PUBLISHER,                   true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_DC_RELATION,                    true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_DC_RIGHTS,                      true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_DIDL_LITE_RES,                  true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS,        false, AccessPermissionsWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_APP_ID,                    false, AppIDWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_CONTENT_URI,               false, StringWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_EXPIRATION_PERIOD,         false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_MEDIA_FIRST_TIME,          false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_MEDIA_PRESENTATION_POINT,  false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_MSO_CONTENT_INDICATOR,     false, BooleanWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_NET_RECORDING_ENTRY,       false, StringWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_ORGANIZATION,              false, StringWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_PRIORITY_FLAG,             false, IntegerWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_RETENTION_PRIORITY,        false, IntegerWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID,      false, StringWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_SCHEDULED_DURATION,        false, DateScheduledDurationWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME, false, DateWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_SPACE_REQUIRED,            false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_OCAP_TASK_STATE,                false, IntegerWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_ALBUM_ART_URI,             true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_AUTHOR,                    true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_CHANNEL_NR,                false, ChannelNrWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_DIRECTOR,                  true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_DVD_REGION_CODE,           false, IntegerWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_GENRE,                     true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_LYRICS_URI,                true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_ORIGINAL_TRACK_NUMBER,     false, IntegerWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_PLAYLIST,                  true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_PRODUCER,                  true,  StringWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_SCHEDULED_END_TIME,        false, DateWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_SCHEDULED_START_TIME,      false, DateWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_STORAGE_FREE,              false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_STORAGE_TOTAL,             false, LongWrapper.class);
        registerProperty(UPnPConstants.QN_UPNP_USER_ANNOTATION,           true,  StringWrapper.class);
    }

    /**
     * Constructor
     */
    public MetadataNodeImpl()
    {
        // Add the standardized set of allowed namespaces here
        // From Table 1-3 [UPNP CDS]
        nameSpaces.put(UPnPConstants.NSN_AV_PREFIX,        UPnPConstants.NSN_AV);
        nameSpaces.put(UPnPConstants.NSN_AVS_PREFIX,       UPnPConstants.NSN_AVS);
        nameSpaces.put(UPnPConstants.NSN_AVDT_PREFIX,      UPnPConstants.NSN_AVDT);
        nameSpaces.put(UPnPConstants.NSN_AVT_EVENT_PREFIX, UPnPConstants.NSN_AVT_EVENT);
        nameSpaces.put(UPnPConstants.NSN_CDS_EVENT_PREFIX, UPnPConstants.NSN_CDS_EVENT);
        nameSpaces.put(UPnPConstants.NSN_DC_PREFIX,        UPnPConstants.NSN_DC);
        nameSpaces.put(UPnPConstants.NSN_DIDL_LITE_PREFIX, UPnPConstants.NSN_DIDL_LITE);
        nameSpaces.put(UPnPConstants.NSN_OCAP_PREFIX,      UPnPConstants.NSN_OCAP_METADATA);
        nameSpaces.put(UPnPConstants.NSN_OCAPAPP_PREFIX,   UPnPConstants.NSN_OCAPAPP);
        nameSpaces.put(UPnPConstants.NSN_RCS_EVENT_PREFIX, UPnPConstants.NSN_RCS_EVENT);
        nameSpaces.put(UPnPConstants.NSN_SRS_PREFIX,       UPnPConstants.NSN_SRS);
        nameSpaces.put(UPnPConstants.NSN_SRS_EVENT_PREFIX, UPnPConstants.NSN_SRS_EVENT);
        nameSpaces.put(UPnPConstants.NSN_UPNP_PREFIX,      UPnPConstants.NSN_UPNP);
        nameSpaces.put(UPnPConstants.NSN_DLNA_PREFIX,      UPnPConstants.NSN_DLNA);        
        nameSpaces.put(UPnPConstants.NSN_XSD_PREFIX,       UPnPConstants.NSN_XSD);
        nameSpaces.put(UPnPConstants.NSN_XSI_PREFIX,       UPnPConstants.NSN_XSI);
        nameSpaces.put(UPnPConstants.NSN_XML_PREFIX,       UPnPConstants.NSN_XML);
    }
    
    public MetadataNodeImpl(String restricted, String searchable)
    {
        this();
        
        if (restricted != null)
        {
            addValue(UPnPConstants.QN_DIDL_LITE_RESTRICTED_ATTR, restricted, 
                    new ExtendedFileAccessPermissions(true, false, true, false, true,
                    false, null, null));
        }
        
        if (searchable != null)
        {
            addValue(UPnPConstants.QN_DIDL_LITE_SEARCHABLE_ATTR, searchable, 
                    new ExtendedFileAccessPermissions(true, false, true, false, true,
                    false, null, null));
        }        
    }

    /**
     * Construct a metadata node with top level element attributes
     *
     * @param id
     * @param parentID
     * @param restricted
     * @param searchable
     */
    public MetadataNodeImpl(String id, String parentID, String restricted, String searchable)
    {
        this(restricted, searchable);

        addValue(UPnPConstants.QN_DIDL_LITE_ID_ATTR, id, 
                new ExtendedFileAccessPermissions(true, false, true, false, true, false, null, null));
        addValue(UPnPConstants.QN_DIDL_LITE_PARENT_ID_ATTR, parentID, 
                new ExtendedFileAccessPermissions(true, false, true, false, true, false, null, null));
    }

    /**
     * Constructs an instance based on a DIDL-Lite XML string containing a
     * single element.
     *
     * @param didlLite
     *            The DIDL-Lite XML string.
     */
    public MetadataNodeImpl(String didlLite)
    {
        this(singleElement(didlLite));
    }

    /**
     * Constructs an instance based on a DOM Node tree.
     *
     * @param node
     *            The parent Node to build from.
     */
    public MetadataNodeImpl(Node node)
    {
        this();

        if (node == null || node.getType() != Node.ELEMENT_NODE)
        {
            throw new IllegalArgumentException("input Node is not of type Node.ELEMENT_TYPE");
        }

        // add the attributes of the DOM node as MetadataNode property entries
        addAttributeEntries(node);

        List nodesToProcess = new ArrayList();

        // loop through child elements strip out <desc> blocks, add namespaces
        if (node.hasChildNodes())
        {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; ++ i)
            {
                Node childNode = childNodes.item(i);

                if (UPnPConstants.QN_DIDL_LITE_DESC.equals(childNode.getName()))
                {
                    // Adding <desc> children to list
                    if (childNode.hasChildNodes())
                    {
                        for (int x = 0; x < childNode.getChildNodes().getLength(); x++)
                        {
                            nodesToProcess.add(childNode.getChildNodes().item(x));
                        }
                    }
                }
                else
                {
                    // Adding non-<desc> children to list
                    nodesToProcess.add(childNode);
                }
            }
        }

        for (Iterator i = nodesToProcess.iterator(); i.hasNext(); )
        {
            Node childNode = (Node) i.next();

            if (childNode.getType() == Node.ELEMENT_NODE)
            {
                QualifiedName name = childNode.getName();

                if (name.namespaceName() == null)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("unknown namespace for element " + name.localPart());
                    }
                }

                if (childNode.numChildren() > 1)
                {
                    // Contains more than a text node, recurse.
                    //TODO : see if there is any way to add EFAP to this property.
                    addValue(name, new MetadataNodeImpl(childNode), null);
                }
                else
                {
                    // A simple node with 1 or 0 children.
                    // If this node has a single text node value record it.
                    if (childNode.numChildren() == 0 ||
                            (childNode.numChildren() == 1 && childNode.getFirstChild().getType() == Node.TEXT_NODE))
                    {
                        if (childNode.getNamespacePrefix() != null
                                && this.nameSpaces != null
                                && this.nameSpaces.get(childNode.getNamespacePrefix()) == null)
                        {
                            // Based on UPnP CDS v3 1.4.1, use the documents namespace definition,
                            // it is recommended that they use the standardized ones.
                            this.nameSpaces.put(childNode.getNamespacePrefix(), name.namespaceName());
                        }

                        String value = childNode.numChildren() == 1 ? childNode.getFirstChild().getValue() : "";
                        if (isSerialized(childNode))  // Detect serialized attribute
                        {
                            boolean multivalued = isMultivalued(name);
                            addValue(name, new SerializableWrapper(multivalued, value), null);
                        }
                        else
                        {
                            accumulateMetadata(name, value);
                        }
                    }

                    // If we have attributes record them.
                    if (childNode.getAttributes() != null && childNode.getAttributes().getLength() > 0)
                    {
                        addAttributeEntries(childNode);
                    }
                }
            }
        }
    }

    /**
     * Registers converters with MetadataNode so that typed objects get marshaled in and out of properly
     * formatted strings in DIDL-Lite.
     *
     * @param property
     * @param multivalued
     * @param valueWrapperClass
     */
    private static synchronized void registerProperty(QualifiedName property, boolean multivalued, Class valueWrapperClass)
    {
        if (property != null && valueWrapperClass != null)
        {
            // Check that class implements correct interface.
            if (! ValueWrapper.class.isAssignableFrom(valueWrapperClass))
            {
                throw new IllegalArgumentException("Does not implement " + ValueWrapper.class.getName());
            }

            // Check for required constructors
            try
            {
                valueWrapperClass.getConstructor(OBJECT_PARAM);
                valueWrapperClass.getConstructor(OBJECT_ARRAY_PARAM);
                valueWrapperClass.getConstructor(STRING_PARAM);
                valueWrapperClass.getConstructor(STRING_ARRAY_PARAM);
            }
            catch (SecurityException e)
            {
                throw new IllegalArgumentException("Security does not allow use of constructor");
            }
            catch (NoSuchMethodException e)
            {
                throw new IllegalArgumentException("Registered Value Wrapper missing constructor");
            }

            if (property.localPart().indexOf('@') >= 0)
            {
                throw new IllegalArgumentException("Can't register dependent property");
            }

            registeredProperties.put(property, new PropertyType(multivalued, valueWrapperClass));
        }
    }

    /**
     * Gets the ContentEntry that contains this MetadataNodeImpl, if any.
     * <p>
     *
     * @return The ContentEntry that contains this MetadataNodeImpl, if any;
     *         else null.
     */
    public synchronized ContentEntry getContainingContentEntry()
    {
        return containingContentEntry;
    }

    /**
     * Sets the ContentEntry that contains this MetadataNodeImpl.
     * <p>
     *
     * @param containingContentEntry
     *            The ContentEntry that contains this MetadataNodeImpl.
     */
    public synchronized void setContainingContentEntry(ContentEntry containingContentEntry)
    {
        this.containingContentEntry = containingContentEntry;
    }

    /**
     * Add metadata with a particular key and value.
     *
     * @param key The key.
     * @param value The value.
     */
    public synchronized void addMetadata(String key, Object value)
    {
        ExtendedFileAccessPermissions efap = containingContentEntry != null
                                                ? containingContentEntry.getExtendedFileAccessPermissions()
                                                : null;

        addMetadata(key, value, efap, efap != null);
    }

    /**
     * Add metadata with a particular name and value.
     *
     * @param name The name.
     * @param value The value.
     */
    public synchronized void addMetadata(QualifiedName name, Object value)
    {
        ExtendedFileAccessPermissions efap = containingContentEntry != null
                                                ? containingContentEntry.getExtendedFileAccessPermissions()
                                                : null;

        addMetadata(null, name, value, efap, efap != null);
    }

    /**
     * Add metadata with a particular key, value, and access permissions.
     *
     * @param key The key.
     * @param value The value.
     * @param efap The access permissions.
     */
    public synchronized void addMetadata(String key, Object value, ExtendedFileAccessPermissions efap)
    {
        addMetadata(key, value, efap, true);
    }

    /**
     * Add metadata with a particular key and value, regardless of the
     * access permissions.
     *
     * @param key The key.
     * @param value The value.
     */
    public synchronized void addMetadataRegardless(String key, Object value)
    {
        addMetadata(key, value, null, false);
    }

    /**
     * Add metadata with a particular name and value, regardless of the
     * access permissions.
     *
     * @param name The name.
     * @param value The value.
     */
    public synchronized void addMetadataRegardless(QualifiedName name, Object value)
    {
        addMetadata(null, name, value, null, false);
    }

    /**
     * Implementation of MetadataNode.addNameSpace() This method assumes that
     * the namespace added is unprefixed (does not have ':') versus prefixed.
     */
    public synchronized void addNameSpace(String newNameSpace, String newURI)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addNameSpace() called. NameSpace: " + newNameSpace + " URI: " + newURI);
        }

        // HNP 2.0 6.3.6.3 #1 null parameters are invalid
        if (newNameSpace == null || newURI == null)
        {
            return;
        }

        // HNP 2.0 6.3.6.3 #1 #2 #3 as ocapApp and valid CDS namespaces are already defined.
        if(nameSpaces.get(newNameSpace) != null)
        {
            return;
        }

        // HNP 2.0 6.3.6.3 #4
        for(Iterator i = nameSpaces.entrySet().iterator();i.hasNext();)
        {
            Map.Entry entry = (Map.Entry)i.next();
            if(entry != null && entry.getValue() != null && entry.getValue().equals(newURI))
            {
                return;
            }
        }

        // HNP 2.0 6.3.6.3 #5
        nameSpaces.put(newNameSpace, newURI);
    }

    /**
     * Implementation of MetadataNode.getKey()
     */
    public synchronized String getKey()
    {
        return key;
    }

    /**
     * Get the keys of all contained metadata.
     *
     * @return The keys of all contained metadata.
     */
    public synchronized String[] getKeys()
    {
        String[] keys = new String[this.properties.size()];
        int x = 0;
        for (Iterator i = this.properties.keySet().iterator(); i.hasNext(); )
        {
            QualifiedName property = (QualifiedName) i.next();
            keys[x++] = unprefixed(key(property));
        }
        return keys;
    }

    /**
     * Get the names of all contained metadata.
     *
     * @return The names of all contained metadata.
     */
    public synchronized QualifiedName[] getNames()
    {
    // TODO: RecordScheduleDirectManual uses this: I don't think it should.
    // TODO: ElementParser uses this: but it really just needs the namespaces.
        List names = new ArrayList();

        recursiveGetNames(names, this);

        return (QualifiedName[]) names.toArray(new QualifiedName[names.size()]);
    }

    /**
     * Implementation of MetadataNode.getMetadata(String)
     */
    public synchronized Object getMetadata(String key)
    {
        if (key == null)
        {
            return null;
        }

        int x = key.lastIndexOf('#');

        Object result;

        if (x >= 0)
        {
            Object o = getMetadata(key.substring(0, x));
            result = o instanceof MetadataNodeImpl
                        ? ((MetadataNodeImpl) o).getMetadata(property(prefixed(key.substring(x + 1))))
                        : null;
        }
        else
        {
            result = getMetadata(property(prefixed(key)));
        }

        return result;
    }

    /**
     * Get the metadata named by a qualified name, if allowed by the
     * access permissions.
     *
     * @param name The qualified name.
     *
     * @return The metadata named by the qualified name, if allowed
     *         by the access permissions.
     */
    public Object getMetadata(QualifiedName name)
    {
        ExtendedFileAccessPermissions efap = (ExtendedFileAccessPermissions)efaps.get(name);

        if (! hasReadPermissions(efap))
        {
            throw new SecurityException("Read access required");
        }

        return getMetadataRegardless(name);
    }

    /**
     * Get the metadata named by a qualified name, regardless of the
     * access permissions.
     *
     * @param name The qualified name.
     *
     * @return The metadata named by the qualified name.
     */
    public Object getMetadataRegardless(QualifiedName name)
    {
        ValueWrapper valueWrapper = (ValueWrapper) properties.get(name);

        return valueWrapper != null ? valueWrapper.getSection6362Value() : null;
    }

    /**
     * Get the single-valued metadata named by a qualified name, as a String.
     *
     * @param name The qualified name.
     *
     * @return The single-valued metadata named by the qualified name, as a String.
     */
    public String getMetadataAsString(QualifiedName name)
    {
        ExtendedFileAccessPermissions efap = (ExtendedFileAccessPermissions)efaps.get(name);
        if (hasReadPermissions(efap))
        {
            ValueWrapper valueWrapper = (ValueWrapper) properties.get(name);
            if (valueWrapper != null)
            {
                assert valueWrapper.getLength() == 1;
                // Trim the leading and trailing spaces.
                return valueWrapper.getXMLValue(0).trim();
            }
        }
        else
        {
            throw new SecurityException("Read access required");
        }
        return null;
    }

    /**
     * Implementation of MetadataNode.getMetadata()
     */
    public synchronized Enumeration getMetadata()
    {
        return new MetadataNodeEnumeration();
    }

    /**
     * Implementation of MetadataNode.getParentNode()
     */
    public synchronized MetadataNode getParentNode()
    {
        return parent;
    }

    /**
     * Implementation of MetadataNode.getExtendedFileAccessPermissions()
     */
    public synchronized ExtendedFileAccessPermissions getExtendedFileAccessPermissions(String key)
    {
        if (key == null)
        {
            return null;
        }

        int x = key.lastIndexOf('#');

        if (x >= 0)
        {
            Object o = getMetadata(key.substring(0, x));
            MetadataNode mn = o instanceof MetadataNode ? (MetadataNode) o : null;
            return mn != null ? mn.getExtendedFileAccessPermissions(key.substring(x + 1)) : null;
        }

        return (ExtendedFileAccessPermissions) efaps.get(property(prefixed(key)));
    }

    /**
     * Generate DIDL-Lite element for this entry HNP 2.0 6.3.5.1
     *
     * @param defaultNamespaceName The name of the default namespace declared in the
     *                             containing XML document.
     * @param filterProperties     QualifiedName filter; null implies all.
     * @param embedded             Indicates if this MetadataNode is embedded in another
     *                             MetadataNode or a top level, full CDS entry
     * @param host                 Host inteface to use in URI for res
     * @param port                 Post to use in URI for res
     *
     * @return The body of the element, and the namespace declarations required
     *         in order to interpret it.
     */
    public DIDLLite.Fragment toDIDLLite(String defaultNamespaceName, final List filterProps, boolean embedded, String host)
    {
        Set requiredElements = null;
        Set requiredAttributes = null;
        
        boolean allIp = false;
        boolean wildCard = false;
        
        List filterProperties = filterProps != null ? new ArrayList(filterProps) : null;
        
        // Detect keyword filter properties
        if (filterProperties != null)
        {
            for (Iterator i = filterProperties.iterator(); i.hasNext();)
            {
                String prop = (String)i.next();
                if("ALLIP".equals(prop))
                {
                    allIp = true;
                    i.remove();
                }
                else if("*".equals(prop))
                {
                    wildCard = true;
                    i.remove();
                }
            }
        }
        else
        {
            wildCard = true;
        }
        
        // Determine elements to generate. 
        if (!wildCard)
        {
            requiredElements = new HashSet();
            requiredElements.add(UPnPConstants.TITLE);
            requiredElements.add(UPnPConstants.UPNP_CLASS);

            requiredAttributes = new HashSet();

            for (Iterator i = filterProperties.iterator(); i.hasNext(); )
            {
                String filterProperty = (String) i.next();

                if (filterProperty.length() == 0)
                {
                    continue;
                }
                
                filterProperty = DIDLLite.prefixed(filterProperty);

                int atSignPos = filterProperty.indexOf('@');

                if (atSignPos >= 0)
                {
                    // Add independent property if dependent property is required
                    requiredElements.add(filterProperty.substring(0, atSignPos));
                    requiredAttributes.add(filterProperty);

                    // If requesting a <res> attribute, "protocolInfo" must be included
                    if (filterProperty.startsWith(UPnPConstants.RESOURCE))
                    {
                        requiredAttributes.add(UPnPConstants.RESOURCE_PROTOCOL_INFO);
                    }
                }
                else
                {
                    requiredElements.add(filterProperty);
                    if (filterProperty.equals(UPnPConstants.RESOURCE)) 
                    {
                        requiredAttributes.add(UPnPConstants.RESOURCE_PROTOCOL_INFO);
                    }
                }
            }
        }

        // Collect elements and attributes
        // Build a data structure of map (namespace prefix => map (element name => list (attribute name)))
        // e.g. (didl-lite => (res => (res@protocolInfo, ...), ...), ...)

        String[] keys = getKeys();

        Map namespacePrefixToElementNameToAttributeNameList = null;

        for (int i = 0, n = keys.length; i < n; ++ i)
        {
            String key = keys[i];

            key = prefixed(key);

            QualifiedName property = property(key);

            // Do not include top level attributes
            if (! isTopLevelAttribute(property))
            {
                if (namespacePrefixToElementNameToAttributeNameList == null)
                {
                    namespacePrefixToElementNameToAttributeNameList = new HashMap();
                }

                String namespacePrefix = getPrefix(key);

                Map elementNameToAttributeNameList = (Map) namespacePrefixToElementNameToAttributeNameList.get(namespacePrefix);
                if (elementNameToAttributeNameList == null)
                {
                    elementNameToAttributeNameList = new HashMap();
                    namespacePrefixToElementNameToAttributeNameList.put(namespacePrefix, elementNameToAttributeNameList);
                }

                String elementName = getElementName(key);

                List attributeNameList = (List) elementNameToAttributeNameList.get(elementName);
                if (attributeNameList == null)
                {
                    attributeNameList = new ArrayList();
                    elementNameToAttributeNameList.put(elementName, attributeNameList);
                }

                if (key.indexOf('@') >= 0)
                {
                    String attributeName = key;
                    attributeNameList.add(attributeName);
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        Map namespaces = new HashMap();
        String nodeType = null;

        // Generate top-level element
        if (!embedded)
        {
            if (!(getMetadataRegardless(UPnPConstants.QN_UPNP_CLASS) instanceof String))
            {
                if (log.isWarnEnabled())
                { 
                    log.warn("MetadataNode does not include " + UPnPConstants.UPNP_CLASS + " and was not rendered");
                }
                return new DIDLLite.Fragment("", null);
            }

            nodeType = ((String) getMetadataRegardless(UPnPConstants.QN_UPNP_CLASS)).startsWith("object.container") ? UPnPConstants.QN_DIDL_LITE_CONTAINER.localPart() : "item";

            sb.append("\t<" + nodeType);

            sb.append(" id=\"").append(getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_ID_ATTR)).append("\"");
            sb.append(" parentID=\"").append(getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_PARENT_ID_ATTR)).append("\"");

            // Required, if not available assume unrestricted
            sb.append(" restricted=\"").append(getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RESTRICTED_ATTR, "0")).append("\"");

            // Special container attribute.  Searchable is not required, but if omitted it implies container is not searchable.
            if (getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_SEARCHABLE_ATTR) != null
                    && (wildCard || requiredAttributes.contains(UPnPConstants.SEARCHABLE_ATTR)))
            {
                sb.append(" searchable=\"").append(getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_SEARCHABLE_ATTR)).append("\"");
            }

            // Special container attribute
            if (UPnPConstants.QN_DIDL_LITE_CONTAINER.localPart().equals(nodeType)
                    && (wildCard || requiredAttributes.contains(UPnPConstants.CHILD_COUNT_ATTR)))
            {
                sb.append(" childCount=\"").append(getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_CHILD_COUNT_ATTR, "0")).append("\"");
            }

            sb.append(">\n");
        }

        // Generate child elements

        if (namespacePrefixToElementNameToAttributeNameList != null)
        {
            for (Iterator ni = namespacePrefixToElementNameToAttributeNameList.entrySet().iterator(); ni.hasNext(); )
            {
                int i = 0;
                Map.Entry cne = (Map.Entry) ni.next();

                String namespacePrefix = (String) cne.getKey();
                Map elementNameToAttributeNameList = (Map) cne.getValue();

                boolean applicationNamespace = ! isStandardNamespace(namespacePrefix);

                String namespaceName = (String) nameSpaces.get(namespacePrefix);
                
                StringBuffer descBuffer = null;
                if(applicationNamespace && !embedded)
                {
                    descBuffer = new StringBuffer();
                }

                boolean defaultNamespace = defaultNamespaceName.equals(namespaceName);

                if (! applicationNamespace && ! defaultNamespace)
                {
                    namespaces.put(namespacePrefix, namespaceName);
                }

                for (Iterator ei = elementNameToAttributeNameList.entrySet().iterator(); ei.hasNext(); )
                {
                    Map.Entry cee = (Map.Entry) ei.next();
                    
                    String elementName = (String) cee.getKey();
                    List attributeNameList = (List) cee.getValue();

                    if (wildCard || isRequired(requiredElements, elementName))
                    {
                        QualifiedName qName = property(elementName);

                        ValueWrapper elemValue = (ValueWrapper) properties.get(qName);

                        if (elemValue == null)
                        {
                            // See the javadoc about QualifiedName vs Property on the key(QualifiedName)
                            // method.

                            if (log.isWarnEnabled())
                            {
                                log.warn("can't find property " + property(elementName));
                            }

                            continue;
                        }
                        
                        appendDIDLLite(descBuffer != null ? descBuffer : sb, qName, attributeNameList, elemValue, namespaces,
                                        applicationNamespace, embedded, defaultNamespace,
                                        wildCard, requiredAttributes, host);

                        if (UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME.equals(qName))
                        {
                            appendDIDLLite(descBuffer != null ? descBuffer : sb, UPnPConstants.QN_OCAP_SCHEDULE_START_DATE_TIME, attributeNameList, elemValue, namespaces,
                                            applicationNamespace, embedded, defaultNamespace,
                                            wildCard, requiredAttributes, host);
                        }
                    }
                }

                // If child elements exist after filtering, for application defined namespaces, add the desc block.
                if (applicationNamespace && !embedded && descBuffer.length() > 0)
                {
                    sb.append("\t\t<desc id=\"desc")
                    .append(i++)
                    .append("\" xmlns:")
                    .append(namespacePrefix)
                    .append("=\"")
                    .append(namespaceName)
                    .append("\"");
                    sb.append(" nameSpace=\"").append(namespaceName).append("\">\n");
                    sb.append(descBuffer);
                    sb.append("\t\t</desc>\n");
                }
            }
        }

        if (!embedded)
        {
            sb.append("\t</").append(nodeType).append(">\n");
        }

        return new DIDLLite.Fragment(sb.toString(), namespaces);
    }
    
    /**
     * Gain access to all defined namespaces
     * 
     * @return a copy of a map that contains all defined namespaces
     */
    public Map getNamespaces()
    {
        return new HashMap(nameSpaces);
    }
    
    /**
     * Strip off dependent property prefix if available off attribute.
     * 
     * @param attributeName dependent property
     * @return prefix to dependent property, or null if there is none.
     */
    public String getAttributePrefix(String attributeName)
    {
        int atPos = attributeName.indexOf("@");
        
        // Should be available cause it is a dependent property.
        if(atPos >= 0)
        {
            String attributePart = attributeName.substring(atPos + 1, attributeName.length());
            int colonPos = attributePart.indexOf(":");
            if(colonPos >= 0)
            {
                return attributePart.substring(0, colonPos);
            }
        }

        return null;
    }

    /**
     * Append DIDL-Lite for an element to a <code>StringBuffer</code>.
     *
     * @param sb                   The <code>StringBuffer</code>.
     * @param qName                The <code>QualifiedName</code> of the element.
     * @param attributeNameList    The list of attribute names for the element.
     * @param elemValue            The value of the element.
     * @param namespaces           The namespaces map to add to for any dependent properties with namespaces.
     * @param applicationNamespace True if the element's namespace is not a standard
     *                             namespace, else false.
     * @param embedded             True if this element is in a <code>MetadataNode</code>
     *                             that is contained in another <code>MetadataNode</code>,
     *                             else false.
     * @param defaultNamespace     True if the element's namespace is the default
     *                             namespace for the containing XML document, else false.
     * @param wildCard             Include all
     * @param requiredAttributes   Attributes required to be included.
     * @param host                 Host interface to use in URI for res
     * @param port                 Port to use in URI for res
     */
    private void appendDIDLLite
        (
            StringBuffer sb,
            QualifiedName qName, List attributeNameList, ValueWrapper elemValue, Map namespaces,
            boolean applicationNamespace, boolean embedded, boolean defaultNamespace,
            boolean wildCard, Set requiredAttributes, String host
        )
    {
        String elementName = key(qName);

        List attrValueList = new ArrayList();
        for (Iterator ai = attributeNameList.iterator(); ai.hasNext(); )
        {
            String attributeName = (String) ai.next();
            String attributePrefix = getAttributePrefix(attributeName);
            if(attributePrefix != null && 
                    namespaces != null &&
                    nameSpaces.get(attributePrefix) != null)
            {
                namespaces.put(attributePrefix, nameSpaces.get(attributePrefix));
            }
            
            attrValueList.add((ValueWrapper) properties.get(property(attributeName)));
        }

        assert attrValueList.size() == attributeNameList.size();

        for (int i = 0, n = elemValue.getLength(); i < n; ++ i)
        {
            // Extra indent for <desc> block elements
            if (applicationNamespace && !embedded)
            {
                sb.append("\t");
            }

            // Generate element
            if (!embedded) // Spacing
            {
                sb.append("\t\t");
            }

            String elemName = Utils.toXMLEscaped(defaultNamespace ? getLocalPart(elementName) : elementName);

            sb.append("<").append(elemName);

            // Generate element's attributes
            for (int j = 0, m = attributeNameList.size(); j < m; ++ j)
            {
                String attributeName = (String) attributeNameList.get(j);

                if (wildCard || isRequired(requiredAttributes, attributeName))
                {
                    String attrName = getAttributeName(defaultNamespace ? getLocalPart(attributeName) : attributeName);

                    ValueWrapper attrValue = (ValueWrapper) attrValueList.get(j);

                    assert attrValue.getLength() == n;

                    sb.append(" ").append(Utils.toXMLEscaped(attrName)).append("=\"");

                    String stubValue = attrValue.getXMLValue(i);
                    String realValue = null;
                    if (attrName.equals("protocolInfo") && host != null)
                    {
                        realValue = MediaServer.substitute(stubValue,
                                MediaServer.HOST_PLACEHOLDER, host);
                    }
                    else
                    {
                        realValue = stubValue;
                    }

                    sb.append(realValue).append("\"");
                }
            }

            if (elemValue instanceof SerializableWrapper)
            {
                sb.append(" " + SERIALIZABLE + "=\"1\"");
            }
            sb.append(">");

            // Generate element's value
            String stubValue = elemValue.getXMLValue(i);
            String realValue = null;

            // Determine if we need to overwrite the output.
            // http://host:port/URI/... based on elements res and ocap:contentURI.
            if ((elemName.toLowerCase().equals("res")
                    || elemName.toLowerCase().equals(UPnPConstants.RESOURCE)
                    || elemName.equals("ocap:contentURI"))
                    && host != null)
            {
                realValue = MediaServer.substitute(stubValue,
                        MediaServer.HOST_PORT_PLACEHOLDER, host + ":" + 
                        MediaServerManager.getInstance().getHttpMediaPortNumber());
            }
            else
            {
                realValue = stubValue;
            }

            sb.append(realValue);

            sb.append("</").append(elemName).append(">");
            if (!embedded)
            {
                sb.append("\n");
            }
        }
    }

    /**
     * Adds metadata to this instance, accumulating like keys into arrays.
     */
    private void accumulateMetadata(QualifiedName name, String value)
    {
        ValueWrapper vw = (ValueWrapper) properties.get(name);

        if (vw == null)
        {
            addMetadata(name, value);
        }
        else
        {
            vw.add(value);
        }
    }

    /**
     * Detect if element is a serialized object
     * @param node Node to check
     * @return true if the node has the ocapSerializedObject attribute
     */
    private boolean isSerialized(Node node)
    {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null)
        {
            for (int i = 0, n = attributes.getLength(); i < n; ++ i)
            {
                Node attr = attributes.item(i);
                if (attr != null && attr.getName() != null &&
                   SERIALIZABLE.equals(attr.getName().localPart()) &&
                   "1".equals(attr.getValue()))
                {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Adds metadata to this instance; called by all public addMetadata()
     * methods.
     * @param checkEfap
     *            true=check the efap permissions, false=ignore efap parameter
     *            and allow add without permissions check
     */
    private void addMetadata(String originalKey, Object value, ExtendedFileAccessPermissions efap, boolean checkEfap)
    {
        if (originalKey == null)
        {
            // the spec doesn't define any exception in this case
            return;
        }

        if (originalKey.indexOf('#') >= 0)
        {
            throw new IllegalArgumentException("Input key contains a pound sign. " + originalKey);
        }

        String key = prefixed(originalKey);

        // check that the key has proper namespace
        //
        if (!checkNameSpace(key))
        {
            throw new IllegalArgumentException("Input key does not have a known namespace. " + key);
        }

        QualifiedName property = property(key);

        addMetadata(originalKey, property, value, efap, checkEfap);
    }

    private boolean isAllowedNonArray(Class c)
    {
        return
               c == AppID.class
            || c == ExtendedFileAccessPermissions.class
            || MetadataNode.class.isAssignableFrom(c) && ! Serializable.class.isAssignableFrom(c)
            || isSerializableNonArray(c);
    }

    private boolean isSerializableNonArray(Class c)
    {
        return ! c.isArray() && Serializable.class.isAssignableFrom(c);
    }

    private synchronized void addMetadata(String originalKey, QualifiedName property, Object value, ExtendedFileAccessPermissions efap, boolean checkEfap)
    {
        if (value != null)
        {
            boolean valid;

            int atSignPos = property.localPart().indexOf('@');

            if (atSignPos == 0)
            {
                valid = value instanceof String;
            }
            else if (atSignPos > 0)
            {
                valid = value instanceof String || value instanceof String[];
            }
            else
            {
                Class c = value.getClass();

                valid = isAllowedNonArray(c) || c.isArray() && isAllowedNonArray(c.getComponentType());

                if (valid && (isSerializableNonArray(c) || c.isArray() && isSerializableNonArray(c.getComponentType())))
                {
                    try
                    {
                        ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());

                        try
                        {
                            oos.writeObject(value);
                        }
                        finally
                        {
                            oos.close();
                        }
                    }
                    catch (IOException e)
                    {
                        valid = false;
                    }
                }
            }

            if (! valid)
            {
                throw new IllegalArgumentException("Input value is not of correct class.");
            }
        }

        Object oldValue = getMetadataRegardless(property);

        // check permissions
        //
        if (checkEfap)
        {
            // check for null input efap parm
            //
            if (efap == null)
            {
                throw new IllegalArgumentException("Input ExtendedFileAccessPermissions is null.");
            }

            if (oldValue != null) // the property already exists
            {
                if (! hasWritePermissions((ExtendedFileAccessPermissions) efaps.get(property)))
                {
                    throw new SecurityException(
                            "Existing property permissions don't allow this app to write to MetadataNode " + property);
                }
            }
            else // the property does NOT exist
            {
                if (! canWriteToContainingContentEntry())
                {
                    throw new SecurityException(
                            "Containing ContentEntry does not allow this app to write to MetadataNode " + property);
                }
            }
        }

        if (value instanceof MetadataNodeImpl)
        {
            MetadataNodeImpl mn = (MetadataNodeImpl) value;

            mn.key = originalKey;
            mn.containingContentEntry = containingContentEntry;
            mn.parent = this;
        }

        addValue(property, value, efap);
    }

    /**
     * Checks that the input key contains a namespace that matches the name
     * space set with addNameSpace();
     */
    private boolean checkNameSpace(String key)
    {
        String namespacePrefix = getPrefix(key);

        if (namespacePrefix.length() > 0)
        {
            return nameSpaces.get(namespacePrefix) != null;
        }

        return true;
    }

    private static boolean isMultivalued(QualifiedName independentProperty)
    {
        assert independentProperty.localPart().indexOf('@') < 0;

        PropertyType pType = (PropertyType) registeredProperties.get(independentProperty);

        return pType != null ? pType.multivalued() : false;
    }

    /**
     * Private class to support Enumeration in MetadataNodeImpl
     */
    private final class MetadataNodeEnumeration implements Enumeration
    {
        private Iterator i;

        public MetadataNodeEnumeration()
        {
            i = properties.values().iterator();
        }

        public boolean hasMoreElements()
        {
            return i.hasNext();
        }

        public Object nextElement()
        {
            Object returnValueWrapper = i.next();

            return returnValueWrapper != null ? ((ValueWrapper)returnValueWrapper).getSection6362Value() : null;
        }
    }

    /**
     * Add the attributes of a DOM node as a property to this MetadataNode If
     * there are no attributes, return silently, taking no action.
     */
    private void addAttributeEntries(Node node)
    {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null)
        {
            for (int i = 0, n = attributes.getLength(); i < n; ++ i)
            {
                Node attr = attributes.item(i);

                if (attr.getType() != Node.ATTRIBUTE_NODE)
                {
                    continue;
                }

                // these are dependent properties
                // key is in the form <parent node name>'@'<attribute node name>
                // except container or item attributes which do not start with
                // parent node name

                QualifiedName nodeName = node.getName();
                QualifiedName attrName = attr.getName();
                
                String propKey;

                if (UPnPConstants.QN_DIDL_LITE_CONTAINER.equals(nodeName) || UPnPConstants.QN_DIDL_LITE_ITEM.equals(nodeName))
                {
                    propKey = "@" + attrName.localPart();
                }
                else
                {
                    String attrPart = attr.getNamespacePrefix() != null ? attr.getNamespacePrefix() + ":" + attrName.localPart() : 
                        attrName.localPart();
                    propKey = nodeName.localPart() + "@" + attrPart;
                }

                if (nodeName.namespaceName() == null)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("unknown namespace for attribute " + propKey);
                    }
                }

                QualifiedName propName = new QualifiedName(nodeName.namespaceName(), propKey);

                accumulateMetadata(propName, attr.getValue());
            }
        }
    }

    /**
     * Adds a value to the properties map, wrapped in a Value subclass
     */
    private void addValue(QualifiedName property, Object value, ExtendedFileAccessPermissions efap)
    {
        if (efap == null)
        {
            efaps.remove(property);
        }
        else
        {
            efaps.put(property, efap);
        }

        ValueWrapper oldStoredValue = (ValueWrapper) properties.get(property);

        if (value == null || value instanceof Object[] && ((Object[]) value).length == 0)
        {
            // See if there was metadata that is being deleted.
            if (oldStoredValue != null)
            {
                processPropertyDeletion(property);
                establishInvariant(property, PROPERTY_DELETION, oldStoredValue, null, null);
            }
        }
        else
        {
            // If the value is not a wrapper class already (from parsing DIDLLite)
            // find registered Value subclass to store property's value or else
            // fall back to HNP 2.0-I03 6.3.6.1 & HN EXT-I05

            ValueWrapper newStoredValue = value instanceof ValueWrapper ? (ValueWrapper) value : wrapped(property, value);

            // Check if the metadata has been changed or added.
            if (oldStoredValue == null)
            {
                processPropertyAddition(property, newStoredValue);
                establishInvariant(property, PROPERTY_ADDITION, null, newStoredValue, null);
            }
            else if (! same(oldStoredValue, newStoredValue))
            {
                processPropertyModification(property, newStoredValue);
                establishInvariant(property, PROPERTY_MODIFICATION, oldStoredValue, newStoredValue, null);
            }
        }
    }

    /**
     * Find all dependent properties associated with an independent property.
     *
     * @param independentProperty The qualified name of the independent property.
     *
     * @return A set of qualified names of the associated dependent properties.
     */
    private Set dependentProperties(QualifiedName independentProperty)
    {
        String iNN = independentProperty.namespaceName();
        String iLP = independentProperty.localPart();

        iLP += '@';

        Set result = new HashSet();

        for (Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            QualifiedName property = (QualifiedName) i.next();

            String dNN = property.namespaceName();
            String dLP = property.localPart();

            if (dNN.equals(iNN) && dLP.startsWith(iLP))
            {
                result.add(property);
            }
        }

        return result;
    }

    /**
     * Establish the invariant of section 6.3.6 of the HNP spec, as
     * detailed in clause 4 of section 6.3.6.1.
     *
     * Bracketed clause designators in this method (e.g. [4.1.1]) refer
     * to equivalently designated clauses of section 6.3.6.1.
     *
     * @param property       The qualified name of the property that is
     *                       being changed.
     * @param propertyChange The nature of the change (addition,
     *                       modification, or deletion).
     * @param A              The old value, if a deletion or
     *                       modification; else null.
     * @param B              The new value, if an addition or
     *                       modification; else null.
     * @param exclusion      The qualified name of the dependent property
     *                       to exclude from the processing, if any;
     *                       else null.
     */
    private void establishInvariant(QualifiedName property, int propertyChange,
                                    ValueWrapper A, ValueWrapper B, QualifiedName exclusion)
    {
        Object a = A != null ? A.getSection6361Value() : null;
        Object b = B != null ? B.getSection6361Value() : null;

        boolean independent = property.localPart().indexOf('@') < 0;

        if (independent)
        {
            // [4.1]
            switch (propertyChange)
            {
            case PROPERTY_ADDITION:
                // [4.1.1]
                // do nothing
                break;
            case PROPERTY_MODIFICATION:
                // [4.1.2]
                boolean aIsArray = a instanceof Object[];
                boolean bIsArray = b instanceof Object[];

                for (Iterator i = dependentProperties(property).iterator(); i.hasNext(); )
                {
                    QualifiedName p = (QualifiedName) i.next();

                    if (p.equals(exclusion))
                    {
                        continue;
                    }

                    if (! aIsArray && ! bIsArray)
                    {
                        // [4.1.2.1]
                        // do nothing
                    }
                    else if (aIsArray && bIsArray && ((Object[]) a).length == ((Object[]) b).length)
                    {
                        // [4.1.2.2]
                        // do nothing
                    }
                    else if (! aIsArray && bIsArray)
                    {
                        // [4.1.2.3]
                        int n = ((Object[]) b).length;
                        String v = (String) getSpecValueRegardless(p);
                        String[] w = new String[n];
                        w[0] = v;
                        processPropertyModification(p, new StringWrapper(isMultivalued(property), w));
                    }
                    else if (aIsArray && ! bIsArray)
                    {
                        // [4.1.2.4]
                        String[] w = (String[]) getSpecValueRegardless(p);
                        String v = w[0];
                        if (v == null)
                        {
                            processPropertyDeletion(p);
                        }
                        else
                        {
                            processPropertyModification(p, new StringWrapper(isMultivalued(property), v));
                        }
                    }
                    else if (aIsArray && bIsArray && ((Object[]) a).length < ((Object[]) b).length)
                    {
                        // [4.1.2.5]
                        String[] w = (String[]) getSpecValueRegardless(p);
                        String[] x = new String[((Object[]) b).length];
                        System.arraycopy(w, 0, x, 0, ((Object[]) a).length);
                        processPropertyModification(p, new StringWrapper(isMultivalued(property), x));
                    }
                    else // if (aIsArray && bIsArray && ((Object[]) a).length > ((Object[]) b).length)
                    {
                        // [4.1.2.6]
                        String[] w = (String[]) getSpecValueRegardless(p);
                        String[] x = new String[((Object[]) b).length];
                        System.arraycopy(w, 0, x, 0, ((Object[]) b).length);
                        processPropertyModification(p, new StringWrapper(isMultivalued(property), x));
                    }
                }
                break;
            case PROPERTY_DELETION:
                // [4.1.3]
                for (Iterator i = dependentProperties(property).iterator(); i.hasNext(); )
                {
                    QualifiedName p = (QualifiedName) i.next();

                    if (p.equals(exclusion))
                    {
                        continue;
                    }

                    processPropertyDeletion(p);
                }
                break;
            }
        }
        else
        {
            // [4.2]
            switch (propertyChange)
            {
            case PROPERTY_ADDITION:
                // [4.2.1]
                {
                boolean bIsArray = b instanceof String[];

                QualifiedName p = independentProperty(property);

                if (p == null)
                {
                    break;
                }

                A = (ValueWrapper) properties.get(p);
                a = A != null ? A.getSection6361Value() : null;

                if (a == null)
                {
                    // [4.2.1.1]
                    if (! bIsArray)
                    {
                        // [4.2.1.1.1]
                        processPropertyAddition(p, new StringWrapper(isMultivalued(p), ""));
                    }
                    else // if (bIsArray)
                    {
                        // [4.2.1.1.2]
                        int n = ((String[]) b).length;
                        String[] w = new String[n];
                        w[0] = "";
                        processPropertyAddition(p, new StringWrapper(isMultivalued(p), w));
                    }
                }
                else
                {
                    // [4.2.1.2]
                    boolean aIsArray = a instanceof Object[];

                    if (! aIsArray && ! bIsArray)
                    {
                        // [4.2.1.2.1]
                        // do nothing
                    }
                    else if (aIsArray && bIsArray && ((Object[]) a).length == ((String[]) b).length)
                    {
                        // [4.2.1.2.2]
                        // do nothing
                    }
                    else if (! aIsArray && bIsArray)
                    {
                        // [4.2.1.2.3]
                        int n = ((String[]) b).length;
                        ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                        Object v = (Object) oldVW.getSection6361Value();
                        Object[] w = (Object[]) Array.newInstance(v.getClass(), n);
                        w[0] = v;
                        ValueWrapper newVW = wrapped(p, w);
                        processPropertyModification(p, newVW);
                        establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                    }
                    else if (aIsArray && ! bIsArray)
                    {
                        // [4.2.1.2.4]
                        ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                        Object[] w = (Object[]) oldVW.getSection6361Value();
                        Object v = w[0];
                        if (v == null)
                        {
                            processPropertyDeletion(p);
                            establishInvariant(p, PROPERTY_DELETION, oldVW, null, property);
                        }
                        else
                        {
                            ValueWrapper newVW = wrapped(p, v);
                            processPropertyModification(p, newVW);
                            establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                        }
                    }
                    else if (aIsArray && bIsArray && ((Object[]) a).length < ((String[]) b).length)
                    {
                        // [4.2.1.2.5]
                        ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                        Object[] w = (Object[]) oldVW.getSection6361Value();
                        Object[] x = (Object[]) Array.newInstance(w.getClass().getComponentType(), ((String[]) b).length);
                        System.arraycopy(w, 0, x, 0, ((Object[]) a).length);
                        ValueWrapper newVW = wrapped(p, x);
                        processPropertyModification(p, newVW);
                        establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                    }
                    else // if (aIsArray && bIsArray && ((Object[]) a).length > ((String[]) b).length)
                    {
                        // [4.2.1.2.6]
                        ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                        Object[] w = (Object[]) oldVW.getSection6361Value();
                        Object[] x = (Object[]) Array.newInstance(w.getClass().getComponentType(), ((String[]) b).length);
                        System.arraycopy(w, 0, x, 0, ((String[]) b).length);
                        ValueWrapper newVW = wrapped(p, x);
                        processPropertyModification(p, newVW);
                        establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                    }
                }
                }
                break;
            case PROPERTY_MODIFICATION:
                // [4.2.2]
                {
                boolean aIsArray = a instanceof String[];
                boolean bIsArray = b instanceof String[];

                QualifiedName p = independentProperty(property);

                if (p == null)
                {
                    break;
                }

                if (! aIsArray && ! bIsArray)
                {
                    // [4.2.2.1]
                    // do nothing
                }
                else if (aIsArray && bIsArray && ((String[]) a).length == ((String[]) b).length)
                {
                    // [4.2.2.2]
                    // do nothing
                }
                else if (! aIsArray && bIsArray)
                {
                    // [4.2.2.3]
                    int n = ((String[]) b).length;
                    ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                    Object v = (Object) oldVW.getSection6361Value();
                    Object[] w = (Object[]) Array.newInstance(v.getClass(), n);
                    w[0] = v;
                    ValueWrapper newVW = wrapped(p, w);
                    processPropertyModification(p, newVW);
                    establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                }
                else if (aIsArray && ! bIsArray)
                {
                    // [4.2.2.4]
                    ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                    Object[] w = (Object[]) oldVW.getSection6361Value();
                    Object v = w[0];
                    if (v == null)
                    {
                        processPropertyDeletion(p);
                        establishInvariant(p, PROPERTY_DELETION, oldVW, null, property);
                    }
                    else
                    {
                        ValueWrapper newVW = wrapped(p, v);
                        processPropertyModification(p, newVW);
                        establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                    }
                }
                else if (aIsArray && bIsArray && ((String[]) a).length < ((String[]) b).length)
                {
                    // [4.2.2.5]
                    ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                    Object[] w = (Object[]) oldVW.getSection6361Value();
                    Object[] x = (Object[]) Array.newInstance(w.getClass().getComponentType(), ((String[]) b).length);
                    System.arraycopy(w, 0, x, 0, ((String[]) a).length);
                    ValueWrapper newVW = wrapped(p, x);
                    processPropertyModification(p, newVW);
                    establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                }
                else // if (aIsArray && bIsArray && ((String[]) a).length > ((String[]) b).length)
                {
                    // [4.2.2.6]
                    ValueWrapper oldVW = (ValueWrapper) properties.get(p);
                    Object[] w = (Object[]) oldVW.getSection6361Value();
                    Object[] x = (Object[]) Array.newInstance(w.getClass().getComponentType(), ((String[]) b).length);
                    System.arraycopy(w, 0, x, 0, ((String[]) b).length);
                    ValueWrapper newVW = wrapped(p, x);
                    processPropertyModification(p, newVW);
                    establishInvariant(p, PROPERTY_MODIFICATION, oldVW, newVW, property);
                }
                }
                break;
            case PROPERTY_DELETION:
                // [4.2.3]
                // do nothing
                break;
            }
        }
    }

    /**
     * Indicate whether or not modification of a given property should be
     * reported to this object's containing <code>ContentEntry</code>.
     *
     * @param property The qualified name of the property.
     *
     * @return False if modification of the property should be reported;
     *         else true.
     */
    private boolean ignoreProperty(QualifiedName property)
    {
        return
            UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID.equals(property)
         || UPnPConstants.QN_UPNP_CONTAINER_UPDATE_ID.equals(property);
    }

    /**
     * Find the independent property with which a dependent property is associated,
     * if any.
     *
     * @param dependentProperty The qualified name of the dependent property.
     *
     * @return The qualified name of the independent property with which the
     *         dependent property is associated, if any; else null.
     */
    private static QualifiedName independentProperty(QualifiedName dependentProperty)
    {
        String dNN = dependentProperty.namespaceName();
        String dLP = dependentProperty.localPart();

        String iNN = dNN;
        String iLP = dLP.substring(0, dLP.indexOf('@'));

        return "".equals(iLP) ? null : new QualifiedName(iNN, iLP);
    }

    /**
     * Notify this object's containing <code>ContentEntry</code> that a given property
     * is being modified.
     *
     * @param property The qualified name of the property.
     */
    private void notifyContentEntry(QualifiedName property)
    {
        // If part of a ContentEntry, notify that a modification has taken place,
        // that is subject to Tracking Changes
        if (containingContentEntry instanceof ContentEntryImpl && !ignoreProperty(property))
        {
            ((ContentEntryImpl)containingContentEntry).modifiedMetadata(property);
        }
    }

    /**
     * Process a property addition.
     *
     * @param property The qualified name of the property that is being added.
     * @param vw       The wrapped value of the property.
     */
    private void processPropertyAddition(QualifiedName property, ValueWrapper vw)
    {
        properties.put(property, vw);
        notifyContentEntry(property);
    }

    /**
     * Process a property deletion.
     *
     * @param property The qualified name of the property that is being deleted.
     */
    private void processPropertyDeletion(QualifiedName property)
    {
        properties.remove(property);
        notifyContentEntry(property);
    }

    /**
     * Process a property modification.
     *
     * @param property The qualified name of the property that is being modified.
     * @param vw       The wrapped new value of the property.
     */
    private void processPropertyModification(QualifiedName property, ValueWrapper vw)
    {
        properties.put(property, vw);
        notifyContentEntry(property);
    }

    /**
     * See if two <code>ValueWrapper</code>s wrap the same value (in XML terms).
     *
     * @param a The first <code>ValueWrapper</code>.
     * @param b The second <code>ValueWrapper</code>.
     *
     * @return True if the two <code>ValueWrappers</code> wrap the same value; else false.
     */
    private static boolean same(ValueWrapper a, ValueWrapper b)
    {
        assert a != null && b != null;

        if (a.getLength() != b.getLength())
        {
            return false;
        }

        for (int i = 0, n = a.getLength(); i < n; ++ i)
        {
            String av = a.getXMLValue(i);
            String bv = b.getXMLValue(i);

            if (av != null ? ! av.equals(bv) : bv != null)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Test if the caller has permission to write this instance based on the
     * containing ContentEntry ExtendedFileAccessPermissions.
     */
    public synchronized boolean canWriteToContainingContentEntry()
    {
        if (containingContentEntry == null)
        {
            // there is no containing ContentEntry
            return true;
        }

        // get the contained by ContentEntry efap
        ExtendedFileAccessPermissions contentEntryEfap = containingContentEntry.getExtendedFileAccessPermissions();

        // The spec is not clear on what to do in the case of a null efap for
        // the containing ContentEntry.
        // Assume that no efap == no permissions == SecurityException
        // However, most of the time the ContentEntry will have a null efap so
        // just return true.
        if (contentEntryEfap == null)
        {
            return true;
        }

        return hasWritePermissions(contentEntryEfap);
    }

    /**
     * Test if the caller has permission to write based on input
     * ExtendedFileAccessPermissions
     */
    public boolean hasWritePermissions(ExtendedFileAccessPermissions efap)
    {
        return hasPermission(efap, true);
    }

    /**
     * Test if the caller has permission to read based on input
     * ExtendedFileAccessPermissions
     */
    public boolean hasReadPermissions(ExtendedFileAccessPermissions efap)
    {
        return hasPermission(efap, false);
    }

    /**
     * Test if the caller has permission to read or write based on input
     * ExtendedFileAccessPermissions
     */
    private boolean hasPermission(ExtendedFileAccessPermissions efap, boolean checkForWritePermission)
    {
        AppID caller = (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);

        // If called from system context, allow manipulation.
        if (caller == null)
        {
            return true;
        }

        if (! canBeAccessed(! checkForWritePermission))
        {
            return false;
        }

        // If no efap, allow manipulation, per HNEXT I05 section 6.5.
        if (efap == null)
        {
            efap = new ExtendedFileAccessPermissions(true, true, false, false, false, false, null, null);
        }

        boolean result;

        if (checkForWritePermission)
        {
            result = osm.hasWriteAccess(getAppID(), efap, caller, OcapSecurityManager.FILE_PERMS_ANY);
        }
        else
        {
            result = osm.hasReadAccess(getAppID(), efap, caller, OcapSecurityManager.FILE_PERMS_ANY);
        }

        return result;
    }

    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        return (ExtendedFileAccessPermissions) getMetadataRegardless(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS);
    }

    /**
     * Returns a String value for a key from the metadata, if allowed
     * by the access permissions.
     *
     * @return value or default String
     */
    protected String getMetadata(String key, String dflt)
    {
        Object value = getMetadata(key);

        return value != null ? value.toString() : dflt;
    }

    /**
     * Returns a String value for a key from the metadata, regardless
     * of the access permissions.
     *
     * @return value or default String
     */
    private String getMetadataRegardless(QualifiedName name, String dflt)
    {
        Object value = getMetadataRegardless(name);

        return value != null ? value.toString() : dflt;
    }

    /**
     * Recursively builds list of all names of all contained objects,
     * MetadataNodes, and contained objects in all contained MetatadaNodes.
     */
    private void recursiveGetNames(List names, MetadataNodeImpl node)
    {
        Set properties = node.properties.keySet();

        for (Iterator i = properties.iterator(); i.hasNext(); )
        {
            QualifiedName property = (QualifiedName) i.next();

            names.add(property);
        }

        Enumeration nodeEnum = node.getMetadata();

        while (nodeEnum.hasMoreElements())
        {
            Object obj = nodeEnum.nextElement();
            if (obj instanceof MetadataNodeImpl)
            {
                recursiveGetNames(names, (MetadataNodeImpl) obj);
            }
        }
    }

    /**
     * Return a Node corresponding to the single element contained within a
     * DIDL-Lite XML string.
     *
     * @param didlLite
     *            The DIDL-Lite XML string.
     * @return The Node corresponding to the single element, or null if an error
     *         is encountered.
     */
    private static Node singleElement(String didlLite)
    {
        Node document = MiniDomParser.parse(didlLite);
        NodeList nodeList = document.getChildNodes();
        
        return nodeList != null && nodeList.getLength() == 1 ? nodeList.item(0) : null;
    }

    public AppID getAppID()
    {
        return (AppID)getMetadataRegardless(UPnPConstants.QN_OCAP_APP_ID);
    }

    public void setAppID(AppID appID)
    {
        addMetadataRegardless(UPnPConstants.QN_OCAP_APP_ID, appID);        
    }

    // Support method for toDIDLite
    private static String getElementName(String element)
    {
        int atSignPos = element.indexOf('@');

        return atSignPos >= 0 ? element.substring(0, atSignPos) : element;
    }

    // Support method for toDIDLite
    private static String getAttributeName(String element)
    {
        int atSignPos = element.indexOf('@');

        assert atSignPos >= 0;

        return element.substring(atSignPos + 1);
    }

    // Support method for toDIDLite
    private boolean isRequired(Set requiredKeys, String key)
    {
        int colonPos = key.indexOf(':');

        assert colonPos >= 0 && (key.indexOf('@') < 0 || colonPos < key.indexOf('@'));

        String namespacePrefix = key.substring(0, colonPos);
        String keyName = key.substring(colonPos + 1);

        for (Iterator i = requiredKeys.iterator(); i.hasNext(); )
        {
            String requiredKey = (String) i.next();

            int requiredColonPos = requiredKey.indexOf(':');

            assert requiredColonPos >= 0 && (requiredKey.indexOf('@') < 0 || requiredColonPos < requiredKey.indexOf('@'));

            String requiredNamespacePrefix = requiredKey.substring(0, requiredColonPos);
            String requiredKeyName = requiredKey.substring(requiredColonPos + 1);

            if (requiredKeyName.equals(keyName)
                && nameSpaces.get(requiredNamespacePrefix).equals(nameSpaces.get(namespacePrefix)))
            {
                return true;
            }
            else if (requiredKey.equals(UPnPConstants.DESC_ELEMENT)
                    && !isStandardNamespace(namespacePrefix))
            {
                // Return true if the required Key is didl-lite:desc and
                // the property being compared has namespace "ocapApp"
                return true;
            }
        }

        return false;
    }

    // Support method for toDIDLite, check to see if element name is
    // an application defined element or standard element.
    private boolean isStandardNamespace(String namespacePrefix)
    {
        for (int i = 0, n = STANDARD_NAMESPACES.length; i < n; ++ i)
        {
            if (nameSpaces.get(STANDARD_NAMESPACES[i]).equals(nameSpaces.get(namespacePrefix)))
            {
                return true;
            }
        }

        return false;
    }

    // Support method for toDIDLite, return local part of string.
    // Format : prefix:localpart
    private static String getLocalPart(String element)
    {
        int colonPos = element.indexOf(':');

        assert colonPos < 0 || element.indexOf('@') < 0 || colonPos < element.indexOf('@');

        return colonPos >= 0 ? element.substring(colonPos + 1) : element;
    }

    // Support method for toDIDLite, return prefix of string.
    // Format : prefix:localpart
    private static String getPrefix(String element)
    {
        int colonPos = element.indexOf(':');

        assert colonPos < 0 || element.indexOf('@') < 0 || colonPos < element.indexOf('@');

        return colonPos >= 0 ? element.substring(0, colonPos) : "";
    }

    /**
     * OcapApp-prefix an unprefixed key.
     *
     * @param key The possibly unprefixed key.
     *
     * @return The key ocapApp-prefixed, if it was unprefixed; else the key.
     */
    private static String prefixed(String key)
    {
        int atSignPos = key.indexOf('@');
        String element = atSignPos >= 0 ? key.substring(0, atSignPos) : key;

        if (element.indexOf(':') >= 0)
        {
            return key;
        }

        return OCAP_APPLICATION_DEFAULT_NAMESPACE_PREFIX + ":" + key;
    }

    /**
     *
     *
     * @param key The possibly ocapApp-prefixed key.
     *
     * @return The key unprefixed, if it was ocapApp-prefixed; else the key.
     */
    private String unprefixed(String key)
    {
        int atSignPos = key.indexOf('@');
        String element = atSignPos >= 0 ? key.substring(0, atSignPos) : key;

        int colonPos = element.indexOf(':');

        if (colonPos < 0 || ! OCAP_APPLICATION_DEFAULT_NAMESPACE_NAME.equals(nameSpaces.get(element.substring(0, colonPos))))
        {
            return key;
        }

        return key.substring(colonPos + 1);
    }

    /**
     * Is a given qualified name a top-level attribute in the CDS default namespace?
     *
     * @param name The qualified name; must not be null.
     *
     * @return True if the qualified name is a top-level attribute; else false.
     */
    private boolean isTopLevelAttribute(QualifiedName name)
    {
        assert name != null;

        String namespaceName = name.namespaceName();
        String localPart = name.localPart();

        return
            namespaceName != null && localPart != null
              && namespaceName.equals(UPnPConstants.NSN_DIDL_LITE) && localPart.startsWith("@");
    }

    /**
     * Compute a key (namespacePrefix:localPart) that represents a property.
     * Any namespace prefix declared to represent the namespace name will do.
     *
     * <p>
     * If none does, we log a warning and return an incorrect result.
     * (Lacking a prefix, it will be interpreted as having an implicit
     * ocapApp prefix.) The likeliest cause of this is a missing or incorrect
     * namespace declaration in input DIDL-Lite. The long-term solution is
     * to switch the RI from using the QualifiedName class to using the new
     * Property class, in which case this method can go away altogether.
     * See also the "can't find property" warning logged in the toDIDLLite method.
     *
     * @param property The property.
     *
     * @return A key that represents the property.
     */
    private String key(QualifiedName property)
    {
        String namespaceName = property.namespaceName();

        if (namespaceName != null)
        {
            for (Iterator i = nameSpaces.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry me = (Map.Entry) i.next();

                String nsn = (String) me.getValue();

                if (namespaceName.equals(nsn))
                {
                    String namespacePrefix = (String) me.getKey();

                    return namespacePrefix + ":" + property.localPart();
                }
            }
        }

        if (log.isWarnEnabled())
        {
            log.warn("can't find namespace prefix for " + property);
        }

        return property.localPart();
    }

    /**
     * Compute a property from a key.
     *
     * @param key The key.
     *
     * @return The property.
     */
    private QualifiedName property(String key)
    {
        int colonPos = key.indexOf(':');

        assert colonPos >= 0 && (key.indexOf('@') < 0 || colonPos < key.indexOf('@'));

        String prefix = key.substring(0, colonPos);
        String localPart = key.substring(colonPos + 1);

        QualifiedName result = new QualifiedName((String) nameSpaces.get(prefix), localPart);

        if (UPnPConstants.QN_OCAP_SCHEDULE_START_DATE_TIME.equals(result))
        {
            result = UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME;
        }

        return result;
    }

    /**
     * Wrap the value of a given property in a <code>ValueWrapper</code>.
     *
     * @param property The qualified name of the property.
     * @param value    The value of the property.
     *
     * @return A <code>ValueWrapper</code> wrapping the value of the property.
     */
    public static final ValueWrapper wrapped(QualifiedName property, Object value)
    {
        assert value != null;

        boolean dependent = property.localPart().indexOf('@') >= 0;
        QualifiedName independentProperty = dependent ? independentProperty(property) : property;

        boolean multivalued = independentProperty != null ? isMultivalued(independentProperty) : false;

        Class valueWrapperClass;

        if (dependent)
        {
            valueWrapperClass = StringWrapper.class;
        }
        else
        {
            PropertyType pType = (PropertyType) registeredProperties.get(independentProperty);

            if (pType != null)
            {
                valueWrapperClass = pType.valueWrapperClass();
            }
            else
            {
                Class c = value.getClass();
                Class d = c.getComponentType();
                if (d != null)
                {
                    c = d;
                }

                if (c == String.class)
                {
                    valueWrapperClass = StringWrapper.class;
                }
                else if (c == Integer.class)
                {
                    valueWrapperClass = IntegerWrapper.class;
                }
                else if (c == Long.class)
                {
                    valueWrapperClass = LongWrapper.class;
                }
                else if (c == Boolean.class)
                {
                    valueWrapperClass = BooleanWrapper.class;
                }
                else if (c == Date.class)
                {
                    valueWrapperClass = DateWrapper.class;
                }
                else if (MetadataNode.class.isAssignableFrom(c))
                {
                    valueWrapperClass = MetadataNodeWrapper.class;
                }
                else if (c == AppID.class)
                {
                    valueWrapperClass = AppIDWrapper.class;
                }
                else if (c == ExtendedFileAccessPermissions.class)
                {
                    valueWrapperClass = AccessPermissionsWrapper.class;
                }
                else if (! c.isArray() && Serializable.class.isAssignableFrom(c))
                {
                    valueWrapperClass = SerializableWrapper.class;
                }
                else
                {
                    valueWrapperClass = null;
                }
            }
        }

        assert valueWrapperClass != null;

        try
        {
            Class[] constructorParam =    value instanceof String[] ? STRING_ARRAY_PARAM
                                        : value instanceof String   ? STRING_PARAM
                                        : value instanceof Object[] ? OBJECT_ARRAY_PARAM
                                        :                             OBJECT_PARAM;
            Constructor c = valueWrapperClass.getConstructor(constructorParam);

            return (ValueWrapper) c.newInstance(new Object[] {Boolean.valueOf(multivalued), value});
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Unable to construct ValueWrapper for key '" + property + "', value = '" + value + "'", e);
            }
            throw new IllegalArgumentException();
        }
    }

    public Object getSpecValueRegardless(String key)
    {
        if (key == null)
        {
            return null;
        }

        int x = key.lastIndexOf('#');

        Object result;

        if (x >= 0)
        {
            Object o = getSpecValueRegardless(key.substring(0, x));
            result = o instanceof MetadataNodeImpl
                        ? ((MetadataNodeImpl) o).getSpecValueRegardless(property(prefixed(key.substring(x + 1))))
                        : null;
        }
        else
        {
            result = getSpecValueRegardless(property(prefixed(key)));
        }

        return result;
    }

    private Object getSpecValueRegardless(QualifiedName name)
    {
        ValueWrapper valueWrapper = (ValueWrapper) properties.get(name);

        if (valueWrapper == null)
        {
            return null;
        }

        Object o = valueWrapper.getSection6361Value();

        return o instanceof Object[] && ((Object[]) o).length == 1 ? ((Object[]) o)[0] : o;
    }

    /**
     * Returns an indication of whether or not properties
     * associated with this object can be accessed, according to the
     * requirements of HNP 2.0 section C.1.1.8.
     *
     * @param forRead True if the access check is for read access; false if
     *                if it is for write access.
     *
     * @return True if they can; false if they cannot.
     */
    private boolean canBeAccessed(boolean forRead)
    {
        return ! (containingContentEntry instanceof ContentEntryImpl)
                || ((ContentEntryImpl) containingContentEntry).canBeAccessed(forRead);
    }

    /**
     * A class indicating the type of a property: whether it is allowed to be
     * multivalued, and what its wrapper class is.
     */
    private static final class PropertyType
    {
        private final boolean multivalued;
        private final Class valueWrapperClass;

        /**
         * Construct a <code>PropertyType</code>.
         *
         * @param multivalued       True if the property is allowed to be
         *                          multivalued; else false.
         * @param valueWrapperClass The <code>ValueWrapper</code> class
         *                          of the property.
         */
        public PropertyType(boolean multivalued, Class valueWrapperClass)
        {
            this.multivalued = multivalued;
            this.valueWrapperClass = valueWrapperClass;
        }

        /**
         * See if the property is allowed to be multivalued.
         *
         * @return True if the property is allowed to be multivalued; else
         *         false.
         */
        public boolean multivalued()
        {
            return multivalued;
        }

        /**
         * Return the <code>ValueWrapper</code> class of the property.
         *
         * @return The <code>ValueWrapper</code> class of the property.
         */
        public Class valueWrapperClass()
        {
            return valueWrapperClass;
        }
    }
}
