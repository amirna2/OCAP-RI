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

package org.havi.ui;

import java.awt.*;
import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.*;
import org.havi.ui.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Class that reads an XML document (e.g., dispconfig.xml) that describes the
 * supported screens, devices, and configurations for a platform.
 * <p>
 * The XML document is expected to follow the DTD (dispconfig.dtd).
 * <p>
 * The DTD is duplicated here for informational purposes:
 * 
 * <pre>
 * <!ELEMENT root (screens)>
 * 
 * <!ELEMENT screens (screen+)>
 * <!ATTLIST screens
 *     default IDREF #REQUIRED
 * >
 * 
 * <!ELEMENT screen (device+, coherent+)>
 * <!ATTLIST screen
 *     id ID #REQUIRED
 *     default IDREFS #REQUIRED
 * >
 * 
 * <!ELEMENT device (config+)>
 * <!ATTLIST device
 *     type (graphics | video | background) #REQUIRED
 *     id ID #REQUIRED
 *     default IDREF #IMPLIED
 * >
 * 
 * <!ELEMENT config (resolution, area, pixelRatio)>
 * <!ATTLIST config
 *     id ID #REQUIRED
 *     interlaced (true | false) #REQUIRED
 *     flicker (true | false) #REQUIRED
 *     still (true | false) "false"
 *     color (true | false) "false"
 *     vidmix (true | false) "true"
 *     gfxmix (true | false) "true"
 *     matte (true | false) "false"
 *     scaling (true | false) "false" 
 * >
 * 
 * <!ELEMENT resolution EMPTY>
 * <!ATTLIST resolution
 *     width NMTOKEN #REQUIRED
 *     height NMTOKEN #REQUIRED>
 * <!ELEMENT area EMPTY>
 * <!ATTLIST area
 *     x CDATA #REQUIRED
 *     y CDATA #REQUIRED
 *     width CDATA #REQUIRED
 *     height CDATA #REQUIRED>
 * <!ELEMENT pixelRatio EMPTY>
 * <!ATTLIST pixelRatio
 *     width NMTOKEN #REQUIRED
 *     height NMTOKEN #REQUIRED>
 * 
 * <!ELEMENT coherent EMPTY>
 * <!ATTLIST coherent
 *     refid IDREFS #REQUIRED>
 * </pre>
 * 
 * @note This is meant to replace the XMLTestConfig class for the purposes of
 *       testing the screens/devices/configurations. The other was just too
 *       difficult to use (because it was capable of a lot more than necessary).
 * 
 * @author Aaron Kamienski
 */
public class XMLDispConfig extends Assert
{
    private Document doc;

    /**
     * Creates an <code>XMLDispConfig</code> based upon an XML document found
     * using <code>getClass().getResource()</code>. The name of the resource is
     * discovered by calling <code>MPEEnv.getSystemProperty("dispconfig")</code>
     * . If no such property exists, then it will fall back to a default (which
     * is not likely the desired result).
     */
    public XMLDispConfig()
    {
        String path = MPEEnv.getSystemProperty("dispconfig", "/org/havi/ui/port/mpe/clientsim/dispconfig.xml");
        iniz(getClass().getResource(path).toString());
    }

    /**
     * Create an <code>XMLDispConfig</code> based upon the given <i>URI</i>. The
     * URI can be generated from a <code>URL</code> using
     * <code>toString()</code>.
     * 
     * @param uri
     *            xml document universal resource identifier
     */
    public XMLDispConfig(String uri)
    {
        iniz(uri);
    }

