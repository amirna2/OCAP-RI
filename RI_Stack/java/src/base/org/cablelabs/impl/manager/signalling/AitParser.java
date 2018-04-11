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

package org.cablelabs.impl.manager.signalling;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitImpl;
import org.cablelabs.impl.signalling.AitImpl.AddressingDescriptor;
import org.cablelabs.impl.signalling.AitImpl.Comparison;
import org.cablelabs.impl.signalling.AitImpl.LogicalOp;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.CRC;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Standalone parsing for an AIT. This class provides an implementation for the
 * {@link org.cablelabs.impl.signalling.Ait} class based upon an array of
 * <code>Section</code>s or an <code>InputStream</code>.
 * <p>
 * Definition of the descriptors and error handling implemented here can be
 * found in MHP 10. Information about the OCAP-specific changes can be found in
 * OCAP 11.
 * 
 * @author Aaron Kamienski
 */
class AitParser
{
    AitParser()
    {
        ait = new AitImpl();
    }
    
    /**
     * Return a signalling object based on the parsed data
     * 
     * @return the signalling
     */
    public Ait getSignalling()
    {
        ait.initialize(version,externalAuth,allApplications,attributeMap,addrGroups);
        return ait;
    }

    /**
     * Creates an instance of <code>AppInfo</code> to be filled in as a result
     * of parsing an AIT. This should be overridden by subclasses to return an
     * extension of <code>AppInfo</code> if necessary.
     * 
     * @return a new <code>AppInfo</code> instance
     */
    protected AppInfo createAppInfo()
    {
        return new AppInfo();
    }

