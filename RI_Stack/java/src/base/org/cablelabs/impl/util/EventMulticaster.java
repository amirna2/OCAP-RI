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

import java.util.EventListener;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.tv.media.MediaSelectEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.davic.mpeg.sections.SectionFilterEvent;
import org.davic.mpeg.sections.SectionFilterListener;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabaseEvent;
import org.dvb.application.AppsDatabaseEventListener;
import org.dvb.dsmcc.StreamEvent;
import org.dvb.dsmcc.StreamEventListener;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.ui.TextOverflowListener;
import org.dvb.user.UserPreferenceChangeEvent;
import org.dvb.user.UserPreferenceChangeListener;
import org.havi.ui.HEventMulticaster;
import org.ocap.environment.EnvironmentEvent;
import org.ocap.environment.EnvironmentListener;
import org.ocap.hardware.PowerModeChangeListener;
import org.ocap.media.ClosedCaptioningEvent;
import org.ocap.media.ClosedCaptioningListener;
import org.ocap.media.VBIFilterEvent;
import org.ocap.media.VBIFilterListener;
import org.ocap.si.TableChangeListener;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.system.EASEvent;
import org.ocap.system.EASListener;

import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.service.SIChangedEvent;
import org.cablelabs.impl.service.SIChangedListener;
import org.cablelabs.impl.service.ServicesDatabase.ServiceChangeListener;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;

import org.apache.log4j.Logger;

/**
 * The <code>EventMulticaster</code> class is intended to assist the platform
 * implementation with the management of various event listeners.
 * <p>
 * The <code>EventMulticaster</code> class is meant to handle event dispatching
 * for the following events:
 * 
 * <ul>
 * <li> {@link org.dvb.application.AppStateChangeEvent AppStateChangeEvent}
 * <li> {@link org.dvb.application.AppsDatabaseEvent AppsDatabaseEvent}
 * <li> {@link org.dvb.event.UserEvent UserEvent}
 * <li> {@link org.cablelabs.impl.service.SIChangedEvent SIChangedEvent}
 * <li> {@link javax.tv.service.transport.NetworkChangeEvent NetworkChangeEvent}
 * <li> {@link javax.tv.service.transport.TransportStreamChangeEvent
 * TransportStreamChangeEvent}
 * <li> {@link javax.tv.service.transport.ServiceDetailsChangeEvent
 * ServiceDetailsChangeEvent}
 * <li> {@link javax.tv.service.navigation.ServiceComponentChangeEvent
 * ServiceComponentChangeEvent}
 * <li> {@link javax.tv.service.selection.ServiceContextEvent
 * ServiceContextEvent}
 * <li> {@link org.ocap.storage.StorageManagerEvent StorageManagerEvent}
 * <li> {@link javax.media.ControllerEvent ControllerEvent}
 * <li> {@link javax.tv.media.MediaSelectEvent MediaSelectEvent}
 * <li> {@link org.davic.net.tuning.NetworkInterfaceEvent NetworkInterfaceEvent}
 * <li> {@link org.ocap.media.ClosedCaptioningEvent ClosedCaptioningEvent}
 * <li> {@link org.cablelabs.impl.signalling.SignallingEvent}
 * <li> {@link org.cablelabs.impl.signalling.AbstractServiceEntry
 * AbstractServiceEntry}
 * <li> {@link org.dvb.user.UserPreferenceChangeListener
 * UserPreferenceChangeListener}
 * <li> {@link org.dvb.ui.TextOverflowListener TextOverflowListener}
 * <li> {@link org.dvb.ui.TextOverflowListener TextOverflowListener}
 * <li> {@link org.ocap.hardware.PowerModeChangeListener PowerModeChangeListener}
 * <li> {@link org.ocap.si.TableChangeListener TableChangeListener}
 * <li> {@link org.ocap.environment.EnvironmentListener EnvironmentListener }
 * </ul>
 * 
 * <p>
 * In addition to the events already handled by <code>HEventMulticaster</code>:
 * 
 * <ul>
 * <li>{@link org.havi.ui.event.HBackgroundImageEvent HBackgroundImageEvent}
 * <li>{@link org.havi.ui.event.HScreenConfigurationEvent
 * HScreenConfigurationEvent}
 * <li>{@link org.havi.ui.event.HScreenLocationModifiedEvent
 * HScreenLocationModifiedEvent}
 * <li>{@link org.havi.ui.event.HActionEvent HActionEvent}
 * <li>{@link org.havi.ui.event.HFocusEvent HFocusEvent}
 * <li>{@link org.havi.ui.event.HItemEvent HItemEvent}
 * <li>{@link org.havi.ui.event.HTextEvent HTextEvent}
 * <li>{@link org.havi.ui.event.HKeyEvent HKeyEvent}
 * <li>{@link org.havi.ui.event.HAdjustmentEvent HAdjustmentEvent}
 * <li>{@link java.awt.event.WindowEvent WindowEvent}
 * <li>{@link org.davic.resources.ResourceStatusEvent ResourceStatusEvent}
 * </ul>
 * 
 * <p>
 * Note, however, that <code>HEventMulticaster</code> handles listeners slightly
 * differently than this class does. The <code>HEventMulticaster</code>
 * implementation follows the <i>blind add</i> model implemented by
 * {@link java.awt.AWTEventMulticaster} which allows the same listener to be
 * added multiple times such that they must be removed an equal number of times.
 * On the other hand, this class, in supporting implementations of MHP, DAVIC,
 * and OCAP classes, remembers a given list only once no matter how many times
 * it is added (see MHP 11.2.7 and OCAP 13.3.1.4). As a consequence of this,
 * <code>EventMulticaster</code> also implements
 * {@link #add(ResourceStatusListener, ResourceStatusListener)}.
 * 
 * @see org.dvb.application.AppStateChangeEventListener
 * @see org.dvb.application.AppsDatabaseEventListener
 * @see org.dvb.event.UserEventListener
 * @see org.dvb.dsmcc.StreamEventListener
 * @see org.cablelabs.impl.service.ServicesDatabase.ServiceChangeListener
 * @see org.davic.net.tuning.NetworkInterfaceListener
 * @see org.ocap.media.ClosedCaptioningListener
 * @see org.cablelabs.impl.signalling.SignallingListener
 * @see org.cablelabs.impl.service.SIChangedListener
 * @see javax.tv.service.transport.NetworkChangeListener
 * @see javax.tv.service.transport.TransportStreamChangeListener
 * @see javax.tv.service.transport.ServiceDetailsChangeListener
 * @see javax.tv.service.navigation.ServiceComponentChangeListener
 * @see javax.tv.service.selection.ServiceContextListener
 * @see org.ocap.storage.StorageManagerListener
 * @see javax.media.ControllerListener
 * @see javax.tv.media.MediaSelectListener
 * 
 * @author Aaron Kamienski
 * @author Amir Nathoo - added ClosedCaptioningEvent
 * @author Todd Earles - added additional events
 * @author Jeff Spruiel - added RecordingPlaybackEvent
 * @author Eric Koldinger - added EnvironmentEvent
 */
