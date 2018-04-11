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
import java.util.Vector;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.ocap.si.ByteParser;
import org.cablelabs.impl.ocap.si.ProgramAssociationTableImpl;
import org.cablelabs.impl.ocap.si.ProgramMapTableImpl;
import org.cablelabs.impl.ocap.si.SIChangeEventImpl;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.NetworksChangedEvent;
import org.cablelabs.impl.service.OcapLocatorImpl;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SIHandle;
import org.cablelabs.impl.service.SILookupFailedException;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SINotAvailableException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.SIRequestInvalidException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDescriptionExt;
import org.cablelabs.impl.service.ServiceDetailsCallback;
import org.cablelabs.impl.service.ServiceDetailsChangedEvent;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.service.TransportStreamsChangedEvent;
import org.cablelabs.impl.service.javatv.navigation.ServiceComponentImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDescriptionImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.service.javatv.service.RatingDimensionImpl;
import org.cablelabs.impl.service.javatv.service.ServiceImpl;
import org.cablelabs.impl.service.javatv.transport.NetworkImpl;
import org.cablelabs.impl.service.javatv.transport.TransportImpl;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.string.MultiString;
import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

// TODO (Josh) Add implementation for releasing the handle for all SI types.
/**
 * The <code>SIDatabase</code> implementation.
 * 
 * @author Brian Greene
 * @author Todd Earles
 */
public class SIDatabaseImpl implements SIDatabase, EDListener
{
    private SICache siCache;

    private int siWaitTime;

