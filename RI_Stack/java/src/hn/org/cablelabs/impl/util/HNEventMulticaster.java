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

import org.davic.net.tuning.NetworkInterface;
import org.ocap.hn.ContentServerEvent;
import org.ocap.hn.ContentServerListener;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationListener;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceListener;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;
import org.cablelabs.impl.ocap.hn.AllStreamingActivityListenerImpl;
import org.cablelabs.impl.ocap.hn.ChannelStreamingActivityListenerImpl;
import org.cablelabs.impl.ocap.hn.RecordingStreamingActivityListenerImpl;

/**
 * The <code>HNEventMulticaster</code> extends <code>EventMulticaster</code>
 * and is intended to assist the platform implementation with the management of
 * various HN specific event listeners.
 * <p>
 * The <code>HNEventMulticaster</code> class is meant to handle event
 * dispatching for the following events:
 * 
 * <ul>
 * <li> {@link org.ocap.hn.DeviceEvent DeviceEvent}
 * <li> {@link org.ocap.hn.NetModuleEvent NetModuleEvent}
 * <li> {@link org.ocap.hn.ContentServerEvent ContentServerEvent}
 * <li> {@link org.ocap.hn.upnp.client.UPnPDeviceListener UPnPDeviceListener}
 * <li> {@link org.ocap.hn.upnp.client.UPnPStateVariableChangeListener UPnPStateVariableChangeListener}
 * <li> {@link org.ocap.hn.upnp.content.StreamingActivityListener StreamingActivityListener}
 * <li> {@link org.ocap.hn.upnp.server.UPnPManagedDeviceListener UPnPManagedDeviceListener}
 * </ul>
 * 
 * @see org.ocap.hn.DeviceEventListener
 * @see org.ocap.hn.NetModuleEventListener
 * @see org.ocap.hn.ContentServerListener
 * @see org.ocap.hn.upnp.client.UPnPDeviceListener
 * @see org.ocap.hn.upnp.client.UPnPStateVariableChangeListener
 * @see org.ocap.hn.upnp.content.StreamingActivityListener
 * @see org.ocap.hn.upnp.server.UPnPManagedDeviceListener
 */
