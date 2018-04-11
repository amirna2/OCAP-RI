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

package org.cablelabs.impl.ocap.hn.util.xml.miniDom;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.util.MPEEnv;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import net.n3.nanoxml.IXMLBuilder;

//import org.apache.log4j.Logger;

/**
 * This class builds a tree of Nodes for a scaled-down DOM parser based on
 * org.w3c.dom.
 * <p>
 * A better, but more time-consuming, approach would have been to integrate the
 * HN XML-parsing logic more tightly into the XmlManager framework.
 */
public class NodeBuilder implements IXMLBuilder
{
    private Node root;

    private String text;

    private Stack nodeStack;

    // Log4J logger.
    private static final Logger log = Logger.getLogger(NodeBuilder.class);

    /**
     * Gets the top level Node. In DOM this would be the Document, but here it
     * is a Node.
     */
    public Node getNode()
    {
        return root;
    }

    public void startBuilding(String systemID, int lineNr)
    {
        root = null;
        text = "";
        nodeStack = new Stack();
    }

    public void newProcessingInstruction(String target, Reader reader)
    {
    }

    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr)
    {
        nsURI = possiblyForcedNamespaceName(nsPrefix, nsURI);

        QualifiedName qn = new QualifiedName(nsURI, name);

        if (UPnPConstants.QN_OCAP_SCHEDULE_START_DATE_TIME.equals(qn))
        {
            qn = UPnPConstants.QN_OCAP_SCHEDULED_START_DATE_TIME;
        }

        NodeImpl node = new NodeImpl(Node.ELEMENT_NODE, nsPrefix, qn);

        if (!nodeStack.empty()) // get the parent if any
        {
            NodeImpl parent = (NodeImpl) nodeStack.peek();

            parent.appendChild(node); // add this child node to parent

            node.setParentNode(parent); // add the parent to this child node
        }

        nodeStack.push(node);
    }

    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type)
    {
        NodeImpl node = (NodeImpl) nodeStack.peek();

        NamedNodeMapImpl nodeMap = (NamedNodeMapImpl) node.getAttributes();

        nsURI = possiblyForcedNamespaceName(nsPrefix, nsURI);

        NodeImpl attributeNode = new NodeImpl(Node.ATTRIBUTE_NODE, nsPrefix, new QualifiedName(nsURI, key));

        attributeNode.setNodeValue(value);

        nodeMap.setNamedItem(attributeNode);
    }

    public void elementAttributesProcessed(String name, String nsPrefix, String nsURI)
    {
    }

    public void endElement(String name, String nsPrefix, String nsURI)
    {
        NodeImpl node = (NodeImpl) nodeStack.pop(); // take the end node off the
                                                    // stack

        if (!text.equals(""))
        {
            NodeImpl textNode = new NodeImpl(Node.TEXT_NODE, null, null);
            textNode.setNodeValue(text);
            node.appendChild(textNode);
        }

        text = "";

        if (nodeStack.empty()) // must be at the end of the elements, save the
                               // root node
        {
            root = node;

            nodeStack = null;
        }
    }

    public void addPCData(Reader reader, String systemID, int lineNr)
    {
        int bufSize = 2048;
        int sizeRead = 0;
        StringBuffer str = new StringBuffer(bufSize);
        char[] buf = new char[bufSize];

        for (;;)
        {
            if (sizeRead >= bufSize)
            {
                bufSize *= 2;
                str.ensureCapacity(bufSize);
            }

            int size;

            try
            {
                size = reader.read(buf);
            }
            catch (IOException e)
            {
                break;
            }

            if (size < 0)
            {
                break;
            }

            str.append(buf, 0, size);
            sizeRead += size;
        }

        text += str;
    }

    public Object getResult()
    {
        return root;
    }

    /**
     * Force the namespace prefix "ocap" to refer to the DIDL-Lite ocap namespace name.
     *
     * @param prefix A namespace prefix, which may or may not be "ocap".
     * @param name The namespace name given for that namespace prefix.
     *
     * @return The DIDL-Lite ocap namespace name if the namespace prefix is "ocap",
     *         else the given namespace name.
     */
    private static String possiblyForcedNamespaceName(String prefix, String name)
    {
        return
                UPnPConstants.NSN_OCAP_PREFIX.equals(prefix)
            && "true".equalsIgnoreCase(MPEEnv.getEnv("OCAP.stb.forceOcapNamespaceName"))
                ? UPnPConstants.NSN_OCAP_METADATA
                : name;
    }
}
