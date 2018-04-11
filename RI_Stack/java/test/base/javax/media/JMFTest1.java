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

package javax.media;

import java.io.IOException;
import java.net.URL;

import javax.tv.locator.Locator;
import javax.tv.media.MediaSelectEvent;
import javax.tv.media.MediaSelectFailedEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.media.MediaSelectSucceededEvent;
import javax.tv.service.selection.ServiceMediaHandler;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.media.DVBMediaSelectControl;
import org.ocap.net.OcapLocator;

public class JMFTest1 extends TestCase implements ControllerListener, MediaSelectListener
{
    Player mediaHandler = null;

    DVBMediaSelectControl dmsc = null;

    int sourceId;

    OcapLocator serviceLocator = null;

    MediaLocator mediaLocator = null;

    short playerStarted = -1;

    short selectionSuccess = -1;

    short stopByRequestEvent = -1;

    short prefetchCompleteEvent = -1;

    short stopAtTimeEvent = -1;

    short stopEvent = -1;

    short stopTimeChangeEvent = -1;

    public void testCreatePlayerFromURL() throws Exception
    {
        try
        {
            URL url = getClass().getResource("JMFTest1.class");
            Player player = Manager.createPlayer(url);
        }
        catch (NoPlayerException noPlayerExc)
        {
            // expected outcome
        }
    }

    //
    // test for NullPointerException uncovered by TCK test
    // javasoft.sqe.tests.api.javax.media.NoPlayerException.NoPlayerException002.NoPlayerException00201
    //
    public void testUnknownSourceType()
    {
        boolean pass = false;
        try
        {
            Player player = Manager.createPlayer(new MediaLocator("xyz:foo"));
            fail("createPlayer should have thrown an exception");
        }
        catch (NullPointerException npe)
        {
            fail("createPlayer threw an null pointer instead of NoPlayer");
        }
        catch (IOException ioe)
        {
            fail("createPlayer threw an io exception instead of NoPlayer");
        }
        catch (NoPlayerException noPlayerExc)
        {
            // expected outcome
        }
    }

