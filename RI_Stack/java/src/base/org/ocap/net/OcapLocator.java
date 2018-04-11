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

package org.ocap.net;

import org.davic.net.InvalidLocatorException;
import java.util.Vector;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

/**
 * <P>
 * This class encapsulates an OCAP URL into an object. This class provides
 * access to locations of various types of items in the transport stream.
 * </P>
 * <P>
 * Note that the org.davic.net.Locator (super class of the OcapLocator) is
 * modified in Section 12.2.1.9.4 <i>Content Referencing</i> of this
 * specification as following: <BLOCKQUOTE> org.davic.net.Locator implements
 * javax.tv.locator.Locator </BLOCKQUOTE> The
 * javax.tv.locator.Locator.toExternalForm() method returns an OCAP URL string
 * that is used to create an OcapLocator instance, in canonical form. If an OCAP
 * locator is in canonical form, the following MUST hold:
 * <UL>
 * <LI>no character is escaped if it is possible to represent it without
 * escaping according to the OCAP URL BNF. (E.g. "%41" is changed to "A").</LI>
 * <LI>hex numbers do not have leading zeros, except for the number zero itself,
 * which is represented as "0x0". (E.g. "0x01" is changed to "0x1").</LI>
 * <LI>all instances of ISO_639_language_code must be lowercase. (E.g. "SPA" is
 * changed to "spa").</LI>
 * </UL>
 * No other change is performed to convert an OCAP locator to its canonical
 * form.
 * </P>
 * <P>
 * All methods defined in this class that return Strings, except for
 * toExternalForm(), return the String in Unicode format. I.e. They MUST
 * un-escape characters in the corresponding portion of the URL that are escaped
 * with the %nn syntax (where that syntax is permitted by the OCAP URL BNF), and
 * they MUST UTF-8 decode the string.
 * </P>
 * <P>
 * All constructors defined in this class that take String parameters, except
 * for the OcapLocator(String url) constructor, require the String in Unicode
 * format. I.e. Where permitted by the OCAP URL BNF they MUST UTF-8 encode the
 * string and they MUST escape (using the %nn syntax) any characters that
 * require escaping. They MUST NOT escape any character that can be represented
 * without escaping.
 * </P>
 * 
 * @see org.davic.net.Locator
 * @see org.dvb.application.AppAttributes#getServiceLocator
 * 
 * @author Aaron Kamienski
 */

public class OcapLocator extends org.davic.net.Locator
{
    private int sourceId = -1;

    private int programNumber = -1;

    private int eventId = -1;

    private int frequency = -1;

    private int[] pid = null;

    private short[] streamType = null;

    private int[] index = null;

    private String[] languageCodes = null;

    private String pathSegments = null;

    private String[] componentName = null;

    private int[] componentTag = null;

    private String serviceName = null;

    private int modulationFormat = -1;

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://source_id".
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the sourceID to construct the locator doesn't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID) throws InvalidLocatorException
    {
        super("ocap://0x" + Integer.toHexString(sourceID));
        setSourceId(sourceID);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number[.m=modulation_format]"
     * .
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getSourceId() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat) throws InvalidLocatorException
    {
        super(prefixUrl(frequency, programNumber, modulationFormat));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://f=frequency[.m=modulation_format]".
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getSourceId() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz.
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog. If the value is -1 the modulation_format is not
     *            specified and the modulation_format term will not be included
     *            in the locator constructed
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int modulationFormat) throws InvalidLocatorException
    {
        super(prefixUrl(frequency, modulationFormat));
        setFrequencyModulation(frequency, modulationFormat);
    }

