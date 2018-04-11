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

package org.davic.mpeg.sections;

import junit.framework.TestCase;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedServiceMgr;

/**
 * The <code>SectionFilterTestBase</code> class provides all section filter unit
 * test subclasses with custom listeners for each type of section filter
 * (simple, ring, and table). This class also provides the setup/teardown
 * methods that install/uninstall the test managers for section filtering and
 * network interface management
 * 
 * @author Greg Rutz
 */
public class SectionFilterTestBase extends TestCase
{
    public SectionFilterTestBase(String name)
    {
        super(name);
    }

    /**
     * Install the test managers.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // Switch to using our test section filter manager
        oldSFMgr = (SectionFilterManager) ManagerManager.getInstance(SectionFilterManager.class);
        ManagerManagerTest.updateManager(SectionFilterManager.class, TestSectionFilterManager.class, false,
                new TestSectionFilterManager());

        // Switch to using our canned network interface manager
        oldNetMgr = (NetManager) ManagerManager.getInstance(NetManager.class);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, false, new CannedNetMgr());

        // Switch to using our canned service manager
        oldSvcMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, false,
                CannedServiceMgr.getInstance());
    }

    /**
     * Unistall the test managers, replacing them with the original managers
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();

        // Replace the section filter manager originally setup by the stack
        ManagerManagerTest.updateManager(SectionFilterManager.class, oldSFMgr.getClass(), false, oldSFMgr);

        // Replace the network interface manager originally setup by the stack
        ManagerManagerTest.updateManager(NetManager.class, oldNetMgr.getClass(), false, oldNetMgr);

        // Replace the service manager originally setup by the stack
        ManagerManagerTest.updateManager(ServiceManager.class, oldSvcMgr.getClass(), false, oldSvcMgr);
    }

    /**
     * Helper function for retrieving the current
     * <code>SectionFilterManager</code>
     * 
     * @return the current <code>SectionFilterManager</code>
     */
    protected TestSectionFilterManager getTestManager()
    {
        return (TestSectionFilterManager) ManagerManager.getInstance(SectionFilterManager.class);
    }

    // Cache variables for our original manager instances
    private SectionFilterManager oldSFMgr;

    private NetManager oldNetMgr;

    private ServiceManager oldSvcMgr;

    /**
     * This class starts a simple thread that will retrieve a single filtered
     * section.
     * 
     * @author Greg Rutz
     */
    protected class SimpleSectionFilterListener extends TestSectionFilterListener
    {
        /**
         * Construct a <code>SimpleSectionFilterListener</code>
         * 
         * @param filter
         *            the <code>SimpleSectionFilter</code> to listen to
         * @param name
         *            the name to assign to the listener thread
         */
        public SimpleSectionFilterListener(SectionFilterGroup filterGroup, final SimpleSectionFilter filter, String name)
        {
            super(filterGroup, filter, name);

            Thread listener = new Thread(name)
            {
                public void run()
                {
                    try
                    {
                        Section s = filter.getSection();
                        if (shouldFreeSections) freeSection(s);
                    }
                    catch (FilteringInterruptedException e)
                    {
                    }
                }
            };
            listener.start();
        }

        /**
         * Section filter event handler. According to the davic spec,
         * SimpleSectionFilters do not use EndOfFilteringEvent.
         * 
         * @param event
         *            the section filter event
         */
        public synchronized void sectionFilterUpdate(SectionFilterEvent event)
        {
            super.sectionFilterUpdate(event);
        }
    }

    /**
     * This class represents a custom listener for table section filters. This
     * listener does not use a thread because all parts of the table will be
     * filtered and stored in the section filter. There is no need to release
     * sections as they are filtered.
     * 
     * @author Greg Rutz
     */
    protected class TableSectionFilterListener extends TestSectionFilterListener
    {
        /**
         * Construct a <code>TableSectionFilterListener</code>
         * 
         * @param filter
         *            the <code>TableSectionFilter</code> to listen to
         */
        public TableSectionFilterListener(SectionFilterGroup filterGroup, TableSectionFilter filter)
        {
            super(filterGroup, filter, "");
        }

        /**
         * Section filter event handler. For table section filters the
         * <code>EndOfFilteringEvent</code> signifies that the last section of
         * the table has been received.
         */
        public synchronized void sectionFilterUpdate(SectionFilterEvent event)
        {
            super.sectionFilterUpdate(event);
            // don't call section received on eof event
            // code should issue a section available event for last section.
        }
    }

    /**
     * The <code>RingSectionFilterListener</code> starts a thread that retrieves
     * filtered sections from the ring and frees them.
     * 
     * @author Greg Rutz
     */
    protected class RingSectionFilterListener extends TestSectionFilterListener
    {
        /**
         * 
         * Construct a <code>RingSectionFilterListener</code>
         * 
         * @param filter
         *            the <code>RingSectionFilter</code> to listen to
         * @param ringSize
         *            the size of the ring filter
         * @param name
         *            the name to assign to the listener thread
         */
        public RingSectionFilterListener(SectionFilterGroup filterGroup, final RingSectionFilter filter,
                final int ringSize, String name)
        {
            super(filterGroup, filter, name);

            listener = new Thread(name)
            {
                public void run()
                {
                    while (true)
                    {
                        if (!runThread) break;

                        synchronized (RingSectionFilterListener.this)
                        {
                            Section s = filter.getSections()[ringIndex];
                            if (s.getFullStatus())
                            {
                                if (shouldFreeSections) freeSection(s);
                                ringIndex = (ringIndex == ringSize - 1) ? 0 : ringIndex + 1;
                            }
                            else
                            {
                                try
                                {
                                    RingSectionFilterListener.this.wait();
                                }
                                catch (InterruptedException e)
                                {
                                }
                            }
                        }
                    }
                }
            };
            listener.start();
        }

