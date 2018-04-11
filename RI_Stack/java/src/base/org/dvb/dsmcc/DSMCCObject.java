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

package org.dvb.dsmcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.security.cert.X509Certificate;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamInterface;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselManager;
import org.cablelabs.impl.dvb.dsmcc.ObjectChangeCallback;
import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.BroadcastFileSys;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.filesys.LoadedFileSys;
import org.cablelabs.impl.manager.filesys.LoadedStreamFileSys;

/**
 * A DSMCCObject is an object which belongs to a DSMCC ServiceDomain. As soon as
 * a ServiceDomain has been attached to the file system hierarchy, DSMCCObject
 * objects can be created to access the ServiceDomain objects.
 * <p>
 * A DSMCCObject is specified by a pathname, which can either be an absolute
 * pathname or a relative pathname. Relative paths shall work as defined in
 * "Broadcast Transport Protocol Access API" in the main body of the
 * specification. Path names must follow the naming conventions of the host
 * platform. The constructors of this class shall accept the absolute paths
 * returned by java.io.File.getAbsolutePath().
 * <p>
 * To access the content of the object:
 * <ul>
 * <li>For a Directory, the method list of the java.io.File class has to be used
 * to get the entries of the directory.
 * <li>For a Stream object, the class DSMCCStream has to be used.
 * <li>For a File, the java.io.FileInputStream class or the
 * java.io.RandomAccessFile has to be used.
 * </ul>
 * NB :
 * <ul>
 * <li>Obviously, for the Object Carousel, the write mode of
 * java.io.RandomAccessFile is not allowed.
 * </ul>
 * <p>
 * DSMCCObjects exist in two states, loaded and unloaded as returned by the
 * isLoaded method. Transitions from unloaded to loaded are triggered by
 * applications calling the asynchronousLoad or synchronousLoad or
 * getSigners(boolean) methods. Transitions from loaded to unloaded are
 * triggered by applications calling the unload method. Attempting to load an
 * already loaded object does not cause it to be re-loaded.
 * <p>
 * The only state transitions for a DSMCCObject shall be only in response to
 * these method calls. There shall be no implicit state transitions in either
 * direction. When the application no longer has any references to an object in
 * the loaded state, the system resources allocated should be freed by the
 * system.
 * <p>
 * The state machine of DSMCCObject is disconnected from any state model of the
 * cache of an MHP receiver's DSMCC client. Objects may appear in that cache
 * without any corresponding DSMCCObject being in the loaded state. Objects
 * which are in that cache and where any corresponding DSMCCObject is not in the
 * loaded state may disappear from that cache at any time. The contents of a
 * object may be accessible to applications from the cache without the
 * DSMCCObject ever being in the loaded state.
 * <p>
 * 
 * NOTE: DSMCCObjects in the loaded state will consume memory in the MHP
 * receiver. If memory in the MHP receiver is short, this memory can only be
 * recovered by the receiver killing the MHP application. Applications which can
 * accept weaker guarantees about the data of a DSMCCObject being available
 * should use the prefetch methods.
 * 
 * @see org.dvb.dsmcc.ServiceDomain
 */
public class DSMCCObject extends File
{
    // ******************* Public Properties ******************** //

    /**
     * Constant to indicate that the data for an object shall only be retrieved
     * where it is already in cache and meets the requirements of cache priority
     * signaling.
     * 
     * Where data is not in the cache, or the contents don't meet the
     * requirements of the of cache priority signaling (i.e. cache priority
     * signalling indicates that an object re-acquisition is required), attempts
     * to load a DSMCCObject shall fail with <code>MPEGDeliveryException</code>
     * or <code>MPEGDeliveryErrorEvent</code> for synchronousLoad and
     * asynchronousLoad respectively.
     * 
     * @since MHP 1.0.1
     */
    public static final int FROM_CACHE = 1;

    /**
     * Constant to indicate that the data for an object shall be automatically
     * be retrieved from the network where the data is not already cached.
     * 
     * Note that this method does not modify the caching policy controlled by
     * the signaling in the OC. So, if the data is signalled as requiring
     * transparent caching then data will be retrieved from the network if
     * required.
     * 
     * @since MHP 1.0.1
     */
    public static final int FROM_CACHE_OR_STREAM = 2;

    /**
     * Constant to indicate that the data for an object shall always be
     * retrieved from the network even if the data has already been cached.
     * 
     * @since MHP 1.0.1
     */
    public static final int FROM_STREAM_ONLY = 3;

    // ******************* Public Methods ******************** //

    /**
     * Create a DSMCCObject object.
     * 
     * @param path
     *            the path to the file.
     */
    public DSMCCObject(String path)
    {
        super(path);
        construct(this);
    }

    /**
     * Create a DSMCCObject object.
     * 
     * @param path
     *            the directory Path.
     * @param name
     *            the file pathname.
     */
    public DSMCCObject(String path, String name)
    {
        super(path, name);
        construct(this);
    }

    /**
     * Create a DSMCCObject object.
     * 
     * @param dir
     *            the directory object.
     * @param name
     *            the file pathname.
     */
    public DSMCCObject(DSMCCObject dir, String name)
    {
        super(dir.getPath(), name);
        construct(this);
    }

