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

package org.cablelabs.impl.ocap.hn.upnp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.ocap.hn.NetworkInterfaceImpl;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.channel.ChannelRequestInterceptor;
import org.cablelabs.impl.ocap.hn.upnp.bms.BasicManagementService;
import org.cablelabs.impl.ocap.hn.upnp.bms.BasicManagementServiceSCPD;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryServiceSCPD;
import org.cablelabs.impl.ocap.hn.upnp.cds.HTTPRequestInterceptor;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.cm.CMSProtocolInfo;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionManagerService;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionManagerServiceSCPD;
import org.cablelabs.impl.ocap.hn.upnp.em.EnergyManagementService;
import org.cablelabs.impl.ocap.hn.upnp.em.EnergyManagementServiceSCPD;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedDeviceImpl;
import org.cablelabs.impl.ocap.hn.upnp.vpop.VPOPService;
import org.cablelabs.impl.ocap.hn.upnp.vpop.VPOPServiceSCPD;
import org.cablelabs.impl.service.MediaServerManagerImpl;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cybergarage.upnp.UPnP;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBObject;
import org.ocap.hardware.Host;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.service.MediaServerManager;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;
import org.ocap.storage.ExtendedFileAccessPermissions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * This is the main class for implementation of the HNP specification.  
 * It is a composite class that utilizes the UPnP Diagnostics API to create a
 * UPnP network device, and contains implementation classes for each of the
 * services required by the HNP specification.
 * 
 * It uses static construction to make sure it is available to the stack and 
 * applications at the earliest possible time, and is a singleton providing
 * easy access to it's services where required in the stack. 
 * 
 */
