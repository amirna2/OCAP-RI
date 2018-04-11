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
package org.cablelabs.xlet.VideoPresentationTest;

import java.util.Vector;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import org.dvb.media.VideoFormatControl;
import org.havi.ui.HScreenRectangle;

/**
 * TestDataFactory
 * 
 * @author Joshua Keplinger
 * 
 */
public class TestDataFactory
{
    public static Vector getTestData(String dataFile) throws Exception
    {
        IXMLElement xml = getDocument(dataFile);

        return createTestData(xml);
    }

    private static IXMLElement getDocument(String dataFile) throws Exception
    {
        IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
        parser.setReader(StdXMLReader.fileReader(dataFile));
        return (IXMLElement) parser.parse();
    }

    private static Vector createTestData(IXMLElement doc)
    {
        Vector data = new Vector();
        Vector vcs = doc.getChildrenNamed("vc");
        for (int vcsCount = 0; vcsCount < vcs.size(); vcsCount++)
        {
            // Parse the vc (VideoConfiguration) tag
            IXMLElement vc = (IXMLElement) vcs.elementAt(vcsCount);
            if (!vc.hasAttribute("parHeight") || !vc.hasAttribute("parWidth") || !vc.hasAttribute("scrHeight")
                    || !vc.hasAttribute("scrWidth")) continue;
            int parHeight = Integer.parseInt(vc.getAttribute("parHeight", "1"));
            int parWidth = Integer.parseInt(vc.getAttribute("parWidth", "1"));
            int scrHeight = Integer.parseInt(vc.getAttribute("scrHeight", "640"));
            int scrWidth = Integer.parseInt(vc.getAttribute("scrWidth", "480"));
            // System.out.println("  vc [parHeight=" + parHeight + ", parWidth="
            // + parWidth +
            // ", scrHeight=" + scrHeight + ", scrWidth=" + scrWidth +"]");
            Vector sources = vc.getChildrenNamed("source");
            for (int sourcesCount = 0; sourcesCount < sources.size(); sourcesCount++)
            {
                // Parse the config tag
                IXMLElement source = (IXMLElement) sources.elementAt(sourcesCount);
                if (!source.hasAttribute("sourceId") || !source.hasAttribute("afd") || !source.hasAttribute("ar"))
                    continue;
                int sourceId = Integer.parseInt(source.getAttribute("sourceId", "0").substring(2), 16);
                int afd = parseVFCConstant(source.getAttribute("afd", "0"));
                int ar = parseVFCConstant(source.getAttribute("ar", "0"));
                // System.out.println("config [sourceId=" + sourceId + ", afd="
                // + afd + ", ar=" + ar + "]");
                Vector results = source.getChildrenNamed("result");
                for (int resultsCount = 0; resultsCount < results.size(); resultsCount++)
                {
                    // Parse the result tag
                    IXMLElement result = (IXMLElement) results.elementAt(resultsCount);
                    if (!result.hasAttribute("dfc")) continue;
                    int dfc = parseVFCConstant(result.getAttribute("dfc", "0"));
                    // System.out.print("      result [dfc=" + dfc + ", ");
                    // Load video areas
                    // Active video area
                    IXMLElement area = (IXMLElement) result.getChildrenNamed("ava").elementAt(0);
                    if (!area.hasAttribute("height") || !area.hasAttribute("width") || !area.hasAttribute("xPosition")
                            || !area.hasAttribute("yPosition")) continue;
                    HScreenRectangle ava = parseHSR(area);
                    // System.out.print("ava=[x=" + ava.x + ", y=" + ava.y +
                    // ", width=" + ava.width +
                    // ", height=" + ava.height + "], ");
                    // Total video area
                    area = (IXMLElement) result.getChildrenNamed("tva").elementAt(0);
                    if (!area.hasAttribute("height") || !area.hasAttribute("width") || !area.hasAttribute("xPosition")
                            || !area.hasAttribute("yPosition")) continue;
                    HScreenRectangle tva = parseHSR(area);
                    // System.out.print("tva=[x=" + tva.x + ", y=" + tva.y +
                    // ", width=" + tva.width +
                    // ", height=" + tva.height + "], ");
                    // Active video area onscreen
                    area = (IXMLElement) result.getChildrenNamed("avaos").elementAt(0);
                    if (!area.hasAttribute("height") || !area.hasAttribute("width") || !area.hasAttribute("xPosition")
                            || !area.hasAttribute("yPosition")) continue;
                    HScreenRectangle avaos = parseHSR(area);
                    // System.out.print("avaos=[x=" + avaos.x + ", y=" + avaos.y
                    // + ", width=" + avaos.width +
                    // ", height=" + avaos.height + "], ");
                    // Total video area onscreen
                    area = (IXMLElement) result.getChildrenNamed("tvaos").elementAt(0);
                    if (!area.hasAttribute("height") || !area.hasAttribute("width") || !area.hasAttribute("xPosition")
                            || !area.hasAttribute("yPosition")) continue;
                    HScreenRectangle tvaos = parseHSR(area);
                    // System.out.println("tvaos=[x=" + tvaos.x + ", y=" +
                    // tvaos.y + ", width=" + tvaos.width +
                    // ", height=" + tvaos.height + "]");

                    // Finally loaded the configuration, create a TestData
                    // object
                    TestData td = new TestData();
                    td.sourceId = sourceId;
                    td.afd = afd;
                    td.ar = ar;

                    td.parHeight = parHeight;
                    td.parWidth = parWidth;
                    td.scrHeight = scrHeight;
                    td.scrWidth = scrWidth;

                    td.dfc = dfc;
                    td.ava = ava;
                    td.tva = tva;
                    td.avaos = avaos;
                    td.tvaos = tvaos;

                    data.addElement(td);
                }
            }
        }

        return data;
    }

    private static HScreenRectangle parseHSR(IXMLElement e)
    {
        float height = Float.valueOf(e.getAttribute("height", "1")).floatValue();
        float width = Float.valueOf(e.getAttribute("width", "1")).floatValue();
        float x = Float.valueOf(e.getAttribute("xPosition", "0")).floatValue();
        float y = Float.valueOf(e.getAttribute("yPosition", "0")).floatValue();

        return new HScreenRectangle(x, y, width, height);
    }

    private static int parseVFCConstant(String constant)
    {
        try
        {
            return VideoFormatControl.class.getField(constant).getInt(VideoFormatControl.class);
        }
        catch (Exception ex)
        {
            // Field isn't there for some reason...
            return -1;
        }
    }
}
