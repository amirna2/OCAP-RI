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

package org.cablelabs.impl.manager.system;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SystemManager;
import org.cablelabs.impl.util.SystemEventUtil;

import org.ocap.system.SystemModuleRegistrar;

/**
 * SystemModuleMgr is needed to relay Manager getInstance() and destroy()
 * methods, since org.ocap.system.SystemModuleRegistrar also requires
 * getInstance().
 */
public class SystemModuleMgr implements SystemManager
{
    private static final Logger log = Logger.getLogger(SystemModuleMgr.class);
    private static SystemModuleMgr instance = null;
    
    /**
     * A constructor of this class. An application must use the
     * {@link SystemModuleMgr#getInstance} method to create an instance.
     */
    protected SystemModuleMgr()
    {
        // Start the receive thread within the system context.
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        if (log.isInfoEnabled())
        {
            log.info("constructing SystemModuleMgr");
        }
        
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                SystemModuleRegistrarImpl registrar = (SystemModuleRegistrarImpl) getSystemModuleRegistrar();

                // TODO: Should this thread ever die? It's not clear at this
                // time if this thread will
                // require any life-cycle maintenance as the result of any POD
                // related activity
                // (e.g. hot pulling or attaching of the POD device).
                while (threadDie == false)
                {
                    // Call native method to get next APDU and associated
                    // session handle.
                    try
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("calling podGetNextAPDU");
                        }
                        PodAPDU apdu = podGetNextAPDU();
                        if (apdu != null)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("received APDU - length: " + apdu.getLength());
                            }              
                            registrar.dispatchReceivedAPDU(apdu);
                        }
                    }
                    catch (Exception e)
                    {
                        SystemEventUtil.logRecoverableError("Error receiving APDU", e);
                    }
                }
                if (log.isInfoEnabled())
                {
                    log.info("Thread dead - no longer retrieving APDUs");
                }
            }
        });
    }

    /**
     * Gets the instance attribute of the SystemModuleMgr class
     * 
     * @return The instance value
     */
    public static synchronized Manager getInstance()
    {
        if(instance == null)
        {
            instance = new SystemModuleMgr();
        }
        return instance;
    }

    /**
     * Returns the SystemModuleRegistrar implementation singleton
     * 
     * @return The SystemModuleRegistrarImpl instance
     */
    public SystemModuleRegistrar getSystemModuleRegistrar() throws SecurityException
    {
        return SystemModuleRegistrarImpl.getInstance();
    }

    /**
     * Description of the Method
     */
    public void destroy()
    {
        threadDie = true;
    }

    /**
     * Native method to get the next APDU. This method blocks until data is
     * available.
     * 
     * @return raw APDU
     */
    private static native PodAPDU podGetNextAPDU();
    
    /**
     * Initialize JNI cache
     */
    private static native void nInit();

    /**
     * Flag that indicates when the associated APDU handler thread should die.
     */
    private boolean threadDie = false;

    /*
     * Static initializer - insures that OcapMain has loaded the native JNI
     * library as this class calls native methods.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
