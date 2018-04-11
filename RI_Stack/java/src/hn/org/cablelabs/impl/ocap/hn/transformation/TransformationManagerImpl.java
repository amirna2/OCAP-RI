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

package org.cablelabs.impl.ocap.hn.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.util.HNEventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ProtectionType;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationListener;
import org.ocap.hn.transformation.TransformationManager;

public class TransformationManagerImpl extends TransformationManager
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(TransformationManagerImpl.class);
    
    /** For initializing the JNI code */
    private static native void jniInit();

    /** permissions */
    private static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");
    
    /** Instance variable **/
    private static TransformationManager s_transformationManager = null;
    
    /** Store default transforms, final so never null and independent from incoming / outgoing arrays **/
    private final TransformationImpl m_availableTransformations[];
    
    /** Store default transforms, final so never null and independent from incoming / outgoing arrays **/
    private final ArrayList m_defaultTransformations = new ArrayList();
    
    /**
     * List of <code>CallerContext</code>s that have added listeners.
     */
    private CallerContext ccList;

    private CallerContextManager ccm = (CallerContextManager)
                                       ManagerManager.getInstance(CallerContextManager.class);    
    
    /**
     * Static class initializer.
     */
    static
    {
        OcapMain.loadLibrary();

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        jniInit(); // Allow the JNI associated with this class to initialize
    }
    
    public static TransformationManager getInstance()
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        return getInstanceRegardless();
    }
    
    public static TransformationManager getInstanceRegardless()
    {
        synchronized (TransformationManagerImpl.class)
        {
            if (s_transformationManager == null)
            {
                s_transformationManager = new TransformationManagerImpl();
            }
        }
        return s_transformationManager;
    }
    
    protected TransformationManagerImpl()
    {
        m_availableTransformations = constructAvailableTransformations();
    }
    
    private TransformationImpl[] constructAvailableTransformations()
    {
        final NativeContentTransformation nativeTransforms[] = getNativeContentTransformations();
        
        if (nativeTransforms == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("TransformationManagerImpl.constructAvailableTransformations: No transformations array returned (see JNI logging for error)");
            }
            return new TransformationImpl[0];
        }
        
        // The native formats are organized in (informat,outformat) pairs. These are 1-for-1
        //  with Transformation objects
        final Collection transforms;
        
        List profileList = new ArrayList(nativeTransforms.length);
        for (int i=0; i<nativeTransforms.length; i++)
        {
            final NativeContentTransformation curNativeTransform = nativeTransforms[i];
            if (log.isDebugEnabled())
            {
                log.debug("available transform: " + curNativeTransform);
            }
            
            ContentFormatImpl sourceContentFormat 
                = new ContentFormatImpl( curNativeTransform.sourceProfile, 
                                         ProtectionType.DTCP_IP );
            OutputVideoContentFormatImpl newOutputContentFormat 
                = new OutputVideoContentFormatImpl( curNativeTransform,
                                                    ProtectionType.DTCP_IP );
            TransformationImpl transformation 
                = new TransformationImpl(sourceContentFormat, newOutputContentFormat);
            profileList.add(transformation);
        } // END for
    
        transforms = profileList;
        TransformationImpl [] availableTransformations = new TransformationImpl[transforms.size()];
        transforms.toArray(availableTransformations);
        
        if (log.isDebugEnabled())
        {
            for (int i=0; i<availableTransformations.length; i++)
            {
                log.debug("Transform " + i + ": " + availableTransformations[i]);
            }
        }
        
        return availableTransformations;
    }
    
    /**
     * Get the list of NativeTransformationCapabilities from the platform.
     * (These require munging to become Transformations)
     * 
     * @return Array of native transformations
     */
    public native NativeContentTransformation [] getNativeContentTransformations();
    
    public synchronized void addTransformationListener(TransformationListener listener)
    {
        if (listener == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addTransformationListener - listener is null");
            }            
            return;
        }

        Data data = getData(ccm.getCurrentContext());
        data.transformationListeners =
            HNEventMulticaster.add(data.transformationListeners, listener);
    }

    public synchronized void removeTransformationListener(TransformationListener listener)
    {
        if (listener == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeTransformationListener - listener is null");
            }            
            return;
        }

        Data data = getData(ccm.getCurrentContext()); 
        if (data.transformationListeners != null)
        {
            data.transformationListeners =
                HNEventMulticaster.remove(data.transformationListeners, listener);
        }
        if (data.transformationListeners == null)
        {
            CallerContext ctx = ccm.getCurrentContext();
            ctx.removeCallbackData(this);
            ccList = CallerContext.Multicaster.remove(ccList, ctx);
        }

    }

    public Transformation[] getSupportedTransformations()
    {
        final Transformation[] transforms = m_availableTransformations;

        if(log.isDebugEnabled())
        {
            log.debug(transforms.length + " transforms returned.");
        }
        return transforms;
    }

    public synchronized Transformation[] setDefaultTransformations(Transformation[] transformations)
    {
        if (transformations == null)
        {
            throw new IllegalArgumentException("null Transformation array provided");
        }
        
        Transformation[] oldTransformations = 
                (Transformation[])m_defaultTransformations.toArray(new Transformation[m_defaultTransformations.size()]);
        
        // Reuse transformation list
        m_defaultTransformations.clear();
        
        if(transformations != null)
        {
            for(int i = 0; i < transformations.length; i++)
            {
                m_defaultTransformations.add(transformations[i]);
            }
        }
        
        if(log.isDebugEnabled())
        {
            log.debug("Set " + m_defaultTransformations.size() + " default transformations.");
        }
        
        return oldTransformations;
    }

    public synchronized Transformation[] getDefaultTransformations()
    {
        if(log.isDebugEnabled())
        {
            log.debug("Getting " + m_defaultTransformations.size() + " default transformations.");
        }
        
        return (Transformation[])m_defaultTransformations.toArray(new Transformation[m_defaultTransformations.size()]);
    }

    public Transformation[] getTransformations(ContentItem item)
    {
        if(item instanceof Transformable)
        {
            final List tList = ((Transformable)item).getTransformations();
            return (Transformation[])tList.toArray(new Transformation[tList.size()]);
        }
        
        if(log.isDebugEnabled())
        {
            log.debug("ContentItem is not of a proper type. Type = " + 
                    item == null ? "null" : item.getClass().getName());
        }
        
        return null;
    }

    public void setTransformations(Transformation[] transformations)
    {
        if (transformations == null)
        {
            throw new IllegalArgumentException("null transformations array supplied");
        }
        
        ContentList entries = MediaServer.getInstance().getCDS().getRootContainer().getEntries(null, true);
        ArrayList contentItems = new ArrayList();
        
        while(entries.hasMoreElements())
        {
            ContentEntry entry = (ContentEntry)entries.nextElement();
            if(entry instanceof Transformable)
            {
                contentItems.add(entry);
            }
        }
        
        setTransformations((ContentItem[])contentItems.toArray(new ContentItem[contentItems.size()]), transformations);        
    }

    public void setTransformations(ContentItem[] items)
    {
        setTransformations(items, getDefaultTransformations());
    }

    // Main setTransformation method, all others funnel through here.
    public synchronized void setTransformations(ContentItem[] items, Transformation[] transformations)
    {
        if ((items == null) || (items.length == 0))
        {
            throw new IllegalArgumentException("The ContentItem array cannot be null or empty");
        }
        
        if (transformations == null)
        {
            throw new IllegalArgumentException("null transformations array supplied");
        }
        
        // Place transformations into a private list for all specified content items to reference.
        ArrayList transforms = new ArrayList();
        if(transformations != null)
        {
            for(int i = 0; i < transformations.length; i++)
            {
                final Transformation t = transformations[i];
                if (!(t instanceof TransformationImpl))
                { // The app tried to pass its own Transformation object
                    if(log.isInfoEnabled())
                    {
                        log.info("setTransformations: Attempt to set unsupported Transformation type " 
                                 + t.getClass() );
                    }
                    throw new IllegalArgumentException("Transformation " + t 
                                                       + " not supported (it did not come from getSupportedTransformations())" );
                }
                transforms.add(t);
            }
        }

        // Assert: items has 1 or more elements
        for(int i = 0; i < items.length; i++)
        {
            if(items[i] instanceof Transformable)
            {
                ((Transformable)items[i]).setTransformations(transforms);
            }
            else
            {
                if(log.isTraceEnabled())
                {
                    log.trace("setTransformations: Transformations not set on ContentItem of type. " + 
                            items[i] == null ? "null" : items[i].getClass().getName());
                }
            }
        }
        
        if(log.isDebugEnabled())
        {
            log.debug("setTransformations: Set " + transforms.size() + " on " + items.length + " ContentItems.");
        }
    }
    
    /**
     * This function will return the OutputVideoContentFormatExt with the provided native ID
     *  
     * @param id The numerical ID of the native transformation
     * 
     * @return The OutputVideoContentFormatExt corresponding to the ID
     */
    public synchronized OutputVideoContentFormatExt getOutputContentFormatForID(final int id)
    {
        for (int i=0; i<m_availableTransformations.length;i++)
        {
            final Transformation curTransform = m_availableTransformations[i];
            final OutputVideoContentFormatExt ocf = (OutputVideoContentFormatExt)
                                                    curTransform.getOutputContentFormat();
            if ((ocf != null) && (id == ocf.getOutputFormatId()))
            {
                return ocf;
            }
        }
        return null;
    }


    /**
     * Return true if 1 or more TransformationManagers are registered. This should be 
     * called prior to 
     * @return
     */
    public boolean transformationListenerRegistered()
    {
        return (this.ccList != null);
    }

    /**
     * Enqueue a notifyTransformationReady indication for all registered TransformationListeners.
     * 
     * This notification helper will cause a notifyTransformationReady indication
     * to be invoked for a single Transformation in the context of the registered listener. 
     * The caller of this function will not be blocked while the notifications are performed.
     * 
     * @param item The ContentItem being notified
     * @param transformations The Transformations to notify for
     */
    public synchronized void enqueueTransformationReadyNotification( final ContentItem item, 
                                                                     final Transformation transform )
    {
        // Send notification to all registered listeners
        CallerContext ctx = ccList;
        
        if (ctx != null)
        {
            ctx.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    Data data = (Data)ctx.getCallbackData(TransformationManagerImpl.this);
                    if (data != null && data.transformationListeners != null)
                    { 
                        data.transformationListeners.notifyTransformationReady(item, transform);
                    }
                }
           });
        }
    }
 
    /**
     * Enqueue a notifyTransformationFailed indication for all registered TransformationListeners.
     * 
     * This notification helper will cause a notifyTransformationFailed indication
     * to be invoked for a single Transformation in the context of the registered listener. 
     * The caller of this function will not be blocked while the notifications are performed.
     * 
     * @param item The ContentItem being notified
     * @param transformations The Transformation to notify for
     */
    public synchronized void enqueueTransformationFailedNotification( final ContentItem item, 
                                                                      final Transformation transform,
                                                                      final int reasonCode )
    {
        // Send notification to all registered listeners
        CallerContext ctx = ccList;
        
        if (ctx != null)
        {
            ctx.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    Data data = (Data)ctx.getCallbackData(TransformationManagerImpl.this);
                    if (data != null && data.transformationListeners != null)
                    {
                        data.transformationListeners.notifyTransformationFailed(
                                                         item, transform, reasonCode );
                    }
                }
            });
        }

    } 

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
        Data data = (Data) ctx.getCallbackData(TransformationManagerImpl.this);
        if (data == null)
        {
            data = new Data();
            ctx.addCallbackData(data, TransformationManagerImpl.this);
            ccList = CallerContext.Multicaster.add(ccList, ctx);
        }
        return data;
    }

    /**
     * Per-context global data. Remembers per-context
     * <code>TransformationListener</code>s.
     */
    private class Data implements CallbackData
    {
        public TransformationListener transformationListeners;

        public void destroy(CallerContext cc)
        {
            synchronized (TransformationManagerImpl.this)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(TransformationManagerImpl.this);
                ccList = CallerContext.Multicaster.remove(ccList, cc);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }
}
