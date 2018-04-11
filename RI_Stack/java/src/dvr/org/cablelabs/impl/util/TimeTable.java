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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Enumeration;

import org.apache.log4j.Logger; //import org.apache.log4j.Logger;
//

/**
 * This class provides a simple TimeTable concept.
 * <p>
 * That is, a list of values with associated times and accessors for adding,
 * removing, and finding values based on a time.
 * <p>
 * The value type, time base, and units are left to the caller's discretion. If
 * the TimeTable is to be serialized, the associated values must be
 * Serializable.
 * <p>
 * <b>Note:</b> This class has been modified to be thread-safe and usable w/o
 * external synchronization. Internal synchronization is done using the
 * <code>m_synch</code> object, not the <code>TimeTable</code> object and
 * presently <code>m_synch</code> is not made visible to outside classes. If you
 * wish to have external synchronization it will not interfere with the internal
 * synchronization if you use the <code>TimeTable</code> object as your
 * synchronization object. No method is <code>synchronized</code>: All
 * synchronization is done using <code>synchronized</code> blocks.
 * <p>
 * <b>Note:</b> This class uses an array to improve serialization and JNI
 * performance. As such, it's best suited for in-order adding. Deleting elements
 * or inserting values out of order will have a much higher cost than inserting
 * in-order.
 * <p>
 * <b>Note:</b> The JNI layer has knowledge of the TimeTable internal data
 * structure. Changing this in Java will require changes to the native code.
 * <p>
 * <b>Note:</b> Presently, adding two values with the same time will cause the
 * first value to be lost. This may need to be revisited. TODO: make sure
 * behavior is correct.
 * <p>
 * <b>Note:</b> The search algorithm is not currently optimized - so searching
 * is in linear (O(n)) time to the TimeTable size. <b>NOTE: modifying a
 * TimeAssociatedElement once it is inside a TimeTable will break the thread
 * safety of the TimeTable class and may make the time order incorrect.</b>
 * 
 * 
 * @author Craig Pratt
 */
public class TimeTable extends Object implements Serializable
{
    private static final Logger log = Logger.getLogger(TimeTable.class.getName());

    // NOTE: Objects of this class are serializable. Changing the memvars
    // requires
    // great care to ensure compatibility when reading serialized objects made
    // with any prior version of this class. Today this means that changing
    // either the timeTable vector or TimeTableElement will break depersistence
    // of recording metadata.
    protected static final long serialVersionUID = -8311992255383298048L;

    private transient Object m_synch = new Object();

    protected int m_size;

    // We're using an array here to make this class more accessible via JNI
    protected TimeAssociatedElement[] m_elements;

    public static final int DEFAULT_INIT_CAPACITY = 10;

    private static final int GROWTH_INCREMENT = 10;

    private int m_initialCapacity = GROWTH_INCREMENT;

    /**
     * Instantiate an empty TimeTable with a given capacity.
     * 
     * @param initialCapacity
     *            The maximum number of elements that the TimeTable can hold.
     */
    public TimeTable(final int initialCapacity)
    {
        m_initialCapacity = initialCapacity;
        initializeTimeTable();
    }

    public TimeTable()
    {
        this(DEFAULT_INIT_CAPACITY);
    }

    /**
     * Used for cloning
     * 
     */
    private TimeTable(int size, TimeAssociatedElement[] elems)
    {
        m_size = size;
        m_elements = elems;
    }

    /**
     * Create a new m_elements with m_initialCapacity Elements
     * 
     */
    private void initializeTimeTable()
    {
        synchronized (m_synch)
        {
            m_elements = new TimeAssociatedElement[m_initialCapacity];
            m_size = 0;
        }
    }

