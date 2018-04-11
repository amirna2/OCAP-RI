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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.si.DescriptorImpl;
import org.cablelabs.impl.ocap.si.PMTElementaryStreamInfoImpl;
import org.cablelabs.impl.ocap.si.ProgramMapTableExt;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SIHandle;
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
import org.cablelabs.impl.service.javatv.navigation.ServiceComponentImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDescriptionImpl;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.service.javatv.service.RatingDimensionImpl;
import org.cablelabs.impl.service.javatv.service.ServiceImpl;
import org.cablelabs.impl.service.javatv.transport.NetworkImpl;
import org.cablelabs.impl.service.javatv.transport.TransportImpl;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;
import org.cablelabs.impl.util.string.MultiString;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIElement;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.NetworkCollection;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;
import javax.tv.service.transport.TransportStreamCollection;

import org.ocap.net.OcapLocator;
import org.ocap.si.Descriptor;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.service.SIDatabaseException;

/**
 * An implementation of <code>SIDatabase</code> with canned service information
 * (SI) and canned behavior. Canned SI is described in static SI objects and
 * canned behavior is invoked by calling methods whose name begin with "canned".
 * 
 * @author Todd Earles
 * @author Brian Greene
 */
public class CannedSIDatabase implements SIDatabase
{
    /** The cache this database is associated with */
    private SICache siCache;

    // Locators used to contruct SI objects
    public OcapLocator l100;

    public OcapLocator l101;

    public OcapLocator l102;

    public OcapLocator l103;

    public OcapLocator l104;

    public OcapLocator l105;

    public OcapLocator l106;

    public OcapLocator l107;

    public OcapLocator l108;

    public OcapLocator l109;

    public OcapLocator l110;

    public OcapLocator l111;

    public OcapLocator l112;

    public OcapLocator l113;

    public OcapLocator l114;

    public OcapLocator l115;

    public OcapLocator l116;

    public OcapLocator l117;

    public OcapLocator dynamicLocator1;

    public OcapLocator jmfLocator1;

    public OcapLocator jmfLocator2;

    public OcapLocator aitLocator1;

    public OcapLocator aitLocator1b;

    public OcapLocator aitLocator2;

    public OcapLocator aitLocator2b;

    public OcapLocator aitLocator3;

    public OcapLocator aitLocator3b;

    // Integer value of next native handle
    private int nextHandle = 1;

    private Vector pmtChangeListeners = new Vector();

    // Rating dimensions
    protected List ratingDimensionList = new LinkedList();

    public RatingDimensionImpl ratingDimension1;

    // Transports
    protected List transports = new LinkedList();

    public TransportImpl transport1;

    public TransportImpl transport2;

    // Networks
    protected List networks = new LinkedList();

    public NetworkImpl network3;

    public NetworkImpl network4;

    public NetworkImpl network5;

    public NetworkImpl network6;

    // Transport streams
    protected List transportStreams = new LinkedList();

    public TransportStreamImpl transportStream7;

    public TransportStreamImpl transportStream8;

    public TransportStreamImpl transportStream9;

    public TransportStreamImpl transportStream10;

    public TransportStreamImpl transportStream11;

    public TransportStreamImpl transportStream12;

    public TransportStreamImpl transportStream13;

    public TransportStreamImpl transportStream14;

    public TransportStreamImpl transportStream15;

    // Services
    protected List hiddenServices = new LinkedList();

    protected List visibleServices = new LinkedList();

    protected List allServices = new LinkedList();

    public ServiceImpl dynamicService1;

    public ServiceImpl jmfService1;

    public ServiceImpl jmfService2;

    public ServiceImpl service15;

    public ServiceImpl service16;

    public ServiceImpl service17;

    public ServiceImpl service18;

    public ServiceImpl service19;

    public ServiceImpl service20;

    public ServiceImpl service21;

    public ServiceImpl service22;

    public ServiceImpl service23;

    public ServiceImpl service24;

    public ServiceImpl service25;

    public ServiceImpl service26;

    public ServiceImpl service27;

    public ServiceImpl service28;

    public ServiceImpl service29;

    public ServiceImpl service30;

    public ServiceImpl service31;

    public ServiceImpl service32;

    public ServiceImpl aitService1;

    public ServiceImpl aitService1b;

    public ServiceImpl aitService2;

    public ServiceImpl aitService2b;

    public ServiceImpl aitService3;

    public ServiceImpl aitService3b;

    // Service details
    protected List serviceDetailsList = new LinkedList();

    public ServiceDetailsImpl dynamicServiceDetails1;

    public ServiceDetailsImpl jmfServiceDetails1;

    public ServiceDetailsImpl jmfServiceDetails2;

    public ServiceDetailsImpl serviceDetails33;

    public ServiceDetailsImpl serviceDetails34;

    public ServiceDetailsImpl serviceDetails35;

    public ServiceDetailsImpl serviceDetails36;

    public ServiceDetailsImpl serviceDetails37;

    public ServiceDetailsImpl serviceDetails38;

    public ServiceDetailsImpl serviceDetails39;

    public ServiceDetailsImpl serviceDetails40;

    public ServiceDetailsImpl serviceDetails41;

    public ServiceDetailsImpl serviceDetails42;

    public ServiceDetailsImpl serviceDetails43;

    public ServiceDetailsImpl serviceDetails44;

    public ServiceDetailsImpl serviceDetails45;

    public ServiceDetailsImpl serviceDetails46;

    public ServiceDetailsImpl serviceDetails47;

    public ServiceDetailsImpl serviceDetails48;

    public ServiceDetailsImpl serviceDetails49;

    public ServiceDetailsImpl serviceDetails50;

    public ServiceDetailsImpl aitServiceDetails1;

    public ServiceDetailsImpl aitServiceDetails1b;

    public ServiceDetailsImpl aitServiceDetails2;

    public ServiceDetailsImpl aitServiceDetails2b;

    public ServiceDetailsImpl aitServiceDetails3;

    public ServiceDetailsImpl aitServiceDetails3b;

    // Service components
    protected List serviceComponentsList = new LinkedList();

    public ServiceComponentImpl dynamicServiceComponent1V;

    public ServiceComponentImpl dynamicServiceComponent1A1;

    public ServiceComponentImpl jmfServiceComponent1V;

    public ServiceComponentImpl jmfServiceComponent1A1;

    public ServiceComponentImpl jmfServiceComponent1A2;

    public ServiceComponentImpl jmfServiceComponent1S1;

    public ServiceComponentImpl jmfServiceComponent1S2;

    public ServiceComponentImpl jmfServiceComponent1D1;

    public ServiceComponentImpl jmfServiceComponent1D2;

    public ServiceComponentImpl jmfServiceComponent2V;

    public ServiceComponentImpl jmfServiceComponent2A1;

    public ServiceComponentImpl jmfServiceComponent2A2;

    public ServiceComponentImpl jmfServiceComponent2S1;

    public ServiceComponentImpl jmfServiceComponent2S2;

    public ServiceComponentImpl jmfServiceComponent2D1;

    public ServiceComponentImpl jmfServiceComponent2D2;

    public ServiceComponentImpl serviceComponent69;

    public ServiceComponentImpl serviceComponent69eng;

    public ServiceComponentImpl serviceComponent69fre;

    public ServiceComponentImpl serviceComponent69spa;

    public ServiceComponentImpl serviceComponent70;

    public ServiceComponentImpl serviceComponent71;

    public ServiceComponentImpl serviceComponent72;

    public ServiceComponentImpl serviceComponent73;

    public ServiceComponentImpl serviceComponent74;

    public ServiceComponentImpl serviceComponent75;

    public ServiceComponentImpl serviceComponent76;

    public ServiceComponentImpl serviceComponent77;

    public ServiceComponentImpl serviceComponent78;

    public ServiceComponentImpl serviceComponent79;

    public ServiceComponentImpl serviceComponent80;

    public ServiceComponentImpl serviceComponent81;

    public ServiceComponentImpl serviceComponent82;

    public ServiceComponentImpl serviceComponent83;

    public ServiceComponentImpl serviceComponent84;

    public ServiceComponentImpl serviceComponent85;

    public ServiceComponentImpl serviceComponent86;

    public ServiceComponentImpl serviceComponent105;

    public ServiceComponentImpl serviceComponent106;

    public ServiceComponentImpl serviceComponent107;

    public ServiceComponentImpl serviceComponent108;

    public ServiceComponentImpl serviceComponent109;

    public ServiceComponentImpl serviceComponent110;

    public ServiceComponentImpl serviceComponent111;

    public ServiceComponentImpl serviceComponent112;

    public ServiceComponentImpl serviceComponent113;

    public ServiceComponentImpl serviceComponent114;

    public ServiceComponentImpl serviceComponent115;

    public ServiceComponentImpl serviceComponent116;

    public ServiceComponentImpl serviceComponent117;

    public ServiceComponentImpl serviceComponent118;

    public ServiceComponentImpl serviceComponent119;

    public ServiceComponentImpl serviceComponent120;

    public ServiceComponentImpl serviceComponent121;

    public ServiceComponentImpl serviceComponent122;

    public ServiceComponentImpl[] aitComponent1;

    public ServiceComponentImpl[] aitComponent1b;

    public ServiceComponentImpl[] aitComponent2;

    public ServiceComponentImpl[] aitComponent2b;

    public ServiceComponentImpl[] aitComponent3;

    public ServiceComponentImpl[] aitComponent3b;

    // Service descriptions
    protected List serviceDescriptionsList = new LinkedList();

    public ServiceDescriptionImpl dynamicServiceDescription1;

    public ServiceDescriptionImpl jmfServiceDescription1;

    public ServiceDescriptionImpl jmfServiceDescription2;

    public ServiceDescriptionImpl serviceDescription33;

    public ServiceDescriptionImpl serviceDescription34;

    public ServiceDescriptionImpl serviceDescription35;

    public ServiceDescriptionImpl serviceDescription36;

    public ServiceDescriptionImpl serviceDescription37;

    public ServiceDescriptionImpl serviceDescription38;

    public ServiceDescriptionImpl serviceDescription39;

    public ServiceDescriptionImpl serviceDescription40;

    public ServiceDescriptionImpl serviceDescription41;

    public ServiceDescriptionImpl serviceDescription42;

    public ServiceDescriptionImpl serviceDescription43;

    public ServiceDescriptionImpl serviceDescription44;

    public ServiceDescriptionImpl serviceDescription45;

    public ServiceDescriptionImpl serviceDescription46;

    public ServiceDescriptionImpl serviceDescription47;

    public ServiceDescriptionImpl serviceDescription48;

    public ServiceDescriptionImpl serviceDescription49;

    public ServiceDescriptionImpl serviceDescription50;

    public ServiceDescriptionImpl aitServiceDescription1;

    public ServiceDescriptionImpl aitServiceDescription1b;

    public ServiceDescriptionImpl aitServiceDescription2;

    public ServiceDescriptionImpl aitServiceDescription2b;

    public ServiceDescriptionImpl aitServiceDescription3;

    public ServiceDescriptionImpl aitServiceDescription3b;

