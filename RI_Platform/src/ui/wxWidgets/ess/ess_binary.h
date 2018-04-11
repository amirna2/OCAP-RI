/*

 ESS Extremely Simple Serialization for C++

 http://www.novadsp.com/ess

 Copyright (c) Jerry Evans, 2008-2009
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

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

#ifndef ESS_BINARY_H
#define ESS_BINARY_H

#include <ostream>
#include <fstream>
#include <string.h>
#include "ess_adapter.h"

namespace ess
{

//-----------------------------------------------------------------------------
// This enumeration is ess specific. Do not adjust your sets.
enum TypeEnum
{
    bool_e, sint_e, // signed int
    uint_e,
    sshort_e,
    ushort_e,
    slong_e,
    ulong_e,
    float_e,
    double_e,
    string_e,
    wstring_e,
    GUID_e,
    container_e, // std::vector/std::list/std::map
    class_e, // any class type
    Max
};

struct TypeTextSize
{
    TypeEnum typeEnum;
    const char* text;
    size_t size;
};

//-----------------------------------------------------------------------------
// for some very basic information lookup
TypeTextSize typeTS[Max] =
{
{ bool_e, "bool", sizeof(bool) },
{ sint_e, "signed int", sizeof(signed int) },
{ uint_e, "unsigned int", sizeof(unsigned int) },
{ sshort_e, "short", sizeof(short) },
{ ushort_e, "unsigned short", sizeof(unsigned short) },
{ slong_e, "long", sizeof(long) },
{ ulong_e, "unsigned long", sizeof(unsigned long) },
{ float_e, "float", sizeof(float) },
{ double_e, "double", sizeof(double) },
{ string_e, "string", sizeof(size_t) },
{ wstring_e, "wstring", sizeof(size_t) },
{ GUID_e, "GUID", sizeof(GUID) },
{ container_e, "container", 0 },
{ class_e, "class", 0 } };

//-----------------------------------------------------------------------------
#define _DEBUG_ESS_BINARY

//-----------------------------------------------------------------------------
// Somewhere to store ESS binary data
class binary_medium
{
    typedef unsigned char T;
    std::vector<T> m_buffer;
    size_t m_offset;

protected:

public:

    // set up and set initial size
    binary_medium(size_t quanta = 64) :
        m_offset(0)
    {
        m_buffer.reserve(quanta);
    }

    // set up and set initial size
    binary_medium(const T* ps, size_t size) :
        m_offset(0)
    {
        m_buffer.assign(ps, ps + size);
    }

    virtual ~binary_medium()
    {
    }

    // read by copying onto external buffer
    virtual void read(void* pv, size_t bytes)
    {
        // runtime check ...
        if (pv == 0 || (m_offset + bytes) > m_buffer.size())
        {
            std::stringstream strs;
            strs << "bad read() in binary_medium: " << " offset: " << m_offset
                    << " offset+bytes: " << m_offset + bytes
                    << " buffer size: " << m_buffer.size();
            //throw std::exception(strs.str().c_str());
            throw std::exception();
        }
        // for clarity
        T* pd = static_cast<T*> (pv);
        const T* ps = &m_buffer[m_offset];
        //
        memcpy(pd, ps, bytes);
        //
        m_offset += bytes;
    }

    // write by appending to internal buffer
    virtual void write(const void* pv, size_t bytes)
    {
        // for clarity
        const T* ps = static_cast<const T*> (pv);
        //
        m_buffer.insert(m_buffer.end(), ps, ps + bytes);
    }

    // reset the read index
    virtual void reset()
    {
        m_offset = 0;
    }

    // where is the read index now?
    const size_t offset() const
    {
        return m_offset;
    }

    // how many bytes in the store?
    const size_t size() const
    {
        return m_buffer.size();
    }

    const T* data() const
    {
        return &m_buffer[0];
    }

    // for testing
    bool operator==(const binary_medium& arg) const
    {
        return (m_buffer == arg.m_buffer);
    }
};

//-----------------------------------------------------------------------------
class binary_loading_adapter: public stream_adapter
{
    binary_loading_adapter& operator=(const binary_loading_adapter&);
    std::string m_name;
    binary_medium& m_buffer_ref;

    // throws if type does match expected
    void _read(TypeEnum te, void* pv, size_t bytes)
    {
        // read an unsigned 32 bit value used as tag
        long sl = 0;
        // check
        _read2(&sl, sizeof(sl));
        // fail ...
        if (((sl & te) != te))
        {
            std::stringstream strs;
            strs << "binary_loading_adapter Expected: " << (short) te
                    << "Got: " << (short) (sl & te);
            //throw std::exception(strs.str().c_str());
            throw std::exception();
        }
        // read the actual data
        _read2(pv, bytes);
    }

    // raw read
    void _read2(void* pv, size_t bytes)
    {
        m_buffer_ref.read(pv, bytes);
    }

public:

    binary_loading_adapter(binary_medium& ref, const std::string& name,
            int version = 1) :
        stream_adapter(version), m_name(name), m_buffer_ref(ref)
    {
    }

    //
    virtual bool storing() const
    {
        return false;
    }

    //
    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, bool& arg)
    {
        _read(bool_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, signed int& arg)
    {
        _read(sint_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, unsigned int& arg)
    {
        _read(uint_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, signed short& arg)
    {
        _read(sshort_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, unsigned short& arg)
    {
        _read(ushort_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, signed long& arg)
    {
        _read(slong_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, unsigned long& arg)
    {
        _read(ulong_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, float& arg)
    {
        _read(float_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, double& arg)
    {
        _read(double_e, &arg, sizeof(arg));
    }

    virtual void read(const std::string& /*tag*/,
            const std::string& /*attribute*/, std::string& arg)
    {
        // read the string header
        size_t length = 0;
        _read(string_e, &length, sizeof(length));
        if (length > 0)
        {
            //
            arg.assign(length, 0);
            // do a raw read - no header LONG expected
            _read2(&arg[0], length);
        }
    }

    virtual void read(const std::string& /*key*/,
            const std::string& /*attribute*/, GUID& arg)
    {
        _read(GUID_e, &arg, sizeof(arg));
    }
};

