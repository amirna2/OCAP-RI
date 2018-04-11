/*
 // Copyright (C) 2003  Davis E. King (davisking@users.sourceforge.net)
 // License: Boost Software License   See LICENSE.txt for the full license.
 //
 // Modifed for use with ESS.
 //
 // ESS Extremely Simple Serialization for C++
 //
 // http://www.novadsp.com/ess
 */

#ifndef XMLPARSER_H
#define XMLPARSER_H

#include <string>
#include <iostream>
#include <sstream>
#include <list>
#include <stack>
#include <set>
#include <vector>
#include <string>

#ifndef EOF
#  define EOF -1
#endif

namespace Chordia
{

//template <typename T>
class xml_source
{
    typedef char T;
    const T* m_ps; // buffer start
    const T* m_pe; // buffer end
    size_t m_chars; // number of elements in buffer

    const T* m_pb; // current buffer pointer;

protected:

    void initialise(const T* ps, size_t chars)
    {
        m_ps = ps;
        m_pb = ps;
        m_pe = (m_pb + chars);
        m_chars = chars;
    }

public:

    // JME hack
    typedef T int_type;

    //
    xml_source(const T* ps, size_t chars) :
        m_ps(ps), m_pe(ps + chars), m_chars(chars), m_pb(ps)
    {

    }

    int_type get()
    {
        if (m_pb >= m_pe)
        {
            return 0;
        }
        int_type ret = *m_pb;
        m_pb++;
        return ret;
    }

    int_type peek()
    {
        return (m_pb < m_pe ? *m_pb : -1);
    }

    bool fail() const
    {
        return !(m_ps && m_pb && m_chars > 0 && m_ps < m_pe);
    }

    // more data?
    bool eof() const
    {
        return (m_pb >= m_pe);
    }

    std::ios::iostate exceptions() const
    {
        return (std::ios::iostate) 0;
    }
    void exceptions(std::ios::iostate)
    {
    }

};

#ifdef _HAS_WIN32_MEMMAP
class xml_memmap_source : public xml_source
{
    //
    Chordia::CFileMapping<char> m_ipfile;

public:

    xml_memmap_source(const std::string& name) :
    xml_source(0,0)
    {
        const char* pszName = name.c_str();
        if (m_ipfile.Open(pszName) == false)
        {
            CSysError se(::GetLastError());
            DBMSG(CMsg("Cannot open file %s\r\n%s",pszName,se.data()));
        }
        else
        {
            // check for UNICODE encodings UTF-8/16/32
            const char* psz = m_ipfile.data();
            unsigned long bytes = static_cast<unsigned long>(m_ipfile.GetSize());
            if ((unsigned char)psz[0] == 0xEF &&
                    (unsigned char)psz[1] == 0xBB &&
                    (unsigned char)psz[2] == 0xBF)
            {
                psz += 3;
                bytes -= 3;
            }

            // set up reader points etc
            initialise(psz,bytes);
        }
    }
};
#endif

// ----------------------------------------------------------------------------------------
typedef std::vector<std::pair<std::string, std::string> > attribute_list;
typedef std::vector<std::pair<std::string, std::string> >::iterator
        attribute_list_iterator;
typedef std::vector<std::pair<std::string, std::string> >::const_iterator
        const_attribute_list_iterator;

// ----------------------------------------------------------------------------------------
class xml_parser
{
    /*
     INITIAL VALUE
     dh_list.size() == 0
     eh_list.size() == 0

     CONVENTION
     dh_list == a sequence of pointers to all the document_handlers that
     have been added to the xml_parser
     eh_list == a sequence of pointers to all the error_handlers that
     have been added to the xml_parser

     use of template parameters:
     map is used to implement the attribute_list interface
     stack is used just inside the parse function
     seq_dh is used to make the dh_list member variable
     seq_eh is used to make the eh_list member variable
     !*/

public:

    xml_parser() :
        m_line_number(0)
    {
    }

    virtual ~xml_parser()
    {
    }

    unsigned long line_number() const
    {
        return m_line_number;
    }

    const char* token() const
    {
        return m_token_text.c_str();
    }

    const char* error_text() const
    {
        return m_error_text.c_str();
    }

private:

    unsigned long m_line_number;
    std::string m_token_text;
    std::string m_error_text;

