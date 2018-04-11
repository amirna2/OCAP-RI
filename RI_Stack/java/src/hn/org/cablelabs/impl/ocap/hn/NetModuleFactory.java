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

package org.cablelabs.impl.ocap.hn;

import java.util.HashMap;
import java.util.Map;
import org.ocap.hn.NetModule;
import org.ocap.hn.upnp.common.UPnPService;

/**
 * NetModuleFactory - factory class for generating specific
 * <code>NetModules</code> from the UPnP <code>IService</code> and associated
 * <code>IActions</code> based on the type of service and if it is a local
 * service.
 *
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * @author Dan Woodard (Flashlight Engineering and Consulting)
 *
 * @version $Revision$
 *
 * @see {@link org.cablelabs.impl.ocap.hn.DeviceList}
 */
public class NetModuleFactory
{
    /**
     * The map of service type to the local <code>NetModule</code> for that
     * service type.
     */
    private static final Map localNetModules = new HashMap();

    /**
     * The map of service type to a <code>NetModuleProducer</code>
     * that can produce the local <code>NetModule</code> for that service type.
     */
    private static final Map netModuleProducers = new HashMap();

    /**
     * Get the local <code>NetModule</code> for a particular service type,
     * if any.
     *
     * @param type The service type.
     *
     * @return A reference to the local <code>NetModule</code> for the given
     *         service type, if one exists; else null.
     */
    public static NetModule localNetModule(int type)
    {
        return (NetModule) localNetModules.get(intern(type));
    }

    /**
     * Register a <code>NetModuleProducer</code> for producing the
     * local <code>NetModule</code> for a particular service type with the
     * <code>NetModuleFactory</code>.
     *
     * @param type The service type.
     * @param nmp The <code>NetModuleProducer</code>.
     */
    public static void registerProducer(int type, NetModuleProducer nmp)
    {
        netModuleProducers.put(intern(type), nmp);
    }

    /**
     * Get (not necessarily create, in spite of the name) a reference to a
     * <code>NetModule</code> for a given device and service.
     * <p>
     * <code>NetModule</code>s for the local device are obtained through
     * <code>NetModuleProducer</code>s and cached by service type.
     * This implies that there can be only one <code>NetModule</code> of
     * a given service type for the local device.
     * <p>
     * <code>NetModule</code>s for remote devices are constructed anew
     * on each call.
     *
     * @param device The device.
     * @param iService The service.
     *
     * @return A reference to the specified <code>NetModule</code>.
     *
     * @throws IllegalStateException if due to a coding error in the stack
     *                               there is no <code>NetModuleProducer</code>
     *                               for the service type.
     */
    /*package*/ 
    // *TODO* - this used to be used in DeviceList, is this functionality needed elsewhere?
    /*
    static NetModule createNetModule(Device device, IService iService)
    {
        IAction[] iActions = UPnPControlPoint.getInstance().getActions(iService);

        NetModule result;

        if (device.isLocal())
        {
            Integer type = intern(iService.getType());

            result = (NetModule) localNetModules.get(type);

            if (result == null)
            {
                NetModuleProducer nmp = (NetModuleProducer) netModuleProducers.get(type);

                if (nmp == null)
                {
                    throw new IllegalStateException();
                }

                result = nmp.netModule(device, iService, iActions);
                localNetModules.put(type, result);
            }
        }
        else
        {
            switch (iService.getType())
            {
                case IService.SCHEDULED_RECORDING_SERVICE_TYPE:
                    result = new RecordingNetModuleImpl(device, iService, iActions);
                    break;

                case IService.CONTENT_DIRECTORY_SERVICE_TYPE:
                    result = new ContentServerNetModuleImpl(device, iService, iActions);
                    break;

                case IService.CONNECTION_MANAGER_SERVICE_TYPE:
                    result = new ConnectionManagerProxy(device, iService, iActions);
                    break;

                default:
                    result = new NetModuleImpl(device, iService, iActions);
                    break;
            }
        }

        return result;
    }
*/
    /**
     * Interning array for use in the absence of autoboxing and Integer.valueOf(int).
     */
    private static final Integer[] INTERN = new Integer[3];

    /**
     * Cache and return an <code>Integer</code> corresponding to an <code>int</code>.
     * <p>
     * We exploit our tricky knowledge that there are only three service types,
     * though the code would continue to function (suboptimally) if there were more.
     *
     * @param i The <code>int</code>.
     *
     * @return A reference to the corresponding <code>Integer</code>.
     */
    private static Integer intern(int i)
    {
        Integer result;

        if (i >= 3)
        {
            result = new Integer(i);
        }
        else
        {
            result = INTERN[i];

            if (result == null)
            {
                result = new Integer(i);
                INTERN[i] = result;
            }
        }

        return result;
    }

    /**
     * A <code>NetModuleProducer</code> can produce the local <code>NetModule</code>
     * for a given device, service, and actions. (Actually, the actions are determined
     * by the service, so by rights they should not be involved here.)
     * <p>
     * <code>NetModuleProducer</code>s are registered with the
     * <code>NetModuleFactory</code>.
     */
    public interface NetModuleProducer
    {
        /**
         * Produce the local <code>NetModule</code> for a given device, service,
         * and actions.
         *
         * @param device The device.
         * @param iService The service.
         * @param iActions The actions.
         *
         * @return A reference to the specified local <code>NetModule</code>.
         */
        //public NetModule netModule(Device device, IService iService, IAction[] iActions);
        public NetModule netModule(UPnPService service);
    }
}