    private void initMappings()
    {
        LinkedList networkList = new LinkedList();
        networkList.add(network6);
        transportToNetworkMapping.put(new Integer(((SIHandleImpl) transport2.getTransportHandle()).handle), networkList);
        networkList = new LinkedList();
        networkList.add(network3);
        networkList.add(network4);
        networkList.add(network5);
        transportToNetworkMapping.put(new Integer(((SIHandleImpl) transport1.getTransportHandle()).handle), networkList);
        networkList = new LinkedList();

        LinkedList serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent76);
        serviceDetailsList.add(serviceComponent112);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails40.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent80);
        serviceDetailsList.add(serviceComponent116);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails44.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent69);
        serviceDetailsList.add(serviceComponent69eng);
        serviceDetailsList.add(serviceComponent69fre);
        serviceDetailsList.add(serviceComponent69spa);
        serviceDetailsList.add(serviceComponent105);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails33.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent79);
        serviceDetailsList.add(serviceComponent115);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails43.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent84);
        serviceDetailsList.add(serviceComponent120);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails48.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent78);
        serviceDetailsList.add(serviceComponent114);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails42.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent73);
        serviceDetailsList.add(serviceComponent109);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails37.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent86);
        serviceDetailsList.add(serviceComponent122);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails50.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent85);
        serviceDetailsList.add(serviceComponent121);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails49.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent72);
        serviceDetailsList.add(serviceComponent108);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails36.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent83);
        serviceDetailsList.add(serviceComponent119);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails47.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent74);
        serviceDetailsList.add(serviceComponent110);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails38.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent77);
        serviceDetailsList.add(serviceComponent113);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails41.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent75);
        serviceDetailsList.add(serviceComponent111);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails39.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent81);
        serviceDetailsList.add(serviceComponent117);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails45.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent70);
        serviceDetailsList.add(serviceComponent106);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails34.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent82);
        serviceDetailsList.add(serviceComponent118);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails46.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(serviceComponent71);
        serviceDetailsList.add(serviceComponent107);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails35.getServiceDetailsHandle()).handle), serviceDetailsList);

        /*
         * Mapping dynamic ServiceComponents to dynamic ServiceDetails
         */
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(dynamicServiceComponent1V);
        serviceDetailsList.add(dynamicServiceComponent1A1);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) dynamicServiceDetails1.getServiceDetailsHandle()).handle), serviceDetailsList);

        /*
         * Mapping JMF ServiceComponents to JMF ServiceDetails
         */
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(jmfServiceComponent1V);
        serviceDetailsList.add(jmfServiceComponent1A1);
        serviceDetailsList.add(jmfServiceComponent1A2);
        serviceDetailsList.add(jmfServiceComponent1S1);
        serviceDetailsList.add(jmfServiceComponent1S2);
        serviceDetailsList.add(jmfServiceComponent1D1);
        serviceDetailsList.add(jmfServiceComponent1D2);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails1.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        serviceDetailsList.add(jmfServiceComponent2V);
        serviceDetailsList.add(jmfServiceComponent2A1);
        serviceDetailsList.add(jmfServiceComponent2A2);
        serviceDetailsList.add(jmfServiceComponent2S1);
        serviceDetailsList.add(jmfServiceComponent2S2);
        serviceDetailsList.add(jmfServiceComponent2D1);
        serviceDetailsList.add(jmfServiceComponent2D2);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails2.getServiceDetailsHandle()).handle), serviceDetailsList);

        /*
         * Mapping AIT ServiceComponents to AIT ServiceDetails
         */
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent1.length; ++i)
            serviceDetailsList.add(aitComponent1[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent1b.length; ++i)
            serviceDetailsList.add(aitComponent1b[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1b.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent2.length; ++i)
            serviceDetailsList.add(aitComponent2[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent2b.length; ++i)
            serviceDetailsList.add(aitComponent2b[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2b.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent3.length; ++i)
            serviceDetailsList.add(aitComponent3[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3.getServiceDetailsHandle()).handle), serviceDetailsList);
        serviceDetailsList = new LinkedList();
        for (int i = 0; i < aitComponent3b.length; ++i)
            serviceDetailsList.add(aitComponent3b[i]);
        serviceDetailsToServiceComponentMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3b.getServiceDetailsHandle()).handle), serviceDetailsList);

        LinkedList networkTList = new LinkedList();
        networkTList.add(transportStream7);
        networkTList.add(transportStream8);
        networkTList.add(transportStream9);
        networkTList.add(transportStream11);
        networkTList.add(transportStream12);
        networkToTransportStreamMapping.put(new Integer(((SIHandleImpl) network3.getNetworkHandle()).handle),
                networkTList);
        networkTList = new LinkedList();
        networkTList.add(transportStream14);
        networkToTransportStreamMapping.put(new Integer(((SIHandleImpl) network6.getNetworkHandle()).handle),
                networkTList);
        networkTList = new LinkedList();
        networkTList.add(transportStream10);
        networkToTransportStreamMapping.put(new Integer(((SIHandleImpl) network4.getNetworkHandle()).handle),
                networkTList);
        networkTList = new LinkedList();
        networkTList.add(transportStream13);
        networkTList.add(transportStream15);
        networkToTransportStreamMapping.put(new Integer(((SIHandleImpl) network5.getNetworkHandle()).handle),
                networkTList);
        networkTList = new LinkedList();

        LinkedList transportTList = new LinkedList();
        transportTList.add(transportStream14);
        transportToTransportStreamMapping.put(new Integer(((SIHandleImpl) transport2.getTransportHandle()).handle),
                transportTList);
        transportTList = new LinkedList();
        transportTList.add(transportStream7);
        transportTList.add(transportStream8);
        transportTList.add(transportStream9);
        transportTList.add(transportStream10);
        transportTList.add(transportStream11);
        transportTList.add(transportStream12);
        transportTList.add(transportStream13);
        transportTList.add(transportStream15);
        transportToTransportStreamMapping.put(new Integer(((SIHandleImpl) transport1.getTransportHandle()).handle),
                transportTList);
        transportTList = new LinkedList();

        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails33.getServiceDetailsHandle()).handle), service15);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails34.getServiceDetailsHandle()).handle), service16);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails35.getServiceDetailsHandle()).handle), service17);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails36.getServiceDetailsHandle()).handle), service18);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails37.getServiceDetailsHandle()).handle), service19);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails38.getServiceDetailsHandle()).handle), service20);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails39.getServiceDetailsHandle()).handle), service21);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails40.getServiceDetailsHandle()).handle), service22);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails41.getServiceDetailsHandle()).handle), service23);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails42.getServiceDetailsHandle()).handle), service24);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails43.getServiceDetailsHandle()).handle), service25);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails44.getServiceDetailsHandle()).handle), service26);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails45.getServiceDetailsHandle()).handle), service27);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails46.getServiceDetailsHandle()).handle), service28);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails47.getServiceDetailsHandle()).handle), service29);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails48.getServiceDetailsHandle()).handle), service30);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails49.getServiceDetailsHandle()).handle), service31);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails50.getServiceDetailsHandle()).handle), service32);

        /*
         * Mapping dynamic ServiceDetails to dynamic Service
         */
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) dynamicServiceDetails1.getServiceDetailsHandle()).handle), dynamicService1);

        /*
         * Mapping JMF ServiceDetails to JMF Service
         */
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails1.getServiceDetailsHandle()).handle), jmfService1);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails2.getServiceDetailsHandle()).handle), jmfService2);

        /*
         * Mapping AIT ServiceDetails to AIT Service
         */
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1.getServiceDetailsHandle()).handle), aitService1);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1b.getServiceDetailsHandle()).handle), aitService1b);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2.getServiceDetailsHandle()).handle), aitService2);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2b.getServiceDetailsHandle()).handle), aitService2b);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3.getServiceDetailsHandle()).handle), aitService3);
        serviceDetailsToServiceMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3b.getServiceDetailsHandle()).handle), aitService3b);

        LinkedList serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails49);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service31.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails38);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service20.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails46);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service28.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails42);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service24.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails35);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service17.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails34);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service16.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails43);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service25.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails45);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service27.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails39);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service21.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails50);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service32.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails37);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service19.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails36);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service18.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails44);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service26.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails33);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service15.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails47);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service29.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails40);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service22.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails48);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service30.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(serviceDetails41);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) service23.getServiceHandle()).handle),
                serviceDetailsTempList);

        /*
         * Map dynamic Service to dynamic ServiceDetails
         */
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(dynamicServiceDetails1);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) dynamicService1.getServiceHandle()).handle),
                serviceDetailsTempList);

        /*
         * Map JMF Service to JMF ServiceDetails
         */
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(jmfServiceDetails1);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) jmfService1.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(jmfServiceDetails2);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) jmfService2.getServiceHandle()).handle),
                serviceDetailsTempList);

        /*
         * Map AIT Service to AIT ServiceDetails
         */
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails1);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService1.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails1b);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService1b.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails2);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService2.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails2b);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService2b.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails3);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService3.getServiceHandle()).handle),
                serviceDetailsTempList);
        serviceDetailsTempList = new LinkedList();
        serviceDetailsTempList.add(aitServiceDetails3b);
        serviceToServiceDetailsMapping.put(new Integer(((SIHandleImpl) aitService3b.getServiceHandle()).handle),
                serviceDetailsTempList);

        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails33.getServiceDetailsHandle()).handle), serviceDescription33);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails34.getServiceDetailsHandle()).handle), serviceDescription34);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails35.getServiceDetailsHandle()).handle), serviceDescription35);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails36.getServiceDetailsHandle()).handle), serviceDescription36);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails37.getServiceDetailsHandle()).handle), serviceDescription37);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails38.getServiceDetailsHandle()).handle), serviceDescription38);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails39.getServiceDetailsHandle()).handle), serviceDescription39);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails40.getServiceDetailsHandle()).handle), serviceDescription40);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails41.getServiceDetailsHandle()).handle), serviceDescription41);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails42.getServiceDetailsHandle()).handle), serviceDescription42);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails43.getServiceDetailsHandle()).handle), serviceDescription43);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails44.getServiceDetailsHandle()).handle), serviceDescription44);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails45.getServiceDetailsHandle()).handle), serviceDescription45);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails46.getServiceDetailsHandle()).handle), serviceDescription46);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails47.getServiceDetailsHandle()).handle), serviceDescription47);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails48.getServiceDetailsHandle()).handle), serviceDescription48);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails49.getServiceDetailsHandle()).handle), serviceDescription49);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) serviceDetails50.getServiceDetailsHandle()).handle), serviceDescription50);

        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) dynamicServiceDetails1.getServiceDetailsHandle()).handle), dynamicServiceDescription1);

        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails1.getServiceDetailsHandle()).handle), jmfServiceDescription1);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) jmfServiceDetails2.getServiceDetailsHandle()).handle), jmfServiceDescription2);

        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1.getServiceDetailsHandle()).handle), aitServiceDescription1);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails1b.getServiceDetailsHandle()).handle), aitServiceDescription1b);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2.getServiceDetailsHandle()).handle), aitServiceDescription2);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails2b.getServiceDetailsHandle()).handle), aitServiceDescription2b);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3.getServiceDetailsHandle()).handle), aitServiceDescription3);
        serviceDetailsToServiceDescriptionMapping.put(new Integer(
                ((SIHandleImpl) aitServiceDetails3b.getServiceDetailsHandle()).handle), aitServiceDescription3b);
    }

    // internal data structures to maintain information regarding the state of
    // various canned behaviors and mappings

    private Map transportToNetworkMapping = new HashMap();

    private Map transportToTransportStreamMapping = new HashMap();

    private Map networkToTransportStreamMapping = new HashMap();

    private Map serviceDetailsToServiceComponentMapping = new HashMap();

    private Map serviceToServiceDetailsMapping = new HashMap();

    private Map serviceDetailsToServiceMapping = new HashMap();

    private Map serviceDetailsToServiceDescriptionMapping = new HashMap();

    /** used to track exceptions that should always be thrown. */
    private List forcedExceptions = new LinkedList();

    private List networkChangeListeners = new LinkedList();

    private List transportStreamChangeListeners = new LinkedList();

    private List serviceDetailsChangeListeners = new LinkedList();

    private List serviceComponentChangeListeners = new LinkedList();

    /** track the acquiredListeners */
    private List acquiredListeners = new LinkedList();

    /** Used to track the number of times handles have been used to create. */
    private Map handleCreateCountMap = new HashMap();

    /** Used to track the number of times handles have been requested. */
    private Map handleRequestedByMap = new HashMap();

    /**
     * Construct a <code>CannedSIDatabase</code><br/>
     */
    public CannedSIDatabase()
    {
    }

    // Description copied from SIDatabase
    public void setSICache(SICache siCache)
    {
        this.siCache = siCache;
    }

    // Description copied from SIDatabase
    public SICache getSICache()
    {
        return siCache;
    }

    /**
     * Create all static SI objects
     */
    public void createStaticSI()
    {
        // Locators
        try
        {
            l100 = new OcapLocator(100);
            l101 = new OcapLocator(101);
            l102 = new OcapLocator(102);
            l103 = new OcapLocator(103);
            l104 = new OcapLocator(104);
            l105 = new OcapLocator(105);
            l106 = new OcapLocator(106);
            l107 = new OcapLocator(107);
            l108 = new OcapLocator(108);
            l109 = new OcapLocator(109);
            l110 = new OcapLocator(110);
            l111 = new OcapLocator(111);
            l112 = new OcapLocator(112);
            l113 = new OcapLocator(113);
            l114 = new OcapLocator(114);
            l115 = new OcapLocator(115);
            l116 = new OcapLocator(116);
            l117 = new OcapLocator(117);

            dynamicLocator1 = new OcapLocator(5000, 101, 1);

            jmfLocator1 = new OcapLocator(1100);
            jmfLocator2 = new OcapLocator(1200);

            aitLocator1 = new OcapLocator(5100);
            aitLocator1b = new OcapLocator(5110);
            aitLocator2 = new OcapLocator(5200);
            aitLocator2b = new OcapLocator(5210);
            aitLocator3 = new OcapLocator(5300);
            aitLocator3b = new OcapLocator(5310);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Rating dimensions
        String[] engFreDeu = new String[] { "eng", "fre", "deu" };
        ratingDimension1 = new RatingDimensionImpl(siCache, new RatingDimensionHandleImpl(151), new MultiString(
                engFreDeu, new String[] { "MMPA", "GUIE", "SPKN" }), (short) 3,
                new MultiString[][] {
                        { new MultiString(engFreDeu, new String[] { "R", "Quieu", "Sp" }),
                                new MultiString(engFreDeu, new String[] { "Restricted", "Souvoplay", "Sprechtn" }) },
                        {
                                new MultiString(engFreDeu, new String[] { "G", "Phro", "Zi" }),
                                new MultiString(engFreDeu, new String[] { "General Admission", "Phronizia",
                                        "Ziegfriest" }) },
                        {
                                new MultiString(engFreDeu, new String[] { "PG", "Phro", "Yv" }),
                                new MultiString(engFreDeu, new String[] { "Parental Guidance", "Phronizia",
                                        "Youth Verboten" }) } }, null);

        // Transports
        transport1 = new TransportImpl(siCache, new TransportHandleImpl(nextNativeHandle()), DeliverySystemType.CABLE,
                1, null);
        transport2 = new TransportImpl(siCache, new TransportHandleImpl(nextNativeHandle()),
                DeliverySystemType.UNKNOWN, 2, null);

        // Networks
        network3 = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()), transport1, 1, "ABC",
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -3)), null);

        network4 = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()), transport1, 2, "HBO",
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        network5 = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()), transport1, 3, "FOX",
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * 0)), null);

        network6 = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()), transport2, 4, "Quie",
                ServiceInformationType.UNKNOWN, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        // Transport streams
        transportStream7 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 5000, 1, network3, 1, "net3data", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 5)), null);

        transportStream8 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 5250, 2, network3, 2, "net3audio", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 3)), null);

        transportStream9 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 5500, 3, network3, 3, "net3vid", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 5)), null);

        transportStream10 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 5750, 4, network4, 4, "testStream4", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 60)), null);

        transportStream11 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 6000, 5, network3, 5, "net3frvid", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 5)), null);

        transportStream12 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 6250, 1, network3, 6, "net3fraudio", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 3)), null);

        transportStream13 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 6500, 2, network5, 7, "testStream7", ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 5)), null);

        transportStream14 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport2, 6750, 3, network6, 8, "testStream8", ServiceInformationType.UNKNOWN, new java.util.Date(
                        System.currentTimeMillis() + (1000 * 60)), null);

        transportStream15 = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                transport1, 7000, 0x10, network5, 9, "testStream9", ServiceInformationType.ATSC_PSIP,
                new java.util.Date(System.currentTimeMillis() + (1000 * 60)), null);

        // Services
        dynamicService1 = new ServiceImpl(siCache, new ServiceHandleImpl(1300), new MultiString(new String[] { "eng" },
                new String[] { "dynamicService1" }), false, ServiceType.DIGITAL_TV, 1300, 1, dynamicLocator1, null);

        jmfService1 = new ServiceImpl(siCache, new ServiceHandleImpl(1100), new MultiString(new String[] { "eng" },
                new String[] { "JMF Service 1" }), false, ServiceType.DIGITAL_TV, 1100, 1, jmfLocator1, null);

        jmfService2 = new ServiceImpl(siCache, new ServiceHandleImpl(1200), new MultiString(new String[] { "eng" },
                new String[] { "JMF Service 2" }), false, ServiceType.DIGITAL_TV, 1200, 1, jmfLocator2, null);

        service15 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService1" }), false, ServiceType.DIGITAL_TV, 101, 1, l100,
                null);

        service16 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService2" }), false, ServiceType.DATA_BROADCAST, 102, 2,
                l101, null);

        service17 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService3" }), false, ServiceType.DATA_BROADCAST, 103, 1,
                l102, null);

        service18 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService4" }), false, ServiceType.DIGITAL_TV, 104, 2, l103,
                null);

        service19 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService5" }), false, ServiceType.DIGITAL_TV, 105, 3, l104,
                null);

        service20 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService6" }), false, ServiceType.DIGITAL_TV, 106, 1, l105,
                null);

        service21 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService7" }), false, ServiceType.DIGITAL_RADIO, 107, 2,
                l106, null);

        service22 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService8" }), false, ServiceType.DIGITAL_RADIO, 108, 3,
                l107, null);

        service23 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService9" }), false, ServiceType.DIGITAL_TV, 109, 4, l108,
                null);

        service24 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService10" }), false, ServiceType.DIGITAL_TV, 110, 5, l109,
                null);

        service25 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService11" }), false, ServiceType.DIGITAL_TV, 1100, -1,
                l110, null);

        service26 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService12" }), false, ServiceType.DIGITAL_TV, 112, 1, l111,
                null);

        service27 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService13" }), false, ServiceType.DIGITAL_TV, 113, 2, l112,
                null);

        service28 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService14" }), false, ServiceType.DIGITAL_TV, 114, 0, l113,
                null);

        service29 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService15" }), true, ServiceType.DIGITAL_TV, 115, 0, l114,
                null);

        service30 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService16" }), true, ServiceType.DIGITAL_TV, 116, 0, l115,
                null);

        service31 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService17" }), true, ServiceType.DIGITAL_TV, 117, 0, l116,
                null);

        service32 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "testService18" }), true, ServiceType.DIGITAL_TV, 118, 0, l117,
                null);

        aitService1 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 1" }), false, ServiceType.DIGITAL_TV,
                aitLocator1.getSourceID(), 1000, aitLocator1, null);
        aitService1b = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 1b" }), false, ServiceType.DIGITAL_TV,
                aitLocator1b.getSourceID(), 1010, aitLocator1b, null);
        aitService2 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 2" }), false, ServiceType.DIGITAL_TV,
                aitLocator2.getSourceID(), 2000, aitLocator2, null);
        aitService2b = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 2b" }), false, ServiceType.DIGITAL_TV,
                aitLocator2b.getSourceID(), 2010, aitLocator2b, null);
        aitService3 = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 3" }), false, ServiceType.DIGITAL_TV,
                aitLocator1.getSourceID(), 3000, aitLocator3, null);
        aitService3b = new ServiceImpl(siCache, new ServiceHandleImpl(nextNativeHandle()), new MultiString(
                new String[] { "eng" }, new String[] { "ait Service 3b" }), false, ServiceType.DIGITAL_TV,
                aitLocator1b.getSourceID(), 3010, aitLocator3b, null);

        // Service details
        dynamicServiceDetails1 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(1310), -1, 103,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "Dynamic ServiceDetails 1" }),
                dynamicService1, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), new int[] {}, true, 0x1FFF, null);

        jmfServiceDetails1 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(1110), 1100, 101,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "JMF ServiceDetails 1" }),
                jmfService1, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), new int[] {}, true, 0x1FFF, null);

        jmfServiceDetails2 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(1210), 1200, 102,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "JMF ServiceDetails 2" }),
                jmfService2, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), new int[] {}, true, 0x1FFF, null);

        serviceDetails33 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 100, 2,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_100" }),
                service15, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -1)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails34 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 101, 3,
                transportStream8, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_101" }),
                service16, DeliverySystemType.CABLE, ServiceInformationType.DVB_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -2)), new int[] { 1, 2, 3 }, false, 0x1FFF, null);

        serviceDetails35 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 102, 4,
                transportStream9, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_102" }),
                service17, DeliverySystemType.TERRESTRIAL, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -3)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails36 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 103, 5,
                transportStream10, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_103" }),
                service18, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -4)), new int[] {}, true, 0x1FFF, null);

        serviceDetails37 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 104, 6,
                transportStream11, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_104" }),
                service19, DeliverySystemType.SATELLITE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -10)), new int[] { 4, 5 }, false, 0x1FFF, null);

        serviceDetails38 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 105, 7,
                transportStream12, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_105" }),
                service20, DeliverySystemType.UNKNOWN, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -10)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails39 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 106, 8,
                transportStream13, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_106" }),
                service21, DeliverySystemType.CABLE, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -12)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails40 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 107, 9,
                transportStream14, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_107" }),
                service22, DeliverySystemType.TERRESTRIAL, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -14)), new int[] {}, true, 0x1FFF, null);

        serviceDetails41 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 108, 10,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_108" }),
                service23, DeliverySystemType.CABLE, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -16)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails42 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 109, 11,
                transportStream8, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_109" }),
                service24, DeliverySystemType.CABLE, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -1)), new int[] {}, true, 0x1FFF, null);

        serviceDetails43 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 110, 12,
                transportStream9, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_110" }),
                service25, DeliverySystemType.TERRESTRIAL, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -2)), new int[] { 4, 5 }, false, 0x1FFF, null);

        serviceDetails44 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 111, 13,
                transportStream10, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_111" }),
                service26, DeliverySystemType.CABLE, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -3)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails45 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 112, 14,
                transportStream11, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_112" }),
                service27, DeliverySystemType.SATELLITE, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -4)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails46 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 113, 15,
                transportStream12, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_113" }),
                service28, DeliverySystemType.UNKNOWN, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -1)), new int[] { 1, 2, 3 }, false, 0x1FFF, null);

        serviceDetails47 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 114, 16,
                transportStream13, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_114" }),
                service29, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -2)), new int[] { 1, 2 }, false, 0x1FFF, null);

        serviceDetails48 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 115, 17,
                transportStream14, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_115" }),
                service30, DeliverySystemType.TERRESTRIAL, ServiceInformationType.DVB_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -3)), new int[] {}, true, 0x1FFF, null);

        serviceDetails49 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 116, 18,
                transportStream7, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_116" }),
                service31, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -4)), new int[] { 4, 5 }, false, 0x1FFF, null);

        serviceDetails50 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()), 117, 19,
                transportStream8, new MultiString(new String[] { "eng" }, new String[] { "Canned_Service_117" }),
                service32, DeliverySystemType.CABLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -10)), new int[] { 1, 2 }, false, 0x1FFF, null);

        aitServiceDetails1 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator1.getSourceID(), 1000, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 1" }), aitService1, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);
        aitServiceDetails1b = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator1b.getSourceID(), 1010, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 1b" }), aitService1b, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);
        aitServiceDetails2 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator2.getSourceID(), 2000, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 2" }), aitService2, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);
        aitServiceDetails2b = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator2b.getSourceID(), 2010, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 2b" }), aitService2b, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);
        aitServiceDetails3 = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator3.getSourceID(), 3000, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 3" }), aitService3, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);
        aitServiceDetails3b = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                aitLocator3b.getSourceID(), 3010, transportStream15, new MultiString(new String[] { "eng" },
                        new String[] { "ait ServiceDetails 3b" }), aitService3b, DeliverySystemType.CABLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000), new int[] {},
                true, 0x1FFF, null);

        // Service components
        dynamicServiceComponent1V = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1311),
                dynamicServiceDetails1, 1311, 1311, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1311,
                new MultiString(new String[] { "eng" }, new String[] { "Dynamic ServiceComponent 1 - Video" }), "eng",
                org.ocap.si.StreamType.MPEG_1_VIDEO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        dynamicServiceComponent1A1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1312),
                dynamicServiceDetails1, 1312, 1312, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1312,
                new MultiString(new String[] { "eng" }, new String[] { "Dynamic ServiceComponent 1 - Audio 1" }),
                "eng", org.ocap.si.StreamType.MPEG_1_AUDIO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1V = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1111),
                jmfServiceDetails1, 1111, 1111, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1111, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 1 - Video" }), "eng",
                org.ocap.si.StreamType.MPEG_1_VIDEO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1A1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1112),
                jmfServiceDetails1, 1112, 1112, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1112, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 1 - Audio 1" }), "eng",
                org.ocap.si.StreamType.MPEG_1_AUDIO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1A2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1113),
                jmfServiceDetails1, 1113, 1113, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1113, new MultiString(
                        new String[] { "fre" }, new String[] { "JMF ServiceComponent 1 - Audio 2" }), "fre",
                org.ocap.si.StreamType.MPEG_1_AUDIO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1S1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1114),
                jmfServiceDetails1, 1114, 1114, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1114, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 1 - Subtitle 1" }), "eng",
                org.ocap.si.StreamType.STD_SUBTITLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1S2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1115),
                jmfServiceDetails1, 1115, 1115, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1115, new MultiString(
                        new String[] { "fre" }, new String[] { "JMF ServiceComponent 1 - Subtitle 2" }), "fre",
                org.ocap.si.StreamType.STD_SUBTITLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1D1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1116),
                jmfServiceDetails1, 1116, 1116, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1116, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 1 - Data 1" }), "eng",
                org.ocap.si.StreamType.ASYNCHRONOUS_DATA, ServiceInformationType.UNKNOWN, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent1D2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1117),
                jmfServiceDetails1, 1117, 1117, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1117, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 1 - Data 2" }), "eng",
                org.ocap.si.StreamType.MPEG_PRIVATE_DATA, ServiceInformationType.UNKNOWN, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2V = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1211),
                jmfServiceDetails2, 1211, 1211, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1211, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 2 - Video" }), "eng",
                org.ocap.si.StreamType.MPEG_1_VIDEO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2A1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1212),
                jmfServiceDetails2, 1212, 1212, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1212, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 2 - Audio 1" }), "eng",
                org.ocap.si.StreamType.MPEG_1_AUDIO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2A2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1213),
                jmfServiceDetails2, 1213, 1213, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1213, new MultiString(
                        new String[] { "fre" }, new String[] { "JMF ServiceComponent 2 - Audio 2" }), "fre",
                org.ocap.si.StreamType.MPEG_1_AUDIO, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2S1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1214),
                jmfServiceDetails2, 1214, 1214, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1214, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 2 - Subtitle 1" }), "eng",
                org.ocap.si.StreamType.STD_SUBTITLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2S2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1215),
                jmfServiceDetails2, 1215, 1215, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1215, new MultiString(
                        new String[] { "fre" }, new String[] { "JMF ServiceComponent 2 - Subtitle 2" }), "fre",
                org.ocap.si.StreamType.STD_SUBTITLE, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2D1 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1216),
                jmfServiceDetails2, 1216, 1216, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1216, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 2 - Data 1" }), "eng",
                org.ocap.si.StreamType.ASYNCHRONOUS_DATA, ServiceInformationType.UNKNOWN, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        jmfServiceComponent2D2 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(1217),
                jmfServiceDetails2, 1217, 1217, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 1217, new MultiString(
                        new String[] { "eng" }, new String[] { "JMF ServiceComponent 2 - Data 2" }), "eng",
                org.ocap.si.StreamType.MPEG_PRIVATE_DATA, ServiceInformationType.UNKNOWN, new java.util.Date(
                        System.currentTimeMillis() - 1000), null);

        serviceComponent69 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails33, 1, 1, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent69" }), "eng", org.ocap.si.StreamType.MPEG_1_VIDEO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent69eng = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails33, 2, 5, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent69eng" }), "eng", org.ocap.si.StreamType.MPEG_1_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent69fre = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails33, 3, 6, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent69fre" }), "fre", org.ocap.si.StreamType.MPEG_1_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent69spa = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails33, 4, 7, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "spa" },
                        new String[] { "serviceComponent69spa" }), "spa", org.ocap.si.StreamType.MPEG_1_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent70 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails34, 2, 2, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent70" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_DATA,
                ServiceInformationType.DVB_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent71 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails35, 3, 3, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent71" }), "eng", org.ocap.si.StreamType.MPEG_1_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent72 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails36, 4, 4, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent72" }), "fre", org.ocap.si.StreamType.MPEG_2_VIDEO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent73 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails37, 5, 5, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent73" }), "fre", org.ocap.si.StreamType.MPEG_PRIVATE_SECTION,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        serviceComponent74 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails38, 6, 6, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent74" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_DATA,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        serviceComponent75 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails39, 7, 7, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent75" }), "eng", org.ocap.si.StreamType.MHEG,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -12)), null);

        serviceComponent76 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails40, 8, 8, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent76" }), "eng", org.ocap.si.StreamType.DSM_CC,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -14)), null);

        serviceComponent77 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails41, 9, 9, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent77" }), "eng", org.ocap.si.StreamType.H_222,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -16)), null);

        serviceComponent78 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails42, 10, 10, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent78" }), "eng", org.ocap.si.StreamType.DSM_CC_MPE,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent79 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails43, 11, 11, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent79" }), "fre", org.ocap.si.StreamType.DSM_CC_UN,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent80 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails44, 12, 12, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "deu" },
                        new String[] { "serviceComponent80" }), "deu",
                org.ocap.si.StreamType.DSM_CC_STREAM_DESCRIPTORS, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent81 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails45, 13, 13, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent81" }), "eng", org.ocap.si.StreamType.DSM_CC_SECTIONS,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent82 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails46, 14, 14, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent82" }), "eng", org.ocap.si.StreamType.AUXILIARY,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent83 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails47, 15, 15, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent83" }), "eng", org.ocap.si.StreamType.VIDEO_DCII,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent84 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails48, 16, 16, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent84" }), "fre", org.ocap.si.StreamType.ATSC_AUDIO,
                ServiceInformationType.DVB_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent85 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails49, 17, 17, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent85" }), "fre", org.ocap.si.StreamType.STD_SUBTITLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent86 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails50, 18, 18, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent86" }), "eng", org.ocap.si.StreamType.ISOCHRONOUS_DATA,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        serviceComponent105 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails33, 37, 37, 21, 46, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent105" }), "eng", org.ocap.si.StreamType.DSM_CC,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent106 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails34, 38, 38, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent106" }), "fre", org.ocap.si.StreamType.MPEG_PRIVATE_DATA,
                ServiceInformationType.DVB_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent107 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails35, 39, 39, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED, 30, new MultiString(
                        new String[] { "eng" }, new String[] { "serviceComponent107" }), "eng",
                org.ocap.si.StreamType.DSM_CC, ServiceInformationType.ATSC_PSIP, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent108 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails36, 40, 40, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent108" }), "fre", org.ocap.si.StreamType.MPEG_1_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent109 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails37, 41, 41, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent109" }), "fre", org.ocap.si.StreamType.MPEG_2_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        serviceComponent110 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails38, 42, 42, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent110" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_SECTION,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        serviceComponent111 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails39, 43, 43, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent111" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_DATA,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -12)), null);

        serviceComponent112 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails40, 44, 44, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent112" }), "eng", org.ocap.si.StreamType.MHEG,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -14)), null);

        serviceComponent113 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails41, 45, 45, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent113" }), "eng", org.ocap.si.StreamType.DSM_CC,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -16)), null);

        serviceComponent114 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails42, 46, 46, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent114" }), "eng", org.ocap.si.StreamType.H_222,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent115 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails43, 47, 47, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent115" }), "fre", org.ocap.si.StreamType.DSM_CC_MPE,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent116 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails44, 48, 48, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "deu" },
                        new String[] { "serviceComponent116" }), "deu", org.ocap.si.StreamType.DSM_CC_UN,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent117 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails45, 49, 49, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent117" }), "eng",
                org.ocap.si.StreamType.DSM_CC_STREAM_DESCRIPTORS, ServiceInformationType.SCTE_SI, new java.util.Date(
                        System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent118 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails46, 50, 50, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent118" }), "eng", org.ocap.si.StreamType.DSM_CC_SECTIONS,
                ServiceInformationType.SCTE_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -1)), null);

        serviceComponent119 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails47, 51, 51, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent119" }), "eng", org.ocap.si.StreamType.AUXILIARY,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -2)), null);

        serviceComponent120 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails48, 52, 52, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent120" }), "fre", org.ocap.si.StreamType.VIDEO_DCII,
                ServiceInformationType.DVB_SI, new java.util.Date(System.currentTimeMillis() + (1000 * -3)), null);

        serviceComponent121 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails49, 53, 2147483647, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "fre" },
                        new String[] { "serviceComponent121" }), "fre", org.ocap.si.StreamType.ATSC_AUDIO,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -4)), null);

        serviceComponent122 = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                serviceDetails50, 54, 2147483647, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "serviceComponent122" }), "eng", org.ocap.si.StreamType.STD_SUBTITLE,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null);

        aitComponent1 = new ServiceComponentImpl[] { new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(
                nextNativeHandle()), aitServiceDetails1, 100, 100, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "aitComponent100" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_SECTION,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };
        aitComponent1b = new ServiceComponentImpl[] { new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(
                nextNativeHandle()), aitServiceDetails1b, 101, 101, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                        new String[] { "aitComponent110b" }), "eng", org.ocap.si.StreamType.MPEG_PRIVATE_SECTION,
                ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };
        aitComponent2 = new ServiceComponentImpl[] {
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails2, 200, 200, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent200" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails2, 210, 210, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent210" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };
        aitComponent2b = new ServiceComponentImpl[] {
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails2b, 201, 201, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent201b" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails2b, 211, 211, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent211b" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };
        aitComponent3 = new ServiceComponentImpl[] {
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3, 300, 300, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent300" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3, 310, 310, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent310" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3, 320, 320, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent320" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };
        aitComponent3b = new ServiceComponentImpl[] {
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3b, 301, 301, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent301b" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3b, 311, 311, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent311b" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null),
                new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                        aitServiceDetails3b, 321, 321, ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED,
                        ServiceComponentImpl.CAROUSEL_ID_UNDEFINED, new MultiString(new String[] { "eng" },
                                new String[] { "aitComponent321b" }), "eng",
                        org.ocap.si.StreamType.MPEG_PRIVATE_SECTION, ServiceInformationType.ATSC_PSIP,
                        new java.util.Date(System.currentTimeMillis() + (1000 * -10)), null) };

        // Service descriptions
        dynamicServiceDescription1 = new ServiceDescriptionImpl(siCache, dynamicServiceDetails1, new MultiString(
                new String[] { "eng" }, new String[] { "Dynamic ServiceDescription 1" }), new java.util.Date(
                System.currentTimeMillis() - 1000), null);

        jmfServiceDescription1 = new ServiceDescriptionImpl(siCache, jmfServiceDetails1, new MultiString(
                new String[] { "eng" }, new String[] { "JMF ServiceDescription 1" }), new java.util.Date(
                System.currentTimeMillis() - 1000), null);

        jmfServiceDescription2 = new ServiceDescriptionImpl(siCache, jmfServiceDetails2, new MultiString(
                new String[] { "eng" }, new String[] { "JMF ServiceDescription 2" }), new java.util.Date(
                System.currentTimeMillis() - 1000), null);

        serviceDescription33 = new ServiceDescriptionImpl(siCache, serviceDetails33, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription33" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -1)), null);

        serviceDescription34 = new ServiceDescriptionImpl(siCache, serviceDetails34, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription34" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -2)), null);

        serviceDescription35 = new ServiceDescriptionImpl(siCache, serviceDetails35, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription35" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -3)), null);

        serviceDescription36 = new ServiceDescriptionImpl(siCache, serviceDetails36, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription36" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -4)), null);

        serviceDescription37 = new ServiceDescriptionImpl(siCache, serviceDetails37, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription37" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -10)), null);

        serviceDescription38 = new ServiceDescriptionImpl(siCache, serviceDetails38, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription38" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -10)), null);

        serviceDescription39 = new ServiceDescriptionImpl(siCache, serviceDetails39, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription39" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -12)), null);

        serviceDescription40 = new ServiceDescriptionImpl(siCache, serviceDetails40, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription40" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -14)), null);

        serviceDescription41 = new ServiceDescriptionImpl(siCache, serviceDetails41, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription41" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -16)), null);

        serviceDescription42 = new ServiceDescriptionImpl(siCache, serviceDetails42, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription42" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -1)), null);

        serviceDescription43 = new ServiceDescriptionImpl(siCache, serviceDetails43, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription43" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -2)), null);

        serviceDescription44 = new ServiceDescriptionImpl(siCache, serviceDetails44, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription44" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -3)), null);

        serviceDescription45 = new ServiceDescriptionImpl(siCache, serviceDetails45, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription45" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -4)), null);

        serviceDescription46 = new ServiceDescriptionImpl(siCache, serviceDetails46, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription46" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -1)), null);

        serviceDescription47 = new ServiceDescriptionImpl(siCache, serviceDetails47, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription47" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -2)), null);

        serviceDescription48 = new ServiceDescriptionImpl(siCache, serviceDetails48, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription48" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -3)), null);

        serviceDescription49 = new ServiceDescriptionImpl(siCache, serviceDetails49, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription49" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -4)), null);

        serviceDescription50 = new ServiceDescriptionImpl(siCache, serviceDetails50, new MultiString(
                new String[] { "eng" }, new String[] { "serviceDescription50" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -10)), null);

        aitServiceDescription1 = new ServiceDescriptionImpl(siCache, aitServiceDetails1, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription1" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);
        aitServiceDescription1b = new ServiceDescriptionImpl(siCache, aitServiceDetails1b, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription1b" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);
        aitServiceDescription2 = new ServiceDescriptionImpl(siCache, aitServiceDetails2, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription2" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);
        aitServiceDescription2b = new ServiceDescriptionImpl(siCache, aitServiceDetails2b, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription2b" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);
        aitServiceDescription3 = new ServiceDescriptionImpl(siCache, aitServiceDetails3, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription3" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);
        aitServiceDescription3b = new ServiceDescriptionImpl(siCache, aitServiceDetails3b, new MultiString(
                new String[] { "eng" }, new String[] { "aitServiceDescription3b" }), new java.util.Date(
                System.currentTimeMillis() + (1000 * -5)), null);

        // initialize the mappings.
        initMappings();

        ratingDimensionList.add(ratingDimension1);

        transports.add(transport1);
        transports.add(transport2);

        networks.add(network3);
        networks.add(network4);
        networks.add(network5);
        networks.add(network6);

        transportStreams.add(transportStream7);
        transportStreams.add(transportStream8);
        transportStreams.add(transportStream9);
        transportStreams.add(transportStream10);
        transportStreams.add(transportStream11);
        transportStreams.add(transportStream12);
        transportStreams.add(transportStream13);
        transportStreams.add(transportStream14);
        transportStreams.add(transportStream15);

        hiddenServices.add(dynamicService1);
        visibleServices.add(service15);
        visibleServices.add(service16);
        visibleServices.add(service17);
        visibleServices.add(service18);
        visibleServices.add(service19);
        visibleServices.add(service20);
        visibleServices.add(service21);
        visibleServices.add(service22);
        visibleServices.add(service23);
        visibleServices.add(service24);
        visibleServices.add(service25);
        visibleServices.add(service26);
        visibleServices.add(service27);
        visibleServices.add(service28);
        visibleServices.add(service29);
        visibleServices.add(service30);
        visibleServices.add(service31);
        visibleServices.add(service32);
        visibleServices.add(jmfService1);
        visibleServices.add(jmfService2);
        visibleServices.add(aitService1);
        visibleServices.add(aitService1b);
        visibleServices.add(aitService2);
        visibleServices.add(aitService2b);
        visibleServices.add(aitService3);
        visibleServices.add(aitService3b);
        allServices.addAll(hiddenServices);
        allServices.addAll(visibleServices);

        serviceDetailsList.add(serviceDetails33);
        serviceDetailsList.add(serviceDetails34);
        serviceDetailsList.add(serviceDetails35);
        serviceDetailsList.add(serviceDetails36);
        serviceDetailsList.add(serviceDetails37);
        serviceDetailsList.add(serviceDetails38);
        serviceDetailsList.add(serviceDetails39);
        serviceDetailsList.add(serviceDetails40);
        serviceDetailsList.add(serviceDetails41);
        serviceDetailsList.add(serviceDetails42);
        serviceDetailsList.add(serviceDetails43);
        serviceDetailsList.add(serviceDetails44);
        serviceDetailsList.add(serviceDetails45);
        serviceDetailsList.add(serviceDetails46);
        serviceDetailsList.add(serviceDetails47);
        serviceDetailsList.add(serviceDetails48);
        serviceDetailsList.add(serviceDetails49);
        serviceDetailsList.add(serviceDetails50);
        serviceDetailsList.add(dynamicServiceDetails1);
        serviceDetailsList.add(jmfServiceDetails1);
        serviceDetailsList.add(jmfServiceDetails2);
        serviceDetailsList.add(aitServiceDetails1);
        serviceDetailsList.add(aitServiceDetails1b);
        serviceDetailsList.add(aitServiceDetails2);
        serviceDetailsList.add(aitServiceDetails2b);
        serviceDetailsList.add(aitServiceDetails3);
        serviceDetailsList.add(aitServiceDetails3b);

        serviceComponentsList.add(serviceComponent69);
        serviceComponentsList.add(serviceComponent69eng);
        serviceComponentsList.add(serviceComponent69fre);
        serviceComponentsList.add(serviceComponent69spa);
        serviceComponentsList.add(serviceComponent70);
        serviceComponentsList.add(serviceComponent71);
        serviceComponentsList.add(serviceComponent72);
        serviceComponentsList.add(serviceComponent73);
        serviceComponentsList.add(serviceComponent74);
        serviceComponentsList.add(serviceComponent75);
        serviceComponentsList.add(serviceComponent76);
        serviceComponentsList.add(serviceComponent77);
        serviceComponentsList.add(serviceComponent78);
        serviceComponentsList.add(serviceComponent79);
        serviceComponentsList.add(serviceComponent80);
        serviceComponentsList.add(serviceComponent81);
        serviceComponentsList.add(serviceComponent82);
        serviceComponentsList.add(serviceComponent83);
        serviceComponentsList.add(serviceComponent84);
        serviceComponentsList.add(serviceComponent85);
        serviceComponentsList.add(serviceComponent86);
        serviceComponentsList.add(serviceComponent105);
        serviceComponentsList.add(serviceComponent106);
        serviceComponentsList.add(serviceComponent107);
        serviceComponentsList.add(serviceComponent108);
        serviceComponentsList.add(serviceComponent109);
        serviceComponentsList.add(serviceComponent110);
        serviceComponentsList.add(serviceComponent111);
        serviceComponentsList.add(serviceComponent112);
        serviceComponentsList.add(serviceComponent113);
        serviceComponentsList.add(serviceComponent114);
        serviceComponentsList.add(serviceComponent115);
        serviceComponentsList.add(serviceComponent116);
        serviceComponentsList.add(serviceComponent117);
        serviceComponentsList.add(serviceComponent118);
        serviceComponentsList.add(serviceComponent119);
        serviceComponentsList.add(serviceComponent120);
        serviceComponentsList.add(serviceComponent121);
        serviceComponentsList.add(serviceComponent122);
        serviceComponentsList.add(dynamicServiceComponent1V);
        serviceComponentsList.add(dynamicServiceComponent1A1);
        serviceComponentsList.add(jmfServiceComponent1V);
        serviceComponentsList.add(jmfServiceComponent1A1);
        serviceComponentsList.add(jmfServiceComponent1A2);
        serviceComponentsList.add(jmfServiceComponent1S1);
        serviceComponentsList.add(jmfServiceComponent1S2);
        serviceComponentsList.add(jmfServiceComponent1D1);
        serviceComponentsList.add(jmfServiceComponent1D2);
        serviceComponentsList.add(jmfServiceComponent2V);
        serviceComponentsList.add(jmfServiceComponent2A1);
        serviceComponentsList.add(jmfServiceComponent2A2);
        serviceComponentsList.add(jmfServiceComponent2S1);
        serviceComponentsList.add(jmfServiceComponent2S2);
        serviceComponentsList.add(jmfServiceComponent2D1);
        serviceComponentsList.add(jmfServiceComponent2D2);
        for (int i = 0; i < aitComponent1.length; ++i)
            serviceComponentsList.add(aitComponent1[i]);
        for (int i = 0; i < aitComponent1b.length; ++i)
            serviceComponentsList.add(aitComponent1b[i]);
        for (int i = 0; i < aitComponent2.length; ++i)
            serviceComponentsList.add(aitComponent2[i]);
        for (int i = 0; i < aitComponent2b.length; ++i)
            serviceComponentsList.add(aitComponent2b[i]);
        for (int i = 0; i < aitComponent3.length; ++i)
            serviceComponentsList.add(aitComponent3[i]);
        for (int i = 0; i < aitComponent3b.length; ++i)
            serviceComponentsList.add(aitComponent3b[i]);

        serviceDescriptionsList.add(serviceDescription33);
        serviceDescriptionsList.add(serviceDescription34);
        serviceDescriptionsList.add(serviceDescription35);
        serviceDescriptionsList.add(serviceDescription36);
        serviceDescriptionsList.add(serviceDescription37);
        serviceDescriptionsList.add(serviceDescription38);
        serviceDescriptionsList.add(serviceDescription39);
        serviceDescriptionsList.add(serviceDescription40);
        serviceDescriptionsList.add(serviceDescription41);
        serviceDescriptionsList.add(serviceDescription42);
        serviceDescriptionsList.add(serviceDescription43);
        serviceDescriptionsList.add(serviceDescription44);
        serviceDescriptionsList.add(serviceDescription45);
        serviceDescriptionsList.add(serviceDescription46);
        serviceDescriptionsList.add(serviceDescription47);
        serviceDescriptionsList.add(serviceDescription48);
        serviceDescriptionsList.add(serviceDescription49);
        serviceDescriptionsList.add(serviceDescription50);
        serviceDescriptionsList.add(dynamicServiceDescription1);
        serviceDescriptionsList.add(jmfServiceDescription1);
        serviceDescriptionsList.add(jmfServiceDescription2);
        serviceDescriptionsList.add(aitServiceDescription1);
        serviceDescriptionsList.add(aitServiceDescription1b);
        serviceDescriptionsList.add(aitServiceDescription2);
        serviceDescriptionsList.add(aitServiceDescription2b);
        serviceDescriptionsList.add(aitServiceDescription3);
        serviceDescriptionsList.add(aitServiceDescription3b);
    }

    /**
     * See
     * 
     * @link SIDatabase for method definition.<br/>
     *       See RatingDimensionMap field detail for the maps for regions. 1
     *       dimension per the 3 regions right now.
     */
    public RatingDimensionHandle[] getSupportedDimensions()
    {
        RatingDimensionHandle[] rdhs = new RatingDimensionHandle[ratingDimensionList.size()];
        for (int i = 0; i < ratingDimensionList.size(); i++)
        {
            RatingDimensionHandleImpl rdh = (RatingDimensionHandleImpl) ((RatingDimensionExt) ratingDimensionList.get(i)).getRatingDimensionHandle();
            rdhs[i] = rdh;
            cannedIncrementRequestedByCount(rdh);
        }
        return rdhs;
    }

    /**
     * The increment key that is update is simply keyed on the name param.
     * 
     * @param name
     * @return
     * @throws SIRequestInvalidException
     * @throws SILookupFailedException
     */
    public RatingDimensionHandle getRatingDimensionByName(String name) throws SIRequestInvalidException,
            SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        if (name == null) throw new NullPointerException("rating dimension name is null");
        cannedIncrementRequestedByCount(name);
        Iterator iter = ratingDimensionList.iterator();
        RatingDimensionImpl rdi = null;
        while (iter.hasNext())
        {
            rdi = (RatingDimensionImpl) iter.next();
            if (rdi.getDimensionName().equals(name)) return rdi.getRatingDimensionHandle();
        }
        throw new SIRequestInvalidException("rating dimension was not found by name in the canned data.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method definition.<br/>
     *       This will return one of the precreated rating dimensions.
     * 
     */
    public RatingDimensionExt createRatingDimension(RatingDimensionHandle ratingDimensionHandle)
            throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckHandle(ratingDimensionHandle, "RatingDimensionHandle", RatingDimensionHandle.class);
        if (ratingDimensionHandle == null) throw new NullPointerException("rating dimension handle is null");
        RatingDimensionExt dimension = null;
        boolean found = false;
        for (int i = 0; i < ratingDimensionList.size(); i++)
        {
            dimension = (RatingDimensionExt) ratingDimensionList.get(i);
            if (((RatingDimensionHandleImpl) dimension.getRatingDimensionHandle()).handle == ((RatingDimensionHandleImpl) ratingDimensionHandle).handle)
            {
                found = true;
                break;
            }
        }
        if (found)
        {
            cannedIncrementCreateCount(ratingDimensionHandle);
            short numLevels = dimension.getNumberOfLevels();
            return new RatingDimensionImpl(siCache, ratingDimensionHandle, dimension.getDimensionNameAsMultiString(),
                    numLevels, dimension.getDescriptionsAsMultiStringArray(), null);
        }
        else
            throw new SIRequestInvalidException("RatingDimensionHandle was invalid");
    }

    /**
     * See
     * 
     * @link SIDatabase for method definition.<br/>
     *       Returns the available transports.<br/>
     */
    public TransportHandle[] getAllTransports()
    {
        TransportHandleImpl[] tsir = new TransportHandleImpl[transports.size()];
        Iterator iter = transports.iterator();
        TransportExt tse = null;
        int i = 0;
        while (iter.hasNext())
        {
            tse = (TransportExt) iter.next();
            tsir[i++] = new TransportHandleImpl(((TransportHandleImpl) tse.getTransportHandle()).handle);
        }
        return tsir;
    }

    /**
     * The count incremented will be available under the key "transportID" + the
     * transportID param as a string.
     * 
     * @param transportID
     * @return
     * @throws SIRequestInvalidException
     * @throws SILookupFailedException
     */
    public TransportHandle getTransportByID(int transportID) throws SIRequestInvalidException, SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount("transportID" + transportID);
        Iterator iter = transports.iterator();
        TransportImpl ti = null;
        while (iter.hasNext())
        {
            ti = (TransportImpl) iter.next();
            if (ti.getTransportID() == transportID) return ti.getTransportHandle();
        }
        throw new SIRequestInvalidException("The requested transportID was not available in the canned test data.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     */
    public TransportExt createTransport(TransportHandle transportHandle) throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckHandle(transportHandle, "transportHandle", TransportHandle.class);
        cannedIncrementCreateCount(transportHandle);
        Iterator iter = transports.iterator();
        TransportExt tse = null;
        while (iter.hasNext())
        {
            tse = (TransportExt) iter.next();
            if (equalHandles(tse.getTransportHandle(), transportHandle))
            {
                return new TransportImpl(siCache, transportHandle, tse.getDeliverySystemType(), tse.getTransportID(),
                        null);
            }
        }
        throw new SIRequestInvalidException(
                "The canned implementation requires creating with a handle that is predefined.");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior.
     */
    public NetworkHandle[] getNetworksByTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(transportHandle, "transportHandle", TransportHandle.class);
        cannedIncrementRequestedByCount(transportHandle);
        Iterator iter = getIterator(transportToNetworkMapping, transportHandle);
        List ret = new LinkedList();
        NetworkExt ne = null;
        while (iter.hasNext())
        {
            ne = (NetworkExt) iter.next();
            ret.add(new NetworkHandleImpl(((NetworkHandleImpl) ne.getNetworkHandle()).handle));
        }

        return (NetworkHandle[]) ret.toArray(new NetworkHandleImpl[ret.size()]);

    }

    // Description copied from SIDatabase
    public NetworkHandle getNetworkByID(TransportHandle transportHandle, int networkID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(transportHandle, "transportHandle", TransportHandle.class);
        NetworkExt n = null;
        Iterator iter = getIterator(transportToNetworkMapping, transportHandle);
        while (iter.hasNext())
        {
            n = (NetworkExt) iter.next();
            if (n.getNetworkID() == networkID)
                return new NetworkHandleImpl(((NetworkHandleImpl) n.getNetworkHandle()).handle);
        }
        throw new SIRequestInvalidException(
                "Requested network was not available on the specified transport with that ID");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior.<br/>
     *       based on handle will return the correct network in the precreated
     *       set network 3 - 6. If the supplied handle is out of that range then
     *       the SIRequestInvalidException will be thrown.<br/>
     */
    public NetworkExt createNetwork(NetworkHandle networkHandle) throws SIRequestInvalidException,
            SINotAvailableException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(networkHandle, "networkHandle", NetworkHandle.class);
        cannedIncrementCreateCount(networkHandle);
        NetworkExt base = null;
        int handle = ((NetworkHandleImpl) networkHandle).handle;
        /*
         * switch (((NetworkHandleImpl)networkHandle).handle) { case 3: base =
         * network3; break; case 4: base = network4; break; case 5: base =
         * network5; break; case 6: base = network6; break; default: throw new
         * SIRequestInvalidException("The requested network handle was not
         * valid"); }
         */
        Iterator it = networks.iterator();
        while (it.hasNext())
        {
            NetworkExt tempNet = (NetworkExt) it.next();
            int tempHandle = ((NetworkHandleImpl) tempNet.getNetworkHandle()).handle;
            if (tempHandle == handle)
            {
                base = tempNet;
                break;
            }
        }
        if (base == null) throw new SIRequestInvalidException("Network not found");
        return new NetworkImpl(siCache, networkHandle, (TransportExt) base.getTransport(), base.getNetworkID(),
                base.getName(), base.getServiceInformationType(), base.getUpdateTime(), null);
    }

    /**
     * see
     * 
     * @link SIDatabase for methodbehavior.<br/>
     *       returns transportStreams based on the transport they were created
     *       with. See the field detail for transportStreams to see which
     *       transports are associated with which TransportStreams.
     */
    public TransportStreamHandle[] getTransportStreamsByTransport(TransportHandle transportHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(transportHandle, "transportHandle", TransportHandle.class);
        cannedIncrementRequestedByCount(transportHandle);
        Iterator iter = getIterator(transportToTransportStreamMapping, transportHandle);
        TransportStreamExt tse = null;
        LinkedList returner = new LinkedList();
        while (iter.hasNext())
        {
            tse = (TransportStreamExt) iter.next();
            returner.add(new TransportStreamHandleImpl(
                    ((TransportStreamHandleImpl) tse.getTransportStreamHandle()).handle));
        }
        return (TransportStreamHandle[]) returner.toArray(new TransportStreamHandle[returner.size()]);
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior<br/>
     *       returns a TransportStreamHandle[]s of transportStreams based on how
     *       the streams were created.<br/>
     *       see the field detail for transportStreams for details on which
     *       TransportStreams were created with which network.
     */
    public TransportStreamHandle[] getTransportStreamsByNetwork(NetworkHandle networkHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(networkHandle, "networkHandle", NetworkHandle.class);
        cannedIncrementRequestedByCount(networkHandle);
        List returner = new LinkedList();
        TransportStreamExt tse = null;
        Iterator iter = getIterator(networkToTransportStreamMapping, networkHandle);
        while (iter.hasNext())
        {
            tse = (TransportStreamExt) iter.next();
            returner.add(new TransportStreamHandleImpl(
                    ((TransportStreamHandleImpl) tse.getTransportStreamHandle()).handle));
        }
        return (TransportStreamHandle[]) returner.toArray(new TransportStreamHandle[returner.size()]);
    }

    // Description copied from SIDatabase
    // have fun testing this one
    public TransportStreamHandle getTransportStreamByID(TransportHandle transportHandle, int frequency, int mode,
            int tsID) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(transportHandle, "transportHandle", TransportHandle.class);
        cannedIncrementRequestedByCount(transportHandle);
        TransportStreamExt ts = null;
        Iterator iter = getIterator(transportToTransportStreamMapping, transportHandle);
        while (iter.hasNext())
        {
            ts = (TransportStreamExt) iter.next();
            if (frequency == ts.getFrequency())
            {
                if (ts.getTransportStreamID() == tsID || tsID == -1)
                {
                    return new TransportStreamHandleImpl(
                            ((TransportStreamHandleImpl) ts.getTransportStreamHandle()).handle);
                }
            }
        }
        throw new SIRequestInvalidException("No matching TransportStream in the canned data.");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior<br/>
     *       returns one of the predefined TransportStreams, else throws an
     *       SIRequestInvalidException (for canned environment).
     */
    public TransportStreamExt createTransportStream(TransportStreamHandle transportStreamHandle)
            throws SIRequestInvalidException, SINotAvailableException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(transportStreamHandle, "transportStreamHandle", TransportStreamHandle.class);
        cannedIncrementCreateCount(transportStreamHandle);
        Iterator iter = transportStreams.iterator();
        TransportStreamExt tse = null;
        while (iter.hasNext())
        {
            tse = (TransportStreamExt) iter.next();
            if (equalHandles(tse.getTransportStreamHandle(), transportStreamHandle))
            {
                return new TransportStreamImpl(siCache, transportStreamHandle, (TransportExt) tse.getTransport(),
                        tse.getFrequency(), tse.getModulationFormat(), tse.getNetwork(), tse.getTransportStreamID(),
                        tse.getDescription(), tse.getServiceInformationType(), tse.getUpdateTime(), null);
            }
        }
        throw new SIRequestInvalidException(
                "You cannot \"create\" a transport stream in the canned environment with a handle ouside of the range [7-14]");
    }

    /**
     * Returns all services
     */
    public ServiceHandle[] getAllServices()
    {
        ServiceHandle[] serviceHandles = new ServiceHandle[visibleServices.size()];

        Iterator iter = visibleServices.iterator();
        int counter = 0;
        while (iter.hasNext())
        {
            ServiceExt service = (ServiceExt) iter.next();
            serviceHandles[counter++] = new ServiceHandleImpl(((ServiceHandleImpl) service.getServiceHandle()).handle);
        }
        return serviceHandles;
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior.
     * 
     *       Note: the hardcoded services have sourceids in the range [100-117]
     */
    public ServiceHandle getServiceBySourceID(int sourceID) throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount(new Integer(sourceID));
        Iterator iter = allServices.iterator();
        ServiceExt se = null;
        while (iter.hasNext())
        {
            se = (ServiceExt) iter.next();
            if (((OcapLocator) se.getLocator()).getSourceID() == sourceID)
                return new ServiceHandleImpl(((ServiceHandleImpl) se.getServiceHandle()).handle);
        }
        throw new SIRequestInvalidException("the sourceid was not in the supported range.");
    }

    public ServiceHandle[] getServicesBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
    SINotAvailableYetException, SILookupFailedException
    {
        return null;
    }
    public ServiceDetailsHandle[] getServiceDetailsBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
    SINotAvailableYetException, SILookupFailedException
    {
        return null;
    }
    
    /**
     * see
     * 
     * @link SIDatabase for method behavior.
     * 
     */
    public ServiceHandle getServiceByAppId(int appID) throws SIRequestInvalidException
    {
        // Fix this!!
        throw new SIRequestInvalidException("not implemented...");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior.<br/>
     *       Canned version throws the exception for null, empty, or unfound
     *       service names.
     */
    public ServiceHandle getServiceByServiceName(String serviceName) throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount(serviceName);
        if (serviceName == null || "".equals(serviceName))
            throw new SIRequestInvalidException("invalid service name.");
        Iterator iter = allServices.iterator();
        ServiceImpl si = null;
        while (iter.hasNext())
        {
            si = (ServiceImpl) iter.next();
            if (si.getName().equals(serviceName))
                return new ServiceHandleImpl(((ServiceHandleImpl) si.getServiceHandle()).handle);
        }
        throw new SIRequestInvalidException(serviceName + " is an invalid service name.");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior.<br/>
     *       Canned version throws the exception for null, empty, or unfound
     *       service number.
     */
    public ServiceHandle getServiceByServiceNumber(int serviceNumber, int minorNumber) throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount("serviceNumber (" + serviceNumber + "." + minorNumber + ")");
        Iterator iter = allServices.iterator();
        ServiceImpl si = null;
        while (iter.hasNext())
        {
            si = (ServiceImpl) iter.next();
            if (si.getServiceNumber() == serviceNumber && si.getMinorNumber() == minorNumber)
                return new ServiceHandleImpl(((ServiceHandleImpl) si.getServiceHandle()).handle);
        }
        throw new SIRequestInvalidException("Service number (" + serviceNumber + "." + minorNumber + ") is invalid.");
    }

    /**
     * Description copied from SIDatabase
     */
    public ServiceHandle getServiceByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount("" + frequency + programNumber);
        Iterator iter = serviceDetailsList.iterator();
        ServiceDetailsImpl dsi = null;
        while (iter.hasNext())
        {
            dsi = (ServiceDetailsImpl) iter.next();
            if (((TransportStreamExt) dsi.getTransportStream()).getFrequency() == frequency
                    && dsi.getProgramNumber() == programNumber
                    && (modulationFormat == -1 || ((TransportStreamExt) dsi.getTransportStream()).getModulationFormat() == modulationFormat))
            {
                ServiceImpl service = (ServiceImpl) getObjectForHandle(serviceDetailsToServiceMapping,
                        dsi.getServiceDetailsHandle());
                return new ServiceHandleImpl(((SIHandleImpl) service.getServiceHandle()).handle);
            }
        }
        throw new SIRequestInvalidException("Service for frequency: " + frequency + " modulation: " + modulationFormat
                + " and programNumber: " + programNumber + " not found");
    }

    /**
     * Description copied from SIDatabase<br/>
     * returns the precreated service matching the parameters passed in, or in
     * this implementation throws the exception if the handle is not found
     * internally.
     */
    public ServiceExt createService(ServiceHandle serviceHandle) throws SIRequestInvalidException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckHandle(serviceHandle, "serviceHandle", ServiceHandle.class);
        cannedIncrementCreateCount(serviceHandle);
        ServiceImpl svc = null;
        Iterator iter = allServices.iterator();
        while (iter.hasNext())
        {
            svc = (ServiceImpl) iter.next();
            if (equalHandles(svc.getServiceHandle(), serviceHandle))
            {
                return new ServiceImpl(siCache, serviceHandle, svc.getNameAsMultiString(), svc.hasMultipleInstances(),
                        svc.getServiceType(), svc.getServiceNumber(), svc.getMinorNumber(), svc.getLocator(), null);
            }

        }
        throw new SIRequestInvalidException(
                "the requested handle was not found in the testing data, therefore is uncreatable in the canned environment.");
    }

    /**
     * see
     * 
     * @link SIDatabase for method behavior<br/>
     *       You may find the ServiceDetails that should be returned for any
     *       given service handle. note: due to the current 1-1 implementation
     *       this method always returns an array of size 1 for known service
     *       handles.
     */
    public ServiceDetailsHandle[] getServiceDetailsByService(ServiceHandle serviceHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedIncrementRequestedByCount(serviceHandle);
        cannedCheckHandle(serviceHandle, "serviceHandle", ServiceHandle.class);

        LinkedList returner = new LinkedList();
        Iterator iter = getIterator(serviceToServiceDetailsMapping, serviceHandle);
        ServiceDetailsImpl sdi = null;
        while (iter.hasNext())
        {
            sdi = (ServiceDetailsImpl) iter.next();
            returner.add(new ServiceDetailsHandleImpl(((SIHandleImpl) sdi.getServiceDetailsHandle()).handle));
        }
        return (ServiceDetailsHandle[]) returner.toArray(new ServiceDetailsHandle[returner.size()]);
    }

    /**
     * Description copied from SIDatabase Note: the canned implementation will
     * return the precreated ServiceDetails
     */
    public ServiceDetailsExt createServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementCreateCount(serviceDetailsHandle);
        Iterator iter = serviceDetailsList.iterator();
        ServiceDetailsImpl sd = null;
        while (iter.hasNext())
        {
            sd = (ServiceDetailsImpl) iter.next();
            if (equalHandles(sd.getServiceDetailsHandle(), serviceDetailsHandle))
            {
                return new ServiceDetailsImpl(siCache, serviceDetailsHandle, sd.getSourceID(), sd.getProgramNumber(),
                        sd.getTransportStream(), sd.getLongNameAsMultiString(), sd.getService(),
                        sd.getDeliverySystemType(), sd.getServiceInformationType(), sd.getUpdateTime(),
                        sd.getCASystemIDs(), sd.isFree(), 0x1FFF, null);
            }
        }
        throw new SIRequestInvalidException("The requested service details does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior.<br/>
     *       The canned implementation returns a precreated service description
     *       for a given serviceDetailsHandle.
     */
    public ServiceDescriptionExt createServiceDescription(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException
    {

        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementCreateCount(serviceDetailsHandle);
        ServiceDescriptionImpl desc = (ServiceDescriptionImpl) getObjectForHandle(
                serviceDetailsToServiceDescriptionMapping, serviceDetailsHandle);
        return new ServiceDescriptionImpl(siCache, desc.getServiceDetails(), desc.getServiceDescriptionAsMultiString(),
                desc.getUpdateTime(), null);
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     * 
     */
    public ServiceComponentHandle[] getServiceComponentsByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle);
        LinkedList returner = new LinkedList();
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            sci = (ServiceComponentImpl) iter.next();
            returner.add(new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle));
        }
        return (ServiceComponentHandle[]) returner.toArray(new ServiceComponentHandle[returner.size()]);
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     */
    public ServiceComponentHandle[] getDefaultMediaComponentsByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle);
        LinkedList returner = new LinkedList();
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        boolean gotVideo = false;
        boolean gotAudio = false;
        while (iter.hasNext())
        {
            sci = (ServiceComponentImpl) iter.next();
            if (!gotVideo && sci.getStreamType() == StreamType.VIDEO)
            {
                returner.add(new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle));
                gotVideo = true;
            }
            if (!gotAudio && sci.getStreamType() == StreamType.AUDIO)
            {
                returner.add(new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle));
                gotAudio = true;
            }
            if (gotVideo && gotAudio) break;
        }

        if (!gotVideo && !gotAudio) throw new SINotAvailableException("No Default Components Available");

        return (ServiceComponentHandle[]) returner.toArray(new ServiceComponentHandle[returner.size()]);
    }

    /**
     * @see
     * @link SIDatabase for method behavior<br/>
     *       returns a ServiceComponentImpl based on service details handle and
     *       PID, throws SIRequestInvalidException if there is no such
     *       component.
     */
    public ServiceComponentHandle getServiceComponentByPID(ServiceDetailsHandle serviceDetailsHandle, int pid)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle.toString() + pid);
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            sci = (ServiceComponentImpl) iter.next();
            if (sci.getPID() == pid)
            {
                return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * @see
     * @link SIDatabase for method behavior<br/>
     *       returns a ServiceComponentImpl based on service details handle and
     *       componentTag, throws SIRequestInvalidException if there is no such
     *       component.
     */
    public ServiceComponentHandle getServiceComponentByTag(ServiceDetailsHandle serviceDetailsHandle, int tag)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle.toString() + tag);
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            try
            {
                sci = (ServiceComponentImpl) iter.next();
                if (sci.getComponentTag() == tag)
                {
                    return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
                }
            }
            catch (Exception e)
            {
                // skip the component if it does not have a component tag...
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior.<br/>
     *       Locates a service component by name from the preexisting list of
     *       components. Throws the SIRequestInvalidException if there isn't one
     *       found
     */
    public ServiceComponentHandle getServiceComponentByName(ServiceDetailsHandle serviceDetailsHandle, String name)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle + name);
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            sci = (ServiceComponentImpl) iter.next();
            if (sci.getName().equals(name))
            {
                return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     */
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle.toString());

        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            try
            {
                sci = (ServiceComponentImpl) iter.next();
                sci.getCarouselID(); // if this succeeds the component has a
                                     // carousel ID
                return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
            }
            catch (Exception e)
            {
                // skip the component if it does not have a carousel ID...
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     */
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle,
            int carouselID) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle.toString());

        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            try
            {
                sci = (ServiceComponentImpl) iter.next();
                if (sci.getCarouselID() == carouselID)
                {
                    return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
                }
            }
            catch (Exception e)
            {
                // skip the component if it does not have a carousel ID...
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior<br/>
     */
    public ServiceComponentHandle getComponentByAssociationTag(ServiceDetailsHandle serviceDetailsHandle,
            int associationTag) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailableYet();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceDetailsHandle, "serviceDetailsHandle", ServiceDetailsHandle.class);
        cannedIncrementRequestedByCount(serviceDetailsHandle.toString() + associationTag);
        Iterator iter = getIterator(serviceDetailsToServiceComponentMapping, serviceDetailsHandle);
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            try
            {
                sci = (ServiceComponentImpl) iter.next();
                if (sci.getAssociationTag() == associationTag)
                {
                    return new ServiceComponentHandleImpl(((SIHandleImpl) sci.getServiceComponentHandle()).handle);
                }
            }
            catch (Exception e)
            {
                // skip the component if it does not have an association tag...
            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    /**
     * See
     * 
     * @link SIDatabase for method behavior.<br/>
     *       The canned implementation returns one of the precreated service
     *       components, or throws the SIRequestInvalidException if a create is
     *       called on a component that doesn't exist in the canned
     *       implementation.
     */
    public ServiceComponentExt createServiceComponent(ServiceComponentHandle serviceComponentHandle)
            throws SIRequestInvalidException, SINotAvailableException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedCheckForcedSINotAvailable();
        cannedCheckHandle(serviceComponentHandle, "serviceComponentHandle", ServiceComponentHandle.class);
        cannedIncrementCreateCount(serviceComponentHandle);
        Iterator iter = serviceComponentsList.iterator();
        ServiceComponentImpl sci = null;
        while (iter.hasNext())
        {
            sci = (ServiceComponentImpl) iter.next();
            if (equalHandles(serviceComponentHandle, sci.getServiceComponentHandle()))
            {
                long componentTag = ServiceComponentImpl.COMPONENT_TAG_UNDEFINED;
                try
                {
                    componentTag = sci.getComponentTag();
                }
                catch (Exception e)
                {
                    // This exception should not be possible with this canned
                    // data because this field
                    // should always be specified. To support undefined values
                    // you need to use the value
                    // "COMPONENT_TAG_UNDEFINED".
                }
                long associationTag = ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED;
                try
                {
                    associationTag = sci.getAssociationTag();
                }
                catch (Exception e)
                {
                    // This exception should not be possible with this canned
                    // data because this field
                    // should always be specified. To support undefined values
                    // you need to use the value
                    // "ASSOCIATION_TAG_UNDEFINED".
                }
                long carouselID = ServiceComponentImpl.CAROUSEL_ID_UNDEFINED;
                try
                {
                    carouselID = sci.getCarouselID();
                }
                catch (Exception e)
                {
                    // This exception should not be possible with this canned
                    // data because this field
                    // should always be specified. To support undefined values
                    // you need to use the value
                    // "CAROUSEL_ID_UNDEFINED".
                }
                return new ServiceComponentImpl(siCache, serviceComponentHandle, sci.getServiceDetails(), sci.getPID(),
                        componentTag, associationTag, carouselID, sci.getNameAsMultiString(),
                        sci.getAssociatedLanguage(), sci.getElementaryStreamType(), sci.getServiceInformationType(),
                        sci.getUpdateTime(), null);

            }
        }
        throw new SIRequestInvalidException("The requested ServiceComponent does not exist in the canned environment.");
    }

    // Description copied from SIDatabase
    public void addSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        acquiredListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeSIAcquiredListener(SIChangedListener listener, CallerContext context)
    {
        acquiredListeners.remove(listener);
    }

    // Description copied from SIDatabase
    public void addSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        acquiredListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeSIChangedListener(SIChangedListener listener, CallerContext context)
    {
        acquiredListeners.remove(listener);
    }

    // Description copied from SIDatabase
    public void addNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        networkChangeListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeNetworkChangeListener(NetworkChangeListener listener, CallerContext context)
    {
        networkChangeListeners.remove(listener);
    }

    // Description copied from SIDatabase
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        transportStreamChangeListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context)
    {
        transportStreamChangeListeners.remove(listener);
    }

    // Description copied from SIDatabase
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        serviceDetailsChangeListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context)
    {
        serviceDetailsChangeListeners.remove(listener);
    }

    // Description copied from SIDatabase
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        serviceComponentChangeListeners.add(listener);
    }

    // Description copied from SIDatabase
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context)
    {
        serviceComponentChangeListeners.remove(listener);
    }

    // canned behaviors.

    /**
     * Forces the specified exception to be thrown each time a
     * <code>SIDatabase</code> method is called that returns such an exception.
     * An exception is not forced if the value specified here is null.
     * 
     * @param e
     *            The exception to force or none if null.
     */
    public void cannedSetForcedException(Class c)
    {
        if (c == null) return;
        getForcedExceptions().add(c);
    }

    /**
     * @param e
     *            Used to remove the forcible throwing of the given exception
     *            type where possible.
     */
    public void cannedRemoveForcedException(Class c)
    {
        if (c == null) return;
        getForcedExceptions().remove(c);
    }

    /**
     * Used to remove all forced exceptions that may have been set.
     */
    public void cannedClearAllForcedExceptions()
    {
        getForcedExceptions().clear();
    }

    /**
     * Send a <code>SIAcquiredEvent</code> to all registered listeners.
     * 
     * @param e
     *            The event to send
     */
    public void cannedSendSIAcquired(SIChangedEvent e)
    {
        Iterator iter = acquiredListeners.iterator();
        while (iter.hasNext())
        {
            ((SIChangedListener) iter.next()).notifyChanged(e);
        }
    }

    /**
     * This method will take an existing SI object and will create a new SI
     * object based on it, with a new handle and a new update time. The new SI
     * object will be retured as a result of this call.
     * 
     * @param siObject
     *            the SI object you wish to use as the basis for creating the
     *            new SI object.
     * @return the new SI object
     */
    public Object cannedAddSI(SIElement siObject)
    {
        Object returner = null;
        if (siObject instanceof ServiceComponentImpl)
        {
            // create a new ServiceComponent based on a new handle and update
            // time
            ServiceComponentImpl sc = (ServiceComponentImpl) siObject;
            long componentTag = ServiceComponentImpl.COMPONENT_TAG_UNDEFINED;
            try
            {
                componentTag = sc.getComponentTag();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "COMPONENT_TAG_UNDEFINED".
            }
            long associationTag = ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED;
            try
            {
                associationTag = sc.getAssociationTag();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "ASSOCIATION_TAG_UNDEFINED".
            }
            long carouselID = ServiceComponentImpl.CAROUSEL_ID_UNDEFINED;
            try
            {
                carouselID = sc.getCarouselID();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "CAROUSEL_ID_UNDEFINED".
            }

            // create the new ServiceComponentImpl
            returner = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                    sc.getServiceDetails(), sc.getPID(), componentTag, associationTag, carouselID,
                    sc.getNameAsMultiString(), sc.getAssociatedLanguage(), sc.getElementaryStreamType(),
                    sc.getServiceInformationType(), new java.util.Date(System.currentTimeMillis()), null);
            serviceComponentsList.add(returner);
            ServiceDetailsHandleImpl sdHandle = (ServiceDetailsHandleImpl) ((ServiceDetailsExt) sc.getServiceDetails()).getServiceDetailsHandle();
            List list = (List) serviceDetailsToServiceComponentMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                serviceDetailsToServiceComponentMapping.put(new Integer(sdHandle.handle), list);
            }
            fireServiceComponentChangeEvent(new ServiceComponentChangeEvent(sc.getServiceDetails(), SIChangeType.ADD,
                    (ServiceComponentImpl) returner));

        }
        else if (siObject instanceof ServiceDetailsExt)
        {
            // create a new ServiceDetails based on a new handle and update time
            ServiceDetailsImpl sd = (ServiceDetailsImpl) siObject;
            // create the new ServiceComponentImpl
            returner = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                    sd.getSourceID(), sd.getProgramNumber(), sd.getTransportStream(), sd.getLongNameAsMultiString(),
                    sd.getService(), sd.getDeliverySystemType(), sd.getServiceInformationType(), new java.util.Date(
                            System.currentTimeMillis()), sd.getCASystemIDs(), sd.isFree(), 0x1FFF, null);
            serviceDetailsList.add(returner);
            ServiceHandleImpl sdHandle = (ServiceHandleImpl) ((ServiceExt) sd.getService()).getServiceHandle();
            List list = (List) serviceToServiceDetailsMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                serviceToServiceDetailsMapping.put(new Integer(sdHandle.handle), list);
            }
            fireServiceDetailsChangeEvent(new ServiceDetailsChangeEvent(
                    ((TransportStreamExt) sd.getTransportStream()).getTransport(), SIChangeType.ADD,
                    (ServiceDetails) returner));
        }
        else if (siObject instanceof TransportStreamExt)
        {
            // create a new TransportStream based on a new handle and update
            // time
            TransportStreamImpl ts = (TransportStreamImpl) siObject;
            // create the new TransportStreamImpl
            returner = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                    (TransportExt) ts.getTransport(), ts.getFrequency() + 10000, ts.getModulationFormat(),
                    ts.getNetwork(), ts.getTransportStreamID() + 100, ts.getDescription(),
                    ts.getServiceInformationType(), new Date(System.currentTimeMillis()), null);
            transportStreams.add(returner);
            // add this stream to the mapping for the transport it references.
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) ts.getTransport()).getTransportHandle();

            List list = (List) transportToTransportStreamMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                transportToTransportStreamMapping.put(new Integer(transportHandle.handle), list);
            }
            fireTransportStreamChangeEvent(new TransportStreamChangeEvent(
                    (TransportStreamCollection) ts.getTransport(), SIChangeType.ADD, (TransportStream) returner));
        }
        else if (siObject instanceof NetworkExt)
        {
            // create a new Network based on a new handle and update time
            NetworkImpl n = (NetworkImpl) siObject;
            // create the new NetworkImpl
            returner = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()),
                    (TransportExt) n.getTransport(), n.getNetworkID() + 100, n.getName(),
                    n.getServiceInformationType(), new Date(System.currentTimeMillis()), null);
            networks.add(returner);

            // add this network to the mapping for the transport it references.
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) n.getTransport()).getTransportHandle();

            List list = (List) transportToNetworkMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                transportToNetworkMapping.put(new Integer(transportHandle.handle), list);
            }
            fireNetworkChangeEvent(new NetworkChangeEvent((NetworkCollection) n.getTransport(), SIChangeType.ADD,
                    (Network) returner));
        }
        return returner;
    }

    /**
     * Used to remove an SI Object from that database. Will also remove the
     * object from all associations and will immediatly fire all relevant events
     * to Listeners.
     * 
     * @param siObject
     */
    public void cannedRemoveSI(Object siObject)
    {
        if (siObject instanceof ServiceComponentImpl)
        {
            // find the ServiceComponent in question
            ServiceComponentImpl sc = (ServiceComponentImpl) siObject;
            Iterator iter = serviceComponentsList.iterator();
            while (iter.hasNext())
            {
                if (((ServiceComponentImpl) iter.next()).equals(sc))
                {
                    iter.remove();
                    break;
                }
            }
            ServiceDetailsHandleImpl sdHandle = (ServiceDetailsHandleImpl) ((ServiceDetailsExt) sc.getServiceDetails()).getServiceDetailsHandle();
            List list = (List) serviceDetailsToServiceComponentMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.remove(sc);
            }
            fireServiceComponentChangeEvent(new ServiceComponentChangeEvent(sc.getServiceDetails(),
                    SIChangeType.REMOVE, (ServiceComponentImpl) sc));
        }
        else if (siObject instanceof ServiceDetailsExt)
        {
            // find the ServiceDetails in question
            ServiceDetailsImpl sc = (ServiceDetailsImpl) siObject;
            Iterator iter = serviceDetailsList.iterator();
            while (iter.hasNext())
            {
                if (((ServiceDetailsImpl) iter.next()).equals(sc))
                {
                    iter.remove();
                    break;
                }
            }
            ServiceHandleImpl sdHandle = (ServiceHandleImpl) ((ServiceExt) sc.getService()).getServiceHandle();
            List list = (List) serviceToServiceDetailsMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.remove(sc);
            }
            fireServiceDetailsChangeEvent(new ServiceDetailsChangeEvent(
                    ((TransportStreamExt) sc.getTransportStream()).getTransport(), SIChangeType.REMOVE,
                    (ServiceDetails) sc));
        }
        else if (siObject instanceof TransportStreamExt)
        {
            TransportStreamImpl ts = (TransportStreamImpl) siObject;
            Iterator iter = transportStreams.iterator();
            while (iter.hasNext())
            {
                if (((TransportStreamImpl) iter.next()).equals(ts))
                {
                    iter.remove();
                    break;
                }
            }
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) ts.getTransport()).getTransportHandle();
            List list = (List) transportToTransportStreamMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.remove(ts);
            }
            fireTransportStreamChangeEvent(new TransportStreamChangeEvent(
                    (TransportStreamCollection) ts.getTransport(), SIChangeType.REMOVE, (TransportStream) ts));
        }
        else if (siObject instanceof NetworkExt)
        {
            // create a new Network based on a new handle and update time
            NetworkImpl n = (NetworkImpl) siObject;
            Iterator iter = networks.iterator();
            while (iter.hasNext())
            {
                if ((((NetworkImpl) iter.next()).equals(n)))
                {
                    iter.remove();
                    break;
                }
            }
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) n.getTransport()).getTransportHandle();

            List list = (List) transportToNetworkMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.remove(n);
            }
            fireNetworkChangeEvent(new NetworkChangeEvent((NetworkCollection) n.getTransport(), SIChangeType.REMOVE,
                    (Network) n));
        }
    }

    /**
     * This method will take an existing SI object and will "update" it in the
     * database.<br/>
     * The updated object will differ from the original in update time, and the
     * modified version will be returned. <br/>
     * Also note, all associations and references will be updated to use the new
     * object, however the reference that was used as the "seed" for the update
     * WILL NOT BE MODIFIED.<br/>
     * All relevant listeners will be fired upon completion of this method.
     * 
     * @param siObject
     *            the SI object you wish to have "updated"
     * @return the new SI object
     */
    public Object cannedUpdateSI(Object siObject)
    {
        Object returner = null;
        if (siObject instanceof ServiceComponentImpl)
        {
            // create a new ServiceComponent based on a new handle and update
            // time
            ServiceComponentImpl sc = (ServiceComponentImpl) siObject;
            long componentTag = ServiceComponentImpl.COMPONENT_TAG_UNDEFINED;
            try
            {
                componentTag = sc.getComponentTag();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "COMPONENT_TAG_UNDEFINED".
            }
            long associationTag = ServiceComponentImpl.ASSOCIATION_TAG_UNDEFINED;
            try
            {
                associationTag = sc.getAssociationTag();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "ASSOCIATION_TAG_UNDEFINED".
            }
            long carouselID = ServiceComponentImpl.CAROUSEL_ID_UNDEFINED;
            try
            {
                carouselID = sc.getCarouselID();
            }
            catch (Exception e)
            {
                // This exception should not be possible with this canned data
                // because this field
                // should always be specified. To support undefined values you
                // need to use the value
                // "CAROUSEL_ID_UNDEFINED".
            }

            // create the new ServiceComponentImpl
            returner = new ServiceComponentImpl(siCache, new ServiceComponentHandleImpl(nextNativeHandle()),
                    sc.getServiceDetails(), sc.getPID(), componentTag, associationTag, carouselID,
                    sc.getNameAsMultiString(), sc.getAssociatedLanguage(), sc.getElementaryStreamType(),
                    sc.getServiceInformationType(), new java.util.Date(System.currentTimeMillis()), null);
            serviceComponentsList.remove(sc);
            serviceComponentsList.add(returner);
            ServiceDetailsHandleImpl sdHandle = (ServiceDetailsHandleImpl) ((ServiceDetailsExt) sc.getServiceDetails()).getServiceDetailsHandle();
            List list = (List) serviceDetailsToServiceComponentMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.remove(sc);
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                serviceDetailsToServiceComponentMapping.put(new Integer(sdHandle.handle), list);
            }
            fireServiceComponentChangeEvent(new ServiceComponentChangeEvent(sc.getServiceDetails(),
                    SIChangeType.MODIFY, (ServiceComponentImpl) returner));

        }
        else if (siObject instanceof ServiceDetailsExt)
        {
            // create a new ServiceDetails based on a new handle and update time
            ServiceDetailsImpl sd = (ServiceDetailsImpl) siObject;
            // create the new ServiceComponentImpl
            returner = new ServiceDetailsImpl(siCache, new ServiceDetailsHandleImpl(nextNativeHandle()),
                    sd.getSourceID(), sd.getProgramNumber(), sd.getTransportStream(), sd.getLongNameAsMultiString(),
                    sd.getService(), sd.getDeliverySystemType(), sd.getServiceInformationType(), new java.util.Date(
                            System.currentTimeMillis()), sd.getCASystemIDs(), sd.isFree(), 0x1FFF, null);
            serviceDetailsList.remove(sd);
            serviceDetailsList.add(returner);
            ServiceHandleImpl sdHandle = (ServiceHandleImpl) ((ServiceExt) sd.getService()).getServiceHandle();
            List list = (List) serviceToServiceDetailsMapping.get(new Integer(sdHandle.handle));
            if (list != null)
            {
                list.remove(sd);
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                serviceToServiceDetailsMapping.put(new Integer(sdHandle.handle), list);
            }
            fireServiceDetailsChangeEvent(new ServiceDetailsChangeEvent(
                    ((TransportStreamExt) sd.getTransportStream()).getTransport(), SIChangeType.MODIFY,
                    (ServiceDetails) returner));
        }
        else if (siObject instanceof TransportStreamExt)
        {
            // create a new TransportStream based on a new handle and update
            // time
            TransportStreamImpl ts = (TransportStreamImpl) siObject;
            // create the new TransportStreamImpl
            returner = new TransportStreamImpl(siCache, new TransportStreamHandleImpl(nextNativeHandle()),
                    (TransportExt) ts.getTransport(), ts.getFrequency(), ts.getModulationFormat(), ts.getNetwork(),
                    ts.getTransportStreamID(), ts.getDescription(), ts.getServiceInformationType(), new Date(
                            System.currentTimeMillis()), null);
            transportStreams.remove(ts);
            transportStreams.add(returner);
            // add this stream to the mapping for the transport it references.
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) ts.getTransport()).getTransportHandle();

            List list = (List) transportToTransportStreamMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.remove(ts);
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                transportToTransportStreamMapping.put(new Integer(transportHandle.handle), list);
            }
            fireTransportStreamChangeEvent(new TransportStreamChangeEvent(
                    (TransportStreamCollection) ts.getTransport(), SIChangeType.MODIFY, (TransportStream) returner));
        }
        else if (siObject instanceof NetworkExt)
        {
            // create a new Network based on a new handle and update time
            NetworkImpl n = (NetworkImpl) siObject;
            // create the new NetworkImpl
            returner = new NetworkImpl(siCache, new NetworkHandleImpl(nextNativeHandle()),
                    (TransportExt) n.getTransport(), n.getNetworkID(), n.getName(), n.getServiceInformationType(),
                    new Date(System.currentTimeMillis()), null);
            networks.remove(n);
            networks.add(returner);

            // add this network to the mapping for the transport it references.
            TransportHandleImpl transportHandle = (TransportHandleImpl) ((TransportExt) n.getTransport()).getTransportHandle();

            List list = (List) transportToNetworkMapping.get(new Integer(transportHandle.handle));
            if (list != null)
            {
                list.remove(n);
                list.add(returner);
            }
            else
            {
                list = new LinkedList();
                list.add(returner);
                transportToNetworkMapping.put(new Integer(transportHandle.handle), list);
            }
            fireNetworkChangeEvent(new NetworkChangeEvent((NetworkCollection) n.getTransport(), SIChangeType.MODIFY,
                    (Network) returner));
        }
        return returner;
    }

    // used to fire a change event to registered NetworkChangeListeners.
    private void fireNetworkChangeEvent(NetworkChangeEvent nce)
    {
        Iterator iter = networkChangeListeners.iterator();
        NetworkChangeListener ncl = null;
        while (iter.hasNext())
        {
            ncl = (NetworkChangeListener) iter.next();
            ncl.notifyChange(nce);
        }
    }

    // used to fire a change event to registered TransportStreamChangeListeners.
    private void fireTransportStreamChangeEvent(TransportStreamChangeEvent tsce)
    {
        Iterator iter = transportStreamChangeListeners.iterator();
        TransportStreamChangeListener tscl = null;
        while (iter.hasNext())
        {
            tscl = (TransportStreamChangeListener) iter.next();
            tscl.notifyChange(tsce);
        }
    }

    // used to fire a change event to registered serviceDetailsChangeListeners.
    private void fireServiceDetailsChangeEvent(ServiceDetailsChangeEvent sdce)
    {
        Iterator iter = serviceDetailsChangeListeners.iterator();
        ServiceDetailsChangeListener sdcl = null;
        while (iter.hasNext())
        {
            sdcl = (ServiceDetailsChangeListener) iter.next();
            sdcl.notifyChange(sdce);
        }
    }

    // used to fire a change event to registered
    // serviceComponentChangeListeners.
    private void fireServiceComponentChangeEvent(ServiceComponentChangeEvent scce)
    {
        Iterator iter = serviceComponentChangeListeners.iterator();
        ServiceComponentChangeListener sccl = null;
        while (iter.hasNext())
        {
            sccl = (ServiceComponentChangeListener) iter.next();
            sccl.notifyChange(scce);
        }
    }

    // used to test if we are forcing SIRequestInvalidExceptions.
    private void cannedCheckForcedSIRequestInvalid() throws SIRequestInvalidException
    {
        if (getForcedExceptions().contains(SIRequestInvalidException.class))
            throw new SIRequestInvalidException(
                    "This Exception was intentionally thrown by the canned implementation to test dependent code exception handling.");
    }

    // used to test if we are forcing SINotAvailableYetExceptions.
    private void cannedCheckForcedSINotAvailableYet() throws SINotAvailableYetException
    {
        siWasRequested = true;
        if (getForcedExceptions().contains(SINotAvailableYetException.class))
            throw new SINotAvailableYetException(
                    "This Exception was intentionally thrown by the canned implementation to test dependent code exception handling.");
    }

    // used to test if we are forcing SINotAvailableExceptions.
    private void cannedCheckForcedSINotAvailable() throws SINotAvailableException
    {
        if (getForcedExceptions().contains(SINotAvailableException.class))
            throw new SINotAvailableException(
                    "This Exception was intentionally thrown by the canned implementation to test dependent code exception handling.");
    }

    /**
     * Used to check an argument to any method taking a handle. Will throw the
     * SIRequestInvalidException for any null of non-Integer argument.
     * 
     * @param handle
     *            - the handle to check
     * @param fieldName
     *            - a convenience to have the field name embedded in the message
     *            of the thrown exception
     * @param c
     *            - the class (interface) that the handle should be an instance
     *            of.
     */
    private void cannedCheckHandle(Object handle, String fieldName, Class c) throws SIRequestInvalidException
    {

        if (handle == null || !(c.isInstance(handle)))
            throw new SIRequestInvalidException(fieldName + " must be non-null and an instance of: " + c.getName());
    }

    // used to increment the create count for a given handle
    private void cannedIncrementCreateCount(Object i)
    {
        Object o = handleCreateCountMap.get(i);
        if (o == null)
            handleCreateCountMap.put(i, new Integer(1));
        else
            handleCreateCountMap.put(i, new Integer(((Integer) o).intValue() + 1));
    }

    // used to increment the requested by count for a given handle
    private void cannedIncrementRequestedByCount(Object i)
    {
        Object o = handleRequestedByMap.get(i);
        if (o == null)
            handleRequestedByMap.put(i, new Integer(1));
        else
            handleRequestedByMap.put(i, new Integer(((Integer) o).intValue() + 1));
    }

    /**
     * Used to track the number of times a given handle has been used in a call
     * to <code>create...</code>. This method will return 0 for a handle that
     * has never been used to create an object.. <br/>
     * Note - the validity of handles is not checked in this method, only that
     * it is non-null.
     * 
     * @param handle
     *            - the object handle to look for.
     * @return the number of times the given <code>Integer</code> object handle
     *         has been used in a call to create.
     */
    public int cannedGetHandleCreateCount(Object handle)
    {
        Integer ret = null;
        if ((ret = (Integer) handleCreateCountMap.get(handle)) == null)
            return 0;
        else
            return ret.intValue();
    }

    /**
     * Used to track the number of times a given handle has been requested. This
     * method will return 0 for a handle that has never been requested. <br/>
     * Note - the validity of handles is not checked in this method, only that
     * it is non-null and an Integer.
     * 
     * @param integer
     *            - the object handle to look for.
     * @return the number of times the given object handle has been requested.
     */
    public int cannedGetHandleRequestedByCount(Object handle)
    {
        Integer ret = null;
        if ((ret = (Integer) handleRequestedByMap.get(handle)) == null)
            return 0;
        else
            return ret.intValue();
    }

    /**
     * Clears the internal counters that track the number of times requests by
     * handles have been made.
     */
    public void cannedClearAllRequestedByCount()
    {
        handleRequestedByMap.clear();
    }

    /**
     * Clears the internal counters that track the number of times
     * <code>create...</code> calls have occured.
     */
    public void cannedClearAllHandleCreateCount()
    {
        handleCreateCountMap.clear();
    }

    /**
     * resets the canned database, including all listeners and counts.
     * 
     */
    public void cannedFullReset()
    {
        this.cannedClearAllForcedExceptions();
        this.cannedClearAllHandleCreateCount();
        this.cannedClearAllRequestedByCount();
    }

    private boolean equalHandles(SIHandle lhs, SIHandle rhs)
    {
        if (((SIHandleImpl) lhs).handle == ((SIHandleImpl) rhs).handle) return true;
        return false;
    }

    private Iterator getIterator(Map m, SIHandle handle) throws SIRequestInvalidException
    {
        // return ((List)m.get(new
        // Integer(((SIHandleImpl)handle).handle))).iterator();
        return ((List) getObjectForHandle(m, handle)).iterator();
    }

    private Object getObjectForHandle(Map m, SIHandle handle) throws SIRequestInvalidException
    {
        if (m.containsKey(new Integer(((SIHandleImpl) handle).handle)))
            return m.get(new Integer(((SIHandleImpl) handle).handle));
        else
            throw new SIRequestInvalidException("Invalid handle");
    }

    private boolean siWasRequested = false;

    public void cannedResetSIWasRequested()
    {
        siWasRequested = false;
    }

    public boolean cannedSIWasRequested() throws InterruptedException
    {
        for (int i = 0; i < 30; i++)
        {
            if (siWasRequested) return true;
            Thread.sleep(100);
            // siWasRequested ? return true : Thread.sleep(100);
        }
        return false;
    }

    private List getForcedExceptions()
    {
        return forcedExceptions;
    }

    // inner classes for handles
    /*
     * Implementations of SIHandle's marker interfaces.
     */
    public static class SIHandleImpl implements SIHandle
    {
        public int handle;

        public SIHandleImpl(int handle)
        {
            this.handle = handle;
        }

        public SIHandleImpl(Integer handle)
        {
            this.handle = handle.intValue();
        }

        /* Used to copy a handle */
        public SIHandleImpl(SIHandle handle)
        {
            this.handle = ((SIHandleImpl) handle).handle;
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

        public int getHandle()
        {
            return this.handle;
        }

        public String toString()
        {
            return "SIHandle: " + handle;
        }
    }

    // Generate the next native handle
    private Integer nextNativeHandle()
    {
        return new Integer(nextHandle++);
    }

    public static class NetworkHandleImpl extends SIHandleImpl implements NetworkHandle
    {
        public NetworkHandleImpl(Integer handle)
        {
            super(handle);
        }

        public NetworkHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class TransportHandleImpl extends SIHandleImpl implements TransportHandle
    {
        public TransportHandleImpl(Integer handle)
        {
            super(handle);
        }

        public TransportHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class RatingDimensionHandleImpl extends SIHandleImpl implements RatingDimensionHandle
    {
        public RatingDimensionHandleImpl(Integer handle)
        {
            super(handle);
        }

        public RatingDimensionHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class ServiceComponentHandleImpl extends SIHandleImpl implements ServiceComponentHandle
    {
        public ServiceComponentHandleImpl(Integer handle)
        {
            super(handle);
        }

        public ServiceComponentHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class ServiceDetailsHandleImpl extends SIHandleImpl implements ServiceDetailsHandle
    {
        public ServiceDetailsHandleImpl(Integer handle)
        {
            super(handle);
        }

        public ServiceDetailsHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class ProgramMapTableHandleImpl extends SIHandleImpl implements ProgramMapTableHandle
    {
        public ProgramMapTableHandleImpl(Integer handle)
        {
            super(handle);
        }

        public ProgramMapTableHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class ProgramMapTableImpl2 implements ProgramMapTableExt
    {
        public ServiceComponentExt[] comp;

        public ServiceDetailsExt sde;

        public int[] version;

        private ProgramMapTableHandle handle;

        public ProgramMapTableImpl2(ProgramMapTableHandle handle, ServiceDetailsExt svc, ServiceComponentExt[] comp)
        {
            this(handle, svc, comp, null);
        }

        public ProgramMapTableImpl2(ProgramMapTableHandle handle, ServiceDetailsExt svc, ServiceComponentExt[] comp,
                int[] version)
        {
            this.comp = comp;
            this.version = version;
            this.handle = handle;
            this.sde = svc;
        }

        public Descriptor[] getOuterDescriptorLoop()
        {
            return new Descriptor[0];
        }

        public int getPcrPID()
        {
            return sde.getPcrPID();
        }

        public PMTElementaryStreamInfo[] getPMTElementaryStreamInfoLoop()
        {
            PMTElementaryStreamInfo[] streams = new PMTElementaryStreamInfo[comp.length];
            for (int i = 0; i < comp.length; ++i)
                streams[i] = new PMTElementaryStreamInfoImpl2(comp[i], (version == null) ? 0 : (version[i]));
            return streams;
        }

        public int getProgramNumber()
        {
            return sde.getProgramNumber();
        }

        public short getTableId()
        {
            // TODO implement getTableId
            return 0;
        }

        public Locator getLocator()
        {
            return sde.getLocator();
        }

        public ServiceInformationType getServiceInformationType()
        {
            // TODO implement getServiceInformationType
            return sde.getServiceInformationType();
        }

        public int getFrequency()
        {
            return ((TransportStreamImpl) sde.getTransportStream()).getFrequency();
        }

        public ProgramMapTableHandle getPMTHandle()
        {
            return handle;
        }

        public int getSourceID()
        {
            return sde.getSourceID();
        }

        public TransportStream getTransportStream()
        {
            return sde.getTransportStream();
        }

        public void setLocator(Locator locator)
        {
            // TODO implement setLocator
        }

        public Date getUpdateTime()
        {
            return new Date();
        }

        public int getServiceHandle()
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    public static class PMTElementaryStreamInfoImpl2 extends PMTElementaryStreamInfoImpl
    {
        public PMTElementaryStreamInfoImpl2(ServiceComponentExt sc, int ver)
        {
            super((OcapLocator) sc.getLocator(), sc.getElementaryStreamType(), (short) sc.getPID(), makeDesc(sc, ver));
        }

        private static Descriptor[] makeDesc(ServiceComponentExt sc, int version)
        {
            Descriptor d = null;

            switch (sc.getElementaryStreamType())
            {
                case org.ocap.si.StreamType.MPEG_PRIVATE_SECTION:
                    // We will signal optimized signaling for the ODD pids!
                    // And use 5lsbits + given version number for ver!
                    if ((sc.getPID() & 1) != 0)
                    {
                        int pidVer = (sc.getPID() + version) & 0x1F;
                        d = new DescriptorImpl((short) 0x6F, new byte[] { 0, 1, (byte) pidVer });
                    }
                    else
                        d = new DescriptorImpl((short) 0x6F, new byte[0]);
                    break;
            }

            return (d == null) ? new Descriptor[0] : new Descriptor[] { d };
        }
    }

    public static class ServiceHandleImpl extends SIHandleImpl implements ServiceHandle
    {
        public ServiceHandleImpl(Integer handle)
        {
            super(handle);
        }

        public ServiceHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public static class TransportStreamHandleImpl extends SIHandleImpl implements TransportStreamHandle
    {
        public TransportStreamHandleImpl(Integer handle)
        {
            super(handle);
        }

        public TransportStreamHandleImpl(int handle)
        {
            super(handle);
        }
    }

    public TransportStreamHandle getTransportStreamBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        cannedCheckForcedSIRequestInvalid();
        cannedIncrementRequestedByCount(new Integer(sourceID));
        Iterator iter = allServices.iterator();
        ServiceExt se = null;
        while (iter.hasNext())
        {
            se = (ServiceExt) iter.next();
            if (((OcapLocator) se.getLocator()).getSourceID() == sourceID)
            {
                try
                {
                    ServiceDetailsImpl sd = (ServiceDetailsImpl) se.getDetails();
                    TransportStreamImpl ts = (TransportStreamImpl) sd.getTransportStream();
                    return ts.getTransportStreamHandle();
                }
                catch (Exception exc)
                {
                    exc.printStackTrace();
                }
            }
        }

        return null;
    }

    public TransportStreamHandle getTransportStreamByProgramNumber(int frequency, int mode, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Method not implemented");
    }

    public ProgramAssociationTable createProgramAssociationTable(ProgramAssociationTableHandle patHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Method not implemented");
    }

    public ProgramMapTable createProgramMapTable(ServiceDetails details) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        return createProgramMapTable(new ProgramMapTableHandleImpl(
                ((ServiceDetailsImpl) details).getServiceDetailsHandle().getHandle()));
    }

    public ProgramMapTable createProgramMapTable(ProgramMapTableHandle pmtHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException
    {
        // throw new RuntimeException("Method not implemented");

        // Locate ServiceDetailsHandle
        ServiceDetailsHandle dsi = new ServiceDetailsHandleImpl(((ProgramMapTableHandleImpl) pmtHandle).handle);

        // Locate ServiceComponents
        ServiceComponentImpl[] comp = (ServiceComponentImpl[]) ((List) getObjectForHandle(
                serviceDetailsToServiceComponentMapping, dsi)).toArray(new ServiceComponentImpl[0]);

        // Locate location info for the service
        // Locate the ServiceDetails based upon the handle
        ServiceDetailsExt sde = createServiceDetails(dsi);

        return new ProgramMapTableImpl2((ProgramMapTableHandleImpl) pmtHandle, sde, comp);
    }

    public void addPATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // TODO Auto-generated method stub

    }

    public void removePATChangeListener(TableChangeListener listener, CallerContext context)
    {
        // TODO Auto-generated method stub

    }

    public ProgramAssociationTableHandle getProgramAssociationTableByID(int frequency, int modulationFormat, int tsID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new RuntimeException("Method not implemented");
    }

    public ProgramAssociationTableHandle getProgramAssociationTableBySourceID(int sourceID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new RuntimeException("Method not implemented");
    }

    public ProgramMapTableHandle getProgramMapTableByProgramNumber(int frequency, int mode, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException
    {
        throw new RuntimeException("Method not implemented");
    }

    public ProgramMapTableHandle getProgramMapTableBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        Iterator iter = this.serviceDetailsList.iterator();
        while (iter.hasNext())
        {
            ServiceDetailsImpl dsi = (ServiceDetailsImpl) iter.next();
            if (dsi.getSourceID() == sourceID)
            {
                return new ProgramMapTableHandleImpl(((SIHandleImpl) dsi.getServiceDetailsHandle()).handle);
            }
        }
        throw new SIRequestInvalidException("the sourceid was not in the supported range.");
    }

    public void addPMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        pmtChangeListeners.addElement(listener);

    }

    public void removePMTChangeListener(TableChangeListener listener, CallerContext context)
    {
        pmtChangeListeners.removeElement(listener);
    }

    public Vector cannedGetPMTChangeListeners()
    {
        return pmtChangeListeners;
    }

    public void removeServiceByHandle(ServiceHandle serviceHandle)
    {
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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    public int getTsIDForTransportStreamHandle(TransportStreamHandle tsHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isOOBAcquired()
    {
        return true;
    }

    public void waitForNITSVCT()
    {
        
    }

    public void waitForOOB() {
        // TODO Auto-generated method stub
        
    }

    public boolean isNITSVCTAcquired() {
        // TODO Auto-generated method stub
        return true;
    }

    public void waitForNTT() {
        // TODO Auto-generated method stub
        
    }
}
