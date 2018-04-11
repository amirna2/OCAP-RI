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

package org.cablelabs.xlet.DvrTest;

import java.util.Enumeration;
import java.util.EventObject;
import java.util.Vector;

import javax.tv.service.selection.PresentationChangedEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;

import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.RecordingTerminatedEvent;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;

class DvrEventPrinter
{

    static String FailSet = " :-(_:-(_Setting_FAILED_FLAG_:-(_:-(";

    static String Fail = " :-(_:-(_FAILED_:-(_:-(";

    static String Pass = " :-)_:-)_PASSED _:-)_:-)";

    static String FindObjFailed = "testframework_error_findObject_failed_on: ";

    /**
     * print all events in the queue.
     * 
     */
    public static void printEventQueue(Vector eventQueue)
    {
        System.out.println("____print_eventQ_start____");
        for (Enumeration e = eventQueue.elements(); e.hasMoreElements();)
        {
            System.out.println("Event Info: BEGIN");
            DvrEventPrinter.printEvent((EventObject) e.nextElement());
            System.out.println("Event Info: END");
        }
        System.out.println("____print_eventQ_end____");
    }

    public static void bufferredPrintEventQueue(Vector eventQueue)
    {
        StringBuffer sb = new StringBuffer(1024);

        sb.append("____print_eventQ_start____\n");
        for (Enumeration e = eventQueue.elements(); e.hasMoreElements();)
        {
            sb.append("Event Info: BEGIN\n");
            DvrEventPrinter.printEvent((EventObject) e.nextElement());
            sb.append("Event Info: END\n");
        }
        sb.append("____print_eventQ_end");
        System.out.println(sb.toString());
    }

    /**
     * 
     * @param event
     */
    static void printEvent(EventObject event)
    {
        if (event instanceof RecordingAlertEvent)
        {
            printRecordingAlertEvent((RecordingAlertEvent) event);
        }
        else if (event instanceof RecordingChangedEvent)
        {
            printRecordingChangedEvent((RecordingChangedEvent) event);
        }
    }

    static void printRecordingAlertEvent(RecordingAlertEvent ral)
    {
        RecordingRequest rr = ral.getRecordingRequest();

        System.out.println();
        System.out.println("\n____RecordingAlertEvent_trace_begin____");
        System.out.println("Event     :   " + ral);
        System.out.println("____RecordingRequest_information____ ");
        printRecording(ral.getRecordingRequest());
        // System.out.println("<<<< RecordingAlertEvent Trace END >>>>");
        System.out.println();

    }

    static void printRecordingAlertEvent(RecordingAlertEvent ral, StringBuffer buff)
    {
        RecordingRequest rr = ral.getRecordingRequest();

        buff.append("____RecordingAlertEvent_trace_begin____\n");
        buff.append("Event     :   " + ral + "\n");
        buff.append("____RecordingRequest_Information____ \n");
        printRecording(ral.getRecordingRequest());
        buff.append("___RecordingAlertEvent_trace_end___\n");
    }

    static void printRecordingChangedEvent(RecordingChangedEvent rce)
    {
        RecordingRequest rr = rce.getRecordingRequest();
        System.out.println();
        System.out.println("____RecordingChangedEvent_trace_begin____");
        System.out.println("rr_EventObj  : " + rce);
        System.out.println("rr_NewState  : " + xletState(rce.getState(), rr));
        System.out.println("rr_OldState  : " + xletState(rce.getOldState(), rr));
        System.out.println("rr_ChangeRsn : " + xletChangeRsn(rce));
        System.out.println("____RecordingRequest_information____");
        System.out.println("____RecordingChangedEvent_trace_end____");
        printRecording(rce.getRecordingRequest());
        System.out.println();
    }

    static void printRecordingChangedEvent(RecordingChangedEvent rce, StringBuffer buff)
    {
        RecordingRequest rr = rce.getRecordingRequest();

        buff.append("\n____RecordingChangedEvent_trace_begin____\n");
        buff.append("rr_EventObj  : " + rce + "\n");
        buff.append("rr_NewState  : " + xletState(rce.getState(), rr) + "\n");
        buff.append("rr_OldState  : " + xletState(rce.getOldState(), rr) + "\n");
        buff.append("rr_ChangeRsn : " + xletChangeRsn(rce) + "\n");
        buff.append("____RecordingRequest_information____\n");
        printRecording(rce.getRecordingRequest());
        buff.append("____RecordingChangedEvent_trace_end____\n");
    }