    /**
     * Returns a boolean indicating whether or not the DSMCCObject has been
     * loaded.
     * 
     * @return true if the file is already loaded, false otherwise.
     */
    public boolean isLoaded()
    {
        FileSys fs = FileSysManager.getFileSys(getRealPath());
        if (fs instanceof LoadedFileSys)
        {
            return true;
        }
        
        return false;
    }

    /**
     * Returns a boolean indicating whether or not the DSMCCObject is a DSMCC
     * Stream object.
     * 
     * @return true if the file is a stream, false if the object is not a stream
     *         or if the object kind is unknown.
     */
    public boolean isStream()
    {
        try
        {
            ObjectCarousel oc = getOC();
            if (oc != null)
            {
                int type = oc.getFileType(this.getRealPath());
                if ((type == ObjectCarousel.TYPE_STREAM) || (type == ObjectCarousel.TYPE_STREAMEVENT))
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            // all exceptions end up meaning that this is not a stream type
            // since we cannot figure out what type of file this is or
            // even if it is a valid file
            if (log.isDebugEnabled())
            {
                log.debug("isStream - ignoring Exception - " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Returns a boolean indicating whether or not the DSMCCObject is a DSMCC
     * StreamEvent object. NB: If isStreamEvent is true then isStream is true
     * also.
     * 
     * @return true if the file is a stream event, false if the object is not a
     *         stream event or if the object kind is unknown.
     */
    public boolean isStreamEvent()
    {
        try
        {
            ObjectCarousel oc = getOC();
            if (oc != null)
            {
                int type = oc.getFileType(this.getRealPath());
                if (type == ObjectCarousel.TYPE_STREAMEVENT)
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            // all exceptions end up meaning that this is not a stream event
            // type
            // since we cannot figure out what type of file this is or
            // even if it is a valid file
            if (log.isDebugEnabled())
            {
                log.debug("isStreamEvent - ignoring Exception - " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Returns a boolean indicating if the kind of the object is known. (The
     * kind of an object is known if the directory containing it is loaded).
     * 
     * @return true if the type of the object is known, false otherwise.
     */
    public boolean isObjectKindKnown()
    {
        FileSys fs = FileSysManager.getFileSys(getRealPath());
        if (fs instanceof LoadedFileSys)
        {
            // Loaded, must know what it is.
            return true;
        }
        // Not loaded, go try to find the file.
        try
        {
            ObjectCarousel oc = getOC();
            if (oc != null)
            {
                return oc.getFileInfoIsKnown(this.getRealPath());
            }
        }
        catch (Exception e)
        {
            // all exceptions end up meaning that we cannot determine if
            // the parent directory is known
            if (log.isDebugEnabled())
            {
                log.debug("isObjectKindKnown - ignoring Exception - " + e.getMessage(), e);
            }
        }

        return false;
    }

    /**
     * This method is used to load a DSMCCObject. This method blocks until the
     * file is loaded. It can be aborted from another thread with the abort
     * method. In this case the InterruptedIOException is thrown. If the IOR of
     * the object itself or one of its parent directories is a Lite Option
     * Profile Body, the MHP implementation will not attempt to resolve it : a
     * ServiceXFRException is thrown to indicate to the application where the
     * DSMCCObject is actually located.
     * 
     * @exception InterruptedIOException
     *                the loading has been aborted.
     * @exception InvalidPathNameException
     *                the Object can not be found, or the serviceDomain isn't in
     *                a attached state.
     * @exception NotEntitledException
     *                the stream carrying the object is scrambled and the user
     *                has no entitlements to descramble the stream.
     * @exception ServiceXFRException
     *                the IOR of the object or one of its parent directories is
     *                a Lite Option Profile Body.
     * @exception InvalidFormatException
     *                an inconsistent DSMCC message has been received.
     * @exception MPEGDeliveryException
     *                an error has occurred while loading data from MPEG stream
     *                such as a timeout
     * @exception ServerDeliveryException
     *                when an MHP terminal cannot communicate with the server
     *                for files delivered over a bi-directional IP connection.
     * @exception InsufficientResourcesException
     *                there is not enough memory to load the object
     */
    public void synchronousLoad() throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException,
            ServerDeliveryException, InvalidPathNameException, NotEntitledException, ServiceXFRException,
            InsufficientResourcesException
    {
        // Return if already loaded.
        if (isLoaded())
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCObject: SynchronousLoad: Already loaded: " + this.getPath());
            }
            return;
        }

        SynchronousLoadListener loadListener = new SynchronousLoadListener();
        if (log.isDebugEnabled())
        {
            log.debug("DSMCCObject: synchronousLoad: Starting synchronous load: " + this.getPath());
        }
        synchronized (loadListener)
        {
            this.asynchronousLoad(loadListener);
            try
            {
    			// Added proper condition for findbugs issues fix
            	while(!loadListener.m_receivedEvent)
            	{
                loadListener.wait();
            }
            }
            catch (InterruptedException e)
            {
                // What should I do here?
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("DSMCCObject: SynchronousLoad: AsynchronousLoad complete: "
                    + loadListener.getEvent().getClass().getName());
        }
        loadListener.getEvent().throwException();
    }

    /**
     * This method is used to asynchronously load a carousel object. This method
     * can fail either asynchronously with an event or synchronously with an
     * exception. When it fails synchronously with an exception, no event shall
     * be sent to the listener. For each call to this method which returns
     * without throwing an exception, one of the following events will be sent
     * to the application (by a listener mechanism) as soon as the loading is
     * done or if an error has occurred: SuccessEvent, InvalidFormatEvent,
     * InvalidPathNameEvent, MPEGDeliveryErrorEvent, ServerDeliveryErrorEvent,
     * ServiceXFRErrorEvent, NotEntitledEvent, LoadingAbortedEvent,
     * InsufficientResourcesEvent.
     * 
     * @param l
     *            an AsynchronousLoadingEventListener to receive events related
     *            to asynchronous loading.
     * 
     * @exception InvalidPathNameException
     *                the Object can not be found, or the serviceDomain isn't in
     *                a attached state.
     */
    public void asynchronousLoad(AsynchronousLoadingEventListener l) throws InvalidPathNameException
    {
        if (log.isDebugEnabled())
        {
            log.debug("DSMCCObject: AsynchronousLoad: Starting asynchronous load: " + this.getPath());
        }
        synchronized (m_sync)
        {
            // First, check if it's already loaded. If so, punt.
            // Technically, this COULD go outside the sync, but just for grins,
            // we'll keep it here.
            if (isLoaded())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCObject: AsynchronousLoad: Already loaded");
                }
                return;
            }
            // Next, is a load in progress
            if (m_loader != null)
            {
                // If one's in progress, just join in.
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCObject: AsynchronousLoad: Adding to previous load");
                }
                m_loader.addListener(l);
                return;
            }
            // Nope, nothing in progress.
            // Start a whole new freakin load.
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCObject: AsynchronousLoad: Starting new load");
            }
            // Get the Object Carousel.
            getOC();
            m_loader = new LoaderObject(this, l, m_sync);
            try
            {
                FileSys fs = FileSysManager.getFileSys(getRealPath());
                if (fs == null || !(fs instanceof BroadcastFileSys))
                {
                    throw new InvalidPathNameException(this.getPath());
                }
                if (log.isDebugEnabled())
                {
                    log.debug("Got filesys: " + fs.getClass().getName());
                }
                // TODO: What, exactly, does getCanonicalPath() do on this box?
                // Does it resolve the ServiceXFRReference's? If so, this is a
                // bug, and will
                // not correctly handle the path
                BroadcastFileSys bfs = (BroadcastFileSys)fs;
                m_loadHandle = bfs.asynchronousLoad(getRealPath(), loadMode, m_loader);
            }
            catch (FileNotFoundException e)
            {
                m_loader = null;
                throw new InvalidPathNameException("path not found -" + this.getPath());
            }
        }
    }

    /**
     * This method is used to abort a load in progress. It can be used to abort
     * either a synchronousLoad or an asynchronousLoad.
     * 
     * @exception NothingToAbortException
     *                There is no loading in progress.
     */
    public void abort() throws NothingToAbortException
    {
        synchronized (m_sync)
        {
            if (m_loadHandle == null)
            {
                throw new NothingToAbortException();
            }
            
            AsyncLoadHandle lh = m_loadHandle;

            // clear the AsyncLoadHandle and the loader
            m_loader = null;
            m_loadHandle = null;
            
            // abort the load. If false is returned, throw an exception
            // since there was nothing to abort
            if (!lh.abort())
                throw new NothingToAbortException();
        }
    }

    /**
     * 
     * Calling this method will issue a hint to the MHP for pre-fetching the
     * object data for that DSMCC object into cache.
     * 
     * @param path
     *            the absolute pathname of the object to pre-fetch.
     * @param priority
     *            the relative priority of this pre-fetch request (higher = more
     *            important)
     * @return true if the MHP supports pre-fetching (i.e. will try to process
     *         the request) and false otherwise. Note that a return value of
     *         'true' is only an indication that the MHP receiver supports
     *         pre-fetching. It is not a guarantee that the requested data will
     *         actually be loaded into cache as the receiver may decide to drop
     *         the request in order to make resources available for regular load
     *         requests.
     */
    public static boolean prefetch(String path, byte priority)
    {
        try
        {
            ObjectCarousel oc = s_ocm.getCarouselByPath(path);
            if (oc != null)
            {
                oc.prefetchFile(path);
            }
        }
        catch (MPEGDeliveryException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught exception while prefetching");
            }
        }
        return true;
    }

    /**
     * 
     * Calling this method will issue a hint to the MHP for pre-fetching the
     * object data for that DSMCC object into cache.
     * 
     * @param dir
     *            the directory object in which to pre-fetch the data.
     * @param path
     *            the relative path name of object to pre-fetch, starting from
     *            the directory object passes as parameter.
     * @param priority
     *            the relative priority of this pre-fetch request (higher = more
     *            important)
     * @return true if the MHP supports pre-fetching (i.e. will try to process
     *         the request) and false otherwise. Note that a return value of
     *         'true' is only an indication that the MHP receiver supports
     *         pre-fetching. It is not a guarantee that the requested data will
     *         actually be loaded into cache as the receiver may decide to drop
     *         the request in order to make resources available for regular load
     *         requests.
     */
    public static boolean prefetch(DSMCCObject dir, String path, byte priority)
    {
        return prefetch(dir.getRealPath() + "/" + path, priority);
    }

    /**
     * When calling this method, the applications gives a hint to the MHP that
     * if this object is not consumed by another application/thread, the system
     * can free all the resources allocated to this object. It is worth noting
     * that if other clients use this object (e.g. a file input stream is opened
     * on this object or if the corresponding stream or stream event is being
     * consumed) the system resources allocated to this object will not be
     * freed. This method puts the DSMCCObject into the unloaded state.
     * 
     * @exception NotLoadedException
     *                the carousel object is not loaded.
     */
    public void unload() throws NotLoadedException
    {
        synchronized (m_sync)
        {
            if (!isLoaded())
            {
                throw new NotLoadedException();
            }
            // call the filesys instance to do the unload
            LoadedFileSys lfs = (LoadedFileSys)FileSysManager.getFileSys(getRealPath());
            lfs.unload();
        }
    }

    /**
     * Returns a URL identifying this carousel object. If the directory entry
     * for the object has not been loaded then null shall be returned.
     * 
     * @since MHP 1.0.1
     * @return a URL identifying the carousel object or null
     */
    public java.net.URL getURL()
    {
        // is this object loaded yet
        if (m_everLoaded)
        {
            // create a URL for this file object
            try
            {
                return new java.net.URL("file://" + getRealPath());
            }
            catch (Exception e)
            {
                // if we get any exceptions,
                // then we can't create the URL so return null
                if (log.isDebugEnabled())
                {
                    log.debug("isStream - Unable to create URL - " + "ignoring Exception - " + e.getMessage());
                }
            }
        }

        return null;
    }

    /**
     * Subscribes an ObjectChangeEventListener to receive notifications of
     * version changes of DSMCCObject.
     * <p>
     * 
     * This listener shall never be fired until after the object has
     * successfully entered the loaded state for the first time. Hence objects
     * which never successfully enter the loaded state (e.g. because the object
     * cannot be found) shall never have this listener fire. Once an object has
     * successfully entered the loaded state once, this event shall continue to
     * be fired when changes are detected by the MHP regardless of further
     * transitions in or out of the loaded state.
     * <p>
     * NOTE: The algorithm used for this change monitoring is implementation
     * dependent. In some implementations, this exception will always be thrown.
     * In other implementations, it will never be thrown. In other
     * implementations, whether it is thrown or not will depend on the
     * complexity and design of the object carousel in which the object is
     * carried. Even where no exception is thrown, implementations are not
     * required to detect all possible forms in which an object may change.
     * 
     * @param listener
     *            the ObjectChangeEventListener to be notified .
     * @throws InsufficientResourcesException
     *             if there are not sufficient resources to monitor the object
     *             for changes.
     */
    public void addObjectChangeEventListener(ObjectChangeEventListener listener) throws InsufficientResourcesException
    {
        if (listener != null)
        {
            // register this listener in our change vector
            synchronized (changeListeners)
            {
                // if this is the first listener, turn on MPE object change
                // checking
                if (changeListeners.size() == 0)
                {
                    // 
                    CCData data = getCCData(currContext);
                    data.addObject(this);
                    if (m_everLoaded)
                    {
                        try
                        {
                            setChangeListener();
                        }
                        catch (Exception e)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("setChangeListener() threw exception", e);
                            }
                            throw new InsufficientResourcesException("Cannet enable object version tracking");
                        }
                    }
                }
                if (!changeListeners.contains(listener))
                {
                    // add this listener into the vector
                    changeListeners.addElement(listener);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("addObjectChangeEventListener - throwing "
                        + "InsufficientResourcesException (cannot register null listener)");
            }
            throw new InsufficientResourcesException("cannot register null listener");
        }
    }

    private void setChangeListener() throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Initializing change listener for " + getRealPath());
        }
        ObjectCarousel oc = getOC();
        if(objectChangeHandle == null)
        {        
            objectChangeHandle = oc.enableObjectChangeEvents(getRealPath(), this, m_changeCallback);
        }
    }

    /**
     * Unsubscribes an ObjectChangeEventListener to receive notifications of
     * version changes of DSMCCObject.
     * 
     * @param listener
     *            a previously registered ObjectChangeEventListener.
     */
    public void removeObjectChangeEventListener(ObjectChangeEventListener listener)
    {
        // unregister this listener from our change vector
        synchronized (changeListeners)
        {
            changeListeners.removeElement(listener);

            // if there are no listeners left on the vector,
            // disable object version tracking
            if (changeListeners.size() == 0)
            {
                // TODO: Should this be synchronized? What if we're doing this
                // as the application is destroyed?
                CCData data = getCCData(currContext);
                data.removeObject(this);
                // call MPE to disable object version tracking
                try
                {
                    m_oc.disableChangeEvents(objectChangeHandle);
                    objectChangeHandle = null;
                }
                catch (Exception e)
                {
                    // ignore exceptions as we can't do anything about them here
                    if (log.isDebugEnabled())
                    {
                        log.debug("removeObjectChangeEventListener - ignoring Exception - " + e.getMessage());
                    }
            }
        }
    }
    }

    /**
     * Asynchronous loading of the directory entry information. Calling this is
     * equivalent of calling the method <code>asynchronousLoad</code> on the
     * parent directory of a <code>DSMCCObject</code>. This method can fail
     * either asynchronously with an event or synchronously with an exception.
     * When it fails synchronously with an exception, no event shall be sent to
     * the listener.
     * 
     * @param l
     *            a listener which will be called when the loading is done.
     * 
     * @exception InvalidPathNameException
     *                if the object cannot be found.
     */
    public void loadDirectoryEntry(AsynchronousLoadingEventListener l) throws InvalidPathNameException
    {
        // Get parentDir for absolute version of this path
        String parentDir = getAbsoluteFile().getParent();

        if (parentDir != null)
        {
            DSMCCObject parentObj = new DSMCCObject(parentDir);
            parentObj.asynchronousLoad(l);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("loadDirectoryEntry - parent directory is null");
            }
            throw new InvalidPathNameException("parent directory is null");
        }

        return;
    }

