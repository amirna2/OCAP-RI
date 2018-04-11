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
 * QuadTreeNode.cpp: implementation for the QuadTreeNode class.
 *
 *  Created on: Feb 19, 2009
 *      Author: Mark Millard
 */

// Include the system header files.
#include <assert.h>
#include <glib.h>

// Include RI Emulator header files.
#include "QuadTreeNode.h"

#define round(x) ((x) >= 0 ? (long)((x) + 0.5) : (long)((x) - 0.5))

QuadTreeNode::QuadTreeNode(long lowerLeftX, long lowerLeftY, long width,
        long height, long level, long number) :
    m_lowerLeftX(lowerLeftX), m_lowerLeftY(lowerLeftY), m_width(width),
            m_height(height), m_ID(level + 1), m_number(number), m_subNodes(
                    NULL)
{
    // Do nothing extra.
}

QuadTreeNode::~QuadTreeNode()
{
    if (!m_subNodes)
    {
        ActiveRegionSet::iterator it = m_regions.begin();
        while (it != m_regions.end())
        {
            //(*it)->Release();
            it++;
        }
        m_regions.clear();
    }
    else
    {
        for (int i = 0; i < 4; i++)
        {
            delete m_subNodes[i];
        }
        g_free( m_subNodes);
        assert(m_regions.empty());
    }
}

long QuadTreeNode::SubDivide()
{
    long result = 0;
    if (!m_subNodes)
    {
        // w is new width for subnodes (half of current width), h similarly.
        long w = m_width / 2;
        long h = m_height / 2;
        // Shorter variable names for current lower x,y position.
        long llx = m_lowerLeftX;
        long lly = m_lowerLeftY;

        // node 0: upper left quadrant (NW)
        // node 1: upper right quadrant (NE)
        // node 2: lower right quadrant (SE)
        // node 3: lower left quaderant (SW)
        m_subNodes = (QuadTreeNode **) g_try_malloc(4 * sizeof(QuadTreeNode *));
        if (m_subNodes != NULL)
        {
            m_subNodes[0] = new QuadTreeNode(llx, lly + h, w, h, m_ID, 1);
            m_subNodes[1] = new QuadTreeNode(llx + w, lly + h, w, h, m_ID, 2);
            m_subNodes[2] = new QuadTreeNode(llx + w, lly, w, h, m_ID, 3);
            m_subNodes[3] = new QuadTreeNode(llx, lly, w, h, m_ID, 4);
        }
    }
    else
    {
        for (int i = 0; i < 4 && (result == 0); i++)
        {
            result = m_subNodes[i]->SubDivide();
        }
    }
    return result;
}

long QuadTreeNode::AddObject(ActiveRegion* object)
{
    long result = 0;
    // If this node has subnodes, figure out which subnode this object belongs
    // in and add it to him; otherwise, just add it to our list.
    if (!m_subNodes)
    {
        // No subnodes, this is a leaf node, so add it to the list.
        std::pair<ActiveRegionSet::iterator, bool> newInsertion;
        newInsertion = m_regions.insert(object);
        if (newInsertion.second)
        {
            // Successful insertion. Don't do anything for now; but one
            // could add reference counting. Only add ref if this object is not already in our set,
            // otherwise we will over ref count it.
            //object->AddRef();
        }
        else
        {
            // They should never add the same object to us twice.
            assert(false);
        }
    }
    else
    {
        // The object may need to go in more than one sub node if it
        // spans multiple quadrants.
        int collisionArray[4] =
        { -1, -1, -1, -1 };
        int collisionIndex = 0;

        long lowerLeftX = 0;
        long lowerLeftY = 0;
        long width = 0;
        long height = 0;
        double e_width, e_height, cx, cy;
        result = object->GetExtent(&e_width, &e_height);
        assert(result == 0);
        result = object->GetPosition(&cx, &cy);
        lowerLeftX = round(cx) - round(e_width / 2);
        lowerLeftY = round(cy) - round(e_height / 2);
        width = round(e_width);
        height = round(e_height);
        long llx, lly, w, h;
        for (int i = 0; i < 4; i++)
        {
            result = m_subNodes[i]->GetSize(&llx, &lly, &w, &h);
            assert(result == 0);
            int left1, left2;
            int right1, right2;
            int top1, top2;
            int bottom1, bottom2;

            left1 = lowerLeftX;
            left2 = llx;
            right1 = lowerLeftX + width;
            right2 = llx + w;
            bottom1 = lowerLeftY;
            bottom2 = lly;
            top1 = lowerLeftY + height;
            top2 = lly + h;
            if (bottom1 > top2 || top1 < bottom2 || right1 < left2 || left1
                    > right2)
            {
                continue;
            }
            else
            {
                collisionArray[collisionIndex++] = i;
            }
        }
        for (int j = 0; j < collisionIndex; j++)
        {
            (void) m_subNodes[collisionArray[j]]->AddObject(object);
        }
    }
    return result;
}

