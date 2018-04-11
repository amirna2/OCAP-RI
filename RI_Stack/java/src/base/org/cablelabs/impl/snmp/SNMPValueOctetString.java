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

package org.cablelabs.impl.snmp;

import org.cablelabs.impl.snmp.drexel.SNMPObject;
import org.cablelabs.impl.snmp.drexel.SNMPOctetString;
import org.ocap.diagnostics.MIBDefinition;
import org.cablelabs.impl.snmp.SNMPBadValueException;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.SimpleTimeZone;
import java.util.GregorianCalendar;
import java.util.Locale;


/**
 * This Class represents an SNMPValue of type 
 * MIBDefinition.SNMP_TYPE_OCTETSTRING 
 */
public class SNMPValueOctetString extends SNMPValue
{
    private static final int NUM_BYTES_IN_RFC2579_DATE_AND_TIME = 11;
    private static final int NUM_BYTES_IN_RFC2579_DATE_AND_TIME_WITHOUT_TIMEZONE = 8;
    private static final int NUM_BYTES_IN_IPV4_FORMAT = 4;
    private static final int NUM_BYTES_IN_IPV6_FORMAT = 16;
    private static final int NUM_BYTES_IN_MAC_ADDRESS = 6;
    private static final int BYTE_SHIFT = 8;
    private static final int SHIFT_3_NIBBLES = 12;
    private static final int SHIFT_2_NIBBLES = 8;
    private static final int SHIFT_1_NIBBLE = 4;
    private static final int MILLIS_PER_DECISECOND = 100;
    private static final int MILLIS_PER_MINUTE = (1000 * 60);
    private static final int MINUTES_PER_HOUR = 60;
    private static final TimeZone UTC_TIME_ZONE = new SimpleTimeZone(0, "UTC");
    private static final int MONTH_OFFSET = 1;
    private static final int MONTH_MAX = MONTH_OFFSET + 11;
    private static final int MONTH_MIN = MONTH_OFFSET;
    private static final int DAY_MIN = 0;
    private static final int DAY_MAX = 30;
    private static final int HOUR_MIN = 0;
    private static final int HOUR_MAX = 23;
    private static final int MINUTE_MIN = 0;
    private static final int MINUTE_MAX = 59;
    private static final int SECOND_MIN = 0;
    private static final int SECOND_MAX = 59;

    // Lazy calculate of time, also this is expensive to calculate
    private Calendar timeLocal;

    /**
     * Constructor 
     * Create an empty Object.
     */
    public SNMPValueOctetString()
    {
        super(new SNMPOctetString());
    }

    /**
     *    Constructor 
     *    Create an octet string from the bytes of the supplied String.
     *    @param value the String to use in construction
     */
    public SNMPValueOctetString(String value)
    {
        super(new SNMPOctetString(value));
    }

    /**
     *    Constructor 
     *    Create an octet string from a date. This is in the format
     *    as defined in RFC2579 DateAndTime ::= TEXTUAL-CONVENTION
     *    @param calendar the Calendar to base the Octet String on
     */
    public SNMPValueOctetString(Calendar calendar)
    {
        // Can't create an SNMPValue directly from a Calendar, so force it to null...
        super(null);
        // Now build the 11 bytes of date
        byte[] dateString = new byte[ calendar.getTimeZone() != null ? NUM_BYTES_IN_RFC2579_DATE_AND_TIME : NUM_BYTES_IN_RFC2579_DATE_AND_TIME_WITHOUT_TIMEZONE];

        dateString[0] = (byte) (calendar.get(Calendar.YEAR) >>> BYTE_SHIFT);
        dateString[1] = (byte) (calendar.get(Calendar.YEAR));
        dateString[2] = (byte) (calendar.get(Calendar.MONTH) + MONTH_OFFSET);
        dateString[3] = (byte) (calendar.get(Calendar.DAY_OF_MONTH));
        dateString[4] = (byte) (calendar.get(Calendar.HOUR_OF_DAY));
        dateString[5] = (byte) (calendar.get(Calendar.MINUTE));
        dateString[6] = (byte) (calendar.get(Calendar.SECOND));
        dateString[7] = (byte) (calendar.get(Calendar.MILLISECOND) / MILLIS_PER_DECISECOND);

        if (calendar.getTimeZone() != null)
        {
            int zoneOffsetInMinutes = calendar.get(Calendar.ZONE_OFFSET) / MILLIS_PER_MINUTE;
            dateString[8] = (byte) (zoneOffsetInMinutes >= 0 ? '+' : '-');
            dateString[9] = (byte) (zoneOffsetInMinutes / MINUTES_PER_HOUR);
            dateString[10] = (byte) (zoneOffsetInMinutes % MINUTES_PER_HOUR);
        }

        super.setObject(new SNMPOctetString(dateString));
    }

