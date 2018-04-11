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

import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;
import org.cablelabs.test.ProxySecurityManager;
import java.awt.Font;
import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import org.ocap.storage.*;
import org.cablelabs.impl.util.MPEEnv;
import junit.framework.*;

/**
 * Global classes.
 **/
/**
 * File filter to exclude all but .pfr files in a directory.
 **/
class pfrFilter implements FilenameFilter
{
    // dir is the directory where the file resides.
    // name is the filename to check/filter
    public boolean accept(File dir, String name)
    {
        if (new File(dir, name).isDirectory()) // Eliminate directories
        {
            return false;
        }
        name = name.toLowerCase(); // Convert to lower case
        return name.endsWith(".pfr"); // Only return .pfr font files
    } // accept
}; // pfrFilter

/**
 * Tests FontFactory.
 * <p>
 * Note that this test will copy necessary resources (specifically a font index
 * file and fonts) to ".". This is necessary for testing the font index file
 * support outside of an Xlet environment. When testing within an Xlet
 * environment, then the files will have to be in the base directory for the
 * application. It is assumed that their original location (at the base of
 * classpath) will fill this role.
 * 
 * @author Aaron Kamienski and Donald Murray
 */
public class FontFactoryTest extends TestCase
{
    // Some private variables used in setup of test working directory.
    private String m_root; // persistent root.

    private String m_oldDir; // Holds older user.dir value to return to.

    private String m_testDir; // Test Directory

    private File m_ddir; // Destination directory

    private boolean m_buildup; // true indicates setUpFontIndex called.

    // These are the equivalent of enums passed as ints that are status values
    // for various functions in this test. Note that the last value should
    // always be FONT_ST_LAST as this allows the user to iterate through the
    // values and determine when the end is reached.
    public static final int FONT_ST_OK = 0; // No Error

    public static final int FONT_ST_ERROR = FONT_ST_OK + 1; // Error

    public static final int FONT_ST_NULL_PTR = FONT_ST_ERROR + 1; // Null Ptr

    public static final int FONT_ST_FILE = FONT_ST_NULL_PTR + 1; // File err

    public static final int FONT_ST_COPY = FONT_ST_FILE + 1; // Copy err

    public static final int FONT_ST_DIR = FONT_ST_COPY + 1; // Dir err

    public static final int FONT_ST_LAST = FONT_ST_DIR + 1; // Last

    /**
     * The font index file.
     */
    static File fontindex = new File("./ocap.fontindex");

    // static final File fontindex = new File("/snfs/ocap.fontindex");
    private boolean fontindexCreated = false;

