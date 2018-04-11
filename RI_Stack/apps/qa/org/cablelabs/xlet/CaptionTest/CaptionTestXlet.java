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

package org.cablelabs.xlet.CaptionTest;

import javax.tv.service.*;
import javax.tv.service.selection.*;
import javax.tv.xlet.*;

import java.awt.*;
import java.awt.event.*;
import org.ocap.ui.event.*;

import org.havi.ui.*;
import org.havi.ui.event.*;
import org.ocap.media.*;
import org.cablelabs.lib.utils.ArgParser;
import java.io.*;

public class CaptionTestXlet extends Container implements Xlet, KeyListener
{

    private static final String CONFIG_FILE = "config_file";

    private static final String CAPTION_SOURCE0 = "caption_source0";

    private static final String CAPTION_SOURCE1 = "caption_source1";

    private static final String LOG_SUPPORTED_SVC = "log_supported_svc";

    private String _config_file;

    private HScene scene;

    private boolean bAlreadyStarted;

    private ClosedCaptioningAttribute ccAttrib = null;

    private Object[] analogFgColors = null;

    private Object[] digitalFgColors = null;

    private Object[] analogBgColors = null;

    private Object[] digitalBgColors = null;

    private int fg_index = 0;

    private int bg_index = 0;

    private Video video;

    private ClosedCaptioningControl ctrl = null;

    private ServiceContext serviceContext;

    private int channel_index = 0;

    /* source ID with closed captioning */
    private int sourceID[] = { 0x3eb, 0x7d6 };

    private int log_supported_svc = 0;

    private Service service;

