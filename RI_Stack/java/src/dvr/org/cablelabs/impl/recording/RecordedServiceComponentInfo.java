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

package org.cablelabs.impl.recording;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.StreamType;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.PidMapEntry;

/**
 * This class is used to encapsulate the persisted SI for
 * <code>ServiceComponent</code>s that are persisted as part of a recorded
 * service.
 * 
 * @see org.cablelabs.impo.recording.RecordingInfo
 * 
 * @author bgreene
 */
public class RecordedServiceComponentInfo implements Serializable
{
    private static final long serialVersionUID = 1291937944542816054L;

    // default values
    private String name = null;

    private String associatedLanguage = null;

    private transient Locator locator = null;

    private transient StreamType streamType = StreamType.VIDEO;

    private transient ServiceInformationType serviceInformationType = ServiceInformationType.UNKNOWN;

    private Date updateTime = null;

    private int PID = -1;

    private short elemStreamType = 0;

    /**
     * @return Returns the elemStreamType.
     */
    public short getElemStreamType()
    {
        return elemStreamType;
    }

    /**
     * @param elemStreamType
     *            The elemStreamType to set.
     */
    public void setElemStreamType(short elemStreamType)
    {
        this.elemStreamType = elemStreamType;
    }

    /**
     * @return Returns the pID.
     */
    public int getPID()
    {
        return PID;
    }

    /**
     * @param pid
     *            The pID to set.
     */
    public void setPID(int pid)
    {
        PID = pid;
    }

    // package private access for testing and RecordedInfo usage if required.
    public RecordedServiceComponentInfo()
    {
    }

    /**
     * Used to construct a RecordedServiceComponentInfo object based on the
     * fields in a ServiceComponent. This will allow the ServiceComponent to be
     * serialized with the rest of the recorded information for a recorded
     * service.
     * 
     * @param sc
     */
    public RecordedServiceComponentInfo(ServiceComponent sc)
    {
        this.name = sc.getName();
        this.associatedLanguage = sc.getAssociatedLanguage();
        this.locator = sc.getLocator();
        this.streamType = sc.getStreamType();
        this.serviceInformationType = sc.getServiceInformationType();
        this.updateTime = sc.getUpdateTime();
    }

    // Note: never called
    public RecordedServiceComponentInfo(String name, String associatedLanguage, Locator locator, StreamType streamType,
            ServiceInformationType serviceInformationType, Date updateTime, int pid, short elemStreamType)
    {
        this.name = name;
        this.associatedLanguage = associatedLanguage;
        this.locator = locator;
        this.streamType = streamType;
        this.serviceInformationType = serviceInformationType;
        this.updateTime = updateTime;
        this.PID = pid;
        this.elemStreamType = elemStreamType;
    }

    /**
     * Used to construct a RecordedServiceComponentInfo object based on the
     * fields in a PidMapTableEntry. This will allow the ServiceComponent to be
     * serialized with the rest of the recorded information for a recorded
     * service.
     * 
     * @param sc
     */
    public RecordedServiceComponentInfo(PidMapEntry pme)
    {
        ServiceComponentExt sce = pme.getServiceComponentReference();
        this.PID = pme.getRecordedPID();
        this.elemStreamType = pme.getRecordedElementaryStreamType();
        this.streamType = PidMapEntry.mediaStreamTypeToStreamType(pme.getStreamType());
        if (sce != null)
        {
            this.name = sce.getName();
            this.associatedLanguage = sce.getAssociatedLanguage();
            this.locator = sce.getLocator(); // FIX ME!!!
            this.serviceInformationType = sce.getServiceInformationType(); // FIX
                                                                           // ME!!!
            this.updateTime = new Date();
        }
        else
        {
            this.name = "";
            this.associatedLanguage = "";
            this.locator = null; // FIX ME!!!
            this.serviceInformationType = ServiceInformationType.SCTE_SI; // FIX
                                                                          // ME!!!
            this.updateTime = new Date(); // FIX ME!!!
        }
    }

    /**
     * Used to construct a RecordedServiceComponentInfo object from another
     * <code>RecordedServiceComponentInfo</code>. The ServiceComponent can be
     * serialized with the rest of the recorded information for a recorded
     * service.
     * 
     * @param sc
     */
    public RecordedServiceComponentInfo(RecordedServiceComponentInfo rsci)
    {
        this.name = rsci.name;
        this.associatedLanguage = rsci.associatedLanguage;
        this.streamType = rsci.streamType;
        this.serviceInformationType = rsci.serviceInformationType;

        if (rsci.locator != null)
        {
            try
            {
                this.locator = new OcapLocator(rsci.locator.toExternalForm());
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                this.locator = null;
            }
        }

        if (rsci.updateTime != null)
        {
            this.updateTime = new Date(rsci.updateTime.getTime());
        }
    }

