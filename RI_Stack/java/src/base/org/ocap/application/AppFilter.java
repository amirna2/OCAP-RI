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

package org.ocap.application;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabaseFilter;

/**
 * <em>AppFilter</em> provides a means of filtering AppIDs. As a subclass of
 * {@link AppsDatabaseFilter}, the method {@link #accept} makes a
 * <code>true</code>/<code>false</code> decision based on an AppID.
 * 
 * <p>
 * An AppFilter contains a list of zero or more {@link AppPattern}s. Each
 * AppPattern has the attributes: <em>pattern</em>, <em>action</em>, and
 * <em>priority</em>. <em>pattern</em> specifies a group of AppIDs with a pair
 * of ranges for organization ID and application ID. <em>action</em> specifies
 * an action assigned to the AppID group; either {@link AppPattern#ALLOW},
 * {@link AppPattern#DENY}, or {@link AppPattern#ASK}. <em>priority</em>
 * specifies this AppPattern's position in the search order: the biggest number
 * comes first. Applications can insert an AppPattern anywhere in the search
 * order by using the priority attribute effectively (<code>AppFilter.add</code>
 * ). When two or more AppPatterns in an AppFilter have the same priority, the
 * search order among them is undefined. It is not recommendable to use
 * AppPatterns that have the same priority but different actions.
 * 
 * <p>
 * When <code>accept</code> is called, the given AppID is compared to the AppID
 * group of each AppPattern in the search order until a match is found. Then, it
 * returns <code>true</code> or <code>false</code> if the action of matching
 * AppPattern is <code>ALLOW</code> or <code>DENY</code> respectively. If no
 * match is found, <code>accept</code> returns <code>true</code>.
 * 
 * <p>
 * If the action of matching AppPattern is <code>ASK</code>, then AppFilter
 * calls <code>AppFilterHandler.accept</code> for the final decision; the
 * matching AppPattern is handed over to this method. Applications can specify
 * the <code>AppFilterHandler</code> with <code>AppFilter.setAskHandler</code>.
 * If no AppFilterHandler is set, AppFilter returns <code>true</code>.
 * 
 * 
 * <p>
 * AppPatterns can have an expiration time and MSO-private information (
 * <em>expirationTime</em> and <em>info</em>). <code>accept</code> and
 * <code>getAppPatterns</code> methods ignore AppPatterns that have expired. The
 * implementation may delete expired AppPatterns from AppFilter.
 * 
 * <p>
 * <b>Example:</b>
 * 
 * <blockquote>
 * 
 * <pre>
 * import org.ocap.application.*;
 * import org.dvb.application.AppID;
 * 
 * AppManagerProxy am = ...;
 * AppPattern[] patterns = {
 *     &#47;* note that search order is dictated by "priority" *&#47;
 *     new AppPattern("10-5f:1-ff", AppPattern.ALLOW, 40),     // #3
 *     new AppPattern("30:2c-34", AppPattern.ALLOW, 100),      // #1
 *     new AppPattern("20-40", AppPattern.DENY, 80),           // #2
 * };
 * AppFilter af = new AppFilter(patterns);
 * 
 * &#47;* false - matches "20-40" *&#47;
 * boolean badOne = af.accept(new AppID(0x30, 0x10));
 * 
 * &#47;* true - matches "30:2c-34" *&#47;
 * boolean goodOne = af.accept(new AppID(0x30, 0x30));
 * 
 * &#47;* will be the second entry: priority between 100 and 80 *&#47;
 * af.add(new AppPattern("40-4f:1000-1fff", DENY, 90));
 * 
 * &#47;* register af with the system *&#47;
 * am.setAppFilter(af);
 * </pre>
 * 
 * </blockquote>
 * 
 * @see AppPattern
 * @see AppFilterHandler
 * @see AppManagerProxy
 * @see org.dvb.application.AppID
 * @see org.dvb.application.AppsDatabaseFilter
 * 
 * @author Aaron Kamienski
 */
public class AppFilter extends AppsDatabaseFilter
{

    /**
     * Constructs an empty AppFilter.
     */
    public AppFilter()
    {
        // Empty
    }

    /**
     * Constructs an AppFilter with initial AppPatterns.
     * 
     * @param patterns
     *            AppPatterns to constitute an AppFilter.
     */
    public AppFilter(AppPattern[] patterns)
    {
        for (int i = 0; i < patterns.length; ++i)
            add(patterns[i]);
    }

