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
 * MetaData.h
 *
 *  Created on: Jul 21, 2009
 *      Author: Mark Millard
 */

/*lint -emacro(1576, ESS_RTTI)*/

#ifndef METADATA_H_
#define METADATA_H_

// Include serialization headers.
#include "ess_stream.h"

namespace ess
{

/**
 * Defines the key/value pair identifying a meta data element.
 */
class KeyValuePair
{
public:

    std::string m_id; /**< The meta data element identifier. */
    std::string m_key; /**< The key for the meta data element. */
    std::string m_value; /**< The value for the meta data element. */
    std::string m_type; /**< The data type for the value. */

public:

    /**
     * The default constructor.
     */
    KeyValuePair();

    /**
     * A copy constructor.
     *
     * @param other A reference to the other KeyValue instance to copy from.
     */
    KeyValuePair(KeyValuePair const &other);

    /**
     * The destructor.
     */
    virtual ~KeyValuePair();

    // Specify the inheritance root for serialization.
    ESS_ROOT( KeyValuePair)
    // Set up RTTI for serialization.
    ESS_RTTI(KeyValuePair, KeyValuePair)

    /**
     * The serialization method.
     * <p>
     * This method is symmetric, working for both reading and writing.
     * </p>
     *
     * @param adapter The stream to serialize.
     */
    virtual void serialize(ess::stream_adapter &adapter)
    {
        //printf("Serializing KeyValuePair\n");
        ESS_STREAM(adapter, m_id);
        ESS_STREAM(adapter, m_key);
        ESS_STREAM(adapter, m_value);
        ESS_STREAM(adapter, m_type);
    }

    /**
     * Assignment operator.
     *
     * @param other The other pair to copy from.
     *
     * @return A reference to <b>this</b> is returned by convention.
     */
    KeyValuePair &operator =(const KeyValuePair &other);

    /**
     * Dump the contents to stdout.
     */
    void dump(int indent);
};

/**
 * Defines a hierarchical meta data node.
 */
class MetaDataNode
{
public:

    std::string m_id; /**< The node identifier. */
    unsigned long m_numKeyValuePairs; /**< The number of meta data elements. */
    KeyValuePair *m_keyValues; /**< The meta data elements. */
    unsigned long m_numNodes; /**< The number of sub-nodes. */
    MetaDataNode *m_nodes; /**< The sub-nodes. */

public:

    /**
     * The default constructor.
     */
    MetaDataNode();

    /**
     * A copy constructor.
     *
     * @param other A reference to the other MetaDataNode instance to copy from.
     */
    MetaDataNode(MetaDataNode const &other);

    /**
     * The destructor.
     */
    virtual ~MetaDataNode();

    // Specify the inheritance root for serialization.
    ESS_ROOT( MetaDataNode)
    // Set up RTTI for serialization.
    ESS_RTTI(MetaDataNode, MetaDataNode)

    /**
     * The serialization method.
     * <p>
     * This method is symmetric, working for both reading and writing.
     * </p>
     *
     * @param adapter The stream to serialize.
     */
    virtual void serialize(ess::stream_adapter &adapter)
    {
        unsigned long i;

        //printf("Serializing MetaDataNode\n");
        ESS_STREAM(adapter, m_id);
        ESS_STREAM(adapter, m_numKeyValuePairs);
        if (! adapter.storing())
        {
            if (m_numKeyValuePairs > 0)
            m_keyValues = new KeyValuePair[m_numKeyValuePairs];
            else
            m_keyValues = NULL;
        }
        if (m_keyValues != NULL)
        {
            for (i = 0; i < m_numKeyValuePairs; i++)
            ess::stream(adapter, m_keyValues[i], ess::FormatEx("KeyValuePair_%d", i));
        }
        ESS_STREAM(adapter, m_numNodes);
        if (! adapter.storing())
        {
            if (m_numNodes > 0)
            m_nodes = new MetaDataNode[m_numNodes];
            else
            m_nodes = NULL;
        }
        if (m_nodes != NULL)
        {
            for (i = 0; i < m_numNodes; i++)
            ess::stream(adapter, m_nodes[i], ess::FormatEx("MetaDataNode_%d", i));
        }
    }

    /**
     * Assignment operator.
     *
     * @param other The other node to copy from.
     *
     * @return A reference to <b>this</b> is returned by convention.
     */
    MetaDataNode &operator =(const MetaDataNode &other);

    /**
     * Dump the contents to stdout.
     */
    void dump(int indent);
};

/**
 * Defines hierarchical meta data.
 */
class MetaData
{
public:

    unsigned long m_numMetaData; /**< The number of top-level meta data nodes. */
    MetaDataNode *m_metaData; /**< The top-level meta data hierarchy nodes. */

public:

    /*
     * The default constructor.
     */
    MetaData();

    /**
     * A copy constructor.
     *
     * @param other A reference to the other MetaData instance to copy from.
     */
    MetaData(MetaData const &other);

    /**
     * The destructor.
     */
    virtual ~MetaData();

    // Specify the inheritance root for serialization.
    ESS_ROOT( MetaData)
    // Set up RTTI for serialization.
    ESS_RTTI(MetaData, MetaData)

    /**
     * The serialization method.
     * <p>
     * This method is symmetric, working for both reading and writing.
     * </p>
     *
     * @param adapter The stream to serialize.
     */
    virtual void serialize(ess::stream_adapter &adapter)
    {
        //printf("Serializing MetaData\n");
        ESS_STREAM(adapter, m_numMetaData);
        if (! adapter.storing())
        {
            if (m_numMetaData > 0)
            m_metaData = new MetaDataNode[m_numMetaData];
            else
            m_metaData = NULL;
        }
        if (m_metaData != NULL)
        {
            // Initialize MetaData.
            for (unsigned int i = 0; i < m_numMetaData; i++)
            ess::stream(adapter, m_metaData[i], ess::FormatEx("MetaData_%d", i));
        }
    }

    /**
     * Assignment operator.
     *
     * @param other The other meta data to copy from.
     *
     * @return A reference to <b>this</b> is returned by convention.
     */
    MetaData &operator =(const MetaData &other);

    /**
     * Dump the contents to stdout.
     */
    void dump(int indent);
};

}
#endif /* METADATA_H_ */
