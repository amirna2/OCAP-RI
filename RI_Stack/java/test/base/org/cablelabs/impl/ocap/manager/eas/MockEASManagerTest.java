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

import junit.framework.Assert;

import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.ocap.hardware.Host;

import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.util.SimpleCondition;

/**
 * An abstract class providing the interface and support code for EAS tests that
 * must be executed under the supervision of the {@link MockEASManager}. It also
 * extends {@link junit.framework.Assert} so the <code>assert</code> methods can
 * be used in the test code.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public abstract class MockEASManagerTest extends Assert implements Runnable
{
    /**
     * A basic implementation of a {@link Section} object implemented around a
     * <code>byte[]</code>.
     */
    public class MockEASSection extends Section
    {
        private volatile byte[] m_data;

        public MockEASSection(final byte[] buffer)
        {
            this.m_data = buffer;
        }

        public byte[] getData() throws NoDataAvailableException
        {
            if (null == this.m_data)
            {
                throw new NoDataAvailableException();
            }

            return this.m_data;
        }
    }

    // Instance Fields

    protected final MockEASManager m_mockEASManager;

    protected boolean m_oobMonitoring;

    protected final SimpleCondition m_startFiltering = new SimpleCondition(false);

    // Constructors

    /**
     * Constructs a new instance of this {@link MockEASManager} test class.
     * Note: <em>do not</em> set host power mode to full power in the
     * constructor as this would set the power mode too soon in the stack boot
     * sequence.
     * 
     * @param manager
     *            a callback reference to the {@link MockEASManager} and
     *            {@link EASManagerImpl} instance
     */
    protected MockEASManagerTest(final MockEASManager manager)
    {
        this.m_mockEASManager = manager;
    }

    // Instance Methods

    /**
     * Called after the {@link MockEASManagerTest} subclass is instantiated,
     * this method embodies the test class to be run under the supervisor of the
     * {@link MockEASManager}. The test is run asynchronously in the OCAP System
     * Context.
     * <p>
     * The <code>run()</code> should call {@link #waitForStartFiltering()} to
     * block test execution until {@link MockEASManager} signals to start EAS
     * section filtering. This typically occurs when a host power mode change
     * event from low to full power occurs (i.e. the power button is pressed).
     * <p>
     * <em>Bears repeating, test commences when the power key is pressed to change
     * the power mode to full power.</em>
     */
    public abstract void run();

    /**
     * Called during the stack boot process when the boot process needs to be
     * restarted in order to launch an <i>Initial Monitor Application</i>. This
     * method should return <code>false</code> if {@link EASManagerImpl} should
     * perform its normal boot process shutdown behavior (i.e., forcing a power
     * mode change to the low power mode).
     * <p>
     * This default implementation returns always returns <code>false</code>.
     * 
     * @return <code>true</code> if the test handled the boot process shutdown
     *         event; otherwise <code>false</code> for the EAS implementation to
     *         handle the event
     * @see EASManagerImpl#monitorApplicationShutdown(org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback)
     */
    protected boolean handleBootProcessShutdown()
    {
        return false;
    }

    /**
     * Called during the stack boot process prior to launching of <i>unbound
     * auto-start applications</i> after the <i>Initial Monitor Application</i>
     * has been launched and configured, if present. This method should return
     * <code>false</code> if {@link EASManagerImpl} should perform its normal
     * boot process started behavior (i.e., forcing a power mode change to the
     * current host power mode).
     * <p>
     * This default implementation returns always returns <code>false</code>.
     * 
     * @return <code>true</code> if the test handled the boot process started
     *         event; otherwise <code>false</code> for the EAS implementation to
     *         handle the event
     * @see EASManagerImpl#monitorApplicationStarted()
     */
    protected boolean handleBootProcessStarted()
    {
        return false;
    }

    /**
     * Called when a POD inserted, ready, or removed is received from the POD
     * manager. This method should return <code>false</code> if
     * {@link EASManagerImpl} should perform its normal POD event behavior
     * (i.e., restarting section table monitoring).
     * <p>
     * This default implementation returns always returns <code>false</code>.
     * 
     * @param event
     *            a {@link PODEvent} object
     * @return <code>true</code> if the test handled the POD event; otherwise
     *         <code>false</code> for the EAS implementation to handle the event
     * @see EASManagerImpl#notify(PODEvent)
     */
    protected boolean handlePODEvent(final PODEvent event)
    {
        return false;
    }

    /**
     * Called when the host power mode changes (for example from full to low
     * power). This method should return <code>false</code> if
     * {@link EASManagerImpl} should perform its normal power mode change
     * behavior (i.e., enabling/disabling EAS processing).
     * <p>
     * This default implementation returns always returns <code>false</code>.
     * 
     * @param newPowerMode
     *            either {@link Host#FULL_POWER} or {@link Host#LOW_POWER}
     * @return <code>true</code> if the test handled the power mode change;
     *         otherwise <code>false</code> for the EAS implementation to handle
     *         the power mode change
     * @see EASManagerImpl#powerModeChanged(int)
     */
    protected boolean handlePowerModeChanged(final int newPowerMode)
    {
        return false;
    }

    /**
     * Starts section table filtering. The test should start generating and
     * sending EAS section tables to
     * {@link EASManagerImpl#notify(boolean, Section) EASManagerImpl}.
     * 
     * @param oobAlert
     *            <code>true</code> if OOB section table monitoring was started;
     *            otherwise <code>false</code> if IB section table monitoring
     *            was started
     * @return <code>true</code> if the test started; otherwise
     *         <code>false</code>
     */
    protected boolean start(final boolean oobMonitoring)
    {
        this.m_oobMonitoring = oobMonitoring;
        this.m_startFiltering.setTrue();
        return true;
    }

    /**
     * Stops section table filtering that had been previously initiated by a
     * call to {@link #start(boolean)}. The test should stop generating and
     * sending EAS section tables to
     * {@link EASManagerImpl#notify(boolean, Section) EASManagerImpl}.
     */
    protected void stop()
    {
        this.m_startFiltering.setFalse();
    }

    /**
     * Waits for {@link EASManagerImpl} to indicate that section filtering has
     * started via {@link #start(boolean)}. The test should invoke this method
     * at the start of the {@link #run()} implementation to synchronize test
     * execution with the completion of EAS initialization and the enabling of
     * EAS processing.
     * 
     * @throws InterruptedException
     *             if the waiting thread is interrupted
     */
    protected void waitForStartFiltering() throws InterruptedException
    {
        this.m_startFiltering.waitUntilTrue();
    }
}
