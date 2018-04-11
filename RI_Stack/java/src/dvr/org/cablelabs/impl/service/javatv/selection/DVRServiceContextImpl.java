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

package org.cablelabs.impl.service.javatv.selection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.Time;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.recording.RecordingManagerInterface;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.DVREventMulticaster;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.shared.dvr.RecordingTerminatedEvent;

public class DVRServiceContextImpl extends ServiceContextImpl implements DVRServiceContextExt, TimeShiftProperties,
        DVRServiceContextDelegateListener
{
    private static final Logger log = Logger.getLogger(DVRServiceContextImpl.class);

    private final TimeShiftPropertiesHolder timeShiftPropertiesHolder = new TimeShiftPropertiesHolder();

    private static RecordingManagerInterface recordingManager;

    private static final CallerContextManager ccm =
        (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);

    public DVRServiceContextImpl(boolean destroyWhenIdle)
    {
        super(destroyWhenIdle);
    }

    public void addTimeShiftListener(TimeShiftListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addTimeShiftListener: " + listener + ": " + getDiagnosticInfo());
        }
        synchronized (mutex)
        {
            timeShiftPropertiesHolder.addTimeShiftListener(listener);
        }
    }

    public void removeTimeShiftListener(TimeShiftListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeTimeShiftListener: " + listener + ": " + getDiagnosticInfo());
        }
        timeShiftPropertiesHolder.removeTimeShiftListener(listener);
    }

    public long getMinimumDuration()
    {
        return timeShiftPropertiesHolder.getMinimumDuration();
    }

    public void setMinimumDuration(long minDuration)
    {
            // leaving as info since this enables the timeshift buffer if it's
            // non-zero
        if (log.isInfoEnabled())
        {
            log.info("setMinimumDuration: " + minDuration + ": " + getDiagnosticInfo());
        }
        timeShiftPropertiesHolder.setMinimumDuration(minDuration);
    }

    public long getMaximumDuration()
    {
        return timeShiftPropertiesHolder.getMaximumDuration();
    }

    public void setMaximumDuration(long maxDuration)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setMaximumDuration: " + maxDuration + ": " + getDiagnosticInfo());
        }
        timeShiftPropertiesHolder.setMaximumDuration(maxDuration);
    }

    public boolean getLastServiceBufferedPreference()
    {
        return timeShiftPropertiesHolder.getLastServiceBufferedPreference();
    }

    public void setLastServiceBufferedPreference(boolean buffer)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setLastServiceBufferedPreference: " + buffer + ": " + getDiagnosticInfo());
        }
        timeShiftPropertiesHolder.setLastServiceBufferedPreference(buffer);
    }

    public boolean getSavePreference()
    {
        return timeShiftPropertiesHolder.getSavePreference();
    }

    public void setSavePreference(boolean save)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setSavePreference: " + save + ": " + getDiagnosticInfo());
        }
        timeShiftPropertiesHolder.setSavePreference(save);
    }

    public void setPresentation(Service service, Time time, float rate, boolean action, boolean persistent)
    {
        timeShiftPropertiesHolder.setPresentation(service, time, rate, action, persistent);
    }

    public NetworkInterface getNetworkInterface(boolean presentation)
    {
        return timeShiftPropertiesHolder.getNetworkInterface(presentation);
    }

    public void setAvailableServiceContextDelegates(List serviceContextDelegates)
    {
        super.setAvailableServiceContextDelegates(serviceContextDelegates);
        if (log.isDebugEnabled())
        {
            log.debug("setAvailableServiceContextDelegates: " + serviceContextDelegates);
        }
        for (Iterator iter = serviceContextDelegates.iterator(); iter.hasNext();)
        {
            ServiceContextDelegate delegate = (ServiceContextDelegate) iter.next();
            if (delegate instanceof DVRServiceContextDelegate)
            {
                ((DVRServiceContextDelegate) delegate).setDVRServiceContextDelegateListener(this);
            }
        }
    }

    protected void cleanup(boolean cleanupCallerContextData)
    {
        synchronized (mutex)
        {
            super.cleanup(cleanupCallerContextData);
            timeShiftPropertiesHolder.cleanup();
        }
    }

    protected ServiceContextDelegate lookupDelegate(Service service)
    {
        ServiceContextDelegate delegateToUse = super.lookupDelegate(service);

        if (delegateToUse instanceof TimeShiftProperties)
        {
            if (log.isDebugEnabled())
            {
                log.debug("delegate to use is a TimeShiftProperties - update with current settings: "
                        + getDiagnosticInfo());
            }
            timeShiftPropertiesHolder.configureTimeShiftProperties((TimeShiftProperties) delegateToUse);
        }
        return delegateToUse;
    }

    public TimeShiftWindowClient getTimeShiftWindowClient()
    {
        DVRServiceContextDelegate tempDelegate = null;
        synchronized (mutex)
        {
            ServiceContextDelegate thisDelegate = getDelegate();
            if (thisDelegate instanceof DVRServiceContextDelegate)
            {
                tempDelegate = (DVRServiceContextDelegate) thisDelegate;
            }
        }
        return tempDelegate == null ? null : tempDelegate.getTimeShiftWindowClient();
    }

    public void requestBuffering()
    {
        DVRServiceContextDelegate tempDelegate = null;
        synchronized (mutex)
        {
            ServiceContextDelegate thisDelegate = getDelegate();
            if (thisDelegate instanceof DVRServiceContextDelegate)
            {
                tempDelegate = (DVRServiceContextDelegate) thisDelegate;
            }
        }
        if (tempDelegate != null)
        {
            tempDelegate.requestBuffering();
        }
    }

    public DVRPresentation getDVRPresentation(Service service)
    {
        return timeShiftPropertiesHolder.getDVRPresentation(service);
    }

    private RecordingManagerInterface getRecordingManager()
    {
        synchronized (DVRServiceContextImpl.class)
        {
            if (recordingManager == null)
            {
                recordingManager = (RecordingManagerInterface) ((org.cablelabs.impl.manager.RecordingManager) ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class)).getRecordingManager();
            }

            return recordingManager;
        }
    }

    public void recordingStopped(int reason)
    {
        if (log.isInfoEnabled())
        {
            log.info("recordingStopped - reason: " + reason + ": " + getDiagnosticInfo());
        }
        if (log.isInfoEnabled())
        {
            log.info("posting RecordingTerminatedEvent, reason " + reason + ": " + getDiagnosticInfo());
        }

        postEvent(new RecordingTerminatedEvent(this, reason));
    }

    public void notifyRecordingServiceContextPresenting(RecordingExt recordingExt,
            ServiceMediaHandler serviceMediaHandler)
    {
        recordingExt.notifyServiceContextPresenting(this, serviceMediaHandler);
    }

    public void notifyTimeShiftEvent(int timeShiftEventReasonCode)
    {
        timeShiftPropertiesHolder.notifyTimeShiftEvent(new TimeShiftEvent(this, timeShiftEventReasonCode));
        // TODO: post servicecontext event AND notify listeners?
    }

    private TimeShiftProperties getTimeShiftProperties()
    {
        synchronized (mutex)
        {
            ServiceContextDelegate thisDelegate = getDelegate();
            if (thisDelegate instanceof TimeShiftProperties)
            {
                return (TimeShiftProperties) thisDelegate;
            }
            return null;
        }
    }

    private class TimeShiftPropertiesHolder implements TimeShiftProperties
    {
        private final Map presentations = new HashMap();

        //explicitly defaulting to zero
        private long minimumDuration = 0;

        private long maximumDuration = 0;

        private boolean lastServiceBufferedPreference = false;

        private boolean savePreference = false;

        /**
         * List of <code>CallerContext</code>s that have added listeners.
         */
        private CallerContext ccList;
        
        private final Object lock = new Object();

        /**
         * Access this object's global data object associated with current context.
         * If none is assigned, then one is created.
         * <p>
         *
         * @param ctx the context to access
         * @return the <code>Data</code> object
         */
        private Data getData(CallerContext ctx)
        {
            synchronized (lock)
            {
                Data data = (Data) ctx.getCallbackData(this);
                if (data == null)
                {
                    data = new Data();
                    ctx.addCallbackData(data, this);
                    ccList = CallerContext.Multicaster.add(ccList, ctx);
                }
                return data;
            }
        }

        /**
         * Per-context global data. Remembers per-context
         * <code>TimeShiftListener</code>s.
         */
        private class Data implements CallbackData
        {
            public TimeShiftListener timeShiftListeners;

            public void destroy(CallerContext cc)
            {
                synchronized (lock)
                {
                    // Simply forget the given cc
                    // No harm done if never added
                    cc.removeCallbackData(TimeShiftPropertiesHolder.this);
                    ccList = CallerContext.Multicaster.remove(ccList, cc);
                }
            }

            public void active(CallerContext cc) { }
            public void pause(CallerContext cc) {  }
        }

        public void cleanup()
        {
            CallerContext ccListCopy;
            synchronized (lock)
            {
                ccListCopy = ccList;
                minimumDuration = 0;
                maximumDuration = 0;
                lastServiceBufferedPreference = false;
                savePreference = false;
                presentations.clear();
            }
            try
            {
                if (ccListCopy != null)
                {
                    ccListCopy.runInContextSync(new Runnable() {
                        public void run()
                        {
                            ccm.getCurrentContext();
                        }
                    });
                }
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Exception caught in cleanup, ignoring: " + getDiagnosticInfo(), e);
                }
            }
        }

        public void configureTimeShiftProperties(TimeShiftProperties properties)
        {
            long minimumDurationCopy;
            long maximumDurationCopy;
            boolean lastServiceBufferedPreferenceCopy;
            boolean savePreferenceCopy;
            synchronized (lock)
            {
                minimumDurationCopy = minimumDuration;
                maximumDurationCopy = maximumDuration;
                lastServiceBufferedPreferenceCopy = lastServiceBufferedPreference;
                savePreferenceCopy = savePreference;
            }
            properties.setMinimumDuration(minimumDurationCopy);
            properties.setMaximumDuration(maximumDurationCopy);
            properties.setLastServiceBufferedPreference(lastServiceBufferedPreferenceCopy);
            properties.setSavePreference(savePreferenceCopy);
        }

        public void addTimeShiftListener(TimeShiftListener addingTimeShiftListener)
        {
            if (log.isInfoEnabled())
            {
                log.info("adding TimeShiftListener: " + addingTimeShiftListener + ": " + getDiagnosticInfo());
            }
            synchronized (lock)
            {
                Data data = getData(ccm.getCurrentContext());
                data.timeShiftListeners = DVREventMulticaster.add(data.timeShiftListeners, addingTimeShiftListener);
    
                // TODO: this is currently a no-op, but would be necessary if we
                // want to notify after buffering has already started
                TimeShiftProperties currentProps = getTimeShiftProperties();
                if (currentProps != null)
                {
                    currentProps.addTimeShiftListener(addingTimeShiftListener);
                }
            }
        }

        public void removeTimeShiftListener(TimeShiftListener removingTimeShiftListener)
        {
            if (log.isInfoEnabled())
            {
                log.info("removing TimeShiftListener: " + removingTimeShiftListener + ": " + getDiagnosticInfo());
            }
            synchronized (lock)
            {
                CallerContext ctx = ccm.getCurrentContext();
                Data data = (Data)ctx.getCallbackData(this);
                if (data != null)
                {
                    if (data.timeShiftListeners != null)
                    {
                        data.timeShiftListeners = DVREventMulticaster.remove(data.timeShiftListeners, removingTimeShiftListener);
                    }
                    if (data.timeShiftListeners == null)
                    {
                        ctx.removeCallbackData(this);
                        ccList = CallerContext.Multicaster.remove(ccList, ctx);
                    }
                }
            }
        }

        public long getMinimumDuration()
        {
            synchronized (lock)
            {
                return minimumDuration;
            }
        }

        public void setMinimumDuration(long minimumDuration)
        {
            checkServiceContextPermission("*");
            boolean setMaxDuration = false;
            long maximumDurationCopy;
            synchronized (lock)
            {
                // minimum duration can be set without requiring maximum duration to
                // first be set
                if ((maximumDuration != 0) && (minimumDuration > maximumDuration))
                {
                    throw new IllegalArgumentException("Minimum duration (" + minimumDuration
                            + ") greater than maximum duration (" + maximumDuration + ")");
                }
                if ((minimumDuration != 0) && (minimumDuration < getRecordingManager().getSmallestTimeShiftDuration()))
                {
                    throw new IllegalArgumentException("Minimum duration " + minimumDuration
                            + " less than OcapRecordingManager.getSmallestTimeShiftDuration(): "
                            + getRecordingManager().getSmallestTimeShiftDuration());
                }
    
                this.minimumDuration = minimumDuration;
                if (minimumDuration == 0)
                {
                    // disabling buffering - set max duration also to zero
                    maximumDuration = 0;
                    setMaxDuration = true;
                }
                maximumDurationCopy = maximumDuration;
            }

            TimeShiftProperties currentProps = getTimeShiftProperties();
            // only allow immediate updates if minduration was updated to zero
            if (currentProps != null && minimumDuration == 0)
            {
                currentProps.setMinimumDuration(minimumDuration);
            }

            if (setMaxDuration)
            {
                if (currentProps != null)
                {
                    currentProps.setMaximumDuration(maximumDurationCopy);
                }
            }
        }

        public long getMaximumDuration()
        {
            synchronized (lock)
            {
                return maximumDuration;
            }
        }

        public void setMaximumDuration(long maximumDuration)
        {
            checkServiceContextPermission("*");
            synchronized (lock)
            {
                if (maximumDuration < minimumDuration)
                {
                    throw new IllegalArgumentException("Maximum duration less than minimum duration");
                }
    
                if ((maximumDuration != 0) && (maximumDuration < getRecordingManager().getSmallestTimeShiftDuration()))
                {
                    throw new IllegalArgumentException(
                            "Maximum duration less than OcapRecordingManager.getSmallestTimeShiftDuration(): "
                                    + getRecordingManager().getSmallestTimeShiftDuration());
                }
    
                this.maximumDuration = maximumDuration;
            }
            TimeShiftProperties currentProps = getTimeShiftProperties();
            if (currentProps != null)
            {
                currentProps.setMaximumDuration(maximumDuration);
            }
        }

        public boolean getLastServiceBufferedPreference()
        {
            synchronized (lock)
            {
                return lastServiceBufferedPreference;
            }
        }

        public void setLastServiceBufferedPreference(boolean lastServiceBufferedPreference)
        {
            checkServiceContextPermission("*");
            synchronized (lock)
            {
                this.lastServiceBufferedPreference = lastServiceBufferedPreference;
            }
            TimeShiftProperties currentProps = getTimeShiftProperties();
            if (currentProps != null)
            {
                currentProps.setLastServiceBufferedPreference(lastServiceBufferedPreference);
            }
        }

        public boolean getSavePreference()
        {
            synchronized (lock)
            {
                return savePreference;
            }
        }

        public void setSavePreference(boolean savePreference)
        {
            checkServiceContextPermission("*");

            if (savePreference)
            {
                throw new IllegalArgumentException("setSavePreference(true) not currently supported");
            }
            synchronized (lock)
            {
                this.savePreference = savePreference;
            }
            TimeShiftProperties currentProps = getTimeShiftProperties();
            if (currentProps != null)
            {
                currentProps.setSavePreference(savePreference);
            }
        }

        public void setPresentation(Service presentationService, Time presentationTime, float presentationRate,
                boolean presentationAction, boolean presentationPersistent)
        {
            checkServiceContextPermission("*");
            DVRPresentation presentation = new DVRPresentation(presentationService, presentationTime, presentationRate,
                    presentationAction, presentationPersistent);
            synchronized (lock)
            {
                // use service ID as key to map
                Object old = presentations.put(((ServiceExt) presentationService).getID(), presentation);
                if (log.isInfoEnabled())
                {
                    log.info("setPresentation - service: " + presentationService + " - set to: " + presentation
                            + ", replacing: " + old + ": " + getDiagnosticInfo());
                }
                if (log.isDebugEnabled())
                {
                    log.debug("services with presentations: " + presentations.keySet() + ": " + getDiagnosticInfo());
                }
            }
        }

        public NetworkInterface getNetworkInterface(boolean presentation)
        {
            // if delegate is a timeshiftproperties, pass the call to the
            // delegate
            TimeShiftProperties currentProps = getTimeShiftProperties();
            if (currentProps != null)
            {
                return currentProps.getNetworkInterface(presentation);
            }

            // no active delegate or delegate does not implement
            // TimeShiftProperties - returning null
            return null;
        }

        /**
         * Get a DVRPresentation for a service, if it was previously set via
         * setPresentation
         *
         * NOTE: Assuming this method called ONLY during the service selection
         * process, since it will remove the DVRPresentation entry in the map
         * associated with the service if the persistent flag is false
         *
         * @param service
         *            the service being presented
         * @return the last DVRPresentation set for the service
         */
        public DVRPresentation getDVRPresentation(Service service)
        {
            DVRServiceContextDelegate tempDelegate = null;
            ServiceContextDelegate thisDelegate = getDelegate();
            if (thisDelegate instanceof DVRServiceContextDelegate)
            {
                tempDelegate = (DVRServiceContextDelegate) thisDelegate;
            }

            DVRPresentation presentation;
            boolean updatePresentation = false;
            synchronized (lock)
            {
                // use service ID as key to map
                presentation = (DVRPresentation) presentations.get(((ServiceExt) service).getID());
                if (log.isInfoEnabled())
                {
                    log.info("getDVRPresentation for service: " + service + " - result: " + presentation + ": " + getDiagnosticInfo());
                }
                if (presentation != null)
                {
                    if (tempDelegate != null)
                    {
                        updatePresentation = true;
                    }
                    // remove from map if entry is not persistent
                    if (!presentation.getPersistent())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("DVRPresentation was not persistent - removing: " + presentation + " for service: "
                                    + service + ": " + getDiagnosticInfo());
                        }
                        presentations.remove(service);
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("services with presentations: " + presentations.keySet() + ": " + getDiagnosticInfo());
                }
            }
            if (updatePresentation)
            {
                tempDelegate.updatePresentation(presentation);
            }

            return presentation;
        }

        public void notifyTimeShiftEvent(final TimeShiftEvent timeShiftEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyTimeShiftEvent: " + timeShiftEvent + ", reason: " + timeShiftEvent.getReason() + ": " + getDiagnosticInfo());
            }

            CallerContext ctx;
            synchronized(lock)
            {
                ctx = ccList;
            }
            if (ctx != null)
            {
                ctx.runInContextAsync(new Runnable() {
                    public void run()
                    {
                        Data data = (Data) ccm.getCurrentContext().getCallbackData(TimeShiftPropertiesHolder.this);
                        if (data != null && data.timeShiftListeners != null)
                        {
                            data.timeShiftListeners.receiveTimeShiftevent(timeShiftEvent);
                        }
                    }
                });
            }
        }
    }
}