    //-----------------------------------------------------------

public:

    enum token_type
    {
        no_token, //
        element_start, // the first tag of an element
        element_end, // the last tag of an element
        empty_element, // the singular tag of an empty element
        pi, // processing instruction
        chars, // the non-markup data between tags
        chars_cdata, // the data from a CDATA section
        dtd, // this token is for an entire dtd
        comment, // this is a token for comments
        doc_start,
        doc_end,
        eof, // this token is returned when we reach the end of input
        entity_error, // unknown
        error,
    // this token indicates that the tokenizer couldn't
    // determine which category the next token fits into
    };

    static const char* TokenType(int tt)
    {
        static char* szTypes[] =
        { (char *) "no_token", //
                (char *) "element_start", // the first tag of an element
                (char *) "element_end", // the last tag of an element
                (char *) "empty_element", // the singular tag of an empty element
                (char *) "pi", // processing instruction
                (char *) "chars", // the non-markup data between tags
                (char *) "chars_cdata", // the data from a CDATA section
                (char *) "dtd", // this token is for an entire dtd
                (char *) "comment", // this is a token for comments
                (char *) "doc_start", (char *) "doc_end", (char *) "eof", // this token is returned when we reach the end of input
                (char *) "entity_error", // unknown
                (char *) "error" // this token indicates that the tokenizer couldn't
                };
        return szTypes[tt];
    }

private:

    // restricted functions: assignment and copy construction
    xml_parser(xml_parser&);
    xml_parser& operator=(xml_parser&);

    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    // member function definitions
    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------

public:

private:

    std::stack<std::string> m_tags;
    attribute_list m_attributes;
    bool m_seen_root_tag; // this is true after we have seen the root tag
    int m_prev_token; // previous token type
    std::string m_chars_buf; // used to collect chars data between consecutive
    // name of root tag
    std::string m_root_tag;
    // name of current tag
    std::string m_tag;

public:

    const char* get_text()
    {
        // we should not have to buffer text unless an entity is contained within it
        // and textual substitution becomes neccessary
        return m_chars_buf.c_str();
    }

    size_t text_length()
    {
        return m_chars_buf.size();
    }

    // clear the text buffer
    void clear_text()
    {
        m_chars_buf.clear();
    }

    // true if tags match
    bool match_tag(const char* pszTag)
    {
        return (m_tag == pszTag);
    }

    // tag
    const char* get_tag()
    {
        return m_tag.c_str();
    }

    size_t tag_length()
    {
        return m_tag.size();
    }

    const_attribute_list_iterator begin_attributes()
    {
        return m_attributes.begin();
    }

    const_attribute_list_iterator end_attributes()
    {
        return m_attributes.end();
    }

    std::string find_attribute(const std::string& name)
    {
        std::string ret = "<none>";
        for (const_attribute_list_iterator ali = m_attributes.begin(); ali
                != m_attributes.end(); ++ali)
        {
            if (ali->first == name)
            {
                return ali->second;
            }
        }
        return ret;
    }