long QuadTreeNode::PurgeObjects()
{
    long result = 0;
    if (!m_subNodes)
    {
        ActiveRegionSet::iterator it = m_regions.begin();
        while (it != m_regions.end())
        {
            //(*it)->Release();
            it++;
        }
        m_regions.clear();
    }
    else
    {
        for (int i = 0; i < 4; i++)
        {
            result = m_subNodes[i]->PurgeObjects();
            assert(result == 0);
        }
        assert(m_regions.empty());
    }
    return result;
}

long QuadTreeNode::GetSize(long* llx, long* lly, long* width, long* height)
{
    *llx = m_lowerLeftX;
    *lly = m_lowerLeftY;
    *width = m_width;
    *height = m_height;
    return 0;
}

// If moving is true, caller should have moved the object forward a time step before
// calling this function.

long QuadTreeNode::GetCollidableObjects(ActiveRegion *object, bool moving,
        ActiveRegionType excludeType, std::set<ActiveRegion *>& collidables)
{
    long result = 0;
    // If this node has subnodes, figure out which subnode this object belongs
    // in and add it to him; otherwise, just add it to our list.
    if (!m_subNodes)
    {
        // No subnodes, this is a leaf node, so return entire list.
        ActiveRegionSet::iterator it = m_regions.begin();
        while (it != m_regions.end())
        {
            // Exclude the actual object they are passing in, since they want objects
            // other than this object in the area.
            if (*it != object)
            {
                // Also if they want to exclude other active regions, then
                // exclude those.
                ActiveRegionType objType;
                result = (*it)->GetActiveRegionType(&objType);
                assert(result == 0);
                if (!(objType == excludeType))
                {
                    (void) collidables.insert(*it);
                }
            }
            it++;
        }
    }
    else
    {
        // The object may need to go in more than one sub node if it
        // spans multiple quadrants.
        double e_width = 0, e_height = 0, cx = 0, cy = 0;
        result = object->GetExtent(&e_width, &e_height);
        assert(result == 0);
        std::set<int> collisionSave;
        // If the region is moving, then look at both the region's previous position
        // and the region's current position, otherwise just look at the current
        // position.
        int times2loop = moving ? 2 : 1;
        for (int k = 0; k < times2loop; k++)
        {
            if (k == 0)
            {
                result = object->GetPosition(&cx, &cy);
            }
            else if (k == 1)
            {
                result = object->GetPrevPosition(&cx, &cy);
            }
            else
            {
                assert(false);
            }
            assert(result == 0);

            long lowerLeftX = round(cx) - round(e_width / 2);
            long lowerLeftY = round(cy) - round(e_height / 2);
            long width = round(e_width);
            long height = round(e_height);

            long llx, lly, w, h;
            int collisionArray[4] =
            { -1, -1, -1, -1 };
            int collisionIndex = 0;

            for (int i = 0; i < 4; i++)
            {
                result = m_subNodes[i]->GetSize(&llx, &lly, &w, &h);
                assert(result == 0);
                int left1, left2;
                int right1, right2;
                int top1, top2;
                int bottom1, bottom2;

                left1 = lowerLeftX;
                left2 = llx;
                right1 = lowerLeftX + width;
                right2 = llx + w;
                bottom1 = lowerLeftY;
                bottom2 = lly;
                top1 = lowerLeftY + height;
                top2 = lly + h;
                if (bottom1 > top2 || top1 < bottom2 || right1 < left2 || left1
                        > right2)
                {
                    continue;
                }
                else
                {
                    collisionArray[collisionIndex++] = i;
                }
            }
            if (k == 0)
            {
                for (int j = 0; j < collisionIndex; j++)
                {
                    (void) collisionSave.insert(collisionArray[j]);
                }
            }
            for (int j = 0; j < collisionIndex; j++)
            {
                if (k == 1)
                {
                    if (collisionSave.find(collisionArray[j])
                            != collisionSave.end())
                    {
                        continue;
                    }
                }
                ActiveRegionSet subNodeCollidables;
                (void) m_subNodes[collisionArray[j]]->GetCollidableObjects(
                        object, moving, excludeType, subNodeCollidables);
                ActiveRegionSet::iterator it = subNodeCollidables.begin();
                while (it != subNodeCollidables.end())
                {
                    (void) collidables.insert(*it);
                    it++;
                }
            }
        }
    }
    return result;
}

