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

package org.cablelabs.impl.service.javatv.service;

import javax.tv.service.SIException;

import org.cablelabs.impl.service.RatingDimensionExt;
import org.cablelabs.impl.service.RatingDimensionHandle;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.string.MultiString;

/**
 * The <code>RatingDimension</code> implementation.
 * 
 * @author Todd Earles
 */
public class RatingDimensionImpl extends RatingDimensionExt
{
    /**
     * Construct a <code>RatingDimension</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param ratingDimensionHandle
     *            The rating dimension handle
     * @param dimensionName
     *            The dimension name
     * @param numberOfLevels
     *            The number of levels in this dimension
     * @param ratingLevelDescriptions
     *            Abbreviated and full rating level description for each rating
     *            level. Neither this argument nor any array entry within it are
     *            allowed to be null. The first dimension is the level number
     *            and must be at least as large as <code>numberOfLevels</code>.
     *            The second dimension must contain at least 2 entries where the
     *            first is the abbreviated name and the second is the full name.
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public RatingDimensionImpl(SICache siCache, RatingDimensionHandle ratingDimensionHandle, MultiString dimensionName,
            short numberOfLevels, MultiString[][] ratingLevelDescriptions, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || ratingDimensionHandle == null || dimensionName == null
                || ratingLevelDescriptions == null || ratingLevelDescriptions.length < numberOfLevels)
            throw new IllegalArgumentException();
        for (int i = 0; i < numberOfLevels; i++)
            if (ratingLevelDescriptions[i].length < 2 || ratingLevelDescriptions[i][0] == null
                    || ratingLevelDescriptions[i][1] == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "RatingDimensionImpl" + ratingDimensionHandle.getHandle() : uniqueID;
        data.ratingDimensionHandle = ratingDimensionHandle;
        data.dimensionName = dimensionName;
        data.numberOfLevels = numberOfLevels;
        data.ratingLevelDescriptions = ratingLevelDescriptions;
    }

    /**
     * Construct a <code>RatingDimension</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @param lsData
     *            The language specific variant data or null if none
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected RatingDimensionImpl(SICache siCache, Data data, LSData lsData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.lsData = lsData;
    }

    // The data objects
    private final Data data;

    private final LSData lsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from RatingDimensionExt
    public RatingDimensionHandle getRatingDimensionHandle()
    {
        return data.ratingDimensionHandle;
    }

    // Description copied from LanguageVariant
    public String getPreferredLanguage()
    {
        if (lsData == null)
            return null;
        else
            return lsData.language;
    }

    // Description copied from LanguageVariant
    public Object createLanguageSpecificVariant(String language)
    {
        LSData d = new LSData();
        d.language = language;
        return new RatingDimensionImpl(siCache, data, d);
    }

    // Description copied from RatingDimension
    public String getDimensionName()
    {
        String language = (lsData == null) ? null : lsData.language;
        return (data.dimensionName == null) ? null : data.dimensionName.getValue(language);
    }

    // Description copied from RatingDimensionExt
    public MultiString getDimensionNameAsMultiString()
    {
        return data.dimensionName;
    }

    // Description copied from RatingDimension
    public short getNumberOfLevels()
    {
        return data.numberOfLevels;
    }

    // Description copied from RatingDimension
    public String[] getRatingLevelDescription(short ratingLevel) throws SIException
    {
        String language = (lsData == null) ? null : lsData.language;
        return getRatingLevelDescription(ratingLevel, language);
    }

    // Description copied from RatingDimensionExt
    public MultiString[][] getDescriptionsAsMultiStringArray()
    {
        return data.ratingLevelDescriptions;
    }

    /**
     * Get language specific rating description.
     */
    private String[] getRatingLevelDescription(short ratingLevel, String language) throws SIException
    {
        if ((ratingLevel < 0) || (ratingLevel >= data.ratingLevelDescriptions.length))
        {
            throw new SIException("trying to access out-of-bounds rating level");
        }
        return new String[] { data.ratingLevelDescriptions[ratingLevel][0].getValue(language),
                data.ratingLevelDescriptions[ratingLevel][1].getValue(language) };
    }

    // Description copied from Object
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        RatingDimensionImpl o = (RatingDimensionImpl) obj;
        return getID().equals(o.getID())
                && getRatingDimensionHandle().getHandle() == o.getRatingDimensionHandle().getHandle()
                && getDimensionName().equals(o.getDimensionName()) && getNumberOfLevels() == o.getNumberOfLevels();
    }

    // Description copied from Object
    public int hashCode()
    {
        return data.dimensionName.hashCode() * 7 + data.ratingDimensionHandle.getHandle();
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", dimensionName=" + getDimensionName() + ", handle=" + getRatingDimensionHandle()
                + ", numberOfLevels=" + getNumberOfLevels() + ", ratingLevelDescriptions="
                + Arrays.toString(data.ratingLevelDescriptions) + ", lsData"
                + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]") + "]";
    }

    /** The SI cache */
    protected final SICache siCache;

    /**
     * The shared data for the invariant and all variants of this object
     */
    static class Data
    {
        /** Object ID for this SI object */
        public Object siObjectID;

        /** The rating dimension handle */
        public RatingDimensionHandle ratingDimensionHandle;

        /** The dimension name */
        public MultiString dimensionName;

        /** The number of levels in this dimension */
        public short numberOfLevels;

        /** Abbreviated and full rating level name */
        public MultiString[][] ratingLevelDescriptions;
    }

    /**
     * The language specific variant data
     */
    static class LSData
    {
        /** The preferred language */
        public String language;
    }
}
