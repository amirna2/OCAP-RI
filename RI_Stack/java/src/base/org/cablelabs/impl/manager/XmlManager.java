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

package org.cablelabs.impl.manager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dvb.application.AppID;
import org.dvb.ui.FontFormatException;

import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.DirInfo;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo.FileInfo;
import org.cablelabs.impl.security.AppPermissions;

/**
 * A <code>Manager</code> that parses OCAP-related data provided in an XML
 * format. The <code>XmlManager</code> implementation is used to parse the
 * following XML files:
 * 
 * <ul>
 * <li>Font Index file
 * <li>Permission Request file
 * <li>Application Description file
 * </ul>
 * 
 * The main reason for this manager's existence is to protect the users (i.e.,
 * the rest of the OCAP stack implementation) of the data from the specific
 * implementation of the XML parser used. This makes it possible to change the
 * underlying API and/or implementation without affecting the main code.
 * 
 * @author Aaron Kamienski
 */
public interface XmlManager extends Manager
{
    /**
     * Parses the font index file found in the given <code>InputStream</code>.
     * Returns an array of <code>FontInfo</code> objects that contain the font
     * information contained within the file.
     * 
     * @param is
     *            the stream from which to read the data
     * @return an array of <code>FontInfo</code> objects; if none were found
     *         then a zero-length array is returned
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     * @throws FontFormatException
     *             if there is an error in the font index file
     */
    public FontInfo[] parseFontIndex(java.io.InputStream is) throws IOException, FontFormatException;

    /**
     * Parses the permission request file found in the given
     * <code>InputStream</code>. Returns a <code>PermissionCollection</code> for
     * the <code>Permission</code>s requested by the file.
     * <p>
     * To accomodate MHP 12.6.2.2, an empty PermissionCollection is returned if
     * there are errors parsing the XML file.
     * 
     * @param prfData
     *            the PRF byte data
     * @param ocapPerms
     *            if <code>true</code> then allow OCAP-specific permissions
     *            (i.e., parse the OCAP DTD)
     * @param monAppPerms
     *            if <code>true</code> then allow OCAP-specific
     *            <code>MonitorAppPermission</code>s; if <code>true</code> then
     *            <i>ocapPerms</i> must also be <code>true</code>
     * @param appid
     *            the appid expected in the PRF
     * @param serviceContextID
     *            the service context ID of the calling app
     * @return the <code>PermissionCollection</code> containing the
     *         <code>Permission</code> objects requested by the file;
     *         <code>null</code> if errors were encountered during parsing
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     */
    public AppPermissions parsePermissionRequest(byte[] prfData, boolean ocapPerms, boolean monAppPerms, AppID appid,
            Long serviceContextID) throws IOException;

    /**
     * Parses the application description file found in the given
     * <code>InputStream</code>. Returns an instance of
     * <code>AppDescriptionInfo</code> containing the information specified by
     * the file.
     * 
     * @param is
     *            the stream from which to read the data
     * @return an instance of <code>AppDescriptionInfo</code>; if none is found
     *         then <code>null</code> is returned
     * 
     * @throws IOException
     *             if there is an error attempting to access the data in the
     *             file
     */
    public AppDescriptionInfo parseAppDescription(java.io.InputStream is) throws IOException;

    /**
     * <code>FontInfo</code> is a data structure used to hold information read
     * from a font index file. Each instance of <code>FontInfo</code>
     * corresponds to an instance of a <code>&lt;font&gt;</code> element
     * contained within the font index file.
     * 
     * <pre>
     *   &lt;fontdirectory&gt;
     *     &lt;font&gt;
     *       {@link #name &lt;name&gt;Tiresias&lt;/name&gt; }
     *       {@link #format &lt;fontformat&gt;PFR&lt;/fontformat&gt; }
     *       {@link #filename &lt;filename&gt;tiresias.pfr&lt;/filename&gt; }
     *       {@link #style &lt;style&gt;BOLD&lt;/style&gt; }
     *       &lt;size {@link #min min="0"} {@link #max max="maxint"}/&gt;
     *     &lt;/font&gt;
     *   &lt;/fontdirectory&gt;
     * </pre>
     * 
     * @see XmlManager#parseFontIndex
     */
    public static class FontInfo
    {
        /**
         * Names the font.
         */
        public String name;

        /**
         * Specifies the format of the font. The only valid value is
         * <code>"PFR"</code>.
         */
        public String format;

        /**
         * Names the file that contains the font.
         */
        public String filename;

        /**
         * The set of available styles for the font represented as a bit set
         * within a <code>BitSet</code> object.
         * <p>
         * Individual styles can be tested in the following manner:
         * 
         * <pre>
         * boolean hasPlain = style.get(Font.PLAIN);
         * 
         * boolean hasItalic = style.get(Font.ITALIC);
         * 
         * boolean hasBold = style.get(Font.BOLD);
         * 
         * boolean hasBoldItalic = style.get(Font.BOLD | Font.ITALIC);
         * </pre>
         * 
         * @see java.awt.Font#PLAIN
         * @see java.awt.Font#BOLD
         * @see java.awt.Font#ITALIC
         */
        public java.util.BitSet style;

        /**
         * The smallest available size for the given font. If not specified by
         * the <code>&lt;size&gt;</code> element, will default to <code>0</code>
         * .
         * 
         * @see #max
         */
        public int min = 0;

        /**
         * The largest available size for the given font. If not specified by
         * the <code>&lt;size&gt;</code> element, will default to
         * {@link java.lang.Integer#MAX_VALUE Integer.MAX_VALUE}.
         * 
         * @see #max
         */
        public int max = Integer.MAX_VALUE;
    }

    /**
     * <code>AppDescriptionFile</code> is a data structure that holds the
     * information contained within the "Application description file". It
     * provides a list of files that need to be installed as well as other
     * related necessary information.
     * <p>
     * Note that this class implements <code>Serializable</code>. This is to
     * enable the serialization of this information into persistent storage if
     * necessary.
     * 
     * @see XmlManager#parseAppDescription
     */
    public static class AppDescriptionFile
    {
        /**
         * Parses the ADF for this app contained in the specified file data.
         * If errors are encountered while reading the ADF, then they are
         * propagated.
         * 
         * @param fileData
         *            the ADF file data
         * @return an <code>AppDescriptionInfo</code> object describing the
         *         files to be stored
         * 
         * @throws IOException
         *             if there was a problem reading/parsing the ADF file
         */
        public static AppDescriptionInfo parseADF(byte[] fileData)
            throws IOException
        {
            InputStream bais = new ByteArrayInputStream(fileData);

            // Parse the ADF file...
            // Throws IOException upon error, otherwise returns
            // AppDescriptionInfo
            InputStream is = new BufferedInputStream(bais);
            XmlManager xml = (XmlManager) ManagerManager.getInstance(XmlManager.class);
            return xml.parseAppDescription(is);
        }

        /**
         * Returns the "default" app description file which includes all files
         * under the subtree
         * 
         * @return the default ADF
         */
        public static AppDescriptionInfo getDefaultADF()
        {
            AppDescriptionInfo info = new AppDescriptionInfo();
            info.files = new FileInfo[2];

            int index = 0;

            if (info.files.length > 1)
            {
                DirInfo dir = info.new DirInfo();
                dir.name = "*";
                dir.files = new FileInfo[0];
                info.files[index++] = dir;
            }

            FileInfo file = info.new FileInfo();
            file.name = "*";
            file.size = 0;
            info.files[index++] = file;

            return info;
        }
    }
}