    /**
     * Initializes this instance using the given <i>URI</i>.
     * 
     * @param uri
     *            location of xml document
     */
    private void iniz(String uri)
    {
        // How should we locate the config file?????
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();

            parser.setErrorHandler(new org.xml.sax.ErrorHandler()
            {
                public void error(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
                {
                    fail(arg1.getMessage());
                }

                public void fatalError(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
                {
                    fail(arg1.getMessage());
                }

                void store(SAXParseException ex, String type)
                {
                }

                public void warning(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
                {
                }
            });
            doc = parser.parse(new InputSource(new StringReader(uri)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("XML parser failure for xml file " + uri);
        }

        assertNotNull("Failed to create document from xml file " + uri, doc);
    }

    /**
     * Returns immediate sub-elements of <i>root</i> with the given tagname.
     * 
     * @param root
     *            root element
     * @param tagName
     *            tag name of desired sub-element(s)
     * @return immediate sub-elements of <i>root</i> with the given tagname.
     */
    private Element[] getElements(Element root, String tagName)
    {
        return getElements(root, tagName, null, null);
    }

    /**
     * Returns immediate sub-elements of <i>root</i> with the given tagname and
     * id (if specified) and type (if specified).
     * 
     * @param root
     *            root element
     * @param tagName
     *            tag name of desired sub-element(s)
     * @param id
     *            desired value of "id" attribute (or null if don't care)
     * @param type
     *            desired value of "type" attribute (or null if don't care)
     * @return immediate sub-elements of <i>root</i> with the given tagname and
     *         id (if specified) and type (if specified).
     */
    private Element[] getElements(Element root, String tagName, String id, String type)
    {
        NodeList list = root.getElementsByTagName(tagName);
        Vector v = new Vector();

        for (int i = 0; i < list.getLength(); ++i)
        {
            Element e = (Element) list.item(i);
            if (id != null)
            {
                if (!id.equals(e.getAttribute("id")) && (type == null || !type.equals(e.getAttribute("type"))))
                    continue;
            }
            else if (type != null && !type.equals(e.getAttribute("type")))
            {
                continue;
            }
            v.addElement((Element) list.item(i));
        }

        Element[] elements = new Element[v.size()];
        v.copyInto(elements);

        return elements;
    }

    /**
     * Locates a single element (i.e., the first) with the given tagname and id
     * (if specified) and type (if specified).
     * 
     * @param root
     *            root element
     * @param tagName
     *            tag name of desired sub-element(s)
     * @param id
     *            desired value of "id" attribute (or null if don't care)
     * @param type
     *            desired value of "type" attribute (or null if don't care)
     * @return immediate sub-element of <i>root</i> with the given tagname and
     *         id (if specified) and type (if specified).
     */
    private Element findElement(Element root, String tagName, String id, String type)
    {
        NodeList list = root.getElementsByTagName(tagName);

        if (id == null && type == null) return (list.getLength() > 0) ? (Element) list.item(0) : null;

        for (int i = 0; i < list.getLength(); ++i)
        {
            Element e = (Element) list.item(i);
            if (id != null)
            {
                if (id.equals(e.getAttribute("id")) && (type == null || type.equals(e.getAttribute("type")))) return e;
            }
            else if (type != null && type.equals(e.getAttribute("type")))
            {
                return e;
            }
        }
        return null;
    }

    /**
     * Returns whether a non-contributing video configuration is implied when
     * background still support is enabled.
     */
    public boolean hasNotContribVideo()
    {
        return null != getNotContribVideo();
    }

    /**
     * Returns the non-contributing video configuration, or null if there is
     * none.
     */
    public Element getNotContribVideo()
    {
        Element notcontrib = findElement(doc.getDocumentElement(), "notcontrib", null, null);
        if (notcontrib != null)
        {
            notcontrib = findElement(notcontrib, "config", null, null);
        }
        return notcontrib;
    }

    /**
     * Returns the list of devices that use the not-contributing video
     * configuration.
     * 
     * @return space-delimited string of device ids; <code>null</code> if there
     *         are none
     */
    public String getNotContribVideoUses()
    {
        Element notcontrib = findElement(doc.getDocumentElement(), "notcontrib", null, null);
        if (notcontrib == null) return null;
        return notcontrib.getAttribute("uses");
    }

    /**
     * Returns <code>true</code> if the given device uses the con-contributing
     * video configuration. That is:
     * <ul>
     * <li><i>device</i> has type attribute of "video"
     * <li><i>device</i> has a valid id
     * <li><i>device</i>'s id is found in the "uses" attribute of the
     * <notcontrib> configuration
     * </ul>
     * 
     * @return <code>true</code> if <i>device</i> uses the con-contributing
     *         video configuration
     */
    public boolean doesDeviceUseNotContrib(Element device)
    {
        System.out.println("doesDeviceUseNotContrib(" + device + ")...");

        if (!"video".equals(device.getAttribute("type"))) return false;
        String id = device.getAttribute("id");
        System.out.println(" device = " + id);
        if (id == null) return false;

        return doesDeviceUseNotContrib(id);
    }

    /**
     * Returns <code>true</code> if the given device uses the con-contributing
     * video configuration. That is:
     * <ul>
     * <li><i>id</i> is found in the "uses" attribute of the <notcontrib>
     * configuration
     * </ul>
     * 
     * @return <code>true</code> if <i>device</i> uses the con-contributing
     *         video configuration
     */
    public boolean doesDeviceUseNotContrib(String id)
    {
        String ids = getNotContribVideoUses();
        System.out.println(" nonContribs = " + ids);
        if (ids == null) return false;

        StringTokenizer tok = new StringTokenizer(ids, " ");
        while (tok.hasMoreTokens())
        {
            if (id.equals(tok.nextToken()))
            {
                System.out.println("return true");
                return true;
            }
        }
        System.out.println("return false");
        return false;
    }

    /**
     * Returns an array of Elements representing the expected screens.
     * 
     * @return an array of Elements representing the expected screens.
     */
    public Element[] getScreens()
    {
        return getElements(doc.getDocumentElement(), "screen");
    }

    /**
     * Returns an array of Elements representing the expected devices.
     * 
     * @param screen
     *            Element representing the screen
     * @return an array of Elements representing the expected devices.
     */
    public Element[] getDevices(Element screen)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());
        return getElements(screen, "device");
    }

    /**
     * Returns an array of Elements representing the expected devices of the
     * desired type.
     * 
     * @param screen
     *            Element representing the screen
     * @param type
     *            the desired type ("graphics", "video", or "bg")
     * @return an array of Elements representing the expected devices.
     */
    public Element[] getDevices(Element screen, String type)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());
        return getElements(screen, "device", null, type);
    }

