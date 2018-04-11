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

package org.cablelabs.xlet.SectionFilterResourceTest;

import org.cablelabs.lib.utils.ArgParser;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;

import java.lang.Thread;

import java.util.Vector;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.davic.mpeg.sections.ConnectionLostException;
import org.davic.mpeg.sections.EndOfFilteringEvent;
import org.davic.mpeg.sections.IncompleteFilteringEvent;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.ForcedDisconnectedEvent;
import org.davic.mpeg.sections.FilterResourcesAvailableEvent;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.RingSectionFilter;
import org.davic.mpeg.sections.Section;
import org.davic.mpeg.sections.SectionAvailableEvent;
import org.davic.mpeg.sections.SectionFilterEvent;
import org.davic.mpeg.sections.SectionFilterGroup;
import org.davic.mpeg.sections.SectionFilterListener;

import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.net.InvalidLocatorException;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.StreamTable;

import org.ocap.net.OcapLocator;

import org.dvb.application.AppsDatabase;
import org.dvb.application.AppID;
import org.dvb.io.ixc.IxcRegistry;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

public class SFTestXlet implements Xlet, SFTestControl
{
    // /////////////////////////////////////////////////////////////////////////////
    // XLET FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        // Store this xlet's name and context
        m_appID = new AppID((int) (Long.parseLong((String) (ctx.getXletProperty("dvb.org.id")), 16)),
                (int) (Long.parseLong((String) (ctx.getXletProperty("dvb.app.id")), 16)));
        m_xletName = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getName();
        m_appPriority = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getPriority();
        m_ctx = ctx;

