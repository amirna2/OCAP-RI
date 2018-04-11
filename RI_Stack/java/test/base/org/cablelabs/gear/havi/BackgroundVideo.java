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

package org.cablelabs.gear.havi;

// AWT imports
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentAdapter;

// HAVi imports
import org.havi.ui.HComponent;

// JMF imports
import javax.media.Player;

// JavaTV imports
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.graphics.AlphaColor;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContextDestroyedEvent;

/**
 * <code>BackgroundVideo</code> is a simple component which simplifies the
 * sizing and positioning of JavaTV background video using the
 * <code>AWTVideoSizeControl</code>. An instance of <code>BackgroundVideo</code>
 * sizes the background video <code>Player</code> using its
 * <code>AWTVideoSizeControl</code>, if available, and ensures that it can be
 * seen.
 * 
 * <p>
 * 
 * A simple way of using a <code>BackgroundVideo</code> in an <code>Xlet</code>
 * would be to:
 * 
 * <ol>
 * 
 * <li>In <code>initXlet</code> create the <code>BackgroundVideo</code>:
 * 
 * <pre>
 * BackgroundVideo bg = new BackgroundVideo();
 * bg.setBounds(100, 100, 128, 96);
 * ...
 * panel.add(bg);
 * </pre>
 * 
 * <li>In <code>startXlet</code>, call {@link #start()}:
 * 
 * <pre>
 * hscene.show();
 * bg.start();
 * </pre>
 * 
 * <li>In <code>pauseXlet</code> and <code>destroyXlet</code>, call
 * {@link #stop()}:
 * 
 * <pre>
 * bg.stop();
 * hscene.setVisible(false);
 * </pre>
 * 
 * </ol>
 * 
 * <p>
 * Note that only one instance of BackgroundVideo should be in use at any one
 * time (although, it is safe to have more than one allocated instance).
 * Actively using more than one can produce unexpected results.
 * 
 * @author Aaron Kamienski
 * @version $Id: BackgroundVideo.java,v 1.2 2002/06/03 21:33:15 aaronk Exp $
 */
public class BackgroundVideo extends HComponent
{
    /**
     * Create a new instance of <code>BackgroundVideo</code>.
     */
    public BackgroundVideo()
    {
        addComponentListener(listener);
    }

    /**
     * Notifies this <code>BackgroundVideo</code> component that it should
     * resize the background video service so that it can be displayed through
     * this <code>BackgroundVideo</code>.
     * 
     * <p>
     * 
     * This method essentially does the following (if any step fails,
     * <code>start()</code> will fail silently):
     * 
     * <ol>
     * <li>Does the equivalent of <code>stop()</code>.
     * <li>Acquires the application's current
     * {@link javax.tv.service.selection.ServiceContext}.
     * <li>Acquires the {@link javax.tv.service.selection.ServiceContentHandler
     * ServiceContentHandlers}.
     * <li>From the list of <code>ServiceContentHandler</code>s the
     * <code>Player</code> that controls the video stream is found.
     * <li>An {@link javax.tv.media.AWTVideoSizeControl} is acquired from the
     * <code>Player</code> and used to scale and reposition the playback to the
     * bounds of this <code>BackgroundVideo</code>.
     * </ol>
     * 
     * If the <code>ServiceContext</code> presentation is terminated, then the
     * equivalent of <code>stop</code> will be invoked. If this
     * <code>BackgroundVideo</code> component is moved or resized, then the
     * video playback will be moved or resized accordingly.
     * 
     * @see #stop()
     */
    public void start()
    {
        // stop if previously active
        stop();

        // Get the ServiceContext
        ServiceContext sc;
        if ((sc = (ServiceContext) getServiceContext()) != null)
        {
            // Add a ServiceContextListener to watch for service
            // terminated/destroyed events.
            sc.addListener(new ServiceContextListener()
            {
                public void receiveServiceContextEvent(ServiceContextEvent e)
                {
                    if (e instanceof PresentationTerminatedEvent || e instanceof ServiceContextDestroyedEvent)
                    {
                        if (DEBUG) System.out.println(e);

                        // "stop" sizing
                        stop();
                        // remove self from serviceContext listener set
                        e.getServiceContext().removeListener(this);
                    }
                }
            });

            // Get the ServiceContentHandlers
            ServiceContentHandler[] sch = sc.getServiceContentHandlers();

            // Find THE Player
            for (int i = 0; i < sch.length; ++i)
            {
                if (sch[i] instanceof Player)
                {
                    // Get the AWTVideoSizeControl
                    // Save the control
                    control = (AWTVideoSizeControl) ((Player) sch[i]).getControl(CONTROL);
                    if (DEBUG) System.out.println("Saving control: " + control);

                    // Size the video for the first time
                    if (control != null)
                    {
                        savedSize = control.getSize();
                        if (DEBUG) System.out.println("Saving size: " + savedSize);

                        resizeVideo();
                        break; // Done
                    }
                }
            }
        }
    }

