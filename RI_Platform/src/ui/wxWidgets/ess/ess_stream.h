/*

 ESS Extremely Simple Serialization for C++

 http://www.novadsp.com/ess

 Copyright (c) Jerry Evans, 2008-2009
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.

 */

#ifndef ESS_STREAM_H
#define ESS_STREAM_H

#pragma once

#include <vector>
#include <map>
#include <set>
#include <algorithm>
#include <sstream>
#include "ess_rtti.h"
#include "ess_adapter.h"

namespace ess
{

//-----------------------------------------------------------------------------
// inline free functions to stream atomic data types
//-----------------------------------------------------------------------------
// bool
inline
void stream(stream_adapter& adapter, bool& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("bool", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// GUID
inline
void stream(stream_adapter& adapter, GUID& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("GUID", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// short
inline
void stream(stream_adapter& adapter, signed short& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("signed_short", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// unsigned short
inline
void stream(stream_adapter& adapter, unsigned short& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("unsigned_short", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// short
inline
void stream(stream_adapter& adapter, signed int& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("signed_int", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// unsigned short
inline
void stream(stream_adapter& adapter, unsigned int& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("unsigned_int", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// signed long
inline void stream(stream_adapter& adapter, signed long& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("signed_long", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// unsigned long
inline
void stream(stream_adapter& adapter, unsigned long& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("unsigned_long", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// double
inline
void stream(stream_adapter& adapter, double& arg, const std::string& name)
{
    std::string key = name;
    if (adapter.storing())
    {
        adapter.write(key, arg);
    }
    else
    {
        adapter.read("double", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// float
inline
void stream(stream_adapter& adapter, float& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("float", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// std::string
inline
void stream(stream_adapter& adapter, std::string& arg, const std::string& name)
{
    if (adapter.storing())
    {
        adapter.write(name, arg);
    }
    else
    {
        adapter.read("string", "value", arg);
    }
}

//-----------------------------------------------------------------------------
// serialize a reference to a non-native class/struct. i.e the Type here is
// user-defined and has to support the essentials for serialization:
// a virtual serialize() function and a static classname function so we
// can restore the correct derived type
template<typename Type>
inline
void stream(stream_adapter& adapter, Type& arg, const std::string& name)
{
    if (adapter.storing())
    {
        // we need to get the correct derived name from the class hence the use of the virtual get_classname
        adapter.begin_class(name_from_instance(arg), name, false);
        // serialize the instance
        arg.serialize(adapter);
        //
        adapter.end_class();
    }
    else
    {
        std::string derived_type;
        // key should contain type name
        adapter.read("class", "derived_type", derived_type);
        // runtime check
        arg.serialize(adapter);
    }
}

//-----------------------------------------------------------------------------
// serialize a pointer to a non-native class/struct.
// has to implement serialize()
template<typename Type>
inline
void stream(stream_adapter& adapter, Type*& arg, const std::string& name)
{
    if (adapter.storing())
    {
        // get the most-derived class name
        std::string derived_type = name_from_instance(*arg);
        // start the class name
        adapter.begin_class(derived_type, name, true);
        // serialize the instance
        arg->serialize(adapter);
        //
        adapter.end_class();
    }
    else
    {
        // imagine we have Class1 and Class2, with Class2 inheriting from Class1
        // the usual compiler type information is insufficient to ensure we
        // reconstitute the right kind class instance - hence the derived type
        std::string derived_type;
        // key should contain type name - respecting inheritance.
        adapter.read("class", "derived_type", derived_type);
        // this handles creation of derived instances
        arg = instance_from_name<Type> (derived_type.c_str());
        //arg = instance_from_name(derived_type);
        // deserialize the instance
        arg->serialize(adapter);
    }
}

//-----------------------------------------------------------------------------
// vectors
// serialize a vector
template<class Type>
inline
void stream(stream_adapter& adapter, std::vector<Type>& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        size_t elements = arg.size();
        adapter.begin_container("vector", name, elements, false);
        for (size_t index = 0; index < elements; index++)
        {
            stream(adapter, arg[index], ess::FormatEx("V%d", index));
        }
        adapter.end_container("vector");
    }
    else
    {
        //
        arg.clear();
        // key should contain type name
        size_t elements = 0;
        adapter.read("vector", "count", elements);
        // the type is inferred from the vector templating
        for (size_t index = 0; index < elements; index++)
        {
            Type t = instance_from_type<Type> ();
            stream(adapter, t, name);
            arg.push_back(t);
        }
    }
}

//-----------------------------------------------------------------------------
// maps
template<typename Key, typename Value>
inline
void stream(stream_adapter& adapter, std::map<Key, Value>& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        size_t index = 0;
        size_t elements = arg.size();
        adapter.begin_container("map", name, elements, false);
        typename std::map<Key, Value>::iterator it;
        for (it = arg.begin(); it != arg.end(); ++it, ++index)
        {
            // the casts are to support VS2003 -
            stream(adapter, (Key) it->first, ess::FormatEx("K%d", index));
            // the curious cast <Value&> is to inhibit a poorly judged warning message
            stream(adapter, (Value&) it->second, ess::FormatEx("V%d", index));
        }
        adapter.end_container("map");
    }
    else
    {
        //
        arg.clear();
        size_t elements = 0;
        adapter.read("map", "count", elements);
        for (size_t index = 0; index < elements; index++)
        {
            Key key = instance_from_type<Key> ();
            stream(adapter, key, name);
            Value value = instance_from_type<Value> ();
            stream(adapter, value, name);
            arg[key] = value;
        }
    }
}

//-----------------------------------------------------------------------------
// sets
template<typename Value>
inline
void stream(stream_adapter& adapter, std::set<Value>& arg,
        const std::string& name)
{
    if (adapter.storing())
    {
        size_t index = 0;
        size_t elements = arg.size();
        adapter.begin_container("set", name, elements, false);
        typename std::set<Value>::iterator it;
        for (it = arg.begin(); it != arg.end(); ++it, ++index)
        {
            // the curious cast <Value&> is to inhibit a poorly judged warning message
            stream(adapter, (Value&) (*it), ess::FormatEx("V%d", index));
        }
        adapter.end_container("set");
    }
    else
    {
        //
        arg.clear();
        size_t elements = 0;
        adapter.read("set", "count", elements);
        for (size_t index = 0; index < elements; index++)
        {
            Value value = instance_from_type<Value> ();
            stream(adapter, value, name);
            arg.insert(value);
        }
    }
}

//-----------------------------------------------------------------------------
// more shorthand - these can be used in your serialize() member function
// the ESS_STREAM stream macro stringizes the member name for you

#define ESS_STREAM(stream_adapter,class_member)        \
	ess::stream(stream_adapter,class_member,#class_member)

} // end of namespace

#endif	// ESS_STREAM_H
