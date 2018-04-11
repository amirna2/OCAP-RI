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

package org.cablelabs.impl.manager.signalling;

import org.cablelabs.impl.signalling.*;
import java.util.*;
import java.lang.reflect.*;
import org.cablelabs.impl.manager.SignallingManager;
import org.ocap.net.OcapLocator;
import org.davic.net.tuning.*;
import org.davic.resources.*;

/**
 * This is a simple <i>command-line</i> program that allows one to monitor for
 * application signalling using the <code>DavicSignallingMgr</code>
 * implementation. This isn't a unit test, but more of a smoke test.
 * 
 * <p>
 * 
 * <pre>
 * Usage: SigMon [options] <which> ...
 * Where <which> is one of "xait", or <serviceId>, or f=<freq>.<prog>[.<qam>]
 * Options:
 * -test         : Use the TestSignallingMgr
 * -mgr=<mgr>    : Use the <mgr> implementation of SignallingManager
 * -tune[=<loc>] : Tune to location prior to test
 *  -<n> : Stop monitoring xait or service after <n> acquisitions
 * </pre>
 * 
 * @author Aaron Kamienski
 * @see TestSignallingMgr
 * @see DavicSignallingMgr
 */
public class SigMon
{
    /**
     * Print usage() information.
     */
    private static void usage()
    {
        System.out.println("Usage: SigMon [options] <which> ...");
        System.out.println("Where <which> is one of \"xait\", <serviceId>, or ");
        System.out.println("                         f=<freq>.<prog>[.<qam>]");
        System.out.println("Note the serviceId, freq, and prog must be in hex");
        System.out.println("(i.e., suitable for use in an OcapLocator.");
        System.out.println();
        System.out.println("Options:");
        System.out.println(" -test         : Use the TestSignallingMgr");
        System.out.println(" -mgr=<mgr>    : Use the <mgr> implementation of SignallingManager");
        System.out.println(" -tune[=<loc>] : Tune to location prior to test");
        System.out.println(" -<n>          : Stop monitoring xait or service after <n> acquisitions");
    }

    private int countDown = 1;

    private boolean tune = false;

    private OcapLocator tuneTo = null;

    SignallingManager sig = (SignallingManager) DavicSignallingMgr.getInstance();

    /**
     * Execute based on given arguments.
     */
    SigMon(String args[]) throws Exception
    {
        // Reference to get library loaded
        Class ocapMain = org.cablelabs.impl.ocap.OcapMain.class;

        for (int i = 0; i < args.length; ++i)
        {
            if (args[i].startsWith("-"))
            {
                if (Character.isDigit(args[i].charAt(1)))
                {
                    countDown = Integer.parseInt(args[i].substring(1));
                    System.out.println("countDown: " + countDown);
                }
                else if ("-test".equals(args[i]))
                {
                    sig = (SignallingManager) TestSignallingMgr.getInstance();
                    System.out.println("SigMgr: " + sig);
                }
                else if (args[i].startsWith("-mgr="))
                {
                    String classname = "org.cablelabs.impl.manager.signalling." + args[i].substring(5);
                    Class clazz = Class.forName(classname);
                    Method m = clazz.getMethod("getInstance", new Class[0]);
                    sig = (SignallingManager) m.invoke(null, new Object[0]);
                    System.out.println("SigMgr: " + sig);
                }
                else if (args[i].startsWith("-tune="))
                {
                    tune = true;
                    tuneTo = new OcapLocator("ocap://" + args[i].substring(6));
                    System.out.println("tuneTo: " + tuneTo);
                }
                else if ("-tune".equals(args[i]))
                {
                    tune = true;
                }
                else if ("-?".equals(args[i]))
                {
                    usage();
                }
                else
                {
                    System.err.println("Unknown option " + args[i]);
                }
            }
            else if ("xait".equals(args[i]))
            {
                tune(null);
                System.out.println("Adding XAIT Listener...");
                sig.addXaitListener(new Listener());
            }
            else
            {
                OcapLocator loc = parseLocator(args[i]);

                tune(loc);
                System.out.println("Adding AIT Listener...");
                sig.addAitListener(loc, new Listener(loc));
            }
        }
    }

