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

package org.cablelabs.impl.manager.lightweighttrigger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotLoadedException;
import org.ocap.dvr.event.LightweightTriggerSession;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordedService;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamInterface;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselConnectionListener;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselManager;
import org.cablelabs.impl.dvb.dsmcc.ObjectChangeCallback;
import org.cablelabs.impl.manager.recording.RecordedServiceImpl;
import org.cablelabs.impl.manager.recording.RecordedServiceLocator;

/**
 * @author Eric Koldinger
 * 
 */
public class MediaTimeTagsCarousel implements ObjectCarousel, LightweightTriggerSessionStoreListener,
        LightweightTriggerEventChangeListener
{
    private Locator m_locator = null;

    private LightweightTriggerEventStoreReadChange m_store = null;

    private PlaybackClient m_pbClient = null;

    private String m_mountPoint = null;

    private MediaTimeTagsStreamEventImpl m_streamEvent = null;

    private LightweightTriggerSessionImpl m_session = null;

    private Vector m_changeListeners = new Vector();

    private int m_versionNumber = 0;

    private static final String s_mountPoint = "/mtt/";

    private static long s_mountNumber = new Date().getTime();

    private static ObjectCarouselManager s_ocManager = ObjectCarouselManager.getInstance();

    private static LightweightTriggerManagerImpl s_lwtManager = (LightweightTriggerManagerImpl) LightweightTriggerManagerImpl.getInstance();

    private static SIManager s_siManager = SIManager.createInstance();

    private static final Logger log = Logger.getLogger(MediaTimeTagsCarousel.class.getName());

    public MediaTimeTagsCarousel(Locator l, int carouselId) throws InvalidLocatorException, MPEGDeliveryException
    {
        if (log.isDebugEnabled())
        {
            log.debug("MediaTimeTagsCarousel constructor, locator=" + l + ", id=" + carouselId);
        }
        if (l instanceof RecordedServiceLocator)
        {
            m_locator = l;
            Service s = s_siManager.getService(m_locator);

            if (s == null || !(s instanceof RecordedService))
            {
                throw new MPEGDeliveryException("No trigger store (Recording) associated with session" + l);
            }
            if (carouselId != ((RecordedServiceImpl) s).getArtificialCarouselID())
            {
                throw new MPEGDeliveryException("Carousel ID Does not match recording artifical carousel: "
                        + carouselId);
            }
            RecordedService rs = (RecordedService) s;
            // The RecordingImpl object is the
            // LightweightTriggerEventStoreReadWriteChange.
            m_store = (LightweightTriggerEventStoreReadChange) s;
            m_pbClient = (PlaybackClient) rs.getRecordingRequest();
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Invalid locator to this constructor: " + l);
            }
            throw new InvalidLocatorException(l, "Carousel ID required with this locator");
        }
        m_mountPoint = makeMountPoint(l);
        m_streamEvent = new MediaTimeTagsStreamEventImpl(this, m_store, m_pbClient);
        s_ocManager.addCarousel(this, new MediaTimeTagsFileSys());
    }

    public MediaTimeTagsCarousel(Locator l) throws InvalidLocatorException, MPEGDeliveryException
    {
        if (l instanceof LightweightTriggerCarouselLocator)
        {
            m_locator = l;
            m_session = (LightweightTriggerSessionImpl) s_lwtManager.getSessionByLocator((OcapLocator) l);
            if (log.isDebugEnabled())
            {
                log.debug("MediaTimeTagsCarousel(Locator l) session=" + m_session);
            }

            if (m_session == null)
            {
                throw new MPEGDeliveryException("Locator cannot resolve " + l);
            }

            m_session.registerStoreListener(this);
            // Created off of a LWTC Locator. Get the buffer store.
            if (log.isDebugEnabled())
            {
                log.debug("MediaTimeTagsCarousel(Locator l) setting buffer store -- session=" + m_session);
            }
            m_store = (LightweightTriggerEventStoreReadChange) m_session.getBufferStore();
            m_pbClient = (PlaybackClient) m_store;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Invalid locator to this constructor: " + l);
            }
            throw new InvalidLocatorException(l, "Carousel ID required with this locator");
        }
        m_mountPoint = makeMountPoint(l);
        m_streamEvent = new MediaTimeTagsStreamEventImpl(this, m_store, m_pbClient);
        s_ocManager.addCarousel(this, new MediaTimeTagsFileSys());
    }

    public Service getService()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Locator getLocator()
    {
        return m_locator;
    }

    public void detach()
    {
        if (m_session != null)
        {
            // If we're associated with a session, remove the store listener.
            m_session.registerStoreListener(null);
        }
        s_ocManager.removeCarousel(this);
    }

    public String getMountPoint() throws NotLoadedException, FileNotFoundException
    {
        return m_mountPoint;
    }

    public boolean isNetworkConnectionAvailable()
    {
        return true;
    }

    public byte[] getNSAPAddress()
    {
        // NSAP Address makes no sense to a MediaTimeTags carousel.
        return null;
    }

    public boolean match(Locator l)
    {
        return (l.equals(m_locator));
    }

    public void addConnectionListener(ObjectCarouselConnectionListener listener)
    {
        // TODO Auto-generated method stub
    }

    public void removeConnectionListener(ObjectCarouselConnectionListener listener)
    {
        // TODO Auto-generated method stub
    }

    public synchronized void bufferStoreChanged(LightweightTriggerSession sess,
            LightweightTriggerEventStoreChange newStore)
    {
        if (m_changeListeners.size() != 0 && m_store != null)
        {
            m_store.unregisterChangeNotification(this);
        }
        m_store = (LightweightTriggerEventStoreReadChange) newStore;
        if (log.isDebugEnabled())
        {
            log.debug("Buffer store changed.  Session: " + sess + " Store: " + newStore + " Stream Event: "
                    + m_streamEvent);
        }
        m_streamEvent.setStore(m_store);
        m_streamEvent.setPlaybackClient((PlaybackClient) ((LightweightTriggerSessionImpl) sess).getBufferStore());
        if (m_changeListeners.size() != 0 && m_store != null)
        {
            m_store.registerChangeNotification(this);
            // Better notify that events have changed.
            eventsChanged();
        }
    }

    public DSMCCStreamInterface getStreamObject(String path) throws IOException, IllegalObjectTypeException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Getting stream object" + path);
        }
        if (!m_mountPoint.equals(path))
        {
            throw new FileNotFoundException(path);
        }
        return m_streamEvent;
    }

    private synchronized static String makeMountPoint(Locator l)
    {
        return s_mountPoint + Long.toHexString(s_mountNumber++);
    }

    public synchronized void disableChangeEvents(Object objectChangeHandle)
    {
        m_changeListeners.remove(objectChangeHandle);
        if (m_changeListeners.size() == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Removing change notification");
            }
            m_store.unregisterChangeNotification(this);
        }
    }

    public synchronized Object enableObjectChangeEvents(String path, DSMCCObject obj, ObjectChangeCallback callback)
            throws IOException
    {
        if (!path.equals(m_mountPoint))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Attempting to enable change events on path that's not mount point");
            }
            throw new FileNotFoundException(path);
        }
        if (m_changeListeners.size() == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("First Listener.  Enabling change notification");
            }
            m_store.registerChangeNotification(this);
        }
        if (!m_changeListeners.contains(callback))
        {
            m_changeListeners.add(callback);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Not adding listener.  Already in list");
            }
        }
        return callback;
    }

    public void eventsChanged()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Notified events changed.  Incrementing version and notifying callbacks");
        }
        int version;
        Vector callbacks = null;
        synchronized (this)
        {
            m_versionNumber++;
            if (log.isDebugEnabled())
            {
                log.debug("Incrementing version number to: " + m_versionNumber);
            }
            version = m_versionNumber % 256;
            callbacks = (Vector) m_changeListeners.clone();
        }
        for (int i = 0; i < callbacks.size(); i++)
        {
            try
            {
                ObjectChangeCallback x = (ObjectChangeCallback) callbacks.elementAt(i);
                x.objectChanged(version);
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Callback " + i + " threw Exception.  Ignoring", e);
                }
        }
    }
    }

    public boolean getFileInfoIsKnown(String path) throws IOException
    {
        if (path.equals(m_mountPoint))
        {
            return true;
        }
        else
        {
            throw new FileNotFoundException(path + " not found");
        }
    }

    public int getFileType(String path) throws IOException
    {
        if (path.equals(m_mountPoint))
        {
            return ObjectCarousel.TYPE_STREAMEVENT;
        }
        else
        {
            throw new FileNotFoundException(path + " not found");
        }
    }

    public void prefetchFile(String path)
    {
        // Do nothing
    }

    public String resolveServiceXfer(String path, byte[] targetNSAP) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public long getVersion()
    {
        return (m_versionNumber % 256);
    }

}
