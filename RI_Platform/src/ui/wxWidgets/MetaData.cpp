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
 * MetaData.cpp
 *
 *  Created on: Jul 21, 2009
 *      Author: Mark Millard
 */

/*lint -e1551*/

// Include header files
#include <iostream>
#include <iomanip>
#include "MetaData.h"

using namespace std;
using namespace ess;

MetaData::MetaData() :
    m_numMetaData(0), m_metaData(NULL)
{
    // Do nothing for now.
}

MetaData::MetaData(MetaData const &other) :
    m_numMetaData(0), m_metaData(NULL)
{
    // Allocate new memory and copy the data.
    MetaDataNode *newNodes = NULL;
    if (other.m_numMetaData > 0)
    {
        newNodes = new MetaDataNode[other.m_numMetaData];
        for (unsigned int i = 0; i < other.m_numMetaData; i++)
            newNodes[i] = other.m_metaData[i];
    }

    // Finish assignment.
    m_numMetaData = other.m_numMetaData;
    m_metaData = newNodes;
}

MetaData::~MetaData()
{
    if (m_metaData != NULL)
        delete[] m_metaData;
}

MetaData &MetaData::operator =(const MetaData &other)
{
    if (this != &other) // Protect against invalid self-assignment.
    {
        // Allocate new memory and copy the data.
        MetaDataNode *newNodes = NULL;
        if (other.m_numMetaData > 0)
        {
            newNodes = new MetaDataNode[other.m_numMetaData];
            for (unsigned int i = 0; i < other.m_numMetaData; i++)
                newNodes[i] = other.m_metaData[i];
        }

        // Deallocate old references.
        if (m_metaData != NULL)
            delete[] m_metaData;

        // Finish assignment.
        m_numMetaData = other.m_numMetaData;
        m_metaData = newNodes;
    }

    // By convention, always return *this.
    return *this;
}

void MetaData::dump(int indent)
{
    for (unsigned int i = 0; i < m_numMetaData; i++)
    {
        cout << "*** Meta Data " << i << " ***" << endl;
        if (m_metaData != NULL)
            m_metaData[i].dump(3);
    }
}

MetaDataNode::MetaDataNode() :
    m_numKeyValuePairs(0), m_keyValues(NULL), m_numNodes(0), m_nodes(NULL)
{
    // Do nothing for now.
}

MetaDataNode::MetaDataNode(MetaDataNode const &other) :
    m_numKeyValuePairs(0), m_keyValues(NULL), m_numNodes(0), m_nodes(NULL)
{
    // Allocate new memory and copy the data.
    KeyValuePair *newKeyValues = NULL;
    MetaDataNode *newNodes = NULL;
    if (other.m_numKeyValuePairs > 0)
    {
        newKeyValues = new KeyValuePair[other.m_numKeyValuePairs];
        for (unsigned int i = 0; i < other.m_numKeyValuePairs; i++)
            newKeyValues[i] = other.m_keyValues[i];
    }
    if (other.m_numNodes > 0)
    {
        newNodes = new MetaDataNode[other.m_numNodes];
        for (unsigned int i = 0; i < other.m_numNodes; i++)
            newNodes[i] = other.m_nodes[i];
    }

    // Finish assignment
    m_id = other.m_id;
    m_numKeyValuePairs = other.m_numKeyValuePairs;
    m_keyValues = newKeyValues;
    m_numNodes = other.m_numNodes;
    m_nodes = newNodes;
}

MetaDataNode::~MetaDataNode()
{
    m_id.clear();
    if (m_keyValues != NULL)
    {
        delete[] m_keyValues;
        m_keyValues = NULL;
    }
    if (m_nodes != NULL)
    {
        delete[] m_nodes;
        m_nodes = NULL;
    }
}

MetaDataNode &MetaDataNode::operator =(const MetaDataNode &other)
{
    if (this != &other) // Protect against invalid self-assignment.
    {
        // Allocate new memory and copy the data.
        KeyValuePair *newKeyValues = NULL;
        MetaDataNode *newNodes = NULL;
        if (other.m_numKeyValuePairs > 0)
        {
            newKeyValues = new KeyValuePair[other.m_numKeyValuePairs];
            for (unsigned int i = 0; i < other.m_numKeyValuePairs; i++)
                newKeyValues[i] = other.m_keyValues[i];
        }
        if (other.m_numNodes > 0)
        {
            newNodes = new MetaDataNode[other.m_numNodes];
            for (unsigned int i = 0; i < other.m_numNodes; i++)
                newNodes[i] = other.m_nodes[i];
        }

        // Deallocate old references.
        if (m_keyValues != NULL)
            delete[] m_keyValues;
        if (m_nodes != NULL)
            delete[] m_nodes;

        // Finish assignment
        m_id = other.m_id;
        m_numKeyValuePairs = other.m_numKeyValuePairs;
        m_keyValues = newKeyValues;
        m_numNodes = other.m_numNodes;
        m_nodes = newNodes;
    }

    // By convention, always return *this.
    return *this;
}

void MetaDataNode::dump(int indent)
{
    unsigned long i;
    cout << setw(indent) << "" << "Node: " << m_id << endl;
    cout << setw(indent) << "" << "   Number of Key/Value Pairs : "
            << m_numKeyValuePairs << endl;
    for (i = 0; i < m_numKeyValuePairs; i++)
    {
        cout << setw(indent + 3) << "" << "*** Key/Value Pair " << i << " ***"
                << endl;
        if (m_keyValues != NULL)
            m_keyValues[i].dump(indent + 3);
    }
    cout << setw(indent) << "" << "   Number of Sub Meta Data Nodes : "
            << m_numNodes << endl;
    for (i = 0; i < m_numNodes; i++)
    {
        cout << setw(indent + 3) << "" << "*** Sub-node " << i << " ***"
                << endl;
        if (m_nodes != NULL)
            m_nodes[i].dump(indent + 3);
    }
}

KeyValuePair::KeyValuePair()
{
    // Do nothing for now.
}

KeyValuePair::KeyValuePair(KeyValuePair const &other)
{
    m_id = other.m_id;
    m_key = other.m_key;
    m_value = other.m_value;
    m_type = other.m_type;
}

KeyValuePair::~KeyValuePair()
{
    m_id.clear();
    m_key.clear();
    m_value.clear();
    m_type.clear();
}

KeyValuePair &KeyValuePair::operator =(const KeyValuePair &other)
{
    if (this != &other) // Protect against invalid self-assignment.
    {
        m_id = other.m_id;
        m_key = other.m_key;
        m_value = other.m_value;
        m_type = other.m_type;
    }

    // By convention, always return *this.
    return *this;
}

void KeyValuePair::dump(int indent)
{
    if (!m_id.empty())
        cout << setw(indent) << "" << "id : " << m_id << endl;
    if (!m_key.empty())
        cout << setw(indent) << "" << "key : " << m_key << endl;
    if (!m_value.empty())
        cout << setw(indent) << "" << "value : " << m_value << endl;
    if (!m_type.empty())
        cout << setw(indent) << "" << "type : " << m_type << endl;

}
