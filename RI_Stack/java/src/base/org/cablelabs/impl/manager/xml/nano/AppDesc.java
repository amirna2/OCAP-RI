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

import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.DirInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.FileInfo;
import org.cablelabs.impl.util.SystemEventUtil;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Parses the application description file.
 * 
 * @author Aaron Kamienski
 */
class AppDesc extends BasicXMLBuilder
{
    public Object getResult()
    {
        return getAppDescInfo();
    }

    private AppDescriptionInfo info = null;

    private boolean valid = false;

    /**
     * Returns the application descripiton information that has been parsed.
     */
    public AppDescriptionInfo getAppDescInfo()
    {
        return (valid) ? info : null;
    }

    /**
     * Constructs a <code>AppDesc</code> object, set to parse the permission
     * request file specified by the given <code>URL</code>
     */
    AppDesc() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
    }

    /**
     * Returns the starting state used when parsing the XML file.
     * 
     * @return the starting state used when parsing the XML file.
     */
    protected State createStartState()
    {
        return new EmptyState()
        {
            public State nextState(String name)
            {
                if ("applicationdescription".equals(name))
                    return new AppDescHandler();
                else
                    throw new FormatError("root should be fontdirectory, not " + name);
            }
        };
    }

    /**
     * Handles <applicationdescription>.
     */
    private class AppDescHandler extends DirState
    {
        public AppDescHandler()
        {
            super(null);
            info = new AppDescriptionInfo();
            valid = false;
        }

        public void start(String tag, Attributes attributes)
        {
            // Don't have attributes like DirState does
        }

        public void end()
        {
            info.files = makeArray();
            valid = true;
        }
    }

    /**
     * Handles &lt;dir&gt;|&lt;object&gt;.
     * <p>
     * There are some requirements placed upon the <code>name</code> attribute.
     * <ul>
     * <li>The value <i>SHALL NOT</i> contain characters outside the set defined
     * by the <i>pchar</i> term (see BNF for {@link org.ocap.net.OcapLocator}.
     * <li>The value <i>SHALL NOT</i> be <code>"."</code> or <code>".."</code>.
     * <li>Unless the value is <code>"*"</code>, it shall not contain
     * <code>'*'</code>.
     * </ul>
     * 
     * Also, support for unescaping '%'-escaped character bytes should be
     * supported.
     * 
     * @see "OCAP 12.2.8.2.2"
     */
    private class ObjectState extends AttribState
    {
        protected Vector parent;

        protected String name;

        public ObjectState(Vector parent)
        {
            this.parent = parent;
        }

        public void start(String tag, Attributes attr)
        {
            // Note: we currently toss the entire ADF on an invalid name.
            // ECO 881 actually says that only that XML element should be
            // tossed.
            // However, this is in conflict with requirement that all explicitly
            // listed files
            // be stored, else nothing is stored. (I think it is better to avoid
            // storage
            // altogether if a file is listed explicitly incorrectly.)

            name = attr.getValue("name");
            if (name == null)
            {
                throw new FormatError("name is required");
            }

            // Unescape '%' sequences per OCAP 12.2.8.2.2 (ECO 0881-4)
            if (name.indexOf('%') != -1)
            {
                name = unescape(name);
            }
            // Validate name per OCAP 12.2.8.2.2 (ECO 0881-4)
            if (".".equals(name) || "..".equals(name) || (!"*".equals(name) && name.indexOf('*') >= 0))
            {
                throw new FormatError("Invalid name attribute value: " + name);
            }
        }

        // TODO: copied from OcapLocator... maybe a utility method would be
        // better?
        private String unescape(String str)
        {
            char[] chars = str.toCharArray();
            ByteArrayOutputStream stream = new ByteArrayOutputStream(chars.length);

            for (int i = 0; i < chars.length; ++i)
            {
                if (chars[i] == '%')
                {
                    if (i + 2 >= chars.length) throw new FormatError("Invalid escape sequence in '" + str + "'");
                    stream.write((byte) dehex(chars[i + 1], chars[i + 2]));
                    i += 2;
                }
                else
                {
                    stream.write(chars[i]);
                }
            }

            try
            {
                return stream.toString("UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                // The UTF-8 encoding should be available.. so this shouldn't
                // happen
                SystemEventUtil.logRecoverableError(e);
                return stream.toString();
            }
        }

        // TODO: copied from OcapLocator... maybe a utility method would be
        // better?
        private static final String hex = "0123456789abcdef";

        // TODO: copied from OcapLocator... maybe a utility method would be
        // better?
        private char dehex(char c1, char c0) throws FormatError
        {
            c1 = (char) hex.indexOf(Character.toLowerCase(c1));
            c0 = (char) hex.indexOf(Character.toLowerCase(c0));
            if (c1 < 0 || c0 < 0) throw new FormatError("Encountered non-hex character %" + c1 + c0);

            return (char) ((c1 * 16) + c0);
        }
    }

    /**
     * Handles <dir>.
     */
    private class DirState extends ObjectState
    {
        private Vector elements = new Vector();

        public DirState(Vector parent)
        {
            super(parent);
        }

        public State nextState(String str)
        {
            if ("dir".equals(str))
                return new DirState(elements);
            else if ("file".equals(str))
                return new FileState(elements);
            else
                return EmptyState.INSTANCE;
        }

        protected FileInfo[] makeArray()
        {
            FileInfo files[] = new FileInfo[elements.size()];
            elements.copyInto(files);
            return files;
        }

        public void end()
        {
            // Generate DirInfo
            DirInfo dir = info.new DirInfo();
            dir.name = name;
            dir.files = makeArray();

            // Add self to parent
            parent.addElement(dir);
        }
    }

    /**
     * Handles <file>.
     */
    private class FileState extends ObjectState
    {
        public FileState(Vector parent)
        {
            super(parent);
        }

        public void start(String tag, Attributes attr)
        {
            super.start(name, attr);

            String sizeStr = attr.getValue("size");
            if (sizeStr == null) throw new FormatError("size is required");

            long size = 0;
            if (!"*".equals(name))
            {
                try
                {
                    // Per 12.2.8.2.2 (ECN 06.0881-4), only decimal size is
                    // accepted
                    size = Long.parseLong(sizeStr);
                }
                catch (Exception e)
                {
                    throw new FormatError(e.getMessage());
                }
            }

            // Don't bother waiting for end, just add info right away
            // Generate FileInfo
            FileInfo file = info.new FileInfo();
            file.name = name;
            file.size = size;

            // Add self to parent
            parent.addElement(file);
        }
    }

    /**
     * An unchecked exception used in-place of a IOException. Thrown from
     * FontIndex when parsing a font index file.
     */
    static class FormatError extends Error
    {
        public FormatError(String msg)
        {
            super(msg);
        }

        public FormatError()
        {
            super();
        }

        public java.io.IOException getException()
        {
            return new java.io.IOException(getMessage());
        }
    }
}