    /**
     * Set the retrieval mode for a <code>DSMCCObject</code>. The default
     * retrieval mode is FROM_CACHE_OR_STREAM. The retrieval mode state is
     * sampled when the object is loaded (whether explicitly or as described in
     * "Constraints on the java.io.File methods for broadcast carousels").
     * Changing the retrieval mode for a loaded object has no effect until the
     * object is unloaded and loaded again.
     * 
     * @param retrieval_mode
     *            the retrieval mode to be used for the object specified as one
     *            of the public static final constants in this class.
     * @throws IllegalArgumentException
     *             if the retrieval_mode specified is not one listed defined for
     *             use with this method.
     * 
     * @since MHP 1.0.1
     */
    public void setRetrievalMode(int retrieval_mode)
    {
        switch (retrieval_mode)
        {
            case FROM_CACHE:
            case FROM_CACHE_OR_STREAM:
            case FROM_STREAM_ONLY:
                loadMode = retrieval_mode;
                break;
            default:
                // illegal retrieval mode!
                if (log.isDebugEnabled())
                {
                    log.debug("setRetrievalMode - illegal retrieval mode");
                }

                throw new IllegalArgumentException("illegal retrieval mode");
        }

        return;
    }

    /**
     * This method shall attempt to validate all certificate chains found for
     * this file in the network. Valid chains do not need to originate from root
     * certificates known to the MHP terminal, e.g. self signing of data files.
     * Applications should note that calls to this method may take some time. If
     * the <code>DSMCCObject</code> is not loaded, this method will return null.
     * If the <code>DSMCCObject</code> is loaded but not authenticated this
     * method will return an outer array of size zero. If the
     * <code>DSMCCObject</code> is loaded, this method returns the same as
     * getSigners(false), except if getSigners(false) would throw an exception,
     * this method will return an outer array of size zero.
     * 
     * <p>
     * NOTE: If the file in the network changes between when it was loaded and
     * when the hash file(s), signature & certificate files are read and those
     * files have been updated to match the new version of the file then the
     * hash value of the data which was loaded will not match the hash value in
     * the hash file in the network and hence no certificate chains will be
     * valid.
     * 
     * @return a two-dimensional array of X.509 certificates, where the first
     *         index of the array determines a certificate chain and the second
     *         index identifies the certificate within the chain. Within one
     *         certificate chain the leaf certificate is first followed by any
     *         intermediate certificate authorities in the order of the chain
     *         with the root CA certificate as the last item.
     * 
     * @since MHP 1.0.1
     */
    public X509Certificate[][] getSigners()
    {
        if (!isLoaded()) return null;

        String path = getRealPath();

        LoadedFileSys lfs = (LoadedFileSys)FileSysManager.getFileSys(path);
        return lfs.getSigners(path);
    }

