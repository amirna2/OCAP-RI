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

package org.cablelabs.impl.manager.auth;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.MPEEnv;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.InvalidFormatException;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotEntitledException;
import org.dvb.dsmcc.ServerDeliveryException;
import org.dvb.dsmcc.ServiceXFRException;
import org.ocap.application.AppFilter;
import org.ocap.application.AppPattern;

/**
 * This is a <i>dummy</i> implementation of the <code>AuthManager</code>
 * interface. An instance of this class may be used in place of the standard
 * <code>AuthManager</code> implementation to disable all authentication at
 * runtime.
 * 
 * @author Aaron Kamienski
 */
public class NoAuthentication implements AuthManager
{
    public static Manager getInstance()
    {
        return new NoAuthentication();
    }

    private NoAuthentication()
    {
        // Support un-authenticated apps...
        setupAppOverrides();
        // Support un-authenticated files...
        // TODO: support un-authenticated files...
    }

    private SysAuthCtx systemCtx = new SysAuthCtx();

    private static class SysAuthCtx extends DummyAuthCtx
    {
        SysAuthCtx()
        {
            super(null);
        }

        public int getAppSignedStatus()
        {
            return AuthInfo.AUTH_UNKNOWN;
        }
    }

    private static class DummyAuthCtx implements AuthContext, CallbackData
    {
        DummyAuthCtx(Auth xlet)
        {
            this.xlet = xlet;
        }

        public int getAppSignedStatus()
        {
            return xlet.getClassAuth();
        }

