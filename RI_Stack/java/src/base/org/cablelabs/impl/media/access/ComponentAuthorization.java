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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.Controller;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.mpeg.ElementaryStream;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.media.MediaPresentationEvent;
import org.ocap.media.NormalMediaPresentationEvent;
import org.ocap.net.OcapLocator;

/**
 * Keeps track of PID-based denial status for a media decode session startup.
 */
public class ComponentAuthorization implements MediaAccessAuthorization
{
    /** Log4j */
    private static final Logger log = Logger.getLogger(ComponentAuthorization.class);

    /**
     * All components comprising the selection on which the
     * {@link ComponentAuthorization} is based.
     */
    private ServiceComponentExt[] components;

    /**
     * The trigger resulting in the authorization check
     */
    private MediaPresentationEvaluationTrigger trigger;

    /**
     * Indicates whether the source is digital
     */
    private boolean digital;

    /**
     * The url of the data source
     */
    private OcapLocator sourceURL;

    //application-provided authorization in the case of Media Access Authorization, stack provided authorization
    //in the case of Conditional Access authorization
    private MediaAccessAuthorization authorization;

    //the elementary stream array provided in the constructor (containing possibly denied and/or not denied streams)
    private ElementaryStreamExt[] streams;

    /**
     * Constructor used to represent conditional access authorization
     *
     * Constructor providing elementary streams, optional components and array where denied pid reasons
     * can be resolved from denied pid reasons using the matching array index
     *
     * @param streams the elementary streams, both denied and not denied, representing this authorization
     * @param components optional array provided component references
     * @param sourceURL OcapLocator representing the source
     * @param trigger MediaPresentationEvaluationTrigger active at the time of authorization creation
     * @param digital true if source is digital
     * @param deniedStreamsPids array of denied stream pids
     * @param deniedStreamsReasons array of denied stream reasons (index to this array is the reason for the denial
     *        of the pid in the same index of deniedStreamPids
     */
    public ComponentAuthorization(ElementaryStreamExt[] streams, ServiceComponentExt[] components,
                                  OcapLocator sourceURL, MediaPresentationEvaluationTrigger trigger, boolean digital,
                                  int[] deniedStreamsPids, int[] deniedStreamsReasons)
    {
        if (components != null)
        {
            this.components = (ServiceComponentExt[]) Arrays.copy(components, ServiceComponentExt.class);
        }
        if (streams != null)
        {
            this.streams = (ElementaryStreamExt[]) Arrays.copy(streams, ElementaryStreamExt.class);
        }
        this.sourceURL = sourceURL;
        this.trigger = trigger;
        this.digital = digital;

        List elementaryStreamAuthorizations = new ArrayList();

        if (streams != null)
        {
            for (int i=0;i<streams.length;i++)
            {
                //using reason zero (stream is denied without reason)
                //examine denied stream pids to see if a stream needs to be set to denied
                boolean denialFound = false;
                int denialReason = 0;

                if (deniedStreamsPids != null)
                {
                    for (int j=0;j<deniedStreamsPids.length;j++)
                    {
                        if (deniedStreamsPids[j] == streams[i].getPID())
                        {
                            denialFound = true;
                            denialReason = deniedStreamsReasons[j];
                        }
                    }
                }
                if (denialFound)
                {
                    elementaryStreamAuthorizations.add(new ElementaryStreamAuthorization(streams[i], true, denialReason));
                }
                else
                {
                    elementaryStreamAuthorizations.add(new ElementaryStreamAuthorization(streams[i]));
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("ComponentAuthorization constructed but streams was null");
            }
        }
        authorization = new ConditionalAccessMediaAuthorization(elementaryStreamAuthorizations);
        if (log.isInfoEnabled())
        {
            log.info("ComponentAuthorization for Conditional Access constructed - streams: " + Arrays.toString(streams) + ", locator: " + sourceURL + ", components: " + components + ", denied stream pids: " + Arrays.toString(deniedStreamsPids) +
            ", denied stream reasons: " + Arrays.toString(deniedStreamsReasons) + ", digital: " + digital + ", trigger: " + trigger + ", CA authorization constructed with: " + elementaryStreamAuthorizations);
        }
    }

    /**
     * Constructor used to represent media access authorization
     *
     * @param streams the elementary streams, both denied and not denied, representing this authorization
     * @param maa the {@link MediaAccessAuthorization#} providing denial information
     * @param sourceURL OcapLocator representing the source
     * @param trigger MediaPresentationEvaluationTrigger active at the time of authorization creation
     * @param digital true if source is digital
     */
    public ComponentAuthorization(ElementaryStreamExt[] streams, MediaAccessAuthorization maa,
            OcapLocator sourceURL, MediaPresentationEvaluationTrigger trigger, boolean digital)
    {
        if (log.isDebugEnabled())
        {
            log.debug("creating MAA component authorization - streams: " + Arrays.toString(streams));
        }
        if (streams != null)
        {
            this.streams = (ElementaryStreamExt[]) Arrays.copy(streams, ElementaryStreamExt.class);
        }
        this.sourceURL = sourceURL;
        this.trigger = trigger;
        this.digital = digital;
        authorization = new SafeMediaAccessAuthorization(maa);
        if (log.isDebugEnabled())
        {
            log.debug("ComponentAuthorization for Media Access constructed - streams: " + Arrays.toString(streams) + ", locator: " + sourceURL + ", media access authorization: " + maa +
                    ", digital: " + digital + ", trigger: " + trigger);
        }
    }

    public int getDenialReasons(ElementaryStream stream)
    {
        return authorization.getDenialReasons(stream);
    }

    public Enumeration getDeniedElementaryStreams()
    {
        return authorization.getDeniedElementaryStreams();
    }

    /**
     * Get the components for which this instance was constructed, or null if not provided.
     * 
     * @return an array of {@link ServiceComponentExt}s.
     */
    public ServiceComponentExt[] getComponents()
    {
        return (ServiceComponentExt[]) Arrays.copy(components, ServiceComponentExt.class);
    }

    public ElementaryStreamExt[] getAuthorizedStreams()
    {
        return getStreams(true);
    }

    public ElementaryStreamExt[] getDeniedStreams()
    {
        return getStreams(false);
    }

    private ElementaryStreamExt[] getStreams(boolean returnAuthorizedStreams)
    {
        Enumeration deniedStreamsEnumeration = authorization.getDeniedElementaryStreams();
        List deniedStreams = new ArrayList();
        //expecting an empty enumeration, but apps may be returninng null
        if (deniedStreamsEnumeration != null)
        {
            while (deniedStreamsEnumeration.hasMoreElements())
            {
                ElementaryStream deniedStream = (ElementaryStream) deniedStreamsEnumeration.nextElement();
                deniedStreams.add(deniedStream);
            }
        }

        //the MediaAccessAuthorization implementation may be inconsistent in returning 'true' for isFullAuthorization but return a non-empty
        //enumeration in getDeniedElementaryStreams - for the 'authorized' path, don't examine denied streams if isFullAuthorization is true
        if (returnAuthorizedStreams)
        {
            List result = new ArrayList();
            boolean isFullAuth = isFullAuthorization();
            for (int i=0;i<streams.length;i++)
            {
                if (isFullAuth || !deniedStreams.contains(streams[i]))
                {
                    result.add(streams[i]);
                }
            }
            if (log.isDebugEnabled()) 
            {
                log.debug("getStreams - returnAuthorizedStreams: " + returnAuthorizedStreams + " - full auth: " + isFullAuth + ", result: " + result);
            }
            return (ElementaryStreamExt[]) Arrays.copy(result.toArray(), ElementaryStreamExt.class);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getStreams - returnAuthorizedStreams: " + returnAuthorizedStreams + ", result: " + deniedStreams);
            }
            return (ElementaryStreamExt[]) Arrays.copy(deniedStreams.toArray(), ElementaryStreamExt.class);
        }
    }

