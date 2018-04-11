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

import org.cablelabs.impl.manager.xml.nano.NanoUtil;
import java.io.StringReader;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.StdXMLReader;
import org.apache.log4j.Logger;

/**
 * This class is a scaled-down DOM parser similar to org.w3c.dom.
 * <p>
 * It uses the createParser method that was pulled out of BasicXMLBuilder. A
 * better, but more time-consuming, approach would have been to integrate the HN
 * XML-parsing logic more tightly into the XmlManager framework.
 */
public class MiniDomParser
{
    /**
     * Build a Node tree based on an input XML string.
     *
     * @param xml
     *            The String to parse into a Node tree.
     * @return The built Node tree root or null if it failed to parse the input
     *         XML.
     */
    public static Node parse(String xml)
    {
        NodeBuilder builder;

        StringReader reader = new StringReader(xml);

        try
        {
            builder = new NodeBuilder();

            IXMLParser parser = NanoUtil.createParser();

            parser.setBuilder(builder);
            parser.setReader(new StdXMLReader(reader));

            parser.parse();
        }
        catch (Exception ex)
        {
            if (log.isDebugEnabled())
            {
                log.debug("parser threw an exception", ex);
            }

            return null;
        }

        return builder.getNode();
    }

    // Log4J logger.
    private static final Logger log = Logger.getLogger(MiniDomParser.class);
}
