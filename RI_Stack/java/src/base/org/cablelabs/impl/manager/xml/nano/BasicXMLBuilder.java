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

package org.cablelabs.impl.manager.xml.nano;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Stack;

import net.n3.nanoxml.IXMLBuilder;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLException;
import net.n3.nanoxml.XMLParserFactory;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;

/**
 * Base class NanoXML builder used to parse an XML file. This implementation
 * maintains a stack of states corresponding to the current element. Each state
 * specifies the sub-elements that it accepts as well as handler for the start
 * and end tags, as well as character data.
 * <p>
 * The implementation was ported from the original SAX implementation (see
 * org.cablelabs.impl.manager.xml.sax.BasicXMLHandler).
 * 
 * @author Aaron Kamienski
 */
abstract class BasicXMLBuilder implements IXMLBuilder
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(BasicXMLBuilder.class.getName());

    protected Stack context;

    protected IXMLParser parser;

    /**
     * Constructs a <code>BasicXMLBuilder</code> object.
     */
    BasicXMLBuilder() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        parser = NanoUtil.createParser();
        parser.setBuilder(this);
        /*
         * parser.setValidator(new NonValidator() { public void parseDTD(String
         * publicID, IXMLReader reader, IXMLEntityResolver entityResolver,
         * boolean external) { // ignore the dtd } });
         */

        context = new Stack();
        context.push(createStartState());
    }

    /**
     * Instructs the object to parse the XML file specified by the given
     * <code>InputStream</code>.
     * 
     * @param is
     *            the <code>InputStream</code> to parse
     */
    public void go(InputStream is) throws IOException, XMLException
    {
        parser.setReader(new StdXMLReader(is));
        parser.parse();
    }

    /**
     * Returns the starting state used when parsing the XML file.
     * 
     * @return the starting state used when parsing the XML file.
     */
    protected abstract State createStartState();

    // ==================== IXMLBuilder ====================

    /**
     * This method is called before the parser starts processing its input.
     * 
     * @param systemID
     *            the system ID of the XML data source.
     * @param lineNr
     *            the line on which the parsing starts.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void startBuilding(String systemID, int lineNr) throws Exception
    {
        // Does nothing
    }

    /**
     * This method is called when a processing instruction is encountered. A PI
     * with a reserved target ("xml" with any case) is never reported.
     * 
     * @param target
     *            the processing instruction target.
     * @param reader
     *            the method can retrieve the parameter of the PI from this
     *            reader. You may close the reader before reading all its data
     *            and you cannot read too much data.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void newProcessingInstruction(String target, Reader reader) throws Exception
    {
        // Does nothing
    }

    /**
     * This method is called when a new XML element is encountered.
     * 
     * @see #endElement
     * 
     * @param name
     *            the name of the element.
     * @param nsPrefix
     *            the prefix used to identify the namespace. If no namespace has
     *            been specified, this parameter is null.
     * @param nsURI
     *            the URI associated with the namespace. If no namespace has
     *            been specified, or no URI is associated with nsPrefix, this
     *            parameter is null.
     * @param systemID
     *            the system ID of the XML data source.
     * @param lineNr
     *            the line in the source where the element starts.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception
    {
        // Handle state change
        // Call startElement for new state
        State state = ((State) context.peek()).nextState(nsPrefix, name);
        context.push(state);
        state.start(name, nsPrefix, nsURI, lineNr);
    }

    /**
     * This method is called when a new attribute of an XML element is
     * encountered.
     * 
     * @param key
     *            the key (name) of the attribute.
     * @param nsPrefix
     *            the prefix used to identify the namespace. If no namespace has
     *            been specified, this parameter is null.
     * @param nsURI
     *            the URI associated with the namespace. If no namespace has
     *            been specified, or no URI is associated with nsPrefix, this
     *            parameter is null.
     * @param value
     *            the value of the attribute.
     * @param type
     *            the type of the attribute. If no type is known, "CDATA" is
     *            returned.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception
    {
        State state = (State) context.peek();
        state.attribute(key, nsPrefix, nsURI, value, type);
    }

    /**
     * This method is called when the attributes of an XML element have been
     * processed.
     * 
     * @see #startElement
     * @see #addAttribute
     * 
     * @param name
     *            the name of the element.
     * @param nsPrefix
     *            the prefix used to identify the namespace. If no namespace has
     *            been specified, this parameter is null.
     * @param nsURI
     *            the URI associated with the namespace. If no namespace has
     *            been specified, or no URI is associated with nsPrefix, this
     *            parameter is null.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void elementAttributesProcessed(String name, String nsPrefix, String nsURI) throws Exception
    {
        State state = (State) context.peek();
        state.attributesDone(name, nsPrefix, nsURI);
    }

    /**
     * This method is called when the end of an XML elemnt is encountered.
     * 
     * @see #startElement
     * 
     * @param name
     *            the name of the element.
     * @param nsPrefix
     *            the prefix used to identify the namespace. If no namespace has
     *            been specified, this parameter is null.
     * @param nsURI
     *            the URI associated with the namespace. If no namespace has
     *            been specified, or no URI is associated with nsPrefix, this
     *            parameter is null.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void endElement(String name, String nsPrefix, String nsURI) throws Exception
    {
        State state = (State) context.pop();
        state.end();
    }

    /**
     * This method is called when a PCDATA element is encountered. A Java reader
     * is supplied from which you can read the data. The reader will only read
     * the data of the element. You don't need to check for boundaries. If you
     * don't read the full element, the rest of the data is skipped. You also
     * don't have to care about entities: they are resolved by the parser.
     * 
     * @param reader
     *            the method can retrieve the data from this reader. You may
     *            close the reader before reading all its data and you cannot
     *            read too much data.
     * @param systemID
     *            the system ID of the XML data source.
     * @param lineNr
     *            the line in the source where the element starts.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public void addPCData(Reader reader, String systemID, int lineNr) throws Exception
    {
        State state = (State) context.peek();
        state.chars(reader);
    }

    /**
     * Returns the result of the building process. This method is called just
     * before the <I>parse</I> method of IXMLParser returns.
     * 
     * @see net.n3.nanoxml.IXMLParser#parse
     * 
     * @return the result of the building process.
     * 
     * @throws java.lang.Exception
     *             If an exception occurred while processing the event.
     */
    public abstract Object getResult() throws Exception;

    // ==================== Utility ====================

    /**
     * Utility method used to parse hexadecimal integers.
     */
    protected static int parseInt(String str) throws NumberFormatException
    {
        if (!str.startsWith("0x")) throw new NumberFormatException("Hex value must start with '0x'");
        return Integer.parseInt(str.substring(2), 16);
    }

    /**
     * Utility method used to parse hexadecimal longs.
     */
    protected static long parseLong(String str) throws NumberFormatException
    {
        if (!str.startsWith("0x")) throw new NumberFormatException("Hex value must start with '0x'");
        return Long.parseLong(str.substring(2), 16);
    }

    /**
     * Utility method used to parse 48-bit application identifiers.
     */
    protected static AppID appid(String str)
    {
        long val = parseLong(str);
        return new AppID((int) ((val >> 16) & 0xFFFFFFFF), (int) (val & 0xFFFF));
    }

    /**
     * JDK118 and PJava doesn't have toURL, so we must emulate our own!
     */
    protected static URL toURL(File f) throws MalformedURLException
    {
        String path = f.getAbsolutePath().replace(File.separatorChar, '/');
        if (f.isDirectory() && !path.endsWith("/")) path = path + '/';
        if (!path.startsWith("/")) path = '/' + path;
        return new URL("file", "", path);
    }

    /**
     * This interface is implemented by parsing state objects. Each
     * <code>State</code> instance represents the parsing of an XML tag. It is
     * responsible for returning new state objects for sub-elements via the
     * {@link #nextState} method.
     */
    protected interface State
    {
        /**
         * Called after pushing this state onto the state stack. Only called
         * when this state is the current state and when newly pushed onto the
         * stack.
         */
        public void start(String name, String nsPrefix, String nsURI, int lineNr);

        /**
         * Called when encountering a new attribute. Only called when this state
         * is the current state and after {@link #start} has been called.
         */
        public void attribute(String key, String nsPrefix, String nsURI, String value, String type);

        /**
         * Called after all attributes have been read.
         */
        public void attributesDone(String name, String nsPrefix, String nsURI);

        /**
         * Called after popping this state from the state stack.
         */
        public void end();

        /**
         * Called when this state is the current state.
         */
        public void chars(Reader reader) throws IOException;

        /**
         * Called when this state is the current state, to determine what state
         * should follow given the next element tag name.
         * 
         * @param ns
         *            the namespace prefix of the sub-element tag
         * @param tag
         *            the name of the sub-element tag
         * @return the next state that should follow; should not be
         *         <code>null</code>
         */
        public State nextState(String ns, String tag);
    }

    /**
     * The default implementation of <code>State</code> which doesn't do
     * anything. A singleton instance if provided by the <code>INSTANCE</code>
     * class variable. This should be used as a base class for other states to
     * simplify their implementations.
     */
    protected static class EmptyState implements State
    {
        public static final EmptyState INSTANCE = new EmptyState();

        /**
         * Calls {@link #start()}.
         */
        public void start(String name, String nsPrefix, String nsURI, int lineNr)
        {
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Start: name=" + name + " pfx=" + nsPrefix + " uri=" + nsURI + " :" + lineNr);
                }
            }
            start();
        }

        /**
         * Called after pushing this state onto the stack by
         * {@link #start(String,String,String,int)}. Parses the attributes.
         */
        public void start()
        { /* empty */
        }

        /**
         * Calls {@link #attribute(String key, String value, String type)}
         */
        public void attribute(String key, String nsPrefix, String nsURI, String value, String type)
        {
            attribute(nsPrefix, key, value, type);
        }

        public void attribute(String ns, String key, String value, String type)
        {
            if (ns != null) key = ns + ":" + key;
            attribute(key, value, type);
        }

        public void attribute(String name, String value, String type)
        { /* to-be-overridden */
        }

        public void attributesDone(String name, String nsPrefix, String nsURI)
        { /* to-be-overridden */
        }

        public void end()
        { /* to-be-overridden */
        }

        public void chars(Reader reader) throws IOException
        { /* to-be-overridden */
        }

        public State nextState(String ns, String str)
        {
            if (ns != null) str = ns + ":" + str;
            return nextState(str);
        }

        public State nextState(String str)
        {
            return INSTANCE;
        }
    }

    /**
     * A basic extension of EmptyState which builds up a set of attributes.
     */
    protected static abstract class AttribState extends EmptyState
    {
        protected Attributes attribs = new Attributes();

        /**
         * Saves the attribute in the attributes Hashtable.
         */
        public void attribute(String ns, String key, String value, String type)
        {
            if (ns != null) key = ns + ":" + key;
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("  attrib: " + key + "=" + value);
                }
            }
            attribs.setValue(key, value);
        }

        public void attributesDone(String name, String nsPrefix, String nsURI)
        {
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("End Attrib: name=" + name + " " + attribs);
                }
            }
            start(name, attribs);
        }

        public abstract void start(String name, Attributes attributes);

        public static class Attributes extends Hashtable
        {
            public String getValue(String key)
            {
                return (String) get(key);
            }

            public String getValue(String key, String defValue)
            {
                String value = getValue(key);
                return (value != null) ? value : defValue;
            }

            public void setValue(String key, String value)
            {
                put(key, value);
            }
        }
    }

    /**
     * A basic extension of EmptyState which builds up a string.
     */
    protected static abstract class StringState extends EmptyState
    {
        protected StringBuffer buf = new StringBuffer();

        public void chars(Reader reader) throws IOException
        {
            char[] charArray = new char[256];
            int nbytes = 0;

            while ((nbytes = reader.read(charArray)) != -1)
            {
                chars(charArray, 0, nbytes);
            }
        }

        public void chars(char chars[], int start, int length)
        {
            buf.append(chars, start, length);
        }
    }

    /**
     * An extension of AttribState which builds up strings as well as
     * attributes.
     */
    protected static abstract class AttribStringState extends AttribState
    {
        protected StringBuffer buf = new StringBuffer();

        public void chars(Reader reader) throws IOException
        {
            char[] charArray = new char[256];
            int nbytes = 0;

            while ((nbytes = reader.read(charArray)) != -1)
            {
                chars(charArray, 0, nbytes);
            }
        }

        public void chars(char chars[], int start, int length)
        {
            buf.append(chars, start, length);
        }
    }

    private static final boolean DEBUG = false;
}
