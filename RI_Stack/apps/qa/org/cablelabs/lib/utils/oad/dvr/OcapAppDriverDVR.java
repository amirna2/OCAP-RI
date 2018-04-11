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

package org.cablelabs.lib.utils.oad.dvr;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.media.Player;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.InteractiveResourceUsage;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.dvr.CompleteRecordings;
import org.dvb.service.selection.DvbServiceContext;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.media.TimeShiftControl;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

/**
 * Purpose: This class contains methods defined in OcapAppDriverInterfaceDVR
 * which includes DVR related functionality for Ocap Xlets running on OCAP stack
 * with OCAP DVR extension supported.
*/
public class OcapAppDriverDVR implements OcapAppDriverInterfaceDVR,
                                         RecordingPlaybackListener,
                                         InteractiveResourceUsage
{
    /**
     * The Singleton instance of this class, as type OcapAppDriverInterfaceDVR
     * in order to restrict public method calls to those defined in the 
     * Interface. Note that other OcapAppDriver classes may have instances of the
     * implementation classes (OcapAppDriverCore for instance) in order to make
     * use of methods that are necessary for other classes in the package
     * org.cablelabs.lib.utils.oad and its sub-packages
     */
    private static OcapAppDriverDVR s_instance;
    
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(OcapAppDriverDVR.class);

    private int m_numTuners;

    // minimum and maximum TSB duration (in sec)
    private static final int MIN_TSB_DURATION = 3000;
    private static final int MAX_TSB_DURATION = 10000;

    // Recording Manager instance, request, and state
    private OcapRecordingManager m_recordingManager;
    private OcapRecordingRequest m_recordingRequest[];
    
    private Map m_bufferingRequests = new HashMap();
    
    private boolean m_dvrBufferingEnabled = true;
    
    private OcapAppDriverCore m_oadCore;
    private int m_currentRecordingIndex;

    private OcapAppDriverDVR()
    {
        if (log.isInfoEnabled())
        {
            log.info("OcapAppDriverDVR()");
        }
        m_oadCore = (OcapAppDriverCore) OcapAppDriverCore.getOADCoreInterface();
        m_oadCore.addInteractiveResourceUsage(this);
        
        // get the recording manager and add us as a listener
        m_recordingManager = (OcapRecordingManager)OcapRecordingManager.getInstance();
        
        // enabled buffering by default
        m_recordingManager.enableBuffering();             
        
        m_recordingManager.addRecordingPlaybackListener(this);
    }

    /**
     * Gets an instance of the OcapAppDriverDVR, but as a OcapAppDriverInterfaceDVR
     * to enforce that all methods be defined in the OcapAppDriverInterfaceDVR class.
     * Using lazy initialization since instantiation of this class requires an
     * OcapApDriverInterfaceCore parameter, and thus cannot be instantiated at class
     * loading.
    */
    public static OcapAppDriverInterfaceDVR getOADDVRInterface()
    {
        if (s_instance == null)
        {
            s_instance = new OcapAppDriverDVR();
        }
        return s_instance;
    }
    
    public void setNumTuners(int numTuners)
    {
        m_numTuners = numTuners;
        m_recordingRequest = new OcapRecordingRequest[m_numTuners];   
    }
    
    /**
     * Generates a String representation of a given ResourceUsage
     * Object.
     * 
     * @param ru the ResourceUsage Object for which to generate a String
     * 
     * @return a String representation of the given ResourceUsage Object, or
     * the result of the Objects toString() method if the ResourceUsage Object
     * is not one of the expected subtypes
     */
    public String stringForUsage(ResourceUsage ru)
    {
        String usageStr = null;
        if (ru instanceof RecordingResourceUsage)
        {
            RecordingResourceUsage rru = 
                (RecordingResourceUsage)ru;
            OcapRecordingRequest orr = (OcapRecordingRequest)
            rru.getRecordingRequest();
            RecordingSpec rs = orr.getRecordingSpec();

            String rsString;
            Date startTime = null;
            long duration = 0;
            if (rs instanceof LocatorRecordingSpec)
            {
                LocatorRecordingSpec lrs = (LocatorRecordingSpec)rs;
                rsString = "LocatorRecording " + lrs.getSource()[0];
                startTime = lrs.getStartTime();
                duration = lrs.getDuration();
            }
            else if (rs instanceof ServiceRecordingSpec)
            {
                ServiceRecordingSpec srs = (ServiceRecordingSpec)rs;
                rsString = "ServiceRecording " + srs.getSource().getLocator().toExternalForm();
                startTime = srs.getStartTime();
                duration = srs.getDuration();
            } 
            else if (rs instanceof ServiceContextRecordingSpec)
            {
                ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec)rs;
                rsString = "ServiceContextRecording " + scrs.getServiceContext().getService().getLocator().toExternalForm();
                startTime = scrs.getStartTime();
                duration = scrs.getDuration();
            } 
            else 
            {
                rsString = rs.toString();
            } 

            long remainingSeconds = 0;
            if (startTime != null)
            {
                remainingSeconds = 
                    ( (startTime.getTime() + duration)
                            - System.currentTimeMillis() ) / 1000;
            }
            return rsString + " (" + remainingSeconds + " seconds remaining)";
        }
       
        if (ru instanceof TimeShiftBufferResourceUsage)
        {
            TimeShiftBufferResourceUsage tsru = (TimeShiftBufferResourceUsage)ru;
            return "TSBResourceUsage (service " 
            + tsru.getService().getLocator().toExternalForm() 
            + ')';
        }
        
        return usageStr;
    }

    /**
     * This listener logs recording playback events, and updates the flag
     * indicating the playback has started.
     */
    public void notifyRecordingPlayback(ServiceContext context,
                                        int artificialCarouselID,
                                        int[] carouselIDs)
    {
        // get the class name w/o the package prefix
        String name = context.getClass().getName();
        int firstChar = name.lastIndexOf('.') + 1;

        if (0 < firstChar)
        {
            name = name.substring(firstChar);
        }

        if (null != carouselIDs)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recording playback event received for: " + name +
                         ", artificialCarouselID = " + artificialCarouselID);
            }

            for (int i = 0; i < carouselIDs.length; i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("carouselIDs[" + i + "] = " + carouselIDs[i]);
                }
            }
        }
        else if (log.isInfoEnabled())
        {
            log.info("Recording playback event received for: " + name);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    /// TSB
    //

    public boolean tsbControl(boolean enable)
    {
        ServiceContext serviceContext = m_oadCore.getPlaybackServiceContext();
        if (serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("tsbControl() - service context null");
            }
            return false;
        }
        // Enables TSB by setting a non-zero minimum and maximum durations
        // on the service context for the given tuner. Disables TSB by setting
        // the durations on the service context to 0.
        if (enable)
        {
            ((TimeShiftProperties) serviceContext).setMinimumDuration(
                                                             MIN_TSB_DURATION);
            ((TimeShiftProperties) serviceContext).setMaximumDuration(
                                                             MAX_TSB_DURATION);
        }
        else
        {
            ((TimeShiftProperties) serviceContext).setMinimumDuration(0);
            ((TimeShiftProperties) serviceContext).setMaximumDuration(0);
        }

        return true;
    }
    
    public boolean isTsbEnabled()
    {
        boolean bRetVal = true;
        ServiceContext serviceContext = m_oadCore.getPlaybackServiceContext();
        if (serviceContext != null)
        {
            if (0 == ((TimeShiftProperties) serviceContext).getMinimumDuration())
            {
                bRetVal = false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("isTsbEnabled() - null service context");
            }
            bRetVal = false;
        }

        return bRetVal;
    }
    
    public boolean isBufferingEnabled()
    {
        return m_dvrBufferingEnabled;
    }
    
    public void toggleBufferingRequest(int serviceIndex)
    {
        Vector servicesList = m_oadCore.getServicesList();
        if (serviceIndex < 0 || serviceIndex >= servicesList.size())
        {
            if (log.isDebugEnabled())
            {
                log.debug("toggleBufferingRequest() - Index " + serviceIndex + 
                        " is out of bounds");
            }
        }
        else
        {
            Service service = (Service)servicesList.get(serviceIndex);
            if (m_bufferingRequests.get(service) != null)
            {
                m_recordingManager.cancelBufferingRequest((BufferingRequest)m_bufferingRequests.get(
                        service));
                m_bufferingRequests.remove(service);
            }
            else
            {
                BufferingRequest bufferingRequest = BufferingRequest.createInstance(service, 
                        MIN_TSB_DURATION, MAX_TSB_DURATION, 
                        new ExtendedFileAccessPermissions(true,true,true,true,true,true,null,null));
                m_recordingManager.requestBuffering(bufferingRequest);
                m_bufferingRequests.put(service, bufferingRequest);
            }   
        }
    }
    
    public void toggleBufferingEnabled()
    {
        m_dvrBufferingEnabled = !m_dvrBufferingEnabled;
        
        if (m_dvrBufferingEnabled)
        {
            m_recordingManager.enableBuffering();
        }
        else
        {
            m_recordingManager.disableBuffering();
        }
    }
    
    public double getBufferTime(boolean startTime)
    {
        double errorValue = Double.NaN;
        float nanosPerSecond = 1000000000.0F;
        float millisPerSecond = 1000.0F;
        int precision = 100; // two digits
        float nanosPerMilli = 1000000000.0F;
        Player player = m_oadCore.getPlayer();
        DvbServiceContext dService = (DvbServiceContext)m_oadCore.getPlaybackServiceContext();
        if ((player != null) && (dService != null))
        {
            Service service = dService.getService();
            if (service instanceof RecordedService)
            {
                RecordedService recService = (RecordedService)service;
                if (startTime)
                {
                return Math.floor((recService.getFirstMediaTime().getNanoseconds() / nanosPerSecond)
                        * precision + .5)
                        / precision;
                }
                else
                {
                    return  Math.floor((recService.getRecordedDuration() / millisPerSecond) * precision
                            + .5)
                            / precision;
                }
            }
            else
            {
                TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                if (control != null)
                {
                    if (startTime)
                    {
                        return Math.floor((control.getBeginningOfBuffer().getNanoseconds() / nanosPerMilli)
                                * precision + .5)
                                / precision;
                    }
                    
                    else
                    {
                        return Math.floor((control.getEndOfBuffer().getNanoseconds() / nanosPerMilli) * precision
                                + .5)
                                / precision;
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("No timeshiftControl for player: " + player);
                    }
                    return errorValue;
                }
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("getBufferTime() - No serviceContext and/or player - unable to obtain buffer start time");
            }
            return errorValue;
        }
    }
    

    public long getDiskSpace()
    {
        long size = 0;
        StorageProxy[] sprox = StorageManager.getInstance().getStorageProxies();

        for (int i = 0; i < sprox.length; i++)
        {
            LogicalStorageVolume[] lsv = sprox[i].getVolumes();

            for (int j = 0; j < lsv.length; j++)
            {
                if (lsv[j] instanceof MediaStorageVolume)
                {
                    size += ((MediaStorageVolume) (lsv[j])).getFreeSpace();
                }
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("Disk space left is " + size + " bytes.");
        }

        return size;
    }

    
    public boolean checkDiskSpace(long fileSize)
    {
        boolean bRetVal = true;
        long size = getDiskSpace();

        if (size < fileSize)
        {
            if (log.isInfoEnabled())
            {
                log.info("Required " + fileSize +
                         " bytes of space. Currently only " + size +
                         " bytes left.");
            }

            bRetVal = false;
        }

        return bRetVal;
    }


    ////////////////////////////////////////////////////////////////////////
    /// Recording
    //

    public String getRecStateStr(int state)
    {
        switch (state)
        {
            case LeafRecordingRequest.COMPLETED_STATE:
                return "COMPLETED_STATE";
            case LeafRecordingRequest.DELETED_STATE:
                return "DELETED_STATE";
            case LeafRecordingRequest.FAILED_STATE:
                return "FAILED_STATE";
            case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                return "IN_PROGRESS_INCOMPLETE_STATE";
            case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                return "IN_PROGRESS_INSUFFICIENT_SPACE_STATE";
            case LeafRecordingRequest.IN_PROGRESS_STATE:
                return "IN_PROGRESS_STATE";
            case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                return "IN_PROGRESS_WITH_ERROR_STATE";
            case LeafRecordingRequest.INCOMPLETE_STATE:
                return "INCOMPLETE_STATE";
            case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                return "PENDING_NO_CONFLICT_STATE";
            case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                return "PENDING_WITH_CONFLICT_STATE";
            default:
                return "UNKNOWN";

        }
    }
    
    public boolean recordTuner(int tunerIndex, long duration, long delay, boolean background)
    {
        ServiceContext serviceContext = m_oadCore.getPlaybackServiceContext();
        if (serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("recordTuner() - service context null");
            }
            return false;
        }
        
        boolean bRetVal = true;
        int resourcePriority = 0;
        String recordingName = new String("Playback Recording");
        MediaStorageVolume m_msv = null;
        long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration (seconds)
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              null,
                                              null);

        // start recording - adjusting for the delay factor
        final Date startTime =
                   new Date(System.currentTimeMillis() + (delay * 1000));

        // the duration also needs to be in msec
        duration *= 1000;

        int retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        byte recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;

        try
        {
            OcapRecordingProperties orp;

            // establish the recording properties
            orp = new OcapRecordingProperties(
                      OcapRecordingProperties.HIGH_BIT_RATE,// bitRate
                      expiration,                           // expirationPeriod
                      retentionPriority,                    // retentionPriority
                      recordingPriority,                    // priorityFlag
                      efap,                                 // access
                      m_oadCore.getOrganization(),        // organization
                      m_msv,                                // destination
                      resourcePriority);                    // resourcePriority
            RecordingSpec rs;

            // formulate the recording request
            if (background)
            {
                if (log.isInfoEnabled())
                {
                    log.info("RecordTuner() Starting " + duration/1000 +
                             " sec. background recording starting " +
                             delay + " seconds from now (" +
                             OcapAppDriverCore.SHORT_DATE_FORMAT.format(startTime) + ')' );
                }

                Service svc = serviceContext.getService();
                rs = new ServiceRecordingSpec(svc, startTime, duration, orp );
            }
            else
            {
                rs = new ServiceContextRecordingSpec(serviceContext,
                                                     startTime,
                                                     duration, orp);
                if (log.isInfoEnabled())
                {
                    log.info("RecordTuner() Starting " + duration/1000 +
                             " sec. foreground recording starting " +
                             delay + " seconds from now (" +
                             OcapAppDriverCore.SHORT_DATE_FORMAT.format(startTime) + ')' );
                }
            }

            // submit the recording request
            m_recordingRequest[tunerIndex] =
                (OcapRecordingRequest) m_recordingManager.record(rs);

            if (null != m_recordingRequest[tunerIndex])
            {
                // Add app specific data
                m_recordingRequest[tunerIndex].addAppData("delayDuration", ""+delay);
                m_recordingRequest[tunerIndex].addAppData("tunerIndex", ""+tunerIndex);
                m_recordingRequest[tunerIndex].addAppData("recordingMode", ""+background);
                if (log.isInfoEnabled())
                {
                    log.info("***" + recordingName + " scheduled as " +
                             m_recordingRequest[tunerIndex].toString() + "***");
                }
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("RecordTuner(): FAILURE due to exception", e);
            }

            bRetVal = false;
        }

        return bRetVal;
    }
    
    public boolean recordService(int serviceIndex, long duration,
                                 long delay, boolean background)
    {
        Vector services = m_oadCore.getServicesList();
        Service svc = (Service) services.elementAt(serviceIndex);
        boolean bRetVal = true;
        int resourcePriority = 0;
        int tunerIndex = 0;
        String recordingName =
                        new String("Service[" + serviceIndex + "] Recording");
        MediaStorageVolume m_msv = null;
        long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration (seconds)
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              null,
                                              null);

        // start recording - adjusting for the delay factor
        final Date startTime =
                   new Date(System.currentTimeMillis() + (delay * 1000));

        // the duration also needs to be in msec
        duration *= 1000;

        int retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        byte recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;

        try
        {
            OcapRecordingProperties orp;

            // establish the recording properties
            orp = new OcapRecordingProperties(
                      OcapRecordingProperties.HIGH_BIT_RATE,// bitRate
                      expiration,                           // expirationPeriod
                      retentionPriority,                    // retentionPriority
                      recordingPriority,                    // priorityFlag
                      efap,                                 // access
                      m_oadCore.getOrganization(),        // organization
                      m_msv,                                // destination
                      resourcePriority);                    // resourcePriority
            RecordingSpec rs = null;
            ServiceContext serviceContext = null;

            // formulate the recording request
            if (background)
            {
                if (log.isInfoEnabled())
                {
                    log.info("RecordService() Starting " + duration/1000 +
                             " sec. background recording starting " +
                             delay + " seconds from now (" +
                             OcapAppDriverCore.SHORT_DATE_FORMAT.format(startTime) + ')' );
                }

                tunerIndex = 1;
                rs = new ServiceRecordingSpec(svc, startTime, duration, orp );
            }
            else
            {
                serviceContext = m_oadCore.getPlaybackServiceContext();
                
                if (null != serviceContext)
                {
                    rs = new ServiceContextRecordingSpec(serviceContext,
                                                         startTime,
                                                         duration, orp);
                    if (log.isInfoEnabled())
                    {
                        log.info("RecordService() Starting " + duration/1000 +
                                 " sec. foreground recording starting " +
                                 delay + " seconds from now (" +
                                 OcapAppDriverCore.SHORT_DATE_FORMAT.format(startTime) + ')' );
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("ERROR: no tuners tuned to: " + svc.getName());
                    }
                }
            }

            if (null != rs)
            {
                // submit the recording request
                m_recordingRequest[tunerIndex] =
                    (OcapRecordingRequest) m_recordingManager.record(rs);

                if (null != m_recordingRequest[tunerIndex])
                {
                    // Add app specific data
                    m_recordingRequest[tunerIndex].addAppData("delayDuration", ""+delay);
                    m_recordingRequest[tunerIndex].addAppData("tunerIndex", ""+tunerIndex);
                    m_recordingRequest[tunerIndex].addAppData("recordingMode", ""+background);
                    if (log.isInfoEnabled())
                    {
                        log.info("***" + recordingName + " scheduled as " +
                             m_recordingRequest[tunerIndex].toString() + "***");
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("RecordService(): FAILURE due to exception", e);
            }

            bRetVal = false;
        }

        return bRetVal;
    }
    
    public boolean waitForRecordingState(int tunerIndex, long timeout,
                                         int recordingState)
    {
        boolean bRetVal = false;
        String desiredStateStr = getRecStateStr(recordingState);

        // wait for recording to reach the given state
        int currentState= getCurrentRecordingState(tunerIndex);
        while (0 < timeout--)
        {
            currentState= getCurrentRecordingState(tunerIndex);
            String stateString = getRecStateStr(currentState);

            if (log.isInfoEnabled())
            {
                log.info("waitForRecordingState() - waiting for state: " + desiredStateStr + 
                        ", current state: " + stateString + ", timeout = " + timeout);
            }

            if (currentState == recordingState)
            {
                bRetVal = true;
                break;
            }

            // sleep for 1 second
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException iex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("waitForRecordingState() - interrupted");
                }
            }
        }

        // if we have waited long enough...
        if (-1 == timeout)
        {
            if (log.isInfoEnabled())
            {
                log.info("waitForRecordingState() - Desired recording state = " + recordingState + " - " + 
                        getRecStateStr(recordingState) +
                         ", not reached within " + timeout + "secs, current state = " + 
                         currentState + " - " + getRecStateStr(currentState));
            }
        }

        return bRetVal;
    }
    
    public boolean recordingStop(int tunerIndex)
    {
        boolean bRetVal = false;

        try
        {
            m_recordingRequest[tunerIndex].cancel();

            if (log.isInfoEnabled())
            {
                log.info("Recording on tuner[" + tunerIndex + "] stopped");
            }

            bRetVal = true;
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception occurred stopping recording request", e);
            }
        }

        return bRetVal;
    }

    public int getNumRecordings()
    {
        return (m_recordingManager.getEntries(new CompleteRecordings()).size());
    }

    public String getRecordingInfo(int recordingIndex)
    {
        String retVal = "not found";
        RecordingList list = m_recordingManager.getEntries(
                                                new CompleteRecordings());
        if (list.size() <= 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("ERROR: No recordings found!");
            }
        }
        else
        {
            OcapRecordingRequest orr =
                (OcapRecordingRequest) list.getRecordingRequest(recordingIndex);

            try
            {
                retVal = orr.getService().toString();
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Exception getting info", e);
                }
            }
        }

        return retVal;
    }
    
    public Service getRecordedService(int recordingIndex)
    {
        RecordingList list = m_recordingManager.getEntries(
                new CompleteRecordings());
        if (list.size() <= 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("ERROR: No recordings found!");
            }
        }
        else
        {
            OcapRecordingRequest orr =
                    (OcapRecordingRequest) list.getRecordingRequest(recordingIndex);

            // get the recording duration (in msec) based on the type of
            // recordingSpec
            try
            {
                return orr.getService();
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Exception getting duration", e);
                }
            }
        }

        return null;
    }
    
    public long getRecordingDuration(int recordingIndex)
    {
        long retVal = -1;
        RecordingList list = m_recordingManager.getEntries(
                                                new CompleteRecordings());
        if (list.size() <= 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("ERROR: No recordings found!");
            }
        }
        else
        {
            OcapRecordingRequest orr =
                (OcapRecordingRequest) list.getRecordingRequest(recordingIndex);

            // get the recording duration (in msec) based on the type of
            // recordingSpec
            try
            {
                RecordedService rs = orr.getService();
                retVal = (rs.getRecordedDuration()/1000); // convert to seconds
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Exception getting duration", e);
                }
            }
        }

        return retVal;
    }
    
    public int getCurrentRecordingState(int tunerIndex)
    {
        return m_recordingRequest[tunerIndex].getState();
    }
    
    public long getCurrentRecordingDuration(int tunerIndex)
    {
        long retVal = -1;

        // get the recording duration (in msec) based on the type of
        // recordingSpec
        try
        {
            RecordedService rs = m_recordingRequest[tunerIndex].getService();
            retVal = (rs.getRecordedDuration() / 1000); // convert to seconds
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception getting duration", e);
            }
        }

        return retVal;
    }
    
    public boolean deleteRecording(int recordingIndex)
    {
        boolean bRetVal = false;

        try
        {
            RecordingList recordings = m_recordingManager.getEntries(new CompleteRecordings());
            OcapRecordingRequest recording = (OcapRecordingRequest)recordings.getRecordingRequest(recordingIndex);
            recording.delete();
            
            bRetVal = true;

            if (log.isInfoEnabled())
            {
                log.info("Recording deleted successfully");
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception deleting recording", e);
            }
        }

        return bRetVal;
    }
    
    public boolean deleteAllRecordings()
    {
        boolean bRetVal = false;

        try
        {
            m_recordingManager.deleteAllRecordings();
            bRetVal = true;

            if (log.isInfoEnabled())
            {
                log.info("All recordings deleted successfully");
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception deleting all recordings", e);
            }
        }

        return bRetVal;
    }



    ////////////////////////////////////////////////////////////////////////
    /// DVR Local Playback
    //


    /**
     * {@inheritDoc}
     * This method should always be called before
     * playbackStart() when initiating DVR playback.
     */
    public boolean playbackStart(int recordingIndex, int waitTimeSecs)
    {
        boolean playbackCreated = false;
        RecordingList list = m_recordingManager.getEntries(
                                                new CompleteRecordings());
        if (list.size() <= 0)
        {
            if (log.isErrorEnabled())
            {
                log.error("createDvrPlayback: No recordings to playback!");
            }
        }
        else
        {
            OcapRecordingRequest orr = (OcapRecordingRequest)list.getRecordingRequest(recordingIndex);
            if (orr != null)
            {
                Service service = null;
                try
                {
                    service = orr.getService();
                    if (service != null)
                    {
                        if (m_oadCore.playbackStart(OcapAppDriverCore.PLAYBACK_TYPE_SERVICE, 
                                OcapAppDriverCore.PLAYBACK_CONTENT_TYPE_DVR, null, service, waitTimeSecs))
                        {
                            m_currentRecordingIndex = recordingIndex;
                            playbackCreated = true;
                        }
                        else
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("createDvrPlayback() - problems creating playback");
                            }
                        }
                    }
                    else
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("createDvrPlayback: recording request service was null");
                        }                        
                    }
                }
                catch (IllegalStateException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("createDvrPlayback: recording request in illegal state: ", e);
                    }                        
                }
                catch (AccessDeniedException ae)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("createDvrPlayback: recording request access denied: ", ae);
                    }                                            
                }
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("createDvrPlayback: recording request was null");
                }
            }
        }

        return playbackCreated;
    }

    public void playNext()
    {
        int maxItems = getNumRecordings();
        m_currentRecordingIndex = m_currentRecordingIndex == maxItems - 1 ? 0 : m_currentRecordingIndex + 1;
        if (m_oadCore.getPlaybackServiceContext() != null)
        {
            m_oadCore.getPlaybackServiceContext().select(getRecordedService(m_currentRecordingIndex));
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playNext() - service context null");
            }
        }
    }

    public void playPrevious()
    {
        int maxItems = getNumRecordings();
        m_currentRecordingIndex = m_currentRecordingIndex == 0 ? maxItems - 1 : m_currentRecordingIndex - 1;
        if (m_oadCore.getPlaybackServiceContext() != null)
        {
            m_oadCore.getPlaybackServiceContext().select(getRecordedService(m_currentRecordingIndex));
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playPrevious() - service context null");
            }
        }
    }
}

