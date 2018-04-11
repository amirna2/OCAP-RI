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

// Declare package.
package org.cablelabs.xlet.PorterDuffTest;

// Import Personal Java packages.
import java.awt.*;
import java.awt.event.*;
import javax.tv.xlet.*;
import javax.tv.util.*;
import java.util.*;

// Import OCAP packages.
import org.havi.ui.*;
import org.dvb.ui.*;
import org.ocap.ui.event.OCRcEvent;

import org.apache.log4j.Logger;


/**
 * The class PorterDuffTestXlet ...
 * 
 */
public class PorterDuffTestXlet extends HContainer implements Xlet, TVTimerWentOffListener, KeyListener
{
    private XletContext ctx;

    private boolean started = false;

    // A HAVi scene
    private HScene scene;

    private TVTimer timer;

    private DVBAlphaComposite m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, 0.5f);

    private static final Logger log = Logger.getLogger(PorterDuffTestXlet.class);

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param XletContext
     *            The context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(XletContext ctx)
    {
        this.ctx = ctx;
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet()
    {
        if (!started)
        {
            scene = HSceneFactory.getInstance().getDefaultHScene();
//            scene.setSize(640, 480);
            scene.setLayout(new BorderLayout());
            scene.setBackgroundMode(HScene.BACKGROUND_FILL);

            initMe();

            scene.add(this);
            scene.addNotify();
            scene.validate();

            scene.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    destroyXlet(true);
                    ctx.notifyDestroyed();
                }
            });

            started = true;
        }

        scene.show();
        requestFocus();
    }

    /**
     * initMe() - sets background and foreground color along with font style.
     * Prints the current date and time and repaints the screen every second
     * with updated time.
     */
    private void initMe()
    {
        scene.setBackground((new Color(0, 234, 0)).darker());
//        scene.setBackground((new Color(0, 0, 234)).darker());
        setForeground(new Color(234, 234, 234).darker());
        setFont(new Font("SansSerif", 0, 36));

        // Register a key listener
        addKeyListener(this);          


        try
        {
            timer = TVTimer.getTimer();
            TVTimerSpec spec = new TVTimerSpec();
            spec.addTVTimerWentOffListener(this);
            spec.setDelayTime(1000);
            spec.setRepeat(true);
            spec = timer.scheduleTimerSpec(spec);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void timerWentOff(TVTimerWentOffEvent e)
    {
        if (scene.isVisible()) repaint();
    }

  //-------------------------------------------------------------------------
    public void keyTyped(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyReleased(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        // Do nothing
    }

  //-------------------------------------------------------------------------
    public void keyPressed(KeyEvent e)
  //-------------------------------------------------------------------------
    {
        switch (e.getKeyCode())
        {
        	case OCRcEvent.VK_1: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.CLEAR");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.Clear;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.CLEAR, 0.5f);
                break;

        	case OCRcEvent.VK_2: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.SRC");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.Src;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC, 0.5f);
                break;

        	case OCRcEvent.VK_3: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.SRC_OVER");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.SrcOver;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, 0.5f);
                break;
            
        	case OCRcEvent.VK_4: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.DST_OVER");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.DstOver;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.DST_OVER, 0.5f);
                break;
            
        	case OCRcEvent.VK_5: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.SRC_IN");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.SrcIn;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_IN, 0.5f);
                break;
            
        	case OCRcEvent.VK_6: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.DST_IN");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.DstIn;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.DST_IN, 0.5f);
                break;
            
        	case OCRcEvent.VK_7: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.SRC_OUT");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.SrcOut;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OUT, 0.5f);
                break;
            
        	case OCRcEvent.VK_8: 
                if (log.isInfoEnabled())
                {
                    log.info("OCRcEvent.VK_1: DVBAlphaComposite.DST_OUT");
                }
//                m_nPorterDuffRule = DVBAlphaComposite.DstOut;
                m_nPorterDuffRule = DVBAlphaComposite.getInstance(DVBAlphaComposite.DST_OUT, 0.5f);
                break;
            
            default:
        }     
        
        repaint();
    }

    /**
     * Paint all.
     * 
     * @param g
     *            The <code>Graphics</code> context.
     */
    public void paint(Graphics g)
    {
        scene.setBackground((new Color(0, 234, 0)).darker());
        setForeground(new Color(234, 234, 234).darker());
        setFont(new Font("SansSerif", 0, 36));

        Dimension size = getSize();

        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        String str = "TEST" + hour + ":" + minute + ":" + second;

        FontMetrics fm = g.getFontMetrics();
        int x = (size.width - fm.stringWidth(str)) / 2;
        int y = (size.height + fm.getAscent()) / 2;

        g.setColor(getForeground());
        g.drawString(str, x, y);

        // m_nPorterDuffRule
        DVBGraphics dvgG = (DVBGraphics) g;
                
        if (log.isInfoEnabled())
        {
            log.info("before getDVBComposite () = " + dvgG.getDVBComposite ());
        }

        try
        {
            dvgG.setDVBComposite (m_nPorterDuffRule);      
        }
        catch (UnsupportedDrawingOperationException ex)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error calling setDVBComposite", ex);
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("after getDVBComposite () = " + dvgG.getDVBComposite ());
        }

        g.setColor(new Color(0, 0, 234));
        g.fillRect(100, 100, 200, 200);
        g.setColor(new Color(234, 0, 0));
        g.fillRect(150, 150, 200, 200);
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean x)
    {
        scene.setVisible(false);
        HSceneFactory.getInstance().dispose(scene);
    }
}
