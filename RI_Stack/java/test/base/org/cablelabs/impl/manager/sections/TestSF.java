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

/*
 * Created on Jul 12, 2006
 */
package org.cablelabs.impl.manager.sections;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.SectionFilterManager.Filter;
import org.cablelabs.impl.manager.SectionFilterManager.FilterCallback;
import org.cablelabs.impl.manager.SectionFilterManager.FilterSpec;
import org.cablelabs.impl.util.Arrays;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;

import org.davic.mpeg.ElementaryStream;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.NotAuthorizedInterface;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.sections.Section;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.StreamTable;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.mpeg.PODExtendedChannel;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramAssociationTableManager;
import org.ocap.si.ProgramMapTableManager;

/**
 * Simple application that can be used to smoke-test SectionFilterManager.
 * <p>
 * Following is an example invocation (as would be found in
 * <code>mpeenv.ini</code>):
 * 
 * <pre>
 * MainClassArgs.0=org.cablelabs.impl.manager.sections.TestSF
 * MainClassArgs.1=ocap://0x41a
 * MainClassArgs.2=-pmt
 * MainClassArgs.3=-filter
 * MainClassArgs.4=pid=65
 * MainClassArgs.5=timesToMatch=1
 * MainClassArgs.6=posFilter=0,0,0,0,0x01,0xe7
 * MainClassArgs.7=posMask=0,0,0,0,0xFF,0xFF
 * MainClassArgs.9=-filter
 * MainClassArgs.10=pid=65
 * MainClassArgs.11=timesToMatch=1
 * MainClassArgs.12=posFilter=0x74
 * MainClassArgs.13=posMask=0xff
 * MainClassArgs.14=negMask=0,0,0,0,0x00,0x3E
 * MainClassArgs.15=negFilter=0,0,0,0,0x00,0x26
 * MainClassArgs.16=-filter
 * MainClassArgs.17=pid=71
 * MainClassArgs.18=timesToMatch=1
 * MainClassArgs.19=-filter
 * MainClassArgs.20=isInBand=false
 * MainClassArgs.21=frequency=-2
 * MainClassArgs.22=pid=0x1FFC
 * MainClassArgs.23=posMask=0x74
 * MainClassArgs.24=posFilter=0xFF
 * MainClassArgs.25=timesToMatch=2
 * </pre>
 * 
 * @author Aaron Kamienski
 */
