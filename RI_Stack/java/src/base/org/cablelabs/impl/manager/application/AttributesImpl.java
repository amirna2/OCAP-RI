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

package org.cablelabs.impl.manager.application;

import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.IllegalProfileParameterException;
import org.dvb.application.LanguageNotAvailableException;
import org.dvb.user.Facility;
import org.dvb.user.GeneralPreference;
import org.dvb.user.Preference;
import org.dvb.user.UserPreferenceManager;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.net.URLLocator;

import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An implementation of <code>OcapAppAttributes</code> that is based upon an
 * {@link AppEntry}.
 * 
 * @author Aaron Kamienski
 * @author Mike Schoonover - fixed bug 5517 and extracted this class to its own
 *         file
 * @author Alan Cohn - changes for ECR OCAP1.0-N-07.1011-2
 */
public class AttributesImpl implements OcapAppAttributes, AppAttributesExt
{
    private static final Logger log = Logger.getLogger(AttributesImpl.class.getName());

    private static final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static final CallerContext systemContext = ccm.getSystemContext();

    private final AppEntry entry;

    private AppIcon icon;

    private int componentTag = -1;

    private OcapLocator locator;

    /**
     * Constructs an instance of <code>AttributesImpl</code> given the
     * <code>AppEntry</code> as acquired from application signalling.
     * 
     * @param entry the application
     * @param serviceLocator the associated service locator
     */
    public AttributesImpl(AppEntry entry, OcapLocator serviceLocator)
    {
        this.entry = entry;
        this.locator = serviceLocator;

        for (int i = 0; i < entry.transportProtocols.length; ++i)
        {
            if (entry.transportProtocols[i] instanceof AppEntry.OcTransportProtocol)
            {
                AppEntry.OcTransportProtocol oc = (AppEntry.OcTransportProtocol) entry.transportProtocols[i];

                // Only consider remoteConnection if controlCode is REMOTE
                // See MHP 11.7.2
                if (oc.remoteConnection == (entry.controlCode == OcapAppAttributes.REMOTE))
                {
                    componentTag = oc.componentTag;
                    break;
                }
            }
        }
    }

    // Description copied from AppAttributes
    public int getType()
    {
        return OcapAppAttributes.OCAP_J;
    }

    // Description copied from AppAttributes
    public String getName()
    {
        // If no names are signalled, return null
        int size = entry.names.size();
        if (size == 0) return null;

        // Create a Facility object from set of supported languages
        String[] languages = new String[entry.names.size()];
        int i = 0;
        for (Enumeration e = entry.names.keys(); e.hasMoreElements();)
        {
            languages[i++] = (String) e.nextElement();
        }
        Facility facility = new Facility("User Language", languages);

        // Look up preferred language using Facility
        UserPreferenceManager upm = UserPreferenceManager.getInstance();
        Preference pref = new GeneralPreference("User Language");
        upm.read(pref, facility);
        String prefLanguage = pref.getMostFavourite();

        // Default to "eng" (if present)
        if (prefLanguage == null && entry.names.get("eng") != null) prefLanguage = "eng";
        // Finally just pick one
        if (prefLanguage == null) prefLanguage = languages[0];

        return (String) entry.names.get(prefLanguage);
    }

    // Description copied from AppAttributes
    public String getName(String iso639code) throws LanguageNotAvailableException
    {
        String str = (String) entry.names.get(iso639code);
        if (str == null) throw new LanguageNotAvailableException(iso639code);
        return str;
    }

    // Description copied from AppAttributes
    public String[][] getNames()
    {
        String[][] names = new String[entry.names.size()][];
        int i = 0;
        for (Enumeration e = entry.names.keys(); e.hasMoreElements();)
        {
            String lang = (String) e.nextElement();
            String name = (String) entry.names.get(lang);

            names[i++] = new String[] { lang, name };
        }
        return names;
    }

    /**
     * Decodes the given profile encoding.
     */
    private String decodeProfile(int profile)
    {
        switch (profile)
        {
            case 1:
                return "mhp.profile.enhanced_broadcast"; // MHP
            case 2:
                return "mhp.profile.interactive_broadcast"; // MHP
            case 0x101:
                return "ocap.profile"; // original profile
            case 0x102:
                return "ocap.profile"; // 0x102 OCAP 1.1.1, "ocap.profile" per
                                       // 18.2.1.1
            default:
                return Integer.toString(profile);
        }
    }

    /**
     * Encodes the given profile string.
     */
    private int encodeProfile(String str) throws IllegalProfileParameterException
    {
        if ("mhp.profile.enhanced_broadcast".equals(str)) // MHP
            return 1;
        else if ("mhp.profile.interactive_broadcast".equals(str)) // MHP
            return 2;
        else if ("ocap.profile".equals(str)) // OCAP
            return 0x102; // OCAP 1.1.1, 18.2.1.1
        else
            throw new IllegalProfileParameterException(str);
    }