    static void printRecording(RecordingRequest rr, StringBuffer buff)
    {

        OcapRecordedService service = null;
        try
        {
            RecordingSpec rs = rr.getRecordingSpec();

            if (rr instanceof ParentRecordingRequest)
            {
                buff.append("RecordingType=ParentRecordingRequest\n");
            }
            else
            {

                buff.append("RecordingType=LeafRecordingRequest\n");
            }

            OcapRecordingProperties orp = null;
            RecordingProperties rp = null;
            try
            {
                // Every spec has a recording properties, the question is
                // if it is of type OcapRecordingProperties.
                rp = rs.getProperties();
            }
            catch (Exception e)
            {
                buff.append("Exception from rr.getProperties, bad recording aborting print.\n");
                return;
            }

            if (rs instanceof PrivateRecordingSpec)
            {
                PrivateRecordingSpec prs = (PrivateRecordingSpec) rr.getRecordingSpec();
                buff.append("prs_privData :  " + (String) prs.getPrivateData() + "\n");
                buff.append("rr_children  :  " + ((ParentRecordingRequest) rr).getKnownChildren().size() + "\n");
                buff.append("rr_state     :  " + xletState(rr) + "\n");
                buff.append("rr_isRoot    :  " + rr.isRoot() + "\n");
                buff.append("rr_hasParent :  " + (rr.isRoot() ? "no" : "yes") + "\n");
                if (!rr.isRoot()) buff.append("rr_parent : " + rr.getParent() + "\n");
                buff.append("rr_root     :" + rr.getRoot() + "\n");
            }

            if (rs instanceof LocatorRecordingSpec)
            {
                LocatorRecordingSpec lrs = (LocatorRecordingSpec) rs;
                buff.append("rr_state     :  " + xletState(rr) + "\n");
                buff.append("rr_isRoot    :  " + rr.isRoot() + "\n");
                buff.append("rr_hasParent :  " + (rr.isRoot() ? "no" : "yes") + "\n");
                if (!rr.isRoot()) buff.append("rr_parent : " + rr.getParent() + "\n");
                buff.append(("rr_root         :" + rr.getRoot()) + "\n");
                if (rp instanceof OcapRecordingProperties)
                {
                    orp = (OcapRecordingProperties) rp;
                }
                buff.append("rr_start_time(ms):  " + lrs.getStartTime() + "\n");
                buff.append("rr_duration  :  " + lrs.getDuration() + "\n");
                // buff.append("Exp (secs):  " +
                // lrs.getProperties().getExpirationPeriod()+ "\n");
            }

            if (rs instanceof ServiceContextRecordingSpec)
            {
                ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec) rs;
                buff.append("rr_start_time:" + scrs.getStartTime() + "\n");
                buff.append("rr_duration  :" + scrs.getDuration() + "\n");
                // buff.append("Exp (secs):" +
                // scrs.getProperties().getExpirationPeriod()+ "\n");
            }

            buff.append("___Recording_properties____");
            if (rp instanceof OcapRecordingProperties)
            {
                orp = (OcapRecordingProperties) rp;
                buff.append("Requested_bit_rate:" + orp.getBitRate() + "\n");

            }
            buff.append("rops_exp(sec):  " + rp.getExpirationPeriod() + "\n");

            buff.append("props_priority:" + orp.getPriorityFlag() + "\n");
            buff.append("rr_rec_req_state   :" + rr.getState() + "\n");

        }
        catch (Exception e)
        {
            buff.append("Ignoring exception during printout\n");
            return;
        }

        try
        {
            if (rr instanceof LeafRecordingRequest)
            {
                service = (OcapRecordedService) ((LeafRecordingRequest) rr).getService();
            }
        }
        catch (Exception e)
        {
            buff.append("Exception_bad_recording_aborting_print.\n");
        }