    /**
     * <P>
     * A constructor of this class for any form of OCAP URL.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform the specified
     * url string to any other form, even if any get methods for the value that
     * is not included in the url string are called.
     * </P>
     * 
     * @param url
     *            a string expression that represents the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the url to construct the locator doesn't specify a valid
     *             OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String url) throws InvalidLocatorException
    {
        super(url);
        if (!url.startsWith("ocap:"))
        {
            throw new InvalidLocatorException("Locator Url " + url + " is not valid ocap locator.");
        }
        LocatorParser parser = new LocatorParser(url);
        parser.parse();
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://source_id[.stream_type[,ISO_639_language_code]{&
     * stream_type[,ISO_639_language_code]}][;event_id]{/path_segments}". Some
     * of the parameters can be omitted according to the OCAP URL BNF
     * definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The streamType shall be a zero length array,
     *            if it is omitted in the OCAP URL.
     * 
     * @param ISO639LanguageCode
     *            an ISO_639_language_code value for the OCAP URL. A combination
     *            of the streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The ISO639LanguageCode shall be a zero length
     *            array, if it is omitted in the OCAP URL. If ISO639LanguageCode
     *            is not a zero-length array, it shall be an array with the same
     *            length as streamType. If ISO639LanguageCode[n] is null, then
     *            the language code for streamType[n] is omitted in the OCAP
     *            URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID, short[] streamType, String[] ISO639LanguageCode, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(sourceID), streamType, ISO639LanguageCode, eventID, pathSegments));
        setSourceId(sourceID);
        setStreamTypes(streamType);
        setLanguageCodes(ISO639LanguageCode);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://source_id[.stream_type[,index]{&stream_type[,index]}]
     * [;event_id]{/path_segments}". Some of parameters can be omitted according
     * to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            streamType shall be a zero length array, if it is omitted in
     *            the OCAP URL.
     * 
     * @param index
     *            an index value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            index shall be a zero length array, if it is omitted in the
     *            OCAP URL. If index is not a zero-length array, it shall be an
     *            array with the same length as streamType. If index[n] is -1,
     *            then the index for streamType[n] is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID, short[] streamType, int[] index, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(sourceID), streamType, index, eventID, pathSegments));
        setSourceId(sourceID);
        setStreamTypes(streamType);
        setIndexes(index);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://source_id[.+PID{&PID}][;event_id]{/path_segments}". Some of
     * parameters can be omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @param PID
     *            a PID value for the OCAP URL. The PID shall be a zero length
     *            array, if it is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID, int[] PID, int eventID, String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(sourceID), PID, eventID, pathSegments));
        setSourceId(sourceID);
        setPIDs(PID);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://source_id[.$component_name{&component_name}]
     * [;event_id]{/path_segments}". Some of parameters can be omitted according
     * to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @param componentName
     *            a component_name value for the OCAP URL. The component_name
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID, String[] componentName, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(sourceID), componentName, eventID, pathSegments));
        setSourceId(sourceID);
        setComponentNames(componentName);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form"ocap://source_id[.@component_tag{&component_tag}][;event_id]{/path_segments}"
     * . Some of parameters can be omitted according to the OCAP URL BNF
     * definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * <P>
     * <STRONG>WARNING:</STRONG> Note that the parameter order for this
     * constructor is different from other OcapLocator constructors - the
     * eventId is <EM>before</EM> the componentTags. If you are an OCAP
     * application author and you get it wrong, your program will compile and
     * run but it will be calling the constructor that expects a list of PIDs
     * instead.
     * </P>
     * 
     * @param sourceID
     *            a source_id value for the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param componentTags
     *            a component_tag value for the OCAP URL. The component_tag
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int sourceID, int eventID, int[] componentTags, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(sourceID), eventID, componentTags, pathSegments));
        setSourceId(sourceID);
        setComponentTags(componentTags);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://n=service_name[.stream_type[,ISO_639_language_code]{&
     * stream_type[,ISO_639_language_code]}][;event_id]{/path_segments}". Some
     * of parameters can be omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param serviceName
     *            a service_name value for the OCAP URL.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The streamType shall be a zero length array,
     *            if it is omitted in the OCAP URL.
     * 
     * @param ISO639LanguageCode
     *            an ISO_639_language_code value for the OCAP URL. A combination
     *            of the streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The ISO639LanguageCode shall be a zero length
     *            array, if it is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String serviceName, short[] streamType, String[] ISO639LanguageCode, int eventID,
            String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(serviceName), streamType, ISO639LanguageCode, eventID, pathSegments));
        setServiceName(serviceName);
        setStreamTypes(streamType);
        setLanguageCodes(ISO639LanguageCode);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://n=service_name[.stream_type[,index]{&stream_type[,index]}]
     * [;event_id]{/path_segments}". Some of parameters can be omitted according
     * to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param serviceName
     *            a service_name value for the OCAP URL.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            streamType shall be a zero length array, if it is omitted in
     *            the OCAP URL.
     * 
     * @param index
     *            an index value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            index shall be a zero length array, if it is omitted in the
     *            OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String serviceName, short[] streamType, int[] index, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(serviceName), streamType, index, eventID, pathSegments));
        setServiceName(serviceName);
        setStreamTypes(streamType);
        setIndexes(index);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://n=service_name[.+PID{&PID}][;event_id]{/path_segments}". Some
     * of parameters can be omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param serviceName
     *            a service_name value for the OCAP URL.
     * 
     * @param PID
     *            a PID value for the OCAP URL. The PID shall be a zero length
     *            array, if it is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String serviceName, int[] PID, int eventID, String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(serviceName), PID, eventID, pathSegments));
        setServiceName(serviceName);
        setPIDs(PID);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://n=service_name[.$component_name{&component_name}]
     * [;event_id]{/path_segments}". Some of parameters can be omitted according
     * to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param serviceName
     *            a service_name value for the OCAP URL.
     * 
     * @param componentName
     *            a component_name value for the OCAP URL. The component_name
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String serviceName, String[] componentName, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(serviceName), componentName, eventID, pathSegments));
        setServiceName(serviceName);
        setComponentNames(componentName);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form"ocap://n=service_name[.@component_tag{&component_tag}][;event_id]{/path_segments}"
     * . Some of parameters can be omitted according to the OCAP URL BNF
     * definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * <P>
     * <STRONG>WARNING:</STRONG> Note that the parameter order for this
     * constructor is different from other OcapLocator constructors - the
     * eventId is <EM>before</EM> the componentTags. If you are an OCAP
     * application author and you get it wrong, your program will compile and
     * run but it will be calling the constructor that expects a list of PIDs
     * instead.
     * </P>
     * 
     * @param serviceName
     *            a service_name value for the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param componentTags
     *            a component_tag value for the OCAP URL. The component_tag
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(String serviceName, int eventID, int[] componentTags, String pathSegments)
            throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(serviceName), eventID, componentTags, pathSegments));
        setServiceName(serviceName);
        setComponentTags(componentTags);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number[.m=modulation_format]
     * [.stream_type[,ISO_639_language_code]
     * {&stream_type[,ISO_639_language_code]}][;event_id]{/path_segments}". Some
     * of parameters can be omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The streamType shall be a zero length array,
     *            if it is omitted in the OCAP URL.
     * 
     * @param ISO639LanguageCode
     *            an ISO_639_language_code value for the OCAP URL. A combination
     *            of the streamType[n] and the ISO639LanguageCode[n] makes a
     *            program_element. The ISO639LanguageCode shall be a zero length
     *            array, if it is omitted in the OCAP URL. If ISO639LanguageCode
     *            is not a zero-length array, it shall be an array with the same
     *            length as streamType. If ISO639LanguageCode[n] is null, then
     *            the language code for streamType[n] is omitted in the OCAP
     *            URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat, short[] streamType,
            String[] ISO639LanguageCode, int eventID, String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(frequency, programNumber, modulationFormat), streamType, ISO639LanguageCode, eventID,
                pathSegments));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
        setStreamTypes(streamType);
        setLanguageCodes(ISO639LanguageCode);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number.[m=modulation_format]
     * [.stream_type[,index]{&stream_type[,index]}]
     * [;event_id]{/path_segments}". Some of parameters can be omitted according
     * to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @param streamType
     *            a stream_type value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            streamType shall be a zero length array, if it is omitted in
     *            the OCAP URL.
     * 
     * @param index
     *            an index value for the OCAP URL. A combination of the
     *            streamType[n] and the index[n] makes a program_element. The
     *            index shall be a zero length array, if it is omitted in the
     *            OCAP URL. If index is not a zero-length array, it shall be an
     *            array with the same length as streamType. If index[n] is -1,
     *            then the index for streamType[n] is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat, short[] streamType, int[] index,
            int eventID, String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(frequency, programNumber, modulationFormat), streamType, index, eventID, pathSegments));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
        setStreamTypes(streamType);
        setIndexes(index);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number[.m=modulation_format]
     * [.+PID{&PID}][;event_id]{/path_segments}". Some of parameters can be
     * omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @param PID
     *            a PID value for the OCAP URL. The PID shall be a zero length
     *            array, if it is omitted in the OCAP URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat, int[] PID, int eventID,
            String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(frequency, programNumber, modulationFormat), PID, eventID, pathSegments));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
        setPIDs(PID);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number[.m=modulation_format]
     * [.$component_name{&component_name}] [;event_id]{/path_segments}".
     * Some of parameters can be omitted according to the OCAP URL BNF
     * definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @param componentName
     *            a component_name value for the OCAP URL. The component_name
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat, String[] componentName, int eventID,
            String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(frequency, programNumber, modulationFormat), componentName, eventID, pathSegments));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
        setComponentNames(componentName);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * <P>
     * A constructor of this class corresponding to the OCAP URL form
     * "ocap://(oobfdc|f=frequency).program_number[.m=modulation_format]
     * [.@component_tag{&component_tag}][;event_id]{/path_segments}". Some
     * of parameters can be omitted according to the OCAP URL BNF definition.
     * </P>
     * <P>
     * Note that the OcapLocator does not automatically transform this OCAP URL
     * BNF form to any other form, even if the getFrequency() etc. is called.
     * </P>
     * <P>
     * <STRONG>WARNING:</STRONG> Note that the parameter order for this
     * constructor is different from other OcapLocator constructors - the
     * eventId is <EM>before</EM> the componentTags. If you are an OCAP
     * application author and you get it wrong, your program will compile and
     * run but it will be calling the constructor that expects a list of PIDs
     * instead.
     * </P>
     * 
     * @param frequency
     *            a frequency value for the OCAP URL in hertz. If the value is
     *            -1 then "oobfdc" is used instead of the frequency term and the
     *            modulationFormat parameter is ignored.
     * 
     * @param programNumber
     *            a program_number value for the OCAP URL
     * 
     * @param modulationFormat
     *            a value representing a modulation_format as specified in SCTE
     *            65. If the value is 0xFF the modulation_format is treated as
     *            NTSC analog and the programNumber parameter is ignored. If the
     *            value is -1 the modulation_format is not specified and the
     *            modulation_format term will not be included in the locator
     *            constructed.
     * 
     * @param eventID
     *            an event_id value for the OCAP URL. The event_id shall be -1,
     *            if it is omitted in the OCAP URL.
     * 
     * @param componentTags
     *            a component_tag value for the OCAP URL. The component_tag
     *            shall be a zero length array, if it is omitted in the OCAP
     *            URL.
     * 
     * @param pathSegments
     *            a path_segments value for the OCAP URL. The pathSegments shall
     *            be null, if it is omitted in the OCAP URL.
     * 
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator don't specify a
     *             valid OCAP URL (e.g. a value is out of range).
     */
    public OcapLocator(int frequency, int programNumber, int modulationFormat, int eventID, int[] componentTags,
            String pathSegments) throws InvalidLocatorException
    {
        super(thisUrl(prefixUrl(frequency, programNumber, modulationFormat), eventID, componentTags, pathSegments));
        setFrequencyProgramModulation(frequency, programNumber, modulationFormat);
        setComponentTags(componentTags);
        setEventId(eventID);
        setPathSegments(pathSegments);
    }

