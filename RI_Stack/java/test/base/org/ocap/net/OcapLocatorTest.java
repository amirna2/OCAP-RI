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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

package org.ocap.net;

import junit.framework.*;
import org.davic.net.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.ocap.si.StreamType;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

// TODO: Further self tests (to verify expected generation of locator string by TestParam) is useful.
// TODO: Find way to simplify negative testing in testConstructor*() methods; using a common framework

/**
 * Tests the OcapLocator.
 * 
 * Note that this is for I12.
 * 
 * <pre>
 * ocap_url = ocap_scheme ":" ocap_hier_part
 * ocap_scheme = "ocap"
 * ocap_hier_part = ocap_net_path | ocap_abs_path
 *     (See restriction 1 in OCAP 16.2.1.1.2.1)
 * ocap_net_path = "//" ocap_entity [ ocap_abs_path ]
 *     (See restriction 2 in OCAP 16.2.1.1.2.1)
 * ocap_entity = ocap_service | ocap_service_component | ocap_transport
 * ocap_service = source_id | named_service | ocap_program
 * ocap_service_component = ocap_service [ "." program_elements ] [ ";" event_id ]
 * program_elements = language_elements | index_elements | PID_elements |
 * component_elements | component_tag_elements
 * language_elements = stream_type [ "," ISO_639_language_code ] * ( "&" stream_type
 * [ "," ISO_639_language_code ] )
 *     (See restriction 3 in OCAP 16.2.1.1.2.1)
 * index_elements = stream_type [ "," index ] * ( "&" stream_type [ "," index ] )
 * PID_elements = "+" PID * ( "&" PID )
 * component_elements = "$" component_name * ( "&" component_name )
 * component_tag_elements = "@" component_tag * ( "&" component_tag )
 * ocap_program = "f=" frequency "." program_number [."m=" modulation_format] |
 * "oobfdc." program_number
 * ocap_transport = "f=" frequency [".m=" modulation_format]
 * source_id = hex_string
 * named_service = "n=" service_name
 * service_name = 1* (unreserved_not_dot | escaped)
 *     (See restriction 4 in OCAP 16.2.1.1.2.1)
 * component_name = 1* (unreserved | escaped)
 *     (See restriction 5 in OCAP 16.2.1.1.2.1)
 * component_tag = hex_string
 * frequency = hex_string
 * program_number = hex_string
 * modulation_format = hex_string
 * stream_type = hex_string
 *     (See restriction 6 in OCAP 16.2.1.1.2.1)
 * ISO_639_language_code = alpha alpha alpha
 *     (See restriction 7 in OCAP 16.2.1.1.2.1)
 * index = hex_string
 * PID = hex_string
 * event_id = hex_string
 * hex_string = "0x" 1*hex
 * hex = digit | "A" | "B" | "C" | "D" | "E" | "F" | "a" | "b" | "c" |
 * "d" | "e" | "f"
 * digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
 * ocap_abs_path = "/" path_segments
 *     (path_segments is defined in IETF RFC 2396 [22].)
 *     (See restriction 8 in OCAP 16.2.1.1.2.1)
 * path_segments = segment *( "/" segment )
 * segment = *pchar *( ";" param )
 * param = *pchar
 * pchar = unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ","
 * unreserved = alphanum | mark
 * unreserved_not_dot = alphanum | mark_not_dot
 * alphanum = alpha | digit
 * alpha = lowalpha | upalpha
 * lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k"
 * | "l" | "m" | "n" | "o" | "p" | "q" | "r" | "s" | "t" | "u" |
 * "v" | "w" | "x" | "y" | "z"
 * upalpha = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K"
 * | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U" |
 * "V" | "W" | "X" | "Y" | "Z"
 * escaped = "%" hex hex
 * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
 * mark_not_dot = "-" | "_" | "!" | "~" | "*" | "!" | "(" | ")"
 * </pre>
 * 
 * In non-informal notation:
 * 
 * <pre>
 * ocap://<source_id>[.<stream_type>[,<ISO_639_language_code>]{&<stream_type>[,<ISO_639_language_code>]}] [;<event_id>]{/<path_segments>}
 * ocap://<source_id>[.<stream_type>[,<index>]{&<stream_type>[,<index>]}][;<event_id>]{/<path_segments>}
 * ocap://<source_id>[.+<PID>{&<PID>}][;<event_id>]{/<path_segments>}
 * ocap://<source_id>[.@<component_tag>{&<component_tag>}][;<event_id>]{/<path_segments>}
 * ocap://<source_id>[.$<component_name>{&<component_name>}][;<event_id>]{/<path_segments>}
 * ocap://n=<service_name>[.<stream_type>[,<ISO_639_language_code>]{&<stream_type>[,<ISO_639_language_code>]}] [;<event_id>]{/<path_segments>}
 * ocap://n=<service_name>[.<stream_type>[,<index>]{&<stream_type>[,<index>]}][;<event_id>]{/<path_segments>}
 * ocap://n=<service_name>[.+<PID>{&<PID>}][;<event_id>]{/<path_segments>}
 * ocap://n=<service_name>[.@<component_tag>{&<component_tag>}][;<event_id>]{/<path_segments>}
 * ocap://n=<service_name>[.$<component_name>{&<component_name>}][;<event_id>]{/<path_segments>}
 * ocap://f=<frequency>.<program_number>[.m=<modulation_format>][.<stream_type>[,<ISO_639_language_code>]{&<stream_type>[,<ISO_639_language_code>]}][;<event_id>]{/<path_segments>}
 * ocap://f=<frequency>.<program_number>[.m=<modulation_format>][.<stream_type>[,<index>]{&<stream_type>[,<index>]}][;<event_id>]{/<path_segments>}
 * ocap://f=<frequency>.<program_number>[.m=<modulation_format>][.+<PID>{&<PID>}][;<event_id>]>]{/<path_segments>}
 * ocap://f=<frequency>.<program_number>[.m=<modulation_format>][.@<component_tag>{&<component_tag>}][;<event_id>]{/<path_segments>}
 * ocap://f=<frequency>.<program_number>[.m=<modulation_format>][.$<component_name>{&<component_name>}][;<event_id>]{/<path_segments>}
 * ocap://oobfdc.<program_number>[.<stream_type>[,<ISO_639_language_code>]{&<stream_type>[,<ISO_639_language_code>]}][;<event_id>]{/<path_segments>}
 * ocap://oobfdc.<program_number>[.<stream_type>[,<index>]{&<stream_type>[,<index>]}][;<event_id>]{/<path_segments>}
 * ocap://oobfdc.<program_number>[.+<PID>{&<PID>}][;<event_id>]{/<path_segments>}
 * ocap://oobfdc.<program_number>[.@<component_tag>{&<component_tag>}][;<event_id>]{/<path_segments>}
 * ocap://oobfdc.<program_number>[.$<component_name>{&<component_name>}][;<event_id>]{/<path_segments>}
 * ocap://frequency[.m=modulation_format]
 * ocap:/<path_segments>
 * </pre>
 * 
 * Some examples:
 * <ul>
 * <li>ocap://0x1234 The service for source_id 0x1234
 * <li>ocap://0x1234.0x2 The first ISO/IEC 13818-2 [27] video stream for
 * source_id 0x1234
 * <li>ocap://0x1234.0x81 The first ATSC A/53C [2] audio stream for source_id
 * 0x1234
 * <li>ocap://n=MOVIE The service with source_name or short_name MOVIE
 * <li>ocap://n=MOVIE.0x81,spa Spanish language ATSC A/53C [2] audio track for
 * the service with source_name or short_name MOVIE
 * <li>ocap://0x1234/<path>/<filename> The file of <filename> in the DSM-CC
 * object carousel in the service for source_id 0x1234.
 * <li>ocap://0x1234.+0x56&0x78;0xBC Two program elements that has PID=0x56 (for
 * video) and 0x78 (for audio) in the service for source_id 0x1234. It is also
 * restricted by event_id 0xBC.
 * </ul>
 * 
 * Stream types:
 * <ul>
 * <li>0x01 VideoMPEG-1 ISO/IEC 11172-2 Video
 * <li>0x02 VideoH.262 ITU-T Rec. H.262 ISO/IEC 13818-2 Video or ISO/IEC 11172-2
 * constrained parameter video stream
 * <li>0x03 AudioMPEG-1 ISO/IEC 11172-3 Audio
 * <li>0x04 AudioMPEG-2 ISO/IEC 13818-3 Audio
 * <li>0x05 MPEG-PrivateSection ITU-T Rec. H.222.0 ISO/IEC 13818-1
 * private-sections
 * <li>0x06 1 MPEG-PrivateData ITU-T Rec. H.222.0 ISO/IEC 13818-1 PES packets
 * containing private data
 * <li>0x07 MHEG ISO/IEC 13522 MHEG
 * <li>0x08 1 DSM-CC Annex A - DSM CC
 * <li>0x09 H.222 ITU-T Rec. H.222.1
 * <li>0x0A DSMCC-MPE ISO/IEC 13818-6 type A (Multi-protocol Encapsulation)
 * <li>0x0B DSMCC-UN ISO/IEC 13818-6 type B (DSM-CC U-N Messages)
 * <li>0x0C DSMCC-StreamDescriptors ISO/IEC 13818-6 type C (DSM-CC Stream
 * Descriptors)
 * <li>0x0D DSMCC-Sections ISO/IEC 13818-6 type D (DSM-CC Sections any type,
 * including private data)
 * <li>0x0E Auxiliary ISO/IEC 13818-1 auxiliary
 * <li>0x80 VideoDCII DigiCipher II video3
 * <li>0x81 AudioATSC ATSC A/53 audio (ATSC Standard A/53, 1995, ATSC Digital
 * Television Standard)
 * <li>0x83 IsochronousData Isochronous data (Data Service Extensions for MPEG-2
 * Transport, STD-096- 011, Rev. 1.0,General Instrument)
 * <li>0x84 AsynchronousData Asynchronous data (Data Service Extensions for
 * MPEG-2 Transport, STD-096- 011, Rev. 1.0,General Instrument)
 * <li>3 Video Matches the video type present in program stream, either:
 * VideoMPEG-1, VideoH.262, or VideoDCII
 * <li>3 Audio Matches the audio type present in program stream, either:
 * AudioMPEG-1, AudioMPEG-2, or AudioATSC
 * <li>4 CC The EIA-608 Closed Caption stream that may be present in either
 * analog or digital channels
 * <li>4 DTVCC The DTV Closed Caption stream that may be present in digital
 * channels
 * </ul>
 */