        // Parse xlet arguments and initialize IXC communication with test
        // runner
        ArgParser ap = null;
        try
        {
            ap = new ArgParser((String[]) (ctx.getXletProperty(XletContext.ARGS)));

            // Lookup test runner's event handler object
            String arg = ap.getStringArg("runner");

            // Parse the individual appID and orgID from the 48-bit int
            long orgIDappID = Long.parseLong(arg.substring(2), 16);
            int oID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
            int aID = (int) (orgIDappID & 0xFFFF);

            m_eventHandler = (SFTestEvents) (IxcRegistry.lookup(ctx, "/" + Integer.toHexString(oID) + "/"
                    + Integer.toHexString(aID) + "/SFTestEvents"));

            // Publish control object via IXC to make it available to the test
            // runner
            IxcRegistry.bind(ctx, "SFTestControl" + m_xletName, this);
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + e.getMessage());
        }

        // ///////////////////////////////////////////////////////////////////////////
        // UI Setup
        //

        // Scene size and position
        try
        {
            m_x = ap.getIntArg("x");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_y = ap.getIntArg("y");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_width = ap.getIntArg("width");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_height = ap.getIntArg("height");
        }
        catch (Exception e)
        {
        }

        // Initialize HScene
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_testInfo.setBounds(m_x, m_y, m_width, m_height);
        m_scene.add(m_testInfo);
        m_scene.validate();

        // Schedule a timer to repaint the test info
        TVTimerWentOffListener timerListener = new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                m_testInfo.repaint();
            }
        };

        repaintSpec = new TVTimerSpec();
        repaintSpec.setAbsolute(false);
        repaintSpec.setRepeat(true);
        repaintSpec.setRegular(true);
        repaintSpec.setTime(1500);
        repaintSpec.addTVTimerWentOffListener(timerListener);
    }

    public void startXlet() throws XletStateChangeException
    {
        m_scene.show();
        if (m_timerRunning)
        {
            try
            {
                repaintSpec = repaintTimer.scheduleTimerSpec(repaintSpec);
            }
            catch (TVTimerScheduleFailedException e1)
            {
            }
        }
    }

    public void pauseXlet()
    {
        if (m_timerRunning) repaintTimer.deschedule(repaintSpec);
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        repaintTimer.deschedule(repaintSpec);
        m_scene.dispose();

        try
        {
            IxcRegistry.unbind(m_ctx, "SFTestControl" + m_xletName);
        }
        catch (NotBoundException e)
        {
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // XLET CONTROL FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    // Called by the test runner to instruct the xlet to create a filter group
    public void createFilterGroup() throws RemoteException
    {
        m_filterGroups.addElement(new FilterGroup(m_willingToRelease, m_priority));
        m_currentFilterGroup = m_filterGroups.size() - 1;
        m_testInfo.repaint();
    }

    // Creates a new RingSectionFilter in the currently selected group
    public void createSectionFilter() throws RemoteException
    {
        if (m_currentFilterGroup < 0)
        {
            System.out.println("TEST ERROR: A filter group must be created before a filter.");
            return;
        }

        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        fg.filters.addElement(new Filter(fg.group.newRingSectionFilter(100, 256)));
        fg.currentFilter = fg.filters.size() - 1;
        m_testInfo.repaint();
    }

    // Moves the current filter group selection forward one position
    public void changeFilterGroup() throws RemoteException
    {
        if (!m_filterGroups.isEmpty())
            m_currentFilterGroup = (m_currentFilterGroup == (m_filterGroups.size() - 1)) ? 0 : m_currentFilterGroup + 1;
        m_testInfo.repaint();
    }

    // Moves the current section filter selection (within the currently selected
    // group) forward one position
    public void changeSectionFilter() throws RemoteException
    {
        if (!m_filterGroups.isEmpty())
        {
            FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
            if (!fg.filters.isEmpty())
                fg.currentFilter = (fg.currentFilter == (fg.filters.size() - 1)) ? 0 : fg.currentFilter + 1;
        }
        m_testInfo.repaint();
    }

    // Attempts to start the currently selected section filter on the given PID
    public void startSectionFilter(int pid, boolean doFiltering) throws RemoteException
    {
        if (m_filterGroups.isEmpty()) return;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        if (fg.filters.isEmpty()) return;

        Filter f = (Filter) fg.filters.elementAt(fg.currentFilter);
        f.pid = pid;
        try
        {
            // All sections on this PID will be matched
            if (doFiltering)
            {
                f.filter.startFiltering(f.filter, pid);
                if (!m_timerRunning)
                {
                    try
                    {
                        repaintSpec = repaintTimer.scheduleTimerSpec(repaintSpec);
                        m_timerRunning = true;
                    }
                    catch (TVTimerScheduleFailedException e1)
                    {
                        m_timerRunning = false;
                    }
                }
            }
            // Filter for a table ID that we won't match -- this will allow us
            // to
            // keep the filter running without clogging the system with a lot of
            // matched sections
            else
            {
                if (m_timerRunning)
                {
                    repaintTimer.deschedule(repaintSpec);
                    m_timerRunning = false;
                }
                f.filter.startFiltering(f.filter, pid, 0x13);
            }

            // Update filter state
            if (fg.state == GROUP_STATE_ATTACHED)
                f.setState(FILTER_STATE_RUNNING);
            else if (fg.state == GROUP_STATE_DETACHED) f.setState(FILTER_STATE_RUN_PENDING);
            f.setMessage("");
        }
        catch (FilterResourceException e)
        {
            f.setMessage("FilterResourceException!");
        }
        catch (IllegalFilterDefinitionException e)
        {
            f.setMessage("IllegalFilterDefinitionException");
            f.setState(FILTER_STATE_STOPPED);
        }
        catch (ConnectionLostException e)
        {
            f.setMessage("ConnectionLostException!");
            f.setState(FILTER_STATE_STOPPED);
        }
        catch (NotAuthorizedException e)
        {
            f.setMessage("NotAuthorizedException!");
            f.setState(FILTER_STATE_STOPPED);
        }
    }

    // Stop the currently selected section filter
    public void stopSectionFilter() throws RemoteException
    {
        if (m_filterGroups.isEmpty()) return;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        if (fg.filters.isEmpty()) return;

        Filter f = (Filter) fg.filters.elementAt(fg.currentFilter);
        f.filter.stopFiltering();
        f.setMessage("Filter stopped");

        // Update filter state
        f.setState(FILTER_STATE_STOPPED);
    }

    // Current filter status is used to verify automated test results.
    public int getCurrentFilterState() throws RemoteException
    {
        if (m_filterGroups.isEmpty()) return -1;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        if (fg.filters.isEmpty()) return -1;

        Filter f = (Filter) fg.filters.elementAt(fg.currentFilter);
        return f.getState();
    }

    // Attempt to attach the currently selected filter group to the transport
    // stream (on the give tuner) tuned to the given frequency
    public void attachFilterGroup(int tuner, int frequency)
    {
        if (m_filterGroups.isEmpty()) return;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        attachFilterGroup(tuner, frequency, fg);
    }

    // Detaches the currently selected filter group
    public void detachFilterGroup() throws RemoteException
    {
        if (m_filterGroups.isEmpty()) return;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        fg.group.detach();
        fg.setState(GROUP_STATE_DETACHED);
    }

    // Deletes the curent filter group and releases all held resources
    public void deleteFilterGroup() throws RemoteException
    {
        if (m_filterGroups.isEmpty()) return;
        FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(m_currentFilterGroup);
        fg.group.detach();
        m_filterGroups.removeElementAt(m_currentFilterGroup);

        if (m_filterGroups.isEmpty())
        {
            m_currentFilterGroup = -1;
        }
        else
        {
            m_currentFilterGroup = (m_currentFilterGroup == 0) ? m_filterGroups.size() - 1 : m_currentFilterGroup - 1;
        }
        m_testInfo.repaint();
    }

    // Toggles the "willing-to-release" resource state of newly create filter
    // groups
    public void toggleWillingToRelease() throws RemoteException
    {
        m_willingToRelease = !m_willingToRelease;
        m_testInfo.repaint();
    }

    // Toggles the filter priority of newly create filter groups
    public void toggleFilterGroupPriority() throws RemoteException
    {
        m_priority = !m_priority;
        m_testInfo.repaint();
    }

    private void attachFilterGroup(int tuner, int frequency, FilterGroup fg)
    {
        fg.setMessage("Attaching group...");

        // Determine the transport stream
        OcapLocator ol;
        try
        {
            ol = new OcapLocator(frequency, -1);
        }
        catch (InvalidLocatorException e)
        {
            fg.setMessage("Invalid locator in attach()!");
            return;
        }
        TransportStream[] ts;
        try
        {
            ts = StreamTable.getTransportStreams(ol);
        }
        catch (NetworkInterfaceException e)
        {
            fg.setMessage("NetworkInterfaceException!");
            return;
        }

        if (fg.state == GROUP_STATE_ATTACHED) fg.setState(GROUP_STATE_DETACHED);

        // Attach the group
        try
        {
            fg.tuner = tuner;
            fg.frequency = frequency;
            fg.group.attach(ts[tuner], fg, null);
            fg.setState(GROUP_STATE_ATTACHED);
            fg.setMessage("Group attached");
        }
        catch (FilterResourceException e)
        {
            fg.setMessage("FilterResourceException!");
        }
        catch (InvalidSourceException e)
        {
            fg.setMessage("InvalidSourceException!");
        }
        catch (TuningException e)
        {
            fg.setMessage("TuningException!");
        }
        catch (NotAuthorizedException e)
        {
            fg.setMessage("NotAuthorizedException!");
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // SECTION FILTER DATA STRUCTURES //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * This class stores information relevant to each SectionFilterGroup created
     * by the Xlet
     */
    private class FilterGroup implements ResourceStatusListener, ResourceClient
    {
        // Construct a FilterGroup with the given resource and priority policies
        FilterGroup(boolean willingToRelease, boolean priority)
        {
            this.willingToRelease = willingToRelease;
            this.priority = priority;
            group = new SectionFilterGroup(2, priority);
            group.addResourceStatusEventListener(this);
        }

        // ResourceStatusEvent handler
        public void statusChanged(ResourceStatusEvent event)
        {
            // We have been forcefully disconnectd due to a resource issue
            // or the tuner has tuned away from our transport stream
            if (event instanceof ForcedDisconnectedEvent)
            {
                setState(GROUP_STATE_DISCONNECTED);
                setMessage("Group disconnected");
            }
            // If we are currently disconnected and we receive notification that
            // new resources may be available, go ahead and try to re-attach
            else if (event instanceof FilterResourcesAvailableEvent)
            {
                setMessage("Filter resources available");
                /*
                 * if (state == GROUP_STATE_DISCONNECTED) { try {
                 * Thread.sleep(2000); } catch (InterruptedException e) { }
                 * attachFilterGroup(tuner,frequency,this); }
                 */
            }
        }

        public void setMessage(String message)
        {
            this.message = message;
            m_testInfo.repaint();
        }

        public void setState(int newState)
        {
            if (state == newState) return;

            switch (newState)
            {
                case GROUP_STATE_DISCONNECTED:
                case GROUP_STATE_DETACHED:
                    // Any running filters are set to the RUN_PENDING state
                    // because they will automaticaly start
                    for (int i = 0; i < filters.size(); ++i)
                    {
                        Filter f = (Filter) filters.elementAt(i);
                        if (f.getState() == FILTER_STATE_RUNNING) f.setState(FILTER_STATE_RUN_PENDING);
                    }
                    break;
                case GROUP_STATE_ATTACHED:
                    // If the group attach succeeds, that means that all filters
                    // that were
                    // in the RUN_PENDING state are now running
                    for (int i = 0; i < filters.size(); ++i)
                    {
                        Filter f = (Filter) filters.elementAt(i);
                        if (f.getState() == FILTER_STATE_RUN_PENDING) f.setState(FILTER_STATE_RUNNING);
                        f.setMessage("");
                    }
                    break;
            }
            state = newState;
            m_testInfo.repaint();
        }

        // ResourceClient methods
        public boolean requestRelease(ResourceProxy arg0, Object arg1)
        {
            if (willingToRelease) setMessage("Detached by request");

            return willingToRelease;
        }

        public void release(ResourceProxy arg0)
        {
        }

        public void notifyRelease(ResourceProxy arg0)
        {
        }

        // Debug message for display on-screen
        private String message = "";

        // Current group state
        private int state = GROUP_STATE_DETACHED;

        // Currently selected section filter
        int currentFilter = -1;

        // Information about transport stream we are attached to
        int frequency = -1;

        int tuner = -1;

        // This resource and priority policies of this group
        boolean willingToRelease = false;

        boolean priority = true;

        // List of Filter objects representing each section filter that is a
        // member of this group
        Vector filters = new Vector();

        // DAVIC filter group
        SectionFilterGroup group;
    }

    /**
     * This class holds information relevant to each section filter created by
     * this xlet.
     */
    private class Filter implements SectionFilterListener
    {
        // Construct a new filter with the given RingSectionFilter
        public Filter(RingSectionFilter f)
        {
            filter = f;
            f.addSectionFilterListener(this);
        }

        // Called when section events are received by this filter
        public void sectionFilterUpdate(SectionFilterEvent event)
        {
            synchronized (lock)
            {
                if (event instanceof IncompleteFilteringEvent)
                {
                    setMessage("Filtering aborted");
                }
                // Section filter is always stopped when EndOfFilteringEvent is
                // seen.
                // For a ring filter, this means that the ring is full and the
                // most
                // recently acquired section could not be inserted
                else if (event instanceof EndOfFilteringEvent)
                {
                    setMessage("Filter ring is full");
                    setState(FILTER_STATE_STOPPED);
                }
                // A new section is available
                else if (event instanceof SectionAvailableEvent)
                {
                    outstandingSections++;
                    lock.notifyAll();
                }
            }
        }

        // Set the state of this filter
        public void setState(int newState)
        {
            if (newState == state) return;

            synchronized (lock)
            {
                // If we have just started running, then we need to start our
                // section handling thread
                if (newState == FILTER_STATE_RUNNING)
                {
                    // If we currently have a section handler thread, make sure
                    // it
                    // is no longer running
                    if (sectionHandler != null)
                    {
                        sectionHandler.runSectionHandler = false;
                        lock.notify();
                    }
                    sectionHandler = new SectionHandlerThread();
                    sectionHandler.start();
                }
                // All other states imply that we are not running, so make sure
                // the
                // section handling thread will stop when all current sections
                // have
                // been handled
                else
                {
                    if (sectionHandler != null) sectionHandler.runSectionHandler = false;
                    lock.notifyAll();
                }
                state = newState;
            }

            m_testInfo.repaint();
        }

        public int getState()
        {
            return state;
        }

        public void setMessage(String message)
        {
            this.message = message;
            m_testInfo.repaint();
        }

        // Section handling thread
        private SectionHandlerThread sectionHandler = null;

        private class SectionHandlerThread extends Thread
        {
            public boolean runSectionHandler = true;

            public void run()
            {
                // As long as our section filter is running or we have sections
                // outstanding, handle sections
                while (runSectionHandler)
                {
                    synchronized (lock)
                    {
                        // If the next section in our ring is full, then handle
                        // it
                        if (outstandingSections != 0)
                        {
                            Section newSection = filter.getSections()[ringIndex];
                            newSection.setEmpty();
                            outstandingSections--;
                            message = "Ring sections = " + outstandingSections;

                            // Increment our current ring index
                            if (++ringIndex == filter.getSections().length) ringIndex = 0;
                        }
                        else
                        {
                            // If the next section in our ring is empty, go to
                            // sleep
                            // until a new section is available
                            try
                            {
                                lock.wait();
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    }
                }
                // This section filter has been terminated, so reset our ring
                // index
                ringIndex = 0;
            }
        }

        // Current filter state
        private int state = FILTER_STATE_STOPPED;

        private String message = ""; // Debug message

        int pid = -1;

        int ringIndex = 0;

        int outstandingSections = 0;

        RingSectionFilter filter;

        Object lock = new Object();
    }

    // /////////////////////////////////////////////////////////////////////////////
    // GUI COMPONENTS //
    // /////////////////////////////////////////////////////////////////////////////

    // GUI Component
    private SFTestUI m_testInfo = new SFTestUI();

    /**
     * This class represents the main visual for the test xlet. It displays all
     * section filters and filter groups created by the xlet.
     */
    private class SFTestUI extends Container
    {
        public SFTestUI()
        {
            super();
            setBackground(Color.black);
            setForeground(Color.white);
            setFont(new Font("tiresias", Font.PLAIN, 14));
        }

        public void paint(Graphics g)
        {
            g.setColor(Color.white);

            // Draw outline
            g.drawRoundRect(2, 2, m_width - 4, m_height - 4, 15, 15);

            int dy = 14;
            int x = 4, y = 4 + dy;

            // App Name and priority
            g.drawString("<" + m_xletName + "> Priority = " + m_appPriority, x, y);
            x += 7;
            y += dy;

            // Group Willing-to-release and group priority policy. Newly created
            // groups
            // will have these values
            String str = "";
            str += (m_willingToRelease) ? "Willing" : "Not Willing";
            str += " / ";
            str += (m_priority) ? "High" : "Low";
            g.drawString(str, x, y);
            x -= 7;
            y += dy;

            y += 5;

            // Filters
            for (int i = 0; i < m_filterGroups.size(); ++i)
            {
                FilterGroup fg = (FilterGroup) m_filterGroups.elementAt(i);

                // Group title
                g.setColor(m_currentFilterGroup == i ? Color.yellow : Color.white);
                str = "Grp " + i + "--" + state2String(fg.state) + " (";
                str += (fg.willingToRelease) ? "W" : "NW";
                str += "/";
                str += (fg.priority) ? "High" : "Low";
                str += ")";
                g.drawString(str, x, y);
                y += dy;

                // Group debug message
                g.drawString(fg.message, x, y);
                x += 7;
                y += dy;

                // Info about each filter in this group
                for (int j = 0; j < fg.filters.size(); ++j)
                {
                    Filter filter = (Filter) fg.filters.elementAt(j);
                    synchronized (filter.lock)
                    {
                        // Filter title and state
                        g.setColor((m_currentFilterGroup == i && fg.currentFilter == j) ? Color.green : Color.white);
                        String pidString = (filter.pid != -1) ? ("0x" + Integer.toHexString(filter.pid)) : "";
                        g.drawString("F" + j + " -- " + pidString + " " + state2String(filter.getState()), x, y);
                        x += 7;
                        y += dy;

                        // Info message regarding this filter
                        g.drawString(filter.message, x, y);
                        y += dy;
                    }
                    x -= 7;
                }
                x -= 7;
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // XLET PRIVATE DATA //
    // /////////////////////////////////////////////////////////////////////////////

    private XletContext m_ctx;

    private String m_xletName;

    private AppID m_appID;

    private int m_appPriority;

    private HScene m_scene;

    // Scene screen position and size
    private int m_x = 0;

    private int m_y = 0;

    private int m_width = 640;

    private int m_height = 480;

    private static final int GROUP_STATE_DISCONNECTED = 1;

    private static final int GROUP_STATE_ATTACHED = 2;

    private static final int GROUP_STATE_DETACHED = 3;

    private static final int FILTER_STATE_RUNNING = 4;

    private static final int FILTER_STATE_RUN_PENDING = 5;

    private static final int FILTER_STATE_STOPPED = 7;

    private String state2String(int state)
    {
        switch (state)
        {
            case GROUP_STATE_DISCONNECTED:
                return "DISCONNECT";
            case GROUP_STATE_ATTACHED:
                return "ATTACHED";
            case GROUP_STATE_DETACHED:
                return "DETACHED";
            case FILTER_STATE_RUNNING:
                return "RUNNING";
            case FILTER_STATE_RUN_PENDING:
                return "RUN_PENDING";
            case FILTER_STATE_STOPPED:
                return "STOPPED";
            default:
                return "";
        }
    }

    // Our currently selected filter group
    private int m_currentFilterGroup = -1;

    // Resource and priority policy with which to create new filter groups
    private boolean m_willingToRelease = false;

    private boolean m_priority = true;

    // Event handler provided by the test runner via IXC
    private SFTestEvents m_eventHandler;

    private Vector m_filterGroups = new Vector();

    TVTimer repaintTimer = TVTimer.getTimer();

    TVTimerSpec repaintSpec = new TVTimerSpec();

    boolean m_timerRunning = false;
}
