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

import java.util.Iterator;
import java.util.Vector;

import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.PCRPidElement;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SILookupFailedException;
import org.cablelabs.impl.service.SINotAvailableException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.SIRequestInvalidException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDescriptionExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.service.TsIDElement;

/**
 * SI database implementation for SI snapshots.
 * 
 * @see SIManagerSnapshot
 * @see SICacheSnapshot
 * 
 * @author ToddEarles
 */
public class SISnapshotDatabase implements SIDatabase
{
    private static final Logger log = Logger.getLogger(SISnapshotDatabase.class);

    protected String id = this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));

    private final boolean detailedLoggingOn = false;

    /** The snapshot cache */
    private SISnapshotCache siCache;

    // See superclass description
    public void setSICache(SICache siCache)
    {
        this.siCache = (SISnapshotCache) siCache;
    }

    // See superclass description
    public SICache getSICache()
    {
        return siCache;
    }

    // ///////////////////////////////////////////////////////////////////////
    // Rating Dimension
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public RatingDimensionHandle[] getSupportedDimensions() throws SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public RatingDimensionHandle getRatingDimensionByName(String name) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public RatingDimensionExt createRatingDimension(RatingDimensionHandle ratingDimensionHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Transport
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public TransportHandle[] getAllTransports() throws SILookupFailedException
    {
        Iterator iter = siCache.getTransportCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            handles.add(((TransportExt) iter.next()).getTransportHandle());
        }
        return (TransportHandle[]) handles.toArray(new TransportHandle[0]);
    }

    // See superclass description
    public TransportHandle getTransportByID(int transportID) throws SIRequestInvalidException, SILookupFailedException
    {
        Iterator iter = siCache.getTransportCollection();
        while (iter.hasNext())
        {
            TransportExt t = (TransportExt) iter.next();
            if (t.getTransportID() == transportID)
            {
                return t.getTransportHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public TransportExt createTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Network
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public NetworkHandle[] getNetworksByTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = siCache.getNetworkCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            NetworkExt n = (NetworkExt) iter.next();
            TransportExt t = (TransportExt) n.getTransport();
            if (t.getTransportHandle().equals(transportHandle))
            {
                handles.add(n.getNetworkHandle());
            }
        }
        return (NetworkHandle[]) handles.toArray(new NetworkHandle[0]);
    }

    // See superclass description
    public NetworkHandle getNetworkByID(TransportHandle transportHandle, int networkID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getNetworkCollection();
        while (iter.hasNext())
        {
            NetworkExt n = (NetworkExt) iter.next();
            TransportExt t = (TransportExt) n.getTransport();
            if (t.getTransportHandle().equals(transportHandle) && n.getNetworkID() == networkID)
            {
                return n.getNetworkHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public NetworkExt createNetwork(NetworkHandle networkHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Transport Stream
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public TransportStreamHandle[] getTransportStreamsByTransport(TransportHandle transportHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getTransportStreamCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            TransportStreamExt ts = (TransportStreamExt) iter.next();
            TransportExt t = (TransportExt) ts.getTransport();
            if (t.getTransportHandle().equals(transportHandle))
            {
                handles.add(ts.getTransportStreamHandle());
            }
        }
        return (TransportStreamHandle[]) handles.toArray(new TransportStreamHandle[0]);
    }

    // See superclass description
    public TransportStreamHandle[] getTransportStreamsByNetwork(NetworkHandle networkHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getTransportStreamCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            TransportStreamExt ts = (TransportStreamExt) iter.next();
            NetworkExt n = (NetworkExt) ts.getNetwork();
            if (n.getNetworkHandle().equals(networkHandle))
            {
                handles.add(ts.getTransportStreamHandle());
            }
        }
        return (TransportStreamHandle[]) handles.toArray(new TransportStreamHandle[0]);
    }

    // See superclass description
    public TransportStreamHandle getTransportStreamByID(TransportHandle transportHandle, int frequency,
            int modulationFormat, int tsID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = siCache.getTransportStreamCollection();
        while (iter.hasNext())
        {
            TransportStreamExt ts = (TransportStreamExt) iter.next();
            TransportExt t = (TransportExt) ts.getTransport();
            if (t.getTransportHandle().equals(transportHandle) && ts.getFrequency() == frequency
                    && ts.getModulationFormat() == modulationFormat
                    && (tsID == -1 || ts.getTransportStreamID() == tsID))
            {
                return ts.getTransportStreamHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public TransportStreamHandle getTransportStreamBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            TransportStreamExt ts = (TransportStreamExt) sd.getTransportStream();
            if (sd.getSourceID() == sourceID)
            {
                return ts.getTransportStreamHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public TransportStreamHandle getTransportStreamByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            TransportStreamExt ts = (TransportStreamExt) sd.getTransportStream();
            if (ts.getFrequency() == frequency && ts.getModulationFormat() == modulationFormat
                    && sd.getProgramNumber() == programNumber)
            {
                return ts.getTransportStreamHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public TransportStreamExt createTransportStream(TransportStreamHandle transportStreamHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Program Association Table (PAT)
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ProgramAssociationTableHandle getProgramAssociationTableByID(int frequency, int modulationFormat, int tsID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ProgramAssociationTableHandle getProgramAssociationTableBySourceID(int sourceID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ProgramAssociationTable createProgramAssociationTable(ProgramAssociationTableHandle patHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Program Map Table (PMT)
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ProgramMapTableHandle getProgramMapTableByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ProgramMapTableHandle getProgramMapTableBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ProgramMapTable createProgramMapTable(ProgramMapTableHandle pmtHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ServiceHandle[] getAllServices() throws SILookupFailedException, SINotAvailableException,
            SINotAvailableYetException
    {
        Iterator iter = siCache.getServiceCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            handles.add(((ServiceExt) iter.next()).getServiceHandle());
        }
        return (ServiceHandle[]) handles.toArray(new ServiceHandle[0]);
    }

    // See superclass description
    public ServiceHandle getServiceBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            if (sd.getSourceID() == sourceID)
            {
                return ((ServiceExt) sd.getService()).getServiceHandle();
            }
        }
        throw new SIRequestInvalidException();
    }
    
    public ServiceHandle[] getServicesBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
    SINotAvailableYetException, SILookupFailedException
    {
        return null;
        /*
        Iterator iter = siCache.getServiceCollection();

        Vector handles = new Vector();
        while (iter.hasNext())
        {
            handles.add(((ServiceExt) iter.next()).getServiceHandle());
        }
        return (ServiceHandle[]) handles.toArray(new ServiceHandle[0]);
        
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();

            if (sd.getSourceID() == sourceID)
            {
                handles.add(((ServiceExt) sd.getService()).getServiceHandle());
            }
        }
        if (LOGGING) log.debug(id + " getServicesBySourceID array length: " + handles.size());
        return (ServiceHandle[]) handles.toArray(new ServiceHandle[0]);
        */
    }    
    
    // See superclass description
    public ServiceHandle getServiceByServiceName(String serviceName) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        while (iter.hasNext())
        {
            ServiceExt s = (ServiceExt) iter.next();
            if (s.getName().equals(serviceName))
            {
                return s.getServiceHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public ServiceHandle getServiceByServiceNumber(int serviceNumber, int minorNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        while (iter.hasNext())
        {
            ServiceExt s = (ServiceExt) iter.next();
            if (s.getServiceNumber() == serviceNumber && (minorNumber == -1 || s.getMinorNumber() == minorNumber))
            {
                return s.getServiceHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public ServiceHandle getServiceByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getServiceDetailsCollection();
        if (log.isDebugEnabled())
        {
            log.debug(id + " getServiceByProgramNumber...called with Freq: " + frequency + " modulation: "
                    + modulationFormat + "pn: " + programNumber);
        }
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            TransportStreamExt ts = (TransportStreamExt) sd.getTransportStream();
            if (log.isDebugEnabled())
            {
                log.debug(id + " getServiceByProgramNumber(" + ts.getFrequency() + "," + sd.getProgramNumber() + ","
                        + ts.getModulationFormat() + ")");
            }

            if (ts.getFrequency() == frequency && ts.getModulationFormat() == modulationFormat)
            {
                if (modulationFormat == 255 || sd.getProgramNumber() == programNumber)
                    return ((ServiceExt) sd.getService()).getServiceHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    public ServiceHandle getServiceByAppId(int appId) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException
    {
        // Is this right?
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public ServiceExt createService(ServiceHandle serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service Details
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ServiceDetailsHandle[] getServiceDetailsByService(ServiceHandle serviceHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getServiceDetailsByService(" + serviceHandle + ")");
        }

        Iterator iter = siCache.getServiceDetailsCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            if (detailedLoggingOn)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " getServiceDetailsByService found: " + sd);
                }
            }

            ServiceExt s = (ServiceExt) sd.getService();
            if (s.getServiceHandle().equals(serviceHandle))
            {
                handles.add(sd.getServiceDetailsHandle());
            }
        }
        return (ServiceDetailsHandle[]) handles.toArray(new ServiceDetailsHandle[0]);
    }

    // See superclass description
    public ServiceDetailsHandle[] getServiceDetailsBySourceID(int sourceId)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getServiceDetailsBySourceID(" + sourceId + ")");
        }

        Iterator iter = siCache.getServiceDetailsCollection();
        Vector handles = new Vector();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            if (detailedLoggingOn)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " getServiceDetailsByService found: " + sd);
                }
            }

            if (sd.getSourceID() == sourceId)
            {
                handles.add(sd.getServiceDetailsHandle());
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(id + " getServiceDetailsBySourceID array length: " + handles.size());
        }
        
        return (ServiceDetailsHandle[]) handles.toArray(new ServiceDetailsHandle[0]);
    }
    
    // See superclass description
    public ServiceDetailsExt createServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service Description
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ServiceDescriptionExt createServiceDescription(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Service Components
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public ServiceComponentHandle[] getServiceComponentsByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getServiceComponentsByServiceDetails(" + serviceDetailsHandle + ")");
        }

        Vector handles = new Vector();
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(siCache.serviceComponentCacheToString());
            }
        }

        Iterator iter = siCache.getServiceComponentCollection();
        while (iter.hasNext())
        {
            ServiceComponentExt sc = (ServiceComponentExt) iter.next();

            if (detailedLoggingOn)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " getServiceComponentsByServiceDetails found:" + sc);
                }
            }

            ServiceDetailsExt sd = (ServiceDetailsExt) sc.getServiceDetails();
            if (sd.getServiceDetailsHandle().equals(serviceDetailsHandle))
            {
                handles.add(sc.getServiceComponentHandle());
            }
        }

        return (ServiceComponentHandle[]) handles.toArray(new ServiceComponentHandle[0]);
    }

    // See superclass description
    public ServiceComponentHandle getServiceComponentByPID(ServiceDetailsHandle serviceDetailsHandle, int pid)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getServiceComponentCollection();
        while (iter.hasNext())
        {
            ServiceComponentExt sc = (ServiceComponentExt) iter.next();
            ServiceDetailsExt sd = (ServiceDetailsExt) sc.getServiceDetails();
            if (sd.getServiceDetailsHandle().equals(serviceDetailsHandle) && sc.getPID() == pid)
            {
                return sc.getServiceComponentHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public ServiceComponentHandle getServiceComponentByTag(ServiceDetailsHandle serviceDetailsHandle, int tag)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ServiceComponentHandle getServiceComponentByName(ServiceDetailsHandle serviceDetailsHandle, String name)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        Iterator iter = siCache.getServiceComponentCollection();
        while (iter.hasNext())
        {
            ServiceComponentExt sc = (ServiceComponentExt) iter.next();
            ServiceDetailsExt sd = (ServiceDetailsExt) sc.getServiceDetails();
            if (sd.getServiceDetailsHandle().equals(serviceDetailsHandle) && sc.getName().equals(name))
            {
                return sc.getServiceComponentHandle();
            }
        }
        throw new SIRequestInvalidException();
    }

    // See superclass description
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle,
            int carouselID) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ServiceComponentHandle getComponentByAssociationTag(ServiceDetailsHandle serviceDetailsHandle,
            int associationTag) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }

    // See superclass description
    public ServiceComponentExt createServiceComponent(ServiceComponentHandle serviceComponentHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        throw new SIRequestInvalidException();
    }

    // ///////////////////////////////////////////////////////////////////////
    // Event Listeners
    // ///////////////////////////////////////////////////////////////////////

    // See superclass description
    public void addSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addPATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removePATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addPMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removePMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    // See superclass description
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        // Silently ignore
    }

    public void registerForPSIAcquisition(ServiceHandle serviceHandle)
    {
    }

    public void unregisterForPSIAcquisition(ServiceHandle serviceHandle)
    {
    }

    public int getPCRPidForServiceDetails(ServiceDetailsHandle serviceDetailsHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getPCRPidForServiceDetails(" + serviceDetailsHandle + ")");
        }

        Iterator iter = siCache.getPCRPidCollection();
        while (iter.hasNext())
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) iter.next();
            if (sd.getServiceDetailsHandle().equals(serviceDetailsHandle))
            {
                PCRPidElement[] pcr = new PCRPidElement[1];                
                pcr[0] = siCache.getCachedPCRPid(serviceDetailsHandle);
                return pcr[0].getIntValue();
            }
        }       
        return 0;
    }

    public ServiceDetailsHandle registerForHNPSIAcquisition(int session) throws SIDatabaseException
    {
        return null;
    }

    public void unregisterForHNPSIAcquisition(int session)
    {
    }

    public ProgramMapTableHandle getProgramMapTableByService(int serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        throw new UnsupportedOperationException();
    }
    
    public int getTsIDForTransportStreamHandle(TransportStreamHandle transportStreamHandle) throws SIRequestInvalidException,
        SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " getTsIDForTransportStreamHandle(" + transportStreamHandle + ")");
        }
    
        Iterator iter = siCache.getTsIDCollection();
        while (iter.hasNext())
        {
            TransportStreamExt ts = (TransportStreamExt) iter.next();
            if (ts.getTransportStreamHandle().equals(transportStreamHandle))
            {
                TsIDElement[] tsId = new TsIDElement[1];                
                tsId[0] = siCache.getCachedTsID(transportStreamHandle);
                return tsId[0].getIntValue();
            }
        }       
        return 0;
    }

    public boolean isOOBAcquired()
    {
        return true;
    }

    public void waitForNITSVCT()
    {
        
    }

    public void waitForOOB() 
    {
        
    }

    public boolean isNITSVCTAcquired() 
    {
        return true;
    }

    public void waitForNTT() 
    {

    }
}