        public AuthInfo getClassAuthInfo(String name, FileSys fs) throws FileSysCommunicationException
        {
            // Authenticate classes based upon app type
            try
            {
                byte[] data = fs.getFileData(name).getByteData();
                return new Auth(getAppSignedStatus(), data);
            }
            catch (IOException e)
            {
                if (XLOGGING)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("File inaccessible", e);
                    }
                }
                return null;
            }
        }

        AuthInfo getFileAuthInfoX(String name, FileSys fs) throws IOException
        {
            // Always authenticate files...
            try
            {
                byte[] data = fs.getFileData(name).getByteData();
                return new Auth(true, data);
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
        }

        AuthInfo getFileAuthInfoX(String name, FileSys fs, byte[] file) throws IOException
        {
            // Always authenticate files...
            try
            {
                return new Auth(true, fs.getFileData(name).getByteData());
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
        }

        AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files)
        {
            // Always authenticate files...
            return new Auth(true);
        }

        String[] getHashfileNames(String dir, FileSys fs) throws FileSysCommunicationException
        {
            // Try the .hashfile method first
            String[] names = getHashfileNamesReal(dir, fs);
            if (names == null && USE_DOT_DIR) names = getHashfileNamesDir(dir, fs);
            return names;
        }

        String[] getHashfileNamesReal(String dir, FileSys fs) throws FileSysCommunicationException
        {
            // Try ourself
            HashFile hf;

            if (((hf = getDirHashFile(dir, fs, "ocap")) != null)
                    || (USE_HASHFILE_DVB && ((hf = getDirHashFile(dir, fs, "dvb")) != null)))
            {
                return hf.getNames();
            }

            return null;
        }

        String[] getHashfileNamesDir(String dir, FileSys fs)
        {
            String listFile = dir + (dir.endsWith("/") ? "" : "/") + ".dir";
            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fs.getFileData(
                        listFile).getByteData())));
                try
                {
                    Vector lines = new Vector();
                    String line;
                    while ((line = in.readLine()) != null)
                    {
                        line = line.trim();
                        if (!line.startsWith("#") && line.length() > 0) lines.addElement(line);
                    }
                    String[] array = new String[lines.size()];
                    lines.copyInto(array);
                    return array;
                }
                catch (Exception e)
                {
                    if (XLOGGING)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Problem parsing " + listFile, e);
                        }
                    }
                    return new String[0];
                }
            }
            catch (Exception e)
            {
                if (XLOGGING)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Inaccessible " + listFile, e);
                    }
                }
                return null;
            }
        }

        public void destroy(CallerContext ctx)
        { /* empty */
        }

        public void active(CallerContext ctx)
        { /* empty */
        }

        public void pause(CallerContext ctx)
        { /* empty */
        }

        private Auth xlet;
    }

    public AuthContext createAuthCtx(String initialFile, int signers, int orgId) throws FileSysCommunicationException
    {
        // FileManager instance
        FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

        int type = AuthInfo.AUTH_UNKNOWN;
        switch (signers)
        {
            case 0:
                type = AuthInfo.AUTH_UNSIGNED;
                break;
            case 1:
                type = AuthInfo.AUTH_SIGNED_OCAP;
                break;
            default:
                type = AuthInfo.AUTH_SIGNED_DUAL;
                break;
        }
        type = overrideAppType(type);

        Auth xlet;
        try
        {
            FileSys fs = fileMgr.getFileSys(initialFile);
            if (log.isDebugEnabled())
            {
                log.debug("Got file system for " + initialFile + ": " + fs.getClass().getName());
            }
            xlet = new Auth(type, fs.getFileData(initialFile).getByteData());
        }
        catch (IOException e)
        {
            xlet = new Auth(AuthInfo.AUTH_UNKNOWN);
        }
        catch (NullPointerException e)
        {
            xlet = new Auth(AuthInfo.AUTH_UNKNOWN);
        }

        return new DummyAuthCtx(xlet);
    }

    private Hashtable appOverrides = new Hashtable();

    private void setupAppOverrides()
    {
        setupAppOverrides("unsigned", AuthInfo.AUTH_UNSIGNED);
        setupAppOverrides("dvb", AuthInfo.AUTH_SIGNED_DVB);
        setupAppOverrides("ocap", AuthInfo.AUTH_SIGNED_OCAP);
        setupAppOverrides("dual", AuthInfo.AUTH_SIGNED_DUAL);
        setupAppOverrides("fail", AuthInfo.AUTH_FAIL);
    }

    private void setupAppOverrides(String typeName, int typeVal)
    {
        AppFilter filter = setupAppOverrides(typeName);
        if (filter != null) appOverrides.put(filter, new Integer(typeVal));
    }

    private AppFilter setupAppOverrides(String type)
    {
        String value = MPEEnv.getEnv("OCAP.fakeauth." + type);
        if (value == null) return null;

        AppFilter filter = new AppFilter();

        // Expect string of space, comma, semi-colon separate filters
        StringTokenizer tok = new StringTokenizer(value, " ,;");
        for (; tok.hasMoreTokens();)
            filter.add(new AppPattern(tok.nextToken(), AppPattern.DENY, 1));
        return filter;
    }

    private int overrideAppType(int type)
    {
        if (appOverrides.size() > 0)
        {
            // Assume called in-context
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cc = ccm.getCurrentContext();
            AppID id = (AppID) cc.get(CallerContext.APP_ID);
            if (id != null)
            {
                for (Enumeration e = appOverrides.keys(); e.hasMoreElements();)
                {
                    AppFilter filter = (AppFilter) e.nextElement();
                    if (!filter.accept(id)) return ((Integer) appOverrides.get(filter)).intValue();
                }
            }
        }

        return type;
    }

    public void setAuthCtx(CallerContext cc, AuthContext ac)
    {
        if (ac == null)
            cc.removeCallbackData(DummyAuthCtx.class);
        else
            cc.addCallbackData((DummyAuthCtx) ac, DummyAuthCtx.class);
    }

    public AuthContext getAuthCtx(CallerContext cc)
    {
        return (AuthContext) cc.getCallbackData(DummyAuthCtx.class);
    }

    private DummyAuthCtx getAuthCtx()
    {
        DummyAuthCtx returnCtx = null;
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        if (cc == ccm.getSystemContext())
            returnCtx = systemCtx; // TODO: should I really have a system
                                   // context? or should it be set?
        else
        {
            returnCtx = (DummyAuthCtx) getAuthCtx(cc);
            if (returnCtx == null) returnCtx = systemCtx;
        }

        return returnCtx;
    }

    public AuthInfo getClassAuthInfo(String targName, FileSys fs) throws FileSysCommunicationException
    {
        return getAuthCtx().getClassAuthInfo(targName, fs);
    }

    public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
    {
        return getAuthCtx().getFileAuthInfoX(targName, fs);
    }

    public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
    {
        return getAuthCtx().getFileAuthInfoX(targName, fs, file);
    }

    public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
    {
        return getAuthCtx().getDirAuthInfo(dir, fs, files);
    }

    public String[] getHashfileNames(String dir, FileSys fs) throws FileSysCommunicationException, IOException
    {
        DummyAuthCtx actx = getAuthCtx();

        if ((actx != null && dir != null && fs != null)) return actx.getHashfileNames(dir, fs);

        // Try ourself
        HashFile hf;

        if ((hf = getDirHashFile(dir, fs, "ocap")) != null || (hf = getDirHashFile(dir, fs, "dvb")) != null)
        {
            return hf.getNames();
        }

        return null;
    }

    /**
     * Get the associated hash file for the specified target directory.
     * 
     * @param dir
     *            is the path string to the target directory for which the
     *            associated hash file will be located.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target directory.
     * @param msgPrefix
     *            is the ocap or dvb name prefix for the hash file associated
     *            with the target directory.
     * 
     * @return HashFile or null if the hash file for the target is not found.
     */
    private static HashFile getDirHashFile(String dir, FileSys fs, String msgPrefix)
            throws FileSysCommunicationException
    {
        String hashpath = dir + "/" + msgPrefix + ".hashfile";
        try
        {
            HashFile hf;

            // Not in cache, try to read the entire contents of the hash file.
            byte[] file = fs.getFileData(hashpath).getByteData();

            // Instantiate a new hash file.
            hf = new HashFile(dir, file);

            // If the hash file was found, return it.
            return hf;
        }
        catch (IOException e)
        {
            if (XLOGGING)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Inaccessible " + hashpath, e);
                }
            }
            return null;
        }
    }

    public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
    {
        return new X509Certificate[0][0];
    }

    public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
            throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
            InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException
    {
        return new X509Certificate[0][0];
    }

    public void invalidate(String targName)
    {
        // Does nothing
    }

    public void registerCRLMount(String path)
    {
        // Does nothing
    }

    public void unregisterCRLMount(String path)
    {
        // Does nothing
    }

    public void setPrivilegedCerts(byte[] codes)
    {
        // Does nothing
    }

    public void destroy()
    {
        // Does nothing...
    }

    private static final boolean XLOGGING = false;

    private static final Logger log = Logger.getLogger(NoAuthentication.class);

    private static final boolean USE_HASHFILE_DVB = "true".equals(MPEEnv.getEnv("OCAP.appstorage.dvb.use", "false")); // check
                                                                                                                      // dvb.hashfile

    private static final boolean USE_DOT_DIR = "true".equals(MPEEnv.getEnv("OCAP.appstorage.dotdir.use", "false")); // check
                                                                                                                    // .dir
                                                                                                                    // file
}
