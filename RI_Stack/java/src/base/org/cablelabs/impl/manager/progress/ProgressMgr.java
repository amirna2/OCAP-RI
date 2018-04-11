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

package org.cablelabs.impl.manager.progress;

import org.cablelabs.impl.manager.ProgressManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ProgressListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of the <code>ProgressManager</code> interface.
 * 
 * @author Michael Werts
 */
public class ProgressMgr implements ProgressManager
{
    private final Object lock = new Object();
    
    /**
     * Causes initialization of AWT and HAVi. Not publicly instantiable.
     */
    private ProgressMgr()
    {
    }

    /**
     * Destroys this manager, causing it to release any and all resources.
     * Should only be called by the <code>ManagerManager</code>.
     */
    public void destroy()
    {
    }

    /**
     * Returns the singleton instance of <code>GraphicsManager</code>. Will be
     * called only once.
     * 
     * @return the singleton instance
     */
    public static synchronized Manager getInstance()
    {
        if (mgr == null)
        {
            mgr = new ProgressMgr();
        }
        return mgr;
    }

    public void addListener(ProgressListener listener)
    {
        synchronized(lock)
        {
            listeners.add(listener);
        }
    }

    public void removeListener(ProgressListener listener) {
        
        synchronized (lock)
        {
            listeners.remove(listener);
        }
    }

    public void signal(int milestone)
    {
        Set listenerCopy;
        synchronized (lock)
        {
            listenerCopy = new HashSet(listeners);
        }
        
        for (Iterator iter = listenerCopy.iterator();iter.hasNext();)
        {
            ((ProgressListener)iter.next()).progressSignalled(milestone);
        }
    }

    /**
     * The singleton instance.
     */
    private static ProgressManager mgr;

    /**
     * 
     */
    private final Set listeners = new HashSet();

    /**
     * Boot process milestones.
     */
    public static final int kStartBoot = 0;

    public static final int kXaitReceived = 1;

    public static final int kSearchingForMonitor = 2;

    public static final int kFoundMonitorApp = 3;

    public static final int kSelectMonitorService = 4;

    public static final int kInitialMonAppClassLoaded = 5;

    public static final int kNumMilestones = 6;
}
