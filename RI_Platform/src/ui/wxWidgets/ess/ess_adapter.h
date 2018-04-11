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

#ifndef ESS_ADAPTER_H
#define ESS_ADAPTER_H

#if defined(_MSC_VER)
#pragma warning(push)
#pragma warning(disable:4100)
#endif

#include <stdarg.h>
#include <stdio.h>

#include "xml_parser.h"

// this is for non-Windows platforms
#if !defined(_WINDOWS)
#ifndef GUID_DEFINED
#define GUID_DEFINED
typedef struct _GUID
{
    unsigned long Data1;
    unsigned short Data2;
    unsigned short Data3;
    unsigned char Data4[8];
} GUID;
#endif
#endif

namespace ess
{

//-----------------------------------------------------------------------------
// quick and dirty formatting helper
inline std::string FormatEx(const char* szFormat, ...)
{
    enum
    {
        _MAX_CHARS = 2 * 8096
    };
    // reserve
    std::string str(_MAX_CHARS, 0);
    va_list argList;
    va_start(argList, szFormat);
#if _MSC_VER >= 1400
    int ret = vsnprintf_s(&str[0],_MAX_CHARS,_MAX_CHARS-1,szFormat,argList);
#else
    int ret = vsnprintf(&str[0], _MAX_CHARS, szFormat, argList);
#endif
    va_end(argList);
    str.resize(ret);
    return str;
}

// facade class to hide reader/writer - hides serialized medium
class stream_adapter
{
protected:

    int m_version;

    stream_adapter(int version) :
        m_version(version)
    {
    }
public:

    virtual ~stream_adapter()
    {
    }

    // must be implemented in derived classes
    virtual bool storing() const = 0;
    int version() const
    {
        return m_version;
    }
    //
    virtual void begin_class(const std::string& derived_type,
            const std::string& name, bool is_pointer)
    {
    }
    virtual void begin_container(const std::string& type,
            const std::string& name, size_t count, bool is_pointer)
    {
    }
    // close a container tag e.g. </map>
    virtual void end_class()
    {
    }
    // close a container tag e.g. </map>
    virtual void end_container(const std::string& type)
    {
    }

    virtual void write(const std::string& key, const bool& arg)
    {
    }
    virtual void write(const std::string& key, const signed short& arg)
    {
    }
    virtual void write(const std::string& key, const unsigned short& arg)
    {
    }
    virtual void write(const std::string& key, const signed int& arg)
    {
    }
    virtual void write(const std::string& key, const unsigned int& arg)
    {
    }
    virtual void write(const std::string& key, const signed long& arg)
    {
    }
    virtual void write(const std::string& key, const unsigned long& arg)
    {
    }
    virtual void write(const std::string& key, const float& arg)
    {
    }
    virtual void write(const std::string& key, const double& arg)
    {
    }
    virtual void write(const std::string& key, const std::string& arg)
    {
    }
    virtual void write(const std::string& key, const GUID& arg)
    {
    }

    virtual void read(const std::string& tag, const std::string& attribute,
            bool& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            signed short& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            unsigned short& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            signed int& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            unsigned int& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            signed long& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            unsigned long& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            float& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            double& arg)
    {
    }
    virtual void read(const std::string& tag, const std::string& attribute,
            std::string& arg)
    {
    }
    virtual void read(const std::string& key, const std::string& attribute,
            GUID& arg)
    {
    }
};

} // end of serializer namespace

#if defined(_MSC_VER)
#pragma warning(pop)
#endif

#endif
