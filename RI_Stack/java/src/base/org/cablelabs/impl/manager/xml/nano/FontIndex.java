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

import org.cablelabs.impl.manager.XmlManager.FontInfo;
import java.awt.Font;
import java.util.BitSet;
import java.util.Vector;

/**
 * Parses a font index file.
 * <p>
 * The implementation was ported from the original SAX implementation (see
 * org.cablelabs.impl.manager.xml.sax.FontIndex).
 * 
 * @author Aaron Kamienski
 */
class FontIndex extends BasicXMLBuilder
{
    private Vector fonts;

    private FontInfo[] fontsarray = null; // will be null until we are done

    public Object getResult()
    {
        return getFontInfo();
    }

    /**
     * Returns the font information that has been parsed.
     */
    public FontInfo[] getFontInfo()
    {
        return fontsarray;
    }

    /**
     * Constructs a <code>FontIndex</code> object, set to parse the permission
     * request file specified by the given <code>URL</code>
     */
    FontIndex() throws ClassNotFoundException, InstantiationException, IllegalAccessException
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
                if ("fontdirectory".equals(name))
                    return new FontDirHandler();
                else
                    throw new FormatError("root should be fontdirectory, not " + name);
            }
        };
    }

    /**
     * Handles <fontdirectory>.
     */
    private class FontDirHandler extends EmptyState
    {
        public State nextState(String str)
        {
            if ("font".equals(str))
                return new FontState();
            else
                return EmptyState.INSTANCE;
        }

        public void start()
        {
            fonts = new Vector();
        }

        public void end()
        {
            fontsarray = new FontInfo[fonts.size()];
            fonts.copyInto(fontsarray);
        }
    }

    /**
     * Used to intialize FontInfo.style if no <style> element is presented.
     */
    private static final BitSet ALL_STYLES;
    static
    {
        ALL_STYLES = new BitSet(4);
        ALL_STYLES.set(Font.PLAIN);
        ALL_STYLES.set(Font.BOLD);
        ALL_STYLES.set(Font.ITALIC);
        ALL_STYLES.set(Font.BOLD | Font.ITALIC);
    }

    /**
     * Handles <font>.
     */
    private class FontState extends EmptyState
    {
        private FontInfo fontinfo;

        public void start()
        {
            fontinfo = new FontInfo();
        }

        public State nextState(String str)
        {
            if ("name".equals(str))
                return new NameState();
            else if ("fontformat".equals(str))
                return new FormatState();
            else if ("filename".equals(str))
                return new FilenameState();
            else if ("style".equals(str))
                return new StyleState();
            else if ("size".equals(str))
                return new SizeState();
            else
                return EmptyState.INSTANCE;
        }

        public void end()
        {
            // save current styles
            if (fontinfo.style == null) fontinfo.style = ALL_STYLES;

            // save fontinfo
            fonts.addElement(fontinfo);
        }

        /**
         * Handles <name>.
         */
        private class NameState extends StringState
        {
            public void end()
            {
                // Simply use the last one found
                fontinfo.name = buf.toString().trim();
            }
        }

        /**
         * Handles <fontformat>.
         */
        private class FormatState extends StringState
        {
            public void end()
            {
                // Simply use the last one found
                fontinfo.format = buf.toString().trim();
            }
        }

        /**
         * Handles <filename>.
         */
        private class FilenameState extends StringState
        {
            public void end()
            {
                // Simply use the last one found
                fontinfo.filename = buf.toString().trim();
            }
        }

        /**
         * Handles <style>.
         */
        private class StyleState extends StringState
        {
            public void end()
            {
                String string = buf.toString().trim();
                int style = 0;

                if ("PLAIN".equals(string))
                    style = Font.PLAIN;
                else if ("BOLD".equals(string))
                    style = Font.BOLD;
                else if ("ITALIC".equals(string))
                    style = Font.ITALIC;
                else if ("BOLD_ITALIC".equals(string))
                    style = Font.BOLD | Font.ITALIC;
                else
                    throw new FormatError("Unknown style '" + string + "'");

                if (fontinfo.style == null) fontinfo.style = new BitSet();
                fontinfo.style.set(style);
            }
        }

        /**
         * Handles <size>.
         */
        private class SizeState extends AttribState
        {
            public void start(String name, Attributes attr)
            {
                String minStr = attr.getValue("min");
                fontinfo.min = (minStr == null) ? 0 : Integer.parseInt(minStr);

                String maxStr = attr.getValue("max");
                fontinfo.max = (maxStr == null || "maxint".equals(maxStr)) ? Integer.MAX_VALUE
                        : Integer.parseInt(maxStr);
            }
        }
    }

    /**
     * An unchecked exception used in-place of a FontFormatException. Thrown
     * from FontIndex when parsing a font index file.
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

        public org.dvb.ui.FontFormatException getException()
        {
            return new org.dvb.ui.FontFormatException(getMessage());
        }
    }
}