    /**
     * This method shall attempt to validate all certificate chains found for
     * this file in the network. The known_root parameter to the method defines
     * whether the MHP terminal shall check if the root certificate in each
     * chain is known to it or not. If the root certificate is checked then
     * chains with unknown root certificates shall not be considered to be
     * valid. If root certificates are not checked then the MHP application is
     * responsible for comparing them with some certificate which it provides
     * (e.g. for self signing of data files). The hash file(s), signature &
     * certificate files shall be shall be fetched from the network in
     * compliance with the caching priority defined in the main body of this
     * specification. If the object is in the loaded state then the data of the
     * file which was loaded shall be used and no new file contents loaded. If
     * the object is not in the loaded state then this method shall attempt to
     * load it as if synchronousLoad had been called. Applications should note
     * that calls to this method may take some time.
     * 
     * <p>
     * NOTE: If the file in the network changes between when it was loaded and
     * when the hash file(s), signature & certificate files are read and those
     * files have been updated to match the new version of the file then the
     * hash value of the data which was loaded will not match the hash value in
     * the hash file in the network and hence no certificate chains will be
     * valid.
     * 
     * @return a two-dimensional array of X.509 certificates, where the first
     *         index of the array determines a certificate chain and the second
     *         index identifies the certificate within the chain. Within one
     *         certificate chain the leaf certificate is first followed by any
     *         intermediate certificate authorities in the order of the chain
     *         with the root CA certificate as the last item. If no certificate
     *         chains are found to be valid then an outer array of size zero
     *         shall be returned.
     * 
     * @param known_root
     *            if true then valid certificate chains are only those where the
     *            root is known to the MHP terminal. If false, the validity of
     *            the chain shall be determined without considering whether the
     *            root is known to the MHP terminal or not.
     * @since MHP 1.0.3
     * @exception InterruptedIOException
     *                the loading has been aborted.
     * @exception InvalidPathNameException
     *                the Object can not be found, or the serviceDomain isn't in
     *                a attached state.
     * @exception NotEntitledException
     *                the stream carrying the object is scrambled and the user
     *                has no entitlements to descramble the stream.
     * @exception ServiceXFRException
     *                the IOR of the object or one of its parent directories is
     *                a Lite Option Profile Body.
     * @exception InvalidFormatException
     *                an inconsistent DSMCC message has been received.
     * @exception MPEGDeliveryException
     *                an error has occurred while loading data from MPEG stream
     *                such as a timeout
     * @exception ServerDeliveryException
     *                when an MHP terminal cannot communicate with the server
     *                for files delivered over a bi-directional IP connection.
     * @exception InsufficientResourcesException
     *                there is not enough memory to load the object
     */
    public X509Certificate[][] getSigners(boolean known_root) throws InvalidFormatException, InterruptedIOException,
            MPEGDeliveryException, ServerDeliveryException, InvalidPathNameException, NotEntitledException,
            ServiceXFRException, InsufficientResourcesException
    {
        if (!isLoaded())
        {
            synchronousLoad();
        }

        String path = getRealPath();
        LoadedFileSys lfs = (LoadedFileSys)FileSysManager.getFileSys(path);
        try
        {
            return lfs.getSigners(path, known_root);
        }
        catch (InvalidFormatException e)
        {
            throw e;
        }
        catch (InterruptedIOException e)
        {
            throw e;
        }
        catch (MPEGDeliveryException e)
        {
            throw e;
        }
        catch (ServerDeliveryException e)
        {
            throw e;
        }
        catch (InvalidPathNameException e)
        {
            throw e;
        }
        catch (NotEntitledException e)
        {
            throw e;
        }
        catch (ServiceXFRException e)
        {
            throw e;
        }
        catch (InsufficientResourcesException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            // otherwise just throw InsufficientResourcesException
            throw new InsufficientResourcesException(e.toString());
        }
    }

