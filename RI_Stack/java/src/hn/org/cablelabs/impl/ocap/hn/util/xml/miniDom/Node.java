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

/**
 * This is the interface to a scaled down Node datatype for DOM based on
 * org.w3c.dom.Node
 *
 * @author Dan Woodard
 *
 */
public interface Node
{
    // Node type constants

    public static final short ELEMENT_NODE = 1;

    public static final short ATTRIBUTE_NODE = 2;

    public static final short TEXT_NODE = 3;

    /**
     * Recursively print out the contents of a node (including children)
     */
    public void dump();

    /**
     * Returns the NamedNodeMap that contains the attributes for this node if
     * the node type is ELEMENT_NODE. Returns null for any other node type.
     */
    public NamedNodeMap getAttributes();

    public NodeList getChildNodes();

    public Node getFirstChild();

    public Node getLastChild();

    /**
     * Returns the name of this node depending on its type. If it is a
     * TEXT_NODE, the name will be null. Otherwise, it is the name of the
     * attribute or the element depending on the type.
     */
    public QualifiedName getName();

    /**
     * Returns the namespace prefix of this node, if any.
     */
    public String getNamespacePrefix();

    public Node getParentNode();

    /**
     * Returns the type of this node.
     */
    public short getType();

    /**
     * Returns the value of this node depending on its type. If the type is
     * TEXT_NODE, it returns the content of the text node. If the type is
     * ELEMENT_NODE, it returns null. If the type is ATTRIBUTE_NODE, it returns
     * the value of the attribute.
     */
    public String getValue();

    /**
     * Returns true if this node has attributes.
     */
    public boolean hasAttributes();

    /**
     * Returns true if this node has children.
     */
    public boolean hasChildNodes();

    public int numChildren();
}
