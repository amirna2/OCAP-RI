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

import java.lang.ref.*;
import java.util.Hashtable;
import java.util.Enumeration;
import org.apache.log4j.Logger;

/**
 * Implementation of <code>RefTracker</code> based upon Java2 Reference API.
 * 
 * @author Aaron Kamienski
 */
public class RefTracker2 extends RefTracker implements Runnable
{
    private static final Logger log = Logger.getLogger(RefTracker2.class);

    public RefTracker2()
    {
        t = new Thread(this, "RefTracker");
    }

    // Description copied from RefTracker
    public void track(Object obj)
    {
        if (!t.isAlive()) t.start();

        try
        {
            String str = obj.toString();
            refs.put(createRef(obj), str);
            if (log.isDebugEnabled())
            {
                log.debug("Tracking " + str);
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Creates a <code>Reference</code> given an <code>Object</code>.
     * 
     * @param obj
     *            the referent <code>Object</code>
     */
    private Reference createRef(Object obj)
    {
        if (false)
            return new SoftReference(obj, rq);
        else if (true)
            return new WeakReference(obj, rq);
        else
            return new PhantomReference(obj, rq);
    }

    /**
     * Overrides <code>Thread.run()</code>. Implements the asynchronous tracking
     * of referents.
     * <p>
     * Calls the <code>ReferenceQueue.remove()</code> method to block and
     * retrieve the next tracked <code>Object</code> to be collected. Then
     * writes a Log4J message to the "REFTRACKER" category specifying the
     * <code>Object</code> in question using its <code>toString()</code> value.
     * 
     * @see Thread#run()
     * @see ReferenceQueue#remove()
     */
    public void run()
    {
        while (!quit)
        {
            try
            {
                Reference r = rq.remove();

                if (log.isDebugEnabled())
                {
                    log.debug("GC enqueued " + refs.get(r));
                }

                r.clear();
                refs.remove(r);
                r = null;
            }
            catch (InterruptedException e)
            {
                continue;
            }
        }

        cleanup();
    }

    /**
     * Stops tracking all tracked objects. Calls <code>Thread.interrupt()</code>
     * .
     * 
     * @see Thread#interrupt()
     */
    public void stop()
    {
        quit = true;
        t.interrupt();
    }

    /**
     * Used to elminate any references on exit -- simply an aid to the GC.
     */
    private void cleanup()
    {
        rq = null;

        Enumeration e = refs.keys();
        while (e.hasMoreElements())
        {
            Reference r = (Reference) e.nextElement();

            r.clear();
            r = null;
        }
        refs.clear();
        e = null;
        refs = null;
    }

    /** Thread used to pull off of the queue. */
    private Thread t;

    /** Used to track referents and their names. */
    private Hashtable refs = new Hashtable();

    /** Used to track references. */
    private ReferenceQueue rq = new ReferenceQueue();

    /** Used to notify the tracker to thread to quit. */
    private boolean quit = false;
}
