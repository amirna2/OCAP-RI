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

package org.cablelabs.impl.util;

import javax.tv.service.navigation.ServiceComponent;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.service.SISnapshotManager;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;

/**
 * This class is used to encapsulate the Pid information needed by the native
 * implementation for conversion, time shift buffering operations.
 * 
 */
public class PidMapTable
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(PidMapTable.class.getName());

    private PidMapEntry[] pidMapEntryArray;

    private int pidTableSize = 0;

    private ServiceDetailsExt serviceDetails;

    private SISnapshotManager siSnapshot;

    public PidMapTable(int size)
    {
        pidTableSize = size;
        pidMapEntryArray = new PidMapEntry[pidTableSize];
    }

    public int getSize()
    {
        return pidTableSize;
    }

    public void setSize(int newSize)
    {
        if (newSize > pidMapEntryArray.length)
        {
            PidMapEntry[] newArray = new PidMapEntry[newSize];
            System.arraycopy(pidMapEntryArray, 0, newArray, 0, pidMapEntryArray.length);
            pidMapEntryArray = newArray;
        }
        pidTableSize = newSize;
    }

    public final void setServiceDetails(final ServiceDetailsExt serviceDetails)
    {
        // these exceptions can be thrown only if matchToEntries is true
        try
        {
            setServiceDetails(serviceDetails, false);
        }
        catch (SIRequestException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * @param serviceDetails
     *            The serviceDetails associated with the PidMapTable. Should be
     *            non-mutable
     * @param matchToEntries
     *            if {@link #pidMapEntryArray} is set set the service component
     *            reference of the matching {@link PidMapEntry}.
     * 
     * @throws InterruptedException
     * @throws SIRequestException
     */
    public final void setServiceDetails(final ServiceDetailsExt serviceDetails, boolean matchToEntries)
            throws SIRequestException, InterruptedException
    {
        this.serviceDetails = serviceDetails;
        if (matchToEntries)
        {
            /*
             * Match up the elementary streams from serviceDetails with the
             * PidMapTable. There will always be one more entry in the
             * PidMapTable then the service details since the PidMapTable had
             * PCR information.
             */
            if (pidMapEntryArray == null || pidMapEntryArray.length < 1)
            {
                throw new IllegalStateException("can't match to entries if the pidMapEntryArray is null or empty");
            }

            ServiceComponent[] serviceComponents = null;
            serviceComponents = this.serviceDetails.getComponents();

            if (log.isDebugEnabled())
            {
                log.debug(" PidMapTable setServiceDetails isAnalog: " + ((ServiceDetailsExt) serviceDetails).isAnalog());
            }
            if (log.isDebugEnabled())
            {
                log.debug(" PidMapTable setServiceDetails serviceComponents.length: " + serviceComponents.length);
            }

            /*
             * sanity check to make sure the # of service components is correct.
             */

            /*
             * if(pidMapEntryArray.length - 1 != serviceComponents.length ) {
             * throw new IllegalStateException(
             * "pidMap and service components don't have correct lengths.  pidMapEntryArray.length="
             * + pidMapEntryArray.length +
             * ", serviceComponents.length="+serviceComponents.length ); }
             */

            for (int i = 0; i < serviceComponents.length; i++)
            {
                ServiceComponentExt component = (ServiceComponentExt) serviceComponents[i];
                int componentPid = component.getPID();
                for (int j = 0; j < getSize(); j++)
                {
                    PidMapEntry entryAtIndex = getEntryAtIndex(j);
                    if (entryAtIndex.getSourcePID() == componentPid)
                    {
                        entryAtIndex.setServiceComponentReference(component);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return Returns the serviceDetails associated with the PidMapTable.
     */
    public final ServiceDetailsExt getServiceDetails()
    {
        return serviceDetails;
    }

    public void addEntryAtIndex(int index, PidMapEntry entry)
    {
        pidMapEntryArray[index] = entry;
    }

    public PidMapEntry getEntryAtIndex(int index)
    {
        if ((pidTableSize == 0) || (index >= pidTableSize)) return null;
        return pidMapEntryArray[index];
    }

    public PidMapEntry findEntryBySourcePID(int srcPID)
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getSourcePID() == srcPID))
                return pidMapEntryArray[i];
        }
        return null;
    }

    public PidMapEntry findEntryByRecordedPID(int recPID)
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getRecordedPID() == recPID))
                return pidMapEntryArray[i];
        }
        return null;
    }

    public PidMapEntry findEntryBySourcePIDSourceStreamType(int srcPID, short srcType)
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getSourcePID() == srcPID)
                    && (pidMapEntryArray[i].getSourceElementaryStreamType() == srcType)) return pidMapEntryArray[i];
        }
        return null;
    }

    public PidMapEntry findEntryByRecordedPIDRecordedStreamType(int recPID, short recType)
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getRecordedPID() == recPID)
                    && (pidMapEntryArray[i].getRecordedElementaryStreamType() == recType)) return pidMapEntryArray[i];
        }
        return null;
    }

    public boolean findEntry(PidMapEntry entry)
    {
        if (entry == null) return false;
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getSourcePID() == entry.getSourcePID())
                    && (pidMapEntryArray[i].getSourceElementaryStreamType() == entry.getSourceElementaryStreamType()))
                return true;
        }
        return false;
    }

    public PidMapEntry getPCRPidMapEntry()
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getStreamType() == MediaStreamType.PCR))
                return pidMapEntryArray[i];
        }
        return null;
    }

    public PidMapEntry getPMTPidMapEntry()
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getStreamType() == MediaStreamType.PMT))
                return pidMapEntryArray[i];
        }
        return null;
    }

    public PidMapEntry findEntryByServiceComponent(ServiceComponentExt sce)
    {
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ((pidMapEntryArray[i] != null) && (pidMapEntryArray[i].getServiceComponentReference() != null)
                    && (pidMapEntryArray[i].getServiceComponentReference().getID() == sce.getID()))
                return pidMapEntryArray[i];
        }
        return null;
    }

    /**
     * Count all Audio/Video streams in the table
     * 
     * @return A count of streams in the table with MediaStreamType.AUDIO or VIDEO
     */
    public int getAVEntryCount()
    {
        int count = 0;
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ( (pidMapEntryArray[i] != null) 
                 && ( (pidMapEntryArray[i].getStreamType() == MediaStreamType.AUDIO)
                      || (pidMapEntryArray[i].getStreamType() == MediaStreamType.VIDEO) ) )
            {
                count++;
            }

        }
        return count;
    }

    /**
     * Count all Audio/Video/Data streams in the table
     * 
     * @return A count of streams in the table with MediaStreamType.AUDIO, VIDEO, DATA, 
     *         SECTIONS, or SUBTITLES.
     */
    public int getAVDEntryCount()
    {
        int count = 0;
        for (int i = 0; i < pidMapEntryArray.length; i++)
        {
            if ( (pidMapEntryArray[i] != null) 
                 && ( (pidMapEntryArray[i].getStreamType() == MediaStreamType.AUDIO)
                      || (pidMapEntryArray[i].getStreamType() == MediaStreamType.VIDEO)
                      || (pidMapEntryArray[i].getStreamType() == MediaStreamType.SECTIONS)
                      || (pidMapEntryArray[i].getStreamType() == MediaStreamType.SUBTITLES)
                      || (pidMapEntryArray[i].getStreamType() == MediaStreamType.DATA) ) )
            {
                count++;
            }

        }
        return count;
    }

    public boolean equals(final Object obj)
    {
        // Make sure we have a good object
        if (obj == null || !(obj instanceof PidMapTable))
        {
            return false;
        }

        final PidMapTable table = (PidMapTable) obj;
        
        if (table.getSize() != this.getSize())
        {
            return false;
        }
        
        // Compare fields
        for (int i = 0; i < this.pidTableSize; i++)
        {
            PidMapEntry entry = table.getEntryAtIndex(i);
            if (entry == null) return false;
            // PidMapEntries are matched based upon
            // StreamType, sourceElementaryStreamType and
            // sourcePID fields..
            // Skip PCR entries
            if (this.getEntryAtIndex(i).getStreamType() != MediaStreamType.PCR)
            {
                if (this.getEntryAtIndex(i).equals(entry))
                    continue;
                else
                    return false;
            }
        }

        return true;
    }
    
    public int hashCode()
    {
        int hash = 7;
        for (int i = 0; i < this.pidTableSize; i++)
        {
            if (this.getEntryAtIndex(i).getStreamType() != MediaStreamType.PCR)
            {
                hash = 31 * hash + this.getEntryAtIndex(i).hashCode();
            }
        }
        return hash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("PidMapTable 0x");
        sb.append(Integer.toHexString(this.hashCode()));
        sb.append(":[");
        for (int pos=0; pos < pidTableSize; pos++)
        {
            sb.append(pos).append(":[");
            if (pidMapEntryArray[pos] == null)
            {
                sb.append("null");
            }
            else
            {
                sb.append("streamType: ")
                  .append(pidMapEntryArray[pos].getStreamType())
                  .append(", SrcElemType ")
                  .append(pidMapEntryArray[pos].getSourceElementaryStreamType())
                  .append(", SrcPID ")
                  .append(pidMapEntryArray[pos].getSourcePID())
                  .append(", RecElemType ")
                  .append(pidMapEntryArray[pos].getRecordedElementaryStreamType())
                  .append(", RecPID ")
                  .append(pidMapEntryArray[pos].getRecordedPID())
                  .append(", ServiceComp ")
                  .append(( (pidMapEntryArray[pos].getServiceComponentReference() != null) 
                            ? pidMapEntryArray[pos].getServiceComponentReference().toString()
                            : "null" ) );
            }
            sb.append(']');
            if (pos < pidTableSize-1)
            {
                sb.append(',');
            }
        } // END for

        sb.append(']');
        return sb.toString();
    }

    public void setSISnapshot(SISnapshotManager snapshot)
    {
        this.siSnapshot = snapshot;
    }

    public SISnapshotManager getSISnapshot()
    {
        return siSnapshot;
    }
} // END class PidMapTable
