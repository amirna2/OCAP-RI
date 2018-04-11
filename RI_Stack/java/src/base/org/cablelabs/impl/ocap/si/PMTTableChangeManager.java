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

package org.cablelabs.impl.ocap.si;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;

import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.service.SIDatabaseImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.util.SystemEventUtil;
import org.apache.log4j.Logger;

/**
 * <code>PATTableChangeManager</code> is responsible for pulling
 * service-specific information out of the <code>SIChangeEvent</code> upon
 * receipt of a table change notification. Then the information is used to
 * notify only the listeners interested in that service.
 * 
 * @author Greg Rutz
 */
public class PMTTableChangeManager extends TableChangeManager implements TableChangeListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(PMTTableChangeManager.class.getName());

    public PMTTableChangeManager(final SICache siCache)
    {
        CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        final PMTTableChangeManager thisObject = this;

        // The TableChangeManagers are singletons first created by applications
        // in their own contexts. However, we want PMT notifications coming from
        // SICache to take place on the system context.
        try
        {
            callerContextManager.getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    siCache.addPMTChangeListener(thisObject);
                }
            });
        }
        catch (Exception e)
        {
            SystemEventUtil.logCatastrophicError(e);
        }
    }

    public void notifyChange(SIChangeEvent event)
    {
        SIChangeType changeType = event.getChangeType();
        ProgramMapTableExt pmt = (ProgramMapTableExt) event.getSIElement();

        if (log.isDebugEnabled())
        {
            log.debug("PMTTableChangeManager notifyChange event:" + event + ", changeType: " + changeType);
        }
        
        // Notify all listeners that are registered to the frequency and
        // program number of the service associated with this PMT
        notifyListeners(new Service(-1, pmt.getFrequency(), pmt.getProgramNumber()), event);

        // Notify all listeners who have registered to a service associated with
        // the sourceID (if available) of this PMT
        if (pmt.getSourceID() != -1)
        {
            notifyListeners(new Service(pmt.getSourceID(), -1, -1), event);

        }

        // Notify all listeners who have registered using this 'Service'
        if (pmt.getServiceHandle() != 0)
        {
            notifyListeners(new Service(pmt.getServiceHandle()), event);
        }            
    }
}
