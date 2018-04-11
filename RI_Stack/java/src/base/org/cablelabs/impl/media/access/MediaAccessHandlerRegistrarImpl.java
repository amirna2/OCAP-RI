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

package org.cablelabs.impl.media.access;

import java.util.Enumeration;
import java.util.Vector;

import javax.media.Player;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SecurityUtil;
import org.davic.mpeg.ElementaryStream;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.system.MonitorAppPermission;

/**
 * Implementation of <code>MediaAccessHandlerRegistrar<code>.
 * 
 * @author Todd Earles
 */
public class MediaAccessHandlerRegistrarImpl extends MediaAccessHandlerRegistrar
{
    /** Log4J */
    private static final Logger log = Logger.getLogger(MediaAccessHandlerRegistrarImpl.class);
    private static final Logger performanceLog = Logger.getLogger("Performance.ServiceSelection");

    /**
     * Constructor
     */
    public MediaAccessHandlerRegistrarImpl()
    {
    }

    // Description copied from MediaAccessHandlerRegistrar
    public synchronized void registerMediaAccessHandler(MediaAccessHandler handler)
    {
        // Make sure caller has permission
        SecurityUtil.checkPermission(new MonitorAppPermission("mediaAccess"));

        // Dispose of current handler
        currentMAH.dispose();

        if (log.isInfoEnabled())
        {
            log.info("registerMediaAccessHandler: " + handler);
        }

        // Set new handler
        if (handler == null)
        {
            currentMAH = new DefaultMAH();
        }
        else
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext ctx = ccm.getCurrentContext();
            currentMAH = new ApplicationMAH(ctx, handler);
        }
    }

    // Description copied from MediaAccessHandlerRegistrar
    public void setExternalTriggers(MediaPresentationEvaluationTrigger[] extTriggers)
    {
        // Nothing from the calling application is held on to here so we don't
        // have to worry about the calling context going away. We keep the
        // original array reference but that is just an object in the Java heap
        // and is not affected by the calling context going away.

        // Set triggers as long as the caller has permission and all specified
        // triggers are classified as optional.
        SecurityUtil.checkPermission(new MonitorAppPermission("mediaAccess"));
        for (int i = 0; i < extTriggers.length; i++)
        {
            if (!extTriggers[i].isOptional())
                throw new IllegalArgumentException("Trigger " + extTriggers[i] + " is not optional");
        }
        this.triggers = extTriggers;
    }

    /**
     * Check whether a trigger is being handled by the external monitor
     * application.
     * 
     * @param trigger
     *            The trigger to check.
     * @return Return true if trigger is handled by monitor app. Otherwise,
     *         return false.
     */
    public boolean isTriggerExternal(MediaPresentationEvaluationTrigger trigger)
    {
        for (int i = 0; i < triggers.length; i++)
            if (triggers[i].equals(trigger)) return true;
        return false;
    }

    /**
     * Check authorization
     */
    public synchronized ComponentAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStreamExt[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
    {

        // Call registered handler
        if (performanceLog.isInfoEnabled())
        {
            performanceLog.info("Media Access Handler Invoked: Locator " +  sourceURL.toExternalForm());
        }
        ComponentAuthorization componentAuthorization = currentMAH.checkMediaAccessAuthorization(p, sourceURL, isSourceDigital, esList, evaluationTrigger);

        if (performanceLog.isInfoEnabled())
        {
            performanceLog.info("Media Access Handler Returned: Locator " + sourceURL.toExternalForm());
        }
        return componentAuthorization;
    }

    /**
     * This class defines a default <code>MediaAccessHandler</code> to be used
     * when one is not registered or when the registered one fails.
     */
    private class DefaultMAH
    {
        /**
         * Check authorization
         */
        public ComponentAuthorization checkMediaAccessAuthorization(final Player p, final OcapLocator sourceURL,
                final boolean isSourceDigital, final ElementaryStreamExt[] esList,
                final MediaPresentationEvaluationTrigger evaluationTrigger)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DefaultMAH (full auth) - checkMediaAccessAuthorization - player: " + p + ", source URL: " + sourceURL
                        + ", digital: " + isSourceDigital + ", stream list: " + Arrays.toString(esList)
                        + ", trigger: " + evaluationTrigger);
            }

            // Return full authorization
            MediaAccessAuthorization maa = new MediaAccessAuthorization()
            {
                public boolean isFullAuthorization()
                {
                    return true;
                }

                public Enumeration getDeniedElementaryStreams()
                {
                    return new Vector().elements();
                }

                public int getDenialReasons(ElementaryStream es)
                {
                    throw new IllegalArgumentException(es + " was not denied");
                }
            };

            return new ComponentAuthorization(esList, maa, sourceURL, evaluationTrigger, isSourceDigital);
        }

        /**
         * Dispose of any resources held by this object.
         */
        public void dispose()
        {
            // Nothing to do here
        }
    }

    /**
     * This class wraps an application defined <code>MediaAccessHandler</code>.
     */
    private class ApplicationMAH extends DefaultMAH implements CallbackData
    {
        /**
         * Constructor
         */
        public ApplicationMAH(CallerContext cc, MediaAccessHandler mah)
        {
            this.ctx = cc;
            this.handler = mah;

            cc.addCallbackData(this, ApplicationMAH.class);
        }

        /**
         * Check authorization
         */
        public ComponentAuthorization checkMediaAccessAuthorization(final Player p, final OcapLocator sourceURL,
                final boolean isSourceDigital, final ElementaryStreamExt[] esList,
                final MediaPresentationEvaluationTrigger evaluationTrigger)
        {
            final ComponentAuthorization[] authorization = { null };
            if (CallerContext.Util.doRunInContextSync(ctx, new Runnable()
            {
                public void run()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("ApplicationMAH - checkMediaAccessAuthorization - player: " + p + ", source URL: " + sourceURL
                                + ", digital: " + isSourceDigital + ", stream list: " + Arrays.toString(esList)
                                + ", trigger: " + evaluationTrigger);
                    }
                    MediaAccessAuthorization maa = handler.checkMediaAccessAuthorization(p, sourceURL, isSourceDigital,
                            esList, evaluationTrigger);
                    authorization[0] = new ComponentAuthorization(esList, maa, sourceURL, evaluationTrigger, isSourceDigital);
                }
            })) return authorization[0];

            // Call the default handler
            return super.checkMediaAccessAuthorization(p, sourceURL, isSourceDigital, esList, evaluationTrigger);
        }

        // Description copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Description copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Description copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // The application which caused creation of this object has been
            // destroyed
            // so cleanup.
            synchronized (MediaAccessHandlerRegistrarImpl.this)
            {
                // It is not necessary to call dispose() here since the entire
                // calling
                // context is being shutdown. If this handler is the current one
                // registered then replace it with the default handler.
                if (currentMAH == this) currentMAH = new DefaultMAH();
            }
        }

        // Description copied from DefaultMAH
        public void dispose()
        {
            // Remove the CallbackData
            ctx.removeCallbackData(ApplicationMAH.class);
        }

        /** The caller context used to execute the handler */
        private CallerContext ctx;

        /** The registered handler */
        private MediaAccessHandler handler;
    }

    /** The current media access handler */
    private DefaultMAH currentMAH = new DefaultMAH();

    /** The current external triggers */
    private MediaPresentationEvaluationTrigger[] triggers = new MediaPresentationEvaluationTrigger[0];
}
