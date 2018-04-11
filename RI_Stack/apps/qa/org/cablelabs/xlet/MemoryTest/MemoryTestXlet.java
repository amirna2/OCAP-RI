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
package org.cablelabs.xlet.MemoryTest;

// Import Personal Java packages.
import java.awt.*;
import java.awt.event.*;
import javax.tv.xlet.*;
import javax.tv.util.*;
import java.util.*;

// Import OCAP packages.
import org.havi.ui.*;
import org.dvb.ui.*;

import org.cablelabs.debug.Memory;

//import org.cablelabs.xlet.MemoryTest.TextDisplay;

/**
 * The class MemoryTestXlet prints information about memory usage each time it
 * it made "active" (started or unpaused). If it still has state from a previous
 * display, it will compute the differences since then and display them.
 * 
 * @version 16 May 2005
 * @author Vidiom Systems Corp.
 */
public class MemoryTestXlet extends HContainer implements Xlet
{
    private XletContext ctx;

    private boolean started = false;

    private Memory stats; // current set of stats

    private boolean statsShown; // stats ever been shown?

    private Memory prevStats; // last set of stats

    // A HAVi scene
    private HScene scene;

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
            scene.setSize(640, 480);
            scene.setLayout(new BorderLayout());
            scene.setBackgroundMode(HScene.BACKGROUND_FILL);

            scene.setBackground((new Color(0, 0, 234)).darker());
            setForeground(new Color(234, 234, 234).darker());
            setFont(new Font("Monospaced", 0, 20));

            addKeyListener(new KeyAdapter()
            {
                public void keyPressed(KeyEvent e)
                {
                    if (scene.isVisible()) repaint();
                }
            });

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

        refreshStats();

        scene.show();
        requestFocus();
    }

    /**
     * Get a new set of memory stats if we have already shown the ones we
     * currently have. We save one set of stats for showing the differences
     * since the last display.
     */
    private void refreshStats()
    {
        if (stats == null)
        {
            stats = new Memory();
            statsShown = false;
        }
        else if (statsShown)
        {
            if (prevStats != null) prevStats.dispose();
            prevStats = stats;
            stats = new Memory();
        }
        /* otherwise, nothing to do */
    }

    private static final int X_HEADING = 5; // left indent amount for headings

    private static final int X_INDENT = 20; // left indent amount for stats

    private static final int X_COLS = 3; // number of stats columns

    private static final int Y_GAP = 0; // getAscent() + getDescent() + Y_GAP is
                                        // "line"

    /**
     * Paint the statistics on the screen and console.
     * 
     * @param g
     *            The <code>Graphics</code> context.
     */
    public void paint(Graphics g)
    {
        if (g instanceof DVBGraphics)
        {
            try
            {
                ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        int colors = stats.getNumColors();
        int c;

        TextDisplay td = new TextDisplay(g, getSize());

        if (stats == null || colors == 0)
        {
            td.displayHeading("     **** Memory Statistics Not Available ****");
            return;
        }

        statsShown = true;

        td.setupTable(X_HEADING, X_COLS, X_INDENT, Y_GAP);

        td.displayHeading("Current Allocations: Total = " + commaSeparate(stats.totalAllocated()));

        for (c = 0; c < colors; ++c)
        {
            td.displayCell(stats.getName(c) + " = " + commaSeparate(stats.getAllocated(c)));
        }
        td.newline();

        td.displayHeading("Max Allocations:");
        for (c = 0; c < colors; ++c)
        {
            if (stats.getMaxAllocated(c) != 0)
            {
                td.displayCell(stats.getName(c) + " = " + commaSeparate(stats.getMaxAllocated(c)));
            }
        }
        td.newline();

        if (prevStats != null)
        {
            int[] diff = stats.subtractAllocated(prevStats);

            td.displayHeading("Differences Since Previous Display:");

            for (c = 0; c < colors; ++c)
            {
                if (diff[c] != 0)
                {
                    td.displayCell(stats.getName(c) + " = " + commaSeparate(diff[c]));
                }
            }
        }
    }

    /**
     * Return a string with the comma-separated integer representation for val.
     * Handles both positive and negative integers.
     * 
     * @param val
     *            value to convert to comma-separated string
     * 
     * @return Comma-separated String value for the integer
     */
    private String commaSeparate(int val)
    {
        StringBuffer buf = new StringBuffer(20);
        int mask = 1000000000; // -2,147,483,648 to 2,147,483,647
        int digits = 0;

        if (val < 0)
        {
            // not expressable as a positive 32-bit 2's complement integer
            if (val == -2147483648) return "-2,147,483,648";
            buf.append('-');
            val = -val;
        }

        while (mask != 0)
        {
            if (val >= mask || digits != 0)
            {
                int sub = val / mask;

                buf.append((char) ('0' + sub));

                val -= sub * mask;

                ++digits;
            }

            if (digits != 0 && (mask == 1000000000 || mask == 1000000 || mask == 1000))
            {
                buf.append(',');
            }

            mask /= 10;
        }

        if (digits == 0) return "0";

        return buf.toString();
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

        if (stats != null) stats.dispose();

        if (prevStats != null) prevStats.dispose();

        stats = prevStats = null;
    }
}
