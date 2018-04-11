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
package org.cablelabs.impl.media.player;

import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.pod.CAElementaryStreamAuthorization;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.access.MediaAccessHandlerRegistrarImpl;
import org.cablelabs.impl.media.access.CASessionMonitor;
import org.cablelabs.impl.media.presentation.Selection;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.davic.mpeg.ElementaryStream;
import org.ocap.media.AlternativeMediaPresentationReason;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;

public class BroadcastAuthorization
{
    private static final Logger log = Logger.getLogger(BroadcastAuthorization.class);

    private final ServicePlayer player;

    private final boolean blockOverride;

    public BroadcastAuthorization(ServicePlayer player, boolean blockOverride)
    {
        this.player = player;
        this.blockOverride = blockOverride;
    }

    public ComponentAuthorization verifyConditionalAccessAuthorization(ServiceDetailsExt serviceDetails, ExtendedNetworkInterface networkInterface,
                                                                       ElementaryStreamExt[] elementaryStreams, ServiceComponentExt[] components,
                                                                       CASessionMonitor caMonitor, boolean isDefaultComponents, OcapLocator sourceURL,
                                                                       MediaPresentationEvaluationTrigger trigger, boolean digital, boolean startNewSession) throws MPEException
    {
        if (log.isInfoEnabled())
        {
            log.info("verifyConditionalAccessAuthorization - serviceDetails: " + serviceDetails + ", components: " + Arrays.toString(components) + ", start new session: " + startNewSession);
        }
        //full CA authorization used if no denial reasons
        ComponentAuthorization fullCAAuthorization = new ComponentAuthorization(elementaryStreams, components, sourceURL, trigger, digital, new int[0], new int[0]);
        if (components.length == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("No components available - returning full authorization ");
            }
            return fullCAAuthorization;
        }
        //if presenting EAS, bypass conditional access checks
        if (blockOverride)
        {
            return fullCAAuthorization;
        }
        //either a new CASession needs to be started or the last event ID is going to be used to determine access  
        if (startNewSession)
        {
            caMonitor.cleanup();
            caMonitor.startDecryptSession(serviceDetails, networkInterface, components);
        }
        int lastCASessionEventID = caMonitor.getLastCASessionEventID();
        if (lastCASessionEventID == CASessionEvent.EventID.TIMEOUT)
        {
            caMonitor.cleanup();
            throw new MPEException("timeout received starting decrypt session");
        }
        //use last event ID -only- to determine if denial reasons need to be retrieved
        boolean isFullAuthorization = (lastCASessionEventID == CASessionEvent.EventID.FULLY_AUTHORIZED);
        if (isFullAuthorization)
        {
            return fullCAAuthorization;
        }
        else
        {
            CAElementaryStreamAuthorization[] authorizations = new CAElementaryStreamAuthorization[components.length];
            //initialize pids
            for (int i=0;i<authorizations.length;i++)
            {
                //construct authorizations (reason defaults to UNDEFINED)
                authorizations[i] = new CAElementaryStreamAuthorization(components[i].getPID());
            }
            //retrieve reasons
            caMonitor.populateElementaryStreamCADenialReasons(authorizations);
            //determine if at least one is full auth up front
            boolean fullyAuthorizedPidFound = false;
            for (int i=0;i<authorizations.length;i++)
            {
                if (authorizations[i].getElementaryStreamCAReason() == CASession.CAStatus.DESCRAMBLING_POSSIBLE_NO_CONDITIONS)
                {
                    fullyAuthorizedPidFound = true;
                    break;
                }
                if (authorizations[i].getElementaryStreamCAReason() == CASession.CAStatus.UNDEFINED)
                {
                    throw new IllegalArgumentException("authorization reason not set for pid: " + authorizations[i].getElementaryStreamPid());
                }
            }
            if (player.isServiceContextPlayer() && fullyAuthorizedPidFound && !isDefaultComponents)
            {
                if (log.isInfoEnabled())
                {
                    log.info("CA authorization checked - servicecontext presentation of non-default components with at least one authorized pid - returning full authorization");
                }
                return fullCAAuthorization;
            }

            int denialIndex = 0;
            int[] deniedStreamsPids = new int[authorizations.length];
            int[] deniedStreamsReasons = new int[authorizations.length];

            for (int i=0;i<authorizations.length;i++)
            {
                int streamDenialReasonEventId = authorizations[i].getElementaryStreamCAReason();
                //determine alternative media presentation reason from CASessionEvent
                int denialReason;
                switch (streamDenialReasonEventId) {
                    case CASession.CAStatus.UNDEFINED:
                        //handled above
                        throw new IllegalArgumentException("undefined event");
                    case CASession.CAStatus.DESCRAMBLING_POSSIBLE_NO_CONDITIONS:
                        //skip streams that were authorized..only not authorized streams have denial reasons set
                        continue;
                    case CASession.CAStatus.DESCRAMBLING_POSSIBLE_WITH_CONDITIONS_PURCHASE:
                    case CASession.CAStatus.DESCRAMBLING_POSSIBLE_WITH_CONDITIONS_TECHNICAL:
                    case CASession.CAStatus.DESCRAMBLING_NOT_POSSIBLE_NO_ENTITLEMENT:
                        denialReason = AlternativeMediaPresentationReason.NO_ENTITLEMENT;
                        break;
                    case CASession.CAStatus.DESCRAMBLING_NOT_POSSIBLE_TECHNICAL:
                        denialReason = AlternativeMediaPresentationReason.HARDWARE_RESOURCE_NOT_AVAILABLE;
                        break;
                    default:
                        throw new IllegalArgumentException("unexpected event id: " + streamDenialReasonEventId);
                }
                deniedStreamsPids[denialIndex] = authorizations[i].getElementaryStreamPid();
                deniedStreamsReasons[denialIndex] = denialReason;
                denialIndex++;
            }
            return new ComponentAuthorization(elementaryStreams, components, sourceURL, trigger, digital, deniedStreamsPids, deniedStreamsReasons);
        }
    }


    /**
     * Check media access authorization on service components we want to present. The input
     * to this method is information about the currently selected service and
     * components.
     * <p/>
     * This method assumes the selected service and components are locked
     * against changes while it is executing. It is the responsibility of the
     * caller to enforce this requirement.
     * <p/>
     * This implementation currently only works with {@link ServiceComponent}
     * components, which means it won't work for
     * {@link org.ocap.shared.dvr.RecordedService} components. For now,
     * subclasses can override this method as appropriate for other component
     * types.
     * 
     * @param selection
     *            selection to authorize
     * @param networkInterface
     *            the network interface which can be used to provide elementary
     *            streams for components
     */
    public ComponentAuthorization verifyMediaAccessAuthorization(Selection selection, ExtendedNetworkInterface networkInterface)
    {
        if (selection == null || selection.getServiceDetails() == null)
        {
            throw new IllegalArgumentException("verifyMediaAccessAuthorization called with null selection or selection with null service details");
        }
        if (log.isInfoEnabled())
        {
            log.info("verifyMediaAccessAuthorization - selection: " + selection);
        }
        // Call the MediaAccessHandler to determine authorization.
        MediaAccessHandlerRegistrar mahr = MediaAccessHandlerRegistrar.getInstance();
        MediaAccessHandlerRegistrarImpl mahri = (MediaAccessHandlerRegistrarImpl) mahr;

        Service svc = selection.getServiceDetails().getService();
        ComponentAuthorization ca;

        if (blockOverride)
        {
            if (log.isInfoEnabled())
            {
                log.info("blockOverride enabled - creating full access authorization");
            }
            ca = getFullAccessMediaAuthorization((OcapLocator) svc.getLocator(), selection.isDigital(),
                    selection.getElementaryStreams(networkInterface, selection.getServiceComponents()),
                    selection.trigger);
        }
        else
        {
            ca = mahri.checkMediaAccessAuthorization(player, (OcapLocator) svc.getLocator(), selection.isDigital(),
                    selection.getElementaryStreams(networkInterface, selection.getServiceComponents()),
                    selection.trigger);
        }

        if (log.isInfoEnabled())
        {
            log.info("checkMediaAccessAuthorization called - full authorization: " + ca.isFullAuthorization());
        }

        return ca;
    }

    private ComponentAuthorization getFullAccessMediaAuthorization(OcapLocator sourceURL, boolean isSourceDigital,
            ElementaryStreamExt[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
    {
        // Return full authorization
        MediaAccessAuthorization maa = new FullAccessAuthorization();

        if (log.isDebugEnabled())
        {
            log.debug("DefaultMAH::checkMediaAccessAuthorization maa: " + maa);
        }

        return new ComponentAuthorization(esList, maa, sourceURL, evaluationTrigger, isSourceDigital);
    }

    private static class FullAccessAuthorization implements MediaAccessAuthorization
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
    }
}
