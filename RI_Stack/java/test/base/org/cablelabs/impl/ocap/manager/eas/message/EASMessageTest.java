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

/*
 * @author Alan Cohn
 */

package org.cablelabs.impl.ocap.manager.eas.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.ocap.manager.eas.EASManagerImplTest;
import org.cablelabs.ocap.util.ConversionUtil;
import org.cablelabs.ocap.util.string.ATSCMultiString;
import org.ocap.system.EASHandler;

import com.ibm.icu.text.UnicodeCompressor;
import org.cablelabs.impl.util.string.ATSCHuffmanEncodeTable;
import org.cablelabs.impl.util.string.ATSCHuffmanEncoder;

public class EASMessageTest extends TestCase
{
    private final boolean OOBALERT = true;

    private final int SEQUENCENUMBER = 1;

    private final int PROTOCOLVERSION = 0;

    private final boolean OUTOFBAND = true;

    private final boolean INBAND = false;

    private final EASMessageForm easmform = new EASMessageForm(1, // sequence_number
            1, // EAS_event_ID
            "EAS", // EAS_originator_code
            "TOR", // EAS_event_code
            "Tornado Warning", // nature_of_activation_text
            60, // alert_message_time_remaining
            0, // event_start_time
            23, // event_duration
            11, // alert_priority
            3, // details_OOB_source_ID
            1, // details_major_channel_number
            2, // details_minor_channel_number
            5, // audio_OOB_source_ID
            "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
            new EASLocCode[] { new EASLocCode(1, 2, 3) }, // location_codes
            new EASOutOfBandExceptionEntry[] { new EASOutOfBandExceptionEntry(0),
                    new EASOutOfBandExceptionEntry(0xFFFF) }, // out-of-band
                                                              // exceptions
            new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1, 2), new EASInBandExceptionEntry(3, 4) }, // in-of-band
                                                                                                                    // exceptions
            new EASMessageDescriptor[] { new EASMessageDescriptor(inbandExceptionChannelsDesc) }, // descriptors
            null, // multi_string_structure for nature_of_activation_text
            null); // multi_string_structure for alert_text

    /*
     * Simple Constructor Test
     */
    public void testSimpleConstructorCompareTest()
    {
        // protected EASMessage(final boolean oobAlert,
        // final int sequenceNumber,
        // final int protocolVersion)
        EASMessage easm1 = new EASMessage(OOBALERT, SEQUENCENUMBER, PROTOCOLVERSION);
        EASMessage easm2 = new EASMessage(OOBALERT, SEQUENCENUMBER, PROTOCOLVERSION);
        EASMessage easm3 = new EASMessage(OOBALERT, SEQUENCENUMBER + 1, PROTOCOLVERSION);
        easm3.EAS_event_ID++; // make different

        assertEquals("Failed equals test", true, easm1.equals(easm2));
        assertEquals("Failed not equals test", false, easm1.equals(easm3));
        assertEquals("Failed compareTo Exp: 0 Act: " + easm1.compareTo(easm2), 0, easm1.compareTo(easm2));
        assertEquals("Failed compareTo Exp: -1 Act: " + easm1.compareTo(easm3), -1, easm1.compareTo(easm3));
    }

    /*
     * Test strict parsing exception.
     */
    public void testStrictParsingException()
    {
        // must clear any previous EAS message.
        EASMessage.resetLastReceivedSequenceNumber();

        EASMessage easm = new EASMessage(OOBALERT, SEQUENCENUMBER, PROTOCOLVERSION);

        EASMessage.setStrictParsing(false);
        assertFalse("strictParsingEnabled returned true", EASMessage.strictParsingEnabled());
        try
        {
            easm.checkStrictParsing("checkStrictParsing test false");
        }
        catch (IllegalArgumentException e)
        {
            fail("checkStrictParsing IllegalArgumentException caught");
        }

        EASMessage.setStrictParsing(true);
        assertTrue("strictParsingEnabled returned false", EASMessage.strictParsingEnabled());
        try
        {
            easm.checkStrictParsing("checkStrictParsing test true");
            fail("checkStrictParsing IllegalArgumentException not caught");
        }
        catch (IllegalArgumentException e)
        {
        }

        // leave in no strict parsing
        EASMessage.setStrictParsing(false);
        assertFalse("strictParsingEnabled returned true", EASMessage.strictParsingEnabled());
    }

    public void testSequenceNum1()
    {
        // must clear any previous EAS message.
        EASMessage.resetLastReceivedSequenceNumber();

        EASMessageGenerator easmgen = new EASMessageGenerator(easmform);
        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        // examine all fields
        assertTrue("test failure", easmform.equals(easMessage));
        assertEquals("Not sequence number 1 is " + EASMessage.getLastReceivedSequenceNumber(), 1,
                EASMessage.getLastReceivedSequenceNumber());
    }

    public void testInBandMessage()
    {
        // must clear any previous EAS message.
        EASMessage.resetLastReceivedSequenceNumber();

        EASMessageGenerator easmgen = new EASMessageGenerator(easmform);
        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(INBAND, easmsg); // inband message
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        assertEquals("Not sequence number 1 is " + EASMessage.getLastReceivedSequenceNumber(), 1,
                EASMessage.getLastReceivedSequenceNumber());

        // check the descriptor array for inbandExceptionChannelsDesc value.

        assertTrue("isServiceExcluded returned false", easMessage.isServiceExcluded(
                ConversionUtil.rfChannelToFrequency((int) easmform.descriptor_array[0].data[3]),
                (int) easmform.descriptor_array[0].data[4] << 8 | easmform.descriptor_array[0].data[5]));
    }

    public void testEASDuplicateAlert()
    {
        // must clear any previous EAS message.
        EASMessage.resetLastReceivedSequenceNumber();

        EASMessageGenerator easmgen = new EASMessageGenerator(easmform);
        byte[] easmsg = easmgen.generate();

        // Create an EAS Message from binary byte array
        EASMessage message = EASMessage.create(OUTOFBAND, easmsg);
        if (message == null)
        {
            fail("Expected message to not be a duplicate");
            return;
        }
        // Second create should fail
        message = EASMessage.create(OUTOFBAND, easmsg);
        if (message != null)
        {
            fail("Expected message to be a duplicate");
            return;
        }

        assertEquals("Not sequence number 1 is " + EASMessage.getLastReceivedSequenceNumber(), 1,
                EASMessage.getLastReceivedSequenceNumber());
    }

    /*
     * test to process lots of EAS Messages
     */
    public void testGoodEasMessages()
    {
        for (int x = 0; x < goodEasTstMsgs.length; x++)
        {
            // must clear any previous EAS message.
            EASMessage.resetLastReceivedSequenceNumber();

            // Create an EAS Message from binary byte array
            EASMessageGenerator easGen = new EASMessageGenerator(goodEasTstMsgs[x]);
            byte[] bArray = easGen.generate();
            EASMessage easMsg = EASMessage.create(OUTOFBAND, bArray);
            if (easMsg == null)
            {
                fail("Unexpected DuplicateAlertMessageException");
                return;
            }
            // examine all fields
            assertTrue("test failure", goodEasTstMsgs[x].equals(easMsg));
        }
    }

    public void testTextAlertFormat()
    {
        final String testStr = "Run for the hills...";
        final String testStr2 = "{0}\n1#{1}\n2#{2}\n3#{3}\n4#{4}\n5#{5}";

        // clear EAS Message
        EASMessage.resetLastReceivedSequenceNumber();
        EASMessage.setTextAlertFormat(null); // default alert text

        // Create new message
        EASMessageGenerator easmgen = new EASMessageGenerator(easmform);
        byte[] easmsg = easmgen.generate();

        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        // Get alert text from EAS Message
        String EASMalertText = easMessage.getFormattedTextAlert(new String[] { "eng" });

        assertEquals("Default Alert Text default not as expected", easmform.alert_text, EASMalertText);

        // Set new Alert format
        EASMessage.setTextAlertFormat(testStr + EASMessage.DEFAULT_TEXT_ALERT_FORMAT);
        // get new text
        EASMalertText = easMessage.getFormattedTextAlert(new String[] { "eng" });

        assertEquals("Alert Text default not as expected", testStr + easmform.alert_text, EASMalertText);

        EASMessage.setTextAlertFormat(testStr2);
        // get new text
        EASMalertText = easMessage.getFormattedTextAlert(new String[] { "eng" });
        System.out.println(">>>" + EASMalertText); // just show it

        EASMessage.setTextAlertFormat(null); // reset to default
    }

    /******************************************************************
     * The following classes are used to create a byte array of binary EAS
     * message.
     *****************************************************************/
    protected static class EASMessageDescriptor
    {
        public byte[] data;

        public EASMessageDescriptor(byte[] data)
        {
            this.data = data;
        }
    }

    private class EASMessageForm
    {
        /**
         * default empty constructor for an EAS message object
         */
        EASMessageForm()
        {
            this.sequence_number = -1;
            this.EAS_Event_ID = -1;
            this.EAS_originator_code = null;
            this.EAS_event_code = null;
            this.nature_of_activation_text = null;
            this.alert_message_time_remaining = -1;
            this.event_start_time = -1;
            this.event_duration = -1;
            this.alert_priority = -1;
            this.details_OOB_source_ID = -1;
            this.details_major_channel_number = -1;
            this.details_minor_channel_number = -1;
            this.audio_OOB_source_ID = -1;
            this.alert_text = null;
            this.location_code_list = null;
            this.out_of_band_exception_list = null;
            this.in_band_exception_list = null;
            this.private_data_descriptors = null;
            this.multi_nature_text = null;
            this.multi_alert_text = null;
        }

        /**
         * constructor for initializing an EAS message object
         */
        EASMessageForm(int sequence_number, int EAS_Event_ID, String EAS_originator_code, String EAS_event_code,
                String nature_of_activation_text, int alert_message_time_remaining, int event_start_time,
                int event_duration, int alert_priority, int details_OOB_source_ID, int details_major_channel_number,
                int details_minor_channel_number, int audio_OOB_source_ID, String alert_text,

                EASLocCode[] location_codes, EASOutOfBandExceptionEntry[] out_of_band_exceptions,
                EASInBandExceptionEntry[] in_band_exceptions, EASMessageDescriptor[] descriptors,
                MultipleString multi_nature_text, MultipleString multi_alert_text)
        {
            this.sequence_number = sequence_number;
            this.EAS_Event_ID = EAS_Event_ID;
            this.EAS_originator_code = EAS_originator_code;
            this.EAS_event_code = EAS_event_code;
            this.nature_of_activation_text = nature_of_activation_text;
            this.alert_message_time_remaining = alert_message_time_remaining;
            this.event_start_time = event_start_time;
            this.event_duration = event_duration;
            this.alert_priority = alert_priority;
            this.details_OOB_source_ID = details_OOB_source_ID;
            this.details_major_channel_number = details_major_channel_number;
            this.details_minor_channel_number = details_minor_channel_number;
            this.audio_OOB_source_ID = audio_OOB_source_ID;
            this.alert_text = alert_text;

            if (null != location_codes)
            {
                this.location_code_list = new Vector();
                for (int i = 0; i < location_codes.length; i++)
                {
                    this.location_code_list.add(new EASLocCode(location_codes[i].state_code,
                            location_codes[i].county_subdivision, location_codes[i].county_code));
                }
            }

            if (null != out_of_band_exceptions)
            {
                this.out_of_band_exception_list = new Vector();
                for (int i = 0; i < out_of_band_exceptions.length; i++)
                {
                    this.out_of_band_exception_list.add(new EASOutOfBandExceptionEntry(
                            out_of_band_exceptions[i].exception_source_id));
                }
            }

            if (null != in_band_exceptions)
            {
                this.in_band_exception_list = new Vector();
                for (int i = 0; i < in_band_exceptions.length; i++)
                {
                    this.in_band_exception_list.add(new EASInBandExceptionEntry(
                            in_band_exceptions[i].exception_major_channel_number,
                            in_band_exceptions[i].exception_major_channel_number));
                }
            }

            if (multi_alert_text == null)
            {
                this.multi_alert_text = new MultipleString(alert_text);
            }
            else
            {
                this.multi_alert_text = multi_alert_text;
            }

            if (multi_nature_text == null)
            {
                this.multi_nature_text = new MultipleString(nature_of_activation_text);
            }
            else
            {
                this.multi_nature_text = multi_nature_text;
            }

            if (descriptors != null)
            {
                descriptor_array = descriptors;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object) Detail comparison of
         * expected EAS fields
         */
        public boolean equals(Object obj)
        {
            if (null == obj || !(obj instanceof EASMessage)) return false;

            EASMessage easm = (EASMessage) obj;

            assertEquals("EAS_event_ID not same", this.EAS_Event_ID, easm.EAS_event_ID);

            assertEquals("alert_priority not same", this.alert_priority, easm.alert_priority);

            assertEquals("audio_OOB_source_ID not same", this.audio_OOB_source_ID, easm.audio_OOB_source_ID);

            assertEquals("details_major_channel_number not same", this.details_major_channel_number,
                    easm.details_major_channel_number);

            assertEquals("details_minor_channel_number not same", this.details_minor_channel_number,
                    easm.details_minor_channel_number);

            assertEquals("alert_message_time_remaining not same", this.alert_message_time_remaining,
                    easm.alert_message_time_remaining);

            assertEquals("details_OOB_source_ID not same", this.details_OOB_source_ID, easm.details_OOB_source_ID);

            assertEquals("EAS_event_code not same", this.EAS_event_code, easm.EAS_event_code);

            assertEquals("EAS_originator_code not same", this.EAS_originator_code, easm.EAS_originator_code);

            assertEquals("event_duration not same", this.event_duration, easm.event_duration);

            assertEquals("event_start_time not same", (int) this.event_start_time, (int) easm.event_start_time);

            assertTrue("isOutOfBandAlert returned false", easm.isOutOfBandAlert());

            // // test EASHandler call back
            // if( null != this.descriptor_array &&
            // ( (int)this.descriptor_array[0].data[0] >=
            // (int)EASMessage.EASDescriptor.USER_PRIVATE ))
            // {
            // EASHandler eash = new EASHandlerExt(this.descriptor_array);
            // easm.notifyPrivateDescriptor( eash );
            // }

            assertTrue("getExpirationTime not in future", System.currentTimeMillis() < easm.getExpirationTime());

            assertFalse("isAlertExpired returned true", easm.isAlertExpired());

            // test isLocationIncluded method in EASMessage
            for (int x = 0; x < this.location_code_list.size(); ++x)
            {
                EASLocCode lc = (EASLocCode) this.location_code_list.elementAt(x);
                assertTrue("isLocationIncluded returned false", easm.isLocationIncluded(new EASLocationCode(
                        lc.state_code, lc.county_code, lc.county_subdivision)));
            }

            // test isServiceExcluded method in EASMessage for out-of-band
            if (null != out_of_band_exception_list)
            {
                for (int x = 0; x < out_of_band_exception_list.size(); ++x)
                {
                    EASOutOfBandExceptionEntry oobe = (EASOutOfBandExceptionEntry) this.out_of_band_exception_list.elementAt(x);
                    assertTrue("isServiceExcluded returned false for out of band",
                            easm.isServiceExcluded(oobe.exception_source_id));
                }
            }

            if (null != in_band_exception_list)
            {
                for (int x = 0; x < in_band_exception_list.size(); ++x)
                {
                    EASInBandExceptionEntry ibe = (EASInBandExceptionEntry) this.in_band_exception_list.elementAt(x);

                    // the major/minor numbers are NOT frequency/program number
                    // values
                    assertFalse("isServiceExcluded return true for inband", easm.isServiceExcluded(
                            ibe.exception_major_channel_number, ibe.exception_minor_channel_number));
                }
            }

            return true;
        }

        /**
         * sequence number (or version number) of the EA message
         */
        int sequence_number;

        /**
         * identifies the particular EA event. When a new EAS message is
         * distributed, a new EAS_event_ID is assigned.
         */
        int EAS_Event_ID;

        /**
         * identifies the entity that activated the EAS
         */
        String EAS_originator_code;

        /**
         * identifies the nature of the EAS activation
         */
        String EAS_event_code;

        /**
         * a short textual representation of the event code
         */
        String nature_of_activation_text;

        /**
         * time remaining to display the message in seconds
         */
        int alert_message_time_remaining;

        /**
         * represents the start time of the message in seconds
         */
        int event_start_time;

        /**
         * the number of minutes that the alert is expected to last
         */
        int event_duration;

        /**
         * priority of the message
         */
        int alert_priority;

        /**
         * source id of a details channel carrying information relevant to the
         * alert
         */
        int details_OOB_source_ID;

        /**
         * major channel number of the details channel. Ignored when OOB SI is
         * available
         */
        int details_major_channel_number;

        /**
         * minor channel number of the details channel. Ignored when OOB SI is
         * available
         */
        int details_minor_channel_number;

        /**
         * source id of an alertnate audio track for the EA
         */
        int audio_OOB_source_ID;

        /**
         * text description of the EA
         */
        String alert_text;

        /**
         * list of <code>EASLocCode</code> objects representing region
         * definitions associated with this alert.
         */
        Vector location_code_list;

        /**
         * list of <code>EASOutOfBandExceptionEntry</code> objects representing
         * the out-of-band exceptions to this EAS Message.
         */
        Vector out_of_band_exception_list;

        /**
         * list of <code>EASInBandExceptionEntry</code> objects representing the
         * in-band exceptions to this EAS Message.
         */
        Vector in_band_exception_list;

        /**
         * contains all of the private descriptors available in the EA message.
         * Each private descriptor can be accessed through the
         * <code>Enumeration</code> interface.
         */
        Enumeration private_data_descriptors;

        EASMessageDescriptor[] descriptor_array;

        MultipleString multi_alert_text;

        MultipleString multi_nature_text;

    }

    /**
     * represents an out-of-band exception list entry.
     */
    private class EASOutOfBandExceptionEntry
    {
        /**
         * default empty constructor for an EAS out-band exception object
         */
        EASOutOfBandExceptionEntry()
        {
            this.exception_source_id = -1;
        }

        /**
         * constructor for initializing an EAS out-band exception object
         */
        EASOutOfBandExceptionEntry(int exception_source_id)
        {
            this.exception_source_id = exception_source_id;
        }

        /**
         * out-of-band exception source-id
         */
        int exception_source_id;
    }

    /**
     * represents an in-band exception list entry.
     */
    private class EASInBandExceptionEntry
    {
        /**
         * default empty constructor for an EAS in-band exception object
         */
        EASInBandExceptionEntry()
        {
            this.exception_major_channel_number = -1;
            this.exception_minor_channel_number = -1;
        }

        /**
         * constructor for initializing an EAS in-band exception object
         */
        EASInBandExceptionEntry(int exception_major_channel_number, int exception_minor_channel_number)
        {
            this.exception_major_channel_number = exception_major_channel_number;
            this.exception_minor_channel_number = exception_minor_channel_number;
        }

        /**
         * major channel number of the exception entry
         */
        int exception_major_channel_number;

        /**
         * minor channel number of the exception entry
         */
        int exception_minor_channel_number;
    }

    /**
     * represents a region definition in an EAS message
     */
    private class EASLocCode
    {
        /**
         * default empty constructor for an EAS out-band exception object
         */
        EASLocCode()
        {
            this.state_code = -1;
            this.county_code = -1;
            this.county_subdivision = -1;
        }

        /**
         * constructor for initializing an EAS out-band exception object
         */
        EASLocCode(int state_code, int county_subdivision, int county_code)
        {
            this.state_code = state_code;
            this.county_subdivision = county_subdivision;
            this.county_code = county_code;
        }

        /**
         * represents the state or territory affected by the EA
         */
        int state_code;

        /**
         * identifies a county within a state.
         */
        int county_code;

        /**
         * identifies county subdivisions affected by this EA.
         */
        int county_subdivision;
    }

    /**
     * Class used to generate an EAS <code>byte</code> stream.
     */
    protected static class EASMessageGenerator
    {
        private static final int mpegSectionSizeMax = 4096;

        private static final int table_ID = 0xD8;

        private EASMessageForm msg;

        public EASMessageGenerator(EASMessageForm msg)
        {
            this.msg = msg;
        }

        /**
         * Generates a byte array for the message information that's been added
         * to this EASGenerator.
         * 
         * @returns a <code>byte[]</code> containing the binary EAS message MPEG
         *          table section.
         */
        public byte[] generate()
        {
            // compute variable-length sections of the EAS message

            // ASSUME: string only contains ASCII-compatible Unicode characters
            byte[] EAS_originator_code_bytes = ((null != this.msg.EAS_originator_code) && (3 == this.msg.EAS_originator_code.length())) ? this.msg.EAS_originator_code.getBytes()
                    : "   ".getBytes();

            // ASSUME: string only contains ASCII-compatible Unicode characters
            byte[] EAS_event_code_bytes = (null != this.msg.EAS_event_code) ? this.msg.EAS_event_code.getBytes()
                    : new byte[0];
            int EAS_event_code_length = EAS_event_code_bytes.length;

            byte[] nature_of_activation_text_bytes = msg.multi_nature_text.encode();
            int nature_of_activation_text_length = nature_of_activation_text_bytes.length;

            byte[] alert_text_bytes = msg.multi_alert_text.encode();
            int alert_text_length = alert_text_bytes.length;

            int location_code_count = (null != this.msg.location_code_list) ? this.msg.location_code_list.size() : 0;

            int out_of_band_exception_count = (null != this.msg.out_of_band_exception_list) ? this.msg.out_of_band_exception_list.size()
                    : 0;

            int in_band_exception_count = (null != this.msg.in_band_exception_list) ? this.msg.in_band_exception_list.size()
                    : 0;

            int descriptors_length = 0;
            if (msg.descriptor_array != null)
            {
                for (int i = 0; i < msg.descriptor_array.length; i++)
                {
                    descriptors_length += msg.descriptor_array[i].data.length;
                }
            }

            // compute the section length of the EAS message
            // and allocate a byte array for the entire message
            int section_length = 12 + EAS_event_code_length + 1 + nature_of_activation_text_length + 19
                    + alert_text_length + 1 + (3 * location_code_count) + 1
                    + (5 * (out_of_band_exception_count + in_band_exception_count)) + 2 + descriptors_length + 4;

            byte[] easSection = new byte[section_length + 3];

            // fill in the byte array with the EAS message according to the spec
            int i = 0;
            easSection[i++] = (byte) (table_ID);
            easSection[i++] = (byte) ((0xB0) | (section_length >> 8));
            easSection[i++] = (byte) ((section_length & 0xFF));
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) ((0xC1) | ((this.msg.sequence_number & 0x1F) << 1));
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) ((this.msg.EAS_Event_ID >> 8) & 0xFF);
            easSection[i++] = (byte) ((this.msg.EAS_Event_ID & 0xFF));
            System.arraycopy(EAS_originator_code_bytes, 0, easSection, i, 3);
            i += 3;
            easSection[i++] = (byte) (EAS_event_code_length);
            System.arraycopy(EAS_event_code_bytes, 0, easSection, i, EAS_event_code_length);
            i += EAS_event_code_length;
            easSection[i++] = (byte) (nature_of_activation_text_length);
            System.arraycopy(nature_of_activation_text_bytes, 0, easSection, i, nature_of_activation_text_length);
            i += nature_of_activation_text_length;
            easSection[i++] = (byte) (this.msg.alert_message_time_remaining);
            easSection[i++] = (byte) ((this.msg.event_start_time >> 24) & 0xFF);
            easSection[i++] = (byte) ((this.msg.event_start_time >> 16) & 0xFF);
            easSection[i++] = (byte) ((this.msg.event_start_time >> 8) & 0xFF);
            easSection[i++] = (byte) ((this.msg.event_start_time >> 0) & 0xFF);
            easSection[i++] = (byte) ((this.msg.event_duration >> 8) & 0xFF);
            easSection[i++] = (byte) ((this.msg.event_duration >> 0) & 0xFF);
            easSection[i++] = (byte) (0xFF);
            easSection[i++] = (byte) (0xF0 | (this.msg.alert_priority & 0x0F));
            easSection[i++] = (byte) ((this.msg.details_OOB_source_ID >> 8) & 0xFF);
            easSection[i++] = (byte) ((this.msg.details_OOB_source_ID >> 0) & 0xFF);
            easSection[i++] = (byte) (0xFC | ((this.msg.details_major_channel_number >> 8) & 0x03));
            easSection[i++] = (byte) ((this.msg.details_major_channel_number >> 0) & 0xFF);
            easSection[i++] = (byte) (0xFC | ((this.msg.details_minor_channel_number >> 8) & 0x03));
            easSection[i++] = (byte) ((this.msg.details_minor_channel_number >> 0) & 0xFF);
            easSection[i++] = (byte) ((this.msg.audio_OOB_source_ID >> 8) & 0xFF);
            easSection[i++] = (byte) ((this.msg.audio_OOB_source_ID >> 0) & 0xFF);

            easSection[i++] = (byte) ((alert_text_length >> 8) & 0xFF);
            easSection[i++] = (byte) ((alert_text_length >> 0) & 0xFF);
            System.arraycopy(alert_text_bytes, 0, easSection, i, alert_text_length & 0xff);
            i += alert_text_length & 0xff;

            easSection[i++] = (byte) (location_code_count);
            for (int j = 0; j < location_code_count; j++)
            {
                EASLocCode location_code = (EASLocCode) this.msg.location_code_list.elementAt(j);
                easSection[i++] = (byte) (location_code.state_code);
                easSection[i++] = (byte) (((location_code.county_subdivision & 0x0F) << 4) | 0x0C | ((location_code.county_code >> 8) & 0x03));
                easSection[i++] = (byte) ((location_code.county_code & 0xFF));
            }
            easSection[i++] = (byte) (out_of_band_exception_count + in_band_exception_count);
            for (int j = 0; j < out_of_band_exception_count; j++)
            {
                // arbitrarity do the out-of-band exceptions first
                EASOutOfBandExceptionEntry out_of_band_exception = (EASOutOfBandExceptionEntry) this.msg.out_of_band_exception_list.elementAt(j);
                easSection[i++] = (byte) (0x00);
                easSection[i++] = (byte) (0x00);
                easSection[i++] = (byte) (0x00);
                easSection[i++] = (byte) ((out_of_band_exception.exception_source_id >> 8) & 0xFF);
                easSection[i++] = (byte) ((out_of_band_exception.exception_source_id >> 0) & 0xFF);
            }
            for (int j = 0; j < in_band_exception_count; j++)
            {
                // arbitrarity do the in-band exceptions second
                EASInBandExceptionEntry in_band_exception = (EASInBandExceptionEntry) this.msg.in_band_exception_list.elementAt(j);
                easSection[i++] = (byte) (0x80);
                easSection[i++] = (byte) (0xFC | ((in_band_exception.exception_major_channel_number >> 8) & 0x03));
                easSection[i++] = (byte) ((in_band_exception.exception_major_channel_number >> 0) & 0xFF);
                easSection[i++] = (byte) (0xFC | ((in_band_exception.exception_minor_channel_number >> 8) & 0x03));
                easSection[i++] = (byte) ((in_band_exception.exception_minor_channel_number >> 0) & 0xFF);
            }
            easSection[i++] = (byte) (0xFC | ((descriptors_length >> 8) & 0x03));
            easSection[i++] = (byte) ((descriptors_length >> 0) & 0xFF);
            // encode the private descriptors, if any are available
            if (msg.descriptor_array != null)
            {
                for (int j = 0; j < msg.descriptor_array.length; j++)
                {
                    int descriptor_length = msg.descriptor_array[j].data.length;
                    System.arraycopy(msg.descriptor_array[j].data, 0, easSection, i, descriptor_length);
                    i += descriptor_length;
                }
            }
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);
            easSection[i++] = (byte) (0x00);

            // ignoring CRC for now as we assume hardware is handling this below
            // us

            return easSection;
        }

    }

    protected static class MultipleString
    {
        protected MultipleString()
        {

        }

        protected MultipleString(String string)
        {
            // in this case, a multiple_string is initialized with a string
            // entry with
            // one segment, no compression, mode = 0x0, and language
            // code = "eng"
            SegmentEntry segmentEntry = new SegmentEntry(string, 0, 0);
            stringEntries.add(new StringEntry(segmentEntry, "eng"));
        }

        // add a new string entry to the multipleString. The string segment data
        // is
        // represented by the array of segment entries.
        protected MultipleString(SegmentEntry entries[], String languageCode)
        {
            stringEntries.add(new StringEntry(entries, languageCode));
        }

        protected MultipleString(StringEntry entries[])
        {
            for (int i = 0; i < entries.length; i++)
            {
                stringEntries.add(entries[i]);
            }
        }

        protected MultipleString(StringEntry entry)
        {
            stringEntries.add(entry);
        }

        // encodes the multipleString and returns an array of bytes
        protected byte[] encode()
        {
            // for each string entry, call the encode method and aggregate the
            // bytes returned.
            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            // write the number of strings
            writer.write(stringEntries.size());

            for (int i = 0; i < stringEntries.size(); i++)
            {
                StringEntry entry = (StringEntry) stringEntries.elementAt(i);
                entry.encode(writer);
            }
            return writer.toByteArray();
        }

        /**
         * vector of StringEntry instances which make up the
         * multiple_string_structure
         */
        private Vector stringEntries = new Vector();
    }

    protected static class StringEntry
    {
        protected StringEntry(SegmentEntry entries[], String langCode)
        {
            languageCode = langCode;
            segmentEntries = entries;
        }

        protected StringEntry(SegmentEntry entry, String langCode)
        {
            languageCode = langCode;
            segmentEntries = new SegmentEntry[] { entry };
        }

        protected void encode(ByteArrayOutputStream writer)
        {
            byte byteArray[];

            // write out ISO_639_language_code
            try
            {
                byteArray = languageCode.getBytes("ASCII");
            }
            catch (UnsupportedEncodingException e)
            {
                System.out.println("UnsupportedEncodingException thrown when converting lang code string to ascii");
                // get the bytes using the default character set
                byteArray = languageCode.getBytes();

            }
            // write out the language code
            writer.write(byteArray, 0, byteArray.length);
            // write out the number of segments
            writer.write(segmentEntries.length);

            for (int i = 0; i < segmentEntries.length; i++)
            {
                segmentEntries[i].encode(writer);
            }
        }

        private String languageCode;

        private SegmentEntry segmentEntries[];
    }

    protected static class SegmentEntry
    {
        protected SegmentEntry(String string, int compression, int mode)
        {
            // no testing for illegal combinations of mode and compression is
            // done so
            // poorly formed messages can be sent to the parser
            this.compression = compression;
            this.mode = mode;
            this.string = string;
        }

        protected void encode(ByteArrayOutputStream writer)
        {
            byte array[];
            ATSCHuffmanEncoder encoder;
            ATSCHuffmanEncodeTable table;

            writer.write(compression);
            writer.write(mode);

            switch (compression)
            {
                case ATSCMultiString.MSS_COMPRESSION_TYPE_NONE:
                    // no compression
                    switch (mode)
                    {
                        case ATSCMultiString.MSS_MODE_UNICODE_STANDARD_COMPRESSION:
                            // select standard compression scheme for Unicode
                            array = UnicodeCompressor.compress(string);
                            writer.write(array.length);
                            writer.write(array, 0, array.length);
                            break;
                        case ATSCMultiString.MSS_MODE_UNICODE_UTF16:
                            // utf-16 representation
                            int outIndex = 0;
                            byte encodeArray[] = new byte[string.length() * 4];

                            // add the byte-order mark
                            encodeArray[outIndex++] = (byte) 0xfe;
                            encodeArray[outIndex++] = (byte) 0xff;

                            char character;
                            for (int i = 0; i < string.length(); i++)
                            {
                                character = string.charAt(i);
                                encodeArray[outIndex++] = (byte) (character >> 8);
                                encodeArray[outIndex++] = (byte) (character & 0xff);
                            }

                            writer.write(outIndex);
                            writer.write(encodeArray, 0, outIndex);
                            break;
                        default:
                            // catch-all for all the other modes.
                            // in this mode, we write the lower 8 bits into the
                            // message and the upper 8 bits are implied by the
                            // mode.
                            //
                            char c;
                            // write the number of 16-bit chars in the string
                            writer.write(string.length());
                            for (int i = 0; i < string.length(); i++)
                            {
                                c = string.charAt(i);
                                writer.write(c & 0xff);
                            }
                            break;

                    }
                    break;
                case ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_TITLE:
                    // huffman compression
                    try
                    {
                        table = new ATSCHuffmanEncodeTable("/org/cablelabs/impl/util/string/ProgramTitle_HuffmanEncodeTable.txt");
                    }
                    catch (IOException e)
                    {
                        System.out.println("IOException caught on encode table creation");
                        // write a length of zero
                        writer.write(0);
                        return;
                    }
                    encoder = new ATSCHuffmanEncoder();

                    array = encoder.encode(new String[] { string }, table);
                    writer.write(array.length);
                    writer.write(array, 0, array.length);
                    break;
                case ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_DESC:
                    // huffman compression
                    try
                    {
                        table = new ATSCHuffmanEncodeTable("/org/cablelabs/impl/util/string/ProgramDescription_HuffmanEncodeTable.txt");
                    }
                    catch (IOException e)
                    {
                        System.out.println("IOException caught on encode table creation");
                        // write a length of zero
                        writer.write(0);
                        return;
                    }

                    encoder = new ATSCHuffmanEncoder();

                    array = encoder.encode(new String[] { string }, table);
                    writer.write(array.length);
                    writer.write(array, 0, array.length);
                    break;
            }
        }

        private int compression;

        private int mode;

        private String string = null;
    }

    /*
     * EASHandler class that verifies the notifyPrivateDescriptor callback.
     */
    protected class EASHandlerExt implements EASHandler
    {
        EASMessageDescriptor[] m_easmd;

        // Constructor to set expected descriptor byte array
        public EASHandlerExt(EASMessageDescriptor[] easmd)
        {
            m_easmd = easmd;
        }

        public boolean notifyPrivateDescriptor(byte[] descriptor)
        {
            // we don't know the order of the descriptors so check all
            // we know about.
            System.out.println("notifyPrivateDescriptor called with descriptor length " + descriptor.length);
            boolean found = false;
            for (int x = 0; !found && x < m_easmd.length; ++x)
            {
                byte[] expected = m_easmd[x].data;
                boolean diff = false;
                for (int z = 0; !diff && z < descriptor.length; z++)
                {
                    if ((byte) expected[z] != (byte) descriptor[z]) diff = true;
                }
                found = !diff;
            }
            assertTrue("No descriptior match found", found);
            return false; // keep going
        }

        public void stopAudio()
        {
        }
    }

    /***********************************************
     * Class initialization from TestCase
     *************************************************/
    // Constructor
    public EASMessageTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite();
        suite.addTest(new EASMessageTest("testSimpleConstructorCompareTest"));
        suite.addTest(new EASMessageTest("testStrictParsingException"));
        suite.addTest(new EASMessageTest("testSequenceNum1"));
        suite.addTest(new EASMessageTest("testInBandMessage"));
        suite.addTest(new EASMessageTest("testEASDuplicateAlert"));
        suite.addTest(new EASMessageTest("testGoodEasMessages"));
        suite.addTest(new EASMessageTest("testTextAlertFormat"));
        return suite;
    }

    /*
     * Global items used in testing
     */
    // Descriptor array for C0, FF User Private values
    final static protected byte descriptorArray1[] = { (byte) 0xC0, 0x07, 'T', 'W', 'C', (byte) 0xAA, (byte) 0xBB,
            (byte) 0xCC, (byte) 0xDD };

    final static protected byte descriptorArray2[] = { (byte) 0xFF, 0x07, 'S', 'U', 'N', (byte) 0xAB, (byte) 0xCD,
            (byte) 0xEF, (byte) 0x88 };

    final static protected byte inbandExceptionChannelsDesc[] = {
            (byte) EASMessage.EASDescriptor.INBAND_EXCEPTION_CHANNELS, (byte) 4, // byte
                                                                                 // count
            (byte) 1, // list count
            (byte) 6, // frequency
            (byte) 0, (byte) 0x64 // 2 bytes for 16-bit program id
    };

    private final EASMessageForm[] goodEasTstMsgs = {
            new EASMessageForm(1, // sequence_number
                    1, // EAS_event_ID
                    "EAS", // EAS_originator_code
                    "TOR", // EAS_event_code
                    "Tornado Warning", // nature_of_activation_text
                    60, // alert_message_time_remaining
                    0, // event_start_time
                    23, // event_duration
                    11, // alert_priority
                    3, // details_OOB_source_ID
                    0, // details_major_channel_number
                    0, // details_minor_channel_number
                    5, // audio_OOB_source_ID
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(0, 0, 0) }, // location_codes
                    new EASOutOfBandExceptionEntry[] { new EASOutOfBandExceptionEntry(0),
                            new EASOutOfBandExceptionEntry(0xFFFF) }, // out-of-band
                                                                      // exceptions
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1, 1),
                            new EASInBandExceptionEntry(99, 99) }, // in-of-band
                                                                   // exceptions
                    null, // descriptors
                    null, // multi_string_structure for
                          // nature_of_activation_text
                    null // multi_string_structure for alert_text
            ), new EASMessageForm(31, // sequence_number
                    0xffff, // EAS_event_ID
                    "EAS", // EAS_originator_code
                    "TOR", // EAS_event_code
                    "Tornado Warning", // nature_of_activation_text
                    120, // alert_message_time_remaining
                    0xFFFFFFFF, // event_start_time
                    6000, // event_duration
                    15, // alert_priority
                    0xFFFF, // details_OOB_source_ID
                    0x3FF, // details_major_channel_number
                    0x3FF, // details_minor_channel_number
                    0xFFFF, // audio_OOB_source_ID
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(2, 3, 4) }, // location_codes
                    null, // out-of-band exceptions
                    null, // in-of-band exceptions
                    null, // descriptors
                    null, // multi_string_structure for
                          // nature_of_activation_text
                    null // multi_string_structure for alert_text
            ),
            // test the bounds of EAS_event_code and EAS_event_code_length by
            // passing in a string that is
            // 260 bytes long
            new EASMessageForm(
                    31,
                    0xffff,
                    "EAS",
                    "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345",
                    "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(5, 6, 7) }, null, null, null, null, null),

            // test the bounds of the nature_of_activation_text and
            // nature_of_activation_text_length
            // by passing in a string that is 260 bytes long
            new EASMessageForm(
                    31,
                    0xffff,
                    "EAS",
                    "TOR",
                    "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345",
                    120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(8, 9, 10) }, null, null, null, null, null),

            // try a value of zero for alert_message_time_remaining
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 0, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(9, 9, 99) }, null, null, null, null, null),

            // try a value of zero for event_start_time - 0xffffffff has been
            // tested in previous messages
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0, // event_start_time
                    6000, 15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(9, 9, 9) }, null, null, null, null, null),

            // try a value of zero for event_duration
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 0, // event_duration
                    15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(1, 1, 1) }, null, null, null, null, null),

            // try a value of 15 for event_duration (6000 has been previously
            // tested).
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 15, // event_duration
                    15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(2, 2, 2) }, null, null, null, null, null),

            // try a value of 0 for alert_priority. 15 has been previously
            // checked.
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 0, // alert_priority
                    0xFFFF, 0x3FF, 0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(3, 3, 3) }, null, null, null, null, null),

            // try a value for 0 for details_OOB_source_ID, 0xffff was tested
            // previously
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0, // details_OOB_source_ID
                    0x3FF, 0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // try a value of 0 for details_major_channel_number 0x3ff was
            // tested previously
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0, // details
                                                                                                                  // major_channel_number
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // try a value of 0 for details_minor_channel_number 0x3ff was
            // tested previously
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0, // details_minor_channel_number
                    0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // try a value of 0 for audio_OOB_source_ID. 0xFFFF was tested
            // previously
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0, // audio_OOB_source_ID
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // try a string length of zero for alert_text
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // message with 31 location codes. 1 location code had been tested
            // in previous messages.
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999),
                            new EASLocCode(99, 9, 999), new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // message with 1 location code and a state code of 0
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(0, 9, 999) }, null, null, null, null, null),

            // message with 1 location code and a subdivision code of 0
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 0, 999) }, null, null, null, null, null),

            // message with 1 location code and a county code of 0
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF, "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 0) }, null, null, null, null, null),

            // message with an OOB exception entries
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) }, null, null,
                    null, null),

            // message with in-band exception entries
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, new EASInBandExceptionEntry[] {
                            new EASInBandExceptionEntry(1023, 1023), new EASInBandExceptionEntry(2, 2) }, null, null,
                    null),

            // message with in-band and OOB exception entries
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF, 0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(3, 3) }, null, null, null),

            // add a couple private descriptors
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Tornado Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(40, 50) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, null),

            // minimum length message
            new EASMessageForm(31, 0xffff, "EAS", "", "", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF, 0x3FF, 0xFFFF, "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, null, null, null, null, null),

            // add a multistring for nature_of_activation_text with type 2
            // huffman encoding
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    new MultipleString(new SegmentEntry[] { new SegmentEntry("Blizzard Warning", 2, 0) }, "eng"), null),

            // add a multistring with 2 types of huffman encoding for
            // nature_of_activation_text
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning take cover now!", 120, 0xFFFFFFFF, 6000, 15,
                    0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    new MultipleString(new SegmentEntry[] { new SegmentEntry("Blizzard Warning ", 2, 0),
                            new SegmentEntry("take cover ", 1, 0), new SegmentEntry("now!", 0, 0) }, "eng"), null),

            // add a multistring for alert_text with all the various types of
            // huffman encoding
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry("A tornado ", 0, 0),
                            new SegmentEntry("has been spotted ", 1, 0), new SegmentEntry("in Polk county ", 2, 0),
                            new SegmentEntry("at 5:13pm.", 2, 0) }, "eng")),

            // add a multistring for alert_text with UTF-16 encoding
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "A tornado has been spotted in Polk county at 5:13pm.", 0, 0x3f) }, "eng")),

            // add a multistring for alert_text with SCSU encoding
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "A tornado has been spotted in Polk county at 5:13pm.", 0, 0x3e) }, "eng")),

            // add a multistring for alert_text with SCSU encoding w/strings
            // outside the ascii set
            // capital greek characters
            new EASMessageForm(
                    31,
                    0xffff,
                    "CIV",
                    "ADR",
                    "Greeks have taken over the headend!",
                    120,
                    0xFFFFFFFF,
                    6000,
                    15,
                    0xFFFF,
                    0x3FF,
                    0x3FF,
                    0xFFFF,
                    "\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398\u0399\u039a\u039b\u039c\u039d\u039e\u039f\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) },
                    new EASOutOfBandExceptionEntry[] { new EASOutOfBandExceptionEntry(0xffff),
                            new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) },
                    new EASMessageDescriptor[] { new EASMessageDescriptor(descriptorArray1),
                            new EASMessageDescriptor(descriptorArray2) },
                    null,
                    new MultipleString(
                            new SegmentEntry[] { new SegmentEntry(
                                    "\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398\u0399\u039a\u039b\u039c\u039d\u039e\u039f\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9",
                                    0, 0x3e) }, "eng")),

            // add a multistring for alert_text with mode = 3 run-length
            // encoding w/strings outside the ascii set
            // capital greek characters
            new EASMessageForm(31, 0xffff, "CIV", "ADR", "Greeks have taken over the headend!", 120, 0xFFFFFFFF, 6000,
                    15, 0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 0, 3) }, "eng")),

            // add a multistring for alert_text with mode = 3 for one segment
            // and mode = 4 run-length encoding
            // for the second segment for strings outside the ascii set
            // capital greek characters followed by cyrillic letters
            new EASMessageForm(
                    31,
                    0xffff,
                    "CIV",
                    "ADR",
                    "You can't read the alert message",
                    120,
                    0xFFFFFFFF,
                    6000,
                    15,
                    0xFFFF,
                    0x3FF,
                    0x3FF,
                    0xFFFF,
                    "\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9\u04a0\u04a1\u04a3\u04a4\u04a5\u04a6\u04a7\u04a8\u04a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] {
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 0, 3),
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 0, 4) }, "eng")),

            // add a multistring for alert_text with UTF-16 encoding w/strings
            // outside the ascii set
            // capital greek characters
            new EASMessageForm(31, 0xffff, "CIV", "ADR", "Greeks have taken over the headend!", 120, 0xFFFFFFFF, 6000,
                    15, 0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9", 0, 0x3f) }, "eng")),

            // add a new multistring with spanish language message
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "A tornado has been spotted in Polk county at 5:13pm.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new StringEntry[] {
                            new StringEntry(new SegmentEntry("A tornado has been spotted in Polk county at 5:13pm.", 0,
                                    0x3e), "eng"),
                            new StringEntry(new SegmentEntry(
                                    "Un tornado se ha marcado en el Condado de Polk en 5:13 de la tarde.", 0, 0x3e),
                                    "esl") })),

            // no language string match for alert_text
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "Un tornado se ha marcado en el Condado de Polk en 5:13 de la tarde.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new StringEntry(new SegmentEntry(
                            "Un tornado se ha marcado en el Condado de Polk en 5:13 de la tarde.", 0, 0x3e), "esl"))),

            // multiple strings with no match for alert_text
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "Ein Tornado ist in der Polk Grafschaft an 5:13pm befleckt worden.", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new StringEntry[] {
                            new StringEntry(new SegmentEntry(
                                    "Ein Tornado ist in der Polk Grafschaft an 5:13pm befleckt worden.", 0, 0x3e),
                                    "deu"),
                            new StringEntry(new SegmentEntry(
                                    "Un tornado se ha marcado en el Condado de Polk en 5:13 de la tarde.", 0, 0x3e),
                                    "esl") })),

            // add a multistring for alert_text with UTF-16 encoding and an
            // illegal compression value
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "A tornado has been spotted in Polk county at 5:13pm.", 1, 0x3f) }, "eng")),

            // add a multistring for alert_text with Unicode compression and an
            // illegal compression value
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "A tornado has been spotted in Polk county at 5:13pm.", 1, 0x3e) }, "eng")),

            // add a multistring for alert_text with mode 0x03 and an illegal
            // compression value
            new EASMessageForm(31, 0xffff, "EAS", "TOR", "Blizzard Warning", 120, 0xFFFFFFFF, 6000, 15, 0xFFFF, 0x3FF,
                    0x3FF,
                    0xFFFF,
                    "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] { new SegmentEntry(
                            "\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 1, 3) }, "eng")),

            // add a multistring for alert_text with mode = 3 for one segment
            // and mode = 4 run-length encoding
            // for the second segment and illegal compression values for both.
            new EASMessageForm(31, 0xffff, "CIV", "ADR", "You can't read the alert message", 120, 0xFFFFFFFF, 6000, 15,
                    0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] {
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 1, 3),
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 1, 4) }, "eng")),

            // add a multistring for alert_text with mode = 3 for one segment
            // and mode = 4 run-length encoding
            // for the second segment and illegal compression values for one.
            new EASMessageForm(31, 0xffff, "CIV", "ADR", "You can't read the alert message", 120, 0xFFFFFFFF, 6000, 15,
                    0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "\u03a0\u03a1\u03a3\u03a4\u03a5\u03a6\u03a7\u03a8\u03a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] {
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 0, 3),
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 1, 4) }, "eng")),

            // add a multistring for alert_text with mode = 3 for one segment
            // and mode = 4 run-length encoding
            // for the second segment and illegal compression values for one.
            new EASMessageForm(31, 0xffff, "CIV", "ADR", "You can't read the alert message", 120, 0xFFFFFFFF, 6000, 15,
                    0xFFFF, 0x3FF, 0x3FF,
                    0xFFFF,
                    "\u04a0\u04a1\u04a3\u04a4\u04a5\u04a6\u04a7\u04a8\u04a9", // alert_text
                    new EASLocCode[] { new EASLocCode(99, 9, 999) }, new EASOutOfBandExceptionEntry[] {
                            new EASOutOfBandExceptionEntry(0xffff), new EASOutOfBandExceptionEntry(0) },
                    new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1023, 1023),
                            new EASInBandExceptionEntry(0, 0) }, new EASMessageDescriptor[] {
                            new EASMessageDescriptor(descriptorArray1), new EASMessageDescriptor(descriptorArray2) },
                    null, new MultipleString(new SegmentEntry[] {
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 1, 3),
                            new SegmentEntry("\u00a0\u00a1\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9", 0, 4) }, "eng")), };
}
