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

package org.cablelabs.impl.manager.service;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceMinorNumber;
import javax.tv.service.ServiceNumber;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.service.AbstractServiceType;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.util.string.MultiString;

/**
 * Canned version of {@link ServicesDatabase}.
 * 
 * @author Todd Earles
 */
public class CannedServicesDatabase implements ServicesDatabase
{
    /**
     * Not publicly instantiable.
     */
    protected CannedServicesDatabase()
    {
        absServices = new Hashtable();
        absServices.put(new Integer(((OcapLocator)abs1.getLocator()).getSourceID()),abs1);
        absServices.put(new Integer(((OcapLocator)abs2.getLocator()).getSourceID()),abs2);
        absServices.put(new Integer(((OcapLocator)abs3.getLocator()).getSourceID()),abs3);
        selServices = new Hashtable();
    }

    /**
     * Returns the singleton instance of <code>ServicesDatabaseImpl</code>.
     * 
     * @return the singleton instance of <code>ServicesDatabaseImpl</code>
     */
    public static CannedServicesDatabase getInstance()
    {
        return singleton;
    }

    // Description copied from ServicesDatabase
    public void bootProcess()
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public void notifyMonAppConfiguring()
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public void notifyMonAppConfigured()
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public void addBootProcessCallback(BootProcessCallback toAdd)
    {
        // throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public void removeBootProcessCallback(BootProcessCallback toRemove)
    {
        // throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public AbstractServiceEntry getServiceEntry(int serviceId)
    {
        switch (serviceId)
        {
            case 0x1FFFF:
                return absEntry1;
            case 0x2FFFF:
                return absEntry2;
            case 0x3FFFF:
                return absEntry3;
            default:
                return null;
        }
    }

    // Description copied from ServicesDatabase
    public void addServiceChangeListener(int serviceId, ServiceChangeListener l)
    {
        // TODO: This method might not ever need to be implemented in the canned
        // environment. The AppDomain tests that rely on this functionality
        // use their own services database and not this one
    }

    // Description copied from ServicesDatabase
    public void removeServiceChangeListener(int serviceId, ServiceChangeListener l)
    {
        // TODO: This method might not ever need to be implemented in the canned
        // environment. The AppDomain tests that rely on this functionality
        // use their own services database and not this one
    }

    // Description copied from ServicesDatabase
    public void unregisterUnboundApp(int serviceId, org.dvb.application.AppID appid) throws IllegalArgumentException
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public void setAppSignalHandler(org.ocap.application.AppSignalHandler handler)
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServicesDatabase
    public AbstractService addSelectedService(int serviceID)
    {
        synchronized (selServices)
        {
            Integer serviceIDObj = new Integer(serviceID);
            if (selServices.get(serviceIDObj) != null)
            {
                return null;
            }
            
            AbstractService service = (AbstractService)absServices.get(serviceIDObj);
            if (service != null)
            {
                selServices.put(serviceIDObj, service);
                return service;
            }
        }
        
        return null;
    }

    // Description copied from ServicesDatabase
    public void removeSelectedService(int serviceID)
    {
        synchronized (selServices)
        {
            selServices.remove(new Integer(serviceID));
        }
    }

    // Description copied from ServicesDatabase
    public boolean isServiceSelected(int serviceID)
    {
        synchronized (selServices)
        {
            if (isServiceSelectedHardcodeResult)
                return isServiceSelectedResult;
            
            return selServices.get(new Integer(serviceID)) != null;
        }
    }

    public boolean isServiceMarked(int serviceID)
    {
        return false;
    }

    /**
     * Set parameters for behavior of isServiceSelected() method
     * 
     * @param hardcodeResult
     *            If true then return <code>result</code>; otherwise, return the
     *            proper value.
     * @param result
     *            Value to return when <code>hardcodeResult</code> is true.
     */
    public void cannedSetIsServiceSelected(boolean hardcodeResult, boolean result)
    {
        isServiceSelectedHardcodeResult = hardcodeResult;
        isServiceSelectedResult = result;
    }

    // Description copied from ServicesDatabase
    public AbstractService getAbstractService(int serviceID)
    {
        return (AbstractService)absServices.get(new Integer(serviceID));
    }

    // Description copied from ServicesDatabase
    public void getAbstractServices(ServiceCollection collection)
    {
        for (Enumeration e = absServices.elements(); e.hasMoreElements();)
        {
            AbstractService service = (AbstractService)e.nextElement();
            collection.add(service);
        }
    }

    private static boolean isServiceSelectedHardcodeResult = false;

    private static boolean isServiceSelectedResult;

    /**
     * Holds all of the abstract services
     */
    public static Hashtable absServices;

    /**
     * Holds all of the selected or being selected services
     */
    public Hashtable selServices;

    /**
     * All 3 of our AbstractServices
     */
    public static CannedAbstractService abs1;

    public static CannedAbstractService abs2;

    public static CannedAbstractService abs3;

    public static OcapLocator locator1;

    public static OcapLocator locator2;

    public static OcapLocator locator3;

    public static AbstractServiceEntry absEntry1;

    public static AbstractServiceEntry absEntry2;

    public static AbstractServiceEntry absEntry3;

    static
    {
        try
        {
            locator1 = new OcapLocator(0x1FFFF);
            locator2 = new OcapLocator(0x2FFFF);
            locator3 = new OcapLocator(0x3FFFF);

            abs1 = new CannedAbstractService(locator1, locator1.getSourceID(), "abstractservice1");
            abs2 = new CannedAbstractService(locator2, locator2.getSourceID(), "abstractservice2");
            abs3 = new CannedAbstractService(locator3, locator3.getSourceID(), "abstractservice3");

            absEntry1 = new AbstractServiceEntry();
            absEntry1.apps = null;
            absEntry1.autoSelect = false;
            absEntry1.id = 0x1FFFF;
            absEntry1.markedForRemoval = false;
            absEntry1.name = "abstractservice1";

            absEntry2 = new AbstractServiceEntry();
            absEntry2.apps = null;
            absEntry2.autoSelect = false;
            absEntry2.id = 0x2FFFF;
            absEntry2.markedForRemoval = false;
            absEntry2.name = "abstractservice2";

            absEntry3 = new AbstractServiceEntry();
            absEntry3.apps = null;
            absEntry3.autoSelect = false;
            absEntry3.id = 0x3FFFF;
            absEntry3.markedForRemoval = false;
            absEntry3.name = "abstractservice3";

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Singleton instance of the <code>ServicesDatabaseImpl</code>.
     */
    private static CannedServicesDatabase singleton = new CannedServicesDatabase();

    /**
     * @author Joshua Keplinger
     */
    public static class CannedAbstractService extends ServiceExt implements AbstractService, ServiceNumber,
            ServiceMinorNumber
    {
        int serviceID;

        OcapLocator locator;

        String name;

        Date updateTime;

        ServiceDetails details;

        public CannedAbstractService(OcapLocator locator, int serviceID, String name)
        {
            this.locator = locator;
            this.serviceID = serviceID;
            this.name = name;
            updateTime = new Date();
            details = new CannedAbstractServiceDetails();
        }

        public Enumeration getAppIDs()
        {
            return new Enumeration()
            {

                public boolean hasMoreElements()
                {
                    return false;
                }

                public Object nextElement()
                {
                    return null;
                }

            };
        }

        public Enumeration getAppAttributes()
        {
            return new Enumeration()
            {

                public boolean hasMoreElements()
                {
                    return false;
                }

                public Object nextElement()
                {
                    return null;
                }

            };
        }

        public SIRequest retrieveDetails(final SIRequestor requestor)
        {
            final SIRequestDummy request = new SIRequestDummy();
            // get the caller context
            CallerContext ctx = getCurrentContext();
            ctx.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    requestor.notifySuccess(new SIRetrievable[] { details });
                }
            });
            return request;
        }

        private CallerContext getCurrentContext()
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            return ccm.getCurrentContext();
        }

        public String getName()
        {
            return name;
        }

        public boolean hasMultipleInstances()
        {
            return false;
        }

        public ServiceType getServiceType()
        {
            return AbstractServiceType.OCAP_ABSTRACT_SERVICE;
        }

        public Locator getLocator()
        {
            return locator;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            CannedAbstractService o = (CannedAbstractService) obj;
            return locator.equals(o.locator) && name.equals(o.name) && serviceID == o.serviceID;
        }

        public void registerForPSIAcquisition()
        {
            // TODO Auto-generated method stub
        }

        public void unregisterForPSIAcquisition()
        {
            // TODO Auto-generated method stub
        }

        public int hashCode()
        {
            return locator.hashCode();
        }

        private class SIRequestDummy implements SIRequest
        {
            public boolean cancel()
            {
                // do nothing.
                return false;
            }
        }

        public int getServiceNumber()
        {
            return -1;
        }

        public int getMinorNumber()
        {
            return -1;
        }

        public Service createSnapshot(SICache siCache)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public MultiString getNameAsMultiString()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceHandle getServiceHandle()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getID()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Object createLanguageSpecificVariant(String language)
        {
            return this;
        }

        public Object createLocatorSpecificVariant(Locator locator)
        {
            throw new UnsupportedOperationException();
        }

        public String getPreferredLanguage()
        {
            return null;
        }

        class CannedAbstractServiceDetails extends ServiceDetailsExt
        {
            public SIRequest retrieveServiceDescription(final SIRequestor requestor)
            {
                final SIRequestDummy request = new SIRequestDummy();
                // get the caller context
                CallerContext ctx = getCurrentContext();
                ctx.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        // per the spec return data unavailable for abstract
                        // services.
                        requestor.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    }
                });
                return request;
            }

            public SIRequest retrieveComponents(final SIRequestor requestor)
            {
                final SIRequestDummy request = new SIRequestDummy();
                // get the caller context
                CallerContext ctx = getCurrentContext();
                ctx.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        // per the spec return data unavailable for abstract
                        // services.
                        requestor.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    }
                });
                return request;
            }

            public ProgramSchedule getProgramSchedule()
            {
                return null;
            }

            public String getLongName()
            {
                return getName();
            }

            public Service getService()
            {
                return CannedAbstractService.this;
            }

            public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {

            }

            public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {

            }

            public DeliverySystemType getDeliverySystemType()
            {
                return DeliverySystemType.CABLE;
            }

            public ServiceInformationType getServiceInformationType()
            {
                return ServiceInformationType.UNKNOWN;
            }

            public Date getUpdateTime()
            {
                return updateTime;
            }

            public int[] getCASystemIDs()
            {
                return new int[0];
            }

            public boolean isFree()
            {
                return true;
            }

            public ServiceDetails createSnapshot(SICache siCache)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public MultiString getLongNameAsMultiString()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public int getPcrPID()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            public int getProgramNumber()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            public ServiceDetailsHandle getServiceDetailsHandle()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public int getSourceID()
            {
                // TODO Auto-generated method stub
                return -1;
            }

            public int getAppID()
            {
                // TODO Auto-generated method stub
                return -1;
            }

            public TransportStream getTransportStream()
            {
                return null;
            }

            public SIRequest retrieveCarouselComponent(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public SIRequest retrievePcrPID(SIRequestor requestor)
            {
                return null;
            }
            
            public SIRequest retrieveDefaultMediaComponents(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Object getID()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public ServiceType getServiceType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Locator getLocator()
            {
                return CannedAbstractService.this.getLocator();
            }

            public int getServiceNumber()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            public int getMinorNumber()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            public Object createLanguageSpecificVariant(String language)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public String getPreferredLanguage()
            {
                // TODO Auto-generated method stub
                return null;
            }

        }
    }

    /**
     * Resets all of the flags to their default values.
     */
    public static void cannedResetAllFlags()
    {
        isServiceSelectedHardcodeResult = false;
    }
}
