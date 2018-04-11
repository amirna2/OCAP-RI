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

package org.cablelabs.impl.ocap.hn.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;

import org.cybergarage.upnp.diag.Message;

import org.ocap.hn.upnp.common.UPnPMessage;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Utility class containing static methods needed for UPnP Diagnostics support.
 */
public class XMLUtil
{
    /**
     * Log4j logging category.
     */
    private static final Logger log = Logger.getLogger(XMLUtil.class);

    /**
     * An empty byte array.
     */
    private static final byte[] EMPTY_BYTE_ARRAY = {};

    /**
     * An empty byte array array.
     */
    private static final byte[][] EMPTY_BYTE_ARRAY_ARRAY = {};

    /**
     * An empty string array.
     */
    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * A static <code>DocumentBuilder</code> used for parsing XML.
     */
    private static final DocumentBuilder documentBuilder = documentBuilder();

    /**
     * Construct an object of this class.
     *
     * <p>
     * Private, to avoid instantiation.
     */
    private XMLUtil()
    {
    }

    /**
     * Combine separate headers into combined headers.
     *
     * @param separateHeaders The separate headers, in the form
     *                        { <header 1>, ..., <header n> }.
     *
     * @return The combined headers, in the form
     *         <header 1> CRLF ... <header n> CRLF.
    */
    private static byte[] combine(String[] separateHeaders)
    {
        if (separateHeaders == null)
        {
            return EMPTY_BYTE_ARRAY;
        }
        
        StringBuffer sb = new StringBuffer();

        for (int i = 0, n = separateHeaders.length; i < n; ++ i)
        {
            sb.append(separateHeaders[i]).append('\r').append('\n');
        }
        
        return sb.toString().getBytes();
    }