        try
        {
            if (service != null)
            {
                buff.append("Recorded_Service_Locator: " + service.getLocator() + "\n");
                buff.append("Service_Service_Name: " + service.getName() + "\n");
                buff.append("Recorded_Size: " + service.getRecordedSize() + "\n");
                buff.append("Recorded_BitRate:" + service.getRecordedBitRate() + "\n");
                buff.append("Recorded_Duration:" + service.getRecordedDuration() + "\n");
                buff.append("Recorded_Media_Locator:" + service.getMediaLocator().toString() + "\n");
            }
        }
        catch (Exception e)
        {
            buff.append("Ignoring_exception_during_printout\n");
        }

    }

    static void printRecording(RecordingRequest rr)
    {
        OcapRecordedService service = null;
        try
        {
            RecordingSpec rs = rr.getRecordingSpec();

            if (rr instanceof ParentRecordingRequest)
            {
                System.out.println("RecordingType=ParentRecordingRequest");
            }
            else
            {

                System.out.println("RecordingType=LeafRecordingRequest");
            }

            OcapRecordingProperties orp = null;
            try
            {
                orp = (OcapRecordingProperties) rs.getProperties();
            }
            catch (Exception e)
            {
                System.out.println("Exception_from_rr.getProperties_bad_recording_aborting_print.");
                return;
            }

            if (rs instanceof PrivateRecordingSpec)
            {
                PrivateRecordingSpec prs = (PrivateRecordingSpec) rr.getRecordingSpec();
                System.out.println("prs_priv_data :  " + (String) prs.getPrivateData());
                if (rr.getState() == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE
                        || rr.getState() == ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
                    System.out.println("rr_children  :  " + ((ParentRecordingRequest) rr).getKnownChildren().size());
                DVRTestRunnerXlet.log("rr_state     :  " + xletState(rr));
                System.out.println("rr_isRoot    :  " + rr.isRoot());
                System.out.println("rr_hasParent :  " + rr.getParent());
                System.out.println("rr_root     :" + rr.getRoot());
            }

            if (rs instanceof LocatorRecordingSpec)
            {
                LocatorRecordingSpec lrs = (LocatorRecordingSpec) rs;
                DVRTestRunnerXlet.log("rr_state     :  " + xletState(rr));
                System.out.println("rr_isRoot    :  " + rr.isRoot());
                System.out.println("rr_hasParent :  " + rr.getParent());
                System.out.println(("rr_root         :" + rr.getRoot()));
                DVRTestRunnerXlet.log("rr_start_time(ms):  " + lrs.getStartTime());
                DVRTestRunnerXlet.log("rr_duration  :  " + lrs.getDuration());
                System.out.println("rr_exp(sec):  " + lrs.getProperties().getExpirationPeriod());
            }

            if (rs instanceof ServiceContextRecordingSpec)
            {
                ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec) rs;
                DVRTestRunnerXlet.log("rr_start_time:" + scrs.getStartTime());
                DVRTestRunnerXlet.log("rr_duration  :" + scrs.getDuration());
                System.out.println("rr_exp(secs):" + scrs.getProperties().getExpirationPeriod());
            }

            System.out.println("rr_priority:" + orp.getPriorityFlag());
            System.out.println("rr_state   :" + rr.getState());
            System.out.println("props_requested_BitRate:" + orp.getBitRate());
        }
        catch (Exception e)
        {
            System.out.println("Exception caught during printout: " + e);
            e.printStackTrace();
            return;
        }

        try
        {
            if (rr instanceof LeafRecordingRequest)
            {
                service = (OcapRecordedService) ((LeafRecordingRequest) rr).getService();
            }
        }
        catch (Exception e)
        {
            System.out.println("No associated recorded service or corrupted recorded service.");
        }

        try
        {
            if (service != null)
            {
                System.out.println("Recorded_Service_Locator: " + service.getLocator());
                System.out.println("Service_Service_Name: " + service.getName());
                System.out.println("Recorded_Size: " + service.getRecordedSize());
                System.out.println("Recorded_BitRate:" + service.getRecordedBitRate());
                System.out.println("Recorded_Duration:" + service.getRecordedDuration());
                System.out.println("Recorded_Media_Locator:" + service.getMediaLocator().toString());
            }
        }
        catch (Exception e)
        {
            System.out.println("Ignoring exception during printout");
        }
    }

    static String xletLeafState(int state)
    {
        String result = null;

        switch (state)
        {
            case LeafRecordingRequest.COMPLETED_STATE:
                result = new String("LeafRecordingRequest.COMPLETED_STATE");
                break;

            case LeafRecordingRequest.DELETED_STATE:
                result = new String("LeafRecordingRequest.DELETED_STATE");
                break;

            case LeafRecordingRequest.FAILED_STATE:
                result = new String("LeafRecordingRequest.FAILED_STATE");
                break;

            case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                result = new String("LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE");
                break;

            case LeafRecordingRequest.IN_PROGRESS_STATE:
                result = new String("LeafRecordingRequest.IN_PROGRESS_STATE");
                break;

            case LeafRecordingRequest.INCOMPLETE_STATE:
                result = new String("LeafRecordingRequest.INCOMPLETE_STATE");
                break;

            case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                result = new String("LeafRecordingRequest.PENDING_NO_CONFLICT_STATE");
                break;

            case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                result = new String("LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE");
                break;

            case OcapRecordingRequest.CANCELLED_STATE:
                result = new String("OcapRecordingRequest.CANCELLED_STATE");
                break;
        }

        return result;
    }

    static String xletSCE(ServiceContextEvent sce)
    {
        if (sce instanceof PresentationTerminatedEvent)
        {
            switch (((PresentationTerminatedEvent) sce).getReason())
            {
                case PresentationTerminatedEvent.ACCESS_WITHDRAWN:
                    return "PresentationTerminatedEvent.ACCESS_WITHDRAWN";

                case PresentationTerminatedEvent.RESOURCES_REMOVED:
                    return "PresentationTerminatedEvent.RESOURCES_REMOVED";

                case PresentationTerminatedEvent.SERVICE_VANISHED:
                    return "PresentationTerminatedEvent.SERVICE_VANISHED";

                case PresentationTerminatedEvent.TUNED_AWAY:
                    return "PresentationTerminatedEvent.TUNED_AWAY";

                case PresentationTerminatedEvent.USER_STOP:
                    return "PresentationTerminatedEvent.USER_STOP";

            }
        }
        if (sce instanceof RecordingTerminatedEvent)
        {

        }
        if (sce instanceof PresentationChangedEvent)
        {

        }
        if (sce instanceof ServiceContextDestroyedEvent)
        {

        }
        if (sce instanceof TimeShiftEvent)
        {

        }
        if (sce instanceof SelectionFailedEvent)
        {

        }
        return null;
    }

    static String xletState(int state, RecordingRequest req)
    {
        if (req instanceof ParentRecordingRequest)
        {
            switch (state)
            {
                case ParentRecordingRequest.CANCELLED_STATE:
                    return "ParentRecordingRequest.CANCELLED_STATE";

                case ParentRecordingRequest.COMPLETELY_RESOLVED_STATE:
                    return "ParentRecordingRequest.COMPLETELY_RESOLVED_STATE";

                case ParentRecordingRequest.PARTIALLY_RESOLVED_STATE:
                    return "ParentRecordingRequest.PARTIALLY_RESOLVED_STATE";

                case ParentRecordingRequest.UNRESOLVED_STATE:
                    return "ParentRecordingRequest.UNRESOLVED_STATE";
            }
        }
        else
        {
            switch (state)
            {
                case LeafRecordingRequest.COMPLETED_STATE:
                    return "LeafRecordingRequest.COMPLETED_STATE";

                case LeafRecordingRequest.DELETED_STATE:
                    return "LeafRecordingRequest.DELETED_STATE";

                case LeafRecordingRequest.FAILED_STATE:
                    return "LeafRecordingRequest.FAILED_STATE";

                case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                    return "LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE";

                case LeafRecordingRequest.IN_PROGRESS_STATE:
                    return "LeafRecordingRequest.IN_PROGRESS_STATE";

                case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                    return "LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE";

                case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                    return "LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE";

                case LeafRecordingRequest.INCOMPLETE_STATE:
                    return "LeafRecordingRequest.INCOMPLETE_STATE";

                case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                    return "LeafRecordingRequest.PENDING_NO_CONFLICT_STATE";

                case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                    return "LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE";

                case OcapRecordingRequest.CANCELLED_STATE:
                    return "OcapRecordingRequest.CANCELLED_STATE";
            }
        }

        return "UnknownState = " + state;
    }

    static String xletState(RecordingRequest req)
    {
        int state = 0;

        try
        {
            state = req.getState();
            if (req instanceof ParentRecordingRequest)
            {
                switch (state)
                {
                    case ParentRecordingRequest.CANCELLED_STATE:
                        return "ParentRecordingRequest.CANCELLED_STATE";

                    case ParentRecordingRequest.COMPLETELY_RESOLVED_STATE:
                        return "ParentRecordingRequest.COMPLETELY_RESOLVED_STATE";

                    case ParentRecordingRequest.PARTIALLY_RESOLVED_STATE:
                        return "ParentRecordingRequest.PARTIALLY_RESOLVED_STATE";

                    case ParentRecordingRequest.UNRESOLVED_STATE:
                        return "ParentRecordingRequest.UNRESOLVED_STATE";
                }
            }
            else
            {
                switch (state)
                {
                    case LeafRecordingRequest.COMPLETED_STATE:
                        return "LeafRecordingRequest.COMPLETED_STATE";

                    case LeafRecordingRequest.DELETED_STATE:
                        return "LeafRecordingRequest.DELETED_STATE";

                    case LeafRecordingRequest.FAILED_STATE:
                        return "LeafRecordingRequest.FAILED_STATE";

                    case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                        return "LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE";

                    case LeafRecordingRequest.IN_PROGRESS_STATE:
                        return "LeafRecordingRequest.IN_PROGRESS_STATE";

                    case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                        return "LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE";

                    case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                        return "LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE";

                    case LeafRecordingRequest.INCOMPLETE_STATE:
                        return "LeafRecordingRequest.INCOMPLETE_STATE";

                    case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        return "LeafRecordingRequest.PENDING_NO_CONFLICT_STATE";

                    case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                        return "LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE";

                    case OcapRecordingRequest.CANCELLED_STATE:
                        return "OcapRecordingRequest.CANCELLED_STATE";
                }
            }

        }
        catch (Exception e)
        {
            System.out.println("xletState_ignoring_exception_during_printout");
        }

        return null;
    }

    public static String xletRecFailedRsn(int state)
    {
        switch (state)
        {
            case RecordingFailedException.ACCESS_WITHDRAWN:
                return "RecordingFailedException.ACCESS_WITHDRAWN";

            case RecordingFailedException.CA_REFUSAL:
                return "RecordingFailedException.CA_REFUSAL";

            case RecordingFailedException.CONTENT_NOT_FOUND:
                return "RecordingFailedException.CONTENT_NOT_FOUND";

            case RecordingFailedException.INSUFFICIENT_RESOURCES:
                return "RecordingFailedException.INSUFFICIENT_RESOURCES";

            case RecordingFailedException.OUT_OF_BANDWIDTH:
                return "RecordingFailedException.OUT_OF_BANDWIDTH";

            case RecordingFailedException.RESOLUTION_ERROR:
                return "RecordingFailedException.RESOLUTION_ERROR";

            case RecordingFailedException.RESOURCES_REMOVED:
                return "RecordingFailedException.RESOURCES_REMOVED";

            case RecordingFailedException.SERVICE_VANISHED:
                return "RecordingFailedException.SERVICE_VANISHED";

            case RecordingFailedException.SPACE_FULL:
                return "RecordingFailedException.SPACE_FULL";

            case RecordingFailedException.TUNED_AWAY:
                return "RecordingFailedException.TUNED_AWAY";

            case RecordingFailedException.TUNING_FAILURE:
                return "RecordingFailedException.TUNING_FAILURE";

            case RecordingFailedException.USER_STOP:
                return "RecordingFailedException.USER_STOP";

            case RecordingFailedException.POWER_INTERRUPTION:
                return "RecordingFailedException.POWER_INTERRUPTION";

            default:
                return null;
        }
    }

    static String xletChangeRsn(RecordingChangedEvent rce)
    {
        switch (rce.getChange())
        {
            case RecordingChangedEvent.ENTRY_ADDED:
                return "RecordingChangedEvent.ENTRY_ADDED";

            case RecordingChangedEvent.ENTRY_DELETED:
                return "RecordingChangedEvent.ENTRY_DELETED";

            case RecordingChangedEvent.ENTRY_STATE_CHANGED:
                return "RecordingChangedEvent.ENTRY_STATE_CHANGED";

        }
        return null;
    }

    static String xletDeletionDetailsReason(int reason)
    {
        return (reason == DeletionDetails.EXPIRED) ? "Reason_Expired" : "Reason_User_Deleted";
    }
}