    // Description copied from AppAttributes
    public String[] getProfiles()
    {
        int size = entry.versions == null ? 0 : entry.versions.size();
        String[] profiles = new String[size];
        if (size > 0)
        {
            int i = 0;
            for (Enumeration e = entry.versions.keys(); e.hasMoreElements();)
            {
                Integer profile = (Integer) e.nextElement();
                profiles[i++] = decodeProfile(profile.intValue());
            }
        }
        return profiles;
    }

    // Description copied from AppAttributes
    public int[] getVersions(String profile) throws IllegalProfileParameterException
    {
        int[] versions = (int[]) entry.versions.get(new Integer(encodeProfile(profile)));
        // The String ocap.profile is encoded as 0x102, but the application may
        // have defined ocap.profile 0x101 (also supported)
        if (versions == null && "ocap.profile".equals(profile))
        {
            versions = (int[]) entry.versions.get(new Integer(0x101));
        }

        if (versions != null)
        {
            int[] copy = new int[versions.length];
            System.arraycopy(versions, 0, copy, 0, copy.length);
            return copy;
        }
        throw new IllegalProfileParameterException("Unable to find application profile for: " + profile);
    }

    // Description copied from AppAttributes
    public boolean getIsServiceBound()
    {
        return entry.serviceBound;
    }

    /**
     * This method determines whether the application is startable or not. An
     * Application is not startable if any of the following apply.
     * <ul>
     * <li>The application is transmitted on a remote connection.
     * <li>The caller of the method does not have the Permissions to start it.
     * <li>if the application is signalled with a control code which is neither
     * AUTOSTART nor PRESENT.
     * </ul>
     * If none of the above apply, then the application is startable.
     * <p>
     * The value returned by this method does not depend on whether the
     * application is actually running or not.
     * 
     * @return true if an application is startable, false otherwise.
     * 
     * @since MHP1.0
     */
    public boolean isStartable()
    {
        // not startable if on a REMOTE connection OR app is neither AUTOSTART
        // nor PRESENT
        // The 'Permissions' mentioned above are that this application is
        // visible and signaled in the currently selected service
        // NOTE: AppsControlPermission is only required to 'CONTROL'
        // (stop/pause/resume) an application by an app that didn't start it, it
        // is not required to start
        boolean notStartable = ((entry.controlCode == REMOTE) || (entry.controlCode != AUTOSTART && entry.controlCode != PRESENT));

        boolean startable = !notStartable;

        if (log.isDebugEnabled())
        {
            log.debug("isStartable -- entry: " + entry + ", result: " + startable);
        }
        return startable;
    }

    // Description copied from AppAttributes
    public AppID getIdentifier()
    {
        return entry.id;
    }

    // Description copied from AppAttributes
    public AppIcon getAppIcon()
    {
        if (icon == null)
        {
        
            String base;
            String iconLoc;
    
            if (entry.iconLocator == null) return null;
    
            // create base string path
            // make sure it does not start with '/' but does end with '/'
            base = entry.baseDirectory;
            if (!base.endsWith("/"))
            {
                base += "/";
            }
            if (base.startsWith("/"))
            {
                base = base.substring(1);
            }
    
            // make sure icon location doesn't start with '/' since base ends with
            // one
            iconLoc = entry.iconLocator;
            if (iconLoc.startsWith("/"))
            {
                iconLoc = iconLoc.substring(1);
            } 
    
            for (int i = 0; i < entry.transportProtocols.length; i++)
            {
                try
                {
                    TransportProtocol tp = entry.transportProtocols[i];
                    if (tp instanceof OcTransportProtocol)
                    {
                        int sourceID = locator.getSourceID();
                        
                        // leave out component tags - locator represents the 'location
                        // of the directory containing the application icons'
                        OcapLocator loc = (sourceID != -1) ?
                                (new OcapLocator(sourceID, -1, new int[] {}, base + iconLoc)) :
                                (new OcapLocator(locator.getFrequency(),
                                                 locator.getProgramNumber(),
                                                 locator.getModulationFormat(),
                                                 -1, new int[] {}, base + iconLoc));
        
                        icon = new AppIconImpl(loc, entry.iconFlags);
                        break;
                    }
                    else if (tp instanceof IcTransportProtocol)
                    {
                        String ict;
                        IcTransportProtocol ictp = (IcTransportProtocol)tp;
        
                        // Just use the first URL (is this correct?)
                        ict = (String)ictp.urls.elementAt(0);
                        
                        // since base starts with a '/' remove any ending '/' from
                        // ictURL
                        if (ict.endsWith("/")) ict = ict.substring(0, ict.length() - 1);
        
                        URLLocator urlLocator = new URLLocator(ict + base + iconLoc);
                        icon = new AppIconImpl(urlLocator, entry.iconFlags);
                        break;
                    }
                }
                catch (org.davic.net.InvalidLocatorException e)
                {
                    continue;
                }
            }
        }
        
        return icon;
    }

    /**
     * Implements {@link AppAttributes#getPriority}.
     * <p>
     * Note that this reflects what is signalled for this application. It
     * doesn't reflect whatever override may be in-place (per
     * {@link AppManagerProxy#setApplicationPriority}.
     * <p>
     * This is because MHP-GEM 11.7.2 says that this method returns what is
     * specified in the application signalling.
     * <p>
     * ECO 1115 corrects this by saying that the source of this info comes from
     * signaling or setApplicationPriority().
     * 
     * @return the priority of the application.
     */
    public int getPriority()
    {
        int appPri;

        if (entry.priority == 255)
            return entry.priority;
        else if ((appPri = AppManager.getAppManager().getApplicationPriority(entry.id, entry.version)) >= 0)
            return appPri;
        else
            return entry.priority;
    }