    /**
     * Parses the AIT/XAIT made up of the given sections.
     * 
     * @param sections
     *            AIT/XAIT is given in form of array <code>Section</code>s
     */
    void parse(Section[] sections)
    {
        AppInfo common = createAppInfo();
        common.isCommon = true;
        Vector parsedSections = new Vector();
        for (int i = 0; i < sections.length; ++i)
        {
            try
            {
                byte[] data = sections[i].getData();
                ByteArrayInputStream input = new ByteArrayInputStream(data);
                SectionParser parsed = parseSection(input);
                parsedSections.addElement(parsed);

                // Consolidate common information in one AppInfo
                common.updateWithCommon(parsed.commonInfo);
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Ignoring section because of error", e);
                }
            }
            catch (NoDataAvailableException e)
            {
                // This is really an internal error
                // TODO(AaronK): in this case, should we simply return NO
                // XAIT???
                SystemEventUtil.logRecoverableError("Could not read Section", e);
            }
            catch (Exception e) // ParsingFinished/IllegalArgumentException/etc
            {
                if (log.isInfoEnabled())
                {
                    log.info("Could not parse Section", e);
                }
            }
            catch (Error e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unexpected error", e);
                }
                throw e;
            }
        }

        // Save the sections
        saveSections(parsedSections, common);
    }

    /**
     * Parses the AIT/XAIT made up of the given sections.
     * 
     * @param sections
     *            AIT/XAIT is given in form of array <code>Section</code>s
     */
    // TODO(AaronK): should this throw exceptions so that the caller can ignore
    // altogether?
    void parse(Vector aits)
    {
        Vector parsedSections = new Vector();
        for (Enumeration en = aits.elements(); en.hasMoreElements();)
        {
            Section[] sections = (Section[]) en.nextElement();

            AppInfo common = createAppInfo();
            common.isCommon = true;

            for (int i = 0; i < sections.length; ++i)
            {
                try
                {
                    byte[] data = sections[i].getData();
                    ByteArrayInputStream input = new ByteArrayInputStream(data);
                    SectionParser parsed = parseSection(input);
                    parsedSections.addElement(parsed);

                    // Consolidate common information in one AppInfo
                    common.updateWithCommon(parsed.commonInfo);
                }
                catch (FormatFailure e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Ignoring section because of error", e);
                    }
                }
                catch (NoDataAvailableException e)
                {
                    // This is really an internal error
                    // TODO(AaronK): in this case, should we simply return NO
                    // XAIT???
                    SystemEventUtil.logRecoverableError("Could not read Section", e);
                }
                catch (Exception e) // ParsingFinished/IllegalArgumentException/etc
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Could not parse Section", e);
                    }
                }
                catch (Error e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Unexpected error", e);
                    }
                    throw e;
                }
            }

            // Save the sections
            saveSections(parsedSections, common);
        }
    }

    /**
     * Invokes {@link #saveSection} for each section in the given list of parsed
     * sections.
     * 
     * @param parsedSections
     *            the list of successfully parsed sections
     * @param common
     *            common loop information
     */
    protected void saveSections(Vector parsedSections, AppInfo common)
    {
        for (Enumeration e = parsedSections.elements(); e.hasMoreElements();)
        {
            SectionParser section = (SectionParser) e.nextElement();

            saveSection(section, common);
        }
    }

    /**
     * Invokes {@link #saveApplication} for every application in the given
     * section. Updates the set of {@link #externalAuth external authorizations}
     * .
     * 
     * @param section
     *            the section to save
     * @param common
     *            common loop information
     */
    protected void saveSection(SectionParser section, AppInfo common)
    {
        // Save all current applications
        if (section.apps != null)
        {
            // applications.addAll(section.apps);
            for (Enumeration e = section.apps.elements(); e.hasMoreElements();)
                saveApplication((AppInfo) e.nextElement(), common);
        }

        // Save all current external authorizations
        if (section.extAuths != null) externalAuth.addAll(section.extAuths);
    }

    /**
     * Saves the given application to the set of applications known by this
     * <code>AitParser</code>, if the application is {@link AppInfo#validate()
     * valid}.
     * 
     * @param app
     *            application information to save
     * @param common
     *            common loop application information to resolve given app
     *            against
     */
    protected void saveApplication(AppInfo app, AppInfo common)
    {
        // Update with information from common loop
        app.updateWithCommon(common);

        if (app.validate())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Saving application: " + app.ae);
            }
            allApplications.addElement(app.ae);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("App dropped for missing/invalid info: " + app);
            }
    }
    }

    /**
     * Parses a single <i>application_information_section</i> presented as an
     * <code>InputStream</code>.
     * 
     * @param input
     *            individual AIT section is given in form of an
     *            <code>InputStream</code>
     * @return the parsed section
     * @throws IOException
     */
    private SectionParser parseSection(InputStream input) throws IOException, FormatFailure, ParsingFinished
    {
        // Wrap the InputStream within a DataInputStream
        if (!(input instanceof BufferedInputStream) && !(input instanceof ByteArrayInputStream))
            input = new BufferedInputStream(input);

        SectionParser section = createSectionParser(new DataInputStream(input));

        // Parse a single section
        section.application_information_section();

        return section;
    }

    /**
     * Creates an instance of <code>SectionParser</code> to be used to parse a
     * single <i>application_information_section</i>. This should be overridden
     * by subclasses to return an extension of <code>SectionParser</code>
     * 
     * @param is
     *            the input stream to be used to read the section
     * @return the newly created section parser
     */
    protected SectionParser createSectionParser(DataInputStream is)
    {
        return new SectionParser(is);
    }

    /**
     * Adds the contents of <i>src</i> to <i>dst</i>.
     */
    static void add(Hashtable dst, Hashtable src, boolean overwrite)
    {
        for (Enumeration e = src.keys(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            if (overwrite || !dst.contains(key)) dst.put(key, src.get(key));
        }
    }

    // CRC Checking
    private boolean enableCRC = false;
    private CRC crc = new CRC();
    
    protected AitImpl ait;
    
    // Data required to construct an AIT
    protected int version;
    protected Vector externalAuth = new Vector();
    protected Vector allApplications = new Vector();
    protected Hashtable attributeMap = new Hashtable();
    protected Hashtable addrGroups = new Hashtable();
    
    /**
     * A single parsed <i>application_information_section</i>. The
     * {@link #application_information_section()} method should be invoked to
     * parse the section. After that returns successfully, then parsed
     * information will be available in the following fields:
     * <ul>
     * <li>Common loop information in {@link #commonInfo}
     * <li>Application loop information in {@link #apps}
     * <li>External authorization information in {@link #extAuths}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    protected class SectionParser
    {
        /**
         * The common loop of this parsed section. This is part of the output of
         * a parsed section.
         */
        public AppInfo commonInfo;

        /**
         * The app loop of this parsed section. This is part of the output of a
         * parsed section.
         */
        public Vector apps;

        /**
         * The external authorization information. This is part of the output of
         * a parsed section.
         */
        public Vector extAuths = new Vector();

        /**
         * The AppInfo object into which currently parsed information should be
         * placed.
         */
        protected AppInfo current;

        /**
         * The DataInputStream to be used to pull section data.
         */
        private DataInputStream is;

        /**
         * Creates an instance of <code>SectionParser</code>.
         * 
         * @param is
         *            the input stream to read the section from
         */
        SectionParser(DataInputStream is)
        {
            this.is = is;
        }

        /**
         * Parses the AIT.
         * <p>
         * Notes on syntax:
         * <ul>
         * <li>bslbf Bit string, left bit first, where "left" is the order in
         * which bit strings are written in this Recommendation I International
         * Standard. Bit strings are written as a string of 1s and OS within
         * single quote marks, e.g. '1000 0001'. Blanks within a bit string are
         * for ease of reading and have no significance.
         * <li>uimsbf Unsigned integer, most significant bit first
         * </ul>
         * 
         * <pre>
         * application_information_section() {
         *   table_id 8 uimsbf
         *   section_syntax_indicator 1 bslbf
         *   reserved_future_use 1 bslbf
         *   reserved 2 bslbf
         *   section_length 12 uimsbf
         *   application_type 16 uimsbf
         *   reserved 2 bslbf
         *   version_number 5 uimsbf
         *   current_next_indicator 1 bslbf
         *   section_number 8 uimsbf
         *   last_section_number 8 uimsbf
         *   reserved_future_use 4 bslbf
         *   common_descriptors_length 12 uimsbf
         *   for(i=0;i&lt;N;i++){
         *     descriptor()
         *   }
         *   reserved_future_use 4 bslbf
         *   application_loop_length 12 uimsbf
         *   for(i=0;i&lt;N;i++){
         *     application_identifier()
         *     application_control_code 8 uimsbf
         *     reserved_future_use 4 bslbf
         *     application_descriptors_loop_length 12 uimsbf
         *     for(j=0;j&lt;N;j++){
         *       descriptor()
         *     }
         *   }
         *   CRC_32 32 rpchof
         * }
         * </pre>
         * 
         * @return
         *         <code>true</ocd> if application_information_section is valid (w/out considering
         * individual descriptors)
         * 
         * @throws IOException
         *             given an IOException
         * @throws FormatFailure
         *             given a failure to parse the given format
         * @throws ParsingFinished
         *             given inability to read table_id
         */
        boolean application_information_section() throws IOException, FormatFailure, ParsingFinished
        {
            boolean valid = true;
            int tmp;

            if (log.isDebugEnabled())
            {
                log.debug("application_information_section");
            }

            enableCRC = true;
            crc.initialiseCRC();

            int table_id = readTableID();

            tmp = readUnsignedShort();
            int section_length = (tmp & 0x0FFF);
            assertTrue("section_length > 1021", section_length <= 1021);

            int saved = mark(section_length);
            try
            {
                assertTrue("TableID should be 0x74", table_id == 0x74);
                assertTrue("section_syntax_indicator == 0", (tmp & 0x8000) != 0);

                int application_type = readUnsignedShort();
                assertTrue(application_type == OcapAppAttributes.OCAP_J);

                // Version and current_next_indicator
                tmp = readUnsignedByte();
                version = (tmp & 0x3e) >> 1;
                assertTrue("current_next_indicator == 0", (tmp & 1) != 0);

                int section_number = readUnsignedByte();
                int last_section_number = readUnsignedByte();
                assertTrue("section_number > last_section_number", section_number <= last_section_number);

                // "common" loop
                commonInfo = common_loop(application_type);
                // inner "application" loop
                apps = application_loop();

                enableCRC = false;

                int parsedCRC = readInt(); // Read the CRC value

                assertTrue("application_information_section CRC does not validate", crc.getFinalCRC() == ~parsedCRC);

                if (log.isDebugEnabled())
                {
                    log.debug("Saving applications " + apps.size());
                }
            }
            catch (FormatFailure e)
            {
                valid = false;
                if (log.isInfoEnabled())
                {
                    log.info("FormatFailure during AIT parsing", e);
                }
                skipRemaining();
            } finally
            {
                enableCRC = false;
            }
            restore(saved);

            return valid;
        }

        /**
         * Reads and returns the <i>table_id</i>. Essentially wraps
         * {@link #readUnsignedByte} in a try/catch so that any
         * {@link EOFException}s can be translated to {@link ParsingFinished}.
         * 
         * @return table_id
         * 
         * @throws IOException
         *             given an IOException (unexpected)
         * @throws FormatFailure
         *             given a failure to parse the given format (unexpected)
         * @throws ParsingFinished
         *             given inability to read table_id
         */
        private int readTableID() throws IOException, FormatFailure, ParsingFinished
        {
            try
            {
                return readUnsignedByte();
            }
            catch (EOFException e)
            {
                throw new ParsingFinished();
            }
        }

        /**
         * Reads the "common" descriptor loop.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected AppInfo common_loop(int type) throws IOException, FormatFailure
        {
            AppInfo common = createAppInfo();
            common.isCommon = true;
            current = common;

            if (log.isDebugEnabled())
            {
                log.debug("application_information_section: common loop");
            }
            int tmp = readUnsignedShort();
            int common_descriptors_length = tmp & 0xFFF;

            // Must be enough to read common_descriptors + app_loop_length+CRC
            // This goes a little beyond what is tested by mark()
            assertLength(common_descriptors_length + 6);

            int saved = mark(common_descriptors_length);
            try
            {
                while (avail > 0)
                {
                    descriptor();
                    // if (Logging.LOGGING)
                    // log.debug("descriptor() done");
                }
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(e);
                }
                skipRemaining();
            }
            restore(saved);

            if (log.isDebugEnabled())
            {
                log.debug("Returning common...");
            }
            return common;
        }

        /**
         * Reads the application_loop.
         * 
         * @return a <code>Vector</code> of applications
         * @throws IOException
         * @throws FormatFailure
         */
        protected Vector application_loop() throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_information_section: app loop");
            }

            Vector foundApps = new Vector();
            int tmp = readUnsignedShort();
            int application_loop_length = tmp & 0xFFF;

            // Must at least be AppID+control_code+desc_loop_length if non-empty
            assertTrue("0 < application_loop_length < 6", application_loop_length == 0 || application_loop_length >= 6);

            // Must be enough to read CRC
            // This goes a little beyond what is tested by mark()
            assertLength(application_loop_length + 4);

            int saved = mark(application_loop_length);
            try
            {
                while (avail > 0)
                {
                    AppInfo info = application_loop_entry();
                    if (info != null)
                    {
                        // OCAP1.1.3 11.2.1.10 overrides MHP1.0.3 10.7.3. If duplicate AppIDs are found,
                        // we will use a combination of addressable attributes, priority, version, and
                        // launch order to determine which one will show up in the AppsDatabase
                        foundApps.add(info);
                    }
                }
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(e);
                }
                skipRemaining();
            }
            restore(saved);

            // Copy the hashtable elements to our return vector
            return foundApps;
        }

        /**
         * Reads one entry in the inner application loop of the
         * application_information_section.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected AppInfo application_loop_entry() throws IOException, FormatFailure
        {
            int tmp;

            // A failure here will cause us to break out of entire loop
            current = createAppInfo();
            
            AppEntry ae = current.ae;

            ae.id = application_identifier();
            ae.controlCode = readUnsignedByte();

            tmp = readUnsignedShort();

            int application_descriptors_loop_length = tmp & 0XFFF;
            int saved = mark(application_descriptors_loop_length);
            try
            {
                while (avail > 0)
                {
                    descriptor();
                    if (log.isDebugEnabled())
                    {
                        log.debug("descriptor() done");
                    }
            }
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(e);
                }
                skipRemaining();
            }
            restore(saved);

            if (log.isInfoEnabled())
            {
                log.info("Read AppInfo: " + current);
            }

            return current;
        }

        /**
         * Reads an AppID.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected AppID application_identifier() throws IOException, FormatFailure
        {
            int oid = readInt();
            int aid = readUnsignedShort();
            return new AppID(oid, aid);
        }

        protected static final int APPLICATION_DESCRIPTOR = 0x00;

        protected static final int APPLICATION_NAME_DESCRIPTOR = 0x01;

        protected static final int TRANSPORT_PROTOCOL_DESCRIPTOR = 0x02;

        protected static final int DVBJ_APPLICATION_DESCRIPTOR = 0x03;

        protected static final int DVBJ_APPLICATION_LOCATION_DESCRIPTOR = 0x04;

        protected static final int EXTERNAL_APPLICATION_AUTHORISATION_DESCRIPTOR = 0x05;

        protected static final int ROUTING_DESCRIPTOR_IPV4 = 0x06;

        protected static final int ROUTING_DESCRIPTOR_IPV6 = 0x07;

        protected static final int DVBHTML_APPLICATION_DESCRIPTOR = 0x08;

        protected static final int DVBHTML_APPLICATION_LOCATION_DESCRIPTOR = 0x09;

        protected static final int DVBHTML_APPLICATION_BOUNDARY_DESCRIPTOR = 0x0A;

        protected static final int APPLICATION_ICONS_DESCRIPTOR = 0x0B;

        protected static final int PREFETCH_DESCRIPTOR = 0x0C;

        protected static final int DII_LOCATION_DESCRIPTOR = 0x0D;

        // reserved to MHP for future use 0x0E-0x5E

        protected static final int PRIVATE_DATA_SPECIFIER_DESCRIPTOR = 0x5F;

        // reserved to MHP for future use 0x60-0x7F

        // OCAP-1.0 11.2.2.5.1
        protected static final int ADDRESSING_DESCRIPTOR = 0x6B;

        // OCAP-1.0 11.2.2.5.2
        protected static final int ATTRIBUTE_MAPPING_DESCRIPTOR = 0x6C;

        // OCAP-1.0 11.2.2.5.3
        protected static final int ADDRESSABLE_APPLICATION_DESCRIPTOR = 0x6D;

        // OCAP-1.1.1 11.2.2.4.18
        protected static final int APPLICATION_MODE_DESCRIPTOR = 0x6f;

        // User defined (note 3) 0x80-0xFE

        protected static final int REGISTERED_API_DESCRIPTOR = 0x6A;

        protected static final int REGISTERED_API_DESCRIPTOR_I15 = 0xB2;

        /**
         * Parses the information contained within a descriptor. Unhandled
         * descriptors are simply skipped.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected void descriptor() throws IOException, FormatFailure
        {
            int descriptor_tag = readUnsignedByte();
            int descriptor_length = readUnsignedByte();

            // if (Logging.LOGGING)
            // log.debug("descriptor["+descriptor_tag+","+descriptor_length+"]");

            int saved = mark(descriptor_length);
            try
            {
                descriptorImpl(descriptor_tag, descriptor_length);
                // if (Logging.LOGGING)
                // log.debug("descriptorImpl() done");
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(e);
                }
                skipRemaining();
            }
            restore(saved);
        }

        /**
         * Parses the information within a descriptor for the given <i>tag</i>
         * and <i>length</i>. Handles the following descriptors:
         * <table>
         * <tr>
         * <th>Tag</th>
         * <th>Name</th>
         * </tr>
         * <tr>
         * <td>0x00</td>
         * <td>application_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x6D</td>
         * <td>addressable_application_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x01</td>
         * <td>application_name_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x0B</td>
         * <td>application_icons_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x05</td>
         * <td>external_application_authorization_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x02</td>
         * <td>transport_protocol_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x06</td>
         * <td>routing_descriptor_ip4</td>
         * </tr>
         * <tr>
         * <td>0x07</td>
         * <td>routing_descriptor_ip7</td>
         * </tr>
         * <tr>
         * <td>0x0c</td>
         * <td>prefetch_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x0d</td>
         * <td>DII_location_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x03</td>
         * <td>dvb_j_application_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x04</td>
         * <td>dvb_j_application_location_descriptor</td>
         * </tr>
         * </table>
         * All others, including those associated with DVB-HTML are simply
         * ignored and skipped over.
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void descriptorImpl(int tag, int length) throws IOException, FormatFailure
        {
            if (SUPPORT_I15_TAGS && tag == REGISTERED_API_DESCRIPTOR_I15) tag = REGISTERED_API_DESCRIPTOR;

            switch (tag)
            {
                case APPLICATION_DESCRIPTOR:
                case ADDRESSABLE_APPLICATION_DESCRIPTOR:
                    application_descriptor(tag, length);
                    break;
                case APPLICATION_NAME_DESCRIPTOR:
                    application_name_descriptor(tag, length);
                    break;
                case TRANSPORT_PROTOCOL_DESCRIPTOR:
                    transport_protocol_descriptor(tag, length);
                    break;
                case DVBJ_APPLICATION_DESCRIPTOR:
                    dvbj_application_descriptor(tag, length);
                    break;
                case DVBJ_APPLICATION_LOCATION_DESCRIPTOR:
                    dvbj_application_location_descriptor(tag, length);
                    break;
                case EXTERNAL_APPLICATION_AUTHORISATION_DESCRIPTOR:
                    external_application_authorisation_descriptor(tag, length);
                    break;
                case ROUTING_DESCRIPTOR_IPV4:
                    routing_descriptor_ipv4(tag, length);
                    break;
                case ROUTING_DESCRIPTOR_IPV6:
                    routing_descriptor_ipv6(tag, length);
                    break;
                case APPLICATION_ICONS_DESCRIPTOR:
                    application_icons_descriptor(tag, length);
                    break;
                case PREFETCH_DESCRIPTOR:
                    prefetch_descriptor(tag, length);
                    break;
                case DII_LOCATION_DESCRIPTOR:
                    dii_location_descriptor(tag, length);
                    break;
                case ATTRIBUTE_MAPPING_DESCRIPTOR:
                    attribute_mapping_descriptor(tag, length);
                    break;
                case ADDRESSING_DESCRIPTOR:
                    addressing_descriptor(tag, length);
                    break;
                case DVBHTML_APPLICATION_DESCRIPTOR:
                case DVBHTML_APPLICATION_LOCATION_DESCRIPTOR:
                case DVBHTML_APPLICATION_BOUNDARY_DESCRIPTOR:
                    SystemEventUtil.logRecoverableError(new Exception("illegal descriptor encountered [" + tag + ","
                            + length + "]"));
                    skip(length);
                    break;
                case REGISTERED_API_DESCRIPTOR:
                    registered_api_descriptor(tag, length);
                    break;
                case APPLICATION_MODE_DESCRIPTOR:
                    application_mode_descriptor(tag, length);
                    break;
                case PRIVATE_DATA_SPECIFIER_DESCRIPTOR:
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("unknown descriptor encountered [" + tag + "," + length + "]");
                    }
                    skip(length);
                    break;
            }
            return;
        }

        /**
         * Handles the application_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x00</td>
         * <td>mandatory</td>
         * <td>exactly one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * application_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   application_profiles_length 8 uimsbf
         *   for( i=0; i&lt;N; i++ ) {
         *     application_profile 16 uimsbf
         *     version.major 8 uimsbf
         *     version.minor 8 uimsbf
         *     version.micro 8 uimsbf
         *   }
         *   service_bound_flag 1 bslbf
         *   visibility 2 bslbf
         *   reserved_future_use 5 bslbf
         *   application_priority 8 uimsbf
         *   for( i=0; i&lt;N; i++ ) {
         *     transport_protocol_label 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void application_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_descriptor");
            }

            // Only valid in app loop
            assertTrue(!current.isCommon);
            // Only use first one
            assertTrue(!current.application_descriptor_seen);

            int tmp;

            int application_profiles_length = readUnsignedByte();
            assertTrue("application_profiles_length not multiple of 5", (application_profiles_length % 5) == 0);
            
            AppEntry ae = current.ae;

            int saved = mark(application_profiles_length);
            try
            {
                /*
                 * It seems that the profiles have no significance... While for
                 * MHP: 1 = 'mhp.profile.enhanced_broadcast' 2 =
                 * 'mhp.profile.interactive_broadcast' OCAP specifies that only
                 * "ocap.profile.basic_profile" is ever returned by
                 * getProfiles(). What does ocap.profile.basic_profile map from?
                 * It doesn't seem to say. However, in OCAP 18.2.1.1 it says:
                 * 
                 * In Table 69 of DVB-MHP 1.0.2 [9]: Profile encoding is not
                 * relevant to the OCAP 1.0 Specification, because OCAP 1.0 does
                 * not subscribe to the DVB-MHP application profile model.
                 * 
                 * This makes me think that perhaps it's hardcoded to always
                 * return that. However, then where does the getVersions(String
                 * profile) information come from?
                 * 
                 * It seems like the terminal_profiles_set is only
                 * "ocap.profile.basic_profile".
                 * 
                 * According to OCAP 13.2.1.10.1: there is only one profile
                 * (OCAP 1.0) and one version (1.0).
                 * 
                 * ((application_profile iselementof terminal_profiles_set) &&
                 * ((application_version.major <
                 * terminal_version.major(application_profile)) ||
                 * ((application_version.major ==
                 * terminal_version.major(application_profile)) &&
                 * ((application_version.minor <
                 * terminal_version.minor(application_profile)) ||
                 * ((application_version.minor ==
                 * terminal_version.minor(application_profile)) &&
                 * (application_version.micro <=
                 * terminal_version.micro(application_profile)))))))
                 */

                ae.versions = new Hashtable();
                while (avail > 0)
                {
                    int application_profile = readUnsignedShort();
                    int[] version = new int[3];
                    version[0] = readUnsignedByte(); // major
                    version[1] = readUnsignedByte(); // minor
                    version[2] = readUnsignedByte(); // micro
                    
                    if (log.isDebugEnabled())
                    {
                        log.debug("\tprofile = 0x" + Integer.toHexString(application_profile) +
                                  ", " + version[0] + " " + version[1] + " " + version[2]);
                    }

                    ae.versions.put(new Integer(application_profile), version);
                }
            }
            catch (FormatFailure e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(e);
                }
                skipRemaining();
                // propogate - so we skip rest of descriptor
                restore(saved);
                throw e;
            }
            restore(saved);

            tmp = readUnsignedByte();
            ae.serviceBound = (tmp & 0x80) != 0;
            ae.visibility = (tmp & 0x60) >> 5;
            ae.priority = readUnsignedByte();

            // For addressable_application_descriptor, the next series
            // of bytes provides address labels for this application
            if (tag == ADDRESSABLE_APPLICATION_DESCRIPTOR)
            {
                int numLabels = readUnsignedByte();
                ae.addressLabels = new int[numLabels];
                for (int i = 0; i < numLabels; ++i)
                    ae.addressLabels[i] = readUnsignedByte();
            }
            else
            {
                ae.addressLabels = null;
            }

            while (avail > 0)
            {
                int label = readUnsignedByte();
                current.transportProtocolLabels.addElement(new Integer(label));
            }
            assertTrue(current.transportProtocolLabels.size() > 0);

            if (log.isDebugEnabled())
            {
                log.debug("\tservice_bound = " + ae.serviceBound);
                log.debug("\tvisibility = " + ae.visibility);
                log.debug("\tpriority = " + ae.priority);
                
                StringBuffer sb = new StringBuffer();
                sb.append("Transport protocol labels = ");
                for (Iterator i = current.transportProtocolLabels.iterator(); i.hasNext();)
            {
                    sb.append("" + i.next() + " ");
            }
                log.debug("\t" + sb.toString());
                
                if (ae.addressLabels != null)
                {
                    sb = new StringBuffer();
                    sb.append("Address labels = ");
                    for (int i = 0; i < ae.addressLabels.length; i++)
                    {
                        sb.append("" + ae.addressLabels[i] + " ");
                    }
                    log.debug("\t" + sb.toString());
                }
            }
            
            // Mark as valid and seen
            current.application_descriptor_seen = true;
        }

        /**
         * Handles the application_name_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x01</td>
         * <td>mandatory</td>
         * <td>exactly one</td>
         * <td>either</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * application_name_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     ISO_639_language_code 24 bslbf
         *     application_name_length 8 uimsbf
         *     for (i=0; i&lt;N; i++) {
         *       application_name_char 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void application_name_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_name_descriptor");
            }
            
            AppEntry ae = current.ae;

            // Only use first one
            assertTrue(ae.names == null);

            Hashtable names = new Hashtable();
            while (avail > 0)
            {
                String iso_639_language_code = utf8(3);
                String application_name = utf8();

                names.put(iso_639_language_code, application_name);
                if (log.isDebugEnabled())
                {
                    log.debug("\t " + application_name + " [" + iso_639_language_code + "]");
                }
            }
            if (names.size() > 0)
            {
                ae.names = names;
            }
        }

        /**
         * Handles the dvbj_application_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x03</td>
         * <td>mandatory</td>
         * <td>exactly one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * dvb_j_application_descriptor(){
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     parameter_length 8 uimsbf
         *     for(j=0; j<parameter_length; j++) {
         *       parameter_byte 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void dvbj_application_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("dvbj_application_descriptor");
            }
            
            AppEntry ae = current.ae;

            // Only valid in app loop
            assertTrue(!current.isCommon);
            // Only use first one
            assertTrue(ae.parameters == null);

            Vector parameters = new Vector();
            while (avail > 0)
                parameters.addElement(utf8());
            ae.parameters = new String[parameters.size()];
            parameters.copyInto(ae.parameters);
            if (log.isDebugEnabled())
            {
                for (int i = 0; i < parameters.size(); i++)
            {
                    log.debug("\tparam = " + parameters.elementAt(i));
            }
            }
        }

        /**
         * Handles the dvbj_application_location_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x04</td>
         * <td>mandatory</td>
         * <td>exactly one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * dvb_j_application_location_descriptor {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   base_directory_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     base_directory_byte 8 uimsbf
         *   }
         *   classpath_extension_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     classpath_extension_byte 8 uimsbf
         *   }
         *   for(i=0; i&lt;N; i++) {
         *     initial_class_byte 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void dvbj_application_location_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("dvbj_application_location_descriptor");
            }
            
            AppEntry ae = current.ae;

            // Only valid in app loop
            assertTrue(!current.isCommon);
            // Only take first one seen
            assertTrue(ae.baseDirectory == null);
            assertTrue(ae.classPathExtension == null);
            assertTrue(ae.className == null);

            // We keep the original version and then another version with leading
            // and trailing '/' characters removed
            ae.signaledBasedir = utf8();
            ae.baseDirectory = ae.signaledBasedir;
            if (ae.baseDirectory.startsWith("/"))
            {
                ae.baseDirectory = ae.baseDirectory.substring(1);
            }
            if (ae.baseDirectory.endsWith("/"))
            {
                ae.baseDirectory = ae.baseDirectory.substring(0, ae.baseDirectory.length()-1);
            }
            
            String classpath = utf8();
            ae.className = utf8(avail);

            if (log.isDebugEnabled())
            {
                log.debug("\tbaseDir = " + ae.baseDirectory);
                log.debug("\tinitialClass = " + ae.className);
            }
            
            StringTokenizer tok = new StringTokenizer(classpath, ";");
            String[] path = new String[tok.countTokens()];
            for (int i = 0; i < path.length; ++i)
            {
                path[i] = tok.nextToken();
                if (log.isDebugEnabled())
                {
                    log.debug("\tclasspath extension = " + path[i]);
                }
            }
            ae.classPathExtension = path;
        }

        /**
         * Handles the application_icons_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x0B</td>
         * <td>optional</td>
         * <td>zero or one</td>
         * <td>either</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * application_icons_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   icon_locator_length 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     icon_locator_byte 8 uimsbf
         *   }
         *   icon_flags 16 bslbf
         *   for (i=0; i&lt;N; i++) {
         *     reserved_future_use 8 bslbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void application_icons_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_icons_descriptor");
            }
            
            AppEntry ae = current.ae;

            ae.iconLocator = utf8();
            ae.iconFlags = readUnsignedShort();
            
            if (log.isDebugEnabled())
            {
                log.debug("\ticonLocator = " + ae.iconLocator);
                log.debug("\ticonFlags = 0x" + Integer.toHexString(ae.iconFlags));
            }

            // reserved for future use
            skip(avail);
        }

        /**
         * Handles the transport_protocol_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x02</td>
         * <td>mandatory</td>
         * <td>at least one per app</td>
         * <td>either</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * transport_protocol_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   protocol_id 16 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     selector_byte 8 uimsbf N1
         *   }
         * }
         * </pre>
         * 
         * Supports implementation-specific protocol_id==0xFFFF for local
         * transport.
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void transport_protocol_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            int protocol_id = readUnsignedShort();
            int transport_protocol_label = readUnsignedByte();

            Integer label = new Integer(transport_protocol_label);
            
            if (log.isDebugEnabled())
            {
                log.debug("transport_protocol_descriptor - label = " + label);
            }

            // Read the selector_bytes
            TransportProtocol info;
            switch (protocol_id)
            {
                case 0x0001: // Transport via OC
                    info = transport_via_OC();
                    break;
                case 0x0101: // Transport via Interaction Channel
                    info = transport_via_IC();
                    
                    // Allow duplicate labels for IC as per OCAP 11.2.1.7
                    Object orig = current.allTransportProtocols.remove(label);
                    if (orig != null)
                    {
                        assertTrue("Duplicate TP labels only allowed for IC protocol", orig instanceof IcTransportProtocol);
                        
                        // Add the new URL to the existing transport protocol object
                        IcTransportProtocol newICTP = (IcTransportProtocol)info;
                        IcTransportProtocol origICTP = (IcTransportProtocol)orig;
                        origICTP.urls.addAll(newICTP.urls);
                        
                        info = origICTP;
                    }
                    break;
                case 0xFFFF: // !!!! Implementation-specific: LoclaTransport
                    info = transport_via_Local();
                    break;
                default:
                    throw new FormatFailure("Unknown transport_protocol==" + protocol_id);
            }

            info.protocol = protocol_id;
            info.label = transport_protocol_label;

            // Save the transport protocol information
            assertTrue("Duplicated TP label", current.allTransportProtocols.get(label) == null);
            current.allTransportProtocols.put(label, info);
        }

        /**
         * Read selector bytes for transport_via_OC.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected TransportProtocol transport_via_OC() throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("\ttransport_via_OC");
            }

            OcTransportProtocol info = new OcTransportProtocol();

            int tmp = readUnsignedByte();
            if ((tmp & 0x80) != 0) // remote_connection
            {
                // Visible, but not autostart
                info.remoteConnection = true;
                /* int original_network_id = */readUnsignedShort(); // ignored
                                                                    // for OCAP
                /* int transport_stream_id = */readUnsignedShort(); // ignored
                                                                    // for OCAP
                info.serviceId = readUnsignedShort();
                
                if (log.isDebugEnabled())
                {
                    log.debug("\tserviceID = " + info.serviceId);
                }
            }
            info.componentTag = readUnsignedByte();
            if (log.isDebugEnabled())
            {
                log.debug("\tcomponentTag = " + info.componentTag);
            }

            return info;
        }

        /**
         * Read selector bytes for transport_via_IC.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected TransportProtocol transport_via_IC() throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("\ttransport_via_IC");
            }

            IcTransportProtocol info = new IcTransportProtocol();

            String url = utf8();
            info.urls.addElement(url);

            if (log.isDebugEnabled())
            {
                log.debug("\turl = " + url);
            }
            
            // These aren't so much format failures, but they keep the data
            // valid
            assertTrue(url.length() > 0);
            assertTrue(url.startsWith("http://"));

            return info;
        }

        /**
         * Read selector bytes for transport_via_Local().
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected TransportProtocol transport_via_Local() throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("transport_via_Local");
            }

            LocalTransportProtocol info = new LocalTransportProtocol();

            // remote_connection is there, but ignored
            int tmp = readUnsignedByte();
            if ((tmp & 0x80) != 0) // remote_connection
            {
                /* int original_network_id = */readUnsignedShort(); // ignored
                /* int transport_stream_id = */readUnsignedShort(); // ignored
                /* int service_id = */readUnsignedShort(); // ignored
            }

            return info;
        }

        /**
         * Handles the routing_descriptor_ipv4.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x06</td>
         * <td>mandatory for multicast ip</td>
         * <td>one or more</td>
         * <td>common</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * routing_descriptor_ip4 () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     component_tag 8 uimsbf
         *     address 32 uimsbf
         *     port_number 16 uimsbf
         *     address_mask 32 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void routing_descriptor_ipv4(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("routing_descriptor_ipv4");
            }

            // Only valid in common loop
            assertTrue(current.isCommon);
            
            AppEntry ae = current.ae;

            while (avail > 0)
            {
                AppEntry.Ipv4RoutingEntry entry = new AppEntry.Ipv4RoutingEntry();
                entry.componentTag = readUnsignedByte();
                entry.address = readInt();
                entry.port = readUnsignedShort();
                entry.mask = readInt();

                if (ae.ipRouting == null)
                {
                    ae.ipRouting = new Vector();
                }
                ae.ipRouting.addElement(entry);
            }
        }

        /**
         * Handles the routing_descriptor_ipv6.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x07</td>
         * <td>mandatory multicast ip</td>
         * <td>one or more</td>
         * <td>common</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * routing_descriptor_ip6 () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     component_tag 8 uimsbf
         *     address 128 uimsbf
         *     port_number 16 uimsbf
         *     address_mask 128 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void routing_descriptor_ipv6(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("routing_descriptor_ipv6");
            }

            // Only valid in common descriptor loop
            assertTrue(current.isCommon);
            
            AppEntry ae = current.ae;

            while (avail > 0)
            {
                AppEntry.Ipv6RoutingEntry entry = new AppEntry.Ipv6RoutingEntry();
                entry.componentTag = readUnsignedByte();
                entry.address = new int[4];
                for (int i = 0; i < 4; ++i)
                    entry.address[i] = readInt();
                entry.port = readUnsignedShort();
                entry.mask = new int[4];
                for (int i = 0; i < 4; ++i)
                    entry.mask[i] = readInt();

                if (ae.ipRouting == null)
                { 
                    ae.ipRouting = new Vector();
                }
                ae.ipRouting.addElement(entry);
            }
        }

        /**
         * Handles the external_application_authorisation_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x05</td>
         * <td>optional</td>
         * <td>zero or more</td>
         * <td>common loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * external_application_authorisation_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     application_identifier()
         *     application_priority 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void external_application_authorisation_descriptor(int tag, int length) throws IOException,
                FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("external_application_authorisation_descriptor");
            }

            // Only valid in common descriptor loop
            assertTrue(current.isCommon);

            while (avail > 0)
            {
                // application_identifier()
                int oid = readInt();
                int aid = readUnsignedShort();

                int application_priority = readUnsignedByte();

                Ait.ExternalAuthorization auth = new Ait.ExternalAuthorization();
                auth.id = new AppID(oid, aid);
                auth.priority = application_priority;
                
                if (log.isDebugEnabled())
                {
                    log.debug("\tappID = " + auth.id);
                    log.debug("\tpriority = " + auth.priority);
                }
                
                extAuths.addElement(auth);
            }
        }

        /**
         * Handles the prefetch_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x0c</td>
         * <td>optional</td>
         * <td>zero or one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * prefetch_descriptor () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     label_length
         *     for(j=0; j<label_length; j++ ) {
         *       label_char 8 uimsbf
         *     }
         *     prefetch_priority 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void prefetch_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("prefetch_descriptor");
            }

            // Only valid in app loop
            assertTrue(!current.isCommon);
            
            AppEntry ae = current.ae;

            // Skip altogether if already seen
            if (ae.prefetch != null)
            {
                skip(length);
            }
            else
            {
                AppEntry.Prefetch prefetch = new AppEntry.Prefetch();

                // transport_protocol_label 8 uimsbf
                prefetch.transportLabel = readUnsignedByte();
                
                if (log.isDebugEnabled())
                {
                    log.debug("\ttransportLabel = " + prefetch.transportLabel);
                }

                Vector v = new Vector();
                while (avail > 0)
                {
                    AppEntry.Prefetch.Pair info = new AppEntry.Prefetch.Pair();

                    // label_length
                    // for(j=0; j<label_length; j++ ) {
                    // label_char 8 uimsbf
                    // }
                    info.label = utf8();
                    // prefetch_priority 8 uimsbf
                    info.priority = readUnsignedByte();
                    if (log.isDebugEnabled())
                    {
                        log.debug("\tlabel = " + info.label + ", priority = " + info.priority);
                    }
                    v.addElement(info);
                }
                prefetch.info = new AppEntry.Prefetch.Pair[v.size()];
                v.copyInto(prefetch.info);

                // Sort prefetch entries based upon priority
                java.util.Arrays.sort(prefetch.info, new java.util.Comparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        // Reversed to give descending order
                        return ((AppEntry.Prefetch.Pair) o2).priority - ((AppEntry.Prefetch.Pair) o1).priority;
                    }
                });

                if (avail == 0)
                {
                    ae.prefetch = prefetch;
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Ignored prefetch_descriptor as it was malformed");
                    }
                }
            }
        }

        /**
         * Handles the dii_location_descriptor.
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x0d</td>
         * <td>optional</td>
         * <td>zero or one</td>
         * <td>either</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * DII_location_descriptor () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     reserved_future_use 1 bslbf
         *     DII_identification 15 uimsbf
         *     association_tag 16 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void dii_location_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("dii_location_descriptor");
            }
            
            AppEntry ae = current.ae;

            // Skip altogether if already seen
            if (ae.diiLocation != null)
            {
                skip(length);
            }
            else
            {
                AppEntry.DiiLocation dii = new AppEntry.DiiLocation();

                // transport_protocol_label 8 uimsbf
                dii.transportLabel = readUnsignedByte();

                if (log.isDebugEnabled())
                {
                    log.debug("\ttransportLabel = " + dii.transportLabel);
                }

                int n = avail / 4;
                dii.diiIdentification = new int[n];
                dii.associationTag = new int[n];

                for (int i = 0; i < n; ++i)
                {
                    dii.diiIdentification[i] = readUnsignedShort();
                    dii.associationTag[i] = readUnsignedShort();
                    if (log.isDebugEnabled())
                    {
                        log.debug("\tdiiID = " + dii.diiIdentification[i] +
                                  ", assocTag = " + dii.associationTag[i]);
                    }
                }
                if (avail == 0)
                {
                    ae.diiLocation = dii;
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Ignored dii_location_descriptor as it was malformed");
                    }
                }
            }
        }

        /**
         * Handles the attribute_mapping_descriptor
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x6C</td>
         * <td>optional</td>
         * <td>zero or more</td>
         * <td>common</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * attribute_mapping_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   attribute_id 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     attribute_name_char 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void attribute_mapping_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("attribute_mapping_descriptor");
            }

            // Only use first one
            assertTrue(current.isCommon);
            
            int attributeID = readUnsignedByte();
            String attributeName = utf8(length-1);

            if (log.isDebugEnabled())
            {
                log.debug("\t[" + attributeID + "," + attributeName + "]");
            }
            
            attributeMap.put(new Integer(attributeID), attributeName);
        }

        /**
         * Handles the addressing_descriptor
         * 
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0x6B</td>
         * <td>optional</td>
         * <td>zero or more</td>
         * <td>common</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * addressing_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   group_identifier 8 uimsbf
         *   address_label 8 uimsbf
         *   priority 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     address_expression_byte 8 uimsbf
         *   }
         * }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void addressing_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("addressing_descriptor");
            }

            // Only use first one
            assertTrue(current.isCommon);

            int groupID = readUnsignedByte();

            AddressingDescriptor ad = ait.new AddressingDescriptor();
            ad.addressLabel = readUnsignedByte();
            ad.priority = readUnsignedByte();

            if (log.isDebugEnabled())
            {
                log.debug("\tgroup = " + groupID + ", label = " + ad.addressLabel + ", priority = " + ad.priority);
            }
            
            Set securityAttributeIDs = new HashSet();
            
            // Read addressing expressions while there is data left
            // in this descriptor
            boolean error = false;
            while (avail > 0)
            {
                int opCode = readUnsignedByte();

                // Depending on the op-code we have either a 1-byte logical
                // operation or a host_attribute_comparison
                switch (opCode)
                {
                    case LogicalOp.AND:
                    case LogicalOp.OR:
                    case LogicalOp.NOT:
                    case LogicalOp.TRUE:
                    {
                        LogicalOp op = ait.new LogicalOp(opCode);
                        ad.expressions.add(op);
                        if (log.isDebugEnabled())
                        {
                            log.debug("\t" + op);
                        }
                        break;
                    }

                    case Comparison.LT:
                    case Comparison.LTE:
                    case Comparison.EQ:
                    case Comparison.GTE:
                    case Comparison.GT:
                    {
                        int attr_id = readUnsignedByte();
                        boolean security = (readUnsignedByte() & 0x80) != 0;
                        if (security)
                        {
                            securityAttributeIDs.add(new Integer(attr_id));
                        }
                        
                        String attr_value = utf8();
                        Comparison comp = ait.new Comparison(opCode, security, attr_id, attr_value);
                        ad.expressions.add(comp);
                        if (log.isDebugEnabled())
                        {
                            log.debug("\t" + comp);
                        }
                        break;
                    }

                    default:
                        if (log.isDebugEnabled())
                        {
                            log.debug("Invalid opcode in addressing_descriptor (" + opCode + ")");
                        }
                        error = true;
                        break;
                }

                if (error)
                {
                    skipRemaining();
                    return;
                }
            }
            ad.securityAttributeIDs = securityAttributeIDs;

            AitImpl.addAddressingDescriptor(addrGroups, groupID, ad);
        }

        /**
         * Handles the registered_api_descriptor.
         * 
         * <p>
         * <p>
         * <table border>
         * <tr>
         * <th>Tag</th>
         * <th>Mandatory</th>
         * <th>Count</th>
         * <th>Loop</th>
         * </tr>
         * <tr>
         * <td>0xB1</td>
         * <td>optional</td>
         * <td>&lt;=16</td>
         * <td>application loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * ocap_j_registered_api_descriptor() {
         *   descriptor_tag 8 uimsbf 0xB1
         *   descriptor_length 8 uimsbf
         *   for( i=0; i&lt;N; i++ } {
         *     registered_api_name_char 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void registered_api_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("registered_api_descriptor");
            }

            AppInfo curr = this.current;

            // Only valid in app loop
            assertTrue(!curr.isCommon);

            if (curr.registeredApiVector == null)
                curr.registeredApiVector = new Vector();

            String api = utf8(avail);
            if (log.isDebugEnabled())
            {
                log.debug("\tapi = " + api);
            }
            if (curr.registeredApiVector.size() < 16)
            {
                curr.registeredApiVector.addElement(api);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Ignoring registered api descriptor \"" + api + "\"");
                }
            }
        }

        /**
         * Handles the application mode descriptor.
         * 
         * @param tag
         * @param length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void application_mode_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_mode_descriptor");
            }
            
            current.ae.application_mode = (readUnsignedByte() >> 5) & 0x7;
            
            if (log.isDebugEnabled())
            {
                log.debug("\tmode = " + current.ae.application_mode);
            }
        }

        /**
         * Reads a length byte followed by a UTF-8 string of that length.
         * 
         * @throws IOException
         * @throws FormatFailure
         */
        protected String utf8() throws IOException, FormatFailure
        {
            return utf8(readUnsignedByte());
        }

        /**
         * Reads a UTF-8 string of the given length.
         * 
         * @length length of string to read
         * @throws IOException
         * @throws FormatFailure
         */
        protected String utf8(int length) throws IOException, FormatFailure
        {
            assertLength(length);

            byte[] buf = new byte[length];

            for (int i = 0; i < length; ++i)
                buf[i] = readByte();

            return new String(buf, 0, buf.length);
        }

        /**
         * Used to <i>mark</i> an area of a specified length for reading. It
         * updates the current number of bytes available for reading (as
         * recorded in <i>avail</i>) with the given length. The old value is
         * returned (minus the new <i>length</i>) for storage on the stack and
         * later restoration via <code>restore()</code>.
         * <p>
         * Used like this whereever a new length is read in:
         * 
         * <pre>
         * int newLength = readUnsignedByte();
         * int saved = mark(newLength); // updates avail
         * try
         * {
         *     // perform reading/parsing within new &quot;avail&quot;
         * }
         * catch (FormatFailure e)
         * {
         *     skipRemaining();
         * }
         * restore(saved);
         * </pre>
         * 
         * @param length
         *            the new length which must be <code>&lt;=</code> the
         *            existing value of <i>avail</i>
         * @return the current value of <i>avail</i> minus <i>length</i> for
         *         later restoration via <code>restore()</code>
         * 
         * @throws FormatFailure
         *             if <code>length &lt; avail</code>
         */
        protected int mark(int length) throws FormatFailure
        {
            if (false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("mark(" + length + ")");
                }
            }

            if (length > avail) throw new FormatFailure("length>avail: " + length + ">" + avail);

            int tmp = avail - length;
            avail = length;
            return tmp;
        }

        /**
         * Used to restore a previously saved value available bytes for an
         * containing section (i.e., an outer loop or descriptor).
         * 
         * @param saved
         *            the value returned by a previous call to
         *            <code>mark(length)</code> to be restored as the value of
         *            <i>avail</i>
         * 
         * @throws IllegalArgumentException
         *             if all data in the previous area hadn't been read
         */
        protected void restore(int saved) throws IllegalArgumentException
        {
            if (false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("restore(" + saved + ")");
                }
            }

            if (avail != 0) throw new IllegalArgumentException("Still have bytes to read " + avail);

            avail = saved;
        }

        /**
         * Skips the remaining bytes in the current area.
         * 
         * @throws IllegalArgumentException
         *             if all data in the previous area had already been read
         * @throws IOException
         */
        protected void skipRemaining() throws IOException
        {
            if (avail < 0) throw new IllegalArgumentException("No bytes remain for area");
            skip(avail);
        }

        /**
         * Used to test a formatting condition.
         * 
         * @throws FormatFailure
         *             if the condition is <code>false</code>
         */
        protected void assertTrue(String str, boolean test) throws FormatFailure
        {
            if (!test) throw new FormatFailure(str);
        }

        /**
         * Used to test a formatting condition.
         * 
         * @throws FormatFailure
         *             if the condition is <code>false</code>
         */
        protected void assertTrue(boolean test) throws FormatFailure
        {
            if (!test) throw new FormatFailure();
        }

        /**
         * Tests that there are at least <i>length</i> bytes remaining in the
         * current area.
         * 
         * @throws FormatFailure
         *             if the condition is <code>false</code>
         */
        protected void assertLength(int length) throws FormatFailure
        {
            if (length > avail)
                throw new FormatFailure("Not enough bytes remaining (" + length + ">" + avail + ")");
        }

        /**
         * Reads bytes into the given array and adjusts <i>avail</i>
         * accordingly.
         * 
         * @param buffer
         *            the buffer to read into
         * @param ofs
         *            the offset into the buffer to start writing
         * @param length
         *            the number of bytes to read
         * @return the number of bytes to read
         * @throws FormatFailure
         *             if not enough <code>byte</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented reading
         */
        protected int readBytes(byte[] buffer, int ofs, int length) throws IOException, FormatFailure
        {
            assertLength(length);

            if (is.read(buffer, ofs, length) != length) throw new EOFException("Unexpected EOF");
            if (DEBUG)
            {
                    for (int i = ofs; i < length; ++i)
                    {
                    if (log.isDebugEnabled())
                    {
                        log.debug(Integer.toHexString(buffer[i] & 0xFF));
                    }
                }
            }
            avail -= length;

            if (enableCRC)
            {
                for (int i = ofs; i < length; ++i)
                    crc.updateCRC(buffer[i] & 0xFF);
            }

            return length;
        }

        /**
         * Reads and returns a single <code>byte</code> and adjusts <i>avail</i>
         * accordingly.
         * 
         * @return the signed value
         * @throws FormatFailure
         *             if not enough <code>byte</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected byte readByte() throws IOException, FormatFailure
        {
            assertLength(1);

            byte x = is.readByte();
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(Integer.toHexString(x & 0xFF));
                }
            }
            avail -= 1;

            if (enableCRC)
            {
                crc.updateCRC(x & 0xFF);
            }

            return x;
        }

        /**
         * Reads and returns a single <code>short</code> and adjusts
         * <i>avail</i> accordingly.
         * 
         * @return the signed value
         * @throws FormatFailure
         *             if not enough <code>short</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected short readShort() throws IOException, FormatFailure
        {
            assertLength(2);

            short x = is.readShort();
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(Integer.toHexString(x & 0xFFFF));
                }
            }
            avail -= 2;

            if (enableCRC)
            {
                crc.updateCRC((x >> 8) & 0xFF);
                crc.updateCRC( x       & 0xFF);
            }

            return x;
        }

        /**
         * Reads and returns a single <code>int</code> and adjusts <i>avail</i>
         * accordingly.
         * 
         * @return the signed value
         * @throws FormatFailure
         *             if not enough <code>int</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected int readInt() throws IOException, FormatFailure
        {
            assertLength(4);

            int x = is.readInt();
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(Integer.toHexString(x));
                }
            }
            avail -= 4;

            if (enableCRC)
            {
                crc.updateCRC((x >> 24) & 0xFF);
                crc.updateCRC((x >> 16) & 0xFF);
                crc.updateCRC((x >>  8) & 0xFF);
                crc.updateCRC( x        & 0xFF);
            }

            return x;
        }

        /**
         * Reads and returns a single 32-bit unsigned integer value and return
         * it as a <code>long</code>.  Adjusts <i>avail</i> accordingly.
         * 
         * @return the unsigned int value
         * @throws FormatFailure
         *             if not enough <code>int</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected long readUnsignedInt() throws IOException, FormatFailure
        {
            assertLength(4);

            long x = 0;
            x |= ((is.readByte() << 24) & 0xFF000000);
            x |= ((is.readByte() << 16) & 0xFF0000);
            x |= ((is.readByte() << 8)  & 0xFF00);
            x |= ( is.readByte()        & 0xFF);
            
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(Long.toHexString(x));
                }
            }
            avail -= 4;
            
            if (enableCRC)
            {
                crc.updateCRC((int)((x >> 24) & 0xFF));
                crc.updateCRC((int)((x >> 16) & 0xFF));
                crc.updateCRC((int)((x >>  8) & 0xFF));
                crc.updateCRC((int)( x        & 0xFF));
            }

            return x;
        }

        /**
         * Reads and returns a single <code>byte</code> and adjusts <i>avail</i>
         * accordingly.
         * 
         * @return the <code>byte</code> expressed as an unsigned value
         * @throws FormatFailure
         *             if not enough <code>byte</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected int readUnsignedByte() throws IOException, FormatFailure
        {
            return readByte() & 0xFF;
        }

        /**
         * Reads and returns a single <code>short</code> and adjusts
         * <i>avail</i> accordingly.
         * 
         * @return the <code>short</code> expressed as an unsigned value
         * @throws FormatFailure
         *             if not enough <code>short</code>s remain
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         */
        protected int readUnsignedShort() throws IOException, FormatFailure
        {
            return readShort() & 0xFFFF;
        }

        /**
         * Skips <i>bytes</i> bytes, adjusting <i>avail</i> accordingly. Does
         * not throw <code>FormatFailure</code> because it is used in cases
         * where such a failure should not be thrown.
         * 
         * @return the number of bytes skipped
         * @throws IOException
         *             if an I/O error occurred which prevented the reading
         * @throws IllegalArgumentException
         *             if too many bytes are to be skipped
         */
        protected long skip(long bytes) throws IOException
        {
            if (bytes > avail)
                throw new IllegalArgumentException("Skipping too many bytes (" + bytes + ">" + avail + ")");

            long skipped = is.skip(bytes);
            avail -= (int) skipped;
            return skipped;
        }

        /**
         * The number of bytes remaining in the current section, loop, or
         * descriptor.
         */
        protected int avail = Integer.MAX_VALUE;
    }

    /**
     * Private exception class used to signal format parsing failures.
     */
    protected static class FormatFailure extends Exception
    {
        public FormatFailure(String str)
        {
            super(str);
        }

        public FormatFailure()
        {
            super();
        }
    }

    /**
     * Private exception class used to indicate failure to read application
     * information section due to end of input.
     * 
     * @author Aaron Kamienski
     */
    protected static class ParsingFinished extends Exception
    {
        // empty
    }

    /**
     * Contains information about an application specified in the AIT. Most
     * fields are self-explanatory, corresponding to information contained
     * within AIT descriptors.
     * <p>
     * This class also defines additional fields that are used during parsing:
     * <ul>
     * <li> {@link #valid}
     * <li> {@link #isCommon}
     * <li> {@link #transportProtocolLabels}
     * <li> {@link #allTransportProtocols}
     * <li> {@link #application_descriptor_seen}
     * <li> {@link #registeredApiVector}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    protected static class AppInfo
    {
        public AppInfo()
        {
            ae = new AppEntry();
        }
        
        protected AppEntry ae;

        /**
         * <code>true</code> if this <code>AppInfo</code> is valid.
         */
        protected boolean valid = true;

        /**
         * <code>true</code> if this <code>AppInfo</code> was generated by the
         * <i>common</i> loop.
         */
        protected boolean isCommon = false;

        /**
         * List of available transport protocols for this app.
         * 
         * @see #allTransportProtocols
         */
        public Vector transportProtocolLabels = new Vector();

        /**
         * Set of available transport protocols (also see those stored within
         * common <code>AppInfo</code>). Values are going to be instances of
         * <code>OCInfo</code> or <code>IPInfo</code>. Keyed by
         * <code>Integer</code>-wrapped transport protocol label.
         */
        public Hashtable allTransportProtocols = new Hashtable(); // keyed by
                                                                  // transport_protocol_label

        /**
         * Mandatory Descriptor seen flag. Other mandatory descriptors are
         * tracked using AppInfo data flags.
         */
        protected boolean application_descriptor_seen = false;

        /**
         * Used to store registeredApis during parsing.
         */
        protected Vector registeredApiVector;

        /**
         * Update this <code>AppInfo</code> with "common" information. This may
         * be used to consolidate common loop information into a single
         * <code>AppInfo</code> or to resolve an application entry using the
         * information from the common loop.
         * <p>
         * Updates the following information:
         * <ul>
         * <li> {@link #type}
         * <li> {@link #names}
         * <li> {@link #allTransportProtocols}
         * <li> {@link #transportProtocols} if !{@link #isCommon} using
         * {@link #transportProtocolLabels labels} and
         * {@link #allTransportProtocols protocols}
         * <li>icon information {@link #iconLocator} and {@link #iconFlags}
         * <li> {@link #diiLocation DII}
         * <li> {@link #ipRouting IP routing}
         * </ul>
         * 
         * @param common
         *            common loop information to add to this
         *            <code>AppInfo</code>
         */
        protected void updateWithCommon(AppInfo common)
        {
            AppEntry cae = common.ae;
            
            // names
            if (cae.names != null && cae.names.size() > 0)
            {
                if (ae.names == null)
                    ae.names = (Hashtable) cae.names.clone();
                else
                    add(ae.names, cae.names, false);
            }

            // allTransportProtocols
            if (!common.allTransportProtocols.isEmpty())
            {
                if (allTransportProtocols.isEmpty())
                    allTransportProtocols = (Hashtable) common.allTransportProtocols.clone();
                else
                    add(allTransportProtocols, common.allTransportProtocols, false);
            }

            // Generate actual set of TransportProtocols... (doesn't apply to
            // common loop)
            if (!isCommon)
            {
                Vector vector = new Vector();
                for (Enumeration e = transportProtocolLabels.elements(); e.hasMoreElements();)
                {
                    TransportProtocol tp = (TransportProtocol) allTransportProtocols.get(e.nextElement());
                    if (tp != null)
                    {
                        vector.addElement(tp);
                    }
                }
                ae.transportProtocols = new TransportProtocol[vector.size()];
                vector.copyInto(ae.transportProtocols);
            }

            // app icons (only accept one)
            if (ae.iconLocator == null && cae.iconLocator != null)
            {
                ae.iconLocator = cae.iconLocator;
                ae.iconFlags = cae.iconFlags;
            }

            // DII location (only accept one)
            if (ae.diiLocation == null && cae.diiLocation != null) ae.diiLocation = cae.diiLocation;

            // routing descriptors
            if (cae.ipRouting != null)
            {
                if (ae.ipRouting == null)
                    ae.ipRouting = (Vector) cae.ipRouting.clone();
                else
                    ae.ipRouting.addAll(cae.ipRouting);
            }
        }

        /**
         * Tests whether the given AppInfo is valid or not. If not, valid is
         * cleared.
         */
        protected boolean validate()
        {
            // Generate array of registered api names
            if (registeredApiVector != null)
            {
                ae.registeredApi = new String[registeredApiVector.size()];
                registeredApiVector.copyInto(ae.registeredApi);
            }
            else
            {
                ae.registeredApi = new String[0];
            }
            registeredApiVector = null;

            // Forget objects used during parsing
            allTransportProtocols.clear();
            transportProtocolLabels.clear();

            valid =
            // missing application_descriptor
            application_descriptor_seen
                    // invalid app_id
                    && ae.id.getOID() != 0 && ae.id.getAID() != 0
                    // missing application_name(s)
                    && ae.names != null && ae.names.size() > 0
                    // missing dvb_j_application
                    && ae.parameters != null
                    // missing/invalid dvbj_application_location
                    && ae.signaledBasedir != null && ae.signaledBasedir.length() > 0
                    && ae.classPathExtension != null
                    && ae.className != null && ae.className.length() > 0
                    // transport_protocol is missing
                    && ae.transportProtocols != null && ae.transportProtocols.length > 0;
            return valid;
        }
    }

    private static final Logger log = Logger.getLogger(AitParser.class.getName());

    private static final boolean DEBUG = false;

    protected static final boolean SUPPORT_I15_TAGS = true && "true".equals(MPEEnv.getEnv("OCAP.xait.I15"));
}