    protected void fileCopy(File src, File dst) throws IOException
    {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }
        out.flush(); // Flush output
        in.close(); // close streams.
        out.close();
    }

    // This utility function sets up the persistent partition and creates a sub
    // directory under which the test is to execute. First, the partition is
    // found, the subdirectory called dirFFTest is created, all files needed are
    // copied into this subdirectory and finally we set user.dir to change
    // directory to that new subdir.
    protected int makeDir()
    {
        m_root = new String(MPEEnv.getSystemProperty("dvb.persistent.root"));
        m_oldDir = new String(System.getProperty("user.dir"));
        m_testDir = new String("/dirFFTest");
        // Create the test directory
        File dir = new File(m_root + m_testDir);
        if (!dir.exists())
        {
            System.out.println("+++Creating directory " + dir.getPath());
            dir.mkdir();
        }
        // Go to the new directory
        System.setProperty("user.dir", m_root + m_testDir);
        System.out.println("+++Changing directory to " + m_root + m_testDir);
        // Copy ocap.fontindex from source to here, if it exists.
        File f = new File("/syscwd/ocap.fontindex");
        if (f.exists())
        {
            try
            {
                System.out.println("+++Copying file " + f.getPath() + " to " + m_root + m_testDir + "/ocap.fontindex");
                fontindex = new File(m_root + m_testDir, "ocap.fontindex");
                fileCopy(f, fontindex);
                fontindexCreated = true;
            }
            catch (Exception e)
            {
                System.out.println("+++Exception copying fontindex: " + e.toString());
                return FONT_ST_COPY;
            }
        }
        else
        {
            System.out.println("+++File " + f.getPath() + "not found");
            System.out.println("+++Create file from scratch");
            fontindex = new File(m_root + m_testDir, "ocap.fontindex");
            String fistring = "<?xml version=\"1.0\"?>\n"
                    + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                    + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>" + "<font>"
                    + "<name>Tiresias</name>" + "<fontformat>PFR</fontformat>" + "<filename>" + fonts[0].rez
                    + "</filename>" + "</font>" + "</fontdirectory>";
            try
            {
                OutputStream os = new FileOutputStream(fontindex);
                os.write(fistring.getBytes(), 0, fistring.length());
                os.flush();
                os.close();
            }
            catch (Exception e)
            {
            }
        }
        // Now set up sys/fonts directory.
        m_ddir = new File(m_root + m_testDir + "/sys/fonts");
        if (!m_ddir.exists())
        {
            System.out.println("+++Creating directory " + m_ddir.getPath());
            m_ddir.mkdirs();
        }
        // Now point to the existing directories
        File sdir = new File("/syscwd/sys/fonts");
        String[] files = sdir.list(new pfrFilter()); // Get font files only
        if (files == null)
        {
            System.out.println("+++files (sdir.list) is null\n");
            return FONT_ST_NULL_PTR;
        }
        System.out.println("+++Number of files in " + sdir.getPath() + " = " + files.length);
        for (int i = 0; i < files.length; ++i)
        { // Copy all .pfr files
            try
            {
                System.out.println("+++Copying file /syscwd/sys/fonts/" + files[i] + " to " + m_root + m_testDir
                        + "/sys/fonts/" + files[i]);
                fileCopy(new File("/syscwd/sys/fonts", files[i]), new File(m_root + m_testDir + "/sys/fonts", files[i]));
            }
            catch (Exception e)
            {
                System.out.println("Exception copying file " + files[i]);
                System.out.println("Exception: " + e.toString());
                return FONT_ST_COPY;
            }
        } // for(int i.... copying files
        return FONT_ST_OK; // No Error
    } // makeDir....Have copied font files to newdir/sys/fonts

    // cleanDir goes to the new file area and deletes all the files and
    // directories there.
    protected int cleanDir()
    {
        File f; // temp file
        // First, cd back to the old user.dir directory
        System.setProperty("user.dir", m_oldDir);
        System.out.println("+++Changing directory to " + m_oldDir);
        String[] files = m_ddir.list(); // Get all files in destination
        for (int i = 0; i < files.length; ++i)
        { // Delete loop
            f = new File(m_ddir, files[i]); // Get file object
            if (f.exists())
            {
                System.out.println("+++Deleting " + files[i]);
                f.delete(); // Delete the file
            }
        } // Delete until all done
        if (m_ddir.exists())
        {
            System.out.println("+++Deleting " + m_ddir.getPath());
            m_ddir.delete(); // Delete /sys/fonts
        }
        f = new File(m_root + m_testDir + "/sys");
        if (f.exists())
        {
            System.out.println("+++Deleting " + f.getPath());
            f.delete(); // Delete /sys
        }
        f = new File(m_root + m_testDir); // Delete ./*
        files = f.list(); // Could be font.index file
        for (int i = 0; i < files.length; ++i)
        {
            f = new File(m_root + m_testDir, files[i]);
            if (f.exists())
            {
                System.out.println("+++Deleting " + f.getPath());
                f.delete();
            }
            fontindexCreated = false;
        }
        f = new File(m_root + m_testDir); // Delete "./"
        if (f.exists())
        {
            System.out.println("+++Deleting " + f.getPath());
            f.delete();
        }
        System.out.println("+++cleanDir completed");
        return FONT_ST_OK;
    } // cleanDir...at this point, all dest files/dirs deleted.

    /**********************************************************************
	 */
    /**
     * Tests FontFactory().
     */
    public void testConstructorDefault() throws Exception
    {
        System.out.println("+++testConstructorDefault\n");

        setUpFontIndex();

        assertTrue("Font index file not present - cannot test", fontindex.exists());

        FontFactory ff = new FontFactory();
        assertNotNull("FontFactory() should be created", ff);
    }

    /**
     * Tests FontFactory() parsing of font index file. Checks for
     * FontFormatException for missing information.
     * <p>
     * Uses package-private FontFactory(InputStream) constructor added and
     * exposed for testing purposes only.
     */
    public void testConstructor_FormatFields() throws Exception
    {
        System.out.println("+++testConstructor_FormatFields\n");

        // Note this test shouldn't need to create filesystem.

        // Generate a FontIndex file InputStream
        // Test for a missing element
        String prefix = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>\n" + "<font>\n";
        String fields[] = { "<name>NoMatter</name>\n", "<fontformat>PFR</fontformat>\n",
                "<filename>" + fonts[0].rez + "</filename>\n" };
        String postfix = "</font>\n" + "</fontdirectory>\n";

        // Test for missing fields
        for (int i = 0; i < fields.length; ++i)
        {
            // Generate fontindex with a missing field
            String fontindex = prefix;
            for (int j = 0; j < fields.length; ++j)
                if (j != i) fontindex += fields[j];
            fontindex += postfix;

            InputStream is = new ByteArrayInputStream(fontindex.getBytes());
            try
            {
                // Private constructor so file system is not needed.
                FontFactory ff = new FontFactory(is);
                fail("+++Expected FontFormatException for missing field: " + fields[i]);
            }
            catch (FontFormatException e)
            {
            }
        }
    }

    /**
     * Tests createFont() from font index file. Checks for IOException
     * generation.
     * <p>
     * Uses package-private FontFactory(InputStream) constructor added and
     * exposed for testing purposes only.
     */
    public void testCreateFontIndex_IO() throws Exception
    {
        System.out.println("+++testCreateFontIndex_IO\n");

        // Shouldn't need to create directories

        // Generate a FontIndex file InputStream
        // Test for a non-existent font file
        String fontindex = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>" + "<font>"
                + "<name>NoMatter</name>" + "<fontformat>PFR</fontformat>" + "<filename>no_such_file.pfr</filename>"
                + "</font>" + "</fontdirectory>";
        InputStream is = new ByteArrayInputStream(fontindex.getBytes());

        FontFactory ff = null;
        try
        {
            /* Special constructor without having to use filesystem */
            ff = new FontFactory(is);
        }
        catch (IOException e)
        {
            fail("IOException should not be thrown until createFont");
        }

        try
        {
            Font f = ff.createFont("NoMatter", 0, 26);
            fail("Expected IOException for missing file");
        }
        catch (IOException e)
        {
        }
    }

    /**
     * Tests FontFactory() parsing of font index file. Checks for
     * FontFormatException for missing information.
     * <p>
     * Uses package-private FontFactory(InputStream) constructor added and
     * exposed for testing purposes only.
     */
    public void testCreateFontIndex_FormatBadFile() throws Exception
    {
        System.out.println("+++testCreateFontIndex_FormatBadFile\n");

        setUpFontIndex(); // to copy font index so that it can be used as bad
                          // font file

        // Generate a FontIndex file InputStream
        // Test for non-font file
        String fontindex = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>" + "<font>"
                + "<name>NoMatter</name>" + "<fontformat>PFR</fontformat>" + "<filename>ocap.fontindex</filename>" // Not
                                                                                                                   // a
                                                                                                                   // PFR
                                                                                                                   // file
                + "</font>" + "</fontdirectory>";
        InputStream is = new ByteArrayInputStream(fontindex.getBytes());
        FontFactory ff = new FontFactory(is);
        try
        {
            Font f = ff.createFont("NoMatter", 0, 26);
            fail("Expected FontFormatException for invalid font file");
        }
        catch (FontFormatException e)
        {
        }
        catch (FontNotAvailableException e)
        {
            // OK to go here....passed.
        }
    }

    /**
     * Tests createFont() from FontFactory().
     */
    /*
     * Default constructor doesn't work for FontFactory in debug mode.
     */
    public void testCreateFontIndex() throws Exception
    {
        System.out.println("+++testCreateFontIndex\n");

        setUpFontIndex();

        assertTrue("Font index file not present - cannot test", fontindex.exists());

        FontFactory ff = new FontFactory();
        assertNotNull("FontFactory() should be created", ff);

        for (int i = 0; i < fonts.length; ++i)
        {
            // Valid font
            Font f = ff.createFont(fonts[i].name, fonts[i].style, fonts[i].size);
            assertNotNull("A Font object is expected for valid font (" + fonts[i].rez + ":" + fonts[i].name + ")", f);
            assertEquals("Expected font of requested size", fonts[i].size, f.getSize());
            assertEquals("Expected font of requested style", fonts[i].style, f.getStyle());
            assertEquals("Expected font name to be the name given", fonts[i].name, f.getName());

            // Non-existent font
            try
            {
                f = ff.createFont("nosuchfont", fonts[i].style, fonts[i].size);
                fail("Expected FontNotAvailableException for unfound font");
            }
            catch (FontNotAvailableException e)
            {
            }
        }
    }

    /**
     * Tests createFont() from FontFactory(). Checks for
     * IllegalArgumentException for bad styles.
     */
    public void testCreateFontIndex_Argument() throws Exception
    {
        System.out.println("+++testCreateFontIndex_Argument\n");

        // Don't need to set up new filesystem for this test.

        setUpFontIndex(); // for font file

        // Generate a FontIndex file InputStream
        // Test for a bad style
        String fontindex = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>" + "<font>"
                + "<name>NoMatter</name>" + "<fontformat>PFR</fontformat>" + "<filename>" + fonts[0].rez
                + "</filename>" + "</font>" + "</fontdirectory>";
        InputStream is = new ByteArrayInputStream(fontindex.getBytes());

        FontFactory ff = new FontFactory(is);
        try
        {
            ff.createFont("NoMatter", 99, 26); // bad style
            fail("Expected IllegalArgumentException for bad style");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Tests createFont() from FontFactory(). Checks for proper support of style
     * information. I.e., which styles are supported by a font.
     * <p>
     * Uses package-private FontFactory(InputStream) constructor added and
     * exposed for testing purposes only.
     * 
     * @note Currently, we know that this fails because not all styles are past
     *       to the native side... so not all styles in the fontindex file will
     *       be available.
     */
    public void testCreateFontIndex_styles() throws Exception
    {
        System.out.println("+++testCreateFontIndex_styles\n");

        // Shouldn't need to create new file system for this test.

        setUpFontIndex(); // to get font files in place

        String name = fonts[0].name;
        String rez = fonts[0].rez;
        int size = fonts[0].size;

        String prefix = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>\n" + "<font>"
                + "<name>" + name + "</name>\n" + "<fontformat>PFR</fontformat>\n" + "<filename>" + rez
                + "</filename>\n";
        String postfix = "</font>\n" + "</fontdirectory>\n";
        int styles[][] = { { Font.PLAIN, Font.BOLD, Font.BOLD | Font.ITALIC, Font.ITALIC }, { Font.PLAIN, Font.BOLD },
                { Font.PLAIN, Font.BOLD | Font.ITALIC, Font.ITALIC }, { Font.BOLD | Font.ITALIC }, {}, // implies
                                                                                                       // all
                                                                                                       // 4!
        };

        for (int i = 0; i < styles.length; ++i)
        {
            String fontindex = prefix;
            for (int j = 0; j < styles[i].length; ++j)
            {
                fontindex += "<style>";
                switch (styles[i][j])
                {
                    case Font.PLAIN:
                        fontindex += "PLAIN";
                        break;
                    case Font.ITALIC:
                        fontindex += "ITALIC";
                        break;
                    case Font.BOLD:
                        fontindex += "BOLD";
                        break;
                    case Font.BOLD + Font.ITALIC:
                        fontindex += "BOLD_ITALIC";
                        break;
                    default:
                        fail("Internal test error");
                }
                fontindex += "</style>\n";
            }
            fontindex += postfix;

            InputStream is = new ByteArrayInputStream(fontindex.getBytes());
            FontFactory ff = new FontFactory(is);

            // Make sure that we can get the specified styles
            int idx = (styles[i].length == 0) ? 0 : i;
            BitSet set = new BitSet(); // so we can remember the styles we
                                       // expect for later
            for (int j = 0; j < styles[idx].length; ++j)
            {
                set.set(styles[idx][j]); // remember this style
                try
                {
                    Font f = ff.createFont(name, styles[idx][j], size);
                    assertNotNull("A Font object is expected for the given style " + idx + ":" + styles[idx][j], f);
                    assertEquals("Expected font of requested style (" + idx + ")", styles[idx][j], f.getStyle());
                }
                catch (FontNotAvailableException e)
                {
                    fail("Expected font to be found for the given style " + idx + ":" + styles[idx][j]);
                }
            }

            // And that we DON'T get the non-requested styles
            for (int style = Font.PLAIN; style <= (Font.BOLD + Font.ITALIC); ++style)
            {
                if (!set.get(style))
                {
                    try
                    {
                        Font f = ff.createFont(name, style, size);
                        fail("Expected FontNotAvailableException for unfound font");
                    }
                    catch (FontNotAvailableException e)
                    {
                    }
                }
            }
        }
    }

    /**
     * Tests FontFactory(URL).
     */

    public void testConstructorUrl() throws Exception
    {
        System.out.println("+++testConstructorUrl\n");

        // Valid fonts
        for (int i = 0; i < fonts.length; ++i)
        {
            FontFactory ff = new FontFactory(getFontResource(fonts[i].rez));
            assertNotNull("FontFactory(URL) for " + fonts[i].rez + " should be created");
            ff = null;
        }
    }

    /**
     * Tests FontFactory(URL) given nonexistent file.
     */
    public void testConstructorURL_IO() throws Exception
    {
        System.out.println("+++testConstructorURL_IO\n");

        // IOException
        try
        {
            URL url = new URL("file", null, "/nonexistent/file/font.pfr");
            FontFactory ff = new FontFactory(url);
            fail("Expected IOException");
        }
        catch (IOException e)
        {
        }
    }

    /**
     * Tests FontFactory(URL) given bad font format.
     */
    public void testConstructorURL_Format() throws Exception
    {
        System.out.println("+++testConstructorURL_Format\n");

        // FontFormatException
        try
        {
            URL url = getClass().getResource("FontFactoryTest.class");
            FontFactory ff = new FontFactory(url);
            fail("Expected FontFormatException");
        }
        catch (FontFormatException e)
        {
        }

    }

    /**
     * Tests FontFactory(URL) given bad URL.
     * <p>
     * IllegalArgumentException if the URL is not both valid and supported. I
     * don't know how to generate this... wouldn't be able to create bad URL!
     */
    /*
     * public void testConstructorURL_Argument() throws Exception {
     * fail("Unimplemented test"); // Don't know how to test this here }
     */

    /**
     * Tests FontFactory(URL) security checks.
     */
    /*
     * Not familiar enough with security APIs to know whats wanted here.
     */
    public void testConstructorURL_Security() throws Exception
    {
        System.out.println("+++testConstructorURL_Security\n");

        // SecurityException
        // Verify security manager is consulted
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            URL url = getFontResource(fonts[0].rez);
            FontFactory ff = new FontFactory(url);

            assertNotNull("SecurityManager.checkPermission should be called", sm.p);
            // !!!Should check for FilePermission("*rez", "read");
            // Although, could be SocketPermission...
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests createFont() from FontFactory(URL).
     */
    public void testCreateFontUrl() throws Exception
    {
        System.out.println("+++testCreateFontUrl\n");

        for (int i = 0; i < fonts.length; ++i)
        {
            FontFactory ff = new FontFactory(getFontResource(fonts[i].rez));
            assertNotNull("FontFactory(URL) for " + fonts[i].rez + " should be created");

            // Valid font
            Font f = ff.createFont(fonts[i].name, fonts[i].style, fonts[i].size);
            assertNotNull("A Font object is expected for valid font (" + fonts[i].rez + ":" + fonts[i].name + ")", f);
            assertEquals("Expected font of requested size", fonts[i].size, f.getSize());
            assertEquals("Expected font of requested style", fonts[i].style, f.getStyle());
            assertEquals("Expected font name to be the name given", fonts[i].name, f.getName());

            // Non-existent font
            try
            {
                f = ff.createFont("nosuchfont", fonts[i].style, fonts[i].size);
                fail("Expected FontNotAvailableException for non-existent font");
            }
            catch (FontNotAvailableException e)
            {
            }
        }
    }

    /**
     * Tests createFont() FontFactory(URL). Checks for IllegalArgumentException
     * for bad style.
     */
    public void testCreateFontUrl_Argument() throws Exception
    {
        System.out.println("+++testCreateFontUrl_Argument\n");

        FontFactory ff = new FontFactory(getFontResource(fonts[0].rez));
        try
        {
            ff.createFont("NoMatter", 99, 26); // bad style
            fail("Expected IllegalArgumentException for bad style");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    static class FontDesc
    {
        public String rez;

        public String name;

        public int style;

        public int size;

        /**
         * @see FontFactoryTest#setUpFontIndex
         * @see FontFactoryTest#tearDownFontIndex
         */
        public boolean created;

        public FontDesc(String rez, String name)
        {
            this(rez, name, 0, 26);
        }

        public FontDesc(String rez, String name, int style, int size)
        {
            this.rez = rez;
            this.name = name;
            this.style = style;
            this.size = size;
        }
    }

    // Found in RI_Stack/fonts/TrueDoc.com
    // Perhaps they should be copied here
    protected static final FontDesc fonts[] = { new FontDesc("fonts/Tires-o_802.pfr", "Tiresias"),
            new FontDesc("fonts/Tires-o_802.pfr", "Tiresias"), new FontDesc("fonts/Tires-o_802.pfr", "Tiresias"),
            new FontDesc("fonts/Tires-o_802.pfr", "Tiresias") };

    protected URL getFontResource(String rez)
    {
        //
        // try to get the resource in any way possible, first by
        // looking on the classpath, then by trying to load it
        // from the file system
        //
        URL url = getClass().getResource(rez);
        if (url == null && !rez.startsWith("/")) url = getClass().getResource("/" + rez);

        String[] baseDirectories = new String[] { "/syscwd/sys", "/snfs/sys", };
        if (url == null)
        {
            String filename = rez;
            if (!rez.startsWith("/"))
            {
                filename = "/" + filename;
            }

            for (int i = 0; i < baseDirectories.length && url == null; i++)
            {

                filename = "/syscwd/sys" + filename;
                File fontFile = new File(filename);
                try
                {
                    url = fontFile.toURL();
                }
                catch (MalformedURLException exc)
                {
                    //
                    // couldn't get the url, fall through with
                    // the null value
                    //
                }
            }
        }

        if (url == null) fail("Could not find font resource \"" + rez + "\" for testing");
        return url;
    }

    /**
     * Sets up the font index file and font resources if necessary.
     * 
     * <ol>
     * <li>Looks for existing font index file and fonts in "."
     * <li>If not available, then will have to create.
     * <li>Get resources from classpath and copy to "."
     * </ol>
     * 
     * Big problem with this function is that in debug mode, the current
     * directory maybe unwritable. So, if it is to be used, we need to change
     * directories and use mkDir instead of using this function.
     */
    protected void setUpFontIndex() throws Exception
    {
        m_buildup = true; // Tells system that setUpFontIndex was called.

        // First, we must change directory to a writeable file system.
        // ....such as /itfs/usr or dvb.persistent.root.
        // Save old directory and cd to the root of the filesystem.
        m_root = new String(MPEEnv.getSystemProperty("dvb.persistent.root"));
        m_oldDir = new String(System.getProperty("user.dir"));
        System.setProperty("user.dir", m_root);
        fontindex = new File("./ocap.fontindex"); // has to be done after CD

        URL url = null;
        if (!fontindex.exists() && (url = getFontResource("ocap.fontindex")) != null)
        {
            // Must create
            File f = new File("/syscwd/ocap.fontindex");
            if (f.exists())
            {
                fileCopy(f, fontindex);
                fontindexCreated = true;
            }
            else
            // Now you have problem because ocap.fontindex doesn't exist
            { // Must create ocap.fontindex from scratch.
                String fistring = "<?xml version=\"1.0\"?>\n"
                        + "<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" "
                        + "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n" + "<fontdirectory>"
                        + "<font>" + "<name>Tiresias</name>" + "<fontformat>PFR</fontformat>" + "<filename>"
                        + fonts[0].rez + "</filename>" + "</font>" + "</fontdirectory>";
                try
                {
                    OutputStream os = new FileOutputStream(fontindex);
                    os.write(fistring.getBytes(), 0, fistring.length());
                    os.flush();
                    os.close();
                }
                catch (Exception e)
                {
                    System.out.println("+++Fontindex could not be created because " + e.toString());
                }
                fontindexCreated = true;
            } // Now OCAP.fontindex has been created from scratch.
        }

        // Now copy the font files themselves.
        for (int i = 0; i < fonts.length; ++i)
        {
            File f = new File(fonts[i].rez);
            if (!f.exists() && (url = getFontResource(fonts[i].rez)) != null)
            {
                // Must create
                copyResource(f, url);
                fonts[i].created = true;
            }
        }
    }

    /**
     * Copies the given resource to the given destination file.
     */
    private void copyResource(File dest, URL src) throws IOException
    {
        // First, must create destination directory if doesn't exist
        String dirname = dest.getParent();
        if (dirname != null)
        {
            File dir = new File(dirname);
            dir.mkdirs();
        }

        // Copy the File
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try
        {
            in = new BufferedInputStream(src.openStream());
            FileOutputStream fos = new FileOutputStream(dest);
            out = new BufferedOutputStream(fos);

            byte[] buf = new byte[512];
            int nbytes;

            while ((nbytes = in.read(buf, 0, buf.length)) > -1)
                out.write(buf, 0, nbytes);
            out.flush();
        }
        finally
        {
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    /**
     * Deletes a directory hierarchy, if possible.
     */
    private void deleteDir(String name) throws Exception
    {
        File parent;

        // Remove the director(y|ies)
        while (name != null && (parent = new File(name)) != null && parent.isDirectory())
        {
            File tmp = parent;
            name = tmp.getParent();

            // //System.out.println("Deleting dir: "+tmp);
            // Stop once we fail
            if (!tmp.delete()) break;
        }
    }

    /**
     * Deletes any copied font index or font files.
     */
    protected void tearDownFontIndex() throws Exception
    {
        if (!m_buildup) // if setUpFontIndex not called, then exit.
            return;
        else
            m_buildup = false;

        if (fontindexCreated)
        {
            // Remove font index file
            fontindexCreated = false;
            fontindex.delete();
        }

        for (int i = 0; i < fonts.length; ++i)
        {
            if (fonts[i].created)
            {
                // Remove font file
                fonts[i].created = false;

                File f = new File(fonts[i].rez);
                f.delete();

                // Assume we created the directory
                deleteDir(f.getParent());
            }
        }
        System.setProperty("user.dir", m_oldDir); // cd to old directory
    }

    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        public void checkPermission(Permission p)
        {
            if (this.p == null) this.p = p;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(FontFactoryTest.class);
        return suite;
    }

    public FontFactoryTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        // //System.out.println(getName());

        super.setUp();
        // This is performed only for the required tests
        // setUpFontIndex();
    }

    protected void tearDown() throws Exception
    {
        tearDownFontIndex();
        super.tearDown();
    }

}