    /**
     * 
     * initXlet
     * 
     */
    public void initXlet(javax.tv.xlet.XletContext ctx)
    {
        bAlreadyStarted = false;
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        try
        {
            serviceContext = ServiceContextFactory.getInstance().createServiceContext();
            video = new Video(serviceContext);

            video.setVisible(true);
        }
        catch (Exception e)
        {
        }

        scene.add(video);
        ccAttrib = ClosedCaptioningAttribute.getInstance();

        try
        {
            ArgParser args = new ArgParser((String[]) ctx.getXletProperty(ctx.ARGS));
            _config_file = args.getStringArg(CONFIG_FILE);
            FileInputStream _fis = new FileInputStream(_config_file);
            try
            {
                ArgParser fopt = new ArgParser(_fis);
                sourceID[0] = fopt.getIntArg(CAPTION_SOURCE0);
                sourceID[1] = fopt.getIntArg(CAPTION_SOURCE1);
                log_supported_svc = fopt.getIntArg(LOG_SUPPORTED_SVC);
                System.out.println(" ***********************************");
                System.out.println("CaptionXlet  source0 " + sourceID[0]);
                System.out.println("CaptionXlet  source1 " + sourceID[1]);
                System.out.println("CaptionXlet  Log Services = " + log_supported_svc);
                System.out.println(" ***********************************");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        video.setLogParameter(log_supported_svc);

    }

    /**
     * To help validate a CTP test. Triggered via the play key.
     */
    private void iterateInstanceOf()
    {
        ccAttrib = ClosedCaptioningAttribute.getInstance();
        if (ccAttrib != null)
        {

            // ----------------------------------------------------------------------------------
            analogFgColors = ccAttrib.getCCCapability(ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR,
                    ClosedCaptioningAttribute.CC_TYPE_ANALOG);
            // Walk index
            System.out.println("analogFgColors length = " + analogFgColors.length);

            int ndx = 0;
            for (ndx = 0; ndx < analogFgColors.length; ndx++)
            {
                if (analogFgColors[ndx] instanceof java.awt.Color)
                {
                    System.out.println("analogFgColors[" + ndx + "] is instanceof java.awt.Color");
                }
                else
                    System.out.println("analogFgColors[" + ndx + "] is NOT instanceof java.awt.Color");
            }

            // ----------------------------------------------------------------------------------
            analogBgColors = ccAttrib.getCCCapability(ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR,
                    ClosedCaptioningAttribute.CC_TYPE_ANALOG);

            System.out.println("analogBgColors length = " + analogBgColors.length);
            for (ndx = 0; ndx < analogBgColors.length; ndx++)
            {
                if (analogBgColors[ndx] instanceof java.awt.Color)
                {
                    System.out.println("analogBgColors[" + ndx + "] is instanceof java.awt.Color");
                }
                else
                    System.out.println("analogBgColors[" + ndx + "i] is NOT instanceof java.awt.Color");
            }

            // ---------------------------------------------------------------------------------
            digitalFgColors = ccAttrib.getCCCapability(ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR,
                    ClosedCaptioningAttribute.CC_TYPE_DIGITAL);

            System.out.println("digitalFgColors length = " + digitalFgColors.length);
            for (ndx = 0; ndx < digitalFgColors.length; ndx++)
            {
                if (digitalFgColors[ndx] instanceof java.awt.Color)
                {
                    System.out.println("digitalFgColors[" + ndx + "] is instanceof java.awt.Color");
                }
                else
                    System.out.println("digitalFgColors[" + ndx + "] is NOT instanceof java.awt.Color");
            }

            // ---------------------------------------------------------------------------------

            digitalBgColors = ccAttrib.getCCCapability(ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR,
                    ClosedCaptioningAttribute.CC_TYPE_DIGITAL);

            System.out.println("digitalBgColorslength = " + digitalBgColors.length);

            for (ndx = 0; ndx < digitalBgColors.length; ndx++)
            {
                if (digitalBgColors[ndx] instanceof java.awt.Color)
                {
                    System.out.println("digitalBgColors[" + ndx + "] is instanceof java.awt.Color");
                }
                else
                    System.out.println("digitalBgColors[" + ndx + "] is NOT instanceof java.awt.Color");
            }
        }

        // ------------
        System.out.println("CaptionTestXlet 1");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_BG_COLOR, CC_TYPE_ANALOG) failed.");
            return;
        }

        System.out.println("CaptionTestXlet 2");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_BG_COLOR, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 5
        System.out.println("CaptionTestXlet 3");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_FG_COLOR, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 6
        System.out.println("CaptionTestXlet 4");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_FG_COLOR, CC_TYPE_DIGITAL) failed.");
            return;
        }

        System.out.println("CaptionTestXlet 5");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_BORDER_COLOR, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_BORDER_COLOR, CC_TYPE_ANALOG) failed.");
            return;
        }

        System.out.println("CaptionTestXlet 6");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_BORDER_COLOR, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_BORDER_COLOR, CC_TYPE_DIGITAL) failed.");
            return;
        }

        System.out.println("CaptionTestXlet 7");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_FILL_COLOR, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_FILL_COLOR, CC_TYPE_ANALOG) failed.");
            return;
        }

        System.out.println("CaptionTestXlet 8");
        if (CaptionTestXlet.validateColor(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_FILL_COLOR, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_FILL_COLOR, CC_TYPE_DIGITAL) failed.");
            return;
        }

    }

    void ClosedCaptioningAttribute110App1()
    {

        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 1");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_OPACITY, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_BG_OPACITY, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 4

        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 2");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_OPACITY, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_BG_OPACITY, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 5
        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 3");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_OPACITY, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_FG_OPACITY, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 6
        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 4");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_OPACITY, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_FG_OPACITY, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 7
        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 5");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_FILL_OPACITY, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_FILL_OPACITY, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 8

        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity 6");
        if (CaptionTestXlet.validateOpacity(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_FILL_OPACITY, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_FILL_OPACITY, CC_TYPE_DIGITAL) failed.");
            return;
        }

        System.out.println("ClosedCaptioningAttribute110App1 validateOpacity Done");
    }

    void ClosedCaptionAttribute150App()
    {
        System.out.println("ClosedCaptionAttribute150App validateString 1");
        if (CaptionTestXlet.validateString(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_STYLE, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_STYLE, CC_TYPE_ANALOG) failed.");
            return;
        }

        // //4
        System.out.println("ClosedCaptionAttribute150App validateString 2");
        if (CaptionTestXlet.validateString(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_STYLE, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_STYLE, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // //5
        System.out.println("ClosedCaptionAttribute150App validateString 3");
        // tx.pathCheck(5);
    }

    void ClosedCaptionAttribute140App()
    {
        System.out.println("ClosedCaptionAttribute140App validateFontProperty 1");
        if (CaptionTestXlet.validateFontProperty(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_ITALICIZED, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_ITALICIZED, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 4
        System.out.println("ClosedCaptionAttribute140App validateFontProperty 2");
        if (CaptionTestXlet.validateFontProperty(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_ITALICIZED, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_ITALICIZED, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 5

        System.out.println("ClosedCaptionAttribute140App validateFontProperty 3");
        if (CaptionTestXlet.validateFontProperty(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_UNDERLINE, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_UNDERLINE, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 6
        System.out.println("ClosedCaptionAttribute140App validateFontProperty 4");
        if (CaptionTestXlet.validateFontProperty(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_FONT_UNDERLINE, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_FONT_UNDERLINE, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 7
        System.out.println("ClosedCaptionAttribute140App validateFontProperty Done");
        // tx.patchCheck(7);
    }

    void ClosedCaptionAttribute130App()
    {

        System.out.println("ClosedCaptionAttribute130App validateBorderType 1");
        if (CaptionTestXlet.validateBorderType(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_BORDER_TYPE, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_BORDER_TYPE, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 4

        System.out.println("ClosedCaptionAttribute130App validateBorderType 2");
        if (CaptionTestXlet.validateBorderType(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_WINDOW_BORDER_TYPE, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_WINDOW_BORDER_TYPE, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 5
        System.out.println("ClosedCaptionAttribute130App validateBorderType Done");
        // tx.patchCheck(5);
    }

    void ClosedCaptioningAttribute120App1()
    {

        System.out.println("ClosedCaptioningAttribute120App1 validatePenSize 1");
        if (CaptionTestXlet.validatePenSize(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE, ClosedCaptioningAttribute.CC_TYPE_ANALOG) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_SIZE, CC_TYPE_ANALOG) failed.");
            return;
        }

        // 4

        System.out.println("ClosedCaptioningAttribute120App1 validatePenSize 2");
        if (CaptionTestXlet.validatePenSize(new Object[] { ccAttrib.getCCAttribute(
                ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE, ClosedCaptioningAttribute.CC_TYPE_DIGITAL) }) == false)
        {
            System.out.println("Verification of ClosedCaptioningControl.getCCAttribute(CC_ATTRIBUTE_PEN_SIZE, CC_TYPE_DIGITAL) failed.");
            return;
        }

        // 5
        System.out.println("ClosedCaptioningAttribute120App1 validatePenSize Done");
        // tx.patchCheck(5);
    }

    public static final boolean validateColor(Object aobj[])
    {

        if (aobj == null)
        {
            System.out.println("ccAttributeColorArray is null.");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (!(aobj[i] instanceof Color))
            {
                System.out.println("ccAttributeColorArray[" + i + "] is not an instance of java.awt.Color.");
                return false;
            }
            else
                System.out.println("object[" + i + "] ccAttributeColorArray is instanceof color");
        }
        return true;
    }

    public static final boolean validateOpacity(Object aobj[])
    {
        if (aobj == null)
        {
            System.out.println("ccAttributeOpacityArray is null.");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (aobj[i] instanceof Integer)
            {
                int j = ((Integer) aobj[i]).intValue();
                if (j != 1 && j != 0 && j != 2 && j != 3)
                {
                    System.out.println("ccAttributeOpacityArray[" + i
                            + "] does not contain correct value - one of ClosedCaptioningAttribute.CC_OPACITY_["
                            + "FLASH|SOLID|TRANSLUCENT|TRANSPARENT].");
                    return false;
                }
            }
            else
            {
                System.out.println("ccAttributeOpacityArray[" + i + "] is not an instance of java.lang.Integer.");
                return false;
            }
        }

        return true;
    }

    public static final boolean validatePenSize(Object aobj[])
    {
        if (aobj == null)
        {
            System.out.println("ccAttributePenSizeArray is null.");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (aobj[i] instanceof Integer)
            {
                int j = ((Integer) aobj[i]).intValue();
                if (j != 2 && j != 0 && j != 1)
                {
                    System.out.println("ccAttributePenSizeArray[" + i
                            + "] does not contain correct value - one of ClosedCaptioningAttribute.CC_PEN_SIZE_"
                            + "[LARGE|SMALL|STANDARD].");
                    return false;
                }
            }
            else
            {
                System.out.println("ccAttributePenSizeArray[" + i + "] is not an instance of java.lang.Integer.");
                return false;
            }
        }

        return true;
    }

    public static final boolean validateBorderType(Object aobj[])
    {
        if (aobj == null)
        {
            System.out.println("ccAttributeBorderTypeArray is null.");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (aobj[i] instanceof Integer)
            {
                int j = ((Integer) aobj[i]).intValue();
                if (j != 2 && j != 0 && j != 1 && j != 4 && j != 5 && j != 3)
                {
                    System.out.println("ccAttributeBorderTypeArray[" + i
                            + "] does not contain correct value - one of ClosedCaptioningAttribute.CC_BORDER_[D"
                            + "EPRESSED|NONE|RAISED|SHADOW_LEFT|SHADOW_RIGHT|UNIFORM].");
                    return false;
                }
            }
            else
            {
                System.out.println("ccAttributeBorderTypeArray[" + i + "] is not an instance of java.lang.Integer.");
                return false;
            }
        }

        return true;
    }

    public static final boolean validateFontProperty(Object aobj[])
    {
        if (aobj == null)
        {
            System.out.println("ccAttributeFontPropertyArray is null.");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (aobj[i] instanceof Integer)
            {
                int j = ((Integer) aobj[i]).intValue();
                if (j != 0 && j != 1)
                {
                    System.out.println("ccAttributeBorderTypeArray[" + i + "] does not contain correct value - 0 or 1.");
                    return false;
                }
            }
            else
            {
                System.out.println("ccAttributeBorderTypeArray[" + i + "] is not an instance of java.lang.Integer.");
                return false;
            }
        }
        return true;
    }

    public static final boolean validateString(Object aobj[])
    {
        if (aobj == null)
        {
            System.out.println("ccAttributeStringArray is null");
            return false;
        }

        for (int i = 0; i < aobj.length; i++)
        {
            if (!(aobj[i] instanceof String))
            {
                System.out.println("ccAttributeStringArray[" + i + "] is not an instance of java.lang.String.");
                return false;
            }
        }
        return true;
    }

    /**
     * startXlet
     */
    public void startXlet()
    {
        if (bAlreadyStarted)
        {
            scene.show();
            scene.requestFocus();
        }
        else
        {
            bAlreadyStarted = true;
            scene.show();
            scene.addKeyListener(this);
            scene.requestFocus();
            repaint();
        }
        service = null;
        try
        {
            // org.ocap.net.OcapLocator ol =
            // new org.ocap.net.OcapLocator(0x3c14dc0, -1, 0xff);

            // service =
            // javax.tv.service.SIManager.createInstance().getService(ol);

            service = javax.tv.service.SIManager.createInstance().getService(new org.ocap.net.OcapLocator(sourceID[0]));
        }
        catch (Exception e)
        {
        }

        if (service == null || (service != null && !video.start(service)))
        {
            System.out.println("could not start video");
        }

    }

    /**
     * pauseXlet
     * 
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    /**
     * destroyXlet
     */
    public void destroyXlet(boolean unconditional)
    {
        scene.setVisible(false);
        HSceneFactory.getInstance().dispose(scene);
        scene = null;
    }

    public void keyTyped(KeyEvent e)
    {
        int key = e.getKeyCode();
    }

    /**
     * keyPressed implementation of KeyListener interface all of the key related
     * processing happens here.
     * 
     * @param e
     */
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {

            case HRcEvent.VK_COLORED_KEY_3:
            { // A
                if (video.menuFunction == video.MENU_CC_SETTINGS)
                {
                    video.ccControl.setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_ON);
                    System.out.println("Closed Captioning is On");
                }
            }
                break;
            case HRcEvent.VK_COLORED_KEY_2:
            { // B
                if (video.menuFunction == video.MENU_CC_SETTINGS)
                {
                    video.ccControl.setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_ON_MUTE);
                    System.out.println("Closed Captioning is On on Mute");
                }
            }
                break;
            case HRcEvent.VK_COLORED_KEY_0:
            { // C
                if (video.menuFunction == video.MENU_CC_SETTINGS)
                {
                    video.ccControl.setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_OFF);
                    System.out.println("Closed Captioning is Off");
                }
            }
                break;

            case HRcEvent.VK_MUTE:
            {
                System.out.println("Caption xlet: Pressed Mute key");

            }
                break;
            case HRcEvent.VK_UP:
            {
                if (video.menuFunction == video.MENU_USER_SETTINGS || video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.setMenuFunction(video.MENU_CC_SETTINGS);
                }
                else if (video.menuFunction == video.MENU_USER_BGCOLOR || video.menuFunction == video.MENU_USER_FGCOLOR
                        || video.menuFunction == video.MENU_USER_FONTSIZE)
                {
                    video.setMenuFunction(video.MENU_USER_SETTINGS);
                }
            }
                break;

            case HRcEvent.VK_DOWN:
            {
            }
                break;

            case OCRcEvent.VK_INFO:
                video.setMenuFunction(video.menuFunction);
                break;
            case HRcEvent.VK_1:
                if (video.menuFunction == video.MENU_CC_SETTINGS)
                {
                    video.setMenuFunction(video.MENU_CC_SERVICES);
                }
                else if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(1, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1);
                }
                else if (video.menuFunction == video.MENU_USER_SETTINGS)
                {
                    video.setMenuFunction(video.MENU_USER_BGCOLOR);
                }
                else if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(0, 0, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(0, 0, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FONTSIZE)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE };
                    Object value[] = { new Integer(ClosedCaptioningAttribute.CC_PEN_SIZE_SMALL) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }

                break;

            case HRcEvent.VK_2:
                if (video.menuFunction == video.MENU_CC_SETTINGS)
                {
                    video.setMenuFunction(video.MENU_USER_SETTINGS);
                    scene.repaint();
                }
                else if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(2, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1);
                }
                else if (video.menuFunction == video.MENU_USER_SETTINGS)
                {
                    video.setMenuFunction(video.MENU_USER_FGCOLOR);
                }
                else if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(128, 128, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(128, 128, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FONTSIZE)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE };
                    Object value[] = { new Integer(ClosedCaptioningAttribute.CC_PEN_SIZE_STANDARD) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }

                break;

            case HRcEvent.VK_3:
                if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(3, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1);
                }
                else if (video.menuFunction == video.MENU_USER_SETTINGS)
                {
                    video.setMenuFunction(video.MENU_USER_FONTSIZE);
                }
                else if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(128, 0, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(128, 0, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FONTSIZE)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE };
                    Object value[] = { new Integer(ClosedCaptioningAttribute.CC_PEN_SIZE_LARGE) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }

                break;

            case HRcEvent.VK_4:
                if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(1, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC2);
                }
                else if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(0, 128, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(0, 128, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                break;

            case HRcEvent.VK_5:
                if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(0, 0, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(1, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC3);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(0, 0, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                break;

            case HRcEvent.VK_6:
                if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(0, 128, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_CC_SERVICES)
                {
                    video.SetCCService(1, ClosedCaptioningControl.CC_ANALOG_SERVICE_CC4);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(0, 128, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                break;

            case HRcEvent.VK_7:

                if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(128, 0, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(128, 0, 128) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                scene.repaint();
                break;

            case HRcEvent.VK_8:

                if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { new Color(128, 128, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { new Color(128, 128, 0) };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                break;

            case HRcEvent.VK_0:

                if (video.menuFunction == video.MENU_USER_BGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR };
                    Object value[] = { null };

                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_FGCOLOR)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR };
                    Object value[] = { null };
                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }
                else if (video.menuFunction == video.MENU_USER_SETTINGS)
                {
                    int[] attributes = { ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_FG_COLOR,
                            ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_BG_COLOR,
                            ClosedCaptioningAttribute.CC_ATTRIBUTE_PEN_SIZE };
                    Object value[] = { null, null, null };

                    ccAttrib.setCCAttribute(attributes, value, ClosedCaptioningAttribute.CC_TYPE_DIGITAL);
                }

                scene.repaint();
                break;

            case HRcEvent.VK_9:
            {
                video.menuOff = true;
            }
                break;
            case HRcEvent.VK_CHANNEL_UP:
            {
                channel_index++;
                if (channel_index > 1) channel_index = 0;
                try
                {
                    service = javax.tv.service.SIManager.createInstance().getService(
                            new org.ocap.net.OcapLocator(sourceID[channel_index]));
                }
                catch (Exception ex)
                {
                }

                if (service == null || (service != null && !video.start(service)))
                {
                    System.out.println("could not start video");
                }

            }
                break;
            case HRcEvent.VK_CHANNEL_DOWN:
            {
                channel_index--;
                if (channel_index < 0) channel_index = 1;
                try
                {
                    service = javax.tv.service.SIManager.createInstance().getService(
                            new org.ocap.net.OcapLocator(sourceID[channel_index]));
                }
                catch (Exception ex)
                {
                }

                if (service == null || (service != null && !video.start(service)))
                {
                    System.out.println("could not start video");
                }

            }
                break;

            case HRcEvent.VK_PLAY:
            {
                // iterateInstanceOf();
                ClosedCaptioningAttribute110App1();
                ClosedCaptioningAttribute120App1();
                ClosedCaptionAttribute130App();
                ClosedCaptionAttribute140App();
                ClosedCaptionAttribute150App();
            }
        }
        scene.repaint();
    }

    /**
     * Key Released, update and display banner nop implementations
     * 
     * @param e
     */
    public void keyReleased(KeyEvent e)
    {

    }
}