public class MediaServer
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(MediaServer.class);

    private static final String OCAP_PROFILE_PREFIX = "OCAP ";
        
    private static final String SP = " ";
    private static final String CRLF = "\r\n";

    public static final String HTTPPREFIX = "HTTP/";

    /**
     * List all the supported HTTP version
     */
    public static final String[] SUPPORTED_HTTP_VERSIONS = { "0.9", "1.0", "1.1" };

    public static final String HIGHEST_SUPPORTED_HTTP_VER = HTTPPREFIX + "1.1";
    
    /** The GMT time zone. */
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");    
    
    /** The start of time in Java-land. */
    private static final GregorianCalendar EPOCH_JAVA= gregorianCalendar(1970, GregorianCalendar.JANUARY, 1);

    /** The start of time in DCE-UUID-land. */
    private static final GregorianCalendar EPOCH_DCE = gregorianCalendar(1582, GregorianCalendar.OCTOBER, 15);

    /** The number of milliseconds from 1582 October 15 to 1970 January 1. */
    private static final long EPOCH_OFFSET = EPOCH_JAVA.getTimeInMillis() - EPOCH_DCE.getTimeInMillis();
    
    private final static int SO_TIMEOUT_MS = 30000;
    
    private static String m_serverIdStr;

    private static boolean m_needsLinkProtection = true;

    private Map m_rootDevice = new HashMap();
    private Map m_mediaDevice = new HashMap();

    private static final MediaServer s_mediaServer = new MediaServer();
    private List m_configuredAddressList = null;
    private InetAddress m_localStreamingAddress = null;
    private int m_localStreamingPort = -1;

    private String m_linkLocalInterface = "";
    
    private final CallerContextManager m_ccm;
    
    // The composite classes for service implementation
    private BasicManagementService m_bms;
    private EnergyManagementService m_ems;
    private ConnectionManagerService m_cms;
    private ContentDirectoryService m_cds;
    private UPnPActionHandler m_srs;
    private VPOPService m_vpop;
    private boolean m_initialized = false;
    private static final List m_interceptors = new ArrayList();    
    private final List m_serverSockets = new ArrayList();

    private static long s_deviceStartTime;
    
    // Constants used to build names of files that store the UUIDs
    private static final String DEFAULT_DIR = "/syscwd";
    private static final String DEFAULT_HN_DIR = "/hn";
    private static final String DEFAULT_DEVICE_UUID_FILENAME = "device_uuid.txt";
    private static final String DEFAULT_DMS_UUID_FILENAME = "dmm_uuid.txt";
    private static final String HNDIR_PROP = "OCAP.persistent.hn";
    private static final String BASEDIR_PROP = "OCAP.persistent.root";
    private static final String DEFAULT_ICONS_PNG_DIR = "/sys/icons/png";
    private static final String DEFAULT_ICONS_JPEG_DIR = "/sys/icons/jpeg";
    private static final String ICONS_JPEG_SMALL = "/JPEG_SM_ICO.jpg";
    private static final String ICONS_JPEG_LARGE = "/JPEG_LRG_ICO.jpg";
    private static final String ICONS_PNG_SMALL = "/PNG_SM_ICO.png";
    private static final String ICONS_PNG_LARGE = "/PNG_LRG_ICO.png";
    
    public static final String HOST_PLACEHOLDER = "__host__";
    public static final String HOST_PORT_PLACEHOLDER = "__host_port__";
    
    public static final String MEDIA_PORT_PROP = "OCAP.hn.server.media.port";
    public static final String SINGLE_UDN_PROP = "OCAP.hn.server.disableMultiUDN";

    public static final String ICON_REQUEST_URI_PREFIX = ContentDirectoryService.CONTENT_EXPORT_URI_PATH_PREFIX + "/icon?id=";
   
    private MediaServer()
    {
        if ("true".equalsIgnoreCase(MPEEnv.getEnv("OCAP.hn.server.disableDtcpIp")))
        {
            if(log.isInfoEnabled())
            {
                log.info("Disable dtcpip value is true, needsLinkProtection is false.");
            }
            m_needsLinkProtection = false;
        }
        else
        {
            if(log.isInfoEnabled())
            {
                log.info("Disable dtcpip value is false or blank, needsLinkProtection is true.");
            }
        }
    	m_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    }
    
    public static MediaServer getInstance()
    {
        return s_mediaServer;
    }

    public boolean isInitialized()
    {
        return m_initialized;
    }    
    
    public synchronized void startMediaServer()
    {
        if(m_initialized)
        {
            if(log.isInfoEnabled())
            {
                log.info("Media Server is already started up.");
            }
            return;
        }
        
        if(log.isInfoEnabled())
        {
            log.info("Media Server is starting up.");
        }
        
        // register interceptor that is always available
        m_interceptors.add(new ChannelRequestInterceptor());            
        
        UPnPDeviceManager devMgr = UPnPDeviceManager.getInstance();
        m_cds = new ContentDirectoryService();
        m_vpop = new VPOPService();
        m_bms = new BasicManagementService();
        m_ems = null;
        m_cms = new ConnectionManagerService();
        
        // create ScheduledRecordingService thru reflection for separable builds
        m_srs = null; 
        
        Method registerMediaServerMethod = null;
        Method initializeStateVariablesMethod = null;
        try
        {
            Class srs = Class.forName("org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService");
            Constructor ctor = srs.getConstructor(new Class[] { ContentDirectoryService.class });
            
            registerMediaServerMethod = srs.getDeclaredMethod(
                    "registerMediaServer", new Class [] { UPnPManagedDevice.class });
            
            initializeStateVariablesMethod = srs.getDeclaredMethod(
                    "initializeStateVariables", new Class [] { });
            
            m_srs = (UPnPActionHandler) ctor.newInstance(new Object[] { m_cds });
        }
        catch (Exception e)
        {
            // default for HN Only build
        }

        // Get the list of configured addresses
        final List inetList = getConfiguredInetAddresses(true);
        
        if(inetList.size() == 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("No InetAddresses were assigned to this MediaServer.");
            }
        }
        
        final String vpopEnvName = "OCAP.hn.server.vpop.enabled";
        final String vpopEnvValue= MPEEnv.getEnv(vpopEnvName);
        final boolean vpopForceEnable = "true".equals(vpopEnvValue);       
        
        // Get all addresses for interfaces
        for(Iterator i = inetList.iterator(); i.hasNext(); )
        {
            InetAddress inet = (InetAddress)i.next();
                
            try
            {
                // Setup root device, sub-device and required services
                m_rootDevice.put(inet, devMgr.createDevice(null, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(getRootDescription(inet))),
                        new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 48, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_JPEG_DIR+ICONS_JPEG_SMALL)) ,
                        new UPnPManagedDeviceIcon("image/jpeg", 120, 120, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_JPEG_DIR+ICONS_JPEG_LARGE)),
                        new UPnPManagedDeviceIcon("image/png", 48, 48, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_PNG_DIR+ICONS_PNG_SMALL)) ,
                        new UPnPManagedDeviceIcon("image/png", 120, 120, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_PNG_DIR+ICONS_PNG_LARGE)) 
                }));
                    
                // set the start time of device
                s_deviceStartTime = System.currentTimeMillis();
                    
                m_bms.registerService(((UPnPManagedDevice)m_rootDevice.get(inet)).createService(BasicManagementService.SERVICE_TYPE, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(BasicManagementServiceSCPD.getSCPD())), 
                        null));

                if("true".equalsIgnoreCase(MPEEnv.getEnv("OCAP.hn.server.energyManagement.enabled")))
                {
                    if (m_ems == null)
                    {
                        m_ems = new EnergyManagementService();
                    }
                    m_ems.registerService(((UPnPManagedDevice)m_rootDevice.get(inet)).createService(EnergyManagementService.SERVICE_TYPE,
                            new ByteArrayInputStream(XMLUtil.toByteArray(EnergyManagementServiceSCPD.getSCPD())),
                            null));
                    
                    if(log.isInfoEnabled())
                    {
                        log.info("EnergyManagement is enabled");
                    }
                }
                else
                {
                    if(log.isInfoEnabled())
                    {
                        log.info("EnergyManagement is not enabled");
                    }
                }                    

                m_mediaDevice.put(inet, devMgr.createDevice((UPnPManagedDevice)m_rootDevice.get(inet), 
                            new ByteArrayInputStream(XMLUtil.toByteArray(getMediaDeviceDescription(inet))),
                            new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 48, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_JPEG_DIR+ICONS_JPEG_SMALL)) ,
                                                          new UPnPManagedDeviceIcon("image/jpeg", 120, 120, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_JPEG_DIR+ICONS_JPEG_LARGE)),
                                                          new UPnPManagedDeviceIcon("image/png", 48, 48, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_PNG_DIR+ICONS_PNG_SMALL)) ,
                                                          new UPnPManagedDeviceIcon("image/png", 120, 120, 24, createIcons(DEFAULT_DIR+DEFAULT_ICONS_PNG_DIR+ICONS_PNG_LARGE)) 
                                                        }));

                ((UPnPManagedDeviceImpl)m_rootDevice.get(inet)).setMediaDevice(true);
                    
                m_cds.registerService(((UPnPManagedDevice)m_mediaDevice.get(inet)).createService(ContentDirectoryService.SERVICE_TYPE, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(ContentDirectoryServiceSCPD.getSCPD())), 
                        null));
                    
                m_cms.registerService(((UPnPManagedDevice)m_mediaDevice.get(inet)).createService(ConnectionManagerService.SERVICE_TYPE, 
                        new ByteArrayInputStream(XMLUtil.toByteArray(ConnectionManagerServiceSCPD.getSCPD())),
                        null));
                    
                // If DVR included register service
                if(m_srs != null)
                {
                    registerMediaServerMethod.invoke(m_srs, new Object[] { m_mediaDevice.get(inet) });
                }
                
                if (vpopForceEnable || 
                        ("mib-conditional".equals(vpopEnvValue) && 
                                    vpopEnabledViaMIB()))
                {
                    try
                    {
                        m_vpop.registerService(((UPnPManagedDevice)m_mediaDevice.get(inet)).createService(VPOPService.SERVICE_TYPE, 
                                new ByteArrayInputStream(XMLUtil.toByteArray(VPOPServiceSCPD.getSCPD())), 
                                null));
                    }
                    catch (Exception e)
                    {
                        if(log.isWarnEnabled())
                        {
                            log.warn("Unable to register VPOP service.", e);
                        }                
                    }

                    if(log.isInfoEnabled())
                    {
                        log.info("VPOP service is configured.");
                    }            
                }
                else
                {
                    if(log.isInfoEnabled())
                    {
                        log.info("VPOP service is not configured.");
                    }
                }
                    
                // Add in the CVP-2 RemoteUIServerService
                RemoteUIServer.getInstance().addRemoteUIServer((UPnPManagedDevice)m_rootDevice.get(inet), inet);
                
                // Advertise on all interfaces.  Setting addresses will cause alives to be sent.
                ((UPnPManagedDevice)m_rootDevice.get(inet)).setInetAddresses((InetAddress[]) new InetAddress[]{ inet } );
                
                if(log.isInfoEnabled())
                {
                    log.info("Media Server is available on " + inet.getHostAddress());
                }                
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to create MediaServer ", e);
                }
            }
        }
            
        if(inetList.size() == 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Media Server local address is " + m_localStreamingAddress.getHostAddress());
            }
        }
        
        // Create default read only Efap for Vpop
        ExtendedFileAccessPermissions vpopEfap = new ExtendedFileAccessPermissions(true, false, true, false, true, false,
                                                        null, null);
        
        // Get the ocHnNetConfigViewPrimaryOutputPortOrgID MIB object value. 
        // If this value is non-zero, it is used to update the other_org field 
        // of the VPOP ocap:acessPermissions property to permit write access by 
        // applications whose organization ID match the value of the MIB object.
        int vpopOtherOrgID = getIntMibValue("1.3.6.1.4.1.4491.2.3.2.2.5.2.0", 
                                            MIBDefinition.SNMP_TYPE_UNSIGNED32);
        
        // If MIB value is non-zero, update the other_org field to permit write access by applications 
        // whose organization ID match the value of the MIB object
        if (vpopOtherOrgID != 0)
        {
            vpopEfap.setPermissions(true, false,                        // world read & write
                                    true, false,                        // org read & write
                                    true, false,                        // app read & write
                                    null, new int[]{vpopOtherOrgID});   // other orgs read & write   
            if (log.isInfoEnabled())
            {
                log.info("startMediaServer() - MIB indicates write access for other org id: " +
                        vpopOtherOrgID);
            }                    
        }
 
        if (vpopForceEnable || 
                ("mib-conditional".equals(vpopEnvValue) && 
                        vpopEnabledViaMIB()))
        {
            m_cds.installVPOPContentItem(vpopEfap);
        }
        
        // Initialization state variables and begin eventing
        m_bms.initializeStateVariables();
        m_cds.initializeStateVariables();
        m_cms.initializeStateVariables();
        if (m_ems != null)
        {
            m_ems.initializeStateVariables();
        }
        
        // If DVR included initialize SRS
        if(m_srs != null)
        {
            try
            {
                initializeStateVariablesMethod.invoke(m_srs, new Object[] { });
            }
            catch (Exception e)
            {
                if(log.isErrorEnabled())
                {
                    log.error("Unable to initialize SRS state variables", e);
                }
            }
        }       
    
        // Build CMS Source/Sink Protocol Info strings for a UPnP MediaServer device.
        // Timing considerations require invocation as late as possible to more reliably
        // populate source protocol info string and send events when protocols updated.
        CMSProtocolInfo.getInstance();
        
        m_initialized = true;           
    }
    
    public synchronized boolean setupContentDelivery(InetAddress[] removeAddresses, InetAddress[] addAddresses)
    {
        // Close and remove old sockets.
        for (int i = 0; i < removeAddresses.length; i++)
        {
            int index = m_serverSockets.indexOf(removeAddresses[i]);
            if(index == -1)
            {
                if(log.isInfoEnabled())
                {
                    log.info("InternetAddress " + removeAddresses[i] + " was not set so is not removed.");
                }
                break;
            }
            
            ServerSocket serverSocket = (ServerSocket)m_serverSockets.get(index);
            if(serverSocket != null)
            {
                try
                {
                    serverSocket.close();
                }
                catch (IOException e)
                {
                    if(log.isWarnEnabled())
                    {
                        log.warn("Exception closing server socket while reconfiguring server.", e);
                    }
                }
                finally
                {
                    m_serverSockets.remove(index);
                }
            }
        }
       
        // Setup new sockets.
        final int mediaPort = getConfiguredMediaStreamingPort();
        if(log.isInfoEnabled())
        {
            log.info("Listening on port " + mediaPort);
        }
        
        for(int i = 0; i < addAddresses.length; i++)
        {
            try
            {
                if (m_localStreamingAddress == null)
                {
                    m_localStreamingAddress = addAddresses[i];
                }
                else if (addAddresses[i].isLoopbackAddress())
                {
                    m_localStreamingAddress = addAddresses[i];
                }
                ServerSocket serverSocket = new ServerSocket(mediaPort, 50, addAddresses[i]);
                m_serverSockets.add(serverSocket);
                m_ccm.getSystemContext().runInContextAsync(new SocketListener(serverSocket));
            }
            catch(IOException e)
            {
                if(log.isInfoEnabled())
                {
                    log.info("Unable to bind to port " + mediaPort);
                }
            }
        }

        return true;
    }
    
    /**
     * Returns the configured media streaming port
     */
    public synchronized int getConfiguredMediaStreamingPort()
    {
        if (m_localStreamingPort < 0)
        {
            String portStr = MPEEnv.getEnv(MEDIA_PORT_PROP, "8000");
            int port = -1;
            
            try
            {
                port = Integer.parseInt(portStr);
                m_localStreamingPort = port;
            }
            catch(NumberFormatException ex)
            {
                if(log.isWarnEnabled())
                {
                    log.warn("Invalid port number " + portStr);
                }
                m_localStreamingPort = 8000; // See default above in MPEEnv.getEnv()
            }
        }
        return m_localStreamingPort;
    } // END getConfiguredMediaStreamingPort
    
    /**
     * Returns a list of all the IP addresses configured for the HN MediaServer
     * 
     * @param forceRescan If true, re-enumerate the list of interfaces and addresses.
     *                    This should be used if there's been a change in the available 
     *                    interfaces and/or addresses available on the interface(s)
     * @return A List of InetAddress objects
     */
    public synchronized List getConfiguredInetAddresses(boolean forceRescan)
    {
        if (forceRescan || (m_configuredAddressList == null))
        {
            List inetList = new LinkedList();
            List interfaceList = new LinkedList();
            final String OCAP_HN_MULTICAST_IFACE = "OCAP.hn.multicast.iface";
            final String netAlias = MPEEnv.getEnv(OCAP_HN_MULTICAST_IFACE, "*");
            
            // Get all interfaces to use.
            try 
            {
                if (netAlias.indexOf("*") != -1)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(OCAP_HN_MULTICAST_IFACE + " includes a wildcard - using all available network interfaces for Home Networking.");
                    }
        
                    Enumeration nis = null;
                    nis = NetworkInterface.getNetworkInterfaces();
        
                    if (nis != null)
                    {
                        while (nis.hasMoreElements())
                        {
                            interfaceList.add(nis.nextElement());
                        }
                    }
                }
                else
                {
                    StringTokenizer strtok = new StringTokenizer(netAlias);
                    while (strtok.hasMoreTokens())
                    {
                        NetworkInterface ni = NetworkInterface.getByName(strtok.nextToken());
                        if(ni != null)
                        {
                            interfaceList.add(ni);
                        }
                    }
                }
            }
            catch(SocketException se)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(se);
                }
            }

            // Check to see if OCAP.hn.server.linkLocal.enable is set to true or if the 
            // persistent link local MIB is set,, if so set a link local address on the
            // first interface in the interfaceList.
            final String linkLocalEnableParam = "OCAP.hn.server.linkLocal.enable";
            final String linkLocalEnableValue = MPEEnv.getEnv(linkLocalEnableParam);
            final boolean linkLocalEnabled = "true".equalsIgnoreCase(linkLocalEnableValue) ||
               linkLocalEnabledViaMIB(); 
            if (linkLocalEnabled)
            {
                boolean linkLocalSet = false;
                NetworkInterface ni = (NetworkInterface) interfaceList.get(0);
                String interfaceName = ni.getName();
                if (ni != null)
                {
                    linkLocalSet = HNAPIImpl.nativeSetLinkLocalAddress(interfaceName);
                    m_linkLocalInterface = interfaceName;
                }
                if (!linkLocalSet)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Failed to set link local address on interface " + ni.getName());
                    }
                }
                else
                {
                    // need to replace the 1st element since we changed the IP addresses
                    // on the interface
                    try
                    {
                        NetworkInterface newNI = NetworkInterface.getByName(interfaceName);
                        ((LinkedList) interfaceList).removeFirst();
                        ((LinkedList) interfaceList).addFirst(newNI);
                        List inetAddrs = new ArrayList();
                        Enumeration e = NetworkInterface.getNetworkInterfaces();
                        while(e.hasMoreElements())
                        {
                            NetworkInterface nni = (NetworkInterface)e.nextElement();
                            Enumeration ie = nni.getInetAddresses();
                            while(ie.hasMoreElements())
                            {
                                InetAddress i = (InetAddress)ie.nextElement();
                                inetAddrs.add(i);
                            }
                        }
                        UPnPControlPoint.getInstance().setInetAddresses(
                          (InetAddress[])inetAddrs.toArray(new InetAddress[inetAddrs.size()]));

                    }
                    catch(SocketException se)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(se);
                        }
                    }
                    catch(SecurityException sse)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(sse);
                        }
                    }
                }  
            }
            
            final String addressPrioritization = "OCAP.hn.server.addressAnnouncementPrioritization";
            final String addressPrioritizationVal = MPEEnv.getEnv(addressPrioritization);
            // Consider link-local de-prioritized unless explicitly set otherwise...
            final boolean linkLocalPrioritized = "link-local".equals(addressPrioritizationVal);
            if(log.isInfoEnabled())
            {
                log.info("Link-local addresses are " + (!linkLocalPrioritized ? "DE-" : "") 
                         + "PRIORITIZED");
            }
            // Get all addresses for interfaces
            for(Iterator i = interfaceList.iterator(); i.hasNext(); )
            {
                Enumeration inetEnum = ((NetworkInterface)i.next()).getInetAddresses();
                while(inetEnum.hasMoreElements())
                {
                    InetAddress inet = (InetAddress)inetEnum.nextElement();
                    if (linkLocalPrioritized)
                    { // Link local addresses go to the front
                        inetList.add((inet.isLinkLocalAddress() ? 0 : inetList.size()), inet);
                    }
                    else
                    { // Link local addresses go to the end
                        inetList.add((inet.isLinkLocalAddress() ? inetList.size() : 0), inet);
                    }
                    
                    if (m_localStreamingAddress == null || inet.isLoopbackAddress())
                    {
                        m_localStreamingAddress = inet;
                    }
                }
            }
            m_configuredAddressList = inetList;
        } // END if (forceRescan || (m_configuredAddressList == null))
        
        return m_configuredAddressList ;
    } // END getConfiguredInetAddresses()
    
    /**
     * Returns the HTTP Server ID string which is formulated
     * using ocap system properties.
     * 
     * @return  string used to id server at platform level
     */
    synchronized public static String getServerIdStr()
    {
        if (m_serverIdStr == null)
        {
            // Formulate a string which looks something like 
            // CableLabs RI 1.0/1.1.4 Rel-F
            StringBuffer sb = new StringBuffer(32);
            sb.append(System.getProperty("ocap.software.vendor_id"));
            sb.append(" ");
            sb.append(System.getProperty("ocap.software.model_id"));
            sb.append("/");
            sb.append(System.getProperty("ocap.software.version"));
            m_serverIdStr = sb.toString();
        }
        return m_serverIdStr;
    }

    /**
     * Returns a "placeholder" the Export URL for a client or consumer.  The placeholder
     * is a null inet address string which will be populated when the actual request
     * for content is made depending on incoming address of the request.
     *
     * @param pathAndParameters
     *            is the path and (optional) parameters to be added.  pathAndParameters MUST include any common prefix and must start with a slash
     * @return A string representing the Content Export URL.
     */
    public static String getContentExportURLPlaceholder(String pathAndParameters)
    {
        return (pathAndParameters == null) 
               ? null 
               : "http://" + HOST_PORT_PLACEHOLDER + pathAndParameters;
    }

    public String getContentLocalURLForm(String pathAndParameters)
    {
        if (m_localStreamingAddress == null)
        {
            getConfiguredInetAddresses(false);
        }
        final int port = getConfiguredMediaStreamingPort();
        
        return "http://" + m_localStreamingAddress.getHostAddress() + ':' + port 
               + pathAndParameters;
    }

    public BasicManagementService getBMS()
    {
        return m_bms;
    }

    public ConnectionManagerService getCMS()
    {
        return m_cms;
    }
    
    public ContentDirectoryService getCDS()
    {
        return m_cds;
    }
    
    public UPnPActionHandler getSRS()
    {
        return m_srs;
    }
    
    public VPOPService getVPOP()
    {
        return m_vpop;
    }

    public UPnPManagedDevice getRootDevice()
    {
        return (UPnPManagedDevice)(m_rootDevice.values().toArray())[0];
    }
    
    public Map getRootDevices()
    {
        return m_rootDevice;
    }
    
    private static String getRootDescription(InetAddress inet)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<device>\n");
        sb.append("    <deviceType>urn:schemas-opencable-com:device:OCAP_HOST:1</deviceType>\n");
        sb.append("    <friendlyName>"    + getDeviceName() +    "</friendlyName>\n");
        sb.append("    <manufacturer>OCAP</manufacturer>\n");
        sb.append("    <manufacturerURL>http://www.cablelabs.com</manufacturerURL>\n");
        sb.append("    <modelDescription>OCAP Reference Implementation Device</modelDescription>\n");
        sb.append("    <modelName>"       + getDeviceName() + "</modelName>\n");
        sb.append("    <modelNumber>OCORI-1.2</modelNumber>\n");
        sb.append("    <modelURL>http://www.cablelabs.com</modelURL>\n");
        sb.append("    <serialNumber>OCORI-000.1.2</serialNumber>\n");
        sb.append("    <UDN>" + createUUID(DEFAULT_DEVICE_UUID_FILENAME, inet) + "</UDN>\n");
        sb.append("    <UPC>111111111111</UPC>\n");
        sb.append("    <ocap:X_OCAPHN>OC-DMS-1.0</ocap:X_OCAPHN>\n");
        sb.append("    <ocap:X_MiddlewareProfile>" + middlewareProfile() + "</ocap:X_MiddlewareProfile>\n");
        sb.append("    <ocap:X_MiddlewareVersion>" + middlewareVersion() + "</ocap:X_MiddlewareVersion>\n");
        sb.append("</device>\n");
                
        return sb.toString();
    }    
    
    private static String getMediaDeviceDescription(InetAddress inet)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<device>\n");
        sb.append("    <deviceType>urn:schemas-upnp-org:device:MediaServer:2</deviceType>\n");
        sb.append("    <friendlyName>" + getServerName() + "</friendlyName>\n");
        sb.append("    <manufacturer>OCAP</manufacturer>\n");
        sb.append("    <manufacturerURL>http://www.cablelabs.com</manufacturerURL>\n");
        sb.append("    <modelDescription>Provides content through UPnP ContentDirectory service</modelDescription>\n");
        sb.append("    <modelName>" + getServerName() + "</modelName>\n");
        sb.append("    <modelNumber>OCORI-DMS-1.2</modelNumber>\n");
        sb.append("    <modelURL>http://www.cablelabs.com</modelURL>\n");
        sb.append("    <serialNumber>OCORI-DMS-000.1.2</serialNumber>\n");
        sb.append("    <UDN>" + createUUID(DEFAULT_DMS_UUID_FILENAME, inet) + "</UDN>\n");
        sb.append("    <ocap:X_OCAPHN>OC-DMS-1.0</ocap:X_OCAPHN>\n");
        sb.append("    <ocap:X_MiddlewareProfile>" + middlewareProfile() + "</ocap:X_MiddlewareProfile>\n");
        sb.append("    <ocap:X_MiddlewareVersion>" + middlewareVersion() + "</ocap:X_MiddlewareVersion>\n");
        sb.append("    <dlna:X_DLNADOC xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">DMS-1.50</dlna:X_DLNADOC>\n");
        sb.append("</device>\n");
                
        return sb.toString();
    }   
    
    private static String getDeviceName()
    {
        // return DescriptionProperties.getProperty("device.name");
        return "OCAP Device";        
    }
    
    public static final String getServerName()
    {
        // return DescriptionProperties.getProperty("server.name");
        String serverModelNamePROP = "OCAP.hn.server.name";
        String friendlyName = MPEEnv.getEnv(serverModelNamePROP);
        if (friendlyName == null) {
           friendlyName = "OCAP Media Server";
        }
        if (log.isInfoEnabled())
        {
            log.info(serverModelNamePROP + " = " + friendlyName);
        }
        return friendlyName;
    }
    
    public static boolean getLinkProtectionFlag()
    {
        return m_needsLinkProtection;
    }

    public static long getDeviceStartTime()
    {
        return s_deviceStartTime;
    } 

    // We have to make an assumption here that the same port was available on all interfaces.
    // We will take the first socket and ask for it's port.
    public int getHttpServerPort()
    {
        if(m_serverSockets.size() > 0)
        {
            ServerSocket serverSocket = (ServerSocket)m_serverSockets.get(0);
            if(serverSocket != null)
            {
                return serverSocket != null ? serverSocket.getLocalPort() : -1;
            }
        }
        
        return -1;
    }
    
    // Add additional interceptors for the MediaServer, VPOP when configured would be an example.
    public void addHTTPRequestInterceptor(HTTPRequestInterceptor interceptor)
    {
        synchronized(m_interceptors)
        {
            m_interceptors.add(interceptor);
        }
    }

    // Returns the interface link local address is assigned on
    public String getLinkLocalInterface()
    {
        return m_linkLocalInterface;
    }
    
    private static final String middlewareProfile()
    {
        String ocapProfile = System.getProperty("ocap.profile");

        assert ocapProfile.startsWith(OCAP_PROFILE_PREFIX);

        return ocapProfile.substring(OCAP_PROFILE_PREFIX.length());
    }

    private static final String middlewareVersion()
    {
        return System.getProperty("ocap.software.version");
    }
    // //////////////////////////////////////////////
    // UUID
    // //////////////////////////////////////////////

    /**
     * Retrieves an already existing UUID from file in persistent storage or creates
     * a UUID and writes it to a file in persistent storage.
     *
     * @return  UUID string which is stored in persistent file
     */
    protected static final String createUUID(String name, InetAddress inet)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createUUID() called");
        }

        String result = null;
        
        // Seed with Mac Address
        UPnP.setMacAddress(Host.getInstance().getReverseChannelMAC());

        String uuid = null;

        String filename = getUUIDFilename(name);
        File f = new File(filename);
        if (f.exists())
        {
            // Read UUID from file
            try
            {
                FileInputStream fis = new FileInputStream(f);
                int size = fis.available();
                byte buffer[] = new byte[size];
                if (fis.read(buffer) != size)
                {
                    // Log error unable to read file contents
                    if (log.isErrorEnabled())
                    {
                        log.error("Unable to read UUID " + size + " bytes from file " + filename);
                    }
                }
                else
                {
                    uuid = new String(buffer, 0, size);
                    if (log.isInfoEnabled())
                    {
                        log.info("UUID seed = " + uuid);
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                // Log error because just asked if file existed
                if (log.isErrorEnabled())
                {
                    log.error("Unable to read UUID file " + filename +
                    " after checking if it exists: ",e);
                }
            }
            catch (IOException e)
            {
                // Log error reporting problems accessing file
                if (log.isErrorEnabled())
                {
                    log.error("Unable to read UUID file " + filename +
                    " problems with i/o: ",e);
                }
            }
        }

        if (uuid == null)
        {
            uuid = UPnP.createUUID(); 

            // Save this UUID to file
            try
            {
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(uuid.getBytes());
                fos.close();
                if (log.isInfoEnabled())
                {
                    log.info("Created & saved UUID seed " + uuid + " to file " + filename);
                }
            }
            catch (FileNotFoundException e)
            {
                // Log error reporting that problems were encountered when trying to write to file
                if (log.isErrorEnabled())
                {
                    log.error("Unable to write UUID seed =" + uuid + " to file " + filename,e);
                }
            }
            catch (IOException e)
            {
                // Log error reporting that i/o exception occurred
                if (log.isErrorEnabled())
                {
                    log.error("I/O problems writing UUID seed = " + uuid + " to file " + filename,e);
                }
            }
        }
        
        if ("true".equalsIgnoreCase(MPEEnv.getEnv(SINGLE_UDN_PROP)))
        {
            if(log.isInfoEnabled())
            {
                log.info("Multiple UDN is disabled, returning uuid " + uuid + " for all IP addresses.");
            }
            return uuid;
        }
        
        
        // Combine device UUID with IP address to create a UUID per IP.
        // TODO : this strategy is violation of DLNA Guideline 7.3.2.26.2.
        byte[] uuidBytes = uuid.getBytes();
        byte[] ipBytes = inet.getAddress();
        byte[] data = new byte[uuidBytes.length + ipBytes.length];
        
        System.arraycopy(uuidBytes, 0, data, 0, uuidBytes.length); 
        System.arraycopy(ipBytes, 0, data, uuidBytes.length, ipBytes.length);         
        
        // Hash the combined data to generate a new UUID for each IP address
        String hash = Utils.hashToHex(data, "MD5");
        
        // Unable to hash will return persisted UDN.
        if(hash != null && hash.length() == 32)
        {
            // Format UUID
            StringBuffer sb = new StringBuffer("uuid:");
            sb.append(hash.substring(0, 8));
            sb.append("-");
            sb.append(hash.substring(8, 12));
            sb.append("-");
            sb.append(hash.substring(12, 16));
            sb.append("-");
            sb.append(hash.substring(16, 20));
            sb.append("-");
            sb.append(hash.substring(20, 32));
            
            result = sb.toString();
        }
        else
        {
            if(log.isWarnEnabled())
            {
                log.warn("Due to hashing issue, unable to generate multiple UDNs.");
            }        
            result = uuid;
        }
        
        if(log.isInfoEnabled())
        {
            log.info("UUID = " + result);
        }        
        
        return result;
     }

    /**
     * Builds a String that contains the path and filename of the default
     * UUID file.
     *
     * @returns The path and filename of the default UUID file.
     */
    private static String getUUIDFilename(String name)
    {
        String filename;
        String hnRoot;
        String pRoot;

        // See if there is a specific HN persistent root defined
        hnRoot = MPEEnv.getEnv(HNDIR_PROP);

        // If no specific HN persistent root is defined, get default persistent root defined in base
        if (hnRoot == null)
        {
            pRoot = MPEEnv.getEnv(BASEDIR_PROP, DEFAULT_DIR);
            hnRoot = pRoot + DEFAULT_HN_DIR;
        }

        // Create the directory if it doesn't exist
        File dir = new File(hnRoot);
        if (!dir.exists())
        {
            if (log.isInfoEnabled())
            {
                log.info("Description.getUUIDFilename() - creating dir for UUID file " + name +
                ", hn root dir: " + hnRoot);
            }
            dir.mkdir();
        }
        filename = hnRoot + "/" + name;

        return filename;
    }
    
    /**
     * Compute a <code>GregorianCalendar</code> that represents midnight on a particular
     * year/month/day GMT.
     *
     * @param year  The year.
     * @param month The month.
     * @param day   The day.
     *
     * @return      The <code>GregorianCalendar</code> representing midnight on the
     *              particular year/month/day GMT.
     */
    private static final GregorianCalendar gregorianCalendar(int year, int month, int day)
    {
        GregorianCalendar result = new GregorianCalendar(GMT);

        result.clear();
        result.set(year, month, day);

        return result;
    }    
    
    /**
     * Strip any characters that are not hexadecimal digits out of a <code>String</code>.
     *
     * @param s The <code>String</code>.
     *
     * @return  The <code>String</code> with any characters that are not hexadecimal digits
     *          stripped out.
     */
    private static final String nonHexStripped(String s)
    {
        char[] ca = s.toCharArray();

        int nc = 0;

        for (int i = 0, n = ca.length; i < n; ++ i)
        {
            char c = ca[i];

            if (! (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F'))
            {
                ++ nc;
            }
        }

        if (nc == 0)
        {
            return s;
        }

        char[] cb = new char[ca.length - nc];

        for (int i = 0, n = ca.length, j = 0; i < n; ++ i)
        {
            char c = ca[i];

            if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')
            {
                cb[j] = c;
                ++ j;
            }
        }

        return new String(cb);
    }

    /**
     * Compute the hex representation of a number, with leading zeroes added as
     * necessary to pad it to a certain length.
     *
     * @param number The number; must fit in an unsigned 32-bit integer: that is,
     *               must be between 0 and 2^32 - 1 inclusive.
     * @param length The length; must not be smaller than needed to represent the
     *               number.
     *
     * @return       The hex representation of the specified number, with leading
     *               zeroes added as necessary to pad it to the specified length.
     */
    private static final String toHexString(long number, int length)
    {
        if (number < 0L || number > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException();
        }

        String s = Long.toHexString(number);
        int n = s.length();

        if (length < n)
        {
            throw new IllegalArgumentException();
        }

        if (length == n)
        {
            return s;
        }

        StringBuffer sb = new StringBuffer();

        char[] ca = new char[length - n];
        Arrays.fill(ca, '0');
        sb.append(ca)
          .append(s);

        return sb.toString();
    }

    public static String substitute(String string, String token, String value)
    {
        String result = string;
        int startToken = result.indexOf(token);
        while (startToken > -1)
        {
            String prefix = result.substring(0, startToken);
            String suffix = result.substring(startToken + token.length(), result.length());
            result = prefix + value + suffix;
            startToken = result.indexOf(token);
        }
        return result;
    }

    // //////////////////////////////////////////////
    // ICONS
    // //////////////////////////////////////////////
    private static byte[] createIcons(String fileName)
    {
        File deviceIcon = new File(fileName);
        if (deviceIcon.exists())
        {
            try
            {
                FileInputStream fis = new FileInputStream(deviceIcon);
                int size = fis.available();
                byte buffer[] = new byte[size];
                if (fis.read(buffer) != size)
                {
                    // Log error unable to read file contents
                    if (log.isErrorEnabled())
                    {
                        log.error("MediaServer.createIcons() - unable to read " + size + " bytes from file " + fileName);
                    }
                    return null;
                }
                return buffer;
            }
            catch (FileNotFoundException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("MediaServer.createIcons() - unable to read file " + fileName);
                }
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("MediaServer.createIcons() - IOException while reading file " + fileName);
                }
            }
        }
        return null;
    }
    
    /**
     * Get the ocHnNetConfigViewPrimaryOutputPort MIB object value.
     * If this value is true, the VPOP feature is supported.  
     *  
     * @return true if the VPOP feature is to be supported, false otherwise
     */
    private boolean vpopEnabledViaMIB()
    {
        boolean enabled = false;
        final String vpopOID = "1.3.6.1.4.1.4491.2.3.2.2.5.1.0";
        
        int val = getIntMibValue(vpopOID, MIBDefinition.SNMP_TYPE_INTEGER);
        if (val == 1)
        {
            enabled = true;
        }
        if (log.isDebugEnabled())
        {
            log.debug("vpopEnabledViaMIB: returning: " + enabled + ", val = " + val);
        }
        return enabled;
    }

    /**
     * Get the ocHnNetConfigPersistentLinkLocalAddres MIB object value.
     * If this value is true, the link local address is used to advertise DMS.  
     *  
     * @return true if the link local address is to be supported, false otherwise
     */
    private boolean linkLocalEnabledViaMIB()
    {
        boolean enabled = false;
        final String linkLocalOID = "1.3.6.1.4.1.4491.2.3.2.2.5.3.0";
        
        int val = getIntMibValue(linkLocalOID, MIBDefinition.SNMP_TYPE_INTEGER);
        if (val == 1)
        {
            enabled = true;
        }
        if (log.isDebugEnabled())
        {
            log.debug("linkLocalEnabledViaMIB: returning: " + enabled + ", val = " + val);
        }
        return enabled;
    }
    
    /**
     * General purpose utility to read a integer mib value.  It will 
     * read both signed and unsigned values.  It can do both because the unsigned 
     * keyword affects the interpretation, not the representation of a number. 
     * In other words, in cases where we aren't interpreting a value 
     * arithmetically� so-called bitwise operations such as AND, OR, XOR� 
     * it makes essentially no difference whether a value is marked as 
     * "signed" or "unsigned".
     * 
     * @return  value read from MIB, zero if not set or problems reading
     */
    private int getIntMibValue(String oid, int dataType)
    {
        int val = 0;

        final MIBManager mibm = MIBManager.getInstance();

        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("getIntMibValue: null returned from queryMibs");
            }
        }
        else if (mibd.length != 1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("getIntMibValue: queryMibs bad array length: " + mibd.length);
            }
        }
        else if (mibd[0].getDataType() != dataType)
        {
            if (log.isWarnEnabled())
            {
                log.warn("getIntMibValue: queryMibs bad dataType: " + mibd[0].getDataType());
            }
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("getIntMibValue: getMIBObject returned null");
                }
            }
            else
            { 
                // We don't have a handy BER parser, so parse the MIB right here...
                final byte[] mibObjBytes = mibo.getData(); 
                final int tag = (mibObjBytes[0] & 0x000000FF);
                final int len = (mibObjBytes[1] & 0x000000FF);  // get the length in bytes

                // Get each byte of data as specified by len field
                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("getIntMibValue: returning: " + val);
        }
        return val;
    }
    
    // Private encapsulated classes to implement a threaded socket server.
    private class SocketListener implements Runnable
    {
        private final ServerSocket m_serverSocket;
        private boolean running = true;
        
        public SocketListener(ServerSocket socket)
        {
            m_serverSocket = socket;
        }
        
        public void run()
        {
            try
            {
                log.info("Media Server accepting connections on " 
                        + m_serverSocket.getInetAddress() + ":" 
                        + m_serverSocket.getLocalPort());
                
                while(running)
                {
                    Socket socket = m_serverSocket.accept();
                    socket.setSoTimeout(SO_TIMEOUT_MS);
                    
                    if(log.isInfoEnabled())
                    {
                        log.info("Client connection from " + socket.getInetAddress());
                    }
                    m_ccm.getSystemContext().runInContextAsync(new RequestProcessor(socket));                    
                 }
            }
            catch (IOException e)
            {
                if(log.isWarnEnabled())
                {
                    log.warn("Socket IOException occurred. ", e);
                }
            }
        }
        
        public void stop()
        {
            running = false;
        }
    }
    
    // Private encapsulated class used to implement threaded request handling
    private class RequestProcessor implements Runnable
    {
        private final Socket m_socket;
        public RequestProcessor(Socket socket)
        {
            m_socket = socket;
        }
        
        public void run()
        {
            try
            {
                if(m_socket != null && m_socket.isConnected())
                {
                    InputStream is = m_socket.getInputStream();
                
                    BufferedReader input = new BufferedReader(new InputStreamReader(is));
                    boolean keepGoing = true;
                    boolean firstRequest = true;
                
                    while(keepGoing)
                    {
                        String startLine = input.readLine();
                        
                        // Support for older clients that start with CRLF
                        if(firstRequest && (startLine == null || startLine.trim().length() == 0))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("Start line is empty, reading next line to support older clients.");
                            }
                            startLine = input.readLine();
                        }
                        firstRequest = false;
                        
                        // startLine must have data at this point.
                        if(startLine != null && startLine.trim().length() > 0)
                        {
                            HTTPRequest request = new HTTPRequest(startLine.trim());
                
                            while(input.ready())
                            {
                                String line = input.readLine();
                                if(line == null || line.trim().length() == 0)
                                {
                                    // End of headers
                                    break;
                                }
                                request.addHeader(line.trim());
                            }

                            if (!request.isValidHttpVer())
                            {
                                if (log.isInfoEnabled()) 
                                {
                                    log.info("Invalid request received: " + request + " - HTTP version not supported");
                                }
                                ContentRequest cr = new ContentRequest(m_socket, ActionStatus.HTTP_VERSION_NOT_SUPPORTED);
                                cr.sendHttpResponse();
                                keepGoing = false;
                            }
                            else if (!request.isHead() && !request.isGet())
                            {
                            	if (log.isInfoEnabled())
                                {
                                    log.info("Unsupported request: " + request + " - HTTP Not Implemented");
                                }
                                ContentRequest cr = new ContentRequest(m_socket, ActionStatus.HTTP_NOT_IMPLEMENTED);
                                cr.sendHttpResponse();
                                keepGoing = false;
                            }
                            else if (!processRequest(request, m_socket))
                            {
                                ContentRequest cr = new ContentRequest(m_socket, request, null, null, request.getRequestURI().toString());
                                cr.setStatus(ActionStatus.HTTP_BAD_REQUEST);
                                cr.sendHttpResponse();
                            }
                                
                            if(!request.isHttp11() || request.isConnectionClose())
                            {
                                keepGoing = false;
                                if(log.isDebugEnabled())
                                {
                                    log.debug("Connection closing as requested. " + m_socket);
                                }
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Keeping connection alive. " + m_socket);
                                }
                            }
                        }
                        else
                        {
                            // End of stream, assume socket was prematurely closed.
                            // So don't bother replying with HTTP error.
                            if(startLine == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("Unexpected end of stream. " + m_socket);
                                }
                            }
                            else
                            {
                                // Whitespaced start line is a bad request.
                                if (log.isInfoEnabled())
                                {
                                    log.info("Invalid start line received: " + startLine);
                                }
                                ContentRequest cr = new ContentRequest(m_socket, ActionStatus.HTTP_BAD_REQUEST);
                                cr.sendHttpResponse();
                            }
                            
                            keepGoing = false;
                        }
                    }
                }
            }
            catch (IOException e)
            {
                if(log.isInfoEnabled())
                {
                    log.info("Socket unexpected close. " + m_socket, e);
                }
            }
            finally
            {
                try
                {
                    if(m_socket != null && m_socket.isConnected())
                    {
                        m_socket.close();
                    }
                }
                catch (IOException e)
                {
                    if(log.isWarnEnabled())
                    {
                        log.warn("Unable to close out socket");
                    }
                }
            }
        }
    }
    
    private boolean processRequest(HTTPRequest httpRequest, Socket socket)
    {
        final ContentContainerImpl.ResBlockReference resBlock;
        URL requestURL = null;
        URL resolvedURL = null;
        
        ContentEntry contentEntry = null;
        
        if (httpRequest.getHeader("host") == null && httpRequest.isHttp11())
        {
            if (log.isInfoEnabled())
            {
                log.info("Http Request did not contain host header.");
            }
            return false;
        }
        
        try
        {
            if(httpRequest.getRequestURI() != null)
            {
                if(httpRequest.getRequestURI().isAbsolute())
                {
                    requestURL  = httpRequest.getRequestURI().toURL();
                    if (log.isInfoEnabled())
                    {
                        log.info("Http request is absolute, using URL: " + requestURL);
                    }
                }
                else
                {
                    requestURL = new URL("http://" + httpRequest.getHeader("host") + httpRequest.getRequestURI().toString());
                    if (log.isInfoEnabled())
                    {
                        log.info("Http request is NOT absolute, using URL: " + requestURL);
                    }
                }
            }
        }
        catch (MalformedURLException mue)
        {
            if (log.isInfoEnabled())
            {
                log.info("Cannot parse URI - ignoring request for " 
                         + httpRequest.getRequestURI());
            }
            return false;
        }

        // Create a NI who's getInetAddress() returns the socket the request was received on
        final org.ocap.hn.NetworkInterface networkInterface 
                = new NetworkInterfaceImpl(socket.getLocalAddress());
        
        final MediaServerManagerImpl msmi = (MediaServerManagerImpl)
                                            (MediaServerManager.getInstance());

        resolvedURL = msmi.invokeHTTPRequestResolutionHandler(socket.getInetAddress(), 
                                                               requestURL, 
                                                               httpRequest.getRequest(), 
                                                               networkInterface);
        
        if (resolvedURL == null)
        {
            resolvedURL = requestURL;
            if (log.isInfoEnabled())
            {
                log.info( "HTTPRequestResolutionHandler returned null, using request URL: " + 
                          resolvedURL.toExternalForm() + " for request " + httpRequest.getRequestURI());
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info( "HTTPRequestResolutionHandler returned " + 
                          resolvedURL.toExternalForm() + " for request " + httpRequest.getRequestURI());
            }
        }
        
        try
        {
            // Do this ContentEntry lookup once to avoid doing this in every interceptor
            if(resolvedURL == null)
            {
                return false;
            }
            
            resBlock = m_cds.getRootContainer().getEntryByURL(resolvedURL);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Ignoring request for " + resolvedURL, e);
            }
            return false;
        }
        
        final URL effectiveURL;

        try
        {
            if(resBlock == null)
            {
                // If this block is executed then it represents device icon is being requested.
                // Send the actual URL received and content entry will be null to be passed to intercept method
                if ((httpRequest.getRequestURI() != null) && (httpRequest.getRequestURI().isAbsolute()))
                {
                    effectiveURL = httpRequest.getRequestURI().toURL();
                }
                else
                {
                    effectiveURL = resolvedURL;
                }
            }
            else
            {
                // Else populate the effectiveURL and the conntent entry
                effectiveURL = new URL(resBlock.getResValue());
                contentEntry = resBlock.getContentEntry();
            }
        }
        catch (MalformedURLException mue)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Error parsing RES URL", mue);
            }
            return false;
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Problem with requested URI: " +  httpRequest.getRequestURI(), e);
            }
            return false;
        }

        // Check to see if there is active session and add scid if needed 
        httpRequest = checkForActiveSession(socket, httpRequest, requestURL);

        final List interceptorCopy;
        synchronized(m_interceptors)
        {
            interceptorCopy = new ArrayList(m_interceptors);
        }

        for (Iterator iter = interceptorCopy.iterator(); iter.hasNext();)
        {
            final HTTPRequestInterceptor interceptor = (HTTPRequestInterceptor)iter.next();

            try
            {
                if (interceptor.intercept(socket, httpRequest, contentEntry, 
                                           requestURL, effectiveURL ))
                {
                    return true;
                }
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn( "Exception delegating intercept to " 
                              + interceptor + " - continuing", e );
                }
            }
        }
        
        return false; // No one wanted it, so it wasn't handled        
    }

    private HTTPRequest checkForActiveSession(Socket socket, HTTPRequest request,
        URL requestURL)
    {

        // only check GETs
        if (!request.isGet())
        {
            return request;
        }

        String scid = request.getHeader("scid.dlna.org");
        if (scid != null)
        {
            // already has scid just continue
            if (log.isDebugEnabled())
            {
               log.debug("request has scid header " + scid);
            }
            return request;
        }

        HNServerSessionManager serverSessionManager =
            HNServerSessionManager.getInstance();

        String sessionID =
            socket.getInetAddress().getHostAddress()
            + requestURL.toString();
        Integer connectionID = serverSessionManager.getActiveSessionConnID(sessionID);

        if (connectionID != null)
        {
            // found save scid on socket but none in request add
            // to look like ocap device
            if (log.isDebugEnabled())
            {
               log.debug("adding scid header to request with value " + connectionID);
            }
            String newHeader = "scid.dlna.org: " + connectionID.toString();
            request.addHeader(newHeader);
        }
        else
        {
            if (log.isDebugEnabled())
            {
               log.debug("initial connection for request " + request);
            }
        }

        return request;

    }
    
    // Static nested class, cohesive to containing class as the only
    // producer of instances.
    static public class HTTPRequest
    {
        private final Map m_headers = new HashMap();
        private final List m_headersRaw = new ArrayList();
        private final String m_startLine;
        
        private final boolean m_get;
        private final boolean m_head;

        // Initialize the minor and major version to 0.0 and use this default to
        // identify the negative case.
        private String m_req_httpVer;
        
        private final URI m_requestURI;
        
        private HTTPRequest(String startLine)
        {
            if(startLine != null && startLine.trim().length() > 0)
            {
                m_startLine = startLine;
                m_get = m_startLine.startsWith("GET");
                m_head = m_startLine.startsWith("HEAD");
            
                int startpos = m_startLine.indexOf(SP);
                int endpos = m_startLine.indexOf(SP, startpos+1);
                if(endpos == -1)
                {
                    // No HTTP version specified in HTTP 0.9
                    m_req_httpVer = "0.9";
                    endpos = m_startLine.length();
                }
                
                URI tmp = null;
                try
                {
                    if(startpos > 0 && endpos > 0)
                    {
                        tmp = new URI(m_startLine.substring(startpos+1, endpos));
                        int prefixIndx = m_startLine.indexOf(HTTPPREFIX);
                        if (prefixIndx > -1)
                        {
                            // Extract last 3 characters after HTTP/ in HTTP/1.1
                            // which will be the HTTP version of the request
                            m_req_httpVer = m_startLine.substring(prefixIndx + HTTPPREFIX.length(),
                                    m_startLine.length());
                        }
                    }
                    else
                    {
                        if(log.isInfoEnabled())
                        {
                            log.info("Unable to parse start line: " + m_startLine);
                        }
                    }
                }
                catch (URISyntaxException e)
                {
                    if(log.isWarnEnabled())
                    {
                        log.warn("Malformed Request URI " + m_startLine.substring(startpos+1, endpos), e);
                    }
                }
                finally
                {
                    m_requestURI = tmp;
                }
            }
            else
            {
                m_get = false;
                m_head = false;
                m_startLine = "";
                m_requestURI = null;
            }
        }
        
        public URI getRequestURI()
        {
            return m_requestURI;
        }
        
        public boolean isGet()
        {
            return m_get;
        }
        
        public boolean isHead()
        {
            return m_head;
        }
        
        public boolean isHttp11()
        {
            return m_startLine.endsWith("HTTP/1.1");
        }
        
        public boolean isHttp10()
        {
            return m_startLine.endsWith("HTTP/1.0");
        }
        
        public boolean isConnectionClose()
        {
            return getHeader("connection") != null && 
                    getHeader("connection").equalsIgnoreCase("close");
        }
        
        public boolean isValidHttpVer()
        {
            // Code changes done to check if the HTTP version received in the
            // request is indeed a valid and supported version. If the version
            // is not supported then throw an unsupported error in the response
            // in a generic way.
            if(m_req_httpVer != null)
            {
                for (int i = 0; i < SUPPORTED_HTTP_VERSIONS.length; i++)
                {
                    if (m_req_httpVer.equalsIgnoreCase(SUPPORTED_HTTP_VERSIONS[i]))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        
        public String getHeader(String key)
        {
            // HTTP 1.1 4.2 Message Headers, names are case insensitive
            return (String)m_headers.get(key.toLowerCase());
        }
        
        public String[] getRequest()
        {
            List strings = new ArrayList();
            strings.add(m_startLine);
            strings.addAll(m_headersRaw);
            
            return (String[])strings.toArray(new String[0]);
        }
        
        private void addHeader(String headerStr)
        {
            if(headerStr != null && headerStr.trim().length() > 0)
            {
                int pos = headerStr.indexOf(":");
                if(pos > -1)
                {
                    m_headersRaw.add(headerStr);

                    // HTTP 1.1 4.2 Message Headers, names are case insensitive
                    m_headers.put(headerStr.substring(0, pos).trim().toLowerCase(), 
                            headerStr.substring(pos+1).trim());
                }
            }
        }
        
        public String toString()
        {
            StringBuffer sb = new StringBuffer(m_startLine + CRLF);
            for(Iterator i = m_headers.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry entry = (Map.Entry)i.next();
                sb.append(entry.getKey() + ": " + entry.getValue() + CRLF);
            }
            
            return sb.toString();
        }
    }
}
