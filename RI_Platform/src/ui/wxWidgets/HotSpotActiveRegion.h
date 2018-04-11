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
 * HotSpotActiveRegion.h
 *
 *  Created on: Feb 23, 2009
 *      Author: Mark Millard
 */

#ifndef _HOTSPOTACTIVEREGION_H_
#define _HOTSPOTACTIVEREGION_H_

// Include RI Emulator header files.
#include "ImageMap.h"
#include "ActiveRegion.h"

class HotSpotActiveRegion: public ActiveRegion
{
public:

    /**
     * A constructor that establishes the (x,y) position.
     *
     * @param x The x position.
     * @param y The y position.
     * @param hotspot The associated Image Map hotspot.
     */
    HotSpotActiveRegion(double x, double y, HotSpot *hotspot);

    /**
     * The destructor.
     */
    virtual ~HotSpotActiveRegion();

    /**
     * Get the associated Image Map hotspot.
     *
     * @param x A pointer to the output parameter to retrieve the hotspot value.
     */
    void GetHotSpot(HotSpot **hotspot);

    /**
     * Get the extent of the region.
     *
     * @param width An output parameter for the width of the region.
     * @param height An output parameter for the height of the region.
     *
     * @return <b>0</b> will be returned if the extent is successfully returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    virtual long GetExtent(double *width, double *height);

    /**
     * Get the position of the region.
     *
     * @param xPos An output parameter for the x position of the region.
     * @param yPos An output parameter for the y position of the region.
     *
     * @return <b>0</b> will be returned if the position is successfully returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    virtual long GetPosition(double *xPos, double *yPos);

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
    virtual long GetPrevPosition(double *xPos, double *yPos);

    /**
     * Set the hotspot's position.
     *
     * @param x The x position.
     * @param y The y position.
     *
     * @return <b>0</b> will be returned if the position is successfully set.
     * Otherwise, <b>-1</b> will be returned.
     */
    long SetPosition(double x, double y);

private:

    // The associated Image Map hotspot.
    HotSpot *m_hotspot;
    // The x position.
    double m_x;
    // The y position.
    double m_y;
    // The previous x position.
    double m_prevX;
    // The previous y position.
    double m_prevY;
};

#endif /* _HOTSPOTACTIVEREGION_H_ */