    public boolean equals(Object o)
    {
        if (o == null || !(this.getClass().isInstance(o))) return false;
        RecordedServiceComponentInfo b = (RecordedServiceComponentInfo) o;
        // simple fields.
        return (equalObjects(this.name, b.name))
                && (equalObjects(this.associatedLanguage, b.associatedLanguage))
                && (equalObjects(this.updateTime, b.updateTime))
                && (equalObjects(this.streamType.toString(), b.streamType.toString())
                        && (equalObjects(this.locator, b.locator)) && (equalObjects(
                        this.serviceInformationType.toString(), b.serviceInformationType.toString())));
    }

    /**
     * compares 2 objects for equality - allows for 2 null objects to be equal
     * to one another.
     * 
     * @param a
     * @param b
     * @return
     */
    private boolean equalObjects(Object a, Object b)
    {
        if ((a == null && (b == null))) return true;
        if ((a == null) != (b == null)) return false;
        if (a != null && a.equals(b)) return true;
        return false;
    }

    /**
     * Provide for de-serialization of non-serializable objects contained
     * within. This takes care of reading <i>transient</i> fields from the given
     * <code>ObjectInputStream</code>.
     * <p>
     * This method is implemented for serialization only, not for general usage.
     * 
     * @param in
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // Read default fields
        in.defaultReadObject();

        // Read in non-serializable/transient fields
        // Locator
        locator = null;
        String loc = in.readUTF();
        // we're writing an empty string to the stream if locator is null in
        // writeObject
        if (!loc.equals(""))
        {
            try
            {
                locator = new OcapLocator(loc);
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                throw new IOException("Invalid locator in RecordingComponentInfo: " + loc);
            }
        }

        // service information type
        int siTypeInt = in.readInt();
        this.serviceInformationType = getServiceInformationType(siTypeInt);
        // stream type
        int streamTypeInt = in.readInt();
        this.streamType = getStreamType(streamTypeInt);
    }

    /**
     * Provide for serialization of non-serializable objects contained within.
     * This takes care of writing <i>transient</i> fields to the given
     * <code>ObjectOutputStream</code>.
     * <p>
     * This method is implemented for serialization only, not for general usage.
     * 
     * @param out
     * 
     * @see #appId
     * @see #fap
     * @see #serviceLocator
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Write default fields
        out.defaultWriteObject();
        // Write out non-serializable/transient fields
        if (locator != null)
        {
            out.writeUTF(locator.toExternalForm());
        }
        else
        {
            // write out an empty string - will be read in readObject and used
            // to ensure locator is still set as null
            out.writeUTF("");
        }
        // service information type
        out.writeInt(getServiceInformationTypeInt(serviceInformationType));
        // stream type
        out.writeInt(getStreamTypeInt(streamType));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (updateTime == null ? 0 : (int) (updateTime.getTime() ^ (updateTime.getTime() >>> 32)));
        hash = 31 * hash + (name == null ? 0 : name.hashCode());
        hash = 31 * hash + (associatedLanguage == null ? 0 : associatedLanguage.hashCode());
        hash = 31 * hash + (streamType == null ? 0 : streamType.toString().hashCode());
        hash = 31 * hash + (serviceInformationType == null ? 0 : serviceInformationType.toString().hashCode());
        hash = 31 * hash + (locator == null ? 0 : locator.toExternalForm().hashCode());
        
        return hash;
    }

    /**
     * Package private method to translate <code>int</code>s to
     * <code>StreamType</code>s. Used in serialization of
     * <code>RecordingComponentInfo</code>
     * 
     * @param streamType
     * @return
     */
    static StreamType getStreamType(int streamType)
    {
        switch (streamType)
        {
            case 1:
                return StreamType.AUDIO;
            case 2:
                return StreamType.DATA;
            case 3:
                return StreamType.SECTIONS;
            case 4:
                return StreamType.SUBTITLES;
            case 5:
                return StreamType.UNKNOWN;
            case 6:
                return StreamType.VIDEO;
            default:
                throw new IllegalArgumentException("Unknown int code: [" + streamType + "] for streamType.");
        }
    }