    /**
     * Returns the start of a url expressed as a <code>String</code> for the
     * given <i>sourceID</i>.
     * 
     * @param sourceID
     * @return the start of a url expressed as a <code>String</code> for the
     *         given <i>sourceID</i>
     */
    private static String prefixUrl(int sourceID)
    {
        return "ocap://0x" + Integer.toHexString(sourceID);
    }

    /**
     * Returns the start of a url expressed as a <code>String</code> for the
     * given <i>serviceName</i>.
     * 
     * @param serviceName
     * @return the start of a url expressed as a <code>String</code> for the
     *         given <i>sourceID</i>
     * 
     * @throws InvalidLocatorException
     *             actually won't ever be thrown
     */
    private static String prefixUrl(String serviceName) throws InvalidLocatorException
    {
        return "ocap://n=" + escape(serviceName, false, false);
    }

    /**
     * Returns the start of a url expressed as a <code>String</code> for the
     * given <i>frequency</i> <i>program</i>.
     * 
     * @param frequency
     * @param program
     * @param modulationFormat
     * @return the start of a url expressed as a <code>String</code> for the
     *         given <i>frequency</i> <i>program</i>
     */
    private static String prefixUrl(int frequency, int program, int modulationFormat)
    {
        if (frequency == -1)
        {
            return "ocap://oobfdc.0x" + Integer.toHexString(program);
        }
        String url = "ocap://f=0x" + Integer.toHexString(frequency) + ".0x" + Integer.toHexString(program);

        return (modulationFormat == -1) ? url : (url + ".m=0x" + Integer.toHexString(modulationFormat));
    }

    /**
     * Returns the start of a url expressed as a <code>String</code> for the
     * given <i>frequency</i> <i>modulation</i>.
     * 
     * @param frequency
     * @param modulationFormat
     * @return the start of a url expressed as a <code>String</code> for the
     *         given <i>frequency</i> <i>modulation</i>
     */
    private static String prefixUrl(int frequency, int modulationFormat)
    {
        String url = "ocap://f=0x" + Integer.toHexString(frequency);

        return (modulationFormat == -1) ? url : (url + ".m=0x" + Integer.toHexString(modulationFormat));
    }

    /**
     * Construct the ocapLocator URL which looks like
     * <code>ocap://sourceID.componentElements</code> or
     * <code>ocap://n=serviceName.componentElements</code>, where:
     * 
     * <pre>
     *    componentElements ="$" componentName * ( "&" componentName )
     * </pre>
     */
    private static String thisUrl(String url, String[] componentName, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        if (componentName == null)
            throw new InvalidLocatorException("zero length componentName array expected instead of null");

        String sep = ".$";
        for (int i = 0; i < componentName.length; ++i)
        {
            url = url + sep + escape(componentName[i], true, false);
            sep = "&";
        }
        // append eventID if it is valid (greater than 0)
        if (eventID > 0) url = url + ";0x" + Integer.toHexString(eventID);

        // append pathSegments if they are present
        if (pathSegments != null)
        {
            if (!pathSegments.startsWith("/")) url = url + "/";
            url = url + escape(pathSegments);
        }

        return url;
    }

    /**
     * Construct the ocapLocator URL which looks like
     * <code>ocap://sourceID.componentElements</code> or
     * <code>ocap://n=serviceName.componentElements</code>, where:
     * 
     * <pre>
     *    componentElements ="@" componentTag * ( "&" componentTag )
     * </pre>
     */
    private static String thisUrl(String url, int eventID, int[] componentTag, String pathSegments)
            throws InvalidLocatorException
    {
        if (componentTag == null)
            throw new InvalidLocatorException("zero length componentTag array expected instead of null");

        String sep = ".@";
        for (int i = 0; i < componentTag.length; ++i)
        {
            url = url + sep + "0x" + Integer.toHexString(componentTag[i]);
            sep = "&";
        }
        // append eventID if it is valid (greater than 0)
        if (eventID > 0) url = url + ";0x" + Integer.toHexString(eventID);

        // append pathSegments if they are present
        if (pathSegments != null)
        {
            if (!pathSegments.startsWith("/")) url = url + "/";
            url = url + escape(pathSegments);
        }

        return url;
    }

    /**
     * Construct the ocapLocator URL which looks like
     * <code>ocap://sourceID.languageElements</code> or
     * <code>ocap://n=serviceName.languageElements</code>, where:
     * 
     * <pre>
     *     languageElements = stream_type [ "," ISO_639_language_code ] *( "&" stream_type [ "," ISO_639_language_code ] )
     * </pre>
     */
    private static String thisUrl(String url, short[] streamType, String[] ISO639LanguageCode, int eventID,
            String pathSegments) throws InvalidLocatorException
    {
        if (streamType == null)
            throw new InvalidLocatorException("zero length streamType array expected instead of null");
        if (ISO639LanguageCode == null)
            throw new InvalidLocatorException("zero length ISO639LanguageCode array expected instead of null");
        if (ISO639LanguageCode.length != 0 && ISO639LanguageCode.length != streamType.length)
            throw new InvalidLocatorException("number of ISO639LanguageCode entries (" + ISO639LanguageCode.length
                    + ") needs to match the number streamType entries (" + streamType.length + ")");

        String sep = ".0x";
        for (int i = 0; i < streamType.length; ++i)
        {
            url = url + sep + Integer.toHexString(streamType[i]);
            if (ISO639LanguageCode.length != 0 && ISO639LanguageCode[i] != null)
                url = url + "," + ISO639LanguageCode[i].toLowerCase(); // canonical
                                                                       // form
            sep = "&0x";
        }
        // append eventID if it is greater than 0
        if (eventID > 0) url = url + ";0x" + Integer.toHexString(eventID);

        // append pathSegments if they are present
        if (pathSegments != null)
        {
            if (!pathSegments.startsWith("/")) url = url + "/";
            url = url + escape(pathSegments);
        }

        return url;
    }

    /**
     * Construct the ocapLocator URL which looks like
     * <code>ocap://sourceID.languageElements</code> or
     * <code>ocap://n=serviceName.languageElements</code>, where:
     * 
     * <pre>
     *     languageElements = stream_type [ "," index ] *( "&" stream_type [ "," index ] )
     * </pre>
     */
    private static String thisUrl(String url, short[] streamType, int[] index, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        if (streamType == null)
            throw new InvalidLocatorException("zero length streamType array expected instead of null");
        if (index == null) throw new InvalidLocatorException("zero length index array expected instead of null");
        if (index.length != 0 && index.length != streamType.length)
            throw new InvalidLocatorException("more index entries (" + index.length + ") than streamType entries ("
                    + streamType.length + ")");

        String sep = ".0x";
        for (int i = 0; i < streamType.length; ++i)
        {
            url = url + sep + Integer.toHexString(streamType[i]);
            if (index.length != 0 && index[i] != -1) url = url + ",0x" + Integer.toHexString(index[i]);
            sep = "&0x";
        }
        // append eventID if it is greater than 0
        if (eventID > 0) url = url + ";0x" + Integer.toHexString(eventID);

        // append pathSegments if they are present
        if (pathSegments != null)
        {
            if (!pathSegments.startsWith("/")) url = url + "/";
            url = url + escape(pathSegments);
        }

        return url;
    }

    /**
     * Construct the ocapLocator URL which looks like
     * <code>ocap://sourceID.pidElements</code> or
     * <code>ocap://n=serviceName.pidElements</code>, where:
     * 
     * <pre>
     *    pidElements ="+" PID * ( "&" PID )
     * </pre>
     */
    private static String thisUrl(String url, int[] PID, int eventID, String pathSegments)
            throws InvalidLocatorException
    {
        if (PID == null) throw new InvalidLocatorException("zero length PID array expected instead of null");

        String sep = ".+0x";
        for (int i = 0; i < PID.length; ++i)
        {
            url = url + sep + Integer.toHexString(PID[i]);
            sep = "&0x";
        }
        // append eventID if it is greater than 0
        if (eventID > 0) url = url + ";0x" + Integer.toHexString(eventID);

        // append pathSegments if they are present
        if (pathSegments != null)
        {
            if (!pathSegments.startsWith("/")) url = url + "/";
            url = url + escape(pathSegments);
        }

        return url;
    }

