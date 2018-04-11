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
import org.ocap.net.OcapLocator;
import org.ocap.si.PATProgram;

import org.cablelabs.impl.service.OcapLocatorImpl;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;

/**
 * 
 * 
 * @author Greg Rutz
 */
public class ProgramAssociationTableImpl implements ProgramAssociationTableExt
{
    /**
     * Construct an PAT from the given byte data.
     * 
     * @param data
     *            the byte data
     * @param patHandle
     *            the handle used by SICache to uniquely identify this PAT
     */
    public ProgramAssociationTableImpl(byte[] data, TransportStream transportStream,
            ProgramAssociationTableHandle patHandle)
    {
        ByteArrayWrapper wrapper = new ByteArrayWrapper(data, 0);

        // Ignore this first integer. It is used exclusively by
        // SIDatabase/SICache
        ByteParser.getInt(wrapper);

        // Get tsId associated with this PAT
        tsId = ByteParser.getInt(wrapper);
        
        // Get frequency associated with this PAT
        frequency = ByteParser.getInt(wrapper);
                
        // Setup data arrays based on number of programs
        int numPrograms = ByteParser.getInt(wrapper);
        PATProgram programs[] = new PATProgram[numPrograms];
        sourceIDs = new int[numPrograms];

        // Build program list and sourceID list
        for (int i = 0; i < numPrograms; ++i)
        {
            programs[i] = new PATProgramImpl(ByteParser.getInt(wrapper), ByteParser.getInt(wrapper));
            sourceIDs[i] = ByteParser.getInt(wrapper);
        }
        this.programs = programs;

        // Create a locator for this PAT (may be overwrittern later)
        try
        {           
            if(frequency == -1)     
                locator = new OcapLocator(frequency, -1);  
            else if(frequency == OcapLocatorImpl.HN_FREQUENCY)
                locator = new OcapLocatorImpl("remoteservice://n=hnservice");
            else if(numPrograms > 0 && frequency > 0)
                locator = new OcapLocator(frequency, programs[0].getProgramNumber(), -1);
        }
        catch (InvalidLocatorException e)
        {
        }

        this.patHandle = patHandle;
        this.transportStream = transportStream;
    }

    /**
     * Get the identifier for this table.
     * 
     * @return The table identifier.
     */
    public short getTableId()
    {
        return PAT_TABLE_ID;
    }

    /**
     * Get the program loop in Program Association Table.
     * 
     * @return The list of PATProgram which represents the program loop in
     *         Program Association Table.
     */
    public PATProgram[] getPrograms()
    {
        return programs;
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
     * Returns all of the SourceIDs of the services associated with this PAT.
     * Any services associated with this PAT that are not signaled in the SVCT
     * (such as a VOD service) will not be represented in this array. For
     * out-of-band PAT, this method will return null.
     * 
     * @return an array of all SVCT-signaled services associated with this PAT.
     */
    public int[] getSourceIDs()
    {
        return sourceIDs;
    }

    /**
     * Returns the frequency of the transport stream associated with this PAT.
     * For out-of-band PAT, this method will return -1
     * 
     * @return the frequency of the transport stream associated with this PAT.
     */
    public int getFrequency()
    {
        return frequency;
    }

    /**
     * Returns the handle associated with this PAT. The handle is used by the
     * SICache to uniquely identify this PAT.
     * 
     * @return the handle associated with this PAT
     */
    public ProgramAssociationTableHandle getPATHandle()
    {
        return patHandle;
    }

    /**
     * Returns the transport stream associated with this PAT
     * 
     * @return the transport stream associated with this PAT
     */
    public TransportStream getTransportStream()
    {
        return transportStream;
    }
    
    /**
     * Returns the transport stream id associated with this PAT
     * 
     * @return the transport stream id associated with this PAT
     */
    public int getTransportStreamId()
    {        
        return tsId;
    }
    
    /**
     * Used to set the locator that was used to request this PAT
     * 
     * @param locator
     *            the locator associated this PAT request
     */
    public void setLocator(Locator locator)
    {
        this.locator = locator;
    }

    private static final short PAT_TABLE_ID = 0x00;

    private int[] sourceIDs = null;

    private int frequency = -1;
    
    private int tsId = -1;

    private ProgramAssociationTableHandle patHandle = null;

    private TransportStream transportStream = null;

    private PATProgram[] programs = null;

    private Locator locator = null;

    private Date updateTime = null;

}
