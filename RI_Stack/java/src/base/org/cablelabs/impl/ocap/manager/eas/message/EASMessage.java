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

package org.cablelabs.impl.ocap.manager.eas.message;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.tv.service.SIManager;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.cablelabs.ocap.util.ConversionUtil;
import org.cablelabs.ocap.util.string.ATSCMultiString;
import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An immutable instance of this class represents a cable emergency alert
 * message as specified in SCTE 18 2007 "Emergency Alert Messaging for Cable"
 * [SCTE 18].
 * <p>
 * At least the intent was to create immutable instances of this class with
 * private, final fields to protect the integrity of a parsed EAS message.
 * However, to allow subclassing for future protocol versions, the instance
 * fields could not be <code>final</code> and required <code>protected</code>
 * access. So the necessary protection was achieved by moving the EAS message
 * classes to their own package, and ensuring no public mutators are defined.
 * <p>
 * Per SCTE 18, the {@link #EAS_event_ID} field value uniquely identifies a
 * given instance. A new <code>EAS_event_ID</code> is assigned if any field,
 * other than {@link #alert_message_time_remaining}, is changed. Therefore, only
 * <code>EAS_event_ID</code> need be used in equivalency tests (e.g.
 * {@link #compareTo(Object)} and {@link #equals(Object)}, and hash codes (e.g.
 * {@link #hashCode()}.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class EASMessage implements Comparable
{
    private static final Logger log = Logger.getLogger(EASMessage.class);

    /**
     * An immutable instance of this class represents a DSM-CC Data Carousel
     * audio file source extracted from an Audio File Descriptor in the EAS
     * message.
     */
    public class EASAudioFileDataCarouselSource extends EASAudioFileSource
    {
        private final int m_programNumber;

        private final long m_downloadId;

        private final long m_moduleId;

        private final int m_applicationId;

        public EASAudioFileDataCarouselSource(final int audioFormat, final String fileName, final int programNumber,
                final long downloadId, final long moduleId, final int applicationId)
        {
            super(audioFormat, fileName);
            this.m_programNumber = programNumber;
            this.m_downloadId = downloadId;
            this.m_moduleId = moduleId;
            this.m_applicationId = applicationId;
        }

        public int getAudioSource()
        {
            return EASAudioFileSource.OOB_DSMCC_DATA_CAROUSEL;
        }

        public int getProgramNumber()
        {
            return this.m_programNumber;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASAudioFileDataCarouselSource");
            buf.append(": audioFormat=").append(this.m_audioFormat);
            buf.append(": programNumber=").append(this.m_programNumber);
            buf.append("; downloadId=").append(this.m_downloadId);
            buf.append("; moduleId=").append(this.m_moduleId);
            buf.append("; appId=0x").append(Integer.toHexString(this.m_applicationId));
            buf.append("; fileName=").append(this.m_fileName);
            return buf.toString();
        }
    }

    /**
     * An immutable instance of this class represents a DSM-CC Object Carousel
     * audio file source extracted from an Audio File Descriptor in the EAS
     * message.
     */
    public class EASAudioFileObjectCarouselSource extends EASAudioFileSource
    {
        private final int m_programNumber;

        private final long m_carouselId;

        private final int m_applicationId;

        public EASAudioFileObjectCarouselSource(final int audioFormat, final String fileName, final int programNumber,
                final long carouselId, final int applicationId)
        {
            super(audioFormat, fileName);
            this.m_programNumber = programNumber;
            this.m_carouselId = carouselId;
            this.m_applicationId = applicationId;
        }

        public int getAudioSource()
        {
            return EASAudioFileSource.OOB_DSMCC_OBJECT_CAROUSEL;
        }

        public int getCarouselId()
        {
            return (int) this.m_carouselId;
        }

        public int getProgramNumber()
        {
            return this.m_programNumber;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASAudioFileObjectCarouselSource");
            buf.append(": audioFormat=").append(this.m_audioFormat);
            buf.append(": programNumber=").append(this.m_programNumber);
            buf.append("; carouselId=").append(this.m_carouselId);
            buf.append("; appId=0x").append(Integer.toHexString(this.m_applicationId));
            buf.append("; fileName=").append(this.m_fileName);
            return buf.toString();
        }
    }

    /**
     * An immutable instance of this class represents an audio file source
     * extracted from an Audio File Descriptor in the EAS message.
     */
    public abstract class EASAudioFileSource
    {
        public static final int OOB_DSMCC_DATA_CAROUSEL = 0x02;

        public static final int OOB_DSMCC_OBJECT_CAROUSEL = 0x01;

        protected final int m_audioFormat;

        protected final String m_fileName;

        public EASAudioFileSource(final int audioFormat, final String fileName)
        {
            this.m_audioFormat = audioFormat;
            this.m_fileName = fileName;
        }

        public abstract int getAudioSource();

        public String getFileName()
        {
            return this.m_fileName;
        }
    }

    /**
     * An immutable instance of this class represents a descriptor present in
     * the Emergency Alert message.
     */
    public class EASDescriptor
    {
        public static final int INBAND_DETAILS_CHANNEL = 0x00;

        public static final int INBAND_EXCEPTION_CHANNELS = 0x01;

        public static final int AUDIO_FILE = 0x02;

        public static final int ATSC_PRIVATE_INFORMATION = 0xAD;

        public static final int USER_PRIVATE = 0xC0;

        private final byte[] m_descriptor;

        /**
         * Constructs a new instance of the receiver.
         */
        public EASDescriptor(final int tag, final byte[] body)
        {
            this.m_descriptor = new byte[body.length + 2];
            this.m_descriptor[0] = (byte) tag;
            this.m_descriptor[1] = (byte) body.length;
            System.arraycopy(body, 0, this.m_descriptor, 2, body.length);
        }

        /**
         * Returns a copy of the descriptor to maintain the immutability of the
         * class.
         * 
         * @return a byte array containing a copy of the descriptor
         */
        public byte[] getDescriptor()
        {
            return (byte[]) this.m_descriptor.clone();
        }

        /**
         * Returns the descriptor length.
         * 
         * @return an integer representing the descriptor length exclusive of
         *         the tag and length bytes
         */
        public int getLength()
        {
            return this.m_descriptor[1] & 0xFF;
        }

        /**
         * Returns the descriptor tag.
         * 
         * @return an integer representing the descriptor tag
         */
        public int getTag()
        {
            return this.m_descriptor[0] & 0xFF;
        }

        /**
         * Determines if the descriptor is user private for
         * {@link org.ocap.system.EASHandler} notification of alternative audio.
         * The descriptor is private if it's an ATSC Private Information
         * Descriptor (0xAD) or a user private descriptor (0xC0 - 0xFF).
         * 
         * @return <code>true</code> if the descriptor is considered user or
         *         ATSC private
         */
        public boolean isPrivate()
        {
            return (getTag() == EASDescriptor.ATSC_PRIVATE_INFORMATION) ? true
                    : ((getTag() < EASDescriptor.USER_PRIVATE) ? false : true);
        }

        /**
         * Returns a string representation of the receiver.
         * 
         * @return a string representation of the object.
         */
        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASDescriptor");
            buf.append(": tag=0x").append(Integer.toHexString(getTag()));
            buf.append("; private=").append(isPrivate());
            buf.append("; length=").append(this.m_descriptor[1] & 0xFF);
            buf.append("; value={");
            for (int i = 2; i < ((this.m_descriptor[1] & 0xFF) + 2); ++i)
            {
                buf.append((this.m_descriptor[i] & 0xFF) < 0x10 ? "0" : "");
                buf.append(Integer.toHexString(this.m_descriptor[i] & 0xFF));
                buf.append((i == (this.m_descriptor[1] & 0xFF) + 1) ? "}" : ",");
            }
            return buf.toString();
        }
    }

    /**
     * An immutable instance of this class represents an in-band exception
     * service, based on the major/minor channel number pair, for which this
     * Emergency Alert event shall not apply.
     */
    protected class EASInBandExceptionChannels
    {
        /**
         * Two unsigned 10-bit integer fields shall represent, in either
         * two-part or one-part channel number format as defined in ATSC A/65C
         * 6.3.2, the virtual channel number of an exception channel, relative
         * to in-band SI.
         */
        private final int exception_major_channel_number;

        private final int exception_minor_channel_number;

        /**
         * Constructs a new instance of the receiver.
         */
        public EASInBandExceptionChannels(final int exception_major_channel_number,
                final int exception_minor_channel_number)
        {
            this.exception_major_channel_number = exception_major_channel_number & 0x3FF;
            this.exception_minor_channel_number = exception_minor_channel_number & 0x3FF;
        }

        /**
         * Indicates whether the given object is equal to the receiver.
         * 
         * @param obj
         *            the <code>EASInBandExceptionChannels</code> object to test
         *            for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(EASInBandExceptionChannels obj)
        {
            return obj == this
                    || (obj != null && obj.exception_major_channel_number == this.exception_major_channel_number && obj.exception_minor_channel_number == this.exception_minor_channel_number);
        }

        /**
         * Indicates whether the given object is equal to the receiver.
         * 
         * @param obj
         *            the object to test for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(Object obj)
        {
            return (obj instanceof EASInBandExceptionChannels) ? equals((EASInBandExceptionChannels) obj) : false;
        }

        /**
         * Returns a hash code for the receiver.
         */
        public int hashCode()
        {
            int result = 17;
            result = 37 * result + this.exception_major_channel_number;
            result = 37 * result + this.exception_minor_channel_number;
            return result;
        }

        /**
         * Returns a string representation of the receiver.
         * 
         * @return a string representation of the object.
         */
        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASInBandExceptionChannels");
            buf.append(": majorNumber=0x").append(Integer.toHexString(this.exception_major_channel_number));
            buf.append("; minorNumber=0x").append(Integer.toHexString(this.exception_minor_channel_number));
            return buf.toString();
        }
    }

    /**
     * An immutable instance of this class represents an in-band exception
     * service, based on the CEA-542-B RF channel identification number and
     * MPEG-2 program number, for which this Emergency Alert event shall not
     * apply.
     */
    protected class EASInBandExceptionDescriptor
    {
        /**
         * The standard QAM frequency, in hertz, and the unsigned 16-bit MPEG-2
         * program number that identifies the service to be excluded. The
         * frequency is determined from the unsigned 8-bit CEA-542-B RF channel
         * identification number passed into the constructor. The RF channel
         * number is retained for debugging purposes only.
         */
        private final byte exception_RF_channel;

        private final int exception_program_number;

        private final int m_exceptionFrequency;

        /**
         * Constructs a new instance of the receiver.
         * 
         * @throws IllegalArgumentException
         *             if the RF channel number is outside the usable range of
         *             2..158 for cable systems
         */
        public EASInBandExceptionDescriptor(final byte exception_RF_channel, final int exception_program_number)
        {
            this.exception_RF_channel = exception_RF_channel;
            this.exception_program_number = exception_program_number & 0xFFFF;
            this.m_exceptionFrequency = ConversionUtil.rfChannelToFrequency(exception_RF_channel & 0xFF);
        }

        /**
         * Constructs a new instance of the receiver.
         */
        public EASInBandExceptionDescriptor(final int exception_frequency, final int exception_program_number)
        {
            this.exception_RF_channel = 0;
            this.exception_program_number = exception_program_number & 0xFFFF;
            this.m_exceptionFrequency = exception_frequency;
        }

        /**
         * Indicates whether the given object is equal to the receiver, based on
         * the QAM frequency and program number.
         * 
         * @param obj
         *            the <code>EASInBandExceptionDescriptor</code> object to
         *            test for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(EASInBandExceptionDescriptor obj)
        {
            return obj == this
                    || (obj != null && obj.m_exceptionFrequency == this.m_exceptionFrequency && obj.exception_program_number == this.exception_program_number);
        }

        /**
         * Indicates whether the given object is equal to the receiver.
         * 
         * @param obj
         *            the object to test for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(Object obj)
        {
            return (obj instanceof EASInBandExceptionDescriptor) ? equals((EASInBandExceptionDescriptor) obj) : false;
        }

        /**
         * Returns a hash code for the receiver.
         */
        public int hashCode()
        {
            int result = 17;
            result = 37 * result + this.m_exceptionFrequency;
            result = 37 * result + this.exception_program_number;
            return result;
        }

        /**
         * Returns a string representation of the receiver.
         * 
         * @return a string representation of the object.
         */
        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASInBandExceptionDescriptor");
            buf.append(": rfChannel=").append(this.exception_RF_channel & 0xFF);
            buf.append("; qamFrequency=").append(this.m_exceptionFrequency / 1000000);
            buf.append("; programNumber=0x").append(Integer.toHexString(this.exception_program_number));
            return buf.toString();
        }
    }

    /**
     * An immutable instance of this class represents an out-of-band exception
     * service for which this Emergency Alert event shall not apply.
     */
    protected class EASOutOfBandExceptionSourceId
    {
        /**
         * An unsigned 16-bit integer field that shall indicate the Source ID of
         * an analog or digital exception service, relative to out-of-band SI.
         */
        private final int exception_OOB_source_ID;

        /**
         * Constructs a new instance of the receiver.
         */
        public EASOutOfBandExceptionSourceId(final int exception_OOB_source_ID)
        {
            this.exception_OOB_source_ID = exception_OOB_source_ID & 0xFFFF;
        }

        /**
         * Indicates whether the given object is equal to the receiver.
         * 
         * @param obj
         *            the <code>EASOutOfBandExceptionSourceId</code> object to
         *            test for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(EASOutOfBandExceptionSourceId obj)
        {
            return obj == this || (obj != null && obj.exception_OOB_source_ID == this.exception_OOB_source_ID);
        }

        /**
         * Indicates whether the given object is equal to the receiver.
         * 
         * @param obj
         *            the object to test for equality
         * @return <code>true</code> if the two objects are equal; otherwise
         *         <code>false</code>
         */
        public boolean equals(Object obj)
        {
            return (obj instanceof EASOutOfBandExceptionSourceId) ? equals((EASOutOfBandExceptionSourceId) obj) : false;
        }

        /**
         * Returns a hash code for the receiver.
         */
        public int hashCode()
        {
            return 37 * 17 + this.exception_OOB_source_ID;
        }

        /**
         * Returns a string representation of the receiver.
         * 
         * @return a string representation of the object.
         */
        public String toString()
        {
            StringBuffer buf = new StringBuffer("EASOutOfBandExceptionSourceId");
            buf.append(": sourceId=0x").append(Integer.toHexString(this.exception_OOB_source_ID));
            return buf.toString();
        }
    }

    /**
     * An instance of this class is a {@link Comparator} implementation that
     * orders EAS messages based on their event expiration time. If the
     * expiration times are equivalent, then the <code>EAS_event_ID</code>
     * determines the ordering between the events.
     */
    private static class ExpirationTimeComparator implements Comparator, Serializable
    {
        private static final long serialVersionUID = 1734374625567470619L;

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2)
        {
            EASMessage em1 = (EASMessage) o1;
            EASMessage em2 = (EASMessage) o2;
            return (em1.m_eventExpirationTime < em2.m_eventExpirationTime ? -1
                    : (em1.m_eventExpirationTime == em2.m_eventExpirationTime ? em1.compareTo(em2) : 1));
        }
    }

    // Class Constants

    public static final int ALERT_PRIORITY_TEST = 0; // 0

    public static final int ALERT_PRIORITY_LOW = 3; // 1 - 3

    public static final int ALERT_PRIORITY_MEDIUM = 7; // 4 - 7

    public static final int ALERT_PRIORITY_HIGH = 11; // 8 - 11

    public static final int ALERT_PRIORITY_MAXIMUM = 15; // 12 - 15

    public static final String DEFAULT_TEXT_ALERT_FORMAT = "{0}";

    public static final int EA_TABLE_ID = 0xD8;

    public static final int EVENT_ID_UNKNOWN = -1;

    public static final Comparator EXPIRATION_TIME_COMPARATOR = new ExpirationTimeComparator();

    public static final int INDEFINITE_ALERT_MESSAGE_TIME_REMAINING = 0;

    public static final int PROTOCOL_VERSION_0 = 0;

    public static final int PROTOCOL_VERSION_UNKNOWN = -1;

    public static final int SEQUENCE_NUMBER_UNKNOWN = -1;

    private static final int MAXIMUM_ALERT_MESSAGE_TIME_REMAINING = 120;

    private static final long IMMEDIATE_START_TIME = 0L;

    private static final int INDEFINITE_EVENT_DURATION = 0;

    private static final int MAXIMUM_EVENT_DURATION = 6000;

    private static final int MINIMUM_EVENT_DURATION = 15;

    private static final int MAXIMUM_LOCATION_CODE_COUNT = 31;

    private static final int MINIMUM_LOCATION_CODE_COUNT = 1;

    private static final String MPEENV_PARSE_STRICT = "OCAP.eas.parse.strict";

    private static final String MPEENV_TEXT_ALERT_FORMAT = "OCAP.eas.text.alert.format";

    private static final Map ORIGINATOR_CODE_MAP;

    private static final long START_TIME_EPOCH;

    // Class Fields

    /**
     * Tracks the sequence number of the most recently received EAS message for
     * duplicate message detection.
     */
    private static int s_lastReceivedSequenceNumber = EASMessage.SEQUENCE_NUMBER_UNKNOWN;

    /**
     * Indicates whether strict parsing is enabled or not. Lazily-initialized by
     * {@link #strictParsingEnabled()} or explicitly initialized for test
     * purposes by {@link #setStrictParsing(boolean)}.
     * <p>
     * An instance of <code>Boolean</code> is used instead of the primitive
     * <code>boolean</code> so a null value can be used to indicate that the
     * value needs to be retrieved from an MPE environment variable (see
     * {@link #MPEENV_PARSE_STRICT}).
     * <p>
     * Note: enableTV Bugzilla issue #3507 identifies EAS compatibility issues
     * with Time-Warner's SARA application framework in that it does not comply
     * with the SCTE 18 specification. "Lenient" parsing was introduced to allow
     * EAS to process non-compliant messages received through that framework.
     */
    private static Boolean s_strictParsing;

    /**
     * A {@link java.text.MessageFormat}-compatible pattern string that
     * specifies how the scrolling text alert messages are to be formatted for
     * displaying to the user. The default pattern is "{0}" which results in
     * only the {@link #alert_text} field being displayed, in the user-preferred
     * language.
     * <p>
     * This default format can be changed by defining "
     * <code>OCAP.eas.text.alert.format</code>" in the <code>mpeenv.ini</code>
     * configuration file, with the desired alert text format. The initial null
     * value indicates that an attempt should be made to retrieve the value from
     * the MPE environment variable before using the default value.
     * <p>
     * See {@link #getFormattedTextAlert(String[])} for a list of EAS message
     * fields are available for inclusion in the message pattern.
     */
    private static MessageFormat s_textAlertFormat;

    // Class Methods

    static
    {
        // Event start times are relative to January 6, 1980, 00:00 UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(1980, Calendar.JANUARY, 6, 0, 0);
        START_TIME_EPOCH = cal.getTime().getTime();

        // Set up EAS originator code -> text map per FCC Rules, Title 47, Part
        // 11, 11.31(d)
        ORIGINATOR_CODE_MAP = new HashMap(4, 1.0f);
        EASMessage.ORIGINATOR_CODE_MAP.put("EAS", "Cable System");
        EASMessage.ORIGINATOR_CODE_MAP.put("CIV", "Civil authorities");
        EASMessage.ORIGINATOR_CODE_MAP.put("WXR", "National Weather Service");
        EASMessage.ORIGINATOR_CODE_MAP.put("PEP", "Primary Entry Point System");
    }

    /**
     * Creates a new instance of the receiver from a byte array containing the
     * cable emergency alert message.
     * <p>
     * Section filtering has performed section table length and CRC checks
     * before the byte array is passed into this method. Therefore, the section
     * table in the byte array is not garbled by network errors and is no larger
     * than 4096 bytes in length. This doesn't preclude an array count or byte
     * length field from being encoded incorrectly and throwing off section
     * table parsing, but some level of reasonableness can be presumed regarding
     * the array contents.
     * <p>
     * The intent for new protocol versions is that this class is extended, and
     * the <code>switch</code> statement in this method is amended to accept the
     * new version and instantiate the new subclass.
     * 
     * @param oobAlert
     *            is <code>true</code> if section table arrived via an
     *            out-of-band Extended Channel, or <code>false</code> if the
     *            section table arrived via an in-band transport stream
     * @param data
     *            a byte array containing the cable emergency alert message
     *            formatted as a MPEG-2 section table
     * @return a valid EASMessage object that doesn't duplicate the most
     *         recently received EAS message, or null if the message represents a duplicate
     * @throws IllegalArgumentException
     *             if invalid EAS section table data is detected
     * @throws NullPointerException
     *             if <code>data</code> is null
     * @throws IllegalStateException
     *             if the section table_ID is not 0xD8 (cable emergency alert
     *             message). The occurrence of this exception should never
     *             happen and would indicate an issue with the section filtering
     *             implementation if it does.
     */
    public static EASMessage create(final boolean oobAlert, final byte[] data)
    {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        EASMessage message = null;

        try
        {
            int[] fields = EASMessage.parseSectionHeader(stream);
            int sequenceNumber = fields[0];
            int protocolVersion = fields[1];

            if (!EASMessage.updateLastReceivedSequenceNumber(sequenceNumber))
            {
                if (log.isInfoEnabled())
                {
                    log.info("duplicate message received - ignoring - sequence number: " + sequenceNumber);
                }
                return null;
            }

            switch (protocolVersion)
            {
                case EASMessage.PROTOCOL_VERSION_0:
                    //constructor may throw an IllegalArgumentException
                    message = new EASMessage(oobAlert, sequenceNumber, protocolVersion, stream);
                    break;
                default:
                    message = new EASMessage(oobAlert, sequenceNumber, protocolVersion);
                    throw new IllegalArgumentException(" Invalid protocol version:<" + protocolVersion + ">");
            }

            message.checkTransmissionRequirements();
            message.normalizeEventTimes();
            message.logParsedMessage();
            return message;
        }
        catch (EOFException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (IllegalArgumentException e)
        {
            if (message == null)
            {
                throw e;
            }
            throw new IllegalArgumentException(message.formatLogMessage(e.getMessage()));
        }
    }

    /**
     * Returns the sequence number of the last received emergency alert message.
     * 
     * @return the sequence number of the last received emergency alert message,
     *         or <code>UNKNOWN_SEQUENCE_NUMBER</code> if no message has been
     *         received yet
     */
    public static synchronized int getLastReceivedSequenceNumber()
    {
        return EASMessage.s_lastReceivedSequenceNumber;
    }

    /**
     * Resets the most recently received sequence number with an unknown value
     * to nullify duplicate message detection on the next message received.
     */
    public static final synchronized void resetLastReceivedSequenceNumber()
    {
        EASMessage.s_lastReceivedSequenceNumber = EASMessage.SEQUENCE_NUMBER_UNKNOWN;
    }

    /**
     * Retrieves an unsigned 8-bit value from the byte stream.
     * <p>
     * Note: method declared final to flag as an inlining candidate and to
     * prevent subclasses from overriding its behavior.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the next 8-bit value as an <code>int</code> to maintain the
     *         most-significant bit of the byte as a value and not a sign bit
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected static final int parseUnsignedByte(ByteArrayInputStream stream) throws EOFException
    {
        int value = stream.read();
        if (value < 0)
        {
            throw new EOFException("Corrupt section table - premature end of stream");
        }
        return value;
    }

    /**
     * Retrieves an unsigned 16-bit value from the byte stream.
     * <p>
     * Note: method declared final to flag as an inlining candidate and to
     * prevent subclasses from overriding its behavior.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the unsigned 16-bit value as an integer
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected static final int parseUnsignedShort(ByteArrayInputStream stream) throws EOFException
    {
        return (EASMessage.parseUnsignedByte(stream) << 8) | EASMessage.parseUnsignedByte(stream);
    }

    /**
     * Enables/disables strict parsing depending on the value of the given
     * parameter. Intended for testing only.
     * 
     * @param enable
     *            <code>true</code> if strict parsing should be enabled;
     *            <code>false</code> otherwise.
     * @see #strictParsingEnabled()
     */
    protected static synchronized void setStrictParsing(final boolean enable)
    {
        EASMessage.s_strictParsing = new Boolean(enable);
    }

    /**
     * Sets the scrolling text alert message format. Intended for testing only.
     * 
     * @param format
     *            a {@link MessageFormat} pattern string for formatting text
     *            alerts. If <code>null</code>, the default format is restored.
     * @throws IllegalArgumentException
     *             if the pattern is invalid
     */
    protected static synchronized void setTextAlertFormat(String format)
    {
        EASMessage.s_textAlertFormat = new MessageFormat((null != format) ? format
                : EASMessage.DEFAULT_TEXT_ALERT_FORMAT);
    }

    /**
     * Determines if strict parsing is enabled or not. Retrieves the setting
     * from the <code>OCAP.eas.parse.strict</code> MPE environment variable
     * definition if the setting has not yet been defined. Defaults to
     * <code>false</code> if the environment variable is undefined.
     * 
     * @return <code>true</code> if strict parsing is enabled, otherwise
     *         <code>false</code>
     */
    protected static synchronized boolean strictParsingEnabled()
    {
        if (EASMessage.s_strictParsing == null)
        {
            EASMessage.s_strictParsing = Boolean.valueOf(MPEEnv.getEnv(EASMessage.MPEENV_PARSE_STRICT, "false"));
        }

        return EASMessage.s_strictParsing.booleanValue();
    }

    /**
     * Returns the String representation format for displaying text alerts to
     * the user. Retrieves the setting from the
     * <code>OCAP.eas.text.alert.format</code> MPE environment variable if the
     * format has not yet been defined. Defaults to "<code>{0}</code>" if the
     * environment variable is undefined, or the pattern string is invalid.
     * 
     * @return the {@link MessageFormat} pattern string representing the text
     *         alert format
     */
    private static synchronized MessageFormat getTextAlertFormat()
    {
        if (EASMessage.s_textAlertFormat == null)
        {
            try
            {
                String format = MPEEnv.getEnv(EASMessage.MPEENV_TEXT_ALERT_FORMAT, EASMessage.DEFAULT_TEXT_ALERT_FORMAT);
                EASMessage.s_textAlertFormat = new MessageFormat(format);
            }
            catch (IllegalArgumentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid alert text format:<" + e.getMessage() + ">, defaulting to:<{0}>");
                }
                EASMessage.s_textAlertFormat = new MessageFormat(EASMessage.DEFAULT_TEXT_ALERT_FORMAT);
            }
        }

        return EASMessage.s_textAlertFormat;
    }

    /**
     * Parses the cable emergency alert section table header from the byte
     * stream and returns selected fields.
     * <p>
     * If logging is enabled, the header fields are checked for SCTE 18
     * requisite values and any discrepancies logged. No exceptions are thrown
     * in these cases to allow EA messages from down-level head-ends to be
     * processed. However the log messages serve as an indicator as to the
     * compliance level of those head-ends.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return a two integer array containing the sequence number in the first
     *         element and the protocol version in the second element
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached,
     *             indicating a corrupted section table
     * @throws IllegalStateException
     *             if the section table_ID is not 0xD8 (cable emergency alert
     *             message). The occurrence of this exception should never
     *             happen and would indicate an issue with the section filtering
     *             implementation if it does.
     */
    private static int[] parseSectionHeader(ByteArrayInputStream stream) throws EOFException
    {
        int tableId = EASMessage.parseUnsignedByte(stream);
        int sectionLength = EASMessage.parseUnsignedShort(stream);
        int bytesAvailable = stream.available();
        int tableIdExtension = EASMessage.parseUnsignedShort(stream);
        int sequenceNumber = EASMessage.parseUnsignedByte(stream);
        int sectionNumber = EASMessage.parseUnsignedByte(stream);
        int lastSectionNumber = EASMessage.parseUnsignedByte(stream);
        int protocolVersion = EASMessage.parseUnsignedByte(stream);

        // included for completeness, should never occur since this table_ID
        // value is how we got here
        if (tableId != EASMessage.EA_TABLE_ID)
        {
            String message = "SCTE-18: table_ID is not a cable emergency alert message:<0x"
                    + Integer.toHexString(tableId) + ">";
            throw new IllegalStateException(message);
        }

        // no else-ifs constructs as we want to see all non-compliance issues
        if (log.isWarnEnabled())
        {
            String prefix = "SCTE-18[sn=" + ((sequenceNumber >> 1) & 0x1F) + "]: ";

            if ((sectionLength & 0x8000) == 0)
        {
                if (log.isWarnEnabled())
        {
                    log.warn(prefix + "section_syntax_indicator not set");
        }
            }
            if ((sectionLength &= 0xFFF) < 43 || sectionLength > 4093)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "section_length not in range of 43..4093:<" + sectionLength + ">");
                }
            }
            if (sectionLength != bytesAvailable)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "section_length:<" + sectionLength + "> doesn't equal available bytes:<"
                        + bytesAvailable + ">");
                }
            }
            if (tableIdExtension != 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "table_id_extension is non-zero:<0x"
                        + Integer.toHexString(tableIdExtension) + ">");
                }
            }
            if ((sequenceNumber & 0x01) == 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "current_next_indicator is not set");
                }
            }
            if (sectionNumber != 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "section_number is non-zero:<" + sectionNumber + ">");
                }
            }
            if (lastSectionNumber != 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(prefix + "last_section_number is non-zero:<" + lastSectionNumber + ">");
                }
        }
        }

        int[] fields = new int[2];
        fields[0] = (sequenceNumber >> 1) & 0x1F;
        fields[1] = protocolVersion;
        return fields;
    }

    /**
     * Updates the most recently received sequence number with the sequence
     * number of the receiver.
     * 
     * @param sequenceNumber
     *            the byte stream containing the EAS section table to be parsed
     * @return true if the sequence number was updated, false if it was a duplicate
     */
    private static synchronized boolean updateLastReceivedSequenceNumber(final int sequenceNumber)
    {
        if (sequenceNumber == EASMessage.s_lastReceivedSequenceNumber)
        {
            return false;
        }

        EASMessage.s_lastReceivedSequenceNumber = sequenceNumber;
        return true;
    }

    // The following instance fields are defined in order of their appearance in
    // the cable emergency alert message structure, and with the names as
    // specified in [SCTE 18] Section 5 for clarity.

    /**
     * An unsigned 5-bit integer field that is the sequence number of this
     * cable_emergency_alert() message. Receiving devices process the
     * sequence_number in order to detect and discard duplicate transmissions.
     * Duplicates of any alert may be sent to overcome possible message loss due
     * to channel noise.
     */
    private final int sequence_number;

    /**
     * An unsigned 8-bit integer field whose function is to allow, in the
     * future, this table type to carry parameters that may be structured
     * differently than those defined in the current protocol. At present, the
     * only valid value for protocol_version is zero.
     */
    private final int protocol_version;

    /**
     * An unsigned 16-bit integer field that shall identify the particular
     * Emergency Alert event. Each time a new EAS message is distributed
     * throughout the cable system, a new EAS_event_ID shall be assigned.
     */
    protected int EAS_event_ID;

    /**
     * Three characters indicating the entity that originally initiated the EAS
     * activation. Receiving devices process the cable_emergency_alert() message
     * for any value in this field. The code is encoded as three ASCII
     * characters which are defined in 47 C.F.R. 11.31(d) [3] and reprinted in
     * [SCTE 18] Table 2.
     */
    protected String EAS_originator_code;

    /**
     * A sequence of characters indicating the nature of the EAS activation.
     * Receiving devices process the cable_emergency_alert() message for any
     * value in this field. The code shall be encoded as <i>n</i> ASCII
     * characters, where <i>n</i> is given by <code>EAS_event_code_length</code>
     * . The event codes are defined in 47 C.F.R. 11.31(e) [3] and reprinted in
     * [SCTE 18] Table 3.
     */
    protected String EAS_event_code;

    /**
     * A data structure containing a <code>multiple_string_structure()</code>,
     * which represents a short textual representation of the event code for
     * on-screen display. The <code>multiple_string_structure()</code> shall be
     * as defined in ATSC A/65C 6.10.
     */
    protected ATSCMultiString nature_of_activation_text;

    /**
     * An unsigned 8-bit integer field in the range 0 to 120 that shall indicate
     * the time remaining in the alert message, in seconds. The start time of
     * the message shall be defined as the time the last bit of the CRC was
     * received. A value of zero shall indicate an alert message period of
     * indefinite duration.
     */
    protected int alert_message_time_remaining;

    /**
     * An unsigned 32-bit integer field representing the start time of this
     * alert event as the number of seconds since 00 hours UTC, January 6th,
     * 1980, with the count of intervening leap seconds included. The
     * <code>event_start_time</code> may be converted to UTC without use of the
     * <code>GPS_UTC_offset</code> (per A/65C Section 6.1) value indicated in
     * the System Time table. A value of zero shall indicate that the event
     * start time is immediate.
     */
    protected long event_start_time;

    /**
     * An unsigned 16-bit integer field that, when nonzero, represents the
     * number of minutes the alert is expected to last. A value of zero
     * indicates that the event duration is unknown (indefinite). When nonzero,
     * the value of event_duration shall be in the range 15 to 6000 (100 hours).
     */
    protected int event_duration;

    /**
     * An unsigned 4-bit integer field that shall indicate the priority of the
     * alert. Receiving devices treat reserved values of alert_priority the same
     * as the next-highest defined value.
     * <table>
     * <tr valign=bottom>
     * <th><code>alert_priority</code></th>
     * <th>Meaning</th>
     * <th>Audio<br>
     * Required</th>
     * </tr>
     * <tr valign=top>
     * <td align=center>0</td>
     * <td><b>Test message</b>: the alert is discarded (after
     * <code>sequence_number</code> processing) by receiving devices except
     * those designed to acknowledge and process test messages. Priority zero is
     * also used to establish a new sequence number per the
     * <code>sequence_number</code> definition.</td>
     * <td align=center>No</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>1-2</td>
     * <td>[Reserved for future use]</td>
     * <td align=center>&nbsp;</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>3</td>
     * <td><b>Low priority</b>: the alert may be disregarded if processing the
     * alert would interrupt viewing of an access-controlled service, as
     * indicated by the presence of a <code>CA_descriptor()</code> (ISO/IEC
     * 13818-1 2.6.16) in the <code>TS_program_map_section()</code>
     * corresponding to the program.</td>
     * <td align=center>No</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>4-6</td>
     * <td>[Reserved for future use]</td>
     * <td align=center>&nbsp;</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>7</td>
     * <td><b>Medium priority</b>: the alert may be disregarded if processing
     * the alert would interrupt viewing of a pay-per-view or video on demand
     * event.</td>
     * <td align=center>No</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>8-10</td>
     * <td>[Reserved for future use]</td>
     * <td align=center>&nbsp;</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>11</td>
     * <td><b>High priority</b>: the alert is processed unconditionally, but can
     * involve text-only display if no audio is available.</td>
     * <td align=center>No</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>12-14</td>
     * <td>[Reserved for future use]</td>
     * <td align=center>&nbsp;</td>
     * </tr>
     * <tr valign=top>
     * <td align=center>15</td>
     * <td><b>Maximum priority</b>: the alert is processed unconditionally. If
     * audio is available without tuning to the details channel, that audio is
     * substituted for program audio for the duration of the alert message. If
     * audio is not available by means other than by tuning to the details
     * channel, the receiving device acquires the details channel for the
     * duration of the alert message.</td>
     * <td align=center>Yes</td>
     * </tr>
     * </table>
     */
    protected int alert_priority;

    /**
     * An unsigned 16-bit integer field that, when non-zero, shall indicate the
     * Source ID of a virtual channel carrying details relevant to the Emergency
     * Alert, where the Source ID references a virtual channel described in
     * out-of-band SI. Receiving devices disregard this field when out-of-band
     * SI is not available. A value of zero shall indicate that no out-of-band
     * details channel reference is available.
     */
    protected int details_OOB_source_ID;

    /**
     * Two unsigned 10-bit integer fields that shall represent a virtual channel
     * number associated with a details channel, in either two-part or one-part
     * channel number format as defined in ATSC A/65C 6.3.2. Both fields shall
     * be set to zero when no in-band details channel reference is available.
     */
    protected int details_major_channel_number;

    protected int details_minor_channel_number;

    /**
     * An unsigned 16-bit integer field that, when non-zero, shall indicate the
     * Source ID of an audio-only virtual channel providing audio related to the
     * alert event, where the Source ID references a virtual channel described
     * in out-of-band SI. Receiving devices disregard this field when
     * out-of-band SI is not available. When audio_OOB_source_ID is zero, no
     * virtual channel is available to provide related audio.
     */
    protected int audio_OOB_source_ID;

    /**
     * A data structure containing a <code>multiple_string_structure()</code>
     * which shall represent a textual description of the emergency alert for
     * on-screen display. Receiving devices scroll alert text slowly across the
     * top of the video screen, from right to left. The
     * <code>multiple_string_structure()</code> shall be as defined in ATSC
     * A/65C 6.10.
     */
    protected ATSCMultiString alert_text;

    /**
     * The list of {@link EASLocationCode} objects representing region
     * definitions associated with this EAS message.
     */
    protected final ArrayList m_locationCodes;

    /**
     * The list of {@link EASInBandExceptionChannels},
     * {@link EASOutOfBandExceptionSourceId} and
     * {@link EASInBandExceptionDescriptor} objects representing the service
     * exceptions for this EAS Message.
     */
    protected final ArrayList m_exceptions;

    /**
     * The list of {@link EASDescriptor} objects representing the descriptors
     * contained in this EAS message.
     */
    protected final ArrayList m_descriptors;

    // The following instance fields are not contained in the cable emergency
    // alert message, but provide state information for a given EAS message.

    /**
     * If <code>true</code>, a user-private descriptor, a ATSC-private
     * information descriptor, and/or an audio file descriptor is present in the
     * list of descriptors contained in this EAS message.
     */
    protected boolean m_audioDescriptorAvailable;

    /**
     * The list of {@link EASAudioFileDataCarouselSource} and
     * {@link EASAudioFileObjectCarouselSource} objects representing audio file
     * sources that may be used to provide an EAS audio override track.
     */
    protected final ArrayList m_audioFileSources;

    /**
     * If not <code>null</code>, the OCAP locator of the details channel
     * corresponding to {@link #details_OOB_source_ID} for an alert received OOB
     * or corresponding to the first In-Band Details Channel Descriptor for an
     * alert received IB. A <code>null</code> value indicates that a details
     * channel is not available.
     */
    protected OcapLocator m_detailsChannelLocator;

    /**
     * The event expiration time, in milliseconds, derived from the normalized
     * {@link #event_start_time} plus {@link #event_duration} fields, or
     * <code>Long.MAX_VALUE</code> if the duration is indefinite.
     */
    protected long m_eventExpirationTime;

    /**
     * The current system time, in milliseconds, the event was received.
     */
    protected final long m_eventReceivedTime;

    /**
     * The {@link #event_start_time} normalized to the current system's epoch,
     * or the current system time if the start time is immediate.
     */
    protected long m_eventStartTime;

    /**
     * If <code>true</code>, this EAS message arrived via an out-of-band
     * Extended channel. Otherwise this message arrived via an in-band transport
     * stream. This flag is used to determine which fields need to be validated
     * per [SCTE 18] 6, Transmission Requirements (Normative).
     */
    private final boolean m_oobAlert;

    // Constructors

    /**
     * Constructs an instance of the receiver using the given parameters, and
     * default values for the remainder of the fields.
     * 
     * @param oobAlert
     *            is <code>true</code> if section table arrived via an
     *            out-of-band Extended channel, or <code>false</code> if the
     *            section table arrived via an in-band transport stream
     * @param sequenceNumber
     *            the sequence number of this EAS message
     * @param protocolVersion
     *            the protocol version of this EAS message
     */
    protected EASMessage(final boolean oobAlert, final int sequenceNumber, final int protocolVersion)
    {
        this.m_oobAlert = oobAlert;
        this.sequence_number = sequenceNumber;
        this.protocol_version = protocolVersion;
        this.EAS_event_ID = EASMessage.EVENT_ID_UNKNOWN;
        this.EAS_originator_code = null;
        this.EAS_event_code = null;
        this.nature_of_activation_text = null;
        this.alert_message_time_remaining = EASMessage.INDEFINITE_ALERT_MESSAGE_TIME_REMAINING;
        this.event_start_time = EASMessage.IMMEDIATE_START_TIME;
        this.event_duration = EASMessage.INDEFINITE_EVENT_DURATION;
        this.alert_priority = 0;
        this.details_OOB_source_ID = 0;
        this.details_major_channel_number = 0;
        this.details_minor_channel_number = 0;
        this.audio_OOB_source_ID = 0;
        this.alert_text = null;
        this.m_locationCodes = new ArrayList();
        this.m_exceptions = new ArrayList();
        this.m_descriptors = new ArrayList();
        this.m_audioDescriptorAvailable = false;
        this.m_audioFileSources = new ArrayList();
        this.m_detailsChannelLocator = null;
        this.m_eventReceivedTime = System.currentTimeMillis();
        this.m_eventStartTime = this.m_eventReceivedTime;
        this.m_eventExpirationTime = Long.MAX_VALUE;
    }

    /**
     * Constructs a new instance of the receiver formatted for EAS protocol
     * version 0.
     * 
     * @param oobAlert
     *            is <code>true</code> if section table arrived via an
     *            out-of-band Extended channel, or <code>false</code> if the
     *            section table arrived via an in-band transport stream
     * @param sequenceNumber
     *            the sequence number of this EAS message
     * @param protocolVersion
     *            the protocol version of this EAS message
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @throws IllegalArgumentException
     *             if invalid EAS section table data is detected
     */
    protected EASMessage(final boolean oobAlert, final int sequenceNumber, final int protocolVersion,
            ByteArrayInputStream stream)
    {
        this(oobAlert, sequenceNumber, protocolVersion);
        try
        {
            this.EAS_event_ID = EASMessage.parseUnsignedShort(stream);
            this.EAS_originator_code = parseASCIIString(stream, 3);
            this.EAS_event_code = parseASCIIString(stream, EASMessage.parseUnsignedByte(stream));
            this.nature_of_activation_text = (EASMessage.parseUnsignedByte(stream) > 0) ? new ATSCMultiString(stream)
                    : null;
            this.alert_message_time_remaining = parseAlertMessageTimeRemaining(stream);
            this.event_start_time = parseUnsignedInt(stream);
            this.event_duration = parseEventDuration(stream);
            this.alert_priority = EASMessage.parseUnsignedShort(stream) & 0xF;
            this.details_OOB_source_ID = parseDetailsOOBSourceId(stream);
            this.details_major_channel_number = EASMessage.parseUnsignedShort(stream) & 0x3FF; // Note:
                                                                                               // not
                                                                                               // used
                                                                                               // with
                                                                                               // OCAP
            this.details_minor_channel_number = EASMessage.parseUnsignedShort(stream) & 0x3FF; // Note:
                                                                                               // not
                                                                                               // used
                                                                                               // with
                                                                                               // OCAP
            this.audio_OOB_source_ID = EASMessage.parseUnsignedShort(stream);
            this.alert_text = (EASMessage.parseUnsignedShort(stream) > 0) ? new ATSCMultiString(stream) : null;
            parseLocationCodes(stream);
            parseExceptionServices(stream);
            parseDescriptors(stream);
            // ignore the CRC_32 field, it was checked before we got this
            // section table
        }
        catch (EOFException e)
        {
            throw new IllegalArgumentException("Corrupt section table - premature end of stream");
        }
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o)
    {
        EASMessage em = (EASMessage) o;
        return (this.EAS_event_ID < em.EAS_event_ID ? -1 : (this.EAS_event_ID == em.EAS_event_ID ? 0 : 1));
    }

    /**
     * Indicates whether the given object is equal to the receiver. Per SCTE 18,
     * the {@link #EAS_event_ID} field determines uniqueness.
     * 
     * @param obj
     *            the <code>EASMessage</code> object to test for equality
     * @return <code>true</code> if the two objects are equal; otherwise
     *         <code>false</code>
     */
    public boolean equals(EASMessage obj)
    {
        return obj == this || (obj != null && this.EAS_event_ID == obj.EAS_event_ID);
    }

    /**
     * Indicates whether the given object is equal to the receiver.
     * 
     * @param obj
     *            the object to test for equality
     * @return <code>true</code> if the two objects are equal; otherwise
     *         <code>false</code>
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof EASMessage) ? equals((EASMessage) obj) : false;
    }

    /**
     * Formats a string identifying the message by its type (IB or OOB),
     * sequence number and event ID, and then appends the given message.
     * 
     * @param message
     *            the message to log
     * @return a string formatted for a log message
     */
    public String formatLogMessage(String message)
    {
        StringBuffer buf = new StringBuffer(isOutOfBandAlert() ? "OOB" : "IB");
        buf.append(" alert[sequenceNumber: ");
        buf.append(this.sequence_number);
        buf.append(",eventId: ");
        buf.append(this.EAS_event_ID);
        buf.append("]: ");
        buf.append(message);
        return buf.toString();
    }

    /**
     * Returns the alert message time remaining of the receiver, in seconds.
     * This time determines the message display time on the monitor.
     * 
     * @return the alert message time remaining in seconds
     */
    public int getAlertMessageTimeRemaining()
    {
        return this.alert_message_time_remaining;
    }

    /**
     * Returns the alert priority.
     * 
     * @return the alert priority of this EAS message
     */
    public int getAlertPriority()
    {
        return this.alert_priority;
    }

    /**
     * Returns the alert text corresponding to the most preferred language if
     * the alert text is included in the EA message.
     * 
     * @param preferredLanguageCodes
     *            the array of preferred languages from which to select the
     *            string to return. Each element must be a three-character ISO
     *            639-2 language code. The array is assumed to be ordered in
     *            descending order of preference.
     * @return the string corresponding to the most preferred language code, or
     *         an <code>null</code> if there's no alert text or a matching
     *         language is not found or the default language ("eng") is not
     *         found, or the alert text has no strings.
     * @throws IllegalArgumentException
     *             if <code>preferredLanguageCodes</code> is <code>null</code>
     *             or has a zero length.
     * @see ATSCMultiString
     */
    public String getAlertText(final String[] preferredLanguageCodes)
    {
        return (this.alert_text != null) ? this.alert_text.getValue(preferredLanguageCodes) : null;
    }

    /**
     * Returns an unmodifiable list of audio file sources contained in this EAS
     * message. The list may be empty.
     * 
     * @return the unmodifiable list of audio file sources
     */
    public List getAudioFileSources()
    {
        return Collections.unmodifiableList(this.m_audioFileSources);
    }

    /**
     * Returns the source ID of an audio-only virtual channel providing audio
     * related to the alert event.
     * 
     * @return the source ID of an audio-only virtual channel if non-zero;
     *         otherwise 0 is returned indicating that no related audio is
     *         available
     */
    public int getAudioOOBSourceID()
    {
        return (isOutOfBandAlert()) ? this.audio_OOB_source_ID : 0;
    }

    /**
     * Returns an unmodifiable list of descriptors contained in this EAS
     * message. The list may be empty.
     * 
     * @return the unmodifiable list of descriptors
     */
    public List getDescriptors()
    {
        return Collections.unmodifiableList(this.m_descriptors);
    }

    /**
     * Returns an {@link OcapLocator} describing the location of the details
     * channel if the alert message specifies a details channel.
     * <p>
     * If the alert message originated from an out-of-band source, a valid
     * non-zero <code>details_OOB_source_ID</code> indicates the availability of
     * an OOB details channel.
     * <p>
     * If the alert message originated from an in-band source, the presence of a
     * valid In-Band Details Channel Descriptor indicates the availability of an
     * IB details channel.
     * 
     * @return the {@link OcapLocator} of the specified IB or OOB details
     *         channel; otherwise <code>null</code> if a details channel was not
     *         specified
     */
    public OcapLocator getDetailsChannelLocator()
    {
        return this.m_detailsChannelLocator;
    }

    /**
     * Returns the alert expiration time, normalized to the system epoch.
     * 
     * @return the alert expiration time of this EAS message, in milliseconds.
     */
    public long getExpirationTime()
    {
        return this.m_eventExpirationTime;
    }

    /**
     * Returns a text alert message formatted according to the established
     * format (a {@link MessageFormat} pattern). The pattern contains one or
     * more variables that are substituted with EA message field values. The
     * following variables are available for use in the formatted message
     * (<i>italized</i> names are derived from other EAS message fields or the
     * system).
     * <table>
     * <tr>
     * <th><var>variable</var></th>
     * <th>Field Name</th>
     * <th>Field Notes</th>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{0}</code></td>
     * <td><code>alert_text()</code></td>
     * <td>Displayed in the user-preferred language.</td>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{1}</code></td>
     * <td><i>event received time</i></td>
     * <td>Displayed as a localized date/time relative to the system time, the
     * time the EA message was received.</td>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{2}</code></td>
     * <td><code>event_start_time</code></td>
     * <td>Displayed as a localized date/time relative to the system time.</td>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{3}</code></td>
     * <td><i>event expiration time</i></td>
     * <td>Displayed as a localized date/time relative to the system time,
     * derived from (<code>event_start_time</code> and
     * <code>event_duration</code>).</td>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{4}</code></td>
     * <td><code>EAS_originator_code</code></td>
     * <td>Displayed as the equivalent text string, in English only.</td>
     * </tr>
     * <tr valign="top">
     * <td align="center"><code>{5}</code></td>
     * <td><code>nature_of_activation_text()</code></td>
     * <td>Displayed in the user-preferred language.</td>
     * </tr>
     * </table>
     * </p> The "date", "time" and "number" styles are supported in the message
     * pattern. For example:
     * 
     * <pre>
     * {2,date,short} from {2,time} to {3,time}: {0}
     * </pre>
     * 
     * Note that {@link MessageFormat} supports a maximum of nine arguments so
     * not all message fields can be included in the text alert; only those in
     * the above table are available for inclusion in the text alert message.
     * 
     * @param preferredLanguageCodes
     *            the array of preferred languages from which to select the
     *            string to return. Each element must be a three-character ISO
     *            639-2 language code. The array is assumed to be ordered in
     *            descending order of preference.
     * @return the formatted text alert
     * @see MessageFormat
     */
    public String getFormattedTextAlert(final String[] preferredLanguageCodes)
    {
        MessageFormat pattern = EASMessage.getTextAlertFormat();
        String alertText = getAlertText(preferredLanguageCodes);

        // If there is no alert text, then the formatter below will return the string "null".  To
        // prevent this, return an empty string
        if(alertText == null)
        {
            return "";
        }
        
        Object[] arguments = new Object[] { alertText, new Date(this.m_eventReceivedTime),
                new Date(this.m_eventStartTime), new Date(this.m_eventExpirationTime), getOriginatorText(),
                getNatureOfActivationText(preferredLanguageCodes), };

        return pattern.format(arguments);
    }

    /**
     * Returns the nature of activation text corresponding to the most preferred
     * language if nature of activation text is included in the EA message.
     * 
     * @param preferredLanguageCodes
     *            the array of preferred languages from which to select the
     *            string to return. Each element must be a three-character ISO
     *            639-2 language code. The array is assumed to be ordered in
     *            descending order of preference.
     * @return the string corresponding to the most preferred language code, or
     *         an <code>null</code> if there's no nature of activation text or a
     *         matching language is not found or the default language ("eng") is
     *         not found, or the nature of activation text has no strings.
     * @throws IllegalArgumentException
     *             if <code>preferredLanguageCodes</code> is <code>null</code>
     *             or has a zero length.
     * @see ATSCMultiString
     */
    public String getNatureOfActivationText(final String[] preferredLanguageCodes)
    {
        return (this.nature_of_activation_text != null) ? this.nature_of_activation_text.getValue(preferredLanguageCodes)
                : null;
    }

    /**
     * Returns the string equivalent of the <code>EAS_originator_code</code>
     * field, in English.
     * 
     * @return the string equivalent of {@link #EAS_originator_code}, or the
     *         originator code if there's no string equivalent
     */
    public String getOriginatorText()
    {
        String originator = (String) EASMessage.ORIGINATOR_CODE_MAP.get(this.EAS_originator_code.toUpperCase());
        return (originator == null) ? this.EAS_originator_code : originator;
    }

    /**
     * Returns a hash code for the receiver.
     */
    public int hashCode()
    {
        return 37 * 17 + this.EAS_event_ID;
    }

    /**
     * Determines if the emergency alert message has expired based on the
     * {@link #event_start_time} and {@link #event_duration} fields.
     * 
     * @return <code>true</code> if the alert has a finite duration and has
     *         expired; otherwise <code>false</code>
     */
    public boolean isAlertExpired()
    {
        return (this.m_eventExpirationTime != Long.MAX_VALUE && this.m_eventExpirationTime <= System.currentTimeMillis());
    }

    /**
     * Determines if the emergency alert message contains alert text. The
     * determination is based on the presence of at least one language code/text
     * string pair being present in the message. The text string may be empty.
     * <p>
     * No presumptions are made regarding the availability of alert text for a
     * particular ISO 639-2 language code.
     * 
     * @return <code>true</code> if the message potentially contains alert text;
     *         otherwise <code>false</code>
     */
    public boolean isAlertTextAvailable()
    {
        return (null != this.alert_text && 0 != this.alert_text.size());
    }

    /**
     * Determines if the emergency alert message contains a reference to an
     * audio override source. That source could be:
     * <ul>
     * <li>a user-private descriptor</li>
     * <li>an ATSC-private information descriptor</li>
     * <li>an audio file descriptor containing one or more audio sources</li>
     * <li>a non-zero <code>audio_OOB_source_ID</code></li>
     * </ul>
     * No presumptions are made regarding the accessibility or availability of
     * any referenced audio source.</p>
     * 
     * @return <code>true</code> if the message contains a reference to a
     *         details channel; otherwise <code>false</code>
     */
    public boolean isAudioChannelAvailable()
    {
        return (0 != getAudioOOBSourceID() || 0 != this.m_audioFileSources.size() || this.m_audioDescriptorAvailable);
    }

    /**
     * Determines if the emergency alert message contains a reference to a
     * details channel source. The source could be:
     * <ul>
     * <li>a non-zero <code>details_OOB_source_ID</code> and the alert
     * originated from an OOB source</li>
     * <li>an In-Band Details Channel Descriptor and the alert originated from
     * an IB source</li>
     * </ul>
     * No presumptions are made regarding the accessibility or availability of
     * the referenced details channel.</p>
     * 
     * @return <code>true</code> if the message contains a reference to a
     *         details channel; otherwise <code>false</code>
     * @see #getDetailsChannelLocator()
     */
    public boolean isDetailsChannelAvailable()
    {
        return (null != this.m_detailsChannelLocator);
    }

    /**
     * Determines if the given geographic location is included in the list of
     * locations associated with this message.
     * 
     * @param location
     *            the {@link EASLocationCode} instance representing the
     *            geographic location of the receiving device
     * @return <code>true</code> if the location is included in the location
     *         code list; otherwise <code>false</code>
     */
    public boolean isLocationIncluded(final EASLocationCode location)
    {
        return this.m_locationCodes.contains(location);
    }

    /**
     * Determines if the message arrived via an out-of-band POD extended channel
     * or an in-band transport stream.
     * 
     * @return <code>true</code> if the message arrived out-of-band; otherwise
     *         <code>false</code>
     */
    public boolean isOutOfBandAlert()
    {
        return this.m_oobAlert;
    }

    /**
     * Determines if the given OOB service is contained in the exception service
     * list.
     * 
     * @param sourceId
     *            the source ID of an analog or digital service, relative to
     *            out-of-band SI
     * @return <code>true</code> if the service is included in the exception
     *         service list; otherwise <code>false</code>
     */
    public boolean isServiceExcluded(final int sourceId)
    {
        return this.m_exceptions.contains(new EASOutOfBandExceptionSourceId(sourceId));
    }

    /**
     * Determines if the given IB service is contained in the exception service
     * list.
     * 
     * @param frequency
     *            the 6MHz frequency band of the RF carrier for the exception
     *            channel, in hertz
     * @param programNumber
     *            the program number of the exception channel in the
     *            corresponding frequency band
     * @return <code>true</code> if the service is included in the exception
     *         service list; otherwise <code>false</code>
     */
    public boolean isServiceExcluded(final int frequency, final int programNumber)
    {
        return this.m_exceptions.contains(new EASInBandExceptionDescriptor(frequency, programNumber));
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("EASMessage");
        buf.append(": OOB alert=").append(isOutOfBandAlert());
        buf.append("; sequence_number=").append(this.sequence_number);
        buf.append("; protocol_version=").append(this.protocol_version);
        buf.append("; EAS_event_ID=").append(this.EAS_event_ID);
        buf.append("; EAS_originator_code=").append(this.EAS_originator_code);
        buf.append("; EAS_event_code=").append(this.EAS_event_code);
        return buf.toString();
    }

    /**
     * Determines if "strict" parsing is enabled or not. If strict parsing is
     * enabled, the given message is thrown as an
     * <code>IllegalArgumentException</code>. Otherwise, the given message is
     * logged as a WARN-level message.
     * 
     * @param message
     *            the validation error message to log or throw
     * @throws IllegalArgumentException
     *             if strict parsing is enabled
     */
    protected void checkStrictParsing(String message)
    {
        if (EASMessage.strictParsingEnabled())
        {
            throw new IllegalArgumentException(message);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn(formatLogMessage(message));
            }
        }
    }

    /**
     * Checks the EAS message transmission requirements per [SCTE 18] 6 and
     * logs debug-level messages if any of the requirements are not met.
     * Transmission requirements are separate from reception requirements in
     * that they define requirements for transmitting equipment that must to be
     * rolled out by MSOs <i>before</i> similar reception requirements are later
     * placed on receiving equipment. This is to avoid "cart-before-the-horse"
     * issues resulting in EAS events being improperly discarded
     */
    protected void checkTransmissionRequirements()
    {
        if (isOutOfBandAlert())
        { // Out-of-band (OOB) transmission requirements
            if (this.alert_text == null && this.details_OOB_source_ID == 0)
            { // [SCTE 18] 6-3
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage("SCTE-18: OOB alert requires alert text or a details source ID"));
                }
            }
            else if (this.alert_priority > EASMessage.ALERT_PRIORITY_HIGH)
            {
                if (this.details_OOB_source_ID == 0)
                { // [SCTE 18] 6-5
                    if (log.isInfoEnabled())
                    {
                        log.info(formatLogMessage("SCTE-18: OOB maximum priority alert requires a details source ID"));
                    }
                }
                else if (this.alert_text != null && this.audio_OOB_source_ID == 0)
                { // [SCTE 18] 6-7
                    if (log.isInfoEnabled())
                    {
                        log.info(formatLogMessage("SCTE-18: OOB maximum priority alert with alert text requires an audio source ID"));
                    }
                }
            }
        }
        else
        { // In-band (IB) transmission requirements
            if (this.alert_text == null && this.details_major_channel_number == 0
                    && this.details_minor_channel_number == 0)
            { // [SCTE 18] 6-2
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage("SCTE-18: IB alert requires alert text or a details channel"));
                }
            }
            else if (this.alert_priority > EASMessage.ALERT_PRIORITY_HIGH && this.details_major_channel_number == 0
                    && this.details_minor_channel_number == 0)
            { // [SCTE 18] 6-4
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage("SCTE-18: IB maximum priority alert requires a details channel"));
                }
            }
        }
    }

    /**
     * Creates the OCAP locator for the given frequency and MPEG-2 program
     * number of an in-band EAS details channel. The modulation format is
     * determined by querying the SI manager.
     * 
     * @param frequency
     *            the standard QAM frequency, in hertz
     * @param programNumber
     *            the MPEG-2 program number found in the PAT for digital
     *            channels, or 0xFFFF for analog channels
     * @return the OCAP locator for the IB details channel, or <code>null</code>
     *         if the locator could not be constructed (e.g. could not determine
     *         modulation format)
     * @throws InvalidLocatorException
     *             if the parameters to construct the locator specify an invalid
     *             OCAP URL (e.g. invalid frequency or program number)
     */
    protected OcapLocator createIBDetailsChannelLocator(final int frequency, final int programNumber)
            throws InvalidLocatorException
    {
        OcapLocator locator = null;

        if (programNumber == 0xFFFF)
        { // analog channel
            locator = new OcapLocator(frequency, programNumber, 0xFF);
        }
        else
        { // digital channel
            Transport[] transports = SIManager.createInstance().getTransports();
            for (int t = 0; locator == null && t < transports.length; ++t)
            {
                try
                {
                    TransportStream[] streams = ((TransportExt) transports[t]).getTransportStreams();
                    for (int s = 0; locator == null && s < streams.length; ++s)
                    {
                        TransportStreamExt stream = (TransportStreamExt) streams[s];
                        if (frequency == stream.getFrequency())
                        {
                            locator = new OcapLocator(frequency, programNumber, stream.getModulationFormat());
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                    continue; // with next transport
                }
                catch (SIRequestException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                    continue; // with next transport
                }
            }
        }

        return locator;
    }

    /**
     * Logs the parsed EA message to the debug log for execution traces,
     * debugging, and general fault isolation tasks.
     */
    protected void logParsedMessage()
    {
        if (log.isInfoEnabled())
        {
            StringBuffer buf = new StringBuffer("Parsed EASMessage:");
            buf.append("\n OOB alert                    = ").append(isOutOfBandAlert());
            buf.append("\n sequence_number              = ").append(this.sequence_number);
            buf.append("\n protocol_version             = ").append(this.protocol_version);
            buf.append("\n EAS_event_ID                 = ").append(this.EAS_event_ID);
            buf.append("\n EAS_originator_code          = ").append(this.EAS_originator_code).append(": ").append(
                    getOriginatorText());
            buf.append("\n EAS_event_code               = ").append(this.EAS_event_code);
            buf.append("\n nature_of_activation_text    = ").append(getNatureOfActivationText(new String[] { "eng" }));
            buf.append("\n alert_message_time_remaining = ").append(getAlertMessageTimeRemaining()).append(" seconds");
            buf.append("\n event_start_time             = ").append(this.event_start_time);
            buf.append("\n event_duration               = ").append(this.event_duration).append(" minutes");
            buf.append("\n alert_priority               = ").append(getAlertPriority());
            buf.append("\n details_OOB_source_ID        = ").append(this.details_OOB_source_ID);
            buf.append("\n details_major_channel_number = ").append(this.details_major_channel_number);
            buf.append("\n details_minor_channel_number = ").append(this.details_minor_channel_number);
            buf.append("\n audio_OOB_source_ID          = ").append(this.audio_OOB_source_ID);
            buf.append("\n alert_text                   = ").append(getAlertText(new String[] { "eng" }));
            buf.append("\n location_code_count          = ").append(this.m_locationCodes.size());
            for (int i = 0; i < this.m_locationCodes.size(); ++i)
            {
                buf.append("\n  location[").append(i).append("]: ").append(this.m_locationCodes.get(i).toString());
            }
            buf.append("\n exception_count              = ").append(this.m_exceptions.size());
            for (int i = 0; i < this.m_exceptions.size(); ++i)
            {
                buf.append("\n  exception[").append(i).append("]: ").append(this.m_exceptions.get(i).toString());
            }
            buf.append("\n descriptor count             = ").append(this.m_descriptors.size());
            for (int i = 0; i < this.m_descriptors.size(); ++i)
            {
                buf.append("\n  descriptor[").append(i).append("]: ").append(this.m_descriptors.get(i).toString());
            }
            buf.append("\n isAudioChannelAvailable      = ").append(isAudioChannelAvailable());
            buf.append("\n number of audio sources      = ").append(this.m_audioFileSources.size());
            for (int i = 0; i < this.m_audioFileSources.size(); ++i)
            {
                buf.append("\n  audio file source[").append(i).append("]: ").append(
                        this.m_audioFileSources.get(i).toString());
            }
            buf.append("\n m_detailsChannelLocator      = ").append(this.m_detailsChannelLocator);
            buf.append("\n m_eventReceivedTime          = ").append(new Date(this.m_eventReceivedTime));
            buf.append("\n m_eventStartTime             = ").append(new Date(this.m_eventStartTime));
            buf.append("\n m_eventExpirationTime        = ").append(new Date(this.m_eventExpirationTime));
            if (log.isInfoEnabled())
            {
                log.info(buf.toString());
            }
    }
    }

    /**
     * Normalizes the original event start time value, relative to 06-Jan-1980
     * 00:00, to the current system epoch for more convenient use. If the event
     * start time is immediate, the current system time is used as the
     * normalized value.
     * <p>
     * Also determine the event expiration time relative to the current system
     * epoch. If the event duration is indefinite, then the expiration time is
     * is set to <code>Long.MAX_VALUE</code>, indicating an indefinite event
     * duration.
     */
    protected void normalizeEventTimes()
    {
        this.m_eventStartTime = (this.event_start_time == EASMessage.IMMEDIATE_START_TIME) ? System.currentTimeMillis()
                : (this.event_start_time * 1000L) + EASMessage.START_TIME_EPOCH;

        this.m_eventExpirationTime = (this.event_duration == EASMessage.INDEFINITE_EVENT_DURATION) ? Long.MAX_VALUE
                : this.m_eventStartTime + (this.event_duration * 60L * 1000L);
    }

    /**
     * Parses the unsigned 8-bit alert message time remaining from the byte
     * stream.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the alert message time remaining, in seconds, for this alert
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     * @throws IllegalArgumentException
     *             if the alert message time remaining is outside the valid
     *             range of 0..120 and strict parsing is enabled
     */
    protected int parseAlertMessageTimeRemaining(ByteArrayInputStream stream) throws EOFException
    {
        int alertMessageTimeRemaining = EASMessage.parseUnsignedByte(stream);

        if (alertMessageTimeRemaining > EASMessage.MAXIMUM_ALERT_MESSAGE_TIME_REMAINING)
        {
            StringBuffer buf = new StringBuffer("Alert message time remaining is outside the valid range of 0..");
            buf.append(EASMessage.MAXIMUM_ALERT_MESSAGE_TIME_REMAINING).append(":<");
            buf.append(alertMessageTimeRemaining).append(">");
            checkStrictParsing(buf.toString());
        }

        return alertMessageTimeRemaining;
    }

    /**
     * Parses a US-ASCII string of the given length from the byte stream.
     * <p>
     * Note: method declared final to flag as an inlining candidate and to
     * prevent subclasses from overriding its behavior.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @param length
     *            the number of bytes to decode from the byte stream
     * @return the string parsed from the byte stream, or an empty string if
     *         <code>length</code> or available bytes equals 0
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected final String parseASCIIString(ByteArrayInputStream stream, final int length) throws EOFException
    {
        try
        {
            return new String(parseByteArray(stream, length), "US-ASCII");
        }
        catch (UnsupportedEncodingException e) // should not occur in a proper
                                               // JVM port
        {
            throw new IllegalStateException("US-ASCII is not a supported charset name");
        }
    }

    /**
     * Parses an audio file source specification from the byte stream and adds
     * it to the list of audio file sources.
     * 
     * @param stream
     *            the byte stream containing the audio file descriptor
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected void parseAudioFileSource(final ByteArrayInputStream stream) throws EOFException
    {
        String fileName = "";
        int audioFormat = EASMessage.parseUnsignedByte(stream);

        if (0 != (audioFormat & 0x80)) // file_name_present
        {
            audioFormat &= 0x3F;
            fileName = new String(parseByteArray(stream, EASMessage.parseUnsignedByte(stream)));
        }

        int audioSource = EASMessage.parseUnsignedByte(stream);
        switch (audioSource)
        {
            case EASAudioFileSource.OOB_DSMCC_DATA_CAROUSEL:
            {
                int prgNo = EASMessage.parseUnsignedShort(stream); // program_number
                long dwnId = parseUnsignedInt(stream); // download_id
                long modId = parseUnsignedInt(stream); // module_id
                int appId = EASMessage.parseUnsignedShort(stream); // application_id
                this.m_audioFileSources.add(new EASAudioFileDataCarouselSource(audioFormat, fileName, prgNo, dwnId,
                        modId, appId));
                break;
            }
            case EASAudioFileSource.OOB_DSMCC_OBJECT_CAROUSEL:
            {
                int prgNo = EASMessage.parseUnsignedShort(stream); // program_number
                long carId = parseUnsignedInt(stream); // carousel_id
                int appId = EASMessage.parseUnsignedShort(stream); // application_id
                this.m_audioFileSources.add(new EASAudioFileObjectCarouselSource(audioFormat, fileName, prgNo, carId,
                        appId));
                break;
            }
            default:
            {
                if (log.isWarnEnabled())
                {
                    log.warn(formatLogMessage("Disregarding audio source - unsupported source type:<0x" + Integer.toHexString(audioSource)
                                + ">"));
                }
            }
        }
    }

    /**
     * Retrieves a byte array of the given length from the byte stream. No more
     * than the available bytes left will be parsed from the stream.
     * <p>
     * Note: method declared final to flag as an inlining candidate and to
     * prevent subclasses from overriding its behavior.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @param length
     *            the number of bytes to decode from the byte stream
     * @return a byte array of the given length, or consisting of what's left in
     *         the byte stream
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected final byte[] parseByteArray(ByteArrayInputStream stream, final int length) throws EOFException
    {
        byte[] bytes = new byte[Math.min(length, stream.available())];
        if (stream.read(bytes, 0, bytes.length) < 0)
        {
            throw new EOFException("Corrupt section table - premature end of stream");
        }
        return bytes;
    }

    /**
     * Parses the list of descriptors from the byte stream, filtering out
     * unsupported descriptors. The list may be empty.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected void parseDescriptors(ByteArrayInputStream stream) throws EOFException
    {
        int byteCount = EASMessage.parseUnsignedShort(stream) & 0x3FF;
        int length;
        int tag;

        for (int d = 0; byteCount > 0; byteCount -= (length + 2), ++d)
        {
            tag = EASMessage.parseUnsignedByte(stream); // present in every
                                                        // descriptor
            length = EASMessage.parseUnsignedByte(stream); // present in every
                                                           // descriptor
            byte[] value = parseByteArray(stream, length); // varies in every
                                                           // descriptor

            switch (tag)
            {
                case EASDescriptor.INBAND_DETAILS_CHANNEL: // [SCTE 18] 5.1.1
                {
                    if (isOutOfBandAlert())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(formatLogMessage("Descriptor[" + d + "]: Disregarding IB Details Channel Descriptor in OOB alert"));
                        }
                        continue;
                    }

                    try
                    {
                        int qamFrequency = ConversionUtil.rfChannelToFrequency(value[2] & 0xFF);
                        int programNumber = (value[3] & 0xFF) << 8 | (value[4] & 0xFF);
                        OcapLocator locator = createIBDetailsChannelLocator(qamFrequency, programNumber);
                        if (null == this.m_detailsChannelLocator)
                        { // retain first IB details channel descriptor only
                            this.m_detailsChannelLocator = locator;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException e) // TODO:
                                                             // alternative: log
                                                             // a warning &
                                                             // continue w/next
                                                             // descriptor
                    {
                        throw new EOFException(
                                "Corrupt section table - IB details channels descriptor improperly encoded");
                    }
                    catch (IllegalArgumentException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(formatLogMessage("Descriptor[" + d + "]: Disregarding invalid IB details channel - " + e.getMessage()));
                        }
                    }
                    catch (InvalidLocatorException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(formatLogMessage("Descriptor[" + d + "]: Disregarding invalid IB details channel - " + e.getMessage()));
                        }
                    }

                    continue;
                }
                case EASDescriptor.INBAND_EXCEPTION_CHANNELS: // [SCTE 18]
                                                              // 5.1.2
                {
                    if (isOutOfBandAlert())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(formatLogMessage("Descriptor[" + d + "]: Disregarding IB Exception Channels Descriptor in OOB alert"));
                        }
                        continue;
                    }

                    int exceptionChannelCount = value[0] & 0xFF;
                    this.m_exceptions.ensureCapacity(this.m_exceptions.size() + exceptionChannelCount);

                    for (int i = 0, j = 1; i < exceptionChannelCount; ++i, j += 3)
                    {
                        try
                        {
                            byte rfChannel = value[j];
                            int programNumber = (value[j + 1] & 0xFF) << 8 | (value[j + 2] & 0xFF);
                            this.m_exceptions.add(new EASInBandExceptionDescriptor(rfChannel, programNumber));
                        }
                        catch (ArrayIndexOutOfBoundsException e) // TODO:
                                                                 // alternative:
                                                                 // log a
                                                                 // warning &
                                                                 // continue
                                                                 // w/next
                                                                 // descriptor
                        {
                            throw new EOFException(
                                    "Corrupt section table - IB exception channels descriptor improperly encoded");
                        }
                        catch (IllegalArgumentException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(formatLogMessage("Descriptor[" + d + "]: Disregarding invalid IB exception channel - "
                                                        + e.getMessage()));
                            }
                        }
                    }

                    continue;
                }
                case EASDescriptor.AUDIO_FILE: // [SCTE 18] 5.1.3
                {
                    int numberOfAudioSources = value[0] & 0xFF;
                    this.m_audioFileSources.ensureCapacity(this.m_audioFileSources.size() + numberOfAudioSources);

                    for (int i = 0, j = 1; i < numberOfAudioSources; ++i)
                    {
                        int loopLength = value[j++] & 0xFF;
                        parseAudioFileSource(new ByteArrayInputStream(value, j, loopLength));
                        j += loopLength;
                    }

                    this.m_audioFileSources.trimToSize();
                    continue;
                }
                case EASDescriptor.ATSC_PRIVATE_INFORMATION: // [SCTE 18] 5.1.4
                {
                    this.m_audioDescriptorAvailable = true;
                    break;
                }
                default:
                {
                    if (tag < EASDescriptor.USER_PRIVATE) // [SCTE 18] 5.1.4
                                                          // and 7.1-13
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(formatLogMessage("Descriptor[" + d + "] Disregarding unrecognized tag:<0x" + Integer.toHexString(tag)
                                                + ">"));
                        }
                        continue;
                    }

                    this.m_audioDescriptorAvailable = true;
                }
            }

            this.m_descriptors.add(new EASDescriptor(tag, value));
        }

        this.m_descriptors.trimToSize();
    }

    /**
     * Parses the unsigned-bit out-of-band source ID for the details channel. A
     * side-effect is to retain the corresponding OCAP locator if the source ID
     * is non-zero and the alert message originated from an OOB source.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the details channel OOB source ID
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected int parseDetailsOOBSourceId(ByteArrayInputStream stream) throws EOFException
    {
        int sourceId = EASMessage.parseUnsignedShort(stream);

        if (isOutOfBandAlert() && sourceId != 0)
        {
            try
            {
                this.m_detailsChannelLocator = new OcapLocator(sourceId);
            }
            catch (InvalidLocatorException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(formatLogMessage("Disregarding invalid OOB details channel - " + e.getMessage()));
                }
            }
        }

        return sourceId;
    }

    /**
     * Parses the unsigned 16-bit event duration from the byte stream.
     * <p>
     * Note: enableTV Bugzilla issue #3507 identifies EAS compatibility issues
     * with Time-Warner's SARA application framework in that it does not comply
     * with SCTE 18 definitions of <code>event_start_time</code>,
     * <code>event_duration</code> and <code>alert_message_time_remaining</code>
     * field usage. The changes to support that non-OCAP application environment
     * <em>were not carried forward into this implementation</em> (e.g. set
     * <code>event_duration</code> to <code>alert_message_time_remaining</code>
     * if <code>event_duration</code> equals 0. This is one possible location to
     * reinstate those changes if they are required.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the event duration, in minutes
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     * @throws IllegalArgumentException
     *             if the event duration is outside the valid set of {0,
     *             15..6000} and strict parsing is enabled
     */
    protected int parseEventDuration(ByteArrayInputStream stream) throws EOFException
    {
        int eventDuration = EASMessage.parseUnsignedShort(stream);

        if (eventDuration != EASMessage.INDEFINITE_EVENT_DURATION
                && (eventDuration < EASMessage.MINIMUM_EVENT_DURATION || eventDuration > EASMessage.MAXIMUM_EVENT_DURATION))
        {
            StringBuffer buf = new StringBuffer("Event duration is outside the valid set of {");
            buf.append(EASMessage.INDEFINITE_EVENT_DURATION).append(",");
            buf.append(EASMessage.MINIMUM_EVENT_DURATION).append("..");
            buf.append(EASMessage.MAXIMUM_EVENT_DURATION).append("}:<");
            buf.append(eventDuration).append(">");
            checkStrictParsing(buf.toString());
        }

        return eventDuration;
    }

    /**
     * Parses the list of exception services from the byte stream for which this
     * emergency alert message does not apply. The list may be empty.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected void parseExceptionServices(ByteArrayInputStream stream) throws EOFException
    {
        int exceptionCount = EASMessage.parseUnsignedByte(stream);
        this.m_exceptions.ensureCapacity(exceptionCount);

        for (int i = 0; i < exceptionCount; ++i)
        {
            if ((EASMessage.parseUnsignedByte(stream) & 0x80) != 0)
            {
                int majorNumber = EASMessage.parseUnsignedShort(stream); // Note:
                                                                         // not
                                                                         // used
                                                                         // with
                                                                         // OCAP
                int minorNumber = EASMessage.parseUnsignedShort(stream); // Note:
                                                                         // not
                                                                         // used
                                                                         // with
                                                                         // OCAP
                this.m_exceptions.add(new EASInBandExceptionChannels(majorNumber, minorNumber));
            }
            else
            {
                stream.skip(2); // skip reserved field
                int sourceId = EASMessage.parseUnsignedShort(stream);
                this.m_exceptions.add(new EASOutOfBandExceptionSourceId(sourceId));
            }
        }

        this.m_exceptions.trimToSize();
    }

    /**
     * Parses and validates the list of region definitions (location codes)
     * encompassing the alert from the byte stream. At least one entry must be
     * present in the list.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     * @throws IllegalArgumentException
     *             if an invalid location count is detected, or strict parsing
     *             is enabled and the location contains an invalid state, county
     *             or subdivision code
     */
    protected void parseLocationCodes(ByteArrayInputStream stream) throws EOFException
    {
        int locationCount = EASMessage.parseUnsignedByte(stream);
        if (locationCount < EASMessage.MINIMUM_LOCATION_CODE_COUNT
                || locationCount > EASMessage.MAXIMUM_LOCATION_CODE_COUNT)
        {
            StringBuffer buf = new StringBuffer("Location code count outside the valid range of ");
            buf.append(EASMessage.MINIMUM_LOCATION_CODE_COUNT).append("..");
            buf.append(EASMessage.MAXIMUM_LOCATION_CODE_COUNT).append(":<");
            buf.append(locationCount).append(">");
            throw new IllegalArgumentException(buf.toString());
        }

        try
        {
            this.m_locationCodes.ensureCapacity(locationCount);

            for (int i = 0; i < locationCount; ++i)
            {
                byte[] locationCode = parseByteArray(stream, 3);
                EASLocationCode location = new EASLocationCode(locationCode);
                try
                {
                    location.validate();
                }
                catch (IllegalArgumentException e)
                {
                    checkStrictParsing(e.getMessage());
                }
                this.m_locationCodes.add(location);
            }

            this.m_locationCodes.trimToSize();
        }
        catch (IllegalArgumentException e)
        {
            throw new EOFException("Corrupt section table - location code must encoded as a 3-byte array");
        }
    }

    /**
     * Retrieves an unsigned 32-bit value from the byte stream.
     * <p>
     * Note: method declared final to flag as an inlining candidate and to
     * prevent subclasses from overriding its behavior.
     * 
     * @param stream
     *            the byte stream containing the EAS section table to be parsed
     * @return the next 32-bit value as a <code>long</code> to maintain the
     *         most-significant bit of the first byte as a value and not a sign
     *         bit
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             section table were incorrect and caused a buffer overrun.
     */
    protected final long parseUnsignedInt(ByteArrayInputStream stream) throws EOFException
    {
        long value = 0;
        for (int i = 0; i < 4; ++i)
        {
            value = (value << 8) | EASMessage.parseUnsignedByte(stream);
        }
        return value;
    }
}
