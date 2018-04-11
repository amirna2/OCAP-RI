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

package org.cablelabs.impl.manager.xml.nano;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.PropertyPermission;
import java.util.Set;

import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContextPermission;

import org.dvb.application.AppID;
import org.dvb.application.AppsControlPermission;
import org.dvb.media.DripFeedPermission;
import org.dvb.net.rc.RCPermission;
import org.dvb.net.tuning.TunerPermission;
import org.dvb.spi.ProviderPermission;
import org.dvb.user.UserPreferencePermission;
import org.ocap.application.OcapIxcPermission;
import org.ocap.service.ServiceTypePermission;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.RegisteredApiUserPermission;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.xml.PermissionExtension;
import org.cablelabs.impl.security.AppPermissions;
import org.cablelabs.impl.security.PersistentFileCredential;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.storage.LogicalStorageVolumeExt;
import org.cablelabs.impl.storage.StorageProxyImpl;

import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Parses a permission request file.
 * <p>
 * This is a port of the original SAX parser to NanoXML.
 * 
 * @author Aaron Kamienski
 */
class PermissionsRequest extends BasicXMLBuilder
{
    public Object getResult()
    {
        return getPermissions();
    }

    /**
     * Constructs a <code>PermissionsRequest</code> object, set to parse the
     * permission request file specified by the given <code>URL</code>.
     * 
     * @param ocapPerms
     *            if <code>true</code> then allow OCAP-specific permissions
     *            (i.e., parse the OCAP DTD)
     * @param monAppPerms
     *            if <code>true</code> then allow OCAP-specific
     *            <code>MonitorAppPermission</code>s; if <code>true</code> then
     *            <i>ocapPerms</i> must also be <code>true</code>
     * @param extensions
     *            a <code>Set</code> of {@link PermissionExtension#} instances
     *            supported by the current configuration.
     * @param appid
     *            the appid expected in the PRF
     * @param serviceContextID
     *            the service context ID of the calling app
     */
    PermissionsRequest(boolean ocapPerms, boolean monAppPerms, Set extensions, AppID appid, Long serviceContextID)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        this.extensions = extensions;
        this.ocap = ocapPerms;
        this.monApp = ocapPerms && monAppPerms;
        this.appid = appid;
        this.serviceContextID = serviceContextID;
    }

    /**
     * Returns the starting state used when parsing the XML file.
     * 
     * @return the starting state used when parsing the XML file.
     */
    protected State createStartState()
    {
        return new EmptyState()
        {
            public State nextState(String name)
            {
                return "permissionrequestfile".equals(name) ? (new PermReqHandler()) : (EmptyState.INSTANCE);
            }
        };
    }

    /**
     * Inner class represents the initial starting state for parsing the
     * permission request file.
     */
    private class PermReqHandler extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            // Check that the orgid and appid match what we expect
            int oid = (int)(parseLong(attr.getValue("orgid")) & 0xFFFFFFFF);
            int aid = parseInt(attr.getValue("appid"));
            AppID id = new AppID(oid, aid);
            if (!id.equals(appid))
            {
                String msg = "Ignoring PRF because it contained AppID " + id + " and we expected " + appid;
                SystemEventUtil.logEvent(msg);
                throw new IllegalArgumentException(msg);
            }

            permissions = new AppPermissions();
        }

        public State nextState(String str)
        {
            if ("file".equals(str))
                return new FileState();
            /*
             * else if ("capermission".equals(str)) return new CAState();
             */
            else if ("applifecyclecontrol".equals(str))
                return new LifeState();
            else if ("returnchannel".equals(str))
                return new RCState();
            else if ("tuning".equals(str))
                return new TuningState();
            else if ("servicesel".equals(str))
                return new ServiceSelectState();
            else if ("userpreferences".equals(str))
                return new UserPrefState();
            else if ("network".equals(str))
                return new NetworkState();
            else if ("dripfeed".equals(str))
                return new DripFeedState();
            else if ("persistentfilecredential".equals(str))
                return new PersFileCredState();
            else if (ocap && "ocap:servicetypepermission".equals(str))
                return new ServiceTypeState();
            else if (monApp && "ocap:monitorapplication".equals(str))
                return new MonAppState();
            else if (ocap && "ocap:registeredapi.user".equals(str))
                return new RegApiUserState();
            else if (ocap && "ocap:ixc".equals(str))
                return new IXCState();
            else
                return EmptyState.INSTANCE;
        }

        /**
         * Performs operations that cannot be performed until we've gone through
         * the entire PRF.
         * <ul>
         * <li>Ensures that SelectPermision is granted if not explicitly denied.
         * <li>Add <code>ServiceTypePermission</code> if needed.
         * <li>Add <code>OcapIxcPermission</code> if needed
         * </ul>
         */
        public void end()
        {
            if (!selectDenied) permissions.add(new SelectPermission("*", "own"));
            if (!serviceType)
            {
                if (monappServiceManager)
                    permissions.add(new ServiceTypePermission("*", "*"));
                else if (monappService) permissions.add(new ServiceTypePermission("*", "own"));
            }
            if (!ixcPermissionFound)
            {
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                String service = "service-" + serviceContextID.longValue();

                // OCAP 1.0 Section 14.2.2.9.2 and 14.2.2.9.3
                permissions.add(new OcapIxcPermission("/" + service + "/signed/*/*/*", "lookup"));
                permissions.add(new OcapIxcPermission("/" + service + "/" + "signed" + "/"
                        + Integer.toHexString(appid.getOID()) + "/" + Integer.toHexString(appid.getAID()) + "/" + "*",
                        "bind"));
            }
        }
    }

    /**
     * Inner class used to parse the <file/> element.
     */
    private class FileState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "true")))
            {
                // Allow access to persistent storage
                permissions.add(new PersistentStoragePermission());

                // Access to files that are owned
                // Access to files under persistent storage root + their
                // org/appid dir
                String root = MPEEnv.getSystemProperty("dvb.persistent.root");
                if (root.endsWith(File.separator)) root = root.substring(0, root.length() - 1);

                // Allow read access to the persistent root directory
                // ${dvb.persistent.root} read
                // ${dvb.persistent.root}/* read
                permissions.add(new FilePermission(root, "read"));
                permissions.add(new FilePermission(root + File.separator + "*", "read"));

                // Access OID directory
                // ${dvb.persistent.root}/${oid} read,write,delete
                // ${dvb.persistent.root}/${oid}/* read,write,delete
                root = root + File.separator + Integer.toHexString(appid.getOID());
                permissions.add(new FilePermission(root, "read,write,delete"));
                permissions.add(new FilePermission(root + File.separator + "*", "read,write,delete"));

                // Access AID directory
                // ${dvb.persistent.root}/${oid}/${aid} read,write
                // ${dvb.persistent.root}/${oid}/${aid}/- read,write,delete
                root = root + File.separatorChar + Integer.toHexString(appid.getAID());
                permissions.add(new FilePermission(root, "read,write"));
                permissions.add(new FilePermission(root + File.separator + "-", "read,write,delete"));

                // Adding permissions to the application for accessing LSV
                StorageProxy[] proxies = StorageManager.getInstance().getStorageProxies();
                for (int i = 0; i < proxies.length; i++)
                {
                    StorageProxyImpl proxyImpl = (StorageProxyImpl) proxies[i];
                    String rootPath = proxyImpl.getLogicalStorageVolumeRootPath();
                    if (rootPath.endsWith(File.separator)) root = root.substring(0, root.length() - 1);

                    // <device_root>/OCAP_LSV read
                    // <device_root>/OCAP_LSV/* read
                    permissions.add(new FilePermission(rootPath, "read"));
                    permissions.add(new FilePermission(rootPath + File.separator + "*", "read"));

                    // <device_root>/OCAP_LSV/${oid}/* read
                    String orgIdPath = rootPath + File.separator + Integer.toHexString(appid.getOID());
                    permissions.add(new FilePermission(orgIdPath + File.separator + "*", "read"));

                    // <device_root>/OCAP_LSV/${oid}/${aid}/* read
                    String appIdPath = orgIdPath + File.separator + Integer.toHexString(appid.getAID());
                    permissions.add(new FilePermission(appIdPath + File.separator + "*", "read"));

                    LogicalStorageVolume[] vols = proxies[i].getVolumes();
                    for (int j = 0; j < vols.length; j++)
                    {
                        // Only add permissions for normal LSVs, not MSVs
                        // TODO: Why not use instanceof?
                        LogicalStorageVolumeExt lsve = (LogicalStorageVolumeExt) vols[j];
                        if (!lsve.isMediaStorageVolume())
                        {
                            String lsvDir = lsve.getPath();
                            if (lsvDir.endsWith(File.separator)) lsvDir = lsvDir.substring(0, root.length() - 1);

                            // <device_root>/OCAP_LSV/${oid}/${aid}/<lsv_name>
                            // read,write
                            // <device_root>/OCAP_LSV/${oid}/${aid}/<lsv_name>/-
                            // read,write,delete
                            permissions.add(new FilePermission(lsvDir, "read,write"));
                            permissions.add(new FilePermission(lsvDir + File.separator + "-", "read,write,delete"));
                        }
                    }
                }
            }
        }
    }

    /**
     * Inner class used to parse the <capermission/> element.
     */
    /*
     * private class CAState extends EmptyState { public State nextState(String
     * str) { if ("casystemid".equals(str)) { return new CASystemId(); } }
     * 
     * private class CASystemId extends AttribState { public void
     * start(Attribute attr) { boolean entitlementquery =
     * "true".equals(attr.getValue("entitlementquery", "false")); String id =
     * attr.getValue("id"); boolean mmi = "true".equals(attr.getValue("mmi"));
     * boolean messagepassing = "true".equals(attr.getValue("messagepassing",
     * "false")); boolean buy = "true".equals(attr.getValue("buy", "false")); }
     * } }
     */

    /**
     * Inner class used to parse the <applifecyclecontrol/> element.
     */
    private class LifeState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "true"))) permissions.add(new AppsControlPermission(null, null));
        }
    }

    /**
     * Inner class used to parse the <returnchannel/> element.
     */
    private class RCState extends EmptyState
    {
        public State nextState(String str)
        {
            if ("defaultisp".equals(str))
                return new DefaultIspState();
            else if ("phonenumber".equals(str))
                return new PhoneState();
            else
                return EmptyState.INSTANCE;
        }

        private class DefaultIspState extends EmptyState
        {
            public void start()
            {
                permissions.add(new RCPermission("target:default"));
            }
        }

        private class PhoneState extends StringState
        {
            public void end()
            {
                String phoneNumber = buf.toString().trim();
                if (!phoneNumber.endsWith("*"))
                {
                    phoneNumber = phoneNumber + "*";
                }
                permissions.add(new RCPermission("target:" + phoneNumber));
            }
        }
    }

    /**
     * Inner class used to parse the <tuning/> element.
     */
    private class TuningState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "true"))) permissions.add(new TunerPermission("*", "*"));
        }
    }

    /**
     * Inner class used to parse the <servicesel/> element.
     */
    private class ServiceSelectState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "true")))
                permissions.add(new SelectPermission("*", "own"));
            else
                selectDenied = true;
        }
    }

    /**
     * Inner class used to parse the <userpreferences/> element.
     */
    private class UserPrefState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("read", "true"))) permissions.add(new UserPreferencePermission("read"));
            if ("true".equals(attr.getValue("write", "false"))) permissions.add(new UserPreferencePermission("write"));
        }
    }

    /**
     * Inner class used to parse the <network/> and <host/> elements.
     */
    private class NetworkState extends EmptyState
    {
        private boolean hostsPresent = false;

        public State nextState(String str)
        {
            if ("host".equals(str))
                return new HostState();
            else
                return EmptyState.INSTANCE;
        }

        public void end()
        {
            if (!hostsPresent) throw new IllegalArgumentException("one or more <host> are required in <network>");
        }

        /**
         * Inner class used to parse the <host/> elements.
         */
        private class HostState extends AttribStringState
        {
            String action = "*";

            public void start(String name, Attributes attr)
            {
                hostsPresent = true;
                action = attr.getValue("action");
            }

            public void end()
            {
                String host = buf.toString().trim();
                permissions.add(new SocketPermission(host.trim(), action));
            }
        }
    }

    /**
     * Inner class used to parse the <dripfeed/> element.
     */
    private class DripFeedState extends AttribState
    {
        public void start(String name, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "true"))) permissions.add(new DripFeedPermission("*", "*"));
        }
    }

    private static final byte[] TRUE = { (byte) 't', (byte) 'r', (byte) 'u', (byte) 'e' };

    private static final byte[] FALSE = { (byte) 'f', (byte) 'a', (byte) 'l', (byte) 's', (byte) 'e' };

    /**
     * Parses the PersistentFileCredentials.
     */
    private class PersFileCredState extends EmptyState
    {
        private String root = MPEEnv.getSystemProperty("dvb.persistent.root");

        private boolean valid = true;

        private String expirationDateString;

        private int grantor = 0;

        private String signature; // in Base64 as from RFC 2045

        private String certFilename;

        private ByteArrayOutputStream filenameBytes = new ByteArrayOutputStream();

        private PersistentFileCredential collection = new PersistentFileCredential();

        public State nextState(String str)
        {
            if ("grantoridentifier".equals(str))
                return new GrantorState();
            else if ("expirationdate".equals(str))
                return new DateState();
            else if ("filename".equals(str))
                return new FilenameState();
            else if ("signature".equals(str))
                return new SignatureState();
            else if ("certchainfileid".equals(str))
                return new CertState();
            else
                return EmptyState.INSTANCE;
        }

        /**
         * Adds the permissions.
         */
        public void end()
        {
            if (!valid || expirationDateString == null || certFilename == null || grantor == 0 || signature == null
                    || !collection.elements().hasMoreElements())
            {
                return;
            }

            if (!IGNORE_CERTIFICATE)
            {
                // Validate the signature using the public key
                // Get the signature bytes
                byte[] signatureBytes = null;

                // Load the certificate
                try
                {
                    Certificate leafCert = null;
                    Signature s = Signature.getInstance("MD5withRSA");
                    s.initVerify(leafCert.getPublicKey());

                    // Signature is computed over following data:
                    // grantee_identifier.oid (32)
                    // .aid (16)
                    // grantor_identifier oid (32)
                    // expiration_date (mm/dd/yyyy)
                    // filename & actions in order they appear in document
                    // read {true|false}
                    // write {true|false}
                    // filename
                    // Note that date/filename/actions should be ASCII.
                    // We don't check.
                    updateSig(s, appid.getOID()); // 32-bit OID
                    updateSig(s, (short) appid.getAID()); // 16-bit AID
                    updateSig(s, grantor); // 32-bit grantor
                    s.update(expirationDateString.getBytes()); // expiration
                                                               // date (ASCII)
                    s.update(filenameBytes.toByteArray()); // filename (ASCII)

                    // Verify
                    if (!s.verify(signatureBytes))
                    {
                        return;
                    }
                }
                catch (Exception e)
                {
                    return;
                }
            }

            // Add permissions with expiration date included
            permissions.add(collection);
        }

        /**
         * Invoke <code>Signature.update()</code> with the bytes from the given
         * <code>int</code> (in big-endian order).
         * 
         * @param s
         *            signature to update
         * @param value
         *            32-bit integer to add to signature
         * @throws SignatureException
         */
        private void updateSig(Signature s, int value) throws SignatureException
        {
            s.update((byte) (value >> 24));
            s.update((byte) (value >> 16));
            s.update((byte) (value >> 8));
            s.update((byte) (value));
        }

        /**
         * Invoke <code>Signature.update()</code> with the bytes from the given
         * <code>short</code> (in big-endian order).
         * 
         * @param s
         *            signature to update
         * @param value
         *            16-bit integer to add to signature
         * @throws SignatureException
         */
        private void updateSig(Signature s, short value) throws SignatureException
        {
            s.update((byte) (value >> 8));
            s.update((byte) (value));
        }

        /**
         *
         */
        private class GrantorState extends AttribState
        {
            public void start(String name, Attributes attr)
            {
                grantor = parseInt(attr.getValue("id"));
            }
        }

        /**
         * Specifies the start/end duration for the credentials. The date needs
         * to be included in the generated Permissions, we handle this through
         * an expiration date.
         */
        private class DateState extends AttribState
        {
            public void start(String name, Attributes attr)
            {
                // Parse date - dd/MM/yyyy
                try
                {
                    final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    expirationDateString = attr.getValue("date");

                    Date expirationDate = format.parse(expirationDateString);
                    collection.setExpiration(expirationDate);
                }
                catch (java.text.ParseException e)
                {
                    valid = false;
                }
            }
        }

        /**
         * Indicates files that the application is granted access to outside of
         * it's persistent storage.
         */
        private class FilenameState extends AttribStringState
        {
            private boolean read, write;

            public void start(String name, Attributes attr)
            {
                read = "true".equals(attr.getValue("read", "true"));
                write = "true".equals(attr.getValue("write", "true"));
            }

            public void end()
            {
                String filename = buf.toString().trim();

                try
                {
                    // Add to byte array for signature computation
                    filenameBytes.write(read ? TRUE : FALSE);
                    filenameBytes.write(write ? TRUE : FALSE);
                    // Assume that filename is valid ASCII, so UTF-8 will be
                    // same (and no NUL char)
                    filenameBytes.write(filename.getBytes());
                }
                catch (IOException e)
                {
                    valid = false;
                    return;
                }

                // Add permission to collection
                if (read || write)
                {
                    String next = "";
                    String actions = "";
                    if (read)
                    {
                        actions = "read";
                        next = ",";
                    }
                    if (write) actions = actions + next + "write,delete";
                    java.io.File file = new java.io.File(root, filename);
                    collection.add(new FilePermission(file.toString(), actions));
                }
            }
        }

        /**
         *
         */
        private class SignatureState extends StringState
        {
            public void end()
            {
                signature = buf.toString().trim();

                // TODO(AaronK): Must decode signature !!!!!
            }
        }

        /**
         *
         */
        private class CertState extends StringState
        {
            public void end()
            {
                certFilename = "dvb.certificates." + buf.toString().trim();

                // TODO(AaronK): Must load certificate!
            }
        }
    }

    /**
     * Inner class used to parse the <ocap:monitorapplication/> element and all
     * sub-elements.
     */
    private class MonAppState extends AttribState
    {
        public void start(String tagName, Attributes attr)
        {
            String value = attr.getValue("value", "false");
            String name;
            if ("true".equals(value))
            {
                name = attr.getValue("name");
            }
            else if (SUPPORT_I15_MONAPP && "true".equals(attr.getValue("ocap:value", "false")))
            {
                name = attr.getValue("ocap:name");
            }
            else
            {
                // Either value="false" or nothing is specified
                return;
            }

            // allow extensions to register monapp permissions
            // the PermissionExtension is responsible for adding
            // MonAppPermission(name)
            for (Iterator iter = extensions.iterator(); iter.hasNext();)
            {
                // E.g., DVR-specific permissions
                ((PermissionExtension) iter.next()).handleMonAppPermission(name, permissions);
            }

            if ("registrar".equals(name) || "service".equals(name) || "servicemanager".equals(name)
                    || "security".equals(name) || "reboot".equals(name) || "systemevent".equals(name)
                    || "handler.*".equals(name) || "handler.appFilter".equals(name) || "handler.resource".equals(name)
                    || "handler.closedCaptioning".equals(name)
                    || "handler.homenetwork".equals(name) || "filterUserEvents".equals(name)
                    || "handler.eas".equals(name) || "setVideoPort".equals(name) || "podApplication".equals(name)
                    || "signal.*".equals(name) || "signal.configured".equals(name) || "logger.config".equals(name) || "properties".equals(name)
                    || "storage".equals(name) || "registeredapi.manager".equals(name) || "vbifiltering".equals(name)
                    || "codeDownload".equals(name) || "mediaAccess".equals(name)
                    || "environment.selection".equals(name) || "diagnostics".equals(name)
                    || "powerMode".equals(name))
            {
                permissions.add(new MonitorAppPermission(name));
                if ("service".equals(name))
                {
                    // Additional permissions implied by
                    // MonitorAppPermission("service")
                    // See 10.2.2.2.3.3
                    permissions.add(new ServiceContextPermission("access", "*"));
                    permissions.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
                    permissions.add(new ServiceContextPermission("create", "own"));
                    permissions.add(new ServiceContextPermission("destroy", "own"));
                    permissions.add(new ServiceContextPermission("stop", "*"));
                    permissions.add(new SelectPermission("*", "own"));
                    // ServiceTypePermission only included if not requested
                    // explicitly
                    // permissions.add(new ServiceTypePermission("*", "own"));

                    monappService = true;
                }
                else if ("servicemanager".equals(name))
                {
                    // Additional permissions implied by
                    // MonitorAppPermission("servicemanager")
                    // See 10.2.2.2.3.3
                    permissions.add(new ServiceContextPermission("access", "*"));
                    permissions.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
                    permissions.add(new ServiceContextPermission("create", "own"));
                    permissions.add(new ServiceContextPermission("destroy", "own"));
                    permissions.add(new ServiceContextPermission("stop", "*"));
                    permissions.add(new SelectPermission("*", "own"));
                    // ServiceTypePermission only included if not requested
                    // explicitly
                    // permissions.add(new ServiceTypePermission("*", "*"));
                    // See 10.2.2.2.5
                    permissions.add(new ProviderPermission("*", "system"));
                    // See 10.2.2.3
                    permissions.add(new org.dvb.application.AppsControlPermission("*", "*"));

                    monappServiceManager = true;
                }
                else if ("properties".equals(name))
                {
                    // Additional permissions implied by
                    // MonitorAppPermission("properties")
                    // See OCAP 13.3.12.3 / 21.2.1.20
                    permissions.add(new PropertyPermission("ocap.hardware.vendor_id", "read"));
                    permissions.add(new PropertyPermission("ocap.hardware.version_id", "read"));
                    permissions.add(new PropertyPermission("ocap.hardware.createdate", "read"));
                    permissions.add(new PropertyPermission("ocap.hardware.serialnum", "read"));
                    permissions.add(new PropertyPermission("ocap.memory.video", "read"));
                    permissions.add(new PropertyPermission("ocap.memory.total", "read"));
                    permissions.add(new PropertyPermission("ocap.cablecard.identifier", "read"));
                    // ECR OCAP1.0.2-N-08.1216-3
                    // additional platform specific system properties
                    String apsspStr[] = { "ocap.hardware.version", "ocap.hardware.model_id", "ocap.software.model_id",
                            "ocap.software.vendor_id", "ocap.software.version" };
                    for (int x = 0; x < apsspStr.length; x++)
                        permissions.add(new PropertyPermission(apsspStr[x], "read"));
                }
            }
            else if (SUPPORT_PRE_ECO_852 && "registeredapi".equals(name))
            {
                permissions.add(new MonitorAppPermission("registeredapi.manager"));
                permissions.add(new RegisteredApiUserPermission("*"));
            }
        }
    }

    /**
     * Inner class used to parse the <ocap:servicetypepermission/> element and
     * all sub-elements.
     */
    private class ServiceTypeState extends AttribState
    {
        public void start(String tagName, Attributes attr)
        {
            if ("true".equals(attr.getValue("value", "false")))
            {
                String type = attr.getValue("type", "broadcast");
                String action = attr.getValue("action", "all");

                // DTD specifies "all", but ServiceTypePermission specifies "*"
                if ("all".equals(action)) action = "*";

                permissions.add(new ServiceTypePermission(type, action));
            }
            else if (SUPPORT_I15_SERVICETYPE && "true".equals(attr.getValue("ocap:value", "false")))
            {
                String type = attr.getValue("ocap:type", "broadcast");
                String actions = attr.getValue("ocap:actions", "*");

                permissions.add(new ServiceTypePermission(type, actions));
            }
            serviceType = true;
        }
    }

    /**
     * Inner class used to parse the <ocap:registeredapi.user/> element.
     */
    private class RegApiUserState extends AttribState
    {
        public void start(String tagName, Attributes attr)
        {
            String name = attr.getValue("name");
            if (name != null) permissions.add(new RegisteredApiUserPermission(name));
        }
    }

    /**
     * Inner class used to parse the <ocap:ixc/> element.
     */
    private class IXCState extends AttribState
    {
        public void start(String tagName, Attributes attr)
        {
            String bindName = attr.getValue("name", "*");
            String service = null;
            String orgID = null;
            String appID = null;

            // Must define an "action" attribute of either "bind" or "lookup"
            String action = attr.getValue("action");
            if (action != null)
            {
                // Convert "scope" attribute into service permission string
                // as per OCAP 1.0 Section 14.2.2.9.3
                String scope = attr.getValue("scope", "service");
                if (scope == null || "service".equals(scope))
                {
                    service = "service-" + serviceContextID.longValue();
                }
                else if ("xservice".equals(scope))
                {
                    service = "service-*";
                }

                // Set orgID, appID based on action
                if ("bind".equals(action))
                {
                    orgID = Integer.toHexString(appid.getOID());
                    appID = Integer.toHexString(appid.getAID());
                }
                else if ("lookup".equals(action))
                {
                    orgID = attr.getValue("oid", "*");
                    appID = attr.getValue("aid", "*");
                }

                // "action" attribute must be either "bind" or "lookup"
                if (orgID != null && appID != null)
                {
                    String permName = "/" + service + "/" + "signed" + "/" + orgID + "/" + appID + "/" + bindName;
                    permissions.add(new OcapIxcPermission(permName, action));
                    ixcPermissionFound = true;
                }
            }
        }
    }

    /**
     * The set of requested permissions.
     */
    private AppPermissions permissions;

    /**
     * Indicates whether <servicesel> was used to deny service selection.
     */
    private boolean selectDenied = false;

    /**
     * Indicates whether or not a <ocap:ixc> permission was parsed. This
     * dictates whether or not the default IXC signed permissions should be
     * added to this applications permission set. See OCAP 1.0 Section
     * 14.2.2.9.3 (from OCAP ECN OCAP1.0-N-07.1045-2)
     */
    private boolean ixcPermissionFound = false;

    /**
     * Indicates that <code>MonitorAppPermission("servicemanager")</code> was
     * requested.
     */
    private boolean monappServiceManager = false;

    /**
     * Indicates that <code>MonitorApplication("service")</code> was requested.
     */
    private boolean monappService = false;

    /**
     * Indicates that <code>ServiceTypePermission</code> was requested.
     */
    private boolean serviceType = false;

    /**
     * The application identifier.
     */
    private AppID appid;

    /**
     * The service context ID of the app
     */
    private Long serviceContextID;

    /**
     * Whether <ocap:monapp> permissions are allowed or not.
     */
    private boolean monApp;

    /**
     * Whether <ocap:*> permissions are allowed or not.
     */
    private boolean ocap;

    /**
     * The set of extensions that are supported.
     */
    private Set extensions;

    /**
     * If <code>true</code> then I16 PRF is supported in addition to the latest.
     */
    private static final boolean SUPPORT_I16_PRF = "true".equals(MPEEnv.getEnv("OCAP.prf.I16"));

    /**
     * If <code>true</code> then "registeredapi" is seen as equivalent to
     * "registeredapi.manager" and RegisteredApiUserPreference("*").
     * Essentially, pre-EC 852 I16 "registeredapi" syntax is accepted as an
     * alternative to EC 852 syntax.
     * 
     * @see #SUPPORT_I16_PRF
     */
    private static final boolean SUPPORT_PRE_ECO_852 = true && SUPPORT_I16_PRF;

    /**
     * If <code>true</code> then I15 PRF is supported in addition to the latest.
     */
    private static final boolean SUPPORT_I15_PRF = "true".equals(MPEEnv.getEnv("OCAP.prf.I15"));

    /**
     * If <code>true</code> then I15 &lt;ocap:monitorapplication> is recognized
     * as an alternative to I16 syntax.
     */
    private static final boolean SUPPORT_I15_MONAPP = true && SUPPORT_I15_PRF;

    /**
     * If <code>true</code> then I04 HN &lt;ocap:homenetpermission> is
     * recognized.
     */
    private static final boolean SUPPORT_I04_HN = true;

    /**
     * If <code>true</code> then I15 &lt;ocap:servicetype> is recognized as an
     * alternative to I16 syntax.
     */
    private static final boolean SUPPORT_I15_SERVICETYPE = true && SUPPORT_I15_PRF;

    // TODO(AaronK): re-enable certificate/signature testing
    private static final boolean IGNORE_CERTIFICATE = true;

    /*
     * Returns the <code>AppID</code> expected/specified in the permission
     * request file.
     */
    public AppID getAppID()
    {
        return appid;
    }

    /**
     * Returns the permissions specified in the permission request file.
     */
    public AppPermissions getPermissions()
    {
        return permissions;
    }
}
