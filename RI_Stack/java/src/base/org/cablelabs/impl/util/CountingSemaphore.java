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
package org.cablelabs.impl.util;

import org.apache.log4j.Logger;

/**
 * A semaphore implementation supporting acquires and releases of permits.
 */
public class CountingSemaphore
{
    private final int m_permits;
    private int m_inUseCount;
    private static Logger log = Logger.getLogger(CountingSemaphore.class);

    private final Object m_lock = new Object();

    /**
     * Constructor
     * @param permits the number of permits supported by this instance.  
     *                Acquire blocks if this number of permits are already acquired until release or releaseAll are called.
     */
    public CountingSemaphore(int permits)
    {
        m_permits = permits;
    }

    /**
     * Acquire a permit.  Will block if all permits are in-use.
     */
    public void acquire()
    {
        if (log.isTraceEnabled())
        {
            log.trace("acquire");
        }
        synchronized(m_lock)
        {
            try
            {
                //block to acquire a permit if all permits in use
                while (m_permits == m_inUseCount)
                {
                    m_lock.wait(1000);
                }
                if (log.isTraceEnabled()) 
                {
                    log.trace("acquired");
                }
                m_inUseCount++;
            }
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("acquire interrupted", e);
                }
            }
        }
    }

    /**
     * Release a permit.  Calling release when there are no in-use permits is a no-op.
     */
    public void release()
    {
        if (log.isTraceEnabled()) 
        {
            log.trace("release");
        }
        synchronized(m_lock)
        {
            //if release called when no permits acquired, no-op 
            if (m_inUseCount > 0)
            {
                m_inUseCount--;
                m_lock.notifyAll();
            }
            if (log.isTraceEnabled())
            {
                log.trace("released");
            }
        }
    }

    /**
     * Block until all in-use permits are released.  Awaiting when there are no in-use permits is a no-op.
     */
    public void await()
    {
        if (log.isTraceEnabled())
        {
            log.trace("await");
        }
        synchronized(m_lock)
        {
            try
            {
                while (m_inUseCount > 0)
                {
                    m_lock.wait(1000);
                }
                if (log.isTraceEnabled())
                {
                    log.trace("await complete");
                }
            }
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("await interrupted", e);
                }
            }
        }
    }

    /**
     * Set the in-use permit count to zero.
     */
    public void releaseAll()
    {
        if (log.isTraceEnabled())
        {
            log.trace("releaseAll");
        }
        synchronized(m_lock)
        {
            m_inUseCount = 0;
            m_lock.notifyAll();
            if (log.isTraceEnabled())
            {
                log.trace("releaseAll");
            }
        }
    }
}