    // Package private methods
    DSMCCStreamInterface getStream()
    {
        FileSys fs = FileSysManager.getFileSys(getRealPath());
        if (fs instanceof LoadedStreamFileSys)
        {
            return ((LoadedStreamFileSys) fs).getStream();
        }
        
        return null;
    }

    // ******************* Private Properties ******************** //

    private int loadMode = FROM_CACHE_OR_STREAM;

    // private boolean loaded;
    private transient CallerContext currContext;

    private transient Vector changeListeners;

    private transient Object objectChangeHandle = null;

    // Loaded state of this object
    private boolean m_everLoaded = false;

    // private long loadedVersion = 0;
    // private boolean loaded = false;

    // Synchronization object.
    private transient Object m_sync;

    private transient LoaderObject m_loader = null;

    private transient AsyncLoadHandle m_loadHandle = null;

    // ******************* Private Classes ******************** //
    private class SynchronousLoadListener implements AsynchronousLoadingEventListener
    {
        private AsynchronousLoadingEvent m_event;
    	// Added for findbugs issues fix
        private boolean m_receivedEvent = false;

        public synchronized void receiveEvent(AsynchronousLoadingEvent e)
        {
            m_event = e;
            m_receivedEvent = true;
            this.notifyAll();
        }

        public AsynchronousLoadingEvent getEvent()
        {
            return m_event;
        }
    }