//-----------------------------------------------------------------------------
// adapter for binary storage
class binary_storing_adapter: public stream_adapter
{
    binary_storing_adapter& operator=(const binary_storing_adapter&);
    std::string m_name;
    binary_medium& m_buffer_ref;

    // throws if type does match expected
    void _write(TypeEnum te, const void* pv, size_t bytes)
    {
        // read an unsigned 32 bit value used as tag
        long sl = 0;
        sl |= te;
        // write the tag
        _write2(&sl, sizeof(sl));
        // read the actual data
        _write2(pv, bytes);
    }

    // raw read
    void _write2(const void* pv, size_t bytes)
    {
        m_buffer_ref.write(pv, bytes);
    }

public:

    binary_storing_adapter(binary_medium& ref, const std::string& name,
            int version = 1) :
        stream_adapter(version), m_name(name), m_buffer_ref(ref)
    {

    }

    //
    virtual bool storing() const
    {
        return true;
    }

    // open a class
    virtual void begin_class(const std::string& derived_type,
            const std::string& name, bool /*is_pointer*/)
    {
        write(name, derived_type);
    }

    // close a container tag e.g. </map>
    virtual void end_class()
    {
    }

    // open a container
    virtual void begin_container(const std::string& /*type*/,
            const std::string& name, size_t count, bool /*is_pointer*/)
    {
        write(name, count);
    }

    // close a container tag e.g. </map>
    virtual void end_container(const std::string& /*type*/)
    {
    }

    virtual void write(const std::string& /*key*/, const bool& arg)
    {
        _write(bool_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const signed int& arg)
    {
        _write(sint_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const unsigned int& arg)
    {
        _write(uint_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const signed short& arg)
    {
        _write(sshort_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const unsigned short& arg)
    {
        _write(ushort_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const signed long& arg)
    {
        _write(slong_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const unsigned long& arg)
    {
        _write(ulong_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const float& arg)
    {
        _write(float_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const double& arg)
    {
        _write(double_e, &arg, sizeof(arg));
    }

    virtual void write(const std::string& /*key*/, const std::string& arg)
    {
        // read the string header
        size_t length = arg.size();
        _write(string_e, &length, sizeof(length));
        if (length > 0)
        {
            // do a raw write - no header LONG expected
            _write2(&arg[0], length);
        }
    }

    virtual void write(const std::string& /*key*/, const GUID& arg)
    {
        _write(GUID_e, &arg, sizeof(arg));
    }
};

} // end of namespace ess

#endif