long QuadTreeNode::RemoveObject(ActiveRegion *object, bool usePreviousPos,
        bool expectedToFindObj)
{
    long result = 0;
    // If this node has subnodes, figure out which subnode this object belongs
    // in and remove it from him; otherwise, just remove it to our list.
    if (!m_subNodes)
    {
        // No subnodes, this is a leaf node, so remove it.
        ActiveRegionSet::iterator it = m_regions.find(object);
        if (it != m_regions.end())
        {
            //(*it)->Release();
            m_regions.erase(it);
            if (!expectedToFindObj)
            {
                assert(false);
            }
        }
        else
        {
            if (expectedToFindObj)
            {
                // Why were we told to remove an object not in our set?
                assert(false);
            }
        }
    }
    else
    {
        // The object may need to be removed from more than one sub node if it
        // spans multiple quadrants.
        int collisionArray[4] =
        { -1, -1, -1, -1 };
        int collisionIndex = 0;
#ifdef _DEBUG
        int noncollisionArray[4] =
        {   -1, -1, -1, -1};
        int noncollisionIndex = 0;
#endif
        double e_width, e_height, cx, cy;
        result = object->GetExtent(&e_width, &e_height);
        assert(result == 0);
        long lowerLeftX = 0;
        long lowerLeftY = 0;
        long width = 0;
        long height = 0;
        if (usePreviousPos)
        {
            result = object->GetPrevPosition(&cx, &cy);
        }
        else
        {
            result = object->GetPosition(&cx, &cy);
        }
        assert(result == 0);
        lowerLeftX = round(cx) - round(e_width / 2);
        lowerLeftY = round(cy) - round(e_height / 2);
        width = round(e_width);
        height = round(e_height);
        long llx, lly, w, h;
        for (int i = 0; i < 4; i++)
        {
            result = m_subNodes[i]->GetSize(&llx, &lly, &w, &h);
            assert(result == 0);
            int left1, left2;
            int right1, right2;
            int top1, top2;
            int bottom1, bottom2;

            left1 = lowerLeftX;
            left2 = llx;
            right1 = lowerLeftX + width;
            right2 = llx + w;
            bottom1 = lowerLeftY;
            bottom2 = lly;
            top1 = lowerLeftY + height;
            top2 = lly + h;
            if (bottom1 > top2 || top1 < bottom2 || right1 < left2 || left1
                    > right2)
            {
#ifdef _DEBUG
                noncollisionArray[noncollisionIndex++] = i;
#endif
                continue;
            }
            else
            {
                collisionArray[collisionIndex++] = i;
            }
        }
        for (int j = 0; j < collisionIndex; j++)
        {
            result = m_subNodes[collisionArray[j]]->RemoveObject(object,
                    usePreviousPos, true);
            assert(result == 0);
        }
#ifdef _DEBUG
        // Validate that we do NOT find this object in subnodes where it should not be, since
        // it is outside of us (the parents) bounds, it should not be in our subnodes.
        for (j = 0; j < noncollisionIndex; j++)
        {
            result = m_subNodes[noncollisionArray[j]]->RemoveObject(object, usePreviousPos, false);
            assert(result == 0);
        }
#endif
    }
    return result;
}

// Call if an object has moved and needs to be updated in the quad tree.
long QuadTreeNode::UpdateObject(ActiveRegion *obj)
{
    long result = 0;
    if (!m_subNodes)
    {
        // This function should only be called on a quad tree node with sub nodes.
        assert(false);
        result = -1;
    }
    else
    {
        int i;

        // First remove the object from subnodes based on its previous position, then add it
        // back with current position.
        for (i = 0; i < 4; i++)
        {
            result = m_subNodes[i]->RemoveObject(obj, true, true);
            assert(result == 0);
        }
        for (i = 0; i < 4; i++)
        {
            result = m_subNodes[i]->AddObject(obj);
            assert(result == 0);
        }
    }
    return result;
}
