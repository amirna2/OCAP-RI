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
package org.cablelabs.xlet.providerregistry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.mpeg.ElementaryStream;
import org.davic.net.InvalidLocatorException;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.io.ixc.IxcRegistry;
import org.dvb.spi.ProviderFailedInstallationException;
import org.dvb.spi.ProviderRegistry;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.ServiceReference;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.havi.ui.event.HKeyEvent;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractServiceType;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.media.TimeShiftControl;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

public class ServiceProviderTestXlet implements Driveable, Xlet, KeyListener, RecordingPlaybackListener, TimeShiftListener,
             ControllerListener
{
    private static final long WAIT_TIME = 20000;

    protected XletContext ctx;

    protected AutoXletClient axc;

    protected Logger log;

    protected Test test;

    private SIManager siManager = null;

    private ServiceList list;

    private ServiceContext serviceContext;

    private SCListener scListener;

    private String testXletName;

    private Service normalService1;

    private Service normalService2;

    private Service normalService3;
    
    private Service normalService4;
    
    private Service normalService5;

    private String knownLocatorString = "ocap://0x9990";

    private String unknownLocatorString = "ocap://0x9991";
    
    private String unknownLocatorString2 = "ocap://0x9992";

    private HScene scene;

    private VidTextBox vbox = null;

    private ServiceFilter abstractFilter = new ServiceFilter()
    {

        public boolean accept(Service service)
        {
            return !service.getServiceType().equals(AbstractServiceType.OCAP_ABSTRACT_SERVICE);
        }

    };

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        keyPressed(event);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
    }

    public void pauseXlet()
    {
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        try
        {
            this.ctx = ctx;

            String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));
            if (args.length < 1)
            {
                throw new XletStateChangeException("Test Xlet was not specified");
            }
            testXletName = args[0];

            // Initialize AutoXlet framework client and grab logger and test
            // objects
            axc = new AutoXletClient(this, ctx);
            test = axc.getTest();

            // If we have successfully connected, initialize our logger from the
            // AutoXletClient, else use a default constructed XletLogger which
            // will
            // send all logging output to standard out.
            if (axc.isConnected())
            {
                log = axc.getLogger();
            }
            else
            {
                log = new XletLogger();
            }

            siManager = SIManager.createInstance();
            list = siManager.filterServices(null);

            ServiceList filtered = siManager.filterServices(abstractFilter);
            if (filtered.size() <= 3)
            {
                throw new XletStateChangeException("Service List is not big enough!");
            }

            OcapLocator loc1;
            OcapLocator loc2;
            OcapLocator loc3;
            OcapLocator loc4;
            OcapLocator loc5;

            // According to Java doc for valid service reference (SelectionProviderInstance.java)
            // the actual locator used in SDV can only contain freq and program number (no modulation mode)
            // I.e transport dependent form of fpq locator
            /**
             * ServiceReference for compliance with ECN 1102
             * <p>
             * From the ECN: This subsection complies with [DVB-GEM 1.0.2] Section 14.9
             * which and defines a standardized textual representation for transport
             * independent locators. The following assertions are made for transport
             * independent and transport dependent locators.
             * <ul>
             * <li>A transport independent locator SHALL be based on a source_id term.</li>
             * 
             * <li>A transport dependent locator SHALL be based on a source_id,
             * service_name, or frequency.program_number terms.</li>
             * 
             * <li>All service objects in the SI database returned the from
             * javax.tv.service.SIManager SHALL be transport dependent.</li>
             * </ul>
             * <p>
             * In addition, this section extends [DVB-GEM 1.0.2] and defines an actual
             * locator format used in the [DVB-MHP 1.1] provider SPI. The format of
             * these locators SHALL contain a frequency and program_number term in order
             * to be considered properly formatted.
             * 
             * Example valid TI locator:
             * <ul>
             * <li>ocap://0x0b12
             * <dd>Identify by source ID</li>
             * </ul>
             * 
             * Example valid TD locators:
             * <ul>
             * <li>ocap://0x0b12
             * <dd>Identify by source ID</li>
             * <li>ocap://n=Fox
             * <dd>Identify by source name</li>
             * <li>ocap://f=0x2254600.0x01
             * <dd>Identify by frequency and program number</li>
             * </ul>
             * 
             * Example invalid TI or TD locators:
             * <ul>
             * <li>ocap://f=0x2254600.0x01.m=0x0A
             * <dd>Identify by frequency and program number with modulation format
             * <li>ocap://oobfdc.0x01
             * <dd>Identify out-of-band servce by program number</li>
             * </ul>
             */
            // Use the following transport dependent locators
            // to map SDV services
            // TODO: Instead of hard-coding the services (these are RI specific)
            // read them from hostapp.properties or config.properties
            
            loc1 = new OcapLocator(0x45a); // sourceId 0x45a, ocap://f=0x1AA4ADC0.0x1 (baby video)
            loc2 = new OcapLocator("ocap://f=0x1D258C40.0x2"); // sourceId 0x44c (golf video)
            loc3 = new OcapLocator("ocap://f=0x26CD78C0.0x1"); // Clouds video
            loc4 = new OcapLocator("ocap://f=0x29A9E4C0.0x6588"); // Table tennis video
            loc5 = new OcapLocator("ocap://f=0x29A9E4C0.0x22");   // Bogus service
            
            normalService1 = siManager.getService(loc1);
            normalService2 = siManager.getService(loc2);
            normalService3 = siManager.getService(loc3);
            normalService4 = siManager.getService(loc4);  
            normalService5 = siManager.getService(loc5);
                           
            // get the recording manager
            m_recordingManager = (OcapRecordingManager) OcapRecordingManager.getInstance();

            m_recordingManager.addRecordingPlaybackListener(this);

            m_tsbLoggingTimerWentOffListener = new TSBLoggingTimerWentOffListener();
            m_tsbLoggingTimer = TVTimer.getTimer();
            m_tsbLoggingTimerSpec = new TVTimerSpec();
            m_tsbLoggingTimerSpec.setAbsolute(false); 
            m_tsbLoggingTimerSpec.setTime(1000); 
            m_tsbLoggingTimerSpec.setRepeat(true); 
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            throw new XletStateChangeException(exc.getMessage());
        }
    }

    private void menu()
    {
        log("Guide - Menu");
        log("1. Service Selection Using an installed provider");
        log("2. Service selection using installed provider, check provider first");
        log("3. Service selection using installed provider, check provider last");
        log("4. Service selection with multiple installed providers");
        log("5. Update Service Information");
        log("6. Update Service Information while selected");
        log("7. Remove service while selected");
        log("8. Unregister provider while selected");
        log("9. Record SPI service");
        log("0. Remove service while recording in progress");
    }
    
    public void startXlet() throws XletStateChangeException
    {
        setupUI();
        try
        {
            serviceContext = ServiceContextFactory.getInstance().createServiceContext();
            scListener = new SCListener();
            serviceContext.addListener(scListener);
            
            if (serviceContext instanceof TimeShiftProperties)
            {
                // Duration is in seconds
                ((TimeShiftProperties) serviceContext).setMinimumDuration(MIN_TSB_DURATION);
                ((TimeShiftProperties) serviceContext).setMaximumDuration(MAX_TSB_DURATION);
            }
            
            menu();
            
            // TODO - add more test cases
            // Map SDV service to something that does not exist
            // Map SDV service to something that exists but has no content
            //testUpdatingServiceInformationWhileSelected(); 
            /*
            // Run one test case at a time (via guide button which will display menu)
            
            testServiceSelectionUsingInstalledProvider(); 
            testServiceSelectionUsingInstalledProviderFirst(); 
            testServiceSelectionUsingInstalledProviderNotFirst(); 
            testServiceSelectionWithMultipleInstalledProviders(); 
            testUpdatingServiceInformation(); 
            testUpdatingServiceInformationWhileSelected(); 
            testRemovingServicesWhileSelected(); 
            testUnregisterServiceWhileSelected(); 
            
            // DVR tests
            testRecordSPIService(); // 1. single tuner ServiceContext recording
                                    // 2. dual tuners ServiceContext selection(service 1) 
                                    //    and background (service 2) recording
            testRemovingSPIServiceWhileRecording(); // Recording enters IN_PROGRESS_WITH_ERROR when service
                                                    // was removed and eventually end in INCOMPLETE_STATE

            showResults();
            */
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            throw new XletStateChangeException("Could not start the xlet" + exc);
        }
    }

    /**
     * Install a SelectionProvider using locator urls that do not exist in the
     * service list. Run through some different selection scenarios
     */
    private void testServiceSelectionUsingInstalledProvider()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            // According to OCAP section 16.2.1.16 (Service Identification)
            // SPI 'actual locator' should contain freq, program number...
            // normalService1 (ocap://f=0x1AA4ADC0.0x1)
            // normalService2 (ocap://f=0x1D258C40.0x2)
            // normalService3 (ocap://f=0x26CD78C0.0x1)
            // normalService4 (ocap://f=0x29A9E4C0.0x6588)
            // Mapping knownLocatorString(ocap://0x9990) to one of the normal services
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService2.getLocator());
           
            // Mapping unknownLocatorString(ocap://0x9991) to ??? (normalService4)             
            // actual locator for normalService4 (ocap://f=0x29A9E4C0.0x6588) 
            // should be returned when select is called
            ServiceReference unknownReference = new ServiceReference(unknownLocatorString, unknownLocatorString);
            
            TestSelectionProvider.ServiceInformation[] sInfoArr = new TestSelectionProvider.ServiceInformation[] {
                    new TestSelectionProvider.ServiceInformation(normalService2, knownReference),
                    new TestSelectionProvider.ServiceInformation(normalService4, unknownReference), };
            
            selectionProvider = new TestSelectionProvider(test, sInfoArr, true);
            
            log("selectionProvider: " + selectionProvider);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);

            log("Verifying Services");
            verifyServiceProviderServices(selectionProvider);

            ServiceList serviceList = siManager.filterServices(null);
            Service knownLocationSPIService = getServiceWithLocatorStr(serviceList, knownLocatorString);
            Service unknownLocationSPIService = getServiceWithLocatorStr(serviceList, unknownLocatorString);
            
            log("knownLocationSPIService: " + knownLocationSPIService);
            log("unknownLocationSPIService: " + unknownLocationSPIService);
            
            //
            // test the transition from a normal service to an spi service of
            // known location
            //
            //selectServiceAndValidate(normalService1);
            selectServiceAndValidate(knownLocationSPIService);
            sleepTime(10000);
            selectServiceAndValidate(unknownLocationSPIService);
            /*
            selectServiceAndValidate(knownLocationSPIService);

            //
            // test the transition from an spi service of known location to
            // a normal service
            //
            selectServiceAndValidate(knownLocationSPIService);
            selectServiceAndValidate(normalService1);

            //
            // test the transition from an spi service of unknown location to
            // a normal service
            // 
            selectServiceAndValidate(unknownLocationSPIService);
            selectServiceAndValidate(normalService1);

            //
            // test the transition from a known location spi service to an
            // unknown location spi service
            //
            selectServiceAndValidate(knownLocationSPIService);
            selectServiceAndValidate(unknownLocationSPIService);

            //
            // test the transition from a unknown location spi service to an
            // known location spi service
            //
            selectServiceAndValidate(unknownLocationSPIService);
            selectServiceAndValidate(knownLocationSPIService);

            testServicesAvailableInAnotherXlet();
            */
        }
        catch (Exception exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            
            if (selectionProvider != null)            
            {
                //ProviderRegistry.getInstance().unregister(selectionProvider);
            }            
        }

    }

    /**
     * Install a ServiceProvider that adds a service with the same locator as a
     * service in the service list. Specify that the service provider should be
     * checked before the other providers. The provider should provide the
     * service to the system since the provider should be queried before the
     * rest of the system.
     */
    private void testServiceSelectionUsingInstalledProviderFirst()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference unknownReference = new ServiceReference(unknownLocatorString, unknownLocatorString);
            TestSelectionProvider.ServiceInformation[] sInfoArr = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService2, unknownReference), };
            selectionProvider = new TestSelectionProvider(test, sInfoArr, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            log("Finding service");
            ServiceList serviceList = siManager.filterServices(null);
            Service service = getServiceWithLocatorStr(serviceList, unknownLocatorString);
            log("Selecting");
            
            selectServiceAndValidate(service);
            sleepTime(10000);
            test.assertTrue("Selection provider should have been called",
            selectionProvider.wasNewSessionCalled());
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }

    /**
     * Install a ServiceProvider that adds a service with the same locator as a
     * service in the service list. Specify that the service provider should be
     * checked after the other providers. The provider should not provide the
     * service to the system since the service should be found in the system
     * first.
     */
    private void testServiceSelectionUsingInstalledProviderNotFirst()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            String locatorStr = normalService2.getLocator().toExternalForm();
            ServiceReference unknownReference = new ServiceReference(locatorStr, locatorStr);
            TestSelectionProvider.ServiceInformation[] sInfoArr = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService3, unknownReference), };
            selectionProvider = new TestSelectionProvider(test, sInfoArr, false);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            log("Finding service");
            ServiceList serviceList = siManager.filterServices(null);
            Service service = getServiceWithLocatorStr(serviceList, locatorStr);
            log("selecting");
            selectServiceAndValidate(service);
            test.assertTrue("Selection provider should not have been called", !selectionProvider.wasNewSessionCalled());
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }
    
    /**
     * Install a service provider, verify that its services are there. Update
     * the service information to remove the old service and add a new one.
     * Verify the new service is present and the old one no longer is.
     */
    private void testUpdatingServiceInformation()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference reference1 = new ServiceReference(knownLocatorString, knownLocatorString);
            ServiceReference reference2 = new ServiceReference(unknownLocatorString, unknownLocatorString);
            // Map unknownLocatorString to normalService3
            TestSelectionProvider.ServiceInformation[] sInfoArr1 = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService3, reference2), };
            // knownLocatorString is unmapped
            selectionProvider = new TestSelectionProvider(test, sInfoArr1, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            
            log("Filtering services");
            ServiceList serviceList = siManager.filterServices(null);
            Service service1 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            Service service2 = getServiceWithLocatorStr(serviceList, unknownLocatorString);
            test.assertTrue(service1 == null); // knownLocatorString is unmapped
            test.assertTrue(service2 != null); // unknownLocatorString mapped to normalService3
            log("service1: " + service1);
            log("service2: " + service2);
            
            log("Updating service information");
            // knownLocatorString mapped to normalService4
            TestSelectionProvider.ServiceInformation[] sInfoArr2 = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService4, reference1), };
            selectionProvider.setServiceInformation(sInfoArr2);
            log("Filtering Services");
            serviceList = siManager.filterServices(null);
            service1 = getServiceWithLocatorStr(serviceList, unknownLocatorString);
            service2 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(service1 == null); // unknownLocatorString is unmapped
            test.assertTrue(service2 != null); // knownLocatorString mapped to normalService4
            log("service1: " + service1);
            log("service2: " + service2);
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }

    /**
     * Install a service provider, verify that its services are there. Select a
     * service. Once selection is complete, change the service location and
     * verify a reselection takes place
     */
    private void testUpdatingServiceInformationWhileSelected()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService2.getLocator());
            ServiceReference unknownReference = new ServiceReference(unknownLocatorString, unknownLocatorString);
            TestSelectionProvider.ServiceInformation[] sInfoArr1 = new TestSelectionProvider.ServiceInformation[] { 
            new TestSelectionProvider.ServiceInformation(normalService2, knownReference)
            /*, new TestSelectionProvider.ServiceInformation(normalService4, unknownReference)*/};

            selectionProvider = new TestSelectionProvider(test, sInfoArr1, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            log("Filtering services");
            ServiceList serviceList = siManager.filterServices(null);
            Service service1 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(service1 != null);

            // select normalService2
            selectServiceAndValidate(service1);

            sleepTime(10000);

            scListener.reset(); 
            // switch to normalService5 is a bogus service (should see altContent)
            log("Updating service information from " + normalService2.getLocator().toExternalForm() + " to "
                    + normalService5.getLocator().toExternalForm());
            knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService5.getLocator());

            selectionProvider.updateService(knownReference, normalService5);

            log("Filtering services");
            serviceList = siManager.filterServices(null);
            Service service2 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(service2 != null);          
            
            sleepTime(10000);

            scListener.reset(); 
            // switch to normalService3 
            log("Updating service information from " + normalService5.getLocator().toExternalForm() + " to "
                    + normalService3.getLocator().toExternalForm());
            knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService3.getLocator());

            selectionProvider.updateService(knownReference, normalService3);

            log("Filtering services");
            serviceList = siManager.filterServices(null);
            Service service3 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(service3 != null);          
            
            sleepTime(10000); 
            
            log("End of test");
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }

    /**
     * Install a service provider, verify that its services are there. Select a
     * service. Once selection is complete, remove the services provided.
     */
    private void testRemovingServicesWhileSelected()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService3.getLocator());
            TestSelectionProvider.ServiceInformation[] sInfoArr1 = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService3, knownReference), };

            selectionProvider = new TestSelectionProvider(test, sInfoArr1, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            log("Filtering services");
            ServiceList serviceList = siManager.filterServices(null);
            Service service1 = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(service1 != null);

            selectServiceAndValidate(service1);
            sleepTime(10000);
            scListener.reset();

            log("Updating service information");
            TestSelectionProvider.ServiceInformation[] sInfoArr2 = {};
            selectionProvider.setServiceInformation(sInfoArr2);
            // Per ECN 1037 altContentErrorEvent is posted 
            boolean received = scListener.waitForAlternativeContentErrorEvent();
            test.assertTrue("Did not end presentation on removal of service", received);
            serviceList = siManager.filterServices(null);
            Service removedService = getServiceWithLocatorStr(serviceList, knownLocatorString);
            log("removedService: " + removedService);
            test.assertTrue("The service should have been removed from the service list", removedService == null);
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }


    /**
     * Install multiple service providers and select the services from them.
     * This test has both providers provide the same service, while the spec
     * doesn't specify which provider will be called, the service should be
     * provided.
     */
    private void testServiceSelectionWithMultipleInstalledProviders()
    {
        TestSelectionProvider selectionProvider1 = null;
        TestSelectionProvider selectionProvider2 = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            String locator1 = "ocap://0x9990";
            String locator2 = "ocap://0x9991";
            String locator3 = "ocap://0x9992";

            ServiceReference ref1 = new KnownServiceReference(locator1, locator1, normalService2.getLocator());
            ServiceReference ref2 = new KnownServiceReference(locator2, locator2, normalService3.getLocator());
            ServiceReference ref3 = new KnownServiceReference(locator3, locator3, normalService4.getLocator());
            TestSelectionProvider.ServiceInformation[] sInfoArr1 = new TestSelectionProvider.ServiceInformation[] {
                    new TestSelectionProvider.ServiceInformation(normalService2, ref1),
                    new TestSelectionProvider.ServiceInformation(normalService4, ref3), };
            TestSelectionProvider.ServiceInformation[] sInfoArr2 = new TestSelectionProvider.ServiceInformation[] {
                    new TestSelectionProvider.ServiceInformation(normalService3, ref2),
                    new TestSelectionProvider.ServiceInformation(normalService4, ref3), };
            selectionProvider1 = new TestSelectionProvider(test, sInfoArr1, true);
            selectionProvider2 = new TestSelectionProvider(test, sInfoArr2, true);

            ProviderRegistry.getInstance().registerSystemBound(selectionProvider1);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider2);

            log("Filtering services");
            ServiceList spiList = siManager.filterServices(null);
            Service service1 = getServiceWithLocatorStr(spiList, locator1);
            Service service2 = getServiceWithLocatorStr(spiList, locator2);
            Service service3 = getServiceWithLocatorStr(spiList, locator3);

            log("Selecting service1: " + service1);
            selectServiceAndValidate(service1);
            sleepTime(10000);          
            log("Selecting service2: " + service2);
            selectServiceAndValidate(service2);
            sleepTime(10000);        
            log("Selecting service3: " + service3);
            selectServiceAndValidate(service3);
            // we don't know which provider is actually providing the service
            // here
            sleepTime(10000);
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider1);
            //ProviderRegistry.getInstance().unregister(selectionProvider2);
        }
    }

    private void testUnregisterServiceWhileSelected()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService3.getLocator());
            TestSelectionProvider.ServiceInformation[] sInfoArr = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService3, knownReference), };
            selectionProvider = new TestSelectionProvider(test, sInfoArr, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);

            log("Finding service");
            ServiceList serviceList = siManager.filterServices(null);
            Service service = getServiceWithLocatorStr(serviceList, knownLocatorString);
            log("Selecting");
            selectServiceAndValidate(service);
            sleepTime(10000);
            log("Unregistering");
            ProviderRegistry.getInstance().unregister(selectionProvider);
            selectionProvider = null;
            // This is a variation of test testRemovingServicesWhileSelected() 
            // PresentationTerminatedEvent is not posted 
            // Only altContent is posted 
            log("Waiting for alternate content error event..");
            boolean received = scListener.waitForAlternativeContentErrorEvent();
            test.assertTrue("Did not receice altContent..", received);

            serviceList = siManager.filterServices(null);
            Service removedService = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue("Service should no longer be in the service list", removedService == null);
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            test.fail(exc.getMessage());
        }
        finally
        {
            if (selectionProvider != null)
            {
                //ProviderRegistry.getInstance().unregister(selectionProvider);
            }
        }
    }

    /**
     * Install a SelectionProvider using locator urls that do not exist in the
     * service list. Run through some different selection scenarios
     */
    private void testRecordSPIService()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            // Mapping knownLocatorString(ocap://0x9990) to normalService3 (OcapLocator(651000000, 1, 16)
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService3.getLocator());
           
            // Mapping unknownLocatorString(ocap://0x9991) to ??? (normalService4)             
            // actual locator for normalService4  OcapLocator(699000000, 25992, 16) should be returned when select is called
            ServiceReference unknownReference = new ServiceReference(unknownLocatorString, unknownLocatorString);
            ServiceReference unknownReference2 = new ServiceReference(unknownLocatorString2, unknownLocatorString2);
            
            TestSelectionProvider.ServiceInformation[] sInfoArr = new TestSelectionProvider.ServiceInformation[] {
                    new TestSelectionProvider.ServiceInformation(normalService3, knownReference),
                    new TestSelectionProvider.ServiceInformation(normalService4, unknownReference),
                    new TestSelectionProvider.ServiceInformation(normalService2, unknownReference2), };
            
            selectionProvider = new TestSelectionProvider(test, sInfoArr, true);
            
            log("selectionProvider: " + selectionProvider);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);

            log("Verifying Services");
            verifyServiceProviderServices(selectionProvider);

            ServiceList serviceList = siManager.filterServices(null);
            Service knownLocationSPIService = getServiceWithLocatorStr(serviceList, knownLocatorString);
            Service unknownLocationSPIService = getServiceWithLocatorStr(serviceList, unknownLocatorString);
            Service unknownLocationSPIService2 = getServiceWithLocatorStr(serviceList, unknownLocatorString2);
            
            log("knownLocationSPIService: " + knownLocationSPIService);
            log("unknownLocationSPIService: " + unknownLocationSPIService);           
            log("unknownLocationSPIService2: " + unknownLocationSPIService2);  
            
            selectServiceAndValidate(knownLocationSPIService);
            //selectServiceAndValidate(unknownLocationSPIService);
            sleepTime(15000);
            // ServiceContext recording spec
            //recordAndWait(MEDIUM_RECORDING_LENGTH, unknownLocationSPIService, false);
            //sleepTime(15000);
            // Service recording spec (for background recording set numTuners to 2 in platform.cfg)
            // Otherwise will see a NofreeInterfaceException            
            recordAndWait(MEDIUM_RECORDING_LENGTH, unknownLocationSPIService, true);    
            sleepTime(10000);
            //recordAndWait(MEDIUM_RECORDING_LENGTH, unknownLocationSPIService2, true);
            //leepTime(25000); // recording 1 should be complete now
            //recordAndWait(SHORT_RECORDING_LENGTH, normalService1, true);
        }
        catch (Exception exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            if (selectionProvider != null)            
            {
                //ProviderRegistry.getInstance().unregister(selectionProvider);
            }
        }

    }
    
    private void testRemovingSPIServiceWhileRecording()
    {
        TestSelectionProvider selectionProvider = null;
        try
        {
            log("Registering Service Provider");
            test.assertTrue(true);
            scListener.reset();
            ServiceReference knownReference = new KnownServiceReference(knownLocatorString, knownLocatorString,
                    normalService3.getLocator());
            TestSelectionProvider.ServiceInformation[] sInfoArr1 = new TestSelectionProvider.ServiceInformation[] { new TestSelectionProvider.ServiceInformation(
                    normalService3, knownReference), };

            selectionProvider = new TestSelectionProvider(test, sInfoArr1, true);
            ProviderRegistry.getInstance().registerSystemBound(selectionProvider);
            log("Filtering services");
            ServiceList serviceList = siManager.filterServices(null);
            Service knownservice = getServiceWithLocatorStr(serviceList, knownLocatorString);
            test.assertTrue(knownservice != null);

            selectServiceAndValidate(knownservice);
            sleepTime(8000);   
            scListener.reset();
            
            recordAndWait(MEDIUM_RECORDING_LENGTH, knownservice, false);
            sleepTime(8000);
            
            log("Updating service information");
            TestSelectionProvider.ServiceInformation[] sInfoArr2 = {};
            selectionProvider.setServiceInformation(sInfoArr2);

            serviceList = siManager.filterServices(null);
            Service removedService = getServiceWithLocatorStr(serviceList, knownLocatorString);
            log("removedService: " + removedService);
            test.assertTrue("The service should have been removed from the service list", removedService == null);
        }
        catch (ProviderFailedInstallationException exc)
        {
            test.fail(exc.getMessage());
        }
        finally
        {
            //ProviderRegistry.getInstance().unregister(selectionProvider);
        }
    }
    
    public void recordAndWait(int seconds, Service service, boolean background)
    {
        class WaitForRecording implements Runnable
        {
            int m_waitTime;

            public WaitForRecording(int waitTime)
            {
                m_waitTime = waitTime;
            }

            public void run()
            {
                waitForRecordingToComplete(m_waitTime);
            }
        }

        if (true == doRecording(seconds, service, background))
        {
            new Thread(new WaitForRecording(seconds))
            {
            }.start();
        }
        else
        {
            log("Unable to start recording");
        }
    }
    
    public boolean doRecording(int length, Service service, boolean background)
    {
        boolean bRetVal = true;
        int retentionPriority;
        byte recordingPriority;
        int resourcePriority = 0;
        String recordingName = "TestoRecording";
        MediaStorageVolume m_msv = null;
        long expiration;
        expiration = 1000 * 60 * 60 * 24; // 24 hour expiration (expressed in
                                          // seconds)

        //log("doRecoring():entry");

        // start recording in 2 seconds
        long m_startTime = System.currentTimeMillis();// + 2000;

        // the duration needs to be in msec
        long duration = length * 1000;

        retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;

        try
        {
            OcapRecordingProperties orp;

            // establish the recording properties
            orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, // bitRate
                    expiration, // expirationPeriod
                    retentionPriority, // retentionPriority
                    recordingPriority, // priorityFlag
                    null, // access
                    null, // organization
                    m_msv, // destination
                    resourcePriority); // resourcePriority

            RecordingSpec rs;
            
            // formulate the recording request
            if (background)
            {
                rs = new ServiceRecordingSpec( service, new Date(m_startTime),
                                               duration, orp ); // recordingProperties (expiration time)
            }
            else
            {
                rs = new ServiceContextRecordingSpec( serviceContext, new Date(m_startTime),
                                                      duration, orp ); // recordingProperties (expiration time)
            }

            // submit the recording request
            m_recordingRequestCurrent = (OcapRecordingRequest) m_recordingManager.record(rs);

            if (m_recordingRequestCurrent != null)
            {
                log("****" + recordingName + " scheduled as " + m_recordingRequestCurrent.toString()
                        + "*****");
            }
        }
        catch (Exception e)
        {
            log("doRecording(): Record: FAILED");
            e.printStackTrace();
            log("doRecording(): Flagged FAILURE in Record due to rm.record() exception: "
                    + e.toString());
            bRetVal = false;
        }
        log("doRecording():exit");
        return bRetVal;
    }
    
    private void sleepTime(int time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException iex)
        {
            log("sleepyTime() interrupted");
        }
    }
    
    public boolean waitForRecordingToComplete(int recordingTime)
    {
        boolean bRetVal = false;
        boolean bDone = false;
        log("waitForRecording():entry");

        // wait for recording to complete (recording time + 3 seconds 'fudge
        // factor'
        int retries = recordingTime + 5;
        while (0 < retries--)
        {
            m_recordingState = m_recordingRequestCurrent.getState();
            switch (m_recordingState)
            {
                case LeafRecordingRequest.COMPLETED_STATE:
                    log("LeafRecordingRequest.COMPLETED_STATE");
                    bRetVal = true;
                    bDone = true;
                    break;

                case LeafRecordingRequest.DELETED_STATE:
                    log("LeafRecordingRequest.DELETED_STATE");
                    break;

                case LeafRecordingRequest.FAILED_STATE:
                    log("LeafRecordingRequest.FAILED_STATE");
                    bDone = true;
                    break;

                case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                    log("LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                    log("LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_STATE:
                    log("LeafRecordingRequest.IN_PROGRESS_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                    log("LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE");
                    break;

                case LeafRecordingRequest.INCOMPLETE_STATE:
                    log("LeafRecordingRequest.INCOMPLETE_STATE");
                    bDone = true;
                    break;

                case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                    log("LeafRecordingRequest.PENDING_NO_CONFLICT_STATE");
                    break;

                case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                    log("LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE");
                    break;

                default:
                    log("Received invalid request state");
                    break;
            }
            log("retries = " + retries);

            if (true == bDone)
            {
                break;
            }

            // sleep for 1 second
            sleepTime(1000);
        }

        // if we have waited long enough...
        if (-1 == retries)
        {
            // ...assume that it is not going to happen
            log("Recording not completed - time expired - stopping recording request");
            try
            {
                m_recordingRequestCurrent.cancel();
                log("Recording request stopped");
            }
            catch (Exception ex)
            {
                log("Exception occurred stopping recording request: " + ex);
                ex.printStackTrace();
            }
        }
        log("waitForRecording():exit, retValue = " + bRetVal);
        return bRetVal;
    }
    
    private void log(String message)
    {
        if (log != null)
        {
            log.log(message);
        }
        else
        {
            log(message);
        }

        if (vbox != null)
        {
            vbox.write(message);
            scene.repaint();
        }
    }

    private void log(Exception e)
    {
        log.log(e);
        vbox.write(e.toString());
        scene.repaint();
    }

    private void setupUI()
    {
        // scene = HSceneFactory.getInstance()
        // .getFullScreenScene(HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());;
        // scene.setLayout(new BorderLayout());
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480);
        Dimension dim = scene.getSize();
        int vboxHeight = (int) (0.50 * dim.height);
        int width = (int) dim.width;

        vbox = new VidTextBox(0, 0, width, vboxHeight, 14, 2000);
        scene.add(vbox);
        scene.addKeyListener(this);
        scene.addKeyListener(vbox);
        scene.validate();
        scene.show();
        scene.requestFocus();

    }

    private Service getServiceWithLocatorStr(ServiceList sList, String locatorStr)
    {
        Service service = null;
        for (int i = 0; i < sList.size(); i++)
        {
            Service testService = sList.getService(i);
            if (testService.getLocator().toString().equals(locatorStr))
            {
                service = testService;
                break;
            }
        }
        return service;
    }

    private void verifyServiceProviderServices(TestSelectionProvider selectionProvider)
            throws ProviderFailedInstallationException
    {

        ServiceList spiList = siManager.filterServices(null);

        Service testKnownService = getServiceWithLocatorStr(spiList, knownLocatorString);
        Service testUnknownService = getServiceWithLocatorStr(spiList, unknownLocatorString);

        test.assertTrue("Could not find the known location spi service", testKnownService != null);
        test.assertTrue("Could not find the unknown location spi service", testUnknownService != null);
    }

    private void selectServiceAndValidate(Service service)
    {
        test.assertTrue("Service should not be null", service != null);
        if (service != null)
        {
            log("Selecting " + service.getLocator().toExternalForm());
            serviceContext.select(service);
            
            boolean received = scListener.waitForNormalContentEvent();
            test.assertTrue("Normal content not received tuning to service " + service.getLocator().toExternalForm(),
                    received);
        }

    }

    private AppProxy getAppProxy()
    {
        AppProxy target = null;
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        Enumeration attrs = db.getAppAttributes(new CurrentServiceFilter());
        log("Looking for " + testXletName);
        while (attrs.hasMoreElements())
        {
            AppAttributes app = (AppAttributes) attrs.nextElement();
            // Find the one high priority app
            log("--" + app.getName());
            if (testXletName.equals(app.getName()))
            {
                target = db.getAppProxy(app.getIdentifier());
                break;
            }
        }

        return target;
    }

    private void testServicesAvailableInAnotherXlet()
    {
        AppProxy targetXlet = getAppProxy();
        try
        {
            TestProxyInterface testProxy = new TestProxyInterface()
            {
                public void assertTrue(String msg, boolean condition) throws RemoteException
                {
                    test.assertTrue(msg, condition);
                }
            };
            IxcRegistry.bind(ctx, "Test", testProxy);
            listIXC(ctx);
            if (targetXlet != null)
            {
                TestStateChangeListener listener = new TestStateChangeListener();
                targetXlet.addAppStateChangeEventListener(listener);
                String[] args = new String[] { knownLocatorString };
                targetXlet.start(args);
                boolean started = listener.waitUntilStarted(WAIT_TIME);
                test.assertTrue("Could not start test xlet", started);
            }
            else
            {
                test.fail("Could not load test xlet");
            }

        }
        catch (AlreadyBoundException exc)
        {
            log(exc);
            test.fail("Could not bind TestHolder in IXC");
        }
        finally
        {
            try
            {
                if (targetXlet != null)
                {
                    targetXlet.stop(true);
                }

                try
                {
                    IxcRegistry.unbind(ctx, "Test");
                }
                catch (NotBoundException exc)
                {
                }
            }
            catch (Exception exc)
            {
                log(exc);
            }
        }
    }

    private void showResults()
    {
        String text;
        TestResult results = test.getTestResult();
        log("TEST RESULTS");
        log("Failures: " + results.failureCount());
        log("Total " + results.runCount());
        log.log(results);
        if (results.wasSuccessful())
        {
            // all the tests passed - background color should be green
            text = "All " + results.runCount() + " tests passed";
        }
        else
        {
            // some of the tests failed - background should be red
            StringBuffer sb = new StringBuffer();
            // iterate over the list of failures and draw them on the screen
            for (Enumeration e = results.failures(); e.hasMoreElements();)
            {
                sb.append(((TestFailure) e.nextElement()).toString()).append('\n');
            }
            text = sb.toString();
        }
    }

    public static void listIXC(XletContext ctx)
    {
        String[] arr = IxcRegistry.list(ctx);
        System.out.println("\nIXC Registry");
        for (int i = 0; i < arr.length; i++)
        {
            System.out.println(arr[i]);
        }
        System.out.println();
    }

    private void dumpServiceList(ServiceList sList)
    {
        for (int i = 0; i < sList.size(); i++)
        {
            Service service = sList.getService(i);
            log(service.getLocator().toExternalForm());
            if (service instanceof org.davic.mpeg.Service)
            {
                org.davic.mpeg.Service davicService = (org.davic.mpeg.Service) service;
                ElementaryStream[] streams = davicService.retrieveElementaryStreams();
                for (int j = 0; j < streams.length; j++)
                {
                    log("-- pid:" + streams[j].getPID());
                }
            }
        }
    }

    private static class SCListener implements ServiceContextListener
    {
        private Vector events = new Vector();

        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            synchronized (events)
            {
                System.out.println("receiveServiceContextEvent event:" + e);
                events.addElement(e);
                events.notifyAll();
            }
        }

        public boolean waitForNormalContentEvent()
        {
            return waitForEventOfClass(NormalContentEvent.class);
        }

        public boolean waitForPresentationTerminatedEvent()
        {
            return waitForEventOfClass(PresentationTerminatedEvent.class);
        }

        public boolean waitForAlternativeContentErrorEvent()
        {
            return waitForEventOfClass(AlternativeContentErrorEvent.class);
        }
        
        private boolean waitForEventOfClass(Class clss)
        {
            synchronized (events)
            {
                long endTime = System.currentTimeMillis() + WAIT_TIME;
                try
                {
                    while (!eventOfClassFound(clss) && System.currentTimeMillis() < endTime)
                    {
                        events.wait(WAIT_TIME / 10);
                    }
                }
                catch (InterruptedException exc)
                {
                    // fall through
                }
            }
            return eventOfClassFound(clss);
        }

        private boolean eventOfClassFound(Class cls)
        {
            synchronized (events)
            {
                for (int i = 0; i < events.size(); i++)
                {
                    if (cls.isAssignableFrom(events.elementAt(i).getClass()))
                    {
                        return true;
                    }
                }
                return false;
            }
        }

        public void reset()
        {
            events.removeAllElements();
        }

    }

    static class TestStateChangeListener implements AppStateChangeEventListener
    {
        private Object waitObject = new Object();

        private boolean started = false;

        public boolean waitUntilStarted(long waitTime)
        {
            synchronized (waitObject)
            {
                long endTime = System.currentTimeMillis() + waitTime;
                try
                {
                    while (!started && System.currentTimeMillis() < endTime)
                    {
                        waitObject.wait(waitTime / 5);
                    }
                }
                catch (InterruptedException exc)
                {

                }
            }
            return started;
        }

        public void stateChange(AppStateChangeEvent evt)
        {
            synchronized (waitObject)
            {
                if (evt.getToState() == AppProxy.STARTED && !evt.hasFailed())
                {
                    started = true;
                }
            }
        }

    }

    public static interface TestProxyInterface extends Remote
    {
        public void assertTrue(String msg, boolean condition) throws RemoteException;
    }

    public void keyPressed(KeyEvent event)
    {
        test.getTestResult().clearTestResults();
        switch (event.getKeyCode())
        {
            case HKeyEvent.VK_GUIDE:
                menu();
                break;
            case HKeyEvent.VK_1:
                testServiceSelectionUsingInstalledProvider();
                break;
            case HKeyEvent.VK_2:
                testServiceSelectionUsingInstalledProviderFirst();
                break;
            case HKeyEvent.VK_3:
                testServiceSelectionUsingInstalledProviderNotFirst();
                break;
            case HKeyEvent.VK_4:
                testServiceSelectionWithMultipleInstalledProviders();
                break;
            case HKeyEvent.VK_5:
                testUpdatingServiceInformation();
                break;
            case HKeyEvent.VK_6:
                testUpdatingServiceInformationWhileSelected();
                break;
            case HKeyEvent.VK_7:
                testRemovingServicesWhileSelected();
                break;
            case HKeyEvent.VK_8:
                testUnregisterServiceWhileSelected();
                break;
            case HKeyEvent.VK_9:
                testRecordSPIService();
                break;
            case HKeyEvent.VK_0:
                testRemovingSPIServiceWhileRecording();
                break;
            default:
                break;
        }
        if (test.getTestResult().runCount() > 0)
        {
            showResults();
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }
    
    public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs)
    {
        // get the class name w/o the package prefix
        String name = context.getClass().getName();
        int firstChar = name.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            name = name.substring(firstChar);
        }
        log("Recording playback event received: context = " + name + ", artificialCarouselID = "
                + artificialCarouselID);
        if (null != carouselIDs)
        {
            for (int i = 0; i < carouselIDs.length; i++)
            {
                log("   carouselIDs[" + i + "] = " + carouselIDs[i]);
            }
        }
    }
    
    // /////////////////////////////////////////////////////
    // TimeShiftListener interface implementation
    // /////////////////////////////////////////////////////
    public void receiveTimeShiftevent(TimeShiftEvent tsevent)
    {
    log("Received TimeShiftEvent: " + tsevent);
    }

    // /////////////////////////////////////////////////////
    // ControllerListener interface implementation
    // /////////////////////////////////////////////////////
    public void controllerUpdate(ControllerEvent cevent)
    {
    log("Received ControllerEvent: " + cevent);
    }
    
    class TSBLoggingTimerWentOffListener implements TVTimerWentOffListener
    {
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            Player player = null; //getServiceContextPlayer();
            if (player != null)
            {
                Service service = serviceContext.getService();
                if (service instanceof RecordedService)
                {
                    RecordedService recService = (RecordedService)service;
                    float nanosPerSecond = 1000000000.0F;
                    float millisPerSecond = 1000.0F;
                    int precision = 100; // two digits
                    double buffStart = Math.floor((recService.getFirstMediaTime().getNanoseconds() / nanosPerSecond)
                            * precision + .5)
                            / precision;
                    double mediaTime = Math.floor((player.getMediaTime().getNanoseconds() / nanosPerSecond) * precision
                            + .5)
                            / precision;
                    double buffEnd = Math.floor((recService.getRecordedDuration() / millisPerSecond) * precision
                            + .5)
                            / precision;
                    double deltaToBuffEnd = Math.floor((buffEnd - mediaTime) * precision + .5) / precision;
                    log("buffer start: " + buffStart + ", media time: " + mediaTime + ", buffer end: "
                            + buffEnd + ", secs to buff end: " + deltaToBuffEnd);
                }
                else
                {
                    TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                    if (control != null)
                    {
                        float nanosPerMilli = 1000000000.0F;
                        int precision = 100; // two digits
                        double buffStart = Math.floor((control.getBeginningOfBuffer().getNanoseconds() / nanosPerMilli)
                                * precision + .5)
                                / precision;
                        double mediaTime = Math.floor((player.getMediaTime().getNanoseconds() / nanosPerMilli) * precision
                                + .5)
                                / precision;
                        double buffEnd = Math.floor((control.getEndOfBuffer().getNanoseconds() / nanosPerMilli) * precision
                                + .5)
                                / precision;
                        double deltaToBuffEnd = Math.floor((buffEnd - mediaTime) * precision + .5) / precision;
                        log("buffer start: " + buffStart + ", media time: " + mediaTime + ", buffer end: "
                                + buffEnd + ", secs to buff end: " + deltaToBuffEnd);
                    }
                    else
                    {
                        log("no timeshiftControl for player: " + player);
                    }
                }
            }
            else
            {
                log("no serviceContext player - unable to display buffer info");
            }
        }
    }
    
    // number of times to loop, presenting 'live' and 'recorded' content
    static final int REPEAT_COUNT = 1;

    // 'true' to present the first existing recording, 'false' to present a new
    // recording
    static final boolean USE_EXISTING = true;

    // 'true' to play from TSB, 'false' to play from disk recording
    static final boolean PLAYBACK_FROM_TSB = false;

    // 'true' to delete existing recordings, 'false' to leave existing
    // recordings
    static final boolean DELETE_EXISTING_RECORDINGS = false;

    // minimum and maximum TSB duration (in sec)
    static final int MIN_TSB_DURATION = 3000;

    static final int MAX_TSB_DURATION = 10000;

    // length of recording to make (in seconds)
    static final int SHORT_RECORDING_LENGTH = 10;

    static final int MEDIUM_RECORDING_LENGTH = 30;

    static final int LONG_RECORDING_LENGTH = 60 * 5;

    // amount of time to playback the recording (in seconds)
    static final int PLAYBACK_TIME = 15;

    private int m_recordingState = 0;// NO_RECORDING;

    private OcapRecordingManager m_recordingManager;

    private TVTimerSpec m_tsbLoggingTimerSpec;

    private TVTimer m_tsbLoggingTimer;

    private boolean m_tsbLoggingEnabled;
    /**
     * The 'current' recording request - this will be non-null if a recording is
     * made during an execution of this program.
     */
    private OcapRecordingRequest m_recordingRequestCurrent;

    /**
     * The 'current' buffering request - this will be non-null if a buffering request is
     * started during an execution of this program.
     */
    private BufferingRequest m_bufferingRequestCurrent;
    
    /**
     * OcapRecordingManager bufferingEnable/Disable flag (buffering is enabled at startup)
     */
    private boolean m_bufferingEnabled = true;

    // this is a list of permissible playback rates - pressing the '<<' or '>>'
    // keys on the remote control will
    float m_playRates[] = new float[] { (float) -64.0, (float) -32.0, (float) -16.0, (float) -8.0, (float) -4.0,
            (float) -2.0, (float) -1.0, (float) 0.0, (float) 0.5, (float) 1.0, (float) 2.0, (float) 4.0, (float) 8.0,
            (float) 16.0, (float) 32.0, (float) 64.0, };

    // number of bytes to use for disk free space test
    public static final long DISK_MIN = 616038400;

    private TVTimerWentOffListener m_tsbLoggingTimerWentOffListener;

    //media access authorization - default to full authorization
    private boolean isFullAuth = true;

}
