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

/*
 * This file provides implementations of the MPE OS Common Download API
 * for the CableLabs Reference Implementation.
 */

/* Header files */
#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>      /* Resolve error code definitions */
#include <mpeos_dbg.h>      /* Resolved MPEOS_LOG support. */
#include <mpeos_cdl.h>		/* Resolve common-download definitions */

mpe_EventQueue g_cdlQueue;
void* g_cdlACT;
mpe_Bool g_isRegistered = FALSE;

/**
 * <i>mpeos_cdlRegister()</i>
 * 
 * Register to receive asynchronous CommonDownload events. 
 * NOTE: Only 1 async event listener is supported.  So subsequent calls 
 *       to mpeos_cdlRegister() will override the previous call (ie, the 
 *       previously registered listener will then never be called again).  
 *
 * @param queueId the ID of the queue to be used for notification events
 * @param act the Event Dispatcher handle (as the asynchronous completion token)
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_cdlRegister(mpe_EventQueue queueId, void *act)
{
    if (g_isRegistered)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_CDL,
                "%s: Must call mpeos_cdlUnregister to unregister previous queue!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    g_cdlQueue = queueId;
    g_cdlACT = act;
    g_isRegistered = TRUE;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_cdlUnregister()</i>
 * 
 * Unregister from receiving asynchronous CommonDownload events. 
 *
 * @param queueId the ID of the queue that was used for notification events
 * @param act the Event Dispatcher handle that was used for notification events
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_cdlUnregister(mpe_EventQueue queueId, void *act)
{
    if (g_cdlQueue != queueId || g_cdlACT != act)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_CDL,
                "%s: Attempt to unregister queue that was not previously registered!\n",
                __FUNCTION__);
        return MPE_EINVAL;
    }

    g_isRegistered = FALSE;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_cdlStartDownload()</i>
 * 
 * Initiate the CommondDownload process. 
 *
 * @return MPE_SUCCESS if successful; other error code if not
 */
mpe_Error mpeos_cdlStartDownload(void)
{
    return MPE_EINVAL;
}