    /**
     * Returns a subtable
     * 
     * @param startTime
     * @param duration
     * @param includePreceedingEntry
     * @return
     */
    public TimeTable subtableFromTimespan(final long startTime, final long duration,
            final boolean includePreceedingEntry)
    {
        TimeAssociatedElement preceedingEntry = null;
        final long endTime = startTime + duration;

        synchronized (m_synch)
        {
            // Trade-off: We'll allocate a tt of the same size to avoid
            // reallocation
            final TimeTable newtt = new TimeTable(this.m_size);

            for (int pos = 0; pos < this.m_size; pos++)
            {
                TimeAssociatedElement curEntry = this.m_elements[pos];
                long curtime = curEntry.time;
                if ((curtime >= startTime) && (curtime < endTime))
                {
                    if ((preceedingEntry != null) && includePreceedingEntry)
                    {
                        newtt.m_elements[newtt.m_size++] = preceedingEntry;
                        preceedingEntry = null; 
                        // The list is sorted, so this can't be made non-null after this point
                    }
                    newtt.m_elements[newtt.m_size++] = curEntry;
                }
                else if (curtime < startTime)
                {
                    preceedingEntry = curEntry;
                }
            } // END for

            // We may get through the whole list and not find an overlapping
            // entry - but we still need to satisfy the includePreceedingEntry
            // requirement
            if ((preceedingEntry != null) && includePreceedingEntry)
            {
                newtt.m_elements[newtt.m_size++] = preceedingEntry;
            }
            return newtt;
        }
    } // END subtableFromTimespan

    /**
     * Inserts the given TimeAssociatedElement. If an element with the same time is
     * already contained in the list, the element reference will be replaced. 
     * 
     * @param elem
     *            Element to insert
     */
    public void addElement(final TimeAssociatedElement elem)
    {
        int pos;
        boolean needToInsert = false;
        boolean willReplace = false;

        synchronized (m_synch)
        {
            for (pos = 0; pos < m_size; pos++)
            {
                // removed when I allowed inserts with same key
                // if (m_elements[pos].time == elem.time)
                // { // No need to insert - we'll replace
                // willReplace = true;
                // if(LOGGING)
                // {
                // // TODO: decide if replace is the right thing to do.
                // log.warn(" addElement in "+this+" replaced value at "+elem.time);
                // }
                //
                // break;
                // }
                if (m_elements[pos].time > elem.time)
                {
                    needToInsert = true;
                    break;
                }
            } // END for

            if (!willReplace && (m_elements.length == m_size))
            { // Need to grow the array
                TimeAssociatedElement[] newtte = new TimeAssociatedElement[m_elements.length + GROWTH_INCREMENT];
                // Copy [0..pos-1] to new array
                System.arraycopy(m_elements, 0, newtte, 0, pos);
                // Insert the new guy
                newtte[pos] = elem; // new TimeAssociatedElement(time,value);
                // Copy the rest
                System.arraycopy(m_elements, pos, newtte, pos + 1, m_size - pos);
                // And replace the old array
                m_elements = newtte;
                m_size++;
                return;
            }

            // Assert: m_size < m_elements.length

            if (needToInsert)
            {
                // Make room
                System.arraycopy(m_elements, pos, m_elements, pos + 1, m_size - pos);
            }

            // Assert: pos==m_size or we've bumped everything up

            // Insert the new guy (or overwrite existing)
            m_elements[pos] = elem; // new Element(time, value);
            if (!willReplace)
            {
                m_size++;
            }
        } // END synchronized
        return;
    } // END addElement

    /**
     * Remove element from the TimeTable. Uses reference equivilence to detect
     * match.
     * 
     * @param value
     *            TimeAssociatedElement elem to remove
     * @throws IllegalArgumentException
     *             if elem is not found
     */
    public void removeElement(final TimeAssociatedElement elem) throws IllegalArgumentException
    {
        int pos;

        synchronized (m_synch)
        {
            for (pos = 0; pos < m_size; pos++)
            {
                if (m_elements[pos] == elem)
                {
                    // Just move everything down a notch
                    System.arraycopy(m_elements, pos + 1, m_elements, pos, m_size - pos);
                    m_size--;
                    return;
                }
            } // END for
        } // END synchronized

        // We won't shrink the array currently
        throw new IllegalArgumentException(elem + " not found in TimeTable");
    } // END removeElement()

    public void removeAllElements()
    {
        synchronized (m_synch)
        {
            initializeTimeTable();
        }
    }

