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
 * ImageMapReader.cpp
 *
 *  Created on: Feb 27, 2009
 *      Author: Mark Millard
 */

// Include system header files.
#include <sys/stat.h>

// Include RI Emulator header files.
#include "ri_log.h"
#include "ImageMapReader.h"
#include "ess_binary.h"

// Logging category.
#define RILOG_CATEGORY g_uiCat
extern log4c_category_t* g_uiCat;

ImageMapReader::ImageMapReader()
{
    // Do nothing extra.
}

ImageMapReader::~ImageMapReader()
{
    // Nothing to do.
}

size_t ImageMapReader::read(FILE *fp)
{
    size_t numRead, retValue = 0;

    struct stat fileStat;
    if (fstat(fileno(fp), &fileStat) == -1)
    {
        RILOG_ERROR("Unable to read Image Map file.\n");
        return 0;
    }

    // Read the header.
    numRead = fread(&m_imageMapHeader, sizeof(ImageMapHeader), 1, fp);
    if (numRead != 1)
    {
        RILOG_ERROR("Unable to read Image Map file header.\n");
        return 0;
    }
    else
        retValue = sizeof(ImageMapHeader);

    // Check that this is a valid image map file.
    if (m_imageMapHeader.m_magicNumber != IMAGEMAP_MAGIC_NUMBER)
    {
        RILOG_ERROR("Invalid Image Map file.\n");
        return 0;
    }

    // Check that the format of this file correlates to the version numbers
    // for this implementation.
    if (m_imageMapHeader.m_majorVersion != MAJOR_VERSION)
    {
        RILOG_ERROR(
                "Invalid Image Map file: major version = %d, expecting %lu.\n",
                m_imageMapHeader.m_majorVersion, MAJOR_VERSION);
        return 0;
    }
    if (m_imageMapHeader.m_minorVersion != MINOR_VERSION)
    {
        RILOG_ERROR(
                "Invalid Image Map file: minor version = %d, expecting %lu.\n",
                m_imageMapHeader.m_minorVersion, MINOR_VERSION);
        return 0;
    }

    // Validate size parameters of image data.
    // TODO = On Linux, the st_size field (of type off_t) may be a signed integer;
    // so casting here could be Windows Mingw implementation specific.
    if (m_imageMapHeader.m_size != (unsigned int) fileStat.st_size)
    {
        RILOG_ERROR("Corrupt Image Map file: size should be %d bytes.\n",
                m_imageMapHeader.m_size);
        return 0;
    }
    if ((m_imageMapHeader.m_baseImage.m_size > m_imageMapHeader.m_size)
            || (m_imageMapHeader.m_rolloverImage.m_size
                    > m_imageMapHeader.m_size)
            || (m_imageMapHeader.m_selectionImage.m_size
                    > m_imageMapHeader.m_size))
    {
        RILOG_ERROR("Corrupt Image Map header.\n");
        return 0;
    }
    if ((m_imageMapHeader.m_baseImage.m_size
            + m_imageMapHeader.m_rolloverImage.m_size
            + m_imageMapHeader.m_selectionImage.m_size)
            > m_imageMapHeader.m_size)
    {
        RILOG_ERROR("Corrupt Image Map header.\n");
        return 0;
    }

    // Read the image data.
    size_t imageMapSize = m_imageMapHeader.m_baseImage.m_size
            + m_imageMapHeader.m_rolloverImage.m_size
            + m_imageMapHeader.m_selectionImage.m_size;
    m_imageMap = new unsigned char[imageMapSize];
    if (!m_imageMap)
    {
        RILOG_ERROR("Unable to allocate enough memory for the Image Map.\n");
        return 0;
    }

    numRead = fread(m_imageMap, sizeof(unsigned char), imageMapSize, fp);
    if (numRead != imageMapSize)
    {
        RILOG_ERROR("Unable to read Image Map file images.\n");
        delete[] m_imageMap;
        m_imageMap = NULL;
        return 0;
    }
    else
        retValue += numRead;

    // Read the hotspot data.
    m_hotspotList.m_numHotspots = m_imageMapHeader.m_numHotspots;
    m_hotspotList.m_hotspots = new HotSpot[m_hotspotList.m_numHotspots];
    if (!m_hotspotList.m_hotspots)
    {
        RILOG_ERROR("Unable to allocate enough memory for the hotspots.\n");
        delete[] m_imageMap;
        m_imageMap = NULL;
        return 0;
    }

    numRead = fread(m_hotspotList.m_hotspots, sizeof(HotSpot),
            m_hotspotList.m_numHotspots, fp);
    if (numRead != m_hotspotList.m_numHotspots)
    {
        RILOG_ERROR("Unable to read Image Map file hotspots.\n");
        delete[] m_imageMap;
        m_imageMap = NULL;
        delete[] m_hotspotList.m_hotspots;
        m_hotspotList.m_numHotspots = 0;
        return 0;
    }
    else
        retValue += (sizeof(HotSpot) * m_hotspotList.m_numHotspots);

    // Read the meta data.
    m_metaDataList.m_numMetaData = m_imageMapHeader.m_numMetaData;
    if (m_metaDataList.m_numMetaData > 0)
    {
        // Read remaining data from image map.
        size_t size = m_imageMapHeader.m_size - retValue;
        char *buffer = new char[size];
        size_t nread = fread(buffer, size, 1, fp);
        if (nread != size)
        {
            RILOG_WARN("%s: Only read %u bytes from buffer.  Expected %u\n",
                       __FUNCTION__, nread, size);
        }
        RILOG_DEBUG("%s: Read %u bytes from buffer\n", __FUNCTION__, nread);

        // Setup unmarshalling for the Meta Data.
        ess::binary_medium storage; // Create a binary storage medium.
        storage.write(buffer, size); // And fill it with what we plan to extract.

        // Version of Meta Data
        const int version = 1;
        // Root name.
        std::string imapRoot = "ImageMapRoot";

        // Stream from binary storage medium.
        ess::binary_loading_adapter adapter(storage, imapRoot, version);

        m_metaDataList.m_metaData
                = new MetaDataNode[m_metaDataList.m_numMetaData];
        for (unsigned long i = 0; i < m_metaDataList.m_numMetaData; i++)
        {
            RILOG_INFO("Loading meta data element %lu.\n", i);
            // Retrieve stored data.
            ess::stream(adapter, m_metaDataList.m_metaData[i], ess::FormatEx(
                    "MetaData_%d", i));

            // Note: the tag parameter in ess::stream must match what was marshalled
            // to the Image Map file; otherwise nothing will be found in the storage
            // medium.
        }

        // Clean up.
        delete[] buffer;
        retValue += size;
    }
    else
        m_metaDataList.m_metaData = NULL;

    // Useful for debugging serialization.
    //if (m_metaDataList.m_metaData)
    //  m_metaDataList.m_metaData->dump(0);

    return retValue;
}
