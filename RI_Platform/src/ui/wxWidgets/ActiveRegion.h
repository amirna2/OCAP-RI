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
 * ActiveRegion.h
 *
 *  Created on: Feb 20, 2009
 *      Author: Mark Millard
 */

#ifndef _ACTIVEREGION_H_
#define _ACTIVEREGION_H_

typedef enum
{
    ACTIVE_REGION_NULL = 0x00000000,
    ACTIVE_REGION_POINT = 0x00000001,
    ACTIVE_REGION_HOTSPOT = 0x00000002
} ActiveRegionType;

/**
 * This abstract class encapsulates an active region for hotspots.
 */
class ActiveRegion
{
public:

    /**
     * The destructor.
     */
    virtual ~ActiveRegion()
    {
    }

    /**
     * Get the Action Region type.
     *
     * @param type An output parameter for the action region type.
     *
     * @return Always returns success, <b>0</b>.
     */
    long GetActiveRegionType(ActiveRegionType *type)
    {
        *type = m_type;
        return 0;
    }

    /**
     * Get the extent of the region.
     *
     * @param width An output parameter for the width of the region.
     * @param height An output parameter for the height of the region.
     *
     * @return <b>0</b> will be returned if the extent is successfully returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    virtual long GetExtent(double *width, double *height) = 0;

    /**
     * Get the position of the region.
     *
     * @param xPos An output parameter for the x position of the region.
     * @param yPos An output parameter for the y position of the region.
     *
     * @return <b>0</b> will be returned if the position is successfully returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    virtual long GetPosition(double *xPos, double *yPos) = 0;

    /**
     * Get the previous position of the region.
     * <p>
     * This method is used if the region is moving.
     * </p>
     *
     * @param xPos An output parameter for the previous x position of the region.
     * @param yPos An output parameter for the previous y position of the region.
     *
     * @return <b>0</b> will be returned if the position is successfully returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    virtual long GetPrevPosition(double *xPos, double *yPos) = 0;

protected:

    /** The type of Active Region. */
    ActiveRegionType m_type;
};

#endif /* _ACTIVEREGION_H_ */
