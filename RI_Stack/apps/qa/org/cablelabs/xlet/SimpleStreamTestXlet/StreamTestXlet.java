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

package org.cablelabs.xlet.SimpleStreamTestXlet;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.dsmcc.DSMCCStreamEvent;
import org.dvb.dsmcc.ServiceDomain;
import org.dvb.dsmcc.StreamEvent;
import org.dvb.dsmcc.StreamEventListener;
import org.ocap.net.OcapLocator;

import org.cablelabs.lib.utils.ArgParser;

public abstract class StreamTestXlet implements Xlet, Runnable, ServiceContextListener
{
    private static final String FREQUENCY = "frequency";

    private static final String PROG_NUM = "prog_num";

    private static final String QAM = "qam";

    private static final String CAROUSEL_ID = "carousel_id";

    private static final String WAIT_TIME = "wait_time";

    private static final String EVENT_NAME = "event_name";

    protected int frequency = 0;

    protected int prog_num = 0;

    protected int qam = 0;

    protected int carousel_id = 0;

    protected int wait_time = 30;

    protected String event_name = null;

    protected Thread runner;

    protected ServiceDomain sd;

    protected ServiceContext sc;

    protected SIManager sim;

    protected OcapLocator ocap_loc;

    private boolean started = false;

    private boolean tuned = false;

    private static final String LINE = "---------------------------------------------------------------------------------------";