public class HNEventMulticaster extends EventMulticaster 
    implements DeviceEventListener, NetModuleEventListener, ContentServerListener,
    UPnPClientDeviceListener, UPnPStateVariableListener, StreamingActivityListener,
    TransformationListener
    //, UPnPManagedDeviceListener
{
    protected HNEventMulticaster(EventListener a, EventListener b)
    {
        super(a, b);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new EventMulticaster instance which chains a with
     * b.
     * 
     * @param a event listener-a
     * @param b event listener-b
     */
    protected static EventListener addInternal(EventListener a, EventListener b)
    {
        if (a == null) return b;
        if (b == null) return a;
        return new HNEventMulticaster(a, b);
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
     * @param a event listener-a
     * @param b event listener-b
     */
    protected static EventListener addOnceInternal(EventListener a, EventListener b)
    {
        // If a is empty just return b
        // If b already contains a, just return b
        if (a == null || contains(b, a)) return b;
        // If b is empty just return a
        // If a already contains b, just return a
        if (b == null || contains(a, b)) return a;
        return new HNEventMulticaster(a, b);
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
     * Adds <i>DeviceEventListener-a </i> with <i>DeviceEventListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a DeviceEventListener-a
     * @param b DeviceEventListener-b
     * @return the resulting multicast listener
     */
    public static DeviceEventListener add(DeviceEventListener a, DeviceEventListener b)
    {
        return (DeviceEventListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>DeviceEventListener-oldl</code> from
     * <i>DeviceEventListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l DeviceEventListener-l
     * @param oldl the DeviceEventListener being removed
     * @return the resulting multicast listener
     */
    public static DeviceEventListener remove(DeviceEventListener l, DeviceEventListener oldl)
    {
        return (DeviceEventListener)removeInternal(l, oldl);
    }

    // Description copied from DeviceEventListener
    public void notify(DeviceEvent event)
    {
        if (a != null)
            ((DeviceEventListener)a).notify(event);
        if (b != null)
            ((DeviceEventListener)b).notify(event);
    }

    /**
     * Adds <i>NetModuleEventListener-a </i> with <i>NetModuleEventListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a NetModuleEventListener-a
     * @param b NetModuleEventListener-b
     * @return the resulting multicast listener
     */
    public static NetModuleEventListener add(NetModuleEventListener a, NetModuleEventListener b)
    {
        return (NetModuleEventListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>NetModuleEventListener-oldl</code> from
     * <i>NetModuleEventListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l DeviceEventListener-l
     * @param oldl the DeviceEventListener being removed
     * @return the resulting multicast listener
     */
    public static NetModuleEventListener remove(NetModuleEventListener l, NetModuleEventListener oldl)
    {
        return (NetModuleEventListener)removeInternal(l, oldl);
    }

    // Description copied from NetModuleEventListener
    public void notify(NetModuleEvent event)
    {
        if (a != null)
            ((NetModuleEventListener)a).notify(event);
        if (b != null)
            ((NetModuleEventListener)b).notify(event);
    }

    /**
     * Adds <i>ContentServerListener-a </i> with <i>ContentServerListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a ContentServerListener-a
     * @param b ContentServerListener-b
     * @return the resulting multicast listener
     */
    public static ContentServerListener add(ContentServerListener a, ContentServerListener b)
    {
        return (ContentServerListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ContentServerListener-oldl</code> from
     * <i>ContentServerListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l ContentServerListener-l
     * @param oldl the ContentServerListener being removed
     * @return the resulting multicast listener
     */
    public static ContentServerListener remove(ContentServerListener l, ContentServerListener oldl)
    {
        return (ContentServerListener)removeInternal(l, oldl);
    }

    // Description copied from NetModuleEventListener
    public void contentUpdated(ContentServerEvent event)
    {
        if (a != null)
            ((ContentServerListener)a).contentUpdated(event);
        if (b != null)
            ((ContentServerListener)b).contentUpdated(event);
    }

    /**
     * Adds <i>UPnPDeviceListener-a </i> with <i>UPnPDeviceListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a UPnPDeviceListener-a
     * @param b UPnPDeviceListener-b
     * @return the resulting multicast listener
     */
    public static UPnPClientDeviceListener add(UPnPClientDeviceListener a, UPnPClientDeviceListener b)
    {
        return (UPnPClientDeviceListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>UPnPDeviceListener-oldl</code> from
     * <i>UPnPDeviceListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l UPnPDeviceListener-l
     * @param oldl the UPnPDeviceListener being removed
     * @return the resulting multicast listener
     */
    public static UPnPClientDeviceListener remove(UPnPClientDeviceListener l, UPnPClientDeviceListener oldl)
    {
        return (UPnPClientDeviceListener)removeInternal(l, oldl);
    }

    // Description copied from UPnPDeviceListener
    public void notifyDeviceAdded(UPnPClientDevice device)
    {
        if (a != null)
            ((UPnPClientDeviceListener)a).notifyDeviceAdded(device);
        if (b != null)
            ((UPnPClientDeviceListener)b).notifyDeviceAdded(device);
    }

    // Description copied from UPnPDeviceListener
    public void notifyDeviceRemoved(UPnPClientDevice device)
    {
        if (a != null)
            ((UPnPClientDeviceListener)a).notifyDeviceRemoved(device);
        if (b != null)
            ((UPnPClientDeviceListener)b).notifyDeviceRemoved(device);
    }

    /**
     * Adds <i>UPnPStateVariableListener-a </i> with <i>UPnPStateVariableListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a UPnPStateVariableListener-a
     * @param b UPnPStateVariableListener-b
     * @return the resulting multicast listener
     */
    public static UPnPStateVariableListener add(UPnPStateVariableListener a, UPnPStateVariableListener b)
    {
        return (UPnPStateVariableListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>UPnPStateVariableListener-oldl</code> from
     * <i>UPnPStateVariableListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l UPnPStateVariableListener-l
     * @param oldl the UPnPStateVariableListener being removed
     * @return the resulting multicast listener
     */
    public static UPnPStateVariableListener remove(UPnPStateVariableListener l, UPnPStateVariableListener oldl)
    {
        return (UPnPStateVariableListener)removeInternal(l, oldl);
    }

    // Description copied from UPnPStateVariableListener
    public void notifyValueChanged(UPnPClientStateVariable variable)
    {
        if (a != null)
            ((UPnPStateVariableListener)a).notifyValueChanged(variable);
        if (b != null)
            ((UPnPStateVariableListener)b).notifyValueChanged(variable);
    }

    // Description copied from UPnPStateVariableListener
    public void notifySubscribed(UPnPClientService service)
    {
        if (a != null)
            ((UPnPStateVariableListener)a).notifySubscribed(service);
        if (b != null)
            ((UPnPStateVariableListener)b).notifySubscribed(service);
    }

    // Description copied from UPnPStateVariableChangeListener
    public void notifyUnsubscribed(UPnPClientService service)
    {
            if (a != null)
                ((UPnPStateVariableListener)a).notifyUnsubscribed(service);
            if (b != null)
                ((UPnPStateVariableListener)b).notifyUnsubscribed(service);
    }

    /**
     * Adds <i>RecordingStreamingActivityListenerImpl-a
     * </i> with <i>RecordingStreamingActivityListenerImpl-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a RecordingStreamingActivityListenerImpl-a
     * @param b RecordingStreamingActivityListenerImpl-b
     * @return the resulting multicast listener
     */
    public static RecordingStreamingActivityListenerImpl add(RecordingStreamingActivityListenerImpl a,
        RecordingStreamingActivityListenerImpl b)
    {
        return (RecordingStreamingActivityListenerImpl)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>RecordingStreamingActivityListenerImpl-oldl</code> from
     * <i>RecordingStreamingActivityListenerImpl-l </i> and returns the resulting multicast listener.
     * 
     * @param l RecordingStreamingActivityListenerImpl-l
     * @param oldl the DeviceEventListener being removed
     * @return the resulting multicast listener
     */
    public static RecordingStreamingActivityListenerImpl remove(RecordingStreamingActivityListenerImpl l,
        RecordingStreamingActivityListenerImpl oldl)
    {
        return (RecordingStreamingActivityListenerImpl)removeInternal(l, oldl);
    }

    /**
     * Adds <i>AllStreamingActivityListenerImpl-a
     * </i> with <i>AllStreamingActivityListenerImpl-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a AllStreamingActivityListenerImpl-a
     * @param b AllStreamingActivityListenerImpl-b
     * @return the resulting multicast listener
     */
    public static AllStreamingActivityListenerImpl add(AllStreamingActivityListenerImpl a,
        AllStreamingActivityListenerImpl b)
    {
        return (AllStreamingActivityListenerImpl)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>RecordingStreamingActivityListenerImpl-oldl</code> from
     * <i>RecordingStreamingActivityListenerImpl-l </i> and returns the resulting multicast listener.
     * 
     * @param l AllStreamingActivityListenerImpl-l
     * @param oldl the AllStreamingActivityListenerImpl being removed
     * @return the resulting multicast listener
     */
    public static AllStreamingActivityListenerImpl remove(AllStreamingActivityListenerImpl l,
        AllStreamingActivityListenerImpl oldl)
    {
        return (AllStreamingActivityListenerImpl)removeInternal(l, oldl);
    }
    /**
     * Adds <i>ChannelStreamingActivityListenerImpl-a
     * </i> with <i>ChannelStreamingActivityListenerImpl-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a ChannelStreamingActivityListenerImpl-a
     * @param b ChannelStreamingActivityListenerImpl-b
     * @return the resulting multicast listener
     */
    public static ChannelStreamingActivityListenerImpl add(ChannelStreamingActivityListenerImpl a,
        ChannelStreamingActivityListenerImpl b)
    {
        return (ChannelStreamingActivityListenerImpl)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>ChannelStreamingActivityListenerImpl-oldl</code> from
     * <i>RecordingStreamingActivityListenerImpl-l </i> and returns the resulting multicast listener.
     * 
     * @param l ChannelStreamingActivityListenerImpl-l
     * @param oldl the DeviceEventListener being removed
     * @return the resulting multicast listener
     */
    public static ChannelStreamingActivityListenerImpl remove(ChannelStreamingActivityListenerImpl l,
        ChannelStreamingActivityListenerImpl oldl)
    {
        return (ChannelStreamingActivityListenerImpl)removeInternal(l, oldl);
    }

    public void notifyStreamingStarted(ContentItem contentItem,
                                       int activityID,
                                       String URI,
                                       org.davic.net.tuning.NetworkInterface tuner)
    {
        if (a != null)
        {
            ((StreamingActivityListener)a).notifyStreamingStarted(contentItem,
                activityID, URI, tuner);
        }
        if (b != null)
        {
            ((StreamingActivityListener)b).notifyStreamingStarted(contentItem,
                activityID, URI, tuner);
        }


    }

    public void notifyStreamingChange(ContentItem contentItem,
                                       int activityID,
                                       String URI,
                                       org.davic.net.tuning.NetworkInterface tuner)
    {
        if (a != null)
        {
            ((StreamingActivityListener)a).notifyStreamingChange(contentItem,
                    activityID, URI, tuner);
        }
        if (b != null)
        {
            ((StreamingActivityListener)b).notifyStreamingChange(contentItem,
                activityID, URI, tuner);
        }

    }

    public void notifyStreamingEnded(ContentItem contentItem,
                                       int activityID,
                                       int reasonOfEnd)
    {
        if (a != null)
        {
            ((StreamingActivityListener)a).notifyStreamingEnded(contentItem,
                activityID, reasonOfEnd);
        }
        if (b != null)
        {
            ((StreamingActivityListener)b).notifyStreamingEnded(contentItem,
                activityID, reasonOfEnd);
        }

    }

    /**
     * Adds <i>TransformationListener-nsformationListenera </i> with <i>Listener-b
     * </i> and
     * returns the resulting multicast listener.
     * 
     * @param a TransformationListener-a
     * @param b TransformationListener-b
     * @return the resulting multicast listener
     */
    public static TransformationListener add(TransformationListener a, TransformationListener b)
    {
        return (TransformationListener)addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>TransformationListener-oldl</code> from
     * <i>TransformationListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l TransformationListener-l
     * @param oldl the TransformationListener being removed
     * @return the resulting multicast listener
     */
    public static TransformationListener remove(TransformationListener l, TransformationListener oldl)
    {
        return (TransformationListener)removeInternal(l, oldl);
    }

    public void notifyTransformationFailed(ContentItem contentItem, Transformation transform, int reasonCode)
    {
        if (a != null)
        {
            ((TransformationListener)a).notifyTransformationFailed(contentItem,
                 transform, reasonCode);
        }
        if (b != null)
        {
            ((TransformationListener)b).notifyTransformationFailed(contentItem,                            transform, reasonCode);
        }
    }

    public void notifyTransformationReady(ContentItem contentItem, Transformation transform)
    {
        if (a != null)
        {
            ((TransformationListener)a).notifyTransformationReady(contentItem,
                transform);
        }
        if (b != null)
        {
            ((TransformationListener)b).notifyTransformationReady(contentItem,                            transform);
        }
    }

}
