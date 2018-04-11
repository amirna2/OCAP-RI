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

#ifndef ESS_XML_H
#define ESS_XML_H

#if defined(_MSC_VER)
#pragma warning(push)
#pragma warning(disable:4100)	// unused parameters
#endif

// for std::stringstream
#include <stdio.h>
#include <stdarg.h>
#include <sstream>

#include "xml_parser.h"
#include "ess_adapter.h"

namespace ess
{

//-----------------------------------------------------------------------------
// Convert a GUID to its string representation
// hahahaha horrible!
inline std::string toString(const GUID& guid)
{
    return FormatEx("%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
            guid.Data1, guid.Data2, guid.Data3, (unsigned int) guid.Data4[0],
            (unsigned int) guid.Data4[1], (unsigned int) guid.Data4[2],
            (unsigned int) guid.Data4[3], (unsigned int) guid.Data4[4],
            (unsigned int) guid.Data4[5], (unsigned int) guid.Data4[6],
            (unsigned int) guid.Data4[7]);
}

//-----------------------------------------------------------------------------
// Convert a string representation to a GUID
inline GUID toGUID(const std::string& arg)
{
    GUID guid;
    unsigned long m3[8];
#if defined(_MSC_VER) && _MSC_VER >= 1400
    sscanf_s(arg.c_str(),
#else
    sscanf(arg.c_str(),
#endif
            "%08x-%04hx-%04hx-%02x%02x-%02x%02x%02x%02x%02x%02x",
            (unsigned int *) &guid.Data1, &guid.Data2, &guid.Data3,
            (unsigned int *) &m3[0], (unsigned int *) &m3[1],
            (unsigned int *) &m3[2], (unsigned int *) &m3[3],
            (unsigned int *) &m3[4], (unsigned int *) &m3[5],
            (unsigned int *) &m3[6], (unsigned int *) &m3[7]);

    for (int i = 0; i < 8; ++i)
    {
        guid.Data4[i] = (unsigned char) m3[i];
    }

    return guid;
}

//-----------------------------------------------------------------------------
// writes data for serializing - this trivial example simply appends to a string
class xml_medium
{
    std::string m_data;

public:
    xml_medium()
    {
        m_data.clear();
    }

    virtual ~xml_medium()
    {
    }

    virtual void write(const std::string& str)
    {
        m_data += str;
    }

    virtual const char* c_str()
    {
        return m_data.c_str();
    }

    const size_t size()
    {
        return m_data.size();
    }

    // for testing
    bool operator==(const xml_medium& arg) const
    {
        return (m_data == arg.m_data);
    }
};

//-----------------------------------------------------------------------------
// XML adapter
// facade class to hide reader/writer - hides serialized medium
class xml_storing_adapter: public stream_adapter
{
    xml_storing_adapter& operator=(const xml_storing_adapter&);
    // the medium
    xml_medium& m_medium;
    // current indentation level
    int m_indents;
    // vector of strings of \t\t etc
    std::vector<std::string> m_tabs;
    // root tag
    std::string m_root;

protected:

    // internal helper
    void write(const std::string& arg)
    {
        m_medium.write(m_tabs[m_indents]);
        m_medium.write(arg);
        m_medium.write("\n");
    }

    // for pretty printing
    void indent(int count)
    {
        m_indents += count;
    }

public:

    xml_storing_adapter(xml_medium& medium, const std::string& root,
            int version = 1, int depth = 64) :
        stream_adapter(version), m_medium(medium), m_root(root)
    {
        // this is for pretty printing
        m_indents = 0;
        std::string str = "";
        for (int i = 0; i < depth; i++)
        {
            m_tabs.push_back(str);
            str += "\t";
        }
        // write out the header
        std::stringstream strs;
        strs
                << "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
                << std::endl;
        strs << "<" << m_root << " version=\"" << m_version << "\">";
        write(strs.str());
    }

    virtual ~xml_storing_adapter()
    {
        std::stringstream strs;
        strs << "</" << m_root << ">";
        // close the XML document with the root tag
        write(strs.str());
    }

    //
    virtual bool storing() const
    {
        return true;
    }

    //
    virtual void begin_class(const std::string& derived_type,
            const std::string& name, bool is_pointer)
    {
        std::string str = FormatEx("<class derived_type=\"%s\" name=\"%s\" >",
                derived_type.c_str(), // derivation from base type
                name.c_str()); // instance name
        write(str);
        indent(+1);
    }

    //
    virtual void begin_container(const std::string& type,
            const std::string& name, size_t count, bool is_pointer)
    {
        std::string str = FormatEx("<%s name=\"%s\" count=\"%d\" >",
                type.c_str(), // vector/map/set/list
                name.c_str(), // instance name
                count); // how many elements in the container
        write(str);
        indent(+1);
    }

    // close a container tag e.g. </map>
    virtual void end_class()
    {
        write("</class>");
        indent(-1);
    }

    // close a container tag e.g. </map>
    virtual void end_container(const std::string& type)
    {
        write(FormatEx("</%s>", type.c_str()));
        indent(-1);
    }

    virtual void write(const std::string& key, const bool& arg)
    {
        write(FormatEx("<bool name=\"%s\" value=\"%s\"/>", key.c_str(),
                (arg ? "true" : "false")));
    }

    virtual void write(const std::string& key, const GUID& arg)
    {
        std::stringstream ss;
        ss << "<GUID name=\"" << key << "\" value=\"" << toString(arg)
                << "\" />";
        write(ss.str());
    }

    virtual void write(const std::string& key, const signed short& arg)
    {
        write(FormatEx("<signed_short name=\"%s\" value=\"%d\"/>", key.c_str(),
                arg));
    }

    virtual void write(const std::string& key, const unsigned short& arg)
    {
        write(FormatEx("<unsigned_short name=\"%s\" value=\"%d\"/>",
                key.c_str(), arg));
    }

    virtual void write(const std::string& key, const signed int& arg)
    {
        write(FormatEx("<signed_int name=\"%s\" value=\"%d\"/>", key.c_str(),
                arg));
    }

    virtual void write(const std::string& key, const unsigned int& arg)
    {
        write(FormatEx("<unsigned_int name=\"%s\" value=\"%d\"/>", key.c_str(),
                arg));
    }

    virtual void write(const std::string& key, const signed long& arg)
    {
        write(FormatEx("<signed_long name=\"%s\" value=\"%d\"/>", key.c_str(),
                arg));
    }

    virtual void write(const std::string& key, const unsigned long& arg)
    {
        write(FormatEx("<unsigned_long name=\"%s\" value=\"%d\"/>",
                key.c_str(), arg));
    }

    virtual void write(const std::string& key, const float& arg)
    {
        write(FormatEx("<float name=\"%s\" value=\"%f\"/>", key.c_str(), arg));
    }

    virtual void write(const std::string& key, const double& arg)
    {
        write(FormatEx("<double name=\"%s\" value=\"%g\"/>", key.c_str(), arg));
    }

    virtual void write(const std::string& key, const std::string& arg)
    {
        write(FormatEx("<string name=\"%s\" length=\"%d\" value=\"%s\"/>",
                key.c_str(), arg.size(), arg.c_str()));
    }
};

//-----------------------------------------------------------------------------
// facade class to hide reader/writer - hides serialized medium
class loading_adapter: public stream_adapter
{
protected:

    // has to be overidden depending on data source
    virtual int vread(const std::string& tag, const std::string& attribute,
            std::string& value) = 0;

public:

    loading_adapter(int version) :
        stream_adapter(version)
    {
    }

    //
    virtual bool storing() const
    {
        return false;
    }

    void read(const std::string& tag, const std::string& attribute, bool& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (value == "true" ? true : false);
    }

    void read(const std::string& tag, const std::string& attribute, GUID& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = ess::toGUID(value);
    }

    void read(const std::string& tag, const std::string& attribute,
            signed short& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (short) atoi(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            unsigned short& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (unsigned short) atoi(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            signed int& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (int) atoi(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            unsigned int& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (unsigned int) atoi(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            signed long& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (long) atol(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            unsigned long& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (unsigned long) atol(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute, float& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (float) atof(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute, double& arg)
    {
        std::string value;
        vread(tag, attribute, value);
        arg = (double) atof(value.c_str());
    }

    void read(const std::string& tag, const std::string& attribute,
            std::string& arg)
    {
        vread(tag, attribute, arg);
    }
};

//-----------------------------------------------------------------------------
//
class xml_loading_adapter: public loading_adapter
{
    // memory-mapped xml source
    Chordia::xml_source& m_xmls;
    // simple non-validating xml parser
    Chordia::xml_parser m_parser;
    // the last token we got
    int m_token;

    // checks tag - finds attribute and returns in value
    virtual int vread(const std::string& tag, const std::string& attribute,
            std::string& value)
    {
        bool done = false;
        while (!done)
        {
            m_token = m_parser.parse2(m_xmls, m_token);

            if (m_token == Chordia::xml_parser::element_start || m_token
                    == Chordia::xml_parser::empty_element)
            {
                if (tag != m_parser.get_tag())
                {
                    std::stringstream strs;
                    strs << "tag check fails... wanted: " << tag << ": "
                            << attribute << " got: " << m_parser.get_tag()
                            << " at line: " << m_parser.line_number();
                    //throw(std::exception(strs.str().c_str()));
                    throw(std::exception());
                }
                value = m_parser.find_attribute(attribute);
                if (value == "<none>")
                {
                    std::stringstream strs;
                    strs << "Invalid value: Tag: " << tag << " Attribute: "
                            << attribute << " at line: "
                            << m_parser.line_number();
                    //throw(std::exception(strs.str().c_str()));
                    throw(std::exception());
                }
                done = true;
            }
            else if (m_token == Chordia::xml_parser::eof)
            {
                std::stringstream strs;
                strs << "unexpected end of file looking for tag: " << tag;
                //throw(std::exception(strs.str().c_str()));
                throw(std::exception());
            }
        }
        return m_token;
    }

public:

    xml_loading_adapter(Chordia::xml_source& xmls, // medium
            const std::string& root, // root tag
            int version // expected version
    ) :
        loading_adapter(version), m_xmls(xmls)
    {
        // doc_start
        m_token = m_parser.parse1(m_xmls);
        // now read until we find root tag ... JME audit this is a bit grim
        bool done = false;
        while (!done)
        {
            m_token = m_parser.parse2(m_xmls, m_token);
            if (root == m_parser.get_tag())
            {
                done = true;
            }
            else if (m_token == Chordia::xml_parser::eof)
            {
                std::stringstream strs;
                strs << "unexpected end of file looking for tag " << root;
                //throw(std::exception(strs.str().c_str()));
                throw(std::exception());
            }
        }
    }
};

} // end namespace

#if defined(_MSC_VER)
#pragma warning(pop)
#endif

#endif