    /** An SIManager instance */
    private final SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
    
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIDatabaseImpl.class.getName());

    /**
     * Performs static initialization for this class
     */
    static
    {
        OcapMain.loadLibrary();
        nativeInit();
    }

    /** Native initialization */
    private native static void nativeInit();

    // Description copied from SIDatabase
    public void setSICache(SICache siCache)
    {
        if (this.siCache == null)
            this.siCache = siCache;
        else
            throw new IllegalArgumentException("SICache already set");
    }

    // Description copied from SIDatabase
    public SICache getSICache()
    {
        if (siCache == null)
            throw new IllegalStateException("SI cache has not been set yet");
        else
            return siCache;
    }

    /**
     * Construct a <code>SIDatabase</code>
     */
    public SIDatabaseImpl()
    {
        // Initialize fields
        callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        ServiceManager sMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        siWaitTime = sMgr.getOOBWaitTime();
        
        // register for events with the native layer.
        nativeEventRegistration();
    }
    
    private native void nativeEventRegistration();

    // Description copied from SIDatabase
    public RatingDimensionHandle[] getSupportedDimensions() throws SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        int[] nHandles = nativeGetSupportedDimensions();

        RatingDimensionHandle[] rdHandles = new RatingDimensionHandle[nHandles.length];
        for (int i = 0; i < rdHandles.length; ++i)
            rdHandles[i] = new RatingDimensionHandleImpl(nHandles[i]);

        return rdHandles;
    }

    // the native method to fetch the supported dimensions from JNI Sidb
    private native int[] nativeGetSupportedDimensions() throws SILookupFailedException, SINotAvailableException,
            SINotAvailableYetException;

    // Description copied from SIDatabase
    public RatingDimensionHandle getRatingDimensionByName(String name) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        checkNull(name, "rating Dimension Name");

        return new RatingDimensionHandleImpl(nativeGetRatingDimensionHandleByName(name));
    }

    // used to get a rating dimension handle by name
    private native int nativeGetRatingDimensionHandleByName(String name) throws SINotAvailableException,
            SINotAvailableYetException;

    // Description copied from SIDatabase
    public RatingDimensionExt createRatingDimension(RatingDimensionHandle ratingDimensionHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        RatingDimensionData rdd = new RatingDimensionData();
        nativeCreateRatingDimension(ratingDimensionHandle.getHandle(), rdd);
        MultiString[][] levels = new MultiString[rdd.levelNames.length][2];
        for (int i = 0; i < rdd.levelNames.length; i++)
        {
            levels[i][0] = new MultiString(rdd.levelNameLanguages[i], rdd.levelNames[i]);
            levels[i][1] = new MultiString(rdd.levelDescriptionLanguages[i], rdd.levelDescriptions[i]);
        }
        return new RatingDimensionImpl(siCache, ratingDimensionHandle, new MultiString(rdd.dimensionLanguages,
                rdd.dimensionNames), (short) rdd.levelNames.length, levels, null);
    }

    // used to "create" a RatingDimension from the native layer.
    private native void nativeCreateRatingDimension(int ratingDimensionHandle, RatingDimensionData rdd)
            throws SILookupFailedException, SINotAvailableException;

    // used to hold the information traveling between the java layer and JNI for
    // RatingDimension creation.
    private static class RatingDimensionData
    {
        public String[] dimensionNames;

        public String[] dimensionLanguages;

        public String[][] levelNameLanguages;

        public String[][] levelNames;

        public String[][] levelDescriptionLanguages;

        public String[][] levelDescriptions;
    }

    // Description copied from SIDatabase
    public TransportHandle[] getAllTransports() throws SILookupFailedException
    {
        int[] transports = nativeGetAllTransports();

        TransportHandle[] tHandles = new TransportHandle[transports.length];
        for (int i = 0; i < tHandles.length; ++i)
            tHandles[i] = new TransportHandleImpl(transports[i]);

        return tHandles;
    }

    // the native call to fetch all transports.
    private native int[] nativeGetAllTransports() throws SILookupFailedException;

    // Description copied from SIDatabase
    public TransportHandle getTransportByID(int transportID) throws SIRequestInvalidException, SILookupFailedException
    {
        return new TransportHandleImpl(nativeGetTransportHandleByTransportId(transportID));
    }

    // used to get a transport handle by transportId from the native SIDB
    private native int nativeGetTransportHandleByTransportId(int transportId);

    // Description copied from SIDatabase
    public TransportExt createTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SILookupFailedException
    {
        TransportData transportData = new TransportData();
        nativeCreateTransport(transportHandle.getHandle(), transportData);
        return new TransportImpl(siCache, transportHandle, transportData.deliverySystemType, transportData.transportId,
                null);
    }

    /** Native construction of a Transport object by handle */
    private native void nativeCreateTransport(int transportHandle, TransportData td) throws SIRequestInvalidException,
            SILookupFailedException;

    /**
     *used to pass data from the Java layer into the JNI layer.
     */
    private static class TransportData
    {
        public DeliverySystemType deliverySystemType;

        public int transportId;
    }

    // Description copied from SIDatabase
    public NetworkHandle[] getNetworksByTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException
    {
        int networks[] = nativeGetNetworksByTransport(transportHandle.getHandle());

        NetworkHandle[] nHandle = new NetworkHandle[networks.length];
        for (int i = 0; i < nHandle.length; ++i)
            nHandle[i] = new NetworkHandleImpl(networks[i]);

        return nHandle;
    }

    private native int[] nativeGetNetworksByTransport(int transportHandle) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public NetworkHandle getNetworkByID(TransportHandle transportHandle, int networkId)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new NetworkHandleImpl(nativeGetNetworkHandleByNetworkId(transportHandle.getHandle(), networkId));
    }

    // used to get a network handle for a particular network on a particular
    // transport.
    private native int nativeGetNetworkHandleByNetworkId(int transportHandle, int networkId)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public NetworkExt createNetwork(NetworkHandle networkHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        NetworkData nd = new NetworkData();
        nativeCreateNetwork(networkHandle.getHandle(), nd);
        TransportHandleImpl transportHandle = new TransportHandleImpl(nd.transportHandle);
        Transport transport = getSICache().getCachedTransport(transportHandle);
        if (transport == null) transport = createTransport(transportHandle);
        return new NetworkImpl(siCache, networkHandle, transport, nd.networkId, nd.name, nd.serviceInformationType,
                new java.util.Date(nd.updateTime), null);
    }

    // the JNI call to create a Network
    private native void nativeCreateNetwork(int networkHandle, NetworkData ndd) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    // a small class to pass data from JNI to
    private static class NetworkData
    {
        public int networkId;

        public String name = "";

        public ServiceInformationType serviceInformationType;

        public long updateTime;

        public int transportHandle;
    }

    // Description copied from SIDatabase
    public TransportStreamHandle[] getTransportStreamsByTransport(TransportHandle transportHandle)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        int[] handles;
        handles = nativeGetTransportStreamsByTransport(transportHandle.getHandle());

        TransportStreamHandle[] tsHandle = new TransportStreamHandle[handles.length];
        for (int i = 0; i < tsHandle.length; ++i)
            tsHandle[i] = new TransportStreamHandleImpl(handles[i]);

        return tsHandle;
    }

    /** The native call to fetch the transportStreams by transport */
    private native int[] nativeGetTransportStreamsByTransport(int transportHandle) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public TransportStreamHandle[] getTransportStreamsByNetwork(NetworkHandle networkHandle)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {

        int[] handles;
        handles = nativeGetTransportStreamsByNetwork(networkHandle.getHandle());

        TransportStreamHandle[] tsHandle = new TransportStreamHandle[handles.length];
        for (int i = 0; i < tsHandle.length; ++i)
            tsHandle[i] = new TransportStreamHandleImpl(handles[i]);

        return tsHandle;
    }

    // the native method to get the transport stream handles for a particular
    // network.
    private native int[] nativeGetTransportStreamsByNetwork(int networkHandle) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public TransportStreamHandle getTransportStreamByID(TransportHandle transportHandle, int frequency,
            int modulationFormat, int tsID) throws SIRequestInvalidException, SINotAvailableYetException,
            SINotAvailableException, SILookupFailedException
    {
        int handle;
        handle = nativeGetTransportStreamHandleByTransportFreqAndTSID(transportHandle.getHandle(), frequency,
                    modulationFormat, tsID);
            
        return new TransportStreamHandleImpl(handle);
    }

    private native int nativeGetTransportStreamHandleByTransportFreqAndTSID(int transportHandle, int frequency,
            int modulationFormat, int tsid) throws SIRequestInvalidException, SINotAvailableYetException,
            SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public TransportStreamHandle getTransportStreamBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        int handle;
        handle = nativeGetTransportStreamHandleBySourceID(sourceID);

        return new TransportStreamHandleImpl(handle);
    }

    private native int nativeGetTransportStreamHandleBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public TransportStreamHandle getTransportStreamByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        int handle;
        try
        {
            handle = nativeGetTransportStreamHandleByProgramNumber(frequency, modulationFormat, programNumber);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForNITSVCT();
            handle = nativeGetTransportStreamHandleByProgramNumber(frequency, modulationFormat, programNumber);
        }
        return new TransportStreamHandleImpl(handle);
    }

    private native int nativeGetTransportStreamHandleByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public TransportStreamExt createTransportStream(TransportStreamHandle transportStreamHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        TransportStreamData tsData = new TransportStreamData();
        nativeCreateTransportStream(transportStreamHandle.getHandle(), tsData);
        TransportHandleImpl transportHandle = new TransportHandleImpl(tsData.transportHandle);
        Transport transport = getSICache().getCachedTransport(transportHandle);
        if (transport == null) transport = createTransport(transportHandle);
        NetworkHandleImpl networkHandle = new NetworkHandleImpl(tsData.networkHandle);
        Network network = getSICache().getCachedNetwork(networkHandle);
        if (network == null) network = createNetwork(networkHandle);
        return new TransportStreamImpl(siCache, transportStreamHandle, transport, tsData.frequency,
                tsData.modulationFormat, network, tsData.transportStreamId, tsData.description,
                tsData.serviceInformationType, new java.util.Date(tsData.lastUpdate), null);
    }

    private native void nativeCreateTransportStream(int tsHandle, TransportStreamData tsData)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    // used to move TransportStream information from java to JNI layer.
    private static class TransportStreamData
    {
        public int transportStreamId;

        public ServiceInformationType serviceInformationType;

        long lastUpdate;

        String description = "";

        public int frequency;

        public int modulationFormat;

        public int transportHandle;

        public int networkHandle;
    }

    public void waitForNITSVCT()
    {
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(" waitForNITSVCT enter...siWaitTime: " + siWaitTime);
            }

            // Wait for OOB SI (NIT/SVCT) if it is not yet available. This is only done the
            // first time
            // this method is called. After waiting, we return the list of available
            // services
            // even if that list is still empty.
            nitSvctAcquiredCondition.waitUntilTrue(siWaitTime);
        }
        catch (InterruptedException ie)
        {
            if (log.isDebugEnabled())
            {
                log.debug("waitForNITSVCT().. interrupted: " + ie);
            }
    }
    }

    public boolean isNITSVCTAcquired()
    {
        return nitSvctAcquiredCondition.getState();        
    }
    
    public boolean isOOBAcquired()
    {
        return (nitSvctAcquiredCondition.getState() && nttAcquiredCondition.getState());        
    }
    
    /** Wait for NTT-SNS table */
    public void waitForNTT()
    {
        try
        {
            // Wait for OOB NTT if it is not yet available. This is only done the
            // first time
            // this method is called. After waiting, we return the list of available
            // services
            // even if that list is still empty.
            nttAcquiredCondition.waitUntilTrue(siWaitTime);            
        }
        catch (InterruptedException ie)
        {
            if (log.isDebugEnabled())
            {
                log.debug("waitForNTT().. interrupted: " + ie);
            }        
        }
    }

    public void waitForOOB()
    {
        // We are separating the NIT/SVCT gate from NTT lock
        waitForNITSVCT();
        waitForNTT();
    }
    
    // Description copied from SIDatabase
    public ServiceHandle[] getAllServices() throws SILookupFailedException, SINotAvailableException,
            SINotAvailableYetException
    {
        // Get the list of native service handles
        int[] nativeHandles = null;
        boolean notAvailableYet = false;
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("Acquiring list of services");
            }
            nativeHandles = nativeGetAllServices();
        }
        catch (SINotAvailableYetException e)
        {
            // if(Logging.LOGGING)
            // log.debug("Caught exception: " + e);
            notAvailableYet = true;
            waitForOOB();
            nativeHandles = nativeGetAllServices();
        }

        // Return array of service handle objects
        ServiceHandle[] sHandles = new ServiceHandle[nativeHandles.length];
        for (int i = 0; i < sHandles.length; ++i)
            sHandles[i] = new ServiceHandleImpl(nativeHandles[i]);

        return sHandles;
    }

    /** Wait for OOB SI if true */
    private boolean waitForOOB = true;

    // native metnod to get all services.
    private native int[] nativeGetAllServices() throws SILookupFailedException, SINotAvailableException,
            SINotAvailableYetException;

    // Description copied from SIDatabase
    public ServiceHandle getServiceBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException
    {
        // TODO(Todd): Should we maintain a mapping from source ID to service
        // handle to avoid going native every time we need to lookup the
        // service handle.
        int siHandle;
        try
        {
            siHandle = nativeGetServiceBySourceID(sourceID);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForOOB();
            siHandle = nativeGetServiceBySourceID(sourceID);
        }

        return new ServiceHandleImpl(siHandle);
    }

    /** Native lookup of service handle by source ID */
    private native int nativeGetServiceBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;
    
    // Description copied from SIDatabase
    public ServiceHandle getServiceByServiceName(String serviceName) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        checkNull(serviceName, "ServiceName");
        int siHandle;
        try
        {
            siHandle = nativeGetServiceByServiceName(serviceName);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForNTT();
            siHandle = nativeGetServiceByServiceName(serviceName);
        }

        return new ServiceHandleImpl(siHandle);
    }

    /** Native lookup of service handle by service name */
    private native int nativeGetServiceByServiceName(String serviceName) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceHandle getServiceByServiceNumber(int serviceNumber, int minorNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        int siHandle;
        try
        {
            siHandle = nativeGetServiceByServiceNumber(serviceNumber, minorNumber);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForOOB();
            siHandle = nativeGetServiceByServiceNumber(serviceNumber, minorNumber);
        }

        return new ServiceHandleImpl(siHandle);
    }

    private native int nativeGetServiceByServiceNumber(int serviceNumber, int minorNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceHandle getServiceByAppId(int appId) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException
    {
        int siHandle;
        try
        {
            siHandle = nativeGetServiceByAppId(appId);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForNTT();
            siHandle = nativeGetServiceByAppId(appId);
        }

        return new ServiceHandleImpl(siHandle);
    }

    private native int nativeGetServiceByAppId(int appId) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;

    public void registerForPSIAcquisition(ServiceHandle serviceHandle)
    {
        nativeRegisterForPSIAcquisition(serviceHandle.getHandle());
    }

    public void unregisterForPSIAcquisition(ServiceHandle serviceHandle)
    {
        nativeUnregisterForPSIAcquisition(serviceHandle.getHandle());
    }

    // Add methods to register/unregister for HN PSI acquisition
    public ServiceDetailsHandle registerForHNPSIAcquisition(int session) throws SIDatabaseException
    {
        int[] handles = nativeRegisterForHNPSIAcquisition(session);
        if (handles == null)
        {
            throw new SIDatabaseException("Unable to register for HN PSI acquisition");
        }
        return new ServiceDetailsHandleImpl(handles[0]);
    }

    public void unregisterForHNPSIAcquisition(int session)
    {
        try
        {
            nativeUnregisterForHNPSIAcquisition(session);
        }
        catch (SIDatabaseException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Unable to unregister for HN PSI acquisition, exception:", e);
            }
        }
    }

    private native void nativeRegisterForPSIAcquisition(int serviceHandle);

    private native void nativeUnregisterForPSIAcquisition(int serviceHandle);

    private native int[] nativeRegisterForHNPSIAcquisition(int session) throws SIDatabaseException;

    private native void nativeUnregisterForHNPSIAcquisition(int session) throws SIDatabaseException;

    // Description copied from SIDatabase
    public ServiceHandle getServiceByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        int siHandle;
        try
        {
            siHandle = nativeGetServiceByProgramNumber(frequency, modulationFormat, programNumber);
        }
        catch (SINotAvailableYetException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SINotAvailableYetException, WaitForSI..");
            }

            waitForOOB();
            siHandle = nativeGetServiceByProgramNumber(frequency, modulationFormat, programNumber);
        }

        return new ServiceHandleImpl(siHandle);
    }

    /** Native lookup of service handle by program number */
    private native int nativeGetServiceByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceExt createService(ServiceHandle serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        // Call native to populate the service data object
        ServiceData serviceData = new ServiceData();
        
        nativeCreateService(serviceHandle.getHandle(), serviceData);

        // Construct the service locator
        OcapLocator locator = null;
        try
        {         
             // If this is for DSG tunnel and if there is a service name create a locator with it
             if(serviceData.appID > 0)
             {  
                if(serviceData.serviceNames != null
                    && serviceData.serviceNames.length != 0 
                    && serviceData.serviceNames[0] != null)
                {
                    locator = new OcapLocator(serviceData.serviceNames[0], -1, new int[0], null);
                    if (log.isInfoEnabled())
                    {
                        log.info("createService DSG locator " + locator);
                    }                    
                }
             }
             else if (serviceData.sourceID == -1)
                {
                int freq = serviceData.frequency;
                int prognum = serviceData.programNumber;
                int modfmt = serviceData.modulationFormat;            

                if (log.isDebugEnabled())
                {
                    log.debug("createService freq: " + freq + ", prognum: " + prognum + ", mode: " + modfmt);
                }

                if(freq == OcapLocatorImpl.HN_FREQUENCY)
                    locator = new OcapLocatorImpl("remoteservice://n=hnservice");                                   
                else if (modfmt == 255)
                    locator = new OcapLocator(freq, modfmt);
                else 
                    locator = new OcapLocator(freq, prognum, modfmt);                    
            }
            else
            {
                locator = new OcapLocator(serviceData.sourceID);
            }
        }
        catch (InvalidLocatorException e)
        {
            // TODO(Todd): What should be done here?
            throw new SIRequestInvalidException("Cannot create locator for service " + serviceData.sourceID);
        }
        
        return new ServiceImpl(siCache, serviceHandle,
                (serviceData.serviceNames == null || serviceData.serviceLanguages == null) ? null : new MultiString(
                        serviceData.serviceLanguages, serviceData.serviceNames), serviceData.hasMultipleInstances,
                serviceData.serviceType, serviceData.serviceNumber, serviceData.minorNumber, locator, null);
    }

    /** Service data object */
    private static class ServiceData
    {
        public String[] serviceNames = null;

        public String[] serviceLanguages = null;

        public boolean hasMultipleInstances;

        public ServiceType serviceType;

        public int serviceNumber;

        public int minorNumber;

        public int sourceID;

        public int appID;

        public int frequency;

        public int programNumber;

        public int modulationFormat;
    }

    /** Native construction of a service object */
    private native void nativeCreateService(int serviceHandle, ServiceData serviceData)
            throws SIRequestInvalidException, SILookupFailedException, SINotAvailableException;

    public ServiceDetailsHandle[] getServiceDetailsBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
    SINotAvailableYetException, SILookupFailedException
	{
		int[] siHandles;
		siHandles = nativeGetServiceDetailsBySourceID(sourceID);
		
        int count = 0;
        for (int i = 0; i < siHandles.length && siHandles[i] != 0; ++i)
        {
        	count++;
        }
		// Return array of service handle objects
        ServiceDetailsHandleImpl[] sDetails = new ServiceDetailsHandleImpl[count];
        
        for (int i = 0; i < sDetails.length; ++i)
            sDetails[i] = new ServiceDetailsHandleImpl(siHandles[i]);
		
		return sDetails;
	}
 
    /** Native lookup of service details handles by source ID */
    private native int[] nativeGetServiceDetailsBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;
    
    // Description copied from SIDatabase
    public ServiceDetailsHandle[] getServiceDetailsByService(ServiceHandle serviceHandle)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        ServiceHandleImpl sHandle = (ServiceHandleImpl) serviceHandle;
        int[] handles = nativeGetServiceDetailsByService(sHandle.getHandle());

        ServiceDetailsHandleImpl[] sDetails = new ServiceDetailsHandleImpl[handles.length];
        for (int i = 0; i < sDetails.length; ++i)
            sDetails[i] = new ServiceDetailsHandleImpl(handles[i]);

        return sDetails;
    }

    private native int[] nativeGetServiceDetailsByService(int serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceDetailsExt createServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        ServiceDetailsData sdd = new ServiceDetailsData();
        nativeCreateServiceDetails(serviceDetailsHandle.getHandle(), sdd);

        // the native layer may not have initialized the CASystemIDs field.
        // check and initialize if not.
        if (sdd.caSystemIds == null) sdd.caSystemIds = new int[0];
        ServiceHandleImpl serviceHandle = new ServiceHandleImpl(sdd.serviceHandle);
        Service service = getSICache().getCachedService(serviceHandle);
        if (service == null) service = createService(serviceHandle);
        TransportStreamHandle transportStreamHandle = new TransportStreamHandleImpl(sdd.transportStreamHandle);
        TransportStream transportStream = getSICache().getCachedTransportStream(transportStreamHandle);
        if (transportStream == null) transportStream = createTransportStream(transportStreamHandle);

        return new ServiceDetailsImpl(siCache, serviceDetailsHandle, sdd.sourceId, sdd.appId, sdd.programNumber,
                transportStream, (sdd.longNames == null || sdd.languages == null) ? null : new MultiString(
                        sdd.languages, sdd.longNames), service, sdd.deliverySystemType, sdd.serviceInformationType,
                new java.util.Date(sdd.updateTime), sdd.caSystemIds, (sdd.isFree == 1), sdd.pcrPID, null);
    }

    private native void nativeCreateServiceDetails(int serviceDetailsHandle, ServiceDetailsData data)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    // inner class to move data from native to java.
    private static class ServiceDetailsData
    {
        public int sourceId;

        public int appId;

        public int programNumber;

        public String[] longNames = null;

        public String[] languages = null;

        public DeliverySystemType deliverySystemType;

        public ServiceInformationType serviceInformationType;

        public long updateTime;

        public int[] caSystemIds;

        public int isFree;

        public int pcrPID;

        public int transportStreamHandle;

        public int serviceHandle;
    }

    // Description copied from SIDatabase
    public ServiceDescriptionExt createServiceDescription(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        ServiceDescriptionData sdd = new ServiceDescriptionData();
        nativeCreateServiceDescription(serviceDetailsHandle.getHandle(), sdd);
        ServiceDetails serviceDetails = getSICache().getCachedServiceDetails(serviceDetailsHandle);
        if (serviceDetails == null) serviceDetails = createServiceDetails(serviceDetailsHandle);

        return new ServiceDescriptionImpl(siCache, serviceDetails,
                (sdd.descriptions == null || sdd.languages == null) ? null : new MultiString(sdd.languages,
                        sdd.descriptions), new java.util.Date(sdd.updateTime), null);
    }

    // the call to the JNI function that will fill in the ServiceDescription
    // object.
    private native void nativeCreateServiceDescription(int serviceHandle, ServiceDescriptionData data)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    // private class to pass data between the Java layer and the JNI layer.
    private static class ServiceDescriptionData
    {
        public String[] languages = null;

        public String[] descriptions = null;

        public long updateTime;
    }

    // Description copied from SIDatabase
    public ProgramAssociationTableHandle getProgramAssociationTableByID(int frequency, int modulationFormat, int tsID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        return new ProgramAssociationTableHandleImpl(nativeGetPATByTransportFreqAndTSID(frequency, modulationFormat,
                tsID));
    }

    private native int nativeGetPATByTransportFreqAndTSID(int frequency, int modulationFormat, int tsid)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ProgramAssociationTableHandle getProgramAssociationTableBySourceID(int sourceID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        return new ProgramAssociationTableHandleImpl(nativeGetPATBySourceID(sourceID));
    }

    private native int nativeGetPATBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableYetException, SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public ProgramMapTableHandle getProgramMapTableByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        return new ProgramMapTableHandleImpl(nativeGetPMTByProgramNumber(frequency, modulationFormat, programNumber));
    }

    /** Native lookup of PMT handle by program number */
    private native int nativeGetPMTByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ProgramMapTableHandle getProgramMapTableBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        return new ProgramMapTableHandleImpl(nativeGetPMTBySourceID(sourceID));
    }
    
    // Description copied from SIDatabase
    public ProgramMapTableHandle getProgramMapTableByService(int serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        return new ProgramMapTableHandleImpl(nativeGetPMTByService(serviceHandle));
    }
    
    /** Native lookup of PMT handle by Service handle */
    private native int nativeGetPMTByService(int serviceHandle) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;
    
    /** Native lookup of PMT handle by source ID */
    private native int nativeGetPMTBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;

    // Description copied from SIDatabase
    public ProgramAssociationTable createProgramAssociationTable(ProgramAssociationTableHandle patHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        byte[] pat = nativeCreateProgramAssociationTable(patHandle.getHandle());
        if (pat == null)
            throw new SINotAvailableException("nativeCreateProgramAssociationTable returned null data array");

        return makePAT(getSICache(), pat);
    }

    private native byte[] nativeCreateProgramAssociationTable(int patHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public ProgramMapTable createProgramMapTable(ProgramMapTableHandle pmtHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        byte[] pmt = nativeCreateProgramMapTable(pmtHandle.getHandle());
        if (pmt == null) throw new SINotAvailableException("nativeCreateProgramMapTable returned null data array");

        return makePMT(getSICache(), pmt);
    }

    private native byte[] nativeCreateProgramMapTable(int pmtHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle[] getServiceComponentsByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        int[] handles = nativeGetServiceComponentsByServiceDetails(serviceDetailsHandle.getHandle());

        ServiceComponentHandle[] scHandles = new ServiceComponentHandle[handles.length];
        for (int i = 0; i < scHandles.length; ++i)
            scHandles[i] = new ServiceComponentHandleImpl(handles[i]);

        return scHandles;
    }

    // the native call to get service components by service details.
    private native int[] nativeGetServiceComponentsByServiceDetails(int serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getServiceComponentByPID(ServiceDetailsHandle serviceDetailsHandle, int pid)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new ServiceComponentHandleImpl(nativeGetServiceComponentByPID(serviceDetailsHandle.getHandle(), pid));
    }

    /** Native lookup of service component handle by component PID */
    private native int nativeGetServiceComponentByPID(int serviceDetailsHandle, int pid)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getServiceComponentByTag(ServiceDetailsHandle serviceDetailsHandle, int tag)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new ServiceComponentHandleImpl(nativeGetServiceComponentByTag(serviceDetailsHandle.getHandle(), tag));
    }

    /** Native lookup of service component handle by component tag */
    private native int nativeGetServiceComponentByTag(int serviceDetailsHandle, int componentTag)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getServiceComponentByName(ServiceDetailsHandle serviceDetailsHandle, String name)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        checkNull(name, "ServiceComponentName");
        return new ServiceComponentHandleImpl(nativeGetServiceComponentByName(serviceDetailsHandle.getHandle(), name));
    }

    /** Native lookup of service component handle by component name */
    private native int nativeGetServiceComponentByName(int serviceDetailsHandle, String componentName)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new ServiceComponentHandleImpl(
                nativeGetCarouselComponentByServiceDetails(serviceDetailsHandle.getHandle()));
    }

    /** Native lookup of carousel component handle by service details */
    private native int nativeGetCarouselComponentByServiceDetails(int serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle,
            int carouselID) throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new ServiceComponentHandleImpl(nativeGetCarouselComponentByServiceDetails(
                serviceDetailsHandle.getHandle(), carouselID));
    }

    /**
     * Native lookup of carousel component handle by service details and
     * carousel ID
     */
    private native int nativeGetCarouselComponentByServiceDetails(int serviceDetailsHandle, int carouselID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentHandle getComponentByAssociationTag(ServiceDetailsHandle serviceDetailsHandle,
            int associationTag) throws SIRequestInvalidException, SINotAvailableYetException, SINotAvailableException,
            SILookupFailedException
    {
        return new ServiceComponentHandleImpl(nativeGetComponentByAssociationTag(serviceDetailsHandle.getHandle(),
                associationTag));
    }

    /** Native lookup of component handle by service details and association tag */
    private native int nativeGetComponentByAssociationTag(int serviceDetailsHandle, int associationTag)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    // Description copied from SIDatabase
    public ServiceComponentExt createServiceComponent(ServiceComponentHandle serviceComponentHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        // Call native to populate the service component data object
        ServiceComponentData serviceComponentData = new ServiceComponentData();
        nativeCreateServiceComponent(serviceComponentHandle.getHandle(), serviceComponentData);

        ServiceDetailsHandle serviceDetailsHandle = new ServiceDetailsHandleImpl(
                serviceComponentData.serviceDetailsHandle);

        ServiceDetails serviceDetails = getSICache().getCachedServiceDetails(serviceDetailsHandle);
        if (serviceDetails == null) serviceDetails = createServiceDetails(serviceDetailsHandle);

        // TODO(Todd): Get real multi-strings from JNI/native (see bug 4761)
        ServiceComponentExt comp = new ServiceComponentImpl(siCache, serviceComponentHandle, serviceDetails,
                serviceComponentData.componentPID, serviceComponentData.componentTag,
                serviceComponentData.associationTag, serviceComponentData.carouselID,
                (serviceComponentData.componentNames == null || serviceComponentData.componentLangs == null) ? null
                        : new MultiString(serviceComponentData.componentLangs, serviceComponentData.componentNames),
                (serviceComponentData.associatedLanguage == null) ? null : serviceComponentData.associatedLanguage,
                serviceComponentData.streamType, serviceComponentData.serviceInformationType, new Date(
                        serviceComponentData.updateTime), null);
        return comp;
    }

    /** Service component data object */
    private static class ServiceComponentData
    {
        public int componentPID;

        public long componentTag = ServiceComponentImpl.COMPONENT_TAG_UNDEFINED;

        public long associationTag = ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED;

        public long carouselID = ServiceComponentImpl.CAROUSEL_ID_UNDEFINED;

        public String[] componentNames = null;

        public String[] componentLangs = null;

        public String associatedLanguage = null;

        public short streamType;

        public ServiceInformationType serviceInformationType;

        public long updateTime;

        public int serviceDetailsHandle;
    }

    /** Native construction of a service component object */
    private native void nativeCreateServiceComponent(int serviceComponentHandle,
            ServiceComponentData serviceComponentData) throws SIRequestInvalidException, SINotAvailableException,
            SILookupFailedException;

    public int getPCRPidForServiceDetails(ServiceDetailsHandle serviceDetailsHandle) throws SIRequestInvalidException,
               SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {        
        return nativeGetPCRPidForServiceDetails(serviceDetailsHandle.getHandle());
    }

    /** Native lookup of PCR Pid service details */
    private native int nativeGetPCRPidForServiceDetails(int serviceDetailsHandle)
     throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
     SILookupFailedException;
    
    
    /** Native lookup of TS ID for transport stream */
    private native int nativeGetTsIDForTransportStream(int tsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;
    
    public int getTsIDForTransportStreamHandle(TransportStreamHandle transportStreamHandle) throws SIRequestInvalidException,
    SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {        
        return nativeGetTsIDForTransportStream(transportStreamHandle.getHandle());
    }
    
    // Description copied from SIDatabase
    public synchronized void addSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.acquiredListeners = EventMulticaster.add(data.acquiredListeners, listener);
    }

    // Description copied from SIDatabase
    public synchronized void removeSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.acquiredListeners = EventMulticaster.remove(data.acquiredListeners, listener);
    }

    // Description copied from SIDatabase
    public synchronized void addSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.acquiredListeners = EventMulticaster.add(data.acquiredListeners, listener);
    }

    // Description copied from SIDatabase
    public synchronized void removeSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.acquiredListeners = EventMulticaster.remove(data.acquiredListeners, listener);
    }

    // Description copied from SIDatabase
    public void addNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.networkChangeListeners = EventMulticaster.add(data.networkChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removeNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.networkChangeListeners = EventMulticaster.remove(data.networkChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.transportStreamChangeListeners = EventMulticaster.add(data.transportStreamChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.transportStreamChangeListeners = EventMulticaster.remove(data.transportStreamChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.serviceDetailsChangeListeners = EventMulticaster.add(data.serviceDetailsChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.serviceDetailsChangeListeners = EventMulticaster.remove(data.serviceDetailsChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.serviceComponentChangeListeners = EventMulticaster.add(data.serviceComponentChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.serviceComponentChangeListeners = EventMulticaster.remove(data.serviceComponentChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void addPATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.patChangeListeners = EventMulticaster.add(data.patChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removePATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.patChangeListeners = EventMulticaster.remove(data.patChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void addPMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Add the listener to the list of listeners for the specified caller
        // context.
        CCData data = getCCData(context);
        data.pmtChangeListeners = EventMulticaster.add(data.pmtChangeListeners, listener);
    }

    // Description copied from SIDatabase
    public void removePMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Remove the listener from the list of listeners for the specified
        // caller context.
        CCData data = getCCData(context);
        data.pmtChangeListeners = EventMulticaster.remove(data.pmtChangeListeners, listener);
    }

    /**
     * Multicast list of caller context objects for tracking listeners per
     * caller context. At any point in time this list will be the complete list
     * of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    /**
     * Per caller context data
     */
    class CCData implements CallbackData
    {
        /** The listeners to be notified of SI acquired events */
        public volatile SIChangedListener acquiredListeners;

        /** The listeners to be notified of network change events */
        public volatile NetworkChangeListener networkChangeListeners;

        /** The listeners to be notified of transport stream change events */
        public volatile TransportStreamChangeListener transportStreamChangeListeners;

        /** The listeners to be notified of service details change events */
        public volatile ServiceDetailsChangeListener serviceDetailsChangeListeners;

        /** The listeners to be notified of service component change events */
        public volatile ServiceComponentChangeListener serviceComponentChangeListeners;

        /** The listeners to be notified of PAT change events */
        public volatile TableChangeListener patChangeListeners;

        /** The listeners to be notified of PMT change events */
        public volatile TableChangeListener pmtChangeListeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (SIDatabaseImpl.this)
            {
                // TODO(Todd): There may be a race condition here if we ever
                // attempt to send an event after the ccdata object is
                // removed from the callercontext. If this happens, the code
                // that sends the event will call getCCData() which will
                // create a new ccdata object. This new object would have no
                // registered listeners so no events get sent. This
                // additional ccdata object may also be leaked.

                // Remove this caller context from the list then throw away
                // the CCData for it.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(SIDatabaseImpl.this);
                acquiredListeners = null;
                networkChangeListeners = null;
                transportStreamChangeListeners = null;
                serviceDetailsChangeListeners = null;
                serviceComponentChangeListeners = null;
                patChangeListeners = null;
                pmtChangeListeners = null;
            }
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private synchronized CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(this);

        // If a data block has not yet been assigned to this caller context
        // then allocate one and add this caller context to ccList.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, this);
            ccList = CallerContext.Multicaster.add(ccList, cc);
        }
        return data;
    }

    /**
     * Process native event
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        if (log.isDebugEnabled())
        {
            log.debug("asyncEvent() received event code: " + Integer.toHexString(eventCode)
                    + ", eventData1(SI handle):0x" + Integer.toHexString(eventData1) + ", eventData2: " + eventData2);
        }

        // Handle event indicating NIT (Network Information Table) and
        // SVCT (Short-form Virtual Channel Table) have been acquired
        if (eventCode == SIEventCodes.SI_EVENT_NIT_SVCT_ACQUIRED)
        {
            nitSvctAcquiredCondition.setTrue();

            // OOB SI could allow some transport stream and network-related
            // requests to now
            // succeed, so notify them
            notifyServiceDetailsAcquired(new NetworksChangedEvent(this, SIChangeType.ADD, null));
            notifyServiceDetailsAcquired(new TransportStreamsChangedEvent(this, SIChangeType.ADD, null));
        }
        // Handle event indicating NTT has been acquired
        // This event always follows the above event
        // (SI_EVENT_NIT_SVCT_ACQUIRED)
        // Which means that NIT and SVCT have been previously acquired
        else if (eventCode == SIEventCodes.SI_EVENT_SI_FULLY_ACQUIRED)
        {
            // In case we came up fully acquired...
            if(nitSvctAcquiredCondition.getState() == false)
            {
                nitSvctAcquiredCondition.setTrue();
                // OOB SI could allow some transport stream and network-related
                // requests to now
                // succeed, so notify them
                notifyServiceDetailsAcquired(new NetworksChangedEvent(this, SIChangeType.ADD, null));
                notifyServiceDetailsAcquired(new TransportStreamsChangedEvent(this, SIChangeType.ADD, null));
            }
            nttAcquiredCondition.setTrue();
        }
        else if (eventCode == SIEventCodes.SI_EVENT_SI_DISABLED)
        {
            if (log.isInfoEnabled())
            {
                log.info("asyncEvent() received event SI_EVENT_SI_DISABLED ");
            }

            nitSvctAcquiredCondition.setTrue();
            nttAcquiredCondition.setTrue();
        }

        // Determine the type of SI change being signaled
        SIChangeType changeType;
        if (eventData2 == SIEventCodes.SI_CHANGE_TYPE_ADD)
            changeType = SIChangeType.ADD;
        else if (eventData2 == SIEventCodes.SI_CHANGE_TYPE_REMOVE)
            changeType = SIChangeType.REMOVE;
        else if (eventData2 == SIEventCodes.SI_CHANGE_TYPE_MODIFY)
            changeType = SIChangeType.MODIFY;
        else
            changeType = SIChangeType.MODIFY;

        // Take appropriate action based on the event code
        if (eventCode == SIEventCodes.SI_EVENT_IB_PMT_UPDATE || eventCode == SIEventCodes.SI_EVENT_OOB_PMT_UPDATE
                || eventCode == SIEventCodes.SI_EVENT_IB_PMT_ACQUIRED
                || eventCode == SIEventCodes.SI_EVENT_OOB_PMT_ACQUIRED)
        {
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent() processing PMT event 0x" + Integer.toHexString(eventCode) + " type: "
                        + eventData2);
            }

            byte[] patpmtData = getPatPmtData();
            if (patpmtData != null)
            {
                notifyServiceDetailsChanged(new ServiceDetailsChangedEvent(this, changeType, null, eventData1));
                notifyPMTChange(new SIChangeEventImpl(this, changeType, makePMT(getSICache(), patpmtData)));
            }
            // Since this is a PMT update flush the PCR PID from the cache
            // so that an updated value can be retrieved
            ServiceDetailsHandle serviceDetailsHandle = new ServiceDetailsHandleImpl(eventData1);
            getSICache().flushCachedPCRPid(serviceDetailsHandle);
        }
        else if (eventCode == SIEventCodes.SI_EVENT_IB_PAT_UPDATE || eventCode == SIEventCodes.SI_EVENT_OOB_PAT_UPDATE
                || eventCode == SIEventCodes.SI_EVENT_IB_PAT_ACQUIRED
                || eventCode == SIEventCodes.SI_EVENT_OOB_PAT_ACQUIRED)
        {
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent() processing PAT event 0x" + Integer.toHexString(eventCode) + " type: "
                        + eventData2);
            }

            byte[] patpmtData = getPatPmtData();
            if (patpmtData != null)
            {
                notifyServiceDetailsChanged(new TransportStreamsChangedEvent(this, changeType, null));
                notifyPATChange(new SIChangeEventImpl(this, changeType, makePAT(getSICache(), patpmtData)));
            }
            // Since this is a PAT update flush the TsID from SI cache
            // so that an updated value can be retrieved
            TransportStreamHandle tsHandle = new TransportStreamHandleImpl(eventData1);
            getSICache().flushCachedTsID(tsHandle);
        }
        else if (eventCode == SIEventCodes.SI_EVENT_SERVICE_DETAILS_UPDATE)
        {
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent() received SI_EVENT_SERVICE_DETAILS_UPDATE for serviceDetailsHandle: " + eventData1);
            }
            ServiceDetailsHandle serviceDetailsHandle = new ServiceDetailsHandleImpl(eventData1);
            ServiceDetailsExt serviceDetails = null;
            // Create serviceDetails
            try 
            {
                ServiceDetailsData sdd = new ServiceDetailsData();
                nativeCreateServiceDetails(serviceDetailsHandle.getHandle(), sdd);
                if (sdd.caSystemIds == null) 
                {
                    sdd.caSystemIds = new int[0];
                }
                ServiceHandleImpl serviceHandle = new ServiceHandleImpl(sdd.serviceHandle);
                // Do not use the Service from SI cache here, re-create it
                // since it may have changed
                Service service = createService(serviceHandle);
                // Update the cached service
                getSICache().putCachedService(serviceHandle, service);
                TransportStreamHandle transportStreamHandle = new TransportStreamHandleImpl(sdd.transportStreamHandle);
                TransportStream transportStream = createTransportStream(transportStreamHandle);
                serviceDetails = new ServiceDetailsImpl(siCache, serviceDetailsHandle, sdd.sourceId, sdd.appId, sdd.programNumber,
                                     transportStream, (sdd.longNames == null || sdd.languages == null) ? null : new MultiString(
                                     sdd.languages, sdd.longNames), service, sdd.deliverySystemType, sdd.serviceInformationType,
                                     new java.util.Date(sdd.updateTime), sdd.caSystemIds, (sdd.isFree == 1), sdd.pcrPID, null);
            } 
            catch (Exception e) 
            {
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent() exception creating ServiceDetails for serviceDetailsHandle: " + serviceDetailsHandle);
                }
            } 

            // Notify ServiceDetailsChangeListeners of ServiceDetailsChangeEvent
            if(serviceDetails != null)
            {
                TransportStreamExt ts = (TransportStreamExt) serviceDetails.getTransportStream();
                TransportExt transport = null;
                if (ts != null)
                {
                    transport = (TransportExt) ts.getTransport();
                    notifyServiceDetailsChanged(new ServiceDetailsChangeEvent(transport, changeType, serviceDetails));
                }

                String methodName = null;
                if(changeType == SIChangeType.ADD)
                {
                    // Notify ServiceDetails callback listeners of a Service 'map' 
                    methodName = "notifyMapped";
                }
                else if(changeType == SIChangeType.REMOVE)
                {
                    // Notify ServiceDetails callback listeners of a Service 'un-map' 
                    methodName = "notifyUnmapped";
                }
                else if(changeType == SIChangeType.MODIFY)
                {
                    // Notify ServiceDetails callback listeners of a Service 're-map' 
                    methodName = "notifyRemapped";
                }
                
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("asyncEvent() calling " + methodName);
                    }
                    // ServiceDetails callbacks are registered via SIManager
                    // it can be invoked either by a SPI
                    // service re-map or via OOB SI updates by SIDatabase
                    TransportExt.callbacks.invokeCallbacks(ServiceDetailsCallback.class.getMethod(methodName,
                            new Class[] { ServiceDetails.class }), new Object[] { serviceDetails });
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }  
            }
        } 
        // TODO(Todd): This code should check to see if the cache has the
        // Service or ServiceDetails before going native.
        else if (eventCode == SIEventCodes.SI_EVENT_SERVICE_COMPONENT_UPDATE)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Received SI_EVENT_SERVICE_COMPONENT_UPDATE");
            }
            if (log.isDebugEnabled())
            {
                log.debug("eventData1: " + eventData1);
            }
            if (log.isDebugEnabled())
            {
                log.debug("eventData2: " + eventData2);
            }
            ServiceComponentChangeEvent event = null;
            ServiceComponentExt sComp = null;
            try
            {
                // Call native to populate the service component data object
                ServiceComponentData serviceComponentData = new ServiceComponentData();
                nativeCreateServiceComponent(eventData1, serviceComponentData);
                ServiceComponentHandle serviceComponentHandle = new ServiceComponentHandleImpl(eventData1);

                ServiceDetailsHandle serviceDetailsHandle = new ServiceDetailsHandleImpl(
                        serviceComponentData.serviceDetailsHandle);
                
                ServiceDetails serviceDetails = createServiceDetails(serviceDetailsHandle);

                // TODO(Todd): Get real multi-strings from JNI/native (see bug
                // 4761)
                sComp = new ServiceComponentImpl(
                        siCache,
                        serviceComponentHandle,
                        serviceDetails,
                        serviceComponentData.componentPID,
                        serviceComponentData.componentTag,
                        serviceComponentData.associationTag,
                        serviceComponentData.carouselID,
                        (serviceComponentData.componentNames == null || serviceComponentData.componentLangs == null) ? null
                                : new MultiString(serviceComponentData.componentLangs,
                                        serviceComponentData.componentNames), serviceComponentData.associatedLanguage,
                        serviceComponentData.streamType, serviceComponentData.serviceInformationType, new Date(
                                serviceComponentData.updateTime), null);
            }
            catch (SIDatabaseException ex)
            {
                SystemEventUtil.logRecoverableError(new Exception(
                        "Exception thrown while creating ServiceComponent for event delivery: " + ex));
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug("SI_CHANGE_TYPE_" + changeType.toString() + " received");
            }
            event = new ServiceComponentChangeEvent(sComp.getServiceDetails(), changeType, sComp);

            final ServiceComponentChangeEvent scevent = event;

            // Notify all listeners. Use a local copy of the ccList so it does
            // not
            // change while we are using it.
            CallerContext ccList = this.ccList;
            if (ccList != null)
            {
                // Execute the runnable in each caller context in ccList
                ccList.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        // Notify listeners. Use a local copy of data so that it
                        // does not change while we are using it.
                        CCData data = getCCData(callerContextManager.getCurrentContext());
                        if ((data != null) && (data.serviceComponentChangeListeners != null))
                            data.serviceComponentChangeListeners.notifyChange(scevent);
                    }
                });
            }
        }
    }

    // Notifies ServiceDetails listeners of the acquisition of new service
    // information
    private void notifyServiceDetailsAcquired(final SIChangedEvent event)
    {
        // Create the event

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CCData data = getCCData(callerContextManager.getCurrentContext());
                    if ((data != null) && (data.acquiredListeners != null))
                        data.acquiredListeners.notifyChanged(event);
                }
            });
        }
    }

    // Notifies ServiceDetails listeners of the change in service information
    private void notifyServiceDetailsChanged(final SIChangedEvent event)
    {
        // Create the event

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CCData data = getCCData(callerContextManager.getCurrentContext());
                    if ((data != null) && (data.acquiredListeners != null))
                        data.acquiredListeners.notifyChanged(event);
                }
            });
        }
    }

    // Notifies ServiceDetails listeners of the change in service information
    // This is triggered when OOB SI changes are detected
    private void notifyServiceDetailsChanged(final ServiceDetailsChangeEvent event)
    {
        // Create the event

        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CCData data = getCCData(callerContextManager.getCurrentContext());
                    if ((data != null) && (data.serviceDetailsChangeListeners != null))
                        data.serviceDetailsChangeListeners.notifyChange(event);
                }
            });
        }
    }
    
    // Notifies registered PAT or PMT listeners of a table change
    private void notifyPMTChange(final SIChangeEvent event)
    {
        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CCData data = getCCData(callerContextManager.getCurrentContext());
                    if (data != null && data.pmtChangeListeners != null) data.pmtChangeListeners.notifyChange(event);
                }
            });
        }
    }

    // Notifies registered PAT listeners of a table change
    private void notifyPATChange(final SIChangeEvent event)
    {
        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CCData data = getCCData(callerContextManager.getCurrentContext());
                    if (data != null && data.patChangeListeners != null) data.patChangeListeners.notifyChange(event);
                }
            });
        }
    }

    /**
     * Releases the ServiceComponent related to this handle.
     * 
     * @param handle
     */
    private static native void nativeReleaseServiceComponentHandle(int handle);

    /**
     * Constructs a ProgramAssociationTable from native byte data. The first
     * 4-bytes of the data array contain an integer used to construct the
     * TransportStreamHandle associated with this PAT
     * 
     * @param data
     *            native byte data
     * @return the ProgramAssociationTable object
     */
    private ProgramAssociationTable makePAT(SICache sicache, byte[] data)
    {
        try
        {
            // Create the patHandle from first integer entry in the byte array
            int tsHandle = ByteParser.getInt(data, 0);
            if (log.isDebugEnabled())
            {
                log.debug("makePAT::tsHandle " + tsHandle);
            }

            TransportStreamHandle transportStreamHandle = new TransportStreamHandleImpl(tsHandle);
            TransportStream transportStream = sicache.getCachedTransportStream(transportStreamHandle);
            if (transportStream == null) transportStream = createTransportStream(transportStreamHandle);

            return new ProgramAssociationTableImpl(data, transportStream, new ProgramAssociationTableHandleImpl(
                    tsHandle));
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        return null;
    }

    /**
     * Constructs a ProgramMapTable from native byte data. The first 4-bytes of
     * the data array contain an integer used to construct the ServiceHandle
     * associated with this PMT
     * 
     * @param data
     *            native byte data
     * @return the ProgramMapTable object
     */
    private ProgramMapTable makePMT(SICache sicache, byte[] data)
    {
        try
        {
            // Get transport stream handle from first int in byte array and
            // get the TransportStream
            TransportStreamHandle transportStreamHandle = new TransportStreamHandleImpl(ByteParser.getInt(data, 0));
            TransportStream transportStream = sicache.getCachedTransportStream(transportStreamHandle);
            if (transportStream == null) transportStream = createTransportStream(transportStreamHandle);
            if (log.isDebugEnabled())
            {
                log.debug("makePMT::transportStream " + transportStream);
            }
            // Create pmtHandle from second integer entry in the byte array
            int serviceHandle = ByteParser.getInt(data, 4);
            if (log.isDebugEnabled())
            {
                log.debug("makePMT::serviceHandle " + serviceHandle);
            }
            return new ProgramMapTableImpl(data, transportStream, new ProgramMapTableHandleImpl(serviceHandle));
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        return null;
    }

    // convenience method to throw NullPointer for methods that may need to.
    private static void checkNull(Object value, String fieldName)
    {
        if (value == null)
            throw new NullPointerException(fieldName + " cannot be null when passed to SIDatabase - see the JavaDoc.");
    }

    // method to pop the byte[] from the Queue.
    private byte[] getPatPmtData()
    {
        if(patpmtDataQueue.size() != 0)
        {
            return (byte[]) patpmtDataQueue.remove(0);
        }
        return null;
    }

    /*
     * The native ED callback will populate the patpmtDataQueue vector by byte
     * array reference with PAT/PMT byte data using this method prior to calling
     * the Java asyncEvent() method with PAT/PMT events
     */
    private void updatePatPmtData(byte[] data)
    {
        if(data != null)
        {
            patpmtDataQueue.addElement(data);
        }
    }

    /*
     * Implementations of SIHandle's marker interfaces.
     */
    private static class SIHandleImpl implements SIHandle
    {
        public int handle;

        public SIHandleImpl(int handle)
        {
            this.handle = handle;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            SIHandleImpl o = (SIHandleImpl) obj;
            return handle == o.handle;
        }

        public int hashCode()
        {
            return handle;
        }

        public String toString()
        {
            return super.toString() + "[handle=" + Integer.toHexString(handle) + "]";
        }

        public int getHandle()
        {
            return this.handle;
        }
    }

    private static class NetworkHandleImpl extends SIHandleImpl implements NetworkHandle
    {
        public NetworkHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class TransportHandleImpl extends SIHandleImpl implements TransportHandle
    {
        public TransportHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class RatingDimensionHandleImpl extends SIHandleImpl implements RatingDimensionHandle
    {
        public RatingDimensionHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class ProgramAssociationTableHandleImpl extends SIHandleImpl implements
            ProgramAssociationTableHandle
    {
        public ProgramAssociationTableHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class ProgramMapTableHandleImpl extends SIHandleImpl implements ProgramMapTableHandle
    {
        public ProgramMapTableHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class ServiceComponentHandleImpl extends SIHandleImpl implements ServiceComponentHandle
    {
        public ServiceComponentHandleImpl(int handle)
        {
            super(handle);
        }

        protected void finalize()
        {
            nativeReleaseServiceComponentHandle(this.getHandle());
        }
    }

    private static class ServiceDetailsHandleImpl extends SIHandleImpl implements ServiceDetailsHandle
    {
        public ServiceDetailsHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class ServiceHandleImpl extends SIHandleImpl implements ServiceHandle
    {
        public ServiceHandleImpl(int handle)
        {
            super(handle);
        }
    }

    private static class TransportStreamHandleImpl extends SIHandleImpl implements TransportStreamHandle
    {
        public TransportStreamHandleImpl(int handle)
        {
            super(handle);
        }
    }

    /** Caller context manager */
    private CallerContextManager callerContextManager;

    /**
     * The native ED callback will populate this vector by byte array reference with
     * PAT/PMT byte data prior to calling the Java asyncEvent() method with
     * PAT/PMT events
     */
    private Vector patpmtDataQueue = new Vector();
    
    /**
     * Condition used to delay resolution of requests for based on info contained in
     * NIT, SVCT tables
     */
    private SimpleCondition nitSvctAcquiredCondition = new SimpleCondition(false);

    /**
     * Condition used to delay resolution of requests for based on info contained in
     * NTT table
     */
    private SimpleCondition nttAcquiredCondition = new SimpleCondition(false);
}