        int ringIndex = 0;
    }

    /**
     * Basic section filter listener class that keeps track of received events
     * and counts the number of received sections
     * 
     * @author Greg Rutz
     */
    protected class TestSectionFilterListener implements SectionFilterListener, ResourceStatusListener
    {
        /**
         * Construct our <code>TestSectionFilterListener</code> and set our
         * thread name and filter. Also register as a listener of the section
         * filter
         * 
         * @param sf
         *            the section filter we will be listening to
         * @param name
         *            the name to be assigned to any listener thread that may be
         *            created
         */
        public TestSectionFilterListener(SectionFilterGroup sfg, SectionFilter sf, String name)
        {
            sectionFilter = sf;
            sectionFilterGroup = sfg;
            this.name = name;
            sf.addSectionFilterListener(this);
            sfg.addResourceStatusEventListener(this);
        }

        /**
         * Section filter event handler. Increments our section count when it
         * receives a section. Also sets flags when other sorts of events are
         * received
         */
        public synchronized void sectionFilterUpdate(SectionFilterEvent event)
        {
            if (event instanceof SectionAvailableEvent)
            {
                sectionAvailableEventReceived = true;
                sectionReceived();
            }
            if (event instanceof EndOfFilteringEvent)
            {
                endOfFilteringEventReceived = true;
            }

            if (event instanceof VersionChangeDetectedEvent)
            {
                versionChangeEventReceived = true;
            }

            if (event instanceof IncompleteFilteringEvent)
            {
                incompleteFilteringEventReceived = true;
            }

            if (event instanceof TimeOutEvent)
            {
                timeOutEventReceived = true;
            }

            this.notifyAll();
        }

        /**
         * Allows caller to choose whether or not this listener will free
         * sections as it receives them
         * 
         * @param free
         *            true if the listener should free incoming sections, false
         *            if it should ignore them (but still count them)
         */
        public synchronized void freeSections(boolean free)
        {
            shouldFreeSections = free;
        }

        /**
         * Handler resource status events from the filter group
         */
        public void statusChanged(ResourceStatusEvent event)
        {
            if (event instanceof ForcedDisconnectedEvent)
                forcedDisconnectedEventReceived = true;
            else if (event instanceof FilterResourcesAvailableEvent) filterResourcesAvailableEventReceived = true;
        }

        /**
         * Stop listening to our section filter and terminate any running
         * listener thread
         */
        public synchronized void stopListening()
        {
            sectionFilter.removeSectionFilterListener(this);
            sectionFilterGroup.removeResourceStatusEventListener(this);
            if (listener != null && listener.isAlive()) runThread = false;
            this.notifyAll();
        }

        public boolean haveReceivedTimeOutEvent()
        {
            return timeOutEventReceived;
        }

        public boolean haveReceivedIncompleteFilteringEvent()
        {
            return incompleteFilteringEventReceived;
        }

        public boolean haveReceivedVersionChangeEvent()
        {
            return versionChangeEventReceived;
        }

        public boolean haveReceivedEndOfFilteringEvent()
        {
            return endOfFilteringEventReceived;
        }

        public boolean haveReceivedSectionAvailableEvent()
        {
            return sectionAvailableEventReceived;
        }

        public boolean haveReceivedForcedDisconnectedEvent()
        {
            return forcedDisconnectedEventReceived;
        }

        public boolean haveReceivedFilterResourcesAvailableEvent()
        {
            return filterResourcesAvailableEventReceived;
        }

        public int getNumOutstandingSections()
        {
            return outstandingSections;
        }

        public int getNumSectionsReceived()
        {
            return numSectionsReceived;
        }

        /**
         * Releases the given section and decrements our outstanding section
         * count
         * 
         * @param s
         *            the section to be freed
         */
        public synchronized void freeSection(Section s)
        {
            if (s != null)
            {
                s.setEmpty();
                --outstandingSections;
            }
        }

        /**
         * Reset all event flags and section counts
         */
        public synchronized void reset()
        {
            outstandingSections = 0;
            numSectionsReceived = 0;
            shouldFreeSections = true;
            timeOutEventReceived = false;
            incompleteFilteringEventReceived = false;
            versionChangeEventReceived = false;
            endOfFilteringEventReceived = false;
            sectionAvailableEventReceived = false;
            forcedDisconnectedEventReceived = false;
            filterResourcesAvailableEventReceived = false;
        }

        /**
         * When a section is received we need to increment our outstanding
         * section count and our total number of sections received
         */
        protected synchronized void sectionReceived()
        {
            ++numSectionsReceived;
            ++outstandingSections;
        }

        // Section counters
        private int outstandingSections = 0;

        private int numSectionsReceived = 0;

        // Control whether or not section handler should release sections
        protected boolean shouldFreeSections = true;

        // Event flags
        private boolean timeOutEventReceived = false;

        private boolean incompleteFilteringEventReceived = false;

        private boolean versionChangeEventReceived = false;

        private boolean endOfFilteringEventReceived = false;

        private boolean sectionAvailableEventReceived = false;

        private boolean forcedDisconnectedEventReceived = false;

        private boolean filterResourcesAvailableEventReceived = false;

        protected SectionFilter sectionFilter = null;

        protected SectionFilterGroup sectionFilterGroup = null;

        protected String name = null;

        protected Thread listener = null;

        protected boolean runThread = true;
    }

    protected ResourceClient dummyResClient = new ResourceClient()
    {
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        public void release(ResourceProxy proxy)
        {
        }

        public void notifyRelease(ResourceProxy proxy)
        {
        }
    };

    protected int NUM_RING_FILTER_SECTIONS = 25;
}