public class OcapLocatorTest extends LocatorTest
{
    /**
     * Quick self-test to make sure we are doing things correctly before we go
     * on with our test.
     * 
     * <ul>
     * <li>TestParam generation of locator string, complete with escaping.
     * </ul>
     */
    public void testSelf() throws Exception
    {
        TestParam param;

        // TestParam w/ escaped path
        param = new TestParam("/alphanum;param/mark:@&=+$,-_.!~*'()/ \t`");
        assertEquals("Path not escaped as expected", "ocap:/alphanum;param/mark:@&=+$,-_.!~*'()/%20%09%60",
                param.getLocatorString());

        // TestParam w/ escaped service_name
        param = new TestParam(-1, "alphanum-_!~*'() \t`.", -1, -1, -1, null, null, null, -1, null, null, null, null);
        assertEquals("Service name not escaped as expected", "ocap://n=alphanum-_!~*'()%20%09%60%2e",
                param.getLocatorString());

        // TestParam w/ escaped component_name
        param = new TestParam(-1, "HBO", -1, -1, -1, null, null, null, -1, null, new String[] { "alphanum-_!~*'()",
                " \t`&" }, null, null);
        assertEquals("Component name not escaped as expected", "ocap://n=HBO.$alphanum-_!~*'()&%20%09%60%26",
                param.getLocatorString());

        // TestParam w/ multibyte character service_name
        String string = new String(new char[] { 0x5d0 });
        param = new TestParam(-1, string, -1, -1, -1, null, null, null, -1, null, null, null, null);
        assertEquals("Service name not escaped as expected", "ocap://n=%d7%90", param.getLocatorString());

        // TestParam w/ multibyte character service_name
        string = new String(new char[] { 0xc0, 'B', 'C' });
        param = new TestParam(-1, string, -1, -1, -1, null, null, null, -1, null, null, null, null);
        assertEquals("Service name not escaped as expected", "ocap://n=%c3%80BC", param.getLocatorString());

        // TestParam w/ multibyte character service_name
        string = new String(new char[] { 0x1d00, 'B', 'C' });
        param = new TestParam(-1, string, -1, -1, -1, null, null, null, -1, null, null, null, null);
        assertEquals("Service name not escaped as expected", "ocap://n=%e1%b4%80BC", param.getLocatorString());

        // Add more tests for TestParam...
    }

