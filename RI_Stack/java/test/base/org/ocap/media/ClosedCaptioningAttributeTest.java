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

package org.ocap.media;

import java.awt.Color;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

/**
 * Tests org.ocap.media.ClosedCaptioningAttribute class.
 * 
 * @author Amir Nathoo
 */
public class ClosedCaptioningAttributeTest extends TestCase
{

    public ClosedCaptioningAttribute attrib = null;

    public Object[] value = null;

    /**
     * Verify that there are no public constructors.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(ClosedCaptioningAttribute.class);
    }

    /**
     * Verify that we can get a instance of Closed Captioning
     */
    public void test_getInstance()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            System.out.println("<<CC TEST>> START test_getInstance()\n");
            ClosedCaptioningAttribute attrib = ClosedCaptioningAttribute.getInstance();
            assertNotNull("getInstance() returned null", attrib);
            assertNotNull("getInstance should check with SecurityManager", sm.p);
            assertTrue("getInstance should check for MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("getInstance should check for MonitorAppPermission(handler.closedCaptioning)",
                    "handler.closedCaptioning", ((MonitorAppPermission) sm.p).getName());
            System.out.println("<<CC TEST>> END test_getInstance(" + attrib + ")\n");
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Get Capabilities for Digital CC Pen ForegroundColorAttributes Should
     * returned a color palette that contains all possible colors
     */
    public void test_getCCDigitalCapsFgColor()
    {
        Object[] c = null;
        Color aColor;
        Color expected = new Color(0, 0, 128);
        int value;
        System.out.println("<<CC TEST>> BEGIN test_getCCDigitalCapsFgColor()\n");
        c = attrib.getCCCapability(0, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
        assertNotNull("<<CC TEST>> FAILED: Attribute value returned null", c);
        aColor = (Color) (c[1]);
        System.out.println("<<CC TEST>> Returned Value is " + aColor.getRGB() + "\n");
        assertEquals("<<CC TEST>> FAILED:  Attribute value should be 0x00000080", (expected.equals(aColor)), true);
        System.out.println("<<CC TEST>> END\n");
    }

    /**
     * @todo disabled per 5128
     */
    public void test_getCCDigitalCapsBorderColor()
    {
        Object[] c;
        System.out.println("<<CC TEST>> BEGIN test_getCCDigitalCapsBorderColor()\n");
        c = attrib.getCCCapability(11, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
        assertNotNull("<<CC TEST>> FAILED: Attribute capability should be not null", c);
        System.out.println("<<CC TEST>> END\n");
    }

    /**
     * @todo disabled per 5128
     */
    public void test_getCCAnalogCapsFgColor()
    {
        Object[] c;
        System.out.println("<<CC TEST>> BEGIN test_getCCAnalogCapsFgColor()\n");
        c = attrib.getCCCapability(0, ClosedCaptioningAttribute.CC_TYPE_ANALOG);
        assertNotNull("<<CC TEST>> FAILED: Attribute value should be not null", c);
        System.out.println("<<CC TEST>> END\n");
    }

    public void test_getCCAttributeDefault()
    {
        Object c = null;
        System.out.println("<<CC TEST>> BEGIN test_getCCAttributeDefault()\n");
        for (int i = 0; i < 12; i++)
        {
            c = attrib.getCCAttribute(i, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
            if (c == null)
                System.out.println("<<CC TEST>> Attribute: " + i + " Not set\n");
            else
                System.out.println("<<CC TEST>> Attribute: " + i + " = " + c + "\n");
            // assertNull("<<CC TEST>> FAILED: Attribute value should be null",
            // c);
        }
        System.out.println("<<CC TEST>> END\n");
    }

    /**
     * @todo disabled per 5128
     */
    public void test_setDigitalCCBorderColor()
    {

        int[] attributes = { 11 };
        System.out.println("<<CC TEST>> BEGIN test_setDigitalCCBorderColor()\n");
        try
        {
            attrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
        }
        catch (IllegalArgumentException e)
        {
            fail("Caught IllegalArgumentException for bad Attribute Value");
            System.out.println("<<CC TEST>> Caught IllegalArgumentException\n");
        }
    }

    /**
     * @todo diabled per 5128
     */
    public void test_setCCAttribute()
    {
        System.out.println("<<CC TEST>> BEGIN test_setCCAttribute()\n");
        int[] attributes = { 0, 1, 2, 3, 4 };
        Object o;
        attrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);

        for (int i = 0; i < 5; i++)
        {
            switch (attributes[i])
            {
                case 0: // fg color
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for FG_COLOR should be 0x808080\n", (value[i].equals(o)),
                            true);
                    break;
                case 1: // bg color
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for BG_COLOR should 0xffffff\n", (value[i].equals(o)), true);
                    break;

                case 2: // fg opacity
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for FG_OPACITY should be TRANSLUCENT\n",
                            (value[i].equals(o)), true);
                    break;

                case 3: // bg opacity
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for BG_OPACITY should be TRANSLUCENT\n",
                            (value[i].equals(o)), true);
                    break;
                case 4: // font style
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for FONT_STYLE should Casual\n", (value[i].equals(o)), true);
                    break;

                case 5: // pen size
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for PEN_SIZE should be SMALL\n", (value[i].equals(o)), true);
                    break;

                case 6: // font italicized
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for FONT_ITALICIZED should be TRUE\n", (value[i].equals(o)),
                            true);
                    break;
                case 7: // font underline
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for FONT_UNDERLINE should be TRUE\n", (value[i].equals(o)),
                            true);
                    break;

                case 8: // window fill color
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for WIN_COLOR should be 0x800000\n", (value[i].equals(o)),
                            true);
                    break;
                case 9: // window fill opacity
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for WIN_OPACITY should be TRANSLUCENT\n",
                            (value[i].equals(o)), true);
                    break;
                case 10:// window border type
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for BORDER_TYPE should be RAISED\n", (value[i].equals(o)),
                            true);
                    break;
                case 11:// window border color
                    o = attrib.getCCAttribute(attributes[i], ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                    assertEquals("<<CC TEST>> FAILED Value for BORDER_COLOR should be 0x000080\n",
                            (value[i].equals(o)), true);
                    break;
            }
        }
        System.out.println("<<CC TEST>> END\n");
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite(ClosedCaptioningAttributeTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        value = new Object[12];
        value[0] = new Color(0, 0, 128); // fg blue
        value[1] = new Color(255, 255, 255); // bg white
        value[2] = new Integer(ClosedCaptioningAttribute.CC_OPACITY_TRANSLUCENT);
        value[3] = new Integer(ClosedCaptioningAttribute.CC_OPACITY_TRANSLUCENT);
        value[4] = new String("Casual");
        value[5] = new Integer(ClosedCaptioningAttribute.CC_PEN_SIZE_SMALL);
        value[6] = new Boolean(true);
        value[7] = new Boolean(true);
        value[8] = new Color(128, 0, 0); // window red
        value[9] = new Integer(ClosedCaptioningAttribute.CC_OPACITY_TRANSLUCENT);
        value[10] = new Integer(ClosedCaptioningAttribute.CC_BORDER_RAISED);
        value[11] = new Color(0, 128, 0); // border green
        attrib = ClosedCaptioningAttribute.getInstance();
    }

    protected void tearDown() throws Exception
    {
        attrib = null;
        value = null;

        super.tearDown();
    }

    public ClosedCaptioningAttributeTest(String name)
    {
        super(name);
    }
}
