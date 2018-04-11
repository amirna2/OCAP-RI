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

package org.cablelabs.impl.ocap.si;

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.transport.TransportStream;

import org.davic.net.InvalidLocatorException;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.Descriptor;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.OcapLocatorImpl;
import org.cablelabs.impl.service.ProgramMapTableHandle;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This interface represents an MPEG-2 Program Map Table (PMT). The getLocator()
 * method defined in the SIElement interface shall return an
 * org.ocap.net.OcapLocator instance. The getServiceInformationType() method
 * defined in the SIElement interface shall return
 * ServiceInformationType.UNKNOWN.
 */
public class ProgramMapTableImpl implements ProgramMapTableExt
{
    /**
     * Construct a PMT from byte data returned by the native SI database
     * 
     * @param data
     *            the PMT byte data
     * @param pmtHandle
     *            the handle used by SICache to uniquely identify this PMT
     */
    public ProgramMapTableImpl(byte[] data, TransportStream transportStream, ProgramMapTableHandle pmtHandle)
    {
        ByteArrayWrapper wrapper = new ByteArrayWrapper(data, 0);

        // Ignore the first 2 integers. They are used exclusively by
        // SIDatabase/SICache
        ByteParser.getInt(wrapper);
        ByteParser.getInt(wrapper);

        // Parse frequency, sourceID, programNumber, and pcrPID
        frequency = ByteParser.getInt(wrapper);
        sourceID = ByteParser.getInt(wrapper);
        programNumber = ByteParser.getInt(wrapper);
        pcrPID = ByteParser.getInt(wrapper);

        // Parse outer descriptors from byte array
        int numOuterDesc = ByteParser.getInt(wrapper);
        if (numOuterDesc > 0)
        {
            outerDescriptors = new Descriptor[numOuterDesc];

            // construct outer loop Descriptors
            for (int i = 0; i < numOuterDesc; i++)
            {
                short descTag = (short) (ByteParser.getByte(wrapper) & 0xFF);
                short descSize = (short) (ByteParser.getByte(wrapper) & 0xFF);
                outerDescriptors[i] = new DescriptorImpl(descTag, ByteParser.getByteArray(wrapper, descSize));
            }
        }

        try
        {
            if(sourceID != -1) 
            {
                this.locator = new OcapLocator(sourceID); 
            }
            else if(frequency == OcapLocatorImpl.HN_FREQUENCY)
            {
                this.locator = new OcapLocatorImpl("remoteservice://n=hnservice");
            }
            else 
            {
                this.locator = new OcapLocator(frequency, programNumber, -1);
            }
        }
        catch (InvalidLocatorException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }

        // Parse elementary streams from byte array
        int numEStreams = ByteParser.getInt(wrapper);
        if (numEStreams > 0)
        {
            esInfo = new PMTElementaryStreamInfo[numEStreams];

            // construct PMTElementaryStreamInfo
            for (int i = 0; i < numEStreams; i++)
            {
                short streamType = (short) (ByteParser.getByte(wrapper) & 0xFF);
                int pid = ByteParser.getInt(wrapper);
                int numDesc = ByteParser.getInt(wrapper);
                Descriptor descriptors[] = new Descriptor[numDesc];

                // construct Descriptors
                for (int j = 0; j < numDesc; j++)
                {
                    short descTag = (short) (ByteParser.getByte(wrapper) & 0xFF);
                    short descSize = (short) (ByteParser.getByte(wrapper) & 0xFF);
                    descriptors[j] = new DescriptorImpl(descTag, ByteParser.getByteArray(wrapper, descSize));
                }

                int[] pidarray = { pid };
                OcapLocator elemLocator = null;
                try
                {
                    if(sourceID != -1)
                    {                    
                        elemLocator = new OcapLocator(sourceID, pidarray, -1, null);
                    }
                    else 
                    {
                        if(frequency == OcapLocatorImpl.HN_FREQUENCY)
                        {
                            elemLocator = new OcapLocatorImpl("remoteservice://n=hnservice");                            
                        }  
                        else
                        {
                            elemLocator = new OcapLocator(frequency, programNumber, -1, pidarray, -1, null);                            
                        }  
                    }
                            
                }
                catch (InvalidLocatorException e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }

                esInfo[i] = new PMTElementaryStreamInfoImpl(elemLocator, streamType, (short) pid, descriptors);
            }
        }

        this.pmtHandle = pmtHandle;
        this.transportStream = transportStream;
    }

    /**
     * Get the program_number field, corresponds with the PMT.
     * 
     * @return The program number corresponds with the PMT.
     */
    public int getProgramNumber()
    {
        return programNumber;
    }

    /**
     * Get the PCR_PID field. Thirteen bit field indicates the PID that shall
     * contain the PCR fields of the transport stream packets.
     * 
     * @return The PCR PID.
     */
    public int getPcrPID()
    {
        return pcrPID;
    }

