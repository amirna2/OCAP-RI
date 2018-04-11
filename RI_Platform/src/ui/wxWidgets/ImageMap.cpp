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
 * ImageMap.cpp
 *
 *  Created on: Feb 17, 2009
 *      Author: Mark Millard
 */

// Include system header files.
#include <stdio.h>
#include <string.h>

// Include RI Emulator header files.
#include "ImageMap.h"

ImageMap::ImageMap()
{
    m_imageMap = NULL;
    m_hotspotList.m_hotspots = NULL;
    m_hotspotList.m_numHotspots = 0;
    m_metaDataList.m_metaData = NULL;
    m_metaDataList.m_numMetaData = 0;
    m_imageMapHeader.m_magicNumber = IMAGEMAP_MAGIC_NUMBER;
    m_imageMapHeader.m_majorVersion = ImageMap::MAJOR_VERSION;
    m_imageMapHeader.m_minorVersion = ImageMap::MINOR_VERSION;
    m_imageMapHeader.m_numHotspots = 0;
    m_imageMapHeader.m_numMetaData = 0;
    m_imageMapHeader.m_size = 0;
    m_imageMapHeader.m_baseImage.m_flags = ImageMap::IMAGE_UNCOMPRESSED;
    m_imageMapHeader.m_baseImage.m_width = 0;
    m_imageMapHeader.m_baseImage.m_height = 0;
    m_imageMapHeader.m_baseImage.m_size = 0;
    m_imageMapHeader.m_rolloverImage.m_flags = ImageMap::IMAGE_UNCOMPRESSED;
    m_imageMapHeader.m_rolloverImage.m_width = 0;
    m_imageMapHeader.m_rolloverImage.m_height = 0;
    m_imageMapHeader.m_rolloverImage.m_size = 0;
    m_imageMapHeader.m_selectionImage.m_flags = ImageMap::IMAGE_UNCOMPRESSED;
    m_imageMapHeader.m_selectionImage.m_width = 0;
    m_imageMapHeader.m_selectionImage.m_height = 0;
    m_imageMapHeader.m_selectionImage.m_size = 0;
}

ImageMap::~ImageMap()
{
    if (m_imageMap)
        delete[] m_imageMap;
    if ((m_hotspotList.m_numHotspots > 0) && (m_hotspotList.m_hotspots != NULL))
        delete[] m_hotspotList.m_hotspots;
    if ((m_metaDataList.m_numMetaData > 0) && (m_metaDataList.m_metaData
            != NULL))
        /*lint -e(1551)*/
        delete[] m_metaDataList.m_metaData;
}

HotSpot *ImageMap::GetHotspots()
{
    HotSpot *hotspots = m_hotspotList.m_hotspots;
    return hotspots;
}

unsigned char *ImageMap::GetBaseImageAddr()
{
    return m_imageMap;
}

unsigned char *ImageMap::GetRolloverImageAddr()
{
    return m_imageMap ? m_imageMap + m_imageMapHeader.m_baseImage.m_size : NULL;
}

unsigned char *ImageMap::GetSelectionImageAddr()
{
    return m_imageMap ? m_imageMap + m_imageMapHeader.m_baseImage.m_size
            + m_imageMapHeader.m_rolloverImage.m_size : NULL;
}

unsigned char *ImageMap::GetRolloverImage(int hotspotId)
{
    unsigned char *addr = GetRolloverImageAddr();

    HotSpot *found = GetHotspot(hotspotId);
    if (found != NULL)
        return addr + found->m_rolloverOffset;
    else
        return NULL; // Not found;
}

unsigned char *ImageMap::GetSelectionImage(int hotspotId)
{
    unsigned char *addr = GetSelectionImageAddr();

    HotSpot *found = GetHotspot(hotspotId);
    if (found != NULL)
        return addr + found->m_selectionOffset;
    else
        return NULL; // Not found;
}

HotSpot *ImageMap::GetHotspot(int hotspotId)
{
    // This routine assumes that hotspots have been sorted by their unique identifier.
    // It also assumes that the rollover and selection image offsets are also sorted,
    // correlating to the hotspot they are associated with.

    // Use a binary search.
    int low = 0;
    int high = m_hotspotList.m_numHotspots - 1;
    while (low <= high)
    {
        int mid = low + ((high - low) / 2); // Note: not (low + high) / 2 !!
        if (m_hotspotList.m_hotspots[mid].m_id > hotspotId)
            high = mid - 1;
        else if (m_hotspotList.m_hotspots[mid].m_id < hotspotId)
            low = mid + 1;
        else
            return &m_hotspotList.m_hotspots[mid]; // Found.
    }
    return NULL; // Not found.
}

void ImageMap::GetBaseImageSize(unsigned int *width, unsigned int *height)
{
    *width = m_imageMapHeader.m_baseImage.m_width;
    *height = m_imageMapHeader.m_baseImage.m_height;
}

bool ImageMap::InHotspot(const HotSpot &hotspot, unsigned int x, unsigned int y)
{
    if ((x < hotspot.m_x) || (x > (hotspot.m_x + hotspot.m_width)) || (y
            < hotspot.m_y) || (y > (hotspot.m_y + hotspot.m_height)))
        return false;
    else
        return true;
}

MetaDataNode *ImageMap::GetMetaDataNode(const char *id)
{
    MetaDataNode *foundNode = NULL;

    // Search all top level meta data nodes until we find the first
    // matching identifier.
    for (unsigned int i = 0; i < m_metaDataList.m_numMetaData; i++)
    {
        foundNode = FindMetaData(&m_metaDataList.m_metaData[i], id);
        if (foundNode != NULL)
            break;
    }

    return foundNode;
}

const std::string ImageMap::GetMetaDataValue(MetaDataNode *node, char *key)
{
    std::string value;

    if ((node != NULL) && (key != NULL))
    {
        unsigned int i;

        for (i = 0; i < node->m_numKeyValuePairs; i++)
        {
            KeyValuePair keyValue = node->m_keyValues[i];
            //if (strcmp(key, keyValue.m_key) == 0)
            if (key == keyValue.m_key)
            {
                value = keyValue.m_value;
                break;
            }
        }
    }

    return value;
}

// Find a node matching the specified identifier by recursively walking
// the tree rooted at the specified node.
MetaDataNode *ImageMap::FindMetaData(MetaDataNode *node, const char *id)
{
    MetaDataNode *foundNode = NULL;
    unsigned int i;

    if ((node != NULL) && (id != NULL))
    {
        if (!node->m_id.empty())
            if (node->m_id == id)
                return node;

        // Walk the node hierarchy looking for a match.
        for (i = 0; i < node->m_numNodes; i++)
        {
            MetaDataNode *nextNode = &node->m_nodes[i];
            foundNode = FindMetaData(nextNode, id);
            if (foundNode != NULL)
                // There was a match; break free and return it.
                break;
        }
    }

    return foundNode;
}