    /**
     * Remove all the elements of the subtable from this table. Elements must
     * pass an == equivilence.
     * 
     * @param subtable
     */
    public void removeSubtable(TimeTable subtable)
    {
        synchronized (m_synch)
        {
            // TODO: Remove from end may be more efficent.
            Enumeration subEnum = subtable.elements();
            while (subEnum.hasMoreElements())
            {
                TimeAssociatedElement elem = (TimeAssociatedElement) subEnum.nextElement();
                removeElement(elem);
            }
        }
    }

    /**
     * Get the TimeTableElement preceding the given time or null if there are no elements
     * preceding the given time or null if the TimeTable is empty.
     * 
     * <b>NOTE: modifying the returned TimeAssociatedElement will break the
     * thread safety of this class.</b>
     * 
     * @param time
     *            Requested time
     * @return TimeTableElement preceding the given time or null if there are no elements
     *         preceding the given time or null if the TimeTable is empty.
     */
    public TimeAssociatedElement getEntryBefore(final long time)
    {
        int pos;
        TimeAssociatedElement elem = null;

        synchronized (m_synch)
        {
            for (pos = 0; pos < this.m_size; pos++)
            {
                if (this.m_elements[pos].time < time)
                { 
                    elem = this.m_elements[pos];
                }
                else
                { // this.m_elements[pos].time >= time
                    break;
                }
            } // END for
            
            return elem;
        }
    } // END getEntryBefore()

    /**
     * Get the TimeAssociatedElement following the given time or
     * null if there are no elements after the given time or all
     * elements are earlier than the given time.
     * 
     * <b>NOTE: modifying the returned TimeAssociatedElement will break the
     * thread safety of this class.</b>
     * 
     * @param time
     *            Requested time
     * @return TimeTableElement immediately after the given time or null.
     */
    public TimeAssociatedElement getEntryAfter(final long time)
    {
        int pos;
        TimeAssociatedElement elem = null;

        synchronized (m_synch)
        {
            for (pos = this.m_size-1; pos >= 0; pos--)
            {
                if (this.m_elements[pos].time > time)
                {
                    elem = this.m_elements[pos];
                }
                else
                { // this.m_elements[pos].time <= time)
                    break;
                }
            } // END for
        }
        // Assert: time is larger than all elements
        return elem;
    } // END getEntryAfter()

    /**
     * Get the TimeTableElement equal to the given time or null if no entry
     * matches the given time.
     * 
     * <b>NOTE: modifying the returned TimeAssociatedElement will break the
     * thread safety of this class.</b>
     * 
     * @param time
     *            Requested time
     * @return TimeTableElement with the given time.
     */
    public TimeAssociatedElement getEntryAt(final long time)
    {
        int pos;
        synchronized (m_synch)
        {
            for (pos = 0; pos < this.m_size; pos++)
            {
                if (this.m_elements[pos].time == time)
                { // Last entry must have been <= time, if any
                    return this.m_elements[pos];
                }
            } // END for
        }
        // Assert: time is unequal to all elements
        return null;
    } // END getEntryAfter()

    /**
     * Get the first (earliest) entry in the TimeTable
     * 
     * @return First (earliest) TimeAssociatedElement in the TimeTable
     */
    public TimeAssociatedElement getFirstEntry()
    {
        return this.m_elements[0];
    }

    /**
     * Get the last (latest) entry in the TimeTable
     * 
     * @return Last (latest) TimeAssociatedElement in the TimeTable
     */
    public TimeAssociatedElement getLastEntry()
    {
        return this.m_elements[m_size - 1];
    }

    /**
     * Return an iteration over the TimeTableElements, in time order.
     * 
     * <b>NOTE: modifying any of the returned TimeAssociatedElement will break
     * the thread safety of this class.</b>
     * 
     * @return An Enumeration over the TimeTable.Elements in the TimeTable
     */
    public Enumeration elements()
    {
        final Enumeration elementEnumeration = new java.util.Enumeration()
        {
            int pos = 0;

            TimeTable tt = getThreadSafeCopy();

            public boolean hasMoreElements()
            {
                return (pos < tt.m_size);
            }

            public Object nextElement()
            {
                return (pos < tt.m_size) ? tt.m_elements[pos++] : null;
            }
        };
        return elementEnumeration;
    }