    /**
     * Returns an array of Elements representing the expected graphics devices.
     * 
     * @param screen
     *            Element representing the screen
     * @return an array of Elements representing the expected devices.
     */
    public Element[] getGraphicsDevices(Element screen)
    {
        return getDevices(screen, "graphics");
    }

    /**
     * Returns an array of Elements representing the expected video devices.
     * 
     * @param screen
     *            Element representing the screen
     * @return an array of Elements representing the expected devices.
     */
    public Element[] getVideoDevices(Element screen)
    {
        return getDevices(screen, "video");
    }

    /**
     * Returns an array of Elements representing the expected background
     * devices.
     * 
     * @param screen
     *            Element representing the screen
     * @return an array of Elements representing the expected devices.
     */
    public Element[] getBackgroundDevices(Element screen)
    {
        return getDevices(screen, "background");
    }

    /**
     * Locates a device with the given id.
     * 
     * @param screen
     *            Element representing the screen
     * @param name
     *            id of desired device
     * @return the desired device or <code>null</code>
     */
    public Element findDevice(Element screen, String name)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());
        return findElement(screen, "device", name, null);
    }

    /**
     * Locates a device with the given id and type.
     * 
     * @param screen
     *            Element representing the screen
     * @param name
     *            id of desired device
     * @param type
     *            type of desired device
     * @return the desired device or <code>null</code>
     */
    public Element findDevice(Element screen, String name, String type)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());
        return findElement(screen, "device", name, type);
    }

    /**
     * Returns the default device(s) of the given type for the given screen.
     * 
     * @param screen
     *            Element representing the screen
     * @param type
     *            type of desired device (or null for any)
     * @return array of default devices
     */
    public Element[] getDefaultDevices(Element screen, String type)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());

        String ids = screen.getAttribute("default");
        StringTokenizer tok = new StringTokenizer(ids, " ");
        Vector v = new Vector();
        while (tok.hasMoreTokens())
        {
            Element device = findElement(screen, "device", tok.nextToken(), type);
            if (device != null) v.addElement(device);
        }

        Element[] e = new Element[v.size()];
        v.copyInto(e);
        return e;
    }

    /**
     * Returns the default graphics device for the screen.
     * 
     * @param screen
     *            Element representing the screen
     * @return the default graphics device for the screen.
     */
    public Element getDefaultGraphicsDevice(Element screen)
    {
        Element e[] = getDefaultDevices(screen, "graphics");
        return (e.length == 0) ? null : e[0];
    }

    /**
     * Returns the default video device for the screen.
     * 
     * @param screen
     *            Element representing the screen
     * @return the default video device for the screen.
     */
    public Element getDefaultVideoDevice(Element screen)
    {
        Element e[] = getDefaultDevices(screen, "video");
        return (e.length == 0) ? null : e[0];
    }

    /**
     * Returns the default background device for the screen.
     * 
     * @param screen
     *            Element representing the screen
     * @return the default background device for the screen.
     */
    public Element getDefaultBackgroundDevice(Element screen)
    {
        Element e[] = getDefaultDevices(screen, "background");
        return (e.length == 0) ? null : e[0];
    }

    /**
     * Returns the configurations for the given device.
     * 
     * @param device
     *            Element representing the device
     * @return array of Elements representing the device's configurations
     */
    public Element[] getConfigurations(Element device)
    {
        assertEquals("Not a device", "device", device.getTagName());
        return getElements(device, "config");
    }

    /**
     * Returns the default configuration for the given device, if one is known.
     * 
     * @param device
     *            Element representing the device
     * @return Element representing the device's default configuration, or null
     */
    public Element getDefaultConfiguration(Element device)
    {
        assertEquals("Not a device", "device", device.getTagName());

        // Check if there is a default naming
        // If there is, return that config
        if (device.hasAttribute("default"))
        {
            String id = device.getAttribute("default");

            return findElement(device, "config", id, null);
        }
        return null;
    }

    /**
     * Returns the expected coherent configurations for the given screen.
     * 
     * @param screen
     *            Element representing the screen
     * @return array of Elements representing the device's configurations
     */
    public Element[] getCoherentConfigs(Element screen)
    {
        assertEquals("Not a screen", "screen", screen.getTagName());
        return getElements(screen, "coherent");
    }

    /**
     * Returns the configuration elements that make up a given coherent
     * configuration.
     * 
     * @param coherent
     *            Element representing a coherent screen configuration
     * @param inclNotContrib
     *            include the non-contrib configuration if present
     * @return array of Elements representing configurations that make up a
     *         coherent screen configuration.
     */
    public Element[] getCoherentElements(Element coherent, boolean inclNotContrib)
    {
        assertEquals("Not a coherent config", "coherent", coherent.getTagName());
        Element screen = (Element) coherent.getParentNode();
        Element devices[] = getDevices(screen);

        String ids = coherent.getAttribute("refid");
        StringTokenizer tok = new StringTokenizer(ids, " ");
        Vector v = new Vector();
        Element notcontrib = inclNotContrib ? getNotContribVideo() : null;
        while (tok.hasMoreTokens())
        {
            String id = tok.nextToken();

            if ("notcontrib".equals(id))
            {
                if (notcontrib != null) v.addElement(notcontrib);
                continue;
            }

            // Look at each device in screen
            for (int i = 0; i < devices.length; ++i)
            {
                Element config = findElement(devices[i], "config", id, null);
                if (config != null)
                {
                    v.addElement(config);
                    break;
                }
            }
        }

        Element elements[] = new Element[v.size()];
        v.copyInto(elements);

        return elements;
    }

    /**
     * Returns <i>req</i> if value of attribute represent true, <i>not</i>
     * otherwise.
     * 
     * @param element
     *            Element to lookup attribute in
     * @param attr
     *            name of attribute to parse as boolean
     * @param req
     *            value to return if attribute is true
     * @param not
     *            value to return if attribute is false
     * @return <i>req</i> if value of attribute represent true, <i>not</i>
     *         otherwise.
     */
    private int isRequired(Element config, String attr, int req, int not)
    {
        return "true".equals(config.getAttribute(attr)) ? req : not;
    }

    /**
     * Returns REQUIRED or REQUIRED_NOT based upon the boolean value of the
     * given attribute.
     * 
     * @param element
     *            Element to lookup attribute in
     * @param attr
     *            name of attribute to parse as boolean
     * @return REQUIRED if attribute is true; REQUIRED_NOT otherwise
     */
    private int isRequired(Element config, String attr)
    {
        return isRequired(config, attr, HScreenConfigTemplate.REQUIRED, HScreenConfigTemplate.REQUIRED_NOT);
    }

    /**
     * Returns a boolean specified by the given attribute of the given element.
     * 
     * @param element
     *            Element to lookup attribute in
     * @param attr
     *            name of attribute to parse as a boolean
     * @return boolean representation of given attribute
     */
    public boolean getBoolean(Element element, String attr)
    {
        return "true".equals(element.getAttribute(attr));
    }

    /**
     * Returns a int specified by the given attribute of the given element.
     * 
     * @param element
     *            Element to lookup attribute in
     * @param attr
     *            name of attribute to parse as a int
     * @return int representation of given attribute
     */
    public int getInt(Element element, String attr)
    {
        return Integer.parseInt(element.getAttribute(attr));
    }

    /**
     * Returns a float specified by the given attribute of the given element.
     * 
     * @param element
     *            Element to lookup attribute in
     * @param attr
     *            name of attribute to parse as a float
     * @return float representation of given attribute
     */
    public float getFloat(Element element, String attr)
    {
        String value = element.getAttribute(attr);
        int index;

        if ((index = value.indexOf("/")) == -1)
        {
            return Float.parseFloat(value);
        }
        else
        {
            float a = Float.parseFloat(value.substring(0, index));
            float b = Float.parseFloat(value.substring(index + 1));

            return a / b;
        }
    }

    /**
     * Generates a Dimension using an element with the given tag in the given
     * configuration element.
     * 
     * @param config
     *            Element representing a configuration
     * @param tag
     *            tag-name of child-Element that should describe a Dimension
     */
    public Dimension getDimension(Element config, String tag)
    {
        Element e = findElement(config, tag, null, null);
        if (e == null) return null;
        return new Dimension(getInt(e, "width"), getInt(e, "height"));
    }

    /**
     * Generates an HScreenRectangle (actually an HRect) using an element with
     * the given tag in the given configuration element.
     * 
     * @param config
     *            Element representing a configuration
     * @param tag
     *            tag-name of child-Element that should describe an
     *            HScreenRectangle
     */
    public HScreenRectangle getHRect(Element config, String tag)
    {
        Element e = findElement(config, tag, null, null);
        if (e == null) return null;
        return new HRect(getFloat(e, "x"), getFloat(e, "y"), getFloat(e, "width"), getFloat(e, "height"));
    }

    /**
     * Generates an HScreenConfigTemplate from the given configuration
     * description.
     * 
     * @param config
     *            Element representing a configuration
     * @return HScreenConfigTemplate that should be useable to locate the actual
     *         configuration
     */
    public HScreenConfigTemplate toTemplate(Element config)
    {
        assertEquals("Not a config", "config", config.getTagName());

        Element device = (Element) config.getParentNode();

        HScreenConfigTemplate t = null;
        String type = device.getAttribute("type");

        if ("graphics".equals(type))
            t = new HGraphicsConfigTemplate();
        else if ("video".equals(type) || "notcontrib".equals(device.getTagName()))
            t = new HVideoConfigTemplate();
        else if ("background".equals(type))
            t = new HBackgroundConfigTemplate();
        else
            fail("Unknown device type");

        t.setPreference(HScreenConfigTemplate.INTERLACED_DISPLAY, isRequired(config, "interlaced"));
        t.setPreference(HScreenConfigTemplate.FLICKER_FILTERING, isRequired(config, "flicker"));
        t.setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, getDimension(config, "pixelRatio"),
                HScreenConfigTemplate.REQUIRED);
        t.setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, getDimension(config, "resolution"),
                HScreenConfigTemplate.REQUIRED);
        t.setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, getHRect(config, "area"),
                HScreenConfigTemplate.REQUIRED);

        if ("graphics".equals(type))
        {
            // Not used in this way... used to see if it can be mixed with a
            // HVConfig
            /*
             * t.setPreference(HGraphicsConfigTemplate.VIDEO_MIXING,
             * isRequired(config, "vidmix"));
             */
            t.setPreference(HGraphicsConfigTemplate.MATTE_SUPPORT, isRequired(config, "matte"));
            t.setPreference(HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT, isRequired(config, "scaling"));
        }
        else if ("video".equals(type))
        {
            // Not used in this way... used to see if it can be mixed with a
            // HGConfig
            /*
             * t.setPreference(HVideoConfigTemplate.GRAPHICS_MIXING,
             * isRequired(config, "gfxmix"));
             */
        }
        else if ("background".equals(type))
        {
            t.setPreference(HBackgroundConfigTemplate.CHANGEABLE_SINGLE_COLOR, isRequired(config, "color"));
            t.setPreference(HBackgroundConfigTemplate.STILL_IMAGE, isRequired(config, "still"));
        }

        return t;
    }

    /**
     * Dump element information to a <code>String</code>. Apparently
     * <code>Element.toString()</code> doesn't give us enough information.
     * 
     * @param element
     *            the element to dump
     * @return description of <i>element</i>
     */
    public String toString(Element element)
    {
        try
        {
            return toString(element, "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    private String toString(Element element, String indent)
    {
        StringBuffer sb = new StringBuffer();
        String newIndent = indent + "    ";

        sb.append(indent).append('<').append(element.getTagName()).append(' ');
        if (element.hasAttributes())
        {
            NamedNodeMap attribs = element.getAttributes();
            final int n = attribs.getLength();
            for (int i = 0; i < n; ++i)
            {
                Attr attr = (Attr) attribs.item(i);
                if (attr.getSpecified())
                    sb.append("\n")
                            .append(newIndent)
                            .append(attr.getName())
                            .append("=\"")
                            .append(attr.getValue())
                            .append("\" ");
                else
                    sb.append("\n").append(newIndent).append(attr.getName()).append("=??? ");
            }
        }
        if (element.hasChildNodes())
        {
            sb.append(indent).append('>');

            NodeList children = element.getChildNodes();
            final int n = children.getLength();
            for (int i = 0; i < n; ++i)
            {
                Node sub = children.item(i);
                if (sub instanceof Element) sb.append('\n').append(toString((Element) sub, newIndent));
            }

            sb.append(indent).append("</").append(element.getTagName()).append('>');
        }
        else
        {
            sb.append("/>");
        }
        return sb.toString();
    }
}

/**
 * Extension of HScreenRectangle to simplify comparisons and toString
 * operations.
 */
class HRect extends HScreenRectangle
{
    public HRect(HScreenRectangle r)
    {
        super(r.x, r.y, r.width, r.height);
    }

    public HRect(float x, float y, float width, float h)
    {
        super(x, y, width, h);
    }

    public boolean equals(Object o)
    {
        HScreenRectangle obj;
        return o instanceof HScreenRectangle && ((obj = (HScreenRectangle) o) != null)
                && TestSupport.areEqual(this, obj);
    }

    public String toString()
    {
        return "HRect[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