    /**
     * Returns the AppPatterns in this AppFilter.
     * 
     * @return the enumeration of AppPatterns. When this AppFilter has no
     *         AppPattern, this method returns an empty Enumeration, not
     *         <code>null</code>.
     */
    public Enumeration getAppPatterns()
    {
        // Note: this considers whether patterns have expired at the time
        // getAppPatterns() is called, as opposed to when
        // Enumeration.hasNextElement()
        // is called. The spec is silent on this question, however, I think that
        // this is the correct time.

        // This version checks for expired entries up-front and removes any
        // from later consideration immediately.
        if (true) // Preferred for now as it is simpler
        {
            synchronized (patterns)
            {
                // Remove any expired patterns
                for (int i = 0; i < patterns.size(); ++i)
                {
                    AppPattern pattern = (AppPattern) patterns.elementAt(i);
                    if (pattern.isExpired()) patterns.removeElementAt(i--);
                }
                // Return enumeration
                return patterns.elements();
            }
        }
        // This version checks for expired entries lazily.
        // However, it does not remove entries from later consideration.
        else
        {
            return new Enumeration()
            {
                private Date time = new Date();

                private Enumeration e = patterns.elements();

                private Object o = next();

                private Object next()
                {
                    while (e.hasMoreElements())
                    {
                        AppPattern p = (AppPattern) e.nextElement();
                        Date exp = p.getExpirationTime();
                        if (exp == null || !exp.before(time)) return p;
                    }
                    return null;
                }

                public boolean hasMoreElements()
                {
                    return o != null;
                }

                public Object nextElement()
                {
                    Object tmp = o;
                    if (tmp == null) throw new NoSuchElementException();
                    o = next();
                    return tmp;
                }
            };
        }
    }

    /**
     * Returns whether this AppFilter accepts the given AppID.
     * 
     * @param appID
     *            an AppID to test.
     * 
     * @return <code>true</code> if <code>appID</code> passes this filter.
     */
    public boolean accept(AppID appID)
    {
        synchronized (patterns)
        {
            // Iterate over sorted vector
            int size = patterns.size();
            for (int i = 0; i < size; ++i)
            {
                AppPattern pattern = (AppPattern) patterns.elementAt(i);

                switch (pattern.match(appID))
                {
                    case AppPattern.ALLOW:
                        return true;
                    case AppPattern.DENY:
                        return false;
                    case AppPattern.ASK:
                        try
                        {
                            return (askHandler != null) ? askHandler.accept(appID, pattern) : true;
                        }
                        catch (Throwable t)
                        {
                            // In case the handler crashes...
                            return true;
                        }
                    case AppPattern.NO_MATCH_EXPIRED:
                        --size;
                        patterns.removeElementAt(i--);
                    case AppPattern.NO_MATCH:
                        break;
                }
            }
        }
        return true;
    }

    /**
     * Adds an AppPattern to this AppFilter.
     * 
     * @param pattern
     *            the AppPattern to add
     */
    public void add(AppPattern pattern)
    {
        if (pattern == null) throw new NullPointerException("null patterns not accepted");

        // Skip altogether if already expired
        if (pattern.isExpired()) return;

        synchronized (patterns)
        {
            int idx = Collections.binarySearch(patterns, pattern, COMPARATOR);
            if (idx < 0) // Not found
                idx = -idx - 1;
            else
                patterns.removeElementAt(idx); // Found; replace equivalent

            patterns.insertElementAt(pattern, idx);
        }
    }

    /**
     * Removes an AppPattern that equals to <code>pattern</code> in this
     * AppFilter. If this AppFilter does not contain <code>pattern</code>, it is
     * unchanged.
     * 
     * @param pattern
     *            the AppPattern to remove.
     * 
     * @return <code>true</code> if the AppFilter contained the specified
     *         AppPattern.
     * 
     * @see AppPattern#equals
     */
    public boolean remove(AppPattern pattern)
    {
        return patterns.removeElement(pattern);
    }

    /**
     * Sets the handler to call when <code>accept</code> hits an AppPatterns
     * with action {@link AppPattern#ASK}.
     * 
     * <p>
     * If a handler is already registered with this AppFilter, the new handler
     * replaces it.
     * 
     * @param handler
     *            the handler to set.
     */
    public void setAskHandler(AppFilterHandler handler)
    {
        askHandler = handler;
    }

    /**
     * Set of patterns.
     */
    private Vector patterns = new Vector();

    /**
     * The askHandler.
     */
    private AppFilterHandler askHandler;

    /**
     * Comparator used for sorting.
     */
    private static Comparator COMPARATOR = new Comparator()
    {
        // Note: ordering of params is reversed to ensure descending-order sort
        public int compare(Object o2, Object o1)
        {
            int cmp = 0;
            // If equals, then equals
            if (o1.equals(o2)) return cmp;
            /*
             * Otherwise, sort by priority, then action, then pattern (Spec
             * allows for any sort order for equal priorities, we only perform
             * the additional comparisons so that we have a deterministic
             * sorting.)
             */
            AppPattern p1 = (AppPattern) o1;
            AppPattern p2 = (AppPattern) o2;
            if ((cmp = p1.getPriority() - p2.getPriority()) != 0 || (cmp = p1.getAction() - p2.getAction()) != 0)
                return cmp;

            // these two strings should NEVER be equal
            // (if they were, then equals should've returned true)
            return p1.getAppIDPattern().compareTo(p2.getAppIDPattern());
        }
    };
}
