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

package org.dvb.ui;

import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cablelabs.impl.dvb.ui.FontFactoryPeer;
import org.cablelabs.impl.dvb.ui.FontFactoryPeerFactory;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.XmlManager;
import org.cablelabs.impl.manager.XmlManager.FontInfo;

/**
 * Provides a mechanism for applications to instantiate fonts that are not built
 * into the system. The two constructors of this class allow fonts to be
 * downloaded either through the font index file of the application or directly
 * from a font file in the format(s) specified in the main body of the
 * present document.
 *
 * @author Aaron Kamienski
 **/
public class FontFactory
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(FontFactory.class.getName());

    /**
     * Constructs a FontFactory for the font index file bound to this
     * application in the application signalling. The call to the constructor is
     * synchronous and shall block until the font index file has been retrieved
     * or an an exception is thrown.
     *
     * @throws FontFormatException
     *             if there is an error in the font index file bound with the
     *             application.
     * @throws IOException
     *             if there is no font index file bound with the application, or
     *             if there is an error attempting to access the data in that
     *             file.
     */
    public FontFactory() throws FontFormatException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("FontFactory ctor");
        }
        // Find font index file in "base directory"
        // Base directory is mapped to "."
        File fontindex = new File("./ocap.fontindex"); // was dvb.fontindex for
                                                       // MHP

        // Initialize using font index file
        InputStream is = null;
        try
        {
            iniz(is = new FileInputStream(fontindex));
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    /**
     * Package private version of the default constructor. Exposed for testing
     * only.
     */
    FontFactory(InputStream is) throws IOException, FontFormatException
    {
        iniz(is);
    }

    /**
     * Initialize using font index file. Created so that it could be called by
     * both the default constructor and the testing-only
     * <code>InputStream</code> constructor.
     */
    private void iniz(InputStream is) throws FontFormatException, IOException
    {
        XmlManager xml = (XmlManager) ManagerManager.getInstance(XmlManager.class);

        // Read font index file
        fonts = xml.parseFontIndex(is);

        // Check for necessary information
        for (int i = 0; i < fonts.length; ++i)
        {
            if (fonts[i].name == null || fonts[i].name.length() == 0 || fonts[i].filename == null
                    || fonts[i].filename.length() == 0 || fonts[i].format == null // ||
                                                                                  // !"PFR".equals(fonts[i].format)
                    || fonts[i].min < 0 || fonts[i].max < 0 || fonts[i].min > fonts[i].max)
                throw new FontFormatException("Invalid or missing font index info");
        }

        // Create peer
        peer = FontFactoryPeerFactory.createFontFactoryPeer();
    }

    /**
     * Constructs a FontFactory for the font file found at the given location.
     * The call to the constructor is synchronous and shall block until the font
     * file has been retrieved or an exception is thrown.
     *
     * @param u
     *            The location of the font file
     *
     * @throws IOException
     *             if there is an error attempting to access the data referenced
     *             by the URL
     * @throws IllegalArgumentException
     *             if the URL is not both valid and supported
     * @throws SecurityException
     *             if access to the specified URL is denied by security policy
     * @throws FontFormatException
     *             if the file at that URL is not a valid font file as specified
     *             in the main body of this specification
     **/
    public FontFactory(URL u) throws IOException, FontFormatException
    {
        byte[] bytes = readFont(u);

        // create native peer
        peer = FontFactoryPeerFactory.createFontFactoryPeer();

        // add font file
        if (0 != peer.addFont(bytes, null, 0, 0, 0))
        {
            destroy();
            throw new FontFormatException();
        }
    }

    /**
     * Read font data from given URL.
     *
     * @param url
     *            location of font data
     * @return array of bytes containing font data
     *
     * @throws IOException
     *             if there is an error accessing the data from the URL
     * @throws SecurityException
     *             if access to the specified URL is denied by security policy
     */
    private byte[] readFont(URL url) throws IOException
    {
        InputStream in = null;
        byte[] bytes = null;

        // Read bytes into byte array
        try
        {
            in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytebuf = new byte[256];
            int n;

            while ((n = in.read(bytebuf, 0, bytebuf.length)) != -1)
                buffer.write(bytebuf, 0, n);

            in.close();
            bytes = buffer.toByteArray();
        }
        finally
        {
            if (in != null) try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }

        // Can we get here and bytes be null??
        if (bytes == null) throw new IOException();

        return bytes;
    }

    /**
     * Private deletion routine. Only called from failed constructor or
     * finalizer.
     */
    private void destroy()
    {
        FontFactoryPeer tmp = peer;
        if (tmp != null)
        {
            peer = null;
            tmp.dispose();
        }
    }

    /**
     * Finds and adds the specified font, if necessary. This is called by
     * createFont() for the font index case. Searches the list of fonts read
     * from the font index file for any matching entries. Each found entry is
     * added.
     * <p>
     * After an entry is added, it is forgotten so that it isn't added again.
     *
     * @param name
     *            font name to search for
     * @param style
     *            font style to search for
     * @param size
     *            font size to search for
     *
     * @throws IOException
     *             if the font file could not be read
     * @throws FontFormatException
     *             if the font file isn't valid
     *
     * @note this can be cleaned up... the array could be turned into a
     *       Vector...
     *
     * @note This is a little odd in that it does essentially the same work that
     *       the native peer does when the newFont() method is called. The whole
     *       font factory implementation <i>could</i> be pushed back up into
     *       Java. E.g., peer.addFont() would be used to generate some sort of
     *       font description handle and only that. Just an idea.
     */
    private void addFont(String name, int style, int size) throws IOException, FontFormatException
    {
        if (fonts == null) return;
        // If we have fonts that haven't been added, see if we have this font
        for (int i = 0; i < fonts.length; ++i)
        {
            if (fonts[i] != null // skip if already added
                    && name.equals(fonts[i].name) && size >= fonts[i].min
                    && size <= fonts[i].max
                    && fonts[i].style.get(style))
            {
                FontInfo tmp = fonts[i];
                fonts[i] = null; // don't try to add again
                addFont(tmp);
            }
        }
    }

    /**
     * Adds the given font read from a font index file.
     *
     * @throws IOException
     *             if the font file could not be read
     * @throws FontFormatException
     *             if the font file isn't valid
     */
    private void addFont(FontInfo font) throws IOException, FontFormatException
    {
        if (log.isDebugEnabled())
        {
            log.debug("addFont: " + font.filename);
        }
        // Read font data
        byte[] bytes = readFont(font.filename);

        // !!!Ick!!! Adds every style separately
        // Problem is that MPE FF API doesn't accept multiple styles
        for (int s = 0; s <= (Font.BOLD | Font.ITALIC); ++s)
        {
            if (!font.style.get(s)) continue;
            // Add font to the FontFactory.
            if (0 != peer.addFont(bytes, font.name, s, font.min, font.max))
            {
                throw new FontFormatException("Couldn't add " + font.name + "," + s + "," + font.min + "-" + font.max);
            }
        }
    }

    /**
     * Read font data from given filename.
     *
     * @param filename
     *            location of font data, relative to the application base
     *            directory (i.e., '.')
     * @return array of bytes containing font data
     *
     * @throws IOException
     *             if there is an error accessing the data from the file
     */
    private byte[] readFont(String filename) throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("readFont: " + filename);
        }
        FileInputStream in = null;
        try
        {
            File font = new File(filename);
            // cast to int okay, no way we will support >2G font files
            int nbytes = (int) font.length();
            // No BufferedInputStream, because we'll read all at once
            in = new FileInputStream(font);
            byte[] bytes = new byte[nbytes];

            // Read entire file into buffer.
            int pos = 0;
            int n;
            while (nbytes > 0 && (n = in.read(bytes, pos, nbytes)) != -1)
            {
                pos += n;
                nbytes -= n;
            }

            // return bytes
            return bytes;
        }
        finally
        {
            if (in != null) in.close();
        }
    }

    /**
     * Creates a font object from the font source associated with this
     * FontFactory. This font will remain valid even if the FontFactory is no
     * longer reachable from application code. The name returned by
     * Font.getName() might not be the same as the name supplied, for example,
     * it might have a string prepended to it that identifies the source
     * FontFactory in a platform-dependant manner. For FontFactory instances
     * bound to the font index file of an application, the call to the method is
     * synchronous and shall block until either an exception is thrown or any
     * required network access has completed.
     * <p>
     * The value of the style argument must be as defined in java.awt.Font.
     * Valid values are the following:
     * <ul>
     * <li><code>java.awt.Font.PLAIN</code>
     * <li><code>java.awt.Font.BOLD</code>
     * <li><code>java.awt.Font.ITALIC</code>
     * <li><code>java.awt.Font.BOLD + java.awt.Font.ITALIC</code>
     * </ul>
     *
     * @param name
     *            the font name
     * @param style
     *            the constant style used, such as java.awt.Font.PLAIN.
     * @param size
     *            the point size of the font
     *
     * @throws FontNotAvailableException
     *             if a font with given parameters cannot be located or created.
     * @throws IOException
     *             if there is an error retrieving a font from the network.
     *             Thrown only for font factory instances bound to the font
     *             index file of an application.
     * @throws IllegalArgumentException
     *             if the style parameter is not in the set of valid values, or
     *             if the size parameter is zero or negative.
     * @throws FontFormatException
     *             if the font file is not a valid font file as specified in the
     *             main body of this specification. Thrown only for font factory
     *             instances bound to the font index file of an application.
     **/
    public java.awt.Font createFont(String name, int style, int size) throws FontNotAvailableException,
            FontFormatException, IOException
    {
        // Should not happen unless garbage collected...
        // which wouldn't happen unless there were no references to this
        // object...
        // which would make it impossible for this method to be called...
        // So, shouldn't happen!
        if (peer == null) throw new java.awt.AWTError("Disposed of FontFactory");

        switch (style)
        {
            case java.awt.Font.PLAIN:
            case java.awt.Font.BOLD:
            case java.awt.Font.ITALIC:
            case java.awt.Font.BOLD + java.awt.Font.ITALIC:
                break;
            default:
                throw new IllegalArgumentException("Invalid style parameter");
        }

        if (size <= 0) throw new IllegalArgumentException("Size is negative or zero");

        // Look up previously created font or create new one
        Font font;
        Font key = new Font(name, style, size);

        // Look up previously created font
        if ((font = (Font) fontMap.get(key)) == null)
        {
            // Create new font and add to table
            if (fonts != null) addFont(name, style, size);
            font = peer.newFont(new LoadedFont(this, name, style, size));
            if (font != null) fontMap.put(key, font);
        }
        if (font == null) throw new FontNotAvailableException(name + "," + style + "," + size);
        return font;
    }

    /**
     * Finalizer deletes native peer implementation.
     */
    protected void finalize()
    {
        fontMap = null;

        destroy();
    }

    /**
     * Information about fonts loaded from font index file. Only set for
     * <code>FontFactory()</code> created using default constructor.
     */
    private FontInfo[] fonts;

    /**
     * The peer for this <code>FontFactory</code>.
     */
    private FontFactoryPeer peer;

    /**
     * Hashtable of font currently held by this font factory.
     */
    private java.util.Hashtable fontMap = new java.util.Hashtable();

    /**
     * Extends <code>java.awt.Font</code> to maintain a reference to the
     * creating <code>FontFactory</code>. This will prevent the factory from
     * being garbage-collected until the font can also be garbage-collected.
     */
    private static class LoadedFont extends Font
    {
        FontFactory ff;

        public LoadedFont(FontFactory ff, String name, int style, int size)
        {
            super(name, style, size);
            this.ff = ff;
        }
    }
}
