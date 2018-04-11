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

package org.cablelabs.impl.manager.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

/**
 * SI cache implementation for SI snapshots.
 * 
 * @see SIManagerSnapshot
 * @see SIDatabaseSnapshot
 * 
 * @author ToddEarles
 */
public class SISnapshotCache extends SICacheImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SISnapshotCache.class);

    private boolean detailedLoggingOn = false;

    /**
     * Construct a snapshot cache and snapshot database and point them at each
     * other.
     */
    public SISnapshotCache()
    {
        super();

        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " Constructor called");
            }
        }

        // Create the hash tables used to snapshot the SI objects
        // WeakHashMaps are not used since we want the objects stored to stay
        // around.
        // Though it is a little confusing in how it is named, the snapshot is
        // not a cache since it is
        // not backed up by anything.
        ratingDimensionCache = Collections.synchronizedMap(new HashMap(10));
        transportCache = Collections.synchronizedMap(new HashMap(5));
        networkCache = Collections.synchronizedMap(new HashMap(5));
        transportStreamCache = Collections.synchronizedMap(new HashMap());
        serviceCache = Collections.synchronizedMap(new HashMap());
        serviceDetailsCache = Collections.synchronizedMap(new HashMap());
        serviceDescriptionCache = Collections.synchronizedMap(new HashMap());
        serviceComponentCache = Collections.synchronizedMap(new LinkedHashMap());
        patCache = Collections.synchronizedMap(new HashMap());
        pmtCache = Collections.synchronizedMap(new HashMap());

        // Construct a snapshot database and set it. don't use setSIDatabase
        // since it sets up
        // listeners, etc.
        siDatabase = new SISnapshotDatabase();

        // Point the cache and database at each other
        siDatabase.setSICache(this);
    }

    /**
     * Initialze with cache flushing turned off. Objects in this snapshot cache
     * should never be flushed.
     */
    protected void initialize()
    {
        if (detailedLoggingOn)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " initialize() called");
            }
        }

        // Do not flush entries from the cache
        flushInterval = 0;

        // don't call superclass initialize since that sets up a whole bunch of
        // timers
        // that aren't needed and which keep the snapshot in memory long after
        // it is no longer used.

    }

    protected SIReference getNewSIReference(Object siObject)
    {
        return new HardSIReference(siObject);
    }

    private class HardSIReference implements SIReference
    {
        private Object referent = null;

        public HardSIReference(Object referent)
        {
            this.referent = referent;
        }

        public Object get()
        {
            return this.referent;
        }

        /*
         * (non-Javadoc)
         * 
         * Shouldn't be called
         * 
         * @see org.cablelabs.impl.manager.service.SIReference#getLastAccess()
         */
        public long getLastAccess()
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " getLastAccess() called (not expected)");
            }
            return System.currentTimeMillis(); // never get rid of this object
                                               // because it has become "old"
        }

        public void updateLastAccess()
        {
            // do nothing
        }
    }
}