    public void testHandler()
    {
        Player mediaHandler = null;
        DVBMediaSelectControl dmsc = null;
        int sourceId;

        System.out.println("Start test - JMFTest1");

        // sourceId = 0x7D3; // on the box
        sourceId = 0x44C; // on the simulator

        try
        {
            int[] p = new int[0];
            serviceLocator = new OcapLocator(sourceId, p, -1, null);
            if (serviceLocator == null)
            {
                throw new org.davic.net.InvalidLocatorException();
            }
            mediaLocator = new MediaLocator(serviceLocator.toExternalForm());
            mediaHandler = (ServiceMediaHandler) javax.media.Manager.createPlayer(mediaLocator);
            if (mediaHandler == null)
            {
                System.out.println("Test results: JMFTest1 - Failed to create the player\n");
            }
            mediaHandler.addControllerListener(this);
        }
        catch (IOException e)
        {
            System.out.println("Test results: JMFTest1  - Content No found\n");
            return;
        }
        catch (NoPlayerException e)
        {
            System.out.println("Test results: JMFTest1 - Player not found\n");
            return;
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            System.out.println("Test results: JMFTest1 - failed to create OCAP locator\n");
            return;
        }

        try
        {
            // get an instancce of DVBMediaSelectControl
            dmsc = (DVBMediaSelectControl) mediaHandler.getControl("org.dvb.media.DVBMediaSelectControl");
            if (dmsc != null)
            {
                // add myself as listener to the mediaselect events
                dmsc.addMediaSelectListener((MediaSelectListener) this);

                // select the service pointed to by serviceLocator
                dmsc.selectServiceMediaComponents((Locator) serviceLocator);

                // wait untill MediaSelectEvent is received
                while (selectionSuccess == -1)
                {
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                }
                if (selectionSuccess == 0)
                {
                    return;
                }

                // set the player to stop 5 seconds later
                long offset = System.currentTimeMillis() + 5000;
                offset *= 1000000; // convert to nanoseconds
                Time t = new javax.media.Time(offset);
                mediaHandler.setStopTime(t);

                // wait untill stopAtTimeEvent event is recieved
                while (stopAtTimeEvent == -1)
                {
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                }

                if (stopAtTimeEvent == 0)
                {
                    return;
                }

                // do a explicit prefetch
                playerStarted = -1;
                prefetchCompleteEvent = -1;
                mediaHandler.prefetch();

                // wait while PrefetchCompleteEvent event is received
                while (prefetchCompleteEvent == -1)
                {
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }

                }
                if (prefetchCompleteEvent == 0)
                {
                    return;
                }

                // set the player to automaticall start after 5 seconds
                offset = System.currentTimeMillis() + 5000;
                offset = offset * 1000000; // convert to nanoseconds
                t = new javax.media.Time(offset);
                mediaHandler.syncStart(t);

                // wait untill you get the start event
                while (playerStarted == -1)
                {
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                }

                if (playerStarted == 0)
                {
                    return;
                }

                // Watch TV for 5 seconds
                try
                {
                    Thread.currentThread().sleep(5000);
                }
                catch (Exception e)
                {
                }

                // stop the player
                mediaHandler.stop();

                // wait for StopByRequestEvent event
                while (stopByRequestEvent == -1)
                {
                    try
                    {
                        Thread.currentThread().sleep(1000);
                    }
                    catch (Exception e)
                    {
                    }
                }
                if (stopByRequestEvent == 0)
                {
                    return;
                }

                // remove the app from the media select listener list.
                dmsc.removeMediaSelectListener((MediaSelectListener) this);
            }
            else
            {
                System.out.println("JMFTest1 - getControl() returned null\n");
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        // remove the app from the controller listener list.
        mediaHandler.removeControllerListener(this);
        mediaHandler.close();
        System.out.println("End test - JMFTest1 Done\n");
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(JMFTest1.class);
        return suite;
    }

    public JMFTest1(String name)
    {
        super(name);
    }

    public void controllerUpdate(ControllerEvent e)
    {
        if (e instanceof ControllerClosedEvent)
        {
            System.out.println("JMFTest1 - ControllerClosedEvent\n");
            playerStarted = 0;
            stopByRequestEvent = 0;
            stopAtTimeEvent = 0;
            stopEvent = 0;
            prefetchCompleteEvent = 0;
            stopTimeChangeEvent = 0;
        }
        else if (e instanceof ControllerErrorEvent)
        {
            System.out.println("JMFTest1 - ControllerErrorEvent\n");
            playerStarted = 0;
            stopByRequestEvent = 0;
            stopAtTimeEvent = 0;
            stopEvent = 0;
            prefetchCompleteEvent = 0;
            stopTimeChangeEvent = 0;
        }
        else if (e instanceof StartEvent)
        {
            System.out.println("JMFTest1 - StartEvent\n");
            playerStarted = 1;
        }
        else if (e instanceof StopByRequestEvent)
        {
            System.out.println("JMFTest1 - StopByRequestEvent received");
            stopByRequestEvent = 1;
        }
        else if (e instanceof StopAtTimeEvent)
        {
            System.out.println("JMFTest1 - StopAtTimeEvent received");
            stopAtTimeEvent = 1;
        }
        else if (e instanceof StopEvent)
        {
            System.out.println("JMFTest1 - StopEvent received");
            stopEvent = 1;
        }
        else if (e instanceof PrefetchCompleteEvent)
        {
            System.out.println("Test results: JMFTest1 - PrefetchCompleteEvent received");
            prefetchCompleteEvent = 1;
        }
        else if (e instanceof StopTimeChangeEvent)
        {
            stopTimeChangeEvent = 1;
            System.out.println("Test results: JMFTest1 - StopTimeChangeEvent received");
        }
        else
        {
            System.out.println("JMFTest1 - Some event");
        }
    }

    public void selectionComplete(MediaSelectEvent e)
    {
        // Media selection succeeded
        if (e instanceof MediaSelectSucceededEvent)
        {
            selectionSuccess = 1;
            System.out.println("Test results: JMFTest1 - Media Selection Success\n");
        }

        // Media selection failed
        else if (e instanceof MediaSelectFailedEvent)
        {
            selectionSuccess = 0;
            System.out.println("Test results: JMFTest1 - Media Selection failed\n");
        }
    }
}