    /**
     *    Constructor  
     * @param value
     *           The byte representation of the value without 
     *           any type or length bytes.
     */
    public SNMPValueOctetString(byte[] value)
    {
        super(new SNMPOctetString(value));
    }

    /**
     * Constructor
     * @param snmpObject the SNMPObject to use directly
     */
    public SNMPValueOctetString(SNMPObject snmpObject)
    {
        super(snmpObject);
    }

    /**
     *  Returns a Calendar from the Octet String where it contains a date
     *  as defined in RFC2579 DateAndTime ::= TEXTUAL-CONVENTION
     *  which is an 8 or 11 byte string depending on timezone being
     *  present. The time returned is local to the machine.
     *
     *  @return the String decoded to a Calendar object in the local time offset
     *  @throws SNMPBadValueException
     */
    public Calendar getTimeAsCalendar() throws SNMPBadValueException
    {
        byte[] raw = (byte[])snmpObject.getValue(); 

        if ((raw.length != NUM_BYTES_IN_RFC2579_DATE_AND_TIME) &&
            (raw.length != NUM_BYTES_IN_RFC2579_DATE_AND_TIME_WITHOUT_TIMEZONE))
        {
            throw new SNMPBadValueException("Wrong number of bytes in Octet String to generate Calendar");
        }
        if (timeLocal == null)
        {
            timeLocal = new GregorianCalendar();
            
            boolean offsetDelivered = raw.length == NUM_BYTES_IN_RFC2579_DATE_AND_TIME;

            // Extract the basic time bytes
            int year, month, day, hour, min, sec, millisecond;
            year = unsigned2BytesToInt(raw[0], raw[1]);
            month = raw[2] - MONTH_OFFSET;
            day = raw[3];
            hour = raw[4];
            min = raw[5];
            sec = raw[6];
            millisecond = raw[7] * MILLIS_PER_DECISECOND;

            if (offsetDelivered)
            {
                // Extract the timezone offset (offset from UTC)
                int offsetHours, offsetMinutes;
                offsetHours = raw[9];
                offsetMinutes = raw[10];
                offsetMinutes += offsetHours * 60;
                if (raw[8] == '-')
                {
                    offsetMinutes *= -1;
                }

                // Make a UTC time zone calendar and set with the calculated UTC time
                Calendar timeUTC = new GregorianCalendar(UTC_TIME_ZONE, Locale.getDefault());
                timeUTC.clear();
                // set timeUTC to local time...
                timeUTC.set(year, month, day, hour, min, sec);
                // ...and adjust it to UTC time. Note UTCTime =  localTime - offsetFromUTC
                timeUTC.add(Calendar.MINUTE, -offsetMinutes);

                // timeUTC now has UTC time so update the local time Calendar. 
                timeLocal.clear();
                timeLocal.setTimeInMillis(timeUTC.getTimeInMillis()); 
            }
            else // No offset, return the time in the local timezone.
            {
                // There is no offset, so return time in the local time.
                timeLocal.clear();
                timeLocal.set(year, month, day, hour, min, sec);
            }
            timeLocal.set(Calendar.MILLISECOND, millisecond);
        }

        return timeLocal;
    }

