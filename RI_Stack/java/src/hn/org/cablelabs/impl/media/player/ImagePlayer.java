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

package org.cablelabs.impl.media.player;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.Time;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

public class ImagePlayer extends AbstractPlayer
{
    private static final Logger log = Logger.getLogger(ImagePlayer.class);

    private ImageComponent imageComponent;

    protected ImagePlayer(CallerContext cc)
    {
        super(cc, new Object(), new ResourceUsageImpl(cc));
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected Presentation createPresentation()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "createPresentation");
        }
        return new NoOpPresentation();
    }

    protected Object doAcquirePrefetchResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doAcquirePrefetchResources");
        }
        // nothing to do - resources acquired in realize
        return null;
    }

    protected Object doAcquireRealizeResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doAcquireRealizeResources");
        }
        String urlString = getSource().getLocator().toExternalForm();
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "url: " + urlString);
        }
        try
        {
            URL url = new URL(urlString);
            imageComponent = new ImageComponent();
            Image image = imageComponent.getToolkit().createImage(url);
            ImageObserverImpl observer = new ImageObserverImpl();
            int height = image.getHeight(observer);
            if (height == -1)
            {
                height = observer.waitFor(ImageObserver.HEIGHT);
            }
            int width = image.getWidth(observer);
            if (width == -1)
            {
                width = observer.waitFor(ImageObserver.WIDTH);
            }
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setting image size: " + width + ", " + height);
            }

            imageComponent.setImage(image);
            imageComponent.setSize(width, height);
            return null;
        }
        catch (MalformedURLException e)
        {
            return "Unable to use URL: " + urlString;
        }
    }

    protected Component doGetVisualComponent()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getVisualComponent: " + imageComponent);
        }
        return imageComponent;
    }

    protected void doReleaseAllResources()
    {
        imageComponent = null;
    }

    protected void doReleasePrefetchedResources()
    {
    }

    protected void doReleaseRealizedResources()
    {
    }

    public boolean getMute()
    {
        return false;
    }

    public float getGain()
    {
        return 0.0F;
    }

    private class ImageComponent extends Component
    {
        Image m_image;

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public void setImage(Image img)
        {
            m_image = img;
        }

        public Image getImage()
        {
            return m_image;
        }

        public void paint(Graphics g)
        {
            if (m_image != null)
            {
                // center this image
                int x = (getBounds().width - m_image.getWidth(this)) / 2;
                int y = (getBounds().height - m_image.getHeight(this)) / 2;

                g.drawImage(m_image, x, y, this);
            }
        }
    }

    // we don't really have a presentation to present with, but abstractPlayer
    // calls presentation.start - we'll just track started state
    class NoOpPresentation implements Presentation
    {
        private boolean started;

        public void start()
        {
            started = true;
            //transition player to started
            notifyStarted();
        }

        public boolean isPresenting()
        {
            return started;
        }

        public void stop()
        {
            started = false;
        }

        public float setRate(float rate)
        {
            return Presentation.RATE_UNDEFINED;
        }

        public void setMediaTime(Time mt)
        {
            //no-op
        }

        public float getRate()
        {
            return Presentation.RATE_UNDEFINED;
        }

        public Time getMediaTime()
        {
            return DURATION_UNBOUNDED;
        }

        public void setMute(boolean mute)
        {
            //no-op
        }

        public float setGain(float gain)
        {
            return 0.0F;
        }

        public Alarm createAlarm(AlarmSpec spec, Alarm.Callback callback)
        {
            // not supported
            return null;
        }

        public void destroyAlarm(Alarm alarm)
        {
            // no-op
        }
    }

    private class ImageObserverImpl implements ImageObserver
    {
        int currentFlags;

        int height;

        int width;

		// Added for findbugs issue fix - start
        public synchronized void setCurrentFlags(int currFlags)
        {
        	this.currentFlags = currFlags;
        }
        
        public synchronized int getCurrentFlags()
        {
        	return this.currentFlags;
        }

		// Added for findbugs issue fix - end
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "imageUpdate called - flags: " + infoflags + ", width: " + width + ", height: " + height);
            }
            if (width != -1)
            {
                this.width = width;
            }
            if (height != -1)
            {
                this.height = height;
            }
            
            synchronized (this)
            {
            	// Added for findbugs issue fix - start
            	// Moved this into the synchronization block from outside
            	setCurrentFlags(infoflags);
            	// Added for findbugs issue fix - end
                notify();
            }
            return ((infoflags & ImageObserver.ALLBITS) != 0);
        }

        public int waitFor(int flag)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "waitFor: " + flag);
            }
			// Added for findbugs issue fix.
			// Added a getter for the boolean flag
            while ((getCurrentFlags() & flag) == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "flags not yet set - waiting");
                }
                synchronized (this)
                {
                    try
                    {
                        wait(50);
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                }
            }
            if (flag == ImageObserver.HEIGHT)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "returning height: " + height);
                }
                return height;
            }
            if (flag == ImageObserver.WIDTH)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "returning width: " + width);
                }
                return width;
            }
            // we don't care about a request for a field other than height or
            // width, return zero
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "unknown - returning zero");
            }
            return 0;
        }
    }
}
