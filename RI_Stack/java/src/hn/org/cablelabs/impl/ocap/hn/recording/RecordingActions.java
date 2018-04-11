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

package org.cablelabs.impl.ocap.hn.recording;

/**
 *  Class to encapsulate Recording Actions definitions needed for separable builds
 */
 
public class RecordingActions
{

    public static final String ELEMENTS_ARG_NAME= "Elements";

    public static final String RESULT_ARG_NAME = "Result";

    public static final String UPDATEID_ARG_NAME = "UpdateID";

    public static final String BROWSE_RECORD_SCHEDULES_ACTION_NAME = "BrowseRecordSchedules";

    public static final String BROWSE_RECORD_TASKS_ACTION_NAME = "BrowseRecordTasks";

    public static final String ENABLE_RECORD_SCHEDULE_ACTION_NAME = "EnableRecordSchedule";

    public static final String GET_ALLOWED_VALUES_ACTION_NAME = "GetAllowedValues";

    public static final String GET_PROPERTY_LIST_ACTION_NAME = "GetPropertyList";

    public static final String GET_RECORD_SCHEDULE_ACTION_NAME = "GetRecordSchedule";

    public static final String GET_RECORD_TASK_ACTION_NAME = "GetRecordTask";

    public static final String GET_SORT_CAPABILITIES_ACTION_NAME = "GetSortCapabilities";

    public static final String GET_STATE_UPDATE_ID_ACTION_NAME = "GetStateUpdateID";

    public static final String CREATE_RECORD_SCHEDULE_ACTION_NAME = "CreateRecordSchedule";

    public static final String DISABLE_RECORD_SCHEDULE_ACTION_NAME = "DisableRecordSchedule";

    public static final String DISABLE_RECORD_TASK_ACTION_NAME = "DisableRecordTask";

    public static final String X_PRIORITIZE_RECORDINGS_ACTION_NAME = "X_PrioritizeRecordings";

    public static final String DELETE_RECORD_TASK_ACTION_NAME = "DeleteRecordTask";

    public static final String DELETE_RECORD_SCHEDULE_ACTION_NAME = "DeleteRecordSchedule";

    public static final String GET_RECORD_TASK_CONFLICTS_ACTION_NAME = "GetRecordTaskConflicts";

    public static final String RECORD_TASK_ID_KEY = "upnp:srsRecordTaskID";

    public static final String RECORD_SCHEDULE_ID_KEY = "upnp:srsRecordScheduleID";


}