    /**
     * Get the static <code>DocumentBuilder</code>.
     *
     * @return The static <code>DocumentBuilder</code>.
     *
     * @throws IllegalStateException if the <code>DocumentBuilder</code>
     *         cannot be constructed due to a <code>ParserConfigurationException</code>.
     */
    private static DocumentBuilder documentBuilder()
    {
        try
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new IllegalStateException("ParserConfigurationException: message is " + e.getMessage());
        }
    }

    /**
     * Separate combined headers into separate headers.
     *
     * @param combinedHeaders The combined headers, in the form
     *                        <header 1> CRLF ... <header n> CRLF.
     *
     * @return The separate headers, in the form
     *         { <header 1>, ..., <header n> }.
     */
    private static byte[][] separate(byte[] combinedHeaders)
    {
        byte[][] separateHeaders;

        if (combinedHeaders == null)
        {
            separateHeaders = EMPTY_BYTE_ARRAY_ARRAY;
        }
        else
        {
            int length = 0;

            for (int i = 0, n = combinedHeaders.length; i < n; ++ i)
            {
                if (combinedHeaders[i] == '\r' && i + 1 < n && combinedHeaders[i + 1] == '\n')
                {
                    ++ length;
                }
            }

            separateHeaders = new byte[length][];

            for (int i = 0, n = combinedHeaders.length, firstCh = 0, j = 0; i < n; ++ i)
            {
                if (combinedHeaders[i] == '\r' && i + 1 < n && combinedHeaders[i + 1] == '\n')
                {
                    int len = i - firstCh;
                    separateHeaders[j] = new byte[len];
                    System.arraycopy(combinedHeaders, firstCh, separateHeaders[j], 0, len);
                    ++ j;
                    i += 2;
                    firstCh = i;
                }
            }
        }

        return separateHeaders;
    }

    /**
     * Convert a <code>Document</code> to a byte array.
     *
     * @param document The <code>Document</code>.
     *
     * @return The byte array.
     */
    public static byte[] toByteArray(Document document)
    {
        return toByteArray(toString(document));
    }

    /**
     * Convert a <code>String</code> to a byte array.
     *
     * @param string The <code>String</code>.
     *
     * @return The byte array.
     *
     * @throws UnsupportedEncodingException if the UTF-8 encoding is not
     *         available. This should not happen!
     */
    public static byte[] toByteArray(String string)
    {
        try
        {
            return string.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Convert a <code>UPnPMessage</code> to a byte array.
     *
     * @param string The <code>UPnPMessage</code>.
     *
     * @return The byte array.
     *
     * @throws UnsupportedEncodingException if the UTF-8 encoding is not
     *         available. This should not happen!
     */
    public static byte[] toByteArray(UPnPMessage m)
    {
        if (m == null)
        {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] body = toByteArray(m.getXML());
        int nBodyBytes = body.length;

        String[] headers = m.getHeaders();
        
        String[] headersAndStartLine = new String[headers.length + 1];
        
        headersAndStartLine[0] = m.getStartLine();        

        for (int i = 0, n = headers.length; i < n; ++ i)
        {
            if (headers[i] != null &&
                    headers[i].trim().toLowerCase().startsWith("content-length:"))
            {
                headers[i] = "Content-Length: " + nBodyBytes;
            }
            headersAndStartLine[i + 1] = headers[i];
        }

        byte[] header = combine(headersAndStartLine);
        int nHeaderBytes = header.length + 2;

        byte[] ba = new byte[nHeaderBytes + nBodyBytes];

        System.arraycopy(header, 0, ba, 0, nHeaderBytes - 2);
        ba[nHeaderBytes - 2] = '\r';
        ba[nHeaderBytes - 1] = '\n';
        System.arraycopy(body, 0, ba, nHeaderBytes, nBodyBytes);

        return ba;
    }

    /**
     * Convert a <code>UPnPMessage</code> to a <code>Message</code>.
     *
     * @param m The <code>UPnPMessage</code>.
     *
     * @return The <code>Message</code>.
     */
    public static Message toMessage(UPnPMessage m)
    {
        assert m != null;

        byte[] body = toByteArray(m.getXML());
        int nBodyBytes = body.length;

        String[] headers = m.getHeaders();
        String[] headersAndStartLine = new String[headers.length + 1];
        
        headersAndStartLine[0] = m.getStartLine();
        
        for (int i = 0; i < headers.length; ++i)
        {
            if (headers[i] != null &&
                    headers[i].trim().toLowerCase().startsWith("content-length:"))
            {
                headers[i] = "Content-Length: " + nBodyBytes;
            }
            headersAndStartLine[i + 1] = headers[i]; 
        }
        
        byte[][] byteHeaders = new byte[headersAndStartLine.length][];
        for(int i = 0; i < headersAndStartLine.length; ++i)
        {
            byteHeaders[i] = headersAndStartLine[i] != null ? headersAndStartLine[i].getBytes() : "".getBytes();
        }
        
        return new Message(byteHeaders, body);
    }

    /**
     * Create a <code>Node</code> from a <code>String</code>.
     *
     * @param string The <code>String</code>.
     *
     * @return The <code>Node</code>.
     *
     * @throws SAXException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>SAXException</code>.
     * @throws IOException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>IOException</code>.
     */
    public static Document toNode(String string)
        throws SAXException, IOException
    {
        return toNode(toByteArray(string));
    }

    /**
     * Create a <code>Node</code> from a byte array.
     *
     * @param bytes The byte array.
     *
     * @return The <code>Node</code>.
     *
     * @throws SAXException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>SAXException</code>.
     * @throws IOException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>IOException</code>.
     */
    public static Document toNode(byte[] bytes)
        throws SAXException, IOException
    {
        return toNode(bytes, 0, bytes.length);
    }

    /**
     * Create a <code>Node</code> from a portion of a byte array.
     *
     * @param bytes The byte array.
     * @param offset The offset of the portion within the byte array.
     * @param length The length of the portion.
     *
     * @return The <code>Node</code>.
     *
     * @throws SAXException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>SAXException</code>.
     * @throws IOException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>IOException</code>.
     */
    public static Document toNode(byte[] bytes, int offset, int length)
        throws SAXException, IOException
    {
        return toNode(new ByteArrayInputStream(bytes, offset, length));
    }

    /**
     * Create a <code>Node</code> from an <code>InputStream</code>.
     *
     * @param stream The <code>InputStream</code>.
     *
     * @return The <code>Node</code>.
     *
     * @throws SAXException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>SAXException</code>.
     * @throws IOException if parsing with the <code>DocumentBuilder</code>
     *         throws a <code>IOException</code>.
     */
    public static Document toNode(InputStream stream)
        throws SAXException, IOException
    {
        synchronized (documentBuilder)
        {
            return documentBuilder.parse(stream);
        }
    }

    /**
     * Convert a byte array to a <code>String</code>.
     *
     * @param ba The byte array.
     *
     * @return The <code>String</code>.
     *
     * @throws UnsupportedEncodingException if the UTF-8 encoding is not
     *         available. This should not happen!
     */
    public static String toString(byte[] ba)
    {
        try
        {
            return new String(ba, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Convert a portion of a byte array to a <code>String</code>.
     *
     * @param ba     The byte array.
     * @param offset The offset of the portion within the byte array.
     * @param length The length of the portion within the byte array.
     *
     * @return The <code>String</code>.
     *
     * @throws UnsupportedEncodingException if the UTF-8 encoding is not
     *         available. This should not happen!
     */
    public static String toString(byte[] ba, int offset, int length)
    {
        try
        {
            return new String(ba, offset, length, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException();
        }
    }

    /**
     * Return a string representation of a <code>Document</code>.
     *
     * @param document The <code>Document</code>.
     *
     * @return A string representation of the <code>Document</code>.
     */
    private static String toString(Document document)
    {
        if (document == null)
        {
            return "";
        }

        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        toStringAppend(sb, document.getDocumentElement(), "");

        return sb.toString();
    }

    /**
     * Append a string representation of an <code>Element</code> to a
     * <code>StringBuffer</code> with a certain indentation.
     * Helper method for <code>toString(Node)</code>.
     *
     * @param sb      The <code>StringBuffer</code>.
     * @param element The <code>Element</code>.
     * @param indent  The indentation.
     */
    private static void toStringAppend(StringBuffer sb, Element element, String indent)
    {
        if (element == null)
        {
            return;
        }

        String tagName = element.getTagName();

        sb.append(indent).append('<').append(tagName);

        if (element.hasAttributes())
        {
            NamedNodeMap attribs = element.getAttributes();

            for (int i = 0, n = attribs.getLength(); i < n; ++ i)
            {
                Attr attr = (Attr) attribs.item(i);

                sb.append(" ")
                        .append(attr.getName())
                        .append("=\"")
                        .append(Utils.toXMLEscaped(attr.getValue()))
                        .append("\"");
            }
        }

        if (element.hasChildNodes())
        {
            sb.append('>');

            NodeList children = element.getChildNodes();

            int n = children.getLength();

            if (n == 1 && children.item(0) instanceof Text)
            {
                sb.append(Utils.toXMLEscaped(((Text) children.item(0)).getData()));
            }
            else
            {
                sb.append('\n');

                String cIndent = "    " + indent;

                for (int i = 0; i < n; ++ i)
                {
                    Node sub = children.item(i);

                    if (sub instanceof Element)
                    {
                        toStringAppend(sb, (Element) sub, cIndent);
                    }
                }

                sb.append(indent);
            }

            sb.append("</").append(tagName).append('>');
        }
        else
        {
            sb.append("/>");
        }

        sb.append('\n');
    }

    /**
     * Convert a byte array to a <code>UPnPMessage</code>.
     *
     * @param ba The byte array.
     *
     * @return The <code>UPnPMessage</code>.
     */
    public static UPnPMessage toUPnPMessage(byte[] ba)
    {
        String startLine;
        String[] headers;

        String string;

        List headerList = new ArrayList();
        int i = 0;

        try
        {
            // RFC2616 says ignore initial empty lines
            do
            {
                int start = i;
                for (; ba[i] != '\r' || ba[i + 1] != '\n'; ++ i)
                {
                }
                string = toString(ba, start, i - start);
                i += 2;
            }
            while (string.length() == 0);

            startLine = string;

            do
            {
                int start = i;
                for (; ba[i] != '\r' || ba[i + 1] != '\n'; ++ i)
                {
                }
                string = toString(ba, start, i - start);
                i += 2;

                if (string.length() != 0)
                {
                    headerList.add(string);
                }
            }
            while (string.length() != 0);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("couldn't parse", e);
            }

            return null;
        }

        int n = headerList.size();

        headers = n > 0 ? (String[]) headerList.toArray(new String[n]) : EMPTY_STRING_ARRAY;

        Document xml;

        if (i == ba.length)
        {
            xml = null;
        }
        else
        {
            try
            {
                xml = XMLUtil.toNode(ba, i, ba.length - i);
            }
            catch (SAXException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("couldn't parse", e);
                }

                return null;
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("couldn't parse", e);
                }

                return null;
            }
        }

        return new UPnPMessage(startLine, headers, xml);
    }

    /**
     * Convert a <code>Message</code> to a <code>UPnPMessage</code>.
     *
     * @param m The <code>Message</code>.
     *
     * @return The <code>UPnPMessage</code>.
     */
    public static UPnPMessage toUPnPMessage(Message m)
    {
        String startLine;
        String[] headers;

        byte[][] messageHeaders = m.getHeaders();

        int nHeaders = messageHeaders != null ? messageHeaders.length : 0;

        if (nHeaders > 0)
        {
            startLine = toString(messageHeaders[0]);
            headers = nHeaders > 1 ? new String[nHeaders - 1] : EMPTY_STRING_ARRAY;

            for (int i = 0, n = headers.length; i < n; ++ i)
            {
                headers[i] = toString(messageHeaders[i + 1]);
            }
        }
        else
        {
            startLine = null;
            headers = null;
        }

        Document xml;

        byte[] messageContent = m.getContent();

        if (messageContent == null || messageContent.length == 0)
        {
            xml = null;
        }
        else
        {
            try
            {
                xml = XMLUtil.toNode(messageContent);
            }
            catch (SAXException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("couldn't parse", e);
                }

                return null;
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("couldn't parse", e);
                }

                return null;
            }
        }

        return new UPnPMessage(startLine, headers, xml);
    }

    /**
     * Utility method to find the child of the supplied node which matches
     * the supplied name
     *
     * @param parent        look for child of this node
     * @param childName     look for a child with this name
     * @return              child with matching name if found, otherwise null
     */
    public static Node getNamedChild(Node parent, String childName)
    {
        Node child = null;
        if (parent != null)
        {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
                Node curChild = children.item(i);
                if (curChild.getNodeName().equalsIgnoreCase(childName))
                {
                    child = curChild;
                    break;
                }
            }
        }
        return child;
    }
    
    /**
     * Utility method to create a clone of a <code>Document</code>.
     * 
     * @param document   document to be cloned
     * @return           the clone of the document
     */
    public static Document clone(Document document) 
    {
        Document clone = null;
        if (document != null)
        {
            
            clone = documentBuilder.newDocument();
            Node originalRoot = document.getDocumentElement();
            Node copiedRoot = clone.importNode(originalRoot, true);
            clone.appendChild(copiedRoot);
        }
        return clone;
    }
}
