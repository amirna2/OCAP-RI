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

package org.cablelabs.impl.snmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;


/**
 * This class is designed to map and OID to an object. This object allows a
 * caller to get OIDs sorted in lexicographical order.
 * <p>
 * The class is thread-safe and returns snap-shots of the map that can be
 * read/written w/o worry about a change to the map. The synchronization does
 * not protect against the
 * 
 * 
 * 
 * 
 * @author acossitt
 * 
 */
public class OIDMap
{
    private static final Logger log = Logger.getLogger(OIDMap.class);

    private TreeMap map = new TreeMap();

    /**
     * @code preventAmbiguiries
     * 
     *       Prevent the putting of OIDs that are part of a previous subtree.
     *       For example:
     *       <ol>
     *       <li>"1.2.3.4" in put into the map
     *       <li>"1.2.3.4.5" is attempted to be put into the map.
     *       {@link #put(OID, Object)} will fail with a
     *       {@link OIDAmbiguityException}.
     *       </ol>
     *       or
     *       <ol>
     *       <li>"1.2.3.4.5" in put into the map
     *       <li>"1.2.3.4" is attempted to be put into the map.
     *       {@link #put(OID, Object)} will fail with a
     *       {@link OIDAmbiguityException}.
     *       </ol>
     * 
     */
    private boolean preventAmbiguities = true;

    public OIDMap()
    {
        // preventAmbiguities is true
    }

    public OIDMap(boolean preventAmbiguities)
    {
        this.preventAmbiguities = preventAmbiguities;
    }

    /**
     * Returns true if this OIDMap has an ancestor of oid that is a parent of
     * oid (including grandparents, grand-grandparents, etc.).
     * 
     * @param oid
     * @return
     */
    public boolean hasParent(OID oid)
    {
        SortedMap ancestors = getAncestors(oid);

        // since some ancestors may not be ambiguous.
        // examples:
        // a greater branch -- 1.2.4 is not going to
        // cause an ambiguity for 1.2.3.4.
        // 1.2 creates an ambiguity for 1.2.3.4

        // if there is going to be a parent it will be first key (the most
        // closely related ancestor)
        if (ancestors.size() == 0)
        {
            return false;
        }

        OID match = (OID) ancestors.firstKey();

        if (match.getRelationship(oid) == OID.cParent)
        {
            return true;
        }

        return false;
    }

    /**
     * Get a tree that includes parents and other ancestors
     */
    public SortedMap getAncestors(OID oid)
    {
        synchronized (map)
        {
            // These OIDs are constructed just for the search. It does not mean
            // that they exist in the
            // OIDMap.
            OID root = oid.getRootAsOid();
            OID parent = oid.getParentAsOid();

            // parts of the map greater than and equal to parent to the OID less
            // than the next unrelated branch
            SortedMap sm = map.subMap(parent, root);

            // changes to SortedMap reflect back to the core data allowing
            // the caller to change things. Can't have that in a thread-safe
            // structure so "clone" and new tree and pass that back.
            TreeMap tm = new TreeMap(sm);

            printTreeContents(tm, "getAncestors");

            return tm;
        }
    }

    public boolean hasChildren(OID oid)
    {
        SortedMap descedents = getChildren(oid);

        if (descedents.size() > 0)
        {
            return true;
        }

        return false;
    }

    /**
     * The sorted map is lexicographical ordered by increasing value of OID with
     * the lowest value in the first element of the array.
     * 
     * @param oid
     * @return
     */
    public SortedMap getChildren(OID oid)
    {
        synchronized (map)
        {
            // OID nextLowestBranch = oid.getRootOfNextLowestBranchAsOid();]

            // These OIDs are constructed just for the search. It does not mean
            // that they exist in the
            // OIDMap.
            OID lowestChild = oid.getLowestChildAsOid();
            // OID nextSib = oid.getNextSibAsOid();

            // parts of the map greater than and equal to the lowestChild to the
            // OID less
            // than oid (the greatest child)
            SortedMap sm = map.subMap(lowestChild, oid);

            // changes to SortedMap reflect back to the core data allowing
            // the caller to change things. Can't have that in a thread-safe
            // structure so "clone" and new tree and pass that back.
            TreeMap tm = new TreeMap(sm);

            printTreeContents(tm, "getChildern");

            return tm;
        }
    }

