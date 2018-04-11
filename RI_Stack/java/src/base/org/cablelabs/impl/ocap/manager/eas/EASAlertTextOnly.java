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

package org.cablelabs.impl.ocap.manager.eas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.ocap.util.CountDownLatch;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBGraphics;
import org.ocap.system.EASEvent;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A concrete instance of {@link EASAlert} that represents the text-only alert
 * strategy. This strategy scrolls the formatted alert text along the top edge
 * of the screen from right to left. Other than the EAS graphics plane, no other
 * resources are required to present the alert. The entire text is guaranteed to
 * be scrolled at least once unless the presentation is forcibly terminated.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASAlertTextOnly extends EASAlert
{
    /**
     * An inner class that extends {@link Component} to scroll alert text across
     * the top of the screen.
     */
    private class ScrollingText extends Component implements ComponentListener, TVTimerWentOffListener
    {
        private static final long serialVersionUID = 7199756435294099579L;

        private static final int SCROLL_RATE = 200; // the repaint() period, in
                                                    // milliseconds

        private String m_alertText; // the filtered alert text to scroll

        private int m_alertWidth; // the filtered alert text width, in pixels

        private int m_marqueeHeight; // the height of the scrolling text band
                                     // (marquee)

        private int m_marqueeWidth; // the width of the scrolling text band
                                    // (marquee)

        private int m_scrollIncrement; // number of pixels to shift left

        private int m_scrollPosition = 0; // subtracted from component width for
                                          // new X position

        private TVTimer m_scrollTimer;

        private TVTimerSpec m_scrollTimerSpec;

        private CountDownLatch m_shownFully; // blocks stop() until text shown
                                             // in its entirety

        private boolean m_updateScroll = false; // true is the scroll position
                                                // should be updated

        /**
         * Constructs a new instance of the receiver.
         */
        public ScrollingText()
        {
            setVisible(false); // don't display until started
            setLocation(0, 25); // safe area

            this.m_marqueeHeight = EASAlertTextOnly.this.m_easPlane.getHeight();
            this.m_marqueeWidth = EASAlertTextOnly.this.m_easPlane.getWidth();

            this.m_scrollTimer = TVTimer.getTimer();
            this.m_scrollTimerSpec = new TVTimerSpec();
            this.m_scrollTimerSpec.addTVTimerWentOffListener(this);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejava.awt.event.ComponentListener#componentHidden(java.awt.event.
         * ComponentEvent)
         */
        public void componentHidden(ComponentEvent e)
        {
            // intentionally empty -- uninteresting event
        }

        /*
         * (non-Javadoc)
         * 
         * @seejava.awt.event.ComponentListener#componentMoved(java.awt.event.
         * ComponentEvent)
         */
        public void componentMoved(ComponentEvent e)
        {
            // intentionally empty -- uninteresting event
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ComponentListener#componentResized(java.awt.event.
         * ComponentEvent)
         */
        public void componentResized(ComponentEvent e)
        {
            // Synchronized in case of race condition with start().
            synchronized (this)
            {
                final int resizedWidth = e.getComponent().getWidth();
                if (log.isInfoEnabled())
                {
                    log.info("EAS graphics plane width resized from:<" + this.m_marqueeWidth + "> to:<" + resizedWidth
                                + ">");
                }

                if (resizedWidth != this.m_marqueeWidth)
                {
                    if (resizedWidth < this.m_marqueeWidth)
                    {
                        this.m_scrollPosition = resizedWidth - (this.m_marqueeWidth - this.m_scrollPosition);
                    }

                    this.m_marqueeWidth = resizedWidth;
                    setSize(this.m_marqueeWidth, this.m_marqueeHeight);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seejava.awt.event.ComponentListener#componentShown(java.awt.event.
         * ComponentEvent)
         */
        public void componentShown(ComponentEvent e)
        {
            // intentionally empty -- uninteresting event
        }

        /**
         * Renders the scroll and adjusts the next scroll position if necessary.
         * 
         * @param g
         *            the graphics context to use for painting
         */
        public void paint(Graphics g)
        {
            // Make sure composite mode is SRC. // TODO: could this be pushed
            // back into the EAS plane?
			// Added for findbugs issues fix - start
        	int l_alertWidth;
        	int l_scrollIncrement;
        	int l_scrollPosition;
        	synchronized(this)
        	{
        		l_alertWidth = this.m_alertWidth;
        		l_scrollIncrement = this.m_scrollIncrement;
        		l_scrollPosition = this.m_scrollPosition;
        	}
			// Added for findbugs issues fix - end
            try
            {
                ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }

            // Get this component's current size.
            Dimension size = getSize();

            // Draw background.
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);

            // Draw alert text.
            g.setFont(getFont());
            g.setColor(getForeground());
			// Added for findbugs issues fix - start
            int x = size.width - l_scrollPosition;
			// Added for findbugs issues fix - end
            int y = (size.height + g.getFontMetrics().getAscent()) / 2;
            g.drawString(this.m_alertText, x, y);

            // Shift alert text to the left if the scroll timer went off. This
            // is a guard condition to avoid scrolling
            // unnecessarily on system-triggered painting operations (e.g.
            // initially made visible, resizing).
            if (updateScroll())
            {
				// Added for findbugs issues fix - start
                if (size.width >= (l_scrollPosition - l_alertWidth))
                {
                	synchronized(this)
                	{
                    	this.m_scrollPosition += l_scrollIncrement;
                	}
                }
				// Added for findbugs issues fix - end
                else
                {
                    this.m_shownFully.countDown();
					// Added for findbugs issues fix - start
                    synchronized(this)
                	{
                    this.m_scrollPosition = 0;
                	}
					// Added for findbugs issues fix - end
                }
            }
        }

        /**
         * Starts scrolling the formatted text across the top of screen, from
         * right to left. Getting the text is deferred as late as possible just
         * in case the user-preferred language codes or EAS text attributes
         * changed. The alert text may be an empty string, but it will not be
         * null at this point.
         */
        public void start()
        {
            this.m_alertText = filter(EASAlertTextOnly.this.m_easMessage.getFormattedTextAlert(EASAlertTextOnly.this.m_preferredLanguages));
            this.m_shownFully = new CountDownLatch(1); // need only show alert
                                                       // text once in its
                                                       // entirety

            // Don't scroll empty alert text.
            if (0 == this.m_alertText.length())
            {
                this.m_shownFully.countDown();
                return;
            }

            // Determine and set the desired dimensions of the component, in
            // pixels.
            // Synchronized in case of race condition with componentResized()
            // event.
            synchronized (this)
            {
                FontMetrics fm = getFontMetrics(getFont()); // width, height,
                                                            // increment
                                                            // relative to font
                                                            // face/size
                this.m_alertWidth = fm.stringWidth(this.m_alertText); // alert
                                                                      // text
                                                                      // width
                                                                      // (can be
                                                                      // >
                                                                      // component
                                                                      // width)
                this.m_marqueeHeight = fm.getHeight() * 4 / 3; // background
                                                               // height
                                                               // slightly
                                                               // taller than
                                                               // text height
                this.m_scrollIncrement = fm.stringWidth("W"); // number of
                                                              // pixels to shift
                                                              // text left on
                                                              // each repaint()
                this.m_scrollPosition = 0; // initial position is 0 pixels from
                                           // right
                setSize(this.m_marqueeWidth, this.m_marqueeHeight);
            }

            // Make this component visible.
            displayAlert(true);
            setVisible(true);

            try
            {
                this.m_scrollTimer.deschedule(this.m_scrollTimerSpec);
                this.m_scrollTimerSpec.setDelayTime(ScrollingText.SCROLL_RATE);
                this.m_scrollTimerSpec.setRegular(true);
                this.m_scrollTimerSpec.setRepeat(true);
                this.m_scrollTimerSpec = this.m_scrollTimer.scheduleTimerSpec(this.m_scrollTimerSpec);
                // this.m_scrollTimer.scheduleTimerSpec(this.m_scrollTimerSpec);
                // // TODO: temporary work-around for OCORI-537
                if (log.isInfoEnabled())
                {
                    log.info("Started text scroll: " + this.m_alertText);
                }
            }
            catch (TVTimerScheduleFailedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to schedule scrolling text timer - " + e.getMessage());
                }
                SystemEventUtil.logRecoverableError("Failed to schedule scrolling text timer - ", e);
            }
        }

        /**
         * Stops scrolling and makes the component non-visible. If the entire
         * alert text hasn't been shown at least once, then wait until that time
         * before stopping. However, the scrolling is unconditionally stopped if
         * <code>force</code> is <code>true</code>.
         * 
         * @param force
         *            <code>true</code> if scrolling should be stopped, even if
         *            the entire alert text has not yet been displayed
         */
        public void stop(final boolean force)
        {
            // Ensure alert text was fully scrolled at least once assuming
            // scrolling was started.
            if (!force && null != this.m_shownFully)
            {
                try
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Waiting for initial text scroll to complete:<" + this.m_shownFully.getCount() + ">");
                    }
                    this.m_shownFully.await();
                }
                catch (InterruptedException e)
                {
                    // intentionally do nothing -- interpret interrupt as a
                    // forced stop
                }
            }

            // Stop scrolling if filtered alert text is a non-empty string and
            // scrolling was started.
            if (null != this.m_alertText && 0 != this.m_alertText.length())
            {
                if (log.isInfoEnabled())
                {
                    log.info("Stopping text scroll");
                }
                this.m_scrollTimer.deschedule(this.m_scrollTimerSpec);
                setVisible(false);
                repaint();
                displayAlert(false);
            }

            this.m_shownFully = null;
            this.m_alertText = null;
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.util.TVTimerWentOffListener#timerWentOff(javax.tv.util.
         * TVTimerWentOffEvent)
         */
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            updateScroll(true);
            repaint();
        }

        /**
         * Replaces each occurrence of the following display control characters
         * with four spaces since they don't display well on a HAVi screen (e.g.
         * they appear as a square): backspace ('\b'), horizontal tab ('\t'),
         * newline ('\n'), vertical tab ('\013'), form feed ('\f'), and carriage
         * return ('\r').
         * <p>
         * Originally reported and fixed in Bugzilla issue #3324, the number of
         * substituted spaces is reduced from 8 to 4, 6 characters are scanned
         * for instead of 3, and the logic is changed to make only a single pass
         * across the alert text string instead of 2 to 4 passes.
         * <p>
         * Must be done the hard way since Java 1.4 <code>java.util.regex</code>
         * package and corresponding <code>String.replaceAll()</code> method are
         * not available in PBP 1.1.
         * 
         * @param text
         *            the alert text to be filtered
         * @return the filtered alert text
         */
        private String filter(String text)
        {
            char[] chars = text.toCharArray();
            StringBuffer sb = new StringBuffer(chars.length);

            for (int i = 0; i < chars.length; ++i)
            {
                if ("\b\t\n\013\f\r".indexOf(chars[i]) >= 0)
                {
                    sb.append("    ");
                }
                else
                {
                    sb.append(chars[i]);
                }
            }

            return sb.toString();
        }

        /**
         * Returns whether the scroll position should be incremented or not.
         * 
         * @return <code>true</code> if the scrolling position should be
         *         updated; otherwise <code>false</code>
         */
        private synchronized boolean updateScroll()
        {
            boolean updateScroll = this.m_updateScroll;
            this.m_updateScroll = false;
            return updateScroll;
        }

        /**
         * Sets the update scroll flag to the given boolean value.
         * 
         * @param update
         *            <code>true</code> if the scroll position should be
         *            updated, otherwise <code>false</code>
         */
        private synchronized void updateScroll(final boolean update)
        {
            this.m_updateScroll = update;
        }
    }

    // Class Fields

    // Class Fields

    // Class Fields

    private static final Logger log = Logger.getLogger(EASAlertTextOnly.class.getName());

    // Instance Fields

    private final Container m_easPlane;

    private String[] m_preferredLanguages = EASAlertTextFactory.DEFAULT_USER_LANGUAGE;

    private ScrollingText m_scrollingText;

    // Constructors

    /**
     * Constructs a new instance of the text-only alert strategy with the given
     * parameters.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    EASAlertTextOnly(final EASState state, final EASMessage message)
    {
        super(state, message);
        this.m_easPlane = super.m_graphicsManager.getEmergencyAlertPlane();
        this.m_scrollingText = new ScrollingText();
    }

    // Instance methods

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#getEventReason()
     */
    public int getReason()
    {
        return EASEvent.EAS_TEXT_DISPLAY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#processAlert()
     */
    public void processAlert()
    {
        if (log.isInfoEnabled())
        {
            log.info("Processing text alert...");
        }

        // Start alert processing.
        super.presentationStarted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#startPresentation()
     */
    public void startPresentation()
    {
        if (log.isInfoEnabled())
        {
            log.info("Starting presentation...");
        }

        // Add to scrolling text component to EAS plane.
        this.m_easPlane.setVisible(false);
        this.m_easPlane.removeAll();
        this.m_easPlane.add(this.m_scrollingText);
        this.m_easPlane.addComponentListener(this.m_scrollingText);

        // Show EAS plane and start scrolling alert text.
        this.m_easPlane.setVisible(true);
        this.m_scrollingText.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASAlert#stopPresentation(boolean)
     */
    public void stopPresentation(final boolean force)
    {
        if (log.isInfoEnabled())
        {
            log.info("stopPresentation - force: " + force);
        }

        // Stop scrolling text.
        this.m_scrollingText.stop(force);

        // Hide EAS plane and remove scrolling text component.
        this.m_easPlane.setVisible(false);
        this.m_easPlane.removeComponentListener(this.m_scrollingText);
        this.m_easPlane.remove(this.m_scrollingText);

        // Complete alert processing.
        presentationTerminated();
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        return "EASAlertTextOnly";
    }

    /**
     * Updates the text attributes of the alert message.
     * 
     * @param font
     *            the requested font (face, style, size)
     * @param fontColor
     *            the requested font (foreground) color and opacity
     * @param backgroundColor
     *            the requested background color and opacity
     */
    void updateAttributes(final Font font, final Color fontColor, final Color backgroundColor)
    {
        this.m_scrollingText.setFont(font);
        this.m_scrollingText.setForeground(fontColor);
        this.m_scrollingText.setBackground(backgroundColor);
        // TODO: Force restart of text scroll in new attributes if currently
        // presenting? Allow attributes to change if presenting?
    }

    /**
     * Updates the user-preferred language settings for the alert message.
     * 
     * @param languageCodes
     *            the list of ISO-639-2 language codes, in descending order of
     *            user preference
     */
    void updatePreferredLanguages(final String[] languageCodes)
    {
        this.m_preferredLanguages = (String[]) languageCodes.clone();
        // TODO: Force restart of text scroll in new language if currently
        // presenting? Allow language to change if presenting?
    }
}