    /**
     * Return an iteration over the TimeTableElements, in reverse time order.
     *
     * <b>NOTE: modifying any of the returned TimeAssociatedElement will break
     * the thread safety of this class.</b>
     *
     * @return An Enumeration over the TimeTable.Elements in the TimeTable
     */
    public Enumeration reverseElements()
    {
        final Enumeration elementEnumeration = new java.util.Enumeration()
        {
            TimeTable tt = getThreadSafeCopy();

            int pos = tt.m_size > 0 ? tt.m_size - 1 : 0;

            public boolean hasMoreElements()
            {
                return (pos > 0);
            }

            public Object nextElement()
            {
                return (pos > 0 ) ? tt.m_elements[pos--] : null;
            }
        };
        return elementEnumeration;
    }

    public boolean isEmpty()
    {
        if (m_size == 0) return true;
        return false;
    }

    public int getSize()
    {
        return m_size;
    }

    /**
     * Merge a TimeTable with <code>this</code> <code>TimeTable</code>
     * 
     * @param tt
     *            The <code>TimeTable</code> that is merged into
     *            <code>this</code>. This function will take a snapshot of
     *            <code>tt</code> and merge it into <code>this</code>. The merge
     *            will use new <code>Elements</code> not referenced by the
     *            merged <code>TimeTable</code>
     */
    public void merge(TimeTable tt)
    {
        synchronized (m_synch)
        {
            Enumeration ttEnum = tt.elements();
            while (ttEnum.hasMoreElements())
            {
                TimeAssociatedElement elem = (TimeAssociatedElement) ttEnum.nextElement();
                // Add a new Element so changes to the element in the merged
                // table
                // do not affect this TimeTable.
                this.addElement(elem); // TimeAssociatedElement is immutable
            }
        }
    }

    /**
     * Gets a clone of the TimeTable, but does not clone the
     * TimeAssociatedElements (shallow clone) We are trading off complete thread
     * safety for efficiency and simplicity.
     * 
     * <b>NOTE: modifying any of the returned TimeAssociatedElement will break
     * the thread safety of this class.</b>
     * 
     * @return
     */
    public TimeTable getThreadSafeCopy()
    {
        synchronized (m_synch)
        {
            try
            {
                return (TimeTable) this.clone();
            }
            catch (CloneNotSupportedException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Adjust the times of all the elements in the TimeTable by the given factor
     * 
     * @param adjustmentFactor
     *            Amount of time to adjust the entries by
     */
    public void adjustEntryTimes(final long adjustmentFactor)
    {
        if (adjustmentFactor == 0)
        {
            return;
        }
        
        synchronized (m_synch)
        {
            Enumeration ttEnum = elements();
            while (ttEnum.hasMoreElements())
            {
                TimeAssociatedElement elem = (TimeAssociatedElement) ttEnum.nextElement();
                elem.time += adjustmentFactor;
            }
        }
    }
    
    //findbugs detected that this class does not implement Cloneable, which is correct.
    public Object clone() throws CloneNotSupportedException
    {
        return new TimeTable(m_size, (TimeAssociatedElement[]) m_elements.clone());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("TimeTable 0x");
        sb.append(Integer.toHexString(this.hashCode()));
        sb.append(":[");

        int pos = 0;

        synchronized (m_synch)
        {
            if (isEmpty())
            {
                sb.append("empty");
            }
            else
            {
                while (true)
                {
                    sb.append(this.m_elements[pos].toString());
                    if (++pos >= this.m_size)
                    {
                        break;
                    }
                    sb.append(", ");
                } // END for
            }
            sb.append(']');
            return sb.toString();
        }
    } // END toString()

    // private void writeObject(ObjectOutputStream out) throws IOException
    // {
    // out.defaultWriteObject();
    // }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        m_synch = new Object();
    }

} // END class TimeTable
