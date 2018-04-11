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

package org.cablelabs.xlet.hsampler;

import java.awt.*;
import java.awt.event.*;
import org.dvb.ui.*;
import org.havi.ui.*;
import org.ocap.ui.event.OCRcEvent;

/**
 * Porter-Duff Sampler demo program.
 */
public class PDSampler extends HContainer
{
    private HScene scene;

    public PDSampler(String args[])
    {
        setLayout(null);
        setSize(640, 480);

        // Create HScene
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setLayout(null);
        scene.setSize(640, 480); // if not already
        scene.add(this);

        setup();

        addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) repaint();
            }
        });
    }

    /**
     * Show the scene.
     */
    void showIt()
    {
        if (scene.isVisible())
            scene.repaint();
        else
            scene.show();
        requestFocus();
    }

    /**
     * Hide the scene.
     */
    void hideIt()
    {
        scene.setVisible(false);
    }

    /**
     * Hide and dispose the scene.
     */
    void disposeIt()
    {
        hideIt();
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /**
     * Performs extendend initialization.
     */
    private void setup()
    {
        /*
         * PDSampler +-- Padding (south,east,west,north) +-- Main (center) +--
         * CLEAR +-- DST_IN +-- DST_OUT +-- DST_OVER +-- SRC +-- SRC_IN +--
         * SRC_OUT +-- SRC_OVER +-- XOR
         */

        setLayout(new BorderLayout());

        Padding east = new Padding();
        Padding west = new Padding();
        Padding south = new Padding();
        Padding north = new Padding();

        add(east, BorderLayout.EAST);
        add(west, BorderLayout.WEST);
        add(south, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        Container main = new HContainer();
        main.setLayout(new GridLayout(3, 3));
        add(main);

        main.add(new PDComponent(DVBAlphaComposite.Clear));
        main.add(new PDComponent(DVBAlphaComposite.DstIn));
        main.add(new PDComponent(DVBAlphaComposite.DstOut));
        main.add(new PDComponent(DVBAlphaComposite.DstOver));
        main.add(new PDComponent(DVBAlphaComposite.Src));
        main.add(new PDComponent(DVBAlphaComposite.SrcIn));
        main.add(new PDComponent(DVBAlphaComposite.SrcOut));
        main.add(new PDComponent(DVBAlphaComposite.SrcOver));
        main.add(new PDComponent(Color.black));

        validate();
    }

    class Padding extends HComponent
    {
        public Dimension getPreferredSize()
        {
            return new Dimension(25, 25);
        }
    }

    private static final Color rect = new DVBColor(0x80, 0x80, 0x80, (int) (0.72F * 0xFF));

    private static final Color circle = new DVBColor(0x80, 0, 0, (int) (0.54F * 0xFF));

    private static final Color text = Color.yellow;

    class PDComponent extends HComponent
    {
        private DVBAlphaComposite mode;

        private Color xor;

        public PDComponent(DVBAlphaComposite mode)
        {
            this.mode = mode;
        }

        public PDComponent(Color xor)
        {
            this.xor = xor;
        }

        private void fail(Graphics g)
        {
            Dimension size = getSize();
            FontMetrics metrics = g.getFontMetrics();
            String str = "FAIL - " + toString();
            int x = (size.width - metrics.stringWidth(str)) / 2;
            int y = (size.height - metrics.getHeight()) / 2;
            g.setColor(Color.red);
            g.drawString(str, x, y);
        }

        public void paint(Graphics g)
        {
            Dimension size = getSize();

            // Draw outline
            g.setColor(Color.lightGray);
            g.drawRect(2, 2, size.width - 5, size.height - 5);
            g.drawRect(3, 3, size.width - 7, size.height - 7);
            g.drawRect(4, 4, size.width - 9, size.height - 9);

            if (!(g instanceof DVBGraphics))
            {
                fail(g);
                return;
            }

            DVBGraphics dvb = (DVBGraphics) g;

            // First draw translucent rectangle w/ 0.72*255
            try
            {
                dvb.setDVBComposite(DVBAlphaComposite.Src);
                dvb.setColor(rect);
                dvb.fillRect(size.width / 8, size.height / 8, size.width / 2, size.height / 2);

                // Then draw translucent circle
                if (mode != null)
                    dvb.setDVBComposite(mode);
                else if (xor != null) dvb.setXORMode(xor);
                dvb.setColor(circle);
                dvb.fillOval(size.width / 3, size.height / 3, size.width / 3, size.width / 3);

                // Then draw mode on the bottom
                FontMetrics metrics = dvb.getFontMetrics();
                int x = 4;
                int y = size.height - metrics.getDescent() - 4;
                dvb.setDVBComposite(DVBAlphaComposite.Src);
                dvb.setColor(text);
                dvb.drawString("" + this, x, y);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(g);
            }
        }

        public String toString()
        {
            if (xor != null)
                return "XOR";
            else if (mode != null)
            {
                switch (mode.getRule())
                {
                    case DVBAlphaComposite.SRC:
                        return "SRC";
                    case DVBAlphaComposite.SRC_OVER:
                        return "SRC_OVER";
                    case DVBAlphaComposite.SRC_IN:
                        return "SRC_IN";
                    case DVBAlphaComposite.SRC_OUT:
                        return "SRC_OUT";
                    case DVBAlphaComposite.DST_IN:
                        return "DST_IN";
                    case DVBAlphaComposite.DST_OUT:
                        return "DST_OUT";
                    case DVBAlphaComposite.DST_OVER:
                        return "DST_OVER";
                    case DVBAlphaComposite.CLEAR:
                        return "CLEAR";
                    default:
                        return "???";
                }
            }
            else
                return "???";
        }
    }

    /**
     * An Xlet interface for PDSampler.
     */
    public static class Xlet implements javax.tv.xlet.Xlet
    {
        private boolean started = false;

        private javax.tv.xlet.XletContext ctx;

        private PDSampler app;

        public void initXlet(javax.tv.xlet.XletContext ctx)
        {
            this.ctx = ctx;
        }

        public void startXlet()
        {
            if (!started)
            {
                started = true;

                String[] args = getArgs();
                try
                {
                    app = new PDSampler(args);
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }

                app.addKeyListener(new KeyAdapter()
                {
                    public void keyPressed(KeyEvent e)
                    {
                        if (e.getKeyCode() == OCRcEvent.VK_EXIT)
                        {
                            if (app != null) app.disposeIt();
                            app = null;
                            ctx.notifyDestroyed();
                        }
                    }
                });
            }

            if (app != null)
            {
                app.showIt();
            }
        }

        public void pauseXlet()
        {
            if (app != null) app.hideIt();
        }

        public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
        {
            if (app != null) app.disposeIt();
        }

        private String[] getArgs()
        {
            String[] params;

            params = (String[]) ctx.getXletProperty("dvb.caller.parameters");
            if (params == null) params = (String[]) ctx.getXletProperty(javax.tv.xlet.XletContext.ARGS);
            if (params == null) params = new String[0];
            return params;
        }
    }
}