    /**
     * Translates the given path string such that escape sequences are inserted
     * where appropriate. Encodes the given string using the '%' syntax
     * specified by RFC 2396 section 2.4.
     * 
     * @param path
     *            the path <code>String</code> to be escaped
     * @return the <i>escaped</i> path string
     * 
     * @see #escape(String, boolean, boolean)
     * 
     * @throws InvalidLocatorException
     *             if path is too long or contains embedded nulls
     */
    private static String escape(String path) throws InvalidLocatorException
    {
        return escape(path, true, true);
    }

    /**
     * Translates the given string such that escape sequences are inserted where
     * appropriate. Encodes the given string using the '%' syntax specified by
     * RFC 2396 section 2.4.
     * <p>
     * 
     * Handles the following:
     * <ul>
     * <li>service_name = 1* (unreserved_not_dot | escaped)
     * <li>component_name = 1* (unreserved | escaped)
     * <li>path_segments->pchar = unreserved | escaped | ":" | "@" | "&" | "=" |
     * "+" | "$" | ","
     * </ul>
     * 
     * @param str
     *            the <code>String</code> to be escaped
     * @param dot
     *            if <code>true</code> then don't escape <code>'.'</code>
     * @param path
     *            if <code>true</code> then don't escape valid <i>pchars</i>
     * @return the <i>escaped</i> string
     * 
     * @throws InvalidLocatorException
     *             if <i>path</i> is <code>true</code> and total UTF-8 length >
     *             254 or has embedded nulls
     * 
     * @see #UNRESERVED
     * @see #PCHARS
     */
    private static String escape(String str, boolean dot, boolean path) throws InvalidLocatorException
    {
        char[] chars = str.toCharArray();
        StringBuffer sb = new StringBuffer(chars.length + 2); // expect a
                                                              // sequence
        boolean escaped = false;
        int length = 0;

        for (int i = 0; i < chars.length; ++i)
        {
            char c = chars[i];

            if (UNRESERVED.get(c) || (dot && '.' == c) || (path && PCHARS.get(c)))
            {
                // Don't escape
                sb.append(c);
                ++length;
            }
            else
            {
                if (path && (c == 0 || c == 0xc080))
                    throw new InvalidLocatorException("Embedded null in '" + str + "'");

                // Escape char values using the UTF-8 encoding
                if (c > 0 && c < 0x80)
                {
                    sb.append('%');
                    if (c <= 0xF) sb.append('0'); // obligatory two hex digits
                    sb.append(Integer.toHexString(c));
                }
                else
                {
                    // this character has a special encoding in UTF-8
                    String string = new String(new char[] { c });
                    byte bytes[];
                    try
                    {
                        bytes = string.getBytes("UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        // encoding failed. In this case just put in the char
                        // value.
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
                    length += bytes.length;
                }
                escaped = true;
            }
        }
        if (path && length > 253) throw new InvalidLocatorException("Path is too long: " + str);

        // Don't bother creating another String if no escaping done
        return escaped ? sb.toString() : str;
    }

    /**
     * The set of <i>unreserved</i> characters, minus '.'.
     * 
     * <pre>
     * UNRESERVED = alphanum | mark_not_dot
     * 
     * alphanum = alpha | digit
     * alpha = lowalpha | upalpha
     * lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k"
     *          | "l" | "m" | "n" | "o" | "p" | "q" | "r" | "s" | "t" | "u"
     *          | "v" | "w" | "x" | "y" | "z"
     * upalpha = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K"
     *         | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
     *         | "V" | "W" | "X" | "Y" | "Z"
     * mark_not_dot = "-" | "_" | "!" | "~" | "*" | "'" | "(" | ")"
     * </pre>
     * 
     * @see #escape(String, boolean, boolean)
     */
    private static final java.util.BitSet UNRESERVED;

    /**
     * The set of otherwise reserved chars available for path segments.
     * 
     * <pre>
     * PCHARS = &quot;:&quot; | &quot;@&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot; | &quot;$&quot; | &quot;,&quot;
     * </pre>
     * 
     * @see #escape(String, boolean, boolean)
     */
    private static final java.util.BitSet PCHARS;

    /**
     * Initializes a <code>BitSet</code> according to the given array.
     */
    private static java.util.BitSet bitset(char[] array)
    {
        java.util.BitSet set = new java.util.BitSet();

        for (int i = 0; i < array.length; ++i)
            set.set(array[i]);

        return set;
    }

    static
    {
        // Initialize PCHARS
        PCHARS = bitset(new char[] { ':', '@', '&', '=', '+', '$', ',', ';', '/' // segment/param
                                                                                 // separators
        });

        // Initialize UNRESERVED
        UNRESERVED = bitset(new char[] { '-', '_', '!', '~', '*', '\'', '(', ')' });
        for (char c = 'a'; c <= 'z'; ++c)
        {
            UNRESERVED.set(c);
            UNRESERVED.set(Character.toUpperCase(c));
        }
        for (char c = '0'; c <= '9'; ++c)
        {
            UNRESERVED.set(c);
        }
    }

    /**
     * Sets the sourceID.
     * 
     * @param sourceId
     *            the sourceID
     * @throws InvalidLocatorException
     *             if sourceID is larger than 24-bit.
     */
    private void setSourceId(int sourceId) throws InvalidLocatorException
    {
        if (sourceId < 0 || sourceId > 0xffffff)
            throw new InvalidLocatorException("sourceID (" + sourceId + ") is not valid.");

        this.sourceId = sourceId;
    }

    /**
     * Sets the service name.
     * 
     * @param serviceName
     *            the service name
     */
    private void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    /**
     * Sets the frequency, program number, and modulation format variables.
     * <ul>
     * <li>If the frequency is -1, then this is oobfdc, and we don't care what
     * the modulation format is.
     * <li>Program number must fit in 16 bits.
     * <li>Modulation format is defined by ANSISCTE652002DVS234 table 5.9.
     * </ul>
     * 
     * @param frequency
     * @param programNumber
     * @param modulationFormat
     * 
     * @throws InvalidLocatorException
     *             if programNumber is larger than 16-bit or modulationFormat is
     *             invalid.
     * 
     * @see #setModulation
     */
    private void setFrequencyProgramModulation(int frequency, int programNumber, int modulationFormat)
            throws InvalidLocatorException
    {
        if ((programNumber & 0xFFFF0000) != 0 || (programNumber == 0x0))
        {
            throw new InvalidLocatorException("programNumber (" + programNumber + ") is not valid.");
        }

        // SCTE 65 stipulates that the frequency be in the range 0 to 4095.875
        // Mhz inclusive.
        // We also allow a value of -1 to indicate OOB. We do not need to check
        // whether the
        // frequency exceeds 4095875000 because such a value is beyond the range
        // of a 32 bit
        // signed integer.
        if (frequency < -1)
        {
            throw new InvalidLocatorException("frequency (" + frequency + ") is not valid.");
        }
        this.programNumber = programNumber;
        this.frequency = frequency;

        // oobfdc: we don't care what the modulationFormat is
        if (frequency == -1)
        {
            return;
        }

        setModulation(modulationFormat);
    }

    /**
     * Sets the frequency and modulation format (for a transport locator).
     * <ul>
     * <li>Frequency must be greater than zero (oobfdc is not allowed).
     * <li>Modulation format is defined by ANSISCTE652002DVS234 table 5.9.
     * </ul>
     * 
     * @param frequency
     * @param modulationFormat
     * 
     * @throws InvalidLocatorException
     *             if frequency is less than zero or modulationFormat is
     *             invalid.
     * 
     * @see #setModulation
     */
    private void setFrequencyModulation(int frequency, int modulationFormat) throws InvalidLocatorException
    {
        // Specifying OOB is not accepted
        if (frequency < 0) throw new InvalidLocatorException("Invalid frequency " + frequency);
        this.frequency = frequency;

        setModulation(modulationFormat);
    }

    /**
     * Sets the modulation variable. From SCTE 65 (Table 5-9) the following are
     * supported:
     * <table border>
     * <tr>
     * <th>modulation_format</th>
     * <th>meaning</th>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>unknown: The modulation format is unknown.</td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>QPSK: The modulation format is QPSK (Quadrature Phase Shift Keying).</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>BPSK: The modulation format is BPSK (Binary Phase Shift Keying).</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>OQPSK: The modulation format is offset QPSK.</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>VSB 8: The modulation format is 8-level VSB (Vestigial Sideband).</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>VSB 16: The modulation format is 16-level VSB.</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>QAM 16: Modulation format 16-level Quadrature Amplitude Modulation
     * (QAM).</td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td>QAM 32: 32-level QAM</td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td>QAM 64: 64-level QAM</td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td>QAM 80: 80-level QAM</td>
     * </tr>
     * <tr>
     * <td>10</td>
     * <td>QAM 96: 96-level QAM</td>
     * </tr>
     * <tr>
     * <td>11</td>
     * <td>QAM 112: 112-level QAM</td>
     * </tr>
     * <tr>
     * <td>12</td>
     * <td>QAM 128: 128-level QAM</td>
     * </tr>
     * <tr>
     * <td>13</td>
     * <td>QAM 160: 160-level QAM</td>
     * </tr>
     * <tr>
     * <td>14</td>
     * <td>QAM 192: 192-level QAM</td>
     * </tr>
     * <tr>
     * <td>15</td>
     * <td>QAM 224: 224-level QAM</td>
     * </tr>
     * <tr>
     * <td>16</td>
     * <td>QAM 256: 256-level QAM</td>
     * </tr>
     * <tr>
     * <td>17</td>
     * <td>QAM 320: 320-level QAM</td>
     * </tr>
     * <tr>
     * <td>18</td>
     * <td>QAM 384: 384-level QAM</td>
     * </tr>
     * <tr>
     * <td>19</td>
     * <td>QAM 448: 448-level QAM</td>
     * </tr>
     * <tr>
     * <td>20</td>
     * <td>QAM 512: 512-level QAM</td>
     * </tr>
     * <tr>
     * <td>21</td>
     * <td>QAM 640: 640-level QAM</td>
     * </tr>
     * <tr>
     * <td>22</td>
     * <td>QAM 768: 768-level QAM</td>
     * </tr>
     * <tr>
     * <td>23</td>
     * <td>QAM 896: 896-level QAM</td>
     * </tr>
     * <tr>
     * <td>24</td>
     * <td>QAM 1024: 1024-level QAM</td>
     * </tr>
     * <tr>
     * <td>25-31</td>
     * <td>Reserved</td>
     * </tr>
     * 
     * Basically, a 5-bit value.
     * <p>
     * 
     * For ATGW, another value of 255 is supported to indicate NTSC analog
     * tuning.
     * 
     * @param modulationFormat
     * @throws InvalidLocatorException
     *             if the modulation is invalid
     */
    private void setModulation(int modulationFormat) throws InvalidLocatorException
    {
        // accept full 5-bit modulationFormat plus -1 and 255 (for ATG-W)
        if (modulationFormat != -1
        // FIXME: remove ATG-W temp support for modformat==255 indicating NTSC
        // analog
                && modulationFormat != 255 && (modulationFormat & ~0x1F) != 0)
        {
            throw new InvalidLocatorException("modulationFormat (" + modulationFormat + ") is not valid.");
        }

        if (modulationFormat == 0xFF && this.programNumber == -1)
        {
            // For analog if program number is unspecified, set it to a positive
            // number
            // currently stack cannot handle negative program numbers for analog
            this.programNumber = 1;
        }
        this.modulationFormat = modulationFormat;
    }

    /**
     * Sets the stream types. Stream type is specified in an unsigned 8 bit
     * field in the PMT. So anything out of that range is invalid.
     * 
     * @param streamType
     * @throws InvalidLocatorException
     *             if any stream types are invalid
     * 
     * @see #setLanguageCodes(String[])
     * @see #setIndexes(int[])
     */
    private void setStreamTypes(short[] streamType) throws InvalidLocatorException
    {
        this.streamType = streamType;
        if (streamType != null)
        {
            for (int i = 0; i < streamType.length; ++i)
            {
                if ((streamType[i] & ~0xFF) != 0)
                    throw new InvalidLocatorException("streamType[" + i + "] is out of range: " + streamType[i]);
            }
        }
    }

    /**
     * Sets the ISO language codes.
     * 
     * @param languageCodes
     *            the array of 3-letter language codes
     * @throws InvalidLocatorException
     *             if an invalid language code is specified.
     */
    private void setLanguageCodes(String[] languageCodes) throws InvalidLocatorException
    {
        if (languageCodes == null) return;
        for (int i = 0; i < languageCodes.length; ++i)
        {
            if (languageCodes[i] != null)
            {
                if (languageCodes[i].length() != 3)
                    throw new InvalidLocatorException("Invalid language code '" + languageCodes[i] + "'");
                languageCodes[i] = languageCodes[i].toLowerCase(); // canonical
                                                                   // form
            }
        }

        this.languageCodes = languageCodes;
    }

    /**
     * Sets the indexes.
     * <ul>
     * <li>The maximum size of a PMT is 0x400 bytes.
     * <li>The "header" of a PMT is 12 bytes.
     * <li>The minimum size of each stream loop is 40 bytes.
     * <li>The CRC32 takes up 4 bytes.
     * </ul>
     * As such, <code>0x400 - 12 - 4</code> is the maximum size of the stream
     * loop. With a minimum size of 40 bytes per stream, that's 25 potential
     * streams. The largest index is thus 24.
     * 
     * @param index
     *            array of indexes (matches up with stream types)
     * 
     * @throws InvalidLocatorException
     *             if invalid stream indices are given.
     * 
     * @see #setStreamTypes(short[])
     */
    private void setIndexes(int[] index) throws InvalidLocatorException
    {
        if (index != null)
        {
            for (int i = 0; i < index.length; ++i)
            {
                // A PMT will probably never have more than 24 elementary
                // streams, but OCAP doesn't
                // specifically set a cap on the index.
                // if ( (index[i] < -1) || (index[i] > 24) )
                if (index[i] < -1)
                    throw new InvalidLocatorException("index[" + i + "]=" + index[i] + " is out-of-range");
            }
        }

        this.index = index;
    }

    /**
     * Sets the event ID. Event IDs must fit into 14 bits.
     * 
     * @param eventId
     * @throws InvalidLocatorException
     *             if an invalid event id is given.
     */
    private void setEventId(int eventId) throws InvalidLocatorException
    {
        if (eventId != -1 && (eventId & ~0x3FFF) != 0)
            throw new InvalidLocatorException("eventID=" + eventId + " is out-of-range");

        this.eventId = eventId;
    }

    /**
     * Sets the PIDs. PIDs must fit into 13 bits.
     * 
     * @param pid
     *            array of PIDs
     * @throws InvalidLocatorException
     *             if invalid PIDs are given.
     */
    private void setPIDs(int[] pid) throws InvalidLocatorException
    {
        if (pid == null) return;
        for (int i = 0; i < pid.length; ++i)
        {
            if ((pid[i] & ~0x1FFF) != 0)
                throw new InvalidLocatorException("pid[" + i + "]=" + pid[i] + " is out-of-range");
        }

        this.pid = pid;
    }

    /**
     * Sets the component names.
     * 
     * @param componentName
     */
    private void setComponentNames(String[] componentName)
    {
        this.componentName = componentName;
    }

    /**
     * Sets the component tags.
     * 
     * @param componentTag
     */
    private void setComponentTags(int[] componentTag) throws InvalidLocatorException
    {
        if (componentTag != null)
        {
            for (int i = 0; i < componentTag.length; ++i)
            {
                if ((componentTag[i] & ~0xFF) != 0)
                    throw new InvalidLocatorException("componentTag[" + i + "] is out of range: " + componentTag[i]);
            }
        }
        this.componentTag = componentTag;
    }

    /**
     * Sets the path segments. Checks for embedded nulls and proper length are
     * performed during the {@link #escape(String) escaping} or
     * {@link LocatorParser#unescape unescaping} process.
     * 
     * @param pathSegments
     * @throws InvalidLocatorException
     *             if path segments are invalid.
     */
    private void setPathSegments(String pathSegments) throws InvalidLocatorException
    {
        if (pathSegments == null) return;

        // Note: expect escape/unescape routines to catch invalid
        // length/contents
        if (pathSegments.startsWith("/"))
            throw new InvalidLocatorException("path (" + pathSegments + ") is not valid");

        this.pathSegments = pathSegments;
    }

    /**
     * This method returns a source_id value of the OCAP URL represented by this
     * OcapLocator instance.
     * 
     * @return a source_id value of the OCAP URL represented by this OcapLocator
     *         instance. If the OCAP URL that is specified to construct an
     *         OcapLocator instance doesn't include it, -1 returns.
     * 
     */
    public int getSourceID()
    {
        return sourceId;
    }

    /**
     * This method returns a service_name value of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a service_name value of the OCAP URL represented by this
     *         OcapLocator instance. If the OCAP URL that is specified to
     *         construct an OcapLocator instance doesn't include it, null
     *         returns.
     */
    public String getServiceName()
    {
        return serviceName;
    }

    /**
     * This method returns a frequency value, in hertz, of the OCAP URL
     * represented by this OcapLocator instance.
     * 
     * @return a frequency value, in hertz, of the OCAP URL represented by this
     *         OcapLocator instance. If the OCAP URL that is specified to
     *         construct an OcapLocator instance doesn't include it or the
     *         locator is OOB, -1 is returned. If the getProgramNumber method
     *         returns a value other than -1, the locator is OOB.
     */
    public int getFrequency()
    {
        return frequency;
    }

    /**
     * This method returns a value representing a modulation_format as specified
     * in SCTE 65. A modulation_format value of 0xFF indicates an NTSC analog
     * video format.
     * 
     * @return a value representing the modulation format. If the OCAP URL that
     *         is specified to construct an OcapLocator instance doesn't include
     *         it or -1 was passed in as the modulation format, -1 is returned.
     *         When the locator contains a frequency term and ths method returns
     *         a -1, a default modulation format value of QAM256 is implied.
     */
    public int getModulationFormat()
    {
        return modulationFormat;
    }

    /**
     * This method returns a program_number value of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a program_number value of the OCAP URL represented by this
     *         OcapLocator instance. If the OCAP URL that is specified to
     *         construct an OcapLocator instance doesn't include it, -1 returns.
     */
    public int getProgramNumber()
    {

        if (this.modulationFormat == 0xFF)
        {
            return -1;
        }

        return programNumber;
    }

    /**
     * This method returns a stream_type value of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a stream_type value of the OCAP URL represented by this
     *         OcapLocator instance. The order of stream_types is same as
     *         specified in the constructor. If the OCAP URL that is specified
     *         to construct an OcapLocator instance doesn't include it, a zero
     *         length array returns.
     */
    public short[] getStreamTypes()
    {
        if (streamType == null) streamType = new short[0];
        return streamType;
    }

    /**
     * This method returns an ISO_639_language_code value of the OCAP URL
     * represented by this OcapLocator instance.
     * 
     * @return an ISO_639_language_code value of the OCAP URL represented by
     *         this OcapLocator instance. The order of ISO_639_language_code is
     *         same as specified in the constructor. If the OCAP URL that is
     *         specified to construct an OcapLocator instance doesn't include
     *         any language codes, a zero length array returns. If the OCAP URL
     *         that is specified to construct an OcapLocator instance includes
     *         any language codes, an array is returned that is the same length
     *         as that returned by getStreamTypes(). Some of the elements in
     *         this array may be null, if no language was specified for the
     *         corresponding stream_type.
     */
    public String[] getLanguageCodes()
    {
        if (languageCodes == null) languageCodes = new String[0];
        return languageCodes;
    }

    /**
     * This method returns an index value of the OCAP URL represented by this
     * OcapLocator instance.
     * 
     * @return an index value of the OCAP URL represented by this OcapLocator
     *         instance. The order of index is same as specified in the
     *         constructor. If the OCAP URL that is specified to construct an
     *         OcapLocator instance doesn't include any indexes, a zero length
     *         array returns. If the OCAP URL that is specified to construct an
     *         OcapLocator instance includes any indexes, an array is returned
     *         that is the same length as that returned by getStreamTypes().
     *         Some of the elements in this array may be -1, if no index was
     *         specified for the corresponding stream_type.
     */
    public int[] getIndexes()
    {
        if (index == null) index = new int[0];
        return index;
    }

    /**
     * This method returns an event_id value of the OCAP URL represented by this
     * OcapLocator instance.
     * 
     * @return an event_id value of the OCAP URL represented by this OcapLocator
     *         instance. If the OCAP URL that is specified to construct an
     *         OcapLocator instance doesn't include it, -1 returns.
     */
    public int getEventId()
    {
        return eventId;
    }

    /**
     * This method returns a PID value of the OCAP URL represented by this
     * OcapLocator instance.
     * 
     * @return a PID value of the OCAP URL represented by this OcapLocator
     *         instance. The order of PID is same as specified in the
     *         constructor. If the OCAP URL that is specified to construct an
     *         OcapLocator instance doesn't include it, a zero length array
     *         returns.
     */
    public int[] getPIDs()
    {
        if (pid == null) pid = new int[0];
        return pid;
    }

    /**
     * This method returns a component_name value of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a component_name value of the OCAP URL represented by this
     *         OcapLocator instance. The order of component_name is same as
     *         specified in the constructor. If the OCAP URL that is specified
     *         to construct an OcapLocator instance doesn't include it, a zero
     *         length array returns.
     */
    public String[] getComponentNames()
    {
        if (componentName == null) componentName = new String[0];
        return componentName;
    }

    /**
     * This method returns a component_tag value of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a component_tag value of the OCAP URL represented by this
     *         OcapLocator instance. The order of component_tags is same as
     *         specified in the constructor. If the OCAP URL that is specified
     *         to construct an OcapLocator instance doesn't include it, a zero
     *         length array returns.
     */
    public int[] getComponentTags()
    {
        if (componentTag == null) componentTag = new int[0];
        return componentTag;
    }

    /**
     * This method returns a path_segments string of the OCAP URL represented by
     * this OcapLocator instance.
     * 
     * @return a path_segments string of the OCAP URL represented by this
     *         OcapLocator instance. If the OCAP URL that is specified to
     *         construct an OcapLocator instance doesn't include it, null
     *         returns.
     */
    public String getPathSegments()
    {
        return pathSegments;
    }

    /**
     * Inner class used to parse a locator string. This is a class so that the
     * data associated with parsing can be globally accessed by the parsing
     * methods, without having to pass parameters around (although I suppose
     * there's nothing wrong with that). It is a separate class (from the
     * OcapLocator) to simplify the "forgetting" of associated data/references.
     * It's a non-static inner class so that it can access the outer class data
     * in order to set it.
     * <p>
     * The class essentially implements a top-down recursive descent parser
     * according the the BNF grammar for the OCAP Locator in the I09-031121
     * version of the spec.
     */
    private class LocatorParser
    {
        private String url;

        private char[] buf;

        private int i;

        LocatorParser(String url)
        {
            this.url = url;
            buf = url.toCharArray();
            i = 0;
        }

        private void assertFailure(String desc) throws InvalidLocatorException
        {
            throw new InvalidLocatorException("Locator Url " + url + " is not valid. " + desc);
        }

        private void assertTrue(String desc, boolean value) throws InvalidLocatorException
        {
            if (!value) assertFailure(desc);
        }

        private void assertFalse(String desc, boolean value) throws InvalidLocatorException
        {
            if (value) assertFailure(desc);
        }

        private void assertLength(int expect) throws InvalidLocatorException
        {
            assertTrue("Ran out of characters", checkLength(expect));
        }

        private boolean checkLength(int expect)
        {
            return i + expect - 1 < buf.length;
        }

        /**
         * Parses the locator string passed in on construction, assigning values
         * to the outer OcapLocator instance.
         * 
         * <pre>
         * ocap_url = ocap_scheme ":" ocap_hier_part
         * ocap_scheme = "ocap"
         * ocap_hier_part = ocap_net_path | ocap_abs_path
         * ocap_net_path = "//" ocap_entity [ ocap_abs_path ]
         * </pre>
         * 
         * @throws InvalidLocatorException
         *             if any parsing errors occur
         */
        public void parse() throws InvalidLocatorException
        {
            assertFalse("Only ocap: locator is accepted", buf.length < 7
                    || !url.substring(0, 6).equalsIgnoreCase("ocap:/"));

            if (buf[6] != '/')
            {
                i = 5;
                ocap_abs_path();
            }
            else
            {
                // start past "ocap://"
                i = 7;

                ocap_entity();
                ocap_abs_path();
            }
        }

        /**
         * <pre>
         * ocap_abs_path = "/" path_segments
         * 
         * path_segments = segment *( "/" segment )
         * segment = *pchar *( ";" param )
         * param = *pchar
         * pchar = unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ","
         * </pre>
         */
        private void ocap_abs_path() throws InvalidLocatorException
        {
            if (i < buf.length)
            {
                assertTrue("Expected a '/' character", buf[i] == '/');
                String path = unescape(buf, i, buf.length - i);
                setPathSegments(path.substring(1));

                // TODO: it would be nice to get rid of the obvious overhead of
                // these checks... (could move into unescape)

                // Check length of pathSegments
                byte[] bytes = path.getBytes();
                assertTrue("Path too long", bytes.length < 255);
                bytes = null;
                // Check for embedded nulls
                char[] chars = path.toCharArray();
                for (int idx = 0; idx < chars.length; ++idx)
                {
                    assertTrue("Embedded null", chars[idx] != 0xc080 && chars[idx] != 0x0000);
                }
            }
        }

        /**
         * <pre>
         * ocap_entity = ocap_service | ocap_service_component | ocap_transport
         * </pre>
         */
        private void ocap_entity() throws InvalidLocatorException
        {
            // Try transport first, if that fails go back to ocap_service
            // [components]
            // TODO: do the more common ocap_service first -- requires making
            // ocap_service "non-destructive"
            if (!ocap_transport())
            {
                ocap_service();
                ocap_service_component();
            }
        }

        /**
         * <pre>
         * ocap_transport = "f=" frequency [".m=" modulation_format]
         * </pre>
         */
        private boolean ocap_transport() throws InvalidLocatorException
        {
            int save = i;
            // f=0x?[.m=?]
            if (checkLength(5) && buf[i] == 'f' && buf[i + 1] == '=')
            {
                i += 2;
                int freq = extract_hex("Frequency expected");
                int mod = -1;
                if (i >= buf.length || buf[i] != '.')
                {
                    setFrequencyModulation(freq, mod);
                    return true;
                }
                ++i;
                if (checkLength(5) // m=0x?
                        && buf[i++] == 'm' && buf[i++] == '=' && isHex())
                {
                    mod = extract_hex("Modulation Format expected");
                    setFrequencyModulation(freq, mod);
                    return true;
                }
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * ocap_service = source_id | service_name | ocap_program
         * ocap_program = "f=" frequency [ "." program_number ] [ ".m=" modulation_format ]
         *              | "oobfdc." program_number
         * </pre>
         */
        private void ocap_service() throws InvalidLocatorException
        {
            // ocap_program
            // f=0x?.?
            if (i + 6 < buf.length && buf[i] == 'f' && buf[i + 1] == '=')
            {
                i += 2;
                int freq = extract_hex("Frequency expected");
                assertLength(4); // .0x????
                assertTrue("Expected a '.' between frequency and programNumber", buf[i] == '.');
                ++i;
                int prog = extract_hex("ProgramNumber expected");

                // .m=0x?
                int mod = -1;
                if (i + 5 < buf.length && buf[i] == '.' && buf[i + 1] == 'm' && buf[i + 2] == '=')
                {
                    i += 3;
                    mod = extract_hex("Modulation Format expected");
                }
                setFrequencyProgramModulation(freq, prog, mod);
            }
            // oobfdc.0x???
            else if (i + 9 < buf.length && buf[i] == 'o' && buf[i + 1] == 'o' && buf[i + 2] == 'b' && buf[i + 3] == 'f'
                    && buf[i + 4] == 'd' && buf[i + 5] == 'c' && buf[i + 6] == '.')
            {
                i += 7;
                int prog = extract_hex("ProgramNumber expected");
                setFrequencyProgramModulation(-1, prog, -1);
            }
            // source_id
            else if (isHex())
            {
                setSourceId(extract_hex("sourceID expected"));
            }
            // service_name
            else if (i + 2 < buf.length && buf[i] == 'n' && buf[i + 1] == '=')
            {
                i += 2;
                service_name();
            }
            else
                assertFailure("Invalid ocap_service specification");
        }

        /**
         * <pre>
         * ocap_service_component = ocap_service [ "." program_elements ] [ ";" event_id ]
         * </pre>
         */
        private void ocap_service_component() throws InvalidLocatorException
        {
            if (i < buf.length && buf[i] == '.')
            {
                ++i;
                program_element();
            }
            if (i < buf.length && buf[i] == ';')
            {
                ++i;
                event_id();
            }
        }

        /**
         * <pre>
         * service_name = 1 * (unreserved_not_dot | escaped)
         * </pre>
         */
        private void service_name() throws InvalidLocatorException
        {
            setServiceName(token(true)); // unescape
        }

        /**
         * <pre>
         * program_elements = language_elements | index_elements | PID_elements | component_elements
         * </pre>
         */
        private void program_element() throws InvalidLocatorException
        {
            if (component_elements() || component_tag_elements() || pid_elements() || index_elements()
                    || language_elements())
            {
                /* does nothing - the point was what was executed up to here. */
            }
        }

        /**
         * <pre>
         * component_elements = "$" component_name * ( "&" component_name )
         * </pre>
         */
        private boolean component_elements() throws InvalidLocatorException
        {
            int save = i;
            if (i < buf.length && buf[i] == '$' && (++i != 0) && i < buf.length)
            {
                Vector comps = new Vector();
                // parse
                do
                {
                    comps.addElement(token(true, true)); // unescape, accept dot
                    // if '&' then loop
                }
                while (i < buf.length && buf[i] == '&' && (++i != 0));
                // save
                String[] array = new String[comps.size()];
                comps.copyInto(array);
                setComponentNames(array);
                return true;
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * component_tag_elements = "@" component_tag * ( "&" component_tag )
         * </pre>
         */
        private boolean component_tag_elements() throws InvalidLocatorException
        {
            int save = i;
            if (i < buf.length && buf[i] == '@' && (++i != 0) && isHex())
            {
                Vector comps = new Vector();
                // parse
                do
                {
                    comps.addElement(new Integer(extract_hex("component tag expected")));
                    // if '&' then loop
                }
                while (i < buf.length && buf[i] == '&' && (++i != 0));
                // save
                int[] array = new int[comps.size()];
                for (int idx = 0; idx < array.length; ++idx)
                    array[idx] = ((Integer) comps.elementAt(idx)).intValue();
                setComponentTags(array);
                return true;
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * PID_elements = "+" PID * ( "&" PID )
         * </pre>
         */
        private boolean pid_elements() throws InvalidLocatorException
        {
            int save = i;
            if (i < buf.length && buf[i] == '+' && (++i != 0) && isHex())
            {
                Vector pids = new Vector();
                // parse
                do
                {
                    pids.addElement(new Integer(extract_hex("pid expected")));
                    // if '&' then loop
                }
                while (i < buf.length && buf[i] == '&' && (++i != 0));
                // save
                int[] array = new int[pids.size()];
                for (int idx = 0; idx < array.length; ++idx)
                {
                    array[idx] = ((Integer) pids.elementAt(idx)).intValue();
                }
                setPIDs(array);
                return true;
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * index_elements = stream_type [ "," index ] * ( "&" stream_type [ "," index ] )
         * </pre>
         */
        private boolean index_elements() throws InvalidLocatorException
        {
            int save = i;
            if (i < buf.length && isHex())
            {
                Vector st = new Vector();
                Vector in = new Vector();
                boolean isIndex = false;
                // parse
                do
                {
                    int type = extract_hex("streamType expected");
                    if ((type & ~0xFFFF) != 0) assertFailure("Out-of-range streamType " + type);
                    st.addElement(new Short((short) type));
                    if (i < buf.length && buf[i] == ',' && (++i != 0))
                    {
                        // [ ',' isHex/extract_hex ]
                        if (!isHex())
                        {
                            if (isIndex) assertFailure("Invalid streamType/index specification");
                            i = save;
                            return false;
                        }
                        isIndex = true;
                        in.addElement(new Integer(extract_hex("index expected")));
                    }
                    else
                        in.addElement(new Integer(-1));
                    // if '&' loop
                }
                while (i < buf.length && buf[i] == '&' && (++i != 0));
                // save -- if wouldn't be all -1 entries
                if (isIndex)
                {
                    int[] indexArray = new int[in.size()];
                    for (int idx = 0; idx < indexArray.length; ++idx)
                    {
                        indexArray[idx] = ((Integer) in.elementAt(idx)).intValue();
                    }
                    setIndexes(indexArray);
                }
                short[] typeArray = new short[st.size()];
                for (int idx = 0; idx < typeArray.length; ++idx)
                {
                    typeArray[idx] = ((Short) st.elementAt(idx)).shortValue();
                }
                setStreamTypes(typeArray);
                return true;
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * language_elements = stream_type [ "," ISO_639_language_code ] * ( "&" stream_type [ "," ISO_639_language_code ] )
         * </pre>
         */
        private boolean language_elements() throws InvalidLocatorException
        {
            int save = i;
            if (i < buf.length && isHex())
            {
                Vector st = new Vector();
                Vector lang = new Vector();
                boolean notIndex = false;
                // parse
                do
                {
                    // check isHex
                    int type = extract_hex("stream_type expected");
                    if ((type & ~0xFFFF) != 0) assertFailure("Out-of-range streamType " + type);
                    st.addElement(new Short((short) type));
                    if (i < buf.length && buf[i] == ',' && (++i != 0))
                    {
                        // [ ',' token ]
                        if (isHex())
                        {
                            if (notIndex) assertFailure("Invalid streamType/lang specification");
                            i = save;
                            return false;
                        }
                        notIndex = true;
                        lang.addElement(token(false)); // don't unescape
                    }
                    else
                        lang.addElement(null);
                    // if '&' loop
                }
                while (i < buf.length && buf[i] == '&' && (++i != 0));
                // save -- if wouldn't be all null entries
                if (notIndex)
                {
                    String[] langArray = new String[lang.size()];
                    lang.copyInto(langArray);
                    setLanguageCodes(langArray);
                }
                short[] typesArray = new short[st.size()];
                for (int idx = 0; idx < typesArray.length; ++idx)
                {
                    typesArray[idx] = ((Short) st.elementAt(idx)).shortValue();
                }
                setStreamTypes(typesArray);
                return true;
            }
            i = save;
            return false;
        }

        /**
         * <pre>
         * event_id = hex_string
         * </pre>
         */
        private void event_id() throws InvalidLocatorException
        {
            setEventId(extract_hex("eventID expected"));
        }

        /**
         * Used to determine if the the input buffer currently points to a
         * hexadecimal token.
         */
        private boolean isHex()
        {
            return i + 2 < buf.length && buf[i] == '0' && Character.toLowerCase(buf[i + 1]) == 'x';
        }

        /**
         * Unescapes the given token and generates a new <code>String</code>.
         * Decodes the given string using the '%' syntax specified by RFC 2396
         * section 2.4.
         * 
         * @param array
         *            the character array containing the token
         * @param start
         *            the offset within the character array of the first
         *            character in the token
         * @param length
         *            the token's length
         * @return an unescaped token
         */
        private String unescape(char[] array, int start, int length) throws InvalidLocatorException
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            String returnString = null;
            boolean convert = false;

            length += start;
            for (int idx = start; idx < length; ++idx)
            {
                char c = array[idx];
                if (c == '%')
                {
                    assertTrue("Too few characters for % escape", idx + 2 < length);
                    convert = true;
                    stream.write((byte) dehex(array[idx + 1], array[idx + 2]));
                    idx += 2;
                }
                else
                {
                    stream.write(c);
                }
            }

            if (!convert)
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

        /**
         * Returns the next token in the input buffer. The <i>dot</i> character
         * is considered a delimeter. This is used to read service_name and
         * language codes.
         * 
         * @param unescape
         *            if <code>true</code> then <i>unescape</i> escape sequences
         */
        private String token(boolean unescape) throws InvalidLocatorException
        {
            return token(unescape, false);
        }

        /**
         * Returns the next token in the input buffer. This is used to read
         * service_name, component_name, and language codes.
         * 
         * @param unescape
         *            if <code>true</code> then <i>unescape</i> escape sequences
         * @param dot
         *            if <code>true</code> then <code>'.'</code> is considered
         *            part of a token (i.e., not a delimeter)
         */
        private String token(boolean unescape, boolean dot) throws InvalidLocatorException
        {
            int save = i;
            assertLength(1); // at least need something!
            LOOP: for (; i < buf.length; ++i)
            {
                switch (buf[i])
                {
                    case '.':
                        if (dot) break;
                    case '/':
                    case ';':
                    case ':':
                    case '$':
                    case '&':
                        break LOOP;
                }
            }
            assertTrue("An empty token was found", i != save);

            return unescape ? unescape(buf, save, i - save) : (new String(buf, save, i - save));
        }

        private static final String hex = "0123456789abcdef";

        /**
         * Parses a hex value from the input buffer. The <code>isHex</code>
         * routine should be called first because this routine will throw an
         * exception if parsing fails.
         * 
         * @param msg
         *            is the message included in the exception upon failure
         */
        private int extract_hex(String msg) throws InvalidLocatorException
        {
            assertTrue(msg, isHex() && hex.indexOf(Character.toLowerCase(buf[i + 2])) >= 0);

            i += 2;

            int value = 0;
            int digit;
            for (; i < buf.length && (digit = hex.indexOf(Character.toLowerCase(buf[i]))) >= 0; ++i)
            {
                value = value * 16 + digit;
            }

            return value;
        }

        /**
         * Parses a 2-digit hex value from the given characters.
         * 
         * @param c1
         *            the most-significant digit
         * @param c0
         *            the least-significant digit
         */
        private char dehex(char c1, char c0) throws InvalidLocatorException
        {
            int char1 = hex.indexOf(Character.toLowerCase(c1));
            int char0 = hex.indexOf(Character.toLowerCase(c0));
            assertTrue("Encountered non-hex character", char1 >= 0 && char0 >= 0);

            return (char) ((char1 * 16) + char0);
        }
    }
}