    private void tune(OcapLocator loc) throws Exception
    {
        if (tune)
        {
            tune = false;

            if (tuneTo != null) loc = tuneTo;

            if (loc != null)
            {
                NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();

                class Listener implements NetworkInterfaceListener, ResourceClient
                {
                    public synchronized void receiveNIEvent(NetworkInterfaceEvent e)
                    {
                        System.out.println(e);
                        if (e instanceof NetworkInterfaceTuningOverEvent) notifyAll();
                    }

                    public synchronized boolean requestRelease(ResourceProxy proxy, Object requestData)
                    {
                        return false;
                    }

                    public synchronized void release(ResourceProxy proxy)
                    {
                        System.out.println("Released!");
                        notifyAll();
                    }

                    public synchronized void notifyRelease(ResourceProxy proxy)
                    {
                        System.out.println("Released!");
                        notifyAll();
                    }
                }
                Listener l = new Listener();
                NetworkInterfaceController nic = new NetworkInterfaceController(l);

                // Tune
                nic.reserveFor(loc, this);
                nic.getNetworkInterface().addNetworkInterfaceListener(l);
                synchronized (l)
                {
                    System.out.println("Start tuning " + System.currentTimeMillis());
                    nic.tune(loc);
                    l.wait(30000);
                    System.out.println("Done tuning " + System.currentTimeMillis());
                }
            }
        }
    }

    private OcapLocator parseLocator(String str) throws Exception
    {
        return new OcapLocator("ocap://" + str);
    }

    /**
     * Dumps the given AIT. Synchronized to avoid mixing print-outs.
     */
    private synchronized void dumpAit(OcapLocator loc, Ait ait)
    {
        System.out.println("AIT recieved for service: " + loc);

        AppEntry[] apps = ait.getApps();
        System.out.println("  Contains " + apps.length + " apps:");
        for (int i = 0; i < apps.length; ++i)
        {
            System.out.println("[" + i + "]= " + apps[i]);
        }
        System.out.println();
    }

    /**
     * Dumps the given XAIT. Synchronized to avoid mixing print-outs.
     */
    private synchronized void dumpXait(Xait xait)
    {
        System.out.println("XAIT received");

        AbstractServiceEntry[] services = xait.getServices();
        System.out.println("  Contains " + services.length + " services:");
        for (int i = 0; i < services.length; ++i)
        {
            System.out.println();
            System.out.println("[" + i + "]= " + services[i]);
            System.out.println("  Contains " + services[i].apps.size() + " apps:");

            int j = 0;
            for (Enumeration e = services[i].apps.elements(); e.hasMoreElements();)
            {
                System.out.println("   [" + j + "]= " + e.nextElement());
            }
        }
        System.out.println();
    }

    /**
     * SignallingListener implementation. One instance is created for each
     * command-line request.
     */
    public class Listener implements SignallingListener
    {
        private int countDown = SigMon.this.countDown;

        private OcapLocator loc = null;

        Listener()
        {
        }

        Listener(OcapLocator loc)
        {
            this.loc = loc;
        }

        /**
         * Randles received signalling and then removes self as listener if
         * countDown is reached.
         */
        public void signallingReceived(SignallingEvent event)
        {
            dump(event);
            if (--countDown == 0)
            {
                if (loc == null)
                    sig.removeXaitListener(this);
                else
                    sig.removeAitListener(loc, this);
            }
        }

        /**
         * Invokes dumpAit() or dumpXait().
         */
        private void dump(SignallingEvent event)
        {
            Ait ait = event.getSignalling();
            if (ait instanceof Xait)
                dumpXait((Xait) ait);
            else
                dumpAit(loc, ait);
        }
    }

    /**
     * Parses "0x"-prefixed as hexadecimal, "o"-prefixed as octal, and
     * everything else as decimal.
     */
    private static int parseInt(String str)
    {
        int rad = 10;
        if (str.startsWith("0x"))
        {
            rad = 16;
            str = str.substring(2);
        }
        else if (str.startsWith("o"))
        {
            rad = 8;
            str = str.substring(1);
        }
        return Integer.parseInt(str, rad);
    }

    public static void main(String args[])
    {
        try
        {
            new SigMon(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
