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

/*
 * Memory.java
 * Created on June 8, 2004
 * @author  afh
 * @author  rry
 */

package org.cablelabs.debug;

/**
 * This is a proprietary class to allow an application to access statistical
 * information on dynamic memory usage.
 */
public class Memory
{
    /**
     * "handle" to mpeos layer memory statistics
     */
    private int handle;

    private int numColors;

    private static final int MEMSTAT_NAME = 1;

    private static final int MEMSTAT_ALLOCATED = 2;

    private static final int MEMSTAT_MAX_ALLOCATED = 3;

    /**
     * Creates a new instance of Memory, only called if programatic access to
     * memory statistics is required.
     */
    public Memory()
    {
        handle = getStats();
        if (handle != 0) numColors = getNumColorsImpl();
    }

    /**
     * Get the number of memory colors for which specific statistics are
     * available for.
     */
    public synchronized int getNumColors()
    {
        if (handle == 0) return 0;

        return numColors;
    }

    /**
     * Get the name of the specified color number.
     * 
     * @param colorIndex
     *            index of the requested color
     * 
     * @return String for the name of the memory color
     * 
     * @see getNumColors
     */
    public synchronized String getName(int colorIndex)
    {
        if (handle == 0) return null;

        return getStringStat(handle, colorIndex, MEMSTAT_NAME);
    }

    /**
     * Get the number of bytes currently allocated within the specified memory
     * color number.
     * 
     * @param colorIndex
     *            index of the requested color
     * 
     * @return Integer number of bytes of the specified memory color that are
     *         currently allocated.
     * 
     * @see getNumColors
     */
    public synchronized int getAllocated(int colorIndex)
    {
        if (handle == 0) return 0;

        return getIntStat(handle, colorIndex, MEMSTAT_ALLOCATED);
    }

    /**
     * Get the highest number of bytes that have ever been allocated from the
     * specified memory color number.
     * 
     * @param colorIndex
     *            index of the requested color
     * 
     * @return Integer number of bytes of the specified memory color that are
     *         currently allocated.
     * 
     * @see getNumColors
     */
    public synchronized int getMaxAllocated(int colorIndex)
    {
        if (handle == 0) return 0;

        return getIntStat(handle, colorIndex, MEMSTAT_MAX_ALLOCATED);
    }

    /**
     * Destroy any resources allocated for this instance of the object.
     */
    public synchronized void dispose()
    {
        if (handle != 0) disposeImpl(handle);

        handle = 0;
        numColors = 0;
    }

    protected void finalize()
    {
        dispose();
    }

    /**
     * Subtract another Memory object from this memory object. This could be
     * used for computing the difference in memory allocated across some span of
     * time. Both Memory objects must not have been disposed.
     * 
     * @param other
     *            reference to a Memory object to subtract from this one.
     * 
     * @return an array of integers, one entry per color, that is the result of
     *         subtracting each other Memory object's Allocated value from this
     *         Memory object's Allocated value.
     */
    public int[] subtractAllocated(Memory other)
    {
        if (numColors == 0 || numColors != other.getNumColors())
        {
            return null;
        }

        int[] ret = new int[numColors];

        for (int color = 0; color < numColors; ++color)
        {
            ret[color] = getAllocated(color) - other.getAllocated(color);
        }

        return ret;
    }

    /**
     * Get the total number of bytes in all the Allocated fields of all the
     * colors in this Memory object.
     * 
     * @return total number of bytes allocated for all the colors
     */
    public int totalAllocated()
    {
        if (handle == 0) return 0;

        int total = 0;

        for (int color = 0; color < numColors; ++color)
        {
            total += getAllocated(color);
        }

        return total;
    }

    /**
     * Trigger a dump of the current statistics, not to the console, all colors,
     * and without a label.
     */
    public static void dumpStatistics()
    {
        dumpStats(false, -1, null);
    }

    /**
     * Trigger a dump of the current statistics.
     * 
     * @param toConsole
     *            flags whether to dump the stats to the console or the default
     *            location.
     * @param color
     *            is the specific color stats to dump (-1 = all colors).
     * @param label
     *            String to use as a preface to the memory dump
     */
    public static void dumpStatistics(boolean toConsole, int color, String label)
    {
        dumpStats(toConsole, color, label);
    }

    // ******************* Private Properties ******************** //

    // ******************* Native Methods ******************** //

    /**
     * Native method interface for triggering a dump of the memory stats.
     * 
     * @param toConsole
     *            flags whether to dump the stats to the console or the default
     *            location
     * @param color
     *            is the specific color stats to dump, (-1 = all colors).
     * @param label
     *            is the label for the current statistical dump.
     */
    private static native void dumpStats(boolean toConsole, int color, String label);

    /**
     * Native method for getting the number of colors available. Will not be
     * called if the stats are not available.
     * 
     * @return integer number of colors for which stats are available
     */
    private native int getNumColorsImpl();

    /**
     * Native method for getting the current snapshot of the memory statistics
     * 
     * @return integer handle for the statistics, handle is passed to various
     *         other native methods for retrieving specific stats.
     */
    private native int getStats();

    /**
     * Native implementation of the dispose method. Frees the native memory
     * allocated to hold the snapshot of the memory statistics.
     * 
     * @param handle
     *            handle of the snapshot to be freed
     */
    private native void disposeImpl(int handle);

    /**
     * Get an integer statistic from the current snapshot of the memory
     * statistics.
     * 
     * @param handle
     *            handle on the memory statistics
     * @param colorIndex
     *            memory color for which to get the statistic
     * @param statIndex
     *            identifier for the stat requested
     * 
     * @return integer value for the statistic
     */
    private native int getIntStat(int handle, int colorIndex, int statIndex);

    /**
     * Get a String statistic from the current snapshot of the memory
     * statistics.
     * 
     * @param handle
     *            handle on the memory statistics
     * @param colorIndex
     *            memory color for which to get the statistic
     * @param statIndex
     *            identifier for the stat requested
     * 
     * @return String value for the statistic
     */
    private native String getStringStat(int handle, int colorIndex, int statIndex);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
