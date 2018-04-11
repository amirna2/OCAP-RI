/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006,
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.upnp.cds;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.navigation.ContentList;

/**
 * Formats response data for the Content Directory Service using the DIDL-Lite
 * specification.
 *
 * @author Michael A. Jastad
 * @version $Revision$
 *
 * @see
 */
public final class DIDLLite
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(DIDLLite.class);
    private static final String OMIT_TAG_NEWLINES_PROP = "OCAP.hn.server.didllite.omit.tag.newlines";
    private static final Map NAME_TO_XSD = nameToXsd();

    /**
     * This value will be used to dynamically set the delimiter between items encoded
     * within a tag.  As newline characters are characterized as whitespace, all parsers
     * should ignore them as a space character.  This has not been shown to be true in all
     * cases such as in the Microsoft stack being used in Windows Media Player.
     */
    private static char xmlTagDelimiter = '\n';
    static
    {
        // Setting the xmlTagDelimiter to a space if a newline is not desired
        if ("true".equals(MPEEnv.getEnv(OMIT_TAG_NEWLINES_PROP, "false")))
        {
            xmlTagDelimiter = ' ';
        }
        if (log.isDebugEnabled())
        {
            log.debug("DIDLLite() - xmlTagDelimiter set to: [" + xmlTagDelimiter + ']');
        }
    }

    /**
     * Creates a new DIDLLite object.
     */
    private DIDLLite()
    {
    }

    public static String getView(ContentEntry entry)
    {
        return getView(entry, null, null);
    }

    public static String getView(ContentEntry entry, Vector filterProperties, String host)
    {
        ContentListImpl cli = new ContentListImpl();
        cli.add(entry);
        return getView(cli, filterProperties, host);
    }

    public static String getView(ContentList list)
    {
        return getView(list, null, null);
    }

    public static String getView(ContentList list, Vector filterProperties, String host)
    {
        ContentListImpl cli = (ContentListImpl) list;

        StringBuffer body = new StringBuffer();

        Map namespaces = new TreeMap();

        for (int i = 0, n = cli.size(); i < n; ++ i)
        {
            ContentEntry entry = (ContentEntry) cli.get(i);

            MetadataNodeImpl metadataNode = (MetadataNodeImpl) entry.getRootMetadataNode();

            if (metadataNode != null)
            {
                Fragment f = metadataNode.toDIDLLite(UPnPConstants.NSN_DIDL_LITE, filterProperties, false, host);

                body.append(f.body());
                namespaces.putAll(f.namespaces());
            }
        }

        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
          .append("<DIDL-Lite")
          .append(xmlTagDelimiter)
          .append("xmlns=\"")
          .append(UPnPConstants.NSN_DIDL_LITE)
          .append("\"")
          .append(xmlTagDelimiter);

        Set entrySet = namespaces.entrySet();

        for (Iterator i = entrySet.iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();

            String prefix = (String) entry.getKey();
            String name = (String) entry.getValue();

            sb.append(" xmlns:")
              .append(prefix)
              .append("=\"")
              .append(name)
              .append("\"")
              .append(xmlTagDelimiter);
        }

        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
          .append(xmlTagDelimiter)
          .append("xsi:schemaLocation=")
          .append("\"")
          .append(UPnPConstants.NSN_DIDL_LITE)
          .append(" ")
          .append("http://www.upnp.org/schemas/av/didl-lite.xsd");

        for (Iterator i = entrySet.iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();

            String name = (String) entry.getValue();
            String xsd = (String) NAME_TO_XSD.get(name);

            if (xsd != null)
            {
                  sb.append(xmlTagDelimiter)
                  .append(name)
                  .append(" ")
                  .append(xsd);
            }
        }

        sb.append("\"")
          .append(xmlTagDelimiter)
          .append(">\n")
          .append(body)
          .append("</DIDL-Lite>\n");

        return sb.toString();
    }

    /**
     * Prefix an unprefixed key with the CDS default namespace prefix.
     *
     * @param key The possibly unprefixed key.
     *
     * @return The key prefixed, if it was unprefixed.
     */
    public static String prefixed(String key)
    {
        int atSignPos = key.indexOf('@');
        String element = atSignPos >= 0 ? key.substring(0, atSignPos) : key;

        return element.indexOf(':') >= 0 ? key : UPnPConstants.NSN_DIDL_LITE_PREFIX + ":" + key;
    }

    /**
     * Return a mapping of namespace name to location URI (the "hint as to the location
     * of a schema document defining names for that namespace name").
     *
     * @return The mapping.
     */
    private static final Map nameToXsd()
    {
        Map result = new HashMap();

        result.put(UPnPConstants.NSN_DIDL_LITE, "http://www.upnp.org/schemas/av/didl-lite.xsd");
        result.put(UPnPConstants.NSN_UPNP,      "http://www.upnp.org/schemas/av/upnp.xsd");

        return result;
    }

    /**
     * An XML fragment, consisting of a body string and a map of namespaces
     * used within the body string.
     */
    public static final class Fragment
    {
        /**
         * The body string.
         */
        private final String body;

        /**
         * The namespace map.
         */
        private final Map namespaces;

        /**
         * Construct a fragment from a body string and a namespace map.
         *
         * @param body The body string.
         * @param namespaces The namespace map.
         */
        public Fragment(String body, Map namespaces)
        {
            this.body = body;
            //findbugs detected. If a null Map is passed to the HashMap constructor an NPE results.
            this.namespaces = namespaces != null? new HashMap(namespaces) : new HashMap();
        }

        /**
         * Return the body string.
         *
         * @return The body string.
         */
        public String body()
        {
            return body;
        }

        /**
         * Return the namespace map.
         *
         * @return The namespace map.
         */
        public Map namespaces()
        {
            return new HashMap(namespaces);
        }
    }
}