    // /////////////////////////////////////////////////////////////////////////////
    // XLET METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        banner("Initializing test: " + this.getClass().getName());
        try
        {
            parseArgs(xletContext);
        }
        catch (Exception e)
        {
            printException(e);
            throw new XletStateChangeException(e.getMessage());
        }
        banner("Initialized");
        message("Frequency, Program, Qam: " + frequency + ", " + prog_num + ", " + qam);
        message("Carousel ID            : " + carousel_id);
        message("Event                  : " + event_name);
    }

    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        // Tune to a carousel and read all the files on it.
        if (!started)
        {
            runner = new Thread(this);
            runner.start();
        }
    }

    public void pauseXlet()
    {
    }

    public void destroyXlet(boolean param) throws XletStateChangeException
    {
    }

    // /////////////////////////////////////////////////////////////////////////////
    // STREAM EVENTS TEST METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * Impementation of ServiceContextListener interface notification of
     * ServiceContext event. Start reading inband carousel files, after tuning
     * is finished. This method gets called after tuning to a channel.
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        if (event == null)
        {
            return;
        }

        try
        {
            if (event instanceof NormalContentEvent)
            {
                synchronized (this)
                {
                    tuned = true;
                    this.notify();
                }
            }
        }
        catch (Exception e)
        {
            printException(e);
        }
    }

    protected void printStreamEvent(StreamEvent e)
    {
        String text;
        int length;
        byte[] data = e.getEventData();
        if (data == null)
        {
            length = 0;
            text = "(null)";
        }
        else if (data.length == 0)
        {
            length = 0;
            text = "(empty)";
        }
        else
        {
            length = data.length;
            text = new String(data);
        }

        String line = "Event Name    : " + e.getEventName() + "\n" + "Event ID      : " + e.getEventId() + "\n"
                + "Event NPT     : " + e.getEventNPT() + "\n" + "Event Data Len: " + length + "\n" + "Event Data    : "
                + text;
        banner(line);
    }

    // /////////////////////////////////////////////////////////////////////////////
    // OBJECT CAROUSEL VIEWING METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    protected ArgParser parseArgs(XletContext xletContext) throws Exception
    {
        // Get hostapp parameters.
        ArgParser args = new ArgParser((String[]) xletContext.getXletProperty(XletContext.ARGS));
        frequency = args.getIntArg(FREQUENCY);
        prog_num = args.getIntArg(PROG_NUM);
        qam = args.getIntArg(QAM);
        carousel_id = args.getIntArg(CAROUSEL_ID);
        wait_time = args.getIntArg(WAIT_TIME);
        event_name = args.getStringArg(EVENT_NAME);
        return args;
    }

    // Attach to a carousel.
    protected ServiceDomain mount(int carouselID, ServiceDomain sd)
    {
        try
        {
            sd.attach(ocap_loc, carouselID);
            banner("Mount complete: " + sd.getMountPoint().getCanonicalPath());
        }
        catch (Exception e)
        {
            printException(e);
            return null;
        }
        return sd;
    }

    protected ServiceDomain mount(int carouselID)
    {
        banner("Mounting C:" + carouselID);
        ServiceDomain s = null;

        try
        {
            s = new ServiceDomain();
            mount(carouselID, s);
        }
        catch (Exception e)
        {
            printException(e);
        }

        return s;
    }

    protected void unmount(ServiceDomain s)
    {
        banner("Unmounting");
        try
        {
            s.detach();
        }
        catch (Exception e)
        {
            printException(e);
        }
    }

    protected void sleep()
    {
        sleep(wait_time);
    }

    protected void sleep(int time)
    {
        banner("Sleeping for " + time + " seconds");
        try
        {
            Thread.sleep(time * 1000);
        }
        catch (InterruptedException e)
        {
            message("Caught interrupted exception for no apparent reason.  Oh well.");
        }
    }

    // Tune inband.
    protected void tune() throws Exception
    {
        banner("Tuning: F:" + frequency + " P:" + prog_num + " Q:" + qam);

        synchronized (this)
        {
            try
            {
                tuned = false;
                sc = ServiceContextFactory.getInstance().createServiceContext();
                sim = (SIManager) SIManager.createInstance();
                sc.addListener(this);

                ocap_loc = new OcapLocator(frequency, prog_num, qam);
                banner("Using locator: " + ocap_loc.toString());
                Service service = sim.getService(ocap_loc);
                sc.select(service);
                this.wait(30 * 1000);
            }
            catch (Exception e)
            {
                printException(e);
            }
            if (!tuned)
            {
                throw new Exception("Tune failed");
            }
        }
    }

    protected String[] list(DSMCCStreamEvent se)
    {
        String names[] = se.getEventList();
        banner("Event list");
        for (int i = 0; i < names.length; i++)
        {
            message(i + ": " + names[i]);
        }
        return names;
    }

    protected int subscribe(DSMCCStreamEvent se, String event, StreamEventListener sel)
    {
        banner("Subscribing to " + event);
        try
        {
            return se.subscribe(event, sel);
        }
        catch (Exception e)
        {
            printException(e);
            return 0;
        }
    }

    protected void unsubscribe(DSMCCStreamEvent se, String event, StreamEventListener sel)
    {
        banner("Unsubscribing by name to " + event);
        try
        {
            se.unsubscribe(event, sel);
        }
        catch (Exception e)
        {
            printException(e);
        }
    }

    protected void unsubscribe(DSMCCStreamEvent se, int event, StreamEventListener sel)
    {
        banner("Unsubscribing by ID to " + event);
        try
        {
            se.unsubscribe(event, sel);
        }
        catch (Exception e)
        {
            printException(e);
        }
    }

    protected int[] subscribeAll(DSMCCStreamEvent se, String events[], StreamEventListener sel)
    {
        int[] ids = new int[events.length];
        for (int i = 0; i < events.length; i++)
        {
            ids[i] = subscribe(se, events[i], sel);
        }
        return ids;
    }

    protected void unsubscribeAll(DSMCCStreamEvent se, String events[], StreamEventListener sel)
    {
        for (int i = 0; i < events.length; i++)
        {
            unsubscribe(se, events[i], sel);
        }
    }

    protected void unsubscribeAll(DSMCCStreamEvent se, int events[], StreamEventListener sel)
    {
        for (int i = 0; i < events.length; i++)
        {
            unsubscribe(se, events[i], sel);
        }
    }

    // Utility functions
    protected void banner(String s)
    {
        message(LINE + "\n" + s + "\n" + LINE);
    }

    protected void message(String s)
    {
        System.out.println(s);
    }

    protected void printException(Exception e)
    {
        banner("Caught Exception: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    class TestEventListener implements StreamEventListener
    {
        int m_instance;

        public TestEventListener(int n)
        {
            m_instance = n;
        }

        public void receiveStreamEvent(StreamEvent e)
        {
            banner("Listener: " + m_instance + " received event " + e.getEventName());
            printStreamEvent(e);
        }
    }
}