    public boolean isFullAuthorization()
    {
        return authorization.isFullAuthorization();
    }

    /**
     * Get the presentation evaluation trigger which caused the generation of
     * this authorization
     * 
     * @return Returns the presentation evaluation trigger
     */
    public MediaPresentationEvaluationTrigger getMediaPresentationEvaluationTrigger()
    {
        return trigger;
    }

    public MediaPresentationEvent getNoReasonAlternativeMediaPresentationEvent(final Controller player)
    {
        return new AlternativeMediaPresentationEvent(player, this)
        {
            public String toString()
            {
                return "AlternativeMediaPresentationEvent: " + player;
            }
        };
    }
    /**
     * Construct a {@link MediaPresentationEvent} based on the current state of
     * the {@link ComponentAuthorization}.
     * 
     * @param player
     *            The {@link Controller} for which the event is constructed.
     * @return Returns the newly constructed event.
     */
    public MediaPresentationEvent getMediaPresentationEvent(final Controller player)
    {
        if (isFullAuthorization())
        {
            return new NormalMediaPresentationEvent(player, this)
            {
                public String toString()
                {
                    return "NormalMediaPresentationEvent: " + player;
                }
            };
        }
        else
        {
            return new AlternativeMediaPresentationEvent(player, this)
            {
                public String toString()
                {
                    return "AlternativeMediaPresentationEvent: " + player;
                }
            };
        }
    }

    /**
     * Indicates whether the source is digital
     * 
     * @return a flag indicating if the source is digital
     **/
    public boolean isDigital()
    {
        return digital;
    }