    /**
     * The sorted map is lexicographical ordered by increasing value of OID with
     * the lowest value in the first element of the array.
     * 
     * @param oid
     * @return
     */
    public SortedMap getParentAndChildren(OID oid)
    {
        synchronized (map)
        {
            // These OIDs are constructed just for the search. It does not mean
            // that they exist in the
            // OIDMap.
            OID lowestChild = oid.getLowestChildAsOid();
            if (log.isDebugEnabled())
            {
                log.debug("lowestChild=" + lowestChild.getString());
            }
            OID nextSib = oid.getNextLowestSibAsOid();
            if (log.isDebugEnabled())
            {
                log.debug("nextSib=" + nextSib.getString());
            }

            printContents("source map for getParentAndChildren");

            // parts of the map greater than and equal to the lowestChild to the
            // OID less
            // than next sib (the parent)
            SortedMap sm = map.subMap(lowestChild, nextSib);

            // changes to SortedMap reflect back to the core data allowing
            // the caller to change things. Can't have that in a thread-safe
            // structure so "clone" and new tree and pass that back.
            TreeMap tm = new TreeMap(sm);

            printTreeContents(tm, "getParentAndChildren results");

            return tm;
        }
    }

    public void printContents(String title)
    {
        if (log.isDebugEnabled())
        {
            //logging guards added for consistency
            synchronized (map)
            {
                log.debug("printContents -- START: " + title);

                Set keySet = map.keySet();
                Iterator i = keySet.iterator();
                while (i.hasNext())
                {
                    OID key = (OID) i.next();
                    String s = key.toString();
                    log.debug("         oid=" + s);

                    Object value = map.get(key);
                    log.debug("         value=" + value);
                }

                log.debug("printContents -- END " + title + "\n");
            }
        }
    }

    private void printTreeContents(TreeMap tm, String title)
    {
        if (log.isDebugEnabled())
        {
            //logging guards added for consistency
            synchronized (tm)
        {
                if (log.isDebugEnabled())
        {
            log.debug("TreeMap Dump -- START: " + title);
        }

            Set keySet = tm.keySet();
            Iterator i = keySet.iterator();
            while (i.hasNext())
            {
                OID key = (OID) i.next();
                String s = key.toString();
                    if (log.isDebugEnabled())
                    {
                                    log.debug("         oid=" + s);
                    }
                }

                if (log.isDebugEnabled())
                {
                            log.debug("TreeMap Dump -- END " + title + "\n");
                }
    }
        }
    }

    public void put(OID key, Object value) throws IllegalArgumentException, OIDAmbiguityException
    {
        synchronized (map)
        {
            if (preventAmbiguities)
            {
                if (hasChildren(key) || hasParent(key) || map.containsKey(key))
                {
                    throw new OIDAmbiguityException();
                }
            }
            map.put(key, value);
        }
    }

    public Object get(OID key) throws IllegalArgumentException
    {
        synchronized (map)
        {
            return map.get(key);
        }
    }

    public void remove(OID key)
    {
        synchronized (map)
        {
            map.remove(key);
        }

    }

    public String[] getOIDs()
    {
        synchronized (map)
        {
            Set keySet = map.keySet();

            String[] oids = new String[keySet.size()];
            Iterator it = keySet.iterator();
            for (int i = 0; it.hasNext(); i++)
            {
                OID oidO = (OID) it.next();
                String oid = oidO.getString();
                oids[i] = oid;
            }
            return oids;
        }

    }

    public ArrayList getValues()
    {
        synchronized (map)
        {
            Collection values = map.values();
            return new ArrayList(values);
        }

    }

    /**
     * generate a sub-tree populated with all object registrations
     * in this OID map for the supplied oid and its subtree.
     *
     * @param oid oid as a string
     * @return map of all registered objects for this oid and it's sub-tree.
     */
    public SortedMap getRegiteredTree(String oid)
    {
        synchronized (map)
        {
            SortedMap sortedMap = getParentAndChildren(new OID(oid));
            if(sortedMap == null || sortedMap.isEmpty())
            {
                Object o = findParent(oid);
                if(o != null)
                {
                    sortedMap = new TreeMap();
                    sortedMap.put(new OID(oid), o);
                }
            }
            return sortedMap;
        }
    }

    /**
     * identify the registered entry in this OIDMap for the supplied oid
     * @param oid oid as a string
     * @return the object registered in this map responsible for the supplied oid
     */
    public Object getRegisteredObject(String oid)
    {
        synchronized (map)
        {
            Object o = get(new OID(oid));
            if(o == null)
            {
                // Find the lowest parent of this oid
                o = findParent(oid);
            }
            return o;
        }
    }

    private Object findParent(String oid)
    {
        Object o = null;
        String parent = oid;
        while(parent.lastIndexOf('.') != -1)
        {
            // Lop off the last SubID
            parent = parent.substring(0, parent.lastIndexOf('.'));
            o = get(new OID(parent));
            if(o != null)
            {
                break;
            }
        }
        return o;
    }
}