    /**
     * Finds the current <code>ServiceContext</code> and returns it. If one
     * cannot be found, then <code>null</code> is returned.
     * 
     * @return the current <code>ServiceContext</code> or <code>null</code> if
     *         none can be found
     */
    private Object getServiceContext()
    {
        /*
         * NOTE: this method returns an Object rather than a ServiceContext to
         * skirt around ClassLoader issues. This is to allow the BackgroundVideo
         * component to be useable and (class-loadable) without having JavaTV
         * present. Although, it is possible for a ClassLoader impl. to try and
         * load EVERY external class reference, most seem to only look at method
         * signatures...
         */

        try
        {
            ServiceContextFactory factory = ServiceContextFactory.getInstance();
            ServiceContext[] array = factory.getServiceContexts();

            if (DEBUG)
            {
                if (factory == null)
                    System.out.println("No ServiceContextFactory");
                else if (array == null)
                    System.out.println("Null ServiceContexts array");
                else
                    System.out.println(array.length + " ServiceContext(s)");
            }

            if (array != null && array.length != 0) return array[0];
        }
        catch (Exception ignored)
        {
            if (DEBUG) ignored.printStackTrace();
        }

        // Cannot get service...
        return null;
    }

    /**
     * Notifies this <code>BackgroundVideo</code> component that it should
     * resize the background video service to its original size.
     * <p>
     * This method is automatically called when this
     * <code>BackgroundVideo</code> is hidden (e.g., with <code>hide</code> or
     * <code>setVisible</code>).
     * 
     * @see #start()
     */
    public void stop()
    {
        // Forget the control
        AWTVideoSizeControl tmp = control;
        control = null;

        // Resize the video to its original size
        if (tmp != null)
        {
            if (DEBUG) System.out.println("Restoring player size: " + savedSize);

            // Should probably be the current size/loc when it was first sized!
            tmp.setSize(savedSize);
            repaint();
        }
    }

    /**
     * Resizes the background video stream such that it can be displayed within
     * this component.
     */
    private void resizeVideo()
    {
        AWTVideoSizeControl control; // local copy
        if ((control = this.control) != null)
        {
            // Get the source video bounds
            Rectangle src = new Rectangle();
            src.setSize(control.getSourceVideoSize());

            // Get the destination video bounds
            Rectangle dest = new Rectangle(getLocationOnScreen(), getSize());

            // Create new player size
            AWTVideoSize resize = new AWTVideoSize(src, dest);
            if (DEBUG) System.out.println("Attempt to size to: " + resize);

            // Get closest approximation
            resize = control.checkSize(resize);

            // Perform sizing
            if (DEBUG) System.out.println("Resize player: " + resize);
            boolean success = control.setSize(resize);

            if (DEBUG) System.out.println("Resize success? " + success);

            // Repaint to get video shown
            repaint();
        }
    }

    /**
     * Override <code>HComponent.paint</code> to show the background video (if
     * possible), else simply fill using the {@link #getBackground() background
     * color}.
     * 
     * <p>
     * 
     * Invoking the <code>start()</code> method will result in the background
     * video stream being positioned and sized to correspond with this
     * <code>BackgroundVideo</code> component. The background video stream is
     * then made visible by rendering this component with a <code>Color</code>
     * which is transparent through to the video plane.
     * 
     * @see #start()
     * @see javax.tv.graphics.AlphaColor
     */
    public void paint(Graphics g)
    {
        // Set the color to punch through to video (if we have video)
        Color bg = getBackground();
        g.setColor((control == null) ? bg : new AlphaColor(bg.getRed(), bg.getGreen(), bg.getBlue(), 0));
        if (DEBUG) System.out.println("BG Color: " + g.getColor());

        // Fill the component
        Dimension size = getSize();
        g.fillRect(0, 0, size.width, size.height);
    }

    /**
     * Sets the preferred size for this component.
     * 
     * @param size
     *            The the size to return as the preferred size.
     */
    public void setPreferredSize(Dimension size)
    {
        prefSize = size;
    }

    /**
     * Returns the preferred size for this component.
     * 
     * @return the preferred size
     */
    public Dimension getPreferredSize()
    {
        return ((prefSize != null) ? prefSize : super.getPreferredSize());
    }

    /**
     * A <code>ComponentListener</code> which ensures that this
     * <code>BackgroundVideo</code>:
     * <ul>
     * <li>is {@link #stop() stopped} when hidden
     * <li>resizes the background video service when moved/resized
     * </ul>
     */
    private ComponentListener listener = new ComponentAdapter()
    {
        public void componentMoved(ComponentEvent e)
        {
            resizeVideo();
        }

        public void componentResized(ComponentEvent e)
        {
            resizeVideo();
        }

        public void componentHidden(ComponentEvent e)
        {
            stop();
        }
    };

    /**
     * The name of the resize control.
     */
    private static final String CONTROL = "javax.tv.media.AWTVideoSizeControl";

    /**
     * The control used to resize the player.
     */
    private AWTVideoSizeControl control;

    /**
     * The saved original size of the background video player.
     */
    private AWTVideoSize savedSize;

    /**
     * The preferred size for this component.
     */
    private Dimension prefSize = new Dimension(10, 10);

    /** Debug flag. */
    private static final boolean DEBUG = false;
}
