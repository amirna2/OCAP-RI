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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.n3.nanoxml.XMLException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.PropertiesManager;
import org.dvb.application.AppID;
import org.dvb.ui.FontFormatException;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.XmlManager;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.xml.PermissionExtension;
import org.cablelabs.impl.security.AppPermissions;

/**
 * An implementation of <code>XmlManager</code> based upon the NanoXML parser
 * library (2.2.3).
 * 
 * @author Aaron Kamienski
 */
public class XmlMgrImpl implements XmlManager
{
    private static final String PERMISSION_EXTENSION_PARAM_PREFIX = "OCAP.permission.extension";

    /**
     * Returns a new instance. Assumes that <i>singleton</i> aspect is
     * maintained by the <code>ManagerManager</code>.
     * 
     * @return new instance of <code>XmlMgrImpl</code>
     */
    public static Manager getInstance()
    {
        return new XmlMgrImpl();
    }

    /**
     * Destroys this manager, causing it to release any and all resources.
     * Should only be called by the <code>ManagerManager</code>.
     */
    public void destroy()
    {
        // Nothing is necessary
    }

    /**
     * Not publicly instantiable.
     */
    private XmlMgrImpl()
    {
        installExtensions();
    }

    /**
     * Installs any permission extensions.
     * <p>
     * Currently installs a DVR extension if available. Currently installs a
     * Front Panel extension if available. Currently installs a Device Settings
     * extension if available. Currently installs a Home Networking extension if
     * available.
     * <p>
     * Perhaps, instead, we should have extension subclasses of
     * <code>XmlMgrImpl</code> that simply install the extensions. This would
     * avoid any class loading trickery as well as the presence of extension
     * references in this class.
     */
    private void installExtensions()
    {
        extensions.addAll(PropertiesManager.getInstance().getInstancesByPrecedence(PERMISSION_EXTENSION_PARAM_PREFIX));
    }

    /**
     * Parses the font index file found in the given <code>InputStream</code>.
     * Returns an array of <code>FontInfo</code> objects that contain the font
     * information contained within the file.
     * 
     * @param is
     *            the stream from which to read the data
     * @return an array of <code>FontInfo</code> objects; if none were found
     *         then a zero-length array is returned
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     * @throws FontFormatException
     *             if there is an error in the font index file
     */
    public FontInfo[] parseFontIndex(java.io.InputStream is) throws IOException, FontFormatException
    {
        try
        {
            FontIndex xml = new FontIndex();
            xml.go(is);

            return xml.getFontInfo();
        }
        catch (XMLException e)
        {
            // throw a FontFormatException if we couldn't read the file
            Exception ex = (e.getException() == null) ? e : e.getException();
            if (log.isDebugEnabled())
            {
                log.debug("Original exception while parsing font index", ex);
            }
            throw new FontFormatException(ex.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (InstantiationException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (FontIndex.FormatError e)
        {
            // Catch parsing exceptions thrown deep within parser
            if (log.isDebugEnabled())
            {
                log.debug("Original exception while parsing font index", e);
            }
            throw e.getException();
        }
    }

    /**
     * Parses the permission request file found in the given
     * <code>InputStream</code>. Returns a <code>PermissionCollection</code> for
     * the <code>Permission</code>s requested by the file.
     * <p>
     * To accomodate MHP 12.6.2.2, an empty PermissionCollection is returned if
     * there are errors parsing the XML file.
     * 
     * @param prfData
     *            the prf byte data
     * @param ocapPerms
     *            if <code>true</code> then allow OCAP-specific permissions
     *            (i.e., parse the OCAP DTD)
     * @param monAppPerms
     *            if <code>true</code> then allow OCAP-specific
     *            <code>MonitorAppPermission</code>s; if <code>true</code> then
     *            <i>ocapPerms</i> must also be <code>true</code>
     * @param appid
     *            the appid expected in the PRF
     * @param serviceContextID
     *            the service context ID of the calling app
     * @return a <code>PermissionCollection</code> containing the
     *         <code>Permission</code> objects requested by the file
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     * 
     * @see PermissionsRequest
     */
    public AppPermissions parsePermissionRequest(byte[] prfData, boolean ocapPerms, boolean monAppPerms, AppID appid,
            Long serviceContextID) throws IOException
    {
        try
        {
            InputStream is = new ByteArrayInputStream(prfData);
            PermissionsRequest xml = new PermissionsRequest(ocapPerms, monAppPerms, new HashSet(extensions), appid,
                    serviceContextID);
            xml.go(is);

            AppPermissions ap = xml.getPermissions();

            // Allow extensions to add any additional permissions from the PRF
            for (Iterator iter = extensions.iterator(); iter.hasNext();)
            {
                PermissionExtension pe = (PermissionExtension) iter.next();
                ap.add(pe.parsePRF(prfData));
            }

            return ap;
        }
        catch (XMLException e)
        {
            Exception ex = (e.getException() == null) ? e : e.getException();
            if (ex instanceof IOException) throw (IOException) ex;
            if (log.isWarnEnabled())
            {
                log.warn("Improperly formed PRF! Ignoring it.", ex);
            }
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Ignored exception parsing PRF", e);
            }
        }
        return new AppPermissions(); // no additional permissions
    }

    /**
     * Parses the application description file found in the given
     * <code>InputStream</code>. Returns an instance of
     * <code>AppDescriptionInfo</code> containing the information specified by
     * the file.
     * 
     * @param is
     *            the stream from which to read the data
     * @return an instance of <code>AppDescriptionInfo</code>; if none is found
     *         then <code>null</code> is returned
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     */
    public AppDescriptionInfo parseAppDescription(java.io.InputStream is) throws IOException
    {
        try
        {
            AppDesc xml = new AppDesc();
            xml.go(is);

            return xml.getAppDescInfo();
        }
        catch (XMLException e)
        {
            // throw a IOException if we couldn't read the file
            Exception ex = (e.getException() == null) ? e : e.getException();
            if (log.isDebugEnabled())
            {
                log.debug("Original exception while parsing application description", ex);
            }
            throw new IOException(ex.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (InstantiationException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new UnsupportedOperationException(e.getMessage());
        }
        catch (AppDesc.FormatError e)
        {
            // Catch parsing exceptions thrown deep within parser
            if (log.isDebugEnabled())
            {
                log.debug("Original exception while parsing application description", e);
            }
            throw e.getException();
        }
    }

    /**
     * The set of <code>PermissionExtension</code>s supported, keyed by the
     * permission name.
     */
    private final Set extensions = new HashSet();

    /**
     * The Log4J Logger.
     */
    private static final Logger log = Logger.getLogger(XmlMgrImpl.class.getName());
}