    // true if attribute found, false if not
    bool find_attribute(const std::string& name, std::string& value)
    {
        for (const_attribute_list_iterator ali = m_attributes.begin(); ali
                != m_attributes.end(); ++ali)
        {
            if (ali->first == name)
            {
                value = ali->second;
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------
    int parse1(xml_source& in)
    {
        m_root_tag.clear();
        m_chars_buf.clear();
        m_seen_root_tag = false; // this is true after we have seen the root tag

        // clear tag vector
        while (m_tags.empty() == false)
        {
            m_tags.pop();
        }

        // skip any whitespace before the start of the document
        while (in.peek() == ' ' || in.peek() == '\t' || in.peek() == '\n'
                || in.peek() == '\r')
        {
            in.get();
        }

        //
        m_line_number = 1;
        m_prev_token = no_token;

        //
        return (in.eof() ? eof : doc_start);
    }

    //
    int parse2(xml_source& in, int prevToken)
    {
        if (prevToken == xml_parser::empty_element || prevToken
                == Chordia::xml_parser::element_end)
        {
            // clear accumulated text buffer
            clear_text();
        }
        //
        return parse2(in);
    }

protected:

    // called until
    int parse2(xml_source& in)
    {
        // chars and chars_cdata tokens so that
        // document_handlers receive all chars data between
        // tags in one call
        // variables to be used with the parsing functions
        std::string target;
        std::string data;
        // variables to use with the get_next_token() function
        int token_kind;
        int status = 0;
        if (get_next_token(in, m_token_text, token_kind, m_line_number)
                == false)
        {
            return EOF;
        }

        // DBMSG(CMsg("Line %d token %d %s",m_line_number+1,token_kind,m_token_text.c_str()));
        switch (token_kind)
        {
        case empty_element:

        case element_start:
        {
            //
            m_attributes.clear();
            status = parse_element(m_token_text, m_tag, m_attributes);
            // if there was no error parsing the element
            if (status == 0)
            {
                if (m_seen_root_tag == false)
                {
                    m_root_tag = m_tag;
                    m_seen_root_tag = true;
                }
            }
            else
            {
                m_error_text = "parse_element: ";
                //throw std::exception(m_error_text.c_str());
                throw std::exception();
            }

            // if this is an element_start token then push the name of
            // the element on to the stack
            if (token_kind == element_start)
            {
                m_tags.push(m_tag);
            }
        }
            break;

            // ----------------------------------------
        case element_end:
        {
            status = parse_element_end(m_token_text, m_tag);
            // if there was no error parsing the element
            if (status == 0)
            {
                // closing tag with no open
                if (m_tags.size() == 0)
                {
                    // they don't match so signal a fatal error
                    m_error_text = "unmatched tag - close with matched open";
                    //throw std::exception(m_error_text.c_str());
                    throw std::exception();
                }
                // unmatched tag
                else if (m_tag != m_tags.top())
                {
                    m_error_text
                            = "unmatched tag - close does not match previous open: ";
                    m_error_text += m_tags.top();
                    //throw std::exception(m_error_text.c_str());
                    throw std::exception();
                }
                else
                {
                    // they match so throw away this element name
                    m_tags.pop();
                }
            }
            else
            {
                //seen_fatal_error = true;
                m_error_text = "parse_element_end";
                //throw std::exception(m_error_text.c_str());
                throw std::exception();
            }
        }
            break;

            // ----------------------------------------
        case pi:
        {
            status = parse_pi(m_token_text, target, data);
            while (in.peek() == ' ' || in.peek() == '\t' || in.peek() == '\n'
                    || in.peek() == '\r')
            {
                in.get();
            }
        }
            break;

            //----------------------------------------
        case chars:
        {
            if (m_seen_root_tag)
            {
                char ch = '\0';
                for (size_t s = 0; s < m_token_text.size(); s++)
                {
                    ch = m_token_text[s];
                    switch (ch)
                    {
                    case '\r':
                    case '\n':
                    case '\t':
                        break;
                    default:
                        m_chars_buf += ch;
                    }
                }
            }
            else if (m_token_text.find_first_not_of(" \t\r\n")
                    != std::string::npos)
            {
                // you can't have non whitespace chars data outside the root element
                m_error_text
                        = "found non whitespace chars data outside the root element ";
                m_error_text += m_token_text;
                //throw std::exception(m_error_text.c_str());
                throw std::exception();
            }
        }
            break;

            //----------------------------------------
        case chars_cdata:
        {
            if (m_tags.size() != 0)
            {
                m_chars_buf += m_token_text;
            }
            else
            {
                // you can't have chars_data outside the root element
                m_error_text = "found CDATA data outside the root element ";
                //throw std::exception(m_error_text.c_str());
                throw std::exception();
            }
        }
            break;

            //----------------------------------------
        case eof:
            // special case ...
            break;

            //----------------------------------------
        case error:
        {
            std::stringstream strs;
            strs << "Lexical error in document at line " << m_line_number
                    << ": " << m_token_text;
            //throw std::exception(strs.str().c_str());
            throw std::exception();
        }
            break;

            //----------------------------------------

        case dtd: // fall though
        case comment: // do nothing
            break;
        } // end case
        // stash for the next iteration
        m_prev_token = token_kind;
        //
        return token_kind;
    }

    // ----------------------------------------------------------------------------------------
    bool get_next_token(xml_source& in, std::string& token_text,
            int& token_kind, unsigned long& line_number)
    {
        token_text.erase();
        xml_source::int_type ch1 = in.get();
        xml_source::int_type ch2;
        switch (ch1)
        {
        // -----------------------------------------
        // this is the start of some kind of a tag
        case '<':
        {
            ch2 = in.get();
            switch (ch2)
            {
            // ---------------------------------
            // this is a dtd, comment, or chars_cdata token
            case '!':
            {
                // if this is a CDATA section *******************************
                if (in.peek() == '[')
                {
                    token_kind = chars_cdata;

                    // throw away the '['
                    in.get();

                    // make sure the next chars are CDATA[
                    xml_source::int_type ch = in.get();
                    if (ch != 'C')
                        token_kind = error;
                    ch = in.get();
                    if (ch != 'D')
                        token_kind = error;
                    ch = in.get();
                    if (ch != 'A')
                        token_kind = error;
                    ch = in.get();
                    if (ch != 'T')
                        token_kind = error;
                    ch = in.get();
                    if (ch != 'A')
                        token_kind = error;
                    ch = in.get();
                    if (ch != '[')
                        token_kind = error;
                    // if this is an error token then end
                    if (token_kind == error)
                        break;

                    // get the rest of the chars and put them into token_text
                    int brackets_seen = 0; // this is the number of ']' chars
                    // we have seen in a row
                    bool seen_closing = false; // true if we have seen ]]>
                    do
                    {
                        ch = in.get();

                        if (ch == '\n')
                            ++line_number;

                        token_text += ch;

                        // if this is the closing
                        if (brackets_seen == 2 && ch == '>')
                            seen_closing = true;
                        // if we are seeing a bracket
                        else if (ch == ']')
                            ++brackets_seen;
                        // if we didn't see a bracket
                        else
                            brackets_seen = 0;

                    } while ((!seen_closing) && (ch != EOF));

                    // check if this is an error token
                    if (ch == EOF)
                    {
                        token_kind = error;
                    }
                    else
                    {
                        token_text.erase(token_text.size() - 3);
                    }
                }
                // this is a comment token ****************************
                else if (in.peek() == '-')
                {
                    token_text += ch1;
                    token_text += ch2;
                    token_text += '-';
                    token_kind = comment;
                    // throw away the '-' char
                    in.get();
                    // make sure the next char is another '-'
                    xml_source::int_type ch = in.get();
                    if (ch != '-')
                    {
                        token_kind = error;
                        break;
                    }

                    token_text += '-';
                    // get the rest of the chars and put them into token_text
                    int hyphens_seen = 0; // this is the number of '-' chars
                    // we have seen in a row
                    bool seen_closing = false; // true if we have seen ]]>
                    do
                    {
                        ch = in.get();

                        if (ch == '\n')
                            ++line_number;

                        token_text += ch;

                        // if this should be a closing block
                        if (hyphens_seen == 2)
                        {
                            if (ch == '>')
                                seen_closing = true;
                            else
                                // this isn't a closing so make it signal error
                                ch = EOF;
                        }
                        // if we are seeing a hyphen
                        else if (ch == '-')
                            ++hyphens_seen;
                        // if we didn't see a hyphen
                        else
                            hyphens_seen = 0;

                    } while ((!seen_closing) && (ch != EOF));

                    // check if this is an error token
                    if (ch == EOF)
                    {
                        token_kind = error;
                    }
                }
                else // this is a dtd token *************************
                {
                    token_text += ch1;
                    token_text += ch2;
                    int bracket_depth = 1; // this is the number of '<' chars seen
                    // minus the number of '>' chars seen

                    xml_source::int_type ch;
                    do
                    {
                        ch = in.get();
                        if (ch == '>')
                            --bracket_depth;
                        else if (ch == '<')
                            ++bracket_depth;
                        else if (ch == '\n')
                            ++line_number;

                        token_text += ch;

                    } while ((bracket_depth > 0) && (ch != EOF));

                    // make sure we didn't just hit EOF
                    if (bracket_depth == 0)
                    {
                        token_kind = dtd;
                    }
                    else
                    {
                        token_kind = error;
                    }
                }
            }
                break;

                // ---------------------------------
                // this is a pi token
            case '?':
            {
                token_text += ch1;
                token_text += ch2;
                xml_source::int_type ch;

                do
                {
                    ch = in.get();
                    token_text += ch;
                    if (ch == '\n')
                        ++line_number;
                    // else if we hit a < then thats an error
                    else if (ch == '<')
                        ch = EOF;
                } while (ch != '>' && ch != EOF);
                // if we hit the end of the pi
                if (ch == '>')
                {
                    // make sure there was a trailing '?'
                    if ((token_text.size() > 3)
                            && (token_text[token_text.size() - 2] != '?'))
                    {
                        token_kind = error;
                    }
                    else
                    {
                        token_kind = pi;
                    }
                }
                // if we hit EOF unexpectedly then error
                else
                {
                    token_kind = error;
                }
            }
                break;
                // ---------------------------------
                // this is an error token
            case EOF:
            {
                token_kind = error;
            }
                break;

                // ---------------------------------
                // this is an element_end token
            case '/':
            {
                token_kind = element_end;
                token_text += ch1;
                token_text += ch2;
                xml_source::int_type ch;
                do
                {
                    ch = in.get();
                    if (ch == '\n')
                        ++line_number;
                    // else if we hit a < then thats an error
                    else if (ch == '<')
                        ch = EOF;
                    token_text += ch;
                } while ((ch != '>') && (ch != EOF));

                // check if this is an error token
                if (ch == EOF)
                {
                    token_kind = error;
                }
            }
                break;
                // ---------------------------------
                // this is an element_start or empty_element token
            default:
            {
                token_text += ch1;
                token_text += ch2;
                xml_source::int_type ch = '\0';
                xml_source::int_type last;
                do
                {
                    last = ch;
                    ch = in.get();
                    if (ch == '\n')
                        ++line_number;
                    // else if we hit a < then thats an error
                    else if (ch == '<')
                        ch = EOF;
                    token_text += ch;
                } while ((ch != '>') && (ch != EOF));

                // check if this is an error token
                if (ch == EOF)
                {
                    token_kind = error;
                }
                // if this is an empty_element
                else if (last == '/')
                {
                    token_kind = empty_element;
                }
                else
                {
                    token_kind = element_start;
                }
            }
                break;
            }
        }
            break;
            // -----------------------------------------
            // this is an eof token
        case EOF:
        {
            token_kind = eof;
        }
            break;

            // -----------------------------------------
            // this is a chars token
        default:
        {
            if (ch1 == '\n')
            {
                ++line_number;
                token_text += ch1;
            }
            // if the first thing in this chars token is an entity reference
            else if (ch1 == '&')
            {
                // JME audit for &nbsp;
                int temp = change_entity(in);
                if (temp == -1)
                {
                    token_kind = entity_error;
                    break;
                }
                else
                {
                    token_text += (char) temp;
                }
            }
            else
            {
                token_text += ch1;
            }
            token_kind = chars;
            xml_source::int_type ch = 0;
            while (in.peek() != '<' && in.peek() != EOF)
            {
                ch = in.get();
                if (ch == '\n')
                {
                    ++line_number;
                }
                // if this is one of the predefined entity references then change it
                if (ch == '&')
                {
                    // JME audit for &nbsp;
                    int temp = change_entity(in);
                    if (temp == -1)
                    {
                        ch = EOF;
                        break;
                    }
                    else
                    {
                        token_text += (char) temp;
                    }
                }
                else
                {
                    token_text += ch;
                }
            }

            // if this is an error token
            if (ch == EOF)
            {
                token_kind = error;
            }
        }
            break;
        }
        //
        return (in.eof() == false);
    }

    // ----------------------------------------------------------------------------------------
    int parse_element(const std::string& token, std::string& name,
            attribute_list& atts)
    {
        name.erase();
        atts.clear();

        // there must be at least one character between the <>
        if (token[1] == '>')
        {
            return -1;
        }

        std::string::size_type i;
        xml_source::int_type ch = token[1];
        i = 2;

        // fill out name.  the name can not contain any of the following characters
        while ((ch != '>') && (ch != ' ') && (ch != '=') && (ch != '/') && (ch
                != '\t') && (ch != '\r') && (ch != '\n'))
        {
            name += ch;
            ch = token[i];
            ++i;
        }

        // skip any whitespaces
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
        {
            ch = token[i];
            ++i;
        }

        // JME map for duplicate checking
        std::set<std::string> mapped;
        // find any attributes
        while (ch != '>' && ch != '/')
        {
            std::string attribute_name;
            std::string attribute_value;

            // fill out attribute_name
            while ((ch != '=') && (ch != ' ') && (ch != '\t') && (ch != '\r')
                    && (ch != '\n') && (ch != '>'))
            {
                attribute_name += ch;
                ch = token[i];
                ++i;
            }

            // you can't have empty attribute names
            if (attribute_name.size() == 0)
                return -1;

            // if we hit > too early then return error
            if (ch == '>')
            {
                return -1;
            }

            // skip any whitespaces
            while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
            {
                ch = token[i];
                ++i;
            }

            // the next char should be a '=', error if it's not
            if (ch != '=')
            {
                return -1;
            }

            // get the next char
            ch = token[i];
            ++i;

            // skip any whitespaces
            while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
            {
                ch = token[i];
                ++i;
            }

            // get the delimiter for the attribute value
            xml_source::int_type delimiter = ch; // this should be either a ' or " character
            ch = token[i]; // get the next char
            ++i;
            if (delimiter != '\'' && delimiter != '"')
            {
                return -1;
            }

            // fill out attribute_value
            while ((ch != delimiter) && (ch != '>'))
            {
                attribute_value += ch;
                ch = token[i];
                ++i;
            }

            // if there was no delimiter then this is an error
            if (ch == '>')
            {
                return -1;
            }

            // go to the next char
            ch = token[i];
            ++i;

            // the next char must be either a '>' or '/' (denoting the end of the tag)
            // or a white space character
            if (ch != '>' && ch != ' ' && ch != '/' && ch != '\t' && ch != '\n'
                    && ch != '\r')
            {
                return -1;
            }

            // skip any whitespaces
            while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
            {
                ch = token[i];
                ++i;
            }

            // add attribute_value and attribute_name to atts
            if (mapped.find(attribute_name) != mapped.end())
            {
                //
                std::string error = attribute_name;
                error += " is already defined";
                //throw std::exception(error.c_str());
                throw std::exception();
                // attributes may not be multiply defined
            }
            else
            {
                atts.push_back(std::make_pair(attribute_name, attribute_value));
            }
        }

        // you can't have an element with no name
        if (name.size() == 0)
        {
            return -1;
        }
        return 0;
    }

    // ----------------------------------------------------------------------------------------
    int parse_pi(const std::string& token, std::string& target,
            std::string& data)
    {
        target.erase();
        data.erase();

        xml_source::int_type ch = token[2];
        std::string::size_type i = 3;
        while (ch != ' ' && ch != '?' && ch != '\t' && ch != '\n' && ch != '\r')
        {
            target += ch;
            ch = token[i];
            ++i;
        }
        if (target.size() == 0)
        {
            return -1;
        }

        // if we aren't at a ? character then go to the next character
        if (ch != '?')
        {
            ch = token[i];
            ++i;
        }

        // if we still aren't at the end of the processing instruction then
        // set this stuff in the data section
        while (ch != '?')
        {
            data += ch;
            ch = token[i];
            ++i;
        }

        return 0;
    }

    // ----------------------------------------------------------------------------------------
    int parse_element_end(const std::string& token, std::string& name)
    {
        name.erase();
        std::string::size_type end = token.size() - 1;
        for (std::string::size_type i = 2; i < end; ++i)
        {
            if (token[i] == ' ' || token[i] == '\t' || token[i] == '\n'
                    || token[i] == '\r')
                break;
            name += token[i];
        }

        if (name.size() == 0)
        {
            return -1;
        }

        return 0;
    }

    //----------------------------------------------------------------------------------------
    // JME handles entities - need to add handler for HTML variants
    int change_entity(xml_source& in)
    {
        std::string ent;
        char ch = in.get();
        while (ch && ch != ';')
        {
            ent += ch;
            ch = in.get();
        }

        if (ent == "&#163;")
        {
            //return '�';
            return 0x3f;
        }
        else if (ent == "&apos;")
        {
            return '\'';
        }
        else if (ent == "&lt;")
        {
            return '<';
        }
        return (ch == EOF ? -1 : ' ');
    }
};
}

#endif // XMLPARSER_H
