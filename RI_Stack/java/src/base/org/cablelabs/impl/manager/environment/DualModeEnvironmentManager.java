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
package org.cablelabs.impl.manager.environment;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.ocap.environment.EnvironmentState;

import org.cablelabs.impl.manager.EnvironmentManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.util.MPEEnv;

public class DualModeEnvironmentManager implements EnvironmentManager
{
    private static DualModeEnvironmentManager singleton;

    Properties environmentProperties = new Properties();

    ExtendedEnvironment ocapEnv = null; // new OcapEnvironment();

    ExtendedEnvironment mfgEnv = null;

    EnvironmentState deselectedState = EnvironmentState.BACKGROUND;

    /**
     * Return the ocap environment.
     */
    public ExtendedEnvironment getOcapEnvironment()
    {
        return ocapEnv;
    }

    /**
     * Select the specified environment. Deselect the other environment.
     * 
     * @param env
     *            The Environment which will be selected.
     */
    public void selectEnvironment(ExtendedEnvironment env)
    {
        if (env != null && env == ocapEnv)
        {
            // TODO: figure out why this has to be called twice
            // if it is not, then focused HScene does not repaint
            env.setState(EnvironmentState.SELECTED);
            env.setState(EnvironmentState.SELECTED);
        }

        // For now just dealing with ocap env
        // Add Mfg Env management later
        // ExtendedEnvironment other = pickOther(env);
        // other.setState(deselectedState);
        // env.setState(EnvironmentState.SELECTED);
    }

    /**
     * Deselect the specified environment. Mark the other environment selected.
     * 
     * @param env
     *            The environment to be deselected.
     */
    public void deselectEnvironment(ExtendedEnvironment env)
    {
        if (env != null && env == ocapEnv)
        {
            env.setState(deselectedState);
        }

        // For now just dealing with ocap env
        // Add Mfg Env management later
        // ExtendedEnvironment other = pickOther(env);
        // env.setState(deselectedState);
        // other.setState(EnvironmentState.SELECTED);
    }

    public void destroy()
    {
        /** Do nothing on destroy **/
    }

    /**
     * Returns the singleton instance of the OcapEnvironmentManager. Will be
     * called only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        if (singleton == null)
        {
            // create singleton instance;
            singleton = new DualModeEnvironmentManager();
            singleton.ocapEnv = new OcapEnvironment(singleton);
        }
        return singleton;
    }

    /**
     * Select the other environment.
     * 
     * @param env
     *            The environment we have.
     * @return The other one.
     */
    private ExtendedEnvironment pickOther(ExtendedEnvironment env)
    {
        if (env == ocapEnv)
            return mfgEnv;
        else
            return ocapEnv;
    }

    /**
     * Create the EnvironmentManager, and load the configuration and create the
     * external environment.
     */
    DualModeEnvironmentManager()
    {

        /*
         * not brining up mfg env yet ... try { // Check property, else get
         * local file String propsFile =
         * MPEEnv.getEnv(SETUP_PROPERTY,"/dualmode.properties");
         * 
         * // Load properties from file java.io.InputStream in =
         * getClass().getResourceAsStream(propsFile); if (in == null) { in =
         * getClass().getResourceAsStream("dualmode.properties"); }
         * 
         * environmentProperties.load(in);
         * 
         * String mfgClassName =
         * environmentProperties.getProperty(MFG_CLASS_NAME);
         * 
         * Class mfgClass = Class.forName(mfgClassName);
         * 
         * mfgEnv = (ExtendedEnvironment) mfgClass.newInstance();
         * 
         * deselectedState =
         * getState(environmentProperties.getProperty(BACKGROUND_STATE_NAME)); }
         * catch (Exception e) { log.error("Unable to load external mode", e);
         * // Catastrophic error???? }
         */
    }

    /**
     * Parse a string containing the name of an environment state.
     * 
     * @param name
     * @return
     */
    private static EnvironmentState getState(String name)
    {
        if (name.equals("BACKGROUND"))
            return EnvironmentState.BACKGROUND;
        else if (name.equals("INACTIVE"))
            return EnvironmentState.INACTIVE;
        else if (name.equals("PRESENTING"))
            return EnvironmentState.PRESENTING;
        else if (name.equals("SELECTED")) return EnvironmentState.SELECTED;
        return null;
    }

    /**
     * Log4J logger.
     */
    private static final Logger log = Logger.getLogger("OcapEnvironmentManager");

    /*
     * Names of properties.
     */
    private static final String SETUP_PROPERTY = "OCAP.dualmode.props";

    private static final String MFG_CLASS_NAME = "mfgClassName";

    private static final String BACKGROUND_STATE_NAME = "backgroundState";
}
