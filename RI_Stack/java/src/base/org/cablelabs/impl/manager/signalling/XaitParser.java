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

import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.XaitImpl;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.application.OcapAppAttributes;

/**
 * Standalone parsing for an XAIT. This class provides an implementation for the
 * {@link Xait} class based upon an array of
 * <code>Section</code>s or an <code>InputStream</code>.
 * <p>
 * Definition of the descriptors and error handling implemented here can be
 * found in MHP 10. Information about the OCAP-specific changes can be found in
 * OCAP 11.
 * 
 * @author Aaron Kamienski
 * @see "MHP-1.0.2 Section 10"
 * @see "OCAP-1.0 Section 11"
 */
class XaitParser extends AitParser
{
    /**
     * Constructs an <code>XaitParser</code> object.
     * 
     * @param source
     *            the source of the XAIT
     */
    XaitParser(int source)
    {
        ait = new XaitImpl();
        this.source = source;
    }
    
    /**
     * Return a signalling object based on the parsed data
     * 
     * @return the signalling
     */
    public Ait getSignalling()
    {
        XaitImpl xait = (XaitImpl)ait;
        xait.initialize(version,externalAuth,allApplications,attributeMap,addrGroups,
                        source,privilegedCertificates,abstractServices);
        return ait;
    }

    /**
     * Overrides {@link AitParser#createAppInfo}. Returns an
     * <code>XaitParser</code>-specific instance of
     * <code>AitParser.AppInfo</code>
     * 
     * @return an instance of {@link AppInfo}
     */
    protected AppInfo createAppInfo()
    {
        return new XAppInfo();
    }

    /**
     * Parses the AIT/XAIT read from the given <code>InputStream</code> as
     * concatenated <i>application_information_sections</i>.
     * 
     * @param input
     *            AIT is given in form of an <code>InputStream</code>
     * @throws IOException
     *             if there are problems reading the InputStream
     * @throws IllegalArgumentException
     *             if any of the section headers are invalid
     */
    void parse(InputStream input) throws IOException, IllegalArgumentException
    {
        if (input == null) throw new IllegalArgumentException("null is not a valid xait");

        // Wrap the InputStream within a DataInputStream
        if (!(input instanceof BufferedInputStream) && !(input instanceof ByteArrayInputStream))
        {
            input = new BufferedInputStream(input);
        }
        DataInputStream is = new DataInputStream(input);

        // Read multiple application_information_sections
        AppInfo common = createAppInfo();
        common.isCommon = true;
        Vector sections = new Vector();
        do
        {
            try
            {
                org.cablelabs.impl.manager.signalling.AitParser.SectionParser section =
                    createSectionParser(is);

                if (!section.application_information_section())
                {
                    throw new IllegalArgumentException("Invalid section header");
                }
                sections.addElement(section);

                // Consolidate common information in one AppInfo
                common.updateWithCommon(section.commonInfo);
            }
            catch (FormatFailure e)
            {
                // Simply forget any and all information in this section
                if (log.isInfoEnabled())
                {
                    log.info("Ignoring section because of error", e);
                }
                throw new IllegalArgumentException(e.getMessage());
            }
            catch (ParsingFinished e)
            {
                // Stop when data's no longer available.
                break;
            }
        }
        while (true);

        if (sections.size() == 0)
        {
            throw new IllegalArgumentException("no application_information_section(s)");
        }

        // Save the sections
        saveSections(sections, common);
    }
    
    /**
     * Overrides {@link AitParser#saveSections}. This additionally saves
     * information about {@link #abstractServices abstract services} and
     * {@link #privilegedCertificates privileged certificates}.
     * 
     * @see #saveApplication(AitParser.AppInfo, AitParser.AppInfo)
     */
    protected void saveSections(Vector sections, AitParser.AppInfo common)
    {
        // Generate single list of abstract services from all sections
        // Generate single set of privileged certs
        for (Enumeration e = sections.elements(); e.hasMoreElements();)
        {
            SectionParser section = (SectionParser) e.nextElement();

            // Save all services
            add(abstractServices, section.services, false);

            // Save 1st set of privCertBytes
            if (privilegedCertificates == null)
            { 
                privilegedCertificates = section.privCertBytes;
            }
        }

        // Finally continue with saving of section information
        super.saveSections(sections, common);
    }

