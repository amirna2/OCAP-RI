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
 * QuadTreeNode.h: interface for the CQuadTreeNode class.
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

#ifndef _QUADTREENODE_H_
#define _QUADTREENODE_H_

// Include STL header files.
#include <set>
using namespace std;

// Include RI Emulator header files.
#include "ActiveRegion.h"

typedef std::set<ActiveRegion *> ActiveRegionSet;

/**
 * This class encapsulates a quad-tree node.
 */
class QuadTreeNode
{
public:

    /**
     * A constructor specifying the extent of the quadrant.
     * <p>
     * To create the root of the tree, call this constructor with <i>lowerLeftX = 0</i> and
     * <i>lowerLeftY = 0</i>.
     * </p>
     *
     * @param lowerLeftX The lower-left x position of the quadrant.
     * @param lowerLeftY The lower-left y position of the quadrant.
     * @param width The width of the quadrant.
     * @param height The height of the quadrant.
     * @param level An identifier for the level this quadrant occurs in the tree.
     * @param number An identifier of the quadrant.
     */
    QuadTreeNode(long lowerLeftX, long lowerLeftY, long width, long height,
            long level = 0, long number = 0);

    /**
     * The destructor.
     */
    virtual ~QuadTreeNode();

    /**
     * Subdivide the tree into quadrants.
     * <p>
     * This method may be called multiple times. Each time it is called the space with
     * be subdivided, adding another level to the tree's hierarchy.
     * </p>
     *
     * @return If the subdivision is successful, then <b>0</b> will be returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    long SubDivide();

    /**
     * Add an Active Region to the quadrant.
     *
     * @param object A pointer to an active region to add to the quad-tree.
     *
     * @return If the object is successfully added, then <b>0</b> will be returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    long AddObject(ActiveRegion *object);

    /**
     * Purge all regions from the tree.
     *
     * @return If the purge is successful, then <b>0</b> will be returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    long PurgeObjects();

    /**
     * Get the size of the quadrant.
     *
     * @param llx A pointer to an output parameter to receive the lower-left x position of the quadrant.
     * @param lly A pointer to an output parameter to receive the lower-left y position of the quadrant.
     * @param width A pointer to an output parameter to receive the width of the quadrant.
     * @param height A pointer to an output parameter to receive the height of the quadrant.
     *
     * @return If the size is successfully retrieved, then <b>0</b> will be returned.
     * Otherwise, <b>-1</b> will be returned.
     */
    long GetSize(long* llx, long* lly, long* width, long* height);

    /**
     * Get a collection of all Active Regions that may intersect the specified
     * Active Region, <i>object</i>.
     *
     * @param object A pointer to the Active Region to check the intersection of.
     * @param moving A flag indicating whether the the Active Region is moving.
     * @param excludeType Exclude this Active Region type from the collection of
     * potential intersections.
     * @param collidables The collection of potential intersecting Active Regions.
     *
     * @return If the potential Active Regions are successfully retrieved,
     * then <b>0</b> will be returned. Otherwise, <b>-1</b> will be returned.
     */
    long
            GetCollidableObjects(ActiveRegion *object, bool moving,
                    ActiveRegionType excludeType,
                    std::set<ActiveRegion *>& collidables);

    /**
     * Remove the specified Active Region from the tree.
     *
     * @param object The Active Region to remove.
     * @param usPreviousPos Use the previous position of the Active Region
     * to determine whether to remove it based on it's previous location.
     * @param expectedToFindObj A flag indicating whether we expect to find
     * the Active Region in the tree.
     *
     * @return If the Active Region is successfully removed,
     * then <b>0</b> will be returned. Otherwise, <b>-1</b> will be returned.
     */
    long RemoveObject(ActiveRegion *object, bool usePreviousPos,
            bool expectedToFindObj);

    /**
     * Update the location of the Active Region in the tree.
     * <p>
     * This method is used to reorganize the quad-tree if the position of the Active
     * Region has changed.
     * </p>
     *
     * @param object A pointer to the Active Region to update.
     */
    long UpdateObject(ActiveRegion *object);

private:

    // The lower-left x position of the quadrant.
    long m_lowerLeftX;
    // The lower-left y position of the quadrant.
    long m_lowerLeftY;
    // The width of the quadrant.
    long m_width;
    // The height of the quadrant.
    long m_height;

    // A level identifier.
    long m_ID;
    // The quadrant identifier.
    long m_number;

    // The list of sub nodes.
    QuadTreeNode** m_subNodes;

    // The collection of Active Regions at this level of the tree.
    //typedef std::set<ActiveRegion *> ActiveRegionSet;
    ActiveRegionSet m_regions;
};

#endif /* _QUADTREENODE_H_ */
