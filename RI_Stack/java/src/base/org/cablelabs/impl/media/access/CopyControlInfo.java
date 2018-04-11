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
 * This class represents time-based CCI data
 */

package org.cablelabs.impl.media.access;

import org.cablelabs.impl.util.HexConverter;
import org.cablelabs.impl.util.TimeAssociatedElement;

public class CopyControlInfo extends TimeAssociatedElement
{
    private static final long serialVersionUID = -5407639898509969341L;

    private byte m_cci;

    /** Bit offset in the CCI byte for the EMI */
    public static final byte EMI_OFFSET = 0;

    /** Mask to apply to the CCI to get the EMI field */
    public static final byte EMI_MASK = 0x03;

    /** Indicates the content is not copy-restricted/ */
    public static final byte EMI_COPY_FREELY = 0x00;

    /** Indicates that the content was marked "Copy Once" but has been copied
     *  one generation and no further copies are permitted (but may
     * be timeshifted up to 90 minutes). */
    public static final byte EMI_COPY_NO_MORE = 0x1;

    /** Indicates that the content may be copied/recorded once, but no more
     *  than one generation. */
    public static final byte EMI_COPY_ONCE = 0x2;

    /** Indicates that the content should not be copied/recorded (but may
     * be timeshifted up to 90 minutes) */
    public static final byte EMI_COPY_NEVER = 0x3;

    public static final String[] emiString = {"COPY_FREELY", "COPY_NO_MORE", "COPY_ONCE", "COPY_NEVER"};

    /** Offset in the CCI byte for the APS (Analog Protection System) */
    public static final byte APS_OFFSET = 2;

    /** Mask to apply to the CCI to get the APS field */
    public static final byte APS_MASK = 0x0C;

    /** Copy Protection Encoding Off */
    public static final byte APS_CP_OFF = 0x0;

    /** AGC Process On, Split Burst Off */
    public static final byte APS_AGC_ON_SB_OFF = 0x1;

    /** AGC Process On, 2 Line Split Burst On */
    public static final byte APS_AGC_ON_SB_2L = 0x2;

    /** AGC Process On, 4 Line Split Burst On */
    public static final byte APS_AGC_ON_SB_4L = 0x3;

    public static final String[] apsString = { "CP_OFF","AGC_SB_OFF",
                                               "AGC_SB_2L","AGC_SB_4L"};

    /** Bit offset in the CCI byte for the CIT (Constrained Image Trigger) */
    public static final byte CIT_OFFSET = 4;

    /** Mask to apply to the CCI to get the CIT field */
    public static final byte CIT_MASK = 0x10;

    /** No Redistribution Control asserted */
    public static final byte CIT_IC_OFF = 0x0;

    /** Redistribution Control required */
    public static final byte CIT_IC_ON = 0x1;

    public static final String[] citString = {"IC_OFF","IC_ON"};

    /** Bit offset in the CCI byte for the RCT (Redistribution Control Trigger) */
    public static final byte RCT_OFFSET = 5;

    /** Mask to apply to the CCI to get the CIT field */
    public static final byte RCT_MASK = 0x20;

    /** No Redistribution Control asserted */
    public static final byte RCT_RC_OFF = 0x0;

    /** Redistribution Control required */
    public static final byte RCT_RC_ON = 0x1;

    public static final String[] rctString = {"RC_OFF","RC_ON"};

    /** Per Table 9.1-6 of CCCP 2.0 */
    public static final byte DTCP_PROTECTION_NOT_REQUIRED = 0x0;
    public static final byte DTCP_PROTECTION_COPY_NO_MORE = EMI_COPY_NO_MORE;
    public static final byte DTCP_PROTECTION_COPY_ONCE = EMI_COPY_ONCE;
    public static final byte DTCP_PROTECTION_COPY_NEVER = EMI_COPY_NEVER;
    public static final byte DTCP_PROTECTION_EPN_MODE = 0x20;

    private static final long NANOS_PER_MILLI = 1000000;

    /**
     * Construct a time-associated CopyControlInfo
     *
     * @param mediaTimeNs Media time associated with the CCI object (in nanoseconds)
     * @param cci The CCI bytes (as defined in CCCP 2.0)
     */
    public CopyControlInfo(final long mediaTimeNs, final byte cci)
    {
        super(mediaTimeNs);
        m_cci = cci;
    }

    /**
     * Construct a CopyControlInfo
     *
     * @param cci The CCI bytes (as defined in CCCP 2.0)
     */
    public CopyControlInfo(final byte cci)
    {
        super(-1);
        m_cci = cci;
    }

    public byte getCCI()
    {
        return m_cci;
    }

    /**
     * Get the EMI field of the CCI
     *
     * @return one of EMI_COPY_FREELY, EMI_COPY_NO_MORE,
     * EMI_COPY_ONCE, or EMI_COPY_NEVER
     */
    public byte getEMI()
    {
        return (byte)((m_cci & EMI_MASK) >> EMI_OFFSET);
    }

