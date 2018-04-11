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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.davic.mpeg.sections.Section;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A mock {@link EASManagerImpl} implementation that provides a full OCAP stack
 * test environment for running emergency alert system (EAS) tests. These tests
 * typically require a fully-initialized stack in order to present video
 * streams.
 * <p>
 * The concept is that this mock manager would instantiate a specified test
 * class, and start it in its own thread with a reference to the mock manager.
 * That reference allows the test class to access the {@link EASManagerImpl}
 * public API as well as support methods in {@link MockEASManager}. The test
 * class could do whatever it needs to perform its test, but the mock manager
 * stays the same, thus providing a more stable test environment.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class MockEASManager extends EASManagerImpl
{
    /**
     * An instance of this class causes the test class to be started with a
     * parameter indicating that IB alert messages should be fed to the
     * {@link EASManagerImpl#notify(boolean, Section)} method.
     */
    private class EASInBandMonitor extends EASSectionTableMonitor
    {
        /**
         * Constructs a new instance of the receiver with the given
         * <code>listener</code> assigned to receive notifications of EAS
         * section table acquisition.
         * 
         * @param listener
         *            the {@link EASSectionTableListener} instance to receive
         *            EAS section table notifications
         */
        public EASInBandMonitor(final EASSectionTableListener listener)
        {
            super(); // Use default constructor to avoid setting up section
                     // filters in superclass.
            super.m_sectionTableListener = listener;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#dispose()
         */
        protected void dispose()
        {
            stop();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#
         * isOutOfBandAlert()
         */
        protected boolean isOutOfBandAlert()
        {
            return false; // always an in-band monitor
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#start()
         */
        protected boolean start()
        {
            return MockEASManager.this.start(isOutOfBandAlert());
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#stop()
         */
        protected void stop()
        {
            MockEASManager.this.stop();
        }
    }

    /**
     * An instance of this class causes the test class to be started with a
     * parameter indicating that OOB alert messages should be fed to the
     * {@link EASManagerImpl#notify(boolean, Section)} method.
     */
    private class EASOutOfBandMonitor extends EASSectionTableMonitor
    {
        /**
         * Constructs a new instance of the receiver with the given
         * <code>listener</code> assigned to receive notifications of EAS
         * section table acquisition.
         * 
         * @param listener
         *            the {@link EASSectionTableListener} instance to receive
         *            EAS section table notifications
         */
        public EASOutOfBandMonitor(EASSectionTableListener listener)
        {
            super(); // Use default constructor to avoid setting up section
                     // filters in superclass.
            super.m_sectionTableListener = listener;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#dispose()
         */
        protected void dispose()
        {
            stop();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#
         * isOutOfBandAlert()
         */
        protected boolean isOutOfBandAlert()
        {
            return true; // always an out-of-band monitor
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#start()
         */
        protected boolean start()
        {
            return MockEASManager.this.start(isOutOfBandAlert());
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#stop()
         */
        protected void stop()
        {
            MockEASManager.this.stop();
        }
    }

    // Class Constants

    public static final String MPEENV_MOCK_TEST_CLASS = "OCAP.eas.mock.testClass";

    // Class Methods

    /**
     * Returns a new instance of this class to satisfy the basic {@link Manager}
     * contract. <code>ManagerManager</code> ensures this instance is treated as
     * a singleton instance.
     * 
     * @return a new instance of the receiver
     */
    public static Manager getInstance()
    {
        return new MockEASManager();
    }

    // Instance Fields

    private final CallerContext m_systemContext;

    private final MockEASManagerTest m_testClass;

    // Constructors

    /**
     * Constructs a new instance of the receiver and starts the
     * {@link MockEASManagerTest} subclass in an asynchronous task if one was
     * specified by the "<code>OCAP.eas.mock.testClass</code>" MPE environment
     * variable.
     */
    protected MockEASManager()
    {
        this.m_systemContext = ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getSystemContext();
        this.m_testClass = getMockTestClass();

        // Start the test class if one was specified.
        if (null != this.m_testClass)
        {
            this.m_systemContext.runInContextAsync(this.m_testClass);
        }
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.pod.PODListener#notify(org.cablelabs.impl.
     * manager.pod.PODEvent)
     */
    public void notify(PODEvent event)
    {
        if (null == this.m_testClass || !this.m_testClass.handlePODEvent(event))
        {
            super.notify(event);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.PowerModeChangeListener#powerModeChanged(int)
     */
    public void powerModeChanged(int newPowerMode)
    {
        if (null == this.m_testClass || !this.m_testClass.handlePowerModeChanged(newPowerMode))
        {
            super.powerModeChanged(newPowerMode);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback#shutdown
     * (org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback)
     */
    public boolean monitorApplicationShutdown(ShutdownCallback callback)
    {
        if (null == this.m_testClass || !this.m_testClass.handleBootProcessShutdown())
        {
            return super.monitorApplicationShutdown(callback);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void monitorApplicationStarted()
    {
        if (null == this.m_testClass || !this.m_testClass.handleBootProcessStarted())
        {
            super.monitorApplicationStarted();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void initialUnboundAutostartApplicationsStarted()
    {
        // Nothing to do (only care about monitor app startup)
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createInBandMonitor()
     */
    protected EASSectionTableMonitor createInBandMonitor()
    {
        return new EASInBandMonitor(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createOutOfBandMonitor
     * ()
     */
    protected EASSectionTableMonitor createOutOfBandMonitor()
    {
        return new EASOutOfBandMonitor(this);
    }

    /**
     * Gets an instance of the {@link MockEASManagerTest} subclass specified by
     * the "<code>OCAP.eas.mock.testClass</code>" MPE environment variable.
     * 
     * @return the instance of the specified {@link MockEASManagerTest}
     *         subclass, or <code>null</code> if an instance could not be
     *         created
     */
    private MockEASManagerTest getMockTestClass()
    {
        Class[] parameterTypes = new Class[] { MockEASManager.class };
        Object[] parameters = new Object[] { this };
        MockEASManagerTest test = null;
        String testName = MPEEnv.getEnv(MockEASManager.MPEENV_MOCK_TEST_CLASS, MockEASManagerStub.class.getName());

        if (null != testName && 0 != testName.length())
        {
            try
            {
                SystemEventUtil.logEvent("Instantiating test:<" + testName + ">");
                Class testClass = Class.forName(testName);
                Constructor testCtor = testClass.getConstructor(parameterTypes);
                test = (MockEASManagerTest) testCtor.newInstance(parameters);
            }
            catch (ClassNotFoundException e)
            {
                SystemEventUtil.logRecoverableError("Can't find test class:<" + testName + ">", e);
            }
            catch (NoSuchMethodException e)
            {
                SystemEventUtil.logRecoverableError("Can't find required constructor:<" + testName + ">", e);
            }
            catch (IllegalArgumentException e)
            {
                SystemEventUtil.logRecoverableError(testName + " constructor arguments are incorrect", e);
            }
            catch (InstantiationException e)
            {
                SystemEventUtil.logRecoverableError(testName + " is an abstract class", e);
            }
            catch (IllegalAccessException e)
            {
                SystemEventUtil.logRecoverableError(testName + " constructor is inaccessible", e);
            }
            catch (InvocationTargetException e)
            {
                SystemEventUtil.logRecoverableError(testName + " constructor threw an exception", e);
                e.printStackTrace();
            }
            catch (ClassCastException e)
            {
                SystemEventUtil.logRecoverableError(testName + " does not extend MockEASManagerTest", e);
            }
        }

        return test;
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
    private boolean start(final boolean oobAlert)
    {
        if (null != this.m_testClass)
        {
            return this.m_testClass.start(oobAlert);
        }
        else
        {
            return false;
        }
    }

    /**
     * Stops section table filtering that had been previously initiated by a
     * call to {@link #start(boolean)}. The test should stop generating and
     * sending EAS section tables to
     * {@link EASManagerImpl#notify(boolean, Section) EASManagerImpl}.
     */
    private void stop()
    {
        if (null != this.m_testClass)
        {
            this.m_testClass.stop();
        }
    }
}
