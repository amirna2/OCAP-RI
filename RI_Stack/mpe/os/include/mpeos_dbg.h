#if !defined(_MPEOS_DBG_H)
#define _MPEOS_DBG_H
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

#include <mpe_types.h>
#include <mpe_dbg.h>
#include <os_dbg.h>
#include <mpeos_event.h>

#ifdef __cplusplus
extern "C"
{
#endif

/* Control the inclusion of logging at compile time. */
#define MPEOS_LOG OS_MPEOS_LOG

/* This variable is described in mpeos_dbg_log.c - to be used by
 * mpeos_dbg.* files only.
 */
extern uint32_t mpeos_g_logControlTbl[];
extern mpe_Bool dbg_logViaUDP;
void mpeos_dbgLogUDP(const char *format, va_list args);

/**
 * <code>mpeos_dbgStatusGetTypes</code> acquires the status types supported by the
 * port-level APIs.  This function should populate the table with the values
 * supported in the porting layer.  The location to populate within the table
 * is specificied in the mpe_dbg.h header file.
 *
 * @param types is a pointer to the global table to populate with the port-level
 *        status types (see mpe_dbg.h for location to populate).
 *
 * @return MPE_SUCCESS if types acquired.
 */
mpe_Error mpeos_dbgStatusGetTypes(mpe_DbgStatusType *types);

/**
 * <code>mpeos_dbgStatusRegister</code> registers an event queue for status events.
 *
 * @param queueId is the event queue to deliver status events to.
 * @param act is the asynchronous callback token to deliver with the event.
 *
 * @return MPE_SUCCESS if the queue was registered successfully.
 */
mpe_Error mpeos_dbgStatusRegister(mpe_EventQueue queueId, void *act);

/**
 * <code>mpeos_dbgStatusUnregister</code> unregisters for status events.
 *
 * @param evq is the previously registered event queue.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error mpeos_dbgStatusUnregister(mpe_EventQueue queueId);

/**
 * <code>mpeos_dbgStatusRegisterInterest</code> registers for delivery of a specific status event.
 *
 * @param typeId is the status type identifier for the event.
 * @param format is the status format identifier.
 * @param param is an optional parameter for the particular status event.
 *
 * @return MPE_SUCCESS if the registration succeeded.
 */
mpe_Error mpeos_dbgStatusRegisterInterest(mpe_DbgStatusId typeId,
        mpe_DbgStatusFormat format, void *param);

/**
 * <code>mpeos_dbgStatusUnregisterInterest</code> unregisters interest in a specific status event.
 *
 * @param typeId is the status type identifier for the event.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error mpeos_dbgStatusUnregisterInterest(mpe_DbgStatusId typeId);

/**
 * A blocking call to process a log entry, possibly resulting the the log entry
 * becoming accessible via the {ocStbHostSystemLogging} table.
 * The entry will not be accessible if table processing is paused or if the
 * group or level threshold criteria of this event do not meet the values
 * specified by the {ocStbHostSystemLoggingResetControl,
 * ocStbHostSystemLoggingLevel, or ocStbHostSystemLoggingGroup} controls.
 *
 * @param oid is a string representation of OID associated with the log table.
 * @param timeStamp is a string representation of the millisecond timestamp
 *                  recorded at the time the event occurred.
 * @param message is the pre-formatted log message that will be truncated at
 *                table entry to 256 bytes (inclusive of a null character).
 *
 * @return MPE_SUCCESS if the log was successfully processed - this does not
 *                     mean the log was actually created and added to the SNMP
 *                     table, it may have been filtered instead.
 * @return MPE_ENOMEM if the log could not be added due to memory limitation.
 * @return MPE_EINVAL if the log could not be processed due to inbound
 *                    parameter(s) being invalid (bad OID, null message, etc).
 */
mpe_Error mpeos_dbgAddLogEntry(char* oid, char* timeStamp, char* message);


#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_DBG_H */

