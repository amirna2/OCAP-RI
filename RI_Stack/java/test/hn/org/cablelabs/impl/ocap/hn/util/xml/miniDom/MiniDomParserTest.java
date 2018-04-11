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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.util.MPEEnv;

import org.ocap.hn.content.MetadataNode;

public class MiniDomParserTest extends TestCase
{
    private static final String ORGANIZATION = "CableLabs";

    // NOTE: due to a bug in MetadataNodeImpl.MetadataNodeImpl(Node), <item> nodes
    //       must have at least two children.

    private static final String BAD_OCAP_DECLARATION =
          "<DIDL-Lite xmlns='urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/' xmlns:dc='http://purl.org/dc/elements/1.1/'>"
        + "  <item xmlns:ocap = 'bad'>"
        + "    <dc:title>Title</dc:title>"
        + "    <ocap:organization>" + ORGANIZATION + "</ocap:organization>"
        + "  </item>"
        + "</DIDL-Lite>"
        ;

    private static final String GOOD_OCAP_DECLARATION =
          "<DIDL-Lite xmlns='urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/' xmlns:dc='http://purl.org/dc/elements/1.1/'>"
        + "  <item xmlns:ocap = '" + UPnPConstants.NSN_OCAP_METADATA + "'>"
        + "    <dc:title>Title</dc:title>"
        + "    <ocap:organization>" + ORGANIZATION + "</ocap:organization>"
        + "  </item>"
        + "</DIDL-Lite>"
        ;

    private static final String NO_OCAP_DECLARATION =
          "<DIDL-Lite xmlns='urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/' xmlns:dc='http://purl.org/dc/elements/1.1/'>"
        + "  <item>"
        + "    <dc:title>Title</dc:title>"
        + "    <ocap:organization>" + ORGANIZATION + "</ocap:organization>"
        + "  </item>"
        + "</DIDL-Lite>"
        ;

    public void testBadOcapDeclarationForcing()
    {
        System.out.println("testBadOcapDeclarationForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(BAD_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("Bad ocap declaration, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        ORGANIZATION, organization);
    }

    public void testBadOcapDeclarationNoForcing()
    {
        System.out.println("testBadOcapDeclarationNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(BAD_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("Bad ocap declaration, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testGoodOcapDeclarationForcing()
    {
        System.out.println("testGoodOcapDeclarationForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(GOOD_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("Good ocap declaration, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        ORGANIZATION, organization);
    }

    public void testGoodOcapDeclarationNoForcing()
    {
        System.out.println("testGoodOcapDeclarationNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(GOOD_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("Good ocap declaration, no forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        ORGANIZATION, organization);
    }

    public void testNoOcapDeclarationForcing()
    {
        System.out.println("testNoOcapDeclarationForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(NO_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("No ocap declaration, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        ORGANIZATION, organization);
    }

    public void testNoOcapDeclarationNoForcing()
    {
        System.out.println("testNoOcapDeclarationNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(NO_OCAP_DECLARATION);
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("No ocap declaration, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testAdbDidlLiteNoForcing()
    {
        System.out.println("testAdbDidlLiteNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(toString("adb-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("ADB DIDL-Lite, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testAdbDidlLiteForcing()
    {
        System.out.println("testAdbDidlLiteForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(toString("adb-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("ADB DIDL-Lite, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        "00000034", organization);
    }

    public void testAdbDidlLite2NoForcing()
    {
        System.out.println("testAdbDidlLite2NoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(toString("didllite-adb.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("ADB DIDL-Lite, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testAdbDidlLite2Forcing()
    {
        System.out.println("testAdbDidlLite2Forcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(toString("didllite-adb.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("ADB DIDL-Lite, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        "00000034", organization);
    }

    public void testCiscoDidlLiteNoForcing()
    {
        System.out.println("testCiscoDidlLiteNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(toString("cisco-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("Cisco DIDL-Lite, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testCiscoDidlLiteForcing()
    {
        System.out.println("testCiscoDidlLiteForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(toString("cisco-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("Cisco DIDL-Lite, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        "00000034", organization);
    }

    public void testMotorolaDidlLiteNoForcing()
    {
        System.out.println("testMotorolaDidlLiteNoForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "false");

        Node n = MiniDomParser.parse(toString("moto-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertNull("Motorola DIDL-Lite, no forcing: got \"" + organization + "\" instead of null", organization);
    }

    public void testMotorolaDidlLiteForcing()
    {
        System.out.println("testMotorolaDidlLiteForcing");

        MPEEnv.setEnv("OCAP.stb.forceOcapNamespaceName", "true");

        Node n = MiniDomParser.parse(toString("moto-didllite.xml"));
        MetadataNode mn = new MetadataNodeImpl(n);
        mn = (MetadataNode) mn.getMetadata("didl-lite:item");
        String organization = (String) mn.getMetadata("ocap:organization");
        assertEquals("Motorola DIDL-Lite, forcing: got \"" + organization + "\" instead of \"" + ORGANIZATION + "\"",
                        "00000034", organization);
    }

    private static String toString(String fileName)
    {
        // TODO: This relative pathname hack is awful. Greg advises doing this instead:
        //       1. Put the file in the same directory as the .java file.
        //       2. Update the build system so that it will copy the file to the installed class file location.
        //       3. Load the file as a resource.

        fileName = "../../../../../../java/test/hn/org/cablelabs/impl/ocap/hn/util/xml/miniDom/" + fileName;

        byte[] buffer = new byte[(int) new File(fileName).length()];

        InputStream is = null;

        try
        {
            is = new FileInputStream(fileName);
            is.read(buffer);
        }
        catch (IOException e)
        {
            e.printStackTrace(System.out);
            fail("Couldn't read " + fileName);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        return new String(buffer);
    }
}