public class TestSF implements FilterCallback
{
    public static void main(String args[])
    {
        try
        {
            (new TestSF(args)).go();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private boolean useStreamTable = false;

    private boolean tuneUsingStreamTable = false;

    private OcapLocator loc;

    private Vector requests = new Vector();

    private TransportStream ts;

    private boolean waitPmt, waitPat;

    private int sourceHash(FilterSpec f)
    {
        return (f.isInBand ? 0 : -1) ^ f.frequency ^ f.transportStreamId ^ f.tunerId;
    }

    private int makeFilter(FilterSpec f, String[] args, int i) throws Exception
    {
        int sourceHash = sourceHash(f);
        Class clazz = f.getClass();
        for (; i < args.length && !args[i].startsWith("-"); ++i)
        {
            // expect <field>=<value>
            int idx = args[i].indexOf('=');
            if (idx < 0) throw new IllegalArgumentException("Cannot parse: " + args[i]);
            String fieldName = args[i].substring(0, idx);
            String value = args[i].substring(idx + 1);

            Field field = clazz.getField(fieldName);
            Class fClass = field.getType();
            if (fClass == boolean.class)
            {
                field.setBoolean(f, "true".equals(value));
            }
            else if (fClass == int.class)
            {
                field.setInt(f, Integer.decode(value).intValue());
            }
            else if (fClass == byte[].class)
            {
                field.set(f, makeArray(value));
            }
            else
                throw new IllegalArgumentException("Cannot handle field type " + fClass);
        }
        if (f instanceof MyFilter) ((MyFilter) f).sourceSet = sourceHash != sourceHash(f);
        System.out.println("####### Created filter: " + f);

        return i;
    }

    private byte[] makeArray(String value) throws Exception
    {
        StringTokenizer tok = new StringTokenizer(value, ",");
        byte[] array = new byte[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i)
        {
            String str = tok.nextToken();
            array[i] = Integer.decode(str).byteValue();
        }
        return array;
    }

    TestSF(String args[]) throws Exception
    {
        ManagerManager.getInstance(EventDispatchManager.class);

        if (args.length < 1) throw new IllegalArgumentException();

        loc = new OcapLocator(args[0]);

        for (int i = 1; i < args.length; ++i)
        {
            if ("-filter".equals(args[i]))
            {
                MyFilter f = new MyFilter();
                i = makeFilter(f, args, i + 1) - 1;
                requests.addElement(f);
            }
            else if ("-table".equals(args[i]))
            {
                useStreamTable = true;
            }
            else if ("-tunetable".equals(args[i]))
            {
                tuneUsingStreamTable = true;
            }
            else if ("-ni".equals(args[i]))
            {
                useStreamTable = false;
            }
            else if ("-sleep".equals(args[i]))
            {
                long ms = Integer.parseInt(args[++i]);
                System.out.println("######## Sleeping " + ms + " ########");
                Thread.sleep(ms);
                System.out.println("######## Slept " + ms + " ########");
            }
            else if ("-wait".equals(args[i]))
            {
                // currently unimplemented
                // the idea is to wait for SIDB to come up...
            }
            else if ("-pmt".equals(args[i]))
            {
                waitPmt = true;
            }
            else if ("-pat".equals(args[i]))
            {
                waitPat = true;
            }
            else
            {
                System.out.println("Unknown argument ignored: " + args[i]);
            }
        }
        System.out.println("######## useStreamTable = " + useStreamTable + " ########");
        if (requests.size() == 0)
        {
            // Default arguments...
            requests.addElement(new MyFilter());
        }
    }

    class MyFilter extends FilterSpec
    {
        MyFilter()
        {
            // Default the priority to something useable
            priority = FILTER_PRIORITY_DAVIC;
        }

        public String toString()
        {
            String str = "FilterSpec[";
            Class clazz = getClass();
            Field[] fields = clazz.getFields();
            for (int i = 0; i < fields.length; ++i)
            {
                Class fClazz = fields[i].getType();
                try
                {
                    if (fClazz == byte[].class)
                    {
                        str += fields[i].getName() + "=" + TestSF.toString(fields[i].get(this)) + ",";
                    }
                    else if (0 == (fields[i].getModifiers() & Modifier.STATIC))
                    {
                        if (fields[i].getName().startsWith("pos"))
                            System.err.println("#####" + fClazz + "!=" + byte[].class + "#####");
                        str += fields[i].getName() + "=" + fields[i].get(this) + ",";
                    }
                }
                catch (Exception e)
                {
                    str += fields[i] + "=<inaccessible>";
                }
            }
            return str + "]";
        }

        public boolean sourceSet;

        public Filter filter;
    }

    /**
     * A much more reasonable toString() for arrays.
     */
    public static String toString(Object array)
    {
        StringBuffer b = new StringBuffer();
        try
        {
            b.append("[ ");

            for (int i = 0; i < Array.getLength(array); ++i)
            {
                if (i > 0) b.append(", ");

                Object o = Array.get(array, i);

                if (o == null)
                    b.append("null");
                else if (o.getClass().isArray())
                    b.append(toString((Object[]) o));
                else if (o instanceof Byte)
                    b.append("0x").append(Integer.toHexString(((Number) o).byteValue() & 0xFF));
                else
                    b.append(o.toString());
            }

            b.append(" ]");
        }
        catch (NullPointerException e)
        {
            return "null";
        }
        return b.toString();
    }

    public void go() throws Exception
    {
        doTune(loc);
        startFilters(requests);
    }

    private void doTune(OcapLocator loc) throws Exception
    {
        if (loc != null)
        {
            NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();

            class Listener implements NetworkInterfaceListener, ResourceClient
            {
                public synchronized void receiveNIEvent(NetworkInterfaceEvent e)
                {
                    System.out.println(e);
                    if (e instanceof NetworkInterfaceTuningOverEvent)
                    {
                        System.out.println("########Tuning complete: "
                                + ((((NetworkInterfaceTuningOverEvent) e).getStatus() == NetworkInterfaceTuningOverEvent.FAILED) ? "FAILED"
                                        : "SUCCESS") + e.getSource() + "########");
                        notifyAll();
                    }
                }

                public synchronized boolean requestRelease(ResourceProxy proxy, Object requestData)
                {
                    System.out.println("########Request DENIED!########");
                    return false;
                }

                public synchronized void release(ResourceProxy proxy)
                {
                    System.out.println("########Released!########");
                    notifyAll();
                }

                public synchronized void notifyRelease(ResourceProxy proxy)
                {
                    System.out.println("########Released!########");
                    notifyAll();
                }
            }
            Listener l = new Listener();
            NetworkInterfaceController nic = new NetworkInterfaceController(l);

            // Tune
            nic.reserveFor(loc, this);
            nic.getNetworkInterface().addNetworkInterfaceListener(l);

            ts = getTs(nic, loc);

            synchronized (l)
            {
                System.out.println("########Start tuning " + System.currentTimeMillis() + "########");
                if (tuneUsingStreamTable)
                {
                    System.out.println("######## Tuning to " + ts + " ########");
                    nic.tune(ts);
                }
                else
                {
                    System.out.println("######## Tuning to " + loc + " ########");
                    nic.tune(loc);
                }
                l.wait(300000);
                System.out.println("########Done tuning " + System.currentTimeMillis() + "########");
            }

            // Now let's wait for PAT/PMT
            if (waitPat) waitPAT(loc);
            if (waitPmt) waitPMT(loc);

            if (useStreamTable)
            {
                ts = getTs(nic, loc);
                System.out.println("######## Using TS from StreamTable: " + ts + " ########");
            }
            else
            {
                ts = nic.getNetworkInterface().getCurrentTransportStream();
                System.out.println("######## Using TS from NI: " + ts + " ########");
            }
        }
    }

    private TransportStream getTs(NetworkInterfaceController nic, OcapLocator loc) throws Exception
    {
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        TransportStream[] streams = StreamTable.getTransportStreams(loc);
        TransportStream ts = null;

        for (int i = 0; i < streams.length; ++i)
        {
            if (nic.getNetworkInterface().equals(nim.getNetworkInterface(streams[i])))
            {
                ts = streams[i];
                break;
            }
        }
        System.out.println("######## Found TS in StreamTable: " + ts + " ########");
        return ts;
    }

    private void waitPAT(OcapLocator loc) throws Exception
    {
        ProgramAssociationTableManager patMgr = ProgramAssociationTableManager.getInstance();
        if (patMgr == null)
        {
            System.out.println("######## PAT support isn't implemented yet ########");
            return;
        }
        class Request implements SIRequestor
        {
            boolean done = false;

            public synchronized void notifyFailure(SIRequestFailureType fail)
            {
                done = true;
                System.out.println("######## PAT FAIL: " + fail + " ########");
            }

            public synchronized void notifySuccess(SIRetrievable[] r)
            {
                done = true;
                System.out.println("######## PAT acquired ########");
            }
        }
        Request req = new Request();
        synchronized (req)
        {
            patMgr.retrieveInBand(req, loc);
            if (req.done != true) wait(30000);
        }
    }

    private void waitPMT(OcapLocator loc) throws Exception
    {
        ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();
        class Request implements SIRequestor
        {
            boolean done = false;

            public synchronized void notifyFailure(SIRequestFailureType fail)
            {
                done = true;
                System.out.println("######## PMT FAIL: " + fail + " ########");
            }

            public synchronized void notifySuccess(SIRetrievable[] r)
            {
                done = true;
                System.out.println("######## PMT acquired ########");
            }
        }
        Request req = new Request();
        synchronized (req)
        {
            pmtMgr.retrieveInBand(req, loc);
            if (req.done != true) req.wait(30000);
        }
    }

    private void startFilters(Vector requests) throws Exception
    {
        SectionFilterManager sf = (SectionFilterManager) ManagerManager.getInstance(SectionFilterManager.class);

        boolean inBand = !(ts instanceof PODExtendedChannel);
        int tsid, freq, tuner;
        if (inBand)
        {
            tsid = ts.getTransportStreamId();
            freq = ((TransportStreamExt) ts).getFrequency();
            tuner = ((ExtendedNetworkInterface) NetworkInterfaceManager.getInstance().getNetworkInterface(ts)).getHandle();
        }
        else
        {
            tsid = -1;
            freq = 0;
            tuner = 0;
        }

        final int N = requests.size();
        for (int i = 0; i < N; ++i)
        {
            MyFilter request = (MyFilter) requests.elementAt(i);

            // Don't modify source info if previously set
            if (!request.sourceSet)
            {
                request.isInBand = inBand;
                request.frequency = freq;
                request.transportStreamId = tsid;
                request.tunerId = tuner;
            }

            System.out.println("######### Starting filter: " + request);
            try
            {
                request.filter = sf.startFilter(request, this);
            }
            catch (org.cablelabs.impl.davic.mpeg.NotAuthorizedException e)
            {
                e.setElementaryStreams(request.isInBand ? ts : PODExtendedChannel.getInstance(), request.pid);
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void dump(byte[] t)
    {
        if (t == null) return;
        /*
         * java.io.ByteArrayOutputStream bos = new
         * java.io.ByteArrayOutputStream(); java.io.PrintWriter out = new
         * java.io.PrintWriter(bos);
         */
        java.io.PrintStream out = System.out;
        out.println();
        for (int i = 0; i < t.length; ++i)
        {
            String str = Integer.toHexString(t[i] & 0xFF);

            if (i % 16 == 0) out.print(toHexString(i) + ": ");

            if ((t[i] & 0xFF) < 0x10) str = "0" + str;
            out.print(str);
            if (i % 16 == 15)
                out.println();
            else if (i % 4 == 3) out.print(" ");
        }
        out.println();

        // log.info(bos.toString());
    }

    static final char[] ABCD = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String toHexString(int x)
    {
        StringBuffer sb = new StringBuffer(8);

        for (int i = 0; i < 8; ++i)
        {
            sb.append(ABCD[(x >> 28) & 0xF]);
            x <<= 4;
        }
        return sb.toString();
    }

    private void dump(Section section) throws Exception
    {
        System.out.println("######## Section ########");
        System.out.println("Section received: full=" + section.getFullStatus());
        System.out.println("            table_id = 0x" + Integer.toHexString(section.table_id()));
        System.out.println("      section_length = 0x" + Integer.toHexString(section.section_length()));
        System.out.println("      version_number = 0x" + Integer.toHexString(section.version_number()));
        System.out.println("      section_number = 0x" + Integer.toHexString(section.section_number()));
        System.out.println(" last_section_number = 0x" + Integer.toHexString(section.last_section_number()));
        System.out.println("########== Start Of Section ==########");
        dump(section.getData());
        System.out.println("########== End Of Section ==########");
        System.out.println();
    }

    public void notifyCanceled(FilterSpec source, int reason)
    {
        System.out.println("######### Filter was cancelled: " + source);
    }

    public void notifySection(FilterSpec source, Section s, boolean last)
    {
        try
        {
            System.out.println("###########" + (last ? "Last" : "") + "Section received for " + source);
            dump(s);
        }
        catch (Exception e)
        {
            System.err.println("########### Problem dumping section: " + source);
            e.printStackTrace();
        }
    }
}
