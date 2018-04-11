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

/**
 * @author Alan Cohn - enableTV
 */
package org.cablelabs.impl.ocap.manager.eas;

import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.ocap.util.string.ATSCMultiString;
import org.ocap.system.EASModuleRegistrar;

import com.ibm.icu.text.UnicodeCompressor;
import org.cablelabs.impl.util.string.ATSCHuffmanEncodeTable;
import org.cablelabs.impl.util.string.ATSCHuffmanEncoder;

/**
 * This JUnit test verifies the ability of the EASAlertTextOnly.java class to
 * present and scroll a text alert message across a TV/Monitor screen.
 * 
 */
public class EASTextAlertTest extends TestCase
{

    private final boolean OUTOFBAND = true;

    private final static byte inbandExceptionChannelsDesc[] = { (byte) 0, // EASMessage.EASDescriptor.INBAND_EXCEPTION_CHANNELS,
            (byte) 4, // byte count
            (byte) 1, // list count
            (byte) 6, // frequency
            (byte) 0, (byte) 0x64 // 2 bytes for 16-bit program id
    };

    private final EASMessageForm easmBase = new EASMessageForm(1, // sequence_number
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
            "Default Text: A tornado has been spotted in Douglas County at 5:13pm. MT end.", // alert_text
            new EASLocCode[] { new EASLocCode(1, 2, 3) }, // location_codes
            new EASOutOfBandExceptionEntry[] { new EASOutOfBandExceptionEntry(0),
                    new EASOutOfBandExceptionEntry(0xFFFF) }, // out-of-band
                                                              // exceptions
            new EASInBandExceptionEntry[] { new EASInBandExceptionEntry(1, 2), new EASInBandExceptionEntry(3, 4) }, // in-of-band
                                                                                                                    // exceptions
            new EASMessageDescriptor[] { new EASMessageDescriptor(inbandExceptionChannelsDesc) }, // descriptors
            null, // multi_string_structure for nature_of_activation_text
            null // multi_string_structure for alert_text
    );

    /********************************************************************************************
     * Tests start here
     ********************************************************************************************/

    public void testEASTextAlertBasic()
    {
        final String FONT_NAME = "SansSerif";
        final int FONT_SIZE = 16;
        final Font font = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);
        final Color fontColor = Color.YELLOW;
        final Color fontBGcolor = Color.BLUE;

        EASMessage.resetLastReceivedSequenceNumber();

        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        EASAlertTextOnly textAlert = new EASAlertTextOnly(null, easMessage);
        textAlert.updateAttributes(font, fontColor, fontBGcolor);
        textAlert.updatePreferredLanguages(new String[] { "eng" });

