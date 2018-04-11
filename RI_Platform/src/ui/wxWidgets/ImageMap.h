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
 * ImageMap.h
 *
 *  Created on: Feb 17, 2009
 *      Author: Mark Millard
 */

#ifndef __IMAGE_MAP_H_
#define __IMAGE_MAP_H_

// Include system header files.
#include <sys/types.h>

// Include Image Map header files.
#include "MetaData.h"

using namespace ess;

#define MakeTag(ch0, ch1, ch2, ch3) \
	( (signed long)(ch0)<<24L | (signed long)(ch1)<<16L | (ch2)<<8 | (ch3) )
#define IMAGEMAP_MAGIC_NUMBER MakeTag('I','M','A','P')

/**
 * Defines the hotspot position and extent.
 */
typedef struct _HotSpot
{
    int m_id; /* A unique hotspot identifier. */
    unsigned int m_x; /* The x coordinate position of the hotspot. */
    unsigned int m_y; /* The y coordinate position of the hotspot. */
    unsigned int m_width; /* The width of the hotspot. */
    unsigned int m_height; /* The height of the hotspot. */
    unsigned int m_event; /* The event code associated with the hotspot. */
    unsigned int m_rolloverOffset; /* Hilight offset relative to rollover image address. */
    unsigned int m_selectionOffset; /* Hilight offset relative to selection image address. */
} HotSpot;

/**
 * Defines the list of hotspots.
 */
typedef struct _HotSpotList
{
    unsigned long m_numHotspots; /* Number of hotspots. */
    HotSpot* m_hotspots; /* The array of hotspots. */
} HotSpotList;

/**
 * Defines header information for a particular image file.
 */
typedef struct _ImageHeader
{
    unsigned int m_flags; /* Meta information about the image. */
    unsigned int m_width; /* The width of the image. */
    unsigned int m_height; /* The height of the image. */
    size_t m_size; /* The size of the image. */
} ImageHeader;

/**
 * Defines the list of meta data.
 */
typedef struct _MetaDataList
{
    unsigned long m_numMetaData; /* Number of meta data trees. */
    MetaDataNode *m_metaData; /* The array of meta data. */
} MetaDataList;

/**
 * Defines the header information in an ImageMap file.
 */
typedef struct _ImageMapHeader
{
    unsigned int m_magicNumber; /* Unique ImageMap file identifier. */
    unsigned short m_majorVersion; /* The major version of the ImageMap format. */
    unsigned short m_minorVersion; /* The minor version of the ImageMap format. */
    size_t m_size; /* The size of the ImageMap file. */
    ImageHeader m_baseImage; /* The size of the base image. */
    ImageHeader m_rolloverImage; /* The size of the rollover highlight image. */
    ImageHeader m_selectionImage; /* The size of the selection highlight image. */
    unsigned long m_numHotspots; /* The number of hotspots in the image map. */
    unsigned long m_numMetaData; /* The number of meta data hierarchies in the image map. */
} ImageMapHeader;

/**
 * This class encapsulates the UI image map.
 * <p>
 * An image map is a construct that supports a raster-based user interface
 * where hotspots identify areas of interactivity.
 * </p>
 */
class ImageMap
{
public:

    /** The major version number for the Image Map format. */
    static const unsigned long MAJOR_VERSION = 1;
    /** The minor version number for the Image Map format. */
    static const unsigned long MINOR_VERSION = 1;
    /** Flag indicating that embedded images are uncompressed. */
    static const unsigned int IMAGE_UNCOMPRESSED = 0x00000000;
    /** Flag indicating that embedded images are compressed. */
    static const unsigned int IMAGE_COMPRESSED = 0x00000001;

    /**
     * The default constructor.
     */
    ImageMap();

    /**
     * The destructor.
     */
    virtual ~ImageMap();

    /**
     * Get the number of hotspots in the Image Map.
     *
     * @return The number of hotspots is returned.
     */
    virtual int GetNumHotspots()
    {
        return m_hotspotList.m_numHotspots;
    }

    /**
     * Get the list of hotspots.
     *
     * @return A pointer to the hotspot list is returned.
     */
    virtual HotSpot *GetHotspots();

