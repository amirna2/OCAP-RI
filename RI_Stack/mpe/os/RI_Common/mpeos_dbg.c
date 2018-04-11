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

/* Header Files */
#include <assert.h>
#include <stdio.h>
/*lint -e(451)*/
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpeos_dbg.h"
#include "mpeos_util.h"

#include <ri_ui_manager.h>

// Debugging messages are enabled.  Default is enabled (1) and 0 for off.
static int g_debugEnabled = 1;

// RI Platform UI Interface Manager instance.
static ri_ui_manager_t* ri_ui_manager = NULL;

/**
 * Maximum size of the message string that can be logged
 */
#define MAX_MSG_SIZE 4096

/**
 * Returns 1 if logging has been requested for the corresponding module (mod)
 * and level (lvl) combination. To be used in mpeos_dbg* files ONLY.
 */
#define WANT_LOG(mod, lvl) (mpeos_g_logControlTbl[(mod)] & (1 << (lvl)))

static ri_log_level mpe2riLogLevel(mpe_LogLevel mpe_level)
{
    switch (mpe_level)
    {
    case MPE_LOG_FATAL:
        return RI_LOG_LEVEL_FATAL;
    case MPE_LOG_ERROR:
        return RI_LOG_LEVEL_ERROR;
    case MPE_LOG_WARN:
        return RI_LOG_LEVEL_WARN;
    case MPE_LOG_INFO:
        return RI_LOG_LEVEL_INFO;
    case MPE_LOG_DEBUG:
        return RI_LOG_LEVEL_DEBUG;
    case MPE_LOG_TRACE1:
    case MPE_LOG_TRACE2:
    case MPE_LOG_TRACE3:
    case MPE_LOG_TRACE4:
    case MPE_LOG_TRACE5:
    case MPE_LOG_TRACE6:
    case MPE_LOG_TRACE7:
    case MPE_LOG_TRACE8:
    case MPE_LOG_TRACE9:
        return RI_LOG_LEVEL_TRACE;
    default:
        return RI_LOG_LEVEL_DEBUG;
    }
}

/**
 * Initialize Debug API.
 */
void mpeos_dbgInit()
{
    const char* envVar;

    mpeos_dbgLogControlInit();

    /* Try to get logging option. */
    envVar = mpeos_envGet("EnableMPELog");
    if (NULL != envVar)
    {
        g_debugEnabled = (stricmp(envVar, "TRUE") == 0);
    }

    // Open the socket we will be using for output.
    ri_ui_manager = ri_get_ui_manager();
    if (ri_ui_manager == NULL)
    {
        fprintf(stderr,
                "ERROR: Unable to retrieve an instance of RI Platform User Interface manager");
        exit(1);
    }
}

/**
 * Terminate the Debug API.
 */
void mpeos_dbgShutdown()
{
    ri_ui_manager = NULL;
}

/**
 * Send a debugging message to the debugging window.
 *
 * @param level The debug logging level.
 * @param module The module or category for debug output.
 * @param format The <i>printf</i> string describing the message.  This can be followed
 *               by 0 or more arguments to the <i>printf</i> format string.
 */
void mpeos_dbgMsgRaw(mpe_LogLevel level, mpe_LogModule module,
        const char *format, ...)
{
#if !defined(MPE_LOG_DISABLE)
    if (WANT_LOG(module, level))
    {
        va_list args = NULL;

        va_start(args, format);
        if (ri_ui_manager != NULL)
            ri_ui_manager->log_msg(ri_ui_manager, mpe2riLogLevel(level),
                    mpe_logModuleStrings[module], format, args);
        va_end(args);
    }
#endif /* MPE_LOG_DISABLE */
}

/**
 * Send a debugging message to the debugging window.
 *
 * @param flag   Not currently used.
 * @param format The <i>printf</i> string describing the message.  This can be followed
 *               by 0 or more arguments to the <i>printf</i> format string.
 */
void mpeos_dbgMsgEx(unsigned int flag, const char *format, ...)
{
    MPE_UNUSED_PARAM(flag);

    // If we're not actually sending debug messages, skip it
    if (g_debugEnabled)
    {
        va_list args = NULL;

        va_start(args, format);
        if (ri_ui_manager != NULL)
            ri_ui_manager->log_msg(ri_ui_manager, RI_LOG_LEVEL_INFO, "",
                    format, args);
        va_end(args);
    }
}

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
mpe_Error mpeos_dbgStatusGetTypes(mpe_DbgStatusType *types)
{
    MPE_UNUSED_PARAM(types);

    return MPE_SUCCESS;
}

/**
 * <code>mpeos_dbgStatusRegister</code> registers an event queue for status events.
 *
 * @param evq is the event queue to deliver status events to.
 * @param act is the asynchronous callback token to deliver with the event.
 *
 * @return MPE_SUCCESS if the queue was registered successfully.
 */
mpe_Error mpeos_dbgStatusRegister(mpe_EventQueue evq, void *act)
{
    MPE_UNUSED_PARAM(evq);
    MPE_UNUSED_PARAM(act);

    return MPE_SUCCESS;
}

/**
 * <code>mpeos_dbgStatusUnregister</code> unregisters for status events.
 *
 * @param evq is the previously registered event queue.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error mpeos_dbgStatusUnregister(mpe_EventQueue evq)
{
    MPE_UNUSED_PARAM(evq);

    return MPE_SUCCESS;
}

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
        mpe_DbgStatusFormat format, void *param)
{
    MPE_UNUSED_PARAM(typeId);
    MPE_UNUSED_PARAM(format);
    MPE_UNUSED_PARAM(param);

    return MPE_SUCCESS;
}

/**
 * <code>mpeos_dbgStatusUnregisterInterest</code> unregisters interest in a specific status event.
 *
 * @param typeId is the status type identifier for the event.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error mpeos_dbgStatusUnregisterInterest(mpe_DbgStatusId typeId)
{
    MPE_UNUSED_PARAM(typeId);

    return MPE_SUCCESS;
}

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
mpe_Error mpeos_dbgAddLogEntry(char* oid, char* timeStamp, char* message)
{
    if ((NULL == oid) || (NULL == timeStamp) || (NULL == message))
    {
        return MPE_EINVAL;
    }

    // In addition to logging via SNMP also log here via the usual logging framework
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_PERF, "%s %s\n", message, timeStamp);

    if (ri_snmpAddLogEntry(oid, timeStamp, message))
        return MPE_SUCCESS;
    else
        return MPE_ENOMEM;
}