    /**
     * Checks to see if the data provided is a valid date or not
     * @return true if valid
     */
    public boolean isValidDate()
    {
        // We'll assume a good date
        boolean isGoodDate = true;
        byte[] raw = (byte[])snmpObject.getValue();

        if (raw == null)
        {
            isGoodDate = false;
        }
        else if ((raw.length != NUM_BYTES_IN_RFC2579_DATE_AND_TIME) && (raw.length != NUM_BYTES_IN_RFC2579_DATE_AND_TIME_WITHOUT_TIMEZONE))
        {
            isGoodDate = false;
        }
        else
        {
            // Assume bad date, i.e. full of zeros
            isGoodDate = false;
            for (int i = 0; i < raw.length; i ++)
            {
                if (raw[i] != (byte)0)
                {
                    isGoodDate = true;
                    break;
                }
            }
            // Get here and isGoodDate will be false if all zeros in the buffer
        }
        if (isGoodDate)
        {
            // Now validate date [a bit]
            // Check month is 1~12 inc, don't care about the year
            if ((raw[2] < MONTH_MIN) || (raw[2] > MONTH_MAX))
            {
                isGoodDate = false;
            }
            // Check day is 0~30 inc
            else if ((raw[3] < DAY_MIN) || (raw[3] > DAY_MAX))
            {
                isGoodDate = false;
            }
            // Check hour is 0~23 inc
            else if ((raw[4] < HOUR_MIN) || (raw[4] > HOUR_MAX))
            {
                isGoodDate = false;
            }
            // Check minute is 0~59
            else if ((raw[5] < MINUTE_MIN) || (raw[5] > MINUTE_MAX))
            {
                isGoodDate = false;
            }
            // Check second is 0~59
            else if ((raw[6] < SECOND_MIN) || (raw[6] > SECOND_MAX))
            {
                isGoodDate = false;
            }
        }
        return isGoodDate;
    }

    /**
     *    Returns the value as a colon separated hex MAC Address
     *
     *    @return MAC Address, e.g. "01:02:03:04:F5:F6:F7:F8"
     *    @throws SNMPBadValueException if the String is not length 8.
     */
    public String getAsMacAddress() throws SNMPBadValueException
    {
        byte[] raw = (byte[])snmpObject.getValue(); // raw data, just V not TLV

        if (raw.length != NUM_BYTES_IN_MAC_ADDRESS)
        {
            throw new SNMPBadValueException("Wrong number of bytes in Octet String for MAC Address");
        }

        return bytesToStringWithSeparator(raw, true, ":", false);
    }

    /**
     *    Returns the value as a dot or colon separated IPv4 or IPv6 Address
     *
     *    @return IP Address, e.g. for IPv4 "127.0.0.1" or for IPv6 "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
     *    @throws SNMPBadValueException if the String length is not 4 or 16.
     */
    public String getAsIpAddress() throws SNMPBadValueException
    {
        byte[] raw = (byte[])snmpObject.getValue(); // raw data, just V not TLV

        if ((raw.length != NUM_BYTES_IN_IPV4_FORMAT) && (raw.length != NUM_BYTES_IN_IPV6_FORMAT))
        {
            throw new SNMPBadValueException("Wrong number of bytes in Octet String for IPAddress");
        }
        // IPv6 has ':' as separator, IPv4 has '.'
        return bytesToStringWithSeparator(raw, 
                                          raw.length == NUM_BYTES_IN_IPV6_FORMAT,                   // IPv4 is in dec, IPv6 is in hex
                                          (raw.length == NUM_BYTES_IN_IPV6_FORMAT) ? ":" : ".",     // IPv4 separator = '.' IPv6 separator = ':'
                                          (raw.length == NUM_BYTES_IN_IPV6_FORMAT) ? true : false); // IPv4 has 8bit values IPv6 has 16bit values
    }