    /**
     * Overrides {@link AitParser#saveApplication}. Associates the given
     * application with the proper abstract service, if there is one. Also takes
     * care of interpreting {@link OcapAppAttributes#REMOTE} as
     * {@link OcapAppAttributes#PRESENT} per OCAP 11.2.2.3 (ECN 913).
     */
    protected void saveApplication(AppInfo app, AppInfo common)
    {
        super.saveApplication(app, common);
        
        XAppEntry xae = (XAppEntry)app.ae;
        
        // Interpret REMOTE as PRESENT (per OCAP 11.2.2.3 -- ECN 913)
        if (xae.controlCode == OcapAppAttributes.REMOTE)
        {
            xae.controlCode = OcapAppAttributes.PRESENT;
        }
        
        // Add to our abstract service apps list
        AbstractServiceEntry ase =
            (AbstractServiceEntry)abstractServices.get(new Integer(xae.serviceId));
        if (ase != null)
        {
            ase.apps.add(xae);
        }
    }

    /**
     * Overrides {@link AitParser#createSectionParser(DataInputStream)}.
     * 
     * @return an instance of {@link SectionParser}
     */
    protected AitParser.SectionParser createSectionParser(DataInputStream is)
    {
        return new SectionParser(is);
    }
    
    // Data required to construct an XAIT
    protected int source;
    protected byte[] privilegedCertificates;
    protected Hashtable abstractServices = new Hashtable();

    /**
     * <code>XaitParser</code>-specific implementation of
     * {@link AitParser.SectionParser} that handles XAIT-specific syntax and
     * semantics. The following XAIT-specific information fields are accessible
     * after successful parsing via {@link #application_information_section()}:
     * <ul>
     * <li>Abstract service information in {@link #services}
     * <li>Privileged certificate bytes in {@link #privCertBytes}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    protected class SectionParser extends AitParser.SectionParser
    {
        /**
         * The abstract service information contained in the common loop, if
         * any.
         */
        public Hashtable services = new Hashtable();

        /**
         * The privileged certificate bytes contained in the common loop, if
         * any.
         */
        public byte[] privCertBytes;

        /**
         * Creates an instance of <code>SectionParser</code>.
         * 
         * @param is
         *            the input stream to read from
         */
        SectionParser(DataInputStream is)
        {
            super(is);
        }

        protected static final int ABSTRACT_SERVICE_DESCRIPTOR = 0x66;

        protected static final int UNBOUND_APPLICATION_DESCRIPTOR = 0x67;

        protected static final int APPLICATION_STORAGE_DESCRIPTOR = 0x69;

        protected static final int PRIVILEGED_CERTIFICATE_DESCRIPTOR = 0x68;

        protected static final int ABSTRACT_SERVICE_DESCRIPTOR_I15 = 0xAE;

        protected static final int UNBOUND_APPLICATION_DESCRIPTOR_I15 = 0xAF;

        protected static final int APPLICATION_STORAGE_DESCRIPTOR_I15 = 0xB0;

        protected static final int PRIVILEGED_CERTIFICATE_DESCRIPTOR_I15 = 0xB1;