        textAlert.startPresentation();
        try
        {
            Thread.sleep(20000); // wait milliseconds
        }
        catch (Exception e)
        {
            System.out.println("EASTextAlertTest Thread.sleep exception " + e);
        }
        textAlert.stopPresentation();
    }

    public void testEASTextAlertFont32()
    {
        final String FONT_NAME = "SansSerif";
        final int FONT_SIZE = 32;
        final Font font = new Font(FONT_NAME, Font.BOLD, FONT_SIZE);
        final Color fontColor = Color.GREEN.darker();
        final Color fontBGcolor = Color.ORANGE;

        EASMessage.resetLastReceivedSequenceNumber();

        easmBase.setAlertText("Font32 test: abcdefghijklmnopqrstuvwxyz " + "ABCDEFGHIJKLMNOPQRSTUVWXYZ "
                + "0123456789 " + "!@#$%^&*()_+-={}[]<>?/" + " end.");

        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        EASAlertTextOnly textAlert = new EASAlertTextOnly(null, easMessage);
        textAlert.updateAttributes(font, fontColor, fontBGcolor);
        textAlert.updatePreferredLanguages(new String[] { "eng" });

        textAlert.startPresentation();
        try
        {
            Thread.sleep(20000); // wait milliseconds
        }
        catch (Exception e)
        {
            System.out.println("EASTextAlertTest Thread.sleep exception " + e);
        }
        textAlert.stopPresentation();
    }

    public void testEASTextAlertNewLine()
    {
        final String FONT_NAME = "SansSerif";
        final int FONT_SIZE = 16;
        final Font font = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);
        final Color fontColor = Color.GRAY.darker();
        final Color fontBGcolor = Color.YELLOW;

        EASMessage.resetLastReceivedSequenceNumber();

        easmBase.setAlertText("Line Control Characters Test: New Line,\n Tab,\t CarriageReturn,\r FormFeed,\f BackSpace,\b end.");

        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);
        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        EASAlertTextOnly textAlert = new EASAlertTextOnly(null, easMessage);
        textAlert.updateAttributes(font, fontColor, fontBGcolor);
        textAlert.updatePreferredLanguages(new String[] { "eng" });

        textAlert.startPresentation();
        try
        {
            Thread.sleep(20000); // wait milliseconds
        }
        catch (Exception e)
        {
            System.out.println("EASTextAlertTest Thread.sleep exception " + e);
        }
        textAlert.stopPresentation();
    }

    public void testEASTextAlertSpanishLanguage()
    {
        final String FONT_NAME = "SansSerif";
        final int FONT_SIZE = 16;
        final Font font = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);
        final Color fontColor = Color.GRAY.darker();
        final Color fontBGcolor = Color.WHITE;

        EASMessage.resetLastReceivedSequenceNumber();

        easmBase.setMultipleStringAlert("Texto en lengua espa�ola se debe mostrar.", "spa");
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);

        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        EASAlertTextOnly textAlert = new EASAlertTextOnly(null, easMessage);
        textAlert.updateAttributes(font, fontColor, fontBGcolor);
        textAlert.updatePreferredLanguages(new String[] { "spa" });

        textAlert.startPresentation();
        try
        {
            Thread.sleep(20000); // wait milliseconds
        }
        catch (Exception e)
        {
            System.out.println("EASTextAlertTest Thread.sleep exception " + e);
        }
        textAlert.stopPresentation();
    }

    public void testEASTextAlertFactoryTest()
    {
        EASMessage.resetLastReceivedSequenceNumber();

        EASAlertTextFactory eastaf = EASAlertTextFactory.getInstance();

        Color fontBGcolor = (Color) eastaf.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR);
        Color fontColor = (Color) eastaf.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR);

        Integer fontSize = (Integer) eastaf.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE);
        int FONT_SIZE = fontSize.intValue();

        String FONT_NAME = (String) eastaf.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE);

        String fontStyle = (String) eastaf.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE);
        int FONT_STYLE = Font.BOLD | Font.ITALIC;

        if (fontStyle.equalsIgnoreCase("PLAIN"))
            FONT_STYLE = Font.PLAIN;
        else if (fontStyle.equalsIgnoreCase("BOLD"))
            FONT_STYLE = Font.BOLD;
        else if (fontStyle.equalsIgnoreCase("ITALIC")) FONT_STYLE = Font.ITALIC;

        Font font = new Font(FONT_NAME, FONT_STYLE, FONT_SIZE);

        easmBase.setAlertText("EASAlertTextFactory defaults: " + "Font Size = " + FONT_SIZE + ", Font Name = "
                + FONT_NAME + ", Font Style = " + fontStyle);
        EASMessageGenerator easmgen = new EASMessageGenerator(easmBase);

        byte[] easmsg = easmgen.generate();
        // Create an EAS Message from binary byte array
        EASMessage easMessage = EASMessage.create(OUTOFBAND, easmsg);
        if (easMessage == null)
        {
            fail("Unexpected duplicate");
            return;
        }

        EASAlertTextOnly textAlert = new EASAlertTextOnly(null, easMessage);
        textAlert.updateAttributes(font, fontColor, fontBGcolor);
        textAlert.updatePreferredLanguages(new String[] { "eng" });

        textAlert.startPresentation();
        try
        {
            Thread.sleep(20000); // wait milliseconds
        }
        catch (Exception e)
        {
            System.out.println("EASTextAlertTest Thread.sleep exception " + e);
        }
        textAlert.stopPresentation();
    }

    public void testEASTextAlertFactoryGetTest()
    {
        EASAlertTextFactory eastaf = EASAlertTextFactory.getInstance();

        Color[] colors = (Color[]) eastaf.getEASCapability(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR);
        System.out.println(colors.length + " Font Colors returned from EASAlertTextFactory:");
        for (int x = 0; x < colors.length; x++)
        {
            System.out.println(colors[x].toString());
        }

        Color[] BGcolors = (Color[]) eastaf.getEASCapability(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR);
        System.out.println(BGcolors.length + " Font Background Colors returned from EASAlertTextFactory:");
        for (int x = 0; x < BGcolors.length; x++)
        {
            System.out.println(BGcolors[x].toString());
        }

        String[] fontName = (String[]) eastaf.getEASCapability(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE);
        System.out.println(fontName.length + " fontName returned from EASAlertTextFactory:");
        for (int x = 0; x < fontName.length; x++)
        {
            System.out.println(fontName[x]);
        }

    }

    /******************************************************************
     * The following classes are used to create a byte array of binary EAS
     * message.
     *****************************************************************/
    public static class EASMessageDescriptor
    {
        public byte[] data;

        public EASMessageDescriptor(byte[] data)
        {
            this.data = data;
        }
    }

    public static class EASMessageForm
    {
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

        /**
         * Multi Language Alert message
         */
        MultipleString multi_alert_text;

        MultipleString multi_nature_text;

        /**
         * default empty constructor for an EAS message object
         */
        public EASMessageForm()
        {
        }

        /**
         * constructor for initializing an EAS message object
         */
        public EASMessageForm(int sequence_number,
                int EAS_Event_ID,
                String EAS_originator_code,
                String EAS_event_code,
                String nature_of_activation_text,
                int alert_message_time_remaining, // seconds
                int event_start_time, int event_duration, int alert_priority, int details_OOB_source_ID,
                int details_major_channel_number, int details_minor_channel_number, int audio_OOB_source_ID,
                String alert_text, EASLocCode[] location_codes, EASOutOfBandExceptionEntry[] out_of_band_exceptions,
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

        public void setAlertPriority(int priority)
        {
            alert_priority = priority;
        }

        public void setEventDuration(int duration)
        {
            event_duration = duration;
        }

        /*
         * Set the alert text in english
         * 
         * @param String alert_text
         */
        public void setAlertText(String alert_text)
        {
            this.multi_alert_text = new MultipleString(alert_text);
        }

        /*
         * set the alert text in user specified language
         * 
         * @param String alert_text, String language
         */
        public void setMultipleStringAlert(String alert_text, String language)
        {
            this.multi_alert_text = new MultipleString(alert_text, language);
        }

        /*
         * @param int number
         * 
         * @return int - the previous sequence number. If new number is 0 then
         * no change to present sequence number.
         */
        public int setSequenceNumber(int number)
        {
            int temp = sequence_number;
            if (number != 0) sequence_number = number;
            return temp;
        }

        /*
         * @param int number
         * 
         * @return int - the sequence number.
         */
        public int getSequenceNumber()
        {
            return sequence_number;
        }

        /*
         * @return int - EAS_Event_ID
         */
        public int getEAS_Event_ID()
        {
            return EAS_Event_ID;
        }

        /*
         * @param int eventID, no change if eventID is 0
         * 
         * @return int previous EAS_Event_ID.
         */
        public int setEAS_Event_ID(int eventID)
        {
            int temp = EAS_Event_ID;
            if (eventID != 0) EAS_Event_ID = eventID;
            return temp;
        }

        public void setDetailsSourceId(final int sourceId)
        {
            details_OOB_source_ID = sourceId;
        }

        public String setEventCode(final String eventCode)
        {
            String temp = EAS_event_code;
            if (null != eventCode)
            {
                EAS_event_code = eventCode;
            }
            return temp;
        }

        /*
         * @param int time, time remaining in seconds
         * 
         * @return int previous time
         */
        public int setTimeRemaining(int time)
        {
            int temp = alert_message_time_remaining;
            alert_message_time_remaining = time;
            return temp;
        }

        public int setEventStartTime(int time)
        {
            int temp = event_start_time;
            event_start_time = time;
            return temp;
        }
    }

    /**
     * represents an out-of-band exception list entry.
     */
    public static class EASOutOfBandExceptionEntry
    {
        /**
         * out-of-band exception source-id
         */
        int exception_source_id;

        /**
         * default empty constructor for an EAS out-band exception object
         */
        public EASOutOfBandExceptionEntry()
        {
            this.exception_source_id = -1;
        }

        /**
         * constructor for initializing an EAS out-band exception object
         */
        public EASOutOfBandExceptionEntry(int exception_source_id)
        {
            this.exception_source_id = exception_source_id;
        }

    }

    /**
     * represents an in-band exception list entry.
     */
    public static class EASInBandExceptionEntry
    {
        /**
         * major channel number of the exception entry
         */
        int exception_major_channel_number;

        /**
         * minor channel number of the exception entry
         */
        int exception_minor_channel_number;

        /**
         * default empty constructor for an EAS in-band exception object
         */
        public EASInBandExceptionEntry()
        {
            this.exception_major_channel_number = -1;
            this.exception_minor_channel_number = -1;
        }

        /**
         * constructor for initializing an EAS in-band exception object
         */
        public EASInBandExceptionEntry(int exception_major_channel_number, int exception_minor_channel_number)
        {
            this.exception_major_channel_number = exception_major_channel_number;
            this.exception_minor_channel_number = exception_minor_channel_number;
        }

    }

    /**
     * represents a region definition in an EAS message
     */
    public static class EASLocCode
    {
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

        /**
         * default empty constructor for an EAS out-band exception object
         */

        public EASLocCode()
        {
            this.state_code = -1;
            this.county_code = -1;
            this.county_subdivision = -1;
        }

        /**
         * constructor for initializing an EAS out-band exception object
         */
        public EASLocCode(int state_code, int county_subdivision, int county_code)
        {
            this.state_code = state_code;
            this.county_subdivision = county_subdivision;
            this.county_code = county_code;
        }

    }

    /**
     * Class used to generate an EAS <code>byte</code> stream.
     */
    public static class EASMessageGenerator
    {
        // private static final int mpegSectionSizeMax = 4096;
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

    public static class MultipleString
    {
        /**
         * vector of StringEntry instances which make up the
         * multiple_string_structure
         */
        private Vector stringEntries = new Vector();

        protected MultipleString()
        {
        }

        public MultipleString(String string)
        {
            // in this case, a multiple_string is initialized with a string
            // entry with
            // one segment, no compression, mode = 0x0, and language
            // code = "eng"
            SegmentEntry segmentEntry = new SegmentEntry(string, 0, 0);
            stringEntries.add(new StringEntry(segmentEntry, "eng"));
        }

        public MultipleString(String string, String language)
        {
            // in this case, a multiple_string is initialized with a string
            // entry with
            // one segment, no compression, mode = 0x0,
            // and language code
            SegmentEntry segmentEntry = new SegmentEntry(string, 0, 0);
            stringEntries.add(new StringEntry(segmentEntry, language));
        }

        // add a new string entry to the multipleString. The string segment data
        // is
        // represented by the array of segment entries.
        public MultipleString(SegmentEntry entries[], String languageCode)
        {
            stringEntries.add(new StringEntry(entries, languageCode));
        }

        public MultipleString(StringEntry entries[])
        {
            for (int i = 0; i < entries.length; i++)
            {
                stringEntries.add(entries[i]);
            }
        }

        public MultipleString(StringEntry entry)
        {
            stringEntries.add(entry);
        }

        // encodes the multipleString and returns an array of bytes
        public byte[] encode()
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
    }

    public static class StringEntry
    {
        private String languageCode;

        private SegmentEntry segmentEntries[];

        public StringEntry(SegmentEntry entries[], String langCode)
        {
            languageCode = langCode;
            segmentEntries = entries;
        }

        public StringEntry(SegmentEntry entry, String langCode)
        {
            languageCode = langCode;
            segmentEntries = new SegmentEntry[] { entry };
        }

        public void encode(ByteArrayOutputStream writer)
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
    }

    public static class SegmentEntry
    {
        private int compression;

        private int mode;

        private String string = null;

        public SegmentEntry(String string, int compression, int mode)
        {
            // no testing for illegal combinations of mode and compression is
            // done so
            // poorly formed messages can be sent to the parser
            this.compression = compression;
            this.mode = mode;
            this.string = string;
        }

        public void encode(ByteArrayOutputStream writer)
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
    }

    /***********************************************
     * Class initialization from TestCase
     *************************************************/
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
        TestSuite suite = new TestSuite(EASTextAlertTest.class);
        return suite;
    }
}