    /**
     * Returns the value as a String based on a byte array
     * @return a String representing the Value in hex
     */
    public String getValueAsString()
    {
        byte[] raw = (byte[])snmpObject.getValue(); // raw data, just V not TLV
        return bytesToStringWithSeparator(raw, true, null, false);
    }
    /**
     * Return the byte array
     *
     */
    public byte[] getValue()
    {
        return (byte[]) snmpObject.getValue();
    }

    /**
     * Set the value from a byte array
     *
     */
    public void setValue(byte[] newValue) throws SNMPBadValueException
    {
        snmpObject.setValue(newValue); 
    }

    /**
     * Set the value from a String
     *
     */
    public void setValue(String newValue) throws SNMPBadValueException
    {
        snmpObject.setValue(newValue); 
    }

    /** 
     *    Returns a space-separated hex string corresponding to the raw bytes.
     */
    public String toHexString()
    {
        return ((SNMPOctetString)snmpObject).toHexString();
    }
    
    /**
     * Returns the raw ASN.1 byte array representing the value.
     *
     * @return a byte array of ASN.1 data
     */
    public byte[] getBEREncoding() 
    {
        return snmpObject.getBEREncoding();
    }

    /**
     * @return the MIBDefinition::SNMP_TYPE<xxx> value
     */
    public int getType()
    {
        return MIBDefinition.SNMP_TYPE_OCTETSTRING;
    }

    /** 
     * Convert two unsigned bytes making up a short, into an int value 
     *
     * @param msb most significant byte
     * @param lsb least significant byte
     * @return the int value 
     */
    private static int unsigned2BytesToInt(byte msb, byte lsb)
    {
        int i = 0;
        i |= msb & 0xFF;
        i <<= 8;
        i |= lsb & 0xFF;
        return i;
    }

    /*
    Array used to make hex decoding quicker...
     */
    private final static String numValues[] = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "A", "B", "C", "D", "E", "F" };

    /**
     * Convert a byte array into a string of numbers with
     * a separator between each byte value.
     * 
     * @param raw - the raw byte data
     * @param isHex - If the string should be a Hex string
     * @param separator - the separator between each number, can be null for no separator
     * @param is16Bit - true if dealing with 16 bit numbers
     * @return the string with separators inserted.
     */
    private String bytesToStringWithSeparator(byte[] raw, boolean isHex, String separator, boolean is16Bit)
    {
        StringBuffer returnStringBuffer = new StringBuffer();
        int incrementer = (is16Bit == true) ? 2 : 1;
        int convert;
        
        for (int i = 0; i < raw.length; i += incrementer)
        {
            if (is16Bit == true)
            {
                convert = ((raw[i] & 0xFF) << BYTE_SHIFT) + (raw[i+1] & 0xFF);
                if (isHex)
                {
                    returnStringBuffer.append(numValues[(convert & 0xF000) >> SHIFT_3_NIBBLES]);
                    returnStringBuffer.append(numValues[(convert & 0x0F00) >> SHIFT_2_NIBBLES]);
                    returnStringBuffer.append(numValues[(convert & 0x00F0) >> SHIFT_1_NIBBLE]);
                    returnStringBuffer.append(numValues[(convert & 0x000F)]);
                }
                else
                {
                    returnStringBuffer.append(convert);
                }
            }
            else // 8 bit numbers 
            {
                convert = raw[i] & 0xFF;
                if (isHex)
                {
                    returnStringBuffer.append(numValues[convert / 16]);
                    returnStringBuffer.append(numValues[convert % 16]);
                }
                else
                {
                    returnStringBuffer.append(convert);
                }
            }
            
            // No separator after last value
            if ((separator != null) && (i != (raw.length - incrementer)))
            {
                returnStringBuffer.append(separator);
            }
        }
        
        return returnStringBuffer.toString();
    }
    
}