    // Description copied from AppAttributes
    public Locator getServiceLocator()
    {
        return locator;
    }

    // Description copied from AppAttributes
    public Object getProperty(String index)
    {
        if ("ocap.j.location".equals(index) || "dvb.j.location.base".equals(index))
        {
            return entry.signaledBasedir;
        }
        else if ("dvb.j.location.cpath.extension".equals(index))
        {
            String ext[] = new String[entry.classPathExtension.length];
            System.arraycopy(entry.classPathExtension, 0, ext, 0, ext.length);
            return ext;
        }
        else if ("dvb.transport.oc.component.tag".equals(index))
        {
            return new Integer(componentTag);
        }
        else
            return null;
    }

    // Description copied from OcapAppAttributes
    public boolean isVisible()
    {
        return entry.visibility == AppEntry.VISIBLE;
    }

    // Description copied from OcapAppAttributes
    public int getApplicationControlCode()
    {
        return entry.controlCode;
    }

    // Description copied from OcapAppAttributes
    public int getStoragePriority()
    {
        if (entry instanceof XAppEntry)
        {
            XAppEntry xae = (XAppEntry)entry;
            AppStorage storage = retrieveApp(xae.version);
            return (storage == null) ? 0 : xae.storagePriority;
        }
        return 0; // Bound apps always have 0 storage priority
    }

    // Description copied from OcapAppAttributes
    public boolean isNewVersionSignaled()
    {
        // If there is new version, then a new version can be stored.
        if (entry instanceof XAppEntry)
        {
            XAppEntry xapp = (XAppEntry)entry;
            return xapp.newVersion != null;
        }
        return false;
    }

    // Description copied from OcapAppAttributes
    public boolean hasNewVersion()
    {
        if (isNewVersionSignaled() == false)
        {
            return false;
        }

        return (retrieveApp(((XAppEntry)entry).newVersion.version) != null);
    }

    /**
     * Returns the <i>version</i> of the application expressed in the
     * {@link AppEntry#version signaling}.
     * 
     * @return the signaled version of the application; zero for bound
     *         applications
     */
    long getVersion()
    {
        return entry.version;
    }

    /**
     * Returns the signaled application mode for this application. That is, the
     * application mode that this application would run in when the
     * application's home environment is not {@link EnvironmentState#SELECTED
     * selected} or {@link EnvironmentState#PRESENTING presenting}.
     * <p>
     * A value of <code>NORMAL_MODE</code> indicates that the application will
     * be terminated when the home environment is not selected or presenting.
     * Any other value indicates the mode that the application would run in.
     * <p>
     * Where no <code>application_mode_descriptor</code> was signaled, the
     * default value of <code>LEGACY_MODE</code> SHALL be returned.
     * <p>
     * The current application mode can be determined by consulting
     * {@link Environment#getState} in addition to considering the signaled
     * application mode.
     * 
     * @return one of {@link #LEGACY_MODE}, {@link #NORMAL_MODE},
     *         {@link #CROSSENVIRONMENT_MODE}, {@link #BACKGROUND_MODE}, or
     *         {@link #PAUSED_MODE}
     */
    public int getApplicationMode()
    {
        return entry.application_mode;
    }

    /*
     * Retrieve the app from storage. Execute this in the system context, as it
     * requires enhanced permissions to check the underyling storage. TODO:
     * Probably should move this into the AppStorageManager itself, but easier
     * here.
     */
    private AppStorage retrieveApp(final long version)
    {
        // If application is found in storage, then return storagePriority
        // if application is NOT found, then return 0
        final AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);

        final AppStorage storage[] = new AppStorage[1];
        storage[0] = null;

        try
        {
            systemContext.runInContextSync(new Runnable()
            {
                public void run()
                {
                    AppStorage x = asm.retrieveApp(entry.id, version, entry.className);
                    storage[0] = x;
                }
            });
        }
        catch (InvocationTargetException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        return storage[0];
    }
    /**
     * This method returns the version information of the application.
     * @return app version as string.
     */
    public String getAppVersion()
    {
        return String.valueOf(entry.version);
    }

   /**
    * This method returns the launch order information of the application.
    * @return the launch order as int
    */
    public int getLaunchOrder()
    {
        int ret = 0;
        
        if (entry instanceof XAppEntry)
        {
            XAppEntry xapp = (XAppEntry)entry;
            ret =  xapp.launchOrder;
        }

        return ret; 
    }

   /**
    * This method returns the service id of the application.
    * @return the service id as int.
    */
    public int getServiceId()
    {
        int ret = 0;
        
        if (entry instanceof XAppEntry)
        {
            XAppEntry xapp = (XAppEntry)entry;
            ret = xapp.launchOrder;
        }
        
        return ret; 
    }
}
