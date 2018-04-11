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

public class NodeImpl implements Node
{
    // for debug printing, indexed by node types

    private static final int numTypes = 3;

    private static final String typeStr[] = { "oops", "Elem", "Attr", "Text" };

    public NamedNodeMap getAttributes()
    {
        return this.attributes;
    }

    public NodeList getChildNodes()
    {
        return this.childNodes;
    }

    public Node getFirstChild()
    {
        return this.childNodes.item(0);
    }

    public Node getLastChild()
    {
        return this.childNodes.item(this.childNodes.getLength() - 1);
    }

    public String getNamespacePrefix()
    {
        return namespacePrefix;
    }

    public short getType()
    {
        return this.type;
    }

    public String getValue()
    {
        return this.value;
    }

    public Node getParentNode()
    {
        return this.parentNode;
    }

    public QualifiedName getName()
    {
        return this.name;
    }

    public boolean hasAttributes()
    {
        if (this.attributes != null)
        {
            if (this.attributes.getLength() > 0)
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasChildNodes()
    {
        return this.childNodes != null;
    }

    public int numChildren()
    {
        if (this.childNodes == null)
        {
            return 0;
        }
        else
        {
            return this.childNodes.getLength();
        }
    }

    static private int dumpLev = 0;

    static private String dumpTab = "      ";

    public void dump()
    {
        dumpLev++;

        String indent = "";
        for (int i = 0; i < dumpLev; i++)
        {
            indent += dumpTab;
        }

        String type = String.valueOf(this.type); // default in case corrupt
        if (this.type > 0 || this.type <= numTypes)
        {
            type = typeStr[this.type];
        }

        System.out.println(indent + "NODE (" + this.name + ")" + "(type=" + type + ") ");
        System.out.println(indent + "---- value = (" + this.value + ") ");

        // print out the attributes of this node
        if (attributes != null)
        {
            for (int i = 0; i < this.attributes.getLength(); i++)
            {
                System.out.println(indent + "..... attr = " + "(" + this.attributes.item(i).getName() + ")" + "("
                        + this.attributes.item(i).getValue() + ")");
            }
        }

        // recurse through the children nodes
        if (this.childNodes != null)
        {
            for (int i = 0; i < this.childNodes.getLength(); i++)
            {
                this.childNodes.item(i).dump();
            }
        }
        dumpLev--;
    }

    // ///////////////////////////////////////////////////////////
    // package private implementation
    // ///////////////////////////////////////////////////////////
    NodeImpl(short type, String namespacePrefix, QualifiedName name)
    {
        this.type = type;
        this.namespacePrefix = namespacePrefix;
// TODO: use Attribute, Element, Text classes, implementing Node interface
        if (this.type == Node.ELEMENT_NODE)
        {
            this.attributes = new NamedNodeMapImpl();
        }
        this.name = name;
    }

    void setNodeValue(String value)
    {
        this.value = value;
    }

    void setNamedNodeMap(NamedNodeMapImpl nodeMap)
    {
        this.attributes = nodeMap;
    }

    void setParentNode(Node parent)
    {
        this.parentNode = parent;
    }

    Node appendChild(Node newChild)
    {
        if (this.childNodes == null)
        {
            this.childNodes = new NodeListImpl();
        }

        this.childNodes.add(newChild);

        return newChild;
    }

    private static final String toString(Object o)
    {
        return o != null ? "\"" + o.toString() + "\"" : "null";
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("NodeImpl [");

        sb.append("namespacePrefix = " + toString(namespacePrefix) + ", ");
        sb.append("name = " + toString(name) + ", ");
        sb.append("type = " + type + ", ");
        sb.append("value = " + toString(value) + ", ");
        sb.append("attributes = " + attributes + ", ");
        sb.append("childNodes = " + childNodes);

        sb.append("]");

        return sb.toString();
    }

    private short type;

    private String namespacePrefix;
    private QualifiedName name;

    private String value;

    private NamedNodeMapImpl attributes = null;

    private Node parentNode = null;

    private NodeListImpl childNodes = null;
}