public class EventMulticaster extends HEventMulticaster implements AppStateChangeEventListener,
        AppsDatabaseEventListener, UserEventListener, SIChangedListener, NetworkChangeListener, StreamEventListener,
        TransportStreamChangeListener, ServiceDetailsChangeListener, ServiceComponentChangeListener,
        ServiceContextListener, StorageManagerListener, ControllerListener, MediaSelectListener,
        NetworkInterfaceListener, ClosedCaptioningListener, SignallingListener, ServiceChangeListener,
        UserPreferenceChangeListener, ResourceStatusListener, TextOverflowListener, TableChangeListener,
        VBIFilterListener, SectionFilterListener, EASListener, PowerModeChangeListener, EnvironmentListener,
        PODListener
{

    private static final Logger log = Logger.getLogger(EventMulticaster.class);

    protected EventMulticaster(EventListener a, EventListener b)
    {
        super(a, b);
    }

    // This method returns the length of the EventMulticaster
    // by recursively descending the multicaster children and adding
    // their lengths. This was added to support debugging of listener
    // leaks.
    // public int multicasterLength()
    // {
    // int aSize = 0;
    // if (a == null)
    // aSize = 0;
    // else if (a instanceof EventMulticaster)
    // aSize = ((EventMulticaster)a).multicasterLength();
    // else
    // aSize = 1;
    //            
    // int bSize = 0;
    // if (b == null)
    // bSize = 0;
    // else if (b instanceof EventMulticaster)
    // bSize = ((EventMulticaster)b).multicasterLength();
    // else
    // bSize = 1;
    //        
    // return aSize + bSize;
    // }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new EventMulticaster instance which chains a with
     * b.
     * 
     * @param a
     *            event listener-a
     * @param b
     *            event listener-b
     */
    protected static EventListener addInternal(EventListener a, EventListener b)
    {
        if (a == null) return b;
        if (b == null) return a;
        return new EventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new EventMulticaster instance which chains a with
     * b.
     * <p>
     * This method is different from {@link #addInternal} in that it adds the
     * given listener only once!
     * 
     * @param a
     *            event listener-a
     * @param b
     *            event listener-b
     */
    protected static EventListener addOnceInternal(EventListener a, EventListener b)
    {
        // If a is empty just return b
        // If b already contains a, just return b
        if (a == null || contains(b, a)) return b;
        // If b is empty just return a
        // If a already contains b, just return a
        if (b == null || contains(a, b)) return a;
        return new EventMulticaster(a, b);
    }

    /**
     * Determines if <i>multi</i> is considered to contain <i>single</i>. This
     * will return <code>true</code> if <code>multi == single</code> or
     * <code>multi instanceof EventMulticaster</code> and any of the following
     * are true:
     * <ul>
     * <li> <code>multi.a == single</code>
     * <li> <code>multi.b == single</code>
     * <li> <code>contains(multi.a, single)</code>
     * <li> <code>contains(multi.b, single)</code>
     * </ul>
     * Otherwise, <code>false</code> is returned.
     * 
     * @param multi
     *            the EventListener/EventMulticaster to search
     * @param single
     *            the EventListener to search for
     * @return <code>true</code> if <i>multi</i> is considered to <i>contain</i>
     *         <i>single</i>, <code>false</code> otherwise
     */
    protected static boolean contains(EventListener multi, EventListener single)
    {
        if (multi == single)
            return true;
        else if (multi != null && multi instanceof EventMulticaster)
        {
            EventMulticaster m = (EventMulticaster) multi;
            return m.a == single || m.b == single || contains(m.a, single) || contains(m.b, single);
        }
        return false;
    }

    /**
     * Returns the resulting multicast listener after removing the old listener
     * from listener-l. If listener-l equals the old listener OR listener-l is
     * null, returns null. Else if listener-l is an instance of
     * EventMulticaster, then it removes the old listener from it. Else, returns
     * listener l.
     * 
     * @param l
     *            the listener being removed from
     * @param oldl
     *            the listener being removed
     */
    protected static EventListener removeInternal(EventListener l, EventListener oldl)
    {
        if (l == oldl || l == null)
        {
            return null;
        }
        else if (l instanceof EventMulticaster)
        {
            return ((EventMulticaster) l).remove(oldl);
        }
        else
        {
            return l;
        }
    }

    /**
     * Removes a listener from this multicaster and returns the result.
     * 
     * <p>
     * This is identical to the version in HEventMulticaster, but it must be
     * here so that it is compiled against EventMulticaster's versions of the
     * addInternal and removeInternal static methods.
     * 
     * @param oldl
     *            the listener to be removed
     */
    protected EventListener remove(EventListener oldl)
    {
        if (oldl == a) return b;
        if (oldl == b) return a;
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b)
        {
            return this;
        }
        return addInternal(a2, b2);
    }

    /**
     * Adds <i>EASListener-a</i> with <i>EASListener-b</i> and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            EASListener-a
     * @param b
     *            EASListener-b
     * @return the resulting multicast listener
     */
    public static EASListener add(EASListener a, EASListener b)
    {
        return (EASListener) addOnceInternal(a, b);
    }

    /**
     * Calls the warn method for the EASListener to send the event passed in.
     * 
     * @param event
     *            - the EAS event to send to the EAS Listener
     */
    public void warn(EASEvent event)
    {
        if (a != null) ((EASListener) a).warn(event);
        if (b != null) ((EASListener) b).warn(event);
    }

    /**
     * Calls the notify method for the EASListener to send the event passed in.
     * 
     * @param event
     *            - the EAS event to send to the EAS Listener
     */
    public void notify(EASEvent event)
    {
        if (a != null) ((EASListener) a).notify(event);
        if (b != null) ((EASListener) b).notify(event);
    }

    /**
     * Removes the old <code>EASListener-oldl</code> from <i>EASListener-l</i>
     * and returns the resulting multicast listener.
     * 
     * @param l
     *            EASListener-l
     * @param oldl
     *            the EASListener being removed
     * @return the resulting multicast listener
     */
    public static EASListener remove(EASListener l, EASListener oldl)
    {
        return (EASListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ClosedCaptioningListener-a</i> with
     * <i>ClosedCaptioningListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            ClosedCaptioningListener-a
     * @param b
     *            ClosedCaptioningListener-b
     * @return the resulting multicast listener
     */
    public static ClosedCaptioningListener add(ClosedCaptioningListener a, ClosedCaptioningListener b)
    {
        return (ClosedCaptioningListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ClosedCaptioningListener-oldl</code> from
     * <i>ClosedCaptioningListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            ClosedCaptioningListener-l
     * @param oldl
     *            the ClosedCaptioningListener being removed
     * @return the resulting multicast listener
     */
    public static ClosedCaptioningListener remove(ClosedCaptioningListener l, ClosedCaptioningListener oldl)
    {
        return (ClosedCaptioningListener) removeInternal(l, oldl);
    }

    /**
     */
    public static VBIFilterListener add(VBIFilterListener a, VBIFilterListener b)
    {
        return (VBIFilterListener) addOnceInternal(a, b);
    }

    /**
     */
    public static VBIFilterListener remove(VBIFilterListener l, VBIFilterListener oldl)
    {
        return (VBIFilterListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>AppStateChangeEventListener-a</i> with
     * <i>AppStateChangeEventListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            AppStateChangeEventListener-a
     * @param b
     *            AppStateChangeEventListener-b
     * @return the resulting multicast listener
     */
    public static AppStateChangeEventListener add(AppStateChangeEventListener a, AppStateChangeEventListener b)
    {
        return (AppStateChangeEventListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>AppStateChangeEventListener</code> from
     * <i>AppStateChangeEventListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            AppStateChangeEventListener-l
     * @param oldl
     *            the AppStateChangeEventListener being removed
     * @return the resulting multicast listener
     */
    public static AppStateChangeEventListener remove(AppStateChangeEventListener l, AppStateChangeEventListener oldl)
    {
        return (AppStateChangeEventListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>AppsDatabaseEventListener-a</i> with
     * <i>AppsDatabaseEventListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            AppsDatabaseEventListener-a
     * @param b
     *            AppsDatabaseEventListener-b
     * @return the resulting multicast listener
     */
    public static AppsDatabaseEventListener add(AppsDatabaseEventListener a, AppsDatabaseEventListener b)
    {
        return (AppsDatabaseEventListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>AppsDatabaseEventListener</code> from
     * <i>AppsDatabaseEventListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            AppsDatabaseEventListener-l
     * @param oldl
     *            the AppsDatabaseEventListener being removed
     * @return the resulting multicast listener
     */
    public static AppsDatabaseEventListener remove(AppsDatabaseEventListener l, AppsDatabaseEventListener oldl)
    {
        return (AppsDatabaseEventListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>UserEventListener-a</i> with <i>UserEventListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            UserEventListener-a
     * @param b
     *            UserEventListener-b
     * @return the resulting multicast listener
     */
    public static UserEventListener add(UserEventListener a, UserEventListener b)
    {
        // This specifically adds the listener once for each call
        return (UserEventListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>UserEventListener</code> from
     * <i>UserEventListener-l</i> and returns the resulting multicast listener.
     * 
     * @param l
     *            UserEventListener-l
     * @param oldl
     *            the UserEventListener being removed
     * @return the resulting multicast listener
     */
    public static UserEventListener remove(UserEventListener l, UserEventListener oldl)
    {
        return (UserEventListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>SIChangedListener-a</i> with <i>SIChangedListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            SIChangedListener-a
     * @param b
     *            SIChangedListener-b
     * @return the resulting multicast listener
     */
    public static SIChangedListener add(SIChangedListener a, SIChangedListener b)
    {
        return (SIChangedListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>SIChangedListener</code> from
     * <i>SIChangedListener-l</i> and returns the resulting multicast listener.
     * 
     * @param l
     *            SIChangedListener-l
     * @param oldl
     *            the SIChangedListener being removed
     * @return the resulting multicast listener
     */
    public static SIChangedListener remove(SIChangedListener l, SIChangedListener oldl)
    {
        return (SIChangedListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>NetworkChangeListener-a</i> with <i>NetworkChangeListener-b</i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            NetworkChangeListener-a
     * @param b
     *            NetworkChangeListener-b
     * @return the resulting multicast listener
     */
    public static NetworkChangeListener add(NetworkChangeListener a, NetworkChangeListener b)
    {
        return (NetworkChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>NetworkChangeListener</code> from
     * <i>NetworkChangeListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            NetworkChangeListener-l
     * @param oldl
     *            the NetworkChangeListener being removed
     * @return the resulting multicast listener
     */
    public static NetworkChangeListener remove(NetworkChangeListener l, NetworkChangeListener oldl)
    {
        return (NetworkChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>StreamEventListener-a</i> with <i>StreamEventListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            StreamEventListener-a
     * @param b
     *            StreamEventListener-b
     * @return the resulting multicast listener
     */
    public static StreamEventListener add(StreamEventListener a, StreamEventListener b)
    {
        return (StreamEventListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>StreamEventListener</code> from
     * <i>StreamEventListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            StreamEventListener-l
     * @param oldl
     *            the StreamEventListener being removed
     * @return the resulting multicast listener
     */
    public static StreamEventListener remove(StreamEventListener l, StreamEventListener oldl)
    {
        return (StreamEventListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>TransportStreamChangeListener-a</i> with
     * <i>TransportStreamChangeListener-b</i> and returns the resulting
     * multicast listener.
     * 
     * @param a
     *            TransportStreamChangeListener-a
     * @param b
     *            TransportStreamChangeListener-b
     * @return the resulting multicast listener
     */
    public static TransportStreamChangeListener add(TransportStreamChangeListener a, TransportStreamChangeListener b)
    {
        return (TransportStreamChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>TransportStreamChangeListener</code> from
     * <i>TransportStreamChangeListener-l</i> and returns the resulting
     * multicast listener.
     * 
     * @param l
     *            TransportStreamChangeListener-l
     * @param oldl
     *            the TransportStreamChangeListener being removed
     * @return the resulting multicast listener
     */
    public static TransportStreamChangeListener remove(TransportStreamChangeListener l,
            TransportStreamChangeListener oldl)
    {
        return (TransportStreamChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ServiceDetailsChangeListener-a</i> with
     * <i>ServiceDetailsChangeListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            ServiceDetailsChangeListener-a
     * @param b
     *            ServiceDetailsChangeListener-b
     * @return the resulting multicast listener
     */
    public static ServiceDetailsChangeListener add(ServiceDetailsChangeListener a, ServiceDetailsChangeListener b)
    {
        return (ServiceDetailsChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ServiceDetailsChangeListener</code> from
     * <i>ServiceDetailsChangeListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            ServiceDetailsChangeListener-l
     * @param oldl
     *            the ServiceDetailsChangeListener being removed
     * @return the resulting multicast listener
     */
    public static ServiceDetailsChangeListener remove(ServiceDetailsChangeListener l, ServiceDetailsChangeListener oldl)
    {
        return (ServiceDetailsChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ServiceComponentChangeListener-a</i> with
     * <i>ServiceComponentChangeListener-b</i> and returns the resulting
     * multicast listener.
     * 
     * @param a
     *            ServiceComponentChangeListener-a
     * @param b
     *            ServiceComponentChangeListener-b
     * @return the resulting multicast listener
     */
    public static ServiceComponentChangeListener add(ServiceComponentChangeListener a, ServiceComponentChangeListener b)
    {
        return (ServiceComponentChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ServiceComponentChangeListener</code> from
     * <i>ServiceComponentChangeListener-l</i> and returns the resulting
     * multicast listener.
     * 
     * @param l
     *            ServiceComponentChangeListener-l
     * @param oldl
     *            the ServiceComponentChangeListener being removed
     * @return the resulting multicast listener
     */
    public static ServiceComponentChangeListener remove(ServiceComponentChangeListener l,
            ServiceComponentChangeListener oldl)
    {
        return (ServiceComponentChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ServiceContextListener-a</i> with <i>ServiceContextListener-b</i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            ServiceContextListener-a
     * @param b
     *            ServiceContextListener-b
     * @return the resulting multicast listener
     */
    public static ServiceContextListener add(ServiceContextListener a, ServiceContextListener b)
    {
        return (ServiceContextListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ServiceContextListener</code> from
     * <i>ServiceContextListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            ServiceContextListener-l
     * @param oldl
     *            the ServiceContextListener being removed
     * @return the resulting multicast listener
     */
    public static ServiceContextListener remove(ServiceContextListener l, ServiceContextListener oldl)
    {
        return (ServiceContextListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>StorageManagerListener-a</i> with <i>StorageManagerListener-b</i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            StorageManagerListener-a
     * @param b
     *            StorageManagerListener-b
     * @return the resulting multicast listener
     */
    public static StorageManagerListener add(StorageManagerListener a, StorageManagerListener b)
    {
        return (StorageManagerListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>StorageManagerListener</code> from
     * <i>StorageManagerListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            StorageManagerListener-l
     * @param oldl
     *            the StorageManagerListener being removed
     * @return the resulting multicast listener
     */
    public static StorageManagerListener remove(StorageManagerListener l, StorageManagerListener oldl)
    {
        return (StorageManagerListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ControllerListener-a</i> with <i>ControllerListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            ControllerListener-a
     * @param b
     *            ControllerListener-b
     * @return the resulting multicast listener
     */
    public static ControllerListener add(ControllerListener a, ControllerListener b)
    {
        return (ControllerListener) addInternal(a, b);
    }

    /**
     * Removes the old <code>ControllerListener</code> from
     * <i>ControllerListener-l</i> and returns the resulting multicast listener.
     * 
     * @param l
     *            ControllerListener-l
     * @param oldl
     *            the ControllerListener being removed
     * @return the resulting multicast listener
     */
    public static ControllerListener remove(ControllerListener l, ControllerListener oldl)
    {
        return (ControllerListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>MediaSelectListener-a</i> with <i>MediaSelectListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            MediaSelectListener-a
     * @param b
     *            MediaSelectListener-b
     * @return the resulting multicast listener
     */
    public static MediaSelectListener add(MediaSelectListener a, MediaSelectListener b)
    {
        return (MediaSelectListener) addInternal(a, b);
    }

    /**
     * Removes the old <code>MediaSelectListener</code> from
     * <i>MediaSelectListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            MediaSelectListener-l
     * @param oldl
     *            the MediaSelectListener being removed
     * @return the resulting multicast listener
     */
    public static MediaSelectListener remove(MediaSelectListener l, MediaSelectListener oldl)
    {
        return (MediaSelectListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>NetworkInterfaceListener-a</i> with
     * <i>NetworkInterfaceListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            NetworkInterfaceListener-a
     * @param b
     *            NetworkInterfaceListener-b
     * @return the resulting multicast listener
     */
    public static NetworkInterfaceListener add(NetworkInterfaceListener a, NetworkInterfaceListener b)
    {
        return (NetworkInterfaceListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>NetworkInterfaceListener</code> from
     * <i>NetworkInterfaceListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            NetworkInterfaceListener-l
     * @param oldl
     *            the NetworkInterfaceListener being removed
     * @return the resulting multicast listener
     */
    public static NetworkInterfaceListener remove(NetworkInterfaceListener l, NetworkInterfaceListener oldl)
    {
        return (NetworkInterfaceListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>SignallingListener-a</i> with <i>SignallingListener-b</i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            SignallingListener-a
     * @param b
     *            SignallingListener-b
     * @return the resulting multicast listener
     */
    public static SignallingListener add(SignallingListener a, SignallingListener b)
    {
        return (SignallingListener) addInternal(a, b);
    }

    /**
     * Removes the old <code>SignallingListener</code> from
     * <i>SignallingListener-l</i> and returns the resulting multicast listener.
     * 
     * @param l
     *            SignallingListener-l
     * @param oldl
     *            the SignallingListener being removed
     * @return the resulting multicast listener
     */
    public static SignallingListener remove(SignallingListener l, SignallingListener oldl)
    {
        return (SignallingListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>SectionFilterListener </i> with <i>SectionFilterListener-b </i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            SectionFilterListener-a
     * @param b
     *            SectionFilterListener-b
     * @return the resulting multicast listener
     */
    public static SectionFilterListener add(SectionFilterListener a, SectionFilterListener b)
    {
        return (SectionFilterListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>SectionFilterListener-oldl</code> from
     * <i>SectionFilterListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            SectionFilterListener-l
     * @param oldl
     *            the SectionFilterListener being removed
     * @return the resulting multicast listener
     */
    public static SectionFilterListener remove(SectionFilterListener l, SectionFilterListener oldl)
    {
        return (SectionFilterListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ServiceChangeListener-a</i> with <i>ServiceChangeListener-b</i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            ServiceChangeListener-a
     * @param b
     *            ServiceChangeListener-b
     * @return the resulting multicast listener
     */
    public static ServiceChangeListener add(ServiceChangeListener a, ServiceChangeListener b)
    {
        return (ServiceChangeListener) addInternal(a, b);
    }

    /**
     * Removes the old <code>ServiceChangeListener</code> from
     * <i>ServiceChangeListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            ServiceChangeListener-l
     * @param oldl
     *            the ServiceChangeListener being removed
     * @return the resulting multicast listener
     */
    public static ServiceChangeListener remove(ServiceChangeListener l, ServiceChangeListener oldl)
    {
        return (ServiceChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>UserPreferenceChangeListener-a </i> with
     * <i>UserPreferenceChangeListener-b </i> and returns the resulting
     * multicast listener.
     * 
     * @param a
     *            UserPreferenceChangeListener-a
     * @param b
     *            UserPreferenceChangeListener-b
     * @return the resulting multicast listener
     */
    public static UserPreferenceChangeListener add(UserPreferenceChangeListener a, UserPreferenceChangeListener b)
    {
        return (UserPreferenceChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>UserPreferenceChangeListener-oldl</code> from
     * <i>UserPreferenceChangeListener-l </i> and returns the resulting
     * multicast listener.
     * 
     * @param l
     *            UserPreferenceChangeListener-l
     * @param oldl
     *            the UserPreferenceChangeListener being removed
     * @return the resulting multicast listener
     */
    public static UserPreferenceChangeListener remove(UserPreferenceChangeListener l, UserPreferenceChangeListener oldl)
    {
        return (UserPreferenceChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>ResourceStatusListener-a </i> with <i>ResourceStatusListener-b
     * </i> and returns the resulting multicast listener.
     * 
     * @param a
     *            ResourceStatusListener-a
     * @param b
     *            ResourceStatusListener-b
     * @return the resulting multicast listener
     */
    public static ResourceStatusListener add(ResourceStatusListener a, ResourceStatusListener b)
    {
        return (ResourceStatusListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ResourceStatusListener-oldl</code> from
     * <i>ResourceStatusListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            ResourceStatusListener-l
     * @param oldl
     *            the ResourceStatusListener being removed
     * @return the resulting multicast listener
     */
    public static ResourceStatusListener remove(ResourceStatusListener l, ResourceStatusListener oldl)
    {
        return (ResourceStatusListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>TextOverflowListener-a </i> with <i>TextOverflowListener-b </i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            TextOverflowListener-a
     * @param b
     *            TextOverflowListener-b
     * @return the resulting multicast listener
     */
    public static TextOverflowListener add(TextOverflowListener a, TextOverflowListener b)
    {
        return (TextOverflowListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>TextOverflowListener-oldl</code> from
     * <i>TextOverflowListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            TextOverflowListener-l
     * @param oldl
     *            the TextOverflowListener being removed
     * @return the resulting multicast listener
     */
    public static TextOverflowListener remove(TextOverflowListener l, TextOverflowListener oldl)
    {
        return (TextOverflowListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>TableChangeListener-a </i> with <i>TableChangeListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            TableChangeListener-a
     * @param b
     *            TableChangeListener-b
     * @return the resulting multicast listener
     */
    public static TableChangeListener add(TableChangeListener a, TableChangeListener b)
    {
        TableChangeListener tcl = (TableChangeListener) addOnceInternal(a, b);
        return tcl;
    }

    /**
     * Removes the old <code>TableChangeListener-oldl</code> from
     * <i>TableChangeListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            TableChangeListener-l
     * @param oldl
     *            the TableChangeListener being removed
     * @return the resulting multicast listener
     */
    public static TableChangeListener remove(TableChangeListener l, TableChangeListener oldl)
    {
        TableChangeListener tcl = (TableChangeListener) removeInternal(l, oldl);
        return tcl;
    }

    /**
     * Adds <i>PowerModeChangeListener-a </i> with <i>PowerModeChangeListener-b
     * </i> and returns the resulting multicast listener.
     * 
     * @param a
     *            PowerModeChangeListener-a
     * @param b
     *            PowerModeChangeListener-b
     * @return the resulting multicast listener
     */
    public static PowerModeChangeListener add(PowerModeChangeListener a, PowerModeChangeListener b)
    {
        return (PowerModeChangeListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>PowerModeChangeListener-oldl</code> from
     * <i>PowerModeChangeListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            PowerModeChangeListener-l
     * @param oldl
     *            the PowerModeChangeListener being removed
     * @return the resulting multicast listener
     */
    public static PowerModeChangeListener remove(PowerModeChangeListener l, PowerModeChangeListener oldl)
    {
        return (PowerModeChangeListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>PODListener-a </i> with <i>PODListener-b </i> and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            PODListener-a
     * @param b
     *            PODListener-b
     * @return the resulting multicast listener
     */
    public static PODListener add(PODListener a, PODListener b)
    {
        return (PODListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>PODListener-oldl</code> from <i>PODListener-l </i>
     * and returns the resulting multicast listener.
     * 
     * @param l
     *            PODListener-l
     * @param old1
     *            the PODListener being removed
     * @return the resulting multicast listener
     */
    public static PODListener remove(PODListener l, PODListener old1)
    {
        return (PODListener) removeInternal(l, old1);
    }

    // Description copied from ClosedCaptioningListener
    public void ccStatusChanged(ClosedCaptioningEvent event)
    {
        if (a != null) ((ClosedCaptioningListener) a).ccStatusChanged(event);
        if (b != null) ((ClosedCaptioningListener) b).ccStatusChanged(event);
    }

    // Description copied from VBIFilterListener
    public void filterUpdate(VBIFilterEvent event)
    {
        if (a != null) ((VBIFilterListener) a).filterUpdate(event);
        if (b != null) ((VBIFilterListener) b).filterUpdate(event);
    }

    // Description copied from AppStateChangeEventListener
    public void stateChange(AppStateChangeEvent event)
    {
        if (a != null) ((AppStateChangeEventListener) a).stateChange(event);
        if (b != null) ((AppStateChangeEventListener) b).stateChange(event);
    }

    // Description copied from UserEventListener
    public void userEventReceived(UserEvent event)
    {
        if (a != null) ((UserEventListener) a).userEventReceived(event);
        if (b != null) ((UserEventListener) b).userEventReceived(event);
    }

    // Description copied from AppsDatabaseEventListener
    public void newDatabase(AppsDatabaseEvent event)
    {
        if (a != null) ((AppsDatabaseEventListener) a).newDatabase(event);
        if (b != null) ((AppsDatabaseEventListener) b).newDatabase(event);
    }

    // Description copied from AppsDatabaseEventListener
    public void entryAdded(AppsDatabaseEvent event)
    {
        if (a != null) ((AppsDatabaseEventListener) a).entryAdded(event);
        if (b != null) ((AppsDatabaseEventListener) b).entryAdded(event);
    }

    // Description copied from AppsDatabaseEventListener
    public void entryRemoved(AppsDatabaseEvent event)
    {
        if (a != null) ((AppsDatabaseEventListener) a).entryRemoved(event);
        if (b != null) ((AppsDatabaseEventListener) b).entryRemoved(event);
    }

    // Description copied from AppsDatabaseEventListener
    public void entryChanged(AppsDatabaseEvent event)
    {
        if (a != null) ((AppsDatabaseEventListener) a).entryChanged(event);
        if (b != null) ((AppsDatabaseEventListener) b).entryChanged(event);
    }

    // Description copied from SIChangedListener
    public void notifyChanged(SIChangedEvent event)
    {
        if (a != null) ((SIChangedListener) a).notifyChanged(event);
        if (b != null) ((SIChangedListener) b).notifyChanged(event);
    }

    // Description copied from NetworkChangeListener
    public void notifyChange(NetworkChangeEvent event)
    {
        if (a != null) ((NetworkChangeListener) a).notifyChange(event);
        if (b != null) ((NetworkChangeListener) b).notifyChange(event);
    }

    // Description cipied from StreamEventListener
    public void receiveStreamEvent(StreamEvent event)
    {
        if (a != null) ((StreamEventListener) a).receiveStreamEvent(event);
        if (b != null) ((StreamEventListener) b).receiveStreamEvent(event);
    }

    // Description copied from TransportStreamChangeListener
    public void notifyChange(TransportStreamChangeEvent event)
    {
        if (a != null) ((TransportStreamChangeListener) a).notifyChange(event);
        if (b != null) ((TransportStreamChangeListener) b).notifyChange(event);
    }

    // Description copied from ServiceDetailsChangeListener
    public void notifyChange(ServiceDetailsChangeEvent event)
    {
        if (a != null) ((ServiceDetailsChangeListener) a).notifyChange(event);
        if (b != null) ((ServiceDetailsChangeListener) b).notifyChange(event);
    }

    // Description copied from ServiceComponentChangeListener
    public void notifyChange(ServiceComponentChangeEvent event)
    {
        if (a != null) ((ServiceComponentChangeListener) a).notifyChange(event);
        if (b != null) ((ServiceComponentChangeListener) b).notifyChange(event);
    }

    // Description copied from ServiceContextListener
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        if (a != null) ((ServiceContextListener) a).receiveServiceContextEvent(event);
        if (b != null) ((ServiceContextListener) b).receiveServiceContextEvent(event);
    }

    // Description copied from StorageManagerListener
    public void notifyChange(StorageManagerEvent event)
    {
        if (a != null) ((StorageManagerListener) a).notifyChange(event);
        if (b != null) ((StorageManagerListener) b).notifyChange(event);
    }

    // Description copied from ControllerListener
    public void controllerUpdate(ControllerEvent event)
    {
        if (a != null) ((ControllerListener) a).controllerUpdate(event);
        if (b != null) ((ControllerListener) b).controllerUpdate(event);
    }

    // Description copied from MediaSelectListener
    public void selectionComplete(MediaSelectEvent event)
    {
        if (a != null) ((MediaSelectListener) a).selectionComplete(event);
        if (b != null) ((MediaSelectListener) b).selectionComplete(event);
    }

    // Description copied from NetworkInterfaceListener
    public void receiveNIEvent(NetworkInterfaceEvent event)
    {
        if (a != null) ((NetworkInterfaceListener) a).receiveNIEvent(event);
        if (b != null) ((NetworkInterfaceListener) b).receiveNIEvent(event);
    }

    // Description copied from SectionFilterListener
    public void sectionFilterUpdate(SectionFilterEvent e)
    {
        if (a != null) ((SectionFilterListener) a).sectionFilterUpdate(e);
        if (b != null) ((SectionFilterListener) b).sectionFilterUpdate(e);
    }

    // Description copied from SignallingListener
    public void signallingReceived(SignallingEvent event)
    {
        if (a != null) ((SignallingListener) a).signallingReceived(event);
        if (b != null) ((SignallingListener) b).signallingReceived(event);
    }

    // Description copied from ServiceChangeListener
    public void serviceUpdate(AbstractServiceEntry entry)
    {
        if (a != null) ((ServiceChangeListener) a).serviceUpdate(entry);
        if (b != null) ((ServiceChangeListener) b).serviceUpdate(entry);
    }

    // Description copied from UserPreferenceChangeListener
    public void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent e)
    {
        if (a != null) ((UserPreferenceChangeListener) a).receiveUserPreferenceChangeEvent(e);
        if (b != null) ((UserPreferenceChangeListener) b).receiveUserPreferenceChangeEvent(e);
    }

    // Description copied from TextOverflowListener
    public void notifyTextOverflow(java.lang.String markedUpString, org.havi.ui.HVisible v,
            boolean overflowedHorizontally, boolean overflowedVertically)
    {
        if (a != null)
            ((TextOverflowListener) a).notifyTextOverflow(markedUpString, v, overflowedHorizontally,
                    overflowedVertically);
        if (b != null)
            ((TextOverflowListener) b).notifyTextOverflow(markedUpString, v, overflowedHorizontally,
                    overflowedVertically);
    }

    // PODListener
    public void notify(PODEvent event)
    {
        if (a != null)
        {
            ((PODListener) a).notify(event);
        }
        if (b != null)
        {
            ((PODListener) b).notify(event);
        }
    }

    // Description copied from TableChangeListener
    public void notifyChange(SIChangeEvent event)
    {
        if (a != null) ((TableChangeListener) a).notifyChange(event);
        if (b != null) ((TableChangeListener) b).notifyChange(event);
    }

    // Description copied from PowerModeChangeListner
    public void powerModeChanged(int newPowerMode)
    {
        if (a != null) ((PowerModeChangeListener) a).powerModeChanged(newPowerMode);
        if (b != null) ((PowerModeChangeListener) b).powerModeChanged(newPowerMode);
    }

    // Description copied from StorageManagerListener
    public void notify(EnvironmentEvent event)
    {
        if (a != null) ((EnvironmentListener) a).notify(event);
        if (b != null) ((EnvironmentListener) b).notify(event);
    }

    /**
     * Adds <i>TextOverflowListener-a </i> with <i>TextOverflowListener-b </i>
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            TextOverflowListener-a
     * @param b
     *            TextOverflowListener-b
     * @return the resulting multicast listener
     */
    public static EnvironmentListener add(EnvironmentListener a, EnvironmentListener b)
    {
        return (EnvironmentListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>TextOverflowListener-oldl</code> from
     * <i>TextOverflowListener-l </i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            TextOverflowListener-l
     * @param oldl
     *            the TextOverflowListener being removed
     * @return the resulting multicast listener
     */
    public static EnvironmentListener remove(EnvironmentListener l, EnvironmentListener oldl)
    {
        return (EnvironmentListener) removeInternal(l, oldl);
    }
}