    /**
     * Package private method to translate <code>StreamType</code>s to
     * <code>int</code>s. Used in serialization of
     * <code>RecordingComponentInfo</code>
     * 
     * @param streamType
     * @return
     */
    static int getStreamTypeInt(StreamType streamType)
    {
        if (streamType.equals(StreamType.AUDIO))
            return 1;
        else if (streamType.equals(StreamType.DATA))
            return 2;
        else if (streamType.equals(StreamType.SECTIONS))
            return 3;
        else if (streamType.equals(StreamType.SUBTITLES))
            return 4;
        else if (streamType.equals(StreamType.UNKNOWN))
            return 5;
        else if (streamType.equals(StreamType.VIDEO))
            return 6;
        else
            throw new IllegalArgumentException("Unknown StreamType... "
                    + " did the JavaTV API add a new one as this method " + "supports all StreamTypes as of 6-21-05");
    }

    /**
     * Package private method to translate <code>int</code>s codes to
     * <code>ServiceInformationType</code>. Used in serialization of
     * <code>RecordingComponentInfo</code>
     * 
     * @param siType
     * @return
     */
    static ServiceInformationType getServiceInformationType(int siType)
    {
        switch (siType)
        {
            case 1:
                return ServiceInformationType.ATSC_PSIP;
            case 2:
                return ServiceInformationType.DVB_SI;
            case 3:
                return ServiceInformationType.SCTE_SI;
            case 4:
                return ServiceInformationType.UNKNOWN;
            default:
                throw new IllegalArgumentException("Unknown int code: [" + siType + "] for serviceInformationType.");
        }
    }

    /**
     * Used to convert <code>ServiceInformationType</code>s to <code>int</code>
     * s. Used in serialization of <code>RecordingComponentInfo</code>
     * 
     * @param siType
     * @return
     */
    static int getServiceInformationTypeInt(ServiceInformationType siType)
    {
        if (siType == null) return 4;
        if (siType.toString().equals(ServiceInformationType.ATSC_PSIP.toString()))
            return 1;
        else if (siType.toString().equals(ServiceInformationType.DVB_SI.toString()))
            return 2;
        else if (siType.toString().equals(ServiceInformationType.SCTE_SI.toString()))
            return 3;
        else if (siType.toString().equals(ServiceInformationType.UNKNOWN.toString()))
            return 4;
        else
            throw new IllegalArgumentException("Unknown ServiceInformationType... "
                    + " did the JavaTV API add a new one as this method "
                    + "supports all ServiceInformationType as of 6-21-05");
    }

    /**
     * @param updateTime
     *            The updateTime to set.
     */
    public void setUpdateTime(long updateTime)
    {
        this.updateTime = new Date(updateTime);
    }

    /**
     * @return Returns the associatedLanguage.
     */
    public String getAssociatedLanguage()
    {
        return associatedLanguage;
    }

    /**
     * @param associatedLanguage
     *            The associatedLanguage to set.
     */
    public void setAssociatedLanguage(String associatedLanguage)
    {
        this.associatedLanguage = associatedLanguage;
    }

    /**
     * @return Returns the locator.
     */
    public Locator getLocator()
    {
        return locator;
    }

    /**
     * @param locator
     *            The locator to set.
     */
    public void setLocator(Locator locator)
    {
        this.locator = locator;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the serviceInformationType.
     */
    public ServiceInformationType getServiceInformationType()
    {
        return serviceInformationType;
    }

    /**
     * @param serviceInformationType
     *            The serviceInformationType to set.
     */
    public void setServiceInformationType(ServiceInformationType serviceInformationType)
    {
        this.serviceInformationType = serviceInformationType;
    }

    /**
     * @return Returns the streamType.
     */
    public StreamType getStreamType()
    {
        return streamType;
    }

    /**
     * @param streamType
     *            The streamType to set.
     */
    public void setStreamType(StreamType streamType)
    {
        this.streamType = streamType;
    }

    /**
     * @return Returns the updateTime.
     */
    public Date getUpdateTime()
    {
        return updateTime;
    }

    /**
     * @param updateTime
     *            The updateTime to set.
     */
    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("RecordedServiceComponentInfo 0x")
                .append(Integer.toHexString(this.hashCode()))
                .append(":{pid 0x")
                .append(Integer.toHexString(PID))
                .append(",streamtype ")
                .append(streamType.toString())
                .append(",name ")
                .append((name == null) ? "null" : name)
                .append(",lang ")
                .append((associatedLanguage == null) ? "null" : associatedLanguage)
                .append(",updateTime ")
                .append(getUpdateTime()) 
                .append(serviceInformationType)
                .append(",source locator ")
                .append((locator == null) ? "null" : locator.toString())
                .append(",estype 0x")
                .append(Integer.toHexString(elemStreamType))
                .append('}');

        return sb.toString();
    }
}
