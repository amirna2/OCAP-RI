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

package javax.tv.service.guide;

import javax.tv.service.SIException;

/**
 * ContentRatingAdvisory indicates, for a given program event, ratings for any
 * or all of the rating dimensions defined in the content rating system for the
 * local rating region. A program event without a content advisory indicates
 * that the rating value for any rating dimension is zero. The absence of
 * ratings for a specific dimension is equivalent to having a zero-valued rating
 * for such a dimension. The absence of ratings for a specific region implies
 * the absence of ratings for all the dimensions in the region.
 * <P>
 * 
 * For example, this information may be obtained in the ATSC Content Advisory
 * Descriptor or the DVB Parental Rating Descriptor. Note that the DVB rating
 * system is based on age only. It can be easily mapped to this rating system as
 * one of the dimensions.
 * 
 * @see javax.tv.service.guide.ProgramEvent
 */
public interface ContentRatingAdvisory
{

    /**
     * Returns a list of names of all dimensions in this rating region by which
     * the <code>ProgramEvent</code> is rated.
     * 
     * @return An array of strings representing all rated dimensions in this
     *         rating region for the <code>ProgramEvent</code>.
     * 
     * @see javax.tv.service.RatingDimension
     */
    public abstract String[] getDimensionNames();

    /**
     * Returns a number representing the rating level in the specified
     * <code>RatingDimension</code> associated with this rating region for the
     * related <code>ProgramEvent</code>.
     * 
     * @param dimensionName
     *            The name of the <code>RatingDimension</code> for which to
     *            obtain the rating level.
     * 
     * @return A number representing the rating level. The meaning is dependent
     *         on the associated rating dimension.
     * 
     * @throws SIException
     *             If <code>dimensionName</code> is not a valid name of a
     *             <code>RatingDimension</code> for the ProgramEvent.
     * 
     * @see javax.tv.service.RatingDimension#getDimensionName
     */
    public abstract short getRatingLevel(String dimensionName) throws SIException;

    /**
     * Returns the rating level display string for the specified dimension. The
     * string is identical to
     * <code>d.getRatingLevelDescription(getRatingLevel(dimensionName))[1]</code>
     * , where <code>d</code> is the <code>RatingDimension</code> obtained by
     * <code>javax.tv.service.SIManager.getRatingDimension(dimensionName)</code>
     * .
     * 
     * @param dimensionName
     *            The name of the <code>RatingDimension</code> for which to
     *            obtain the rating level text.
     * 
     * @return A string representing the textual value of this rating level.
     * 
     * @throws SIException
     *             If dimensionName is not a valid <code>RatingDimension</code>
     *             name for the <code>ProgramEvent</code>.
     * 
     * @see javax.tv.service.RatingDimension#getDimensionName
     * @see javax.tv.service.RatingDimension#getRatingLevelDescription
     */
    public abstract String getRatingLevelText(String dimensionName) throws SIException;

    /**
     * Provides a single string representing textual rating values for all
     * dimensions in which the program event is rated. The result will be a
     * representation of the strings obtained via
     * <code>d.getRatingLevelDescription(getRatingLevel(d.getDimensionName()))[0]</code>
     * , for all dimensions <code>d</code> obtained through
     * <code>javax.tv.service.SIManager.getRatingDimension(n)</code>, for all
     * dimension names <code>n</code> obtained from
     * <code>getDimensionNames()</code>.
     * 
     * @return A string representing the rating level values for all dimensions
     *         in which this program event is rated. The format of the string
     *         may be implementation-specific.
     * 
     * @see #getDimensionNames
     * @see javax.tv.service.RatingDimension#getRatingLevelDescription
     */
    public String getDisplayText();

    /**
     * Compares the current rating value with the system rating ceiling. The
     * rating ceiling is set in a system-dependent manner. Content that exceeds
     * the rating ceiling cannot be displayed.
     * 
     * @return <code>true</code> if the rating exceeds the current system rating
     *         ceiling; <code>false</code> otherwise.
     */
    public boolean exceeds();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