    /**
     * Get the APS field of the CCI
     *
     * @return one of APS_CP_OFF, APS_AGC_ON_SB_OFF,
     *         APS_AGC_ON_SB_2L, APS_AGC_ON_SB_4L
     */
    public byte getAPS()
    {
        return (byte)((m_cci & APS_MASK) >> APS_OFFSET);
    }

    /**
     * Get the CIT field of the CCI
     *
     * @return CIT_IC_OFF or CIT_IC_ON
     */
    public byte getCIT()
    {
        return (byte)((m_cci & CIT_MASK) >> CIT_OFFSET);
    }

    /**
     * Get the RCT field of the CCI
     *
     * @return RCT_RC_OFF or RCT_RC_ON
     */
    public byte getRCT()
    {
        return (byte)((m_cci & RCT_MASK) >> RCT_OFFSET);
    }

    /**
     * Indicates the content is not copy-restricted
     *
     * @return true if content is marked "Copy Freely" or false if not
     */
    public boolean isEMICopyFreely()
    {
        return (getEMI() == EMI_COPY_FREELY);
    }

    /**
     * Indicates that the content should not be copied/recorded, but may
     * be timeshifted (up to 90 minutes)
     *
     * @return true if content is marked "Copy Never" or false if not
     */
    public boolean isEMICopyNever()
    {
        return (getEMI() == EMI_COPY_NEVER);
    }

    /**
     * Indicates that the content may be copied/recorded once, but no more than one
     * generation.
     *
     * @return true if content is marked "Copy Once" or false if not
     */
    public boolean isEMICopyOnce()
    {
        return (getEMI() == EMI_COPY_ONCE);
    }

    /**
     * Indicates that the content was marked "Copy Once" but has been copied
     * one generation and no further copies are permitted.
     *
     * @return true if content is marked "Copy No More" or false if not
     */
    public boolean isEMICopyNoMore()
    {
        return (getEMI() == EMI_COPY_NO_MORE);
    }

    public boolean dtcpEncryptionRequired()
    {
        // True unless both EMI is 0 and RCT is 0
        return !((m_cci & (RCT_MASK | EMI_MASK)) == 0);
    }

    /**
     * Returns the DTCP Output Protection, as defined by Table 9.1-6 of CCCP 2.0
     *
     * @return Either DTCP_PROTECTION_NOT_REQUIRED, DTCP_PROTECTION_EPN_MODE,
     * DTCP_PROTECTION_COPY_NO_MORE, DTCP_PROTECTION_COPY_ONCE,
     * or DTCP_PROTECTION_COPY_NEVER
     */
    public byte dtcpProtectionMode()
    {
        if ((m_cci & EMI_MASK) == 0)
        {
            // When EMI is not asserted, mode is dictated by RCT
            return (byte)(m_cci & RCT_MASK);
        }

        return (byte)(m_cci & EMI_MASK);
    }

    /**
     * Indicates if the content is timeshift-restricted. If this returns true,
     * timeshift must be limited to 90 minutes from the effective time of the
     * CCI indication.
     *
     * @return true if the CCI/EMI indicates the content is timeshift-restricted
     *         false if the content is unrestricted for timeshift.
     */
    public boolean isTimeshiftRestricted()
    {
        return isEMICopyNever() || isEMICopyNoMore();
    }

    public boolean isRecordable()
    {
        return isEMICopyOnce() || isEMICopyFreely();
    }

    public boolean equals(Object object)
    {
        if (object == null) return false;

        if (!(object instanceof CopyControlInfo)) return false;

        CopyControlInfo that = (CopyControlInfo)object;

        if (this.m_cci != that.m_cci) return false;

        return super.equals(object);
    }

    //findbugs detected - equal objects must have equal hash codes.
    public int hashCode()
    {
        return new Byte(this.m_cci).hashCode();
    }

    public long getTimeNanos()
    {
        return time;
    }

    public void setTimeNanos(final long timeNS)
    {
        time = timeNS;
    }

    public long getTimeMillis()
    {
        return time/NANOS_PER_MILLI;
    }

    public void setTimeMillis(final long timeMS)
    {
        time = timeMS*NANOS_PER_MILLI;
    }

    /** Provide a copy of this CopyControlInfo */
    public Object clone() throws CloneNotSupportedException
    {
        return new CopyControlInfo(m_cci);
    }

    public String toString()
    {
        return "CCI:[" + getTimeMillis()
                + "ms, 0x" + HexConverter.byteToHexString(m_cci)
                + '(' + emiString[getEMI()]
                + ' ' + apsString[getAPS()]
                + ' ' + citString[getCIT()]
                + ' ' + rctString[getRCT()]
                + ")]";
    }
} // END class TimeAssociatedCCI