    /**
     * Get the outer descriptor loop. List of descriptors that pertains to all
     * programs.
     * 
     * @return The outer descriptor loop.
     */
    public Descriptor[] getOuterDescriptorLoop()
    {
        /* Get descriptor loop info from native SI DB */
        return outerDescriptors;
    }

    /**
     * Get elementary stream information blocks. Each block contains elementary
     * stream data for a particular stream type.
     * 
     * @return The elementary stream information blocks.
     */
    public PMTElementaryStreamInfo[] getPMTElementaryStreamInfoLoop()
    {
        /* Get elementary stream info from native SI DB */
        return esInfo;
    }

    /**
     * Returns the table_id field of the table. Eight bit field that identifies
     * the table.
     * 
     * @return The table Id.
     */
    public short getTableId()
    {
        return PMT_TABLE_ID;
    }

    /**
     * Reports the <code>Locator</code> of this <code>SIElement</code>.
     * 
     * @return Locator The locator referencing this <code>SIElement</code>
     */
    public Locator getLocator()
    {
        return locator;
    }

    /**
     * Tests two <code>SIElement</code> objects for equality. Returns
     * <code>true</code> if and only if:
     * <ul>
     * <li><code>obj</code>'s class is the same as the class of this
     * <code>SIElement</code>, and
     * <p>
     * <li><code>obj</code>'s <code>Locator</code> is equal to the
     * <code>Locator</code> of this object (as reported by
     * <code>SIElement.getLocator()</code>, and
     * <p>
     * <li><code>obj</code> and this object encapsulate identical data.
     * </ul>
     * 
     * @param obj
     *            The object against which to test for equality.
     * 
     * @return <code>true</code> if the two <code>SIElement</code> objects are
     *         equal; <code>false</code> otherwise.
     */
    public boolean equals(Object obj)
    {
        // Check same class
        //findbugs detected - contract for equals requires that a null argument return false.
        if (obj != null && this.getClass().getName().equals(obj.getClass().getName()))
        {
            ProgramMapTableImpl pmt = (ProgramMapTableImpl) obj;

            // Check same locator
            if (this.getLocator().equals(pmt.getLocator()))
            {
                // Check same contents
                // TODO: Implement
            }
            return false;
        }
        return false;
    }

    /**
     * Reports the hash code value of this <code>SIElement</code>. Two
     * <code>SIElement</code> objects that are equal will have identical hash
     * codes.
     * 
     * @return The hash code value of this <code>SIElement</code>.
     */
    public int hashCode()
    {
        return getLocator().hashCode();
    }

    /**
     * Reports the SI format in which this <code>SIElement</code> was delivered.
     * 
     * @return The SI format in which this SI element was delivered.
     */
    public ServiceInformationType getServiceInformationType()
    {
        // TODO: Implement this -- information should be brought up from native
        return ServiceInformationType.UNKNOWN;
    }

    /**
     * Returns the time when this object was last updated from data in the
     * broadcast.
     * 
     * @return The date of the last update in UTC format, or <code>null</code>
     *         if unknown.
     */
    public Date getUpdateTime()
    {
        // TODO: Implement this -- information should be brought up from native
        return updateTime;
    }

    /**
     * Returns the SourceID of the service associated with this PMT. For dynamic
     * services (such as VOD), which are not signaled in the SVCT, this value
     * will return -1. For out-of-band PMT, this value will also return -1.
     * 
     * @return the SourceID of the service associated with this PMT
     */
    public int getSourceID()
    {
        return sourceID;
    }

    /**
     * Returns the frequency of the transport stream associated with this PMT.
     * For out-of-band PMT, this value will return -1.
     * 
     * @return the frequency of the transport stream associated with this PMT
     */
    public int getFrequency()
    {
        return frequency;
    }

    /**
     * Returns the handle associated with this PMT. The handle is used by the
     * SICache to uniquely identify the PMT
     * 
     * @return the handle associated with this PMT
     */
    public ProgramMapTableHandle getPMTHandle()
    {
        return pmtHandle;
    }

    public int getServiceHandle()
    {
        return pmtHandle.getHandle();
    }
    
    /**
     * Returns the transport stream associated with this PMT
     * 
     * @return the transport stream associated with this PMT
     */
    public TransportStream getTransportStream()
    {
        return transportStream;
    }

    /**
     * Used to set the locator that was used to request this PMT
     * 
     * @param locator
     *            the locator associated this PMT request
     */
    public void setLocator(Locator locator)
    {
        this.locator = locator;
    }

    private static final short PMT_TABLE_ID = 0x02;

    private int sourceID = -1;

    private int frequency = -1;

    private ProgramMapTableHandle pmtHandle = null;

    private TransportStream transportStream = null;

    private int programNumber = 0;

    private int pcrPID = 0;

    private Descriptor[] outerDescriptors = null;

    private PMTElementaryStreamInfo[] esInfo = null;

    /** Timestamp when this object was populated from the SI database */
    private Date updateTime = null;

    private Locator locator = null;
}