        /**
         * Overrides <code>super.descriptorImpl()</code> to provide additional
         * support for XAIT-specific tags:
         * 
         * <table>
         * <tr>
         * <th>Tag</th>
         * <th>Name</th>
         * </tr>
         * <tr>
         * <td>0x66</td>
         * <td>abstract_service_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x67</td>
         * <td>unbound_application_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x69</td>
         * <td>application_storage_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x68</td>
         * <td>privileged_certificate_descriptor</td>
         * </tr>
         * <tr>
         * <td>0x6a</td>
         * <td>registered_api_descriptor</td>
         * </tr>
         * </table>
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
            if (SUPPORT_I15_TAGS) // if static final false, then code will be
                                  // removed
            {
                switch (tag)
                {
                    case ABSTRACT_SERVICE_DESCRIPTOR_I15:
                    case ABSTRACT_SERVICE_DESCRIPTOR:
                        abstract_service_descriptor(tag, length);
                        break;
                    case UNBOUND_APPLICATION_DESCRIPTOR_I15:
                    case UNBOUND_APPLICATION_DESCRIPTOR:
                        unbound_application_descriptor(tag, length);
                        break;
                    case APPLICATION_STORAGE_DESCRIPTOR_I15:
                    case APPLICATION_STORAGE_DESCRIPTOR:
                        application_storage_descriptor(tag, length);
                        break;
                    case PRIVILEGED_CERTIFICATE_DESCRIPTOR_I15:
                    case PRIVILEGED_CERTIFICATE_DESCRIPTOR:
                        privileged_certificate_descriptor(tag, length);
                        break;
                    default:
                        super.descriptorImpl(tag, length);
                        break;
                }
            }
            else
            {
                switch (tag)
                {
                    case ABSTRACT_SERVICE_DESCRIPTOR:
                        abstract_service_descriptor(tag, length);
                        break;
                    case UNBOUND_APPLICATION_DESCRIPTOR:
                        unbound_application_descriptor(tag, length);
                        break;
                    case APPLICATION_STORAGE_DESCRIPTOR:
                        application_storage_descriptor(tag, length);
                        break;
                    case PRIVILEGED_CERTIFICATE_DESCRIPTOR:
                        privileged_certificate_descriptor(tag, length);
                        break;
                    default:
                        super.descriptorImpl(tag, length);
                        break;
                }
            }
        }

        /**
         * Handles the abstract_service_descriptor.
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
         * <td>0xae</td>
         * <td>mandatory</td>
         * <td>one or more</td>
         * <td>common loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * abstract_service_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAE
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   reserved_for_future_use 7 uimsbf
         *   auto_select 1 bslbf
         *   for (i=0; i&lt;N; i++) {
         *     service_name_byte 8 uimsbf
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
        protected void abstract_service_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("abstract_service_descriptor");
            }

            // Only valid in common descriptor loop
            assertTrue(current.isCommon);

            AbstractServiceEntry service = new AbstractServiceEntry();
            service.apps = new Vector();

            int tmp = readUnsignedByte() << 16;
            tmp |= readUnsignedByte() << 8;
            tmp |= readUnsignedByte();
            service.id = tmp;
            if (ENFORCE_SERVICEID)
            {
                // host: 0x010000 - 0x1FFFFF
                if (source == Xait.HOST_DEVICE)
                    assertTrue(service.id >= 0x010000 && service.id <= 0x1FFFF);
                // MSO: 0x020000 - 0xFFFFFF
                else
                    assertTrue(service.id >= 0x020000 && service.id <= 0xFFFFFF);
            }
            else
            {
                assertTrue(service.id >= 0x010000 && service.id <= 0xFFFFFF);
            }

            tmp = readUnsignedByte();
            service.autoSelect = (tmp & 0x01) != 0;

            service.name = utf8(avail);

            services.put(new Integer(service.id), service);

            if (log.isInfoEnabled())
            {
                log.info("\t" + service);
            }
        }

        /**
         * Handles the unbound_application_descriptor.
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
         * <td>0xaf</td>
         * <td>mandatory</td>
         * <td>exactly one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * unbound_application_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAF
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   version_number 32 uimsbf
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
        protected void unbound_application_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("unbound_application_descriptor");
            }

            XAppInfo curr = (XAppInfo)current;

            // Only valid in app loop
            assertTrue(!curr.isCommon);

            // Only take first one seen
            assertTrue(!curr.unbound_application_descriptor_seen);
            
            XAppEntry xae = (XAppEntry)curr.ae;

            int tmp = readUnsignedByte() << 16;
            tmp |= readUnsignedByte() << 8;
            tmp |= readUnsignedByte();
            xae.serviceId = tmp;

            xae.version = readUnsignedInt();

            if (log.isDebugEnabled())
            {
                log.debug("\tserviceID = 0x" + Integer.toHexString(xae.serviceId) +
                          ", version = " + xae.version);
            }
            
            curr.unbound_application_descriptor_seen = true;
        }

        /**
         * Handles the application_storage_descriptor.
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
         * <td>optional</td>
         * <td>one</td>
         * <td>app loop</td>
         * </tr>
         * </table>
         * 
         * If none is included, a storage_priority of zero is assumed.
         * 
         * <pre>
         * application_storage_descriptor() {
         *   descriptor_tag 8 uimsbf 0xB0
         *   descriptor_length 8 uimsbf
         *   storage_priority 16 uimsbf
         *   launch_order 8 uimsbf
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
        protected void application_storage_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("application_storage_descriptor");
            }

            XAppInfo curr = (XAppInfo)current;

            // Only valid in app loop
            assertTrue(!curr.isCommon);

            // Only take first one seen
            assertTrue(!curr.application_storage_descriptor_seen);
            
            XAppEntry xae = (XAppEntry)curr.ae;

            xae.storagePriority = readUnsignedShort();
            xae.launchOrder = readUnsignedByte();

            if (log.isDebugEnabled())
            {
                log.debug("\tstoragePriority = " + xae.storagePriority +
                          ", launchOrder = " + xae.launchOrder);
            }
            
            curr.application_storage_descriptor_seen = true;
        }

        /**
         * Handles the privileged_certificate_descriptor.
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
         * <td>mandatory</td>
         * <td>one</td>
         * <td>common loop</td>
         * </tr>
         * </table>
         * 
         * <pre>
         * privileged_certificate_descriptor() {
         *   descriptor_tag 8 uimbsf 0xB1
         *   descriptor_length 8 uimbsf
         *   for(i=0; i&lt;N; i++) {
         *     for(j=0; j&lt;20; j++) {
         *       certificate_identifier_byte 8 uimbsf SHA-1 Hash
         *     }
         *   }
         * </pre>
         * 
         * @param tag
         *            the descriptor tag
         * @param length
         *            the descriptor length
         * @throws IOException
         * @throws FormatFailure
         */
        protected void privileged_certificate_descriptor(int tag, int length) throws IOException, FormatFailure
        {
            if (log.isDebugEnabled())
            {
                log.debug("privileged_certificate_descriptor");
            }

            // Only valid in common loop
            assertTrue(current.isCommon);

            // Only take first one seen
            assertTrue(privCertBytes == null);

            // certificate_identifiers are 20 bytes in length
            assertTrue(avail > 0 && avail % 20 == 0);

            // Read certificate_identifiers
            privCertBytes = new byte[avail];
            readBytes(privCertBytes, 0, avail);
            
            if (log.isDebugEnabled())
            {
                int i = 0;
                while (i < privCertBytes.length)
                {
                    StringBuffer sb = new StringBuffer();
                    do
                    {
                        int x = privCertBytes[i++] & 0xFF;
                        String hex = Integer.toHexString(x);
                        sb.append(hex.length() == 1 ? ("0" + hex) : hex);
                    }
                    while (i % 20 != 0);
                    log.debug("\tcert = " + sb.toString());
                }
            }
        }
    }

    /**
     * Contains information about an application specified in the XAIT. Most
     * fields are self-explanatory, corresponding to information contained
     * within XAIT descriptors.
     * <p>
     * This class also defines additional fields that are used during parsing:
     * <ul>
     * <li> {@link #unbound_application_descriptor_seen}
     * <li> {@link #application_storage_descriptor_seen}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    protected class XAppInfo extends AppInfo 
    {
        public XAppInfo()
        {
            XAppEntry xae = new XAppEntry();
            xae.source = source;
            ae = xae;
        }
        
        protected boolean unbound_application_descriptor_seen = false;

        protected boolean application_storage_descriptor_seen = false;

        /**
         * Tests whether the given AppInfo is valid or not. If not, valid is
         * cleared.
         */
        protected boolean validate()
        {
            if (super.validate())
            {
                valid =
                // missing unbound_application_descriptor
                unbound_application_descriptor_seen
                // Unbound apps are bound to a single Abstract Service (OCAP 11.2.2.3.3)
                        && ae.serviceBound;
            }
            return valid;
        }
    }

    private static final boolean ENFORCE_SERVICEID = false;

    private static final Logger log = Logger.getLogger(XaitParser.class.getName());
}