    /**
     * Return the locator for the source
     * 
     * @return the OcapLocator for the source of the streams
     */
    public OcapLocator getSourceURL()
    {
        return sourceURL;
    }

    /**
     * This wraps a {@link MediaAccessAuthorization}, providing safe access to
     * its member methods. Because a MediaAccessAuthorization contains
     * application code, the stack must not trust it and should catch unchecked
     * exceptions and do something graceful if they happen. We actually
     * encountered such a situation in certification testing where the
     * {@link #getDeniedElementaryStreams()} method caused an
     * {@link ClassCastException}, which, since it wasn't caught, caused
     * problems upstream.
     *
     * @author schoonma
     */
    static class SafeMediaAccessAuthorization implements MediaAccessAuthorization
    {
        private MediaAccessAuthorization maa;

        /**
         * Construct for a {@link MediaAccessAuthorization}.
         *
         * @param maa
         *            - the {@link MediaAccessAuthorization} to wrap.
         */
        public SafeMediaAccessAuthorization(MediaAccessAuthorization maa)
        {
            this.maa = maa;
        }

        public int getDenialReasons(ElementaryStream es)
        {
            try
            {
                return maa.getDenialReasons(es);
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("MediaAccessAuthorization.getDenialReasons() error: " + t.getMessage());
                }
                SystemEventUtil.logRecoverableError(t);
                return 0;
            }
        }

        public Enumeration getDeniedElementaryStreams()
        {
            try
            {
                return maa.getDeniedElementaryStreams();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("MediaAccessAuthorization.getDeniedElementaryStreams() error: " + t.getMessage());
                }
                SystemEventUtil.logRecoverableError(t);
                return new Vector().elements();
            }
        }

        public boolean isFullAuthorization()
        {
            try
            {
                return maa.isFullAuthorization();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("MediaAccessAuthorization.isFullAuthorization() error: " + t.getMessage());
                }
                SystemEventUtil.logRecoverableError(t);
                return true;
            }
        }

        public String toString()
        {
            return "SafeMediaAccessAuthorization";
        }
    }

    private class ConditionalAccessMediaAuthorization implements MediaAccessAuthorization
    {
        private final List elementaryStreamAuthorizations;

        public ConditionalAccessMediaAuthorization(List elementaryStreamAuthorizations)
        {
            this.elementaryStreamAuthorizations = elementaryStreamAuthorizations;
        }

        public boolean isFullAuthorization()
        {
            //for CA authorization, no denied streams = 'full authorization' 
            for (Iterator iter = elementaryStreamAuthorizations.iterator();iter.hasNext();)
            {
                ElementaryStreamAuthorization auth = (ElementaryStreamAuthorization)iter.next();
                if (auth.denied)
                {
                    return false;
                }
            }
            return true;
        }

        public Enumeration getDeniedElementaryStreams()
        {
            List result = new ArrayList();
            for (Iterator iter = elementaryStreamAuthorizations.iterator();iter.hasNext();)
            {
                ElementaryStreamAuthorization auth = (ElementaryStreamAuthorization)iter.next();
                if (auth.denied)
                {
                    result.add(auth.stream);
                }
            }
            return Collections.enumeration(result);
        }

        public int getDenialReasons(ElementaryStream es)
        {
            for (Iterator iter = elementaryStreamAuthorizations.iterator();iter.hasNext();)
            {
                ElementaryStreamAuthorization auth = (ElementaryStreamAuthorization)iter.next();
                if (auth.stream.equals(es))
                {
                    if (!auth.denied)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("getDenialReasons called but stream not denied: " + es);
                        }
                    }
                    //may not be denied, but documentation does not mention an exception should be thrown in that case
                    return auth.denialReason;
                }
            }
            if (log.isWarnEnabled())
            {
                log.warn("getDenialReasons called but stream not found: " + es);
            }
            //no denial reason found
            return 0;
        }

        public String toString()
        {
            return "ConditionalAccessAuthorization";
        }
    }

    private class ElementaryStreamAuthorization
    {
        ElementaryStreamExt stream;
        boolean denied;
        int denialReason;

        /**
         * Constructor supporting initial denial state
         * @param stream
         * @param denied
         * @param denialReason
         */
        public ElementaryStreamAuthorization(ElementaryStreamExt stream, boolean denied, int denialReason)
        {
            this.stream = stream;
            this.denied = denied;
            this.denialReason = denialReason;
        }

        /**
         * Constructor supporting initially not denied streams
         *
         * @param stream
         */
        public ElementaryStreamAuthorization(ElementaryStreamExt stream)
        {
            this.stream = stream;
            denied = false;
            denialReason = 0;
        }

        public String toString()
        {
            return "ElementaryStreamAuthorization - stream: " + stream + ", denied: " + denied + ", reason: " + denialReason;
        }
    }
}