    class AborterObject implements Runnable
    {
        LoaderObject m_loader;

        AsynchronousLoadingEvent m_event;

        AborterObject(LoaderObject l, AsynchronousLoadingEvent e)
        {
            m_loader = l;
            m_event = e;
        }

        public void run()
        {
            m_loader.sendEvent(m_event);
        }

    }

    class LoaderObject implements AsyncLoadCallback
    {
        DSMCCObject m_parent;

        Vector m_listeners;

        Object m_sync;

        boolean m_aborted = false;

        LoaderObject(DSMCCObject parent, AsynchronousLoadingEventListener listener, Object syncObject)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCObject.LoaderObject.  Creating: " + parent.getRealPath());
            }

            m_sync = syncObject;
            m_parent = parent;
            m_listeners = new Vector();
            m_listeners.add(listener);
        }

        boolean addListener(AsynchronousLoadingEventListener l)
        {
            synchronized (m_sync)
            {
                if (m_listeners == null)
                {
                    return false;
                }
                m_listeners.add(l);
            }
            return true;
        }

        /**
         * Send an AsynchronousLoadingEvent to every listener currently
         * listening to this load operation.
         */
        void sendEvent(AsynchronousLoadingEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCObject.LoaderObject.  Sending event: " + m_parent.getRealPath() + ": Event: "
                        + e.getClass().getName());
            }
            Vector listeners = null;
            // Get the listeners vector, and clear it out of the object
            synchronized (m_sync)
            {
                listeners = m_listeners;
                m_listeners = null;
            }
            // Check that we still have a listener. If not, something else has
            // signalled. Just get
            // out of here in that case.
            if (listeners == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCObject.LoaderObject. Events already sent");
                }
                return;
            }
            // Now, loop over everything, and send the event.
            while (!listeners.isEmpty())
            {
                AsynchronousLoadingEventListener listener = (AsynchronousLoadingEventListener) listeners.remove(0);
                listener.receiveEvent(e);
            }
        }

        /**
         * Perform the actual abort. Since the thread running the actual load is
         * down in MPE at this point, and can't really be interrupted, we just
         * mark that we're aborted, and allow the load to complete. It will note
         * the aborted call, and die at that point. We create an Aborter object
         * which will do the actual signalling to do the abort on a different
         * thread.
         */
        public void abort()
        {
            synchronized (m_sync)
            {
                m_aborted = true;
                if (m_listeners == null)
                {
                    return;
                }
            }
            AborterObject a = new AborterObject(this, new LoadingAbortedEvent(m_parent));
            m_parent.currContext.runInContext(a);

        }

        /**
         * Actually perform a load. Go into JNI/MPE and load file into an array.
         * The array is created in the JNI layer. When the load completes, check
         * to see if the load has been aborted. If it has,
         */
        public void done(FileSys fs, Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCObject.LoaderObject.  Complete: " + m_parent.getRealPath());
            }
                if (fs != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCObject.LoaderObject: Loaded " + fs.length(m_parent.getRealPath()) + " bytes");
                }
            }
                if (e != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DSMCCObject.LoaderObject: Exception: " + e.getClass().getName() + " ::" + e.getMessage());
                }
            }

            if (e == null)
            {
                synchronized (m_sync)
                {
                    // If aborted, already sent the ABORT messages
                    if (m_aborted)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("DSMCCObject.LoaderObject.  Load aborted. " + m_parent.getRealPath());
                        }
                        return;
                    }

                    if (fs == null)
                    {
                        // null out the loader and return
                        m_parent.m_loader = null;
                        m_parent.m_loadHandle = null;
                        return;
                    }

                    synchronized (changeListeners)
                    {
                        if (changeListeners.size() != 0)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Change listeners already active when load completed.  Starting");
                            }
                            try
                            {
                                setChangeListener();
                            }
                            catch (IOException ex)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Caught IOException starting change listeners.  Ignoring", ex);
                                }
                            }

                        }
                    }
                    m_parent.m_everLoaded = true;

                    m_parent.m_loader = null;
                    m_parent.m_loadHandle = null;
                }
                sendEvent(new SuccessEvent(m_parent));
            }
            else
            {
                if (e instanceof FileNotFoundException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  FileNotFoundException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new InvalidPathnameEvent(m_parent));
                }
                else if (e instanceof ServiceXFRException)
                {
                    // This will be a blank exception, as the JNI doesn't fill
                    // in the target.
                    // We need to do it here.
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  SerivceXFRException: " + m_parent.getRealPath());
                    }
                    byte targetNSAP[] = new byte[20];
                    String remotePath;
                    try
                    {
                        ObjectCarousel oc = getOC();
                        if (oc != null)
                        {
                            remotePath = oc.resolveServiceXfer(m_parent.getRealPath(), targetNSAP);
                            ServiceXFRReference xfr = new ServiceXFRReference(targetNSAP, remotePath);
                            sendEvent(new ServiceXFRErrorEvent(m_parent, xfr));
                        }
                        else
                        {
                            sendEvent(new MPEGDeliveryErrorEvent(m_parent));
                        }
                    }
                    catch (IOException ex)
                    {
                        sendEvent(new MPEGDeliveryErrorEvent(m_parent));
                    }
                }
                else if (e instanceof InvalidPathNameException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  InvalidPathNameException: " + m_parent.getRealPath()
                                + " " + e.getMessage());
                    }
                    sendEvent(new InvalidPathnameEvent(m_parent));
                }
                else if (e instanceof NotEntitledException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  NotEntitledException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new NotEntitledEvent(m_parent));
                }
                else if (e instanceof InvalidFormatException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  InvalidFormatException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new InvalidFormatEvent(m_parent));
                }
                else if (e instanceof MPEGDeliveryException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  MPEGDeliveryException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new MPEGDeliveryErrorEvent(m_parent));
                }
                else if (e instanceof ServerDeliveryException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  ServerDeliveryException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new ServerDeliveryErrorEvent(m_parent));
                }
                else if (e instanceof InsufficientResourcesException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  InsufficientResourcesException: "
                                + m_parent.getRealPath() + " " + e.getMessage());
                    }
                    sendEvent(new InsufficientResourcesEvent(m_parent));
                }
                else if (e instanceof IOException)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  IOException: " + m_parent.getRealPath() + " "
                                + e.getMessage());
                    }
                    sendEvent(new MPEGDeliveryErrorEvent(m_parent));
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DSMCCObject.LoaderObject.  Error: " + e.getClass().getName() + " : "
                                + e.getMessage());
                    }
                }
            }
        }
    }

    // ******************* Private Methods ******************** //

    /**
     * Package private method (for testing) to get the current object retrieval
     * mode.
     */
    int getRetrievalMode()
    {
        return loadMode;
    }

    /**
     * Private method to wrap getting this object's canonical path (basically,
     * eats any exceptions and returns a null string on errors).
     */
    private String getRealPath()
    {
        String returnedPath;
        try
        {
            returnedPath = getCanonicalPath();
        }
        catch (Exception e)
        {
            returnedPath = null;
        }
        return returnedPath;
    }

    /**
     * Private method to provide a common class initialization routine for all
     * of the various constructors.
     */
    private static void construct(DSMCCObject obj)
    {
        // Create the synchronization object.
        obj.m_sync = new Object();

        // save off this object in current context to be used for async events
        obj.currContext = ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getCurrentContext();

        // create the listener vectors
        // Here because it's ugly otherwise
        obj.changeListeners = new Vector();
    }

    // TODO: try to refactor these next few functions into the above EdListener
    // classes?

    /**
     * Private method to process asynchronous object change events for this
     * object.
     */
    private void processObjectChangeEvents(ObjectChangeEvent event)
    {
        // If the object has never been loaded, we don't handle the event yet.
        if (!m_everLoaded)
        {
            return;
        }

        // don't send object change events if this object isn't loaded
        // if ( (nativeGetFileIsLoaded(this.getRealPath())) && (event != null) )
        // BUG????: TODO:
        // In our implementation, this information is brought into the cache
        // before the event is sent, thus the information is inherently loaded.
        if (event != null)
        {
            // for each loading listener in this object,
            // call its listener with this event
            // leaving them in the vector
            for (int i = (changeListeners.size() - 1); i >= 0; i--)
            {
                // get the listener (leave it in the vector)
                ObjectChangeEventListener changeListener = (ObjectChangeEventListener) changeListeners.elementAt(i);

                // call the listener with the event
                if (changeListener != null)
                {
                    changeListener.receiveObjectChangeEvent(event);
                }
            }
        }

        // remove any cached data for the path in the source object
        FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        DSMCCObject source = (DSMCCObject) event.getSource();
        String path;

        // get the path for the event source object
        try
        {
            path = source.getCanonicalPath();
        }
        catch (IOException e)
        {
            path = source.getPath();
        }
        // remove the entry for path from the cache
        fileMgr.flushCache(path);

        return;
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private static CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(DSMCCObject.class);

        // If a data block has not yet been assigned to this caller context
        // then allocate one.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, DSMCCObject.class);
        }
        return data;
    }

    // Static class.
    static class CCData implements CallbackData
    {
        /**
         * The domains list is used to keep track of all DSMCCObjects objects
         * currently in the attached state for this caller context.
         */
        public volatile Vector dsmccObjects = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // Discard the caller context data for this caller context.
            cc.removeCallbackData(DSMCCObject.class);

            // Remove each ServiceDomain object from the domains list, and
            // delete it.
            int size = dsmccObjects.size();

            for (int i = 0; i < size; i++)
            {
                try
                {
                    // Grab the first element in the queue
                    DSMCCObject obj = (DSMCCObject) dsmccObjects.elementAt(i);
                    // And detach it
                    if (obj.objectChangeHandle != null)
                    {
                        obj.m_oc.disableChangeEvents(obj.objectChangeHandle);
                        obj.objectChangeHandle = null;
                    }
                }
                catch (Exception e)
                {
                    // Ignore any exceptions
                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy() ignoring Exception " + e);
                    }
                }
            }
            // Toss the whole thing
            dsmccObjects = null;
        }

        public void addObject(DSMCCObject o)
        {
            dsmccObjects.add(o);
        }

        public void removeObject(DSMCCObject o)
        {
            dsmccObjects.remove(o);
        }
    }

    private transient ObjectCarousel m_oc = null;

	// Added for findbugs issues fix
    private ObjectCarousel getOC()
    {
            synchronized (m_sync)
            {
                if (m_oc == null)
                {
                    try
                    {
                        m_oc = s_ocm.getCarouselByPath(getRealPath());
                    }
                    catch (MPEGDeliveryException e)
                    {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Unable to getCarouselByPath(" + getRealPath() + ")", e);
                    }
                }
            }
        }
        return m_oc;
    }

    static
    {
        // insure that the OCAP (MPE) library is loaded so that these native
        // method calls work!
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();

        // Insure implicitly used managers are initialized.
        ManagerManager.getInstance(EventDispatchManager.class);
        ManagerManager.getInstance(FileManager.class);
    }

    private static ObjectCarouselManager s_ocm = ObjectCarouselManager.getInstance();

    private transient ObjectChangeCallback m_changeCallback = new ObjectChangeCallback()
    {
        public void objectChanged(int version)
        {
            final DSMCCObject d = DSMCCObject.this;
            if (log.isDebugEnabled())
            {
                log.debug("ObjectChangeCallback called: " + d.getRealPath() + ": " + version);
            }
            final ObjectChangeEvent oce = new ObjectChangeEvent(d, version);
            currContext.runInContext(new Runnable()
            {
                public void run()
                {
                    d.processObjectChangeEvents(oce);
                }
            });
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see java.io.File#list()
     * 
     * Per MHP 11.5.1.1, list does a load if not loaded already. If given a
     * generic File object, the load does not truly happen, as there is no
     * "isLoaded()" method on File. However, on a DSMCCObject, we need to
     * actually do the synchronous load.
     */
    public String[] list()
    {
        FileSys fs = FileSysManager.getFileSys(getRealPath());
        if (fs instanceof LoadedFileSys)
        {
            return fs.list(getRealPath());
        }

        try
        {
            synchronousLoad();
            return fs.list(getRealPath());
        }
        catch (ServiceXFRException e)
        {
            // Per MHP 11.5.1.1, return empty array if ServiceXFR is encountered
            // during load.
            return new String[0];
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Deserialize an object, and initialize any non-serialized fields.
     * 
     * @param aInputStream
     *            The stream to read.
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException
    {
        // always perform the default de-serialization first
        aInputStream.defaultReadObject();

        construct(this);
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCObject.class.getName());
}