    /**
     * Tests the sourceID constructor.
     */
    public void testConstructorSourceID() throws Exception
    {
        OcapLocator loc;

        loc = new OcapLocator(0x1234);
        checkConstructor(loc, "ocap://0x1234", null, 0x1234, -1, -1, -1, null, null, null, null, null, -1, null, null);

        // MSO Abstract service
        loc = new OcapLocator(0xFFFFFF);
        // Assume lower-case as OCAP doesn't specify)
        checkConstructor(loc, "ocap://0xffffff", null, 0xFFFFFF, -1, -1, -1, null, null, null, null, null, -1, null,
                null);

        // host Abstract service
        loc = new OcapLocator(0x01FFFF);
        // Assume lower-case as OCAP doesn't specify)
        checkConstructor(loc, "ocap://0x1ffff", null, 0x01FFFF, -1, -1, -1, null, null, null, null, null, -1, null,
                null);

        try
        {
            new OcapLocator(0x12345678);
            fail("Out of range source_id should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the freq/prog constructor.
     */
    public void testConstructorFreqProg() throws Exception
    {
        OcapLocator loc;

        loc = new OcapLocator(0x1234, 0x27, -1);
        checkConstructor(loc, "ocap://f=0x1234.0x27", null, -1, 0x1234, 0x27, -1, null, null, null, null, null, -1,
                null, null);

        try
        {
            new OcapLocator(0x12345678, 0x12345678, -1);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the freq/prog/modulation constructor.
     */
    public void testConstructorFreqProgModulation() throws Exception
    {
        OcapLocator loc;

        // check all accepted QAM modes
        for (int qm = 0; qm <= 31; qm++)
        {
            loc = new OcapLocator(0x1234, 0x27, qm);
            checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x" + Integer.toHexString(qm), null, -1, 0x1234, 0x27, qm,
                    null, null, null, null, null, -1, null, null);
        }
        loc = new OcapLocator(0x1234, 0x27, -1);
        checkConstructor(loc, "ocap://f=0x1234.0x27", null, -1, 0x1234, 0x27, -1, null, null, null, null, null, -1,
                null, null);
        loc = new OcapLocator(0x1234, 0x27, 0);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x0", null, -1, 0x1234, 0x27, 0, null, null, null, null, null,
                -1, null, null);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, -1);
        checkConstructor(loc, "ocap://oobfdc.0x27", null, -1, -1, 0x27, -1, null, null, null, null, null, -1, null,
                null);
        loc = new OcapLocator(-1, 0x27, 0);
        checkConstructor(loc, "ocap://oobfdc.0x27", null, -1, -1, 0x27, -1, null, null, null, null, null, -1, null,
                null);
        loc = new OcapLocator(-1, 0x27, 1000);
        checkConstructor(loc, "ocap://oobfdc.0x27", null, -1, -1, 0x27, -1, null, null, null, null, null, -1, null,
                null);

        // 0xFF is used to indicate NTSC analog mode
        loc = new OcapLocator(0x1234, 0x27, 0xff);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0xff", null, -1, 0x1234, 0x27, 0xff, null, null, null, null,
                null, -1, null, null);

        // try some invalid modes
        try
        {
            new OcapLocator(0x12345678, 0x12345678, 0x12);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, -2);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the freq/modulation constructor.
     */
    public void testConstructorFreqModulation() throws Exception
    {
        OcapLocator loc;

        // check all accepted QAM modes
        for (int qm = 0; qm <= 31; qm++)
        {
            loc = new OcapLocator(0x1234, qm);
            checkConstructor(loc, "ocap://f=0x1234.m=0x" + Integer.toHexString(qm), null, -1, 0x1234, -1, qm, null,
                    null, null, null, null, -1, null, null);
        }
        loc = new OcapLocator(0x1234, -1);
        checkConstructor(loc, "ocap://f=0x1234", null, -1, 0x1234, -1, -1, null, null, null, null, null, -1, null, null);
        loc = new OcapLocator(0x1234, 0);
        checkConstructor(loc, "ocap://f=0x1234.m=0x0", null, -1, 0x1234, -1, 0, null, null, null, null, null, -1, null,
                null);

        // 0xFF is used to indicate NTSC analog mode
        loc = new OcapLocator(0x1234, 0xff);
        checkConstructor(loc, "ocap://f=0x1234.m=0xff", null, -1, 0x1234, -1, 0xff, null, null, null, null, null, -1,
                null, null);

        // try some invalid modes
        try
        {
            new OcapLocator(0x12345678, 0x20);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, -2);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int sourceID, int[] PID, int eventID, String path)
     * constructor.
     */
    public void testConstructorPID() throws Exception
    {
        OcapLocator loc;
        int[] pid = { 1, 3, 6 };
        int event = 13;
        // String path = "/path/to/dir";
        String path = PATH_NOT_TOOLONG;

        loc = new OcapLocator(0x1234, pid, event, path);
        checkConstructor(loc, "ocap://0x1234.+0x1&0x3&0x6;0xd/" + path, null, 0x1234, -1, -1, -1, null, pid, null,
                null, null, event, null, path);

        // MSO Abstract service
        loc = new OcapLocator(0x123456, pid, event, path);
        checkConstructor(loc, "ocap://0x123456.+0x1&0x3&0x6;0xd/" + path, null, 0x123456, -1, -1, -1, null, pid, null,
                null, null, event, null, path);

        // Host Abstract service
        loc = new OcapLocator(0x12345, pid, event, path);
        checkConstructor(loc, "ocap://0x12345.+0x1&0x3&0x6;0xd/" + path, null, 0x12345, -1, -1, -1, null, pid, null,
                null, null, event, null, path);

        try
        {
            new OcapLocator(0x12345678, pid, event, path);
            fail("Out of range source_id should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, pid, event, "//bad/path");
            fail("Invalid path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, pid, event, PATH_TOOLONG);
            fail("Invalid path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, pid, event, PATH_NULL);
            fail("Invalid path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, (int[]) null, event, path);
            fail("A null PID array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            // pids are 13 bits
            new OcapLocator(0x1234, new int[] { 1, 0x2001, 6 }, event, path);
            fail("Out of range pid should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            // events are 14 bits
            new OcapLocator(0x1234, pid, 0x7001, path);
            fail("Out of range event should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int sourceID, short[] streamType, int[] index, int eventID,
     * String path) constructor.
     */
    public void testConstructorIndex() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x7, 0x6, 0xe };
        int[] index = { -1, 0x1, 0x2 };
        int[] noindex = {};
        int event = 0x13;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, streamType, index, event, path);
        checkConstructor(loc, "ocap://0x1234.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, 0x1234, -1, -1, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(0x1234, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://0x1234.0x7&0x6&0xe;0x13/path/to/dir", null, 0x1234, -1, -1, -1, streamType, null,
                noindex, null, null, event, null, path);

        // MSO Abstract service
        loc = new OcapLocator(0x123456, streamType, index, event, path);
        checkConstructor(loc, "ocap://0x123456.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, 0x123456, -1, -1, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(0x123456, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://0x123456.0x7&0x6&0xe;0x13/path/to/dir", null, 0x123456, -1, -1, -1, streamType,
                null, noindex, null, null, event, null, path);

        // Host Abstract service
        loc = new OcapLocator(0x12345, streamType, index, event, path);
        checkConstructor(loc, "ocap://0x12345.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, 0x12345, -1, -1, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(0x12345, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://0x12345.0x7&0x6&0xe;0x13/path/to/dir", null, 0x12345, -1, -1, -1, streamType,
                null, noindex, null, null, event, null, path);

        // Test for invalidFormat:
        // Out-of-range sourceID
        try
        {
            new OcapLocator(0x1234567, streamType, index, event, path);
            fail("Out of range sourceID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, new short[] { 0x1 }, index, event, path);
            fail("More indices than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, new int[] { 0x1 }, event, path);
            fail("More streamTypes than indices should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, null, index, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, (int[]) null, event, path);
            fail("A null index array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, index, event, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, index, event, PATH_NULL);
            fail("Embedded nulls in path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, new int[] { 1, -2, 2 }, event, path);
            fail("Out-of-range index should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, new short[] { 0xe, 0x101, -1 }, index, event, path);
            fail("Out-of-range streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int sourceID, short[] streamType, String[] ISO639LangCode, int
     * eventID, String path) constructor.
     */
    public void testConstructorLanguageCode() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x9, 0x1, 0x84, 0x5 };
        String[] lang = { "spa", null, "eng", "cpe" };
        String[] nolang = {};
        int event = 0x1;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, streamType, lang, event, path);
        checkConstructor(loc, "ocap://0x1234.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, 0x1234, -1, -1, -1,
                streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(0x1234, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://0x1234.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, 0x1234, -1, -1, -1, streamType,
                null, null, nolang, null, event, null, path);

        // MSO Abstract service
        loc = new OcapLocator(0x123456, streamType, lang, event, path);
        checkConstructor(loc, "ocap://0x123456.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, 0x123456, -1, -1,
                -1, streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(0x123456, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://0x123456.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, 0x123456, -1, -1, -1,
                streamType, null, null, nolang, null, event, null, path);

        // Host Abstract service
        loc = new OcapLocator(0x12345, streamType, lang, event, path);
        checkConstructor(loc, "ocap://0x12345.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, 0x12345, -1, -1, -1,
                streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(0x12345, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://0x12345.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, 0x12345, -1, -1, -1, streamType,
                null, null, nolang, null, event, null, path);

        // Test for invalidFormat:
        // Out-of-range sourceID
        try
        {
            new OcapLocator(0x1234567, streamType, lang, event, path);
            fail("Out of range sourceID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, new short[] { 0x1 }, lang, event, path);
            fail("More languageCodes than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, new String[] { "spa" }, event, path);
            fail("More streamTypes than langs should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, null, lang, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, (String[]) null, event, path);
            fail("A null langaugeCodes array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, lang, event, PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, lang, event, PATH_NULL);
            fail("Embedded null in path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, new String[] { "spa", "engl", null, "cpe" }, event, path);
            fail("Invalid language should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, streamType, lang, 0x4123, path);
            fail("Invalid eventID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, new short[] { 0x1, 0x2, 0x101, -1 }, lang, event, path);
            fail("Invalid streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int sourceID, String[] component, int eventID, String path)
     * constructor.
     */
    public void testConstructorComponent() throws Exception
    {
        OcapLocator loc;
        String[] comp = { "a", "b", "c", "\u00d8" };
        int event = 0xd;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, comp, event, path);
        checkConstructor(loc, "ocap://0x1234.$a&b&c&%c3%98;0xd/path/to/dir", null, 0x1234, -1, -1, -1, null, null,
                null, null, comp, event, null, path);

        // MSO Abstract Service
        loc = new OcapLocator(0x123456, comp, event, path);
        checkConstructor(loc, "ocap://0x123456.$a&b&c&%c3%98;0xd/path/to/dir", null, 0x123456, -1, -1, -1, null, null,
                null, null, comp, event, null, path);

        // Host Abstract Service
        loc = new OcapLocator(0x12345, comp, event, path);
        checkConstructor(loc, "ocap://0x12345.$a&b&c&%c3%98;0xd/path/to/dir", null, 0x12345, -1, -1, -1, null, null,
                null, null, comp, event, null, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator(0xFF000000, comp, event, path);
            fail("Out of range sourceID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, (String[]) null, event, path);
            fail("A null components array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, comp, event, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, comp, event, PATH_NULL);
            fail("Embedded null in  path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (String serviceName, short streamType[], String
     * ISO639LanguageCode[], int eventID, String pathSegments) constructor.
     */
    public void testConstructorServiceNameLanguageCode() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x9, 0x1, 0x84, 0x5 };
        String[] lang = { "spa", "eng", null, "cpe" };
        String[] nolang = {};
        int event = 0x17;
        String path = "path/to/dir";

        // with langs
        loc = new OcapLocator("HBO", streamType, lang, event, path);
        checkConstructor(loc, "ocap://n=HBO.0x9,spa&0x1,eng&0x84&0x5,cpe;0x17/path/to/dir", "HBO", -1, -1, -1, -1,
                streamType, null, null, lang, null, event, null, path);
        // without langs
        loc = new OcapLocator("HBO", streamType, nolang, event, path);
        checkConstructor(loc, "ocap://n=HBO.0x9&0x1&0x84&0x5;0x17/path/to/dir", "HBO", -1, -1, -1, -1, streamType,
                null, null, nolang, null, event, null, path);

        // with langs and serviceName with multi-byte characters
        loc = new OcapLocator("Galavisión", streamType, lang, event, path);
        checkConstructor(loc, "ocap://n=Galavisi%c3%b3n.0x9,spa&0x1,eng&0x84&0x5,cpe;0x17/path/to/dir", "Galavisión",
                -1, -1, -1, -1, streamType, null, null, lang, null, event, null, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator("HBO", new short[] { 0x1 }, lang, event, path);
            fail("More languageCodes than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", streamType, new String[] { "spa" }, event, path);
            fail("More streamTypes than langs should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", null, lang, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", streamType, (String[]) null, event, path);
            fail("A null langaugeCodes array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", streamType, lang, event, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", streamType, lang, event, PATH_NULL);
            fail("A path with embedded null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", streamType, new String[] { "spa", "english", null, "cpe" }, event, path);
            fail("Invalid language code should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("HBO", new short[] { 0x1, 0x101, -1, 0x2 }, lang, event, path);
            fail("Invalid streamType code should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int sourceID, int eventID, int[] componentTags, String
     * pathSegments) constructor.
     */
    public void testConstructorSourceIDComponentTags() throws Exception
    {
        OcapLocator loc;
        int[] comp = { 10, 20, 30 };
        int event = 0xd;
        String path = "path/to/dir";

        // all components
        loc = new OcapLocator(0x1234, event, comp, path);
        checkConstructor(loc, "ocap://0x1234.@0xa&0x14&0x1e;0xd/path/to/dir", null, 0x1234, -1, -1, -1, null, null,
                null, null, null, event, comp, path);

        // no event id
        loc = new OcapLocator(0x1234, -1, comp, path);
        checkConstructor(loc, "ocap://0x1234.@0xa&0x14&0x1e/path/to/dir", null, 0x1234, -1, -1, -1, null, null, null,
                null, null, -1, comp, path);

        // no component tags
        loc = new OcapLocator(0x1234, event, new int[0], path);
        checkConstructor(loc, "ocap://0x1234;0xd/path/to/dir", null, 0x1234, -1, -1, -1, null, null, null, null, null,
                event, new int[0], path);

        // no path
        loc = new OcapLocator(0x1234, event, comp, null);
        checkConstructor(loc, "ocap://0x1234.@0xa&0x14&0x1e;0xd", null, 0x1234, -1, -1, -1, null, null, null, null,
                null, event, comp, null);

        // Test for invalidFormat;
        // Out-of-range sourceID
        try
        {
            new OcapLocator(0x1234567, event, comp, path);
            fail("Out of range sourceID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, event, (int[]) null, path);
            fail("A null components array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, event, comp, PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, event, comp, PATH_NULL);
            fail("Embedded nulls in path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (String serviceName, short streamType[], int index[], int
     * eventID, String pathSegments) constructor.
     */
    public void testConstructorServiceNameIndex() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x7, 0x6, 0xe };
        int[] index = { 0x1, -1, 0x2 };
        int[] noindex = {};
        int event = 0x13;
        String path = "path/to/dir";

        loc = new OcapLocator("TBS", streamType, index, event, path);
        checkConstructor(loc, "ocap://n=TBS.0x7,0x1&0x6&0xe,0x2;0x13/path/to/dir", "TBS", -1, -1, -1, -1, streamType,
                null, index, null, null, event, null, path);
        loc = new OcapLocator("TBS", streamType, noindex, event, path);
        checkConstructor(loc, "ocap://n=TBS.0x7&0x6&0xe;0x13/path/to/dir", "TBS", -1, -1, -1, -1, streamType, null,
                noindex, null, null, event, null, path);

        // check for proper escaping
        loc = new OcapLocator("\u00c0BC", streamType, index, event, path);
        checkConstructor(loc, "ocap://n=%c3%80BC.0x7,0x1&0x6&0xe,0x2;0x13/path/to/dir", "\u00c0BC", -1, -1, -1, -1,
                streamType, null, index, null, null, event, null, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator("TBS", new short[] { 0x1 }, index, event, path);
            fail("More indices than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", streamType, new int[] { 0x1 }, event, path);
            fail("More streamTypes than indices should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", null, index, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", streamType, (int[]) null, event, path);
            fail("A null index array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            // Obviously cannot have an index > maximum size of PMT!
            new OcapLocator("TBS", streamType, new int[] { 1, -2, 2 }, event, path);
            fail("An invalid index should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", streamType, index, event, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", streamType, index, event, PATH_NULL);
            fail("A path with null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("TBS", new short[] { 0x1, 0x100, -0x2 }, index, event, path);
            fail("Out-of-range streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (String serviceName, int PID[], int eventID, String
     * pathSegments) constructor.
     */
    public void testConstructorServiceNamePID() throws Exception
    {
        OcapLocator loc;
        int[] pid = { 1, 3, 6 };
        int event = 13;
        String path = "path/to/dir";

        loc = new OcapLocator("CableNewsNetwork", pid, event, path);
        checkConstructor(loc, "ocap://n=CableNewsNetwork.+0x1&0x3&0x6;0xd/path/to/dir", "CableNewsNetwork", -1, -1, -1,
                -1, null, pid, null, null, null, event, null, path);

        // check for proper escaping
        loc = new OcapLocator("\u00c0BC", pid, event, path);
        checkConstructor(loc, "ocap://n=%c3%80BC.+0x1&0x3&0x6;0xd/path/to/dir", "\u00c0BC", -1, -1, -1, -1, null, pid,
                null, null, null, event, null, path);

        try
        {
            new OcapLocator("CNN", pid, event, "//bad/path");
            fail("Invalid path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("CableNewsNetwork", (int[]) null, event, path);
            fail("A null PID array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("CNN", new int[] { 1, 0x2001, 6 }, event, path);
            fail("Out of range pid should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("CNN", pid, 0x7001, path);
            fail("Out of range event should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("CNN", pid, event, PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("CNN", pid, event, PATH_NULL);
            fail("Path with embedded nulls should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (String serviceName, String componentName[], int eventID,
     * String pathSegments) constructor.
     */
    public void testConstructorServiceNameComponent() throws Exception
    {
        OcapLocator loc;
        String[] comp = { "a", "b", "c", "\u00d8" };
        int event = 0xd;
        String path = "path/to/dir";

        loc = new OcapLocator("WB", comp, event, path);
        checkConstructor(loc, "ocap://n=WB.$a&b&c&%c3%98;0xd/path/to/dir", "WB", -1, -1, -1, -1, null, null, null,
                null, comp, event, null, path);

        // test for proper escaping
        loc = new OcapLocator("\u00c0BC", comp, event, path);
        checkConstructor(loc, "ocap://n=%c3%80BC.$a&b&c&%c3%98;0xd/path/to/dir", "\u00c0BC", -1, -1, -1, -1, null,
                null, null, null, comp, event, null, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator("WB", (String[]) null, event, path);
            fail("A null components array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("WB", comp, event, PATH_TOOLONG);
            fail("A too long path null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("WB", comp, event, PATH_NULL);
            fail("A path with embedded null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (String serviceName, int eventID, int[] componentTags, String
     * pathSegments) constructor.
     */
    public void testConstructorServiceNameComponentTags() throws Exception
    {
        OcapLocator loc;
        int[] comp = { 0x10, 0x20, 0x30 };
        int event = 0xd;
        String path = "path/to/dir";

        // all params
        loc = new OcapLocator("WB", event, comp, path);
        checkConstructor(loc, "ocap://n=WB.@0x10&0x20&0x30;0xd/path/to/dir", "WB", -1, -1, -1, -1, null, null, null,
                null, null, event, comp, path);

        // all params w/escaped characters
        loc = new OcapLocator("Galavisión", event, comp, path);
        checkConstructor(loc, "ocap://n=Galavisi%c3%b3n.@0x10&0x20&0x30;0xd/path/to/dir", "Galavisión", -1, -1, -1, -1,
                null, null, null, null, null, event, comp, path);

        // no event
        loc = new OcapLocator("WB", -1, comp, path);
        checkConstructor(loc, "ocap://n=WB.@0x10&0x20&0x30/path/to/dir", "WB", -1, -1, -1, -1, null, null, null, null,
                null, -1, comp, path);

        // no comp
        loc = new OcapLocator("WB", event, new int[0], path);
        checkConstructor(loc, "ocap://n=WB;0xd/path/to/dir", "WB", -1, -1, -1, -1, null, null, null, null, null, event,
                new int[0], path);

        // no path
        loc = new OcapLocator("WB", event, comp, null);
        checkConstructor(loc, "ocap://n=WB.@0x10&0x20&0x30;0xd", "WB", -1, -1, -1, -1, null, null, null, null, null,
                event, comp, null);

        // Test for invalidFormat:
        try
        {
            new OcapLocator("WB", event, (int[]) null, path);
            fail("A null components array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("WB", event, comp, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("WB", event, comp, PATH_NULL);
            fail("A path with null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int frequency, int programNumber, short streamType[], String
     * ISO639LanguageCode[], int eventID, String pathSegments) constructor.
     */
    public void testConstructorFreqProgramLanguage() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x9, 0x1, 0x84, 0x5 };
        String[] lang = { "spa", null, "eng", "cpe" };
        String[] nolang = {};
        int event = 0x1;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, 0x27, 0x10, streamType, lang, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, -1,
                0x1234, 0x27, 0x10, streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, 0x10, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, -1, 0x1234, 0x27,
                0x10, streamType, null, null, nolang, null, event, null, path);

        loc = new OcapLocator(0x1234, 0x27, -1, streamType, lang, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, -1, 0x1234,
                0x27, -1, streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, -1, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, -1, 0x1234, 0x27, -1,
                streamType, null, null, nolang, null, event, null, path);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, 0x10, streamType, lang, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, -1, -1, 0x27,
                -1, streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(-1, 0x27, 0x10, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, -1, -1, 0x27, -1,
                streamType, null, null, nolang, null, event, null, path);

        loc = new OcapLocator(-1, 0x27, -1, streamType, lang, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x9,spa&0x1&0x84,eng&0x5,cpe;0x1/path/to/dir", null, -1, -1, 0x27,
                -1, streamType, null, null, lang, null, event, null, path);
        loc = new OcapLocator(-1, 0x27, 100, streamType, nolang, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x9&0x1&0x84&0x5;0x1/path/to/dir", null, -1, -1, 0x27, -1,
                streamType, null, null, nolang, null, event, null, path);

        try
        {
            new OcapLocator(0x12345678, 0x12345678, 0x12, streamType, lang, event, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20, streamType, lang, event, path);
            fail("Out of range modulationFormat should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, new short[] { 0x1 }, lang, event, path);
            fail("More languageCodes than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, streamType, new String[] { "spa" }, event, path);
            fail("More streamTypes than langs should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, null, lang, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, streamType, (String[]) null, event, path);
            fail("A null languageCodes array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, streamType, lang, event, PATH_TOOLONG);
            fail("Path too long should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, streamType, lang, event, PATH_NULL);
            fail("Path with embedded null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, streamType, new String[] { "spa", "english", null, "cpe" }, event, path);
            fail("Invalid language code should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x12, 0x10, new short[] { 0x1, 0x7F00, 0x2 }, lang, event, path);
            fail("Invalid streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int frequency, int programNumber, int modulationFormat, short
     * streamType[], int index[], int eventID, String pathSegments) constructor.
     */
    public void testConstructorFreqProgramIndex() throws Exception
    {
        OcapLocator loc;
        short[] streamType = { 0x7, 0x6, 0xe };
        int[] index = { -1, 0x1, 0x2 };
        int[] noindex = {};
        int event = 0x13;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, 0x27, 0x10, streamType, index, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, -1, 0x1234,
                0x27, 0x10, streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, 0x10, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.0x7&0x6&0xe;0x13/path/to/dir", null, -1, 0x1234, 0x27, 0x10,
                streamType, null, noindex, null, null, event, null, path);

        loc = new OcapLocator(0x1234, 0x27, -1, streamType, index, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, -1, 0x1234, 0x27, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, -1, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.0x7&0x6&0xe;0x13/path/to/dir", null, -1, 0x1234, 0x27, -1,
                streamType, null, noindex, null, null, event, null, path);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, 0x10, streamType, index, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, -1, -1, 0x27, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(-1, 0x27, 0x10, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x7&0x6&0xe;0x13/path/to/dir", null, -1, -1, 0x27, -1, streamType,
                null, noindex, null, null, event, null, path);

        loc = new OcapLocator(-1, 0x27, -1, streamType, index, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x7&0x6,0x1&0xe,0x2;0x13/path/to/dir", null, -1, -1, 0x27, -1,
                streamType, null, index, null, null, event, null, path);
        loc = new OcapLocator(-1, 0x27, 100, streamType, noindex, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.0x7&0x6&0xe;0x13/path/to/dir", null, -1, -1, 0x27, -1, streamType,
                null, noindex, null, null, event, null, path);

        // Test for invalidFormat:
        // Out-of-range sourceID
        try
        {
            new OcapLocator(0x12345678, 0x12345678, 0x12, streamType, index, event, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20, streamType, index, event, path);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, new short[] { 0x1 }, index, event, path);
            fail("More indices than streamTypes should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, streamType, new int[] { 0x1 }, event, path);
            fail("More streamTypes than indices should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, null, index, event, path);
            fail("A null streamType array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, streamType, (int[]) null, event, path);
            fail("A null index array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, streamType, index, event, PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, streamType, index, event, PATH_NULL);
            fail("Embedded nulls in path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, streamType, new int[] { 1, 2, -2 }, event, path);
            fail("Out of range index should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, new short[] { 0x1, 0x2, 0x4000 }, index, event, path);
            fail("Out of range streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int frequency, int programNumber, int modulationFormat, int
     * PID[], int eventID, String pathSegments) constructor.
     */
    public void testConstructorFreqProgramPID() throws Exception
    {
        OcapLocator loc;
        int[] pid = { 1, 3, 6 };
        int event = 13;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, 0x27, 0x10, pid, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.+0x1&0x3&0x6;0xd/path/to/dir", null, -1, 0x1234, 0x27, 0x10,
                null, pid, null, null, null, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, -1, pid, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.+0x1&0x3&0x6;0xd/path/to/dir", null, -1, 0x1234, 0x27, -1, null,
                pid, null, null, null, event, null, path);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, 0x10, pid, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.+0x1&0x3&0x6;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, pid,
                null, null, null, event, null, path);
        loc = new OcapLocator(-1, 0x27, 100, pid, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.+0x1&0x3&0x6;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, pid,
                null, null, null, event, null, path);

        try
        {
            new OcapLocator(0x12345678, 0x12345677, 0x12, pid, event, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20, pid, event, path);
            fail("Out of range modulation_format should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, pid, event, "//bad/path");
            fail("Invalid path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x1234, 0x27, 0x10, (int[]) null, event, path);
            fail("A null PID array should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x12, new int[] { 1, 0x2001, 6 }, event, path);
            fail("Out of range pid should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x12, pid, 0x7001, path);
            fail("Out of range event id should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int frequency, int programNumber, int modulationFormat, String
     * componentName[], int eventID, String pathSegments) constructor.
     */
    public void testConstructorFreqProgramComponentName() throws Exception
    {
        OcapLocator loc;
        String[] comp = { "a", "b", "c", "\u00d8" };
        int event = 0xd;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, 0x27, 0x10, comp, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.$a&b&c&%c3%98;0xd/path/to/dir", null, -1, 0x1234, 0x27,
                0x10, null, null, null, null, comp, event, null, path);
        loc = new OcapLocator(0x1234, 0x27, -1, comp, event, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.$a&b&c&%c3%98;0xd/path/to/dir", null, -1, 0x1234, 0x27, -1, null,
                null, null, null, comp, event, null, path);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, 0x10, comp, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.$a&b&c&%c3%98;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, null,
                null, null, comp, event, null, path);
        loc = new OcapLocator(-1, 0x27, -1, comp, event, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.$a&b&c&%c3%98;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, null,
                null, null, comp, event, null, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator(0x12345678, 0x12345678, 0x12, comp, event, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20, comp, event, path);
            fail("Out of range modulationFormat should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, (String[]) null, event, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, comp, event, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, comp, event, PATH_NULL);
            fail("A path with null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the (int frequency, int programNumber, int modulationFormat, int
     * eventID, int[] componentTags, String pathSegments) constructor.
     */
    public void testConstructorFreqProgramComponentTags() throws Exception
    {
        OcapLocator loc;
        int[] comp = { 0x10, 0x20, 0x30 };
        int event = 0xd;
        String path = "path/to/dir";

        loc = new OcapLocator(0x1234, 0x27, 0x10, event, comp, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.m=0x10.@0x10&0x20&0x30;0xd/path/to/dir", null, -1, 0x1234, 0x27,
                0x10, null, null, null, null, null, event, comp, path);
        loc = new OcapLocator(0x1234, 0x27, -1, event, comp, path);
        checkConstructor(loc, "ocap://f=0x1234.0x27.@0x10&0x20&0x30;0xd/path/to/dir", null, -1, 0x1234, 0x27, -1, null,
                null, null, null, null, event, comp, path);

        // oobfdc, qam is ignored
        loc = new OcapLocator(-1, 0x27, 0x10, event, comp, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.@0x10&0x20&0x30;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, null,
                null, null, null, event, comp, path);
        loc = new OcapLocator(-1, 0x27, -1, event, comp, path);
        checkConstructor(loc, "ocap://oobfdc.0x27.@0x10&0x20&0x30;0xd/path/to/dir", null, -1, -1, 0x27, -1, null, null,
                null, null, null, event, comp, path);

        // Test for invalidFormat:
        try
        {
            new OcapLocator(0x12345678, 0x12345678, 0x12, event, comp, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x1234, 0x20, event, comp, path);
            fail("Out of range modulationFormat should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, event, (int[]) null, path);
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, event, comp, PATH_TOOLONG);
            fail("A too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator(0x12345678, 0x12, 0x10, event, comp, PATH_NULL);
            fail("A path with null should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Tests the String constructor.
     */
    public void testConstructorString() throws Exception
    {
        OcapLocator loc;

        loc = new OcapLocator("ocap://0x1234");
        checkConstructor(loc, "ocap://0x1234", null, 0x1234, -1, -1, -1, null, null, null, null, null, -1, null, null);

        loc = new OcapLocator("ocap://0xFFFFFF");
        checkConstructor(loc, "ocap://0xFFFFFF", null, 0xFFFFFF, -1, -1, -1, null, null, null, null, null, -1, null,
                null);

        loc = new OcapLocator("ocap://f=0x1234.0x2.m=0x10");
        checkConstructor(loc, "ocap://f=0x1234.0x2.m=0x10", null, -1, 0x1234, 0x2, 0x10, null, null, null, null, null,
                -1, null, null);

        loc = new OcapLocator("ocap://f=0x1234.0x2");
        checkConstructor(loc, "ocap://f=0x1234.0x2", null, -1, 0x1234, 0x2, -1, null, null, null, null, null, -1, null,
                null);

        loc = new OcapLocator("ocap://f=0x1234.m=0x10");
        checkConstructor(loc, "ocap://f=0x1234.m=0x10", null, -1, 0x1234, -1, 0x10, null, null, null, null, null, -1,
                null, null);

        loc = new OcapLocator("ocap://f=0x1234");
        checkConstructor(loc, "ocap://f=0x1234", null, -1, 0x1234, -1, -1, null, null, null, null, null, -1, null, null);

        loc = new OcapLocator("ocap://oobfdc.0x2");
        checkConstructor(loc, "ocap://oobfdc.0x2", null, -1, -1, 0x2, -1, null, null, null, null, null, -1, null, null);

        loc = new OcapLocator("ocap://n=HBO/application/directory");
        checkConstructor(loc, "ocap://n=HBO/application/directory", "HBO", -1, -1, -1, -1, null, null, null, null,
                null, -1, null, "application/directory");
        loc = new OcapLocator("ocap://n=%c3%80BC/application/directory");
        checkConstructor(loc, "ocap://n=%c3%80BC/application/directory", "\u00c0BC", -1, -1, -1, -1, null, null, null,
                null, null, -1, null, "application/directory");

        loc = new OcapLocator("ocap://n=%e1%b4%80BC/application/directory");
        checkConstructor(loc, "ocap://n=%e1%b4%80BC/application/directory", "\u1d00BC", -1, -1, -1, -1, null, null,
                null, null, null, -1, null, "application/directory");

        loc = new OcapLocator("ocap://n=%e1%b4%80BC.$%c3%98/application/directory");
        checkConstructor(loc, "ocap://n=%e1%b4%80BC.$%c3%98/application/directory", "\u1d00BC", -1, -1, -1, -1, null,
                null, null, null, new String[] { "\u00d8" }, -1, null, "application/directory");

        // Test some bad locators
        try
        {
            new OcapLocator("ocap://0x1234567");
            fail("Out of range serviceId should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://oobfdc.24");
            fail("Incorrect syntax should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://oobfdc.");
            fail("Incorrect syntax should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap:///blah");
            fail("Incorrect syntax should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap:blah");
            fail("Incorrect syntax should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://HBO");
            fail("Incorrect syntax should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("dvb://0x1234");
            fail("A DVB locator should be an invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("http://www.cablelabs.org");
            fail("An HTTP locator should be an invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://f=0x12345678.0x12345678");
            fail("Out of range program should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://f=0x12345678.0x1234.m=0x20");
            fail("Out of range modulationFormat should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0x12345678");
            fail("Out of range source_id should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://f=0x12345678.m=0x10.+0x56&0x78;0xBC");
            fail("TransportLocator should not accept service components");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0x1234.+0x56&0x2000");
            fail("Out of range PID should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0x1234.+0x56&0x200;0x4000");
            fail("Out of range event id should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://n=MOVIE.0x81,0xFFFFFFFE");
            fail("Out of range index should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://n=MOVIE.0x100");
            fail("Out of range streamType should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap:" + PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0xabcd" + PATH_TOOLONG);
            fail("Too long path should result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Test the (String url) constructor; make sure it doesn't allow incorrect
     * mixing of streamType/Language and streamType/index; PID and component.
     */
    public void testConstructorBadMix() throws Exception
    {
        try
        {
            new OcapLocator("ocap://0x1234.0x12.m=0x10,spa&0x13,0x14");
            fail("Mixed streamType/language and streamType/index should " + "result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0x1234.0x13.m=0x10,0x14&0x12,spa");
            fail("Mixed streamType/index and streamType/language should " + "result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
        try
        {
            new OcapLocator("ocap://0x1234.+0x10&vid");
            fail("Mixed PID and componentNames should " + "result in invalid locator");
        }
        catch (InvalidLocatorException e)
        { /* expected */
        }
    }

    /**
     * Overrides LocatorTest.createLocator(). This is used by the superclass
     * testcases in org.davic.net.LocatorTest.
     */
    protected Locator createLocator(String url) throws Exception
    {
        return new OcapLocator(url);
    }

    /**
     * Common routine for verifying a constructor.
     */
    private void checkConstructor(OcapLocator loc, String url, String serviceName, int sourceID, int freq, int program,
            int mod, short[] streamType, int[] PID, int[] index, String[] isoLang, String[] comp, int eventID,
            int[] componentTags, String path)
    {
        if (serviceName != null)
            assertEquals(url + " Incorrect value for serviceName returned on constructor", serviceName,
                    loc.getServiceName());
        assertEquals(url + " Incorrect value for sourceID on constructor", sourceID, loc.getSourceID());
        assertEquals(url + " Incorrect value for frequencey on constructor", freq, loc.getFrequency());
        assertEquals(url + " Incorrect value for program on constructor", program, loc.getProgramNumber());
        assertEquals(url + " Incorrect value for modulationFormat on constructor", mod, loc.getModulationFormat());
        assertArrayEquals(url + " streamTypes:", streamType, loc.getStreamTypes());
        assertArrayEquals(url + " PIDs:", PID, loc.getPIDs());
        // assertArrayEquals(url+" indices:", index, loc.getIndices());
        assertArrayEquals(url + " isoLang:", isoLang, loc.getLanguageCodes());
        assertArrayEquals(url + " components:", comp, loc.getComponentNames());
        assertEquals(url + " Incorrect value for eventID on constructor", eventID, loc.getEventId());
        assertArrayEquals(url + " componentTags: ", componentTags, loc.getComponentTags());
        assertEquals(url + " Incorrect value for path on constructor", path, loc.getPathSegments());
        assertEquals(url + " Incorrect value for toExternalForm returned on constructor", url, loc.toExternalForm());
    }

    /**
     * Set of supported streamType value codes. Entries match up to their
     * associated code string in the STREAM_TYPE_CODES array.
     */
    private static final short STREAM_TYPE_VALUES[] = { StreamType.MPEG_1_VIDEO, StreamType.MPEG_2_VIDEO,
            StreamType.MPEG_1_AUDIO, StreamType.MPEG_2_AUDIO, StreamType.MPEG_PRIVATE_SECTION,
            StreamType.MPEG_PRIVATE_DATA, StreamType.MHEG, StreamType.DSM_CC, StreamType.H_222, StreamType.DSM_CC_MPE,
            StreamType.DSM_CC_UN, StreamType.DSM_CC_STREAM_DESCRIPTORS, StreamType.DSM_CC_SECTIONS,
            StreamType.AUXILIARY, StreamType.VIDEO_DCII, StreamType.ATSC_AUDIO, StreamType.STD_SUBTITLE,
            StreamType.ISOCHRONOUS_DATA, StreamType.ASYNCHRONOUS_DATA, };

    /**
     * Asserts that the two arrays are equal.
     */
    private static void assertArrayEquals(String name, String[] a1, String[] a2)
    {
        assertNotNull(name + " should not be null", a2);
        assertEquals(name + " unexpected length", ((a1 == null) ? 0 : a1.length), a2.length);
        if (a1 != null) for (int i = 0; i < a1.length; ++i)
            if (a1[i] == null)
                assertSame(name + " array element should be null [" + i + "]", a1[i], a2[i]);
            else
                assertEquals(name + " incorrect array element value [" + i + "]", a1[i], a2[i]);
    }

    /**
     * Asserts that the two arrays are equal.
     */
    private static void assertArrayEquals(String name, int[] a1, int[] a2)
    {
        assertNotNull(name + " should not be null", a2);
        assertEquals(name + " unexpected length", ((a1 == null) ? 0 : a1.length), a2.length);
        if (a1 != null) for (int i = 0; i < a1.length; ++i)
            assertEquals(name + " incorrect array element value [" + i + "]", a1[i], a2[i]);
    }

    /**
     * Asserts that the two arrays are equal.
     */
    private static void assertArrayEquals(String name, short[] a1, short[] a2)
    {
        assertNotNull(name + " should not be null", a2);
        assertEquals(name + " unexpected length", ((a1 == null) ? 0 : a1.length), a2.length);
        if (a1 != null) for (int i = 0; i < a1.length; ++i)
            assertEquals(name + " incorrect array element value [" + i + "]", a1[i], a2[i]);
    }

    /**
     * A parameterized testcase. The parameter is a TestParam object that holds
     * information that should be contained in an OcapLocator. The TestParam
     * object is capable of generating a locator string based on that
     * information. That string is used to generate an OcapLocator (see
     * {@link #setUp}). Each of the test routines verify that the values
     * returned by the OcapLocator match the expected values embedded in the
     * TestParam.
     * <p>
     * This class extends InterfaceTestCase, but not because it tests an
     * interface. Instead it makes use of the factory support to provide for
     * parameterized test cases.
     */
    public static class SubTest extends InterfaceTestCase
    {
        public static InterfaceTestSuite isuite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(SubTest.class);
            suite.setName("Parameterized");
            return suite;
        }

        public SubTest(String name, ImplFactory f)
        {
            super(name, TestParam.class, f);
        }

        protected TestParam getTestParam()
        {
            return (TestParam) createImplObject();
        }

        protected TestParam test;

        protected String str;

        protected OcapLocator loc;

        protected void setUp() throws Exception
        {
            super.setUp();
            test = getTestParam();
            str = test.getLocatorString();
            loc = new OcapLocator(str);
        }

        protected void tearDown() throws Exception
        {
            test = null;
            str = null;
            loc = null;
            super.tearDown();
        }

        /**
         * Tests getLocator().
         */
        public void testToExternalForm() throws Exception
        {
            assertEquals("Incorrect locator value", str, loc.toExternalForm());
        }

        /**
         * Tests getSourceID().
         */
        public void testGetSourceID() throws Exception
        {
            assertEquals("Incorrect sourceID returned", test.sourceID, loc.getSourceID());
        }

        /**
         * Tests getServiceName().
         */
        public void testGetServiceName() throws Exception
        {
            assertEquals("Incorrect serviceName", test.serviceName, loc.getServiceName());
        }

        /**
         * Tests getShortName().
         */
        // to be removed
        /*
         * public void testGetShortName() throws Exception {
         * assertEquals("Incorrect shortName", test.shortName,
         * loc.getShortName()); }
         */

        /**
         * Tests getFrequency().
         */
        public void testGetFrequency() throws Exception
        {
            assertEquals("Incorrect frequency", test.frequency, loc.getFrequency());
        }

        /**
         * Tests getProgramNumber().
         */
        public void testGetProgramNumber() throws Exception
        {
            assertEquals("Incorrect programNumber", test.programNumber, loc.getProgramNumber());
        }

        /**
         * Tests getProgramNumber().
         */
        public void testGetModulationFormat() throws Exception
        {
            assertEquals("Incorrect modulationFormat", test.modulationFormat, loc.getModulationFormat());
        }

        /**
         * Tests getStreamTypes().
         */
        public void testGetStreamTypes() throws Exception
        {
            short[] streamTypes = loc.getStreamTypes();

            assertNotNull("streamTypes array should not be null", streamTypes);
            if (test.streamTypes == null || test.streamTypes.length == 0)
                assertTrue("Expected no streamTypes (array of length 0)", streamTypes.length == 0);
            else
            {
                assertArrayEquals("getStreamTypes()", test.streamTypes, streamTypes);
            }
        }

        /**
         * Tests getIndexes().
         */
        public void testGetIndexes() throws Exception
        {
            int[] indexes = loc.getIndexes();

            assertNotNull("indexes array should not be null", indexes);
            if (test.indexes == null || test.indexes.length == 0)
                assertEquals("Expected no indexes (array of length 0)", 0, indexes.length);
            else
            {
                assertEquals("Expected same number of indexes as streamTypes", loc.getStreamTypes().length,
                        indexes.length);
                assertEquals("Unexpected number of indexes", test.indexes.length, indexes.length);
                for (int i = 0; i < indexes.length; ++i)
                {
                    if (test.indexes[i] == -1)
                        assertEquals("No stream type expected for [" + i + "]", test.indexes[i], indexes[i]);
                    else
                        assertEquals("Unexpected index", test.indexes[i], indexes[i]);
                }
            }
        }

        /**
         * Tests getLanguageCodes().
         */
        public void testGetLanguageCodes() throws Exception
        {
            String[] languageCodes = loc.getLanguageCodes();

            assertNotNull("languageCodes array should not be null", languageCodes);
            if (test.languageCodes == null || test.languageCodes.length == 0)
                assertEquals("Expected no languageCodes (array of length 0)", 0, languageCodes.length);
            else
            {
                assertEquals("Expected same number of languageCodes as streamTypes", loc.getStreamTypes().length,
                        languageCodes.length);
                assertEquals("Unexpected number of languageCodes", test.languageCodes.length, languageCodes.length);
                for (int i = 0; i < languageCodes.length; ++i)
                {
                    if (test.languageCodes[i] == null)
                        assertSame("No stream type expected for [" + i + "]", test.languageCodes[i], languageCodes[i]);
                    else
                        assertEquals("Unexpected languageCode", test.languageCodes[i], languageCodes[i]);
                }
            }
        }

        /**
         * Tests getEventId().
         */
        public void testGetEventId() throws Exception
        {
            assertEquals("Incorrect eventId", test.eventId, loc.getEventId());
        }

        /**
         * Tests getPIDs().
         */
        public void testGetPIDs() throws Exception
        {
            int[] pids = loc.getPIDs();

            assertNotNull("pids array should not be null", pids);
            if (test.pids == null || test.pids.length == 0)
                assertTrue("Expected no pids (array of length 0)", pids.length == 0);
            else
            {
                assertEquals("Unexpected number of pids", test.pids.length, pids.length);
                for (int i = 0; i < pids.length; ++i)
                {
                    assertEquals("Unexpected pid", test.pids[i], pids[i]);
                }
            }
        }

        /**
         * Tests getComponentNames().
         */
        public void testGetComponentNames() throws Exception
        {
            String[] componentNames = loc.getComponentNames();

            assertNotNull("componentNames array should not be null", componentNames);
            if (test.componentNames == null || test.componentNames.length == 0)
                assertTrue("Expected no componentNames (array of length 0)", componentNames.length == 0);
            else
            {
                assertEquals("Unexpected number of componentNames", test.componentNames.length, componentNames.length);
                for (int i = 0; i < componentNames.length; ++i)
                {
                    if (test.componentNames[i] == null)
                        assertSame("No componentName expected for [" + i + "]", test.componentNames[i],
                                componentNames[i]);
                    else
                        assertEquals("Unexpected componentName", test.componentNames[i], componentNames[i]);
                }
            }
        }

        /**
         * Tests getComponentTags().
         */
        public void testGetComponentTags() throws Exception
        {
            int[] componentTags = loc.getComponentTags();

            assertNotNull("componentTags array should not be null", componentTags);
            if (test.componentTags == null || test.componentTags.length == 0)
                assertTrue("Expected no componentTags (array of length 0)", componentTags.length == 0);
            else
            {
                assertEquals("Unexpected number of componentTags", test.componentTags.length, componentTags.length);
                for (int i = 0; i < componentTags.length; ++i)
                {
                    if (test.componentTags[i] == -1)
                        assertEquals("No component tag expected for [" + i + "]", test.componentTags[i],
                                componentTags[i]);
                    else
                        assertEquals("Unexpected componentTag", test.componentTags[i], componentTags[i]);
                }
            }
        }

        /**
         * Tests getPathSegments.
         */
        public void testGetPathSegments() throws Exception
        {
            String path = loc.getPathSegments();
            String testpath = (test.path != null) ? test.path : test.abs_path;
            if (testpath == null)
                assertSame("No path segments expected", testpath, path);
            else
                assertEquals("Unexpected path segments", testpath, path);
        }
    }

    /**
     * A simple class used to hold test parameters and construct a locator from
     * those parameters. Also implements to ImplFactory.
     * 
     * @see SubTest
     */
    public static class TestParam extends Assert implements ImplFactory
    {
        /**
         * Implements the ImplFactory method so that the TestParam object can
         * serve as the factory added to the SubTest InterfaceTestCase.
         */
        public Object createImplObject()
        {
            return this;
        }

        /**
         * Constructor for specifying a locator that consists of:
         * 
         * <pre>
         *   ocap:abs_path
         * </pre>
         */
        public TestParam(String abs_path)
        {
            this.abs_path = abs_path;
        }

        /**
         * Constructor specifying a locator that does not consist of simply an
         * <code>abs_path</code>.
         */
        public TestParam(int sourceID, String serviceName, int frequency, int program, int modulationFormat,
                short[] streamTypes, int[] indexes, String[] languageCodes, int eventId, int[] pids,
                String[] componentNames, int[] componentTags, String path)
        {
            this.sourceID = sourceID;
            this.serviceName = serviceName;
            this.frequency = frequency;
            this.programNumber = program;
            this.modulationFormat = modulationFormat;
            this.streamTypes = streamTypes;
            this.indexes = indexes;
            this.languageCodes = languageCodes;
            this.eventId = eventId;
            this.pids = pids;
            this.componentNames = componentNames;
            this.componentTags = componentTags;
            this.path = path;
        }

        /**
         * Generates the locator string.
         */
        public String toString()
        {
            try
            {
                return getLocatorString();
            }
            catch (Exception e)
            {
                return super.toString();
            }
        }

        public String getLocatorString() throws Exception
        {
            return "ocap:" + ocap_hier_part();
        }

        private String ocap_hier_part() throws Exception
        {
            return (abs_path == null) ? (ocap_net_path()) : ocap_abs_path(abs_path);
        }

        private String ocap_abs_path(String absPath)
        {
            return (absPath == null) ? "" : escapedSegments((absPath.startsWith("/") ? absPath : ("/" + absPath)));
        }

        private String ocap_net_path() throws Exception
        {
            return "//" + ocap_entity() + ocap_abs_path(path);
        }

        private String ocap_entity() throws Exception
        {
            return ocap_service() + program_elements() + event_id();
        }

        private String ocap_service() throws Exception
        {
            boolean fail = sourceID != -1;
            if (frequency != -1 || programNumber != -1)
            {
                assertFalse("Too much information", fail);
                fail = true;
            }
            if (serviceName != null)
            {
                assertFalse("Too much information", fail);
                fail = true;
            }

            // source_id
            if (sourceID != -1)
                return hex_str(sourceID);
            // ocap_program
            else if (frequency == -1 && programNumber != -1)
                return "oobfdc." + hex_str(programNumber);
            else if (frequency != -1 && programNumber != -1)
                return "f=" + hex_str(frequency) + "." + hex_str(programNumber)
                        + ((modulationFormat == -1) ? "" : (".m=" + hex_str(modulationFormat)));
            // service_name
            else if (serviceName != null) return "n=" + escapedService(serviceName);

            fail("None of source_id, service_name, or ocap_program are set");
            return "";
        }

        private String language_elements()
        {
            // language_elements := stream_type [ "," ISO] *( "&" stream_type [
            // "," ISO] )
            // index_elements := stream_type [ "," index] *( "&" stream_type [
            // "," index] )

            if (streamTypes != null && streamTypes.length > 0)
            {
                assertTrue("No pids/components expected", (pids == null || pids.length == 0)
                        && (componentNames == null || componentNames.length == 0)
                        && (componentTags == null || componentTags.length == 0));

                assertTrue("Only one of languageCodes or indexes expected", (languageCodes == null && indexes != null)
                        || (languageCodes != null && indexes == null) || (languageCodes == null && indexes == null));

                String st = "";
                String sep = "";
                for (int i = 0; i < streamTypes.length; ++i)
                {
                    st = st + sep + hex_str(streamTypes[i]);
                    if (languageCodes != null && i < languageCodes.length && languageCodes[i] != null)
                        st = st + "," + languageCodes[i];
                    else if (indexes != null && i < indexes.length && indexes[i] != -1)
                        st = st + "," + hex_str(indexes[i]);
                    sep = "&";
                }
                return st;
            }
            return null;
        }

        private String pid_elements()
        {
            // pid_elements := "+" PID * ("&" PID)
            if (pids != null && pids.length > 0)
            {
                assertTrue("No componentNames expected", (componentNames == null || componentNames.length == 0));
                assertTrue("No componentTags expected", (componentTags == null || componentTags.length == 0));
                assertTrue("streamTypes not expected for pids", streamTypes == null || streamTypes.length == 0);
                String st = "";
                String sep = "+";
                for (int i = 0; i < pids.length; ++i)
                {
                    st = st + sep + hex_str(pids[i]);
                    sep = "&";
                }
                return st;
            }
            return null;
        }

        private String component_elements()
        {
            // component_elements := "$" component_name * ("&" component_name)
            if (componentNames != null && componentNames.length > 0)
            {
                assertTrue("No pids expected", (pids == null || pids.length == 0));
                assertTrue("No componentTags expected", (componentTags == null || componentTags.length == 0));
                assertTrue("streamTypes not expected for componentNames", streamTypes == null
                        || streamTypes.length == 0);
                String st = "";
                String sep = "$";
                for (int i = 0; i < componentNames.length; ++i)
                {
                    st = st + sep + escapedComponent(componentNames[i]);
                    sep = "&";
                }
                return st;
            }
            // component_tags := "@" component_tag * ("&" component_tag)
            else if (componentTags != null && componentTags.length > 0)
            {
                assertTrue("No pids expected", (pids == null || pids.length == 0));
                assertTrue("No componentNames expected", (componentNames == null || componentNames.length == 0));
                assertTrue("streamTypes not expected for componentNames", streamTypes == null
                        || streamTypes.length == 0);
                String st = "";
                String sep = "@";
                for (int i = 0; i < componentTags.length; ++i)
                {
                    st = st + sep + hex_str(componentTags[i]);
                    sep = "&";
                }
                return st;
            }
            return null;
        }

        private String program_elements()
        {
            String str;
            return ((str = language_elements()) != null || (str = pid_elements()) != null || (str = component_elements()) != null) ? ("." + str)
                    : "";
        }

        private String event_id()
        {
            return (eventId != -1) ? (";" + hex_str(eventId)) : "";
        }

        private String hex_str(int hex)
        {
            return "0x" + Integer.toHexString(hex);
        }

        private boolean isUnreserved(char c)
        {
            return unreservedChars.get(c);
        }

        /**
         * Where can escaped characters show up?
         * <ul>
         * <li>service_name = 1* (unreserved_not_dot | escaped)
         * <li>component_name = 1* (unreserved | escaped)
         * <li>path_segments->segment->param->pchar = unreserved | escaped | ":"
         * | "@" | "&" | "=" | "+" | "$" | ","
         * </ul>
         * 
         * When can they be specified?
         * <ul>
         * <li>OcapLocator(String)
         * </ul>
         * 
         * All methods that return a String (aside from toExternalForm())
         * <i>unescape</i> characters. All constructors that take strings must
         * handle escape characters and escape where necessary.
         * 
         * From RFC 2396:
         * 
         * <pre>
         * 2.4. Escape Sequences
         * 
         *    Data must be escaped if it does not have a representation using an
         *    unreserved character; this includes data that does not correspond to
         *    a printable character of the US-ASCII coded character set, or that
         *    corresponds to any US-ASCII character that is disallowed, as
         *    explained below.
         * 
         * 2.4.1. Escaped Encoding
         * 
         *    An escaped octet is encoded as a character triplet, consisting of the
         *    percent character "%" followed by the two hexadecimal digits
         *    representing the octet code. For example, "%20" is the escaped
         *    encoding for the US-ASCII space character.
         * 
         *       escaped     = "%" hex hex
         *       hex         = digit | "A" | "B" | "C" | "D" | "E" | "F" |
         *                             "a" | "b" | "c" | "d" | "e" | "f"
         * 
         * 2.4.2. When to Escape and Unescape
         * 
         *    A URI is always in an "escaped" form, since escaping or unescaping a
         *    completed URI might change its semantics.  Normally, the only time
         *    escape encodings can safely be made is when the URI is being created
         *    from its component parts; each component may have its own set of
         *    characters that are reserved, so only the mechanism responsible for
         *    generating or interpreting that component can determine whether or
         *    not escaping a character will change its semantics. Likewise, a URI
         *    must be separated into its components before the escaped characters
         *    within those components can be safely decoded.
         * 
         *    In some cases, data that could be represented by an unreserved
         *    character may appear escaped; for example, some of the unreserved
         *    "mark" characters are automatically escaped by some systems.  If the
         *    given URI scheme defines a canonicalization algorithm, then
         *    unreserved characters may be unescaped according to that algorithm.
         *    For example, "%7e" is sometimes used instead of "~" in an http URL
         *    path, but the two are equivalent for an http URL.
         * 
         *    Because the percent "%" character always has the reserved purpose of
         *    being the escape indicator, it must be escaped as "%25" in order to
         *    be used as data within a URI.  Implementers should be careful not to
         *    escape or unescape the same string more than once, since unescaping
         *    an already unescaped string might lead to misinterpreting a percent
         *    data character as another escaped character, or vice versa in the
         *    case of escaping an already escaped string.
         * </pre>
         */
        private String escaped(String str, TestChar test)
        {
            StringBuffer sb = new StringBuffer();
            char[] chars = str.toCharArray();
            boolean escaped = false;

            for (int i = 0; i < chars.length; ++i)
            {
                if (test.accept(chars[i]))
                {
                    sb.append(chars[i]);
                }
                else
                {
                    if (chars[i] < 0x80 && chars[i] > 0)
                    {
                        // just a regular ascii character
                        sb.append('%');
                        if ((chars[i] & 0xff) <= 0x0F)
                        {
                            sb.append('0');
                        }
                        sb.append(Integer.toHexString(chars[i]));
                    }
                    else
                    {
                        // character has a special UTF-8 encoding
                        String string;
                        byte bytes[];

                        string = new String(new char[] { chars[i] });
                        try
                        {
                            bytes = string.getBytes("UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            System.out.println("UnsupportedEncodingException");
                            bytes = string.getBytes();
                        }
                        for (int j = 0; j < bytes.length; j++)
                        {
                            sb.append('%');
                            if ((bytes[j] & 0xff) < 0xf)
                            {
                                sb.append('0');
                            }
                            sb.append(Integer.toHexString(bytes[j] & 0xff));
                        }
                    }
                    escaped = true;
                }
            }
            return escaped ? sb.toString() : str;
        }

        private interface TestChar
        {
            boolean accept(char c);
        }

        /**
         * Returns a properly escaped version of the given string.
         * 
         * Used to escape a component name:
         * 
         * <pre>
         * component_name = 1 * (unreserved | escaped)
         * </pre>
         */
        private String escapedComponent(String str)
        {
            return escaped(str, new TestChar()
            {
                public boolean accept(char c)
                {
                    return isUnreserved(c) || '.' == c;
                }
            });
        }

        /**
         * Returns a properly escaped version of the given string, assuming no
         * dot characters are allowed.
         * 
         * Used to escape a service name as in:
         * 
         * <pre>
         * <li> service_name = 1* (unreserved_not_dot | escaped)
         * </pre>
         */
        private String escapedService(String str)
        {
            return escaped(str, new TestChar()
            {
                public boolean accept(char c)
                {
                    return isUnreserved(c);
                }
            });
        }

        /**
         * Returns a properly escaped version of the given string, assuming only
         * unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ",".
         * 
         * Used to escape a path_segment(s) as in:
         * 
         * <pre>
         * <li> path_segments->segment->param->pchar = unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ","
         * </pre>
         * 
         * From RFC 2396: Within a path segment, the characters "/", ";", "=",
         * and "?" are reserved.
         */
        private String escapedSegments(String str)
        {
            return escaped(str, new TestChar()
            {
                public boolean accept(char c)
                {
                    return isUnreserved(c) || pchars.get(c) || c == '/' || c == ';' // segment/param
                                                                                    // separators
                            || c == '.';
                }
            });
        }

        private int dehex(char c1, char c0)
        {
            String hex = "0123456789abcdef";
            int c[] = { Character.toLowerCase(c1), Character.toLowerCase(c0) };
            int value = 0;

            for (int i = 0; i < c.length; ++i)
            {
                int newvalue = hex.indexOf(c[i]);
                assertFalse("Invalid hex character", newvalue != -1);
                value = (value << 8) | newvalue;
            }
            return value;
        }

        /**
         * Unescapes characters in the string.
         */
        private String unescape(String str)
        {
            char[] chars = str.toCharArray();
            boolean escaped = false;
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            String returnString = null;

            for (int i = 0; i < chars.length; ++i)
            {
                char c = chars[i];
                if (chars[i] == '%' && i + 2 < chars.length)
                {
                    escaped = true;
                    stream.write((byte) dehex(chars[i + 1], chars[i + 2]));
                    i += 2;
                }
                else
                {
                    stream.write(c);
                }
            }

            if (!escaped)
            {
                returnString = stream.toString();
            }
            else
            {
                try
                {
                    returnString = stream.toString("UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    returnString = stream.toString();
                }
            }
            return returnString;
        }

        private static final char mark_not_dot[] = { '-', '_', '!', '~', '*', '\'', '(', ')' };

        private static final char pchars_array[] = { ':', '@', '&', '=', '+', '$', ',' };

        private static final java.util.BitSet unreservedChars;

        private static final java.util.BitSet pchars;
        static
        {
            unreservedChars = new java.util.BitSet();

            for (char c = 'a'; c <= 'z'; ++c)
                unreservedChars.set(c);
            for (char c = 'A'; c <= 'Z'; ++c)
                unreservedChars.set(c);
            for (char c = '0'; c <= '9'; ++c)
                unreservedChars.set(c);
            for (int i = 0; i < mark_not_dot.length; ++i)
                unreservedChars.set(mark_not_dot[i]);

            pchars = new java.util.BitSet();
            for (int i = 0; i < pchars_array.length; ++i)
                pchars.set(pchars_array[i]);
        }

        public int sourceID = -1;

        public String serviceName = null;

        public int frequency = -1;

        public int programNumber = -1;

        public int modulationFormat = -1;

        public short[] streamTypes = null;

        public String[] languageCodes = null;

        public int[] indexes = null;

        public int eventId = -1;

        public int[] pids = null;

        public String[] componentNames = null;

        public int[] componentTags = null;

        public String path = null;

        public String abs_path = null;
    }

    /**
     * Some stream_types used for testing.
     */
    private static final short[] stream_types = { 0x1, 0xe, 0x84, 0x3 };

    /**
     * Some eventIDs used for testing.
     */
    private static int[] event_id = { -1, 0x3eee };

    private static final String PATH_NOT_TOOLONG = "abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx"
            + "/abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx"
            + "/abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx"
            + "/abcdefghijklmnopqrstuvwx" + "/abcdefghijklmnopqrstuvwx" + "/abc";

    private static final String PATH_TOOLONG = PATH_NOT_TOOLONG + "x";

    private static final String PATH_NULL = "/path/with/\u0000null/byte";

    /**
     * Add tests for the given ocap_service.
     */
    private static void addTests(InterfaceTestSuite isuite, int sourceID, String serviceName, int freq, int prog,
            int modulationFormat)
    {
        /*
         * public TestParam(int sourceID, String serviceName, int frequency, int
         * program, int modulationFormat, short[] streamTypes, int[] indexes,
         * String[] languageCodes, int eventId, int[] pids, String[]
         * componentNames, int[] componentTags, String path)
         */
        final short[] noStreamTypes = null;
        final String[] noLanguages = null;
        final int[] noIndexes = null;
        final int[] noPids = null;
        final String[] noComponents = null;
        final int[] noComponentTags = null;
        final String noPath = null;
        // No service_component
        isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes, noIndexes,
                noLanguages, -1, noPids, noComponents, noComponentTags, noPath));

        // Service_components:
        // event_ids (with and without)
        for (int i = 0; i < event_id.length; ++i)
        {
            // Program_Elements:

            // none
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, noComponents, noComponentTags, noPath));

            // language_elements
            // index_elements
            for (int j = 0; j < stream_types.length; ++j)
            {
                isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat,
                        new short[] { stream_types[j] }, noIndexes, noLanguages, event_id[i], noPids, noComponents,
                        noComponentTags, noPath));
                isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat,
                        new short[] { stream_types[j] }, noIndexes, new String[] { "spa" }, event_id[i], noPids,
                        noComponents, noComponentTags, noPath));
                isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat,
                        new short[] { stream_types[j] }, new int[] { 0x2 }, noLanguages, event_id[i], noPids,
                        noComponents, noComponentTags, noPath));
            }
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, new short[] {
                    stream_types[0], stream_types[1], stream_types[2] }, noIndexes,
                    new String[] { "spa", null, "eng" }, event_id[i], noPids, noComponents, noComponentTags, noPath));
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, new short[] {
                    stream_types[0], stream_types[1], stream_types[2] }, new int[] { 0x1, 0x2, -1 }, noLanguages,
                    event_id[i], noPids, noComponents, noComponentTags, noPath));
            // PID_elements
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], new int[] { 0x23 }, noComponents, noComponentTags, noPath));
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], new int[] { 0x23, 0x25, 0x10 }, noComponents, noComponentTags,
                    noPath));
            // component_elements (names)
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, new String[] { "ajk" }, noComponentTags, noPath));
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, new String[] { "ajk", "abc", "xyz" }, noComponentTags,
                    noPath));
            // component_elements (tags)
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, noComponents, new int[] { 0x9 }, noPath));
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, noComponents, new int[] { 0x10, 0x33, 0x7 }, noPath));
            // ...with unescaped chars
            isuite.addFactory(new TestParam(sourceID, serviceName, freq, prog, modulationFormat, noStreamTypes,
                    noIndexes, noLanguages, event_id[i], noPids, new String[] { "abc xyz", "-_.!~*'()X \t`%:@&=+," }, // following
                                                                                                                      // X
                                                                                                                      // should
                                                                                                                      // be
                                                                                                                      // escaped
                    noComponentTags, noPath));
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
        TestSuite suite = new TestSuite(OcapLocatorTest.class);

        InterfaceTestSuite isuite = SubTest.isuite();

        // set to true and modify the addFactory call to
        // test a single locator format
        if (false)
        {
            // Quick Test
            addTests(isuite, -1, "-_!~*'()X .\t`%:@&=+$,", -1, -1, -1);
            // isuite.addFactory(new
            // TestParam("unescaped:@&=+$,/escaped \t`%/other;param"));
            return isuite;
        }

        // abs_path
        isuite.addFactory(new TestParam("a/dir/filename"));
        // all stream_types
        isuite.addFactory(new TestParam(0xabc, null, -1, -1, -1, STREAM_TYPE_VALUES, null, null, -1, null, null, null,
                null));

        addTests(isuite, 0x5432, null, -1, -1, -1); // sourceID
        addTests(isuite, -1, "ABC", -1, -1, -1); // serviceName
        addTests(isuite, -1, null, 0xff, 0x66, -1); // freq.prog
        addTests(isuite, -1, null, 0xff, 0x66, 0x10); // freq.prog.mod
        addTests(isuite, -1, null, -1, 0x66, -1); // oobfdc.prog

        // Handling of escape characters
        // service name to be escaped (all chars following X)
        addTests(isuite, -1, "-_!~*'()X .\t`%:@&=+$,", -1, -1, -1);
        // path_segments to be escaped
        isuite.addFactory(new TestParam("unescaped:@&=+$,/escaped \t`%/other;param"));
        // component name to be escaped handled by addTests()

        suite.addTest(isuite);

        return suite;
    }

    public OcapLocatorTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