    /**
     * Get the address of the base image data.
     *
     * @return A pointer to the base image data is returned.
     */
    unsigned char *GetBaseImageAddr();

    /**
     * Get the address of the rollover image data.
     *
     * @return A pointer to the rollover image data is returned.
     */
    unsigned char *GetRolloverImageAddr();

    /**
     * Get the address of the selection image data.
     *
     * @return A pointer to the selection image data is returned.
     */
    unsigned char *GetSelectionImageAddr();

    /**
     * Get the size of the base image.
     *
     * @param width An output parameter to receive the width of the image.
     * @param height An output parameter to receive the height of the image.
     */
    void GetBaseImageSize(unsigned int *width, unsigned int *height);

    /**
     * Get the rollover image for the specified hotspot.
     *
     * @param hotspotId The identifier for the hotpsot.
     *
     * @return A pointer to the rollover image for the specified
     * hotspot is returned.
     */
    virtual unsigned char *GetRolloverImage(int hotspotId);

    /**
     * Get the selection image for the specified hotspot.
     *
     * @param hotspotId The identifier for the hotpsot.
     *
     * @return A pointer to the selection image for the specified
     * hotspot is returned.
     */
    virtual unsigned char *GetSelectionImage(int hotspotId);

    /**
     * Get the hotspot for the specified identifier.
     *
     * @param hotspotId The identifier for the hotspot
     *
     * @return A point to the found <code>HotSpot</code> is
     * returned. <b>NULL</b> will be returned if the hotspot is
     * not located.
     */
    virtual HotSpot *GetHotspot(int hotspotId);

    /**
     * Determine if the specified (x,y) coordinate is in the given hotpsot.
     *
     * @param hotspot A reference to the hotspot to test.
     * @param x The x value of the coordinate.
     * @param y The y value of the coordinate.
     *
     * @return <b>true</b> will be returned if the (x,y) coordinate
     * intersects with the specified hotspot. Otherwise <b>false</b>
     * will be returned.
     */
    bool InHotspot(const HotSpot &hotspot, unsigned int x, unsigned int y);

    /**
     * Get the number of meta data in the Image Map.
     *
     * @return The number of meta data trees is returned.
     */
    int GetNumMetaData()
    {
        return m_metaDataList.m_numMetaData;
    }

    /**
     * Retrieve the hierarchical list of meta data.
     *
     * @return A pointer to the root of meta data is returned.
     * If there is no meta data, then <b>NULL</b> will be returned.
     */
    MetaDataNode *GetMetaData()
    {
        return m_metaDataList.m_metaData;
    }

    /**
     * Retrieve the meta data node corresponding to the specified identifier.
     *
     * @param id The meta data node id.
     *
     * @return A pointer to the meta data node matching the parameter <i>id</i>
     * is returned. If no match can be found, then <b>NULL</b> will be returned.
     */
    MetaDataNode *GetMetaDataNode(const char *id);

    /**
     * Retrieve the value of the meta data mapped to the specified key.
     *
     * @param node A pointer to a node in the meta data hierarchy.
     * @param key A pointer to a character string identifying the key.
     *
     * @return The value is returned as a <code>string</code>.
     * The string will be empty if there is no value for the specified
     * key.
     */
    const std::string GetMetaDataValue(MetaDataNode *node, char *key);

    /**
     * Find the meta data node matching the specified identifier.
     *
     * @param node A pointer to the root of the hierarchical tree to search.
     * @param id The identifier of the node to search for.
     *
     * @return If a matching meta data node is found, then a pointer to it will
     * be returned. Otherwise, <b>NULL</b> will be returned.
     */
    MetaDataNode *FindMetaData(MetaDataNode *node, const char *id);

protected:

    /** The Image Map header. */
    ImageMapHeader m_imageMapHeader;
    /** The Image Map. */
    unsigned char *m_imageMap;
    /** The list of hotspots. */
    HotSpotList m_hotspotList;
    /** The hierarchical collection of meta data. */
    MetaDataList m_metaDataList;
};

#endif /* __IMAGE_MAP_H_ */
